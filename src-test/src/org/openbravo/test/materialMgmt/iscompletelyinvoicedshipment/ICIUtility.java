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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.materialMgmt.iscompletelyinvoicedshipment;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DbUtility;

public class ICIUtility {

  /**
   * Set the default context for the Automated Tests with:
   * 
   * <ul>
   * <li>User: Openbravo</li>
   * <li>Role: F&amp;B International Group Administrator</li>
   * <li>Client: F&amp;B International Group</li>
   * <li>Organization: F&amp;B España</li>
   * </ul>
   */
  public static void setTestContextSpain() {
    OBContext.setOBContext(ICIConstants.OPENBRAVO_USER_ID, ICIConstants.FNB_INT_GROUP_ADMIN_ROLE_ID,
        ICIConstants.FNB_GROUP_CLIENT_ID, ICIConstants.FNB_ESPANA_ORG_ID);
  }

  /**
   * Creates and saves a New {@link Order} with the default Parameters for the automated Tests
   */
  public static Order createNewSalesOrderWithDefaultParameters() {
    ICIOrderHeaderParameters orderHeaderParameters = new ICIOrderHeaderParameters();
    Order order = ICIUtility.createNewOrder(orderHeaderParameters);
    return order;
  }

  private static Order createNewOrder(ICIOrderHeaderParameters parameters) {
    Order order = OBProvider.getInstance().get(Order.class);
    order.setOrganization(parameters.getOrganization());
    order.setClient(parameters.getClient());
    order.setDocumentType(parameters.getDocumentType());
    order.setTransactionDocument(parameters.getDocumentType());
    order.setDocumentNo(FIN_Utility.getDocumentNo(parameters.getOrganization(),
        ICIConstants.SALES_ORDER_DOCBASETYPE, ICIConstants.C_ORDER_TABLE));
    order.setAccountingDate(parameters.getAccountingDate());
    order.setOrderDate(parameters.getOrderDate());
    order.setWarehouse(parameters.getWarehouse());
    if (StringUtils.isNotEmpty(parameters.getInvoiceTerms())) {
      order.setInvoiceTerms(parameters.getInvoiceTerms());
    }
    order.setBusinessPartner(parameters.getBusinessPartner());
    order.setPartnerAddress(parameters.getBusinessPartnerLocation());
    order.setPriceList(parameters.getPriceList());
    order.setCurrency(parameters.getCurrency());
    order.setSummedLineAmount(BigDecimal.ZERO);
    order.setGrandTotalAmount(BigDecimal.ZERO);
    order.setSalesTransaction(parameters.isReceipt());
    order.setPaymentMethod(parameters.getPaymentMethod());
    order.setPaymentTerms(parameters.getPaymentTerms());

    OBDal.getInstance().save(order);
    OBDal.getInstance().flush();

    return order;
  }

  /**
   * Creates and saves a new {@link OrderLine} with the default parameters for the automated tests
   */
  public static OrderLine createNewSalesOrderLineWithDefaultParameters(Order order) {
    ICIOrderLineParameters orderLineParameters = new ICIOrderLineParameters(order);
    OrderLine orderLine = ICIUtility.createNewOrderLine(orderLineParameters);
    return orderLine;
  }

  /**
   * Creates and saves a new {@link OrderLine} with the default parameters for the automated tests
   */
  public static OrderLine createNewOrderLine(ICIOrderLineParameters parameters) {
    // Create one line
    OrderLine orderLine = OBProvider.getInstance().get(OrderLine.class);
    orderLine.setOrganization(parameters.getOrganization());
    orderLine.setClient(parameters.getClient());
    orderLine.setSalesOrder(parameters.getOrder());
    orderLine.setOrderDate(parameters.getOrderDate());
    orderLine.setWarehouse(parameters.getWarehouse());
    orderLine.setCurrency(parameters.getCurrency());
    orderLine.setLineNo(parameters.getLineNo());
    orderLine.setProduct(parameters.getProduct());
    orderLine.setUOM(parameters.getUom());
    orderLine.setInvoicedQuantity(BigDecimal.ZERO);
    orderLine.setOrderedQuantity(parameters.getOrderedQuantity());
    orderLine.setUnitPrice(parameters.getNetUnitPrice());
    orderLine.setListPrice(parameters.getNetListPrice());
    orderLine.setPriceLimit(parameters.getPriceLimit());
    orderLine.setTax(parameters.getTaxRate());
    orderLine.setLineNetAmount(parameters.getLineNetAmount());

    OBDal.getInstance().save(orderLine);
    OBDal.getInstance().flush();

    return orderLine;

  }

  /**
   * Calls C_Order_Post method to process a {@link Order} given as a parameter
   */
  public static boolean processOrder(Order order) {
    return processDocument(order, ICIConstants.C_ORDER_POST_ID);
  }

