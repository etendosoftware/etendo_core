package org.openbravo.test.purchaseOrder;

import static org.openbravo.test.costing.utils.TestCostingConstants.EURO_ID;

import java.math.BigDecimal;
import java.util.Date;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.test.stockReservation.StockReservationTestUtils;

/**
 * Utility class for managing purchase orders, goods receipts, and invoices.
 * Contains constants for common entities and methods to create and process documents.
 * This class cannot be instantiated.
 */
public class PurchaseOrderUtils {

  // Constant identifiers for common test entities
  public static final String BPARTNER_ID = "858B90C7AF0A4533863EEC65437382BF"; // Business Partner: Bebidas Alegres, S.L.
  public static final String WAREHOUSE_RN_ID = "B2D40D8A5D644DD89E329DC297309055"; // Warehouse: España Región Norte
  public static final String PAYMENT_METHOD_ID = "A97CFD2AFC234B59BB0A72189BD8FC2A"; // Payment Method: Transferencia
  public static final String DOCTYPE_PO_ID = "AB22CE8FFA5E4AF29F2AC90FCDD400D8"; // Transaction Document: Purchase Order
  public static final String LOCATION_ID = "1F38687D74244B369F0B16B3858CD256"; // Partner Address: .Barcelona, C\Mayor, 23
  public static final String PAYMENT_TERM = "66BA1164A7394344BB9CD1A6ECEED05D"; // Payment Term: 30 days
  public static final String PRICELIST = "91AE1E96A30844209CD996917E193BE1"; // Price List: Tarifa Bebidas Alegres
  public static final String PRODUCT_PRICE = "C01BB3FED5084DBBB2BB51B42C80E135"; // Product Price: Cerveza Ale 0,5L
  public static final String TAX_ID = "18A499249FF74A68B79510F0AD341141"; // Tax: Adquisiciones IVA 21%
  private static final String DOCTYPE_GR_ID = "BBC17DD5FD25470BB2752D096B72D412"; // Document Type: MM Receipt
  private static final String DOCTYPE_PI_ID = "F2EB2EAD2612449A83C28DA84689D78B"; // Transaction Document: AP Invoice
  public static final String LOCATOR_RN_ID = "54EB861A446D464EAA433477A1D867A6"; // Locator: Rn-0-0-0
  public static final String DRAFT = "DR";
  public static final String COMPLETED = "CO";
  public static final String REACTIVATE = "RE";

  /**
   * Private constructor to prevent instantiation of the utility class.
   */
  private PurchaseOrderUtils() {
  }

