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
 * All portions are Copyright (C) 2009-2025 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.instancemanagement;

import java.util.HashMap;

import javax.enterprise.context.Dependent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.process.ResponseActionsBuilder;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_process.HeartbeatProcess;
import org.openbravo.erpCommon.obps.ActivationKey;
import org.openbravo.erpCommon.obps.ActiveInstanceProcess;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.System;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;

import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Data;
import com.smf.jobs.Result;

/**
 * Action for managing instance activation and deactivation.
 * This replaces the legacy HttpSecureAppServlet implementation.
 *
 * @author Etendo
 */
@Dependent
public class InstanceManagementAction extends Action {
  private static final Logger log = LogManager.getLogger();

  // Constants for JSON parameter keys (these come from the UI form, not AD parameters)
  private static final String COMMAND = "command";
  private static final String PUBLIC_KEY = "publicKey";
  private static final String PURPOSE = "purpose";
  private static final String INSTANCE_NO = "instanceNo";
  private static final String FILE_CONTENT = "fileContent";
  public static final String SUCCESS = "Success";

  @Override
  protected Data preRun(JSONObject jsonContent) {
    // This action doesn't process entity records, so we return an empty Data object
    // to avoid the ENTITY_NAME lookup error with BaseOBObject
    return new Data();
  }

  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    ActionResult result = new ActionResult();
    result.setType(Result.Type.SUCCESS);

    try {
      String command = parameters.optString(COMMAND, "DEFAULT");

      log.debug("Instance Management Action - Command: {}", command);
      log.debug("Parameters: {}", parameters);

      switch (command) {
        case "ACTIVATE":
          result = handleActivate(parameters);
          break;
        case "DEACTIVATE":
          result = handleDeactivate();
          break;
        case "CANCEL":
          result = handleCancel();
          break;
        case "INSTALLFILE":
          result = handleInstallFile(parameters);
          break;
        default:
          result.setType(Result.Type.INFO);
          result.setMessage("Instance management action executed successfully");
          break;
      }

    } catch (Exception e) {
      log.error("Error executing instance management action", e);
      result.setType(Result.Type.ERROR);
      result.setMessage("Error: " + e.getMessage());
    }

