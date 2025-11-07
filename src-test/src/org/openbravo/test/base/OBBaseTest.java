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
 * All portions are Copyright (C) 2014-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.base;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBConfigFileProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.DalContextListener;
import org.openbravo.dal.core.DalLayerInitializer;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.ExternalConnectionPool;
import org.openbravo.model.ad.access.User;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.TestConstants.Clients;
import org.openbravo.test.base.TestConstants.Orgs;
import org.openbravo.test.base.TestConstants.Roles;
import org.openbravo.test.base.TestConstants.Users;
import org.openbravo.test.base.mock.OBServletContextMock;

/**
 * OBBaseTest class which can/should be extended by most other test classes which want to make use
 * of the Openbravo test infrastructure.
 *
 * @author inigosanchez
 */

public class OBBaseTest {

  static {
    // Adds the package location of the plugins used by Log4j
    // in order to make them available before initialization
    PluginManager.addPackage("org.openbravo.test.base");
  }

  private static final Logger log = LogManager.getLogger();
  private static final String TEST_LOG_APPENDER_NAME = "TestLogAppender";
  private static TestLogAppender testLogAppender;
  private static List<String> disabledTestCases;
  private boolean disabledTestCase = false;

  @AfterEach
  protected void cleanupDalSession() {
    // Lógica para resetear el admin mode (ya la tenías en finished)
    if (OBContext.getOBContext() != null
        && !OBContext.getOBContext().getUser().getId().equals("0")
        && !OBContext.getOBContext().getRole().getId().equals("0")
        && OBContext.getOBContext().isInAdministratorMode()) {
      OBContext.clearAdminModeStack();
      OBContext.restorePreviousMode();
      log.warn("The test left the OBContext in administrator mode, it has been restored.");
    }

    // Logic to rollback the test session
    try {
      if (SessionHandler.isSessionHandlerPresent()) {
        log.debug("Doing rollback of the test session.");
        SessionHandler.getInstance().rollback();
      }
    } catch (final Exception e) {
      log.error("Error rolling back the test session", e);
    } finally {
      try {
        if (SessionHandler.isSessionHandlerPresent(ExternalConnectionPool.READONLY_POOL)) {
          SessionHandler.getInstance().commitAndClose(ExternalConnectionPool.READONLY_POOL);
        }
      } catch (Exception ex) {
        log.error("Error cleaning up read-only session", ex);
      }

      SessionHandler.deleteSessionHandler();
      OBContext.setOBContext((OBContext) null);
      log.debug("SessionHandler and OBContext cleaned up after test execution.");
    }
  }

  /**
   * Record ID of Client "F&amp;B International Group"
   */
  protected static final String TEST_CLIENT_ID = "23C59575B9CF467C9620760EB255B389";

  /**
   * Record ID of Organization "F&amp;B España - Región Norte"
   */
  protected static final String TEST_ORG_ID = Orgs.ESP_NORTE;

  /**
   * Record ID of Organization "F&amp;B US West Coast"
   */
  protected static final String TEST_US_ORG_ID = Orgs.US_WEST;

  /**
   * Record ID of Warehouse "España Región Norte"
   */
  protected static final String TEST_WAREHOUSE_ID = "B2D40D8A5D644DD89E329DC297309055";

  /**
   * Record ID of User "F&amp;BAdmin"
   */
  protected static final String TEST_USER_ID = "A530AAE22C864702B7E1C22D58E7B17B";

  /**
   * Record ID of User "F&amp;BESRNUser" - Any user with less privileges than {@link #TEST_USER_ID}
   */
  protected static final String TEST2_USER_ID = "75449AFBAE7F46029F26C85C4CCF714B";

  /**
   * Record IDs of available users different than {@link #TEST_USER_ID} Note: Initialized to null,
   * need to call {@link #getRandomUser} at least once
   */
  protected static List<User> userIds = null;

  /**
   * Record ID of Role "F&amp;B International Group Admin"
   */
  protected static final String TEST_ROLE_ID = "42D0EEB1C66F497A90DD526DC597E6F0";

  /**
   * Record ID of a Order in Draft status
   */
  protected static final String TEST_ORDER_ID = "F8492493E92C4EE5B5251AC4574778B7";

