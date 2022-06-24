/*
 ************************************************************************************
 * Copyright (C) 2001-2019 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.base.secureApp;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.data.FieldProvider;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.exception.PoolNotFoundException;

@SuppressWarnings("serial")
class DefaultValuesData implements FieldProvider {
  public String columnname;
  static Logger log4j = LogManager.getLogger();

  @Override
  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("columnname")) {
      return columnname;
    } else {
      return null;
    }
  }

  /**
   * Select for relation
   */
  public static String select(ConnectionProvider connectionProvider, String selArg, String table,
      String clients, String orgs) throws ServletException {

    //@formatter:off
    String sql = 
            "select " + selArg + " as COLUMNNAME " +
            "  from " + table + " " +
            " where isActive = 'Y' " +
            "   and isDefault = 'Y' " +
            "   and AD_Client_ID in " + parseIds(clients) +
            "   and AD_Org_ID in " + parseIds(orgs) +
            " order by AD_Client_ID";
    //@formatter:on
    ResultSet result = null;
    String resultado = "";
    PreparedStatement st = null;
    try {
      st = connectionProvider.getPreparedStatement(sql);

      result = st.executeQuery();

      if (result.next()) {
        resultado = UtilSql.getValue(result, "COLUMNNAME");
      }
      result.close();
    } catch (SQLException e) {
      log4j.error("SQL error in query:{}", sql, e);
      throw new ServletException(
          "@CODE=" + e.getErrorCode() + "@" + e.getMessage());
    } catch (NoConnectionAvailableException ec) {
      log4j.error("Connection error in query:{}", sql, ec);
      throw new ServletException("@CODE=NoConnectionAvailable");
    } catch (PoolNotFoundException ep) {
      log4j.error("Pool error in query:{}", sql, ep);
      throw new ServletException("@CODE=NoConnectionAvailable");
    } catch (Exception ex) {
      log4j.error("Exception in query:{}", sql, ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        if (result != null) {
          result.close();
        }
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ex) {
        log4j.error("Error releasing prepared statement:{}", sql, ex);
      }
    }
    return (resultado);
  }

  /**
   * Parses a list of parameters in the form "'id1','id2','id3'" to a string formatted for sql IN
   * clause "('id1', 'id2', 'id3')"
   * 
   * @param parameters
   *          String with parameters formatted like so "'id1', 'id2', 'id3'"
   * @return Formatted parameter list for SQL IN clause: "('id1', 'id2', 'id3')"
   */
  private static String parseIds(String parameters) {
    return Arrays.stream(parameters.split(",")).collect(Collectors.joining(",", "(", ")"));
  }
}
