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

### Original login session composition (in progress)

The next web integration block retains `LoginHandler`, `LoginUtils`, password
verification and role/client/organization checks. `LoginUtils` currently also
selects a warehouse and initializes ledger, currency and accounting-dimension
session values. Extract these ERP contributions behind a server-owned composition
contract; the default remains the original ERP implementation. An explicitly
selected platform implementation must not load those classes or execute their SQL.
Missing or invalid composition must fail, never silently disable ERP behavior.
HTTP parameters must not select the implementation. Existing public method
signatures and the ordering of session initialization remain unchanged.

`LoginSessionSupport` now owns that contract. Trusted server property
`login.session.support.class` selects its implementation once per application
class loader. Absent configuration retains `ErpLoginSessionSupport`; empty,
missing or wrong-type configuration fails initialization. The ERP-free composition
explicitly selects `org.openbravo.base.secureApp.PlatformLoginSessionSupport`.
No request parameter or automatic missing-class fallback selects platform mode.
This is an internal deployment boundary, not an authentication bypass.

`verifyClassicLogin` first executes the baseline WAR's original `LoginUtils`, then
the extracted canonical class, in separate JVMs over the owned copy. Both execute
password checks, defaults, full session initialization, same-scope light login and
invalid role/client/organization rejection. It compares every session string set
by full login except the random CSRF token, using private value-hash reports; the
current fixture matched 580 values. It independently checks CSRF rotation and
context stability. `verifyModelUiApi` also checks `LoginUtils` and `RoleDefaults`
public/protected declarations and JVM descriptors against the original WAR.

`verifyPlatformLoginSession` loads the same `LoginUtils` warehouse facade with
the platform contribution and no ERP adapter, Product, Warehouse or BusinessPartner
classes. Its connection provider rejects any SQL access. Four isolated negative
configuration checks verify failure rather than fallback. This gate proves only
the extracted contribution, **not full platform login**. The probe currently
needs Quartz's exception type through the unchanged `VariablesSecureApp` API;
this dependency is probe-local, not added to either headless composition.

The newly built ERP WAR also passed real original browser login, shell readiness,
Product projection/references/filtering/paging/order, anonymous denial, six
restricted role scopes and fresh-session restoration on temporary port 8095.
The pre-existing 8093 process was not restarted by this regression.

The role-session payload is now split: canonical `LoginUtils.readRoleSession`
reads only role level and client search key through a parameterized Hibernate
projection, preserving the original role/user association and active role/client
predicates. ERP approval values are supplied by `ErpLoginSessionSupport`.
The existing SQLC `SeguridadData.select` API remains unchanged for legacy callers.
ERP session initialization currently performs one extra indexed role projection;
this is not a per-record datasource query. `verifyClassicLogin` still matches all
580 deterministic values against the original WAR.

`verifyUiMenu` also invokes this exact shared projection against the ERP-free
database and checks missing user associations, parameter binding, inactive roles
and inactive clients. Negative fixtures use transaction-local SQL setup rather
than disabling DAL access-level enforcement. The original `validUserRole`,
`validRoleClient` and `validRoleOrg` checks remain mandatory and unchanged; this
projection is not itself a complete authentication or authorization policy.

Remaining login dependencies are explicit: generic session-default
SQLC queries and preferences must run on the minimal schema; `LoginHandler`, the
login page, web lifecycle/license policy, and the profile widget must be integrated
without their remaining Warehouse dependencies. This extraction does not make
the platform UI navigable and must not be presented as completed browser CRUD.

The full-login integration fixture composes the original generic login
metadata (`AD_SYSTEM`, `AD_SESSION`, password/lock and backend-role settings),
regenerates the original selected SQLC collaborators from their canonical XSQL,
and invokes the original password/default/session path. SQLC generation may use
the owned ERP copy for its metadata discovery; the resulting login runtime must
execute on the independently generated ERP-free database, never fall back to that
copy. Keep the existing smaller UI and headless fixtures unchanged. A service
login pass is a prerequisite for, not a substitute for, the real servlet/browser
flow. Do not disable user locking or permissions to make this fixture pass.

