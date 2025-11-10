package com.etendoerp.hibernate;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.PatternFunctionDescriptorBuilder;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EtendoPostgreSQLDialect extends PostgreSQLDialect {

  private static final Logger log = LogManager.getLogger();
  private static final FunctionReturnTypeResolver INTEGER_RETURN_TYPE;
  static {
    TypeConfiguration typeConfiguration = new TypeConfiguration();
    BasicType<Integer> integerBasicType = typeConfiguration.getBasicTypeRegistry().resolve(
        StandardBasicTypes.INTEGER);
    INTEGER_RETURN_TYPE = StandardFunctionReturnTypeResolvers.invariant(integerBasicType);
  }
  private static final FunctionReturnTypeResolver STRING_RETURN_TYPE;
  static {
    TypeConfiguration typeConfiguration = new TypeConfiguration();
    BasicType<String> stringBasicType = typeConfiguration.getBasicTypeRegistry().resolve(StandardBasicTypes.STRING);
    STRING_RETURN_TYPE = StandardFunctionReturnTypeResolvers.invariant(stringBasicType);
  }
  private static final FunctionReturnTypeResolver TIMESTAMP_RETURN_TYPE;
  static {
    TypeConfiguration typeConfiguration = new TypeConfiguration();
    BasicType<Date> timestampBasicType = typeConfiguration.getBasicTypeRegistry().resolve(StandardBasicTypes.TIMESTAMP);
    TIMESTAMP_RETURN_TYPE = StandardFunctionReturnTypeResolvers.invariant(timestampBasicType);
  }

  @Override
  public void initializeFunctionRegistry(FunctionContributions functionContributions) {
    super.initializeFunctionRegistry(functionContributions);

    log.info("Registering trunc(timestamp):timestamp function");
    registerTruncFunction(functionContributions);
    log.info("Registering ad_isorgincluded(character varying, character varying, character varying):numeric function");
    registerAdIsOrgInclided(functionContributions);
    log.info("Registering ad_isorgincluded_treenode(character varying, character varying, character varying):numeric function");
    registerAdIsOrgInclidedTreeNode(functionContributions);
    log.info("Registering ad_org_isinnaturaltree(character varying, character varying, character varying):character varying function");
    registerAdIsOrgIsInNaturalTree(functionContributions);
    log.info("Registering ad_org_getcalendarowner(character varying):character varying function");
    registerAdOrgGetCalendarOwner(functionContributions);
    log.info("Registering obequals(numeric, numeric):character varying function");
    registerObEquals(functionContributions);
    log.info("Registering add_days(timestamp, int):timestamp function");
    registerAddDays(functionContributions);
    log.info("Registering now():timestamp function");
    registerNow(functionContributions);
    log.info("Registering hqlagg(character varying):character varying function");
    registerHqlAgg(functionContributions);
    log.info("Registering aprm_seqnoinvpaidstatus(character varying, character varying, character):character varying function");
    registerAprmSeqNoInvPaidStatus(functionContributions);
    log.info("Registering aprm_seqnumberpaymentstatus(character varying):character varying function");
    registerAprmSeqNumberPaymentStatus(functionContributions);

    //TODO: Add more custom functions if needed
  }

  private static void registerTruncFunction(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "trunc",
        FunctionKind.NORMAL,
        "date_trunc('day', ?1)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(1));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(TIMESTAMP_RETURN_TYPE).register();
  }

  private static void registerAdIsOrgInclided(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "ad_isorgincluded",
        FunctionKind.NORMAL,
        "ad_isorgincluded(?1, ?2, ?3)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(3));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(INTEGER_RETURN_TYPE).register();
  }

  private static void registerAdIsOrgInclidedTreeNode(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "ad_isorgincluded_treenode",
        FunctionKind.NORMAL,
        "ad_isorgincluded_treenode(?1, ?2, ?3)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(3));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(INTEGER_RETURN_TYPE).register();
  }

  private static void registerAdIsOrgIsInNaturalTree(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "ad_org_isinnaturaltree",
        FunctionKind.NORMAL,
        "ad_org_isinnaturaltree(?1, ?2, ?3)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(3));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(STRING_RETURN_TYPE).register();
  }

  private static void registerAdOrgGetCalendarOwner(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "ad_org_getcalendarowner",
        FunctionKind.NORMAL,
        "ad_org_getcalendarowner(?1)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(1));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(STRING_RETURN_TYPE).register();
  }

  private static void registerObEquals(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "obequals",
        FunctionKind.NORMAL,
        "obequals(?1, ?2)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(2));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(STRING_RETURN_TYPE).register();
  }

  private static void registerAddDays(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "add_days",
        FunctionKind.NORMAL,
        "add_days(?1, ?2)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(2));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(TIMESTAMP_RETURN_TYPE).register();
  }

  private static void registerNow(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "now",
        FunctionKind.NORMAL,
        "now()"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.NO_ARGS);
    patternFunctionDescriptorBuilder.setReturnTypeResolver(TIMESTAMP_RETURN_TYPE).register();
  }

  private static void registerHqlAgg(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "hqlagg",
        FunctionKind.AGGREGATE,
        "array_to_string(array_agg(?1), ',')"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(1));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(STRING_RETURN_TYPE).register();
  }

  private static void registerAprmSeqNoInvPaidStatus(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "aprm_seqnoinvpaidstatus",
        FunctionKind.NORMAL,
        "aprm_seqnoinvpaidstatus(?1, ?2, ?3)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(3));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(STRING_RETURN_TYPE).register();
  }

  private static void registerAprmSeqNumberPaymentStatus(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "aprm_seqnumberpaymentstatus",
        FunctionKind.NORMAL,
        "aprm_seqnumberpaymentstatus(?1)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(1));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(STRING_RETURN_TYPE).register();
  }

}
