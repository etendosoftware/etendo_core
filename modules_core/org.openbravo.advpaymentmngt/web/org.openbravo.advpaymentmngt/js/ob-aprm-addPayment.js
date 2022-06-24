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
 * All portions are Copyright (C) 2014-2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
OB.APRM.AddPayment = {
  ordInvDataArrived: function(startRow, endRow) {
    var i,
      selectedRecords = this.getSelectedRecords();
    this.Super('dataArrived', arguments);
    for (i = 0; i < selectedRecords; i++) {
      this.setEditValues(
        this.getRecordIndex(selectedRecords[i]),
        selectedRecords[i]
      );
    }
  },
  ordInvTransformData: function(newData, dsResponse) {
    var i,
      j,
      record,
      data,
      ids,
      grid,
      editedRecord,
      isSelected,
      selectedRecord,
      curAmount,
      curOutstandingAmt,
      curPending,
      availableAmt,
      checkContainsAny;
    checkContainsAny = function(base, arrayCompare) {
      var i,
        arrayBase = base.replaceAll(' ', '').split(',');
      for (i = 0; i < arrayCompare.length; i++) {
        if (arrayBase.contains(arrayCompare[i].trim())) {
          return true;
        }
      }
      return false;
    };

    data = this.Super('transformData', arguments) || newData;
    if (this.dataSource.view.parameterName !== 'order_invoice') {
      return data;
    }
    grid = this.dataSource.view.viewGrid;
    if (grid.changedTrxType) {
      grid.selectedIds = [];
      grid.deselectedIds = [];
      grid.data.savedData = [];
    }
    for (i = 0; i < data.length; i++) {
      record = data[i];
      ids = OB.APRM.AddPayment.orderAndRemoveDuplicates(record.id);
      record.id = ids;
      record.invoiceNo = OB.APRM.AddPayment.orderAndRemoveDuplicates(
        record.invoiceNo
      );
      record.salesOrderNo = OB.APRM.AddPayment.orderAndRemoveDuplicates(
        record.salesOrderNo
      );
      if (
        grid.changedTrxType &&
        grid.editedSelectedRecords &&
        grid.editedSelectedRecords.length >= 1
      ) {
        isSelected = false;
        editedRecord = isc.addProperties({}, record);

        curAmount = isc.isA.Number(editedRecord.amount)
          ? new BigDecimal(String(editedRecord.amount))
          : BigDecimal.prototype.ZERO;
        curOutstandingAmt = isc.isA.Number(editedRecord.outstandingAmount)
          ? new BigDecimal(String(editedRecord.outstandingAmount))
          : BigDecimal.prototype.ZERO;
        for (j = 0; j < grid.editedSelectedRecords.length; j++) {
          selectedRecord = grid.editedSelectedRecords[j];

          if (checkContainsAny(ids, selectedRecord.ids)) {
            isSelected = true;
            curPending = curOutstandingAmt.subtract(curAmount);
            availableAmt = new BigDecimal(String(selectedRecord.amount));
            if (availableAmt.subtract(curPending).signum() === 1) {
              curAmount = BigDecimal.prototype.ZERO.add(curOutstandingAmt);
              selectedRecord.amount = Number(
                availableAmt.subtract(curPending).toString()
              );
            } else {
              curAmount = curAmount.add(availableAmt);
              selectedRecord.amount = 0;
            }

            if (selectedRecord.Writeoff) {
              record.Writeoff = true;
            }
            record.obSelected = true;
          }
        }
        if (isSelected) {
          record.amount = Number(curAmount.toString());
          grid.selectedIds.push(record.id);
          grid.pneSelectedRecords.push(record);
          grid.data.savedData.push(editedRecord);
        }
      }
    }
    grid.changedTrxType = false;
    return data;
  }
};

OB.APRM.AddPayment.onLoad = function(view) {
  var form = view.theForm,
    orderInvoiceGrid = form.getItem('order_invoice').canvas.viewGrid,
    glitemGrid = form.getItem('glitem').canvas.viewGrid,
    creditUseGrid = form.getItem('credit_to_use').canvas.viewGrid,
    overpaymentAction = form.getItem('overpayment_action'),
    payment = form.getItem('fin_payment_id').getValue(),
    issotrx = form.getItem('issotrx').getValue(),
    trxtype = form.getItem('trxtype') ? form.getItem('trxtype').getValue() : '',
    trxtypeParam = null,
    orgParam = null,
    bankStatementLineAmount = null,
    bankStatementLineId;
  if (
    view &&
    view.callerField &&
    view.callerField.view &&
    view.callerField.view.callerField &&
    view.callerField.view.callerField.record && //
    typeof view.callerField.view.callerField.record.affinity !== 'undefined' && //
    typeof view.callerField.view.callerField.record.matchingType !== 'undefined'
  ) {
    // If all this conditions are true it means that we are inside the 'Add Payment' process, inside the 'Add Transaction' process, inside the 'Match Statement' process
    // and in this case we need the 'bankStatementLineId'
    bankStatementLineId = view.callerField.view.callerField.record.id;
    view.theForm.addField(
      isc.OBTextItem.create({
        name: 'bankStatementLineId',
        value: bankStatementLineId
      })
    );
    view.theForm.hideItem('bankStatementLineId');
  }

  OB.APRM.AddPayment.paymentMethodMulticurrency(view, view.theForm, !payment);
  OB.APRM.AddPayment.reloadLabels(form);
  glitemGrid.fetchData();
  creditUseGrid.fetchData();
  orderInvoiceGrid.selectionChanged = OB.APRM.AddPayment.selectionChanged;
  orderInvoiceGrid.userSelectAllRecords =
    OB.APRM.AddPayment.userSelectAllRecords;
  orderInvoiceGrid.deselectAllRecords = OB.APRM.AddPayment.deselectAllRecords;
  orderInvoiceGrid.dataProperties.transformData =
    OB.APRM.AddPayment.ordInvTransformData;
  glitemGrid.removeRecordClick = OB.APRM.AddPayment.removeRecordClick;
  creditUseGrid.selectionChanged = OB.APRM.AddPayment.selectionChangedCredit;
  creditUseGrid.userSelectAllRecords = OB.APRM.AddPayment.userSelectAllRecords;
  creditUseGrid.deselectAllRecords = OB.APRM.AddPayment.deselectAllRecords;
  orderInvoiceGrid.dataArrived = OB.APRM.AddPayment.ordInvDataArrived;

  form.isCreditAllowed =
    form.getItem('received_from').getValue() !== undefined &&
    form.getItem('received_from').getValue() !== null;
  OB.APRM.AddPayment.checkSingleActionAvailable(form);
  overpaymentAction.originalValueMap = isc.addProperties(
    {},
    overpaymentAction.getValueMap()
  );
  if (issotrx) {
    form.focusInItem(form.getItem('actual_payment'));
  }
  if (trxtype === '') {
    trxtypeParam = form.getField(0);
    form.removeField(0);
    orgParam = form.getField(0);
    form.removeField(0);
    bankStatementLineAmount = form.getField(0);
    form.removeField(0);
    form.addField(trxtypeParam);
    form.addField(orgParam);
    form.addField(bankStatementLineAmount);
  }
};

OB.APRM.AddPayment.addNewGLItem = function(grid) {
  var returnObject = isc.addProperties({}, grid.data[0]);
  returnObject.paidOut = 0;
  returnObject.receivedIn = 0;
  return returnObject;
};

