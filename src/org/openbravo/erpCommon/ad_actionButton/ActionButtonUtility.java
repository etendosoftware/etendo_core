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
package org.openbravo.erpCommon.ad_actionButton;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.smf.jobs.Result;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.utility.*;
import org.openbravo.financial.ResetAccounting;
import org.openbravo.model.ad.ui.Tab;

import javax.servlet.ServletException;

public class ActionButtonUtility {
  static Logger log4j = LogManager.getLogger();

  public static FieldProvider[] docAction(ConnectionProvider conn, VariablesSecureApp vars,
      String strDocAction, String strReference, String strDocStatus, String strProcessing,
      String strTable) {
    return docAction(conn, vars, strDocAction, strReference, "", strDocStatus, strProcessing,
        strTable, null);
  }

  public static FieldProvider[] docAction(ConnectionProvider conn, VariablesSecureApp vars,
      String strDocAction, String strReference, String strDocStatus, String strProcessing,
      String strTable, String tabId) {
    return docAction(conn, vars, strDocAction, strReference, "", strDocStatus, strProcessing,
        strTable, tabId);
  }

  public static FieldProvider[] docAction(ConnectionProvider conn, VariablesSecureApp vars,
      String strDocAction, String strReference, String validationRule, String strDocStatus,
      String strProcessing, String strTable, String tabId) {
    FieldProvider[] ld = null;
    boolean isQuotation = false;
    String windowId = "";
    if (tabId != null) {
      OBContext.setAdminMode(true);
      try {
        Tab tab = OBDal.getInstance().get(Tab.class, tabId);
        windowId = tab.getWindow().getId().toString();
      } finally {
        OBContext.restorePreviousMode();
      }
    }

    if (log4j.isDebugEnabled()) {
      log4j.debug("DocAction - generating combo elements for table: " + strTable
          + " - actual status: " + strDocStatus);
    }
    try {
      ComboTableData comboTableData = new ComboTableData(vars, conn, "LIST", "DocAction",
          strReference, validationRule,
          Utility.getContext(conn, vars, "#AccessibleOrgTree", "ActionButtonUtility"),
          Utility.getContext(conn, vars, "#User_Client", "ActionButtonUtility"), 0);
      Utility.fillSQLParameters(conn, vars, null, comboTableData, "ActionButtonUtility", "");
      ld = comboTableData.select(false);
      comboTableData = null;
      isQuotation = "Y"
          .equals(vars.getGlobalVariable("inpisQuotation", windowId + "|isQuotation", "N"));

    } catch (Exception e) {
      return null;
    }
    SQLReturnObject[] data = null;
    if (ld != null) {
      Vector<SQLReturnObject> v = new Vector<>();
      SQLReturnObject data1 = new SQLReturnObject();
      if (!strProcessing.equals("") && strProcessing.equals("Y")) {
        data1.setData("ID", "XL");
        v.addElement(data1);
      } else if (strDocStatus.equals("NA")) {
        data1.setData("ID", "AP");
        v.addElement(data1);
        data1 = new SQLReturnObject();
        data1.setData("ID", "RJ");
        v.addElement(data1);
        data1 = new SQLReturnObject();
        data1.setData("ID", "VO");
        v.addElement(data1);
      } else if (strDocStatus.equals("DR") || strDocStatus.equals("IP")) {
        data1.setData("ID", "CO");
        v.addElement(data1);
        if (!strTable.equals("319") && !strTable.equals("800212") && !isQuotation
            && !"224".equals(strTable)
            && (strDocStatus.equals("IP") || (!strTable.equals("318")))) {
          data1 = new SQLReturnObject();
          data1.setData("ID", "VO");
          v.addElement(data1);
        }
      } else if ((strDocStatus.equals("CO")) && !(strTable.equals("318")) && // C_Invoice
          !(strTable.equals("319"))) { // M_InOut
        // Exclude Close for tables C_Invoice and M_InOut because it has
        // no sense for them
        data1.setData("ID", "CL");
        v.addElement(data1);
      }
      data1 = new SQLReturnObject();
      if (strTable.equals("259") && !isQuotation) { // C_Order
        if (strDocStatus.equals("DR")) {
          data1.setData("ID", "PR");
          v.addElement(data1);
        } else if (strDocStatus.equals("CO")) {
          data1.setData("ID", "RE");
          v.addElement(data1);
        } else if (strDocStatus.equals("NC") || strDocStatus.equals("AE")
            || strDocStatus.equals("ME")) {
          data1.setData("ID", "CO");
          v.addElement(data1);
          data1 = new SQLReturnObject();
          data1.setData("ID", "CL");
          v.addElement(data1);
          data1 = new SQLReturnObject();
          data1.setData("ID", "RJ");
          v.addElement(data1);
        }
      } else if (strTable.equals("259") && isQuotation) { // Quotations (C_Order)
        if (strDocStatus.equals("UE")) {
          data1.setData("ID", "RJ");
          v.addElement(data1);
          data1 = new SQLReturnObject();
          data1.setData("ID", "RE");
          v.addElement(data1);
        }
      } else if (strTable.equals("318")) { // C_Invoice
        if (strDocStatus.equals("CO")) {
          data1.setData("ID", "RC");
          v.addElement(data1);
          data1 = new SQLReturnObject();
          data1.setData("ID", "RE");
          v.addElement(data1);
        }
      } else if (strTable.equals("319")) { // M_InOut
        if (strDocStatus.equals("CO")) {
          data1.setData("ID", "RC");
          v.addElement(data1);
        }
      } else if (strTable.equals("224")) { // GL_Journal
        if (strDocStatus.equals("CO")) {
          data1.setData("ID", "RE");
          v.addElement(data1);
        }
      } else if (strTable.equals("800212")) { // M_Requisition
        if (strDocStatus.equals("CO")) {
          data1.setData("ID", "RE");
          v.addElement(data1);
        }
      }

      data = new SQLReturnObject[v.size()];
      if (log4j.isDebugEnabled()) {
        log4j.debug("DocAction - total combo elements: " + data.length);
      }
      int ind1 = 0, ind2 = 0;
      while (ind1 < ld.length && ind2 < v.size()) {
        for (int j = 0; j < v.size(); j++) {
          SQLReturnObject sqlro = v.get(j);
          if (sqlro.getField("ID").equals(ld[ind1].getField("ID"))) {
            if (log4j.isDebugEnabled()) {
              log4j.debug("DocAction - Element: " + ind1 + " - ID: " + sqlro.getField("ID"));
            }
            data[ind2] = sqlro;
            data[ind2].setData("NAME", ld[ind1].getField("NAME"));
            data[ind2].setData("DESCRIPTION", ld[ind1].getField("DESCRIPTION"));
            ind2++;
            break;
          }
        }
        ind1++;
      }
      // Exclude null values in the array
      List<SQLReturnObject> result = new ArrayList<SQLReturnObject>();
      for (SQLReturnObject sqlr : data) {
        if (sqlr != null) {
          result.add(sqlr);
        }
      }
      data = result.toArray(new SQLReturnObject[0]);

    }
    return data;
  }

