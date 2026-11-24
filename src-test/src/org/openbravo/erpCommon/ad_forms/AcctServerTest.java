package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;

/**
 * Unit tests for AcctServer.
 * Focuses on pure logic methods that can be tested without database/static dependencies.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class AcctServerTest {

  private static final String GET_STR_ACCOUNT = "getStrAccount";
  private static final String AMOUNTS_FIELD = "Amounts";

  private AcctServer instance;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    // We need a concrete subclass to test AcctServer since it is abstract.
    // Use ObjenesisStd to create an instance of a concrete subclass.
    instance = objenesis.newInstance(DocGLJournal.class);

    // Initialize fields that getAmount and isBalanced rely on
    Field amountsField = AcctServer.class.getDeclaredField(AMOUNTS_FIELD);
    amountsField.setAccessible(true);
    amountsField.set(instance, new String[]{"100", "200", "50", "75"});

    instance.ZERO = BigDecimal.ZERO;
  }
  /** Constants. */

  @Test
  public void testConstants() {
    assertEquals("N", AcctServer.STATUS_NotPosted);
    assertEquals("Y", AcctServer.STATUS_Posted);
    assertEquals("E", AcctServer.STATUS_Error);
    assertEquals("b", AcctServer.STATUS_NotBalanced);
    assertEquals("c", AcctServer.STATUS_NotConvertible);
    assertEquals("p", AcctServer.STATUS_PeriodClosed);
    assertEquals("i", AcctServer.STATUS_InvalidAccount);
    assertEquals("y", AcctServer.STATUS_PostPrepared);
    assertEquals("C", AcctServer.STATUS_InvalidCost);
    assertEquals("L", AcctServer.STATUS_DocumentLocked);
    assertEquals("D", AcctServer.STATUS_DocumentDisabled);
    assertEquals("T", AcctServer.STATUS_TableDisabled);
    assertEquals("-1", AcctServer.NO_CURRENCY);
  }
  /** Doc type constants. */

  @Test
  public void testDocTypeConstants() {
    assertEquals("ARI", AcctServer.DOCTYPE_ARInvoice);
    assertEquals("ARC", AcctServer.DOCTYPE_ARCredit);
    assertEquals("API", AcctServer.DOCTYPE_APInvoice);
    assertEquals("APC", AcctServer.DOCTYPE_APCredit);
    assertEquals("GLJ", AcctServer.DOCTYPE_GLJournal);
    assertEquals("MMS", AcctServer.DOCTYPE_MatShipment);
    assertEquals("MMR", AcctServer.DOCTYPE_MatReceipt);
  }
  /** Table id constants. */

  @Test
  public void testTableIdConstants() {
    assertEquals("318", AcctServer.TABLEID_Invoice);
    assertEquals("D1A97202E832470285C9B1EB026D54E2", AcctServer.TABLEID_Payment);
    assertEquals("4D8C3B3C31D1410DA046140C9F024D17", AcctServer.TABLEID_Transaction);
    assertEquals("224", AcctServer.TABLEID_GLJournal);
  }
  /** Get amount with valid index. */

  @Test
  public void testGetAmountWithValidIndex() {
    assertEquals("100", instance.getAmount(0));
    assertEquals("200", instance.getAmount(1));
    assertEquals("50", instance.getAmount(2));
    assertEquals("75", instance.getAmount(3));
  }
  /** Get amount with negative index. */

  @Test
  public void testGetAmountWithNegativeIndex() {
    assertNull(instance.getAmount(-1));
  }
  /** Get amount with index out of bounds. */

  @Test
  public void testGetAmountWithIndexOutOfBounds() {
    assertNull(instance.getAmount(4));
    assertNull(instance.getAmount(100));
  }
  /**
   * Get amount with empty string.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetAmountWithEmptyString() throws Exception {
    Field amountsField = AcctServer.class.getDeclaredField(AMOUNTS_FIELD);
    amountsField.setAccessible(true);
    amountsField.set(instance, new String[]{"", "200", "", "75"});

    assertEquals("0", instance.getAmount(0));
    assertEquals("200", instance.getAmount(1));
    assertEquals("0", instance.getAmount(2));
  }
  /** Get amount no args. */

  @Test
  public void testGetAmountNoArgs() {
    assertEquals("100", instance.getAmount());
  }
  /** Get and set status. */

  @Test
  public void testGetAndSetStatus() {
    instance.setStatus("Y");
    assertEquals("Y", instance.getStatus());

    instance.setStatus("N");
    assertEquals("N", instance.getStatus());
  }
  /**
   * Is balanced when multi currency.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsBalancedWhenMultiCurrency() throws Exception {
    instance.MultiCurrency = true;
    assertTrue(instance.isBalanced());
  }
  /** Set and get background. */

  @Test
  public void testSetAndGetBackground() {
    assertFalse(instance.isBackground());
    instance.setBackground(true);
    assertTrue(instance.isBackground());
    instance.setBackground(false);
    assertFalse(instance.isBackground());
  }
  /** Set batch size. */

  @Test
  public void testSetBatchSize() {
    instance.setBatchSize("500");
    assertEquals("500", instance.batchSize);
  }
  /** Get servlet info. */

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }
  /** Get invalid account parameters. */

  @Test
  public void testGetInvalidAccountParameters() {
    Map<String, String> params = instance.getInvalidAccountParameters("@TestAccount@", "TestEntity", "TestSchema");

    assertNotNull(params);
    assertEquals(3, params.size());
    assertEquals("@TestAccount@", params.get(AcctServer.ACCOUNT_PARAM));
    assertEquals("TestEntity", params.get(AcctServer.ENTITY));
    assertEquals("TestSchema", params.get(AcctServer.ACCOUNTING_SCHEMA));
  }
  /** Get invalid cost parameters. */

  @Test
  public void testGetInvalidCostParameters() {
    Map<String, String> params = instance.getInvalidCostParameters("ProductA", "2024-01-01");

    assertNotNull(params);
    assertEquals(2, params.size());
    assertEquals("ProductA", params.get("Product"));
    assertEquals("2024-01-01", params.get("Date"));
  }
  /** Set and get message result. */

  @Test
  public void testSetAndGetMessageResult() {
    assertNull(instance.getMessageResult());

    org.openbravo.erpCommon.utility.OBError error = new org.openbravo.erpCommon.utility.OBError();
    error.setType("Error");
    error.setMessage("Test error");
    instance.setMessageResult(error);

    assertNotNull(instance.getMessageResult());
    assertEquals("Error", instance.getMessageResult().getType());
    assertEquals("Test error", instance.getMessageResult().getMessage());
  }
  /**
   * Get str account receipt int.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetStrAccountReceiptINT() throws Exception {
    Method method = AcctServer.class.getDeclaredMethod(GET_STR_ACCOUNT, String.class, boolean.class);
    method.setAccessible(true);

    String result = (String) method.invoke(null, "INT", true);
    assertEquals("@InTransitPaymentAccountIN@", result);
  }
  /**
   * Get str account receipt dep.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetStrAccountReceiptDEP() throws Exception {
    Method method = AcctServer.class.getDeclaredMethod(GET_STR_ACCOUNT, String.class, boolean.class);
    method.setAccessible(true);

    String result = (String) method.invoke(null, "DEP", true);
    assertEquals("@DepositAccount@", result);
  }
  /**
   * Get str account receipt other.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetStrAccountReceiptOther() throws Exception {
    Method method = AcctServer.class.getDeclaredMethod(GET_STR_ACCOUNT, String.class, boolean.class);
    method.setAccessible(true);

    String result = (String) method.invoke(null, "CLE", true);
    assertEquals("@ClearedPaymentAccount@", result);
  }
  /**
   * Get str account non receipt int.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetStrAccountNonReceiptINT() throws Exception {
    Method method = AcctServer.class.getDeclaredMethod(GET_STR_ACCOUNT, String.class, boolean.class);
    method.setAccessible(true);

    String result = (String) method.invoke(null, "INT", false);
    assertEquals("@InTransitPaymentAccountOUT@", result);
  }
  /**
   * Get str account non receipt cle.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetStrAccountNonReceiptCLE() throws Exception {
    Method method = AcctServer.class.getDeclaredMethod(GET_STR_ACCOUNT, String.class, boolean.class);
    method.setAccessible(true);

    String result = (String) method.invoke(null, "CLE", false);
    assertEquals("@ClearedPaymentAccountOUT@", result);
  }
  /**
   * Get str account non receipt wit.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetStrAccountNonReceiptWIT() throws Exception {
    Method method = AcctServer.class.getDeclaredMethod(GET_STR_ACCOUNT, String.class, boolean.class);
    method.setAccessible(true);

    String result = (String) method.invoke(null, "WIT", false);
    assertEquals("@WithdrawalAccount@", result);
  }
  /**
   * Get connection provider.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetConnectionProvider() throws Exception {
    org.openbravo.database.ConnectionProvider mockConn = org.mockito.Mockito.mock(org.openbravo.database.ConnectionProvider.class);
    Field connField = AcctServer.class.getDeclaredField("connectionProvider");
    connField.setAccessible(true);
    connField.set(instance, mockConn);

    assertEquals(mockConn, instance.getConnectionProvider());
  }
  /**
   * Get conversion date.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetConversionDate() throws Exception {
    instance.DateAcct = "2024-01-15";
    Method method = AcctServer.class.getDeclaredMethod("getConversionDate");
    method.setAccessible(true);

    String result = (String) method.invoke(instance);
    assertEquals("2024-01-15", result);
  }
  /** Object field provider. */

  @Test
  public void testObjectFieldProvider() {
    assertNull(instance.getObjectFieldProvider());

    org.openbravo.data.FieldProvider[] fps = new org.openbravo.data.FieldProvider[0];
    instance.setObjectFieldProvider(fps);
    assertNotNull(instance.getObjectFieldProvider());
    assertEquals(0, instance.getObjectFieldProvider().length);
  }
  /** Amt type constants. */

  @Test
  public void testAmtTypeConstants() {
    assertEquals(0, AcctServer.AMTTYPE_Gross);
    assertEquals(1, AcctServer.AMTTYPE_Net);
    assertEquals(2, AcctServer.AMTTYPE_Charge);
    assertEquals(0, AcctServer.AMTTYPE_Invoice);
    assertEquals(1, AcctServer.AMTTYPE_Allocation);
    assertEquals(2, AcctServer.AMTTYPE_Discount);
    assertEquals(3, AcctServer.AMTTYPE_WriteOff);
  }
  /** Insert note deprecated. */

  @Test
  public void testInsertNoteDeprecated() {
    boolean result = instance.insertNote("client", "org", "user", "table",
        "record", "msg", "text", "ref", null, null, null);
    assertFalse(result);
  }
  /** Get not calculated cost parameters. */

  @Test
  public void testGetNotCalculatedCostParameters() {
    org.openbravo.model.materialmgmt.transaction.MaterialTransaction mockTrx =
        org.mockito.Mockito.mock(
            org.openbravo.model.materialmgmt.transaction.MaterialTransaction.class);
    org.openbravo.model.common.plm.Product mockProduct =
        org.mockito.Mockito.mock(org.openbravo.model.common.plm.Product.class);

    org.mockito.Mockito.when(mockTrx.getIdentifier()).thenReturn("TrxIdentifier");
    org.mockito.Mockito.when(mockTrx.getProduct()).thenReturn(mockProduct);
    org.mockito.Mockito.when(mockProduct.getIdentifier()).thenReturn("ProductIdentifier");

    Map<String, String> params = instance.getNotCalculatedCostParameters(mockTrx);

    assertNotNull(params);
    assertEquals(2, params.size());
    assertEquals("TrxIdentifier", params.get("trx"));
    assertEquals("ProductIdentifier", params.get("product"));
  }
  /**
   * Is balanced when balance is zero.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsBalancedWhenBalanceIsZero() throws Exception {
    AcctServer spy = org.mockito.Mockito.spy(instance);
    org.mockito.Mockito.doReturn(BigDecimal.ZERO).when(spy).getBalance();
    spy.MultiCurrency = false;
    spy.ZERO = BigDecimal.ZERO;

    assertTrue(spy.isBalanced());
  }
  /**
   * Is balanced when balance is not zero.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsBalancedWhenBalanceIsNotZero() throws Exception {
    AcctServer spy = org.mockito.Mockito.spy(instance);
    org.mockito.Mockito.doReturn(new BigDecimal("100")).when(spy).getBalance();
    spy.MultiCurrency = false;
    spy.ZERO = BigDecimal.ZERO;

    assertFalse(spy.isBalanced());
  }
  /**
   * Is convertible when no currency.
   * @throws Exception if an error occurs
   */

  @Test
  public void testIsConvertibleWhenNoCurrency() throws Exception {
    instance.C_Currency_ID = "-1";

    AcctSchema mockSchema = org.mockito.Mockito.mock(AcctSchema.class);
    org.openbravo.database.ConnectionProvider mockConn =
        org.mockito.Mockito.mock(org.openbravo.database.ConnectionProvider.class);

    assertTrue(instance.isConvertible(mockSchema, mockConn));
  }
  /**
   * Get amount with null amounts.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetAmountWithNullAmounts() throws Exception {
    Field amountsField = AcctServer.class.getDeclaredField(AMOUNTS_FIELD);
    amountsField.setAccessible(true);
    amountsField.set(instance, null);

    assertNull(instance.getAmount(0));
  }
  /**
   * Get conversion date with different values.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetConversionDateWithDifferentValues() throws Exception {
    Method method = AcctServer.class.getDeclaredMethod("getConversionDate");
    method.setAccessible(true);

    instance.DateAcct = "2025-06-15";
    assertEquals("2025-06-15", method.invoke(instance));

    instance.DateAcct = "2020-12-31";
    assertEquals("2020-12-31", method.invoke(instance));

    instance.DateAcct = "";
    assertEquals("", method.invoke(instance));
  }

  /**
   * ETP-6365: the document's own rate (C_Conversion_Rate_Document) is found whatever the posting
   * context. The background posting runs with the System context, whose readable client filter
   * hid the tenant's rows: the count ran outside admin mode and with the readable filters on, so
   * it saw nothing and the document fell back to the general rate (or was not convertible). The
   * query mock answers a row only when it is counted in admin mode with both filters off.
   *
   * @throws Exception if the reflection or the mocks fail
   */
  @Test
  public void isConvertibleReadsTheDocumentRateOutsideTheTenantContext() throws Exception {
    AcctServer doc = new ObjenesisStd().newInstance(DocGLJournal.class);
    doc.AD_Table_ID = AcctServer.TABLEID_Invoice;
    doc.Record_ID = "INVOICE-ID";
    doc.C_Currency_ID = "MXN-ID";
    doc.p_lines = new DocLine[0];
    AcctSchema schema = new ObjenesisStd().newInstance(AcctSchema.class);
    schema.m_C_Currency_ID = "EUR-ID";

    AtomicBoolean adminMode = new AtomicBoolean(false);
    AtomicBoolean clientFilterOff = new AtomicBoolean(false);
    AtomicBoolean orgFilterOff = new AtomicBoolean(false);
    @SuppressWarnings("unchecked")
    OBQuery<ConversionRateDoc> query = mock(OBQuery.class);
    doAnswer(inv -> {
      clientFilterOff.set(!(Boolean) inv.getArgument(0));
      return null;
    }).when(query).setFilterOnReadableClients(anyBoolean());
    doAnswer(inv -> {
      orgFilterOff.set(!(Boolean) inv.getArgument(0));
      return null;
    }).when(query).setFilterOnReadableOrganization(anyBoolean());
    when(query.count()).thenAnswer(
        inv -> adminMode.get() && clientFilterOff.get() && orgFilterOff.get() ? 1 : 0);
    ConversionRateDoc rate = mock(ConversionRateDoc.class);
    when(rate.getRate()).thenReturn(new BigDecimal("0.05"));
    when(query.list()).thenReturn(List.of(rate));

    @SuppressWarnings("unchecked")
    OBCriteria<Currency> currencies = mock(OBCriteria.class);
    when(currencies.add(any())).thenReturn(currencies);
    when(currencies.setProjection(any())).thenReturn(currencies);
    when(currencies.count()).thenReturn(1);
    Currency euro = mock(Currency.class);
    when(euro.getStandardPrecision()).thenReturn(2L);
    when(currencies.list()).thenReturn(List.of(euro));

    OBDal dal = mock(OBDal.class);
    doReturn(query).when(dal).createQuery(eq(ConversionRateDoc.class), anyString());
    doReturn(currencies).when(dal).createCriteria(Currency.class);

    try (MockedStatic<OBDal> obDal = mockStatic(OBDal.class);
        MockedStatic<OBContext> obContext = mockStatic(OBContext.class);
        MockedStatic<AcctServer> generalRate = mockStatic(AcctServer.class)) {
      obDal.when(OBDal::getInstance).thenReturn(dal);
      obContext.when(() -> OBContext.setAdminMode(true)).thenAnswer(inv -> {
        adminMode.set(true);
        return null;
      });
      obContext.when(OBContext::restorePreviousMode).thenAnswer(inv -> {
        adminMode.set(false);
        return null;
      });
      // The general-rate fallback finds nothing, as with no range open at 00:00.
      generalRate.when(() -> AcctServer.getConvertedAmt(anyString(), anyString(), anyString(),
          any(), any(), any(), any(), any(ConnectionProvider.class))).thenReturn("");

      assertTrue(doc.isConvertible(schema, mock(ConnectionProvider.class)));
      generalRate.verify(() -> AcctServer.getConvertedAmt(anyString(), anyString(), anyString(),
          any(), any(), any(), any(), any(ConnectionProvider.class)), never());
    }
  }
}
