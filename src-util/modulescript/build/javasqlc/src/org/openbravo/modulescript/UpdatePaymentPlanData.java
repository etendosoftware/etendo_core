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
class UpdatePaymentPlanData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String id;
  public String amount;
  public String rownum;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("id"))
      return id;
    else if (fieldName.equalsIgnoreCase("amount"))
      return amount;
    else if (fieldName.equals("rownum"))
      return rownum;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static UpdatePaymentPlanData[] dummy(ConnectionProvider connectionProvider)    throws ServletException {
    return dummy(connectionProvider, 0, 0);
  }

  public static UpdatePaymentPlanData[] dummy(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT '' AS id, '' AS amount FROM DUAL";

    ResultSet result;
    Vector<UpdatePaymentPlanData> vector = new Vector<UpdatePaymentPlanData>(0);
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
        UpdatePaymentPlanData objectUpdatePaymentPlanData = new UpdatePaymentPlanData();
        objectUpdatePaymentPlanData.id = UtilSql.getValue(result, "id");
        objectUpdatePaymentPlanData.amount = UtilSql.getValue(result, "amount");
        objectUpdatePaymentPlanData.rownum = Long.toString(countRecord);
        objectUpdatePaymentPlanData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUpdatePaymentPlanData);
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
    UpdatePaymentPlanData objectUpdatePaymentPlanData[] = new UpdatePaymentPlanData[vector.size()];
    vector.copyInto(objectUpdatePaymentPlanData);
    return(objectUpdatePaymentPlanData);
  }

  public static UpdatePaymentPlanData[] getWrongRecords(ConnectionProvider connectionProvider)    throws ServletException {
    return getWrongRecords(connectionProvider, 0, 0);
  }

  public static UpdatePaymentPlanData[] getWrongRecords(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT ps.fin_payment_schedule_id as id, sum(psd.amount + COALESCE(psd.writeoffamt, 0)) as amount" +
      "        FROM fin_payment_scheduledetail psd" +
      "        INNER JOIN fin_payment_schedule ps " +
      "        ON (ps.fin_payment_schedule_id = psd.fin_payment_schedule_order " +
      "        OR ps.fin_payment_schedule_id = psd.fin_payment_schedule_invoice)" +
      "        WHERE psd.isinvoicepaid = 'Y'" +
      "        AND psd.iscanceled = 'N' " +
      "        AND psd.fin_payment_detail_id IS NOT NULL" +
      "        GROUP BY ps.fin_payment_schedule_id, ps.paidamt, ps.outstandingamt, ps.amount" +
      "        HAVING (ps.paidamt <> sum(psd.amount + COALESCE(psd.writeoffamt, 0))" +
      "        OR (ps.outstandingamt <> ps.amount - sum(psd.amount + COALESCE(psd.writeoffamt, 0))))";

    ResultSet result;
    Vector<UpdatePaymentPlanData> vector = new Vector<UpdatePaymentPlanData>(0);
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
        UpdatePaymentPlanData objectUpdatePaymentPlanData = new UpdatePaymentPlanData();
        objectUpdatePaymentPlanData.id = UtilSql.getValue(result, "id");
        objectUpdatePaymentPlanData.amount = UtilSql.getValue(result, "amount");
        objectUpdatePaymentPlanData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUpdatePaymentPlanData);
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
    UpdatePaymentPlanData objectUpdatePaymentPlanData[] = new UpdatePaymentPlanData[vector.size()];
    vector.copyInto(objectUpdatePaymentPlanData);
    return(objectUpdatePaymentPlanData);
  }

  public static int update(ConnectionProvider connectionProvider, String amount1, String amount2, String id)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE fin_payment_schedule " +
      "        SET paidamt = to_number(?), outstandingamt = amount - to_number(?)" +
      "        WHERE fin_payment_schedule_id = ?";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, amount1);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, amount2);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, id);

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

  public static boolean isExecuted(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT count(*) as exist" +
      "        FROM DUAL" +
      "        WHERE EXISTS (SELECT 1 FROM ad_preference" +
      "                      WHERE attribute = 'PaymentPlanUpdatedV4')";

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
      "           get_uuid(), '0', '0', 'Y', '0', NOW(), '0', NOW(),'PaymentPlanUpdatedV4')";

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
