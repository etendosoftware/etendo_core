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
 * All portions are Copyright (C) 2009-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.base;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.SessionInfo;

public class SystemStatusListener implements ServletContextListener {
  private static final Logger logger = LogManager.getLogger();

  @Override
  public void contextDestroyed(ServletContextEvent arg0) {

  }

  /*
   * This context listener checks if the last build went well, and if it did, it updates the status
   * to reflect that Tomcat was restarted
   */
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    ConnectionProvider cp = ConnectionProviderContextListener.getPool(sce.getServletContext());
    try {
      String st = SystemStatusListenerData.getSystemStatus(cp);
      if (st.equals("RB60") || st.equals("RB50") || st.equals("RB51")) {
        SystemStatusListenerData.setSystemStatus(cp, "RB70");
      }
    } catch (ServletException e) {
      logger.error("Error while updating system status", e);
    } finally {
      // detaching db connection from thread so can it be returned to pool
      SessionInfo.init();
    }
  }
}
