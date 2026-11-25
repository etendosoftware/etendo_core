package org.openbravo.erpCommon.utility.reporting.printing; //NOSONAR

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.reporting.DocumentType;

@SuppressWarnings("java:S00120")
final class PrintControllerRequestResolver {
  private PrintControllerRequestResolver() {
  }

  static RequestContext resolve(HttpServletRequest request, VariablesSecureApp vars)
      throws ServletException {
    String servletPath = request.getServletPath().toLowerCase();
    if (servletPath.indexOf("quotations") != -1) {
      return buildContext(vars, DocumentType.QUOTATION, "PRINTQUOTATIONS", ".inpcOrderId");
    }
    if (servletPath.indexOf("orders") != -1) {
      return buildContext(vars, DocumentType.SALESORDER, "PRINTORDERS", ".inpcOrderId");
    }
    if (servletPath.indexOf("invoices") != -1) {
      return buildContext(vars, DocumentType.SALESINVOICE, "PRINTINVOICES",
          ".inpcInvoiceId");
    }
    if (servletPath.indexOf("shipments") != -1) {
      return buildContext(vars, DocumentType.SHIPMENT, "PRINTSHIPMENTS", ".inpmInoutId");
    }
    if (servletPath.indexOf("payments") != -1) {
      return buildContext(vars, DocumentType.PAYMENT, "PRINTPAYMENTS", ".inpfinPaymentId");
    }
    return new RequestContext(DocumentType.UNKNOWN, null, null);
  }

  private static RequestContext buildContext(VariablesSecureApp vars, DocumentType documentType,
      String sessionValuePrefix, String parameterSuffix) {
    String documentId = vars.getSessionValue(sessionValuePrefix + parameterSuffix + "_R");
    if ("".equals(documentId)) {
      documentId = vars.getSessionValue(sessionValuePrefix + parameterSuffix);
    }
    return new RequestContext(documentType, sessionValuePrefix, documentId);
  }

  static final class RequestContext {
    final DocumentType documentType;
    final String sessionValuePrefix;
    final String documentId;

    RequestContext(DocumentType documentType, String sessionValuePrefix, String documentId) {
      this.documentType = documentType;
      this.sessionValuePrefix = sessionValuePrefix;
      this.documentId = documentId;
    }
  }
}