  /**
   * Creates and saves a new {@link Invoice} with the default parameters for the automated tests
   */
  public static Invoice createNewSalesInvoiceWithDefaultParameters() throws Exception {
    ICIInvoiceHeaderParameters invoiceHeaderParameters = new ICIInvoiceHeaderParameters();
    Invoice invoice = ICIUtility.createNewInvoice(invoiceHeaderParameters);
    return invoice;
  }

  private static Invoice createNewInvoice(ICIInvoiceHeaderParameters parameters) throws Exception {

    // Create header
    Invoice invoice = OBProvider.getInstance().get(Invoice.class);
    invoice.setOrganization(parameters.getOrganization());
    invoice.setClient(parameters.getClient());
    invoice.setDocumentType(parameters.getDocumentType());
    invoice.setTransactionDocument(parameters.getDocumentType());
    invoice.setDocumentNo(FIN_Utility.getDocumentNo(parameters.getOrganization(),
        ICIConstants.SALES_INVOICE_DOCBASETYPE, ICIConstants.C_INVOICE_TABLE));
    invoice.setAccountingDate(parameters.getAccountingDate());
    invoice.setInvoiceDate(parameters.getInvoiceDate());
    invoice.setTaxDate(parameters.getTaxDate());
    invoice.setBusinessPartner(parameters.getBusinessPartner());
    invoice.setPartnerAddress(parameters.getBusinessPartnerLocation());
    invoice.setPriceList(parameters.getPriceList());
    invoice.setCurrency(parameters.getCurrency());
    invoice.setSummedLineAmount(BigDecimal.ZERO);
    invoice.setGrandTotalAmount(BigDecimal.ZERO);
    invoice.setWithholdingamount(BigDecimal.ZERO);
    invoice.setSalesTransaction(parameters.isReceipt());
    invoice.setPaymentMethod(parameters.getPaymentMethod());
    invoice.setPaymentTerms(parameters.getPaymentTerms());

    OBDal.getInstance().save(invoice);
    OBDal.getInstance().flush();

    return invoice;
  }

  /**
   * Creates and saves a new {@link InvoiceLine} with the default parameters for the automated tests
   */
  public static InvoiceLine createNewSalesInvoiceLineWithDefaultParameters(Invoice invoice) {
    ICIInvoiceLineParameters invoiceLineParameters = new ICIInvoiceLineParameters(invoice);
    InvoiceLine invoiceLine = ICIUtility.createNewInvoiceLine(invoiceLineParameters);
    return invoiceLine;
  }

  /**
   * Creates and saves a new {@link InvoiceLine} with the default parameters for the automated tests
   */
  public static InvoiceLine createNewInvoiceLine(ICIInvoiceLineParameters parameters) {
    InvoiceLine invoiceLine = OBProvider.getInstance().get(InvoiceLine.class);
    invoiceLine.setOrganization(parameters.getOrganization());
    invoiceLine.setClient(parameters.getClient());
    invoiceLine.setInvoice(parameters.getInvoice());
    invoiceLine.setLineNo(parameters.getLineNo());
    invoiceLine.setProduct(parameters.getProduct());
    invoiceLine.setUOM(parameters.getUom());
    invoiceLine.setInvoicedQuantity(parameters.getInvoicedQuantity());
    invoiceLine.setUnitPrice(parameters.getNetUnitPrice());
    invoiceLine.setListPrice(parameters.getNetListPrice());
    invoiceLine.setPriceLimit(parameters.getPriceLimit());
    invoiceLine.setTax(parameters.getTaxRate());
    invoiceLine.setLineNetAmount(parameters.getLineNetAmount());

    OBDal.getInstance().save(invoiceLine);
    OBDal.getInstance().flush();

    return invoiceLine;
  }

  /**
   * Calls C_Invoice_Post method to process a {@link Invoice} given as a parameter
   */
  public static boolean processInvoice(Invoice invoice) throws Exception {
    return processDocument(invoice, ICIConstants.C_INVOICE_POST_ID);
  }

