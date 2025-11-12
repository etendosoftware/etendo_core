package org.openbravo.test.createInvoiceFromOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.openbravo.test.createInvoiceFromOrder.CreateOrderFromQuotationTestUtils.createOrder;
import static org.openbravo.test.createInvoiceFromOrder.CreateOrderFromQuotationTestUtils.createOrderLine;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

public class CreateOrderFromQuotationTest extends WeldBaseTest {

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN, TestConstants.Clients.FB_GRP,
        TestConstants.Orgs.ESP_NORTE);
    OBContext currentContext = OBContext.getOBContext();
    VariablesSecureApp vsa = new VariablesSecureApp(currentContext.getUser().getId(),
        currentContext.getCurrentClient().getId(), currentContext.getCurrentOrganization().getId(),
        currentContext.getRole().getId());
    RequestContext.get().setVariableSecureApp(vsa);
  }

  @Test
  @Issue("#277")
  public void testErrorMessageOnOrderCreationFromQuotation() {
    Order quotation = createAndProcessQuotation();
    try {
      OBError response = executeConvertQuotationIntoOrderProcess(quotation);
      assertEquals("error", response.getType());
      assertEquals(response.getMessage(), OBMessageUtils.messageBD("BusinessPartnerNoInvoicingAddress"));
    } catch (Exception e) {
      fail("Expected OBException to be thrown");
    }
  }

  @AfterEach
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
    PriceList priceList = OBDal.getInstance().get(PriceList.class, CreateOrderFromQuotationTestUtils.PRICELIST);
    PaymentTerm paymtTerm = OBDal.getInstance().get(PaymentTerm.class, CreateOrderFromQuotationTestUtils.PAYMENT_TERM);
    Warehouse ware = OBDal.getInstance().get(Warehouse.class, CreateOrderFromQuotationTestUtils.WAREHOUSE_ID);
    Currency eur = OBDal.getInstance().get(Currency.class, EURO_ID);
    DocumentType docType = OBDal.getInstance().get(DocumentType.class, CreateOrderFromQuotationTestUtils.DOCTYPE_ID);

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
        CreateOrderFromQuotationTestUtils.PRODUCT_PRICE);
    return createOrderLine(quotation, productPrice, Long.valueOf("10"), new Date(), new Date(), new BigDecimal(10),
        CreateOrderFromQuotationTestUtils.TAX_ID);
  }

}
