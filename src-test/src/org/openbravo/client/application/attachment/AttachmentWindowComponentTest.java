package org.openbravo.client.application.attachment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.model.ad.domain.Validation;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.AttachmentMethod;

/**
 * Tests for {@link AttachmentWindowComponent}.
 */
@SuppressWarnings("java:S112")
@RunWith(MockitoJUnitRunner.Silent.class)
public class AttachmentWindowComponentTest {

  private static final String PARSE_VALIDATION = "parseValidation";
  private static final String PARAM_X = "paramX";

  private static final String TEST_TAB_ID = "TAB001";
  private static final String TEST_ATT_METHOD_ID = "ATT001";

  private AttachmentWindowComponent instance;

  @Mock
  private Tab mockTab;

  @Mock
  private AttachmentMethod mockAttMethod;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(AttachmentWindowComponent.class);

    when(mockTab.getId()).thenReturn(TEST_TAB_ID);
    when(mockAttMethod.getId()).thenReturn(TEST_ATT_METHOD_ID);

    setPrivateField(instance, "tab", mockTab);
    setPrivateField(instance, "attMethod", mockAttMethod);
    setPrivateField(instance, "uniqueString", "12345");
  }
  /**
   * Get window client class name not in development.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetWindowClientClassNameNotInDevelopment() throws Exception {
    setupAdcsWithDevelopmentMode(false);

    String result = instance.getWindowClientClassName();

    String expected = KernelConstants.ID_PREFIX + TEST_TAB_ID + KernelConstants.ID_PREFIX
        + TEST_ATT_METHOD_ID;
    assertEquals(expected, result);
  }
  /**
   * Get window client class name in development.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetWindowClientClassNameInDevelopment() throws Exception {
    setupAdcsWithDevelopmentMode(true);

    String result = instance.getWindowClientClassName();

    String expected = KernelConstants.ID_PREFIX + TEST_TAB_ID + KernelConstants.ID_PREFIX
        + TEST_ATT_METHOD_ID + KernelConstants.ID_PREFIX + "12345";
    assertEquals(expected, result);
  }

  private void setupAdcsWithDevelopmentMode(boolean inDevelopment) throws Exception {
    org.openbravo.client.application.window.ApplicationDictionaryCachedStructures mockAdcs =
        mock(org.openbravo.client.application.window.ApplicationDictionaryCachedStructures.class);
    when(mockAdcs.isInDevelopment()).thenReturn(inDevelopment);
    Field adcsField = findField(instance.getClass(), "adcs");
    adcsField.setAccessible(true);
    adcsField.set(instance, mockAdcs);
  }
  /** Get attachment method id. */

  @Test
  public void testGetAttachmentMethodId() {
    // Act
    String result = instance.getAttachmentMethodId();

    // Assert
    assertEquals(TEST_ATT_METHOD_ID, result);
  }
  /** Get parent window. */

  @Test
  public void testGetParentWindow() {
    // Arrange
    org.openbravo.model.ad.ui.Window mockWindow = mock(org.openbravo.model.ad.ui.Window.class);
    when(mockTab.getWindow()).thenReturn(mockWindow);

    // Act
    org.openbravo.model.ad.ui.Window result = instance.getParentWindow();

    // Assert
    assertEquals(mockWindow, result);
  }
  /**
   * Parse validation extracts columns.
   * @throws Exception if an error occurs
   */

  @Test
  public void testParseValidationExtractsColumns() throws Exception {
    List<String> allParams = new ArrayList<>();
    allParams.add("colA");
    allParams.add("colB");

    Map<String, List<String>> dynCols = invokeParseValidation("'colA' = 'colB'", allParams);

    assertNotNull(dynCols);
  }
  /**
   * Parse validation no quotes in code.
   * @throws Exception if an error occurs
   */

  @Test
  public void testParseValidationNoQuotesInCode() throws Exception {
    Map<String, List<String>> dynCols = invokeParseValidation("no quotes here", new ArrayList<>());

    assertEquals(0, dynCols.size());
  }
  /**
   * Parse validation with double quotes.
   * @throws Exception if an error occurs
   */

  @Test
  public void testParseValidationWithDoubleQuotes() throws Exception {
    List<String> allParams = new ArrayList<>();
    allParams.add("colA");
    allParams.add("colB");

    Map<String, List<String>> dynCols = invokeParseValidation("\"colA\" == \"colB\"", allParams);

    assertNotNull(dynCols);
  }

  private Map<String, List<String>> invokeParseValidation(String validationCode,
      List<String> allParams) throws Exception {
    Validation validation = mock(Validation.class);
    when(validation.getValidationCode()).thenReturn(validationCode);

    Map<String, List<String>> dynCols = new HashMap<>();

    Method parseValidation = AttachmentWindowComponent.class.getDeclaredMethod(PARSE_VALIDATION,
        Validation.class, Map.class, List.class, String.class);
    parseValidation.setAccessible(true);
    parseValidation.invoke(instance, validation, dynCols, allParams, PARAM_X);

    return dynCols;
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws IllegalAccessException, NoSuchFieldException {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private Field findField(Class<?> clazz, String fieldName) {
    while (clazz != null) {
      try {
        return clazz.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }
    throw new RuntimeException("Field not found: " + fieldName);
  }
}
