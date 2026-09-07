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
Tree, TreeNode, and OrganizationType support the real organization access tree.
Security data and cross-tenant control rows are generated as XML for verifyOBDal
and verifyPlatform. Application column metadata uses stable hexadecimal IDs to
match DBSM's numeric primary-key comparison.
Run verifySecurityGeneration to exercise the real generator with this selection.
UI selector references for conventional foreign keys become equivalent TableDir
references; no replacement Java entity or ORM mapping is handwritten.
# Original UI dictionary slice

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
Complete field/tab definition rendering and browser delivery remain pending.
Separate ui-cache-runtime.log,
ui-field-runtime.log and ui-dal-runtime.log files keep diagnostics from overwriting
one another. Neither gate implies browser UI delivery.
