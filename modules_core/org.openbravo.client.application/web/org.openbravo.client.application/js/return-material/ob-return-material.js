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
 * All portions are Copyright (C) 2011-2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB.RM = OB.RM || {};

/**
 * Check that entered return quantity is less than original inout qty.
 */

OB.RM.RMReturnedUOMValidate = function(item, validator, value, record) {
  var movementQty = isc.isA.Number(record.movementQuantity)
      ? new BigDecimal(String(record.movementQuantity))
      : BigDecimal.prototype.ZERO,
    returnedQty = isc.isA.Number(record.returnQtyOtherRM)
      ? new BigDecimal(String(record.returnQtyOtherRM))
      : BigDecimal.prototype.ZERO,
    newReturnedQty = isc.isA.Number(record.returned)
      ? new BigDecimal(String(record.returned))
      : BigDecimal.prototype.ZERO;

  var applyUOM =
    OB.PropertyStore.get('UomManagement') !== null &&
    OB.PropertyStore.get('UomManagement') === 'Y' &&
    record.returnedUOM !== record.uOM;

  if (applyUOM) {
    newReturnedQty = newReturnedQty.multiply(
      isc.isA.Number(record.aumConversionRate)
        ? new BigDecimal(String(record.aumConversionRate))
        : BigDecimal.prototype.ONE
    );
  }
  if (
    newReturnedQty.compareTo(movementQty.subtract(returnedQty)) <= 0 &&
    record.returned > 0
  ) {
    item.grid.view.messageBar.hide(true);
    return true;
  } else {
    if (applyUOM) {
      item.grid.view.messageBar.setMessage(
        isc.OBMessageBar.TYPE_ERROR,
        null,
        OB.I18N.getLabel('OBUIAPP_RM_OutOfRange_AUM', [
          newReturnedQty.toString(),
          record.uOM$_identifier,
          movementQty.subtract(returnedQty).toString()
        ])
      );
    } else {
      item.grid.view.messageBar.setMessage(
        isc.OBMessageBar.TYPE_ERROR,
        null,
        OB.I18N.getLabel('OBUIAPP_RM_OutOfRange', [
          movementQty.subtract(returnedQty).toString()
        ])
      );
    }
    return false;
  }
};

OB.RM.RMOrderQtyValidate = function(item, validator, value, record) {
  if (!isc.isA.Number(value)) {
    return false;
  }
  // Check if record has related shipment to skip check.
  if (record.goodsShipmentLine === null || record.goodsShipmentLine === '') {
    return value !== null && value > 0;
  }
  var movementQty =
      record.movementQuantity !== null
        ? new BigDecimal(String(record.movementQuantity))
        : BigDecimal.prototype.ZERO,
    returnedQty =
      record.returnQtyOtherRM !== null
        ? new BigDecimal(String(record.returnQtyOtherRM))
        : BigDecimal.prototype.ZERO,
    newReturnedQty = new BigDecimal(String(value));

  var applyUOM =
    OB.PropertyStore.get('UomManagement') !== null &&
    OB.PropertyStore.get('UomManagement') === 'Y' &&
    record.returnedUOM !== record.uOM;

  if (applyUOM) {
    newReturnedQty = newReturnedQty.multiply(
      new BigDecimal(String(record.aumConversionRate))
    );
  }
  if (
    value !== null &&
    newReturnedQty.compareTo(movementQty.subtract(returnedQty)) <= 0 &&
    value > 0
  ) {
    item.grid.view.messageBar.hide(true);
    return true;
  } else {
    if (applyUOM) {
      item.grid.view.messageBar.setMessage(
        isc.OBMessageBar.TYPE_ERROR,
        null,
        OB.I18N.getLabel('OBUIAPP_RM_OutOfRange_AUM', [
          newReturnedQty.toString(),
          record.uOM$_identifier,
          movementQty.subtract(returnedQty).toString()
        ])
      );
    } else {
      item.grid.view.messageBar.setMessage(
        isc.OBMessageBar.TYPE_ERROR,
        null,
        OB.I18N.getLabel('OBUIAPP_RM_OutOfRange', [
          movementQty.subtract(returnedQty).toString()
        ])
      );
    }
    return false;
  }
};

/**
 * Set quantity, storage bin and condition of the goods.
 */
