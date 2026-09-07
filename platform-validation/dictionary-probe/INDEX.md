# Dictionary bootstrap probe

Build the minimal technical schema from the existing dictionary mappings and DBSM
table definitions, then use the real ModelProvider and Java entity generator.
verifyPlatform includes generated compilation, real DAL/security checks, XML
schema/data upgrades, operational preservation, and repeat-safe reconciliation.
UpgradeValidation orchestrates the v1/v2 JVMs and compares complete table snapshots.
TomcatValidation keeps the disposable database alive during actual WAR HTTP and
redeployment checks, then checks persistence and rejects server lifecycle leaks.
EntityMetadataValidation checks identifier nullability for empty, mandatory and
optional identifiers with no database and no UI kernel on its runtime classpath.
It is also required by verifyPlatform.

## Security profile

SecurityFixture selects original physical columns and source dictionary rows for
seven security entities. Warehouse, BusinessPartner and their user association
properties are intentionally absent.
It also selects TableAccess, Table and ClientInformation
as required by the existing access checker and context. UI Process metadata and
its generated Java type are intentionally absent. Original user-default
references are retained through their reference-table metadata.
Role and user-role fixture records use organization `0`, as required by the original
client-level DAL access rules. Organization access grants still select `O1`.
Tree, TreeNode, and OrganizationType support the real organization access tree.
Security data and cross-tenant control rows are generated as XML for verifyOBDal
and verifyPlatform. Application column metadata uses stable hexadecimal IDs to
match DBSM's numeric primary-key comparison.
Run verifySecurityGeneration to exercise the real generator with this selection.
UI selector references for conventional foreign keys become equivalent TableDir
references; no replacement Java entity or ORM mapping is handwritten.
# Original UI dictionary slice

`prepareOriginalUiInstance` retains an owned PostgreSQL fixture and compiles its
UI/login dictionary entities without running disposable probe applications.
`build/original-ui-instance-location.txt` records the private properties path,
owned container name and compiled entity directory. The latter contains an ordered
`runtime-classpath.txt` for server composition. An existing pointer is never
overwritten. This prepares application inputs, not an original UI deployment.
The database survives Tomcat restarts, not stopping its temporary Docker container.

`verifyUiLogin` additionally renders the unchanged user-info template with original
roles/languages/organization data and empty warehouse options. It selects the
platform profile policy explicitly; backend-restricted roles, inactive roles and
inactive user-role assignments remain excluded by the canonical role query.
It then launches a separate HTTP authentication JVM against the same disposable
database, with generated UI entities and canonical default authentication classes.
The child uses ephemeral loopback Tomcat, commits only its own credential fixture,
and verifies cookie-backed sessions and original success/failure AD_Session rows.
Logs are separate: `build/ui-http-authentication.log`. This is not UI browser CRUD.

UiDictionaryFixture supplies application-owned window/tab/field rows using the
original dictionary schema. verifyUiDictionary uses a disposable PostgreSQL
database and separate ui-generated-entities output; headless generation is unchanged.
The optional slice also retains field groups, display/layout flags and column
read-only expressions. verifyUiDal validates their generated APIs and persisted
values through OBDal; it does not yet execute the original field handler.
Field.Property retains its original selector parent and model-element domain.
The selector/datasource bootstrap schema reuses the same canonical mapping-to-XML
projection helper as the core dictionary; headless schemas do not include it.
The generated ADReference API exposes column reference/search-key relationships,
parent references and original module ownership. Base UI reference rows come from
canonical XML; headless entity selection remains unchanged.

verifyUiCache starts a UI-only Weld SE container with explicit bean discovery and
executes the canonical production dictionary cache over 47 generated entities.
It checks initialized tab reuse after clearing the Hibernate session, detached
field-reference reads and application field/auxiliary-input relationships.
The optional UI selection includes generic process, reference, selector and
datasource metadata, not ERP process definitions or business entities. Selected
module modifiedTables columns are composed from canonical XML while preserving
column/package ownership. A generated process-extension accessor is exercised
with an OBDal write/read/unlink inside a transaction that is rolled back.

verifyUiFields constructs original handler fields and renders their form-logic
template. It checks application field names, original String/YesNo/FKCombo editor
types and complementary active-state display/read-only rules. The fixture retains
original UI definition rows, required audit elements, translation relationships and
module layout columns; application fields receive valid display lengths.
verifyUiFieldDefinitions renders the unchanged full field macro, including editor
and grid properties, against the real handler with empty grid overrides. Its
48-entity fixture retains canonical Column/Tab flags and Tab translation metadata.
Literal Y/N XML defaults are preserved for UI seed data. Complete tab/window
rendering, database grid overrides and browser delivery remain pending.
Separate ui-cache-runtime.log,
ui-field-runtime.log, ui-field-definitions-runtime.log and ui-dal-runtime.log files keep diagnostics from overwriting
one another. Neither gate implies browser UI delivery.

verifyUiWindow composes both application windows through the original CDI-managed
StandardWindowComponent, tab, form, grid and datasource components. Its 55-entity
dictionary retains complete generic grid-configuration, note, preference,
model-mapping and datasource-field metadata. It loads canonical templates and
their dependencies from XML, writes rendered JavaScript under build/, and checks
seven Hibernate subtab hierarchy cases. ui-window-runtime.log is separate from
the narrower field gates. Original login/navigation and browser CRUD are pending;
the working window classpath is not a deliverable shared UI artifact.

verifyUiMenu adds complete generic menu/access metadata (65 generated entities)
and tests the original request-scoped menu for editable, read-only and denied
roles. It exercises original HQL and the shared Y/N mapping using boolean and
legacy string literals, parameters, writes, extraction and legacy null comparisons.
ui-menu-runtime.log and ui-menu-<role>.js preserve separate diagnostics. This is
not a login, HTTP-session or web-container licensing-policy test.

verifyUiLogin extends the menu fixture to 67 generic/own entities with AD_SYSTEM,
AD_SESSION and password/lock metadata. It composes the canonical PostgreSQL DUAL
view and invokes FullLoginValidation using generated original SQLC collaborators.
It preserves separate ui-login-runtime.log diagnostics; no HTTP/browser session
is exercised by this service integration gate.

The login/navigation fixture now retains 69 entities: it adds the canonical navbar
component and role-access tables. Six original registrations/templates are loaded;
the fixture configures the existing menu button as dynamic and role-restricted to
exercise both assembly paths. The menu data is generated separately, as required
by the original UI contract. Other widgets' server backends are not certified here.
