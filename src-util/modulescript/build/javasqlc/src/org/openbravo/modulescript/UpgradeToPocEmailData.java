//Sqlc generated V1.O00-1
package org.openbravo.modulescript;

import java.sql.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import jakarta.servlet.ServletException;

import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.data.UtilSql;
import org.openbravo.database.SessionInfo;
import java.util.*;

@SuppressWarnings("serial")
class UpgradeToPocEmailData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String adClientId;
  public String adOrgId;
  public String isactive;
  public String cPocConfigurationId;
  public String server;
  public String senderaddress;
  public String auth;
  public String accountname;
  public String accountpass;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("ad_client_id") || fieldName.equals("adClientId"))
      return adClientId;
    else if (fieldName.equalsIgnoreCase("ad_org_id") || fieldName.equals("adOrgId"))
      return adOrgId;
    else if (fieldName.equalsIgnoreCase("isactive"))
      return isactive;
    else if (fieldName.equalsIgnoreCase("c_poc_configuration_id") || fieldName.equals("cPocConfigurationId"))
      return cPocConfigurationId;
    else if (fieldName.equalsIgnoreCase("server"))
      return server;
    else if (fieldName.equalsIgnoreCase("senderaddress"))
      return senderaddress;
    else if (fieldName.equalsIgnoreCase("auth"))
      return auth;
    else if (fieldName.equalsIgnoreCase("accountname"))
      return accountname;
    else if (fieldName.equalsIgnoreCase("accountpass"))
      return accountpass;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static UpgradeToPocEmailData[] newConfigurationData(ConnectionProvider connectionProvider)    throws ServletException {
    return newConfigurationData(connectionProvider, 0, 0);
  }

  public static UpgradeToPocEmailData[] newConfigurationData(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT ad_client_id, ad_org_id, isactive, c_poc_configuration_id, smtpserver as server, smtpserversenderaddress as senderaddress, issmtpauthorization as auth, smtpserveraccount as accountname, smtpserverpassword as accountpass FROM c_poc_configuration";

    ResultSet result;
    Vector<UpgradeToPocEmailData> vector = new Vector<UpgradeToPocEmailData>(0);
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
        UpgradeToPocEmailData objectUpgradeToPocEmailData = new UpgradeToPocEmailData();
        objectUpgradeToPocEmailData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectUpgradeToPocEmailData.adOrgId = UtilSql.getValue(result, "ad_org_id");
        objectUpgradeToPocEmailData.isactive = UtilSql.getValue(result, "isactive");
        objectUpgradeToPocEmailData.cPocConfigurationId = UtilSql.getValue(result, "c_poc_configuration_id");
        objectUpgradeToPocEmailData.server = UtilSql.getValue(result, "server");
        objectUpgradeToPocEmailData.senderaddress = UtilSql.getValue(result, "senderaddress");
        objectUpgradeToPocEmailData.auth = UtilSql.getValue(result, "auth");
        objectUpgradeToPocEmailData.accountname = UtilSql.getValue(result, "accountname");
        objectUpgradeToPocEmailData.accountpass = UtilSql.getValue(result, "accountpass");
        objectUpgradeToPocEmailData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUpgradeToPocEmailData);
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
    UpgradeToPocEmailData objectUpgradeToPocEmailData[] = new UpgradeToPocEmailData[vector.size()];
    vector.copyInto(objectUpgradeToPocEmailData);
    return(objectUpgradeToPocEmailData);
  }

  public static UpgradeToPocEmailData[] oldConfigurationData(ConnectionProvider connectionProvider)    throws ServletException {
    return oldConfigurationData(connectionProvider, 0, 0);
  }

  public static UpgradeToPocEmailData[] oldConfigurationData(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT ad_client_id, ad_org_id, smtphost as server, requestemail as senderaddress, issmtpauthorization as auth, requestuser as accountname, requestuserpw as accountpass FROM ad_client";

    ResultSet result;
    Vector<UpgradeToPocEmailData> vector = new Vector<UpgradeToPocEmailData>(0);
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
        UpgradeToPocEmailData objectUpgradeToPocEmailData = new UpgradeToPocEmailData();
        objectUpgradeToPocEmailData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectUpgradeToPocEmailData.adOrgId = UtilSql.getValue(result, "ad_org_id");
        objectUpgradeToPocEmailData.server = UtilSql.getValue(result, "server");
        objectUpgradeToPocEmailData.senderaddress = UtilSql.getValue(result, "senderaddress");
        objectUpgradeToPocEmailData.auth = UtilSql.getValue(result, "auth");
        objectUpgradeToPocEmailData.accountname = UtilSql.getValue(result, "accountname");
        objectUpgradeToPocEmailData.accountpass = UtilSql.getValue(result, "accountpass");
        objectUpgradeToPocEmailData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUpgradeToPocEmailData);
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
    UpgradeToPocEmailData objectUpgradeToPocEmailData[] = new UpgradeToPocEmailData[vector.size()];
    vector.copyInto(objectUpgradeToPocEmailData);
    return(objectUpgradeToPocEmailData);
  }

  public static int deleteOldConfigurationData(ConnectionProvider connectionProvider, String adClientId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE ad_client SET" +
      "           smtphost = NULL," +
      "           requestemail = NULL," +
      "           issmtpauthorization = 'N'," +
      "           requestuser = NULL," +
      "           requestuserpw = NULL," +
      "           requestfolder = NULL," +
      "           updated = now()," +
      "           updatedby = '0'" +
      "        WHERE" +
      "           ad_client_id = ?";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, adClientId);

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

  public static int insertNewConfigurationData(Connection conn, ConnectionProvider connectionProvider, String adClientId, String smtpserver, String smtpserversenderaddress, String issmtpauthorization, String smtpserveraccount, String smtpserverpassword)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      INSERT INTO c_poc_configuration(" +
      "            ad_client_id, ad_org_id, c_poc_configuration_id, smtpserver, smtpserversenderaddress," +
      "            issmtpauthorization, smtpserveraccount, smtpserverpassword, isactive," +
      "            created, createdby, updated, updatedby)" +
      "      VALUES (?, '0', GET_UUID(), ?, ?," +
      "            ?, ?, ?, 'Y'," +
      "            now(), '0', now(), '0')";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(conn, strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, adClientId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, smtpserver);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, smtpserversenderaddress);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, issmtpauthorization);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, smtpserveraccount);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, smtpserverpassword);

      SessionInfo.saveContextInfoIntoDB(conn);
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
        connectionProvider.releaseTransactionalPreparedStatement(st);
      } catch(Exception e){
        log4j.error("Error during release*Statement of query: " + strSql, e);
      }
    }
    return(updateCount);
  }

  public static int changeNewConfigurationDataOrg(ConnectionProvider connectionProvider, String cPocConfigurationId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE c_poc_configuration SET" +
      "           ad_org_id = '0'," +
      "           updated = now()," +
      "           updatedby = '0'" +
      "        WHERE" +
      "           c_poc_configuration_id = ?";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, cPocConfigurationId);

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
