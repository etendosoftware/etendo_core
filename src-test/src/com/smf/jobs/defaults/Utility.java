package com.smf.jobs.defaults;

/**
 * Utility class that provides constant values and prevents instantiation.
 *
 * <p>This class contains constants used across various parts of the application, such as
 * status codes, messages, and SQL queries. It is designed to be a utility class and should
 * not be instantiated.
 */
public class Utility {

  /**
   * Private constructor to prevent instantiation of the utility class.
   *
   * @throws IllegalStateException if an attempt to instantiate is made.
   */
  private Utility() {
    throw new IllegalStateException("Utility class");
  }

  public static final String SUCCESS = "Success";
  public static final String TEST_MESSAGE = "Test message";
  public static final String SHIPMENT_PROCESSED_SUCCESSFULLY = "Shipment processed successfully";
  public static final String RESULT_SHOULD_NOT_BE_NULL = "Result should not be null";
  public static final String SHOULD_RETURN_SUCCESS_TYPE = "Should return success type";
  public static final String DOC_ACTION = "DocAction";

  public static final String TEST_ID = "testId" ;

  static final String DRAFT_STATUS = "DR";

  public static final String COMPLETE = "CO";
  public static final String ACTIONS = "actions";

  public static final String QUERY_ORDERS_WITHOUT_TAX = "SELECT * FROM Orders WHERE includeTax = 'N'";
  public static final String LINES_INCLUDE_TAXES = "linesIncludeTaxes";
  public static final String TRUE = "true";

  public static final String POSTED = "posted";
  public static final String ORGANIZATION = "organization";
  public static final String CLIENT = "client";
  public static final String TEST_ORG_ID = "testOrgId";
  public static final String SUCCESS_CAPITALIZED = "Success";
  public static final String ERROR = "Error";
  public static final String TABLE_NOT_FOUND = "TableNotFound";

  public static final String SELECTION = "_selection";
  public static final String TEST_JSON = "testJSON";
  public static final String NOT_SELECTED = "NotSelected";

}
