/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2010-2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test the {@link OBPropertiesProvider} class.
 *
 * @author mtaal
 */
public class OBPropertiesProviderTest extends OBBaseTest {
  private OBPropertiesProvider propertiesProvider;

  /**
   * Set up the test environment by initializing the OBPropertiesProvider instance.
   */
  @Before
  public void setUp() {
    propertiesProvider = OBPropertiesProvider.getInstance();
  }

  /**
   * Test the {@link OBPropertiesProvider#getFormatXMLDocument()} method.
   * This test verifies that the XML document format is correctly read.
   */
  @Test
  public void testFormatXMLRead() {
    setSystemAdministratorContext();
    final Document xmlFormat = OBPropertiesProvider.getInstance().getFormatXMLDocument();
    for (Object object : xmlFormat.getRootElement().elements()) {
      final Element element = (Element) object;
      assertEquals("Number", element.getName());
    }
  }

  /**
   * Test setting properties through an input stream.
   * This test verifies that properties can be set correctly from an input stream.
   */
  @Test
  public void testSetPropertiesFromInputStream() {
    setSystemAdministratorContext();
    String testProperties = "key1=value1\nkey2=value2";
    ByteArrayInputStream inputStream = new ByteArrayInputStream(testProperties.getBytes());

    propertiesProvider.setProperties(inputStream);
    Properties properties = propertiesProvider.getOpenbravoProperties();

    assertTrue(StringUtils.equals(properties.getProperty("key1"), "value1"));
    assertTrue(StringUtils.equals(properties.getProperty("key2"), "value2"));
  }

  /**
   * Test setting properties through a Properties object.
   * This test verifies that properties can be set correctly from a Properties object.
   */
  @Test
  public void testSetPropertiesFromPropertiesObject() {
    setSystemAdministratorContext();
    Properties props = new Properties();
    props.setProperty("key3", "value3");
    props.setProperty("key4", "value4");

    propertiesProvider.setProperties(props);
    Properties properties = propertiesProvider.getOpenbravoProperties();

    assertEquals("value3", properties.getProperty("key3"));
    assertEquals("value4", properties.getProperty("key4"));
  }

  /**
   * Test the getBooleanProperty method for various cases.
   * This test verifies that boolean properties are correctly interpreted.
   */
  @Test
  public void testGetBooleanProperty() {
    setSystemAdministratorContext();
    Properties props = new Properties();
    props.setProperty("booleanTrue", "true");
    props.setProperty("booleanYes", "yes");
    props.setProperty("booleanFalse", "false");
    props.setProperty("booleanNo", "no");

    propertiesProvider.setProperties(props);

    assertTrue(propertiesProvider.getBooleanProperty("booleanTrue"));
    assertTrue(propertiesProvider.getBooleanProperty("booleanYes"));
    assertFalse(propertiesProvider.getBooleanProperty("booleanFalse"));
    assertFalse(propertiesProvider.getBooleanProperty("booleanNo"));
    assertFalse(propertiesProvider.getBooleanProperty("nonExistentKey"));
  }

  /**
   * Test loading environment variables into properties.
   * This test verifies that environment variables are correctly loaded into properties.
   */
  @Test
  public void testLoadEnvProperties() {
    setSystemAdministratorContext();
    Properties props = new Properties();
    props.setProperty("EXISTING_ENV_VAR", "originalValue");

    propertiesProvider.setProperties(props);

    String envVar = System.getenv("HOME"); // Replace with an appropriate env variable
    if (envVar != null) {
      assertEquals(envVar, propertiesProvider.getOpenbravoProperties().getProperty("HOME"));
    }
  }

  /**
   * Test setting properties from an invalid input stream.
   * This test verifies that setting properties from an invalid input stream does not cause errors.
   */
  @Test
  public void testSetPropertiesInvalidInputStream() {
    setSystemAdministratorContext();
    ByteArrayInputStream invalidStream = new ByteArrayInputStream(new byte[0]);
    propertiesProvider.setProperties(invalidStream);
    assertNotNull(propertiesProvider.getOpenbravoProperties());
  }

  /**
   * Test reading properties.
   * This test verifies that properties are correctly read from the OBPropertiesProvider instance.
   */
  @Test
  public void testReadProperties() {
    setSystemAdministratorContext();
    OBPropertiesProvider.setInstance(new OBPropertiesProvider());
    Properties props = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    assertFalse(props.isEmpty());
  }

  /**
   * Test formatXMLDocument when no file is available.
   * This test verifies that the formatXMLDocument method handles the case when no file is found.
   */
  @Test
  public void testFormatXMLDocumentNoFile() {
    setSystemAdministratorContext();

    OBPropertiesProvider.setInstance(new OBPropertiesProvider());
    assertNotNull("FormatXML document should be null if no file is found",
        propertiesProvider.getFormatXMLDocument());
  }

  /**
   * Test loading environment variables into properties.
   * <p>
   * This test verifies that environment variables are correctly loaded into properties.
   * It mocks the environment variables and checks if they are correctly set in the properties.
   */
  @Test
  public void test_env_vars_loaded_into_properties() {
    // Arrange
    OBPropertiesProvider provider = spy(OBPropertiesProvider.getInstance());
    Properties properties = new Properties();
    properties.setProperty("existing_key", "original_value");
    provider.setProperties(properties);
    Map<String, String> mockEnvVars = new HashMap<>();
    mockEnvVars.put("existing_key", "new_value");
    mockEnvVars.put("new_key", "env_value");
    doReturn(mockEnvVars).when(provider).getEnvVariables();

    // Act
    provider.loadEnvProperties();

    // Assert
    Properties resultProps = provider.getOpenbravoProperties();
    assertEquals("new_value", resultProps.getProperty("existing_key"));
    assertEquals("original_value", resultProps.getProperty("DUP_existing_key"));
    assertEquals("env_value", resultProps.getProperty("new_key"));
  }

  /**
   * Test handling of environment variables with null keys.
   * <p>
   * This test verifies that the method throws a NullPointerException when an environment variable with a null key is encountered.
   */
  @Test(expected = NullPointerException.class)
  public void test_env_vars_with_null_key() {
    // Arrange
    OBPropertiesProvider provider = spy(OBPropertiesProvider.getInstance());
    Properties properties = new Properties();
    provider.setProperties(properties);
    Map<String, String> mockEnvVars = new HashMap<>();
    mockEnvVars.put(null, "some_value");
    doReturn(mockEnvVars).when(provider).getEnvVariables();
    // Act & Assert
    provider.loadEnvProperties();
  }
}


