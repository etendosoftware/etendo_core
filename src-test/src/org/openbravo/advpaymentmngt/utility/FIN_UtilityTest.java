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
package org.openbravo.advpaymentmngt.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FIN_Utility}.
 */
@DisplayName("FIN_Utility")
public class FIN_UtilityTest {

  // ── getMapFromStringList ─────────────────────────────────────────────

  @Nested
  @DisplayName("getMapFromStringList")
  class GetMapFromStringList {

    @Test
    @DisplayName("parses parenthesized quoted IDs")
    void parsesParenthesizedQuotedIds() {
      Map<String, String> result = FIN_Utility.getMapFromStringList("('id1','id2','id3')");
      assertEquals(3, result.size());
      assertEquals("id1", result.get("id1"));
      assertEquals("id2", result.get("id2"));
      assertEquals("id3", result.get("id3"));
    }

    @Test
    @DisplayName("parses plain comma-separated IDs")
    void parsesPlainCommaSeparated() {
      Map<String, String> result = FIN_Utility.getMapFromStringList("id1,id2");
      assertEquals(2, result.size());
      assertEquals("id1", result.get("id1"));
      assertEquals("id2", result.get("id2"));
    }

    @Test
    @DisplayName("empty parentheses returns empty map")
    void emptyParentheses() {
      Map<String, String> result = FIN_Utility.getMapFromStringList("()");
      assertTrue(result.isEmpty());
    }
  }

  // ── getDaysBetween ───────────────────────────────────────────────────

  @Nested
  @DisplayName("getDaysBetween")
  class GetDaysBetween {

    @Test
    @DisplayName("same date returns 0")
    void sameDateReturnsZero() {
      Date now = new Date();
      assertEquals(0L, FIN_Utility.getDaysBetween(now, now));
    }

    @Test
    @DisplayName("dates 5 days apart returns 5")
    void fiveDaysApart() {
      Calendar cal = Calendar.getInstance();
      cal.set(2025, Calendar.JANUARY, 1, 12, 0, 0);
      Date begin = cal.getTime();
      cal.add(Calendar.DAY_OF_MONTH, 5);
      Date end = cal.getTime();
      assertEquals(5L, FIN_Utility.getDaysBetween(begin, end));
    }

    @Test
    @DisplayName("end before begin returns negative")
    void endBeforeBeginReturnsNegative() {
      Calendar cal = Calendar.getInstance();
      cal.set(2025, Calendar.JANUARY, 10, 12, 0, 0);
      Date begin = cal.getTime();
      cal.set(2025, Calendar.JANUARY, 7, 12, 0, 0);
      Date end = cal.getTime();
      assertEquals(-3L, FIN_Utility.getDaysBetween(begin, end));
    }
  }

  // ── getDepositAmount ─────────────────────────────────────────────────

  @Nested
  @DisplayName("getDepositAmount")
  class GetDepositAmount {

    @Test
    @DisplayName("receipt with positive amount returns amount")
    void receiptPositive() {
      BigDecimal amt = new BigDecimal("100.00");
      assertEquals(amt, FIN_Utility.getDepositAmount(true, amt));
    }

    @Test
    @DisplayName("receipt with negative amount returns ZERO")
    void receiptNegative() {
      assertEquals(BigDecimal.ZERO,
          FIN_Utility.getDepositAmount(true, new BigDecimal("-50.00")));
    }

