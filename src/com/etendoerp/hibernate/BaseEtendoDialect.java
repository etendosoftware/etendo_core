package com.etendoerp.hibernate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.PatternFunctionDescriptorBuilder;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;

import com.etendoerp.hibernate.spi.FunctionContributorRegistry;

/**
 * Base class for Etendo custom dialects.
 * <p>
 * Provides common function registration logic that can be shared across different database dialects.
 * Supports extension through {@link com.etendoerp.hibernate.spi.FunctionContributor} implementations.
 * </p>
 */
public abstract class BaseEtendoDialect {

  protected static final Logger log = LogManager.getLogger();
  protected FunctionContributorRegistry contributorRegistry;

  /**
   * Constructor that accepts a FunctionContributorRegistry.
   *
   * @param contributorRegistry The registry for external function contributors
   */
  protected BaseEtendoDialect(FunctionContributorRegistry contributorRegistry) {
    this.contributorRegistry = contributorRegistry;
  }

  /**
   * Registers common functions that are available in both Oracle and PostgreSQL with identical implementations.
   *
   * @param functionContributions Function contributions provided by Hibernate.
   */
  protected void registerCommonFunctions(FunctionContributions functionContributions) {
    log.debug("Registering common functions");

    log.debug("Registering ad_isorgincluded(character varying, character varying, character varying):numeric function");
    registerAdIsOrgIncluded(functionContributions);

    log.debug("Registering ad_isorgincluded_treenode(character varying, character varying, character varying):numeric function");
    registerAdIsOrgIncludedTreeNode(functionContributions);

    log.debug("Registering ad_org_isinnaturaltree(character varying, character varying, character varying):character varying function");
    registerAdOrgIsInNaturalTree(functionContributions);

    log.debug("Registering ad_org_getcalendarowner(character varying):character varying function");
    registerAdOrgGetCalendarOwner(functionContributions);

    log.debug("Registering obequals(numeric, numeric):character varying function");
    registerObEquals(functionContributions);

    log.debug("Registering aprm_seqnoinvpaidstatus(character varying, character varying, character):character varying function");
    registerAprmSeqNoInvPaidStatus(functionContributions);

    log.debug("Registering m_get_default_aum_for_document(character varying, character varying):character varying function");
    registerMGetDefaultAumForDocument(functionContributions);

    log.debug("Registering m_get_converted_aumqty(character varying, character varying, character varying):numeric function");
    registerMGetConvertedAumQty(functionContributions);

    log.debug("Registering aprm_seqnumberpaymentstatus(character varying):character varying function");
    registerAprmSeqNumberPaymentStatus(functionContributions);

    log.debug("Registering ad_org_getperiodcontrolallow(character varying):character varying function");
    registerAdOrgGetPeriodControlAllow(functionContributions);

    log.debug("Registering ad_org_getperiodcontrolallowtn(character varying):character varying function");
    registerAdOrgGetPeriodControlAllowtn(functionContributions);

    log.debug("Registering get_uuid():character varying function");
    registerGetUUID(functionContributions);

    log.debug("Registering m_isparent_ch_value(character varying, character varying, character varying):character varying function");
    registerMIsParentChValue(functionContributions);

    log.debug("Registering m_getjsondescription(character varying, character varying, character varying):character varying function");
    registerMGetjsondescription(functionContributions);

    log.debug("Registering m_getjsondescription(character varying, character varying, character varying, character varying):character varying function");
    registerMGetjsondescription2(functionContributions);

    log.debug("Registering to_timestamp():timestamp function");
    registerToTimestamp(functionContributions);

    log.debug("Common functions registered");
  }

  // ========== COMMON CUSTOM ETENDO FUNCTIONS ==========

  /**
   * Registers the `ad_isorgincluded` function in the function registry.
   * This function is available in both Oracle and PostgreSQL.
   *
   * @param functionContributions Function contributions provided by Hibernate.
   */
  protected static void registerAdIsOrgIncluded(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "ad_isorgincluded",
        FunctionKind.NORMAL,
        "ad_isorgincluded(?1, ?2, ?3)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(3));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.INTEGER_RETURN_TYPE).register();
  }

  protected static void registerAdIsOrgIncludedTreeNode(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "ad_isorgincluded_treenode",
        FunctionKind.NORMAL,
        "ad_isorgincluded_treenode(?1, ?2, ?3)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(3));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.INTEGER_RETURN_TYPE).register();
  }

  protected static void registerAdOrgIsInNaturalTree(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "ad_org_isinnaturaltree",
        FunctionKind.NORMAL,
        "ad_org_isinnaturaltree(?1, ?2, ?3)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(3));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.STRING_RETURN_TYPE).register();
  }

  protected static void registerAdOrgGetCalendarOwner(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "ad_org_getcalendarowner",
        FunctionKind.NORMAL,
        "ad_org_getcalendarowner(?1)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(1));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.STRING_RETURN_TYPE).register();
  }

  protected static void registerObEquals(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "obequals",
        FunctionKind.NORMAL,
        "obequals(?1, ?2)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(2));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.STRING_RETURN_TYPE).register();
  }

  protected static void registerAprmSeqNoInvPaidStatus(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "aprm_seqnoinvpaidstatus",
        FunctionKind.NORMAL,
        "aprm_seqnoinvpaidstatus(?1, ?2, ?3)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(3));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.STRING_RETURN_TYPE).register();
  }

