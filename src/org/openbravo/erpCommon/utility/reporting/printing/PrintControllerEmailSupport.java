package org.openbravo.erpCommon.utility.reporting.printing; //NOSONAR

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.reporting.Report;

@SuppressWarnings("java:S00120")
final class PrintControllerEmailSupport {
  private PrintControllerEmailSupport() {
  }

  static void updateEnvironmentInfo(PocData[] pocData, HashMap<String, Boolean> checks) {
    if (pocData == null) {
      return;
    }
    final Map<String, PocData> customerMap = new HashMap<>();
    final Map<String, PocData> salesRepMap = new HashMap<>();
    int docCounter = 0;
    checks.put(PrintController.CHECK_MORE_THAN_ONE_DOCUMENT, false);
    for (final PocData documentData : pocData) {
      if (documentData == null) {
        continue;
      }
      docCounter++;
      if (documentData.contactEmail != null) {
        customerMap.putIfAbsent(documentData.contactEmail, documentData);
      }
      if (documentData.salesrepEmail != null) {
        salesRepMap.putIfAbsent(documentData.salesrepEmail, documentData);
      }
    }
    if (docCounter > 1) {
      checks.put(PrintController.CHECK_MORE_THAN_ONE_DOCUMENT, true);
    }
    boolean moreThanOneCustomer = customerMap.size() > 1;
    boolean moreThanOneSalesRep = salesRepMap.size() > 1;
    checks.put(PrintController.CHECK_MORE_THAN_ONE_CUSTOMER, Boolean.valueOf(moreThanOneCustomer));
    checks.put(PrintController.CHECK_MORE_THAN_ONE_SALES_REP, Boolean.valueOf(moreThanOneSalesRep));
  }

  static String[] getHiddenTags(PocData[] pocData, List<AttachContent> attachedContent,
      VariablesSecureApp vars, HashMap<String, Boolean> checks, int differentDocTypesCount) {
    if (pocData == null) {
      return new String[0];
    }
    final Map<String, PocData> customerMap = new HashMap<>();
    final Map<String, PocData> salesRepMap = new HashMap<>();
    for (final PocData documentData : pocData) {
      if (documentData == null) {
        continue;
      }
      if (documentData.contactEmail != null) {
        customerMap.putIfAbsent(documentData.contactEmail, documentData);
      }
      if (documentData.salesrepEmail != null) {
        salesRepMap.putIfAbsent(documentData.salesrepEmail, documentData);
      }
    }
    boolean moreThanOneCustomer = customerMap.size() > 1;
    boolean moreThanOneSalesRep = salesRepMap.size() > 1;
    checks.put(PrintController.CHECK_MORE_THAN_ONE_CUSTOMER, Boolean.valueOf(moreThanOneCustomer));
    checks.put(PrintController.CHECK_MORE_THAN_ONE_SALES_REP, Boolean.valueOf(moreThanOneSalesRep));

    String[] discard = selectDiscardTags(moreThanOneCustomer, moreThanOneSalesRep);

    if (differentDocTypesCount > 1) {
      return appendDiscard(discard, "discardSelect");
    }
    if (attachedContent == null && vars.getMultiFile("inpFile") == null) {
      return appendDiscard(discard, "view");
    }
    return discard;
  }

  static boolean hasSingleAttachmentDoc(Map<String, Report> reports) {
    return reports != null && reports.size() == 1;
  }

  private static String[] selectDiscardTags(boolean moreThanOneCustomer, boolean moreThanOneSalesRep) {
    if (moreThanOneCustomer && moreThanOneSalesRep) {
      return new String[] { "replyTo", "replyTo_bottomMargin" };
    } else if (moreThanOneCustomer) {
      return new String[] { "multSalesRep", "multSalesRepCount" };
    } else if (moreThanOneSalesRep) {
      return new String[] { "replyTo", "replyTo_bottomMargin", "multiCustomerFlag" };
    } else {
      return new String[] { "multipleCustomer", "multipleCustomer_bottomMargin", "multiCustomerFlag" };
    }
  }

  private static String[] appendDiscard(String[] discard, String value) {
    String[] discardAux = new String[discard.length + 1];
    System.arraycopy(discard, 0, discardAux, 0, discard.length);
    discardAux[discard.length] = value;
    return discardAux;
  }
}
