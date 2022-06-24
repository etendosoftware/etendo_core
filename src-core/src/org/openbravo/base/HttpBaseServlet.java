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
package org.openbravo.base;

import java.io.File;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.ConnectionProviderImpl;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.exception.PoolNotFoundException;
import org.openbravo.xmlEngine.XmlEngine;
import org.xml.sax.XMLReader;

/**
 * This class is intended to be extended by the HttpSecureAppServlet and provides methods for basic
 * management of request/response, database connections, transactions, FOP rendering, and others
 * that do not require authentication. It is loaded upon startup of the application server.
 */
public class HttpBaseServlet extends HttpServlet implements ConnectionProvider {
  private static final long serialVersionUID = 1L;
  protected ConnectionProvider myPool;
  public static String strDireccion;
  protected String strReplaceWith;
  protected String strReplaceWithFull;
  protected String strDefaultServlet;
  public XmlEngine xmlEngine = null;
  private static String strContext = null;
  private static String prefix = null;
  protected Logger log4j = LogManager.getLogger(this.getClass());

  protected ConfigParameters globalParameters;

  /**
   * Loads basic configuration settings that this class and all that extend it require to function
   * properly. Also instantiates XmlEngine object. This method is called upon load of the class,
   * which is configured to be loaded upon start of the application server. See also web.xml
   * (load-on-startup).
   */
  @Override
  public void init(ServletConfig config) {
    try {
      super.init(config);
      if (prefix == null) {
        prefix = config.getServletContext().getRealPath("/");
        if (prefix == null || prefix.equals("")) {
          // deployment in weblogic through ear file does not deploy
          // phisically
          // the files,
          // so we need to obtain the path through getClass method
          java.net.URL url = this.getClass().getResource("/");
          String mSchemaPath = url.getFile();
          if (mSchemaPath != null && !mSchemaPath.equals("")) {
            String separator = "/";
            int lastSlash = mSchemaPath.lastIndexOf(separator);
            if (lastSlash == -1) {
              separator = "\\";
              lastSlash = mSchemaPath.lastIndexOf(separator);
            }
            prefix = mSchemaPath.substring(0, lastSlash);
            prefix = prefix.substring(0, prefix.lastIndexOf(separator));
            prefix = prefix.substring(0, prefix.lastIndexOf(separator) + 1);
            // lastSlash = mSchemaPath.lastIndexOf(separator);
            // mSchemaPath = mSchemaPath.substring(0,
            // lastSlash);
            // lastSlash = mSchemaPath.lastIndexOf(separator);
            // prefix = mSchemaPath.substring(0, lastSlash+1);
          }
        }
        if (log4j.isDebugEnabled()) {
          log4j.debug("************************prefix: " + prefix);
        }
        if (strContext == null || strContext.equals("")) {
          String path = "/";
          int secondPath = -1;
          int firstPath = prefix.lastIndexOf(path);
          if (firstPath == -1) {
            path = "\\";
            firstPath = prefix.lastIndexOf(path);
          }
          if (firstPath != -1) {
            secondPath = prefix.lastIndexOf(path, firstPath - 1);
            strContext = prefix.substring(secondPath + 1, firstPath);
          }
        }
        if (log4j.isDebugEnabled()) {
          log4j.debug("context: " + strContext);
        }

      }

      globalParameters = ConfigParameters.retrieveFrom(config.getServletContext());
      strDefaultServlet = globalParameters.strDefaultServlet;

      if (myPool == null) {
        try {
          makeConnection(config);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }

      xmlEngine = new XmlEngine(this.myPool);
      xmlEngine.fileBaseLocation = new File(globalParameters.getBaseDesignPath());
      xmlEngine.strReplaceWhat = globalParameters.strReplaceWhat;
      xmlEngine.strReplaceWith = globalParameters.strLocalReplaceWith;
      log4j.debug("Replace attribute value: \"" + xmlEngine.strReplaceWhat + "\" with: \""
          + xmlEngine.strReplaceWith + "\".");
      XmlEngine.strTextDividedByZero = globalParameters.strTextDividedByZero;
      xmlEngine.fileXmlEngineFormat = new File(globalParameters.getXmlEngineFileFormatPath());
      xmlEngine.initialize();

      log4j.debug("Text of divided by zero: " + XmlEngine.strTextDividedByZero);

    } catch (ServletException e) {
      e.printStackTrace();
    }
  }

  /**
   * Initialization of basic servlet variables required by subsequent operations of this class and
   * the ones that extend it. Normally called within the service() method of this class.
   * 
   * @param request
   *          HttpServletRequest object where details of the HTTP request are.
   * @param response
   *          HttpServletResponse object where the response will be written and returned to the
   *          user.
   * @throws IOException
   * @throws ServletException
   */
  public void initialize(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    strDireccion = HttpBaseUtils.getLocalAddress(request);
    String strActualUrl = HttpBaseUtils.getLocalHostAddress(request);
    if (log4j.isDebugEnabled()) {
      log4j.debug("Server name: " + strActualUrl);
    }
    HttpSession session = request.getSession(true);
    String strLanguage = "";
    try {
      strLanguage = (String) session.getAttribute("#AD_LANGUAGE");
      if (strLanguage == null || strLanguage.trim().equals("")) {
        strLanguage = "";
      }
    } catch (Exception e) {
      strLanguage = "";
    }
    xmlEngine.fileBaseLocation = new File(getBaseDesignPath(strLanguage));
    xmlEngine.sessionLanguage = strLanguage;
    strReplaceWith = globalParameters.strLocalReplaceWith.replace("@actual_url@", strActualUrl)
        .replace("@actual_url_context@", strDireccion);
    strReplaceWithFull = strReplaceWith;
    strReplaceWith = HttpBaseUtils.getRelativeUrl(request, strReplaceWith);
    if (log4j.isDebugEnabled()) {
      log4j.debug("xmlEngine.strReplaceWith: " + strReplaceWith);
    }
    xmlEngine.strReplaceWith = strReplaceWith;

  }

  /**
   * Called by the HttpSecureAppServlet within its service() method to indirectly call the service()
   * method of the HttpServlet base class because HttpBaseServlet.service() is then replaced by the
   * HttpSecureAppServlets one.
   * 
   * @param request
   *          HttpServletRequest object where details of the HTTP request are.
   * @param response
   *          HttpServletResponse object where the response will be written and returned to the
   *          user.
   * @throws IOException
   * @throws ServletException
   */
  public void serviceInitialized(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    super.service(request, response);
  }

  /**
   * A dispatcher method that calls the initialization upon every request to the servlet before it
   * hands over the final dispatchment to the HttpServlet base class.
   * 
   * @param request
   *          HttpServletRequest object where details of the HTTP request are.
   * @param response
   *          HttpServletResponse object where the response will be written and returned to the
   *          user.
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void service(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    initialize(request, response);
    if (log4j.isDebugEnabled()) {
      log4j.debug("Call to HttpServlet.service");
    }
    super.service(request, response);
  }

  /**
   * Returns the absolute path to the correct language subfolder within the context's src-loc
   * folder.
   * 
   * @param language
   *          String specifying the language folder required, e.g. es_ES
   * @return String with the absolute path on the local drive.
   */
  protected String getBaseDesignPath(String language) {
    if (log4j.isDebugEnabled()) {
      log4j.debug("*********************Base path: " + globalParameters.strBaseDesignPath);
    }
    String strNewAddBase = globalParameters.strDefaultDesignPath;
    String strFinal = globalParameters.strBaseDesignPath;
    // if (!language.equals("") && !language.equals("en_US")) {
    // strNewAddBase = language;
    // }
    if (!strFinal.endsWith("/" + strNewAddBase)) {
      strFinal += "/" + strNewAddBase;
    }

    if (!globalParameters.prefix.endsWith("/")) {
      strFinal = "/" + strFinal;
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("*********************Base path: " + strFinal);
    }
    return globalParameters.prefix + strFinal;
  }

  /**
   * Redirects all HTTP GET requests to be handled by the doPost method of the extending class.
   * 
   * @param request
   *          HttpServletRequest object where details of the HTTP request are.
   * @param response
   *          HttpServletResponse object where the response will be written and returned to the
   *          user.
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doPost(request, response);
  }

  public void doGetCall(HttpServletRequest request, HttpServletResponse response) throws Exception {
    doPostCall(request, response);
  }

  public void doPostCall(HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    return;
  }

  /**
   * Retrieves an open autocommit connection from the connection pool managed by this class.
   * 
   * @return A Connection object containing the open connection.
   * @throws NoConnectionAvailableException
   */
  @Override
  public Connection getConnection() throws NoConnectionAvailableException {
    return (myPool.getConnection());
  }

  /**
   * Return the bbdd.rdbms property defined within the config/Openbravo.properties configuration
   * file. This property defines the type of the database (ORACLE or POSTGRES).
   * 
   * @return String containing the database type (ORACLE or POSTGRES).
   */
  @Override
  public String getRDBMS() {
    return (myPool.getRDBMS());
  }

  /**
   * Retrieves an open connection that is not automatically commited from the connection pool
   * managed by this class.
   * 
   * @return A Connection object containing the open connection
   * @throws NoConnectionAvailableException
   * @throws SQLException
   */
  @Override
  public Connection getTransactionConnection() throws NoConnectionAvailableException, SQLException {
    return myPool.getTransactionConnection();
  }

  /**
   * First commit the connection specified and then store it back into the pool of available
   * connections managed by this class.
   * 
   * @param conn
   *          The Connection object required to be committed and stored back into the pool.
   * @throws SQLException
   */
  @Override
  public void releaseCommitConnection(Connection conn) throws SQLException {
    myPool.releaseCommitConnection(conn);
  }

  /**
   * First rollback the connection specified and then store it back into the pool of available
   * connections managed by this class.
   * 
   * @param conn
   *          The Connection object required to be rolled back and stored back into the pool.
   * @throws SQLException
   */
  @Override
  public void releaseRollbackConnection(Connection conn) throws SQLException {
    myPool.releaseRollbackConnection(conn);
  }

  /**
   * Returns a PreparedStatement object that contains the specified strSql prepared on top of a
   * connection retrieved from the poolName pool of connections.
   * 
   * @param poolName
   *          The name of the pool to retrieve the connection from.
   * @param strSql
   *          The SQL statement to prepare.
   * @return PreparedStatement object with the strSql prepared.
   * @throws Exception
   */
  @Override
  public PreparedStatement getPreparedStatement(String poolName, String strSql) throws Exception {
    return (myPool.getPreparedStatement(poolName, strSql));
  }

  /**
   * Returns a PreparedStatement object that contains the specified strSql prepared on top of a
   * connection retrieved from a default connection pool.
   * 
   * @param strSql
   *          The SQL statement to prepare.
   * @return PreparedStatement object with the strSql prepared.
   * @throws Exception
   */
  @Override
  public PreparedStatement getPreparedStatement(String strSql) throws Exception {
    return (myPool.getPreparedStatement(strSql));
  }

  /**
   * Returns a PreparedStatement object that contains the specified strSql prepared on top of the
   * connection conn passed to the method.
   * 
   * @param conn
   *          The Connection object containing the connection.
   * @param strSql
   *          The SQL statement to prepare.
   * @return PreparedStatement object with the strSql prepared.
   * @throws SQLException
   */
  @Override
  public PreparedStatement getPreparedStatement(Connection conn, String strSql)
      throws SQLException {
    return (myPool.getPreparedStatement(conn, strSql));
  }

  /**
   * Closes the preparedStatement and releases the connection on top of which this statement was
   * prepared.
   * 
   * @param preparedStatement
   *          Object containing prepared statement to release.
   * @throws SQLException
   */
  @Override
  public void releasePreparedStatement(PreparedStatement preparedStatement) throws SQLException {
    try {
      myPool.releasePreparedStatement(preparedStatement);
    } catch (Exception ex) {
    }
  }

  /**
   * Returns a Statement object for sending SQL statements to the database based on a connection
   * retrieved from the poolName.
   * 
   * @param poolName
   *          The name of the pool to retrieve the connection from.
   * @return Prepared Statement object requested.
   * @throws Exception
   */
  @Override
  public Statement getStatement(String poolName) throws Exception {
    return (myPool.getStatement(poolName));
  }

  /**
   * Returns a Statement object for sending SQL statements to the database based on a connection
   * retrieved from the default pool of connections.
   * 
   * @return Prepared Statement object requested.
   * @throws Exception
   */
  @Override
  public Statement getStatement() throws Exception {
    return (myPool.getStatement());
  }

  /**
   * Returns a Statement object for sending SQL statements to the database based on the connection
   * conn provided.
   * 
   * @return Prepared Statement object requested.
   * @throws SQLException
   */
  @Override
  public Statement getStatement(Connection conn) throws SQLException {
    return (myPool.getStatement(conn));
  }

  /**
   * Closes the statement and releases the connection back into the pool.
   * 
   * @param statement
   *          Object containing the statement to release.
   * @throws SQLException
   */
  @Override
  public void releaseStatement(Statement statement) throws SQLException {
    myPool.releaseStatement(statement);
  }

  /**
   * Only closes the statement since it is probably part of a series of statements that use the same
   * connection. The connection must be then closed manually after all statements have been closed.
   * 
   * @param statement
   *          Object containing the statement to release.
   * @throws SQLException
   */
  @Override
  public void releaseTransactionalStatement(Statement statement) throws SQLException {
    myPool.releaseTransactionalStatement(statement);
  }

  /**
   * Only closes the preparedStatement since it is probably part of a series of statements that use
   * the same connection. The connection must be then closed manually after all statements have been
   * closed.
   * 
   * @param preparedStatement
   *          Object containing the prepared statement to release.
   * @throws SQLException
   */
  @Override
  public void releaseTransactionalPreparedStatement(PreparedStatement preparedStatement)
      throws SQLException {
    myPool.releaseTransactionalPreparedStatement(preparedStatement);
  }

  /**
   * Returns a prepared callable statement for the specified strSql based on the connection
   * retrieved from the poolName.
   * 
   * @param poolName
   *          The name of the pool to retrieve the connection from.
   * @param strSql
   *          The callable SQL statement to prepare.
   * @return CallableStatement object with the strSql prepared.
   * @throws SQLException
   */
  @Override
  public CallableStatement getCallableStatement(String poolName, String strSql) throws Exception {
    return (myPool.getCallableStatement(poolName, strSql));
  }

  /**
   * Returns a prepared callable statement for the specified strSql based on the connection
   * retrieved from the default pool of connections.
   * 
   * @param strSql
   *          The callable SQL statement to prepare.
   * @return CallableStatement object with the strSql prepared.
   * @throws Exception
   */
  @Override
  public CallableStatement getCallableStatement(String strSql) throws Exception {
    return (myPool.getCallableStatement(strSql));
  }

  /**
   * Returns a prepared callable statement for the specified strSql based on the connection conn
   * provided.
   * 
   * @param conn
   *          The Connection object containing the connection.
   * @param strSql
   *          The callable SQL statement to prepare.
   * @return CallableStatement object with the strSql prepared.
   * @throws SQLException
   */
  @Override
  public CallableStatement getCallableStatement(Connection conn, String strSql)
      throws SQLException {
    return (myPool.getCallableStatement(conn, strSql));
  }

  /**
   * Closes the prepared callableStatement and releases the connection on top of which this callable
   * statement was prepared.
   * 
   * @param callableStatement
   *          Object containing prepared callable statement to release.
   * @throws SQLException
   */
  @Override
  public void releaseCallableStatement(CallableStatement callableStatement) throws SQLException {
    myPool.releaseCallableStatement(callableStatement);
  }

  /**
   * Not implemented yet.
   * 
   * @return String with "Status unavailable" or "Not implemented yet"
   */
  protected String getPoolStatus() {
    if (myPool instanceof ConnectionProviderImpl) {
      return ((ConnectionProviderImpl) myPool).getStatus();
    } else {
      return "Status unavailable";
    }

  }

  /**
   * Returns an instance of the xerces XML parser.
   * 
   * @return XMLReader object with the parser instance.
   * @throws ServletException
   */
  static XMLReader createParser() throws ServletException {
    String parserClassName = System.getProperty("org.xml.sax.parser");
    if (parserClassName == null) {
      parserClassName = "org.apache.xerces.parsers.SAXParser";
    }
    try {
      return (XMLReader) Class.forName(parserClassName).getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private void makeConnection(ServletConfig config) throws PoolNotFoundException {
    if (myPool != null) {
      try {
        myPool.destroy();
      } catch (Exception ignored) {
      }
      myPool = null;
    }

    myPool = ConnectionProviderContextListener.getPool(config.getServletContext());
  }

  @Override
  public String getStatus() {
    return myPool.getStatus();
  }

  @Override
  public String getServletInfo() {
    return "This servlet adds some functions (connection to the database, xmlEngine, logging) over HttpServlet";
  }
}
