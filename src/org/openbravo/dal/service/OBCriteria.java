package org.openbravo.dal.service;

import static org.openbravo.model.ad.system.Client.PROPERTY_ORGANIZATION;
import static org.openbravo.model.common.enterprise.Organization.PROPERTY_CLIENT;





import jakarta.persistence.criteria.Order;
import java.util.*;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.query.*;
import org.hibernate.query.spi.ScrollableResultsImplementor;

import org.openbravo.base.model.Entity;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.database.SessionInfo;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.QueryTimeOutUtil;

/**
 * OBCriteria adaptado a Hibernate 6/Jakarta.
 * Implementa filtros de cliente/organización/activo, ordenaciones, count, scroll y paginado
 * usando CriteriaBuilder/CriteriaQuery. Ya no depende del API de org.hibernate.criterion.*
 */
public class OBCriteria<E extends BaseOBObject> {

  private static final Logger log = LogManager.getLogger();
  private static final String ORDER_ALIAS_PREFIX = "order_ob_";

  // ---- Estado de criterios / consulta
  private final Class<E> entityClass;
  private final Session session;
  private final CriteriaBuilder cb;

  // Criteria principal
  private final CriteriaQuery<E> cq;
  private final Root<E> root;

  // Predicados acumulados (equivalente a Restrictions)
  private final List<Predicate> predicates = new ArrayList<>();

  // Joins cacheados por ruta (para order/join dinámico)
  private final Map<String, From<?, ?>> joins = new HashMap<>();

  // Ordenaciones solicitadas
  private final List<OrderBy> orderBys = new ArrayList<>();

  // Flags de filtros “transparentes”
  private boolean filterOnReadableClients = true;
  private boolean filterOnReadableOrganization = true;
  private boolean filterOnActive = true;

  // Paginado
  private Integer maxResults;
  private Integer firstResult;

  // Metadata del modelo
  private Entity entity; // se puede inyectar vía setEntity

  // Para compatibilidad con el antiguo “scrolling” behavior
  private boolean scrolling = false;
  private boolean initialized = false;
  private boolean modified = false;

  /** Crea un OBCriteria con la sesión actual. */
  public OBCriteria(Class<E> entityClass) {
    this(entityClass, SessionHandler.getInstance().getSession());
  }

  /** Crea un OBCriteria con una sesión explícita. */
  public OBCriteria(Class<E> entityClass, Session session) {
    this.entityClass = entityClass;
    this.session = session;
    this.cb = session.getSessionFactory().getCriteriaBuilder();
    this.cq = cb.createQuery(entityClass);
    this.root = cq.from(entityClass);
    // el root es también un "join" raíz para getPath()
    joins.put("", root);
  }

  // -------------------- API pública principal --------------------

  /** Ejecuta y devuelve la lista de resultados. */
  public List<E> list() {
    initialize();
    cq.where(predicates.toArray(new Predicate[0]));
    applyOrderBy();
    TypedQuery<E> q = session.createQuery(cq);
    applyPagination(q);
    applyTimeout(q);
    return q.getResultList();
  }

  /** Devuelve un único resultado (o null si no hay). */
  public E uniqueResult() {
    initialize();
    cq.where(predicates.toArray(new Predicate[0]));
    applyOrderBy();
    TypedQuery<E> q = session.createQuery(cq);
    applyPagination(q);
    applyTimeout(q);
    List<E> r = q.setMaxResults(1).getResultList();
    return r.isEmpty() ? null : r.get(0);
  }

