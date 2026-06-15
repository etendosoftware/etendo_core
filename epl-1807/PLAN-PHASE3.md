# EPL-1807 — Phase 3 Plan: Deferred Recalculation Engine

Branch: `epic/ETP-3504`. Do not push.

## Context

Phase 1 added the `AD_COLUMN_COMP_DEPENDENCY` metadata table and the
`AD_STOREDCOLUMN_DIRTY` queue table. Phase 2 extended the ORM layer to treat
`Computation_Mode='S'` columns as read-only. Phase 3 (enqueue half, already
committed) generates per-dependency `AFTER` triggers that write dirty rows into
`AD_STOREDCOLUMN_DIRTY`.

This plan covers the **remainder of Phase 3**: the half that *consumes* the dirty
rows (deferred recalculation at end of transaction) plus read-only enforcement,
dbsm integration, the pilot, build wiring, and tests.

## Locked decisions

1. **Naming:** all objects live under the `ad_scd_` namespace (no `sf_` realign).
2. **Watched-column guard:** add an `IS DISTINCT FROM` value check in the enqueue
   function (not just `AFTER UPDATE OF <cols>`).
3. **Incremental / hash regeneration:** out of scope → Phase 5. Phase 3 keeps the
   unconditional full rebuild on every `update.database`.
4. **Pilot:** lives in module `com.etendo.test` (javapackage `com.etendo.test`,
   id `FF33B6827BAC422CA69953AD9899B933`, DB prefix `ETTST`), not core.

## Design decision: one generic deferred trigger, not per-column

Nothing about the deferred pass is column-specific **in code** — the only
per-column facts (`computation_function`, target table/column/PK) are **data** in
`AD_COLUMN`. So the deferred side is **not generated per column**. A single static
constraint trigger drives a generic processor that looks everything up from
metadata.

A `DEFERRABLE` constraint trigger **must** be `FOR EACH ROW` (Postgres forbids
`FOR EACH STATEMENT` on constraint triggers). So a txn that enqueues N dirty rows
fires the deferred trigger N times **at commit, sequentially within the single
backend process** — there is no intra-transaction parallelism. The first firing
claims the drain via the `my.scd_refreshing` GUC and processes the whole queue for
the transaction; firings 2..N see the flag set and return immediately.

