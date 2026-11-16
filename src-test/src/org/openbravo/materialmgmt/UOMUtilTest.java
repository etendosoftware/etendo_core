package org.openbravo.materialmgmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Criterion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.plm.ProductUOM;
import org.openbravo.model.common.uom.UOM;

/**
 * Unit tests for the {@link UOMUtil} class.
 * This class tests the utility methods for managing Units of Measure (UOM).
 */
@RunWith(MockitoJUnitRunner.class)
public class UOMUtilTest {

  private static final String RESULT_NOT_NULL = "The result should not be null";
  private static final String SHOULD_BE_EMPTY_ARRAY = "Should be an empty array";
  private static final String TEST_PRODUCT_ID = "TEST_PRODUCT_ID";
  private static final String TEST_DOCTYPE_ID = "TEST_DOCTYPE_ID";
  private static final String TEST_UOM_ID = "TEST_UOM_ID";
  private static final String TEST_UOM_NAME = "Test UOM";
  private static final String UOM_PROPERTY = "UomManagement";
  private static final String TEST_CLIENT_ID = "TEST_CLIENT_ID";
  private static final String SYSTEM_CLIENT_ID = "0";
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<FieldProviderFactory> mockedFieldProviderFactory;
  private MockedStatic<UOMUtil> mockedUOMUtil;
  private OBDal mockDal;
  private OBCriteria<ProductUOM> mockCriteria;

  /**
   * The call UOMUtil makes to read the UomManagement preference, with matchers for a lookup by
   * client only.
   */
  private static String preferenceLookup(String clientId) throws Exception {
    return Preferences.getPreferenceValue(eq(UOM_PROPERTY), eq(true), eq(clientId), ArgumentMatchers.isNull(),
        ArgumentMatchers.isNull(), ArgumentMatchers.isNull(), ArgumentMatchers.isNull());
  }

  /**
   * Sets up the mock environment before each test.
   * Initializes mocked static classes and mock objects for dependencies.
   */
  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBDal = mockStatic(OBDal.class);
    mockedFieldProviderFactory = mockStatic(FieldProviderFactory.class);
    mockedUOMUtil = mockStatic(UOMUtil.class, Mockito.CALLS_REAL_METHODS);