`verifyUiLogin` adds AD_SYSTEM and AD_SESSION to the 65-entity menu fixture and
retains the canonical PostgreSQL DUAL view for DateTimeData. It exercises original
password hashing, enabled one-failure user locking, configured defaults, full
session values and denial of invalid role/client/organization selections without
context or CSRF mutation. Test credentials are randomly generated in memory and
fixture changes are rolled back. LoginServlet/LoginHandler HTTP delivery, profile
switching and browser navigation remain separate uncompleted integration work.

The original profile component and save handler must use LoginSessionSupport for
warehouse context/default contributions. Keep UI entry signatures unchanged and
package the canonical profile classes in the optional shared UI artifact. ERP
retains its DAL warehouse assignment; platform accepts no warehouse default and
rejects nonempty domain identifiers rather than silently applying an ERP setting.
Profile authorization and transaction ownership remain in the original handler.
RoleInfo also delegates its warehouse projection query to this contribution;
organization selection, natural-tree distribution and the public RoleWarehouseInfo
shape stay in the shared original UI. The shared artifact now owns UserInfoComponent,
UserInfoWidgetActionHandler (including its private helper), and RoleInfo (including
its public nested DTO). ERP WAR assembly excludes their old loose definitions.
The API gate compares all four externally visible class declarations/descriptors
against the baseline; the private helper has no public module-facing contract.
This removes warehouse dependencies from the profile block but does not implement
platform licensing policy, servlet session reset, password-change integration or
complete navbar rendering. Those remain explicit integration work, not passing
claims inferred from the component getter and contribution tests.

Profile extraction verification (2026-09-07): verifyUiLogin and the fail-closed
platform composition probes pass; verifyClassicLogin preserves 580 deterministic
session values and checks ERP warehouse defaults/options. The profile API checks
retain 11 UserInfoComponent, 5 UserInfoWidgetActionHandler, 9 RoleInfo and 5 nested
RoleWarehouseInfo baseline entries with none missing. Shared artifact ownership
and headless exclusion pass. The final ERP WAR on temporary port 8095 passes the
original browser shell/login/Product regression (40 rows, projection, references,
filtering, ordering, paging, anonymous denial and six restricted roles). This is
ERP browser coverage, not platform browser profile-save/password-change coverage.
Local logs: `/private/tmp/et27-profile-final-server.log` and
`/private/tmp/et27-profile-final-browser.log`. Objective Guard alignment remains
unconfigured and is not claimed as a passing acceptance gate.

Shell integration uses canonical NavigationBarComponent, its dictionary-driven
generator and MainLayoutComponent. The fixture composes original navbar registrations
and configures the original menu button as dynamic through XML metadata, preserving
navbar role access. Do not replace templates or remove the separate ERP licensing
checks in UserInfoComponent/LoginHandler. Rendering this assembly is still not
browser delivery or a claim that every widget's backend is integrated.
The profile role-list policy is extracted as `UserInfoAccessPolicy`: a trusted,
application-classloader selection defaults to the original ERP build/license policy.
An explicit platform implementation contributes no ERP restriction, but does not
replace authentication or the original active-user-role/backend-access query.
The ERP implementation must live in a separate ERP-only UI contribution artifact;
invalid configuration fails closed. LoginHandler licensing remains unchanged.
The server-only property `ui.userInfo.accessPolicy.class` selects
`org.openbravo.client.application.navigationbarcomponents.PlatformUserInfoAccessPolicy`
for platform. ERP leaves it unset and loads `ErpUserInfoAccessPolicy` from
`platform-ui-erp-contributions.jar`, not `platform-ui-components.jar`. Neither
artifact is allowed in headless WARs. `verifyUiPolicyComposition` exercises six
fresh JVM selections, including invalid values and the missing default ERP artifact.
`verifyUiLogin` renders the original user-info template and checks the unchanged
role query against inactive role/assignment and backend restriction cases.
Verified in `/private/tmp/et27-ui-policy-v3.log`: ERP-free original user-info
rendering, role exclusions, six policy selections, shared/ERP-only/headless packaging,
and XML/DBSM upgrade, restart and zero-delta repetition. Final composition/API/nested
boundary checks passed in `/private/tmp/et27-ui-policy-gates.log`.
`/private/tmp/et27-ui-policy-browser.log` confirms original ERP browser profile
role/organization/warehouse data and the existing Product/six-role regression.
The negative DAL test exposed invalid fixture role organizations; role and user-role
XML records now use organization `0`, without weakening access-level validation.
Platform HTTP login/shell delivery and browser CRUD remain pending; this extraction
does not assert complete licensing-state coverage or alter LoginHandler checks.