**Known limitation (acceptable for Phase 3):** the shared `my.scd_refreshing` flag
suppresses enqueues during the drain, so a multi-level chain (column B depends on
column A's *target* table) will not auto-cascade within the same commit. The Phase 3
pilot is single-level. True chaining is a later phase. §11's "recursive guard"
requirement (prevent infinite loops) is satisfied for free by this flag.

## Workstreams

### A. Engine functions — committed core, `src-db/database/model/functions/`

| Function | Role |
|---|---|
| `ad_scd_recompute(p_column_id varchar, p_target_id varchar)` | Recompute ONE target row. Looks up target table/col/PK + `computation_function` from `AD_COLUMN`. Takes a `FOR UPDATE` row lock on the target (see concurrency note), then `UPDATE … SET col = fn(target) WHERE col IS DISTINCT FROM fn(target)`. |
| `ad_scd_process_dirty()` | Drain orchestrator. Guarded by `my.scd_refreshing`; sets it; loops dirty rows for `transaction_id = pg_current_xact_id()::text::bigint AND refresh_mode='S'` `ORDER BY computation_sequence_number, target_record_id`; calls `ad_scd_recompute`; deletes processed rows. |
| `ad_scd_rebuild(p_column_id varchar)` | Full idempotent rebuild — loop `ad_scd_recompute` over all target rows of the column. |
| `ad_scd_check(p_column_id varchar) returns integer` | Count of stale rows (where stored value `IS DISTINCT FROM` recomputed value). |

`my.scd_refreshing` follows the existing `my.triggers_disabled` GUC convention
(`current_setting('my.scd_refreshing', true)`, `SET LOCAL` so it clears at txn end).

**Concurrency (cross-transaction):** two concurrent txns touching different child
rows of the same target both recompute `fn(target)` and `UPDATE` the same target
row. Under READ COMMITTED this risks a lost aggregate. `ad_scd_recompute` therefore
takes `SELECT 1 FROM <target> WHERE <pk> = p_target_id FOR UPDATE` **before**
computing, so the second drain blocks until the first commits, then re-reads and
recomputes correctly. Contends only when two txns hit the same aggregate.

### B. Static deferred trigger — committed core

`ad_scd_dirty_aiu` — `AFTER INSERT ON ad_storedcolumn_dirty DEFERRABLE INITIALLY
DEFERRED FOR EACH ROW EXECUTE FUNCTION ad_scd_process_dirty()`.

**Verify first:** whether dbsm's trigger XML supports `CONSTRAINT … DEFERRABLE`.
If not, deploy this trigger via the ModuleScript (like the enqueue triggers) instead
of XML.

### C. Enqueue generator changes — `GenerateStoredComputedTriggers.java`

- Add `my.scd_refreshing` early-return guard alongside the existing
  `my.triggers_disabled` check.
- Add a watched-column `IS DISTINCT FROM` value guard on UPDATE: pass watched column
  names into `buildFunctionDdl`; on `TG_OP='UPDATE'` return early unless at least one
  watched column actually changed value. INSERT/DELETE always enqueue.

### D. ReadOnlyLogic propagation (§4.4)

Three enforcement points so `AD_Field.ReadOnlyLogic='Y'` for every field backed by a
`Computation_Mode='S'` column:
- **SQL step** — `UPDATE ad_field` for fields whose column is stored-computed.
- **Callout** on the Field window's `AD_Column_ID`.
- **`@OBDALEventHandler`** on `AD_Field` save (covers programmatic saves).

### E. dbsm exclude filter (§5.3)

Exclude **all** `ad_scd_*` objects from `export.database` so they don't show as
drift or get dropped. `excludeFilter.xml` adds `<excludedFunction name="AD_SCD\_%"/>`
and `<excludedTrigger name="AD_SCD\_%"/>` (a single pattern each), which covers both
the per-dependency enqueue functions/triggers (`ad_scd_%_trf` / `ad_scd_%_trg`) **and**
the static engine objects (`ad_scd_recompute`, `ad_scd_process_dirty`, `ad_scd_rebuild`,
`ad_scd_check`, and the `ad_scd_dirty_aiu` constraint trigger). None of these live in
dbsm XML — every `ad_scd_*` object is created at runtime by the
`GenerateStoredComputedTriggers` ModuleScript via raw JDBC.

### F. Pilot → `com.etendo.test`

Move/keep as model + sourcedata under `modules/com.etendo.test` (owned by module
`FF33B6827BAC422CA69953AD9899B933`):
- `ettst_sumlineamounts(p_c_order_id varchar) returns numeric` function.
- `EM_Ettst_Linetotal` column metadata on `C_Order` (`computation_mode='S'`,
  `computation_function='ETTST_SUMLINEAMOUNTS'`, `refresh_mode='S'`, seq 10).
- The `AD_COLUMN_COMP_DEPENDENCY` row (`C_OrderLine`, insert/update/delete, watched
  `linenetamt`, UNION resolver).

The deleted core `ETTST_SUMLINEAMOUNTS.xml` stays deleted.

### G. Build wiring

Ensure the modulescript compile runs so
`GenerateStoredComputedTriggers.class` exists in
`src-util/modulescript/build/classes` and is discovered/executed on
`update.database`. (`update.database` does not depend on `compile.modulescript`, so
the class must be compiled by the smartbuild/full-build flow first.)

### H. Integration tests — delegate to `test-generator`

Per §11 exit criteria:
- insert a child → target refreshes at commit
- update a **watched** column → refreshes
- update a **non-watched** column → no enqueue, no write
- delete a child → target refreshes
- bulk update → single correct drain, no spurious writes (`IS DISTINCT FROM`)
- recursive-loop guard → `my.scd_refreshing` blocks re-entry

## Implementation order

A → B → C → D → E → F → G, then H.

Two verifications gate their workstreams:
- dbsm support for `DEFERRABLE` constraint triggers → decides B's deploy path.
- exact dbsm exclude mechanism → decides E.

## Exit criteria (§11)

Pilot passes all integration tests; no spurious writes; no recursive loops.

## Commit

```
Epic EPL-1807: Add deferred recalculation engine for stored computed columns
```
