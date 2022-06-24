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
class DeleteDuplicateCommissionDetailData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String cCommissionamtId;
  public String cCommissionrunId;
  public String amtresult;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("c_commissionamt_id") || fieldName.equals("cCommissionamtId"))
      return cCommissionamtId;
    else if (fieldName.equalsIgnoreCase("c_commissionrun_id") || fieldName.equals("cCommissionrunId"))
      return cCommissionrunId;
    else if (fieldName.equalsIgnoreCase("amtresult"))
      return amtresult;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static DeleteDuplicateCommissionDetailData[] select(ConnectionProvider connectionProvider)    throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static DeleteDuplicateCommissionDetailData[] select(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "       SELECT CAMT.C_COMMISSIONAMT_ID, " +
      "              CAMT.C_COMMISSIONRUN_ID, " +
      "              CASE WHEN (((CAMT.ACTUALQTY - CLINE.QTYSUBTRACT) * CLINE.QTYMULTIPLIER) < 0 AND CLINE.ISPOSITIVEONLY = 'Y') THEN 0 ELSE ((CAMT.ACTUALQTY - CLINE.QTYSUBTRACT) * CLINE.QTYMULTIPLIER) END " +
      "              + CASE WHEN (((CAMT.CONVERTEDAMT - CLINE.AMTSUBTRACT) * CLINE.AMTMULTIPLIER) < 0 AND CLINE.ISPOSITIVEONLY = 'Y') THEN 0 ELSE ((CAMT.CONVERTEDAMT - CLINE.AMTSUBTRACT) * CLINE.AMTMULTIPLIER) END AS AMTRESULT " +
      "       FROM C_COMMISSIONAMT CAMT " +
      "       JOIN C_COMMISSIONLINE CLINE ON CAMT.C_COMMISSIONLINE_ID = CLINE.C_COMMISSIONLINE_ID";

    ResultSet result;
    Vector<DeleteDuplicateCommissionDetailData> vector = new Vector<DeleteDuplicateCommissionDetailData>(0);
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
        DeleteDuplicateCommissionDetailData objectDeleteDuplicateCommissionDetailData = new DeleteDuplicateCommissionDetailData();
        objectDeleteDuplicateCommissionDetailData.cCommissionamtId = UtilSql.getValue(result, "c_commissionamt_id");
        objectDeleteDuplicateCommissionDetailData.cCommissionrunId = UtilSql.getValue(result, "c_commissionrun_id");
        objectDeleteDuplicateCommissionDetailData.amtresult = UtilSql.getValue(result, "amtresult");
        objectDeleteDuplicateCommissionDetailData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectDeleteDuplicateCommissionDetailData);
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
    DeleteDuplicateCommissionDetailData objectDeleteDuplicateCommissionDetailData[] = new DeleteDuplicateCommissionDetailData[vector.size()];
    vector.copyInto(objectDeleteDuplicateCommissionDetailData);
    return(objectDeleteDuplicateCommissionDetailData);
  }

  public static int deleteDuplicateCommissionDetail(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      DELETE FROM C_COMMISSIONDETAIL " +
      "         WHERE C_COMMISSIONDETAIL_ID IN (" +
      "         SELECT DISTINCT T2.C_COMMISSIONDETAIL_ID " +
      "         FROM C_COMMISSIONDETAIL T1, C_COMMISSIONDETAIL T2 " +
      "         WHERE T1.C_COMMISSIONAMT_ID = T2.C_COMMISSIONAMT_ID AND T1.C_INVOICELINE_ID = T2.C_INVOICELINE_ID " +
      "         AND T1.C_COMMISSIONDETAIL_ID  < T2.C_COMMISSIONDETAIL_ID " +
      "        )";

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

  public static int updateCommissionQty(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      UPDATE C_COMMISSIONAMT CAMT" +
      "      SET ACTUALQTY = CASE WHEN (SELECT SUM(ACTUALQTY)" +
      "                      FROM C_COMMISSIONDETAIL" +
      "                      WHERE C_COMMISSIONAMT_ID = CAMT.C_COMMISSIONAMT_ID) IS NULL THEN 0" +
      "                      ELSE ((SELECT SUM(ACTUALQTY)" +
      "                      FROM C_COMMISSIONDETAIL" +
      "                      WHERE C_COMMISSIONAMT_ID = CAMT.C_COMMISSIONAMT_ID)) END," +
      "          CONVERTEDAMT = CASE WHEN (SELECT SUM(CONVERTEDAMT)" +
      "                         FROM C_COMMISSIONDETAIL" +
      "                         WHERE C_COMMISSIONAMT_ID = CAMT.C_COMMISSIONAMT_ID) IS NULL THEN 0" +
      "                         ELSE ((SELECT SUM(CONVERTEDAMT)" +
      "                         FROM C_COMMISSIONDETAIL" +
      "                         WHERE C_COMMISSIONAMT_ID = CAMT.C_COMMISSIONAMT_ID)) END";

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

  public static int updateCommissionAmount(ConnectionProvider connectionProvider, String amount, String commissionAmtId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      UPDATE C_COMMISSIONAMT " +
      "      SET COMMISSIONAMT = TO_NUMBER(?) " +
      "      WHERE C_COMMISSIONAMT_ID = ?";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, amount);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, commissionAmtId);

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

  public static int updateCommissionPayment(ConnectionProvider connectionProvider, String amount, String commissionRunId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      UPDATE C_COMMISSIONRUN" +
      "      SET GRANDTOTAL = TO_NUMBER(?)" +
      "      WHERE C_COMMISSIONRUN_ID = ?";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, amount);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, commissionRunId);

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

  public static boolean isDeleteDuplicateCommissionDetailExecuted(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT count(*) as exist" +
      "        FROM DUAL" +
      "        WHERE EXISTS (SELECT 1 FROM ad_preference" +
      "                      WHERE attribute = 'DeleteDuplicateCommissionDetail')";

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

  public static int createPreference(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "           INSERT INTO ad_preference (" +
      "           ad_preference_id, ad_client_id, ad_org_id, isactive," +
      "           createdby, created, updatedby, updated,attribute" +
      "           ) VALUES (" +
      "           get_uuid(), '0', '0', 'Y', '0', NOW(), '0', NOW(),'DeleteDuplicateCommissionDetail')";

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
