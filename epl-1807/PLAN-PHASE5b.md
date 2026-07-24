# EPL-1807 — Phase 5b Plan: Build-time (`update.database`) validation of stored computed column definitions

Branch: `epic/EPL-1807`. Do not push. **Design only — no code changes in this plan.**

## Context

Phases 3–5 shipped the stored-computed engine: DB triggers enqueue dirty rows into
`AD_STOREDCOLUMN_DIRTY`, and a drain (`ad_scd_*` on PostgreSQL, `StoredColumnRecomputer` in
Java on Oracle) recomputes columns marked `AD_Column.Computation_Mode = 'S'` by calling their
`Computation_Function`. Dependencies are configured on `AD_COLUMN_COMP_DEPENDENCY` (+ the
`AD_COMPDEP_WATCHED_COL` child table from 3b).

Two guards already exist, but both are **narrow**:

- **Runtime save-time DAL guard** — `src/org/openbravo/event/ColumnStoredComputedHandler.java`
  (an `EntityPersistenceEventObserver` on `Column`). When `Computation_Mode='S'` it enforces
  *SQLLogic blank + Computation_Function set + Computation_Sequence_Number > 0*, throwing
  `OBException(ETGO_StoredComputedColDef)`. It only fires for **new writes through the DAL**;
  it never re-validates metadata already in the DB, and it checks only definition *shape*, not
  deeper correctness (function existence, signature, watched-column existence, cycles).
  Its sibling `ColumnCompDependencyTargetHandler.java` (3b) enforces the
  `Target_ID_Resolver_SQL` XOR `Target_Link_Column` rule (`ETGO_CompDepTargetXor`), also
  per-save only.
- **Generator defensive skips** —
  `src-util/modulescript/src/org/openbravo/modulescript/GenerateStoredComputedTriggers.java`
  silently **skips + logs a warning** for many broken definitions (missing `REFRESH_MODE`,
  neither resolver source set, non-portable resolver, UPDATE event without watched columns, no
  events at all). These are *soft-skips*: `update.database` succeeds and the column simply
  never recomputes. The Java drain does the same — `StoredColumnRecomputer.metaFor()` logs
  *"Stored column {} has incomplete recompute metadata — skipping"* and no-ops.

### The gap

There is **no upfront, whole-DB, build-time check that a stored computed column's *definition*
is correct.** Broken definitions are absorbed silently and surface later as a column that is
just wrong. Phase 5b closes this: at `update.database` time, validate **every** stored computed
definition already in the DB and **fail the build with a clear, aggregated message** when a
hard rule is violated, so a misconfiguration is caught at deploy instead of in production.

### Relationship to the runtime DAL guard (complementary, not redundant)

| | `ColumnStoredComputedHandler` (DAL) | Phase 5b (build) |
|---|---|---|
| When | Per save, pre-commit | Once per `update.database` |
| Scope | The single row being written | **All** `Computation_Mode='S'` rows + their deps |
| Depth | Definition shape only | Shape + function existence/signature/return/volatility + deps + watched cols + cycles + drift + indexes |
| On failure | Rejects that one save (`OBException`) | **Stops the build** (`BuildException`) with an aggregated report |
| Catches legacy bad data | No | **Yes** |

They share the *same shape rules* (V1–V3) via one helper — see **Reuse**.

---

## Locked decisions (proposed)

1. **The engine is unreleased.** No production install has real stored computed columns yet, so
   hard-failing the build on a bad definition **cannot brick a live upgrade**. Phase 5b ships as
   **hard-fail from day one** for the definition-integrity rules; only the heuristic/fuzzy
   checks (type-compat, nullable resolver, volatility on Oracle, indexes) are **warn-only**.
2. **Single source of truth for shape rules.** V1–V3 (the same three the DAL handler enforces)
   are extracted into one pure helper used by *both* the DAL handler and the build validator —
   never duplicated.
3. **Build-time messages are English build-log strings — build-only codes are NOT `AD_MESSAGE`
   rows.** ModuleScripts run under Ant via a raw JDBC `ConnectionProvider` with **no
   `OBContext`/`OBMessageUtils`**, so the validator does not call `messageBD()` and its output is a
   clear English aggregated report to the build log + the thrown `BuildException`. Because those
   codes are never rendered through the translation layer, the build-only violation codes (V4–V16)
   are plain `static final String` constants in `StoredComputedValidator`, **not** `AD_MESSAGE`
   rows — a DB row that is never looked up via `messageBD()` and never translated would be dead
   weight. The only keys that remain `AD_MESSAGE` entries are the two the **runtime DAL handlers**
   render in the UI (`ETGO_StoredComputedColDef` for V1–V3, `ETGO_CompDepTargetXor` for V11); the
   build validator reuses those same two codes as its labels for those specific rules.