  /**
   * Record ID of Product "Zumo de Fresa Bio 0,33L"
   */
  protected static final String TEST_PRODUCT_ID = "61047A6B06B3452B85260C7BCF08E78D";

  /**
   * Map representation of current Organization tree for Client {@link #TEST_CLIENT_ID}
   */
  protected static Map<String, String[]> TEST_ORG_TREE = new HashMap<>();

  static {

    // "F&B International Group"
    TEST_ORG_TREE.put("19404EAD144C49A0AF37D54377CF452D", new String[]{ "" });

    // "F&B España, S.A."
    TEST_ORG_TREE.put("B843C30461EA4501935CB1D125C9C25A", new String[]{ "" });

    // "F&B US, Inc."
    TEST_ORG_TREE.put("2E60544D37534C0B89E765FE29BC0B43", new String[]{ "" });

  }

  /**
   * Record ID of the QA Test client
   */
  protected static final String QA_TEST_CLIENT_ID = "4028E6C72959682B01295A070852010D";

  /**
   * Record ID of the Main organization of QA Test client
   */
  protected static final String QA_TEST_ORG_ID = "43D590B4814049C6B85C6545E8264E37";

  /**
   * Record ID of the "Admin" user of QA Test client
   */
  protected static final String QA_TEST_ADMIN_USER_ID = "4028E6C72959682B01295A0735CB0120";

  /**
   * Record ID of the "Customer" Business Partner Category
   */
  protected static final String TEST_BP_CATEGORY_ID = "4028E6C72959682B01295F40C38C02EB";

  /**
   * Record ID of the geographical location "c\ de la Costa 54, San Sebastián 12784"
   */
  protected static final String TEST_LOCATION_ID = "A21EF1AB822149BEB65D055CD91F261B";

  /**
   * ISO code of the Euro currency
   */
  protected static final String EURO = "EUR";

  /**
   * Record ID of the Euro currency
   */
  protected static final String EURO_ID = "102";

  /**
   * ISO code of the US Dollar currency
   */
  protected static final String DOLLAR = "USD";

  /**
   * Record ID of the US Dollar currency
   */
  protected static final String DOLLAR_ID = "100";

  /**
   * Initializes DAL, it also creates a log appender that can be used to assert on logs. This log
   * appender is disabled by default, to activate it set the level with
   * {@link OBBaseTest#setTestLogAppenderLevel(Level)}.
   *
   * @see TestLogAppender
   */
  @BeforeAll
  public static void classSetUp() throws Exception {
    initializeTestLogAppender();
    staticInitializeDalLayer();
    initializeDisabledTestCases();
  }

  protected static void initializeTestLogAppender() {
    final LoggerContext context = LoggerContext.getContext(false);
    final Configuration config = context.getConfiguration();

    testLogAppender = config.getAppender(TEST_LOG_APPENDER_NAME);
  }

  /**
   * Sets the current user to the {@link #TEST_USER_ID} user. This method also mocks the servlet
   * context through the {@link DalContextListener} if the test case is configured to do so.
   */
  @BeforeEach
  public void setUp() throws Exception {
    // clear the session otherwise it keeps the old model
    setTestUserContext();
    if (shouldMockServletContext()) {
      setMockServletContext();
    }
    assumeThat("Disabled test case by configuration ", disabledTestCase, is(false));
  }

  /**
   * Test log appender is reset and switched off. This method also cleans the mock servlet context
   * when it applies.
   */
  @AfterEach
  public void testDone(TestInfo testInfo) {
    log.info("*** Ending test case " + testInfo.getDisplayName() + " ***");
    if (testLogAppender != null) {
      testLogAppender.reset();
      setTestLogAppenderLevel(Level.OFF);
    }
    if (shouldMockServletContext()) {
      cleanMockServletContext();
    }
  }

  /**
   * @return {@code true} if the test case should mock the servlet context. Otherwise, return
   *     {@code false}.
   */
  protected boolean shouldMockServletContext() {
    return false;
  }

  private void setMockServletContext() {
    OBServletContextMock mockServletContext = new OBServletContextMock();
    DalContextListener.setServletContext(mockServletContext);
  }

