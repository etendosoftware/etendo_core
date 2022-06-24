package org.openbravo.erpCommon.utilitySequence;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;

import java.sql.Connection;

public interface UtilitySequenceActionInterface {
    String getDocumentNo(ConnectionProvider conn, VariablesSecureApp vars,
                                       String WindowNo, String TableName, String C_DocTypeTarget_ID, String C_DocType_ID,
                                       boolean onlyDocType, boolean updateNext);

    String getDocumentNo(Connection conn, ConnectionProvider con,
                                       VariablesSecureApp vars, String WindowNo, String TableName, String C_DocTypeTarget_ID,
                                       String C_DocType_ID, boolean onlyDocType, boolean updateNext);

    String getDocumentNo(ConnectionProvider conn, String AD_Client_ID, String TableName,
                                       boolean updateNext);

    String getDocumentNoConnection(Connection conn, ConnectionProvider con,
                                                 String AD_Client_ID, String TableName, boolean updateNext);
}
