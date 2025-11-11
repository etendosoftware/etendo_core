package org.openbravo.erpReports;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.secureApp.VariablesSecureApp;

/**
 * Tests for {@link RptM_Requisition} servlet.
 */
@ExtendWith(MockitoExtension.class)
public class RptM_RequisitionTest {

  @InjectMocks
  @Spy
  private RptM_Requisition servlet;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private VariablesSecureApp vars;

  /**
   * Tests {@code doPost} with command DEFAULT.
   *
   * @throws IOException
   *     if an input or output error occurs
   * @throws ServletException
   *     if a servlet error occurs
   */
  @Test
  public void testDoPostCommandDefaultCallsPrintPagePartePDF() throws IOException, ServletException {
    doReturn(vars).when(servlet).createVars(request);
    when(vars.commandIn(TestUtils.DEFAULT)).thenReturn(true);
    when(vars.getSessionValue(TestUtils.RPT_M_REQUISITION_INPM_REQUISITION_ID_R)).thenReturn(StringUtils.EMPTY);
    when(vars.getSessionValue(TestUtils.RPT_M_REQUISITION_INPM_REQUISITION_ID)).thenReturn(TestUtils.SOME_ID);

    doNothing().when(servlet).printPagePartePDF(any(HttpServletResponse.class), any(VariablesSecureApp.class),
        anyString());

    servlet.doPost(request, response);

    verify(vars).commandIn(TestUtils.DEFAULT);
    verify(vars).getSessionValue(TestUtils.RPT_M_REQUISITION_INPM_REQUISITION_ID_R);
    verify(vars).getSessionValue(TestUtils.RPT_M_REQUISITION_INPM_REQUISITION_ID);
    verify(servlet).printPagePartePDF(eq(response), eq(vars), eq(TestUtils.SOME_ID));
  }

  /**
   * Tests {@code getServletInfo}.
   */
  @Test
  public void testGetServletInfoReturnsCorrectString() {
    String info = servlet.getServletInfo();
    assertEquals("Servlet that presents the RptMRequisitions seeker", info);
  }
}
