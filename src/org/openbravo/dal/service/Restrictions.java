package org.openbravo.dal.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.Dependent;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Factory class for creating Restriction instances.
 * Provides static methods compatible with legacy Hibernate Criteria API.
 *
 * @author refactored for Hibernate 6.5.2
 */
@Dependent
public class Restrictions {

  /**
   * Apply an "equal" constraint to the named property
   *
   * @param propertyName The name of the property
   * @param value The value to use in comparison
   * @return Restriction
   */
  public static Restriction eq(String propertyName, Object value) {
    return new SimpleRestriction(propertyName, value, RestrictionType.EQ);
  }

  /**
   * Apply a "not equal" constraint to the named property
   *
   * @param propertyName The name of the property
   * @param value The value to use in comparison
   * @return Restriction
   */
  public static Restriction ne(String propertyName, Object value) {
    return new SimpleRestriction(propertyName, value, RestrictionType.NE);
  }

  /**
   * Apply a "greater than" constraint to the named property
   *
   * @param propertyName The name of the property
   * @param value The value to use in comparison
   * @return Restriction
   */
  public static Restriction gt(String propertyName, Object value) {
    return new SimpleRestriction(propertyName, value, RestrictionType.GT);
  }

  /**
   * Apply a "greater than or equal" constraint to the named property
   *
   * @param propertyName The name of the property
   * @param value The value to use in comparison
   * @return Restriction
   */
  public static Restriction ge(String propertyName, Object value) {
    return new SimpleRestriction(propertyName, value, RestrictionType.GE);
  }

  /**
   * Apply a "less than" constraint to the named property
   *
   * @param propertyName The name of the property
   * @param value The value to use in comparison
   * @return Restriction
   */
  public static Restriction lt(String propertyName, Object value) {
    return new SimpleRestriction(propertyName, value, RestrictionType.LT);
  }

  /**
   * Apply a "less than or equal" constraint to the named property
   *
   * @param propertyName The name of the property
   * @param value The value to use in comparison
   * @return Restriction
   */
  public static Restriction le(String propertyName, Object value) {
    return new SimpleRestriction(propertyName, value, RestrictionType.LE);
  }

  /**
   * Apply a "like" constraint to the named property
   *
   * @param propertyName The name of the property
   * @param value The value to use in comparison
   * @return Restriction
   */
  public static Restriction like(String propertyName, String value) {
    return new SimpleRestriction(propertyName, value, RestrictionType.LIKE);
  }

  /**
   * A case-insensitive "like" (similar to Postgres ilike operator)
   *
   * @param propertyName The name of the property
   * @param value The value to use in comparison
   * @return Restriction
   */
  public static Restriction ilike(String propertyName, String value) {
    return new ILikeRestriction(propertyName, value);
  }

  /**
   * Apply an "in" constraint to the named property
   *
   * @param propertyName The name of the property
   * @param values The literal values to use in the IN restriction
   * @return Restriction
   */
  public static Restriction in(String propertyName, Object... values) {
    return new InRestriction(propertyName, values);
  }

  /**
   * Apply an "in" constraint to the named property
   *
   * @param propertyName The name of the property
   * @param values The literal values to use in the IN restriction
   * @return Restriction
   */
  public static Restriction in(String propertyName, Collection<?> values) {
    return new InRestriction(propertyName, values.toArray());
  }

  /**
   * Apply an "is null" constraint to the named property
   *
   * @param propertyName The name of the property
   * @return Restriction
   */
  public static Restriction isNull(String propertyName) {
    return new NullRestriction(propertyName, true);
  }

  /**
   * Apply an "is not null" constraint to the named property
   *
   * @param propertyName The name of the property
   * @return Restriction
   */
  public static Restriction isNotNull(String propertyName) {
    return new NullRestriction(propertyName, false);
  }

  /**
   * Apply a "between" constraint to the named property
   *
   * @param propertyName The name of the property
   * @param low The low value
   * @param high The high value
   * @return Restriction
   */
  public static Restriction between(String propertyName, Object low, Object high) {
    return new BetweenRestriction(propertyName, low, high);
  }

  /**
   * Apply an "equal" constraint to two properties
   *
   * @param propertyName One property name
   * @param otherPropertyName The other property name
   * @return Restriction
   */
  public static Restriction eqProperty(String propertyName, String otherPropertyName) {
    return new PropertyComparisonRestriction(propertyName, otherPropertyName, ComparisonType.EQ);
  }

