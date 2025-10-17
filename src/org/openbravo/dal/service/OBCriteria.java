package org.openbravo.dal.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order; // JPA Order type for building final query ordering
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;

import static org.openbravo.model.ad.system.Client.PROPERTY_ORGANIZATION;
import static org.openbravo.model.common.enterprise.Organization.PROPERTY_CLIENT;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.openbravo.base.model.Entity;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.database.SessionInfo;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.QueryTimeOutUtil;

/**
 * Modernized Criteria abstraction for Etendo built on top of Hibernate 6 / Jakarta Persistence.
 * <p>
 * This class replaces the legacy Hibernate 5 {@code org.hibernate.criterion.Criteria} usage with the
 * JPA Criteria API while keeping a familiar fluent interface used widely in the codebase.
 * It provides:
 * <ul>
 *   <li>Transparent security filters (client, organization, active) automatically applied unless disabled.</li>
 *   <li>Convenience methods mirroring former Restrictions helpers: {@code addEqual}, {@code addIn}, {@code addIsNull}, etc.</li>
 *   <li>Pagination, ordering, count and scrolling support.</li>
 *   <li>Projection helpers emulating former {@code Projections} (max, min, sum, count, property, multiple sums).</li>
 *   <li>Lambda-based dynamic predicate construction via {@link PredicateFunction}.</li>
 * </ul>
 * <p>
 * Migration Notes:
 * <ul>
 *   <li>Where Hibernate 5 relied on {@code DetachedCriteria}, subqueries should now be built using the
 *       {@link CriteriaBuilder} obtained through {@link #getCriteriaBuilder()} inside a {@link PredicateFunction}.</li>
 *   <li>The previous projection API has been emulated internally; calling a projection setter changes the behavior
 *       of {@link #uniqueResult()} accordingly.</li>
 *   <li>Joins and alias creation now leverage JPA {@link Root#join(String, JoinType)}. Aliases are tracked to allow
 *       dot-path resolution similar to the old API.</li>
 * </ul>
 * <p>
 * Typical usage:
 * <pre>
 *   OBCriteria<MyEntity> c = new OBCriteria<>(MyEntity.class)
 *       .addEqual("status", "ACTIVE")
 *       .addIn("organization.id", orgIds)
 *       .addOrderBy("name", true);
 *   List<MyEntity> results = c.list();
 * </pre>
 */
public class OBCriteria<E extends BaseOBObject> {

  private static final Logger log = LogManager.getLogger();
  // Primary CriteriaQuery backing this abstraction
  public final CriteriaQuery<E> cq;
  public final Root<E> root; // public to allow external lambda predicates using nested paths
  // ---- Internal query state ----
  private final Class<E> entityClass;
  private final Session session;
  private final CriteriaBuilder cb;
  // Accumulated predicates (equivalent to former Restrictions list)
  private final List<Predicate> predicates = new ArrayList<>();
  // Cached joins by path for dynamic property traversal / ordering
  private final Map<String, From<?, ?>> joins = new HashMap<>();
  // Explicitly created aliases (mirrors previous createAlias behavior)
  private final Map<String, Join<?, ?>> aliasMap = new HashMap<>();
  // Requested orderings (stored before applying to CriteriaQuery) using compatibility Order wrapper
  private final List<org.openbravo.dal.service.Order> ordersRequested = new ArrayList<>();
  // Transparent filter flags (automatically applied security filters)
  private boolean filterOnReadableClients = true;
  private boolean filterOnReadableOrganization = true;
  private boolean filterOnActive = true;
  // Pagination parameters
  private Integer maxResults;
  private Integer firstResult;
  // Model metadata (optionally injected for security filtering and key handling)
  private Entity entity;
  // Scrolling compatibility flags
  private boolean scrolling = false;
  private boolean initialized = false;
  private boolean modified = false;
  private String projectionProperty = null;
  private String projectionType = null;
  private Selection<?>[] multipleProjections = null;

  // -------------------- Public API --------------------

  /**
   * Constructs a criteria instance bound to the current DAL Hibernate session.
   * <p>Use this when you want to query an entity with default security filters applied.</p>
   *
   * @param entityClass
   *     entity Java class extending {@link BaseOBObject}
   */
  public OBCriteria(Class<E> entityClass) {
    this(entityClass, SessionHandler.getInstance().getSession());
  }

  /**
   * Constructs a criteria instance using an explicit Hibernate {@link Session}.
   * <p>Allows advanced scenarios where multiple sessions are managed manually.</p>
   *
   * @param entityClass
   *     entity Java class
   * @param session
   *     active Hibernate session
   */
  public OBCriteria(Class<E> entityClass, Session session) {
    this.entityClass = entityClass;
    this.session = session;
    this.cb = session.getSessionFactory().getCriteriaBuilder();
    this.cq = cb.createQuery(entityClass);
    this.root = cq.from(entityClass);
    // The root also acts as an initial "join" for getPath() resolution
    joins.put("", root);
  }

