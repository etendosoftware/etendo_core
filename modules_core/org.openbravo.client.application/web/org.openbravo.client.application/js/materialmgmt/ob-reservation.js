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
 * All portions are Copyright (C) 2012-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB.Reservation = OB.Reservation || {};

/**
 * Check that entered quantity to reserve is available in the selected record
 * and that total reserved quantity is below the needed quantity
 */
OB.Reservation.QuantityValidate = function(item, validator, value, record) {
  var availableQty = isc.isA.Number(record.availableQty)
      ? new BigDecimal(String(record.availableQty))
      : BigDecimal.prototype.ZERO,
    releasedQty = isc.isA.Number(record.released)
      ? new BigDecimal(String(record.released))
      : BigDecimal.prototype.ZERO,
    reservedinothersQty = isc.isA.Number(record.reservedinothers)
      ? new BigDecimal(String(record.reservedinothers))
      : BigDecimal.prototype.ZERO,
    quantity = null,
    reservedQty = BigDecimal.prototype.ZERO,
    totalQty = isc.isA.Number(record.reservationQuantity)
      ? new BigDecimal(String(record.reservationQuantity))
      : BigDecimal.prototype.ZERO,
    selectedRecords = item.grid.getSelectedRecords(),
    selectedRecordsLength = selectedRecords.length,
    editedRecord = null,
    isQtyVariable = item.grid.view.sourceView.getContextInfo(
      false,
      true,
      true,
      true
    ).isQuantityVariable,
    i;

  if (!isc.isA.Number(value)) {
    return false;
  }
  if (value === null || value < 0) {
    return false;
  }
  quantity = new BigDecimal(String(value));
  if (
    quantity
      .subtract(releasedQty)
      .compareTo(availableQty.subtract(reservedinothersQty)) > 0
  ) {
    isc.warn(
      OB.I18N.getLabel('OBUIAPP_Res_MoreQtyThanAvailable', [
        record.availableQty,
        record.reservedinothers
      ])
    );
    return false;
  }
  if (quantity.compareTo(releasedQty) < 0) {
    isc.warn(
      OB.I18N.getLabel('OBUIAPP_Res_LessThanReleased', [record.released])
    );
    return false;
  }
  for (i = 0; i < selectedRecordsLength; i++) {
    editedRecord = isc.addProperties(
      {},
      selectedRecords[i],
      item.grid.getEditedRecord(selectedRecords[i])
    );
    if (isc.isA.Number(editedRecord.quantity)) {
      reservedQty = reservedQty.add(
        new BigDecimal(String(editedRecord.quantity))
      );
    }
  }
  if (reservedQty.compareTo(totalQty) > 0 && isQtyVariable !== 'Y') {
    isc.warn(
      OB.I18N.getLabel('OBUIAPP_Res_MoreThanReservationQty', [
        totalQty.toString()
      ])
    );
    return false;
  }
  // get reservation quantity and released quantity to check totals
  return true;
};

OB.Reservation.PrereservationQuantityValidate = function(
  item,
  validator,
  value,
  record
) {
  var reservedQty = isc.isA.Number(record.reservedQty)
      ? new BigDecimal(String(record.reservedQty))
      : BigDecimal.prototype.ZERO,
    purchasedQty = isc.isA.Number(record.purchasedQty)
      ? new BigDecimal(String(record.purchasedQty))
      : BigDecimal.prototype.ZERO,
    receivedQty = isc.isA.Number(record.receivedQty)
      ? new BigDecimal(String(record.receivedQty))
      : BigDecimal.prototype.ZERO,
    pendingQty = purchasedQty.subtract(receivedQty),
    orderedQuantity = isc.isA.Number(record.orderedQuantity)
      ? new BigDecimal(String(record.orderedQuantity))
      : BigDecimal.prototype.ZERO,
    otherReservedQty = isc.isA.Number(record.otherReservedQty)
      ? new BigDecimal(String(record.otherReservedQty))
      : BigDecimal.prototype.ZERO,
    solTotalReserved = BigDecimal.prototype.ZERO,
    totalQty = BigDecimal.prototype.ZERO,
    selectedRecords = item.grid.getSelectedRecords(),
    selectedRecordsLength = selectedRecords.length,
    editedRecord = null,
    i;

  if (!isc.isA.Number(value)) {
    return false;
  }
  if (value === null || value < 0) {
    return false;
  }
  reservedQty = new BigDecimal(String(value));
  solTotalReserved = reservedQty.add(otherReservedQty);
  if (solTotalReserved.compareTo(orderedQuantity) > 0) {
    isc.warn(
      OB.I18N.getLabel('OBUIAPP_Res_MoreThanOrderedQty', [
        record.orderedQuantity,
        record.otherReservedQty
      ])
    );
    return false;
  }

  for (i = 0; i < selectedRecordsLength; i++) {
    editedRecord = isc.addProperties(
      {},
      selectedRecords[i],
      item.grid.getEditedRecord(selectedRecords[i])
    );
    if (isc.isA.Number(editedRecord.reservedQty)) {
      totalQty = totalQty.add(new BigDecimal(String(editedRecord.reservedQty)));
    }
  }
  if (totalQty.compareTo(pendingQty) > 0) {
    isc.warn(
      OB.I18N.getLabel('OBUIAPP_Res_MoreThanPendingQty', [
        solTotalReserved.toString()
      ])
    );
    return false;
  }
  return true;
};
