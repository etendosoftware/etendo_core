package com.etendoerp.legacy.utilitySequence;

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
     *          Handler for the database connection.
     * @param vars
     *          Handler for the session info.
     * @param WindowNo
     *          Window id.
     * @param TableName
     *          Table name.
     * @param C_DocTypeTarget_ID
     *          Id of the doctype target.
     * @param C_DocType_ID
     *          id of the doctype.
     * @param onlyDocType
     *          Search only for doctype.
     * @param updateNext
     *          Save the new sequence in database.
     * @return String with the new document number.
     */
    @Override
    public String getDocumentNo(ConnectionProvider conn, VariablesSecureApp vars,
                                       String WindowNo, String TableName, String C_DocTypeTarget_ID, String C_DocType_ID,
                                       boolean onlyDocType, boolean updateNext) {
        if (TableName == null || TableName.length() == 0) {
            throw new IllegalArgumentException("Utility.getDocumentNo - required parameter missing");
        }
        final String AD_Client_ID = Utility.getContext(conn, vars, "AD_Client_ID", WindowNo);

        final String cDocTypeID = (C_DocTypeTarget_ID.equals("") ? C_DocType_ID : C_DocTypeTarget_ID);
        if (cDocTypeID.equals("")) {
            return getDocumentNo(conn, AD_Client_ID, TableName, updateNext);
        }

        if (AD_Client_ID.equals("0")) {
            throw new UnsupportedOperationException("Utility.getDocumentNo - Cannot add System records");
        }

        CSResponse cs = null;
        try {
            cs = DocumentNoData.nextDocType(conn, cDocTypeID, AD_Client_ID, (updateNext ? "Y" : "N"));
        } catch (final ServletException e) {
        }

        if (cs == null || cs.razon == null || cs.razon.equals("")) {
            if (!onlyDocType) {
                return getDocumentNo(conn, AD_Client_ID, TableName, updateNext);
            } else {
                return "0";
            }
        } else {
            return cs.razon;
        }
    }

    @Override
    public String getDocumentNo(Connection conn, ConnectionProvider con,
                                       VariablesSecureApp vars, String WindowNo, String TableName, String C_DocTypeTarget_ID,
                                       String C_DocType_ID, boolean onlyDocType, boolean updateNext) {
        if (TableName == null || TableName.length() == 0) {
            throw new IllegalArgumentException("Utility.getDocumentNo - required parameter missing");
        }
        String AD_Client_ID = Utility.getContext(con, vars, "AD_Client_ID", WindowNo);
        if ("".equals(AD_Client_ID)) {
            AD_Client_ID = vars.getClient();
        }

        final String cDocTypeID = (C_DocTypeTarget_ID.equals("") ? C_DocType_ID : C_DocTypeTarget_ID);
        if (cDocTypeID.equals("")) {
            return getDocumentNo(con, AD_Client_ID, TableName, updateNext);
        }

        if (AD_Client_ID.equals("0")) {
            throw new UnsupportedOperationException("Utility.getDocumentNo - Cannot add System records");
        }

        CSResponse cs = null;
        try {

            cs = DocumentNoData.nextDocTypeConnection(conn, con, cDocTypeID, AD_Client_ID,
                    (updateNext ? "Y" : "N"));
        } catch (final ServletException e) {
        }

        if (cs == null || cs.razon == null || cs.razon.equals("")) {
            if (!onlyDocType) {
                return getDocumentNoConnection(conn, con, AD_Client_ID, TableName, updateNext);
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
     *          Handler for the database connection.
     * @param AD_Client_ID
     *          String with the client id.
     * @param TableName
     *          Table name.
     * @param updateNext
     *          Save the new sequence in database.
     * @return String with the new document number.
     */
    @Override
    public String getDocumentNo(ConnectionProvider conn, String AD_Client_ID, String TableName,
                                       boolean updateNext) {
        if (TableName == null || TableName.length() == 0) {
            throw new IllegalArgumentException("Utility.getDocumentNo - required parameter missing");
        }

        CSResponse cs = null;
        try {
            cs = DocumentNoData.nextDoc(conn, "DocumentNo_" + TableName, AD_Client_ID,
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
     *          Handler for the database connection.
     * @param AD_Client_ID
     *          String with the client id.
     * @param TableName
     *          Table name.
     * @param updateNext
     *          Save the new sequence in database.
     * @return String with the new document number.
     */
    @Override
    public String getDocumentNoConnection(Connection conn, ConnectionProvider con,
                                                 String AD_Client_ID, String TableName, boolean updateNext) {
        if (TableName == null || TableName.length() == 0) {
            throw new IllegalArgumentException("Utility.getDocumentNo - required parameter missing");
        }

        CSResponse cs = null;
        try {
            cs = DocumentNoData.nextDocConnection(conn, con, "DocumentNo_" + TableName, AD_Client_ID,
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
