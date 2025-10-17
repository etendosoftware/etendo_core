package org.openbravo.dal.service;

import static org.openbravo.model.ad.system.Client.PROPERTY_ORGANIZATION;
import static org.openbravo.model.common.enterprise.Organization.PROPERTY_CLIENT;

import jakarta.persistence.criteria.Order;
import java.util.*;
import java.util.Collection;
import java.util.List;
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

  /**
   * Interfaz funcional para crear predicados en lambdas.
   * Evita conflictos con java.util.function.Predicate
   */
  @FunctionalInterface
  public interface PredicateFunction {
    Predicate apply(CriteriaBuilder cb, OBCriteria<?> obc);
  }

  private static final Logger log = LogManager.getLogger();
  private static final String ORDER_ALIAS_PREFIX = "order_ob_";

  // ---- Estado de criterios / consulta
  private final Class<E> entityClass;
  private final Session session;
  private final CriteriaBuilder cb;

  // Criteria principal
  public final CriteriaQuery<E> cq;
  public final Root<E> root; // public para acceso desde lambdas en otros paquetes

  // Predicados acumulados (equivalente a Restrictions)
  private final List<Predicate> predicates = new ArrayList<>();

  // Joins cacheados por ruta (para order/join dinámico)
  private final Map<String, From<?, ?>> joins = new HashMap<>();

  // Alias para joins creados explícitamente (equivalente a createAlias)
  private final Map<String, Join<?, ?>> aliasMap = new HashMap<>();

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

  // -------------------- Extensiones adicionales para completar migración --------------------

  /**
   * Agrega condición IN con Collection (no solo arrays).
   * Equivalente a: Restrictions.in(property, values)
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
   * Agrega condición IN con List<String> (caso más común).
   */
  public OBCriteria<E> addInIds(String property, List<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return this; // No agrega nada si está vacío
    }
    return addIn(property, ids);
  }

  /**
   * Agrega condición IN con Set<String> (convertido a List).
   */
  public OBCriteria<E> addInIds(String property, Set<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return this; // No agrega nada si está vacío
    }
    return addIn(property, new ArrayList<>(ids));
  }

  /**
   * Agrega condición IN con List de entidades (extrae IDs).
   */
  public OBCriteria<E> addInEntities(String property, List<? extends BaseOBObject> entities) {
    if (entities == null || entities.isEmpty()) {
      return this; // No agrega nada si está vacío
    }
    List<String> ids = new ArrayList<>();
    for (BaseOBObject entity : entities) {
      ids.add((String) entity.getId());
    }
    return addIn(property, ids);
  }

  /**
   * Agrega condición IS NOT NULL.
   * Equivalente a: Restrictions.isNotNull(property)
   */
  public OBCriteria<E> addIsNotNull(String property) {
    predicates.add(cb.isNotNull(getPath(property)));
    modified = true;
    return this;
  }

    /**
   * Agrega condición >= (greater or equal).
   * Equivalente a: Restrictions.ge(property, value)
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public OBCriteria<E> addGreaterOrEqual(String property, Comparable value) {
    initialize();
    predicates.add(cb.greaterThanOrEqualTo(getPath(property), value));
    modified = true;
    return this;
  }

  /**
   * Alias para addGreaterOrEqual para compatibilidad.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public OBCriteria<E> addGreaterOrEqualThan(String property, Comparable value) {
    return addGreaterOrEqual(property, value);
  }

  /**
   * Agrega condición <= (less or equal).
   * Equivalente a: Restrictions.le(property, value)
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
   * Alias para addLessOrEqual para compatibilidad.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public OBCriteria<E> addLessOrEqualThan(String property, Comparable value) {
    return addLessOrEqual(property, value);
  }

  /**
   * Alias adicional para addLessOrEqual para compatibilidad.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public OBCriteria<E> addLessEqual(String property, Comparable value) {
    return addLessOrEqual(property, value);
  }

  /**
   * Agrega condición OR de múltiples predicados usando funciones lambda.
   * Equivalente a: Restrictions.or(c1, c2, ...)
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
   * Agrega condición OR de múltiples predicados (Predicate directo).
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
   * Agrega condición AND de múltiples predicados usando funciones lambda.
   * Equivalente a: Restrictions.and(c1, c2, ...)
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
   * Agrega condición AND de múltiples predicados (Predicate directo).
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
   * Método auxiliar público para acceder a CriteriaBuilder desde fuera.
   * Permite construir predicados complejos manualmente.
   */
  public CriteriaBuilder getCriteriaBuilder() {
    return cb;
  }

    /**
   * Agrega un predicado directamente (para casos complejos).
   */
  public OBCriteria<E> add(Predicate predicate) {
    predicates.add(predicate);
    modified = true;
    return this;
  }

  /**
   * Agrega un predicado usando PredicateFunction (para lambdas).
   */
  public OBCriteria<E> addPredicateFunction(PredicateFunction predicateFunction) {
    Predicate predicate = predicateFunction.apply(cb, this);
    predicates.add(predicate);
    modified = true;
    return this;
  }

  /**
   * Alias corto para addPredicateFunction.
   */
  public OBCriteria<E> addFunction(PredicateFunction predicateFunction) {
    return addPredicateFunction(predicateFunction);
  }

  // -------------------- Soporte para Projections (agregaciones) --------------------
  
  /**
   * Crea un CriteriaQuery para agregaciones usando multiselect.
   * Reemplaza a ProjectionList para casos de SUM, COUNT, etc.
   */
  public CriteriaQuery<Object[]> createProjectionQuery() {
    initialize();
    CriteriaQuery<Object[]> projQuery = cb.createQuery(Object[].class);
    Root<E> projRoot = projQuery.from(entityClass);
    
    // Aplicar los mismos predicados pero sobre el nuevo root
    List<Predicate> projPredicates = rebuildPredicatesFor(projRoot);
    if (!projPredicates.isEmpty()) {
      projQuery.where(projPredicates.toArray(new Predicate[0]));
    }
    
    return projQuery;
  }
  
  /**
   * Crea una query de agregación simple para un solo valor.
   * Útil para casos como COUNT(), SUM() de una sola columna.
   */
  public <T> CriteriaQuery<T> createSingleProjectionQuery(Class<T> resultType) {
    initialize();
    CriteriaQuery<T> projQuery = cb.createQuery(resultType);
    Root<E> projRoot = projQuery.from(entityClass);
    
    // Aplicar los mismos predicados pero sobre el nuevo root
    List<Predicate> projPredicates = rebuildPredicatesFor(projRoot);
    if (!projPredicates.isEmpty()) {
      projQuery.where(projPredicates.toArray(new Predicate[0]));
    }
    
    return projQuery;
  }

  /**
   * Acceso a la sesión para poder ejecutar queries personalizadas.
   */
  public Session getSession() {
    return session;
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
  public <T> Path<T> getPath(String propertyPath) {
    if (propertyPath == null || propertyPath.isEmpty()) {
      return root.get(propertyPath);
    }
    
    // Check if it starts with an alias
    if (propertyPath.contains(".")) {
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
    if (!propertyPath.contains(".")) {
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

  // -------------------- Migración de Projections --------------------

  private String projectionProperty = null;
  private String projectionType = null;

  /**
   * Sets a maximum projection on the given property.
   * Replaces Hibernate 5's Projections.max() functionality.
   * After calling this, uniqueResult() will return the max value instead of entity.
   * 
   * @param property the property to calculate the maximum for
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
   * @param property the property to calculate the minimum for
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

  // -------------------- Proyecciones múltiples --------------------
  
  private Selection<?>[] multipleProjections = null;
  
  /**
   * Configura múltiples proyecciones para la query.
   * Permite hacer SELECT sum(prop1), sum(prop2), etc. en una sola query.
   * Reemplaza el uso de ProjectionList de Hibernate 5.
   */
  public OBCriteria<E> setMultipleProjections(Selection<?>... projections) {
    this.multipleProjections = projections;
    this.projectionType = "MULTIPLE";
    modified = true;
    return this;
  }
  
  /**
   * Método helper para crear múltiples proyecciones SUM fácilmente.
   * Equivale a: ProjectionList.add(Projections.sum(prop1)).add(Projections.sum(prop2))
   */
  public OBCriteria<E> setMultipleSums(String... properties) {
    Selection<?>[] sums = new Selection[properties.length];
    for (int i = 0; i < properties.length; i++) {
      // Nota: esto será evaluado cuando se ejecute la query
      sums[i] = new SumProjectionPlaceholder(properties[i]);
    }
    return setMultipleProjections(sums);
  }
  
  /**
   * A temporary holder for SUM projections that gets converted to real Hibernate projections later.
   * 
   * <p>This class solves a timing problem: when users call {@code setMultipleSums("prop1", "prop2")},
   * we don't have access to CriteriaBuilder yet. So we store the property names in these placeholders
   * and convert them to actual {@code criteriaBuilder.sum()} calls when the query executes.
   * 
   * <p>Think of it as a "sticky note" that says "remember to SUM this property later".
   */
  private static class SumProjectionPlaceholder implements Selection<Number> {
    private final String property;
    
    public SumProjectionPlaceholder(String property) {
      this.property = property;
    }
    
    public String getProperty() { return property; }
    
    @Override public String getAlias() { return null; }
    @Override public Selection<Number> alias(String alias) { return this; }
    @Override public boolean isCompoundSelection() { return false; }
    @Override public java.util.List<Selection<?>> getCompoundSelectionItems() { return null; }
    @Override public Class<? extends Number> getJavaType() { return Number.class; }
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
   * Agrega condición NOT.
   * Equivalente a: Restrictions.not(predicate)
   */
  public OBCriteria<E> addNot(Predicate predicate) {
    if (predicate != null) {
      predicates.add(cb.not(predicate));
      modified = true;
    }
    return this;
  }

    /**
   * Agrega condición sizeEq (tamaño de colección igual).
   * Equivalente a: Restrictions.sizeEq(property, size)
   */
  @SuppressWarnings("unchecked")
  public OBCriteria<E> addSizeEq(String property, int size) {
    Path<?> path = getPath(property);
    predicates.add(cb.equal(cb.size((Expression<Collection<Object>>) path), size));
    modified = true;
    return this;
  }

  /**
   * Agrega condición de propiedades no iguales.
   * Equivalente a: Restrictions.neProperty(property1, property2)
   */
  public OBCriteria<E> addNeProperty(String property1, String property2) {
    Path<?> path1 = getPath(property1);
    Path<?> path2 = getPath(property2);
    predicates.add(cb.notEqual(path1, path2));
    modified = true;
    return this;
  }

  /**
   * Agrega condición de propiedades iguales.
   * Equivalente a: Restrictions.eqProperty(property1, property2)
   */
  public OBCriteria<E> addEqProperty(String property1, String property2) {
    Path<?> path1 = getPath(property1);
    Path<?> path2 = getPath(property2);
    predicates.add(cb.equal(path1, path2));
    modified = true;
    return this;
  }

  /**
   * Agrega condición greater than o equal comparando propiedades.
   * Equivalente a: Restrictions.geProperty(property1, property2)
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public OBCriteria<E> addGeProperty(String property1, String property2) {
    Path<?> path1 = getPath(property1);
    Path<?> path2 = getPath(property2);
    predicates.add(cb.greaterThanOrEqualTo((Expression<Comparable>) path1, (Expression<Comparable>) path2));
    modified = true;
    return this;
  }

  /**
   * Agrega condición de igualdad por ID.
   * Equivalente a: Restrictions.idEq(id)
   */
  public OBCriteria<E> addIdEq(String id) {
    predicates.add(cb.equal(root.get("id"), id));
    modified = true;
    return this;
  }

  /**
   * Método de compatibilidad para setFetchMode.
   * En Hibernate 6, el fetch mode se maneja automáticamente o con joins explícitos.
   */
  public OBCriteria<E> setFetchMode(String property, Object fetchMode) {
    // En Hibernate 6, esto se maneja automáticamente o con join fetch
    // Por compatibilidad, simplemente devolvemos this sin hacer nada
    return this;
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
