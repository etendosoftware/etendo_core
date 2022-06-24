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

package org.openbravo.test.copyLinesFromOrders.data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Attribute;
import org.openbravo.model.common.plm.AttributeSet;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.AttributeUse;
import org.openbravo.model.common.plm.AttributeValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.tax.TaxCategory;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.model.pricing.pricelist.ProductPrice;

/**
 * Check that created line has the same product and attributes than the line from it was created
 * from. Check three cases: 6.1 - Product without attributes (already tested in other cases) 6.2 -
 * Product with attributes (an instance attribute). In this case the attribute is copied. 6.3 -
 * Product with attributes (not an instance attribute). In this case the attribute isn't copied.
 * 
 * @author Mark
 * 
 */
public class CLFOTestDataPO_18 extends CopyLinesFromOrdersTestData {

  private static String TEST_ORDERFROM1_DOCUMENTNO = "CLFOTestData18_OrderFrom1";
  private static String TEST_ORDERFROM2_DOCUMENTNO = "CLFOTestData18_OrderFrom2";
  private static String TEST_ORDERTO1_DOCUMENTNO = "CLFOTestData18_OrderTo1";

  private static final String USE_ATTRIBUTE_SET_VALUE_AS_DEFAULT = "D";
  private static final String ATTRIBUTE_SIZE = "SizePO";
  private static final String ATTRIBUTE_VALUE_XL = "XL";

  private static final String SHORT_PRODUCT_WITH_INSTANCE_ATTRIBUTE_ID = SequenceIdData.getUUID();
  private static final String SHORT_ATTRIBUTE_INSTANCE_ID = SequenceIdData.getUUID();
  private static final String SHORT_PRODUCT_NAME = "ShortPO"
      + SHORT_PRODUCT_WITH_INSTANCE_ATTRIBUTE_ID;
  private static final String PENCIL_PRODUCT_WITH_NON_INSTANCE_ATTRIBUTE_ID = SequenceIdData
      .getUUID();
  private static final String PENCIL_ATTRIBUTE_INSTANCE_ID = SequenceIdData.getUUID();
  private static final String PENCIL_PRODUCT_NAME = "PencilPO"
      + PENCIL_PRODUCT_WITH_NON_INSTANCE_ATTRIBUTE_ID;
  private static final String COLOUR_ATTRIBUTE_SET_ID = "88A3449ED778448488BE1B3707C21B1B";
  private static final String BLUE_ATTRIBUTE_SET_VALUE_ID = "BC8886699D4A4D8EB8749B71C9844AA7";
  private static final String ORDER_LINE_PENCIL_ATTRIBUTE_INSTANCE_ID = SequenceIdData.getUUID();
  private static final String ORDER_LINE_SHORT_ATTRIBUTE_INSTANCE_ID = SequenceIdData.getUUID();

