# Platform validation

## Purpose

This executable extraction proof lives inside etendo_core. It reuses Java 17,
Gradle, DBSM XML, PostgreSQL, the real dictionary, entity generator, OBDal,
Hibernate mappings, security context, interceptor, and PostgreSQL dialect.
It does not install ERP business modules or replace the DAL with mocks.

The extraction now proceeds incrementally in the existing motor, not through a
second implementation inside this validation project. The first cut makes the user business-partner association
optional in OBContext and removes BusinessPartner from the minimal dictionary.
ERP dictionaries retain that association and its eager initialization behavior.
This is not yet a fully extracted core: accounting and process dependencies
still require explicit extraction. The runtime selector is deferred while these
dependency boundaries are addressed.

The first extraction regression runs `verifyPlatform verifyTomcat` without
BusinessPartner and `verifyClassicJson` against the external read-only Classic
database. The minimal dictionary explicitly rejects reintroducing the entity or
its User property. This validates the tested Product access path, not all ERP
processes or every non-null business-partner relationship. The metadata-based
initialization is a transitional compatibility seam, not the final extension API.

ERP context initialization is isolated in ErpContextSupport. The ERP build retains
the original OBContext Warehouse signatures. The minimal build generates a variant
from the same source, excluding only explicitly marked ERP sections. It omits
Warehouse from the dictionary and runtime; no second context implementation is
maintained. See platform-module-boundaries.md for variant isolation constraints.

## Run the complete proof

The minimal Request HTTP adapter supports authenticated GET, POST and PUT.
PUT requires an existing readable `id` and a validated `title`; table write
permissions are enforced by OBDal. `Accept: application/json` selects a JSON
response envelope, while the original text representation remains available for
compatibility. This remains a fixture-specific adapter, not the final generic
REST API or production authentication implementation.

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
| Real dictionary and generated Java | ModelProvider resolves metadata; GenerateEntitiesTask emits fifteen entity classes, compiled with existing DAL sources. |
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
relations. Original user-default reference-table metadata is retained. The UI
Process descriptor and generated Java class are no longer required by the minimal
access checker. Selected metadata belongs to the
fixture's core-compatibility module.

The fifteen entities are Category, Request, User, Role, UserRoles,
RoleOrganization, Client, Language, Organization,
TableAccess, Table, ClientInformation, Tree, TreeNode, and OrganizationType.
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
A full ERP regression suite has not been run. Hibernate HBM and development-pool
warnings remain visible.

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

## Existing Classic database and Product datasource milestone

The next acceptance target is the existing Product datasource behavior on this
WAR, backed by a database installed with Classic. This is not yet implemented.
The endpoint must use existing module services, not proxy Classic or fabricate
JSON. Acceptance includes selected fields, reference identifiers, pagination,
sorting, authorization, and client/organization isolation. Unsupported parameters
and installed-module fields must be reported explicitly.
The compatibility URL must retain the `/etendo` context and the exact path
`/org.openbravo.service.datasource/Product`. The local port is configurable:
use a separate port while Classic occupies 8080; use 8080 only when available.
Do not replace or stop the existing Classic deployment to claim URL compatibility.

Before DAL bootstrap, `verifyClassicDatabase -PclassicProperties=/absolute/path/to/Openbravo.properties`
performs a read-only JDBC preflight. It checks PostgreSQL, the Product dictionary
entry, the JSON/datasource module registrations, and product availability. It does
not execute sessionConfig, schema updates, module scripts, or application startup.
Credentials are loaded from the external file and are not included in the report.
The report is `build/classic-database-result.txt`; passing this preflight does not
prove that the module or HTTP endpoint works. The runtime profile will remain
separate from the disposable fixture profile and will not run automatic DDL.

`verifyClassicModel` additionally reads the real Classic dictionary with
ModelProvider and generates sources into `build/classic-generated-entities`.
Existing `src-gen` entities bootstrap a separate compatibility source set; freshly
generated classes take precedence in the verification JVM. The check requires
fresh generated source and a mapping class for every persistent entity, including
Product. Virtual datasource/HQL entities are excluded exactly as in the generator.
This deliberately retains the installed entity model for compatibility; it does
not claim to have extracted Product from all ERP entity dependencies.
The first successful database check found 656 dictionary entities and generated
636 persistent entity classes. The profile includes the real selector domain
implementations and their metadata mappings. A TreeDomainType HQL predicate now
binds its boolean parameter so the existing Y/N mapping performs conversion.
This validates dictionary bootstrap, not yet a DAL SessionFactory or HTTP service.

