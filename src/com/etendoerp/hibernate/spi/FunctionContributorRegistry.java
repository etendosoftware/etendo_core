package com.etendoerp.hibernate.spi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.boot.model.FunctionContributions;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Central registry for managing function contributors across modules.
 * <p>
 * This service discovers all {@link FunctionContributor} implementations
 * annotated with {@link DatabaseFunction} and executes them in priority order.
 * </p>
 */
@ApplicationScoped
public class FunctionContributorRegistry {

  private static final Logger log = LogManager.getLogger();

  @Inject
  @DatabaseFunction
  private Instance<FunctionContributor> allContributors;

  /**
   * Register all common functions that work in both databases.
   *
   * @param functionContributions Function contributions provided by Hibernate
   */
  public void contributeCommonFunctions(FunctionContributions functionContributions) {
    log.debug("Registering common functions from external modules");
    executeContributors(DatabaseType.COMMON, functionContributions);
  }

  /**
   * Register PostgreSQL-specific functions.
   *
   * @param functionContributions Function contributions provided by Hibernate
   */
  public void contributePostgresqlFunctions(FunctionContributions functionContributions) {
    log.debug("Registering PostgreSQL-specific functions from external modules");
    executeContributors(DatabaseType.POSTGRESQL, functionContributions);
  }

  /**
   * Register Oracle-specific functions.
   *
   * @param functionContributions Function contributions provided by Hibernate
   */
  public void contributeOracleFunctions(FunctionContributions functionContributions) {
    log.debug("Registering Oracle-specific functions from external modules");
    executeContributors(DatabaseType.ORACLE, functionContributions);
  }

  /**
   * Execute contributors for a specific database type in priority order.
   */
  private void executeContributors(DatabaseType targetType, FunctionContributions functionContributions) {
    if (allContributors.isUnsatisfied()) {
      log.debug("No external function contributors found");
      return;
    }

    List<FunctionContributor> matchingContributors = new ArrayList<>();

    // Filter contributors by database type
    for (FunctionContributor contributor : allContributors) {
      DatabaseFunction annotation = contributor.getClass().getAnnotation(DatabaseFunction.class);
      if (annotation != null) {
        for (DatabaseType type : annotation.value()) {
          if (type == targetType) {
            matchingContributors.add(contributor);
            break;
          }
        }
      }
    }

    if (matchingContributors.isEmpty()) {
      log.debug("No function contributors found for database type: {}", targetType);
      return;
    }

    // Sort by priority (lower values first)
    matchingContributors.sort(Comparator.comparingInt(FunctionContributor::getPriority));

    log.debug("Found {} function contributor(s) for {}", matchingContributors.size(), targetType);

    for (FunctionContributor contributor : matchingContributors) {
      try {
        log.debug("Executing function contributor: {} (priority: {}) for database type: {}",
            contributor.getClass().getSimpleName(),
            contributor.getPriority(),
            targetType);
        contributor.contribute(functionContributions);
      } catch (Exception e) {
        log.error("Error executing function contributor: {} for database type: {}",
            contributor.getClass().getSimpleName(),
            targetType,
            e);
      }
    }
  }

}
