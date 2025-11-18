package com.etendoerp.legacy.utilitySequence;

import java.sql.Connection;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.CSResponse;
import org.openbravo.erpCommon.utility.DocumentNoData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.erpCommon.utilitySequence.UtilitySequenceActionInterface;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.servlet.ServletException;

@ApplicationScoped
@Default
public class UtilitySequenceLegacy implements UtilitySequenceActionInterface {

  static Logger log4j = LogManager.getLogger();

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
  public String getDocumentNo(ConnectionProvider conn, VariablesSecureApp vars,
      String windowNo, String tableName, String c_DocTypeTarget_ID, String c_DocType_ID,
      boolean onlyDocType, boolean updateNext) {
    if (StringUtils.isBlank(tableName)) {
      throw new IllegalArgumentException("Utility.getDocumentNo - required parameter missing");
    }
    final String AD_Client_ID = Utility.getContext(conn, vars, "AD_Client_ID", windowNo);

    final String cDocTypeID = (StringUtils.isBlank(c_DocTypeTarget_ID) ? c_DocType_ID : c_DocTypeTarget_ID);
    if (StringUtils.isBlank(cDocTypeID)) {
      return getDocumentNo(conn, AD_Client_ID, tableName, updateNext);
    }

    if (StringUtils.equals(AD_Client_ID, "0")) {
      throw new UnsupportedOperationException("Utility.getDocumentNo - Cannot add System records");
    }

    CSResponse cs = null;
    try {
      cs = DocumentNoData.nextDocType(conn, cDocTypeID, AD_Client_ID, (updateNext ? "Y" : "N"));
    } catch (final ServletException e) {
      log4j.error(e);
    }

    if (cs == null || StringUtils.isBlank(cs.razon) || StringUtils.equals(cs.razon, "")) {
      if (!onlyDocType) {
        return getDocumentNo(conn, AD_Client_ID, tableName, updateNext);
      } else {
        return "0";
      }
    } else {
      return cs.razon;
    }
  }

  @Override
  public String getDocumentNo(Connection conn, ConnectionProvider con,
      VariablesSecureApp vars, String windowNo, String tableName, String c_DocTypeTarget_ID,
      String c_DocType_ID, boolean onlyDocType, boolean updateNext) {
    if (StringUtils.isBlank(tableName)) {
      throw new IllegalArgumentException("Utility.getDocumentNo - required parameter missing");
    }
    String AD_Client_ID = Utility.getContext(con, vars, "AD_Client_ID", windowNo);
    if (StringUtils.equals("", AD_Client_ID)) {
      AD_Client_ID = vars.getClient();
    }

    final String cDocTypeID = (StringUtils.equals("", c_DocTypeTarget_ID) ? c_DocType_ID : c_DocTypeTarget_ID);
    if (StringUtils.equals("", cDocTypeID)) {
      return getDocumentNo(con, AD_Client_ID, tableName, updateNext);
    }

    if (AD_Client_ID.equals("0")) {
      throw new UnsupportedOperationException("Utility.getDocumentNo - Cannot add System records");
    }

    CSResponse cs = null;
    try {

      cs = DocumentNoData.nextDocTypeConnection(conn, con, cDocTypeID, AD_Client_ID,
          (updateNext ? "Y" : "N"));
    } catch (final ServletException e) {
      log4j.error(e);
    }

    if (cs == null || StringUtils.isBlank(cs.razon) || StringUtils.equals(cs.razon, "")) {
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
   * @param ad_Client_ID
   *     String with the client id.
   * @param tableName
   *     Table name.
   * @param updateNext
   *     Save the new sequence in database.
   * @return String with the new document number.
   */
  @Override
  public String getDocumentNo(ConnectionProvider conn, String ad_Client_ID, String tableName,
      boolean updateNext) {
    if (tableName == null || tableName.length() == 0) {
      throw new IllegalArgumentException("Utility.getDocumentNo - required parameter missing");
    }

    CSResponse cs = null;
    try {
      cs = DocumentNoData.nextDoc(conn, "DocumentNo_" + tableName, ad_Client_ID,
          (updateNext ? "Y" : "N"));
    } catch (final ServletException e) {
      log4j.error(e);
    }

    if (cs == null || StringUtils.isBlank(cs.razon)) {
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
   * @param ad_Client_ID
   *     String with the client id.
   * @param tableName
   *     Table name.
   * @param updateNext
   *     Save the new sequence in database.
   * @return String with the new document number.
   */
  @Override
  public String getDocumentNoConnection(Connection conn, ConnectionProvider con,
      String ad_Client_ID, String tableName, boolean updateNext) {
    if (StringUtils.isBlank(tableName)) {
      throw new IllegalArgumentException("Utility.getDocumentNo - required parameter missing");
    }

    CSResponse cs = null;
    try {
      cs = DocumentNoData.nextDocConnection(conn, con, "DocumentNo_" + tableName, ad_Client_ID,
          (updateNext ? "Y" : "N"));
    } catch (final ServletException e) {
      log4j.error(e);
    }

    if (cs == null || StringUtils.isBlank(cs.razon)) {
      return "";
    } else {
      return cs.razon;
    }
  }
}
