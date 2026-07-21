# EPL-1807 — Review Plan: Full decomposed code review of the `epic/EPL-1807` branch

Branch: `epic/EPL-1807`. **Review only — no code changes in this plan.** Do not push.

## Context

The epic branch is ready for a full code review before it merges to `main`. A raw
`main...HEAD` three-dot diff reports **45 commits, 64 files changed, +20732 / -109**, but that
diff is **not** a clean EPL-1807 changeset — three independent features are braided on the same
branch. The review must be scoped to EPL-1807 only, decomposed into parallel finder lanes, and
consolidated into a single verdict.

### The braid (why the raw diff over-counts)

| Feature | What it is | Files | In this review? |
|---------|-----------|-------|-----------------|
| **EPL-1807** | Stored computed columns (AD feature + engine + build/DAL guards) | ~47 | **Yes — review target** |
| **ETP-3828** | `InitialOrgSetup` accounting hooks | 7 | **No — already reviewed under its own Feature PR** |
| **ETP-3519** | WebService `PATCH` support | 4 | **No — already reviewed under its own Feature PR** |

Because the two non-EPL-1807 features already passed their own Feature PR review, this review
**scopes to EPL-1807 commits only**. Recommendation: build the review diff from the EPL-1807
commits rather than a raw branch diff:

```bash
git log --grep "EPL-1807" --oneline main..HEAD          # enumerate the EPL-1807 commits
git diff main...HEAD -- <EPL-1807 paths>                 # or diff by the lane file lists below
```

Do not review by `main...HEAD` wholesale — it pulls in the excluded ETP-3828 / ETP-3519 files.

### Relationship to the in-flight Phase 5b review

A **Phase 5b review (Alex)** is already running, scoped to the uncommitted Phase 5b validator diff
(`StoredComputedValidator` + `ValidateStoredComputedColumns` + the `ColumnStoredComputedHandler`
refactor). It **must not be duplicated.** Lane 5 below **reuses that review's verdict** for the
validator subset and reviews only the remainder of its scope.

---

## Review decomposition

Six lanes: five parallel **finder** lanes (each a focused `alex` review agent) plus one
**synthesis** lane that runs last. Each finder lane owns a disjoint slice of the EPL-1807 surface
so the lanes can run concurrently without overlapping findings.

### Lane 1 — DB schema & DDL

| | |
|---|---|
| **Scope** | `src-db/database/model/tables/AD_COLUMN_COMP_DEPENDENCY.xml`, `AD_COMPDEP_WATCHED_COL.xml`, `AD_STOREDCOLUMN_DIRTY.xml`, `AD_COLUMN.xml`, `AD_WINDOW.xml`; `src-db/database/model/functions/C_INVOICELINETAX_INSERT.xml`, `C_ORDERLINETAX_INSERT.xml`; `src-db/database/model/triggers/AD_COLUMN_MOD_TRG.xml`; `src-db/database/model/excludeFilter.xml` |
| **Hunts** | Trigger re-entrancy / recursion-guard correctness; PostgreSQL/Oracle dialect parity; index & FK coverage; DDL correctness; `excludeFilter` scoping (are the generated `sf_*` objects correctly excluded from dbsm?) |

### Lane 2 — AD metadata / sourcedata

| | |
|---|---|
| **Scope** | The 18 files under `src-db/database/sourcedata/` (`AD_MESSAGE`, `AD_PROCESS`, `AD_REFERENCE`, `AD_COLUMN` reference data, etc.) |
| **Hunts** | Message keys in sourcedata match the code constants that reference them (esp. `ETGO_StoredComputedColDef`, `ETGO_CompDepTargetXor`); no orphan or duplicate UUIDs; correct module ownership; referential integrity of the reference data |

### Lane 3 — Model & codegen

| | |
|---|---|
| **Scope** | `src/org/openbravo/base/model/Column.java`, `Column.hbm.xml`, `Property.java`; `src/org/openbravo/dal/core/DalMappingGenerator.java`; `src/org/openbravo/base/gen/entity.ftl` |
| **Hunts** | `hbm` mapping correctness for the new columns/entities; generated-entity blast radius (does the `entity.ftl` setter-suppression touch only stored computed properties?); DAL codegen backward compatibility with existing `SQLLOGIC` virtual columns |

### Lane 4 — Recompute engine

| | |
|---|---|
| **Scope** | `src/org/openbravo/erpCommon/ad_process/StoredColumnQueueProcessor.java`, `StoredColumnRebuild.java`, `StoredColumnRecomputer.java` |
| **Hunts** | Locking / concurrency (`FOR UPDATE` / `SKIP LOCKED`); transaction handling + recursion guard (`my.scd_refreshing` GUC); PostgreSQL/Oracle parity; SQL-identifier quoting / injection surface; error handling & rollback semantics |

### Lane 5 — Build-time & DAL guards *(runs AFTER the Phase 5b review lands)*