OB.APRM.AddPayment.paymentMethodMulticurrency = function(
  view,
  form,
  recalcConvRate
) {
  var callback,
    financialAccountId = form.getItem('fin_financial_account_id').getValue(),
    paymentMethodId = form.getItem('fin_paymentmethod_id').getValue(),
    isSOTrx = form.getItem('issotrx').getValue(),
    currencyId = form.getItem('c_currency_id').getValue(),
    paymentDate = form.getItem('payment_date').getValue(),
    orgId = form.getItem('ad_org_id').getValue(),
    trxtype = form.getItem('trxtype') ? form.getItem('trxtype').getValue() : '';

  callback = function(response, data, request) {
    var isShown = false;
    if (data.currencyId) {
      if (!form.getItem('c_currency_id').valueMap) {
        form.getItem('c_currency_id').valueMap = {};
      }
      form.getItem('c_currency_id').setValue(data.currencyId);
      form.getItem('c_currency_id').valueMap[data.currencyId] =
        data.currencyIdIdentifier;
    }
    isShown =
      data.isPayIsMulticurrency &&
      currencyId !== data.currencyToId &&
      currencyId !== undefined;
    if (data.isWrongFinancialAccount && trxtype === '') {
      form.getItem('fin_financial_account_id').setValue('');
    } else if (!data.isWrongFinancialAccount) {
      if (!form.getItem('c_currency_to_id').valueMap) {
        form.getItem('c_currency_to_id').valueMap = {};
      }
      form.getItem('c_currency_to_id').setValue(data.currencyToId);
      form.getItem('c_currency_to_id').valueMap[data.currencyToId] =
        data.currencyToIdentifier;
      if (recalcConvRate) {
        form.getItem('conversion_rate').setValue(data.conversionrate);
        form.getItem('converted_amount').setValue(data.convertedamount);
        OB.APRM.AddPayment.updateConvertedAmount(view, form, false);
      }
    }
    form.getItem('conversion_rate').visible = isShown;
    form.getItem('converted_amount').visible = isShown;
    form.getItem('c_currency_to_id').visible = isShown;
    form.redraw();
  };

  OB.RemoteCallManager.call(
    'org.openbravo.advpaymentmngt.actionHandler.PaymentMethodMulticurrencyActionHandler',
    {
      paymentMethodId: paymentMethodId,
      currencyId: currencyId,
      isSOTrx: isSOTrx,
      financialAccountId: financialAccountId,
      paymentDate: paymentDate,
      orgId: orgId
    },
    {},
    callback
  );
};

OB.APRM.AddPayment.checkSingleActionAvailable = function(form) {
  var documentAction = form.getItem('document_action');
  documentAction.fetchData(function(item, dsResponse, data, dsRequest) {
    if (dsResponse.totalRows === 1) {
      item.setValueFromRecord(data[0]);
    } else {
      item.clearValue();
    }
  });
};

OB.APRM.AddPayment.financialAccountOnChange = function(item, view, form, grid) {
  var affectedParams = [];
  OB.APRM.AddPayment.paymentMethodMulticurrency(view, form, true);
  OB.APRM.AddPayment.checkSingleActionAvailable(form);
  affectedParams.push(form.getField('c_currency_id_readonly_logic').paramId);
  OB.APRM.AddPayment.recalcDisplayLogicOrReadOnlyLogic(
    form,
    view,
    affectedParams
  );
};

OB.APRM.AddPayment.paymentDateOnChange = function(item, view, form, grid) {
  OB.APRM.AddPayment.paymentMethodMulticurrency(view, form, true);
};

OB.APRM.AddPayment.paymentMethodOnChange = function(item, view, form, grid) {
  var ordinvgrid = form.getItem('order_invoice').canvas.viewGrid,
    defaultFilter = ordinvgrid.filterEditor.getEditForm().getValues(),
    trxtype = form.getItem('trxtype') ? form.getItem('trxtype').getValue() : '',
    affectedParams = [];
  isc.addProperties(defaultFilter, {
    paymentMethodName: item.getElementValue()
  });
  OB.APRM.AddPayment.paymentMethodMulticurrency(view, form, true);
  OB.APRM.AddPayment.checkSingleActionAvailable(form);
  if (trxtype !== '') {
    ordinvgrid.setFilterEditorCriteria(defaultFilter);
    ordinvgrid.filterByEditor();
  }
  affectedParams.push(form.getField('c_currency_id_readonly_logic').paramId);
  OB.APRM.AddPayment.recalcDisplayLogicOrReadOnlyLogic(
    form,
    view,
    affectedParams
  );
};

OB.APRM.AddPayment.currencyOnChange = function(item, view, form, grid) {
  var trxtype = form.getItem('trxtype')
      ? form.getItem('trxtype').getValue()
      : '',
    ordinvgrid = form.getItem('order_invoice').canvas.viewGrid,
    newCriteria;
  if (trxtype !== '') {
    OB.APRM.AddPayment.paymentMethodMulticurrency(view, form, true);

    // fetch data after change trx type, filters should be preserved and ids of
    // the selected records should be sent
    newCriteria = ordinvgrid.addSelectedIDsToCriteria(
      ordinvgrid.getCriteria(),
      true
    );
    newCriteria.criteria = newCriteria.criteria || [];
    // add dummy criterion to force fetch
    newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
    ordinvgrid.invalidateCache();

    form.redraw();
  }
};

OB.APRM.AddPayment.transactionTypeOnChangeFunction = function(
  item,
  view,
  form,
  grid
) {
  var ordinvgrid = form.getItem('order_invoice').canvas.viewGrid,
    selectedRecords = ordinvgrid.getSelectedRecords(),
    editedSelectedRecords = [],
    editedRecord,
    i,
    newCriteria;

  if (item.getValue() === item.oldSelectedValue) {
    // only fetch new data if the selected value has changed.
    return;
  }
  item.oldSelectedValue = item.getValue();
  // Load current selection values to redistribute amounts when new data is loaded.
  for (i = 0; i < selectedRecords.length; i++) {
    editedRecord = ordinvgrid.getEditedRecord(
      ordinvgrid.getRecordIndex(selectedRecords[i])
    );
    editedRecord.ids = selectedRecords[i].id.replaceAll(' ', '').split(',');
    editedSelectedRecords.push(editedRecord);
  }
  ordinvgrid.editedSelectedRecords = editedSelectedRecords;
  ordinvgrid.changedTrxType = true;

  // fetch data after change trx type, filters should be preserved and ids of
  // the selected records should be sent
  newCriteria = ordinvgrid.addSelectedIDsToCriteria(
    ordinvgrid.getCriteria(),
    true
  );
  newCriteria.criteria = newCriteria.criteria || [];
  // add dummy criterion to force fetch
  newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
  ordinvgrid.invalidateCache();

  form.redraw();
};

OB.APRM.AddPayment.actualPaymentOnChange = function(item, view, form, grid) {
  var issotrx = form.getItem('issotrx').getValue();
  if (issotrx) {
    OB.APRM.AddPayment.distributeAmount(view, form, true);
    OB.APRM.AddPayment.updateConvertedAmount(view, form, false);
  }
};

OB.APRM.AddPayment.orderInvoiceOnLoadGrid = function(grid) {
  var issotrx = this.view.theForm.getItem('issotrx').getValue(),
    payment = this.view.theForm.getItem('fin_payment_id').getValue();
  grid.isReady = true;
  if (grid.obaprmAllRecordsSelectedByUser) {
    delete grid.obaprmAllRecordsSelectedByUser;
  }
  if ((issotrx || !payment) && grid.selectedIds.length === 0) {
    OB.APRM.AddPayment.distributeAmount(this.view, this.view.theForm, false);
  } else {
    OB.APRM.AddPayment.updateInvOrderTotal(this.view.theForm, grid);
  }
  OB.APRM.AddPayment.refreshEditedSelectedRecordsInGrid(grid);
  OB.APRM.AddPayment.tryToUpdateActualExpected(this.view.theForm);
};

OB.APRM.AddPayment.refreshEditedSelectedRecordsInGrid = function(grid) {
  var editedSelectedRecords = grid.editedSelectedRecords;
  if (editedSelectedRecords && editedSelectedRecords.length > 0) {
    editedSelectedRecords.forEach(function(record) {
      OB.APRM.AddPayment.doSelectionChanged(record, true, grid.view);
    });
  }
};

