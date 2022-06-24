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
 * All portions are Copyright (C) 2008-2019 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_process;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.ConnectionProviderContextListener;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.ui.ProcessGroup;
import org.openbravo.model.ad.ui.ProcessGroupList;
import org.openbravo.model.ad.ui.ProcessRequest;
import org.openbravo.scheduling.OBScheduler;
import org.openbravo.scheduling.ProcessBundle;

/**
 * Schedules a background process
 *
 * @author awolski
 */
public class ScheduleProcess extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;

  private static final String PROCESS_REQUEST_ID = "AD_Process_Request_ID";
  private static final String ERROR_MESSAGE = "SCHED_ERROR";
  private static final String SUCCESS_MESSAGE = "SCHED_SUCCESS";
  private static final Logger log = LogManager.getLogger();

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    final VariablesSecureApp vars = new VariablesSecureApp(request);
    final String requestId = getRequestId(vars);
    final String group = vars.getStringParameter("inpisgroup");

    try {
      if (!OBScheduler.getInstance().isSchedulingAllowed()) {
        if (OBScheduler.getInstance().getScheduler().getMetaData().isJobStoreClustered()) {
          log.info("Not scheduling process because current there is no scheduler instance active");
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

      // Avoid launch empty groups
      if (group.equals("Y") && isEmptyProcessGroup(requestId)) {
        advisePopUp(request, response, "ERROR", OBMessageUtils.getI18NMessage("Error", null),
            OBMessageUtils.getI18NMessage("PROGROUP_NoProcess",
                new String[] { getProcessGroup(requestId).getName() }));
        return;
      }
      // This class extends non serializable classes and is added to the data map
      // using the ConnectionProvider available in the context listener instead.
      final ProcessBundle bundle = ProcessBundle.request(requestId, vars,
          ConnectionProviderContextListener.getPool());
      OBScheduler.getInstance().schedule(requestId, bundle);

    } catch (final Exception e) {
      String message = Utility.messageBD(this, getErrorMessage(), vars.getLanguage());
      String processErrorTit = Utility.messageBD(this, "Error", vars.getLanguage());
      advisePopUp(request, response, "ERROR", processErrorTit, message);
      log.error("Error scheduling process request with ID {}", requestId, e);
    }
    String message = Utility.messageBD(this, getSuccessMessage(), vars.getLanguage());
    String processTitle = Utility.messageBD(this, "Success", vars.getLanguage());
    advisePopUpRefresh(request, response, "SUCCESS", processTitle, message);
  }

  private String getRequestId(VariablesSecureApp vars) {
    String windowId = vars.getStringParameter("inpwindowId");
    String requestId = vars.getSessionValue(windowId + "|" + PROCESS_REQUEST_ID);
    if (requestId.isEmpty()) {
      return vars.getStringParameter(PROCESS_REQUEST_ID);
    }
    return requestId;
  }

  private boolean isEmptyProcessGroup(String requestId) {
    OBCriteria<ProcessGroupList> processListCri = OBDal.getInstance()
        .createCriteria(ProcessGroupList.class);
    processListCri
        .add(Restrictions.eq(ProcessGroupList.PROPERTY_PROCESSGROUP, getProcessGroup(requestId)));
    processListCri.setMaxResults(1);
    return processListCri.uniqueResult() == null;
  }

  private ProcessGroup getProcessGroup(String requestId) {
    return OBDal.getInstance().get(ProcessRequest.class, requestId).getProcessGroup();
  }

  /**
   * @return the search key of the AD_MESSAGE used as the error message of the process.
   */
  protected String getErrorMessage() {
    return ERROR_MESSAGE;
  }

  /**
   * @return the search key of the AD_MESSAGE used as the success message of the process.
   */
  protected String getSuccessMessage() {
    return SUCCESS_MESSAGE;
  }
}
