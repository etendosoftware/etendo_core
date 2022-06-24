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
 * All portions are Copyright (C) 2001-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.businessUtility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.Map;

import javax.mail.internet.MimeUtility;
import javax.servlet.ServletConfig;
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
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.client.application.attachment.AttachImplementationManager;
import org.openbravo.client.application.attachment.AttachmentAH;
import org.openbravo.client.application.attachment.AttachmentUtils;
import org.openbravo.client.application.attachment.CoreAttachImplementation;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.ad.utility.AttachmentMethod;

public class TabAttachments extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static Logger log = LogManager.getLogger();

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    final VariablesSecureApp vars = new VariablesSecureApp(request);
    post(vars, request, response);
  }

  public void post(VariablesSecureApp vars, HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException {

    AttachImplementationManager aim = WeldUtils
        .getInstanceFromStaticBeanManager(AttachImplementationManager.class);
    ApplicationDictionaryCachedStructures adcs = WeldUtils
        .getInstanceFromStaticBeanManager(ApplicationDictionaryCachedStructures.class);
    if (vars.getCommand().startsWith("SAVE_NEW")) {
      File tempFile = null;
      String strMessage = "";
      JSONObject obj = null;
      Tab tab = null;
      String key = null;
      try {
        OBContext.setAdminMode(true);

        final String strParamValues = vars.getStringParameter("paramValues");
        JSONObject paramValues;
        paramValues = new JSONObject(strParamValues);
        final String strTab = paramValues.getString("inpTabId");
        tab = adcs.getTab(strTab);
        key = paramValues.getString("inpKey");
        if (!IsIDFilter.instance.accept(key)) {
          throw new OBSecurityException("Invalid key for attachment in tab " + tab + " ID:" + key);
        }

        final String strDocumentOrganization = paramValues.getString("inpDocumentOrg");
        final FileItem file = vars.getMultiFile("inpname");
        if (file == null) {
          throw new ServletException("Empty file");
        }
        final String tmpFolder = System.getProperty("java.io.tmpdir");

        String strName = file.getName();
        int i = strName.lastIndexOf(File.separator);
        if (i != -1) {
          strName = strName.substring(i + 1);
        }
        tempFile = new File(tmpFolder, strName);
        try {
          file.write(tempFile);
        } catch (Exception e) {
          log.error("Error creating temp file", e);
          throw new OBException(OBMessageUtils.messageBD("ErrorUploadingFile"), e);
        }

        AttachmentMethod attachMethod = AttachmentUtils.getAttachmentMethod();
        Map<String, String> requestParams = ParameterUtils.buildRequestMap(request);
        for (Parameter param : adcs.getMethodMetadataParameters(attachMethod.getId(), strTab)) {
          String value = null;
          if (param.isFixed()) {
            continue;

          }
          if (paramValues.isNull(param.getDBColumnName())) {
            continue;
          }

          value = paramValues.getString(param.getDBColumnName());

          requestParams.put(param.getId(), value);
        }

        aim.upload(requestParams, strTab, key, strDocumentOrganization, tempFile);
        obj = AttachmentAH.getAttachmentJSONObject(tab, key);
      } catch (JSONException e) {
        throw new OBException(OBMessageUtils.messageBD("ErrorUploadingFile"), e);
      } catch (OBException e) {
        OBDal.getInstance().rollbackAndClose();
        log.error("Error uploading the file", e);
        if (key != null) {
          obj = AttachmentAH.getAttachmentJSONObject(tab, key);
        }
        strMessage = e.getMessage();
      } finally {
        OBContext.restorePreviousMode();
        if (tempFile != null && tempFile.exists()) {
          // If tempFile still exists in attachments/tmp must be removed
          tempFile.delete();
        }
      }
      printResponse(response, vars, obj, strMessage);

    } else if (vars.commandIn("DOWNLOAD_FILE")) {
      final String strFileReference = vars.getStringParameter("attachmentId");
      ByteArrayOutputStream os = null;
      try {
        OBContext.setAdminMode(true);
        os = new ByteArrayOutputStream();
        aim.download(strFileReference, os);
        Attachment attachment = OBDal.getInstance().get(Attachment.class, strFileReference);

        if (StringUtils.isEmpty(attachment.getDataType())) {
          response.setContentType("application/txt");
        } else {
          response.setContentType(attachment.getDataType());
        }

        response.setCharacterEncoding("UTF-8");
        String userAgent = request.getHeader("user-agent");
        if (userAgent.contains("MSIE")) {
          response.setHeader("Content-Disposition", "attachment; filename=\""
              + URLEncoder.encode(attachment.getName().replace("\"", "\\\""), "utf-8") + "\"");
        } else {
          response.setHeader("Content-Disposition",
              "attachment; filename=\""
                  + MimeUtility.encodeWord(attachment.getName().replace("\"", "\\\""), "utf-8", "Q")
                  + "\"");
        }

        response.getOutputStream().write(os.toByteArray());
        response.getOutputStream().flush();
        response.getOutputStream().close();

      } catch (OBException e) {
        log.error("Error downloading file.", e);
        printResponse(response, vars, null, e.getMessage());

      } finally {
        if (os != null) {
          os.close();
        }
        OBContext.restorePreviousMode();
      }

    } else if (vars.getCommand().contains("DOWNLOAD_ALL")) {
      String tabId = vars.getStringParameter("tabId");
      String recordIds = vars.getStringParameter("recordIds");
      ByteArrayOutputStream os = null;
      try {
        os = new ByteArrayOutputStream();
        aim.downloadAll(tabId, recordIds, os);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=attachments.zip");
        response.getOutputStream().write(os.toByteArray());
        response.getOutputStream().flush();
        response.getOutputStream().close();
      } catch (OBException e) {
        log.error("Error downloading all files.", e);
        printResponse(response, vars, null, e.getMessage());

      } finally {
        if (os != null) {
          os.close();
        }
      }

    } else {
      pageError(response);
    }
  }

  private void printResponse(HttpServletResponse response, VariablesSecureApp vars, JSONObject obj,
      String strMessage) throws IOException {
    response.setContentType("text/html; charset=UTF-8");
    Writer writer = response.getWriter();
    writer.write("<HTML><BODY><script type=\"text/javascript\">");
    writer.write("var iscWindow = top.OB || parent.OB;\n");
    if (obj != null) {
      final String buttonId = vars.getStringParameter("buttonId");
      writer.write(
          "iscWindow.Utilities.uploadFinished(\"" + buttonId + "\"," + obj.toString() + ");");
    }
    if (StringUtils.isNotBlank(strMessage)) {
      final String viewId = vars.getStringParameter("viewId");
      writer.write(
          "iscWindow.Utilities.writeErrorMessage(\"" + viewId + "\",\"" + strMessage + "\");");
    }
    writer.write("</SCRIPT></BODY></HTML>");

  }

  /**
   * Provides the directory in which the attachment has to be stored. For example for tableId "259",
   * recordId "0F3A10E019754BACA5844387FB37B0D5", the file directory returned is
   * "259/0F3/A10/E01/975/4BA/CA5/844/387/FB3/7B0/D5". In case 'SaveAttachmentsOldWay' preference is
   * enabled then the file directory returned is "259-0F3A10E019754BACA5844387FB37B0D5"
   * 
   * @param tableID
   *          UUID of the table
   * 
   * @param recordID
   *          UUID of the record
   * 
   * @return file directory to save the attachment
   * @deprecated use {@link CoreAttachImplementation#getAttachmentDirectoryForNewAttachments}
   *             instead
   */
  @Deprecated
  public static String getAttachmentDirectoryForNewAttachments(String tableID, String recordID) {
    return CoreAttachImplementation.getAttachmentDirectoryForNewAttachments(tableID, recordID);
  }

  /**
   * Provides the directory in which the attachment is stored. For example for tableId "259",
   * recordId "0F3A10E019754BACA5844387FB37B0D5", and fileName "test.txt" the file directory
   * returned is "259/0F3/A10/E01/975/4BA/CA5/844/387/FB3/7B0/D5". In case 'SaveAttachmentsOldWay'
   * preference is enabled then the file directory returned is
   * "259-0F3A10E019754BACA5844387FB37B0D5"
   * 
   * @param tableID
   *          UUID of the table
   * 
   * @param recordID
   *          UUID of the record
   * 
   * @param fileName
   *          Name of the file
   * 
   * @return file directory in which the attachment is stored
   * @deprecated use {@link CoreAttachImplementation#getAttachmentDirectory} instead
   */
  @Deprecated
  public static String getAttachmentDirectory(String tableID, String recordID, String fileName) {
    return CoreAttachImplementation.getAttachmentDirectory(tableID, recordID, fileName);
  }

  /**
   * Provides the value to be saved in path field in c_file. The path field is used to get the
   * location of the attachment. For example 259/0F3/A10/E01/975/4BA/CA5/844/387/FB3/7B0/D5. This
   * path is relative to the attachments folder
   * 
   * @param fileDirectory
   *          the directory that is retrieved from getFileDirectory()
   * 
   * @return value to be saved in path in c_file
   * @deprecated use {@link CoreAttachImplementation#getPath} instead
   */
  @Deprecated
  public static String getPath(String fileDirectory) {
    return CoreAttachImplementation.getPath(fileDirectory);
  }

  /**
   * Splits the path name component so that the resulting path name is 3 characters long sub
   * directories. For example 12345 is splitted to 123/45
   * 
   * @param origname
   *          Original name
   * @return splitted name.
   * @deprecated use {@link CoreAttachImplementation#splitPath} instead
   */
  @Deprecated
  public static String splitPath(final String origname) {
    return CoreAttachImplementation.splitPath(origname);
  }
}
