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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ComboTableData}.
 */
@DisplayName("ComboTableData")
public class ComboTableDataTest {

  private static final String WHERE = "WHERE";
  private static final String COL1_EQUALS_X = "col1 = 'x'";

  // ── Default no-arg constructor ────────────────────────────────────────

  @Nested
  @DisplayName("Default no-arg constructor")
  class DefaultConstructor {
    @Test
    @DisplayName("creates instance without exception")
    void createsInstance() {
      ComboTableData ctd = new ComboTableData();
      assertNotNull(ctd);
    }
  }

  // ── addSelectField / addFromField / addWhereField / addOrderByField ──

  @Nested
  @DisplayName("addSelectField, addFromField, addWhereField, addOrderByField")
  class AddFieldMethods {

    @SuppressWarnings("unchecked")
    private <T> List<T> getField(ComboTableData ctd, String fieldName) throws Exception {
      Field f = ComboTableData.class.getDeclaredField(fieldName);
      f.setAccessible(true);
      return (List<T>) f.get(ctd);
    }

    @Test
    @DisplayName("addSelectField adds to select list")
    void addSelectField() throws Exception {
      ComboTableData ctd = new ComboTableData();
      ctd.addSelectField("col1", "alias1");
      List<?> selectFields = getField(ctd, "select");
      assertNotNull(selectFields);
      assertEquals(1, selectFields.size());
    }

    @Test
    @DisplayName("addFromField adds to from list")
    void addFromField() throws Exception {
      ComboTableData ctd = new ComboTableData();
      ctd.addFromField("table1", "t1");
      List<?> fromFields = getField(ctd, "from");
      assertNotNull(fromFields);
      assertEquals(1, fromFields.size());
    }

    @Test
    @DisplayName("addWhereField adds to where list")
    void addWhereField() throws Exception {
      ComboTableData ctd = new ComboTableData();
      ctd.addWhereField(COL1_EQUALS_X, WHERE);
      List<?> whereFields = getField(ctd, "where");
      assertNotNull(whereFields);
      assertEquals(1, whereFields.size());
    }

    @Test
    @DisplayName("addOrderByField adds to orderBy list")
    void addOrderByField() throws Exception {
      ComboTableData ctd = new ComboTableData();
      ctd.addOrderByField("col1 ASC");
      List<?> orderByFields = getField(ctd, "orderBy");
      assertNotNull(orderByFields);
      assertEquals(1, orderByFields.size());
    }
  }

  // ── parseContext ──────────────────────────────────────────────────────

  @Nested
  @DisplayName("parseContext")
  class ParseContext {

    @Test
    @DisplayName("null returns empty string")
    void nullReturnsEmpty() {
      ComboTableData ctd = new ComboTableData();
      assertEquals("", ctd.parseContext(null, WHERE));
    }

    @Test
    @DisplayName("empty returns empty string")
    void emptyReturnsEmpty() {
      ComboTableData ctd = new ComboTableData();
      assertEquals("", ctd.parseContext("", WHERE));
    }

    @Test
    @DisplayName("no @ returns unchanged")
    void noAtReturnsUnchanged() {
      ComboTableData ctd = new ComboTableData();
      assertEquals(COL1_EQUALS_X, ctd.parseContext(COL1_EQUALS_X, WHERE));
    }

    @Test
    @DisplayName("@#User_Client@ replaced with client list placeholder")
    void userClientReplaced() {
      ComboTableData ctd = new ComboTableData();
      String result = ctd.parseContext("client IN (@#User_Client@)", WHERE);
      assertTrue(result.contains(ComboTableData.CLIENT_LIST_PARAM_HOLDER));
    }

    @Test
    @DisplayName("@#User_Org@ replaced with org list placeholder")
    void userOrgReplaced() {
      ComboTableData ctd = new ComboTableData();
      String result = ctd.parseContext("org IN (@#User_Org@)", WHERE);
      assertTrue(result.contains(ComboTableData.ORG_LIST_PARAM_HOLDER));
    }

    @Test
    @DisplayName("@SomeParam@ replaced with ?")
    void someParamReplaced() {
      ComboTableData ctd = new ComboTableData();
      String result = ctd.parseContext("col = @SomeParam@", WHERE);
      assertTrue(result.contains("?"));
      assertFalse(result.contains("@SomeParam@"));
    }
  }

  // ── replaceIgnoreCase (private) ───────────────────────────────────────

  @Nested
  @DisplayName("replaceIgnoreCase (private)")
  class ReplaceIgnoreCase {

    private String invoke(ComboTableData ctd, String data, String replaceWhat,
        String replaceWith) throws Exception {
      Method method = ComboTableData.class.getDeclaredMethod(
          "replaceIgnoreCase", String.class, String.class, String.class);
      method.setAccessible(true);
      return (String) method.invoke(ctd, data, replaceWhat, replaceWith);
    }

    @Test void nullReturnsEmpty() throws Exception {
      assertEquals("", invoke(new ComboTableData(), null, "a", "b"));
    }

    @Test void emptyReturnsEmpty() throws Exception {
      assertEquals("", invoke(new ComboTableData(), "", "a", "b"));
    }

    @Test
    @DisplayName("case insensitive replacement")
    void caseInsensitive() throws Exception {
      String result = invoke(new ComboTableData(), "SELECT FooTable.col", "footable.", "t1.");
      assertEquals("SELECT t1.col", result);
    }

    @Test
    @DisplayName("no match returns original")
    void noMatch() throws Exception {
      String result = invoke(new ComboTableData(), "SELECT col", "xyz.", "t1.");
      assertEquals("SELECT col", result);
    }

