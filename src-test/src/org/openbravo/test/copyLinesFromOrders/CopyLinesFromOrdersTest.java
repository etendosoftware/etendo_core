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
 * All portions are Copyright (C) 2017-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.copyLinesFromOrders;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.common.actionhandler.copyfromorderprocess.CopyFromOrdersProcess;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestConstants;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataPO_13;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataPO_14;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataPO_15;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataPO_16;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataPO_17;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataPO_18;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataPO_19;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataPO_20;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataPerformance;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataSO_01;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataSO_02;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataSO_03;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataSO_04;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataSO_05;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataSO_06;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataSO_07;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataSO_08;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataSO_AUM_21;
import org.openbravo.test.copyLinesFromOrders.data.CLFOTestDataSO_AUM_22;
import org.openbravo.test.copyLinesFromOrders.data.CopyLinesFromOrdersTestData;
import org.openbravo.test.copyLinesFromOrders.data.JSONUtils;
import org.openbravo.test.copyLinesFromOrders.data.OrderData;
import org.openbravo.test.copyLinesFromOrders.data.OrderLineData;
import org.openbravo.test.copyLinesFromOrders.data.UOMManagementUtil;

/**
 * Tests cases to check Copy Lines From Orders process
 * 
 * @author Mark
 * 
 */
public class CopyLinesFromOrdersTest extends WeldBaseTest {
  final static private Logger log = LogManager.getLogger();

  // User Openbravo
  private final String USER_ID = "100";
  // Client QA Testing
  private final String CLIENT_ID = "4028E6C72959682B01295A070852010D";
  // Organization Spain
  private final String ORGANIZATION_ID = "357947E87C284935AD1D783CF6F099A1";
  // Role QA Testing Admin
  private final String ROLE_ID = "4028E6C72959682B01295A071429011E";

  // Client F&B
  private final String FB_CLIENT_ID = "23C59575B9CF467C9620760EB255B389";
  // Organization Spain
  private final String FB_ORGANIZATION_ID = "B843C30461EA4501935CB1D125C9C25A";
  // Role QA Testing Admin
  private final String FB_ROLE_ID = "42D0EEB1C66F497A90DD526DC597E6F0";

  // Test information
  // private CopyLinesFromOrdersTestData data;
  private String testNumber;
  private String testDescription;
  private boolean executeAsQAAdmin;

  // Orders to be processed
  private OrderData processedOrder;

  // Orders to be copied
  private OrderData[] ordersCopiedFrom;
  // Order Lines for each order to be copied
  private OrderLineData[][] orderLinesCopiedFrom;

  // Expected order amounts
  private String[] expectedOrderAmounts;
  // Expected order lines data
  private HashMap<String, String[]> expectedOrderLinesData;

  public CopyLinesFromOrdersTest() {

  }

  private void setData() {
    this.testNumber = data.getTestNumber();
    this.testDescription = data.getTestDescription();
    this.executeAsQAAdmin = data.isExecuteAsQAAdmin();
    this.ordersCopiedFrom = data.getOrdersCopiedFrom();
    this.orderLinesCopiedFrom = data.getOrderLinesCopiedFrom();
    this.processedOrder = data.getOrder();
    this.expectedOrderAmounts = data.getExpectedOrderAmounts();
    this.expectedOrderLinesData = data.getExpectedOrderLines();
  }

  public static final List<CopyLinesFromOrdersTestData> PARAMS = Arrays.asList(
      new CLFOTestDataSO_01(), new CLFOTestDataSO_02(), new CLFOTestDataSO_03(),
      new CLFOTestDataSO_04(), new CLFOTestDataSO_05(), new CLFOTestDataSO_06(),
      new CLFOTestDataSO_07(), new CLFOTestDataSO_08(), new CLFOTestDataPerformance(10, 5),
      new CLFOTestDataPO_13(), new CLFOTestDataPO_14(), new CLFOTestDataPO_15(),
      new CLFOTestDataPO_16(), new CLFOTestDataPO_17(), new CLFOTestDataPO_18(),
      new CLFOTestDataPO_19(), new CLFOTestDataPO_20(), new CLFOTestDataSO_AUM_21(),
      new CLFOTestDataSO_AUM_22());

