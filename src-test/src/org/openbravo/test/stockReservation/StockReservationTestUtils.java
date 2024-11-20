/**
 * Utility class for creating inventory counts and sales orders for testing
 * automated warehouse reservation functionality.
 */
package org.openbravo.test.stockReservation;

import java.math.BigDecimal;
import java.util.Date;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.ProductPrice;

public class StockReservationTestUtils {

  // Constant identifiers for common test entities
  public static final String WAREHOUSE_RN_ID = "B2D40D8A5D644DD89E329DC297309055"; // Warehouse: España Región Norte
  public static final String WAREHOUSE_RS_ID = "5848641D712545C7AE0FE9634A163648"; // Warehouse: España Región Sur
  public static final String LOCATOR_RN_ID = "54EB861A446D464EAA433477A1D867A6"; // Locator: Rn-0-0-0
  public static final String LOCATOR_RS_ID = "2594CC2B85F645E791C44F741DBE6A54"; // Locator: Rs-0-0-0
  public static final String BPARTNER_ID = "A6750F0D15334FB890C254369AC750A8"; // Business Partner: Alimentos y Supermercados, S.A
  public static final String PAYMENT_METHOD_ID = "A97CFD2AFC234B59BB0A72189BD8FC2A"; // Payment Method: Transferencia
  public static final String PRICELIST = "AEE66281A08F42B6BC509B8A80A33C29"; // Price List: Tarifa de ventas
  public static final String DOCTYPE_ID = "466AF4B0136A4A3F9F84129711DA8BD3"; // Document Type: Standard Order
  public static final String PAYMENT_TERM = "66BA1164A7394344BB9CD1A6ECEED05D"; // Payment Term: 30 days
  public static final String PRODUCT_PRICE = "65AFE199A49747E48AAE418D611FCAFD"; // Product Price: Zumo de Pera 0,5L
  public static final String TAX_ID = "696801EA1AAF46A4AF56E367B40459AE"; // Tax: Entregas IVA 21%
  public static final String LOCATION_ID = "6518D3040ED54008A1FC0C09ED140D66"; // Location: La Coruña, C\Pescadores, 87

  /**
   * Private constructor to prevent instantiation of the utility class.
   */
  private StockReservationTestUtils() {

  }

  /**
   * Creates an inventory count with specified attributes.
   *
   * @param name        the name of the inventory count
   * @param productPrice the product price object for the inventory
   * @param storageBin  the locator where the inventory is stored
   * @param warehouse   the warehouse associated with the inventory count
   * @return the created InventoryCount object
   */
  static InventoryCount createInventoryCount(String name, ProductPrice productPrice, Locator storageBin, Warehouse warehouse) {
    InventoryCount inventoryCount = OBProvider.getInstance().get(InventoryCount.class);
    Client client = OBContext.getOBContext().getCurrentClient();
    Organization org = OBContext.getOBContext().getCurrentOrganization();

    // Set header attributes
    inventoryCount.setClient(client);
    inventoryCount.setOrganization(org);
    inventoryCount.setMovementDate(new Date());
    inventoryCount.setName(name);
    inventoryCount.setWarehouse(warehouse);
    inventoryCount.setInventoryType("N");

    OBDal.getInstance().save(inventoryCount);
    OBDal.getInstance().flush();

    // Set lines attributes
    InventoryCountLine inventoryCountLine = OBProvider.getInstance().get(InventoryCountLine.class);
    Product product = productPrice.getProduct();

    inventoryCountLine.setClient(inventoryCount.getClient());
    inventoryCountLine.setOrganization(inventoryCount.getOrganization());
    inventoryCountLine.setPhysInventory(inventoryCount);
    inventoryCountLine.setLineNo(10L);
    inventoryCountLine.setProduct(product);

    OBCriteria<StorageDetail> storageDetailCriteria = OBDal.getInstance().createCriteria(StorageDetail.class);
    storageDetailCriteria.add(Restrictions.eq(StorageDetail.PROPERTY_PRODUCT, product));
    storageDetailCriteria.add(Restrictions.eq(StorageDetail.PROPERTY_STORAGEBIN, storageBin));
    StorageDetail storageDetail = (StorageDetail) storageDetailCriteria.setMaxResults(1).uniqueResult();

    inventoryCountLine.setStorageBin(storageBin);
    inventoryCountLine.setUOM(product.getUOM());
    inventoryCountLine.setBookQuantity(storageDetail.getQuantityOnHand());
    inventoryCountLine.setQuantityCount(new BigDecimal(1000));

    OBDal.getInstance().save(inventoryCountLine);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(inventoryCount);

    return inventoryCount;
  }