| | |
|---|---|
| **Scope** | `src-util/modulescript/src/org/openbravo/modulescript/GenerateStoredComputedTriggers.java`, `EnforceStoredComputedReadOnly.java`, `StoredComputedValidator.java`, `ValidateStoredComputedColumns.java`; `build.xml`; `src/org/openbravo/event/ColumnStoredComputedHandler.java`, `ColumnCompDependencyTargetHandler.java`, `ADFieldStoredComputedHandler.java`; `src/org/openbravo/erpCommon/ad_callouts/SCD_TargetLinkColumn.java` |
| **Hunts** | Build-order / classpath assumptions (modulescript compiled before `src/`; `project.class.path` includes the modulescript `build/classes`); validator rule correctness (V1–V16); DAL guard coverage across **all** save paths (UI, web services, imports); callout behavior |
| **Reuse** | **Reuse the in-flight Phase 5b review's verdict** for `StoredComputedValidator` + `ValidateStoredComputedColumns` + the `ColumnStoredComputedHandler` refactor. Review only the remainder of the scope above; do not re-open the validator subset. |

### Lane 6 — Synthesis / integration *(runs last)*

| | |
|---|---|
| **Scope** | All lane reports + `epl-1807/*.md` plan docs |
| **Hunts** | End-to-end coherence (docs ↔ code ↔ DB tables ↔ message keys); half-wired paths (a table/column/message declared but never read, or referenced but never declared); dedup and rank findings across lanes; produce one consolidated verdict |

---

## Execution model

- **Lanes 1–4** run **now**, as four parallel `alex` review agents, one per lane, each given only
  its file list and hunt checklist.
- **Lane 5** **waits** for the in-flight Phase 5b review to land, then reviews the remainder of its
  scope and **inherits** the Phase 5b verdict for the validator subset. The already-running Phase 5b
  Alex is **not** duplicated.
- **Lane 6** runs **last**, after Lanes 1–5 have returned, and synthesizes all reports into a single
  consolidated verdict.

```
Lane 1 ─┐
Lane 2 ─┤
Lane 3 ─┼─▶ Lane 6 (synthesis) ─▶ consolidated verdict
Lane 4 ─┤
Lane 5 ─┘   (Lane 5 gated on the in-flight Phase 5b review)
```

### Two execution options

| | (a) LOCAL decomposed | (b) `/code-review ultra` |
|---|---|---|
| **What** | Spawn Lanes 1–4 now as parallel Alex agents; Lane 5 after Phase 5b; Lane 6 last | Cloud multi-agent adversarial review |
| **Trigger** | Coordinator-controlled | User-triggered and billed |
| **Best for** | Early review, coordinator-controlled scope | The **final merge-to-main gate** |
| **Coordinator can launch?** | Yes | **No** — must be triggered by the user |

**Recommendation:** run **(a)** now for an early, scoped pass; reserve **(b)** for the final
merge-to-main gate. The coordinator cannot launch (b) — surface it to the user when the branch is
ready for the merge gate.

---

## Verdict aggregation

- Each finder lane returns one of **APPROVE / APPROVE WITH NITS / REJECT**, with findings attached.
- **Lane 6 consolidates** the five lane verdicts (plus the inherited Phase 5b verdict) into a single
  branch-level verdict, deduplicating and ranking findings across lanes.
- Any **REJECT** finding routes back to the **Developer in the same worktree** for a fix, then
  re-enters the lane that rejected it. Per pipeline rules: **max 3 rejection cycles per phase**, then
  escalate to the user.
- The branch is review-clean only when every lane is APPROVE or APPROVE WITH NITS and Lane 6 issues
  a consolidated APPROVE.

---

## Out of scope

The following files are on the branch but are **deliberately excluded** — each already passed review
under its own Feature PR, and re-reviewing them here would duplicate that work:

| Feature | Excluded files | Why excluded |
|---------|----------------|--------------|
| **ETP-3828** — `InitialOrgSetup` accounting hooks | `src/org/openbravo/erpCommon/businessUtility/InitialOrgSetup.java`, `InitialOrgSetupAccountingContext.java`, `InitialOrgSetupAccountingHandler.java`, `InitialOrgSetupAccountingResult.java`; `src/org/openbravo/erpCommon/ad_forms/InitialOrgSetup.java`; 2 tests under `src-test/.../generalsetup/enterprise/organization/` | Already reviewed under its own Feature PR |
| **ETP-3519** — WebService `PATCH` support | `src/org/openbravo/service/web/WebService.java`, `WebServiceServlet.java`, `BaseWebServiceServlet.java`; `src-test/.../service/web/WebServicePatchTest.java` | Already reviewed under its own Feature PR |

The **Phase 5b validator subset** (`StoredComputedValidator`, `ValidateStoredComputedColumns`, the
`ColumnStoredComputedHandler` refactor) is also out of scope for a *fresh* review — its verdict is
**inherited** from the in-flight Phase 5b review (see Lane 5).

---

## Exit criteria

- The review diff is scoped to EPL-1807 commits (`git log --grep "EPL-1807"`), not a raw
  `main...HEAD` diff; the ETP-3828 and ETP-3519 files are excluded.
- Each of Lanes 1–5 has returned a verdict (Lane 5 inheriting the Phase 5b verdict for the validator
  subset), with findings attached.
- Lane 6 has produced a single consolidated verdict; every REJECT has been routed to the Developer
  and cleared (or escalated after 3 cycles).
- `/code-review ultra` (option b) is surfaced to the user as the final merge-to-main gate.
