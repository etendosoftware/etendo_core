package org.openbravo.dal.service;

import static org.openbravo.model.ad.system.Client.PROPERTY_ORGANIZATION;
import static org.openbravo.model.common.enterprise.Organization.PROPERTY_CLIENT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.util.CheckException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.database.SessionInfo;
import org.openbravo.model.common.enterprise.Organization;

/**
 * This object is an implementation wrapping JPA Criteria API for Hibernate 6.5.2.
 * It adds transparent client and organization filtering to queries.
 * <p>
 * This class offers convenience methods to set orderby and restrictions compatible
 * with the legacy Hibernate Criteria API.
 *
 * @see OBContext#getReadableClients()
 * @see OBContext#getReadableOrganizations()
 *
 * @author mtaal (original)
 * @author refactored for Hibernate 6.5.2
 */
public class OBCriteria<E extends BaseOBObject> {
  private static final Logger log = LogManager.getLogger();
  private static final String ROOT_ALIAS = "e";

  private final Session session;
  private Class<E> entityClass;
  private Entity entity;
  private String alias;

  private boolean filterOnReadableClients = true;
  private boolean filterOnReadableOrganization = true;
  private boolean filterOnActive = true;

  private List<RestrictionEntry> restrictions = new ArrayList<>();
  private List<OrderByEntry> orderBys = new ArrayList<>();
  private List<AliasEntry> aliases = new ArrayList<>();
  private Map<String, FetchMode> fetchModes = new HashMap<>();

  private ProjectionEntry projection;

  private Integer maxResults;
  private Integer firstResult;
  private Integer timeout;

  private boolean initialized = false;

  // ----------------------- Class Constructor ------------------------------- //
  public OBCriteria(String entityOrClassName) {
    this(entityOrClassName, ROOT_ALIAS, SessionHandler.getInstance().getSession());
  }

  public OBCriteria(String entityOrClassName, Session session) {
    this(entityOrClassName, ROOT_ALIAS, session);
  }

  public OBCriteria(String entityOrClassName, String alias) {
    this(entityOrClassName, alias, SessionHandler.getInstance().getSession());
  }

  @SuppressWarnings("unchecked")
  public OBCriteria(String entityOrClassName, String alias, Session session) {
    this.session = session;
    this.alias = alias;
    try {
      entity = ModelProvider.getInstance().getEntity(entityOrClassName, true);
      entityClass = (Class<E>) entity.getMappingClass();
    } catch (CheckException e) {
      try {
        entityClass = (Class<E>) Class.forName(entityOrClassName);
        entity = ModelProvider.getInstance().getEntity(entityClass);
      } catch (ClassNotFoundException ex) {
        throw new OBException("Entity or class not found: " + entityOrClassName, ex);
      }
    } catch (Exception ex){
      throw new OBException("Entity or class not found: " + entityOrClassName, ex);
    }
  }
  // ----------------------- End Class Constructor ------------------------------- //

  /**
   * Execute the query and return the list of results
   *
   * @return the list of Objects retrieved through this Criteria object
   */
  public List<E> list() {
    if (projection != null) {
      // When projection is set, return raw results (not entities)
      return (List<E>) executeProjectionQueryRaw();
    }

    CriteriaBuilder cb = session.getCriteriaBuilder();
    CriteriaQuery<E> cq = cb.createQuery(entityClass);
    Root<E> root = cq.from(entityClass);

    applyAliases(root);
    applyPredicates(cb, cq, root);
    applyOrderBy(cb, cq, root);

    Query<E> query = session.createQuery(cq);
    applyPagination(query);
    applyTimeout(query);

    if (log.isDebugEnabled()) {
      log.debug("Executing criteria query for " + entityClass.getName());
    }

    return query.getResultList();
  }

  public <F> List<F> list(Class<F> className) {
    if (projection != null) {
      return executeProjectionQuery(className);
    } else {
      throw new OBException("Projection must be set to use this method");
    }
  }

  /**
   * Execute projection query returning raw results (for compatibility with legacy code)
   */
  @SuppressWarnings("unchecked")
  private List<?> executeProjectionQueryRaw() {
    // Check if it's a ProjectionList (multiple projections)
    if (projection.getProjection() instanceof ProjectionList) {
      return executeMultiProjectionQuery();
    } else {
      return executeSingleProjectionQuery();
    }
  }

  /**
   * Execute query with single projection
   */
  private List<?> executeSingleProjectionQuery() {
    CriteriaBuilder cb = session.getCriteriaBuilder();
    CriteriaQuery<Object> cq = cb.createQuery(Object.class);
    Root<E> root = cq.from(entityClass);

    // Apply projection
    Expression<?> projectionExpr = projection.toExpression(cb, root);
    cq.select(projectionExpr);

    applyPredicates(cb, cq, root);
    applyAliases(root);

    Query<Object> query = session.createQuery(cq);
    applyPagination(query);
    applyTimeout(query);

    return query.getResultList();
  }

