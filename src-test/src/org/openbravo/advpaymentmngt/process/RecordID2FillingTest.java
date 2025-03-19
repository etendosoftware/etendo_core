package org.openbravo.advpaymentmngt.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.query.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;

/**
 * Test class for RecordID2Filling.
 */
@RunWith(MockitoJUnitRunner.class)
public class RecordID2FillingTest {

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;

  @Mock
  private ProcessBundle mockProcessBundle;

  @Mock
  private ProcessLogger mockLogger;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Session mockSession;

  @Mock
  private OBCriteria<AcctSchema> mockAcctSchemaCriteria;

  @Mock
  private OBCriteria<FinAccPaymentMethod> mockFinAccPMCriteria;

  @Mock
  private Query mockObjectArrayQuery;

  @Mock
  private Query mockAcctCombQuery;

  @Mock
  private Query mockAccountingFactQuery;

  @Mock
  private Query mockGenericQuery;

  @Mock
  private ScrollableResults mockScrollableResults;

  @Mock
  private OBQuery<AccountingFact> mockOBAccountingFactQuery;

  @Mock
  private OBQuery<FIN_PaymentScheduleDetail> mockOBPaymentScheduleDetailQuery;

  @Mock
  private AccountingFact mockAccountingFact;

  @Mock
  private AcctSchema mockAcctSchema;

  @Mock
  private AccountingCombination mockAcctComb;

  @Mock
  private ElementValue mockElementValue;

  @Mock
  private FIN_Payment mockPayment;

  @Mock
  private FIN_PaymentMethod mockPaymentMethod;

  @Mock
  private FinAccPaymentMethod mockFinAccPM;

  @Mock
  private FIN_PaymentScheduleDetail mockPSD;

  @Spy
  private RecordID2Filling recordID2Filling = new RecordID2Filling();

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    when(mockProcessBundle.getLogger()).thenReturn(mockLogger);
    when(mockOBDal.getSession()).thenReturn(mockSession);

    when(mockOBDal.createCriteria(AcctSchema.class)).thenReturn(mockAcctSchemaCriteria);
    List<AcctSchema> acctSchemaList = new ArrayList<>();
    when(mockAcctSchema.getId()).thenReturn(TestConstants.TEST_ACCT_SCHEMA_ID);
    acctSchemaList.add(mockAcctSchema);
    when(mockAcctSchemaCriteria.list()).thenReturn(acctSchemaList);

    when(mockSession.createQuery(anyString(), eq(Object[].class))).thenReturn(mockObjectArrayQuery);
    when(mockSession.createQuery(anyString(), eq(AccountingCombination.class))).thenReturn(mockAcctCombQuery);
    when(mockSession.createQuery(anyString(), eq(AccountingFact.class))).thenReturn(mockAccountingFactQuery);
    when(mockSession.createQuery(anyString())).thenReturn(mockGenericQuery);

    when(mockObjectArrayQuery.setParameter(anyString(), any())).thenReturn(mockObjectArrayQuery);
    when(mockAcctCombQuery.setParameter(anyString(), any())).thenReturn(mockAcctCombQuery);

    when(mockGenericQuery.setParameterList(anyString(), (Collection) any())).thenReturn(mockGenericQuery);
    when(mockAccountingFactQuery.setParameterList(anyString(), any(Collection.class))).thenReturn(
        mockAccountingFactQuery);

    when(mockAccountingFactQuery.setFetchSize(anyInt())).thenReturn(mockAccountingFactQuery);
    when(mockAccountingFactQuery.scroll(any(ScrollMode.class))).thenReturn(mockScrollableResults);

    when(mockOBDal.createQuery(eq(AccountingFact.class), anyString())).thenReturn(mockOBAccountingFactQuery);
    when(mockOBDal.createQuery(eq(FIN_PaymentScheduleDetail.class), anyString())).thenReturn(
        mockOBPaymentScheduleDetailQuery);
    when(mockOBAccountingFactQuery.setNamedParameter(anyString(), any())).thenReturn(mockOBAccountingFactQuery);
    when(mockOBAccountingFactQuery.setFilterOnReadableClients(anyBoolean())).thenReturn(mockOBAccountingFactQuery);
    when(mockOBAccountingFactQuery.setFilterOnReadableOrganization(anyBoolean())).thenReturn(mockOBAccountingFactQuery);
    when(mockOBPaymentScheduleDetailQuery.setNamedParameter(anyString(), any())).thenReturn(
        mockOBPaymentScheduleDetailQuery);

    when(mockOBDal.createCriteria(FinAccPaymentMethod.class)).thenReturn(mockFinAccPMCriteria);
    when(mockFinAccPMCriteria.add(any(Criterion.class))).thenReturn(mockFinAccPMCriteria);
    when(mockFinAccPMCriteria.setMaxResults(anyInt())).thenReturn(mockFinAccPMCriteria);
    when(mockFinAccPMCriteria.uniqueResult()).thenReturn(mockFinAccPM);

    when(mockGenericQuery.executeUpdate()).thenReturn(10);

    when(mockAcctComb.getAccount()).thenReturn(mockElementValue);
    when(mockElementValue.getId()).thenReturn(TestConstants.ACCOUNT_ID);

    when(mockAccountingFact.getId()).thenReturn("testAccountingFactId");
    when(mockAccountingFact.getRecordID()).thenReturn("testRecordId");

    when(mockFinAccPM.getUponReceiptUse()).thenReturn("INT");

