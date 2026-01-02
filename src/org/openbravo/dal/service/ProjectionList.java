package org.openbravo.dal.service;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

// Projection list implementation for multiple projections
public class ProjectionList implements Projection {
  private final List<Projection> projections = new ArrayList<>();

  public ProjectionList() {
  }

  public ProjectionList add(Projection projection) {
    projections.add(projection);
    return this;
  }

  public List<Projection> getProjections() {
    return projections;
  }

  public int getLength() {
    return projections.size();
  }

  @Override
  public Expression<?> toExpression(CriteriaBuilder cb, Root<?> root) {
    // This shouldn't be called directly for projection lists
    // The list should be handled specially in the query execution
    throw new UnsupportedOperationException(
        "ProjectionList.toExpression should not be called directly. Use getProjections() instead.");
  }
}
