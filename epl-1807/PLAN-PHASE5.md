# EPL-1807 — Phase 5 Plan: Oracle Support

Branch: `epic/EPL-1807`. Do not push.

## Context

Phases 3–4 delivered the full stored-computed-column engine on **PostgreSQL only**:

- Phase 3: per-dependency enqueue triggers + the **synchronous** (`'S'`) deferred drain
  (`ad_scd_dirty_aiu` constraint trigger → `ad_scd_process_dirty` → `ad_scd_recompute`).
- Phase 4: the **queued** (`'Q'`) async drain (Java `StoredColumnQueueProcessor`, which calls
  the shipped PL/pgSQL `ad_scd_recompute`), initial population, and per-row retry / dead-letter.

Phase 5 adds Oracle. Oracle is treated as a **first-class queued platform**, not an excluded
one — the synchronous `'S'` path cannot exist on Oracle (no deferred constraint-trigger
firing, no `pg_current_xact_id()`), so on Oracle every column behaves as `'Q'`.

---

## Model — enqueue-on-Oracle, silently force `'Q'`

Per user directive: instead of *rejecting* `'S'` on Oracle, we **silently downgrade it to
`'Q'`**.

- The per-dependency **enqueue triggers ARE created on Oracle** (Oracle PL/SQL dialect), so
  source-table changes still write dirty rows.
- Those triggers **always write `Refresh_Mode='Q'`**, ignoring the column's configured mode —
  a column an admin set to `'S'` behaves as `'Q'` on Oracle.
- The constraint trigger **`ad_scd_dirty_aiu` is NOT created** on Oracle, and
  **`ad_scd_process_dirty` is NOT deployed** (both require deferred firing) — there is no
  synchronous drain.
- The **async Java processor (Phase 4 Workstream A) is the only drain** on Oracle.

Dialect detection: `cp.getRDBMS()` returns `"POSTGRE"` / `"ORACLE"` (precedent
`PgJdbcDatesIssue.java:60`). Branch in `GenerateStoredComputedTriggers`'s
`execute()` / `deployEngine` / `buildFunctionDdl`.

---

## Workstream C1 — Generator: Oracle-dialect enqueue DDL

When `getRDBMS()='ORACLE'`, emit the enqueue trigger in Oracle PL/SQL:

| PG construct (current) | Oracle replacement |
|---|---|
| standalone fn + `EXECUTE FUNCTION` | inline trigger body (`CREATE OR REPLACE TRIGGER … BEGIN … END;`) |
| `NEW.`/`OLD.`/`TG_OP` | `:NEW`/`:OLD`/`INSERTING`/`UPDATING`/`DELETING` |
| `NOW()` | `SYSDATE` |
| `INSERT … ON CONFLICT DO NOTHING` | `MERGE` on `(AD_COLUMN_ID, TARGET_RECORD_ID)` where `IS_IGNORED='N'` |
| `pg_current_xact_id()` | leave `TRANSACTION_ID` NULL (unused — only the `'S'` drain reads it) |
| `Refresh_Mode` literal from column | **forced literal `'Q'`** |
| `DROP TRIGGER IF EXISTS … ON tbl` | drop-by-name, catch `ORA-04080` (does-not-exist) |
| orphan cleanup via `pg_proc` | `user_triggers` / `user_objects` |

The embedded resolver SQL needs only the single sanctioned transform `NEW.`→`:NEW.` /
`OLD.`→`:OLD.` (see "Resolver SQL dialect", below). `FROM dual` is already portable, so no
other rewrite of the resolver body is required.

`deployEngine` on Oracle: skip `ad_scd_dirty_aiu` and `ad_scd_process_dirty` entirely.

## Workstream C2 — Async recompute in Java (cross-platform enabler)

**Phase 4 ships the async processor calling the shipped PL/pgSQL `ad_scd_recompute` (PG).**
Phase 5 refactors the recompute step to run **in Java**, so one Java drain serves both
platforms and Oracle needs **zero** engine PL/SQL functions.

Per claimed dirty row the processor issues, on its own connection inside the Phase 4 A4
savepoint:

```sql
SELECT 1 FROM <table> WHERE <pk> = ? FOR UPDATE;     -- cross-txn lock (Phase 3 concurrency note)
UPDATE <table> SET <col> = <fn>(<pk>) WHERE <pk> = ?; -- recompute + write (distinct clause dropped)
```

Metadata (`<table>`, `<col>`, `<pk>`, `<fn>`) is read once from `AD_COLUMN`/`AD_TABLE` and
cached. The sentinel case (`target_record_id IS NULL`) loops the same method over all PKs of
the target table (replacing `ad_scd_rebuild` on the async path).

Consequences:
- **One Java drain serves both platforms.** Oracle deploys only the enqueue triggers (C1).
- Postgres keeps PL/pgSQL `ad_scd_recompute` **only** for its in-transaction `'S'` drain
  (constraint-trigger-driven, no Java in that path). The Java and PL/pgSQL implementations
  must stay behaviorally identical (covered by tests run on both DBs).
- The recompute path runs on every `'Q'` drain on the PG dev DB, so it is continuously
  exercised; only a thin dialect seam (the refreshing-guard statement, C3) is Oracle-specific.

