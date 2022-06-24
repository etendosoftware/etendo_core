package com.smf.jobs.defaults;

import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonUtility;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;

import java.util.List;


public class Post extends Action {

  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    Logger log = LogManager.getLogger();
    ActionResult result = new ActionResult();
    try {
      List<BaseOBObject> registers = getInputContents(getInputClass());
      var vars = RequestContext.get().getVariablesSecureApp();
      result.setType(Result.Type.SUCCESS);
      int errors = 0;
      int success = 0;

      for (BaseOBObject register : registers) {
        OBError messageResult;
        String posted = (String) register.get("posted");
        Organization org = (Organization) register.get("organization");
        Client client = (Client) register.get("client");
        String tableId = register.getEntity().getTableId();
        if (!"Y".equals(posted)) {
          messageResult = ActionButtonUtility.processButton(vars, register.getId().toString(), tableId, org.getId(), new DalConnectionProvider());
        } else {
          messageResult = ActionButtonUtility.resetAccounting(vars, client.getId(), org.getId(), tableId, register.getId().toString(), new DalConnectionProvider());
        }
        if ("error".equalsIgnoreCase(messageResult.getType())) {
          result.setType(Result.Type.ERROR);
          errors++;
        }
        if ("success".equalsIgnoreCase(messageResult.getType())) {
          result.setType(Result.Type.SUCCESS);
          success++;
        }
        result.setMessage(messageResult.getTitle().concat(": ").concat(messageResult.getMessage()));
      }
      massiveMessageHandler(result, registers, errors, success);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      result.setType(Result.Type.ERROR);
      result.setMessage(e.getMessage());
    }
    return result;
  }

  private void massiveMessageHandler(ActionResult result, List<BaseOBObject> registers, int errors, int success) {
    if (registers.size() > 1) {
      if (success == registers.size()) {
        result.setType(Result.Type.SUCCESS);
      } else if (errors == registers.size()) {
        result.setType(Result.Type.ERROR);
      } else {
        result.setType(Result.Type.WARNING);
      }
      result.setMessage(String.format(OBMessageUtils.messageBD("DJOBS_PostUnpostMessage"), success, errors));
      result.setOutput(getInput());
    }
  }

  @Override
  protected Class<BaseOBObject> getInputClass() {
    return BaseOBObject.class;
  }
}