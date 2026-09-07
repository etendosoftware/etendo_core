# Persistence extraction feasibility

## Accepted foundation

Preserve Java, Gradle, XML schema and managed data, PostgreSQL, OBDal, Hibernate
queries, update.database, and modular applications without ERP business features.
Prepare extraction inside etendo_core. All new code and documentation is English.
The earlier command/HTTP prototype in a temporary directory is superseded.

## First executable slice

Run the existing DBSM 1.2.0 artifact and Hibernate 6.5.2.Final against a new,
disposable PostgreSQL 16 container. Do not load Etendo application classes or read
Openbravo.properties. The standalone Gradle build in platform-validation uses the
existing wrapper without applying the root Etendo plugins.

Check XML schema creation, XML reference data import, a relationship query,
transaction rollback, foreign-key enforcement, and a schema upgrade preserving
operational data. Hibernate must validate the schema, never create or update it.

This is a component-level feasibility check, not the full update.database task.
It does not establish OBDal bootstrap, generated mappings, client/organization
filters, module installation, custom HQL functions, or full ERP compatibility.
Those remain mandatory follow-up gates.

## Source investigation

The user identified https://github.com/etendosoftware/dbsm as the artifact source.
Inspected main revision: 43b8e23561aab14b8e1ee9f726ca62b23899639a. Its build currently
depends on directories from etendo_core rather than declaring all dependencies.
DBUpdater unconditionally constructs ModuleScriptHandler even when module scripts
are disabled. DatabaseUtils also references OBPropertiesProvider and SystemService.
Therefore a separate JAR does not yet prove full build/runtime independence.
The artifact version is pinned; source HEAD has not been proven byte-identical to it.

The user's Jakarta reference resolves to branch epic/ETP-2587-Y27 at
d71f10be694eb0bb157e7096bd973f8bd99eaa4f. Against the inspected main revision, its
only changed file is DBSMOBUtil.java (connection/SSL handling and an OBException
dependency). Its public SSLMODE constant is absent from the installed artifact.
Therefore this run validates the installed 1.2.0 artifact with Hibernate/Jakarta,
not a source build of that branch or its SSL behavior.

Validated artifact SHA-256:
`d41733925c4e9369a82da21cf3714ae1ae91a1a97d6a1b8ef326aa7c61a78222`.

## Run

Use JDK 17 and a running Docker daemon with postgres:16 already available:

```sh
./gradlew -p platform-validation verifyPersistenceFoundation
```

The build accepts -PdbsmJar=/absolute/path/dbsm-1.2.0.jar. By default it locates the
single cached 1.2.0 artifact in the Gradle user cache. Other dependencies use Maven
coordinates matching the existing core where applicable. The container uses a
random loopback port and temporary storage and is stopped after the run.

## Result

PASS on 2026-09-06 with Corretto 17.0.18, Gradle 8.12.1, DBSM 1.2.0,
Hibernate ORM 6.5.2.Final, PostgreSQL JDBC 42.7.8, and a disposable postgres:16
container. The successful Gradle invocation took 6 seconds on this environment;
this is a smoke check duration, not a performance benchmark.

Twelve checks completed:

1. OBDal and an ERP Product class are absent from the runtime classpath.
2. DBSM creates exactly two application tables from XML in an empty database.
3. DBSM imports a category record from source-data XML.
4. Hibernate/Jakarta persists a request and executes parameterized HQL with a join.
5. A flushed Hibernate insert is absent after rollback.
6. PostgreSQL rejects a missing parent with foreign-key SQLSTATE 23503.
7. PostgreSQL rejects a null required title with SQLSTATE 23502.
8. DBSM adds the v2 XML column while preserving the existing request.
9. Hibernate validates the upgraded schema and persists the new property.
10. DBSM reports no remaining model differences; repeating the schema update
   preserves operational rows.
11. DBSM DataComparator and alterData update a managed category and insert another
    from v2 XML, preserving a local category outside the dataset and both requests.
12. Repeating managed-data reconciliation produces an empty delta and preserves
    category and request counts.

The harness writes build/validation-result.txt (under platform-validation).
It uses manually annotated Java entities and the standard PostgreSQL Hibernate
dialect. No generated mappings, OBDal, custom Etendo SQL functions, or full module
system are substituted or mocked. Managed-data reconciliation is verified for the
explicit category dataset, not the full application dictionary or module lifecycle.
Hibernate's built-in pool warning is expected in this disposable harness; it is
not a production pool recommendation.

