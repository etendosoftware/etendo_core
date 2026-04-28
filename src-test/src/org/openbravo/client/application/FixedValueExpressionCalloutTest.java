package org.openbravo.client.application;

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
/** Tests for {@link FixedValueExpressionCallout}. */

@RunWith(MockitoJUnitRunner.Silent.class)
public class FixedValueExpressionCalloutTest {

  private static final String INPEVALUATEFIXEDVALUE = "inpevaluatefixedvalue";
  private static final String GET_EXPRESSION = "getExpression";

  private FixedValueExpressionCallout instance;

  @Mock
  private VariablesSecureApp mockVars;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(FixedValueExpressionCallout.class);
  }
  /**
   * Get expression when evaluate fixed value is y.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetExpressionWhenEvaluateFixedValueIsY() throws Exception {
    when(mockVars.getStringParameter(INPEVALUATEFIXEDVALUE)).thenReturn("Y");
    when(mockVars.getStringParameter("inpfixedvalue")).thenReturn("someExpression");

    Method getExpression = FixedValueExpressionCallout.class.getDeclaredMethod(GET_EXPRESSION,
        VariablesSecureApp.class);
    getExpression.setAccessible(true);
    String result = (String) getExpression.invoke(instance, mockVars);

    assertEquals("someExpression", result);
  }
  /**
   * Get expression when evaluate fixed value is n.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetExpressionWhenEvaluateFixedValueIsN() throws Exception {
    when(mockVars.getStringParameter(INPEVALUATEFIXEDVALUE)).thenReturn("N");

    Method getExpression = FixedValueExpressionCallout.class.getDeclaredMethod(GET_EXPRESSION,
        VariablesSecureApp.class);
    getExpression.setAccessible(true);
    String result = (String) getExpression.invoke(instance, mockVars);

    assertEquals("", result);
  }
  /**
   * Get expression when evaluate fixed value is empty.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetExpressionWhenEvaluateFixedValueIsEmpty() throws Exception {
    when(mockVars.getStringParameter(INPEVALUATEFIXEDVALUE)).thenReturn("");

    Method getExpression = FixedValueExpressionCallout.class.getDeclaredMethod(GET_EXPRESSION,
        VariablesSecureApp.class);
    getExpression.setAccessible(true);
    String result = (String) getExpression.invoke(instance, mockVars);

    assertEquals("", result);
  }
  /**
   * Get expression when evaluate fixed value is null.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetExpressionWhenEvaluateFixedValueIsNull() throws Exception {
    when(mockVars.getStringParameter(INPEVALUATEFIXEDVALUE)).thenReturn(null);

    Method getExpression = FixedValueExpressionCallout.class.getDeclaredMethod(GET_EXPRESSION,
        VariablesSecureApp.class);
    getExpression.setAccessible(true);
    String result = (String) getExpression.invoke(instance, mockVars);

    assertEquals("", result);
  }
}
