package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test class for {@link AddPaymentDocumentNoActionHandler} which handles the generation of payment document numbers.
 * This test suite verifies the functionality of payment document number generation for both sales and purchase
 * transactions, as well as error handling for invalid inputs.
 *
 * <p>The test class uses Mockito to mock static dependencies:</p>
 * <ul>
 *   <li>{@link OBDal} - For database access operations</li>
 *   <li>{@link FIN_Utility} - For document number generation</li>
 *   <li>{@link StringUtils} - For string manipulation utilities</li>
 * </ul>
 *
 * @see AddPaymentDocumentNoActionHandler
 * @see OBBaseTest
 */
public class AddPaymentDocumentNoActionHandlerTest extends OBBaseTest {

  /**
   * Rule for testing expected exceptions.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddPaymentDocumentNoActionHandler actionHandler;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<FIN_Utility> mockedFINUtility;
  private MockedStatic<StringUtils> mockedStringUtils;

  /**
   * Sets up the test environment before each test method.
   * Initializes mocks and creates a new instance of the action handler.
   *
   * @throws Exception
   *     if any error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    // Initialize mocks
    MockitoAnnotations.openMocks(this);
    actionHandler = new AddPaymentDocumentNoActionHandler();

    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedFINUtility = mockStatic(FIN_Utility.class);
    mockedStringUtils = mockStatic(StringUtils.class);
  }

  /**
   * Cleans up resources after each test method.
   * Closes all static mocks to prevent memory leaks.
   */
  @After
  public void tearDown() {
    // Close static mocks
    if (mockedOBDal != null) mockedOBDal.close();
    if (mockedFINUtility != null) mockedFINUtility.close();
    if (mockedStringUtils != null) mockedStringUtils.close();
  }

  /**
   * Tests the execution of the action handler for a sales transaction.
   * Verifies that the correct document number is generated and formatted for sales transactions.
   *
   * @throws Exception
   *     if any error occurs during the test execution
   */
  @Test
  public void testExecuteSalesTransaction() throws Exception {
    // GIVEN
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = "{\"organization\":\"orgId\", \"issotrx\":\"true\"}";

    // Mock OBDal
    OBDal mockOBDal = mock(OBDal.class);
    Organization mockOrganization = mock(Organization.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    when(mockOBDal.get(Organization.class, "orgId")).thenReturn(mockOrganization);

    // Mock StringUtils
    when(StringUtils.isNotEmpty(anyString())).thenReturn(true);

    // Mock FIN_Utility
    when(FIN_Utility.getDocumentNo(any(Organization.class), anyString(), anyString(), anyBoolean())).thenReturn(
        "ARR-001");

    // WHEN
    JSONObject result = actionHandler.execute(parameters, jsonData);

    // THEN
    assertNotNull(result);
    assertTrue(result.has(TestConstants.PAYMENT_DOCUMENT_NO));
    assertEquals("<ARR-001>", result.getString(TestConstants.PAYMENT_DOCUMENT_NO));
  }

  /**
   * Tests the execution of the action handler for a purchase transaction.
   * Verifies that the correct document number is generated and formatted for purchase transactions.
   *
   * @throws Exception
   *     if any error occurs during the test execution
   */
  @Test
  public void testExecutePurchaseTransaction() throws Exception {
    // GIVEN
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = "{\"organization\":\"orgId\", \"issotrx\":\"false\"}";

    // Mock OBDal
    OBDal mockOBDal = mock(OBDal.class);
    Organization mockOrganization = mock(Organization.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    when(mockOBDal.get(Organization.class, "orgId")).thenReturn(mockOrganization);

    // Mock StringUtils
    when(StringUtils.isNotEmpty(anyString())).thenReturn(true);

    // Mock FIN_Utility
    when(FIN_Utility.getDocumentNo(any(Organization.class), anyString(), anyString(), anyBoolean())).thenReturn(
        "APP-001");

    // WHEN
    JSONObject result = actionHandler.execute(parameters, jsonData);

    // THEN
    assertNotNull(result);
    assertTrue(result.has(TestConstants.PAYMENT_DOCUMENT_NO));
    assertEquals("<APP-001>", result.getString(TestConstants.PAYMENT_DOCUMENT_NO));
  }

  /**
   * Tests the execution of the action handler with invalid JSON data.
   * Verifies that the handler throws an OBException when provided with malformed JSON data.
   */
  @Test
  public void testExecuteInvalidJSONData() {
    // GIVEN
    Map<String, Object> parameters = new HashMap<>();
    String invalidJsonData = "invalid json";

    // THEN
    expectedException.expect(OBException.class);

    // WHEN
    actionHandler.execute(parameters, invalidJsonData);
  }
}