The HTTP integration gate executes canonical `DefaultAuthenticationManager` and
`AuthenticationManager` in a real loopback Tomcat request against the disposable UI
dictionary, including `SessionLogin` persistence and `VariablesSecureApp` backed by
an actual HttpSession. A test-only servlet may wire these services to isolate their
runtime requirements; it is not a replacement login implementation or an accepted
platform UI endpoint. Verify incorrect credentials, fresh session ID, persisted
AD_Session and subsequent cookie authentication before integrating the UI shell.
Do not route platform REST through the ERP web-service licensing path or disable
ERP LoginHandler restrictions to make this gate pass.
`verifyUiLogin` now starts a separate disposable Tomcat JVM after the original UI
rendering checks. Canonical authentication classes are compiled unchanged; the
fixture additionally retains Client.DaysToPasswordExpiration. It verifies failed
and successful AD_Session records, full session values and CSRF initialization,
rotation immediately around the successful login, rejection of the previous cookie,
and anonymous isolation using one Tomcat request worker. The fixture admin scope
is explicitly closed and asserted absent before session publication and on reuse.
Final HTTP/role/rendering checks passed in `/private/tmp/et27-http-auth-scope.log`;
API/shared-artifact/headless/nested boundary checks passed in
`/private/tmp/et27-http-auth-final.log`. The test stops its ephemeral Tomcat and its
parent fixture owns database cleanup. No production servlet or WAR was replaced.
This proves only the stateful web authentication path: expiry/reset, logout/session
listener cleanup, stateless licensing paths and the platform shell remain unproven.
The optional UI artifact owns these canonical classes and the navigation-bar/layout
templates; ERP excludes their former loose copies. `verifyUiLogin` checks each
generated script with Node's syntax checker (Node is a test prerequisite).
`node platform-validation/ui-navigation-validation.mjs` additionally evaluates the
generated descriptor arrays in isolated contexts, checking static widgets, dynamic
placeholders and the menu descriptor for editable/read-only/denied roles. It does
not execute widget callbacks or substitute for browser acceptance.

Navigation extraction regression: `verifyUiLogin` and `verifyUiArtifactPackaging`
passed with the 69-entity ERP-free fixture; the three-role descriptor evaluator
passed. `verifyModelUiApi` and `verifyPlatformNestedBoundary` also passed.
The ERP browser regression on temporary Tomcat 8095 passed original shell/login,
40-row Product database equality, projection/references/filtering/sorting/paging,
six restricted roles and anonymous denial with the shared navigation artifact.
Local logs: `/private/tmp/et27-navbar-final.log`,
`/private/tmp/et27-navbar-erp-server.log` and
`/private/tmp/et27-navbar-erp-browser.log`. This does not prove platform HTTP shell
delivery, widget backends or browser CRUD; those remain acceptance requirements.

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

## Integration-sized work packages

`verifyUiMenu` extends the window composition with the original GlobalMenu,
MenuManager and ApplicationMenuComponent. Seed application-owned menu/tree and
window-access rows and verify editable, read-only and denied role menus. Keep the
original non-web test semantics explicit: this does not exercise ActivationKey's
web-container policy or replace the real login/session deployment requirement.

The menu gate exposed an existing Hibernate 6 `yes_no` integration defect: HQL
`module.enabled=true` renders a PostgreSQL CHAR/boolean comparison. Preserve the
OBYesNoType class hierarchy, constructors and legacy methods, but expose Hibernate's
converted-basic-type contract and relational JDBC descriptors. Retain its legacy
Java-descriptor null/false equality and the existing BasicType null comparisons.
Validate literal and parameter queries together
with actual writes/rollback; do not rewrite module queries to conceal the defect.

