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
 * All portions are Copyright (C) 2001-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;

/**
 * @author Fernando Iriazabal
 * 
 *         Handler to manage all the process for the tab's query building.
 */
public class TableSQLData {
  private static Logger log4j = LogManager.getLogger();

  /**
   * Defines how many rows will be shown at maximum in any datagrid inside the scrollable area. If
   * there are more rows in the source table multiple pages of this size will be used.
   */
  public static final int maxRowsPerGridPage = 10000;

  /**
   * This function retrieves the stored backend page number for the given key from the session,
   * adjust it if needed based on the movePage request parameter, stored the new value into the
   * session and return it to the caller.
   * 
   * @param vars
   * @param currPageKey
   * @throws ServletException
   */
  public static int calcAndGetBackendPage(VariablesSecureApp vars, String currPageKey)
      throws ServletException {

    String movePage = vars.getStringParameter("movePage", "");
    log4j.debug("movePage action: " + movePage);

    // get current page
    String strPage = vars.getSessionValue(currPageKey, "0");
    int page = Integer.valueOf(strPage);

    // reset page on filter change
    if (vars.getStringParameter("newFilter", "0").equals("1")) {
      page = 0;
      vars.setSessionValue(currPageKey, String.valueOf(page));
    }

    // need to change page?
    if (movePage.length() > 0) {
      if (movePage.equals("FIRSTPAGE")) {
        page = 0;
      } else if (movePage.equals("PREVIOUSPAGE")) {
        page = Math.max(--page, 0);
        log4j.debug("page-- newPage=" + page);
      } else if (movePage.equals("NEXTPAGE")) {
        page++;
        log4j.debug("page++ newPage=" + page);
      } else {
        throw new ServletException("Unknown action for movePage: " + movePage);
      }
      vars.setSessionValue(currPageKey, String.valueOf(page));
    }

    return page;
  }

}
