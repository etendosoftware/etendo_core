# ET-27 exploration checkpoint

## Purpose and status

This work is parked for future continuation, not delivered as a complete extracted platform. Branch: `feature/platform-validation` (explicit user choice), Jira key: `ET-27`. Previous committed baseline: `3bf83458`, serving canonical UI browser resources.

The checkpoint preserves the pending original-UI bootstrap and authentication lifecycle work. No full compilation, database validation, ERP regression or browser CRUD acceptance was rerun while archiving it. Committing the work does not certify its behavior.

## Preserved changes

- `platform-validation/ui-runtime/PlatformShell.java`: experimental platform bootstrap using canonical application templates, formatting, resource ordering and widgets, with platform-specific branding and optional profile-field projection.
- `RetainedUiServer.java`: authenticated shell, bootstrap and type-script routes with request context initialization.
- `platform-validation/build.gradle`: bootstrap resources and canonical component compilation; shared layout overlay in the ERP UI WAR.
- Canonical `ob-layout.js`: `MODULE-BOUNDARY ui-startup` extension seam. An absent contribution list preserves the previous ERP heartbeat/registration call; the platform supplies an empty list. This still requires ERP regression.
- `UiHttpAuthenticationValidation.java`: pending assertions for system pre-authentication context, logout and shutdown session persistence using the original listener and connection pool.
- `browser-probe/inspect-platform-shell.mjs`: diagnostic browser probe. Class presence is explicitly not proof of widget initialization or editable-window functionality.

## What is not established

Do not claim the original platform UI can manage a custom entity end-to-end without Product, Warehouse or BusinessPartner. Generated components and delivered resources are intermediate evidence, not browser CRUD. Do not infer that all four compositions still pass from this checkpoint. The original profile documentation contains historical validation results and port assignments, not a current service inventory.

## Resume safely

1. Read this file, `platform-module-boundaries.md`, `platform-profiles.md` and `platform-impact-analysis.md`. Inspect Git status and current commits before changes.
2. Inspect existing owned containers, private fixture configuration and listening processes. Preserve databases and credentials. Port 8091 has been reused by different experiments; never assume the listener's identity.
3. With JDK 17 and the documented retained fixture prerequisites, start with `./gradlew -p platform-validation compileRetainedUiRuntime`. Resolve compilation before launching anything. Inspect task definitions before running database-related gates; use only disposable or explicitly owned validation databases.
4. Run the HTTP authentication lifecycle gate in its isolated fixture. Confirm login, cookie rotation, logout and shutdown deactivation without elevated session leakage.
5. Run `runOriginalUi` only against the verified retained instance, choosing an unused port. The diagnostic probe currently hardcodes 8091 and a fixture window ID; verify those targets before using it. Supply test credentials through environment variables, never source files.
6. Establish actual original-widget initialization and custom-entity create/read/update behavior. If continuation requires duplicating authentication, persistence or widgets, reconsider the extraction seam instead of growing a second implementation.
7. Regress the ERP UI startup fallback and the shared layout WAR overlay; rerun both headless packaging/classpath separation gates. Preserve public class/method signatures wherever possible.

Useful discovery: `rg -n 'UiHttpAuthenticationValidation|compileRetainedUiRuntime|runOriginalUi|verifyProfilePackaging' platform-validation/build.gradle`.

## Separate continuation: executable knowledge

The later research moved to `/Users/sebastianbarrozo/Documents/work/knowledge-runtime-mvp`. Its `docs/handoff.md` records consolidation and bounded business-knowledge extraction. That repository has since advanced independently beyond the original handoff commit `143d633`; consult its current state rather than resetting it to this historical baseline.

It is not a replacement platform implementation or an Etendo dependency. Its future source extraction should inspect Etendo read-only unless separately authorized. The Java impact analyzer at `/Users/sebastianbarrozo/Documents/work/epic/java-impact-analyzer` is another independent tool: structural paths and incomplete analyses do not prove dead code or runtime absence.

## Archive boundaries

This checkpoint intentionally excludes ignored builds, WARs, private configuration, credentials, database contents and running-process state. Preserve those locally where needed; reproduce artifacts from documented tasks. No service was stopped, database changed, or remote push performed for this archive. Uncommitted work in the independently active knowledge-runtime repository is outside this Etendo checkpoint and must be handled by its current owner/thread.
