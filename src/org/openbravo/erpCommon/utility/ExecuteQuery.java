/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2001-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.data.FieldProvider;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;

/**
 * @author Fernando Iriazabal
 * 
 *         Implements the needed methods to execute the differents kinds of queries in the database.
 */
public class ExecuteQuery {

  private static final Logger log4j = LogManager.getLogger();
  private ConnectionProvider pool;
  private Vector<String> parameters = new Vector<String>();
  private String sql;

  /**
   * Constructor
   */
  public ExecuteQuery() {
  }

  /**
   * Constructor
   * 
   * @param _conn
   *          Handler for the database connection.
   * @param _sql
   *          String with the query.
   * @param _parameters
   *          Vector with the query's parameters.
   * @throws Exception
   */
  public ExecuteQuery(ConnectionProvider _conn, String _sql, Vector<String> _parameters)
      throws Exception {
    setPool(_conn);
    setSQL(_sql);
    setParameters(_parameters);
  }

  /**
   * Setter for the database connection handler.
   * 
   * @param _conn
   *          Object handler for the database connection.
   * @throws Exception
   */
  private void setPool(ConnectionProvider _conn) throws Exception {
    if (_conn == null) {
      throw new Exception("The pool is null");
    }
    this.pool = _conn;
  }

  /**
   * Getter for the database connection handler.
   * 
   * @return Object with the database connection handler.
   */
  private ConnectionProvider getPool() {
    return this.pool;
  }

  /**
   * Setter for the query.
   * 
   * @param _sql
   *          String with the query.
   * @throws Exception
   */
  private void setSQL(String _sql) throws Exception {
    this.sql = ((_sql == null) ? "" : _sql);
  }

  /**
   * Getter for the query.
   * 
   * @return String with the query.
   */
  private String getSQL() {
    return this.sql;
  }

  /**
   * Setter for the query parameters.
   * 
   * @param _parameters
   *          Vector with the parameters.
   * @throws Exception
   */
  private void setParameters(Vector<String> _parameters) throws Exception {
    this.parameters = _parameters;
  }

  /**
   * Getter for the query parameters.
   * 
   * @return Vector with the parameters.
   */
  private Vector<String> getParameters() {
    return this.parameters;
  }

  /**
   * Returns the selected parameter.
   * 
   * @param position
   *          Position of the selected parameter.
   * @return String with the parameter.
   */
  private String getParameter(int position) {
    if (this.parameters == null || this.parameters.size() < position) {
      return "";
    } else {
      return this.parameters.elementAt(position);
    }
  }

  /**
   * Executes the query.
   * 
   * @return Array of FieldProviders with the result of the query.
   * @throws ServletException
   */
  public FieldProvider[] select() throws ServletException {
    PreparedStatement st = null;
    ResultSet result;
    Vector<SQLReturnObject> vector = new Vector<SQLReturnObject>(0);

    String strSQL = getSQL();
    if (log4j.isDebugEnabled()) {
      log4j.debug("SQL: " + strSQL);
    }

    try {
      st = getPool().getPreparedStatement(strSQL);
      Vector<String> params = getParameters();
      if (params != null) {
        for (int iParameter = 0; iParameter < params.size(); iParameter++) {
          if (log4j.isDebugEnabled()) {
            log4j.debug("PARAMETER " + iParameter + ":" + getParameter(iParameter));
          }
          UtilSql.setValue(st, iParameter + 1, 12, null, getParameter(iParameter));
        }
      }
      result = st.executeQuery();

      boolean first = true;
      int numColumns = 0;
      Vector<String> names = new Vector<String>(0);
      while (result.next()) {
        if (first) {
          ResultSetMetaData rmeta = result.getMetaData();
          numColumns = rmeta.getColumnCount();
          for (int i = 1; i <= numColumns; i++) {
            names.addElement(rmeta.getColumnName(i));
          }
          first = false;
        }
        SQLReturnObject sqlReturnObject = new SQLReturnObject();
        for (int i = 0; i < numColumns; i++) {
          sqlReturnObject.setData(names.elementAt(i), UtilSql.getValue(result, names.elementAt(i)));
        }
        vector.addElement(sqlReturnObject);
      }
      result.close();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSQL.toString() + "Exception:" + e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage(), e);
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSQL.toString() + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        getPool().releasePreparedStatement(st);
      } catch (Exception e) {
        log4j.error("Error during release*Statement of query: " + strSQL, e);
      }
    }
    FieldProvider objectListData[] = new FieldProvider[vector.size()];
    vector.copyInto(objectListData);
    return (objectListData);
  }

}
