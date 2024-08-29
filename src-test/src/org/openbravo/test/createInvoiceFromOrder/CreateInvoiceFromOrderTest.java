package org.openbravo.test.createInvoiceFromOrder;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.advpaymentmngt.test.TestUtility;
import org.openbravo.advpaymentmngt.test.Value;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_process.ConvertQuotationIntoOrder;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
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
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.TestConstants;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;
import com.smf.jobs.defaults.invoices.CreateFromOrder;

public class CreateInvoiceFromOrderTest extends WeldBaseTest {

  public static final String PAYMENT_TERM = "66BA1164A7394344BB9CD1A6ECEED05D"; // 30 days
  public static final String PRODUCT_PRICE = "4028E6C72959682B01295B03CEE40245";
  public static final String TAX_ID = "5A74E390B82747F9A5754C8EB1BDB47A"; // VAT 3%
  private static final String DOCTYPE_ID = "D00B3241E3D14D83A48157DEF6BB58FE"; // Quotation
  private static final String BPARTNER_ID = "A6750F0D15334FB890C254369AC750A8"; // Alimentos y Supermercados, S.A
  private static final String WAREHOUSE_ID = "B2D40D8A5D644DD89E329DC297309055"; // España Región Norte
  private static final String PAYMENT_METHOD_ID = "1ECC7ADB9EA2442FA4E4DA566AFD806D"; // Cash
  private static final String PRICELIST = "AEE66281A08F42B6BC509B8A80A33C29"; // Tarifa de ventas

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN, TestConstants.Clients.FB_GRP,
        TestConstants.Orgs.ESP_NORTE);
    VariablesSecureApp vsa = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(), OBContext.getOBContext().getCurrentOrganization().getId(),
        OBContext.getOBContext().getRole().getId());
    RequestContext.get().setVariableSecureApp(vsa);
  }

  @Test
  @Issue("#277")
  public void testWarningMessageOnOrderCreationFromQuotation() throws Exception {
    Order quotation = createAndProcessQuotation();
    try {
      OBError response = executeConvertQuotationIntoOrderProcess(quotation);
      assertEquals("warning", response.getType());
      assertThat(response.getMessage(), containsString("@OrderCreatedNoInvoiceAddress@"));
    } catch (Exception e) {
      fail("Expected OBException to be thrown");
    }
  }

  @Test
  @Issue("#277")
  public void testErrorOnInvoiceCreationFromOrder() {
    Order quotation = createAndProcessQuotation();

    try {
      executeConvertQuotationIntoOrderProcess(quotation);
      ActionResult result = executeCreateFromOrderProcess(quotation);
      assertEquals(Result.Type.ERROR, result.getType());
      assertEquals(result.getMessage(), OBMessageUtils.messageBD("NoInvoicingAddress"));
    } catch (Exception e) {
      fail("Expected OBException to be thrown");
    }
  }

  @After
  public void cleanUp() {
    OBDal.getInstance().rollbackAndClose();
  }

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
  private static Order createOrder(PriceList priceList, PaymentTerm paymentTerm, Warehouse warehouse, Currency currency,
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
  private static OrderLine createOrderLine(Order order, Product product, Long lineNo, Date orderDate,
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
   * Creates and processes a test quotation with an order line.
   *
   * @return The processed quotation.
   */
  private Order createAndProcessQuotation() {
    Order quotation = createTestQuotation();
    OrderLine line = createTestOrderLine(quotation);
    OBDal.getInstance().save(quotation);
    OBDal.getInstance().flush();

    quotation.setDocumentStatus("UE");
    quotation.setDocumentAction("--");
    OBDal.getInstance().save(quotation);
    OBDal.getInstance().save(line);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(quotation);
    return quotation;
  }

  /**
   * Executes the process to create an action from the given order.
   *
   * @param order
   *     The order to use for creating the action.
   * @return An ActionResult object containing the result of the process.
   * @throws Exception
   *     If an error occurs while creating or invoking the action.
   */
  private ActionResult executeCreateFromOrderProcess(Order order) throws Exception {
    Class<CreateFromOrder> c = CreateFromOrder.class;
    Object object = c.getDeclaredConstructor().newInstance();
    Method method = c.getDeclaredMethod("action", JSONObject.class, MutableBoolean.class);
    method.setAccessible(true);
    JSONObject parameters = createParameters(order.getId());
    return (ActionResult) method.invoke(object, parameters, null);
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
  private JSONObject createParameters(String orderId) throws JSONException {
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

  /**
   * Executes the process to convert a quotation into an order.
   *
   * @param quotation
   *     The quotation to be converted into an order.
   * @return An OBError object containing the result of the conversion process.
   * @throws Exception
   *     If an error occurs during the process execution.
   */
  private OBError executeConvertQuotationIntoOrderProcess(Order quotation) throws Exception {
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    ConnectionProvider conn = new DalConnectionProvider();
    ProcessBundle pb = new ProcessBundle("", vars).init(conn);
    HashMap<String, Object> parameters = new HashMap<>();
    parameters.put("C_Order_ID", quotation.getId());
    pb.setParams(parameters);
    new ConvertQuotationIntoOrder().doExecute(pb);
    return (OBError) pb.getResult();
  }

  /**
   * Creates a test quotation with predefined values for price list, payment term, warehouse, currency, and document type.
   *
   * @return The created quotation.
   */
  private Order createTestQuotation() {
    PriceList priceList = OBDal.getInstance().get(PriceList.class, PRICELIST);
    PaymentTerm paymtTerm = OBDal.getInstance().get(PaymentTerm.class, PAYMENT_TERM);
    Warehouse ware = OBDal.getInstance().get(Warehouse.class, WAREHOUSE_ID);
    Currency eur = OBDal.getInstance().get(Currency.class, EURO_ID);
    DocumentType docType = OBDal.getInstance().get(DocumentType.class, DOCTYPE_ID);

    return createOrder(priceList, paymtTerm, ware, eur, docType, "Test Document - 0001", "DR", "CO", new Date());
  }

  /**
   * Creates a test order line associated with the given order.
   *
   * @param quot
   *     The order to which the order line will be associated.
   * @return The created order line.
   */
  private OrderLine createTestOrderLine(Order quot) {
    ProductPrice productPrice = OBDal.getInstance().get(ProductPrice.class, PRODUCT_PRICE);
    return createOrderLine(quot, productPrice.getProduct(), Long.valueOf("10"), new Date(), new Date(),
        new BigDecimal(10), TAX_ID, productPrice.getStandardPrice(), productPrice.getListPrice());
  }

}
