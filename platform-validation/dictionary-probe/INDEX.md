# Dictionary bootstrap probe

Build the minimal technical schema from the existing dictionary mappings and DBSM
table definitions, then use the real ModelProvider and Java entity generator.
verifyPlatform includes generated compilation, real DAL/security checks, XML
schema/data upgrades, operational preservation, and repeat-safe reconciliation.
UpgradeValidation orchestrates the v1/v2 JVMs and compares complete table snapshots.
TomcatValidation keeps the disposable database alive during actual WAR HTTP and
redeployment checks, then checks persistence and rejects server lifecycle leaks.

## Security profile

SecurityFixture selects original physical columns and source dictionary rows for
seven security entities plus Warehouse and BusinessPartner compatibility entities.
It also selects TableAccess, Table, ClientInformation, and a Process descriptor
as required by the existing access checker and context. Original user-default
references are retained through their reference-table metadata.
Tree, TreeNode, and OrganizationType support the real organization access tree.
Security data and cross-tenant control rows are generated as XML for verifyOBDal
and verifyPlatform. Application column metadata uses stable hexadecimal IDs to
match DBSM's numeric primary-key comparison.
Run verifySecurityGeneration to exercise the real generator with this selection.
UI selector references for conventional foreign keys become equivalent TableDir
references; no replacement Java entity or ORM mapping is handwritten.