  /**
   * Apply a "not equal" constraint to two properties
   *
   * @param propertyName One property name
   * @param otherPropertyName The other property name
   * @return Restriction
   */
  public static Restriction neProperty(String propertyName, String otherPropertyName) {
    return new PropertyComparisonRestriction(propertyName, otherPropertyName, ComparisonType.NE);
  }

  /**
   * Apply a "less than" constraint to two properties
   *
   * @param propertyName One property name
   * @param otherPropertyName The other property name
   * @return Restriction
   */
  public static Restriction ltProperty(String propertyName, String otherPropertyName) {
    return new PropertyComparisonRestriction(propertyName, otherPropertyName, ComparisonType.LT);
  }

  /**
   * Apply a "less than or equal" constraint to two properties
   *
   * @param propertyName One property name
   * @param otherPropertyName The other property name
   * @return Restriction
   */
  public static Restriction leProperty(String propertyName, String otherPropertyName) {
    return new PropertyComparisonRestriction(propertyName, otherPropertyName, ComparisonType.LE);
  }

  /**
   * Apply a "greater than" constraint to two properties
   *
   * @param propertyName One property name
   * @param otherPropertyName The other property name
   * @return Restriction
   */
  public static Restriction gtProperty(String propertyName, String otherPropertyName) {
    return new PropertyComparisonRestriction(propertyName, otherPropertyName, ComparisonType.GT);
  }

  /**
   * Apply a "greater than or equal" constraint to two properties
   *
   * @param propertyName One property name
   * @param otherPropertyName The other property name
   * @return Restriction
   */
  public static Restriction geProperty(String propertyName, String otherPropertyName) {
    return new PropertyComparisonRestriction(propertyName, otherPropertyName, ComparisonType.GE);
  }

  /**
   * Constrain a collection valued property to be empty
   *
   * @param propertyName The name of the collection property
   * @return Restriction
   */
  public static Restriction isEmpty(String propertyName) {
    return new CollectionSizeRestriction(propertyName, 0, ComparisonType.EQ);
  }

  /**
   * Constrain a collection valued property to be non-empty
   *
   * @param propertyName The name of the collection property
   * @return Restriction
   */
  public static Restriction isNotEmpty(String propertyName) {
    return new CollectionSizeRestriction(propertyName, 0, ComparisonType.GT);
  }

  /**
   * Constrain a collection valued property by size (equals)
   *
   * @param propertyName The name of the collection property
   * @param size The size to use in comparison
   * @return Restriction
   */
  public static Restriction sizeEq(String propertyName, int size) {
    return new CollectionSizeRestriction(propertyName, size, ComparisonType.EQ);
  }

  /**
   * Constrain a collection valued property by size (not equals)
   *
   * @param propertyName The name of the collection property
   * @param size The size to use in comparison
   * @return Restriction
   */
  public static Restriction sizeNe(String propertyName, int size) {
    return new CollectionSizeRestriction(propertyName, size, ComparisonType.NE);
  }

  /**
   * Constrain a collection valued property by size (greater than)
   *
   * @param propertyName The name of the collection property
   * @param size The size to use in comparison
   * @return Restriction
   */
  public static Restriction sizeGt(String propertyName, int size) {
    return new CollectionSizeRestriction(propertyName, size, ComparisonType.GT);
  }

  /**
   * Constrain a collection valued property by size (less than)
   *
   * @param propertyName The name of the collection property
   * @param size The size to use in comparison
   * @return Restriction
   */
  public static Restriction sizeLt(String propertyName, int size) {
    return new CollectionSizeRestriction(propertyName, size, ComparisonType.LT);
  }

  /**
   * Constrain a collection valued property by size (greater than or equal)
   *
   * @param propertyName The name of the collection property
   * @param size The size to use in comparison
   * @return Restriction
   */
  public static Restriction sizeGe(String propertyName, int size) {
    return new CollectionSizeRestriction(propertyName, size, ComparisonType.GE);
  }

  /**
   * Constrain a collection valued property by size (less than or equal)
   *
   * @param propertyName The name of the collection property
   * @param size The size to use in comparison
   * @return Restriction
   */
  public static Restriction sizeLe(String propertyName, int size) {
    return new CollectionSizeRestriction(propertyName, size, ComparisonType.LE);
  }

  /**
   * Apply an "equal" constraint to the identifier property
   *
   * @param value The value to use in comparison
   * @return Restriction
   */
  public static Restriction idEq(Object value) {
    return new IdRestriction(value);
  }

