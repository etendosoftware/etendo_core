package org.openbravo.test.stockReservation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openbravo.test.costing.utils.TestCostingConstants.EURO_ID;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.model.ad.domain.Preference;
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
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.ProductPrice;

/**
 * Utility class for creating inventory counts and sales orders for testing
 * automated warehouse reservation functionality.
 */
public class StockReservationTestUtils {

  // Constant identifiers for common test entities
  public static final String WAREHOUSE_RN_ID = "B2D40D8A5D644DD89E329DC297309055"; // Warehouse: España Región Norte
  public static final String WAREHOUSE_RS_ID = "5848641D712545C7AE0FE9634A163648"; // Warehouse: España Región Sur
  public static final String LOCATOR_RN_ID = "54EB861A446D464EAA433477A1D867A6"; // Locator: Rn-0-0-0
  public static final String LOCATOR_RN = "Rn-0-0-0";
  public static final String LOCATOR_RS_ID = "2594CC2B85F645E791C44F741DBE6A54"; // Locator: Rs-0-0-0
  public static final String LOCATOR_RS = "RS-0-0-0";
  public static final String BPARTNER_ID = "9E6850C866BD4921AD0EB7F7796CE2C7"; // Business Partner: Hoteles Buenas Noches, S.A.
  public static final String PAYMENT_METHOD_ID = "A97CFD2AFC234B59BB0A72189BD8FC2A"; // Payment Method: Transferencia
  public static final String PRICELIST = "AEE66281A08F42B6BC509B8A80A33C29"; // Price List: Tarifa de ventas
  public static final String DOCTYPE_ID = "466AF4B0136A4A3F9F84129711DA8BD3"; // Document Type: Standard Order
  public static final String PAYMENT_TERM = "66BA1164A7394344BB9CD1A6ECEED05D"; // Payment Term: 30 days
  public static final String PRODUCT_PRICE = "65AFE199A49747E48AAE418D611FCAFD"; // Product Price: Zumo de Pera 0,5L
  public static final String TAX_ID = "696801EA1AAF46A4AF56E367B40459AE"; // Tax: Entregas IVA 21%
  public static final String LOCATION_ID = "BFE1FB707BA84A6D8AF61A785F3CE1C1"; // Location: Valencia, Av. de las Fuentes, 56
  public static final String DRAFT = "DR";
  public static final String COMPLETED = "CO";
  public static final String REACTIVATE = "RE";
  public static final String AUTOMATIC_RESERVATION = "CRP";
  public static final String MANUAL_RESERVATION = "CR";
  public static final String PREFERENCE_PROPERTY = "StockReservations";
  public static final String ERROR = "An unexpected exception occurred: ";
  public static final BigDecimal _1000 = new BigDecimal(1000);
  public static final BigDecimal STOCK_DEFAULT = new BigDecimal(141640);
  public static final BigDecimal ZERO = new BigDecimal(0);

  /**
   * Private constructor to prevent instantiation of the utility class.
   */
  private StockReservationTestUtils() {

  }

  /**
   * Creates an inventory count with a specified header and corresponding lines.
   *
   * @param name
   *     the name of the inventory count
   * @param bigDecimal
   *     the quantity to be counted for the inventory line
   * @param storageBinId
   *     the storage bin where the inventory count takes place
   * @param warehouseId
   *     the warehouse associated with the inventory count
   * @return the created {@link InventoryCount} object with header and lines set
   */
  public static InventoryCount createInventoryCount(String name, BigDecimal bigDecimal, String storageBinId,
      String warehouseId) {
    InventoryCount inventoryCount = OBProvider.getInstance().get(InventoryCount.class);
    Client client = OBContext.getOBContext().getCurrentClient();
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    Locator storageBin = OBDal.getInstance().get(Locator.class, storageBinId);
    Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);
    ProductPrice productPrice = OBDal.getInstance().get(ProductPrice.class, StockReservationTestUtils.PRODUCT_PRICE);

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

    inventoryCountLine.setClient(inventoryCount.getClient());
    inventoryCountLine.setOrganization(inventoryCount.getOrganization());
    inventoryCountLine.setPhysInventory(inventoryCount);
    inventoryCountLine.setLineNo(10L);
    inventoryCountLine.setProduct(productPrice.getProduct());

    OBCriteria<StorageDetail> storageDetailCriteria = OBDal.getInstance().createCriteria(StorageDetail.class);
    storageDetailCriteria.add(Restrictions.eq(StorageDetail.PROPERTY_PRODUCT, productPrice.getProduct()));
    storageDetailCriteria.add(Restrictions.eq(StorageDetail.PROPERTY_STORAGEBIN, storageBin));
    StorageDetail storageDetail = (StorageDetail) storageDetailCriteria.setMaxResults(1).uniqueResult();

    inventoryCountLine.setStorageBin(storageBin);
    inventoryCountLine.setUOM(productPrice.getProduct().getUOM());
    inventoryCountLine.setBookQuantity(storageDetail.getQuantityOnHand());
    inventoryCountLine.setQuantityCount(bigDecimal);

    OBDal.getInstance().save(inventoryCountLine);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(inventoryCount);

