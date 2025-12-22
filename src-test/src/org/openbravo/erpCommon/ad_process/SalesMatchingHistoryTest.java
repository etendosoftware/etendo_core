package org.openbravo.erpCommon.ad_process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.sales.SIMatch;
import org.openbravo.model.sales.SOMatch;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;

/**
 * Unit tests for {@link SalesMatchingHistory}.
 * Uses MockitoExtension to enable Mockito mocks and static mocking for dependencies.
 */
@ExtendWith(MockitoExtension.class)
class SalesMatchingHistoryTest {
  private static final String SUCCESS = "Success";
  private static final String MATCHED_DAYS_ERROR = "MatchedSalesDaysBackError";
  private SalesMatchingHistory salesMatchingHistory;

  @Mock
  private Client mockClient;

  @Mock
  private Organization mockOrganization;

  @Mock
  private User mockUser;

  @Mock
  private Role mockRole;

  @Mock
  private Session mockSession;

  @Mock
  private OBContext mockOBContext;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBProvider mockOBProvider;

  @Mock
  private OBCriteria mockOBCriteria;

  @Mock
  private BaseOBObject mockBaseOBObject;

  @Mock
  private InvoiceLine mockInvoiceLine;

  @Mock
  private InvoiceLine mockInvoiceLine2;

  @Mock
  private Invoice mockInvoice;

  @Mock
  private OrderLine mockOrderLine;

  @Mock
  private Product mockProduct;

  @Mock
  private SOMatch mockSOMatch;

  @Mock
  private SIMatch mockSIMatch;

  @Mock
  private ShipmentInOut mockShipmentInOut;

  @Mock
  private ShipmentInOutLine mockShipmentInOutLine;

  @Mock
  private ProcessBundle mockProcessBundle;

  @Mock
  private ProcessLogger mockProcessLogger;

  /**
   * Initializes a new instance of {@link SalesMatchingHistory} before each test.
   */
  @BeforeEach
  void setup() {
    salesMatchingHistory = spy(new SalesMatchingHistory());
  }

  /**
   * Verifies that {@link SalesMatchingHistory#doExecute(ProcessBundle)} runs the
   * process successfully.
   * <p>
   * The test ensures that:
   * <ul>
   *   <li>Both {@code backfillSIMatch} and {@code backfillSOMatch} are invoked.</li>
   *   <li>The result {@link OBError} is set with type {@code "Success"}.</li>
   *   <li>No exception is thrown during normal execution.</li>
   * </ul>
   *
   * @throws Exception
   *     if the underlying process execution throws an unexpected error
   */
  @Test
  void testDoExecuteRunsBackfillAndSetsSuccess() throws Exception {
    when(mockProcessBundle.getLogger()).thenReturn(mockProcessLogger);

    int daysBack = 90;
    Date fromDate = new Date();

    try (MockedStatic<SalesMatchingHistory> historyStatic = mockStatic(SalesMatchingHistory.class,
        Mockito.CALLS_REAL_METHODS); MockedStatic<OBDal> obDalStatic = mockStatic(
        OBDal.class); MockedStatic<OBMessageUtils> msgStatic = mockStatic(
        OBMessageUtils.class); MockedStatic<OBContext> obContextStatic = mockStatic(OBContext.class)) {

      historyStatic.when(SalesMatchingHistory::getAmountOfDays).thenReturn(daysBack);
      historyStatic.when(() -> SalesMatchingHistory.calculateFromDate(daysBack)).thenReturn(fromDate);

      obContextStatic.when(() -> OBContext.setAdminMode(false)).thenAnswer(inv -> null);
      obContextStatic.when(OBContext::restorePreviousMode).thenAnswer(inv -> null);
      obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

      msgStatic.when(() -> OBMessageUtils.messageBD(anyString())).thenAnswer(inv -> inv.getArgument(0, String.class));

      doNothing().when(salesMatchingHistory).backfillSIMatch(fromDate);
      doNothing().when(salesMatchingHistory).backfillSOMatch(fromDate);

      salesMatchingHistory.doExecute(mockProcessBundle);

      verify(salesMatchingHistory).backfillSIMatch(fromDate);
      verify(salesMatchingHistory).backfillSOMatch(fromDate);
      verify(mockOBDal).flush();
      verify(mockProcessLogger).logln(SUCCESS);

      ArgumentCaptor<OBError> errorCaptor = ArgumentCaptor.forClass(OBError.class);
      verify(mockProcessBundle).setResult(errorCaptor.capture());
      OBError result = errorCaptor.getValue();
      assertEquals(SUCCESS, result.getType());
      assertEquals(SUCCESS, result.getTitle());
    }
  }

