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
 * All portions are Copyright (C) 2019 Openbravo SLU 
 * All Rights Reserved. 
 ************************************************************************
 */

package org.openbravo.materialmgmt;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.ComponentProvider;

/**
 * Implementation of ManageVariantsCustomProductCharacteristicWhereClause for the Manage Variants
 * process
 * 
 * Defines a where clause that returns the variant characteristics of the selected product. If the
 * product characteristic references a characterictisc value subset, only the values that are part
 * of the subset are shown
 */
@ApplicationScoped
@ComponentProvider.Qualifier(ManageVariantsCustomProductCharacteristicWhereClause.MANAGE_VARIANTS_PROCESS_ID)
public class ManageVariantsCustomProductCharacteristicWhereClause
    implements ProductCharacteristicCustomWhereClause {

  static final String MANAGE_VARIANTS_PROCESS_ID = "FE3A8C134D41488DB3A69837BD54B56A";

  private static final Logger log = LogManager.getLogger();

  @Override
  public String getCustomWhereClause(Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    try {
      JSONObject params = new JSONObject(requestParameters.get("_buttonOwnerContextInfo"));
      String productId = params.getString("inpmProductId");
      queryNamedParameters.put("productId", productId);
      StringBuilder hqlWhereClause = new StringBuilder();
      hqlWhereClause.append(" exists (from ProductCharacteristic pc ");
      hqlWhereClause.append(
          " where pc.characteristic = c and pc.product.id = :productId and pc.variant = true ");
      hqlWhereClause.append(
          " and (pc.characteristicSubset is null or exists (from CharacteristicSubsetValue csv where csv.characteristicSubset.id = pc.characteristicSubset.id and csv.characteristicValue.id = v.id)))");
      return hqlWhereClause.toString();
    } catch (JSONException e) {
      log.error("There was a problem when creating a JSONObject from the request parameters", e);
      throw new OBException(e);
    }
  }

}
