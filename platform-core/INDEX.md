# Shared platform core

Standalone Java library build for implementations incrementally extracted from
the existing motor. Currently contains only ReadableScopeResolver. This is not
yet a runnable platform and does not replace platform-validation acceptance tests.

The build selects canonical source files from ../src without copying them and
enforces a JDK-only boundary. ERP, UI and legacy facade classes must not be added.
See [module boundaries](../docs/platform-module-boundaries.md) for ownership,
API compatibility and the remaining four-profile acceptance criteria.