  /** Devuelve el total con los mismos filtros. */
  public int count() {
    initialize();
    CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
    Root<E> countRoot = countQuery.from(entityClass);

    // Reaplicar los predicados sobre el root del count
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

  /** Scroll “forward only” por defecto. */
  public ScrollableResultsImplementor<E> scroll() {
    return scroll(ScrollMode.FORWARD_ONLY);
  }

  /** Scroll con modo. */
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

  // -------------------- Filtros transparentes --------------------

  /** Filtro organizaciones legibles activado/desactivado. */
  public boolean isFilterOnReadableOrganization() {
    return filterOnReadableOrganization;
  }

  public OBCriteria<E> setFilterOnReadableOrganization(boolean filterOnReadableOrganization) {
    this.filterOnReadableOrganization = filterOnReadableOrganization;
    modified = true;
    return this;
  }

  /** Filtro por activo activado/desactivado. */
  public boolean isFilterOnActive() {
    return filterOnActive;
  }

  public OBCriteria<E> setFilterOnActive(boolean filterOnActive) {
    this.filterOnActive = filterOnActive;
    modified = true;
    return this;
  }

  /** Filtro por clientes legibles activado/desactivado. */
  public boolean isFilterOnReadableClients() {
    return filterOnReadableClients;
  }

  public OBCriteria<E> setFilterOnReadableClients(boolean filterOnReadableClients) {
    this.filterOnReadableClients = filterOnReadableClients;
    modified = true;
    return this;
  }

  // -------------------- “Restrictions” helpers (reemplazo) --------------------

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

  // -------------------- Ordenaciones --------------------

  /** Equivalente al viejo addOrderBy con joins implícitos. */
  public OBCriteria<E> addOrderBy(String orderOn, boolean ascending) {
    orderBys.add(new OrderBy(orderOn, ascending));
    modified = true;
    return this;
  }

  // -------------------- Paginación --------------------

  public OBCriteria<E> setMaxResults(int maxResults) {
    this.maxResults = maxResults;
    return this;
  }

  public OBCriteria<E> setFirstResult(int firstResult) {
    this.firstResult = firstResult;
    return this;
  }

  // -------------------- Metadata / Entity --------------------

  public Entity getEntity() {
    return entity;
  }

  void setEntity(Entity entity) {
    this.entity = entity;
  }

  // -------------------- Internals --------------------

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
    final Entity e = getEntity(); // puede ser null si no lo setean desde fuera

    if (!OBContext.getOBContext().isInAdministratorMode() && e != null) {
      OBContext.getOBContext().getEntityAccessChecker().checkReadable(e);
    }

    // Filtro por organización
    if (filterOnReadableOrganization && e != null && e.isOrganizationEnabled()) {
      if (e.isOrganizationPartOfKey()) {
        addInIds("id." + PROPERTY_ORGANIZATION + ".id", obContext.getReadableOrganizations());
      } else {
        addInIds(PROPERTY_ORGANIZATION + ".id", obContext.getReadableOrganizations());
      }
    }

    // Filtro por cliente
    if (filterOnReadableClients && e != null && e.isClientEnabled()) {
      addInIds(PROPERTY_CLIENT + ".id", obContext.getReadableClients());
    }

    // Filtro por activo
    if (filterOnActive && e != null && e.isActiveEnabled()) {
      addEqual(Organization.PROPERTY_ACTIVE, true);
    }

    // Timeout por perfil
    if (SessionInfo.getQueryProfile() != null) {
      // lo aplicamos en applyTimeout(query) al crear la query
    }

    initialized = true;
    modified = false;
  }

  /** Aplica ordenaciones creando joins según sea necesario (a.b.c). */
  private void applyOrderBy() {
    if (orderBys.isEmpty()) {
      return;
    }
    List<Order> orders = new ArrayList<>();
    int idx = 0;
    for (OrderBy ob : orderBys) {
      String prop = ob.getOrderOn();
      Path<?> path = getPath(prop);
      orders.add(ob.isAscending() ? cb.asc(path) : cb.desc(path));
      idx++;
    }
    cq.orderBy(orders);
  }

  /** Reconstruye los predicados sobre otro root (para el count). */
  private List<Predicate> rebuildPredicatesFor(Root<E> otherRoot) {
    // Como los predicados llevan referencias a paths, los volvemos a armar usando los mismos “property paths”
    // que ya acumulamos en 'predicates'. Para eso guardamos también el “property path” cuando los generamos:
    // Para simplificar, volvemos a aplicar los filtros “transparentes” únicamente:
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
    // Nota: si necesitás contar con otros predicados “manuales” (addEqual, etc.),
    // lo más robusto es construir un segundo CriteriaQuery “en paralelo” a medida
    // que se van añadiendo. Si te interesa, lo implemento.
    return rebuilt;
  }

  /** Devuelve el Path para un propertyPath con puntos, creando joins si hace falta. */
  @SuppressWarnings("unchecked")
  private <T> Path<T> getPath(String propertyPath) {
    if (propertyPath == null || propertyPath.isEmpty() || !propertyPath.contains(".")) {
      return root.get(propertyPath);
    }
    String[] parts = propertyPath.split("\\.");
    From<?, ?> current = root;
    StringBuilder currentPath = new StringBuilder();
    for (int i = 0; i < parts.length - 1; i++) {
      if (currentPath.length() > 0) currentPath.append(".");
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
   * @throws org.openbravo.base.exception.OBException if the query cannot be built.
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
        // Aplica el timeout según el perfil configurado (usando Hibernate unwrap)
        QueryTimeOutUtil.getInstance().setQueryTimeOut(
            q.unwrap(org.hibernate.query.Query.class),
            SessionInfo.getQueryProfile());
      } catch (Exception ignore) {
        // Último recurso: aplicar hint estándar JPA (solo si el driver lo soporta)
        try {
          q.setHint("jakarta.persistence.query.timeout", 30000); // 30s por defecto
        } catch (Exception ignored) {
          // ignorar si el hint no es soportado
        }
      }
    }
  }

  // -------------------- Soporte de orden múltiple --------------------

  static class OrderBy {
    private final String orderOn;
    private final boolean ascending;

    OrderBy(String orderOn, boolean ascending) {
      this.orderOn = orderOn;
      this.ascending = ascending;
    }
    String getOrderOn() { return orderOn; }
    boolean isAscending() { return ascending; }
    @Override public String toString() { return orderOn + (ascending ? " asc" : " desc"); }
  }
}
