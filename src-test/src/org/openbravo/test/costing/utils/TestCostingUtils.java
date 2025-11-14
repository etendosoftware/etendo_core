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
 * All portions are Copyright (C) 2017-2019 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.costing.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBConfigFileProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.costing.CancelCostAdjustment;
import org.openbravo.costing.CostingBackground;
import org.openbravo.costing.CostingRuleProcess;
import org.openbravo.costing.InventoryAmountUpdateProcess;
import org.openbravo.costing.LCCostMatchFromInvoiceHandler;
import org.openbravo.costing.LCMatchingCancelHandler;
import org.openbravo.costing.LCMatchingProcessHandler;
import org.openbravo.costing.LandedCostProcessHandler;
import org.openbravo.costing.ManualCostAdjustmentProcessHandler;
import org.openbravo.costing.PriceDifferenceBackground;
import org.openbravo.costing.ReactivateLandedCost;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.ConnectionProviderImpl;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.ad_process.VerifyBOM;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.exception.PoolNotFoundException;
import org.openbravo.materialmgmt.InventoryCountProcess;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductAccounts;
import org.openbravo.model.common.plm.ProductBOM;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.cost.CostAdjustmentLine;
import org.openbravo.model.materialmgmt.cost.Costing;
import org.openbravo.model.materialmgmt.cost.InventoryAmountUpdate;
import org.openbravo.model.materialmgmt.cost.InventoryAmountUpdateLine;
import org.openbravo.model.materialmgmt.cost.LCDistributionAlgorithm;
import org.openbravo.model.materialmgmt.cost.LCMatched;
import org.openbravo.model.materialmgmt.cost.LCReceipt;
import org.openbravo.model.materialmgmt.cost.LCReceiptLineAmt;
import org.openbravo.model.materialmgmt.cost.LandedCost;
import org.openbravo.model.materialmgmt.cost.LandedCostCost;
import org.openbravo.model.materialmgmt.cost.TransactionCost;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InternalConsumption;
import org.openbravo.model.materialmgmt.transaction.InternalConsumptionLine;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ProductionLine;
import org.openbravo.model.materialmgmt.transaction.ProductionPlan;
import org.openbravo.model.materialmgmt.transaction.ProductionTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.model.procurement.ReceiptInvoiceMatch;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.costing.assertclass.CostAdjustmentAssert;
import org.openbravo.test.costing.assertclass.DocumentPostAssert;
import org.openbravo.test.costing.assertclass.LandedCostCostMatchedAssert;
import org.openbravo.test.costing.assertclass.LandedCostReceiptLineAmountAssert;
import org.openbravo.test.costing.assertclass.MatchedInvoicesAssert;
import org.openbravo.test.costing.assertclass.OrderToReceiptResult;
import org.openbravo.test.costing.assertclass.PhysicalInventoryAssert;
import org.openbravo.test.costing.assertclass.ProductCostingAssert;
import org.openbravo.test.costing.assertclass.ProductTransactionAssert;

public class TestCostingUtils {

  public static void enableAutomaticPriceDifferenceCorrectionPreference() {
    if (enableAutomaticPriceDifferenceCorrectionPreferenceHasNotBeenSet()) {
      setEnableAutomaticPriceDifferenceCorrectionPreference();
    }
  }

  public static void disableAutomaticPriceDifferenceCorrectionPreference() {
    Preference preference = getEnableAutomaticPriceDifferenceCorrectionsPreference();
    if (preference != null) {
      unsetEnableAutomaticPriceDifferenceCorrectionPreference(preference);
    }
  }

  private static void setEnableAutomaticPriceDifferenceCorrectionPreference() {
    Preference preference = OBProvider.getInstance().get(Preference.class);
    preference.setOrganization(TestCostingConstants.ALL_ORGANIZATIONS);
    preference.setProperty(TestCostingConstants.ENABLE_AUTOMATIC_PRICE_CORRECTION_TRXS);
    preference.setSearchKey("Y");
    preference.setVisibleAtClient(null);
    preference.setVisibleAtOrganization(null);
    preference.setVisibleAtRole(null);
    OBDal.getInstance().save(preference);
    OBDal.getInstance().flush();
  }

  private static void unsetEnableAutomaticPriceDifferenceCorrectionPreference(
      Preference preference) {
    OBDal.getInstance().remove(preference);
    OBDal.getInstance().flush();
  }

  private static boolean enableAutomaticPriceDifferenceCorrectionPreferenceHasNotBeenSet() {
    Preference preference = getEnableAutomaticPriceDifferenceCorrectionsPreference();
    return preference == null;
  }

  private static Preference getEnableAutomaticPriceDifferenceCorrectionsPreference() {
    List<Preference> preferenceList = Preferences.getPreferences(
        TestCostingConstants.ENABLE_AUTOMATIC_PRICE_CORRECTION_TRXS, true, null, null, null, null,
        null);
    return (preferenceList.size() == 0) ? null : preferenceList.get(0);
  }

  public static void assertOriginalTotalAndUnitCostOfProductTransaction(Product costingProduct,
      int originalTransactionCost, int totalTransactionCost, int unitTransactionCost) {
    assertTrue(costingProduct.getMaterialMgmtMaterialTransactionList().size() == 1);
    assertTrue(costingProduct.getMaterialMgmtMaterialTransactionList()
        .get(0)
        .getTransactionCost()
        .intValue() == originalTransactionCost);
    assertTrue(costingProduct.getMaterialMgmtMaterialTransactionList()
        .get(0)
        .getTotalCost()
        .intValue() == totalTransactionCost);
    assertTrue(costingProduct.getMaterialMgmtMaterialTransactionList()
        .get(0)
        .getUnitCost()
        .intValue() == unitTransactionCost);
  }

  // Create a Product cloning a created one
  public static Product createProduct(String name, BigDecimal purchasePrice) {
    return createProduct(name, purchasePrice, purchasePrice);
  }

  // Create a Product cloning a created one
  public static Product createProduct(String name, BigDecimal purchasePrice, String currencyId) {
    return createProduct(name, "I", purchasePrice, purchasePrice, null, null, 0, currencyId, null,
        null);
  }

  // Create a Product cloning a created one
  public static Product createProduct(String name, BigDecimal purchasePrice,
      BigDecimal salesPrice) {
    return createProduct(name, "I", purchasePrice, salesPrice, null, null, 0,
        TestCostingConstants.EURO_ID, null, null);
  }

  // Create a Product cloning a created one
  public static Product createProduct(String name, BigDecimal purchasePrice, BigDecimal cost,
      String costType) {
    return createProduct(name, purchasePrice, cost, costType, 0);
  }

  // Create a Product cloning a created one
  public static Product createProduct(String name, BigDecimal purchasePrice, BigDecimal cost,
      String costType, int year) {
    return createProduct(name, "I", purchasePrice, purchasePrice, cost, costType, year,
        TestCostingConstants.EURO_ID, null, null);
  }

  // Create a Product cloning a created one
  public static Product createProduct(String name, String productType, BigDecimal purchasePrice,
      BigDecimal cost, String costType, int year) {
    return createProduct(name, productType, purchasePrice, purchasePrice, cost, costType, year,
        TestCostingConstants.EURO_ID, null, null);
  }

  // Create a Product cloning a created one
  public static Product createProduct(String name, List<Product> productList,
      List<BigDecimal> quantityList) {
    return createProduct(name, null, null, null, null, null, 0, TestCostingConstants.EURO_ID,
        productList, quantityList);
  }

  // Create a Product cloning a created one
  public static Product createProduct(String name, String productType, BigDecimal purchasePrice,
      BigDecimal salesPrice, BigDecimal cost, String costType, int year, String currencyId,
      List<Product> productList, List<BigDecimal> quantityList) {
    List<String> productIdList = new ArrayList<String>();
    if (productList != null) {
      for (Product product : productList) {
        productIdList.add(product.getId());
      }
    }
    return cloneProduct(name, getNumberOfCostingProducts(name) + 1, productType, purchasePrice,
        salesPrice, cost, costType, year, currencyId, productIdList, quantityList);
  }

