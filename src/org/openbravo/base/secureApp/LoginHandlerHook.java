package org.openbravo.base.secureApp;

import org.openbravo.erpCommon.utility.OBError;

public interface LoginHandlerHook {

  /*
   * Returns an OBError when an error occurred and null if it succeeds
   */
  OBError process(String userName, String action);
}
