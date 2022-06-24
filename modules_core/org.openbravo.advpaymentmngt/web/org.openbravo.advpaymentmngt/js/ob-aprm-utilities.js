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
 * All portions are Copyright (C) 2011-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB.APRM = {};

OB.APRM.bankTransitoryAccountCalloutResponse = function(
  me,
  confirmMessage,
  financialAccountId
) {
  isc.confirm(confirmMessage, function(value) {
    var post;
    if (value) {
      var bankTransitoryAccount = me.getField('fINTransitoryAcct')._value,
        bankTransitoryAccountDesc = me.getField('fINTransitoryAcct').valueMap[
          bankTransitoryAccount
        ];

      me.getField('clearedPaymentAccount').valueMap[
        bankTransitoryAccount
      ] = bankTransitoryAccountDesc;
      me.getField('clearedPaymentAccount').setValue(bankTransitoryAccount);
      me.getField('clearedPaymentAccountOUT').valueMap[
        bankTransitoryAccount
      ] = bankTransitoryAccountDesc;
      me.getField('clearedPaymentAccountOUT').setValue(bankTransitoryAccount);

      post = {
        eventType: 'bankTransitoryCalloutResponse',
        financialAccountId: financialAccountId
      };

      OB.RemoteCallManager.call(
        'org.openbravo.advpaymentmngt.APRMActionHandler',
        post,
        {},
        {}
      );
    }
  });
};

OB.APRM.validateMPPUserWarnedAwaiting = false;
OB.APRM.validateMPPUserWarnedSign = false;

OB.APRM.validateModifyPaymentPlanAmounts = function(
  item,
  validator,
  value,
  record
) {
  var indRow,
    allRows = item.grid.data.allRows,
    row,
    allGreen = true,
    totalExpected = new BigDecimal('0'),
    totalReceived = new BigDecimal('0'),
    totalOutstanding = new BigDecimal('0'),
    isNumber = isc.isA.Number,
    contextInfo = item.grid.view.parentWindow.activeView.getContextInfo(
      false,
      true,
      true,
      false
    ),
    invoiceOutstanding = new BigDecimal(String(contextInfo.inpoutstandingamt)),
    invoiceGrandTotal = new BigDecimal(String(contextInfo.inpgrandtotal)),
    invoicePaidTotal = new BigDecimal(String(contextInfo.inptotalpaid));

  if (
    new BigDecimal(String(value)).compareTo(new BigDecimal('0')) !== 0 &&
    new BigDecimal(String(value)).compareTo(new BigDecimal('0')) !==
      invoiceOutstanding.compareTo(new BigDecimal('0'))
  ) {
    if (!OB.APRM.validateMPPUserWarnedSign) {
      OB.APRM.validateMPPUserWarnedSign = true;
      isc.warn(OB.I18N.getLabel('APRM_DifferentSignError'));
    }
    return false;
  }

  for (indRow = 0; indRow < allRows.length; indRow++) {
    row = allRows[indRow];

    if (
      !isNumber(row.expected) ||
      !isNumber(row.outstanding) ||
      !isNumber(row.received)
    ) {
      return false;
    }

    totalExpected = totalExpected.add(new BigDecimal(String(row.expected)));
    totalOutstanding = totalOutstanding.add(
      new BigDecimal(String(row.outstanding))
    );
    totalReceived = totalReceived.add(new BigDecimal(String(row.received)));
  }
  if (
    totalOutstanding
      .abs()
      .compareTo(invoiceGrandTotal.subtract(invoicePaidTotal).abs()) !== 0
  ) {
    return false;
  }
  for (indRow = 0; indRow < allRows.length; indRow++) {
    row = allRows[indRow];
    row.expected = Number(
      new BigDecimal(String(row.outstanding)).add(
        new BigDecimal(String(row.received))
      )
    );
  }
  if (
    new BigDecimal(String(record.awaitingExecutionAmount))
      .abs()
      .compareTo(new BigDecimal(String(record.outstanding)).abs()) > 0
  ) {
    if (!OB.APRM.validateMPPUserWarnedAwaiting) {
      OB.APRM.validateMPPUserWarnedAwaiting = true;
      isc.warn(OB.I18N.getLabel('APRM_AwaitingExecutionAmountError'));
    }
    return false;
  }
  for (indRow = 0; indRow < allRows.length; indRow++) {
    if (
      typeof item.grid.rowHasErrors(allRows[indRow]) !== 'undefined' &&
      item.grid.rowHasErrors(allRows[indRow]) &&
      allRows[indRow] !== record
    ) {
      allGreen = false;
    }
  }
  if (allGreen) {
    OB.APRM.validateMPPUserWarnedAwaiting = false;
    OB.APRM.validateMPPUserWarnedSign = false;
  }
  return true;
};

OB.APRM.selectionChangePaymentProposalPickAndEdit = function(
  grid,
  record,
  state
) {
  if (state) {
    var paidamount = new BigDecimal(String(record.payment));

    if (paidamount.compareTo(new BigDecimal('0')) === 0) {
      grid.setEditValue(
        grid.getRecordIndex(record),
        'payment',
        record.outstanding
      );
      grid.setEditValue(
        grid.getRecordIndex(record),
        'difference',
        Number(new BigDecimal('0'))
      );
    }
  }
};

OB.APRM.onChangePaymentProposalPickAndEditPayment = function(
  item,
  view,
  form,
  grid
) {
  var paymentField = grid.getFieldByColumnName('Payment'),
    paidamount = new BigDecimal(
      String(grid.getEditedCell(item.rowNum, paymentField) || 0)
    ),
    outstandingField = grid.getFieldByColumnName('Outstanding'),
    outstanding = new BigDecimal(
      String(grid.getEditedCell(item.rowNum, outstandingField)) || 0
    );

  grid.setEditValue(
    item.rowNum,
    'difference',
    Number(outstanding.subtract(paidamount))
  );
  grid.setEditValue(item.rowNum, 'payment', Number(paidamount));
};