  /**
   * Execute query with multiple projections (ProjectionList)
   */
  private List<?> executeMultiProjectionQuery() {
    CriteriaBuilder cb = session.getCriteriaBuilder();
    CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
    Root<E> root = cq.from(entityClass);

    // Get all projections from ProjectionList
    ProjectionList projectionList = (ProjectionList) projection.getProjection();
    List<Projection> projections = projectionList.getProjections();

    // Convert each projection to an expression
    Expression<?>[] expressions = new Expression<?>[projections.size()];
    for (int i = 0; i < projections.size(); i++) {
      expressions[i] = projections.get(i).toExpression(cb, root);
    }

    // Use multiselect for multiple projections
    cq.multiselect(expressions);

    applyPredicates(cb, cq, root);
    applyAliases(root);

    Query<Object[]> query = session.createQuery(cq);
    applyPagination(query);
    applyTimeout(query);

    return query.getResultList();
  }

  /**
   * Execute projection query
   */
  @SuppressWarnings("unchecked")
  private <F> List<F> executeProjectionQuery(Class<F> className) {
    CriteriaBuilder cb = session.getCriteriaBuilder();
    CriteriaQuery<Object> cq = cb.createQuery(Object.class);
    Root<E> root = cq.from(entityClass);

    // Apply projection
    Expression<?> projectionExpr = projection.toExpression(cb, root);
    cq.select(projectionExpr);

    applyPredicates(cb, cq, root);
    applyAliases(root);

    Query<Object> query = session.createQuery(cq);
    applyPagination(query);
    applyTimeout(query);

    List<Object> rawResults = query.getResultList();
    List<F> results = new ArrayList<>();
    for (Object result : rawResults) {
      results.add((F) result);
    }
    return results;
  }

  /**
   * A convenience method which returns the count of objects.
   *
   * @return the count of the objects using the filter set in this Criteria
   */
  public int count() {
    if (projection != null) {
      throw new OBException("Projection must not be set to to use this method");
    }
    CriteriaBuilder cb = session.getCriteriaBuilder();
    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    Root<E> root = cq.from(entityClass);

    cq.select(cb.count(root));
    applyPredicates(cb, cq, root);
    applyAliases(root);

    Query<Long> query = session.createQuery(cq);

    if (log.isDebugEnabled()) {
      log.debug("Counting using criteria for " + entityClass.getName());
    }

    Long result = query.getSingleResult();
    return result != null ? result.intValue() : 0;
  }

  /**
   * Execute the query and return scrollable results
   */
  public ScrollableResults scroll() {
    return scroll(ScrollMode.FORWARD_ONLY);
  }

  /**
   * Execute the query and return scrollable results with specified scroll mode
   */
  public ScrollableResults scroll(ScrollMode scrollMode) {
    if (projection != null) {
      throw new OBException("Projection must not be set to to use this method");
    }
    CriteriaBuilder cb = session.getCriteriaBuilder();
    CriteriaQuery<E> cq = cb.createQuery(entityClass);
    Root<E> root = cq.from(entityClass);

    applyPredicates(cb, cq, root);
    applyAliases(root);
    applyOrderBy(cb, cq, root);

    Query<E> query = session.createQuery(cq);
    applyPagination(query);
    applyTimeout(query);

    return query.scroll(scrollMode);
  }

  /**
   * Execute the query expecting a single result
   */
  public E uniqueResult() {
    if (projection != null) {
      throw new OBException("Projection must not be set to to use this method");
    }
    List<?> results = list();
    if (results.isEmpty()) {
      return null;
    }
    if (results.size() > 1) {
      throw new RuntimeException("Query returned more than one result");
    }
    return (E) results.get(0);
  }

  /**
   * Execute the query expecting a single result
   */
  public <F> F uniqueResult(Class<F> className) {
    if (projection != null) {
      List<?> results = list(className);
      if (results.isEmpty()) {
        return null;
      }
      if (results.size() > 1) {
        throw new RuntimeException("Query returned more than one result");
      }
      return (F) results.get(0);
    } else {
      throw new OBException("Projection must be set to use this method");
    }
  }

