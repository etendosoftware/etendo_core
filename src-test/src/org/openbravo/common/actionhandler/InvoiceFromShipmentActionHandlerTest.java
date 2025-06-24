package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.dal.core.DalContextListener;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.InvoiceFromGoodsShipmentUtil;
import org.openbravo.materialmgmt.InvoiceGeneratorFromGoodsShipment;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.service.json.JsonUtils;
import org.openbravo.test.base.mock.OBServletContextMock;

/**
 * Unit tests for the {@link InvoiceFromShipmentActionHandler} class.
 * Verifies the behavior of the action handler when generating invoices from shipments.
 */
public class InvoiceFromShipmentActionHandlerTest {

  private InvoiceFromShipmentActionHandler actionHandler;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<JsonUtils> mockedJsonUtils;
  private MockedStatic<InvoiceFromGoodsShipmentUtil> mockedInvoiceFromGoodsShipmentUtil;
  private MockedStatic<DalContextListener> mockedDalContextListener;
  private MockedStatic<InvoiceGeneratorFromGoodsShipment> mockedInvoiceGenerator;

  private OBDal mockOBDal;
  private Invoice mockInvoice;

  /**
   * Sets up the test environment before each test.
   * Initializes the action handler and mocks required for testing.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @BeforeEach
  public void setUp() throws Exception {
    actionHandler = new InvoiceFromShipmentActionHandler();

    mockOBDal = mock(OBDal.class);
    mockInvoice = mock(Invoice.class);
    PriceList mockPriceList = mock(PriceList.class);
    ServletContext mockServletContext = new OBServletContextMock();

    mockedOBDal = mockStatic(OBDal.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedJsonUtils = mockStatic(JsonUtils.class);
    mockedInvoiceFromGoodsShipmentUtil = mockStatic(InvoiceFromGoodsShipmentUtil.class);
    mockedDalContextListener = mockStatic(DalContextListener.class);
    mockedInvoiceGenerator = mockStatic(InvoiceGeneratorFromGoodsShipment.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    mockedDalContextListener.when(DalContextListener::getServletContext).thenReturn(mockServletContext);
    when(mockOBDal.getProxy(eq(PriceList.class), anyString())).thenReturn(mockPriceList);

    mockedJsonUtils.when(JsonUtils::createDateFormat).thenReturn(new java.text.SimpleDateFormat("yyyy-MM-dd"));
  }

  /**
   * Cleans up the test environment after each test.
   * Closes all mocked static objects to release resources.
   */
  @AfterEach
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
    if (mockedJsonUtils != null) {
      mockedJsonUtils.close();
    }
    if (mockedInvoiceFromGoodsShipmentUtil != null) {
      mockedInvoiceFromGoodsShipmentUtil.close();
    }
    if (mockedDalContextListener != null) {
      mockedDalContextListener.close();
    }
    if (mockedInvoiceGenerator != null) {
      mockedInvoiceGenerator.close();
    }
  }

  /**
   * Tests the {@code doExecute} method when an exception occurs.
   * Verifies that the error message is correctly returned in the response.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecuteException() throws Exception {
    // Given
    Map<String, Object> parameters = new HashMap<>();
    String content = "{" + "\"M_InOut_ID\": \"TEST_SHIPMENT_ID\"," + "\"_params\": {" + "    \"DateInvoiced\": \"2023-01-01\"," + "    \"priceList\": \"TEST_PRICELIST_ID\"," + "    \"processInvoice\": true" + "  }" + "}";

    when(mockOBDal.getProxy(eq(PriceList.class), anyString())).thenThrow(new RuntimeException("Test exception"));

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(ActionHandlerTestConstants.ERROR_TITLE)).thenReturn(ActionHandlerTestConstants.ERROR_TITLE);

    JSONObject result = actionHandler.doExecute(parameters, content);

    assertNotNull(result);
    assertTrue(result.has("message"));

    JSONObject message = result.getJSONObject("message");
    assertEquals("error", message.getString(ActionHandlerTestConstants.SEVERITY));
    assertEquals(ActionHandlerTestConstants.ERROR_TITLE, message.getString(ActionHandlerTestConstants.TITLE));
    assertEquals("Test exception", message.getString("text"));
  }

  /**
   * Tests the {@code getSuccessMessage} method with a valid invoice.
   * Verifies that the success message is correctly generated.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetSuccessMessageWithInvoice() throws Exception {
    String documentNo = "INV-001";
    String invoiceStatus = "Completed";
    String successMessage = ActionHandlerTestConstants.SUCCESS;
    String newInvoiceMessage = "New Invoice {0} generated with status {1}";

    when(mockInvoice.getDocumentNo()).thenReturn(documentNo);
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(ActionHandlerTestConstants.SUCCESS)).thenReturn(successMessage);
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("NewInvoiceGenerated")).thenReturn(newInvoiceMessage);
    mockedInvoiceFromGoodsShipmentUtil.when(
        () -> InvoiceFromGoodsShipmentUtil.getInvoiceStatus(mockInvoice)).thenReturn(invoiceStatus);

    JSONObject result = actionHandler.getSuccessMessage(mockInvoice);

    assertNotNull(result);
    assertEquals("success", result.getString(ActionHandlerTestConstants.SEVERITY));
    assertEquals(successMessage, result.getString(ActionHandlerTestConstants.TITLE));

    String expectedText = String.format(newInvoiceMessage, documentNo, invoiceStatus);
    assertEquals(expectedText, result.getString("text"));
  }

  /**
   * Tests the {@code getSuccessMessage} method with a null invoice.
   * Verifies that the appropriate message is returned when no invoice is generated.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetSuccessMessageNullInvoice() throws Exception {
    String successMessage = ActionHandlerTestConstants.SUCCESS;
    String noInvoiceMessage = "No Invoice Generated";

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(ActionHandlerTestConstants.SUCCESS)).thenReturn(successMessage);
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("NoInvoiceGenerated")).thenReturn(noInvoiceMessage);

    JSONObject result = actionHandler.getSuccessMessage(null);

    assertNotNull(result);
    assertEquals("success", result.getString(ActionHandlerTestConstants.SEVERITY));
    assertEquals(successMessage, result.getString(ActionHandlerTestConstants.TITLE));
    assertEquals(noInvoiceMessage, result.getString("text"));
  }
}