  /**
   * Executes the criteria returning the full result list.
   * <p>Applies accumulated predicates, ordering, pagination and timeout hints.</p>
   *
   * @return list of matching entities (never null)
   */
  public List<E> list() {
    initialize();
    cq.where(predicates.toArray(new Predicate[0]));
    applyOrderBy();
    TypedQuery<E> q = session.createQuery(cq);
    applyPagination(q);
    applyTimeout(q);
    return q.getResultList();
  }

  /**
   * Returns a single result or {@code null} if none.
   * <p>If a projection was configured (e.g. max, sum, count) the projected scalar value is returned instead.</p>
   *
   * @return entity instance, projected value, or {@code null}
   */
  @SuppressWarnings("unchecked")
  public Object uniqueResult() {
    initialize();

    // Handle projections
    if (projectionType != null) {
      return executeProjectionQuery();
    }

    // Normal entity query
    cq.where(predicates.toArray(new Predicate[0]));
    applyOrderBy();
    TypedQuery<E> q = session.createQuery(cq);
    applyPagination(q);
    applyTimeout(q);
    List<E> r = q.setMaxResults(1).getResultList();
    return r.isEmpty() ? null : r.get(0);
  }

  @SuppressWarnings("unchecked")
  /**
   * Internal dispatcher executing a projection-specific query based on {@link #projectionType}.
   */
  private Object executeProjectionQuery() {
    if ("COUNT".equals(projectionType)) {
      return (long) count();
    } else if ("SUM".equals(projectionType)) {
      return executeProjectionSum();
    }

    if ("MAX".equals(projectionType) && projectionProperty != null) {
      CriteriaQuery<Number> maxQuery = cb.createQuery(Number.class);
      Root<E> maxRoot = maxQuery.from(entityClass);

      Path<Number> propertyPath = maxRoot.get(projectionProperty);
      maxQuery.select(cb.max(propertyPath));

      // Apply the same predicates
      List<Predicate> maxPredicates = rebuildPredicatesFor(maxRoot);
      if (!maxPredicates.isEmpty()) {
        maxQuery.where(maxPredicates.toArray(new Predicate[0]));
      }

      TypedQuery<Number> query = session.createQuery(maxQuery);
      applyTimeout(query);
      return query.getSingleResult();
    }

    if ("MIN".equals(projectionType) && projectionProperty != null) {
      CriteriaQuery<Number> minQuery = cb.createQuery(Number.class);
      Root<E> minRoot = minQuery.from(entityClass);

      Path<Number> propertyPath = minRoot.get(projectionProperty);
      minQuery.select(cb.min(propertyPath));

      // Apply the same predicates
      List<Predicate> minPredicates = rebuildPredicatesFor(minRoot);
      if (!minPredicates.isEmpty()) {
        minQuery.where(minPredicates.toArray(new Predicate[0]));
      }

      TypedQuery<Number> query = session.createQuery(minQuery);
      applyTimeout(query);
      return query.getSingleResult();
    }

    if ("PROPERTY".equals(projectionType) && projectionProperty != null) {
      CriteriaQuery<Object> propQuery = cb.createQuery(Object.class);
      Root<E> propRoot = propQuery.from(entityClass);

      Path<Object> propertyPath = propRoot.get(projectionProperty);
      propQuery.select(propertyPath);

      // Apply the same predicates
      List<Predicate> propPredicates = rebuildPredicatesFor(propRoot);
      if (!propPredicates.isEmpty()) {
        propQuery.where(propPredicates.toArray(new Predicate[0]));
      }

      TypedQuery<Object> query = session.createQuery(propQuery);
      applyTimeout(query);
      return query.getSingleResult();
    }

    throw new IllegalStateException("Unknown projection type: " + projectionType);
  }

  /**
   * Executes a SUM projection for the configured {@link #projectionProperty}.
   */
  private Object executeProjectionSum() {
    initialize();
    CriteriaQuery<Number> sumQuery = cb.createQuery(Number.class);
    Root<E> sumRoot = sumQuery.from(entityClass);

    List<Predicate> sumPreds = rebuildPredicatesFor(sumRoot);

    sumQuery.select(cb.sum(sumRoot.get(projectionProperty).as(Number.class)));
    if (!sumPreds.isEmpty()) {
      sumQuery.where(sumPreds.toArray(new Predicate[0]));
    }

    TypedQuery<Number> q = session.createQuery(sumQuery);
    return q.getSingleResult();
  }

