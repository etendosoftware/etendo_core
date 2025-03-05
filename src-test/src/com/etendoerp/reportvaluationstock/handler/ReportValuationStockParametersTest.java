package com.etendoerp.reportvaluationstock.handler;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.ReportDefinition;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Unit tests for the {@link ReportValuationStock} class.
 *
 * <p>This class is responsible for testing the functionality of the
 * {@code addAdditionalParameters} method in the {@code ReportValuationStock} class.
 *
 * <p>Tests include scenarios for invalid dates and missing required parameters.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReportValuationStockParametersTest {


  @Mock
  private ReportDefinition mockProcess;

  @Mock
  private RequestContext mockRequestContext;

  @Mock
  private VariablesSecureApp mockVars;

  @Mock
  private OBPropertiesProvider mockPropertiesProvider;


  @Mock
  private OBError mockError;

  private ReportValuationStock reportValuationStock;
  private MockedStatic<RequestContext> mockedRequestContext;
  private MockedStatic<OBPropertiesProvider> mockedPropertiesProvider;
  private MockedStatic<OBMessageUtils> mockedMessageUtils;

  /**
   * Sets up the test environment by initializing mocks and static instances.
   */
  @Before
  public void setUp() {
    reportValuationStock = new ReportValuationStock() {
    };

    mockedRequestContext = mockStatic(RequestContext.class);
    mockedPropertiesProvider = mockStatic(OBPropertiesProvider.class);
    mockedMessageUtils = mockStatic(OBMessageUtils.class);

    mockedRequestContext.when(RequestContext::get).thenReturn(mockRequestContext);
    when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);

    mockedPropertiesProvider.when(OBPropertiesProvider::getInstance)
        .thenReturn(mockPropertiesProvider);

    mockedMessageUtils.when(() -> OBMessageUtils.translateError(anyString()))
        .thenReturn(mockError);
  }

  /**
   * Cleans up the mocked static instances after each test.
   */
  @After
  public void tearDown() {
    if (mockedRequestContext != null) {
      mockedRequestContext.close();
    }
    if (mockedPropertiesProvider != null) {
      mockedPropertiesProvider.close();
    }
    if (mockedMessageUtils != null) {
      mockedMessageUtils.close();
    }
  }

  /**
   * Tests the {@code addAdditionalParameters} method for invalid date format.
   *
   * <p>Expects an {@link OBException} to be thrown when the date format is invalid.
   *
   * @throws Exception if the test fails unexpectedly
   */
  @Test(expected = OBException.class)
  public void testAddAdditionalParametersInvalidDate() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    JSONObject jsonContent = new JSONObject();
    JSONObject params = new JSONObject();

    params.put("AD_Org_ID", TestUtils.TEST_ORG_ID);
    params.put("Date", "invalid-date");
    jsonContent.put("_params", params);

    reportValuationStock.addAdditionalParameters(mockProcess, jsonContent, parameters);
  }

  /**
   * Tests the {@code addAdditionalParameters} method for missing required parameters.
   *
   * <p>Expects an {@link OBException} to be thrown when a required parameter is missing.
   *
   * @throws Exception if the test fails unexpectedly
   */
  @Test(expected = OBException.class)
  public void testAddAdditionalParametersMissingRequiredParameter() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    JSONObject jsonContent = new JSONObject();
    JSONObject params = new JSONObject();

    jsonContent.put("_params", params);

    reportValuationStock.addAdditionalParameters(mockProcess, jsonContent, parameters);
  }
}