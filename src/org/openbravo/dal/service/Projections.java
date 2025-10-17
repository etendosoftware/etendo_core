package org.openbravo.dal.service;

import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;

/**
 * Clase de compatibilidad para reemplazar Hibernate 5 Projections.
 * Proporciona métodos estáticos para crear proyecciones en Hibernate 6.
 */
public class Projections {

  public static Selection<?> id() {
    return new IdProjection();
  }
  
  public static Selection<?> count(String property) {
    return new CountProjection(property);
  }
  
  public static Selection<?> sum(String property) {
    return new SumProjection(property);
  }
  
  public static Selection<?> max(String property) {
    return new MaxProjection(property);
  }
  
  public static Selection<?> min(String property) {
    return new MinProjection(property);
  }
  
  public static Selection<?> property(String property) {
    return new PropertyProjection(property);
  }
  
  public static ProjectionList projectionList() {
    return new ProjectionList();
  }
  
  // Clases internas para representar las proyecciones
  private static class IdProjection implements Selection<String> {
    @Override public String getAlias() { return null; }
    @Override public Selection<String> alias(String alias) { return this; }
    @Override public boolean isCompoundSelection() { return false; }
    @Override public java.util.List<Selection<?>> getCompoundSelectionItems() { return null; }
    @Override public Class<? extends String> getJavaType() { return String.class; }
  }
  
  private static class CountProjection implements Selection<Long> {
    private final String property;
    public CountProjection(String property) { this.property = property; }
    @Override public String getAlias() { return null; }
    @Override public Selection<Long> alias(String alias) { return this; }
    @Override public boolean isCompoundSelection() { return false; }
    @Override public java.util.List<Selection<?>> getCompoundSelectionItems() { return null; }
    @Override public Class<? extends Long> getJavaType() { return Long.class; }
  }
  
  private static class SumProjection implements Selection<Number> {
    private final String property;
    public SumProjection(String property) { this.property = property; }
    @Override public String getAlias() { return null; }
    @Override public Selection<Number> alias(String alias) { return this; }
    @Override public boolean isCompoundSelection() { return false; }
    @Override public java.util.List<Selection<?>> getCompoundSelectionItems() { return null; }
    @Override public Class<? extends Number> getJavaType() { return Number.class; }
  }
  
  private static class MaxProjection implements Selection<Number> {
    private final String property;
    public MaxProjection(String property) { this.property = property; }
    @Override public String getAlias() { return null; }
    @Override public Selection<Number> alias(String alias) { return this; }
    @Override public boolean isCompoundSelection() { return false; }
    @Override public java.util.List<Selection<?>> getCompoundSelectionItems() { return null; }
    @Override public Class<? extends Number> getJavaType() { return Number.class; }
  }
  
  private static class MinProjection implements Selection<Number> {
    private final String property;
    public MinProjection(String property) { this.property = property; }
    @Override public String getAlias() { return null; }
    @Override public Selection<Number> alias(String alias) { return this; }
    @Override public boolean isCompoundSelection() { return false; }
    @Override public java.util.List<Selection<?>> getCompoundSelectionItems() { return null; }
    @Override public Class<? extends Number> getJavaType() { return Number.class; }
  }
  
  private static class PropertyProjection implements Selection<Object> {
    private final String property;
    public PropertyProjection(String property) { this.property = property; }
    @Override public String getAlias() { return null; }
    @Override public Selection<Object> alias(String alias) { return this; }
    @Override public boolean isCompoundSelection() { return false; }
    @Override public java.util.List<Selection<?>> getCompoundSelectionItems() { return null; }
    @Override public Class<? extends Object> getJavaType() { return Object.class; }
  }
}