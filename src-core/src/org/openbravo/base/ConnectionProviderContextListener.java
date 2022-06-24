/*
 ************************************************************************************
 * Copyright (C) 2001-2015 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */

package org.openbravo.base;

import java.io.FileInputStream;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.ConnectionProviderImpl;
import org.openbravo.database.JNDIConnectionProvider;
import org.openbravo.exception.PoolNotFoundException;

/**
 * Manage the creation and destruction of the db connection pool..
 * 
 * @author Ben Sommerville
 */
public class ConnectionProviderContextListener implements ServletContextListener {
  public static final String POOL_ATTRIBUTE = "openbravoPool";
  private static Logger log4j = LogManager.getLogger();
  private static ConnectionProvider pool;

  @Override
  public void contextInitialized(ServletContextEvent event) {
    ServletContext context = event.getServletContext();
    ConfigParameters configParameters = ConfigParameters.retrieveFrom(context);

    try {
      pool = createPool(configParameters);
      context.setAttribute(POOL_ATTRIBUTE, pool);
    } catch (PoolNotFoundException e) {
      log4j.error("Unable to create a connection pool", e);
    }

  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    ServletContext context = event.getServletContext();
    destroyPool(getPool(context));
    context.removeAttribute(POOL_ATTRIBUTE);
  }

  public static ConnectionProvider getPool(ServletContext context) {
    return (ConnectionProvider) context.getAttribute(POOL_ATTRIBUTE);
  }

  public static ConnectionProvider getPool() {
    return pool;
  }

  public static void reloadPool(ServletContext context) throws Exception {
    ConnectionProvider connectionPool = getPool(context);
    if (connectionPool instanceof ConnectionProviderImpl) {
      ConfigParameters configParameters = ConfigParameters.retrieveFrom(context);
      String strPoolFile = configParameters.getPoolFilePath();
      boolean isRelative = !strPoolFile.startsWith("/") && !strPoolFile.substring(1, 1).equals(":");
      ((ConnectionProviderImpl) connectionPool).reload(strPoolFile, isRelative,
          configParameters.strContext);
      ;
    }
  }

  private ConnectionProvider createPool(ConfigParameters configParameters)
      throws PoolNotFoundException {
    return createXmlPool(configParameters);
  }

  private static ConnectionProvider createXmlPool(ConfigParameters configParameters)
      throws PoolNotFoundException {
    try {
      String strPoolFile = configParameters.getPoolFilePath();
      boolean isRelative = !strPoolFile.startsWith("/") && !strPoolFile.substring(1, 1).equals(":");

      if (useJNDIConnProvider(strPoolFile)) {
        return new JNDIConnectionProvider(strPoolFile, isRelative);
      } else {
        return new ConnectionProviderImpl(strPoolFile, isRelative, configParameters.strContext);
      }

    } catch (Exception ex) {
      throw new PoolNotFoundException(ex.getMessage(), ex);
    }
  }

  private static boolean useJNDIConnProvider(String strPoolFile) {
    Properties properties = new Properties();
    String jndiUsage = null;
    try {
      properties.load(new FileInputStream(strPoolFile));
      String externalPool = properties.getProperty("db.externalPoolClassName");
      if (externalPool != null && !"".equals(externalPool)) {
        // external pools should handle jndi datasources
        return false;
      }
      jndiUsage = properties.getProperty("JNDI.usage");
    } catch (Exception e) {
      log4j.error("Error checking JNDI mode file:" + strPoolFile, e);
    }
    return ("yes".equals(jndiUsage) ? true : false);
  }

  private static void destroyPool(ConnectionProvider connectionPool) {
    if (connectionPool != null && connectionPool instanceof JNDIConnectionProvider) {
      try {
        ((JNDIConnectionProvider) connectionPool).destroy();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else if (connectionPool != null && connectionPool instanceof ConnectionProvider) {
      try {
        (connectionPool).destroy();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

}
