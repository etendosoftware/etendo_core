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
 * All portions are Copyright (C) 2014-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.db;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.query.Query;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.provider.OBSingleton;
import org.openbravo.base.session.OBPropertiesProvider;

/**
 * Utility class with that allows to set a query timeout for hibernate queries, hibernate criterias
 * and sql statements.
 * 
 * @author AugustoMauch
 */

public class QueryTimeOutUtil implements OBSingleton {

  private static Logger log = LogManager.getLogger();

  private static QueryTimeOutUtil instance;

  // Prefix of the query timeout property in Openbravo.properties
  private static final String QUERY_TIMEOUT_PREFIX_PROPERTY_NAME = "db.queryTimeout.";

  // Maps query type with query timeout
  private Map<String, Integer> queryTimeOutMap;

  // Specifies if a time out can be applied. It will be false if:
  // * either the jdbc driver or the database do not support to set the timeout of a statement or
  // * if there are no timeouts defined in Openbravo.properties
  boolean canApplyTimeOut = true;

  /**
   * @return the singleton instance of the QueryTimeOutUtil class
   */
  public static synchronized QueryTimeOutUtil getInstance() {
    if (instance == null) {
      instance = OBProvider.getInstance().get(QueryTimeOutUtil.class);
    }
    return instance;
  }

  public QueryTimeOutUtil() {
    loadQueryTimeOutMap();
    testQueryTimeOut();
  }

  /**
   * Loads all the query timeouts from Openbravo.properties. If there are not query timeouts defined
   * canApplyTimeOut is set to false
   */
  private void loadQueryTimeOutMap() {
    queryTimeOutMap = new HashMap<>();
    final Properties obProperties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    Iterator<Object> keys = obProperties.keySet().iterator();
    while (keys.hasNext()) {
      String propName = (String) keys.next();
      if (propName.startsWith(QUERY_TIMEOUT_PREFIX_PROPERTY_NAME)) {
        String value = null;
        try {
          value = obProperties.getProperty(propName);
          int intValue = Integer.parseInt(value);
          queryTimeOutMap.put(propName.substring(QUERY_TIMEOUT_PREFIX_PROPERTY_NAME.length()),
              intValue);
        } catch (NumberFormatException e) {
          log.error("Invalid value for " + propName + " property: " + value, e);
        }
      }

    }
    if (queryTimeOutMap.isEmpty()) {
      canApplyTimeOut = false;
    }
  }

  /**
   * Checks if it is supported to set the query timeout of a statement. If an exception is thrown
   * canApplyTimeOut is set to false
   */
  private void testQueryTimeOut() {
    if (canApplyTimeOut) {
      String sql = "SELECT * FROM C_UOM";
      Statement statement = null;
      try {
        statement = new DalConnectionProvider(false).getPreparedStatement(sql);
        statement.setQueryTimeout(1);
      } catch (Exception e) {
        canApplyTimeOut = false;
      } finally {
        try {
          if (statement != null) {
            statement.close();
          }
        } catch (SQLException e) {
          log.warn("Statement.setQueryTimeout method not supported", e);
        }
      }
    }
  }

  /**
   * Sets a timeout for a hibernate query, if possible
   * 
   * @param query
   * @param type
   *          query type, it will be used to fetch the proper timeout
   * 
   */
  public void setQueryTimeOut(Query<?> query, String type) {
    if (canApplyTimeOut && checkQueryType(type)) {
      query.setTimeout(queryTimeOutMap.get(type));
    }
  }

  /**
   * Sets a timeout for a hibernate criteria (i.e. OBCriteria), if possible
   * 
   * @param criteria
   * @param type
   *          query type, it will be used to fetch the proper timeout
   */
  public void setQueryTimeOut(Criteria criteria, String type) {
    if (canApplyTimeOut && checkQueryType(type)) {
      criteria.setTimeout(queryTimeOutMap.get(type));
    }
  }

  /**
   * Sets a timeout for a sql statement, if possible
   * 
   * @param type
   *          query type, it will be used to fetch the proper timeout
   */
  public void setQueryTimeOut(Statement statement, String type) {
    if (canApplyTimeOut && checkQueryType(type)) {
      try {
        statement.setQueryTimeout(queryTimeOutMap.get(type));
      } catch (SQLException e) {
        log.warn("setQueryTimeOut could not be executed", e);
      }
    }
  }

  /**
   * Checks if the provided query type is included in the queryTimeOutMap
   * 
   * @param type
   * @return true if the provided query type is a key of the queryTimeOutMap map
   */
  private boolean checkQueryType(String type) {
    if (queryTimeOutMap.containsKey(type)) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Sets the 0 the timeout of a hibernate query
   */
  public static void resetQueryTimeOut(Query<?> query) {
    query.setTimeout(0);
  }

  /**
   * Sets the 0 the timeout of a hibernate criteria
   */
  public static void resetQueryTimeOut(Criteria criteria) {
    criteria.setTimeout(0);
  }

  /**
   * Sets the 0 the timeout of a sql statement
   */
  public static void resetQueryTimeOut(Statement statement) {
    try {
      statement.setQueryTimeout(0);
    } catch (SQLException e) {
      log.warn("resetQueryTimeOut could not be executed", e);
    }
  }
}
