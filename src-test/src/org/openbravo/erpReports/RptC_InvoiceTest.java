package org.openbravo.erpReports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.DateTimeData;

/**
 * Tests for {@link RptC_Invoice}.
 */
@ExtendWith(MockitoExtension.class)
public class RptC_InvoiceTest {

  @InjectMocks
  @Spy
  private RptC_Invoice servlet;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private VariablesSecureApp vars;

  /**
   * Tests doPost with DEFAULT command and empty session value.
   * @throws IOException if an input or output error occurs during the servlet execution
   * @throws ServletException if the servlet encounters difficulty while handling the request
   */
  @Test
  public void testDoPostCommandDefaultCallsPrintPagePDF() throws IOException, ServletException {
    doReturn(vars).when(servlet).createVars(request);
    when(vars.commandIn(TestUtils.DEFAULT)).thenReturn(true);
    when(vars.getSessionValue(TestUtils.RPT_C_INVOICE_INPCINVOICEID_R)).thenReturn("");
    when(vars.getSessionValue(TestUtils.RPT_C_INVOICE_INPCINVOICEID)).thenReturn(TestUtils.SOME_ID);

    doNothing().when(servlet).printPagePDF(any(HttpServletResponse.class), any(VariablesSecureApp.class),
        anyString());

    servlet.doPost(request, response);

    verify(vars).commandIn(TestUtils.DEFAULT);
    verify(vars).getSessionValue(TestUtils.RPT_C_INVOICE_INPCINVOICEID_R);
    verify(vars).getSessionValue(TestUtils.RPT_C_INVOICE_INPCINVOICEID);
    verify(servlet).printPagePDF(eq(response), eq(vars), eq(TestUtils.SOME_ID));
  }

  /**
   * Tests doPost with DEFAULT command and non-empty session value.
   * @throws IOException if an input or output error occurs during the servlet execution
   * @throws ServletException if the servlet encounters difficulty while handling the request
   */
  @Test
  public void testDoPostCommandDefaultWithNonEmptySessionValue() throws IOException, ServletException {
    doReturn(vars).when(servlet).createVars(request);
    when(vars.commandIn(TestUtils.DEFAULT)).thenReturn(true);
    when(vars.getSessionValue(TestUtils.RPT_C_INVOICE_INPCINVOICEID_R)).thenReturn(TestUtils.SOME_ID);

    doNothing().when(servlet).printPagePDF(any(HttpServletResponse.class), any(VariablesSecureApp.class),
        anyString());

    servlet.doPost(request, response);

    verify(vars).commandIn(TestUtils.DEFAULT);
    verify(vars).getSessionValue(TestUtils.RPT_C_INVOICE_INPCINVOICEID_R);
    verify(servlet).printPagePDF(eq(response), eq(vars), eq(TestUtils.SOME_ID));
  }

  /**
   * Tests doPost with FIND command.
   * @throws IOException if an input or output error occurs during the servlet execution
   * @throws ServletException if the servlet encounters difficulty while handling the request
   */
  @Test
  public void testDoPostCommandFindCallsPrintPagePDF() throws IOException, ServletException {
    doReturn(vars).when(servlet).createVars(request);
    when(vars.commandIn(TestUtils.DEFAULT)).thenReturn(false);
    when(vars.commandIn(TestUtils.FIND)).thenReturn(true);

    when(vars.getStringParameter(TestUtils.INPC_BPARTNER_ID)).thenReturn(TestUtils.PARTNER_ID);
    when(vars.getStringParameter(TestUtils.INP_DATE_INVOICE_FROM)).thenReturn(TestUtils.DATE);
    when(vars.getStringParameter(TestUtils.INP_DATE_INVOICE_TO)).thenReturn(TestUtils.DATE);
    when(vars.getStringParameter(TestUtils.INP_INVOICEDOCUMENTNO_FROM)).thenReturn(TestUtils.DOC_NO_FROM);
    when(vars.getStringParameter(TestUtils.INP_INVOICEDOCUMENTNO_TO)).thenReturn(TestUtils.DOC_NO_TO);

    try (MockedStatic<RptCInvoiceData> rptCInvoiceDataMock = mockStatic(RptCInvoiceData.class);
         MockedStatic<DateTimeData> dateTimeDataMock = mockStatic(DateTimeData.class)) {

      dateTimeDataMock.when(() -> DateTimeData.nDaysAfter(any(), anyString(), anyString()))
          .thenReturn(TestUtils.DATE);

      RptCInvoiceData dataMock = new RptCInvoiceData();
      dataMock.cInvoiceId = TestUtils.DUMMY_INVOICE_ID;
      rptCInvoiceDataMock.when(() -> RptCInvoiceData.select(any(), anyString(), anyString(), anyString(), anyString(), anyString()))
          .thenReturn(new RptCInvoiceData[] { dataMock });

      doNothing().when(servlet).printPagePDF(any(HttpServletResponse.class), any(VariablesSecureApp.class), anyString());

      servlet.doPost(request, response);

      verify(vars).commandIn(TestUtils.FIND);
      verify(vars).getStringParameter(TestUtils.INPC_BPARTNER_ID);
      verify(vars).getStringParameter(TestUtils.INP_DATE_INVOICE_FROM);
      verify(vars).getStringParameter(TestUtils.INP_DATE_INVOICE_TO);
      verify(vars).getStringParameter(TestUtils.INP_INVOICEDOCUMENTNO_FROM);
      verify(vars).getStringParameter(TestUtils.INP_INVOICEDOCUMENTNO_TO);

      verify(servlet).printPagePDF(eq(response), eq(vars), eq("(dummyInvoiceId)"));
    }
  }

  /**
   * Tests getServletInfo.
   */
  @Test
  public void testGetServletInfoReturnsCorrectString() {
    String info = servlet.getServletInfo();
    assertEquals("Servlet that presents the RptCOrders seeker", info);
  }
}
