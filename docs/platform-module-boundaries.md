# Platform module boundaries

## Status and rule

This is the extraction map for ET-27, not a claim that the modules already exist.
The production implementation remains in the existing source tree. Move behavior
once, preserve it with tests, and then change packaging. Do not copy engines into
platform-validation or make the shared core depend on a compatibility module.

Searchable source markers use `MODULE-BOUNDARY <id>` and refer to the entries
below. Each marker describes a destination and an unresolved dependency, not a
runtime flag or a permanent exemption from dependency checks.

## Compatibility invariant

Preserve existing module-facing fully qualified class names, constructors and
public/protected method signatures wherever possible. Preserve parameter and
return types, static/instance form, visibility, inheritance contracts and checked
exceptions. Prefer internal delegation and compatibility facades over visible API
changes. Source compatibility alone is insufficient: already compiled modules
also depend on JVM descriptors and class hierarchy.

The ERP profile must supply the legacy facade and its referenced ERP types. The
ERP-free profiles need not expose ERP-specific APIs, but must share the underlying
motor rather than fork its implementation. If preserving an API prevents a clean
boundary, document the conflict and request approval before introducing a visible
breaking change. Do not silently replace typed APIs with Object, generics or new
packages. Package moves must not leave callers without their original facade.

Platform-only alternative classes are an explicitly accepted fallback, not the
first choice. Prefer composition and adapters; when legacy ERP-typed signatures
prevent an ERP-free classpath, a separate platform facade may expose only platform
contracts. Keep the existing ERP facade and signatures intact. Both facades must
delegate to the same implementation of authorization, persistence and lifecycle
rules, with shared contract tests. Do not copy OBContext or maintain two security
engines. Prefer distinct class names; any same-name build variant requires explicit
packaging isolation and must never coexist on one runtime classpath. Document the
reason, profile ownership and supported API for each alternative before adding it.

The minimal OBContext build variant is generated from the canonical source by
excluding explicitly marked `ERP-COMPAT-BEGIN` / `ERP-COMPAT-END` sections. This
fallback is necessary because the Warehouse return/parameter JVM descriptors
cannot exist in an ERP-free classpath without retaining that entity. No context
logic is copied or independently edited. ERP compiles the original source with
all signatures; platform compiles the generated variant without Warehouse APIs
and initialization hooks. The two same-name classes must never share a runtime.
The build rejects malformed markers and residual Warehouse references. This is
a transitional compilation boundary, not a second security implementation.

Before each module extraction, compare the exposed API against the pre-extraction
baseline and run representative existing callers. Add binary linkage checks using
callers compiled against that baseline where packaging or hierarchy changes. These
checks are required work, not yet an implemented repository-wide compatibility gate.

`verifyContextApi -PclassicWar=/absolute/path/to/classic.war` compares OBContext's
public/protected declarations and JVM descriptors with the supplied Classic WAR.
It rejects removed or changed declarations, including the class declaration,
while allowing additions. The report identifies the baseline WAR by SHA-256.
This initial gate does not cover the whole repository, behavior, serialization,
reflection-only contracts or binary execution of third-party modules.

The representative binary linkage probe is compiled only against the supplied
Classic WAR classes/libraries, then execute that unchanged caller against the
refactored ERP context. Exercise construction, thread context and the Warehouse-
typed getters/setters. Verify the loaded context originates from the refactored
output rather than accidentally testing the baseline again. This supplements
descriptor comparison; it cannot establish compatibility of arbitrary modules.
`verifyLegacyContextLinkage` passed against the supplied baseline WAR and current
refactored context. It is excluded from the ordinary source set, so the test does
not silently recompile the consumer against the new API. The consolidated context
descriptor, shared-core integration, profile byte-equality and nested-WAR gates
also passed after the current extraction changes.

## Target ownership

| Destination | Owns | Must not require |
| --- | --- | --- |
| platform-core | Dictionary, generation, DAL, transactions, tenant/organization authorization, module contracts | ERP entities, UI services, compatibility implementations |
| platform-db-postgresql | PostgreSQL implementation of the persistence/database contracts | ERP workflows or UI |
| platform-dbsm | XML schema and managed-data reconciliation using the existing DBSM | UI startup or ERP installation |
| platform-compat-etendo | Legacy API facade and ERP-specific context initialization | Inclusion in the ERP-free profiles |
| platform-rest | Authenticated HTTP transport over shared services | UI modules or assets |
| platform-ui | Minimal UI consuming shared application services | ERP business model |
| etendo-erp | Existing ERP entities, processes and existing UI integrations | A second persistence engine |
| platform-validation | Fixtures, launch probes and acceptance tests | Ownership of production core behavior |

