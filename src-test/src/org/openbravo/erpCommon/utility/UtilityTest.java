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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openbravo.base.structure.BaseOBObject;

/**
 * Unit tests for {@link Utility} pure-logic static methods.
 * No mocking required — these are all self-contained.
 */
@SuppressWarnings("java:S1149") // Vector is required by the Utility API under test
@DisplayName("Utility")
public class UtilityTest {

  private static final String COMMA_SEPARATED_ABC = "a, b, c";
  private static final String QUOTED_AB_LIST = "('a', 'b')";

  // ── Reference type checks ────────────────────────────────────────────

  @Nested
  @DisplayName("isID")
  class IsID {
    @Test void testIs13() { assertTrue(Utility.isID("13")); }
    @Test void testNot12() { assertFalse(Utility.isID("12")); }
    @Test void testNull() { assertFalse(Utility.isID(null)); }
    @Test void testEmpty() { assertFalse(Utility.isID("")); }
  }

  @Nested
  @DisplayName("isDecimalNumber")
  class IsDecimalNumber {
    @Test void test12() { assertTrue(Utility.isDecimalNumber("12")); }
    @Test void test22() { assertTrue(Utility.isDecimalNumber("22")); }
    @Test void test29() { assertTrue(Utility.isDecimalNumber("29")); }
    @Test void test80008() { assertTrue(Utility.isDecimalNumber("80008")); }
    @Test void testNot11() { assertFalse(Utility.isDecimalNumber("11")); }
    @Test void testNull() { assertFalse(Utility.isDecimalNumber(null)); }
    @Test void testEmpty() { assertFalse(Utility.isDecimalNumber("")); }
  }

  @Nested
  @DisplayName("isIntegerNumber")
  class IsIntegerNumber {
    @Test void test11() { assertTrue(Utility.isIntegerNumber("11")); }
    @Test void test13() { assertTrue(Utility.isIntegerNumber("13")); }
    @Test void test25() { assertTrue(Utility.isIntegerNumber("25")); }
    @Test void testNot12() { assertFalse(Utility.isIntegerNumber("12")); }
    @Test void testNull() { assertFalse(Utility.isIntegerNumber(null)); }
    @Test void testEmpty() { assertFalse(Utility.isIntegerNumber("")); }
  }

  @Nested
  @DisplayName("isDateTime")
  class IsDateTime {
    @Test void test15() { assertTrue(Utility.isDateTime("15")); }
    @Test void test16() { assertTrue(Utility.isDateTime("16")); }
    @Test void test24() { assertTrue(Utility.isDateTime("24")); }
    @Test void testNot12() { assertFalse(Utility.isDateTime("12")); }
    @Test void testNull() { assertFalse(Utility.isDateTime(null)); }
    @Test void testEmpty() { assertFalse(Utility.isDateTime("")); }
  }

  @Nested
  @DisplayName("isNumericParameter")
  class IsNumericParameter {
    @Test void testDecimal12() { assertTrue(Utility.isNumericParameter("12")); }
    @Test void testInteger11() { assertTrue(Utility.isNumericParameter("11")); }
    @Test void testID13IsFalse() { assertFalse(Utility.isNumericParameter("13")); }
    @Test void testNonNumeric() { assertFalse(Utility.isNumericParameter("15")); }
    @Test void testNull() { assertFalse(Utility.isNumericParameter(null)); }
  }

  // ── UUID / Hex validation ────────────────────────────────────────────

  @Nested
  @DisplayName("isUUIDString")
  class IsUUIDString {
    @Test void testValid32Hex() {
      assertTrue(Utility.isUUIDString("A1B2C3D4E5F6A1B2C3D4E5F6A1B2C3D4"));
    }
    @Test void test31Chars() {
      assertFalse(Utility.isUUIDString("A1B2C3D4E5F6A1B2C3D4E5F6A1B2C3D"));
    }
    @Test void testNonHex() {
      assertFalse(Utility.isUUIDString("G1B2C3D4E5F6A1B2C3D4E5F6A1B2C3D4"));
    }
    @Test void testLowercase() {
      assertTrue(Utility.isUUIDString("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4"));
    }
  }

  @Nested
  @DisplayName("isHexStringChar")
  class IsHexStringChar {
    @Test void testDigits() {
      for (char c = '0'; c <= '9'; c++) assertTrue(Utility.isHexStringChar(c));
    }
    @Test void testLowerHex() {
      for (char c = 'a'; c <= 'f'; c++) assertTrue(Utility.isHexStringChar(c));
    }
    @Test void testUpperHex() {
      for (char c = 'A'; c <= 'F'; c++) assertTrue(Utility.isHexStringChar(c));
    }
    @Test void testNonHex() {
      assertFalse(Utility.isHexStringChar('g'));
      assertFalse(Utility.isHexStringChar('Z'));
      assertFalse(Utility.isHexStringChar(' '));
    }
  }

  // ── Vector / String operations ───────────────────────────────────────

  @Nested
  @DisplayName("getStringVector")
  class GetStringVector {
    @Test void testCommaSeparated() {
      Vector<String> v = Utility.getStringVector(COMMA_SEPARATED_ABC);
      assertEquals(3, v.size());
      assertEquals("a", v.get(0));
      assertEquals("b", v.get(1));
      assertEquals("c", v.get(2));
    }
    @Test void testDeduplicates() {
      Vector<String> v = Utility.getStringVector("a, b, a");
      assertEquals(2, v.size());
    }
    @Test void testSingleElement() {
      Vector<String> v = Utility.getStringVector("only");
      assertEquals(1, v.size());
      assertEquals("only", v.get(0));
    }
  }

