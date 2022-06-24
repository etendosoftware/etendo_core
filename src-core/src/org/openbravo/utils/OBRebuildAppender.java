/*
 ************************************************************************************
 * Copyright (C) 2001-2018 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.utils;

import java.io.File;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.openbravo.database.ConnectionProviderImpl;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.exception.PoolNotFoundException;

/* This class inserts the rebuild log into a table in the database.
 * This information is used in the rebuild window in Openbravo
 */
@Plugin(name = "OBRebuildAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class OBRebuildAppender extends AbstractAppender {

    private ConnectionProviderImpl cp;
    private Connection connection;
    private static String baseDir;

    @PluginFactory
    public static OBRebuildAppender createAppender(@PluginAttribute("name") String name,
                                                   @PluginElement("Filter") Filter filter,
                                                   @PluginElement("Layout") Layout<? extends Serializable> layout) {
        return new OBRebuildAppender(name, filter, layout);
    }

    protected OBRebuildAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout);
    }

    @Override
    public void append(LogEvent event) {
        if (event.getLevel().isLessSpecificThan(Level.DEBUG)) {
            return;
        }

        try {
            Connection con = getConnection();
            if (con == null) {
                return;
            }

            String message = event.getMessage().getFormattedMessage();
            if (message.length() > 3000) {
                message = message.substring(0, 2997) + "...";
            }
            try (PreparedStatement ps = connection.prepareStatement( //
                    "INSERT INTO \n" //
                            + "ad_error_log \n"
                            + "  (ad_error_log_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, system_status, error_level, message, line_number) \n"
                            + "SELECT get_uuid(), '0', '0', 'Y', now(), '0', now(), '0', system_status, ?, ?, (SELECT coalesce(max(line_number)+1,1) FROM AD_ERROR_LOG) "
                            + "  FROM ad_system_info")) {
                String level = event.getLevel().toString();

                ps.setString(1, level);
                ps.setString(2, message);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            // We will not log an error if the insertion in the log table
            // goes wrong for two different reasons:
            // - First, it could cause problems if the message itself is redirected to the log again
            // - Second, if the instance which is being rebuild doesn't yet have the log table, or the
            // table is being recreated, the insertion will fail, and this is ok.
            // We don't need to have log lines in the database in that case
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.commit();
                }
            } catch (SQLException e) {
            }
        }
    }

    private Connection getConnection()
            throws PoolNotFoundException, NoConnectionAvailableException, SQLException {
        if (cp == null) {
            File fProp = null;
            String userDir = System.getProperty("user.dir");
            if (baseDir != null) {
                fProp = new File(baseDir, "config/Openbravo.properties");
            } else if(new File(userDir, "config/Openbravo.properties").exists()){
                fProp = new File(userDir, "config/Openbravo.properties");
            } else if (new File("../../config/Openbravo.properties").exists()) {
                fProp = new File("../../config/Openbravo.properties");
            } else if (new File("../config/Openbravo.properties").exists()) {
                fProp = new File("../config/Openbravo.properties");
            } else if (new File("config/Openbravo.properties").exists()) {
                fProp = new File("config/Openbravo.properties");
            }
            cp = new ConnectionProviderImpl(fProp.getAbsolutePath());
        }
        if (connection == null || connection.isClosed()) {
            connection = cp.getConnection();
        }
        return connection;
    }

    /*
     * Sets the basedir (directory of the Openbravo sources, which needs to contain a
     * config/Openbravo.properties folder
     */
    public void setBasedir(String basedir) {
        baseDir = basedir;
    }

    /*
     * returns the basedir (directory of the Openbravo sources, which needs to contain a
     * config/Openbravo.properties folder
     */
    public String getBasedir() {
        return baseDir;
    }

}
