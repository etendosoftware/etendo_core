package org.openbravo.advpaymentmngt.hqlinjections;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for AddPaymentGLItemInjector
 * Tests the HQL injection for Payment GL Item table filtering
 */
@RunWith(MockitoJUnitRunner.class)
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
  @Before
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
    
    assertEquals(CORRECT_HQL_CONDITION, EXPECTED_HQL, result);
    assertTrue(PID_PARAMETER, queryNamedParameters.containsKey(PID));
    assertEquals("Parameter value should match", paymentId, queryNamedParameters.get(PID));
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
      
      assertEquals(CORRECT_HQL_CONDITION, EXPECTED_HQL, result);
      assertEquals("Parameter value should match for " + paymentId, 
          paymentId, queryNamedParameters.get(PID));
    }
  }

  /**
   * Verifies behavior when payment ID is null.
   */
  @Test
  public void testInsertHqlWithNullPaymentId() {
    requestParameters.put(FIN_PAYMENT_ID, null);
    
    String result = injector.insertHql(requestParameters, queryNamedParameters);
    
    assertEquals(CORRECT_HQL_CONDITION, EXPECTED_HQL, result);
    assertTrue(PID_PARAMETER, queryNamedParameters.containsKey(PID));
    assertNull("Parameter value should be null", queryNamedParameters.get(PID));
  }

  /**
   * Verifies behavior when payment ID is empty string.
   */
  @Test
  public void testInsertHqlWithEmptyPaymentId() {
    String emptyId = "";
    requestParameters.put(FIN_PAYMENT_ID, emptyId);
    
    String result = injector.insertHql(requestParameters, queryNamedParameters);
    
    assertEquals(CORRECT_HQL_CONDITION, EXPECTED_HQL, result);
    assertEquals("Parameter value should be empty string", emptyId, queryNamedParameters.get(PID));
  }

  /**
   * Verifies behavior when payment ID parameter is missing.
   */
  @Test
  public void testInsertHqlWithMissingPaymentIdParameter() {
    // Don't put any fin_payment_id in requestParameters
    
    String result = injector.insertHql(requestParameters, queryNamedParameters);
    
    assertEquals(CORRECT_HQL_CONDITION, EXPECTED_HQL, result);
    assertTrue(PID_PARAMETER, queryNamedParameters.containsKey(PID));
    assertNull("Parameter value should be null when missing", queryNamedParameters.get(PID));
  }

  /**
   * Verifies that the returned HQL string format is always consistent.
   */
  @Test
  public void testReturnedHqlStringFormat() {
    requestParameters.put(FIN_PAYMENT_ID, "test-payment");
    
    String result = injector.insertHql(requestParameters, queryNamedParameters);
    
    assertNotNull("Result should not be null", result);
    assertTrue("Should start with 'p.id'", result.startsWith("p.id"));
    assertTrue("Should contain '='", result.contains("="));
    assertTrue("Should contain ':pid'", result.contains(":pid"));
    assertEquals("Should match exact format", EXPECTED_HQL, result);
  }

  /**
   * Verifies that multiple calls with different values update the parameter correctly.
   */
  @Test
  public void testMultipleCallsUpdateParameter() {
    // First call
    requestParameters.put(FIN_PAYMENT_ID, "first-payment");
    injector.insertHql(requestParameters, queryNamedParameters);
    assertEquals("First value should be set", "first-payment", queryNamedParameters.get(PID));
    
    // Second call with different value
    requestParameters.put(FIN_PAYMENT_ID, "second-payment");
    injector.insertHql(requestParameters, queryNamedParameters);
    assertEquals("Second value should replace first", "second-payment", queryNamedParameters.get(PID));
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
    
    assertEquals("Existing param 1 should be preserved", "value1", queryNamedParameters.get("existingParam1"));
    assertEquals("Existing param 2 should be preserved", 123, queryNamedParameters.get("existingParam2"));
    assertEquals("New param should be added", "new-payment", queryNamedParameters.get(PID));
    assertEquals("Should have 3 parameters", 3, queryNamedParameters.size());
  }

  /**
   * Verifies handling of whitespace in payment ID.
   */
  @Test
  public void testPaymentIdWithWhitespace() {
    String paymentIdWithSpaces = "  payment-id-with-spaces  ";
    requestParameters.put(FIN_PAYMENT_ID, paymentIdWithSpaces);
    
    String result = injector.insertHql(requestParameters, queryNamedParameters);
    
    assertEquals(CORRECT_HQL_CONDITION, EXPECTED_HQL, result);
    assertEquals("Parameter value should preserve whitespace", 
        paymentIdWithSpaces, queryNamedParameters.get(PID));
  }

  /**
   * Verifies handling of special characters in payment ID.
   */
  @Test
  public void testPaymentIdWithSpecialCharacters() {
    String paymentIdWithSpecialChars = "payment#123!@$%";
    requestParameters.put(FIN_PAYMENT_ID, paymentIdWithSpecialChars);
    
    String result = injector.insertHql(requestParameters, queryNamedParameters);
    
    assertEquals(CORRECT_HQL_CONDITION, EXPECTED_HQL, result);
    assertEquals("Parameter value should preserve special characters", 
        paymentIdWithSpecialChars, queryNamedParameters.get(PID));
  }
}
