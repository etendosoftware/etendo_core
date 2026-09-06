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

Nine assertions completed:

1. OBDal and an ERP Product class are absent from the runtime classpath.
2. DBSM creates exactly two application tables from XML in an empty database.
3. DBSM imports a category record from source-data XML.
4. Hibernate/Jakarta persists a request and executes parameterized HQL with a join.
5. A flushed Hibernate insert is absent after rollback.
6. PostgreSQL rejects a missing parent with foreign-key SQLSTATE 23503.
7. DBSM adds the v2 XML column while preserving the existing request.
8. Hibernate validates the upgraded schema and persists the new property.
9. DBSM reports no remaining model differences; repeating the schema update
   preserves operational rows.

The harness writes build/validation-result.txt (under platform-validation).
It uses manually annotated Java entities and the standard PostgreSQL Hibernate
dialect. No generated mappings, OBDal, custom Etendo SQL functions, or full module
system are substituted or mocked. XML data import is verified once; repeat-safe
managed-data reconciliation and managed-data upgrades are NOT yet verified.
Hibernate's built-in pool warning is expected in this disposable harness; it is
not a production pool recommendation.

## Findings that affect extraction

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
