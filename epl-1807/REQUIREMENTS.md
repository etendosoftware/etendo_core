# Product Requirement: Stored Computed Columns in Application Dictionary

---

## 1. Problem Statement

Etendo supports computed columns through `AD_Column.SQLLOGIC`. These columns are exposed as virtual, lazily-evaluated properties: the SQL expression is embedded in the Hibernate mapping as a formula and evaluated at query time. While useful for deriving display values from related data, the virtual approach has significant runtime costs.

Expensive per-row expressions slow down grid queries in proportion to result set size. Virtual columns cannot be indexed, so filtering and sorting on them trigger full scans. Their dependencies are implicit — a change in a referenced table produces no notification and no refresh. Broken SQL or type mismatches surface at runtime in production rather than during setup or compilation. These risks compound as windows grow in usage and data volume grows.

The goal is to introduce a first-class Application Dictionary feature for **stored computed columns**: physically persisted database columns whose values are maintained by the system from dictionary-defined SQL functions and explicit dependency metadata. The feature preserves the functional value of computed columns while making performance, correctness, and dependency relationships explicit, enforceable, and auditable.

---

## 2. Goals and Non-Goals

### Goals

- **Bounded read performance.** Grid, filter, sort, report, and API queries over computed values should perform like queries over any other indexed physical column — not arbitrarily slowly depending on function complexity and data volume.
- **Single source of truth.** The rule that produces a computed value is declared once in the Application Dictionary and applied consistently. Drift between callouts, background scripts, and the dictionary definition is not possible.
- **Early failure.** Configuration mistakes — missing physical column, broken function, missing dependencies, type mismatches — must be caught at setup and validation time, not in production.
- **Operational recovery.** When data is migrated, a module is updated, or a computation function is corrected, there must be a supported and repeatable way to recompute all stored values and verify their correctness.
- **Correct and auditable derivation.** The stored value is always the output of the declared function, never written by application code, callouts, or manual edits.

### Non-Goals

- Do not replace existing virtual computed columns (`SQLLOGIC`) immediately or force migration.
- Do not support HQL as a computation source.
- Do not add module-specific or window-specific logic to generic services.
- Do not rely on manually created triggers or functions outside the dictionary-managed model.
- Do not guarantee intra-transaction consistency — business logic that needs a refreshed stored value during the same transaction must call the computation function directly.
- Do not allow the stored column refresh to become a synchronization point, lock source, or failure path for unrelated business operations.
- Do not guarantee that every computed column is a good candidate for storage; eligibility must be validated.

---

## 3. Requirements

### 3.1 Functional Requirements

1. Declarable on any `AD_Column` in the Application Dictionary.
2. The target column must physically exist in the database.
3. Read-only from the UI, DAL, and all data services at all times.
4. The SQL function is the sole writer; no other code path may set the value.
5. Dependencies (source tables and trigger events) declared in the dictionary and exportable as part of the module.
6. Refresh triggers generated from dictionary metadata; hand-written triggers are not acceptable.
7. Idempotent rebuild recomputes all rows from scratch.
8. Consistency check identifies rows where the stored value differs from the function output.
9. Existing virtual computed columns (`SQLLOGIC`) continue to work without change.
10. DAL entities map stored computed columns as physical properties, not `_computedColumns` proxies.

### 3.2 Validation and Compilation Requirements

Fail with a clear error when:

- Physical column missing for `Computation_Mode = S`.
- Column has both `SQLLOGIC` and `Computation_Mode = S`.
- Computation function does not exist in the database.
- Function signature incompatible with the target table's ID type.
- Function return type incompatible with the column's reference and data type.
- Function is `VOLATILE` (side effects detected).
- No dependency rows declared for a synchronous column.
- Update-event dependency has no watched columns.
- Target ID resolver SQL can return `NULL`.
- Cycle detected between stored computed columns.
- Deployed triggers differ from current metadata.
- Required filter/sort indexes are missing.

Warn (not fail) on sequential scans or `STABLE` rather than `IMMUTABLE` function classification.

### 3.3 Concurrency Requirements

- Mid-transaction reads return the pre-transaction value.
- N affected rows produce exactly N recalculations after deduplication, not N × (statement count).
- Refresh does not update audit columns.
- A function error rolls back the entire originating transaction.
- Refresh does not re-trigger dependency collection recursively.

### 3.4 Performance Requirements

- Read performance (grid, filter, sort, report, API) must improve over virtual computed columns.
- Write-side overhead bounded by the declared dependency set and measurable in isolation.
- Refresh skips writes when the value is unchanged.

### 3.5 Acceptance Criteria

- Fully declarable and exportable as part of a module.
- Each validation failure (physical column, function, dependencies, triggers) produces a distinct, actionable error.
- DAL maps as physical property, no setter; `save()` emits no SQL for the column.
- Existing virtual computed columns remain fully compatible.
- Synchronous refresh writes each affected row exactly once, after all business DML.
- Mid-transaction reads return the pre-transaction value.
- N affected rows → N dirty entries → N recalculations.
- Function error rolls back the entire transaction.
- Rebuild repairs corrupted stored values.
- Consistency check detects stale stored values.
- Tests cover: insert, update, delete dependencies; bulk update deduplication; rebuild; consistency check failure.

### 3.6 Build-time validation rules (Phase 5b)

The generic requirements in §3.2 are realised at build time by `StoredComputedValidator`
(run via the `ValidateStoredComputedColumns` ModuleScript on every `update.database`, and
re-run as Gate 0 inside `GenerateStoredComputedTriggers`). Each rule below has a stable code,
a severity, and defined PostgreSQL / Oracle coverage. `HARD` (`ERROR`) rules abort the build;
`SOFT` (`WARN`) rules are logged only. The whole set is collected into one aggregated report
(errors first, then warnings) and thrown as a single `BuildException`. Enforcement is governed
by the `ETGO_SCD_VALIDATION` toggle (default `enforce`; `warn` downgrades everything to a log —
see `OPERATIONS.md`).

