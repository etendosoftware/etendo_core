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

package org.openbravo.test.pricelist;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.PriceListSchema;
import org.openbravo.model.pricing.pricelist.PriceListSchemeLine;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.db.CallProcess;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.pricelist.data.PriceListSchemaLineTestData;
import org.openbravo.test.pricelist.data.PriceListTestConstants;
import org.openbravo.test.pricelist.data.PriceListTestData;
import org.openbravo.test.pricelist.data.PriceListTestData1;
import org.openbravo.test.pricelist.data.PriceListTestData10;
import org.openbravo.test.pricelist.data.PriceListTestData11;
import org.openbravo.test.pricelist.data.PriceListTestData12;
import org.openbravo.test.pricelist.data.PriceListTestData13;
import org.openbravo.test.pricelist.data.PriceListTestData14;
import org.openbravo.test.pricelist.data.PriceListTestData15;
import org.openbravo.test.pricelist.data.PriceListTestData16;
import org.openbravo.test.pricelist.data.PriceListTestData17;
import org.openbravo.test.pricelist.data.PriceListTestData2;
import org.openbravo.test.pricelist.data.PriceListTestData3;
import org.openbravo.test.pricelist.data.PriceListTestData4;
import org.openbravo.test.pricelist.data.PriceListTestData5;
import org.openbravo.test.pricelist.data.PriceListTestData6;
import org.openbravo.test.pricelist.data.PriceListTestData7;
import org.openbravo.test.pricelist.data.PriceListTestData8;
import org.openbravo.test.pricelist.data.PriceListTestData9;

/**
 * Tests cases to check Price Lists Generation
 * 
 * @author Mark
 *
 */
@RunWith(Parameterized.class)
public class PriceListTest extends OBBaseTest {
  final static private Logger log = LogManager.getLogger();

  // User Openbravo
  private final String USER_ID = "100";
  // Client QA Testing
  private final String CLIENT_ID = "4028E6C72959682B01295A070852010D";
  // Organization Spain
  private final String ORGANIZATION_ID = "357947E87C284935AD1D783CF6F099A1";
  // Role QA Testing Admin
  private final String ROLE_ID = "4028E6C72959682B01295A071429011E";
  // M_PRICELIST_CREATE Procedure ID and NAME
  private final String M_PRICELIST_CREATE_PROCEDURE_ID = "103";

  // Test information
  private String testNumber;
  private String testDescription;

  // Price List Scheme Lines
  private PriceListSchemaLineTestData[] testPriceListRules;
  private HashMap<String, String[]> expectedProductPricesData;

  // Price List Header
  private String currencyId;
  private boolean isSalesPrice;
  private boolean isBasedOnCost;
  private boolean isPriceIncludesTax;
  private boolean isDefault;

  // Price List Version
  private String basePriceListVersionId;

  public PriceListTest(String testNumber, String testDescription, PriceListTestData data) {
    this.testNumber = testNumber;
    this.testDescription = testDescription;
    this.testPriceListRules = data.getTestPriceListRulesData();
    this.currencyId = data.getCurrencyId();
    this.isSalesPrice = data.isSalesPrice();
    this.isBasedOnCost = data.isBasedOnCost();
    this.isPriceIncludesTax = data.isPriceIncludesTax();
    this.isDefault = data.isDefault();
    this.basePriceListVersionId = data.getBasePriceListVersionId();
    this.expectedProductPricesData = data.getExpectedProductPrices();
  }

