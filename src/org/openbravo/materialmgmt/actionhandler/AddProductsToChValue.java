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
 * All portions are Copyright (C) 2013-2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.materialmgmt.actionhandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.CharacteristicsUtils;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.db.DbUtility;

public class AddProductsToChValue extends BaseProcessActionHandler {
  final static private Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    OBContext.setAdminMode(true);
    try {
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject params = jsonRequest.getJSONObject("_params");
      JSONObject view = params.getJSONObject("m_prodchview_v");
      JSONArray selected = view.getJSONArray("_selection");
      log.debug("{}", jsonRequest);
      final String strChValueId = jsonRequest.getString("inpmChValueId");
      CharacteristicValue chValue = OBDal.getInstance()
          .get(CharacteristicValue.class, strChValueId);

      int total = processProducts(chValue, selected);

      Map<String, String> map = new HashMap<String, String>();
      map.put("productNumer", Integer.toString(total));
      map.put("chValueName", chValue.getName());

      String messageText = OBMessageUtils.messageBD("AddProductsResult");
      JSONObject msg = new JSONObject();
      msg.put("severity", "success");
      msg.put("text", OBMessageUtils.parseTranslation(messageText, map));
      jsonResponse.put("message", msg);

    } catch (Exception e) {
      log.error("Error in Add Products to Ch Value Action Handler", e);

      try {
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonResponse.put("message", errorMessage);
      } catch (JSONException e2) {
        // do nothing, give up
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    return jsonResponse;
  }

  private int processProducts(CharacteristicValue chValue, JSONArray selected) {
    int count = 0;
    List<String> selectedProductIds = new ArrayList<String>();
    try {
      for (int i = 0; i < selected.length(); i++) {
        JSONObject productJSON = selected.getJSONObject(i);
        String strProductId;
        strProductId = productJSON.getString("product");
        final Product product = OBDal.getInstance().get(Product.class, strProductId);
        CharacteristicsUtils.setCharacteristicValue(product, chValue);
        selectedProductIds.add(strProductId);
        count++;
        for (Product variant : product.getProductGenericProductList()) {
          if (selectedProductIds.contains(variant.getId())) {
            continue;
          }
          CharacteristicsUtils.setCharacteristicValue(variant, chValue);
          count++;
        }
      }
    } catch (JSONException ignore) {
    }
    return count;

  }
}
