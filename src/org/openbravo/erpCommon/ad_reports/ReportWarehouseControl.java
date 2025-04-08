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
package org.openbravo.erpCommon.ad_reports;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportWarehouseControl extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strDateFrom = vars.getGlobalVariable("inpDateFrom", "ReportWarehouseControl|DateFrom",
          "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "ReportWarehouseControl|DateTo", "");
      String strReferential = vars.getGlobalVariable("inpReferential",
          "ReportWarehouseControl|Referential", "");
      printPageDataSheet(response, vars, strDateFrom, strDateTo, strReferential);
    } else if (vars.commandIn("FIND")) {
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "ReportWarehouseControl|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
          "ReportWarehouseControl|DateTo");
      String strReferential = vars.getRequestGlobalVariable("inpReferential",
          "ReportWarehouseControl|Referential");
      printPageDataSheet(response, vars, strDateFrom, strDateTo, strReferential);
    } else {
      pageError(response);
    }
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strDateFrom, String strDateTo, String strReferential)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = null;

    ReportWarehouseControlData[] data = null;
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    int limit = 0;
    if (!vars.commandIn("DEFAULT")) {
      limit = Integer.parseInt(Utility.getPreference(vars, "ReportsLimit", ""));

      String pgLimit = null, oraLimit = null;
      if (StringUtils.equalsIgnoreCase(readOnlyCP.getRDBMS(), "ORACLE")) {
        oraLimit = String.valueOf(limit + 1);
      } else {
        pgLimit = String.valueOf(limit + 1);
      }

      data = ReportWarehouseControlData.select(readOnlyCP,
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportWarehouseControl"),
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportWarehouseControl"),
          strDateFrom, DateTimeData.nDaysAfter(readOnlyCP, strDateTo, "1"), strReferential, pgLimit,
          oraLimit);
    }

    if (data == null || data.length == 0 || vars.commandIn("DEFAULT")) {
      String discard[] = { "sectionDescription" };
      xmlDocument = xmlEngine
          .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportWarehouseControl", discard)
          .createXmlDocument();
      data = ReportWarehouseControlData.set();
      if (log4j.isDebugEnabled()) {
        log4j.debug("DEFAULT INPUT");
      }
    } else {
      String discard[] = { "discard" };
      xmlDocument = xmlEngine
          .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportWarehouseControl", discard)
          .createXmlDocument();
    }

    ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(), "ReportWarehouseControl", false,
        "", "", "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
          "org.openbravo.erpCommon.ad_reports.ReportWarehouseControl");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(),
          "ReportWarehouseControl.html", classInfo.id, classInfo.type, strReplaceWith,
          tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(),
          "ReportWarehouseControl.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportWarehouseControl");
      if (limit > 0 && data.length > limit) {
        myMessage = new OBError();
        myMessage.setType("Warning");
        myMessage.setTitle("");
        String msgbody = Utility.messageBD(readOnlyCP, "ReportsLimit", vars.getLanguage());
        msgbody = msgbody.replace("@limit@", String.valueOf(limit + 1));
        myMessage.setMessage(msgbody);
      }
      vars.removeMessage("ReportWarehouseControl");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("referential", strReferential);
    xmlDocument.setData("structure1", data);
    out.println(xmlDocument.print());
    out.close();
  }

  @Override
  public String getServletInfo() {
    return "Servlet ReportWarehouseControl. This Servlet was made by Jon Alegria";
  }
}
