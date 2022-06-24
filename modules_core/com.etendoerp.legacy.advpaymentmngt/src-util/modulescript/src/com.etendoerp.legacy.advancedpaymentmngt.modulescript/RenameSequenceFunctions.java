package com.etendoerp.legacy.advancedpaymentmngt.modulescript;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.Connection;
import java.sql.PreparedStatement;

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
            PreparedStatement ps = cp
                    .getPreparedStatement("DROP FUNCTION IF EXISTS AD_SEQUENCE_DOC; ALTER FUNCTION LEGAP_SEQUENCE_DOC RENAME TO AD_SEQUENCE_DOC");
            ps.executeUpdate();
            ps = cp
                    .getPreparedStatement("DROP FUNCTION IF EXISTS AD_SEQUENCE_DOCTYPE; ALTER FUNCTION LEGAP_SEQUENCE_DOCTYPE RENAME TO AD_SEQUENCE_DOCTYPE");
            ps.executeUpdate();
            ps = cp
                    .getPreparedStatement("DROP FUNCTION IF EXISTS AD_SEQUENCE_NEXT; ALTER FUNCTION LEGAP_SEQUENCE_NEXT RENAME TO AD_SEQUENCE_NEXT");
            ps.executeUpdate();
            ps = cp
                    .getPreparedStatement("DROP FUNCTION IF EXISTS AD_SEQUENCE_NEXTNO; ALTER FUNCTION LEGAP_SEQUENCE_NEXTNO RENAME TO AD_SEQUENCE_NEXTNO");
            ps.executeUpdate();
        } catch (Exception e) {
            handleError(e);
        }
    }
}