  // Returns the number of products with name costing Product
  private static int getNumberOfCostingProducts(String name) {
    try {
      final OBCriteria<Product> criteria = OBDal.getInstance().createCriteria(Product.class);
      criteria.addLike(Product.PROPERTY_NAME, name + "-%");
      return criteria.count();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new product cloning costing Product 1
  private static Product cloneProduct(String name, int num, String productType,
      BigDecimal purchasePrice, BigDecimal salesPrice, BigDecimal cost, String costType, int year,
      String currencyId, List<String> productIdList, List<BigDecimal> quantityList) {
    try {
      Product product = OBDal.getInstance()
          .get(Product.class, TestCostingConstants.COSTING_PRODUCT_ID);
      Product productClone = (Product) DalUtil.copy(product, false);
      setGeneralData(productClone);

      productClone.setSearchKey(name + "-" + num);
      productClone.setName(name + "-" + num);
      productClone.setMaterialMgmtMaterialTransactionList(null);
      productClone.setProductType(productType);
      OBDal.getInstance().save(productClone);

      if (productIdList.isEmpty()) {

        StringBuffer where = new StringBuffer();
        where.append(" as pp ");
        where.append(" join pp." + ProductPrice.PROPERTY_PRICELISTVERSION + " as plv");
        where.append(" join plv." + PriceListVersion.PROPERTY_PRICELIST + " as pl");
        where.append(" where pp." + ProductPrice.PROPERTY_PRODUCT + ".id = :productId");
        if (purchasePrice.compareTo(salesPrice) == 0) {
          where.append(" and pl." + PriceList.PROPERTY_SALESPRICELIST + " = false");
        }
        where.append(" order by pl." + PriceList.PROPERTY_NAME);
        OBQuery<ProductPrice> hql = OBDal.getInstance()
            .createQuery(ProductPrice.class, where.toString());
        hql.setNamedParameter("productId", TestCostingConstants.COSTING_PRODUCT_ID);

        int i = 0;
        for (ProductPrice productPrice : hql.list()) {
          ProductPrice productPriceClone = (ProductPrice) DalUtil.copy(productPrice, false);
          setGeneralData(productPriceClone);
          if (i % 2 == 0) {
            if (currencyId.equals(TestCostingConstants.DOLLAR_ID)) {
              productPriceClone.setPriceListVersion(OBDal.getInstance()
                  .get(Product.class, TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID)
                  .getPricingProductPriceList()
                  .get(0)
                  .getPriceListVersion());
            }
            productPriceClone.setStandardPrice(purchasePrice);
            productPriceClone.setListPrice(purchasePrice);
          } else {
            productPriceClone.setStandardPrice(salesPrice);
            productPriceClone.setListPrice(salesPrice);
          }
          productPriceClone.setProduct(productClone);
          OBDal.getInstance().save(productPriceClone);
          productClone.getPricingProductPriceList().add(productPriceClone);
          i++;
        }

        if (cost != null) {
          Costing productCosting = OBProvider.getInstance().get(Costing.class);
          setGeneralData(productCosting);
          if (year != 0) {
            productCosting.setStartingDate(DateUtils.addYears(product.getPricingProductPriceList()
                .get(0)
                .getPriceListVersion()
                .getValidFromDate(), year));
          } else {
            productCosting.setStartingDate(new Date());
          }
          Calendar calendar = Calendar.getInstance();
          calendar.set(9999, 11, 31);
          productCosting.setEndingDate(calendar.getTime());
          productCosting.setManual(true);
          productCosting.setCostType(costType);
          productCosting.setCost(cost);
          productCosting
              .setCurrency(OBDal.getInstance().get(Currency.class, TestCostingConstants.EURO_ID));
          productCosting.setWarehouse(
              OBDal.getInstance().get(Warehouse.class, TestCostingConstants.SPAIN_WAREHOUSE_ID));
          productCosting.setProduct(productClone);
          productClone.getMaterialMgmtCostingList().add(productCosting);
        }
      }

      else {
        productClone.setBillOfMaterials(true);
        int i = 0;
        for (String productBOMId : productIdList) {
          ProductBOM productBOMClone = OBProvider.getInstance().get(ProductBOM.class);
          setGeneralData(productBOMClone);
          productBOMClone.setLineNo((i + 1) * 10L);
          productBOMClone.setProduct(productClone);
          productBOMClone.setBOMProduct(OBDal.getInstance().get(Product.class, productBOMId));
          productBOMClone.setBOMQuantity(quantityList.get(i));
          i++;

          OBDal.getInstance().save(productBOMClone);
          OBDal.getInstance().flush();
          OBDal.getInstance().refresh(productBOMClone);
        }
        OBDal.getInstance().save(productClone);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(productClone);

        verifyBOM(productClone.getId());
        productClone.setBOMVerified(true);
      }

      OBDal.getInstance().save(productClone);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(productClone);

      OBCriteria<ProductAccounts> criteria = OBDal.getInstance()
          .createCriteria(ProductAccounts.class);
      criteria.addEqual(ProductAccounts.PROPERTY_PRODUCT, product);
      criteria.addIsNotNull(ProductAccounts.PROPERTY_INVOICEPRICEVARIANCE);
      criteria.setMaxResults(1);
      productClone.getProductAccountsList()
          .get(0)
          .setInvoicePriceVariance(
              ((ProductAccounts) criteria.uniqueResult()).getInvoicePriceVariance());

      OBDal.getInstance().save(productClone);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(productClone);

      return productClone;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new product cloning costing Product 1
  public static Product addProductPriceCost(String name, String productType,
      BigDecimal purchasePrice, BigDecimal salesPrice, BigDecimal cost, String costType, int year,
      String currencyId) {
    try {
      int num = TestCostingUtils.getNumberOfCostingProducts(name);
      Product product = OBDal.getInstance()
          .get(Product.class, TestCostingConstants.COSTING_PRODUCT_ID);
      Product productClone = (Product) DalUtil.copy(product, false);
      setGeneralData(productClone);

      productClone.setSearchKey(name + "-" + num);
      productClone.setName(name + "-" + num);
      productClone.setMaterialMgmtMaterialTransactionList(null);
      productClone.setProductType(productType);
      OBDal.getInstance().save(productClone);

      StringBuffer where = new StringBuffer();
      where.append(" as pp ");
      where.append(" join pp." + ProductPrice.PROPERTY_PRICELISTVERSION + " as plv");
      where.append(" join plv." + PriceListVersion.PROPERTY_PRICELIST + " as pl");
      where.append(" where pp." + ProductPrice.PROPERTY_PRODUCT + ".id = :productId");
      where.append(" order by pl." + PriceList.PROPERTY_NAME);
      OBQuery<ProductPrice> hql = OBDal.getInstance()
          .createQuery(ProductPrice.class, where.toString());
      hql.setNamedParameter("productId", TestCostingConstants.COSTING_PRODUCT_ID);

      for (ProductPrice productPrice : hql.list()) {
        if (productPrice.getPriceListVersion().getPriceList().isSalesPriceList()
            && salesPrice != null) {
          ProductPrice productPriceClone = (ProductPrice) DalUtil.copy(productPrice, false);
          setGeneralData(productPriceClone);
          productPriceClone.setStandardPrice(salesPrice);
          productPriceClone.setListPrice(salesPrice);
          productPriceClone.setProduct(productClone);
          OBDal.getInstance().save(productPriceClone);
          productClone.getPricingProductPriceList().add(productPriceClone);
        } else if (!productPrice.getPriceListVersion().getPriceList().isSalesPriceList()
            && purchasePrice != null) {
          ProductPrice productPriceClone = (ProductPrice) DalUtil.copy(productPrice, false);
          setGeneralData(productPriceClone);
          productPriceClone.setStandardPrice(purchasePrice);
          productPriceClone.setListPrice(purchasePrice);
          productPriceClone.setProduct(productClone);
          OBDal.getInstance().save(productPriceClone);
          productClone.getPricingProductPriceList().add(productPriceClone);
        }
      }

      if (cost != null) {
        Costing productCosting = OBProvider.getInstance().get(Costing.class);
        setGeneralData(productCosting);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2000, 01, 01);
        productCosting.setStartingDate(calendar.getTime());
        calendar = Calendar.getInstance();
        calendar.set(9999, 11, 31);
        productCosting.setEndingDate(calendar.getTime());
        productCosting.setManual(true);
        // productCosting.setPermanent(true);
        productCosting.setCostType(costType);
        productCosting.setCost(cost);
        productCosting
            .setCurrency(OBDal.getInstance().get(Currency.class, TestCostingConstants.EURO_ID));
        productCosting.setProduct(productClone);
        productCosting.setWarehouse(
            OBDal.getInstance().get(Warehouse.class, TestCostingConstants.SPAIN_EAST_WAREHOUSE_ID));
        productClone.getMaterialMgmtCostingList().add(productCosting);
      }
      OBDal.getInstance().save(productClone);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(productClone);

      return productClone;
    } catch (

    Exception e) {
      throw new OBException(e);
    }
  }

  public static Product addBOMProducts(Product product, List<Product> productList,
      List<BigDecimal> quantityList) {
    List<String> productIdList = new ArrayList<String>();
    if (productList != null) {
      for (Product raw_product : productList) {
        productIdList.add(raw_product.getId());
      }
    }
    Product productClone = product;
    try {
      productClone.setBillOfMaterials(true);
      int i = 0;
      for (String productBOMId : productIdList) {
        ProductBOM productBOMClone = OBProvider.getInstance().get(ProductBOM.class);
        setGeneralData(productBOMClone);
        productBOMClone.setLineNo((i + 1) * 10L);
        productBOMClone.setProduct(productClone);
        productBOMClone.setBOMProduct(OBDal.getInstance().get(Product.class, productBOMId));
        productBOMClone.setBOMQuantity(quantityList.get(i));
        i++;

        OBDal.getInstance().save(productBOMClone);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(productBOMClone);
      }
      OBDal.getInstance().save(productClone);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(productClone);

      verifyBOM(productClone.getId());
      productClone.setBOMVerified(true);

      OBDal.getInstance().save(productClone);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(productClone);
    } catch (Exception e) {
      throw new OBException(e);
    }
    return productClone;
  }

  // Set common fields in all tables
  public static void setGeneralData(BaseOBObject document) {
    try {
      document.set("client",
          OBDal.getInstance().get(Client.class, TestCostingConstants.QATESTING_CLIENT_ID));
      document.set("organization",
          OBDal.getInstance().get(Organization.class, TestCostingConstants.SPAIN_ORGANIZATION_ID));
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Run Verify BOM process
  private static void verifyBOM(String productId) {
    try {
      OBDal.getInstance().commitAndClose();
      VariablesSecureApp vars = null;
      vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getCurrentClient().getId(),
          OBContext.getOBContext().getCurrentOrganization().getId(),
          OBContext.getOBContext().getRole().getId(),
          OBContext.getOBContext().getLanguage().getLanguage());
      ConnectionProvider conn = new DalConnectionProvider(true);
      ProcessBundle pb = new ProcessBundle(TestCostingConstants.VERIFYBOM_PROCESS_ID, vars)
          .init(conn);
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("M_Product_ID", productId);
      pb.setParams(parameters);
      new VerifyBOM().execute(pb);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Order cloning a created one and book it
  public static Order createPurchaseOrder(Product product, BigDecimal price, BigDecimal quantity,
      int day) {
    try {
      Order purchaseOrder = cloneOrder(product.getId(), false, price, quantity, day);
      bookOrder(purchaseOrder);
      return purchaseOrder;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Sales Order cloning a created one and book it
  public static Order createSalesOrder(Product product, BigDecimal price, BigDecimal quantity,
      int day) {
    try {
      Order salesOrder = cloneOrder(product.getId(), true, price, quantity, day);
      bookOrder(salesOrder);
      return salesOrder;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Reactivate purchase order, update product price and book it
  public static void updatePurchaseOrder(Order order, BigDecimal price) {
    try {
      Order purchaseOrder = reactivateOrder(order);
      purchaseOrder = updateOrderProductPrice(purchaseOrder, price);
      bookOrder(purchaseOrder);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Invoice cloning a created one, complete it and post it
  public static Invoice createPurchaseInvoice(Product product, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      Invoice purchaseInvoice = cloneInvoice(product.getId(), false, price, quantity, day);
      return postPurchaseInvoice(purchaseInvoice, product.getId(), price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Invoice from a purchase order, complete it and post it
  public static Invoice createPurchaseInvoice(Order purchaseOrder, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      Invoice purchaseInvoice = createInvoiceFromOrder(purchaseOrder.getId(), false, price,
          quantity, day);
      String productId = purchaseInvoice.getInvoiceLineList().get(0).getProduct().getId();
      return postPurchaseInvoice(purchaseInvoice, productId, price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Invoice from a goods receipt, complete it and post it
  public static Invoice createPurchaseInvoice(ShipmentInOut goodsReceipt, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return createPurchaseInvoice(goodsReceipt, price, quantity, null, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Invoice from a goods receipt, complete it and post it
  public static Invoice createPurchaseInvoice(ShipmentInOut goodsReceipt, BigDecimal price,
      BigDecimal quantity, BigDecimal conversion, int day) {
    try {
      Invoice purchaseInvoice = createInvoiceFromMovement(goodsReceipt.getId(), false, price,
          quantity, day);
      if (conversion != null) {
        createConversion(purchaseInvoice, conversion);
      }
      String productId = purchaseInvoice.getInvoiceLineList().get(0).getProduct().getId();
      return postPurchaseInvoice(purchaseInvoice, productId, price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Invoice from many purchase orders, complete it and post it
  public static Invoice createPurchaseInvoice(List<Order> purchaseOrderList,
      List<BigDecimal> priceList, List<BigDecimal> quantityList, int day) {
    try {
      List<String> purchaseOrderIdList = new ArrayList<String>();
      for (Order purchaseOrder : purchaseOrderList) {
        purchaseOrderIdList.add(purchaseOrder.getId());
      }

      Invoice purchaseInvoice = createInvoiceFromOrders(purchaseOrderIdList, false, priceList,
          quantityList, day);
      String productId = purchaseInvoice.getInvoiceLineList().get(0).getProduct().getId();
      return postPurchaseInvoice(purchaseInvoice, productId,
          getAveragePrice(priceList, quantityList), getTotalQuantity(quantityList));
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Invoice from many goods receipts, complete it and post it
  public static Invoice createPurchaseInvoice(List<ShipmentInOut> goodsReceiptList,
      List<BigDecimal> priceList, BigDecimal quantity, int day) {
    try {
      List<String> goodsReceipIdtList = new ArrayList<String>();
      List<BigDecimal> quantityList = new ArrayList<BigDecimal>();
      for (ShipmentInOut goodsReceipt : goodsReceiptList) {
        goodsReceipIdtList.add(goodsReceipt.getId());
        quantityList
            .add(goodsReceipt.getMaterialMgmtShipmentInOutLineList().get(0).getMovementQuantity());
      }
      Invoice purchaseInvoice = createInvoiceFromMovements(goodsReceipIdtList, false, priceList,
          quantity, day);
      String productId = purchaseInvoice.getInvoiceLineList().get(0).getProduct().getId();

      return postPurchaseInvoice(purchaseInvoice, productId,
          getAveragePrice(priceList, quantityList), quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Reactivate purchase order, update product price and book it
  public static void updatePurchaseInvoice(Invoice invoice, BigDecimal price) {
    try {
      Invoice purchaseInvoice = reactivateInvoice(invoice);
      purchaseInvoice = updateInvoiceProductPrice(purchaseInvoice, price);
      InvoiceLine purchaseInvoiceLine = purchaseInvoice.getInvoiceLineList().get(0);
      String productId = purchaseInvoiceLine.getProduct().getId();
      postPurchaseInvoice(purchaseInvoice, productId, price,
          purchaseInvoiceLine.getInvoicedQuantity(), false);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Complete a Purchase Invoice and post it
  public static Invoice postPurchaseInvoice(Invoice purchaseInvoice, String productId,
      BigDecimal price, BigDecimal quantity) {
    try {
      return postPurchaseInvoice(purchaseInvoice, productId, price, quantity, true);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Complete a Purchase Invoice and post it
  public static Invoice postPurchaseInvoice(Invoice purchaseInvoice, String productId,
      BigDecimal price, BigDecimal quantity, boolean assertMatchedInvoice) {
    try {
      completeDocument(purchaseInvoice);
      OBDal.getInstance().commitAndClose();
      postDocument(purchaseInvoice);
      Invoice invoice = OBDal.getInstance().get(Invoice.class, purchaseInvoice.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList
          .add(new DocumentPostAssert("40000", BigDecimal.ZERO, quantity.multiply(price), null));
      for (InvoiceLine purchaseInvoiceLine : invoice.getInvoiceLineList()) {
        documentPostAssertList
            .add(new DocumentPostAssert(purchaseInvoiceLine.getProduct().getId(), "60000",
                purchaseInvoiceLine.getInvoicedQuantity()
                    .multiply(purchaseInvoiceLine.getUnitPrice()),
                BigDecimal.ZERO, purchaseInvoiceLine.getInvoicedQuantity()));
      }
      assertDocumentPost(invoice, null, documentPostAssertList);

      if (invoice.getInvoiceLineList().get(0).getGoodsShipmentLine() != null
          && assertMatchedInvoice) {
        for (InvoiceLine purchaseInvoiceLine : invoice.getInvoiceLineList()) {
          purchaseInvoiceLine = OBDal.getInstance()
              .get(InvoiceLine.class, purchaseInvoiceLine.getId());
          postMatchedPurchaseInvoice(purchaseInvoiceLine,
              purchaseInvoiceLine.getGoodsShipmentLine());
        }
      }
      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt cloning a created one, complete it and post it
  public static ShipmentInOut createGoodsReceipt(Product product, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return createGoodsReceipt(product, price, quantity, TestCostingConstants.LOCATOR_L01_ID, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt cloning a created one, complete it and post it
  public static ShipmentInOut createGoodsReceipt(Product product, BigDecimal price,
      BigDecimal quantity, String locatorId, int day) {
    try {
      ShipmentInOut goodsReceipt = cloneMovement(product.getId(), false, quantity, locatorId, day);
      return postGoodsReceipt(goodsReceipt, product.getId(), price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt from a purchase order, complete it and post it
  public static ShipmentInOut createGoodsReceipt(Order purchaseOrder, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return createGoodsReceipt(purchaseOrder, price, quantity, TestCostingConstants.LOCATOR_L01_ID,
          day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt from a purchase order, complete it and post it
  public static ShipmentInOut createGoodsReceipt(Order purchaseOrder, BigDecimal price,
      BigDecimal quantity, String locatorId, int day) {
    try {
      ShipmentInOut goodsReceipt = createMovementFromOrder(purchaseOrder.getId(), false, quantity,
          locatorId, day);
      String productId = goodsReceipt.getMaterialMgmtShipmentInOutLineList()
          .get(0)
          .getProduct()
          .getId();
      return postGoodsReceipt(goodsReceipt, productId, price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt from many purchase orders, complete it and post it
  public static ShipmentInOut createGoodsReceipt(List<Order> purchaseOrderList, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return createGoodsReceipt(purchaseOrderList, price, quantity,
          TestCostingConstants.LOCATOR_L01_ID, day, null);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt from many purchase orders, complete it and post it
  public static ShipmentInOut createGoodsReceipt(List<Order> purchaseOrderList, BigDecimal price,
      BigDecimal quantity, int day, List<Invoice> invoiceList) {
    try {
      return createGoodsReceipt(purchaseOrderList, price, quantity,
          TestCostingConstants.LOCATOR_L01_ID, day, invoiceList);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt from many purchase orders, complete it and post it
  public static ShipmentInOut createGoodsReceipt(List<Order> purchaseOrderList, BigDecimal price,
      BigDecimal quantity, String locatorId, int day, List<Invoice> invoiceList) {
    try {
      List<String> purchaseOrderIdList = new ArrayList<String>();
      for (Order purchaseOrder : purchaseOrderList) {
        purchaseOrderIdList.add(purchaseOrder.getId());
      }

      ShipmentInOut goodsReceipt = createMovementFromOrders(purchaseOrderIdList, false, quantity,
          locatorId, day);
      String productId = goodsReceipt.getMaterialMgmtShipmentInOutLineList()
          .get(0)
          .getProduct()
          .getId();
      if (invoiceList != null) {
        createLandedCostCost(invoiceList, goodsReceipt);
      }
      return postGoodsReceipt(goodsReceipt, productId, price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt from a purchase invoice, complete it and post it
  public static ShipmentInOut createGoodsReceipt(Invoice purchaseInvoice, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return createGoodsReceipt(purchaseInvoice, price, quantity,
          TestCostingConstants.LOCATOR_L01_ID, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt from a purchase invoice, complete it and post it
  public static ShipmentInOut createGoodsReceipt(Invoice purchaseInvoice, BigDecimal price,
      BigDecimal quantity, String locatorId, int day) {
    try {
      ShipmentInOut goodsReceipt = createMovementFromInvoice(purchaseInvoice.getId(), false,
          quantity, locatorId, day);
      String productId = goodsReceipt.getMaterialMgmtShipmentInOutLineList()
          .get(0)
          .getProduct()
          .getId();
      return postGoodsReceipt(goodsReceipt, productId, price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Complete a Goods Receipt and post it
  public static ShipmentInOut postGoodsReceipt(ShipmentInOut goodsReceipt, String productId,
      BigDecimal price, BigDecimal quantity) {
    try {
      completeDocument(goodsReceipt);
      runCostingBackground();
      ShipmentInOut receipt = OBDal.getInstance().get(ShipmentInOut.class, goodsReceipt.getId());
      postDocument(receipt);
      receipt = OBDal.getInstance().get(ShipmentInOut.class, goodsReceipt.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      for (ShipmentInOutLine goodsReceiptLine : receipt.getMaterialMgmtShipmentInOutLineList()) {
        if (receipt.getMaterialMgmtShipmentInOutLineList().size() == 1) {
          documentPostAssertList.add(new DocumentPostAssert(goodsReceiptLine.getProduct().getId(),
              "35000", goodsReceiptLine.getMovementQuantity().multiply(price), BigDecimal.ZERO,
              goodsReceiptLine.getMovementQuantity()));
          documentPostAssertList.add(new DocumentPostAssert(goodsReceiptLine.getProduct().getId(),
              "40090", BigDecimal.ZERO, goodsReceiptLine.getMovementQuantity().multiply(price),
              goodsReceiptLine.getMovementQuantity()));
        } else {
          documentPostAssertList
              .add(new DocumentPostAssert(goodsReceiptLine.getProduct().getId(), "35000",
                  goodsReceiptLine.getMovementQuantity()
                      .multiply(goodsReceiptLine.getSalesOrderLine().getUnitPrice()),
                  BigDecimal.ZERO, goodsReceiptLine.getMovementQuantity()));
          documentPostAssertList.add(new DocumentPostAssert(goodsReceiptLine.getProduct().getId(),
              "40090", BigDecimal.ZERO,
              goodsReceiptLine.getMovementQuantity()
                  .multiply(goodsReceiptLine.getSalesOrderLine().getUnitPrice()),
              goodsReceiptLine.getMovementQuantity()));
        }
      }
      assertDocumentPost(receipt, null, documentPostAssertList);

      if (receipt.getInvoice() != null) {
        int i = 0;
        for (InvoiceLine purchaseInvoiceLine : receipt.getInvoice().getInvoiceLineList()) {
          postMatchedPurchaseInvoice(purchaseInvoiceLine,
              receipt.getMaterialMgmtShipmentInOutLineList().get(i));
          i++;
        }
      }
      return receipt;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert and post Purchase Invoice Matched Invoices
  public static void postMatchedPurchaseInvoice(InvoiceLine purchaseInvoiceLine,
      ShipmentInOutLine goodsReceiptLine) {
    try {
      OBCriteria<ReceiptInvoiceMatch> criteria1 = OBDal.getInstance()
          .createCriteria(ReceiptInvoiceMatch.class);
      criteria1.addEqual(ReceiptInvoiceMatch.PROPERTY_INVOICELINE, purchaseInvoiceLine);
      criteria1
          .addEqual(ReceiptInvoiceMatch.PROPERTY_GOODSSHIPMENTLINE, goodsReceiptLine);
      criteria1.setMaxResults(1);
      ReceiptInvoiceMatch receiptInvoiceMatch = (ReceiptInvoiceMatch) criteria1.uniqueResult();
      assertMatchedInvoice(receiptInvoiceMatch,
          new MatchedInvoicesAssert(purchaseInvoiceLine, goodsReceiptLine));

      postDocument(receiptInvoiceMatch);
      receiptInvoiceMatch = OBDal.getInstance()
          .get(ReceiptInvoiceMatch.class, receiptInvoiceMatch.getId());

      BigDecimal invoicePrice = OBDal.getInstance()
          .get(Invoice.class, purchaseInvoiceLine.getInvoice().getId())
          .getCurrencyConversionRateDocList()
          .size() == 0
              ? purchaseInvoiceLine.getUnitPrice()
              : purchaseInvoiceLine.getUnitPrice()
                  .multiply(OBDal.getInstance()
                      .get(Invoice.class, purchaseInvoiceLine.getInvoice().getId())
                      .getCurrencyConversionRateDocList()
                      .get(0)
                      .getRate());
      OBCriteria<AccountingFact> criteria2 = OBDal.getInstance()
          .createCriteria(AccountingFact.class);
      criteria2.addEqual(AccountingFact.PROPERTY_RECORDID,
          goodsReceiptLine.getShipmentReceipt().getId());
      criteria2.addEqual(AccountingFact.PROPERTY_LINEID, goodsReceiptLine.getId());
      criteria2.addOrderBy(AccountingFact.PROPERTY_SEQUENCENUMBER, true);
      criteria2.setMaxResults(1);
      BigDecimal receiptPrice = ((AccountingFact) criteria2.uniqueResult()).getDebit()
          .divide(receiptInvoiceMatch.getQuantity());

      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(
          new DocumentPostAssert("40090", receiptPrice.multiply(receiptInvoiceMatch.getQuantity()),
              BigDecimal.ZERO, goodsReceiptLine.getMovementQuantity()));
      documentPostAssertList.add(new DocumentPostAssert("60000", BigDecimal.ZERO,
          invoicePrice.multiply(receiptInvoiceMatch.getQuantity()),
          purchaseInvoiceLine.getInvoicedQuantity()));
      if (!invoicePrice.equals(receiptPrice)) {
        if (invoicePrice.compareTo(receiptPrice) > 0) {
          documentPostAssertList.add(new DocumentPostAssert("99904",
              invoicePrice.multiply(receiptInvoiceMatch.getQuantity())
                  .add(receiptPrice.multiply(receiptInvoiceMatch.getQuantity()).negate()),
              BigDecimal.ZERO, goodsReceiptLine.getMovementQuantity()));
        } else {
          documentPostAssertList.add(new DocumentPostAssert("99904", BigDecimal.ZERO,
              receiptPrice.multiply(receiptInvoiceMatch.getQuantity())
                  .add(invoicePrice.multiply(receiptInvoiceMatch.getQuantity()).negate()),
              goodsReceiptLine.getMovementQuantity()));
        }
      }
      assertDocumentPost(receiptInvoiceMatch, purchaseInvoiceLine.getProduct().getId(),
          documentPostAssertList);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Shipment cloning a created one, complete it and post it
  public static ShipmentInOut createGoodsShipment(Product product, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return createGoodsShipment(product, price, quantity, TestCostingConstants.LOCATOR_L01_ID,
          day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Shipment cloning a created one, complete it and post it
  public static ShipmentInOut createGoodsShipment(Product product, BigDecimal price,
      BigDecimal quantity, String locatorId, int day) {
    try {
      ShipmentInOut goodsShipment = cloneMovement(product.getId(), true, quantity, locatorId, day);
      return postGoodsShipment(goodsShipment, product.getId(), price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Shipment from a sales order, complete it and post it
  public static ShipmentInOut createGoodsShipment(Order salesOrder, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return createGoodsShipment(salesOrder, price, quantity, TestCostingConstants.LOCATOR_L01_ID,
          day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Shipment from a sales order, complete it and post it
  private static ShipmentInOut createGoodsShipment(Order salesOrder, BigDecimal price,
      BigDecimal quantity, String locatorId, int day) {
    try {
      ShipmentInOut goodsShipment = createMovementFromOrder(salesOrder.getId(), true, quantity,
          locatorId, day);
      String productId = goodsShipment.getMaterialMgmtShipmentInOutLineList()
          .get(0)
          .getProduct()
          .getId();
      return postGoodsShipment(goodsShipment, productId, price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Complete a Goods Receipt and post it
  public static ShipmentInOut postGoodsShipment(ShipmentInOut goodsShipment, String productId,
      BigDecimal price, BigDecimal quantity) {
    try {
      completeDocument(goodsShipment);
      runCostingBackground();
      ShipmentInOut shipment = OBDal.getInstance().get(ShipmentInOut.class, goodsShipment.getId());
      postDocument(shipment);
      shipment = OBDal.getInstance().get(ShipmentInOut.class, goodsShipment.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      for (ShipmentInOutLine goodsShipmentLine : shipment.getMaterialMgmtShipmentInOutLineList()) {
        if (shipment.getMaterialMgmtShipmentInOutLineList().size() == 1) {
          documentPostAssertList.add(new DocumentPostAssert("99900",
              goodsShipmentLine.getMovementQuantity().multiply(price), BigDecimal.ZERO,
              goodsShipmentLine.getMovementQuantity()));
          documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
              goodsShipmentLine.getMovementQuantity().multiply(price),
              goodsShipmentLine.getMovementQuantity()));
        } else {
          documentPostAssertList.add(new DocumentPostAssert("99900",
              goodsShipmentLine.getMovementQuantity()
                  .multiply(goodsShipmentLine.getSalesOrderLine().getUnitPrice()),
              BigDecimal.ZERO, goodsShipmentLine.getMovementQuantity()));
          documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
              goodsShipmentLine.getMovementQuantity()
                  .multiply(goodsShipmentLine.getSalesOrderLine().getUnitPrice()),
              goodsShipmentLine.getMovementQuantity()));
        }
      }
      assertDocumentPost(shipment, productId, documentPostAssertList);
      return shipment;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Cancel Goods Receipt
  public static ShipmentInOut cancelGoodsReceipt(ShipmentInOut goodsReceipt, BigDecimal price) {
    try {
      ShipmentInOut gReceipt = OBDal.getInstance().get(ShipmentInOut.class, goodsReceipt.getId());
      gReceipt.setDocumentAction("RC");
      OBDal.getInstance().save(gReceipt);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      gReceipt = OBDal.getInstance().get(ShipmentInOut.class, gReceipt.getId());
      OBDal.getInstance().refresh(gReceipt);
      gReceipt = (ShipmentInOut) completeDocument(gReceipt);
      OBDal.getInstance().refresh(gReceipt);

      runCostingBackground();
      ShipmentInOut receipt = OBDal.getInstance()
          .get(ShipmentInOut.class, gReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0)
          .getMaterialMgmtShipmentInOutLineCanceledInoutLineList()
          .get(0)
          .getShipmentReceipt();
      String productId = receipt.getMaterialMgmtShipmentInOutLineList().get(0).getProduct().getId();

      postDocument(receipt);
      receipt = OBDal.getInstance().get(ShipmentInOut.class, receipt.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      for (ShipmentInOutLine goodsReceiptLine : receipt.getMaterialMgmtShipmentInOutLineList()) {
        documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
            goodsReceiptLine.getMovementQuantity().negate().multiply(price),
            goodsReceiptLine.getMovementQuantity()));
        documentPostAssertList.add(new DocumentPostAssert("40090",
            goodsReceiptLine.getMovementQuantity().negate().multiply(price), BigDecimal.ZERO,
            goodsReceiptLine.getMovementQuantity()));
      }
      assertDocumentPost(receipt, productId, documentPostAssertList);
      return receipt;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Cancel Goods Shipment
  public static ShipmentInOut cancelGoodsShipment(ShipmentInOut goodsShipment, BigDecimal price) {
    try {
      ShipmentInOut gShipment = OBDal.getInstance().get(ShipmentInOut.class, goodsShipment.getId());
      gShipment.setDocumentAction("RC");
      OBDal.getInstance().save(gShipment);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      gShipment = OBDal.getInstance().get(ShipmentInOut.class, gShipment.getId());
      OBDal.getInstance().refresh(gShipment);
      gShipment = (ShipmentInOut) completeDocument(gShipment);
      OBDal.getInstance().refresh(gShipment);

      runCostingBackground();
      ShipmentInOut shipment = OBDal.getInstance()
          .get(ShipmentInOut.class, gShipment.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0)
          .getMaterialMgmtShipmentInOutLineCanceledInoutLineList()
          .get(0)
          .getShipmentReceipt();
      String productId = shipment.getMaterialMgmtShipmentInOutLineList()
          .get(0)
          .getProduct()
          .getId();

      postDocument(shipment);
      shipment = OBDal.getInstance().get(ShipmentInOut.class, shipment.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      for (ShipmentInOutLine goodsShipmentLine : shipment.getMaterialMgmtShipmentInOutLineList()) {
        documentPostAssertList.add(new DocumentPostAssert("99900", BigDecimal.ZERO,
            goodsShipmentLine.getMovementQuantity().negate().multiply(price),
            goodsShipmentLine.getMovementQuantity()));
        documentPostAssertList.add(new DocumentPostAssert("35000",
            goodsShipmentLine.getMovementQuantity().negate().multiply(price), BigDecimal.ZERO,
            goodsShipmentLine.getMovementQuantity()));
      }
      assertDocumentPost(shipment, productId, documentPostAssertList);
      return shipment;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Movement, complete it and post it
  public static InternalMovement createGoodsMovement(Product product, BigDecimal price,
      BigDecimal quantity, String locatorFromId, String locatorToId, int day) {
    try {
      InternalMovement goodsMovement = createGoodsMovement(product.getId(), quantity, locatorFromId,
          locatorToId, day);
      OBDal.getInstance().commitAndClose();
      completeDocument(goodsMovement, TestCostingConstants.PROCESSMOVEMENT_PROCESS_ID);
      goodsMovement.setProcessed(true);
      runCostingBackground();
      InternalMovement movement = OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId());
      postDocument(movement);
      movement = OBDal.getInstance().get(InternalMovement.class, goodsMovement.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(
          new DocumentPostAssert("35000", BigDecimal.ZERO, quantity.multiply(price), quantity));
      documentPostAssertList.add(
          new DocumentPostAssert("35000", quantity.multiply(price), BigDecimal.ZERO, quantity));
      assertDocumentPost(movement, product.getId(), documentPostAssertList);
      return movement;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Internal Consumption, complete it and post it
  public static InternalConsumption createInternalConsumption(Product product, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return createInternalConsumption(product, price, quantity,
          TestCostingConstants.LOCATOR_L01_ID, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Internal Consumption, complete it and post it
  public static InternalConsumption createInternalConsumption(Product product, BigDecimal price,
      BigDecimal quantity, String locatorId, int day) {
    try {
      InternalConsumption internalConsumption = createInternalConsumption(product.getId(), quantity,
          locatorId, day);
      OBDal.getInstance().commitAndClose();
      completeDocument(internalConsumption, TestCostingConstants.PROCESSCONSUMPTION_PROCESS_ID);
      internalConsumption.setProcessed(true);
      internalConsumption.setStatus("CO");
      runCostingBackground();
      InternalConsumption consumption = OBDal.getInstance()
          .get(InternalConsumption.class, internalConsumption.getId());
      postDocument(consumption);
      consumption = OBDal.getInstance().get(InternalConsumption.class, consumption.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(
          new DocumentPostAssert("99900", quantity.multiply(price), BigDecimal.ZERO, quantity));
      documentPostAssertList.add(
          new DocumentPostAssert("35000", BigDecimal.ZERO, quantity.multiply(price), quantity));
      assertDocumentPost(consumption, product.getId(), documentPostAssertList);
      return consumption;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Cancel a internal consumption
  public static InternalConsumption cancelInternalConsumption(
      InternalConsumption internalConsumption) {
    try {
      cancelInternalConsumption(internalConsumption.getId());
      internalConsumption.setStatus("VO");
      runCostingBackground();
      return internalConsumption;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Inventory Amount Update and process it
  public static InventoryAmountUpdate createInventoryAmountUpdate(Product product,
      BigDecimal originalPrice, BigDecimal finalPrice, BigDecimal quantity, int day) {
    try {
      return createInventoryAmountUpdate(product, originalPrice, originalPrice, finalPrice,
          quantity, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Inventory Amount Update and process it
  public static InventoryAmountUpdate createInventoryAmountUpdate(Product product, BigDecimal cost,
      BigDecimal originalPrice, BigDecimal finalPrice, BigDecimal quantity, int day) {
    try {
      InventoryAmountUpdate inventoryAmountUpdate = createInventoryAmountUpdate(product.getId(),
          originalPrice, finalPrice, quantity, day);
      processInventoryAmountUpdate(inventoryAmountUpdate.getId());
      runCostingBackground();

      List<InventoryCount> inventoryCountList = getPhysicalInventory(inventoryAmountUpdate.getId());
      assertPhysicalInventory(inventoryCountList,
          new PhysicalInventoryAssert(product, finalPrice, quantity, day));

      postDocument(inventoryCountList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          quantity.multiply(cost), quantity.negate()));
      documentPostAssertList1.add(new DocumentPostAssert("61000", quantity.multiply(cost),
          BigDecimal.ZERO, quantity.negate()));
      assertDocumentPost(inventoryCountList.get(0), product.getId(), documentPostAssertList1);
      postDocument(inventoryCountList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("35000", quantity.multiply(finalPrice),
          BigDecimal.ZERO, quantity));
      documentPostAssertList2.add(new DocumentPostAssert("61000", BigDecimal.ZERO,
          quantity.multiply(finalPrice), quantity));
      assertDocumentPost(inventoryCountList.get(1), product.getId(), documentPostAssertList2);
      return inventoryAmountUpdate;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Inventory Amount Update and process it
  public static ProductionTransaction createBillOfMaterialsProduction(Product product,
      BigDecimal quantity, String locatorId, int day) {
    return createBillOfMaterialsProduction(product, quantity, locatorId, day, true, false);
  }

  // Create a Inventory Amount Update and process it
  public static ProductionTransaction createBillOfMaterialsProduction(Product product,
      BigDecimal quantity, String locatorId, int day, boolean sortProductionLine,
      boolean orderByLineNo) {
    try {
      ProductionTransaction billOfMaterialsProduction = createBillOfMaterialsProduction(
          product.getId(), quantity, locatorId, day);
      processBillOfMaterialsProduction(billOfMaterialsProduction);
      billOfMaterialsProduction.setRecordsCreated(true);
      OBDal.getInstance().save(billOfMaterialsProduction);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(billOfMaterialsProduction);
      OBDal.getInstance().commitAndClose();
      processBillOfMaterialsProduction(billOfMaterialsProduction);
      billOfMaterialsProduction.setProcessed(true);
      OBDal.getInstance().save(billOfMaterialsProduction);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(billOfMaterialsProduction);
      OBDal.getInstance().commitAndClose();
      runCostingBackground();

      ProductionTransaction bMaterialsProduction = OBDal.getInstance()
          .get(ProductionTransaction.class, billOfMaterialsProduction.getId());
      postDocument(bMaterialsProduction);
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      List<ProductionLine> productionLinesList = getProductionLines(
          billOfMaterialsProduction.getId(), orderByLineNo);
      if (sortProductionLine) {
        productionLinesList.add(0, productionLinesList.get(productionLinesList.size() - 1));
        productionLinesList.remove(productionLinesList.size() - 1);

        final OBCriteria<AccountingFact> criteria1 = OBDal.getInstance()
            .createCriteria(AccountingFact.class);
        criteria1.addEqual(AccountingFact.PROPERTY_RECORDID, billOfMaterialsProduction.getId());
        criteria1.addOrderBy(AccountingFact.PROPERTY_SEQUENCENUMBER, true);

        if (!criteria1.list()
            .get(2)
            .getQuantity()
            .equals(productionLinesList.get(1).getMovementQuantity())) {
          productionLinesList.add(1, productionLinesList.get(productionLinesList.size() - 1));
          productionLinesList.remove(productionLinesList.size() - 1);
        }
      }
      int i = 0;
      for (ProductionLine productionLine : productionLinesList) {
        BigDecimal amountTotal = BigDecimal.ZERO;

        if (i == 0) {
          OBCriteria<ProductBOM> criteria2 = OBDal.getInstance().createCriteria(ProductBOM.class);
          criteria2.addEqual(ProductBOM.PROPERTY_PRODUCT, productionLine.getProduct());
          for (ProductBOM productBOM : criteria2.list()) {
            amountTotal = amountTotal.add(productBOM.getBOMQuantity()
                .multiply(productBOM.getBOMProduct()
                    .getPricingProductPriceList()
                    .get(0)
                    .getStandardPrice()));
          }
          amountTotal = amountTotal.multiply(productionLine.getMovementQuantity());
          documentPostAssertList1.add(new DocumentPostAssert(productionLine.getProduct().getId(),
              "35000", amountTotal, BigDecimal.ZERO, productionLine.getMovementQuantity()));
          documentPostAssertList1.add(new DocumentPostAssert(productionLine.getProduct().getId(),
              "61000", BigDecimal.ZERO, amountTotal, productionLine.getMovementQuantity()));
        }

        else {
          amountTotal = amountTotal.add(productionLine.getMovementQuantity()
              .negate()
              .multiply(productionLine.getProduct()
                  .getPricingProductPriceList()
                  .get(0)
                  .getStandardPrice()));
          documentPostAssertList1.add(new DocumentPostAssert(productionLine.getProduct().getId(),
              "35000", BigDecimal.ZERO, amountTotal, productionLine.getMovementQuantity()));
          documentPostAssertList1.add(new DocumentPostAssert(productionLine.getProduct().getId(),
              "61000", amountTotal, BigDecimal.ZERO, productionLine.getMovementQuantity()));
        }

        i++;
      }

      bMaterialsProduction = OBDal.getInstance()
          .get(ProductionTransaction.class, billOfMaterialsProduction.getId());
      assertDocumentPost(bMaterialsProduction, null, documentPostAssertList1);

      return bMaterialsProduction;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Update transaction total cost amount
  public static void manualCostAdjustment(MaterialTransaction materialTransaction,
      BigDecimal amount, boolean incremental, int day) {
    try {
      manualCostAdjustment(materialTransaction, amount, incremental, true, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Update transaction total cost amount
  public static void manualCostAdjustment(MaterialTransaction materialTransaction,
      BigDecimal amount, boolean incremental, boolean unitCost, int day) {
    try {
      manualCostAdjustment(materialTransaction.getId(), amount, incremental, unitCost, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Cancel cost adjustment
  public static void cancelCostAdjustment(CostAdjustment costAdjustment) {
    try {
      cancelCostAdjustment(costAdjustment.getId());
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Sales Order cloning a created one and book it
  public static Order createReturnFromCustomer(ShipmentInOut goodsShipment, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      Order returnFromCustomer = createReturnFromCustomer(goodsShipment.getId(), price, quantity,
          day);
      bookOrder(returnFromCustomer);
      return returnFromCustomer;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Sales Order cloning a created one and book it
  public static ShipmentInOut createReturnMaterialReceipt(Order returnFromCustomer,
      BigDecimal price, BigDecimal quantity, int day) {
    try {
      ShipmentInOut returnMaterialReceipt = createReturnMaterialReceipt(returnFromCustomer.getId(),
          price, quantity, TestCostingConstants.LOCATOR_L01_ID, day);
      String productId = returnMaterialReceipt.getMaterialMgmtShipmentInOutLineList()
          .get(0)
          .getProduct()
          .getId();

      completeDocument(returnMaterialReceipt);
      runCostingBackground();
      ShipmentInOut returnReceipt = OBDal.getInstance()
          .get(ShipmentInOut.class, returnMaterialReceipt.getId());
      postDocument(returnReceipt);
      returnReceipt = OBDal.getInstance().get(ShipmentInOut.class, returnMaterialReceipt.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99900", BigDecimal.ZERO,
          quantity.multiply(price), quantity.negate()));
      documentPostAssertList.add(new DocumentPostAssert("35000", quantity.multiply(price),
          BigDecimal.ZERO, quantity.negate()));
      assertDocumentPost(returnReceipt, productId, documentPostAssertList);
      return returnReceipt;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Invoice from a landed cost, complete it and post it
  public static Invoice createPurchaseInvoiceLandedCost(String landedCostTypeId, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return createPurchaseInvoiceLandedCost(landedCostTypeId, price, quantity, null, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Invoice from a landed cost, complete it and post it
  public static Invoice createPurchaseInvoiceLandedCost(String landedCostTypeId, BigDecimal price,
      BigDecimal quantity, BigDecimal conversion, int day) {
    try {
      Invoice purchaseInvoice = createInvoiceLandedCost(landedCostTypeId, price, quantity, day);
      if (conversion != null) {
        createConversion(purchaseInvoice, conversion);
      }
      completeDocument(purchaseInvoice);
      OBDal.getInstance().commitAndClose();
      postDocument(purchaseInvoice);
      Invoice invoice = OBDal.getInstance().get(Invoice.class, purchaseInvoice.getId());

      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      if (landedCostTypeId.equals(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID)) {
        documentPostAssertList
            .add(new DocumentPostAssert("40000", BigDecimal.ZERO, quantity.multiply(price), null));
        documentPostAssertList.add(
            new DocumentPostAssert("62900", quantity.multiply(price), BigDecimal.ZERO, quantity));
        assertDocumentPost(invoice, null, documentPostAssertList);
      }

      else if (landedCostTypeId
          .equals(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID)) {
        documentPostAssertList.add(new DocumentPostAssert("40000", BigDecimal.ZERO,
            quantity.multiply(price).add(quantity.multiply(price).divide(new BigDecimal("10"))),
            null));
        documentPostAssertList.add(new DocumentPostAssert("47200",
            quantity.multiply(price).divide(new BigDecimal("10")), BigDecimal.ZERO, null));
        documentPostAssertList.add(
            new DocumentPostAssert("62400", quantity.multiply(price), BigDecimal.ZERO, quantity));
        assertDocumentPost(invoice, landedCostTypeId, documentPostAssertList);
      }

      else {
        documentPostAssertList
            .add(new DocumentPostAssert("40000", BigDecimal.ZERO, quantity.multiply(price), null));
        documentPostAssertList.add(
            new DocumentPostAssert("62800", quantity.multiply(price), BigDecimal.ZERO, quantity));
        assertDocumentPost(invoice, landedCostTypeId, documentPostAssertList);
      }

      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Landed Cost from a list of landed cost types, complete it and post
  // it
  public static LandedCost createLandedCost(List<String> landedCostTypeId,
      List<BigDecimal> amountList, List<ShipmentInOut> receiptList,
      List<ShipmentInOutLine> receiptLineList, int day) {
    try {
      List<String> receiptIdList = new ArrayList<String>();
      List<String> receiptLineIdList = new ArrayList<String>();

      if (receiptList == null) {
        for (ShipmentInOutLine receiptLine : receiptLineList) {
          receiptIdList.add(null);
          receiptLineIdList.add(receiptLine.getId());
        }
      } else if (receiptLineList == null) {
        for (ShipmentInOut receipt : receiptList) {
          receiptIdList.add(receipt.getId());
          receiptLineIdList.add(null);
        }
      } else {
        for (int i = 0; i < receiptList.size(); i++) {
          receiptIdList.add(receiptList.get(i) != null ? receiptList.get(i).getId() : null);
          receiptLineIdList
              .add(receiptLineList.get(i) != null ? receiptLineList.get(i).getId() : null);
        }
      }

      LandedCost landedCost = createLandedCost(landedCostTypeId, amountList, null, receiptIdList,
          receiptLineIdList, day);
      processLandedCost(landedCost.getId());
      // Reload the landedCost object after the session has been closed
      landedCost = OBDal.getInstance().get(LandedCost.class, landedCost.getId());
      OBDal.getInstance().refresh(landedCost);
      return postLandedCostHeader(landedCost);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Landed Cost from a list of purchase invoices and goods receipt, complete it and
  // post
  // it
  public static LandedCost createLandedCost(List<Invoice> invoiceList,
      List<ShipmentInOut> receiptList, int day) {
    try {
      List<String> invoiceIdList = new ArrayList<String>();
      List<String> receiptLineIdList = new ArrayList<String>();
      for (Invoice invoice : invoiceList) {
        invoiceIdList.add(invoice.getId());
      }
      List<String> receiptIdList = new ArrayList<String>();
      for (ShipmentInOut receipt : receiptList) {
        receiptIdList.add(receipt.getId());
        receiptLineIdList.add(null);
      }

      LandedCost landedCost = createLandedCost(null, null, invoiceIdList, receiptIdList,
          receiptLineIdList, day);
      processLandedCost(landedCost.getId());
      // Reload the landedCost object after the session has been closed
      landedCost = OBDal.getInstance().get(LandedCost.class, landedCost.getId());
      OBDal.getInstance().refresh(landedCost);
      return postLandedCost(landedCost);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Landed Cost from a list of purchase invoices and goods receipt and complete it
  // it
  public static List<LandedCostCost> createLandedCostCost(List<Invoice> invoiceList,
      ShipmentInOut receipt) {
    try {
      List<String> invoiceIdList = new ArrayList<String>();
      for (Invoice invoice : invoiceList) {
        invoiceIdList.add(invoice.getId());
      }

      List<LandedCostCost> landedCostCostList = createLandedCostCost(invoiceIdList,
          receipt.getId());
      return landedCostCostList;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Complete a Landed Cost and post it
  public static LandedCost postLandedCost(LandedCost landedCost) {
    try {
      postLandedCostHeader(landedCost);
      LandedCost lc = OBDal.getInstance().get(LandedCost.class, landedCost.getId());
      for (LandedCostCost landedCostCost : lc.getLandedCostCostList()) {
        postLandedCostLine(landedCostCost,
            OBDal.getInstance().get(LandedCostCost.class, landedCostCost.getId()).getInvoiceLine());
      }
      return landedCost;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Post landed cost header
  public static LandedCost postLandedCostHeader(LandedCost landedCost) {
    try {
      StringBuffer where = new StringBuffer();
      where.append(" as t1 ");
      where.append("\n left join t1." + LCReceipt.PROPERTY_GOODSSHIPMENT + " t2");
      where.append("\n where t1." + LCReceipt.PROPERTY_LANDEDCOST + " = :landedCost");
      where.append("\n order by t2." + ShipmentInOut.PROPERTY_DOCUMENTNO);
      where.append("\n , t1." + LCReceipt.PROPERTY_CREATIONDATE);
      OBQuery<LCReceipt> criteria1 = OBDal.getInstance()
          .createQuery(LCReceipt.class, where.toString());
      criteria1.setNamedParameter("landedCost",
          OBDal.getInstance().get(LandedCost.class, landedCost.getId()));
      List<LCReceipt> landedCostReceiptList = criteria1.list();

      OBCriteria<LandedCostCost> criteria2 = OBDal.getInstance()
          .createCriteria(LandedCostCost.class);
      criteria2.addEqual(LandedCostCost.PROPERTY_LANDEDCOST, landedCost);
      criteria2.addOrderBy(LandedCostCost.PROPERTY_LINENO, true);
      List<LandedCostCost> landedCostCostList = criteria2.list();

      BigDecimal receiptTotalAmount = BigDecimal.ZERO;
      for (LCReceipt landedCostReceipt : landedCostReceiptList) {
        if (!landedCostReceipt.getGoodsShipment()
            .getMaterialMgmtShipmentInOutLineList()
            .get(0)
            .getProduct()
            .getProductType()
            .equals("S")) {
          if (landedCostReceipt.getGoodsShipmentLine() == null) {
            receiptTotalAmount = receiptTotalAmount
                .add(getTransactionAmount(landedCostReceipt.getGoodsShipment(), landedCost));
          } else {
            receiptTotalAmount = receiptTotalAmount.add(
                getTransactionLineAmount(landedCostReceipt.getGoodsShipmentLine(), landedCost));
          }
        }
      }

      List<List<LandedCostReceiptLineAmountAssert>> landedCostReceiptLineAmountAssertListList = new ArrayList<List<LandedCostReceiptLineAmountAssert>>();

      for (LCReceipt landedCostReceipt : landedCostReceiptList) {

        List<LandedCostReceiptLineAmountAssert> landedCostReceiptLineAmountAssertList = new ArrayList<LandedCostReceiptLineAmountAssert>();

        if (!landedCostReceipt.getGoodsShipment()
            .getMaterialMgmtShipmentInOutLineList()
            .get(0)
            .getProduct()
            .getProductType()
            .equals("S")) {

          for (LandedCostCost landedCostCost : landedCostCostList) {

            if (landedCostCost.getLandedCostMatchedList().isEmpty()) {

              if (landedCostReceipt.getGoodsShipmentLine() != null) {

                BigDecimal amount = landedCostCost.getAmount()
                    .multiply(getTransactionLineAmount(landedCostReceipt.getGoodsShipmentLine(),
                        landedCost))
                    .divide(receiptTotalAmount, 4, RoundingMode.HALF_UP);

                landedCostReceiptLineAmountAssertList.add(new LandedCostReceiptLineAmountAssert(
                    OBDal.getInstance().get(LandedCostCost.class, landedCostCost.getId()),
                    OBDal.getInstance()
                        .get(ShipmentInOutLine.class,
                            landedCostReceipt.getGoodsShipmentLine().getId()),
                    amount));

              } else {
                for (ShipmentInOutLine receiptLine : landedCostReceipt.getGoodsShipment()
                    .getMaterialMgmtShipmentInOutLineList()) {

                  BigDecimal amount = landedCostCost.getAmount()
                      .multiply(getTransactionLineAmount(receiptLine, landedCost))
                      .divide(receiptTotalAmount, 4, RoundingMode.HALF_UP);

                  landedCostReceiptLineAmountAssertList.add(new LandedCostReceiptLineAmountAssert(
                      OBDal.getInstance().get(LandedCostCost.class, landedCostCost.getId()),
                      OBDal.getInstance().get(ShipmentInOutLine.class, receiptLine.getId()),
                      amount));

                }
              }
            }

            else {

              OBCriteria<LCMatched> criteria3 = OBDal.getInstance().createCriteria(LCMatched.class);
              criteria3.addEqual(LCMatched.PROPERTY_LANDEDCOSTCOST, landedCostCost);
              criteria3.addOrderBy(LCMatched.PROPERTY_CREATIONDATE, true);

              for (LCMatched landedCostMatched : criteria3.list()) {

                if (landedCostReceipt.getGoodsShipmentLine() != null) {

                  BigDecimal amount = landedCostMatched.getAmount()
                      .multiply(getTransactionLineAmount(landedCostReceipt.getGoodsShipmentLine(),
                          landedCost))
                      .divide(receiptTotalAmount, 4, RoundingMode.HALF_UP);

                  landedCostReceiptLineAmountAssertList.add(new LandedCostReceiptLineAmountAssert(
                      OBDal.getInstance().get(LandedCostCost.class, landedCostCost.getId()),
                      OBDal.getInstance()
                          .get(ShipmentInOutLine.class,
                              landedCostReceipt.getGoodsShipmentLine().getId()),
                      amount));

                } else {
                  for (ShipmentInOutLine receiptLine : landedCostReceipt.getGoodsShipment()
                      .getMaterialMgmtShipmentInOutLineList()) {

                    BigDecimal amount = landedCostMatched.getAmount()
                        .multiply(getTransactionLineAmount(receiptLine, landedCost))
                        .divide(receiptTotalAmount, 4, RoundingMode.HALF_UP);

                    landedCostReceiptLineAmountAssertList.add(new LandedCostReceiptLineAmountAssert(
                        OBDal.getInstance().get(LandedCostCost.class, landedCostCost.getId()),
                        OBDal.getInstance().get(ShipmentInOutLine.class, receiptLine.getId()),
                        amount));

                  }
                }
              }
            }
          }
        }

        landedCostReceiptLineAmountAssertListList.add(landedCostReceiptLineAmountAssertList);
      }

      BigDecimal landedCostCostAmount = BigDecimal.ZERO;
      for (LandedCostCost landedCostCost : landedCostCostList) {
        if (landedCostCost.getLandedCostMatchedList().size() == 1) {
          landedCostCostAmount = landedCostCost.isMatchingAdjusted()
              ? landedCostCost.getMatchingAmount()
              : landedCostCost.getAmount();
        } else {
          for (LCMatched landedCostMatched : landedCostCost.getLandedCostMatchedList()) {
            landedCostCostAmount = landedCostCostAmount.add(landedCostMatched.getAmount());
          }
        }
      }

      for (LandedCostCost landedCostCost : landedCostCostList) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal maxAmount = BigDecimal.ZERO;
        int maxI = 0;
        int maxJ = 0;
        int i = 0;
        for (List<LandedCostReceiptLineAmountAssert> landedCostReceiptLineAmountAssertList : landedCostReceiptLineAmountAssertListList) {
          int j = 0;
          for (LandedCostReceiptLineAmountAssert landedCostReceiptLineAmountAssert : landedCostReceiptLineAmountAssertList) {
            if (landedCostReceiptLineAmountAssert.getLandedCostCost().equals(landedCostCost)) {
              totalAmount = totalAmount.add(landedCostReceiptLineAmountAssert.getAmount());
              if (landedCostReceiptLineAmountAssert.getAmount().compareTo(maxAmount) > 0) {
                maxAmount = landedCostReceiptLineAmountAssert.getAmount();
                maxI = i;
                maxJ = j;
              }
            }
            j++;
          }
          i++;
        }

        if (!totalAmount.setScale(4, RoundingMode.HALF_UP)
            .equals(landedCostCostAmount.setScale(4, RoundingMode.HALF_UP))) {
          landedCostReceiptLineAmountAssertListList.get(maxI)
              .set(maxJ, new LandedCostReceiptLineAmountAssert(
                  landedCostReceiptLineAmountAssertListList.get(maxI).get(maxJ).getLandedCostCost(),
                  landedCostReceiptLineAmountAssertListList.get(maxI).get(maxJ).getReceiptLine(),
                  landedCostReceiptLineAmountAssertListList.get(maxI)
                      .get(maxJ)
                      .getAmount()
                      .add(totalAmount.add(
                          (landedCostCost.isMatchingAdjusted() ? landedCostCost.getMatchingAmount()
                              : landedCostCost.getAmount()).negate())
                          .negate())));
        }
      }

      int i = 0;
      for (LCReceipt landedCostReceipt : landedCostReceiptList) {
        assertLandedCostReceiptLineAmount(landedCostReceipt.getId(),
            landedCostReceiptLineAmountAssertListList.get(i));
        i++;
      }

      List<LCReceipt> lCReceiptList = new ArrayList<LCReceipt>(landedCostReceiptList);
      i = 0;
      for (LCReceipt landedCostReceipt : landedCostReceiptList) {
        if (landedCostReceipt.getGoodsShipmentLine() != null && i < landedCostReceiptList.size() - 1
            && landedCostReceiptList.get(i + 1).getGoodsShipmentLine() != null
            && landedCostReceipt.getGoodsShipment()
                .getDocumentNo()
                .equals(landedCostReceiptList.get(i + 1).getGoodsShipment().getDocumentNo())
            && landedCostReceipt.getGoodsShipmentLine()
                .getLineNo()
                .compareTo(
                    landedCostReceiptList.get(i + 1).getGoodsShipmentLine().getLineNo()) > 0) {
          lCReceiptList.set(i, landedCostReceiptList.get(i + 1));
          lCReceiptList.set(i + 1, landedCostReceipt);

        }
        i++;
      }

      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();

      for (LandedCostCost landedCostCost : landedCostCostList) {

        String account;
        if (landedCostCost.getLandedCostType()
            .equals(OBDal.getInstance()
                .get(Product.class, TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID)
                .getLandedCostTypeList()
                .get(0))) {
          account = "62400";
        } else if (landedCostCost.getLandedCostType()
            .equals(OBDal.getInstance()
                .get(Product.class, TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID)
                .getLandedCostTypeList()
                .get(0))) {
          account = "62800";
        } else {
          account = "62900";
        }

        for (LCReceipt landedCostReceipt : lCReceiptList) {

          if (!landedCostReceipt.getGoodsShipment()
              .getMaterialMgmtShipmentInOutLineList()
              .get(0)
              .getProduct()
              .getProductType()
              .equals("S")) {

            if (landedCostReceipt.getGoodsShipmentLine() != null) {

              BigDecimal amount = landedCostCost.getAmount()
                  .multiply(getTransactionLineAmount(landedCostReceipt.getGoodsShipmentLine(),
                      landedCost))
                  .divide(receiptTotalAmount, 4, RoundingMode.HALF_UP);

              documentPostAssertList.add(new DocumentPostAssert(
                  landedCostReceipt.getGoodsShipmentLine().getProduct().getId(), "35000", amount,
                  BigDecimal.ZERO, null));
              documentPostAssertList
                  .add(new DocumentPostAssert(account, BigDecimal.ZERO, amount, null));

            } else {

              OBCriteria<ShipmentInOutLine> criteria3 = OBDal.getInstance()
                  .createCriteria(ShipmentInOutLine.class);
              criteria3.addEqual(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT,
                  landedCostReceipt.getGoodsShipment());
              criteria3.addOrderBy(ShipmentInOutLine.PROPERTY_LINENO, true);

              for (ShipmentInOutLine receiptLine : criteria3.list()) {

                BigDecimal amount = landedCostCost.getAmount()
                    .multiply(getTransactionLineAmount(receiptLine, landedCost))
                    .divide(receiptTotalAmount, 4, RoundingMode.HALF_UP);

                documentPostAssertList.add(new DocumentPostAssert(receiptLine.getProduct().getId(),
                    "35000", amount, BigDecimal.ZERO, null));
                documentPostAssertList
                    .add(new DocumentPostAssert(account, BigDecimal.ZERO, amount, null));

              }
            }
          }
        }
      }

      postDocument(landedCost);
      assertDocumentPost(landedCost, null, documentPostAssertList);

      return landedCost;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Post landed cost line
  public static LandedCostCost postLandedCostLine(LandedCostCost landedCostCost,
      InvoiceLine invoiceLine) {
    try {
      List<LandedCostCostMatchedAssert> landedCostCostMatchedAssertList = new ArrayList<LandedCostCostMatchedAssert>();
      InvoiceLine iLine = OBDal.getInstance().get(InvoiceLine.class, invoiceLine.getId());
      landedCostCostMatchedAssertList.add(new LandedCostCostMatchedAssert(iLine));
      if (!iLine.getInvoice().getCurrencyConversionRateDocList().isEmpty()) {
        landedCostCostMatchedAssertList.add(new LandedCostCostMatchedAssert(iLine));
      }
      assertLandedCostCostMatched(landedCostCost.getId(), landedCostCostMatchedAssertList);

      postDocument(landedCostCost);
      LandedCostCost lcCost = OBDal.getInstance().get(LandedCostCost.class, landedCostCost.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();

      String account;
      String productId;
      if (lcCost.getLandedCostType()
          .equals(OBDal.getInstance()
              .get(Product.class, TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID)
              .getLandedCostTypeList()
              .get(0))) {
        account = "62400";
        productId = TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID;
      } else if (lcCost.getLandedCostType()
          .equals(OBDal.getInstance()
              .get(Product.class, TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID)
              .getLandedCostTypeList()
              .get(0))) {
        account = "62800";
        productId = TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID;
      } else {
        account = "62900";
        productId = null;
      }

      if (lcCost.getLandedCostMatchedList().size() == 1) {

        documentPostAssertList.add(new DocumentPostAssert(productId, account, BigDecimal.ZERO,
            lcCost.getMatchingAmount(), null));

        if (!lcCost.getAmount()
            .setScale(2, RoundingMode.HALF_UP)
            .equals(lcCost.getMatchingAmount().setScale(2, RoundingMode.HALF_UP))
            && lcCost.isMatchingAdjusted()) {

          documentPostAssertList
              .add(new DocumentPostAssert(account, lcCost.getAmount(), BigDecimal.ZERO, null));

          LandedCost landedCost = OBDal.getInstance()
              .get(LandedCost.class, landedCostCost.getLandedCost().getId());

          if (landedCost.getLandedCostReceiptList().size() > 1 && !OBDal.getInstance()
              .get(LandedCost.class, landedCostCost.getLandedCost().getId())
              .getLandedCostReceiptList()
              .get(0)
              .getGoodsShipment()
              .getMaterialMgmtShipmentInOutLineList()
              .get(0)
              .getProduct()
              .equals(OBDal.getInstance()
                  .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                  .getLandedCostReceiptList()
                  .get(1)
                  .getGoodsShipment()
                  .getMaterialMgmtShipmentInOutLineList()
                  .get(0)
                  .getProduct())) {

            final OBCriteria<AccountingFact> criteria = OBDal.getInstance()
                .createCriteria(AccountingFact.class);
            criteria.addEqual(AccountingFact.PROPERTY_RECORDID, lcCost.getId());
            criteria.addOrderBy(AccountingFact.PROPERTY_SEQUENCENUMBER, true);

            if (criteria.list()
                .get(2)
                .getForeignCurrencyDebit()
                .setScale(2, RoundingMode.HALF_UP)
                .equals(lcCost.getMatchingAmount()
                    .add(lcCost.getAmount().negate())
                    .multiply(getTransactionAmount(
                        landedCost.getLandedCostReceiptList().get(0).getGoodsShipment(),
                        landedCost))
                    .divide(getTransactionAmount(
                        landedCost.getLandedCostReceiptList().get(0).getGoodsShipment(), landedCost)
                            .add(getTransactionAmount(
                                landedCost.getLandedCostReceiptList().get(1).getGoodsShipment(),
                                landedCost)),
                        2, RoundingMode.HALF_UP))) {

              documentPostAssertList.add(new DocumentPostAssert(
                  OBDal.getInstance()
                      .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                      .getLandedCostReceiptList()
                      .get(0)
                      .getGoodsShipment()
                      .getMaterialMgmtShipmentInOutLineList()
                      .get(0)
                      .getProduct()
                      .getId(),
                  "35000",
                  lcCost.getMatchingAmount()
                      .add(lcCost.getAmount().negate())
                      .multiply(getTransactionAmount(
                          landedCost.getLandedCostReceiptList().get(0).getGoodsShipment(),
                          landedCost))
                      .divide(getTransactionAmount(
                          landedCost.getLandedCostReceiptList().get(0).getGoodsShipment(),
                          landedCost)
                              .add(getTransactionAmount(
                                  landedCost.getLandedCostReceiptList().get(1).getGoodsShipment(),
                                  landedCost)),
                          2, RoundingMode.HALF_UP),
                  BigDecimal.ZERO, null));

              documentPostAssertList.add(new DocumentPostAssert(
                  OBDal.getInstance()
                      .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                      .getLandedCostReceiptList()
                      .get(1)
                      .getGoodsShipment()
                      .getMaterialMgmtShipmentInOutLineList()
                      .get(0)
                      .getProduct()
                      .getId(),
                  "35000",
                  lcCost.getMatchingAmount()
                      .add(lcCost.getAmount().negate())
                      .multiply(getTransactionAmount(
                          landedCost.getLandedCostReceiptList().get(1).getGoodsShipment(),
                          landedCost))
                      .divide(getTransactionAmount(
                          landedCost.getLandedCostReceiptList().get(0).getGoodsShipment(),
                          landedCost)
                              .add(getTransactionAmount(
                                  landedCost.getLandedCostReceiptList().get(1).getGoodsShipment(),
                                  landedCost)),
                          2, RoundingMode.HALF_UP),
                  BigDecimal.ZERO, null));
            }

            else {
              documentPostAssertList.add(new DocumentPostAssert(
                  OBDal.getInstance()
                      .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                      .getLandedCostReceiptList()
                      .get(1)
                      .getGoodsShipment()
                      .getMaterialMgmtShipmentInOutLineList()
                      .get(0)
                      .getProduct()
                      .getId(),
                  "35000",
                  lcCost.getMatchingAmount()
                      .add(lcCost.getAmount().negate())
                      .multiply(getTransactionAmount(
                          landedCost.getLandedCostReceiptList().get(1).getGoodsShipment(),
                          landedCost))
                      .divide(getTransactionAmount(
                          landedCost.getLandedCostReceiptList().get(0).getGoodsShipment(),
                          landedCost)
                              .add(getTransactionAmount(
                                  landedCost.getLandedCostReceiptList().get(1).getGoodsShipment(),
                                  landedCost)),
                          2, RoundingMode.HALF_UP),
                  BigDecimal.ZERO, null));

              documentPostAssertList.add(new DocumentPostAssert(
                  OBDal.getInstance()
                      .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                      .getLandedCostReceiptList()
                      .get(0)
                      .getGoodsShipment()
                      .getMaterialMgmtShipmentInOutLineList()
                      .get(0)
                      .getProduct()
                      .getId(),
                  "35000",
                  lcCost.getMatchingAmount()
                      .add(lcCost.getAmount().negate())
                      .multiply(getTransactionAmount(
                          landedCost.getLandedCostReceiptList().get(0).getGoodsShipment(),
                          landedCost))
                      .divide(getTransactionAmount(
                          landedCost.getLandedCostReceiptList().get(0).getGoodsShipment(),
                          landedCost)
                              .add(getTransactionAmount(
                                  landedCost.getLandedCostReceiptList().get(1).getGoodsShipment(),
                                  landedCost)),
                          2, RoundingMode.HALF_UP),
                  BigDecimal.ZERO, null));
            }

          } else {
            if (lcCost.getAmount()
                .add(lcCost.getMatchingAmount().negate())
                .compareTo(BigDecimal.ZERO) > 0) {
              documentPostAssertList.add(new DocumentPostAssert(
                  OBDal.getInstance()
                      .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                      .getLandedCostReceiptList()
                      .get(0)
                      .getGoodsShipment()
                      .getMaterialMgmtShipmentInOutLineList()
                      .get(0)
                      .getProduct()
                      .getId(),
                  "35000", BigDecimal.ZERO,
                  lcCost.getAmount().add(lcCost.getMatchingAmount().negate()), null));
            } else {
              documentPostAssertList.add(new DocumentPostAssert(
                  OBDal.getInstance()
                      .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                      .getLandedCostReceiptList()
                      .get(0)
                      .getGoodsShipment()
                      .getMaterialMgmtShipmentInOutLineList()
                      .get(0)
                      .getProduct()
                      .getId(),
                  "35000", lcCost.getMatchingAmount().add(lcCost.getAmount().negate()),
                  BigDecimal.ZERO, null));
            }
          }
        } else {
          documentPostAssertList.add(
              new DocumentPostAssert(account, lcCost.getMatchingAmount(), BigDecimal.ZERO, null));
        }
      }

      else {

        final OBCriteria<LCMatched> criteria1 = OBDal.getInstance().createCriteria(LCMatched.class);
        criteria1.addEqual(LCMatched.PROPERTY_LANDEDCOSTCOST, landedCostCost);
        criteria1.addOrderBy(LCMatched.PROPERTY_CREATIONDATE, true);
        List<LCMatched> landedCostCostMatchedList = criteria1.list();

        final OBCriteria<AccountingFact> criteria2 = OBDal.getInstance()
            .createCriteria(AccountingFact.class);
        criteria2.addEqual(AccountingFact.PROPERTY_RECORDID, lcCost.getId());
        criteria2.addOrderBy(AccountingFact.PROPERTY_SEQUENCENUMBER, true);

        if (!criteria2.list()
            .get(0)
            .getForeignCurrencyDebit()
            .setScale(2, RoundingMode.HALF_UP)
            .equals(landedCostCostMatchedList.get(0).getAmount().setScale(2, RoundingMode.HALF_UP))
            && !criteria2.list()
                .get(0)
                .getForeignCurrencyCredit()
                .setScale(2, RoundingMode.HALF_UP)
                .equals(landedCostCostMatchedList.get(0)
                    .getAmount()
                    .setScale(2, RoundingMode.HALF_UP))) {
          Collections.reverse(landedCostCostMatchedList);
        }

        for (LCMatched landedCostCostMatched : landedCostCostMatchedList) {
          if (landedCostCostMatched.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            documentPostAssertList.add(new DocumentPostAssert(productId, account,
                landedCostCostMatched.getAmount().negate(), BigDecimal.ZERO, null));
          } else {
            documentPostAssertList.add(new DocumentPostAssert(productId, account, BigDecimal.ZERO,
                landedCostCostMatched.getAmount(), null));
          }
        }

        if (!criteria2.list()
            .get(2)
            .getForeignCurrencyDebit()
            .setScale(2, RoundingMode.HALF_UP)
            .equals(landedCostCostMatchedList.get(0).getAmount().setScale(2, RoundingMode.HALF_UP))
            && !criteria2.list()
                .get(2)
                .getForeignCurrencyCredit()
                .setScale(2, RoundingMode.HALF_UP)
                .equals(landedCostCostMatchedList.get(0)
                    .getAmount()
                    .setScale(2, RoundingMode.HALF_UP))) {
          Collections.reverse(landedCostCostMatchedList);
        }

        int i = 0;
        for (LCMatched landedCostCostMatched : landedCostCostMatchedList) {
          if (i == 0) {
            if (landedCostCostMatched.getAmount().compareTo(BigDecimal.ZERO) < 0) {
              documentPostAssertList.add(new DocumentPostAssert(account, BigDecimal.ZERO,
                  landedCostCostMatched.getAmount().negate(), null));
            } else {
              documentPostAssertList.add(new DocumentPostAssert(account,
                  landedCostCostMatched.getAmount(), BigDecimal.ZERO, null));
            }
          } else {
            if (landedCostCostMatched.getAmount().compareTo(BigDecimal.ZERO) < 0) {
              documentPostAssertList.add(new DocumentPostAssert(
                  lcCost.getLandedCostReceiptLineAmtList()
                      .get(0)
                      .getGoodsShipmentLine()
                      .getProduct()
                      .getId(),
                  "35000", BigDecimal.ZERO, landedCostCostMatched.getAmount().negate(), null));
            } else {
              documentPostAssertList
                  .add(new DocumentPostAssert(
                      lcCost.getLandedCostReceiptLineAmtList()
                          .get(0)
                          .getGoodsShipmentLine()
                          .getProduct()
                          .getId(),
                      "35000", landedCostCostMatched.getAmount(), BigDecimal.ZERO, null));
            }
          }
          i++;
        }
      }

      assertDocumentPost(lcCost, null, documentPostAssertList);

      return lcCost;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Reactivate landed cost
  public static void cancelLandedCost(LandedCost landedCost) {
    try {
      for (LandedCostCost landedCostCost : landedCost.getLandedCostCostList()) {
        unpostDocument(landedCostCost);
        cancelLandedCostCost(landedCostCost.getId(), null);
      }
      unpostDocument(landedCost);
      reactivateLandedCost(landedCost.getId(), null);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Match a purchase invoice landed cost with a landed cost cost
  public static void matchInvoiceLandedCost(InvoiceLine purchaseInvoiceLineLandedCost,
      LandedCostCost landedCostCost, boolean matching) {
    try {
      matchInvoiceLandedCost(purchaseInvoiceLineLandedCost.getId(), landedCostCost.getId(),
          purchaseInvoiceLineLandedCost.getLineNetAmount(), null, matching);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Match a purchase invoice landed cost with a landed cost cost
  public static void matchInvoiceLandedCost(InvoiceLine purchaseInvoiceLineLandedCost,
      LandedCostCost landedCostCost, LCMatched landedCostMatched, boolean matching) {
    try {
      matchInvoiceLandedCost(purchaseInvoiceLineLandedCost.getId(), landedCostCost.getId(),
          purchaseInvoiceLineLandedCost.getLineNetAmount(), landedCostMatched.getId(), matching);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Match a landed cost cost
  public static void matchInvoiceLandedCost(LandedCostCost landedCostCost, boolean matching,
      String error) {
    try {
      matchInvoiceLandedCost(landedCostCost.getId(), matching, error);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Change organization currency
  public static void changeOrganizationCurrency(String organizationId, String currencyId) {
    try {
      changeOrganizationCurrency(OBDal.getInstance().get(Organization.class, organizationId),
          currencyId == null ? null : OBDal.getInstance().get(Currency.class, currencyId));
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**********************************************
   * Specific methods for tests
   **********************************************/

  // Create a new order cloning a previous created one
  public static Order cloneOrder(String productId, boolean issotrx, BigDecimal price,
      BigDecimal quantity, int day, String orderId) {
    try {
      Order order = OBDal.getInstance().get(Order.class, orderId);
      Order orderClone = (Order) DalUtil.copy(order, false);
      Product product = OBDal.getInstance().get(Product.class, productId);
      TestCostingUtils.setGeneralData(orderClone);

      orderClone
          .setDocumentNo(getDocumentNo(order.getDocumentType().getDocumentSequence().getId()));
      orderClone.setOrderDate(DateUtils.addDays(new Date(), day));
      orderClone.setScheduledDeliveryDate(DateUtils.addDays(new Date(), day));
      orderClone.setSummedLineAmount(BigDecimal.ZERO);
      orderClone.setGrandTotalAmount(BigDecimal.ZERO);

      // Get the first line associated with the order and clone it to the new
      // order
      OrderLine orderLine = order.getOrderLineList().get(0);
      OrderLine orderCloneLine = (OrderLine) DalUtil.copy(orderLine, false);

      TestCostingUtils.setGeneralData(orderCloneLine);
      orderCloneLine.setOrderDate(DateUtils.addDays(new Date(), day));
      orderCloneLine.setScheduledDeliveryDate(DateUtils.addDays(new Date(), day));

      orderCloneLine.setProduct(product);
      orderCloneLine.setOrderedQuantity(quantity);
      orderCloneLine.setUnitPrice(price);
      orderCloneLine.setListPrice(price);
      orderCloneLine.setStandardPrice(price);
      orderCloneLine.setLineNetAmount(quantity.multiply(price));
      orderCloneLine.setTaxableAmount(quantity.multiply(price));

      if (product.getPricingProductPriceList()
          .get(0)
          .getPriceListVersion()
          .equals(OBDal.getInstance()
              .get(Product.class, TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID)
              .getPricingProductPriceList()
              .get(0)
              .getPriceListVersion())) {
        orderClone
            .setCurrency(OBDal.getInstance().get(Currency.class, TestCostingConstants.DOLLAR_ID));
        orderClone.setPriceList(
            product.getPricingProductPriceList().get(0).getPriceListVersion().getPriceList());
        orderClone.setBusinessPartner(OBDal.getInstance()
            .get(BusinessPartner.class, TestCostingConstants.BUSINESSPARTNER_VENDOR_USA_ID));
        orderClone.setPartnerAddress(OBDal.getInstance()
            .get(BusinessPartner.class, TestCostingConstants.BUSINESSPARTNER_VENDOR_USA_ID)
            .getBusinessPartnerLocationList()
            .get(0));
        orderCloneLine
            .setCurrency(OBDal.getInstance().get(Currency.class, TestCostingConstants.DOLLAR_ID));
        orderCloneLine.setBusinessPartner(OBDal.getInstance()
            .get(BusinessPartner.class, TestCostingConstants.BUSINESSPARTNER_VENDOR_USA_ID));
        orderCloneLine.setPartnerAddress(OBDal.getInstance()
            .get(BusinessPartner.class, TestCostingConstants.BUSINESSPARTNER_VENDOR_USA_ID)
            .getBusinessPartnerLocationList()
            .get(0));
      }

      orderCloneLine.setSalesOrder(orderClone);
      orderClone.getOrderLineList().add(orderCloneLine);

      OBDal.getInstance().save(orderClone);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(orderClone);

      return orderClone;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  public static Order cloneOrder(String productId, boolean issotrx, BigDecimal price,
      BigDecimal quantity, int day) {
    String orderId = issotrx ? TestCostingConstants.ORDEROUT_ID : TestCostingConstants.ORDERIN_ID;
    return cloneOrder(productId, issotrx, price, quantity, day, orderId);
  }

  // Book a order
  public static void bookOrder(Order order) {
    try {
      final List<Object> parameters = new ArrayList<Object>();
      parameters.add(null);
      parameters.add(order.getId());
      parameters.add("N");
      final String procedureName = "c_order_post1";
      CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);

      OBDal.getInstance().save(order);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(order);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Reactivate a order
  public static Order reactivateOrder(Order order) {
    try {
      order.setDocumentStatus("CO");
      order.setDocumentAction("RE");
      OBDal.getInstance().save(order);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(order);
      bookOrder(order);
      return order;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Update order product price
  public static Order updateOrderProductPrice(Order order, BigDecimal price) {
    try {
      OrderLine orderLine = order.getOrderLineList().get(0);

      orderLine.setUpdated(new Date());
      orderLine.setUnitPrice(price);
      orderLine.setListPrice(price);
      orderLine.setStandardPrice(price);
      orderLine.setLineNetAmount(orderLine.getOrderedQuantity().multiply(price));
      orderLine.setTaxableAmount(orderLine.getOrderedQuantity().multiply(price));

      OBDal.getInstance().save(order);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(order);
      return order;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new invoice cloning a previous created one
  public static Invoice cloneInvoice(String productId, boolean issotrx, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return cloneInvoice(productId, issotrx, price, quantity, null, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new invoice cloning a previous created one
  public static Invoice cloneInvoice(String productId, boolean issotrx, BigDecimal price,
      BigDecimal quantity, String bpartnerId, int day) {
    try {
      Invoice invoice = null;
      if (!issotrx) {
        invoice = OBDal.getInstance().get(Invoice.class, TestCostingConstants.INVOICEIN_ID);
      }

      Invoice invoiceClone = (Invoice) DalUtil.copy(invoice, false);
      TestCostingUtils.setGeneralData(invoiceClone);

      if (issotrx) {
        invoiceClone.setDocumentNo(
            getDocumentNo(invoiceClone.getDocumentType().getDocumentSequence().getId()));
      } else {
        invoiceClone.setDocumentNo(getDocumentNo(TestCostingConstants.INVOICEIN_SEQUENCE_ID));
      }

      invoiceClone.setInvoiceDate(DateUtils.addDays(new Date(), day));
      invoiceClone.setAccountingDate(DateUtils.addDays(new Date(), day));
      invoiceClone.setSummedLineAmount(BigDecimal.ZERO);
      invoiceClone.setGrandTotalAmount(BigDecimal.ZERO);
      invoiceClone.setPriceList(OBDal.getInstance()
          .get(Product.class, productId)
          .getPricingProductPriceList()
          .get(0)
          .getPriceListVersion()
          .getPriceList());
      if (bpartnerId != null) {
        invoiceClone.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
        invoiceClone.setPartnerAddress(OBDal.getInstance()
            .get(BusinessPartner.class, bpartnerId)
            .getBusinessPartnerLocationList()
            .get(0));
      }

      // Get the first line associated with the invoice and clone it to the new
      // invoice
      InvoiceLine invoiceLine = invoice.getInvoiceLineList().get(0);
      InvoiceLine invoiceLineClone = (InvoiceLine) DalUtil.copy(invoiceLine, false);

      TestCostingUtils.setGeneralData(invoiceLineClone);

      invoiceLineClone.setProduct(OBDal.getInstance().get(Product.class, productId));
      invoiceLineClone.setInvoicedQuantity(quantity);
      invoiceLineClone.setUnitPrice(price);
      invoiceLineClone.setListPrice(price);
      invoiceLineClone.setStandardPrice(price);
      invoiceLineClone.setLineNetAmount(quantity.multiply(price));
      invoiceLineClone.setTaxableAmount(quantity.multiply(price));
      if (bpartnerId != null) {
        invoiceLineClone
            .setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
      }

      invoiceLineClone.setInvoice(invoiceClone);
      invoiceClone.getInvoiceLineList().add(invoiceLineClone);

      OBDal.getInstance().save(invoiceClone);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoiceClone);

      return invoiceClone;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new invoice from a order
  public static Invoice createInvoiceFromOrder(String orderId, boolean issotrx, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      Order order = OBDal.getInstance().get(Order.class, orderId);
      Invoice invoice = cloneInvoice(order.getOrderLineList().get(0).getProduct().getId(), issotrx,
          price, quantity, day);

      invoice.setSalesOrder(order);
      invoice.setOrderDate(order.getOrderDate());

      int i = 0;
      for (OrderLine orderLine : order.getOrderLineList()) {
        InvoiceLine invoiceLine;

        if (i == 0) {
          invoiceLine = invoice.getInvoiceLineList().get(i);
        }

        else {
          invoiceLine = (InvoiceLine) DalUtil.copy(invoice.getInvoiceLineList().get(0), false);
          TestCostingUtils.setGeneralData(invoiceLine);
          invoiceLine.setInvoice(invoice);
          invoice.getInvoiceLineList().add(invoiceLine);
        }

        invoiceLine.setSalesOrderLine(orderLine);
        invoiceLine.setLineNo((i + 1) * 10L);
        invoiceLine.setInvoicedQuantity(orderLine.getOrderedQuantity());

        if (order.getOrderLineList().size() == 1) {
          invoiceLine.setUnitPrice(price);
          invoiceLine.setListPrice(price);
          invoiceLine.setStandardPrice(price);
          invoiceLine.setLineNetAmount(orderLine.getOrderedQuantity().multiply(price));
        }

        else {
          invoiceLine.setUnitPrice(orderLine.getUnitPrice());
          invoiceLine.setListPrice(orderLine.getUnitPrice());
          invoiceLine.setStandardPrice(orderLine.getUnitPrice());
          invoiceLine
              .setLineNetAmount(orderLine.getOrderedQuantity().multiply(orderLine.getUnitPrice()));
        }

        OBCriteria<ShipmentInOutLine> criteria = OBDal.getInstance()
            .createCriteria(ShipmentInOutLine.class);
        criteria.addEqual(ShipmentInOutLine.PROPERTY_SALESORDERLINE, orderLine);
        criteria.setMaxResults(1);
        ShipmentInOutLine shipmentInOutLine = (ShipmentInOutLine) criteria.uniqueResult();
        if (shipmentInOutLine != null) {
          invoiceLine.setGoodsShipmentLine(shipmentInOutLine);
        }

        orderLine.getInvoiceLineList().add(invoiceLine);
        orderLine.setInvoiceDate(invoice.getInvoiceDate());
        orderLine.setInvoicedQuantity(invoiceLine.getInvoicedQuantity());

        i++;
      }

      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoice);

      order.getInvoiceList().add(invoice);
      order.setReinvoice(true);

      OBDal.getInstance().save(order);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(order);
      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new invoice from a movement
  public static Invoice createInvoiceFromMovement(String movementId, boolean issotrx,
      BigDecimal price, BigDecimal quantity, int day) {
    try {

      ShipmentInOut movement = OBDal.getInstance().get(ShipmentInOut.class, movementId);
      Invoice invoice = cloneInvoice(
          movement.getMaterialMgmtShipmentInOutLineList().get(0).getProduct().getId(), issotrx,
          price, quantity, movement.getBusinessPartner().getId(), day);
      invoice.getMaterialMgmtShipmentInOutList().add(movement);

      int i = 0;
      for (ShipmentInOutLine movementLine : movement.getMaterialMgmtShipmentInOutLineList()) {
        InvoiceLine invoiceLine;

        if (i == 0) {
          invoiceLine = invoice.getInvoiceLineList().get(i);
        }

        else {
          invoiceLine = (InvoiceLine) DalUtil.copy(invoice.getInvoiceLineList().get(0), false);
          TestCostingUtils.setGeneralData(invoiceLine);
          invoiceLine.setInvoice(invoice);
          invoice.getInvoiceLineList().add(invoiceLine);
        }

        invoiceLine.setGoodsShipmentLine(movementLine);
        invoiceLine.setLineNo((i + 1) * 10L);
        invoiceLine.setInvoicedQuantity(movementLine.getMovementQuantity());

        if (movement.getMaterialMgmtShipmentInOutLineList().size() == 1) {
          invoiceLine.setUnitPrice(price);
          invoiceLine.setListPrice(price);
          invoiceLine.setStandardPrice(price);
          invoiceLine.setLineNetAmount(movementLine.getMovementQuantity().multiply(price));
        }

        else {
          invoiceLine.setUnitPrice(movementLine.getSalesOrderLine().getUnitPrice());
          invoiceLine.setListPrice(movementLine.getSalesOrderLine().getUnitPrice());
          invoiceLine.setStandardPrice(movementLine.getSalesOrderLine().getUnitPrice());
          invoiceLine.setLineNetAmount(movementLine.getMovementQuantity()
              .multiply(movementLine.getSalesOrderLine().getUnitPrice()));
        }

        if (movement.getSalesOrder() != null) {
          invoiceLine.setSalesOrderLine(movementLine.getSalesOrderLine());

          movementLine.getSalesOrderLine().getInvoiceLineList().add(invoiceLine);
          movementLine.getSalesOrderLine().setInvoiceDate(invoice.getInvoiceDate());
          movementLine.getSalesOrderLine().setInvoicedQuantity(invoiceLine.getInvoicedQuantity());
        }

        movementLine.setReinvoice(true);
        movementLine.getInvoiceLineList().add(invoiceLine);

        i++;
      }

      if (movement.getSalesOrder() != null) {
        invoice.setSalesOrder(movement.getSalesOrder());
        invoice.setOrderDate(movement.getSalesOrder().getOrderDate());
        invoice.setCurrency(movement.getSalesOrder().getCurrency());

        movement.getSalesOrder().getInvoiceList().add(invoice);
        movement.getSalesOrder().setReinvoice(true);

        OBDal.getInstance().save(movement.getSalesOrder());
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(movement.getSalesOrder());
      }

      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoice);
      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new invoice from many movements
  public static Invoice createInvoiceFromOrders(List<String> orderIdList, boolean issotrx,
      List<BigDecimal> priceList, List<BigDecimal> quantityList, int day) {
    try {
      Order order = OBDal.getInstance().get(Order.class, orderIdList.get(0));
      Invoice invoice = cloneInvoice(order.getOrderLineList().get(0).getProduct().getId(), issotrx,
          priceList.get(0), quantityList.get(0), day);

      invoice.setSalesOrder(order);
      invoice.setOrderDate(order.getOrderDate());

      int i = 0;
      for (String orderId : orderIdList) {
        order = OBDal.getInstance().get(Order.class, orderId);
        OrderLine orderLine = order.getOrderLineList().get(0);
        InvoiceLine invoiceLine;

        if (i == 0) {
          invoiceLine = invoice.getInvoiceLineList().get(i);
        }

        else {
          invoiceLine = (InvoiceLine) DalUtil.copy(invoice.getInvoiceLineList().get(0), false);
          TestCostingUtils.setGeneralData(invoiceLine);
          invoiceLine.setInvoice(invoice);
          invoice.getInvoiceLineList().add(invoiceLine);
        }

        invoiceLine.setSalesOrderLine(orderLine);
        invoiceLine.setLineNo((i + 1) * 10L);
        invoiceLine.setInvoicedQuantity(quantityList.get(i));
        invoiceLine.setUnitPrice(priceList.get(i));
        invoiceLine.setListPrice(priceList.get(i));
        invoiceLine.setStandardPrice(priceList.get(i));
        invoiceLine.setLineNetAmount(quantityList.get(i).multiply(priceList.get(i)));

        OBCriteria<ShipmentInOutLine> criteria = OBDal.getInstance()
            .createCriteria(ShipmentInOutLine.class);
        criteria.addEqual(ShipmentInOutLine.PROPERTY_SALESORDERLINE, orderLine);
        criteria.setMaxResults(1);
        ShipmentInOutLine shipmentInOutLine = (ShipmentInOutLine) criteria.uniqueResult();
        if (shipmentInOutLine != null) {
          invoiceLine.setGoodsShipmentLine(shipmentInOutLine);
        }

        order.getInvoiceList().add(invoice);
        order.setReinvoice(true);
        orderLine.getInvoiceLineList().add(invoiceLine);
        orderLine.setInvoiceDate(invoice.getInvoiceDate());
        orderLine.setInvoicedQuantity(invoiceLine.getInvoicedQuantity());

        OBDal.getInstance().save(order);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(order);

        i++;
      }

      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoice);
      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new invoice from many movements
  public static Invoice createInvoiceFromMovements(List<String> movementIdList, boolean issotrx,
      List<BigDecimal> priceList, BigDecimal quantity, int day) {
    try {
      BigDecimal priceAvg = getAveragePrice(priceList);

      Invoice invoice = cloneInvoice(OBDal.getInstance()
          .get(ShipmentInOut.class, movementIdList.get(0))
          .getMaterialMgmtShipmentInOutLineList()
          .get(0)
          .getProduct()
          .getId(), issotrx, priceAvg, quantity, day);

      int i = 0;
      for (String movementId : movementIdList) {
        ShipmentInOut movement = OBDal.getInstance().get(ShipmentInOut.class, movementId);
        ShipmentInOutLine movementLine = movement.getMaterialMgmtShipmentInOutLineList().get(0);
        InvoiceLine invoiceLine;

        if (i == 0) {
          invoiceLine = invoice.getInvoiceLineList().get(i);
        }

        else {
          invoiceLine = (InvoiceLine) DalUtil.copy(invoice.getInvoiceLineList().get(0), false);
          TestCostingUtils.setGeneralData(invoiceLine);
          invoiceLine.setInvoice(invoice);
          invoice.getInvoiceLineList().add(invoiceLine);
        }

        invoice.getMaterialMgmtShipmentInOutList().add(movement);
        invoiceLine.setProduct(movementLine.getProduct());
        invoiceLine.setGoodsShipmentLine(movementLine);
        invoiceLine.setLineNo((i + 1) * 10L);
        invoiceLine.setInvoicedQuantity(movementLine.getMovementQuantity());
        invoiceLine.setUnitPrice(priceList.get(i));
        invoiceLine.setListPrice(priceList.get(i));
        invoiceLine.setStandardPrice(priceList.get(i));
        invoiceLine.setLineNetAmount(movementLine.getMovementQuantity().multiply(priceList.get(i)));
        invoiceLine.setTaxableAmount(movementLine.getMovementQuantity().multiply(priceList.get(i)));

        if (movement.getSalesOrder() != null) {
          invoice.setSalesOrder(movement.getSalesOrder());
          invoice.setOrderDate(movement.getSalesOrder().getOrderDate());
          invoiceLine.setSalesOrderLine(movementLine.getSalesOrderLine());

          movement.getSalesOrder().getInvoiceList().add(invoice);
          movement.getSalesOrder().setReinvoice(true);
          movementLine.getSalesOrderLine().getInvoiceLineList().add(invoiceLine);
          movementLine.getSalesOrderLine().setInvoiceDate(invoice.getInvoiceDate());
          movementLine.getSalesOrderLine().setInvoicedQuantity(invoiceLine.getInvoicedQuantity());

          OBDal.getInstance().save(movement.getSalesOrder());
          OBDal.getInstance().flush();
          OBDal.getInstance().refresh(movement.getSalesOrder());
        }

        OBDal.getInstance().save(invoice);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(invoice);

        movement.setInvoice(invoice);
        movementLine.setReinvoice(true);
        movementLine.getInvoiceLineList().add(invoiceLine);

        OBDal.getInstance().save(movement);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(movement);

        i++;
      }
      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Reactivate a invoice
  public static Invoice reactivateInvoice(Invoice purchaseInvoice) {
    try {
      Invoice invoice = OBDal.getInstance().get(Invoice.class, purchaseInvoice.getId());
      invoice.setDocumentStatus("CO");
      invoice.setDocumentAction("RE");
      invoice.setPosted("N");
      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoice);
      completeDocument(invoice);
      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Update invoice product price
  public static Invoice updateInvoiceProductPrice(Invoice invoice, BigDecimal price) {
    try {
      InvoiceLine invoiceLine = invoice.getInvoiceLineList().get(0);

      invoiceLine.setUpdated(new Date());
      invoiceLine.setUnitPrice(price);
      invoiceLine.setStandardPrice(price);
      invoiceLine.setLineNetAmount(invoiceLine.getInvoicedQuantity().multiply(price));

      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoice);
      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new movement cloning a previous created one
  public static ShipmentInOut cloneMovement(String productId, boolean issotrx, BigDecimal quantity,
      String locatorId, int day) {
    try {
      return cloneMovement(productId, issotrx, quantity, locatorId, null, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new movement cloning a previous created one
  public static ShipmentInOut cloneMovement(String productId, boolean issotrx, BigDecimal quantity,
      String locatorId, String bpartnerId, int day) {
    try {
      ShipmentInOut movement;
      if (issotrx) {
        movement = OBDal.getInstance()
            .get(ShipmentInOut.class, TestCostingConstants.MOVEMENTOUT_ID);
      } else {
        movement = OBDal.getInstance().get(ShipmentInOut.class, TestCostingConstants.MOVEMENTIN_ID);
      }

      ShipmentInOut movementClone = (ShipmentInOut) DalUtil.copy(movement, false);
      TestCostingUtils.setGeneralData(movement);

      if (issotrx) {
        movementClone.setDocumentNo(
            getDocumentNo(movementClone.getDocumentType().getDocumentSequence().getId()));
      } else {
        movementClone.setDocumentNo(getDocumentNo(TestCostingConstants.SHIPMENTIN_SEQUENCE_ID));
      }

      movementClone.setMovementDate(DateUtils.addDays(new Date(), day));
      movementClone.setAccountingDate(DateUtils.addDays(new Date(), day));
      movementClone.setWarehouse(OBDal.getInstance().get(Locator.class, locatorId).getWarehouse());
      if (bpartnerId != null) {
        movementClone
            .setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
        movementClone.setPartnerAddress(OBDal.getInstance()
            .get(BusinessPartner.class, bpartnerId)
            .getBusinessPartnerLocationList()
            .get(0));
      }

      // Get the first line associated with the movement and clone it to the new
      // movement
      ShipmentInOutLine movementLine = movement.getMaterialMgmtShipmentInOutLineList().get(0);
      ShipmentInOutLine movementLineClone = (ShipmentInOutLine) DalUtil.copy(movementLine, false);

      TestCostingUtils.setGeneralData(movementLineClone);

      movementLineClone.setProduct(OBDal.getInstance().get(Product.class, productId));
      movementLineClone.setMovementQuantity(quantity);
      movementLineClone.setStorageBin(OBDal.getInstance().get(Locator.class, locatorId));
      if (bpartnerId != null) {
        movementLineClone
            .setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
      }

      movementLineClone.setShipmentReceipt(movementClone);
      movementClone.getMaterialMgmtShipmentInOutLineList().add(movementLineClone);

      OBDal.getInstance().save(movementClone);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(movementClone);

      return movementClone;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new Goods Movement cloning a previous created one
  public static InternalMovement createGoodsMovement(String productId, BigDecimal quantity,
      String locatorFromId, String locatorToId, int day) {
    try {
      InternalMovement movement = OBProvider.getInstance().get(InternalMovement.class);
      TestCostingUtils.setGeneralData(movement);
      movement.setName(OBDal.getInstance().get(Product.class, productId).getName() + " - "
          + formatDate(DateUtils.addDays(new Date(), day)));
      movement.setMovementDate(DateUtils.addDays(new Date(), day));
      movement.setPosted("N");
      movement.setDocumentNo(getDocumentNo(TestCostingConstants.MOVEMENT_SEQUENCE_ID));

      InternalMovementLine movementLine = OBProvider.getInstance().get(InternalMovementLine.class);
      TestCostingUtils.setGeneralData(movementLine);
      movementLine.setStorageBin(OBDal.getInstance().get(Locator.class, locatorFromId));
      movementLine.setNewStorageBin(OBDal.getInstance().get(Locator.class, locatorToId));
      movementLine.setProduct(OBDal.getInstance().get(Product.class, productId));
      movementLine.setLineNo(10L);
      movementLine.setMovementQuantity(quantity);
      movementLine.setUOM(OBDal.getInstance().get(UOM.class, TestCostingConstants.UOM_ID));

      movementLine.setMovement(movement);
      movement.getMaterialMgmtInternalMovementLineList().add(movementLine);

      OBDal.getInstance().save(movement);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(movement);
      return movement;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new Goods Movement cloning a previous created one
  public static InternalConsumption createInternalConsumption(String productId, BigDecimal quantity,
      String locatorId, int day) {
    try {
      InternalConsumption consumption = OBProvider.getInstance().get(InternalConsumption.class);
      TestCostingUtils.setGeneralData(consumption);
      consumption.setName(OBDal.getInstance().get(Product.class, productId).getName() + " - "
          + formatDate(DateUtils.addDays(new Date(), day)));
      consumption.setMovementDate(DateUtils.addDays(new Date(), day));

      InternalConsumptionLine consumptionLine = OBProvider.getInstance()
          .get(InternalConsumptionLine.class);
      TestCostingUtils.setGeneralData(consumptionLine);
      consumptionLine.setStorageBin(OBDal.getInstance().get(Locator.class, locatorId));
      consumptionLine.setProduct(OBDal.getInstance().get(Product.class, productId));
      consumptionLine.setLineNo(10L);
      consumptionLine.setMovementQuantity(quantity);
      consumptionLine.setUOM(OBDal.getInstance().get(UOM.class, TestCostingConstants.UOM_ID));

      consumptionLine.setInternalConsumption(consumption);
      consumption.getMaterialMgmtInternalConsumptionLineList().add(consumptionLine);

      OBDal.getInstance().save(consumption);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(consumption);
      return consumption;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Cancel a internal consumption
  public static void cancelInternalConsumption(String internalConsumptionId) {
    try {
      OBDal.getInstance().commitAndClose();
      String procedureName = "m_internal_consumption_post1";
      final List<Object> parameters = new ArrayList<Object>();
      parameters.add(null);
      parameters.add(internalConsumptionId);
      parameters.add("VO");
      CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new movement from a order
  public static ShipmentInOut createMovementFromOrder(String orderId, boolean issotrx,
      BigDecimal quantity, String locatorId, int day) {
    try {
      Order order = OBDal.getInstance().get(Order.class, orderId);
      ShipmentInOut movement = cloneMovement(order.getOrderLineList().get(0).getProduct().getId(),
          issotrx, quantity, locatorId, order.getBusinessPartner().getId(), day);

      movement.setSalesOrder(order);
      movement.setOrderDate(order.getOrderDate());
      movement.getMaterialMgmtShipmentInOutLineList()
          .get(0)
          .setSalesOrderLine(order.getOrderLineList().get(0));
      movement.getMaterialMgmtShipmentInOutLineList()
          .get(0)
          .getOrderLineList()
          .add(order.getOrderLineList().get(0));

      OBDal.getInstance().save(movement);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(movement);
      return movement;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new movement from a invoice
  public static ShipmentInOut createMovementFromInvoice(String invoiceId, boolean issotrx,
      BigDecimal quantity, String locatorId, int day) {
    try {
      Invoice invoice = OBDal.getInstance().get(Invoice.class, invoiceId);
      ShipmentInOut movement = cloneMovement(
          invoice.getInvoiceLineList().get(0).getProduct().getId(), issotrx, quantity, locatorId,
          day);

      movement.setInvoice(invoice);
      movement.getMaterialMgmtShipmentInOutLineList().get(0).setReinvoice(true);
      movement.getMaterialMgmtShipmentInOutLineList()
          .get(0)
          .getInvoiceLineList()
          .add(invoice.getInvoiceLineList().get(0));

      if (invoice.getSalesOrder() != null) {
        movement.getMaterialMgmtShipmentInOutLineList()
            .get(0)
            .setSalesOrderLine(invoice.getSalesOrder().getOrderLineList().get(0));
      }

      OBDal.getInstance().save(movement);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(movement);

      invoice.getMaterialMgmtShipmentInOutList().add(movement);
      invoice.getInvoiceLineList()
          .get(0)
          .setGoodsShipmentLine(movement.getMaterialMgmtShipmentInOutLineList().get(0));

      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoice);

      return movement;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new movement from many orders
  public static ShipmentInOut createMovementFromOrders(List<String> orderIdList, boolean issotrx,
      BigDecimal quantity, String locatorId, int day) {
    try {
      ShipmentInOut movement = cloneMovement(OBDal.getInstance()
          .get(Order.class, orderIdList.get(0))
          .getOrderLineList()
          .get(0)
          .getProduct()
          .getId(), issotrx, quantity, locatorId, day);

      int i = 0;
      for (String orderId : orderIdList) {
        Order order = OBDal.getInstance().get(Order.class, orderId);

        if (i > 0) {
          ShipmentInOutLine movementLine = (ShipmentInOutLine) DalUtil
              .copy(movement.getMaterialMgmtShipmentInOutLineList().get(0), false);
          TestCostingUtils.setGeneralData(movementLine);
          movementLine.setShipmentReceipt(movement);
          movement.getMaterialMgmtShipmentInOutLineList().add(movementLine);
        }

        movement.setSalesOrder(order);
        movement.setOrderDate(order.getOrderDate());
        movement.getMaterialMgmtShipmentInOutLineList()
            .get(i)
            .setProduct(order.getOrderLineList().get(0).getProduct());
        movement.getMaterialMgmtShipmentInOutLineList()
            .get(i)
            .setSalesOrderLine(order.getOrderLineList().get(0));
        movement.getMaterialMgmtShipmentInOutLineList()
            .get(i)
            .getOrderLineList()
            .add(order.getOrderLineList().get(0));
        movement.getMaterialMgmtShipmentInOutLineList().get(i).setLineNo((i + 1) * 10L);
        movement.getMaterialMgmtShipmentInOutLineList()
            .get(i)
            .setMovementQuantity(order.getOrderLineList().get(0).getOrderedQuantity());

        OBDal.getInstance().save(movement);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(movement);

        order.getMaterialMgmtShipmentInOutList().add(movement);
        order.getOrderLineList()
            .get(0)
            .setGoodsShipmentLine(movement.getMaterialMgmtShipmentInOutLineList().get(i));
        order.getOrderLineList()
            .get(0)
            .getMaterialMgmtShipmentInOutLineList()
            .add(movement.getMaterialMgmtShipmentInOutLineList().get(i));

        OBDal.getInstance().save(order);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(order);

        i++;
      }

      return movement;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Inventory Amount Update
  public static InventoryAmountUpdate createInventoryAmountUpdate(String productId,
      BigDecimal originalPrice, BigDecimal finalPrice, BigDecimal quantity, int day) {
    try {
      InventoryAmountUpdate inventoryAmountUpdate = OBProvider.getInstance()
          .get(InventoryAmountUpdate.class);
      TestCostingUtils.setGeneralData(inventoryAmountUpdate);

      final OBCriteria<DocumentType> criteria = OBDal.getInstance()
          .createCriteria(DocumentType.class);
      criteria.addEqual(DocumentType.PROPERTY_NAME, "Inventory Amount Update");
      criteria.setMaxResults(1);
      DocumentType documentType = (DocumentType) criteria.uniqueResult();

      inventoryAmountUpdate.setDocumentType(documentType);
      inventoryAmountUpdate.setDocumentNo(
          getDocumentNo(inventoryAmountUpdate.getDocumentType().getDocumentSequence().getId()));
      inventoryAmountUpdate.setDocumentDate(DateUtils.addDays(new Date(), day));
      OBDal.getInstance().save(inventoryAmountUpdate);

      InventoryAmountUpdateLine inventoryAmountUpdateLine = OBProvider.getInstance()
          .get(InventoryAmountUpdateLine.class);
      TestCostingUtils.setGeneralData(inventoryAmountUpdateLine);
      inventoryAmountUpdateLine.setReferenceDate(DateUtils.addDays(new Date(), day));
      inventoryAmountUpdateLine.setProduct(OBDal.getInstance().get(Product.class, productId));
      inventoryAmountUpdateLine.setWarehouse(
          OBDal.getInstance().get(Warehouse.class, TestCostingConstants.SPAIN_WAREHOUSE_ID));
      inventoryAmountUpdateLine.setInventoryAmount(quantity.multiply(finalPrice));
      inventoryAmountUpdateLine.setCurrentInventoryAmount(quantity.multiply(originalPrice));
      inventoryAmountUpdateLine.setOnHandQty(quantity);
      inventoryAmountUpdateLine.setUnitCost(finalPrice);
      inventoryAmountUpdateLine.setCurrentUnitCost(originalPrice);
      inventoryAmountUpdateLine.setCaInventoryamt(inventoryAmountUpdate);

      inventoryAmountUpdate.getInventoryAmountUpdateLineList().add(inventoryAmountUpdateLine);

      OBDal.getInstance().save(inventoryAmountUpdate);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(inventoryAmountUpdate);

      return inventoryAmountUpdate;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Inventory Amount Update
  public static ProductionTransaction createBillOfMaterialsProduction(String productId,
      BigDecimal quantity, String locatorId, int day) {
    try {
      ProductionTransaction billOfMaterialsProduction = OBProvider.getInstance()
          .get(ProductionTransaction.class);
      TestCostingUtils.setGeneralData(billOfMaterialsProduction);

      billOfMaterialsProduction
          .setName("BOM - " + OBDal.getInstance().get(Product.class, productId).getName());
      billOfMaterialsProduction.setMovementDate(DateUtils.addDays(new Date(), day));
      billOfMaterialsProduction
          .setDocumentNo(getDocumentNo(TestCostingConstants.PRODUCTION_DOCUMENTSEQUENCE_ID));
      billOfMaterialsProduction.setSalesTransaction(true);

      ProductionPlan productionPlan = OBProvider.getInstance().get(ProductionPlan.class);
      TestCostingUtils.setGeneralData(productionPlan);
      productionPlan.setProduction(billOfMaterialsProduction);
      productionPlan.setLineNo(10L);
      productionPlan.setProduct(OBDal.getInstance().get(Product.class, productId));
      productionPlan.setProductionQuantity(quantity);
      productionPlan.setStorageBin(OBDal.getInstance().get(Locator.class, locatorId));

      billOfMaterialsProduction.getMaterialMgmtProductionPlanList().add(productionPlan);

      OBDal.getInstance().save(billOfMaterialsProduction);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(billOfMaterialsProduction);

      return billOfMaterialsProduction;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Sales Order cloning a created one and book it
  public static Order createReturnFromCustomer(String goodsShipmentId, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      ShipmentInOut goodsShipment = OBDal.getInstance().get(ShipmentInOut.class, goodsShipmentId);

      Order order = goodsShipment.getMaterialMgmtShipmentInOutLineList()
          .get(0)
          .getSalesOrderLine()
          .getSalesOrder();
      order.getOrderLineList().get(0).setGoodsShipmentLine(null);

      OBDal.getInstance().save(order);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(order);

      Order returnFromCustomer = cloneOrder(
          goodsShipment.getMaterialMgmtShipmentInOutLineList().get(0).getProduct().getId(), true,
          price, quantity, day, TestCostingConstants.RETURNFROMCUSTOMER_ID);
      returnFromCustomer.setDocumentType(OBDal.getInstance()
          .get(DocumentType.class, TestCostingConstants.RFCORDER_DOCUMENTTYPE_ID));
      returnFromCustomer.setTransactionDocument(OBDal.getInstance()
          .get(DocumentType.class, TestCostingConstants.RFCORDER_DOCUMENTTYPE_ID));
      returnFromCustomer.setDocumentNo(
          getDocumentNo(returnFromCustomer.getDocumentType().getDocumentSequence().getId()));
      returnFromCustomer.setSummedLineAmount(price.multiply(quantity.negate()));
      returnFromCustomer.setGrandTotalAmount(price.multiply(quantity.negate()));

      returnFromCustomer.getOrderLineList().get(0).setOrderedQuantity(quantity.negate());
      returnFromCustomer.getOrderLineList().get(0).setReservedQuantity(quantity.negate());
      returnFromCustomer.getOrderLineList()
          .get(0)
          .setLineNetAmount(price.multiply(quantity.negate()));
      returnFromCustomer.getOrderLineList()
          .get(0)
          .setGoodsShipmentLine(goodsShipment.getMaterialMgmtShipmentInOutLineList().get(0));

      OBDal.getInstance().save(returnFromCustomer);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(returnFromCustomer);

      return returnFromCustomer;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Sales Order cloning a created one and book it
  public static ShipmentInOut createReturnMaterialReceipt(String returnFromCustomerId,
      BigDecimal price, BigDecimal quantity, String locatorId, int day) {
    try {
      Order returnFromCustomer = OBDal.getInstance().get(Order.class, returnFromCustomerId);
      ShipmentInOut returnMaterialReceipt = cloneMovement(
          returnFromCustomer.getOrderLineList().get(0).getProduct().getId(), true, quantity,
          locatorId, day);

      returnMaterialReceipt.setDocumentType(OBDal.getInstance()
          .get(DocumentType.class, TestCostingConstants.RFCRECEIPT_DOCUMENTTYPE_ID));
      returnMaterialReceipt.setDocumentNo(
          getDocumentNo(returnMaterialReceipt.getDocumentType().getDocumentSequence().getId()));

      returnMaterialReceipt.getMaterialMgmtShipmentInOutLineList()
          .get(0)
          .setMovementQuantity(quantity.negate());
      returnMaterialReceipt.getMaterialMgmtShipmentInOutLineList()
          .get(0)
          .setSalesOrderLine(returnFromCustomer.getOrderLineList().get(0));

      OBDal.getInstance().save(returnMaterialReceipt);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(returnMaterialReceipt);

      return returnMaterialReceipt;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new invoice from a order
  public static Invoice createInvoiceLandedCost(String landedCostTypeId, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      Invoice invoice = cloneInvoice(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID,
          false, price, quantity, day);
      InvoiceLine invoiceLine = invoice.getInvoiceLineList().get(0);

      if (landedCostTypeId.equals(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID)) {
        invoiceLine.setAccount(OBDal.getInstance().get(GLItem.class, landedCostTypeId));
        invoiceLine.setFinancialInvoiceLine(true);
        invoiceLine.setProduct(null);
      }

      else if (landedCostTypeId
          .equals(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID)) {
        OBCriteria<TaxRate> criteria = OBDal.getInstance().createCriteria(TaxRate.class);
        criteria.addEqual(TaxRate.PROPERTY_TAXCATEGORY,
            OBDal.getInstance().get(Product.class, landedCostTypeId).getTaxCategory());
        criteria.addEqual(TaxRate.PROPERTY_ORGANIZATION, OBDal.getInstance()
            .get(Organization.class, TestCostingConstants.SPAIN_ORGANIZATION_ID));
        criteria.setMaxResults(1);
        invoiceLine.setTax((TaxRate) criteria.uniqueResult());
      }

      else {
        invoice
            .setCurrency(OBDal.getInstance().get(Currency.class, TestCostingConstants.DOLLAR_ID));
        invoiceLine.setProduct(OBDal.getInstance().get(Product.class, landedCostTypeId));
      }

      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoice);
      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Landed Cost from a list of purchase invoices and goods receipt
  public static LandedCost createLandedCost(List<String> landedCostTypeIdList,
      List<BigDecimal> amountList, List<String> invoiceIdList, List<String> receiptIdList,
      List<String> receiptLineIdList, int day) {
    try {
      LandedCost landedCost = OBProvider.getInstance().get(LandedCost.class);
      TestCostingUtils.setGeneralData(landedCost);
      landedCost.setReferenceDate(DateUtils.addDays(new Date(), day));
      landedCost.setDocumentType(OBDal.getInstance()
          .get(DocumentType.class, TestCostingConstants.LANDEDCOST_DOCUMENTTYPE_ID));
      landedCost
          .setDocumentNo(getDocumentNo(landedCost.getDocumentType().getDocumentSequence().getId()));
      OBDal.getInstance().save(landedCost);

      for (int i = 0; i < (landedCostTypeIdList != null ? landedCostTypeIdList.size()
          : invoiceIdList.size()); i++) {
        LandedCostCost landedCostCost = OBProvider.getInstance().get(LandedCostCost.class);
        TestCostingUtils.setGeneralData(landedCostCost);

        if (landedCostTypeIdList != null) {
          String landedCostTypeId = landedCostTypeIdList.get(i);

          if (landedCostTypeId.equals(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID)) {
            landedCostCost.setLandedCostType(OBDal.getInstance()
                .get(GLItem.class, landedCostTypeId)
                .getLandedCostTypeAccountList()
                .get(0));
          } else {
            landedCostCost.setLandedCostType(OBDal.getInstance()
                .get(Product.class, landedCostTypeId)
                .getLandedCostTypeList()
                .get(0));
          }

          landedCostCost.setAmount(amountList.get(i));

          if (landedCostTypeId.equals(TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID)) {
            landedCostCost.setCurrency(
                OBDal.getInstance().get(Currency.class, TestCostingConstants.DOLLAR_ID));
          } else {
            landedCostCost
                .setCurrency(OBDal.getInstance().get(Currency.class, TestCostingConstants.EURO_ID));
          }
        }

        else {
          String invoiceId = invoiceIdList.get(i);
          InvoiceLine invoiceLine = OBDal.getInstance()
              .get(Invoice.class, invoiceId)
              .getInvoiceLineList()
              .get(0);

          if (invoiceLine.getAccount() != null) {
            landedCostCost
                .setLandedCostType(invoiceLine.getAccount().getLandedCostTypeAccountList().get(0));
          } else {
            landedCostCost
                .setLandedCostType(invoiceLine.getProduct().getLandedCostTypeList().get(0));
          }

          landedCostCost.setInvoiceLine(invoiceLine);
          landedCostCost.setAmount(invoiceLine.getLineNetAmount());
          landedCostCost.setCurrency(invoiceLine.getInvoice().getCurrency());
        }

        landedCostCost.setLandedCostDistributionAlgorithm(OBDal.getInstance()
            .get(LCDistributionAlgorithm.class, TestCostingConstants.LANDEDCOSTCOST_ALGORITHM_ID));
        landedCostCost.setAccountingDate(DateUtils.addDays(new Date(), day));
        landedCostCost.setLineNo((i + 1) * 10L);
        landedCostCost.setDocumentType(OBDal.getInstance()
            .get(DocumentType.class, TestCostingConstants.LANDEDCOSTCOST_DOCUMENTTYPE_ID));

        landedCostCost.setLandedCost(landedCost);
        landedCost.getLandedCostCostList().add(landedCostCost);

        OBDal.getInstance().save(landedCostCost);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(landedCostCost);
      }

      for (int i = 0; i < receiptIdList.size(); i++) {
        LCReceipt landedCostReceipt = OBProvider.getInstance().get(LCReceipt.class);
        TestCostingUtils.setGeneralData(landedCostReceipt);
        if (receiptIdList.get(i) != null) {
          landedCostReceipt
              .setGoodsShipment(OBDal.getInstance().get(ShipmentInOut.class, receiptIdList.get(i)));
        }
        if (receiptLineIdList.get(i) != null) {
          landedCostReceipt.setGoodsShipmentLine(
              OBDal.getInstance().get(ShipmentInOutLine.class, receiptLineIdList.get(i)));
        }
        landedCostReceipt.setLandedCost(landedCost);
        landedCost.getLandedCostReceiptList().add(landedCostReceipt);

        OBDal.getInstance().save(landedCostReceipt);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(landedCostReceipt);
      }

      OBDal.getInstance().save(landedCost);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(landedCost);
      return landedCost;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a list of Landed Cost Cost from a list of purchase invoices and a goods receipt
  public static List<LandedCostCost> createLandedCostCost(List<String> invoiceIdList,
      String receiptId) {
    try {
      List<LandedCostCost> landedCostCostList = new ArrayList<LandedCostCost>();
      int i = 0;
      for (String invoiceId : invoiceIdList) {
        LandedCostCost landedCostCost = OBProvider.getInstance().get(LandedCostCost.class);
        InvoiceLine invoiceLine = OBDal.getInstance()
            .get(Invoice.class, invoiceId)
            .getInvoiceLineList()
            .get(0);
        TestCostingUtils.setGeneralData(landedCostCost);

        if (invoiceLine.getAccount() != null) {
          landedCostCost
              .setLandedCostType(invoiceLine.getAccount().getLandedCostTypeAccountList().get(0));
        } else {
          landedCostCost.setLandedCostType(invoiceLine.getProduct().getLandedCostTypeList().get(0));
        }

        landedCostCost.setGoodsShipment(OBDal.getInstance().get(ShipmentInOut.class, receiptId));
        landedCostCost.setInvoiceLine(invoiceLine);
        landedCostCost.setAmount(invoiceLine.getLineNetAmount());
        landedCostCost.setLandedCostDistributionAlgorithm(OBDal.getInstance()
            .get(LCDistributionAlgorithm.class, TestCostingConstants.LANDEDCOSTCOST_ALGORITHM_ID));
        landedCostCost.setCurrency(invoiceLine.getInvoice().getCurrency());
        landedCostCost.setAccountingDate(new Date());
        landedCostCost.setLineNo((i + 1) * 10L);

        landedCostCostList.add(landedCostCost);
        OBDal.getInstance().save(landedCostCost);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(landedCostCost);

        i++;
      }

      return landedCostCostList;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a conversion rate for an invoice
  public static ConversionRateDoc createConversion(Invoice purchaseInvoice, BigDecimal rate) {
    try {
      ConversionRateDoc conversion = OBProvider.getInstance().get(ConversionRateDoc.class);
      TestCostingUtils.setGeneralData(conversion);
      conversion.setCurrency(purchaseInvoice.getCurrency());
      conversion
          .setToCurrency(purchaseInvoice.getCurrency().getId().equals(TestCostingConstants.EURO_ID)
              ? OBDal.getInstance().get(Currency.class, TestCostingConstants.DOLLAR_ID)
              : OBDal.getInstance().get(Currency.class, TestCostingConstants.EURO_ID));
      conversion.setInvoice(purchaseInvoice);
      conversion.setRate(rate);
      conversion.setForeignAmount(
          purchaseInvoice.getInvoiceLineList().get(0).getLineNetAmount().multiply(rate));

      OBDal.getInstance().save(conversion);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(conversion);
      return conversion;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Change organization currency
  public static void changeOrganizationCurrency(Organization organization, Currency currency) {
    try {
      organization.setCurrency(currency);
      OBDal.getInstance().save(organization);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(organization);
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Cancel landed cost cost matching
  public static void cancelLandedCostCost(String landedCostCostId, String error) {
    try {
      OBDal.getInstance().commitAndClose();
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String content = "{\r    'M_LC_Cost_ID':'" + landedCostCostId + "', \r}";
      Object object = new LCMatchingCancelHandler();
      Class<? extends Object> clazz = object.getClass();
      Method method = clazz.getDeclaredMethod("execute", Map.class, String.class);
      method.setAccessible(true);
      String response = ((JSONObject) method.invoke(object, parameters, content)).toString();
      if (error == null) {
        assertTrue(response.contains("success"));
        assertFalse(response.contains("error"));
      } else {
        assertTrue(response.contains(error));
        assertTrue(response.contains("error"));
        assertFalse(response.contains("success"));
      }
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Reactivate landed cost
  public static void reactivateLandedCost(String landedCostId, String error) {
    try {
      OBDal.getInstance().commitAndClose();
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String content = "{\r    'inpmLandedcostId':'" + landedCostId + "', \r}";
      Object object = new ReactivateLandedCost();
      Class<? extends Object> clazz = object.getClass();
      Method method = clazz.getDeclaredMethod("execute", Map.class, String.class);
      method.setAccessible(true);
      String response = ((JSONObject) method.invoke(object, parameters, content)).toString();
      if (error == null) {
        assertTrue(response.contains("success"));
        assertFalse(response.contains("error"));
      } else {
        assertTrue(response.contains(error));
        assertTrue(response.contains("error"));
        assertFalse(response.contains("success"));
      }
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Match a purchase invoice landed cost with a landed cost cost
  public static void matchInvoiceLandedCost(String purchaseInvoiceLineLandedCostId,
      String landedCostCostId, BigDecimal amount, String landedCostMatchedId, boolean matching) {
    try {
      OBDal.getInstance().commitAndClose();
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String content = "{\r    'C_InvoiceLine_ID':'" + purchaseInvoiceLineLandedCostId + "', \r ";
      content += "'_params':{\r 'LCCosts':{\r '_selection':[\r {\r 'matched':false, \r ";
      content += "'isMatchingAdjusted':" + matching + ", \r 'processMatching':true, \r ";
      content += "'matchedAmt':" + amount + ", \r ";
      content += "'landedCostCost':'" + landedCostCostId + "', \r ";
      content += "'matchedLandedCost':'";
      content += landedCostMatchedId == null ? "" : landedCostMatchedId;
      content += "', \r}\r ]\r }\r }\r }";
      Object object = new LCCostMatchFromInvoiceHandler();
      Class<? extends Object> clazz = object.getClass();
      Method method = clazz.getDeclaredMethod("doExecute", Map.class, String.class);
      method.setAccessible(true);
      String response = ((JSONObject) method.invoke(object, parameters, content)).toString();
      assertTrue(response.contains("success"));
      assertFalse(response.contains("error"));
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Match a purchase invoice landed cost with a landed cost cost
  public static void matchInvoiceLandedCost(String landedCostCostId, boolean matching,
      String error) {
    try {
      OBDal.getInstance().commitAndClose();
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String content = "{\r    'M_LC_Cost_ID':'" + landedCostCostId + "', \r ";
      content += "'_params':{\r 'IsMatchingAdjusted':" + matching + "\r }\r}";
      Object object = new LCMatchingProcessHandler();
      Class<? extends Object> clazz = object.getClass();
      Method method = clazz.getDeclaredMethod("execute", Map.class, String.class);
      method.setAccessible(true);
      String response = ((JSONObject) method.invoke(object, parameters, content)).toString();
      if (error == null) {
        assertTrue(response.contains("success"));
        assertFalse(response.contains("error"));
      } else {
        assertTrue(response.contains(error));
        assertTrue(response.contains("error"));
        assertFalse(response.contains("success"));
      }
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Unpost landed cost
  public static void unpostDocument(BaseOBObject document) {
    try {
      final OBCriteria<AccountingFact> criteria = OBDal.getInstance()
          .createCriteria(AccountingFact.class);
      criteria.addEqual(AccountingFact.PROPERTY_RECORDID, document.getId());
      for (AccountingFact accountingFact : criteria.list()) {
        OBDal.getInstance().remove(accountingFact);
      }
      BaseOBObject doc = OBDal.getInstance().get(document.getClass(), document.getId());
      doc.set("posted", "N");
      OBDal.getInstance().save(doc);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(doc);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Complete a document
  public static BaseOBObject completeDocument(BaseOBObject document) {
    try {
      return completeDocument(document, null);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Complete a document
  public static BaseOBObject completeDocument(BaseOBObject document, String processId) {
    try {
      final OBCriteria<Table> criteria = OBDal.getInstance().createCriteria(Table.class);
      criteria.addEqual(Table.PROPERTY_NAME, document.getEntityName());
      criteria.setMaxResults(1);
      String procedureName = ((Table) criteria.uniqueResult()).getDBTableName() + "_post";

      final List<Object> parameters = new ArrayList<Object>();
      if (processId == null) {
        parameters.add(null);
        parameters.add(document.getId());
      }

      else {
        ProcessInstance processInstance = OBProvider.getInstance().get(ProcessInstance.class);
        TestCostingUtils.setGeneralData(processInstance);
        processInstance.setProcess(OBDal.getInstance().get(Process.class, processId));
        processInstance.setRecordID(document.getId().toString());
        processInstance.setClient(
            OBDal.getInstance().get(Client.class, TestCostingConstants.QATESTING_CLIENT_ID));
        processInstance.setUserContact(
            OBDal.getInstance().get(User.class, TestCostingConstants.ADMIN_USER_ID));
        OBDal.getInstance().save(processInstance);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(processInstance);
        OBDal.getInstance().commitAndClose();
        parameters.add(processInstance.getId());
      }

      CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);

      OBDal.getInstance().save(document);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(document);
      return document;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Post a document
  public static void postDocument(BaseOBObject document) {
    ConnectionProvider conn = getConnectionProvider();
    Connection con = null;

    try {
      final OBCriteria<Table> criteria = OBDal.getInstance().createCriteria(Table.class);
      criteria.addEqual(Table.PROPERTY_NAME, document.getEntityName());
      criteria.setMaxResults(1);
      String tableId = ((Table) criteria.uniqueResult()).getId();
      con = conn.getTransactionConnection();
      AcctServer acct = AcctServer.get(tableId, ((Client) document.get("client")).getId(),
          ((Organization) document.get("organization")).getId(), conn);

      if (acct == null) {
        conn.releaseRollbackConnection(con);
        return;
      } else if (!acct.post((String) document.getId(), false,
          new VariablesSecureApp("100", ((Client) document.get("client")).getId(),
              ((Organization) document.get("organization")).getId()),
          conn, con) || acct.errors != 0) {
        conn.releaseRollbackConnection(con);
        return;
      }

      document.set("posted", "Y");

      conn.releaseCommitConnection(con);
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      try {
        conn.releaseRollbackConnection(con);
      } catch (Exception e2) {
        throw new OBException(e2);
      }
    }
    return;
  }

  // Process a Inventory Amount Update
  public static void processInventoryAmountUpdate(String inventoryAmountUpdateId) {
    try {
      OBDal.getInstance().commitAndClose();
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String content = "{\r    'M_Ca_Inventoryamt_ID':'" + inventoryAmountUpdateId
          + "', \r    'inpadOrgId':'" + TestCostingConstants.SPAIN_ORGANIZATION_ID + "', \r}";
      Object object = new InventoryAmountUpdateProcess();
      Class<? extends Object> clazz = object.getClass();
      Method method = clazz.getDeclaredMethod("execute", Map.class, String.class);
      method.setAccessible(true);
      String response = ((JSONObject) method.invoke(object, parameters, content)).toString();
      assertTrue(response.contains("success"));
      assertFalse(response.contains("error"));
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Process a Bill of Materials Production
  public static void processBillOfMaterialsProduction(
      ProductionTransaction billOfMaterialsProduction) {
    try {
      String procedureName = "m_production_run";
      final List<Object> parameters = new ArrayList<Object>();
      ProcessInstance processInstance = OBProvider.getInstance().get(ProcessInstance.class);
      TestCostingUtils.setGeneralData(processInstance);
      processInstance.setProcess(OBDal.getInstance()
          .get(Process.class, TestCostingConstants.PROCESSPRODUCTION_PROCESS_ID));
      processInstance.setRecordID(billOfMaterialsProduction.getId());
      processInstance.setClient(
          OBDal.getInstance().get(Client.class, TestCostingConstants.QATESTING_CLIENT_ID));
      processInstance.setUserContact(
          OBDal.getInstance().get(User.class, TestCostingConstants.ADMIN_USER_ID));
      OBDal.getInstance().save(processInstance);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(processInstance);
      OBDal.getInstance().commitAndClose();
      parameters.add(processInstance.getId());

      CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);

      OBDal.getInstance().save(billOfMaterialsProduction);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(billOfMaterialsProduction);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Update transaction total cost amount
  public static void manualCostAdjustment(String materialTransactionId, BigDecimal amount,
      boolean incremental, boolean unitCost, int day) {
    try {
      OBDal.getInstance().commitAndClose();
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String content = "{\r    'M_Transaction_ID':'" + materialTransactionId
          + "', \r    '_params':{\r        'Cost':" + amount.toString() + ", \r        'DateAcct':'"
          + formatDate(DateUtils.addDays(new Date(), day)) + "', \r        'IsIncremental':"
          + incremental + ", \r        'IsUnitCost':" + unitCost + "\r    }\r}";
      Object object = new ManualCostAdjustmentProcessHandler();
      Class<? extends Object> clazz = object.getClass();
      Method method = clazz.getDeclaredMethod("execute", Map.class, String.class);
      method.setAccessible(true);
      String response = ((JSONObject) method.invoke(object, parameters, content)).toString();
      assertTrue(response.contains("success"));
      assertFalse(response.contains("error"));
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Cancel cost adjusment
  public static void cancelCostAdjustment(String costAdjusmentId) {
    try {
      OBDal.getInstance().commitAndClose();
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String content = "{\r    'inpmCostadjustmentId':'" + costAdjusmentId + "', \r}";
      Object object = new CancelCostAdjustment();
      Class<? extends Object> clazz = object.getClass();
      Method method = clazz.getDeclaredMethod("execute", Map.class, String.class);
      method.setAccessible(true);
      String response = ((JSONObject) method.invoke(object, parameters, content)).toString();
      assertTrue(response.contains("success"));
      assertFalse(response.contains("error"));
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Process Landed Cost
  public static void processLandedCost(String landedCostId) {
    try {
      OBDal.getInstance().commitAndClose();
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String content = "{\r    'M_Landedcost_ID':'" + landedCostId + "', \r}";
      Object object = new LandedCostProcessHandler();
      Class<? extends Object> clazz = object.getClass();
      Method method = clazz.getDeclaredMethod("execute", Map.class, String.class);
      method.setAccessible(true);
      String response = ((JSONObject) method.invoke(object, parameters, content)).toString();
      assertTrue(response.contains("success"));
      assertFalse(response.contains("error"));
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Run Costing Background process
  public static void runCostingBackground() {
    try {
      VariablesSecureApp vars = null;
      vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getCurrentClient().getId(),
          OBContext.getOBContext().getCurrentOrganization().getId(),
          OBContext.getOBContext().getRole().getId(),
          OBContext.getOBContext().getLanguage().getLanguage());
      ConnectionProvider conn = new DalConnectionProvider(true);
      ProcessBundle pb = new ProcessBundle(CostingBackground.AD_PROCESS_ID, vars).init(conn);
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      pb.setParams(parameters);
      new CostingBackground().execute(pb);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Run Price Correction Background
  public static void runPriceBackground() {
    try {
      VariablesSecureApp vars = null;
      vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getCurrentClient().getId(),
          OBContext.getOBContext().getCurrentOrganization().getId(),
          OBContext.getOBContext().getRole().getId(),
          OBContext.getOBContext().getLanguage().getLanguage());
      ConnectionProvider conn = new DalConnectionProvider(true);
      ProcessBundle pb = new ProcessBundle(PriceDifferenceBackground.AD_PROCESS_ID, vars)
          .init(conn);
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      pb.setParams(parameters);
      new PriceDifferenceBackground().execute(pb);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Validate Costing Rule
  public static void validateCostingRule(String costingRuleId) {
    try {
      OBDal.getInstance().commitAndClose();
      VariablesSecureApp vars = null;
      vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getCurrentClient().getId(),
          OBContext.getOBContext().getCurrentOrganization().getId(),
          OBContext.getOBContext().getRole().getId(),
          OBContext.getOBContext().getLanguage().getLanguage());
      ConnectionProvider conn = new DalConnectionProvider(true);
      ProcessBundle pb = new ProcessBundle(TestCostingConstants.VALIDATECOSTINGRULE_PROCESS_ID,
          vars).init(conn);
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("M_Costing_Rule_ID", costingRuleId);
      pb.setParams(parameters);
      new CostingRuleProcess().execute(pb);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Calculates the next document number for this sequence
  public static String getDocumentNo(String sequenceId) {
    try {
      Sequence sequence = OBDal.getInstance().get(Sequence.class, sequenceId);
      String prefix = sequence.getPrefix() == null ? "" : sequence.getPrefix();
      String suffix = sequence.getSuffix() == null ? "" : sequence.getSuffix();
      String documentNo = prefix + sequence.getNextAssignedNumber().toString() + suffix;
      sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + sequence.getIncrementBy());
      return documentNo;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Calculates the average price of a price list
  public static BigDecimal getAveragePrice(List<BigDecimal> priceList) {
    try {
      BigDecimal priceAvg = BigDecimal.ZERO;
      for (BigDecimal price : priceList) {
        priceAvg = priceAvg.add(price);
      }
      return priceAvg.divide(new BigDecimal(priceList.size()));
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Calculates the average price of a price list
  public static BigDecimal getAveragePrice(List<BigDecimal> priceList,
      List<BigDecimal> quantityList) {
    try {
      BigDecimal priceTotal = BigDecimal.ZERO;
      for (int i = 0; i < quantityList.size(); i++) {
        priceTotal = priceTotal.add(quantityList.get(i).multiply(priceList.get(i)));
      }
      return priceTotal.divide(getTotalQuantity(quantityList), 5, RoundingMode.HALF_UP);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Calculates the total amount of a quantity list
  public static BigDecimal getTotalQuantity(List<BigDecimal> quantityList) {
    try {
      BigDecimal quantityTotal = BigDecimal.ZERO;
      for (BigDecimal quantity : quantityList) {
        quantityTotal = quantityTotal.add(quantity);
      }
      return quantityTotal;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Get Cost Adjustments created for a product
  public static List<CostAdjustment> getCostAdjustment(String productId) {
    try {
      String myQuery = "SELECT DISTINCT t1 "
          + "FROM CostAdjustment t1 LEFT JOIN t1.costAdjustmentLineList t2 LEFT JOIN t2.inventoryTransaction t3 "
          + "WHERE t3.product.id = :productId " + "ORDER BY t1.documentNo";
      Query<CostAdjustment> query = OBDal.getInstance()
          .getSession()
          .createQuery(myQuery, CostAdjustment.class);
      query.setParameter("productId", productId);
      List<CostAdjustment> costAdjustmentList = query.list();
      return costAdjustmentList.isEmpty() ? null : costAdjustmentList;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Get Physical Inventory
  public static List<InventoryCount> getPhysicalInventory(String inventoryAmountUpdateId) {
    try {
      String myQuery = "SELECT t1 "
          + "FROM MaterialMgmtInventoryCount t1, InventoryAmountUpdate t2 LEFT JOIN t2.inventoryAmountUpdateLineList t3 LEFT JOIN t3.inventoryAmountUpdateLineInventoriesList t4 "
          + "WHERE (t4.initInventory = t1 OR t4.closeInventory = t1) AND t2.id = :inventoryAmountUpdateId "
          + "ORDER BY t1.name";
      Query<InventoryCount> query = OBDal.getInstance()
          .getSession()
          .createQuery(myQuery, InventoryCount.class);
      query.setParameter("inventoryAmountUpdateId", inventoryAmountUpdateId);
      return query.list();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Get Product Transaction list
  public static List<MaterialTransaction> getProductTransactions(String productId,
      boolean orderByTransProcessDate) {
    try {
      OBCriteria<MaterialTransaction> criteria = OBDal.getInstance()
          .createCriteria(MaterialTransaction.class);
      criteria.addEqual(MaterialTransaction.PROPERTY_PRODUCT,
          OBDal.getInstance().get(Product.class, productId));
      if (orderByTransProcessDate) {
        criteria.addOrderBy(MaterialTransaction.PROPERTY_TRANSACTIONPROCESSDATE, true);
      } else {
        criteria.addOrderBy(MaterialTransaction.PROPERTY_MOVEMENTDATE, true);
        criteria.addOrderBy(MaterialTransaction.PROPERTY_MOVEMENTQUANTITY, true);
      }
      return criteria.list();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Get Product Transaction for Production Line
  public static MaterialTransaction getProductTransactionsForProductionLine(
      ProductionLine productionLine) {
    try {
      OBCriteria<MaterialTransaction> criteria = OBDal.getInstance()
          .createCriteria(MaterialTransaction.class);
      criteria.addEqual(MaterialTransaction.PROPERTY_PRODUCTIONLINE, productionLine);
      criteria.setMaxResults(1);
      return criteria.uniqueResult() != null ? (MaterialTransaction) criteria.uniqueResult() : null;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Get Product Transaction list
  public static List<MaterialTransaction> getProductTransactions(String productId) {
    return getProductTransactions(productId, false);
  }

  // Get Product Transaction list
  public static List<TransactionCost> getProductTransactionCosts(String transactionId) {
    try {
      StringBuffer where = new StringBuffer();
      where.append(" as t1 ");
      where.append("\n left join t1." + TransactionCost.PROPERTY_COSTADJUSTMENTLINE + " t2");
      where.append("\n left join t2." + CostAdjustmentLine.PROPERTY_COSTADJUSTMENT + " t3");
      where.append(
          "\n where t1." + TransactionCost.PROPERTY_INVENTORYTRANSACTION + " = :transaction");
      where.append("\n order by t3." + CostAdjustment.PROPERTY_DOCUMENTNO + " desc");
      where.append("\n , t2." + CostAdjustmentLine.PROPERTY_LINENO + " desc");
      OBQuery<TransactionCost> hql = OBDal.getInstance()
          .createQuery(TransactionCost.class, where.toString());
      hql.setNamedParameter("transaction",
          OBDal.getInstance().get(MaterialTransaction.class, transactionId));
      return hql.list();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  public static Boolean existsProductTransactionCostByCostIsUnitCostCurrency(
      MaterialTransaction transaction, Boolean isUnitCost, String currencyId, BigDecimal cost) {
    if (transaction != null) {
      try {
        StringBuffer where = new StringBuffer();
        where.append(" as t ");
        where.append(
            "\n where t." + TransactionCost.PROPERTY_INVENTORYTRANSACTION + " = :transaction");
        where.append("\n and t." + TransactionCost.PROPERTY_UNITCOST + " = :unitcost");
        where.append("\n and t." + TransactionCost.PROPERTY_CURRENCY + " = :currencyId");
        where.append("\n and t." + TransactionCost.PROPERTY_COST + " = :cost");
        OBQuery<TransactionCost> hql = OBDal.getInstance()
            .createQuery(TransactionCost.class, where.toString());
        hql.setNamedParameter("transaction", transaction);
        hql.setNamedParameter("unitcost", isUnitCost);
        hql.setNamedParameter("currencyId", OBDal.getInstance().get(Currency.class, currencyId));
        hql.setNamedParameter("cost", cost);
        hql.setMaxResult(1);
        return hql.uniqueResult() != null;
      } catch (Exception e) {
        throw new OBException(e);
      }
    } else {
      return false;
    }
  }

  // Get Product Costing list
  public static List<Costing> getProductCostings(String productId) {
    try {
      // Ordenar por la inventory transaction también
      StringBuffer where = new StringBuffer();
      where.append(" as t1 ");
      where.append("\n join t1." + Costing.PROPERTY_WAREHOUSE + " t2");
      where.append("\n where t1." + Costing.PROPERTY_PRODUCT + " = :product");
      where.append("\n order by t1." + Costing.PROPERTY_MANUAL + " desc");
      where.append("\n , t1." + Costing.PROPERTY_COSTTYPE + " desc");
      where.append("\n , t2." + Warehouse.PROPERTY_NAME + " desc");
      where.append("\n , t1." + Costing.PROPERTY_ENDINGDATE);
      where.append("\n , t1." + Costing.PROPERTY_TOTALMOVEMENTQUANTITY);
      OBQuery<Costing> hql = OBDal.getInstance().createQuery(Costing.class, where.toString());
      hql.setNamedParameter("product", OBDal.getInstance().get(Product.class, productId));
      return hql.list();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Get Production Line list
  public static List<ProductionLine> getProductionLines(String productionTransactionId,
      boolean orderByLineNo) {
    try {
      StringBuffer where = new StringBuffer();
      where.append(" as t1 ");
      where.append("\n left join t1." + ProductionLine.PROPERTY_PRODUCTIONPLAN + " t2");
      where.append("\n left join t1." + ProductionLine.PROPERTY_PRODUCT + " t3");
      where.append(
          "\n where t2." + ProductionPlan.PROPERTY_PRODUCTION + " = :productionTransaction");
      if (orderByLineNo) {
        where.append("\n order by t1." + ProductionLine.PROPERTY_LINENO);
      } else {
        where.append("\n order by t3." + Product.PROPERTY_NAME);
      }
      OBQuery<ProductionLine> hql = OBDal.getInstance()
          .createQuery(ProductionLine.class, where.toString());
      hql.setNamedParameter("productionTransaction",
          OBDal.getInstance().get(ProductionTransaction.class, productionTransactionId));
      return hql.list();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Get Production Line list Order by Line No
  public static List<ProductionLine> getProductionLines(String productionTransactionId) {
    return getProductionLines(productionTransactionId, false);

  }

  // Get transaction amount
  public static BigDecimal getTransactionAmount(ShipmentInOut transaction,
      LandedCost actualLandedCost) {
    try {
      BigDecimal amount = BigDecimal.ZERO;
      for (ShipmentInOutLine transactionLine : transaction.getMaterialMgmtShipmentInOutLineList()) {
        amount = amount.add(getTransactionLineAmount(transactionLine, actualLandedCost));
      }
      return amount;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Get transaction line amount
  public static BigDecimal getTransactionLineAmount(ShipmentInOutLine inoutline,
      LandedCost actualLandedCost) {
    try {
      MaterialTransaction transaction = inoutline.getMaterialMgmtMaterialTransactionList().get(0);
      BigDecimal originalTransactionCost = transaction.getTransactionCost();
      BigDecimal previousUnitCostAdjustments = getCostFromPreviousUnitCostAdjustments(transaction,
          actualLandedCost);
      BigDecimal totalTransactionCost = originalTransactionCost;
      if (previousUnitCostAdjustments != null) {
        totalTransactionCost = originalTransactionCost.add(previousUnitCostAdjustments);
      }
      return totalTransactionCost;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  public static BigDecimal getCostFromPreviousUnitCostAdjustments(MaterialTransaction transaction,
      LandedCost landedCost) {
    StringBuilder hql = new StringBuilder("");
    hql.append(" select sum(tc.cost)");
    hql.append(" from TransactionCost tc");
    hql.append(" where tc.inventoryTransaction.id = :transactionID");
    hql.append(" and tc.unitCost = true");
    hql.append(" and tc.costAdjustmentLine is not null");
    if (landedCost != null && landedCost.getCostAdjustment() != null) {
      hql.append(" and tc.costAdjustmentLine.costAdjustment.id <> :costAdjusmentID");
    }

    Query<BigDecimal> query = OBDal.getInstance()
        .getSession()
        .createQuery(hql.toString(), BigDecimal.class);
    query.setParameter("transactionID", transaction.getId());
    if (landedCost != null && landedCost.getCostAdjustment() != null) {
      query.setParameter("costAdjusmentID", landedCost.getCostAdjustment().getId());
    }

    return query.uniqueResult();
  }

  // Assert common fields in all tables
  public static void assertGeneralData(BaseOBObject document) {
    try {
      assertEquals(((Client) document.get("client")).getId(),
          TestCostingConstants.QATESTING_CLIENT_ID);
      assertEquals(((Organization) document.get("organization")).getName(), "Spain");
      assertTrue(((Boolean) document.get("active")));
      assertEquals(((User) document.get("createdBy")).getId(),
          TestCostingConstants.ADMIN_USER_ID);
      assertEquals(((User) document.get("updatedBy")).getId(),
          TestCostingConstants.ADMIN_USER_ID);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert Matched Invoices
  public static void assertMatchedInvoice(ReceiptInvoiceMatch receiptInvoiceMatch,
      MatchedInvoicesAssert matchedInvoicesAssert) {
    try {
      assertGeneralData(receiptInvoiceMatch);
      assertEquals(receiptInvoiceMatch.getGoodsShipmentLine(),
          matchedInvoicesAssert.getMovementLine());
      assertEquals(receiptInvoiceMatch.getInvoiceLine(), matchedInvoicesAssert.getInvoiceLine());
      assertEquals(receiptInvoiceMatch.getProduct(),
          matchedInvoicesAssert.getInvoiceLine().getProduct());

      assertEquals(formatDate(receiptInvoiceMatch.getTransactionDate()),
          formatDate(matchedInvoicesAssert.getInvoiceLine().getInvoice().getInvoiceDate()));
      assertEquals(receiptInvoiceMatch.getQuantity(),
          matchedInvoicesAssert.getMovementLine().getMovementQuantity());

      assertTrue(receiptInvoiceMatch.isProcessed());
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert Physical Inventory
  public static void assertPhysicalInventory(List<InventoryCount> physicalInventoryList,
      PhysicalInventoryAssert physicalInventoryAssert) {
    try {
      int i = 0;
      for (InventoryCount physicalInventory : physicalInventoryList) {
        assertGeneralData(physicalInventory);
        assertGeneralData(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0));

        if (i % 2 == 0) {
          assertEquals(physicalInventory.getName(), "Inventory Amount Update Closing Inventory");
          assertEquals(physicalInventory.getInventoryType(), "C");

          assertEquals(
              physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getBookQuantity(),
              physicalInventoryAssert.getQuantity());
          assertEquals(
              physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getQuantityCount(),
              BigDecimal.ZERO);
          assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getCost(),
              null);
          assertEquals(
              physicalInventory.getMaterialMgmtInventoryCountLineList()
                  .get(0)
                  .getRelatedInventory(),
              physicalInventoryList.get(i + 1).getMaterialMgmtInventoryCountLineList().get(0));
        }

        else {
          assertEquals(physicalInventory.getName(), "Inventory Amount Update Opening Inventory");
          assertEquals(physicalInventory.getInventoryType(), "O");

          assertEquals(
              physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getBookQuantity(),
              BigDecimal.ZERO);
          assertEquals(
              physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getQuantityCount(),
              physicalInventoryAssert.getQuantity());
          assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList()
              .get(0)
              .getCost()
              .setScale(2, RoundingMode.HALF_UP), physicalInventoryAssert.getPrice());
          assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList()
              .get(0)
              .getRelatedInventory(), null);
        }

        assertEquals(physicalInventory.getDescription(), null);
        assertEquals(physicalInventory.getWarehouse(),
            OBDal.getInstance().get(Warehouse.class, TestCostingConstants.SPAIN_WAREHOUSE_ID));
        assertEquals(formatDate(physicalInventory.getMovementDate()),
            formatDate(DateUtils.addDays(new Date(), physicalInventoryAssert.getDay())));
        assertTrue(physicalInventory.isProcessed());
        assertFalse(physicalInventory.isUpdateQuantities());
        assertFalse(physicalInventory.isGenerateList());
        assertEquals(physicalInventory.getTrxOrganization(), null);
        assertEquals(physicalInventory.getProject(), null);
        assertEquals(physicalInventory.getSalesCampaign(), null);
        assertEquals(physicalInventory.getActivity(), null);
        assertEquals(physicalInventory.getStDimension(), null);
        assertEquals(physicalInventory.getNdDimension(), null);
        assertEquals(physicalInventory.getCostCenter(), null);
        assertEquals(physicalInventory.getAsset(), null);

        assertEquals(
            physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getPhysInventory(),
            physicalInventory);
        assertEquals(
            physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getStorageBin(),
            OBDal.getInstance().get(Locator.class, TestCostingConstants.LOCATOR_L01_ID));
        assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getProduct(),
            physicalInventoryAssert.getProduct());
        assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getLineNo(),
            Long.valueOf(10L));
        assertEquals(
            physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getDescription(),
            null);
        assertEquals(
            physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getAttributeSetValue(),
            OBDal.getInstance().get(AttributeSetInstance.class, "0"));
        assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getOrderUOM(),
            null);
        assertEquals(
            physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getOrderQuantity(),
            null);
        assertEquals(
            physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getUOM().getId(),
            TestCostingConstants.UOM_ID);
        assertEquals(
            physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getQuantityOrderBook(),
            null);

        i++;
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert Landed Cost Cost Matched
  public static void assertLandedCostCostMatched(String landedCostCostId,
      List<LandedCostCostMatchedAssert> landedCostCostMatchedAssertList) {
    try {
      LandedCostCost landedCostCost = OBDal.getInstance()
          .get(LandedCostCost.class, landedCostCostId);
      assertEquals(landedCostCost.getLandedCostMatchedList().size(),
          landedCostCostMatchedAssertList.size());

      OBCriteria<LCMatched> criteria1 = OBDal.getInstance().createCriteria(LCMatched.class);
      criteria1.addEqual(LCMatched.PROPERTY_LANDEDCOSTCOST, landedCostCost);
      criteria1.addOrderBy(LCMatched.PROPERTY_CREATIONDATE, true);
      List<LCMatched> landedCostCostMatchedList = criteria1.list();

      if (!landedCostCostMatchedList.get(0)
          .getAmount()
          .setScale(2, RoundingMode.HALF_UP)
          .equals(landedCostCostMatchedAssertList.get(0)
              .getInvoiceLine()
              .getLineNetAmount()
              .setScale(2, RoundingMode.HALF_UP))) {
        Collections.reverse(landedCostCostMatchedList);
      }

      int i = 0;
      for (LCMatched landedCostCostMatched : landedCostCostMatchedList) {
        assertGeneralData(landedCostCostMatched);
        assertEquals(landedCostCostMatched.getLandedCostCost(), landedCostCost);
        assertEquals(landedCostCostMatched.getInvoiceLine(),
            landedCostCostMatchedAssertList.get(i).getInvoiceLine());

        if (i == 0) {
          assertEquals(landedCostCostMatched.getAmount().setScale(2, RoundingMode.HALF_UP),
              landedCostCostMatchedAssertList.get(i)
                  .getInvoiceLine()
                  .getLineNetAmount()
                  .setScale(2, RoundingMode.HALF_UP));
          assertEquals(
              landedCostCostMatched.getAmountInInvoiceCurrency() == null
                  ? landedCostCostMatched.getAmountInInvoiceCurrency()
                  : landedCostCostMatched.getAmountInInvoiceCurrency()
                      .setScale(2, RoundingMode.HALF_UP),
              landedCostCost.getInvoiceLine() != null ? null
                  : landedCostCostMatchedAssertList.get(i)
                      .getInvoiceLine()
                      .getLineNetAmount()
                      .setScale(2, RoundingMode.HALF_UP));
          assertFalse(landedCostCostMatched.isConversionmatching());
        }

        else {
          Calendar calendar = Calendar.getInstance();
          calendar.set(9999, 0, 1);
          OBCriteria<ConversionRate> criteria2 = OBDal.getInstance()
              .createCriteria(ConversionRate.class);
          criteria2.addEqual(ConversionRate.PROPERTY_CLIENT,
              OBDal.getInstance().get(Client.class, TestCostingConstants.QATESTING_CLIENT_ID));
          criteria2.addEqual(ConversionRate.PROPERTY_CURRENCY,
              OBDal.getInstance().get(Currency.class, TestCostingConstants.DOLLAR_ID));
          criteria2.addEqual(ConversionRate.PROPERTY_TOCURRENCY,
              OBDal.getInstance().get(Currency.class, TestCostingConstants.EURO_ID));
          criteria2.addGreaterOrEqual(ConversionRate.PROPERTY_VALIDTODATE, calendar.getTime());
          criteria2.setMaxResults(1);
          BigDecimal rate = ((ConversionRate) criteria2.uniqueResult()).getMultipleRateBy();

          assertEquals(landedCostCostMatched.getAmount().setScale(2, RoundingMode.HALF_UP),
              landedCostCostMatchedAssertList.get(i)
                  .getInvoiceLine()
                  .getLineNetAmount()
                  .multiply(landedCostCostMatchedAssertList.get(i)
                      .getInvoiceLine()
                      .getInvoice()
                      .getCurrencyConversionRateDocList()
                      .get(0)
                      .getRate())
                  .add(landedCostCostMatchedAssertList.get(i)
                      .getInvoiceLine()
                      .getLineNetAmount()
                      .multiply(rate)
                      .negate())
                  .divide(rate, 2, RoundingMode.HALF_UP));
          assertEquals(landedCostCostMatched.getAmountInInvoiceCurrency(), null);
          assertTrue(landedCostCostMatched.isConversionmatching());
        }

        i++;
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert Landed Cost Receipt Line Amount
  public static void assertLandedCostReceiptLineAmount(String landedCostReceiptId,
      List<LandedCostReceiptLineAmountAssert> landedCostReceiptLineAmountAssertList) {
    try {
      LCReceipt landedCostReceipt = OBDal.getInstance().get(LCReceipt.class, landedCostReceiptId);
      assertEquals(landedCostReceipt.getLandedCostReceiptLineAmtList().size(),
          landedCostReceiptLineAmountAssertList.size());

      StringBuffer where = new StringBuffer();
      where.append(" as t1 ");
      where.append("\n left join t1." + LCReceiptLineAmt.PROPERTY_LANDEDCOSTCOST + " t2");
      where.append("\n left join t1." + LCReceiptLineAmt.PROPERTY_GOODSSHIPMENTLINE + " t3");
      where.append(
          "\n where t1." + LCReceiptLineAmt.PROPERTY_LANDEDCOSTRECEIPT + " = :landedCostReceipt");
      where.append("\n order by t2." + LandedCostCost.PROPERTY_LINENO);
      where.append("\n , t3." + ShipmentInOutLine.PROPERTY_LINENO);
      OBQuery<LCReceiptLineAmt> criteria = OBDal.getInstance()
          .createQuery(LCReceiptLineAmt.class, where.toString());
      criteria.setNamedParameter("landedCostReceipt", landedCostReceipt);
      List<LCReceiptLineAmt> landedCostReceiptLineAmountList = criteria.list();

      if (landedCostReceiptLineAmountList.size() > 0 && !landedCostReceiptLineAmountList.get(0)
          .getAmount()
          .setScale(4, RoundingMode.HALF_UP)
          .equals(landedCostReceiptLineAmountAssertList.get(0)
              .getAmount()
              .setScale(4, RoundingMode.HALF_UP))) {
        Collections.reverse(landedCostReceiptLineAmountList);
      }

      int i = 0;
      for (LCReceiptLineAmt landedCostReceiptLineAmount : landedCostReceiptLineAmountList) {
        LandedCostReceiptLineAmountAssert landedCostReceiptLineAmountAssert = landedCostReceiptLineAmountAssertList
            .get(i);
        assertGeneralData(landedCostReceiptLineAmount);

        assertEquals(landedCostReceiptLineAmount.getAmount().setScale(4, RoundingMode.HALF_UP),
            landedCostReceiptLineAmountAssert.getAmount().setScale(4, RoundingMode.HALF_UP));
        assertEquals(landedCostReceiptLineAmount.getLandedCostReceipt(), landedCostReceipt);
        assertEquals(landedCostReceiptLineAmount.getLandedCostCost(),
            landedCostReceiptLineAmountAssert.getLandedCostCost());
        assertEquals(landedCostReceiptLineAmount.getGoodsShipmentLine(),
            landedCostReceiptLineAmountAssert.getReceiptLine());

        i++;
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  public static void assertProductTransaction(String productId,
      List<ProductTransactionAssert> productTransactionAssertList) {
    assertProductTransaction(productId, productTransactionAssertList, false);
  }

  // Assert Product Transactions
  public static void assertProductTransaction(String productId,
      List<ProductTransactionAssert> productTransactionAssertList, boolean orderByProcessDate) {
    try {
      List<MaterialTransaction> materialTransactionList = getProductTransactions(productId,
          orderByProcessDate);
      assertEquals(materialTransactionList.size(), productTransactionAssertList.size());

      int i = 0;
      int j = 0;
      for (MaterialTransaction materialTransaction : materialTransactionList) {
        ProductTransactionAssert productTransactionAssert = productTransactionAssertList.get(i);
        assertGeneralData(materialTransaction);

        assertEquals(materialTransaction.getProjectIssue(), null);
        assertEquals(materialTransaction.getAttributeSetValue(),
            OBDal.getInstance().get(AttributeSetInstance.class, "0"));
        assertEquals(materialTransaction.getOrderUOM(), null);
        assertEquals(materialTransaction.getOrderQuantity(), null);

        assertEquals(formatDate(materialTransaction.getTransactionProcessDate()),
            formatDate(new Date()));
        assertFalse(materialTransaction.isManualcostadjustment());
        assertEquals(materialTransaction.isCheckpricedifference(),
            productTransactionAssert.isPriceDifference());
        assertEquals(materialTransaction.isCostPermanent(), productTransactionAssert.isPermanent());

        if (productTransactionAssert.getOriginalPrice() != null) {
          assertEquals(materialTransaction.getCurrency().getId(),
              productTransactionAssert.getCurrency().getId());
          assertEquals(materialTransaction.getCostingAlgorithm().getName(), "Average Algorithm");
          assertTrue(materialTransaction.isCostCalculated());
          assertEquals(materialTransaction.getCostingStatus(), "CC");
          assertTrue(materialTransaction.isProcessed());
        }

        else {
          assertEquals(materialTransaction.getCurrency(), null);
          assertEquals(materialTransaction.getCostingAlgorithm(), null);
          assertFalse(materialTransaction.isCostCalculated());
          assertEquals(materialTransaction.getCostingStatus(), "NC");
          assertFalse(materialTransaction.isProcessed());
        }

        if (productTransactionAssert.getShipmentReceiptLine() != null) {

          if (!productTransactionAssert.getShipmentReceiptLine()
              .getShipmentReceipt()
              .isSalesTransaction()) {
            assertEquals(materialTransaction.getMovementQuantity(),
                productTransactionAssert.getShipmentReceiptLine().getMovementQuantity());
          } else {
            assertEquals(materialTransaction.getMovementQuantity(),
                productTransactionAssert.getShipmentReceiptLine().getMovementQuantity().negate());
          }

          if ((!productTransactionAssert.getShipmentReceiptLine()
              .getShipmentReceipt()
              .isSalesTransaction()
              && productTransactionAssert.getShipmentReceiptLine().getCanceledInoutLine() == null)
              || (productTransactionAssert.getShipmentReceiptLine()
                  .getShipmentReceipt()
                  .isSalesTransaction()
                  && productTransactionAssert.getShipmentReceiptLine()
                      .getCanceledInoutLine() != null)
              || productTransactionAssert.getShipmentReceiptLine()
                  .getShipmentReceipt()
                  .getDocumentType()
                  .getName()
                  .equals("RFC Receipt")) {
            assertEquals(materialTransaction.getTransactionCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getOriginalPrice()
                    .multiply(materialTransaction.getMovementQuantity())
                    .setScale(2, RoundingMode.HALF_UP));
            assertEquals(materialTransaction.getTotalCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getTotalPrice()
                    .multiply(materialTransaction.getMovementQuantity())
                    .setScale(2, RoundingMode.HALF_UP));
            assertEquals(materialTransaction.getUnitCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getUnitPrice()
                    .multiply(materialTransaction.getMovementQuantity())
                    .setScale(2, RoundingMode.HALF_UP));
          }

          else {
            assertEquals(materialTransaction.getTransactionCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getOriginalPrice()
                    .multiply(materialTransaction.getMovementQuantity().negate())
                    .setScale(2, RoundingMode.HALF_UP));
            assertEquals(materialTransaction.getTotalCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getTotalPrice()
                    .multiply(materialTransaction.getMovementQuantity().negate())
                    .setScale(2, RoundingMode.HALF_UP));
            assertEquals(materialTransaction.getUnitCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getUnitPrice()
                    .multiply(materialTransaction.getMovementQuantity().negate())
                    .setScale(2, RoundingMode.HALF_UP));
          }

          if (materialTransaction.getGoodsShipmentLine() != null) {
            assertEquals(materialTransaction.getGoodsShipmentLine().getId(),
                productTransactionAssert.getShipmentReceiptLine().getId());
          }
          if (materialTransaction.getPhysicalInventoryLine() != null) {
            assertEquals(materialTransaction.getPhysicalInventoryLine().getId(),
                productTransactionAssert.getInventoryLine().getId());
          }
          if (materialTransaction.getMovementLine() != null) {
            assertEquals(materialTransaction.getMovementLine().getId(),
                productTransactionAssert.getMovementLine().getId());
          }
          if (materialTransaction.getInternalConsumptionLine() != null) {
            assertEquals(materialTransaction.getInternalConsumptionLine().getId(),
                productTransactionAssert.getConsumptionLine().getId());
          }
          if (materialTransaction.getProductionLine() != null) {
            assertEquals(materialTransaction.getProductionLine().getId(),
                productTransactionAssert.getProductionLine().getId());
          }
          assertEquals(materialTransaction.getMovementType(),
              productTransactionAssert.getShipmentReceiptLine()
                  .getShipmentReceipt()
                  .getMovementType());
          assertEquals(materialTransaction.getStorageBin().getId(),
              productTransactionAssert.getShipmentReceiptLine().getStorageBin().getId());
          assertEquals(materialTransaction.getProduct().getId(),
              productTransactionAssert.getShipmentReceiptLine().getProduct().getId());
          assertEquals(formatDate(materialTransaction.getMovementDate()),
              formatDate(productTransactionAssert.getShipmentReceiptLine()
                  .getShipmentReceipt()
                  .getMovementDate()));
          assertEquals(materialTransaction.getUOM().getId(),
              productTransactionAssert.getShipmentReceiptLine().getUOM().getId());
          assertTrue(materialTransaction.isCheckReservedQuantity());
        }

        else if (productTransactionAssert.getInventoryLine() != null) {

          if (j % 2 == 0) {
            assertEquals(materialTransaction.getPhysicalInventoryLine(),
                productTransactionAssert.getInventoryLine()
                    .getInventoryAmountUpdateLineInventoriesList()
                    .get(0)
                    .getCloseInventory()
                    .getMaterialMgmtInventoryCountLineList()
                    .get(0));
            assertEquals(materialTransaction.getMovementQuantity(),
                productTransactionAssert.getInventoryLine().getOnHandQty().negate());
            assertEquals(materialTransaction.getTransactionCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getOriginalPrice()
                    .multiply(materialTransaction.getMovementQuantity().negate())
                    .setScale(2, RoundingMode.HALF_UP));
            assertEquals(materialTransaction.getTotalCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getTotalPrice()
                    .multiply(materialTransaction.getMovementQuantity().negate())
                    .setScale(2, RoundingMode.HALF_UP));
            assertEquals(materialTransaction.getUnitCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getUnitPrice()
                    .multiply(materialTransaction.getMovementQuantity().negate())
                    .setScale(2, RoundingMode.HALF_UP));
            assertFalse(materialTransaction.isCheckReservedQuantity());
          }

          else {
            assertEquals(materialTransaction.getPhysicalInventoryLine(),
                productTransactionAssert.getInventoryLine()
                    .getInventoryAmountUpdateLineInventoriesList()
                    .get(0)
                    .getInitInventory()
                    .getMaterialMgmtInventoryCountLineList()
                    .get(0));
            assertEquals(materialTransaction.getMovementQuantity(),
                productTransactionAssert.getInventoryLine().getOnHandQty());
            assertEquals(materialTransaction.getTransactionCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getOriginalPrice()
                    .multiply(materialTransaction.getMovementQuantity()));
            assertEquals(materialTransaction.getTotalCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getTotalPrice()
                    .multiply(materialTransaction.getMovementQuantity()));
            assertEquals(materialTransaction.getUnitCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getUnitPrice()
                    .multiply(materialTransaction.getMovementQuantity()));
            assertFalse(materialTransaction.isCheckReservedQuantity());
          }

          assertEquals(materialTransaction.getGoodsShipmentLine(),
              productTransactionAssert.getShipmentReceiptLine());
          assertEquals(materialTransaction.getMovementLine(),
              productTransactionAssert.getMovementLine());
          assertEquals(materialTransaction.getInternalConsumptionLine(),
              productTransactionAssert.getConsumptionLine());
          assertEquals(materialTransaction.getProductionLine(),
              productTransactionAssert.getProductionLine());
          assertEquals(materialTransaction.getMovementType(), "I+");
          assertEquals(materialTransaction.getStorageBin(),
              productTransactionAssert.getInventoryLine()
                  .getInventoryAmountUpdateLineInventoriesList()
                  .get(0)
                  .getCloseInventory()
                  .getMaterialMgmtInventoryCountLineList()
                  .get(0)
                  .getStorageBin());
          assertEquals(materialTransaction.getProduct(),
              productTransactionAssert.getInventoryLine().getProduct());
          assertEquals(formatDate(materialTransaction.getMovementDate()), formatDate(
              productTransactionAssert.getInventoryLine().getCaInventoryamt().getDocumentDate()));
          assertEquals(materialTransaction.getUOM(),
              productTransactionAssert.getInventoryLine()
                  .getInventoryAmountUpdateLineInventoriesList()
                  .get(0)
                  .getCloseInventory()
                  .getMaterialMgmtInventoryCountLineList()
                  .get(0)
                  .getUOM());

          j++;
        }

        else if (productTransactionAssert.getMovementLine() != null) {

          if (j % 2 == 0) {
            assertEquals(materialTransaction.getMovementQuantity(),
                productTransactionAssert.getMovementLine().getMovementQuantity().negate());
            assertEquals(materialTransaction.getTransactionCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getOriginalPrice()
                    .multiply(materialTransaction.getMovementQuantity().negate()));
            assertEquals(materialTransaction.getTotalCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getTotalPrice()
                    .multiply(materialTransaction.getMovementQuantity().negate()));
            assertEquals(materialTransaction.getUnitCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getUnitPrice()
                    .multiply(materialTransaction.getMovementQuantity().negate()));
            assertEquals(materialTransaction.getMovementType(), "M-");
            assertEquals(materialTransaction.getStorageBin(),
                productTransactionAssert.getMovementLine().getStorageBin());
          }

          else {
            assertEquals(materialTransaction.getMovementQuantity(),
                productTransactionAssert.getMovementLine().getMovementQuantity());
            assertEquals(materialTransaction.getTransactionCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getOriginalPrice()
                    .multiply(materialTransaction.getMovementQuantity()));
            assertEquals(materialTransaction.getTotalCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getTotalPrice()
                    .multiply(materialTransaction.getMovementQuantity()));
            assertEquals(materialTransaction.getUnitCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getUnitPrice()
                    .multiply(materialTransaction.getMovementQuantity()));
            assertEquals(materialTransaction.getMovementType(), "M+");
            assertEquals(materialTransaction.getStorageBin(),
                productTransactionAssert.getMovementLine().getNewStorageBin());
          }

          assertEquals(materialTransaction.getGoodsShipmentLine(),
              productTransactionAssert.getShipmentReceiptLine());
          assertEquals(materialTransaction.getPhysicalInventoryLine(),
              productTransactionAssert.getInventoryLine());
          assertEquals(materialTransaction.getMovementLine(),
              productTransactionAssert.getMovementLine());
          assertEquals(materialTransaction.getInternalConsumptionLine(),
              productTransactionAssert.getConsumptionLine());
          assertEquals(materialTransaction.getProductionLine(),
              productTransactionAssert.getProductionLine());
          assertEquals(materialTransaction.getProduct(),
              productTransactionAssert.getMovementLine().getProduct());
          assertEquals(formatDate(materialTransaction.getMovementDate()), formatDate(
              productTransactionAssert.getMovementLine().getMovement().getMovementDate()));
          assertEquals(materialTransaction.getUOM(),
              productTransactionAssert.getMovementLine().getUOM());
          assertTrue(materialTransaction.isCheckReservedQuantity());

          j++;
        }

        else if (productTransactionAssert.getProductionLine() != null) {

          assertEquals(materialTransaction.getTransactionCost().setScale(2, RoundingMode.HALF_UP),
              productTransactionAssert.getOriginalPrice()
                  .multiply(materialTransaction.getMovementQuantity().abs()));
          assertEquals(materialTransaction.getTotalCost().setScale(2, RoundingMode.HALF_UP),
              productTransactionAssert.getTotalPrice()
                  .multiply(materialTransaction.getMovementQuantity().abs()));
          assertEquals(materialTransaction.getUnitCost().setScale(2, RoundingMode.HALF_UP),
              productTransactionAssert.getUnitPrice()
                  .multiply(materialTransaction.getMovementQuantity().abs()));

          assertEquals(materialTransaction.getMovementQuantity(),
              productTransactionAssert.getProductionLine().getMovementQuantity());
          assertEquals(materialTransaction.getMovementType(), "P+");
          assertEquals(materialTransaction.getStorageBin().getId(),
              productTransactionAssert.getProductionLine().getStorageBin().getId());

          assertEquals(materialTransaction.getGoodsShipmentLine(),
              productTransactionAssert.getShipmentReceiptLine());
          assertEquals(materialTransaction.getPhysicalInventoryLine(),
              productTransactionAssert.getInventoryLine());
          assertEquals(materialTransaction.getMovementLine(),
              productTransactionAssert.getMovementLine());
          assertEquals(materialTransaction.getInternalConsumptionLine(),
              productTransactionAssert.getConsumptionLine());
          assertEquals(materialTransaction.getProduct().getId(),
              productTransactionAssert.getProductionLine().getProduct().getId());
          assertEquals(formatDate(materialTransaction.getMovementDate()),
              formatDate(productTransactionAssert.getProductionLine()
                  .getProductionPlan()
                  .getProduction()
                  .getMovementDate()));
          assertEquals(materialTransaction.getUOM().getId(),
              productTransactionAssert.getProductionLine().getUOM().getId());
          assertTrue(materialTransaction.isCheckReservedQuantity());

          j++;
        }

        else {

          if (j % 2 == 0) {
            assertEquals(materialTransaction.getMovementQuantity(),
                productTransactionAssert.getConsumptionLine().getMovementQuantity().negate());
            assertEquals(materialTransaction.getTransactionCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getOriginalPrice()
                    .multiply(productTransactionAssert.getConsumptionLine().getMovementQuantity()));
            assertEquals(materialTransaction.getTotalCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getTotalPrice()
                    .multiply(productTransactionAssert.getConsumptionLine().getMovementQuantity()));
            assertEquals(materialTransaction.getUnitCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getUnitPrice()
                    .multiply(productTransactionAssert.getConsumptionLine().getMovementQuantity()));
            assertTrue(materialTransaction.isCheckReservedQuantity());
          }

          else {
            assertEquals(materialTransaction.getMovementQuantity(),
                productTransactionAssert.getConsumptionLine().getMovementQuantity().negate());
            assertEquals(materialTransaction.getTransactionCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getOriginalPrice()
                    .multiply(productTransactionAssert.getConsumptionLine()
                        .getMovementQuantity()
                        .negate()));
            assertEquals(materialTransaction.getTotalCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getTotalPrice()
                    .multiply(productTransactionAssert.getConsumptionLine()
                        .getMovementQuantity()
                        .negate()));
            assertEquals(materialTransaction.getUnitCost().setScale(2, RoundingMode.HALF_UP),
                productTransactionAssert.getUnitPrice()
                    .multiply(productTransactionAssert.getConsumptionLine()
                        .getMovementQuantity()
                        .negate()));
            assertTrue(materialTransaction.isCheckReservedQuantity());
          }

          assertEquals(materialTransaction.getGoodsShipmentLine(),
              productTransactionAssert.getShipmentReceiptLine());
          assertEquals(materialTransaction.getPhysicalInventoryLine(),
              productTransactionAssert.getInventoryLine());
          assertEquals(materialTransaction.getMovementLine(),
              productTransactionAssert.getMovementLine());
          assertEquals(materialTransaction.getInternalConsumptionLine(),
              productTransactionAssert.getConsumptionLine());

          assertEquals(materialTransaction.getMovementType(), "D-");
          assertEquals(materialTransaction.getStorageBin(),
              productTransactionAssert.getConsumptionLine().getStorageBin());
          assertEquals(materialTransaction.getProduct(),
              productTransactionAssert.getConsumptionLine().getProduct());
          assertEquals(formatDate(materialTransaction.getMovementDate()),
              formatDate(productTransactionAssert.getConsumptionLine()
                  .getInternalConsumption()
                  .getMovementDate()));
          assertEquals(materialTransaction.getUOM(),
              productTransactionAssert.getConsumptionLine().getUOM());

          j++;
        }

        StringBuffer where = new StringBuffer();
        where.append(" as t1 ");
        where.append("\n left join t1." + CostAdjustmentLine.PROPERTY_COSTADJUSTMENT + " t2");
        where.append(
            "\n where t1." + CostAdjustmentLine.PROPERTY_INVENTORYTRANSACTION + " = :transaction");
        where.append("\n order by t2." + CostAdjustment.PROPERTY_DOCUMENTNO + " desc");
        where.append("\n , t1." + CostAdjustmentLine.PROPERTY_LINENO + " desc");
        OBQuery<CostAdjustmentLine> hql = OBDal.getInstance()
            .createQuery(CostAdjustmentLine.class, where.toString());
        hql.setNamedParameter("transaction", materialTransaction);
        List<CostAdjustmentLine> costAdjustmentLineList = hql.list();

        if (productTransactionAssert.getOriginalPrice() != null) {
          assertEquals(materialTransaction.getTransactionCostList().size(),
              costAdjustmentLineList.size() + 1);
        } else {
          assertEquals(materialTransaction.getTransactionCostList().size(),
              costAdjustmentLineList.size());
        }

        int k = 0;
        for (TransactionCost materialTransactionCost : getProductTransactionCosts(
            materialTransaction.getId())) {
          assertGeneralData(materialTransactionCost);
          assertEquals(materialTransactionCost.getInventoryTransaction(), materialTransaction);
          assertEquals(formatDate(materialTransactionCost.getCostDate()),
              formatDate(materialTransaction.getTransactionProcessDate()));
          assertEquals(materialTransactionCost.getCurrency().getId(),
              productTransactionAssert.getCurrency().getId());

          if (k == 0) {
            assertEquals(materialTransactionCost.getCost(),
                materialTransaction.getTransactionCost());
            assertEquals(materialTransactionCost.getCostAdjustmentLine(), null);
            assertEquals(formatDate(materialTransactionCost.getAccountingDate()),
                formatDate(materialTransaction.getMovementDate()));
            assertTrue(materialTransactionCost.isUnitCost());
          }

          else {

            Calendar calendar = Calendar.getInstance();
            calendar.set(9999, 0, 1);
            OBCriteria<ConversionRate> criteria2 = OBDal.getInstance()
                .createCriteria(ConversionRate.class);
            criteria2.addEqual(ConversionRate.PROPERTY_CLIENT,
                OBDal.getInstance().get(Client.class, TestCostingConstants.QATESTING_CLIENT_ID));
            criteria2.addEqual(ConversionRate.PROPERTY_CURRENCY,
                OBDal.getInstance().get(Currency.class, TestCostingConstants.DOLLAR_ID));
            criteria2.addEqual(ConversionRate.PROPERTY_TOCURRENCY,
                OBDal.getInstance().get(Currency.class, TestCostingConstants.EURO_ID));
            criteria2.addGreaterOrEqual(ConversionRate.PROPERTY_VALIDTODATE, calendar.getTime());
            criteria2.setMaxResults(1);
            BigDecimal rate = ((ConversionRate) criteria2.uniqueResult()).getMultipleRateBy();

            if (productTransactionAssert.getCurrency().getId().equals(TestCostingConstants.EURO_ID)
                && costAdjustmentLineList.get(k - 1)
                    .getCurrency()
                    .getId()
                    .equals(TestCostingConstants.DOLLAR_ID)) {
              assertEquals(materialTransactionCost.getCost().setScale(4, RoundingMode.HALF_UP),
                  costAdjustmentLineList.get(k - 1)
                      .getAdjustmentAmount()
                      .multiply(rate)
                      .setScale(4, RoundingMode.HALF_UP));
            } else {
              assertEquals(materialTransactionCost.getCost(),
                  costAdjustmentLineList.get(k - 1).getAdjustmentAmount());
            }

            assertEquals(materialTransactionCost.getCostAdjustmentLine(),
                costAdjustmentLineList.get(k - 1));
            assertEquals(formatDate(materialTransactionCost.getAccountingDate()),
                formatDate(costAdjustmentLineList.get(k - 1).getAccountingDate()));
            assertEquals(materialTransactionCost.isUnitCost(),
                costAdjustmentLineList.get(k - 1).isUnitCost());
          }

          k++;
        }

        i++;
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert Product Costing
  public static void assertProductCosting(String productId,
      List<ProductCostingAssert> productCostingAssertList) {
    try {
      Product product = OBDal.getInstance().get(Product.class, productId);
      List<Costing> productCostingList = getProductCostings(productId);
      assertEquals(productCostingList.size(), productCostingAssertList.size());

      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>(
          productCostingAssertList);
      Collections.reverse(productCostingAssertList2);
      int j = 0;
      for (ProductCostingAssert productCostingAssert : productCostingAssertList2) {
        if (productCostingAssert.getWarehouse()
            .getId()
            .equals(TestCostingConstants.SPAIN_WAREHOUSE_ID)) {
          break;
        } else {
          j++;
        }
      }
      int indexWarehouse1 = productCostingAssertList2.size() - 1 - j;

      j = 0;
      for (ProductCostingAssert productCostingAssert : productCostingAssertList2) {
        if (productCostingAssert.getWarehouse()
            .getId()
            .equals(TestCostingConstants.SPAIN_EAST_WAREHOUSE_ID)) {
          break;
        } else {
          j++;
        }
      }
      int indexWarehouse2 = productCostingAssertList2.size() - 1 - j;

      int i = 0;
      for (Costing productCosting : productCostingList) {

        ProductCostingAssert productCostingAssert = productCostingAssertList.get(i);
        assertGeneralData(productCosting);

        assertEquals(productCosting.getCost().setScale(4, RoundingMode.HALF_UP),
            productCostingAssert.getFinalCost().setScale(4, RoundingMode.HALF_UP));

        assertEquals(
            productCosting.getPrice() == null ? null
                : productCosting.getPrice().setScale(4, RoundingMode.HALF_UP),
            productCostingAssert.getPrice() == null ? null
                : productCostingAssert.getPrice().setScale(4, RoundingMode.HALF_UP));

        assertEquals(
            productCosting.getOriginalCost() == null ? null
                : productCosting.getOriginalCost().setScale(4, RoundingMode.HALF_UP),
            productCostingAssert.getOriginalCost() == null ? null
                : productCostingAssert.getOriginalCost().setScale(4, RoundingMode.HALF_UP));

        if (productCostingAssert.getQuantity() == null) {
          assertEquals(productCosting.getQuantity(), null);
        } else {
          assertEquals(productCosting.getQuantity(),
              productCostingAssert.getTransaction().getMovementQuantity());
        }

        assertEquals(productCosting.isManual(), productCostingAssert.isManual());
        assertEquals(productCosting.isPermanent(), !productCostingAssert.isManual());

        assertEquals(productCosting.getInvoiceLine(), null);

        assertFalse(productCosting.isProduction());
        assertEquals(productCosting.getWarehouse(), productCostingAssert.getWarehouse());
        assertEquals(productCosting.getInventoryTransaction(),
            productCostingAssert.getTransaction());
        assertEquals(productCosting.getCurrency(),
            productCostingAssert.getTransaction() != null
                ? productCostingAssert.getTransaction().getCurrency()
                : OBDal.getInstance().get(Currency.class, TestCostingConstants.EURO_ID));
        assertEquals(productCosting.getCostType(), productCostingAssert.getType());

        if (productCostingAssert.getYear() != 0) {
          assertEquals(formatDate(productCosting.getStartingDate()), formatDate(DateUtils.addYears(
              product.getPricingProductPriceList().get(0).getPriceListVersion().getValidFromDate(),
              productCostingAssert.getYear())));
        } else {
          assertEquals(formatDate(productCosting.getStartingDate()), formatDate(new Date()));
        }

        if (productCostingAssert.getType().equals("STA") || i == indexWarehouse1
            || i == indexWarehouse2) {
          Calendar calendar = Calendar.getInstance();
          calendar.set(9999, 11, 31);
          assertEquals(formatDate(productCosting.getEndingDate()), formatDate(calendar.getTime()));
        } else {
          assertEquals(formatDate(productCosting.getEndingDate()),
              formatDate(productCostingList.get(i + 1).getStartingDate()));
        }

        i++;
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert Cost Adjustment
  public static void assertCostAdjustment(List<CostAdjustment> costAdjustmentList,
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList) {
    try {

      // Assert Cost Adjustment header data
      assertEquals(costAdjustmentList.size(), costAdjustmentAssertList.size());
      int i = 0;
      for (CostAdjustment costAdjustment : costAdjustmentList) {

        List<CostAdjustmentAssert> costAdjustmentAssertLineList = costAdjustmentAssertList.get(i);
        assertGeneralData(costAdjustment);
        assertEquals(costAdjustment.getDocumentType().getName(), "Cost Adjustment");
        assertEquals(formatDate(costAdjustment.getReferenceDate()), formatDate(new Date()));
        assertEquals(costAdjustment.getSourceProcess(),
            costAdjustmentAssertLineList.get(0).getType());
        assertTrue(costAdjustment.isProcessed());
        assertFalse(costAdjustment.isProcess());
        assertEquals(costAdjustment.getDocumentStatus(),
            costAdjustmentAssertLineList.get(0).getStatus());
        assertFalse(costAdjustment.isCancelProcess());
        assertEquals(costAdjustment.getCostAdjustmentLineList().size(),
            costAdjustmentAssertLineList.size());

        if (costAdjustmentAssertLineList.get(0).getStatus().equals("VO")) {
          OBCriteria<CostAdjustmentLine> criteria = OBDal.getInstance()
              .createCriteria(CostAdjustmentLine.class);
          criteria.addEqual(CostAdjustmentLine.PROPERTY_INVENTORYTRANSACTION,
              costAdjustmentAssertLineList.get(0).getMaterialTransaction());
          criteria.addEqual(CostAdjustmentLine.PROPERTY_ADJUSTMENTAMOUNT,
              costAdjustmentAssertLineList.get(0).getAmount().negate());
          criteria.addNotEqual(CostAdjustmentLine.PROPERTY_COSTADJUSTMENT, costAdjustment);
          criteria.setMaxResults(1);
          assertEquals(costAdjustment.getCostAdjustmentCancel(),
              ((CostAdjustmentLine) criteria.uniqueResult()).getCostAdjustment()
                  .getCostAdjustmentCancel() != null ? null
                      : criteria.list().get(0).getCostAdjustment());
        } else {
          assertEquals(costAdjustment.getCostAdjustmentCancel(), null);
        }

        // Assert Cost Adjustment lines data
        int j = 0;
        for (CostAdjustmentLine costAdjustmentLine : costAdjustment.getCostAdjustmentLineList()) {

          CostAdjustmentAssert costAdjustmentAssertLine = costAdjustmentAssertLineList.get(j);
          assertGeneralData(costAdjustment);

          assertEquals(costAdjustmentLine.getCostAdjustment(), costAdjustment);
          assertEquals(costAdjustmentLine.getInventoryTransaction(),
              costAdjustmentAssertLine.getMaterialTransaction());
          assertEquals(costAdjustmentLine.getLineNo(), Long.valueOf((j + 1) * 10L));

          assertEquals(costAdjustmentLine.getAdjustmentAmount().setScale(2, RoundingMode.HALF_UP),
              costAdjustmentAssertLine.getAmount().setScale(2, RoundingMode.HALF_UP));
          assertEquals(costAdjustmentLine.isSource(), costAdjustmentAssertLine.isSource());
          assertEquals(costAdjustmentLine.isUnitCost(), costAdjustmentAssertLine.isUnit());
          assertEquals(formatDate(costAdjustmentLine.getAccountingDate()),
              formatDate(DateUtils.addDays(new Date(), costAdjustmentAssertLine.getDay())));
          assertTrue(costAdjustmentLine.isRelatedTransactionAdjusted());
          assertEquals(costAdjustmentLine.getCurrency(), costAdjustmentAssertLine.getCurrency());

          if (costAdjustmentAssertLine.getType().equals("NSC")) {
            assertFalse(costAdjustmentLine.isBackdatedTrx());
            assertTrue(costAdjustmentLine.isNegativeStockCorrection());
          } else if (costAdjustmentAssertLine.getType().equals("BDT")) {
            assertTrue(costAdjustmentLine.isBackdatedTrx());
            assertFalse(costAdjustmentLine.isNegativeStockCorrection());
          } else {
            assertFalse(costAdjustmentLine.isBackdatedTrx());
            assertFalse(costAdjustmentLine.isNegativeStockCorrection());
          }

          if (costAdjustmentAssertLine.getAmount()
              .setScale(2, RoundingMode.HALF_UP)
              .equals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
              || (costAdjustmentAssertLine.getType().equals("LC")
                  && !costAdjustmentAssertLine.isNeedPosting())) {
            assertFalse(costAdjustmentLine.isNeedsPosting());
          } else {
            assertTrue(costAdjustmentLine.isNeedsPosting());
          }

          assertParentCostAdjustmentLine(costAdjustment, costAdjustmentAssertLineList, j,
              costAdjustmentLine, costAdjustmentAssertLine);

          j++;
        }
        i++;
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  private static void assertParentCostAdjustmentLine(CostAdjustment costAdjustment,
      List<CostAdjustmentAssert> costAdjustmentAssertLineList, int j,
      CostAdjustmentLine costAdjustmentLine, CostAdjustmentAssert costAdjustmentAssertLine) {
    if (j == 0
        || (j == 1 && costAdjustmentAssertLine.isSource()
            && !costAdjustmentLine.getInventoryTransaction()
                .getStorageBin()
                .equals(costAdjustment.getCostAdjustmentLineList()
                    .get(j - 1)
                    .getInventoryTransaction()
                    .getStorageBin()))
        || (j == 1 && !costAdjustmentLine.getInventoryTransaction()
            .getProduct()
            .equals(costAdjustment.getCostAdjustmentLineList()
                .get(j - 1)
                .getInventoryTransaction()
                .getProduct()))
        || (j == 2
            && !costAdjustmentLine.getInventoryTransaction()
                .getProduct()
                .equals(costAdjustment.getCostAdjustmentLineList()
                    .get(j - 2)
                    .getInventoryTransaction()
                    .getProduct())
            && !costAdjustmentLine.getInventoryTransaction()
                .getProduct()
                .equals(costAdjustment.getCostAdjustmentLineList()
                    .get(j - 1)
                    .getInventoryTransaction()
                    .getProduct()))
        || (j == 3 && !costAdjustmentLine.getInventoryTransaction().getProduct().isBillOfMaterials()
            && !costAdjustmentLine.getInventoryTransaction()
                .getProduct()
                .equals(costAdjustment.getCostAdjustmentLineList()
                    .get(j - 3)
                    .getInventoryTransaction()
                    .getProduct())
            && !costAdjustmentLine.getInventoryTransaction()
                .getProduct()
                .equals(costAdjustment.getCostAdjustmentLineList()
                    .get(j - 2)
                    .getInventoryTransaction()
                    .getProduct())
            && !costAdjustmentLine.getInventoryTransaction()
                .getProduct()
                .equals(costAdjustment.getCostAdjustmentLineList()
                    .get(j - 1)
                    .getInventoryTransaction()
                    .getProduct()))) {
      assertEquals(costAdjustmentLine.getParentCostAdjustmentLine(), null);
    } else if (costAdjustmentLine.getInventoryTransaction()
        .getProduct()
        .equals(costAdjustment.getCostAdjustmentLineList()
            .get(0)
            .getInventoryTransaction()
            .getProduct())
        && (costAdjustmentLine.getInventoryTransaction()
            .getStorageBin()
            .equals(costAdjustment.getCostAdjustmentLineList()
                .get(0)
                .getInventoryTransaction()
                .getStorageBin())
            || costAdjustmentAssertLineList.size() == 2)) {
      assertEquals(costAdjustmentLine.getParentCostAdjustmentLine(),
          costAdjustment.getCostAdjustmentLineList().get(0));
    } else if (costAdjustmentLine.getInventoryTransaction()
        .getProduct()
        .equals(costAdjustment.getCostAdjustmentLineList()
            .get(1)
            .getInventoryTransaction()
            .getProduct())
        && (costAdjustmentLine.getInventoryTransaction()
            .getStorageBin()
            .equals(costAdjustment.getCostAdjustmentLineList()
                .get(1)
                .getInventoryTransaction()
                .getStorageBin())
            || costAdjustmentAssertLineList.size() == 3)) {
      assertEquals(costAdjustmentLine.getParentCostAdjustmentLine(),
          costAdjustment.getCostAdjustmentLineList().get(1));
    } else if ((costAdjustmentLine.getInventoryTransaction()
        .getProduct()
        .equals(costAdjustment.getCostAdjustmentLineList()
            .get(2)
            .getInventoryTransaction()
            .getProduct())
        && (costAdjustmentLine.getInventoryTransaction()
            .getStorageBin()
            .equals(costAdjustment.getCostAdjustmentLineList()
                .get(2)
                .getInventoryTransaction()
                .getStorageBin())
            || costAdjustmentAssertLineList.size() == 4))
        || costAdjustmentLine.getInventoryTransaction().getProduct().isBillOfMaterials()
            && costAdjustmentLine.getAdjustmentAmount()
                .equals(costAdjustment.getCostAdjustmentLineList().get(2).getAdjustmentAmount())) {
      assertEquals(costAdjustmentLine.getParentCostAdjustmentLine(),
          costAdjustment.getCostAdjustmentLineList().get(2));
    } else if (costAdjustmentLine.getInventoryTransaction().getProduct().isBillOfMaterials()) {
      assertEquals(costAdjustmentLine.getParentCostAdjustmentLine(),
          costAdjustment.getCostAdjustmentLineList().get(4));
    } else {
      assertEquals(costAdjustmentLine.getParentCostAdjustmentLine(),
          costAdjustment.getCostAdjustmentLineList().get(3));
    }
  }

  // Assert amounts and dates of a posted document
  @SuppressWarnings("unchecked")
  public static void assertDocumentPost(BaseOBObject document, String productId,
      List<DocumentPostAssert> documentPostAssertList) {
    try {

      assertEquals(document.get("posted"), "Y");

      final OBCriteria<Table> criteria1 = OBDal.getInstance().createCriteria(Table.class);
      criteria1.addEqual(Table.PROPERTY_NAME, document.getEntityName());
      criteria1.setMaxResults(1);
      Table table = (Table) criteria1.uniqueResult();

      final OBCriteria<AccountingFact> criteria2 = OBDal.getInstance()
          .createCriteria(AccountingFact.class);
      criteria2.addEqual(AccountingFact.PROPERTY_RECORDID, document.getId());
      criteria2.addEqual(AccountingFact.PROPERTY_TABLE, table);
      criteria2.addOrderBy(AccountingFact.PROPERTY_SEQUENCENUMBER, true);
      List<AccountingFact> accountingFactList = criteria2.list();
      String groupId = accountingFactList.get(0).getGroupID();
      Date previousDate = accountingFactList.get(0).getAccountingDate();

      assertEquals(accountingFactList.size(), documentPostAssertList.size());

      int i = 0;
      for (AccountingFact accountingFact : accountingFactList) {

        if (previousDate != accountingFact.getAccountingDate()) {
          groupId = accountingFact.getGroupID();
          previousDate = accountingFact.getAccountingDate();
        }

        String lineListProperty = Character.toLowerCase(document.getEntityName().charAt(0))
            + document.getEntityName().substring(1) + "LineList";

        BaseOBObject line = null;
        if (document.getEntityName().equals(ReceiptInvoiceMatch.ENTITY_NAME)) {
          if (i % 2 == 0) {
            line = ((ReceiptInvoiceMatch) document).getGoodsShipmentLine();
          } else {
            line = ((ReceiptInvoiceMatch) document).getInvoiceLine();
          }
        } else if (document.getEntityName().equals(ProductionTransaction.ENTITY_NAME)) {
          StringBuffer where = new StringBuffer();
          where.append(" as t1 ");
          where.append("\n left join t1." + ProductionLine.PROPERTY_PRODUCTIONPLAN + " t2");
          where.append(
              "\n where t2." + ProductionPlan.PROPERTY_PRODUCTION + " = :productionTransaction");
          where.append("\n order by t1." + ProductionLine.PROPERTY_LINENO);
          OBQuery<ProductionLine> hql = OBDal.getInstance()
              .createQuery(ProductionLine.class, where.toString());
          hql.setNamedParameter("productionTransaction",
              OBDal.getInstance().get(ProductionTransaction.class, document.getId()));
          line = hql.list().get(i / 2);
        } else if (document.getEntityName().equals(CostAdjustment.ENTITY_NAME)) {
          final OBCriteria<CostAdjustmentLine> criteria3 = OBDal.getInstance()
              .createCriteria(CostAdjustmentLine.class);
          criteria3.addEqual(CostAdjustmentLine.PROPERTY_COSTADJUSTMENT, document);
          criteria3.addEqual(CostAdjustmentLine.PROPERTY_NEEDSPOSTING, true);
          criteria3.addOrderBy(CostAdjustmentLine.PROPERTY_LINENO, true);
          line = criteria3.list().get(i / 2);
        } else if (productId != null
            && (productId.equals(TestCostingConstants.LANDEDCOSTTYPE_FEES_ID)
                || productId.equals(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID)
                || productId.equals(TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID))) {
          line = ((List<BaseOBObject>) OBDal.getInstance()
              .get(document.getClass(), document.getId())
              .get(lineListProperty)).get(0);
        } else if (document.getEntityName().equals(LandedCost.ENTITY_NAME)) {
          StringBuffer where = new StringBuffer();
          where.append(" as t1 ");
          where.append("\n join t1." + LCReceiptLineAmt.PROPERTY_LANDEDCOSTRECEIPT + " t2");
          where.append("\n join t1." + LCReceiptLineAmt.PROPERTY_LANDEDCOSTCOST + " t3");
          where.append("\n join t1." + LCReceiptLineAmt.PROPERTY_GOODSSHIPMENTLINE + " t4");
          where.append("\n left join t4." + ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT + " t5");
          where.append("\n where t2." + LCReceipt.PROPERTY_LANDEDCOST + " = :landedCost");
          where.append("\n order by t3." + LandedCostCost.PROPERTY_LINENO);
          where.append("\n , t5." + ShipmentInOut.PROPERTY_DOCUMENTNO);
          where.append("\n , t4." + ShipmentInOutLine.PROPERTY_LINENO);
          OBQuery<LCReceiptLineAmt> hql = OBDal.getInstance()
              .createQuery(LCReceiptLineAmt.class, where.toString());
          LandedCost landedCost = OBDal.getInstance().get(LandedCost.class, document.getId());
          hql.setNamedParameter("landedCost", landedCost);
          line = hql.list().get(i / 2);
        } else if (document.getEntityName().equals(LandedCostCost.ENTITY_NAME)) {
          if (((LandedCostCost) document).getLandedCostMatchedList().size() == 1) {
            line = ((LandedCostCost) document).getLandedCostMatchedList().get(0);
          } else if (!((LandedCostCost) document).getAmount()
              .setScale(2, RoundingMode.HALF_UP)
              .equals(
                  ((LandedCostCost) document).getMatchingAmount().setScale(2, RoundingMode.HALF_UP))
              && ((LandedCostCost) document).isMatchingAdjusted()) {
            if (i == 0) {
              line = ((LandedCostCost) document).getLandedCostMatchedList().get(0);
            } else {
              line = ((LandedCostCost) document).getLandedCostMatchedList().get(1);
            }
          } else {
            line = ((LandedCostCost) document).getLandedCostMatchedList().get(i / 2);
          }
        } else if (document.getEntityName().equals(Invoice.ENTITY_NAME) && i > 0) {
          line = ((List<BaseOBObject>) OBDal.getInstance()
              .get(document.getClass(), document.getId())
              .get(lineListProperty)).get(i - 1);
        } else {
          line = ((List<BaseOBObject>) OBDal.getInstance()
              .get(document.getClass(), document.getId())
              .get(lineListProperty)).get(i / 2);
        }
        DocumentPostAssert documentPostAssert = documentPostAssertList.get(i);
        assertGeneralData(accountingFact);

        /* Accounting window fields assert */

        assertEquals(accountingFact.getTable(), table);
        assertEquals(accountingFact.getRecordID(), document.getId());
        assertEquals(accountingFact.getAccountingSchema().getName(), "Main US/A/Euro");

        assertEquals(accountingFact.getAccount().getSearchKey(), documentPostAssert.getAccount());
        assertEquals(accountingFact.getQuantity(), documentPostAssert.getQuantity());

        BigDecimal rate;
        if ((productId != null && productId.equals(TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID))
            || (document.getEntityName().equals(Invoice.ENTITY_NAME)
                && ((Invoice) document).getCurrency()
                    .getId()
                    .equals(TestCostingConstants.DOLLAR_ID))
            || (document.getEntityName().equals(LandedCost.ENTITY_NAME)
                && ((LCReceiptLineAmt) line).getLandedCostCost()
                    .getLandedCostType()
                    .equals(OBDal.getInstance()
                        .get(Product.class, TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID)
                        .getLandedCostTypeList()
                        .get(0)))
            || (document.getEntityName().equals(LandedCostCost.ENTITY_NAME)
                && ((LCMatched) line).getInvoiceLine().getProduct() != null
                && ((LCMatched) line).getInvoiceLine()
                    .getProduct()
                    .getId()
                    .equals(TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID))
            || (!document.getEntityName().equals(LandedCostCost.ENTITY_NAME)
                && !document.getEntityName().equals(LandedCost.ENTITY_NAME)
                && documentPostAssert.getProductId() != null
                && !OBDal.getInstance()
                    .get(Product.class, documentPostAssert.getProductId())
                    .getPricingProductPriceList()
                    .isEmpty()
                && OBDal.getInstance()
                    .get(Product.class, documentPostAssert.getProductId())
                    .getPricingProductPriceList()
                    .get(0)
                    .getPriceListVersion()
                    .equals(OBDal.getInstance()
                        .get(Product.class, TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID)
                        .getPricingProductPriceList()
                        .get(0)
                        .getPriceListVersion()))) {

          if (document.getEntityName().equals(Invoice.ENTITY_NAME)
              && ((Invoice) document).getCurrencyConversionRateDocList().size() != 0) {
            rate = ((Invoice) document).getCurrencyConversionRateDocList().get(0).getRate();
          } else {
            Calendar calendar = Calendar.getInstance();
            calendar.set(9999, 0, 1);
            OBCriteria<ConversionRate> criteria = OBDal.getInstance()
                .createCriteria(ConversionRate.class);
            criteria.addEqual(ConversionRate.PROPERTY_CLIENT,
                OBDal.getInstance().get(Client.class, TestCostingConstants.QATESTING_CLIENT_ID));
            criteria.addEqual(ConversionRate.PROPERTY_CURRENCY,
                OBDal.getInstance().get(Currency.class, TestCostingConstants.DOLLAR_ID));
            criteria.addEqual(ConversionRate.PROPERTY_TOCURRENCY,
                OBDal.getInstance().get(Currency.class, TestCostingConstants.EURO_ID));
            criteria.addGreaterOrEqual(ConversionRate.PROPERTY_VALIDTODATE, calendar.getTime());
            criteria.setMaxResults(1);
            rate = ((ConversionRate) criteria.uniqueResult()).getMultipleRateBy();
          }
        }

        else {
          rate = BigDecimal.ONE;
        }

        assertEquals(accountingFact.getDebit().setScale(2, RoundingMode.HALF_UP),
            documentPostAssert.getDebit()
                .multiply(rate)
                .setScale(2,
                    document.getEntityName().equals(LandedCost.ENTITY_NAME) ? RoundingMode.HALF_EVEN
                        : RoundingMode.HALF_UP));
        assertEquals(accountingFact.getCredit().setScale(2, RoundingMode.HALF_UP),
            documentPostAssert.getCredit()
                .multiply(rate)
                .setScale(2,
                    document.getEntityName().equals(LandedCost.ENTITY_NAME) ? RoundingMode.HALF_EVEN
                        : RoundingMode.HALF_UP));

        if ((productId != null && productId.equals(TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID))
            || (document.getEntityName().equals(Invoice.ENTITY_NAME)
                && ((Invoice) document).getCurrency()
                    .getId()
                    .equals(TestCostingConstants.DOLLAR_ID))
            || (document.getEntityName().equals(LandedCost.ENTITY_NAME)
                && ((LCReceiptLineAmt) line).getLandedCostCost()
                    .getLandedCostType()
                    .equals(OBDal.getInstance()
                        .get(Product.class, TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID)
                        .getLandedCostTypeList()
                        .get(0)))
            || (document.getEntityName().equals(LandedCostCost.ENTITY_NAME)
                && ((LCMatched) line).getInvoiceLine().getProduct() != null
                && ((LCMatched) line).getInvoiceLine()
                    .getProduct()
                    .getId()
                    .equals(TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID))) {
          rate = BigDecimal.ONE;
        }

        else if ((document.getEntityName().equals(ShipmentInOut.ENTITY_NAME)
            || document.getEntityName().equals(CostAdjustment.ENTITY_NAME)
            || (document.getEntityName().equals(ReceiptInvoiceMatch.ENTITY_NAME)
                && (line.getEntityName().equals(ShipmentInOutLine.ENTITY_NAME)
                    || (line.getEntityName().equals(InvoiceLine.ENTITY_NAME)
                        && ((InvoiceLine) line).getInvoice()
                            .getCurrency()
                            .getId()
                            .equals(TestCostingConstants.DOLLAR_ID)))))
            && OBDal.getInstance()
                .get(Organization.class, TestCostingConstants.SPAIN_ORGANIZATION_ID)
                .getCurrency() != null
            && OBDal.getInstance()
                .get(Organization.class, TestCostingConstants.SPAIN_ORGANIZATION_ID)
                .getCurrency()
                .getId()
                .equals(TestCostingConstants.DOLLAR_ID)
            && !accountingFact.getCurrency()
                .getId()
                .equals(accountingFact.getAccountingSchema().getCurrency().getId())) {
          Calendar calendar = Calendar.getInstance();
          calendar.set(9999, 0, 1);
          OBCriteria<ConversionRate> criteria = OBDal.getInstance()
              .createCriteria(ConversionRate.class);
          criteria.addEqual(ConversionRate.PROPERTY_CLIENT,
              OBDal.getInstance().get(Client.class, TestCostingConstants.QATESTING_CLIENT_ID));
          criteria.addEqual(ConversionRate.PROPERTY_CURRENCY,
              OBDal.getInstance().get(Currency.class, TestCostingConstants.EURO_ID));
          criteria.addEqual(ConversionRate.PROPERTY_TOCURRENCY,
              OBDal.getInstance().get(Currency.class, TestCostingConstants.DOLLAR_ID));
          criteria.addGreaterOrEqual(ConversionRate.PROPERTY_VALIDTODATE, calendar.getTime());
          criteria.setMaxResults(1);
          rate = ((ConversionRate) criteria.uniqueResult()).getMultipleRateBy();
        }

        else if (document.getEntityName().equals(ReceiptInvoiceMatch.ENTITY_NAME)
            && line.getEntityName().equals(InvoiceLine.ENTITY_NAME)
            && !((InvoiceLine) line).getInvoice().getCurrencyConversionRateDocList().isEmpty()
            && BigDecimal.ZERO.compareTo(((InvoiceLine) line).getInvoice()
                .getCurrencyConversionRateDocList()
                .get(0)
                .getRate()) != 0) {
          rate = BigDecimal.ONE.divide(((InvoiceLine) line).getInvoice()
              .getCurrencyConversionRateDocList()
              .get(0)
              .getRate());
        }

        assertEquals(accountingFact.getForeignCurrencyDebit().setScale(2, RoundingMode.HALF_UP),
            documentPostAssert.getDebit()
                .multiply(rate)
                .setScale(2,
                    document.getEntityName().equals(LandedCost.ENTITY_NAME) ? RoundingMode.HALF_EVEN
                        : RoundingMode.HALF_UP));
        assertEquals(accountingFact.getForeignCurrencyCredit().setScale(2, RoundingMode.HALF_UP),
            documentPostAssert.getCredit()
                .multiply(rate)
                .setScale(2,
                    document.getEntityName().equals(LandedCost.ENTITY_NAME) ? RoundingMode.HALF_EVEN
                        : RoundingMode.HALF_UP));

        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTime(accountingFact.getAccountingDate());
        calendar1.set(Calendar.DAY_OF_MONTH, calendar1.getActualMinimum(Calendar.DAY_OF_MONTH));
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(accountingFact.getAccountingDate());
        calendar2.set(Calendar.DAY_OF_MONTH, calendar2.getActualMaximum(Calendar.DAY_OF_MONTH));
        final OBCriteria<Period> criteria3 = OBDal.getInstance().createCriteria(Period.class);
        criteria3.addEqual(Period.PROPERTY_STARTINGDATE, calendar1.getTime());
        criteria3.addEqual(Period.PROPERTY_ENDINGDATE, calendar2.getTime());
        criteria3.setMaxResults(1);
        assertEquals(accountingFact.getPeriod(), (Period) criteria3.uniqueResult());

        if (document.getEntityName().equals(CostAdjustment.ENTITY_NAME)) {
          assertEquals(formatDate(accountingFact.getTransactionDate()), formatDate(new Date()));
          assertEquals(formatDate(accountingFact.getAccountingDate()),
              formatDate(((CostAdjustmentLine) line).getAccountingDate()));
          if (((CostAdjustmentLine) line).getInventoryTransaction()
              .getGoodsShipmentLine() != null) {
            assertEquals(accountingFact.getBusinessPartner(),
                ((CostAdjustmentLine) line).getInventoryTransaction()
                    .getGoodsShipmentLine()
                    .getShipmentReceipt()
                    .getBusinessPartner());
          } else {
            assertEquals(accountingFact.getBusinessPartner(), null);
          }
        } else if (document.getEntityName().equals(InventoryCount.ENTITY_NAME)) {
          assertEquals(formatDate(accountingFact.getTransactionDate()),
              formatDate(((InventoryCount) document).getMovementDate()));
          assertEquals(formatDate(accountingFact.getAccountingDate()),
              formatDate(((InventoryCount) document).getMovementDate()));
          assertEquals(accountingFact.getBusinessPartner(), null);
        } else if (document.getEntityName().equals(ReceiptInvoiceMatch.ENTITY_NAME)) {
          assertEquals(formatDate(accountingFact.getTransactionDate()),
              formatDate(((ReceiptInvoiceMatch) document).getTransactionDate()));
          assertEquals(formatDate(accountingFact.getAccountingDate()),
              formatDate(((ReceiptInvoiceMatch) document).getTransactionDate()));
          assertEquals(accountingFact.getBusinessPartner(),
              ((ReceiptInvoiceMatch) document).getInvoiceLine().getBusinessPartner());
        } else if (document.getEntityName().equals(InternalMovement.ENTITY_NAME)) {
          assertEquals(formatDate(accountingFact.getTransactionDate()),
              formatDate(((InternalMovement) document).getMovementDate()));
          assertEquals(formatDate(accountingFact.getAccountingDate()),
              formatDate(((InternalMovement) document).getMovementDate()));
          assertEquals(accountingFact.getBusinessPartner(), null);
        } else if (document.getEntityName().equals(InternalConsumption.ENTITY_NAME)) {
          assertEquals(formatDate(accountingFact.getTransactionDate()),
              formatDate(((InternalConsumption) document).getMovementDate()));
          assertEquals(formatDate(accountingFact.getAccountingDate()),
              formatDate(((InternalConsumption) document).getMovementDate()));
          assertEquals(accountingFact.getBusinessPartner(), null);
        } else if (document.getEntityName().equals(ProductionTransaction.ENTITY_NAME)) {
          assertEquals(formatDate(accountingFact.getTransactionDate()),
              formatDate(((ProductionTransaction) document).getMovementDate()));
          assertEquals(formatDate(accountingFact.getAccountingDate()),
              formatDate(((ProductionTransaction) document).getMovementDate()));
          assertEquals(accountingFact.getBusinessPartner(), null);
        } else if (document.getEntityName().equals(LandedCost.ENTITY_NAME)) {
          assertEquals(formatDate(accountingFact.getTransactionDate()),
              formatDate(((LandedCost) document).getReferenceDate()));
          final Date shipmentAcctDate = (OBDal.getInstance()
              .get(ShipmentInOutLine.class,
                  ((LCReceiptLineAmt) line).getGoodsShipmentLine().getId())).getShipmentReceipt()
                      .getAccountingDate();
          Date accountingDate = ((LandedCost) document).getReferenceDate();
          if (shipmentAcctDate.after(accountingDate)) {
            accountingDate = shipmentAcctDate;
          }
          assertEquals(formatDate(accountingFact.getAccountingDate()), formatDate(accountingDate));
          if (i % 2 == 0) {
            assertEquals(accountingFact.getBusinessPartner(),
                OBDal.getInstance()
                    .get(ShipmentInOutLine.class,
                        ((LCReceiptLineAmt) line).getGoodsShipmentLine().getId())
                    .getBusinessPartner());
          } else {
            assertEquals(accountingFact.getBusinessPartner(), null);
          }
        } else if (document.getEntityName().equals(LandedCostCost.ENTITY_NAME)) {
          assertEquals(formatDate(accountingFact.getTransactionDate()),
              formatDate(((LandedCostCost) document).getAccountingDate()));
          final Date maxInvoiceAcctDate = getMaxInvoiceAccountingDate((LandedCostCost) document);
          Date accountingDate = ((LandedCostCost) document).getAccountingDate();
          if (maxInvoiceAcctDate.after(accountingDate)) {
            accountingDate = maxInvoiceAcctDate;
          }
          assertEquals(formatDate(accountingFact.getAccountingDate()), formatDate(accountingDate));
          if (i == 0 || (documentPostAssert.getProductId() != null
              && OBDal.getInstance()
                  .get(InvoiceLine.class,
                      ((LandedCostCost) document).getLandedCostMatchedList()
                          .get(0)
                          .getInvoiceLine()
                          .getId())
                  .getProduct() != null
              && documentPostAssert.getProductId()
                  .equals(OBDal.getInstance()
                      .get(InvoiceLine.class,
                          ((LandedCostCost) document).getLandedCostMatchedList()
                              .get(0)
                              .getInvoiceLine()
                              .getId())
                      .getProduct()
                      .getId()))) {
            assertEquals(accountingFact.getBusinessPartner(),
                OBDal.getInstance()
                    .get(InvoiceLine.class,
                        ((LandedCostCost) document).getLandedCostMatchedList()
                            .get(0)
                            .getInvoiceLine()
                            .getId())
                    .getBusinessPartner());
          } else {
            assertEquals(accountingFact.getBusinessPartner(), null);
          }
        } else {
          assertEquals(formatDate(accountingFact.getTransactionDate()),
              formatDate((Date) document.get("accountingDate")));
          assertEquals(formatDate(accountingFact.getAccountingDate()),
              formatDate((Date) document.get("accountingDate")));
          assertEquals(accountingFact.getBusinessPartner(), document.get("businessPartner"));
        }

        if ((productId != null && productId.equals(TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID))
            || (document.getEntityName().equals(Invoice.ENTITY_NAME)
                && ((Invoice) document).getCurrency()
                    .getId()
                    .equals(TestCostingConstants.DOLLAR_ID))
            || (document.getEntityName().equals(ReceiptInvoiceMatch.ENTITY_NAME)
                && line.getEntityName().equals(InvoiceLine.ENTITY_NAME)
                && ((InvoiceLine) line).getInvoice()
                    .getCurrency()
                    .getId()
                    .equals(TestCostingConstants.DOLLAR_ID))
            || (document.getEntityName().equals(LandedCost.ENTITY_NAME)
                && ((LCReceiptLineAmt) line).getLandedCostCost()
                    .getLandedCostType()
                    .equals(OBDal.getInstance()
                        .get(Product.class, TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID)
                        .getLandedCostTypeList()
                        .get(0)))
            || (document.getEntityName().equals(LandedCostCost.ENTITY_NAME)
                && ((LCMatched) line).getInvoiceLine().getProduct() != null
                && ((LCMatched) line).getInvoiceLine()
                    .getProduct()
                    .getId()
                    .equals(TestCostingConstants.LANDEDCOSTTYPE_USD_COST_ID))
            || (!document.getEntityName().equals(Invoice.ENTITY_NAME)
                && !(document.getEntityName().equals(ReceiptInvoiceMatch.ENTITY_NAME)
                    && line.getEntityName().equals(InvoiceLine.ENTITY_NAME))
                && OBDal.getInstance()
                    .get(Organization.class, TestCostingConstants.SPAIN_ORGANIZATION_ID)
                    .getCurrency() != null
                && OBDal.getInstance()
                    .get(Organization.class, TestCostingConstants.SPAIN_ORGANIZATION_ID)
                    .getCurrency()
                    .getId()
                    .equals(TestCostingConstants.DOLLAR_ID)
                && !accountingFact.getCurrency()
                    .getId()
                    .equals(accountingFact.getAccountingSchema().getCurrency().getId()))) {
          assertEquals(accountingFact.getCurrency(),
              OBDal.getInstance().get(Currency.class, TestCostingConstants.DOLLAR_ID));
        } else {
          assertEquals(accountingFact.getCurrency(),
              OBDal.getInstance().get(Currency.class, TestCostingConstants.EURO_ID));
        }

        if (productId != null
            && productId.equals(TestCostingConstants.LANDEDCOSTTYPE_TRANSPORTATION_COST_ID)) {
          if (i == 0) {
            assertEquals(accountingFact.getProduct(), null);
            assertEquals(accountingFact.getUOM(), null);
            assertEquals(accountingFact.getTax(), null);
          } else if (i == 1) {
            assertEquals(accountingFact.getProduct(), null);
            assertEquals(accountingFact.getUOM(), null);
            assertEquals(accountingFact.getLineID(), null);
            assertEquals(accountingFact.getRecordID2(), null);

            OBCriteria<TaxRate> criteria = OBDal.getInstance().createCriteria(TaxRate.class);
            criteria.addEqual(TaxRate.PROPERTY_TAXCATEGORY,
                OBDal.getInstance().get(Product.class, productId).getTaxCategory());
            criteria.addEqual(TaxRate.PROPERTY_ORGANIZATION, OBDal.getInstance()
                .get(Organization.class, TestCostingConstants.SPAIN_ORGANIZATION_ID));
            criteria.setMaxResults(1);
            assertEquals(accountingFact.getTax(), (TaxRate) criteria.uniqueResult());
          } else {
            assertEquals(accountingFact.getProduct().getId(), productId);
            assertEquals(accountingFact.getUOM(), line.get("uOM"));
            assertEquals(accountingFact.getLineID(), line.getId());
            assertEquals(accountingFact.getRecordID2(), null);
            assertEquals(accountingFact.getTax(), null);
          }
        }

        else {
          if (document.getEntityName().equals(Invoice.ENTITY_NAME) && i == 0) {
            assertEquals(accountingFact.getProduct(), null);
            assertEquals(accountingFact.getUOM(), null);
            assertEquals(accountingFact.getTax(), null);
          } else {
            if (productId == null) {
              assertEquals(accountingFact.getProduct(),
                  documentPostAssert.getProductId() == null ? null
                      : OBDal.getInstance().get(Product.class, documentPostAssert.getProductId()));
            } else {
              assertEquals(accountingFact.getProduct().getId(), productId);
            }
            if (line.getEntity().getProperty("uOM", false) == null) {
              assertEquals(accountingFact.getUOM(), null);
            } else {
              assertEquals(accountingFact.getUOM(), line.get("uOM"));
            }
            if (!document.getEntityName().equals(LandedCost.ENTITY_NAME)) {
              assertEquals(accountingFact.getLineID(), line.getId());
            }
            assertEquals(accountingFact.getRecordID2(), null);
            assertEquals(accountingFact.getTax(), null);
          }
        }

        assertEquals(accountingFact.getProject(), null);
        assertEquals(accountingFact.getCostcenter(), null);
        assertEquals(accountingFact.getAsset(), null);
        assertEquals(accountingFact.getStDimension(), null);
        assertEquals(accountingFact.getNdDimension(), null);

        /* Rest of fields assert */

        if (document.getEntityName().equals(ShipmentInOut.ENTITY_NAME)) {
          assertEquals(accountingFact.getGLCategory().getName(), "Material Management");
        } else if (document.getEntityName().equals(Invoice.ENTITY_NAME)) {
          assertEquals(accountingFact.getGLCategory().getName(), "AP Invoice");
        } else if (document.getEntityName().equals(CostAdjustment.ENTITY_NAME)) {
          assertEquals(accountingFact.getGLCategory().getName(), "None");
        } else {
          assertEquals(accountingFact.getGLCategory().getName(), "Standard");
        }

        assertEquals(accountingFact.getPostingType(), "A");

        if (document.getEntityName().equals(ReceiptInvoiceMatch.ENTITY_NAME)) {
          assertEquals(accountingFact.getStorageBin(), null);
        } else if (document.getEntityName().equals(InternalMovement.ENTITY_NAME)) {
          if (i % 2 == 0) {
            assertEquals(accountingFact.getStorageBin(),
                line.get(InternalMovementLine.PROPERTY_STORAGEBIN));
          } else {
            assertEquals(accountingFact.getStorageBin(),
                line.get(InternalMovementLine.PROPERTY_NEWSTORAGEBIN));
          }
        } else if (line.getEntity().getProperty("storageBin", false) == null) {
          assertEquals(accountingFact.getStorageBin(), null);
        } else {
          assertEquals(accountingFact.getStorageBin(), line.get("storageBin"));
        }

        if (document.getEntityName().equals(InventoryCount.ENTITY_NAME)) {
          assertEquals(accountingFact.getDocumentType(), null);
          assertEquals(accountingFact.getDocumentCategory(), "MMI");
        } else if (document.getEntityName().equals(ReceiptInvoiceMatch.ENTITY_NAME)) {
          assertEquals(accountingFact.getDocumentType(), null);
          assertEquals(accountingFact.getDocumentCategory(), "MXI");
        } else if (document.getEntityName().equals(InternalMovement.ENTITY_NAME)) {
          assertEquals(accountingFact.getDocumentType(), null);
          assertEquals(accountingFact.getDocumentCategory(), "MMM");
        } else if (document.getEntityName().equals(InternalConsumption.ENTITY_NAME)) {
          assertEquals(accountingFact.getDocumentType(), null);
          assertEquals(accountingFact.getDocumentCategory(), "MIC");
        } else if (document.getEntityName().equals(ProductionTransaction.ENTITY_NAME)) {
          assertEquals(accountingFact.getDocumentType(), null);
          assertEquals(accountingFact.getDocumentCategory(), "MMP");
        } else if (document.getEntityName().equals(LandedCost.ENTITY_NAME)) {
          assertEquals(accountingFact.getDocumentType(), null);
          assertEquals(accountingFact.getDocumentCategory(), "LDC");
        } else if (document.getEntityName().equals(LandedCostCost.ENTITY_NAME)) {
          assertEquals(accountingFact.getDocumentType(), null);
          assertEquals(accountingFact.getDocumentCategory(), "LCC");
        } else {
          assertEquals(accountingFact.getDocumentType(), document.get("documentType"));
          assertEquals(accountingFact.getDocumentCategory(),
              ((DocumentType) document.get("documentType")).getDocumentCategory());
        }

        assertEquals(accountingFact.getSalesRegion(), null);
        assertEquals(accountingFact.getSalesCampaign(), null);
        assertEquals(accountingFact.getActivity(), null);
        assertEquals(accountingFact.getGroupID(), groupId);
        assertEquals(accountingFact.getType(), "N");
        assertEquals(accountingFact.getValue(), documentPostAssert.getAccount());
        assertEquals(accountingFact.getWithholding(), null);
        assertFalse(accountingFact.isModify());
        assertEquals(accountingFact.getDateBalanced(), null);

        final OBCriteria<ElementValue> criteria4 = OBDal.getInstance()
            .createCriteria(ElementValue.class);
        criteria4
            .addEqual(ElementValue.PROPERTY_SEARCHKEY, documentPostAssert.getAccount());
        criteria4.setMaxResults(1);
        assertEquals(accountingFact.getAccountingEntryDescription(),
            ((ElementValue) criteria4.uniqueResult()).getDescription());

        i++;
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  private static Date getMaxInvoiceAccountingDate(final LandedCostCost lcCost) {
    //@formatter:off
    String hql = ""
        + " select max(i.accountingDate)"
        + " from LandedCostMatched lcm "
        + "   join lcm.invoiceLine il"
        + "   join il.invoice i"
        + " where lcm.active = true"
        + "   and lcm.landedCostCost.id = :lcCostId";
    //@formatter:on
    return (Date) OBDal.getInstance()
        .getSession()
        .createQuery(hql)
        .setParameter("lcCostId", lcCost.getId())
        .uniqueResult();
  }

  // Format date
  public static String formatDate(Date date) {
    return new SimpleDateFormat("yyyy-MM-dd").format(date);
  }

  public static OrderToReceiptResult executeOrderToReceiptFlow(final String productName,
      final BigDecimal purchasePrice, final BigDecimal quantity) {
    Product product = TestCostingUtils.createProduct(productName, purchasePrice);
    Order purchaseOrder = createPurchaseOrder(product, purchasePrice, quantity, 0);
    ShipmentInOut goodsReceipt = createMovementFromOrder(purchaseOrder.getId(), false, quantity,
        TestCostingConstants.LOCATOR_L01_ID, 0);
    completeDocument(goodsReceipt);
    return new OrderToReceiptResult(product, goodsReceipt);
  }

  public static void sortTransactionsByMovementQuantity(
      final List<MaterialTransaction> transactionList) {
    Collections.sort(transactionList, new Comparator<MaterialTransaction>() {
      @Override
      public int compare(MaterialTransaction firstTransaction,
          MaterialTransaction secondTransaction) {
        return firstTransaction.getMovementQuantity()
            .compareTo(secondTransaction.getMovementQuantity());
      }
    });
  }

  public static void assertTransactionsCountIsTwo(final String productId) {
    Product product = OBDal.getInstance().get(Product.class, productId);
    assertThat("The product should have 2 transactions",
        product.getMaterialMgmtMaterialTransactionList().size(), equalTo(2));
  }

  public static void assertTransactionsCostsAre3And30(final String productId) {
    Product product = OBDal.getInstance().get(Product.class, productId);
    sortTransactionsByMovementQuantity(product.getMaterialMgmtMaterialTransactionList());
    assertThat("The transaction cost should be 3",
        product.getMaterialMgmtMaterialTransactionList().get(0).getTransactionCost().intValue(),
        equalTo(3));
    assertThat("The transaction cost should be 30",
        product.getMaterialMgmtMaterialTransactionList().get(1).getTransactionCost().intValue(),
        equalTo(30));
  }

  public static void assertTransactionCostsAdjustmentsForTestIssue37279(final String productId) {
    Product product = OBDal.getInstance().get(Product.class, productId);
    sortTransactionsByMovementQuantity(product.getMaterialMgmtMaterialTransactionList());
    assertThat("There should be 2 cost adjustment lines for the first transaction",
        product.getMaterialMgmtMaterialTransactionList().get(0).getTransactionCostList().size(),
        equalTo(2));
    assertThat("There should be 2 cost adjustment lines for the second transaction",
        product.getMaterialMgmtMaterialTransactionList().get(1).getTransactionCostList().size(),
        equalTo(2));

    assertThat("The total cost for the first transaction should be 20",
        product.getMaterialMgmtMaterialTransactionList().get(0).getTotalCost().intValue(),
        equalTo(20));

    assertThat("The total cost for the first transaction should be 200",
        product.getMaterialMgmtMaterialTransactionList().get(1).getTotalCost().intValue(),
        equalTo(200));
  }

  public static void assertTransactionCostsAdjustmentsForTestIssue37279GoodsReceiptWithNoRelatedPurchaseOrder(
      final String productId) {
    Product product = OBDal.getInstance().get(Product.class, productId);
    sortTransactionsByMovementQuantity(product.getMaterialMgmtMaterialTransactionList());
    assertThat("There should be 2 cost adjustment lines for the first transaction",
        product.getMaterialMgmtMaterialTransactionList().get(0).getTransactionCostList().size(),
        equalTo(2));
    assertThat("There should be 1 cost adjustment line for the second transaction",
        product.getMaterialMgmtMaterialTransactionList().get(1).getTransactionCostList().size(),
        equalTo(1));

    assertThat("The total cost for the first transaction should be 20",
        product.getMaterialMgmtMaterialTransactionList().get(0).getTotalCost().intValue(),
        equalTo(20));

    assertThat("The total cost for the second transaction should be 30",
        product.getMaterialMgmtMaterialTransactionList().get(1).getTotalCost().intValue(),
        equalTo(30));
  }

  public static void proessInventoryCount(final InventoryCount physicalInventory) {
    InventoryCountProcess inventoryCountProcess = WeldUtils
        .getInstanceFromStaticBeanManager(InventoryCountProcess.class);
    physicalInventory.setProcessNow(true);
    OBDal.getInstance().save(physicalInventory);
    if (SessionHandler.isSessionHandlerPresent()) {
      SessionHandler.getInstance().commitAndStart();
    }
    inventoryCountProcess.processInventory(physicalInventory);
    physicalInventory.setProcessNow(false);

    OBDal.getInstance().save(physicalInventory);
    OBDal.getInstance().flush();
  }

  public static InventoryCount createPhysicalInventory(final String name, final Product product,
      final BigDecimal quantityCount, final String inventoryType, final int day) {
    InventoryCount physicalInventory = OBProvider.getInstance().get(InventoryCount.class);
    TestCostingUtils.setGeneralData(physicalInventory);
    physicalInventory.setInventoryType(inventoryType);
    physicalInventory.setMovementDate(DateUtils.addDays(new Date(), day));
    physicalInventory.setName(name);
    Warehouse warehouse = OBDal.getInstance()
        .get(Warehouse.class, TestCostingConstants.SPAIN_WAREHOUSE_ID);
    physicalInventory.setWarehouse(warehouse);
    OBDal.getInstance().save(physicalInventory);

    InventoryCountLine physicalInventoryLine = OBProvider.getInstance()
        .get(InventoryCountLine.class);
    TestCostingUtils.setGeneralData(physicalInventoryLine);
    physicalInventoryLine.setPhysInventory(physicalInventory);
    physicalInventoryLine.setProduct(product);
    physicalInventoryLine.setQuantityCount(quantityCount);
    Locator storageBin = OBDal.getInstance()
        .get(Locator.class, TestCostingConstants.LOCATOR_L01_ID);
    physicalInventoryLine.setStorageBin(storageBin);
    physicalInventoryLine.setUOM(OBDal.getInstance().get(UOM.class, TestCostingConstants.UOM_ID));
    physicalInventoryLine.setBookQuantity(getQuantityOnHandOfProductInLocator(product, storageBin));
    physicalInventoryLine.setLineNo(10L);

    physicalInventory.getMaterialMgmtInventoryCountLineList().add(physicalInventoryLine);

    OBDal.getInstance().save(physicalInventory);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(physicalInventory);
    return physicalInventory;
  }

  public static BigDecimal getQuantityOnHandOfProductInLocator(final Product product,
      final Locator storageBin) {
    OBCriteria<StorageDetail> storageDetailCriteria = OBDal.getInstance()
        .createCriteria(StorageDetail.class);
    storageDetailCriteria.addEqual(StorageDetail.PROPERTY_PRODUCT, product);
    storageDetailCriteria.addEqual(StorageDetail.PROPERTY_STORAGEBIN, storageBin);
    storageDetailCriteria.setMaxResults(1);
    StorageDetail storageDetail = (StorageDetail) storageDetailCriteria.uniqueResult();
    if (storageDetail != null) {
      return (BigDecimal) storageDetail.getQuantityOnHand();
    }
    return null;
  }

  public static ConnectionProvider getConnectionProvider() {
    try {
      final String propFile = OBConfigFileProvider.getInstance().getFileLocation();
      final ConnectionProvider conn = new ConnectionProviderImpl(
          propFile + "/Openbravo.properties");
      return conn;
    } catch (PoolNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }
}
