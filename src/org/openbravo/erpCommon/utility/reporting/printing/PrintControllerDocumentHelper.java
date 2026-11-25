package org.openbravo.erpCommon.utility.reporting.printing; //NOSONAR

import java.util.Collection;

import javax.servlet.ServletException;

import org.openbravo.erpCommon.utility.reporting.DocumentType;
import org.openbravo.erpCommon.utility.reporting.Report;

@SuppressWarnings("java:S00120")
final class PrintControllerDocumentHelper {
  private PrintControllerDocumentHelper() {
  }

  static String normalizeDocumentId(String strDocumentId) {
    return strDocumentId.replace("(", "").replace(")", "").replace("'", "");
  }

  static String sanitizeDocumentIdentifier(String strDocumentId) {
    StringBuilder sanitizedDocIdBuilder = new StringBuilder();
    for (int index = 0; index < strDocumentId.length(); index++) {
      char currentCharacter = strDocumentId.charAt(index);
      if (Character.isLetterOrDigit(currentCharacter) || currentCharacter == ','
          || currentCharacter == '-') {
        sanitizedDocIdBuilder.append(currentCharacter);
      }
    }
    return sanitizedDocIdBuilder.toString();
  }

  static String getCommaSeparatedString(String[] documentIds) {
    StringBuilder result = new StringBuilder("(");
    for (int index = 0; index < documentIds.length; index++) {
      if (index > 0) {
        result.append(',');
      }
      result.append("'").append(documentIds[index]).append("'");
    }
    result.append(')');
    return result.toString();
  }

  static String getFilenameForReports(Collection<Report> reports) {
    if (reports == null) {
      return "";
    }
    String filename = "";
    for (Report report : reports) {
      filename = report.getFilename();
    }
    return filename;
  }

  static String[] orderByDocumentNo(PrintController controller, DocumentType documentType,
      String[] documentIds) throws ServletException {
    String strTable = documentType.getTableName();
    StringBuilder strIds = new StringBuilder();
    strIds.append("'");
    for (int i = 0; i < documentIds.length; i++) {
      if (i > 0) {
        strIds.append("', '");
      }
      strIds.append(sanitizeDocumentIdentifier(documentIds[i]));
    }
    strIds.append("'");

    PrintControllerData[] printControllerData;
    if ("C_INVOICE".equals(strTable)) {
      printControllerData = PrintControllerData.selectInvoices(controller, strIds.toString());
    } else if ("C_ORDER".equals(strTable)) {
      printControllerData = PrintControllerData.selectOrders(controller, strIds.toString());
    } else if ("FIN_PAYMENT".equals(strTable)) {
      printControllerData = PrintControllerData.selectPayments(controller, strIds.toString());
    } else {
      return documentIds;
    }

    if (printControllerData == null) {
      return documentIds;
    }
    String[] documentIdsOrdered = new String[printControllerData.length];
    for (int i = 0; i < printControllerData.length; i++) {
      documentIdsOrdered[i] = printControllerData[i].getField("Id");
    }
    return documentIdsOrdered;
  }
}