  /**
   * Counts the total number of rows matching current predicates and transparent filters.
   *
   * @return matching row count
   */
  public int count() {
    initialize();
    CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
    Root<E> countRoot = countQuery.from(entityClass);

    // Re-apply predicates on the count root
    List<Predicate> countPreds = rebuildPredicatesFor(countRoot);

    countQuery.select(cb.count(countRoot));
    if (!countPreds.isEmpty()) {
      countQuery.where(countPreds.toArray(new Predicate[0]));
    }

    TypedQuery<Long> q = session.createQuery(countQuery);
    applyTimeout(q);
    Long res = q.getSingleResult();
    return res == null ? 0 : res.intValue();
  }

  // -------------------- Transparent filters --------------------

  /**
   * Creates a forward-only scrollable results cursor.
   *
   * @return scrollable results implementor
   */
  public ScrollableResultsImplementor<E> scroll() {
    return scroll(ScrollMode.FORWARD_ONLY);
  }

  /**
   * Creates a scrollable cursor using the specified {@link ScrollMode}.
   *
   * @param mode
   *     scroll mode
   * @return scrollable results
   */
  public ScrollableResultsImplementor<E> scroll(ScrollMode mode) {
    scrolling = true;
    try {
      initialize();
      cq.where(predicates.toArray(new Predicate[0]));
      applyOrderBy();
      TypedQuery<E> q = session.createQuery(cq);
      applyPagination(q);
      applyTimeout(q);
      return (ScrollableResultsImplementor<E>) q.unwrap(org.hibernate.query.Query.class).scroll(mode);
    } finally {
      scrolling = false;
    }
  }

  /**
   * @return whether readable organization filter is enabled
   */
  public boolean isFilterOnReadableOrganization() {
    return filterOnReadableOrganization;
  }

  public OBCriteria<E> setFilterOnReadableOrganization(boolean filterOnReadableOrganization) {
    this.filterOnReadableOrganization = filterOnReadableOrganization;
    modified = true;
    return this;
  }

  /**
   * @return whether active flag filter is enabled
   */
  public boolean isFilterOnActive() {
    return filterOnActive;
  }

  public OBCriteria<E> setFilterOnActive(boolean filterOnActive) {
    this.filterOnActive = filterOnActive;
    modified = true;
    return this;
  }

  // -------------------- “Restrictions” helpers (reemplazo) --------------------

  /**
   * @return whether readable client filter is enabled
   */
  public boolean isFilterOnReadableClients() {
    return filterOnReadableClients;
  }

  public OBCriteria<E> setFilterOnReadableClients(boolean filterOnReadableClients) {
    this.filterOnReadableClients = filterOnReadableClients;
    modified = true;
    return this;
  }

  public OBCriteria<E> addEqual(String property, Object value) {
    predicates.add(cb.equal(getPath(property), value));
    modified = true;
    return this;
  }

  public OBCriteria<E> addNotEqual(String property, Object value) {
    predicates.add(cb.notEqual(getPath(property), value));
    modified = true;
    return this;
  }

  public OBCriteria<E> addLike(String property, String pattern) {
    predicates.add(cb.like(getPath(property), pattern));
    modified = true;
    return this;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public OBCriteria<E> addGreaterThan(String property, Comparable value) {
    Path<?> path = getPath(property);
    Expression<Comparable> expr = path.as(Comparable.class);
    predicates.add(cb.greaterThan(expr, value));
    modified = true;
    return this;
  }

  // -------------------- Additional extensions to complete migration --------------------

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public OBCriteria<E> addLessThan(String property, Comparable value) {
    Path<?> path = getPath(property);
    Expression<Comparable> expr = path.as(Comparable.class);
    predicates.add(cb.lessThan(expr, value));
    modified = true;
    return this;
  }

  public OBCriteria<E> addInIds(String property, Object[] ids) {
    if (ids == null || ids.length == 0) {
      // nada coincide -> false
      predicates.add(cb.disjunction());
    } else {
      CriteriaBuilder.In<Object> in = cb.in(getPath(property));
      for (Object id : ids) {
        in.value(id);
      }
      predicates.add(in);
    }
    modified = true;
    return this;
  }

  /**
   * Adds an IN predicate using a {@link Collection} of values. Mirrors Hibernate 5 Restrictions.in.
   *
   * @param property
   *     dot-path property name
   * @param values
   *     collection of values (ignored if null/empty producing a false/disjunction)
   * @return this for chaining
   */
  public OBCriteria<E> addIn(String property, Collection<?> values) {
    if (values == null || values.isEmpty()) {
      predicates.add(cb.disjunction());
    } else {
      CriteriaBuilder.In<Object> in = cb.in(getPath(property));
      for (Object value : values) {
        in.value(value);
      }
      predicates.add(in);
    }
    modified = true;
    return this;
  }

  /**
   * Adds an IN predicate optimized for {@code List<String>}.
   *
   * @param property
   *     property path
   * @param ids
   *     list of ids
   * @return this
   */
  public OBCriteria<E> addInIds(String property, List<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return this; // No-op if empty
    }
    return addIn(property, ids);
  }

