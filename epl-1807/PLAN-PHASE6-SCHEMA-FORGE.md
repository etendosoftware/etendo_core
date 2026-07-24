# EPL-1807 — Phase 6 (later step): Schema Forge pipeline integration

> **Priority: LOW / later step.** This is a follow-up to the Phase 6 engine hardening in
> [`PLAN-PHASE6.md`](./PLAN-PHASE6.md). It is **split out on purpose**: the runtime already
> treats stored computed columns correctly (see below), so nothing here is functionally
> required — it is a *design-pipeline clarity + guardrail* improvement. Schedule it **after**
> the engine workstreams (1, 2, 3, 4, 7) land, or whenever the Schema Forge team has capacity.
> It shares no code with those workstreams and can be picked up independently by a different
> agent/repo.

This is a **design document only — no code, DDL, or AD data changes.**

---

## Why this is low priority (what already works)

Runtime already handles stored columns end-to-end, so there is **no user-facing bug** to fix:

- **`push-to-neo.js`** already queries `AD_Column.Computation_Mode` and, when `'S'`, forces
  `ETGO_SF_FIELD.Is_ReadOnly='Y'` regardless of curated visibility (the block at
  `pushFieldFromContract` / `isStoredComputed`).
- **NEO Headless** keys both `NeoFieldFilter` (strips the column from writes) and
  `NeoDiscoveryHelper` (renders the input disabled) off `IsReadOnly`. So a stored column is
  already read-only in the served API and the rendered SPA.
- **`resolve-curated.js` `virtualFields`** (~L496) already models "computed columns whose
  derivation logic lives in Java/SQL" as forced-`readOnly` virtual fields — the natural
  attachment point for a first-class `storedComputed` annotation.

This workstream only surfaces the classification **earlier and more explicitly** through the
design pipeline (contract + generated SPA affordance) and adds a **validator guardrail** so a
curated window can't silently mark a stored column editable. It does **not** change runtime
behavior, and **must not regress it**.

**Target repo:** `etendo_schema_forge`. **Cross-repo note:** the source signal
(`Computation_Mode='S'`) originates in `etendo_core` AD data; the pipeline reads it from the DB
during extract/push. **No `com.etendoerp.go` change.** Ships on the shared branch per
`docs/branch-workflow.md`; depends only on AD metadata already present in `etendo_core`, not on
Phase 6 Workstreams 1–7.

---

## Locked decisions (inherited)

Same as `PLAN-PHASE6.md` — in particular: `decisions.json` / AD metadata is the source of truth
(never hand-edit `contract.json` or generated output — fix generators); never invent IDs
(`make uuid`); kebab-case spec names.

---

## Design

- **A — Extract/resolve: annotate the field.** In `resolve-curated.js`, when the raw schema
  field carries `Computation_Mode='S'` (surface it through `extract-from-db.js` if not already in
  `schema-raw.json`), stamp `field.storedComputed = true` and force
  `visibility: 'readOnly'` with a `readOnlyLogic.reason = 'stored-computed'` (reusing the existing
  `readOnlyLogic.evaluable === false` / `readOnlySource: 'server'` path that `generate-frontend.js`
  already understands at `buildReadOnlyLogicPart`). This mirrors the existing `virtualFields`
  treatment of Java-derived computed columns (`resolve-curated.js` ~L496), so it needs no new
  generator concept — just a new source of the same annotation.
- **B — Contract: carry the flag.** `generate-contract.js` emits `storedComputed: true` and the
  server-side `readOnlyLogic` on the field so the contract is self-describing (an agent reading
  the contract knows the field is server-derived, never a form input).
- **C — Frontend: explicit affordance.** `generate-frontend.js` already emits `readOnly: true`
  and `readOnlySource: 'server'`; add a small badge/help affordance ("computed automatically")
  driven by `storedComputed` so users understand why the field is disabled. i18n keys added to
  **both** `en_US.json` and `es_ES.json` per the i18n guide.
