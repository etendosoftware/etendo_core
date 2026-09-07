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

Finish the ERP initialization boundary regression, then separate the legacy typed
context facade from shared context ownership. Address accounting and process/UI
type dependencies next. Promote the extracted implementation into independently
buildable Gradle modules only with enforced dependency direction and profile tests.
Keep the existing Product validation as a compatibility control throughout.
