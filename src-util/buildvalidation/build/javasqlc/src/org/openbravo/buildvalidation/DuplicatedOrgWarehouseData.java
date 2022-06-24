//Sqlc generated V1.O00-1
package org.openbravo.buildvalidation;

import java.sql.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;

import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.data.UtilSql;
import java.util.*;

class DuplicatedOrgWarehouseData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String client;
  public String organization;
  public String warehouse;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("client"))
      return client;
    else if (fieldName.equalsIgnoreCase("organization"))
      return organization;
    else if (fieldName.equalsIgnoreCase("warehouse"))
      return warehouse;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static DuplicatedOrgWarehouseData[] DuplicatedOrganizationWarehouse(ConnectionProvider connectionProvider)    throws ServletException {
    return DuplicatedOrganizationWarehouse(connectionProvider, 0, 0);
  }

  public static DuplicatedOrgWarehouseData[] DuplicatedOrganizationWarehouse(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT c.name as client, o.name as organization, w.name as warehouse" +
      "        FROM ad_org_warehouse ow" +
      "            left join ad_org o on ow.ad_org_id=o.ad_org_id" +
      "            left join m_warehouse w on ow.m_warehouse_id = w.m_warehouse_id" +
      "            left join ad_client c on ow.ad_client_id = c.ad_client_id" +
      "        GROUP BY ow.m_warehouse_id, ow.ad_org_id, c.name, o.name, w.name" +
      "        HAVING count(*)>1";

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
        DuplicatedOrgWarehouseData objectDuplicatedOrgWarehouseData = new DuplicatedOrgWarehouseData();
        objectDuplicatedOrgWarehouseData.client = UtilSql.getValue(result, "client");
        objectDuplicatedOrgWarehouseData.organization = UtilSql.getValue(result, "organization");
        objectDuplicatedOrgWarehouseData.warehouse = UtilSql.getValue(result, "warehouse");
        objectDuplicatedOrgWarehouseData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectDuplicatedOrgWarehouseData);
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
    DuplicatedOrgWarehouseData objectDuplicatedOrgWarehouseData[] = new DuplicatedOrgWarehouseData[vector.size()];
    vector.copyInto(objectDuplicatedOrgWarehouseData);
    return(objectDuplicatedOrgWarehouseData);
  }

  public static boolean existsDuplicatedOrgWarehouse(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT count(*) AS EXISTING" +
      "        FROM ad_org_warehouse" +
      "        GROUP BY ad_org_id, m_warehouse_id" +
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

/**
Check if the AD_Org_Warehouse table exist
 */
  public static boolean existOrgWarehouseTable(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "       SELECT count(*) AS EXISTING" +
      "       FROM ad_table" +
      "       WHERE ad_table_id = '26673F55911848E894D837F57207A92B'";

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
}