OB.APRM.AddPayment.glitemsOnLoadGrid = function(grid) {
  if (!grid.isReady) {
    // If Gl Items Grid contains records when first opened then section is uncollapsed
    if (grid.getSelectedRecords() && grid.getSelectedRecords().size() > 0) {
      grid.view.theForm
        .getItem('7B6B5F5475634E35A85CF7023165E50B')
        .expandSection();
    }
    if (grid.autoFitFieldWidths) {
      // There is a problem with the grid calculating the auto fit field width if it is drawn inside an collapsed section.
      // Also, the "_updateFieldWidths" ListGrid function cannot be overwritten.
      // With this the re-calculation is forced once the grid has been already drawn in its place, so the auto fit field width can be properly calculated.
      grid.setAutoFitFieldWidths(false);
      grid.setAutoFitFieldWidths(true);
    }
  }
  grid.isReady = true;
  OB.APRM.AddPayment.updateGLItemsTotal(this.view.theForm, 0, false);
  OB.APRM.AddPayment.tryToUpdateActualExpected(this.view.theForm);
};

OB.APRM.AddPayment.creditOnLoadGrid = function(grid) {
  grid.isReady = true;
  if (grid.obaprmAllRecordsSelectedByUser) {
    delete grid.obaprmAllRecordsSelectedByUser;
  }
  OB.APRM.AddPayment.updateCreditTotal(this.view.theForm);
  OB.APRM.AddPayment.tryToUpdateActualExpected(this.view.theForm);
};

OB.APRM.AddPayment.tryToUpdateActualExpected = function(form) {
  var orderInvoiceGrid = form.getItem('order_invoice').canvas.viewGrid,
    glitemGrid = form.getItem('glitem').canvas.viewGrid,
    creditGrid = form.getItem('credit_to_use').canvas.viewGrid;

  if (orderInvoiceGrid.isReady && glitemGrid.isReady && creditGrid.isReady) {
    OB.APRM.AddPayment.updateActualExpected(form);
  }
};

OB.APRM.AddPayment.orderInvoiceAmountOnChange = function(
  item,
  view,
  form,
  grid
) {
  OB.APRM.AddPayment.updateActualExpected(form);
  OB.APRM.AddPayment.updateInvOrderTotal(form, grid);
};

OB.APRM.AddPayment.orderInvoiceTotalAmountOnChange = function(
  item,
  view,
  form,
  grid
) {
  OB.APRM.AddPayment.updateActualExpected(form);
  OB.APRM.AddPayment.updateTotal(form);
};

OB.APRM.AddPayment.glItemTotalAmountOnChange = function(
  item,
  view,
  form,
  grid
) {
  OB.APRM.AddPayment.updateActualExpected(form);
  OB.APRM.AddPayment.updateTotal(form);
};

OB.APRM.AddPayment.distributeAmount = function(
  view,
  form,
  onActualPaymentChange
) {
  var amount = new BigDecimal(
      String(form.getItem('actual_payment').getValue() || 0)
    ),
    orderInvoice = form.getItem('order_invoice').canvas.viewGrid,
    issotrx = form.getItem('issotrx').getValue(),
    payment = form.getItem('fin_payment_id').getValue(),
    negativeamt = BigDecimal.prototype.ZERO,
    differenceamt = BigDecimal.prototype.ZERO,
    creditamt = new BigDecimal(
      String(form.getItem('used_credit').getValue() || 0)
    ),
    glitemamt = new BigDecimal(
      String(form.getItem('amount_gl_items').getValue() || 0)
    ),
    orderInvoiceData = orderInvoice.data.localData,
    total = orderInvoice.data.totalRows,
    autoDistributeAmt = OB.PropertyStore.get('APRM_AutoDistributeAmt'),
    writeoff,
    amt,
    outstandingAmount,
    i,
    showMessageProperty,
    showMessage,
    message;

  if (autoDistributeAmt !== 'N' && autoDistributeAmt !== '"N"') {
    if (orderInvoice.data.cachedRows < orderInvoice.data.totalRows) {
      showMessageProperty = OB.PropertyStore.get('APRM_ShowNoDistributeMsg');
      showMessage =
        showMessageProperty !== 'N' && showMessageProperty !== '"N"';
      if (showMessage) {
        orderInvoice.contentView.messageBar.setMessage(
          isc.OBMessageBar.TYPE_INFO,
          '<div><div class="' +
            OB.Styles.MessageBar.leftMsgContainerStyle +
            '">' +
            OB.I18N.getLabel('APRM_NoDistributeMsg') +
            '</div><div class="' +
            OB.Styles.MessageBar.rightMsgContainerStyle +
            '"><a href="#" class="' +
            OB.Styles.MessageBar.rightMsgTextStyle +
            '" onclick="' +
            "window['" +
            orderInvoice.contentView.messageBar.ID +
            "'].hide(); OB.PropertyStore.set('APRM_ShowNoDistributeMsg', 'N');\">" +
            OB.I18N.getLabel('OBUIAPP_NeverShowMessageAgain') +
            '</a></div></div>',
          ' '
        );
      }
      OB.APRM.AddPayment.updateInvOrderTotal(form, orderInvoice);
      return;
    } else {
      // hide the message bar if it is still showing the APRM_NoDistributeMsg message and the distribution is about to be done
      message = orderInvoice.contentView.messageBar.text.contents;
      if (message.contains(OB.I18N.getLabel('APRM_NoDistributeMsg'))) {
        orderInvoice.contentView.messageBar.hide();
      }
    }
    // subtract glitem amount
    amount = amount.subtract(glitemamt);
    // add credit amount
    amount = amount.add(creditamt);

    for (i = 0; i < total; i++) {
      if (
        isc.isA.Object(orderInvoiceData[i]) &&
        !isc.isA.emptyObject(orderInvoiceData[i])
      ) {
        outstandingAmount = new BigDecimal(
          String(orderInvoiceData[i].outstandingAmount)
        );
        if (outstandingAmount.signum() < 0) {
          negativeamt = negativeamt.add(
            new BigDecimal(Math.abs(outstandingAmount).toString())
          );
        }
      }
    }

    if (
      amount.compareTo(negativeamt.negate()) > 0 &&
      (onActualPaymentChange || payment)
    ) {
      amount = amount.add(negativeamt);
    }

    for (i = 0; i < total; i++) {
      if (
        !isc.isA.Object(orderInvoiceData[i]) ||
        isc.isA.emptyObject(orderInvoiceData[i])
      ) {
        continue;
      }
      writeoff = orderInvoice.getEditValues(i).writeoff;
      amt = new BigDecimal(String(orderInvoice.getEditValues(i).amount || 0));
      if (writeoff === null || writeoff === undefined) {
        writeoff = orderInvoice.getRecord(i).writeoff;
        amt = new BigDecimal(String(orderInvoice.getRecord(i).amount || 0));
      }
      if (writeoff && issotrx) {
        amount = amount.subtract(amt);
        continue;
      } else {
        outstandingAmount = new BigDecimal(
          String(orderInvoice.getRecord(i).outstandingAmount)
        );
        if (
          payment &&
          !onActualPaymentChange &&
          orderInvoice.getRecord(i).obSelected
        ) {
          outstandingAmount = new BigDecimal(
            String(orderInvoice.getRecord(i).amount)
          );
        } else if (outstandingAmount.signum() < 0 && amount.signum() < 0) {
          if (Math.abs(outstandingAmount) > Math.abs(amount)) {
            differenceamt = outstandingAmount.subtract(amount);
            outstandingAmount = amount;
            amount = amount.subtract(differenceamt);
          }
        } else if (
          outstandingAmount.signum() > -1 &&
          amount.signum() > -1 &&
          outstandingAmount.compareTo(amount) > 0
        ) {
          outstandingAmount = amount;
        }
        // do not distribute again when the selectionChanged method is invoked
        orderInvoice.preventDistributingOnSelectionChanged = true;
        if (amount.signum() === 0) {
          if (
            outstandingAmount.signum() < 0 &&
            (onActualPaymentChange || payment)
          ) {
            orderInvoice.setEditValue(
              i,
              'amount',
              Number(outstandingAmount.toString())
            );
            orderInvoice.selectRecord(i);
          } else {
            orderInvoice.deselectRecord(i);
            orderInvoice.setEditValue(i, 'amount', Number('0'));
          }
        } else if (amount.signum() === 1) {
          orderInvoice.setEditValue(
            i,
            'amount',
            Number(outstandingAmount.toString())
          );
          orderInvoice.selectRecord(i);
          if (outstandingAmount.signum() >= 0 || amount.signum() <= 0) {
            amount = amount.subtract(outstandingAmount);
          }
        } else {
          if (outstandingAmount.signum() < 0) {
            orderInvoice.setEditValue(
              i,
              'amount',
              Number(outstandingAmount.toString())
            );
            orderInvoice.selectRecord(i);
            if (outstandingAmount.signum() <= 0 || amount.signum() <= 0) {
              amount = amount.subtract(outstandingAmount);
            }
          } else {
            orderInvoice.deselectRecord(i);
            orderInvoice.setEditValue(i, 'amount', Number('0'));
          }
        }
        delete orderInvoice.preventDistributingOnSelectionChanged;
      }
    }
    OB.APRM.AddPayment.updateActualExpected(form);
  }
  OB.APRM.AddPayment.updateInvOrderTotal(form, orderInvoice);
};

