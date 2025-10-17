package org.openbravo.dal.service;

import jakarta.persistence.criteria.Predicate;
import java.util.Collection;

/**
 * Clase de compatibilidad para reemplazar Hibernate 5 Restrictions.
 * Proporciona métodos estáticos que devuelven PredicateFunction compatibles con OBCriteria.
 */
public class Restrictions {
  
  public static OBCriteria.PredicateFunction eq(String property, Object value) {
    return (cb, obc) -> cb.equal(obc.getPath(property), value);
  }
  
  public static OBCriteria.PredicateFunction ne(String property, Object value) {
    return (cb, obc) -> cb.notEqual(obc.getPath(property), value);
  }
  
  public static OBCriteria.PredicateFunction isNull(String property) {
    return (cb, obc) -> cb.isNull(obc.getPath(property));
  }
  
  public static OBCriteria.PredicateFunction isNotNull(String property) {
    return (cb, obc) -> cb.isNotNull(obc.getPath(property));
  }
  
  public static OBCriteria.PredicateFunction like(String property, String pattern) {
    return (cb, obc) -> cb.like(obc.getPath(property), pattern);
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static OBCriteria.PredicateFunction gt(String property, Comparable value) {
    return (cb, obc) -> cb.greaterThan(obc.getPath(property), value);
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static OBCriteria.PredicateFunction ge(String property, Comparable value) {
    return (cb, obc) -> cb.greaterThanOrEqualTo(obc.getPath(property), value);
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static OBCriteria.PredicateFunction lt(String property, Comparable value) {
    return (cb, obc) -> cb.lessThan(obc.getPath(property), value);
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static OBCriteria.PredicateFunction le(String property, Comparable value) {
    return (cb, obc) -> cb.lessThanOrEqualTo(obc.getPath(property), value);
  }
  
  public static OBCriteria.PredicateFunction in(String property, Collection<?> values) {
    return (cb, obc) -> obc.getPath(property).in(values);
  }
  
  public static OBCriteria.PredicateFunction in(String property, Object[] values) {
    return (cb, obc) -> obc.getPath(property).in(values);
  }
  
  public static OBCriteria.PredicateFunction eqProperty(String property1, String property2) {
    return (cb, obc) -> cb.equal(obc.getPath(property1), obc.getPath(property2));
  }
  
  public static OBCriteria.PredicateFunction neProperty(String property1, String property2) {
    return (cb, obc) -> cb.notEqual(obc.getPath(property1), obc.getPath(property2));
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static OBCriteria.PredicateFunction geProperty(String property1, String property2) {
    return (cb, obc) -> cb.greaterThanOrEqualTo(
        obc.getPath(property1).as(Comparable.class), 
        obc.getPath(property2).as(Comparable.class)
    );
  }
  
  public static OBCriteria.PredicateFunction idEq(String id) {
    return (cb, obc) -> cb.equal(obc.root.get("id"), id);
  }
  
  public static OBCriteria.PredicateFunction and(OBCriteria.PredicateFunction... predicates) {
    return (cb, obc) -> {
      Predicate[] preds = new Predicate[predicates.length];
      for (int i = 0; i < predicates.length; i++) {
        preds[i] = predicates[i].apply(cb, obc);
      }
      return cb.and(preds);
    };
  }
  
  public static OBCriteria.PredicateFunction or(OBCriteria.PredicateFunction... predicates) {
    return (cb, obc) -> {
      Predicate[] preds = new Predicate[predicates.length];
      for (int i = 0; i < predicates.length; i++) {
        preds[i] = predicates[i].apply(cb, obc);
      }
      return cb.or(preds);
    };
  }
  
  public static OBCriteria.PredicateFunction not(OBCriteria.PredicateFunction predicate) {
    return (cb, obc) -> cb.not(predicate.apply(cb, obc));
  }
}