These names are intended Gradle module boundaries; they are not current artifact
coordinates. Existing packages may remain stable in the compatibility facade.

The initial `platform-core` Gradle project now compiles ReadableScopeResolver from
its canonical existing source location. It has no dependencies and disables
implicit source compilation; `check` verifies the resulting JAR depends only on
java.base using jdeps. This establishes a real build boundary for extracted code,
not an independently bootable platform yet. Add further shared implementations
only after removing their outward ERP/UI dependencies. The legacy build continues
to compile the same source until distribution assembly can consume the shared JAR
without duplicate classes. Never package both copies in one distribution.

The validation build consumes platform-core as a Gradle project dependency.
Its boundary check rejects local duplicate shared classes in the bootstrap and
DAL outputs. Both minimal and Classic-compatible runtimes must resolve the same
shared artifact before further core extraction is considered integrated.

```bash
JAVA_HOME=/path/to/jdk-17 ./gradlew -p platform-core clean build --console=plain
```

## Concrete extraction seams

| ID | Current code | Intended destination | Remaining coupling and separation gate |
| --- | --- | --- | --- |
| CTX-ERP-INIT | ErpContextSupport | platform-compat-etendo | Calls generated User/Warehouse and the existing SessionHandler; move behind the shared context extension contract, retaining initialization order. |
| CTX-LEGACY-API | OBContext Warehouse field and accessors | platform-compat-etendo facade | Public JVM signatures refer to Warehouse. Do not erase or replace these signatures without an explicit compatibility strategy and legacy caller tests. |
| CTX-SHARED | OBContext identity, authorization and session context | platform-core | Still mixed with ERP, accounting and servlet concerns; extract the shared state/lifecycle rather than maintaining two OBContext implementations. |
| SEC-READ-SCOPE | ReadableScopeResolver | platform-core | Pure client/organization scope rules; only JDK dependencies. OBContext adapts existing Role and organization-tree providers without changing its API. |
| SEC-UI-DIAGNOSTICS | EntityAccessChecker process-name diagnostics | Compatibility security policy | Use the generic DAL for optional process metadata, not a generated UI class dependency. Minimal table-access security excludes the Process entity; full window/process authorization remains to be separated. |

BusinessPartner is absent from the minimal dictionary and generated User model.
Its optional initialization still belongs to CTX-ERP-INIT when the ERP dictionary
defines it. Warehouse remains in the ERP facade but is excluded from the minimal
variant, dictionary and runtime. This does not remove all remaining ERP or UI
policy coupling from the shared motor.

## Profile composition and acceptance

`platformUiWar` layers only the minimal request UI assets over `platformWar`.
The REST WAR does not include those assets. Both use the same authenticated
Request servlet and generated DAL; UI code does not own persistence or permissions.
Browser interaction and durable profile startup remain separate acceptance gates.
Run `verifyTomcat` for the REST profile or `verifyTomcat -PplatformUi=true` for
the optional UI profile. Both execute the same authenticated persistence/update
checks and redeployment test; static UI routes must return 404 in REST and be
served in UI mode. These HTTP checks do not execute browser JavaScript.

For interactive validation, `verifyTomcat -PkeepDatabase=true` retains its owned
database and private properties only after all checks pass. Stopping the database
container removes its temporary data. Stop the reported container when finished.

With JDK 17, use the reported private properties path in separate terminals:

```bash
./gradlew -p platform-validation runPlatform -PplatformProperties=/absolute/private.properties -PplatformPort=8091
./gradlew -p platform-validation runPlatform -PplatformProperties=/absolute/private.properties -PplatformPort=8092 -PplatformUi=true
```

REST is at `http://127.0.0.1:8091/platform/requests`; the optional UI is at
`http://127.0.0.1:8092/platform/index.html`. The owner-only properties file contains
`platform.validation.token` and `platform.validation.readOnlyToken`. Enter the
appropriate token in the UI without committing or sharing that file. Tokens are
fixture-scoped; they are not an authentication design for production. Stop each
foreground server with Ctrl-C before stopping the database. Do not clean the
validation build directory while using its retained configuration or deployments.

Browser acceptance is reproducible with locally installed Playwright and Chrome:

