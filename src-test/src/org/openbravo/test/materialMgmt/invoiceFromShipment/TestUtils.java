/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2018-2019 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.materialMgmt.invoiceFromShipment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.db.CallStoredProcedure;

public class TestUtils {

  private static final String COMPLETE_ACTION = "CO";
  private static final String DRAFT_STATUS = "DR";

  /**
   * Returns a new Product based on the given one. It is a clone of the first one but with different
   * name and value
   * 
   * @param productId
   *          Id of the original Product
   * @param name
   *          Name of the original Product
   * @return A new Product clone based on the original one
   */
  static Product cloneProduct(final String productId, final String name) {
    final Product oldProduct = OBDal.getInstance().get(Product.class, productId);
    final Product newProduct = (Product) DalUtil.copy(oldProduct, false);
    int numberOfProductsWithSameName = getNumberOfProducts(name) + 1;

    newProduct.setSearchKey(name + "-" + numberOfProductsWithSameName);
    newProduct.setName(name + "-" + numberOfProductsWithSameName);
    newProduct.setId(SequenceIdData.getUUID());
    newProduct.setNewOBObject(true);

    OBDal.getInstance().save(newProduct);

    // Clone Prices
    final List<ProductPrice> oldPrices = oldProduct.getPricingProductPriceList();
    final List<ProductPrice> newPrices = new ArrayList<>();
    for (ProductPrice productPrice : oldPrices) {
      final ProductPrice newProductPrice = (ProductPrice) DalUtil.copy(productPrice, false);
      newProductPrice.setNewOBObject(true);
      newProductPrice.setId(SequenceIdData.getUUID());
      newProductPrice.setProduct(newProduct);
      OBDal.getInstance().save(newProductPrice);
      newPrices.add(newProductPrice);
    }
    newProduct.setPricingProductPriceList(newPrices);
    OBDal.getInstance().flush();

    return newProduct;
  }