## Findings that affect extraction

Reference-data reconciliation uses DBSM DataComparator and alterData rather than
custom upserts. The fixture explicitly owns GENERAL and IT through an OBDataset
secondary predicate. LOCAL and the operational table are outside its scope.
Real modules must declare their own ownership; treating all rows as managed could
delete user-owned data. This remains distinct from full update.database.

- DBSM's createTables alone does not activate foreign keys. Follow its explicit
  NOT NULL and foreign-key activation steps; a successful table creation is not
  sufficient evidence of consistency.
- The isolated build needs explicit dependencies previously supplied indirectly,
  including commons-collections4 and Apache ORO for XML data conversion.
- The artifact includes logging configuration that references OBRebuildAppender.
  The harness supplies its own logging configuration without that core class.
- Keeping the full DBUpdater, module scripts, and dictionary bootstrap independent
  still requires mapping their transitive dependencies. This check does not run
  the top-level update.database command or prove its guarantees.

## Validation verdict and next gate

The XML/PostgreSQL/Hibernate component boundary is executable without ERP business
classes. Selective reuse is technically supported by this result. The full
platform extraction remains unproven, particularly OBDal and module semantics.

Next: characterize the OBDal bootstrap dependency closure, identify the minimum
dictionary/security dataset, then repeat persistence and upgrade checks using
real OBDal and generated mappings. Preserve existing Hibernate versions while
establishing that baseline. Treat the Jakarta branch and artifact provenance as
separate checks before claiming source-build compatibility.

## Dictionary bootstrap compilation

The bootstrap source set compiles the actual ModelProvider and GenerateEntitiesTask
from this checkout, resolving their Java source dependencies from src and src-core.
Run `./gradlew -p platform-validation compileBootstrapJava` with JDK 17.
This compilation passes without generated ERP entity classes. It includes the
existing session factory wrapper, PostgreSQL dialect, and metadata classes.
Redisson is a compile dependency reached through the existing core; this does not
start Redis or establish that a Redis service is required for the bootstrap.

This is an intermediate build milestone. Database-backed dictionary initialization,
entity generation, and OBDal acceptance are still pending.

## Real dictionary and entity generation

`./gradlew -p platform-validation verifyDictionaryBootstrap` now passes with JDK 17.
It derives nine technical dictionary tables from the existing core HBM definitions
and DBSM physical table definitions, adds the two non-ERP tables, and imports a
minimal generated XML dataset. The physical schema is checked with ModelComparator.
The real ModelProvider resolves the two entities and their relationship, and the
real GenerateEntitiesTask produces Category.java and Request.java using the core
FreeMarker templates. No ModelProvider override or mock is used.

Results: platform-validation/build/dictionary-result.txt. Generated schema, data,
and Java sources are under platform-validation/build. The temporary properties
file has owner-only permissions and is deleted after the run along with the
disposable PostgreSQL container. Existing databases and core configuration files
are not used.

An existing compatibility detail was reproduced: Hibernate's strict schema
validator rejects NUMERIC dictionary fields mapped to Integer, including
AD_COLUMN.FIELDLENGTH. This probe keeps the original types, disables Hibernate DDL,
and validates physical schema equivalence through DBSM before reading metadata.
The foundation probe continues to exercise Hibernate schema validation for its
simple annotated entities. Hibernate HBM deprecation warnings remain visible.

Generated source compilation and DAL mappings still require the real security and
base-object dependencies. DalMappingGenerator references DalPropertyAccessStrategy,
which reaches BaseOBObject and generated Language/client/organization classes.
These are the next bootstrap boundary; this successful source-generation test
does not claim runtime DAL or access-filter validation.

## Accepted v1 compatibility scope

The user explicitly accepts retaining real generated Warehouse and BusinessPartner entities in v1
to reduce extraction cost. OBContext currently exposes this type and initializes
the user's default warehouse. This does not require implementing warehouse
business workflows. Removing that dependency is deferred until real DAL,
security, and upgrade checks pass; it must not be replaced by a fake entity.

The security-generation profile selects physical columns and corresponding source
dictionary rows from this checkout. It preserves original entity/package names
and primitive domain references, while excluding unselected ERP fields. Foreign
keys exposed through UI selectors are projected to TableDir references to the same
physical target, using existing core domain code. Selector behavior itself is not
part of this profile. Generation is an
intermediate check, not proof of a complete security context or DAL execution.

