package org.openbravo.erpReports;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

import javax.enterprise.inject.Instance;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
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
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.common.hooks.PrintControllerHook;
import org.openbravo.common.hooks.PrintControllerHookManager;
import org.openbravo.dal.core.DalContextListener;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.reporting.DocumentType;
import org.openbravo.erpCommon.utility.reporting.printing.PrintController;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.TestConstants;
import org.openbravo.test.base.mock.HttpServletRequestMock;
import org.openbravo.test.base.mock.ServletContextMock;
import org.openbravo.xmlEngine.XmlEngine;

public class PrintControllerHookTest extends WeldBaseTest {

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
    }).when(hooksInstancesMock).forEach(Mockito.any());

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
    servletContextMock.setAttribute("openbravoConfig", configParametersMock);
  }

  private void setupConfigParams(String contextPath) throws Exception {
    setInaccessibleField(configParametersMock, "strFTPDirectory", contextPath + "/attachments");
    setInaccessibleField(configParametersMock, "strBaseDesignPath", "src-loc");
    setInaccessibleField(configParametersMock, "strDefaultDesignPath", "design");
    setInaccessibleField(configParametersMock, "prefix", contextPath + "/WebContent/");
  }

  @Test
  @DisplayName("The print process should execute pre and post process methods from all hooks available")
  public void testPrintingWithoutAttachmentExecutesHooks() throws Exception {
    try (MockedStatic<WeldUtils> weldUtilsMock = Mockito.mockStatic(WeldUtils.class)) {

      // Stub response output stream
      Mockito.doReturn(outputStreamMock).when(responseMock).getOutputStream();

      HttpServletRequest request = new HttpServletRequestMock();
      RequestContext.get().setRequest(request);
      VariablesSecureApp vars = setVarsForTest();

      // Mock WeldUtils static methods
      weldUtilsMock.when(() -> WeldUtils.getInstanceFromStaticBeanManager(PrintControllerHookManager.class))
          .thenReturn(printControllerHookManagerMock);
      weldUtilsMock.when(() -> WeldUtils.getInstanceFromStaticBeanManager(ApplicationDictionaryCachedStructures.class))
          .thenReturn(applicationDictionaryCachedStructuresMock);
      Mockito.doReturn(false).when(applicationDictionaryCachedStructuresMock).isInDevelopment();

      // Execute the print method
      invokePrintMethod(request, vars);

      // Verify that preProcess and postProcess are called exactly once
      Mockito.verify(hook).preProcess(Mockito.any());
      Mockito.verify(hook).postProcess(Mockito.any());
    }
  }

  private void invokePrintMethod(HttpServletRequest request, VariablesSecureApp vars)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Method printMethod = PrintController.class.getDeclaredMethod("post", HttpServletRequest.class,
        HttpServletResponse.class, VariablesSecureApp.class, DocumentType.class, String.class, String.class);
    printMethod.setAccessible(true);
    printMethod.invoke(printControllerMock, request, responseMock, vars,
        DocumentType.SALESINVOICE, "PRINTINVOICES",
        "65C78E1C0CF0464C83CC4D0BE8EB6D94");
  }

  private VariablesSecureApp setVarsForTest() throws Exception {
    VariablesSecureApp vars = new VariablesSecureApp(TestConstants.Users.ADMIN,
        TestConstants.Clients.FB_GRP, TestConstants.Orgs.ESP_NORTE,
        TestConstants.Roles.FB_GRP_ADMIN);
    RequestContext.get().setVariableSecureApp(vars);
    vars.setSessionValue("#AD_ReportDecimalSeparator", ".");
    vars.setSessionValue("#AD_ReportGroupingSeparator", ",");
    vars.setSessionValue("#AD_ReportNumberFormat", "#,##0.00");
    vars.setSessionValue("inpTabId", "263");
    setInaccessibleField(vars, "command", "PRINT");
    return vars;
  }

  private void setInaccessibleField(Object target, String fieldName, Object value) throws Exception {
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
}
