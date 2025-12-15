package org.openbravo.dal.service;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Detached criteria implementation for use in subqueries.
 * This allows creating criteria that can be used as subqueries in other criteria.
 *
 * @author refactored for Hibernate 6.5.2
 */
public class DetachedCriteria {

  private final Class<?> entityClass;
  private String alias;

  private List<RestrictionEntry> restrictions = new ArrayList<>();
  private List<AliasEntry> aliases = new ArrayList<>();
  private ProjectionEntry projection;

  /**
   * Create a detached criteria for the given entity class
   */
  private DetachedCriteria(Class<?> entityClass) {
    this.entityClass = entityClass;
    this.alias = "subquery";
  }

  /**
   * Create a detached criteria for the given entity class
   */
  private DetachedCriteria(Class<?> entityClass, String alias) {
    this.entityClass = entityClass;
    this.alias = alias;
  }

  /**
   * Factory method to create a detached criteria by entity class name
   *
   * @param entityClassName The entity class name
   * @return DetachedCriteria instance
   */
  public static DetachedCriteria forClass(String entityClassName) {
    try {
      Class<?> clazz = Class.forName(entityClassName);
      return new DetachedCriteria(clazz);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Entity class not found: " + entityClassName, e);
    }
  }

  /**
   * Factory method to create a detached criteria by entity class name
   *
   * @param entityName The entity class name
   * @return DetachedCriteria instance
   */
  public static DetachedCriteria forEntityName(String entityName, String alias) {
    try {
      Class<?> clazz = Class.forName(entityName);
      DetachedCriteria detachedCriteria = new DetachedCriteria(clazz);
      detachedCriteria.alias = alias;
      return detachedCriteria;
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Entity class not found: " + entityName, e);
    }
  }

  /**
   * Factory method to create a detached criteria by entity class
   *
   * @param entityClass The entity class
   * @return DetachedCriteria instance
   */
  public static DetachedCriteria forClass(Class<?> entityClass) {
    return new DetachedCriteria(entityClass);
  }

  /**
   * Factory method to create a detached criteria by entity class with alias
   *
   * @param entityClass The entity class
   * @param alias The alias
   * @return DetachedCriteria instance
   */
  public static DetachedCriteria forClass(Class<?> entityClass, String alias) {
    return new DetachedCriteria(entityClass, alias);
  }

  /**
   * Add a restriction to this criteria
   *
   * @param restriction The restriction to add
   * @return this DetachedCriteria instance, for method chaining
   */
  public DetachedCriteria add(Restriction restriction) {
    restrictions.add(new RestrictionEntry(restriction));
    return this;
  }

  /**
   * Create an alias for an association
   *
   * @param associationPath The path to the association
   * @param alias The alias to use
   * @return this DetachedCriteria instance, for method chaining
   */
  public DetachedCriteria createAlias(String associationPath, String alias) {
    aliases.add(new AliasEntry(associationPath, alias, null));
    return this;
  }

  /**
   * Create an alias for an association with specified join type
   *
   * @param associationPath The path to the association
   * @param alias The alias to use
   * @param joinType The type of join
   * @return this DetachedCriteria instance, for method chaining
   */
  public DetachedCriteria createAlias(String associationPath, String alias, jakarta.persistence.criteria.JoinType joinType) {
    aliases.add(new AliasEntry(associationPath, alias, joinType));
    return this;
  }

  /**
   * Set a projection for this criteria
   *
   * @param projection The projection to apply
   * @return this DetachedCriteria instance, for method chaining
   */
  public DetachedCriteria setProjection(Projection projection) {
    this.projection = new ProjectionEntry(projection);
    return this;
  }

  /**
   * Convert this detached criteria to a JPA Subquery
   *
   * @param cb CriteriaBuilder
   * @param query Parent query
   * @return Subquery instance
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public <T> Subquery<T> toSubquery(CriteriaBuilder cb, CriteriaQuery<?> query) {
    Subquery subquery = query.subquery(entityClass);
    Root subRoot = subquery.from(entityClass);

    // Apply projection if present
    if (projection != null) {
      Expression<?> projectionExpr = projection.toExpression(cb, subRoot);
      subquery.select(projectionExpr);
    } else {
      subquery.select(subRoot);
    }

    // Apply restrictions
    List<Predicate> predicates = new ArrayList<>();
    for (RestrictionEntry entry : restrictions) {
      Predicate predicate = entry.getRestriction().toPredicate(cb, subRoot);
      if (predicate != null) {
        predicates.add(predicate);
      }
    }

    if (!predicates.isEmpty()) {
      subquery.where(predicates.toArray(new Predicate[0]));
    }

    return subquery;
  }

  /**
   * Get the entity class
   */
  public Class<?> getEntityClass() {
    return entityClass;
  }

  // Inner classes

  static class RestrictionEntry {
    private final Restriction restriction;

    public RestrictionEntry(Restriction restriction) {
      this.restriction = restriction;
    }

    public Restriction getRestriction() {
      return restriction;
    }
  }

  static class AliasEntry {
    private final String path;
    private final String alias;
    private final jakarta.persistence.criteria.JoinType joinType;

    public AliasEntry(String path, String alias, jakarta.persistence.criteria.JoinType joinType) {
      this.path = path;
      this.alias = alias;
      this.joinType = joinType;
    }

    public String getPath() {
      return path;
    }

    public String getAlias() {
      return alias;
    }

    public jakarta.persistence.criteria.JoinType getJoinType() {
      return joinType;
    }
  }

  static class ProjectionEntry {
    private final Projection projection;

    public ProjectionEntry(Projection projection) {
      this.projection = projection;
    }

    @SuppressWarnings("rawtypes")
    public Expression<?> toExpression(CriteriaBuilder cb, Root<?> root) {
      return projection.toExpression(cb, root);
    }
  }
}