    @Test
    @DisplayName("receipt with zero returns ZERO")
    void receiptZero() {
      assertEquals(BigDecimal.ZERO, FIN_Utility.getDepositAmount(true, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("payment with negative amount returns absolute value")
    void paymentNegative() {
      assertEquals(new BigDecimal("50.00"),
          FIN_Utility.getDepositAmount(false, new BigDecimal("-50.00")));
    }

    @Test
    @DisplayName("payment with positive amount returns ZERO")
    void paymentPositive() {
      assertEquals(BigDecimal.ZERO,
          FIN_Utility.getDepositAmount(false, new BigDecimal("100.00")));
    }
  }

  // ── getPaymentAmount ─────────────────────────────────────────────────

  @Nested
  @DisplayName("getPaymentAmount")
  class GetPaymentAmount {

    @Test
    @DisplayName("receipt with negative returns absolute value")
    void receiptNegative() {
      assertEquals(new BigDecimal("50.00"),
          FIN_Utility.getPaymentAmount(true, new BigDecimal("-50.00")));
    }

    @Test
    @DisplayName("receipt with positive returns ZERO")
    void receiptPositive() {
      assertEquals(BigDecimal.ZERO,
          FIN_Utility.getPaymentAmount(true, new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("payment with positive returns amount")
    void paymentPositive() {
      BigDecimal amt = new BigDecimal("200.00");
      assertEquals(amt, FIN_Utility.getPaymentAmount(false, amt));
    }

    @Test
    @DisplayName("payment with negative returns ZERO")
    void paymentNegative() {
      assertEquals(BigDecimal.ZERO,
          FIN_Utility.getPaymentAmount(false, new BigDecimal("-100.00")));
    }
  }

  // ── formatNumber(BigDecimal, String, String, String) ─────────────────

  @Nested
  @DisplayName("formatNumber (4-arg)")
  class FormatNumber {

    @Test
    @DisplayName("formats with standard pattern")
    void formatsWithStandardPattern() {
      String result = FIN_Utility.formatNumber(
          new BigDecimal("1234567.89"), "#,##0.00", ".", ",");
      assertEquals("1,234,567.89", result);
    }

    @Test
    @DisplayName("null separators default to dot and comma")
    void nullSeparatorsDefault() {
      String result = FIN_Utility.formatNumber(
          new BigDecimal("1000.50"), "#,##0.00", null, null);
      assertEquals("1,000.50", result);
    }

    @Test
    @DisplayName("zero formats correctly")
    void zeroFormats() {
      String result = FIN_Utility.formatNumber(
          BigDecimal.ZERO, "#,##0.00", ".", ",");
      assertEquals("0.00", result);
    }
  }

  // ── getFirstNonEmpty ─────────────────────────────────────────────────

  @Nested
  @DisplayName("getFirstNonEmpty")
  class GetFirstNonEmpty {

    @Test
    @DisplayName("returns first matching key value")
    void returnsFirstMatch() throws Exception {
      JSONObject json = new JSONObject();
      json.put("key1", "value1");
      json.put("key2", "value2");
      assertEquals("value1", FIN_Utility.getFirstNonEmpty(json, "key1", "key2"));
    }

    @Test
    @DisplayName("skips blank and returns second")
    void skipsBlankReturnsSecond() throws Exception {
      JSONObject json = new JSONObject();
      json.put("key1", "");
      json.put("key2", "value2");
      assertEquals("value2", FIN_Utility.getFirstNonEmpty(json, "key1", "key2"));
    }

    @Test
    @DisplayName("returns null when no keys match")
    void returnsNullWhenNoMatch() throws Exception {
      JSONObject json = new JSONObject();
      assertNull(FIN_Utility.getFirstNonEmpty(json, "missing1", "missing2"));
    }
  }

  // ── getDefaultAddPaymentDocument ─────────────────────────────────────

  @Nested
  @DisplayName("getDefaultAddPaymentDocument")
  class GetDefaultAddPaymentDocument {

    @Test
    @DisplayName("BPD returns RCIN")
    void bpdReturnsRCIN() throws Exception {
      JSONObject json = new JSONObject();
      json.put("trxtype", "BPD");
      assertEquals("RCIN", FIN_Utility.getDefaultAddPaymentDocument(json));
    }

    @Test
    @DisplayName("BPW returns PDOUT")
    void bpwReturnsPDOUT() throws Exception {
      JSONObject json = new JSONObject();
      json.put("trxtype", "BPW");
      assertEquals("PDOUT", FIN_Utility.getDefaultAddPaymentDocument(json));
    }

    @Test
    @DisplayName("unknown type returns empty")
    void unknownReturnsEmpty() throws Exception {
      JSONObject json = new JSONObject();
      json.put("trxtype", "OTHER");
      assertEquals("", FIN_Utility.getDefaultAddPaymentDocument(json));
    }

    @Test
    @DisplayName("no trxtype returns empty")
    void noTrxTypeReturnsEmpty() throws Exception {
      JSONObject json = new JSONObject();
      assertEquals("", FIN_Utility.getDefaultAddPaymentDocument(json));
    }
  }

  // ── escape (private) ─────────────────────────────────────────────────

  @Nested
  @DisplayName("escape (private)")
  class Escape {

    private String invokeEscape(String input) throws Exception {
      Method method = FIN_Utility.class.getDeclaredMethod("escape", String.class);
      method.setAccessible(true);
      return (String) method.invoke(null, input);
    }

    @Test
    @DisplayName("replaces > and <")
    void replacesAngleBrackets() throws Exception {
      assertEquals("&lt;b&gt;text&lt;/b&gt;", invokeEscape("<b>text</b>"));
    }

    @Test
    @DisplayName("no special chars returns unchanged")
    void noSpecialChars() throws Exception {
      assertEquals("normal text", invokeEscape("normal text"));
    }

    @Test
    @DisplayName("replaces only >")
    void replacesGreaterThan() throws Exception {
      assertEquals("a &gt; b", invokeEscape("a > b"));
    }

    @Test
    @DisplayName("replaces only <")
    void replacesLessThan() throws Exception {
      assertEquals("a &lt; b", invokeEscape("a < b"));
    }
  }
}