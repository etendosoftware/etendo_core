package org.openbravo.advpaymentmngt.executionprocess;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.PaymentRun;
import org.openbravo.model.financialmgmt.payment.PaymentRunPayment;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test class for LeaveAsCredit
 */
@RunWith(MockitoJUnitRunner.class)
public class LeaveAsCreditTest extends OBBaseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // Static mocks
    private MockedStatic<OBDal> mockedOBDal;
    private MockedStatic<OBContext> mockedOBContext;

    // Mocks
    @Mock
    private OBDal obDal;

    @Mock
    private PaymentRun mockPaymentRun;

    @Mock
    private PaymentRunPayment mockPaymentRunPayment;

    @Mock
    private FIN_Payment mockPayment;

    @InjectMocks
    private LeaveAsCredit classUnderTest;

    @Before
    public void setUp() throws Exception {
        // Initialize static mocks
        mockedOBDal = mockStatic(OBDal.class);
        mockedOBContext = mockStatic(OBContext.class);

        // Configure static mocks
        mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
        mockedOBContext.when(() -> OBContext.setAdminMode(false)).thenAnswer(invocation -> null);
        mockedOBContext.when(OBContext::restorePreviousMode).thenAnswer(invocation -> null);

        // Configure OBDal behaviors
        doNothing().when(obDal).save(any());
        doNothing().when(obDal).flush();

        // Configure mock payment run
        List<PaymentRunPayment> paymentRunPayments = new ArrayList<>();
        paymentRunPayments.add(mockPaymentRunPayment);
        when(mockPaymentRun.getFinancialMgmtPaymentRunPaymentList()).thenReturn(paymentRunPayments);

        // Configure mock payment run payment
        when(mockPaymentRunPayment.getPayment()).thenReturn(mockPayment);

    }

    @After
    public void tearDown() {
        if (mockedOBDal != null) {
            mockedOBDal.close();
        }
        if (mockedOBContext != null) {
            mockedOBContext.close();
        }
    }

    /**
     * Test the execute method with a payment that has a zero amount
     */
    @Test
    public void testExecuteZeroPaymentAmount() throws Exception {
        // GIVEN
        BigDecimal zeroAmount = BigDecimal.ZERO;

        when(mockPayment.getAmount()).thenReturn(zeroAmount);
        when(mockPayment.isReceipt()).thenReturn(false);

        // WHEN
        OBError result = classUnderTest.execute(mockPaymentRun);

        // THEN
        verify(mockPayment, times(0)).setGeneratedCredit(any());
        verify(mockPayment, times(0)).setProcessed(false);

        verify(mockPayment).setStatus("PPM");
        verify(mockPaymentRunPayment).setResult("S");

        assertEquals("Success", result.getType());
        assertEquals("@Success@", result.getMessage());
    }

    /**
     * Test the execute method with a payment that has a positive amount
     */
    @Test
    public void testExecutePositivePaymentAmount() throws Exception {
        // GIVEN
        BigDecimal positiveAmount = new BigDecimal("100.00");

        when(mockPayment.getAmount()).thenReturn(positiveAmount);
        when(mockPayment.isReceipt()).thenReturn(true);

        // WHEN
        OBError result = classUnderTest.execute(mockPaymentRun);

        // THEN
        verify(mockPayment, times(0)).setGeneratedCredit(any());
        verify(mockPayment, times(0)).setProcessed(false);

        verify(mockPayment).setStatus("RPR");
        verify(mockPaymentRunPayment).setResult("S");

        assertEquals("Success", result.getType());
        assertEquals("@Success@", result.getMessage());
    }

}
