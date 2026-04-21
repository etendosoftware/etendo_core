package org.openbravo.dal.service;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
public class SubqueriesTest {

  @Mock
  private CriteriaBuilder cb;
  @Mock
  private Root<Object> root;

  private DetachedCriteria dc;

  @BeforeEach
  public void setUp() {
    dc = DetachedCriteria.forClass(Object.class);
  }

  // --- Factory method tests ---

  @Test
  public void testExistsCreatesExistsRestriction() {
    assertInstanceOf(Subqueries.ExistsRestriction.class, Subqueries.exists(dc));
  }

  @Test
  public void testNotExistsCreatesExistsRestriction() {
    assertInstanceOf(Subqueries.ExistsRestriction.class, Subqueries.notExists(dc));
  }

  @Test
  public void testPropertyInCreatesPropertySubqueryRestriction() {
    assertInstanceOf(Subqueries.PropertySubqueryRestriction.class,
        Subqueries.propertyIn("id", dc));
  }

  @Test
  public void testPropertyNotInCreatesPropertySubqueryRestriction() {
    assertInstanceOf(Subqueries.PropertySubqueryRestriction.class,
        Subqueries.propertyNotIn("id", dc));
  }

  @Test
  public void testPropertyEqCreatesPropertySubqueryRestriction() {
    assertInstanceOf(Subqueries.PropertySubqueryRestriction.class,
        Subqueries.propertyEq("id", dc));
  }

  @Test
  public void testPropertyNeCreatesPropertySubqueryRestriction() {
    assertInstanceOf(Subqueries.PropertySubqueryRestriction.class,
        Subqueries.propertyNe("id", dc));
  }

  @Test
  public void testPropertyGtCreatesPropertySubqueryRestriction() {
    assertInstanceOf(Subqueries.PropertySubqueryRestriction.class,
        Subqueries.propertyGt("id", dc));
  }

  @Test
  public void testPropertyLtCreatesPropertySubqueryRestriction() {
    assertInstanceOf(Subqueries.PropertySubqueryRestriction.class,
        Subqueries.propertyLt("id", dc));
  }

  @Test
  public void testPropertyGeCreatesPropertySubqueryRestriction() {
    assertInstanceOf(Subqueries.PropertySubqueryRestriction.class,
        Subqueries.propertyGe("id", dc));
  }

  @Test
  public void testPropertyLeCreatesPropertySubqueryRestriction() {
    assertInstanceOf(Subqueries.PropertySubqueryRestriction.class,
        Subqueries.propertyLe("id", dc));
  }

  @Test
  public void testInValueCreatesValueSubqueryRestriction() {
    assertInstanceOf(Subqueries.ValueSubqueryRestriction.class,
        Subqueries.in("val", dc));
  }

  @Test
  public void testNotInValueCreatesValueSubqueryRestriction() {
    assertInstanceOf(Subqueries.ValueSubqueryRestriction.class,
        Subqueries.notIn("val", dc));
  }

  @Test
  public void testEqValueCreatesValueSubqueryRestriction() {
    assertInstanceOf(Subqueries.ValueSubqueryRestriction.class,
        Subqueries.eq("val", dc));
  }

  @Test
  public void testNeValueCreatesValueSubqueryRestriction() {
    assertInstanceOf(Subqueries.ValueSubqueryRestriction.class,
        Subqueries.ne("val", dc));
  }

  // --- toPredicate should throw UnsupportedOperationException ---

  @Test
  public void testExistsToPredicateThrows() {
    Restriction r = Subqueries.exists(dc);
    assertThrows(UnsupportedOperationException.class, () -> r.toPredicate(cb, root));
  }

  @Test
  public void testPropertyInToPredicateThrows() {
    Restriction r = Subqueries.propertyIn("id", dc);
    assertThrows(UnsupportedOperationException.class, () -> r.toPredicate(cb, root));
  }

  @Test
  public void testValueInToPredicateThrows() {
    Restriction r = Subqueries.in("val", dc);
    assertThrows(UnsupportedOperationException.class, () -> r.toPredicate(cb, root));
  }

  // --- SubqueryOperator enum coverage ---

  @Test
  public void testSubqueryOperatorValues() {
    Subqueries.SubqueryOperator[] values = Subqueries.SubqueryOperator.values();
    assertNotNull(values);
    org.junit.jupiter.api.Assertions.assertEquals(8, values.length);
  }
}
