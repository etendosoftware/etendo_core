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
 * All portions are Copyright (C) 2018-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductServiceLinked;
import org.openbravo.service.db.DbUtility;

public class ServicesModifyTaxCopyConfiguration extends BaseProcessActionHandler {
  private static final String MESSAGE = "message";
  private static final String SEVERITY = "severity";
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode(true);
    JSONObject errorMessage = new JSONObject();
    try {
      jsonRequest = new JSONObject(content);
      log.debug("{}", jsonRequest);

      final JSONArray selectedLines = jsonRequest.getJSONObject("_params")
          .getJSONObject("servicesModifyTax")
          .getJSONArray("_selection");
      if (selectedLines.length() == 0) {
        errorMessage.put(SEVERITY, "error");
        errorMessage.put("title", OBMessageUtils.messageBD("NotSelected"));
        jsonRequest.put(MESSAGE, errorMessage);
        return jsonRequest;
      }

      final Product serviceProduct = OBDal.getInstance()
          .getProxy(Product.class, jsonRequest.getString("inpmProductId"));

      for (int i = 0; i < selectedLines.length(); i++) {
        final JSONObject selectedLine = selectedLines.getJSONObject(i);
        log.debug("{}", selectedLine);

        final Product originalProduct = OBDal.getInstance()
            .getProxy(Product.class, selectedLine.getString(Product.PROPERTY_ID));
        appendConfig(originalProduct, serviceProduct);
      }

      errorMessage.put(SEVERITY, "success");
      errorMessage.put("title", OBMessageUtils.messageBD("Success"));
      jsonRequest.put(MESSAGE, errorMessage);
    } catch (Exception e) {
      log.error("Error in ServicesModifyTaxCopyConfiguration Action Handler", e);
      OBDal.getInstance().rollbackAndClose();
      try {
        jsonRequest = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        errorMessage = new JSONObject();
        errorMessage.put(SEVERITY, "error");
        errorMessage.put("text", message);
        jsonRequest.put(MESSAGE, errorMessage);

      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonRequest;
  }

  private void appendConfig(Product sourceProduct, Product targetProduct) {
    // Remove existing configuration
    //@formatter:off
    final String hqlDelete = "delete "
                           + "from M_PRODUCT_SERVICELINKED "
                           + "where product.id = :productId";
    //@formatter:on
    OBDal.getInstance()
        .getSession()
        .createQuery(hqlDelete)
        .setParameter("productId", targetProduct.getId())
        .executeUpdate();

    // Add new configuration
    OBCriteria<ProductServiceLinked> obc = OBDal.getInstance()
        .createCriteria(ProductServiceLinked.class);
    obc.add(Restrictions.eq(ProductServiceLinked.PROPERTY_PRODUCT, sourceProduct));
    for (ProductServiceLinked sourceProductServiceLinked : obc.list()) {
      ProductServiceLinked targetProductServiceLinked = OBProvider.getInstance()
          .get(ProductServiceLinked.class);
      targetProductServiceLinked.setOrganization(targetProduct.getOrganization());
      targetProductServiceLinked.setProduct(targetProduct);
      targetProductServiceLinked
          .setProductCategory(sourceProductServiceLinked.getProductCategory());
      targetProductServiceLinked.setTaxCategory(sourceProductServiceLinked.getTaxCategory());
      OBDal.getInstance().save(targetProductServiceLinked);
    }
  }
}
