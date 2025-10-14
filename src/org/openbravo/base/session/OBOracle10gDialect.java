package org.openbravo.base.session;

import java.sql.Types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.StandardBasicTypes;

/**
 * Adaptación del Oracle10gDialect para Hibernate 6.x.
 * Mantiene compatibilidad con el esquema Openbravo.
 */
public class OBOracle10gDialect extends OracleDialect {

  private static final Logger log = LogManager.getLogger();

  public OBOracle10gDialect() {
    super();
    log.debug("Created Openbravo specific Oracle Dialect (Hibernate 6)");
  }

  public OBOracle10gDialect(DialectResolutionInfo info) {
    super(info);
    log.debug("Created Openbravo specific Oracle Dialect (Hibernate 6)");
  }

  /**
   * Registro de funciones SQL personalizadas.
   */
  @Override
  public void initializeFunctionRegistry(FunctionContributions functionContributions) {
    super.initializeFunctionRegistry(functionContributions);

    SqmFunctionRegistry registry = functionContributions.getFunctionRegistry();

    registry.namedDescriptorBuilder("to_number")
        .setInvariantType(functionContributions.getTypeConfiguration()
            .getBasicTypeRegistry()
            .resolve(StandardBasicTypes.BIG_DECIMAL))
        .register();
  }

  /**
   * Personaliza los tipos de columna para VARCHAR y NVARCHAR.
   */
  @Override
  protected String columnType(int sqlTypeCode) {
    switch (sqlTypeCode) {
      case Types.VARCHAR:
        // Por defecto usaremos NVARCHAR2 para campos grandes
        return "nvarchar2";
      case Types.NVARCHAR:
        return "nvarchar2";
      case Types.NCHAR:
        return "nchar";
      default:
        return super.columnType(sqlTypeCode);
    }
  }
}
