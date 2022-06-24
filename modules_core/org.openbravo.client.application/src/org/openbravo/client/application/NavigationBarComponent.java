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
package org.openbravo.client.application;

import java.util.Collection;

import org.openbravo.client.application.NavigationBarComponentGenerator.NBComponent;
import org.openbravo.client.kernel.SessionDynamicTemplateComponent;

/**
 * This class generates the set of navigation bar components which are accessible for the user of
 * the current context.
 * 
 */
public class NavigationBarComponent extends SessionDynamicTemplateComponent {

  @Override
  public String getId() {
    return ApplicationConstants.NAVIGATION_BAR_ID;
  }

  @Override
  protected String getTemplateId() {
    return ApplicationConstants.NAVIGATION_BAR_TEMPLATE_ID;
  }

  public Collection<NBComponent> getNavigationBarComponents() {
    return NavigationBarComponentGenerator.getInstance()
        .getNavigationBarComponents(getParameters());
  }
}
