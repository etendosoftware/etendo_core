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

isc.ClassFactory.defineClass('OBRelativeDateItem', isc.RelativeDateItem);

isc.OBRelativeDateItem.addProperties({
  showChooserIcon: false,
  showPickerTimeItem: false,
  timeUnitOptions: ['day', 'week', 'month', 'quarter', 'year'],
  todayTitle: OB.I18N.getLabel('OBUISC_DateChooser.todayButtonTitle'),

  millisecondsAgoTitle: OB.I18N.getLabel('OBUIAPP_milliseconds_ago'),
  secondsAgoTitle: OB.I18N.getLabel('OBUIAPP_seconds_ago'),
  minutesAgoTitle: OB.I18N.getLabel('OBUIAPP_minutes_ago'),
  hoursAgoTitle: OB.I18N.getLabel('OBUIAPP_hours_ago'),
  daysAgoTitle: OB.I18N.getLabel('OBUIAPP_days_ago'),
  weeksAgoTitle: OB.I18N.getLabel('OBUIAPP_weeks_ago'),
  monthsAgoTitle: OB.I18N.getLabel('OBUIAPP_months_ago'),
  quartersAgoTitle: OB.I18N.getLabel('OBUIAPP_quarters_ago'),
  yearsAgoTitle: OB.I18N.getLabel('OBUIAPP_years_ago'),

  millisecondsFromNowTitle: OB.I18N.getLabel('OBUIAPP_milliseconds_from_now'),
  secondsFromNowTitle: OB.I18N.getLabel('OBUIAPP_seconds_from_now'),
  minutesFromNowTitle: OB.I18N.getLabel('OBUIAPP_minutes_from_now'),
  hoursFromNowTitle: OB.I18N.getLabel('OBUIAPP_hours_from_now'),
  daysFromNowTitle: OB.I18N.getLabel('OBUIAPP_days_from_now'),
  weeksFromNowTitle: OB.I18N.getLabel('OBUIAPP_weeks_from_now'),
  monthsFromNowTitle: OB.I18N.getLabel('OBUIAPP_months_from_now'),
  quartersFromNowTitle: OB.I18N.getLabel('OBUIAPP_quarters_from_now'),
  yearsFromNowTitle: OB.I18N.getLabel('OBUIAPP_years_from_now'),

  startDate: Date.createLogicalDate(1951, 0, 1),
  endDate: Date.createLogicalDate(2050, 11, 31),

  presetOptions: {
    $today: OB.I18N.getLabel('OBUISC_DateChooser.todayButtonTitle'),
    $yesterday: OB.I18N.getLabel('OBUIAPP_Yesterday'),
    $tomorrow: OB.I18N.getLabel('OBUIAPP_Tomorrow'),
    '-1w': OB.I18N.getLabel('OBUIAPP_Current_day_of_last_week'),
    '+1w': OB.I18N.getLabel('OBUIAPP_Current_day_of_next_week'),
    '-1m': OB.I18N.getLabel('OBUIAPP_Current_day_of_last_month'),
    '+1m': OB.I18N.getLabel('OBUIAPP_Current_day_of_next_month')
  },

  // Function to load just needed OB.DateItemProperties properties, since all of them can not be loaded
  // because there are some parameters like "init", "pickerDataChanged", ... that cannot be overwritten
  // because SmartClient also overwrites them while creating this isc.RelativeDateItem definition.
  // Fixes issue: https://issues.openbravo.com/view.php?id=21552
  addDateItemProperties: function() {
    this.setDateParams = OB.DateItemProperties.setDateParams;
    this.parseValue = OB.DateItemProperties.parseValue;
    this.expandPart = OB.DateItemProperties.expandPart;
    this.reachedLength = OB.DateItemProperties.reachedLength;
    this.isNumber = OB.DateItemProperties.isNumber;
    this.isSeparator = OB.DateItemProperties.isSeparator;
    this.setDateParams();
  },

  areDateItemPropertiesSet: false,

  blurValue: function() {
    if (
      this.editor &&
      this.editor.items[0] &&
      this.editor.items[0].getElementValue
    ) {
      return this.editor.items[0].getElementValue();
    } else {
      return null;
    }
  },

  validateOnExit: true,
  showErrorIcon: false,

  validateRelativeDateItem: function(value) {
    var isADate = Object.prototype.toString.call(value) === '[object Date]';
    if (value === null || isADate) {
      this.editor.items[0].textBoxStyle = this.editor.items[0].textBoxStyleNormal;
      this.editor.items[0].redraw();
      return true;
    } else {
      this.editor.items[0].textBoxStyle = this.editor.items[0].textBoxStyleError;
      this.editor.items[0].redraw();
      return false;
    }
  },

  validators: [
    {
      type: 'custom',
      condition: function(item, validator, value) {
        return item.validateRelativeDateItem(value);
      }
    }
  ],

  blur: function() {
    var blurValue = this.blurValue(),
      newBlurValue = '',
      jsValue,
      digitRegExp = new RegExp('^\\d+$', 'gm'),
      newValue,
      i;

    if (!this.areDateItemPropertiesSet) {
      this.addDateItemProperties();
      this.areDateItemPropertiesSet = true;
    }

    // Remove all kind of separators of the input value
    for (i = 0; i < blurValue.length; i++) {
      if (!this.isSeparator(blurValue, i)) {
        newBlurValue += blurValue[i];
      }
    }

    // If are only digits/numbers
    if (digitRegExp.test(newBlurValue)) {
      newValue = this.parseValue();
      if (newValue) {
        jsValue = OB.Utilities.Date.OBToJS(newValue, this.dateFormat);
        // if jsValue == null then this is an illegal date, will be
        // caught later
        if (jsValue) {
          this.setValue(jsValue);
        }
      }
    }

    this.Super('blur', arguments);
  },

  displayFormat: OB.Format.date,
  inputFormat: OB.Format.date,
  pickerConstructor: 'OBDateChooser',

  // overridden as the displayDateFormat does not seem to work fine
  formatDate: function(dt) {
    return OB.Utilities.Date.JSToOB(dt, OB.Format.date);
  },

  // updateEditor() Fired when the value changes (via updateValue or setValue)
  // Shows or hides the quantity box and updates the hint to reflect the current value.
  // overridden to solve: https://issues.openbravo.com/view.php?id=16295
  updateEditor: function() {
    if (!this.valueField || !this.quantityField) {
      return;
    }

    var focusItem,
      selectionRange,
      mustRefocus = false;

    if (this.valueField.hasFocus) {
      focusItem = this.valueField;
      selectionRange = this.valueField.getSelectionRange();
    } else if (this.quantityField.hasFocus) {
      focusItem = this.quantityField;
      selectionRange = this.quantityField.getSelectionRange();
    }

    var value = this.valueField.getValue();

    var showQuantity =
      value && isc.isA.String(value) && this.relativePresets[value];

    if (!showQuantity) {
      if (this.quantityField.isVisible()) {
        mustRefocus = true;
        this.quantityField.hide();
      }
    } else {
      if (!this.quantityField.isVisible()) {
        mustRefocus = true;
        this.quantityField.show();
      }
    }

    if (this.calculatedDateField) {
      value = this.getValue();
      var displayValue = this.editor.getValue('valueField');
      // only show if the value is not a direct date
      // https://issues.openbravo.com/view.php?id=16295
      if (displayValue && displayValue.length > 0) {
        displayValue = OB.Utilities.trim(displayValue);
        // if it starts with a number then it must be a real date
        if (displayValue.charAt(0) < '0' || displayValue.charAt(0) > '9') {
          this.calculatedDateField.setValue(
            !value ? '' : '(' + this.formatDate(value) + ')'
          );
        } else {
          this.calculatedDateField.setValue('');
        }
      } else {
        this.calculatedDateField.setValue('');
      }
    }

    // If we redrew the form to show or hide the qty field, we may need to refocus and
    // reset the selection range
    if (mustRefocus && focusItem !== null) {
      if (!showQuantity && focusItem === this.quantityField) {
        this.valueField.focusInItem();
      } else {
        if (selectionRange) {
          focusItem.delayCall('setSelectionRange', [
            selectionRange[0],
            selectionRange[1]
          ]);
        }
      }
    }
    this.calculatedDateField.canFocus = false;
  },

  // overridden because the picker is now part of the combo and not a separate field.
  // custom code to center the picker over the picker icon
  getPickerRect: function() {
    // we want the date chooser to float centered over the picker icon.
    var form = this.canvas;
    return [
      this.getPageLeft() + form.getLeft(),
      this.getPageTop() + form.getTop() - 40
    ];
  }
});

