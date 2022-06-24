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
class GrantAccessToProcessDefinitionData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String clientId;
  public String clientName;
  public String roleId;
  public String roleName;
  public String processName;
  public String moduleName;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("client_id") || fieldName.equals("clientId"))
      return clientId;
    else if (fieldName.equalsIgnoreCase("client_name") || fieldName.equals("clientName"))
      return clientName;
    else if (fieldName.equalsIgnoreCase("role_id") || fieldName.equals("roleId"))
      return roleId;
    else if (fieldName.equalsIgnoreCase("role_name") || fieldName.equals("roleName"))
      return roleName;
    else if (fieldName.equalsIgnoreCase("process_name") || fieldName.equals("processName"))
      return processName;
    else if (fieldName.equalsIgnoreCase("module_name") || fieldName.equals("moduleName"))
      return moduleName;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static GrantAccessToProcessDefinitionData[] getRolesToBeUpdated(ConnectionProvider connectionProvider, String tableId, String oldId)    throws ServletException {
    return getRolesToBeUpdated(connectionProvider, tableId, oldId, 0, 0);
  }

  public static GrantAccessToProcessDefinitionData[] getRolesToBeUpdated(ConnectionProvider connectionProvider, String tableId, String oldId, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT distinct ad_client.ad_client_id as client_id, ad_client.name as client_name, " +
      "          ad_role.ad_role_id as role_id, ad_role.name as role_name," +
      "          ad_process.name AS process_name, ad_module.name as module_name" +
      "          FROM ad_ref_data_loaded " +
      "          JOIN ad_client ON (ad_ref_data_loaded.ad_client_id = ad_client.ad_client_id)" +
      "          JOIN ad_process_access ON (ad_ref_data_loaded.specific_id = ad_process_access.ad_process_access_id)" +
      "          JOIN ad_process ON (ad_process.ad_process_id = ad_process_access.ad_process_id)" +
      "          JOIN ad_module ON (ad_module.ad_module_id = ad_ref_data_loaded.ad_module_id)" +
      "          JOIN ad_role ON (ad_role.ad_role_id = ad_process_access.ad_role_id)" +
      "          WHERE ad_ref_data_loaded.ad_table_id = ?" +
      "          AND ad_process_access.ad_process_id = ?" +
      "          ORDER BY ad_role.name, ad_process.name, ad_client.name";

    ResultSet result;
    Vector<GrantAccessToProcessDefinitionData> vector = new Vector<GrantAccessToProcessDefinitionData>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, tableId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, oldId);

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
        GrantAccessToProcessDefinitionData objectGrantAccessToProcessDefinitionData = new GrantAccessToProcessDefinitionData();
        objectGrantAccessToProcessDefinitionData.clientId = UtilSql.getValue(result, "client_id");
        objectGrantAccessToProcessDefinitionData.clientName = UtilSql.getValue(result, "client_name");
        objectGrantAccessToProcessDefinitionData.roleId = UtilSql.getValue(result, "role_id");
        objectGrantAccessToProcessDefinitionData.roleName = UtilSql.getValue(result, "role_name");
        objectGrantAccessToProcessDefinitionData.processName = UtilSql.getValue(result, "process_name");
        objectGrantAccessToProcessDefinitionData.moduleName = UtilSql.getValue(result, "module_name");
        objectGrantAccessToProcessDefinitionData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectGrantAccessToProcessDefinitionData);
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
    GrantAccessToProcessDefinitionData objectGrantAccessToProcessDefinitionData[] = new GrantAccessToProcessDefinitionData[vector.size()];
    vector.copyInto(objectGrantAccessToProcessDefinitionData);
    return(objectGrantAccessToProcessDefinitionData);
  }

  public static int grantAccess(ConnectionProvider connectionProvider, String newId, String oldId, String tableId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      INSERT INTO obuiapp_process_access(" +
      "            obuiapp_process_access_id, obuiapp_process_id, ad_role_id, ad_client_id, " +
      "            ad_org_id, isactive, created, createdby, updated, updatedby, " +
      "            isreadwrite, inherited_from)" +
      "         (SELECT" +
      "            get_uuid(), ?, ad_role_id, ad_client_id," +
      "            ad_org_id, isactive, now(), '0', now(), '0'," +
      "            isreadwrite, inherited_from" +
      "          FROM" +
      "            ad_process_access" +
      "          WHERE" +
      "            ad_process_id = ?" +
      "          AND" +
      "            NOT EXISTS (SELECT 1 " +
      "                        FROM obuiapp_process_access " +
      "                        WHERE obuiapp_process_access.ad_role_id = ad_process_access.ad_role_id" +
      "                        AND obuiapp_process_id = ?)" +
      "          AND" +
      "            NOT EXISTS (SELECT 1 " +
      "                        FROM ad_ref_data_loaded " +
      "                        WHERE ad_table_id = ? " +
      "                        AND ad_ref_data_loaded.ad_client_id = ad_process_access.ad_client_id" +
      "                        AND specific_id = ad_process_access_id))";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, newId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, oldId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, newId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, tableId);

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

  public static int createAlertRule(ConnectionProvider connectionProvider, String clientId, String name)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      INSERT INTO ad_alertrule(" +
      "            ad_alertrule_id, ad_client_id, ad_org_id, isactive, " +
      "            created, createdby, updated, updatedby, " +
      "            name, ad_tab_id, " +
      "            filterclause, sql, type)" +
      "         VALUES (" +
      "            get_uuid(), ?, '0', 'Y'," +
      "            now(), '0', now(), '0', " +
      "            ?, '119'," +
      "            '', '', 'E')";

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

  public static String getAlertRule(ConnectionProvider connectionProvider, String clientId, String name)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT ad_alertrule_id " +
      "            FROM ad_alertrule" +
      "            WHERE ad_client_id = ?" +
      "              AND name = ?";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, name);

      result = st.executeQuery();
      if(result.next()) {
        strReturn = UtilSql.getValue(result, "ad_alertrule_id");
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

  public static String getAdminRole(ConnectionProvider connectionProvider, String clientId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT ad_role_id" +
      "            FROM ad_role" +
      "            WHERE ad_client_id = ?" +
      "              AND is_client_admin = 'Y'";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientId);

      result = st.executeQuery();
      if(result.next()) {
        strReturn = UtilSql.getValue(result, "ad_role_id");
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

  public static int createAlert(ConnectionProvider connectionProvider, String clientId, String description, String alertRuleId, String roleId, String recordId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      INSERT INTO ad_alert(" +
      "            ad_alert_id, ad_client_id, ad_org_id, isactive, " +
      "            created, createdby, updated, updatedby, " +
      "            description, ad_alertrule_id, ad_role_id," +
      "            ad_user_id, isfixed, m_warehouse_id, note," +
      "            record_id, referencekey_id, status)" +
      "         VALUES (" +
      "            get_uuid(), ?, '0', 'Y'," +
      "            now(), '0', now(), '0', " +
      "            ?, ?, ?," +
      "            null, 'N', null, ''," +
      "            ?, ?, 'NEW')";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, description);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, alertRuleId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, roleId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, recordId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, roleId);

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

  public static boolean existRecipient(ConnectionProvider connectionProvider, String clientId, String alertRuleId, String roleId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "       SELECT COUNT(*) AS EXISTING" +
      "       FROM ad_alertrecipient" +
      "       WHERE ad_client_id = ?" +
      "       AND ad_alertrule_id = ?" +
      "       AND ad_role_id = ?";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, alertRuleId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, roleId);

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

  public static int createRecipients(ConnectionProvider connectionProvider, String clientId, String alertRuleId, String roleId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      INSERT INTO ad_alertrecipient(" +
      "            ad_alertrecipient_id, ad_user_id, ad_client_id, ad_org_id, isactive, " +
      "            created, createdby, updated, updatedby, " +
      "            ad_alertrule_id, ad_role_id, inherited_from)" +
      "         VALUES (" +
      "            get_uuid(), null, ?, '0', 'Y'," +
      "            now(), '0', now(), '0', " +
      "            ?, ?, null)";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, alertRuleId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, roleId);

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
