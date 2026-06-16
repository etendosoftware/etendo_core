/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2021–2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.utility.reporting.printing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.reporting.DocumentType;

@SuppressWarnings({ "java:S120" })
@RunWith(MockitoJUnitRunner.Silent.class)
public class PrintControllerRequestResolverTest {

  // -------------------------------------------------------------------------
  // resolve — document type routing
  // -------------------------------------------------------------------------

  @Test
  public void testResolve_quotationsPath_returnsQuotationContext() throws ServletException {
    PrintControllerRequestResolver.RequestContext ctx = resolve("/PrintQuotations", "doc1");
    assertEquals(DocumentType.QUOTATION, ctx.documentType);
    assertEquals("PRINTQUOTATIONS", ctx.sessionValuePrefix);
  }

  @Test
  public void testResolve_ordersPath_returnsSalesOrderContext() throws ServletException {
    PrintControllerRequestResolver.RequestContext ctx = resolve("/PrintOrders", "doc1");
    assertEquals(DocumentType.SALESORDER, ctx.documentType);
    assertEquals("PRINTORDERS", ctx.sessionValuePrefix);
  }

  @Test
  public void testResolve_invoicesPath_returnsSalesInvoiceContext() throws ServletException {
    PrintControllerRequestResolver.RequestContext ctx = resolve("/PrintInvoices", "doc1");
    assertEquals(DocumentType.SALESINVOICE, ctx.documentType);
    assertEquals("PRINTINVOICES", ctx.sessionValuePrefix);
  }

  @Test
  public void testResolve_shipmentsPath_returnsShipmentContext() throws ServletException {
    PrintControllerRequestResolver.RequestContext ctx = resolve("/PrintShipments", "doc1");
    assertEquals(DocumentType.SHIPMENT, ctx.documentType);
    assertEquals("PRINTSHIPMENTS", ctx.sessionValuePrefix);
  }

  @Test
  public void testResolve_paymentsPath_returnsPaymentContext() throws ServletException {
    PrintControllerRequestResolver.RequestContext ctx = resolve("/PrintPayments", "doc1");
    assertEquals(DocumentType.PAYMENT, ctx.documentType);
    assertEquals("PRINTPAYMENTS", ctx.sessionValuePrefix);
  }

  @Test
  public void testResolve_unknownPath_returnsUnknownContext() throws ServletException {
    HttpServletRequest request = mockRequest("/SomeOtherServlet");
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    PrintControllerRequestResolver.RequestContext ctx =
        PrintControllerRequestResolver.resolve(request, vars);

    assertEquals(DocumentType.UNKNOWN, ctx.documentType);
    assertNull(ctx.sessionValuePrefix);
    assertNull(ctx.documentId);
  }

  @Test
  public void testResolve_pathMatchIsCaseInsensitive() throws ServletException {
    PrintControllerRequestResolver.RequestContext ctx = resolve("/PRINTINVOICES", "doc1");
    assertEquals(DocumentType.SALESINVOICE, ctx.documentType);
  }

  // -------------------------------------------------------------------------
  // buildContext — session key fallback (_R → plain)
  // -------------------------------------------------------------------------

  @Test
  public void testResolve_rSuffixKeyHasValue_usesRSuffixValue() throws ServletException {
    HttpServletRequest request = mockRequest("/PrintInvoices");
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getSessionValue("PRINTINVOICES.inpcInvoiceId_R")).thenReturn("invoice-R-id");

    PrintControllerRequestResolver.RequestContext ctx =
        PrintControllerRequestResolver.resolve(request, vars);

    assertEquals("invoice-R-id", ctx.documentId);
  }

  @Test
  public void testResolve_rSuffixKeyEmpty_fallsBackToPlainKey() throws ServletException {
    HttpServletRequest request = mockRequest("/PrintInvoices");
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getSessionValue("PRINTINVOICES.inpcInvoiceId_R")).thenReturn("");
    when(vars.getSessionValue("PRINTINVOICES.inpcInvoiceId")).thenReturn("invoice-plain-id");

    PrintControllerRequestResolver.RequestContext ctx =
        PrintControllerRequestResolver.resolve(request, vars);

    assertEquals("invoice-plain-id", ctx.documentId);
  }

  @Test
  public void testResolve_documentIdPropagatedInContext() throws ServletException {
    PrintControllerRequestResolver.RequestContext ctx = resolve("/PrintOrders", "order-42");
    assertEquals("order-42", ctx.documentId);
  }

  // -------------------------------------------------------------------------
  // helpers
  // -------------------------------------------------------------------------

  private static HttpServletRequest mockRequest(String servletPath) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getServletPath()).thenReturn(servletPath);
    return request;
  }

  private static PrintControllerRequestResolver.RequestContext resolve(
      String servletPath, String documentId) throws ServletException {
    HttpServletRequest request = mockRequest(servletPath);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getSessionValue(org.mockito.ArgumentMatchers.endsWith("_R"))).thenReturn(documentId);
    return PrintControllerRequestResolver.resolve(request, vars);
  }
}