  @Rule
  public ParameterCdiTestRule<CopyLinesFromOrdersTestData> parameterValuesRule = new ParameterCdiTestRule<CopyLinesFromOrdersTestData>(
      PARAMS);

  private @ParameterCdiTest CopyLinesFromOrdersTestData data;

  private static UOMManagementUtil uomUtil = new UOMManagementUtil();

  @BeforeClass
  public static void executeBeforeTests() {
    OBContext.setAdminMode();
    try {
      // Save the UomManagement preference before execute the tests
      uomUtil.saveUOMPreferenceStatusBeforeExecuteProcess();
      // Set the UomManagement preference disable by default, as expected in almost all tests, it
      // will be changed in case the Test needed
      uomUtil.setUOMPreference(CLFOTestConstants.DISABLE_AUM, true);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @AfterClass
  public static void executeAfterTests() {
    OBContext.setAdminMode();
    try {
      // Restore the UomManagement preference at the status it had starting the tests
      uomUtil.restoreUOMPreferenceStatus();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Execute the test with the current data
   * <ul>
   * <li>Create Orders to be copied from</li>
   * <li>Create the Order to be processed</li>
   * <li>Execute the Copy Lines From Orders process</li>
   * <li>Validate that copied lines and order amounts are correct</li>
   * </ul>
   */
  @Test
  public void testCopyLinesFromOrders() {
    setUpTest();

    try {
      List<Order> ordersFrom = createOrdersToBeCopiedFrom();
      Order processingOrder = createOrderToBeProcessed();
      applyTestSettingsBeforeExecuteProcess();
      executeCopyLinesFromOrdersProcess(processingOrder, ordersFrom);
      validateGeneratedData(processingOrder);
      log.info("Test Completed successfully {}: {} ", this.testNumber, this.testDescription);
    }

    catch (Exception e) {
      log.error("Error when executing testCopyLinesFromOrders", e);
      assertFalse(true);
    }
  }

  private void applyTestSettingsBeforeExecuteProcess() {
    OBContext.setAdminMode();
    try {
      data.applyTestSettingsBeforeExecuteProcess();
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private void setUpTest() {
    setData();
    log.info("Test Started {}: {} ", this.testNumber, this.testDescription);
    setOBContext();
    OBContext.setAdminMode();
    try {
      applyTestSettings();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method executes extra configuration settings needed by some specific tests. If a test need
   * to perform a particular action then this method should be implemented in this way
   */
  private void applyTestSettings() {
    data.applyTestSettings();
  }

  /**
   * Set the OB Context according the client used by the running test
   */
  private void setOBContext() {
    if (this.executeAsQAAdmin) {
      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
    } else {
      OBContext.setOBContext(USER_ID, FB_ROLE_ID, FB_CLIENT_ID, FB_ORGANIZATION_ID);
    }
  }

  /**
   * Executes the Copy Lines From Orders process.
   * 
   * <ul>
   * <li>Format the order list to a JSONArray as expected in CopyFromOrdersProcess process</li>
   * <li>Copy the order lines from passed order list to the processing order</li>
   * </ul>
   * 
   * @param processingOrder
   * @param ordersFrom
   */
  private void executeCopyLinesFromOrdersProcess(Order processingOrder, List<Order> ordersFrom) {
    CopyFromOrdersProcess copyFromOrdersProcess = WeldUtils
        .getInstanceFromStaticBeanManager(CopyFromOrdersProcess.class);
    JSONArray orders = createJSONFromOrders(ordersFrom);
    long startTime = System.currentTimeMillis();
    copyFromOrdersProcess.copyOrderLines(processingOrder, orders);
    long endTime = System.currentTimeMillis();
    log.info(String.format("CopyFromOrdersProcess: Time taken to complete the process: %d ms",
        (endTime - startTime)));
    OBDal.getInstance().refresh(processingOrder);
  }

  private JSONArray createJSONFromOrders(List<Order> ordersFrom) {
    return JSONUtils.createJSONFromOrders(ordersFrom);
  }

  private Order createOrderToBeProcessed() {
    return createOrder(processedOrder, null);
  }

  private List<Order> createOrdersToBeCopiedFrom() {
    List<Order> orderList = new ArrayList<Order>();
    for (int index = 0; index < ordersCopiedFrom.length; index++) {
      orderList.add(createOrder(ordersCopiedFrom[index], orderLinesCopiedFrom[index]));
    }
    return orderList;
  }

  private Order createOrder(OrderData orderData, OrderLineData[] orderLinesData) {
    Order order = createOrderHeaderFromData(orderData);
    for (int i = 0; orderLinesData != null && i < orderLinesData.length; i++) {
      addOrderLineFromData(order, orderLinesData[i]);
    }
    return order;
  }

  public Order createOrderHeaderFromData(OrderData orderData) {
    Order testOrder = OBProvider.getInstance().get(Order.class);
    testOrder.setDocumentNo(orderData.getDocumentNo());
    testOrder.setOrderDate(new Date());
    testOrder.setAccountingDate(new Date());
    testOrder.setScheduledDeliveryDate(
        DateUtils.addDays(new Date(), orderData.getDeliveryDaysCountFromNow()));
    testOrder.setDocumentType(
        OBDal.getInstance().getProxy(DocumentType.class, orderData.getDocumentTypeId()));
    testOrder.setTransactionDocument(
        OBDal.getInstance().getProxy(DocumentType.class, orderData.getDocumentTypeId()));
    testOrder.setOrganization(
        OBDal.getInstance().getProxy(Organization.class, orderData.getOrganizationId()));
    testOrder.setBusinessPartner(
        OBDal.getInstance().getProxy(BusinessPartner.class, orderData.getBusinessPartnerId()));
    testOrder.setPartnerAddress(
        OBDal.getInstance().getProxy(Location.class, orderData.getBusinessPartnerLocationId()));
    testOrder.setInvoiceAddress(
        OBDal.getInstance().getProxy(Location.class, orderData.getBusinessPartnerLocationId()));
    testOrder.setSummedLineAmount(BigDecimal.ZERO);
    testOrder.setGrandTotalAmount(BigDecimal.ZERO);
    testOrder.setCurrency(
        OBDal.getInstance().getProxy(Currency.class, CLFOTestConstants.EUR_CURRENCY_ID));
    testOrder.setSalesTransaction(orderData.isSales());
    testOrder.setPriceIncludesTax(orderData.isPriceIncludingTaxes());
    testOrder
        .setPriceList(OBDal.getInstance().getProxy(PriceList.class, orderData.getPriceListId()));
    testOrder.setPaymentMethod(
        OBDal.getInstance().getProxy(FIN_PaymentMethod.class, orderData.getPaymentMethodId()));
    testOrder.setPaymentTerms(
        OBDal.getInstance().getProxy(PaymentTerm.class, orderData.getPaymentTermsId()));
    testOrder
        .setWarehouse(OBDal.getInstance().getProxy(Warehouse.class, orderData.getWarehouseId()));
    OBDal.getInstance().save(testOrder);
    OBDal.getInstance().flush();
    return testOrder;
  }

  private void addOrderLineFromData(Order order, OrderLineData line) {
    AttributeSetInstance attributeSetInstance = StringUtils
        .isNotEmpty(line.getAttributeSetInstanceId())
            ? OBDal.getInstance()
                .getProxy(AttributeSetInstance.class, line.getAttributeSetInstanceId())
            : null;
    UOM operativeUOM = null;
    BigDecimal operativeQuantity = BigDecimal.ZERO;
    if (UOMUtil.isUomManagementEnabled()) {
      if (StringUtils.isEmpty(line.getOperativeUOMId())) {
        operativeUOM = OBDal.getInstance().getProxy(UOM.class, line.getUomId());
        operativeQuantity = line.getOrderedQuantity();
      } else {
        operativeUOM = OBDal.getInstance().getProxy(UOM.class, line.getOperativeUOMId());
        operativeQuantity = line.getOperativeQuantity();
      }
    }
    addOrderLine(order, line.getLineNo(),
        OBDal.getInstance().getProxy(BusinessPartner.class, line.getBusinessPartnerId()),
        OBDal.getInstance().getProxy(Organization.class, line.getOrganizationId()),
        OBDal.getInstance().getProxy(Product.class, line.getProductId()),
        OBDal.getInstance().getProxy(UOM.class, line.getUomId()), line.getOrderedQuantity(),
        line.getPrice(), OBDal.getInstance().getProxy(TaxRate.class, line.getTaxId()),
        OBDal.getInstance().getProxy(Warehouse.class, line.getWarehouseId()), line.getDescription(),
        attributeSetInstance, operativeQuantity, operativeUOM);
  }

  public void addOrderLine(Order order, Long lineNo, BusinessPartner businessPartner,
      Organization organization, Product product, UOM uom, BigDecimal orderedQuantity,
      BigDecimal price, TaxRate tax, Warehouse warehouse, String description,
      AttributeSetInstance attributeSetInstance, BigDecimal operativeQuantity, UOM operativeUOM) {
    OrderLine testOrderLine = OBProvider.getInstance().get(OrderLine.class);
    testOrderLine.setLineNo(lineNo);
    testOrderLine.setOrderDate(new Date());
    testOrderLine.setOrganization(organization);
    testOrderLine.setBusinessPartner(businessPartner);
    testOrderLine.setWarehouse(warehouse);
    testOrderLine.setDescription(description);
    testOrderLine.setCurrency(
        OBDal.getInstance().getProxy(Currency.class, CLFOTestConstants.EUR_CURRENCY_ID));
    testOrderLine.setProduct(product);
    testOrderLine.setUOM(uom);
    testOrderLine.setOrderedQuantity(orderedQuantity);
    if (UOMUtil.isUomManagementEnabled()) {
      testOrderLine.setOperativeUOM(operativeUOM);
      testOrderLine.setOperativeQuantity(operativeQuantity);
    }

    if (order.isPriceIncludesTax()) {
      testOrderLine.setGrossUnitPrice(price);
      testOrderLine.setGrossListPrice(price);
      testOrderLine.setBaseGrossUnitPrice(price);
    } else {
      testOrderLine.setUnitPrice(price);
      testOrderLine.setListPrice(price);
      testOrderLine.setStandardPrice(price);
    }
    testOrderLine.setTax(tax);
    testOrderLine.setLineGrossAmount(orderedQuantity.multiply(price));
    testOrderLine.setLineNetAmount(orderedQuantity.multiply(price));

    testOrderLine.setSalesOrder(order);
    order.getOrderLineList().add(testOrderLine);

    if (attributeSetInstance != null) {
      OBContext.setAdminMode();
      try {
        testOrderLine.setAttributeSetValue(attributeSetInstance);
        attributeSetInstance.getOrderLineList().add(testOrderLine);
      } finally {
        OBContext.restorePreviousMode();
      }
    }

    OBDal.getInstance().save(testOrderLine);
    OBDal.getInstance().flush();
  }

  private void validateGeneratedData(Order processingOrder) {
    validateExpectedOrderAmounts(processingOrder);
    validateExpectedOrderLines(processingOrder);
  }

  private void validateExpectedOrderAmounts(Order processingOrder) {
    String expectedTotalNetAmount = expectedOrderAmounts[0];
    String expectedTotalGrossAmount = expectedOrderAmounts[1];
    assertThat(
        testNumber + ". Wrong Total Net Amount = "
            + processingOrder.getSummedLineAmount().toString() + "). Was expected "
            + expectedTotalNetAmount,
        processingOrder.getSummedLineAmount(),
        comparesEqualTo(new BigDecimal(expectedTotalNetAmount)));

    assertThat(
        testNumber + ". Wrong Total Gross Amount = "
            + processingOrder.getGrandTotalAmount().toString() + "). Was expected "
            + expectedTotalNetAmount,
        processingOrder.getGrandTotalAmount(),
        comparesEqualTo(new BigDecimal(expectedTotalGrossAmount)));
  }

  private void validateExpectedOrderLines(Order processingOrder) {
    assertThat(
        testNumber + ". Number of lines copied = " + processingOrder.getOrderLineList().size()
            + " different than expected (" + expectedOrderLinesData.size() + ")",
        processingOrder.getOrderLineList().size(), comparesEqualTo(expectedOrderLinesData.size()));

    for (OrderLine orderLineCopied : processingOrder.getOrderLineList()) {
      String lineNo = orderLineCopied.getLineNo().toString();
      if (!expectedOrderLinesData.containsKey(lineNo)) {
        assertTrue(testNumber + ". Line No. " + lineNo + " isn't expected in the list", false);
      } else {
        String[] expectedLineData = expectedOrderLinesData.get(lineNo);
        String expectedProductName = expectedLineData[0];
        String expectedOrderedQty = expectedLineData[1];
        String expectedUOM = expectedLineData[2];
        String expectedNetUnitPrice = expectedLineData[3];
        String expectedListPrice = expectedLineData[4];
        String expectedGrossUnitPrice = expectedLineData[5];
        String expectedTax = expectedLineData[6];
        String expectedReferenceOrderDocumentNo = expectedLineData[7];
        String expectedBPAddress = expectedLineData[8];
        String expectedOrganization = expectedLineData[9];
        String expectedAttributeValue = expectedLineData[10];
        String expectedOperativeQty = expectedLineData[11];
        String expectedOperativeUOM = expectedLineData[12];
        String expectedDescription = expectedLineData[13];

        String referenceOrderDocumentNo = orderLineCopied.getSOPOReference()
            .getSalesOrder()
            .getDocumentNo();
        assertThat(
            testNumber + ". Wrong Reference Order = " + referenceOrderDocumentNo
                + "). Was expected " + expectedReferenceOrderDocumentNo,
            referenceOrderDocumentNo, comparesEqualTo(expectedReferenceOrderDocumentNo));

        String productName = orderLineCopied.getProduct().getName();
        assertThat(testNumber + ". Wrong Product = " + productName + "). Was expected "
            + expectedProductName, productName, comparesEqualTo(expectedProductName));

        BigDecimal orderQty = orderLineCopied.getOrderedQuantity();
        assertThat(
            testNumber + ". Wrong Ordered Qty = " + orderQty + "). Was expected "
                + expectedOrderedQty,
            orderQty, comparesEqualTo(new BigDecimal(expectedOrderedQty)));

        String uom = orderLineCopied.getUOM().getName();
        assertThat(testNumber + ". Wrong UOM = " + uom + "). Was expected " + expectedUOM, uom,
            comparesEqualTo(expectedUOM));

        BigDecimal netUnitPrice = orderLineCopied.getUnitPrice();
        assertThat(
            testNumber + ". Wrong Net Unit Price = " + netUnitPrice + "). Was expected "
                + expectedNetUnitPrice,
            netUnitPrice, comparesEqualTo(new BigDecimal(expectedNetUnitPrice)));

        BigDecimal listPrice = orderLineCopied.getListPrice();
        assertThat(testNumber + ". Wrong List Price = " + listPrice + "). Was expected "
            + expectedListPrice, listPrice, comparesEqualTo(new BigDecimal(expectedListPrice)));

        BigDecimal grossPrice = orderLineCopied.getGrossUnitPrice();
        assertThat(
            testNumber + ". Wrong Gross Unit Price = " + grossPrice + "). Was expected "
                + expectedGrossUnitPrice,
            grossPrice, comparesEqualTo(new BigDecimal(expectedGrossUnitPrice)));

        String taxName = orderLineCopied.getTax().getName();
        assertThat(testNumber + ". Wrong Tax = " + taxName + "). Was expected " + expectedTax,
            taxName, comparesEqualTo(expectedTax));

        // Check information was copied from the header
        String headerOrderDate = processingOrder.getOrderDate().toString();
        String lineOrderDate = orderLineCopied.getOrderDate().toString();
        assertThat(testNumber + ". Wrong Order Line Date = " + lineOrderDate + "). Was expected "
            + headerOrderDate, lineOrderDate, comparesEqualTo(headerOrderDate));

        String headerDeliveryDate = processingOrder.getOrderDate().toString();
        String lineDeliveryDate = orderLineCopied.getOrderDate().toString();
        assertThat(
            testNumber + ". Wrong Order Line Schedule Delivery Date = " + lineDeliveryDate
                + "). Was expected " + headerDeliveryDate,
            lineDeliveryDate, comparesEqualTo(headerDeliveryDate));

        String lineDescription = orderLineCopied.getDescription();
        if (StringUtils.isNotEmpty(expectedDescription)
            || StringUtils.isNotEmpty(lineDescription)) {
          assertThat(
              testNumber + ". Wrong Order Line Description = " + lineDescription
                  + "). Was expected " + expectedDescription,
              lineDescription, comparesEqualTo(expectedDescription));
        }

        // Check BP Address
        String businessPartnerAddress = orderLineCopied.getPartnerAddress().getIdentifier();
        assertThat(testNumber + ". Wrong Partner Address = " + businessPartnerAddress
            + "). Was expected " + expectedBPAddress, businessPartnerAddress,
            comparesEqualTo(expectedBPAddress));

        // Check Organization
        String organization = orderLineCopied.getOrganization().getName();
        assertThat(testNumber + ". Wrong Organization = " + organization + "). Was expected "
            + expectedOrganization, organization, comparesEqualTo(expectedOrganization));

        // Check Attribute
        String attributeValue = orderLineCopied.getAttributeSetValue() != null
            ? orderLineCopied.getAttributeSetValue()
                .getAttributeSet()
                .getAttributeUseList()
                .get(0)
                .getAttribute()
                .getAttributeValueList()
                .get(0)
                .getName()
            : "";
        if (StringUtils.isNotEmpty(attributeValue)
            || StringUtils.isNotEmpty(expectedAttributeValue)) {
          assertThat(
              testNumber + ". Wrong Attribute Value = " + attributeValue + "). Was expected "
                  + expectedAttributeValue,
              attributeValue, comparesEqualTo(expectedAttributeValue));
        }

        // Check AUM
        if (UOMUtil.isUomManagementEnabled()) {
          BigDecimal operativeQty = orderLineCopied.getOperativeQuantity();
          assertThat(
              testNumber + ". Wrong Operative Qty = " + operativeQty + "). Was expected "
                  + expectedOperativeQty,
              operativeQty, comparesEqualTo(new BigDecimal(expectedOperativeQty)));

          String operativeAUM = orderLineCopied.getOperativeUOM().getName();
          assertThat(testNumber + ". Wrong Operative UOM = " + operativeAUM + "). Was expected "
              + expectedOperativeUOM, operativeAUM, comparesEqualTo(expectedOperativeUOM));
        }
      }
    }
  }
}
