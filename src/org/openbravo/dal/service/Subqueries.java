package org.openbravo.dal.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Factory class for creating subquery-based restrictions.
 * Provides static methods compatible with legacy Hibernate Subqueries API.
 *
 * @author refactored for Hibernate 6.5.2
 */
public class Subqueries {

  /**
   * Creates a criterion which checks for the existence of rows in the subquery result
   *
   * @param dc The detached criteria representing the subquery
   * @return The Restriction
   */
  public static Restriction exists(DetachedCriteria dc) {
    return new ExistsRestriction(dc, false);
  }

  /**
   * Creates a criterion which checks for the non-existence of rows in the subquery result
   *
   * @param dc The detached criteria representing the subquery
   * @return The Restriction
   */
  public static Restriction notExists(DetachedCriteria dc) {
    return new ExistsRestriction(dc, true);
  }

  /**
   * Creates a criterion which checks that the value of a given property is in the set of values
   * in the subquery result.
   *
   * @param propertyName The name of the property to use in comparison
   * @param dc The detached criteria representing the subquery
   * @return The Restriction
   */
  public static Restriction propertyIn(String propertyName, DetachedCriteria dc) {
    return new PropertySubqueryRestriction(propertyName, dc, SubqueryOperator.IN);
  }

  /**
   * Creates a criterion which checks that the value of a given property is not in the set of values
   * in the subquery result.
   *
   * @param propertyName The name of the property to use in comparison
   * @param dc The detached criteria representing the subquery
   * @return The Restriction
   */
  public static Restriction propertyNotIn(String propertyName, DetachedCriteria dc) {
    return new PropertySubqueryRestriction(propertyName, dc, SubqueryOperator.NOT_IN);
  }

  /**
   * Creates a criterion which checks that the value of a given property equals the value in
   * the subquery result.
   *
   * @param propertyName The name of the property to use in comparison
   * @param dc The detached criteria representing the subquery
   * @return The Restriction
   */
  public static Restriction propertyEq(String propertyName, DetachedCriteria dc) {
    return new PropertySubqueryRestriction(propertyName, dc, SubqueryOperator.EQ);
  }

  /**
   * Creates a criterion which checks that the value of a given property is not equal to the value
   * in the subquery result.
   *
   * @param propertyName The name of the property to use in comparison
   * @param dc The detached criteria representing the subquery
   * @return The Restriction
   */
  public static Restriction propertyNe(String propertyName, DetachedCriteria dc) {
    return new PropertySubqueryRestriction(propertyName, dc, SubqueryOperator.NE);
  }

  /**
   * Creates a criterion which checks that the value of a given property is greater than the value
   * in the subquery result.
   *
   * @param propertyName The name of the property to use in comparison
   * @param dc The detached criteria representing the subquery
   * @return The Restriction
   */
  public static Restriction propertyGt(String propertyName, DetachedCriteria dc) {
    return new PropertySubqueryRestriction(propertyName, dc, SubqueryOperator.GT);
  }

  /**
   * Creates a criterion which checks that the value of a given property is less than the value
   * in the subquery result.
   *
   * @param propertyName The name of the property to use in comparison
   * @param dc The detached criteria representing the subquery
   * @return The Restriction
   */
  public static Restriction propertyLt(String propertyName, DetachedCriteria dc) {
    return new PropertySubqueryRestriction(propertyName, dc, SubqueryOperator.LT);
  }

  /**
   * Creates a criterion which checks that the value of a given property is greater than or equal to
   * the value in the subquery result.
   *
   * @param propertyName The name of the property to use in comparison
   * @param dc The detached criteria representing the subquery
   * @return The Restriction
   */
  public static Restriction propertyGe(String propertyName, DetachedCriteria dc) {
    return new PropertySubqueryRestriction(propertyName, dc, SubqueryOperator.GE);
  }

  /**
   * Creates a criterion which checks that the value of a given property is less than or equal to
   * the value in the subquery result.
   *
   * @param propertyName The name of the property to use in comparison
   * @param dc The detached criteria representing the subquery
   * @return The Restriction
   */
  public static Restriction propertyLe(String propertyName, DetachedCriteria dc) {
    return new PropertySubqueryRestriction(propertyName, dc, SubqueryOperator.LE);
  }

  /**
   * Creates a criterion which checks that the value of a literal is IN the values in the
   * subquery result.
   *
   * @param value The literal value to use in comparison
   * @param dc The detached criteria representing the subquery
   * @return The Restriction
   */
  public static Restriction in(Object value, DetachedCriteria dc) {
    return new ValueSubqueryRestriction(value, dc, SubqueryOperator.IN);
  }

  /**
   * Creates a criterion which checks that the value of a literal is NOT IN the values in the
   * subquery result.
   *
   * @param value The literal value to use in comparison
   * @param dc The detached criteria representing the subquery
   * @return The Restriction
   */
  public static Restriction notIn(Object value, DetachedCriteria dc) {
    return new ValueSubqueryRestriction(value, dc, SubqueryOperator.NOT_IN);
  }

