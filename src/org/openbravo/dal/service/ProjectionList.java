package org.openbravo.dal.service;

import jakarta.persistence.criteria.Selection;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase de compatibilidad para reemplazar Hibernate 5 ProjectionList.
 * Acumula múltiples proyecciones para usar con multiselect en Hibernate 6.
 */
public class ProjectionList {
  
  private final List<Selection<?>> projections = new ArrayList<>();
  
  public ProjectionList add(Selection<?> projection) {
    projections.add(projection);
    return this;
  }
  
  public Selection<?>[] toArray() {
    return projections.toArray(new Selection[0]);
  }
  
  public List<Selection<?>> getProjections() {
    return new ArrayList<>(projections);
  }
  
  public int size() {
    return projections.size();
  }
  
  public boolean isEmpty() {
    return projections.isEmpty();
  }
}