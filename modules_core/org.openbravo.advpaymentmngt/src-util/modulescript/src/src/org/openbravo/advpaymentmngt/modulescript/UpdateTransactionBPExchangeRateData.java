//Sqlc generated V1.O00-1
package org.openbravo.advpaymentmngt.modulescript;

import java.sql.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;

import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.data.UtilSql;
import java.util.*;

class UpdateTransactionBPExchangeRateData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String name;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("name"))
      return name;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static UpdateTransactionBPExchangeRateData[] select(ConnectionProvider connectionProvider)    throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static UpdateTransactionBPExchangeRateData[] select(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT 1 as name from dual";

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
        UpdateTransactionBPExchangeRateData objectUpdateTransactionBPExchangeRateData = new UpdateTransactionBPExchangeRateData();
        objectUpdateTransactionBPExchangeRateData.name = UtilSql.getValue(result, "name");
        objectUpdateTransactionBPExchangeRateData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUpdateTransactionBPExchangeRateData);
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
    UpdateTransactionBPExchangeRateData objectUpdateTransactionBPExchangeRateData[] = new UpdateTransactionBPExchangeRateData[vector.size()];
    vector.copyInto(objectUpdateTransactionBPExchangeRateData);
    return(objectUpdateTransactionBPExchangeRateData);
  }

  public static boolean selectCheckBP(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT COUNT(1) AS NAME FROM DUAL" +
      "        WHERE EXISTS(SELECT 1 FROM FIN_FINACC_TRANSACTION FT" +
      "                     LEFT JOIN FIN_PAYMENT FP ON FP.FIN_PAYMENT_ID = FT.FIN_PAYMENT_ID " +
      "                     WHERE FT.C_BPARTNER_ID IS NULL AND FP.C_BPARTNER_ID IS NOT NULL)";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if(result.next()) {
        boolReturn = !UtilSql.getValue(result, "name").equals("0");
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

  public static int updateBP(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE FIN_FINACC_TRANSACTION SET C_BPARTNER_ID = (SELECT C_BPARTNER_ID FROM FIN_PAYMENT WHERE FIN_PAYMENT_ID = FIN_FINACC_TRANSACTION.FIN_PAYMENT_ID)" +
      "        WHERE EXISTS(SELECT 1 FROM FIN_FINACC_TRANSACTION FT " +
      "                     LEFT JOIN FIN_PAYMENT FP ON FP.FIN_PAYMENT_ID = FT.FIN_PAYMENT_ID" +
      "                     WHERE FT.C_BPARTNER_ID IS NULL AND FP.C_BPARTNER_ID IS NOT NULL" +
      "                     AND FT.FIN_FINACC_TRANSACTION_ID = FIN_FINACC_TRANSACTION.FIN_FINACC_TRANSACTION_ID)";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      updateCount = st.executeUpdate();
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
    return(updateCount);
  }

  public static boolean selectCheckExchange(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT COUNT(1) AS NAME FROM DUAL" +
      "        WHERE EXISTS(SELECT 1 FROM C_CONVERSION_RATE_DOCUMENT" +
      "                              WHERE APRM_FINACC_TRANSACTION_V_ID IS NOT NULL AND FIN_FINACC_TRANSACTION_ID IS NULL)";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if(result.next()) {
        boolReturn = !UtilSql.getValue(result, "name").equals("0");
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

  public static int updateExchange(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE C_CONVERSION_RATE_DOCUMENT SET FIN_FINACC_TRANSACTION_ID = APRM_FINACC_TRANSACTION_V_ID" +
      "        WHERE APRM_FINACC_TRANSACTION_V_ID IS NOT NULL AND FIN_FINACC_TRANSACTION_ID IS NULL";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      updateCount = st.executeUpdate();
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
    return(updateCount);
  }
}
