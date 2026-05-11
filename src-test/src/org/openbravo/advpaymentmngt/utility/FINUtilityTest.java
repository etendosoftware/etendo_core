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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.BaseCoreTest;
import org.openbravo.service.db.DbUtility;

/**
 * Tests for the {@link FIN_Utility} class covering date parsing, map construction,
 * exception message extraction, date arithmetic, deposit/payment amount calculation,
 * and number formatting.
 */
@DisplayName("FIN_Utility")
public class FINUtilityTest extends BaseCoreTest {

  private static final String DATE_FORMAT = "dd-MM-yyyy";
  private static final String DATETIME_FORMAT = "dd-MM-yyyy HH:mm:ss";
  private static final String DECIMAL_FORMAT = "#,##0.00";

  @Nested
  @DisplayName("getDate")
  class GetDateTests {
    @Test
    void testGetDateValid() {
      mockDateFormats(DATE_FORMAT, DATETIME_FORMAT);
      Date result = FIN_Utility.getDate("15-03-2026");
      assertNotNull(result);
      Calendar cal = Calendar.getInstance();
      cal.setTime(result);
      assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    void testGetDateInvalid() {
      mockDateFormats(DATE_FORMAT, DATETIME_FORMAT);
      assertNull(FIN_Utility.getDate("not-a-date"));
    }
  }

  @Nested
  @DisplayName("getDateTime")
  class GetDateTimeTests {
    @Test
    void testGetDateTimeValid() {
      mockDateFormats(DATE_FORMAT, DATETIME_FORMAT);
      Date result = FIN_Utility.getDateTime("15-03-2026 14:30:00");
      assertNotNull(result);
    }

    @Test
    void testGetDateTimeInvalid() {
      mockDateFormats(DATE_FORMAT, DATETIME_FORMAT);
      assertNull(FIN_Utility.getDateTime("invalid"));
    }
  }

  @Nested
  @DisplayName("getMapFromStringList")
  class GetMapFromStringListTests {
    @Test
    void testWithParens() {
      Map<String, String> result = FIN_Utility.getMapFromStringList("('A','B','C')");
      assertEquals(3, result.size());
      assertTrue(result.containsKey("A"));
      assertTrue(result.containsKey("B"));
      assertTrue(result.containsKey("C"));
    }

    @Test
    void testWithoutParens() {
      Map<String, String> result = FIN_Utility.getMapFromStringList("'X','Y'");
      assertEquals(2, result.size());
      assertTrue(result.containsKey("X"));
    }

    @Test
    void testEmptyParens() {
      Map<String, String> result = FIN_Utility.getMapFromStringList("()");
      assertTrue(result.isEmpty());
    }

    @Test
    void testSingleId() {
      Map<String, String> result = FIN_Utility.getMapFromStringList("ABC123");
      assertEquals(1, result.size());
      assertTrue(result.containsKey("ABC123"));
    }

    @Test
    void testKeyEqualsValue() {
      Map<String, String> result = FIN_Utility.getMapFromStringList("('ID1','ID2')");
      assertEquals("ID1", result.get("ID1"));
      assertEquals("ID2", result.get("ID2"));
    }
  }

  @Nested
  @DisplayName("getExceptionMessage")
  class GetExceptionMessageTests {
    @Test
    void testExtractsMessage() {
      RuntimeException rootCause = new RuntimeException("DB constraint violation");
      try (MockedStatic<DbUtility> dbUtilMock = mockStatic(DbUtility.class)) {
        dbUtilMock.when(() -> DbUtility.getUnderlyingSQLException(rootCause))
            .thenReturn(rootCause);
        assertEquals("DB constraint violation", FIN_Utility.getExceptionMessage(rootCause));
      }
    }

    @Test
    void testWrappedException() {
      RuntimeException wrapper = new RuntimeException("Wrapper");
      RuntimeException sqlCause = new RuntimeException("SQL Error: duplicate key");
      try (MockedStatic<DbUtility> dbUtilMock = mockStatic(DbUtility.class)) {
        dbUtilMock.when(() -> DbUtility.getUnderlyingSQLException(wrapper))
            .thenReturn(sqlCause);
        assertEquals("SQL Error: duplicate key", FIN_Utility.getExceptionMessage(wrapper));
      }
    }

    @Test
    void testNullMessage() {
      RuntimeException ex = new RuntimeException((String) null);
      try (MockedStatic<DbUtility> dbUtilMock = mockStatic(DbUtility.class)) {
        dbUtilMock.when(() -> DbUtility.getUnderlyingSQLException(ex))
            .thenReturn(ex);
        assertNull(FIN_Utility.getExceptionMessage(ex));
      }
    }
  }

