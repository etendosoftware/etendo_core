# EPL-1807 — Phase 6: Deferred "Excluded from MVP" hardening (engine / core)

This plan covers the engine and operational items intentionally deferred out of the MVP
(`REQUIREMENTS.md` §8) and the operational hardening flagged in §5.2, §7.6.

> **Split note.** The Schema Forge pipeline integration (formerly Workstream 6, plus the F17
> validator rule) is a **lower-priority, later step** and lives in its own file:
> [`PLAN-PHASE6-SCHEMA-FORGE.md`](./PLAN-PHASE6-SCHEMA-FORGE.md). It is split out because the
> runtime already treats stored columns correctly, so it is a design-clarity/guardrail
> improvement with no functional dependency on the workstreams here. Pick it up after these land.

Phases 3b, 4 (async
queue + initial population), and 5 (Oracle) are shipped; the engine
(`ad_scd_recompute` / `ad_scd_process_dirty` / `ad_scd_rebuild` / `ad_scd_check`), the
constraint trigger `ad_scd_dirty_aiu`, the dirty table `AD_STOREDCOLUMN_DIRTY`, the async
processor `StoredColumnQueueProcessor`, and the `GenerateStoredComputedTriggers` deploy
modulescript already exist and are the foundation everything below builds on.

This is a **design document only — no code, DDL, or AD data changes.** It is scoped as a menu of
independently shippable workstreams; a team can pick any subset. Each workstream states its
target repo and any cross-repo/branch coordination.

---

## Context

**Repos in play.** Three, sharing branches per `docs/branch-workflow.md`:

| Repo | Role in Phase 6 |
|------|-----------------|
| `etendo_core` (this repo) | Engine functions, Java processes/modulescripts, AD process metadata, DDL. Workstreams 1, 2, 3, 4, 7. |

The `etendo_schema_forge` design pipeline and the `com.etendoerp.go` runtime are **out of scope
for this file** — they are covered by the deferred follow-up
[`PLAN-PHASE6-SCHEMA-FORGE.md`](./PLAN-PHASE6-SCHEMA-FORGE.md).

**What already exists and grounds each workstream:**

- **`ad_scd_check(p_column_id varchar) RETURNS integer`** — deployed by
  `GenerateStoredComputedTriggers.CHECK_FN`. Counts target rows where the stored value
  `IS DISTINCT FROM` the recomputation function, i.e. rows currently out of sync. This is the
  primitive Workstream 1 wraps into a scheduled health check. It exists per column; there is no
  cross-column sweep today.
- **`GenerateStoredComputedTriggers`** currently redeploys **every** dependency's engine objects
  on every `update.database` (force mode). `buildFunctionDdl` does **not** embed a metadata
  comment/hash; first-activation detection uses only a `pg_proc` snapshot
  (`SELECT proname FROM pg_proc WHERE proname LIKE 'ad_scd_%_trf'`). `REQUIREMENTS.md` §5.2
  describes a SHA-256 metadata hash for incremental regen that is **not yet implemented**.
  Workstream 2 implements it.

---

## Locked decisions (inherited)

Carried unchanged from Phases 3b–5:

1. **Generic engine, no per-window Java.** Engine behavior lives in the `ad_scd_*` PL/pgSQL
   namespace (PostgreSQL) or the shared Java recompute path (Oracle). No column-specific code.
2. **`decisions.json` / AD metadata is the source of truth.** Never hand-edit generated output
   (`contract.json`, `generated/`, deployed `ad_scd_*_trf` functions). Fix generators.
3. **Never invent IDs.** New AD objects (processes, references, messages, parameters) get their
   primary key from `make uuid`; existing IDs are looked up via `menu-cache.js` /
   `resolve-menu.js` / DB query. This plan writes **no** literal UUIDs.
4. **Refresh-mode contract.** `'S'` = synchronous (deferred constraint trigger, transactionally
   consistent), `'Q'` = queued (eventually consistent), `'M'` = manual. Oracle downgrades `'S'`
   to `'Q'` silently.
5. **Kebab-case spec names** for any Schema Forge artifact reference.

---

## Workstreams

