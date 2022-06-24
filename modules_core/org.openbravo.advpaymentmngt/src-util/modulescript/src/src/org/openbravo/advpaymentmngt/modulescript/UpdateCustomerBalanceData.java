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

class UpdateCustomerBalanceData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String cBpartnerId;
  public String customercredit;
  public String existpreference;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("c_bpartner_id") || fieldName.equals("cBpartnerId"))
      return cBpartnerId;
    else if (fieldName.equalsIgnoreCase("customercredit"))
      return customercredit;
    else if (fieldName.equalsIgnoreCase("existpreference"))
      return existpreference;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static UpdateCustomerBalanceData[] select(ConnectionProvider connectionProvider)    throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static UpdateCustomerBalanceData[] select(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT '' as c_bpartner_id, '' as customercredit, '' as existpreference FROM DUAL";

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
        UpdateCustomerBalanceData objectUpdateCustomerBalanceData = new UpdateCustomerBalanceData();
        objectUpdateCustomerBalanceData.cBpartnerId = UtilSql.getValue(result, "c_bpartner_id");
        objectUpdateCustomerBalanceData.customercredit = UtilSql.getValue(result, "customercredit");
        objectUpdateCustomerBalanceData.existpreference = UtilSql.getValue(result, "existpreference");
        objectUpdateCustomerBalanceData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUpdateCustomerBalanceData);
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
    UpdateCustomerBalanceData objectUpdateCustomerBalanceData[] = new UpdateCustomerBalanceData[vector.size()];
    vector.copyInto(objectUpdateCustomerBalanceData);
    return(objectUpdateCustomerBalanceData);
  }

  public static boolean isCustomerBalanceFixed(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT count(*) as existpreference" +
      "        FROM ad_preference" +
      "        WHERE attribute = 'IsCustomerBalanceRestoredV2'        ";

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

  public static boolean hasIsCustomerBalanceRestoredWithValue(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT count(*) as existpreference" +
      "        FROM ad_preference" +
      "        WHERE attribute = 'IsCustomerBalanceRestoredV2' AND to_char(value)='Y'        ";

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

  public static int deleteIsCustomerBalanceRestoredWithValue(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        DELETE FROM ad_preference" +
      "        WHERE attribute = 'IsCustomerBalanceRestoredV2' AND to_char(value)='Y'        ";

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

  public static int resetCustomerCredit(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE c_bpartner SET so_creditused = 0, updatedby='0', updated=now()";

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

  public static UpdateCustomerBalanceData[] calculateCustomerCredit(ConnectionProvider connectionProvider)    throws ServletException {
    return calculateCustomerCredit(connectionProvider, 0, 0);
  }

  public static UpdateCustomerBalanceData[] calculateCustomerCredit(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        SELECT A.c_bpartner_id, SUM(A.amount) as customercredit" +
      "        FROM (" +
      "          SELECT bp.c_bpartner_id, COALESCE(SUM(c_currency_convert(ps.outstandingamt * (CASE WHEN inv.issotrx = 'Y' THEN 1 ELSE -1 END), inv.c_currency_id, bp.bp_currency_id, inv.created, null, inv.ad_client_id, inv.ad_org_id)), 0) as amount" +
      "          FROM c_invoice inv" +
      "          JOIN c_bpartner bp" +
      "          ON inv.c_bpartner_id = bp.c_bpartner_id" +
      "          JOIN fin_payment_schedule ps" +
      "          ON inv.c_invoice_id = ps.c_invoice_id" +
      "          WHERE ps.outstandingamt <> 0" +
      "          GROUP BY bp.c_bpartner_id" +
      "          UNION ALL" +
      "          SELECT bp.c_bpartner_id, COALESCE(SUM(c_currency_convert((p.generated_credit - p.used_credit) * (CASE WHEN p.isreceipt = 'Y' THEN -1 ELSE 1 END), p.c_currency_id, bp.bp_currency_id, p.created, null, p.ad_client_id, p.ad_org_id)), 0) as amount" +
      "          FROM FIN_PAYMENT p" +
      "          JOIN c_bpartner bp" +
      "          ON p.c_bpartner_id = bp.c_bpartner_id" +
      "          WHERE (p.generated_credit - p.used_credit) <> 0" +
      "          AND p.generated_credit <> 0" +
      "          AND p.processed = 'Y'" +
      "          GROUP BY bp.c_bpartner_id" +
      "        ) A" +
      "        GROUP BY A.c_bpartner_id";

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
        UpdateCustomerBalanceData objectUpdateCustomerBalanceData = new UpdateCustomerBalanceData();
        objectUpdateCustomerBalanceData.cBpartnerId = UtilSql.getValue(result, "c_bpartner_id");
        objectUpdateCustomerBalanceData.customercredit = UtilSql.getValue(result, "customercredit");
        objectUpdateCustomerBalanceData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUpdateCustomerBalanceData);
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
    UpdateCustomerBalanceData objectUpdateCustomerBalanceData[] = new UpdateCustomerBalanceData[vector.size()];
    vector.copyInto(objectUpdateCustomerBalanceData);
    return(objectUpdateCustomerBalanceData);
  }

  public static int updateCustomerCredit(ConnectionProvider connectionProvider, String cumstomeCredit, String businessPartnerId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        UPDATE c_bpartner SET so_creditused = TO_NUMBER(?), updatedby='0', updated=now() WHERE c_bpartner_id = ?";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, cumstomeCredit);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, businessPartnerId);

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
      "          'IsCustomerBalanceRestoredV2'" +
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