  @Override
  public void initialize() {

    // Order will be created from
    OrderData orderFrom1 = new OrderData();
    orderFrom1.setSales(false);
    orderFrom1.setPriceIncludingTaxes(false);
    orderFrom1.setDocumentNo(TEST_ORDERFROM1_DOCUMENTNO);
    orderFrom1.setDocumentTypeId(CLFOTestConstants.DOCUMENT_TYPE_PURCHASE_ORDER);
    orderFrom1.setBusinessPartnerId(BPartnerDataConstants.VENDOR_A);
    orderFrom1.setBusinessPartnerLocationId(BPartnerDataConstants.VENDOR_A_LOCATION_ID);
    orderFrom1.setPriceListId(CLFOTestConstants.PURCHASE_PRICE_LIST_ID);
    orderFrom1.setOrganizationId(CLFOTestConstants.SPAIN_ORGANIZATION_ID);
    orderFrom1.setPaymentMethodId(CLFOTestConstants.PAYMENT_METHOD_1_SPAIN);
    orderFrom1.setPaymentTermsId(CLFOTestConstants.PAYMENT_TERMS_30_5_DAYS);
    orderFrom1.setWarehouseId(CLFOTestConstants.SPAIN_EAST_WAREHOUSE);
    orderFrom1.setDeliveryDaysCountFromNow(0);
    orderFrom1.setDescription("");

    OrderData orderFrom2 = new OrderData();
    orderFrom2.setSales(false);
    orderFrom2.setPriceIncludingTaxes(false);
    orderFrom2.setDocumentNo(TEST_ORDERFROM2_DOCUMENTNO);
    orderFrom2.setDocumentTypeId(CLFOTestConstants.DOCUMENT_TYPE_PURCHASE_ORDER);
    orderFrom2.setBusinessPartnerId(BPartnerDataConstants.VENDOR_A);
    orderFrom2.setBusinessPartnerLocationId(BPartnerDataConstants.VENDOR_A_LOCATION_ID);
    orderFrom2.setPriceListId(CLFOTestConstants.PURCHASE_PRICE_LIST_ID);
    orderFrom2.setOrganizationId(CLFOTestConstants.SPAIN_ORGANIZATION_ID);
    orderFrom2.setPaymentMethodId(CLFOTestConstants.PAYMENT_METHOD_1_SPAIN);
    orderFrom2.setPaymentTermsId(CLFOTestConstants.PAYMENT_TERMS_30_5_DAYS);
    orderFrom2.setWarehouseId(CLFOTestConstants.SPAIN_EAST_WAREHOUSE);
    orderFrom2.setDeliveryDaysCountFromNow(0);
    orderFrom2.setDescription("");
    setOrdersCopiedFrom(new OrderData[] { orderFrom1, orderFrom2 });

    // Create lines to that order
    OrderLineData order1Line1 = new OrderLineData();
    order1Line1.setLineNo(10L);
    order1Line1.setBusinessPartnerId(BPartnerDataConstants.VENDOR_A);
    order1Line1.setBusinessPartnerLocationId(BPartnerDataConstants.VENDOR_A_LOCATION_ID);
    order1Line1.setOrganizationId(CLFOTestConstants.SPAIN_ORGANIZATION_ID);
    order1Line1.setProductId(SHORT_PRODUCT_WITH_INSTANCE_ATTRIBUTE_ID);
    order1Line1.setAttributeSetInstanceId(ORDER_LINE_SHORT_ATTRIBUTE_INSTANCE_ID);
    order1Line1.setUomId(CLFOTestConstants.UNIT_UOM_ID);
    order1Line1.setOrderedQuantity(new BigDecimal("20"));
    order1Line1.setPrice(new BigDecimal("10.00"));
    order1Line1.setTaxId(CLFOTestConstants.EXEMPT_PURCHASE_TAX_ID);
    order1Line1.setWarehouseId(CLFOTestConstants.SPAIN_EAST_WAREHOUSE);
    order1Line1.setDescription(CLFOTestConstants.LINE1_DESCRIPTION);

    OrderLineData order2Line1 = new OrderLineData();
    order2Line1.setLineNo(20L);
    order2Line1.setBusinessPartnerId(BPartnerDataConstants.VENDOR_A);
    order2Line1.setBusinessPartnerLocationId(BPartnerDataConstants.VENDOR_A_LOCATION_ID);
    order2Line1.setOrganizationId(CLFOTestConstants.SPAIN_ORGANIZATION_ID);
    order2Line1.setProductId(PENCIL_PRODUCT_WITH_NON_INSTANCE_ATTRIBUTE_ID);
    order2Line1.setAttributeSetInstanceId(ORDER_LINE_PENCIL_ATTRIBUTE_INSTANCE_ID);
    order2Line1.setUomId(CLFOTestConstants.UNIT_UOM_ID);
    order2Line1.setOrderedQuantity(new BigDecimal("10"));
    order2Line1.setPrice(new BigDecimal("10.00"));
    order2Line1.setTaxId(CLFOTestConstants.EXEMPT_PURCHASE_TAX_ID);
    order2Line1.setWarehouseId(CLFOTestConstants.SPAIN_EAST_WAREHOUSE);
    order2Line1.setDescription(CLFOTestConstants.LINE2_DESCRIPTION);
    OrderLineData[][] expectedOrderLins = new OrderLineData[2][1];
    expectedOrderLins[0] = new OrderLineData[] { order1Line1 };
    expectedOrderLins[1] = new OrderLineData[] { order2Line1 };
    setOrderLinesCopiedFrom(expectedOrderLins);

    // Information of the order that will be processed
    OrderData orderToBeProcessed = new OrderData();
    orderToBeProcessed.setSales(false);
    orderToBeProcessed.setPriceIncludingTaxes(false);
    orderToBeProcessed.setDocumentNo(TEST_ORDERTO1_DOCUMENTNO);
    orderToBeProcessed.setDocumentTypeId(CLFOTestConstants.DOCUMENT_TYPE_STANDARD);
    orderToBeProcessed.setBusinessPartnerId(BPartnerDataConstants.VENDOR_A);
    orderToBeProcessed.setBusinessPartnerLocationId(BPartnerDataConstants.VENDOR_A_LOCATION_ID);
    orderToBeProcessed.setPriceListId(CLFOTestConstants.PURCHASE_PRICE_LIST_ID);
    orderToBeProcessed.setOrganizationId(CLFOTestConstants.SPAIN_ORGANIZATION_ID);
    orderToBeProcessed.setPaymentMethodId(CLFOTestConstants.PAYMENT_METHOD_1_SPAIN);
    orderToBeProcessed.setPaymentTermsId(CLFOTestConstants.PAYMENT_TERMS_30_5_DAYS);
    orderToBeProcessed.setWarehouseId(CLFOTestConstants.SPAIN_EAST_WAREHOUSE);
    orderToBeProcessed.setDeliveryDaysCountFromNow(0);
    orderToBeProcessed.setDescription("");
    setOrder(orderToBeProcessed);

    /**
     * This array will be used to verify the Order Header values after the process is executed. Has
     * the following structure: [Total Net Amount expected, Total Gross amount expected]
     */
    setExpectedOrderAmounts(new String[] { "300.00", "300.00" });

    /**
     * This Map will be used to verify the Order Lines values after the process is executed. Map has
     * the following structure: <Line No, [Product Name, Ordered Qty, UOM, Net Unit Price Expected,
     * List Price Expected, Gross Unit Price, Tax, Reference to PO/SO DocumentNo From it was
     * created, BP Address, Organization, Attribute Value, Operative Qty, Operative UOM]>
     */
    HashMap<String, String[]> expectedOrderLines = new HashMap<String, String[]>();
    expectedOrderLines.put("10",
        new String[] { SHORT_PRODUCT_NAME, "20", CLFOTestConstants.UNIT_UOM_NAME, "10.00", "10.00",
            "0", CLFOTestConstants.EXEMPT_PURCHASE_TAX_NAME, TEST_ORDERFROM1_DOCUMENTNO,
            BPartnerDataConstants.VENDOR_A_LOCATION, CLFOTestConstants.SPAIN_ORGANIZATION_NAME,
            ATTRIBUTE_VALUE_XL, null, null, CLFOTestConstants.LINE1_DESCRIPTION });
    expectedOrderLines.put("20",
        new String[] { PENCIL_PRODUCT_NAME, "10", CLFOTestConstants.UNIT_UOM_NAME, "10.00", "10.00",
            "0", CLFOTestConstants.EXEMPT_PURCHASE_TAX_NAME, TEST_ORDERFROM2_DOCUMENTNO,
            BPartnerDataConstants.VENDOR_A_LOCATION, CLFOTestConstants.SPAIN_ORGANIZATION_NAME, "",
            null, null, CLFOTestConstants.LINE2_DESCRIPTION });
    setExpectedOrderLines(expectedOrderLines);
  }

