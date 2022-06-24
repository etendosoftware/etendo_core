/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2014-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB.ValidateCostingRule = {};


OB.ValidateCostingRule.onLoad = function (view) {
  view.messageBar.setMessage(isc.OBMessageBar.TYPE_INFO, null, OB.I18N.getLabel('CostingRuleHelp'));
};


OB.ValidateCostingRule.onProcess = function (view, actionHandlerCall, clientSideValidationFail) {
  var callbackOnProcessActionHandler, execute;
  callbackOnProcessActionHandler = function (response, data, request) {
    execute = function (ok) {
      if (ok) {
        actionHandlerCall();
      } else {
        view.parentElement.parentElement.closeClick();
      }
    };
    if (data.message.text) {
      isc.confirm(data.message.text, execute);
    } else {
      actionHandlerCall();
    }
  };
  view.messageBar.setMessage(isc.OBMessageBar.TYPE_INFO, null, OB.I18N.getLabel('CostingRuleHelp'));
  OB.RemoteCallManager.call('org.openbravo.costing.CostingRuleProcessOnProcessHandler', {
    ruleId: view.parentWindow.view.lastRecordSelected.id
  }, {}, callbackOnProcessActionHandler);
};