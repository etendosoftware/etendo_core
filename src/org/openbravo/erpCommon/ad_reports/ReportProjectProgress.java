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
 * All portions are Copyright (C) 2008-2017 Openbravo SLU 
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.ValueListFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.erpCommon.utility.UtilityData;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportProjectProgress extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    // Use ReadOnly Connection Provider
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    String referenceId = UtilityData.getReferenceId(readOnlyCP, "ProjectStatus");
    String[] referenceValues = Utility.getReferenceValues(readOnlyCP, vars.getLanguage(),
        referenceId);
    ValueListFilter valueFilter = new ValueListFilter(referenceValues);
    if (vars.commandIn("DEFAULT")) {
      String strRefDate = vars.getGlobalVariable("inpRefDate", "ReportProjectProgress|RefDateFrom",
          DateTimeData.today(readOnlyCP));
      String strOlderFirst = vars.getStringParameter("inpOlderFirst");
      String strStartDateFrom = vars.getGlobalVariable("inpStartDateFrom",
          "ReportProjectProgress|StartDateFrom", "");
      String strStartDateTo = vars.getGlobalVariable("inpStartDateTo",
          "ReportProjectProgress|StartDateTo", "");
      String strContractDateFrom = vars.getGlobalVariable("inpContractDateFrom",
          "ReportProjectProgress|ContractDateFrom", "");
      String strContractDateTo = vars.getGlobalVariable("inpContractDateTo",
          "ReportProjectProgress|ContractDateTo", "");
      String strEndingDateFrom = vars.getGlobalVariable("inpEndingDateFrom",
          "ReportProjectProgress|EndingDateFrom", "");
      String strEndingDateTo = vars.getGlobalVariable("inpEndingDateTo",
          "ReportProjectProgress|EndingDateTo", "");
      String strProject = vars.getGlobalVariable("inpcProjectId", "ReportProjectProgress|Project",
          "");
      String strProjectStatus = vars.getInGlobalVariable("inpProjectstatus",
          "ReportProjectProgress|Projectstatus", "", valueFilter);
      String strBPartner = vars.getGlobalVariable("inpcBPartnerId", "ReportProjectProgress|Partner",
          "");
      String strResponsible = vars.getGlobalVariable("inpResponsible",
          "ReportProjectProgress|Responsible", "");

      printPageDataSheet(response, vars, strRefDate, strOlderFirst, strStartDateFrom,
          strStartDateTo, strContractDateFrom, strContractDateTo, strEndingDateFrom,
          strEndingDateTo, strProject, strProjectStatus, strBPartner, strResponsible);

    } else if (vars.commandIn("HTML") || vars.commandIn("PDF")) {
      String strRefDate = vars.getRequestGlobalVariable("inpRefDate",
          "ReportProjectProgress|RefDateFrom");
      String strOlderFirst = vars.getRequestGlobalVariable("inpOlderFirst",
          "ReportProjectProgress|OlderFirst");
      String strStartDateFrom = vars.getRequestGlobalVariable("inpStartDateFrom",
          "ReportProjectProgress|StartDateFrom");
      String strStartDateTo = vars.getRequestGlobalVariable("inpStartDateTo",
          "ReportProjectProgress|StartDateTo");
      String strContractDateFrom = vars.getRequestGlobalVariable("inpContractDateFrom",
          "ReportProjectProgress|ContractDateFrom");
      String strContractDateTo = vars.getRequestGlobalVariable("inpContractDateTo",
          "ReportProjectProgress|ContractDateTo");
      String strEndingDateFrom = vars.getRequestGlobalVariable("inpEndingDateFrom",
          "ReportProjectProgress|EndingDateFrom");
      String strEndingDateTo = vars.getRequestGlobalVariable("inpEndingDateTo",
          "ReportProjectProgress|EndingDateTo");
      String strProject = vars.getRequestGlobalVariable("inpcProjectId",
          "ReportProjectProgress|Project");
      String strProjectStatus = vars.getRequestInGlobalVariable("inpProjectstatus",
          "ReportProjectProgress|Projectstatus", valueFilter);
      String strBPartner = vars.getRequestGlobalVariable("inpcBPartnerId",
          "ReportProjectProgress|Partner");
      String strResponsible = vars.getRequestGlobalVariable("inpResponsible",
          "ReportProjectProgress|Responsible");

      String strOutput = "html";
      if (vars.commandIn("PDF")) {
        strOutput = "pdf";
      }

      try {
        printPageDataHtml(response, vars, strRefDate, strOlderFirst, strStartDateFrom,
            strStartDateTo, strContractDateFrom, strContractDateTo, strEndingDateFrom,
            strEndingDateTo, strProject, strProjectStatus, strBPartner, strResponsible, strOutput);
      } catch (ParseException e) {
        e.printStackTrace();
      }

    } else {
      pageError(response);
    }
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strRefDate, String strOlderFirst, String strStartDateFrom, String strStartDateTo,
      String strContractDateFrom, String strContractDateTo, String strEndingDateFrom,
      String strEndingDateTo, String strProject, String strProjectStatus, String strBPartner,
      String strResponsible) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();

    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportProjectProgress")
        .createXmlDocument();

    // Use ReadOnly Connection Provider
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(), "ReportProjectProgress", false,
        "", "", "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
          "org.openbravo.erpCommon.ad_reports.ReportProjectProgress");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(),
          "ReportProjectProgress.html", classInfo.id, classInfo.type, strReplaceWith,
          tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(),
          "ReportProjectProgress.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    {
      OBError myMessage = vars.getMessage("ReportProjectProgress");
      vars.removeMessage("ReportProjectProgress");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("direction", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");

    // Reference Date
    xmlDocument.setParameter("refDate", strRefDate);
    xmlDocument.setParameter("datedisplayFormatRef", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("datesaveFormatRef", vars.getSessionValue("#AD_SqlDateFormat"));

    // Project Starting Date
    xmlDocument.setParameter("startDateFrom", strStartDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormatStart",
        vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormatStart", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("startDateTo", strStartDateTo);
    xmlDocument.setParameter("dateTodisplayFormatStart", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormatStart", vars.getSessionValue("#AD_SqlDateFormat"));

    // Project Contract Date
    xmlDocument.setParameter("contractDateFrom", strContractDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormatContract",
        vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormatContract",
        vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("contractDateTo", strContractDateTo);
    xmlDocument.setParameter("dateTodisplayFormatContract",
        vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormatContract", vars.getSessionValue("#AD_SqlDateFormat"));

    // Project Ending Date
    xmlDocument.setParameter("endingDateFrom", strEndingDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormatEnding",
        vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormatEnding", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("endingDateTo", strEndingDateTo);
    xmlDocument.setParameter("dateTodisplayFormatEnding",
        vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormatEnding", vars.getSessionValue("#AD_SqlDateFormat"));

    xmlDocument.setParameter("paramOlderFirst", strOlderFirst);
    xmlDocument.setParameter("project", strProject);
    xmlDocument.setParameter("cProjectStatus", strProjectStatus);
    xmlDocument.setParameter("partnerid", strBPartner);
    xmlDocument.setParameter("responsible", strResponsible);

    // Project selector
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_Project_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportProjectProgress"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportProjectProgress"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportProjectProgress",
          strProject);
      xmlDocument.setData("reportC_Project_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    // Project Status multiple selector
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "LIST",
          "C_Project_status", "ProjectStatus", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportProjectProgress"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportProjectProgress"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportProjectProgress",
          strProjectStatus);
      xmlDocument.setData("reportC_PROJECTSTATUS", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    // Person in Charge drop-down list
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLE",
          "Responsible_ID", "Responsible employee", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportProjectProgress"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportProjectProgress"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportProjectProgress",
          strResponsible);
      xmlDocument.setData("reportResponsible", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    out.println(xmlDocument.print());
    out.close();
  }

  private void printPageDataHtml(HttpServletResponse response, VariablesSecureApp vars,
      String strRefDate, String strOlderFirst, String strStartDateFrom, String strStartDateTo,
      String strContractDateFrom, String strContractDateTo, String strEndingDateFrom,
      String strEndingDateTo, String strProject, String strProjectStatus, String strBPartner,
      String strResponsible, String strOutput)
      throws IOException, ServletException, ParseException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataHtml");
    }

    String discard[] = { "discard" };

    String strOlderPhasesTasksFirst = StringUtils.isEmpty(strOlderFirst)
        ? ", PRP.SEQNO DESC, PRT.SEQNO DESC"
        : ", PRP.SEQNO ASC, PRT.SEQNO ASC";

    // Use ReadOnly Connection Provider
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    ReportProjectProgressData[] data = ReportProjectProgressData.select(readOnlyCP,
        vars.getLanguage(),
        Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportProjectProgress"),
        Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportProjectProgress"),
        strStartDateFrom, DateTimeData.nDaysAfter(readOnlyCP, strStartDateTo, "1"),
        strContractDateFrom, DateTimeData.nDaysAfter(readOnlyCP, strContractDateTo, "1"),
        strEndingDateFrom, DateTimeData.nDaysAfter(readOnlyCP, strEndingDateTo, "1"), strProject,
        strProjectStatus, strBPartner, strResponsible, strOlderPhasesTasksFirst);

    String strReferenceDate = StringUtils.isEmpty(strRefDate) ? DateTimeData.today(readOnlyCP)
        : strRefDate;

    if (data == null || data.length == 0) {
      data = ReportProjectProgressData.set();
      discard[0] = "discardAll";
    } else {
      // Calculation of some values of the Project Progress report
      DateFormat DateFormatter = Utility.getDateFormatter(vars);
      String strProjectId = "";
      String strDaysElapsed = "";
      String strProjectContractDuration = "";
      String strProjectDaysDelayed = "";
      String strTimeBurned = "";
      String strCompletionPerc = "";
      String strCumDaysDelayed = "";
      String strPhaseId = "";
      String strPhaseContractDuration = "";
      String strPhaseDaysDelayed = "";
      String strTaskId = "";
      String strTaskContractDuration = "";
      String strTaskDaysDelayed = "";
      int dataStart = 0;
      for (int i = 0; data != null && i < data.length; i++) {

        // Values calculation for the project
        if (StringUtils.isNotEmpty(data[i].projectid)
            && !StringUtils.equals(data[i].projectid, strProjectId)) {
          strProjectId = data[i].projectid;

          // Reset phases and tasks values
          strPhaseContractDuration = "";
          strPhaseDaysDelayed = "";
          strTaskContractDuration = "";
          strTaskDaysDelayed = "";

          // Calculate the labor days elapsed from the project
          // starting date to the report reference date
          if (StringUtils.isNotEmpty(data[i].startingdate)) {
            if (Utility.isBiggerDate(strReferenceDate, data[i].startingdate, DateFormatter)) {
              data[i].dayselapsed = Utility.calculateLaborDays(data[i].startingdate,
                  strReferenceDate, DateFormatter);
            } else if (StringUtils.equals(data[i].startingdate, strReferenceDate)) {
              data[i].dayselapsed = "0";
            } else {
              data[i].dayselapsed = "";
            }
          } else {
            data[i].dayselapsed = "";
          }
          strDaysElapsed = data[i].dayselapsed;

          // Calculate the project contract duration in labor days
          // (from its starting date to its contract date)
          if (StringUtils.isNotEmpty(data[i].startingdate)
              && StringUtils.isNotEmpty(data[i].contractdate)) {
            if (Utility.isBiggerDate(data[i].contractdate, data[i].startingdate, DateFormatter)) {
              data[i].projectcontractduration = Utility.calculateLaborDays(data[i].startingdate,
                  data[i].contractdate, DateFormatter);
              Integer ProjectContractDuration = Integer.parseInt(data[i].projectcontractduration)
                  + 1;
              data[i].projectcontractduration = ProjectContractDuration.toString();
            } else if (StringUtils.equals(data[i].startingdate, data[i].contractdate)) {
              data[i].projectcontractduration = "1";
            } else {
              data[i].projectcontractduration = "";
            }
          } else {
            data[i].projectcontractduration = "";
          }
          strProjectContractDuration = data[i].projectcontractduration;

          // Calculate the time burned as the quotient of the days
          // elapsed and project contract duration
          if (StringUtils.isNotEmpty(strDaysElapsed) && !StringUtils.equals(strDaysElapsed, "0")
              && StringUtils.isNotEmpty(strProjectContractDuration)
              && !StringUtils.equals(strProjectContractDuration, "0")) {
            int decimalPlace = 2;
            BigDecimal daysElapsed = new BigDecimal(strDaysElapsed);
            BigDecimal contractDuration = new BigDecimal(strProjectContractDuration);
            BigDecimal timeBurned = ((daysElapsed.multiply(new BigDecimal("100")))
                .divide(contractDuration, 12, RoundingMode.HALF_EVEN)).setScale(decimalPlace,
                    RoundingMode.UP);
            data[i].timeburned = timeBurned.toPlainString();
          } else {
            data[i].timeburned = "";
          }
          strTimeBurned = data[i].timeburned;

          // Calculate the labor days delayed of the project (from its
          // contract date to its ending date)
          if (StringUtils.isNotEmpty(data[i].endingdate)
              && StringUtils.isNotEmpty(data[i].contractdate)) {
            if (Utility.isBiggerDate(data[i].endingdate, data[i].contractdate, DateFormatter)) {
              data[i].daysdelayed = Utility.calculateLaborDays(data[i].contractdate,
                  data[i].endingdate, DateFormatter);
            } else if (Utility.isBiggerDate(data[i].contractdate, data[i].endingdate,
                DateFormatter)) {
              data[i].daysdelayed = "-" + Utility.calculateLaborDays(data[i].endingdate,
                  data[i].contractdate, DateFormatter);
            } else if (StringUtils.equals(data[i].endingdate, data[i].contractdate)) {
              data[i].daysdelayed = "0";
            } else {
              data[i].daysdelayed = "";
            }
          } else {
            data[i].daysdelayed = "";
          }
          strProjectDaysDelayed = data[i].daysdelayed;

        } else {
          data[i].dayselapsed = strDaysElapsed;
          data[i].projectcontractduration = strProjectContractDuration;
          data[i].timeburned = strTimeBurned;
          data[i].daysdelayed = strProjectDaysDelayed;
        }

        // Values calculation for the phase
        if (StringUtils.isNotEmpty(data[i].phaseid)
            && !StringUtils.equals(data[i].phaseid, strPhaseId)) {
          strPhaseId = data[i].phaseid;

          // Reset tasks values
          strTaskContractDuration = "";
          strTaskDaysDelayed = "";

          // Calculates the phase contract duration in labor days
          // (from its starting date to its contract date)
          if (StringUtils.isNotEmpty(data[i].phasestartingdate)
              && StringUtils.isNotEmpty(data[i].phasecontractdate)) {
            if (Utility.isBiggerDate(data[i].phasecontractdate, data[i].phasestartingdate,
                DateFormatter)) {
              data[i].phasecontractduration = Utility.calculateLaborDays(data[i].phasestartingdate,
                  data[i].phasecontractdate, DateFormatter);
              Integer PhaseContractDuration = Integer.parseInt(data[i].phasecontractduration) + 1;
              data[i].phasecontractduration = PhaseContractDuration.toString();
            } else if (StringUtils.equals(data[i].phasestartingdate, data[i].phasecontractdate)) {
              data[i].phasecontractduration = "1";
            } else {
              data[i].phasecontractduration = "";
            }
          } else {
            data[i].phasecontractduration = "";
          }
          strPhaseContractDuration = data[i].phasecontractduration;

          // Calculates the labor days delayed of the phase (from its
          // contract date to its ending date)
          if (StringUtils.isNotEmpty(data[i].phaseendingdate)
              && StringUtils.isNotEmpty(data[i].phasecontractdate)) {
            if (Utility.isBiggerDate(data[i].phaseendingdate, data[i].phasecontractdate,
                DateFormatter)) {
              data[i].phasedaysdelayed = Utility.calculateLaborDays(data[i].phasecontractdate,
                  data[i].phaseendingdate, DateFormatter);
            } else if (Utility.isBiggerDate(data[i].phasecontractdate, data[i].phaseendingdate,
                DateFormatter)) {
              data[i].phasedaysdelayed = "-" + Utility.calculateLaborDays(data[i].phaseendingdate,
                  data[i].phasecontractdate, DateFormatter);
            } else if (StringUtils.equals(data[i].phaseendingdate, data[i].phasecontractdate)) {
              data[i].phasedaysdelayed = "0";
            } else {
              data[i].phasedaysdelayed = "";
            }
          } else {
            data[i].phasedaysdelayed = "";
          }
          strPhaseDaysDelayed = data[i].phasedaysdelayed;
        } else {
          data[i].phasecontractduration = strPhaseContractDuration;
          data[i].phasedaysdelayed = strPhaseDaysDelayed;
        }

        // Values calculation for the task
        if (StringUtils.isNotEmpty(data[i].taskid)
            && !StringUtils.equals(data[i].taskid, strTaskId)) {
          strTaskId = data[i].taskid;

          // Calculates the task contract duration in labor days (from
          // its starting date to its contract date)
          if (StringUtils.isNotEmpty(data[i].taskstartingdate)
              && StringUtils.isNotEmpty(data[i].taskcontractdate)) {
            if (Utility.isBiggerDate(data[i].taskcontractdate, data[i].taskstartingdate,
                DateFormatter)) {
              data[i].taskcontractduration = Utility.calculateLaborDays(data[i].taskstartingdate,
                  data[i].taskcontractdate, DateFormatter);
              Integer TaskContractDuration = Integer.parseInt(data[i].taskcontractduration) + 1;
              data[i].taskcontractduration = TaskContractDuration.toString();
            } else if (StringUtils.equals(data[i].taskstartingdate, data[i].taskcontractdate)) {
              data[i].taskcontractduration = "1";
            } else {
              data[i].taskcontractduration = "";
            }
          } else {
            data[i].taskcontractduration = "";
          }
          strTaskContractDuration = data[i].taskcontractduration;

          // Calculates the labor days delayed of the task (from its
          // contract date to its ending date)
          if (StringUtils.isNotEmpty(data[i].taskendingdate)
              && StringUtils.isNotEmpty(data[i].taskcontractdate)) {
            if (Utility.isBiggerDate(data[i].taskendingdate, data[i].taskcontractdate,
                DateFormatter)) {
              data[i].taskdaysdelayed = Utility.calculateLaborDays(data[i].taskcontractdate,
                  data[i].taskendingdate, DateFormatter);
            } else if (Utility.isBiggerDate(data[i].taskcontractdate, data[i].taskendingdate,
                DateFormatter)) {
              data[i].taskdaysdelayed = "-" + Utility.calculateLaborDays(data[i].taskendingdate,
                  data[i].taskcontractdate, DateFormatter);
            } else if (StringUtils.equals(data[i].taskendingdate, data[i].taskcontractdate)) {
              data[i].taskdaysdelayed = "0";
            } else {
              data[i].taskdaysdelayed = "";
            }
          } else {
            data[i].taskdaysdelayed = "";
          }
          strTaskDaysDelayed = data[i].taskdaysdelayed;
        } else {
          data[i].taskcontractduration = strTaskContractDuration;
          data[i].taskdaysdelayed = strTaskDaysDelayed;
        }

        // Calculation of completion percentage and cumulative days
        // delays of the project
        if ((i == data.length - 1)
            || !StringUtils.equals(data[i].projectid, data[i + 1].projectid)) {
          BigDecimal completionPerc = BigDecimal.ZERO;
          Integer totalContractDuration = 0;
          Integer completedContractDuration = 0;
          Integer totalItems = 0;
          Integer completedItems = 0;
          int itemsWithoutContractDuration = 0;
          boolean noCompletionPerc = false;
          Integer CumDaysDelayed = 0;

          for (int j = dataStart; j < i + 1; j++) {
            if (StringUtils.isNotEmpty(data[j].taskid)) {
              // Calculation of the number of contract days and
              // the number of the completed contract days
              if (StringUtils.isNotEmpty(data[j].taskcontractduration)
                  && itemsWithoutContractDuration == 0) {
                totalContractDuration = totalContractDuration
                    + Integer.parseInt(data[j].taskcontractduration);
                if (StringUtils.equals(data[j].taskcomp, "Y")) {
                  completedContractDuration = completedContractDuration
                      + Integer.parseInt(data[j].taskcontractduration);
                }
              } else {
                itemsWithoutContractDuration++;
              }
              totalItems++;
              if (StringUtils.equals(data[j].taskcomp, "Y")) {
                completedItems++;
              }

              // Calculation of the cumulative days delayed
              if (StringUtils.isNotEmpty(data[j].taskdaysdelayed)) {
                CumDaysDelayed = CumDaysDelayed + Integer.parseInt(data[j].taskdaysdelayed);
              }

            } else if (StringUtils.isNotEmpty(data[j].phaseid)) { // For phases
              // without tasks
              // Calculation of the number of contract days and
              // the number of the completed contract days
              if (StringUtils.isNotEmpty(data[j].phasecontractduration)
                  && itemsWithoutContractDuration == 0) {
                totalContractDuration = totalContractDuration
                    + Integer.parseInt(data[j].phasecontractduration);
                if (StringUtils.equals(data[j].phasecomp, "Y")) {
                  completedContractDuration = completedContractDuration
                      + Integer.parseInt(data[j].phasecontractduration);
                }
              } else {
                itemsWithoutContractDuration++;
              }
              totalItems++;
              if (StringUtils.equals(data[j].phasecomp, "Y")) {
                completedItems++;
              }

              // Calculation of the cumulative days delayed
              if (StringUtils.isNotEmpty(data[j].phasedaysdelayed)) {
                CumDaysDelayed = CumDaysDelayed + Integer.parseInt(data[j].phasedaysdelayed);
              }

            } else { // For projects without phases nor tasks
              // There is no completion percentage
              noCompletionPerc = true;

              // The cumulative days delayed are equal to the
              // project's days delayed
              if (StringUtils.isNotEmpty(data[j].cumdaysdelayed)) {
                CumDaysDelayed = Integer.parseInt(data[j].cumdaysdelayed);
              }
            }
          }

          // Calculate the Completion Percentage
          if (noCompletionPerc) {
            strCompletionPerc = "";
          } else {
            if (itemsWithoutContractDuration == 0) {
              // Calculate the Completion Percentage as the
              // quotient between CompletedContractDuration and
              // TotalContractDuration
              completionPerc = (new BigDecimal(completedContractDuration)
                  .multiply(new BigDecimal("100"))).divide(new BigDecimal(totalContractDuration),
                      12, RoundingMode.HALF_EVEN);
            } else {
              // Calculate the Completion Percentage as the
              // quotient between CompletedItems and TotalItems
              completionPerc = (new BigDecimal(completedItems).multiply(new BigDecimal("100")))
                  .divide(new BigDecimal(totalItems), 12, RoundingMode.HALF_EVEN);
            }
            int decimalPlace = 2;
            strCompletionPerc = (completionPerc.setScale(decimalPlace, RoundingMode.UP))
                .toPlainString();
          }

          // Get the Cumulative Days Delayed
          strCumDaysDelayed = CumDaysDelayed.toString();

          for (int j = dataStart; j < i + 1; j++) {
            data[j].completionperc = strCompletionPerc;
            data[j].cumdaysdelayed = strCumDaysDelayed;
          }
          dataStart = i + 1;
        }
      }
    }

    String strReportName = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportProjectProgress.jrxml";

    if (StringUtils.equals(strOutput, "pdf")) {
      response.setHeader("Content-disposition", "inline; filename=ReportProjectProgress.pdf");
    }

    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("REFERENCE_DATE", strReferenceDate);

    renderJR(vars, response, strReportName, strOutput, parameters, data, null);

  }

  @Override
  public String getServletInfo() {
    return "Servlet ReportProjectProgress";
  } // end of getServletInfo() method

}