    @Test
    @DisplayName("multiple occurrences all replaced")
    void multipleOccurrences() throws Exception {
      String result = invoke(new ComboTableData(), "TBL.a AND TBL.b", "TBL.", "t.");
      assertEquals("t.a AND t.b", result);
    }
  }

  // ── isInArray (private) ───────────────────────────────────────────────

  @Nested
  @DisplayName("isInArray (private)")
  class IsInArray {

    private boolean invoke(ComboTableData ctd, String[] data, String element) throws Exception {
      Method method = ComboTableData.class.getDeclaredMethod(
          "isInArray", String[].class, String.class);
      method.setAccessible(true);
      return (boolean) method.invoke(ctd, data, element);
    }

    @Test void nullArrayReturnsFalse() throws Exception {
      assertFalse(invoke(new ComboTableData(), null, "x"));
    }

    @Test void emptyArrayReturnsFalse() throws Exception {
      assertFalse(invoke(new ComboTableData(), new String[]{}, "x"));
    }

    @Test void matchReturnsTrue() throws Exception {
      assertTrue(invoke(new ComboTableData(), new String[]{"a", "b", "c"}, "b"));
    }

    @Test void noMatchReturnsFalse() throws Exception {
      assertFalse(invoke(new ComboTableData(), new String[]{"a", "b", "c"}, "x"));
    }

    @Test void nullElementReturnsFalse() throws Exception {
      assertFalse(invoke(new ComboTableData(), new String[]{"a"}, null));
    }

    @Test void emptyElementReturnsFalse() throws Exception {
      assertFalse(invoke(new ComboTableData(), new String[]{"a"}, ""));
    }
  }

  // ── setParameter ─────────────────────────────────────────────────────

  @Nested
  @DisplayName("setParameter")
  class SetParameter {

    @SuppressWarnings("unchecked")
    private Map<String, String> getParametersMap(ComboTableData ctd) throws Exception {
      Field f = ComboTableData.class.getDeclaredField("parameters");
      f.setAccessible(true);
      return (Map<String, String>) f.get(ctd);
    }

    @Test
    @DisplayName("stores value with uppercased key")
    void storesValueWithUppercasedKey() throws Exception {
      ComboTableData ctd = new ComboTableData();
      ctd.setParameter("myKey", "myValue");
      Map<String, String> params = getParametersMap(ctd);
      assertEquals("myValue", params.get("MYKEY"));
    }

    @Test
    @DisplayName("null name throws exception")
    void nullNameThrows() {
      ComboTableData ctd = new ComboTableData();
      assertThrows(Exception.class, () -> ctd.setParameter(null, "value"));
    }

    @Test
    @DisplayName("empty name throws exception")
    void emptyNameThrows() {
      ComboTableData ctd = new ComboTableData();
      assertThrows(Exception.class, () -> ctd.setParameter("", "value"));
    }

    @Test
    @DisplayName("null value removes key")
    void nullValueRemovesKey() throws Exception {
      ComboTableData ctd = new ComboTableData();
      ctd.setParameter("key1", "val1");
      ctd.setParameter("key1", null);
      Map<String, String> params = getParametersMap(ctd);
      assertFalse(params.containsKey("KEY1"));
    }

    @Test
    @DisplayName("empty value removes key")
    void emptyValueRemovesKey() throws Exception {
      ComboTableData ctd = new ComboTableData();
      ctd.setParameter("key1", "val1");
      ctd.setParameter("key1", "");
      Map<String, String> params = getParametersMap(ctd);
      assertFalse(params.containsKey("KEY1"));
    }
  }

  // ── canBeCached / isAllowedCrossOrgReference ─────────────────────────

  @Nested
  @DisplayName("canBeCached and isAllowedCrossOrgReference")
  class BooleanGetters {

    @Test
    @DisplayName("canBeCached defaults to false")
    void canBeCachedDefault() {
      ComboTableData ctd = new ComboTableData();
      assertFalse(ctd.canBeCached());
    }

    @Test
    @DisplayName("isAllowedCrossOrgReference defaults to false")
    void isAllowedCrossOrgReferenceDefault() {
      ComboTableData ctd = new ComboTableData();
      assertFalse(ctd.isAllowedCrossOrgReference());
    }
  }

  // ── getObjectName ────────────────────────────────────────────────────

  @Nested
  @DisplayName("getObjectName")
  class GetObjectName {

    @Test
    @DisplayName("default returns null")
    void defaultReturnsNull() {
      ComboTableData ctd = new ComboTableData();
      assertNull(ctd.getObjectName());
    }
  }

  // ── Multiple addField operations ─────────────────────────────────────

  @Nested
  @DisplayName("Multiple addField operations")
  class MultipleAddFields {

    @SuppressWarnings("unchecked")
    private <T> List<T> getField(ComboTableData ctd, String fieldName) throws Exception {
      Field f = ComboTableData.class.getDeclaredField(fieldName);
      f.setAccessible(true);
      return (List<T>) f.get(ctd);
    }

    @Test
    @DisplayName("multiple select fields accumulate")
    void multipleSelectFields() throws Exception {
      ComboTableData ctd = new ComboTableData();
      ctd.addSelectField("col1", "a1");
      ctd.addSelectField("col2", "a2");
      ctd.addSelectField("col3", "a3");
      List<?> selectFields = getField(ctd, "select");
      assertEquals(3, selectFields.size());
    }

    @Test
    @DisplayName("multiple where fields accumulate")
    void multipleWhereFields() throws Exception {
      ComboTableData ctd = new ComboTableData();
      ctd.addWhereField("c1 = 1", WHERE);
      ctd.addWhereField("c2 = 2", WHERE);
      ctd.addWhereField("c3 = 3", WHERE);
      List<?> whereFields = getField(ctd, "where");
      assertEquals(3, whereFields.size());
    }
  }
}
