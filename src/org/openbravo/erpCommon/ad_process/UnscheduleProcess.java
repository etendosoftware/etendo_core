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
 * All portions are Copyright (C) 2008-2020 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_process;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.OBScheduler;
import org.openbravo.scheduling.ProcessContext;
import org.quartz.SchedulerException;

public class UnscheduleProcess extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;

  private static final String PROCESS_REQUEST_ID = "AD_Process_Request_ID";
  private static final Logger log = LogManager.getLogger();

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    final VariablesSecureApp vars = new VariablesSecureApp(request);

    final String windowId = vars.getStringParameter("inpwindowId");
    final String requestId = vars.getSessionValue(windowId + "|" + PROCESS_REQUEST_ID);
    try {
      if (!OBScheduler.getInstance().isSchedulingAllowed()) {
        if (OBScheduler.getInstance().getScheduler().getMetaData().isJobStoreClustered()) {
          log.info("Not un-scheduling process because there is no scheduler instance active");
          advisePopUp(request, response, "ERROR",
              OBMessageUtils.messageBD("NoSchedulerInstanceActiveTitle"),
              OBMessageUtils.messageBD("NoSchedulerInstanceActiveMsg"));
        } else {
          log.info(
              "Not scheduling process because current context background policy is 'no-execute'");
          advisePopUp(request, response, "ERROR",
              OBMessageUtils.messageBD("BackgroundPolicyNoExecuteTitle"),
              OBMessageUtils.messageBD("BackgroundPolicyNoExecuteMsg"));
        }
        return;
      }

      OBScheduler.getInstance().unschedule(requestId, new ProcessContext(vars));
    } catch (final SchedulerException e) {
      String message = Utility.messageBD(this, "UNSCHED_ERROR", vars.getLanguage());
      String processErrorTit = Utility.messageBD(this, "Error", vars.getLanguage());
      advisePopUp(request, response, "ERROR", processErrorTit, message + " " + e.getMessage());
    }
    String message = Utility.messageBD(this, "UNSCHED_SUCCESS", vars.getLanguage());
    String processTitle = Utility.messageBD(this, "Success", vars.getLanguage());
    advisePopUpRefresh(request, response, "SUCCESS", processTitle, message);
  }
}
