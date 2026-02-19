package org.openbravo.client.kernel.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.session.OBPropertiesProvider;

/**
 * Tests for AbsoluteDateTimeUIDefinition.
 */
@RunWith(MockitoJUnitRunner.class)
public class AbsoluteDateTimeUIDefinitionTest {

  private AbsoluteDateTimeUIDefinition instance;

  @Mock
  private OBPropertiesProvider mockPropertiesProvider;

  private MockedStatic<OBPropertiesProvider> propertiesProviderStatic;

  @Before
  public void setUp() throws Exception {
    propertiesProviderStatic = mockStatic(OBPropertiesProvider.class);
    propertiesProviderStatic.when(OBPropertiesProvider::getInstance)
        .thenReturn(mockPropertiesProvider);

    Properties props = new Properties();
    props.setProperty("dateTimeFormat.java", "dd-MM-yyyy HH:mm:ss");
    lenient().when(mockPropertiesProvider.getOpenbravoProperties()).thenReturn(props);

    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(AbsoluteDateTimeUIDefinition.class);
  }

  @After
  public void tearDown() {
    if (propertiesProviderStatic != null) {
      propertiesProviderStatic.close();
    }
  }

  @Test
  public void testGetParentType() {
    assertEquals("datetime", instance.getParentType());
  }

  @Test
  public void testGetFormEditorType() {
    assertEquals("OBAbsoluteDateTimeItem", instance.getFormEditorType());
  }

  @Test
  public void testGetClientFormatObject() throws Exception {
    Method method = AbsoluteDateTimeUIDefinition.class.getDeclaredMethod("getClientFormatObject");
    method.setAccessible(true);
    String result = (String) method.invoke(instance);
    assertEquals("OB.Format.dateTime", result);
  }

  @Test
  public void testGetClassicFormatReturnsDateTimeFormat() throws Exception {
    Method method = AbsoluteDateTimeUIDefinition.class.getDeclaredMethod("getClassicFormat");
    method.setAccessible(true);
    SimpleDateFormat format = (SimpleDateFormat) method.invoke(instance);

    assertEquals("dd-MM-yyyy HH:mm:ss", format.toPattern());
  }

  @Test
  public void testCreateFromClassicStringWithNull() {
    Object result = instance.createFromClassicString(null);
    assertNull(result);
  }

  @Test
  public void testCreateFromClassicStringWithEmpty() {
    Object result = instance.createFromClassicString("");
    assertNull(result);
  }

  @Test
  public void testCreateFromClassicStringWithNullString() {
    Object result = instance.createFromClassicString("null");
    assertNull(result);
  }

  @Test
  public void testCreateFromClassicStringWithISOFormat() {
    // When the value contains "T", it should return the value as-is
    String isoDate = "2024-01-15T10:30:00";
    Object result = instance.createFromClassicString(isoDate);
    assertEquals(isoDate, result);
  }
}