    return result;
  }

  /**
   * Handles the activation of an instance
   */
  private ActionResult handleActivate(JSONObject parameters) {
    ActionResult result = new ActionResult();
    ResponseActionsBuilder responseActions = getResponseBuilder();

    try {
      // Get parameters from the request (coming from the UI form)
      // If publicKey is not in parameters, get it from ActivationKey
      String publicKey = parameters.optString(PUBLIC_KEY, "");
      if (publicKey.isEmpty()) {
        ActivationKey ak = ActivationKey.getInstance();
        if (ak.hasActivationKey()) {
          publicKey = ak.getPublicKey();
        }
      }
      String purpose = parameters.optString(PURPOSE, "");
      String instanceNo = parameters.optString(INSTANCE_NO, "");
      OBError processResult = activateCancelRemote(publicKey, purpose, instanceNo, true);

      if (StringUtils.equals(SUCCESS, processResult.getType())) {
        result.setType(Result.Type.SUCCESS);
        result.setMessage(processResult.getMessage());
        responseActions.showMsgInProcessView(ResponseActionsBuilder.MessageType.SUCCESS,
            processResult.getMessage());
        responseActions.setRefreshParent(true);

        // Check for heartbeat warning
        ActivationKey ak = ActivationKey.getInstance();
        ConnectionProvider cp = new DalConnectionProvider(false);
        VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

        if (ak.isActive() && ak.isTrial() && !ak.isHeartbeatActive()) {
          result.setType(Result.Type.WARNING);
          String title = Utility.messageBD(cp, "OPS_NOT_HB_ACTIVE_TITLE", vars.getLanguage());
          String message = Utility.messageBD(cp, "OPS_NOT_HB_ACTIVE", vars.getLanguage());
          String warningMsg = title + ": " + message;
          result.setMessage(warningMsg);
          responseActions.showMsgInProcessView(ResponseActionsBuilder.MessageType.WARNING, warningMsg);
        }
      } else {
        result.setType(Result.Type.ERROR);
        result.setMessage(processResult.getMessage());
        responseActions.showMsgInProcessView(ResponseActionsBuilder.MessageType.ERROR,
            processResult.getMessage());

        // Enable retry on failure
        responseActions.retryExecution();
      }
    } catch (Exception e) {
      log.error("Error activating instance", e);
      result.setType(Result.Type.ERROR);
      String errorMsg = "Error activating instance: " + e.getMessage();
      result.setMessage(errorMsg);
      responseActions.showMsgInProcessView(ResponseActionsBuilder.MessageType.ERROR, errorMsg);

      // Enable retry on exception
      responseActions.retryExecution();
    }

    result.setResponseActionsBuilder(responseActions);
    result.setOutput(getInput());
    return result;
  }

  /**
   * Handles the deactivation of an instance
   */
  protected ActionResult handleDeactivate() {
    ActionResult result = new ActionResult();
    ResponseActionsBuilder responseActions = getResponseBuilder();
    OBContext.setAdminMode();
    ConnectionProvider cp = new DalConnectionProvider(false);

    try {
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

      // Check for commercial modules installed in the instance
      OBCriteria<Module> qMods = OBDal.getInstance().createCriteria(Module.class);
      qMods.add(Restrictions.eq(Module.PROPERTY_COMMERCIAL, true));
      qMods.add(Restrictions.eq(Module.PROPERTY_ENABLED, true));
      qMods.addOrder(Order.asc(Module.PROPERTY_NAME));

      // core can be commercial, do not take it into account
      qMods.add(Restrictions.ne(Module.PROPERTY_ID, "0"));

      boolean deactivable = true;
      StringBuilder commercialModules = new StringBuilder();

      for (Module mod : qMods.list()) {
        deactivable = false;
        commercialModules.append("<br/>").append(mod.getName());
      }

      if (!deactivable) {
        String errorMsg = Utility.messageBD(cp, "CannotDeactivateWithCommercialModules", vars.getLanguage())
            + commercialModules.toString();
        result.setType(Result.Type.ERROR);
        result.setMessage(errorMsg);
        responseActions.showMsgInProcessView(ResponseActionsBuilder.MessageType.ERROR, errorMsg);

        // Enable retry on validation failure
        responseActions.retryExecution();
      } else {
        // Deactivate instance
        System sys = OBDal.getInstance().get(System.class, "0");
        sys.setActivationKey(null);
        sys.setInstanceKey(null);
        OBDal.getInstance().flush();
        ActivationKey.reload();

        String successMsg = Utility.messageBD(cp, SUCCESS, vars.getLanguage());
        result.setType(Result.Type.SUCCESS);
        result.setMessage(successMsg);
        responseActions.showMsgInProcessView(ResponseActionsBuilder.MessageType.SUCCESS, successMsg);
        responseActions.setRefreshParent(true);

        ActiveInstanceProcess.updateShowProductionFields("N");

        // When deactivating a cloned instance insert a dummy heartbeat log so it is not detected as
        // a cloned instance anymore.
        if (HeartbeatProcess.isClonedInstance()) {
          ActiveInstanceProcess.insertDummyHBLog();
        }
      }

    } catch (Exception e) {
      log.error("Error deactivating instance", e);
      String errorMsg = Utility.parseTranslation(cp, RequestContext.get().getVariablesSecureApp(),
          RequestContext.get().getVariablesSecureApp().getLanguage(), e.getMessage());
      result.setType(Result.Type.ERROR);
      result.setMessage(errorMsg);
      responseActions.showMsgInProcessView(ResponseActionsBuilder.MessageType.ERROR, errorMsg);

      // Enable retry on exception
      responseActions.retryExecution();
    } finally {
      OBContext.restorePreviousMode();
    }

    result.setResponseActionsBuilder(responseActions);
    result.setOutput(getInput());
    return result;
  }

  /**
   * Handles the cancellation of an instance
   */
  private ActionResult handleCancel() {
    ActionResult result = new ActionResult();
    ResponseActionsBuilder responseActions = getResponseBuilder();

    try {
      // Get parameters from DB for cancellation
      System sys = OBDal.getInstance().get(System.class, "0");
      String publicKey = sys.getInstanceKey();
      String instanceNo = ActivationKey.getInstance().getProperty(INSTANCE_NO);
      String purpose = ActivationKey.getInstance().getProperty(PURPOSE);

      OBError processResult = activateCancelRemote(publicKey, purpose, instanceNo, false);

      if (StringUtils.equals(SUCCESS, processResult.getType())) {
        result.setType(Result.Type.SUCCESS);
        result.setMessage(processResult.getMessage());
        responseActions.showMsgInProcessView(ResponseActionsBuilder.MessageType.SUCCESS,
            processResult.getMessage());
        responseActions.setRefreshParent(true);
      } else {
        result.setType(Result.Type.ERROR);
        result.setMessage(processResult.getMessage());
        responseActions.showMsgInProcessView(ResponseActionsBuilder.MessageType.ERROR,
            processResult.getMessage());

        // Enable retry on failure
        responseActions.retryExecution();
      }

    } catch (Exception e) {
      log.error("Error canceling instance", e);
      String errorMsg = "Error canceling instance: " + e.getMessage();
      result.setType(Result.Type.ERROR);
      result.setMessage(errorMsg);
      responseActions.showMsgInProcessView(ResponseActionsBuilder.MessageType.ERROR, errorMsg);

      // Enable retry on exception
      responseActions.retryExecution();
    }

    result.setResponseActionsBuilder(responseActions);
    result.setOutput(getInput());
    return result;
  }

  /**
   * Handles the installation of an activation key from a file
   */
  private ActionResult handleInstallFile(JSONObject parameters) {
    ActionResult result = new ActionResult();
    ResponseActionsBuilder responseActions = getResponseBuilder();

    try {
      // Get publicKey from request parameters (coming from the UI form)
      // If not provided, get it from ActivationKey
      String publicKey = parameters.optString(PUBLIC_KEY, "");
      if (publicKey.isEmpty()) {
        ActivationKey ak = ActivationKey.getInstance();
        if (ak.hasActivationKey()) {
          publicKey = ak.getPublicKey();
        }
      }
      String fileContent = parameters.optString(FILE_CONTENT);

      if (fileContent == null || fileContent.isEmpty()) {
        result.setType(Result.Type.ERROR);
        result.setMessage("No file content provided");
        responseActions.showMsgInProcessView(ResponseActionsBuilder.MessageType.ERROR,
            "No file content provided");

        // Enable retry on validation failure
        responseActions.retryExecution();

        result.setResponseActionsBuilder(responseActions);
        result.setOutput(getInput());
        return result;
      }

      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      ProcessBundle pb = new ProcessBundle(null, vars);
      HashMap<String, Object> params = new HashMap<>();

      params.put(PUBLIC_KEY, publicKey);
      params.put("activationKey", fileContent);
      params.put("activate", true);

      pb.setParams(params);

      new ActiveInstanceProcess().execute(pb);

      OBError msg = (OBError) pb.getResult();
      Result.Type resultType = Result.Type.valueOf(msg.getType().toUpperCase());
      result.setType(resultType);
      result.setMessage(msg.getMessage());

      // Show message based on result type
      ResponseActionsBuilder.MessageType msgType = ResponseActionsBuilder.MessageType.valueOf(
          msg.getType().toUpperCase());
      responseActions.showMsgInProcessView(msgType, msg.getMessage());

      if (resultType == Result.Type.SUCCESS) {
        responseActions.setRefreshParent(true);
      } else if (resultType == Result.Type.ERROR) {
        responseActions.retryExecution();
      }

    } catch (Exception e) {
      log.error("Error installing activation file", e);
      ConnectionProvider cp = new DalConnectionProvider(false);
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      String errorMsg = Utility.parseTranslation(cp, vars, vars.getLanguage(), e.getMessage());
      result.setType(Result.Type.ERROR);
      result.setMessage(errorMsg);
      responseActions.showMsgInProcessView(ResponseActionsBuilder.MessageType.ERROR, errorMsg);

      // Enable retry on exception
      responseActions.retryExecution();
    }

    result.setResponseActionsBuilder(responseActions);
    result.setOutput(getInput());
    return result;
  }

  /**
   * Activates or cancels the instance remotely
   *
   * @param publicKey
   *     The public key (from UI form)
   * @param purpose
   *     The instance purpose (from UI form)
   * @param instanceNo
   *     The instance number (from UI form)
   * @param activate
   *     true for activation, false for cancellation
   * @return OBError with the result of the operation
   */
  protected OBError activateCancelRemote(String publicKey, String purpose, String instanceNo,
      boolean activate) {
    try {
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      ProcessBundle pb = new ProcessBundle(null, vars);

      HashMap<String, Object> params = new HashMap<>();
      params.put("activate", activate);
      params.put(PUBLIC_KEY, publicKey);
      params.put(PURPOSE, purpose);
      params.put(INSTANCE_NO, instanceNo);

      pb.setParams(params);


      new ActiveInstanceProcess().execute(pb);

      return (OBError) pb.getResult();
    } catch (Exception e) {
      log.error("Error in activateCancelRemote", e);
      OBError errorMsg = new OBError();
      errorMsg.setType("Error");
      errorMsg.setMessage("Error in activateCancelRemote: " + e.getMessage());
      return errorMsg;
    }
  }

  @Override
  protected Class<?> getInputClass() {
    // This action doesn't operate on specific entities
    // We return Client.class to avoid ENTITY_NAME error with BaseOBObject
    // The preRun() method will override the input with an empty Data object
    return org.openbravo.model.ad.access.User.class;
  }

  @Override
  protected Class<?> getOutputClass() {
    return null;
  }
}
