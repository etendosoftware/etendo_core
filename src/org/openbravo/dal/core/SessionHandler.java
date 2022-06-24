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
 * All portions are Copyright (C) 2008-2021 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.dal.core;

import static org.openbravo.database.ExternalConnectionPool.DEFAULT_POOL;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.provider.OBNotSingleton;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.Identifiable;
import org.openbravo.base.util.Check;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ExternalConnectionPool;
import org.openbravo.service.db.DbUtility;

/**
 * Keeps the Hibernate Session and Transaction in a ThreadLocal so that it is available throughout
 * the application. This class provides convenience methods to get a Session and to
 * create/commit/rollback a transaction.
 * 
 * @author mtaal
 */
// TODO: revisit when looking at factory pattern and dependency injection
// framework
public class SessionHandler implements OBNotSingleton {
  private static final Logger log = LogManager.getLogger();

  private static ExternalConnectionPool externalConnectionPool;

  {
    String poolClassName = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("db.externalPoolClassName");
    if (poolClassName != null && !"".equals(poolClassName)) {
      try {
        externalConnectionPool = ExternalConnectionPool.getInstance(poolClassName);
      } catch (Throwable e) {
        externalConnectionPool = null;
        if (!log.isDebugEnabled()) {
          log.info("Could not load external DB pool [" + poolClassName + "]. Using old pool.");
        } else {
          log.warn("External connection pool class not found: " + poolClassName, e);
        }
      }
    }
  }

  // The threadlocal which handles the session
  private static ThreadLocal<SessionHandler> sessionHandler = new ThreadLocal<SessionHandler>();
  private static ThreadLocal<Boolean> checkingSessionDirty = new ThreadLocal<Boolean>();

  /**
   * Removes the current SessionHandler from the ThreadLocal. A call to getInstance will create a
   * new SessionHandler. A call to getSession will create a session and a transaction.
   */
  public static void deleteSessionHandler() {
    log.debug("Removing sessionhandler");
    sessionHandler.set(null);
  }

  /**
   * Checks whether a session handler is present for this thread and also available for the default
   * pool.
   * 
   * @return true if a session handler is present for this thread and available for the default
   *         pool, false otherwise
   */
  public static boolean isSessionHandlerPresent() {
    return isSessionHandlerPresent(DEFAULT_POOL);
  }

  /**
   * Checks whether a session handler is present for this thread and also available for the pool
   * whose name is passed as parameter.
   * 
   * @param pool
   *          the name of the pool
   * 
   * @return true if a session handler is present for this thread and available for the specified
   *         pool, false otherwise
   */
  public static boolean isSessionHandlerPresent(String pool) {
    return sessionHandler.get() != null && sessionHandler.get().isAvailablePool(pool);
  }

  /**
   * Checks if the session handler has available sessions which are not closed yet.
   * 
   * @return true if there are sessions not closed, false otherwise
   */
  public static boolean existsOpenedSessions() {
    return sessionHandler.get() != null && sessionHandler.get().hasAvailablePools();
  }

  /**
   * Returns the SessionHandler of this thread. If there is none then a new one is created.
   * 
   * @return the sessionhandler for this thread
   */
  public static SessionHandler getInstance() {
    SessionHandler sh = sessionHandler.get();
    if (sh == null) {
      log.debug("Creating sessionHandler");
      sh = getCreateSessionHandler();
      sessionHandler.set(sh);
    }
    return sh;
  }

  private static SessionHandler getCreateSessionHandler() {
    if (!OBProvider.getInstance().isRegistered(SessionHandler.class)) {
      OBProvider.getInstance().register(SessionHandler.class, SessionHandler.class, false);
    }
    return OBProvider.getInstance().get(SessionHandler.class);
  }

  private Map<String, Session> sessions = new HashMap<>();
  private Map<String, Transaction> trxs = new HashMap<>();
  private Map<String, Connection> connections = new HashMap<>();
  private Set<String> availablePools = new HashSet<String>();

  // Sets the session handler at rollback so that the controller can rollback
  // at the end
  private boolean doRollback = false;

  /** @return the session */
  public Session getSession() {
    return getSession(DEFAULT_POOL);
  }

