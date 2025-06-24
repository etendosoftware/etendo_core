package org.openbravo.materialmgmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.VariantChDescUpdateProcessor.VariantChDescUpdateRunnable;
import org.openbravo.service.importprocess.ImportEntry;
import org.openbravo.service.importprocess.ImportEntryManager;

/**
 * Unit tests for {@link VariantChDescUpdateProcessor} class.
 * This class verifies the behavior of the VariantChDescUpdateProcessor and its inner class
 * VariantChDescUpdateRunnable using mock dependencies and static method mocking.
 */
@RunWith(MockitoJUnitRunner.class)
public class VariantChDescUpdateProcessorTest {

  private static final String TEST_PRODUCT_ID = "TEST_PRODUCT_ID";
  private static final String JSON_PRODUCT_IDS_PREFIX = "{ \"productIds\": [\"";
  private static final String JSON_PRODUCT_IDS_SUFFIX = "\"] }";
  private static final String TEST_ENTRY_ID = "TEST_ENTRY_ID";
  private static final String VARIANT_CH_DESC_UPDATE = "VariantChDescUpdate";

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<WeldUtils> mockedWeldUtils;
  private MockedStatic<SessionHandler> mockedSessionHandler;
  private MockedStatic<ImportEntryManager> mockedImportEntryManager;

  @Mock
  private ImportEntry mockImportEntry;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Session mockSession;

  @Mock
  private VariantChDescUpdateProcess mockUpdateProcess;

  @Mock
  private ImportEntryManager mockImportEntryManager;

  @InjectMocks
  private VariantChDescUpdateProcessor processor;

  private VariantChDescUpdateRunnable runnable;

  /**
   * Sets up the required mock objects and static method mocks before each test.
   */
  @Before
  public void setUp() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedWeldUtils = mockStatic(WeldUtils.class);
    mockedSessionHandler = mockStatic(SessionHandler.class);
    mockedImportEntryManager = mockStatic(ImportEntryManager.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    when(mockOBDal.getSession()).thenReturn(mockSession);

    mockedImportEntryManager.when(ImportEntryManager::getInstance).thenReturn(mockImportEntryManager);

    runnable = new VariantChDescUpdateProcessor.VariantChDescUpdateRunnable();
  }

  /**
   * Cleans up static mocks after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedWeldUtils != null) {
      mockedWeldUtils.close();
    }
    if (mockedSessionHandler != null) {
      mockedSessionHandler.close();
    }
    if (mockedImportEntryManager != null) {
      mockedImportEntryManager.close();
    }
  }

  /**
   * Verifies that the processor can handle an ImportEntry with type 'VariantChDescUpdate'.
   */
  @Test
  public void testCanHandleImportEntryValidEntryType() {
    // GIVEN
    when(mockImportEntry.getTypeofdata()).thenReturn(VARIANT_CH_DESC_UPDATE);

    // WHEN
    boolean canHandle = processor.canHandleImportEntry(mockImportEntry);

    // THEN
    assertTrue("Processor should handle 'VariantChDescUpdate' type", canHandle);
  }

  /**
   * Verifies that the processor cannot handle an ImportEntry with an unsupported type.
   */
  @Test
  public void testCanHandleImportEntryInvalidEntryType() {
    // GIVEN
    when(mockImportEntry.getTypeofdata()).thenReturn("OtherType");

    // WHEN
    boolean canHandle = processor.canHandleImportEntry(mockImportEntry);

    // THEN
    assertFalse("Processor should not handle types other than 'VariantChDescUpdate'", canHandle);
  }

  /**
   * Ensures that the processor retrieves the correct process selection key.
   */
  @Test
  public void testGetProcessSelectionKey() {
    // GIVEN
    when(mockImportEntry.getTypeofdata()).thenReturn(VARIANT_CH_DESC_UPDATE);

    // WHEN
    String key = processor.getProcessSelectionKey(mockImportEntry);

    // THEN
    assertEquals("Key should be the type of data", VARIANT_CH_DESC_UPDATE, key);
  }

  /**
   * Validates the creation of a VariantChDescUpdateRunnable instance using WeldUtils.
   */
  @Test
  public void testCreateImportEntryProcessRunnable() {
    // GIVEN
    VariantChDescUpdateRunnable mockRunnable = mock(VariantChDescUpdateRunnable.class);
    mockedWeldUtils.when(
        () -> WeldUtils.getInstanceFromStaticBeanManager(VariantChDescUpdateRunnable.class)).thenReturn(mockRunnable);

    // WHEN
    Object importEntryRunnable = processor.createImportEntryProcessRunnable();

    // THEN
    assertEquals("Should return the correct runnable instance", mockRunnable, importEntryRunnable);
    mockedWeldUtils.verify(() -> WeldUtils.getInstanceFromStaticBeanManager(VariantChDescUpdateRunnable.class));
  }

