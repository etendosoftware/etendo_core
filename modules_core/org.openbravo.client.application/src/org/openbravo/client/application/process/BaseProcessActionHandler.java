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
 * All portions are Copyright (C) 2010-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.process;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.util.Check;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.client.application.Process;
import org.openbravo.client.application.ProcessAccess;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.EntityAccessChecker;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.access.WindowAccess;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.service.db.DbUtility;

/**
 * 
 * @author iperdomo
 */
public abstract class BaseProcessActionHandler extends BaseActionHandler {

  private static final Logger log = LogManager.getLogger();

  private static final String GRID_REFERENCE_ID = "FF80818132D8F0F30132D9BC395D0038";

  @Override
  protected final JSONObject execute(Map<String, Object> parameters, String content) {
    String processId = null;
    try {
      OBContext.setAdminMode(true);

      processId = (String) parameters.get("processId");
      Check.isNotNull(processId, "Process ID missing in request");

      final Process processDefinition = OBDal.getInstance().get(Process.class, processId);
      Check.isNotNull(processDefinition, "Not valid process id");

      if (!hasAccess(processDefinition, parameters)) {
        JSONObject jsonRequest = new JSONObject();

        JSONObject err = new JSONObject();
        err.put("severity", "error");
        err.put("text", OBMessageUtils.getI18NMessage("OBUIAPP_NoAccess", null));
        jsonRequest.put("message", err);

        log.error("No access to process " + processDefinition);
        return jsonRequest;
      }

      JSONObject context = null;
      if (StringUtils.isNotEmpty(content)) {
        try {
          context = new JSONObject(content);
        } catch (JSONException e) {
          log.error("Error getting context for process definition " + processDefinition, e);
        }
      }
      for (Parameter param : processDefinition.getOBUIAPPParameterList()) {
        if (param.isFixed()) {
          if (param.isEvaluateFixedValue()) {
            parameters.put(param.getDBColumnName(),
                ParameterUtils.getParameterFixedValue(fixRequestMap(parameters, context), param));
          } else {
            parameters.put(param.getDBColumnName(), param.getFixedValue());
          }
        }
      }
      // Set information for audit trail
      SessionInfo.setProcessType("PD");
      SessionInfo.setProcessId(processId);
      SessionInfo.saveContextInfoIntoDB(OBDal.getInstance().getConnection(false));

      // Adds compatibility with legacy process definitions
      // If the handler of the process definition has not been updated, then it expects the
      // _selection and _allRows properties to be accessible directly from the _params object
      Process process = OBDal.getInstance().get(Process.class, processId);
      String updatedContent = content;
      if (process.isGridlegacy()) {
        log.warn("Process " + process.getName()
            + " is marked as Grid Legacy, you should consider migrating it to prevent parameter conversion");

        JSONObject jsonRequest = new JSONObject(content);
        if (!jsonRequest.isNull("_params")) {
          try {
            Parameter gridParameter = null;
            boolean shouldConvert = false;
            for (Parameter param : process.getOBUIAPPParameterList()) {
              if (GRID_REFERENCE_ID.equals(param.getReference().getId())) {
                if (gridParameter != null) {
                  log.error(
                      "Error while trying to conver parameters to legacy mode. There are more than one grid parameter. Not converting it.");
                  shouldConvert = false;
                } else {
                  gridParameter = param;
                  shouldConvert = true;
                }
              }
            }

            if (gridParameter == null) {
              log.info("There is no grid parameter in proces " + process.getName()
                  + ". No conversion is needed so Grid Legacy can be safelly unflagged.");
            }

            if (shouldConvert) {
              JSONObject jsonparams = jsonRequest.getJSONObject("_params");
              if (jsonparams.has(gridParameter.getDBColumnName())
                  && !jsonparams.isNull(gridParameter.getDBColumnName())) {
                JSONObject jsongrid = jsonparams.getJSONObject(gridParameter.getDBColumnName());
                jsonRequest.put("_selection", jsongrid.getJSONArray("_selection"));
                jsonRequest.put("_allRows", jsongrid.getJSONArray("_allRows"));
              }
              updatedContent = jsonRequest.toString();
            }
          } catch (Exception e) {
            log.error("Error while converting parameters. Sending them without conversion", e);
          }
        }
      }
      return doExecute(parameters, updatedContent);

    } catch (Exception e) {
      log.error("Error trying to execute process [{}]: ", processId, e);
      OBDal.getInstance().rollbackAndClose();

      final Throwable ex = DbUtility
          .getUnderlyingSQLException(e.getCause() != null ? e.getCause() : e);
      OBError msg = OBMessageUtils.translateError(ex.getMessage());
      return getResponseBuilder()
          .showMsgInProcessView(MessageType.ERROR, msg.getTitle(), msg.getMessage())
          .build();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Permissions to processes can be given in 2 ways:
   * <p>
   * Explicit grant to process definition access:
   * <ul>
   * <li>whenever the process is directly executed from menu
   * <li>in case the process is marked as "Requires Explicit Access Permission"
   * <li>in case there is a "Secured Process" preference for current window
   * </ul>
   * <p>
   * Inherited from window access in case it is invoked from a window button and none of the
   * previous conditions is satisfied.
   * 
   */
  public static boolean hasAccess(Process processDefinition, Map<String, Object> parameters) {
    // Check Process Definition Access Level
    String userLevel = OBContext.getOBContext().getUserLevel();
    int accessLevel = Integer.parseInt(processDefinition.getDataAccessLevel());
    if (!EntityAccessChecker.hasCorrectAccessLevel(userLevel, accessLevel)) {
      return false;
    }
    // Check Process Definition Permission
    String windowId = (String) parameters.get("windowId");
    if (windowId != null && !"null".equals(windowId)) {
      Window window = OBDal.getInstance().get(Window.class, windowId);

      boolean checkPermission = processDefinition.isRequiresExplicitAccessPermission();

      if (!checkPermission) {
        try {
          checkPermission = Preferences.YES.equals(Preferences.getPreferenceValue("SecuredProcess",
              true, OBContext.getOBContext().getCurrentClient(),
              OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
              OBContext.getOBContext().getRole(), window));
        } catch (PropertyException e) {
          // do nothing, property is not set so securedProcess is false
        }
      }
      if (!checkPermission) {
        // check if window is accessible
        OBCriteria<WindowAccess> qAccess = OBDal.getInstance().createCriteria(WindowAccess.class);
        qAccess.add(Restrictions.eq(WindowAccess.PROPERTY_WINDOW, window));
        qAccess
            .add(Restrictions.eq(WindowAccess.PROPERTY_ROLE, OBContext.getOBContext().getRole()));
        return qAccess.count() > 0;
      }
    }

    // The process now can be:
    // * Secured process invoked from window
    // * Invoked from menu (without window)
    // In any of these two cases, security is checked based on process access
    OBCriteria<ProcessAccess> qAccess = OBDal.getInstance().createCriteria(ProcessAccess.class);
    qAccess.add(Restrictions.eq(ProcessAccess.PROPERTY_OBUIAPPPROCESS, processDefinition));
    qAccess.add(Restrictions.eq(ProcessAccess.PROPERTY_ROLE, OBContext.getOBContext().getRole()));
    return qAccess.count() > 0;
  }

  /**
   * The request map is &lt;String, Object&gt; because includes the HTTP request and HTTP session,
   * is not required to handle process parameters
   * 
   * @deprecated use {@link BaseProcessActionHandler#fixRequestMap(Map, JSONObject)}
   */
  @Deprecated
  protected Map<String, String> fixRequestMap(Map<String, Object> parameters) {
    return fixRequestMap(parameters, null);
  }

  protected static ResponseActionsBuilder getResponseBuilder() {
    return new ResponseActionsBuilder();
  }

  protected abstract JSONObject doExecute(Map<String, Object> parameters, String content);
}
