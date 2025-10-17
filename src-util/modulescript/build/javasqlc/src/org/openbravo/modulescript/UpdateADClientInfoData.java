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
class UpdateADClientInfoData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String adClientId;
  public String uuid;
  public String clientname;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("ad_client_id") || fieldName.equals("adClientId"))
      return adClientId;
    else if (fieldName.equalsIgnoreCase("uuid"))
      return uuid;
    else if (fieldName.equalsIgnoreCase("clientname"))
      return clientname;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static UpdateADClientInfoData[] select(ConnectionProvider connectionProvider)    throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static UpdateADClientInfoData[] select(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT '' as ad_client_id, '' as UUID, '' as clientname" +
      "      FROM DUAL";

    ResultSet result;
    Vector<UpdateADClientInfoData> vector = new Vector<UpdateADClientInfoData>(0);
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
        UpdateADClientInfoData objectUpdateADClientInfoData = new UpdateADClientInfoData();
        objectUpdateADClientInfoData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectUpdateADClientInfoData.uuid = UtilSql.getValue(result, "uuid");
        objectUpdateADClientInfoData.clientname = UtilSql.getValue(result, "clientname");
        objectUpdateADClientInfoData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUpdateADClientInfoData);
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
    UpdateADClientInfoData objectUpdateADClientInfoData[] = new UpdateADClientInfoData[vector.size()];
    vector.copyInto(objectUpdateADClientInfoData);
    return(objectUpdateADClientInfoData);
  }

  public static int update(ConnectionProvider connectionProvider, String clientID)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      UPDATE AD_CLIENTINFO SET AD_TREE_CAMPAIGN_ID = " +
      "        (SELECT AD_TREE_ID FROM AD_TREE T " +
      "         WHERE TREETYPE = 'MC' AND AD_CLIENT_ID = ?) " +
      "        WHERE AD_CLIENT_ID = ? ";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientID);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientID);

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

  public static UpdateADClientInfoData[] selectClientsID(ConnectionProvider connectionProvider)    throws ServletException {
    return selectClientsID(connectionProvider, 0, 0);
  }

  public static UpdateADClientInfoData[] selectClientsID(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT AD_CLIENT_ID " +
      "      FROM AD_CLIENTINFO CI" +
      "      WHERE CI.AD_TREE_CAMPAIGN_ID IS NULL AND CI.AD_CLIENT_ID <> '0'";

    ResultSet result;
    Vector<UpdateADClientInfoData> vector = new Vector<UpdateADClientInfoData>(0);
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
        UpdateADClientInfoData objectUpdateADClientInfoData = new UpdateADClientInfoData();
        objectUpdateADClientInfoData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectUpdateADClientInfoData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUpdateADClientInfoData);
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
    UpdateADClientInfoData objectUpdateADClientInfoData[] = new UpdateADClientInfoData[vector.size()];
    vector.copyInto(objectUpdateADClientInfoData);
    return(objectUpdateADClientInfoData);
  }

  public static String getUUID(ConnectionProvider connectionProvider)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT get_uuid() as UUID" +
      "      FROM dual";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if(result.next()) {
        strReturn = UtilSql.getValue(result, "uuid");
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
    return(strReturn);
  }

  public static UpdateADClientInfoData[] selectClientsMissingTree(ConnectionProvider connectionProvider, String columnname)    throws ServletException {
    return selectClientsMissingTree(connectionProvider, columnname, 0, 0);
  }

  public static UpdateADClientInfoData[] selectClientsMissingTree(ConnectionProvider connectionProvider, String columnname, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT C.AD_CLIENT_ID, C.NAME as clientname" +
      "      FROM AD_CLIENTINFO CI INNER JOIN AD_CLIENT C ON (C.AD_CLIENT_ID = CI.AD_CLIENT_ID)" +
      "      WHERE CI.AD_CLIENT_ID <> '0'" +
      "      AND 1=1 AND CI.";
    strSql = strSql + ((columnname==null || columnname.equals(""))?"":columnname);
    strSql = strSql + 
      " IS NULL";

    ResultSet result;
    Vector<UpdateADClientInfoData> vector = new Vector<UpdateADClientInfoData>(0);
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);
      if (columnname != null && !(columnname.equals(""))) {
        }

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
        UpdateADClientInfoData objectUpdateADClientInfoData = new UpdateADClientInfoData();
        objectUpdateADClientInfoData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectUpdateADClientInfoData.clientname = UtilSql.getValue(result, "clientname");
        objectUpdateADClientInfoData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUpdateADClientInfoData);
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
    UpdateADClientInfoData objectUpdateADClientInfoData[] = new UpdateADClientInfoData[vector.size()];
    vector.copyInto(objectUpdateADClientInfoData);
    return(objectUpdateADClientInfoData);
  }

  public static int createTree(ConnectionProvider connectionProvider, String treeId, String clientId, String nameAndDesc, String treetype)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      INSERT INTO ad_tree(" +
      "            ad_tree_id, ad_client_id, ad_org_id, created, createdby, updated, " +
      "            updatedby, isactive, name, description, treetype, isallnodes)" +
      "      VALUES (?, ?, '0', now(), '0', now(), " +
      "            '0', 'Y', ?, ?, ?, 'Y')";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, treeId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientId);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, nameAndDesc);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, nameAndDesc);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, treetype);

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

  public static int updateClientTree(ConnectionProvider connectionProvider, String columnname, String treeID, String clientID)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      UPDATE AD_CLIENTINFO " +
      "      SET ";
    strSql = strSql + ((columnname==null || columnname.equals(""))?"":columnname);
    strSql = strSql + 
      " = ? " +
      "      WHERE AD_CLIENT_ID = ? ";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      if (columnname != null && !(columnname.equals(""))) {
        }
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, treeID);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientID);

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

  public static UpdateADClientInfoData[] selectClientsWithoutTree(ConnectionProvider connectionProvider, String columnname)    throws ServletException {
    return selectClientsWithoutTree(connectionProvider, columnname, 0, 0);
  }

  public static UpdateADClientInfoData[] selectClientsWithoutTree(ConnectionProvider connectionProvider, String columnname, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      SELECT AD_CLIENT_ID " +
      "      FROM AD_CLIENTINFO CI" +
      "      WHERE CI.";
    strSql = strSql + ((columnname==null || columnname.equals(""))?"":columnname);
    strSql = strSql + 
      " IS NULL " +
      "      AND CI.AD_CLIENT_ID <> '0'";

    ResultSet result;
    Vector<UpdateADClientInfoData> vector = new Vector<UpdateADClientInfoData>(0);
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);
      if (columnname != null && !(columnname.equals(""))) {
        }

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
        UpdateADClientInfoData objectUpdateADClientInfoData = new UpdateADClientInfoData();
        objectUpdateADClientInfoData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectUpdateADClientInfoData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUpdateADClientInfoData);
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
    UpdateADClientInfoData objectUpdateADClientInfoData[] = new UpdateADClientInfoData[vector.size()];
    vector.copyInto(objectUpdateADClientInfoData);
    return(objectUpdateADClientInfoData);
  }

