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
 * All portions are Copyright (C) 2001-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

public class SQLExecutor extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strSQL = vars.getGlobalVariable("inpSQL", "SQLExecutor|sql", "");
      SQLExecutor_Query[] data = null;
      String strRecordRange = Utility.getContext(this, vars, "#RecordRange", "SQLExecutor");
      int intRecordRange = (strRecordRange.equals("") ? 0 : Integer.parseInt(strRecordRange));
      String strInitRecord = vars.getSessionValue("SQLExecutor|initRecordNumber");
      int initRecordNumber = (strInitRecord.equals("") ? 0 : Integer.parseInt(strInitRecord));
      try {
        if (StringUtils.isNotBlank(strSQL) && !strSQL.toUpperCase().trim().startsWith("SELECT ")) {
          vars.removeSessionValue("SQLExecutor|sql");
          throw new ServletException("Invalid SQL statement");
        }
        data = SQLExecutor_Query.select(this, strSQL, initRecordNumber, intRecordRange);
      } catch (Exception ex) {
        showErrorMessage(vars, ex);
      }
      printPage(response, vars, strSQL, data, strInitRecord, initRecordNumber, intRecordRange);
    } else if (vars.commandIn("FIND")) {
      String strSQL = vars.getRequestGlobalVariable("inpSQL", "SQLExecutor|sql");
      SQLExecutor_Query[] data = null;
      String strRecordRange = Utility.getContext(this, vars, "#RecordRange", "SQLExecutor");
      int intRecordRange = (strRecordRange.equals("") ? 0 : Integer.parseInt(strRecordRange));
      vars.setSessionValue("SQLExecutor|initRecordNumber", "0");
      int initRecordNumber = 0;
      try {
        if (StringUtils.isNotBlank(strSQL) && !strSQL.toUpperCase().trim().startsWith("SELECT ")) {
          vars.removeSessionValue("SQLExecutor|sql");
          throw new ServletException("Invalid SQL statement");
        }
        data = SQLExecutor_Query.select(this, strSQL, initRecordNumber, intRecordRange);
      } catch (Exception ex) {
        showErrorMessage(vars, ex);
      }
      printPage(response, vars, strSQL, data, "0", initRecordNumber, intRecordRange);
    } else if (vars.commandIn("RELATION_XLS")) {
      String strSQL = vars.getGlobalVariable("inpSQL", "SQLExecutor|sql", "");
      vars.setSessionValue("SQLExecutor|sql", strSQL);
      SQLExecutor_Query[] data = null;
      String strRecordRange = Utility.getContext(this, vars, "#RecordRange", "SQLExecutor");
      int intRecordRange = (strRecordRange.equals("") ? 0 : Integer.parseInt(strRecordRange));
      vars.setSessionValue("SQLExecutor|initRecordNumber", "0");
      int initRecordNumber = 0;
      try {
        if (StringUtils.isNotBlank(strSQL) && !strSQL.toUpperCase().trim().startsWith("SELECT ")) {
          vars.removeSessionValue("SQLExecutor|sql");
          throw new ServletException("Invalid SQL statement");
        }
        data = SQLExecutor_Query.select(this, strSQL, initRecordNumber, intRecordRange);
        printExcel(response, vars, strSQL, data);
      } catch (Exception ex) {
        showErrorMessage(vars, ex);
      }
    } else if (vars.commandIn("FIRST_RELATION")) {
      vars.setSessionValue("SQLExecutor|initRecordNumber", "0");
      response.sendRedirect(strDireccion + request.getServletPath() + "?Command=DEFAULT");
    } else if (vars.commandIn("PREVIOUS_RELATION")) {
      String strInitRecord = vars.getSessionValue("SQLExecutor|initRecordNumber");
      String strRecordRange = Utility.getContext(this, vars, "#RecordRange", "SQLExecutor");
      int intRecordRange = strRecordRange.equals("") ? 0 : Integer.parseInt(strRecordRange);
      if (strInitRecord.equals("") || strInitRecord.equals("0")) {
        vars.setSessionValue("SQLExecutor|initRecordNumber", "0");
      } else {
        int initRecord = (strInitRecord.equals("") ? 0 : Integer.parseInt(strInitRecord));
        initRecord -= intRecordRange;
        strInitRecord = ((initRecord < 0) ? "0" : Integer.toString(initRecord));
        vars.setSessionValue("SQLExecutor|initRecordNumber", strInitRecord);
      }
      response.sendRedirect(strDireccion + request.getServletPath() + "?Command=DEFAULT");
    } else if (vars.commandIn("NEXT_RELATION")) {
      String strInitRecord = vars.getSessionValue("SQLExecutor|initRecordNumber");
      String strRecordRange = Utility.getContext(this, vars, "#RecordRange", "SQLExecutor");
      int intRecordRange = strRecordRange.equals("") ? 0 : Integer.parseInt(strRecordRange);
      int initRecord = (strInitRecord.equals("") ? 0 : Integer.parseInt(strInitRecord));
      if (initRecord == 0) {
        initRecord = 1;
      }
      initRecord += intRecordRange;
      strInitRecord = ((initRecord < 0) ? "0" : Integer.toString(initRecord));
      vars.setSessionValue("SQLExecutor|initRecordNumber", strInitRecord);
      response.sendRedirect(strDireccion + request.getServletPath() + "?Command=DEFAULT");
    } else if (vars.commandIn("LAST_RELATION")) {
      String strRecordRange = Utility.getContext(this, vars, "#RecordRange", "SQLExecutor");
      String strSQL = vars.getGlobalVariable("inpSQL", "SQLExecutor|sql", "");
      String strInitRecord = lastRange(vars, strSQL, strRecordRange);
      vars.setSessionValue("SQLExecutor|initRecordNumber", strInitRecord);
      response.sendRedirect(strDireccion + request.getServletPath() + "?Command=DEFAULT");
    } else {
      pageError(response);
    }
  }

  private void showErrorMessage(VariablesSecureApp vars, Exception ex) {
    OBError myMessage = new OBError();
    myMessage.setType("Error");
    myMessage.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
    myMessage.setMessage(Utility.messageBD(this, ex.getMessage(), vars.getLanguage()));
    vars.setMessage("SQLExecutor", myMessage);
  }

  private String lastRange(VariablesSecureApp vars, String strSQL, String strRecordRange) {
    SQLExecutor_Query[] data = null;
    int initRecord = 0;
    try {
      data = SQLExecutor_Query.select(this, strSQL);
    } catch (Exception ex) {
      return "0";
    }
    while (initRecord < data.length) {
      initRecord += Integer.parseInt(strRecordRange);
    }
    initRecord -= Integer.parseInt(strRecordRange);
    if (initRecord < 0) {
      initRecord = 0;
    }
    return Integer.toString(initRecord);

  }

  private void printExcel(HttpServletResponse response, VariablesSecureApp vars, String strSQL,
      SQLExecutor_Query[] data) throws IOException, ServletException {
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_forms/SQLExecutor_Excel")
        .createXmlDocument();
    SQLExecutorData[] dataHeader = null;
    StringBuffer dataBuffer = new StringBuffer();
    if (data != null && data.length != 0) {
      dataHeader = new SQLExecutorData[data[0].name.size()];
      for (int i = 0; i < data[0].name.size(); i++) {
        dataHeader[i] = new SQLExecutorData();
        dataHeader[i].header = data[0].name.elementAt(i);
      }
      for (int j = 0; j < data.length; j++) {
        dataBuffer.append("<tr>\n");
        for (int k = 0; k < data[0].name.size(); k++) {
          dataBuffer.append("<td>");
          dataBuffer.append(data[j].getField(Integer.toString(k)));
          dataBuffer.append("</td>\n");
        }
        dataBuffer.append("</tr>\n");
      }
    }
    xmlDocument.setParameter("data", dataBuffer.toString());
    xmlDocument.setData("structureHeader", dataHeader);

    response.setContentType("text/xls; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strSQL,
      SQLExecutor_Query[] data, String strInitRecord, int initRecordNumber, int intRecordRange)
      throws IOException, ServletException {
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_forms/SQLExecutor")
        .createXmlDocument();
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("sql", strSQL);

    SQLExecutorData[] dataHeader = null;
    StringBuffer dataBuffer = new StringBuffer();
    if (data != null && data.length != 0) {
      dataHeader = new SQLExecutorData[data[0].name.size()];
      for (int i = 0; i < data[0].name.size(); i++) {
        dataHeader[i] = new SQLExecutorData();
        dataHeader[i].header = data[0].name.elementAt(i);
      }
      for (int j = 0; j < data.length; j++) {
        int evenOdd = j % 2;
        dataBuffer.append("<tr class=\"TableDetailRow" + String.valueOf(evenOdd) + "\">\n");
        for (int k = 0; k < data[0].name.size(); k++) {
          dataBuffer.append("<td>");
          dataBuffer.append(data[j].getField(Integer.toString(k)));
          dataBuffer.append("</td>\n");
        }
        dataBuffer.append("</tr>\n");
      }
    }
    xmlDocument.setData("structureHeader", dataHeader);
    xmlDocument.setParameter("data", dataBuffer.toString());

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "SQLExecutor", false,
        "document.frmMain.inpKey", "myGrid", null, false, "ad_forms", strReplaceWith, false, true);
    toolbar.prepareQueryTemplate((initRecordNumber > 1),
        (data != null && data.length != 0 && data.length >= intRecordRange),
        vars.getSessionValue("#ShowTest", "N").equals("Y"));
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(this, vars, "org.openbravo.erpCommon.ad_forms.SQLExecutor");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "SQLExecutor.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "SQLExecutor.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("SQLExecutor");
      vars.removeMessage("SQLExecutor");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  @Override
  public String getServletInfo() {
    return "Servlet for the standard SQL execution";
  }
}
