/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.portal;

import org.openbravo.client.kernel.BaseTemplateComponent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.email.EmailEventContentGenerator;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;

/**
 * Convenience class that provides a set of common utilities for templates of emails sent by portal
 * events.
 * 
 * @see EmailEventContentGenerator
 * @author asier
 * 
 */
public abstract class PortalEmailBody extends BaseTemplateComponent {
  public String getClientName() {
    return OBContext.getOBContext().getCurrentClient().getName();
  }

  public String getUrl() {
    String url = "";
    try {
      url = Preferences.getPreferenceValue("PortalURL", true,
          OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), null, null, null);
    } catch (PropertyException e) {
      // no preference set, ignore it
    }
    return url;
  }

  public String getContactEmail() {
    String email = "";
    try {
      email = Preferences.getPreferenceValue("PortalContactEmail", true,
          OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), null, null, null);
    } catch (PropertyException e) {
      // no preference set, ignore it
    }
    return email;
  }

}
