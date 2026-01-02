package org.openbravo.dal.service;

/**
 * Represents fetch mode for associations in OBCriteria.
 * Controls whether associations are loaded eagerly or lazily.
 *
 * This is a simplified version compatible with Hibernate 6.5.2
 *
 * @author refactored for Hibernate 6.5.2
 */
public enum FetchMode {

  /**
   * Default fetch mode - use the default behavior defined in the mapping
   */
  DEFAULT,

  /**
   * Fetch using a join (eager loading)
   * This will load the association in the same query
   */
  JOIN,

  /**
   * Fetch using a select (lazy loading)
   * This will load the association in a separate query when accessed
   */
  SELECT;

  /**
   * Get the default fetch mode
   *
   * @return FetchMode.DEFAULT
   */
  public static FetchMode getDefault() {
    return DEFAULT;
  }
}