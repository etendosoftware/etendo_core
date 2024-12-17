package com.etendoerp.reportvaluationstock.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.ReportDefinition;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.JRFieldProviderDataSource;
import org.openbravo.model.common.enterprise.Organization;

import net.sf.jasperreports.engine.JRDataSource;

/**
 * Unit tests for the {@link ReportValuationStock} class.
 * This class uses Mockito to mock dependencies and test different
 * scenarios for the Report Valuation Stock functionality.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReportValuationStockGetReportTest {

  @Mock
  private VariablesSecureApp vars;

  @Mock
  private ConnectionProvider readOnlyCP;

  @Mock
  private Organization filterOrg;

  @Mock
  private ReportDefinition mockProcess;

  @Mock
  private ReportValuationStockData mockData;

  private ReportValuationStock reportValuationStock;
  private Method getReportValuationStockDataMethod;

  private static final String TEST_DATE = "2024-01-01";
  private static final String TEST_CATEGORY = "TEST_CATEGORY";
  private static final String TEST_CURRENCY = "102";
  private static final String TEST_WAREHOUSE = "TEST_WAREHOUSE";
  private static final String TEST_ORG = "TEST_ORG";
  private static final String TEST_CLIENT = "TEST_CLIENT";
  private static final String TEST_LANGUAGE = "en_US";

  private static final String DATE_NEXT = "dateNext";
  private static final String MAX_AGG_DATE = "maxAggDate";
  private static final String DATE_FORMAT = "dateFormat";
  private static final String ORG_IDS = "orgIds";
  private static final String ERROR_RESULT_NULL = "Result should not be null";
  private static final String ERROR_DATA_LENGTH = "Should return expected data length";
  private static final String ERROR_EXPECTED_DATA = "Should return expected data";


  /**
   * Sets up the test environment by initializing the required objects
   * and configuring mock behaviors.
   *
   * @throws Exception if reflection or setup fails
   */
  @Before
  public void setUp() throws Exception {
    reportValuationStock = new ReportValuationStock();

    getReportValuationStockDataMethod = ReportValuationStock.class.getDeclaredMethod(
        "getReportValuationStockData",
        VariablesSecureApp.class, String.class, String.class, String.class,
        boolean.class, String.class, String.class, String.class, String.class,
        ConnectionProvider.class, Organization.class, String.class, String.class,
        String.class, String.class, String.class, String.class, String.class
    );
    getReportValuationStockDataMethod.setAccessible(true);

    when(vars.getLanguage()).thenReturn(TEST_LANGUAGE);
  }

  /**
   * Tests the retrieval of report data with cost type enabled
   * and no warehouse consolidation.
   *
   * @throws Exception if the method invocation or mocks fail
   */
  @Test
  public void testGetReportDataWithCostTypeAndNoWarehouseConsolidation() throws Exception {
    ReportValuationStockData[] expectedData = new ReportValuationStockData[] { mockData };

    try (MockedStatic<ReportValuationStockData> mockedStatic = mockStatic(ReportValuationStockData.class)) {
      mockedStatic.when(() -> ReportValuationStockData.select(
          eq(readOnlyCP), anyString(), anyString(), anyString(),
          anyString(), anyString(), anyString(), anyString(),
          anyString(), anyString(), anyString(), anyString(),
          anyString(), anyString(), anyString(), anyString())
      ).thenReturn(expectedData);

      ReportValuationStockData[] result = (ReportValuationStockData[]) getReportValuationStockDataMethod.invoke(
          reportValuationStock,
          vars, TEST_DATE, TEST_CATEGORY, TEST_CURRENCY, false, "processTime",
          "N", "STA", TEST_WAREHOUSE, readOnlyCP, filterOrg, TEST_ORG,
          ORG_IDS, TEST_ORG, TEST_CLIENT, DATE_NEXT, MAX_AGG_DATE, DATE_FORMAT
      );

      assertNotNull(ERROR_RESULT_NULL, result);
      assertEquals(ERROR_DATA_LENGTH, expectedData.length, result.length);
      assertEquals(ERROR_EXPECTED_DATA, expectedData[0], result[0]);
    }
  }

  /**
   * Tests the retrieval of report data without cost type
   * and with warehouse consolidation enabled.
   *
   * @throws Exception if the method invocation or mocks fail
   */
  @Test
  public void testGetReportDataWithoutCostTypeAndWithWarehouseConsolidation() throws Exception {
    ReportValuationStockData[] expectedData = new ReportValuationStockData[] { mockData };

    try (MockedStatic<ReportValuationStockData> mockedStatic = mockStatic(ReportValuationStockData.class)) {
      mockedStatic.when(() -> ReportValuationStockData.selectClusteredByWarehouseWithoutCost(
          eq(readOnlyCP), anyString(), anyString(), anyString(),
          anyString(), anyString(), anyString(), anyString(),
          anyString(), anyString(), anyString())
      ).thenReturn(expectedData);

      ReportValuationStockData[] result = (ReportValuationStockData[]) getReportValuationStockDataMethod.invoke(
          reportValuationStock,
          vars, TEST_DATE, TEST_CATEGORY, TEST_CURRENCY, true, "processTime",
          "N", null, TEST_WAREHOUSE, readOnlyCP, filterOrg, TEST_ORG,
          ORG_IDS, TEST_ORG, TEST_CLIENT, DATE_NEXT, MAX_AGG_DATE, DATE_FORMAT
      );

      assertNotNull(ERROR_RESULT_NULL, result);
      assertEquals(ERROR_DATA_LENGTH, expectedData.length, result.length);
      assertEquals(ERROR_EXPECTED_DATA, expectedData[0], result[0]);
    }
  }

  /**
   * Tests the retrieval of report data without cost type
   * and no warehouse consolidation.
   *
   * @throws Exception if the method invocation or mocks fail
   */
  @Test
  public void testGetReportDataWithoutCostTypeAndNoWarehouseConsolidation() throws Exception {
    ReportValuationStockData[] expectedData = new ReportValuationStockData[] { mockData };

    try (MockedStatic<ReportValuationStockData> mockedStatic = mockStatic(ReportValuationStockData.class)) {
      mockedStatic.when(() -> ReportValuationStockData.selectWithoutCost(
          eq(readOnlyCP), anyString(), anyString(), anyString(),
          anyString(), anyString(), anyString(), anyString(),
          anyString(), anyString(), anyString())
      ).thenReturn(expectedData);

      ReportValuationStockData[] result = (ReportValuationStockData[]) getReportValuationStockDataMethod.invoke(
          reportValuationStock,
          vars, TEST_DATE, TEST_CATEGORY, TEST_CURRENCY, false, "processTime",
          "N", null, TEST_WAREHOUSE, readOnlyCP, filterOrg, TEST_ORG,
          ORG_IDS, TEST_ORG, TEST_CLIENT, DATE_NEXT, MAX_AGG_DATE, DATE_FORMAT
      );

      assertNotNull(ERROR_RESULT_NULL, result);
      assertEquals(ERROR_DATA_LENGTH, expectedData.length, result.length);
      assertEquals(ERROR_EXPECTED_DATA, expectedData[0], result[0]);
    }
  }

  /**
   * Tests the generation of report data using parameters
   * provided in a map.
   */
  @Test
  public void testGetReportData() {
    Map<String, Object> parameters = new HashMap<>();
    HashMap<String, Object> jasperParams = new HashMap<>();
    parameters.put("JASPER_REPORT_PARAMETERS", jasperParams);

    JRFieldProviderDataSource mockDataSource = mock(JRFieldProviderDataSource.class);
    jasperParams.put("SUMMARY_DATASET", mockDataSource);

    JRDataSource result = reportValuationStock.getReportData(parameters);

    assertNotNull("Should return data source", result);
    assertEquals("Should return correct data source", mockDataSource, result);
  }

  /**
   * Tests the addition of additional parameters to the report with
   * an invalid date format, expecting an {@link OBException}.
   *
   * @throws Exception if JSON manipulation fails
   */
  @Test(expected = OBException.class)
  public void testAddAdditionalParametersInvalidDate() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    JSONObject jsonContent = new JSONObject();
    JSONObject params = new JSONObject();

    params.put("AD_Org_ID", "testOrgId");
    params.put("Date", "invalid-date");
    jsonContent.put("_params", params);

    reportValuationStock.addAdditionalParameters(mockProcess, jsonContent, parameters);
  }
}