  /**
   * Add extra settings to the test
   * <ul>
   * <li>Create a new Attribute
   * <ul>
   * <li>Organization = Spain</li>
   * <li>Name = Size</li>
   * <li>Instance Attribute = Yes</li>
   * <li>List = Yes</li>
   * </ul>
   * </li>
   * <li>Add an Attribute Value to it
   * <ul>
   * <li>Search key = Name = XL</li>
   * </ul>
   * </li>
   * <li>Create a new Attribute Set
   * <ul>
   * <li>Organization = Spain</li>
   * <li>Name = Size</li>
   * </ul>
   * </li>
   * <li>Add as assigned attribute the previously created Size attribute.</li>
   * <li>Create a new Product and assign the created Size Attribute Set to it</li>
   * <li>Create a new Product and assign the existing Colour Attribute Set to it</li>
   * </ul>
   */
  @Override
  public void applyTestSettings() {
    // Create a new product with a new instance attribute
    Attribute attribute = createNewAttribute(true);
    AttributeValue attributeSetValue = createAttributeValueToAttribute(attribute);
    AttributeSet attributeSet = createNewAttributeSet();
    addAssignAttributeToAttributeSet(attribute, attributeSet);
    createNewProduct(SHORT_PRODUCT_WITH_INSTANCE_ATTRIBUTE_ID, SHORT_PRODUCT_NAME, attributeSet,
        attributeSetValue, SHORT_ATTRIBUTE_INSTANCE_ID);

    // Create a new product using an existing NOT instance attribute. In this case COLOUR
    AttributeSet colourAttributeSet = OBDal.getInstance()
        .get(AttributeSet.class, COLOUR_ATTRIBUTE_SET_ID);
    AttributeValue blueAttributeSetValue = OBDal.getInstance()
        .get(AttributeValue.class, BLUE_ATTRIBUTE_SET_VALUE_ID);
    createNewProduct(PENCIL_PRODUCT_WITH_NON_INSTANCE_ATTRIBUTE_ID, PENCIL_PRODUCT_NAME,
        colourAttributeSet, blueAttributeSetValue, PENCIL_ATTRIBUTE_INSTANCE_ID);

    // Create the Instances to be referenced in the order lines
    createAttributeSetInstance(ORDER_LINE_SHORT_ATTRIBUTE_INSTANCE_ID, attributeSet);
    createAttributeSetInstance(ORDER_LINE_PENCIL_ATTRIBUTE_INSTANCE_ID, colourAttributeSet);
  }

