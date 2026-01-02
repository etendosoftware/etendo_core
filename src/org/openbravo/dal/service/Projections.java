package org.openbravo.dal.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

/**
 * Factory class for creating Projection instances.
 * Provides static methods compatible with legacy Hibernate Projections API.
 *
 * @author refactored for Hibernate 6.5.2
 */
public class Projections {

  /**
   * Create a row count projection (count(*))
   *
   * @return Projection
   */
  public static Projection rowCount() {
    return new RowCountProjection();
  }

  /**
   * Create a count projection for a specific property
   *
   * @param propertyName The property to count
   * @return Projection
   */
  public static Projection count(String propertyName) {
    return new CountProjection(propertyName, false);
  }

  /**
   * Create a distinct count projection
   *
   * @param propertyName The property to count
   * @return Projection
   */
  public static Projection countDistinct(String propertyName) {
    return new CountProjection(propertyName, true);
  }

  /**
   * Create a max projection
   *
   * @param propertyName The property to get max value
   * @return Projection
   */
  public static Projection max(String propertyName) {
    return new AggregateProjection(propertyName, AggregateType.MAX);
  }

  /**
   * Create a min projection
   *
   * @param propertyName The property to get min value
   * @return Projection
   */
  public static Projection min(String propertyName) {
    return new AggregateProjection(propertyName, AggregateType.MIN);
  }

  /**
   * Create a sum projection
   *
   * @param propertyName The property to sum
   * @return Projection
   */
  public static Projection sum(String propertyName) {
    return new AggregateProjection(propertyName, AggregateType.SUM);
  }

  /**
   * Create an avg projection
   *
   * @param propertyName The property to average
   * @return Projection
   */
  public static Projection avg(String propertyName) {
    return new AggregateProjection(propertyName, AggregateType.AVG);
  }

  /**
   * Create a property projection (select a specific property)
   *
   * @param propertyName The property to select
   * @return Projection
   */
  public static Projection property(String propertyName) {
    return new PropertyProjection(propertyName);
  }

  /**
   * Create a distinct projection
   *
   * @param projection The projection to make distinct
   * @return Projection
   */
  public static Projection distinct(Projection projection) {
    return new DistinctProjection(projection);
  }

  /**
   * Create a group property projection (for GROUP BY)
   *
   * @param propertyName The property to group by
   * @return Projection
   */
  public static Projection groupProperty(String propertyName) {
    return new PropertyProjection(propertyName);
  }

  /**
   * Create an identifier projection (primary key)
   *
   * @return Projection
   */
  public static Projection id() {
    return new IdProjection();
  }

  /**
   * Create a projection list to combine multiple projections
   *
   * @return ProjectionList
   */
  public static ProjectionList projectionList() {
    return new ProjectionList();
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

  // Aggregate type enum
  enum AggregateType {
    MAX, MIN, SUM, AVG
  }

  // Row count projection implementation
  static class RowCountProjection implements Projection {
    @Override
    public Expression<?> toExpression(CriteriaBuilder cb, Root<?> root) {
      return cb.count(root);
    }
  }

  // Count projection implementation
  static class CountProjection implements Projection {
    private final String propertyName;
    private final boolean distinct;

    public CountProjection(String propertyName, boolean distinct) {
      this.propertyName = propertyName;
      this.distinct = distinct;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Expression<?> toExpression(CriteriaBuilder cb, Root<?> root) {
      Path path = getPath(root, propertyName);
      if (distinct) {
        return cb.countDistinct(path);
      } else {
        return cb.count(path);
      }
    }
  }

  // Aggregate projection implementation
  static class AggregateProjection implements Projection {
    private final String propertyName;
    private final AggregateType type;

    public AggregateProjection(String propertyName, AggregateType type) {
      this.propertyName = propertyName;
      this.type = type;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Expression<?> toExpression(CriteriaBuilder cb, Root<?> root) {
      Path path = getPath(root, propertyName);

      switch (type) {
        case MAX:
          return cb.max(path);
        case MIN:
          return cb.min(path);
        case SUM:
          return cb.sum(path);
        case AVG:
          return cb.avg(path);
        default:
          throw new IllegalStateException("Unknown aggregate type: " + type);
      }
    }
  }

  // Property projection implementation
  static class PropertyProjection implements Projection {
    private final String propertyName;

    public PropertyProjection(String propertyName) {
      this.propertyName = propertyName;
    }

    @Override
    public Expression<?> toExpression(CriteriaBuilder cb, Root<?> root) {
      return getPath(root, propertyName);
    }
  }

  // Distinct projection implementation
  static class DistinctProjection implements Projection {
    private final Projection projection;

    public DistinctProjection(Projection projection) {
      this.projection = projection;
    }

    @Override
    public Expression<?> toExpression(CriteriaBuilder cb, Root<?> root) {
      Expression<?> expr = projection.toExpression(cb, root);
      // Note: DISTINCT is typically handled at the query level in JPA
      // This is a simplified implementation
      return expr;
    }
  }

  // ID projection implementation
  static class IdProjection implements Projection {
    @Override
    public Expression<?> toExpression(CriteriaBuilder cb, Root<?> root) {
      return root.get("id");
    }
  }

  // Private constructor to prevent instantiation
  private Projections() {
  }
}