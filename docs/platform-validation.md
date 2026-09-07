# Platform validation

## Purpose

This executable extraction proof lives inside etendo_core. It reuses Java 17,
Gradle, DBSM XML, PostgreSQL, the real dictionary, entity generator, OBDal,
Hibernate mappings, security context, interceptor, and PostgreSQL dialect.
It does not install ERP business modules or replace the DAL with mocks.

Warehouse and BusinessPartner are explicitly accepted v1 compatibility entities.
Their selected identity/audit fields remain; their business workflows are not
installed. Removing these dependencies is deferred to a later extraction.

## Run the complete proof

From the repository root, with JDK 17, Docker, and a local postgres:16 image:

```sh
JAVA_HOME=/path/to/jdk-17 ./gradlew -p platform-validation verifyPlatform --console=plain
```

The existing Gradle wrapper runs without applying the root ERP plugins.
Supply `-PdbsmJar=/absolute/path/dbsm-1.2.0.jar` if the single cached 1.2.0 artifact
is unavailable. Other dependencies are declared explicitly in the standalone build.

The command creates disposable PostgreSQL containers on random loopback ports,
uses random passwords and temporary storage, and removes its containers afterward.
It never reads the existing Openbravo.properties or connects to existing databases.
Temporary properties files have owner-only permissions and are deleted afterward.
Generated Java, XML, classes, and logs remain under platform-validation/build.

## Acceptance checks

| Requirement | Executable evidence |
| --- | --- |
| XML schema and initial data | DBSM creates the projected schema and imports dictionary, security, and managed category XML into empty PostgreSQL. |
| Real dictionary and generated Java | ModelProvider resolves metadata; GenerateEntitiesTask emits eighteen entity classes, compiled with existing DAL sources. |
| Real non-admin context | OBContext loads the fixture user, role, client, organization, language, and organization tree. |
| OBDal persistence and HQL | v1 creates a local category and an operational request referencing the XML-managed category, commits, and queries the relationship with a named parameter. |
| Isolation | Default queries see only the current client's allowed organization and active rows. Separate control queries disable filters and expose the expected additional rows. |
| Restricted permissions | Missing, excluded, and inactive grants deny access; read-only grants allow reads but deny writes. |
| Rollback | An OBDal insert is flushed, rolled back, and absent when queried from a new session. |
| Physical constraints | PostgreSQL rejects invalid foreign keys with SQLSTATE 23503 and null required titles with 23502, before and after upgrade. |
| Schema and dictionary v2 | DBSM adds DESCRIPTION from XML and reconciles its AD_COLUMN row using DataComparator and alterData. |
| Managed data v2 | DBSM renames GENERAL and inserts IT from XML, preserving categories outside the managed dataset. |
| Operational preservation | Every original request field is compared before and after schema/data migration and the DAL restart. |
| Actual generated v2 code | The real generator runs again; javac compiles v2 entities. A separate JVM loads them first, invokes generated description accessors, commits with OBDal, and queries the new property. v1 asserts that accessor is absent. |
| Idempotence | Repeating schema, dictionary, and managed-data reconciliation yields zero deltas. Another v2 JVM runs successfully; canonical snapshots of every table remain identical. |

Results are reported only after the disposable database is removed:

- `build/platform-result.txt`: complete lifecycle status.
- `build/platform-v1.log`: persistence, rollback, filters, and permissions.
- `build/platform-v2.log` and `build/platform-v2-repeat.log`: upgraded DAL checks.
- `build/platform-v2-schema.xml`, `build/platform-v2-data.xml`: upgrade inputs.
- `build/upgraded-entities`, `build/upgraded-classes`: actual v2 generated code.

The lifecycle passed on 2026-09-06 with Corretto 17.0.18, Gradle 8.12.1, DBSM 1.2.0,
Hibernate ORM 6.5.2.Final, PostgreSQL JDBC 42.7.8, and PostgreSQL 16.
Run duration is a smoke-test observation, not a performance benchmark.

## Extraction boundaries

Technical columns derive from existing dictionary HBM mappings and physical DBSM
table definitions. Security/entity metadata is selected from core source data.
UI selector references for conventional foreign keys become equivalent TableDir
relations. Original user-default reference-table metadata is retained. The Process
descriptor comes from client.application because the access checker references
its Java type; no UI module is installed. Selected metadata belongs to the
fixture's core-compatibility module.

The eighteen entities are Category, Request, User, Role, UserRoles,
RoleOrganization, Client, Language, Organization, Warehouse, BusinessPartner,
TableAccess, Table, ClientInformation, Tree, TreeNode, OrganizationType, and Process.
Physical dictionary tables also exist but do not all become runtime entities.
This is a minimal projection, not a promise of unchanged legacy-module compatibility.

The complete proof never registers the manually annotated entities used by the
separate foundation probe. Its mappings, classes, SessionFactory, and security
checks are actual generated/core implementations.

