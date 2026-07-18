# EPL-1807 — Phase 4 Plan: Async Queue & Initial Population (PostgreSQL)

Branch: `epic/EPL-1807`. Do not push.

## Context

Phase 3 shipped the enqueue + **synchronous** drain. `GenerateStoredComputedTriggers`
already deploys, via raw JDBC at `update.database`:

- per-dependency enqueue functions/triggers (`ad_scd_<dep>_trf` / `_trg`),
- the generic engine — `ad_scd_recompute`, `ad_scd_process_dirty`, `ad_scd_rebuild`,
  `ad_scd_check`,
- the static constraint trigger `ad_scd_dirty_aiu` (`DEFERRABLE INITIALLY DEFERRED`).

That covers `Refresh_Mode = 'S'` end-to-end (verified by the
`com.etendoerp.storedcomputedcolumn` pilot, multi-column + multi-source). Phase 4 closes the two **PostgreSQL** gaps that the synchronous
path intentionally left open:

1. **Queued mode (`Refresh_Mode = 'Q'`)** — nothing drains `'Q'` dirty rows today.
   `ad_scd_process_dirty` filters `refresh_mode = 'S'` and the constraint trigger fires
   `WHEN (NEW.refresh_mode = 'S')`, so `'Q'` rows accumulate forever. This is the
   headline deliverable (REQUIREMENTS §5.5).
2. **Initial population of existing records** — activating a stored-computed column on a
   table that already has data leaves every existing row stale until something writes it.
   There is no automatic backfill today (REQUIREMENTS §3 step 9, §5.5 step 2 sentinel).

**Oracle support is out of scope for Phase 4 → see `PLAN-PHASE5.md`.** Phase 4 ships
PostgreSQL only: the async processor calls the shipped PL/pgSQL `ad_scd_recompute`. Phase 5
later refactors that recompute step into Java (the cross-platform enabler) and adds the
Oracle-dialect enqueue triggers.

> **Naming note.** REQUIREMENTS calls the engine functions `sf_rebuild` / `sf_check` and
> the session flag `sf.refreshing`. The shipped implementation uses the `ad_scd_`
> namespace (`ad_scd_rebuild`, `ad_scd_check`, GUC `my.scd_refreshing`). This plan uses
> the **as-deployed** `ad_scd_` names; treat the `sf_` names in REQUIREMENTS as aliases.

---

## Locked / inherited decisions

1. **Namespace:** everything stays under `ad_scd_` (no `sf_` realign).
2. **Recursion / trigger-disable guards:** reuse the existing `my.scd_refreshing` and
   `my.triggers_disabled` GUC conventions — the async processor sets `my.scd_refreshing`
   while writing, exactly like the synchronous drain, so its own writes do not re-enqueue.
3. **Dedup across transactions:** the unique constraint only dedups *within* a
   transaction. Cross-transaction duplicate `'Q'` rows for the same target are accepted
   as wasteful-but-correct; `SELECT DISTINCT ON` dedup is post-MVP (REQUIREMENTS §5.5).
4. **Sentinel:** `Target_Record_ID IS NULL` means "recompute all rows of this column" —
   used only by initial population and manual bulk reset, never by dependency triggers.

---

## Workstream A — Async queue processor (`Refresh_Mode = 'Q'`)

The drainer for `'Q'` runs **outside** the originating transaction, on a schedule. Per
REQUIREMENTS §5.5 it is a **Java background process request**, not a DB-side timer — so it
plugs into Etendo's existing ProcessRequest / scheduler infrastructure. In Phase 4 it drains
the PostgreSQL queue by calling the shipped PL/pgSQL `ad_scd_recompute`; the same Java
process becomes the cross-platform drain once Phase 5 moves recompute into Java (→ see
`PLAN-PHASE5.md`).

### A1. Process request bean — core

New `@Background`-style process (e.g. `StoredColumnQueueProcessor`) registered as an
`AD_Process` so it can be scheduled via **Process Request / Scheduler**.

Parameter (REQUIREMENTS §5.5):

