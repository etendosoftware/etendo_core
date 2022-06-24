/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2013-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.materialmgmt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.GenericJDBCException;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Image;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductAccounts;
import org.openbravo.model.common.plm.ProductCharacteristic;
import org.openbravo.model.common.plm.ProductCharacteristicConf;
import org.openbravo.model.common.plm.ProductCharacteristicValue;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

public class VariantAutomaticGenerationProcess implements Process {
  private static final Logger log4j = LogManager.getLogger();
  private static final int searchKeyLength = getSearchKeyColumnLength();
  private static final String SALES_PRICELIST = "SALES";
  private static final String PURCHASE_PRICELIST = "PURCHASE";

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(OBMessageUtils.messageBD("Success"));

    try {
      // retrieve standard params
      final String recordID = (String) bundle.getParams().get("M_Product_ID");
      Product product = OBDal.getInstance().get(Product.class, recordID);

      runChecks(product);

      int totalMaxLength = product.getSearchKey().length();
      long variantNumber = 1;
      Map<String, ProductCharacteristicAux> prChUseCode = new HashMap<String, ProductCharacteristicAux>();

      OBCriteria<ProductCharacteristic> prChCrit = OBDal.getInstance()
          .createCriteria(ProductCharacteristic.class);
      prChCrit.add(Restrictions.eq(ProductCharacteristic.PROPERTY_PRODUCT, product));
      prChCrit.add(Restrictions.eq(ProductCharacteristic.PROPERTY_VARIANT, true));
      prChCrit.addOrderBy(ProductCharacteristic.PROPERTY_SEQUENCENUMBER, true);
      List<ProductCharacteristic> prChCritList = prChCrit.list();
      List<String> prChs = new ArrayList<String>();
      for (ProductCharacteristic pc : prChCritList) {
        prChs.add(pc.getId());
      }
      int chNumber = prChs.size();
      String[] currentValues = new String[chNumber];

      int i = 0;
      for (ProductCharacteristic prCh : prChCritList) {
        List<String> prChConfs = new ArrayList<String>();
        for (ProductCharacteristicConf pcc : prCh.getProductCharacteristicConfList()) {
          prChConfs.add(pcc.getId());
        }
        long valuesCount = prChConfs.size();

        boolean useCode = true;
        int maxLength = 0;
        for (String id : prChConfs) {
          ProductCharacteristicConf prChConf = OBDal.getInstance()
              .get(ProductCharacteristicConf.class, id);
          if (StringUtils.isBlank(prChConf.getCode())) {
            useCode = false;
            break;
          }
          if (prChConf.getCode().length() > maxLength) {
            maxLength = prChConf.getCode().length();
          }
        }

        variantNumber = variantNumber * valuesCount;
        if (useCode) {
          totalMaxLength += maxLength;
        }

        if (!prChConfs.isEmpty()) {
          ProductCharacteristicAux prChAux = new ProductCharacteristicAux(useCode, prChConfs);
          currentValues[i] = prChAux.getNextValue();
          prChUseCode.put(prCh.getId(), prChAux);
          i++;
        }
      }
      totalMaxLength += Long.toString(variantNumber).length();
      boolean useCodes = totalMaxLength <= searchKeyLength;

      boolean hasNext = variantNumber > 0;
      int productNo = 0;
      int k = 0;
      Long start = System.currentTimeMillis();
      boolean multilingualDocs = OBDal.getInstance()
          .get(Client.class, bundle.getContext().getClient())
          .isMultilingualDocuments();
      while (hasNext) {
        k = k + 1;
        // Create variant product
        product = OBDal.getInstance().get(Product.class, recordID);
        Product variant = (Product) DalUtil.copy(product);

        if (multilingualDocs) {
          variant.set(Product.PROPERTY_PRODUCTTRLLIST, null);
        }

        if (product.getImage() != null) {
          Image newPrImage = (Image) DalUtil.copy(product.getImage(), false);
          OBDal.getInstance().save(newPrImage);
          variant.setImage(newPrImage);
        }

        variant.setGenericProduct(product);
        variant.setProductAccountsList(Collections.<ProductAccounts> emptyList());
        variant.setGeneric(false);
        for (ProductCharacteristic prCh : variant.getProductCharacteristicList()) {
          prCh.setProductCharacteristicConfList(
              Collections.<ProductCharacteristicConf> emptyList());
        }

        String searchKey = product.getSearchKey();
        for (i = 0; i < chNumber; i++) {
          ProductCharacteristicConf prChConf = OBDal.getInstance()
              .get(ProductCharacteristicConf.class, currentValues[i]);
          ProductCharacteristicAux prChConfAux = prChUseCode.get(prChs.get(i));

          if (useCodes && prChConfAux.isUseCode()) {
            searchKey += prChConf.getCode();
          }
        }
        for (int j = 0; j < (Long.toString(variantNumber).length()
            - Integer.toString(productNo).length()); j++) {
          searchKey += "0";
        }
        searchKey += productNo;
        variant.setSearchKey(searchKey);
        OBDal.getInstance().save(variant);
        String strChDesc = "";
        for (i = 0; i < chNumber; i++) {
          ProductCharacteristicConf prChConf = OBDal.getInstance()
              .get(ProductCharacteristicConf.class, currentValues[i]);
          ProductCharacteristicValue newPrChValue = OBProvider.getInstance()
              .get(ProductCharacteristicValue.class);
          newPrChValue.setCharacteristic(prChConf.getCharacteristicOfProduct().getCharacteristic());
          newPrChValue.setCharacteristicValue(prChConf.getCharacteristicValue());
          newPrChValue.setProduct(variant);
          newPrChValue.setOrganization(product.getOrganization());
          if (StringUtils.isNotBlank(strChDesc)) {
            strChDesc += ", ";
          }
          strChDesc += prChConf.getCharacteristicOfProduct().getCharacteristic().getName() + ":";
          strChDesc += " " + prChConf.getCharacteristicValue().getName();
          OBDal.getInstance().save(newPrChValue);
          if (prChConf.getCharacteristicOfProduct().isDefinesPrice()
              && prChConf.getNetUnitPrice() != null) {
            setPrice(variant, prChConf.getNetUnitPrice(),
                prChConf.getCharacteristicOfProduct().getPriceListType());
          }
          if (prChConf.getCharacteristicOfProduct().isDefinesImage()
              && prChConf.getImage() != null) {
            Image newImage = (Image) DalUtil.copy(prChConf.getImage(), false);
            OBDal.getInstance().save(newImage);
            variant.setImage(newImage);
          }
        }
        variant.setCharacteristicDescription(strChDesc);
        OBDal.getInstance().save(variant);

        for (i = 0; i < chNumber; i++) {
          ProductCharacteristicAux prChConfAux = prChUseCode.get(prChs.get(i));
          currentValues[i] = prChConfAux.getNextValue();
          if (!prChConfAux.isIteratorReset()) {
            break;
          } else if (i + 1 == chNumber) {
            hasNext = false;
          }
        }
        productNo++;

        // Creates variants from 1 to 1000 and shows time spent on it.
        if (k == 1000) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
          log4j.debug(
              "Variants loop: " + productNo + " : " + ((System.currentTimeMillis()) - (start)));
          k = 0;
          start = System.currentTimeMillis();
        }
      }

