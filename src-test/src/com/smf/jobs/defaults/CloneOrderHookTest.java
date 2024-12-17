package com.smf.jobs.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.CloneOrderHookCaller;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.service.db.CallStoredProcedure;

/**
 * Test class for {@link CloneOrderHook}.
 * <p>
 * This class performs unit tests for the {@link CloneOrderHook} class methods,
 * ensuring the functionality of order cloning processes.
 * </p>
 */
@RunWith(MockitoJUnitRunner.class)
public class CloneOrderHookTest {

  @InjectMocks
  private CloneOrderHook cloneOrderHook;

  @Mock
  private Order originalOrder;

  @Mock
  private Order clonedOrder;

  @Mock
  private User currentUser;

  @Mock
  private OBDal obDal;

  @Mock
  private Session session;

  @Mock
  private OBContext obContext;

  @Mock
  private OrderLine originalOrderLine;

  @Mock
  private Product product;

  @Mock
  private PriceList priceList;

  @Mock
  private Client client;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<WeldUtils> mockedWeldUtils;
  private MockedStatic<DalUtil> mockedDalUtil;
  private MockedStatic<CallStoredProcedure> mockedCallStoredProcedure;
  private Method cloneOrderMethod;

  /**
   * Sets up the test environment by initializing mocks and reflection method.
   *
   * @throws Exception if the reflection setup fails.
   */
  @Before
  public void setUp() throws Exception {
    cloneOrderMethod = CloneOrderHook.class.getDeclaredMethod(
        "cloneOrder",
        User.class,
        Order.class,
        Order.class
    );
    cloneOrderMethod.setAccessible(true);

    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedWeldUtils = mockStatic(WeldUtils.class);
    mockedDalUtil = mockStatic(DalUtil.class);
    mockedCallStoredProcedure = mockStatic(CallStoredProcedure.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);
    when(obDal.getSession()).thenReturn(session);
  }

  /**
   * Tests the {@code cloneOrder} method to verify proper order cloning.
   *
   * @throws Exception if the reflection invocation fails.
   */
  @Test
  public void testCloneOrder() throws Exception {
    mock(Organization.class);

    List<OrderLine> originalOrderLines = new ArrayList<>();
    originalOrderLines.add(originalOrderLine);
    when(originalOrder.getOrderLineList()).thenReturn(originalOrderLines);
    when(clonedOrder.getOrderLineList()).thenReturn(new ArrayList<>());

    when(originalOrder.getPriceList()).thenReturn(priceList);
    when(originalOrder.getClient()).thenReturn(client);
    when(originalOrder.isSalesTransaction()).thenReturn(true);
    when(priceList.getId()).thenReturn("testPriceListId");
    when(client.getId()).thenReturn("testClientId");

    when(originalOrderLine.getProduct()).thenReturn(product);
    when(product.getId()).thenReturn("testProductId");
    when(originalOrderLine.getId()).thenReturn("testOrderLineId");
    when(originalOrderLine.getOrderlineServiceRelationList()).thenReturn(new ArrayList<>());

    OrderLine clonedOrderLine = mock(OrderLine.class);
    mockedDalUtil.when(() -> DalUtil.copy(any(OrderLine.class), eq(false)))
        .thenReturn(clonedOrderLine);

    OBQuery<PriceListVersion> mockQuery = mock(OBQuery.class);
    when(obDal.createQuery(eq(PriceListVersion.class), anyString())).thenReturn(mockQuery);
    when(mockQuery.setNamedParameter(anyString(), any())).thenReturn(mockQuery);
    when(mockQuery.list()).thenReturn(new ArrayList<>());

    CallStoredProcedure mockStoredProcedure = mock(CallStoredProcedure.class);
    when(mockStoredProcedure.call(anyString(), any(), any())).thenReturn(BigDecimal.ONE);
    mockedCallStoredProcedure.when(CallStoredProcedure::getInstance).thenReturn(mockStoredProcedure);

    CloneOrderHookCaller mockCaller = mock(CloneOrderHookCaller.class);
    mockedWeldUtils.when(() -> WeldUtils.getInstanceFromStaticBeanManager(CloneOrderHookCaller.class))
        .thenReturn(mockCaller);
    doNothing().when(mockCaller).executeHook(any(Order.class));

    Order result = (Order) cloneOrderMethod.invoke(cloneOrderHook, currentUser, originalOrder, clonedOrder);

    assertNotNull("Cloned order should not be null", result);

    verify(clonedOrder).setDocumentAction("CO");
    verify(clonedOrder).setDocumentStatus("DR");
    verify(clonedOrder).setPosted("N");
    verify(clonedOrder).setProcessed(false);
    verify(clonedOrder).setDelivered(false);
    verify(clonedOrder, times(2)).setSalesTransaction(anyBoolean()); // Allow 2 calls
    verify(clonedOrder).setDocumentNo(null);
    verify(clonedOrder).setCreatedBy(currentUser);
    verify(clonedOrder).setUpdatedBy(currentUser);
    verify(clonedOrder).setGrandTotalAmount(BigDecimal.ZERO);
    verify(clonedOrder).setSummedLineAmount(BigDecimal.ZERO);

    verify(obDal).save(clonedOrder);
    verify(obDal).flush();
    verify(obDal).refresh(clonedOrder);

    verify(clonedOrderLine).setSalesOrder(clonedOrder);
    verify(clonedOrderLine).setReservedQuantity(BigDecimal.ZERO);
    verify(clonedOrderLine).setDeliveredQuantity(BigDecimal.ZERO);
    verify(clonedOrderLine).setInvoicedQuantity(BigDecimal.ZERO);
  }

  /**
   * Tests the {@code getLineNetAmt} method to ensure it retrieves the correct line amount.
   * <p>
   * It verifies that the query is executed correctly and returns the expected result.
   * </p>
   */
  @Test
  public void testGetLineNetAmt() {
    String testOrderId = "test-order-id";
    BigDecimal expectedAmount = new BigDecimal("100.00");
    List<BigDecimal> amounts = new ArrayList<>();
    amounts.add(expectedAmount);

    @SuppressWarnings("unchecked")
    Query<BigDecimal> mockQuery = mock(Query.class);
    when(session.createQuery(anyString(), eq(BigDecimal.class))).thenReturn(mockQuery);
    when(mockQuery.setParameter(anyString(), anyString())).thenReturn(mockQuery);
    when(mockQuery.list()).thenReturn(amounts);

    BigDecimal result = CloneOrderHook.getLineNetAmt(testOrderId);

    assertEquals("Line net amount should match expected value", expectedAmount, result);
    verify(mockQuery).setParameter("orderId", testOrderId);
  }

  /**
   * Tests the {@code shouldCopyChildren} method.
   * <p>
   * Ensures that the method always returns {@code false}.
   * </p>
   */
  @Test
  public void testShouldCopyChildren() {
    boolean result = cloneOrderHook.shouldCopyChildren(true);
    assertEquals("Should always return false regardless of input", false, result);
  }

  /**
   * Tests the {@code preCopy} method.
   * <p>
   * Verifies that the original record is returned without modification.
   * </p>
   */
  @Test
  public void testPreCopy() {
    BaseOBObject result = cloneOrderHook.preCopy(originalOrder);
    assertEquals("Should return original record without modification", originalOrder, result);
  }

  /**
   * Cleans up the test environment and closes static mocks.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedWeldUtils != null) {
      mockedWeldUtils.close();
    }
    if (mockedDalUtil != null) {
      mockedDalUtil.close();
    }
    if (mockedCallStoredProcedure != null) {
      mockedCallStoredProcedure.close();
    }
  }

}