Only two rules reuse existing `AD_MESSAGE` keys (they also fire in the runtime DAL observer):
`ETGO_StoredComputedColDef` (V1–V3) and `ETGO_CompDepTargetXor` (V11). All other codes are
build-only English constants — **no new `AD_MESSAGE` rows and no new UUIDs** are introduced.

| Rule | Check | Severity | Code | PostgreSQL | Oracle |
|------|-------|----------|------|------------|--------|
| V1 | Stored column (`Computation_Mode='S'`) must have empty `SQLLogic` | HARD | `ETGO_StoredComputedColDef` | ✅ | ✅ |
| V2 | Stored column must have a `Computation_Function` | HARD | `ETGO_StoredComputedColDef` | ✅ | ✅ |
| V3 | Stored column must have `Computation_Sequence_Number > 0` | HARD | `ETGO_StoredComputedColDef` | ✅ | ✅ |
| V4 | Computation function must exist in the database | HARD | `ETGO_ScdFunctionMissing` | ✅ | ✅ (existence only) |
| V5 | Function arity must be 1; the single arg should be a string/ID type | HARD (arity≠1) / SOFT (non-string arg) | `ETGO_ScdFunctionSignature` | ✅ | ⏭️ skipped (no signature introspection) |
| V6 | Function return type compatible with column reference family | HARD (void/trigger/record) / SOFT (family mismatch) | `ETGO_ScdFunctionReturnType` | ✅ | ⏭️ skipped |
| V7 | Function should be `IMMUTABLE`/`STABLE`, not `VOLATILE` | SOFT | `ETGO_ScdFunctionVolatile` | ✅ | ⏭️ skipped |
| V8 | Active stored column must have ≥1 active dependency row | HARD | `ETGO_ScdNoDependencies` | ✅ | ✅ |
| V9 | Update-event dependency must declare ≥1 watched column | HARD | `ETGO_ScdUpdateNoWatched` | ✅ | ✅ |
| V10 | Watched column must belong to the dependency's source table | HARD | `ETGO_ScdWatchedColumnTable` | ✅ | ✅ |
| V11 | Dependency must set exactly one of `target_id_resolver_sql` / `target_link_column_id` | HARD | `ETGO_CompDepTargetXor` | ✅ | ✅ |
| V14 | No dependency cycle among stored computed columns | HARD (unordered) / SOFT (sequence-ordered) | `ETGO_ScdDependencyCycle` | ✅ | ✅ |
| V15 | Deployed triggers/functions must match current metadata | HARD (missing) / SOFT (drift) | `ETGO_ScdTriggerMissing` / `ETGO_ScdTriggerDrift` | ✅ (presence + body) | ✅ presence only (no body drift) |
| V16 | FK/watched columns should have a supporting index | SOFT | `ETGO_ScdMissingIndex` | ✅ (`pg_index`) | ✅ (`user_ind_columns`) |

**Oracle degradations.** Oracle function introspection is existence-only, so V4 fires but
V5–V7 are skipped (no reliable arity/return-type/volatility catalog for the deployed PL/SQL).
V15 on Oracle verifies trigger presence but not body drift. Catalog/index introspection
failures are best-effort: they log a warning and skip rather than aborting the build; only
genuine query failures inside a HARD check are wrapped and rethrown.

**Shared pure logic.** `checkShape(...)` (V1–V3) and `findCycles(...)` (V14) take only
String/primitive/collection arguments — no DAL types — so the same code backs both the
build-time JDBC path and the runtime DAL observer (`ColumnStoredComputedHandler`), guaranteeing
the UI/API save guard and the build gate never diverge.

---

## 4. Proposed Solution

### 4.1 Summary

The core idea is straightforward: give each computed column a real physical database column and let the system — not application code — maintain it.

A new `Computation_Mode = S` flag on `AD_Column` marks a column as stored computed. The developer declares a SQL function that computes the value given the target record's ID, and a set of dependency rows that state which source tables and events (insert, update, delete) should trigger a refresh. Everything else — the triggers, the refresh logic, the read-only enforcement — is generated and owned by the system.

**The refresh mechanism is two-phase, end-of-transaction:**

The first phase is lightweight dirty collection. Database AFTER triggers fire during each DML statement on a source table, check whether the relevant event and watched columns match, resolve affected target record IDs, and insert those IDs into a shared tracking table (`AD_StoredColumn_Dirty`). These triggers do nothing else — no computation, no locks beyond the insert.

The second phase is deferred recalculation. A `DEFERRABLE INITIALLY DEFERRED` constraint trigger on `AD_StoredColumn_Dirty` fires for each inserted dirty row, deferred until just before commit. The first firing reads and deletes all dirty rows for the current transaction in a single ordered pass; subsequent firings from the same transaction find no remaining rows and return immediately. This ensures the computation function is called exactly once per target row, in `Computation_Sequence_Number` order.

This approach has four properties that drive the rest of the design:

1. **Fast dirty collection.** Triggers insert IDs and return — no computation during DML.
2. **Non-blocking.** The deferred pass runs at commit time, not between statements.
3. **End-of-transaction consistency.** The computation function always sees the final committed state of all source data for that transaction.
4. **No intra-transaction guarantee.** A stored column reflects transaction-boundary state only. Business logic that needs a current value during a transaction must call the function directly.

For columns where synchronous refresh is too costly — very expensive computation — an asynchronous queued mode leaves dirty rows after commit for a background process to drain independently.

The system also enforces that stored computed columns are never editable: Hibernate mapping suppresses writes at the DAL layer, `AD_Field.ReadOnlyLogic` is set automatically, the Schema Forge pipeline promotes them to read-only in every generated UI, and a validation rule in the pipeline blocks misconfiguration.

### 4.2 How It Works

The following describes the full lifecycle of a stored computed column value from a business write through to the stored result.

**Step 1 — Normal business DML (during transaction)**

Application code writes through DAL as normal. Hibernate never includes stored computed columns in any DML — they have no setter (`insert="false" update="false"`).