4. **Severity is explicit per rule** (hard-fail vs warn), tabulated below. Warn-only rules log a
   `WARN` and let the build proceed; hard-fail rules are collected and reported together, then
   the build stops.

---

## Where it hooks in

### ModuleScript ordering reality (investigated)

`ModuleScriptHandler` discovers ModuleScripts by reading compiled `.class` files from
`src-util/modulescript/build/classes` and each module's `build/classes`, then calls
`preExecute` on each. **There is no inter-ModuleScript dependency/prerequisite ordering** — only
a `Collections.sort` on *module folders*, not on core scripts. So we **cannot guarantee** a
standalone validator ModuleScript runs *before* `GenerateStoredComputedTriggers`.

`ModuleScript.handleError(Throwable)` logs the error and throws `BuildException`, which aborts
`update.database`. That is exactly the "stop the build with a message" behavior we want — but
the default `BuildException` text is generic, so the validator must throw its **own**
`BuildException` carrying the aggregated, human-readable violation report (not route through the
generic `handleError`).

### Recommendation: **(c) both, layered** — one shared helper, one dedicated ModuleScript, one generator guard

- **`StoredComputedValidator`** (new, pure-ish helper) — **the single source of truth** for all
  rules. Pure predicate methods for shape/graph (no DB, unit-testable) + JDBC introspection
  methods for the DB checks. Owns the `ETGO_*` message keys and the report formatting.
  - Suggested location: `src/org/openbravo/erpCommon/ad_process/StoredComputedValidator.java`
    (alongside `StoredColumnRecomputer`), OR a small dedicated package
    `src/org/openbravo/event/scd/`. Prefer the former to sit with the existing SCD Java.
- **`ValidateStoredComputedColumns extends ModuleScript`** (new) — **the primary build-time
  entry point** and documented/testable owner. Class:
  `org.openbravo.modulescript.ValidateStoredComputedColumns`
  (file `src-util/modulescript/src/org/openbravo/modulescript/ValidateStoredComputedColumns.java`,
  mirroring `EnforceStoredComputedReadOnly`). Runs every `update.database`, executes all rules
  via `StoredComputedValidator`, and on any hard violation throws a `BuildException` with the
  aggregated report. This is where V1–V14 and V17 live (definition + dependency + graph +
  refresh-mode + refresh ordering).
- **Guard inside `GenerateStoredComputedTriggers.execute()`** — because ordering across scripts
  is not deterministic, add a one-line call to
  `StoredComputedValidator.assertDefinitionsValid(cp)` as the **first statement** of
  `execute()` (before `deployEngine`). This makes validation a **deterministic precondition of
  deployment within the same script**: if the definitions are invalid, *no* triggers are
  deployed this run. It reuses the exact same helper — no logic duplication.
- **Drift check (V15) lives at the *end* of the generator**, not in the pre-deploy validator,
  because it compares deployed objects against freshly-generated DDL and therefore must run
  *after* deployment. It calls `StoredComputedValidator.checkDeploymentDrift(cp, deps, watched)`
  reusing the generator's already-materialized `DepRow`/watched data and its own
  `buildFunctionDdl`.

**Why not (a)-only** (fold everything into the generator): the generator is already large and
its job is *deploy*, not *diagnose*; a dedicated script is independently testable and mirrors
the existing `EnforceStoredComputedReadOnly` precedent (a focused, single-purpose SCD
ModuleScript). **Why not (b)-only** (dedicated script, nothing in the generator): non-
deterministic ordering means a standalone script might run *after* the generator has already
deployed triggers from a broken definition; the generator guard closes that window
deterministically. The layered design gives SRP + testability (script/helper) *and* fail-before-
deploy (generator guard) with **zero duplicated logic** (one helper).

---

## Validation rules

Legend — **Severity:** `HARD` = collect + fail the build; `WARN` = log and continue.
`PG`/`ORA` note dialect differences. All queries are scoped to
`AD_Column.Computation_Mode = 'S' AND IsActive = 'Y'` and its active dependencies.

### Group A — Definition shape (shared with the DAL handler)

