package org.openbravo.test.createInvoiceFromOrder;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.openbravo.test.createInvoiceFromOrder.CreateInvoiceFromOrderTestUtils.createOrder;
import static org.openbravo.test.createInvoiceFromOrder.CreateInvoiceFromOrderTestUtils.createOrderLine;
import static org.openbravo.test.createInvoiceFromOrder.CreateInvoiceFromOrderTestUtils.createParameters;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang.mutable.MutableBoolean;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_process.ConvertQuotationIntoOrder;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
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
  public void testWarningMessageOnOrderCreationFromQuotation() {
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
    PriceList priceList = OBDal.getInstance().get(PriceList.class, CreateInvoiceFromOrderTestUtils.PRICELIST);
    PaymentTerm paymtTerm = OBDal.getInstance().get(PaymentTerm.class, CreateInvoiceFromOrderTestUtils.PAYMENT_TERM);
    Warehouse ware = OBDal.getInstance().get(Warehouse.class, CreateInvoiceFromOrderTestUtils.WAREHOUSE_ID);
    Currency eur = OBDal.getInstance().get(Currency.class, EURO_ID);
    DocumentType docType = OBDal.getInstance().get(DocumentType.class, CreateInvoiceFromOrderTestUtils.DOCTYPE_ID);

    return createOrder(priceList, paymtTerm, ware, eur, docType, "Test Document - 0001", "DR", "CO", new Date());
  }

  /**
   * Creates a test order line associated with the given order.
   *
   * @param quotation
   *     The order to which the order line will be associated.
   * @return The created order line.
   */
  private OrderLine createTestOrderLine(Order quotation) {
    ProductPrice productPrice = OBDal.getInstance().get(ProductPrice.class,
        CreateInvoiceFromOrderTestUtils.PRODUCT_PRICE);
    return createOrderLine(quotation, productPrice.getProduct(), Long.valueOf("10"), new Date(), new Date(),
        new BigDecimal(10), CreateInvoiceFromOrderTestUtils.TAX_ID, productPrice.getStandardPrice(),
        productPrice.getListPrice());
  }

}
