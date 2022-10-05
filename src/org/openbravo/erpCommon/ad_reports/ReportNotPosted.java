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
 * All portions are Copyright (C) 2001-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.StringCollectionUtils;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportNotPosted extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static final String DEFAULT = "Default";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      printPageDataSheet(request, response, vars, "", "");
    } else if (vars.commandIn("FIND")) {
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom", "ReportNotPosted|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "ReportNotPosted|DateTo");
      printPageDataSheet(request, response, vars, strDateFrom, strDateTo);
    } else {
      pageError(response);
    }
  }

  private void printPageDataSheet(HttpServletRequest request, HttpServletResponse response, VariablesSecureApp vars,
      String strDateFrom, String strDateTo) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }


    boolean notValidDate = (!StringUtils.isEmpty(strDateFrom) && !isValidDate(strDateFrom) || !StringUtils.isEmpty(
        strDateTo) && !isValidDate(strDateTo));

    boolean dateFromNotBeforeTo = !StringUtils.isEmpty(strDateFrom) && !isDateFromBeforeDateTo(strDateFrom, strDateTo);

    if (notValidDate || dateFromNotBeforeTo) {
      ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
      bdError(request, response, "", Utility.messageBD(readOnlyCP, "JS5", vars.getLanguage()));
    } else {
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportNotPosted")
          .createXmlDocument();

      // Use ReadOnly Connection Provider
      ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
      String orgIds = StringCollectionUtils
          .commaSeparated(OBContext.getOBContext().getReadableOrganizations());

      ReportNotPostedData[] data = ReportNotPostedData.select(readOnlyCP, vars.getLanguage(),
          vars.getClient(), orgIds, strDateFrom, strDateTo);
      // }// DateTimeData.nDaysAfter

      ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(), "ReportNotPosted", false, "", "",
          "", false, "ad_reports", strReplaceWith, false, true);
      toolbar.prepareSimpleToolBarTemplate();

      xmlDocument.setParameter("toolbar", toolbar.toString());

      try {
        WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
            "org.openbravo.erpCommon.ad_reports.ReportNotPosted");
        xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
        xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
        xmlDocument.setParameter("childTabContainer", tabs.childTabs());
        xmlDocument.setParameter("theme", vars.getTheme());
        NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(), "ReportNotPosted.html",
            classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
        xmlDocument.setParameter("navigationBar", nav.toString());
        LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(), "ReportNotPosted.html",
            strReplaceWith);
        xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
      } catch (Exception ex) {
        throw new ServletException(ex);
      }
      {
        OBError myMessage = vars.getMessage("ReportNotPosted");
        vars.removeMessage("ReportNotPosted");
        if (myMessage != null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

      if (vars.commandIn("FIND") && data.length == 0) {
        // No data has been found. Show warning message.
        xmlDocument.setParameter("messageType", "WARNING");
        xmlDocument.setParameter("messageTitle",
            Utility.messageBD(readOnlyCP, "ProcessStatus-W", vars.getLanguage()));
        xmlDocument.setParameter("messageMessage",
            Utility.messageBD(readOnlyCP, "NoDataFound", vars.getLanguage()));
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
      xmlDocument.setData("structure1", data);
      out.println(xmlDocument.print());
      out.close();
    }
  }

  @Override
  public String getServletInfo() {
    return "Servlet ReportNotPosted. This Servlet was made by Juan Pablo Calvente";
  } // end of the getServletInfo() method

  public boolean isValidDate(String input) {
    try {
      String strDateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("dateFormat.java");
      DateFormat dateFormat = new SimpleDateFormat(strDateFormat);
      dateFormat.parse(input);
      return true;
    } catch (ParseException e) {
      return false;
    }
  }

  public boolean isDateFromBeforeDateTo(String strDateFrom, String strDateTo) {
    try {
      String strDateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("dateFormat.java");
      DateFormat dateFormat = new SimpleDateFormat(strDateFormat);
      Date dateFrom = dateFormat.parse(strDateFrom);
      Date dateTo = dateFormat.parse(strDateTo);
      return dateFrom.before(dateTo);
    } catch (ParseException e) {
      return false;
    }
  }

  @Override
  protected void bdError(HttpServletRequest request, HttpServletResponse response, String strTitle,
      String strMessage) throws IOException {
    final XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/base/secureApp/Error")
        .createXmlDocument();

    String myTheme;
    if (request != null) {
      myTheme = getSessionValue(request.getSession());
    } else {
      myTheme = DEFAULT;
    }

    xmlDocument.setParameter("theme", myTheme);
    xmlDocument.setParameter("ParamTitulo", strTitle);
    xmlDocument.setParameter("ParamTexto", strMessage);
    response.setContentType("text/html; charset=UTF-8");
    final PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private String getSessionValue(HttpSession session) {
    String auxStr;
    try {
      auxStr = (String) session.getAttribute("#Theme".toUpperCase());
      if (auxStr == null || auxStr.trim().equals("")) {
        auxStr = DEFAULT;
      }
    } catch (Exception e) {
      auxStr = DEFAULT;
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("Session attribute: " + "#Theme" + ":..." + auxStr);
    }
    return auxStr;
  }
}
