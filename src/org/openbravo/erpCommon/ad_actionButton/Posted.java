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
 * All portions are Copyright (C) 2001-2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_actionButton;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.financial.ResetAccounting;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

import static org.openbravo.erpCommon.ad_actionButton.ActionButtonUtility.processButton;

public class Posted extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Posted: doPost");
    }

    final String generalLedgerJournalReport_ID = "800000";
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strKey = vars.getGlobalVariable("inpKey", "Posted|key");
      String strTableId = vars.getGlobalVariable("inpTableId", "Posted|tableId");
      String strTabId = vars.getGlobalVariable("inpTabId", "Posted|tabId");
      String strPosted = vars.getGlobalVariable("inpPosted", "Posted|posted");
      String strProcessId = vars.getGlobalVariable("inpProcessId", "Posted|processId", "");
      String strPath = vars.getGlobalVariable("inpPath", "Posted|path",
          strDireccion + request.getServletPath());
      String strWindowId = vars.getGlobalVariable("inpWindowId", "Posted|windowId", "");
      String strForcedTableId = vars.getGlobalVariable("inpforcedTableId",
          strWindowId + "|FORCED_TABLE_ID", "");
      String strTabName = vars.getGlobalVariable("inpTabName", "Posted|tabName", "");
      String strModify = "N";
      if (strPosted.equals("Y")) {
        final Table table = OBDal.getInstance().get(Table.class, strTableId);
        final OBCriteria<AccountingFact> fact = OBDal.getInstance()
            .createCriteria(AccountingFact.class);
        fact.add(Restrictions.eq(AccountingFact.PROPERTY_RECORDID, strKey));
        fact.add(Restrictions.eq(AccountingFact.PROPERTY_TABLE, table));
        fact.add(Restrictions.eq(AccountingFact.PROPERTY_MODIFY, true));
        if (fact.count() > 0) {
          strModify = "Y";
        }
      }
      printPage(response, vars, strKey, strWindowId, strTabId, strProcessId, strTableId,
          strForcedTableId, strPath, strTabName, strPosted, strModify);
    } else if (vars.commandIn("SAVE")) {
      String strKey = vars.getRequiredGlobalVariable("inpKey", "Posted|key");
      String strTableId = vars.getRequiredGlobalVariable("inpTableId", "Posted|tableId");
      String strTabId = vars.getRequestGlobalVariable("inpTabId", "Posted|tabId");
      String strPosted = vars.getRequiredGlobalVariable("inpPosted", "Posted|posted");
      vars.getRequestGlobalVariable("inpProcessId", "Posted|processId");
      vars.getRequestGlobalVariable("inpPath", "Posted|path");
      vars.getRequestGlobalVariable("inpWindowId", "Posted|windowId");
      vars.getRequestGlobalVariable("inpTabName", "Posted|tabName");
      String strEliminar = vars.getStringParameter("inpEliminar", "N");

      if (log4j.isDebugEnabled()) {
        log4j.debug("SAVE, strPosted: " + strPosted + " Elim " + strEliminar);
      }
      if (!"Y".equals(strPosted)) {
        OBError messageResult = processButton(vars, strKey, strTableId, vars.getOrg(), this);
        if (!"Success".equals(messageResult.getType())) {
          vars.setMessage(strTabId, messageResult);
          printPageClosePopUp(response, vars);
        } else {
          PostedData[] data = PostedData.select(this, strKey, strTableId);
          if (data == null || data.length == 0 || data[0].id.equals("")) {
            // vars.setSessionValue(strWindowId + "|" + strTabName +
            // ".message", messageResult);
            vars.setMessage(strTabId, messageResult);
            printPageClosePopUp(response, vars);
          } else {
            String title;
            OBContext.setAdminMode();
            try {
              Process genLedJour = OBDal.getInstance()
                  .get(Process.class, generalLedgerJournalReport_ID);
              if (genLedJour != null) {
                title = genLedJour.getIdentifier();
              } else {
                title = "POST";
              }
            } finally {
              OBContext.restorePreviousMode();
            }
            PostedData[] data2 = null;
            data2 = PostedData.selectAccSchemas(this, "ACCTDESCRIPTION",
                Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedger"),
                Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
                strTableId, strKey, vars.getLanguage());

            String accSchemas = "";
            String schemaNames = "";
            for (int i = 0; i < data2.length; i++) {

              if (i + 1 == data2.length) {
                accSchemas = accSchemas + data2[i].schemaId;
                schemaNames = schemaNames + data2[i].schemaName;
              } else {
                accSchemas = accSchemas + data2[i].schemaId + ",";
                schemaNames = schemaNames + data2[i].schemaName;
              }
            }

            if (data2.length == 1) {
              printPageClosePopUp(response, vars,
                  strDireccion
                      + "/ad_reports/ReportGeneralLedgerJournal.html?Command=DIRECT&inpTable="
                      + strTableId + "&inpRecord=" + strKey + "&inpOrg=" + data[0].org
                      + "&inpAccSchemas=" + accSchemas + "&posted=Y",
                  title);
            } else if (data2.length > 1) {
              printPageClosePopUp(response, vars,
                  strDireccion
                      + "/ad_reports/ReportGeneralLedgerJournal.html?Command=DIRECT&inpTable="
                      + strTableId + "&inpRecord=" + strKey + "&inpOrg=" + data[0].org
                      + "&inpAccSchemas=" + accSchemas + "&posted=Y",
                  title + " - " + data2[0].schemaName);
            } else {
              printPageClosePopUp(response, vars);
            }
          }
        }
      }
      if (strEliminar.equals("N")) {
        PostedData[] data = PostedData.select(this, strKey, strTableId);
        if (data == null || data.length == 0 || data[0].id.equals("")) {
          vars.setMessage(strTabId,
              Utility.translateError(this, vars, vars.getLanguage(), "NoFactAcct"));
          printPageClosePopUp(response, vars);
        } else {
          String title;
          OBContext.setAdminMode();
          try {
            Process genLedJour = OBDal.getInstance()
                .get(Process.class, generalLedgerJournalReport_ID);
            if (genLedJour != null) {
              title = genLedJour.getIdentifier();
            } else {
              title = "POST";
            }
          } finally {
            OBContext.restorePreviousMode();
          }
          PostedData[] data2 = null;
          data2 = PostedData.selectAccSchemas(this, "ACCTDESCRIPTION",
              Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedger"),
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
              strTableId, strKey, vars.getLanguage());

          String accSchemas = "";
          String schemaNames = "";
          for (int i = 0; i < data2.length; i++) {

            if (i + 1 == data2.length) {
              accSchemas = accSchemas + data2[i].schemaId;
              schemaNames = schemaNames + data2[i].schemaName;
            } else {
              accSchemas = accSchemas + data2[i].schemaId + ",";
              schemaNames = schemaNames + data2[i].schemaName;
            }
          }

          if (data2.length == 1) {
            printPageClosePopUp(response, vars,
                strDireccion
                    + "/ad_reports/ReportGeneralLedgerJournal.html?Command=DIRECT&inpTable="
                    + strTableId + "&inpRecord=" + strKey + "&inpOrg=" + data[0].org
                    + "&inpAccSchemas=" + accSchemas + "&posted=Y",
                title);
          } else {
            printPageClosePopUp(response, vars,
                strDireccion
                    + "/ad_reports/ReportGeneralLedgerJournal.html?Command=DIRECT&inpTable="
                    + strTableId + "&inpRecord=" + strKey + "&inpOrg=" + data[0].org
                    + "&inpAccSchemas=" + accSchemas + "&posted=Y",
                title + " - " + data2[0].schemaName);
          }
        }
      } else {
        if (log4j.isDebugEnabled()) {
          log4j.debug("SAVE, delete");
        }
        long start = System.currentTimeMillis();
        PostedData[] data = PostedData.select(this, strKey, strTableId);
        OBError myMessage = ActionButtonUtility.resetAccounting(vars, data[0].client, data[0].org, strTableId, strKey,
            data[0].dateacct, new DalConnectionProvider());
        log4j.debug("Total deleting /milis: " + (System.currentTimeMillis() - start));
        vars.setMessage(strTabId, myMessage);
        printPageClosePopUp(response, vars);
      }

    } else {
      pageErrorPopUp(response);
    }
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strKey,
      String windowId, String strTab, String strProcessId, String strTableId,
      String strForcedTableId, String strPath, String strTabName, String strPosted,
      String strModify) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: Button process Posted");
    }

    ActionButtonDefaultData[] data = null;
    String strHelp = "", strDescription = "";
    if (vars.getLanguage().equals("en_US")) {
      data = ActionButtonDefaultData.select(this, strProcessId);
    } else {
      data = ActionButtonDefaultData.selectLanguage(this, vars.getLanguage(), strProcessId);
    }

    if (data != null && data.length != 0) {
      strDescription = data[0].description;
      strHelp = data[0].help;
    }
    String[] discard = {"", ""};
    if (strHelp.equals("")) {
      discard[0] = new String("helpDiscard");
    }
    if (!"Y".equals(strPosted)) {
      discard[1] = new String("selEliminar");
    }
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/Posted", discard)
        .createXmlDocument();
    xmlDocument.setParameter("key", strKey);
    xmlDocument.setParameter("window", windowId);
    xmlDocument.setParameter("tab", strTab);
    xmlDocument.setParameter("process", strProcessId);
    if ("".equals(strForcedTableId) || strForcedTableId == null) {
      xmlDocument.setParameter("table", strTableId);
    } else {
      xmlDocument.setParameter("table", strForcedTableId);
    }
    xmlDocument.setParameter("posted", strPosted);
    xmlDocument.setParameter("path", strPath);
    xmlDocument.setParameter("tabname", strTabName);

    {
      OBError myMessage = vars.getMessage("Posted");
      vars.removeMessage("Posted");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("question",
        Utility.messageBD(this, "StartProcess?", vars.getLanguage()));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("description", strDescription);
    xmlDocument.setParameter("help", strHelp);
    xmlDocument.setParameter("modify", strModify);

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  @Override
  public String getServletInfo() {
    return "Servlet Posted";
  } // end of getServletInfo() method
}
