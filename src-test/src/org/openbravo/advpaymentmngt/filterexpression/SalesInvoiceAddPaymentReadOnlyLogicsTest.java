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
import org.openbravo.advpaymentmngt.TestConstants;

/**
 * Unit tests for the SalesInvoiceAddPaymentReadOnlyLogics class.
 */
public class SalesInvoiceAddPaymentReadOnlyLogicsTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private SalesInvoiceAddPaymentReadOnlyLogics logics;
  private Map<String, String> emptyRequestMap;
  private Map<String, String> populatedRequestMap;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
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

  /**
   * Tests the getSeq method.
   */
  @Test
  public void testGetSeq() {
    assertEquals("The sequence number must be 100", 100L, logics.getSeq());
  }

  /**
   * Tests the getPaymentDocumentNoReadOnlyLogic method with an empty map.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetPaymentDocumentNoReadOnlyLogicEmptyMap() throws JSONException {
    assertFalse(TestConstants.EMPTY_MAP_MESSAGE, logics.getPaymentDocumentNoReadOnlyLogic(emptyRequestMap));
  }

  /**
   * Tests the getPaymentDocumentNoReadOnlyLogic method with a populated map.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetPaymentDocumentNoReadOnlyLogicPopulatedMap() throws JSONException {
    assertFalse(TestConstants.POPULATED_MAP_MESSAGE, logics.getPaymentDocumentNoReadOnlyLogic(populatedRequestMap));
  }

  /**
   * Tests the getReceivedFromReadOnlyLogic method with an empty map.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetReceivedFromReadOnlyLogicEmptyMap() throws JSONException {
    assertTrue("The method should return true with an empty map", logics.getReceivedFromReadOnlyLogic(emptyRequestMap));
  }

  /**
   * Tests the getReceivedFromReadOnlyLogic method with a populated map.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetReceivedFromReadOnlyLogicPopulatedMap() throws JSONException {
    assertTrue("The method should return true with a populated map",
        logics.getReceivedFromReadOnlyLogic(populatedRequestMap));
  }

  /**
   * Tests the getPaymentMethodReadOnlyLogic method with an empty map.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetPaymentMethodReadOnlyLogicEmptyMap() throws JSONException {
    assertFalse(TestConstants.EMPTY_MAP_MESSAGE, logics.getPaymentMethodReadOnlyLogic(emptyRequestMap));
  }

  /**
   * Tests the getPaymentMethodReadOnlyLogic method with a populated map.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetPaymentMethodReadOnlyLogicPopulatedMap() throws JSONException {
    assertFalse(TestConstants.POPULATED_MAP_MESSAGE, logics.getPaymentMethodReadOnlyLogic(populatedRequestMap));
  }

  /**
   * Tests the getActualPaymentReadOnlyLogic method with an empty map.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetActualPaymentReadOnlyLogicEmptyMap() throws JSONException {
    assertFalse(TestConstants.EMPTY_MAP_MESSAGE, logics.getActualPaymentReadOnlyLogic(emptyRequestMap));
  }

  /**
   * Tests the getActualPaymentReadOnlyLogic method with a populated map.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetActualPaymentReadOnlyLogicPopulatedMap() throws JSONException {
    assertFalse(TestConstants.POPULATED_MAP_MESSAGE, logics.getActualPaymentReadOnlyLogic(populatedRequestMap));
  }

  /**
   * Tests the getPaymentDateReadOnlyLogic method with an empty map.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetPaymentDateReadOnlyLogicEmptyMap() throws JSONException {
    assertFalse(TestConstants.EMPTY_MAP_MESSAGE, logics.getPaymentDateReadOnlyLogic(emptyRequestMap));
  }

  /**
   * Tests the getPaymentDateReadOnlyLogic method with a populated map.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetPaymentDateReadOnlyLogicPopulatedMap() throws JSONException {
    assertFalse(TestConstants.POPULATED_MAP_MESSAGE, logics.getPaymentDateReadOnlyLogic(populatedRequestMap));
  }

  /**
   * Tests the getFinancialAccountReadOnlyLogic method with an empty map.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetFinancialAccountReadOnlyLogicEmptyMap() throws JSONException {
    assertFalse(TestConstants.EMPTY_MAP_MESSAGE, logics.getFinancialAccountReadOnlyLogic(emptyRequestMap));
  }

  /**
   * Tests the getFinancialAccountReadOnlyLogic method with a populated map.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetFinancialAccountReadOnlyLogicPopulatedMap() throws JSONException {
    assertFalse(TestConstants.POPULATED_MAP_MESSAGE, logics.getFinancialAccountReadOnlyLogic(populatedRequestMap));
  }

  /**
   * Tests the getCurrencyReadOnlyLogic method with an empty map.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetCurrencyReadOnlyLogicEmptyMap() throws JSONException {
    assertTrue("The method should return true with an empty map", logics.getCurrencyReadOnlyLogic(emptyRequestMap));
  }

  /**
   * Tests the getCurrencyReadOnlyLogic method with a populated map.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetCurrencyReadOnlyLogicPopulatedMap() throws JSONException {
    assertTrue("The method should return true with a populated map",
        logics.getCurrencyReadOnlyLogic(populatedRequestMap));
  }

  /**
   * Tests the read-only logic methods with invalid JSON values.
   */
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

  /**
   * Tests the read-only logic methods with a large request map.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
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