The 65-entity `verifyUiMenu` composition now renders the original application menu
for three roles: both own windows editable, Requests read-only, and no window
access. These are real MenuManager/GlobalMenu authorization queries over seeded
menu, tree, module-enabled and access metadata, not fabricated menu objects. Each
request-scoped menu uses a real OBContext and writes its generated JavaScript to
ignored `build/ui-menu-<role>.js`; all three outputs passed syntax parsing.

OBYesNoType now declares the converted-basic-type contract while retaining its
previous superclass and public/protected API. The relational type is String to
support both existing `'Y'/'N'` HQL and `true/false` literals. Tests cover both
values, parameter binding, physical CHAR storage, extraction after session eviction
and the prior distinct descriptor/type null-comparison behavior. The original
menu HQL remains unchanged. Menu/window, XML/DBSM lifecycle and API gates passed;
the original ERP Product datasource then passed against the owned Classic copy
(80-row DAL count, nonempty projected datasource page). Shared artifact packaging
and nested-WAR boundary checks also passed. The existing live ERP process was not
restarted, so this service regression does not claim a redeployed browser session.

The next integration gate, `verifyUiWindow`, must use CDI-managed original tab,
form, grid and datasource components, original database template dependencies and
real dictionary entities. Its isolated working output is not a production shared
artifact. Do not override datasource generation or replace template-facing objects
with maps to make the tab render. Missing generic runtime services belong to this
composition block; ERP-specific services require explicit extension boundaries.

Move `KernelUtils.getTabSubtabs(Tab, boolean)` to the existing Hibernate session
without changing its public signature. Preserve the SQLC predicate: same window,
sequence strictly after the parent and before the next tab at the same or a lower
level (legacy fallback 999999); optionally restrict to the immediate child level.
Do not introduce active/client filters or an ordering guarantee absent from the
original SQL. Test hierarchy boundaries independently of window rendering.

The working `verifyUiWindow` composition now generates 55 entities. It uses the
real CDI-managed `StandardWindowComponent` for both Requests and Categories,
including their tab, form, grid, application datasource and original notes
datasource. Canonical template rows and the tab-to-field template dependency come
from the original module XML. Grid configuration is selected by the original
database queries over complete GCSystem/GCTab/GCField metadata (empty overrides
in this fixture), not injected substitutes in the standard-window path.

Generated JavaScript is written to ignored `build/ui-window-<window-id>.js` files;
both outputs passed JavaScript syntax parsing. Seven DAL hierarchy cases cover
immediate/all descendants, peer boundaries, leaf tabs, inactive metadata, foreign
windows and the legacy upper limit. Existing model/API, field-rendering and
headless XML/DBSM lifecycle gates passed. These are server-side rendering checks,
not a browser CRUD or authentication claim.

`compileUiWindowComponents` and `prepareUiWindowTemplates` remain isolated
integration outputs. They reuse canonical implementations but still include broad
legacy utility/JSON classes; they must not be packaged wholesale as the final
generic UI layer. No original-platform login/menu deployment is delivered here.
Next: original authentication/shell/navigation, explicit ERP contributions and
shared UI artifact ownership, followed by browser CRUD and four-profile checks.

`verifyUiFieldDefinitions` is the integration gate for the complete canonical
field macro, distinct from `verifyUiFields` (field construction and form logic).
It renders real handler definitions using the original FreeMarker processor and
the repository's unchanged field template. The test-only wrapper invokes the
macro; it is not a replacement frontend or a deployed template. A failing gate
records unfinished runtime integration and must not be reported as UI delivery.

The gate now passes with 48 generated entities after composing canonical Column
and Tab scalar metadata, application-owned Tab extension columns and AD_TAB_TRL.
This includes the complete field macro's editor and grid-property branches, not
just the form-logic template. The fixture preserves literal Y/N XML defaults
instead of forcing flags such as sorting/filtering to false. Empty grid-configuration
overrides are supplied through the original handler's existing package-private
setter in the test; database-driven grid-configuration overrides remain pending.
The source macro is read directly for this integration test and has not yet been
added to the deliverable shared UI artifact. Complete tab/window rendering,
original shell startup and browser CRUD remain unproven.

The refreshed bytecode inventory has no missing direct Column methods and only
`Tab.isAccountingTab()` remaining among the previously measured Tab methods.
This is model API coverage, not ERP feature coverage. `verifyUiFieldDefinitions`,
`verifyUiFields` and `verifyPlatform` passed together against disposable databases.

