package com.etendoerp.reportvaluationstock.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.materialmgmt.cost.CostingAlgorithm;

/**
 * Unit tests for the {@link ReportValuationStock} class.
 * <p>
 * This class validates the behavior of the {@code printReport} method, ensuring it correctly
 * formats parameters, handles costing algorithms, and processes custom number formats.
 * </p>
 */

@RunWith(MockitoJUnitRunner.class)
public class ReportValuationStockPrintTest {

  @InjectMocks
  private ReportValuationStock reportValuationStock;

  @Mock
  private VariablesSecureApp vars;

  @Mock
  private CostingAlgorithm costingAlgorithm;

  private Method printReportMethod;



  /**
   * Sets up the test environment by initializing mocks and reflective access to the method under test.
   *
   * @throws Exception if the method cannot be accessed.
   */

  @Before
  public void setUp() throws Exception {
    printReportMethod = ReportValuationStock.class.getDeclaredMethod(
        "printReport",
        VariablesSecureApp.class,
        String.class,
        ReportValuationStockData[].class,
        String.class,
        CostingAlgorithm.class,
        Map.class
    );
    printReportMethod.setAccessible(true);

    when(vars.getSessionValue("#AD_ReportDecimalSeparator")).thenReturn(".");
    when(vars.getSessionValue("#AD_ReportGroupingSeparator")).thenReturn(",");
    when(vars.getSessionValue("#AD_ReportNumberFormat")).thenReturn(TestUtils.NUMBER_FORMAT);
    when(vars.getJavaDateFormat()).thenReturn("yyyy-MM-dd");

    when(costingAlgorithm.getName()).thenReturn(TestUtils.TEST_ALGORITHM_NAME);
  }

  /**
   * Tests the {@code printReport} method with a defined costing algorithm.
   * <p>
   * Validates that the method correctly processes headers and parameters
   * when a costing algorithm is provided.
   * </p>
   *
   * @throws Exception if the method invocation fails.
   */

  @Test
  public void testPrintReportWithCostingAlgorithm() throws Exception {
    ReportValuationStockData[] testData = new ReportValuationStockData[0];
    Map<String, Object> parameters = new HashMap<>();
    DecimalFormat mockFormat = new DecimalFormat(TestUtils.NUMBER_FORMAT);

    try (MockedStatic<OBMessageUtils> obMessageUtilsMock = mockStatic(OBMessageUtils.class);
         MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {

      obMessageUtilsMock.when(() -> OBMessageUtils.messageBD("ValuedStockReport_CostHeader"))
          .thenReturn("Cost Header @algorithm@");
      obMessageUtilsMock.when(() -> OBMessageUtils.messageBD("ValuedStockReport_ValuationHeader"))
          .thenReturn("Valuation Header @algorithm@");
      obMessageUtilsMock.when(() -> OBMessageUtils.parseTranslation(anyString(), any()))
          .thenReturn(TestUtils.TEST_TRANSLATED_HEADER)
          .thenReturn(TestUtils.TEST_TRANSLATED_VALUATION);

      utilityMock.when(() -> Utility.getFormat(any(), anyString())).thenReturn(mockFormat);

      printReportMethod.invoke(
          reportValuationStock,
          vars,
          TestUtils.TEST_DATE,
          testData,
          TestUtils.TEST_COST_TYPE,
          costingAlgorithm,
          parameters
      );

      assertNotNull(TestUtils.ERROR_PARAMETERS_NULL, parameters);
      assertEquals("Should have correct cost header",
          TestUtils.TEST_TRANSLATED_HEADER, parameters.get("ALG_COST"));
      assertEquals("Should have correct valuation header",
          TestUtils.TEST_TRANSLATED_VALUATION, parameters.get("SUM_ALG_COST"));
      assertEquals("Should have correct title",
          "Valued Stock Report", parameters.get("TITLE"));
      assertEquals("Should have correct date",
          TestUtils.TEST_DATE, parameters.get("DATE"));
      assertNotNull("Should have number format",
          parameters.get(TestUtils.NUMBER_FORMAT_KEY));
      assertEquals(TestUtils.ERROR_DECIMAL_FORMAT,
          mockFormat, parameters.get(TestUtils.COST_FORMAT_KEY));
    }
  }

  /**
   * Tests the {@code printReport} method without a costing algorithm.
   * <p>
   * Confirms that the method correctly processes headers and parameters
   * when no costing algorithm is provided.
   * </p>
   *
   * @throws Exception if the method invocation fails.
   */

  @Test
  public void testPrintReportWithoutCostingAlgorithm() throws Exception {
    ReportValuationStockData[] testData = new ReportValuationStockData[0];
    Map<String, Object> parameters = new HashMap<>();
    DecimalFormat mockFormat = new DecimalFormat(TestUtils.NUMBER_FORMAT);

    try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
      utilityMock.when(() -> Utility.getFormat(any(), anyString())).thenReturn(mockFormat);

      printReportMethod.invoke(
          reportValuationStock,
          vars,
          TestUtils.TEST_DATE,
          testData,
          null,
          null,
          parameters
      );

      assertNotNull(TestUtils.ERROR_PARAMETERS_NULL, parameters);
      assertEquals("Should have empty cost header",
          "", parameters.get("ALG_COST"));
      assertEquals("Should have empty valuation header",
          "", parameters.get("SUM_ALG_COST"));
      assertEquals("Should have correct title",
          "Valued Stock Report", parameters.get("TITLE"));
      assertEquals("Should have correct date",
          TestUtils.TEST_DATE, parameters.get("DATE"));
      assertNotNull("Should have number format",
          parameters.get(TestUtils.NUMBER_FORMAT_KEY));
      assertEquals(TestUtils.ERROR_DECIMAL_FORMAT,
          mockFormat, parameters.get(TestUtils.COST_FORMAT_KEY));
    }
  }

  /**
   * Tests the {@code printReport} method with custom number formats and separators.
   * <p>
   * Ensures that the method correctly applies custom formatting settings for decimal
   * and grouping separators.
   * </p>
   *
   * @throws Exception if the method invocation fails.
   */

  @Test
  public void testPrintReportWithCustomFormats() throws Exception {
    ReportValuationStockData[] testData = new ReportValuationStockData[0];
    Map<String, Object> parameters = new HashMap<>();
    DecimalFormat mockFormat = new DecimalFormat(TestUtils.NUMBER_FORMAT);

    when(vars.getSessionValue("#AD_ReportDecimalSeparator")).thenReturn(",");
    when(vars.getSessionValue("#AD_ReportGroupingSeparator")).thenReturn(".");
    when(vars.getSessionValue("#AD_ReportNumberFormat")).thenReturn(TestUtils.NUMBER_FORMAT);

    try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
      utilityMock.when(() -> Utility.getFormat(any(), anyString())).thenReturn(mockFormat);

      printReportMethod.invoke(
          reportValuationStock,
          vars,
          TestUtils.TEST_DATE,
          testData,
          TestUtils.TEST_COST_TYPE,
          null,
          parameters
      );

      assertNotNull(TestUtils.ERROR_PARAMETERS_NULL, parameters);
      assertNotNull("Should have number format with custom separators",
          parameters.get(TestUtils.NUMBER_FORMAT_KEY));
      assertEquals(TestUtils.ERROR_DECIMAL_FORMAT,
          mockFormat, parameters.get(TestUtils.COST_FORMAT_KEY));
    }
  }
}