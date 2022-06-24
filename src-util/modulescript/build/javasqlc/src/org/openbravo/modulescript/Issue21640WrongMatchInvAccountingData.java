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
class Issue21640WrongMatchInvAccountingData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String mMatchinvId;
  public String adClientId;
  public String adOrgId;
  public String matchinv;
  public String adRoleId;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("m_matchinv_id") || fieldName.equals("mMatchinvId"))
      return mMatchinvId;
    else if (fieldName.equalsIgnoreCase("ad_client_id") || fieldName.equals("adClientId"))
      return adClientId;
    else if (fieldName.equalsIgnoreCase("ad_org_id") || fieldName.equals("adOrgId"))
      return adOrgId;
    else if (fieldName.equalsIgnoreCase("matchinv"))
      return matchinv;
    else if (fieldName.equalsIgnoreCase("ad_role_id") || fieldName.equals("adRoleId"))
      return adRoleId;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static Issue21640WrongMatchInvAccountingData[] select(ConnectionProvider connectionProvider)    throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static Issue21640WrongMatchInvAccountingData[] select(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT '' AS m_matchinv_id, '' AS ad_client_id, '' AS ad_org_id, '' as matchinv, '' as ad_role_id" +
      "      FROM DUAL";

    ResultSet result;
    Vector<Issue21640WrongMatchInvAccountingData> vector = new Vector<Issue21640WrongMatchInvAccountingData>(0);
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
        Issue21640WrongMatchInvAccountingData objectIssue21640WrongMatchInvAccountingData = new Issue21640WrongMatchInvAccountingData();
        objectIssue21640WrongMatchInvAccountingData.mMatchinvId = UtilSql.getValue(result, "m_matchinv_id");
        objectIssue21640WrongMatchInvAccountingData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectIssue21640WrongMatchInvAccountingData.adOrgId = UtilSql.getValue(result, "ad_org_id");
        objectIssue21640WrongMatchInvAccountingData.matchinv = UtilSql.getValue(result, "matchinv");
        objectIssue21640WrongMatchInvAccountingData.adRoleId = UtilSql.getValue(result, "ad_role_id");
        objectIssue21640WrongMatchInvAccountingData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectIssue21640WrongMatchInvAccountingData);
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
    Issue21640WrongMatchInvAccountingData objectIssue21640WrongMatchInvAccountingData[] = new Issue21640WrongMatchInvAccountingData[vector.size()];
    vector.copyInto(objectIssue21640WrongMatchInvAccountingData);
    return(objectIssue21640WrongMatchInvAccountingData);
  }

  public static Issue21640WrongMatchInvAccountingData[] select2(ConnectionProvider connectionProvider, String client)    throws ServletException {
    return select2(connectionProvider, client, 0, 0);
  }

  public static Issue21640WrongMatchInvAccountingData[] select2(ConnectionProvider connectionProvider, String client, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT DISTINCT mi.m_matchinv_id, mi.ad_org_id, ad_column_identifier('m_matchinv', fa.record_id, 'en_US') as matchinv" +
      "      FROM fact_acct fa JOIN m_matchinv mi ON fa.record_id = mi.m_matchinv_id" +
      "      WHERE fa.ad_table_id = '472'" +
      "        AND fa.ad_client_id = ?" +
      "      GROUP BY fa.fact_acct_group_id, mi.m_matchinv_id, mi.ad_org_id, fa.record_id" +
      "      HAVING count(*) > 3";

    ResultSet result;
    Vector<Issue21640WrongMatchInvAccountingData> vector = new Vector<Issue21640WrongMatchInvAccountingData>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, client);

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
        Issue21640WrongMatchInvAccountingData objectIssue21640WrongMatchInvAccountingData = new Issue21640WrongMatchInvAccountingData();
        objectIssue21640WrongMatchInvAccountingData.mMatchinvId = UtilSql.getValue(result, "m_matchinv_id");
        objectIssue21640WrongMatchInvAccountingData.adOrgId = UtilSql.getValue(result, "ad_org_id");
        objectIssue21640WrongMatchInvAccountingData.matchinv = UtilSql.getValue(result, "matchinv");
        objectIssue21640WrongMatchInvAccountingData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectIssue21640WrongMatchInvAccountingData);
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
    Issue21640WrongMatchInvAccountingData objectIssue21640WrongMatchInvAccountingData[] = new Issue21640WrongMatchInvAccountingData[vector.size()];
    vector.copyInto(objectIssue21640WrongMatchInvAccountingData);
    return(objectIssue21640WrongMatchInvAccountingData);
  }

  public static Issue21640WrongMatchInvAccountingData[] select1(ConnectionProvider connectionProvider, String client)    throws ServletException {
    return select1(connectionProvider, client, 0, 0);
  }

  public static Issue21640WrongMatchInvAccountingData[] select1(ConnectionProvider connectionProvider, String client, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT DISTINCT mi.m_matchinv_id, mi.ad_org_id, ad_column_identifier('m_matchinv', fa.record_id, 'en_US') as matchinv" +
      "      FROM fact_acct fa" +
      "          JOIN m_matchinv mi ON fa.record_id = mi.m_matchinv_id" +
      "          JOIN m_inoutline iol ON mi.m_inoutline_id = iol.m_inoutline_id" +
      "          JOIN c_invoiceline il ON mi.c_invoiceline_id = il.c_invoiceline_id" +
      "          LEFT JOIN fact_acct far ON far.line_id = iol.m_inoutline_id AND far.account_id = fa.account_id" +
      "          LEFT JOIN fact_acct fi ON fi.line_id = il.c_invoiceline_id AND fi.account_id = fa.account_id AND fi.ad_table_id = '318'" +
      "      WHERE (round((select movementqty from m_inoutline where m_inoutline_id=mi.m_inoutline_id),2) = " +
      "            round((select qtyinvoiced from c_invoiceline where c_invoiceline_id=mi.c_invoiceline_id),2))     " +
      "        AND fa.ad_table_id = '472'" +
      "        AND (COALESCE(far.amtacctcr + far.amtacctdr, fa.amtacctdr + fa.amtacctcr) <> (fa.amtacctdr + fa.amtacctcr)" +
      "            OR COALESCE(fi.amtacctcr + fi.amtacctdr, fa.amtacctdr + fa.amtacctcr) <> (fa.amtacctdr + fa.amtacctcr))" +
      "        AND fa.ad_client_id = ?";

    ResultSet result;
    Vector<Issue21640WrongMatchInvAccountingData> vector = new Vector<Issue21640WrongMatchInvAccountingData>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, client);

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
        Issue21640WrongMatchInvAccountingData objectIssue21640WrongMatchInvAccountingData = new Issue21640WrongMatchInvAccountingData();
        objectIssue21640WrongMatchInvAccountingData.mMatchinvId = UtilSql.getValue(result, "m_matchinv_id");
        objectIssue21640WrongMatchInvAccountingData.adOrgId = UtilSql.getValue(result, "ad_org_id");
        objectIssue21640WrongMatchInvAccountingData.matchinv = UtilSql.getValue(result, "matchinv");
        objectIssue21640WrongMatchInvAccountingData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectIssue21640WrongMatchInvAccountingData);
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
    Issue21640WrongMatchInvAccountingData objectIssue21640WrongMatchInvAccountingData[] = new Issue21640WrongMatchInvAccountingData[vector.size()];
    vector.copyInto(objectIssue21640WrongMatchInvAccountingData);
    return(objectIssue21640WrongMatchInvAccountingData);
  }

  public static Issue21640WrongMatchInvAccountingData[] getClients(ConnectionProvider connectionProvider)    throws ServletException {
    return getClients(connectionProvider, 0, 0);
  }

  public static Issue21640WrongMatchInvAccountingData[] getClients(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT DISTINCT ad_client_id" +
      "      FROM c_acctschema_table" +
      "      WHERE ad_table_id = '472'" +
      "        AND isactive = 'Y'";

    ResultSet result;
    Vector<Issue21640WrongMatchInvAccountingData> vector = new Vector<Issue21640WrongMatchInvAccountingData>(0);
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
        Issue21640WrongMatchInvAccountingData objectIssue21640WrongMatchInvAccountingData = new Issue21640WrongMatchInvAccountingData();
        objectIssue21640WrongMatchInvAccountingData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectIssue21640WrongMatchInvAccountingData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectIssue21640WrongMatchInvAccountingData);
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
    Issue21640WrongMatchInvAccountingData objectIssue21640WrongMatchInvAccountingData[] = new Issue21640WrongMatchInvAccountingData[vector.size()];
    vector.copyInto(objectIssue21640WrongMatchInvAccountingData);
    return(objectIssue21640WrongMatchInvAccountingData);
  }

  public static String getAlertRuleId(ConnectionProvider connectionProvider, String name, String client)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "       SELECT MAX(ad_alertrule_id) AS name" +
      "       FROM AD_ALERTRULE" +
      "       WHERE NAME LIKE ?" +
      "         AND ISACTIVE = 'Y'" +
      "         AND AD_CLIENT_ID = ?";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, name);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, client);

      result = st.executeQuery();
      if(result.next()) {
        strReturn = UtilSql.getValue(result, "name");
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
    return(strReturn);
  }

  public static boolean existsAlertRule(ConnectionProvider connectionProvider, String alertRule, String client)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "       SELECT COUNT(*) AS EXISTING" +
      "       FROM AD_ALERTRULE" +
      "       WHERE NAME = ?" +
      "         AND ISACTIVE = 'Y'" +
      "         AND AD_CLIENT_ID = ?";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, alertRule);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, client);

      result = st.executeQuery();
      if(result.next()) {
        boolReturn = !UtilSql.getValue(result, "existing").equals("0");
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
    return(boolReturn);
  }

  public static boolean existsAlert(ConnectionProvider connectionProvider, String alertRule, String matchinv)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "       SELECT COUNT(*) AS EXISTING" +
      "       FROM AD_ALERT" +
      "       WHERE AD_ALERTRULE_ID = ?" +
      "         AND REFERENCEKEY_ID = ?" +
      "         AND ISFIXED = 'N'";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, alertRule);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, matchinv);

      result = st.executeQuery();
      if(result.next()) {
        boolReturn = !UtilSql.getValue(result, "existing").equals("0");
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
    return(boolReturn);
  }

  public static Issue21640WrongMatchInvAccountingData[] getRoleIds(ConnectionProvider connectionProvider, String window, String clientId)    throws ServletException {
    return getRoleIds(connectionProvider, window, clientId, 0, 0);
  }

  public static Issue21640WrongMatchInvAccountingData[] getRoleIds(ConnectionProvider connectionProvider, String window, String clientId, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "       SELECT distinct ad_role_id" +
      "       FROM ad_window_access" +
      "       WHERE ad_window_id = ?" +
      "       AND AD_CLIENT_ID = ?" +
      "         AND ISACTIVE = 'Y'";

    ResultSet result;
    Vector<Issue21640WrongMatchInvAccountingData> vector = new Vector<Issue21640WrongMatchInvAccountingData>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, window);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientId);

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
        Issue21640WrongMatchInvAccountingData objectIssue21640WrongMatchInvAccountingData = new Issue21640WrongMatchInvAccountingData();
        objectIssue21640WrongMatchInvAccountingData.adRoleId = UtilSql.getValue(result, "ad_role_id");
        objectIssue21640WrongMatchInvAccountingData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectIssue21640WrongMatchInvAccountingData);
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
    Issue21640WrongMatchInvAccountingData objectIssue21640WrongMatchInvAccountingData[] = new Issue21640WrongMatchInvAccountingData[vector.size()];
    vector.copyInto(objectIssue21640WrongMatchInvAccountingData);
    return(objectIssue21640WrongMatchInvAccountingData);
  }

  public static int insertAlertRule(ConnectionProvider connectionProvider, String clientId, String name, String tabId, String sql)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      INSERT INTO AD_ALERTRULE (" +
      "        AD_ALERTRULE_ID, AD_CLIENT_ID, AD_ORG_ID,ISACTIVE," +
      "        CREATED, CREATEDBY,  UPDATED, UPDATEDBY," +
      "        NAME, AD_TAB_ID, FILTERCLAUSE, TYPE," +
      "        SQL" +
      "      ) VALUES (" +
      "        get_uuid(), ?, '0', 'Y'," +
      "        now(), '100', now(), '100'," +
      "        ?, ?, '', 'E'," +
      "        ?" +
      "      )";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, name);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, tabId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, sql);

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

  public static int updateAlertRule(ConnectionProvider connectionProvider, String clientId, String name)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE AD_AlertRule " +
      "        SET SQL='', TYPE='E' WHERE AD_Client_ID = ? AND NAME = ?";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, name);

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

  public static int insertAlert(ConnectionProvider connectionProvider, String client, String org, String description, String adAlertRuleId, String recordId, String referencekey_id)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      INSERT INTO AD_Alert (" +
      "        AD_Alert_ID, AD_Client_ID, AD_Org_ID, IsActive," +
      "        Created, CreatedBy, Updated, UpdatedBy," +
      "        Description, AD_AlertRule_ID, Record_Id, Referencekey_ID" +
      "      ) VALUES (" +
      "        get_uuid(), ?, ?, 'Y'," +
      "        NOW(), '0', NOW(), '0'," +
      "        ?, ?, ?, ?)";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, client);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, org);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, description);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, adAlertRuleId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, recordId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, referencekey_id);

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

  public static int insertAlertRecipient(ConnectionProvider connectionProvider, String client, String org, String adAlertRuleId, String role)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "    INSERT INTO ad_alertrecipient(" +
      "            ad_user_id, ad_client_id, ad_org_id, isactive, created, createdby, " +
      "            updated, updatedby, ad_alertrecipient_id, ad_alertrule_id, ad_role_id, " +
      "            sendemail)" +
      "    VALUES (null, ?, ?, 'Y', now(), '100', " +
      "            now(), '100', get_uuid(), ?, ?, " +
      "            'N')";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, client);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, org);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, adAlertRuleId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, role);

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