Do not estimate remaining work by counting resolved exceptions. The selective UI
fixture has omitted both API properties and valid field defaults; those omissions
are not evidence of ERP coupling. Inventory the complete original UI entry set and
its static dependency closure, then compare its bytecode method calls with the
freshly generated model API in one batch. Keep direct roots, transitive candidates,
missing generated types, missing methods and forbidden ERP paths separate. Static
reachability is an upper bound: reflective/CDI, SQL, template and browser behavior
still require runtime checks.

Execute four work packages: (1) compose the generic UI metadata and dependencies,
(2) boot original authentication, shell and one generated application window,
(3) exercise related-entity CRUD/security and restart persistence, and (4) validate
the shared ERP UI and both headless compositions together. Package (1) must produce
a categorized dependency report and complete rendering, not another getter-only
checkpoint. No calendar estimate is justified until the first inventory separates
fixture incompleteness from actual extraction/adaptation work.

### Measured baseline and next integration blocks

The first JDK bytecode inventory of the prepared original WAR found 364 UI
implementation roots, 115 directly referenced generated model types, and 143
missing method descriptors across 24 types already present in the reduced model.
The largest API gaps were Tab (26), Parameter (13), Column (12) and SelectorField
(12). After indexing four project-bearing JARs as well as WEB-INF/classes, the
transitive candidate closure contains 1,558 classes and reaches 479 generated
types, 434 absent from this fixture. No project-namespace type remains unresolved
in that static scope. **Do not import that closure**:
these are static candidates, including ERP functionality, not a platform bill of
materials or an estimate of required implementation changes.

The direct missing-type inventory identifies the remaining generic feature groups:

| Integration block | Missing examples | Required proof |
| --- | --- | --- |
| Window/form/grid runtime | Grid configurations, field/tab access, complete Tab/Column APIs | Original field definitions and generated window render |
| Authentication and shell | Menu, messages/translations, navbar, preferences, view access | Original login and navigation to an application window |
| Related-entity interaction | Datasource fields, selector translations, FIC metadata | Browser filter/sort/page/select/create/update and denied writes |
| Shared UI services | Personalization, notes, attachments, images | Explicit shared modules and compatible ERP behavior |
| Distribution and lifecycle | Profile assembly, deployment, restart | Four compositions, isolation and restart-persistent CRUD |

Direct forbidden-entity references also identify concrete ERP seams: order/product
business-logic handlers; product image/characteristic observers;
CharacteristicsUIDefinition; and Warehouse use in UserInfoComponent and
UserInfoWidgetActionHandler (including its session setter). Business-specific
contributions belong in ERP-only composition. User/session UI needs a generic
extension boundary while preserving existing ERP behavior and visible APIs.
This list is not exhaustive: indirect SQL/utility dependencies need review too.

Run the reproducible read-only inventory with JDK 17 and an explicit fresh UI
classes directory printed by the validation workbench:

```sh
node platform-validation/ui-closure-inventory.mjs --self-test
node platform-validation/ui-closure-inventory.mjs --generated platform-validation/build/ui-dal-classes-<id>
```

Its JSON distinguishes direct roots, direct missing model types, transitive
candidates, exact missing method descriptors/callers and paths to forbidden
entities. The snapshot above used 45 generated original-model types plus the two
application entities. It does not measure browser coverage, reflective discovery,
SQL correctness or remaining elapsed time. Refresh it after each metadata block.

Next, retain canonical UI reference-definition rows and their generated relationship
to ADReference. Initialize the original reference controller, including its date
and datetime bootstrap references, without falling back to fabricated editor types.

Execute the canonical OBViewFieldHandler in the isolated UI DAL next. Compile its
working dependency slice separately while resolving missing generic classes and
metadata; do not introduce replacement handlers or add Classic runtime support to
the ERP-free test. The working slice is not a deliverable shared UI artifact until
the handler runs and its canonical implementations are packaged once for both UI
consumers. Keep this integration gate distinct from the already-passing template
and metadata gates so unfinished field rendering cannot be mistaken for completion.

