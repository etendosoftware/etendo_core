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
 * All portions are Copyright (C) 2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.common.actionhandler;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBLedgerUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;

public class AgingGeneralLedgerByOrganizationActionHandler extends BaseActionHandler {

  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject resultMessage = new JSONObject();
    JSONObject errorMessage = new JSONObject();
    try {

      JSONObject jsonContent = new JSONObject(content);
      JSONObject data = new JSONObject(content);
      String orgId = jsonContent.getString("organization");

      String generalLedgerId = OBLedgerUtils.getOrgLedger(orgId);
      AcctSchema generalLedger = OBDal.getInstance().get(AcctSchema.class, generalLedgerId);

      data.put("value", generalLedgerId);
      data.put("identifier", generalLedger.getName());
      resultMessage.put("response", data);

    } catch (JSONException e) {
      log.error("Error creating JSON Object ", e);
      e.printStackTrace();
      try {
        errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("title", "Error");
        errorMessage.put("text", e.getMessage());
        resultMessage.put("message", errorMessage);
      } catch (JSONException e1) {
        log.error("Error creating JSON Object ", e);
      }
    }

    return resultMessage;
  }

}
