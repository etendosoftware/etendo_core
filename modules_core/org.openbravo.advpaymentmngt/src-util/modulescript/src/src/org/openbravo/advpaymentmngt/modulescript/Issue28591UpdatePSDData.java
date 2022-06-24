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

class Issue28591UpdatePSDData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String finPaymentScheduledetailId;
  public String outstandingamt;
  public String wrongamt;
  public String finPaymentScheduleId;
  public String cInvoiceId;
  public String cCurrencyId;
  public String bpCurrencyId;
  public String finPaymentId;
  public String finPaymentDetailId;
  public String isreceipt;
  public String cBpartnerId;
  public String paidamt;
  public String finPaymentProposalId;
  public String amount;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("FIN_PAYMENT_SCHEDULEDETAIL_ID") || fieldName.equals("finPaymentScheduledetailId"))
      return finPaymentScheduledetailId;
    else if (fieldName.equalsIgnoreCase("OUTSTANDINGAMT"))
      return outstandingamt;
    else if (fieldName.equalsIgnoreCase("WRONGAMT"))
      return wrongamt;
    else if (fieldName.equalsIgnoreCase("FIN_PAYMENT_SCHEDULE_ID") || fieldName.equals("finPaymentScheduleId"))
      return finPaymentScheduleId;
    else if (fieldName.equalsIgnoreCase("C_INVOICE_ID") || fieldName.equals("cInvoiceId"))
      return cInvoiceId;
    else if (fieldName.equalsIgnoreCase("C_CURRENCY_ID") || fieldName.equals("cCurrencyId"))
      return cCurrencyId;
    else if (fieldName.equalsIgnoreCase("BP_CURRENCY_ID") || fieldName.equals("bpCurrencyId"))
      return bpCurrencyId;
    else if (fieldName.equalsIgnoreCase("FIN_PAYMENT_ID") || fieldName.equals("finPaymentId"))
      return finPaymentId;
    else if (fieldName.equalsIgnoreCase("FIN_PAYMENT_DETAIL_ID") || fieldName.equals("finPaymentDetailId"))
      return finPaymentDetailId;
    else if (fieldName.equalsIgnoreCase("ISRECEIPT"))
      return isreceipt;
    else if (fieldName.equalsIgnoreCase("C_BPARTNER_ID") || fieldName.equals("cBpartnerId"))
      return cBpartnerId;
    else if (fieldName.equalsIgnoreCase("PAIDAMT"))
      return paidamt;
    else if (fieldName.equalsIgnoreCase("FIN_PAYMENT_PROPOSAL_ID") || fieldName.equals("finPaymentProposalId"))
      return finPaymentProposalId;
    else if (fieldName.equalsIgnoreCase("AMOUNT"))
      return amount;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static Issue28591UpdatePSDData[] select(ConnectionProvider connectionProvider)    throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static Issue28591UpdatePSDData[] select(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT '' as fin_payment_scheduledetail_id, '' as outstandingamt, '' as wrongamt, " +
      "        '' as fin_payment_schedule_id, '' as c_invoice_id, '' as c_currency_id, '' as bp_currency_id," +
      "        '' as fin_payment_id, '' as fin_payment_detail_id, '' as isreceipt, '' as c_bpartner_id, '' as paidamt," +
      "        '' as fin_payment_proposal_id, '' as amount " +
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
        Issue28591UpdatePSDData objectIssue28591UpdatePSDData = new Issue28591UpdatePSDData();
        objectIssue28591UpdatePSDData.finPaymentScheduledetailId = UtilSql.getValue(result, "FIN_PAYMENT_SCHEDULEDETAIL_ID");
        objectIssue28591UpdatePSDData.outstandingamt = UtilSql.getValue(result, "OUTSTANDINGAMT");
        objectIssue28591UpdatePSDData.wrongamt = UtilSql.getValue(result, "WRONGAMT");
        objectIssue28591UpdatePSDData.finPaymentScheduleId = UtilSql.getValue(result, "FIN_PAYMENT_SCHEDULE_ID");
        objectIssue28591UpdatePSDData.cInvoiceId = UtilSql.getValue(result, "C_INVOICE_ID");
        objectIssue28591UpdatePSDData.cCurrencyId = UtilSql.getValue(result, "C_CURRENCY_ID");
        objectIssue28591UpdatePSDData.bpCurrencyId = UtilSql.getValue(result, "BP_CURRENCY_ID");
        objectIssue28591UpdatePSDData.finPaymentId = UtilSql.getValue(result, "FIN_PAYMENT_ID");
        objectIssue28591UpdatePSDData.finPaymentDetailId = UtilSql.getValue(result, "FIN_PAYMENT_DETAIL_ID");
        objectIssue28591UpdatePSDData.isreceipt = UtilSql.getValue(result, "ISRECEIPT");
        objectIssue28591UpdatePSDData.cBpartnerId = UtilSql.getValue(result, "C_BPARTNER_ID");
        objectIssue28591UpdatePSDData.paidamt = UtilSql.getValue(result, "PAIDAMT");
        objectIssue28591UpdatePSDData.finPaymentProposalId = UtilSql.getValue(result, "FIN_PAYMENT_PROPOSAL_ID");
        objectIssue28591UpdatePSDData.amount = UtilSql.getValue(result, "AMOUNT");
        objectIssue28591UpdatePSDData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectIssue28591UpdatePSDData);
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
    Issue28591UpdatePSDData objectIssue28591UpdatePSDData[] = new Issue28591UpdatePSDData[vector.size()];
    vector.copyInto(objectIssue28591UpdatePSDData);
    return(objectIssue28591UpdatePSDData);
  }

  public static Issue28591UpdatePSDData[] selectPS(ConnectionProvider connectionProvider)    throws ServletException {
    return selectPS(connectionProvider, 0, 0);
  }

  public static Issue28591UpdatePSDData[] selectPS(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      select  sum(ps.paidamt) as paidamt, sum (ps.outstandingamt) as outstandingamt, i.c_invoice_id" +
      "      from FIN_Payment_Schedule ps , c_invoice i" +
      "      where ps.c_invoice_id=i.c_invoice_id" +
      "      and i.ispaid='N'" +
      "      group by i.c_invoice_id , i.totalpaid" +
      "      having i.totalpaid <> sum(ps.paidamt)  and sum (ps.outstandingamt) =0";

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
        Issue28591UpdatePSDData objectIssue28591UpdatePSDData = new Issue28591UpdatePSDData();
        objectIssue28591UpdatePSDData.paidamt = UtilSql.getValue(result, "PAIDAMT");
        objectIssue28591UpdatePSDData.outstandingamt = UtilSql.getValue(result, "OUTSTANDINGAMT");
        objectIssue28591UpdatePSDData.cInvoiceId = UtilSql.getValue(result, "C_INVOICE_ID");
        objectIssue28591UpdatePSDData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectIssue28591UpdatePSDData);
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
    Issue28591UpdatePSDData objectIssue28591UpdatePSDData[] = new Issue28591UpdatePSDData[vector.size()];
    vector.copyInto(objectIssue28591UpdatePSDData);
    return(objectIssue28591UpdatePSDData);
  }

  public static int updateFinPaymentschedule(ConnectionProvider connectionProvider, String outstandingamt, String paidamt, String invoiceId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      update fin_payment_schedule set paidamt=to_number(?) , outstandingamt= to_number(?) where c_invoice_id=?        ";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, outstandingamt);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, paidamt);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, invoiceId);

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

  public static Issue28591UpdatePSDData[] selectPSD(ConnectionProvider connectionProvider)    throws ServletException {
    return selectPSD(connectionProvider, 0, 0);
  }

  public static Issue28591UpdatePSDData[] selectPSD(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      select ps.outstandingamt as outstandingamt, max(psd.fin_payment_scheduledetail_id) as fin_payment_scheduledetail_id" +
      "      from fin_payment_scheduledetail psd" +
      "      LEFT JOIN fin_payment_schedule ps ON ps.fin_payment_schedule_id = COALESCE(psd.fin_payment_schedule_invoice,psd.fin_payment_schedule_order)" +
      "      where psd.fin_payment_detail_id is null and ps.outstandingamt > 0" +
      "      group by ps.outstandingamt, ps.fin_payment_schedule_id" +
      "      having sum(psd.amount) <> ps.outstandingamt";

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
        Issue28591UpdatePSDData objectIssue28591UpdatePSDData = new Issue28591UpdatePSDData();
        objectIssue28591UpdatePSDData.outstandingamt = UtilSql.getValue(result, "OUTSTANDINGAMT");
        objectIssue28591UpdatePSDData.finPaymentScheduledetailId = UtilSql.getValue(result, "FIN_PAYMENT_SCHEDULEDETAIL_ID");
        objectIssue28591UpdatePSDData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectIssue28591UpdatePSDData);
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
    Issue28591UpdatePSDData objectIssue28591UpdatePSDData[] = new Issue28591UpdatePSDData[vector.size()];
    vector.copyInto(objectIssue28591UpdatePSDData);
    return(objectIssue28591UpdatePSDData);
  }

  public static int updatePSDAmount(ConnectionProvider connectionProvider, String outStandingAmount, String finPaymentScheduledetailId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE FIN_PAYMENT_SCHEDULEDETAIL SET AMOUNT=TO_NUMBER(?)," +
      "        updatedby='0', updated=now()" +
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

  public static boolean updateWrongPSD(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT count(*) as exist" +
      "        FROM DUAL" +
      "        WHERE EXISTS (SELECT 1 FROM ad_preference" +
      "                      WHERE attribute = 'Issue28591updateWrongPSD2')";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if(result.next()) {
        boolReturn = !UtilSql.getValue(result, "EXIST").equals("0");
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

  public static Issue28591UpdatePSDData[] selectPaymentProposal(ConnectionProvider connectionProvider)    throws ServletException {
    return selectPaymentProposal(connectionProvider, 0, 0);
  }

  public static Issue28591UpdatePSDData[] selectPaymentProposal(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "       select pp.fin_payment_proposal_id, sum(psd.amount) as amount" +
      "       from fin_payment_proposal pp, fin_payment_prop_detail ppd, fin_payment_scheduledetail psd" +
      "       where pp.fin_payment_proposal_id = ppd.fin_payment_proposal_id" +
      "       and ppd.fin_payment_scheduledetail_id = psd.fin_payment_scheduledetail_id" +
      "       and ppd.amount > psd.amount" +
      "       and psd.fin_payment_detail_id IS NULL" +
      "       group by pp.fin_payment_proposal_id, pp.amount";

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
        Issue28591UpdatePSDData objectIssue28591UpdatePSDData = new Issue28591UpdatePSDData();
        objectIssue28591UpdatePSDData.finPaymentProposalId = UtilSql.getValue(result, "FIN_PAYMENT_PROPOSAL_ID");
        objectIssue28591UpdatePSDData.amount = UtilSql.getValue(result, "AMOUNT");
        objectIssue28591UpdatePSDData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectIssue28591UpdatePSDData);
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
    Issue28591UpdatePSDData objectIssue28591UpdatePSDData[] = new Issue28591UpdatePSDData[vector.size()];
    vector.copyInto(objectIssue28591UpdatePSDData);
    return(objectIssue28591UpdatePSDData);
  }

  public static int updatePaymentProposal(ConnectionProvider connectionProvider, String sumAmt, String finPaymentProposalId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      update fin_payment_proposal" +
      "      set amount = to_number(?)" +
      "      where fin_payment_proposal_id =?";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, sumAmt);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, finPaymentProposalId);

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

  public static int updatePaymentProp(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "       update fin_payment_prop_detail ppd" +
      "       set amount = ( select psd.amount  from fin_payment_scheduledetail  psd  where ppd.fin_payment_scheduledetail_id = psd.fin_payment_scheduledetail_id     and ppd.amount > psd.amount" +
      "       and psd.fin_payment_detail_id IS NULL)" +
      "       WHERE EXISTS ( select psd.amount  from fin_payment_scheduledetail psd  where ppd.fin_payment_scheduledetail_id = psd.fin_payment_scheduledetail_id     and ppd.amount > psd.amount" +
      "       and psd.fin_payment_detail_id IS NULL)";

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
      "          'Issue28591updateWrongPSD2'" +
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
