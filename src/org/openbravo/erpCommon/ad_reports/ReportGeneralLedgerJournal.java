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
package org.openbravo.erpCommon.ad_reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesHistory;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.AccountingSchemaMiscData;
import org.openbravo.erpCommon.businessUtility.Tree;
import org.openbravo.erpCommon.businessUtility.TreeData;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBLedgerUtils;
import org.openbravo.erpCommon.utility.ReferencedLink;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.gl.GLJournal;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportGeneralLedgerJournal extends HttpSecureAppServlet {
  private static final String GL_JOURNAL_TABLE_ID = "224";

  private static final long serialVersionUID = 1L;

  /**
   * Keeps a comma-separated list of the accounting entries that has been shown, from the newest one
   * to the oldest one. Used for navigation purposes
   */
  private static final String PREVIOUS_ACCTENTRIES = "ReportGeneralLedgerJournal.previousAcctEntries";
  private static final String PREVIOUS_ACCTENTRIES_OLD = "ReportGeneralLedgerJournal.previousAcctEntriesOld";

  /**
   * Keeps a comma-separated list of the line's range that has been shown, from the newest one to
   * the oldest one. Used for navigation purposes
   */
  private static final String PREVIOUS_RANGE = "ReportGeneralLedgerJournal.previousRange";
  private static final String PREVIOUS_RANGE_OLD = "ReportGeneralLedgerJournal.previousRangeOld";
  private static final Map<String, Integer> userExecutions = new HashMap<String, Integer>();
  private static int reportExecutions = 0;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (log4j.isDebugEnabled()) {
      log4j.debug("Command: " + vars.getStringParameter("Command"));
    }

    if (vars.commandIn("DEFAULT")) {
      String strOrg = vars.getGlobalVariable("inpOrg", "ReportGeneralLedgerJournal|Org",
          vars.getOrg());
      String strcAcctSchemaId = vars.getSessionValue("ReportGeneralLedgerJournal|cAcctSchemaId");
      if (StringUtils.isEmpty(strcAcctSchemaId)) {
        strcAcctSchemaId = OBLedgerUtils.getOrgLedger(strOrg);
      }
      String strDateFrom = vars.getGlobalVariable("inpDateFrom",
          "ReportGeneralLedgerJournal|DateFrom", "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "ReportGeneralLedgerJournal|DateTo",
          "");
      String strDocument = vars.getGlobalVariable("inpDocument",
          "ReportGeneralLedgerJournal|Document", "");
      String strDocumentNo = vars.getGlobalVariable("inpDocumentNo",
          "ReportGeneralLedgerJournal|DocumentNo", "");
      String strShowClosing = vars.getGlobalVariable("inpShowClosing",
          "ReportGeneralLedgerJournal|ShowClosing", "Y");
      String strShowReg = vars.getGlobalVariable("inpShowReg", "ReportGeneralLedgerJournal|ShowReg",
          "Y");
      String strShowOpening = vars.getGlobalVariable("inpShowOpening",
          "ReportGeneralLedgerJournal|ShowOpening", "Y");
      String strShowRegular = vars.getGlobalVariable("inpShowRegular",
          "ReportGeneralLedgerJournal|ShowRegular", "Y");
      String strShowDivideUp = vars.getGlobalVariable("inpShowDivideUp",
          "ReportGeneralLedgerJournal|ShowDivideUp", "Y");
      String strRecord = vars.getGlobalVariable("inpRecord", "ReportGeneralLedgerJournal|Record",
          "");
      String strTable = vars.getGlobalVariable("inpTable", "ReportGeneralLedgerJournal|Table", "");
      log4j.debug("********DEFAULT***************  strShowClosing: " + strShowClosing);
      log4j.debug("********DEFAULT***************  strShowReg: " + strShowReg);
      log4j.debug("********DEFAULT***************  strShowOpening: " + strShowOpening);
      String initRecordNumberOld = vars
          .getSessionValue("ReportGeneralLedgerJournal.initRecordNumberOld", "0");
      if (StringUtils
          .equals(vars.getSessionValue("ReportGeneralLedgerJournal.initRecordNumber", "0"), "0")) {
        vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber", "0");
        vars.setSessionValue(PREVIOUS_ACCTENTRIES, "0");
        vars.setSessionValue(PREVIOUS_RANGE, "");
      } else if (!StringUtils.equals(initRecordNumberOld, "-1")) {
        vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber", initRecordNumberOld);
        vars.setSessionValue(PREVIOUS_ACCTENTRIES, vars.getSessionValue(PREVIOUS_ACCTENTRIES_OLD));
        vars.setSessionValue(PREVIOUS_RANGE, vars.getSessionValue(PREVIOUS_RANGE_OLD));
      }
      String strPageNo = vars.getGlobalVariable("inpPageNo", "ReportGeneralLedgerJournal|PageNo",
          "1");
      String strEntryNo = vars.getGlobalVariable("inpEntryNo", "ReportGeneralLedgerJournal|EntryNo",
          "1");
      String strShowDescription = vars.getGlobalVariable("inpShowDescription",
          "ReportGeneralLedgerJournal|ShowDescription", "");
      String strcelementvaluefrom = vars.getGlobalVariable("inpcElementValueIdFrom",
          "ReportGeneralLedgerJournal|C_ElementValue_IDFROM", "");
      String strcelementvalueto = vars.getGlobalVariable("inpcElementValueIdTo",
          "ReportGeneralLedgerJournal|C_ElementValue_IDTO", "");
      String strcelementvaluefromdes = "", strcelementvaluetodes = "";
      ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
      if (StringUtils.isNotEmpty(strcelementvaluefrom)) {
        strcelementvaluefromdes = ReportGeneralLedgerData.selectSubaccountDescription(readOnlyCP,
            strcelementvaluefrom);
      }
      if (StringUtils.isNotEmpty(strcelementvalueto)) {
        strcelementvaluetodes = ReportGeneralLedgerData.selectSubaccountDescription(readOnlyCP,
            strcelementvalueto);
      }
      strcelementvaluefromdes = (StringUtils.equals(strcelementvaluefromdes, "null")) ? ""
          : strcelementvaluefromdes;
      strcelementvaluetodes = (StringUtils.equals(strcelementvaluetodes, "null")) ? ""
          : strcelementvaluetodes;
      vars.setSessionValue("inpElementValueIdFrom_DES", strcelementvaluefromdes);
      vars.setSessionValue("inpElementValueIdTo_DES", strcelementvaluetodes);
      printPageDataSheet(response, vars, strDateFrom, strDateTo, strDocument, strDocumentNo, strOrg,
          strTable, strRecord, "", strcAcctSchemaId, strShowClosing, strShowReg, strShowOpening,
          strPageNo, strEntryNo, strShowDescription, strShowRegular, strShowDivideUp, "", "",
          strcelementvaluefrom, strcelementvalueto, strcelementvaluefromdes, strcelementvaluetodes);
    } else if (vars.commandIn("DIRECT")) {
      String strTable = vars.getGlobalVariable("inpTable", "ReportGeneralLedgerJournal|Table");
      String strRecord = vars.getGlobalVariable("inpRecord", "ReportGeneralLedgerJournal|Record");
      String strAccSchemas = vars.getGlobalVariable("inpAccSchemas",
          "ReportGeneralLedgerJournal|AccSchemas");
      String paramschemas = vars.getStringParameter("inpParamschemas");
      String strPosted = vars.getStringParameter("posted");
      if (strPosted == "") {
        if (paramschemas != "") {
          strAccSchemas = paramschemas;
        }
      }

      String[] accSchemas = strAccSchemas.split(",");
      String strcAcctSchemaId = accSchemas[0];
      String schemas = "";
      for (int i = 1; i < accSchemas.length; i++) {
        if (i + 1 == accSchemas.length) {
          schemas = schemas + accSchemas[i];
        } else {
          schemas = schemas + accSchemas[i] + ",";
        }
      }
      setHistoryCommand(request, "DIRECT");
      vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber", "0");
      printPageDataSheet(response, vars, "", "", "", "", "", strTable, strRecord, "",
          strcAcctSchemaId, "", "", "", "1", "1", "", "Y", "", schemas, strPosted, "", "", "", "");
    } else if (vars.commandIn("DIRECT2")) {
      String strFactAcctGroupId = vars.getGlobalVariable("inpFactAcctGroupId",
          "ReportGeneralLedgerJournal|FactAcctGroupId");
      String strOrg = vars.getGlobalVariable("inpOrg", "ReportGeneralLedger|Org", vars.getOrg());
      String strcAcctSchemaId = vars.getGlobalVariable("inpcAcctSchemaId",
          "ReportGeneralLedger|cAcctSchemaId");
      if (StringUtils.isEmpty(strcAcctSchemaId)) {
        strcAcctSchemaId = OBLedgerUtils.getOrgLedger(strOrg);
      }
      setHistoryCommand(request, "DIRECT2");
      vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber", "0");
      printPageDataSheet(response, vars, "", "", "", "", strOrg, "", "", strFactAcctGroupId,
          strcAcctSchemaId, "", "", "", "1", "1", "", "Y", "", "", "", "", "", "", "");
    } else if (vars.commandIn("FIND")) {
      try {
        OBError msg = checkReportUsage(vars);
        if (msg != null) {
          advise(request, response, msg.getType(), msg.getTitle(), msg.getMessage());
          return;
        }
        String strcAcctSchemaId = vars.getRequestGlobalVariable("inpcAcctSchemaId",
            "ReportGeneralLedger|cAcctSchemaId");
        vars.setSessionValue("ReportGeneralLedgerJournal|cAcctSchemaId", strcAcctSchemaId);
        String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
            "ReportGeneralLedgerJournal|DateFrom");
        String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
            "ReportGeneralLedgerJournal|DateTo");
        String strDocument = vars.getRequestGlobalVariable("inpDocument",
            "ReportGeneralLedgerJournal|Document");
        String strDocumentNo = vars.getRequestGlobalVariable("inpDocumentNo",
            "ReportGeneralLedgerJournal|DocumentNo");
        String strOrg = vars.getGlobalVariable("inpOrg", "ReportGeneralLedgerJournal|Org", "0");
        String strShowClosing = vars.getRequestGlobalVariable("inpShowClosing",
            "ReportGeneralLedgerJournal|ShowClosing");
        if (StringUtils.isEmpty(strShowClosing)) {
          vars.setSessionValue("ReportGeneralLedgerJournal|ShowClosing", "N");
        }
        String strShowDivideUp = vars.getRequestGlobalVariable("inpShowDivideUp",
            "ReportGeneralLedgerJournal|ShowDivideUp");
        if (StringUtils.isEmpty(strShowDivideUp)) {
          vars.setSessionValue("ReportGeneralLedgerJournal|ShowDivideUp", "N");
        }
        String strShowRegular = vars.getRequestGlobalVariable("inpShowRegular",
            "ReportGeneralLedgerJournal|ShowRegular");
        if (StringUtils.isEmpty(strShowRegular)) {
          vars.setSessionValue("ReportGeneralLedgerJournal|ShowRegular", "N");
        }
        String strShowReg = vars.getRequestGlobalVariable("inpShowReg",
            "ReportGeneralLedgerJournal|ShowReg");
        if (StringUtils.isEmpty(strShowReg)) {
          vars.setSessionValue("ReportGeneralLedgerJournal|ShowReg", "N");
        }
        String strShowOpening = vars.getRequestGlobalVariable("inpShowOpening",
            "ReportGeneralLedgerJournal|ShowOpening");
        if (StringUtils.isEmpty(strShowOpening)) {
          vars.setSessionValue("ReportGeneralLedgerJournal|ShowOpening", "N");
        }
        if (!(StringUtils.equals(strShowOpening, "Y")) && !(StringUtils.equals(strShowReg, "Y"))
            && !(StringUtils.equals(strShowRegular, "Y"))
            && !(StringUtils.equals(strShowClosing, "Y"))
            && !(StringUtils.equals(strShowDivideUp, "Y"))) {
          strShowRegular = "Y";
          vars.setSessionValue("ReportGeneralLedgerJournal|ShowRegular", "Y");
        }
        String strShowClosing1 = vars.getStringParameter("inpShowClosing");
        String strShowReg1 = vars.getStringParameter("inpShowReg");
        String strShowOpening1 = vars.getStringParameter("inpShowOpening");
        String strShowDivideUp1 = vars.getStringParameter("inpShowDivideUp");
        log4j.debug("********FIND***************  strShowClosing: " + strShowClosing);
        log4j.debug("********FIND***************  strShowReg: " + strShowReg);
        log4j.debug("********FIND***************  strShowOpening: " + strShowOpening);
        log4j.debug("********FIND***************  strShowDivideUp: " + strShowDivideUp);
        log4j.debug("********FIND***************  strShowClosing1: " + strShowClosing1);
        log4j.debug("********FIND***************  strShowReg1: " + strShowReg1);
        log4j.debug("********FIND***************  strShowOpening1: " + strShowOpening1);
        log4j.debug("********FIND***************  strShowDivideUp1: " + strShowDivideUp1);
        vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber", "0");
        vars.setSessionValue(PREVIOUS_ACCTENTRIES, "0");
        vars.setSessionValue(PREVIOUS_RANGE, "");
        setHistoryCommand(request, "DEFAULT");
        String strPageNo = vars.getRequestGlobalVariable("inpPageNo",
            "ReportGeneralLedgerJournal|PageNo");
        String strEntryNo = vars.getRequestGlobalVariable("inpEntryNo",
            "ReportGeneralLedgerJournal|EntryNo");
        String strShowDescription = vars.getRequestGlobalVariable("inpShowDescription",
            "ReportGeneralLedgerJournal|ShowDescription");
        if (StringUtils.isEmpty(strShowDescription)) {
          vars.setSessionValue("ReportGeneralLedgerJournal|ShowDescription", "N");
        }
        String strcelementvaluefrom = vars.getRequestGlobalVariable("inpcElementValueIdFrom",
            "ReportGeneralLedgerJournal|C_ElementValue_IDFROM");
        String strcelementvalueto = vars.getRequestGlobalVariable("inpcElementValueIdTo",
            "ReportGeneralLedgerJournal|C_ElementValue_IDTO");
        String strcelementvaluefromdes = "", strcelementvaluetodes = "";
        ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
        if (StringUtils.isNotEmpty(strcelementvaluefrom)) {
          strcelementvaluefromdes = ReportGeneralLedgerData.selectSubaccountDescription(readOnlyCP,
              strcelementvaluefrom);
        }
        if (StringUtils.isNotEmpty(strcelementvalueto)) {
          strcelementvaluetodes = ReportGeneralLedgerData.selectSubaccountDescription(readOnlyCP,
              strcelementvalueto);
        }
        vars.setSessionValue("inpElementValueIdFrom_DES", strcelementvaluefromdes);
        vars.setSessionValue("inpElementValueIdTo_DES", strcelementvaluetodes);

        printPageDataSheet(response, vars, strDateFrom, strDateTo, strDocument, strDocumentNo,
            strOrg, "", "", "", strcAcctSchemaId, strShowClosing, strShowReg, strShowOpening,
            strPageNo, strEntryNo, strShowDescription, strShowRegular, strShowDivideUp, "", "",
            strcelementvaluefrom, strcelementvalueto, strcelementvaluefromdes,
            strcelementvaluetodes);
      } finally {
        decreaseReportUsage();
      }
    } else if (vars.commandIn("PDF", "XLS")) {
      try {
        if (log4j.isDebugEnabled()) {
          log4j.debug("PDF");
        }
        OBError msg = checkReportUsage(vars);
        if (msg != null) {
          advisePopUp(request, response, msg.getType(), msg.getTitle(), msg.getMessage());
          return;
        }
        String strcAcctSchemaId = vars.getRequestGlobalVariable("inpcAcctSchemaId",
            "ReportGeneralLedger|cAcctSchemaId");
        String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
            "ReportGeneralLedgerJournal|DateFrom");
        String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
            "ReportGeneralLedgerJournal|DateTo");
        String strDocument = vars.getRequestGlobalVariable("inpDocument",
            "ReportGeneralLedgerJournal|Document");
        String strDocumentNo = vars.getRequestGlobalVariable("inpDocumentNo",
            "ReportGeneralLedgerJournal|DocumentNo");
        String strOrg = vars.getGlobalVariable("inpOrg", "ReportGeneralLedgerJournal|Org", "0");
        String strShowClosing = vars.getRequestGlobalVariable("inpShowClosing",
            "ReportGeneralLedgerJournal|ShowClosing");
        if (StringUtils.isEmpty(strShowClosing)) {
          vars.setSessionValue("ReportGeneralLedgerJournal|ShowClosing", "N");
        }
        String strShowRegular = vars.getRequestGlobalVariable("inpShowRegular",
            "ReportGeneralLedgerJournal|ShowRegular");
        if (StringUtils.isEmpty(strShowRegular)) {
          vars.setSessionValue("ReportGeneralLedgerJournal|ShowRegular", "N");
        }
        String strShowReg = vars.getRequestGlobalVariable("inpShowReg",
            "ReportGeneralLedgerJournal|ShowReg");
        if (StringUtils.isEmpty(strShowReg)) {
          vars.setSessionValue("ReportGeneralLedgerJournal|ShowReg", "N");
        }
        String strShowOpening = vars.getRequestGlobalVariable("inpShowOpening",
            "ReportGeneralLedgerJournal|ShowOpening");
        if (StringUtils.isEmpty(strShowOpening)) {
          vars.setSessionValue("ReportGeneralLedgerJournal|ShowOpening", "N");
        }
        String strShowDivideUp = vars.getRequestGlobalVariable("inpShowDivideUp",
            "ReportGeneralLedgerJournal|ShowDivideUp");
        if (StringUtils.isEmpty(strShowDivideUp)) {
          vars.setSessionValue("ReportGeneralLedgerJournal|ShowDivideUp", "N");
        }
        // In case all flags "Type" are deactivated, the "Regular" one is activated by default
        if (!(StringUtils.equals(strShowOpening, "Y")) && !(StringUtils.equals(strShowReg, "Y"))
            && !(StringUtils.equals(strShowRegular, "Y"))
            && !(StringUtils.equals(strShowClosing, "Y"))
            && !(StringUtils.equals(strShowDivideUp, "Y"))) {
          strShowRegular = "Y";
          vars.setSessionValue("ReportGeneralLedgerJournal|ShowRegular", "Y");
        }

        String strTable = vars.getStringParameter("inpTable");
        String strRecord = vars.getStringParameter("inpRecord");
        String strPageNo = vars.getGlobalVariable("inpPageNo", "ReportGeneralLedgerJournal|PageNo",
            "1");
        String strEntryNo = vars.getGlobalVariable("inpEntryNo",
            "ReportGeneralLedgerJournal|EntryNo", "1");
        String strShowDescription = vars.getRequestGlobalVariable("inpShowDescription",
            "ReportGeneralLedgerJournal|ShowDescription");
        if (StringUtils.isEmpty(strShowDescription)) {
          vars.setSessionValue("ReportGeneralLedgerJournal|ShowDescription", "N");
        }
        /*
         * Scenario 1: We will have FactAcctGroupId while the request redirect from
         * ReportGeneralLedger Report. Otherwise we don't need to use FactAcctGroupId for PDF or
         * Excel report. So we have to check the immediate history command has DIRECT2 (It means
         * previous request from ReportGeneralLedger Report) Scenario 2: If we print once in PDF, it
         * will reset the history of COMMAND with DEFAULT, so same record of redirect wont print
         * more than one time. It will consider as default in second time.Scenario 3: If user change
         * the filter criteria, however he has come from ReportGeneralLedger Report(DIRECT2) We
         * don't take strFactAcctGroupId(will take care of criteria from current screen)
         */
        String strFactAcctGroupId = "";
        if (StringUtils.isEmpty(strDateFrom) && StringUtils.isEmpty(strDocument)
            && StringUtils.equals(strOrg, "0") && StringUtils.isEmpty(strShowClosing)
            && StringUtils.isEmpty(strShowReg) && StringUtils.isEmpty(strShowOpening)
            && StringUtils.isEmpty(strRecord)) {

          int currentHistoryIndex = Integer
              .parseInt(new VariablesHistory(request).getCurrentHistoryIndex());
          String currentCommand = vars.getSessionValue("reqHistory.command" + currentHistoryIndex);
          if (StringUtils.equals(currentCommand, "DIRECT2")) {
            strFactAcctGroupId = vars.getGlobalVariable("inpFactAcctGroupId",
                "ReportGeneralLedgerJournal|FactAcctGroupId");
          }
        }

        setHistoryCommand(request, "DEFAULT");
        String strcelementvaluefrom = vars.getRequestGlobalVariable("inpcElementValueIdFrom",
            "ReportGeneralLedgerJournal|C_ElementValue_IDFROM");
        String strcelementvalueto = vars.getRequestGlobalVariable("inpcElementValueIdTo",
            "ReportGeneralLedgerJournal|C_ElementValue_IDTO");
        printPagePDF(request, response, vars, strDateFrom, strDateTo, strDocument, strDocumentNo,
            strOrg, strTable, strRecord, strFactAcctGroupId, strcAcctSchemaId, strShowClosing,
            strShowReg, strShowOpening, strPageNo, strEntryNo,
            StringUtils.equals(strShowDescription, "Y") ? "Y" : "", strShowRegular, strShowDivideUp,
            strcelementvaluefrom, strcelementvalueto);
      } finally {
        decreaseReportUsage();
      }
    } else if (vars.commandIn("PREVIOUS_RELATION")) {
      String strInitRecord = vars.getSessionValue("ReportGeneralLedgerJournal.initRecordNumber");
      String strPreviousRecordRange = vars.getSessionValue(PREVIOUS_RANGE);

      String[] previousRecord = strPreviousRecordRange.split(",");
      strPreviousRecordRange = previousRecord[0];
      int intRecordRange = StringUtils.isEmpty(strPreviousRecordRange) ? 0
          : Integer.parseInt(strPreviousRecordRange);
      strPreviousRecordRange = previousRecord[1];
      intRecordRange += StringUtils.isEmpty(strPreviousRecordRange) ? 0
          : Integer.parseInt(strPreviousRecordRange);

      // Remove parts of the previous range
      StringBuffer sb_previousRange = new StringBuffer();
      for (int i = 2; i < previousRecord.length; i++) {
        sb_previousRange.append(previousRecord[i] + ",");
      }
      vars.setSessionValue(PREVIOUS_RANGE, sb_previousRange.toString());

      // Remove parts of the previous accounting entries
      String[] previousAcctEntries = vars.getSessionValue(PREVIOUS_ACCTENTRIES).split(",");
      StringBuffer sb_previousAcctEntries = new StringBuffer();
      for (int i = 2; i < previousAcctEntries.length; i++) {
        sb_previousAcctEntries.append(previousAcctEntries[i] + ",");
      }

      if (StringUtils.isEmpty(strInitRecord) || StringUtils.equals(strInitRecord, "0")) {
        vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber", "0");
      } else {
        int initRecord = (StringUtils.isEmpty(strInitRecord) ? 0 : Integer.parseInt(strInitRecord));
        initRecord -= intRecordRange;
        strInitRecord = ((initRecord < 0) ? "0" : Integer.toString(initRecord));
        vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber", strInitRecord);
        vars.setSessionValue(PREVIOUS_ACCTENTRIES, sb_previousAcctEntries.toString());
      }

      vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumberOld", "-1");
      response.sendRedirect(strDireccion + request.getServletPath());
    } else if (vars.commandIn("NEXT_RELATION")) {
      vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumberOld", "-1");
      response.sendRedirect(strDireccion + request.getServletPath());
    } else if (vars.commandIn("DOC")) {
      String org = vars.getStringParameter("inpOrg");
      String accSchema = vars.getStringParameter("inpcAcctSchemaId");
      String strDocument = vars.getRequestGlobalVariable("inpDocument",
          "ReportGeneralLedgerJournal|Document");
      Set<String> docbasetypes = getDocuments(org, accSchema);
      String combobox = getJSONComboBox(docbasetypes, strDocument, false, vars);

      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      out.println("objson = " + combobox);
      out.close();
    } else if (vars.commandIn("LEDGER")) {
      String strOrg = vars.getStringParameter("inpOrg");
      String strcAcctSchemaId = OBLedgerUtils.getOrgLedger(strOrg);
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      out.print(StringEscapeUtils.escapeHtml(strcAcctSchemaId));
      out.close();
    } else {
      pageError(response);
    }
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strDateFrom, String strDateTo, String strDocument, String strDocumentNo, String strOrg,
      String strTable, String strRecord, String strFactAcctGroupId, String strcAcctSchemaId,
      String strShowClosing, String strShowReg, String strShowOpening, String strPageNo,
      String strEntryNo, String strShowDescription, String strShowRegular, String strShowDivideUp,
      String accShemas, String strPosted, String strcelementvaluefrom, String strcelementvalueto,
      String strcelementvaluefromdes, String strcelementvaluetodes)
      throws IOException, ServletException {
    String strAllaccounts = "Y";
    if (StringUtils.isNotEmpty(strcelementvaluefrom)) {
      strAllaccounts = "N";
    }
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    String strRecordRange = Utility.getContext(readOnlyCP, vars, "#RecordRange",
        "ReportGeneralLedgerJournal");
    int intRecordRangePredefined = (StringUtils.isEmpty(strRecordRange) ? 0
        : Integer.parseInt(strRecordRange));
    String strInitRecord = vars.getSessionValue("ReportGeneralLedgerJournal.initRecordNumber");
    int initRecordNumber = (StringUtils.isEmpty(strInitRecord) ? 0
        : Integer.parseInt(strInitRecord));
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = null;
    ReportGeneralLedgerJournalData scrollData = null;
    ReportGeneralLedgerJournalData[] data = null;
    ReportGeneralLedgerJournalData[] dataCountLines = null;
    ReportGeneralLedgerJournalData scrollCountLines = null;
    String strPosition = "0";
    ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(), "ReportGeneralLedgerJournal",
        false, "", "", "imprimir();return false;", false, "ad_reports", strReplaceWith, false,
        true);
    toolbar.setEmail(false);
    int totalAcctEntries = 0;
    int lastRecordNumber = 0;
    String rowNum = "0";
    String oraLimit1 = null;
    String oraLimit2 = null;
    String pgLimit = null;
    Map<String, String> tablesToTabsMap = new HashMap<String, String>();
    try {
      if (vars.commandIn("FIND") || vars.commandIn("DEFAULT") && (!StringUtils
          .equals(vars.getSessionValue("ReportGeneralLedgerJournal.initRecordNumber"), "0")
          || StringUtils.equals(
              vars.getSessionValue("ReportGeneralLedgerJournal.initRecordNumberOld", ""), "0"))) {
        String strCheck = buildCheck(strShowClosing, strShowReg, strShowOpening, strShowRegular,
            strShowDivideUp);
        String strTreeOrg = TreeData.getTreeOrg(readOnlyCP, vars.getClient());
        String strOrgFamily = getFamily(strTreeOrg, strOrg);
        if (StringUtils.isEmpty(strRecord)) {
          // Stores the number of lines per accounting entry
          try {
            scrollCountLines = ReportGeneralLedgerJournalData.selectCountGroupedLines(readOnlyCP,
                rowNum, Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportGeneralLedger"),
                Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
                strDateFrom, DateTimeData.nDaysAfter(readOnlyCP, strDateTo, "1"), strDocument,
                getDocumentIds(vars.getClient(), strDocument, strDocumentNo), strcAcctSchemaId,
                strOrgFamily, strCheck, strAllaccounts, strcelementvaluefrom, strcelementvalueto,
                null, null, null);
            Vector<ReportGeneralLedgerJournalData> res = new Vector<ReportGeneralLedgerJournalData>();
            while (scrollCountLines.next()) {
              res.add(scrollCountLines.get());
              totalAcctEntries += Integer.parseInt(scrollCountLines.get().groupedlines);
            }
            dataCountLines = new ReportGeneralLedgerJournalData[res.size()];
            res.copyInto(dataCountLines);
          } finally {
            if (scrollCountLines != null) {
              scrollCountLines.close();
            }
          }
          String strInitAcctEntries = vars.getSessionValue(PREVIOUS_ACCTENTRIES);
          int acctEntries = (StringUtils.isEmpty(strInitAcctEntries) ? 0
              : Integer.parseInt(strInitAcctEntries.split(",")[0]));

          int groupedLines[] = new int[intRecordRangePredefined + 1];
          int i = 1;
          while (groupedLines[i - 1] <= intRecordRangePredefined
              && dataCountLines.length >= acctEntries) {
            if (dataCountLines.length > acctEntries) {
              groupedLines[i] = groupedLines[i - 1]
                  + Integer.parseInt(dataCountLines[acctEntries].groupedlines);
              i++;
            }
            acctEntries++;
          }

          int intRecordRangeUsed = 0;
          if (dataCountLines.length != acctEntries - 1) {
            if (i == 2) {
              // The first entry is bigger than the predefined range
              intRecordRangeUsed = groupedLines[i - 1];
              acctEntries++;
            } else if (i - 2 >= 0) {
              intRecordRangeUsed = groupedLines[i - 2];
            }
          } else {
            // Include also the last entry
            intRecordRangeUsed = groupedLines[i - 1];
          }

          // Hack for sqlC first record
          if (initRecordNumber == 0) {
            lastRecordNumber = initRecordNumber + intRecordRangeUsed + 1;
          } else {
            lastRecordNumber = initRecordNumber + intRecordRangeUsed;
          }
          vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber",
              String.valueOf(lastRecordNumber));
          vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumberOld", strInitRecord);

          // Stores historical for navigation purposes
          vars.setSessionValue(PREVIOUS_ACCTENTRIES_OLD,
              vars.getSessionValue(PREVIOUS_ACCTENTRIES));
          vars.setSessionValue(PREVIOUS_ACCTENTRIES,
              String.valueOf(acctEntries - 1) + "," + vars.getSessionValue(PREVIOUS_ACCTENTRIES));
          vars.setSessionValue(PREVIOUS_RANGE_OLD, vars.getSessionValue(PREVIOUS_RANGE));
          vars.setSessionValue(PREVIOUS_RANGE,
              String.valueOf(intRecordRangeUsed) + "," + vars.getSessionValue(PREVIOUS_RANGE));
          if (StringUtils.equalsIgnoreCase(readOnlyCP.getRDBMS(), "ORACLE")) {
            rowNum = "ROWNUM";
            oraLimit1 = String
                .valueOf((initRecordNumber == 0 ? initRecordNumber : initRecordNumber - 1)
                    + intRecordRangeUsed);
            oraLimit2 = ((initRecordNumber == 0 ? initRecordNumber : initRecordNumber - 1) + 1)
                + " AND " + oraLimit1;
          } else {
            rowNum = "0";
            pgLimit = intRecordRangeUsed + " OFFSET "
                + (initRecordNumber == 0 ? initRecordNumber : initRecordNumber - 1);
          }
          scrollData = ReportGeneralLedgerJournalData.select(readOnlyCP, rowNum, "'N'",
              Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportGeneralLedger"),
              Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
              strDateFrom, DateTimeData.nDaysAfter(readOnlyCP, strDateTo, "1"), strDocument,
              getDocumentIds(vars.getClient(), strDocument, strDocumentNo), strcAcctSchemaId,
              strOrgFamily, strCheck, strAllaccounts, strcelementvaluefrom, strcelementvalueto,
              vars.getLanguage(), pgLimit, oraLimit1, oraLimit2);
          Vector<ReportGeneralLedgerJournalData> res = new Vector<ReportGeneralLedgerJournalData>();
          while (scrollData.next()) {
            addDataToResponse(scrollData.get(), res, tablesToTabsMap);
          }
          data = new ReportGeneralLedgerJournalData[res.size()];
          res.copyInto(data);
          if (data != null && data.length > 0) {
            strPosition = ReportGeneralLedgerJournalData.selectCount(readOnlyCP,
                Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportGeneralLedger"),
                Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
                strDateFrom, DateTimeData.nDaysAfter(readOnlyCP, strDateTo, "1"), strDocument,
                getDocumentIds(vars.getClient(), strDocument, strDocumentNo), strcAcctSchemaId,
                strOrgFamily, strCheck, strAllaccounts, strcelementvaluefrom, strcelementvalueto,
                data[0].dateacct, data[0].identifier);
          }
        } else {
          if (StringUtils.equalsIgnoreCase(readOnlyCP.getRDBMS(), "ORACLE")) {
            rowNum = "ROWNUM";
            oraLimit1 = String
                .valueOf((initRecordNumber == 0 ? initRecordNumber : initRecordNumber - 1)
                    + intRecordRangePredefined);
            oraLimit2 = ((initRecordNumber == 0 ? initRecordNumber : initRecordNumber - 1) + 1)
                + " AND " + oraLimit1;
          } else {
            rowNum = "0";
            pgLimit = intRecordRangePredefined + " OFFSET "
                + (initRecordNumber == 0 ? initRecordNumber : initRecordNumber - 1);
          }
          scrollData = ReportGeneralLedgerJournalData.selectDirect(readOnlyCP, rowNum,
              StringUtils.equals(strShowDescription, "Y") ? "'Y'" : "'N'",
              Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportGeneralLedger"),
              Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
              strTable, strRecord, strcAcctSchemaId, vars.getLanguage(), pgLimit, oraLimit1,
              oraLimit2);
          Vector<ReportGeneralLedgerJournalData> res = new Vector<ReportGeneralLedgerJournalData>();
          while (scrollData.next()) {
            addDataToResponse(scrollData.get(), res, tablesToTabsMap);
          }
          data = new ReportGeneralLedgerJournalData[res.size()];
          res.copyInto(data);
          if (data != null && data.length > 0) {
            strPosition = ReportGeneralLedgerJournalData.selectCountDirect(readOnlyCP,
                Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportGeneralLedger"),
                Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
                strTable, strRecord, strFactAcctGroupId, data[0].dateacct, data[0].identifier);
          }
        }
      } else if (vars.commandIn("DIRECT")) {
        scrollData = ReportGeneralLedgerJournalData.selectDirect(this, rowNum,
            StringUtils.equals(strShowDescription, "Y") ? "'Y'" : "'N'",
            Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportGeneralLedger"),
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
            strTable, strRecord, strcAcctSchemaId, vars.getLanguage(), null, null, null);
        Vector<ReportGeneralLedgerJournalData> res = new Vector<ReportGeneralLedgerJournalData>();
        while (scrollData.next()) {
          addDataToResponse(scrollData.get(), res, tablesToTabsMap);
        }
        data = new ReportGeneralLedgerJournalData[res.size()];
        res.copyInto(data);
        if (data != null && data.length > 0) {
          strPosition = ReportGeneralLedgerJournalData.selectCountDirect(this,
              Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportGeneralLedger"),
              Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
              strTable, strRecord, strFactAcctGroupId, data[0].dateacct, data[0].identifier);
        }
      } else if (vars.commandIn("DIRECT2")) {
        scrollData = ReportGeneralLedgerJournalData.selectDirect2(readOnlyCP,
            Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportGeneralLedger"),
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
            strFactAcctGroupId, vars.getLanguage());
        Vector<ReportGeneralLedgerJournalData> res = new Vector<ReportGeneralLedgerJournalData>();
        while (scrollData.next()) {
          addDataToResponse(scrollData.get(), res, tablesToTabsMap);
        }
        data = new ReportGeneralLedgerJournalData[res.size()];
        res.copyInto(data);
        if (data != null && data.length > 0) {
          strPosition = ReportGeneralLedgerJournalData.selectCountDirect2(readOnlyCP,
              Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportGeneralLedger"),
              Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
              strFactAcctGroupId, data[0].dateacct, data[0].identifier);
        }
      }
      if (data == null || data.length == 0) {
        String discard[] = { "secTable" };
        toolbar.prepareRelationBarTemplate(false, false,
            "submitCommandForm('XLS', false, null, 'ReportGeneralLedgerJournal.xls', 'EXCEL');return false;");
        xmlDocument = xmlEngine
            .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportGeneralLedgerJournal",
                discard)
            .createXmlDocument();
        data = ReportGeneralLedgerJournalData.set("0");
      } else {
        data = notshow(data, vars);
        boolean hasPrevious = !(data == null || data.length == 0 || initRecordNumber <= 1);
        boolean hasNext = !(data == null || data.length == 0
            || lastRecordNumber >= totalAcctEntries);
        toolbar.prepareRelationBarTemplate(true, true,
            "submitCommandForm('XLS', false, null, 'ReportGeneralLedgerJournal.xls', 'EXCEL');return false;");
        xmlDocument = xmlEngine
            .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportGeneralLedgerJournal")
            .createXmlDocument();

        String jsDisablePreviousNext = "function checkPreviousNextButtons(){";
        if (!hasPrevious) {
          jsDisablePreviousNext += "disableToolBarButton('linkButtonPrevious');";
        }
        if (!hasNext) {
          jsDisablePreviousNext += "disableToolBarButton('linkButtonNext');";
        }
        jsDisablePreviousNext += "}";
        xmlDocument.setParameter("jsDisablePreviousNext", jsDisablePreviousNext);
      }
    } finally {
      if (scrollData != null) {
        scrollData.close();
      }
    }
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "LIST", "",
          "C_DocType DocBaseType", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportGeneralLedgerJournal"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportGeneralLedgerJournal"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
          "ReportGeneralLedgerJournal", strDocument);
      xmlDocument.setData("reportDocument", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    xmlDocument.setParameter("toolbar", toolbar.toString());
    try {
      WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
          "org.openbravo.erpCommon.ad_reports.ReportGeneralLedgerJournal");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(),
          "ReportGeneralLedgerJournal.html", classInfo.id, classInfo.type, strReplaceWith,
          tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(),
          "ReportGeneralLedgerJournal.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportGeneralLedgerJournal");
      vars.removeMessage("ReportGeneralLedgerJournal");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("document", strDocument);
    xmlDocument.setParameter("cAcctschemaId", strcAcctSchemaId);
    xmlDocument.setParameter("cAcctschemas", accShemas);
    xmlDocument.setParameter("posted", strPosted);

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR", "AD_ORG_ID",
          "", "", Utility.getContext(readOnlyCP, vars, "#User_Org", "ReportGeneralLedgerJournal"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportGeneralLedgerJournal"), '*');
      comboTableData.fillParameters(null, "ReportGeneralLedgerJournal", "");
      xmlDocument.setData("reportAD_ORGID", "liststructure", comboTableData.select(false));
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setData("reportC_ACCTSCHEMA_ID", "liststructure",
        AccountingSchemaMiscData.selectC_ACCTSCHEMA_ID(readOnlyCP,
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
            Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportGeneralLedger"),
            strcAcctSchemaId));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("adOrgId", strOrg);
    xmlDocument.setParameter("groupId", strPosition);
    xmlDocument.setParameter("paramRecord", strRecord);
    xmlDocument.setParameter("paramTable", strTable);
    vars.setSessionValue("ReportGeneralLedgerJournal|Record", strRecord);
    vars.setSessionValue("ReportGeneralLedgerJournal|Table", strTable);
    xmlDocument.setParameter("inpPageNo", strPageNo);
    xmlDocument.setParameter("inpDocumentNo", strDocumentNo);
    xmlDocument.setParameter("inpEntryNo", strEntryNo);
    // If none of the "show" flags is active, then regular is checked
    xmlDocument.setParameter("showRegular",
        (StringUtils.isEmpty(strShowRegular)) ? "N" : strShowRegular);
    xmlDocument.setParameter("showClosing",
        (StringUtils.isEmpty(strShowClosing)) ? "N" : strShowClosing);
    xmlDocument.setParameter("showReg", (StringUtils.isEmpty(strShowReg)) ? "N" : strShowReg);
    xmlDocument.setParameter("showOpening",
        (StringUtils.isEmpty(strShowOpening)) ? "N" : strShowOpening);
    xmlDocument.setParameter("showDivideUp",
        (StringUtils.isEmpty(strShowDivideUp)) ? "N" : strShowDivideUp);
    xmlDocument.setParameter("showDescription",
        (StringUtils.isEmpty(strShowDescription)) ? "N" : strShowDescription);
    xmlDocument.setParameter("paramElementvalueIdTo", strcelementvalueto);
    xmlDocument.setParameter("paramElementvalueIdFrom", strcelementvaluefrom);
    xmlDocument.setParameter("inpElementValueIdTo_DES", strcelementvaluetodes);
    xmlDocument.setParameter("inpElementValueIdFrom_DES", strcelementvaluefromdes);

    xmlDocument.setData("structure1", data);
    out.println(xmlDocument.print());
    out.close();
  }

  private void addDataToResponse(ReportGeneralLedgerJournalData data,
      Vector<ReportGeneralLedgerJournalData> res, Map<String, String> tablesToTabsMap) {
    // If it is a GLJournal without a Batch header change the TabId that is going to navigate to
    // based on the defined Navigation Rules
    if (StringUtils.equals(data.adTableId, GL_JOURNAL_TABLE_ID)
        && StringUtils.isEmpty(data.journalbatchId)) {
      if (tablesToTabsMap.containsKey(data.adTableId)) {
        data.tabId = tablesToTabsMap.get(data.adTableId);
      } else {
        String tabId = getTabIdFromTable(data.adTableId, data.id);
        tablesToTabsMap.put(data.adTableId, tabId);
        data.tabId = tabId;
      }
    }
    res.add(data);
  }

  private String getTabIdFromTable(final String adTableId, final String recordId) {
    try {
      OBContext.setAdminMode(true);
      return ReferencedLink.applyRules(null, adTableId,
          OBDal.getInstance().get(GLJournal.class, recordId).getEntity(), recordId, false, true);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private ReportGeneralLedgerJournalData[] notshow(ReportGeneralLedgerJournalData[] data,
      VariablesSecureApp vars) {
    for (int i = 0; i < data.length - 1; i++) {
      if ((StringUtils.equals(data[i].identifier.toString(), data[i + 1].identifier.toString()))
          && (StringUtils.equals(data[i].dateacct.toString(), data[i + 1].dateacct.toString()))) {
        data[i + 1].newstyle = "visibility: hidden";
      }
    }
    return data;
  }

  private void printPagePDF(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strDateFrom, String strDateTo, String strDocument,
      String strDocumentNo, String strOrg, String strTable, String strRecord,
      String strFactAcctGroupId, String strcAcctSchemaId, String strShowClosing, String strShowReg,
      String strShowOpening, String strPageNo, String strEntryNo, String strShowDescription,
      String strShowRegular, String strShowDivideUp, String strcelementvaluefrom,
      String strcelementvalueto) throws IOException, ServletException {

    ReportGeneralLedgerJournalData scrollData = null;
    ReportGeneralLedgerJournalData[] data = null;

    String strAllaccounts = "Y";
    if (StringUtils.isNotEmpty(strcelementvaluefrom)) {
      strAllaccounts = "N";
    }
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    String strTreeOrg = TreeData.getTreeOrg(readOnlyCP, vars.getClient());
    String strOrgFamily = getFamily(strTreeOrg, strOrg);
    try {
      OBError myMessage = vars.getMessage("ReportGeneralLedgerJournal");
      vars.removeMessage("ReportGeneralLedgerJournal");
      if (myMessage != null) {
        advisePopUp(request, response, myMessage.getType(), myMessage.getTitle(),
            myMessage.getMessage());
        return;
      }
      if (StringUtils.isNotEmpty(strFactAcctGroupId)) {
        scrollData = ReportGeneralLedgerJournalData.selectDirect2(readOnlyCP,
            Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportGeneralLedger"),
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
            strFactAcctGroupId, vars.getLanguage());

      } else if (StringUtils.isEmpty(strRecord)) {
        String strCheck = buildCheck(strShowClosing, strShowReg, strShowOpening, strShowRegular,
            strShowDivideUp);
        // Get the number of records that the query would return
        int recordCount = Integer.parseInt(ReportGeneralLedgerJournalData.selectCountNoOfRecords(
            readOnlyCP, Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportGeneralLedger"),
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
            strDateFrom, DateTimeData.nDaysAfter(readOnlyCP, strDateTo, "1"), strDocument,
            getDocumentIds(vars.getClient(), strDocument, strDocumentNo), strcAcctSchemaId,
            strOrgFamily, strCheck, strAllaccounts, strcelementvaluefrom, strcelementvalueto,
            StringUtils.equals(strShowDescription, "Y") ? "'Y'" : "'N'"));
        // Get the limit of number of records shown for a report based on the preference
        int limit = Integer.parseInt(Utility.getPreference(vars, "ReportsLimit", ""));
        // Do not print the XLS report if there are more than 65532 records
        if (vars.commandIn("XLS") && recordCount > 65532) {
          advisePopUp(request, response, "ERROR",
              Utility.messageBD(readOnlyCP, "ProcessStatus-E", vars.getLanguage()),
              Utility.messageBD(readOnlyCP, "numberOfRowsExceeded", vars.getLanguage()));
          // Do not print the PDF report if the number of records is greater than the limit of
          // records shown
        } else if (limit > 0 && recordCount > limit) {
          advisePopUp(request, response, "WARNING",
              Utility.messageBD(readOnlyCP, "ProcessStatus-W", vars.getLanguage()),
              Utility.messageBD(readOnlyCP, "ReportsLimit", vars.getLanguage())
                  .replace("@limit@", String.valueOf(limit)));
        } else {
          scrollData = ReportGeneralLedgerJournalData.select(readOnlyCP, "0",
              StringUtils.equals(strShowDescription, "Y") ? "'Y'" : "'N'",
              Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportGeneralLedger"),
              Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
              strDateFrom, DateTimeData.nDaysAfter(readOnlyCP, strDateTo, "1"), strDocument,
              getDocumentIds(vars.getClient(), strDocument, strDocumentNo), strcAcctSchemaId,
              strOrgFamily, strCheck, strAllaccounts, strcelementvaluefrom, strcelementvalueto,
              vars.getLanguage(), null, null, null);
        }
      } else {
        scrollData = ReportGeneralLedgerJournalData.selectDirect(readOnlyCP, "0",
            StringUtils.equals(strShowDescription, "Y") ? "'Y'" : "'N'",
            Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportGeneralLedger"),
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
            strTable, strRecord, strcAcctSchemaId, vars.getLanguage(), null, null, null);
      }
      Vector<ReportGeneralLedgerJournalData> res = new Vector<ReportGeneralLedgerJournalData>();
      while (scrollData.next()) {
        res.add(scrollData.get());
      }
      data = new ReportGeneralLedgerJournalData[res.size()];
      res.copyInto(data);
      if (data == null || data.length == 0) {
        advisePopUp(request, response, "WARNING",
            Utility.messageBD(readOnlyCP, "ProcessStatus-W", vars.getLanguage()),
            Utility.messageBD(readOnlyCP, "NoDataFound", vars.getLanguage()));
      } else {
        String strSubtitle = (Utility.messageBD(readOnlyCP, "LegalEntity", vars.getLanguage())
            + ": ") + ReportGeneralLedgerJournalData.selectCompany(readOnlyCP, vars.getClient())
            + "\n";
        ;

        SimpleDateFormat javaSDF = new SimpleDateFormat(vars.getJavaDateFormat());
        SimpleDateFormat sqlSDF = new SimpleDateFormat(
            vars.getSqlDateFormat().replace('Y', 'y').replace('D', 'd'));

        if (!StringUtils.equals(strOrg, "0")) {
          strSubtitle += (Utility.messageBD(readOnlyCP, "OBUIAPP_Organization", vars.getLanguage())
              + ": ") + ReportGeneralLedgerJournalData.selectOrg(readOnlyCP, strOrg) + "\n";
        }

        if (StringUtils.isNotEmpty(strDateFrom) || StringUtils.isNotEmpty(strDateTo)) {
          try {
            strSubtitle += (Utility.messageBD(readOnlyCP, "From", vars.getLanguage()) + ": ")
                + (StringUtils.isNotEmpty(strDateFrom) ? javaSDF.format(sqlSDF.parse(strDateFrom))
                    : "")
                + "  " + (Utility.messageBD(readOnlyCP, "OBUIAPP_To", vars.getLanguage()) + ": ")
                + (StringUtils.isNotEmpty(strDateTo) ? javaSDF.format(sqlSDF.parse(strDateTo)) : "")
                + "\n";
          } catch (ParseException e) {
            log4j.error("Error when parsing dates", e);
          }
        }

        if (StringUtils.isNotEmpty(strcAcctSchemaId)) {
          AcctSchema financialMgmtAcctSchema = OBDal.getReadOnlyInstance()
              .get(AcctSchema.class, strcAcctSchemaId);
          strSubtitle += Utility.messageBD(readOnlyCP, "generalLedger", vars.getLanguage()) + ": "
              + financialMgmtAcctSchema.getName();
        }

        String strOutput;
        String strReportName;
        if (vars.commandIn("PDF")) {
          strOutput = "pdf";
          strReportName = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportGeneralLedgerJournal.jrxml";
        } else {
          strOutput = "xls";
          strReportName = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportGeneralLedgerJournalExcel.jrxml";
        }

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("ShowDescription", strShowDescription);
        parameters.put("Subtitle", strSubtitle);
        parameters.put("PageNo", strPageNo);
        parameters.put("InitialEntryNumber", strEntryNo);
        parameters.put("TaxID", ReportGeneralLedgerJournalData.selectOrgTaxID(readOnlyCP, strOrg));
        parameters.put("strDateFormat", vars.getJavaDateFormat());
        renderJR(vars, response, strReportName, "JournalEntriesReport", strOutput, parameters, data,
            null);
      }
    } finally {
      if (scrollData != null) {
        scrollData.close();
      }
    }
  }

  private String getFamily(String strTree, String strChild) throws IOException, ServletException {
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    return Tree.getMembers(readOnlyCP, strTree, StringUtils.isEmpty(strChild) ? "0" : strChild);
  }

  private String buildCheck(String strShowClosing, String strShowReg, String strShowOpening,
      String strShowRegular, String strShowDivideUp) {
    String[] strElements = { StringUtils.equals(strShowClosing, "Y") ? "'C'" : "",
        StringUtils.equals(strShowReg, "Y") ? "'R'" : "",
        StringUtils.equals(strShowOpening, "Y") ? "'O'" : "",
        StringUtils.equals(strShowRegular, "Y") ? "'N'" : "",
        StringUtils.equals(strShowDivideUp, "Y") ? "'D'" : "" };
    int no = 0;
    String strCheck = "";
    for (int i = 0; i < strElements.length; i++) {
      if (StringUtils.isNotEmpty(strElements[i])) {
        if (no != 0) {
          strCheck = strCheck + ", ";
        }
        strCheck = strCheck + strElements[i];
        no++;
      }
    }
    return strCheck;
  }

  private <T extends BaseOBObject> String getJSONComboBox(Set<String> docbseTypes,
      String selectedValue, boolean isMandatory, VariablesSecureApp vars) {

    JSONObject json = new JSONObject();
    JSONArray select = new JSONArray();
    Map<String, String> attr = null;
    try {
      int i = 0;
      if (!isMandatory) {
        attr = new HashMap<String, String>();
        attr.put("value", "");
        attr.put("selected", "false");
        attr.put("text", "");
        select.put(i, attr);
        i++;
      }
      for (String dbt : docbseTypes) {
        attr = new HashMap<String, String>();
        attr.put("value", dbt);
        attr.put("selected", (StringUtils.equals(dbt, selectedValue)) ? "true" : "false");
        attr.put("text",
            Utility.getListValueName("C_DocType DocBaseType", dbt, vars.getLanguage()));
        select.put(i, attr);
        json.put("optionlist", select);
        i++;
      }
      json.put("ismandatory", String.valueOf(isMandatory));

    } catch (JSONException e) {
      log4j.error("Error creating JSON object for representing subaccount lines", e);
    }

    return json.toString();
  }

  public static Set<String> getDocuments(String org, String accSchema) {

    OBContext.setAdminMode();
    try {
      Set<String> orgStrct = OBContext.getOBContext()
          .getOrganizationStructureProvider()
          .getNaturalTree(org);
      //@formatter:off
      final String whereClause = " as cd ,"
          + "   FinancialMgmtAcctSchemaTable as ca "
          + " where cd.table.id = ca.table.id"
          + "   and ca.accountingSchema.id = :accSchemaId "
          + "   and ca.active = 'Y'"
          + "   and cd.organization.id in :orgNaturalTree"
          + "   and ca.organization.id in :orgNaturalTree"
          + " order by cd.documentCategory";
      
      //@formatter:on
      final OBQuery<DocumentType> obqDt = OBDal.getReadOnlyInstance()
          .createQuery(DocumentType.class, whereClause);
      obqDt.setNamedParameter("accSchemaId", accSchema);
      obqDt.setNamedParameter("orgNaturalTree", orgStrct);
      obqDt.setFilterOnReadableOrganization(false);
      TreeSet<String> docBaseTypes = new TreeSet<String>();
      for (DocumentType doc : obqDt.list()) {
        docBaseTypes.add(doc.getDocumentCategory());
      }
      return docBaseTypes;

    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * Builds dynamic SQL to filter by document No
   */
  private String getDocumentIds(String strClient, String strDocument, String strDocumentNo) {
    if (StringUtils.isBlank(strDocument) || StringUtils.isBlank(strDocumentNo)) {
      return null;
    }

    try {
      OBContext.setAdminMode();
      String documentNo = StringEscapeUtils.escapeSql(strDocumentNo);
      documentNo = documentNo.replaceAll(";", "");

      //@formatter:off
      final String where = " select t.name "
          + "  from DocumentType as d"
          + "    join d.table as t"
          + "  where d.documentCategory = :document"
          + "    and d.client.id = :client"
          + "  group by d.documentCategory, t.name";
      
      //@formatter:on
      Query<String> qry = OBDal.getReadOnlyInstance().getSession().createQuery(where, String.class);
      qry.setMaxResults(1);
      qry.setParameter("document", strDocument);
      qry.setParameter("client", strClient);
      String tablename = qry.uniqueResult();

      if (StringUtils.isBlank(tablename)) {
        return null;
      }

      OBCriteria<BaseOBObject> query = OBDal.getInstance().createCriteria(tablename);
      query.add(Restrictions.eq("documentNo", documentNo));

      return Utility.getInStrList(query.list(), true);
    } catch (Exception ignore) {
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private OBError checkReportUsage(VariablesSecureApp vars) {
    String userId = getUser();
    int userExecution = userExecutions.containsKey(userId) ? userExecutions.get(userId) : 0;
    userExecution++;
    userExecutions.put(userId, userExecution);
    reportExecutions++;

    String userExecutionPreference = Utility.getPreference(vars, "ReportsUserExecutionsLimit", "");
    int userExecutionLimit = Integer
        .parseInt(StringUtils.isEmpty(userExecutionPreference) ? "0" : userExecutionPreference);
    String reportExecutionPreference = Utility.getPreference(vars, "ReportsExecutionsLimit", "");
    int reportExecutionLimit = Integer
        .parseInt(StringUtils.isEmpty(reportExecutionPreference) ? "0" : reportExecutionPreference);

    // userExecutionLimit = 0 means that the userExecutionLimit must not be checked
    if (userExecution > userExecutionLimit && userExecutionLimit > 0) {
      OBError myMessage = new OBError();
      myMessage.setType("Error");
      myMessage.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
      String msgbody = Utility.messageBD(this, "ReportsUserExecutionsLimit", vars.getLanguage());
      String userName = OBDal.getInstance().get(User.class, userId).getName();
      msgbody = String.format(msgbody, userExecutionLimit, userName);
      myMessage.setMessage(msgbody);
      return myMessage;
    }

    // reportExecutionLimit = 0 means that the reportExecutionLimit must not be checked
    else if (reportExecutions > reportExecutionLimit && reportExecutionLimit > 0) {
      OBError myMessage = new OBError();
      myMessage.setType("Error");
      myMessage.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
      String msgbody = Utility.messageBD(this, "ReportsExecutionsLimit", vars.getLanguage());
      msgbody = String.format(msgbody, reportExecutionLimit);
      myMessage.setMessage(msgbody);
      return myMessage;
    }

    return null;
  }

  private String getUser() {
    return (String) RequestContext.get().getSession().getAttribute("#Authenticated_user");
  }

  private void decreaseReportUsage() {
    String userId = getUser();
    if (userExecutions.containsKey(userId)) {
      userExecutions.put(userId, userExecutions.get(userId) - 1);
      if (userExecutions.get(userId) <= 0) {
        userExecutions.remove(userId);
      }
    }
    reportExecutions--;
    if (reportExecutions < 0) {
      reportExecutions = 0;
    }
  }

  @Override
  public String getServletInfo() {
    return "Servlet ReportGeneralLedgerJournal. This Servlet was made by Pablo Sarobe modified by everybody";
  }
}