| Parameter         | Type    | Default | Description                                          |
|-------------------|---------|---------|------------------------------------------------------|
| `Max Records`     | INTEGER | 100     | Max dirty rows claimed per execution                 |
| `Retry_Threshold` | INTEGER | 5       | `retry_count` at which a failing `'Q'` row is dead-lettered (A4.3) |

**Execution per invocation (one DB transaction):**

1. Claim up to `Max Records` rows from `AD_StoredColumn_Dirty` where `Refresh_Mode = 'Q'`,
   `ORDER BY Computation_Sequence_Number ASC, Created ASC`, using `SELECT … FOR UPDATE`
   (deliberately **without** `SKIP LOCKED` — see A3).
2. **Sentinel first:** for each claimed row with `Target_Record_ID IS NULL`, call
   `ad_scd_rebuild(<column_id>)`, delete the sentinel, and drop from the batch any other
   claimed rows for the same column (the rebuild already covered them).
3. Group the remaining rows by `AD_Column_ID`; process in `Created` order within a group.
4. For each row: `SET LOCAL my.scd_refreshing = 'Y'`, call `ad_scd_recompute(column_id,
   target_id)` (which already does the `IS DISTINCT FROM` write guard + `FOR UPDATE`
   target lock from Phase 3), then **delete that dirty row** immediately.
5. Commit. On any error the whole batch rolls back and the rows remain for the next run
   (at-least-once; recompute is idempotent).

### A2. Constraint-trigger scope is already correct

`ad_scd_dirty_aiu` fires `WHEN (NEW.refresh_mode = 'S')`, so inserting `'Q'` rows does
**not** trigger the synchronous drain — they simply persist for A1. No change needed to the
trigger; confirm with a test that a `'Q'` insert leaves the row in the table post-commit.

### A3. Concurrency — the drain is serial

**Concurrent drainers are NOT supported.** Schedule exactly one Process Request, on one node.

The claim is ordered by `Computation_Sequence_Number`, and that ordering is a correctness
requirement rather than a nicety: chained stored columns may read one another's stored values,
and a downstream column only sees a fresh upstream value because the lower-sequence column is
recomputed first. A second drainer running concurrently orders **its own half** of the queue
independently, so a downstream column can be recomputed before — or alongside — the upstream
column it reads, storing a stale value. The claim therefore uses a plain `FOR UPDATE` (no
`SKIP LOCKED`), so a concurrent fetch blocks instead of claiming a disjoint, independently
ordered batch.

`ad_scd_recompute`'s `FOR UPDATE` lock on the **target** row still prevents two writers racing
on the same aggregate, but a target lock cannot restore cross-column *ordering* — hence the
serial requirement.

This restricts only the drainer. Concurrent **user** transactions writing to source tables are
fully supported; they just enqueue dirty rows for the single drainer to process in order.

> **Superseded (per-client partitioning):** the queue is now partitioned by `AD_CLIENT_ID` — the
> "single drainer" ordering requirement holds *within* a client, while different clients drain
> independently. See `OPERATIONS.md` for the current behavior.

### A4. Failure / retry / poison rows — **Q mode only**

**Scope decision (locked):** error handling lives **only** in the `'Q'` async path. The
synchronous `'S'` deferred drain (`ad_scd_process_dirty`, Phase 3) stays **fail-hard**: a
recompute error propagates, the constraint trigger raises, and the user's COMMIT aborts.
Never swallow, retry, or dead-letter in `'S'` — the synchronous contract is "the stored
value is correct at commit, or the transaction does not commit." No change to
`ad_scd_process_dirty`. See A4.0.

The async processor (A1) cannot fail the user's transaction (it runs in its own scheduled
transaction), so a permanently-failing `'Q'` row would otherwise wedge its batch forever.
A4 gives `'Q'` rows per-row retry, error capture, and dead-lettering.

**A4.0 — `'S'` is fail-hard (no code change).** Documented here so the asymmetry is
deliberate, not an oversight. `'S'` rows never carry retry/error/ignored state.