The field integration uses a UI-only Weld SE container with explicit bean discovery.
Generate its generic process, auxiliary-input, parameter and model-implementation
metadata from the canonical XML; do not supply generated Classic entity classes.
Keep CDI test-container dependencies out of the DAL and both headless artifacts.
Compose selected `modifiedTables` columns from those same UI modules as well as
their base tables. Preserve module ownership of extension columns and packages:
projecting their owner to Core changes generated extension accessor names. Verify
the original process-definition accessor through generated Java and real OBDal.
The resulting optional slice generates 42 entities. `verifyUiCache` checks real
Weld registration, production-cache window initialization and tab reuse after
clearing the Hibernate session, including detached field reference reads; the module
extension test writes, reloads and unlinks a process reference through unchanged
generated accessor signatures, within a rolled-back transaction. UI DAL, headless
XML/DBSM lifecycle, shared-artifact packaging and nested-WAR gates passed.
The subsequent visual metadata batch generates 47 entities and makes
`verifyUiFields` pass: canonical reference definitions (including date/datetime),
required audit elements and translations, field-group metadata and module layout
extensions let the original handler construct application fields. Assertions cover
title/category/active, the original String/YesNo/FKCombo editors, complementary
active-state display/read-only rules and their original form-logic template output.
Referenced element rows are selected as a set and missing rows fail generation;
every application field has a valid display length.

This gate does not yet render the complete ob-view-field/ob-view-tab definition or
exercise original-platform browser UI/CRUD. The expanded working class slice is
still outside sharedUiJar; its ERP-specific reference implementations must be
separated before packaging the complete generic UI. UI cache/DAL, XML/DBSM
lifecycle, shared-artifact packaging and nested-WAR regressions passed with this
batch. The integration-sized work packages above remain incomplete.

The current form-metadata slice exposes Column.reference through the generated
ADReference entity and uses the original String/ID/TableDir reference rows in the
UI fixture. Preserve their names and domain implementations, because the original
field handler and reference controller use them. Keep the headless fixture's
selection unchanged; test reference traversal and IDs through real OBDal.
The isolated runtime generates 25 entities and verifies Column.getReference(),
Column.getReferenceSearchKey(), selector parent-reference traversal and the
owning module's original Java package. The UI fixture imports only the required
reference-owner module descriptors, failing if one is unavailable. This is
dictionary ownership, not full module lifecycle or selector widget startup.
The original UniqueIdDomainType is compiled as a generic model type; no UI type
is added to headless runtimes. UI DAL, model/context API, XML/DBSM lifecycle and
shared-artifact/nested-WAR regressions passed for this increment.

The ERP role-switch regression had one scope mismatch followed by a successful
fresh-session run. Record bounded browser request timing on failure, without
cookies, credentials, bodies or product records, before attributing it to a race.
Do not retry a mismatched response inside the assertion or relax scope equality.
The repeated browser gate reproduced 40 rows where a restricted role expected 19,
with MyOpenbravoActionHandler active. Independently, OBContext.setOBContext(request)
reinitializes a session context in place when role/client/organization changes;
an older request can still hold that same object. Add a deterministic retained-
request regression and replace an out-of-sync context rather than mutating it.
Keep same-scope reuse and existing public APIs; test a late old-request publish
followed by another request as well. Browser overlap is diagnostic evidence, not
by itself proof of the exact production interleaving.
The retained-request test failed before the fix and passes after context
replacement, including same-scope reuse and late completion. Five independent
strict ERP browser runs passed with six restricted roles each. UI DAL, XML/DBSM
upgrade/rollback, headless HTTP/redeployment, nested packaging and baseline API
and compiled-caller checks also passed. These checks cover the extracted context
behavior; they do not claim every possible session-concurrency scenario is solved.

Column-ID property resolution belongs to the model, not the UI kernel. Add the
lookup to the canonical Entity implementation and make both existing
KernelUtils.getPropertyFromColumn overloads retain their signatures and delegate.
Preserve first-match ordering and the legacy non-ID preference with ID fallback;
keep the existing exception in the compatibility wrapper. Test null/unmapped IDs,
duplicate column IDs and fallback behavior without a database or UI classpath,
then exercise the same lookup on generated application metadata through OBDal.
The no-database metadata test passes. The reusable descriptor gate
`verifyModelUiApi` preserves all 87 Entity and 22 KernelUtils baseline declarations;
`verifyContextApi` still preserves all 74 OBContext declarations. These checks
used existing generated sources with database prerequisites explicitly excluded.
Integrated verification was initially blocked by Docker startup timeout and
SQLSTATE 08001. After access recovered, the UI DAL/lifecycle/API/packaging gates
passed against regenerated models. The deployed lookup change also passed the
browser checks above with the subsequent context-isolation fix.

