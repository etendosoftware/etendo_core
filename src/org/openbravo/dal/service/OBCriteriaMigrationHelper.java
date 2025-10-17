package org.openbravo.dal.service;

import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;

import org.openbravo.base.structure.BaseOBObject;

/**
 * Helper class para facilitar la migración de código complejo de Hibernate 5 a 6.
 * Proporciona métodos estáticos para casos comunes de migración.
 *
 * @author Migration Script - Hibernate 6
 */
public class OBCriteriaMigrationHelper {

  /**
   * Crea rápidamente una query con múltiples agregaciones (reemplazo de ProjectionList).
   * <p>
   * EJEMPLO DE USO - MIGRACIÓN DE:
   * <p>
   * final ProjectionList projections = Projections.projectionList();
   * projections.add(Projections.sum("debit"));
   * projections.add(Projections.sum("credit"));
   * initialBalanceQuery.setProjection(projections);
   * List<Object[]> results = initialBalanceQuery.list();
   * <p>
   * A:
   * <p>
   * List<Object[]> results = OBCriteriaMigrationHelper.executeMultipleAggregations(
   * AccountingFact.class,
   * obc -> {
   * // aplicar los mismos filtros que antes
   * obc.addEqual("property", value);
   * },
   * (cb, root) -> new Selection[]{
   * cb.sum(root.get("debit")),
   * cb.sum(root.get("credit"))
   * }
   * );
   */
  public static <E extends BaseOBObject> List<Object[]> executeMultipleAggregations(
      Class<E> entityClass,
      FilterSetup<E> filterSetup,
      ProjectionSetup<E> projectionSetup) {

    OBCriteria<E> obc = new OBCriteria<>(entityClass);

    // Aplicar filtros
    if (filterSetup != null) {
      filterSetup.apply(obc);
    }

    // Crear query de proyección
    CriteriaQuery<Object[]> projQuery = obc.createProjectionQuery();
    Root<E> root = (Root<E>) projQuery.getRoots().iterator().next();

    // Aplicar proyecciones
    Selection<?>[] selections = projectionSetup.apply(obc.getCriteriaBuilder(), root);
    projQuery.multiselect(selections);

    return obc.getSession().createQuery(projQuery).getResultList();
  }

  /**
   * Crea rápidamente una query con una sola agregación.
   */
  public static <E extends BaseOBObject, R> R executeSingleAggregation(
      Class<E> entityClass,
      Class<R> resultType,
      FilterSetup<E> filterSetup,
      SingleProjectionSetup<E, R> projectionSetup) {

    OBCriteria<E> obc = new OBCriteria<>(entityClass);

    // Aplicar filtros
    if (filterSetup != null) {
      filterSetup.apply(obc);
    }

    // Crear query de proyección
    CriteriaQuery<R> projQuery = obc.createSingleProjectionQuery(resultType);
    Root<E> root = (Root<E>) projQuery.getRoots().iterator().next();

    // Aplicar proyección
    Expression<R> expression = projectionSetup.apply(obc.getCriteriaBuilder(), root);
    projQuery.select(expression);

    return obc.getSession().createQuery(projQuery).getSingleResult();
  }

  /**
   * Helper para migrar Restrictions.and() complejos.
   */
  public static <E extends BaseOBObject> void addComplexAnd(
      OBCriteria<E> criteria,
      ComplexPredicateSetup<E> predicateSetup) {

    CriteriaBuilder cb = criteria.getCriteriaBuilder();
    OBCriteria.PredicateFunction[] predicateFunctions = predicateSetup.apply(cb, criteria);
    criteria.addAnd(predicateFunctions);
  }

  /**
   * Helper para migrar Restrictions.or() complejos.
   */
  public static <E extends BaseOBObject> void addComplexOr(
      OBCriteria<E> criteria,
      ComplexPredicateSetup<E> predicateSetup) {

    CriteriaBuilder cb = criteria.getCriteriaBuilder();
    OBCriteria.PredicateFunction[] predicateFunctions = predicateSetup.apply(cb, criteria);
    criteria.addOr(predicateFunctions);
  }

  // ==================== INTERFACES FUNCIONALES ====================

  @FunctionalInterface
  public interface FilterSetup<E extends BaseOBObject> {
    void apply(OBCriteria<E> criteria);
  }

  @FunctionalInterface
  public interface ProjectionSetup<E extends BaseOBObject> {
    Selection<?>[] apply(CriteriaBuilder cb, Root<E> root);
  }

  @FunctionalInterface
  public interface SingleProjectionSetup<E extends BaseOBObject, R> {
    Expression<R> apply(CriteriaBuilder cb, Root<E> root);
  }

  @FunctionalInterface
  public interface ComplexPredicateSetup<E extends BaseOBObject> {
    OBCriteria.PredicateFunction[] apply(CriteriaBuilder cb, OBCriteria<E> criteria);
  }
}