**A4.1 — Schema (3 new columns on `AD_STOREDCOLUMN_DIRTY`).** Model XML +
`AD_Column` sourcedata (the table already has `AD_Column` metadata for its 13 columns; the
new ones need matching records for export/import consistency, but no window/tab — the table
has no maintained UI):

| Column | Type | Default | Notes |
|---|---|---|---|
| `RETRY_COUNT` | `DECIMAL(10,0)` | `0` | Incremented each time a `'Q'` row's recompute fails. |
| `ERROR_MSG` | `VARCHAR(4000)`/nullable | — | `SQLERRM` of the last failure; `NULL` when healthy. |
| `IS_IGNORED` | `CHAR(1)` | `'N'` | `'Y'` once dead-lettered. Add `… IN ('Y','N')` check. |

**A4.2 — Per-row recompute with savepoint (replaces A1 step 4/5 batch-rollback for `'Q'`).**
Process each claimed `'Q'` row inside its own savepoint so one poison row cannot abort the
others in the batch:

```
FOR each claimed 'Q' row:
  SAVEPOINT s;
  BEGIN
    SET LOCAL my.scd_refreshing = 'Y';
    PERFORM ad_scd_recompute(column_id, target_id);
    DELETE the dirty row;          -- success
  EXCEPTION WHEN OTHERS THEN
    ROLLBACK TO SAVEPOINT s;
    UPDATE the dirty row
      SET retry_count = retry_count + 1,
          error_msg   = left(SQLERRM, 4000),
          is_ignored  = CASE WHEN retry_count + 1 >= :threshold THEN 'Y' ELSE 'N' END,
          updated     = now();
  END;
COMMIT;   -- successes are durable; failed rows persist with bookkeeping
```

The claim query (A1 step 1) adds `AND is_ignored = 'N'` so dead-lettered rows are skipped
on every future run. Successful rows are deleted; the batch as a whole commits.

**A4.3 — Dead-letter threshold.** A `Retry_Threshold` **process parameter** on the async
drain process (`AD_PROCESS_PARA`, Integer, `DefaultValue = 5`), set on the **Process
Request** screen alongside `Max Records`. Read from the `ProcessBundle`; keep a `5` fallback
in Java in case the param is null (the `DefaultValue` only pre-fills the UI field, it is not
guaranteed in the bundle). At `retry_count >= threshold` the row flips `is_ignored = 'Y'`
and is logged at WARN with `(ad_column_id, target_record_id, error_msg)` so it is
diagnosable.

**A4.4 — Reset on re-trigger (in the enqueue function).** A new source change re-enqueues
the `(column, target)` via the per-dependency trigger (`buildFunctionDdl`). Because the
dedup key includes `TRANSACTION_ID`, a new transaction inserts a **fresh** row rather than
conflicting — so a stale dead-lettered row would otherwise linger forever. Fix: before the
`INSERT … ON CONFLICT DO NOTHING`, the enqueue function deletes any prior dead-lettered row
for the same target:

```sql
DELETE FROM ad_storedcolumn_dirty
 WHERE ad_column_id = '{column_id}' AND target_record_id = v_target_id
   AND is_ignored = 'Y';
```

This is safe for `'S'` columns too — they never set `is_ignored='Y'`, so the DELETE is a
no-op there. A genuine new change thus gives the row a clean retry (`retry_count=0`,
`error_msg=NULL`, `is_ignored='N'`) via the fresh insert. Requires extending
`GenerateStoredComputedTriggers.buildFunctionDdl`.

### A5. Scheduling

**As shipped:** no **Process Request** template is delivered — not even an inactive one.
Each installation creates its own Process Request for the queue processor, so the interval
matches local latency tolerance and load rather than inheriting an arbitrary default.

Documented that `'Q'` columns lag by the scheduler interval and are eventually consistent.
The operator procedure (step-by-step Process Request setup, *Max Records* / *Retry
Threshold* parameters, and tuning guidance) lives in `OPERATIONS.md` → "Scheduling the
queue processor".

---

## Workstream B — Initial population for existing records

Activating a stored-computed column on a populated table must backfill existing rows.
This is the "open topic" — wire it to the existing engine, do not invent a second path.

