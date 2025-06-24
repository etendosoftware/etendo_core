//Sqlc generated V1.O00-1
package org.openbravo.buildvalidation;

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
class DuplicateDocExchangeRateData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String documentno;
  public String referencekeyId;
  public String isreceipt;
  public String adClientId;
  public String adOrgId;
  public String adRoleId;
  public String issotrx;
  public String recordinfo;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("documentno"))
      return documentno;
    else if (fieldName.equalsIgnoreCase("referencekey_id") || fieldName.equals("referencekeyId"))
      return referencekeyId;
    else if (fieldName.equalsIgnoreCase("isreceipt"))
      return isreceipt;
    else if (fieldName.equalsIgnoreCase("ad_client_id") || fieldName.equals("adClientId"))
      return adClientId;
    else if (fieldName.equalsIgnoreCase("ad_org_id") || fieldName.equals("adOrgId"))
      return adOrgId;
    else if (fieldName.equalsIgnoreCase("ad_role_id") || fieldName.equals("adRoleId"))
      return adRoleId;
    else if (fieldName.equalsIgnoreCase("issotrx"))
      return issotrx;
    else if (fieldName.equalsIgnoreCase("recordinfo"))
      return recordinfo;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static DuplicateDocExchangeRateData[] dummy(ConnectionProvider connectionProvider)    throws ServletException {
    return dummy(connectionProvider, 0, 0);
  }

  public static DuplicateDocExchangeRateData[] dummy(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT '' AS documentno, '' AS referencekey_id, '' AS isreceipt, '' AS ad_client_id," +
      "             '' AS ad_org_id, '' AS ad_role_id, '' AS issotrx, '' AS recordinfo" +
      "      FROM DUAL";

    ResultSet result;
    Vector<DuplicateDocExchangeRateData> vector = new Vector<DuplicateDocExchangeRateData>(0);
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
        DuplicateDocExchangeRateData objectDuplicateDocExchangeRateData = new DuplicateDocExchangeRateData();
        objectDuplicateDocExchangeRateData.documentno = UtilSql.getValue(result, "documentno");
        objectDuplicateDocExchangeRateData.referencekeyId = UtilSql.getValue(result, "referencekey_id");
        objectDuplicateDocExchangeRateData.isreceipt = UtilSql.getValue(result, "isreceipt");
        objectDuplicateDocExchangeRateData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectDuplicateDocExchangeRateData.adOrgId = UtilSql.getValue(result, "ad_org_id");
        objectDuplicateDocExchangeRateData.adRoleId = UtilSql.getValue(result, "ad_role_id");
        objectDuplicateDocExchangeRateData.issotrx = UtilSql.getValue(result, "issotrx");
        objectDuplicateDocExchangeRateData.recordinfo = UtilSql.getValue(result, "recordinfo");
        objectDuplicateDocExchangeRateData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectDuplicateDocExchangeRateData);
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
    DuplicateDocExchangeRateData objectDuplicateDocExchangeRateData[] = new DuplicateDocExchangeRateData[vector.size()];
    vector.copyInto(objectDuplicateDocExchangeRateData);
    return(objectDuplicateDocExchangeRateData);
  }

  public static DuplicateDocExchangeRateData[] selectDupInvoiceExcRate(ConnectionProvider connectionProvider)    throws ServletException {
    return selectDupInvoiceExcRate(connectionProvider, 0, 0);
  }

  public static DuplicateDocExchangeRateData[] selectDupInvoiceExcRate(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "             select crd.c_invoice_id as referencekey_id, i.issotrx, crd.ad_client_id, crd.ad_org_id," +
      "                    ad_column_identifier('C_INVOICE', crd.c_invoice_id,'en_US') as recordinfo" +
      "             from c_conversion_rate_document crd join c_invoice i on (crd.c_invoice_id = i.c_invoice_id)" +
      "             where crd.fin_payment_id is null and crd.aprm_finacc_transaction_v_id is null" +
      "             group by crd.c_currency_id, crd.c_currency_id_to, crd.c_invoice_id, crd.fin_payment_id," +
      "                      crd.aprm_finacc_transaction_v_id, crd.ad_org_id, crd.ad_client_id, i.issotrx" +
      "             having count(*) > 1";

    ResultSet result;
    Vector<DuplicateDocExchangeRateData> vector = new Vector<DuplicateDocExchangeRateData>(0);
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
        DuplicateDocExchangeRateData objectDuplicateDocExchangeRateData = new DuplicateDocExchangeRateData();
        objectDuplicateDocExchangeRateData.referencekeyId = UtilSql.getValue(result, "referencekey_id");
        objectDuplicateDocExchangeRateData.issotrx = UtilSql.getValue(result, "issotrx");
        objectDuplicateDocExchangeRateData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectDuplicateDocExchangeRateData.adOrgId = UtilSql.getValue(result, "ad_org_id");
        objectDuplicateDocExchangeRateData.recordinfo = UtilSql.getValue(result, "recordinfo");
        objectDuplicateDocExchangeRateData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectDuplicateDocExchangeRateData);
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
    DuplicateDocExchangeRateData objectDuplicateDocExchangeRateData[] = new DuplicateDocExchangeRateData[vector.size()];
    vector.copyInto(objectDuplicateDocExchangeRateData);
    return(objectDuplicateDocExchangeRateData);
  }

  public static DuplicateDocExchangeRateData[] selectDupPaymentExcRate(ConnectionProvider connectionProvider)    throws ServletException {
    return selectDupPaymentExcRate(connectionProvider, 0, 0);
  }

  public static DuplicateDocExchangeRateData[] selectDupPaymentExcRate(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "             select crd.fin_payment_id as referencekey_id, p.isreceipt, crd.ad_client_id, crd.ad_org_id," +
      "                    ad_column_identifier('FIN_PAYMENT', crd.fin_payment_id,'en_US') as recordinfo" +
      "             from c_conversion_rate_document crd join fin_payment p on (crd.fin_payment_id = p.fin_payment_id)" +
      "             where crd.c_invoice_id is null and crd.aprm_finacc_transaction_v_id is null" +
      "             group by crd.c_currency_id, crd.c_currency_id_to, crd.c_invoice_id, crd.fin_payment_id, crd.aprm_finacc_transaction_v_id," +
      "                      crd.ad_org_id, crd.ad_client_id, p.isreceipt" +
      "             having count(*) > 1";

    ResultSet result;
    Vector<DuplicateDocExchangeRateData> vector = new Vector<DuplicateDocExchangeRateData>(0);
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
        DuplicateDocExchangeRateData objectDuplicateDocExchangeRateData = new DuplicateDocExchangeRateData();
        objectDuplicateDocExchangeRateData.referencekeyId = UtilSql.getValue(result, "referencekey_id");
        objectDuplicateDocExchangeRateData.isreceipt = UtilSql.getValue(result, "isreceipt");
        objectDuplicateDocExchangeRateData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectDuplicateDocExchangeRateData.adOrgId = UtilSql.getValue(result, "ad_org_id");
        objectDuplicateDocExchangeRateData.recordinfo = UtilSql.getValue(result, "recordinfo");
        objectDuplicateDocExchangeRateData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectDuplicateDocExchangeRateData);
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
    DuplicateDocExchangeRateData objectDuplicateDocExchangeRateData[] = new DuplicateDocExchangeRateData[vector.size()];
    vector.copyInto(objectDuplicateDocExchangeRateData);
    return(objectDuplicateDocExchangeRateData);
  }

  public static DuplicateDocExchangeRateData[] selectDupTrxExcRate(ConnectionProvider connectionProvider)    throws ServletException {
    return selectDupTrxExcRate(connectionProvider, 0, 0);
  }

  public static DuplicateDocExchangeRateData[] selectDupTrxExcRate(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "             select crd.aprm_finacc_transaction_v_id as referencekey_id, crd.ad_client_id, crd.ad_org_id," +
      "                    ad_column_identifier('FIN_FINACC_TRANSACTION', crd.aprm_finacc_transaction_v_id,'en_US') as recordinfo" +
      "             from c_conversion_rate_document crd join fin_finacc_transaction ft on (crd.aprm_finacc_transaction_v_id = ft.fin_finacc_transaction_id)" +
      "             where crd.c_invoice_id is null and crd.fin_payment_id is null" +
      "             group by crd.c_currency_id, crd.c_currency_id_to, crd.c_invoice_id, crd.fin_payment_id, crd.aprm_finacc_transaction_v_id," +
      "                      crd.ad_org_id, crd.ad_client_id" +
      "             having count(*) > 1";

    ResultSet result;
    Vector<DuplicateDocExchangeRateData> vector = new Vector<DuplicateDocExchangeRateData>(0);
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
        DuplicateDocExchangeRateData objectDuplicateDocExchangeRateData = new DuplicateDocExchangeRateData();
        objectDuplicateDocExchangeRateData.referencekeyId = UtilSql.getValue(result, "referencekey_id");
        objectDuplicateDocExchangeRateData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectDuplicateDocExchangeRateData.adOrgId = UtilSql.getValue(result, "ad_org_id");
        objectDuplicateDocExchangeRateData.recordinfo = UtilSql.getValue(result, "recordinfo");
        objectDuplicateDocExchangeRateData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectDuplicateDocExchangeRateData);
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
    DuplicateDocExchangeRateData objectDuplicateDocExchangeRateData[] = new DuplicateDocExchangeRateData[vector.size()];
    vector.copyInto(objectDuplicateDocExchangeRateData);
    return(objectDuplicateDocExchangeRateData);
  }

  public static String getAlertRuleId(ConnectionProvider connectionProvider, String name, String client)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "       SELECT MAX(ad_alertrule_id) AS name" +
      "       FROM AD_ALERTRULE" +
      "       WHERE NAME = ?" +
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

  public static boolean existsAlert(ConnectionProvider connectionProvider, String alertRule, String payment)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "       SELECT COUNT(*) AS EXISTING" +
      "       FROM AD_ALERT" +
      "       WHERE AD_ALERTRULE_ID = ?" +
      "       AND REFERENCEKEY_ID = ?" +
      "       AND ISFIXED = 'N'";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, alertRule);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, payment);

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

  public static int insertAlertRule(ConnectionProvider connectionProvider, String clientId, String orgId, String name, String tabId, String sql)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      INSERT INTO AD_ALERTRULE (" +
      "        AD_ALERTRULE_ID, AD_CLIENT_ID, AD_ORG_ID,ISACTIVE," +
      "        CREATED, CREATEDBY,  UPDATED, UPDATEDBY," +
      "        NAME, AD_TAB_ID, FILTERCLAUSE, TYPE," +
      "        SQL" +
      "      ) VALUES (" +
      "        get_uuid(), ?, ?, 'Y'," +
      "        now(), '100', now(), '100'," +
      "        ?, ?, '', 'D'," +
      "        ?" +
      "      )";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, orgId);
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

  public static int insertAlert(ConnectionProvider connectionProvider, String client, String description, String adAlertRuleId, String recordId, String referencekey_id)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      INSERT INTO AD_Alert (" +
      "        AD_Alert_ID, AD_Client_ID, AD_Org_ID, IsActive," +
      "        Created, CreatedBy, Updated, UpdatedBy," +
      "        Description, AD_AlertRule_ID, Record_Id, Referencekey_ID" +
      "      ) VALUES (" +
      "        get_uuid(), ?, '0', 'Y'," +
      "        NOW(), '0', NOW(), '0'," +
      "        ?, ?, ?, ?)";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, client);
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

  public static DuplicateDocExchangeRateData[] getRoleId(ConnectionProvider connectionProvider, String window, String clientId)    throws ServletException {
    return getRoleId(connectionProvider, window, clientId, 0, 0);
  }

  public static DuplicateDocExchangeRateData[] getRoleId(ConnectionProvider connectionProvider, String window, String clientId, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "       SELECT distinct r.ad_role_id" +
      "       FROM ad_window_access wa join ad_role r on (wa.ad_role_id=r.ad_role_id)" +
      "       WHERE wa.ad_window_id = ?" +
      "             AND wa.ad_client_id = ?" +
      "             AND wa.isactive = 'Y'" +
      "             AND r.isactive = 'Y'" +
      "             AND r.ismanual = 'Y'";

    ResultSet result;
    Vector<DuplicateDocExchangeRateData> vector = new Vector<DuplicateDocExchangeRateData>(0);
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
        DuplicateDocExchangeRateData objectDuplicateDocExchangeRateData = new DuplicateDocExchangeRateData();
        objectDuplicateDocExchangeRateData.adRoleId = UtilSql.getValue(result, "ad_role_id");
        objectDuplicateDocExchangeRateData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectDuplicateDocExchangeRateData);
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
    DuplicateDocExchangeRateData objectDuplicateDocExchangeRateData[] = new DuplicateDocExchangeRateData[vector.size()];
    vector.copyInto(objectDuplicateDocExchangeRateData);
    return(objectDuplicateDocExchangeRateData);
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

/**
Check if the C_Conversion_Rate_Document table exist
 */
  public static boolean existConvRateDoctable(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "       SELECT count(*) AS EXISTING" +
      "       FROM ad_table" +
      "       WHERE ad_table_id = 'FF808181308EA42301308FB5F7BC0049'";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

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
}
