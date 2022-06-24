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
package org.openbravo.client.application;

import java.util.Collection;

import org.openbravo.client.application.NavigationBarComponentGenerator.NBComponent;
import org.openbravo.client.kernel.BaseTemplateComponent;
import org.openbravo.client.kernel.Template;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

/**
 * This class generates the navigation bar components which are defined as dynamic.
 * 
 * @author iperdomo
 */
public class MainLayoutComponent extends BaseTemplateComponent {

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.client.kernel.BaseTemplateComponent#getComponentTemplate()
   */
  @Override
  protected Template getComponentTemplate() {
    return OBDal.getInstance().get(Template.class, ApplicationConstants.MAIN_LAYOUT_TEMPLATE_ID);
  }

  public Collection<NBComponent> getNavigationBarComponents() {
    return NavigationBarComponentGenerator.getInstance()
        .getDynamicNavigationBarComponents(getParameters());
  }

  public String getVersion() {
    return getETag();
  }

  @Override
  public String getETag() {
    // also encodes the role id in the etag
    if (getModule().isInDevelopment() != null && getModule().isInDevelopment()) {
      return super.getETag();
    } else {
      return OBContext.getOBContext().getLanguage().getId() + "_"
          + OBContext.getOBContext().getRole().getId() + "_" + getModule().getVersion();
    }
  }
}
