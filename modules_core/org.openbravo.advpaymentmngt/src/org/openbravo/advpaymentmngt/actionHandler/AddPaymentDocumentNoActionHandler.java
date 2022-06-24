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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.advpaymentmngt.actionHandler;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;

public class AddPaymentDocumentNoActionHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    try {
      final JSONObject jsonData = new JSONObject(data);
      JSONObject result = new JSONObject();
      String strDocNo = null;
      final String strOrganization = jsonData.getString("organization");
      if (StringUtils.isNotEmpty(strOrganization)) {
        Organization organization = OBDal.getInstance().get(Organization.class, strOrganization);
        strDocNo = FIN_Utility.getDocumentNo(organization,
            jsonData.getString("issotrx").toString().equals("true") ? "ARR" : "APP", "FIN_Payment",
            false);
      }
      result.put("payment_documentno", "<" + strDocNo + ">");
      return result;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }
}
