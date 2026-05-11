/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ComboTableData}.
 */
@DisplayName("ComboTableData Tests")
public class ComboTableDataTest {

  private ComboTableData comboTableData;

  @BeforeEach
  void setUp() {
    comboTableData = new ComboTableData();
  }

  @Nested
  @DisplayName("Constructor tests")
  class ConstructorTests {

    @Test
    @DisplayName("Default no-arg constructor should create a non-null instance")
    void defaultConstructorShouldCreateInstance() {
      assertNotNull(comboTableData);
    }
  }

  @Nested
  @DisplayName("addSelectField tests")
  class AddSelectFieldTests {

    @Test
    @DisplayName("Should add a field to the select list")
    void shouldAddFieldToSelectList() throws Exception {
      comboTableData.addSelectField("table.column", "alias1");

      Field selectField = ComboTableData.class.getDeclaredField("select");
      selectField.setAccessible(true);
      @SuppressWarnings("unchecked")
      List<Object> selectList = (List<Object>) selectField.get(comboTableData);

      assertNotNull(selectList);
      assertEquals(1, selectList.size());
    }

    @Test
    @DisplayName("Should add multiple fields to the select list")
    void shouldAddMultipleFieldsToSelectList() throws Exception {
      comboTableData.addSelectField("table.col1", "alias1");
      comboTableData.addSelectField("table.col2", "alias2");

      Field selectField = ComboTableData.class.getDeclaredField("select");
      selectField.setAccessible(true);
      @SuppressWarnings("unchecked")
      List<Object> selectList = (List<Object>) selectField.get(comboTableData);

      assertNotNull(selectList);
      assertEquals(2, selectList.size());
    }
  }

  @Nested
  @DisplayName("addFromField tests")
  class AddFromFieldTests {

    @Test
    @DisplayName("Should add a field to the from list")
    void shouldAddFieldToFromList() throws Exception {
      comboTableData.addFromField("my_table", "t");

      Field fromField = ComboTableData.class.getDeclaredField("from");
      fromField.setAccessible(true);
      @SuppressWarnings("unchecked")
      List<Object> fromList = (List<Object>) fromField.get(comboTableData);

      assertNotNull(fromList);
      assertEquals(1, fromList.size());
    }
  }

  @Nested
  @DisplayName("addWhereField tests")
  class AddWhereFieldTests {

    @Test
    @DisplayName("Should add a field to the where list")
    void shouldAddFieldToWhereList() throws Exception {
      comboTableData.addWhereField("t.active = 'Y'", "FILTER");

      Field whereField = ComboTableData.class.getDeclaredField("where");
      whereField.setAccessible(true);
      @SuppressWarnings("unchecked")
      List<Object> whereList = (List<Object>) whereField.get(comboTableData);

      assertNotNull(whereList);
      assertEquals(1, whereList.size());
    }
  }

  @Nested
  @DisplayName("addOrderByField tests")
  class AddOrderByFieldTests {

    @Test
    @DisplayName("Should add a field to the orderBy list")
    void shouldAddFieldToOrderByList() throws Exception {
      comboTableData.addOrderByField("t.name");

      Field orderByField = ComboTableData.class.getDeclaredField("orderBy");
      orderByField.setAccessible(true);
      @SuppressWarnings("unchecked")
      List<Object> orderByList = (List<Object>) orderByField.get(comboTableData);

      assertNotNull(orderByList);
      assertEquals(1, orderByList.size());
    }
  }

  @Nested
  @DisplayName("parseContext tests")
  class ParseContextTests {

    @Test
    @DisplayName("Should return empty string for null context")
    void shouldReturnEmptyForNull() {
      assertEquals("", comboTableData.parseContext(null, "WHERE"));
    }

    @Test
    @DisplayName("Should return empty string for empty context")
    void shouldReturnEmptyForEmptyString() {
      assertEquals("", comboTableData.parseContext("", "WHERE"));
    }

    @Test
    @DisplayName("Should return same string when no @ symbols present")
    void shouldReturnSameStringWithoutAtSymbols() {
      String input = "table.column = 'value'";
      assertEquals(input, comboTableData.parseContext(input, "WHERE"));
    }

    @Test
    @DisplayName("Should replace @#User_Client@ with CLIENT_LIST_PARAM_HOLDER")
    void shouldReplaceUserClientWithPlaceholder() {
      String input = "ad_client_id IN (@#User_Client@)";
      String result = comboTableData.parseContext(input, "WHERE");
      assertTrue(result.contains(ComboTableData.CLIENT_LIST_PARAM_HOLDER));
      assertFalse(result.contains("@"));
    }

