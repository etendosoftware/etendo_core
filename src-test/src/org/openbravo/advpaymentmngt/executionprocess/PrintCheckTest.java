package org.openbravo.advpaymentmngt.executionprocess;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.financialmgmt.payment.PaymentRun;
import org.openbravo.model.financialmgmt.payment.PaymentRunParameter;

/**
 * Test class for the PrintCheck class.
 */
@RunWith(MockitoJUnitRunner.class)
public class PrintCheckTest {

  private PrintCheck printCheck;

  @Mock
  private PaymentRun paymentRun;

  @Mock
  private PaymentRunParameter parameter;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    printCheck = new PrintCheck();
    List<PaymentRunParameter> parameters = new ArrayList<>();
    parameters.add(parameter);
    when(paymentRun.getFinancialMgmtPaymentRunParameterList()).thenReturn(parameters);
  }

  /**
   * Tests the execute method with an invalid check number.
   *
   * @throws ServletException
   *     if a servlet error occurs
   */
  @Test
  public void testExecuteInvalidCheckNumber() throws ServletException {
    // Arrange
    String invalidCheckNumber = "ABC";
    when(parameter.getValueOfTheTextParameter()).thenReturn(invalidCheckNumber);

    // Act
    OBError result = printCheck.execute(paymentRun);

    // Assert
    assertEquals("Error", result.getType());
    assertEquals("@APRM_NotValidNumber@", result.getMessage());
    verify(paymentRun, never()).setStatus(anyString());
  }
}
