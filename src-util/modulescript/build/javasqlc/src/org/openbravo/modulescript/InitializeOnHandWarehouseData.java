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
class InitializeOnHandWarehouseData implements FieldProvider {
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

  public static InitializeOnHandWarehouseData[] select(Connection conn, ConnectionProvider connectionProvider)    throws ServletException {
    return select(conn, connectionProvider, 0, 0);
  }

  public static InitializeOnHandWarehouseData[] select(Connection conn, ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        select '' as name" +
      "        from dual";

    ResultSet result;
    Vector<InitializeOnHandWarehouseData> vector = new Vector<InitializeOnHandWarehouseData>(0);
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(conn, strSql);

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
        InitializeOnHandWarehouseData objectInitializeOnHandWarehouseData = new InitializeOnHandWarehouseData();
        objectInitializeOnHandWarehouseData.name = UtilSql.getValue(result, "name");
        objectInitializeOnHandWarehouseData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectInitializeOnHandWarehouseData);
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
        connectionProvider.releaseTransactionalPreparedStatement(st);
      } catch(Exception e){
        log4j.error("Error during release*Statement of query: " + strSql, e);
      }
    }
    InitializeOnHandWarehouseData objectInitializeOnHandWarehouseData[] = new InitializeOnHandWarehouseData[vector.size()];
    vector.copyInto(objectInitializeOnHandWarehouseData);
    return(objectInitializeOnHandWarehouseData);
  }

  public static boolean hasOnHandWarehouse(Connection conn, ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        select count(*) as name from dual" +
      "        where exists (select 1 from ad_org_warehouse)";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(conn, strSql);

      result = st.executeQuery();
      if(result.next()) {
        boolReturn = !UtilSql.getValue(result, "name").equals("0");
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
        connectionProvider.releaseTransactionalPreparedStatement(st);
      } catch(Exception e){
        log4j.error("Error during release*Statement of query: " + strSql, e);
      }
    }
    return(boolReturn);
  }

  public static int initializeOnHandWarehouse(Connection conn, ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        INSERT INTO ad_org_warehouse (" +
      "          ad_org_warehouse_id, ad_client_id, ad_org_id, isactive," +
      "          created, createdby, updated, updatedby," +
      "          m_warehouse_id, priority" +
      "        ) " +
      "          SELECT get_uuid(), wh.ad_client_id, o.ad_org_id, 'Y'," +
      "                now(), '0', now(), '0'," +
      "                wh.m_warehouse_id, 10" +
      "          FROM m_warehouse wh" +
      "              JOIN ad_org o ON ad_org_isinnaturaltree(wh.ad_org_id, o.ad_org_id, wh.ad_client_id) = 'Y'" +
      "              JOIN aD_orgtype ot ON o.ad_orgtype_id = ot.ad_orgtype_id" +
      "          WHERE ot.istransactionsallowed = 'Y'";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(conn, strSql);

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
}
