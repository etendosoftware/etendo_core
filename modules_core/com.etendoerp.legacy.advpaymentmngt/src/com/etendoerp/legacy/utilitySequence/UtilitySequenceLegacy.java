package com.etendoerp.legacy.utilitySequence;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.CSResponse;
import org.openbravo.erpCommon.utility.DocumentNoData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.erpCommon.utilitySequence.UtilitySequenceActionInterface;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.servlet.ServletException;
import java.sql.Connection;

@ApplicationScoped
@Default
public class UtilitySequenceLegacy implements UtilitySequenceActionInterface {

  /**
   * Gets the document number from the database.
   *
   * @param conn
   *     Handler for the database connection.
   * @param vars
   *     Handler for the session info.
   * @param windowNo
   *     Window id.
   * @param tableName
   *     Table name.
   * @param c_DocTypeTarget_ID
   *     Id of the doctype target.
   * @param c_DocType_ID
   *     id of the doctype.
   * @param onlyDocType
   *     Search only for doctype.
   * @param updateNext
   *     Save the new sequence in database.
   * @return String with the new document number.
   */
  @Override
  public String getDocumentNo(ConnectionProvider conn, VariablesSecureApp vars, String windowNo,
      String tableName, String c_DocTypeTarget_ID, String c_DocType_ID, boolean onlyDocType,
      boolean updateNext) {
    if (StringUtils.isEmpty(tableName)) {
      throw new IllegalArgumentException("Utility.getDocumentNo - required parameter missing");
    }
    final String AD_Client_ID = Utility.getContext(conn, vars, "AD_Client_ID", windowNo);
    final String AD_Org_ID = Utility.getContext(conn, vars, "AD_Org_ID", windowNo);

    final String cDocTypeID = StringUtils.defaultIfBlank(c_DocTypeTarget_ID, c_DocType_ID);
    if (StringUtils.isEmpty(cDocTypeID)) {
      return getDocumentNoByOrg(conn, AD_Client_ID, AD_Org_ID, tableName, updateNext);
    }

    if (StringUtils.equals("0", AD_Client_ID)) {
      throw new UnsupportedOperationException("Utility.getDocumentNo - Cannot add System records");
    }

    CSResponse cs = null;
    try {
      cs = DocumentNoData.nextDocType(conn, cDocTypeID, AD_Client_ID, (updateNext ? "Y" : "N"));
    } catch (final ServletException e) {
      throw new OBException("Error: Failed to find the next sequence number");
    }

    if (cs == null || StringUtils.isBlank(cs.razon)) {
      if (!onlyDocType) {
        return getDocumentNoByOrg(conn, AD_Client_ID, AD_Org_ID, tableName, updateNext);
      } else {
        return "0";
      }
    } else {
      return cs.razon;
    }
  }

  @Override
  public String getDocumentNo(Connection conn, ConnectionProvider con, VariablesSecureApp vars,
      String windowNo, String tableName, String c_DocTypeTarget_ID, String c_DocType_ID,
      boolean onlyDocType, boolean updateNext) {
    if (StringUtils.isEmpty(tableName)) {
      throw new IllegalArgumentException("Utility.getDocumentNo - required parameter missing");
    }
    String AD_Client_ID = Utility.getContext(con, vars, "AD_Client_ID", windowNo);
    if (StringUtils.isBlank(AD_Client_ID)) {
      AD_Client_ID = vars.getClient();
    }
    final String AD_Org_ID = Utility.getContext(con, vars, "AD_Org_ID", windowNo);

    final String cDocTypeID = StringUtils.defaultIfBlank(c_DocTypeTarget_ID, c_DocType_ID);
    if (StringUtils.isBlank(cDocTypeID)) {
      return getDocumentNoByOrg(con, AD_Client_ID, AD_Org_ID, tableName, updateNext);
    }

    if (StringUtils.equals("0", AD_Client_ID)) {
      throw new UnsupportedOperationException("Utility.getDocumentNo - Cannot add System records");
    }

    CSResponse cs = null;
    try {

      cs = DocumentNoData.nextDocTypeConnection(conn, con, cDocTypeID, AD_Client_ID,
          (updateNext ? "Y" : "N"));
    } catch (final ServletException e) {
      throw new OBException("Error: Failed to find the next sequence number");
    }

    if (cs == null || StringUtils.isBlank(cs.razon)) {
      if (!onlyDocType) {
        return getDocumentNoConnection(conn, con, AD_Client_ID, tableName, updateNext);
      } else {
        return "0";
      }
    } else {
      return cs.razon;
    }
  }

  /**
   * Gets the document number from database.
   *
   * @param conn
   *     Handler for the database connection.
   * @param AD_Client_ID
   *     String with the client id.
   * @param tableName
   *     Table name.
   * @param updateNext
   *     Save the new sequence in database.
   * @return String with the new document number.
   */
  @Override
  public String getDocumentNo(ConnectionProvider conn, String AD_Client_ID, String tableName,
      boolean updateNext) {
    if (StringUtils.isEmpty(tableName)) {
      throw new IllegalArgumentException("Utility.getDocumentNo - required parameter missing");
    }

    CSResponse cs = null;
    try {
      cs = DocumentNoData.nextDoc(conn, "DocumentNo_" + tableName, AD_Client_ID,
          (updateNext ? "Y" : "N"));
    } catch (final ServletException e) {
    }

    if (cs == null || cs.razon == null) {
      return "";
    } else {
      return cs.razon;
    }
  }

  /**
   * Gets the document number from database.
   *
   * @param conn
   *     Handler for the database connection.
   * @param AD_Client_ID
   *     String with the client id.
   * @param AD_Org_ID
   *     String with the organization id.
   * @param tableName
   *     Table name.
   * @param updateNext
   *     Save the new sequence in database.
   * @return String with the new document number.
   */
  public String getDocumentNoByOrg(ConnectionProvider conn, String AD_Client_ID, String AD_Org_ID,
      String tableName, boolean updateNext) {
    if (StringUtils.isEmpty(tableName)) {
      throw new IllegalArgumentException("Utility.getDocumentNo - required parameter missing");
    }

    CSResponse cs = null;
    try {
      cs = DocumentNoData.nextDocByOrg(conn, "DocumentNo_" + tableName, AD_Client_ID, AD_Org_ID,
          (updateNext ? "Y" : "N"));
    } catch (final ServletException e) {
    }

    if (cs == null || cs.razon == null) {
      return "";
    } else {
      return cs.razon;
    }
  }

  /**
   * Gets the document number from database.
   *
   * @param conn
   *     Handler for the database connection.
   * @param AD_Client_ID
   *     String with the client id.
   * @param tableName
   *     Table name.
   * @param updateNext
   *     Save the new sequence in database.
   * @return String with the new document number.
   */
  @Override
  public String getDocumentNoConnection(Connection conn, ConnectionProvider con,
      String AD_Client_ID, String tableName, boolean updateNext) {
    if (StringUtils.isEmpty(tableName)) {
      throw new IllegalArgumentException("Utility.getDocumentNo - required parameter missing");
    }

    CSResponse cs = null;
    try {
      cs = DocumentNoData.nextDocConnection(conn, con, "DocumentNo_" + tableName, AD_Client_ID,
          (updateNext ? "Y" : "N"));
    } catch (final ServletException e) {
    }

    if (cs == null || cs.razon == null) {
      return "";
    } else {
      return cs.razon;
    }
  }
}
