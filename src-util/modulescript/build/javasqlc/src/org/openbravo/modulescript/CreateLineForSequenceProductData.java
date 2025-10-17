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
class CreateLineForSequenceProductData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String maSequenceId;
  public String total;
  public String maSequenceproductId;
  public String num;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("ma_sequence_id") || fieldName.equals("maSequenceId"))
      return maSequenceId;
    else if (fieldName.equalsIgnoreCase("total"))
      return total;
    else if (fieldName.equalsIgnoreCase("ma_sequenceproduct_id") || fieldName.equals("maSequenceproductId"))
      return maSequenceproductId;
    else if (fieldName.equalsIgnoreCase("num"))
      return num;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static CreateLineForSequenceProductData[] select(ConnectionProvider connectionProvider)    throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static CreateLineForSequenceProductData[] select(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT MA_SEQUENCE.MA_SEQUENCE_ID, '' AS TOTAL, '' AS MA_SEQUENCEPRODUCT_ID, '' AS NUM" +
      "      FROM MA_SEQUENCEPRODUCT " +
      "          JOIN MA_SEQUENCE ON MA_SEQUENCEPRODUCT.MA_SEQUENCE_ID = MA_SEQUENCE.MA_SEQUENCE_ID" +
      "      WHERE MA_SEQUENCEPRODUCT.LINE IS NULL" +
      "      GROUP BY MA_SEQUENCE.MA_SEQUENCE_ID";

    ResultSet result;
    Vector<CreateLineForSequenceProductData> vector = new Vector<CreateLineForSequenceProductData>(0);
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
        CreateLineForSequenceProductData objectCreateLineForSequenceProductData = new CreateLineForSequenceProductData();
        objectCreateLineForSequenceProductData.maSequenceId = UtilSql.getValue(result, "ma_sequence_id");
        objectCreateLineForSequenceProductData.total = UtilSql.getValue(result, "total");
        objectCreateLineForSequenceProductData.maSequenceproductId = UtilSql.getValue(result, "ma_sequenceproduct_id");
        objectCreateLineForSequenceProductData.num = UtilSql.getValue(result, "num");
        objectCreateLineForSequenceProductData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectCreateLineForSequenceProductData);
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
    CreateLineForSequenceProductData objectCreateLineForSequenceProductData[] = new CreateLineForSequenceProductData[vector.size()];
    vector.copyInto(objectCreateLineForSequenceProductData);
    return(objectCreateLineForSequenceProductData);
  }

  public static boolean existsNull(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT COUNT(1) AS NUM FROM DUAL" +
      "     	WHERE EXISTS (SELECT 1 FROM MA_SEQUENCEPRODUCT WHERE LINE IS NULL)";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if(result.next()) {
        boolReturn = !UtilSql.getValue(result, "num").equals("0");
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

  public static CreateLineForSequenceProductData[] selectSequenceProducts(ConnectionProvider connectionProvider, String maSequenceId)    throws ServletException {
    return selectSequenceProducts(connectionProvider, maSequenceId, 0, 0);
  }

  public static CreateLineForSequenceProductData[] selectSequenceProducts(ConnectionProvider connectionProvider, String maSequenceId, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT MA_SEQUENCEPRODUCT_ID" +
      "        FROM MA_SEQUENCEPRODUCT" +
      "        WHERE MA_SEQUENCE_ID = ?" +
      "        ORDER BY PRODUCTIONTYPE";

    ResultSet result;
    Vector<CreateLineForSequenceProductData> vector = new Vector<CreateLineForSequenceProductData>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, maSequenceId);

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
        CreateLineForSequenceProductData objectCreateLineForSequenceProductData = new CreateLineForSequenceProductData();
        objectCreateLineForSequenceProductData.maSequenceproductId = UtilSql.getValue(result, "ma_sequenceproduct_id");
        objectCreateLineForSequenceProductData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectCreateLineForSequenceProductData);
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
    CreateLineForSequenceProductData objectCreateLineForSequenceProductData[] = new CreateLineForSequenceProductData[vector.size()];
    vector.copyInto(objectCreateLineForSequenceProductData);
    return(objectCreateLineForSequenceProductData);
  }

  public static int updateline(ConnectionProvider connectionProvider, String line, String maSequenceProductId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      UPDATE MA_SEQUENCEPRODUCT SET LINE = TO_NUMBER(?) WHERE MA_SEQUENCEPRODUCT_ID = ?";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, line);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, maSequenceProductId);

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
