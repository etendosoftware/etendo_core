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
class DeleteInventoryLinesData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String dummy;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("dummy"))
      return dummy;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static DeleteInventoryLinesData[] select(ConnectionProvider connectionProvider)    throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static DeleteInventoryLinesData[] select(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT 1 as DUMMY FROM DUAL";

    ResultSet result;
    Vector<DeleteInventoryLinesData> vector = new Vector<DeleteInventoryLinesData>(0);
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
        DeleteInventoryLinesData objectDeleteInventoryLinesData = new DeleteInventoryLinesData();
        objectDeleteInventoryLinesData.dummy = UtilSql.getValue(result, "dummy");
        objectDeleteInventoryLinesData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectDeleteInventoryLinesData);
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
    DeleteInventoryLinesData objectDeleteInventoryLinesData[] = new DeleteInventoryLinesData[vector.size()];
    vector.copyInto(objectDeleteInventoryLinesData);
    return(objectDeleteInventoryLinesData);
  }

  public static int deleteInventoryLines(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      DELETE FROM m_storage_detail" +
      "        WHERE m_storage_detail_id IN" +
      "        	(SELECT m_storage_detail_id" +
      "        	FROM M_STORAGE_DETAIL MS" +
      "	        WHERE m_product_id IN (SELECT p.m_product_id" +
      "	                      FROM m_product p JOIN m_attributeset aset ON p.m_attributeset_id = aset.m_attributeset_id" +
      "	                      WHERE coalesce(p.attrsetvaluetype, '-') <> 'F'" +
      "	                      AND aset.isoneattrsetvalrequired = 'Y')" +
      "	        AND COALESCE(m_attributesetinstance_id, '0') = '0'" +
      "	        AND QTYONHAND = 0 " +
      "	        AND PREQTYONHAND = 0 " +
      "	        AND (QTYORDERONHAND=0 OR QTYORDERONHAND IS NULL)" +
      "	        AND (PREQTYORDERONHAND=0 OR PREQTYORDERONHAND IS NULL)" +
      "	        AND NOT EXISTS (SELECT 1 FROM m_stock_aux SA WHERE SA.M_STORAGE_DETAIL_ID = MS.M_STORAGE_DETAIL_ID)" +
      "	        AND NOT EXISTS (SELECT 1 FROM m_stock_proposed SA WHERE SA.M_STORAGE_DETAIL_ID = MS.M_STORAGE_DETAIL_ID))";

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

  public static int deleteStockAuxLines(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      DELETE FROM m_stock_aux" +
      "        WHERE m_storage_detail_id IN" +
      "        	(SELECT m_storage_detail_id" +
      "        	FROM M_STORAGE_DETAIL MS" +
      "	        WHERE m_product_id IN (SELECT p.m_product_id" +
      "	                      FROM m_product p JOIN m_attributeset aset ON p.m_attributeset_id = aset.m_attributeset_id" +
      "	                      WHERE coalesce(p.attrsetvaluetype, '-') <> 'F'" +
      "	                      AND aset.isoneattrsetvalrequired = 'Y')" +
      "	        AND COALESCE(m_attributesetinstance_id, '0') = '0'" +
      "	        AND QTYONHAND = 0 " +
      "	        AND PREQTYONHAND = 0 " +
      "	        AND (QTYORDERONHAND=0 OR QTYORDERONHAND IS NULL)" +
      "	        AND (PREQTYORDERONHAND=0 OR PREQTYORDERONHAND IS NULL))" +
      "	        AND COALESCE(quantity, 0) = 0" +
      "			AND COALESCE(qtyorder, 0) = 0";

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