  /**
   * Gets a {@code Session} from the connection pool whose name is passed as parameter. If it was
   * not created previously, this methods returns a newly created session from that pool.
   * 
   * @param pool
   *          the name of the pool used to retrieve the session
   * @return the session
   */
  public Session getSession(String pool) {
    String thePool = pool;
    if (thePool == null) {
      thePool = DEFAULT_POOL;
    }
    Session theSession = sessions.get(thePool);
    if (theSession == null) {
      begin(thePool);
    }
    return sessions.get(thePool);
  }

  /**
   * Checks whether current session is dirty (there are remaining changes to be sent to DB). Note
   * {@link Session#isDirty()} should not be directly invoked because it triggers Entity Persistence
   * Observers to be executed for modified entities. This method handles it so that they are not
   * called.
   */
  public boolean isSessionDirty(String pool) {
    try {
      checkingSessionDirty.set(true);
      return getSession(pool).isDirty();
    } finally {
      checkingSessionDirty.set(false);
    }
  }

  /**
   * Checks dirtiness for default session.
   * 
   * @see SessionHandler#isSessionDirty()
   */
  public boolean isSessionDirty() {
    return isSessionDirty(DEFAULT_POOL);
  }

  /** Returns true when the session is in process of checking for dirtiness. */
  static boolean isCheckingDirtySession() {
    return Boolean.TRUE.equals(checkingSessionDirty.get());
  }

  protected void setSession(Session thisSession) {
    setSession(DEFAULT_POOL, thisSession);
  }

  private void setSession(String pool, Session thisSession) {
    sessions.put(pool, thisSession);
  }

  protected Session createSession() {
    return createSession(DEFAULT_POOL);
  }

  private Session createSession(String pool) {
    SessionFactory sf = SessionFactoryController.getInstance().getSessionFactory();
    // Checks if the session connection has to be obtained using an external connection pool
    if (externalConnectionPool != null && getConnection(pool) == null) {
      Connection externalConnection;
      try {
        externalConnection = getNewConnection(pool);
        setConnection(pool, externalConnection);
      } catch (SQLException e) {
        throw new OBException("Could not get connection to create DAL session", e);
      }
    }

    Connection connection = getConnection(pool);
    if (connection != null) {
      // If the connection has been obtained using an external connection pool it is passed to
      // openSession, to prevent a new connection to be created using the Hibernate default
      // connection pool
      return sf.withOptions().connection(connection).openSession();
    } else {
      return sf.openSession();
    }
  }

  protected void closeSession() {
    closeSession(DEFAULT_POOL);
  }

  void closeSession(String pool) {
    Session session = sessions.get(pool);
    if (session != null) {
      if (session.isOpen()) {
        session.close();
      }
      removeSession(pool);
    }
  }

  /** Commits all remaining sessions and closes them */
  public void cleanUpSessions() {
    for (String pool : sessions.keySet()) {
      commitAndCloseNoCheck(pool);
    }
    clearSessions();
    clearTransactions();
    clearConnections();
  }

  private void removeSession(String pool) {
    sessions.remove(pool);
  }

  private void clearSessions() {
    sessions.clear();
  }

  /** Gets a new {@code Connection} from the connection pool. */
  public Connection getNewConnection() throws SQLException {
    return getNewConnection(DEFAULT_POOL);
  }

  /**
   * Gets a new {@code Connection} from the connection pool.
   * 
   * @param pool
   *          the name of the pool used to retrieve the connection
   * @return a {@code Connection} from the specified pool
   */
  public Connection getNewConnection(String pool) throws SQLException {
    Connection newConnection;
    if (externalConnectionPool != null) {
      newConnection = externalConnectionPool.getConnection(pool);
      try {
        // Autocommit is disabled because DAL is taking into account his logical and DAL is setting
        // autoCommit to false to maintain transactional way of working.
        newConnection.setAutoCommit(false);
      } catch (SQLException e) {
        log.error("Error setting connection to auto-commit mode", e);
      }
    } else {
      // getting connection from Hibernate pool
      DalSessionFactory sf = (DalSessionFactory) SessionFactoryController.getInstance()
          .getSessionFactory();
      newConnection = sf.getJdbcConnectionAccess().obtainConnection();
      sf.initConnection(newConnection);
    }
    return newConnection;
  }

