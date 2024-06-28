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
 * All portions are Copyright (C) 2022 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.dal.core;

/**
 * Provides cleanup methods for closing the DAL session of the current thread and also cleaning the
 * OBContext
 */
public class DalThreadCleaner {

  private static final DalThreadCleaner INSTANCE = new DalThreadCleaner();

  private DalThreadCleaner() {
  }

  /**
   * @return the DalThreadCleaner singleton instance
   */
  public static DalThreadCleaner getInstance() {
    return INSTANCE;
  }

  /**
   * Commits the default DAL session and closes it and the other sessions. Cleanups the OBContext.
   */
  public void cleanWithCommit() {
    clean(false);
  }

  /**
   * Rollbacks the default DAL session and closes it and the other sessions. Cleanups the OBContext.
   */
  public void cleanWithRollback() {
    clean(true);
  }

  private void clean(boolean errorOccured) {
    try {
      closeDefaultPoolSession(errorOccured);
    } finally {
      try {
        closeOtherSessions();
      } finally {
        SessionHandler.deleteSessionHandler();
        OBContext.setOBContext((OBContext) null);
      }
    }
  }

  private void closeDefaultPoolSession(boolean errorOccured) {
    SessionHandler sessionHandler = SessionHandler.isSessionHandlerPresent()
        ? SessionHandler.getInstance()
        : null;
    if (sessionHandler != null && sessionHandler.doSessionInViewPatter()) {
      // application software can force a rollback
      if (sessionHandler.getDoRollback() || errorOccured) {
        sessionHandler.rollback();
      } else if (sessionHandler.getSession().getTransaction().isActive()) {
        sessionHandler.commitAndClose();
      }
    }
  }

  private void closeOtherSessions() {
    if (SessionHandler.existsOpenedSessions()) {
      SessionHandler.getInstance().cleanUpSessions();
    }
  }
}
