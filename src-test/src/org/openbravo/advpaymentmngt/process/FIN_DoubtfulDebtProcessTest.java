package org.openbravo.advpaymentmngt.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;

import static org.mockito.Mockito.when;

/**
 * Tests for {@link FIN_DoubtfulDebtProcess}.
 * Tests the private getDifferenceOfAmountsOrZero utility method.
 */
@RunWith(MockitoJUnitRunner.class)
public class FIN_DoubtfulDebtProcessTest {

  private FIN_DoubtfulDebtProcess instance;

  @Mock
  private FIN_PaymentScheduleDetail mockPsd;

  @Before
  public void setUp() {
    instance = new FIN_DoubtfulDebtProcess();
  }

  // --- Tests for getDifferenceOfAmountsOrZero ---

  @Test
  public void testGetDifferenceReturnsPositiveDifference() throws Exception {
    // debtAmount=100, psd.amount=30 => 100-30=70 > 0 => returns 70
    when(mockPsd.getAmount()).thenReturn(new BigDecimal("30"));
    BigDecimal result = invokeGetDifference(new BigDecimal("100"), mockPsd);
    assertEquals(new BigDecimal("70"), result);
  }

  @Test
  public void testGetDifferenceReturnsZeroWhenEqual() throws Exception {
    // debtAmount=50, psd.amount=50 => 50-50=0 => not < 0, returns 0
    when(mockPsd.getAmount()).thenReturn(new BigDecimal("50"));
    BigDecimal result = invokeGetDifference(new BigDecimal("50"), mockPsd);
    assertTrue(BigDecimal.ZERO.compareTo(result) == 0);
  }

  @Test
  public void testGetDifferenceReturnsZeroWhenNegative() throws Exception {
    // debtAmount=20, psd.amount=50 => 20-50=-30 < 0 => returns ZERO
    when(mockPsd.getAmount()).thenReturn(new BigDecimal("50"));
    BigDecimal result = invokeGetDifference(new BigDecimal("20"), mockPsd);
    assertTrue(BigDecimal.ZERO.compareTo(result) == 0);
  }

  @Test
  public void testGetDifferenceWithDecimalAmounts() throws Exception {
    // debtAmount=100.50, psd.amount=30.25 => 70.25
    when(mockPsd.getAmount()).thenReturn(new BigDecimal("30.25"));
    BigDecimal result = invokeGetDifference(new BigDecimal("100.50"), mockPsd);
    assertEquals(new BigDecimal("70.25"), result);
  }

  @Test
  public void testGetDifferenceWithSmallDebtReturnsZero() throws Exception {
    // debtAmount=0.01, psd.amount=0.02 => -0.01 < 0 => returns ZERO
    when(mockPsd.getAmount()).thenReturn(new BigDecimal("0.02"));
    BigDecimal result = invokeGetDifference(new BigDecimal("0.01"), mockPsd);
    assertTrue(BigDecimal.ZERO.compareTo(result) == 0);
  }

  @Test
  public void testGetDifferenceWithLargeAmounts() throws Exception {
    // debtAmount=999999.99, psd.amount=1.01 => 999998.98
    when(mockPsd.getAmount()).thenReturn(new BigDecimal("1.01"));
    BigDecimal result = invokeGetDifference(new BigDecimal("999999.99"), mockPsd);
    assertEquals(new BigDecimal("999998.98"), result);
  }

  @Test
  public void testGetDifferenceWithZeroDebtReturnsZero() throws Exception {
    // debtAmount=0, psd.amount=100 => -100 < 0 => returns ZERO
    when(mockPsd.getAmount()).thenReturn(new BigDecimal("100"));
    BigDecimal result = invokeGetDifference(BigDecimal.ZERO, mockPsd);
    assertTrue(BigDecimal.ZERO.compareTo(result) == 0);
  }

  // --- Helper ---

  private BigDecimal invokeGetDifference(BigDecimal debtAmount, FIN_PaymentScheduleDetail psd)
      throws Exception {
    Method method = FIN_DoubtfulDebtProcess.class.getDeclaredMethod(
        "getDifferenceOfAmountsOrZero", BigDecimal.class, FIN_PaymentScheduleDetail.class);
    method.setAccessible(true);
    return (BigDecimal) method.invoke(instance, debtAmount, psd);
  }
}
