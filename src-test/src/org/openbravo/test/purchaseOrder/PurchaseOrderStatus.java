package org.openbravo.test.purchaseOrder;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.costing.utils.TestCostingUtils.reactivateInvoice;
import static org.openbravo.test.purchaseOrder.PurchaseOrderUtils.createPurchaseOrder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.hibernate.criterion.Restrictions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.advpaymentmngt.ProcessOrderUtil;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.financial.ResetAccounting;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.utils.Utils;

/**
 * Test class for verifying purchase order status updates during order processing.
 * Extends {@link WeldBaseTest} and uses dependency injection for utility access.
 */
public class PurchaseOrderStatus extends WeldBaseTest {

  @Inject
  private WeldUtils weldUtils;

  /**
   * Initializes OBContext and secure app variables before each test.
   *
   * @throws Exception
   *     if setup fails
   */
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    Utils.initializeTestContext();
  }

  /**
   * Verifies that the purchase order status is correctly updated when a
   * purchase order is created, a goods receipt is posted, and a purchase
   * invoice is posted.
   *
   * @throws Exception
   *     if any error occurs during the test
   */
  @Test
  public void testPurchaseOrderStatus() {
    Invoice purchaseInvoice = null;
    Order purchaseOrder = null;
    ShipmentInOut goodsReceipt = null;

    try {
      purchaseOrder = createPurchaseOrder();
      processOrder(purchaseOrder, PurchaseOrderUtils.COMPLETED);

      OBDal.getInstance().commitAndClose();
      OBDal.getInstance().refresh(purchaseOrder);

      for (OrderLine orderLine : purchaseOrder.getOrderLineList()) {
        assertEquals(BigDecimal.TEN, orderLine.getOrderedQuantity());
        assertEquals(BigDecimal.ZERO, orderLine.getDeliveredQuantity());
      }

      assertEquals(Long.valueOf(0), purchaseOrder.getDeliveryStatusPurchase());
      assertEquals(Long.valueOf(0), purchaseOrder.getInvoiceStatus());

      goodsReceipt = PurchaseOrderUtils.createGoodsReceipt(purchaseOrder);
      processGoodsReceipt(goodsReceipt);

      OBDal.getInstance().commitAndClose();
      OBDal.getInstance().refresh(purchaseOrder);

      for (OrderLine orderLine : purchaseOrder.getOrderLineList()) {
        assertEquals(BigDecimal.TEN, orderLine.getOrderedQuantity());
        assertEquals(BigDecimal.TEN, orderLine.getDeliveredQuantity());
      }

      assertEquals(Long.valueOf(100), purchaseOrder.getDeliveryStatusPurchase());
      assertEquals(Long.valueOf(0), purchaseOrder.getInvoiceStatus());

      purchaseInvoice = PurchaseOrderUtils.createPurchaseInvoice(goodsReceipt);
      processPurchaseInvoice(purchaseInvoice);

      OBDal.getInstance().commitAndClose();
      OBDal.getInstance().refresh(purchaseOrder);

      for (OrderLine orderLine : purchaseOrder.getOrderLineList()) {
        assertEquals(BigDecimal.TEN, orderLine.getOrderedQuantity());
        assertEquals(BigDecimal.TEN, orderLine.getDeliveredQuantity());
      }

      assertEquals(Long.valueOf(100), purchaseOrder.getDeliveryStatusPurchase());
      assertEquals(Long.valueOf(100), purchaseOrder.getInvoiceStatus());

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    } finally {
      cleanUpData(purchaseOrder, goodsReceipt, purchaseInvoice);
    }
  }

  /**
   * Cleans up the test environment by rolling back the transaction and closing the session.
   */
  @After
  public void cleanUp() {
    OBDal.getInstance().rollbackAndClose();
  }

  /**
   * Processes a sales order by invoking the order processing utility.
   * This method executes a specific document action on the provided order
   * using the {@link ProcessOrderUtil} class. It ensures that the action is
   * processed within the context of the current session.
   *
   * @param order
   *     the sales order to be processed
   * @param docAction
   *     the document action to be performed on the order
   */
  private void processOrder(Order order, String docAction) {
    var processor = weldUtils.getInstance(ProcessOrderUtil.class);

    processor.process(order.getId(), docAction, RequestContext.get().getVariablesSecureApp(),
        new DalConnectionProvider(false));
  }

  /**
   * Posts a goods receipt and processes the inventory changes.
   *
   * @param receipt
   *     the goods receipt to be posted
   * @return the processed goods receipt
   */
  private static ShipmentInOut processGoodsReceipt(ShipmentInOut receipt) {
    final List<Object> params = new ArrayList<>();
    params.add(null);
    params.add(receipt.getId());

    CallStoredProcedure.getInstance().call("M_INOUT_POST", params, null, true, false);

    OBDal.getInstance().refresh(receipt);
    return receipt;
  }

  /**
   * Posts a purchase invoice and processes the necessary accounting entries.
   *
   * @param invoice
   *     the purchase invoice to be posted
   * @return the processed purchase invoice
   */
  private static Invoice processPurchaseInvoice(Invoice invoice) {
    final List<Object> params = new ArrayList<>();
    params.add(null);
    params.add(invoice.getId());

    CallStoredProcedure.getInstance().call("C_INVOICE_POST", params, null, true, false);

    OBDal.getInstance().refresh(invoice);
    return invoice;
  }

  /**
   * Cleans up the test data by reactivating and deleting the given purchase order, goods receipt, and purchase invoice.
   * This method is used to restore the initial state of the test environment after a test has been executed.
   *
   * @param purchaseOrder the purchase order to reactivate and delete
   * @param goodsReceipt the goods receipt to reactivate and delete
   * @param purchaseInvoice the purchase invoice to reactivate and delete
   */
  private void cleanUpData(Order purchaseOrder, ShipmentInOut goodsReceipt, Invoice purchaseInvoice) {
    if (purchaseInvoice != null) {
      reactivateAndDeletePurchaseInvoice(purchaseInvoice);
    }

    if (goodsReceipt != null) {
      reactivateAndDeleteShipment(goodsReceipt);
    }

    if (purchaseOrder != null) {
      reactivateAndDeletePurchaseOrder(purchaseOrder);
    }

    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
  }

  /**
   * Reactivates and deletes the given purchase invoice. This method is used to restore the
   * initial state of a purchase invoice after a test has been executed.
   *
   * @param purchaseInvoice the purchase invoice to reactivate and delete
   */
  private void reactivateAndDeletePurchaseInvoice(Invoice purchaseInvoice) {
    try {
      if (purchaseInvoice == null) {
        return;
      }

      purchaseInvoice = OBDal.getInstance().get(Invoice.class, purchaseInvoice.getId());

      ResetAccounting.delete(purchaseInvoice.getClient().getId(), purchaseInvoice.getOrganization().getId(),
          purchaseInvoice.getEntity().getTableId(), purchaseInvoice.getId(),
          OBDateUtils.formatDate(purchaseInvoice.getAccountingDate()), null);

      OBDal.getInstance().refresh(purchaseInvoice);

      if (purchaseInvoice.isProcessed()) {
        reactivateInvoice(purchaseInvoice);
      }

      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

      purchaseInvoice = OBDal.getInstance().get(Invoice.class, purchaseInvoice.getId());
      OBDal.getInstance().remove(purchaseInvoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      Assert.fail(e.getMessage());
    }
  }

  /**
   * Reactivates and deletes the given purchase order. This method is used to restore the initial state
   * of a purchase order after a test has been executed.
   *
   * @param purchaseOrder the purchase order to reactivate and delete
   */
  private void reactivateAndDeletePurchaseOrder(Order purchaseOrder) {
    try {
      if (purchaseOrder == null) {
        return;
      }

      purchaseOrder = OBDal.getInstance().get(Order.class, purchaseOrder.getId());

      for (OrderLine line : purchaseOrder.getOrderLineList()) {
        line.setDeliveredQuantity(BigDecimal.ZERO);
        line.setInvoicedQuantity(BigDecimal.ZERO);

        for (InvoiceLine invoiceLine : line.getInvoiceLineList()) {
          invoiceLine.setSalesOrderLine(null);
          OBDal.getInstance().save(invoiceLine);
        }

        OBDal.getInstance().save(line);
      }
      OBDal.getInstance().flush();

      processOrder(purchaseOrder, PurchaseOrderUtils.REACTIVATE);

      OBDal.getInstance().remove(purchaseOrder);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      Assert.fail(e.getMessage());
    }
  }

  /**
   * Reactivates and deletes the given shipment in/out. This method is used to restore the initial state of a shipment in/out after a test has been executed.
   *
   * @param shipmentInOut
   *     the shipment in/out to reactivate and delete
   */
  private void reactivateAndDeleteShipment(ShipmentInOut shipmentInOut) {
    try {
      if (shipmentInOut == null) {
        return;
      }
      shipmentInOut = OBDal.getInstance().get(ShipmentInOut.class, shipmentInOut.getId());

      TriggerHandler.getInstance().disable();

      OBCriteria<ShipmentInOutLine> criteria = OBDal.getInstance().createCriteria(ShipmentInOutLine.class);
      criteria.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, shipmentInOut));
      List<ShipmentInOutLine> lines = criteria.list();

      for (ShipmentInOutLine line : lines) {
        MaterialTransaction tx = getTransactionByLine(line);
        if (tx != null) {
          OBDal.getInstance().remove(tx);
        }
      }
      OBDal.getInstance().flush();

      shipmentInOut.setProcessed(false);
      shipmentInOut.setPosted("N");
      OBDal.getInstance().save(shipmentInOut);
      OBDal.getInstance().flush();

      for (ShipmentInOutLine line : lines) {
        OBDal.getInstance().remove(line);
      }
      OBDal.getInstance().flush();

      OBDal.getInstance().remove(shipmentInOut);
      OBDal.getInstance().flush();

      if (TriggerHandler.getInstance().isDisabled()) {
        TriggerHandler.getInstance().enable();
      }

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      Assert.fail(e.getMessage());
    } finally {
      if (TriggerHandler.getInstance().isDisabled()) {
        TriggerHandler.getInstance().enable();
      }
    }
  }

  /**
   * Returns a material transaction for the given shipment in/out line.
   *
   * @param line
   *     the shipment in/out line
   * @return the material transaction or {@code null}
   */
  private static MaterialTransaction getTransactionByLine(ShipmentInOutLine line) {
    OBCriteria<MaterialTransaction> cTransaction = OBDal.getInstance().createCriteria(MaterialTransaction.class);
    cTransaction.add(Restrictions.eq(MaterialTransaction.PROPERTY_GOODSSHIPMENTLINE, line));
    cTransaction.setMaxResults(1);
    return (MaterialTransaction) cTransaction.uniqueResult();
  }
}