  /** Parameterized possible combinations for price list computation */
  @Parameters(name = "idx:{0} name:{1}")
  public static Collection<Object[]> params() {
    return Arrays.asList(new Object[][] { { "01",
        "PriceListSchema with one rule associated to the same Product Category. Unit Price and List Price discounts applied.",
        new PriceListTestData1() //
        },
        { "02",
            "PriceListSchema with one rule associated to specific Product. Unit Price and List Price discounts applied.",
            new PriceListTestData2() //
        },
        { "03",
            "PriceListSchema with more than one rule associated. First to an entire Product category and second one to specific Product. Unit Price and List Price discounts applied in both rules.",
            new PriceListTestData3() //
        },
        { "04",
            "Price List Schema with Fixed Prices for an entire Product Category and selected base Price List.",
            new PriceListTestData4() //
        },
        { "05",
            "Price List Schema with Cost Prices and an entire Product Category and without select base Price List.",
            new PriceListTestData5() //
        },
        { "06",
            "Price List Schema with Cost Prices and an selected Product and without select base Price List.",
            new PriceListTestData6() //
        },
        { "07",
            "Price List Schema with Fixed Price or Cost Based, an entire Product Category and base Price List.",
            new PriceListTestData7() //
        },
        { "08",
            "Price List Schema with Fixed Price or Cost Based, and selected Product and base Price List.",
            new PriceListTestData8() //
        },
        { "09",
            "Price List Schema with Fixed Price or Cost plus Margin Based, an entire Product Category and base Price List.",
            new PriceListTestData9() //
        },
        { "10",
            "Price List Schema with Fixed Price or Cost plus Margin Based, and selected Product and base Price List.",
            new PriceListTestData10() //
        },
        { "11",
            "Price List Schema with different unit price and list price discounts of ZERO percent, surcharge amounts and all products of a Product Category and base Price List.",
            new PriceListTestData11() //
        },
        { "12",
            "Price List Schema with different unit price and list price discounts, surcharge amounts and all products of a Product Category and base Price List.",
            new PriceListTestData12() //
        },
        { "13",
            "Price List Schema with different Limit (PO) price and Limit (PO) price discounts of ZERO percent, surcharge amounts and all products of a Product Category. Price List Based On Cost.",
            new PriceListTestData13() //
        },
        { "14",
            "Price List Schema with different Limit (PO) price and Limit (PO) price discounts, surcharge amounts and all products of a Product Category and base Price List. Price list doesn't based on cost.",
            new PriceListTestData14() //
        },
        { "15",
            "Price List Schema with more than one discount line, each associated to different Product Categories and rules. Price List Based on Cost.",
            new PriceListTestData15() //
        },
        { "16",
            "Price List Schema with four different rules applied. Unit Price and List Price discounts applied.",
            new PriceListTestData16() //
        },
        { "17",
            "Data used for Test: Price List Schema with more than one rule applied. One rule with 50% unit price for one product, and second one with one product of of same Category and an unit price of 50%",
            new PriceListTestData17() //
        }, });
  }

  @Test
  public void testPriceListProductPrices() {

    log.info("Test Started {}: {} ", this.testNumber, this.testDescription);

    // Set QA context
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);

    try {
      PriceListSchema priceListSchema = createPriceListSchema();
      addPriceListSchemeLines(priceListSchema);

      PriceList priceList = createPriceList();
      PriceListVersion priceListVersion = addPriceListVersion(priceList, priceListSchema);

      generateProductPriceList(priceListVersion);
      validateGeneratedPrices(priceListVersion);

      deletePriceList(priceList);
      deletePriceListSchema(priceListSchema);

      log.info("Test Completed successfully");
    }

