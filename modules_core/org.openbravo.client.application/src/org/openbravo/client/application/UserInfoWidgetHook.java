package org.openbravo.client.application;

import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.access.User;

public interface UserInfoWidgetHook {

  /*
   * Returns an OBError when an error occurred and null if it succeeds
   */
  OBError process(User user, String newPwd);
}