OB.APRM.AddPayment.updateTotal = function(form) {
  var invOrdTotalItem = form.getItem('amount_inv_ords'),
    glItemsTotalItem = form.getItem('amount_gl_items'),
    totalItem = form.getItem('total'),
    totalAmt;

  totalAmt = new BigDecimal(String(invOrdTotalItem.getValue() || 0));
  totalAmt = totalAmt.add(
    new BigDecimal(String(glItemsTotalItem.getValue() || 0))
  );

  totalItem.setValue(Number(totalAmt.toString()));
  OB.APRM.AddPayment.updateDifference(form);
};

OB.APRM.AddPayment.updateDifference = function(form) {
  var total = new BigDecimal(String(form.getItem('total').getValue() || 0)),
    actualPayment = new BigDecimal(
      String(form.getItem('actual_payment').getValue() || 0)
    ),
    expectedPayment = new BigDecimal(
      String(form.getItem('expected_payment').getValue() || 0)
    ),
    credit = new BigDecimal(
      String(form.getItem('used_credit').getValue() || 0)
    ),
    differenceItem = form.getItem('difference'),
    expectedDifferenceItem = form.getItem('expectedDifference'),
    totalGLItems = new BigDecimal(
      String(form.getItem('amount_gl_items').getValue() || 0)
    ),
    diffAmt = actualPayment.add(credit).subtract(total),
    expectedDiffAmt = expectedPayment
      .add(credit)
      .subtract(total)
      .add(totalGLItems),
    affectedParams = [];
  differenceItem.setValue(Number(diffAmt.toString()));
  if (expectedDiffAmt.signum() === 0) {
    expectedDifferenceItem.setValue(Number(diffAmt.toString()));
  } else {
    expectedDifferenceItem.setValue(Number(expectedDiffAmt.toString()));
  }
  if (diffAmt.signum() !== 0) {
    OB.APRM.AddPayment.updateDifferenceActions(form);
  }
  affectedParams.push(
    form.getField('overpayment_action_display_logic').paramId
  );
  OB.APRM.AddPayment.recalcDisplayLogicOrReadOnlyLogic(
    form,
    form.paramWindow,
    affectedParams
  );
};

OB.APRM.AddPayment.updateDifferenceActions = function(form) {
  var issotrx = form.getItem('issotrx').getValue(),
    overpaymentAction = form.getItem('overpayment_action'),
    actualPayment = new BigDecimal(
      String(form.getItem('actual_payment').getValue() || 0)
    ),
    newValueMap = {},
    defaultValue = '',
    trxtype = form.getItem('trxtype') ? form.getItem('trxtype').getValue() : '';
  if (trxtype !== '') {
    form.isCreditAllowed =
      form.getItem('received_from').getValue() !== undefined &&
      form.getItem('received_from').getValue() !== null;
  }
  // Update difference action available values.
  if (form.isCreditAllowed) {
    newValueMap.CR = overpaymentAction.originalValueMap.CR;
    if (issotrx || actualPayment.signum() === 0) {
      // On payment outs allow refund of credit (when actual payment is zero and something is being paid).
      newValueMap.RE = overpaymentAction.originalValueMap.RE;
    } else {
      defaultValue = 'CR';
    }
  }
  overpaymentAction.setValueMap(newValueMap);
  overpaymentAction.setValue(defaultValue);
};

OB.APRM.AddPayment.updateInvOrderTotal = function(form, grid) {
  var totalAmt = BigDecimal.prototype.ZERO,
    amountField = grid.getFieldByColumnName('amount'),
    selectedRecords = grid.selectedIds,
    invOrdTotalItem = form.getItem('amount_inv_ords'),
    amt,
    i,
    bdAmt;

  for (i = 0; i < selectedRecords.length; i++) {
    amt = grid.getEditedCell(
      grid.getRecordIndex(grid.data.localData.find('id', grid.selectedIds[i])),
      amountField
    );
    bdAmt = new BigDecimal(String(amt));
    totalAmt = totalAmt.add(bdAmt);
  }
  invOrdTotalItem.setValue(Number(totalAmt.toString()));
  OB.APRM.AddPayment.updateTotal(form);
};

OB.APRM.AddPayment.selectionChanged = function(record, state) {
  var orderInvoice = this.view.theForm.getItem('order_invoice').canvas.viewGrid;
  if (!orderInvoice.preventDistributingOnSelectionChanged) {
    this.fireOnPause(
      'selectionChanged' + record.id,
      function() {
        OB.APRM.AddPayment.doSelectionChanged(record, state, this.view);
      },
      200
    );
    this.Super('selectionChanged', arguments);
  }
};

OB.APRM.AddPayment.doSelectionChanged = function(record, state, view) {
  var orderInvoice = view.theForm.getItem('order_invoice').canvas.viewGrid,
    amount = new BigDecimal(
      String(view.theForm.getItem('actual_payment').getValue() || 0)
    ),
    distributedAmount = new BigDecimal(
      String(view.theForm.getItem('amount_inv_ords').getValue() || 0)
    ),
    issotrx = view.theForm.getItem('issotrx').getValue(),
    outstandingAmount = new BigDecimal(String(record.outstandingAmount)),
    selectedIds = orderInvoice.selectedIds,
    glitem = new BigDecimal(
      String(view.theForm.getItem('amount_gl_items').getValue() || 0)
    ),
    credit = new BigDecimal(
      String(view.theForm.getItem('used_credit').getValue() || 0)
    ),
    bslamount = new BigDecimal(
      String(view.theForm.getItem('bslamount').getValue() || 0)
    ),
    i;

  amount = amount.subtract(distributedAmount);
  // subtract glitem amount
  amount = amount.subtract(glitem);
  // add credit amount
  amount = amount.add(credit);

  if (issotrx) {
    if (amount.signum() !== 0 && state) {
      if (outstandingAmount.signum() < 0 && amount.signum() < 0) {
        if (Math.abs(outstandingAmount) > Math.abs(amount)) {
          outstandingAmount = amount;
        }
      } else {
        if (outstandingAmount.compareTo(amount) > 0) {
          outstandingAmount = amount;
        }
      }
      if (amount.signum() === 0) {
        orderInvoice.setEditValue(
          orderInvoice.getRecordIndex(record),
          'amount',
          Number('0')
        );
      } else {
        orderInvoice.setEditValue(
          orderInvoice.getRecordIndex(record),
          'amount',
          Number(outstandingAmount.toString())
        );
      }
    }
  } else {
    for (i = 0; i < selectedIds.length; i++) {
      if (selectedIds[i] === record.id) {
        if (bslamount.compareTo(BigDecimal.prototype.ZERO) !== 0) {
          if (outstandingAmount.compareTo(amount) > 0) {
            orderInvoice.setEditValue(
              orderInvoice.getRecordIndex(record),
              'amount',
              Number(amount.toString())
            );
          } else {
            orderInvoice.setEditValue(
              orderInvoice.getRecordIndex(record),
              'amount',
              Number(outstandingAmount.toString())
            );
          }
        } else {
          orderInvoice.setEditValue(
            orderInvoice.getRecordIndex(record),
            'amount',
            Number(outstandingAmount.toString())
          );
        }
      }
    }
  }
  if (
    !orderInvoice.obaprmAllRecordsSelectedByUser ||
    (orderInvoice.obaprmAllRecordsSelectedByUser &&
      orderInvoice.getRecordIndex(record) === orderInvoice.getTotalRows() - 1)
  ) {
    OB.APRM.AddPayment.updateInvOrderTotal(view.theForm, orderInvoice);
    OB.APRM.AddPayment.updateActualExpected(view.theForm);
    OB.APRM.AddPayment.updateDifference(view.theForm);
    if (orderInvoice.obaprmAllRecordsSelectedByUser) {
      delete orderInvoice.obaprmAllRecordsSelectedByUser;
    }
  }
};

