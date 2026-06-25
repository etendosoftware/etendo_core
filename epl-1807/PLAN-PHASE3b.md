# EPL-1807 — Phase 3b Plan: Watched-column subtab, auto-resolved target link, stress tests

Branch: `epic/EPL-1807`. Do not push.

## Context

Phase 3 shipped the synchronous (`'S'`) stored-computed engine: per-dependency enqueue
triggers + the deferred drain (`ad_scd_dirty_aiu` → `ad_scd_process_dirty` →
`ad_scd_recompute`). Dependencies are configured on `AD_COLUMN_COMP_DEPENDENCY`, where two
fields are currently awkward:

- **`WATCHED_COLUMNS`** is a comma-delimited `VARCHAR(200)` of free text — no validation,
  no FK, typo-prone, split in code (`parseWatchedColumns`, `GenerateStoredComputedTriggers.java:386`).
- **`TARGET_ID_RESOLVER_SQL`** is a `required VARCHAR(4000)` of hand-written SQL embedded
  verbatim into the trigger (NOTES.md). Every dependency must author it even though the
  source table almost always has a single FK to the target table that fully determines it.

Phase 3b makes both **declarative and mostly automatic**, and adds a stress-test proposal.
This plan is **design only — no code changes yet.** Three topics:

1. Promote watched columns to a **validated child table / subtab** of the dependency.
2. Make `TARGET_ID_RESOLVER_SQL` **optional**, auto-resolved from a (callout-prefilled,
   overridable) **target-link column**, with the resolver pattern derived from the FK's
   `IsParent` flag.
3. A **stress-test proposal** (fan-in, fan-out, concurrent contention) in `com.etendo.test`.

## Locked decisions

1. **Watched columns:** drop the `WATCHED_COLUMNS` string column entirely. **No data
   migration** — the engine is unreleased; the pilot's watched column is re-entered manually
   as a child row.
2. **Resolver is now one of two inputs (XOR):** `TARGET_ID_RESOLVER_SQL` becomes optional
   (nullable). A new optional `TARGET_LINK_COLUMN_ID` is the declarative alternative.
   **Exactly one** of the two must be set per dependency — neither is individually required,
   but at least one is. Enforced by validation; the UI hides whichever field the other makes
   irrelevant.
3. **Pattern selection is mechanical:** an `IsParent='Y'` FK is **immutable** (a child cannot
   be reparented) → `COALESCE(NEW.fk, OLD.fk)`. A non-parent FK **can change** → `NEW.fk ∪
   OLD.fk` UNION form. The `IsParent` flag is the *only* signal needed to pick the pattern.
4. **Discovery gate is "exactly one FK":** presence of a single source→target FK is what
   lets the callout prefill the link column; `IsParent` only selects the pattern.
5. **Discovery runs only in the callout** (UI), persisting its result in
   `TARGET_LINK_COLUMN_ID`. **No deploy-time discovery / no fallback** — the generator only
   *reads* the stored value. Rows created without the UI (sourcedata/dataset/script) must set
   one of the two fields themselves.
6. **Stress modes:** synchronous `'S'` only (`'Q'` is Phase 4). Small-by-default, crankable.
7. New AD objects (table, columns, tab, fields, reference, validations, callout): all UUIDs
   via `make uuid`. All table/column/constraint identifiers must respect the **Oracle 30-char
   limit**.

---

## Topic 1 — Watched columns as a child table / subtab

### A. New child table `AD_COMPDEP_WATCHED_COL`

Parent: `AD_COLUMN_COMP_DEPENDENCY`. One row per watched column.

| Column | Type | Notes |
|---|---|---|
| `AD_COMPDEP_WATCHED_COL_ID` | VARCHAR(32) PK | `make uuid` |
| `AD_CLIENT_ID`, `AD_ORG_ID`, `ISACTIVE`, audit cols | standard | per AD convention |
| `AD_COLUMN_COMP_DEPENDENCY_ID` | VARCHAR(32) FK → parent | the dependency this row watches |
| `AD_COLUMN_ID` | VARCHAR(32) FK → `AD_COLUMN` | the watched column on the **source** table |
| `SEQNO` | DECIMAL(10,0) default 10 | display order |

