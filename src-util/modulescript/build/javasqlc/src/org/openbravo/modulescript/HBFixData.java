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
class HBFixData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String isheartbeatactive;
  public String adProcessRequestId;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("isheartbeatactive"))
      return isheartbeatactive;
    else if (fieldName.equals("adProcessRequestId"))
      return adProcessRequestId;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static String select(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT isHeartbeatActive FROM AD_System_Info";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if(result.next()) {
        strReturn = UtilSql.getValue(result, "isheartbeatactive");
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

  public static HBFixData[] selectScheduled(ConnectionProvider connectionProvider, String adProcessId)    throws ServletException {
    return selectScheduled(connectionProvider, adProcessId, 0, 0);
  }

  public static HBFixData[] selectScheduled(ConnectionProvider connectionProvider, String adProcessId, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT ad_process_request_id" +
      "        FROM ad_process_request" +
      "       WHERE status = 'SCH'" +
      "         AND ad_process_id = ?";

    ResultSet result;
    Vector<HBFixData> vector = new Vector<HBFixData>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, adProcessId);

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
        HBFixData objectHBFixData = new HBFixData();
        objectHBFixData.adProcessRequestId = UtilSql.getValue(result, "ad_process_request_id");
        objectHBFixData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectHBFixData);
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
    HBFixData objectHBFixData[] = new HBFixData[vector.size()];
    vector.copyInto(objectHBFixData);
    return(objectHBFixData);
  }

  public static HBFixData[] selectUnscheduled(ConnectionProvider connectionProvider, String adProcessId)    throws ServletException {
    return selectUnscheduled(connectionProvider, adProcessId, 0, 0);
  }

  public static HBFixData[] selectUnscheduled(ConnectionProvider connectionProvider, String adProcessId, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT ad_process_request_id" +
      "        FROM ad_process_request" +
      "       WHERE status = 'UNS'" +
      "         AND ad_process_id = ?" +
      "       ORDER BY created";

    ResultSet result;
    Vector<HBFixData> vector = new Vector<HBFixData>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, adProcessId);

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
        HBFixData objectHBFixData = new HBFixData();
        objectHBFixData.adProcessRequestId = UtilSql.getValue(result, "ad_process_request_id");
        objectHBFixData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectHBFixData);
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
    HBFixData objectHBFixData[] = new HBFixData[vector.size()];
    vector.copyInto(objectHBFixData);
    return(objectHBFixData);
  }

  public static HBFixData[] selectMisfired(ConnectionProvider connectionProvider, String adProcessId)    throws ServletException {
    return selectMisfired(connectionProvider, adProcessId, 0, 0);
  }

  public static HBFixData[] selectMisfired(ConnectionProvider connectionProvider, String adProcessId, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT ad_process_request_id" +
      "        FROM ad_process_request" +
      "       WHERE status = 'MIS'" +
      "         AND ad_process_id = ?" +
      "       ORDER BY created";

    ResultSet result;
    Vector<HBFixData> vector = new Vector<HBFixData>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, adProcessId);

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
        HBFixData objectHBFixData = new HBFixData();
        objectHBFixData.adProcessRequestId = UtilSql.getValue(result, "ad_process_request_id");
        objectHBFixData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectHBFixData);
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
    HBFixData objectHBFixData[] = new HBFixData[vector.size()];
    vector.copyInto(objectHBFixData);
    return(objectHBFixData);
  }

  public static int updateToScheduled(ConnectionProvider connectionProvider, String adProcessRequestId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      UPDATE ad_process_request" +
      "         SET status = 'SCH'" +
      "       WHERE ad_process_request_id = ?";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, adProcessRequestId);

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

  public static int deleteDuplicated(ConnectionProvider connectionProvider, String adProcessRequestId, String adProcessId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      DELETE FROM ad_process_request" +
      "       WHERE ad_process_request_id <> ?" +
      "         AND ad_process_id = ?" +
      "         AND (status = 'MIS' OR status = 'UNS')";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, adProcessRequestId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, adProcessId);

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

  public static int insert(ConnectionProvider connectionProvider, String obContext)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      INSERT INTO ad_process_request" +
      "      (ad_process_request_id, ad_client_id, ad_org_id, isactive," +
      "       created, createdby, updated, updatedby, ad_process_id," +
      "       ad_user_id, isrolesecurity, ob_context," +
      "       status, channel, timing_option, start_time," +
      "       start_date, frequency, daily_interval," +
      "       day_mon, day_tue, day_wed," +
      "       day_thu, day_fri, day_sat, day_sun, monthly_option," +
      "       finishes, daily_option," +
      "       schedule, reschedule, unschedule)" +
      "      VALUES (get_uuid(), '0', '0', 'Y'," +
      "              NOW(), '100', NOW(), '100', '1005800000'," +
      "              '100', 'Y', ? ," +
      "              'SCH', 'Process Scheduler', 'S', NOW()," +
      "              NOW(), '4', 7," +
      "              'N', 'N', 'N'," +
      "              'N', 'N', 'N', 'N', 'S'," +
      "              'N', 'N'," +
      "              'N', 'N', 'N')";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, obContext);

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
