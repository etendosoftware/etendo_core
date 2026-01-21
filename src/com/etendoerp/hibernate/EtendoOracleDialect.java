package com.etendoerp.hibernate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.PatternFunctionDescriptorBuilder;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;

import com.etendoerp.hibernate.spi.FunctionContributorRegistry;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

/**
 * Extends {@link OracleDialect} to provide additional support for custom Oracle functions.
 * <p>
 * This class registers specific functions used in the context of Etendo.
 * Supports extension through {@link com.etendoerp.hibernate.spi.FunctionContributor} implementations.
 * </p>
 */
@Dependent
public class EtendoOracleDialect extends OracleDialect {

  // Logger for function registration information.
  private static final Logger log = LogManager.getLogger();

  @Inject
  private FunctionContributorRegistry contributorRegistry;

  private BaseEtendoDialect baseDialect;

  /**
   * Default constructor.
   */
  public EtendoOracleDialect() {
    super();
    initializeBaseDialect();
    if (baseDialect != null) {
      log.debug("Created Etendo specific Oracle Dialect");
    }
  }

  /**
   * Constructor with dialect resolution info.
   *
   * @param info the dialect resolution information
   */
  public EtendoOracleDialect(DialectResolutionInfo info) {
    super(info);
    initializeBaseDialect();
    if (baseDialect != null) {
      log.debug("Created Etendo specific Oracle Dialect");
    }
  }

  /**
   * Constructor with explicit database version.
   *
   * @param version the database version
   */
  public EtendoOracleDialect(DatabaseVersion version) {
    super(version);
    initializeBaseDialect();
    if (baseDialect != null) {
      log.debug("Created Etendo specific Oracle Dialect");
    }
  }

  /**
   * Initialize the base dialect with Oracle-specific implementations.
   */
  private void initializeBaseDialect() {
    baseDialect = new BaseEtendoDialect(contributorRegistry) {

      @Override
      protected void registerHqlAgg(FunctionContributions functionContributions) {
        PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder =
            new PatternFunctionDescriptorBuilder(
                functionContributions.getFunctionRegistry(),
                "hqlagg",
                FunctionKind.AGGREGATE,
                "listagg(?1, ',') within group (order by ?1)"
            );
        patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(1));
        patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.STRING_RETURN_TYPE).register();
      }

      @Override
      protected void registerTrunc(FunctionContributions functionContributions) {
        PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder =
            new PatternFunctionDescriptorBuilder(
                functionContributions.getFunctionRegistry(),
                "trunc",
                FunctionKind.NORMAL,
                "trunc(?1)"
            );
        patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(1));
        patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.TIMESTAMP_RETURN_TYPE).register();
      }

      @Override
      protected void registerToDate(FunctionContributions functionContributions) {
        PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder =
            new PatternFunctionDescriptorBuilder(
                functionContributions.getFunctionRegistry(),
                "to_date",
                FunctionKind.NORMAL,
                "to_date(?1, ?2)"
            );
        patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.between(1, 2));
        patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.TIMESTAMP_RETURN_TYPE).register();
      }

      @Override
      protected void registerNow(FunctionContributions functionContributions) {
        PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder =
            new PatternFunctionDescriptorBuilder(
                functionContributions.getFunctionRegistry(),
                "now",
                FunctionKind.NORMAL,
                "sysdate"
            );
        patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.NO_ARGS);
        patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.TIMESTAMP_RETURN_TYPE).register();
      }

      @Override
      protected void registerAddDays(FunctionContributions functionContributions) {
        PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder =
            new PatternFunctionDescriptorBuilder(
                functionContributions.getFunctionRegistry(),
                "add_days",
                FunctionKind.NORMAL,
                "(?1 + ?2)"
            );
        patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(2));
        patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.TIMESTAMP_RETURN_TYPE).register();
      }

      @Override
      protected void registerSubtractDays(FunctionContributions functionContributions) {
        PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder =
            new PatternFunctionDescriptorBuilder(
                functionContributions.getFunctionRegistry(),
                "substract_days",
                FunctionKind.NORMAL,
                "(?1 - ?2)"
            );
        patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(2));
        patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.TIMESTAMP_RETURN_TYPE).register();
      }

      @Override
      protected void registerSpecificFunctions(FunctionContributions functionContributions) {
        log.debug("Registering to_number function(character varying): numeric");
        EtendoOracleDialect.registerToNumber(functionContributions);

        // Allow external modules to contribute Oracle-specific functions
        if (contributorRegistry != null) {
          contributorRegistry.contributeOracleFunctions(functionContributions);
        }
      }
    };
  }

  /**
   * Initializes the custom function registry.
   *
   * @param functionContributions Function contributions provided by Hibernate.
   */
  @Override
  public void initializeFunctionRegistry(FunctionContributions functionContributions) {
    super.initializeFunctionRegistry(functionContributions);

    if (baseDialect == null) {
      initializeBaseDialect();
    }

    // Register common functions (identical implementation in both databases)
    baseDialect.registerCommonFunctions(functionContributions);

    // Register database-specific implementations of common function names
    log.debug("Registering hqlagg(character varying):character varying function");
    baseDialect.registerHqlAgg(functionContributions);

    log.debug("Registering trunc(timestamp):timestamp function");
    baseDialect.registerTrunc(functionContributions);

    log.debug("Registering to_date(character varying):timestamp function");
    baseDialect.registerToDate(functionContributions);

    log.debug("Registering now():timestamp function");
    baseDialect.registerNow(functionContributions);

    log.debug("Registering add_days(timestamp, int):timestamp function");
    baseDialect.registerAddDays(functionContributions);

    log.debug("Registering substract_days(timestamp, int):timestamp function");
    baseDialect.registerSubtractDays(functionContributions);

    // Register Oracle-only specific functions
    baseDialect.registerSpecificFunctions(functionContributions);

    // Verify cast function is registered
    SqmFunctionRegistry registry = functionContributions.getFunctionRegistry();
    if (registry.findFunctionDescriptor("cast") != null) {
      log.debug("Cast function already registered by parent dialect");
    }

    log.debug("Function registry initialized for Etendo Oracle Dialect");
  }

  /**
   * Registers the `to_number` function in the function registry.
   * This is Oracle-specific.
   *
   * @param functionContributions Function contributions provided by Hibernate.
   */
  private static void registerToNumber(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder =
        new PatternFunctionDescriptorBuilder(
            functionContributions.getFunctionRegistry(),
            "to_number",
            FunctionKind.NORMAL,
            "to_number(?1)"
        );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(1));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.BIGDECIMAL_RETURN_TYPE).register();
  }

  /**
   * Returns the maximum length for VARCHAR columns in Oracle.
   * Oracle 19c supports up to 32767 bytes if MAX_STRING_SIZE is EXTENDED,
   * but we use the standard 4000 byte limit for compatibility.
   *
   * @return maximum varchar length (4000 for Oracle standard mode)
   */
  @Override
  public int getMaxVarcharLength() {
    return 4000;
  }

  /**
   * Returns the maximum length for NVARCHAR columns in Oracle.
   *
   * @return maximum nvarchar length (2000 characters for Oracle)
   */
  @Override
  public int getMaxNVarcharLength() {
    return 2000;
  }

}