  /**
   * Apply an "equals" constraint to each property in the key set of a Map
   *
   * @param propertyNameValues a map from property names to values
   * @return Restriction
   */
  public static Restriction allEq(Map<String, ?> propertyNameValues) {
    List<Restriction> restrictions = new ArrayList<>();
    for (Map.Entry<String, ?> entry : propertyNameValues.entrySet()) {
      restrictions.add(eq(entry.getKey(), entry.getValue()));
    }
    return and(restrictions.toArray(new Restriction[0]));
  }

  /**
   * Group expressions together in a single conjunction (A and B and C...)
   * This form creates an empty conjunction that can be added to later
   *
   * @return Conjunction
   */
  public static Conjunction conjunction() {
    return new Conjunction();
  }

  /**
   * Group expressions together in a single disjunction (A or B or C...)
   * This form creates an empty disjunction that can be added to later
   *
   * @return Disjunction
   */
  public static Disjunction disjunction() {
    return new Disjunction();
  }

  /**
   * Return the conjunction (AND) of two restrictions
   *
   * @param lhs One restriction
   * @param rhs The other restriction
   * @return Restriction
   */
  public static Restriction and(Restriction lhs, Restriction rhs) {
    return new LogicalRestriction(lhs, rhs, LogicalRestriction.Type.AND);
  }

  /**
   * Return the conjunction (AND) of multiple restrictions
   *
   * @param restrictions The restrictions to combine with AND
   * @return Restriction
   */
  public static Restriction and(Restriction... restrictions) {
    if (restrictions == null || restrictions.length == 0) {
      return new AlwaysTrueRestriction();
    }
    if (restrictions.length == 1) {
      return restrictions[0];
    }

    return new MultiLogicalRestriction(restrictions, LogicalRestriction.Type.AND);
  }

  /**
   * Return the disjunction (OR) of two restrictions
   *
   * @param lhs One restriction
   * @param rhs The other restriction
   * @return Restriction
   */
  public static Restriction or(Restriction lhs, Restriction rhs) {
    return new LogicalRestriction(lhs, rhs, LogicalRestriction.Type.OR);
  }

  /**
   * Return the disjunction (OR) of multiple restrictions
   *
   * @param restrictions The restrictions to combine with OR
   * @return Restriction
   */
  public static Restriction or(Restriction... restrictions) {
    if (restrictions == null || restrictions.length == 0) {
      return new AlwaysFalseRestriction();
    }
    if (restrictions.length == 1) {
      return restrictions[0];
    }

    return new MultiLogicalRestriction(restrictions, LogicalRestriction.Type.OR);
  }

  /**
   * Return the negation (NOT) of a restriction
   *
   * @param restriction The restriction to be negated
   * @return Restriction
   */
  public static Restriction not(Restriction restriction) {
    return new NotRestriction(restriction);
  }

  /**
   * Apply an "equal" constraint to the named property, or "is null" if value is null
   *
   * @param propertyName The name of the property
   * @param value The value to use in comparison
   * @return Restriction
   */
  public static Restriction eqOrIsNull(String propertyName, Object value) {
    return value == null ? isNull(propertyName) : eq(propertyName, value);
  }

  /**
   * Apply a "not equal" constraint to the named property, or "is not null" if value is null
   *
   * @param propertyName The name of the property
   * @param value The value to use in comparison
   * @return Restriction
   */
  public static Restriction neOrIsNotNull(String propertyName, Object value) {
    return value == null ? isNotNull(propertyName) : ne(propertyName, value);
  }

  /**
   * Create a conjunction (AND) of multiple restrictions
   *
   * @param restrictions The restrictions to combine
   * @return Restriction
   */
  public static Restriction conjunction(Restriction... restrictions) {
    if (restrictions.length == 0) {
      return new AlwaysTrueRestriction();
    }
    if (restrictions.length == 1) {
      return restrictions[0];
    }

    Restriction result = restrictions[0];
    for (int i = 1; i < restrictions.length; i++) {
      result = and(result, restrictions[i]);
    }
    return result;
  }

  /**
   * Create a disjunction (OR) of multiple restrictions
   *
   * @param restrictions The restrictions to combine
   * @return Restriction
   */
  public static Restriction disjunction(Restriction... restrictions) {
    if (restrictions.length == 0) {
      return new AlwaysFalseRestriction();
    }
    if (restrictions.length == 1) {
      return restrictions[0];
    }

    Restriction result = restrictions[0];
    for (int i = 1; i < restrictions.length; i++) {
      result = or(result, restrictions[i]);
    }
    return result;
  }

