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

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.openbravo.dal.core.OBContext;
import org.openbravo.email.EmailEventContentGenerator;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;

/**
 * Email generator for {@link GrantPortalAccessProcess#EVT_NEW_USER} event which is triggered when a
 * user is granted with portal privileges.
 * 
 * @author asier
 * 
 */
public class NewUserEmailGenerator implements EmailEventContentGenerator {

  @Inject
  private NewUserEmailBody body;

  @Override
  public String getSubject(Object data, String event) {
    String msg;
    @SuppressWarnings("unchecked")
    User user = (User) ((Map<String, Object>) data).get("user");
    if (user.isGrantPortalAccess()) {
      msg = "Portal_PasswordChanged";
    } else {
      msg = "Portal_UserWelcomeSubject";
    }
    return OBMessageUtils.getI18NMessage(msg,
        new String[] { OBContext.getOBContext().getCurrentClient().getName() });
  }

  @SuppressWarnings("unchecked")
  @Override
  public String getBody(Object data, String event) {
    body.setData((Map<String, Object>) data);
    return body.generate();
  }

  @Override
  public String getContentType() {
    return "text/html; charset=utf-8";
  }

  @Override
  public boolean isValidEvent(String event, Object data) {
    return GrantPortalAccessProcess.EVT_NEW_USER.equals(event);
  }

  @Override
  public int getPriority() {
    return 100;
  }

  @Override
  public boolean preventsOthersExecution() {
    return false;
  }

  @Override
  public boolean isAsynchronous() {
    return false;
  }

  @Override
  public List<File> getAttachments(Object data, String event) {
    return null;
  }
}