**Step 2 — Dirty collection (AFTER triggers, still within the transaction)**

AFTER triggers on each declared source table fire immediately after each DML statement. Each trigger covers only the event(s) in its dependency row and performs no event-type check. Each trigger:

1. For update triggers, checks whether any of the `Watched_Columns` changed. If none changed, returns immediately.
2. Runs `Target_ID_Resolver_SQL` to find the affected target record IDs.
3. Inserts one row per ID into `AD_StoredColumn_Dirty` using `INSERT … ON CONFLICT DO NOTHING`, recording `Transaction_ID = pg_current_xact_id()::bigint` alongside the target ID. Duplicate entries — from multiple statements affecting the same target within the same transaction — are silently discarded by the unique constraint on `(AD_Column_ID, Target_Record_ID, Transaction_ID)`.

**Step 3 — Deferred recalculation (end of transaction, synchronous mode only)**

A `DEFERRABLE INITIALLY DEFERRED` constraint trigger on `AD_StoredColumn_Dirty` fires for each inserted dirty row, deferred until just before commit. The trigger fires once per inserted row, but the first invocation processes and deletes all dirty rows for the current transaction in a single pass; subsequent invocations (from other dirty-row inserts within the same deferred phase) find no remaining rows and return immediately. The trigger function:

1. Sets the session flag `SET LOCAL "sf.refreshing" = 'true'` to prevent dependency triggers from firing during the recalculation pass.
2. Selects all dirty rows where `Refresh_Mode = S` and `Transaction_ID = pg_current_xact_id()::bigint`, ordered by `Computation_Sequence_Number` ascending — this ensures the trigger processes only the rows that belong to the current transaction, and that columns with lower sequence numbers refresh before those that may depend on them.
3. Calls the computation function once per row and writes the result only when it differs from the current stored value (`IS DISTINCT FROM`). Audit columns are not touched.
4. Deletes all processed dirty rows.

If the computation function raises an error, the entire transaction rolls back.

**Step 4 — Async recalculation (queued mode, outside the originating transaction)**

For `Refresh_Mode = Q` columns, dirty rows persist after commit. A background process claims batches with `SKIP LOCKED`, calls the computation function, writes results, and deletes processed rows. The stored column lags by the scheduler interval.

**Step 5 — Manual rebuild**

Operators call `sf_rebuild(<column_id>)` to recompute all rows — used for initial population, post-migration repair, or after a function bug is fixed.

### 4.3 Data Model and Structure

Changes to `AD_Column` and the new tables are included in the AD dataset.

#### AD_Column — new fields

| Field                         | Type    | Values / Notes                                                                                                                                                       |
| ----------------------------- | ------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Computation_Mode`            | CHAR(1) | `N` normal (default), `V` virtual/SQLLOGIC (existing), `S` stored computed                                                                                           |
| `Computation_Function`        | VARCHAR | Fully qualified SQL function name; required when `Computation_Mode = S`                                                                                              |
| `Refresh_Mode`                | CHAR(1) | `S` synchronous end-of-transaction, `Q` queued async, `M` manual                                                                                                     |
| `Computation_Sequence_Number` | INTEGER | Global refresh order. Default `10` for `S` columns, `0` otherwise. Must be at least 10 greater than the sequence number of any stored computed column it depends on. |

#### AD_Column_Computation_Dependency — new table

Declares which source tables and events trigger a refresh of a stored computed column.

| Field                                 | Type        | Description                                                                                                                      |
| ------------------------------------- | ----------- | -------------------------------------------------------------------------------------------------------------------------------- |
| `AD_Column_Computation_Dependency_ID` | VARCHAR(32) | Primary key                                                                                                                      |
| `AD_Column_ID`                        | VARCHAR(32) | FK → `AD_Column` (the stored computed column)                                                                                    |
| `AD_Module_ID`                        | VARCHAR(32) | FK → `AD_Module`; the module that declares this dependency row, used for dataset export/import via DB Source Manager — same pattern as all other AD dictionary tables |
| `SeqNo`                               | INTEGER     | Row ordering within a stored column's dependency set; used in generated trigger object names (`sf_dep_fn_<column_id>_<seqno>`) |
| `Source_Table_ID`                     | VARCHAR(32) | FK → `AD_Table`; the table whose changes trigger a refresh                                                                       |
| `Insert_Event`                        | BOOLEAN     | Fire on INSERT into the source table                                                                                             |
| `Update_Event`                        | BOOLEAN     | Fire on UPDATE of the source table                                                                                               |
| `Delete_Event`                        | BOOLEAN     | Fire on DELETE from the source table                                                                                             |
| `Watched_Columns`                     | TEXT        | Comma-separated `AD_Column_ID` values; update trigger fires only when one of these changes. Required when `Update_Event = true`. |
| `Target_ID_Resolver_SQL`              | TEXT        | SQL expression resolving affected target record IDs from the source row. Must never return NULL.                                 |

#### AD_StoredColumn_Dirty — new table

The shared dirty-tracking table. Each row represents one target record that needs recalculation for one stored computed column.

| Field                         | Type        | Mandatory | Description                                                                                                                                                                          |
| ----------------------------- | ----------- | --------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `AD_StoredColumn_Dirty_ID`    | VARCHAR(32) | Yes       | Primary key (UUID)                                                                                                                                                                   |
| `AD_Column_ID`                | VARCHAR(32) | Yes       | FK → `AD_Column`; identifies which stored column is dirty                                                                                                                            |
| `Target_Record_ID`            | VARCHAR(32) | No        | Target record ID. `NULL` is a sentinel meaning "recompute all records" (initial population or bulk reset)                                                                            |
| `Transaction_ID`              | BIGINT      | No        | ID of the transaction that created this row. Set to `pg_current_xact_id()::bigint` on PostgreSQL; `NULL` on Oracle (not applicable in async-only mode). Allows the deferred pass to select only its own rows and is useful for debugging. |
| `Refresh_Mode`                | CHAR(1)     | Yes       | Copied from `AD_Column.Refresh_Mode` at insert time                                                                                                                                  |
| `Computation_Sequence_Number` | INTEGER     | Yes       | Copied from `AD_Column` at insert time; drives deferred pass ordering                                                                                                                |
| `Created`                     | TIMESTAMP   | Yes       | Insertion time; drives FIFO ordering for the async queue                                                                                                                             |

**Deduplication constraints:**

- `UNIQUE (AD_Column_ID, Target_Record_ID, Transaction_ID)` — duplicate dirty events within the same transaction are dropped via `INSERT … ON CONFLICT DO NOTHING`. On Oracle, where `Transaction_ID` is `NULL`, deduplication is handled via `MERGE` in the PL/SQL trigger.
- `UNIQUE (AD_Column_ID, Transaction_ID) WHERE Target_Record_ID IS NULL` — at most one "process all" sentinel per column per transaction. A separate partial index is required because `NULL` values are not equal in standard unique constraints.

Dependency triggers always resolve concrete target IDs and never insert the null sentinel. Only the initial population mechanism does.

**Indexes:**

```sql
-- Covers deferred pass reads, filtered to current transaction and ordered by sequence number
CREATE INDEX ad_storedcolumn_dirty_sync
    ON AD_StoredColumn_Dirty (Transaction_ID, Computation_Sequence_Number)
    WHERE Refresh_Mode = 'S';