`./gradlew -p platform-validation verifySecurityGeneration` passes on disposable
PostgreSQL and now generates eighteen sources: Category, Request, User, Role, UserRoles,
RoleOrganization, Client, Language, Organization, Warehouse, BusinessPartner,
TableAccess, Table, ClientInformation, Tree, TreeNode, OrganizationType, and the
technical Process descriptor.
Warehouse and BusinessPartner retain only selected identity/audit fields for now,
not ERP workflows.
Outputs are in build/security-generated-entities and build/security-generation-result.txt.
Every modeled Java output is checked after fresh generation; stale generated files
cannot satisfy the check. The probe does not exercise incremental generation,
whose timestamp path currently expects metadata kinds absent from this minimal
dictionary. Existing baseline generation uses its separate output directory.

## Real DAL compilation and mapping bootstrap

`./gradlew -p platform-validation verifyDalMappings` passes with JDK 17. It first
generates and compiles the selected entities with the actual OBDal,
DalMappingGenerator, DalSessionFactoryController, and their source dependencies.
It then starts another disposable PostgreSQL, recreates the same dictionary,
registers every compiled entity with OBProvider, generates the real mappings,
and opens the real DAL SessionFactory. A parameterized HQL count query against
ProofRequest returns zero as expected in the empty application table.

The mapping-bootstrap milestone modified no production Java files. No manual ORM mappings or replacement DAL
classes are used. The actual controller supplies OBInterceptor, the Etendo
PostgreSQL dialect, and the sequence service contributors emitted by the generator.
Jandex is an explicit compile dependency for the existing metadata contributor.

The profile adds user defaults and access flags required by the real security
classes. TableAccess and Table support EntityAccessChecker. ClientInformation
supports OBContext. The Process descriptor is selected from
modules_core/org.openbravo.client.application source metadata because
EntityAccessChecker references its Java type. Selected metadata is assigned to
the fixture's core-compatibility module; no UI or ERP business module installation
or process execution occurs. Unselected fields, translation tables, and workflows
are not validated. Missing Process translation warnings remain visible.

Outputs: build/dal-mapping.hbm.xml and build/dal-mapping-result.txt. The SessionFactory
and disposable PostgreSQL are closed after the run. This milestone does not yet
prove OBDal persistence, a populated security context, access filters, or runtime
dictionary upgrades. Those remain the next acceptance checks, not optional work.

## Headless access policy

The next runtime check introduces an explicit opt-in table-permission mode,
`dal.security.tableAccessOnly=true`. The default ERP window/process-derived policy
remains unchanged. In this mode, active AD_TABLE_ACCESS rows grant read or write
access subject to the existing entity access-level check; missing grants deny
access, exclusions win, and UI-derived permissions are not inferred. Client and
organization enforcement remains in the existing DAL. This is production policy
code, not a permissive test replacement, and requires positive and negative tests.

`./gradlew -p platform-validation verifyOBDal` exercises the opt-in mode against
disposable PostgreSQL. The fixture imports security rows via DBSM XML, initializes
the actual OBContext for a non-admin user/role, and uses generated entities with
client, organization, and active properties. The real organization-tree provider
reads fixture tree/type rows. Control records belong to another organization,
another client, and an inactive record in the current organization.

The command checks OBDal persistence and parameterized relationship queries,
default client/organization/active filtering, deliberately unfiltered control
counts, rollback after flush, missing grants, read-only grants, excluded grants,
and inactive grants. No administrator-mode override is used by the test to
persist the application records. Existing internal initialization uses the core's
normal temporary admin scope. Output: build/obdal-result.txt.

Two production files now have narrowly scoped changes: EntityAccessChecker adds
the explicit table-only policy; OBContext binds its system-language Boolean
parameter instead of emitting a SQL Boolean literal against the existing CHAR
Y/N type. The latter fixes a PostgreSQL failure reproduced during actual context
initialization. The default ERP permission path remains the default, but a full
ERP regression run has not been performed. Configure the access policy before
DAL bootstrap; switching modes in a running process is not supported here.

The projected physical schema is serialized and reread before use so DBSM foreign
table objects refer to the final merged model. The importer's identity tracking
otherwise retained an earlier category table shape and deferred dependent rows.
Filter-control tests use a fresh OBQuery per configuration because changing
filters on an already executed query retained stale parameters in this checkout.

Remaining acceptance: schema and managed-data upgrades through the real generated
DAL mapping, operational-row preservation across that upgrade, and an idempotent
repeat. Foundation-only upgrade results do not satisfy that remaining DAL gate.
