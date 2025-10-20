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
 * All portions are Copyright (C) 2010-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.myob;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.dal.core.OBContext;

/**
 * 
 * @author iperdomo
 */
@ApplicationScoped
@ComponentProvider.Qualifier(MyOpenbravoComponentProvider.COMPONENT_TYPE)
public class MyOpenbravoComponentProvider extends BaseComponentProvider {
  public static final String COMPONENT_TYPE = "OBMYOB_MyOpenbravo";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    if (componentId.equals(MyOpenbravoWidgetComponent.COMPONENT_ID)) {
      final MyOpenbravoWidgetComponent component = getComponent(MyOpenbravoWidgetComponent.class);
      component.setParameters(parameters);
      return component;
    }
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    globalResources
        .add(createStaticResource("web/org.openbravo.client.myob/js/ob-myopenbravo.js", false));
    globalResources
        .add(createStaticResource("web/org.openbravo.client.myob/js/ob-widget.js", false));
    globalResources
        .add(createStaticResource("web/org.openbravo.client.myob/js/ob-url-widget.js", false));
    globalResources.add(
        createStaticResource("web/org.openbravo.client.myob/js/ob-showparameter-widget.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.myob/js/ob-community-branding-widget.js", false));
    globalResources
        .add(createStaticResource("web/org.openbravo.client.myob/js/ob-calendar-widget.js", false));
    globalResources
        .add(createStaticResource("web/org.openbravo.client.myob/js/ob-about-popup.js", false));

    globalResources.add(createStaticResource(
        "web/org.openbravo.client.myob/js/form/formitem/ob-formitem-widgetinform.js", false));

    globalResources.add(createStaticResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_PARAMETER + "/org.openbravo.client.myob/ob-widget-styles.js",
        false));
    globalResources
        .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_PARAMETER
            + "/org.openbravo.client.myob/ob-myopenbravo-styles.js", false));

    globalResources.add(createStyleSheetResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_PARAMETER + "/org.openbravo.client.myob/ob-widget-styles.css",
        false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_PARAMETER
            + "/org.openbravo.client.myob/ob-myopenbravo-styles.css", false));

    // RTL files should be added at the end. Don't add more files after them
    if (OBContext.getOBContext().isRTL()) {
      globalResources.add(createStyleSheetResource(
          "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
              + KernelConstants.SKIN_PARAMETER + "/org.openbravo.client.myob/ob-rtl-styles.css",
          false));

      globalResources.add(createStaticResource(
          "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
              + KernelConstants.SKIN_PARAMETER + "/org.openbravo.client.myob/ob-rtl-styles.js",
          false));
    }

    return globalResources;
  }
}
