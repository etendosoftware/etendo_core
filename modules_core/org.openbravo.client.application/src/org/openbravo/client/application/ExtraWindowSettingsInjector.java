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
 ************************************************************************
 */
package org.openbravo.client.application;

import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;

/**
 * Classes implementing this interface are injected in the {@link WindowSettingsActionHandler}
 * class. Using this interface it is possible to customize the return object of that class or to
 * perform some custom validations. It is also possible to add some JavaScript functions to be
 * executed on the Callback function of the Action Handler.
 */
public interface ExtraWindowSettingsInjector {

  /**
   * This method is executed in the {@link WindowSettingsActionHandler#execute(Map, String) execute}
   * method.
   * 
   * Using the WindowSettingsActionHandler.EXTRA_CALLBACK key it is possible to return a
   * List&lt;String&gt; with JavaScript functions that are executed on the callback of the
   * WindowSettingsActionHandler execution. These functions only receive the "data" object as
   * argument.
   * 
   * All other keys are included in a "extraSettings" JavaScript object in the response "data"
   * object.
   * 
   * @param parameters
   *          the parameters Map of the current WindowSettingsActionHandler execution.
   * @param json
   *          the JSONObject instance of the response of the WindowSettingsActionHandler.
   * @return A Map&lt;String, Object&gt; with all the extra settings desired to be included in the
   *         WindowSettingsActionHandler response.
   * @throws OBException
   */
  public Map<String, Object> doAddSetting(Map<String, Object> parameters, JSONObject json)
      throws OBException;
}
