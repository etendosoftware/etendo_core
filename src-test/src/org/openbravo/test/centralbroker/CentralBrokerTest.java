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
 * All portions are Copyright (C) 2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):
 *   Rafael Queralta Pozo <rafaelcuba81@gmail.com>,
 ************************************************************************
 */

package org.openbravo.test.centralbroker;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.junit.Test;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductAUM;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test Central Broker methods
 * 
 * @author rqueralta
 */

public class CentralBrokerTest extends OBBaseTest {

  private static final String PRODUCT_ID = "4028E6C72959682B01295ADC1E6E0230";
  private static final String DOCUMENT_ID = "466AF4B0136A4A3F9F84129711DA8BD3";
  private static final String UOM1 = "72BA247D31F745F3AF11F74A5E2CCBEF";
  private static final String UOM2 = "6FA87C4EE1FD4C86940A5F2E47C429DA";
  private static final String UOM3 = "2EBC93C05D75431E9EEFB29CEC76F244";

  @Test
  public void test01() {
    // Add three AUM to product 4028E6C72959682B01295ADC1E6E0230
    addAUM(UOM1, BigDecimal.valueOf(2), "1234567890", "P", "S", "S");
    addAUM(UOM2, BigDecimal.valueOf(3), "0012568437", "S", "P", "S");
    addAUM(UOM3, BigDecimal.valueOf(4), "1157423658", "S", "S", "P");
  }

  @Test
  public void test02() {
    // test method getDefaultAUMForDocument()
    String aumId = UOMUtil.getDefaultAUMForDocument(PRODUCT_ID, DOCUMENT_ID);
    assertEquals(aumId, "72BA247D31F745F3AF11F74A5E2CCBEF");
  }

  @Test
  public void test03() {
    // test method getConvertedQty()
    BigDecimal convertion = UOMUtil.getConvertedQty(PRODUCT_ID, BigDecimal.valueOf(10), UOM1);
    assertEquals(convertion.toString(), "20.00");
  }

  @Test
  public void test04() {
    // test getConvertedQtyReverse()
    BigDecimal convertion = UOMUtil.getConvertedAumQty(PRODUCT_ID, BigDecimal.valueOf(100), UOM1);
    assertEquals(convertion.toString(), "50.00");
  }

  @Test
  public void test05() {
    // test method getAvailableUOMsForDocument()
    List<UOM> aumList = UOMUtil.getAvailableUOMsForDocument(PRODUCT_ID, DOCUMENT_ID);
    assertEquals(aumList.size(), 4);
  }

  @Test
  public void test06() {
    // Remove all AUM before added to product 4028E6C72959682B01295ADC1E6E0230
    OBCriteria<ProductAUM> pAUMCriteria = OBDal.getInstance().createCriteria(ProductAUM.class);
    pAUMCriteria.add(Restrictions.eq("product.id", PRODUCT_ID));
    for (ProductAUM pAUM : pAUMCriteria.list()) {
      OBDal.getInstance().remove(pAUM);
    }
  }

  private void addAUM(String C_UOM_ID, BigDecimal CONVERSION_RATE, String GTIN,
      String SALES_PRIORITY, String PURCHASE_PRIORITY, String LOGISTICS_PRIORITY) {
    UOM uOM = OBDal.getInstance().get(UOM.class, C_UOM_ID);
    Product product = OBDal.getInstance().get(Product.class, PRODUCT_ID);
    ProductAUM productAUM = new ProductAUM();
    productAUM.setUOM(uOM);
    productAUM.setConversionRate(CONVERSION_RATE);
    productAUM.setProduct(product);
    productAUM.setGtin(GTIN);
    productAUM.setSales(SALES_PRIORITY);
    productAUM.setPurchase(PURCHASE_PRIORITY);
    productAUM.setLogistics(LOGISTICS_PRIORITY);
    productAUM.setClient(OBContext.getOBContext().getCurrentClient());
    productAUM.setOrganization(OBContext.getOBContext().getCurrentOrganization());
    productAUM.setCreatedBy(OBContext.getOBContext().getUser());
    productAUM.setUpdatedBy(OBContext.getOBContext().getUser());
    productAUM.setUpdated(new Date());
    productAUM.setCreationDate(new Date());
    OBDal.getInstance().save(productAUM);
  }

}