  /**
   * Apply all predicates including filters and custom restrictions
   */
  private void applyPredicates(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<?> root) {
    List<Predicate> predicates = new ArrayList<>();

    final OBContext obContext = OBContext.getOBContext();
    final Entity e = getEntity();

    if (!OBContext.getOBContext().isInAdministratorMode()) {
      OBContext.getOBContext().getEntityAccessChecker().checkReadable(e);
    }

    // Organization filter
    if (isFilterOnReadableOrganization() && e.isOrganizationPartOfKey()) {
      Path<String> orgPath = root.get("id").get(PROPERTY_ORGANIZATION).get("id");
      predicates.add(orgPath.in((Object[]) obContext.getReadableOrganizations()));
    } else if (isFilterOnReadableOrganization() && e.isOrganizationEnabled()) {
      Path<String> orgPath = root.get(PROPERTY_ORGANIZATION).get("id");
      predicates.add(orgPath.in((Object[]) obContext.getReadableOrganizations()));
    }

    // Client filter
    if (isFilterOnReadableClients() && getEntity().isClientEnabled()) {
      Path<String> clientPath = root.get(PROPERTY_CLIENT).get("id");
      predicates.add(clientPath.in((Object[]) obContext.getReadableClients()));
    }

    // Active filter
    if (isFilterOnActive() && e.isActiveEnabled()) {
      predicates.add(cb.equal(root.get(Organization.PROPERTY_ACTIVE), true));
    }

    // Apply custom restrictions
    for (RestrictionEntry entry : restrictions) {
      Predicate predicate = entry.getRestriction().toPredicate(cb, root);
      if (predicate != null) {
        predicates.add(predicate);
      }
    }

    if (!predicates.isEmpty()) {
      cq.where(predicates.toArray(new Predicate[0]));
    }

    if (SessionInfo.getQueryProfile() != null) {
      // QueryTimeOutUtil integration if needed
    }

    initialized = true;
  }

  /**
   * Apply aliases (joins) to the query
   */
  private void applyAliases(Root<?> root) {
    for (AliasEntry aliasEntry : aliases) {
      String path = aliasEntry.getPath();
      jakarta.persistence.criteria.JoinType joinType = aliasEntry.getJoinType();

      // Use INNER join by default if not specified
      if (joinType == null) {
        joinType = jakarta.persistence.criteria.JoinType.INNER;
      }

      String[] parts = path.split("\\.");

      From<?, ?> current = root;
      for (String part : parts) {
        current = createOrGetJoin(current, part, joinType);
      }
    }
  }

  /**
   * Create or get existing join with specified join type
   */
  private Join<?, ?> createOrGetJoin(From<?, ?> from, String attributeName, jakarta.persistence.criteria.JoinType joinType) {
    // Check if join already exists
    for (Join<?, ?> join : from.getJoins()) {
      if (join.getAttribute().getName().equals(attributeName)) {
        return join;
      }
    }
    // Create new join with specified join type
    return from.join(attributeName, joinType);
  }

  /**
   * Apply order by clauses
   */
  private void applyOrderBy(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<E> root) {
    if (orderBys.isEmpty()) {
      return;
    }

    List<Order> orders = new ArrayList<>();
    for (OrderByEntry entry : orderBys) {
      String propertyPath = entry.getProperty();
      Path<?> path = getPath(root, propertyPath);

      if (entry.isAscending()) {
        orders.add(cb.asc(path));
      } else {
        orders.add(cb.desc(path));
      }
    }

    cq.orderBy(orders);
  }

  /**
   * Get path from root navigating through properties
   */
  @SuppressWarnings("rawtypes")
  private Path getPath(Root<E> root, String propertyPath) {
    String[] parts = propertyPath.split("\\.");
    Path path = root;

    for (String part : parts) {
      path = path.get(part);
    }

    return path;
  }

  /**
   * Apply pagination parameters to query
   */
  private void applyPagination(Query<?> query) {
    if (firstResult != null) {
      query.setFirstResult(firstResult);
    }
    if (maxResults != null) {
      query.setMaxResults(maxResults);
    }
  }

  /**
   * Apply timeout to query
   */
  private void applyTimeout(Query<?> query) {
    if (timeout != null) {
      query.setTimeout(timeout);
    }
  }

  /**
   * Convenience method to add an order by clause
   *
   * @param orderOn the property on which to order
   * @param ascending if true then order ascending, false order descending
   * @return this OBCriteria instance, for method chaining
   */
  public OBCriteria<E> addOrderBy(String orderOn, boolean ascending) {
    orderBys.add(new OrderByEntry(orderOn, ascending));
    return this;
  }

  /**
   * Convenience method to add an order by clause
   *
   * @param orderOn the property on which to order
   * @return this OBCriteria instance, for method chaining
   */
  public OBCriteria<E> addOrderBy(String orderOn) {
    orderBys.add(new OrderByEntry(orderOn, true));
    return this;
  }

  /**
   * Add a restriction to constrain the results
   *
   * @param restriction The restriction to be applied
   * @return this OBCriteria instance, for method chaining
   */
  public OBCriteria<E> add(Restriction restriction) {
    restrictions.add(new RestrictionEntry(restriction));
    return this;
  }

