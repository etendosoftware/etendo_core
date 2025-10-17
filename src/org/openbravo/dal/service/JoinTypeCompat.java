package org.openbravo.dal.service;

import jakarta.persistence.criteria.JoinType;

/**
 * Clase de compatibilidad para JoinType de Hibernate 5.
 * Proporciona constantes con los nombres que se usaban en Hibernate 5.
 */
public class JoinTypeCompat {
  
  public static final JoinType LEFT_OUTER_JOIN = JoinType.LEFT;
  public static final JoinType INNER_JOIN = JoinType.INNER;
  public static final JoinType RIGHT_OUTER_JOIN = JoinType.RIGHT;
}