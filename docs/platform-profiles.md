# Platform validation profiles

## Scope

ET-27 validates four compositions of the existing Java motor, not a replacement
ERP implementation. Run commands from the repository root with Java 17 selected
through JAVA_HOME. Gradle uses platform-validation as the independent build;
platform-core is included as a project dependency. Docker and an available
PostgreSQL container image are required for disposable database tests. Browser
probes additionally require locally installed Playwright and Chrome.

| Composition | Build task | Start task/options | Local URL |
| --- | --- | --- | --- |
| Platform REST | platformWar | runPlatform, port 8091 | http://127.0.0.1:8091/platform/requests |
| Platform static UI | platformUiWar | runPlatform, platformUi=true, port 8092 | http://127.0.0.1:8092/platform/ |
| Existing ERP UI | erpUiWar | runErpUi, port 8093 | http://127.0.0.1:8093/etendo/ |
| ERP REST Product slice | erpHeadlessWar | runErpHeadless, port 8094 | http://127.0.0.1:8094/etendo/org.openbravo.service.datasource/Product |

The user's Classic instance on 8080 is separate and must not be replaced.

## Prepare isolated databases

Create and retain the ERP-free fixture:

```bash
./gradlew -p platform-validation verifyTomcat -PkeepDatabase=true
```

After successful verification, build/platform-instance-location.txt under
platform-validation contains the private properties path and owned container
name. Use that path as PLATFORM_PROPERTIES below. Do not commit its credentials
or bearer tokens. The fixture uses temporary PostgreSQL storage: stopping the
container loses its data. Retention supports local interaction, not durable hosting.

For ERP, provide an existing Classic WAR and its generated source tree in src-gen.
Prepare a dedicated database copy from the original configuration:

```bash
./gradlew -p platform-validation prepareClassicCopy \
  -PclassicProperties=/absolute/original/Openbravo.properties
```

The snapshot reads the original database without installing or updating anything.
Use the private configuration reported in build/classic-copy-location.txt as
COPY_PROPERTIES. All full ERP lifecycle work must target that owned copy. The
ERP UI launcher checks its running container label and port before starting.
Do not run migrations against the original database; new migration/destructive
scope requires approval.

## Build and run independently

Set PLATFORM_PROPERTIES, COPY_PROPERTIES and CLASSIC_WAR to the appropriate
absolute paths. These variables contain paths, not passwords. Start each profile
in its own terminal:

```bash
./gradlew -p platform-validation platformWar
./gradlew -p platform-validation runPlatform \
  -PplatformProperties="$PLATFORM_PROPERTIES" -PplatformPort=8091
```

```bash
./gradlew -p platform-validation platformUiWar
./gradlew -p platform-validation runPlatform \
  -PplatformProperties="$PLATFORM_PROPERTIES" -PplatformUi=true -PplatformPort=8092
```

```bash
./gradlew -p platform-validation erpUiWar \
  -PclassicProperties="$COPY_PROPERTIES" -PclassicWar="$CLASSIC_WAR"
./gradlew -p platform-validation runErpUi \
  -PclassicProperties="$COPY_PROPERTIES" -PclassicWar="$CLASSIC_WAR" \
  -PcopyProperties="$COPY_PROPERTIES" -PplatformPort=8093
```

```bash
./gradlew -p platform-validation erpHeadlessWar \
  -PclassicProperties="$COPY_PROPERTIES" -PclassicWar="$CLASSIC_WAR"
./gradlew -p platform-validation runErpHeadless \
  -PclassicProperties="$COPY_PROPERTIES" -PclassicWar="$CLASSIC_WAR" -PplatformPort=8094
```

Stop a foreground profile with Ctrl-C in its launch terminal. For a detached
validation process, first inspect its listening port with
`lsof -nP -iTCP:8091 -sTCP:LISTEN` (substitute the intended validation port), then
inspect the returned PID with `ps -p PID -o command=`. Only after verifying the
expected validation launcher, WAR and port, use `kill -TERM PID`. Never use a
blanket Java/Tomcat kill command. Do not stop a shared database container while
another profile uses it, and do not clean build/ while retained configurations
or expanded deployments there are in use.

## Interaction

The platform UI accepts the fixture bearer token in its Access token field and
supports load, create and update for the application-owned Request entity.
The separate read-only token must reject writes. Tokens stay out of browser
storage. The REST profile uses the same API and data without UI assets:

```bash
curl --fail http://127.0.0.1:8091/platform/requests \
  -H "Authorization: Bearer $PLATFORM_TOKEN" -H 'Accept: application/json'
```

Supply PLATFORM_TOKEN privately from the retained properties. This fixture
identity mechanism is not production authentication or a generic REST framework.

ERP UI uses the original login/session. ERP headless uses the existing validation
Basic adapter and the original password verifier, with assigned-role and
organization checks. Supply credentials privately through the environment:

```bash
curl --fail http://127.0.0.1:8094/etendo/org.openbravo.service.datasource/Product \
  --user "$PLATFORM_TEST_USERNAME:$PLATFORM_TEST_PASSWORD" \
  --data-urlencode '_operationType=fetch' --data-urlencode 'windowId=140' \
  --data-urlencode 'tabId=180' --data-urlencode '_startRow=0' \
  --data-urlencode '_endRow=4' --data-urlencode '_sortBy=searchKey' \
  --data-urlencode '_selectedProperties=id,name,searchKey,client,organization'
```

