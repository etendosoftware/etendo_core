package org.openbravo.advpaymentmngt.hqlinjections;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.base.mock.MockitoStaticMockUtils.mockStaticSafely;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;

/**
 * Tests for AddPaymentCreditToUseInjector class
 */
@RunWith(MockitoJUnitRunner.class)
public class AddPaymentCreditToUseInjectorTest {

  private static final String BUSINESS_PARTNER_ID = "TEST_BP_ID";
  private static final String INVALID_BP_ID = "-1";

  @Mock
  private OBDal obDal;

  @Mock
  private BusinessPartner businessPartner;

  @InjectMocks
  private AddPaymentCreditToUseInjector classUnderTest;

  private MockedStatic<OBDal> mockedOBDal;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    // Initialize static mocks
    mockedOBDal = mockStaticSafely(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

    // Setup business partner mock
    when(businessPartner.getId()).thenReturn(BUSINESS_PARTNER_ID);
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    // Close static mocks
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
  }

  /**
   * Test the insertHql method with a valid business partner ID and sales transaction set to true
   */
  @Test
  public void testInsertHqlValidBusinessPartnerSalesTransaction() {
    // GIVEN
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(TestConstants.RECEIVED_FROM, BUSINESS_PARTNER_ID);
    requestParameters.put(TestConstants.IS_SO_TRX, "true");

    Map<String, Object> queryNamedParameters = new HashMap<>();

    when(obDal.get(BusinessPartner.class, BUSINESS_PARTNER_ID)).thenReturn(businessPartner);

    // WHEN
    String result = classUnderTest.insertHql(requestParameters, queryNamedParameters);

    // THEN
    assertEquals(TestConstants.BP_RECEIPT_CONDITION, result);
    assertEquals(BUSINESS_PARTNER_ID, queryNamedParameters.get("bp"));
    assertEquals(true, queryNamedParameters.get(TestConstants.IS_SO_TRX));
  }

  /**
   * Test the insertHql method with a valid business partner ID and sales transaction set to false
   */
  @Test
  public void testInsertHqlValidBusinessPartnerPurchaseTransaction() {
    // GIVEN
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(TestConstants.RECEIVED_FROM, BUSINESS_PARTNER_ID);
    requestParameters.put(TestConstants.IS_SO_TRX, "false");

    Map<String, Object> queryNamedParameters = new HashMap<>();

    when(obDal.get(BusinessPartner.class, BUSINESS_PARTNER_ID)).thenReturn(businessPartner);

    // WHEN
    String result = classUnderTest.insertHql(requestParameters, queryNamedParameters);

    // THEN
    assertEquals(TestConstants.BP_RECEIPT_CONDITION, result);
    assertEquals(BUSINESS_PARTNER_ID, queryNamedParameters.get("bp"));
    assertEquals(false, queryNamedParameters.get(TestConstants.IS_SO_TRX));
  }

  /**
   * Test the insertHql method with a null business partner ID
   */
  @Test
  public void testInsertHqlNullBusinessPartner() {
    // GIVEN
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(TestConstants.IS_SO_TRX, "true");

    Map<String, Object> queryNamedParameters = new HashMap<>();

    // WHEN
    String result = classUnderTest.insertHql(requestParameters, queryNamedParameters);

    // THEN
    assertEquals(TestConstants.BP_RECEIPT_CONDITION, result);
    assertEquals(INVALID_BP_ID, queryNamedParameters.get("bp"));
    assertEquals(true, queryNamedParameters.get(TestConstants.IS_SO_TRX));
  }

  /**
   * Test the insertHql method with a business partner ID that doesn't exist in the database
   */
  @Test
  public void testInsertHqlNonExistentBusinessPartner() {
    // GIVEN
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(TestConstants.RECEIVED_FROM, BUSINESS_PARTNER_ID);
    requestParameters.put(TestConstants.IS_SO_TRX, "true");

    Map<String, Object> queryNamedParameters = new HashMap<>();

    when(obDal.get(BusinessPartner.class, BUSINESS_PARTNER_ID)).thenReturn(null);

    // WHEN
    String result = classUnderTest.insertHql(requestParameters, queryNamedParameters);

    // THEN
    assertEquals(TestConstants.BP_RECEIPT_CONDITION, result);
    assertEquals(INVALID_BP_ID, queryNamedParameters.get("bp"));
    assertEquals(true, queryNamedParameters.get(TestConstants.IS_SO_TRX));
  }

  /**
   * Test the insertHql method with missing issotrx parameter (should default to false)
   */
  @Test
  public void testInsertHqlMissingIsSoTrx() {
    // GIVEN
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(TestConstants.RECEIVED_FROM, BUSINESS_PARTNER_ID);

    Map<String, Object> queryNamedParameters = new HashMap<>();

    when(obDal.get(BusinessPartner.class, BUSINESS_PARTNER_ID)).thenReturn(businessPartner);

    // WHEN
    String result = classUnderTest.insertHql(requestParameters, queryNamedParameters);

    // THEN
    assertEquals(TestConstants.BP_RECEIPT_CONDITION, result);
    assertEquals(BUSINESS_PARTNER_ID, queryNamedParameters.get("bp"));
    assertEquals(false, queryNamedParameters.get(TestConstants.IS_SO_TRX));
  }
}