  /** Gets current session's {@code Connection} if it's set, {@code null} if not. */
  public Connection getConnection() {
    // use getSession to create the session if it does not exist yet
    // in that case, the session will be created together with a new connection
    getSession(DEFAULT_POOL);
    return getConnection(DEFAULT_POOL);
  }

  private Connection getConnection(String pool) {
    return connections.get(pool);
  }

  /**
   * Sets the connection of the default pool to be used by the handler.
   * 
   * @param newConnection
   *          the connection of the default pool.
   */
  public void setConnection(Connection newConnection) {
    setConnection(DEFAULT_POOL, newConnection);
  }

  private void setConnection(String pool, Connection newConnection) {
    connections.put(pool, newConnection);
  }

  private void removeConnection(String pool) {
    connections.remove(pool);
  }

  private void clearConnections() {
    connections.clear();
  }

  private Transaction getTransaction(String pool) {
    return trxs.get(pool);
  }

  private void setTransaction(String pool, Transaction newTransaction) {
    trxs.put(pool, newTransaction);
  }

  private void removeTransaction(String pool) {
    trxs.remove(pool);
  }

  private void clearTransactions() {
    trxs.clear();
  }

  private void setAvailablePool(String pool) {
    availablePools.add(pool);
  }

  private void setUnavailablePool(String pool) {
    if (availablePools.contains(pool)) {
      availablePools.remove(pool);
    }
  }

  private boolean isAvailablePool(String pool) {
    return availablePools.contains(pool);
  }

  private boolean hasAvailablePools() {
    return !availablePools.isEmpty();
  }

  /**
   * Returns true when the current SessionHandler has a transaction and it is active.
   */
  public boolean isCurrentTransactionActive() {
    return isCurrentTransactionActive(DEFAULT_POOL);
  }

  private boolean isCurrentTransactionActive(String pool) {
    return trxs.containsKey(pool) && trxs.get(pool).isActive();
  }

  /**
   * Begins a new Transaction on the current HibernateSession and assigns it to the SessionHandler.
   * 
   * @throws OBException
   *           if there is already an available active transaction.
   */
  public void beginNewTransaction() throws OBException {
    beginNewTransaction(DEFAULT_POOL);
  }

  private void beginNewTransaction(String pool) throws OBException {
    if (isCurrentTransactionActive(pool)) {
      throw new OBException(
          "Not possible to start a new transaction while there is still one active.");
    }
    Session session = getSession(pool);
    if (!isCurrentTransactionActive(pool)) {
      // getSession has returned an existing session, so just begin a new transaction
      setTransaction(pool, session.beginTransaction());
    }
  }

  /**
   * Saves the object in this getSession().
   * 
   * @param obj
   *          the object to persist
   */
  public void save(Object obj) {
    save(DEFAULT_POOL, obj);
  }

  /**
   * Saves the object in the session of the pool whose name is passed as parameter.
   * 
   * @param pool
   *          the name of the pool used to retrieve the session where the object will be saved
   * @param obj
   *          the object to persist
   */
  public void save(String pool, Object obj) {
    if (Identifiable.class.isAssignableFrom(obj.getClass())) {
      getSession(pool).saveOrUpdate(((Identifiable) obj).getEntityName(), obj);
    } else {
      getSession(pool).saveOrUpdate(obj);
    }
  }

  /**
   * Delete the object from the db.
   * 
   * @param obj
   *          the object to remove
   */
  public void delete(Object obj) {
    delete(DEFAULT_POOL, obj);
  }

  /**
   * Delete the object from the db.
   * 
   * @param pool
   *          the name of the pool used to retrieve the session where the object will be deleted
   * @param obj
   *          the object to remove
   */
  public void delete(String pool, Object obj) {
    if (Identifiable.class.isAssignableFrom(obj.getClass())) {
      getSession(pool).delete(((Identifiable) obj).getEntityName(), obj);
    } else {
      getSession(pool).delete(obj);
    }
  }

  /**
   * Queries for a certain object using the class and id. If not found then null is returned.
   * 
   * @param clazz
   *          the class to query
   * @param id
   *          the id to use for querying
   * @return the retrieved object, can be null
   */
  public <T extends Object> T find(Class<T> clazz, Object id) {
    return find(DEFAULT_POOL, clazz, id);
  }

