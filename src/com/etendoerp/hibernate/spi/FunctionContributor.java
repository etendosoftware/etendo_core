
package com.etendoerp.hibernate.spi;

import org.hibernate.boot.model.FunctionContributions;

/**
 * Interface for contributing custom database functions to Etendo dialects.
 * <p>
 * Modules can implement this interface to register their own custom functions
 * that will be available in HQL/JPQL queries.
 * </p>
 * <p>
 * Implementations should be annotated with {@link DatabaseFunction} to specify
 * which databases they support.
 * </p>
 *
 * @see DatabaseFunction
 */
public interface FunctionContributor {

  /**
   * Register custom functions for the specified database dialect.
   *
   * @param functionContributions Function contributions provided by Hibernate
   */
  void contribute(FunctionContributions functionContributions);

  /**
   * Priority order for this contributor. Lower values execute first.
   * Default is 100.
   *
   * @return the priority value
   */
  default int getPriority() {
    return 100;
  }
}