    mockDal = Mockito.mock(OBDal.class);
    OBContext mockContext = Mockito.mock(OBContext.class);
    mockCriteria = Mockito.mock(OBCriteria.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockDal);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockContext);

    mockedOBContext.when(() -> OBContext.setAdminMode(anyBoolean())).thenAnswer(invocation -> null);
    mockedOBContext.when(OBContext::restorePreviousMode).thenAnswer(invocation -> null);

    mockedFieldProviderFactory.when(() -> FieldProviderFactory.getFieldProviderArray(any(List.class))).thenAnswer(
        invocation -> {
          List<Map<String, String>> list = invocation.getArgument(0);
          return new FieldProvider[list.size()];
        });
  }

  /**
   * Cleans up the mock environment after each test.
   * Closes any mocked static classes.
   */
  @After
  public void tearDown() {
    if (mockedOBContext != null) mockedOBContext.close();
    if (mockedOBDal != null) mockedOBDal.close();
    if (mockedFieldProviderFactory != null) mockedFieldProviderFactory.close();
    if (mockedUOMUtil != null) mockedUOMUtil.close();
  }

  /**
   * Tests the {@link UOMUtil#selectDefaultAUM(String, String)} method for a successful scenario.
   */
  @Test
  public void testSelectDefaultAUMSuccess() {
    UOM mockUOM = Mockito.mock(UOM.class);
    FieldProvider[] mockFieldProviders = new FieldProvider[1];

    when(mockUOM.getName()).thenReturn(TEST_UOM_NAME);
    when(mockDal.get(UOM.class, TEST_UOM_ID)).thenReturn(mockUOM);

    mockedUOMUtil.when(() -> UOMUtil.getDefaultAUMForDocument(TEST_PRODUCT_ID, TEST_DOCTYPE_ID)).thenReturn(
        TEST_UOM_ID);

    mockedFieldProviderFactory.when(() -> FieldProviderFactory.getFieldProviderArray(any(List.class))).thenReturn(
        mockFieldProviders);

    FieldProvider[] result = UOMUtil.selectDefaultAUM(TEST_PRODUCT_ID, TEST_DOCTYPE_ID);

    assertNotNull(RESULT_NOT_NULL, result);
    assertEquals("Should be of the expected size", mockFieldProviders.length, result.length);

    mockedOBContext.verify(OBContext::restorePreviousMode, Mockito.atLeastOnce());
  }

  /**
   * Tests the {@link UOMUtil#selectDefaultAUM(String, String)} method with null parameters.
   */
  @Test
  public void testSelectDefaultAUMNullParameters() {
    FieldProvider[] emptyArray = new FieldProvider[0];
    mockedFieldProviderFactory.when(() -> FieldProviderFactory.getFieldProviderArray(any(List.class))).thenReturn(
        emptyArray);

    FieldProvider[] result = UOMUtil.selectDefaultAUM(null, null);

    assertNotNull(RESULT_NOT_NULL, result);
    assertEquals(SHOULD_BE_EMPTY_ARRAY, 0, result.length);

    mockedOBContext.verify(() -> OBContext.setAdminMode(anyBoolean()), Mockito.never());
  }

  /**
   * Tests the {@link UOMUtil#selectDefaultAUM(String, String)} method with empty string parameters.
   */
  @Test
  public void testSelectDefaultAUMEmptyParameters() {
    FieldProvider[] emptyArray = new FieldProvider[0];
    mockedFieldProviderFactory.when(() -> FieldProviderFactory.getFieldProviderArray(any(List.class))).thenReturn(
        emptyArray);

    FieldProvider[] result = UOMUtil.selectDefaultAUM("", "");

    assertNotNull(RESULT_NOT_NULL, result);
    assertEquals(SHOULD_BE_EMPTY_ARRAY, 0, result.length);
  }

  /**
   * Tests the {@link UOMUtil#selectAUM(String, String)} method for a successful scenario.
   */
  @Test
  public void testSelectAUMSuccess() {
    List<UOM> mockUOMList = new ArrayList<>();
    UOM mockUOM1 = Mockito.mock(UOM.class);
    UOM mockUOM2 = Mockito.mock(UOM.class);
    mockUOMList.add(mockUOM1);
    mockUOMList.add(mockUOM2);

    FieldProvider[] expectedFieldProviders = new FieldProvider[2];

    when(mockUOM1.getId()).thenReturn("UOM_ID_1");
    when(mockUOM1.getName()).thenReturn("UOM Name 1");
    when(mockUOM2.getId()).thenReturn("UOM_ID_2");
    when(mockUOM2.getName()).thenReturn("UOM Name 2");

    mockedUOMUtil.when(() -> UOMUtil.getAvailableUOMsForDocument(TEST_PRODUCT_ID, TEST_DOCTYPE_ID)).thenReturn(
        mockUOMList);

    mockedFieldProviderFactory.when(() -> FieldProviderFactory.getFieldProviderArray(any(List.class))).thenReturn(
        expectedFieldProviders);

    FieldProvider[] result = UOMUtil.selectAUM(TEST_PRODUCT_ID, TEST_DOCTYPE_ID);

    assertNotNull(RESULT_NOT_NULL, result);
    assertEquals("Should contain an array of the expected size", expectedFieldProviders.length, result.length);

  }

  /**
   * Tests the {@link UOMUtil#selectAUM(String, String)} method with null parameters.
   */
  @Test
  public void testSelectAUMNullParameters() {
    FieldProvider[] emptyArray = new FieldProvider[0];
    mockedFieldProviderFactory.when(() -> FieldProviderFactory.getFieldProviderArray(any(List.class))).thenReturn(
        emptyArray);

    FieldProvider[] result = UOMUtil.selectAUM(null, null);

    assertNotNull(RESULT_NOT_NULL, result);
    assertEquals(SHOULD_BE_EMPTY_ARRAY, 0, result.length);
  }

  /**
   * Tests the {@link UOMUtil#selectAUM(String, String)} method with empty string parameters.
   */
  @Test
  public void testSelectAUMEmptyParameters() {
    FieldProvider[] emptyArray = new FieldProvider[0];
    mockedFieldProviderFactory.when(() -> FieldProviderFactory.getFieldProviderArray(any(List.class))).thenReturn(
        emptyArray);

    FieldProvider[] result = UOMUtil.selectAUM("", "");

    assertNotNull(RESULT_NOT_NULL, result);
    assertEquals(SHOULD_BE_EMPTY_ARRAY, 0, result.length);
  }

  /**
   * Tests the {@link UOMUtil#selectUOM(String)} method for a successful scenario.
   */
  @Test
  public void testSelectUOMSuccess() {
    List<ProductUOM> mockProductUOMList = new ArrayList<>();
    ProductUOM mockProductUOM1 = Mockito.mock(ProductUOM.class);
    ProductUOM mockProductUOM2 = Mockito.mock(ProductUOM.class);
    UOM mockUOM1 = Mockito.mock(UOM.class);
    UOM mockUOM2 = Mockito.mock(UOM.class);

    mockProductUOMList.add(mockProductUOM1);
    mockProductUOMList.add(mockProductUOM2);

    FieldProvider[] expectedFieldProviders = new FieldProvider[2];

    when(mockProductUOM1.getId()).thenReturn("PRODUCT_UOM_ID_1");
    when(mockProductUOM1.getUOM()).thenReturn(mockUOM1);
    when(mockProductUOM2.getId()).thenReturn("PRODUCT_UOM_ID_2");
    when(mockProductUOM2.getUOM()).thenReturn(mockUOM2);
    when(mockUOM1.getName()).thenReturn("UOM Name 1");
    when(mockUOM2.getName()).thenReturn("UOM Name 2");

    when(mockDal.createCriteria(ProductUOM.class)).thenReturn(mockCriteria);
    when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);
    when(mockCriteria.addOrderBy(anyString(), anyBoolean())).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(mockProductUOMList);

    mockedFieldProviderFactory.when(() -> FieldProviderFactory.getFieldProviderArray(any(List.class))).thenReturn(
        expectedFieldProviders);

    FieldProvider[] result = UOMUtil.selectUOM(TEST_PRODUCT_ID);

    assertNotNull(RESULT_NOT_NULL, result);
    assertEquals("Should contain an array of the expected size", expectedFieldProviders.length, result.length);

    mockedOBContext.verify(OBContext::restorePreviousMode, Mockito.atLeastOnce());

    Mockito.verify(mockCriteria).add(any(Criterion.class));
    Mockito.verify(mockCriteria).addOrderBy(eq("uOM.name"), eq(true));
    Mockito.verify(mockCriteria).list();
  }

  /**
   * Tests the {@link UOMUtil#selectUOM(String)} method with a null parameter.
   */
  @Test
  public void testSelectUOMNullParameter() {
    FieldProvider[] emptyArray = new FieldProvider[0];
    mockedFieldProviderFactory.when(() -> FieldProviderFactory.getFieldProviderArray(any(List.class))).thenReturn(
        emptyArray);

    FieldProvider[] result = UOMUtil.selectUOM(null);

    assertNotNull(RESULT_NOT_NULL, result);
    assertEquals(SHOULD_BE_EMPTY_ARRAY, 0, result.length);
  }

  /**
   * Tests the {@link UOMUtil#selectUOM(String)} method with an empty string parameter.
   */
  @Test
  public void testSelectUOMEmptyParameter() {
    FieldProvider[] emptyArray = new FieldProvider[0];
    mockedFieldProviderFactory.when(() -> FieldProviderFactory.getFieldProviderArray(any(List.class))).thenReturn(
        emptyArray);

    FieldProvider[] result = UOMUtil.selectUOM("");

    assertNotNull(RESULT_NOT_NULL, result);
    assertEquals(SHOULD_BE_EMPTY_ARRAY, 0, result.length);
  }

  /**
   * Tests that {@link UOMUtil#isUomManagementEnabled()} resolves the preference against the client
   * of the current context.
   *
   * @throws Exception
   *           if the preference lookup cannot be stubbed or verified
   */
  @Test
  public void testIsUomManagementEnabledUsesCurrentClient() throws Exception {
    mockCurrentClient(TEST_CLIENT_ID);

    try (MockedStatic<Preferences> mockedPreferences = mockStatic(Preferences.class)) {
      stubPreference(mockedPreferences, TEST_CLIENT_ID, "Y");

      assertTrue("Should be enabled when the preference is Y for the current client", UOMUtil.isUomManagementEnabled());

      mockedPreferences.verify(() -> preferenceLookup(SYSTEM_CLIENT_ID), never());
    }
  }

  /**
   * Regression test: a client level override of the preference must win over the System default.
   * Before the fix the preference was always resolved against the System client, so the override
   * was ignored.
   *
   * @throws Exception
   *           if the preference lookup cannot be stubbed or verified
   */
  @Test
  public void testIsUomManagementEnabledHonorsClientOverride() throws Exception {
    mockCurrentClient(TEST_CLIENT_ID);

    // Any lookup answers Y (the System default) except the one for the current client
    try (MockedStatic<Preferences> mockedPreferences = mockStatic(Preferences.class, invocation -> "Y")) {
      stubPreference(mockedPreferences, TEST_CLIENT_ID, "N");

      assertFalse("Should be disabled when the current client overrides the preference with N",
          UOMUtil.isUomManagementEnabled());
    }
  }

  /**
   * Tests that the System client is used when there is no current context.
   *
   * @throws Exception
   *           if the preference lookup cannot be stubbed or verified
   */
  @Test
  public void testIsUomManagementEnabledFallsBackToSystemWithoutContext() throws Exception {
    mockedOBContext.when(OBContext::getOBContext).thenReturn(null);

    try (MockedStatic<Preferences> mockedPreferences = mockStatic(Preferences.class)) {
      stubPreference(mockedPreferences, SYSTEM_CLIENT_ID, "Y");

      assertTrue("Should resolve the preference against the System client", UOMUtil.isUomManagementEnabled());
    }
  }

  /**
   * Tests that the System client is used when the context has no current client.
   *
   * @throws Exception
   *           if the preference lookup cannot be stubbed or verified
   */
  @Test
  public void testIsUomManagementEnabledFallsBackToSystemWithoutCurrentClient() throws Exception {
    mockCurrentClient(null);

    try (MockedStatic<Preferences> mockedPreferences = mockStatic(Preferences.class)) {
      stubPreference(mockedPreferences, SYSTEM_CLIENT_ID, "Y");

      assertTrue("Should resolve the preference against the System client", UOMUtil.isUomManagementEnabled());
    }
  }

  /**
   * Tests that the method returns false when the preference is not defined.
   *
   * @throws Exception
   *           if the preference lookup cannot be stubbed or verified
   */
  @Test
  public void testIsUomManagementEnabledPreferenceNotFound() throws Exception {
    mockCurrentClient(TEST_CLIENT_ID);

    try (MockedStatic<Preferences> mockedPreferences = mockStatic(Preferences.class)) {
      mockedPreferences.when(() -> preferenceLookup(TEST_CLIENT_ID)).thenThrow(new PropertyNotFoundException());

      assertFalse("Should be disabled when the preference is not found", UOMUtil.isUomManagementEnabled());
    }
  }

  /**
   * Tests that the method returns false when the preference value is null.
   *
   * @throws Exception
   *           if the preference lookup cannot be stubbed or verified
   */
  @Test
  public void testIsUomManagementEnabledNullValue() throws Exception {
    mockCurrentClient(TEST_CLIENT_ID);

    try (MockedStatic<Preferences> mockedPreferences = mockStatic(Preferences.class)) {
      stubPreference(mockedPreferences, TEST_CLIENT_ID, null);

      assertFalse("Should be disabled when the preference value is null", UOMUtil.isUomManagementEnabled());
    }
  }

  /**
   * Sets the current context to one whose current client has the given id. If the id is null, the
   * context has no current client.
   */
  private void mockCurrentClient(String clientId) {
    OBContext context = Mockito.mock(OBContext.class);
    if (clientId != null) {
      Client client = Mockito.mock(Client.class);
      when(client.getId()).thenReturn(clientId);
      when(context.getCurrentClient()).thenReturn(client);
    }
    mockedOBContext.when(OBContext::getOBContext).thenReturn(context);
  }

  private void stubPreference(MockedStatic<Preferences> mockedPreferences, String clientId, String value) {
    mockedPreferences.when(() -> preferenceLookup(clientId)).thenReturn(value);
  }
}