/**
Updates client tree info for already created trees
 */
  public static int updateClientTreeAuto(ConnectionProvider connectionProvider, String columnname, String treetype, String clientID)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "      UPDATE AD_CLIENTINFO " +
      "      SET ";
    strSql = strSql + ((columnname==null || columnname.equals(""))?"":columnname);
    strSql = strSql + 
      " = " +
      "        (SELECT AD_TREE_ID FROM AD_TREE T " +
      "         WHERE TREETYPE = ? AND AD_CLIENT_ID = ?) " +
      "        WHERE AD_CLIENT_ID = ? ";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      if (columnname != null && !(columnname.equals(""))) {
        }
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, treetype);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientID);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, clientID);

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

  public static int insertMissingTreeNodes(ConnectionProvider connectionProvider, String treetype, String tablename)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "    INSERT" +
      "    INTO AD_TreeNode" +
      "      (" +
      "        ad_treeNode_Id, AD_Client_ID, AD_Org_ID, IsActive," +
      "        Created, CreatedBy, Updated," +
      "        UpdatedBy, AD_Tree_ID, Node_ID," +
      "        Parent_ID, SeqNo" +
      "      )" +
      "      SELECT get_uuid(), a.ad_client_id, a.ad_org_id, a.isactive," +
      "	now(), '0', now(), " +
      "	'0', (SELECT ad_tree_id from ad_tree where treetype = ? AND ad_tree.ad_client_id = a.ad_client_id) AS treeID, a.";
    strSql = strSql + ((tablename==null || tablename.equals(""))?"":tablename);
    strSql = strSql + 
      "_id," +
      "	'0', 999" +
      "      FROM ";
    strSql = strSql + ((tablename==null || tablename.equals(""))?"":tablename);
    strSql = strSql + 
      " a" +
      "      WHERE NOT EXISTS (SELECT 1 " +
      "                        FROM AD_TREENODE tn INNER JOIN AD_TREE t ON (t.ad_tree_id=tn.ad_tree_id)" +
      "                        WHERE t.treetype = ?" +
      "		        AND 1=1 AND a.";
    strSql = strSql + ((tablename==null || tablename.equals(""))?"":tablename);
    strSql = strSql + 
      "_id = tn.node_id)";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
    st = connectionProvider.getPreparedStatement(strSql);
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, treetype);
      if (tablename != null && !(tablename.equals(""))) {
        }
      if (tablename != null && !(tablename.equals(""))) {
        }
      iParameter++; UtilSql.setValue(st, iParameter, 12, null, treetype);
      if (tablename != null && !(tablename.equals(""))) {
        }

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
