package com.smf.jobs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
/** Tests for {@link Filter}. */

@RunWith(MockitoJUnitRunner.class)
public class FilterTest {

  private static final String DEFAULT_BATCH_SIZE = "DEFAULT_BATCH_SIZE";

  private Filter instance;
  private MockedStatic<Preferences> prefStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(Filter.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (prefStatic != null) {
      prefStatic.close();
    }
  }
  /**
   * Get results returns null initially.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetResultsReturnsNullInitially() throws Exception {
    // currentResult is null before hasNext() is called
    assertNull(instance.getResults());
  }
  /**
   * Next page advances current page.
   * @throws Exception if an error occurs
   */

  @Test
  public void testNextPageAdvancesCurrentPage() throws Exception {
    // Set DEFAULT_BATCH_SIZE via static field
    Field batchField = Filter.class.getDeclaredField(DEFAULT_BATCH_SIZE);
    batchField.setAccessible(true);
    batchField.set(null, 1000);

    // Set currentPage to 0
    Field pageField = Filter.class.getDeclaredField("currentPage");
    pageField.setAccessible(true);
    pageField.set(instance, 0);

    instance.nextPage();

    // currentPage should be 0 + 1000 + 1 = 1001
    assertEquals(1001, pageField.get(instance));
  }
  /**
   * Next page advances from non zero page.
   * @throws Exception if an error occurs
   */

  @Test
  public void testNextPageAdvancesFromNonZeroPage() throws Exception {
    Field batchField = Filter.class.getDeclaredField(DEFAULT_BATCH_SIZE);
    batchField.setAccessible(true);
    batchField.set(null, 500);

    Field pageField = Filter.class.getDeclaredField("currentPage");
    pageField.setAccessible(true);
    pageField.set(instance, 100);

    instance.nextPage();

    // currentPage should be 100 + 500 + 1 = 601
    assertEquals(601, pageField.get(instance));
  }
  /**
   * Constructor sets entity and rsql.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConstructorSetsEntityAndRsql() throws Exception {
    // Mock Preferences to avoid DB call in constructor
    prefStatic = mockStatic(Preferences.class);
    prefStatic.when(() -> Preferences.getPreferenceValue("Filter_Batch_Size", true,
        (String) null, null, null, null, null))
        .thenThrow(new PropertyException("Not found"));

    Filter filter = new Filter("ADUser", "name==John");

    Field entityField = Filter.class.getDeclaredField("entityName");
    entityField.setAccessible(true);
    assertEquals("ADUser", entityField.get(filter));

    Field rsqlField = Filter.class.getDeclaredField("rsql");
    rsqlField.setAccessible(true);
    assertEquals("name==John", rsqlField.get(filter));
  }
  /**
   * Constructor with valid batch size.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConstructorWithValidBatchSize() throws Exception {
    prefStatic = mockStatic(Preferences.class);
    prefStatic.when(() -> Preferences.getPreferenceValue("Filter_Batch_Size", true,
        (String) null, null, null, null, null))
        .thenReturn("2000");


    Field batchField = Filter.class.getDeclaredField(DEFAULT_BATCH_SIZE);
    batchField.setAccessible(true);
    assertEquals(2000, batchField.get(null));

    // Reset to default
    batchField.set(null, 1000);
  }
}
