//Sqlc generated V1.O00-1
package org.openbravo.advpaymentmngt.modulescript;

import java.sql.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;

import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.data.UtilSql;

class UpdateMatchedDocumentData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String existpreference;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("existpreference"))
      return existpreference;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static boolean isMatchedDocumentUpdated(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT count(*) as existpreference" +
      "        FROM ad_preference" +
      "        WHERE attribute = 'IsMatchedDocumentUpdated'        ";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if(result.next()) {
        boolReturn = !UtilSql.getValue(result, "existpreference").equals("0");
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

  public static int updateTransaction(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE fin_bankstatementline" +
      "           SET matched_document = 'T'" +
      "        WHERE fin_finacc_transaction_id IS NOT NULL" +
      "        AND   EXISTS (SELECT 1" +
      "                      FROM fin_finacc_transaction ft" +
      "                      WHERE ft.fin_finacc_transaction_id = fin_bankstatementline.fin_finacc_transaction_id" +
      "                      AND   ft.createdbyalgorithm = 'N')";

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

  public static int updatePayment(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE fin_bankstatementline" +
      "           SET matched_document = 'P'" +
      "        WHERE fin_finacc_transaction_id IS NOT NULL" +
      "        AND   EXISTS (SELECT 1" +
      "                      FROM fin_finacc_transaction ft," +
      "                           fin_payment" +
      "                      WHERE ft.fin_finacc_transaction_id = fin_bankstatementline.fin_finacc_transaction_id" +
      "                      AND   ft.createdbyalgorithm = 'Y'" +
      "                      AND   fin_payment.fin_payment_id = ft.fin_payment_id" +
      "                      AND   fin_payment.createdbyalgorithm = 'N')";

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

  public static int updateCredit(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE fin_bankstatementline" +
      "           SET matched_document = 'C'" +
      "        WHERE fin_finacc_transaction_id IS NOT NULL" +
      "        AND   EXISTS (SELECT 1" +
      "                      FROM fin_finacc_transaction ft," +
      "                           fin_payment," +
      "                           fin_payment_scheduledetail," +
      "                           fin_payment_detail" +
      "                      WHERE ft.fin_finacc_transaction_id = fin_bankstatementline.fin_finacc_transaction_id" +
      "                      AND   ft.createdbyalgorithm = 'Y'" +
      "                      AND   fin_payment.createdbyalgorithm = 'Y'" +
      "                      AND   fin_payment.fin_payment_id = ft.fin_payment_id" +
      "                      AND   fin_payment_scheduledetail.fin_payment_detail_id = fin_payment_detail.fin_payment_detail_id" +
      "                      AND   fin_payment_detail.fin_payment_id = fin_payment.fin_payment_id" +
      "                      AND   fin_payment_scheduledetail.fin_payment_schedule_order IS NULL" +
      "                      AND   fin_payment_scheduledetail.fin_payment_schedule_invoice IS NULL)";

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

  public static int updateInvoice(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE fin_bankstatementline" +
      "           SET matched_document = 'I'" +
      "        WHERE fin_finacc_transaction_id IS NOT NULL" +
      "        AND   EXISTS (SELECT 1" +
      "                      FROM fin_finacc_transaction ft," +
      "                           fin_payment," +
      "                           fin_payment_scheduledetail," +
      "                           fin_payment_detail" +
      "                      WHERE ft.fin_finacc_transaction_id = fin_bankstatementline.fin_finacc_transaction_id" +
      "                      AND   ft.createdbyalgorithm = 'Y'" +
      "                      AND   fin_payment.createdbyalgorithm = 'Y'" +
      "                      AND   fin_payment.fin_payment_id = ft.fin_payment_id" +
      "                      AND   fin_payment_scheduledetail.fin_payment_detail_id = fin_payment_detail.fin_payment_detail_id" +
      "                      AND   fin_payment_detail.fin_payment_id = fin_payment.fin_payment_id" +
      "                      AND   fin_payment_scheduledetail.fin_payment_schedule_invoice IS NOT NULL)";

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

  public static int updateOrder(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE fin_bankstatementline" +
      "           SET matched_document = 'O'" +
      "        WHERE fin_finacc_transaction_id IS NOT NULL" +
      "        AND   EXISTS (SELECT 1" +
      "                      FROM fin_finacc_transaction ft," +
      "                           fin_payment," +
      "                           fin_payment_scheduledetail," +
      "                           fin_payment_detail" +
      "                      WHERE ft.fin_finacc_transaction_id = fin_bankstatementline.fin_finacc_transaction_id" +
      "                      AND   ft.createdbyalgorithm = 'Y'" +
      "                      AND   fin_payment.createdbyalgorithm = 'Y'" +
      "                      AND   fin_payment.fin_payment_id = ft.fin_payment_id" +
      "                      AND   fin_payment_scheduledetail.fin_payment_detail_id = fin_payment_detail.fin_payment_detail_id" +
      "                      AND   fin_payment_detail.fin_payment_id = fin_payment.fin_payment_id" +
      "                      AND   fin_payment_scheduledetail.fin_payment_schedule_order IS NOT NULL" +
      "                      AND   fin_payment_scheduledetail.fin_payment_schedule_invoice IS NULL)";

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
      "          'IsMatchedDocumentUpdated'" +
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
