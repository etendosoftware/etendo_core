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

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollableResults;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.financial.FinancialUtils;

public class CheckAvailableCreditActionHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    try {
      final JSONObject jsonData = new JSONObject(data);
      final String businessPartnerId = jsonData.getString("businessPartnerId");
      final String currencyId = jsonData.getString("currencyId");
      JSONObject result = new JSONObject();

      if (currencyId != null && !StringUtils.isEmpty(currencyId)) {
        ScrollableResults scroll = FinancialUtils.getPaymentsWithCredit(businessPartnerId,
            currencyId);
        try {
          result.put("availableCredit", scroll.next());
        } catch (Exception e) {
          result.put("availableCredit", false);
        } finally {
          scroll.close();
        }
      } else {
        result.put("availableCredit", false);
      }
      return result;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }
}