```bash
node platform-validation/browser-probe/verify-platform.mjs /absolute/private.properties
```

The probe exercises actual page controls: authenticated load, create, edit,
reload with a read-only token, denied update and unchanged persisted title.
It rejects JavaScript errors and retains one clearly named test record per run
in the owned fixture database. It does not print or store the tokens. This passed
against the running UI profile; full production identity and ERP UI acceptance
remain outside this result.

1. ERP UI: shared core plus PostgreSQL/DBSM, compatibility, ERP and existing UI.
   Verify authenticated original Product behavior against persisted data.
2. Platform UI: shared core plus PostgreSQL/DBSM, application-owned model, REST
   and minimal UI. No Product, Warehouse or BusinessPartner classes or tables.
3. Platform REST: the same core and application model, without UI artifacts or
   startup dependencies. Verify authenticated CRUD, isolation and absent UI routes.
4. ERP REST: shared core, compatibility and ERP behavior without UI artifacts or
   startup dependencies. Verify the original Product REST contract and its
   authorization against persisted data. Business rules and process permissions
   currently hosted by UI modules must be separated, not discarded with the UI.

ERP inclusion and UI inclusion are independent composition choices. All four
combinations are required by the user; the original three-profile goal text is
supplemented by this requirement. No profile is complete merely because a route
is hidden. ERP headless must not fall back to weaker table-only permissions to
avoid extracting the existing authorization policy. Scope business-process
compatibility claims to actually tested operations.

For every separation, inspect the built runtime classpath/WAR and generated
schema as well as executing tests. A missing route, an unused class or a smaller
compile include list does not prove modular independence. Preserve XML upgrades,
operational data, rollback and idempotence checks in both ERP-free profiles.

## Immediate sequence

Nested artifact audit: inspect all class entries in WEB-INF/classes and every
WEB-INF/lib JAR, including multi-release entries. ERP-free artifacts must not
contain ERP business entity classes or Openbravo client/UI packages. The ERP
headless audit rejects known UI servlet/rendering entry points and JSP engine
classes even when hidden inside a dependency. Record artifact SHA-256 and class
origins in ignored build reports so audits are tied to exact bytes. These are
explicit boundary rules, not a semantic proof that every remaining library is
needed; retained legacy dependencies still require classification.
`verifyPlatformNestedBoundary` and `verifyErpNestedBoundary` passed, including
negative/positive rule controls for inner and multi-release class names. The
current artifacts contain 73 libraries / 31,532 class entries (platform) and 227
libraries / 81,003 class entries (ERP headless, after provider and legacy-screen removal). Reports are
`build/platform-war-boundary.txt` and `build/erp-headless-war-boundary.txt` under
platform-validation. Counts include third-party classes and are not a measure of
extracted core ownership or executed code. The ERP gate checks named UI entry
points, not all possible custom-module UI implementations.
The class-origin inventory exposed additional module UI resource providers under
com.etendoerp and com.smf, outside the initial Openbravo package filter. Exclude
component-provider implementations and explicit SmartClient/skin/UI packages from
the ERP headless assembly, and extend the nested auditor accordingly. Preserve
generated dictionary entities separately; package names alone do not make them
renderers. Re-run Product and organization authorization after this removal.
The strengthened audit initially rejected ten retained provider classes, exposing
a stale-archive issue: Gradle did not invalidate the ZIP for closure-filter policy
changes. The headless archive now declares build.gradle as an input. Rebuilding
removed all ten entries; the nested audit and restarted 8094 Product/HTTP/JDBC
checks passed, including all six restricted contexts. No removed provider was
added back to make the test pass.
The next explicit exclusions cover DataSourceComponent (visual datasource
generation), legacy Login/Menu screens and VerticalMenu. Keep LoginUtils,
authentication helpers and generated AD menu metadata: those are distinct from
screen/rendering entry points and may participate in security or dictionary
compatibility. Validate the Product slice after changing this boundary.
This exclusion passed the nested audit and the restarted 8094 HTTP/JDBC
regression: 40 visible client products, 40 excluded foreign-client products and
six restricted organization contexts. Login/Menu/VerticalMenu and
DataSourceComponent were not reintroduced. These removals affect only the
headless artifact; the ERP UI composition retains the original screen classes.

