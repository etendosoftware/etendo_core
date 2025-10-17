package org.openbravo.dal.service;

import jakarta.persistence.criteria.Subquery;

/**
 * Clase de compatibilidad para reemplazar Hibernate 5 Subqueries.
 * Proporciona métodos estáticos para trabajar con subqueries.
 */
public class Subqueries {
  
  public static OBCriteria.PredicateFunction exists(DetachedCriteria detachedCriteria) {
    return (cb, obc) -> {
      // Crear subquery EXISTS
      Subquery<Long> subquery = obc.cq.subquery(Long.class);
      // En una implementación completa, configuraríamos el subquery
      // basándose en el DetachedCriteria
      return cb.exists(subquery);
    };
  }
  
  public static OBCriteria.PredicateFunction notExists(DetachedCriteria detachedCriteria) {
    return (cb, obc) -> {
      // Crear subquery NOT EXISTS  
      Subquery<Long> subquery = obc.cq.subquery(Long.class);
      // En una implementación completa, configuraríamos el subquery
      // basándose en el DetachedCriteria
      return cb.not(cb.exists(subquery));
    };
  }
  
  public static <T> OBCriteria.PredicateFunction in(String property, DetachedCriteria detachedCriteria) {
    return (cb, obc) -> {
      // Crear subquery IN
      Subquery<T> subquery = obc.cq.subquery((Class<T>) Object.class);
      // En una implementación completa, configuraríamos el subquery
      // basándose en el DetachedCriteria
      return obc.getPath(property).in(subquery);
    };
  }
  
  public static <T> OBCriteria.PredicateFunction notIn(String property, DetachedCriteria detachedCriteria) {
    return (cb, obc) -> {
      // Crear subquery NOT IN
      Subquery<T> subquery = obc.cq.subquery((Class<T>) Object.class);
      // En una implementación completa, configuraríamos el subquery
      // basándose en el DetachedCriteria
      return cb.not(obc.getPath(property).in(subquery));
    };
  }
}