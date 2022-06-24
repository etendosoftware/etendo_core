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

// == OBDateItem ==
// OBDateItem inherits from SmartClient DateItem
// adds autocomplete and formatting based on the Openbravo date pattern
isc.ClassFactory.defineClass('OBDateItem', isc.DateItem);

// done like this because the props are re-used in the minidaterange
OB.DateItemProperties = {
  operator: 'equals',
  // ** {{{ pickerConstructor }}} **
  // Picker constructor class
  pickerConstructor: 'OBDateChooser',
  useSharedPicker: true,

  // ** {{{ useTextField }}} **
  // use text field for date entry
  useTextField: true,

  // ** {{{ changeOnKeypress }}} **
  // Fire change event on key press.
  changeOnKeypress: false,

  // is done by the blur event defined here
  validateOnExit: false,
  validateOnChange: false,
  stopOnError: false,

  setDateParams: function() {
    var i,
      dateFormatUpper,
      index = 0,
      length,
      currentTime;

    this.dateFormat = OB.Format.date;
    this.dateParts = [];

    dateFormatUpper = this.dateFormat.toUpperCase();
    length = dateFormatUpper.length;
    this.dateSeparator = OB.Utilities.Date.getDateSeparator(this.dateFormat);

    for (i = 0; i < length; i++) {
      if (this.isSeparator(dateFormatUpper, i)) {
        index++;
      } else {
        this.dateParts[index] = dateFormatUpper.charAt(i);
      }
    }
    currentTime = new Date();
    this.currentMonth = String(currentTime.getMonth() + 1);
    if (this.currentMonth.length === 1) {
      this.currentMonth = '0' + this.currentMonth;
    }
    this.currentDay = String(currentTime.getDate());
    if (this.currentDay.length === 1) {
      this.currentDay = '0' + this.currentDay;
    }
    this.currentYear = String(currentTime.getFullYear());
  },

  doInit: function() {
    this.setDateParams();

    var ret = this.Super('init', arguments);

    if (this.textField) {
      this.textField.selectOnClick = this.selectOnClick;
      if (this.length) {
        this.textField.length = this.length;
      }
      this.textField.changed = function() {
        // when the textfield of the date is updated, the date
        // field should be flagged as changed
        // see issue 20071 (https://issues.openbravo.com/view.php?id=20071)
        this._textChanged = true;
        this.parentItem._hasChanged = true;
        // There is a mechanism to prevent infinite looping in number fields
        // (see issue https://issues.openbravo.com/view.php?id=17290) that
        // interferes with the correct behaviour of the date fields
        // The infinite looping described in the issue does not apply to date fields,
        // so it is safe to delete the saveFocusItemChanged flag when a date is modified
        if (
          this.parentItem.form &&
          this.parentItem.form.view &&
          this.parentItem.form.view.viewForm
        ) {
          delete this.parentItem.form.view.viewForm.saveFocusItemChanged;
        }
      };
      // This is needed for the unit tests to be able to enter the dates using the setValue method
      this.dateTextField.setValue = function(newValue) {
        var oldValue = this.getValue();
        if (newValue && !this.parentItem.hasSeparator(newValue)) {
          newValue = oldValue;
        }
        this.Super('setValue', newValue);
        // only flag the date as changed if it had a value, and it
        // has been actually changed
        if (!newValue || !oldValue || oldValue === newValue) {
          return;
        }
        this.parentItem.textField._textChanged = true;
        this.parentItem._hasChanged = true;
        if (
          this.parentItem.form &&
          this.parentItem.form.view &&
          this.parentItem.form.view.viewForm
        ) {
          delete this.parentItem.form.view.viewForm.saveFocusItemChanged;
        }
      };
    }

    if (this.showDisabled === false) {
      this.textField.showDisabled = false;
    }

    return ret;
  },

  // compare while ignoring milli difference
  compareValues: function(value1, value2) {
    return 0 === isc.Date.compareLogicalDates(value1, value2);
  },

  parseValue: function() {
    var i,
      str = this.blurValue(),
      length,
      parts = ['', '', ''],
      partIndex = 0,
      result;

    if (!str || isc.isA.Date(str) || str.replace(/0/g, '') === '') {
      return str;
    }
    length = str.length;
    for (i = 0; i < length; i++) {
      if (this.isNumber(str, i)) {
        if (this.reachedLength(parts[partIndex], partIndex)) {
          partIndex++;
        }
        if (partIndex === 3) {
          break;
        }
        parts[partIndex] = parts[partIndex] + str.charAt(i);
      } else if (this.isSeparator(str, i)) {
        partIndex++;
      } else {
        // invalid date
        return str;
      }
      if (partIndex === 3) {
        break;
      }
    }

    if (partIndex === 0 && parts[partIndex] !== '') {
      // Only one or two digits have been entered, they represent the number of days
      if (this.dateParts[1] === 'D') {
        parts[1] = parts[0];
        parts[0] = '';
      } else if (this.dateParts[2] === 'D') {
        parts[2] = parts[0];
        parts[0] = '';
      }
    }

    for (i = 0; i < 3; i++) {
      if (
        (parts[i] === '0' || parts[i] === '00') &&
        (this.dateParts[i] === 'D' || this.dateParts[i] === 'M')
      ) {
        return str;
      } else {
        parts[i] = this.expandPart(parts[i], i);
      }
    }
    result =
      parts[0] + this.dateSeparator + parts[1] + this.dateSeparator + parts[2];
    return result;
  },

  expandPart: function(part, index) {
    var year;
    if (this.reachedLength(part, index)) {
      return part;
    }
    if (part === '') {
      if (this.dateParts[index] === 'D') {
        return this.currentDay;
      } else if (this.dateParts[index] === 'M') {
        return this.currentMonth;
      } else {
        return this.currentYear;
      }
    } else if (this.dateParts[index] === 'Y') {
      year = parseInt(part, 10);
      if (year <= 50) {
        return String(2000 + year);
      } else if (year < 100) {
        return String(1900 + year);
      } else {
        return '2' + part;
      }
    } else if (part.length === 1) {
      return '0' + part;
    }
    return part;
  },

  reachedLength: function(part, index) {
    var maxLength;
    if (this.dateParts[index] === 'D' || this.dateParts[index] === 'M') {
      maxLength = 2;
    } else {
      maxLength = 4;
    }
    return part.length >= maxLength;
  },

  isNumber: function(str, position) {
    return str.charAt(position) >= '0' && str.charAt(position) <= '9';
  },

  isSeparator: function(str, position) {
    return (
      str.charAt(position) === '-' ||
      str.charAt(position) === '\\' ||
      str.charAt(position) === '/' ||
      str.charAt(position) ===
        OB.Utilities.Date.getDateSeparator(OB.Format.date)
    );
  },

  hasSeparator: function(str) {
    var result = false,
      i;
    if (typeof str === 'string') {
      for (i = 0; i < str.length; i++) {
        if (this.isSeparator(str, i)) {
          result = true;
          break;
        }
      }
    }
    return result;
  },

  pickerDataChanged: function(picker) {
    this.isAPickerDataChange = true;
    this.Super('pickerDataChanged', arguments);
    this.isAPickerDataChange = false;
    // update the date field after picking a new date
    this.textField._textChanged = true;
    this.updateValue();
    if (this.form.focusInNextItem) {
      if (this.form.handleItemChange) {
        this._hasChanged = true;
        this.form.handleItemChange(this);
      }

      this.form.focusInNextItem(this.name);
    } else {
      // See issue https://issues.openbravo.com/view.php?id=29544
      // This is done to keep the focus when a date is selected from the picker in a parameter window
      if (this.form.handleItemChange) {
        this.form.handleItemChange(this);
      }
    }
  }
};

