package com.etendoerp.hibernate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.PatternFunctionDescriptorBuilder;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;

import com.etendoerp.hibernate.spi.FunctionContributorRegistry;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

/**
 * Extends {@link PostgreSQLDialect} to provide additional support for custom PostgreSQL functions.
 * <p>
 * This class registers specific functions used in the context of Etendo.
 * Supports extension through {@link com.etendoerp.hibernate.spi.FunctionContributor} implementations.
 * </p>
 */
@Dependent
public class EtendoPostgresDialect extends PostgreSQLDialect {

  // Logger for function registration information.
  private static final Logger log = LogManager.getLogger();

  @Inject
  private FunctionContributorRegistry contributorRegistry;

  private BaseEtendoDialect baseDialect;

  /**
   * Default constructor.
   */
  public EtendoPostgresDialect() {
    super();
    initializeBaseDialect();
    if (baseDialect != null) {
      log.debug("Created Etendo specific Postgresql Dialect");
    }
  }

  /**
   * Constructor with dialect resolution info.
   *
   * @param info the dialect resolution information
   */
  public EtendoPostgresDialect(DialectResolutionInfo info) {
    super(info);
    initializeBaseDialect();
    if (baseDialect != null) {
      log.debug("Created Etendo specific Postgresql Dialect");
    }
  }

  /**
   * Constructor with explicit database version.
   *
   * @param version the database version
   */
  public EtendoPostgresDialect(DatabaseVersion version) {
    super(version);
    initializeBaseDialect();
    if (baseDialect != null) {
      log.debug("Created Etendo specific Postgresql Dialect");
    }
  }

  /**
   * Initialize the base dialect with PostgreSQL-specific implementations.
   */
  private void initializeBaseDialect() {
    baseDialect = new BaseEtendoDialect(contributorRegistry) {

      @Override
      protected void registerHqlAgg(FunctionContributions functionContributions) {
        PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
            functionContributions.getFunctionRegistry(),
            "hqlagg",
            FunctionKind.AGGREGATE,
            "array_to_string(array_agg(?1), ',')"
        );
        patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(1));
        patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.STRING_RETURN_TYPE).register();
      }

      @Override
      protected void registerTrunc(FunctionContributions functionContributions) {
        PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
            functionContributions.getFunctionRegistry(),
            "trunc",
            FunctionKind.NORMAL,
            "date_trunc('day', ?1)"
        );
        patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(1));
        patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.TIMESTAMP_RETURN_TYPE).register();
      }

      @Override
      protected void registerToDate(FunctionContributions functionContributions) {
        PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
            functionContributions.getFunctionRegistry(),
            "to_date",
            FunctionKind.NORMAL,
            "to_date($1, $2)"
        );
        patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.between(1, 2));
        patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.TIMESTAMP_RETURN_TYPE).register();
      }

      @Override
      protected void registerNow(FunctionContributions functionContributions) {
        PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
            functionContributions.getFunctionRegistry(),
            "now",
            FunctionKind.NORMAL,
            "now()"
        );
        patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.NO_ARGS);
        patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.TIMESTAMP_RETURN_TYPE).register();
      }

      @Override
      protected void registerAddDays(FunctionContributions functionContributions) {
        PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
            functionContributions.getFunctionRegistry(),
            "add_days",
            FunctionKind.NORMAL,
            "add_days(?1, ?2)"
        );
        patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(2));
        patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.TIMESTAMP_RETURN_TYPE).register();
      }

      @Override
      protected void registerSubtractDays(FunctionContributions functionContributions) {
        PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
            functionContributions.getFunctionRegistry(),
            "substract_days",
            FunctionKind.NORMAL,
            "substract_days(?1, ?2)"
        );
        patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(2));
        patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.TIMESTAMP_RETURN_TYPE).register();
      }

      @Override
      protected void registerSpecificFunctions(FunctionContributions functionContributions) {
        // PostgreSQL has no unique functions that don't exist in Oracle in this current setup
        // This hook is available for future PostgreSQL-only functions

        // Allow external modules to contribute PostgreSQL-specific functions
        if (contributorRegistry != null) {
          contributorRegistry.contributePostgresqlFunctions(functionContributions);
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

    baseDialect.log.debug("Registering add_days(timestamp, int):timestamp function");
    baseDialect.registerAddDays(functionContributions);

    log.debug("Registering substract_days(timestamp, int):timestamp function");
    baseDialect.registerSubtractDays(functionContributions);

    // Register PostgreSQL-only specific functions
    baseDialect.registerSpecificFunctions(functionContributions);

    log.debug("Function registry initialized for Etendo Postgresql Dialect");
  }

}
