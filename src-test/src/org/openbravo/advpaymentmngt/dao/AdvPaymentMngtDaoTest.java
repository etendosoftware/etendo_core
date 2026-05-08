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
package org.openbravo.advpaymentmngt.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openbravo.base.BaseCoreTest;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

/**
 * Unit tests for {@link AdvPaymentMngtDao}.
 * Factory methods (getNewPayment, etc.) require Hibernate entity construction
 * and are left for integration tests. This class tests constants and delegation.
 */
@DisplayName("AdvPaymentMngtDao")
public class AdvPaymentMngtDaoTest extends BaseCoreTest {

  private AdvPaymentMngtDao dao;

  @BeforeEach
  void setUpDao() {
    dao = new AdvPaymentMngtDao();
  }

  @Nested
  @DisplayName("Payment status constants")
  class StatusConstants {
    @Test
    void testAwaitingPayment() {
      assertEquals("RPAP", dao.PAYMENT_STATUS_AWAITING_PAYMENT);
    }

    @Test
    void testPaymentMade() {
      assertEquals("PPM", dao.PAYMENT_STATUS_PAYMENT_MADE);
    }

    @Test
    void testCanceled() {
      assertEquals("RPVOID", dao.PAYMENT_STATUS_CANCELED);
    }

    @Test
    void testPaymentReceived() {
      assertEquals("RPR", dao.PAYMENT_STATUS_PAYMENT_RECEIVED);
    }

    @Test
    void testAwaitingExecution() {
      assertEquals("RPAE", dao.PAYMENT_STATUS_AWAITING_EXECUTION);
    }

    @Test
    void testPaymentCleared() {
      assertEquals("RPPC", dao.PAYMENT_STATUS_PAYMENT_CLEARED);
    }

    @Test
    void testDepositNotCleared() {
      assertEquals("RDNC", dao.PAYMENT_STATUS_DEPOSIT_NOT_CLEARED);
    }

    @Test
    void testWithdrawalNotCleared() {
      assertEquals("PWNC", dao.PAYMENT_STATUS_WITHDRAWAL_NOT_CLEARED);
    }
  }

  @Nested
  @DisplayName("getObject")
  class GetObject {
    @Test
    void testDelegatesToOBDal() {
      FIN_Payment payment = mock(FIN_Payment.class);
      when(obDal.get(FIN_Payment.class, "PAY1")).thenReturn(payment);

      assertSame(payment, dao.getObject(FIN_Payment.class, "PAY1"));
      verify(obDal).get(FIN_Payment.class, "PAY1");
    }

    @Test
    void testReturnsNullWhenNotFound() {
      when(obDal.get(FIN_Payment.class, "NONEXISTENT")).thenReturn(null);
      assertEquals(null, dao.getObject(FIN_Payment.class, "NONEXISTENT"));
    }
  }
}
