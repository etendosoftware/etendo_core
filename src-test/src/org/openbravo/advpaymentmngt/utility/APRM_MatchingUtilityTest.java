package org.openbravo.advpaymentmngt.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

/**
 * Tests for APRM_MatchingUtility.
 */
@SuppressWarnings({"java:S101", "java:S112"})
@RunWith(MockitoJUnitRunner.class)
public class APRM_MatchingUtilityTest {

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBContext mockOBContext;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obContextStatic = mockStatic(OBContext.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) {
      obDalStatic.close();
    }
    if (obContextStatic != null) {
      obContextStatic.close();
    }
  }
  /** Fix mixed line receipt sets deposit not cleared. */

  @Test
  public void testFixMixedLineReceiptSetsDepositNotCleared() {
    // Arrange
    FIN_FinaccTransaction mixedLine = mock(FIN_FinaccTransaction.class);
    when(mixedLine.getDepositAmount()).thenReturn(new BigDecimal("100.00"));
    when(mixedLine.getFinPayment()).thenReturn(null);

    // Act
    APRM_MatchingUtility.fixMixedLine(mixedLine);

    // Assert
    verify(mixedLine).setStatus(APRMConstants.PAYMENT_STATUS_DEPOSIT_NOT_CLEARED);
    verify(mixedLine).setReconciliation(null);
    verify(mockOBDal).save(mixedLine);
  }
  /** Fix mixed line withdrawal sets withdrawal not cleared. */

  @Test
  public void testFixMixedLineWithdrawalSetsWithdrawalNotCleared() {
    // Arrange
    FIN_FinaccTransaction mixedLine = mock(FIN_FinaccTransaction.class);
    when(mixedLine.getDepositAmount()).thenReturn(BigDecimal.ZERO);
    when(mixedLine.getFinPayment()).thenReturn(null);

    // Act
    APRM_MatchingUtility.fixMixedLine(mixedLine);

    // Assert
    verify(mixedLine).setStatus(APRMConstants.PAYMENT_STATUS_WITHDRAWAL_NOT_CLEARED);
    verify(mixedLine).setReconciliation(null);
    verify(mockOBDal).save(mixedLine);
  }
  /** Fix mixed line receipt with payment updates payment status. */

  @Test
  public void testFixMixedLineReceiptWithPaymentUpdatesPaymentStatus() {
    // Arrange
    FIN_FinaccTransaction mixedLine = mock(FIN_FinaccTransaction.class);
    FIN_Payment payment = mock(FIN_Payment.class);
    when(mixedLine.getDepositAmount()).thenReturn(new BigDecimal("50.00"));
    when(mixedLine.getFinPayment()).thenReturn(payment);

    // Act
    APRM_MatchingUtility.fixMixedLine(mixedLine);

    // Assert
    verify(mixedLine).setStatus(APRMConstants.PAYMENT_STATUS_DEPOSIT_NOT_CLEARED);
    verify(payment).setStatus(APRMConstants.PAYMENT_STATUS_DEPOSIT_NOT_CLEARED);
    verify(mockOBDal).save(mixedLine);
    verify(mockOBDal).save(payment);
  }
  /** Fix mixed line withdrawal with payment updates payment status. */

  @Test
  public void testFixMixedLineWithdrawalWithPaymentUpdatesPaymentStatus() {
    // Arrange
    FIN_FinaccTransaction mixedLine = mock(FIN_FinaccTransaction.class);
    FIN_Payment payment = mock(FIN_Payment.class);
    when(mixedLine.getDepositAmount()).thenReturn(BigDecimal.ZERO);
    when(mixedLine.getFinPayment()).thenReturn(payment);

    // Act
    APRM_MatchingUtility.fixMixedLine(mixedLine);

    // Assert
    verify(mixedLine).setStatus(APRMConstants.PAYMENT_STATUS_WITHDRAWAL_NOT_CLEARED);
    verify(payment).setStatus(APRMConstants.PAYMENT_STATUS_WITHDRAWAL_NOT_CLEARED);
  }
  /**
   * Get matched document returns transaction when not created by algorithm.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetMatchedDocumentReturnsTransactionWhenNotCreatedByAlgorithm() throws Exception {
    // Arrange
    FIN_FinaccTransaction transaction = mock(FIN_FinaccTransaction.class);
    when(transaction.isCreatedByAlgorithm()).thenReturn(false);

    // Act
    String result = invokeGetMatchedDocument(transaction);

    // Assert
    assertEquals("T", result);
  }
  /**
   * Get matched document returns transaction when no payment.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetMatchedDocumentReturnsTransactionWhenNoPayment() throws Exception {
    // Arrange
    FIN_FinaccTransaction transaction = mock(FIN_FinaccTransaction.class);
    when(transaction.isCreatedByAlgorithm()).thenReturn(true);
    when(transaction.getFinPayment()).thenReturn(null);

    // Act
    String result = invokeGetMatchedDocument(transaction);

    // Assert
    assertEquals("T", result);
  }
  /**
   * Get matched document returns payment when payment not created by algorithm.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetMatchedDocumentReturnsPaymentWhenPaymentNotCreatedByAlgorithm()
      throws Exception {
    // Arrange
    FIN_FinaccTransaction transaction = mock(FIN_FinaccTransaction.class);
    FIN_Payment payment = mock(FIN_Payment.class);
    when(transaction.isCreatedByAlgorithm()).thenReturn(true);
    when(transaction.getFinPayment()).thenReturn(payment);
    when(payment.isCreatedByAlgorithm()).thenReturn(false);

    // Act
    String result = invokeGetMatchedDocument(transaction);

    // Assert
    assertEquals("P", result);
  }

  private String invokeGetMatchedDocument(FIN_FinaccTransaction transaction) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = APRM_MatchingUtility.class.getDeclaredMethod("getMatchedDocument",
        FIN_FinaccTransaction.class);
    method.setAccessible(true);
    return (String) method.invoke(null, transaction);
  }
}
