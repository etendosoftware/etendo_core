package com.etendoerp.reportvaluationstock.handler;

/**
 * Utility class for testing constants and utility methods related to stock valuation reports.
 *
 * <p>This class provides constant values for testing purposes, including identifiers, messages,
 * formats, and other test-specific configurations. It is designed to be a utility class and
 * should not be instantiated.
 */
public class TestUtils {

  /**
   * Private constructor to prevent instantiation of the utility class.
   *
   * @throws IllegalStateException if an attempt to instantiate is made.
   */
  private TestUtils() {
    throw new IllegalStateException("Utility class");
  }


  public static final String MOCKED_WARNING_MESSAGE = "Mocked warning message";
  public static final String MESSAGE = "message";
  public static final String WARNING = "warning";
  public static final String TRX_WITH_NO_COST = "TrxWithNoCost";
  public static final String TEST_DATE = "2024-01-01";
  public static final String TEST_ORG_ID = "testOrgId";
  public static final String TEST_WAREHOUSE_ID = "testWarehouseId";
  public static final String TEST_CATEGORY_ID = "testCategoryId";
  public static final String TEST_CURRENCY_ID = "testCurrencyId";
  public static final String TEST_CLIENT_ID = "testClientId";
  public static final String WAREHOUSE_NOT_IN_LE = "WarehouseNotInLE";

  public static final String CLIENT_ID = "testClient";
  public static final String ORG_ID = "testOrg";
  public static final String WAREHOUSE_ID_1 = "warehouse1";
  public static final String WAREHOUSE_ID_2 = "warehouse2";

  public static final String ERROR_RESULT_NULL = "Result should not be null";
  public static final String CATEGORY_NAME = "categoryName";
  public static final String TEST_CATEGORY = "TestCategory";
  public static final String TOTAL_COST = "totalCost";
  public static final String TEST_COST_VALUE = "100.00";
  public static final String ERROR_ONE_CATEGORY = "Should contain one category";

  public static final String TEST_COST_TYPE = "AVA";
  public static final String TEST_ALGORITHM_NAME = "Average Algorithm";
  public static final String TEST_TRANSLATED_HEADER = "Translated Cost Header";
  public static final String TEST_TRANSLATED_VALUATION = "Translated Valuation Header";
  public static final String NUMBER_FORMAT = "#,##0.00";
  public static final String ERROR_PARAMETERS_NULL = "Parameters should not be null";
  public static final String NUMBER_FORMAT_KEY = "NUMBERFORMAT";
  public static final String ERROR_DECIMAL_FORMAT = "Should have correct decimal format";
  public static final String COST_FORMAT_KEY = "COSTFORMAT";

  public static final String PROCESS_TIME = "processTime";

  public static final String TEST_CURRENCY = "102";
  public static final String TEST_WAREHOUSE = "TEST_WAREHOUSE";
  public static final String TEST_ORG = "TEST_ORG";
  public static final String TEST_CLIENT = "TEST_CLIENT";
  public static final String TEST_LANGUAGE = "en_US";

  public static final String DATE_NEXT = "dateNext";
  public static final String MAX_AGG_DATE = "maxAggDate";
  public static final String DATE_FORMAT = "dateFormat";
  public static final String ORG_IDS = "orgIds";
  public static final String ERROR_DATA_LENGTH = "Should return expected data length";
  public static final String ERROR_EXPECTED_DATA = "Should return expected data";
}