  /**
   * Calls C_Invoice_Post method to reactivate an already Processed {@link Invoice} given as a
   * parameter
   */
  public static boolean reactivateInvoice(Invoice invoice) {
    try {
      invoice.setDocumentStatus("CO");
      invoice.setDocumentAction("RE");
      invoice.setPosted("N");
      OBDal.getInstance().flush();
      return processDocument(invoice, ICIConstants.C_INVOICE_POST_ID);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * Creates and saves a new {@link ShipmentInOut} with the default parameters for the automated
   * tests
   */
  public static ShipmentInOut createNewShipmentWithDefaultParameters(Order order) {
    ICIShipmentHeaderParameters shipmentHeaderParameters = new ICIShipmentHeaderParameters(order);
    ShipmentInOut shipment = ICIUtility.createNewShipment(shipmentHeaderParameters);
    return shipment;
  }

  /**
   * Creates and saves a new {@link ShipmentInOut} with the default parameters for the automated
   * tests
   */
  private static ShipmentInOut createNewShipment(ICIShipmentHeaderParameters parameters) {
    try {
      ShipmentInOut shipment = OBProvider.getInstance().get(ShipmentInOut.class);
      shipment.setClient(parameters.getOrder().getClient());
      shipment.setOrganization(parameters.getOrder().getOrganization());
      DocumentType mmShipment = OBDal.getInstance()
          .get(DocumentType.class, ICIConstants.SHIPMENT_DOCTYPE_ID);
      shipment.setDocumentType(mmShipment);
      shipment.setDocumentNo(FIN_Utility.getDocumentNo(parameters.getOrder().getOrganization(),
          ICIConstants.GOODS_SHIPMENT_DOCBASETYPE, ICIConstants.M_INOUT_TABLE));
      shipment.setMovementDate(DateUtils.addDays(new Date(), parameters.getDay()));
      shipment.setAccountingDate(DateUtils.addDays(new Date(), parameters.getDay()));
      shipment.setWarehouse(parameters.getLocator().getWarehouse());
      shipment.setBusinessPartner(parameters.getOrder().getBusinessPartner());
      shipment.setPartnerAddress(parameters.getOrder().getPartnerAddress());
      shipment.setSalesOrder(parameters.getOrder());
      shipment.setOrderDate(parameters.getOrder().getOrderDate());
      OBDal.getInstance().save(shipment);
      OBDal.getInstance().flush();

      return shipment;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * Creates and saves a new {@link ShipmentInOutLine} with the default parameters for the automated
   * tests
   */
  public static ShipmentInOutLine createNewShipmentLineWithDefaultParameters(
      ShipmentInOut shipment) {
    ICIShipmentLineParameters shipmentLineParameters = new ICIShipmentLineParameters(shipment);
    ShipmentInOutLine shipmentLine = ICIUtility.createShipmentLine(shipmentLineParameters);
    return shipmentLine;
  }

  /**
   * Creates and saves a new {@link ShipmentInOutLine} with the default parameters for the automated
   * tests
   */
  public static ShipmentInOutLine createShipmentLine(ICIShipmentLineParameters parameters) {
    ShipmentInOutLine shipmentLine = OBProvider.getInstance().get(ShipmentInOutLine.class);
    shipmentLine.setLineNo(10L);
    shipmentLine.setClient(parameters.getShipment().getClient());
    shipmentLine.setOrganization(parameters.getShipment().getOrganization());
    shipmentLine.setProduct(parameters.getProduct());
    shipmentLine.setUOM(parameters.getProduct().getUOM());
    shipmentLine.setMovementQuantity(parameters.getQuantity());
    shipmentLine.setStorageBin(parameters.getLocator());
    shipmentLine.setBusinessPartner(parameters.getShipment().getBusinessPartner());
    shipmentLine.setShipmentReceipt(parameters.getShipment());

    OBDal.getInstance().save(shipmentLine);
    OBDal.getInstance().flush();

    return shipmentLine;
  }

  /**
   * Calls M_Inout_Post method to process a {@link ShipmentInOut} given as a parameter
   */
  public static boolean processShipment(BaseOBObject document) {
    return processDocument(document, ICIConstants.M_INOUT_POST_ID);
  }

  private static boolean processDocument(BaseOBObject document, String processId) {
    OBContext.setAdminMode(true);
    try {
      Process process = null;
      process = OBDal.getInstance().get(Process.class, processId);
      final ProcessInstance pinstance = CallProcess.getInstance()
          .call(process, document.getId().toString(), null);
      return (pinstance.getResult() == 0L);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Reloads the given {@link ShipmentInOut} object and then refreshes it from the Database to
   * retrieve it's actual value into the existing Hibernate session
   */
  public static ShipmentInOut reloadAndRefreshShipment(ShipmentInOut shipmentParameter) {
    // Reload and refresh Shipment object to reflect latest changes in Database
    ShipmentInOut shipment = OBDal.getInstance()
        .get(ShipmentInOut.class, shipmentParameter.getId());
    OBDal.getInstance().refresh(shipment);
    return shipment;
  }

  /**
   * Returns the cause of a trigger exception (BatchupdateException).
   * 
   * Hibernate and JDBC will wrap the exception thrown by the trigger in another exception (the
   * java.sql.BatchUpdateException) and this exception is sometimes wrapped again. Also the
   * java.sql.BatchUpdateException stores the underlying trigger exception in the nextException and
   * not in the cause property.
   * 
   * @param t
   *          exception.
   * @return the underlying trigger message.
   */
  public static String getExceptionMessage(Throwable t) {
    Throwable throwable = DbUtility.getUnderlyingSQLException(t);
    return throwable.getMessage();
  }
}
