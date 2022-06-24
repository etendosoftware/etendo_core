//Sqlc generated V1.O00-1
package org.openbravo.modulescript;

import java.sql.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.servlet.ServletException;

import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.data.UtilSql;
import org.openbravo.database.SessionInfo;
import java.util.*;

@SuppressWarnings("serial")
class InitializeGeneralLedgerOrganizationData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String cAcctschemaId;
  public String adOrgId;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("c_acctschema_id") || fieldName.equals("cAcctschemaId"))
      return cAcctschemaId;
    else if (fieldName.equalsIgnoreCase("ad_org_id") || fieldName.equals("adOrgId"))
      return adOrgId;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static InitializeGeneralLedgerOrganizationData[] selectGeneralLedger(ConnectionProvider connectionProvider)    throws ServletException {
    return selectGeneralLedger(connectionProvider, 0, 0);
  }

  public static InitializeGeneralLedgerOrganizationData[] selectGeneralLedger(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        select (select min(ad_org_acctschema.c_acctschema_id)" +
      "        from ad_org_acctschema " +
      "        where ad_org.ad_org_id = ad_org_acctschema.ad_org_id" +
      "        and ad_org_acctschema.isactive = 'Y'" +
      "        and created in (select min(created) from ad_org_acctschema where isactive = 'Y' group by ad_org_id)" +
      "        ) as c_acctschema_id , ad_org_id" +
      "        from ad_org" +
      "        where exists (select 1 " +
      "        from ad_org_acctschema " +
      "        where ad_org.ad_org_id = ad_org_acctschema.ad_org_id " +
      "        and ad_org_acctschema.isactive = 'Y')" +
      "        and c_acctschema_id is null";

    ResultSet result;
    Vector<InitializeGeneralLedgerOrganizationData> vector = new Vector<InitializeGeneralLedgerOrganizationData>(0);
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      long countRecord = 0;
      long countRecordSkip = 1;
      boolean continueResult = true;
      while(countRecordSkip < firstRegister && continueResult) {
        continueResult = result.next();
        countRecordSkip++;
      }
      while(continueResult && result.next()) {
        countRecord++;
        InitializeGeneralLedgerOrganizationData objectInitializeGeneralLedgerOrganizationData = new InitializeGeneralLedgerOrganizationData();
        objectInitializeGeneralLedgerOrganizationData.cAcctschemaId = UtilSql.getValue(result, "c_acctschema_id");
        objectInitializeGeneralLedgerOrganizationData.adOrgId = UtilSql.getValue(result, "ad_org_id");
        objectInitializeGeneralLedgerOrganizationData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectInitializeGeneralLedgerOrganizationData);
        if (countRecord >= numberRegisters && numberRegisters != 0) {
          continueResult = false;
        }
      }
      result.close();
    } catch(SQLException e){
      if (log4j.isDebugEnabled()) {
        log4j.error("SQL error in query: " + strSql, e);
      } else {
        log4j.error("SQL error in query: " + strSql + " :" + e);
      }
      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch(Exception ex){
      if (log4j.isDebugEnabled()) {
        log4j.error("Exception in query: " + strSql, ex);
      } else {
        log4j.error("Exception in query: " + strSql + " :" + ex);
      }
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch(Exception e){
        log4j.error("Error during release*Statement of query: " + strSql, e);
      }
    }
    InitializeGeneralLedgerOrganizationData objectInitializeGeneralLedgerOrganizationData[] = new InitializeGeneralLedgerOrganizationData[vector.size()];
    vector.copyInto(objectInitializeGeneralLedgerOrganizationData);
    return(objectInitializeGeneralLedgerOrganizationData);
  }

  public static int initializeGl(ConnectionProvider connectionProvider, String cAcctschemaId, String adOrgId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        update ad_org set c_acctschema_id=? " +
      "        where ad_org_id=?";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, cAcctschemaId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, adOrgId);

      SessionInfo.saveContextInfoIntoDB(connectionProvider.getConnection());
      updateCount = st.executeUpdate();
    } catch(SQLException e){
      if (log4j.isDebugEnabled()) {
        log4j.error("SQL error in query: " + strSql, e);
      } else {
        log4j.error("SQL error in query: " + strSql + " :" + e);
      }
      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch(Exception ex){
      if (log4j.isDebugEnabled()) {
        log4j.error("Exception in query: " + strSql, ex);
      } else {
        log4j.error("Exception in query: " + strSql + " :" + ex);
      }
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch(Exception e){
        log4j.error("Error during release*Statement of query: " + strSql, e);
      }
    }
    return(updateCount);
  }
}
