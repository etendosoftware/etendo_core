# EPL-1807 — Operating the async queue (`Refresh_Mode = 'Q'`)

This note covers the operational side of Phase 4: how to schedule the background
processor, what eventual-consistency guarantees a `'Q'` column gives, and the manual
repair entry points. It complements the design in `REQUIREMENTS.md` and `PLAN-PHASE4.md`.

## Refresh modes recap

A stored computed column (`Computation_Mode = 'S'`) chooses how its dirty rows are drained
through `Refresh_Mode` on each dependency:

| Mode | Meaning | When the value is correct |
|------|---------|---------------------------|
| `'S'` Sync   | Deferred constraint trigger recomputes inside the *same* transaction. | At commit — always consistent. Fails hard if the recompute fails. |
| `'Q'` Queued | Dirty rows persist after commit; a background process drains them later. | **Eventually** — after the next queue-processor run. |
| `'M'` Manual | Dirty rows persist; an operator runs the rebuild on demand. | Only after a manual `Rebuild Stored Column` run. |

## Eventual consistency for `'Q'` columns

A `'Q'` column is **eventually consistent**, not transactionally consistent:

- A business transaction that changes a watched source row commits immediately. Its dirty
  row(s) stay in `AD_STOREDCOLUMN_DIRTY` until the background processor claims them.
- Between commit and the next processor run, the stored value reflects the **previous**
  computation. Grids, reports, and API reads see the stale value during that window.
- The maximum staleness is bounded by the **scheduler interval** plus one processing pass.
  With a 1-minute interval, a `'Q'` value is at most ~1 minute behind its sources under
  normal load (longer if the queue is backlogged).
- `ad_scd_check(<column_id>)` reports which target rows are currently out of sync — use it
  to confirm the queue has caught up, e.g. after a bulk import.

Pick `'Q'` only for columns whose consumers tolerate this lag (dashboards, aggregate KPIs,
non-blocking displays). Anything that must be exact at read time (validations, document
totals enforced at save) belongs on `'S'`.

## Scheduling the queue processor

The drain is the AD process **Stored Computed Column Queue Processor**
(`Value = StoredColumnQueueProcessor`, `AD_Process_ID = D35DC63A8838412890AEE01D31CD70A3`).
It is **not** shipped with an active Process Request — each installation enables it so the
interval matches local latency tolerance and load.

To enable it, create a **Process Request** (*General Setup → Process Scheduling → Process
Request*) per installation:

1. **Process** = `Stored Computed Column Queue Processor`.
2. **Timing** = Scheduled; **Frequency** = e.g. every 1 minute (tune to your lag tolerance).
3. Parameters:
   - **Max Records** — batch size per run (default `100`). Raise it if the queue backlogs;
     keep it bounded so a single run stays short and `SKIP LOCKED` lets parallel runs help.
   - **Retry Threshold** — failures before a row is dead-lettered (default `5`).

Multiple concurrent runs are safe: rows are claimed `FOR UPDATE SKIP LOCKED`, so parallel
scheduler threads drain disjoint sets and never compute the same target twice.

### Tuning guidance

- **Lower interval → fresher data, more polling overhead.** Most dashboards are fine at
  1–5 minutes.
- **Backlog growing run over run** → raise *Max Records* first, then add a second concurrent
  Process Request (SKIP LOCKED makes this scale linearly until DB write throughput is the
  limit).
- A large **initial population** (see below) for a `'Q'` column enqueues a single null
  sentinel; the first processor run after deploy does the full rebuild, which can be long.
  Schedule the first run for a maintenance window on big tables.

## Failure handling (dead-lettering)

When a per-target recompute fails, the processor isolates it in its own savepoint so the
rest of the batch still commits, then on the dirty row:

- increments `RETRY_COUNT`,
- stores the error text in `ERROR_MSG`,
- sets `IS_IGNORED = 'Y'` once `RETRY_COUNT` reaches **Retry Threshold** (logged at WARN).

Dead-lettered rows (`IS_IGNORED = 'Y'`) are skipped by future runs so one poison row cannot
stall the queue. To investigate:

```sql
SELECT ad_column_id, target_record_id, retry_count, error_msg
FROM   ad_storedcolumn_dirty
WHERE  is_ignored = 'Y'
ORDER  BY updated DESC;
```

A **fresh source change** on a dead-lettered `(column, target)` clears the ignored row
automatically (re-insert resets `retry_count = 0`, `error_msg = NULL`, `is_ignored = 'N'`),
giving it a clean retry. To force a retry without a source change, fix the root cause and run
the manual rebuild below (or `UPDATE … SET is_ignored = 'N', retry_count = 0` on the row).

## Manual repair / on-demand rebuild

The AD process **Rebuild Stored Column** (`Value = StoredColumnRebuild`,
`AD_Process_ID = DA0CCF7EF06F46588AD5E7EF5073FC81`) fully re-derives one column by calling
the idempotent engine function `ad_scd_rebuild(<column_id>)`. Use it for:

- post-migration repair after fixing a resolver or computation function,
- one-off initial population of a `'M'` column,
- clearing a backlog of dead-lettered rows after the underlying defect is fixed.

It is always safe to re-run: every target row is recomputed from its current dependencies.

## Initial population on first activation

When `GenerateStoredComputedTriggers` deploys a column's `ad_scd_*` objects for the first
time (detected via a `pg_proc` snapshot taken before deploy), it backfills existing rows
according to the column's refresh mode:

- **`'S'`** → rebuilds inline during `update.database`, **unless** the target table exceeds
  `LARGE_TABLE_THRESHOLD` (100,000 rows). Above the threshold it logs a WARN and enqueues a
  sentinel instead, so the build does not block — drain it via the queue or a manual rebuild.
- **`'Q'`** → enqueues one null sentinel; the next queue-processor run does the full rebuild
  off-line.
- **`'M'`** → does nothing; run **Rebuild Stored Column** when ready.