  // Helper method to get nested path
  @SuppressWarnings("rawtypes")
  static Path getPath(Root<?> root, String propertyName) {
    String[] parts = propertyName.split("\\.");
    Path path = root;

    for (String part : parts) {
      path = path.get(part);
    }

    return path;
  }

  // Restriction type enum
  enum RestrictionType {
    EQ, NE, GT, GE, LT, LE, LIKE
  }

  // Comparison type enum (for property comparisons and collections)
  enum ComparisonType {
    EQ, NE, GT, GE, LT, LE
  }

  // Simple restriction implementation
  static class SimpleRestriction implements Restriction {
    private final String propertyName;
    private final Object value;
    private final RestrictionType type;

    public SimpleRestriction(String propertyName, Object value, RestrictionType type) {
      this.propertyName = propertyName;
      this.value = value;
      this.type = type;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      Path path = getPath(root, propertyName);

      switch (type) {
        case EQ:
          return value != null ? cb.equal(path, value) : cb.isNull(path);
        case NE:
          return value != null ? cb.notEqual(path, value): cb.isNotNull(path);
        case GT:
          return cb.greaterThan(path, (Comparable) value);
        case GE:
          return cb.greaterThanOrEqualTo(path, (Comparable) value);
        case LT:
          return cb.lessThan(path, (Comparable) value);
        case LE:
          return cb.lessThanOrEqualTo(path, (Comparable) value);
        case LIKE:
          return cb.like(path, value.toString());
        default:
          throw new IllegalStateException("Unknown restriction type: " + type);
      }
    }
  }

  // IN restriction implementation
  static class InRestriction implements Restriction {
    private final String propertyName;
    private final Object[] values;

    public InRestriction(String propertyName, Object[] values) {
      this.propertyName = propertyName;
      this.values = values;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      Path path = getPath(root, propertyName);
      return path.in(values);
    }
  }

  // NULL restriction implementation
  static class NullRestriction implements Restriction {
    private final String propertyName;
    private final boolean isNull;

    public NullRestriction(String propertyName, boolean isNull) {
      this.propertyName = propertyName;
      this.isNull = isNull;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      Path path = getPath(root, propertyName);
      return isNull ? cb.isNull(path) : cb.isNotNull(path);
    }
  }

  // BETWEEN restriction implementation
  static class BetweenRestriction implements Restriction {
    private final String propertyName;
    private final Object low;
    private final Object high;

    public BetweenRestriction(String propertyName, Object low, Object high) {
      this.propertyName = propertyName;
      this.low = low;
      this.high = high;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      Path path = getPath(root, propertyName);
      return cb.between(path, (Comparable) low, (Comparable) high);
    }
  }

  // Logical restriction (AND/OR) implementation
  static class LogicalRestriction implements Restriction {
    enum Type { AND, OR }

    private final Restriction lhs;
    private final Restriction rhs;
    private final Type type;

    public LogicalRestriction(Restriction lhs, Restriction rhs, Type type) {
      this.lhs = lhs;
      this.rhs = rhs;
      this.type = type;
    }

    @Override
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      Predicate leftPredicate = lhs.toPredicate(cb, root);
      Predicate rightPredicate = rhs.toPredicate(cb, root);

