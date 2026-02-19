package org.openbravo.erpCommon.info;

import static org.junit.Assert.assertEquals;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Tests for {@link AttributeSetInstance}.
 * Focuses on testable private utility methods: replace, instanceValue, generateScript.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class AttributeSetInstanceTest {

  private static final String TEST_ATTR_VALUE_ID = "testAttrValueId";
  private static final String TEST_VALUE = "testValue";

  private static final String TEST_ATTRIBUTE_ID = "100";
  private static final String TEST_ATTRIBUTE_ID_2 = "200";

  private AttributeSetInstance instance;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(AttributeSetInstance.class);
  }

  // --- Tests for replace(String) method ---
  /**
   * Replace removes spaces.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceRemovesSpaces() throws Exception {
    String result = invokeReplace("Hello World");
    assertEquals("HelloWorld", result);
  }
  /**
   * Replace removes hash.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceRemovesHash() throws Exception {
    String result = invokeReplace("Item#1");
    assertEquals("Item1", result);
  }
  /**
   * Replace removes ampersand.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceRemovesAmpersand() throws Exception {
    String result = invokeReplace("A&B");
    assertEquals("AB", result);
  }
  /**
   * Replace removes comma.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceRemovesComma() throws Exception {
    String result = invokeReplace("A,B");
    assertEquals("AB", result);
  }
  /**
   * Replace removes parentheses.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceRemovesParentheses() throws Exception {
    String result = invokeReplace("Method(param)");
    assertEquals("Methodparam", result);
  }
  /**
   * Replace removes multiple special chars.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceRemovesMultipleSpecialChars() throws Exception {
    String result = invokeReplace("Name #1 (A&B, C)");
    assertEquals("Name1ABC", result);
  }
  /**
   * Replace with empty string.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceWithEmptyString() throws Exception {
    String result = invokeReplace("");
    assertEquals("", result);
  }
  /**
   * Replace with no special chars.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceWithNoSpecialChars() throws Exception {
    String result = invokeReplace("SimpleText");
    assertEquals("SimpleText", result);
  }

  // --- Tests for instanceValue(AttributeSetInstanceData[], String, boolean) method ---
  /**
   * Instance value returns empty for null data.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInstanceValueReturnsEmptyForNullData() throws Exception {
    String result = invokeInstanceValue(null, TEST_ATTRIBUTE_ID, false);
    assertEquals("", result);
  }
  /**
   * Instance value returns empty for empty data.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInstanceValueReturnsEmptyForEmptyData() throws Exception {
    AttributeSetInstanceData[] data = new AttributeSetInstanceData[0];
    String result = invokeInstanceValue(data, TEST_ATTRIBUTE_ID, false);
    assertEquals("", result);
  }
  /**
   * Instance value returns value when not list.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInstanceValueReturnsValueWhenNotList() throws Exception {
    AttributeSetInstanceData[] data = new AttributeSetInstanceData[] {
        createInstanceData(TEST_ATTRIBUTE_ID, TEST_VALUE, TEST_ATTR_VALUE_ID)
    };
    String result = invokeInstanceValue(data, TEST_ATTRIBUTE_ID, false);
    assertEquals(TEST_VALUE, result);
  }
  /**
   * Instance value returns attribute value id when list.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInstanceValueReturnsAttributeValueIdWhenList() throws Exception {
    AttributeSetInstanceData[] data = new AttributeSetInstanceData[] {
        createInstanceData(TEST_ATTRIBUTE_ID, TEST_VALUE, TEST_ATTR_VALUE_ID)
    };
    String result = invokeInstanceValue(data, TEST_ATTRIBUTE_ID, true);
    assertEquals(TEST_ATTR_VALUE_ID, result);
  }
  /**
   * Instance value returns empty when attribute not found.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInstanceValueReturnsEmptyWhenAttributeNotFound() throws Exception {
    AttributeSetInstanceData[] data = new AttributeSetInstanceData[] {
        createInstanceData(TEST_ATTRIBUTE_ID, TEST_VALUE, TEST_ATTR_VALUE_ID)
    };
    String result = invokeInstanceValue(data, TEST_ATTRIBUTE_ID_2, false);
    assertEquals("", result);
  }
  /**
   * Instance value finds correct attribute in multiple entries.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInstanceValueFindsCorrectAttributeInMultipleEntries() throws Exception {
    AttributeSetInstanceData[] data = new AttributeSetInstanceData[] {
        createInstanceData(TEST_ATTRIBUTE_ID, "value1", "attrValue1"),
        createInstanceData(TEST_ATTRIBUTE_ID_2, "value2", "attrValue2")
    };
    String result = invokeInstanceValue(data, TEST_ATTRIBUTE_ID_2, false);
    assertEquals("value2", result);
  }

  // --- Tests for generateScript(VariablesSecureApp, AttributeSetInstanceData[]) method ---
  /**
   * Generate script returns empty for null fields.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGenerateScriptReturnsEmptyForNullFields() throws Exception {
    String result = invokeGenerateScript(null);
    assertEquals("", result);
  }
  /**
   * Generate script returns empty for empty fields.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGenerateScriptReturnsEmptyForEmptyFields() throws Exception {
    AttributeSetInstanceData[] fields = new AttributeSetInstanceData[0];
    String result = invokeGenerateScript(fields);
    assertEquals("", result);
  }
  /**
   * Generate script returns java script function.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGenerateScriptReturnsJavaScriptFunction() throws Exception {
    AttributeSetInstanceData[] fields = new AttributeSetInstanceData[] {
        createFieldData("Color", "Y", "N", "N", "N", "N", "N")
    };
    String result = invokeGenerateScript(fields);
    String expected = "function onloadFunctions() {\n  return true;\n}\n";
    assertEquals(expected, result);
  }

  // --- Helper methods ---

  private String invokeReplace(String input) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AttributeSetInstance.class.getDeclaredMethod("replace", String.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, input);
  }

  private String invokeInstanceValue(AttributeSetInstanceData[] data, String attributeId,
      boolean isList) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AttributeSetInstance.class.getDeclaredMethod("instanceValue",
        AttributeSetInstanceData[].class, String.class, boolean.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, data, attributeId, isList);
  }

  private String invokeGenerateScript(AttributeSetInstanceData[] fields) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AttributeSetInstance.class.getDeclaredMethod("generateScript",
        org.openbravo.base.secureApp.VariablesSecureApp.class,
        AttributeSetInstanceData[].class);
    method.setAccessible(true);
    return (String) method.invoke(instance, (Object) null, fields);
  }

  private AttributeSetInstanceData createInstanceData(String attributeId, String value,
      String attributeValueId) {
    ObjenesisStd objenesis = new ObjenesisStd();
    AttributeSetInstanceData data = objenesis.newInstance(AttributeSetInstanceData.class);
    data.mAttributeId = attributeId;
    data.value = value;
    data.mAttributevalueId = attributeValueId;
    return data;
  }

  private AttributeSetInstanceData createFieldData(String elementName, String islist,
      String islot, String isserno, String isguaranteedate, String islockable,
      String ismandatory) {
    ObjenesisStd objenesis = new ObjenesisStd();
    AttributeSetInstanceData data = objenesis.newInstance(AttributeSetInstanceData.class);
    data.elementname = elementName;
    data.islist = islist;
    data.islot = islot;
    data.isserno = isserno;
    data.isguaranteedate = isguaranteedate;
    data.islockable = islockable;
    data.ismandatory = ismandatory;
    data.mAttributeId = "100";
    data.isoneattrsetvalrequired = "N";
    data.mLotctlId = "";
    data.mSernoctlId = "";
    return data;
  }
}