OB.RM.RMOrderSelectionChange = function(grid, record, state) {
  var contextInfo = null;
  if (state) {
    contextInfo = grid.view.parentWindow.activeView.getContextInfo(
      false,
      true,
      true,
      true
    );
    if (!contextInfo.inpdateordered) {
      contextInfo = grid.view.parentWindow.activeView.parentView.getContextInfo(
        false,
        true,
        true,
        true
      );
    }
    if (!record.returnReason) {
      record.returnReason = contextInfo.inpcReturnReasonId;
    }
    OB.RemoteCallManager.call(
      'org.openbravo.common.actionhandler.RFCServiceReturnableActionHandler',
      {
        rfcOrderDate: contextInfo.inpdateordered,
        goodsShipmentId: record.id,
        productId: record.product
      },
      {},
      function(response, data, request) {
        if (data.message) {
          if (data.message.severity === isc.OBMessageBar.TYPE_ERROR) {
            grid.view.messageBar.setMessage(
              isc.OBMessageBar.TYPE_ERROR,
              data.message.title,
              data.message.text
            );
            grid.deselectRecord(record);
          } else {
            grid.view.messageBar.setMessage(
              isc.OBMessageBar.TYPE_WARNING,
              data.message.title,
              data.message.text
            );
          }
        }
      }
    );
  }
};
/**
 * Check that entered received quantity is less than pending qty.
 */
OB.RM.RMReceiptQtyValidate = function(item, validator, value, record) {
  if (value !== null && value <= record.pending && value > 0) {
    return true;
  } else {
    item.grid.view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_ERROR,
      null,
      OB.I18N.getLabel('OBUIAPP_RM_ReceivingMoreThanPending', [record.pending])
    );
    return false;
  }
};

/**
 * Set quantity, storage bin and condition of the goods.
 */
OB.RM.RMReceiptSelectionChange = function(grid, record, state) {
  var contextInfo = null;
  if (state) {
    record.receiving = record.pending;
    contextInfo = grid.view.parentWindow.activeView.getContextInfo(
      false,
      true,
      true,
      true
    );
    record.storageBin = contextInfo.ReturnLocator;
    if (!record.conditionGoods) {
      record.conditionGoods = contextInfo.inpmConditionGoodsId;
    }
  }
};

/**
 * Check that entered shipped quantity is less than pending qty.
 */
OB.RM.RMShipmentQtyValidate = function(item, validator, value, record) {
  var orderLine = record.orderLine,
    pendingQty = record.pending,
    selectedRecords = item.grid.getSelectedRecords(),
    selectedRecordsLength = selectedRecords.length,
    editedRecord = null,
    storageBin = record.storageBin,
    i;
  //Checking available stock
  if (storageBin === null && !record.hasOverIssueBin) {
    item.grid.view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_ERROR,
      null,
      OB.I18N.getLabel('OBUIAPP_RM_NotAvailableStock', [record.rMOrderNo])
    );
    return false;
  }
  if (storageBin !== null && !record.hasOverIssueBin) {
    // check value is positive and below available qty and pending qty.
    // This check it is only needed if there isn't any storage bin with Overissue inventory status in the RTVS Warehouse
    if (
      value === null ||
      value < 0 ||
      value > record.pending ||
      value > record.availableQty
    ) {
      if (record.pending < record.availableQty) {
        item.grid.view.messageBar.setMessage(
          isc.OBMessageBar.TYPE_ERROR,
          null,
          OB.I18N.getLabel('OBUIAPP_RM_MoreThanPending', [record.pending])
        );
      } else {
        item.grid.view.messageBar.setMessage(
          isc.OBMessageBar.TYPE_ERROR,
          null,
          OB.I18N.getLabel('OBUIAPP_RM_MoreThanAvailable', [
            record.availableQty
          ])
        );
      }
      return false;
    }
    // check shipped total quantity for the order line is below pending qty.
    var isUomManagementEnabled = OB.PropertyStore.get('UomManagement');
    for (i = 0; i < selectedRecordsLength; i++) {
      editedRecord = isc.addProperties(
        {},
        selectedRecords[i],
        item.grid.getEditedRecord(selectedRecords[i])
      );
      if (editedRecord.orderLine === orderLine) {
        if (isUomManagementEnabled === 'Y') {
          if (record.returnedUOM === editedRecord.returnedUOM) {
            pendingQty -= editedRecord.movementQuantity;
          } else {
            var movementQuantity = new BigDecimal(
              String(editedRecord.movementQuantity)
            );
            var rate = new BigDecimal(String(editedRecord.rate));
            pendingQty -=
              editedRecord.returnedUOM !== editedRecord.uOM
                ? movementQuantity.multiply(rate).toString()
                : movementQuantity.divide(rate).toString();
          }
        }
        if (pendingQty < 0) {
          if (isUomManagementEnabled === 'Y') {
            item.grid.view.messageBar.setMessage(
              isc.OBMessageBar.TYPE_ERROR,
              null,
              OB.I18N.getLabel('OBUIAPP_RM_TooMuchShippedInUomManagement', [])
            );
          } else {
            item.grid.view.messageBar.setMessage(
              isc.OBMessageBar.TYPE_ERROR,
              null,
              OB.I18N.getLabel('OBUIAPP_RM_TooMuchShipped', [record.pending])
            );
          }
          return false;
        }
      }
    }
  }
  item.grid.view.messageBar.hide();
  return true;
};

/**
 * Set quantity
 */