OB.APRM.AddPayment.userSelectAllRecords = function() {
  this.obaprmAllRecordsSelectedByUser = true;
  this.Super('userSelectAllRecords', arguments);
};

OB.APRM.AddPayment.deselectAllRecords = function() {
  this.obaprmAllRecordsSelectedByUser = true;
  this.Super('deselectAllRecords', arguments);
};

OB.APRM.AddPayment.updateActualExpected = function(form) {
  var orderInvoice = form.getItem('order_invoice').canvas.viewGrid,
    issotrx = form.getItem('issotrx').getValue(),
    totalAmountoutstanding = BigDecimal.prototype.ZERO,
    totalAmount = BigDecimal.prototype.ZERO,
    actualPayment = form.getItem('actual_payment'),
    expectedPayment = form.getItem('expected_payment'),
    generateCredit = new BigDecimal(
      String(form.getItem('generateCredit').getValue() || 0)
    ),
    glitemtotal = new BigDecimal(
      String(form.getItem('amount_gl_items').getValue() || 0)
    ),
    credit = new BigDecimal(
      String(form.getItem('used_credit').getValue() || 0)
    ),
    bslamount = new BigDecimal(
      String(form.getItem('bslamount').getValue() || 0)
    ),
    selectedRecords = orderInvoice.selectedIds,
    actpayment,
    i;
  for (i = 0; i < selectedRecords.length; i++) {
    totalAmountoutstanding = totalAmountoutstanding.add(
      new BigDecimal(
        String(
          orderInvoice.getEditedCell(
            orderInvoice.getRecordIndex(
              orderInvoice.data.localData.find(
                'id',
                orderInvoice.selectedIds[i]
              )
            ),
            orderInvoice.getFieldByColumnName('outstandingAmount')
          )
        )
      )
    );
    totalAmount = totalAmount.add(
      new BigDecimal(
        String(
          orderInvoice.getEditedCell(
            orderInvoice.getRecordIndex(
              orderInvoice.data.localData.find(
                'id',
                orderInvoice.selectedIds[i]
              )
            ),
            orderInvoice.getFieldByColumnName('amount')
          )
        )
      )
    );
  }
  if (selectedRecords.length > 0) {
    expectedPayment.setValue(Number(totalAmountoutstanding));
  } else {
    expectedPayment.setValue(Number('0'));
  }
  if (!issotrx) {
    actpayment = totalAmount.add(glitemtotal).add(generateCredit);
    actualPayment.setValue(Number(actpayment));
    if (credit.compareTo(BigDecimal.prototype.ZERO) > 0) {
      if (credit.compareTo(actpayment) > 0) {
        actualPayment.setValue(Number('0'));
      } else {
        actualPayment.setValue(Number(actpayment.subtract(credit)));
      }
    }
    if (bslamount.compareTo(BigDecimal.prototype.ZERO) !== 0) {
      var bslAmountConverted = OB.APRM.AddPayment.getConvertedAmount(
        form,
        bslamount,
        false
      );
      if (actpayment.compareTo(bslAmountConverted.abs()) <= 0) {
        actpayment = bslAmountConverted.abs();
        actualPayment.setValue(Number(actpayment));
      }
    }
    OB.APRM.AddPayment.updateDifference(form);
    OB.APRM.AddPayment.updateConvertedAmount(null, form, false);
  }

  // force redraw to ensure display logic is properly executed
  form.redraw();
};

OB.APRM.AddPayment.getConvertedAmount = function(
  form,
  amount,
  directConversion
) {
  var currencyPrecision = form.getItem('StdPrecision').getValue();
  var exchangeRate = new BigDecimal(
    String(form.getItem('conversion_rate').getValue() || 1)
  );
  if (directConversion) {
    return amount
      .multiply(exchangeRate)
      .setScale(currencyPrecision, BigDecimal.prototype.ROUND_HALF_UP);
  } else {
    return amount.divide(
      exchangeRate,
      currencyPrecision,
      BigDecimal.prototype.ROUND_HALF_UP
    );
  }
};

OB.APRM.AddPayment.removeRecordClick = function(rowNum, record) {
  this.Super('removeRecordClick', rowNum, record);

  OB.APRM.AddPayment.updateGLItemsTotal(this.view.theForm, rowNum, true);
};

OB.APRM.AddPayment.updateGLItemsTotal = function(form, rowNum, remove) {
  var totalAmt = BigDecimal.prototype.ZERO,
    grid = form.getItem('glitem').canvas.viewGrid,
    receivedInField = grid.getFieldByColumnName('received_in'),
    paidOutField = grid.getFieldByColumnName('paid_out'),
    glItemTotalItem = form.getItem('amount_gl_items'),
    issotrx = form.getItem('issotrx').getValue(),
    affectedParams = [],
    i,
    receivedInAmt,
    paidOutAmt,
    allRecords;

  grid.saveAllEdits();
  // allRecords should be initialized after grid.saveAllEdits()
  allRecords = grid.data.allRows ? grid.data.allRows.length : 0;
  for (i = 0; i < allRecords; i++) {
    if (remove && i === rowNum) {
      continue;
    }
    receivedInAmt = new BigDecimal(
      String(grid.getEditedCell(i, receivedInField) || 0)
    );
    paidOutAmt = new BigDecimal(
      String(grid.getEditedCell(i, paidOutField) || 0)
    );

    if (issotrx) {
      totalAmt = totalAmt.add(receivedInAmt);
      totalAmt = totalAmt.subtract(paidOutAmt);
    } else {
      totalAmt = totalAmt.subtract(receivedInAmt);
      totalAmt = totalAmt.add(paidOutAmt);
    }
  }
  if (allRecords === 0) {
    totalAmt = BigDecimal.prototype.ZERO;
  }

  glItemTotalItem.setValue(Number(totalAmt.toString()));
  OB.APRM.AddPayment.updateTotal(form);
  affectedParams.push(
    form.getField('overpayment_action_display_logic').paramId
  );
  OB.APRM.AddPayment.recalcDisplayLogicOrReadOnlyLogic(
    form,
    grid.view,
    affectedParams
  );
  return true;
};