  protected static void registerMGetDefaultAumForDocument(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "m_get_default_aum_for_document",
        FunctionKind.NORMAL,
        "m_get_default_aum_for_document(?1, ?2)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(2));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.STRING_RETURN_TYPE).register();
  }

  protected static void registerMGetConvertedAumQty(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "m_get_converted_aumqty",
        FunctionKind.NORMAL,
        "m_get_converted_aumqty(?1, ?2, ?3)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(3));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.BIGDECIMAL_RETURN_TYPE).register();
  }

  protected static void registerAprmSeqNumberPaymentStatus(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "aprm_seqnumberpaymentstatus",
        FunctionKind.NORMAL,
        "aprm_seqnumberpaymentstatus(?1)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(1));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.STRING_RETURN_TYPE).register();
  }

  protected static void registerAdOrgGetPeriodControlAllow(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "ad_org_getperiodcontrolallow",
        FunctionKind.NORMAL,
        "ad_org_getperiodcontrolallow(?1)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(1));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.STRING_RETURN_TYPE).register();
  }

  protected static void registerAdOrgGetPeriodControlAllowtn(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "ad_org_getperiodcontrolallowtn",
        FunctionKind.NORMAL,
        "ad_org_getperiodcontrolallowtn(?1)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(1));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.STRING_RETURN_TYPE).register();
  }

  protected static void registerGetUUID(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "get_uuid",
        FunctionKind.NORMAL,
        "get_uuid()"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.NO_ARGS);
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.STRING_RETURN_TYPE).register();
  }

  protected static void registerMIsParentChValue(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "m_isparent_ch_value",
        FunctionKind.NORMAL,
        "m_isparent_ch_value(?1, ?2, ?3)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(3));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.INTEGER_RETURN_TYPE).register();
  }

  protected static void registerMGetjsondescription(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "m_getjsondescription",
        FunctionKind.NORMAL,
        "m_getjsondescription(?1, ?2, ?3"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(3));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.STRING_RETURN_TYPE).register();
  }

  protected static void registerMGetjsondescription2(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "m_getjsondescription",
        FunctionKind.NORMAL,
        "m_getjsondescription(?1, ?2, ?3, ?4)"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.exactly(4));
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.STRING_RETURN_TYPE).register();
  }

  protected void registerToTimestamp(FunctionContributions functionContributions) {
    PatternFunctionDescriptorBuilder patternFunctionDescriptorBuilder = new PatternFunctionDescriptorBuilder(
        functionContributions.getFunctionRegistry(),
        "to_timestamp",
        FunctionKind.NORMAL,
        "to_timestamp()"
    );
    patternFunctionDescriptorBuilder.setArgumentsValidator(StandardArgumentsValidators.NO_ARGS);
    patternFunctionDescriptorBuilder.setReturnTypeResolver(FunctionReturnType.TIMESTAMP_RETURN_TYPE).register();
  }

  // ========== ABSTRACT METHODS FOR DATABASE-SPECIFIC IMPLEMENTATIONS ==========

  /**
   * Registers the `hqlagg` function in the function registry.
   * The implementation differs between databases:
   * - PostgreSQL: array_to_string(array_agg(?1), ',')
   * - Oracle: listagg(?1, ',') within group (order by ?1)
   *
   * @param functionContributions Function contributions provided by Hibernate.
   */
  protected abstract void registerHqlAgg(FunctionContributions functionContributions);

  /**
   * Registers the `trunc` function in the function registry.
   * The implementation differs between databases:
   * - PostgreSQL: date_trunc('day', ?1)
   * - Oracle: trunc(?1)
   *
   * @param functionContributions Function contributions provided by Hibernate.
   */
  protected abstract void registerTrunc(FunctionContributions functionContributions);

  /**
   * Registers the `to_date` function in the function registry.
   * Both databases have this function but with slight syntax differences.
   *
   * @param functionContributions Function contributions provided by Hibernate.
   */
  protected abstract void registerToDate(FunctionContributions functionContributions);

  /**
   * Registers the `now` function in the function registry.
   * The implementation differs between databases:
   * - PostgreSQL: now()
   * - Oracle: sysdate or current_timestamp
   *
   * @param functionContributions Function contributions provided by Hibernate.
   */
  protected abstract void registerNow(FunctionContributions functionContributions);

  /**
   * Registers the `add_days` function in the function registry.
   * The implementation differs between databases:
   * - PostgreSQL: add_days(?1, ?2)
   * - Oracle: ?1 + ?2
   *
   * @param functionContributions Function contributions provided by Hibernate.
   */
  protected abstract void registerAddDays(FunctionContributions functionContributions);

  /**
   * Registers the `substract_days` function in the function registry.
   * The implementation differs between databases:
   * - PostgreSQL: substract_days(?1, ?2)
   * - Oracle: ?1 - ?2
   *
   * @param functionContributions Function contributions provided by Hibernate.
   */
  protected abstract void registerSubtractDays(FunctionContributions functionContributions);

  /**
   * Hook method for registering database-specific functions that don't exist in the other database.
   *
   * @param functionContributions Function contributions provided by Hibernate.
   */
  protected abstract void registerSpecificFunctions(FunctionContributions functionContributions);
}