  public static FieldProvider[] projectAction(ConnectionProvider conn, VariablesSecureApp vars,
      String strProjectAction, String strReference, String strProjectStatus) {
    FieldProvider[] ld = null;
    try {
      ComboTableData comboTableData = new ComboTableData(vars, conn, "LIST", "ProjectAction",
          strReference, "",
          Utility.getContext(conn, vars, "#AccessibleOrgTree", "ActionButtonUtility"),
          Utility.getContext(conn, vars, "#User_Client", "ActionButtonUtility"), 0);
      Utility.fillSQLParameters(conn, vars, null, comboTableData, "ActionButtonUtility", "");
      ld = comboTableData.select(false);
      comboTableData = null;
    } catch (Exception e) {
      return null;
    }
    SQLReturnObject[] data = null;
    if (ld != null) {
      Vector<SQLReturnObject> v = new Vector<>();
      SQLReturnObject data1 = new SQLReturnObject();
      if (strProjectStatus.equals("NF") || strProjectStatus.equals("OP")) {
        data1.setData("ID", "OR");
        v.addElement(data1);
        data1 = new SQLReturnObject();
        data1.setData("ID", "OC");
        v.addElement(data1);
      } else if (strProjectStatus.equals("OR")) {
        data1.setData("ID", "OC");
        v.addElement(data1);
      }

      if (v.size() > 0) {
        data = new SQLReturnObject[v.size()];
        v.copyInto(data);
        for (int i = 0; i < data.length; i++) {
          for (int j = 0; j < ld.length; j++) {
            if (data[i].getField("ID").equals(ld[j].getField("ID"))) {
              data[i].setData("NAME", ld[j].getField("NAME"));
              data[i].setData("DESCRIPTION", ld[j].getField("DESCRIPTION"));
              break;
            }
          }
        }
      }
    }
    return data;
  }

  public static OBError resetAccounting(VariablesSecureApp vars, String client, String org, String strTableId,
      String strKey, String dateAcct, ConnectionProvider con) {
    OBError myMessage = new OBError();
    myMessage.setType(Result.Type.SUCCESS.toString());
    try {
      HashMap<String, Integer> hm = ResetAccounting.delete(client, org,
          strTableId, strKey, dateAcct, dateAcct);
      myMessage.setMessage(
          Utility.parseTranslation(con, vars, vars.getLanguage(), "@UnpostedDocuments@ = "
              + hm.get("updated") + ", @DeletedEntries@ = " + hm.get("deleted")));
      myMessage.setTitle(Utility.messageBD(con, Result.Type.SUCCESS.toString(), vars.getLanguage()));
    } catch (OBException e) {
      myMessage.setType("Error");
      myMessage.setMessage(
          Utility.parseTranslation(con, vars, vars.getLanguage(), e.getMessage()));
    }
    return myMessage;
  }

  public static OBError processButton(VariablesSecureApp vars, String strKey, String strTableId, String strOrg,
      ConnectionProvider connectionProvider)
      throws ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("ProcessButton strKey: {} strTableId: {}", strKey, strTableId);
    }
    OBError myMessage = null;
    Connection con = null;
    try {
      con = connectionProvider.getConnection();
      AcctServer acct = AcctServer.get(strTableId, vars.getClient(), strOrg, connectionProvider);
      if (acct == null) {
        myMessage = Utility.translateError(connectionProvider, vars, vars.getLanguage(), "ProcessRunError");
        return myMessage;
      } else {
        Boolean isPost = false;
        String obException = "";
        obException = acct.catchPostError(strKey, false, vars, connectionProvider, con);
        if (obException != null) {
          myMessage = Utility.translateError(connectionProvider, vars, vars.getLanguage(), obException);
          SessionHandler.getInstance().commitAndStart();
          return myMessage;
        }
      }
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      log4j.error(e);
      myMessage = Utility.translateError(connectionProvider, vars, vars.getLanguage(), e.getMessage());
    }

    if (myMessage == null) {
      myMessage = new OBError();
      myMessage.setType("Success");
      myMessage.setTitle("");
      myMessage.setMessage(OBMessageUtils.messageBD("Success"));
    }
    return myMessage;
  }


}
