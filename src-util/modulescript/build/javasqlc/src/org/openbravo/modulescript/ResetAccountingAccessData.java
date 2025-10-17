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
class ResetAccountingAccessData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String exist;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("exist"))
      return exist;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static ResetAccountingAccessData[] select(ConnectionProvider connectionProvider)    throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static ResetAccountingAccessData[] select(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT '' as exist FROM DUAL";

    ResultSet result;
    Vector<ResetAccountingAccessData> vector = new Vector<ResetAccountingAccessData>(0);
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
        ResetAccountingAccessData objectResetAccountingAccessData = new ResetAccountingAccessData();
        objectResetAccountingAccessData.exist = UtilSql.getValue(result, "exist");
        objectResetAccountingAccessData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectResetAccountingAccessData);
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
    ResetAccountingAccessData objectResetAccountingAccessData[] = new ResetAccountingAccessData[vector.size()];
    vector.copyInto(objectResetAccountingAccessData);
    return(objectResetAccountingAccessData);
  }

  public static int insert(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        INSERT INTO OBUIAPP_Process_Access" +
      "        (" +
      "          OBUIAPP_Process_Access_ID, OBUIAPP_Process_ID, AD_Role_ID, AD_Client_ID," +
      "          AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy" +
      "        )" +
      "        SELECT" +
      "          get_uuid(), 'C6ED4B93E0D54C08A57072AEEC40E6EC', ad_role_id, ad_client_id," +
      "          ad_org_id, isactive, to_date(now()), '0', to_date(now()), '0'" +
      "        FROM AD_Process_Access apa" +
      "        WHERE ad_process_id = 'E264309FF8244A94936502BF51829109'" +
      "        AND NOT EXISTS (" +
      "          SELECT 1" +
      "          FROM OBUIAPP_Process_Access opa" +
      "          WHERE opa.ad_role_id = apa.ad_role_id" +
      "          AND opa.obuiapp_process_id = 'C6ED4B93E0D54C08A57072AEEC40E6EC'" +
      "        )";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

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
