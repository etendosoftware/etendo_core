/*
 * Unit tests for AdvancedQueryBuilder.
 */
package org.openbravo.service.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Tests for {@link AdvancedQueryBuilder}.
 */
@SuppressWarnings({"java:S1448", "java:S112"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class AdvancedQueryBuilderTest {

  private static final String EQUALS = "equals";
  private static final String GREATER_THAN = "greaterThan";
  private static final String LESS_THAN = "lessThan";
  private static final String GREATER_OR_EQUAL = "greaterOrEqual";
  private static final String LESS_OR_EQUAL = "lessOrEqual";
  private static final String CONTAINS = "contains";
  private static final String ORG_IN_0 = "org in ('0')";

  private AdvancedQueryBuilder instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(AdvancedQueryBuilder.class);
  }

  // --- getHqlOperator tests (public static method) ---
  /** Get hql operator equals. */

  @Test
  public void testGetHqlOperatorEquals() {
    assertEquals("=", AdvancedQueryBuilder.getHqlOperator(EQUALS));
  }
  /** Get hql operator not equal. */

  @Test
  public void testGetHqlOperatorNotEqual() {
    assertEquals("!=", AdvancedQueryBuilder.getHqlOperator("notEqual"));
  }
  /** Get hql operator i equals. */

  @Test
  public void testGetHqlOperatorIEquals() {
    assertEquals("=", AdvancedQueryBuilder.getHqlOperator("iEquals"));
  }
  /** Get hql operator i not equal. */

  @Test
  public void testGetHqlOperatorINotEqual() {
    assertEquals("!=", AdvancedQueryBuilder.getHqlOperator("iNotEqual"));
  }
  /** Get hql operator greater than. */

  @Test
  public void testGetHqlOperatorGreaterThan() {
    assertEquals(">", AdvancedQueryBuilder.getHqlOperator(GREATER_THAN));
  }
  /** Get hql operator less than. */

  @Test
  public void testGetHqlOperatorLessThan() {
    assertEquals("<", AdvancedQueryBuilder.getHqlOperator(LESS_THAN));
  }
  /** Get hql operator greater or equal. */

  @Test
  public void testGetHqlOperatorGreaterOrEqual() {
    assertEquals(">=", AdvancedQueryBuilder.getHqlOperator(GREATER_OR_EQUAL));
  }
  /** Get hql operator less or equal. */

  @Test
  public void testGetHqlOperatorLessOrEqual() {
    assertEquals("<=", AdvancedQueryBuilder.getHqlOperator(LESS_OR_EQUAL));
  }
  /** Get hql operator contains. */

  @Test
  public void testGetHqlOperatorContains() {
    assertEquals("like", AdvancedQueryBuilder.getHqlOperator(CONTAINS));
  }
  /** Get hql operator starts with. */

  @Test
  public void testGetHqlOperatorStartsWith() {
    assertEquals("like", AdvancedQueryBuilder.getHqlOperator("startsWith"));
  }
  /** Get hql operator ends with. */

  @Test
  public void testGetHqlOperatorEndsWith() {
    assertEquals("like", AdvancedQueryBuilder.getHqlOperator("endsWith"));
  }
  /** Get hql operator in set. */

  @Test
  public void testGetHqlOperatorInSet() {
    assertEquals("in", AdvancedQueryBuilder.getHqlOperator("inSet"));
  }
  /** Get hql operator not in set. */

  @Test
  public void testGetHqlOperatorNotInSet() {
    assertEquals("in", AdvancedQueryBuilder.getHqlOperator("notInSet"));
  }
  /** Get hql operator is null. */

  @Test
  public void testGetHqlOperatorIsNull() {
    assertEquals("is", AdvancedQueryBuilder.getHqlOperator("isNull"));
  }
  /** Get hql operator not null. */

  @Test
  public void testGetHqlOperatorNotNull() {
    assertEquals("is not", AdvancedQueryBuilder.getHqlOperator("notNull"));
  }
  /** Get hql operator exists. */

  @Test
  public void testGetHqlOperatorExists() {
    assertEquals("exists", AdvancedQueryBuilder.getHqlOperator("exists"));
  }
  /** Get hql operator equals field. */

  @Test
  public void testGetHqlOperatorEqualsField() {
    assertEquals("=", AdvancedQueryBuilder.getHqlOperator("equalsField"));
  }
  /** Get hql operator not equal field. */

  @Test
  public void testGetHqlOperatorNotEqualField() {
    assertEquals("!=", AdvancedQueryBuilder.getHqlOperator("notEqualField"));
  }
  /** Get hql operator unknown returns null. */

  @Test
  public void testGetHqlOperatorUnknownReturnsNull() {
    assertNull(AdvancedQueryBuilder.getHqlOperator("unknownOperator"));
  }

  // --- isLike tests (private method via reflection) ---
  /**
   * Is like true for contains.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsLikeTrueForContains() throws Exception {
    assertTrue(invokeIsLike(CONTAINS));
  }
  /**
   * Is like true for i contains.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsLikeTrueForIContains() throws Exception {
    assertTrue(invokeIsLike("iContains"));
  }
  /**
   * Is like true for starts with.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsLikeTrueForStartsWith() throws Exception {
    assertTrue(invokeIsLike("startsWith"));
  }
  /**
   * Is like true for i starts with.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsLikeTrueForIStartsWith() throws Exception {
    assertTrue(invokeIsLike("iStartsWith"));
  }
  /**
   * Is like true for ends with.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsLikeTrueForEndsWith() throws Exception {
    assertTrue(invokeIsLike("endsWith"));
  }
  /**
   * Is like false for equals.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsLikeFalseForEquals() throws Exception {
    assertFalse(invokeIsLike(EQUALS));
  }
  /**
   * Is like false for greater than.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsLikeFalseForGreaterThan() throws Exception {
    assertFalse(invokeIsLike(GREATER_THAN));
  }

  // --- isNot tests (private method via reflection) ---
  /**
   * Is not true for not contains.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsNotTrueForNotContains() throws Exception {
    assertTrue(invokeIsNot("notContains"));
  }
  /**
   * Is not true for i not contains.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsNotTrueForINotContains() throws Exception {
    assertTrue(invokeIsNot("iNotContains"));
  }
  /**
   * Is not true for not in set.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsNotTrueForNotInSet() throws Exception {
    assertTrue(invokeIsNot("notInSet"));
  }
  /**
   * Is not true for not.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsNotTrueForNot() throws Exception {
    assertTrue(invokeIsNot("not"));
  }
  /**
   * Is not false for equals.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsNotFalseForEquals() throws Exception {
    assertFalse(invokeIsNot(EQUALS));
  }
  /**
   * Is not false for contains.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsNotFalseForContains() throws Exception {
    assertFalse(invokeIsNot(CONTAINS));
  }

  // --- isGreaterOperator tests ---
  /**
   * Is greater operator true.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsGreaterOperatorTrue() throws Exception {
    assertTrue(invokeIsGreater(GREATER_THAN));
    assertTrue(invokeIsGreater(GREATER_OR_EQUAL));
    assertTrue(invokeIsGreater("iGreaterThan"));
    assertTrue(invokeIsGreater("iGreaterOrEqual"));
  }
  /**
   * Is greater operator false.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsGreaterOperatorFalse() throws Exception {
    assertFalse(invokeIsGreater(LESS_THAN));
    assertFalse(invokeIsGreater(EQUALS));
    assertFalse(invokeIsGreater(null));
  }

  // --- isLesserOperator tests ---
  /**
   * Is lesser operator true.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsLesserOperatorTrue() throws Exception {
    assertTrue(invokeIsLesser(LESS_THAN));
    assertTrue(invokeIsLesser(LESS_OR_EQUAL));
    assertTrue(invokeIsLesser("iLessThan"));
    assertTrue(invokeIsLesser("iLessOrEqual"));
  }
  /**
   * Is lesser operator false.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsLesserOperatorFalse() throws Exception {
    assertFalse(invokeIsLesser(GREATER_THAN));
    assertFalse(invokeIsLesser(EQUALS));
    assertFalse(invokeIsLesser(null));
  }

  // --- createInClause tests ---
  /**
   * Create in clause with values.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCreateInClauseWithValues() throws Exception {
    String result = invokeCreateInClause(new String[]{"org1", "org2", "org3"});
    assertEquals(" in ('org1', 'org2', 'org3')", result);
  }
  /**
   * Create in clause empty.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCreateInClauseEmpty() throws Exception {
    String result = invokeCreateInClause(new String[]{});
    assertEquals(" in ('') ", result);
  }
  /**
   * Create in clause single value.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCreateInClauseSingleValue() throws Exception {
    String result = invokeCreateInClause(new String[]{"only"});
    assertEquals(" in ('only')", result);
  }

  // --- buildLocalWhereClause tests ---
  /**
   * Build local where clause with both parts.
   * @throws Exception if an error occurs
   */

  @Test
  public void testBuildLocalWhereClauseWithBothParts() throws Exception {
    String result = invokeBuildLocalWhereClause("condition = 1", new StringBuilder(ORG_IN_0));
    assertEquals("(condition = 1) and org in ('0')", result);
  }
  /**
   * Build local where clause with where only.
   * @throws Exception if an error occurs
   */

  @Test
  public void testBuildLocalWhereClauseWithWhereOnly() throws Exception {
    String result = invokeBuildLocalWhereClause("condition = 1", new StringBuilder());
    assertEquals("(condition = 1)", result);
  }
  /**
   * Build local where clause with org only.
   * @throws Exception if an error occurs
   */

  @Test
  public void testBuildLocalWhereClauseWithOrgOnly() throws Exception {
    String result = invokeBuildLocalWhereClause("", new StringBuilder(ORG_IN_0));
    assertEquals(ORG_IN_0, result);
  }
  /**
   * Build local where clause with null where.
   * @throws Exception if an error occurs
   */

  @Test
  public void testBuildLocalWhereClauseWithNullWhere() throws Exception {
    String result = invokeBuildLocalWhereClause(null, new StringBuilder(ORG_IN_0));
    assertEquals(ORG_IN_0, result);
  }
  /**
   * Build local where clause both empty.
   * @throws Exception if an error occurs
   */

  @Test
  public void testBuildLocalWhereClauseBothEmpty() throws Exception {
    String result = invokeBuildLocalWhereClause("", new StringBuilder());
    assertEquals("", result);
  }

  // --- unEscapeOperator tests ---
  /**
   * Un escape operator replaces escaped and.
   * @throws Exception if an error occurs
   */

  @Test
  public void testUnEscapeOperatorReplacesEscapedAnd() throws Exception {
    String result = invokeUnEscapeOperator("value\\andmore");
    assertEquals("valueandmore", result);
  }
  /**
   * Un escape operator replaces escaped or.
   * @throws Exception if an error occurs
   */

  @Test
  public void testUnEscapeOperatorReplacesEscapedOr() throws Exception {
    String result = invokeUnEscapeOperator("value\\ormore");
    assertEquals("valueormore", result);
  }
  /**
   * Un escape operator null returns null.
   * @throws Exception if an error occurs
   */

  @Test
  public void testUnEscapeOperatorNullReturnsNull() throws Exception {
    Object result = invokeUnEscapeOperatorRaw(null);
    assertNull(result);
  }

  // --- getBetweenOperator tests ---
  /**
   * Get between operator between right clause.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetBetweenOperatorBetweenRightClause() throws Exception {
    String result = invokeGetBetweenOperator("between", true);
    assertEquals(LESS_THAN, result);
  }
  /**
   * Get between operator between left clause.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetBetweenOperatorBetweenLeftClause() throws Exception {
    String result = invokeGetBetweenOperator("between", false);
    assertEquals(GREATER_THAN, result);
  }
  /**
   * Get between operator i between right clause.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetBetweenOperatorIBetweenRightClause() throws Exception {
    String result = invokeGetBetweenOperator("iBetween", true);
    assertEquals("iLessThan", result);
  }
  /**
   * Get between operator between inclusive right clause.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetBetweenOperatorBetweenInclusiveRightClause() throws Exception {
    String result = invokeGetBetweenOperator("betweenInclusive", true);
    assertEquals(LESS_OR_EQUAL, result);
  }
  /**
   * Get between operator between inclusive left clause.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetBetweenOperatorBetweenInclusiveLeftClause() throws Exception {
    String result = invokeGetBetweenOperator("betweenInclusive", false);
    assertEquals(GREATER_OR_EQUAL, result);
  }
  /**
   * Get between operator unsupported.
   * @throws Exception if an error occurs
   */

  @Test(expected = IllegalArgumentException.class)
  public void testGetBetweenOperatorUnsupported() throws Exception {
    invokeGetBetweenOperator(EQUALS, false);
  }

  // --- Helper methods for reflection-based testing ---

  private boolean invokeIsLike(String operator) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("isLike", String.class);
    method.setAccessible(true);
    return (boolean) method.invoke(instance, operator);
  }

  private boolean invokeIsNot(String operator) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("isNot", String.class);
    method.setAccessible(true);
    return (boolean) method.invoke(instance, operator);
  }

  private boolean invokeIsGreater(String operator) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("isGreaterOperator", String.class);
    method.setAccessible(true);
    return (boolean) method.invoke(instance, operator);
  }

  private boolean invokeIsLesser(String operator) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("isLesserOperator", String.class);
    method.setAccessible(true);
    return (boolean) method.invoke(instance, operator);
  }

  private String invokeCreateInClause(String[] values) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("createInClause", String[].class);
    method.setAccessible(true);
    return (String) method.invoke(instance, (Object) values);
  }

  private String invokeBuildLocalWhereClause(String localWhere, StringBuilder orgPart) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("buildLocalWhereClause", String.class, StringBuilder.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, localWhere, orgPart);
  }

  @SuppressWarnings("unchecked")
  private String invokeUnEscapeOperator(String val) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("unEscapeOperator", Object.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, val);
  }

  private Object invokeUnEscapeOperatorRaw(Object val) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("unEscapeOperator", Object.class);
    method.setAccessible(true);
    return method.invoke(instance, val);
  }

  private String invokeGetBetweenOperator(String operator, boolean rightClause) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("getBetweenOperator", String.class, boolean.class);
    method.setAccessible(true);
    try {
      return (String) method.invoke(instance, operator, rightClause);
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof IllegalArgumentException) {
        throw (IllegalArgumentException) e.getCause();
      }
      throw e;
    }
  }
}
