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
 * All portions are Copyright (C) 2014-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB.ReservedGoodMovement = OB.ReservedGoodMovement || {};

/**
 * Check that entered movementqty is not higher than (ReservedQty-releasedQty).
 * Check that entered movementqty is higher than 0
 */
OB.ReservedGoodMovement.QuantityValidate = function(
  item,
  validator,
  value,
  record
) {
  var movementQty = null,
    releasedQty = isc.isA.Number(record.releasedqty)
      ? new BigDecimal(String(record.releasedqty))
      : BigDecimal.prototype.ZERO,
    quantity = isc.isA.Number(record.quantity)
      ? new BigDecimal(String(record.quantity))
      : BigDecimal.prototype.ZERO;
  if (!isc.isA.Number(value)) {
    return false;
  }
  if (value === null || value < 0) {
    return false;
  }
  movementQty = new BigDecimal(String(value));
  //if movementQty>quantity-releasedqty ERROR
  if (movementQty.compareTo(quantity.subtract(releasedQty)) > 0) {
    isc.warn(OB.I18N.getLabel('OBUIAPP_MoveQtyLowerthanReserved'));
    return false;
  }
  //Cannot move qty 0
  if (movementQty.compareTo(BigDecimal.prototype.ZERO) === 0) {
    isc.warn(OB.I18N.getLabel('OBUIAPP_DefineQtyToMove'));
    return false;
  }

  return true;
};

/**
 * Check that entered storageBin is different from actual storageBin.
 * Check that entered storageBin is not null
 */

OB.ReservedGoodMovement.StorageValidate = function(
  item,
  validator,
  value,
  record
) {
  if (value === null) {
    isc.warn(OB.I18N.getLabel('OBUIAPP_DefineStorageBin'));
    return false;
  }
  //if storageBien == NewStorageBin ERROR
  if (record.storageBin === value) {
    isc.warn(OB.I18N.getLabel('OBUIAPP_DifferentSB'));
    return false;
  }

  return true;
};