isc.OBDateItem.addProperties(OB.DateItemProperties, {
  validateOnExit: true,

  startDate: Date.createLogicalDate(1951, 0, 1),
  endDate: Date.createLogicalDate(2050, 11, 31),

  init: function() {
    // this call super.init
    if (typeof this.doInit === 'function') {
      return this.doInit();
    } else {
      return this.Super('init', arguments);
    }
  },

  setValue: function(value) {
    var ret,
      oldValue,
      newArguments = arguments;

    oldValue = this.getValue();
    if (
      Object.prototype.toString.call(newArguments[0]) === '[object String]' &&
      !this.hasSeparator(newArguments[0])
    ) {
      newArguments[0] = oldValue;
    }

    //Setting the value to the textual value in case text field is changed. Fixes Issue https://issues.openbravo.com/view.php?id=22381
    if (
      this.textField._textChanged &&
      this.textField.getValue() &&
      this.textField.getValue() !== value
    ) {
      value = OB.Utilities.Date.OBToJS(
        this.textField.getValue(),
        this.showTime ? OB.Format.dateTime : OB.Format.date
      );
    }

    ret = this.Super('setValue', newArguments);

    return ret;
  },

  setDateParameterValue: function(value) {
    // See issue https://issues.openbravo.com/view.php?id=29544
    // After setting the parameter value from an onchange function, the _textChanged was always true. This prevented the parameter value to be updated after the first change.
    this.setValue(value);
    if (this.textField) {
      delete this.textField._textChanged;
    }
  },

  expandValue: function() {
    var newValue = this.parseValue(),
      oldValue = this.blurValue();

    if (oldValue !== newValue) {
      this.dateTextField.setValue(newValue);
    }
  },

  // update the value in update value as this is called from cellEditEnd in the
  // grid, after losing the focus on the form and when autosaving
  updateValue: function() {
    var savingWithShortcut;
    if (this.grid && this.grid.view && this.grid.view.savingWithShortcut) {
      savingWithShortcut = this.grid.view.savingWithShortcut;
    } else {
      savingWithShortcut = false;
    }
    if (
      this.grid &&
      this.grid._preventDateParsing &&
      !this.grid._autoSaving &&
      !savingWithShortcut &&
      !this.grid._leavingCell
    ) {
      return;
    }
    if (this.textField._textChanged) {
      this.expandValue();
      this.Super('updateValue', arguments);
      //  when the date field has a callout and all the mandatory fields have been entered,
      //  the grid does not save the value before making the FIC call, so the value has to
      //  be saved explicitly
      //  See issue 19694 (https://issues.openbravo.com/view.php?id=19694)
      if (this.grid && this.grid.isEditing && this.grid.isEditing()) {
        this.grid.setEditValue(
          this.grid.getEditRow(),
          this.name,
          this.getValue(),
          true,
          true
        );
      }
      this.textField._textChanged = false;
    }
  },

  getPickerData: function() {
    var dateValue = this.getValue(),
      date,
      utcDate,
      utcMonth,
      utcFullYear,
      utcHours,
      utcMinutes,
      utcSeconds;
    // To visualize in the calendar the displayed value instead of internal value, since due to the GMT difference it can be in a different day
    if (this.isAbsoluteDateTime && isc.isA.Date(dateValue)) {
      date = new Date(dateValue);
      utcDate = date.getUTCDate();
      utcMonth = date.getUTCMonth();
      utcFullYear = date.getUTCFullYear();
      utcHours = date.getUTCHours();
      utcMinutes = date.getUTCMinutes();
      utcSeconds = date.getUTCSeconds();
      // Set the month initially to January to prevent error like this
      // provided date: 15/02/2014
      // today: 31/03/2014
      // date.setDate(today.getDate()) would result in Mon Mar 02 2014 18:00:00 GMT+0100 (CET), because February does not have 31 days
      date.setMonth(0);
      date.setDate(utcDate);
      date.setMonth(utcMonth);
      date.setFullYear(utcFullYear);
      date.setHours(utcHours);
      date.setMinutes(utcMinutes);
      date.setSeconds(utcSeconds);
    } else {
      date = dateValue;
    }
    if (isc.isA.Date(date)) {
      return date;
    } else {
      return this.Super('getPickerData', arguments);
    }
  },

  blur: function() {
    // force the update of the date when its field loses the focus
    // it has to be done before the call to the super because the
    // date should be updated before calling handleItemChange,
    // which is called in the super blur
    this.updateValue();
    this.Super('blur', arguments);
  },

  blurValue: function() {
    return this.dateTextField.getElementValue();
  },

  validateOBDateItem: function(value) {
    var dateValue = OB.Utilities.Date.OBToJS(value, this.dateFormat);
    var isValid = true;
    if (this.getValue() && dateValue === null) {
      isValid = false;
    }
    var isRequired = this.required;
    if (isValid === false) {
      return false;
    } else if (isRequired === true && value === null) {
      return false;
    }
    return true;
  },

  formSaved: function(request, response, data) {
    if (this.textField) {
      delete this.textField._textChanged;
    }
  },

  validators: [
    {
      type: 'custom',
      condition: function(item, validator, value) {
        return item.validateOBDateItem(value);
      }
    }
  ]
});

OB.I18N.getLabel(
  'OBUIAPP_InvalidValue',
  null,
  isc.OBDateItem,
  'invalidValueLabel'
);
OB.I18N.getLabel(
  'OBUISC_Validator.requiredField',
  null,
  isc.OBDateItem,
  'requiredValueLabel'
);
