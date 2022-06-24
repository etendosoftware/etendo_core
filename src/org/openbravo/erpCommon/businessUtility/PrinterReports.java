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
 * All portions are Copyright (C) 2001-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.businessUtility;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.Sqlc;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.utils.FormatUtilities;
import org.openbravo.xmlEngine.XmlDocument;

public class PrinterReports extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT")) {
      String strDirectPrint = vars.getStringParameter("inpdirectprint", "N");
      String strPDFPath = vars.getStringParameter("inppdfpath");
      String strKeyColumnId = vars.getStringParameter("inpkeyColumnId");
      String strHiddenKey = vars.getStringParameter("inphiddenkey");
      String strButtonType = vars.getStringParameter("inpButtonType");
      if (strHiddenKey == null || "".equals(strHiddenKey)) {
        strHiddenKey = "inp" + Sqlc.TransformaNombreColumna(strKeyColumnId);
      }
      String strWindowId = vars.getStringParameter("inpwindowId");
      String inptabId = vars.getStringParameter("inpTabId");
      String strHiddenValue = vars.getGlobalVariable("inphiddenvalue",
          strWindowId + "|" + strKeyColumnId);
      String strIsDirectPDF = vars.getStringParameter("inpIsDirectPDF");
      Window window;
      try {
        OBContext.setAdminMode(true);
        window = OBDal.getInstance().get(Window.class, strWindowId);
      } finally {
        OBContext.restorePreviousMode();
      }
      String strIsDirectAttach = vars.getStringParameter("inpIsDirectAttach");
      if ("printButton".equals(strButtonType)) {
        String directAttachPref = isDirectAttach(OBContext.getOBContext(), window);
        if (strIsDirectPDF == null || "".equals(strIsDirectPDF)) {
          strIsDirectPDF = Preferences.NO.equals(directAttachPref) ? "true" : "false";
        }
        if (strIsDirectAttach == null || "".equals(strIsDirectAttach)) {
          strIsDirectAttach = Preferences.YES.equals(directAttachPref) ? "true" : "false";
        }
      } else {
        strIsDirectPDF = "false";
        strIsDirectAttach = "false";
      }
      printPage(response, vars, strDirectPrint, strPDFPath, strHiddenKey, strHiddenValue, inptabId,
          strIsDirectPDF, strIsDirectAttach);
    } else {
      pageError(response);
    }
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars,
      String strDirectPrint, String strPDFPath, String strHiddenKey, String strHiddenValue,
      String inptabId, String strIsDirectPDF, String strIsDirectAttach)
      throws IOException, ServletException {
    String localStrPDFPath = strPDFPath;
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    String[] discard = { "isPrintPreview" };
    if (strDirectPrint.equals("N")) {
      discard[0] = new String("isDirectPrint");
    }
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/businessUtility/PrinterReports", discard)
        .createXmlDocument();
    String mapping = "";
    if (localStrPDFPath.startsWith("..")) {
      localStrPDFPath = localStrPDFPath.substring(2);
      mapping = localStrPDFPath;
      localStrPDFPath = FormatUtilities.replace(PrinterReportsData.select(this, localStrPDFPath));
    } else {
      mapping = PrinterReportsData.selectMapping(this, localStrPDFPath);
    }

    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("pdfPath", mapping);
    xmlDocument.setParameter("directPrint", strDirectPrint);
    xmlDocument.setParameter("isDirectPDF", "isDirectPDF = " + strIsDirectPDF + ";\r\n");
    xmlDocument.setParameter("isDirectAttach", "isDirectAttach = " + strIsDirectAttach + ";\r\n");
    // if (strPDFPath.startsWith("..")) strPDFPath =
    // strPDFPath.substring(2);

    // String mapping =
    // FormatUtilities.replace(PrinterReportsData.select(this, localStrPDFPath));
    localStrPDFPath = FormatUtilities.replace(localStrPDFPath);
    vars.setSessionValue("inpTabID", inptabId);
    final String hiddenValue = quouteIds(strHiddenValue);
    vars.setSessionValue(localStrPDFPath + "." + strHiddenKey, "(" + hiddenValue + ")");
    if (!strHiddenValue.equals("")) {
      vars.setSessionValue(localStrPDFPath + "." + strHiddenKey, "(" + hiddenValue + ")");
    } else {
      vars.getRequestInGlobalVariable(strHiddenKey, localStrPDFPath + "." + strHiddenKey,
          IsIDFilter.instance);
    }

    // vars.getRequestInGlobalVariable(strHiddenKey + "_R", mapping + "." +
    // strHiddenKey + "_R");

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private String quouteIds(String idList) throws ServletException {
    final String[] ids = idList.split(",");
    final StringBuilder quoted = new StringBuilder();
    for (int i = 0; i < ids.length; i++) {
      if (!IsIDFilter.instance.accept(ids[i])) {
        log4j.error("Input: " + idList + " not accepted by filter: IsIDFilter");
        throw new ServletException("Input: " + idList + " is not an accepted input");
      }
      if (i > 0) {
        quoted.append(",");
      }
      quoted.append("'").append(ids[i]).append("'");
    }
    return quoted.toString();
  }

  private String isDirectAttach(OBContext context, Window window) {
    String preferenceValue;
    try {
      preferenceValue = Preferences.getPreferenceValue("AttachByDefault", true,
          context.getCurrentClient(), context.getCurrentOrganization(), context.getUser(),
          context.getRole(), window);
    } catch (PropertyException e) {
      return "";
    }
    return preferenceValue;
  }
}