  /**
   * Creates a criterion which checks that the value of a literal equals the value in
   * the subquery result.
   *
   * @param value The literal value to use in comparison
   * @param dc The detached criteria representing the subquery
   * @return The Restriction
   */
  public static Restriction eq(Object value, DetachedCriteria dc) {
    return new ValueSubqueryRestriction(value, dc, SubqueryOperator.EQ);
  }

  /**
   * Creates a criterion which checks that the value of a literal is not equal to the value in
   * the subquery result.
   *
   * @param value The literal value to use in comparison
   * @param dc The detached criteria representing the subquery
   * @return The Restriction
   */
  public static Restriction ne(Object value, DetachedCriteria dc) {
    return new ValueSubqueryRestriction(value, dc, SubqueryOperator.NE);
  }

  // Helper method to get path
  @SuppressWarnings("rawtypes")
  private static Path getPath(Root<?> root, String propertyName) {
    String[] parts = propertyName.split("\\.");
    Path path = root;

    for (String part : parts) {
      path = path.get(part);
    }

    return path;
  }

  // Subquery operator enum
  enum SubqueryOperator {
    IN, NOT_IN, EQ, NE, GT, LT, GE, LE
  }

  // EXISTS restriction implementation
  static class ExistsRestriction implements Restriction {
    private final DetachedCriteria detachedCriteria;
    private final boolean negate;

    public ExistsRestriction(DetachedCriteria detachedCriteria, boolean negate) {
      this.detachedCriteria = detachedCriteria;
      this.negate = negate;
    }

    @Override
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      throw new UnsupportedOperationException(
          "Subquery restrictions require the query context. This should not be called directly.");
    }

    public <T> Predicate toPredicateWithQuery(CriteriaBuilder cb, CriteriaQuery<?> query, Root<T> root) {
      Subquery<?> subquery = detachedCriteria.toSubquery(cb, query);

      if (negate) {
        return cb.not(cb.exists(subquery));
      } else {
        return cb.exists(subquery);
      }
    }
  }

  // Property subquery restriction implementation
  static class PropertySubqueryRestriction implements Restriction {
    private final String propertyName;
    private final DetachedCriteria detachedCriteria;
    private final SubqueryOperator operator;

    public PropertySubqueryRestriction(String propertyName, DetachedCriteria detachedCriteria,
        SubqueryOperator operator) {
      this.propertyName = propertyName;
      this.detachedCriteria = detachedCriteria;
      this.operator = operator;
    }

    @Override
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      throw new UnsupportedOperationException(
          "Subquery restrictions require the query context. This should not be called directly.");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> Predicate toPredicateWithQuery(CriteriaBuilder cb, CriteriaQuery<?> query, Root<T> root) {
      Path path = getPath(root, propertyName);
      Subquery subquery = detachedCriteria.toSubquery(cb, query);

      switch (operator) {
        case IN:
          return path.in(subquery);
        case NOT_IN:
          return cb.not(path.in(subquery));
        case EQ:
          return cb.equal(path, subquery);
        case NE:
          return cb.notEqual(path, subquery);
        case GT:
          return cb.greaterThan(path, subquery);
        case LT:
          return cb.lessThan(path, subquery);
        case GE:
          return cb.greaterThanOrEqualTo(path, subquery);
        case LE:
          return cb.lessThanOrEqualTo(path, subquery);
        default:
          throw new IllegalStateException("Unknown subquery operator: " + operator);
      }
    }
  }

  // Value subquery restriction implementation
  static class ValueSubqueryRestriction implements Restriction {
    private final Object value;
    private final DetachedCriteria detachedCriteria;
    private final SubqueryOperator operator;

    public ValueSubqueryRestriction(Object value, DetachedCriteria detachedCriteria,
        SubqueryOperator operator) {
      this.value = value;
      this.detachedCriteria = detachedCriteria;
      this.operator = operator;
    }

    @Override
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      throw new UnsupportedOperationException(
          "Subquery restrictions require the query context. This should not be called directly.");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> Predicate toPredicateWithQuery(CriteriaBuilder cb, CriteriaQuery<?> query, Root<T> root) {
      Expression valueExpr = cb.literal(value);
      Subquery subquery = detachedCriteria.toSubquery(cb, query);

      switch (operator) {
        case IN:
          return valueExpr.in(subquery);
        case NOT_IN:
          return cb.not(valueExpr.in(subquery));
        case EQ:
          return cb.equal(valueExpr, subquery);
        case NE:
          return cb.notEqual(valueExpr, subquery);
        case GT:
          return cb.greaterThan(valueExpr, subquery);
        case LT:
          return cb.lessThan(valueExpr, subquery);
        case GE:
          return cb.greaterThanOrEqualTo(valueExpr, subquery);
        case LE:
          return cb.lessThanOrEqualTo(valueExpr, subquery);
        default:
          throw new IllegalStateException("Unknown subquery operator: " + operator);
      }
    }
  }

  // Private constructor to prevent instantiation
  private Subqueries() {
  }
}