### B1. Trigger point

When a column transitions to `Computation_Mode = 'S'` and its dependency objects are
(re)deployed, `GenerateStoredComputedTriggers` already knows the column is "new" if no
generated objects existed before this run. Per REQUIREMENTS §3 step 9, on first activation:

- `Refresh_Mode = 'S'` → call `ad_scd_rebuild(<column_id>)` **directly** at deploy time
  (synchronous, in the `update.database` transaction). Bounded by table size — see B3.
- `Refresh_Mode = 'Q'` → insert **one null-sentinel** dirty row
  (`Target_Record_ID = NULL`, `Refresh_Mode='Q'`) so the async processor (A1 step 2)
  performs the rebuild off-line.
- `Refresh_Mode = 'M'` → do nothing; operator runs `ad_scd_rebuild` manually.

### B2. Detecting "first activation"

`GenerateStoredComputedTriggers` must distinguish *new* columns from *already-deployed*
ones so it does not re-run a full rebuild on every `update.database`. Options:

- **(recommended)** check `pg_proc` / `pg_trigger` for the column's existing `ad_scd_*`
  objects before (re)creating them — absence ⇒ first activation ⇒ populate. The generator
  already queries `pg_proc` for orphan cleanup, so the catalog round-trip is in hand.
- Persist a "last populated" marker on `AD_Column` (heavier; adds AD model surface).

Go with the catalog-probe approach unless it proves unreliable across re-deploys.

### B3. Large-table guard

A synchronous `'S'` rebuild inside `update.database` blocks the build on big tables. Add a
row-count threshold: above it, log a warning and **enqueue a sentinel** (deferring to the
async/manual path) instead of inline rebuild. Threshold value → confirm with user.

### B4. Manual entry point

`ad_scd_rebuild(<column_id>)` is already shipped and idempotent; expose it as an AD process
("Rebuild stored column") for post-migration repair and on-demand initial population, in
addition to the automatic path above.

---

## Workstream C — Engine tweaks for the PostgreSQL `'Q'` path

The async processor (A1) calls the **already-shipped PL/pgSQL `ad_scd_recompute`** — Phase 4
introduces no new engine functions. Two adjustments to the existing engine / generator are in
scope here; both are also prerequisites that keep Phase 5 (Oracle) a clean addition.

### C1. Drop the recompute distinct clause (SETTLED)

`ad_scd_recompute` (and `ad_scd_rebuild`, which loops it) currently writes
`UPDATE … SET <col> = <fn>(<pk>) WHERE <pk> = ? AND <col> IS DISTINCT FROM <fn>(<pk>)`. The
`IS DISTINCT FROM` guard forces the computation function to run **twice per row** (once in
`SET`, once in `WHERE`). Paying a second full aggregate compute to skip a no-op write is a bad
trade — **drop the clause** and always write:

```sql
UPDATE <table> SET <col> = <fn>(<pk>) WHERE <pk> = ?;
```

**Untouched:** the enqueue trigger's watched-column guard
(`NEW.<col> IS DISTINCT FROM OLD.<col>` — compares materialized values, no function call,
cheap) and `ad_scd_check` (detecting staleness *is* its job). Unconditional writes mean a
recompute on an unchanged value still writes (WAL, non-SCD `AFTER UPDATE` triggers on the
target); the recursion guard prevents SCD re-enqueue, and the enqueue watched-column guard
already filtered no-op source changes upstream, so this is rare in practice.

### C2. Resolver portability enforcement in the generator

`target_id_resolver_sql` is authored **once**, in **portable** SQL. Even though Phase 4 only
generates PostgreSQL triggers, the generator must enforce portability **now** so resolvers are
Oracle-ready before Phase 5 exists. Add a lightweight validation pass over each resolver
before deploying that **fails the dependency** (logged, skipped) if it contains a known
non-portable token — minimally a bare `SELECT … WHERE` with no `FROM` (must use `FROM dual`,
which is portable: Etendo ships a `public.dual` view on Postgres), plus a small denylist of
PG-only constructs (`::` casts, `pg_*`, `ON CONFLICT`, etc.). The `NEW.`/`OLD.` correlation
prefix is the **only** sanctioned dialect-specific element — Phase 5's Oracle generator
rewrites it to `:NEW.`/`:OLD.`; everything else must already be portable.

