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
class UpdateQtyDeliveredData implements FieldProvider {
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

  public static UpdateQtyDeliveredData[] select(ConnectionProvider connectionProvider)    throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static UpdateQtyDeliveredData[] select(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        select 1 as dummy from dual";

    ResultSet result;
    Vector<UpdateQtyDeliveredData> vector = new Vector<UpdateQtyDeliveredData>(0);
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
        UpdateQtyDeliveredData objectUpdateQtyDeliveredData = new UpdateQtyDeliveredData();
        objectUpdateQtyDeliveredData.dummy = UtilSql.getValue(result, "dummy");
        objectUpdateQtyDeliveredData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUpdateQtyDeliveredData);
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
    UpdateQtyDeliveredData objectUpdateQtyDeliveredData[] = new UpdateQtyDeliveredData[vector.size()];
    vector.copyInto(objectUpdateQtyDeliveredData);
    return(objectUpdateQtyDeliveredData);
  }

  public static int updateQtyDelivered(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE c_orderline ol" +
      "        SET qtydelivered = COALESCE((SELECT SUM(COALESCE(iol.movementqty, 0))" +
      "                                     FROM m_inoutline iol" +
      "                                     JOIN m_inout io ON iol.m_inout_id = io.m_inout_id AND io.processed = 'Y'" +
      "                                     WHERE iol.c_orderline_id = ol.c_orderline_id" +
      "                                     ), 0)" +
      "        WHERE EXISTS (SELECT 1" +
      "                      FROM c_order o" +
      "                      JOIN c_doctype dt ON o.c_doctypetarget_id = dt.c_doctype_id" +
      "                      WHERE o.c_order_id = ol.c_order_id" +
      "                      AND dt.isreturn = 'Y'" +
      "                      AND o.issotrx = 'N'" +
      "                      AND o.processed = 'Y'" +
      "                     )" +
      "        AND ol.qtydelivered = 0";

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

  public static int updateIsDelivered(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE C_Order o" +
      "        SET IsDelivered = 'Y'" +
      "        WHERE o.IsDelivered = 'N'" +
      "        AND o.processed = 'Y'" +
      "        AND (o.issotrx = 'Y' OR (o.issotrx = 'N' AND (SELECT dt.isreturn FROM c_doctype dt WHERE o.c_doctypetarget_id = dt.c_doctype_id) = 'Y'))" +
      "        AND NOT EXISTS (SELECT 1" +
      "                        FROM c_orderline ol" +
      "                        WHERE o.c_order_id = ol.c_order_id" +
      "                        AND ol.c_order_discount_id IS NULL" +
      "                        AND ol.QTYORDERED <> ol.QTYDELIVERED" +
      "                       )";

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
