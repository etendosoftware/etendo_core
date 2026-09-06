# Dictionary bootstrap probe

Build the minimal technical schema from the existing dictionary mappings and DBSM
table definitions, then use the real ModelProvider and Java entity generator.
Compiling the generated entities, DAL mapping generation, and DAL/security checks
are subsequent milestones.

## Security profile

SecurityFixture selects original physical columns and source dictionary rows for
seven security entities plus Warehouse and BusinessPartner compatibility entities.
It also selects TableAccess, Table, ClientInformation, and a Process descriptor
as required by the existing access checker and context. Original user-default
references are retained through their reference-table metadata.
Run verifySecurityGeneration to exercise the real generator with this selection.
UI selector references for conventional foreign keys become equivalent TableDir
references; no replacement Java entity or ORM mapping is handwritten.
