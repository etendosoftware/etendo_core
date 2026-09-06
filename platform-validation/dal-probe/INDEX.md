# DAL runtime probe

DalMappingValidation registers compiled generated classes, uses the core mapping
generator and session factory controller, then executes parameterized HQL.
It does not yet establish OBDal persistence or a populated security context.
