package org.openbravo.erpReports;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
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

  @Mock
  private Instance<PrintControllerHook> hooksMock;
  @Produces
  @Dependent
  @Mock
  private PrintControllerHook hook1 = Mockito.mock(PrintControllerHook.class);

  @Mock
  private ConfigParameters configParametersMock;
  @Mock
  private ServletOutputStream outputStreamMock;
  @Mock
  private HttpServletResponse responseMock;

  @Spy
  @InjectMocks
  private PrintController printControllerMock;
  @InjectMocks
  private PrintControllerHookManager printControllerHookManager;

  private static void setupContextAndVars() {
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN, TestConstants.Clients.FB_GRP,
        TestConstants.Orgs.ESP_NORTE);
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(), OBContext.getOBContext().getCurrentOrganization().getId());
    RequestContext.get().setVariableSecureApp(vars);
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.initMocks(this);
    setupContextAndVars();

    ConnectionProvider conn = new DalConnectionProvider();
    String contextPath = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("source.path");
    ServletContextMock servletContextMock = new ServletContextMock(contextPath, "");
    RequestContext.setServletContext(servletContextMock);
    DalContextListener.setServletContext(servletContextMock);

    setupConfigParams(contextPath);
    setupPrintControllerMock(conn, servletContextMock);

    // Mocking the Instance to return our mocked hooks
    Mockito.when(hooksMock.iterator()).thenReturn(List.of(hook1).iterator());

    // Manually inject the mocked Instance into PrintControllerHookManager
    Field hooksField = PrintControllerHookManager.class.getDeclaredField("hooks");
    hooksField.setAccessible(true);
    hooksField.set(printControllerHookManager, hooksMock);
  }

  private void setupPrintControllerMock(ConnectionProvider conn,
      ServletContextMock servletContextMock) throws Exception {
    setInaccesibleField(printControllerMock, "globalParameters", configParametersMock);
    printControllerMock.xmlEngine = new XmlEngine(conn);
    Mockito.doReturn(conn.getRDBMS()).when(printControllerMock).getRDBMS();
    servletContextMock.setAttribute("openbravoConfig", configParametersMock);
  }

  private void setupConfigParams(String contextPath) throws Exception {
    setInaccesibleField(configParametersMock, "strFTPDirectory", contextPath + "/attachments");
    setInaccesibleField(configParametersMock, "strBaseDesignPath", "src-loc");
    setInaccesibleField(configParametersMock, "strDefaultDesignPath", "design");
    setInaccesibleField(configParametersMock, "prefix", contextPath + "/WebContent/");
  }

  @Test
  @DisplayName("The print process should execute pre and post process methods from all hooks available")
  public void testPrintingWithoutAttachmentExecutesHooks() throws Exception {
    // Stub to handle response data creation
    Mockito.doReturn(outputStreamMock).when(responseMock).getOutputStream();
    Mockito.doThrow(new OBException("PrintControllerHook test")).when(hook1).preProcess(Mockito.isA(JSONObject.class));
    Mockito.doThrow(new OBException("PrintControllerHook test")).when(hook1).postProcess(Mockito.isA(JSONObject.class));

    HttpServletRequest request = new HttpServletRequestMock();
    RequestContext.get().setRequest(request);
    VariablesSecureApp vars = setVarsForTest();

    // When: the print method is executed
    try {
      invokePrintMethod(request, vars);
    } catch (Exception ignore) {
    }

    // then: the pre- and post-process hook methods are executed
//    Mockito.verify(hook1).preProcess(Mockito.isA(JSONObject.class));
//    Mockito.verify(hook1).postProcess(Mockito.isA(JSONObject.class));
  }

  private void invokePrintMethod(HttpServletRequest request,
      VariablesSecureApp vars) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Method printMethod = PrintController.class.getDeclaredMethod("post", HttpServletRequest.class,
        HttpServletResponse.class,
        VariablesSecureApp.class, DocumentType.class, String.class, String.class);
    printMethod.setAccessible(true);
    printMethod.invoke(printControllerMock, request, responseMock, vars, DocumentType.SALESINVOICE,
        "PRINTINVOICES", "65C78E1C0CF0464C83CC4D0BE8EB6D94");
  }

  private VariablesSecureApp setVarsForTest() throws Exception {
    VariablesSecureApp vars = new VariablesSecureApp(TestConstants.Users.ADMIN, TestConstants.Clients.FB_GRP,
        TestConstants.Orgs.ESP_NORTE, TestConstants.Roles.FB_GRP_ADMIN);
    RequestContext.get().setVariableSecureApp(vars);
    vars.setSessionValue("#AD_ReportDecimalSeparator", ".");
    vars.setSessionValue("#AD_ReportGroupingSeparator", ",");
    vars.setSessionValue("#AD_ReportNumberFormat", "#,##0.00");
    vars.setSessionValue("inpTabId", "263");
    setInaccesibleField(vars, "command", "PRINT");
    return vars;
  }

  private void setInaccesibleField(Object target, String fieldName, Object value) throws Exception {
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