-- Covers async queue reads, ordered by insertion time
CREATE INDEX ad_storedcolumn_dirty_queue
    ON AD_StoredColumn_Dirty (Created)
    WHERE Refresh_Mode = 'Q';
```

### 4.4 UI and API Enforcement

Read-only enforcement is applied at every layer — no single layer is trusted as the sole gatekeeper.

**DAL layer:** `insert="false" update="false"`, no setter; the column never appears in `save()` SQL.

**AD_Field:** Every `AD_Field` backed by `Computation_Mode = S` gets `ReadOnlyLogic = 'Y'` — set by the Gradle task (all tabs/windows), a callout (at field-creation time), and an `@OBDALEventHandler` (every save, including programmatic).

**Schema Forge pipeline:** `resolve-curated.js` forces visibility to `readOnly` regardless of `decisions.json`. `push-to-neo.js` sets `Is_ReadOnly = true` in `ETGO_SF_FIELD`. `validate-pipeline.js` (rule F11) blocks any `contract.json` field with `readOnly: false` backed by a stored computed column.

**Generated React components:** `generate-frontend.js` emits stored computed fields as display-only — never as `<input>` or `<select>`.

---

## 5. Technical Design

### 5.1 DAL Mapping Changes

**`src/org/openbravo/base/model/Column.java`**

Add four fields populated from the new `AD_Column` columns during model bootstrap:

- `computationMode` (char)
- `computationFunction` (String)
- `refreshMode` (char)
- `computationSequenceNumber` (int)

**`src/org/openbravo/base/model/Property.java`**

- `initializeFromColumn(Column)` — copy the new fields from `Column`.
- `isStoredComputed()` — returns `true` when `computationMode == 'S'`.

**`src/org/openbravo/dal/core/DalMappingGenerator.java`**

Extend `generatePrimitiveMapping(Property)` to emit `insert="false" update="false"` for stored computed properties:

```java
if (p.isInactive() || p.getEntity().isView()
        || p.getSqlLogic() != null
        || p.isStoredComputed()) {           // new
    sb.append(" update=\"false\" insert=\"false\"");
}
```

**`src/org/openbravo/base/gen/entity.ftl`**

Suppress the generated setter for stored computed properties; accidental writes are caught at compile time.

### 5.2 Trigger Generation

#### Generated Database Objects

For each stored computed column the system generates and owns three categories of database object.

**1. Dependency trigger functions and triggers** (one set per `AD_Column_Computation_Dependency` row)

```
sf_dep_fn_<column_id>_<seqno>()   -- PL/pgSQL trigger function
sf_dep_<column_id>_<seqno>        -- AFTER INSERT OR UPDATE OR DELETE on source table
```

Each trigger is declared for the specific event(s) configured in the dependency row — the trigger function performs no event-type check. For update triggers, the function checks whether any `Watched_Columns` changed and returns immediately if none did. Otherwise it runs `Target_ID_Resolver_SQL` to find affected target IDs and inserts rows into `AD_StoredColumn_Dirty` via `INSERT … ON CONFLICT DO NOTHING`, setting `Transaction_ID = pg_current_xact_id()::bigint` on each inserted row. A PostgreSQL session-level variable (`sf.refreshing`) prevents these triggers from firing during the deferred recalculation pass: each dependency trigger function checks `current_setting('sf.refreshing', true)` at entry and returns immediately if the flag is set. The deferred pass sets it with `SET LOCAL "sf.refreshing" = 'true'` before beginning recalculation; `SET LOCAL` automatically clears the flag at the end of the transaction.

**2. Deferred recalculation trigger function and trigger** (one per stored computed column)

```
sf_deferred_fn_<column_id>()      -- PL/pgSQL trigger function
sf_deferred_<column_id>           -- CONSTRAINT AFTER INSERT ON ad_storedcolumn_dirty
                                  --   DEFERRABLE INITIALLY DEFERRED