- FK `AD_COLUMN_ID` → `AD_COLUMN` (the watched column lives on the source table).
- Unique index on `(AD_COLUMN_COMP_DEPENDENCY_ID, AD_COLUMN_ID)` — no duplicate watches.
- Identifier-length note: `AD_COMPDEP_WATCHED_COL` = 22 chars; PK/constraint suffixes stay
  ≤ 30 (`_KEY`, `_AD_COLUMN`, etc.). Confirm each generated name during implementation.

### B. Drop `WATCHED_COLUMNS`

Remove the `VARCHAR(200)` column from `AD_COLUMN_COMP_DEPENDENCY.xml` (lines 64–67). No
migration.

### C. AD metadata — subtab

- New tab on the dependency window, **child of the "Computation Dependency" tab**, table
  `AD_COMPDEP_WATCHED_COL`, tab level +1, linked via `AD_COLUMN_COMP_DEPENDENCY_ID`.
- The `AD_COLUMN_ID` field is a **picklist scoped to the source table** — a Table reference
  with validation `AD_Column.AD_Table_ID = @SOURCE_TABLE_ID@ AND AD_Column.IsActive = 'Y'`.
  (`@SOURCE_TABLE_ID@` resolves from the parent dependency record.) Same validation pattern
  reused by Topic 2's link column.

### D. Generator — read watched columns from the child table

- `QUERY_DEPS` (`GenerateStoredComputedTriggers.java:58`) no longer selects
  `d.watched_columns`.
- Replace `parseWatchedColumns(String)` (line 386) with a query that loads watched column
  **names** per dependency, e.g. join `AD_COMPDEP_WATCHED_COL` → `AD_COLUMN` →
  `LOWER(ad_column.columnname)` filtered by `ad_column_comp_dependency_id` and `isactive='Y'`.
  Load all rows once (same materialize-before-DDL discipline as the dependency cursor,
  lines 80–101) and group by dependency id.
- `DepRow.watchedCols` (string) → `DepRow.watchedColNames` (`List<String>`), feeding the
  existing `buildFunctionDdl(..., watchedColNames)` (line 118) and `buildEventClause` (line
  406) unchanged. The `UPDATE OF <cols>` clause and the `IS DISTINCT FROM` value guard are
  unaffected — only their *source* changes from string-split to child rows.

---

## Topic 2 — Optional, auto-resolved `TARGET_ID_RESOLVER_SQL`

### Resolution: exactly one of two inputs (XOR)

A dependency is configured by **one** of these — never both, never neither:

1. **`TARGET_ID_RESOLVER_SQL` non-empty** → used **verbatim** (full escape hatch: any join /
   UNION / multi-target SQL). Existing behavior, unchanged.
2. **`TARGET_LINK_COLUMN_ID` set** → generator **renders** the resolver from that FK column at
   deploy; **pattern derived from that column's `IsParent`** (see table below).

There is **no deploy-time discovery**. The FK is discovered once by the **callout** (H) and
persisted in `TARGET_LINK_COLUMN_ID`; the generator merely reads it. If a dependency reaches
deploy with **both fields empty** (only possible via a non-UI insert that bypassed
validation), the generator **fails that dependency** (logged + skipped, like the existing
`REFRESH_MODE`-missing skip at lines 105–109).

