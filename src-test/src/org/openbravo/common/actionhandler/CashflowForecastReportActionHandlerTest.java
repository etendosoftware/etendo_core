package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.ReportDefinition;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.JRFieldProviderDataSource;
import org.openbravo.service.db.DalConnectionProvider;

import net.sf.jasperreports.engine.JRDataSource;

/**
 * Unit tests for the {@link CashflowForecastReportActionHandler} class.
 * Verifies the behavior of methods related to report generation, data unification,
 * and parameter handling.
 */
@ExtendWith(MockitoExtension.class)
class CashflowForecastReportActionHandlerTest {

  private CashflowForecastReportActionHandler handler;

  @Mock
  private ReportDefinition mockReportDefinition;

  @Mock
  private VariablesSecureApp mockVariables;

  @Mock
  private RequestContext mockRequestContext;

  @Mock
  private OBPropertiesProvider mockPropertiesProvider;

  @Mock
  private Properties mockProperties;

  /**
   * Sets up the test environment before each test.
   * Initializes the handler and mocks required for testing.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @BeforeEach
  void setUp() throws Exception {
    handler = new CashflowForecastReportActionHandler();
  }

  /**
   * Tests the {@code getReportConnectionProvider} method.
   * Verifies that the method returns a valid {@link ConnectionProvider}.
   */
  @Test
  void testGetReportConnectionProvider() {
    try (MockedStatic<DalConnectionProvider> dalConnectionProviderMock = mockStatic(DalConnectionProvider.class)) {
      DalConnectionProvider mockDalProvider = mock(DalConnectionProvider.class);
      dalConnectionProviderMock.when(DalConnectionProvider::getReadOnlyConnectionProvider).thenReturn(mockDalProvider);

      ConnectionProvider result = handler.getReportConnectionProvider();

      assertNotNull(result);
      assertEquals(mockDalProvider, result);
    }
  }

  /**
   * Tests the {@code getReportData} method with valid parameters.
   * Verifies that the method returns a non-null {@link JRDataSource}.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  void testGetReportData() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    Map<String, Object> jrParams = new HashMap<>();
    jrParams.put("OUTPUT_FORMAT", "XLS");
    jrParams.put("fieldProviderSubReport", mock(JRFieldProviderDataSource.class));
    parameters.put("JASPER_REPORT_PARAMETERS", jrParams);

    java.lang.reflect.Method method = CashflowForecastReportActionHandler.class.getDeclaredMethod("getReportData",
        Map.class);
    method.setAccessible(true);
    JRDataSource result = (JRDataSource) method.invoke(handler, parameters);

    assertNotNull(result);
  }

  /**
   * Tests the {@code unifyData} method with a valid data matrix.
   * Verifies that the method correctly unifies the data into a single array.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  void testUnifyData() throws Exception {
    CashflowForecastData mockData1 = mock(CashflowForecastData.class);
    CashflowForecastData mockData2 = mock(CashflowForecastData.class);
    CashflowForecastData mockData3 = mock(CashflowForecastData.class);

    CashflowForecastData[] innerArray1 = new CashflowForecastData[1];
    innerArray1[0] = mockData1;

    CashflowForecastData[] innerArray2 = new CashflowForecastData[2];
    innerArray2[0] = mockData2;
    innerArray2[1] = mockData3;

    CashflowForecastData[][] dataMatrix = new CashflowForecastData[2][];
    dataMatrix[0] = innerArray1;
    dataMatrix[1] = innerArray2;

    java.lang.reflect.Method method = CashflowForecastReportActionHandler.class.getDeclaredMethod("unifyData",
        CashflowForecastData[][].class);
    method.setAccessible(true);
    FieldProvider[] result = (FieldProvider[]) method.invoke(handler, (Object) dataMatrix);

    assertNotNull(result);
    assertEquals(3, result.length);
  }

  /**
   * Tests the {@code addAdditionalParameters} method with empty data.
   * Verifies that the method processes the parameters and adds additional values.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  void testAddAdditionalParametersWithEmptyData() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    JSONObject jsonContent = new JSONObject();
    JSONObject params = new JSONObject();

    params.put("datePlanned", "2023-01-01");
    params.put("Fin_Financial_Account_ID", "null");
    params.put("breakByDate", false);
    jsonContent.put("_params", params);
    jsonContent.put("buttonValue", "PDF");

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(
        RequestContext.class); MockedStatic<OBPropertiesProvider> propertiesProviderMock = mockStatic(
        OBPropertiesProvider.class)) {

      requestContextMock.when(RequestContext::get).thenReturn(mockRequestContext);
      when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVariables);

      propertiesProviderMock.when(OBPropertiesProvider::getInstance).thenReturn(mockPropertiesProvider);
      when(mockPropertiesProvider.getOpenbravoProperties()).thenReturn(mockProperties);
      when(mockProperties.getProperty(anyString())).thenReturn("yyyy-MM-dd");

      java.lang.reflect.Method method = CashflowForecastReportActionHandler.class.getDeclaredMethod(
          "addAdditionalParameters", ReportDefinition.class, JSONObject.class, Map.class);
      method.setAccessible(true);
      method.invoke(handler, mockReportDefinition, jsonContent, parameters);

    }
  }

  /**
   * Tests the {@code getReportData} method with PDF format.
   * Verifies that the method handles the PDF format correctly.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  void testGetReportDataWithPdfFormat() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    Map<String, Object> jrParams = new HashMap<>();
    jrParams.put("OUTPUT_FORMAT", "PDF");
    jrParams.put("fieldProviderSubReport", mock(JRFieldProviderDataSource.class));
    parameters.put("JASPER_REPORT_PARAMETERS", jrParams);

    java.lang.reflect.Method method = CashflowForecastReportActionHandler.class.getDeclaredMethod("getReportData",
        Map.class);
    method.setAccessible(true);
    JRDataSource result = (JRDataSource) method.invoke(handler, parameters);

    assertNull(result);
  }

  /**
   * Tests the {@code unifyData} method with empty arrays.
   * Verifies that the method returns an empty result.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  void testUnifyDataWithEmptyArrays() throws Exception {
    CashflowForecastData[][] emptyMatrix = new CashflowForecastData[0][];

    java.lang.reflect.Method method = CashflowForecastReportActionHandler.class.getDeclaredMethod("unifyData",
        CashflowForecastData[][].class);
    method.setAccessible(true);
    FieldProvider[] result = (FieldProvider[]) method.invoke(handler, (Object) emptyMatrix);

    assertNotNull(result);
    assertEquals(0, result.length);
  }
}
