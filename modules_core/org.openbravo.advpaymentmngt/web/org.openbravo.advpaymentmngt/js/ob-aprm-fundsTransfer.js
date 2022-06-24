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
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
OB.APRM.FundsTranfer = {
  depositToOnChange: function(item, view, form, grid) {
    // the callback called after the server side call returns
    var callback = function(response, data, request) {
      var currencyTo = form.getItem('c_currency_to_id');
      if (!currencyTo.valueMap) {
        currencyTo.valueMap = {};
      }
      currencyTo.valueMap[data.currencyID] = data.currencyISO;
      currencyTo.setValue(data.currencyID);

      // redraw popup after field change to update display logic
      form.redraw();
    };
    // do a server side call and on return call the callback
    OB.RemoteCallManager.call(
      'org.openbravo.advpaymentmngt.actionHandler.FundsTransferOnChangeDepositToActionHandler',
      {
        accountID: item.getValue()
      },
      {},
      callback
    );
  },
  onLoad: function(view) {
    var form = view.theForm,
      description = form.getItem('description');

    description.setValue(OB.I18N.getLabel('FundsTransfer'));
  }
};