Five engine/core workstreams. Numbering matches the `REQUIREMENTS.md` §8 order the user gave;
letters inside a workstream sub-number its tasks (mirroring Phase 4's A1–A5 style).

> **Numbering gaps are intentional.** Two former workstreams keep their slots empty so existing
> cross-references stay stable: **Workstream 5** (`Refresh_Group` batching) was dropped by user
> decision — the extra metadata + grouped-drain complexity is not justified. **Workstream 6**
> (Schema Forge pipeline integration) was split into the lower-priority follow-up
> [`PLAN-PHASE6-SCHEMA-FORGE.md`](./PLAN-PHASE6-SCHEMA-FORGE.md).

### Workstream 1 — Consistency-check health checks (`etendo_core`)

**Rationale.** `ad_scd_check(column_id)` already answers "how many target rows of *this* column
are stale?" but there is no scheduled, all-columns sweep. Silent drift can accumulate for `'Q'`
columns (queue backlog / dead-lettered rows) and `'M'` columns (never rebuilt), or after a
resolver bug is fixed but rows were never re-derived. Operators need a periodic report.

**Target repo:** `etendo_core`.

**Design.**

- **1A — SQL sweep function.** Add a generic engine function
  `ad_scd_check_all() RETURNS TABLE(ad_column_id varchar, table_name varchar, stale_rows integer)`
  that iterates every **active** stored column
  (`AD_Column.Computation_Mode='S' AND AD_Column.IsActive='Y'`, joined through
  `AD_COLUMN_COMP_DEPENDENCY` to know which are engine-managed) and calls the existing
  `ad_scd_check(id)` per column. Deployed by `GenerateStoredComputedTriggers` alongside the
  other engine functions (single ownership of the `ad_scd_*` namespace). Skips columns with no
  deployed `_trf` function (not yet activated).
- **1B — Result destination: a status table (chosen over log-only / notification-only).**
  Add `AD_STOREDCOLUMN_HEALTH` (columns: `AD_STOREDCOLUMN_HEALTH_ID` PK, standard AD audit
  columns, `AD_COLUMN_ID` FK, `STALE_ROWS` numeric, `CHECKED` timestamp, `RUN_ID` varchar to
  group one sweep). Rationale: a table is queryable from AD windows, drives a KPI/notification,
  and preserves history for trend analysis — a log line does neither. The AD table + its window
  are created via `make uuid` for every new record (table, tab, fields, window, columns).
- **1C — Scheduled AD process wrapping the sweep.** New AD process
  (`Value = StoredColumnHealthCheck`, `AD_Process_ID` via `make uuid`) implementing
  `org.openbravo.scheduling.Process`. Each run: generate a `RUN_ID`, call `ad_scd_check_all()`,
  upsert one `AD_STOREDCOLUMN_HEALTH` row per column, and **log a WARN + emit an AD notification
  (alert)** for any column with `STALE_ROWS > 0`. Ships **inactive** (no Process Request) —
  installations schedule it like `StoredColumnQueueProcessor` (see `OPERATIONS.md`). Parameters:
  `Warn_Threshold` (default 1 — stale rows above this raise the alert), `Auto_Repair` (default
  `'N'`; when `'Y'`, enqueue a rebuild sentinel per stale column instead of only reporting).
- **1D — Alerts.** Reuse the AD Alert framework: an Alert Rule over `AD_STOREDCOLUMN_HEALTH`
  where `STALE_ROWS > 0` and `CHECKED` is within the last run window. No custom notification
  code — the process only writes the table; the alert rule surfaces it.

**Effort:** M (3–4 d). One engine function, one AD table+window, one Java process, one alert
rule. No new engine semantics — pure orchestration of `ad_scd_check`.

**Tests (delegate to `test-generator`):**
- `ad_scd_check_all()` returns one row per active engine-managed column and matches
  per-column `ad_scd_check` values on a fixture with a deliberately corrupted stored value.
- Sweep skips not-yet-activated columns (no `_trf`) without error.
- Process writes exactly one `AD_STOREDCOLUMN_HEALTH` row per column per `RUN_ID`; WARN + alert
  only above `Warn_Threshold`.
- `Auto_Repair='Y'` enqueues a rebuild sentinel for a stale column and the next queue run
  clears it.

**Risks.** On very wide installations the sweep's `IS DISTINCT FROM` scans are expensive
(each `ad_scd_check` full-scans its target). Mitigate: it runs on a slow schedule (hourly/daily),
`Warn_Threshold` bounds noise, and Workstream 4's EXPLAIN advice covers the underlying scans.

---

### Workstream 2 — Incremental trigger regeneration (`etendo_core`)

**Rationale.** `GenerateStoredComputedTriggers` redeploys **every** dependency on **every**
`update.database`, even when nothing changed — slow on installs with many stored columns and
noisy in the modulescript log. `REQUIREMENTS.md` §5.2 specifies a metadata hash to skip
unchanged deploys; it is designed but unimplemented.

**Target repo:** `etendo_core`.

**Design.**

- **2A — What is hashed.** SHA-256 over the *canonical metadata inputs* that determine a
  column's generated DDL, so any change that would alter the emitted function/trigger changes the
  hash: the column's `Computation_SQL` (or Java qualifier), `Refresh_Mode`,
  `Computation_Sequence_Number`, `Target_Id_Resolver_SQL` / `Target_Link_Column_Id`, the ordered
  set of watched dependency columns (`AD_COMPDEP_WATCHED_COL`), and the **generator version**
  (a constant bumped whenever `buildFunctionDdl` changes shape, so a generator upgrade forces a
  full redeploy). Concatenate in a fixed order with field separators, hash, hex-encode.
- **2B — Where the hash is stored: embedded in the generated object comment (chosen over a
  side table).** Emit `-- sf:metadata_hash=<hex>` as the first line inside `buildFunctionDdl`'s
  output, and read it back via `obj_description(oid)` / `pg_proc.prosrc` on the deployed
  function. Rationale (matches §5.2): the hash lives with the artifact it describes, survives DB
  dumps/restores, and cannot drift from a separate table. No new DDL.
- **2C — Skip logic.** Replace the current force-deploy loop: for each column, compute the
  fresh hash; read the deployed function's embedded hash; if equal, **skip** (log at DEBUG);
  if different or absent, redeploy the full set for that column (function + trigger + check). A
  `--force` / system-property escape hatch redeploys everything (recovery from manual DB edits).
- **2D — Interaction with first-activation.** First activation is still detected by the absence
  of the deployed function (existing `pg_proc` snapshot). Hash-skip only applies to *already
  deployed* columns, so initial population (Phase 4) is unaffected.
- **2E — Oracle.** On Oracle the engine deploys no PL/pgSQL functions (Phase 5); the hash is
  stored/read against the Oracle enqueue trigger's comment instead, or the feature is a no-op
  there. Keep the hash computation DB-agnostic; only the read-back is per-DB.

**Effort:** M (3–4 d). Hash builder + comment emit/parse + skip branch. Highest-value item for
`update.database` runtime on large installs.

**Tests (delegate to `test-generator`):**
- Identical metadata across two runs → second run deploys nothing (assert no `CREATE OR REPLACE`
  executed).
- Changing `Computation_SQL` / a watched column / `Refresh_Mode` each independently flips the
  hash and triggers redeploy.
- Bumping the generator-version constant forces redeploy of every column.
- Missing/garbled embedded hash on a deployed function → treated as changed → redeploy.
- `--force` redeploys regardless of hash equality.

**Risks.** A hash that omits an input silently skips a needed redeploy → stale generated code.
Mitigate: unit-test the input set explicitly, include the generator version, and keep the
`--force` recovery path documented in `OPERATIONS.md`.

---

### Workstream 3 — Pre-commit staleness hook (`etendo_core`, dev-time)

**Rationale.** A developer can edit a `Computation_SQL` or dependency in AD source data
(`src-db/.../sourcedata`) without re-running the generator, committing metadata whose deployed
hash (Workstream 2) would no longer match. This is a dev-time guard, not a runtime one.

**Target repo:** `etendo_core` (git hook + a small checker; depends on Workstream 2's hash).

**Design.**

- **3A — Checker.** A standalone command (Ant target or small Java/CLI entry) that, for every
  stored column in the *working-tree* AD source data, computes the Workstream-2 metadata hash and
  compares it against the hash the last generator run recorded. Since the generator embeds the
  hash in the deployed function (DB), the dev-time checker instead compares against a committed
  manifest file (`src-util/modulescript/.../scd-hash-manifest.properties`, `column_id=hash`)
  that the generator writes on every run. Staged changes to AD stored-column source data without
  a matching manifest update ⇒ fail.
- **3B — Hook.** A `pre-commit` hook (opt-in via the repo's hook-install target, mirroring
  Schema Forge's `.githooks/pre-commit` model) runs the checker only when staged files touch
  stored-column AD source data. Bypassable with `git commit --no-verify` (WIP only). CI runs the
  same checker in shadow/annotate mode first, blocking later.

**Effort:** S (1–2 d), but **blocked on Workstream 2** (needs the canonical hash + manifest).

**Tests (delegate to `test-generator`):**
- Editing a stored column's `Computation_SQL` in source data without updating the manifest →
  checker exits non-zero.
- Editing + regenerating (manifest updated) → checker passes.
- Non-stored-column edits → checker is a no-op (hook does not fire).

**Risks.** Manifest churn/merge conflicts on busy branches. Mitigate: one line per column, sorted
deterministically so diffs are minimal and conflicts are line-local.

---

### Workstream 4 — EXPLAIN-based seq-scan warnings + index validation (`etendo_core`)

**Rationale.** Resolver SQL (`Target_Id_Resolver_SQL`) and computation SQL run per dirty row and
per rebuild; if the columns they filter/join on are unindexed, a stored column silently degrades
to O(n) scans at every source change. There is no check today.

**Target repo:** `etendo_core`.

**Design.**

- **4A — Advisory analyzer (build/admin time, not per-transaction).** A generic engine function
  or Java admin routine `ad_scd_explain(column_id)` that runs `EXPLAIN (FORMAT JSON)` on the
  column's resolver and computation SQL (with representative bind values / a sample target id)
  and inspects the plan JSON for `Seq Scan` nodes on tables above a row threshold. Returns a
  list of `(table, filter/join column, estimated rows, suggested index)`.
- **4B — Integration point: the health check, not the hot path.** Wire 4A into Workstream 1's
  `StoredColumnHealthCheck` process behind an `Explain_Analysis` parameter (default `'N'`), so
  seq-scan advice lands in `AD_STOREDCOLUMN_HEALTH` / the log next to staleness. It **never**
  runs inside `ad_scd_recompute`/the trigger — EXPLAIN is diagnostic only.
- **4C — Index existence validation.** For each filter/sort column the resolver/computation
  references, check `pg_index` for a covering index and warn when absent. This is cheaper than
  EXPLAIN and can run every health sweep; EXPLAIN stays opt-in.
- **4D — Deploy-time lint (optional).** `GenerateStoredComputedTriggers` can, at first
  activation of a column, run 4C once and log a WARN with the suggested `CREATE INDEX` — a
  one-time nudge without blocking the build.

**Effort:** M (3–4 d). Plan-JSON parsing is the bulk; the `pg_index` check is small.

**Tests (delegate to `test-generator`):**
- Resolver filtering on an unindexed column → analyzer flags a seq scan and suggests the index.
- After adding the index → no warning.
- Analyzer is never invoked from the recompute/trigger path (assert hot path issues no `EXPLAIN`).
- Oracle: 4A degrades gracefully (Oracle plan format differs) or is PG-only and no-ops on Oracle.

**Risks.** EXPLAIN cost estimates depend on stats freshness; a warning may be a false positive on
a tiny dev DB. Mitigate: threshold on estimated rows, advisory-only (never blocks), opt-in.

---

### Workstream 6 — Schema Forge pipeline integration → **moved**

Split out to the lower-priority follow-up
[`PLAN-PHASE6-SCHEMA-FORGE.md`](./PLAN-PHASE6-SCHEMA-FORGE.md) (design items A–E, including the
new **F17** validator rule). Not required for the engine hardening in this file; schedule it
after Workstreams 1–4 and 7.

---

### Workstream 7 — Performance baseline (`etendo_core`, §7.6)

**Rationale.** There is no measured baseline for the recompute/enqueue/drain paths, so
regressions (e.g. a resolver change, an engine tuning change) can't be caught quantitatively and
tuning guidance in `OPERATIONS.md` is qualitative.

**Target repo:** `etendo_core` (harness lives in the gitignored `com.etendo.test` module — commit
it in that module's own repo, as Phase 5 established for its SQL scenario harness).

**Design.**

- **7A — What to measure.**
  1. **Sync `'S'` commit overhead** — median/p95 added latency on a business transaction that
     dirties N target rows (constraint-trigger recompute at commit) vs. the same transaction with
     the column removed.
  2. **Async drain throughput** — dirty rows/sec drained by `StoredColumnQueueProcessor` at
     `Max_Records` = 100 / 500 / 1000, single vs. two concurrent Process Requests (validates the
     `SKIP LOCKED` linear-scaling claim in `OPERATIONS.md`).
  3. **Rebuild time** — `ad_scd_rebuild(column)` wall-clock at 10k / 100k / 1M target rows
     (calibrates the `LARGE_TABLE_THRESHOLD = 100_000` enqueue-vs-inline decision).
  4. **`update.database` deploy time** — with vs. without Workstream 2's hash-skip, at 10 / 100
     stored columns (quantifies the incremental-regen payoff).
  5. **Health sweep cost** — `ad_scd_check_all()` wall-clock vs. number of columns/table sizes
     (bounds Workstream 1's schedule).
- **7B — Harness.** Seed-data generator (parameterized target-table sizes) + a JUnit/OBBaseTest
  driver that times each scenario over K iterations and reports median/p95, run on PostgreSQL and
  Oracle. Emits a CSV/markdown baseline table checked into the `com.etendo.test` module.
- **7C — Regression gate (optional).** A CI job that fails if a scenario regresses beyond a
  tolerance vs. the committed baseline — enable once the baseline stabilizes.

**Effort:** M–L (4–6 d) — seed generation + timing harness + a first baseline capture.

**Tests (delegate to `test-generator`):** the harness *is* the test artifact; deliverable is the
baseline table + the driver. Assert the driver runs deterministically and produces stable
medians across repeated runs on a fixed dataset.

**Risks.** Sandbox timing artifacts (see MEMORY: psql `\timing` over-reports 100s+ here) — use
`EXPLAIN ANALYZE` / `/usr/bin/time` / in-process `System.nanoTime`, never `\timing`, and run on a
real DB host for the published baseline.

---

## Settled design decisions (Phase 6)

- **Health-check output is a status table + AD alert, not log-only.** Queryable, historical,
  drives notifications (Workstream 1B/1D).
- **Incremental-regen hash lives in the generated object's comment, not a side table**
  (`REQUIREMENTS.md` §5.2) — survives dump/restore, can't drift (Workstream 2B).
- **The generator version is part of the hash** so a generator upgrade forces a full redeploy
  (Workstream 2A).
- **EXPLAIN analysis is diagnostic and opt-in, never on the recompute hot path** (Workstream 4B).
- **Health-check repair is an explicit opt-in parameter** (`Auto_Repair`, default `'N'`): the
  process reports by default and only enqueues rebuild sentinels when an operator turns it on
  (Workstream 1C). Decided per user — reporting is the safe default, repair is available on demand.
- **The perf harness lives in `com.etendo.test`** (gitignored module, its own repo), like the
  Phase 5 SQL scenario harness (Workstream 7B).

---

## Open design questions

None open for the engine/core workstreams. (The one Schema Forge question — F17 enforcement
without DB access — now lives in
[`PLAN-PHASE6-SCHEMA-FORGE.md`](./PLAN-PHASE6-SCHEMA-FORGE.md).)

---

## Tests — delegate to `test-generator`

Every workstream's test bullets above are delegated to `test-generator`. No test is written by
the implementing agent directly. Java/engine tests use JUnit/OBBaseTest and the `com.etendo.test`
SQL scenario harness. (Schema Forge test delegation is covered in its own follow-up plan.)

---

## Recommended implementation order

**Independent (parallelizable) — no cross-workstream dependency:**

- **Workstream 1** (health checks) — depends only on the shipped `ad_scd_check`.
- **Workstream 7** (perf baseline) — should start early so it can baseline *before* Workstream 2
  changes behavior, giving a before/after comparison.

**Sequenced:**

- **Workstream 2** (incremental regen) → **Workstream 3** (pre-commit hook). W3 needs W2's
  canonical hash + manifest; do W2 first.
- **Workstream 4** (EXPLAIN/index advice) is best merged **after Workstream 1**, since 4B wires
  into the `StoredColumnHealthCheck` process W1 creates. 4C/4D (index-existence lint) can start
  independently and land into W1 when ready.

**Suggested wave plan:**

- **Wave A (parallel):** W1, W7, W2.
- **Wave B:** W3 (after W2), W4 (after W1).
- **Later (lower priority, separate repo):** Schema Forge pipeline integration —
  [`PLAN-PHASE6-SCHEMA-FORGE.md`](./PLAN-PHASE6-SCHEMA-FORGE.md).

## Exit criteria

- A scheduled health check reports per-column staleness into a queryable AD table and raises an
  alert on drift (W1).
- `update.database` skips redeploy of unchanged stored columns via an embedded metadata hash;
  a dev-time hook catches metadata edited without regeneration (W2, W3).
- Unindexed filter/sort columns behind resolvers/computations are surfaced as advisory warnings,
  never on the hot path (W4).
- A committed performance baseline exists for sync overhead, async throughput, rebuild, deploy,
  and sweep, on PostgreSQL and Oracle (W7).

## Commit

```
Epic EPL-1807: Plan Phase 6 deferred-item hardening for stored computed columns
```