    catch (Exception e) {
      log.error("Error when executing testPriceListProductPrices", e);
      assertFalse(true);
    }

  }

  /**
   * Create a new Price List Schema (Header only)
   * 
   * @return The new Price List Schema created.
   */
  private PriceListSchema createPriceListSchema() {
    PriceListSchema pls = OBProvider.getInstance().get(PriceListSchema.class);
    pls.setClient(OBDal.getInstance().get(Client.class, CLIENT_ID));
    pls.setOrganization(OBDal.getInstance().get(Organization.class, ORGANIZATION_ID));
    pls.setName(PriceListTestConstants.PRICE_LIST_SCHEMA_NAME);
    OBDal.getInstance().save(pls);
    return pls;
  }

  /**
   * Add lines to a Price List Schema. It creates as many lines as defined in the Price List Schema
   * test data, and add them to the price list schema.
   * 
   * @param priceListSchema
   *          The Price List Schema where the lines will be added
   */
  private void addPriceListSchemeLines(PriceListSchema priceListSchema) {
    for (int i = 0; i < testPriceListRules.length; i++) {
      PriceListSchemeLine line = createPriceListSchemeLine(priceListSchema, testPriceListRules[i],
          i);
      priceListSchema.getPricingPriceListSchemeLineList().add(line);
      OBDal.getInstance().save(priceListSchema);
    }
  }

  /**
   * Creates a new Price List Scheme Line based on Price List Rule Data of the test, and link it to
   * the Price List Schema passed as parameter.
   * 
   * @param priceListSchema
   *          The Price List Schema where the lines will be added
   * @param priceListRuleData
   *          The Line Data where the line will be copied
   * @param seqNo
   *          The sequence number of the line
   * @return The Price List Scheme Line created
   */
  private PriceListSchemeLine createPriceListSchemeLine(PriceListSchema priceListSchema,
      PriceListSchemaLineTestData priceListRuleData, int seqNo) {

    PriceListSchemeLine plsl = OBProvider.getInstance().get(PriceListSchemeLine.class);
    plsl.setClient(OBDal.getInstance().get(Client.class, CLIENT_ID));
    plsl.setOrganization(OBDal.getInstance().get(Organization.class, ORGANIZATION_ID));
    plsl.setSequenceNumber((seqNo + 1) * 10L);
    plsl.setPriceLimitDiscount(BigDecimal.ZERO);
    plsl.setPriceListSchema(priceListSchema);

    // Reference section fields
    plsl.setConversionDate(new Date());
    if (StringUtils.isNotEmpty(priceListRuleData.getBusinessPartnerId())) {
      BusinessPartner businessPartnerData = OBDal.getInstance()
          .get(BusinessPartner.class, priceListRuleData.getBusinessPartnerId());
      plsl.setBusinessPartner(businessPartnerData);
    }
    if (StringUtils.isNotEmpty(priceListRuleData.getProductCategoryId())) {
      ProductCategory productCategoryData = OBDal.getInstance()
          .get(ProductCategory.class, priceListRuleData.getProductCategoryId());
      plsl.setProductCategory(productCategoryData);
    }
    if (StringUtils.isNotEmpty(priceListRuleData.getProductId())) {
      Product productData = OBDal.getInstance()
          .get(Product.class, priceListRuleData.getProductId());
      plsl.setProduct(productData);
    }

    // List Price section fields
    plsl.setBaseListPrice(priceListRuleData.getBaseListPriceValue());

    if (StringUtils.equals(priceListRuleData.getBaseListPriceValue(),
        PriceListTestConstants.REFLIST_VALUE_NET_LIST_PRICE)
        || StringUtils.equals(priceListRuleData.getBaseListPriceValue(),
            PriceListTestConstants.REFLIST_VALUE_NET_UNIT_PRICE)
        || StringUtils.equals(priceListRuleData.getBaseListPriceValue(),
            PriceListTestConstants.REFLIST_VALUE_LIMIT_PO_PRICE)) {

      plsl.setSurchargeListPriceAmount(priceListRuleData.getSurchargeListPriceAmount());
      plsl.setListPriceDiscount(priceListRuleData.getListPriceDiscount());
    } else {
      plsl.setSurchargeListPriceAmount(BigDecimal.ZERO);
      plsl.setListPriceDiscount(BigDecimal.ZERO);
    }

    if (StringUtils.equals(priceListRuleData.getBaseListPriceValue(),
        PriceListTestConstants.REFLIST_VALUE_COST)
        || StringUtils.equals(priceListRuleData.getBaseListPriceValue(),
            PriceListTestConstants.REFLIST_VALUE_FIXED_PRICE_OR_COST_BASED)
        || StringUtils.equals(priceListRuleData.getBaseListPriceValue(),
            PriceListTestConstants.REFLIST_VALUE_FIXED_PRICE_OR_COST_PLUS_MARGIN)) {

      plsl.setListPriceMargin(priceListRuleData.getListPriceMargin());
    } else {
      plsl.setListPriceMargin(BigDecimal.ZERO);
    }

    if (StringUtils.equals(priceListRuleData.getBaseListPriceValue(),
        PriceListTestConstants.REFLIST_VALUE_FIXED_PRICE)
        || StringUtils.equals(priceListRuleData.getBaseListPriceValue(),
            PriceListTestConstants.REFLIST_VALUE_FIXED_PRICE_OR_COST_BASED)
        || StringUtils.equals(priceListRuleData.getBaseListPriceValue(),
            PriceListTestConstants.REFLIST_VALUE_FIXED_PRICE_OR_COST_PLUS_MARGIN)) {

      plsl.setFixedListPrice(priceListRuleData.getFixedListPrice());
    } else {
      plsl.setFixedListPrice(BigDecimal.ZERO);
    }

    // Unit Price (Standard Price) section fields
    plsl.setStandardBasePrice(priceListRuleData.getBaseStandardPriceValue());

    if (StringUtils.equals(priceListRuleData.getBaseStandardPriceValue(),
        PriceListTestConstants.REFLIST_VALUE_NET_LIST_PRICE)
        || StringUtils.equals(priceListRuleData.getBaseStandardPriceValue(),
            PriceListTestConstants.REFLIST_VALUE_NET_UNIT_PRICE)
        || StringUtils.equals(priceListRuleData.getBaseStandardPriceValue(),
            PriceListTestConstants.REFLIST_VALUE_LIMIT_PO_PRICE)) {

      plsl.setSurchargeStandardPriceAmount(priceListRuleData.getSurchargeStandardPriceAmount());
      plsl.setStandardPriceDiscount(priceListRuleData.getStandardPriceDiscount());
    } else {
      plsl.setSurchargeStandardPriceAmount(BigDecimal.ZERO);
      plsl.setStandardPriceDiscount(BigDecimal.ZERO);
    }

    if (StringUtils.equals(priceListRuleData.getBaseStandardPriceValue(),
        PriceListTestConstants.REFLIST_VALUE_COST)
        || StringUtils.equals(priceListRuleData.getBaseStandardPriceValue(),
            PriceListTestConstants.REFLIST_VALUE_FIXED_PRICE_OR_COST_BASED)
        || StringUtils.equals(priceListRuleData.getBaseStandardPriceValue(),
            PriceListTestConstants.REFLIST_VALUE_FIXED_PRICE_OR_COST_PLUS_MARGIN)) {

      plsl.setUnitPriceMargin(priceListRuleData.getUnitPriceMargin());
    } else {
      plsl.setUnitPriceMargin(BigDecimal.ZERO);
    }

    if (StringUtils.equals(priceListRuleData.getBaseStandardPriceValue(),
        PriceListTestConstants.REFLIST_VALUE_FIXED_PRICE)
        || StringUtils.equals(priceListRuleData.getBaseStandardPriceValue(),
            PriceListTestConstants.REFLIST_VALUE_FIXED_PRICE_OR_COST_BASED)
        || StringUtils.equals(priceListRuleData.getBaseStandardPriceValue(),
            PriceListTestConstants.REFLIST_VALUE_FIXED_PRICE_OR_COST_PLUS_MARGIN)) {

      plsl.setFixedStandardPrice(priceListRuleData.getFixedStandardPrice());
    } else {
      plsl.setFixedStandardPrice(BigDecimal.ZERO);
    }

    OBDal.getInstance().save(plsl);

    return plsl;
  }

  /**
   * Creates a new Price List Header based on Price List Data of the test
   * 
   * @return The Price List created
   */
  private PriceList createPriceList() {
    PriceList pl = OBProvider.getInstance().get(PriceList.class);
    pl.setClient(OBDal.getInstance().get(Client.class, CLIENT_ID));
    pl.setOrganization(OBDal.getInstance().get(Organization.class, ORGANIZATION_ID));
    pl.setName(PriceListTestConstants.PRICE_LIST_NAME);
    pl.setCurrency(OBDal.getInstance().get(Currency.class, currencyId));
    pl.setSalesPriceList(isSalesPrice);
    pl.setCostBasedPriceList(isBasedOnCost);
    pl.setPriceIncludesTax(isPriceIncludesTax);
    pl.setDefault(isDefault);
    OBDal.getInstance().save(pl);
    return pl;
  }

  /**
   * Creates a new Price List Version based on Price List Version Data of the test, and link it to
   * the Price List passed as parameter.
   * 
   * @param priceList
   *          The Price List where the new version will be added
   * @param priceListSchema
   *          The Version's Price List Schema
   * @return The new Price List Version
   */
  private PriceListVersion addPriceListVersion(PriceList priceList,
      PriceListSchema priceListSchema) {
    PriceListVersion plv = OBProvider.getInstance().get(PriceListVersion.class);
    plv.setClient(OBDal.getInstance().get(Client.class, CLIENT_ID));
    plv.setOrganization(OBDal.getInstance().get(Organization.class, ORGANIZATION_ID));
    plv.setName(PriceListTestConstants.PRICE_LIST_NAME);
    plv.setPriceList(priceList);
    plv.setValidFromDate(new Date());
    plv.setPriceListSchema(priceListSchema);
    if (StringUtils.isNotEmpty(basePriceListVersionId)) {
      plv.setBasePriceListVersion(
          OBDal.getInstance().get(PriceListVersion.class, basePriceListVersionId));
    }
    OBDal.getInstance().save(plv);
    priceList.getPricingPriceListVersionList().add(plv);
    return plv;
  }

  /**
   * Delete a PriceListSchema and all it lines.
   * 
   * @param priceListSchema
   *          The Price List Schema to be deleted
   */
  private void deletePriceListSchema(PriceListSchema priceListSchema) {
    for (PriceListSchemeLine priceListSchemeLine : priceListSchema
        .getPricingPriceListSchemeLineList()) {
      OBDal.getInstance().remove(priceListSchemeLine);
    }
    priceListSchema.getPricingPriceListSchemeLineList().clear();
    OBDal.getInstance().remove(priceListSchema);

    log.debug("Price List Schema Deleted:" + priceListSchema.getName());
  }

  /**
   * Delete a PriceList and all it versions.
   * 
   * @param priceList
   *          The Price List to be deleted
   */
  private void deletePriceList(PriceList priceList) {
    for (PriceListVersion priceListVersion : priceList.getPricingPriceListVersionList()) {
      for (ProductPrice productPrice : priceListVersion.getPricingProductPriceList()) {
        OBDal.getInstance().remove(productPrice);
      }
      priceListVersion.getPricingProductPriceList().clear();
      OBDal.getInstance().remove(priceListVersion);
    }
    priceList.getPricingPriceListVersionList().clear();
    OBDal.getInstance().remove(priceList);

    log.debug("Price List Deleted:" + priceList.getName());
  }

  /**
   * Executes the M_PRICELIST_CREATE procedure to generate the Product Price List according the
   * Price List Version passed.
   * 
   * @param priceListVersion
   *          The Price List Version from the product prices will be generated
   */
  private void generateProductPriceList(PriceListVersion priceListVersion) {
    CallProcess.getInstance()
        .call(OBDal.getInstance().get(Process.class, M_PRICELIST_CREATE_PROCEDURE_ID),
            priceListVersion.getId(), null);

    OBDal.getInstance().refresh(priceListVersion);
  }

  /**
   * Verifies that generated Product Prices List are the expected
   * 
   * @param priceListVersion
   *          The Price List Version to be verified
   */
  private void validateGeneratedPrices(PriceListVersion priceListVersion) {

    int productPriceListCount = priceListVersion.getPricingProductPriceList().size();
    assertThat(
        testNumber + ". Number of lines obtained(" + productPriceListCount
            + ") different than expected (" + expectedProductPricesData.size() + ")",
        expectedProductPricesData.size(), comparesEqualTo(productPriceListCount));

    for (ProductPrice productPrice : priceListVersion.getPricingProductPriceList()) {

      String productName = productPrice.getProduct().getName();
      if (!expectedProductPricesData.containsKey(productName)) {
        assertTrue(testNumber + ". Product " + productName + " isn't expected in the list", false);

      } else {
        String[] prices = expectedProductPricesData.get(productName);
        String expectedUnitPrice = prices[0];
        String expectedListPrice = prices[1];

        assertThat(
            testNumber + ". Wrong Unit Price (" + productPrice.getStandardPrice().toString()
                + ") for product (" + productName + "). Was expected " + expectedUnitPrice,
            new BigDecimal(expectedUnitPrice), comparesEqualTo(productPrice.getStandardPrice()));

        assertThat(
            testNumber + ". Wrong List Price (" + productPrice.getListPrice().toString()
                + ") for product (" + productName + "). Was expected " + expectedListPrice,
            new BigDecimal(expectedListPrice), comparesEqualTo(productPrice.getListPrice()));
      }
    }
  }
}
