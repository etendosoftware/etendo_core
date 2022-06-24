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
 * All portions are Copyright (C) 2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB.CheckAvailableCredit = {};

OB.CheckAvailableCredit.onLoad = function (view) {
  var callback, form = view.theForm,
      businessPartnerId = view.parentWindow.view.getContextInfo().C_BPartner_ID,
      currencyId = form.getItem('C_Currency_ID').getValue();

  callback = function (response, data, request) {
    if (data.availableCredit) {
      view.messageBar.setMessage(isc.OBMessageBar.TYPE_WARNING, null, OB.I18N.getLabel('BPCurrencyChange'));
      form.getItem('c_glitem_id').visible = true;
      form.getItem('c_glitem_id').setRequired(true);
      if (form.getItem('c_glitem_id').textBoxStyle.indexOf('Required') == -1) {
        form.getItem('c_glitem_id').textBoxStyle += 'Required';
      }
    } else {
      form.getItem('c_glitem_id').visible = false;
    }
    form.redraw();
    if (view) {
      view.handleButtonsStatus();
    }
  };

  OB.RemoteCallManager.call('org.openbravo.common.actionhandler.CheckAvailableCreditActionHandler', {
    businessPartnerId: businessPartnerId,
    currencyId: currencyId
  }, {}, callback);
};

OB.CheckAvailableCredit.onProcess = function (view, actionHandlerCall, clientSideValidationFail) {
  var form = view.theForm,
      currencyFromId = view.parentWindow.view.getContextInfo().inpbpCurrencyId,
      currencyToId = form.getItem('C_Currency_ID').getValue(),
      glItemId = form.getItem('c_glitem_id').getValue(),
      setAmount = form.getItem("Amount").getValue(),
      currentBalance = view.parentWindow.view.getContextInfo().inpsoCreditused,
      foreignAmount = form.getItem('Foreign_Amount').getValue();

  if (currencyFromId !== currencyToId && glItemId && setAmount === true && currentBalance === 0 && foreignAmount !== 0) {
    view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('BPCurrencyChangeRate'));
    return clientSideValidationFail();
  } else {
    actionHandlerCall();
  }
};