  /**
   * Create an alias (join) for an association path
   *
   * @param associationPath The path to the association
   * @param alias The alias to use
   * @return this OBCriteria instance, for method chaining
   */
  public OBCriteria<E> createAlias(String associationPath, String alias) {
    aliases.add(new AliasEntry(associationPath, alias, null));
    return this;
  }

  /**
   * Create an alias (join) for an association path with specified join type
   *
   * @param associationPath The path to the association
   * @param alias The alias to use
   * @param joinType The type of join (INNER, LEFT, RIGHT)
   * @return this OBCriteria instance, for method chaining
   */
  public OBCriteria<E> createAlias(String associationPath, String alias, jakarta.persistence.criteria.JoinType joinType) {
    aliases.add(new AliasEntry(associationPath, alias, joinType));
    return this;
  }

  /**
   * Set the fetch mode for an association path
   * This controls how the association is loaded (EAGER vs LAZY)
   *
   * @param associationPath The path to the association
   * @param fetchMode The fetch mode (JOIN for eager, SELECT for lazy)
   * @return this OBCriteria instance, for method chaining
   */
  public OBCriteria<E> setFetchMode(String associationPath, FetchMode fetchMode) {
    fetchModes.put(associationPath, fetchMode);
    // When fetch mode is JOIN, also create an alias to force the join
    if (fetchMode == FetchMode.JOIN) {
      createAlias(associationPath, associationPath.replace(".", "_"), jakarta.persistence.criteria.JoinType.LEFT);
    }
    return this;
  }

  /**
   * Set a limit upon the number of objects to be retrieved
   *
   * @param maxResults The maximum number of results
   * @return this OBCriteria instance, for method chaining
   */
  public OBCriteria<E> setMaxResults(int maxResults) {
    this.maxResults = maxResults;
    return this;
  }

  /**
   * Set the first result to be retrieved
   *
   * @param firstResult The first result to retrieve, numbered from 0
   * @return this OBCriteria instance, for method chaining
   */
  public OBCriteria<E> setFirstResult(int firstResult) {
    this.firstResult = firstResult;
    return this;
  }

  /**
   * Set query timeout in seconds
   *
   * @param timeout timeout in seconds
   * @return this OBCriteria instance, for method chaining
   */
  public OBCriteria<E> setTimeout(int timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * Create a Hibernate Query from this criteria (for timeout management)
   * This is used by QueryTimeOutUtil
   *
   * @return Query object or null if projection is set
   */
  public Query<E> createQuery() {
    if (projection != null) {
      return null; // Cannot create typed query with projections
    }

    CriteriaBuilder cb = session.getCriteriaBuilder();
    CriteriaQuery<E> cq = cb.createQuery(entityClass);
    Root<E> root = cq.from(entityClass);

    applyPredicates(cb, cq, root);
    applyAliases(root);
    applyOrderBy(cb, cq, root);

    Query<E> query = session.createQuery(cq);
    applyPagination(query);

    return query;
  }

  // Getters and setters

  public Entity getEntity() {
    return entity;
  }

  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  public boolean isFilterOnReadableOrganization() {
    return filterOnReadableOrganization;
  }

  public OBCriteria<E> setFilterOnReadableOrganization(boolean filterOnReadableOrganization) {
    this.filterOnReadableOrganization = filterOnReadableOrganization;
    return this;
  }

  public boolean isFilterOnActive() {
    return filterOnActive;
  }

  public OBCriteria<E> setFilterOnActive(boolean filterOnActive) {
    this.filterOnActive = filterOnActive;
    return this;
  }

  public boolean isFilterOnReadableClients() {
    return filterOnReadableClients;
  }

  public OBCriteria<E> setFilterOnReadableClients(boolean filterOnReadableClients) {
    this.filterOnReadableClients = filterOnReadableClients;
    return this;
  }

  public Projection getProjection() {
    return projection != null ? projection.getProjection() : null;
  }

  public void setProjection(Projection projection) {
    if (this.projection == null) {
      this.projection = new ProjectionEntry(projection);
    } else {
      this.projection.setProjection(projection);
    }
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

  static class OrderByEntry {
    private final String property;
    private final boolean ascending;

    public OrderByEntry(String property, boolean ascending) {
      this.property = property;
      this.ascending = ascending;
    }

    public String getProperty() {
      return property;
    }

    public boolean isAscending() {
      return ascending;
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
    private Projection projection;

    public ProjectionEntry(Projection projection) {
      this.projection = projection;
    }

    @SuppressWarnings("rawtypes")
    public Expression<?> toExpression(CriteriaBuilder cb, Root<?> root) {
      return projection.toExpression(cb, root);
    }

    public Projection getProjection() {
      return projection;
    }

    public void setProjection(Projection projection) {
      this.projection = projection;
    }
  }
}
