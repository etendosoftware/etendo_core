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
 * All portions are Copyright (C) 2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.kernel.reference;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Color;
import org.openbravo.model.ad.ui.Field;

public class FKColorUIDefinition extends ForeignKeyUIDefinition {
  @Override
  public String getFormEditorType() {
    return "OBColorItem";
  }

  @Override
  public String getFieldProperties(Field field, boolean getValueFromSession) {
    try {
      final JSONObject json = new JSONObject(super.getFieldProperties(field, getValueFromSession));
      if (json.has("value")) {
        Color color = OBDal.getInstance().get(Color.class, json.get("value"));
        if (color != null) {
          json.put("identifier", color.getIdentifier());
        }
      }
      return json.toString();
    } catch (JSONException e) {
      throw new OBException("Error while computing Color Selector data", e);
    }
  }
}
