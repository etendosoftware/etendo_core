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

OB.APRM.FindTransactions = {};

OB.APRM.FindTransactions.onProcess = function(
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

  if (
    view &&
    typeof view.getContextInfo === 'function' &&
    view.callerField &&
    view.callerField.view &&
    typeof view.callerField.view.getContextInfo === 'function'
  ) {
    var i,
      trxSelection = view.getContextInfo().findtransactiontomatch._selection;

    if (trxSelection && trxSelection[0]) {
      var totalTrxAmt = BigDecimal.prototype.ZERO,
        blineAmt = new BigDecimal(String(view.callerField.record.amount)),
        hideSplitConfirmation = OB.PropertyStore.get(
          'APRM_MATCHSTATEMENT_HIDE_PARTIALMATCH_POPUP',
          view.windowId
        );
      for (i = 0; i < trxSelection.length; i++) {
        var trxDepositAmt = new BigDecimal(
            String(trxSelection[i].depositAmount)
          ),
          trxPaymentAmt = new BigDecimal(String(trxSelection[i].paymentAmount)),
          trxAmt = trxDepositAmt.subtract(trxPaymentAmt);
        totalTrxAmt = totalTrxAmt.add(trxAmt);
      }
      if (totalTrxAmt.compareTo(blineAmt) !== 0) {
        // Split required
        if (hideSplitConfirmation === 'Y') {
          // Continue with the match
          actionHandlerCall();
        } else {
          if (isc.isA.emptyObject(OB.TestRegistry.registry)) {
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
                  totalTrxAmt,
                  OB.Format.defaultNumericMask,
                  OB.Format.defaultDecimalSymbol,
                  OB.Format.defaultGroupingSymbol,
                  OB.Format.defaultGroupingSize
                )
              ]),
              execute
            );
          } else {
            execute(true);
          }
        }
      } else {
        // Continue with the match
        actionHandlerCall();
      }
    } else {
      // No Transaction selected
      view.messageBar.setMessage(
        isc.OBMessageBar.TYPE_ERROR,
        null,
        OB.I18N.getLabel('APRM_SELECT_RECORD_ERROR')
      );
      clientSideValidationFail();
    }
  }
};