  /**
   * Queries for a certain object using the class and id. If not found then null is returned.
   * 
   * @param pool
   *          the name of the pool used to obtain the connection to execute the query
   * @param clazz
   *          the class to query
   * @param id
   *          the id to use for querying
   * @return the retrieved object, can be null
   */
  @SuppressWarnings("unchecked")
  public <T extends Object> T find(String pool, Class<T> clazz, Object id) {
    // translates a class to an entityname because the hibernate
    // getSession().get method can not handle class names if the entity was
    // mapped with entitynames.
    if (Identifiable.class.isAssignableFrom(clazz)) {
      return (T) find(pool, DalUtil.getEntityName(clazz), id);
    }
    return (T) getSession(pool).get(clazz, (Serializable) id);
  }

  /**
   * Queries for a certain object using the entity name and id. If not found then null is returned.
   * 
   * @param entityName
   *          the name of the entity to query
   * @param id
   *          the id to use for querying
   * @return the retrieved object, can be null
   * 
   * @see Entity
   */
  public BaseOBObject find(String entityName, Object id) {
    return find(DEFAULT_POOL, entityName, id);
  }

  /**
   * Queries for a certain object using the entity name and id. If not found then null is returned.
   * 
   * @param pool
   *          the name of the pool used to obtain the connection to execute the query
   * @param entityName
   *          the name of the entity to query
   * @param id
   *          the id to use for querying
   * @return the retrieved object, can be null
   * 
   * @see Entity
   */
  public BaseOBObject find(String pool, String entityName, Object id) {
    return (BaseOBObject) getSession(pool).get(entityName, (Serializable) id);
  }

  /**
   * Create a query object from the current getSession().
   * 
   * @param qryStr
   *          the HQL query
   * @param clazz
   *          the class of the query's resulting objects
   * @return a new Query object
   */
  public <T> Query<T> createQuery(String qryStr, Class<T> clazz) {
    return createQuery(DEFAULT_POOL, qryStr, clazz);
  }

  /**
   * Create a query object from the current getSession().
   * 
   * @param qryStr
   *          the HQL query
   * @return a new Query object
   */
  @SuppressWarnings("unchecked")
  public <T extends Object> Query<T> createQuery(String qryStr) {
    return (Query<T>) createQuery(DEFAULT_POOL, qryStr, Object.class);
  }

  private <T> Query<T> createQuery(String pool, String qryStr, Class<T> clazz) {
    return getSession(pool).createQuery(qryStr, clazz);
  }

  /**
   * Starts a transaction.
   */
  protected void begin() {
    begin(DEFAULT_POOL);
  }

  private void begin(String pool) {
    Check.isTrue(sessions.get(pool) == null, "Session must be null before begin");
    setSession(pool, createSession(pool));
    getSession(pool).setHibernateFlushMode(FlushMode.COMMIT);
    Check.isTrue(getTransaction(pool) == null, "tx must be null before begin");
    setTransaction(pool, getSession(pool).beginTransaction());
    setAvailablePool(pool);
    log.debug("Transaction started");
  }

  /**
   * Commits the transaction and closes the session, should normally be called at the end of all the
   * work.
   */
  public void commitAndClose() {
    commitAndClose(DEFAULT_POOL);
  }

  /**
   * Commits the transaction and closes the session, should normally be called at the end of all the
   * work.
   * 
   * @param pool
   *          the name of the pool which the transaction belongs.
   */
  public void commitAndClose(String pool) {
    boolean err = true;
    try {
      Check.isFalse(TriggerHandler.getInstance().isDisabled(),
          "Triggers disabled, commit is not allowed when in triggers-disabled mode, "
              + "call TriggerHandler.enable() before committing");

      checkInvariant(pool);
      flushRemainingChanges(pool);
      err = false;
    } finally {
      if (err) {
        // close transaction in case checks or flush failed, other case transaction is closed in
        // commit
        closeTransaction(pool, err);
      }
    }

    commitAndCloseNoCheck(pool);
  }

