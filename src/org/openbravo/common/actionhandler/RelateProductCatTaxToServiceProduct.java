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
 * All portions are Copyright (C) 2018-2019 Openbravo SLU 
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
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.common.plm.ProductServiceLinked;
import org.openbravo.model.financialmgmt.tax.TaxCategory;
import org.openbravo.service.db.DbUtility;

public class RelateProductCatTaxToServiceProduct extends BaseProcessActionHandler {
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
          .getJSONObject("relateProdCatNewTax")
          .getJSONArray("_selection");
      if (selectedLines.length() == 0) {
        errorMessage.put(SEVERITY, "error");
        errorMessage.put("title", OBMessageUtils.messageBD("NotSelected"));
        jsonRequest.put(MESSAGE, errorMessage);
        return jsonRequest;
      }

      final Product serviceProduct = OBDal.getInstance()
          .getProxy(Product.class, jsonRequest.getString("inpmProductId"));
      final Client serviceProductClient = OBDal.getInstance()
          .getProxy(Client.class, jsonRequest.getString("inpadClientId"));
      final Organization serviceProductOrg = OBDal.getInstance()
          .getProxy(Organization.class, jsonRequest.getString("inpadOrgId"));

      for (int i = 0; i < selectedLines.length(); i++) {
        final JSONObject selectedLine = selectedLines.getJSONObject(i);
        log.debug("{}", selectedLine);

        final ProductCategory productCategory = OBDal.getInstance()
            .getProxy(ProductCategory.class, selectedLine.getString(ProductCategory.PROPERTY_ID));
        final TaxCategory taxCategory = OBDal.getInstance()
            .getProxy(TaxCategory.class, selectedLine.getString("taxCategory"));

        ProductServiceLinked productServiceLinked = OBProvider.getInstance()
            .get(ProductServiceLinked.class);
        productServiceLinked.setClient(serviceProductClient);
        productServiceLinked.setOrganization(serviceProductOrg);
        productServiceLinked.setProduct(serviceProduct);
        productServiceLinked.setProductCategory(productCategory);
        productServiceLinked.setTaxCategory(taxCategory);
        OBDal.getInstance().save(productServiceLinked);
        if ((i % 100) == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
      }

      errorMessage.put(SEVERITY, "success");
      errorMessage.put("title", OBMessageUtils.messageBD("Success"));
      jsonRequest.put(MESSAGE, errorMessage);
    } catch (Exception e) {
      log.error("Error in RelateProductCatTaxToServiceProduct Action Handler", e);
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
}
