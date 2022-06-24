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
 * All portions are Copyright (C) 2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):
 ************************************************************************
 */

package org.openbravo.client.application.businesslogic;

import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.module.Module;
import org.openbravo.service.centralrepository.CentralRepository;
import org.openbravo.service.centralrepository.CentralRepository.Service;

/** Process to register a module in Central Repository */
public class RegisterModuleActionHandler extends BaseProcessActionHandler {

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject registationInfo = getRegistrationInfo(content);

    // Do not keep connection open while communicating with WS
    OBDal.getInstance().commitAndClose();

    JSONObject crResponse = CentralRepository.executeRequest(Service.REGISTER_MODULE,
        registationInfo);

    try {
      ResponseActionsBuilder rb = getResponseBuilder();
      String msg = OBMessageUtils
          .getI18NMessage(crResponse.getJSONObject("response").getString("msg"));
      if (crResponse.getBoolean("success")) {
        rb.showMsgInProcessView(MessageType.SUCCESS, OBMessageUtils.getI18NMessage("ProcessOK"),
            msg);
        Module module = OBDal.getInstance()
            .get(Module.class, registationInfo.getJSONObject("module").get("moduleID"));
        module.setRegisterModule(true);
      } else {
        rb.showMsgInProcessView(MessageType.ERROR, OBMessageUtils.getI18NMessage("Error"), msg,
            true).retryExecution();
      }

      return rb.build();
    } catch (JSONException e) {
      throw new OBException(e);
    }
  }

  private JSONObject getRegistrationInfo(String content) {
    JSONObject r = new JSONObject();
    try {
      JSONObject req = new JSONObject(content);
      JSONObject params = req.getJSONObject("_params");
      r.put("user", params.getString("user"));
      r.put("password", params.getString("password"));

      Module module = OBDal.getInstance().get(Module.class, req.getString("AD_Module_ID"));
      JSONObject jsonModule = new JSONObject();
      jsonModule.put("moduleID", module.getId());
      jsonModule.put("name", module.getName());
      jsonModule.put("packageName", module.getJavaPackage());
      jsonModule.put("author", module.getAuthor());
      jsonModule.put("type", module.getType());
      jsonModule.put("help", module.getHelpComment());
      if (!module.getModuleDBPrefixList().isEmpty()) {
        jsonModule.put("dbPrefix", module.getModuleDBPrefixList().get(0).getName());
      }
      jsonModule.put("description", module.getDescription());
      r.put("module", jsonModule);
    } catch (JSONException e) {
      throw new OBException(e);
    }
    return r;
  }

}