The pilot resolver is authored portably as:

```sql
SELECT NEW.c_order_id FROM dual WHERE NEW.c_order_id IS NOT NULL
UNION
SELECT OLD.c_order_id FROM dual WHERE OLD.c_order_id IS NOT NULL
```

---

## Workstream D — Cross-cutting

### D1. dbsm exclude filter

Confirm the Phase 3 `excludeFilter.xml` patterns (`AD_SCD\_%` function/trigger) already
cover any new objects. The async processor adds no DB objects (it is Java), so likely no
change — verify.

### D2. Tests — delegate to `test-generator`

Per REQUIREMENTS §7.4:

- `'Q'` column accumulates dirty rows after a business txn **without** synchronous processing.
- Background processor claims a batch, writes results, deletes processed rows.
- Null-sentinel row triggers full `ad_scd_rebuild` and is deleted; sibling individual rows
  for the same column are skipped.
- Chained columns drain upstream-first: a column whose function reads another stored column's
  value sees the refreshed upstream value, because the batch is drained in
  `Computation_Sequence_Number` order (including when the chain spans successive batches).
- Concurrent **user** transactions writing watched source rows all enqueue correctly and are
  drained in order by the single drainer (per client — see the per-client partitioning note above
  and `OPERATIONS.md`).
- Consistency check (`ad_scd_check`) detects values the queue has not yet refreshed.
- **Poison row (Q):** a row whose recompute always fails increments `retry_count`, captures
  `error_msg`, does **not** block its batch-mates (savepoint isolation), and flips
  `is_ignored='Y'` at the threshold; subsequent runs skip it (`is_ignored='N'` filter).
- **Reset on re-trigger (Q):** a fresh source change on a dead-lettered `(column,target)`
  clears the ignored row and recomputes cleanly (`retry_count=0`, `error_msg=NULL`).
- **`'S'` fail-hard (contrast):** a failing `'S'` recompute aborts the user's COMMIT and
  writes **no** retry/ignored bookkeeping — proves the Q-only scoping.
- Initial population: activate a column on a pre-populated table → all existing rows correct
  (one path per refresh mode).

(Oracle tests live in `PLAN-PHASE5.md`.)

Extend the `com.etendo.test` SQL scenario harness with the `'Q'` and initial-population
cases. **Reminder:** that harness lives in the gitignored `com.etendo.test` module — commit
it in that module's own repo, not in etendo_core.

---

## Implementation order

A1–A3 (the queue processor, the headline) → A4/A5 → B (initial population, reuses A's
sentinel path) → C1–C2 (engine tweaks) → D, then tests.

Everything in Phase 4 is **settled** — no open design questions. Failure handling: A4
retry/dead-letter, **`'Q'` only**; `'S'` stays fail-hard (no bookkeeping). Threshold: a
`Retry_Threshold` process parameter (default 5) on the async drain process. Async path calls
the shipped PL/pgSQL `ad_scd_recompute` with its distinct clause dropped (C1). Resolver
portability is enforced at generation time (C2).

**Oracle is out of scope → `PLAN-PHASE5.md`** (enqueue-on-Oracle / silently force `'Q'`, plus
the Java-recompute refactor that lets one drain serve both platforms).

## Exit criteria

- A `'Q'` column drains correctly and idempotently via the scheduled processor, running as a
  single serial instance (concurrent drainers are not supported — A3); nothing drains
  synchronously.
- Activating a stored-computed column on a populated table backfills all existing rows via
  the mode-appropriate path.
- Poison `'Q'` rows retry, capture `error_msg`, isolate via savepoint, and dead-letter at the
  threshold; `'S'` recompute failures stay fail-hard with no bookkeeping.
- All §7.4 async + initial-population tests pass.

## Commit

```
Epic EPL-1807: Add async queue processor and initial population
```
