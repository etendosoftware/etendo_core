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
     keep it bounded so a single run stays short.
   - **Retry Threshold** — failures before a row is dead-lettered (default `5`).

> **Run exactly one drainer per client. Two drainers for the same client are not supported.**
> The queue is partitioned by `AD_Client_ID`: each processor run drains only the rows of the
> client it runs under (read from `bundle.getContext().getClient()`), so schedule **one** Process
> Request per active client, and in a clustered installation make sure only one node runs each
> client's drainer. Within a client the processor drains the queue **serially**, in
> `Computation_Sequence_Number` order, and that ordering is a correctness requirement: chained
> stored columns may read one another's stored values, and a downstream column only sees a fresh
> upstream value because the upstream column is recomputed first. A second concurrent drainer for
> the *same* client orders its own portion of that client's queue independently, so a downstream
> column can be recomputed before the upstream column it reads and will store a **stale** value.
>
> Different clients' drainers **may** run concurrently — client partitions are disjoint, so they
> never contend and cannot reorder one another's chains. Concurrency is unsafe only *within* a
> single client.
>
> Note that `PREVENTCONCURRENT='Y'` on the process record is a useful guard but **not** a
> guarantee: the scheduler's check is node-local (so it does not stop a second node in a cluster)
> and it does not veto a run under a different client or organization. Enforce one-drainer-per-client
> operationally.

This restriction applies to the `'Q'` drain on **both** PostgreSQL and Oracle. It does **not**
restrict ordinary concurrent *user* activity: any number of users may write to source tables at
once — their enqueue triggers simply add dirty rows to the queue, which the client's single
drainer then processes in order.

### Tuning guidance

- **Lower interval → fresher data, more polling overhead.** Most dashboards are fine at
  1–5 minutes.
- **Backlog growing run over run** → raise *Max Records* and/or shorten the interval. Do **not**
  add a second Process Request to drain in parallel: concurrent drainers are not supported (see
  above) and will corrupt chained column values. The `'Q'` drain scales vertically (bigger
  batches, more frequent runs), not horizontally.
- A large **initial population** (see below) for a `'Q'` column enqueues one null sentinel
  **per client** that has rows in the target table; each client's first processor run after
  deploy does that client's full rebuild, which can be long. Schedule the first run for a
  maintenance window on big tables.

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

**Client scope:** the rebuild recomputes only the **caller's** client's rows, matching the
per-client partitioning of the async queue — a regular operator repairs only their own client.
The one exception is a caller running as **System** (`AD_CLIENT_ID='0'`): a System rebuild
recomputes **all** clients' rows, which is the intended cross-client repair path. The caller's
client is read from the process bundle context (`bundle.getContext().getClient()`).

## Initial population on first activation

When `GenerateStoredComputedTriggers` deploys a column's `ad_scd_*` objects for the first
time (detected via a `pg_proc` snapshot taken before deploy), it backfills existing rows
according to the column's refresh mode:

- **`'S'`** → rebuilds inline during `update.database`, **unless** the target table exceeds
  `LARGE_TABLE_THRESHOLD` (100,000 rows). Above the threshold it logs a WARN and enqueues a
  sentinel instead, so the build does not block — drain it via the queue or a manual rebuild.
- **`'Q'`** → enqueues one null sentinel **per client** that has rows in the target table (each
  carries that client's real `AD_CLIENT_ID`); each client's next queue-processor run does that
  client's full rebuild off-line.
- **`'M'`** → does nothing; run **Rebuild Stored Column** when ready.

## Build-time validation (`update.database`) and the `ETGO_SCD_VALIDATION` toggle

Phase 5b adds a whole-DB validation gate that runs on every `update.database`, before any
trigger DDL is applied. It is implemented by the `ValidateStoredComputedColumns` ModuleScript
(delegating to `StoredComputedValidator`) and re-run as **Gate 0** inside
`GenerateStoredComputedTriggers`, so a broken definition aborts the build *before* it can
deploy inconsistent database objects. It validates definition shape, computation-function
existence/signature/return-type/volatility, dependency completeness, target-resolver XOR,
dependency cycles, deployed-trigger drift, supporting FK indexes, and refresh ordering
(rules V1–V17 — see `REQUIREMENTS.md §3.6`). The check is **read-only and idempotent**: it inspects catalog and
AD metadata but never writes.

By default the gate is **enforcing**: any hard (`ERROR`) violation prints the aggregated
report and aborts `update.database` with a `BuildException`. Soft (`WARN`) findings are always
logged but never block.

The `ETGO_SCD_VALIDATION` toggle controls whether hard violations block the build:

| Value | Behaviour |
|-------|-----------|
| `enforce` (default) | Hard violations abort `update.database`; warnings are logged. |
| `warn` | **All** violations — hard and soft — are logged as warnings; the build proceeds. Escape hatch only. |

Resolution order (first match wins): JVM system property `-DETGO_SCD_VALIDATION=…`, then the
`ETGO_SCD_VALIDATION` environment variable. Any value other than `warn` (case-insensitive),
and the absence of the toggle entirely, means **enforce**.

```bash
# One-off warn-only build (does not block on hard violations):
./gradlew update.database -DETGO_SCD_VALIDATION=warn
# or:
ETGO_SCD_VALIDATION=warn ./gradlew update.database
```

Use `warn` only to unblock an emergency build while a definition defect is being fixed, or to
survey the full violation set on a legacy database before committing to enforcement. Restore
the default (`enforce`, i.e. unset the toggle) as soon as the reported definitions are fixed —
running with `warn` permanently defeats the guard and lets inconsistent stored computed columns
reach production. The aggregated report lists every violation at once (errors first, then
warnings), so a single `warn` run surfaces the complete backlog to work through.