ERP-free regression must explicitly reject Product, Warehouse and BusinessPartner
runtime classes, generated mappings and physical tables. Do not infer their absence
from successful application CRUD or a small entity count. Apply the check inside
the real DAL validation so both v1 and XML-upgraded v2 runs enforce it.
The strengthened `verifyPlatform` passed: ERP types absent from classpath and
dictionary/mappings, ERP tables absent from user schemas, generated v1/v2 DAL
execution, PostgreSQL FK/NOT NULL enforcement and repeat updates with zero deltas
and preservation of every table snapshot. This validates the disposable minimal
model; it does not imply arbitrary ERP schema migrations have been tested.

ERP headless candidate assembly: reuse the existing Product adapter and original
DAL/JSON services, but remove client/UI implementation packages except an explicit
compatibility allowlist and generated dictionary entities. Generated metadata
whose historical package contains `client` or `userinterface` is not a renderer
and remains required by the existing ERP dictionary. Preserve those types rather
than deleting database metadata. The allowlist initially contains request context,
kernel utilities, system preferences, dictionary cache and selector constants.
Remove form/report/rendering implementation packages and use the JSP-free server.
Run the candidate on a separate port with read-only copy configuration. This is
not accepted as ERP headless until runtime, security and package audits pass;
custom datasource hooks and arbitrary ERP workflows remain outside the adapter's
tested contract. Any missing class must be classified before adding it back.
The first request exposed a real service-to-UI linkage: BaseDataSourceService
initialized its URL by loading DataSourceServlet, which extends BaseKernelServlet.
Move the shared route text into DataSourceConstants; preserve the servlet's
existing accessor and private backing field. The candidate excludes DataSourceServlet
itself, so a successful Product request cannot silently rely on that UI hierarchy.

The rebuilt candidate is running on 8094. `verifyProductHttp` and
`verifyProductDatabase` passed there: 40 client products, 40 excluded foreign-client
products and six restricted organization contexts (19 or 21 visible products).
Root, login, UI kernel and index routes returned 404. The packaging gate passed
for absent known UI entry points/public assets and present original DAL/services.
This is a verified read-only Product slice, not complete ERP workflow or module
compatibility. Nested dependency auditing and remaining compatibility-class
separation are still required; the allowlist is an explicit transitional boundary,
not a claim that all retained code is already a clean platform module.

Build and start with Java 17 (external paths are examples, never credentials):

```bash
./gradlew -p platform-validation runErpHeadless \
  -PclassicProperties=/absolute/owned-copy/Openbravo.properties \
  -PclassicWar=/absolute/classic.war -PplatformPort=8094
```

Use `erpHeadlessWar` to build without starting, or `verifyErpHeadlessPackaging`
for its packaging gate. With test credentials supplied through the process
environment, run `verifyProductHttp verifyProductDatabase` with
`-PclassicProperties=/absolute/owned-copy/Openbravo.properties -PplatformPort=8094`.
Stop the foreground launch with Ctrl-C; do not stop its database container when
other validation profiles still use the copy. The compatibility lifecycle forces
read-only PostgreSQL transactions even for the copy. Writes and UI actions are
not exposed by this candidate.

Container composition must also separate UI dependencies. Jasper/JSP belongs only
to the original ERP UI launch classpath, not the shared Tomcat probe or platform
REST launcher. Disable the default JSP servlet for non-JSP validation deployments
and verify HTTP persistence with Jasper unavailable. Static platform UI assets do
not require a JSP engine. This removes a container-level dependency; it does not
resolve the remaining ERP service dependencies on UI-module classes.
Both `verifyTomcat` and `verifyTomcat -PplatformUi=true` passed with this
composition: `JspServlet` unavailable, actual WAR startup, authenticated HTTP
persistence/update checks and redeployment. The live platform instances on 8091
and 8092 were restarted with the reduced container classpath and the same retained
database. Chrome create/update/read-only rejection passed on 8092; both APIs
returned the same record IDs and retained a pre-restart record. The 8091 REST
instance returned 404 for index.html and 401 for anonymous requests. Its live
runtime now uses the extracted identifier metadata as well. The separate ERP UI
instance remains unchanged by this platform-only restart.

`erpUiWar` assembles the existing ERP UI resources and deployment descriptor from
the supplied Classic WAR, overlaying the refactored motor/generated classes and
shared-core JAR. It excludes properties files except the inspected, non-secret
Quartz configuration, so database credentials must be supplied at
deployment. This is an assembly step, not proof of successful ERP startup. Runtime
verification must use the owned Classic copy with `background.policy=no-execute`,
`import.disable.process=true`, `cluster=false` and Redis integration disabled.
Do not run the full legacy lifecycle against the original Classic database.

