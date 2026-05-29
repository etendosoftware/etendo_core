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

import java.util.ArrayList;
import java.util.Vector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for pure static methods in {@link Utility} that have no database
 * or framework dependencies.
 */
@SuppressWarnings({"java:S1149"})
@DisplayName("Utility pure methods")
public class UtilityPureMethodsTest {

  private static final String CSV_ABC = "a,b,c";

  // ── isID ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("isID")
  class IsID {
    @Test void referenceThirteenIsID() { assertTrue(Utility.isID("13")); }
    @Test void referenceTwelveIsNotID() { assertFalse(Utility.isID("12")); }
    @Test void nullIsNotID() { assertFalse(Utility.isID(null)); }
    @Test void emptyIsNotID() { assertFalse(Utility.isID("")); }
  }

  // ── isDecimalNumber ──────────────────────────────────────────────

  @Nested
  @DisplayName("isDecimalNumber")
  class IsDecimalNumber {
    @Test void ref12IsDecimal() { assertTrue(Utility.isDecimalNumber("12")); }
    @Test void ref22IsDecimal() { assertTrue(Utility.isDecimalNumber("22")); }
    @Test void ref29IsDecimal() { assertTrue(Utility.isDecimalNumber("29")); }
    @Test void ref80008IsDecimal() { assertTrue(Utility.isDecimalNumber("80008")); }
    @Test void ref11IsNotDecimal() { assertFalse(Utility.isDecimalNumber("11")); }
    @Test void nullIsNotDecimal() { assertFalse(Utility.isDecimalNumber(null)); }
    @Test void emptyIsNotDecimal() { assertFalse(Utility.isDecimalNumber("")); }
  }

  // ── isIntegerNumber ──────────────────────────────────────────────

  @Nested
  @DisplayName("isIntegerNumber")
  class IsIntegerNumber {
    @Test void ref11IsInteger() { assertTrue(Utility.isIntegerNumber("11")); }
    @Test void ref13IsInteger() { assertTrue(Utility.isIntegerNumber("13")); }
    @Test void ref25IsInteger() { assertTrue(Utility.isIntegerNumber("25")); }
    @Test void ref12IsNotInteger() { assertFalse(Utility.isIntegerNumber("12")); }
    @Test void nullIsNotInteger() { assertFalse(Utility.isIntegerNumber(null)); }
    @Test void emptyIsNotInteger() { assertFalse(Utility.isIntegerNumber("")); }
  }

  // ── isDateTime ───────────────────────────────────────────────────

  @Nested
  @DisplayName("isDateTime")
  class IsDateTime {
    @Test void ref15IsDateTime() { assertTrue(Utility.isDateTime("15")); }
    @Test void ref16IsDateTime() { assertTrue(Utility.isDateTime("16")); }
    @Test void ref24IsDateTime() { assertTrue(Utility.isDateTime("24")); }
    @Test void ref12IsNotDateTime() { assertFalse(Utility.isDateTime("12")); }
    @Test void nullIsNotDateTime() { assertFalse(Utility.isDateTime(null)); }
    @Test void emptyIsNotDateTime() { assertFalse(Utility.isDateTime("")); }
  }

  // ── isNumericParameter ───────────────────────────────────────────

  @Nested
  @DisplayName("isNumericParameter")
  class IsNumericParameter {
    @Test void ref12IsNumeric() { assertTrue(Utility.isNumericParameter("12")); }
    @Test void ref11IsNumeric() { assertTrue(Utility.isNumericParameter("11")); }
    @Test void ref13IsNotNumericBecauseIsID() { assertFalse(Utility.isNumericParameter("13")); }
    @Test void nullIsNotNumeric() { assertFalse(Utility.isNumericParameter(null)); }
  }

  // ── getStringVector ──────────────────────────────────────────────

  @Nested
  @DisplayName("getStringVector")
  class GetStringVector {
    @Test
    @DisplayName("splits comma-separated string")
    void splitsCsv() {
      Vector<String> v = Utility.getStringVector(CSV_ABC);
      assertEquals(3, v.size());
      assertEquals("a", v.get(0));
      assertEquals("b", v.get(1));
      assertEquals("c", v.get(2));
    }

    @Test
    @DisplayName("trims whitespace")
    void trimsWhitespace() {
      Vector<String> v = Utility.getStringVector(" a , b , c ");
      assertEquals("a", v.get(0));
    }

    @Test
    @DisplayName("removes duplicates")
    void removesDuplicates() {
      Vector<String> v = Utility.getStringVector("a,b,a,c");
      assertEquals(3, v.size());
    }
  }

  // ── getIntersectionVector ────────────────────────────────────────

  @Nested
  @DisplayName("getIntersectionVector")
  class GetIntersectionVector {
    @Test
    @DisplayName("returns common elements")
    void returnsCommon() {
      Vector<String> v1 = new Vector<>();
      v1.add("a"); v1.add("b"); v1.add("c");
      Vector<String> v2 = new Vector<>();
      v2.add("b"); v2.add("c"); v2.add("d");

      Vector<String> result = Utility.getIntersectionVector(v1, v2);
      assertEquals(2, result.size());
      assertTrue(result.contains("b"));
      assertTrue(result.contains("c"));
    }

    @Test
    @DisplayName("no common returns empty")
    void noCommon() {
      Vector<String> v1 = new Vector<>();
      v1.add("a");
      Vector<String> v2 = new Vector<>();
      v2.add("b");
      assertEquals(0, Utility.getIntersectionVector(v1, v2).size());
    }
  }

  // ── getVectorToString ────────────────────────────────────────────

  @Nested
  @DisplayName("getVectorToString")
  class GetVectorToString {
    @Test
    @DisplayName("joins with comma-space")
    void joinsWithComma() {
      Vector<String> v = new Vector<>();
      v.add("a"); v.add("b"); v.add("c");
      assertEquals("a, b, c", Utility.getVectorToString(v));
    }