  /**
   * Verifies that {@link SalesMatchingHistory#backfillSIMatch(Date)} creates
   * {@link SIMatch} records for eligible sales invoice lines.
   * <p>
   * The test simulates invoice lines linked to goods shipment lines and checks that:
   * <ul>
   *   <li>The criteria used to retrieve invoice lines is applied correctly.</li>
   *   <li>A new {@link SIMatch} instance is created when no match exists yet.</li>
   *   <li>The match is persisted through {@link OBDal#save(Object)}.</li>
   * </ul>
   */
  @Test
  void testBackfillSIMatchCreatesMatchesForEligibleLines() {
    Date fromDate = new Date();
    SIMatch existingMatch = null;
    BigDecimal qty = new BigDecimal("4");

    when(mockInvoiceLine.getGoodsShipmentLine()).thenReturn(mockShipmentInOutLine);
    when(mockInvoiceLine2.getGoodsShipmentLine()).thenReturn(null);
    when(mockInvoiceLine.getInvoicedQuantity()).thenReturn(qty);

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {
      obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

      when(mockOBDal.createCriteria(InvoiceLine.class)).thenReturn(mockOBCriteria);
      when(mockOBDal.createCriteria(SIMatch.class)).thenReturn(mockOBCriteria);
      when(mockOBCriteria.createAlias(anyString(), anyString())).thenReturn(mockOBCriteria);
      when(mockOBCriteria.add(any())).thenReturn(mockOBCriteria);
      when(mockOBCriteria.list()).thenReturn(Arrays.asList(mockInvoiceLine, mockInvoiceLine2));
      when(mockOBCriteria.add(any())).thenReturn(mockOBCriteria);
      when(mockOBCriteria.uniqueResult()).thenReturn(existingMatch);

      doNothing().when(salesMatchingHistory).createShipmentSIMatch(any(InvoiceLine.class), any(ShipmentInOutLine.class),
          any(BigDecimal.class), any(Date.class));

      salesMatchingHistory.backfillSIMatch(fromDate);

      ArgumentCaptor<BigDecimal> qtyCaptor = ArgumentCaptor.forClass(BigDecimal.class);
      ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);

      verify(salesMatchingHistory).createShipmentSIMatch(eq(mockInvoiceLine), eq(mockShipmentInOutLine),
          qtyCaptor.capture(), dateCaptor.capture());

      verify(salesMatchingHistory, never()).createShipmentSIMatch(eq(mockInvoiceLine2), any(ShipmentInOutLine.class),
          any(BigDecimal.class), any(Date.class));

      assertEquals(qty, qtyCaptor.getValue());
      assertNotNull(dateCaptor.getValue());

      verify(mockOBDal).flush();
    }
  }

  /**
   * Verifies that {@link SalesMatchingHistory#createShipmentSIMatch(InvoiceLine, ShipmentInOutLine, BigDecimal, Date)}
   * correctly creates and persists a {@link SIMatch} for the Sales Invoice → Goods Shipment scenario.
   * <p>
   * The test checks that:
   * <ul>
   *   <li>The new match is initialized with the client, organization and audit fields.</li>
   *   <li>The invoice line and shipment line are properly linked.</li>
   *   <li>The quantity and transaction date are set from the invoice.</li>
   *   <li>The match is saved via {@link OBDal#save(Object)}.</li>
   * </ul>
   */
  @Test
  void testCreateShipmentSIMatchCreatesAndSavesMatch() {
    BigDecimal qty = new BigDecimal("5");
    Date now = new Date();
    Date acctDate = new Date();

    when(mockInvoiceLine.getClient()).thenReturn(mockClient);
    when(mockInvoiceLine.getOrganization()).thenReturn(mockOrganization);
    when(mockInvoiceLine.getInvoice()).thenReturn(mockInvoice);
    when(mockInvoiceLine.getProduct()).thenReturn(mockProduct);
    when(mockInvoice.getAccountingDate()).thenReturn(acctDate);

    try (MockedStatic<OBProvider> obProviderStatic = mockStatic(
        OBProvider.class); MockedStatic<OBContext> obContextStatic = mockStatic(
        OBContext.class); MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {

      obProviderStatic.when(OBProvider::getInstance).thenReturn(mockOBProvider);
      when(mockOBProvider.get(SIMatch.class)).thenReturn(mockSIMatch);

      obContextStatic.when(OBContext::getOBContext).thenReturn(mockOBContext);
      when(mockOBContext.getUser()).thenReturn(mockUser);

      obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

      salesMatchingHistory.createShipmentSIMatch(mockInvoiceLine, mockShipmentInOutLine, qty, now);

      verify(mockSIMatch).setClient(mockClient);
      verify(mockSIMatch).setOrganization(mockOrganization);
      verify(mockSIMatch).setActive(true);
      verify(mockSIMatch).setCreationDate(now);
      verify(mockSIMatch).setCreatedBy(mockUser);
      verify(mockSIMatch).setUpdated(now);
      verify(mockSIMatch).setUpdatedBy(mockUser);
      verify(mockSIMatch).setGoodsShipmentLine(mockShipmentInOutLine);
      verify(mockSIMatch).set(SIMatch.PROPERTY_INVOICELINE, mockInvoiceLine);
      verify(mockSIMatch).set(SIMatch.PROPERTY_TRANSACTIONDATE, acctDate);
      verify(mockSIMatch).set(SIMatch.PROPERTY_QUANTITY, qty);
      verify(mockSIMatch).set(SIMatch.PROPERTY_PROCESSNOW, false);
      verify(mockSIMatch).set(SIMatch.PROPERTY_PROCESSED, true);
      verify(mockSIMatch).set(SIMatch.PROPERTY_POSTED, "N");
      verify(mockSIMatch).set(SIMatch.PROPERTY_PRODUCT, mockProduct);
      verify(mockOBDal).save(mockSIMatch);
    }
  }

  /**
   * Verifies that {@link SalesMatchingHistory#backfillSOMatch(Date)} delegates
   * to the shipment- and invoice-based backfill methods and flushes changes.
   * <p>
   * The test ensures that:
   * <ul>
   *   <li>{@code backfillSOMatchFromShipments} is called with the expected date.</li>
   *   <li>{@code backfillSOMatchFromInvoices} is called with the same date.</li>
   *   <li>{@link OBDal#flush()} is invoked at the end of the method.</li>
   * </ul>
   */
  @Test
  void testBackfillSOMatchDelegatesAndFlushes() {
    Date fromDate = new Date();

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {
      obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

      doNothing().when(salesMatchingHistory).backfillSOMatchFromShipments(any(Date.class), any(Date.class));
      doNothing().when(salesMatchingHistory).backfillSOMatchFromInvoices(any(Date.class), any(Date.class));

      salesMatchingHistory.backfillSOMatch(fromDate);

      ArgumentCaptor<Date> shipmentsFromDateCaptor = ArgumentCaptor.forClass(Date.class);
      ArgumentCaptor<Date> shipmentsNowCaptor = ArgumentCaptor.forClass(Date.class);
      ArgumentCaptor<Date> invoicesFromDateCaptor = ArgumentCaptor.forClass(Date.class);
      ArgumentCaptor<Date> invoicesNowCaptor = ArgumentCaptor.forClass(Date.class);

      verify(salesMatchingHistory).backfillSOMatchFromShipments(shipmentsFromDateCaptor.capture(),
          shipmentsNowCaptor.capture());
      verify(salesMatchingHistory).backfillSOMatchFromInvoices(invoicesFromDateCaptor.capture(),
          invoicesNowCaptor.capture());

      assertEquals(fromDate, shipmentsFromDateCaptor.getValue());
      assertEquals(fromDate, invoicesFromDateCaptor.getValue());
      assertNotNull(shipmentsNowCaptor.getValue());
      assertSame(shipmentsNowCaptor.getValue(), invoicesNowCaptor.getValue());

      verify(mockOBDal).flush();
    }
  }

  /**
   * Verifies that {@link SalesMatchingHistory#backfillSOMatchFromShipments(Date, Date)}
   * creates {@link SOMatch} records for eligible shipment lines (SO → GS).
   * <p>
   * The test simulates shipment lines linked to sales order lines and checks that:
   * <ul>
   *   <li>The criteria to select shipments uses the correct alias, dates and statuses.</li>
   *   <li>A new {@link SOMatch} is created when no existing match is found.</li>
   *   <li>The match is saved through {@link OBDal#save(Object)}.</li>
   * </ul>
   */
  @Test
  void testBackfillSOMatchFromShipmentsCreatesMatchesForEligibleLines() {
    Date fromDate = new Date();
    Date now = new Date();

    ShipmentInOutLine shipLineWithOrder = mock(ShipmentInOutLine.class);
    ShipmentInOutLine shipLineWithoutOrder = mock(ShipmentInOutLine.class);

    when(shipLineWithOrder.getSalesOrderLine()).thenReturn(mockOrderLine);
    when(shipLineWithoutOrder.getSalesOrderLine()).thenReturn(null);

    BigDecimal movementQty = new BigDecimal("3");
    when(shipLineWithOrder.getMovementQuantity()).thenReturn(movementQty);

    doAnswer(invocation -> {
      int c = invocation.getArgument(0);
      return c + 1;
    }).when(salesMatchingHistory).incrementAndMaybeFlush(anyInt());

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {
      obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

      when(mockOBDal.createCriteria(ShipmentInOutLine.class)).thenReturn(mockOBCriteria);
      when(mockOBDal.createCriteria(SOMatch.class)).thenReturn(mockOBCriteria);
      when(mockOBCriteria.add(any())).thenReturn(mockOBCriteria);
      when(mockOBCriteria.createAlias(anyString(), anyString())).thenReturn(mockOBCriteria);
      when(mockOBCriteria.list()).thenReturn(Arrays.asList(shipLineWithOrder, shipLineWithoutOrder));
      when(mockOBCriteria.add(any())).thenReturn(mockOBCriteria);
      when(mockOBCriteria.uniqueResult()).thenReturn(null);

      doNothing().when(salesMatchingHistory).createShipmentSOMatch(any(ShipmentInOutLine.class), any(OrderLine.class),
          any(BigDecimal.class), any(Date.class));

      salesMatchingHistory.backfillSOMatchFromShipments(fromDate, now);

      ArgumentCaptor<BigDecimal> qtyCaptor = ArgumentCaptor.forClass(BigDecimal.class);
      ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);

      verify(salesMatchingHistory).createShipmentSOMatch(eq(shipLineWithOrder), eq(mockOrderLine), qtyCaptor.capture(),
          dateCaptor.capture());
      verify(salesMatchingHistory, never()).createShipmentSOMatch(eq(shipLineWithoutOrder), any(OrderLine.class),
          any(BigDecimal.class), any(Date.class));

      assertEquals(movementQty, qtyCaptor.getValue());
      assertEquals(now, dateCaptor.getValue());
    }
  }

  /**
   * Verifies that {@link SalesMatchingHistory#backfillSOMatchFromInvoices(Date, Date)}
   * creates {@link SOMatch} records for eligible invoice lines (SO → SI).
   * <p>
   * The test simulates sales invoice lines linked to order lines and checks that:
   * <ul>
   *   <li>The criteria to select invoices uses the correct alias, dates and statuses.</li>
   *   <li>A new {@link SOMatch} is created when no existing match is found.</li>
   *   <li>The match is saved through {@link OBDal#save(Object)}.</li>
   * </ul>
   */
  @Test
  void testBackfillSOMatchFromInvoicesCreatesMatchesForEligibleLines() {
    Date fromDate = new Date();
    Date now = new Date();

    when(mockInvoiceLine.getSalesOrderLine()).thenReturn(mockOrderLine);
    when(mockInvoiceLine2.getSalesOrderLine()).thenReturn(null);

    BigDecimal qty = new BigDecimal("5");
    when(mockInvoiceLine.getInvoicedQuantity()).thenReturn(qty);

    doAnswer(invocation -> {
      int c = invocation.getArgument(0);
      return c + 1;
    }).when(salesMatchingHistory).incrementAndMaybeFlush(anyInt());

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {
      OBCriteria<SOMatch> soMatchCrit = mock(String.valueOf(SOMatch.class));

      obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.createCriteria(InvoiceLine.class)).thenReturn(mockOBCriteria);
      when(mockOBDal.createCriteria(SOMatch.class)).thenReturn(soMatchCrit);

      when(mockOBCriteria.createAlias(anyString(), anyString())).thenReturn(mockOBCriteria);
      when(mockOBCriteria.add(any())).thenReturn(mockOBCriteria);
      when(mockOBCriteria.list()).thenReturn(Arrays.asList(mockInvoiceLine, mockInvoiceLine2));

      when(soMatchCrit.add(any())).thenReturn(soMatchCrit);
      when(soMatchCrit.uniqueResult()).thenReturn(null);

      doNothing().when(salesMatchingHistory).createInvoiceSOMatch(any(InvoiceLine.class), any(OrderLine.class),
          any(BigDecimal.class), any(Date.class));

      salesMatchingHistory.backfillSOMatchFromInvoices(fromDate, now);

      ArgumentCaptor<BigDecimal> qtyCaptor = ArgumentCaptor.forClass(BigDecimal.class);
      ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);

      verify(salesMatchingHistory).createInvoiceSOMatch(eq(mockInvoiceLine), eq(mockOrderLine), qtyCaptor.capture(),
          dateCaptor.capture());
      verify(salesMatchingHistory, never()).createInvoiceSOMatch(eq(mockInvoiceLine2), any(OrderLine.class),
          any(BigDecimal.class), any(Date.class));

      assertEquals(qty, qtyCaptor.getValue());
      assertEquals(now, dateCaptor.getValue());
    }
  }

  /**
   * Verifies that {@link SalesMatchingHistory#createShipmentSOMatch(ShipmentInOutLine, OrderLine, BigDecimal, Date)}
   * correctly creates and persists a {@link SOMatch} for the SO → GS case.
   * <p>
   * The test asserts that:
   * <ul>
   *   <li>Client, organization and audit fields are populated from the shipment line.</li>
   *   <li>The sales order line and shipment line are linked to the match.</li>
   *   <li>The transaction date is taken from the shipment header movement date.</li>
   *   <li>The quantity and product are set as expected.</li>
   *   <li>The entity is saved through {@link OBDal#save(Object)}.</li>
   * </ul>
   */
  @Test
  void testCreateShipmentSOMatchCreatesAndSavesMatch() {
    BigDecimal qty = new BigDecimal("3");
    Date now = new Date();

    try (MockedStatic<OBProvider> obProviderStatic = mockStatic(
        OBProvider.class); MockedStatic<OBContext> obContextStatic = mockStatic(
        OBContext.class); MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {

      obProviderStatic.when(OBProvider::getInstance).thenReturn(mockOBProvider);
      when(mockOBProvider.get(SOMatch.class)).thenReturn(mockSOMatch);

      obContextStatic.when(OBContext::getOBContext).thenReturn(mockOBContext);
      when(mockOBContext.getUser()).thenReturn(mockUser);

      obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

      when(mockShipmentInOutLine.getClient()).thenReturn(mockClient);
      when(mockShipmentInOutLine.getOrganization()).thenReturn(mockOrganization);
      when(mockShipmentInOutLine.getShipmentReceipt()).thenReturn(mockShipmentInOut);
      when(mockShipmentInOutLine.getProduct()).thenReturn(mockProduct);
      when(mockShipmentInOut.getMovementDate()).thenReturn(now);

      salesMatchingHistory.createShipmentSOMatch(mockShipmentInOutLine, mockOrderLine, qty, now);

      verify(mockOBProvider).get(SOMatch.class);
      verify(mockSOMatch).setClient(mockClient);
      verify(mockSOMatch).setOrganization(mockOrganization);
      verify(mockSOMatch).setActive(true);
      verify(mockSOMatch).setCreationDate(now);
      verify(mockSOMatch).setCreatedBy(mockUser);
      verify(mockSOMatch).setUpdated(now);
      verify(mockSOMatch).setUpdatedBy(mockUser);
      verify(mockSOMatch).setSalesOrderLine(mockOrderLine);
      verify(mockSOMatch).setGoodsShipmentLine(mockShipmentInOutLine);
      verify(mockSOMatch).setTransactionDate(now);
      verify(mockSOMatch).setQuantity(qty);
      verify(mockSOMatch).setProcessNow(false);
      verify(mockSOMatch).setProcessed(true);
      verify(mockSOMatch).setPosted("N");
      verify(mockSOMatch).setProduct(mockProduct);
      verify(mockOBDal).save(mockSOMatch);
    }
  }

  /**
   * Verifies that {@link SalesMatchingHistory#createInvoiceSOMatch(InvoiceLine, OrderLine, BigDecimal, Date)}
   * correctly creates and persists a {@link SOMatch} for the SO → SI case.
   * <p>
   * The test asserts that:
   * <ul>
   *   <li>Client, organization and audit fields are populated from the invoice line.</li>
   *   <li>The sales order line and invoice line are linked to the match.</li>
   *   <li>Invoice-related fields (quantity, transaction date, product, flags) are
   *       set via {@link SalesMatchingHistory#fillInvoiceMatchCommonData(BaseOBObject, InvoiceLine, BigDecimal)}.</li>
   *   <li>The entity is saved through {@link OBDal#save(Object)}.</li>
   * </ul>
   */
  @Test
  void testCreateInvoiceSOMatchCreatesAndSavesMatch() {
    BigDecimal qty = new BigDecimal("5");
    Date now = new Date();

    try (MockedStatic<OBProvider> obProviderStatic = mockStatic(
        OBProvider.class); MockedStatic<OBContext> obContextStatic = mockStatic(
        OBContext.class); MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {

      obProviderStatic.when(OBProvider::getInstance).thenReturn(mockOBProvider);
      when(mockOBProvider.get(SOMatch.class)).thenReturn(mockSOMatch);

      obContextStatic.when(OBContext::getOBContext).thenReturn(mockOBContext);
      when(mockOBContext.getUser()).thenReturn(mockUser);

      obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

      when(mockInvoiceLine.getClient()).thenReturn(mockClient);
      when(mockInvoiceLine.getOrganization()).thenReturn(mockOrganization);

      doNothing().when(salesMatchingHistory).fillInvoiceMatchCommonData(mockSOMatch, mockInvoiceLine, qty);

      salesMatchingHistory.createInvoiceSOMatch(mockInvoiceLine, mockOrderLine, qty, now);

      verify(mockOBProvider).get(SOMatch.class);
      verify(mockSOMatch).setClient(mockClient);
      verify(mockSOMatch).setOrganization(mockOrganization);
      verify(mockSOMatch).setActive(true);
      verify(mockSOMatch).setCreationDate(now);
      verify(mockSOMatch).setCreatedBy(mockUser);
      verify(mockSOMatch).setUpdated(now);
      verify(mockSOMatch).setUpdatedBy(mockUser);
      verify(mockSOMatch).setSalesOrderLine(mockOrderLine);
      verify(salesMatchingHistory).fillInvoiceMatchCommonData(mockSOMatch, mockInvoiceLine, qty);
      verify(mockOBDal).save(mockSOMatch);
    }
  }

  /**
   * Verifies that {@link SalesMatchingHistory#fillInvoiceMatchCommonData(BaseOBObject, InvoiceLine, BigDecimal)}
   * populates all common fields when the invoice line has a product.
   * <p>
   * The test checks that:
   * <ul>
   *   <li>The invoice line, transaction date and quantity are set on the match.</li>
   *   <li>The {@code processNow}, {@code processed} and {@code posted} flags are initialized correctly.</li>
   *   <li>The product from the invoice line is copied to the match.</li>
   * </ul>
   */
  @Test
  void testFillInvoiceMatchCommonDataWithProduct() {
    BigDecimal qty = new BigDecimal("5");
    Date accountingDate = new Date();

    when(mockInvoiceLine.getInvoice()).thenReturn(mockInvoice);
    when(mockInvoice.getAccountingDate()).thenReturn(accountingDate);
    when(mockInvoiceLine.getProduct()).thenReturn(mockProduct);

    salesMatchingHistory.fillInvoiceMatchCommonData(mockBaseOBObject, mockInvoiceLine, qty);

    verify(mockBaseOBObject).set(SIMatch.PROPERTY_INVOICELINE, mockInvoiceLine);
    verify(mockBaseOBObject).set(SIMatch.PROPERTY_TRANSACTIONDATE, accountingDate);
    verify(mockBaseOBObject).set(SIMatch.PROPERTY_QUANTITY, qty);
    verify(mockBaseOBObject).set(SIMatch.PROPERTY_PROCESSNOW, false);
    verify(mockBaseOBObject).set(SIMatch.PROPERTY_PROCESSED, true);
    verify(mockBaseOBObject).set(SIMatch.PROPERTY_POSTED, "N");
    verify(mockBaseOBObject).set(SIMatch.PROPERTY_PRODUCT, mockProduct);

    verifyNoMoreInteractions(mockBaseOBObject);
  }

  /**
   * Verifies that {@link SalesMatchingHistory#fillInvoiceMatchCommonData(BaseOBObject, InvoiceLine, BigDecimal)}
   * behaves correctly when the invoice line has no product.
   * <p>
   * The test ensures that:
   * <ul>
   *   <li>The invoice line, transaction date and quantity are still set on the match.</li>
   *   <li>Control flags ({@code processNow}, {@code processed}, {@code posted}) are initialized as expected.</li>
   *   <li>No product is written to the match when the invoice line has a {@code null} product.</li>
   * </ul>
   */
  @Test
  void testFillInvoiceMatchCommonDataWithoutProduct() {
    BigDecimal qty = new BigDecimal("3");
    Date accountingDate = new Date();

    when(mockInvoiceLine.getInvoice()).thenReturn(mockInvoice);
    when(mockInvoice.getAccountingDate()).thenReturn(accountingDate);
    when(mockInvoiceLine.getProduct()).thenReturn(null);

    salesMatchingHistory.fillInvoiceMatchCommonData(mockBaseOBObject, mockInvoiceLine, qty);

    verify(mockBaseOBObject).set(SIMatch.PROPERTY_INVOICELINE, mockInvoiceLine);
    verify(mockBaseOBObject).set(SIMatch.PROPERTY_TRANSACTIONDATE, accountingDate);
    verify(mockBaseOBObject).set(SIMatch.PROPERTY_QUANTITY, qty);
    verify(mockBaseOBObject).set(SIMatch.PROPERTY_PROCESSNOW, false);
    verify(mockBaseOBObject).set(SIMatch.PROPERTY_PROCESSED, true);
    verify(mockBaseOBObject).set(SIMatch.PROPERTY_POSTED, "N");
    verify(mockBaseOBObject, never()).set(eq(SIMatch.PROPERTY_PRODUCT), any());
    verifyNoMoreInteractions(mockBaseOBObject);
  }

  /**
   * Verifies that {@link SalesMatchingHistory#incrementAndMaybeFlush(int)}
   * increments the counter without flushing when the batch size threshold
   * has not been reached.
   * <p>
   * The test asserts that:
   * <ul>
   *   <li>The returned counter is incremented by one.</li>
   *   <li>{@link OBDal#flush()} and session {@code clear()} are not invoked.</li>
   * </ul>
   */
  @Test
  void testIncrementAndMaybeFlushDoesNotFlushBeforeBatchSize() {
    int initialCounter = 5;

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {
      obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

      int result = salesMatchingHistory.incrementAndMaybeFlush(initialCounter);

      assertEquals(6, result);
      verify(mockOBDal, never()).flush();
      verify(mockSession, never()).clear();
    }
  }

  /**
   * Verifies that {@link SalesMatchingHistory#incrementAndMaybeFlush(int)}
   * flushes and clears the DAL session when the counter reaches the batch
   * boundary (e.g. a multiple of 100).
   * <p>
   * The test asserts that:
   * <ul>
   *   <li>The returned counter is incremented.</li>
   *   <li>{@link OBDal#flush()} is called once at the batch boundary.</li>
   *   <li>The Hibernate session {@code clear()} is also invoked.</li>
   * </ul>
   */
  @Test
  void testIncrementAndMaybeFlushFlushesOnBatchBoundary() {
    int initialCounter = 99;

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {
      obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.getSession()).thenReturn(mockSession);

      int result = salesMatchingHistory.incrementAndMaybeFlush(initialCounter);

      assertEquals(100, result);
      verify(mockOBDal, times(1)).flush();
      verify(mockSession, times(1)).clear();
    }
  }

  /**
   * Verifies that {@link SalesMatchingHistory#getAmountOfDays()} returns the
   * integer value stored in the {@code MatchedSalesDaysBack} preference.
   * <p>
   * The test mocks the preference lookup and checks that:
   * <ul>
   *   <li>The returned value matches the configured numeric string.</li>
   *   <li>No exception is thrown when the preference is valid.</li>
   * </ul>
   */
  @Test
  void testGetAmountOfDaysReturnsPreferenceValue() {
    try (MockedStatic<OBContext> obContextMock = mockStatic(
        OBContext.class); MockedStatic<Preferences> prefMock = mockStatic(Preferences.class)) {

      obContextMock.when(OBContext::getOBContext).thenReturn(mockOBContext);
      when(mockOBContext.getCurrentClient()).thenReturn(mockClient);
      when(mockOBContext.getCurrentOrganization()).thenReturn(mockOrganization);
      when(mockOBContext.getUser()).thenReturn(mockUser);
      when(mockOBContext.getRole()).thenReturn(mockRole);

      prefMock.when(
          () -> Preferences.getPreferenceValue("MatchedSalesDaysBack", true, mockClient, mockOrganization, mockUser,
              mockRole, null)).thenReturn("30");

      int result = SalesMatchingHistory.getAmountOfDays();

      assertEquals(30, result);
    }
  }

  /**
   * Verifies that {@link SalesMatchingHistory#getAmountOfDays()} throws an
   * {@link OBException} when the {@code MatchedSalesDaysBack} preference
   * is missing or cannot be parsed as an integer.
   * <p>
   * The test ensures that the error message key
   * {@code "MatchedSalesDaysBackError"} is used in this scenario.
   */
  @Test
  void testGetAmountOfDaysThrowsWhenPreferenceInvalid() {
    try (MockedStatic<OBContext> obContextMock = mockStatic(
        OBContext.class); MockedStatic<Preferences> prefMock = mockStatic(
        Preferences.class); MockedStatic<OBMessageUtils> msgMock = mockStatic(OBMessageUtils.class)) {

      obContextMock.when(OBContext::getOBContext).thenReturn(mockOBContext);
      when(mockOBContext.getCurrentClient()).thenReturn(mockClient);
      when(mockOBContext.getCurrentOrganization()).thenReturn(mockOrganization);
      when(mockOBContext.getUser()).thenReturn(mockUser);
      when(mockOBContext.getRole()).thenReturn(mockRole);

      prefMock.when(
          () -> Preferences.getPreferenceValue("MatchedSalesDaysBack", true, mockClient, mockOrganization, mockUser,
              mockRole, null)).thenReturn("not-a-number");

      msgMock.when(() -> OBMessageUtils.messageBD(MATCHED_DAYS_ERROR)).thenReturn(MATCHED_DAYS_ERROR);

      OBException ex = assertThrows(OBException.class, SalesMatchingHistory::getAmountOfDays);
      assertEquals(MATCHED_DAYS_ERROR, ex.getMessage());
    }
  }

  /**
   * Verifies that {@link SalesMatchingHistory#calculateFromDate(int)} returns
   * today's date at midnight when the offset is zero days.
   * <p>
   * The test checks that:
   * <ul>
   *   <li>The time-of-day components (hour, minute, second, millisecond) are zeroed.</li>
   *   <li>No subtraction is applied to the calendar day.</li>
   * </ul>
   */
  @Test
  void testCalculateFromDateWithZeroDays() {
    int amountOfDays = 0;
    Date result = SalesMatchingHistory.calculateFromDate(amountOfDays);
    assertNotNull(result);

    Calendar expectedCal = Calendar.getInstance();
    expectedCal.set(Calendar.HOUR_OF_DAY, 0);
    expectedCal.set(Calendar.MINUTE, 0);
    expectedCal.set(Calendar.SECOND, 0);
    expectedCal.set(Calendar.MILLISECOND, 0);
    Date expected = expectedCal.getTime();

    assertEquals(expected, result);
  }

  /**
   * Verifies that {@link SalesMatchingHistory#calculateFromDate(int)} correctly
   * subtracts the given number of days from today's date.
   * <p>
   * The test asserts that:
   * <ul>
   *   <li>The resulting date is exactly {@code amountOfDays} days in the past.</li>
   *   <li>The time-of-day components are normalized to midnight.</li>
   * </ul>
   */
  @Test
  void testCalculateFromDateWithPositiveDays() {
    int amountOfDays = 10;
    Date result = SalesMatchingHistory.calculateFromDate(amountOfDays);
    assertNotNull(result);

    Calendar expectedCal = Calendar.getInstance();
    expectedCal.set(Calendar.HOUR_OF_DAY, 0);
    expectedCal.set(Calendar.MINUTE, 0);
    expectedCal.set(Calendar.SECOND, 0);
    expectedCal.set(Calendar.MILLISECOND, 0);
    expectedCal.add(Calendar.DAY_OF_YEAR, -amountOfDays);
    Date expected = expectedCal.getTime();

    assertEquals(expected, result);
  }
}
