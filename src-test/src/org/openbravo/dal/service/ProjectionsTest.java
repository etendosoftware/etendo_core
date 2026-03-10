package org.openbravo.dal.service;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProjectionsTest {

  @Mock
  private CriteriaBuilder cb;
  @Mock
  private Root<Object> root;
  @Mock
  private Path<Object> path;
  @Mock
  private Expression<Long> longExpr;

  @SuppressWarnings("unchecked")
  private void mockPath(String propertyName) {
    when(root.get(propertyName)).thenReturn(path);
  }

  // --- Factory method tests ---

  @Test
  public void testRowCountCreatesRowCountProjection() {
    assertInstanceOf(Projections.RowCountProjection.class, Projections.rowCount());
  }

  @Test
  public void testCountCreatesCountProjection() {
    assertInstanceOf(Projections.CountProjection.class, Projections.count("id"));
  }

  @Test
  public void testCountDistinctCreatesCountProjection() {
    assertInstanceOf(Projections.CountProjection.class, Projections.countDistinct("id"));
  }

  @Test
  public void testMaxCreatesAggregateProjection() {
    assertInstanceOf(Projections.AggregateProjection.class, Projections.max("price"));
  }

  @Test
  public void testMinCreatesAggregateProjection() {
    assertInstanceOf(Projections.AggregateProjection.class, Projections.min("price"));
  }

  @Test
  public void testSumCreatesAggregateProjection() {
    assertInstanceOf(Projections.AggregateProjection.class, Projections.sum("amount"));
  }

  @Test
  public void testAvgCreatesAggregateProjection() {
    assertInstanceOf(Projections.AggregateProjection.class, Projections.avg("score"));
  }

  @Test
  public void testPropertyCreatesPropertyProjection() {
    assertInstanceOf(Projections.PropertyProjection.class, Projections.property("name"));
  }

  @Test
  public void testDistinctCreatesDistinctProjection() {
    Projection inner = Projections.property("name");
    assertInstanceOf(Projections.DistinctProjection.class, Projections.distinct(inner));
  }

  @Test
  public void testGroupPropertyCreatesPropertyProjection() {
    assertInstanceOf(Projections.PropertyProjection.class, Projections.groupProperty("org"));
  }

  @Test
  public void testIdCreatesIdProjection() {
    assertInstanceOf(Projections.IdProjection.class, Projections.id());
  }

  @Test
  public void testProjectionListCreatesProjectionList() {
    assertNotNull(Projections.projectionList());
    assertInstanceOf(ProjectionList.class, Projections.projectionList());
  }

  // --- toExpression tests ---

  @SuppressWarnings("unchecked")
  @Test
  public void testRowCountToExpressionCallsCbCount() {
    when(cb.count(any(Root.class))).thenReturn(longExpr);

    Expression<?> result = Projections.rowCount().toExpression(cb, root);

    assertNotNull(result);
    verify(cb).count(root);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCountToExpressionCallsCbCount() {
    mockPath("id");
    when(cb.count(any())).thenReturn(longExpr);

    Projections.count("id").toExpression(cb, root);

    verify(cb).count(path);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCountDistinctToExpressionCallsCbCountDistinct() {
    mockPath("id");
    when(cb.countDistinct(any())).thenReturn(longExpr);

    Projections.countDistinct("id").toExpression(cb, root);

    verify(cb).countDistinct(path);
  }


  @SuppressWarnings("unchecked")
  @Test
  public void testPropertyToExpressionReturnsPath() {
    mockPath("name");

    Expression<?> result = Projections.property("name").toExpression(cb, root);

    assertNotNull(result);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testIdToExpressionGetsIdPath() {
    when(root.get("id")).thenReturn(path);

    Expression<?> result = Projections.id().toExpression(cb, root);

    assertNotNull(result);
    verify(root).get("id");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testDistinctDelegatesToInnerProjection() {
    mockPath("name");
    Projection inner = Projections.property("name");
    Projection distinct = Projections.distinct(inner);

    Expression<?> result = distinct.toExpression(cb, root);

    assertNotNull(result);
  }

  // --- ProjectionList tests ---

  @Test
  public void testProjectionListAddAndGetLength() {
    ProjectionList list = Projections.projectionList();
    list.add(Projections.property("a"));
    list.add(Projections.property("b"));
    list.add(Projections.max("c"));

    assertNotNull(list.getProjections());
    org.junit.jupiter.api.Assertions.assertEquals(3, list.getLength());
    org.junit.jupiter.api.Assertions.assertEquals(3, list.getProjections().size());
  }

  @Test
  public void testProjectionListToExpressionThrows() {
    ProjectionList list = Projections.projectionList();
    assertThrows(UnsupportedOperationException.class, () -> list.toExpression(cb, root));
  }

  @Test
  public void testProjectionListAddReturnsThis() {
    ProjectionList list = Projections.projectionList();
    ProjectionList returned = list.add(Projections.property("a"));
    org.junit.jupiter.api.Assertions.assertSame(list, returned);
  }
}
