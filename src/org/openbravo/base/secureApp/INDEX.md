# Secure application services

Original login, session initialization, servlet security and SQLC queries.

- `LoginUtils` retains its existing module-facing API and authorization checks.
- `LoginSessionSupport` selects a trusted, application-scoped business contribution.
- `ErpLoginSessionSupport` owns the original warehouse and accounting initialization.
- `PlatformLoginSessionSupport` explicitly excludes that business contribution.

The platform implementation does not replace authentication. Remaining SQLC,
servlet and deployment dependencies are tracked in `docs/platform-module-boundaries.md`.
