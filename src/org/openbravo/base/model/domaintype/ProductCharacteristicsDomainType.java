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

package org.openbravo.base.model.domaintype;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * The type used in the Product Characteristic reference.
 */
public class ProductCharacteristicsDomainType extends StringDomainType {
  final private static Logger log = LogManager.getLogger();

  /**
   * Columns that use the ProductCharacteristics reference store in the database a String
   * representation of the product characteristics. Under certain circumstances the value sent from
   * the client to the datasource is not that String, but a JSON object that contains it in its
   * dbValue property. In that case, return the dbValue property
   * 
   * @param value
   *          the value sent from the client to the datasource
   * @return the String representation of the product characteristics
   */
  public static Object fixValue(Object value) {
    Object fixedValue = value;
    // if the value is a JSONObject that contains the dbValue property, return that property
    if (value instanceof JSONObject) {
      JSONObject jsonObjectValue = (JSONObject) value;
      if (jsonObjectValue.has("dbValue")) {
        try {
          fixedValue = jsonObjectValue.get("dbValue");
        } catch (JSONException e) {
          log.error("Exception while getting a value from a json object", e);
        }
      }
    }
    return fixedValue;
  }
}
