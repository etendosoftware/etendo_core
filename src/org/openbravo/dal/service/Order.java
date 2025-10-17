package org.openbravo.dal.service;

/**
 * Clase de compatibilidad para reemplazar Hibernate 5 Order.
 * Proporciona métodos estáticos para crear órdenes en Hibernate 6.
 */
public class Order {
  
  private final String property;
  private final boolean ascending;
  
  private Order(String property, boolean ascending) {
    this.property = property;
    this.ascending = ascending;
  }
  
  public static Order asc(String property) {
    return new Order(property, true);
  }
  
  public static Order desc(String property) {
    return new Order(property, false);
  }
  
  public String getProperty() {
    return property;
  }
  
  public boolean isAscending() {
    return ascending;
  }
}