**Rendering ≠ discovery.** Two distinct steps, deliberately split:
- *Which FK to walk* — discovered once in the callout, stored in `TARGET_LINK_COLUMN_ID`.
- *FK → resolver SQL string* (COALESCE/UNION, `FROM dual`, and Phase 5's `NEW.`→`:NEW.`) —
  rendered at deploy from the stored FK. The synthesized SQL is **never persisted**, so the
  pattern/dialect stays generated (Phase 5 emits Oracle from the same stored FK, no migration).

### Pattern derivation (mechanical, from `IsParent`)

| FK kind | Mutability | Synthesized resolver (portable, `FROM dual`) |
|---|---|---|
| `IsParent='Y'` | immutable | `SELECT COALESCE(NEW.<fk>, OLD.<fk>) FROM dual` |
| non-parent FK | mutable | `SELECT NEW.<fk> FROM dual WHERE NEW.<fk> IS NOT NULL UNION SELECT OLD.<fk> FROM dual WHERE OLD.<fk> IS NOT NULL` |

Both forms are already the NOTES.md canonical patterns and are Oracle-ready (`FROM dual`;
`public.dual` view exists on Postgres). The Phase 4/5 `NEW.`→`:NEW.` transform applies to
synthesized resolvers identically.

### E. Schema changes on `AD_COLUMN_COMP_DEPENDENCY`

- `TARGET_ID_RESOLVER_SQL`: `required="true"` → `required="false"` (nullable), keep
  `VARCHAR(4000)`.
- New column `TARGET_LINK_COLUMN_ID` VARCHAR(32), **nullable**, FK → `AD_COLUMN`.

### F. AD metadata — link-column field + XOR display/validation

- New field on the Computation Dependency tab: `TARGET_LINK_COLUMN_ID`, a **picklist scoped
  to the source table** — Table reference validated `AD_Column.AD_Table_ID =
  @SOURCE_TABLE_ID@ AND AD_Column.IsActive = 'Y'`, ideally further narrowed to FK-typed
  columns (`AD_Reference_ID IN ('18','19','30')` — Table / TableDir / Search). Same
  validation family as Topic 1's watched-column field.
- **Mutual-exclusion display logic** (the two inputs are XOR):
  - `TARGET_LINK_COLUMN_ID` shown only when `TARGET_ID_RESOLVER_SQL` is empty
    (`@TARGET_ID_RESOLVER_SQL@ is null` / blank).
  - `TARGET_ID_RESOLVER_SQL` shown only when `TARGET_LINK_COLUMN_ID` is empty.
  - New record (both empty) → both visible. The callout fills the link column → the SQL field
    hides. To switch to hand-written SQL, the admin clears the link column → the SQL field
    reappears. Clean toggle, no separate mode flag.
- **Validation that exactly one is set:** a check / model validator (e.g. `@OBDALEventHandler`
  on `AD_COLUMN_COMP_DEPENDENCY` save) rejects rows where both are empty **or** both are set.
  Covers programmatic/import saves, not just the UI. (A pure DB check constraint can also
  express XOR; the event handler additionally gives a localizable message.)
- Field help documents the XOR and that the callout prefills the link column.

### G. FK-discovery routine (callout-only)

One helper (a parameterized SQL query, optionally wrapped in a small Java method) that, given
`(source_table_id, target_table_id)`, returns candidate FK columns with their `IsParent`
flag. Used **only by the callout** (H) — there is no deploy-time caller.

Target table for a dependency = `AD_Column[AD_COLUMN_ID].AD_Table_ID` (the stored-computed
column lives on the target).

### H. Callout — auto-fill `TARGET_LINK_COLUMN_ID`

SimpleCallout on `SOURCE_TABLE_ID` (also re-fire on `AD_COLUMN_ID` change, since that fixes
the target table):

1. Resolve target table from `AD_COLUMN_ID`.
2. Call the discovery routine (G).
3. Apply discovery precedence: single parent FK → set it; else single FK → set it; else leave
   blank and surface a soft message (`N candidates — pick one` / `no FK — enter resolver SQL`).
4. Write into `TARGET_LINK_COLUMN_ID`, **editable** (admin can override, or clear it to write
   resolver SQL instead — the SQL field reappears via the XOR display logic).

**Clobber rule:** the callout overwrites the link column only when it is **empty** or its
current value no longer belongs to the (new) source table — never silently replace a still-
valid deliberate override.

### I. Generator — render-or-verbatim (no discovery)

- `QUERY_DEPS` (`GenerateStoredComputedTriggers.java:58`) joins `TARGET_LINK_COLUMN_ID` to
  `AD_COLUMN` to pull the FK column name + its `IsParent` flag; `DepRow` gains
  `targetLinkColumn` (name) and `targetLinkIsParent`.
- Per dependency, the generator picks the resolver string **before** calling
  `buildFunctionDdl` (line 336), so the inlining path (line 364) is unchanged — only the
  *source* of the string differs:
  - `TARGET_ID_RESOLVER_SQL` non-empty → use it verbatim.
  - else `TARGET_LINK_COLUMN_ID` set → render COALESCE (parent) / UNION (non-parent) from the
    stored FK name. **No discovery query** — the FK is already chosen.
  - else (both empty) → **skip + warn** (mirror lines 105–109).

---

## Topic 3 — Stress-test proposal (`'S'` only)

Harness lives in the gitignored `com.etendo.test` module (commit it in that module's own
repo, **not** etendo_core). Small-by-default so CI stays fast; a single knob cranks scale up
to ~100k for manual load runs. Assertions are **correctness + linearity + zero spurious
writes**, not wall-clock budgets.

The two concerns stress **different** axes:

### Mode 1 — single-transaction batch (drain throughput)

One fat transaction; the `ad_scd_dirty_aiu` constraint trigger fires once per dirty row,
sequentially in one backend at commit (no intra-txn parallelism). Two shapes:

- **Fan-in:** one order, many lines (default ~1k, crank to ~100k). Bulk insert/update/delete
  lines in one txn → assert the single target aggregate is correct and the drain runs **once**
  (one recompute per distinct target, not per line).
- **Fan-out:** one bulk `UPDATE c_orderline SET linenetamt = ...` touching lines across many
  distinct orders (default ~100 orders × ~10 lines, crank to ~100k orders). Assert **every**
  affected order recomputes and **no** unaffected order is touched
  (`IS DISTINCT FROM` guard ⇒ zero spurious writes).

Assert near-linear scaling of drain cost vs. batch size (relative, across two scales — not an
absolute time budget).

### Mode 2 — concurrent contention (lock correctness)

**Parallel transactions** — ≥2 real connections each committing a change to a child of the
**same** target order at once. `ad_scd_recompute` takes
`SELECT 1 FROM <target> WHERE pk=? FOR UPDATE` before computing, so the second drain blocks on
the first, then re-reads. Assert the **final aggregate is correct (no lost update)** under
concurrency. This is the only mode that requires multiple connections; Mode 1 is single-txn.

### Scale knob

A single config constant (env / system property) drives row counts; CI uses the small
default, manual runs override. Document the crank in the harness README.

---

## Implementation order

1. **Schema** (A, B, E): child table, drop `WATCHED_COLUMNS`, nullable resolver, new
   `TARGET_LINK_COLUMN_ID`.
2. **Shared discovery routine** (G) — needed by both generator and callout.
3. **Generator** (D, I): watched columns from child table; render-or-verbatim resolver from
   the stored fields (no discovery); fail-loud when both fields empty.
4. **AD metadata + callout** (C, F, H): subtab, link-column field, XOR display logic + XOR
   validation, callout.
5. **Pilot update** in `com.etendo.test`: remove the watched-string + explicit UNION resolver
   from the dependency row; set `TARGET_LINK_COLUMN_ID` to the `c_order_id` FK (leave resolver
   SQL empty); add the `linenetamt` watched row manually. Verify the rendered resolver matches
   the prior hand-written UNION.
6. **Stress harness** (Topic 3) — delegate to `test-generator`.

## Exit criteria

- Watched columns are validated child rows; no `WATCHED_COLUMNS` string remains; generator
  reads them from the child table and behaves identically (same `UPDATE OF` + value guard).
- Exactly one of `TARGET_ID_RESOLVER_SQL` / `TARGET_LINK_COLUMN_ID` is set (XOR enforced); the
  callout prefills the link column visibly; the two fields hide each other; the generator
  renders COALESCE/UNION from the stored FK per `IsParent`; both-empty fails loud at deploy.
- The pilot passes all Phase 3 integration tests with **no hand-written resolver SQL** (link
  column only).
- Stress harness: fan-in / fan-out single-txn drains correct with zero spurious writes and
  near-linear scaling; concurrent-contention test shows no lost aggregate.

## Commit

```
Epic EPL-1807: Watched-column subtab, auto-resolved target link, stress tests
```
