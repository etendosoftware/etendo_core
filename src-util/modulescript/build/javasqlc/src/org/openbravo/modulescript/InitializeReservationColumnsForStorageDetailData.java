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
class InitializeReservationColumnsForStorageDetailData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String mProductId;
  public String mLocatorId;
  public String mAttributesetinstanceId;
  public String cUomId;
  public String reservedqty;
  public String allocatedqty;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("m_product_id") || fieldName.equals("mProductId"))
      return mProductId;
    else if (fieldName.equalsIgnoreCase("m_locator_id") || fieldName.equals("mLocatorId"))
      return mLocatorId;
    else if (fieldName.equalsIgnoreCase("m_attributesetinstance_id") || fieldName.equals("mAttributesetinstanceId"))
      return mAttributesetinstanceId;
    else if (fieldName.equalsIgnoreCase("c_uom_id") || fieldName.equals("cUomId"))
      return cUomId;
    else if (fieldName.equalsIgnoreCase("reservedqty"))
      return reservedqty;
    else if (fieldName.equalsIgnoreCase("allocatedqty"))
      return allocatedqty;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static InitializeReservationColumnsForStorageDetailData[] selectReservationAmounts(ConnectionProvider connectionProvider)    throws ServletException {
    return selectReservationAmounts(connectionProvider, 0, 0);
  }

  public static InitializeReservationColumnsForStorageDetailData[] selectReservationAmounts(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        select r.m_product_id, rs.m_locator_id, rs.m_attributesetinstance_id, r.c_uom_id, SUM(rs.quantity - rs.releasedqty) as reservedqty, " +
      "        SUM(CASE WHEN rs.isallocated = 'Y' THEN (rs.quantity - rs.releasedqty) ELSE 0 END) as allocatedqty" +
      "        from m_reservation r, m_reservation_stock rs" +
      "        where r.m_reservation_id = rs.m_reservation_id" +
      "        group by r.m_product_id, rs.m_locator_id, rs.m_attributesetinstance_id, r.c_uom_id" +
      "        having SUM(rs.quantity - rs.releasedqty) <> 0";

    ResultSet result;
    Vector<InitializeReservationColumnsForStorageDetailData> vector = new Vector<InitializeReservationColumnsForStorageDetailData>(0);
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
        InitializeReservationColumnsForStorageDetailData objectInitializeReservationColumnsForStorageDetailData = new InitializeReservationColumnsForStorageDetailData();
        objectInitializeReservationColumnsForStorageDetailData.mProductId = UtilSql.getValue(result, "m_product_id");
        objectInitializeReservationColumnsForStorageDetailData.mLocatorId = UtilSql.getValue(result, "m_locator_id");
        objectInitializeReservationColumnsForStorageDetailData.mAttributesetinstanceId = UtilSql.getValue(result, "m_attributesetinstance_id");
        objectInitializeReservationColumnsForStorageDetailData.cUomId = UtilSql.getValue(result, "c_uom_id");
        objectInitializeReservationColumnsForStorageDetailData.reservedqty = UtilSql.getValue(result, "reservedqty");
        objectInitializeReservationColumnsForStorageDetailData.allocatedqty = UtilSql.getValue(result, "allocatedqty");
        objectInitializeReservationColumnsForStorageDetailData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectInitializeReservationColumnsForStorageDetailData);
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
    InitializeReservationColumnsForStorageDetailData objectInitializeReservationColumnsForStorageDetailData[] = new InitializeReservationColumnsForStorageDetailData[vector.size()];
    vector.copyInto(objectInitializeReservationColumnsForStorageDetailData);
    return(objectInitializeReservationColumnsForStorageDetailData);
  }

  public static int updateStorageDetail(ConnectionProvider connectionProvider, String reservedqty, String allocatedqty, String mAttributesetinstanceId, String mLocatorId, String mProductId, String cUomId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        update M_Storage_Detail set reservedqty = to_number(?), allocatedqty = to_number(?)" +
      "        where m_attributesetinstance_id = ?" +
      "        and m_locator_id = ?" +
      "        and m_product_id = ?" +
      "        and c_uom_id = ?" +
      "        and m_product_uom_id is null";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, reservedqty);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, allocatedqty);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, mAttributesetinstanceId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, mLocatorId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, mProductId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, cUomId);

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

  public static int createPreference(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "           INSERT INTO ad_preference (" +
      "           ad_preference_id, ad_client_id, ad_org_id, isactive," +
      "           createdby, created, updatedby, updated,attribute" +
      "           ) VALUES (" +
      "           get_uuid(), '0', '0', 'Y', '0', NOW(), '0', NOW(),'InitializeReservationColumnsForStorageDetail')";

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

  public static boolean isMigrated(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT count(*) as exist" +
      "        FROM DUAL" +
      "        WHERE EXISTS (SELECT 1 FROM ad_preference" +
      "                      WHERE attribute = 'InitializeReservationColumnsForStorageDetail')";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if(result.next()) {
        boolReturn = !UtilSql.getValue(result, "exist").equals("0");
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
