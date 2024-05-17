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
 * All portions are Copyright (C) 2008-2022 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.dal.core;

/**
 * Encapsulates a thread so that when the thread returns the session/transaction is
 * closed/committed/rolledback. It also ensures that the OBContext is removed from the thread.
 * 
 * Note that cleaning up the thread is particularly important in webcontainer environments because
 * webcontainers (tomcat) re-use thread instances for new requests (using a threadpool).
 * 
 * @author mtaal
 */

public abstract class DalThreadHandler extends ThreadHandler {

  /** @see ThreadHandler#doBefore */
  @Override
  public void doBefore() {
  }

  /** @see ThreadHandler#doFinal */
  @Override
  public void doFinal(boolean errorOccured) {
    DalThreadCleaner cleaner = DalThreadCleaner.getInstance();
    if (errorOccured) {
      cleaner.cleanWithRollback();
    } else {
      cleaner.cleanWithCommit();
    }
  }
}
