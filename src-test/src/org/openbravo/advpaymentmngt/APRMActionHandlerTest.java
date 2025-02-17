package org.openbravo.advpaymentmngt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;

@RunWith(MockitoJUnitRunner.class)
public class APRMActionHandlerTest {

  private static final String FINANCIAL_ACCOUNT_ID = "test-fin-account-id";
  private MockedStatic<OBDal> mockedOBDal;

  @Mock
  private OBDal mockDal;

  @Mock
  private FIN_FinancialAccount mockFinAccount;

  @Mock
  private FinAccPaymentMethod mockPaymentMethod1;

  @Mock
  private FinAccPaymentMethod mockPaymentMethod2;

  @Captor
  private ArgumentCaptor<String> clearingUseCaptor;

  private APRMActionHandler handler;
  private Map<String, Object> parameters;

  @Before
  public void setUp() {
    // Initialize mocked static OBDal
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockDal);

    // Set up financial account mock
    when(mockDal.get(FIN_FinancialAccount.class, FINANCIAL_ACCOUNT_ID))
        .thenReturn(mockFinAccount);

    // Create list of payment methods
    List<FinAccPaymentMethod> paymentMethods = new ArrayList<>();
    paymentMethods.add(mockPaymentMethod1);
    paymentMethods.add(mockPaymentMethod2);

    // Set up financial account to return payment methods
    when(mockFinAccount.getFinancialMgmtFinAccPaymentMethodList())
        .thenReturn(paymentMethods);

    handler = new APRMActionHandler();
    parameters = new HashMap<>();
  }

  @After
  public void tearDown() {
    mockedOBDal.close();
  }

  @Test
  public void testUpdatePaymentMethodConfiguration() {
    // Execute method under test
    handler.updatePaymentMethodConfiguration(FINANCIAL_ACCOUNT_ID);

    // Verify payment methods were updated correctly
    verify(mockPaymentMethod1).setOUTUponClearingUse(clearingUseCaptor.capture());
    assertEquals("CLE", clearingUseCaptor.getValue());

    verify(mockPaymentMethod1).setINUponClearingUse(clearingUseCaptor.capture());
    assertEquals("CLE", clearingUseCaptor.getValue());

    verify(mockPaymentMethod2).setOUTUponClearingUse("CLE");
    verify(mockPaymentMethod2).setINUponClearingUse("CLE");

    // Verify save order
    InOrder inOrder = inOrder(mockDal, mockPaymentMethod1, mockPaymentMethod2);
    inOrder.verify(mockDal).save(mockPaymentMethod1);
    inOrder.verify(mockDal).save(mockPaymentMethod2);
    inOrder.verify(mockDal).flush();
  }



  @Test
  public void testUpdatePaymentMethodConfiguration_EmptyPaymentMethods() {
    when(mockFinAccount.getFinancialMgmtFinAccPaymentMethodList())
        .thenReturn(new ArrayList<>());

    handler.updatePaymentMethodConfiguration(FINANCIAL_ACCOUNT_ID);

    verify(mockDal, never()).save(any(FinAccPaymentMethod.class));
    verify(mockDal).flush();
  }

  @Test
  public void testExecute_BankTransitoryCallout() throws Exception {
    String content = new JSONObject()
        .put("eventType", "bankTransitoryCalloutResponse")
        .put("financialAccountId", FINANCIAL_ACCOUNT_ID)
        .toString();

    JSONObject result = handler.execute(parameters, content);

    assertNotNull(result);
    verify(mockPaymentMethod1).setOUTUponClearingUse("CLE");
    verify(mockPaymentMethod1).setINUponClearingUse("CLE");
    verify(mockDal).flush();
  }

  @Test
  public void testExecute_UnsupportedEventType() throws Exception {
    String content = new JSONObject()
        .put("eventType", "unsupportedEvent")
        .put("financialAccountId", FINANCIAL_ACCOUNT_ID)
        .toString();

    JSONObject result = handler.execute(parameters, content);

    assertNotNull(result);
    verify(mockDal, never()).save(any());
    verify(mockDal, never()).flush();
  }

  @Test
  public void testExecute_InvalidJson() throws Exception {
    String invalidJson = "{ invalid json content }";

    JSONObject result = handler.execute(parameters, invalidJson);

    assertNotNull(result);
    verify(mockDal, never()).save(any());
    verify(mockDal, never()).flush();
  }

  @Test
  public void testExecute_MissingFinancialAccountId() throws Exception {
    String content = new JSONObject()
        .put("eventType", "bankTransitoryCalloutResponse")
        .toString();

    JSONObject result = handler.execute(parameters, content);

    assertNotNull(result);
    verify(mockDal, never()).save(any());
    verify(mockDal, never()).flush();
  }

}
