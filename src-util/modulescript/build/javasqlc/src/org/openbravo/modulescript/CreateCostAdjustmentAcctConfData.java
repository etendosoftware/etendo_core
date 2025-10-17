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
class CreateCostAdjustmentAcctConfData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String cAcctschemaId;
  public String adClientId;
  public String adOrgId;
  public String adTableId;
  public String name;
  public String cPeriodId;
  public String value;
  public String status;
  public String isdefaultacct;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("c_acctschema_id") || fieldName.equals("cAcctschemaId"))
      return cAcctschemaId;
    else if (fieldName.equalsIgnoreCase("ad_client_id") || fieldName.equals("adClientId"))
      return adClientId;
    else if (fieldName.equalsIgnoreCase("ad_org_id") || fieldName.equals("adOrgId"))
      return adOrgId;
    else if (fieldName.equalsIgnoreCase("ad_table_id") || fieldName.equals("adTableId"))
      return adTableId;
    else if (fieldName.equalsIgnoreCase("name"))
      return name;
    else if (fieldName.equalsIgnoreCase("c_period_id") || fieldName.equals("cPeriodId"))
      return cPeriodId;
    else if (fieldName.equalsIgnoreCase("value"))
      return value;
    else if (fieldName.equalsIgnoreCase("status"))
      return status;
    else if (fieldName.equalsIgnoreCase("isdefaultacct"))
      return isdefaultacct;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static CreateCostAdjustmentAcctConfData[] selectAcctSchema(ConnectionProvider connectionProvider)    throws ServletException {
    return selectAcctSchema(connectionProvider, 0, 0);
  }

  public static CreateCostAdjustmentAcctConfData[] selectAcctSchema(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "    SELECT C_AcctSchema_ID, ad_client_id, '' as ad_org_id, '' as ad_table_id, '' as name, '' as c_period_id, '' as value, '' as status," +
      "    '' as isdefaultacct" +
      "    FROM C_AcctSchema";

    ResultSet result;
    Vector<CreateCostAdjustmentAcctConfData> vector = new Vector<CreateCostAdjustmentAcctConfData>(0);
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
        CreateCostAdjustmentAcctConfData objectCreateCostAdjustmentAcctConfData = new CreateCostAdjustmentAcctConfData();
        objectCreateCostAdjustmentAcctConfData.cAcctschemaId = UtilSql.getValue(result, "c_acctschema_id");
        objectCreateCostAdjustmentAcctConfData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectCreateCostAdjustmentAcctConfData.adOrgId = UtilSql.getValue(result, "ad_org_id");
        objectCreateCostAdjustmentAcctConfData.adTableId = UtilSql.getValue(result, "ad_table_id");
        objectCreateCostAdjustmentAcctConfData.name = UtilSql.getValue(result, "name");
        objectCreateCostAdjustmentAcctConfData.cPeriodId = UtilSql.getValue(result, "c_period_id");
        objectCreateCostAdjustmentAcctConfData.value = UtilSql.getValue(result, "value");
        objectCreateCostAdjustmentAcctConfData.status = UtilSql.getValue(result, "status");
        objectCreateCostAdjustmentAcctConfData.isdefaultacct = UtilSql.getValue(result, "isdefaultacct");
        objectCreateCostAdjustmentAcctConfData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectCreateCostAdjustmentAcctConfData);
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
    CreateCostAdjustmentAcctConfData objectCreateCostAdjustmentAcctConfData[] = new CreateCostAdjustmentAcctConfData[vector.size()];
    vector.copyInto(objectCreateCostAdjustmentAcctConfData);
    return(objectCreateCostAdjustmentAcctConfData);
  }

  public static boolean selectTables(ConnectionProvider connectionProvider, String acctSchemaId, String tableId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        select count(*) as name" +
      "        from c_acctschema_table where c_acctschema_id = ?" +
      "        and ad_table_id = ?";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, acctSchemaId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, tableId);

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
        connectionProvider.releasePreparedStatement(st);
      } catch(Exception e){
        log4j.error("Error during release*Statement of query: " + strSql, e);
      }
    }
    return(boolReturn);
  }

  public static int insertAcctSchemaTable(Connection conn, ConnectionProvider connectionProvider, String acctSchemaId, String tableId, String clientId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      INSERT INTO c_acctschema_table(" +
      "            c_acctschema_table_id, c_acctschema_id, ad_table_id, ad_client_id," +
      "            ad_org_id, isactive, created, createdby, updated, updatedby," +
      "            ad_createfact_template_id, acctdescription)" +
      "    VALUES (get_uuid(), ?, ?, ?," +
      "            '0', 'Y', now(), '100', now(), '100'," +
      "            null, null)";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(conn, strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, acctSchemaId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, tableId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientId);

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