The next `verifyClassicDal` gate starts the real DalSessionFactoryController with
the full generated Classic model and runs Product HQL. It explicitly checks that
the DAL JDBC connection is read-only and rolls back the query transaction.
It does not establish role security or datasource HTTP compatibility by itself.

The Classic JSON compatibility source set compiles DefaultDataSourceService and
DefaultJsonDataService from the existing modules. Compilation dependencies will
be inspected before runtime wiring; no empty replacement services or proxy to
Classic may stand in for their fetch behavior.

Direct compilation exposed transitive references to Quartz, Jasper, and generated
SQLC utilities through shared Classic helpers. The initial compatibility layer
therefore accepts `-PclassicWar=/absolute/path/to/classic.war` as an explicit local
support artifact. Only `WEB-INF/classes` and `WEB-INF/lib` are extracted for
compilation; web.xml and property files are excluded. Listener classes may be
present, but no Classic listener registrations or deployment configuration are
activated. This is a broad legacy support layer, not a minimal platform dependency
claim. The local artifact inspected contains 3,110 class files and 219 library
JARs. Starting any subsystem or packaging this support requires separate runtime
wiring and verification; successful compilation alone is not module startup.
The current `compileClassicJsonJava` gate passes with the explicit support WAR.
It compiles both service implementations from repository sources; supporting
classes come from the supplied artifact. Runtime CDI, security and fetch behavior
remain to be verified before this support is packaged for HTTP use.

Standalone wiring will use explicit constructors for the same JSON/datasource
implementations, supplying a real CachedPreference and an explicit collection of
JSON action hooks. The existing no-argument CDI path remains unchanged. This is
dependency wiring, not a replacement fetch implementation; installed custom hooks
must be explicitly supplied or reported as unsupported in the standalone profile.

The user-approved next acceptance is a startup switch, `platform.runtime=classic`
or `platform.runtime=platform-core`, within the same WAR and with the same database
and datasource URL. WAR size is not an acceptance constraint. The selected mode
must control actual lifecycle/service initialization, not proxy requests or simply
rename endpoints. Platform mode must not activate Classic startup listeners or
schedulers. The switch is not yet implemented; both modes still require HTTP
comparison with equivalent credentials and request parameters.

Independent platform startup is a mandatory architectural requirement, even
though removing all ERP compatibility entities is deferred beyond this HTTP
milestone. Classic support must remain opt-in: `verifyPlatform` and `verifyTomcat`
must run without classicWar or classicProperties. Those existing minimal profiles
exclude Warehouse, BusinessPartner and UI Process, but do not
yet prove a completely ERP-free core. Final independence requires a standalone
build and startup using only an application-owned model, with no Classic artifact
or ERP entities. The runtime switch is not evidence of that independence.

The internal `verifyClassicJson` gate now exercises the original services with an
authenticated test user, explicit real preferences and dictionary cache, Product
window 140/tab 180, fetch flags, selected properties and reference identifiers.
The password checker does not invoke automatic hash upgrades. Credentials are
read from PLATFORM_TEST_USERNAME and PLATFORM_TEST_PASSWORD and are never written
to reports. The gate checks the selected client and rejects an incorrect password,
but does not yet prove full HTTP security, organization isolation, pagination
equivalence or custom module hooks. Its explicit hook list is currently empty.
The cache's module-development query uses a named boolean parameter to preserve
the existing Y/N database mapping under Hibernate 6.

The next HTTP gate packages a compatibility WAR with a platform-specific
descriptor, external read-only database configuration and the exact Product URL.
HTTP Basic credentials are checked against the existing password hash without
upgrading it. This local validation adapter is loopback-only, not production
authentication. Each request owns its preferences, dictionary cache and JSON
service instance; no request identity is stored in a global service singleton.

### Running the compatibility endpoint

Use JDK 17 and an existing Classic configuration and WAR:

```sh
./gradlew -p platform-validation runCompatibility \
  -PclassicProperties=/absolute/path/to/Openbravo.properties \
  -PclassicWar=/absolute/path/to/classic.war \
  -PplatformPort=8090
```

This foreground command starts a separate loopback Tomcat at the `/etendo`
context. Stop it with Ctrl-C; it does not stop Classic or PostgreSQL. The output
WAR is `platform-validation/build/libs/platform-compatibility.war`. This artifact
currently supports only the platform lifecycle; the classic/platform-core switch
remains pending. The compatibility support is intentionally broad and is not an
independent platform-core module.

