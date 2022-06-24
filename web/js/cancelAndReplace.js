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
 * All portions are Copyright (C) 2016-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB.CancelAndReplace = {};
OB.CancelAndReplace.ClientSideEventHandlersPreSaveUpdate = {};
OB.CancelAndReplace.ClientSideEventHandlersPreDelete = {};
OB.CancelAndReplace.SALES_ORDERLINES_TAB = '187';

OB.CancelAndReplace.ClientSideEventHandlersPreSaveUpdate.showMessage = function (view, form, grid, extraParameters, actions) {
  var data = extraParameters.data,
      newOrderedQuantity = data.orderedQuantity,
      replacementRecords = [];


  callback = function (response, cdata, request) {
    if (cdata && cdata.result.length && cdata.result[0].deliveredQuantity > newOrderedQuantity) {
      // Update flow
      view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('CannotOrderLessThanDeliveredInCancelReplace'));
      return;
    }
    OB.EventHandlerRegistry.callbackExecutor(view, form, grid, extraParameters, actions);
  }

  if (data.replacedorderline && !extraParameters.isNewRecord && view.getParentRecord().documentStatus === 'TMP') {
    replacementRecords.push(data);
    // Calling action handler
    OB.RemoteCallManager.call('org.openbravo.common.actionhandler.CancelAndReplaceGetCancelledOrderLine', {
      records: replacementRecords
    }, {}, callback);
  } else {
    OB.EventHandlerRegistry.callbackExecutor(view, form, grid, extraParameters, actions);
  }

};

OB.EventHandlerRegistry.register(OB.CancelAndReplace.SALES_ORDERLINES_TAB, OB.EventHandlerRegistry.PRESAVE, OB.CancelAndReplace.ClientSideEventHandlersPreSaveUpdate.showMessage, 'OBCancelAndReplace_ShowMessage');

OB.CancelAndReplace.ClientSideEventHandlersPreDelete.showMessage = function (view, form, grid, extraParameters, actions) {
  var recordsToDelete = extraParameters.recordsToDelete,
      replacementRecords = [],
      record, deliveredQuantity;

  callback = function (response, cdata, request) {
    for (i = 0; i < cdata.result.length; i++) {
      record = cdata.result[i].record;
      deliveredQuantity = cdata.result[i].deliveredQuantity;
      if (deliveredQuantity !== 0) {
        var msgInfo = [];
        msgInfo.push(record.lineNo);
        msgInfo.push(record.product$_identifier);
        view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('CannotDeleteLineWithDeliveredQtyInReplacementLine', msgInfo));
        return;
      }
    }
    OB.EventHandlerRegistry.callbackExecutor(view, form, grid, extraParameters, actions);
  };

  if (view.getParentRecord().documentStatus === 'TMP') {
    for (i = 0; i < recordsToDelete.length; i++) {
      record = recordsToDelete[i];
      if (record.replacedorderline) {
        replacementRecords.push(record);
      }
    }

    if (replacementRecords.length) {
      //Calling action handler
      OB.RemoteCallManager.call('org.openbravo.common.actionhandler.CancelAndReplaceGetCancelledOrderLine', {
        records: replacementRecords
      }, {}, callback);
    } else {
      OB.EventHandlerRegistry.callbackExecutor(view, form, grid, extraParameters, actions);
    }
  } else {
    OB.EventHandlerRegistry.callbackExecutor(view, form, grid, extraParameters, actions);
  }
};

OB.EventHandlerRegistry.register(OB.CancelAndReplace.SALES_ORDERLINES_TAB, OB.EventHandlerRegistry.PREDELETE, OB.CancelAndReplace.ClientSideEventHandlersPreDelete.showMessage, 'OBCancelAndReplace_ShowMessage');