**V1 — SQLLogic must be blank on a stored computed column.** *(user: "ad_column has a sqllogic
value as well as stored computed — do not allow that")*
- Detect: `SELECT ad_column_id FROM ad_column WHERE computation_mode='S' AND isactive='Y' AND COALESCE(TRIM(sqllogic),'')<>''`.
- PG/ORA: identical. Severity: **HARD**. Key: `ETGO_StoredComputedColDef` (reused).

**V2 — Computation_Function must be set.**
- Detect: same scope, `COALESCE(TRIM(computation_function),'')=''`.
- Severity: **HARD**. Key: `ETGO_StoredComputedColDef` (reused).

**V3 — Computation_Sequence_Number must be set and > 0.**
- Detect: `computation_sequence_number IS NULL OR computation_sequence_number <= 0`.
- Severity: **HARD**. Key: `ETGO_StoredComputedColDef` (reused).

> V1–V3 are exactly the DAL handler's rule applied to *all existing rows*. They call the same
> pure predicate `StoredComputedValidator.checkShape(mode, sqlLogic, fn, seq)`.

### Group B — Computation function correctness (build-only, DB introspection)

**V4 — Computation_Function must exist as a DB function.** *(user: "missing function")*
- PG: `SELECT 1 FROM pg_proc WHERE lower(proname)=lower(?)` (optionally schema-qualified).
- ORA: `SELECT 1 FROM all_objects WHERE object_type IN ('FUNCTION') AND lower(object_name)=lower(?)`
  (or `all_procedures` incl. package functions).
- Severity: **HARD**. Key: `ETGO_ScdFunctionMissing`.

**V5 — Function signature/parameters must be compatible.** *(user: "computation function
signature is incompatible, i.e. wrong parameters")* — the engine calls `fn(<pk>)`, i.e. exactly
one argument = the target row's primary key (a `VARCHAR`/UUID id).
- PG: `pg_proc.pronargs = 1` and the single `proargtypes[0]` is a character type
  (`varchar`/`text`/`bpchar`). Join `pg_type`.
- ORA: `SELECT count(*) FROM all_arguments WHERE object_name=UPPER(?) AND in_out='IN'` = 1, arg
  datatype in (`VARCHAR2`,`CHAR`).
- Severity: **HARD** for wrong arity (0 or >1 non-default args); **WARN** for a plausible-but-
  unexpected single-arg type. Key: `ETGO_ScdFunctionSignature`.

**V6 — Function return type must be compatible with the AD_Column reference/type.** *(user:
"function return type is incompatible with the AD_Column reference/type metadata")*
- Map `AD_Column.AD_Reference_ID` → expected SQL type family: numeric refs (Amount `12`,
  Number `22`, Integer `11`, Quantity `29`) → numeric/decimal; String `10`/Text `14` → varchar/
  text; Date `15`/DateTime `16` → date/timestamp; YesNo `20` → char(1)/boolean.
- PG: resolve `pg_proc.prorettype` → `pg_type.typname`, compare against the expected family.
- ORA: `all_arguments` row with `position=0` (return) datatype.
- Severity: **WARN** by default (the ref→type mapping is coarse and false positives would be
  disruptive); **HARD** only for a categorical mismatch (e.g. a Number column whose function
  returns `text`/`date`). Key: `ETGO_ScdFunctionReturnType`.

**V7 — Function should be side-effect free.** *(user: "the function is not side-effect free")* —
a recompute function must not mutate data (it is called inside the drain, potentially many times).
- PG: `pg_proc.provolatile` — expect `i` (IMMUTABLE) or `s` (STABLE); `v` (VOLATILE) is a smell.
  Also `pg_proc.prokind='f'` (a plain function, not a procedure).
- ORA: no volatility marker; best-effort only (optionally scan `all_source` for `INSERT|UPDATE|
  DELETE|MERGE` in the body, or check it is declared `DETERMINISTIC`).
- Severity: **WARN** (PG volatility is a strong hint but IMMUTABLE-misdeclaration is common;
  Oracle can't reliably prove it). Key: `ETGO_ScdFunctionVolatile`.

### Group C — Dependency correctness

**V8 — A synchronous ('S') or queued ('Q') stored computed column must have at least one active
dependency.** *(user: "a synchronous ('S') function, also Queued mode must have an active
dependency"; "dependencies are missing for a synchronous stored computation")* — with no
`AD_COLUMN_COMP_DEPENDENCY` the column is never enqueued and never recomputes after initial
population. Applies to **both** auto-refreshing modes; manual (`'M'`) refresh is exempt — the
operator repopulates it on demand via `StoredColumnRebuild`.
- Detect: `computation_mode='S' AND refresh_mode IN ('S','Q')` columns with **no** active
  `ad_column_comp_dependency` rows (LEFT JOIN … WHERE dep IS NULL).
- Severity: **HARD**. Key: `ETGO_ScdNoDependencies`.

**V9 — A dependency declaring an UPDATE event must have watched columns.** *(user: "a dependency
update event is declared without watched columns")* — the generator currently **silently drops**
the UPDATE event in this case (an unguarded UPDATE trigger risks a re-enqueue loop). Phase 5b
elevates that silent drop to an explicit build failure so the misconfiguration is fixed, not
hidden.
- Detect: `d.update_event='Y'` and **no** active `ad_compdep_watched_col` rows for that dep.
- Severity: **HARD**. Key: `ETGO_ScdUpdateNoWatched`.

**V10 — Watched columns must belong to the dependency's source table.** *(task item 3: "watched/
source columns referenced by each ColumnCompDependency actually exist on the source table")* —
the `AD_COMPDEP_WATCHED_COL.AD_Column_ID` FK guarantees the column *exists*, but not that it is
a column of the dependency's `Source_Table_ID`.
- Detect: join `ad_compdep_watched_col` → `ad_column` and assert
  `ad_column.ad_table_id = dependency.source_table_id`.
- Severity: **HARD**. Key: `ETGO_ScdWatchedColumnTable`.

**V11 — Target resolution XOR must hold for every dependency.** — exactly one of
`Target_ID_Resolver_SQL` / `Target_Link_Column_ID` set (the DAL 3b rule, applied to all rows).
- Detect: `(resolver blank) = (link_column IS NULL)` → violation (both set or neither set).
- Severity: **HARD**. Key: `ETGO_CompDepTargetXor` (reused).

**V12 — Removed.** *(user: "we can skip the null check (V12) but the original code should skip
dirty record generation if the target id is null")* — the static "resolver SQL can return NULL
ids" check is dropped: proving non-nullness of arbitrary resolver SQL is undecidable and produced
false positives. The invariant is instead enforced at **runtime** in the enqueue path — the PG
`buildFunctionDdl` loop skips dirty-record generation when the resolved target id is NULL, using
the same `IF v_target_id IS NULL THEN CONTINUE` guard the Oracle path already has. This is a code
change to `GenerateStoredComputedTriggers` (see Phase 4/5), not a build-time validation rule, so
there is no `AD_MESSAGE` and no `ETGO_Scd*` code for it.

### Group D — Refresh mode

**V13 — Removed.** *(user: "V13 check can be removed")* — the Refresh_Mode domain / RDBMS-
consistency check is dropped from build-time validation. The engine still reads `refresh_mode`,
but validating its value here adds no safety: an invalid value simply falls through to the default
queued path, and the Oracle `'S'→'Q'` downgrade is intended behavior, not a misconfiguration.

### Group E — Dependency graph (cycle detection)

**V14 — The stored-computed dependency graph must be acyclic.** *(user: "cycle detection (2 or 3
levels deep)"; "cycles between stored computed columns exist without explicit refresh ordering")*
- Graph model: one node per `Computation_Mode='S'` column. Add a directed edge **A → B** when
  recomputing A can dirty B, i.e. B has an active dependency whose `Source_Table_ID` = A's target
  table **and** whose watched columns include A's stored column (A's recompute writes A's column
  on A's target table; if B watches that column on that table, A's write enqueues B).
- Detection: standard **DFS with three-color marking** (white/gray/black); a back-edge to a
  gray node is a cycle. DFS finds cycles of **any** length — the "2 or 3 levels deep" the user
  mentions are just the common short cases; the algorithm is not depth-limited. On detection,
  surface the **full cycle path** (column names) in the message, not just the fact of a cycle.
- **Every cycle is HARD — unconditionally.** An earlier revision of this plan proposed downgrading
  a cycle to WARN when `Computation_Sequence_Number` was "strictly increasing along every edge of
  the cycle". That condition is **unsatisfiable by construction**: going around a closed loop it
  demands `seq[first] < … < seq[last] < seq[first]`. It *should* be unsatisfiable — a cycle is
  precisely a dirty set with **no topological order**, so no sequence assignment can make the drain
  refresh it correctly. Sequence numbers order an acyclic graph; they cannot break a cycle. The
  downgrade branch was therefore dead code resting on an unsound idea, and has been removed.
- Pure/unit-testable: the graph + DFS is a pure function over an edge list — see Testing.
- Severity: **HARD** (every cycle). Key: `ETGO_ScdDependencyCycle`.

**V17 — Every edge of the acyclic graph must be honoured by `Computation_Sequence_Number`.**
- Same edge set as V14: **A → B** when B declares an active dependency whose `Source_Table_ID` is
  A's target table and whose watched columns include A's stored column — i.e. B's computation
  function reads A's stored value. That declaration is exactly how a user says "B depends on A".
- Chaining works by **ordering, not cascade**: the recursion guard (`my.scd_refreshing`) means the
  engine's own recompute writes never enqueue anything, so a chain A → B only works because a
  single source write dirties A and B *independently* and the drain then recomputes them in
  `Computation_Sequence_Number` order. Both drains order that way
  (`GenerateStoredComputedTriggers.PROCESS_DIRTY_FN` for `'S'`,
  `StoredColumnQueueProcessor.FETCH_SQL` for `'Q'`). Sequence numbers consistent with the
  dependency graph are therefore **load-bearing for correctness**, not a cosmetic hint.
- Fires when `seq[A]` is null, `seq[B]` is null, or `seq[A] >= seq[B]`. **Equality counts:** ties
  break on `target_record_id` (`'S'`) or `created` (`'Q'`), both arbitrary with respect to the
  dependency, so equal numbers give no ordering guarantee at all.
- Edges inside a cycle already reported by V14 are **suppressed** — the cycle is the real finding
  and per-edge noise on top of it is unhelpful.
- Severity: **WARN**, never ERROR. An unset or non-positive sequence number on an `'S'` column is
  already a hard V3 error, so erroring here would only double-report it; what remains (two
  configured columns merely ordered wrongly relative to each other) belongs to the same advisory
  family as V15/V16 — a fix a developer should make, not a reason to fail an install.
- Pure/unit-testable: `findSequenceOrderViolations(adjacency, seqByNode, cycles)` is a pure
  function over the same edge list. Key: `ETGO_ScdSequenceOrder`.

### Group F — Deployment integrity (runs *after* generation, inside the generator)

**V15 — Deployed triggers must exist and match the generated definition.** *(user: "trigger
definitions expected from dictionary metadata are missing or drift from generated definitions")*
- For each active dependency, the expected objects are `ad_scd_<depId>_trf` (PG function) +
  `ad_scd_<depId>_trg` (trigger), or `ad_scd_<depId>_trg` (Oracle inline trigger).
- Missing: PG `pg_proc`/`pg_trigger`, ORA `user_triggers` — expected object absent.
- Drift: compare the deployed PG function body (`pg_get_functiondef`) / Oracle trigger source
  (`user_source`) against a freshly rendered `buildFunctionDdl(...)` / `deployOracleTrigger`
  string (normalize whitespace before comparing).
- Severity: **HARD** for a missing expected object; **WARN** for body drift (drift usually means
  the generator ran; a mismatch is worth surfacing but rebuild-on-next-run self-heals).
- Location: end of `GenerateStoredComputedTriggers.execute()` (post-deploy), reusing its
  materialized `deps`/`watchedByDep`. Key: `ETGO_ScdTriggerMissing` / `ETGO_ScdTriggerDrift`.

### Group G — Performance advisories

**V16 — Recommended index on the target-resolving FK / link column.** *(user: "required indexes
for finding target column is needed → flagged as a warning")* — the enqueue resolver walks a
source→target FK (`Target_Link_Column_ID`, or the FK inside a hand-written resolver) and the
recompute reads child rows by that FK; without an index those lookups scan.
- Detect (best-effort): for each dependency's `Target_Link_Column_ID` (when set), check
  `pg_index`/`user_indexes` for an index whose leading column is that FK column on the source
  table. Hand-written resolvers can't be introspected reliably → advise generically.
- Severity: **WARN only** (advisory; never blocks a build). Key: `ETGO_ScdMissingIndex`.

### Severity summary

| Rule | Check | Default severity |
|---|---|---|
| V1 | SQLLogic blank on 'S' | HARD |
| V2 | Computation_Function set | HARD |
| V3 | Computation_Sequence_Number > 0 | HARD |
| V4 | Function exists | HARD |
| V5 | Function arity/param type | HARD (arity) / WARN (type) |
| V6 | Return type vs AD ref | WARN (HARD on categorical mismatch) |
| V7 | Side-effect free | WARN |
| V8 | 'S'/'Q' column has ≥1 dependency | HARD |
| V9 | UPDATE event has watched cols | HARD |
| V10 | Watched col on source table | HARD |
| V11 | Target resolver XOR | HARD |
| ~~V12~~ | Removed — runtime NULL-target guard instead | — |
| ~~V13~~ | Removed — Refresh_Mode check dropped | — |
| V14 | Acyclic dependency graph | HARD (every cycle) |
| V15 | Trigger present / no drift | HARD (missing) / WARN (drift) |
| V16 | Index on target FK | WARN |
| V17 | Per-edge refresh ordering (seq[A] < seq[B] on every A → B) | WARN |

---

## Failure behavior

- **Collect, then fail once.** The validator accumulates all hard violations across all columns/
  dependencies into a list, logs every warning, and — if the hard list is non-empty — throws a
  **single** `BuildException` with an aggregated, numbered report so the operator fixes
  everything in one pass rather than one-error-per-rerun.
- **Message format** (build log + exception text), e.g.:
  ```
  Stored computed column validation failed (N error(s), M warning(s)):
    [ERROR] ETGO_ScdFunctionMissing: column C_Order.EM_Ettst_LineTotal — Computation_Function
            'ettst_sumlineamounts' does not exist in the database.
    [ERROR] ETGO_ScdDependencyCycle: cycle A → B → A among stored computed columns
            (C_Order.X → C_OrderLine.Y → C_Order.X) with no strict refresh ordering.
    [WARN ] ETGO_ScdMissingIndex: dependency <id> — no index on source FK c_order_id.
  Fix the definitions above and re-run update.database.
  ```
- **Do not route through the generic `handleError`** — throw the validator's own
  `BuildException(report)` so the aggregated report (not "…failed.") is what stops the build.
- **Migration safety.** Because the engine is unreleased (Locked decision 1) the definition-
  integrity rules hard-fail immediately. Should a grace period ever be needed, wrap enforcement
  in a toggle (system property / env var `ETGO_SCD_VALIDATION=warn|enforce`, default `enforce`)
  so an operator can temporarily downgrade hard→warn to unblock an upgrade — documented in
  `OPERATIONS.md`. The heuristic rules (V6 partial, V7, V16) are warn-only and never block
  regardless.

### Violation codes

Two kinds, deliberately kept separate:

**A. Reused `AD_MESSAGE` keys (already exist — the runtime DAL handlers render these in the UI).**
No new rows; the build validator reuses them as labels for the same rules the DAL enforces:
- `ETGO_StoredComputedColDef` — V1–V3 (shape rules, shared with `ColumnStoredComputedHandler`)
- `ETGO_CompDepTargetXor` — V11 (target XOR, shared with `ColumnCompDependencyTargetHandler`)

**B. Build-only codes — English `static final String` constants in `StoredComputedValidator`, NOT
`AD_MESSAGE` rows.** These are emitted only by the build-time validator into the English build log
(the ModuleScript has no `OBMessageUtils` and never translates them), so an `AD_MESSAGE` row would
never be looked up and never translated — creating one is dead weight. The codes keep the `ETGO_`
prefix used in the rule table below purely as stable label strings (used as the `[ERROR]`/`[WARN ]`
prefix in the aggregated report); they are constants, not `AD_MESSAGE` values:

`ETGO_ScdValidationFailed` (report header), `ETGO_ScdFunctionMissing`, `ETGO_ScdFunctionSignature`,
`ETGO_ScdFunctionReturnType`, `ETGO_ScdFunctionVolatile`, `ETGO_ScdNoDependencies`,
`ETGO_ScdUpdateNoWatched`, `ETGO_ScdWatchedColumnTable`, `ETGO_ScdResolverNullable`,
`ETGO_ScdRefreshModeInvalid`, `ETGO_ScdRefreshModeOracle`, `ETGO_ScdDependencyCycle`,
`ETGO_ScdTriggerMissing`, `ETGO_ScdTriggerDrift`, `ETGO_ScdMissingIndex`.

> No new `AD_MESSAGE.xml` rows and no new UUIDs are created by this plan — the two UI-rendered keys
> already exist. If any of these build-only checks is ever surfaced through the DAL/UI in future,
> promote just that one code to an `AD_MESSAGE` row at that point.

---

## Reuse (single source of truth)

The rule logic must **not** be duplicated between the DAL handler and the build validator.

- Extract the shape predicate the DAL handler already implements into
  `StoredComputedValidator.checkShape(String computationMode, String sqlLogic, String fn, Long seq)`
  returning a violation (message key + detail) or none.
  - `ColumnStoredComputedHandler.validateStoredComputedDefinition(Column)` is refactored to build
    the tuple from the DAL object and call `checkShape(...)`, throwing
    `OBException(messageBD(key))` on a violation. Behavior is unchanged; the logic now lives in
    one place.
  - `ValidateStoredComputedColumns` (build) and the `GenerateStoredComputedTriggers` guard call
    the **same** `checkShape(...)` per row read over JDBC.
- The cycle detector `StoredComputedValidator.findCycles(edges, seqByNode)` is a pure function
  (no DB) — directly unit-testable and reused anywhere a graph is available.
- The DB-introspection checks (V4–V16) live as JDBC methods on `StoredComputedValidator` keyed by
  RDBMS, so the ModuleScript and the generator guard/drift call identical code paths.
- Keep the helper free of `OBContext`/DAL imports in its pure methods so the ModuleScript (Ant/
  JDBC, no OBContext) can call them; DB methods take a `ConnectionProvider`/`Connection`.

---

## Testing

Delegate all test authoring per CLAUDE.md (`test-generator` / `/etendo:test`).

- **Pure-logic JUnit** (`src-test/…`, no DB):
  - `checkShape(...)`: matrix over mode ∈ {S, non-S} × sqlLogic set/blank × fn set/blank × seq
    null/0/positive — assert exactly the DAL handler's current outcomes (guards against
    regressions from the refactor).
  - `findCycles(...)`: acyclic graph → none; 2-node cycle; 3-node cycle; self-loop; every cycle
    is hard regardless of the sequence numbers on it.
  - `findSequenceOrderViolations(...)` (V17): well-ordered chain `a(1) → b(2)` → none; equal
    numbers `a(0) → b(0)` → warn; decreasing numbers → warn; missing (null) number on either end
    → warn; path `a(1) → b(2) → c(1)` → warn on `b → c` only; edges inside a V14-reported cycle
    → suppressed.
  - Coarse type/ref compatibility map (V6): representative ref→type pairs, compatible + mismatch.
- **DB / integration** (`OBBaseTest`, PostgreSQL): seed a deliberately-broken `AD_Column` +
  `AD_COLUMN_COMP_DEPENDENCY` (+ `AD_COMPDEP_WATCHED_COL`) fixture per rule, invoke the
  `StoredComputedValidator` DB methods against the test DB, and assert the expected violation
  key/severity is reported. One negative fixture per hard rule (V4, V8, V9, V10, V11, V14) plus a
  clean all-valid fixture that reports zero violations.
  - **Seeding illegal shapes (V1–V3) must bypass the DAL.** `ColumnStoredComputedHandler` now
    rejects a stored computed column with SQLLogic set / missing function / missing sequence at
    persist time, so those illegal definitions **cannot** be created through the DAL (`OBDal.save`)
    or a normal `.xml` import — the observer throws first. Fixtures for V1–V3 must be inserted with
    **raw SQL/JDBC** (direct `INSERT`/`UPDATE` on `AD_COLUMN`, no observer in the path). The
    build-only rules V4–V16 are not policed by the DAL, so their fixtures can be seeded the normal
    way. This split is also the boundary between the two test targets: the DAL handler's own shape
    tests assert the *rejection*; the build validator's V1–V3 tests assert *detection* of rows that
    slipped in via raw SQL.
- ~~**SQL fixtures / documentation** — new
  `modules/com.etendo.test/src-test/sql/stored_computed_validation_scenarios.sql` alongside the
  existing `stored_computed_*` files, documenting each negative case and its detection query
  (parity with `stored_computed_engine_scenarios.sql`). Note the `com.etendo.test` module is
  committed in its own repo, not `etendo_core` (per 3b).~~
  **Superseded** — never written. The JUnit validator suite (`StoredComputedValidatorTest`,
  `StoredComputedValidatorPureTest`, `StoredComputedValidatorRulesTest`,
  `StoredComputedValidatorLiveDbTest`, landed in `c77354b1`) covers the same validation rules
  with better isolation and CI integration than a live-DB SQL script would.
- **Drift (V15)**: an integration test that deploys via the generator, tampers with one deployed
  function body, re-runs `checkDeploymentDrift`, and asserts a drift warning; and one that drops
  a deployed trigger and asserts a `ETGO_ScdTriggerMissing` hard error.

---

## Risks / open questions / rollout

- **Legacy bad data.** Mitigated by Locked decision 1 (engine unreleased → safe to hard-fail).
  The `ETGO_SCD_VALIDATION=warn` escape hatch exists if a grace period is ever required.
- **Oracle introspection parity.** `all_arguments`/`all_procedures`/`user_source` differ from
  the PG catalogs; Oracle has **no volatility flag** (V7 is warn/best-effort there) and packaged
  functions complicate name resolution (V4/V5). Open question: do we support package-qualified
  `Computation_Function` names on Oracle, and if so how do we introspect them? Default: support
  plain schema-level functions first; warn (not fail) when a name can't be resolved on Oracle.
- **Type-compatibility false positives (V6).** The `AD_Reference_ID` → SQL-type family map is
  coarse (custom references, `AD_Reference_Value_ID` lists, etc.). Kept **warn-only** except for
  categorical mismatches to avoid blocking legitimate setups. Open question: enumerate the exact
  ref-id → type-family table (extend the `IN (...)` sets above during implementation).
- **NULL target ids (was V12, now a runtime guard).** Static detection of nullable resolver SQL was
  dropped (undecidable, false-positive-prone). The invariant is enforced at runtime instead: the PG
  `buildFunctionDdl` loop skips dirty-record generation when `v_target_id IS NULL` (`IF … THEN
  CONTINUE`), matching the Oracle path — a small change to `GenerateStoredComputedTriggers`.
- **Performance.** All queries are scoped to `Computation_Mode='S'` columns (a handful, not the
  full ~tens-of-thousands-row `AD_Column`), plus their few dependencies; catalog lookups are per-
  function. Cost is negligible on every `update.database`. The cycle DFS is over the same tiny
  node set.
- **Ordering caveat (V15).** Drift lives in the generator (post-deploy) precisely because a
  standalone validator has no guaranteed order relative to the generator. Confirm during
  implementation that placing it at the end of `execute()` sees the just-deployed objects (same
  connection, committed per `execute()`).
- **Message availability at build time (settled).** ModuleScripts run under Ant on a raw JDBC
  `ConnectionProvider` with no `OBContext`/`OBMessageUtils`, so build output is English-only.
  Decision: build-only codes V4–V16 are English `static final String` constants in
  `StoredComputedValidator` (used as the `[ERROR]`/`[WARN ]` label in the aggregated report) — **no
  new `AD_MESSAGE` rows**. Only the two codes that are also rendered by the runtime DAL handlers
  (`ETGO_StoredComputedColDef` for the shape check V1–V3, `ETGO_CompDepTargetXor` for the target XOR
  V11) stay as `AD_MESSAGE` entries, and both already exist — the plan adds none.

---

## Task breakdown (ordered)

1. [ ] **Shared helper** — create `StoredComputedValidator` with: pure `checkShape(...)`, pure
       `findCycles(edges, seqByNode)`, the coarse ref→type map, and JDBC methods
       `assertDefinitionsValid(cp)` / per-group `check*` / `checkDeploymentDrift(...)`. Define a
       `Violation(severity, code, detail)` value type (`code` = the English label string, not an
       `AD_MESSAGE` value) + report formatter + the `BuildException`-throwing entry point.
2. [ ] **Refactor the DAL handler** — `ColumnStoredComputedHandler` delegates its shape check to
       `StoredComputedValidator.checkShape(...)` (behavior-preserving; verify with existing/added
       unit tests).
3. [ ] **Group A/B/C rules** (V1–V11) — implement the JDBC detection queries (PG + Oracle) in
       the helper, each returning `Violation`s with the right key/severity.
4. [ ] **Cycle detection** (V14) — build the edge list query, implement `findCycles` (3-color
       DFS), surface the full cycle path in the message; every cycle is hard. Reuse the same edge
       list for the V17 per-edge ordering advisory (`findSequenceOrderViolations`).
5. [ ] **Dedicated ModuleScript** — `ValidateStoredComputedColumns extends ModuleScript`
       (`src-util/modulescript/…`), runs `assertDefinitionsValid(cp)` and throws its own
       aggregated `BuildException`; mirror `EnforceStoredComputedReadOnly` structure.
6. [ ] **Generator guard** — call `StoredComputedValidator.assertDefinitionsValid(cp)` as the
       first statement of `GenerateStoredComputedTriggers.execute()` (fail before any DDL).
7. [ ] **Drift check** (V15) — add `checkDeploymentDrift(...)` and invoke it at the end of
       `GenerateStoredComputedTriggers.execute()` reusing its materialized `deps`/`watchedByDep`
       and `buildFunctionDdl`.
8. [ ] **Index advisory** (V16) — implement the warn-only FK-index check (PG + Oracle).
9. [ ] **Violation codes** — declare the build-only codes (`ETGO_Scd*`, V4–V16) as English
       `static final String` constants in `StoredComputedValidator`; **no new `AD_MESSAGE.xml` rows,
       no `make uuid`**. Reuse the two existing UI-rendered keys `ETGO_StoredComputedColDef` (V1–V3)
       and `ETGO_CompDepTargetXor` (V11) for the runtime DAL paths.
10. [ ] **Rollout toggle** — `ETGO_SCD_VALIDATION` (default `enforce`); document in
        `OPERATIONS.md`.
11. [ ] **Tests** (delegate to Tester) — pure-logic JUnit (shape, cycles, type map), OBBaseTest
        per-rule negative + clean fixtures, drift/missing-trigger integration tests, and
        ~~`stored_computed_validation_scenarios.sql` in `com.etendo.test`~~ *(superseded — the
        SQL script was never written; the JUnit validator suite `StoredComputedValidatorTest` /
        `StoredComputedValidatorPureTest` / `StoredComputedValidatorRulesTest` /
        `StoredComputedValidatorLiveDbTest`, landed in `c77354b1`, covers the same rules with
        better isolation and CI integration — see the Tests section above)*.
12. [ ] **Docs** — update `epl-1807/REQUIREMENTS.md` / `OPERATIONS.md` and field help with the
        validation rules and the failure/toggle behavior.

## Exit criteria

- `update.database` **hard-fails with a clear aggregated message** when any hard rule (V1–V5
  arity, V8–V11, V14 any cycle, V15 missing) is violated by *any* existing stored computed
  definition — verified by the negative fixtures.
- The DAL handler and the build validator share one `checkShape` implementation; no duplicated
  rule logic.
- Warn-only rules (V6 type, V7 volatility, V16 index, V15 drift, V17 ordering) log clearly and
  never block the build.
- Cycle detection reports the full column path and hard-fails on **every** cycle, regardless of
  the `Computation_Sequence_Number` values on it.
- The V17 ordering advisory warns on every `A → B` edge where `seq[A] >= seq[B]` (or either is
  unset), and stays silent on edges inside a cycle already reported by V14.
- All new tests pass on PostgreSQL; Oracle-specific introspection paths are covered or explicitly
  documented as best-effort.

## Commit

```
Epic EPL-1807: Build-time validation of stored computed column definitions
```