  private void cleanMockServletContext() {
    DalContextListener.setServletContext(null);
  }

  /**
   * Defines the threshold {@link Level} that will make messages to be tracked by
   * {@link TestLogAppender}. Note after test completion appender is reset and its level is set back
   * to Level.OFF disabling in this manner subsequent logging track.
   */
  protected void setTestLogAppenderLevel(org.apache.logging.log4j.Level level) {

    File log4jConfig = new File("src-test/src/log4j2-test.xml");

    final LoggerContext context = LoggerContext.getContext(false);
    context.setConfigLocation(log4jConfig.toURI());

    final Configuration config = context.getConfiguration();

    testLogAppender = config.getAppender(TEST_LOG_APPENDER_NAME);

    LoggerConfig rootLoggerConfig = config.getRootLogger();
    rootLoggerConfig.removeAppender(TEST_LOG_APPENDER_NAME);

    rootLoggerConfig.addAppender(testLogAppender, level, null);
    context.updateLoggers();
  }

  /**
   * Include in messages possible stack traces for logged Throwables
   */
  protected void setLogStackTraces(boolean log) {
    testLogAppender.setLogStackTraces(log);
  }

  /**
   * Returns log appender in order to be possible to do assertions on it
   */
  protected TestLogAppender getTestLogAppender() {
    return testLogAppender;
  }

  /**
   * Initializes the DAL layer, can be overridden to add specific initialization behavior.
   *
   * @throws Exception
   */
  protected void initializeDalLayer() throws Exception {
    DalLayerInitializer.getInstance().setInitialized(false);
    log.info("Creating custom DAL layer initialization...");
    staticInitializeDalLayer();
  }

  protected static void staticInitializeDalLayer() throws Exception {
    DalLayerInitializer initializer = DalLayerInitializer.getInstance();
    if (!initializer.isInitialized()) {
      initializer.initialize(true);
    }
  }

  protected static void initializeDisabledTestCases() {
    boolean alreadyInitialized = disabledTestCases != null;
    if (alreadyInitialized) {
      return;
    }

    Path disabledTestsConfig = Paths.get(OBConfigFileProvider.getInstance().getFileLocation(),
        "disabled-tests");
    if (!Files.exists(disabledTestsConfig)) {
      disabledTestCases = new ArrayList<>();
      return;
    }
    try {
      disabledTestCases = Files.readAllLines(disabledTestsConfig, StandardCharsets.UTF_8);
    } catch (IOException e) {
      log.error("Error reading disabled test configuration", e);
      disabledTestCases = new ArrayList<>();
    }
  }

  protected ConnectionProvider getConnectionProvider() {
    return new DalConnectionProvider(false);
  }

  /**
   * Set the current user to the 0 user.
   */
  protected void setSystemAdministratorContext() {
    OBContext.setOBContext(Users.SYSTEM);
  }

  /**
   * Sets the current user to the {@link #TEST_USER_ID} user.
   */
  protected void setTestUserContext() {
    OBContext.setOBContext(TEST_USER_ID, TEST_ROLE_ID, TEST_CLIENT_ID, TEST_ORG_ID);
  }

  /**
   * Sets the current user to the 100 user as F&amp;B Group Admin
   */
  protected void setTestAdminContext() {
    OBContext.setOBContext(Users.ADMIN, Roles.FB_GRP_ADMIN, Clients.FB_GRP, Orgs.MAIN);
  }

  /**
   * Sets the current user to the 100 user as QA Admin
   */
  protected static void setQAAdminContext() {
    OBContext.setOBContext(Users.ADMIN, Roles.QA_ADMIN_ROLE, QA_TEST_CLIENT_ID, QA_TEST_ORG_ID);
  }

