package com.etendoerp.base

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.Configuration
import org.apache.logging.log4j.core.config.LoggerConfig
import org.apache.logging.log4j.core.config.plugins.util.PluginManager
import org.hibernate.dialect.function.SQLFunction
import org.openbravo.base.provider.OBConfigFileProvider
import org.openbravo.base.session.SessionFactoryController
import org.openbravo.dal.core.DalContextListener
import org.openbravo.dal.core.DalLayerInitializer
import org.openbravo.dal.core.OBContext
import org.openbravo.database.ConnectionProvider
import org.openbravo.model.ad.access.User
import org.openbravo.service.db.DalConnectionProvider
import org.openbravo.test.base.TestConstants
import org.openbravo.test.base.TestLogAppender
import org.openbravo.test.base.mock.OBServletContextMock
import spock.lang.Specification
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.hamcrest.CoreMatchers.is
import static org.junit.Assume.assumeThat


class EBaseSpecification extends Specification {
    private static final Logger log = LogManager.getLogger();
    private static final String TEST_LOG_APPENDER_NAME = "TestLogAppender";
    private static TestLogAppender testLogAppender;
    private static List<String> disabledTestCases;
    private boolean disabledTestCase = false;

    private boolean errorOccured = false;

    /**
     * Record ID of Client "F&amp;B International Group"
     */
    protected static final String TEST_CLIENT_ID = "23C59575B9CF467C9620760EB255B389";

    /**
     * Record ID of Organization "F&amp;B España - Región Norte"
     */
    protected static final String TEST_ORG_ID = "E443A31992CB4635AFCAEABE7183CE85";

    /**
     * Record ID of Organization "F&amp;B US West Coast"
     */
    protected static final String TEST_US_ORG_ID = TestConstants.Orgs.US_WEST;

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
     * need to call {#getRandomUser} at least once
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

    def setup() throws Exception {
        PluginManager.addPackage("org.openbravo.test.base");
        setTestUserContext();
        errorOccured = false;
        if (shouldMockServletContext()) {
            setMockServletContext();
        }
        assumeThat("Disabled test case by configuration ", disabledTestCase, is(false));
    }

    def cleanup() {
        if (testLogAppender != null) {
            testLogAppender.reset();
            setTestLogAppenderLevel(Level.OFF);
        }
        if (shouldMockServletContext()) {
            cleanMockServletContext();
        }
    }

    def setupSpec() throws Exception {
        initializeTestLogAppender();
        staticInitializeDalLayer();
        initializeDisabledTestCases();
    }

    private void setMockServletContext() {
        OBServletContextMock mockServletContext = new OBServletContextMock();
        DalContextListener.setServletContext(mockServletContext);
    }

    protected boolean shouldMockServletContext() {
        return false;
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
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();

        testLogAppender = config.getAppender(TEST_LOG_APPENDER_NAME);

        LoggerConfig rootLoggerConfig = config.getRootLogger();
        rootLoggerConfig.removeAppender(TEST_LOG_APPENDER_NAME);

        rootLoggerConfig.addAppender(testLogAppender, level, null);
        context.updateLoggers();
    }

    /**
     * Initializes the DAL layer, can be overridden to add specific initialization behavior.
     *
     * @param sqlFunctions
     *          a Map with SQL functions to be registered in Hibernate during the DAL layer
     *          initialization. It can be null if not needed.
     * @throws Exception
     */
    protected void initializeDalLayer(Map<String, SQLFunction> sqlFunctions) throws Exception {
        if (areAllSqlFunctionsRegistered(sqlFunctions)) {
            // do not re-initialize the DAL layer, as the provided SQL functions are already registered
            return;
        }
        DalLayerInitializer.getInstance().setInitialized(false);
        log.info("Creating custom DAL layer initialization...");
        staticInitializeDalLayer(sqlFunctions);
    }

    private boolean areAllSqlFunctionsRegistered(Map<String, SQLFunction> sqlFunctions) {
        if (sqlFunctions == null || sqlFunctions.isEmpty()) {
            return true;
        }
        Map<String, SQLFunction> registeredFunctions = SessionFactoryController.getInstance()
                .getConfiguration()
                .getSqlFunctions();
        if (registeredFunctions == null) {
            return false;
        }
        for (String sqlFunction : sqlFunctions.keySet()) {
            if (!registeredFunctions.containsKey(sqlFunction)) {
                return false;
            }
        }
        return true;
    }
    protected static void staticInitializeDalLayer() throws Exception {
        staticInitializeDalLayer2(null);
    }

    private static void staticInitializeDalLayer2(Map<String, SQLFunction> sqlFunctions)
            throws Exception {
        DalLayerInitializer initializer = DalLayerInitializer.getInstance();
        if (!initializer.isInitialized()) {
            initializer.setSQLFunctions(sqlFunctions);
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
        OBContext.setOBContext(TestConstants.Users.SYSTEM);
    }

    /**
     * Sets the current user to the {@link #TEST_USER_ID} user.
     */
    protected void setTestUserContext() {
        OBContext.setOBContext(TEST_USER_ID, TEST_ROLE_ID, TEST_CLIENT_ID, TEST_ORG_ID);
    }

    /** Sets the current user to the 100 user as F&B Group Admin */
    protected void setTestAdminContext() {
        OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN, TestConstants.Clients.FB_GRP, TestConstants.Orgs.MAIN);
    }

    /** Sets the current user to the 100 user as QA Admin */
    protected static void setQAAdminContext() {
        OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.QA_ADMIN_ROLE, QA_TEST_CLIENT_ID, QA_TEST_ORG_ID);
    }

    protected static void initializeTestLogAppender() {
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();

        testLogAppender = config.getAppender(TEST_LOG_APPENDER_NAME);
    }

}