  /**
   * Creates a new purchase order with one line, and saves it.
   *
   * @return the created order
   */
  public static Order createPurchaseOrder() {
    Order order = OBProvider.getInstance().get(Order.class);
    Client client = OBContext.getOBContext().getCurrentClient();
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, BPARTNER_ID);
    FIN_PaymentMethod paymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class, PAYMENT_METHOD_ID);
    Location location = OBDal.getInstance().get(Location.class, LOCATION_ID);
    Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, WAREHOUSE_RN_ID);
    PriceList priceList = OBDal.getInstance().get(PriceList.class, PRICELIST);
    PaymentTerm paymentTerm = OBDal.getInstance().get(PaymentTerm.class, PAYMENT_TERM);
    Currency currency = OBDal.getInstance().get(Currency.class, EURO_ID);
    DocumentType docType = OBDal.getInstance().get(DocumentType.class, DOCTYPE_PO_ID);
    ProductPrice productPrice = OBDal.getInstance().get(ProductPrice.class, PRODUCT_PRICE);
    TaxRate tax = OBDal.getInstance().get(TaxRate.class, TAX_ID);

    // Header
    order.setClient(client);
    order.setOrganization(org);
    order.setTransactionDocument(docType);
    order.setDocumentNo("PO-" + System.currentTimeMillis());
    order.setDocumentStatus(DRAFT);
    order.setSalesTransaction(false);
    order.setAccountingDate(new Date());
    order.setOrderDate(new Date());
    order.setScheduledDeliveryDate(new Date());
    order.setBusinessPartner(bp);
    order.setPartnerAddress(location);
    order.setPriceList(priceList);
    order.setPaymentMethod(paymentMethod);
    order.setPaymentTerms(paymentTerm);
    order.setWarehouse(warehouse);
    order.setDocumentAction(COMPLETED);
    order.setDocumentType(docType);
    order.setCurrency(currency);
    order.setInvoiceAddress(location);

    OBDal.getInstance().save(order);
    OBDal.getInstance().flush();

    // Lines
    OrderLine line = OBProvider.getInstance().get(OrderLine.class);
    Product product = productPrice.getProduct();
    BigDecimal unitPrice = productPrice.getStandardPrice();
    line.setClient(order.getClient());
    line.setOrganization(order.getOrganization());
    line.setSalesOrder(order);
    line.setOrderDate(new Date());
    line.setWarehouse(order.getWarehouse());
    line.setLineNo(10L);
    line.setProduct(product);
    line.setOrderedQuantity(BigDecimal.valueOf(10));
    line.setUOM(product.getUOM());
    line.setUnitPrice(unitPrice);
    line.setLineNetAmount(BigDecimal.valueOf(10).multiply(unitPrice));
    line.setTax(tax);
    line.setCurrency(currency);

    OBDal.getInstance().save(line);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(order);

    return order;
  }

  /**
   * Creates a Goods Receipt for a given Purchase Order.
   *
   * @param purchaseOrder the Purchase Order to generate the Goods Receipt for
   * @return the created Goods Receipt
   */
  public static ShipmentInOut createGoodsReceipt(Order purchaseOrder) {
    OBDal.getInstance().refresh(purchaseOrder);

    Client client = OBContext.getOBContext().getCurrentClient();
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    DocumentType docType = OBDal.getInstance().get(DocumentType.class, DOCTYPE_GR_ID);
    Locator storageBin = OBDal.getInstance().get(Locator.class, LOCATOR_RN_ID);
    ProductPrice productPrice = OBDal.getInstance().get(ProductPrice.class, StockReservationTestUtils.PRODUCT_PRICE);

    // Header
    ShipmentInOut receipt = OBProvider.getInstance().get(ShipmentInOut.class);
    receipt.setClient(client);
    receipt.setOrganization(org);
    receipt.setBusinessPartner(purchaseOrder.getBusinessPartner());
    receipt.setPartnerAddress(purchaseOrder.getPartnerAddress());
    receipt.setDocumentType(docType);
    receipt.setDocumentNo("GR-" + System.currentTimeMillis());
    receipt.setDocumentStatus(DRAFT);
    receipt.setMovementDate(new Date());
    receipt.setAccountingDate(new Date());
    receipt.setSalesTransaction(false);
    receipt.setProcessed(false);
    receipt.setWarehouse(purchaseOrder.getWarehouse());
    receipt.setDocumentAction(COMPLETED);

    OBDal.getInstance().save(receipt);
    OBDal.getInstance().flush();

    OBDal.getInstance().refresh(purchaseOrder);

    // Lines from Purchase Order
    for (OrderLine orderLine : purchaseOrder.getOrderLineList()) {
      ShipmentInOutLine receiptLine = OBProvider.getInstance().get(ShipmentInOutLine.class);

      receiptLine.setClient(receipt.getClient());
      receiptLine.setOrganization(receipt.getOrganization());
      receiptLine.setShipmentReceipt(receipt);
      receiptLine.setLineNo(orderLine.getLineNo());
      receiptLine.setProduct(orderLine.getProduct());
      receiptLine.setUOM(orderLine.getUOM());
      receiptLine.setMovementQuantity(orderLine.getOrderedQuantity());

      OBCriteria<StorageDetail> storageDetailCriteria = OBDal.getInstance().createCriteria(StorageDetail.class);
      storageDetailCriteria.add(Restrictions.eq(StorageDetail.PROPERTY_PRODUCT, productPrice.getProduct()));
      storageDetailCriteria.add(Restrictions.eq(StorageDetail.PROPERTY_STORAGEBIN, storageBin));

      receiptLine.setStorageBin(storageBin);
      receiptLine.setSalesOrderLine(orderLine);

      OBDal.getInstance().save(receiptLine);
    }

    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(receipt);

    return receipt;
  }

  /**
   * Creates a new purchase invoice with one line, and saves it.
   *
   * @param goodsReceipt
   *          the goods receipt to create the invoice from
   * @return the created invoice
   */
  public static Invoice createPurchaseInvoice(ShipmentInOut goodsReceipt) {
    OBDal.getInstance().refresh(goodsReceipt);

    Invoice invoice = OBProvider.getInstance().get(Invoice.class);
    Client client = OBContext.getOBContext().getCurrentClient();
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, BPARTNER_ID);
    FIN_PaymentMethod paymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class, PAYMENT_METHOD_ID);
    Location location = OBDal.getInstance().get(Location.class, LOCATION_ID);
    PriceList priceList = OBDal.getInstance().get(PriceList.class, PRICELIST);
    PaymentTerm paymentTerm = OBDal.getInstance().get(PaymentTerm.class, PAYMENT_TERM);
    Currency currency = OBDal.getInstance().get(Currency.class, EURO_ID);
    DocumentType docType = OBDal.getInstance().get(DocumentType.class, DOCTYPE_PI_ID);
    TaxRate tax = OBDal.getInstance().get(TaxRate.class, TAX_ID);

    // Header
    invoice.setClient(client);
    invoice.setOrganization(org);
    invoice.setTransactionDocument(docType);
    invoice.setDocumentNo("PI-" + System.currentTimeMillis());
    invoice.setDocumentStatus(DRAFT);
    invoice.setSalesTransaction(false);
    invoice.setAccountingDate(new Date());
    invoice.setInvoiceDate(new Date());
    invoice.setBusinessPartner(bp);
    invoice.setPartnerAddress(location);
    invoice.setPriceList(priceList);
    invoice.setPaymentMethod(paymentMethod);
    invoice.setPaymentTerms(paymentTerm);
    invoice.setDocumentAction(COMPLETED);
    invoice.setDocumentType(docType);
    invoice.setCurrency(currency);

    OBDal.getInstance().save(invoice);
    OBDal.getInstance().flush();

    // Lines from Goods Receipt
    for (ShipmentInOutLine receiptLine : goodsReceipt.getMaterialMgmtShipmentInOutLineList()) {
      InvoiceLine invoiceLine = OBProvider.getInstance().get(InvoiceLine.class);

      invoiceLine.setClient(invoice.getClient());
      invoiceLine.setOrganization(invoice.getOrganization());
      invoiceLine.setInvoice(invoice);
      invoiceLine.setLineNo(10L);
      invoiceLine.setProduct(receiptLine.getProduct());
      invoiceLine.setUOM(receiptLine.getUOM());
      invoiceLine.setInvoicedQuantity(receiptLine.getMovementQuantity());
      invoiceLine.setUnitPrice(BigDecimal.valueOf(1.36));
      invoiceLine.setTax(tax);
      invoiceLine.setLineNetAmount(invoiceLine.getInvoicedQuantity().multiply(invoiceLine.getUnitPrice()));
      invoiceLine.setSalesOrderLine(receiptLine.getSalesOrderLine());
      invoiceLine.setGoodsShipmentLine(receiptLine);

      OBDal.getInstance().save(invoiceLine);
    }

    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(invoice);

    return invoice;
  }
}
