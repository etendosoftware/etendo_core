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
class DuplicatedClientWarehouseValueData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String client;
  public String searchkey;
  public String warehouse;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("client"))
      return client;
    else if (fieldName.equalsIgnoreCase("searchkey"))
      return searchkey;
    else if (fieldName.equalsIgnoreCase("warehouse"))
      return warehouse;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static DuplicatedClientWarehouseValueData[] duplicatedClientWarehouseValue(ConnectionProvider connectionProvider)    throws ServletException {
    return duplicatedClientWarehouseValue(connectionProvider, 0, 0);
  }

  public static DuplicatedClientWarehouseValueData[] duplicatedClientWarehouseValue(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT c.name AS client," +
      "               w.value AS searchkey," +
      "               w.name AS warehouse" +
      "        FROM m_warehouse w" +
      "        JOIN ad_client c ON w.ad_client_id = c.ad_client_id" +
      "        WHERE EXISTS (SELECT 1" +
      "                      FROM m_warehouse w2" +
      "                      WHERE w2.m_warehouse_id <> w.m_warehouse_id" +
      "                        AND w2.ad_client_id = w.ad_client_id" +
      "                        AND w2.value = w.value)" +
      "        ORDER BY c.name," +
      "                 w.value," +
      "                 w.name";

    ResultSet result;
    Vector<DuplicatedClientWarehouseValueData> vector = new Vector<DuplicatedClientWarehouseValueData>(0);
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
        DuplicatedClientWarehouseValueData objectDuplicatedClientWarehouseValueData = new DuplicatedClientWarehouseValueData();
        objectDuplicatedClientWarehouseValueData.client = UtilSql.getValue(result, "client");
        objectDuplicatedClientWarehouseValueData.searchkey = UtilSql.getValue(result, "searchkey");
        objectDuplicatedClientWarehouseValueData.warehouse = UtilSql.getValue(result, "warehouse");
        objectDuplicatedClientWarehouseValueData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectDuplicatedClientWarehouseValueData);
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
    DuplicatedClientWarehouseValueData objectDuplicatedClientWarehouseValueData[] = new DuplicatedClientWarehouseValueData[vector.size()];
    vector.copyInto(objectDuplicatedClientWarehouseValueData);
    return(objectDuplicatedClientWarehouseValueData);
  }

  public static boolean existsDuplicatedClientWarehouseValue(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT count(*) AS EXISTING" +
      "        FROM m_warehouse" +
      "        GROUP BY ad_client_id, value" +
      "        HAVING count(*)>1";

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
