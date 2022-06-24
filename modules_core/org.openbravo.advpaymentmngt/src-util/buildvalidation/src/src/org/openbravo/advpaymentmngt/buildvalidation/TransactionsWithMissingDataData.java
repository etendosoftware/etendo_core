//Sqlc generated V1.O00-1
package org.openbravo.advpaymentmngt.buildvalidation;

import java.sql.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;

import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.data.UtilSql;
import java.util.*;

class TransactionsWithMissingDataData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String documentno;
  public String finFinaccTransactionId;
  public String adClientId;
  public String adOrgId;
  public String adRoleId;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("documentno"))
      return documentno;
    else if (fieldName.equalsIgnoreCase("fin_finacc_transaction_id") || fieldName.equals("finFinaccTransactionId"))
      return finFinaccTransactionId;
    else if (fieldName.equalsIgnoreCase("ad_client_id") || fieldName.equals("adClientId"))
      return adClientId;
    else if (fieldName.equalsIgnoreCase("ad_org_id") || fieldName.equals("adOrgId"))
      return adOrgId;
    else if (fieldName.equalsIgnoreCase("ad_role_id") || fieldName.equals("adRoleId"))
      return adRoleId;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static TransactionsWithMissingDataData[] dummy(ConnectionProvider connectionProvider)    throws ServletException {
    return dummy(connectionProvider, 0, 0);
  }

  public static TransactionsWithMissingDataData[] dummy(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT '' AS documentno, '' AS fin_finacc_transaction_id, '' AS ad_client_id," +
      "             '' AS ad_org_id, '' AS ad_role_id" +
      "      FROM DUAL";

    ResultSet result;
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
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
        TransactionsWithMissingDataData objectTransactionsWithMissingDataData = new TransactionsWithMissingDataData();
        objectTransactionsWithMissingDataData.documentno = UtilSql.getValue(result, "documentno");
        objectTransactionsWithMissingDataData.finFinaccTransactionId = UtilSql.getValue(result, "fin_finacc_transaction_id");
        objectTransactionsWithMissingDataData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectTransactionsWithMissingDataData.adOrgId = UtilSql.getValue(result, "ad_org_id");
        objectTransactionsWithMissingDataData.adRoleId = UtilSql.getValue(result, "ad_role_id");
        objectTransactionsWithMissingDataData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectTransactionsWithMissingDataData);
        if (countRecord >= numberRegisters && numberRegisters != 0) {
          continueResult = false;
        }
      }
      result.close();
    } catch(SQLException e){
      log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch(Exception ex){
      log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch(Exception ignore){
        ignore.printStackTrace();
      }
    }
    TransactionsWithMissingDataData objectTransactionsWithMissingDataData[] = new TransactionsWithMissingDataData[vector.size()];
    vector.copyInto(objectTransactionsWithMissingDataData);
    return(objectTransactionsWithMissingDataData);
  }

/**
This query returns transactions with deposit and payment amount equal to Zero and/or Transaction Date null and/or Accounting Date null
 */
  public static TransactionsWithMissingDataData[] selectTransactionsWithMissingData(ConnectionProvider connectionProvider)    throws ServletException {
    return selectTransactionsWithMissingData(connectionProvider, 0, 0);
  }

/**
This query returns transactions with deposit and payment amount equal to Zero and/or Transaction Date null and/or Accounting Date null
 */
  public static TransactionsWithMissingDataData[] selectTransactionsWithMissingData(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT ad_column_identifier('fin_finacc_transaction_id', fin_finacc_transaction_id, 'en_US') as documentno, fin_finacc_transaction_id, ad_client_id, ad_org_id " +
      "      FROM fin_finacc_transaction" +
      "      WHERE  statementdate IS NULL " +
      "      OR dateacct IS NULL";

    ResultSet result;
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
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
        TransactionsWithMissingDataData objectTransactionsWithMissingDataData = new TransactionsWithMissingDataData();
        objectTransactionsWithMissingDataData.documentno = UtilSql.getValue(result, "documentno");
        objectTransactionsWithMissingDataData.finFinaccTransactionId = UtilSql.getValue(result, "fin_finacc_transaction_id");
        objectTransactionsWithMissingDataData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectTransactionsWithMissingDataData.adOrgId = UtilSql.getValue(result, "ad_org_id");
        objectTransactionsWithMissingDataData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectTransactionsWithMissingDataData);
        if (countRecord >= numberRegisters && numberRegisters != 0) {
          continueResult = false;
        }
      }
      result.close();
    } catch(SQLException e){
      log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch(Exception ex){
      log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch(Exception ignore){
        ignore.printStackTrace();
      }
    }
    TransactionsWithMissingDataData objectTransactionsWithMissingDataData[] = new TransactionsWithMissingDataData[vector.size()];
    vector.copyInto(objectTransactionsWithMissingDataData);
    return(objectTransactionsWithMissingDataData);
  }

/**
Check if the FIN_Finacc_Transaction table exist
 */
  public static boolean existAPRMbasetables(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "       SELECT count(*) AS EXISTING" +
      "       FROM ad_table" +
      "       WHERE ad_table_id = '4D8C3B3C31D1410DA046140C9F024D17'";

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
      log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch(Exception ex){
      log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch(Exception ignore){
        ignore.printStackTrace();
      }
    }
    return(boolReturn);
  }
}