  private Product createNewProduct(String productId, String productName, AttributeSet attributeSet,
      AttributeValue attributeSetValue, String attributeSetInstanceId) {
    Product product = null;
    // Create Attribute Set Instance for the product
    AttributeSetInstance attrInstance = createAttributeSetInstance(attributeSetInstanceId,
        attributeSet);
    // Clone the Final Good B product
    product = cloneFinalGoodBProduct();
    // Define the new information
    defineNewProductInformation(productId, productName, attributeSet, product, attrInstance);
    // Add a product price to the product
    createProductPrice(product);

    return product;
  }

  private void createProductPrice(Product product) {
    ProductPrice productPrice = OBProvider.getInstance().get(ProductPrice.class);
    productPrice.setProduct(product);
    productPrice.setListPrice(new BigDecimal("10.00"));
    productPrice.setPriceLimit(new BigDecimal("10.00"));
    productPrice.setStandardPrice(new BigDecimal("10.00"));
    productPrice.setPriceListVersion(OBDal.getInstance()
        .getProxy(PriceListVersion.class, CLFOTestConstants.PURCHASE_PRICE_LIST_VERSION_ID));
    product.getPricingProductPriceList().add(productPrice);
    OBDal.getInstance().save(productPrice);
    OBDal.getInstance().flush();
  }

  private void defineNewProductInformation(String productId, String productName,
      AttributeSet attributeSet, Product product, AttributeSetInstance attrInstance) {
    if (StringUtils.isNotEmpty(productId)) {
      product.setId(productId);
    }
    product.setNewOBObject(true);
    product.setAttributeSet(attributeSet);
    product.setSearchKey(productName);
    product.setName(productName);
    product.setSale(true);
    product.setPurchase(true);
    product.setProductCategory(
        OBDal.getInstance().getProxy(ProductCategory.class, "4028E6C72959682B01295ADC1CC80228"));
    product.setTaxCategory(
        OBDal.getInstance().getProxy(TaxCategory.class, "E02F948001F44F479D709EBC6911E310"));
    product.setUseAttributeSetValueAs(USE_ATTRIBUTE_SET_VALUE_AS_DEFAULT);
    product.setEnforceAttribute(true);
    product.setAttributeSetValue(attrInstance);
    product.setUOM(OBDal.getInstance().getProxy(UOM.class, CLFOTestConstants.UNIT_UOM_ID));
    OBDal.getInstance().save(product);
    OBDal.getInstance().flush();
  }

