/*
 * Unit tests for AdvancedQueryBuilder.
 */
package org.openbravo.service.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Tests for {@link AdvancedQueryBuilder}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AdvancedQueryBuilderTest {

  private AdvancedQueryBuilder instance;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(AdvancedQueryBuilder.class);
  }

  // --- getHqlOperator tests (public static method) ---

  @Test
  public void testGetHqlOperatorEquals() {
    assertEquals("=", AdvancedQueryBuilder.getHqlOperator("equals"));
  }

  @Test
  public void testGetHqlOperatorNotEqual() {
    assertEquals("!=", AdvancedQueryBuilder.getHqlOperator("notEqual"));
  }

  @Test
  public void testGetHqlOperatorIEquals() {
    assertEquals("=", AdvancedQueryBuilder.getHqlOperator("iEquals"));
  }

  @Test
  public void testGetHqlOperatorINotEqual() {
    assertEquals("!=", AdvancedQueryBuilder.getHqlOperator("iNotEqual"));
  }

  @Test
  public void testGetHqlOperatorGreaterThan() {
    assertEquals(">", AdvancedQueryBuilder.getHqlOperator("greaterThan"));
  }

  @Test
  public void testGetHqlOperatorLessThan() {
    assertEquals("<", AdvancedQueryBuilder.getHqlOperator("lessThan"));
  }

  @Test
  public void testGetHqlOperatorGreaterOrEqual() {
    assertEquals(">=", AdvancedQueryBuilder.getHqlOperator("greaterOrEqual"));
  }

  @Test
  public void testGetHqlOperatorLessOrEqual() {
    assertEquals("<=", AdvancedQueryBuilder.getHqlOperator("lessOrEqual"));
  }

  @Test
  public void testGetHqlOperatorContains() {
    assertEquals("like", AdvancedQueryBuilder.getHqlOperator("contains"));
  }

  @Test
  public void testGetHqlOperatorStartsWith() {
    assertEquals("like", AdvancedQueryBuilder.getHqlOperator("startsWith"));
  }

  @Test
  public void testGetHqlOperatorEndsWith() {
    assertEquals("like", AdvancedQueryBuilder.getHqlOperator("endsWith"));
  }

  @Test
  public void testGetHqlOperatorInSet() {
    assertEquals("in", AdvancedQueryBuilder.getHqlOperator("inSet"));
  }

  @Test
  public void testGetHqlOperatorNotInSet() {
    assertEquals("in", AdvancedQueryBuilder.getHqlOperator("notInSet"));
  }

  @Test
  public void testGetHqlOperatorIsNull() {
    assertEquals("is", AdvancedQueryBuilder.getHqlOperator("isNull"));
  }

  @Test
  public void testGetHqlOperatorNotNull() {
    assertEquals("is not", AdvancedQueryBuilder.getHqlOperator("notNull"));
  }

  @Test
  public void testGetHqlOperatorExists() {
    assertEquals("exists", AdvancedQueryBuilder.getHqlOperator("exists"));
  }

  @Test
  public void testGetHqlOperatorEqualsField() {
    assertEquals("=", AdvancedQueryBuilder.getHqlOperator("equalsField"));
  }

  @Test
  public void testGetHqlOperatorNotEqualField() {
    assertEquals("!=", AdvancedQueryBuilder.getHqlOperator("notEqualField"));
  }

  @Test
  public void testGetHqlOperatorUnknownReturnsNull() {
    assertNull(AdvancedQueryBuilder.getHqlOperator("unknownOperator"));
  }

  // --- isLike tests (private method via reflection) ---

  @Test
  public void testIsLikeTrueForContains() throws Exception {
    assertTrue(invokeIsLike("contains"));
  }

  @Test
  public void testIsLikeTrueForIContains() throws Exception {
    assertTrue(invokeIsLike("iContains"));
  }

  @Test
  public void testIsLikeTrueForStartsWith() throws Exception {
    assertTrue(invokeIsLike("startsWith"));
  }

  @Test
  public void testIsLikeTrueForIStartsWith() throws Exception {
    assertTrue(invokeIsLike("iStartsWith"));
  }

  @Test
  public void testIsLikeTrueForEndsWith() throws Exception {
    assertTrue(invokeIsLike("endsWith"));
  }

  @Test
  public void testIsLikeFalseForEquals() throws Exception {
    assertFalse(invokeIsLike("equals"));
  }

  @Test
  public void testIsLikeFalseForGreaterThan() throws Exception {
    assertFalse(invokeIsLike("greaterThan"));
  }

  // --- isNot tests (private method via reflection) ---

  @Test
  public void testIsNotTrueForNotContains() throws Exception {
    assertTrue(invokeIsNot("notContains"));
  }

  @Test
  public void testIsNotTrueForINotContains() throws Exception {
    assertTrue(invokeIsNot("iNotContains"));
  }

  @Test
  public void testIsNotTrueForNotInSet() throws Exception {
    assertTrue(invokeIsNot("notInSet"));
  }

  @Test
  public void testIsNotTrueForNot() throws Exception {
    assertTrue(invokeIsNot("not"));
  }

  @Test
  public void testIsNotFalseForEquals() throws Exception {
    assertFalse(invokeIsNot("equals"));
  }

  @Test
  public void testIsNotFalseForContains() throws Exception {
    assertFalse(invokeIsNot("contains"));
  }

  // --- isGreaterOperator tests ---

  @Test
  public void testIsGreaterOperatorTrue() throws Exception {
    assertTrue(invokeIsGreater("greaterThan"));
    assertTrue(invokeIsGreater("greaterOrEqual"));
    assertTrue(invokeIsGreater("iGreaterThan"));
    assertTrue(invokeIsGreater("iGreaterOrEqual"));
  }

  @Test
  public void testIsGreaterOperatorFalse() throws Exception {
    assertFalse(invokeIsGreater("lessThan"));
    assertFalse(invokeIsGreater("equals"));
    assertFalse(invokeIsGreater(null));
  }

  // --- isLesserOperator tests ---

  @Test
  public void testIsLesserOperatorTrue() throws Exception {
    assertTrue(invokeIsLesser("lessThan"));
    assertTrue(invokeIsLesser("lessOrEqual"));
    assertTrue(invokeIsLesser("iLessThan"));
    assertTrue(invokeIsLesser("iLessOrEqual"));
  }

  @Test
  public void testIsLesserOperatorFalse() throws Exception {
    assertFalse(invokeIsLesser("greaterThan"));
    assertFalse(invokeIsLesser("equals"));
    assertFalse(invokeIsLesser(null));
  }

  // --- createInClause tests ---

  @Test
  public void testCreateInClauseWithValues() throws Exception {
    String result = invokeCreateInClause(new String[]{"org1", "org2", "org3"});
    assertEquals(" in ('org1', 'org2', 'org3')", result);
  }

  @Test
  public void testCreateInClauseEmpty() throws Exception {
    String result = invokeCreateInClause(new String[]{});
    assertEquals(" in ('') ", result);
  }

  @Test
  public void testCreateInClauseSingleValue() throws Exception {
    String result = invokeCreateInClause(new String[]{"only"});
    assertEquals(" in ('only')", result);
  }

  // --- buildLocalWhereClause tests ---

  @Test
  public void testBuildLocalWhereClauseWithBothParts() throws Exception {
    String result = invokeBuildLocalWhereClause("condition = 1", new StringBuilder("org in ('0')"));
    assertEquals("(condition = 1) and org in ('0')", result);
  }

  @Test
  public void testBuildLocalWhereClauseWithWhereOnly() throws Exception {
    String result = invokeBuildLocalWhereClause("condition = 1", new StringBuilder());
    assertEquals("(condition = 1)", result);
  }

  @Test
  public void testBuildLocalWhereClauseWithOrgOnly() throws Exception {
    String result = invokeBuildLocalWhereClause("", new StringBuilder("org in ('0')"));
    assertEquals("org in ('0')", result);
  }

  @Test
  public void testBuildLocalWhereClauseWithNullWhere() throws Exception {
    String result = invokeBuildLocalWhereClause(null, new StringBuilder("org in ('0')"));
    assertEquals("org in ('0')", result);
  }

  @Test
  public void testBuildLocalWhereClauseBothEmpty() throws Exception {
    String result = invokeBuildLocalWhereClause("", new StringBuilder());
    assertEquals("", result);
  }

  // --- unEscapeOperator tests ---

  @Test
  public void testUnEscapeOperatorReplacesEscapedAnd() throws Exception {
    String result = invokeUnEscapeOperator("value\\andmore");
    assertEquals("valueandmore", result);
  }

  @Test
  public void testUnEscapeOperatorReplacesEscapedOr() throws Exception {
    String result = invokeUnEscapeOperator("value\\ormore");
    assertEquals("valueormore", result);
  }

  @Test
  public void testUnEscapeOperatorNullReturnsNull() throws Exception {
    Object result = invokeUnEscapeOperatorRaw(null);
    assertNull(result);
  }

  // --- getBetweenOperator tests ---

  @Test
  public void testGetBetweenOperatorBetweenRightClause() throws Exception {
    String result = invokeGetBetweenOperator("between", true);
    assertEquals("lessThan", result);
  }

  @Test
  public void testGetBetweenOperatorBetweenLeftClause() throws Exception {
    String result = invokeGetBetweenOperator("between", false);
    assertEquals("greaterThan", result);
  }

  @Test
  public void testGetBetweenOperatorIBetweenRightClause() throws Exception {
    String result = invokeGetBetweenOperator("iBetween", true);
    assertEquals("iLessThan", result);
  }

  @Test
  public void testGetBetweenOperatorBetweenInclusiveRightClause() throws Exception {
    String result = invokeGetBetweenOperator("betweenInclusive", true);
    assertEquals("lessOrEqual", result);
  }

  @Test
  public void testGetBetweenOperatorBetweenInclusiveLeftClause() throws Exception {
    String result = invokeGetBetweenOperator("betweenInclusive", false);
    assertEquals("greaterOrEqual", result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetBetweenOperatorUnsupported() throws Exception {
    invokeGetBetweenOperator("equals", false);
  }

  // --- Helper methods for reflection-based testing ---

  private boolean invokeIsLike(String operator) throws Exception {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("isLike", String.class);
    method.setAccessible(true);
    return (boolean) method.invoke(instance, operator);
  }

  private boolean invokeIsNot(String operator) throws Exception {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("isNot", String.class);
    method.setAccessible(true);
    return (boolean) method.invoke(instance, operator);
  }

  private boolean invokeIsGreater(String operator) throws Exception {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("isGreaterOperator", String.class);
    method.setAccessible(true);
    return (boolean) method.invoke(instance, operator);
  }

  private boolean invokeIsLesser(String operator) throws Exception {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("isLesserOperator", String.class);
    method.setAccessible(true);
    return (boolean) method.invoke(instance, operator);
  }

  private String invokeCreateInClause(String[] values) throws Exception {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("createInClause", String[].class);
    method.setAccessible(true);
    return (String) method.invoke(instance, (Object) values);
  }

  private String invokeBuildLocalWhereClause(String localWhere, StringBuilder orgPart) throws Exception {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("buildLocalWhereClause", String.class, StringBuilder.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, localWhere, orgPart);
  }

  @SuppressWarnings("unchecked")
  private String invokeUnEscapeOperator(String val) throws Exception {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("unEscapeOperator", Object.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, val);
  }

  private Object invokeUnEscapeOperatorRaw(Object val) throws Exception {
    Method method = AdvancedQueryBuilder.class.getDeclaredMethod("unEscapeOperator", Object.class);
    method.setAccessible(true);
    return method.invoke(instance, val);
  }

  private String invokeGetBetweenOperator(String operator, boolean rightClause) throws Exception {
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