isc.OBRelativeDateItem.changeDefaults('quantityFieldDefaults', {
  // max 1000 days/months in the past/future
  max: 1000,
  alwaysTakeSpace: false,

  // after leaving the quantity field the next time the rangeitem is visited the
  // focus should go to the value field again
  blur: function() {
    if (this.form && this.form._isRedrawing) {
      return;
    }

    this.Super('blur', arguments);
    this.form.setFocusItem(this.form.getItem('valueField'));
  }
});

isc.OBRelativeDateItem.changeDefaults('valueFieldDefaults', {
  init: function() {
    this.icons = [
      {
        width: this.calendarIconWidth,
        height: this.calendarIconHeight,
        hspace: this.calendarIconHspace,
        canFocus: false,
        showFocused: false,
        item: this,
        src: this.calendarIconSrc,
        click: function() {
          this.item.form.canvasItem.showPicker();
        }
      }
    ];
    this.Super('init', arguments);
  }
});

isc.ClassFactory.defineClass('OBDateRangeItem', isc.DateRangeItem);

isc.OBDateRangeItem.addProperties({
  relativeItemConstructor: 'OBRelativeDateItem'
});

// == OBMiniDateRangeItem ==
// OBMiniDateRangeItem inherits from SmartClient MiniDateRangeItem
// Is used for filtering dates in the grid. Contains the following classes:
// - OBDateRangeDialog: the popup
// - OBMiniDateRangeItem: the filter item itself
isc.ClassFactory.defineClass('OBDateRangeDialog', isc.DateRangeDialog);