OB.APRM.validatePaymentProposalPickAndEdit = function(
  item,
  validator,
  value,
  record
) {
  if (!isc.isA.Number(record.payment)) {
    isc.warn(OB.I18N.getLabel('APRM_NotValidNumber'));
    return false;
  }

  var i,
    row,
    allRows = item.grid.data.allRows || item.grid.data.localData,
    outstanding = new BigDecimal(String(record.outstanding)),
    paidamount = new BigDecimal(String(record.payment));

  if (outstanding.abs().compareTo(paidamount.abs()) < 0) {
    isc.warn(OB.I18N.getLabel('APRM_MoreAmountThanOutstanding'));
    return false;
  }

  for (i = 0; i < allRows.size(); i++) {
    if (record.id === allRows.get(i).id) {
      row = allRows.get(i);
      break;
    }
  }
  var contextInfo = null;
  contextInfo = item.grid.view.parentWindow.activeView.getContextInfo(
    false,
    true,
    true,
    true
  );

  // When possible to capture on change event, move this code to another method
  if (row) {
    if (contextInfo.inplimitwriteoff && contextInfo.inplimitwriteoff !== '') {
      var differencewriteoff = OB.Utilities.Number.JSToOBMasked(
        row.difference * contextInfo.inpfinaccTxnConvertRate,
        OB.Format.defaultNumericMask,
        OB.Format.defaultDecimalSymbol,
        OB.Format.defaultGroupingSymbol,
        OB.Format.defaultGroupingSize
      );
      if (
        differencewriteoff > contextInfo.inplimitwriteoff &&
        record.writeoff === true
      ) {
        isc.warn(OB.I18N.getLabel('APRM_NotAllowWriteOff'));
        return false;
      }
    }
  } else {
    return false;
  }

  return true;
};

OB.APRM.addNew = function(grid) {
  var selectedRecord = grid.view.parentWindow.views[0].getParentRecord(),
    allRows = grid.data.allRows || grid.data.localData,
    totalExpected = new BigDecimal('0'),
    totalReceived = new BigDecimal('0');
  var returnObject = isc.addProperties({}, allRows[0]);
  var indRow,
    row,
    totalOutstandingInOthers = new BigDecimal('0');
  for (indRow = 0; indRow < allRows.length; indRow++) {
    row = allRows[indRow];
    totalOutstandingInOthers = totalOutstandingInOthers.add(
      new BigDecimal(String(row.outstanding))
    );
    totalExpected = totalExpected.add(new BigDecimal(String(row.expected)));
    totalReceived = totalReceived.add(new BigDecimal(String(row.received)));
  }
  returnObject.outstanding = Number(
    totalExpected.subtract(totalReceived).subtract(totalOutstandingInOthers)
  );
  returnObject.received = 0;
  returnObject.expected = 0;
  returnObject.awaitingExecutionAmount = 0;
  delete returnObject.id;
  returnObject.paymentMethod = selectedRecord.paymentMethod;
  returnObject[
    'paymentMethod' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER
  ] =
    selectedRecord[
      'paymentMethod' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER
    ];
  returnObject.currency = selectedRecord.currency;
  returnObject[
    'currency' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER
  ] =
    selectedRecord[
      'currency' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER
    ];
  returnObject.duedate = selectedRecord.invoiceDate;
  //General properties
  returnObject.organization = selectedRecord.organization;
  returnObject.client = selectedRecord.client;
  returnObject.invoice = selectedRecord.id;
  return returnObject;
};

OB.APRM.deleteRow = function(grid, rowNum, record) {
  if (
    new BigDecimal(String(record.awaitingExecutionAmount)).compareTo(
      new BigDecimal('0')
    ) !== 0
  ) {
    isc.warn(OB.I18N.getLabel('APRM_AwaitingExecutionAmountNotDeleted'));
    return false;
  }
  if (
    new BigDecimal(String(record.received)).compareTo(new BigDecimal('0')) !== 0
  ) {
    isc.warn(OB.I18N.getLabel('APRM_ReceivedAmountNotDeleted'));
    return false;
  }
  return true;
};

OB.APRM.validateDoubtfulDebtPickAndEdit = function(
  item,
  validator,
  value,
  record
) {
  if (!isc.isA.Number(record.doubtfulDebtAmount)) {
    isc.warn(OB.I18N.getLabel('APRM_NotValidNumber'));
    return false;
  }

  var outstanding = new BigDecimal(String(record.outstandingamt)),
    amount = new BigDecimal(String(record.doubtfulDebtAmount));

  if (outstanding.abs().compareTo(amount.abs()) < 0) {
    isc.warn(OB.I18N.getLabel('APRM_DoubtfulDebtMoreAmountThanOutstanding'));
    return false;
  }

  return true;
};

OB.APRM.selectDoubtfulDebtPickAndEdit = function(grid, record, state) {
  var percentage = grid.view.parentWindow.views[0].getParentRecord().percentage;
  if (state) {
    record.doubtfulDebtAmount = Number(
      new BigDecimal(String(record.outstandingamt))
        .multiply(new BigDecimal(String(percentage)))
        .divide(
          new BigDecimal('100'),
          record.currency$standardPrecision,
          BigDecimal.prototype.ROUND_HALF_UP
        )
    );
  } else {
    record.doubtfulDebtAmount = Number(new BigDecimal('0'));
  }
  return true;
};
