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
 * All portions are Copyright (C) 2014-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB.APRM.AddTransaction = {};

OB.APRM.AddTransaction.onLoad = function(view) {
  var bankStatementLineId = view.callerField.record.id;
  view.theForm.addField(
    isc.OBTextItem.create({
      name: 'bankStatementLineId',
      value: bankStatementLineId
    })
  );
  view.theForm.hideItem('bankStatementLineId');
  view.theForm
    .getItem('ad_org_id_process')
    .setValue(view.theForm.getItem('ad_org_id').getValue());
};

OB.APRM.AddTransaction.onProcess = function(
  view,
  actionHandlerCall,
  clientSideValidationFail
) {
  var execute;

  execute = function(ok) {
    if (ok) {
      actionHandlerCall();
    } else {
      clientSideValidationFail();
    }
  };

  // Called from Match Statement grid when we have view.callerField.record.match
  if (
    view &&
    view.callerField &&
    view.callerField.record &&
    view.callerField.record.match &&
    typeof view.getContextInfo === 'function'
  ) {
    var blineAmt = view.callerField.record.amount,
      trxDepositAmt = view.getContextInfo().depositamt,
      trxPaymentAmt = view.getContextInfo().withdrawalamt,
      trxAmt = trxDepositAmt - trxPaymentAmt,
      paymentId = view.getContextInfo().fin_payment_id,
      glitemId = view.getContextInfo().c_glitem_id,
      trxType = view.getContextInfo().trxtype,
      hideSplitConfirmation = OB.PropertyStore.get(
        'APRM_MATCHSTATEMENT_HIDE_PARTIALMATCH_POPUP',
        view.windowId
      );

    if (('BPD' === trxType || 'BPW' === trxType) && !glitemId && !paymentId) {
      view.messageBar.setMessage(
        isc.OBMessageBar.TYPE_ERROR,
        null,
        OB.I18N.getLabel('APRM_INVALID_TRANSACTION')
      );
      clientSideValidationFail();
    } else if (trxAmt !== blineAmt) {
      // Split required
      if (hideSplitConfirmation === 'Y') {
        // Continue with the match
        actionHandlerCall();
      } else {
        isc.confirm(
          OB.I18N.getLabel('APRM_SplitBankStatementLineConfirm', [
            OB.Utilities.Number.JSToOBMasked(
              blineAmt,
              OB.Format.defaultNumericMask,
              OB.Format.defaultDecimalSymbol,
              OB.Format.defaultGroupingSymbol,
              OB.Format.defaultGroupingSize
            ),
            OB.Utilities.Number.JSToOBMasked(
              trxAmt,
              OB.Format.defaultNumericMask,
              OB.Format.defaultDecimalSymbol,
              OB.Format.defaultGroupingSymbol,
              OB.Format.defaultGroupingSize
            )
          ]),
          execute
        );
      }
    } else {
      // Continue with the match
      actionHandlerCall();
    }
  } else {
    actionHandlerCall();
  }
};

OB.APRM.AddTransaction.trxTypeOnChangeFunction = function(
  item,
  view,
  form,
  grid
) {
  if (item.getValue() === 'BPW') {
    form.getItem('depositamt').setDisabled(true);
    form.getItem('withdrawalamt').setDisabled(false);
    form.getItem('depositamt').setValue(Number('0'));
  } else if (item.getValue() === 'BPD') {
    form.getItem('depositamt').setDisabled(false);
    form.getItem('withdrawalamt').setDisabled(true);
    form.getItem('withdrawalamt').setValue(Number('0'));
  } else {
    form.getItem('depositamt').setDisabled(false);
    form.getItem('withdrawalamt').setDisabled(false);
    form.getItem('description').setValue('');
  }
};

OB.APRM.AddTransaction.paymentOnChangeFunction = function(
  item,
  view,
  form,
  grid
) {
  var callback,
    strPaymentId = item.getValue(),
    strDescription = form.getItem('description').getValue();

  callback = function(response, data, request) {
    form.getItem('description').setValue(data.description);
    form.getItem('depositamt').setValue(data.depositamt);
    form.getItem('withdrawalamt').setValue(data.paymentamt);
    form.getItem('c_bpartner_id').setValue(data.cBpartnerId);
  };

  OB.RemoteCallManager.call(
    'org.openbravo.advpaymentmngt.actionHandler.AddTransactionOnChangePaymentActionHandler',
    {
      strPaymentId: strPaymentId,
      strDescription: strDescription
    },
    {},
    callback
  );
};

OB.APRM.AddTransaction.glitemOnChangeFunction = function(
  item,
  view,
  form,
  grid
) {
  var callback,
    strGLItemId = item.getValue(),
    strDescription = form.getItem('description').getValue();

  callback = function(response, data, request) {
    form.getItem('description').setValue(data.description);
  };

  OB.RemoteCallManager.call(
    'org.openbravo.advpaymentmngt.actionHandler.GLItemTransactionActionHandler',
    {
      strGLItemId: strGLItemId,
      strDescription: strDescription
    },
    {},
    callback
  );
};

OB.APRM.AddTransaction.trxDateOnChangeFunction = function(
  item,
  view,
  form,
  grid
) {
  form.getItem('dateacct').setDateParameterValue(new Date(item.getValue()));
};

OB.APRM.AddTransaction.organizationOnChangeFunction = function(
  item,
  view,
  form,
  grid
) {
  form
    .getItem('ad_org_id_process')
    .setValue(form.getItem('ad_org_id').getValue());
};

OB.APRM.AddTransaction.amtOnChangeFunction = function(item, view, form, grid) {
  var trxType = form.getItem('trxtype').getValue();
  if (trxType === 'BF') {
    if (item.name === 'depositamt') {
      form.getItem('withdrawalamt').setValue(Number('0'));
    } else if (item.name === 'withdrawalamt') {
      form.getItem('depositamt').setValue(Number('0'));
    }
  }
};
