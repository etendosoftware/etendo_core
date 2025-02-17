package org.openbravo.advpaymentmngt.executionprocess;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.financialmgmt.payment.PaymentRun;
import org.openbravo.model.financialmgmt.payment.PaymentRunParameter;

@RunWith(MockitoJUnitRunner.class)
public class PrintCheckTest {

  private PrintCheck printCheck;

  @Mock
  private PaymentRun paymentRun;

  @Mock
  private PaymentRunParameter parameter;

  @Mock
  private OBDal obDal;

  @Before
  public void setUp() {
    printCheck = new PrintCheck();
    List<PaymentRunParameter> parameters = new ArrayList<>();
    parameters.add(parameter);
    when(paymentRun.getFinancialMgmtPaymentRunParameterList()).thenReturn(parameters);

  }


  @Test
  public void testExecute_InvalidCheckNumber() throws ServletException {
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