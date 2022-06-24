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
 * All portions are Copyright (C) 2015 Openbravo SLU 
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
import org.openbravo.model.ad.access.ServiceProduct;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.db.DbUtility;

public class RelateProductsToServiceProduct extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode(true);
    JSONObject errorMessage = new JSONObject();
    try {
      jsonRequest = new JSONObject(content);
      log.debug("{}", jsonRequest);

      JSONArray selectedLines = jsonRequest.getJSONObject("_params")
          .getJSONObject("servicesRelatedProducts")
          .getJSONArray("_selection");
      if (selectedLines.length() == 0) {
        errorMessage.put("severity", "error");
        errorMessage.put("title", OBMessageUtils.messageBD("NotSelected"));
        jsonRequest.put("message", errorMessage);
        return jsonRequest;
      }

      final Product serviceProduct = (Product) OBDal.getInstance()
          .getProxy(Product.ENTITY_NAME, jsonRequest.getString("inpmProductId"));
      final Client serviceProductClient = (Client) OBDal.getInstance()
          .getProxy(Client.ENTITY_NAME, jsonRequest.getString("inpadClientId"));
      final Organization serviceProductOrg = (Organization) OBDal.getInstance()
          .getProxy(Organization.ENTITY_NAME, jsonRequest.getString("inpadOrgId"));

      for (int i = 0; i < selectedLines.length(); i++) {
        JSONObject selectedLine = selectedLines.getJSONObject(i);
        log.debug("{}", selectedLine);

        final Product product = (Product) OBDal.getInstance()
            .getProxy(Product.ENTITY_NAME, selectedLine.getString(Product.PROPERTY_ID));

        ServiceProduct sp = OBProvider.getInstance().get(ServiceProduct.class);
        sp.setClient(serviceProductClient);
        sp.setOrganization(serviceProductOrg);
        sp.setProduct(serviceProduct);
        sp.setRelatedProduct(product);
        OBDal.getInstance().save(sp);
        if ((i % 100) == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
      }

      errorMessage.put("severity", "success");
      errorMessage.put("title", OBMessageUtils.messageBD("Success"));
      jsonRequest.put("message", errorMessage);
    } catch (Exception e) {
      log.error("Error in RelateProductsToServiceProduct Action Handler", e);
      OBDal.getInstance().rollbackAndClose();
      try {
        jsonRequest = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonRequest.put("message", errorMessage);

      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonRequest;
  }
}