isc.OBDateRangeDialog.addProperties({
  rangeItemConstructor: 'OBDateRangeItem',

  initWidget: function() {
    this.Super('initWidget', arguments);
    this.rangeForm.setFocusItem(this.rangeItem);

    var fromField = this.rangeForm.items[0].fromField,
      toField = this.rangeForm.items[0].toField;
    this.clearButton.click = function() {
      this.creator.clearValues();
      fromField.validate();
      toField.validate();
    };
  },

  show: function() {
    this.Super('show', arguments);
    var fromField = this.rangeForm.items[0].fromField,
      toField = this.rangeForm.items[0].toField;
    fromField.calculatedDateField.canFocus = false;
    fromField.validate();
    toField.calculatedDateField.canFocus = false;
    toField.validate();
    fromField.valueField.focusInItem();
    this.rangeForm.focus();
  },

  // trick: overridden to let the ok and clear button change places
  addAutoChild: function(name, props) {
    if (name === 'okButton') {
      return this.Super('addAutoChild', [
        'clearButton',
        {
          canFocus: true,
          title: this.clearButtonTitle
        }
      ]);
    } else if (name === 'clearButton') {
      return this.Super('addAutoChild', [
        'okButton',
        {
          canFocus: true,
          title: this.okButtonTitle
        }
      ]);
    } else {
      return this.Super('addAutoChild', arguments);
    }
  }
});

// == OBMinDateRangeItem ==
// Item used for filtering by dates in the grid. Replaces the normal Smartclient
// MiniDateRangeItem to make it editable.
isc.ClassFactory.defineClass('OBMiniDateRangeItem', isc.OBTextItem);

