package org.openbravo.test.createInvoiceFromOrder;

import java.math.BigDecimal;
import java.util.Date;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.test.TestUtility;
import org.openbravo.advpaymentmngt.test.Value;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;

public class CreateInvoiceFromOrderTestUtils {

  public static final String PAYMENT_TERM = "66BA1164A7394344BB9CD1A6ECEED05D"; // Payment Term: 30 days
  public static final String PRODUCT_PRICE = "4028E6C72959682B01295B03CEE40245"; // Product Price
  public static final String TAX_ID = "5A74E390B82747F9A5754C8EB1BDB47A"; // Tax: VAT 3%
  public static final String DOCTYPE_ID = "D00B3241E3D14D83A48157DEF6BB58FE"; // Document Type: Quotation
  public static final String BPARTNER_ID = "A6750F0D15334FB890C254369AC750A8"; // Business Partner: Alimentos y Supermercados, S.A
  public static final String WAREHOUSE_ID = "B2D40D8A5D644DD89E329DC297309055"; // Warehouse: España Región Norte
  public static final String PAYMENT_METHOD_ID = "1ECC7ADB9EA2442FA4E4DA566AFD806D"; // Payment Method: Cash
  public static final String PRICELIST = "AEE66281A08F42B6BC509B8A80A33C29"; // Price List: Tarifa de ventas

  /**
   * Creates an order with the specified attributes.
   *
   * @param priceList
   *     The price list to apply to the order.
   * @param paymentTerm
   *     The payment term for the order.
   * @param warehouse
   *     The warehouse associated with the order.
   * @param currency
   *     The currency for the order.
   * @param docType
   *     The document type for the order.
   * @param docNo
   *     The document number for the order.
   * @param docStatus
   *     The document status of the order.
   * @param docAction
   *     The document action for the order.
   * @param orderDate
   *     The date of the order.
   * @return The created order.
   */
  static Order createOrder(PriceList priceList, PaymentTerm paymentTerm, Warehouse warehouse, Currency currency,
      DocumentType docType, String docNo, String docStatus, String docAction, Date orderDate) {

    Order order = OBProvider.getInstance().get(Order.class);
    Client client = OBContext.getOBContext().getCurrentClient();
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, BPARTNER_ID);
    FIN_PaymentMethod testPaymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class, PAYMENT_METHOD_ID);
    Location location = TestUtility.getOneInstance(Location.class, new Value(Location.PROPERTY_BUSINESSPARTNER, bp));
    location.setInvoiceToAddress(false);

    // Set order attributes
    order.setClient(client);
    order.setOrganization(org);
    order.setDocumentNo(docNo);
    order.setDocumentStatus(docStatus);
    order.setDocumentAction(docAction);
    order.setDocumentType(docType);
    order.setTransactionDocument(docType);
    order.setOrderDate(orderDate);
    order.setAccountingDate(orderDate);
    order.setBusinessPartner(bp);
    order.setPaymentMethod(testPaymentMethod);
    order.setPartnerAddress(bp.getBusinessPartnerLocationList().get(0));
    order.setCurrency(currency);
    order.setFormOfPayment("5");
    order.setPaymentTerms(paymentTerm);
    order.setInvoiceTerms("I");
    order.setDeliveryTerms("A");
    order.setFreightCostRule("I");
    order.setDeliveryMethod("P");
    order.setPriority("5");
    order.setWarehouse(warehouse);
    order.setPartnerAddress(location);
    order.setPriceList(priceList);

    return order;
  }

  /**
   * Creates an order line for a given order with specified details.
   *
   * @param order
   *     The order to which the line will be added.
   * @param product
   *     The product for the order line.
   * @param lineNo
   *     The line number for the order line.
   * @param orderDate
   *     The date of the order.
   * @param scheduledDeliveryDate
   *     The scheduled delivery date.
   * @param orderedQty
   *     The quantity ordered.
   * @param taxId
   *     The ID of the tax rate to apply.
   * @param unitPrice
   *     The unit price of the product.
   * @param listPrice
   *     The list price of the product.
   * @return The created order line.
   */
  static OrderLine createOrderLine(Order order, Product product, Long lineNo, Date orderDate,
      Date scheduledDeliveryDate, BigDecimal orderedQty, String taxId, BigDecimal unitPrice, BigDecimal listPrice) {
    OrderLine line = OBProvider.getInstance().get(OrderLine.class);
    TaxRate tax = OBDal.getInstance().get(TaxRate.class, taxId);
    line.setClient(order.getClient());
    line.setOrganization(order.getOrganization());
    line.setSalesOrder(order);
    line.setLineNo(lineNo);
    line.setOrderDate(orderDate);
    line.setScheduledDeliveryDate(scheduledDeliveryDate);
    line.setWarehouse(order.getWarehouse());
    line.setUOM(product.getUOM());
    line.setOrderedQuantity(orderedQty);
    line.setCurrency(order.getCurrency());
    line.setTax(tax);
    line.setUnitPrice(unitPrice);
    line.setProduct(product);
    line.setListPrice(listPrice);
    line.setLineNetAmount(orderedQty.multiply(unitPrice));

    return line;
  }

  /**
   * Creates a JSON object containing parameters for an order.
   *
   * @param orderId
   *     The ID of the order to include in the JSON object.
   * @return A JSONObject with the order parameters.
   * @throws JSONException
   *     If an error occurs while creating the JSON object.
   */
  static JSONObject createParameters(String orderId) throws JSONException {
    JSONObject parameters = new JSONObject();
    JSONObject orderGrid = new JSONObject();
    JSONArray selectionArray = new JSONArray();
    JSONObject selectionObject = new JSONObject();

    selectionObject.put("id", orderId);
    selectionArray.put(selectionObject);
    orderGrid.put("_selection", selectionArray);
    parameters.put("orderGrid", orderGrid);
    parameters.put("_buttonValue", "someValue");

    return parameters;
  }

}
