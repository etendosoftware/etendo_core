package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;

/**
 * Unit tests for the PurchaseInvoiceAddPaymentDefaultValues class.
 */
@RunWith(MockitoJUnitRunner.class)
public class PurchaseInvoiceAddPaymentDefaultValuesTest {

  @Mock
  private Invoice mockInvoice;

  @Mock
  private BusinessPartner mockBusinessPartner;

  @Mock
  private Currency mockCurrency;

  @Mock
  private Organization mockOrganization;

  @Spy
  @InjectMocks
  private PurchaseInvoiceAddPaymentDefaultValues defaultValues;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBDateUtils> mockedOBDateUtils;
  private Map<String, String> requestMap;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    // Initialize static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDateUtils = mockStatic(OBDateUtils.class);

    // Configure mocked objects
    OBDal mockOBDal = mock(OBDal.class);
    JSONObject mockContext = new JSONObject();
    mockContext.put("inpcInvoiceId", TestConstants.INVOICE_ID);

    // Set up static mocks
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    mockedOBDateUtils.when(() -> OBDateUtils.formatDate(any(Date.class))).thenReturn(TestConstants.FORMATTED_DATE);

    // Setup request map
    requestMap = new HashMap<>();
    requestMap.put("context", mockContext.toString());

    // Setup mock invoice
    when(mockOBDal.get(Invoice.class, TestConstants.INVOICE_ID)).thenReturn(mockInvoice);
    when(mockInvoice.getBusinessPartner()).thenReturn(mockBusinessPartner);
    when(mockInvoice.getCurrency()).thenReturn(mockCurrency);
    when(mockInvoice.getOrganization()).thenReturn(mockOrganization);
    when(mockBusinessPartner.getId()).thenReturn(TestConstants.BUSINESS_PARTNER_ID);
    when(mockCurrency.getId()).thenReturn(TestConstants.CURRENCY_ID);
    when(mockCurrency.getStandardPrecision()).thenReturn(2L);
    when(mockOrganization.getId()).thenReturn(TestConstants.ORGANIZATION_ID);

    // Setup payment schedule list
    List<FIN_PaymentSchedule> paymentScheduleList = new ArrayList<>();
    FIN_PaymentSchedule mockPaymentSchedule = mock(FIN_PaymentSchedule.class);
    paymentScheduleList.add(mockPaymentSchedule);

    doReturn(new BigDecimal(TestConstants.AMOUNT)).when(defaultValues).getPendingAmount(requestMap);
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    // Close all static mocks
    if (mockedOBDal != null) mockedOBDal.close();
    if (mockedOBDateUtils != null) mockedOBDateUtils.close();
  }

  /**
   * Tests the getDefaultExpectedAmount method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultExpectedAmount() throws JSONException {
    // When
    String result = defaultValues.getDefaultExpectedAmount(requestMap);

    // Then
    assertEquals(TestConstants.AMOUNT, result);
  }

  /**
   * Tests the getDefaultActualAmount method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultActualAmount() throws JSONException {
    // When
    String result = defaultValues.getDefaultActualAmount(requestMap);

    // Then
    assertEquals(TestConstants.AMOUNT, result);
  }

  /**
   * Tests the getDefaultIsSOTrx method.
   */
  @Test
  public void testGetDefaultIsSOTrx() {
    // When
    String result = defaultValues.getDefaultIsSOTrx(requestMap);

    // Then
    assertEquals("N", result);
  }

  /**
   * Tests the getDefaultTransactionType method.
   */
  @Test
  public void testGetDefaultTransactionType() {
    // When
    String result = defaultValues.getDefaultTransactionType(requestMap);

    // Then
    assertEquals("I", result);
  }

  /**
   * Tests the getDefaultPaymentType method.
   */
  @Test
  public void testGetDefaultPaymentType() {
    // When
    String result = defaultValues.getDefaultPaymentType(requestMap);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultOrderType method.
   */
  @Test
  public void testGetDefaultOrderType() {
    // When
    String result = defaultValues.getDefaultOrderType(requestMap);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultInvoiceType method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultInvoiceType() throws JSONException {
    // When
    String result = defaultValues.getDefaultInvoiceType(requestMap);

    // Then
    assertEquals(TestConstants.INVOICE_ID, result);
  }

  /**
   * Tests the getDefaultConversionRate method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultConversionRate() throws JSONException {
    // When
    String result = defaultValues.getDefaultConversionRate(requestMap);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultConvertedAmount method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultConvertedAmount() throws JSONException {
    // When
    String result = defaultValues.getDefaultConvertedAmount(requestMap);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultReceivedFrom method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultReceivedFrom() throws JSONException {
    // When
    String result = defaultValues.getDefaultReceivedFrom(requestMap);

    // Then
    assertEquals(TestConstants.BUSINESS_PARTNER_ID, result);
  }

  /**
   * Tests the getDefaultStandardPrecision method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultStandardPrecision() throws JSONException {
    // When
    String result = defaultValues.getDefaultStandardPrecision(requestMap);

    // Then
    assertEquals("2", result);
  }

  /**
   * Tests the getDefaultCurrency method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultCurrency() throws JSONException {
    // When
    String result = defaultValues.getDefaultCurrency(requestMap);

    // Then
    assertEquals(TestConstants.CURRENCY_ID, result);
  }

  /**
   * Tests the getOrganization method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOrganization() throws JSONException {
    // When
    String result = defaultValues.getOrganization(requestMap);

    // Then
    assertEquals(TestConstants.ORGANIZATION_ID, result);
  }

  /**
   * Tests the getDefaultDocument method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultDocument() throws JSONException {
    // When
    String result = defaultValues.getDefaultDocument(requestMap);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the getDefaultPaymentDate method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetDefaultPaymentDate() throws JSONException {
    // When
    String result = defaultValues.getDefaultPaymentDate(requestMap);

    // Then
    assertEquals(TestConstants.FORMATTED_DATE, result);
  }

  /**
   * Tests the getBankStatementLineAmount method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetBankStatementLineAmount() throws JSONException {
    // When
    String result = defaultValues.getBankStatementLineAmount(requestMap);

    // Then
    assertEquals("", result);
  }

  /**
   * Tests the getSeq method.
   */
  @Test
  public void testGetSeq() {
    // When
    long result = defaultValues.getSeq();

    // Then
    assertEquals(100L, result);
  }
}
