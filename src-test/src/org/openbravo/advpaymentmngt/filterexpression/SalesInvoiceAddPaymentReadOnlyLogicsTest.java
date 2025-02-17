package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SalesInvoiceAddPaymentReadOnlyLogicsTest {

  private SalesInvoiceAddPaymentReadOnlyLogics logics;
  private Map<String, String> emptyRequestMap;
  private Map<String, String> populatedRequestMap;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    logics = new SalesInvoiceAddPaymentReadOnlyLogics();
    emptyRequestMap = new HashMap<>();
    populatedRequestMap = new HashMap<>();
    populatedRequestMap.put("C_BPartner_ID", "1000000");
    populatedRequestMap.put("C_Currency_ID", "100");
    populatedRequestMap.put("C_DocType_ID", "1000");
    populatedRequestMap.put("PaymentMethod", "B");
  }

  @Test
  public void testGetSeq() {
    assertEquals("The sequence number must be 100", 100L, logics.getSeq());
  }

  @Test
  public void testGetPaymentDocumentNoReadOnlyLogic_EmptyMap() throws JSONException {
    assertFalse("The method should return false with an empty map",
        logics.getPaymentDocumentNoReadOnlyLogic(emptyRequestMap));
  }

  @Test
  public void testGetPaymentDocumentNoReadOnlyLogic_PopulatedMap() throws JSONException {
    assertFalse("The method should return false with a populated map",
        logics.getPaymentDocumentNoReadOnlyLogic(populatedRequestMap));
  }

  @Test
  public void testGetReceivedFromReadOnlyLogic_EmptyMap() throws JSONException {
    assertTrue("The method should return true with an empty map",
        logics.getReceivedFromReadOnlyLogic(emptyRequestMap));
  }

  @Test
  public void testGetReceivedFromReadOnlyLogic_PopulatedMap() throws JSONException {
    assertTrue("The method should return true with a populated map",
        logics.getReceivedFromReadOnlyLogic(populatedRequestMap));
  }

  @Test
  public void testGetPaymentMethodReadOnlyLogic_EmptyMap() throws JSONException {
    assertFalse("The method should return false with an empty map",
        logics.getPaymentMethodReadOnlyLogic(emptyRequestMap));
  }

  @Test
  public void testGetPaymentMethodReadOnlyLogic_PopulatedMap() throws JSONException {
    assertFalse("The method should return false with a populated map",
        logics.getPaymentMethodReadOnlyLogic(populatedRequestMap));
  }

  @Test
  public void testGetActualPaymentReadOnlyLogic_EmptyMap() throws JSONException {
    assertFalse("The method should return false with an empty map",
        logics.getActualPaymentReadOnlyLogic(emptyRequestMap));
  }

  @Test
  public void testGetActualPaymentReadOnlyLogic_PopulatedMap() throws JSONException {
    assertFalse("The method should return false with a populated map",
        logics.getActualPaymentReadOnlyLogic(populatedRequestMap));
  }

  @Test
  public void testGetPaymentDateReadOnlyLogic_EmptyMap() throws JSONException {
    assertFalse("The method should return false with an empty map",
        logics.getPaymentDateReadOnlyLogic(emptyRequestMap));
  }

  @Test
  public void testGetPaymentDateReadOnlyLogic_PopulatedMap() throws JSONException {
    assertFalse("The method should return false with a populated map",
        logics.getPaymentDateReadOnlyLogic(populatedRequestMap));
  }

  @Test
  public void testGetFinancialAccountReadOnlyLogic_EmptyMap() throws JSONException {
    assertFalse("The method should return false with an empty map",
        logics.getFinancialAccountReadOnlyLogic(emptyRequestMap));
  }

  @Test
  public void testGetFinancialAccountReadOnlyLogic_PopulatedMap() throws JSONException {
    assertFalse("The method should return false with a populated map",
        logics.getFinancialAccountReadOnlyLogic(populatedRequestMap));
  }

  @Test
  public void testGetCurrencyReadOnlyLogic_EmptyMap() throws JSONException {
    assertTrue("The method should return true with an empty map",
        logics.getCurrencyReadOnlyLogic(emptyRequestMap));
  }

  @Test
  public void testGetCurrencyReadOnlyLogic_PopulatedMap() throws JSONException {
    assertTrue("The method should return true with a populated map",
        logics.getCurrencyReadOnlyLogic(populatedRequestMap));
  }

  @Test
  public void testWithInvalidJSONValues() {
    Map<String, String> invalidJsonMap = new HashMap<>();
    invalidJsonMap.put("jsonValue", "{invalid:json}");

    try {
      assertFalse(logics.getPaymentDocumentNoReadOnlyLogic(invalidJsonMap));
      assertTrue(logics.getReceivedFromReadOnlyLogic(invalidJsonMap));
      assertFalse(logics.getPaymentMethodReadOnlyLogic(invalidJsonMap));
      assertFalse(logics.getActualPaymentReadOnlyLogic(invalidJsonMap));
      assertFalse(logics.getPaymentDateReadOnlyLogic(invalidJsonMap));
      assertFalse(logics.getFinancialAccountReadOnlyLogic(invalidJsonMap));
      assertTrue(logics.getCurrencyReadOnlyLogic(invalidJsonMap));
    } catch (JSONException e) {
      fail("JSONException was not expected: " + e.getMessage());
    }
  }

  @Test
  public void testWithLargeRequestMap() throws JSONException {
    Map<String, String> largeMap = new HashMap<>();
    for (int i = 0; i < 1000; i++) {
      largeMap.put("key" + i, "value" + i);
    }

    assertFalse(logics.getPaymentDocumentNoReadOnlyLogic(largeMap));
    assertTrue(logics.getReceivedFromReadOnlyLogic(largeMap));
    assertFalse(logics.getPaymentMethodReadOnlyLogic(largeMap));
    assertFalse(logics.getActualPaymentReadOnlyLogic(largeMap));
    assertFalse(logics.getPaymentDateReadOnlyLogic(largeMap));
    assertFalse(logics.getFinancialAccountReadOnlyLogic(largeMap));
    assertTrue(logics.getCurrencyReadOnlyLogic(largeMap));
  }
}