```

This fires on `AD_StoredColumn_Dirty` itself as a deferred constraint trigger. It executes the end-of-transaction recalculation pass described in section 4.2, Step 3.

**3. Rebuild and consistency-check functions** (generic, not generated per column)

```
sf_rebuild(p_column_id VARCHAR)   -- idempotent full rebuild for the given column
sf_check(p_column_id VARCHAR)     -- returns count of stale rows for the given column
```

These are static functions shipped as part of core. They look up the computation function name and target table dynamically from `AD_Column` metadata and execute the rebuild or check via dynamic SQL. No per-column variant is generated.

All generated objects carry a comment block containing the `AD_Column_ID`, generator version, and a SHA-256 hash of the dependency metadata used to generate them:

```sql
-- sf:generated=true
-- sf:column_id=<AD_Column_ID>
-- sf:generator_version=1
-- sf:metadata_hash=<SHA-256 of dependency rows + function name + target column>
CREATE OR REPLACE FUNCTION sf_dep_fn_<column_id>_<seqno>() …
```

#### Gradle Task: `generateStoredComputedTriggers`

```bash
./gradlew generateStoredComputedTriggers
./gradlew generateStoredComputedTriggers -Pcolumn=<AD_Column_ID>   # single column
./gradlew generateStoredComputedTriggers -Pincremental=true        # default
./gradlew generateStoredComputedTriggers -Pforce=true              # bypass hash check
```

Task steps:

1. **Load metadata** — query `AD_Column` and `AD_Column_Computation_Dependency` for all columns where `Computation_Mode = S`.
2. **Validate** — run all hard validation checks before any DDL (see section 3.2). Abort on first failure per column.
3. **Generate SQL** — render trigger function bodies and trigger DDL from templates. The generic `sf_rebuild` and `sf_check` functions are part of core and are not generated here.
4. **Compute hash** — SHA-256 of the rendered SQL per column.
5. **Incremental check** — read the hash from the deployed object's comment block. Skip DDL for a column if the hash matches. In force mode, skip the check.
6. **Apply DDL** — for changed or new columns: `DROP FUNCTION IF EXISTS … CASCADE`, then `CREATE OR REPLACE FUNCTION`, then `CREATE CONSTRAINT TRIGGER`. Apply in `SeqNo` order within each column.
7. **Set AD_Field read-only** — run the `ReadOnlyLogic = 'Y'` propagation SQL for all affected fields.
8. **Update export** — write updated trigger XML to the module's `src-db/` directory.
9. **Initial population** — for columns being activated for the first time (no existing generated objects): call `sf_rebuild(<column_id>)` directly for `Refresh_Mode = S`; insert a null sentinel into `AD_StoredColumn_Dirty` for `Refresh_Mode = Q`; do nothing for `Refresh_Mode = M`.
10. **Report** — print a summary of columns processed, skipped, created, updated, and dropped.

The task runs automatically as part of `update.database` after the standard schema application step. It also runs as a pre-commit hook: if any trigger in `src-db/` is stale relative to the current AD metadata hash, the commit is rejected.

#### Incremental Update Logic

The hash in each generated object's comment is the staleness signal — derived from live AD metadata, never from source files. Any change to `Watched_Columns`, `Target_ID_Resolver_SQL`, or the dependency set invalidates the hash and regenerates all triggers for that column (all-or-nothing per column). Manual edits to generated database objects are overwritten on the next run — intentionally.

### 5.3 DB Source Manager Integration

The generated triggers (`sf_dep_*`, `sf_deferred_*`) are managed exclusively by the `generateStoredComputedTriggers` Gradle task and must not be touched by DB Source Manager during `export.database` or `update.database` runs. This is achieved by adding the generated trigger name patterns to the dbsm exclude filter configuration, so dbsm ignores them entirely.

### 5.4 Schema Forge Pipeline Changes

**`resolve-curated.js`**

Force visibility to `readOnly` for any field with `Computation_Mode = S`, regardless of `decisions.json` classification.

**`push-to-neo.js`**

Set `Is_ReadOnly = true` in `ETGO_SF_FIELD` for any `Computation_Mode = S` column, overriding all other configuration.

**`generate-frontend.js`**

Emit stored computed fields as display-only in `HeaderForm.jsx` and line-form components — never as `<input>` or `<select>`.

**`validate-pipeline.js` — new rule**

Rule F11: flag any field in `contract.json` backed by `Computation_Mode = S` with `readOnly: false` or `visibility: editable`.

### 5.5 Asynchronous Queue Processor

The async process request handles `Refresh_Mode = Q` columns outside the originating transaction.

**Parameters:**

| Parameter     | Type    | Default | Description                                 |
| ------------- | ------- | ------- | ------------------------------------------- |
| `Max Records` | INTEGER | 100     | Maximum dirty rows to process per execution |

**Execution per invocation:**

1. Claim up to `Max Records` rows from `AD_StoredColumn_Dirty` where `Refresh_Mode = Q`, ordered by `Computation_Sequence_Number` ascending, using `SELECT … FOR UPDATE SKIP LOCKED`.
2. Handle sentinel rows (`Target_Record_ID IS NULL`) first: call `sf_rebuild(<column_id>)` for each, then delete the sentinel and remove any individual dirty rows for the same column from the batch.
3. Group remaining rows by `AD_Column_ID` and process in `Created` order within each group.
4. For each row, call the computation function and write the result only when it differs from the current stored value. Set the `sf.refreshing` session flag to suppress dependency triggers.
5. Delete each processed row immediately after its value is written.
6. Commit. If the computation function raises an error for any row, the entire batch transaction rolls back and dirty rows remain for the next run.

Multiple instances may run concurrently. `SKIP LOCKED` ensures each instance claims a disjoint batch; no target record is computed simultaneously by two instances. Note that different transactions can each produce a dirty row for the same `(AD_Column_ID, Target_Record_ID)` — the unique constraint only prevents within-transaction duplicates (where `Transaction_ID` is the same). The async queue may therefore contain multiple rows for the same target record from different transactions; the processor will compute the target once per row, which is correct (idempotent) but wasteful. A deduplication step during batch claiming (`SELECT DISTINCT ON (AD_Column_ID, Target_Record_ID)`) could reduce redundant work in high-throughput scenarios but is deferred to post-MVP.

---

## 6. Oracle Support

The synchronous refresh mechanism described in section 4.2 is PostgreSQL-specific by design and cannot be implemented on Oracle. The two fundamental blockers are:

**Deferred constraint triggers do not exist in Oracle.** The entire end-of-transaction recalculation pass relies on a `DEFERRABLE INITIALLY DEFERRED` constraint trigger that fires once at commit time on `AD_StoredColumn_Dirty`. Oracle supports deferred *constraints* but not deferred *trigger firing* on arbitrary tables. There is no Oracle mechanism to replicate this behavior without changing the architecture.

**Transaction ID tracking is not available.** `pg_current_xact_id()` has no Oracle equivalent. The `Transaction_ID` column is nullable for this reason — Oracle triggers leave it `NULL`. This is not a blocker since Oracle does not use the deferred constraint trigger (which is the only reader of `Transaction_ID`).

The synchronous path also relies on several other PostgreSQL-specific features — PL/pgSQL, `INSERT … ON CONFLICT DO NOTHING`, partial indexes, `IS DISTINCT FROM`, `SET LOCAL` session variables, and `pg_proc` catalog queries — none of which have direct Oracle equivalents.

### Oracle mode: asynchronous recomputation only

On Oracle, stored computed columns operate exclusively in `Refresh_Mode = Q` (queued async). The synchronous end-of-transaction refresh (`Refresh_Mode = S`) is not supported.

This means:

- The AD model and DAL mapping changes (`insert/update="false"`, suppressed setter) are identical on both databases.
- Dependency triggers on Oracle are written in PL/SQL and use `MERGE` for deduplication instead of `ON CONFLICT DO NOTHING`.
- The deferred constraint trigger is not created on Oracle; dirty rows are always left for the async queue processor.
- The async process request (Java-based) works on both databases without modification.
- `Refresh_Mode = S` is rejected at validation time on Oracle; `Q` and `M` are both supported.
- Stored columns on Oracle are eventually consistent — reflecting committed data as of the last queue processor run.

The AD field configuration UI must warn about eventual consistency when `Refresh_Mode = Q` is selected on an Oracle installation.

---

## 7. Test Setup and Approaches

### 7.1 Test Infrastructure

Tests require a running PostgreSQL instance with the full Etendo schema. A dedicated test module provides:

- `SF_TEST_COMPUTED_TARGET` — target table with multiple stored computed columns in a declared sequence order.
- `sf_compute_test_value(p_id VARCHAR)` — deterministic computation function.
- `SF_TEST_COMPUTED_SOURCE` — source table whose changes trigger refresh, with dependency rows covering insert, update, and delete events.

`generateStoredComputedTriggers` runs against the test module before the suite; tables are truncated between cases.

### 7.2 Unit Tests

**DAL mapping (no database required):**

- A stored computed property returns `true` from `Property.isStoredComputed()`.
- `DalMappingGenerator` emits `insert="false" update="false"` in the Hibernate XML for a stored computed property and does not emit a `formula` attribute.
- The generated entity class has no setter for a stored computed property (verified by reflection or by asserting the generated source does not contain `set<PropertyName>`).
- A DAL `save()` on an entity with a stored computed property does not include that column in the emitted SQL (verified by inspecting the Hibernate SQL interceptor output).

**Validation logic (no database required):**

- Each hard failure mode (missing physical column, missing function, SQLLOGIC conflict, missing dependencies, missing watched columns, cycle detection) produces a distinct error message and prevents any DDL.
- A valid column passes all checks without error.
- Cycle detection correctly identifies a two-column cycle and a three-column cycle.

### 7.3 Integration Tests

Each test case runs within its own database transaction that is rolled back after the assertion, except for tests that specifically verify post-commit state.

| Scenario                                         | What to verify                                                                                                                                       |
| ------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| Insert into source table                         | Dirty row inserted for the target; deferred pass writes the correct stored value after commit.                                                       |
| Update a watched column in source table          | Dirty row inserted; stored value updated.                                                                                                            |
| Update a non-watched column in source table      | No dirty row inserted; stored value unchanged.                                                                                                       |
| Delete from source table                         | Dirty row inserted (if `Delete_Event = true`); stored value updated or cleared.                                                                      |
| Bulk update of N source rows                     | Exactly N distinct dirty rows in `AD_StoredColumn_Dirty` after all statements; exactly N stored value writes after the deferred pass.                |
| Stored value already current                     | Dirty row collected; deferred pass calls function but does not issue an UPDATE (verified via `IS DISTINCT FROM` guard).                              |
| Computation function raises an error             | Entire transaction rolls back; dirty rows do not persist.                                                                                            |
| Session flag guard                               | The deferred pass does not re-trigger dependency triggers (no recursive dirty rows).                                                                 |
| Deferred pass ordering                           | A column with `Computation_Sequence_Number = 20` that depends on one with `10` is refreshed after the lower-numbered column in the same transaction. |
| Rebuild function                                 | Manually corrupt a stored value; call `sf_rebuild(<column_id>)`; verify the value is corrected.                                                      |
| Consistency check                                | Manually corrupt a stored value; call `sf_check(<column_id>)`; verify it returns a non-zero count.                                                   |

### 7.4 Async Queue Tests

- A `Refresh_Mode = Q` column accumulates dirty rows after a business transaction without processing them synchronously.
- The background processor claims a batch, writes results, and deletes processed rows.
- A null sentinel row triggers a full rebuild via `sf_rebuild(<column_id>)` and is deleted after completion.
- Individual dirty rows for the same column as a sentinel are skipped (already covered by the rebuild).
- Two concurrent process request instances claim disjoint row sets; no target record is computed twice.
- The consistency check detects values that were not refreshed by the queue processor.

### 7.5 Schema Forge Pipeline Tests

- `resolve-curated.js` promotes a field with `Computation_Mode = S` from `editable` to `readOnly` in the resolved curated schema, regardless of the `decisions.json` classification.
- `push-to-neo.js` writes `Is_ReadOnly = true` to `ETGO_SF_FIELD` for a stored computed field, overriding any other value.
- `validate-pipeline.js` rule F11 fails when `contract.json` declares a stored computed field with `readOnly: false`.
- A generated `HeaderForm.jsx` renders a stored computed field as a display-only component with no interactive input.

### 7.6 Performance Baseline

Before and after migrating the pilot column from virtual to stored computed:

- Measure grid query time over a representative result set with filtering and sorting on the computed column.
- Measure single-record save time with and without a stored computed column in the dependency chain.
- Confirm the deferred pass duration scales linearly with distinct dirty rows, not with DML statement count.

---

## 8. MVP Scope

The MVP proves the end-to-end mechanism for the synchronous refresh path with a single pilot column. It excludes async queuing, advanced static analysis, DB Source Manager automation, and incremental trigger management.

### Effort Estimate (AI-assisted development)

Estimates assume Claude Code generation for boilerplate, SQL templates, and test scaffolding, with human review and iterative correctness testing. One person-day = one developer using AI assistance full-time on the task.

| Phase | Key work | Days |
|---|---|---|
| 1 — Application Dictionary and Data Model | 4 new AD_Column fields, 2 new tables with indexes, AD window/tab setup, export + `update.database` verification | 2 |
| 2 — Read-Only Hibernate Mapping | 4 targeted Java file changes, 1 unit test | 0.5 |
| 3 — Trigger Generation and Deferred Refresh | Dependency trigger template, deferred constraint trigger (Transaction_ID scoping + IS DISTINCT FROM), session flag, Gradle task, generic sf_rebuild/sf_check, AD_Field enforcement (callout + EventHandler), pilot column, integration tests | 6–8 |
| 4 — Hard Validation | Physical column, function existence + signature + volatility checks (pg_proc introspection), cycle detection, test fixtures | 2–3 |
| **Total** | | **10.5–13.5 days** |

Phase 3 carries the most risk. The deferred constraint trigger is conceptually simple but correctness under concurrency — Transaction_ID scoping, IS DISTINCT FROM guard — requires careful iterative testing that AI generation cannot fully replace. The upper bound applies if the trigger needs more than one design iteration.

### In Scope

- `AD_Column` extended with `Computation_Mode`, `Computation_Function`, `Refresh_Mode` (`S` and `M` only; `Q` excluded).
- `AD_Column_Computation_Dependency` table with all fields including `SeqNo`.
- `AD_StoredColumn_Dirty` table with UUID primary key, nullable `Target_Record_ID`, mandatory `Refresh_Mode` and `Created`; unique constraint on `(AD_Column_ID, Target_Record_ID, Transaction_ID)`; partial unique index for the null sentinel; sync partial index on `(Transaction_ID, Computation_Sequence_Number)`.
- AD windows and list references so new fields are editable in the dictionary UI.
- Dependency AFTER triggers per dependency row: insert dirty rows, `INSERT … ON CONFLICT DO NOTHING`, session flag guard.
- Deferred recalculation constraint trigger (`DEFERRABLE INITIALLY DEFERRED`) on `AD_StoredColumn_Dirty`: ordered by `Computation_Sequence_Number`, `IS DISTINCT FROM` write guard, no audit-column updates.
- Hard validation failures only: missing physical column, missing function, missing dependencies for synchronous mode, missing watched columns on update events, cycle detection.
- Read-only Hibernate mapping: `Column.isStoredComputed()`, `Property.isStoredComputed()`, `DalMappingGenerator` emits `insert="false" update="false"`, `entity.ftl` suppresses setter.
- `AD_Field.ReadOnlyLogic = 'Y'` propagation via Gradle task, callout, and `@OBDALEventHandler`.
- Generic `sf_rebuild(<column_id>)` and `sf_check(<column_id>)` functions shipped as part of core and callable manually.
- Initial population for `Refresh_Mode = S` via direct `sf_rebuild(<column_id>)` call in the Gradle task.
- Trigger creation in force mode (no incremental hash check).
- One pilot column migrated from virtual to stored computed, with before/after validation.
- Unit tests for DAL mapping and hard validation.
- Integration tests for insert, update, delete, bulk update, and rebuild.

### Excluded from MVP

- `Refresh_Mode = Q` and the background queue processor.
- Consistency-check function and scheduled health checks.
- Incremental trigger regeneration (hash comparison).
- Pre-commit hook for trigger staleness.
- EXPLAIN-based sequential scan warnings.
- Index validation for filter/sort columns.
- `Refresh_Group` batching logic.
- Schema Forge pipeline changes (`resolve-curated.js`, `push-to-neo.js`, `generate-frontend.js`, `validate-pipeline.js` rule F11).

---

## 9. Open Questions

- What level of SQL function static analysis (EXPLAIN, type inference) is practical to implement in the first iteration without introducing significant build-time complexity? Or how easy can it be done to analyze/read EXPLAIN results during build time (use llm/ai?)?
- **_TBD_** Should missing indexes for declared filter/sort columns be a hard validation failure or a warning with an explicit developer override?
- **_TBD_** how to detect fan-out?
- Should stored computed columns be allowed on audit-sensitive tables by default, or should this require an explicit opt-in flag?
- Should `Computation_Sequence_Number` be editable by module authors, or should the system auto-assign it from dependency graph analysis and expose it as read-only?
- Is explicit race condition handling between concurrent transactions refreshing the same target record necessary? Each transaction computes from its own snapshot and writes only when the value differs (`IS DISTINCT FROM`), so the last writer naturally wins and no value is lost. The risk of a briefly stale stored value between two concurrent commits is likely acceptable for computed display data. Is the added complexity of `SELECT FOR UPDATE` with deterministic ordering justified for this use case?

---

## 10. Risks and Mitigations

| Risk                                                                                                                                                                                       | Mitigation                                                                                                                                                                                                                                                                                                                                                                             |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Write overhead at commit time.** The deferred pass adds work just before commit on every transaction that touches a dependency source.                                                   | Explicit dependency declarations with watched-column filters minimize unnecessary dirty collection. Dirty-set deduplication limits recalculation to one pass per target row regardless of statement count.                                                                                                                                                                             |
| **Stale data from missing dependency declarations.** If a source table is not declared as a dependency, changes to it will not trigger a refresh.                                          | Mandatory dependency metadata enforced at validation time. The generic `sf_check(<column_id>)` function enables scheduled health checks to detect drift. `sf_rebuild(<column_id>)` provides repair.                                                                                                                                                                                    |
| **Recursive trigger loops.** The deferred refresh writes to the target table, which could re-trigger the dependency triggers and produce an infinite loop.                                 | A session-level flag (`sf.refreshing`) is set before the deferred pass begins. All generated dependency triggers check this flag and return immediately if set.                                                                                                                                                                                                                        |
| **Computation function errors rolling back business transactions.** A function that raises an error will roll back the entire transaction, including all business DML in that transaction. | `IMMUTABLE` or `STABLE` volatility is required and validated at compilation. Function errors are deployment blockers — operators can switch to `Refresh_Mode = M` to unblock while fixing. |
| **Function side effects corrupting the two-phase model.** A computation function that modifies data breaks the assumption that the deferred pass is a pure read-then-write step.           | Compilation validation checks for `VOLATILE` volatility and rejects it. Documentation and code review guidelines reinforce the restriction.                                                                                                                                                                                                                                            |

---

## 11. Implementation Phases

### Phase 1 — Application Dictionary and Data Model

- Add `Computation_Mode`, `Computation_Function`, `Refresh_Mode`, `Computation_Sequence_Number` to `AD_Column`.
- Create `AD_Column_Computation_Dependency` with all fields including `SeqNo`.
- Create `AD_StoredColumn_Dirty`: UUID PK, nullable `Target_Record_ID`, mandatory `Refresh_Mode`, `Computation_Sequence_Number`, `Created`; unique constraint on `(AD_Column_ID, Target_Record_ID, Transaction_ID)`; partial unique index for the null sentinel; sync and queue partial indexes.
- Add AD windows and list references for the new fields.
- Export and run `update.database` clean.

**Exit:** new tables and columns exist, are exportable, and pass `update.database`.

---

### Phase 2 — Read-Only Hibernate Mapping

- Add `computationMode`, `computationFunction`, `refreshMode`, `computationSequenceNumber` to `Column.java`; populate during model bootstrap.
- Add `isStoredComputed()` to `Property.java`; wire from `initializeFromColumn`.
- Extend `DalMappingGenerator.generatePrimitiveMapping` to emit `insert="false" update="false"` when `isStoredComputed()`.
- Suppress generated setter in `entity.ftl`.

**Exit:** DAL `save()` on a stored computed column emits no SQL for that column.

---

### Phase 3 — Trigger Generation and Deferred Refresh

Largest phase — delivers the working end-to-end synchronous refresh.

- Implement `generateStoredComputedTriggers` in force mode: dependency trigger functions/triggers and the deferred recalculation trigger.
- Embed metadata comment block (including hash) in each generated object.
- Implement `sf.refreshing` session flag to suppress dependency triggers during the deferred pass.
- Ship `sf_rebuild` and `sf_check` as core functions.
- Integrate the task into `update.database` after standard schema application.
- Implement `AD_Field.ReadOnlyLogic = 'Y'` propagation: Gradle task SQL step, callout on `AD_Column_ID`, and `@OBDALEventHandler` on `AD_Field`.
- Migrate the pilot column end-to-end: function, dependencies, task run, value verification.
- Integration tests: insert, update (watched and non-watched), delete, bulk update, recursive loop guard.

**Exit:** pilot column passes all integration tests; no spurious writes; no recursive loops.

---

### Phase 4 — Hard Validation

- Implement all hard failure checks (section 3.2) in `generateStoredComputedTriggers` or a standalone `ValidateStoredColumns` task.
- Errors print a clear message per column and abort before any DDL.
- Test fixtures: one valid column, one per failure mode.

**Exit:** each failure mode has a test confirming no DDL is applied.

---

### Phase 5 — Incremental Trigger Regeneration

- Compare AD-derived SHA-256 hash against deployed object comment; skip DDL when equal.
- Add `-Pforce=true` flag to bypass the check.
- Add drop logic for columns no longer marked `Computation_Mode = S`.
- Add pre-commit hook: reject commits where any trigger in `src-db/` is stale.

**Exit:** changing `Watched_Columns` regenerates only that column's triggers; unchanged columns are skipped.

---

### Phase 6 — Queued Refresh and Consistency Checks

- `Refresh_Mode = Q` in dependency triggers: leave dirty rows after commit.
- Initial population for `Q` in `generateStoredComputedTriggers`: insert null sentinel on first activation.
- Async process request: `SKIP LOCKED` batch claim, sentinel rows via `sf_rebuild`, compute/write/delete individual rows.
- Expose consistency check as `checkStoredColumns` Gradle task and scheduled background process.
- Schema Forge pipeline changes: `resolve-curated.js`, `push-to-neo.js`, `generate-frontend.js`, `validate-pipeline.js` rule F11.

**Exit:** `Refresh_Mode = Q` column accumulates dirty rows, background processor resolves them, consistency check detects corrupted values.

---

### Phase 5b — Build-time definition validation

- Implement `StoredComputedValidator` (`src-util/modulescript/`) with rules V1–V16 (§3.6), shared pure `checkShape`/`findCycles` used by both the build gate and the runtime DAL observer.
- Run it on every `update.database` via the `ValidateStoredComputedColumns` ModuleScript, and re-run it as Gate 0 (definitions) + Gate 1 (deployment drift) inside `GenerateStoredComputedTriggers`.
- Collect all violations into one aggregated report and throw a single `BuildException`; hard failures abort before any DDL.
- Add the `ETGO_SCD_VALIDATION` toggle (`enforce` default / `warn` escape hatch), resolved from JVM system property then environment variable.
- Reuse only the two existing `AD_MESSAGE` keys (`ETGO_StoredComputedColDef`, `ETGO_CompDepTargetXor`); all other codes are build-only constants — no new AD messages or UUIDs.

**Exit:** each hard rule (V1–V16) has a fixture confirming the build aborts with the correct code; `warn` mode logs the same findings without aborting; the DAL observer and the build gate agree on V1–V3 and V14 via the shared pure methods.