OB.APRM.AddPayment.glItemAmountOnChange = function(item, view, form, grid) {
  var receivedInField = grid.getFieldByColumnName('received_in'),
    paidOutField = grid.getFieldByColumnName('paid_out'),
    receivedInAmt = new BigDecimal(
      String(grid.getEditedCell(item.rowNum, receivedInField) || 0)
    ),
    paidOutAmt = new BigDecimal(
      String(grid.getEditedCell(item.rowNum, paidOutField) || 0)
    );

  if (item.columnName === 'received_in' && receivedInAmt.signum() !== 0) {
    grid.setEditValue(item.rowNum, 'paidOut', Number('0'));
  } else if (item.columnName === 'paid_out' && paidOutAmt.signum() !== 0) {
    grid.setEditValue(item.rowNum, 'receivedIn', Number('0'));
  }

  OB.APRM.AddPayment.updateGLItemsTotal(form, item.rowNum, false);
  OB.APRM.AddPayment.updateActualExpected(form);
  OB.APRM.AddPayment.updateDifference(form);
  return true;
};

OB.APRM.AddPayment.updateCreditTotal = function(form) {
  var totalAmt = BigDecimal.prototype.ZERO,
    grid = form.getItem('credit_to_use').canvas.viewGrid,
    amountField = grid.getFieldByColumnName('paymentAmount'),
    selectedRecords = grid.getSelectedRecords(),
    creditTotalItem = form.getItem('used_credit'),
    i,
    creditAmt;

  for (i = 0; i < selectedRecords.length; i++) {
    creditAmt = new BigDecimal(
      String(
        grid.getEditedCell(grid.getRecordIndex(selectedRecords[i]), amountField)
      )
    );
    totalAmt = totalAmt.add(creditAmt);
  }
  creditTotalItem.setValue(Number(totalAmt.toString()));
  OB.APRM.AddPayment.updateTotal(form);
  return true;
};

OB.APRM.AddPayment.updateCreditOnChange = function(item, view, form, grid) {
  var issotrx = form.getItem('issotrx').getValue();

  OB.APRM.AddPayment.updateCreditTotal(form);
  if (issotrx) {
    OB.APRM.AddPayment.distributeAmount(view, form, true);
  }
  OB.APRM.AddPayment.updateDifference(form);
  OB.APRM.AddPayment.updateActualExpected(form);
  return true;
};

OB.APRM.AddPayment.selectionChangedCredit = function(record, state) {
  var creditgrid = this.view.theForm.getItem('credit_to_use').canvas.viewGrid;

  if (!creditgrid.preventDistributingOnSelectionChanged) {
    this.fireOnPause(
      'selectionChangedCredit' + record.id,
      function() {
        OB.APRM.AddPayment.doSelectionChangedCredit(record, state, this.view);
      },
      200
    );
    this.Super('selectionChanged', arguments);
  }
};

OB.APRM.AddPayment.orderInvoiceGridValidation = function(
  item,
  validator,
  value,
  record
) {
  var outstanding = new BigDecimal(String(record.outstandingAmount)),
    paidamount = new BigDecimal(String(record.amount));

  if (!isc.isA.Number(record.amount)) {
    item.grid.view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_ERROR,
      null,
      OB.I18N.getLabel('APRM_NotValidNumber')
    );
    return false;
  }
  if (outstanding.abs().compareTo(paidamount.abs()) < 0) {
    item.grid.view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_ERROR,
      null,
      OB.I18N.getLabel('APRM_MoreAmountThanOutstanding')
    );
    return false;
  }
  if (paidamount.signum() === 0 && record.writeoff === false) {
    item.grid.view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_ERROR,
      null,
      OB.I18N.getLabel('APRM_JSZEROUNDERPAYMENT')
    );
    return false;
  }
  if (
    (paidamount.signum() < 0 && outstanding.signum() > 0) ||
    (paidamount.signum() > 0 && outstanding.signum() < 0)
  ) {
    item.grid.view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_ERROR,
      null,
      OB.I18N.getLabel('APRM_ValueOutOfRange')
    );
    return false;
  }
  return true;
};

OB.APRM.AddPayment.creditValidation = function(item, validator, value, record) {
  var outstanding = new BigDecimal(String(record.outstandingAmount)),
    paidamount = new BigDecimal(String(record.paymentAmount));

  if (!isc.isA.Number(record.paymentAmount)) {
    item.grid.view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_ERROR,
      null,
      OB.I18N.getLabel('APRM_NotValidNumber')
    );
    return false;
  }
  if (outstanding.abs().compareTo(paidamount.abs()) < 0) {
    item.grid.view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_ERROR,
      null,
      OB.I18N.getLabel('APRM_MoreAmountThanOutstanding')
    );
    return false;
  }
  if (paidamount.signum() === 0) {
    item.grid.view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_ERROR,
      null,
      OB.I18N.getLabel('aprm_biggerthanzero')
    );
    return false;
  }
  return true;
};

OB.APRM.AddPayment.doSelectionChangedCredit = function(record, state, view) {
  var issotrx = view.theForm.getItem('issotrx'),
    grid = view.theForm.getItem('credit_to_use').canvas.viewGrid,
    amountField = grid.getFieldByColumnName('paymentAmount'),
    outstanding = new BigDecimal(String(record.outstandingAmount));

  if (state) {
    grid.setEditValue(
      grid.getRecordIndex(record),
      amountField,
      Number(outstanding)
    );
  } else {
    grid.setEditValue(grid.getRecordIndex(record), amountField, '0');
  }
  if (
    !grid.obaprmAllRecordsSelectedByUser ||
    (grid.obaprmAllRecordsSelectedByUser &&
      grid.getRecordIndex(record) === grid.getTotalRows() - 1)
  ) {
    OB.APRM.AddPayment.updateCreditTotal(view.theForm);
    OB.APRM.AddPayment.updateActualExpected(view.theForm);
    if (issotrx) {
      OB.APRM.AddPayment.distributeAmount(view, view.theForm, true);
    }
  }
};

OB.APRM.AddPayment.conversionRateOnChange = function(item, view, form, grid) {
  OB.APRM.AddPayment.updateConvertedAmount(view, form, false);
};

OB.APRM.AddPayment.convertedAmountOnChange = function(item, view, form, grid) {
  OB.APRM.AddPayment.updateConvertedAmount(view, form, true);
};

OB.APRM.AddPayment.updateConvertedAmount = function(
  view,
  form,
  recalcExchangeRate
) {
  var actualPayment = new BigDecimal(
      String(form.getItem('actual_payment').getValue() || 0)
    ),
    actualConvertedItem = form.getItem('converted_amount'),
    exchangeRateItem = form.getItem('conversion_rate'),
    newConvertedAmount = BigDecimal.prototype.ZERO,
    newExchangeRate = BigDecimal.prototype.ONE;

  var exchangeRate = new BigDecimal(String(exchangeRateItem.getValue() || 1));
  var actualConverted = new BigDecimal(
    String(actualConvertedItem.getValue() || 0)
  );
  if (!actualConverted || !exchangeRate) {
    return;
  }
  if (recalcExchangeRate) {
    if (actualConverted && actualPayment) {
      if (actualPayment.compareTo(newConvertedAmount) !== 0) {
        newExchangeRate = actualConverted.divide(actualPayment, 15, 2);
        exchangeRateItem.setValue(Number(newExchangeRate.toString()));
      }
    } else {
      exchangeRateItem.setValue(Number(newExchangeRate.toString()));
    }
  } else if (exchangeRate) {
    newConvertedAmount = OB.APRM.AddPayment.getConvertedAmount(
      form,
      actualPayment,
      true
    );
    exchangeRateItem.setValue(Number(exchangeRate.toString()));
    actualConvertedItem.setValue(Number(newConvertedAmount.toString()));
  } else {
    actualConvertedItem.setValue(Number(actualConverted.toString()));
  }
};

/*
 * Retrieves a string of comma separated values and returns it ordered and with the duplicates removed.
 */
OB.APRM.AddPayment.orderAndRemoveDuplicates = function(val) {
  var valArray = val
      .replaceAll(' ', '')
      .split(',')
      .sort(),
    retVal;

  valArray = valArray.filter(function(elem, pos, self) {
    return self.indexOf(elem) === pos;
  });

  retVal = valArray.toString().replaceAll(',', ', ');
  return retVal;
};