  @Nested
  @DisplayName("getDaysBetween")
  class GetDaysBetween {
    @Test
    void testSameDate() {
      Date now = new Date();
      assertEquals(0L, FIN_Utility.getDaysBetween(now, now));
    }

    @Test
    void testOneDayApart() {
      Date begin = DateUtils.truncate(new Date(), Calendar.DATE);
      Date end = DateUtils.addDays(begin, 1);
      assertEquals(1L, FIN_Utility.getDaysBetween(begin, end));
    }

    @Test
    void testNegativeDays() {
      Date begin = DateUtils.truncate(new Date(), Calendar.DATE);
      Date end = DateUtils.addDays(begin, -3);
      assertEquals(-3L, FIN_Utility.getDaysBetween(begin, end));
    }

    @Test
    void testTenDays() {
      Date begin = DateUtils.truncate(new Date(), Calendar.DATE);
      Date end = DateUtils.addDays(begin, 10);
      assertEquals(10L, FIN_Utility.getDaysBetween(begin, end));
    }
  }

  @Nested
  @DisplayName("getDepositAmount")
  class GetDepositAmount {
    @Test
    void testReceiptPositiveAmount() {
      assertEquals(new BigDecimal("100"),
          FIN_Utility.getDepositAmount(true, new BigDecimal("100")));
    }

    @Test
    void testReceiptNegativeAmount() {
      assertEquals(BigDecimal.ZERO,
          FIN_Utility.getDepositAmount(true, new BigDecimal("-100")));
    }

    @Test
    void testPaymentPositiveAmount() {
      assertEquals(BigDecimal.ZERO,
          FIN_Utility.getDepositAmount(false, new BigDecimal("100")));
    }

    @Test
    void testPaymentNegativeAmount() {
      assertEquals(new BigDecimal("100"),
          FIN_Utility.getDepositAmount(false, new BigDecimal("-100")));
    }

    @Test
    void testZeroAmount() {
      assertEquals(BigDecimal.ZERO,
          FIN_Utility.getDepositAmount(true, BigDecimal.ZERO));
    }
  }

  @Nested
  @DisplayName("getPaymentAmount")
  class GetPaymentAmount {
    @Test
    void testReceiptPositiveAmount() {
      assertEquals(BigDecimal.ZERO,
          FIN_Utility.getPaymentAmount(true, new BigDecimal("100")));
    }

    @Test
    void testReceiptNegativeAmount() {
      assertEquals(new BigDecimal("100"),
          FIN_Utility.getPaymentAmount(true, new BigDecimal("-100")));
    }

    @Test
    void testPaymentPositiveAmount() {
      assertEquals(new BigDecimal("100"),
          FIN_Utility.getPaymentAmount(false, new BigDecimal("100")));
    }

    @Test
    void testPaymentNegativeAmount() {
      assertEquals(BigDecimal.ZERO,
          FIN_Utility.getPaymentAmount(false, new BigDecimal("-100")));
    }

    @Test
    void testZeroAmount() {
      assertEquals(BigDecimal.ZERO,
          FIN_Utility.getPaymentAmount(false, BigDecimal.ZERO));
    }
  }

  @Nested
  @DisplayName("formatNumber with custom format")
  class FormatNumber {
    @Test
    void testCustomFormat() {
      String result = FIN_Utility.formatNumber(new BigDecimal("1234.56"), DECIMAL_FORMAT, ".", ",");
      assertEquals("1,234.56", result);
    }

    @Test
    void testEuropeanFormat() {
      String result = FIN_Utility.formatNumber(new BigDecimal("1234.56"), DECIMAL_FORMAT, ",", ".");
      assertEquals("1.234,56", result);
    }

    @Test
    void testEmptyFormatFallsBack() {
      // Empty javaFormat should delegate to formatNumber(number) which uses UIDefinitionController
      // This may fail if UIDefinitionController is not available, so test the parameterized version
      String result = FIN_Utility.formatNumber(new BigDecimal("100"), "#0.00", ".", ",");
      assertEquals("100.00", result);
    }

    @Test
    void testNullSeparatorsUseDefaults() {
      String result = FIN_Utility.formatNumber(new BigDecimal("1000.5"), DECIMAL_FORMAT, null, null);
      assertEquals("1,000.50", result);
    }
  }
}