    @Test
    @DisplayName("empty vector returns empty string")
    void emptyVector() {
      assertEquals("", Utility.getVectorToString(new Vector<>()));
    }
  }

  // ── isElementInList ──────────────────────────────────────────────

  @Nested
  @DisplayName("isElementInList")
  class IsElementInList {
    @Test void findsElement() {
      assertTrue(Utility.isElementInList("('a','b','c')", "b"));
    }
    @Test void notFound() {
      assertFalse(Utility.isElementInList("('a','b','c')", "x"));
    }
    @Test void withoutParentheses() {
      assertTrue(Utility.isElementInList(CSV_ABC, "b"));
    }
    @Test void quotedElement() {
      assertTrue(Utility.isElementInList("'a','b','c'", "'b'"));
    }
  }

  // ── stringList ───────────────────────────────────────────────────

  @Nested
  @DisplayName("stringList")
  class StringList {
    @Test
    @DisplayName("quotes unquoted items")
    void quotesUnquoted() {
      assertEquals("'a', 'b', 'c'", Utility.stringList(CSV_ABC));
    }

    @Test
    @DisplayName("preserves already quoted items")
    void preservesQuoted() {
      assertEquals("'a', 'b'", Utility.stringList("'a','b'"));
    }

    @Test
    @DisplayName("handles brackets")
    void handlesBrackets() {
      String result = Utility.stringList("(a,b)");
      assertTrue(result.startsWith("("));
      assertTrue(result.endsWith(")"));
    }
  }

  // ── isUUIDString / isHexStringChar ───────────────────────────────

  @Nested
  @DisplayName("isUUIDString")
  class IsUUIDString {
    @Test void valid32HexIsUUID() {
      assertTrue(Utility.isUUIDString("0123456789ABCDEF0123456789abcdef"));
    }
    @Test void shortStringIsNotUUID() {
      assertFalse(Utility.isUUIDString("0123456789"));
    }
    @Test void nonHexCharsIsNotUUID() {
      assertFalse(Utility.isUUIDString("0123456789ABCDEF0123456789GHIJKL"));
    }
  }

  @Nested
  @DisplayName("isHexStringChar")
  class IsHexStringChar {
    @Test void digitsAreHex() { assertTrue(Utility.isHexStringChar('0')); }
    @Test void lowerAFAreHex() { assertTrue(Utility.isHexStringChar('a')); }
    @Test void upperAFAreHex() { assertTrue(Utility.isHexStringChar('F')); }
    @Test void gIsNotHex() { assertFalse(Utility.isHexStringChar('g')); }
    @Test void spaceIsNotHex() { assertFalse(Utility.isHexStringChar(' ')); }
  }

  // ── isBigDecimal ─────────────────────────────────────────────────

  @Nested
  @DisplayName("isBigDecimal")
  class IsBigDecimal {
    @Test void validNumber() { assertTrue(Utility.isBigDecimal("123.456")); }
    @Test void integerNumber() { assertTrue(Utility.isBigDecimal("42")); }
    @Test void negativeNumber() { assertTrue(Utility.isBigDecimal("-99.9")); }
    @Test void notANumber() { assertFalse(Utility.isBigDecimal("abc")); }
    @Test void withSpaces() { assertTrue(Utility.isBigDecimal(" 42 ")); }
  }

  // ── stringToArrayList ────────────────────────────────────────────

  @Nested
  @DisplayName("stringToArrayList")
  class StringToArrayList {
    @Test
    @DisplayName("splits comma-separated string")
    void splitsString() {
      ArrayList<String> result = Utility.stringToArrayList(CSV_ABC);
      assertEquals(3, result.size());
      assertEquals("a", result.get(0));
    }

    @Test
    @DisplayName("null array returns empty list")
    void nullArray() {
      ArrayList<String> result = Utility.stringToArrayList((String[]) null);
      assertEquals(0, result.size());
    }

    @Test
    @DisplayName("string array to list")
    void arrayToList() {
      ArrayList<String> result = Utility.stringToArrayList(new String[]{"x", "y"});
      assertEquals(2, result.size());
    }
  }

  // ── focusFieldJS ─────────────────────────────────────────────────

  @Nested
  @DisplayName("focusFieldJS")
  class FocusFieldJS {
    @Test
    @DisplayName("generates JS with element id")
    void generatesJS() {
      String result = Utility.focusFieldJS("myField");
      assertTrue(result.contains("myField"));
      assertTrue(result.contains("setWindowElementFocus"));
    }
  }

  // ── addSystem ────────────────────────────────────────────────────

  @Nested
  @DisplayName("addSystem")
  class AddSystem {
    @Test
    @DisplayName("adds 0 to list without it")
    void addsZero() {
      String result = Utility.addSystem("1,2,3");
      assertTrue(result.contains("0"));
      assertTrue(result.contains("1"));
    }

    @Test
    @DisplayName("does not duplicate 0 if already present")
    void doesNotDuplicate() {
      String result = Utility.addSystem("0,1,2");
      long countZero = result.chars().filter(c -> c == '0').count();
      // 0 should appear exactly once
      assertEquals(1, countZero);
    }
  }

  // ── formatDate ───────────────────────────────────────────────────

  @Nested
  @DisplayName("formatDate")
  class FormatDate {
    @Test
    @DisplayName("formats date with pattern")
    void formatsDate() {
      java.util.Calendar cal = java.util.Calendar.getInstance();
      cal.set(2025, java.util.Calendar.MARCH, 15);
      String result = Utility.formatDate(cal.getTime(), "yyyy-MM-dd");
      assertEquals("2025-03-15", result);
    }
  }
}
