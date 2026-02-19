package org.openbravo.client.kernel.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.service.json.JsonUtils;

/**
 * Tests for {@link AbsoluteTimeUIDefinition}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AbsoluteTimeUIDefinitionTest {

  private AbsoluteTimeUIDefinition uiDefinition;
  private MockedStatic<OBPropertiesProvider> propertiesProviderStatic;
  private OBPropertiesProvider propertiesProvider;
  private Properties properties;

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    uiDefinition = objenesis.newInstance(AbsoluteTimeUIDefinition.class);

    // Set the xmlTimeFormat field via reflection
    Field xmlTimeFormatField = AbsoluteTimeUIDefinition.class.getDeclaredField("xmlTimeFormat");
    xmlTimeFormatField.setAccessible(true);
    xmlTimeFormatField.set(uiDefinition, JsonUtils.createJSTimeFormat());

    // Mock OBPropertiesProvider for getClassicFormat()
    propertiesProvider = mock(OBPropertiesProvider.class);
    properties = new Properties();
    properties.put("dateTimeFormat.java", "dd-MM-yyyy HH:mm:ss");
    when(propertiesProvider.getOpenbravoProperties()).thenReturn(properties);

    propertiesProviderStatic = mockStatic(OBPropertiesProvider.class);
    propertiesProviderStatic.when(OBPropertiesProvider::getInstance).thenReturn(propertiesProvider);
  }

  @After
  public void tearDown() {
    if (propertiesProviderStatic != null) {
      propertiesProviderStatic.close();
    }
  }

  @Test
  public void testGetParentType() {
    assertEquals("time", uiDefinition.getParentType());
  }

  @Test
  public void testGetFormEditorType() {
    assertEquals("OBAbsoluteTimeItem", uiDefinition.getFormEditorType());
  }

  @Test
  public void testConvertToClassicStringWithNull() {
    String result = uiDefinition.convertToClassicString(null);
    assertEquals("", result);
  }

  @Test
  public void testConvertToClassicStringWithEmptyString() {
    String result = uiDefinition.convertToClassicString("");
    assertEquals("", result);
  }

  @Test
  public void testConvertToClassicStringWithStringValue() {
    String result = uiDefinition.convertToClassicString("10:30:00");
    assertEquals("10:30:00", result);
  }

  @Test
  public void testConvertToClassicStringWithTimestamp() {
    Timestamp ts = Timestamp.valueOf("2024-01-15 10:30:45.0");
    String result = uiDefinition.convertToClassicString(ts);
    assertNotNull(result);
    assertTrue(result.contains("10:30:45"));
  }

  @Test
  public void testCreateFromClassicStringWithNull() {
    Object result = uiDefinition.createFromClassicString(null);
    assertNull(result);
  }

  @Test
  public void testCreateFromClassicStringWithEmpty() {
    Object result = uiDefinition.createFromClassicString("");
    assertNull(result);
  }

  @Test
  public void testCreateFromClassicStringWithNullLiteral() {
    Object result = uiDefinition.createFromClassicString("null");
    assertNull(result);
  }

  @Test
  public void testCreateFromClassicStringWithValidTime() {
    Object result = uiDefinition.createFromClassicString("14:30:00");
    assertNotNull(result);
    assertTrue(result instanceof String);
    // The result should be in ISO time format (yyyy-MM-dd'T'HH:mm:ss)
    String timeStr = (String) result;
    assertTrue(timeStr.contains("T"));
    assertTrue(timeStr.contains("14:30:00"));
  }

  @Test(expected = OBException.class)
  public void testCreateFromClassicStringWithInvalidValue() {
    uiDefinition.createFromClassicString("not-a-time");
  }

  @Test
  public void testConvertToClassicStringWithAmPmFormat() {
    // Reconfigure with AM/PM format
    properties.put("dateTimeFormat.java", "dd-MM-yyyy hh:mm:ss a");

    // Reset classicFormat so it gets re-initialized
    try {
      Field classicFormatField = AbsoluteTimeUIDefinition.class.getDeclaredField("classicFormat");
      classicFormatField.setAccessible(true);
      classicFormatField.set(uiDefinition, null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    Timestamp ts = Timestamp.valueOf("2024-01-15 14:30:00.0");
    String result = uiDefinition.convertToClassicString(ts);
    assertNotNull(result);
  }
}