  private Product cloneFinalGoodBProduct() {
    Product product;
    Product productFGA = OBDal.getInstance()
        .get(Product.class, CLFOTestConstants.FINAL_GOOD_B_PRODUCT_ID);
    product = (Product) DalUtil.copy(productFGA, false);
    // Avoid duplication in UPC breaking retail CI
    product.setUPCEAN(StringUtils.left(UUID.randomUUID().toString(), 30));
    return product;
  }

  private AttributeSetInstance createAttributeSetInstance(String attributeSetInstanceId,
      AttributeSet attributeSet) {
    AttributeSetInstance attrInstance = OBProvider.getInstance().get(AttributeSetInstance.class);
    if (StringUtils.isNotEmpty(attributeSetInstanceId)) {
      attrInstance.setId(attributeSetInstanceId);
      attrInstance.setNewOBObject(true);
    }
    attrInstance.setAttributeSet(attributeSet);
    attrInstance.setOrganization(
        OBDal.getInstance().get(Organization.class, CLFOTestConstants.SPAIN_ORGANIZATION_ID));
    if (!attributeSet.getAttributeUseList().isEmpty()) {
      AttributeValue attributeValue = attributeSet.getAttributeUseList()
          .get(0)
          .getAttribute()
          .getAttributeValueList()
          .get(0);
      attrInstance.setDescription(attributeValue.getName());
    }
    OBDal.getInstance().save(attrInstance);
    OBDal.getInstance().flush();
    return attrInstance;
  }

  private void addAssignAttributeToAttributeSet(Attribute attribute, AttributeSet attributeSet) {
    AttributeUse attributeUse = OBProvider.getInstance().get(AttributeUse.class);
    attributeUse.setActive(true);
    attributeUse.setSequenceNumber(10L);
    attributeUse.setAttribute(attribute);
    attributeUse.setAttributeSet(attributeSet);
    OBDal.getInstance().save(attributeUse);
    attributeSet.getAttributeUseList().add(attributeUse);
    OBDal.getInstance().flush();
  }

  private AttributeSet createNewAttributeSet() {
    AttributeSet attributeSet = OBProvider.getInstance().get(AttributeSet.class);
    attributeSet.setName(ATTRIBUTE_SIZE);
    attributeSet.setExpirationDate(false);
    attributeSet.setLot(false);
    attributeSet.setSerialNo(false);
    attributeSet.setOrganization(
        OBDal.getInstance().get(Organization.class, CLFOTestConstants.SPAIN_ORGANIZATION_ID));
    OBDal.getInstance().save(attributeSet);
    OBDal.getInstance().flush();
    return attributeSet;
  }

  private AttributeValue createAttributeValueToAttribute(Attribute attribute) {
    AttributeValue attributeValue = OBProvider.getInstance().get(AttributeValue.class);
    attributeValue.setSearchKey(ATTRIBUTE_VALUE_XL);
    attributeValue.setName(ATTRIBUTE_VALUE_XL);
    attributeValue.setActive(true);
    attributeValue.setOrganization(
        OBDal.getInstance().get(Organization.class, CLFOTestConstants.SPAIN_ORGANIZATION_ID));
    attributeValue.setAttribute(attribute);
    attribute.getAttributeValueList().add(attributeValue);
    OBDal.getInstance().save(attributeValue);
    OBDal.getInstance().flush();
    return attributeValue;
  }

  private Attribute createNewAttribute(boolean isInstance) {
    Attribute attribute = OBProvider.getInstance().get(Attribute.class);
    attribute.setInstanceAttribute(isInstance);
    attribute.setMandatory(true);
    attribute.setName(ATTRIBUTE_SIZE);
    attribute.setList(true);
    attribute.setActive(true);
    attribute.setOrganization(
        OBDal.getInstance().get(Organization.class, CLFOTestConstants.SPAIN_ORGANIZATION_ID));
    OBDal.getInstance().save(attribute);
    OBDal.getInstance().flush();
    return attribute;
  }

  @Override
  public String getTestNumber() {
    return "18";
  }

  @Override
  public String getTestDescription() {
    return "Check that created line has the same product and attributes than the line from it was created from.";
  }

  @Override
  public boolean isExecuteAsQAAdmin() {
    return true;
  }
}
