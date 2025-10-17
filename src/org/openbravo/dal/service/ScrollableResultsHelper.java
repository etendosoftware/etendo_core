package org.openbravo.dal.service;

import org.hibernate.query.spi.ScrollableResultsImplementor;

/**
 * Utilidades helper para la migración de ScrollableResults de Hibernate 5 a 6.
 */
public class ScrollableResultsHelper {
  
  /**
   * Obtiene el resultado actual del ScrollableResults.
   * En Hibernate 6, get() sin parámetros devuelve el resultado actual.
   */
  public static Object getCurrentResult(ScrollableResultsImplementor<?> scroll) {
    return scroll.get();
  }
  
  /**
   * Obtiene el elemento en el índice específico del resultado actual.
   * En Hibernate 6, get() devuelve Object[] si hay múltiples columnas,
   * o el objeto directo si hay una sola columna.
   */
  public static Object getResultAtIndex(ScrollableResultsImplementor<?> scroll, int index) {
    Object result = scroll.get();
    if (result instanceof Object[]) {
      Object[] array = (Object[]) result;
      return array[index];
    } else {
      // Si solo hay una columna y pedimos índice 0, devolvemos el objeto directo
      if (index == 0) {
        return result;
      } else {
        throw new IllegalArgumentException("Single result, but requested index " + index);
      }
    }
  }
  
  /**
   * Obtiene el resultado como Object[] para compatibilidad.
   */
  public static Object[] getResultAsArray(ScrollableResultsImplementor<?> scroll) {
    Object result = scroll.get();
    if (result instanceof Object[]) {
      return (Object[]) result;
    } else {
      // Si es un solo objeto, lo envolvemos en un array
      return new Object[]{result};
    }
  }
}