    @Test
    @DisplayName("Should replace @#User_Org@ with ORG_LIST_PARAM_HOLDER")
    void shouldReplaceUserOrgWithPlaceholder() {
      String input = "ad_org_id IN (@#User_Org@)";
      String result = comboTableData.parseContext(input, "WHERE");
      assertTrue(result.contains(ComboTableData.ORG_LIST_PARAM_HOLDER));
      assertFalse(result.contains("@"));
    }

    @Test
    @DisplayName("Should replace @SomeParam@ with ? and add parameter")
    void shouldReplaceCustomParamWithQuestionMark() {
      String input = "column = @SomeParam@";
      String result = comboTableData.parseContext(input, "WHERE");
      assertTrue(result.contains("?"));
      assertFalse(result.contains("@SomeParam@"));
    }
  }

  @Nested
  @DisplayName("replaceIgnoreCase tests (private method via reflection)")
  class ReplaceIgnoreCaseTests {

    private String invokeReplaceIgnoreCase(String data, String replaceWhat, String replaceWith)
        throws Exception {
      Method method = ComboTableData.class.getDeclaredMethod(
          "replaceIgnoreCase", String.class, String.class, String.class);
      method.setAccessible(true);
      return (String) method.invoke(comboTableData, data, replaceWhat, replaceWith);
    }

    @Test
    @DisplayName("Should return empty string for null data")
    void shouldReturnEmptyForNull() throws Exception {
      assertEquals("", invokeReplaceIgnoreCase(null, "a", "b"));
    }

    @Test
    @DisplayName("Should return empty string for empty data")
    void shouldReturnEmptyForEmptyString() throws Exception {
      assertEquals("", invokeReplaceIgnoreCase("", "a", "b"));
    }

    @Test
    @DisplayName("Should replace case-insensitively")
    void shouldReplaceCaseInsensitively() throws Exception {
      String result = invokeReplaceIgnoreCase("Hello World", "world", "EARTH");
      assertEquals("Hello EARTH", result);
    }

    @Test
    @DisplayName("Should return original string when no match found")
    void shouldReturnOriginalWhenNoMatch() throws Exception {
      String result = invokeReplaceIgnoreCase("Hello World", "xyz", "abc");
      assertEquals("Hello World", result);
    }

    @Test
    @DisplayName("Should replace all occurrences case-insensitively")
    void shouldReplaceAllOccurrences() throws Exception {
      String result = invokeReplaceIgnoreCase("foo FOO Foo", "foo", "bar");
      assertEquals("bar bar bar", result);
    }
  }

  @Nested
  @DisplayName("isInArray tests (private method via reflection)")
  class IsInArrayTests {

    private boolean invokeIsInArray(String[] data, String element) throws Exception {
      Method method = ComboTableData.class.getDeclaredMethod(
          "isInArray", String[].class, String.class);
      method.setAccessible(true);
      return (boolean) method.invoke(comboTableData, data, element);
    }

    @Test
    @DisplayName("Should return false for null array")
    void shouldReturnFalseForNullArray() throws Exception {
      assertFalse(invokeIsInArray(null, "test"));
    }

    @Test
    @DisplayName("Should return false for empty array")
    void shouldReturnFalseForEmptyArray() throws Exception {
      assertFalse(invokeIsInArray(new String[] {}, "test"));
    }

    @Test
    @DisplayName("Should return true when element is found")
    void shouldReturnTrueWhenElementFound() throws Exception {
      assertTrue(invokeIsInArray(new String[] { "alpha", "beta", "gamma" }, "beta"));
    }

    @Test
    @DisplayName("Should return false when element is not found")
    void shouldReturnFalseWhenElementNotFound() throws Exception {
      assertFalse(invokeIsInArray(new String[] { "alpha", "beta", "gamma" }, "delta"));
    }

    @Test
    @DisplayName("Should match case-insensitively")
    void shouldMatchCaseInsensitively() throws Exception {
      assertTrue(invokeIsInArray(new String[] { "ALPHA", "BETA" }, "alpha"));
    }

    @Test
    @DisplayName("Should return false for null element")
    void shouldReturnFalseForNullElement() throws Exception {
      assertFalse(invokeIsInArray(new String[] { "alpha" }, null));
    }

    @Test
    @DisplayName("Should return false for empty element")
    void shouldReturnFalseForEmptyElement() throws Exception {
      assertFalse(invokeIsInArray(new String[] { "alpha" }, ""));
    }
  }
}