  /**
   * Sets the current user. For the 0, 100 and 1000000 users this method should not be used. For
   * these users one of the other context-set methods should be used: {@link #setTestAdminContext()}
   * , {@link #setTestUserContext()} or {@link #setSystemAdministratorContext()}.
   *
   * @param userId
   *     the id of the user to use.
   */
  protected void setUserContext(String userId) {
    if (userId.equals("0")) {
      log.warn("Forwarding the call to setSystemAdministratorContext, "
          + "consider using that method directly");
      setSystemAdministratorContext();
    } else if (userId.equals("100")) {
      log.warn("Forwarding the call to setFBGroupAdminContext method, "
          + "consider using that method directly");
      setTestAdminContext();
    } else if (userId.equals("1000000")) {
      log.warn("User id 1000000 is not longer available, please update your test. "
          + "Forwarding call to the setTestUserContext method, "
          + "consider using that method directly");
      setTestUserContext();
    } else {
      OBContext.setOBContext(userId);
    }
  }

  /**
   * Gets a random User (Record ID) from the available ones in the test client. The ID is one
   * different than {@link #TEST_USER_ID}
   *
   * @return A record ID of a available user
   */
  protected User getRandomUser() {
    if (userIds == null) {
      setTestUserContext();

      String[] excludedUserIds = { "100", TEST_USER_ID };
      List<String> excludedList = Arrays.asList(excludedUserIds);

      String hql = "where id NOT IN (:excludedIds) and aDUserRolesList is not empty";

      List<User> users = OBDal.getInstance()
          .createQuery(User.class, hql)
          .setNamedParameter("excludedIds", excludedList)
          .list();

      if (users.isEmpty()) {
        throw new RuntimeException("Unable to initialize the list of available users");
      }
      userIds = new ArrayList<>(users);
    }

    Random r = new Random();
    return userIds.get(r.nextInt(userIds.size()));
  }

  /**
   * Prints the stacktrace of the exception to System.err. Handles the case that the exception is a
   * SQLException which has the real causing exception in the
   * {@link SQLException#getNextException()} method.
   *
   * @param e
   *     the exception to report.
   */
  protected void reportException(Exception e) {
    if (e == null) {
      return;
    }
    e.printStackTrace(System.err);
    if (e instanceof SQLException) {
      reportException(((SQLException) e).getNextException());
    }
  }

  /**
   * Does a rollback of the transaction;
   */
  public void rollback() {
    OBDal.getInstance().rollbackAndClose();
  }

  /**
   * Commits the transaction to the database.
   */
  public void commitTransaction() {
    OBDal.getInstance().commitAndClose();
  }

  /**
   * Convenience method, gets an instance for the passed Class from the database. If there are no
   * records for that class then an exception is thrown. If there is more than one result then an
   * arbitrary instance is returned (the first one in the un-ordered resultset).
   *
   * @param <T>
   *     the specific class to query for.
   * @param clz
   *     instances
   * @return an instance of clz.
   */
  protected <T extends BaseOBObject> T getOneInstance(Class<T> clz) {
    final OBCriteria<T> obc = OBDal.getInstance().createCriteria(clz);
    if (obc.list().size() == 0) {
      throw new OBException("There are zero instances for class " + clz.getName());
    }
    return obc.list().get(0);
  }

  /**
   * Extends the read and write access of the current user to also include the passed class. This
   * can be used to circumvent restrictive access which is not usefull for the test itself.
   *
   * @param clz
   *     after this call the current user (in the {@link OBContext}) will have read/write
   *     access to this class.
   */
  protected void addReadWriteAccess(Class<?> clz) {
    final Entity entity = ModelProvider.getInstance().getEntity(clz);
    if (!OBContext.getOBContext().getEntityAccessChecker().getWritableEntities().contains(entity)) {
      OBContext.getOBContext().getEntityAccessChecker().getWritableEntities().add(entity);
    }
    if (!OBContext.getOBContext().getEntityAccessChecker().getReadableEntities().contains(entity)) {
      OBContext.getOBContext().getEntityAccessChecker().getReadableEntities().add(entity);
    }
  }

  /**
   * Counts the total occurences in the database for the passed class. Note that active, client and
   * organization filtering applies.
   *
   * @param <T>
   *     a class type parameter
   * @param clz
   *     the class to count occurences for
   * @return the number of occurences which are active and belong to the current client/organization
   */
  protected <T extends BaseOBObject> int count(Class<T> clz) {
    final OBCriteria<T> obc = OBDal.getInstance().createCriteria(clz);
    return obc.count();
  }
}