      OBDal.getInstance().flush();
      OBDal.getInstance().getSession().clear();

      String message = OBMessageUtils.messageBD("variantsCreated");
      Map<String, String> map = new HashMap<String, String>();
      map.put("variantNo", Long.toString(productNo));
      msg.setMessage(OBMessageUtils.parseTranslation(message, map));
      bundle.setResult(msg);

      // Postgres wraps the exception into a GenericJDBCException
    } catch (GenericJDBCException ge) {
      log4j.error("Exception processing variant generation", ge);
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD(bundle.getConnection(), "Error",
          bundle.getContext().getLanguage()));
      msg.setMessage(ge.getSQLException().getMessage().split("\n")[0]);
      bundle.setResult(msg);
      OBDal.getInstance().rollbackAndClose();
    } catch (final Exception e) {
      log4j.error("Exception processing variant generation", e);
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD(bundle.getConnection(), "Error",
          bundle.getContext().getLanguage()));
      msg.setMessage(FIN_Utility.getExceptionMessage(e));
      bundle.setResult(msg);
      OBDal.getInstance().rollbackAndClose();
    }

  }

  private static int getSearchKeyColumnLength() {
    final Entity prodEntity = ModelProvider.getInstance().getEntity(Product.ENTITY_NAME);

    final Property searchKeyProperty = prodEntity.getProperty(Product.PROPERTY_SEARCHKEY);
    return searchKeyProperty.getFieldLength();
  }

  private void runChecks(Product product) throws OBException {
    // Check existence of variants
    if (!product.getProductGenericProductList().isEmpty()) {
      throw new OBException(OBMessageUtils.parseTranslation("@ProductWithVariantsError@"));
    }
    // Check it is a generic product
    if (!product.isGeneric()) {
      throw new OBException(OBMessageUtils.parseTranslation("@ProductIsNotGenericError@"));
    }
    // Check existence of variant characteristic assigned to the product
    boolean errorFlag = true;
    for (ProductCharacteristic prCh : product.getProductCharacteristicList()) {
      if (prCh.isVariant()) {
        errorFlag = false;
        break;
      }
    }
    if (errorFlag) {
      throw new OBException(OBMessageUtils.parseTranslation("@GenericWithNoVariantChError@"));
    }
  }

  private void setPrice(Product variant, BigDecimal price, String strPriceListType) {
    List<ProductPrice> prodPrices = OBDao.getActiveOBObjectList(variant,
        Product.PROPERTY_PRICINGPRODUCTPRICELIST);
    for (ProductPrice prodPrice : prodPrices) {
      boolean isSOPriceList = prodPrice.getPriceListVersion().getPriceList().isSalesPriceList();
      if (SALES_PRICELIST.equals(strPriceListType) && !isSOPriceList) {
        continue;
      } else if (PURCHASE_PRICELIST.equals(strPriceListType) && isSOPriceList) {
        continue;
      }
      prodPrice.setStandardPrice(price);
      prodPrice.setListPrice(price);
      prodPrice.setPriceLimit(price);
      OBDal.getInstance().save(prodPrice);
    }
  }

  private static class ProductCharacteristicAux {
    private boolean useCode;
    private boolean isIteratorReset;
    private List<String> values;
    private Iterator<String> iterator;

    ProductCharacteristicAux(boolean _useCode, List<String> _values) {
      useCode = _useCode;
      values = _values;
    }

    public boolean isUseCode() {
      return useCode;
    }

    public boolean isIteratorReset() {
      return isIteratorReset;
    }

    public String getNextValue() {
      String prChConf;
      if (iterator == null || !iterator.hasNext()) {
        iterator = values.iterator();
        isIteratorReset = true;
      } else {
        isIteratorReset = false;
      }
      prChConf = iterator.next();
      return prChConf;
    }
  }
}
