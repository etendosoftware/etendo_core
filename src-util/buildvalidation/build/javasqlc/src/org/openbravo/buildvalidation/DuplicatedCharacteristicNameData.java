//Sqlc generated V1.O00-1
package org.openbravo.buildvalidation;

import java.sql.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.servlet.ServletException;

import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.data.UtilSql;
import java.util.*;

@SuppressWarnings("serial")
class DuplicatedCharacteristicNameData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String mCharacteristicId;
  public String characteristic;
  public String value;
  public String count;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("m_characteristic_id") || fieldName.equals("mCharacteristicId"))
      return mCharacteristicId;
    else if (fieldName.equalsIgnoreCase("characteristic"))
      return characteristic;
    else if (fieldName.equalsIgnoreCase("value"))
      return value;
    else if (fieldName.equalsIgnoreCase("count"))
      return count;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static DuplicatedCharacteristicNameData[] DuplicatedCharacteristicNameData(ConnectionProvider connectionProvider)    throws ServletException {
    return DuplicatedCharacteristicNameData(connectionProvider, 0, 0);
  }

  public static DuplicatedCharacteristicNameData[] DuplicatedCharacteristicNameData(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT ch.m_characteristic_id, ch.name as characteristic, chv.name as value, count(*)" +
      "        FROM m_ch_value chv" +
      "            join m_characteristic ch on (ch.m_characteristic_id = chv.m_characteristic_id)" +
      "        GROUP BY ch.m_characteristic_id, ch.name, chv.name" +
      "        HAVING count(*) > 1        ";

    ResultSet result;
    Vector<DuplicatedCharacteristicNameData> vector = new Vector<DuplicatedCharacteristicNameData>(0);
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
        DuplicatedCharacteristicNameData objectDuplicatedCharacteristicNameData = new DuplicatedCharacteristicNameData();
        objectDuplicatedCharacteristicNameData.mCharacteristicId = UtilSql.getValue(result, "m_characteristic_id");
        objectDuplicatedCharacteristicNameData.characteristic = UtilSql.getValue(result, "characteristic");
        objectDuplicatedCharacteristicNameData.value = UtilSql.getValue(result, "value");
        objectDuplicatedCharacteristicNameData.count = UtilSql.getValue(result, "count");
        objectDuplicatedCharacteristicNameData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectDuplicatedCharacteristicNameData);
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
    DuplicatedCharacteristicNameData objectDuplicatedCharacteristicNameData[] = new DuplicatedCharacteristicNameData[vector.size()];
    vector.copyInto(objectDuplicatedCharacteristicNameData);
    return(objectDuplicatedCharacteristicNameData);
  }

/**
Checks if table exists in AD.
 */
  public static boolean tableExists(ConnectionProvider connectionProvider, String tableName)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "             select tablename" +
      "             from ad_table" +
      "             where lower(tablename) = ?";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, tableName);

      result = st.executeQuery();
      if(result.next()) {
        boolReturn = !UtilSql.getValue(result, "tablename").equals("0");
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