The selector increment retains the actual parent/child reference metadata for
Field.Property and compiles canonical selector domain implementations/mappings into
the optional shared artifact. The child uses ModelElementDomainType, a primitive
model-property reference; its parent remains SelectorDomainType. Derive the small
selector/datasource bootstrap tables from their original Hibernate mappings and
module XML. No reference rewriting and no handwritten substitute domain are
allowed. Verify generated Field.getProperty() and persistence with the isolated
DAL before attempting the selector widget or original field handler.
The isolated runtime now verifies the original parent/child IDs, shared-class
origin and generated getter/setter, including flush/clear/reload in a transaction
that is rolled back. It does not yet test selector suggestions or browser writes.
The shared artifact includes the original domain mappings; the packaging gate
rejects their loose duplicates in ERP UI. UI DAL, XML/DBSM lifecycle, nested WAR
boundaries and original ERP browser shell/Product/six-role regressions passed.

The current dictionary increment retains the original field layout/display metadata,
field groups and column read-only expressions. These are generic UI metadata,
not ERP entities. Select their schema and generated API from canonical XML;
validate persisted values and field/column/group relationships with the isolated
DAL. Do not implement replacement getters or a second field renderer to work
around a deliberately truncated dictionary. This increment alone is not execution
of OBViewFieldHandler or proof of browser form behavior.
The field `Property` was initially deferred: its original
reference is a selector from the selector module, not a plain string reference.
Adding it exposed missing reference `95E2A8B50A254B2AAE6774B8C2F28120` during
ModelProvider startup. The selector increment above resolves this missing domain
without converting its reference to text just to generate its getter.
`verifyUiDal` now generates 24 entities and passes checks for persisted field
visibility rules, column read-only logic, layout flags, group membership and
column-to-property resolution. The original template processor still passes in
that isolated runtime. `verifyPlatform`, `verifyPlatformNestedBoundary` and
`verifyUiArtifactPackaging` passed after this metadata-only increment. No
production class or method signature changed; headless selection remains unchanged.

The original template processor requires module, template and template-dependency
entities in addition to window metadata. The optional UI slice selects these from
the core/kernel XML, imports the original form-template row, and packages the
canonical FreeMarker processor/resolver and form template in the shared UI JAR.
The runtime gate must exercise that processor against the database-loaded
template without adding UI classes to headless classpaths. Full field-handler,
servlet and browser integration remains a separate, required goal step.
The isolated runtime now generates 23 entities and successfully resolves and
renders the canonical form template through the shared FreeMarker implementation.
The probe supplies a small view-model to exercise template behavior; it does not
replace or yet test OBViewFieldHandler, and does not claim end-user form operation.
The expanded shared artifact still uses Classic APIs at compile time. Its new
template classes are exercised on the ERP-free runtime without Classic support
classes. The expanded artifact is now also verified in the original ERP UI:
the browser gate passed shell/session initialization, Product projection and
reference identifiers, filtering, ordering, pagination, anonymous denial and
independent database equality across six restricted roles (40 default-scope rows).

After the validation containers disappeared, the authorized Classic database
container was started and a fresh owned copy was restored using a read-only
snapshot. No source database migration or source Tomcat startup was performed.
The ERP validation Tomcat was started on loopback port 8093 against that copy.
`verifyUiDal` and `verifyUiArtifactPackaging` passed again: the 23-entity
ERP-free runtime executes the original template processor, ERP UI packages the
same shared JAR, and both headless WARs exclude that artifact.
The copy configuration/container pointer is generated in
`platform-validation/build/classic-copy-location.txt`; do not reuse historical
container names after an environment restart. This checkpoint does not establish
original platform login, field-handler execution or browser CRUD for app entities.

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
