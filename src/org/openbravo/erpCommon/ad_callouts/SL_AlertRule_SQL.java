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
package org.openbravo.erpCommon.ad_callouts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.FormatUtilities;

/**
 * Checks the SQL in Alert Rule to ensure all required columns are included.
 */
public class SL_AlertRule_SQL extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String sql = info.getStringParameter("inpsql");

    if (StringUtils.isEmpty(sql)) {
      return;
    }

    String msg = "";
    if (sql.toUpperCase().trim().startsWith("SELECT ")) {
      PreparedStatement st = null;
      Connection con = null;
      ConnectionProvider cp = new DalConnectionProvider(false);
      try {
        con = cp.getTransactionConnection();
        con.setReadOnly(true);

        st = con.prepareStatement(sql);
        ResultSetMetaData rmeta = st.getMetaData();
        if (!existsColumn(rmeta, "AD_CLIENT_ID")) {
          msg = "AD_CLIENT_ID ";
        }
        if (!existsColumn(rmeta, "AD_ORG_ID")) {
          msg += "AD_ORG_ID ";
        }
        if (!existsColumn(rmeta, "CREATED")) {
          msg += "CREATED ";
        }
        if (!existsColumn(rmeta, "CREATEDBY")) {
          msg += "CREATEDBY ";
        }
        if (!existsColumn(rmeta, "UPDATED")) {
          msg += "UPDATED ";
        }
        if (!existsColumn(rmeta, "UPDATEDBY")) {
          msg += "UPDATEDBY ";
        }
        if (!existsColumn(rmeta, "ISACTIVE")) {
          msg += "ISACTIVE ";
        }
        if (!existsColumn(rmeta, "AD_USER_ID")) {
          msg += "AD_USER_ID ";
        }
        if (!existsColumn(rmeta, "AD_ROLE_ID")) {
          msg += "AD_ROLE_ID ";
        }
        if (!existsColumn(rmeta, "RECORD_ID")) {
          msg += "RECORD_ID ";
        }
        if (!existsColumn(rmeta, "DESCRIPTION")) {
          msg += "DESCRIPTION ";
        }
        if (!existsColumn(rmeta, "REFERENCEKEY_ID")) {
          msg += "REFERENCEKEY_ID";
        }
        if (StringUtils.isNotEmpty(msg)) {
          msg = Utility.messageBD(this, "notColumnInQuery", info.vars.getLanguage()) + msg;
        }
      } catch (Exception ex) {
        msg = "error in query: " + FormatUtilities.replaceJS(ex.toString());
      } finally {

        if (st != null) {
          try {
            st.close();
          } catch (SQLException e) {
            log4j.error("Error closing statement in Alert Rule query: " + sql, e);
          }
        }

        if (con != null) {
          try {
            con.setReadOnly(false);
          } catch (SQLException e) {
            log4j.error("Error resetting readonly to connection in Alert Rule query: " + sql, e);
          }
        }
        try {
          cp.releaseRollbackConnection(con);
        } catch (SQLException e) {
          log4j.error("Error releasing statement in Alert Rule query: " + sql, e);
        }
      }
    } else {
      msg = Utility.messageBD(this, "AlertSelectConstraint", info.vars.getLanguage());
    }

    if (StringUtils.isNotEmpty(msg)) {
      info.showMessage(msg);
    }
  }

  private boolean existsColumn(ResultSetMetaData rmeta, String col) {
    try {
      for (int i = 1; i <= rmeta.getColumnCount(); i++) {
        if (StringUtils.equalsIgnoreCase(rmeta.getColumnName(i), col)) {
          return true;
        }
      }
    } catch (SQLException ignore) {
    }
    return false;
  }
}
