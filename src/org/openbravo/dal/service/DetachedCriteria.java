package org.openbravo.dal.service;

import jakarta.persistence.criteria.*;
import org.hibernate.Session;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.SessionHandler;

/**
 * Clase de compatibilidad para reemplazar Hibernate 5 DetachedCriteria.
 * En Hibernate 6, usamos Subquery directamente.
 */
public class DetachedCriteria {
  
  private final String entityName;
  private final String alias;
  private Subquery<?> subquery;
  
  private DetachedCriteria(String entityName, String alias) {
    this.entityName = entityName;
    this.alias = alias;
  }
  
  public static DetachedCriteria forEntityName(String entityName) {
    return new DetachedCriteria(entityName, null);
  }
  
  public static DetachedCriteria forEntityName(String entityName, String alias) {
    return new DetachedCriteria(entityName, alias);
  }
  
  public DetachedCriteria add(OBCriteria.PredicateFunction restriction) {
    // En una implementación real, acumularíamos las restricciones
    // Para este caso, simplemente devolvemos this para compatibilidad
    return this;
  }
  
  public DetachedCriteria setProjection(Selection<?> projection) {
    // En una implementación real, estableceríamos la proyección
    // Para este caso, simplemente devolvemos this para compatibilidad
    return this;
  }
  
  /**
   * Convierte a Subquery para uso en queries principales.
   * Esto debe ser llamado dentro del contexto de una CriteriaQuery.
   */
  public <T> Subquery<T> toSubquery(CriteriaQuery<?> parentQuery, Class<T> resultType) {
    return parentQuery.subquery(resultType);
  }
  
  // Getters para compatibilidad
  public String getEntityName() {
    return entityName;
  }
  
  public String getAlias() {
    return alias;
  }
}