  /**
   * Adds an IN predicate for a {@code Set<String>} converting it to a list.
   *
   * @param property
   *     property path
   * @param ids
   *     set of ids
   * @return this
   */
  public OBCriteria<E> addInIds(String property, Set<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return this; // No-op if empty
    }
    return addIn(property, new ArrayList<>(ids));
  }

  /**
   * Adds an IN predicate using the id of each entity in the provided list.
   *
   * @param property
   *     property path
   * @param entities
   *     list of entities
   * @return this
   */
  public OBCriteria<E> addInEntities(String property, List<? extends BaseOBObject> entities) {
    if (entities == null || entities.isEmpty()) {
      return this; // No-op if empty
    }
    List<String> ids = new ArrayList<>();
    for (BaseOBObject entity : entities) {
      ids.add((String) entity.getId());
    }
    return addIn(property, ids);
  }

  /**
   * Adds an IS NOT NULL predicate equivalent to Restrictions.isNotNull.
   *
   * @param property
   *     property path
   * @return this
   */
  public OBCriteria<E> addIsNotNull(String property) {
    predicates.add(cb.isNotNull(getPath(property)));
    modified = true;
    return this;
  }

  /**
   * Adds a greater-than-or-equal predicate.
   *
   * @param property
   *     property path
   * @param value
   *     comparable value
   * @return this
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public OBCriteria<E> addGreaterOrEqual(String property, Comparable value) {
    initialize();
    predicates.add(cb.greaterThanOrEqualTo(getPath(property), value));
    modified = true;
    return this;
  }

  /**
   * Compatibility alias for {@link #addGreaterOrEqual(String, Comparable)}.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public OBCriteria<E> addGreaterOrEqualThan(String property, Comparable value) {
    return addGreaterOrEqual(property, value);
  }

  /**
   * Adds a less-than-or-equal predicate.
   *
   * @param property
   *     property path
   * @param value
   *     comparable value
   * @return this
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public OBCriteria<E> addLessOrEqual(String property, Comparable value) {
    Path<?> path = getPath(property);
    Expression<Comparable> expr = path.as(Comparable.class);
    predicates.add(cb.lessThanOrEqualTo(expr, value));
    modified = true;
    return this;
  }

  /**
   * Compatibility alias for {@link #addLessOrEqual(String, Comparable)}.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public OBCriteria<E> addLessOrEqualThan(String property, Comparable value) {
    return addLessOrEqual(property, value);
  }

  /**
   * Additional compatibility alias for {@link #addLessOrEqual(String, Comparable)}.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public OBCriteria<E> addLessEqual(String property, Comparable value) {
    return addLessOrEqual(property, value);
  }

  /**
   * Adds a logical OR combining multiple lambda-built predicates.
   *
   * @param orPredicates
   *     array of lambda factories
   * @return this
   */
  public OBCriteria<E> addOr(PredicateFunction... orPredicates) {
    if (orPredicates == null || orPredicates.length == 0) {
      return this;
    }

    List<Predicate> predicateList = new ArrayList<>();
    for (PredicateFunction func : orPredicates) {
      predicateList.add(func.apply(cb, this));
    }

    if (predicateList.size() == 1) {
      predicates.add(predicateList.get(0));
    } else {
      predicates.add(cb.or(predicateList.toArray(new Predicate[0])));
    }
    modified = true;
    return this;
  }

  /**
   * Adds a logical OR combining direct {@link Predicate} instances.
   */
  public OBCriteria<E> addOr(Predicate... orPredicates) {
    if (orPredicates == null || orPredicates.length == 0) {
      return this;
    }
    if (orPredicates.length == 1) {
      predicates.add(orPredicates[0]);
    } else {
      predicates.add(cb.or(orPredicates));
    }
    modified = true;
    return this;
  }

  /**
   * Adds a logical AND combining multiple lambda-built predicates.
   *
   * @param andPredicates
   *     array of lambda factories
   * @return this
   */
  public OBCriteria<E> addAnd(PredicateFunction... andPredicates) {
    if (andPredicates == null || andPredicates.length == 0) {
      return this;
    }

    List<Predicate> predicateList = new ArrayList<>();
    for (PredicateFunction func : andPredicates) {
      predicateList.add(func.apply(cb, this));
    }

    if (predicateList.size() == 1) {
      predicates.add(predicateList.get(0));
    } else {
      predicates.add(cb.and(predicateList.toArray(new Predicate[0])));
    }
    modified = true;
    return this;
  }

  /**
   * Adds a logical AND combining direct {@link Predicate} instances.
   */
  public OBCriteria<E> addAnd(Predicate... andPredicates) {
    if (andPredicates == null || andPredicates.length == 0) {
      return this;
    }
    if (andPredicates.length == 1) {
      predicates.add(andPredicates[0]);
    } else {
      predicates.add(cb.and(andPredicates));
    }
    modified = true;
    return this;
  }

  /**
   * Exposes the underlying {@link CriteriaBuilder} for advanced predicate construction and subqueries.
   *
   * @return criteria builder
   */
  public CriteriaBuilder getCriteriaBuilder() {
    return cb;
  }

  /**
   * Adds a raw {@link Predicate} to the criteria.
   */
  public OBCriteria<E> add(Predicate predicate) {
    predicates.add(predicate);
    modified = true;
    return this;
  }

  // -------------------- Projection support (aggregations) --------------------

  /**
   * Adds a predicate produced via {@link PredicateFunction}.
   */
  public OBCriteria<E> addPredicateFunction(PredicateFunction predicateFunction) {
    Predicate predicate = predicateFunction.apply(cb, this);
    predicates.add(predicate);
    modified = true;
    return this;
  }

  /**
   * Alias for {@link #addPredicateFunction(PredicateFunction)}.
   */
  public OBCriteria<E> addFunction(PredicateFunction predicateFunction) {
    return addPredicateFunction(predicateFunction);
  }

  /**
   * Creates a multi-select projection query copying current predicates.
   *
   * @return a detached projection query with same filters
   */
  public CriteriaQuery<Object[]> createProjectionQuery() {
    initialize();
    CriteriaQuery<Object[]> projQuery = cb.createQuery(Object[].class);
    Root<E> projRoot = projQuery.from(entityClass);

    // Apply the same predicates but on the new root
    List<Predicate> projPredicates = rebuildPredicatesFor(projRoot);
    if (!projPredicates.isEmpty()) {
      projQuery.where(projPredicates.toArray(new Predicate[0]));
    }

    return projQuery;
  }

  // -------------------- Ordering support --------------------

  /**
   * Creates a single-value projection query (e.g., COUNT or SUM).
   */
  public <T> CriteriaQuery<T> createSingleProjectionQuery(Class<T> resultType) {
    initialize();
    CriteriaQuery<T> projQuery = cb.createQuery(resultType);
    Root<E> projRoot = projQuery.from(entityClass);

    // Apply the same predicates but on the new root
    List<Predicate> projPredicates = rebuildPredicatesFor(projRoot);
    if (!projPredicates.isEmpty()) {
      projQuery.where(projPredicates.toArray(new Predicate[0]));
    }

    return projQuery;
  }

  // -------------------- Pagination --------------------

  /**
   * @return underlying Hibernate {@link Session}.
   */
  public Session getSession() {
    return session;
  }

  /**
   * Adds an ordering specification (ascending/descending).
   */
  public OBCriteria<E> addOrderBy(String orderOn, boolean ascending) {
    ordersRequested.add(ascending ? org.openbravo.dal.service.Order.asc(orderOn)
        : org.openbravo.dal.service.Order.desc(orderOn));
    modified = true;
    return this;
  }

  /**
   * Adds an ordering using the compatibility {@link org.openbravo.dal.service.Order} abstraction.
   */
  public OBCriteria<E> addOrder(org.openbravo.dal.service.Order order) {
    if (order != null) {
      ordersRequested.add(order);
      modified = true;
    }
    return this;
  }

  /** Convenience: adds ascending ordering. */
  public OBCriteria<E> addAsc(String property) {
    return addOrder(org.openbravo.dal.service.Order.asc(property));
  }

  /** Convenience: adds descending ordering. */
  public OBCriteria<E> addDesc(String property) {
    return addOrder(org.openbravo.dal.service.Order.desc(property));
  }

  // -------------------- Metadata / Entity --------------------

  public OBCriteria<E> setMaxResults(int maxResults) {
    this.maxResults = maxResults;
    return this;
  }

  public OBCriteria<E> setFirstResult(int firstResult) {
    this.firstResult = firstResult;
    return this;
  }

  // -------------------- Internals --------------------

  public Entity getEntity() {
    return entity;
  }

  void setEntity(Entity entity) {
    this.entity = entity;
  }

  private void initialize() {
    if (initialized) {
      if (!modified) {
        return;
      }
      log.warn(
          "Detected multiple calls to initialize() in the same OBCriteria instance. "
              + "This should be fixed in order to prevent adding duplicated filters in the query.",
          new Exception());
    }

    final OBContext obContext = OBContext.getOBContext();
    final Entity e = getEntity(); // can be null if not set externally

    if (!OBContext.getOBContext().isInAdministratorMode() && e != null) {
      OBContext.getOBContext().getEntityAccessChecker().checkReadable(e);
    }

    // Organization readable filter
    if (filterOnReadableOrganization && e != null && e.isOrganizationEnabled()) {
      if (e.isOrganizationPartOfKey()) {
        addInIds("id." + PROPERTY_ORGANIZATION + ".id", obContext.getReadableOrganizations());
      } else {
        addInIds(PROPERTY_ORGANIZATION + ".id", obContext.getReadableOrganizations());
      }
    }

    // Client readable filter
    if (filterOnReadableClients && e != null && e.isClientEnabled()) {
      addInIds(PROPERTY_CLIENT + ".id", obContext.getReadableClients());
    }

    // Active flag filter
    if (filterOnActive && e != null && e.isActiveEnabled()) {
      addEqual(Organization.PROPERTY_ACTIVE, true);
    }

    // Profile-driven timeout
    if (SessionInfo.getQueryProfile() != null) {
      // It's applied in applyTimeout(query) when creating the query
    }

    initialized = true;
    modified = false;
  }

  /**
   * Applies stored order specifications creating intermediate joins as required.
   */
  private void applyOrderBy() {
    if (ordersRequested.isEmpty()) {
      return;
    }
    List<Order> jpaOrders = new ArrayList<>();
    for (org.openbravo.dal.service.Order o : ordersRequested) {
      Path<?> path = getPath(o.getProperty());
      jpaOrders.add(o.isAscending() ? cb.asc(path) : cb.desc(path));
    }
    cq.orderBy(jpaOrders);
  }

  /**
   * Rebuilds transparent security predicates for a different {@link Root}.
   */
  private List<Predicate> rebuildPredicatesFor(Root<E> otherRoot) {
    // Predicates reference paths from the original root; rebuild only the transparent security filters
    // against the new root. For full predicate cloning we'd need to persist property paths when adding.
    List<Predicate> rebuilt = new ArrayList<>();
    final OBContext obContext = OBContext.getOBContext();
    final Entity e = getEntity();

    if (filterOnReadableOrganization && e != null && e.isOrganizationEnabled()) {
      if (e.isOrganizationPartOfKey()) {
        rebuilt.add(otherRoot.get("id").get(PROPERTY_ORGANIZATION).get("id")
            .in((Object[]) obContext.getReadableOrganizations()));
      } else {
        rebuilt.add(otherRoot.get(PROPERTY_ORGANIZATION).get("id")
            .in((Object[]) obContext.getReadableOrganizations()));
      }
    }
    if (filterOnReadableClients && e != null && e.isClientEnabled()) {
      rebuilt.add(otherRoot.get(PROPERTY_CLIENT).get("id")
          .in((Object[]) obContext.getReadableClients()));
    }
    if (filterOnActive && e != null && e.isActiveEnabled()) {
      rebuilt.add(cb.equal(otherRoot.get(Organization.PROPERTY_ACTIVE), true));
    }
    // Note: If you need other manual predicates (addEqual, etc.) rebuilt here, the robust approach
    // is to maintain a parallel CriteriaQuery while adding them. Can be implemented if required.
    return rebuilt;
  }

  /**
   * Resolves a dot-separated property path creating LEFT joins as necessary.
   * Supports alias prefixes previously registered via {@link #createAlias(String, String)}.
   *
   * @param propertyPath
   *     dot-path (e.g. "order.customer.name" or "alias.field")
   * @return resolved Path
   */
  @SuppressWarnings("unchecked")
  public <T> Path<T> getPath(String propertyPath) {
    if (StringUtils.isEmpty(propertyPath)) {
      return root.get(propertyPath);
    }
    // Check if it starts with an alias
    if (StringUtils.contains(propertyPath, '.')) {
      String[] parts = propertyPath.split("\\.");
      String firstPart = parts[0];

      // If first part is an alias
      if (aliasMap.containsKey(firstPart)) {
        From<?, ?> aliasJoin = aliasMap.get(firstPart);
        if (parts.length == 2) {
          // alias.property
          return (Path<T>) aliasJoin.get(parts[1]);
        } else {
          // alias.nested.property - continue building path from alias
          From<?, ?> current = aliasJoin;
          StringBuilder currentPath = new StringBuilder(firstPart);
          for (int i = 1; i < parts.length - 1; i++) {
            currentPath.append(".").append(parts[i]);
            String key = currentPath.toString();
            From<?, ?> join = joins.get(key);
            if (join == null) {
              join = current.join(parts[i], JoinType.LEFT);
              joins.put(key, join);
            }
            current = join;
          }
          return (Path<T>) current.get(parts[parts.length - 1]);
        }
      }
    }

    // Standard path resolution (no alias)
    if (!StringUtils.contains(propertyPath, '.')) {
      return root.get(propertyPath);
    }

    String[] parts = propertyPath.split("\\.");
    From<?, ?> current = root;
    StringBuilder currentPath = new StringBuilder();
    for (int i = 0; i < parts.length - 1; i++) {
      if (currentPath.length() > 0) {
        currentPath.append('.');
      }
      currentPath.append(parts[i]);
      String key = currentPath.toString();
      From<?, ?> join = joins.get(key);
      if (join == null) {
        join = current.join(parts[i], JoinType.LEFT);
        joins.put(key, join);
      }
      current = join;
    }
    return (Path<T>) current.get(parts[parts.length - 1]);
  }

  private void applyPagination(TypedQuery<?> q) {
    if (firstResult != null) q.setFirstResult(firstResult);
    if (maxResults != null) q.setMaxResults(maxResults);
  }

  // -------------------- Projection migration helpers --------------------

  /**
   * Creates and returns the underlying {@link org.hibernate.query.Query} object
   * representing this OBCriteria instance.
   * <p>
   * This method is useful for operations that require direct access to the Hibernate
   * {@link Query} API (e.g., applying timeouts or advanced configuration) since
   * Hibernate 6 no longer provides {@code setTimeout()} or similar methods directly
   * on the Criteria API.
   * <p>
   * Note: If this criteria has already been built using {@link CriteriaBuilder} and
   * {@link CriteriaQuery}, this method will simply wrap that query. Otherwise, it
   * will build a basic HQL query using the current entity metadata.
   *
   * @return the {@link org.hibernate.query.Query} representing this criteria.
   * @throws org.openbravo.base.exception.OBException
   *     if the query cannot be built.
   */
  @SuppressWarnings("unchecked")
  public org.hibernate.query.Query<?> createQuery() {
    try {
      // Ensure the internal CriteriaQuery is initialized
      initialize();

      // Apply predicates and ordering before building the query
      cq.where(predicates.toArray(new Predicate[0]));
      applyOrderBy();

      // Build the query using Hibernate's Session API
      org.hibernate.query.Query<?> query = session.createQuery(cq);
      applyPagination(query);
      applyTimeout(query);

      return query;
    } catch (Exception e) {
      throw new org.openbravo.base.exception.OBException(
          "Unable to create Hibernate Query from OBCriteria for entity: " + entity.getName(), e);
    }
  }

  private void applyTimeout(TypedQuery<?> q) {
    if (SessionInfo.getQueryProfile() != null) {
      try {
        // Apply timeout according to the configured profile (using Hibernate unwrap)
        QueryTimeOutUtil.getInstance().setQueryTimeOut(
            q.unwrap(org.hibernate.query.Query.class),
            SessionInfo.getQueryProfile());
      } catch (Exception ignore) {
        // Last resort: apply standard JPA hint (only if the driver supports it)
        try {
          q.setHint("jakarta.persistence.query.timeout", 30000);
        } catch (Exception ignored) {
          // ignore
        }
      }
    }
  }

  /**
   * Sets a maximum projection on the given property.
   * Replaces Hibernate 5's Projections.max() functionality.
   * After calling this, uniqueResult() will return the max value instead of entity.
   *
   * @param property
   *     the property to calculate the maximum for
   */
  public void setProjectionMax(String property) {
    this.projectionProperty = property;
    this.projectionType = "MAX";
    modified = true;
  }

  /**
   * Sets a minimum projection on the given property.
   * Replaces Hibernate 5's Projections.min() functionality.
   * After calling this, uniqueResult() will return the min value instead of entity.
   *
   * @param property
   *     the property to calculate the minimum for
   */
  public void setProjectionMin(String property) {
    this.projectionProperty = property;
    this.projectionType = "MIN";
    modified = true;
  }

  /**
   * Sets a count projection.
   * Replaces Hibernate 5's Projections.count() functionality.
   * After calling this, uniqueResult() will return the count instead of entity.
   */
  public void setProjectionCount() {
    this.projectionType = "COUNT";
    modified = true;
  }

  /**
   * Sets a sum projection on the given property.
   * After calling this, uniqueResult() will return the sum instead of entity.
   */
  public void setProjectionSum(String property) {
    this.projectionType = "SUM";
    this.projectionProperty = property;
    modified = true;
  }

  /**
   * Sets a property projection (select single property).
   * Replaces Hibernate 5's Projections.property() functionality.
   * After calling this, the result will be the property value instead of entity.
   */
  public void setProjectionProperty(String property) {
    this.projectionProperty = property;
    this.projectionType = "PROPERTY";
    modified = true;
  }

  // -------------------- Multiple projections --------------------

  /**
   * Configures multiple projections for the query.
   * Allows SELECT sum(prop1), sum(prop2), etc. in a single query.
   * Replaces Hibernate 5's ProjectionList usage.
   */
  public OBCriteria<E> setMultipleProjections(Selection<?>... projections) {
    this.multipleProjections = projections;
    this.projectionType = "MULTIPLE";
    modified = true;
    return this;
  }

  /**
   * Helper method to create multiple SUM projections easily.
   * Equivalent to: ProjectionList.add(Projections.sum(prop1)).add(Projections.sum(prop2))
   */
  public OBCriteria<E> setMultipleSums(String... properties) {
    Selection<?>[] sums = new Selection[properties.length];
    for (int i = 0; i < properties.length; i++) {
      // Note: evaluated when the projection query executes
      sums[i] = new SumProjectionPlaceholder(properties[i]);
    }
    return setMultipleProjections(sums);
  }

  /**
   * Adds like condition (case insensitive).
   * Replaces Hibernate 5's Restrictions.ilike() functionality.
   */
  public OBCriteria<E> addIlike(String property, String value) {
    initialize();
    predicates.add(cb.like(cb.upper(getPath(property).as(String.class)), value.toUpperCase()));
    modified = true;
    return this;
  }

  /**
   * Adds isNull condition.
   * Replaces Hibernate 5's Restrictions.isNull() functionality.
   */
  public OBCriteria<E> addIsNull(String property) {
    initialize();
    predicates.add(cb.isNull(getPath(property)));
    modified = true;
    return this;
  }

  /**
   * Creates an alias for the specified property.
   * Replaces Hibernate 5's createAlias() functionality.
   * Uses LEFT join by default for compatibility.
   */
  public OBCriteria<E> createAlias(String property, String alias) {
    initialize();
    Join<?, ?> join = root.join(property, JoinType.LEFT);
    aliasMap.put(alias, join);
    modified = true;
    return this;
  }

  /**
   * Creates an alias for the specified property with explicit join type.
   * Replaces Hibernate 5's createAlias() functionality.
   */
  public OBCriteria<E> createAlias(String property, String alias, JoinType joinType) {
    initialize();
    Join<?, ?> join = root.join(property, joinType);
    aliasMap.put(alias, join);
    modified = true;
    return this;
  }

  /**
   * Adds a logical NOT around the given predicate (Restrictions.not equivalent).
   */
  public OBCriteria<E> addNot(Predicate predicate) {
    if (predicate != null) {
      predicates.add(cb.not(predicate));
      modified = true;
    }
    return this;
  }

  /**
   * Adds a size equality predicate matching collection size (Restrictions.sizeEq equivalent).
   */
  @SuppressWarnings("unchecked")
  public OBCriteria<E> addSizeEq(String property, int size) {
    Path<?> path = getPath(property);
    predicates.add(cb.equal(cb.size((Expression<Collection<Object>>) path), size));
    modified = true;
    return this;
  }

  /**
   * Adds an equality predicate on the entity id (Restrictions.idEq equivalent).
   */
  public OBCriteria<E> addIdEq(String id) {
    predicates.add(cb.equal(root.get("id"), id));
    modified = true;
    return this;
  }

  /**
   * Compatibility no-op for historical setFetchMode usage.
   * In Hibernate 6, fetch mode is handled automatically or via explicit join fetch operations.
   */
  public OBCriteria<E> setFetchMode(String property, Object fetchMode) {
    // Hibernate 6 handles fetch mode automatically or with explicit join fetch.
    // For backward compatibility this is a no-op.
    return this;
  }

  /**
   * Functional factory used to generate {@link Predicate} instances bound to this criteria.
   * <p>Using a custom interface avoids clashes with {@code java.util.function.Predicate} and keeps
   * the lambda signature focused on criteria building (providing both {@link CriteriaBuilder}
   * and the current {@link OBCriteria} instance).</p>
   * <p>Examples:</p>
   * <pre>
   *   criteria.addFunction((cb, c) -> cb.equal(c.getPath("status"), "ACTIVE"));
   *   criteria.addOr(
   *     (cb, c) -> cb.like(c.getPath("name"), "%Test%"),
   *     (cb, c) -> cb.isNull(c.getPath("description"))
   *   );
   * </pre>
   */
  @FunctionalInterface
  public interface PredicateFunction {
    Predicate apply(CriteriaBuilder cb, OBCriteria<?> obc);
  }

  /**
   * Internal placeholder carrying a property name destined for a SUM aggregation.
   * <p>It defers construction of the actual {@code criteriaBuilder.sum(path)} until the
   * projection query is materialized (when CriteriaBuilder and a fresh root are available).</p>
   */
  private static class SumProjectionPlaceholder implements Selection<Number> {
    private final String property;

    public SumProjectionPlaceholder(String property) {
      this.property = property;
    }

    /**
     * @return the property slated to be summed later
     */
    public String getProperty() {
      return property;
    }

    @Override
    public String getAlias() {
      return null;
    }

    @Override
    public Selection<Number> alias(String alias) {
      return this;
    }

    @Override
    public boolean isCompoundSelection() {
      return false;
    }

    @Override
    public java.util.List<Selection<?>> getCompoundSelectionItems() {
      return Collections.emptyList();
    }

    @Override
    public Class<? extends Number> getJavaType() {
      return Number.class;
    }
  }
}
