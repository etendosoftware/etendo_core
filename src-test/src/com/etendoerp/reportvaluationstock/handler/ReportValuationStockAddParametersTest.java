package com.etendoerp.reportvaluationstock.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.servlet.ServletException;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.model.domaintype.DateDomainType;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.application.ReportDefinition;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Unit test class for {@link ReportValuationStock#addAdditionalParameters}.
 * It validates the correct behavior when additional parameters are added
 * to the report generation process.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReportValuationStockAddParametersTest {

  /**
   * Rule to handle expected exceptions during test execution.
   */
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  private ReportDefinition mockProcess;

  @Mock
  private RequestContext mockRequestContext;

  @Mock
  private VariablesSecureApp mockVars;

  @Mock
  private OBPropertiesProvider mockPropertiesProvider;

  @Mock
  private Properties mockProperties;

  private MockedStatic<RequestContext> mockedRequestContext;
  private MockedStatic<OBPropertiesProvider> mockedPropertiesProvider;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;

  private ReportValuationStock reportValuationStock;



  /**
   * Set up the test environment, including mocks and static contexts.
   *
   * @throws ServletException if an error occurs during setup
   */
  @Before
  public void setUp() throws ServletException {
    reportValuationStock = spy(new ReportValuationStock());

    mockedRequestContext = mockStatic(RequestContext.class);
    mockedPropertiesProvider = mockStatic(OBPropertiesProvider.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);

    mockedRequestContext.when(RequestContext::get).thenReturn(mockRequestContext);
    mockedPropertiesProvider.when(OBPropertiesProvider::getInstance).thenReturn(mockPropertiesProvider);

    mockedOBMessageUtils.when(() -> OBMessageUtils.translateError(anyString()))
        .thenReturn(null);
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(anyString()))
        .thenReturn("Test Message");

    when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);
    when(mockPropertiesProvider.getOpenbravoProperties()).thenReturn(mockProperties);
    when(mockProperties.getProperty("dateFormat.java")).thenReturn("yyyy-MM-dd");

    doNothing().when(reportValuationStock).buildData(
        any(VariablesSecureApp.class),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any(Boolean.class),
        any(Map.class)
    );
  }

  /**
   * Clean up static mocks after each test case execution.
   */
  @After
  public void tearDown() {
    if (mockedRequestContext != null) {
      mockedRequestContext.close();
    }
    if (mockedPropertiesProvider != null) {
      mockedPropertiesProvider.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
  }

  /**
   * Tests that additional parameters are added correctly to the report process
   * when valid input JSON content is provided.
   *
   * @throws Exception if an unexpected error occurs during the test
   */
  @Test
  public void testAddAdditionalParametersValidInput() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    JSONObject jsonContent = new JSONObject();
    JSONObject params = new JSONObject();

    params.put("AD_Org_ID", TestUtils.TEST_ORG_ID);
    params.put("M_Warehouse_ID", TestUtils.TEST_WAREHOUSE_ID);
    params.put("WarehouseConsolidation", true);
    params.put("M_Product_Category_ID", TestUtils.TEST_CATEGORY_ID);
    params.put("C_Currency_ID", TestUtils.TEST_CURRENCY_ID);
    params.put("Date", TestUtils.TEST_DATE);

    jsonContent.put("_params", params);
    jsonContent.put(ApplicationConstants.BUTTON_VALUE, "PDF");

    mock(DateDomainType.class);

    reportValuationStock.addAdditionalParameters(mockProcess, jsonContent, parameters);

    assertNotNull("Parameters should not be null", parameters);
    assertEquals("PDF", parameters.get("OUTPUT_FORMAT"));
  }
}
