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
package org.openbravo.advpaymentmngt.process;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

/**
 * Tests for {@link FIN_PaymentProcess}.
 */
@SuppressWarnings({"java:S101", "java:S112"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class FIN_PaymentProcessTest {

  private FIN_PaymentProcess instance;

  @Mock
  private FIN_Payment mockPayment;

  @Mock
  private FIN_Payment mockReversedPayment;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(FIN_PaymentProcess.class);
  }

  /**
   * When action is RV and reversedPayment is not null, an exception should be thrown.
   * @throws Exception if reflection setup fails
   */
  @Test
  public void testThrowExceptionWhenActionIsRVAndReversedPaymentIsNotNull() throws Exception {
    when(mockPayment.getReversedPayment()).thenReturn(mockReversedPayment);

    Method method = FIN_PaymentProcess.class.getDeclaredMethod(
        "throwExceptionIfPaymentIsAlreadyReversed", String.class, FIN_Payment.class);
    method.setAccessible(true);

    try {
      method.invoke(instance, "RV", mockPayment);
      fail("Expected a RuntimeException to be thrown");
    } catch (InvocationTargetException e) {
      assertNotNull("Exception cause should not be null", e.getCause());
    }
  }

  /**
   * When action is RV and reversedPayment is null, no exception.
   * @throws Exception if reflection setup fails
   */
  @Test
  public void testNoExceptionWhenActionIsRVAndReversedPaymentIsNull() throws Exception {
    when(mockPayment.getReversedPayment()).thenReturn(null);

    Method method = FIN_PaymentProcess.class.getDeclaredMethod(
        "throwExceptionIfPaymentIsAlreadyReversed", String.class, FIN_Payment.class);
    method.setAccessible(true);

    method.invoke(instance, "RV", mockPayment);
  }

  /**
   * When action is not RV, no exception regardless of reversedPayment.
   * @throws Exception if reflection setup fails
   */
  @Test
  public void testNoExceptionWhenActionIsNotRV() throws Exception {
    when(mockPayment.getReversedPayment()).thenReturn(mockReversedPayment);

    Method method = FIN_PaymentProcess.class.getDeclaredMethod(
        "throwExceptionIfPaymentIsAlreadyReversed", String.class, FIN_Payment.class);
    method.setAccessible(true);

    method.invoke(instance, "P", mockPayment);
  }

  /**
   * When action is null, no exception.
   * @throws Exception if reflection setup fails
   */
  @Test
  public void testNoExceptionWhenActionIsNull() throws Exception {
    when(mockPayment.getReversedPayment()).thenReturn(mockReversedPayment);

    Method method = FIN_PaymentProcess.class.getDeclaredMethod(
        "throwExceptionIfPaymentIsAlreadyReversed", String.class, FIN_Payment.class);
    method.setAccessible(true);

    method.invoke(instance, null, mockPayment);
  }

}
