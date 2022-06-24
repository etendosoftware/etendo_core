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
class AdminFlagOnCreateDefaultsData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String admin;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("admin"))
      return admin;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static AdminFlagOnCreateDefaultsData[] select(ConnectionProvider connectionProvider)    throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static AdminFlagOnCreateDefaultsData[] select(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT 1 AS admin FROM dual";

    ResultSet result;
    Vector<AdminFlagOnCreateDefaultsData> vector = new Vector<AdminFlagOnCreateDefaultsData>(0);
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
        AdminFlagOnCreateDefaultsData objectAdminFlagOnCreateDefaultsData = new AdminFlagOnCreateDefaultsData();
        objectAdminFlagOnCreateDefaultsData.admin = UtilSql.getValue(result, "admin");
        objectAdminFlagOnCreateDefaultsData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectAdminFlagOnCreateDefaultsData);
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
    AdminFlagOnCreateDefaultsData objectAdminFlagOnCreateDefaultsData[] = new AdminFlagOnCreateDefaultsData[vector.size()];
    vector.copyInto(objectAdminFlagOnCreateDefaultsData);
    return(objectAdminFlagOnCreateDefaultsData);
  }

  public static int updateClientAdmin(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE ad_role" +
      "        SET is_client_admin = (" +
      "              SELECT CASE WHEN EXISTS (SELECT 1" +
      "                                       FROM ad_form_access, ad_role_orgaccess" +
      "                                       WHERE ad_form_access.ad_role_id = ad_role_orgaccess.ad_role_id" +
      "                                         AND ad_role.ad_role_id = ad_form_access.ad_role_id" +
      "                                         AND ad_form_id = 'DE2329ABCAA84D5F99B59043CFFFE454'" +
      "                                         AND isreadwrite = 'Y'" +
      "                                         AND ad_role_orgaccess.ad_org_id = '0') THEN 'Y'" +
      "                     ELSE 'N'" +
      "                     END as admin" +
      "              FROM dual)" +
      "        WHERE is_client_admin IS NULL";

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

  public static int updateOrgAdmin(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE ad_role_orgaccess" +
      "        SET is_org_admin = (" +
      "              SELECT CASE WHEN EXISTS (SELECT 1" +
      "                                       FROM ad_form_access" +
      "                                       WHERE ad_role_orgaccess.ad_role_id = ad_form_access.ad_role_id" +
      "                                         AND ad_form_id = 'DE2329ABCAA84D5F99B59043CFFFE454'" +
      "                                         AND isreadwrite = 'Y'" +
      "                                         AND NOT EXISTS (SELECT 1 FROM ad_role_orgaccess ro" +
      "                                                         WHERE ro.ad_role_id = ad_form_access.ad_role_id" +
      "                                                           AND ro.ad_org_id = '0')) THEN 'Y'" +
      "                     ELSE 'N'" +
      "                     END as admin" +
      "              FROM dual)" +
      "        WHERE is_org_admin IS NULL";

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

  public static int updateRoleAdmin(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE ad_user_roles" +
      "        SET is_role_admin = (" +
      "              SELECT CASE WHEN EXISTS (SELECT 1" +
      "                                       FROM ad_window_access, ad_user_roles ur" +
      "                                       WHERE ad_window_id = '111'" +
      "                                         AND isreadwrite = 'Y'" +
      "                                         AND ur.ad_role_id = ad_window_access.ad_role_id" +
      "                                         AND ur.ad_user_id = ad_user_roles.ad_user_id) THEN 'Y'" +
      "                          ELSE 'N' END as admin" +
      "              FROM dual)" +
      "        WHERE is_role_admin IS NULL";

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