OB.APRM.AddPayment.documentOnChange = function(item, view, form, grid) {
  var document = form.getItem('trxtype')
      ? form.getItem('trxtype').getValue()
      : '',
    issotrx = form.getItem('issotrx'),
    affectedParams = [],
    ordinvgrid = form.getItem('order_invoice').canvas.viewGrid,
    organization = form.getItem('ad_org_id'),
    newCriteria,
    callback;
  if (document === 'RCIN') {
    issotrx.setValue(true);
  } else {
    issotrx.setValue(false);
  }

  form.getItem('fin_paymentmethod_id').setValue(null);
  form.getItem('received_from').setValue(null);
  if (!form.paramWindow.parentWindow) {
    form.getItem('fin_financial_account_id').setValue(null);
  }
  OB.APRM.AddPayment.reloadLabels(form);
  affectedParams.push(form.getField('credit_to_use_display_logic').paramId);
  affectedParams.push(form.getField('actual_payment_readonly_logic').paramId);
  OB.APRM.AddPayment.recalcDisplayLogicOrReadOnlyLogic(
    form,
    view,
    affectedParams
  );

  if (document !== '') {
    // fetch data after change trx type, filters should be preserved and ids of
    // the selected records should be sent
    newCriteria = ordinvgrid.addSelectedIDsToCriteria(
      ordinvgrid.getCriteria(),
      true
    );
    newCriteria.criteria = newCriteria.criteria || [];
    // add dummy criterion to force fetch
    newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
    ordinvgrid.invalidateCache();
    form.redraw();
  }

  callback = function(response, data, request) {
    form.getItem('payment_documentno').setValue(data.payment_documentno);
  };

  if (document !== '') {
    OB.RemoteCallManager.call(
      'org.openbravo.advpaymentmngt.actionHandler.AddPaymentDocumentNoActionHandler',
      {
        organization: organization.getValue(),
        issotrx: issotrx.getValue()
      },
      {},
      callback
    );
  }
};

OB.APRM.AddPayment.organizationOnChange = function(item, view, form, grid) {
  var ordinvgrid = form.getItem('order_invoice').canvas.viewGrid,
    organization = form.getItem('ad_org_id')
      ? form.getItem('ad_org_id').getValue()
      : '',
    newCriteria,
    callback;
  form.getItem('fin_paymentmethod_id').setValue(null);
  form.getItem('received_from').setValue(null);
  form.getItem('fin_financial_account_id').setValue(null);
  callback = function(response, data, request) {
    form.getItem('c_currency_id').setValue(data.currency);
    form.getItem('c_currency_id').valueMap[data.currency] =
      data.currencyIdIdentifier;
    // fetch data after change organization, filters should be preserved and ids of
    // the selected records should be sent
    newCriteria = ordinvgrid.addSelectedIDsToCriteria(
      ordinvgrid.getCriteria(),
      true
    );
    newCriteria.criteria = newCriteria.criteria || [];
    // add dummy criterion to force fetch
    newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
    ordinvgrid.invalidateCache();
    form.redraw();
  };

  if (organization !== '') {
    OB.RemoteCallManager.call(
      'org.openbravo.advpaymentmngt.actionHandler.AddPaymentOrganizationActionHandler',
      {
        organization: organization
      },
      {},
      callback
    );
  }
};

OB.APRM.AddPayment.receivedFromOnChange = function(item, view, form, grid) {
  var affectedParams = [],
    trxtype = form.getItem('trxtype') ? form.getItem('trxtype').getValue() : '',
    callback,
    receivedFrom = form.getItem('received_from').getValue(),
    isSOTrx = form.getItem('issotrx').getValue(),
    financialAccount = form.getItem('fin_financial_account_id').getValue(),
    ordinvgrid = form.getItem('order_invoice').canvas.viewGrid,
    paymentMethodItem = form.getItem('fin_paymentmethod_id'),
    newCriteria = {};
  affectedParams.push(form.getField('credit_to_use_display_logic').paramId);
  OB.APRM.AddPayment.recalcDisplayLogicOrReadOnlyLogic(
    form,
    view,
    affectedParams
  );

  callback = function(response, data, request) {
    if (data.paymentMethodId !== '') {
      paymentMethodItem.setValue(data.paymentMethodId);
      paymentMethodItem.valueMap[data.paymentMethodId] = data.paymentMethodName;
      form.redraw();
      OB.APRM.AddPayment.paymentMethodOnChange(
        paymentMethodItem,
        view,
        form,
        grid
      );
    }
  };

  if (trxtype !== '') {
    OB.RemoteCallManager.call(
      'org.openbravo.advpaymentmngt.actionHandler.ReceivedFromPaymentMethodActionHandler',
      {
        receivedFrom: receivedFrom,
        isSOTrx: isSOTrx,
        financialAccount: financialAccount
      },
      {},
      callback
    );
    newCriteria = ordinvgrid.addSelectedIDsToCriteria(
      ordinvgrid.getCriteria(),
      true
    );
    newCriteria.criteria = newCriteria.criteria || [];
    // add dummy criterion to force fetch
    newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
    ordinvgrid.invalidateCache();

    form.redraw();
  }
};

OB.APRM.AddPayment.recalcDisplayLogicOrReadOnlyLogic = function(
  form,
  view,
  affectedParams
) {
  var callbackDisplayLogicActionHandler,
    params = {},
    thisform,
    thisview,
    creditUseGrid = form.getItem('credit_to_use').canvas.viewGrid;
  thisform = form;
  thisview = view;
  params.context = form.paramWindow.getContextInfo();
  // Before sending the context, the grids with the information about orders and invoices,
  // gl items and credit used are removed from it.
  // This data is not used for calculating the display or read only logic of the rest of the parameters
  // and sending the grid can have an impact in the performance of this process.
  delete params.context.order_invoice;
  delete params.context.credit_to_use;
  delete params.context.glitem;
  if (form.paramWindow.parentWindow && form.paramWindow.parentWindow.windowId) {
    params.context.inpwindowId = form.paramWindow.parentWindow.windowId;
  }

  callbackDisplayLogicActionHandler = function(response, data, request) {
    var i,
      field,
      def,
      values = data.values,
      newCriteria = {};

    for (i in values) {
      if (Object.prototype.hasOwnProperty.call(values, i)) {
        def = values[i];
        field = thisform.getItem(i);
        if (field) {
          if (isc.isA.Object(def)) {
            if (def.identifier && def.value) {
              field.valueMap = field.valueMap || {};
              field.valueMap[def.value] = def.identifier;
              field.setValue(def.value);
            }
          } else {
            field.setValue(
              thisform.paramWindow.getTypeSafeValue(field.typeInstance, def)
            );
          }
        }
      }
    }
    if (thisview) {
      thisview.handleReadOnlyLogic();
    }
    // If credit grid is now displayed fetch data
    if (
      values.credit_to_use_display_logic &&
      values.credit_to_use_display_logic === 'Y'
    ) {
      newCriteria.criteria = [];
      // add dummy criterion to force fetch
      newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
      creditUseGrid.fetchData(newCriteria);
    }
    thisform.redraw();
    if (thisview) {
      thisview.handleButtonsStatus();
    }
  };

  thisview.fireOnPause(
    'recalcDisplayLogicOrReadOnlyLogic' + affectedParams,
    function() {
      OB.RemoteCallManager.call(
        'org.openbravo.advpaymentmngt.actionHandler.AddPaymentDisplayLogicActionHandler',
        {
          affectedParams: affectedParams,
          params: params
        },
        {},
        callbackDisplayLogicActionHandler
      );
    },
    200
  );
};