    Field loggerField = RecordID2Filling.class.getDeclaredField("logger");
    loggerField.setAccessible(true);
    loggerField.set(recordID2Filling, mockLogger);
  }

  /**
   * Cleans up the test environment after each test.
   */
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
   * Tests the getSchemas method.
   */
  @Test
  public void testGetSchemas() {
    Set<AcctSchema> result = recordID2Filling.getSchemas();

    assertNotNull(TestConstants.RESULT_NOT_NULL_MESSAGE, result);
    assertEquals("It should contain the expected schemas", 1, result.size());
  }

  /**
   * Tests the getBPAccountList method.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetBPAccountList() throws Exception {
    List<Object[]> accountList = new ArrayList<>();
    Object[] accountRow = new Object[2];
    accountRow[0] = mockAcctComb;
    accountRow[1] = mockAcctComb;
    accountList.add(accountRow);
    when(mockObjectArrayQuery.list()).thenReturn(accountList);

    Method getBPAccountListMethod = RecordID2Filling.class.getDeclaredMethod("getBPAccountList", boolean.class,
        String.class);
    getBPAccountListMethod.setAccessible(true);
    Set<String> result = (Set<String>) getBPAccountListMethod.invoke(recordID2Filling, true,
        TestConstants.TEST_ACCT_SCHEMA_ID);

    assertNotNull(TestConstants.RESULT_NOT_NULL_MESSAGE, result);
    assertEquals("It should contain the expected accounts", 1, result.size());
    assertTrue("It should contain the test account ID", result.contains(TestConstants.ACCOUNT_ID));
  }

  /**
   * Tests the getFAAccountList method.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetFAAccountList() throws Exception {
    List<AccountingCombination> accountList = new ArrayList<>();
    accountList.add(mockAcctComb);
    when(mockAcctCombQuery.list()).thenReturn(accountList);

    Method getFAAccountListMethod = RecordID2Filling.class.getDeclaredMethod("getFAAccountList", boolean.class,
        String.class);
    getFAAccountListMethod.setAccessible(true);
    Set<String> result = (Set<String>) getFAAccountListMethod.invoke(recordID2Filling, true,
        TestConstants.TEST_ACCT_SCHEMA_ID);

    assertNotNull(TestConstants.RESULT_NOT_NULL_MESSAGE, result);
    assertEquals("It should contain the expected accounts", 1, result.size());
    assertTrue("It should contain the test account ID", result.contains(TestConstants.ACCOUNT_ID));
  }

  /**
   * Tests the getUse method.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetUse() throws Exception {
    Method getUseMethod = RecordID2Filling.class.getDeclaredMethod("getUse", FIN_PaymentMethod.class, String.class,
        boolean.class, String.class);
    getUseMethod.setAccessible(true);
    String result = (String) getUseMethod.invoke(recordID2Filling, mockPaymentMethod, "testFinAccountId", true, "PAY");

    assertEquals("It should return the expected value", "INT", result);
  }

  /**
   * Tests the getAccountingEntryPosition method.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetAccountingEntryPosition() throws Exception {
    List<AccountingFact> accountingFactList = new ArrayList<>();
    accountingFactList.add(mockAccountingFact);
    when(mockOBAccountingFactQuery.list()).thenReturn(accountingFactList);

    Set<String> accounts = new HashSet<>();
    accounts.add(TestConstants.ACCOUNT_ID);

    Method getAccountingEntryPositionMethod = RecordID2Filling.class.getDeclaredMethod("getAccountingEntryPosition",
        AccountingFact.class, Set.class);
    getAccountingEntryPositionMethod.setAccessible(true);
    int result = (int) getAccountingEntryPositionMethod.invoke(recordID2Filling, mockAccountingFact, accounts);

    assertEquals("It should return the expected position", 0, result);
  }

  /**
   * Tests the getOrderedPSDs method.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetOrderedPSDs() throws Exception {
    List<FIN_PaymentScheduleDetail> psdList = new ArrayList<>();
    psdList.add(mockPSD);
    when(mockOBPaymentScheduleDetailQuery.list()).thenReturn(psdList);

    Method getOrderedPSDsMethod = RecordID2Filling.class.getDeclaredMethod("getOrderedPSDs", FIN_Payment.class);
    getOrderedPSDsMethod.setAccessible(true);
    List<FIN_PaymentScheduleDetail> result = (List<FIN_PaymentScheduleDetail>) getOrderedPSDsMethod.invoke(
        recordID2Filling, mockPayment);

    assertNotNull(TestConstants.RESULT_NOT_NULL_MESSAGE, result);
    assertEquals("It should contain the expected PSDs", 1, result.size());
  }

  /**
   * Tests the doExecute method and verifies that queries are executed.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecute_VerifyQueriesExecuted() throws Exception {
    Set<AcctSchema> schemas = new HashSet<>();
    schemas.add(mockAcctSchema);

    doReturn(schemas).when(recordID2Filling).getSchemas();

    doReturn(new HashSet<String>()).when(recordID2Filling).getBPAccountList(anyBoolean(), anyString());
    doReturn(new HashSet<String>()).when(recordID2Filling).getFAAccountList(anyBoolean(), anyString());

    recordID2Filling.doExecute(mockProcessBundle);

    verify(mockGenericQuery, atLeast(3)).executeUpdate();
    verify(mockLogger, atLeast(3)).logln(anyString());
    verify(mockOBDal, atLeast(3)).flush();
  }
}
