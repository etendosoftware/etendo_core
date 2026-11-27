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
package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openbravo.model.common.currency.Currency;

/**
 * Unit tests for {@link SetNewBPCurrency}.
 */
@SuppressWarnings({ "java:S101" })
@DisplayName("SetNewBPCurrency")
public class SetNewBPCurrencyTest {

  private static final BigDecimal RATE = new BigDecimal("0.000667");

  /**
   * Regression tests for the Credit Line Limit conversion performed when the currency of a Business
   * Partner changes.
   */
  @Nested
  @DisplayName("convertCreditLimit")
  class ConvertCreditLimit {

    private Currency usd;

    @BeforeEach
    void setUp() {
      usd = mock(Currency.class);
      when(usd.getStandardPrecision()).thenReturn(2L);
    }

    @Test
    @DisplayName("converts the limit with the rate and the destination currency precision")
    void convertsLimitWithRate() {
      BigDecimal result = SetNewBPCurrency.convertCreditLimit(new BigDecimal("1000000"), RATE, usd);

      assertEquals(new BigDecimal("667.00"), result);
    }

    @Test
    @DisplayName("returns null when the limit is null")
    void returnsNullWhenLimitIsNull() {
      assertNull(SetNewBPCurrency.convertCreditLimit(null, RATE, usd));
    }

    @Test
    @DisplayName("returns the same zero limit unchanged")
    void returnsSameInstanceWhenLimitIsZero() {
      BigDecimal limit = BigDecimal.ZERO;

      assertSame(limit, SetNewBPCurrency.convertCreditLimit(limit, RATE, usd));
    }

    @Test
    @DisplayName("returns the limit unchanged when the rate is null")
    void returnsLimitWhenRateIsNull() {
      BigDecimal limit = new BigDecimal("1000");

      assertSame(limit, SetNewBPCurrency.convertCreditLimit(limit, null, usd));
    }

    @Test
    @DisplayName("returns the limit unchanged when the rate is zero")
    void returnsLimitWhenRateIsZero() {
      BigDecimal limit = new BigDecimal("1000");

      assertSame(limit, SetNewBPCurrency.convertCreditLimit(limit, BigDecimal.ZERO, usd));
    }

    @Test
    @DisplayName("rounds half up to the standard precision of the destination currency")
    void roundsHalfUp() {
      // 1.005 * 1 = 1.005 -> 1.01 with HALF_UP (HALF_EVEN would give 1.00)
      assertEquals(new BigDecimal("1.01"),
          SetNewBPCurrency.convertCreditLimit(new BigDecimal("1.005"), BigDecimal.ONE, usd));
      // 2.004 -> 2.00
      assertEquals(new BigDecimal("2.00"),
          SetNewBPCurrency.convertCreditLimit(new BigDecimal("2.004"), BigDecimal.ONE, usd));
    }

    @Test
    @DisplayName("uses the precision of the destination currency")
    void usesDestinationPrecision() {
      Currency yen = mock(Currency.class);
      when(yen.getStandardPrecision()).thenReturn(0L);

      assertEquals(new BigDecimal("1500"),
          SetNewBPCurrency.convertCreditLimit(new BigDecimal("1000"), new BigDecimal("1.5"), yen));
    }
  }
}