OB.APRM.AddPayment.reloadLabels = function(form) {
  var callbackReloadLabelsActionHandler,
    params = {};
  params.businessPartner = form.getItem('received_from').paramId;
  params.financialAccount = form.getItem('fin_financial_account_id').paramId;
  params.issotrx = form.getItem('issotrx').getValue();

  callbackReloadLabelsActionHandler = function(response, data, request) {
    form.getItem('received_from').title = data.values.businessPartner;
    form.getItem('fin_financial_account_id').title =
      data.values.financialAccount;
    form.markForRedraw();
  };

  OB.RemoteCallManager.call(
    'org.openbravo.advpaymentmngt.actionHandler.AddPaymentReloadLabelsActionHandler',
    {},
    params,
    callbackReloadLabelsActionHandler
  );
};

OB.APRM.AddPayment.onProcess = function(
  view,
  actionHandlerCall,
  clientSideValidationFail
) {
  var orderInvoiceGrid = view.theForm.getItem('order_invoice').canvas.viewGrid,
    receivedFrom = view.theForm.getItem('received_from').getValue(),
    currencyId = view.theForm.getItem('c_currency_id').getValue(),
    issotrx = view.theForm.getItem('issotrx').getValue(),
    finFinancialAccount = view.theForm
      .getItem('fin_financial_account_id')
      .getValue(),
    amountInvOrds = new BigDecimal(
      String(view.theForm.getItem('amount_inv_ords').getValue() || 0)
    ),
    total = new BigDecimal(
      String(view.theForm.getItem('total').getValue() || 0)
    ),
    actualPayment = new BigDecimal(
      String(view.theForm.getItem('actual_payment').getValue() || 0)
    ),
    overpaymentField = view.theForm.getItem('overpayment_action'),
    overpaymentAction = overpaymentField.getValue(),
    creditTotalItem = new BigDecimal(
      String(view.theForm.getItem('used_credit').getValue() || 0)
    ),
    document = view.theForm.getItem('trxtype')
      ? view.theForm.getItem('trxtype').getValue()
      : '',
    selectedRecords = orderInvoiceGrid.getSelectedRecords(),
    writeOffLimitPreference = OB.PropertyStore.get(
      'WriteOffLimitPreference',
      view.windowId
    ),
    totalWriteOffAmount = BigDecimal.prototype.ZERO,
    writeOffLineAmount = BigDecimal.prototype.ZERO,
    totalOustandingAmount = BigDecimal.prototype.ZERO,
    glitemGrid = view.theForm.getItem('glitem').canvas.viewGrid,
    outstandingAmount,
    i,
    callbackOnProcessActionHandler,
    writeoff,
    allRecords,
    receivedInAmt,
    paidOutAmt,
    receivedInField,
    paidOutField;

  // Check if there is pending amount to distribute that could be distributed
  for (i = 0; i < selectedRecords.length; i++) {
    outstandingAmount = new BigDecimal(
      String(selectedRecords[i].outstandingAmount)
    );
    totalOustandingAmount = totalOustandingAmount.add(outstandingAmount);
  }
  for (i = 0; i < orderInvoiceGrid.data.totalRows; i++) {
    writeoff = orderInvoiceGrid.getEditValues(i).writeoff;
    if (writeoff === null || writeoff === undefined) {
      writeoff = orderInvoiceGrid.getRecord(i).writeoff;
    }
    if (writeoff) {
      writeOffLineAmount = new BigDecimal(
        String(orderInvoiceGrid.getRecord(i).outstandingAmount || 0)
      ).subtract(
        new BigDecimal(String(orderInvoiceGrid.getEditedRecord(i).amount || 0))
      );
      totalWriteOffAmount = totalWriteOffAmount.add(writeOffLineAmount);
    }
  }

  // If there is Overpayment check it exists a business partner
  if (overpaymentAction && receivedFrom === null) {
    view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_ERROR,
      null,
      OB.I18N.getLabel('APRM_CreditWithoutBPartner')
    );
    return clientSideValidationFail();
  }
  //If Actual Payment amount is negative, it is not necessary to use credit.
  if (
    total.compareTo(BigDecimal.prototype.ZERO) < 0 &&
    creditTotalItem.signum() !== 0
  ) {
    view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_ERROR,
      null,
      OB.I18N.getLabel('APRM_CreditWithNegativeAmt')
    );
    return clientSideValidationFail();
  }
  if (
    actualPayment.compareTo(total.subtract(creditTotalItem)) > 0 &&
    totalOustandingAmount.compareTo(amountInvOrds.add(totalWriteOffAmount)) > 0
  ) {
    // Not all the payment amount has been allocated
    view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_ERROR,
      null,
      OB.I18N.getLabel('APRM_JSNOTALLAMOUTALLOCATED')
    );
    return clientSideValidationFail();
  } else if (total.compareTo(actualPayment.add(creditTotalItem)) > 0) {
    // More than available amount has been distributed
    view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_ERROR,
      null,
      OB.I18N.getLabel('APRM_JSMOREAMOUTALLOCATED')
    );
    return clientSideValidationFail();
  }

  if (
    creditTotalItem.compareTo(BigDecimal.prototype.ZERO) !== 0 &&
    total.compareTo(creditTotalItem) < 0 &&
    (overpaymentField.isVisible() && overpaymentAction === 'CR')
  ) {
    view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_ERROR,
      null,
      OB.I18N.getLabel('APRM_MORECREDITAMOUNT')
    );
    return clientSideValidationFail();
  }

  if (
    document !== null &&
    document !== '' &&
    actualPayment.compareTo(BigDecimal.prototype.ZERO) === 0 &&
    view.parentWindow &&
    view.parentWindow.windowId &&
    !overpaymentAction
  ) {
    view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_ERROR,
      null,
      OB.I18N.getLabel('APRM_ZEROAMOUNTPAYMENTTRANSACTION')
    );
    return clientSideValidationFail();
  }

  //It is not possible to add a glitem with both amounts equal to 0
  allRecords = glitemGrid.data.allRows ? glitemGrid.data.allRows.length : 0;
  for (i = 0; i < allRecords; i++) {
    receivedInField = glitemGrid.getFieldByColumnName('received_in');
    paidOutField = glitemGrid.getFieldByColumnName('paid_out');
    receivedInAmt = new BigDecimal(
      String(glitemGrid.getEditedCell(i, receivedInField) || 0)
    );
    paidOutAmt = new BigDecimal(
      String(glitemGrid.getEditedCell(i, paidOutField) || 0)
    );
    if (receivedInAmt.signum() === 0 && paidOutAmt.signum() === 0) {
      view.messageBar.setMessage(
        isc.OBMessageBar.TYPE_ERROR,
        null,
        OB.I18N.getLabel('APRM_GLITEMSDIFFERENTZERO')
      );
      return clientSideValidationFail();
    }
  }
  callbackOnProcessActionHandler = function(response, data, request) {
    //Check if there are blocked Business Partners
    if (data.message.severity === 'error') {
      view.messageBar.setMessage(
        isc.OBMessageBar.TYPE_ERROR,
        data.message.title,
        data.message.text
      );
      return clientSideValidationFail();
    }
    // Check if the write off limit has been exceeded
    if (writeOffLimitPreference === 'Y') {
      if (totalWriteOffAmount > data.writeofflimit) {
        view.messageBar.setMessage(
          isc.OBMessageBar.TYPE_ERROR,
          null,
          OB.I18N.getLabel('APRM_NotAllowWriteOff')
        );
        return clientSideValidationFail();
      }
    }
    actionHandlerCall();
  };

  OB.RemoteCallManager.call(
    'org.openbravo.advpaymentmngt.actionHandler.AddPaymentOnProcessActionHandler',
    {
      issotrx: issotrx,
      receivedFrom: receivedFrom,
      currencyId: currencyId,
      usesCredit: creditTotalItem.compareTo(BigDecimal.prototype.ZERO) !== 0,
      generatesCredit:
        overpaymentField.isVisible() && overpaymentAction === 'CR',
      selectedRecords: selectedRecords,
      finFinancialAccount: finFinancialAccount
    },
    {},
    callbackOnProcessActionHandler
  );
};