isc.OBMiniDateRangeItem.addProperties({}, OB.DateItemProperties, {
  validateOnExit: false,
  showPickerIcon: false,
  filterOnKeypress: false,
  operator: 'equals',
  // prevents date formatting using the simple type formatters
  applyStaticTypeFormat: true,

  // note this one needs to be set to let the formatDate be called below
  dateDisplayFormat: OB.Format.date,

  textBoxStyle: 'textItem',
  shouldSaveValue: true,
  rangeDialogConstructor: 'OBDateRangeDialog',
  rangeDialogDefaults: {
    autoDraw: false,
    destroyOnClose: false,
    clear: function() {
      if (this.destroying) {
        return;
      }
      this.Super('clear', arguments);
    }
  },
  iconVAlign: 'center',
  pickerIconDefaults: {
    name: 'showDateRange',
    src: '[SKIN]/DynamicForm/DatePicker_icon.gif',
    width: 16,
    height: 16,
    showOver: false,
    showFocused: false,
    showFocusedWithItem: false,
    hspace: 0,
    click: function(form, item, icon) {
      if (!item.disabled) {
        item.showRangeDialog();
      }
    }
  },

  allowRelativeDates: true,

  // if the user enters a date directly
  singleDateMode: false,
  singleDateValue: null,
  singleDateDisplayValue: null,
  // In P&E grids, on blur will be overridden to ensure correct record selection having filter on change disabled
  canOverrideOnBlur: true,

  init: function() {
    this.addAutoChild('rangeDialog', {
      fromDate: this.fromDate,
      toDate: this.toDate,
      rangeItemProperties: {
        allowRelativeDates: this.allowRelativeDates
      },
      dateDisplayFormat: this.dateDisplayFormat,
      callback: this.getID() + '.rangeDialogCallback(value)'
    });

    this.icons = [
      isc.addProperties(
        {
          prompt: this.pickerIconPrompt
        },
        this.pickerIconDefaults,
        this.pickerIconProperties
      )
    ];

    this.rangeItem = this.rangeDialog.rangeItem;
    this.rangeItem.name = this.name;

    // this call super.init
    if (this.doInit) {
      this.doInit();
    }
  },

  blurValue: function() {
    return this.getElementValue();
  },

  expandSingleValue: function() {
    var newValue = this.parseValue(),
      oldValue = this.mapValueToDisplay(),
      dateValue;

    if (!this.singleDateMode) {
      return;
    }

    // Apply the empty filter if the date text has been deleted
    if (newValue === '' && this.getFieldCriterionFromGrid()) {
      return true;
    }

    if (newValue === oldValue) {
      return false;
    }

    if (this.singleDateMode) {
      dateValue = OB.Utilities.Date.OBToJS(newValue, this.dateFormat);
      if (isc.isA.Date(dateValue)) {
        dateValue.logicalDate = true;
        this.singleDateValue = dateValue;
        this.singleDateDisplayValue = newValue;
        this.singleDateMode = true;
        this.setElementValue(newValue, newValue);
      } else {
        this.singleDateValue = null;
        this.singleDateMode = false;
      }
      return true;
    }
    return false;
  },

  getFieldCriterionFromGrid: function() {
    var currentGridCriteria,
      fieldCriterion,
      criteria,
      sourceGrid = this.getSourceGrid();
    if (sourceGrid && sourceGrid.getCriteria) {
      currentGridCriteria = sourceGrid.getCriteria();
      if (currentGridCriteria) {
        criteria = currentGridCriteria.criteria || [];
        fieldCriterion = criteria.find('fieldName', this.getFieldName());
      }
    }
    return fieldCriterion;
  },

  getSourceGrid: function() {
    return this.grid && this.grid.sourceWidget;
  },

  clearFilterValues: function() {
    this.singleDateValue = null;
    this.singleDateDisplayValue = '';
    this.singleDateMode = true;
    this.rangeItemValue = null;
    this.rangeItem.setValue(null);
    this.setElementValue('', '');
  },

  clearValue: function() {
    // Clear all the filter values, using clearFilterValues
    // See issue https://issues.openbravo.com/view.php?id=29554
    this.clearFilterValues();
    return this.Super('clearValue', arguments);
  },

  setSingleDateValue: function(value) {
    var displayValue = OB.Utilities.Date.JSToOB(value, this.dateFormat);
    this.singleDateValue = value;
    this.singleDateDisplayValue = displayValue;
    this.singleDateMode = true;
    this.setElementValue(displayValue, displayValue);
    // Use setValue() to prevent the clearing of the filter when reapplying the criteria of the form
    // See issue https://issues.openbravo.com/view.php?id=31705
    this.setValue(displayValue);
  },

  blur: function() {
    if (this.form && this.form._isRedrawing) {
      return;
    }

    delete this.previousLazyFilterValue;
    if (this.expandSingleValue()) {
      this.form.grid.performAction();
    }
    return this.Super('blur', arguments);
  },

  showRangeDialog: function() {
    if (!this.rangeItemValue) {
      this.rangeDialog.clear();
      this.rangeItem.fromField.setValue(null);
      this.rangeItem.fromField.quantityField.hide();
      this.rangeItem.toField.setValue(null);
      this.rangeItem.toField.quantityField.hide();
    }
    this.rangeDialog.show();
  },

  rangeDialogCallback: function(value) {
    var data = value,
      illegalStart =
        data && data.start && !this.isCorrectRangeValue(data.start),
      illegalEnd = data && data.end && !this.isCorrectRangeValue(data.end),
      sourceGrid = this.getSourceGrid();
    if (illegalStart || illegalEnd) {
      return;
    }
    this.singleDateMode = false;
    this.singleDateValue = null;
    if (
      sourceGrid &&
      sourceGrid.lazyFiltering &&
      sourceGrid.sorter &&
      this.rangeChanged(data)
    ) {
      sourceGrid.filterHasChanged = true;
      sourceGrid.sorter.enable();
    }
    this.rangeItemValue = value;
    this.displayValue();
    this.form.grid.performAction();
  },

  rangeChanged: function(newRange) {
    var currentStart, currentEnd, newStart, newEnd;
    if (!newRange) {
      return newRange !== this.rangeItemValue;
    }
    if (!this.rangeItemValue) {
      return true;
    }
    currentStart = this.getAbsoluteDate(this.rangeItemValue.start, 'start');
    currentEnd = this.getAbsoluteDate(this.rangeItemValue.end, 'end');
    newStart = this.getAbsoluteDate(newRange.start, 'start');
    newEnd = this.getAbsoluteDate(newRange.end, 'end');
    return (
      0 !== isc.Date.compareLogicalDates(currentStart, newStart) ||
      0 !== isc.Date.compareLogicalDates(currentEnd, newEnd)
    );
  },

  getAbsoluteDate: function(date, rangePosition) {
    var RDI = isc.OBRelativeDateItem;
    return RDI.isRelativeDate(date)
      ? RDI.getAbsoluteDate(date.value, null, null, rangePosition)
      : date;
  },

  hasAdvancedCriteria: function() {
    return (
      this.singleDateMode ||
      (this.rangeItem !== null && this.rangeItem.hasAdvancedCriteria())
    );
  },

  setCriterion: function(criterion) {
    if (!criterion) {
      return;
    }

    if (criterion.operator === 'isNull') {
      this.setValue('#');
      return;
    }

    if (criterion.operator === 'notNull') {
      this.setValue('!#');
      return;
    }

    if (criterion.operator === 'equals') {
      this.setSingleDateValue(criterion.value);
      return;
    }

    if (this.rangeItem) {
      // Clear the range before applying the criteria
      // See issue https://issues.openbravo.com/view.php?id=29661
      this.rangeItem.setValue(null);
      this.rangeItem.setCriterion(criterion);
      this.singleDateMode = false;
      this.singleDateValue = null;
      this.rangeItemValue = this.rangeItem.getValue();
      this.displayValue();
    }
  },

  getCriterion: function() {
    var value = this.blurValue();
    if (value === '#') {
      return {
        fieldName: this.name,
        operator: 'isNull'
      };
    }
    if (value === '!#') {
      return {
        fieldName: this.name,
        operator: 'notNull'
      };
    }
    if (value === '' || value === null) {
      return {};
    }
    if (this.singleDateValue) {
      return {
        fieldName: this.name,
        operator: 'equals',
        value: this.singleDateValue
      };
    }
    var criteria = this.rangeItem ? this.rangeItem.getCriterion() : null;
    if (criteria) {
      criteria = this.makeLogicalDates(criteria);
    }
    return criteria;
  },

  // Sets the logicalDate property to true to the date values contained in the criteria.
  // This way the dates will always be serialized as a Date, and not as a DateTime
  // See issue https://issues.openbravo.com/view.php?id=22885
  makeLogicalDates: function(criteria) {
    var criteriaCopy = isc.shallowClone(criteria),
      innerCriteria = criteriaCopy.criteria,
      i;
    if (innerCriteria && innerCriteria.length) {
      for (i = 0; i < innerCriteria.length; i++) {
        if (isc.isA.Date(innerCriteria[i].value)) {
          innerCriteria[i].value.logicalDate = true;
        }
      }
    }
    return criteriaCopy;
  },

  canEditCriterion: function(criterion) {
    if (
      criterion.fieldName === this.name &&
      (criterion.operator === 'isNull' || criterion.operator === 'notNull')
    ) {
      return true;
    }
    if (this.singleDateMode && criterion.fieldName === this.name) {
      return true;
    }
    return this.rangeItem ? this.rangeItem.canEditCriterion(criterion) : false;
  },

  itemHoverHTML: function(item, form) {
    return this.mapValueToDisplay();
  },

  updateStoredDates: function() {
    var value = this.rangeItemValue,
      i,
      newValue,
      length;

    if (value) {
      if (isc.DataSource.isAdvancedCriteria(value)) {
        // value has come back as an AdvancedCriteria!
        newValue = {};
        length = value.criteria.length;

        for (i = 0; i < length; i++) {
          var criterion = value.criteria[i];
          if (
            criterion.operator === 'greaterThan' ||
            criterion.operator === 'greaterOrEqual'
          ) {
            newValue.start = criterion.value;
          } else if (
            criterion.operator === 'lessThan' ||
            criterion.operator === 'lessOrEqual'
          ) {
            newValue.end = criterion.value;
          }
        }
        value = newValue;
      }

      this.fromDate = value.start;
      this.toDate = value.end;
    } else {
      this.fromDate = null;
      this.toDate = null;
    }
  },

  displayValue: function(value) {
    var displayValue = this.mapValueToDisplay(value) || '';
    this.setElementValue(displayValue, value);
    // Use setValue() to prevent the clearing of the filter invoked through setItemValues method of the form when this.getValue() returns undefined
    // See issue https://issues.openbravo.com/view.php?id=29554
    this.setValue(displayValue);
  },

  setElementValue: function() {
    return this.Super('setElementValue', arguments);
  },

  compareValues: function(value1, value2) {
    if (
      this.isTargetRecordBeingOpened() &&
      value1 === '' &&
      value2 === undefined
    ) {
      // prevent extra DS requests when opening a record directly by ignoring false updates in the item value
      return true;
    }
    return 0 === isc.Date.compareLogicalDates(value1, value2);
  },

  isTargetRecordBeingOpened: function() {
    return (
      this.grid &&
      this.grid.parentElement &&
      this.grid.parentElement.targetRecordId
    );
  },

  mapDisplayToValue: function(display) {
    return display;
  },

  mapValueToDisplay: function(value) {
    if (this.singleDateMode) {
      if (this.singleDateDisplayValue) {
        return this.singleDateDisplayValue;
      }
    }
    if (!this.rangeItemValue) {
      if (!value) {
        return '';
      }
      return value;
    }
    value = this.rangeItemValue;
    var start = this.getAbsoluteDate(value.start, 'start'),
      end = this.getAbsoluteDate(value.end, 'end');

    var prompt;
    if (start || end) {
      if (this.dateDisplayFormat) {
        if (start) {
          prompt = this.formatDate(start);
        }
        if (end) {
          if (prompt) {
            prompt += ' - ' + this.formatDate(end);
          } else {
            prompt = this.formatDate(end);
          }
        }
      } else {
        prompt = Date.getFormattedDateRangeString(start, end);
      }
      if (!start) {
        prompt = this.toDateOnlyPrefix + ' ' + prompt;
      } else if (!end) {
        prompt = this.fromDateOnlyPrefix + ' ' + prompt;
      }
    }
    this.prompt = prompt || '';
    return this.prompt;
  },

  getCriteriaValue: function() {
    return this.getCriterion();
  },

  isCorrectRangeValue: function(value) {
    if (!value) {
      return false;
    }
    if (isc.isA.Date(value)) {
      return true;
    }
    if (value._constructor && value._constructor === 'RelativeDate') {
      return true;
    }
    return false;
  },

  keyPress: function(item, form, keyName, characterValue) {
    var enterOnEmptyItem,
      sourceGrid = this.getSourceGrid(),
      dateBeforeFormatting = item.getEnteredValue();
    if (keyName === 'Enter') {
      if (this.singleDateMode) {
        enterOnEmptyItem = !item.getValue() && !item.getEnteredValue();
        this.expandSingleValue();
        if (sourceGrid && sourceGrid.lazyFiltering && sourceGrid.sorter) {
          if (item.previousLazyFilterValue === undefined && enterOnEmptyItem) {
            // pressing enter without having edited the filter
            item.previousLazyFilterValue = item.mapValueToDisplay();
          } else if (
            item.previousLazyFilterValue !== item.mapValueToDisplay()
          ) {
            // filter has changed
            sourceGrid.filterHasChanged = true;
            sourceGrid.sorter.enable();
            item.previousLazyFilterValue = item.mapValueToDisplay();
            // If date is already formatted correctly in expandSingleValue, filter with current value on enter
            if (dateBeforeFormatting === item.mapValueToDisplay()) {
              this.form.grid.performAction();
            }
            return false;
          } else if (!enterOnEmptyItem || item.previousLazyFilterValue !== '') {
            // filter has not changed: let grid decide if filtering should be performed
            if (enterOnEmptyItem) {
              item.previousLazyFilterValue = '';
            }
            this.form.grid.performAction();
            return false;
          }
        } else {
          this.form.grid.performAction();
        }
        if (!enterOnEmptyItem) {
          return false;
        }
      }
      this.showRangeDialog();
      return false;
    } else if (
      characterValue ||
      keyName === 'Backspace' ||
      keyName === 'Delete'
    ) {
      // only do this if something got really typed in
      this.fromDate = null;
      this.toDate = null;

      // typing, change to single date mode
      this.singleDateMode = true;
      this.singleDateValue = null;
      this.rangeItemValue = null;
      // typing a new value
      this.singleDateDisplayValue = null;
    }
    return true;
  },

  // Explicit destroy of the rangedialog as formitems don't have
  // an auto delete of autochilds
  destroy: function() {
    this.destroying = true;
    if (this.rangeDialog) {
      this.rangeDialog.rangeForm.destroy();
      this.rangeDialog.mainLayout.destroy();
      this.rangeDialog.destroying = true;
      this.rangeDialog.destroy();
      this.rangeDialog.destroying = false;
    }
    this.Super('destroy', arguments);
    this.destroying = false;
  },

  clear: function() {
    if (this.destroying) {
      return;
    }
    this.Super('clear', arguments);
  },

  formatDate: function(dt) {
    return OB.Utilities.Date.JSToOB(dt, OB.Format.date);
  }
});
