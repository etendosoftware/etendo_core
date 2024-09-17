package org.openbravo.erpReports;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import javax.enterprise.inject.Instance;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.common.hooks.PrintControllerHook;
import org.openbravo.common.hooks.PrintControllerHookManager;
import org.openbravo.dal.core.DalContextListener;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.reporting.DocumentType;
import org.openbravo.erpCommon.utility.reporting.Report;
import org.openbravo.erpCommon.utility.reporting.printing.PrintController;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.TestConstants;
import org.openbravo.test.base.mock.HttpServletRequestMock;
import org.openbravo.test.base.mock.ServletContextMock;
import org.openbravo.xmlEngine.XmlEngine;

/**
 * Test class for the {@link PrintController} that verifies the behavior of print hooks
 * during the printing process. This class uses JUnit and Mockito for unit testing and
 * is designed to ensure that the pre-processing and post-processing methods of all
 * available hooks are executed correctly.
 *
 * <p>
 * The tests set up a mocked environment that simulates the necessary context for
 * executing print commands. The class utilizes a parameterized test rule to run tests
 * with different command types (e.g., PRINT and ARCHIVE).
 * </p>
 *
 * <p>
 * The main focus of this test class is to validate that the hooks' methods are called
 * as expected when a print command is executed. It also ensures that the print
 * controller is configured correctly with necessary parameters.
 * </p>
 *
 * <p>
 * This class extends {@link WeldBaseTest} to leverage CDI (Contexts and Dependency Injection)
 * functionalities and utilizes various mocking techniques to isolate the behavior of the
 * components being tested.
 * </p>
 *
 * <p>
 * The following methods are included in this class:
 * </p>
 *
 * <ul>
 *   <li>{@link #setUp()} - Initializes the test environment and mocks necessary components.</li>
 *   <li>{@link #setupContextAndVars()} - Sets up the Openbravo context and secure variables.</li>
 *   <li>{@link #setupPrintControllerMock(ConnectionProvider, ServletContextMock)} - Configures the print controller mock.</li>
 *   <li>{@link #setupConfigParams(String)} - Sets up configuration parameters for testing.</li>
 *   <li>{@link #testPrintingExecutesHooks()} - Tests that pre and post process methods from all hooks are executed.</li>
 *   <li>{@link #invokePrintMethod(HttpServletRequest, VariablesSecureApp)} - Invokes the print method on the print controller.</li>
 *   <li>{@link #setVarsForTest(String)} - Sets secure variables for the test based on the command type.</li>
 *   <li>{@link #setInaccessibleField(Object, String, Object)} - Sets a private field on the given object.</li>
 *   <li>{@link #getField(Class, String)} - Retrieves a field from the class or its superclasses.</li>
 *   <li>{@link #tearDown()} - Cleans up after each test, closing any mocks.</li>
 * </ul>
 *
 * <p>
 * The class uses annotations such as {@link Before}, {@link After}, and {@link Test} to define
 * the lifecycle of the tests and the specific test cases to execute.
 * </p>
 *
 * @see PrintController
 * @see PrintControllerHook
 * @see PrintControllerHookManager
 * @see ApplicationDictionaryCachedStructures
 * @see VariablesSecureApp
 */
public class PrintControllerHookTest extends WeldBaseTest {

  public static final String COMMAND_ARCHIVE = "ARCHIVE";
  public static final String COMMAND_PRINT = "PRINT";
  public static final String TEST_INVOICE_ID = "65C78E1C0CF0464C83CC4D0BE8EB6D94";
  private static final List<String> PARAMS = Arrays.asList(COMMAND_PRINT, COMMAND_ARCHIVE);
  /**
   * A rule that facilitates parameterized testing for CDI (Contexts and Dependency Injection)
   * by providing a set of parameters for each test case. This field is an instance of
   * {@link ParameterCdiTestRule} initialized with a list of command types, specifically
   * {@code COMMAND_PRINT} and {@code COMMAND_ARCHIVE}.
   * <p>
   * This parameterized rule allows the testing of different command execution paths,
   * such as printing and archiving, within the context of the test methods.
   * The associated test methods can utilize the injected parameters to verify
   * the behavior of the print process and ensure that pre- and post-processing hooks
   * are executed correctly.
   */
  @Rule
  public ParameterCdiTestRule<String> parameterCdiTestRule = new ParameterCdiTestRule<>(PARAMS);
  MockedStatic<WeldUtils> weldUtilsMock;
  @Spy
  private Instance<PrintControllerHook> hooksInstancesMock;
  @Mock
  private PrintControllerHook hook;
  @Mock
  private ApplicationDictionaryCachedStructures applicationDictionaryCachedStructuresMock;
  @Mock
  private ConfigParameters configParametersMock;
  @Mock
  private ServletOutputStream outputStreamMock;
  @Mock
  private HttpServletResponse responseMock;
  @Spy
  private PrintControllerHookManager printControllerHookManagerMock;
  @Spy
  @InjectMocks
  private PrintController printControllerMock;
  private @ParameterCdiTest
  String printCommandType;

  private static void setupContextAndVars() {
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN,
        TestConstants.Clients.FB_GRP, TestConstants.Orgs.ESP_NORTE);
    VariablesSecureApp vars = new VariablesSecureApp(
        OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId());
    RequestContext.get().setVariableSecureApp(vars);
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.openMocks(this);
    weldUtilsMock = Mockito.mockStatic(WeldUtils.class);
    setupContextAndVars();