    return inventoryCount;
  }

  /**
   * Creates a sales order with specified attributes.
   *
   * @param docNo
   *     the document number of the order
   * @param docStatus
   *     the document status of the order
   * @param docAction
   *     the document action for the order
   * @param orderedQty
   *     the quantity ordered for the product
   * @param taxId
   *     the tax rate identifier for the order line
   * @param reservation
   *     the reservation flag for the order line
   * @return the created Order object
   */
  public static Order createOrder(String docNo, String docStatus, String docAction, BigDecimal orderedQty, String taxId,
      String reservation) {

    Order order = OBProvider.getInstance().get(Order.class);
    Client client = OBContext.getOBContext().getCurrentClient();
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, BPARTNER_ID);
    FIN_PaymentMethod testPaymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class, PAYMENT_METHOD_ID);
    Location location = OBDal.getInstance().get(Location.class, LOCATION_ID);
    Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, WAREHOUSE_RN_ID);
    PriceList priceList = OBDal.getInstance().get(PriceList.class, PRICELIST);
    PaymentTerm paymentTerm = OBDal.getInstance().get(PaymentTerm.class, PAYMENT_TERM);
    Currency currency = OBDal.getInstance().get(Currency.class, EURO_ID);
    DocumentType docType = OBDal.getInstance().get(DocumentType.class, DOCTYPE_ID);
    ProductPrice productPrice = OBDal.getInstance().get(ProductPrice.class, PRODUCT_PRICE);

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

  /**
   * Creates a Preference object for Stock Reservation.
   *
   * @return the created Preference object
   */
  public static Preference stockReservationPreference() {

    Preference preference = OBProvider.getInstance().get(Preference.class);
    Client client = OBContext.getOBContext().getCurrentClient();
    Organization org = OBContext.getOBContext().getCurrentOrganization();

    // Set preference attributes
    preference.setClient(client);
    preference.setOrganization(org);
    preference.setActive(true);
    preference.setPropertyList(true);
    preference.setSelected(true);
    preference.setProperty(PREFERENCE_PROPERTY);
    preference.setSearchKey("Y");

    OBDal.getInstance().save(preference);
    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();

    return preference;
  }

  /**
   * Verifies that the reservation contains a stock entry with the specified storage bin and quantity.
   *
   * @param reservation
   *     the Reservation object to check for matching stock
   * @param binSearchKey
   *     the search key of the storage bin to be verified
   * @param expectedQuantity
   *     the expected quantity of the stock in the reservation
   * @throws AssertionError
   *     if no matching stock entry with the given storage bin and expected quantity is found
   */
  public static void verifyAutomaticReservationDetails(Reservation reservation, String binSearchKey,
      BigDecimal expectedQuantity) {
    List<ReservationStock> reservationStocks = findReservationStocksForReservation(reservation);

    boolean foundMatchingBin = reservationStocks.stream().anyMatch(
        stock -> binSearchKey.equals(stock.getStorageBin().getSearchKey()) && stock.getQuantity().compareTo(
            expectedQuantity) == 0);

    assertTrue(String.format("A StorageBin with searchKey '%s' and quantity '%s' was not found.", binSearchKey,
        expectedQuantity), foundMatchingBin);
  }

  /**
   * Verifies that the reservation does not contain a stock entry with the specified storage bin and quantity.
   *
   * @param reservation
   *     the Reservation object to check for matching stock
   * @param binSearchKey
   *     the search key of the storage bin to be verified
   * @param expectedQuantity
   *     the expected quantity of the stock in the reservation
   * @throws AssertionError
   *     if a matching stock entry with the given storage bin and expected quantity is found
   */
  public static void verifyManualReservationDetails(Reservation reservation, String binSearchKey,
      BigDecimal expectedQuantity) {
    List<ReservationStock> reservationStocks = findReservationStocksForReservation(reservation);

    boolean foundMatchingBin = reservationStocks.stream().anyMatch(
        stock -> binSearchKey.equals(stock.getStorageBin().getSearchKey()) && stock.getQuantity().compareTo(
            expectedQuantity) == 0);

    assertFalse(
        String.format("A StorageBin with searchKey '%s' and quantity '%s' was found.", binSearchKey, expectedQuantity),
        foundMatchingBin);
  }

  /**
   * Finds the reservation associated with a specific sales order.
   *
   * @param salesOrder
   *     the sales order for which the reservation is to be found
   * @return the reservation associated with the sales order, or {@code null} if none exists
   */
  public static Reservation findReservationForOrder(Order salesOrder) {
    OBDal.getInstance().refresh(salesOrder);
    OBCriteria<Reservation> reservationCriteria = OBDal.getInstance().createCriteria(Reservation.class);
    reservationCriteria.add(Restrictions.eq(Reservation.PROPERTY_SALESORDERLINE + ".id", salesOrder.getOrderLineList().get(0).getId()));
    return (Reservation) reservationCriteria.setMaxResults(1).uniqueResult();
  }

  /**
   * Retrieves the list of reservation stock entries for a given reservation.
   *
   * @param reservation
   *     the reservation for which stock entries are to be retrieved
   * @return a list of {@link ReservationStock} entries associated with the reservation
   */
  public static List<ReservationStock> findReservationStocksForReservation(Reservation reservation) {
    OBCriteria<ReservationStock> reservationStockCriteria = OBDal.getInstance().createCriteria(ReservationStock.class);
    reservationStockCriteria.add(Restrictions.eq(ReservationStock.PROPERTY_RESERVATION + ".id", reservation.getId()));
    return reservationStockCriteria.list();
  }
}