  private void commitAndCloseNoCheck(String pool) {
    boolean err = true;
    Connection con = getConnection(pool);
    Transaction trx = getTransaction(pool);
    try {
      if (con == null || !con.isClosed()) {
        if (con != null) {
          con.setAutoCommit(false);
        }
        if (trx != null && trx.isActive()) {
          if (trx.getStatus() == TransactionStatus.MARKED_ROLLBACK) {
            log.error(
                "Hibernate transaction was marked for rollback, its DB transaction is also rolled back.",
                new Exception("stack trace"));
          }
          trx.commit();
        }
      }
      err = false;
    } catch (SQLException e) {
      log.error("Error while closing the connection in pool " + pool,
          DbUtility.getUnderlyingSQLException(e));
    } finally {
      closeTransaction(pool, err);
    }
    log.debug("Transaction closed, session closed in pool " + pool);
  }

  private void closeTransaction(String pool, boolean err) {
    Connection con = getConnection(pool);
    Transaction trx = getTransaction(pool);
    if (err && trx != null) {
      try {
        trx.rollback();
      } catch (Throwable t) {
        // ignore these exception not to hide others
      }
    }
    try {
      if (con != null && !con.isClosed()) {
        con.close();
      }
    } catch (SQLException e) {
      log.error("Error while closing the connection in pool " + pool, e);
    }
    closeSession(pool);
    removeTransaction(pool);
    removeConnection(pool);
    setUnavailablePool(pool);
  }

  /**
   * Commits the transaction and starts a new transaction.
   */
  public void commitAndStart() {
    commitAndStart(DEFAULT_POOL);
  }

  private void commitAndStart(String pool) {
    Check.isFalse(TriggerHandler.getInstance().isDisabled(),
        "Triggers disabled, commit is not allowed when in triggers-disabled mode, "
            + "call TriggerHandler.enable() before committing");

    checkInvariant(pool);
    flushRemainingChanges(pool);
    Transaction trx = getTransaction(pool);
    if (trx != null) {
      trx.commit();
    }
    setTransaction(pool, getSession(pool).beginTransaction());
    log.debug("Committed and started new transaction");
  }

  private void flushRemainingChanges(String pool) {

    // business event handlers can change the data
    // during flush, flush several times until
    // the session is really cleaned up
    int countFlushes = 0;
    while (isSessionDirty(pool)) {
      OBDal.getInstance(pool).flush();
      countFlushes++;
      // arbitrary point to give up...
      if (countFlushes > 100) {
        log.error("Infinite loop in flushing session, tried more than 100 flushes");
        break;
      }
    }
  }

  /**
   * Rolls back the transaction and closes the session.
   */
  public void rollback() {
    rollback(DEFAULT_POOL);
  }

  /**
   * Rolls back the transaction and closes the session.
   * 
   * @param pool
   *          the name of the pool which the transaction belongs.
   */
  public void rollback(String pool) {
    log.debug("Rolling back transaction in pool " + pool);
    Connection con = getConnection(pool);
    try {
      checkInvariant(pool);
      if (con == null || !con.isClosed()) {
        getTransaction(pool).rollback();
      }
    } catch (SQLException e) {
      log.error("Error while closing the connection in pool " + pool, e);
    } finally {
      removeTransaction(pool);
      removeConnection(pool);
      setUnavailablePool(pool);
      try {
        if (con != null && !con.isClosed()) {
          con.close();
        }
        log.debug("Closing session");
        closeSession(pool);
      } catch (SQLException e) {
        log.error("Error while closing the connection in pool " + pool, e);
      }
    }
  }

  /**
   * The invariant is that for begin, rollback and commit the session etc. are alive
   */
  private void checkInvariant(String pool) {
    Check.isNotNull(sessions.get(pool), "Session is null");
    Transaction theTx = getTransaction(pool);
    Check.isNotNull(theTx, "Tx is null");
    Check.isTrue(theTx.isActive(), "Tx is not active");
  }

  /**
   * Registers that the transaction should be rolled back. Is used by the {@link DalThreadHandler}.
   * 
   * @param setRollback
   *          if true then the transaction will be rolled back at the end of the thread.
   */
  public void setDoRollback(boolean setRollback) {
    if (setRollback) {
      log.debug("Rollback is set to true");
    }
    this.doRollback = setRollback;
  }

  /** @return the doRollback value */
  public boolean getDoRollback() {
    return doRollback;
  }

  /**
   * Returns true if the session-in-view pattern should be supported. That is that the session is
   * closed and committed at the end of the request.
   * 
   * @return always true in this implementation
   */
  public boolean doSessionInViewPatter() {
    return true;
  }

}