The following curl prompts for the password rather than recording it in a file:

```sh
curl --user admin \
  'http://127.0.0.1:8090/etendo/org.openbravo.service.datasource/Product' \
  --data-urlencode '_operationType=fetch' \
  --data-urlencode 'windowId=140' \
  --data-urlencode 'tabId=180' \
  --data-urlencode '_startRow=0' \
  --data-urlencode '_endRow=100' \
  --data-urlencode '_sortBy=searchKey' \
  --data-urlencode '_selectedProperties=id,name,searchKey,client,organization,uOM,productCategory,taxCategory' \
  --data-urlencode '_noCount=true' \
  --data-urlencode '_noActiveFilter=true'
```

`verifyProductHttp` checks the running server using PLATFORM_TEST_USERNAME and
PLATFORM_TEST_PASSWORD supplied externally. It verifies authentication failures,
the exact route, JSON status, reference identifiers, ordering, inclusive-end
pagination, and rejection of mutations, raw where clauses and unsupported windows.
The first passing run returned 21 products for the automatically selected active
role and organization. This is not yet equivalent to the user's 40-row Classic
example: role/context matching and cross-organization tests remain pending.

Current boundaries: Product fetch only, window 140/tab 180, at most 101 rows,
HTTP Basic instead of Classic JSESSIONID, loopback-only access, no automatic hash
upgrade, no custom JSON action hooks, and no general login/lockout/session service.
The adapter rejects unknown parameters; null @Product.*@ context placeholders are
accepted as inert. The request parameter allowlist is explicit in ProductServlet.
Full selected-property parity, arbitrary criteria, explicit role selection and
production authentication are not established by this check.

The security adapter accepts explicit X-Platform-Role and X-Platform-Organization
headers. Authentication and authorization are separate: a valid password does not
authorize an arbitrary role or organization. Requested roles must be active user
memberships and requested organizations must be active role grants. Absent headers
prefer the user's configured defaults, then a deterministic permitted context.
Product table access remains checked by the existing entity access checker.
Response headers expose the selected role, client and organization IDs so callers
can repeat the same context. The current gate verifies explicit-context replay
and rejects unknown role/organization IDs with HTTP 403 after authentication.

After preferring the user's configured default role, the test user receives 40
products. `verifyProductDatabase -PclassicProperties=/absolute/path/to/Openbravo.properties`
compares the running endpoint with independent read-only JDBC queries: all 40
Product IDs, organization IDs, names and search keys match the complete test-client
set, and 40 foreign-client control products are excluded. This gate deliberately
requires the test administrator to see the complete client dataset (at most 101
products); it is not a generic expected-result rule for restricted roles.
Restricted-organization checks and Classic lifecycle comparison remain pending.

The restricted-organization gate uses existing user-role memberships and computes
readable organizations independently with JDBC: direct grants plus ancestors and
descendants in the client's organization tree (or all client organizations for a
root grant). It requires a role exposing a nonempty proper subset of the client's
products, compares exact HTTP IDs, and verifies that denied context requests do
not change the subsequent administrator result. No fixture permissions are added
to the existing database.
The gate passed for six existing restricted roles: each returned exactly its
independently computed 19- or 21-product subset, excluding the other same-client
products. The administrator's complete 40-product set was restored afterward.
Classic lifecycle comparison remains pending. Classic startup includes scheduler
initialization and process-run updates, so testing that lifecycle must use an
isolated database copy rather than the user's original database.

`prepareClassicCopy` creates a logical snapshot with pg_dump using a read-only
source connection and restores it into an owned, loopback-only postgres:16
container. The source database is not altered. The private copy configuration
sets background.policy=no-execute and uses fresh database credentials. Its
directory is mode 0700; credentials and dump files are never versioned. The dump
is deleted after restore. The copy stays running for lifecycle comparison and
must be stopped explicitly using its reported container name when finished.

```bash
./gradlew -p platform-validation prepareClassicCopy \
  -PclassicProperties=/absolute/path/to/Openbravo.properties
docker stop <reported-owned-container-name>
```

This helper requires Docker Desktop host networking through
`host.docker.internal` and a locally available `postgres:16` image. Both dump
and restore use the PostgreSQL 16 utilities from that image; the source must be
compatible with that client version. It does not invoke database migrations.
The verified Classic snapshot restored 80 persisted Product records.

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
