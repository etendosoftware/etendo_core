package org.openbravo.common.actionhandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollableResults;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.exception.OBException;
import org.openbravo.financial.FinancialUtils;

@RunWith(MockitoJUnitRunner.class)
public class CheckAvailableCreditActionHandlerTest {

  private static final String BP_ID = "TEST_BP_001";
  private static final String CURRENCY_ID = "102";

  private CheckAvailableCreditActionHandler instance;

  @Mock
  private ScrollableResults mockScroll;

  private MockedStatic<FinancialUtils> financialUtilsStatic;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(CheckAvailableCreditActionHandler.class);
    financialUtilsStatic = mockStatic(FinancialUtils.class);
  }

  @After
  public void tearDown() {
    if (financialUtilsStatic != null) {
      financialUtilsStatic.close();
    }
  }

  private JSONObject invokeExecute(String data) throws Exception {
    Method method = CheckAvailableCreditActionHandler.class.getDeclaredMethod("execute",
        Map.class, String.class);
    method.setAccessible(true);
    return (JSONObject) method.invoke(instance, new HashMap<String, Object>(), data);
  }

  @Test
  public void testExecuteWithValidCurrencyAndCreditAvailable() throws Exception {
    financialUtilsStatic
        .when(() -> FinancialUtils.getPaymentsWithCredit(BP_ID, CURRENCY_ID))
        .thenReturn(mockScroll);
    when(mockScroll.next()).thenReturn(true);

    JSONObject data = new JSONObject();
    data.put("businessPartnerId", BP_ID);
    data.put("currencyId", CURRENCY_ID);

    JSONObject result = invokeExecute(data.toString());

    assertNotNull(result);
    assertEquals(true, result.get("availableCredit"));
    verify(mockScroll).close();
  }

  @Test
  public void testExecuteWithValidCurrencyAndNoCreditAvailable() throws Exception {
    financialUtilsStatic
        .when(() -> FinancialUtils.getPaymentsWithCredit(BP_ID, CURRENCY_ID))
        .thenReturn(mockScroll);
    when(mockScroll.next()).thenReturn(false);

    JSONObject data = new JSONObject();
    data.put("businessPartnerId", BP_ID);
    data.put("currencyId", CURRENCY_ID);

    JSONObject result = invokeExecute(data.toString());

    assertNotNull(result);
    assertEquals(false, result.get("availableCredit"));
    verify(mockScroll).close();
  }

  @Test
  public void testExecuteWithEmptyCurrencyReturnsFalse() throws Exception {
    JSONObject data = new JSONObject();
    data.put("businessPartnerId", BP_ID);
    data.put("currencyId", "");

    JSONObject result = invokeExecute(data.toString());

    assertNotNull(result);
    assertEquals(false, result.get("availableCredit"));
  }

  @Test
  public void testExecuteWithScrollExceptionReturnsFalse() throws Exception {
    financialUtilsStatic
        .when(() -> FinancialUtils.getPaymentsWithCredit(BP_ID, CURRENCY_ID))
        .thenReturn(mockScroll);
    when(mockScroll.next()).thenThrow(new RuntimeException("DB error"));

    JSONObject data = new JSONObject();
    data.put("businessPartnerId", BP_ID);
    data.put("currencyId", CURRENCY_ID);

    JSONObject result = invokeExecute(data.toString());

    assertNotNull(result);
    assertEquals(false, result.get("availableCredit"));
    verify(mockScroll).close();
  }

  @Test(expected = OBException.class)
  public void testExecuteWithInvalidJsonThrowsOBException() throws Exception {
    try {
      invokeExecute("invalid json");
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof OBException) {
        throw (OBException) e.getCause();
      }
      throw new RuntimeException(e);
    }
  }
}
