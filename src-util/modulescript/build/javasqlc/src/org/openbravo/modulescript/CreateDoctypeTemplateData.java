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
class CreateDoctypeTemplateData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String cDoctypeId;
  public String adClientId;
  public String name;
  public String vCount;
  public String docbasetype;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("c_doctype_id") || fieldName.equals("cDoctypeId"))
      return cDoctypeId;
    else if (fieldName.equalsIgnoreCase("ad_client_id") || fieldName.equals("adClientId"))
      return adClientId;
    else if (fieldName.equalsIgnoreCase("name"))
      return name;
    else if (fieldName.equalsIgnoreCase("v_count") || fieldName.equals("vCount"))
      return vCount;
    else if (fieldName.equalsIgnoreCase("docbasetype"))
      return docbasetype;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static CreateDoctypeTemplateData[] select(ConnectionProvider connectionProvider)    throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static CreateDoctypeTemplateData[] select(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      select c_doctype_id, ad_client_id, name,(select count(*) from c_poc_doctype_template ct where ct.c_doctype_id = c_doctype.c_doctype_id) AS v_count, docBaseType " +
      "      from c_doctype where docbasetype in ('MMR','MMS')";

    ResultSet result;
    Vector<CreateDoctypeTemplateData> vector = new Vector<CreateDoctypeTemplateData>(0);
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
        CreateDoctypeTemplateData objectCreateDoctypeTemplateData = new CreateDoctypeTemplateData();
        objectCreateDoctypeTemplateData.cDoctypeId = UtilSql.getValue(result, "c_doctype_id");
        objectCreateDoctypeTemplateData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectCreateDoctypeTemplateData.name = UtilSql.getValue(result, "name");
        objectCreateDoctypeTemplateData.vCount = UtilSql.getValue(result, "v_count");
        objectCreateDoctypeTemplateData.docbasetype = UtilSql.getValue(result, "docbasetype");
        objectCreateDoctypeTemplateData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectCreateDoctypeTemplateData);
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
    CreateDoctypeTemplateData objectCreateDoctypeTemplateData[] = new CreateDoctypeTemplateData[vector.size()];
    vector.copyInto(objectCreateDoctypeTemplateData);
    return(objectCreateDoctypeTemplateData);
  }

  public static int insertDoctypeTemplate(Connection conn, ConnectionProvider connectionProvider, String doctypetemplateId, String clientId, String doctypeId, String name, String templatelocation, String reportfilename, String templatefilename)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      INSERT INTO c_poc_doctype_template(" +
      "            c_poc_doctype_template_id, ad_client_id, ad_org_id, isactive, " +
      "            created, createdby, updated, updatedby, c_doctype_id, name, " +
      "            templatelocation, reportfilename, templatefilename)" +
      "      VALUES (?, ?, '0', 'Y', " +
      "            now(), '0', now(), '0', ?, ?, " +
      "            ?, ?, ?)";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(conn, strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, doctypetemplateId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, doctypeId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, name);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, templatelocation);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, reportfilename);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, templatefilename);

      SessionInfo.saveContextInfoIntoDB(conn);
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
        connectionProvider.releaseTransactionalPreparedStatement(st);
      } catch(Exception e){
        log4j.error("Error during release*Statement of query: " + strSql, e);
      }
    }
    return(updateCount);
  }

  public static int insertEmailDefinition(Connection conn, ConnectionProvider connectionProvider, String clientId, String doctypetemplateId)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      INSERT INTO c_poc_emaildefinition(" +
      "            c_poc_emaildefinition_id, ad_client_id, ad_org_id, isactive, " +
      "            created, createdby, updated, updatedby, c_poc_doctype_template_id, " +
      "            subject, body, isdefault, ad_language)" +
      "      VALUES (GET_UUID(), ?, '0', 'Y', " +
      "            now(), '0', now(), '0', ?, " +
      "            '', '', 'Y', 'en_US')";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(conn, strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, doctypetemplateId);

      SessionInfo.saveContextInfoIntoDB(conn);
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
        connectionProvider.releaseTransactionalPreparedStatement(st);
      } catch(Exception e){
        log4j.error("Error during release*Statement of query: " + strSql, e);
      }
    }
    return(updateCount);
  }
}