  /**
   * Tests the processing of an ImportEntry with a single product.
   * This test verifies that the process correctly handles an entry containing
   * one product, ensuring all operations are performed as expected.
   *
   * @throws Exception if any unexpected exception occurs during the test.
   */
  @Test
  public void testProcessEntryWithSingleProduct() throws Exception {
    // GIVEN
    String jsonInfoStr = JSON_PRODUCT_IDS_PREFIX + TEST_PRODUCT_ID + JSON_PRODUCT_IDS_SUFFIX;

    when(mockImportEntry.getId()).thenReturn(TEST_ENTRY_ID);
    when(mockImportEntry.getJsonInfo()).thenReturn(jsonInfoStr);

    mockedWeldUtils.when(() -> WeldUtils.getInstanceFromStaticBeanManager(VariantChDescUpdateProcess.class)).thenReturn(
        mockUpdateProcess);

    mockedSessionHandler.when(SessionHandler::isSessionHandlerPresent).thenReturn(true);

    // WHEN
    runnable.processEntry(mockImportEntry);

    // THEN
    mockedOBContext.verify(() -> OBContext.setAdminMode(true));
    mockedOBContext.verify(OBContext::restorePreviousMode);

    verify(mockUpdateProcess).update(eq(TEST_PRODUCT_ID), isNull());
    verify(mockOBDal).flush();
    verify(mockSession).clear();
    verify(mockImportEntryManager).setImportEntryProcessed(TEST_ENTRY_ID);
    verify(mockOBDal).commitAndClose();
  }

  /**
   * Tests the processing of an ImportEntry with multiple products.
   * This test ensures that when an ImportEntry contains multiple product IDs,
   * the process updates each product individually and performs the necessary
   * operations for all products.
   *
   * @throws Exception if any unexpected exception occurs during the test.
   */
  @Test
  public void testProcessEntryWithMultipleProducts() throws Exception {
    // GIVEN
    String product1Id = "PRODUCT_1_ID";
    String product2Id = "PRODUCT_2_ID";
    String jsonInfoStr = JSON_PRODUCT_IDS_PREFIX + product1Id + "\", \"" + product2Id + JSON_PRODUCT_IDS_SUFFIX;

    when(mockImportEntry.getId()).thenReturn(TEST_ENTRY_ID);
    when(mockImportEntry.getJsonInfo()).thenReturn(jsonInfoStr);

    mockedWeldUtils.when(() -> WeldUtils.getInstanceFromStaticBeanManager(VariantChDescUpdateProcess.class)).thenReturn(
        mockUpdateProcess);

    mockedSessionHandler.when(SessionHandler::isSessionHandlerPresent).thenReturn(true);

    // WHEN
    runnable.processEntry(mockImportEntry);

    // THEN
    mockedOBContext.verify(() -> OBContext.setAdminMode(true));
    mockedOBContext.verify(OBContext::restorePreviousMode);

    verify(mockUpdateProcess).update(eq(product1Id), isNull());
    verify(mockUpdateProcess).update(eq(product2Id), isNull());
    verify(mockOBDal, times(2)).flush();
    verify(mockSession, times(2)).clear();
    verify(mockImportEntryManager).setImportEntryProcessed(TEST_ENTRY_ID);
    verify(mockOBDal).commitAndClose();
  }

  /**
   * Verifies the behavior when an exception is thrown during the update process.
   * This test ensures that when an exception occurs during the update operation,
   * the context is restored, and the entry is marked as processed.
   *
   * @throws Exception if any unexpected exception occurs during the test.
   */
  @Test
  public void testProcessEntryWithExceptionInUpdate() throws Exception {
    // GIVEN
    String jsonInfoStr = JSON_PRODUCT_IDS_PREFIX + TEST_PRODUCT_ID + JSON_PRODUCT_IDS_SUFFIX;

    when(mockImportEntry.getId()).thenReturn(TEST_ENTRY_ID);
    when(mockImportEntry.getJsonInfo()).thenReturn(jsonInfoStr);

    mockedWeldUtils.when(() -> WeldUtils.getInstanceFromStaticBeanManager(VariantChDescUpdateProcess.class)).thenReturn(
        mockUpdateProcess);

    doThrow(new RuntimeException("Test exception")).when(mockUpdateProcess).update(eq(TEST_PRODUCT_ID), isNull());

    mockedSessionHandler.when(SessionHandler::isSessionHandlerPresent).thenReturn(true);

    // WHEN
    runnable.processEntry(mockImportEntry);

    // THEN
    mockedOBContext.verify(() -> OBContext.setAdminMode(true));
    mockedOBContext.verify(OBContext::restorePreviousMode);

    verify(mockUpdateProcess).update(eq(TEST_PRODUCT_ID), isNull());
    verify(mockImportEntryManager).setImportEntryProcessed(TEST_ENTRY_ID);
    verify(mockOBDal).commitAndClose();
  }

  /**
   * Ensures that invalid JSON info in an ImportEntry triggers an exception.
   */
  @Test
  public void testProcessEntryWithInvalidJsonInfo() {
    // GIVEN
    String invalidJsonInfoStr = "{ invalid_json }";

    when(mockImportEntry.getJsonInfo()).thenReturn(invalidJsonInfoStr);

    // WHEN & THEN
    try {
      runnable.processEntry(mockImportEntry);
      fail("Should have thrown an exception for invalid JSON");
    } catch (Exception e) {
      // Expected exception
      mockedOBContext.verify(() -> OBContext.setAdminMode(true));
      mockedOBContext.verify(OBContext::restorePreviousMode);
    }
  }
}
