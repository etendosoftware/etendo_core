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

import org.openbravo.client.kernel.Template;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;

/**
 * Generates the body for {@link AccountCancelledEmailGenerator}
 * 
 * @author asier
 * 
 */
public class AccountCancelledEmailBody extends PortalEmailBody {
  private User user;

  @Override
  protected Template getComponentTemplate() {
    return OBDal.getInstance().get(Template.class, "E5D5653B19734DA5AE3BEB7019B3D1E7");
  }

  public void setData(User data) {
    this.user = data;
  }

  public User getUser() {
    return this.user;
  }

  @Override
  public Object getData() {
    return this;
  }
}