`runErpUi` requires the private properties produced by prepareClassicCopy. Before
deployment its launcher verifies the running Docker container's copy label and
published loopback database port against those properties. It expands the WAR in
a private directory, writes deployment-only configuration there, disables
scheduler/import/cluster/Redis startup and uses a separate HTTP port (8093).
Original database configuration and the packaged WAR are not rewritten.
The guarded deployment reaches the existing `/etendo/security/Login` HTML on
8093, with Quartz initialized but not started. The launcher selects the validation
console logging configuration, avoiding the build-time OBRebuildAppender. The
restarted instance logged no ERROR/SEVERE or OBRebuildAppender errors during this
smoke test. Existing Hibernate and CDI fallback warnings remain.

`browser-probe/verify-erp-ui.mjs` uses the original login page in Chrome and its
session cookie to fetch the original Product datasource. It passed against the
owned copy with 40 persisted products, without the Basic compatibility adapter.
Supply `PLATFORM_TEST_USERNAME`, `PLATFORM_TEST_PASSWORD` and
`PLATFORM_COPY_CONTAINER` (the running labeled copy container name) through the
process environment, then run:

```bash
node platform-validation/browser-probe/verify-erp-ui.mjs
```

The expanded probe passed with 40 products: six selected scalar/reference fields,
reference identifiers, Product identity, absence of an unselected business field,
ascending/descending search-key order and two contiguous five-record pages.
It preserves the original inclusive `_endRow` semantics. An anonymous request
must return exactly the existing login redirect script (HTTP 200, JavaScript),
never Product data. This is legacy UI behavior, not a recommended headless API
authentication response. Full selected-property coverage and general ERP workflow
compatibility remain unproven on this deployment. ERP headless acceptance remains
pending.

The ERP session scope regression obtains independent expectations through a
read-only transaction in the explicitly labeled Classic-copy container. It passed
for 40 default-client products (including scalar value equality) and six restricted
roles, excluding foreign-client and out-of-scope same-client products. It switches
assigned roles using the existing user-profile action with `default=false`
(session only), compares Product IDs with organization grants and the organization
tree, then verifies that a fresh login still returns the original default set.
Business data, user defaults and grants are not changed to make this test pass.
The tested scope is this existing fixture and its assigned roles, not arbitrary
module-specific authorization policies or all possible role configurations.

The current headless extraction blockers are concrete service dependencies:
`BaseDataSourceService` uses `CachedPreference` and
`ApplicationDictionaryCachedStructures` from `client.application`, while
`DefaultDataSourceService` and `AdvancedQueryBuilder` use `client.kernel.KernelUtils`.
The latter also uses `RequestContext`. Dictionary metadata and authorization
must survive extraction; deleting their current UI-module locations is not a
valid headless implementation. Keep the original ERP UI HTTP regression as the
behavioral control while separating these dependencies.

First extraction: identifier nullability belongs to `base.model.Entity`, not the
UI kernel. Add the model operation and retain
`KernelUtils.hasNullableIdentifierProperties(Entity)` as a forwarding compatibility
method. JSON query construction will call the entity directly so this metadata
check cannot initialize the UI kernel singleton/CDI cache. Other KernelUtils and
RequestContext usages still require extraction; this step alone is not headless
completion. `verifyEntityMetadata` passed for empty, mandatory and optional
identifiers with `KernelUtils` absent from the test classpath. Both the legacy
facade and updated query service compile from source. `verifyClassicJson` passed
against the owned copy with the updated classes: 80 persisted products through
HQL and an 11-row original datasource fetch. The ERP UI WAR was then rebuilt and
its owned-copy instance restarted on 8093. The complete browser Product contract,
independent database equality, six restricted roles and fresh-login isolation
passed against that updated deployment. Expected `AccessTableNoView` errors were
logged for roles denied table access; no extraction-related startup failure was
observed. The two ERP-free live profiles were subsequently rebuilt/restarted and
verified as described in the container composition check above. The older 8090
compatibility exploration instance is not part of that restart or current proof.

Finish the ERP initialization boundary regression, then separate the legacy typed
context facade from shared context ownership. Address accounting and process/UI
type dependencies next. Promote the extracted implementation into independently
buildable Gradle modules only with enforced dependency direction and profile tests.
Keep the existing Product validation as a compatibility control throughout.
# Shared original UI extraction checkpoint