> **Distinct clause** is already dropped in Phase 4 from PL/pgSQL `ad_scd_recompute` /
> `ad_scd_rebuild` (forcing the computation function to run twice per row is a bad trade).
> The Java recompute here inherits the same rule — always write, no
> `WHERE <col> IS DISTINCT FROM <fn>(<pk>)` guard.

Rejected alternative: port `ad_scd_recompute`/`rebuild`/`check` to Oracle PL/SQL (a second
full engine copy that never runs on the PG dev box → parity bugs surface only on Oracle).

## Workstream C3 — Recursion / trigger-disable guard on Oracle

Postgres uses the `my.triggers_disabled` and `my.scd_refreshing` GUCs so the engine's own
writes to target tables don't re-enqueue. On Oracle the async processor writes target rows
too → those fire the enqueue triggers → re-enqueue loop. Oracle needs the equivalent via a
`SYS_CONTEXT` namespace (`DBMS_SESSION.set_context`), set by the Java processor while
recomputing and checked at the top of each Oracle enqueue trigger. **Reuse whatever
mechanism Etendo's existing Oracle triggers already use for the global trigger-disable**
rather than inventing a new namespace.

---

## Settled design decisions (inherited)

**Resolver SQL dialect — portable-by-contract, single auto-transform.**
`target_id_resolver_sql` is authored **once** and stored in **one** column (no `*_ORA`
variant). The resolver **must be written in portable SQL** — the only dialect divergence the
generator introduces is rewriting the correlation prefix `NEW.`→`:NEW.` and `OLD.`→`:OLD.`
when emitting the Oracle enqueue trigger (case-insensitive, dot-qualified, e.g.
`(?i)\bNEW\.`→`:NEW.`). Nothing else is rewritten.

Viable because `FROM dual` is portable: Etendo ships a `public.dual` view on Postgres (single
`dummy` column, one row), so a `SELECT … WHERE …` that would otherwise be PG-bare becomes
portable by writing `SELECT … FROM dual WHERE …`. The pilot resolver is authored portably as:

```sql
SELECT NEW.c_order_id FROM dual WHERE NEW.c_order_id IS NOT NULL
UNION
SELECT OLD.c_order_id FROM dual WHERE OLD.c_order_id IS NOT NULL
```

On Oracle the generator emits the same string with `NEW.`/`OLD.`→`:NEW.`/`:OLD.`.

**Enforcement (required):** the generator runs a lightweight validation pass over each
resolver before deploying and **fails the dependency** (logged, skipped) if it contains a
known non-portable token — minimally a bare `SELECT … WHERE` with no `FROM` (must use
`FROM dual`), plus a small denylist of PG-only constructs (`::` casts, `pg_*`, `ON CONFLICT`,
etc.). This enforcement belongs in Phase 4's generator so PG-authored resolvers are portable
*before* Oracle generation exists; Phase 5 relies on it.

**Dedup without `transaction_id`.** The unique `(column, target, transaction_id)` key dedups
per-transaction on PG. With `transaction_id` NULL on Oracle there is no per-transaction
dedup; the Oracle `MERGE` instead dedups on `(column, target)` where `IS_IGNORED='N'`.

**`'S'` is not rejected on Oracle** (silently accepted as `'Q'`). The AD field UI should warn
"on Oracle this behaves as queued / eventually-consistent" so the admin isn't surprised by
the downgrade.

---

## Open design questions

1. **Scope shape.** Full Oracle enqueue (C1–C3) vs. interim build-safety guard that *skips*
   all SCD deployment on Oracle (feature absent — **not** the "silently force `'Q'`" model).
   This plan assumes full C1–C3; revisit if an Oracle target is not yet on the roadmap.

---

## Tests — delegate to `test-generator`

Run the recompute assertions against Oracle:

- `'S'` column on Oracle silently enqueues as `'Q'`; no constraint trigger / no synchronous
  drain exists; the async processor drains correctly.
- Oracle enqueue trigger writes dirty rows for INSERT / watched-UPDATE / DELETE; non-watched
  UPDATE does not enqueue.
- The `NEW.`/`OLD.`→`:NEW.`/`:OLD.` resolver transform produces a compiling Oracle trigger and
  resolves the correct target IDs.
- Recursion guard (C3) prevents the async processor's own target writes from re-enqueuing.
- Java recompute (C2) produces identical results to the PL/pgSQL `ad_scd_recompute` on the
  same fixtures (parity test, run on both DBs).

Extend the `com.etendo.test` SQL scenario harness with the Oracle cases. **Reminder:** that
harness lives in the gitignored `com.etendo.test` module — commit it in that module's own
repo, not in etendo_core.

---

## Implementation order

C2 (Java recompute refactor — swap the Phase 4 async path off PL/pgSQL) → C1 (Oracle enqueue
DDL) → C3 (Oracle recursion guard), then Oracle tests.

## Exit criteria

- On Oracle, an `'S'` column silently enqueues as `'Q'` and drains via the async processor;
  no constraint trigger or synchronous drain exists — eventual consistency, never staleness.
- One Java recompute path serves both PostgreSQL and Oracle; Oracle deploys zero engine
  PL/SQL functions.
- All Oracle + parity tests pass.

## Commit

```
Epic EPL-1807: Add Oracle support for stored computed columns
```