  /**
   * Returns the number of products with same Product name
   */
  static int getNumberOfProducts(final String name) {
    try {
      final OBCriteria<Product> criteria = OBDal.getInstance().createCriteria(Product.class);
      criteria.add(Restrictions.like(Product.PROPERTY_NAME, name + "-%"));
      return criteria.count();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * Returns a new Order Line based on the given one. It is a clone of the first one but without
   * being invoiced or delivered
   * 
   * @return A new Order Line clone based on the original one
   */
  public static Order cloneOrder(final String orderId, final String docNo) {
    final Order oldOrder = OBDal.getInstance().get(Order.class, orderId);
    final Order newOrder = (Order) DalUtil.copy(oldOrder, false);
    int numberOfOrdersWithSameDocNo = getNumberOfOrders(docNo) + 1;

    newOrder.setId(SequenceIdData.getUUID());
    newOrder.setDocumentNo(docNo + "-" + numberOfOrdersWithSameDocNo);
    newOrder.setProcessed(false);
    newOrder.setDocumentStatus(DRAFT_STATUS);
    newOrder.setDocumentAction(COMPLETE_ACTION);
    newOrder.setCancelled(false);
    newOrder.setOrderDate(Calendar.getInstance().getTime());
    newOrder.setScheduledDeliveryDate(Calendar.getInstance().getTime());
    newOrder.setNewOBObject(true);

    OBDal.getInstance().save(newOrder);

    for (OrderLine orderLine : oldOrder.getOrderLineList()) {
      cloneOrderLine(orderLine, newOrder);
    }

    OBDal.getInstance().flush();

    return newOrder;
  }

  /**
   * Returns the number of orders with same Document Number
   */
  static int getNumberOfOrders(final String docNo) {
    try {
      final OBCriteria<Order> criteria = OBDal.getInstance().createCriteria(Order.class);
      criteria.add(Restrictions.like(Order.PROPERTY_DOCUMENTNO, docNo + "-%"));
      return criteria.list().size();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * Returns a new Order Line based on the given one. It is a clone of the first one but without
   * being invoiced or delivered
   * 
   * @param oldLine
   *          Order Line to be cloned
   * @param newOrder
   *          new Order (a clone of the original one)
   * @return A new Order Line clone based on the original one
   */
  public static OrderLine cloneOrderLine(final OrderLine oldLine, final Order newOrder) {

    // Skip discount lines
    if (oldLine.getOrderDiscount() != null) {
      return null;
    }

    final OrderLine newLine = (OrderLine) DalUtil.copy(oldLine, false);

    newLine.setId(SequenceIdData.getUUID());
    newLine.setSalesOrder(newOrder);
    newLine.setDeliveredQuantity(BigDecimal.ZERO);
    newLine.setInvoicedQuantity(BigDecimal.ZERO);
    newLine.setReservedQuantity(BigDecimal.ZERO);
    newLine.setNewOBObject(true);

    OBDal.getInstance().save(newLine);
    newOrder.getOrderLineList().add(newLine);

    return newLine;
  }

  /**
   * Calls C_Order_Post Database Function to complete the given Order
   * 
   * @param order
   *          Order to be completed
   * @throws OBException
   */
  static void processOrder(final Order order) throws OBException {
    final List<Object> parameters = new ArrayList<Object>();
    parameters.add(null);
    parameters.add(order.getId());
    final String procedureName = "c_order_post1";
    CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
  }

  /**
   * Returns a new Goods Receipt/Shipment based on the given one. It is a clone of the first one but
   * in a not completed status
   * 
   * @param mInoutId
   *          Id of original Goods Receipt/Shipment to clone
   * @param docNo
   *          docNo to set to the new Goods Receipt/Shipment
   * @return a Goods Receipt/Shipment not completed
   */
  static ShipmentInOut cloneReceiptShipment(final String mInoutId, final String docNo) {
    final ShipmentInOut oldInOut = OBDal.getInstance().get(ShipmentInOut.class, mInoutId);
    final ShipmentInOut newInOut = (ShipmentInOut) DalUtil.copy(oldInOut, false);
    int numberOfShipmentsWithSameDocNo = getNumberOfShipments(docNo) + 1;

    newInOut.setId(SequenceIdData.getUUID());
    newInOut.setDocumentNo(docNo + "-" + numberOfShipmentsWithSameDocNo);
    newInOut.setDocumentStatus(DRAFT_STATUS);
    newInOut.setDocumentAction(COMPLETE_ACTION);
    newInOut.setProcessed(false);
    newInOut.setMovementDate(new Date());
    newInOut.setOrderDate(new Date());
    newInOut.setNewOBObject(true);
    newInOut.setSalesOrder(null);

    OBDal.getInstance().save(newInOut);

    for (ShipmentInOutLine line : oldInOut.getMaterialMgmtShipmentInOutLineList()) {
      cloneReceiptShipmentLine(line, newInOut);
    }

    OBDal.getInstance().flush();
    return newInOut;
  }

  /**
   * Returns the number of Goods Receipts/Shipments with same Document Number
   */
  static int getNumberOfShipments(final String docNo) {
    try {
      final OBCriteria<ShipmentInOut> criteria = OBDal.getInstance()
          .createCriteria(ShipmentInOut.class);
      criteria.add(Restrictions.like(ShipmentInOut.PROPERTY_DOCUMENTNO, docNo + "-%"));
      return criteria.list().size();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * Returns a new Goods Receipt/Shipment Line based on the given one. It is a clone of the first
   * one but with different product
   * 
   * @param line
   *          Original Goods Receipt/Shipment
   * @param newInOut
   *          new Goods Receipt/Shipment (a clone of the original one)
   * @return A new Goods Receipt/Shipment Line clone based on the original one
   */
  static ShipmentInOutLine cloneReceiptShipmentLine(final ShipmentInOutLine oldLine,
      final ShipmentInOut newInOut) {
    final ShipmentInOutLine newLine = (ShipmentInOutLine) DalUtil.copy(oldLine, false);

    newLine.setId(SequenceIdData.getUUID());
    newLine.setShipmentReceipt(newInOut);
    newLine.setNewOBObject(true);
    newLine.setSalesOrderLine(null);

    OBDal.getInstance().save(newLine);
    newInOut.getMaterialMgmtShipmentInOutLineList().add(newLine);

    return newLine;
  }

  /**
   * Calls M_Inout_Post Database Function to complete the given Shipment/Receipt
   * 
   * @param shipmentReceipt
   *          Shipment or Receipt to be completed
   * @throws OBException
   */
  static void processShipmentReceipt(final ShipmentInOut shipmentReceipt) throws OBException {
    final List<Object> parameters = new ArrayList<Object>();
    parameters.add(null);
    parameters.add(shipmentReceipt.getId());
    final String procedureName = "m_inout_post";
    CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
  }

  /**
   * Calls the C_INVOICE_POST stored procedure
   * 
   * @param invoice
   *          The Invoice.
   * @throws Exception
   */
  public static void processInvoice(final Invoice invoice) throws Exception {
    if (invoice != null) {
      final List<Object> parameters = new ArrayList<>();
      parameters.add(null); // Process Instance parameter
      parameters.add(invoice.getId());
      CallStoredProcedure.getInstance().call("C_INVOICE_POST", parameters, null, true, false);
    }
  }
}
