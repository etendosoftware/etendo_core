package com.smf.jobs.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.ProcessInvoiceUtil;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonUtils;

import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;
import com.smf.jobs.Data;

/**
 * Unit tests for the ProcessInvoices class.
 * This class contains tests to validate the behavior of processing invoices
 * in various scenarios, such as handling void dates, verifying successful
 * processing, and ensuring proper pre-run setup.
 * It uses JUnit and Mockito for testing and mocking dependencies.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessInvoicesTest {


  @Spy
  @InjectMocks
  private TestableProcessInvoices processInvoices;

  @Mock
  private WeldUtils mockWeldUtils;

  @Mock
  private ProcessInvoiceUtil mockProcessInvoiceUtil;

  @Mock
  private RequestContext mockRequestContext;

  @Mock
  private VariablesSecureApp mockVars;

  @Mock
  private Invoice mockInvoice;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Data mockData;

  private Method processInvoiceMethod;
  private Method getInputContentsMethod;


  /**
   * Sets up the test environment, including retrieving the private method
   * "processInvoice" from the ProcessInvoices class for invocation in tests.
   *
   * @throws Exception if the method cannot be accessed or found.
   */
  @Before
  public void setUp() throws Exception {
    processInvoiceMethod = ProcessInvoices.class.getDeclaredMethod(
        "processInvoice",
        Invoice.class,
        String.class,
        String.class,
        String.class
    );
    processInvoiceMethod.setAccessible(true);

    // Add reflection setup for getInputContents
    getInputContentsMethod = Action.class.getDeclaredMethod(
        "getInputContents",
        Class.class
    );
    getInputContentsMethod.setAccessible(true);
  }

  /**
   * Tests the processInvoice method when void dates are not provided.
   * Validates that the invoice is processed successfully with the provided
   * document action.
   *
   * @throws Exception if the method invocation fails.
   */
  @Test
  public void testProcessInvoiceWithoutVoidDates() throws Exception {
    String invoiceId = "test-invoice-id";
    String docAction = "CO";
    OBError expectedResult = new OBError();
    expectedResult.setType(Utility.SUCCESS);
    expectedResult.setMessage("Invoice processed successfully");

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class)) {
      requestContextMock.when(RequestContext::get).thenReturn(mockRequestContext);
      when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);
      when(mockWeldUtils.getInstance(ProcessInvoiceUtil.class)).thenReturn(mockProcessInvoiceUtil);
      when(mockInvoice.getId()).thenReturn(invoiceId);
      when(mockProcessInvoiceUtil.process(
          eq(invoiceId),
          eq(docAction),
          eq(""),
          eq(""),
          any(VariablesSecureApp.class),
          any(DalConnectionProvider.class)
      )).thenReturn(expectedResult);

      OBError result = (OBError) processInvoiceMethod.invoke(
          processInvoices,
          mockInvoice,
          docAction,
          null,
          null
      );

      assertEquals(Utility.SHOULD_RETURN_SUCCESS_TYPE, Utility.SUCCESS, result.getType());
      assertEquals("Should return correct message", "Invoice processed successfully", result.getMessage());
    }
  }

  /**
   * Tests the processInvoice method when void dates are provided.
   * Validates that the invoice is processed successfully and formatted
   * dates are passed to the process method.
   *
   * @throws Exception if the method invocation fails.
   */
  @Test
  public void testProcessInvoiceWithVoidDates() throws Exception {
    String invoiceId = "test-invoice-id";
    String docAction = "VO";
    String voidDate = "2024-01-15";
    String voidAcctDate = "2024-01-15";

    OBError expectedResult = new OBError();
    expectedResult.setType(Utility.SUCCESS);

    SimpleDateFormat jsonDateFormat = JsonUtils.createDateFormat();
    Date testDate = jsonDateFormat.parse(voidDate);
    String formattedDate = OBDateUtils.formatDate(testDate);

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class)) {
      requestContextMock.when(RequestContext::get).thenReturn(mockRequestContext);
      when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);
      when(mockWeldUtils.getInstance(ProcessInvoiceUtil.class)).thenReturn(mockProcessInvoiceUtil);
      when(mockInvoice.getId()).thenReturn(invoiceId);
      when(mockProcessInvoiceUtil.process(
          eq(invoiceId),
          eq(docAction),
          eq(formattedDate),
          eq(formattedDate),
          any(VariablesSecureApp.class),
          any(DalConnectionProvider.class)
      )).thenReturn(expectedResult);

      OBError result = (OBError) processInvoiceMethod.invoke(
          processInvoices,
          mockInvoice,
          docAction,
          voidDate,
          voidAcctDate
      );

      assertEquals(Utility.SHOULD_RETURN_SUCCESS_TYPE, Utility.SUCCESS, result.getType());
    }
  }

  /**
   * A testable subclass of ProcessInvoices that exposes certain protected
   * or private methods for testing purposes.
   */
  public static class TestableProcessInvoices extends ProcessInvoices {
    @Override
    public Data getInput() {
      return super.getInput();
    }
  }

  /**
   * Verifies that the getInputClass method returns the correct input class
   * for processing invoices.
   */
  @Test
  public void testGetInputClass() {
    assertEquals("Should return Invoice.class", Invoice.class, processInvoices.getInputClass());
  }

   /**
   * Tests the action method to ensure it correctly handles processing invoices
   * when the processing result is successful.
   *
   * @throws Exception if the action execution fails.
   */
  @Test
  public void testActionWithSuccessfulProcessing() throws Exception {
    JSONObject parameters = new JSONObject();
    parameters.put("DocAction", "CO");
    MutableBoolean isStopped = new MutableBoolean(false);

    List<Invoice> mockInvoices = List.of(mockInvoice);
    OBError successResult = new OBError();
    successResult.setType(Utility.SUCCESS);

    // Instead of using doReturn().when(), use reflection
    when(getInputContentsMethod.invoke(processInvoices, Invoice.class))
        .thenReturn(mockInvoices);

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class)) {
      requestContextMock.when(RequestContext::get).thenReturn(mockRequestContext);
      when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);
      when(mockWeldUtils.getInstance(ProcessInvoiceUtil.class))
          .thenReturn(mockProcessInvoiceUtil);
      when(mockInvoice.getId()).thenReturn("testId");
      when(mockProcessInvoiceUtil.process(
          anyString(),
          anyString(),
          anyString(),
          anyString(),
          any(VariablesSecureApp.class),
          any(DalConnectionProvider.class)
      )).thenReturn(successResult);

      ActionResult result = processInvoices.action(parameters, isStopped);

      assertEquals(Utility.SHOULD_RETURN_SUCCESS_TYPE, Result.Type.SUCCESS, result.getType());
    }
  }

  /**
   * Tests the preRun method to verify that invoices with the "process now"
   * flag are properly locked, updated, and saved before processing.
   *
   * @throws Exception if the preRun setup or execution fails.
   */
  @Test
  public void testPreRunWithLockedInvoice() throws Exception {
    JSONObject jsonContent = new JSONObject();
    JSONObject params = new JSONObject();
    params.put("DocAction", "CO");
    jsonContent.put("_params", params);

    List<Invoice> mockInvoices = List.of(mockInvoice);
    OBError successResult = new OBError();
    successResult.setType(Utility.SUCCESS);

    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class)) {

      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      doNothing().when(mockOBDal).save(any());
      doNothing().when(mockOBDal).flush();

      // Use reflection for getInputContents
      when(getInputContentsMethod.invoke(processInvoices, Invoice.class))
          .thenReturn(mockInvoices);
      doReturn(mockData).when(processInvoices).getInput();

      // Rest of the test remains the same
      when(mockInvoice.isProcessNow()).thenReturn(true);
      when(mockInvoice.getId()).thenReturn("testId");

      requestContextMock.when(RequestContext::get).thenReturn(mockRequestContext);
      when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);
      when(mockWeldUtils.getInstance(ProcessInvoiceUtil.class))
          .thenReturn(mockProcessInvoiceUtil);
      when(mockProcessInvoiceUtil.process(
          anyString(),
          eq("XL"),
          anyString(),
          anyString(),
          any(VariablesSecureApp.class),
          any(DalConnectionProvider.class)
      )).thenReturn(successResult);

      Data result = processInvoices.preRun(jsonContent);

      assertNotNull("Result should not be null", result);
      verify(mockInvoice, times(1)).setAPRMProcessinvoice("--");
      verify(mockOBDal, times(1)).save(mockInvoice);
      verify(mockOBDal, times(1)).flush();
    }
  }
}