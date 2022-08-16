package com.etendoerp.legacy.advancedpaymentmngt.modulescript;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;

import org.apache.commons.lang.StringUtils;

import org.openbravo.utils.FileUtility;
import org.openbravo.utils.FormatUtilities;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

public class RenameSequenceFunctions extends ModuleScript {
    private static final Logger log4j = LogManager.getLogger();

    @Override
    public void execute() {
        log4j.info("Alter legacy functions");
        try {
            ConnectionProvider cp = getConnectionProvider();
            DatabaseMetaData metaData = cp.getConnection().getMetaData();
            if ( StringUtils.equals("Oracle", metaData.getDatabaseProductName())) {
                log4j.info("Detected Oracle DB");
                log4j.info("Execute DROP ");
                PreparedStatement ps = cp
                        .getPreparedStatement(
                          "declare\n" +
                          "   c int;\n" +
                          "begin\n" +
                          "       select count(*) into c from user_procedures where object_type = 'PROCEDURE' and object_name = 'AD_SEQUENCE_DOC';\n" +
                          "       if c = 1 then\n" +
                          "          execute immediate 'DROP PROCEDURE AD_SEQUENCE_DOC';\n" +
                          "       end if;\n" +
                          "       select count(*) into c from user_procedures where object_type = 'PROCEDURE' and object_name = 'AD_SEQUENCE_DOCTYPE';\n" +
                          "       if c = 1 then\n" +
                          "          execute immediate 'DROP PROCEDURE AD_SEQUENCE_DOCTYPE';\n" +
                          "       end if;\n" +
                          "       select count(*) into c from user_procedures where object_type = 'PROCEDURE' and object_name = 'AD_SEQUENCE_NEXT';\n" +
                          "       if c = 1 then\n" +
                          "          execute immediate 'DROP PROCEDURE AD_SEQUENCE_NEXT';\n" +
                          "       end if;\n" +
                          "       select count(*) into c from user_procedures where object_type = 'FUNCTION' and object_name = 'AD_SEQUENCE_NEXTNO';\n" +
                          "       if c = 1 then\n" +
                          "          execute immediate 'DROP FUNCTION AD_SEQUENCE_NEXTNO';\n" +
                          "       end if;\n" +
                          "end;\n");

                ps.executeUpdate();
                ps = cp
                  .getPreparedStatement(
                    "create or replace PROCEDURE AD_SEQUENCE_DOC(p_sequencename IN VARCHAR2, p_ad_client_id IN VARCHAR2, p_update_next IN CHAR, p_documentno OUT VARCHAR2)\n" +
                    "AS BEGIN\n" +
                    "  LEGAP_SEQUENCE_DOC(P_SEQUENCENAME, P_AD_CLIENT_ID, P_UPDATE_NEXT, P_DOCUMENTNO);\n" +
                    "END AD_SEQUENCE_DOC;");
                ps.executeUpdate();
                ps = cp
                        .getPreparedStatement(
                          "create or replace PROCEDURE AD_SEQUENCE_DOCTYPE(p_doctype_id IN VARCHAR2, p_id IN VARCHAR2, p_update_next IN CHAR, p_documentno OUT VARCHAR2) \n" +
                          "AS\n" +
                          "BEGIN\n" +
                          "  LEGAP_SEQUENCE_DOCTYPE(p_doctype_id, p_id, P_UPDATE_NEXT, P_DOCUMENTNO);\n" +
                          "END AD_SEQUENCE_DOCTYPE;\n");
                ps.executeUpdate();
                ps = cp
                        .getPreparedStatement("create or replace PROCEDURE AD_SEQUENCE_NEXT(p_tablename IN VARCHAR2, p_id IN VARCHAR2, p_nextno OUT VARCHAR2) \n" +
                          "AS\n" +
                          "BEGIN\n" +
                          "  LEGAP_SEQUENCE_NEXT(p_tablename, p_id, p_nextno);\n" +
                          "END AD_SEQUENCE_NEXT;");
                ps.executeUpdate();
                ps = cp
                        .getPreparedStatement(
                          "create or replace FUNCTION AD_SEQUENCE_NEXTNO(p_tablename IN VARCHAR2) RETURN VARCHAR2\n" +
                          "AS\n" +
                          "BEGIN\n" +
                          "  RETURN LEGAP_SEQUENCE_NEXTNO(p_tablename);\n" +
                          "END AD_SEQUENCE_NEXTNO\n" +
                          ";");
                ps.executeUpdate();
            } else {
                PreparedStatement ps = cp
                        .getPreparedStatement("DROP FUNCTION IF EXISTS AD_SEQUENCE_DOC; ALTER FUNCTION LEGAP_SEQUENCE_DOC RENAME TO AD_SEQUENCE_DOC;");
                ps.executeUpdate();
                ps = cp
                        .getPreparedStatement("DROP FUNCTION IF EXISTS AD_SEQUENCE_DOCTYPE; ALTER FUNCTION LEGAP_SEQUENCE_DOCTYPE RENAME TO AD_SEQUENCE_DOCTYPE;");
                ps.executeUpdate();
                ps = cp
                        .getPreparedStatement("DROP FUNCTION IF EXISTS AD_SEQUENCE_NEXT; ALTER FUNCTION LEGAP_SEQUENCE_NEXT RENAME TO AD_SEQUENCE_NEXT;");
                ps.executeUpdate();
                ps = cp
                        .getPreparedStatement("DROP FUNCTION IF EXISTS AD_SEQUENCE_NEXTNO; ALTER FUNCTION LEGAP_SEQUENCE_NEXTNO RENAME TO AD_SEQUENCE_NEXTNO;");
                ps.executeUpdate();
            }
        } catch (Exception e) {
            handleError(e);
        }
    }
}