    // Set up servlet context and configurations
    ConnectionProvider conn = new DalConnectionProvider();
    String contextPath = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties().getProperty("source.path");
    ServletContextMock servletContextMock = new ServletContextMock(contextPath, "");
    RequestContext.setServletContext(servletContextMock);
    DalContextListener.setServletContext(servletContextMock);

    setupConfigParams(contextPath);
    setupPrintControllerMock(conn, servletContextMock);

    // Mock hooksInstancesMock to return our mocked hook
    Mockito.when(hooksInstancesMock.iterator())
        .thenAnswer(invocation -> List.of(hook).iterator());
    Mockito.doAnswer(invocation -> {
      Consumer<PrintControllerHook> action = invocation.getArgument(0);
      action.accept(hook);
      return null;
    }).when(hooksInstancesMock).forEach(any());

    // Inject the mocked hooks into PrintControllerHookManager
    Field hooksField = PrintControllerHookManager.class.getDeclaredField("hooks");
    hooksField.setAccessible(true);
    hooksField.set(printControllerHookManagerMock, hooksInstancesMock);
  }

  private void setupPrintControllerMock(ConnectionProvider conn,
      ServletContextMock servletContextMock) throws Exception {
    setInaccessibleField(printControllerMock, "globalParameters", configParametersMock);
    printControllerMock.xmlEngine = new XmlEngine(conn);
    Mockito.doReturn(conn.getRDBMS()).when(printControllerMock).getRDBMS();
    if (StringUtils.equals(COMMAND_ARCHIVE, printCommandType)) {
      Mockito.doNothing().when(printControllerMock).buildReport(any(), any(), anyString(), any(), any());
    }
    servletContextMock.setAttribute("openbravoConfig", configParametersMock);
  }

  private void setupConfigParams(String contextPath) throws NoSuchFieldException, IllegalAccessException {
    setInaccessibleField(configParametersMock, "strFTPDirectory", contextPath + "/attachments");
    setInaccessibleField(configParametersMock, "strBaseDesignPath", "src-loc");
    setInaccessibleField(configParametersMock, "strDefaultDesignPath", "design");
    setInaccessibleField(configParametersMock, "prefix", contextPath + "/WebContent/");
  }

  @Test
  @DisplayName("The print process should execute pre and post process methods from all hooks available")
  public void testPrintingExecutesHooks() throws Exception {
    // Stub response output stream
    Mockito.doReturn(outputStreamMock).when(responseMock).getOutputStream();

    HttpServletRequest request = new HttpServletRequestMock();
    RequestContext.get().setRequest(request);
    VariablesSecureApp vars = setVarsForTest(printCommandType);

    // Mock WeldUtils static methods
    weldUtilsMock.when(() -> WeldUtils.getInstanceFromStaticBeanManager(PrintControllerHookManager.class))
        .thenReturn(printControllerHookManagerMock);
    weldUtilsMock.when(() -> WeldUtils.getInstanceFromStaticBeanManager(ApplicationDictionaryCachedStructures.class))
        .thenReturn(applicationDictionaryCachedStructuresMock);
    Mockito.doReturn(false).when(applicationDictionaryCachedStructuresMock).isInDevelopment();

    // Execute the print method
    invokePrintMethod(request, vars);

    // Verify that preProcess and postProcess are called exactly once
    Mockito.verify(hook).preProcess(any());
    Mockito.verify(hook).postProcess(any());
  }

  private void invokePrintMethod(HttpServletRequest request, VariablesSecureApp vars)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Method printMethod = PrintController.class.getDeclaredMethod("post", HttpServletRequest.class,
        HttpServletResponse.class, VariablesSecureApp.class, DocumentType.class, String.class, String.class);
    printMethod.setAccessible(true);
    printMethod.invoke(printControllerMock, request, responseMock, vars,
        DocumentType.SALESINVOICE, "PRINTINVOICES",
        TEST_INVOICE_ID);
  }

  private VariablesSecureApp setVarsForTest(String commandType) throws Exception {
    VariablesSecureApp vars = new VariablesSecureApp(TestConstants.Users.ADMIN,
        TestConstants.Clients.FB_GRP, TestConstants.Orgs.ESP_NORTE,
        TestConstants.Roles.FB_GRP_ADMIN);
    RequestContext.get().setVariableSecureApp(vars);
    HashMap<String, Report> reportsHashMap = new HashMap<>();
    reportsHashMap.put(TEST_INVOICE_ID,
        new Report(DocumentType.SALESINVOICE, TEST_INVOICE_ID, "en_US",
            "3421180B470A49BC8C072E8287BC9342", false, Report.OutputTypeEnum.ARCHIVE));
    vars.setSessionValue("#AD_ReportDecimalSeparator", ".");
    vars.setSessionValue("#AD_ReportGroupingSeparator", ",");
    vars.setSessionValue("#AD_ReportNumberFormat", "#,##0.00");
    vars.setSessionValue("inpTabId", "263");
    if (StringUtils.equals(COMMAND_ARCHIVE, commandType)) {
      vars.setSessionObject("PRINTINVOICES.Documents", reportsHashMap);
    }
    setInaccessibleField(vars, "command", commandType);
    return vars;
  }

  private void setInaccessibleField(Object target, String fieldName,
      Object value) throws NoSuchFieldException, IllegalAccessException {
    Field field = getField(target.getClass(), fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    Class<?> currentClass = clazz;
    while (currentClass != null) {
      try {
        return currentClass.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        currentClass = currentClass.getSuperclass();
      }
    }
    throw new NoSuchFieldException("Field " + fieldName + " not found in class hierarchy");
  }

  @After
  public void tearDown() {
    weldUtilsMock.close();
  }
}
