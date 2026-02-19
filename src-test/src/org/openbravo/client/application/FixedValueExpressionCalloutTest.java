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

@RunWith(MockitoJUnitRunner.Silent.class)
public class FixedValueExpressionCalloutTest {

  private FixedValueExpressionCallout instance;

  @Mock
  private VariablesSecureApp mockVars;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(FixedValueExpressionCallout.class);
  }

  @Test
  public void testGetExpressionWhenEvaluateFixedValueIsY() throws Exception {
    when(mockVars.getStringParameter("inpevaluatefixedvalue")).thenReturn("Y");
    when(mockVars.getStringParameter("inpfixedvalue")).thenReturn("someExpression");

    Method getExpression = FixedValueExpressionCallout.class.getDeclaredMethod("getExpression",
        VariablesSecureApp.class);
    getExpression.setAccessible(true);
    String result = (String) getExpression.invoke(instance, mockVars);

    assertEquals("someExpression", result);
  }

  @Test
  public void testGetExpressionWhenEvaluateFixedValueIsN() throws Exception {
    when(mockVars.getStringParameter("inpevaluatefixedvalue")).thenReturn("N");

    Method getExpression = FixedValueExpressionCallout.class.getDeclaredMethod("getExpression",
        VariablesSecureApp.class);
    getExpression.setAccessible(true);
    String result = (String) getExpression.invoke(instance, mockVars);

    assertEquals("", result);
  }

  @Test
  public void testGetExpressionWhenEvaluateFixedValueIsEmpty() throws Exception {
    when(mockVars.getStringParameter("inpevaluatefixedvalue")).thenReturn("");

    Method getExpression = FixedValueExpressionCallout.class.getDeclaredMethod("getExpression",
        VariablesSecureApp.class);
    getExpression.setAccessible(true);
    String result = (String) getExpression.invoke(instance, mockVars);

    assertEquals("", result);
  }

  @Test
  public void testGetExpressionWhenEvaluateFixedValueIsNull() throws Exception {
    when(mockVars.getStringParameter("inpevaluatefixedvalue")).thenReturn(null);

    Method getExpression = FixedValueExpressionCallout.class.getDeclaredMethod("getExpression",
        VariablesSecureApp.class);
    getExpression.setAccessible(true);
    String result = (String) getExpression.invoke(instance, mockVars);

    assertEquals("", result);
  }
}