The existing datasource uses inclusive end-row semantics. The headless adapter
is loopback-only and read-only; it does not enable arbitrary processes, custom
datasource hooks, criteria or writes. Do not infer complete module compatibility.

## Reproducible checks

```bash
./gradlew -p platform-validation verifyPlatform
./gradlew -p platform-validation verifyTomcat
./gradlew -p platform-validation verifyTomcat -PplatformUi=true
./gradlew -p platform-validation verifyProfilePackaging verifyPlatformNestedBoundary
./gradlew -p platform-validation verifyContextApi verifyLegacyContextLinkage \
  verifySharedCoreIntegration verifyErpNestedBoundary \
  -PclassicProperties="$COPY_PROPERTIES" -PclassicWar="$CLASSIC_WAR"
./gradlew -p platform-validation verifyProductHttp verifyProductDatabase \
  -PclassicProperties="$COPY_PROPERTIES" -PplatformPort=8094
node platform-validation/browser-probe/verify-platform.mjs "$PLATFORM_PROPERTIES"
node platform-validation/browser-probe/verify-erp-ui.mjs
```

Product checks require PLATFORM_TEST_USERNAME and PLATFORM_TEST_PASSWORD. The ERP
browser probe also requires PLATFORM_COPY_CONTAINER, the labeled copy name; its
independent expectations use a read-only Docker/psql transaction. Browser probes
are scoped to these local test profiles. The platform browser probe retains one
test-owned record per run; ERP role changes use default=false and do not modify
the user's saved defaults.

## Evidence boundaries

### Acceptance checkpoint: 2026-09-07

The deployment-profile validation milestone passed its consolidated regression
run: verifyPersistenceFoundation, verifyPlatform, verifyTomcat,
verifyProfilePackaging, verifyContextApi, verifyLegacyContextLinkage,
verifySharedCoreIntegration, verifyPlatformNestedBoundary and
verifyErpNestedBoundary. A separate verifyTomcat run with platformUi=true passed
HTTP checks, persistence and redeployment. Both live browser probes were rerun:
the minimal UI created, updated and reloaded a persisted record and rejected a
read-only write; the original ERP UI returned 40 products and matched independent
database expectations across six restricted roles. The ERP headless HTTP/JDBC
probe also passed its Product and six-role checks.

All four validation instances remain on the ports listed above. The live ERP
headless JVM loaded ProductServlet without the audited kernel servlet, template,
login/menu or Jasper entry points. Together with the nested archive checks, this
supports the tested headless composition; it is not an assertion that every
legacy dependency has already been extracted or eliminated.

ERP and UI are independent composition choices. The fourth composition is an
additional verified Product slice, not a claim that all ERP processes already run
headlessly. Full module compatibility, production authentication, broad ERP
workflow coverage and extraction of the remaining motor into separate artifacts
remain subsequent work, outside this deployment-profile validation milestone.

The latest ERP UI rebuild, including the shared datasource route constant, passed
the original browser-session Product contract and six-role database comparison
on 2026-09-07. All four validation ports were confirmed listening alongside the
separate Classic 8080 instance. These are local validation results, not a production
deployment certificate.

| Requirement | Verification |
| --- | --- |
| Real XML dictionary, generation, OBDal/Hibernate and PostgreSQL | verifyPlatform v1/v2 JVMs, parameterized HQL and persistence |
| No Product/Warehouse/BusinessPartner in minimal platform | DAL checks classes, dictionary, mappings and physical tables; nested WAR audit checks libraries |
| Transactions, isolation and robustness | Flushed-insert rollback, client/org/active filtering, denied grants, PostgreSQL FK/NOT NULL checks |
| XML upgrades preserve data and are repeat-safe | DBSM schema/dictionary/managed-data upgrade, operational rows and complete table snapshots, zero repeat deltas |
| Platform UI and REST share one backend | WAR backend byte equality and independent Tomcat HTTP/redeployment checks |
| Platform has no UI startup dependency | Client/UI packages absent from nested class audit; Jasper absent from actual container classpath; UI routes404 in REST |
| Existing ERP Product contract | Original browser login, selected fields/reference identifiers, ordering/paging, independent database equality and six restricted roles |
| ERP REST Product contract | Original services through adapter, HTTP/JDBC equality, six restricted roles, denied anonymous/invalid context requests, UI routes404 |
| Legacy API preservation | 74 OBContext declarations/descriptors plus a baseline-compiled caller executed against the refactored context |

This is a validation milestone, not completion of the entire platform extraction.
The shared platform-core JAR currently owns ReadableScopeResolver only. Most motor
code is still compiled from canonical existing sources; the ERP-free OBContext
variant removes explicitly marked ERP sections from that source, not a copied
implementation. The ERP adapter retains substantial legacy support libraries.
See [platform-module-boundaries.md](platform-module-boundaries.md) for actual
ownership, compatibility allowlists and remaining extraction work. Known-rule
audits do not prove every retained dependency is necessary, every module is
compatible, or production authentication/performance/readiness.