verifyUiDal compiles the optional generated visual entities against the actual
minimal DAL classpath and starts a separate JVM while its disposable PostgreSQL
database is alive. It must resolve window/tab/field relationships with real
OBDal/Hibernate, reject ERP entity classes and prove that UI entities load from
the new generated output. This runtime metadata gate precedes servlet/login and
browser UI wiring; it is not a substitute for the goal's browser CRUD checks.
The initial runtime test passed for all 20 generated entities, with OBDal reading
two windows and six Request fields through their generated tab/column/table
relationships. Visual metadata reads use the original administrative dictionary
access pattern; this does not yet validate end-user window access or UI CRUD.

The optional original-UI dictionary slice extends the existing security fixture
with AD_COLUMN, AD_WINDOW, AD_TAB, AD_FIELD and AD_WINDOW_ACCESS metadata selected
from canonical XML. Its generated sources have a separate output directory so
headless entity generation remains unchanged. verifyUiDictionary must create and
read that dictionary through PostgreSQL/DBSM and the original ModelProvider and
entity generator. This initial projection supplies two app-owned windows and
their tabs/fields; it is not yet a complete dictionary for original UI startup.

The first UI artifact, platform-ui-components.jar, compiles the canonical
ApplicationComponentProvider rather than taking its bytecode from the Classic
WAR. ERP UI packaging consumes it and excludes the old loose provider class.
This is an initial ownership boundary, not yet an independent UI engine: the
provider still compiles against legacy support APIs and the platform consumer
is pending. REST profiles must not contain this artifact or provider class.
verifyUiResourceContributions tests the compiled hook, its legacy resource flags
and order, and an alternative composition using the same provider implementation.
verifyUiArtifactPackaging compares the packaged JAR bytes with the build output,
rejects a loose provider duplicate in ERP UI, and rejects that artifact and class
in either headless WAR. These build checks do not replace ERP browser regression
after deployment or the still-pending platform original-UI startup.
The ERP browser regression must also wait for the original OB.MainView tab set
and SmartClient OBViewGrid class, and exercise an exact Product search-key filter.
An authenticated datasource response alone is insufficient UI startup evidence.
The first runtime packaging trial passed Product REST but failed original shell
initialization: application styles/layout were absent from generated resources.
The UI JAR therefore declares META-INF/beans.xml with annotated discovery, making
its application-scoped provider an explicit CDI bean archive. Runtime shell
verification, rather than Java class loading alone, gates this packaging change.
After the descriptor was added and ERP UI redeployed on the owned copy, the
strengthened browser test passed original shell initialization, exact Product
filtering, projection/references/paging and independent six-role data isolation.
This verifies the ERP consumer of the initial artifact, not yet the ERP-free UI
consumer or the full UI extraction.

The next acceptance target is original dictionary-driven UI operation for
application-owned entities without Product, Warehouse or BusinessPartner, using
the same UI artifacts as ERP. Both headless compositions must remain UI-free.

Initial source inspection found Warehouse dependencies in
UserInfoWidgetActionHandler, Product dependencies in CharacteristicsUIDefinition
and RemoveImagesEventHandler, and business-process scripts registered globally
by ApplicationComponentProvider. These must become explicit ERP contributions,
not empty entity stubs or a copied application frontend.

Run `node platform-validation/ui-dependency-inventory.mjs` for a deterministic
source inventory of model imports and direct web/js resource registrations in
the application, kernel and selector modules. This is a discovery tool, not a
transitive dependency proof: reflective loading, SQL, templates, dynamic resource
paths, login and servlet initialization still require separate analysis and real
runtime tests. It does not treat Organization or other shared security metadata
as ERP business entities. Run with `--self-test` to verify its matching rules.

The first extraction should separate ERP resource contributions from the shared
application provider while preserving the ERP resource ordering and public API.
The initial seam is a protected addApplicationSpecificResources hook at the exact
existing tail position. Its legacy default retains all seven registrations,
including permission recalculation, without changing order or Classic-mode flags.
This is a backward-compatible extension point, not yet the modular extraction:
the default implementation still belongs to the legacy provider. The platform
composition must explicitly supply its applicable contributions before it can
claim independence; permission recalculation must not be discarded merely because
it currently resides next to ERP scripts.
Then separate user-context ERP preferences and expand the minimal XML dictionary
for original login, menu, windows, tabs, fields and references. Passing this
inventory alone does not satisfy the UI goal.
