//Sqlc generated V1.O00-1
package org.openbravo.buildvalidation;

import java.sql.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import jakarta.servlet.ServletException;

import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.data.UtilSql;
import java.util.*;

@SuppressWarnings("serial")
class PreferenceConflictData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String attribute;
  public String count;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("attribute"))
      return attribute;
    else if (fieldName.equalsIgnoreCase("count"))
      return count;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static PreferenceConflictData[] differentOrgDifferentValues(ConnectionProvider connectionProvider)    throws ServletException {
    return differentOrgDifferentValues(connectionProvider, 0, 0);
  }

  public static PreferenceConflictData[] differentOrgDifferentValues(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "          select distinct attribute, '' as count" +
      "            from ad_preference p " +
      "            where exists (" +
      "                SELECT 1" +
      "                  FROM AD_Preference " +
      "                   WHERE IsActive='Y' " +
      "                   and p.attribute = attribute" +
      "                  group by ad_client_id, AD_Window_ID, aD_user_id" +
      "                  having count(*)>1)" +
      "            and exists (select 1" +
      "                   from ad_preference p1" +
      "                  where p1.isactive='Y'" +
      "                    and p1.ad_org_id != p.ad_org_id" +
      "                    and p1.attribute = p.attribute" +
      "                    and to_char(p1.value) != to_char(p.value)" +
      "                    and coalesce(ad_window_id,'.') = coalesce(p.ad_window_id,'.')" +
      "                    and coalesce(ad_user_id, '.') = coalesce(p.ad_user_id, '.')" +
      "                    and coalesce(ad_client_id, '.') = coalesce(p.ad_client_id, '.'))";

    ResultSet result;
    Vector<PreferenceConflictData> vector = new Vector<PreferenceConflictData>(0);
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
        PreferenceConflictData objectPreferenceConflictData = new PreferenceConflictData();
        objectPreferenceConflictData.attribute = UtilSql.getValue(result, "attribute");
        objectPreferenceConflictData.count = UtilSql.getValue(result, "count");
        objectPreferenceConflictData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectPreferenceConflictData);
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
    PreferenceConflictData objectPreferenceConflictData[] = new PreferenceConflictData[vector.size()];
    vector.copyInto(objectPreferenceConflictData);
    return(objectPreferenceConflictData);
  }

  public static boolean alreadymp16(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "         select count(*) as count " +
      "           from user_tab_columns " +
      "          where table_name ='AD_PREFERENCE' " +
      "            and column_name = 'AD_MODULE_ID'";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if(result.next()) {
        boolReturn = !UtilSql.getValue(result, "count").equals("0");
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
