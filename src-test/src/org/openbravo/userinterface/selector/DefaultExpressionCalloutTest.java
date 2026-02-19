package org.openbravo.userinterface.selector;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.secureApp.VariablesSecureApp;

/**
 * Tests for {@link DefaultExpressionCallout}.
 * Tests the getExpression protected method via reflection.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultExpressionCalloutTest {

  private DefaultExpressionCallout instance;

  @Mock
  private VariablesSecureApp mockVars;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(DefaultExpressionCallout.class);
  }

  @Test
  public void testGetExpressionReturnsDefaultExpressionValue() throws Exception {
    // Arrange
    when(mockVars.getStringParameter("inpLastFieldChanged")).thenReturn("inpdefaultExpression");
    when(mockVars.getStringParameter("inpdefaultExpression")).thenReturn("some_default_expression");

    // Act
    String result = invokeGetExpression(mockVars);

    // Assert
    assertEquals("some_default_expression", result);
  }

  @Test
  public void testGetExpressionReturnsFilterExpressionValue() throws Exception {
    // Arrange
    when(mockVars.getStringParameter("inpLastFieldChanged")).thenReturn("inpfilterExpression");
    when(mockVars.getStringParameter("inpfilterExpression")).thenReturn("some_filter_expression");

    // Act
    String result = invokeGetExpression(mockVars);

    // Assert
    assertEquals("some_filter_expression", result);
  }

  @Test
  public void testGetExpressionReturnsEmptyForUnknownField() throws Exception {
    // Arrange
    when(mockVars.getStringParameter("inpLastFieldChanged")).thenReturn("unknownField");

    // Act
    String result = invokeGetExpression(mockVars);

    // Assert
    assertEquals("", result);
  }

  @Test
  public void testGetExpressionReturnsEmptyForNullLastFieldChanged() throws Exception {
    // Arrange
    when(mockVars.getStringParameter("inpLastFieldChanged")).thenReturn(null);

    // Act
    String result = invokeGetExpression(mockVars);

    // Assert
    assertEquals("", result);
  }

  private String invokeGetExpression(VariablesSecureApp vars) throws Exception {
    Method method = DefaultExpressionCallout.class.getDeclaredMethod("getExpression",
        VariablesSecureApp.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, vars);
  }
}
