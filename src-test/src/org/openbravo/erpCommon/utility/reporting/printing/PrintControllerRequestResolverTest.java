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
package org.openbravo.erpCommon.utility.reporting.printing; //NOSONAR

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

/**
 * Tests for {@link PrintControllerRequestResolver}.
 */
@SuppressWarnings({"java:S100", "java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class PrintControllerRequestResolverTest {

  private static final String PATH_INVOICES = "/PrintInvoices";

  // -------------------------------------------------------------------------
  // resolve — document type routing
  // -------------------------------------------------------------------------
  /**
   * Quotations servlet path resolves to QUOTATION document type.
   * @throws ServletException if the request cannot be resolved
   */
  @Test
  public void testResolve_quotationsPath_returnsQuotationContext() throws ServletException {
    PrintControllerRequestResolver.RequestContext ctx = resolve("/PrintQuotations", "doc1");
    assertEquals(DocumentType.QUOTATION, ctx.documentType);
    assertEquals("PRINTQUOTATIONS", ctx.sessionValuePrefix);
  }
  /**
   * Orders servlet path resolves to SALESORDER document type.
   * @throws ServletException if the request cannot be resolved
   */
  @Test
  public void testResolve_ordersPath_returnsSalesOrderContext() throws ServletException {
    PrintControllerRequestResolver.RequestContext ctx = resolve("/PrintOrders", "doc1");
    assertEquals(DocumentType.SALESORDER, ctx.documentType);
    assertEquals("PRINTORDERS", ctx.sessionValuePrefix);
  }
  /**
   * Invoices servlet path resolves to SALESINVOICE document type.
   * @throws ServletException if the request cannot be resolved
   */
  @Test
  public void testResolve_invoicesPath_returnsSalesInvoiceContext() throws ServletException {
    PrintControllerRequestResolver.RequestContext ctx = resolve(PATH_INVOICES, "doc1");
    assertEquals(DocumentType.SALESINVOICE, ctx.documentType);
    assertEquals("PRINTINVOICES", ctx.sessionValuePrefix);
  }
  /**
   * Shipments servlet path resolves to SHIPMENT document type.
   * @throws ServletException if the request cannot be resolved
   */
  @Test
  public void testResolve_shipmentsPath_returnsShipmentContext() throws ServletException {
    PrintControllerRequestResolver.RequestContext ctx = resolve("/PrintShipments", "doc1");
    assertEquals(DocumentType.SHIPMENT, ctx.documentType);
    assertEquals("PRINTSHIPMENTS", ctx.sessionValuePrefix);
  }
  /**
   * Payments servlet path resolves to PAYMENT document type.
   * @throws ServletException if the request cannot be resolved
   */
  @Test
  public void testResolve_paymentsPath_returnsPaymentContext() throws ServletException {
    PrintControllerRequestResolver.RequestContext ctx = resolve("/PrintPayments", "doc1");
    assertEquals(DocumentType.PAYMENT, ctx.documentType);
    assertEquals("PRINTPAYMENTS", ctx.sessionValuePrefix);
  }
  /**
   * Unknown servlet path resolves to UNKNOWN with null prefix and document ID.
   * @throws ServletException if the request cannot be resolved
   */
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
  /**
   * Path matching is case-insensitive.
   * @throws ServletException if the request cannot be resolved
   */
  @Test
  public void testResolve_pathMatchIsCaseInsensitive() throws ServletException {
    PrintControllerRequestResolver.RequestContext ctx = resolve("/PRINTINVOICES", "doc1");
    assertEquals(DocumentType.SALESINVOICE, ctx.documentType);
  }

  // -------------------------------------------------------------------------
  // buildContext — session key fallback (_R → plain)
  // -------------------------------------------------------------------------
  /**
   * When the _R session key has a value it is used as the document ID.
   * @throws ServletException if the request cannot be resolved
   */
  @Test
  public void testResolve_rSuffixKeyHasValue_usesRSuffixValue() throws ServletException {
    HttpServletRequest request = mockRequest(PATH_INVOICES);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getSessionValue("PRINTINVOICES.inpcInvoiceId_R")).thenReturn("invoice-R-id");

    PrintControllerRequestResolver.RequestContext ctx =
        PrintControllerRequestResolver.resolve(request, vars);

    assertEquals("invoice-R-id", ctx.documentId);
  }
  /**
   * When the _R key is empty the plain session key is used as fallback.
   * @throws ServletException if the request cannot be resolved
   */
  @Test
  public void testResolve_rSuffixKeyEmpty_fallsBackToPlainKey() throws ServletException {
    HttpServletRequest request = mockRequest(PATH_INVOICES);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getSessionValue("PRINTINVOICES.inpcInvoiceId_R")).thenReturn("");
    when(vars.getSessionValue("PRINTINVOICES.inpcInvoiceId")).thenReturn("invoice-plain-id");

    PrintControllerRequestResolver.RequestContext ctx =
        PrintControllerRequestResolver.resolve(request, vars);

    assertEquals("invoice-plain-id", ctx.documentId);
  }
  /**
   * The resolved document ID is propagated into the RequestContext.
   * @throws ServletException if the request cannot be resolved
   */
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
