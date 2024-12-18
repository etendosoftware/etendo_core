package com.etendoerp.reportvaluationstock.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.data.FieldProvider;

/**
 * Unit tests for the {@link ReportValuationStock} class.
 * <p>
 * This class validates the behavior of the {@code getSummaryProductCategories} method
 * in various scenarios, including handling empty data, single category, multiple entries
 * in the same category, and null costs.
 * </p>
 */

@RunWith(MockitoJUnitRunner.class)
public class ReportValuationStockSummaryTest {

  @InjectMocks
  private ReportValuationStock reportValuationStock;

  private Method getSummaryProductCategoriesMethod;




  /**
   * Sets up the test environment by initializing reflective access to the method under test.
   *
   * @throws Exception if the method cannot be accessed.
   */
  @Before
  public void setUp() throws Exception {
    getSummaryProductCategoriesMethod = ReportValuationStock.class.getDeclaredMethod(
        "getSummaryProductCategories",
        ReportValuationStockData[].class
    );
    getSummaryProductCategoriesMethod.setAccessible(true);
  }

  /**
   * Sets the value of a field in the target object using reflection.
   *
   * @param target    the target object whose field is to be modified
   * @param fieldName the name of the field to set
   * @param value     the value to assign to the field
   * @throws Exception if the field cannot be accessed or set
   */

  private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getField(fieldName);
    field.set(target, value);
  }

  /**
   * Tests the {@code getSummaryProductCategories} method with empty input data.
   * <p>
   * Ensures that the method returns an empty result when provided with no data.
   * </p>
   *
   * @throws Exception if the method invocation fails.
   */

  @Test
  public void testGetSummaryProductCategoriesWithEmptyData() throws Exception {
    ReportValuationStockData[] data = new ReportValuationStockData[0];

    FieldProvider[] result = (FieldProvider[]) getSummaryProductCategoriesMethod.invoke(
        reportValuationStock,
        (Object) data
    );

    assertNotNull(TestUtils.ERROR_RESULT_NULL, result);
    assertEquals("Empty data should return empty result", 0, result.length);
  }

  /**
   * Tests the {@code getSummaryProductCategories} method with a single data entry.
   * <p>
   * Validates that the method correctly processes and returns data for one category.
   * </p>
   *
   * @throws Exception if the method invocation fails.
   */

  @Test
  public void testGetSummaryProductCategoriesWithSingleCategory() throws Exception {
    ReportValuationStockData singleData = new ReportValuationStockData();
    setFieldValue(singleData, TestUtils.CATEGORY_NAME, TestUtils.TEST_CATEGORY);
    setFieldValue(singleData, TestUtils.TOTAL_COST, TestUtils.TEST_COST_VALUE);

    ReportValuationStockData[] data = new ReportValuationStockData[] { singleData };

    FieldProvider[] result = (FieldProvider[]) getSummaryProductCategoriesMethod.invoke(
        reportValuationStock,
        (Object) data
    );

    assertNotNull(TestUtils.ERROR_RESULT_NULL, result);
    assertEquals(TestUtils.ERROR_ONE_CATEGORY, 1, result.length);
    assertEquals("Should have correct category name", TestUtils.TEST_CATEGORY,
        result[0].getField("category"));
    assertEquals("Should have correct cost", TestUtils.TEST_COST_VALUE,
        result[0].getField("cost"));
  }

  /**
   * Tests the {@code getSummaryProductCategories} method with multiple entries in the same category.
   * <p>
   * Verifies that the method aggregates costs correctly for a single category.
   * </p>
   *
   * @throws Exception if the method invocation fails.
   */

  @Test
  public void testGetSummaryProductCategoriesWithMultipleEntriesSameCategory() throws Exception {
    ReportValuationStockData data1 = new ReportValuationStockData();
    setFieldValue(data1, TestUtils.CATEGORY_NAME, TestUtils.TEST_CATEGORY);
    setFieldValue(data1, TestUtils.TOTAL_COST, TestUtils.TEST_COST_VALUE);

    ReportValuationStockData data2 = new ReportValuationStockData();
    setFieldValue(data2, TestUtils.CATEGORY_NAME, TestUtils.TEST_CATEGORY);
    setFieldValue(data2, TestUtils.TOTAL_COST, "50.00");

    ReportValuationStockData[] data = new ReportValuationStockData[] { data1, data2 };

    FieldProvider[] result = (FieldProvider[]) getSummaryProductCategoriesMethod.invoke(
        reportValuationStock,
        (Object) data
    );

    assertNotNull(TestUtils.ERROR_RESULT_NULL, result);
    assertEquals(TestUtils.ERROR_ONE_CATEGORY, 1, result.length);
    assertEquals("Category name should match", TestUtils.TEST_CATEGORY,
        result[0].getField("category"));
    assertEquals("Total cost should be summed correctly", "150.00",
        result[0].getField("cost"));
  }

  /**
   * Tests the {@code getSummaryProductCategories} method with null cost values.
   * <p>
   * Confirms that the method handles null costs by treating them as zero.
   * </p>
   *
   * @throws Exception if the method invocation fails.
   */

  @Test
  public void testGetSummaryProductCategoriesWithNullCosts() throws Exception {
    ReportValuationStockData nullCostData = new ReportValuationStockData();
    setFieldValue(nullCostData, TestUtils.CATEGORY_NAME, TestUtils.TEST_CATEGORY);
    setFieldValue(nullCostData, TestUtils.TOTAL_COST, null);

    ReportValuationStockData[] data = new ReportValuationStockData[] { nullCostData };

    FieldProvider[] result = (FieldProvider[]) getSummaryProductCategoriesMethod.invoke(
        reportValuationStock,
        (Object) data
    );

    assertNotNull(TestUtils.ERROR_RESULT_NULL, result);
    assertEquals(TestUtils.ERROR_ONE_CATEGORY, 1, result.length);
    assertEquals("Cost should be zero for null values", "0",
        result[0].getField("cost"));
  }
}