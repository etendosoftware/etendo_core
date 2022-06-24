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
 * All portions are Copyright (C) 2015-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.info;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.OrderLine;

public class ServiceProductPricePrecisionFilterExpression implements FilterExpression {
  private Logger log = LogManager.getLogger();

  @Override
  public String getExpression(Map<String, String> requestMap) {
    String strCurrentParam = "";
    try {
      JSONObject context = new JSONObject(requestMap.get("context"));
      strCurrentParam = requestMap.get("currentParam");
      final OrderLine orderLine = OBDal.getInstance()
          .get(OrderLine.class, context.getString("inpcOrderlineId"));
      return orderLine.getSalesOrder().getCurrency().getPricePrecision().toString();
    } catch (JSONException e) {
      log.error("Error trying to get default value of " + strCurrentParam + " " + e.getMessage(),
          e);
      return null;
    }
  }
}
