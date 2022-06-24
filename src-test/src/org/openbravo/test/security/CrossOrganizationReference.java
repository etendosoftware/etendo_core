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
 * All portions are Copyright (C) 2016-2021 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.security;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.test.datasource.BaseDataSourceTestDal;

/**
 * Base class for test for cross organization reference functionality.
 * 
 * @author alostale
 *
 */
public class CrossOrganizationReference extends BaseDataSourceTestDal {
  protected static final String SPAIN_ORG = "357947E87C284935AD1D783CF6F099A1";
  protected static final String SPAIN_WAREHOUSE = "4D7B97565A024DB7B4C61650FA2B9560";

  protected static final String USA_ORG = "5EFF95EB540740A3B10510D9814EFAD5";
  protected static final String USA_WAREHOUSE = "4028E6C72959682B01295ECFE2E20270";
  protected static final String USA_ORDER = "6394CC7B913240CCB6A54FB9C22477AF";
  protected static final String USA_BP = "4028E6C72959682B01295F40D4D20333";

  private static final String CREDIT_ORDER_DOC_TYPE = "FF8080812C2ABFC6012C2B3BDF4C0056";
  private static final String CUST_A = "4028E6C72959682B01295F40C3CB02EC";
  private static final String CUST_A_LOCATION = "4028E6C72959682B01295F40C43802EE";
  private static final String PAYMENT_TERM = "7B308C5CB9674BB3A56E63D85887058A";
  private static final String PRICE_LIST = "4028E6C72959682B01295B03CE480243";
  private static final String OUM = "4028E6C72959682B01295ADC1A380221";
  private static final String PRODUCT = "4028E6C72959682B01295ADC1D07022A";
  private static final String TAX = "3271411A5AFB490A91FB618B6B789C24";

  protected static List<BaseOBObject> createdObjects = new ArrayList<BaseOBObject>();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  /** Creates a default order */
  protected Order createOrder(String orgId) {
    return createOrder(orgId, new HashMap<String, Object>());
  }

  /** Creates a default order in the given warehouse */
  @SuppressWarnings("serial")
  protected Order createOrder(String orgId, final String warehouseId) {
    return createOrder(orgId, new HashMap<String, Object>() {
      {
        put(Order.PROPERTY_WAREHOUSE, OBDal.getInstance().getProxy(Warehouse.class, warehouseId));
      }
    });
  }

  /** Creates a default order being possible to overwrite any value */
  protected Order createOrder(String orgId, Map<String, Object> propertyValues) {
    Order order = OBProvider.getInstance().get(Order.class);
    order.setOrganization(OBDal.getInstance().getProxy(Organization.class, orgId));
    order.setDocumentType(OBDal.getInstance().getProxy(DocumentType.class, CREDIT_ORDER_DOC_TYPE));
    order.setTransactionDocument(
        OBDal.getInstance().getProxy(DocumentType.class, CREDIT_ORDER_DOC_TYPE));
    order.setDocumentNo("TestCrossOrg");

    order.setBusinessPartner(OBDal.getInstance().getProxy(BusinessPartner.class, CUST_A));
    order.setPartnerAddress(OBDal.getInstance().getProxy(Location.class, CUST_A_LOCATION));
    order.setCurrency(OBDal.getInstance().getProxy(Currency.class, EURO_ID));
    order.setPaymentTerms(OBDal.getInstance().getProxy(PaymentTerm.class, PAYMENT_TERM));
    order.setWarehouse(OBDal.getInstance().getProxy(Warehouse.class, SPAIN_WAREHOUSE));
    order.setPriceList(OBDal.getInstance().getProxy(PriceList.class, PRICE_LIST));
    order.setOrderDate(new Date());
    order.setAccountingDate(new Date());

    setProperties(propertyValues, order);

    OBDal.getInstance().save(order);
    OBDal.getInstance().flush();
    return order;
  }

  /** Creates a default order line for given order */
  protected OrderLine createOrderLine(Order order) {
    return createOrderLine(order, new HashMap<String, Object>());
  }

  /** Creates a default order line for given order allowing to overwrite any value */
  protected OrderLine createOrderLine(Order order, Map<String, Object> propertyValues) {
    OrderLine ol = OBProvider.getInstance().get(OrderLine.class);
    Organization org;

    if (propertyValues.containsKey(OrderLine.PROPERTY_ORGANIZATION)) {
      org = (Organization) propertyValues.get(OrderLine.PROPERTY_ORGANIZATION);
    } else {
      org = order.getOrganization();
    }

    ol.setSalesOrder(order);
    ol.setOrganization(org);
    ol.setLineNo(100L);
    ol.setOrderDate(new Date());
    ol.setWarehouse(org.getOrganizationWarehouseList().get(0).getWarehouse());
    ol.setProduct(OBDal.getInstance().getProxy(Product.class, PRODUCT));
    ol.setUOM(OBDal.getInstance().getProxy(UOM.class, OUM));
    ol.setOrderedQuantity(BigDecimal.TEN);
    ol.setCurrency(OBDal.getInstance().getProxy(Currency.class, EURO_ID));
    ol.setTax(OBDal.getInstance().getProxy(TaxRate.class, TAX));

    setProperties(propertyValues, ol);

    OBDal.getInstance().save(ol);
    OBDal.getInstance().flush();
    return ol;
  }

  private void setProperties(Map<String, Object> propertyValues, BaseOBObject obj) {
    for (Entry<String, Object> propertyValue : propertyValues.entrySet()) {
      obj.set(propertyValue.getKey(), propertyValue.getValue());
    }
  }

  @Before
  public void setRole() {
    setQAAdminContext();
  }

  @AfterClass
  public static void removeCreatedObjects() {
    OBContext.setOBContext("0");
    OBContext.setAdminMode(false);
    try {
      Collections.reverse(createdObjects);
      for (BaseOBObject obj : createdObjects) {
        OBDal.getInstance().remove(obj);
        OBDal.getInstance().flush();
      }
      OBDal.getInstance().commitAndClose();
      createdObjects.clear();
    } catch (Exception ignore) {
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Changes allowed cross org reference setting
   * 
   * @param colIds
   *          list of columns ids to change
   * @param allowCrossOrgColumns
   *          value to set
   */
  static void setUpAllowedCrossOrg(List<String> colIds, boolean allowCrossOrgColumns)
      throws Exception {
    OBContext.setOBContext("0");
    for (String colId : colIds) {
      Column col = OBDal.getInstance().get(Column.class, colId);
      Property p = ModelProvider.getInstance()
          .getEntityByTableId(col.getTable().getId())
          .getPropertyByColumnName(col.getDBColumnName());
      p.setAllowedCrossOrgReference(allowCrossOrgColumns);
    }
  }
}