  @Nested
  @DisplayName("getIntersectionVector")
  class GetIntersectionVector {
    @Test void testOverlap() {
      Vector<String> v1 = new Vector<>(List.of("a", "b", "c"));
      Vector<String> v2 = new Vector<>(List.of("b", "c", "d"));
      Vector<String> result = Utility.getIntersectionVector(v1, v2);
      assertEquals(2, result.size());
      assertTrue(result.contains("b"));
      assertTrue(result.contains("c"));
    }
    @Test void testDisjoint() {
      Vector<String> v1 = new Vector<>(List.of("a", "b"));
      Vector<String> v2 = new Vector<>(List.of("c", "d"));
      assertTrue(Utility.getIntersectionVector(v1, v2).isEmpty());
    }
    @Test void testOneEmpty() {
      Vector<String> v1 = new Vector<>();
      Vector<String> v2 = new Vector<>(List.of("a"));
      assertTrue(Utility.getIntersectionVector(v1, v2).isEmpty());
    }
  }

  @Nested
  @DisplayName("getVectorToString")
  class GetVectorToString {
    @Test void testMultiple() {
      Vector<String> v = new Vector<>(List.of("a", "b", "c"));
      assertEquals(COMMA_SEPARATED_ABC, Utility.getVectorToString(v));
    }
    @Test void testEmpty() {
      assertEquals("", Utility.getVectorToString(new Vector<>()));
    }
    @Test void testSingle() {
      assertEquals("x", Utility.getVectorToString(new Vector<>(List.of("x"))));
    }
  }

  // ── isElementInList ──────────────────────────────────────────────────

  @Nested
  @DisplayName("isElementInList")
  class IsElementInList {
    @Test void testFound() {
      assertTrue(Utility.isElementInList("(a, b, c)", "b"));
    }
    @Test void testNotFound() {
      assertFalse(Utility.isElementInList("(a, b, c)", "d"));
    }
    @Test void testWithQuotes() {
      assertTrue(Utility.isElementInList(QUOTED_AB_LIST, "a"));
    }
    @Test void testElementWithQuote() {
      assertTrue(Utility.isElementInList(QUOTED_AB_LIST, "'b'"));
    }
  }

  // ── stringList ───────────────────────────────────────────────────────

  @Nested
  @DisplayName("stringList")
  class StringList {
    @Test void testQuotesUnquoted() {
      assertEquals("'a', 'b', 'c'", Utility.stringList(COMMA_SEPARATED_ABC));
    }
    @Test void testPreservesQuoted() {
      assertEquals("'a', 'b'", Utility.stringList("'a', 'b'"));
    }
    @Test void testWithBrackets() {
      assertEquals(QUOTED_AB_LIST, Utility.stringList("(a, b)"));
    }
  }

  // ── getInStrList ─────────────────────────────────────────────────────

  @Nested
  @DisplayName("getInStrList")
  class GetInStrList {
    @Test void testMultipleObjects() {
      BaseOBObject obj1 = mock(BaseOBObject.class);
      BaseOBObject obj2 = mock(BaseOBObject.class);
      when(obj1.getId()).thenReturn("ID1");
      when(obj2.getId()).thenReturn("ID2");
      List<BaseOBObject> list = new ArrayList<>();
      list.add(obj1);
      list.add(obj2);
      assertEquals("'ID1', 'ID2'", Utility.getInStrList(list));
    }

    @Test void testEmptyList() {
      assertEquals("", Utility.getInStrList(new ArrayList<>()));
    }

    @Test void testWithParentheses() {
      BaseOBObject obj = mock(BaseOBObject.class);
      when(obj.getId()).thenReturn("ID1");
      List<BaseOBObject> list = new ArrayList<>();
      list.add(obj);
      assertEquals("('ID1')", Utility.getInStrList(list, true));
    }

    @Test void testWithoutParentheses() {
      BaseOBObject obj = mock(BaseOBObject.class);
      when(obj.getId()).thenReturn("ID1");
      List<BaseOBObject> list = new ArrayList<>();
      list.add(obj);
      assertEquals("'ID1'", Utility.getInStrList(list, false));
    }
  }

  // ── focusFieldJS ─────────────────────────────────────────────────────

  @Nested
  @DisplayName("focusFieldJS")
  class FocusFieldJS {
    @Test void testContainsId() {
      String js = Utility.focusFieldJS("myField");
      assertTrue(js.contains("myField"));
      assertTrue(js.contains("setWindowElementFocus"));
    }
    @Test void testContainsFunction() {
      assertTrue(Utility.focusFieldJS("x").contains("function focusOnField()"));
    }
  }

  // ── addSystem ────────────────────────────────────────────────────────

  @Nested
  @DisplayName("addSystem")
  class AddSystem {
    @Test void testAddsZero() {
      String result = Utility.addSystem("'1','2'");
      assertTrue(result.contains("0"));
    }
    @Test void testAlreadyHasZero() {
      String result = Utility.addSystem("0,'1'");
      // Should still contain 0 (no duplicate, but present)
      assertTrue(result.contains("0"));
    }
  }

  // ── getServletInfo ───────────────────────────────────────────────────

  @Nested
  @DisplayName("getServletInfo")
  class GetServletInfo {
    @Test void testReturnsDescription() {
      Utility u = new Utility();
      assertEquals("This servlet add some functions", u.getServletInfo());
    }
  }
}
