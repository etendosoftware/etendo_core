package org.openbravo.erpReports;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.secureApp.VariablesSecureApp;


/**
 * Tests for {@link RptC_Remittance} servlet.
 */
@RunWith(MockitoJUnitRunner.class)
public class RptC_RemittanceTest {

  @InjectMocks
  @Spy
  private RptC_Remittance servlet;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private VariablesSecureApp vars;

  /**
   * Tests {@code doPost} with command DEFAULT and empty session value.
   *
   * @throws IOException
   *     if an I/O error occurs
   * @throws ServletException
   *     if a servlet error occurs
   */
  @Test
  public void testDoPostCommandDefaultCallsPrintPagePDF() throws IOException, ServletException {
    doReturn(vars).when(servlet).createVars(request);
    when(vars.commandIn(TestUtils.DEFAULT)).thenReturn(true);
    when(vars.getSessionValue(TestUtils.RPT_C_REM_INPC_REM_ID_R)).thenReturn("");
    when(vars.getSessionValue(TestUtils.RPT_C_REM_INPC_REM_ID)).thenReturn(TestUtils.SOME_ID);
    when(vars.getLanguage()).thenReturn(TestUtils.SOME_LANGUAGE);

    doNothing().when(servlet).printPagePDF(any(HttpServletResponse.class), any(VariablesSecureApp.class), anyString(),
        anyString());

    servlet.doPost(request, response);

    verify(vars).commandIn(TestUtils.DEFAULT);
    verify(vars).getSessionValue(TestUtils.RPT_C_REM_INPC_REM_ID_R);
    verify(vars).getSessionValue(TestUtils.RPT_C_REM_INPC_REM_ID);
    verify(vars).getLanguage();
    verify(servlet).printPagePDF(eq(response), eq(vars), eq(TestUtils.SOME_ID), eq(TestUtils.SOME_LANGUAGE));
  }

  /**
   * Tests {@code doPost} with command DEFAULT and non-empty session value.
   *
   * @throws IOException
   *     if an I/O error occurs
   * @throws ServletException
   *     if a servlet error occurs
   */
  @Test
  public void testDoPostCommandDefaultWithNonEmptySessionValue() throws IOException, ServletException {
    doReturn(vars).when(servlet).createVars(request);
    when(vars.commandIn(TestUtils.DEFAULT)).thenReturn(true);
    when(vars.getSessionValue(TestUtils.RPT_C_REM_INPC_REM_ID_R)).thenReturn(TestUtils.SOME_ID);
    when(vars.getLanguage()).thenReturn(TestUtils.SOME_LANGUAGE);

    doNothing().when(servlet).printPagePDF(any(HttpServletResponse.class), any(VariablesSecureApp.class), anyString(),
        anyString());

    servlet.doPost(request, response);

    verify(vars).commandIn(TestUtils.DEFAULT);
    verify(vars).getSessionValue(TestUtils.RPT_C_REM_INPC_REM_ID_R);
    verify(vars).getLanguage();
    verify(servlet).printPagePDF(eq(response), eq(vars), eq(TestUtils.SOME_ID), eq(TestUtils.SOME_LANGUAGE));
  }

  /**
   * Tests {@code getServletInfo}.
   */
  @Test
  public void testGetServletInfoReturnsCorrectString() {
    String info = servlet.getServletInfo();
    assertEquals("Servlet that presents the RptCOrders seeker", info);
  }
}
