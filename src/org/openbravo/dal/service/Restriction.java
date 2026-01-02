package org.openbravo.dal.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Base interface for all restriction types.
 * Restrictions are converted to JPA Predicates when the query is executed.
 */
public interface Restriction {

  /**
   * Convert this restriction to a JPA Predicate
   *
   * @param cb CriteriaBuilder to use for creating predicates
   * @param root Root entity for the query
   * @return The predicate representing this restriction
   */
  <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root);
}
