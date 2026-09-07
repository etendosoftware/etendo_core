# DAL runtime probe

DalMappingValidation registers compiled generated classes, uses the core mapping
generator and session factory controller, then executes parameterized HQL.
verifyOBDal additionally initializes the real context from XML security data and
checks non-admin persistence, client/organization/active filters, flushed rollback,
and restrictive table grants. Runtime schema/data upgrades remain pending.