## Production changes

- EntityAccessChecker supports `dal.security.tableAccessOnly=true`, an explicit
  opt-in policy without windows/selectors. Active table grants are subject to
  existing access levels; missing grants deny, exclusions win, and read-only
  restrictions prevail. Permissions are published only after their complete load
  succeeds. Existing client/organization enforcement remains active. The usual
  ERP window/process-derived policy remains the default.
- OBContext binds its system-language Boolean parameter. The previous literal
  produced invalid Boolean-versus-CHAR SQL with the existing Y/N type.

Configure the policy before bootstrap; runtime mode switching is not supported.
A full ERP regression suite has not been run. Hibernate HBM, development pool,
and missing Process-translation warnings remain visible.

## DBSM provenance

Source: [etendosoftware/dbsm](https://github.com/etendosoftware/dbsm).
Inspected main revision: `43b8e23561aab14b8e1ee9f726ca62b23899639a`.
The user's Jakarta reference resolves to `epic/ETP-2587-Y27` at
`d71f10be694eb0bb157e7096bd973f8bd99eaa4f`. Its difference from the inspected main
revision is DBSMOBUtil.java: connection/SSL handling and an OBException dependency.
Its SSLMODE constant is absent from the installed artifact.

Validated artifact: `com.etendoerp:dbsm:1.2.0`, SHA-256:
`d41733925c4e9369a82da21cf3714ae1ae91a1a97d6a1b8ef326aa7c61a78222`.
The proof does not claim source/artifact parity or that branch's SSL behavior.
The source build still depends on directories from etendo_core.

## WAR and Tomcat validation

Build: `./gradlew -p platform-validation war`.
Artifact: `platform-validation/build/libs/platform-validation.war`.
End-to-end check: `./gradlew -p platform-validation verifyTomcat` with JDK 17 and
the same disposable PostgreSQL prerequisites. Tomcat dependencies use Maven Central.

The test deploys this actual WAR into [Apache Tomcat 10.1](https://tomcat.apache.org/migration-10.1.html),
pinned to 10.1.59, using Catalina's embedded launcher in an isolated child JVM.
It is a real WAR/classloader deployment, not a servlet invoked directly or a
replacement HTTP server. The application classes exist only inside the WAR.
The connector binds a random loopback port. Existing Tomcat deployments are untouched.

The listener initializes the real DAL on deployment. GET and POST /platform/requests
use OBDal, named-parameter HQL, normal client/organization filters, and request-bound
transactions. Tests verify 201 creation, 200 lookup, 401 missing/invalid tokens,
403 restricted writes, 400 invalid input, and context isolation on a reused worker.
The WAR is stopped and redeployed; the committed row remains queryable. A separate
JDBC assertion confirms the HTTP request actually persisted in PostgreSQL.

Output: build/tomcat-result.txt and build/tomcat-http.log. Tomcat and PostgreSQL
are stopped afterward; no persistent demo URL is left running. The listener closes
the SessionFactory and deregisters only JDBC drivers owned by its WAR.

The WAR excludes the test runner and container-provided Servlet API. It includes
its logging configuration but no database properties or access tokens. Startup
requires the external JVM property `platform.validation.properties` to name a
compatible database configuration with `dal.security.tableAccessOnly=true` and
distinct `platform.validation.token` / `platform.validation.readOnlyToken` secrets.
The test generates these secrets and deletes the private configuration afterward.
The tokens map to fixture identities U1/R1 and U1/R_READ; this is deliberately a
validation authentication adapter, not a production login/authorization service.
Do not deploy it against production data. Schema provisioning remains a separate
DBSM step; the WAR does not silently run DDL on application startup.

## Production scope limits

This is functional platform-validation, not a production platform distribution.
It uses real DBSM schema/data primitives, not the root `update.database` task or
full DBUpdater/module-script lifecycle. It proves this additive migration and
repeatability, not arbitrary destructive migrations, DDL atomicity, crash recovery,
all custom HQL functions, all ERP modules, or production performance.

DBSM createTables requires explicit NOT NULL and foreign-key activation. The final
projected schema is serialized and reread so foreign-table identities match the
merged model. Dictionary NUMERIC-to-Integer mappings reject Hibernate's strict
validator; the proof preserves those types, disables automatic DDL, and checks
physical equivalence through DBSM ModelComparator instead.

The generator runs fresh: its incremental timestamp path expects metadata kinds
absent from this projection. Filter-control tests create a new OBQuery per
configuration because changing filters after execution retained stale parameters
in this checkout. These are documented boundaries, not hidden fixes.

Diagnostic commands remain: verifyPersistenceFoundation, verifyDictionaryBootstrap,
verifySecurityGeneration, verifyDalMappings, verifyOBDal. Only verifyPlatform
establishes the complete lifecycle above.
