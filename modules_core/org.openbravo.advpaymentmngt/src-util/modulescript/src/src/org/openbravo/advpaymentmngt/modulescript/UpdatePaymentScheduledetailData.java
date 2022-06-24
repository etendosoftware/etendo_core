//Sqlc generated V1.O00-1
package org.openbravo.advpaymentmngt.modulescript;

import java.sql.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;

import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.data.UtilSql;
import java.util.*;

class UpdatePaymentScheduledetailData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String finPaymentScheduledetailId;
  public String outstandingamt;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("fin_payment_scheduledetail_id") || fieldName.equals("finPaymentScheduledetailId"))
      return finPaymentScheduledetailId;
    else if (fieldName.equalsIgnoreCase("outstandingamt"))
      return outstandingamt;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static UpdatePaymentScheduledetailData[] select(ConnectionProvider connectionProvider)    throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static UpdatePaymentScheduledetailData[] select(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT '' as fin_payment_scheduledetail_id, '' as outstandingamt " +
      "        FROM DUAL";

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
        UpdatePaymentScheduledetailData objectUpdatePaymentScheduledetailData = new UpdatePaymentScheduledetailData();
        objectUpdatePaymentScheduledetailData.finPaymentScheduledetailId = UtilSql.getValue(result, "fin_payment_scheduledetail_id");
        objectUpdatePaymentScheduledetailData.outstandingamt = UtilSql.getValue(result, "outstandingamt");
        objectUpdatePaymentScheduledetailData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUpdatePaymentScheduledetailData);
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
    UpdatePaymentScheduledetailData objectUpdatePaymentScheduledetailData[] = new UpdatePaymentScheduledetailData[vector.size()];
    vector.copyInto(objectUpdatePaymentScheduledetailData);
    return(objectUpdatePaymentScheduledetailData);
  }

  public static UpdatePaymentScheduledetailData[] selectPSD(ConnectionProvider connectionProvider)    throws ServletException {
    return selectPSD(connectionProvider, 0, 0);
  }

  public static UpdatePaymentScheduledetailData[] selectPSD(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      select psd.fin_payment_scheduledetail_id as fin_payment_scheduledetail_id, ps.outstandingamt as outstandingamt from fin_payment_scheduledetail " +
      "      psd LEFT JOIN fin_payment_schedule ps ON ps.fin_payment_schedule_id = COALESCE(psd.fin_payment_schedule_invoice,psd.fin_payment_schedule_order)" +
      "      where psd.fin_payment_detail_id is null and psd.amount = 0 and psd.writeoffamt = 0";

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
        UpdatePaymentScheduledetailData objectUpdatePaymentScheduledetailData = new UpdatePaymentScheduledetailData();
        objectUpdatePaymentScheduledetailData.finPaymentScheduledetailId = UtilSql.getValue(result, "fin_payment_scheduledetail_id");
        objectUpdatePaymentScheduledetailData.outstandingamt = UtilSql.getValue(result, "outstandingamt");
        objectUpdatePaymentScheduledetailData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUpdatePaymentScheduledetailData);
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
    UpdatePaymentScheduledetailData objectUpdatePaymentScheduledetailData[] = new UpdatePaymentScheduledetailData[vector.size()];
    vector.copyInto(objectUpdatePaymentScheduledetailData);
    return(objectUpdatePaymentScheduledetailData);
  }

  public static int updatePaymentScheduledetailAmount(ConnectionProvider connectionProvider, String outStandingAmount, String finPaymentScheduledetailId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE FIN_PAYMENT_SCHEDULEDETAIL SET AMOUNT=TO_NUMBER(?)  " +
      "        WHERE FIN_PAYMENT_SCHEDULEDETAIL_ID = ?";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, outStandingAmount);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, finPaymentScheduledetailId);

      updateCount = st.executeUpdate();
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
    return(updateCount);
  }

  public static int removePaymentScheduledetail(ConnectionProvider connectionProvider, String finPaymentScheduledetailId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        DELETE FROM FIN_PAYMENT_SCHEDULEDETAIL WHERE FIN_PAYMENT_SCHEDULEDETAIL_ID = ? ";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, finPaymentScheduledetailId);

      updateCount = st.executeUpdate();
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
    return(updateCount);
  }

  public static boolean updateWrongPaymentScheduledetail(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT count(*) as exist" +
      "        FROM DUAL" +
      "        WHERE EXISTS (SELECT 1 FROM ad_preference" +
      "                      WHERE attribute = 'updateWrongPaymentScheduledetail')";

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

  public static int createPreference(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        INSERT INTO ad_preference (" +
      "          ad_preference_id, ad_client_id, ad_org_id, isactive," +
      "          createdby, created, updatedby, updated," +
      "          attribute" +
      "        ) VALUES (" +
      "          get_uuid(), '0', '0', 'Y'," +
      "          '0', NOW(), '0', NOW()," +
      "          'updateWrongPaymentScheduledetail'" +
      "        )";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      updateCount = st.executeUpdate();
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
    return(updateCount);
  }
}
