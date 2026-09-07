# Secure application services

Original login, session initialization, servlet security and SQLC queries.

- `LoginUtils` retains its existing module-facing API and authorization checks.
- `LoginSessionSupport` selects a trusted, application-scoped business contribution.
- `ErpLoginSessionSupport` owns warehouse, approval and accounting initialization.
- `PlatformLoginSessionSupport` explicitly excludes that business contribution.

The platform implementation does not replace authentication. Remaining SQLC,
servlet and deployment dependencies are tracked in `docs/platform-module-boundaries.md`.
The shared role-session projection uses parameterized Hibernate without ERP
approval/currency or User business-partner columns; legacy SQLC APIs remain intact.
Profile warehouse defaults and id/name/organization option projections use the
same session contribution. The ERP adapter retains the original DAL assignment
and HQL predicates; platform contributes empty options and rejects nonempty defaults.
