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

import javax.inject.Inject;

import org.openbravo.email.EmailEventContentGenerator;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;

/**
 * Email generator for {@link AccountChangeObserver#EVT_ACCOUNT_CANCELLED} event which is triggered
 * when a portal user is set as inactive.
 * 
 * @author asier
 * 
 */
public class AccountCancelledEmailGenerator implements EmailEventContentGenerator {
  @Inject
  private AccountCancelledEmailBody body;

  @Override
  public String getSubject(Object data, String event) {
    return OBMessageUtils.getI18NMessage("Portal_AccountCancelledSubject",
        new String[] { ((User) data).getClient().getName() });
  }

  @Override
  public String getBody(Object data, String event) {
    body.setData((User) data);
    return body.generate();
  }

  @Override
  public String getContentType() {
    return "text/html; charset=utf-8";
  }

  @Override
  public boolean isValidEvent(String event, Object data) {
    return AccountChangeObserver.EVT_ACCOUNT_CANCELLED.equals(event);
  }

  @Override
  public int getPriority() {
    return 100;
  }

  @Override
  public boolean preventsOthersExecution() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isAsynchronous() {
    return true;
  }

  @Override
  public List<File> getAttachments(Object data, String event) {
    return null;
  }

}
