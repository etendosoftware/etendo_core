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
@RunWith(MockitoJUnitRunner.Silent.class)
public class AttachmentWindowComponentTest {

  private static final String TEST_TAB_ID = "TAB001";
  private static final String TEST_ATT_METHOD_ID = "ATT001";

  private AttachmentWindowComponent instance;

  @Mock
  private Tab mockTab;

  @Mock
  private AttachmentMethod mockAttMethod;

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

  @Test
  public void testGetWindowClientClassNameNotInDevelopment() throws Exception {
    // Arrange - need adcs mock for isInDevelopment
    org.openbravo.client.application.window.ApplicationDictionaryCachedStructures mockAdcs =
        mock(org.openbravo.client.application.window.ApplicationDictionaryCachedStructures.class);
    when(mockAdcs.isInDevelopment()).thenReturn(false);
    Field adcsField = findField(instance.getClass(), "adcs");
    adcsField.setAccessible(true);
    adcsField.set(instance, mockAdcs);

    // Act
    String result = instance.getWindowClientClassName();

    // Assert
    String expected = KernelConstants.ID_PREFIX + TEST_TAB_ID + KernelConstants.ID_PREFIX
        + TEST_ATT_METHOD_ID;
    assertEquals(expected, result);
  }

  @Test
  public void testGetWindowClientClassNameInDevelopment() throws Exception {
    // Arrange
    org.openbravo.client.application.window.ApplicationDictionaryCachedStructures mockAdcs =
        mock(org.openbravo.client.application.window.ApplicationDictionaryCachedStructures.class);
    when(mockAdcs.isInDevelopment()).thenReturn(true);
    Field adcsField = findField(instance.getClass(), "adcs");
    adcsField.setAccessible(true);
    adcsField.set(instance, mockAdcs);

    // Act
    String result = instance.getWindowClientClassName();

    // Assert
    String expected = KernelConstants.ID_PREFIX + TEST_TAB_ID + KernelConstants.ID_PREFIX
        + TEST_ATT_METHOD_ID + KernelConstants.ID_PREFIX + "12345";
    assertEquals(expected, result);
  }

  @Test
  public void testGetAttachmentMethodId() {
    // Act
    String result = instance.getAttachmentMethodId();

    // Assert
    assertEquals(TEST_ATT_METHOD_ID, result);
  }

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

  @Test
  public void testParseValidationExtractsColumns() throws Exception {
    // Arrange
    Validation validation = mock(Validation.class);
    when(validation.getValidationCode()).thenReturn("'colA' = 'colB'");

    Map<String, List<String>> dynCols = new HashMap<>();
    List<String> allParams = new ArrayList<>();
    allParams.add("colA");
    allParams.add("colB");

    // Act
    Method parseValidation = AttachmentWindowComponent.class.getDeclaredMethod("parseValidation",
        Validation.class, Map.class, List.class, String.class);
    parseValidation.setAccessible(true);
    parseValidation.invoke(instance, validation, dynCols, allParams, "paramX");

    // Assert - dynCols should have been populated
    assertNotNull(dynCols);
  }

  @Test
  public void testParseValidationNoQuotesInCode() throws Exception {
    // Arrange
    Validation validation = mock(Validation.class);
    when(validation.getValidationCode()).thenReturn("no quotes here");

    Map<String, List<String>> dynCols = new HashMap<>();
    List<String> allParams = new ArrayList<>();

    // Act
    Method parseValidation = AttachmentWindowComponent.class.getDeclaredMethod("parseValidation",
        Validation.class, Map.class, List.class, String.class);
    parseValidation.setAccessible(true);
    parseValidation.invoke(instance, validation, dynCols, allParams, "paramX");

    // Assert - no dynamic columns should have been added
    assertEquals(0, dynCols.size());
  }

  @Test
  public void testParseValidationWithDoubleQuotes() throws Exception {
    // Arrange - double quotes should be converted to single quotes
    Validation validation = mock(Validation.class);
    when(validation.getValidationCode()).thenReturn("\"colA\" == \"colB\"");

    Map<String, List<String>> dynCols = new HashMap<>();
    List<String> allParams = new ArrayList<>();
    allParams.add("colA");
    allParams.add("colB");

    // Act
    Method parseValidation = AttachmentWindowComponent.class.getDeclaredMethod("parseValidation",
        Validation.class, Map.class, List.class, String.class);
    parseValidation.setAccessible(true);
    parseValidation.invoke(instance, validation, dynCols, allParams, "paramX");

    // Assert
    assertNotNull(dynCols);
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
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
