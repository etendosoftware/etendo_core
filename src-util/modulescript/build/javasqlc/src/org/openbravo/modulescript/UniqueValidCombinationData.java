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
class UniqueValidCombinationData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String count;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("count"))
      return count;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static UniqueValidCombinationData[] select(ConnectionProvider connectionProvider)    throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static UniqueValidCombinationData[] select(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        select 1 as count from dual";

    ResultSet result;
    Vector<UniqueValidCombinationData> vector = new Vector<UniqueValidCombinationData>(0);
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
        UniqueValidCombinationData objectUniqueValidCombinationData = new UniqueValidCombinationData();
        objectUniqueValidCombinationData.count = UtilSql.getValue(result, "count");
        objectUniqueValidCombinationData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUniqueValidCombinationData);
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
    UniqueValidCombinationData objectUniqueValidCombinationData[] = new UniqueValidCombinationData[vector.size()];
    vector.copyInto(objectUniqueValidCombinationData);
    return(objectUniqueValidCombinationData);
  }

  public static boolean selectDuplicates(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      select count(1) as count" +
      "        from (select 1" +
      "        from c_validcombination" +
      "        where M_PRODUCT_ID is null" +
      "        and C_BPARTNER_ID is null" +
      "        and C_PROJECT_ID is null" +
      "        and C_CAMPAIGN_ID is null" +
      "        and C_SALESREGION_ID is null" +
      "        and C_ACTIVITY_ID is null" +
      "        and USER1_ID is null" +
      "        and USER2_ID is null" +
      "        and isactive = 'Y'" +
      "        group by account_id, ad_client_id, c_acctschema_id" +
      "        having count(account_id) > 1) a";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if(result.next()) {
        boolReturn = !UtilSql.getValue(result, "count").equals("0");
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

  public static boolean selectMissingValidCombination(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        select count(1) as count" +
      "        from dual " +
      "        where exists (select 1 " +
      "                    from c_elementvalue " +
      "                    where not exists(select 1 " +
      "                                    from c_validcombination " +
      "                                    where c_validcombination.account_id = c_elementvalue_id " +
      "                                    and isactive='Y' " +
      "                                    and M_PRODUCT_ID is null" +
      "                                    and C_BPARTNER_ID is null" +
      "                                    and C_PROJECT_ID is null" +
      "                                    and C_CAMPAIGN_ID is null" +
      "                                    and C_SALESREGION_ID is null" +
      "                                    and C_ACTIVITY_ID is null" +
      "                                    and USER1_ID is null" +
      "                                    and USER2_ID is null)" +
      "                     and elementlevel = 'S'" +
      "                     and isactive = 'Y')";

    ResultSet result;
    boolean boolReturn = false;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if(result.next()) {
        boolReturn = !UtilSql.getValue(result, "count").equals("0");
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

  public static int updateJournalLineDimensions(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        update gl_journalline" +
      "        set " +
      "        M_PRODUCT_ID =coalesce(M_PRODUCT_ID, (select M_PRODUCT_ID from c_validcombination where c_validcombination_id = gl_journalline.c_validcombination_id))," +
      "        C_BPARTNER_ID =coalesce(C_BPARTNER_ID, (select C_BPARTNER_ID from c_validcombination where c_validcombination_id = gl_journalline.c_validcombination_id))," +
      "        C_PROJECT_ID =coalesce(C_PROJECT_ID, (select C_PROJECT_ID from c_validcombination where c_validcombination_id = gl_journalline.c_validcombination_id))," +
      "        C_CAMPAIGN_ID =coalesce(C_CAMPAIGN_ID, (select C_CAMPAIGN_ID from c_validcombination where c_validcombination_id = gl_journalline.c_validcombination_id))," +
      "        C_SALESREGION_ID =coalesce(C_SALESREGION_ID, (select C_SALESREGION_ID from c_validcombination where c_validcombination_id = gl_journalline.c_validcombination_id))," +
      "        C_ACTIVITY_ID =coalesce(C_ACTIVITY_ID, (select C_ACTIVITY_ID from c_validcombination where c_validcombination_id = gl_journalline.c_validcombination_id))," +
      "        USER1_ID =coalesce(USER1_ID, (select USER1_ID from c_validcombination where c_validcombination_id = gl_journalline.c_validcombination_id))," +
      "        USER2_ID =coalesce(USER2_ID, (select USER2_ID from c_validcombination where c_validcombination_id = gl_journalline.c_validcombination_id))," +
      "	UPDATED = NOW()" +
      "        where exists (select 1 from c_validcombination" +
      "                                    where (M_PRODUCT_ID is not null" +
      "                                    or C_BPARTNER_ID is not null" +
      "                                    or C_PROJECT_ID is not null" +
      "                                    or C_CAMPAIGN_ID is not null" +
      "                                    or C_SALESREGION_ID is not null" +
      "                                    or C_ACTIVITY_ID is not null" +
      "                                    or USER2_ID is not null" +
      "                                    or USER1_ID is not null)" +
      "                                    and c_validcombination_id = gl_journalline.c_validcombination_id)";

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

  public static int updateValidCombinationDeactivate(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        update c_validcombination set isactive='N', UPDATED = NOW()" +
      "        where (M_PRODUCT_ID is not null" +
      "        or C_BPARTNER_ID is not null" +
      "        or C_PROJECT_ID is not null" +
      "        or C_CAMPAIGN_ID is not null" +
      "        or C_SALESREGION_ID is not null" +
      "        or C_ACTIVITY_ID is not null" +
      "        or USER1_ID is not null" +
      "        or USER2_ID is not null)" +
      "	AND ISACTIVE = 'Y'";

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

  public static int updateValidCombinationDeactivateDuplicated(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        update c_validcombination set isactive='N', UPDATED = NOW()" +
      "        where account_id in (select account_id" +
      "                            from c_validcombination" +
      "                            where M_PRODUCT_ID is null" +
      "                            and C_BPARTNER_ID is null" +
      "                            and C_PROJECT_ID is null" +
      "                            and C_CAMPAIGN_ID is null" +
      "                            and C_SALESREGION_ID is null" +
      "                            and C_ACTIVITY_ID is null" +
      "                            and USER1_ID is null" +
      "                            and USER2_ID is null" +
      "			    AND ISACTIVE = 'Y'" +
      "                            group by account_id, ad_client_id, c_acctschema_id" +
      "                            having count(account_id) > 1)" +
      "        and c_validcombination_id not in (select max(c_validcombination_id)" +
      "                                        from c_validcombination" +
      "                                        where M_PRODUCT_ID is null" +
      "                                        and C_BPARTNER_ID is null" +
      "                                        and C_PROJECT_ID is null" +
      "                                        and C_CAMPAIGN_ID is null" +
      "                                        and C_SALESREGION_ID is null" +
      "                                        and C_ACTIVITY_ID is null" +
      "                                        and USER1_ID is null" +
      "                                        and USER2_ID is null" +
      "					AND ISACTIVE = 'Y'" +
      "                                        group by account_id, ad_client_id, c_acctschema_id" +
      "                                        having count(account_id) > 1" +
      "                                        )" +
      "        AND ISACTIVE='Y'";

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

  public static int insertValidCombinations(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "        INSERT INTO C_VALIDCOMBINATION" +
      "          (C_VALIDCOMBINATION_ID, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, CREATED, CREATEDBY," +
      "           UPDATED, UPDATEDBY, ALIAS, COMBINATION, DESCRIPTION, ISFULLYQUALIFIED," +
      "           C_ACCTSCHEMA_ID, ACCOUNT_ID, M_PRODUCT_ID, C_BPARTNER_ID, AD_ORGTRX_ID, C_LOCFROM_ID," +
      "           C_LOCTO_ID, C_SALESREGION_ID, C_PROJECT_ID, C_CAMPAIGN_ID, C_ACTIVITY_ID, USER1_ID," +
      "           USER2_ID)" +
      "        select get_uuid(), c_elementvalue.ad_client_id, c_elementvalue.ad_org_id, 'Y', now(), '100'," +
      "        now(), '100', c_elementvalue.value, c_elementvalue.value, '', 'Y', " +
      "        c_acctschema_element.c_acctschema_id, c_elementvalue.c_elementvalue_id, null, null, c_elementvalue.ad_org_id, null," +
      "        null, null, null, null, null, null, " +
      "        null" +
      "        from c_elementvalue, c_acctschema_element" +
      "        where c_elementvalue.elementlevel = 'S'" +
      "        and c_elementvalue.c_element_id = c_acctschema_element.c_element_id" +
      "        and not exists (select 1 from c_validcombination" +
      "                                        where c_validcombination.account_id = c_elementvalue.c_elementvalue_id" +
      "                                        and isactive='Y'" +
      "                                        and c_validcombination.c_acctschema_id = c_acctschema_element.c_acctschema_id)";

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
