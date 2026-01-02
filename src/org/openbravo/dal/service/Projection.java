package org.openbravo.dal.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

/**
 * Interface for projections in OBCriteria.
 * Projections allow selecting specific properties or aggregated values instead of full entities.
 */
public interface Projection {

  /**
   * Convert this projection to a JPA Expression
   *
   * @param cb CriteriaBuilder to use for creating expressions
   * @param root Root entity for the query
   * @return The expression representing this projection
   */
  Expression<?> toExpression(CriteriaBuilder cb, Root<?> root);
}