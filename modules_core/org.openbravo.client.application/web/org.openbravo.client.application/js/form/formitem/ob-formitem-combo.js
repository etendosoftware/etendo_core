/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use. this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2015-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

// == OBComboBoxItem ==
// Base Combo Box for list and selector references
isc.ClassFactory.defineClass('OBComboBoxItem', isc.ComboBoxItem);

isc.OBComboBoxItem.addProperties({
  // ** {{{ filterComplete }}} **
  //
  // Prevents validation of this item while filtering because real value is not
  // set yet.
  // see issues #26189 and #28651
  filterComplete: function(response, data, request, fromSharedPickList) {
    var ret;

    if (
      request &&
      request.params &&
      request.params.recordIdInForm &&
      request.params.recordIdInForm !== this.form.recordIdInForm
    ) {
      return;
    }
    this.preventValidation = true;
    ret = this.Super('filterComplete', arguments);
    delete this.preventValidation;
    return ret;
  },

  // Override showPicker to ensure that we select the text when clicking on the drop-down
  // See issue #31274
  showPicker: function() {
    this.selectValue();
    this.Super('showPicker', arguments);
  },

  // Override handleKeyPress to ensure that we select the text when pressing the keyboard shortcut of the drop-down
  // See issue #31377
  handleKeyPress: function() {
    if (this.keyboardShortcutPressed()) {
      this.selectValue();
    }
    return this.Super('handleKeyPress', arguments);
  },

  // Checks if the ALT + keyboard down key combination has been pressed on a valid state of the combo box
  keyboardShortcutPressed: function() {
    if (this.hasFocus && !this.isReadOnly()) {
      var keyName = isc.EH.lastEvent.keyName;
      if (keyName === 'Arrow_Down' && isc.EH.altKeyDown()) {
        return true;
      }
    }
    return false;
  },

  // This function will fall through to filterComplete() when the filter operation returns
  filterDataBoundPickList: function(requestProperties, dropCache) {
    if (this.form && this.form.view && this.form.view.isShowingForm) {
      // Identify the record being currently edited in form view and include it in the request.
      // It will be used to avoid problems when a new record is opened in form view before filterComplete() finishes
      // See issue https://issues.openbravo.com/view.php?id=31331
      requestProperties.params = requestProperties.params || {};
      requestProperties.params.recordIdInForm = this.form.recordIdInForm;
    }
    if (
      this.grid &&
      this.grid.parentElement &&
      this.grid.parentElement.getClassName() === 'OBPickAndExecuteGrid'
    ) {
      // Identify the case when using an Pick & Execute grid and trying to filter with local window parameters
      // This will add those parameters to the request, so it handles combos in grids in those type of windows correctly
      // See issue https://issues.openbravo.com/view.php?id=42239
      requestProperties = requestProperties || {};
      requestProperties.params = requestProperties.params || {};
      isc.addProperties(
        requestProperties.params,
        this.grid.parentElement.view.theForm.getValues()
      );
    }
    return this.Super('filterDataBoundPickList', [
      requestProperties,
      dropCache
    ]);
  },
  // Override mapDisplayToValue to handle combo data when changing records
  mapDisplayToValue: function(value) {
    const displayedValue = this.Super('mapDisplayToValue', arguments);
    // Uses getValueMap() instead of this.valueMap because the latter is not always updated when changing records
    const valueMap = this.getValueMap();
    if (valueMap && !(displayedValue in valueMap)) {
      // displayedValue has not been found in valueMap, check if value is an element of valueMap, return id if so
      for (let id in valueMap) {
        if (value === valueMap[id]) {
          return id;
        }
      }
    }
    // Element not present in valueMap either, returning Super value
    return displayedValue;
  }
});