      if (type == Type.AND) {
        return cb.and(leftPredicate, rightPredicate);
      } else {
        return cb.or(leftPredicate, rightPredicate);
      }
    }
  }

  // Multi logical restriction (AND/OR for multiple restrictions)
  static class MultiLogicalRestriction implements Restriction {
    private final Restriction[] restrictions;
    private final LogicalRestriction.Type type;

    public MultiLogicalRestriction(Restriction[] restrictions, LogicalRestriction.Type type) {
      this.restrictions = restrictions;
      this.type = type;
    }

    @Override
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      Predicate[] predicates = new Predicate[restrictions.length];
      for (int i = 0; i < restrictions.length; i++) {
        predicates[i] = restrictions[i].toPredicate(cb, root);
      }

      if (type == LogicalRestriction.Type.AND) {
        return cb.and(predicates);
      } else {
        return cb.or(predicates);
      }
    }
  }

  // NOT restriction implementation
  static class NotRestriction implements Restriction {
    private final Restriction restriction;

    public NotRestriction(Restriction restriction) {
      this.restriction = restriction;
    }

    @Override
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      return cb.not(restriction.toPredicate(cb, root));
    }
  }

  // Always true restriction (for empty conjunctions)
  static class AlwaysTrueRestriction implements Restriction {
    @Override
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      return cb.conjunction();
    }
  }

  // Always false restriction (for empty disjunctions)
  static class AlwaysFalseRestriction implements Restriction {
    @Override
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      return cb.disjunction();
    }
  }

  // Case-insensitive LIKE restriction
  static class ILikeRestriction implements Restriction {
    private final String propertyName;
    private final String value;

    public ILikeRestriction(String propertyName, String value) {
      this.propertyName = propertyName;
      this.value = value;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      Path path = getPath(root, propertyName);
      return cb.like(cb.lower(path), value.toLowerCase());
    }
  }

  // Property comparison restriction
  static class PropertyComparisonRestriction implements Restriction {
    private final String propertyName;
    private final String otherPropertyName;
    private final ComparisonType type;

    public PropertyComparisonRestriction(String propertyName, String otherPropertyName, ComparisonType type) {
      this.propertyName = propertyName;
      this.otherPropertyName = otherPropertyName;
      this.type = type;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      Path path1 = getPath(root, propertyName);
      Path path2 = getPath(root, otherPropertyName);

      switch (type) {
        case EQ:
          return cb.equal(path1, path2);
        case NE:
          return cb.notEqual(path1, path2);
        case GT:
          return cb.greaterThan(path1, path2);
        case GE:
          return cb.greaterThanOrEqualTo(path1, path2);
        case LT:
          return cb.lessThan(path1, path2);
        case LE:
          return cb.lessThanOrEqualTo(path1, path2);
        default:
          throw new IllegalStateException("Unknown comparison type: " + type);
      }
    }
  }

  // Collection size restriction
  static class CollectionSizeRestriction implements Restriction {
    private final String propertyName;
    private final int size;
    private final ComparisonType type;

    public CollectionSizeRestriction(String propertyName, int size, ComparisonType type) {
      this.propertyName = propertyName;
      this.size = size;
      this.type = type;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      Expression<Integer> sizeExpr = cb.size(root.get(propertyName));

      switch (type) {
        case EQ:
          return cb.equal(sizeExpr, size);
        case NE:
          return cb.notEqual(sizeExpr, size);
        case GT:
          return cb.greaterThan(sizeExpr, size);
        case GE:
          return cb.greaterThanOrEqualTo(sizeExpr, size);
        case LT:
          return cb.lessThan(sizeExpr, size);
        case LE:
          return cb.lessThanOrEqualTo(sizeExpr, size);
        default:
          throw new IllegalStateException("Unknown comparison type: " + type);
      }
    }
  }

  // ID restriction
  static class IdRestriction implements Restriction {
    private final Object value;

    public IdRestriction(Object value) {
      this.value = value;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      Path idPath = root.get("id");
      return cb.equal(idPath, value);
    }
  }

  // Conjunction class for dynamic AND building
  public static class Conjunction implements Restriction {
    private final List<Restriction> restrictions = new ArrayList<>();

    public Conjunction() {
    }

    public Conjunction add(Restriction restriction) {
      restrictions.add(restriction);
      return this;
    }

    @Override
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      if (restrictions.isEmpty()) {
        return cb.conjunction();
      }

      Predicate[] predicates = new Predicate[restrictions.size()];
      for (int i = 0; i < restrictions.size(); i++) {
        predicates[i] = restrictions.get(i).toPredicate(cb, root);
      }

      return cb.and(predicates);
    }
  }

  // Disjunction class for dynamic OR building
  public static class Disjunction implements Restriction {
    private final List<Restriction> restrictions = new ArrayList<>();

    public Disjunction() {
    }

    public Disjunction add(Restriction restriction) {
      restrictions.add(restriction);
      return this;
    }

    @Override
    public <T> Predicate toPredicate(CriteriaBuilder cb, Root<T> root) {
      if (restrictions.isEmpty()) {
        return cb.disjunction();
      }

      Predicate[] predicates = new Predicate[restrictions.size()];
      for (int i = 0; i < restrictions.size(); i++) {
        predicates[i] = restrictions.get(i).toPredicate(cb, root);
      }

      return cb.or(predicates);
    }
  }

  // Private constructor to prevent instantiation
  private Restrictions() {
  }
}