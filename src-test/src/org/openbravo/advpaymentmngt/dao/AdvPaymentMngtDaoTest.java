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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.BaseCoreTest;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

/**
 * Unit tests for {@link AdvPaymentMngtDao}.
 * Factory methods (getNewPayment, etc.) require Hibernate entity construction
 * and are left for integration tests. This class tests constants and delegation.
 */
public class AdvPaymentMngtDaoTest extends BaseCoreTest {

  private AdvPaymentMngtDao dao;

  @Before
  public void setUpDao() {
    dao = new AdvPaymentMngtDao();
  }

  // Payment status constants

  @Test
  public void testAwaitingPayment() {
    assertEquals("RPAP", dao.PAYMENT_STATUS_AWAITING_PAYMENT);
  }

  @Test
  public void testPaymentMade() {
    assertEquals("PPM", dao.PAYMENT_STATUS_PAYMENT_MADE);
  }

  @Test
  public void testCanceled() {
    assertEquals("RPVOID", dao.PAYMENT_STATUS_CANCELED);
  }

  @Test
  public void testPaymentReceived() {
    assertEquals("RPR", dao.PAYMENT_STATUS_PAYMENT_RECEIVED);
  }

  @Test
  public void testAwaitingExecution() {
    assertEquals("RPAE", dao.PAYMENT_STATUS_AWAITING_EXECUTION);
  }

  @Test
  public void testPaymentCleared() {
    assertEquals("RPPC", dao.PAYMENT_STATUS_PAYMENT_CLEARED);
  }

  @Test
  public void testDepositNotCleared() {
    assertEquals("RDNC", dao.PAYMENT_STATUS_DEPOSIT_NOT_CLEARED);
  }

  @Test
  public void testWithdrawalNotCleared() {
    assertEquals("PWNC", dao.PAYMENT_STATUS_WITHDRAWAL_NOT_CLEARED);
  }

  // getObject

  @Test
  public void testDelegatesToOBDal() {
    FIN_Payment payment = mock(FIN_Payment.class);
    when(obDal.get(FIN_Payment.class, "PAY1")).thenReturn(payment);

    assertSame(payment, dao.getObject(FIN_Payment.class, "PAY1"));
    verify(obDal).get(FIN_Payment.class, "PAY1");
  }

  @Test
  public void testReturnsNullWhenNotFound() {
    when(obDal.get(FIN_Payment.class, "NONEXISTENT")).thenReturn(null);
    assertEquals(null, dao.getObject(FIN_Payment.class, "NONEXISTENT"));
  }
}
