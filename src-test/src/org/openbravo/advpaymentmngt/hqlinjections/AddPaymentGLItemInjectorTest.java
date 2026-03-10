package org.openbravo.advpaymentmngt.hqlinjections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for AddPaymentGLItemInjector
 * Tests the HQL injection for Payment GL Item table filtering
 */
@ExtendWith(MockitoExtension.class)
public class AddPaymentGLItemInjectorTest {

  private static final String FIN_PAYMENT_ID = "fin_payment_id";
  private static final String PID = "pid";
  private static final String EXPECTED_HQL = "p.id = :pid";
  public static final String PID_PARAMETER = "Should add pid parameter";
  public static final String CORRECT_HQL_CONDITION = "Should return correct HQL condition";

  private AddPaymentGLItemInjector injector;
  private Map<String, String> requestParameters;
  private Map<String, Object> queryNamedParameters;

  /**
   * Set up test fixtures before each test.
   * Initializes injector instance and parameter maps.
   */
  @BeforeEach
  public void setUp() {
    injector = new AddPaymentGLItemInjector();
    requestParameters = new HashMap<>();
    queryNamedParameters = new HashMap<>();
  }

  /**
   * Verifies that valid payment ID is correctly inserted into HQL.
   */
  @Test
  public void testInsertHqlWithValidPaymentId() {
    String paymentId = "ABC123";
    requestParameters.put(FIN_PAYMENT_ID, paymentId);

    String result = injector.insertHql(requestParameters, queryNamedParameters);

    assertEquals(EXPECTED_HQL, result, CORRECT_HQL_CONDITION);
    assertTrue(queryNamedParameters.containsKey(PID), PID_PARAMETER);
    assertEquals(paymentId, queryNamedParameters.get(PID), "Parameter value should match");
  }

  /**
   * Verifies that different payment ID values are handled correctly.
   */
  @Test
  public void testInsertHqlWithDifferentPaymentIds() {
    String[] paymentIds = {
        "12345",
        "PAYMENT-001",
        "abc-def-ghi",
        "00000000-0000-0000-0000-000000000000"
    };

    for (String paymentId : paymentIds) {
      requestParameters.clear();
      queryNamedParameters.clear();
      requestParameters.put(FIN_PAYMENT_ID, paymentId);

      String result = injector.insertHql(requestParameters, queryNamedParameters);

      assertEquals(EXPECTED_HQL, result, CORRECT_HQL_CONDITION);
      assertEquals(paymentId, queryNamedParameters.get(PID),
          "Parameter value should match for " + paymentId);
    }
  }

  /**
   * Verifies behavior when payment ID is null.
   */
  @Test
  public void testInsertHqlWithNullPaymentId() {
    requestParameters.put(FIN_PAYMENT_ID, null);

    String result = injector.insertHql(requestParameters, queryNamedParameters);

    assertEquals(EXPECTED_HQL, result, CORRECT_HQL_CONDITION);
    assertTrue(queryNamedParameters.containsKey(PID), PID_PARAMETER);
    assertNull(queryNamedParameters.get(PID), "Parameter value should be null");
  }

  /**
   * Verifies behavior when payment ID is empty string.
   */
  @Test
  public void testInsertHqlWithEmptyPaymentId() {
    String emptyId = "";
    requestParameters.put(FIN_PAYMENT_ID, emptyId);

    String result = injector.insertHql(requestParameters, queryNamedParameters);

    assertEquals(EXPECTED_HQL, result, CORRECT_HQL_CONDITION);
    assertEquals(emptyId, queryNamedParameters.get(PID), "Parameter value should be empty string");
  }

  /**
   * Verifies behavior when payment ID parameter is missing.
   */
  @Test
  public void testInsertHqlWithMissingPaymentIdParameter() {
    // Don't put any fin_payment_id in requestParameters

    String result = injector.insertHql(requestParameters, queryNamedParameters);

    assertEquals(EXPECTED_HQL, result, CORRECT_HQL_CONDITION);
    assertTrue(queryNamedParameters.containsKey(PID), PID_PARAMETER);
    assertNull(queryNamedParameters.get(PID), "Parameter value should be null when missing");
  }

  /**
   * Verifies that the returned HQL string format is always consistent.
   */
  @Test
  public void testReturnedHqlStringFormat() {
    requestParameters.put(FIN_PAYMENT_ID, "test-payment");

    String result = injector.insertHql(requestParameters, queryNamedParameters);

    assertNotNull(result, "Result should not be null");
    assertTrue(result.startsWith("p.id"), "Should start with 'p.id'");
    assertTrue(result.contains("="), "Should contain '='");
    assertTrue(result.contains(":pid"), "Should contain ':pid'");
    assertEquals(EXPECTED_HQL, result, "Should match exact format");
  }

  /**
   * Verifies that multiple calls with different values update the parameter correctly.
   */
  @Test
  public void testMultipleCallsUpdateParameter() {
    // First call
    requestParameters.put(FIN_PAYMENT_ID, "first-payment");
    injector.insertHql(requestParameters, queryNamedParameters);
    assertEquals("first-payment", queryNamedParameters.get(PID), "First value should be set");

    // Second call with different value
    requestParameters.put(FIN_PAYMENT_ID, "second-payment");
    injector.insertHql(requestParameters, queryNamedParameters);
    assertEquals("second-payment", queryNamedParameters.get(PID), "Second value should replace first");
  }

  /**
   * Verifies that existing parameters in the map are not affected.
   */
  @Test
  public void testExistingParametersPreserved() {
    queryNamedParameters.put("existingParam1", "value1");
    queryNamedParameters.put("existingParam2", 123);

    requestParameters.put(FIN_PAYMENT_ID, "new-payment");
    injector.insertHql(requestParameters, queryNamedParameters);

    assertEquals("value1", queryNamedParameters.get("existingParam1"), "Existing param 1 should be preserved");
    assertEquals(123, queryNamedParameters.get("existingParam2"), "Existing param 2 should be preserved");
    assertEquals("new-payment", queryNamedParameters.get(PID), "New param should be added");
    assertEquals(3, queryNamedParameters.size(), "Should have 3 parameters");
  }

  /**
   * Verifies handling of whitespace in payment ID.
   */
  @Test
  public void testPaymentIdWithWhitespace() {
    String paymentIdWithSpaces = "  payment-id-with-spaces  ";
    requestParameters.put(FIN_PAYMENT_ID, paymentIdWithSpaces);

    String result = injector.insertHql(requestParameters, queryNamedParameters);

    assertEquals(EXPECTED_HQL, result, CORRECT_HQL_CONDITION);
    assertEquals(paymentIdWithSpaces, queryNamedParameters.get(PID),
        "Parameter value should preserve whitespace");
  }

  /**
   * Verifies handling of special characters in payment ID.
   */
  @Test
  public void testPaymentIdWithSpecialCharacters() {
    String paymentIdWithSpecialChars = "payment#123!@$%";
    requestParameters.put(FIN_PAYMENT_ID, paymentIdWithSpecialChars);

    String result = injector.insertHql(requestParameters, queryNamedParameters);

    assertEquals(EXPECTED_HQL, result, CORRECT_HQL_CONDITION);
    assertEquals(paymentIdWithSpecialChars, queryNamedParameters.get(PID),
        "Parameter value should preserve special characters");
  }
}
