package org.openbravo.erpReports;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.secureApp.VariablesSecureApp;

/**
 * Tests for {@link ReportTaxPaymentJR}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReportTaxPaymentJRTest {

  @InjectMocks
  @Spy
  private ReportTaxPaymentJR servlet;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private VariablesSecureApp vars;

  /**
   * Tests doPost with DEFAULT command.
   *
   * @throws IOException
   *     if an input or output error occurs during the servlet execution
   * @throws ServletException
   *     if the servlet encounters difficulty while handling the request
   */
  @Test
  public void testDoPostCommandDefaultCallsPrintPageDataSheet() throws IOException, ServletException {
    doReturn(vars).when(servlet).createVars(request);
    when(vars.commandIn(TestUtils.DEFAULT)).thenReturn(true);
    when(vars.getSessionValue(TestUtils.RPT_TAXPAYMENT_ID)).thenReturn(TestUtils.SOME_ID);

    try (MockedStatic<TaxPaymentData> taxPaymentDataMock = mockStatic(TaxPaymentData.class)) {
      TaxPaymentData dataMock = new TaxPaymentData();
      dataMock.datefrom = TestUtils.DATE_FROM;
      dataMock.dateto = TestUtils.DATE_TO;
      taxPaymentDataMock.when(() -> TaxPaymentData.select(any(), eq(TestUtils.CLEAN_ID))).thenReturn(
          new TaxPaymentData[]{ dataMock });

      doNothing().when(servlet).printPageDataSheet(any(HttpServletResponse.class), any(VariablesSecureApp.class),
          anyString(), anyString());

      servlet.doPost(request, response);

      verify(vars).commandIn(TestUtils.DEFAULT);
      verify(vars).getSessionValue(TestUtils.RPT_TAXPAYMENT_ID);
      verify(servlet).printPageDataSheet(eq(response), eq(vars), eq(TestUtils.DATE_FROM), eq(TestUtils.DATE_TO));
    }
  }

  /**
   * Tests doPost with EDIT_HTML command and valid TypeVatReport.
   *
   * @throws IOException
   *     if an input or output error occurs during the servlet execution
   * @throws ServletException
   *     if the servlet encounters difficulty while handling the request
   */
  @Test
  public void testDoPostCommandEditHtmlCallsPrintReportJRRegisterByVat() throws IOException, ServletException {
    doReturn(vars).when(servlet).createVars(request);
    when(vars.commandIn(TestUtils.DEFAULT)).thenReturn(false);
    when(vars.commandIn(TestUtils.EDIT_HTML, TestUtils.EDIT_PDF)).thenReturn(true);
    when(vars.getSessionValue(TestUtils.RPT_TAXPAYMENT_ID)).thenReturn(TestUtils.SOME_ID);
    when(vars.getRequestGlobalVariable(TestUtils.INP_TYPE_VAT_REPORT, TestUtils.TYPE_VAT_REPORT_PARAM)).thenReturn(
        TestUtils.TYPE_01);

    try (MockedStatic<TaxPaymentData> taxPaymentDataMock = mockStatic(TaxPaymentData.class)) {
      TaxPaymentData dataMock = new TaxPaymentData();
      dataMock.datefrom = TestUtils.DATE_FROM;
      dataMock.dateto = TestUtils.DATE_TO;
      taxPaymentDataMock.when(() -> TaxPaymentData.select(any(), eq(TestUtils.CLEAN_ID))).thenReturn(
          new TaxPaymentData[]{ dataMock });

      doNothing().when(servlet).printReportJRRegisterByVat(any(HttpServletResponse.class),
          any(VariablesSecureApp.class), anyString(), anyString(), anyString());

      servlet.doPost(request, response);

      verify(vars).commandIn(TestUtils.EDIT_HTML, TestUtils.EDIT_PDF);
      verify(vars).getSessionValue(TestUtils.RPT_TAXPAYMENT_ID);
      verify(vars).getRequestGlobalVariable(TestUtils.INP_TYPE_VAT_REPORT, TestUtils.TYPE_VAT_REPORT_PARAM);
      verify(servlet).printReportJRRegisterByVat(eq(response), eq(vars), eq(TestUtils.DATE_FROM), eq(TestUtils.DATE_TO),
          eq(TestUtils.TYPE_01));
    }
  }

  /**
   * Tests getServletInfo.
   */
  @Test
  public void testGetServletInfoReturnsCorrectString() {
    String info = servlet.getServletInfo();
    assertEquals("Servlet ReportVatRegisterJR.", info);
  }
}
