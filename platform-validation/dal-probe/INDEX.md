# DAL runtime probe

DalMappingValidation registers compiled generated classes, uses the core mapping
generator and session factory controller, then executes parameterized HQL.
verifyOBDal additionally initializes the real context from XML security data and
checks non-admin persistence, client/organization/active filters, flushed rollback,
and restrictive table grants. verifyPlatform runs v1 and regenerated v2 classes in
separate JVMs, verifies the new accessor through OBDal and HQL, and repeats v2.
# Optional visual metadata runtime

UiDalValidation is compiled only by verifyUiDal alongside the newly generated
UI dictionary entities. It checks class origins, ERP class absence and real OBDal
queries across windows, tabs, fields and columns in a separate JVM.
