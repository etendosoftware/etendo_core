/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2019-2020 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):
 ************************************************************************
 */

package org.openbravo.client.application.businesslogic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DbUtility;

/**
 * Generic base class to be extended to implement an import by uploading a file. The actual
 * processing happens in a subclass which needs to override two methods.
 * 
 * The resulting (error) messages can be returned as a file to download by the user.
 * 
 * @author mtaal
 */

public abstract class ProcessUploadedFile extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static Logger log = LogManager.getLogger();

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    final VariablesSecureApp vars = new VariablesSecureApp(request);
    post(vars, request, response);
  }

  public void post(VariablesSecureApp vars, HttpServletRequest request,
      HttpServletResponse response) {

    final String strParamValues = vars.getStringParameter("paramValues");
    JSONObject paramValues = null;
    try {
      paramValues = new JSONObject(strParamValues);
    } catch (JSONException e) {
      throw new OBException(OBMessageUtils.messageBD("ErrorUploadingFile"), e, true);
    }

    try {
      OBContext.setAdminMode(true);

      if ("upload".equals(paramValues.getString("command"))) {
        uploadFile(vars, request, response, paramValues);
      } else if ("download".equals(paramValues.getString("command"))) {
        readAndReturnResult(response, paramValues);
      } else {
        pageError(response);
      }
    } catch (Exception e) {
      log.error("Error uploading file", e);
      try {
        final Throwable ex = DbUtility
                .getUnderlyingSQLException(e.getCause() != null ? e.getCause() : e);
        final OBError errMsg = OBMessageUtils.translateError(ex.getMessage());
        printResponse(response, paramValues, new JSONObject(), errMsg.getMessage());
      } catch (Exception e2) {
        log.error("Error sending error message", e2);
        throw new OBException(
            OBMessageUtils.messageBD("ErrorUploadingFile") + " " + e2.getMessage(), e2, true);
      }
      throw new OBException(OBMessageUtils.messageBD("ErrorUploadingFile"), e, true);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  protected void uploadFile(VariablesSecureApp vars, HttpServletRequest request,
      HttpServletResponse response, JSONObject paramValues) throws Exception {

    File tempFile = null;
    try {
      final FileItem fileItem = vars.getMultiFile("inpname");
      if (fileItem == null) {
        throw new ServletException("Empty file");
      }
      final String tmpFolder = System.getProperty("java.io.tmpdir");

      String strName = fileItem.getName();
      int i = strName.lastIndexOf(File.separator);
      if (i != -1) {
        strName = strName.substring(i + 1);
      }
      tempFile = new File(tmpFolder, strName);
      fileItem.write(tempFile);
      final UploadResult uploadResult = processFile(tempFile, paramValues);
      final String importErrorHandling = paramValues.getString("importErrorHandling");
      if (uploadResult.getErrorCount() > 0 && importErrorHandling.equals("stop_at_error")) {
        OBDal.getInstance().rollbackAndClose();
      } else {
        OBDal.getInstance().commitAndClose();
      }

      JSONObject obj = new JSONObject();
      obj.put("msg", uploadResult.getResultMessage());
      final String resultFileName = uploadResult.writeToFile(fileItem.getName());
      obj.put("fileName", resultFileName);
      printResponse(response, paramValues, obj, null);
    } finally {
      if (tempFile != null && tempFile.exists()) {
        tempFile.delete();
      }
    }
  }

  private void printResponse(HttpServletResponse response, JSONObject paramValues, JSONObject obj,
      String strMessage) throws Exception {
    response.setContentType("text/html; charset=UTF-8");
    Writer writer = response.getWriter();
    writer.write("<HTML><BODY><script type=\"text/javascript\">");
    writer.write("var iscWindow = top.OB || parent.OB;\n");
    if (obj != null) {
      final String buttonId = paramValues.getString("buttonID");
      writer.write(
          "iscWindow.Utilities.uploadFinished(\"" + buttonId + "\"," + obj.toString() + ");");
    }
    if (StringUtils.isNotBlank(strMessage)) {
      final String viewId = paramValues.getString("viewID");
      writer.write(
          "iscWindow.Utilities.writeErrorMessage(\"" + viewId + "\",\"" + strMessage + "\");");
    }
    writer.write("</SCRIPT></BODY></HTML>");
  }

  protected void readAndReturnResult(HttpServletResponse response, JSONObject paramValues)
      throws Exception {
    final String fileName = paramValues.getString("fileName");
    final String tmpFolder = System.getProperty("java.io.tmpdir");

    response.setContentType("text/plain");
    response.setHeader("Content-disposition", "attachment; filename=" + fileName);
    Writer w = response.getWriter();
    try {
      try (BufferedReader br = Files
          .newBufferedReader(Paths.get(tmpFolder + File.separator + fileName))) {
        String line;
        while ((line = br.readLine()) != null) {
          if (line.trim().length() == 0) {
            continue;
          }
          w.write(line + "\n");
          w.flush();
        }
      }
    } finally {
      w.close();
    }
  }

  protected UploadResult processFile(File file, JSONObject paramValues) throws Exception {

    final String importMode = paramValues.getString("importMode");

    if (importMode.equals("replace_import")) {
      clearBeforeImport(paramValues.getString("inpOwnerId"), paramValues);
    }

    return doProcessFile(paramValues, file);
  }

  /**
   * This method is called when the parameter import mode is set to import and replace. In that case
   * the current content should be removed.
   * 
   * The ownerId is the id of the parent record.
   */
  protected abstract void clearBeforeImport(String ownerId, JSONObject paramValues);

  /**
   * Is called to process the content of the uploaded file. The results should be returned in the
   * uploadResult object.
   */
  protected abstract UploadResult doProcessFile(JSONObject paramValues, File file) throws Exception;

  public static class UploadResult {
    private int lineCount = 0;
    private int errorCount = 0;
    private StringBuilder errorMessages = new StringBuilder();

    public int getLineCount() {
      return lineCount;
    }

    public int getErrorCount() {
      return errorCount;
    }

    public void addErrorMessage(String msg) {
      errorMessages.append(msg + "\n");
    }

    public String getErrorMessages() {
      return errorMessages.toString();
    }

    public boolean areThereErrors() {
      return errorMessages.length() > 0;
    }

    public void incTotalCount() {
      lineCount++;
      if (lineCount > 50000) {
        throw new OBException("Maximum number of lines (50000) in upload reached");
      }
    }

    public void incErrorCount() {
      errorCount++;
    }

    public String getResultMessage() {
      return OBMessageUtils.getI18NMessage("OBUIAPP_PROCESSEDOVERVIEW",
          new String[] { lineCount + "", errorCount + "" });
    }

    public String writeToFile(String fileName) throws Exception {
      final String resultFileName = "result_" + fileName;
      final String tmpFolder = System.getProperty("java.io.tmpdir");
      try (BufferedWriter writer = Files
          .newBufferedWriter(Paths.get(tmpFolder + File.separator + resultFileName))) {
        writer.write(getResultMessage() + "\n" + errorMessages);
      }
      return resultFileName;
    }
  }
}
