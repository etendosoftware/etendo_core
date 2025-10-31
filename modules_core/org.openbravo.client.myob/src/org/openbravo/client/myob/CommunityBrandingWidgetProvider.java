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
 * All portions are Copyright (C) 2010-2011 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.myob;

import jakarta.enterprise.context.RequestScoped;

/**
 * Provides the community branding widget.
 * 
 * @author mtaal
 */
@RequestScoped
public class CommunityBrandingWidgetProvider extends WidgetProvider {

  private static final String WIDGETCLIENTCLASS = "OBCommunityBrandingWidget";

  @Override
  public String generate() {
    throw new UnsupportedOperationException(
        "URLWidget definition should be pre-loaded on the client");
  }

  @Override
  public String getClientSideWidgetClassName() {
    return WIDGETCLIENTCLASS;
  }

}