- **D — push-to-neo: no change needed.** It already forces read-only from
  `Computation_Mode='S'`. Optionally assert consistency (warn if the contract marked a
  `Computation_Mode='S'` column editable — should be impossible after A).
- **E — New validator rule (next free number = F17).** Per the validator reference's
  "adding a new rule" process. **Note:** the reference doc documents up to F11, but the code has
  already grown to F16 (`ruleF12`…`ruleF16` in `validate-pipeline.js`), so the next free number is
  **F17**, not F12. F17 (**BLOCK**): a field whose backing `AD_Column.Computation_Mode='S'`
  (as recorded in `schema-raw.json` / contract metadata — the validator runs **without DB
  access**, so it must read the flag from committed artifacts, not the DB) must have
  `visibility: 'readOnly'` and `storedComputed: true` in the contract. If a stored column is
  marked `editable` (or missing the flag) in `decisions.json`/`contract.json`, F17 fires.
  Rule shape mirrors `ruleF12`: read the artifact, return `violation('F17', name, 'BLOCK', msg,
  fix)` or `null`; register it in the window-rules dispatch array; add a fixture under
  `cli/test/fixtures/pipeline-validator/` (one passing, one violating window); add tests in
  `cli/test/validate-pipeline.test.js`; and **update the rules table in
  `docs/pipeline-validator-reference.md`** (canonical — a rule that isn't documented there
  doesn't exist). While the flag is absent from older `schema-raw.json`, F17 emits
  `skipped: missing stored-computed metadata` (same pattern as F1/F2's missing-hash skip) so it
  doesn't block un-backfilled artifacts.

**Effort:** M (3–4 d): resolver/contract/frontend annotation + i18n + one validator rule with
fixture, tests, and doc update.

**Cross-repo/branch coordination.** The `Computation_Mode` signal must reach `schema-raw.json`
via `extract-from-db.js` — confirm the extractor already selects `AD_Column.Computation_Mode`
(add it if not).

---

## Settled design decisions

- **The next free validator rule number is F17** (code is at F16; the reference doc's F11 is
  stale), and it degrades to `skipped` until `Computation_Mode` is in committed artifacts
  (design item E).

---

## Open design questions

1. **F17 enforcement without DB access.** The validator can only see `Computation_Mode` if the
   extractor writes it into `schema-raw.json`. Is adding that flag to every artifact's raw schema
   (a backfill of existing artifacts) in scope, or does F17 ship in `skipped` mode until artifacts
   are re-extracted? This plan assumes ship-skipped-then-backfill (mirroring F1/F2's missing-hash
   rollout).

---

## Tests — delegate to `test-generator`

Per the Schema Forge repo's mandatory delegation rule, all tests go to `test-generator`
(Vitest / Node test runner, fixtures under `cli/test/fixtures/pipeline-validator/`):

- `resolve-curated.js`: a `Computation_Mode='S'` raw field resolves to `readOnly` +
  `storedComputed:true` + `readOnlyLogic.reason='stored-computed'`, even if `decisions.json`
  marked it editable.
- `generate-frontend.js`: emits `readOnly: true`, `readOnlySource: 'server'`, and the
  computed-badge affordance; i18n keys resolve in both locales.
- F17 fixtures: passing window (stored column → readOnly) yields no violation; violating window
  (stored column → editable) yields one BLOCK; artifact without the metadata yields `skipped`.

**Risks.** The validator has no DB access, so F17 can only enforce what's in committed artifacts;
if `Computation_Mode` isn't extracted into `schema-raw.json`, the rule can't see it. Mitigate:
the extractor change (design item A) is a prerequisite of E, and the rule degrades to `skipped`
until the flag is present.

---

## Exit criteria

- Stored columns are read-only + explicitly labelled end-to-end through the Schema Forge pipeline,
  enforced by validator rule **F17** (fixture + tests + documented in
  `pipeline-validator-reference.md`).

## Commit

```
Epic EPL-1807: Plan Phase 6 Schema Forge pipeline integration (deferred follow-up)
```
