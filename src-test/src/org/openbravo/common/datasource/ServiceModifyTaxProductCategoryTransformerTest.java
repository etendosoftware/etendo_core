package org.openbravo.common.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link ServiceModifyTaxProductCategoryTransformer} class.
 * Verifies the behavior of the HQL transformation method under different scenarios.
 */
public class ServiceModifyTaxProductCategoryTransformerTest {

  /**
   * Instance of the transformer being tested.
   */
  private ServiceModifyTaxProductCategoryTransformer transformer;

  /**
   * Sets up the test environment before each test.
   * Initializes the transformer instance.
   */
  @BeforeEach
  public void setUp() {
    transformer = new ServiceModifyTaxProductCategoryTransformer();
  }

  /**
   * Tests the {@code transformHqlQuery} method when the tab ID matches the expected value.
   * Verifies that the HQL query is transformed correctly and the named parameters are set.
   */
  @Test
  public void testTransformHqlQueryWithMatchingTabId() {
    String hqlQuery = "SELECT * FROM Product WHERE @insertion_point@";
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("tabId", "B51960EFD3E04B79917A1277C751232F");
    requestParameters.put("@Product.id@", "12345");

    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    String expectedQuery = "SELECT * FROM Product WHERE  not exists (select sl.id " + "             from M_PRODUCT_SERVICELINKED sl " + "             where sl.productCategory.id = cat.id " + "             and sl.product.id = :productId)";
    assertEquals(expectedQuery, result, "The transformed query should match the expected query");
    assertEquals("12345", queryNamedParameters.get("productId"), "The productId parameter should be set");
  }

  /**
   * Tests the {@code transformHqlQuery} method when the tab ID does not match the expected value.
   * Verifies that the HQL query remains unchanged and no named parameters are set.
   */
  @Test
  public void testTransformHqlQueryWithNonMatchingTabId() {
    String hqlQuery = "SELECT * FROM Product WHERE @insertion_point@";
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("tabId", "NON_MATCHING_TAB_ID");

    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    assertEquals(hqlQuery, result, "The query should remain unchanged");
    assertEquals(0, queryNamedParameters.size(), "The named parameters should remain empty");
  }
}
