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
class UpdateEmailPasswordsData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String cPocConfigurationId;
  public String smtpserverpassword;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("c_poc_configuration_id") || fieldName.equals("cPocConfigurationId"))
      return cPocConfigurationId;
    else if (fieldName.equalsIgnoreCase("smtpserverpassword"))
      return smtpserverpassword;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static UpdateEmailPasswordsData[] select(ConnectionProvider connectionProvider)    throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static UpdateEmailPasswordsData[] select(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT C_POC_CONFIGURATION_ID, SMTPSERVERPASSWORD FROM C_POC_CONFIGURATION";

    ResultSet result;
    Vector<UpdateEmailPasswordsData> vector = new Vector<UpdateEmailPasswordsData>(0);
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
        UpdateEmailPasswordsData objectUpdateEmailPasswordsData = new UpdateEmailPasswordsData();
        objectUpdateEmailPasswordsData.cPocConfigurationId = UtilSql.getValue(result, "c_poc_configuration_id");
        objectUpdateEmailPasswordsData.smtpserverpassword = UtilSql.getValue(result, "smtpserverpassword");
        objectUpdateEmailPasswordsData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUpdateEmailPasswordsData);
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
    UpdateEmailPasswordsData objectUpdateEmailPasswordsData[] = new UpdateEmailPasswordsData[vector.size()];
    vector.copyInto(objectUpdateEmailPasswordsData);
    return(objectUpdateEmailPasswordsData);
  }

  public static int update(ConnectionProvider connectionProvider, String password, String c_poc_configuration)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      UPDATE C_POC_CONFIGURATION SET SMTPSERVERPASSWORD = ? WHERE C_POC_CONFIGURATION_ID = ?";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, password);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, c_poc_configuration);

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
