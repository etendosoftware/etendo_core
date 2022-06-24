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
 * All portions are Copyright (C) 2011-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

//== OBListFilterItem ==
// Combo box for list references in filter editors.
isc.ClassFactory.defineClass('OBListFilterItem', isc.OBListItem);

isc.OBListFilterItem.addProperties({
  allowExpressions: false,
  moveFocusOnPickValue: false,
  operator: 'equals',
  validateOnExit: false,
  validateOnChange: false,
  filterOnKeypress: true,
  addUnknownValues: false,

  defaultToFirstOption: false,

  multiple: true,
  multipleAppearance: 'picklist',
  multipleValueSeparator: ' or ',

  // remove the width so that smartclient will autoflow the content
  // will make sure that the picklist is resized.
  // http://forums.smartclient.com/showthread.php?p=93868#post93868
  getPickListFields: function() {
    var ret = this.Super('getPickListFields', arguments);
    delete ret[0].width;
    return ret;
  },

  // overridden to prevent selection of first item
  selectDefaultItem: function() {},

  showPickList: function() {
    this.Super('showPickList', arguments);

    var value, i;
    //remove double equals symbol used for filtering purposes, so that the appropriate item is selected.
    value = this.getValue();
    if (value && value.length > 0) {
      for (i = 0; i < value.length; i++) {
        if (value[i].indexOf('==') === 0) {
          value[i] = value[i].substring(2, value[i].length);
        }
      }
    } else {
      // If no value is selected, deselect all
      this.pickList.selection.deselectAll();
    }
    this.selectItemFromValue(value);
  },

  // note: can't override changed as it is used by the filter editor
  // itself, see the RecordEditor source code and the changed event
  change: function(form, item, value, oldValue) {
    if (this._pickedValue || !value) {
      // filter with a delay to let the value be set
      isc.Page.setEvent(
        isc.EH.IDLE,
        this.form.grid,
        isc.Page.FIRE_ONCE,
        'performAction'
      );
    }
    this.Super('change', arguments);
  },

  handleKeyPress: function() {
    if (isc.EH.getKey() === 'Space') {
      if (this.isPickListShown() && !this.pickList.bodyKeyPress('Space')) {
        return false;
      }
    }
    return this.Super('handleKeyPress', arguments);
  }
});

isc.OBListFilterItem.changeDefaults('pickListProperties', {
  showOverAsSelected: false,

  bodyKeyPress: function(event, eventInfo) {
    var focusedRecord = this.getRecord(this.getFocusRow()),
      selectedRecords = this.getSelectedRecords(),
      isSelectedRecord = false,
      recordIdentifier =
        this.fields && this.fields[1] && this.fields[1].name
          ? this.fields[1].name
          : '',
      i;
    for (i = 0; i < selectedRecords.length; i++) {
      if (
        focusedRecord[recordIdentifier] === selectedRecords[i][recordIdentifier]
      ) {
        isSelectedRecord = true;
        break;
      }
    }
    if (event === 'Space') {
      // bodyKeyPress doesn't capture 'Space' key press, so used this workaround
      // to obtain it from the 'handleKeyPress' of the parent item
      if (focusedRecord) {
        if (isSelectedRecord) {
          this.deselectRecord(focusedRecord);
        } else {
          this.selectRecord(focusedRecord);
        }
        this.multiSelectChanged();
        // Return false to avoid propagation in the parent item
        return false;
      } else {
        // Return true to ensure propagation in the parent item
        return true;
      }
    }
    if (isc.EH.getKey() === 'Enter') {
      if (!isSelectedRecord) {
        this.selectRecord(focusedRecord);
      }
    }
    return this.Super('bodyKeyPress', arguments);
  }
});
