# DAL runtime probe

DalMappingValidation registers compiled generated classes, uses the core mapping
generator and session factory controller, then executes parameterized HQL.
verifyOBDal additionally initializes the real context from XML security data and
checks non-admin persistence, client/organization/active filters, flushed rollback,
and restrictive table grants. verifyPlatform runs v1 and regenerated v2 classes in
separate JVMs, verifies the new accessor through OBDal and HQL, and repeats v2.
# Optional visual metadata runtime

UiDalValidation is compiled only by verifyUiDal alongside the newly generated
UI dictionary entities. It checks class origins, ERP class absence and real OBDal
queries across windows, tabs, fields and columns in a separate JVM.
The menu gate also invokes the canonical login role-session projection on that
database and verifies missing associations, bound parameters and inactive scopes.

`PlatformLoginSessionValidation` checks the original warehouse facade with explicit
ERP-free session support, no ERP runtime types, a SQL-rejecting connection provider,
stable application-scoped selection and fail-closed invalid configuration. This
does not exercise the full login servlet or replace browser acceptance.

`FullLoginValidation` runs original password hashing, failed-login locking,
defaults and full session initialization against the disposable UI dictionary.
It checks authorized session values and rejects invalid role/client/organization
selections without changing the existing context or CSRF token. It uses generated
original SQLC collaborators; browser login remains a separate acceptance gate.
The full login gate loads the original UserInfoComponent and RoleInfo from the
shared UI artifact and checks current role/organization, empty warehouse context
and preserved organization keys with empty warehouse option lists.

## HTTP authentication integration

`UiHttpAuthenticationValidation` is a disposable Tomcat harness for the canonical
default authenticator, database sessions and cookie-backed login variables. It is
not a deployed platform endpoint: its pre-authentication metadata context is a
test fixture identity, and it does not install the original session listener or
exercise logout/expiration cleanup. Never package this probe as application code.