  /**
   * Creates a sales order with specified attributes.
   *
   * @param priceList   the price list for the order
   * @param paymentTerm the payment term for the order
   * @param docType     the document type of the order
   * @param docNo       the document number of the order
   * @param docStatus   the document status of the order
   * @param docAction   the document action for the order
   * @param currency    the currency of the order
   * @param productPrice the product price associated with the order line
   * @param orderedQty  the quantity ordered for the product
   * @param taxId       the tax rate identifier for the order line
   * @param reservation the reservation flag for the order line
   * @return the created Order object
   */
  static Order createOrder(PriceList priceList, PaymentTerm paymentTerm, DocumentType docType, String docNo,
      String docStatus, String docAction, Currency currency, ProductPrice productPrice, BigDecimal orderedQty,
      String taxId, String reservation) {

    Order order = OBProvider.getInstance().get(Order.class);
    Client client = OBContext.getOBContext().getCurrentClient();
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, BPARTNER_ID);
    FIN_PaymentMethod testPaymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class, PAYMENT_METHOD_ID);
    Location location = OBDal.getInstance().get(Location.class, LOCATION_ID);
    Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, StockReservationTestUtils.WAREHOUSE_RN_ID);

    // Set order attributes
    order.setClient(client);
    order.setOrganization(org);
    order.setTransactionDocument(docType);
    order.setDocumentNo(docNo);
    order.setDocumentStatus(docStatus);
    order.setAccountingDate(new Date());
    order.setOrderDate(new Date());
    order.setBusinessPartner(bp);
    order.setPartnerAddress(location);
    order.setPartnerAddress(bp.getBusinessPartnerLocationList().get(0));
    order.setPriceList(priceList);
    order.setScheduledDeliveryDate(new Date());
    order.setPaymentMethod(testPaymentMethod);
    order.setPaymentTerms(paymentTerm);
    order.setWarehouse(warehouse);
    order.setInvoiceTerms("D");
    order.setDocumentAction(docAction);
    order.setDocumentType(docType);
    order.setCurrency(currency);
    order.setInvoiceAddress(location);

    OBDal.getInstance().save(order);
    OBDal.getInstance().flush();

    // Set order line attributes
    OrderLine line = OBProvider.getInstance().get(OrderLine.class);
    TaxRate tax = OBDal.getInstance().get(TaxRate.class, taxId);
    Product product = productPrice.getProduct();
    BigDecimal unitPrice = productPrice.getStandardPrice();

    line.setClient(order.getClient());
    line.setOrganization(order.getOrganization());
    line.setSalesOrder(order);
    line.setOrderDate(new Date());
    line.setWarehouse(order.getWarehouse());
    line.setLineNo(10L);
    line.setProduct(product);
    line.setOrderedQuantity(orderedQty);
    line.setUOM(product.getUOM());
    line.setUnitPrice(unitPrice);
    line.setLineNetAmount(orderedQty.multiply(unitPrice));
    line.setTax(tax);
    line.setCreateReservation(reservation);
    line.setCurrency(currency);

    OBDal.getInstance().save(line);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(order);

    return order;
  }
}
