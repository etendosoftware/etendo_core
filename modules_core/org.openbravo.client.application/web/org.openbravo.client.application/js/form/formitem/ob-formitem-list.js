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
 * All portions are Copyright (C) 2011-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

// == OBListItem ==
// Combo box for list references, note is extended by OBFKItem again.
isc.ClassFactory.defineClass('OBListItem', isc.OBComboBoxItem);

isc.OBListItem.addProperties({
  operator: 'equals',
  hasPickList: true,
  showPickListOnKeypress: true,
  cachePickListResults: false,
  completeOnTab: true,
  validateOnExit: true,

  selectOnFocus: true,
  // still do select on focus initially
  doInitialSelectOnFocus: true,

  // textMatchStyle is used for the client-side picklist
  textMatchStyle: 'substring',

  pickListProperties: {
    showHeaderContextMenu: false
  },

  // NOTE: Setting this property to false fixes the issue when using the mouse
  // to pick a value
  // FIXME: Sometimes the field label gets a red color (a blink)
  // if set to false then the picklist is shown at focus:
  // https://issues.openbravo.com/view.php?id=18075
  // addUnknownValues: true,
  // changeOnKeypress should not be set to false together
  // with addUnknownValues (to false) as this will
  // cause the picklist not to show
  // changeOnKeypress: false,
  moveFocusOnPickValue: true,

  hidePickListOnBlur: function() {
    // when the form gets redrawn the the focus may not be in
    // the item but it is still the item which gets the focus
    // after redrawing
    if (
      this.form &&
      this.form._isRedrawing &&
      this.form.getFocusItem() === this
    ) {
      return;
    }

    this.Super('hidePickListOnBlur', arguments);
  },

  // is overridden to keep track that a value has been explicitly picked
  pickValue: function(value) {
    var i, referenceType;
    this._pickedValue = true;
    // force the update of the list
    // if the user has entered with the keyboard the exact content of a list option,
    // its callout would not be called because the change would not be detected
    // see issue https://issues.openbravo.com/view.php?id=21491
    this._value = this.value
      ? this._value.concat(Math.random())
      : Math.random();
    //in case the reference is a foreign key reference,
    //adding double equals to filter the exact value and not all matching sub strings.
    //Refer issue https://issues.openbravo.com/view.php?id=24574.
    referenceType = isc.SimpleType.getType(this.type).editorType;
    if (value && isc.isA.Array(value) && referenceType !== 'OBListItem') {
      // value is an array when picking in a FK selector drop down
      // add '==' if needed
      for (i = 0; i < value.length; i++) {
        //do not append when composite identifiers are present.
        if (value[i].indexOf(' - ') === -1) {
          value[i] = '==' + value[i];
        }
      }
    }
    this.Super('pickValue', arguments);
    delete this._pickedValue;
    if (this.moveFocusOnPickValue && this.form.focusInNextItem) {
      // update the display before moving the focus
      this.updateValueMap(true);
      // Only focus in the next item if the key that triggered the event is
      // not the tab key, so the focus is not moved twice
      // See issue https://issues.openbravo.com/view.php?id=21419
      if (isc.EH.getKeyName() !== 'Tab') {
        this.form.focusInNextItem(this.name);
      }
    }
  },

  changed: function(form, item, value) {
    this.Super('changed', arguments);
    // if not picking a value then don't do a fic call
    // otherwise every keypress would result in a fic call
    if (!this._pickedValue) {
      return;
    }
    if (this._hasChanged && this.form && this.form.handleItemChange) {
      this.form.handleItemChange(this);
    }
  },

  // to solve: https://issues.openbravo.com/view.php?id=17800
  // in chrome the order of the valueMap object is not retained
  // the solution is to keep a separate entries array with the
  // records in the correct order, see also the setEntries/setEntry
  // methods
  getClientPickListData: function() {
    if (this.entries) {
      return this.entries;
    }
    return this.Super('getClientPickListData', arguments);
  },

  setEntries: function(entries) {
    var length = entries.length,
      i,
      id,
      identifier,
      valueField = this.getValueFieldName(),
      valueMap = {};
    this.entries = [];
    for (i = 0; i < length; i++) {
      id = entries[i][OB.Constants.ID] || '';
      identifier = entries[i][OB.Constants.IDENTIFIER] || '';
      valueMap[id] = identifier.asHTML();
      this.entries[i] = {};
      this.entries[i][valueField] = id;
    }
    this.setValueMap(valueMap);
  },

  setEntry: function(id, identifier) {
    var i,
      entries = this.entries || [],
      entry = {},
      valueField = this.getValueFieldName(),
      length = entries.length;
    for (i = 0; i < length; i++) {
      if (entries[i][valueField] === id) {
        return;
      }
    }

    // not found add/create a new one
    entry[valueField] = id;

    if (id && identifier) {
      entry[OB.Constants.ID] = id;
      entry[OB.Constants.IDENTIFIER] = identifier;
    }

    entries.push(entry);
    this.setEntries(entries);
  },

  // prevent ids from showing up
  mapValueToDisplay: function(value) {
    var ret = this.Super('mapValueToDisplay', arguments);

    // the datasource should handle it
    if (this.optionDataSource) {
      return ret;
    }

    if (this.valueMap) {
      // handle multi-select
      if (isc.isA.Array(value)) {
        this.lastSelectedValue = value;
      } else if (this.valueMap[value]) {
        this.lastSelectedValue = value;
        return this.valueMap[value].unescapeHTML();
      }
    }

    if (ret === value && this.isDisabled()) {
      return '';
    }

    // don't update the valuemap if the value is null or undefined
    if (ret === value && value) {
      if (!this.valueMap) {
        this.valueMap = {};
        this.valueMap[value] = '';
        return '';
      } //there may be cases if the value is an number within 10 digits, it is identified as an UUID. In that case check is done to confirm whether it is indeed UUID by checking if it is available in the valueMap.
      else if (
        !this.valueMap[value] &&
        OB.Utilities.isUUID(value) &&
        Object.prototype.hasOwnProperty.call(this.valueMap, value)
      ) {
        return '';
      }
    }
    return ret;
  },

  isUnknownValue: function(value) {
    var i, array;
    if (
      !value ||
      !this.multiple ||
      !value.contains(this.multipleValueSeparator)
    ) {
      return this.Super('isUnknownValue', arguments);
    }
    // handle multi-select
    array = value.split(this.multipleValueSeparator);
    for (i = 0; i < array.length; i++) {
      if (this.isUnknownValue(array[i])) {
        return true;
      }
    }
    return false;
  },

  mapDisplayToValue: function(display) {
    var i, array, result;

    if (display === '') {
      return null;
    }
    if (
      this.lastSelectedValue &&
      display === this.mapValueToDisplay(this.lastSelectedValue)
    ) {
      // Prevents mapDisplayToValue from failing when there are several
      // entries in the valuemap with the same value
      // See issue https://issues.openbravo.com/view.php?id=21553
      return this.lastSelectedValue;
    } else if (
      !display ||
      !this.multiple ||
      !display.contains(this.multipleValueSeparator)
    ) {
      return this.Super('mapDisplayToValue', arguments);
    } else {
      array = display.split(this.multipleValueSeparator);
      result = [];
      for (i = 0; i < array.length; i++) {
        result.add(this.Super('mapDisplayToValue', [array[i]]));
      }
      return result;
    }
  }
});
