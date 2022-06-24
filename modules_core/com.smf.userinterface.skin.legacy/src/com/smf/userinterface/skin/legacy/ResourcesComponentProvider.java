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
 * All portions are Copyright (C) 2011-2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package com.smf.userinterface.skin.legacy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;

/**
 * 
 * @author dbaz
 */
@ApplicationScoped
@ComponentProvider.Qualifier(ResourcesComponentProvider.COMPONENT_TYPE)
public class ResourcesComponentProvider extends BaseComponentProvider {
  public static final String COMPONENT_TYPE = "com.smf.userinterface.skin.legacy_Resources";
  public static final String SKIN_NAME = "Legacy";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();

    String legacySkin = "N";
    try {
      legacySkin = org.openbravo.erpCommon.businessUtility.Preferences.getPreferenceValue(
          "SKINLEG_LegacySkin", true, OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(), null);
    } catch (Exception ignored) { }

    if(legacySkin == null || !legacySkin.equals("Y")) {
      return globalResources;
    }

    globalResources.add(createStaticResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + ResourcesComponentProvider.SKIN_NAME
            + "/org.openbravo.client.application/ob-personalization-styles.js", false));

    globalResources.add(createStaticResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + ResourcesComponentProvider.SKIN_NAME
            + "/org.openbravo.client.myob/ob-myopenbravo-styles.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + ResourcesComponentProvider.SKIN_NAME
            + "/org.openbravo.client.myob/ob-widget-styles.js", false));

    globalResources.add(createStaticResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + ResourcesComponentProvider.SKIN_NAME
            + "/org.openbravo.client.querylist/ob-querylist-styles.js", false));

    globalResources.add(createStaticResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + ResourcesComponentProvider.SKIN_NAME
            + "/org.openbravo.userinterface.selector/ob-selector-item-styles.js", false));


    globalResources.add(createStyleSheetResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + ResourcesComponentProvider.SKIN_NAME
            + "/org.openbravo.client.application/ob-personalization-styles.css", false));

    globalResources.add(createStyleSheetResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + ResourcesComponentProvider.SKIN_NAME
            + "/org.openbravo.client.myob/ob-myopenbravo-styles.css", false));
    globalResources.add(createStyleSheetResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + ResourcesComponentProvider.SKIN_NAME
            + "/org.openbravo.client.myob/ob-widget-styles.css", false));

    globalResources.add(createStyleSheetResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + ResourcesComponentProvider.SKIN_NAME
            + "/org.openbravo.client.querylist/ob-querylist-styles.css", false));

    globalResources.add(createStyleSheetResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + ResourcesComponentProvider.SKIN_NAME
            + "/org.openbravo.userinterface.selector/ob-selector-item-styles.css", false));

    // RTL files should be added at the end. Don't add more files after them
    if (OBContext.getOBContext().isRTL()) {
      globalResources.add(createStyleSheetResource(
          "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
              + ResourcesComponentProvider.SKIN_NAME
              + "/org.openbravo.client.application/ob-rtl-styles.css", false));

      globalResources.add(createStaticResource(
          "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
              + ResourcesComponentProvider.SKIN_NAME
              + "/org.openbravo.client.application/ob-rtl-styles.js", false));
    }

    return globalResources;
  }
}