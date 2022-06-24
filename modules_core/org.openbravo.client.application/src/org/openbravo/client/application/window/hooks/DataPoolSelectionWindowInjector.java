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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.window.hooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.ExtraWindowSettingsInjector;

/**
 * This class checks when Data Pool Selection screen loads that read-only pool is configured and
 * shows a message when it's not.
 */
public class DataPoolSelectionWindowInjector implements ExtraWindowSettingsInjector {

  private static final String DATA_POOL_SEL_WINDOW_ID = "48B7215F9BF6458E813E6B280DEDB958";
  private static final String DATA_POOL_SEL_TAB_ID = "D829B2F06F444694B7080C9BA19428E6";

  private static final String RO_POOL_URL_PROPERTIES_KEY = "bbdd.readonly.url";
  private static final String WINDOW_ID_PARAMETER = "windowId";

  private static final String TAB_ID_KEY = "tabId";
  private static final String WINDOW_MESSAGE_KEY = "messageKey";
  private static final String EXTRA_CALLBACKS_KEY = "extraCallbacks";

  private static final String RO_POOL_NOT_AVAILABLE_MESSAGE_KEY = "OBUIAPP_ROPoolNotAvailable";
  private static final String SHOW_MESSAGE_CALLBACK_FUNCTION = "OB.Utilities.ExtraWindowSettingsActions.showInfoMessage";

  @Override
  public Map<String, Object> doAddSetting(Map<String, Object> parameters, JSONObject json)
      throws OBException {

    String windowId = (String) parameters.get(WINDOW_ID_PARAMETER);
    if (DATA_POOL_SEL_WINDOW_ID.equals(windowId) && readOnlyPoolIsNotAvailable()) {
      return getShowInfoMessageSettings();
    }

    return Collections.emptyMap();
  }

  private Map<String, Object> getShowInfoMessageSettings() {
    Map<String, Object> settings = new HashMap<>(3);
    settings.put(WINDOW_MESSAGE_KEY, RO_POOL_NOT_AVAILABLE_MESSAGE_KEY);
    settings.put(TAB_ID_KEY, DATA_POOL_SEL_TAB_ID);

    List<String> callbackList = new ArrayList<>(1);
    callbackList.add(SHOW_MESSAGE_CALLBACK_FUNCTION);
    settings.put(EXTRA_CALLBACKS_KEY, callbackList);

    return settings;
  }

  private boolean readOnlyPoolIsNotAvailable() {
    return !OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .containsKey(RO_POOL_URL_PROPERTIES_KEY);
  }
}
