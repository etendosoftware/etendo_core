package org.openbravo.erpCommon.utility.reporting.printing; //NOSONAR

import java.io.InputStream;
import java.io.OutputStream;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.common.hooks.PrintControllerHookManager;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.reporting.DocumentType;

@SuppressWarnings("java:S00120")
final class PrintControllerHookSupport {
  private static final String DOCUMENT_ID = "documentId";
  private static final String DOCUMENT_TYPE = "documentType";
  private static final String REPORT_INPUT_STREAM = "reportInputStream";
  private static final String REPORT_OUTPUT_STREAM = "reportOutputStream";

  private PrintControllerHookSupport() {
  }

  static void setPostHookParams(DocumentType documentType, JSONObject jsonParams, String documentId,
      InputStream reportInputStream, OutputStream reportOutputStream)
      throws PrintControllerHookManager.PrintControllerHookException {
    try {
      jsonParams.put(DOCUMENT_ID, documentId);
      jsonParams.put(DOCUMENT_TYPE, documentType);
      jsonParams.put(REPORT_INPUT_STREAM, reportInputStream);
      jsonParams.put(REPORT_OUTPUT_STREAM, reportOutputStream);
    } catch (JSONException exception) {
      throw new PrintControllerHookManager.PrintControllerHookException(exception.getMessage());
    }
  }

  static void setPreHookParams(DocumentType documentType, JSONObject jsonParams,
      String documentId) throws JSONException {
    jsonParams.put(DOCUMENT_ID, documentId);
    jsonParams.put(DOCUMENT_TYPE, documentType);
  }

  static void executePreProcessHooks(PrintControllerHookManager hookManager, JSONObject jsonParams)
      throws JSONException {
    try {
      hookManager.executeHooks(jsonParams, hookManager.getPreProcess());
    } catch (PrintControllerHookManager.PrintControllerHookException exception) {
      throw new OBException(String.format(OBMessageUtils.messageBD(
          PrintController.ERROR_PRINTING_DOCUMENT_KEY), PrintController.LIST_ITEM_TAG
          + exception.getMessage() + PrintController.CLOSE_LIST_ITEM_TAG));
    }
  }
}