OB.RM.RMShipmentSelectionChange = function(grid, record, state) {
  var orderLine = record.orderLine,
    shippedQty = BigDecimal.prototype.ZERO,
    selectedRecords = grid.getSelectedRecords(),
    pending = new BigDecimal(String(record.pending)),
    availableQty = new BigDecimal(String(record.availableQty)),
    storageBin = record.storageBin,
    editedRecord = null,
    isstocked = record.stocked,
    i;
  //calculate already shipped qty on grid
  var calculateAlreadyShippedQtyOnGrid = function() {
    var isUomManagementEnabled = OB.PropertyStore.get('UomManagement');
    for (i = 0; i < selectedRecords.length; i++) {
      editedRecord = isc.addProperties(
        {},
        selectedRecords[i],
        grid.getEditedRecord(selectedRecords[i])
      );
      if (
        editedRecord.orderLine === orderLine &&
        selectedRecords[i].id !== record.id
      ) {
        if (isUomManagementEnabled === 'Y') {
          if (record.returnedUOM === editedRecord.returnedUOM) {
            shippedQty = shippedQty.add(
              new BigDecimal(String(editedRecord.movementQuantity))
            );
          } else {
            var movementQuantity = new BigDecimal(
              String(editedRecord.movementQuantity)
            );
            var rate = new BigDecimal(String(editedRecord.rate));
            shippedQty =
              editedRecord.returnedUOM !== editedRecord.uOM
                ? shippedQty.add(movementQuantity.multiply(rate))
                : shippedQty.add(movementQuantity.divide(rate));
          }
        } else {
          shippedQty = shippedQty.add(
            new BigDecimal(String(editedRecord.movementQuantity))
          );
        }
      }
    }
    pending = pending.subtract(shippedQty);
    if (pending.compareTo(availableQty) < 0) {
      record.movementQuantity = pending.toString();
    } else {
      record.movementQuantity = availableQty.toString();
    }
  };
  if (state) {
    // Checking available stock
    if (storageBin === null && isstocked) {
      // Check if exists any storage bin with overissue inventory status
      var callback = function(response, data, request) {
        if (data.overissueBin === '') {
          grid.view.messageBar.setMessage(
            isc.OBMessageBar.TYPE_ERROR,
            null,
            OB.I18N.getLabel('OBUIAPP_RM_NotAvailableStock', [record.rMOrderNo])
          );
          record.hasOverIssueBin = false;
          return false;
        } else {
          calculateAlreadyShippedQtyOnGrid();
          record.hasOverIssueBin = true;
          record.storageBin = data.overissueBin;
          record.storageBin$_identifier = data.storageBin$_identifier;
        }
      };
      OB.RemoteCallManager.call(
        'org.openbravo.advpaymentmngt.actionHandler.CheckExistsOverissueBinForRFCShipmentWH',
        {
          warehouseId: grid.view.parentWindow.activeView.getContextInfo(
            false,
            true,
            true,
            true
          ).inpmWarehouseId
        },
        {},
        callback
      );
    } else {
      calculateAlreadyShippedQtyOnGrid();
    }
  }
};

/**
 * Update Pending and Available Qty values
 */
OB.RM.RMShipmentQtyValuesChange = function(item, view, form, grid) {
  var record = grid.getSelectionObject().lastSelectionItem;
  if (typeof record === 'undefined') {
    record = grid.getSelectedRecord();
  }
  if (item.pickList.getSelection()[0].id !== item.getValue()) {
    var pending = record.pending;
    grid.setEditValue(
      item.grid.getEditRow(),
      'pending',
      record.pendingQtyInAUM
    );
    record.pending = record.pendingQtyInAUM;
    record.pendingQtyInAUM = pending;
    var availableQty = record.availableQty;
    grid.setEditValue(
      item.grid.getEditRow(),
      'availableQty',
      record.availableQtyInAUM
    );
    record.availableQty = record.availableQtyInAUM;
    record.availableQtyInAUM = availableQty;
    grid.setEditValue(
      item.grid.getEditRow(),
      'returnedUOM',
      item.pickList.getSelection()[0].id
    );
    record.returnedUOM = item.pickList.getSelection()[0].id;
  }
};

/**
 * Update Pending value
 */
OB.RM.RMReceiptQtyValuesChange = function(item, view, form, grid) {
  var record = grid.getSelectionObject().lastSelectionItem;
  if (typeof record === 'undefined') {
    record = grid.getSelectedRecord();
  }
  if (item.pickList.getSelection()[0].id !== item.getValue()) {
    var pending = record.pending;
    grid.setEditValue(
      item.grid.getEditRow(),
      'pending',
      record.pendingQtyInAUM
    );
    record.pending = record.pendingQtyInAUM;
    record.pendingQtyInAUM = pending;
    grid.setEditValue(
      item.grid.getEditRow(),
      'returnedUOM',
      item.pickList.getSelection()[0].id
    );
    record.returnedUOM = item.pickList.getSelection()[0].id;
  }
};
