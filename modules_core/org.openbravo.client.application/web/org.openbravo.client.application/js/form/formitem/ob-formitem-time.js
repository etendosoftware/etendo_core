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
 * All portions are Copyright (C) 2011-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

// set global time formatters
isc.Time.shortDisplayFormat = OB.Utilities.getTimeFormatDefinition().timeFormatter;
isc.Time.displayFormat = OB.Utilities.getTimeFormatDefinition().timeFormatter;

// == OBTimeItem ==
// For entering times.
isc.ClassFactory.defineClass('OBTimeItem', isc.TimeItem);

isc.OBTimeItem.addClassMethods({
  setTodaysDate: function(date) {
    var today = new Date();
    // Set the month initially to January to prevent error like this
    // provided date: 15/02/2014
    // today: 31/03/2014
    // date.setDate(today.getDate()) would result in Mon Mar 02 2014 18:00:00 GMT+0100 (CET), because february does not have 31 days
    date.setMonth(0);
    date.setDate(today.getDate());
    date.setMonth(today.getMonth());
    date.setYear(today.getFullYear());
  }
});

isc.OBTimeItem.addProperties({
  useTextField: true,
  operator: 'equals',
  validateOnExit: true,
  showHint: false,
  timeFormatter: isc.Time.displayFormat,

  mapValueToDisplay: function(value) {
    var newValue = value;
    if (isc.isA.Date(newValue)) {
      if (this.isAbsoluteTime) {
        // In the case of an absolute time, the time needs to be converted in order to avoid the UTC conversion
        // http://forums.smartclient.com/showthread.php?p=116135
        newValue = OB.Utilities.Date.addTimezoneOffset(newValue);
      }
      newValue = isc.Time.toShortTime(newValue, this.timeFormatter);
    }
    return newValue;
  },

  mapDisplayToValue: function(value) {
    var newValue = value;
    if (
      newValue &&
      Object.prototype.toString.call(newValue) === '[object String]'
    ) {
      newValue = isc.Time.parseInput(newValue);
      isc.OBTimeItem.setTodaysDate(newValue);
      if (this.isAbsoluteTime) {
        // In the case of an absolute time, the time needs to be converted in order to avoid the UTC conversion
        // http://forums.smartclient.com/showthread.php?p=116135
        newValue = OB.Utilities.Date.substractTimezoneOffset(newValue);
      }
    }
    return newValue;
  },

  // make sure that the undo/save buttons get enabled, needs to be done like
  // this because changeOnKeypress is false. Activating changeOnKeypress makes the
  // item not editable as it is reformatted on keyStroke, the same happens calling
  // from this method form.itemChangeActions
  keyPress: function(item, form, keyName, characterValue) {
    var i,
      f = this.form,
      toolBarButtons;

    if (
      f &&
      f.view &&
      f.view.toolBar &&
      f.view.messageBar &&
      f.setHasChanged &&
      (characterValue || keyName === 'Backspace' || keyName === 'Delete')
    ) {
      toolBarButtons = f.view.toolBar.leftMembers;
      f.setHasChanged(true);
      f.view.messageBar.hide();
      for (i = 0; i < toolBarButtons.length; i++) {
        if (toolBarButtons[i].updateState) {
          toolBarButtons[i].updateState();
        }
      }
    }
  },

  // SmartClient's TimeItem doesn't keep time zone. Preserve it in case the
  // string contains time zone. So time in this format is kept: 12:00+01:00
  setValue: function(value) {
    if (isc.isA.String(value) && (value.contains('+') || value.contains('-'))) {
      value = isc.Time.parseInput(value, null, null, true);
    }
    if (value && isc.isA.String(value)) {
      value = isc.Time.parseInput(value);
    }
    if (value && isc.isA.Date(value)) {
      if (this.isAbsoluteTime) {
        value = OB.Utilities.Date.addTimezoneOffset(value);
      }
      isc.OBTimeItem.setTodaysDate(value);
      if (this.isAbsoluteTime) {
        value = OB.Utilities.Date.substractTimezoneOffset(value);
      }
    }
    return this.Super('setValue', arguments);
  },

  getValue: function() {
    var value = this.Super('getValue', arguments);
    if (value && isc.isA.Date(value) && !this.isAbsoluteTime) {
      isc.OBTimeItem.setTodaysDate(value);
    }
    return value;
  },

  /* The following functions allow proper timeGrid operation */

  doShowTimeGrid: function(timeValue) {
    if (this.timeGrid && !this.timeGrid.isVisible()) {
      this.timeGrid.show();
      if (this.getValue()) {
        this.timeGrid.selectTimeInList(timeValue);
      }
    }
  },
  doHideTimeGrid: function(timeValue) {
    var me = this;
    if (this.timeGrid) {
      setTimeout(function() {
        me.timeGrid.hide();
      }, 100);
    }
  },

  init: function() {
    var oldShowHint,
      hint,
      formatDefinition = OB.Utilities.getTimeFormatDefinition();

    this.timeFormat = formatDefinition.timeFormat;

    this.Super('init', arguments);

    if (this.items && this.items[0]) {
      // Since Smartclient 10.0d, there is a TextItem inside the TimeItem
      // which is the one that at least renders the value
      if (this.isAbsoluteTime) {
        this.items[0].isAbsoluteTime = true;
      }
    }

    if (this.showTimeGrid && this.form && !this.timeGrid) {
      oldShowHint = this.showHint;
      this.showHint = true;
      hint = this.getHint();
      this.showHint = oldShowHint;
      this.timeGridProps = this.timeGridProps || {};
      this.timeGrid = isc.OBTimeItemGrid.create(
        isc.addProperties(
          {
            formItem: this,
            timeFormat: hint || this.timeFormat,
            is24hTime: formatDefinition.is24h
          },
          this.timeGridProps
        )
      );
      this.form.addChild(this.timeGrid); // Added grid in the form to avoid position problems
    }
  },

  keyDown: function() {
    if (this.timeGrid) {
      if (
        isc.EH.getKey() === 'Arrow_Up' &&
        (!isc.EH.ctrlKeyDown() &&
          !isc.EH.altKeyDown() &&
          !isc.EH.shiftKeyDown()) &&
        this.timeGrid.isVisible()
      ) {
        this.timeGrid.selectPreviousRecord();
      } else if (
        isc.EH.getKey() === 'Arrow_Down' &&
        (!isc.EH.ctrlKeyDown() &&
          !isc.EH.altKeyDown() &&
          !isc.EH.shiftKeyDown()) &&
        this.timeGrid.isVisible()
      ) {
        this.timeGrid.selectNextRecord();
      } else {
        this.timeGrid.hide();
      }
    }
  },

  click: function() {
    var selectedDate = isc.Time.parseInput(this.getEnteredValue());
    if (this.isAbsoluteTime) {
      selectedDate = OB.Utilities.Date.addTimezoneOffset(selectedDate);
    }
    this.doShowTimeGrid(selectedDate);
  },

  focus: function() {
    var selectedDate = this.getValue();
    if (this.isAbsoluteTime) {
      selectedDate = OB.Utilities.Date.addTimezoneOffset(selectedDate);
    }
    this.doShowTimeGrid(selectedDate);
    return this.Super('focus', arguments);
  },
  blur: function() {
    this.doHideTimeGrid();
    return this.Super('blur', arguments);
  },
  moved: function() {
    if (this.timeGrid) {
      this.timeGrid.updatePosition();
    }
    return this.Super('moved', arguments);
  },
  formSaved: function(request, response, data) {
    var UTCOffsetInMiliseconds;
    if (data && this.getValue() !== data[this.name]) {
      // it has not been converted to the local time yet, do it now
      if (data[this.name] && data[this.name].getFullYear() <= 1970) {
        UTCOffsetInMiliseconds = OB.Utilities.Date.getUTCOffsetInMiliseconds();
        data[this.name].setTime(
          data[this.name].getTime() + UTCOffsetInMiliseconds
        );
      }
      this.setValue(data[this.name]);
    }
  }
});

isc.OBTimeItem.changeDefaults('textFieldDefaults', {
  getTextBoxStyle: function() {
    // Changes in 'setDisable' in the parent item doesn't affect the text field (issue #29561)
    // With this hack, each time the text box style should be retreived, we ensure also that
    // the 'disable' state is in sync with the parent item.
    // PS: It cannot be done by overwriting 'setDisable' in the parent item, because default
    //     form states (and 'disabled: true'could be one of them), doesn't pass
    //     through 'setDisabled' function.
    var me = this;
    if (this.parentItem.isDisabled() && !this.isDisabled()) {
      // Timeout to avoid fireOnPause
      setTimeout(function() {
        me.setDisabled(true);
      }, 10);
    } else if (!this.parentItem.isDisabled() && this.isDisabled()) {
      // Timeout to avoid fireOnPause
      setTimeout(function() {
        me.setDisabled(false);
      }, 10);
    }
    // SC does not handle properly styles for inner textItem representing the time,
    // this is a temporary hack till it is fixed in SC code
    //   see issue #27670
    return (
      this.parentItem.textBoxStyle +
      (this.isDisabled() ? 'Disabled' : this.required ? 'Required' : '')
    );
  }
});

isc.ClassFactory.defineClass('OBTimeItemGrid', isc.ListGrid);

isc.OBTimeItemGrid.addProperties({
  formItem: null,
  timeFormat: null,
  data: null,
  showHeader: false,
  selectionType: 'single',
  visibility: 'hidden',
  precission: 'minute',
  // Possible values are 'hour', 'minute' and 'second'
  is24hTime: true,
  minTime: '00:00:00',
  maxTime: '23:59:59',
  // Be careful with setting it as '24:00:00' since it is considered as '00:00:00' of the following day
  timeGranularity: 1800,
  // In seconds
  timeReference: '00:00:00',
  showDiffText: null,
  timeLabels: null,
  maxTimeStringLength: 0,
  _avoidHideOnBlur: false,
  _waitingForReFocus: [],

  dateObjToTimeString: function(dateObj) {
    var tmpString,
      isPM = false,
      dateString = '';
    if (
      this.precission === 'hour' ||
      this.precission === 'minute' ||
      this.precission === 'second'
    ) {
      tmpString = dateObj.getHours();
      if (!this.is24hTime && tmpString - 12 >= 0) {
        tmpString = tmpString - 12;
        isPM = true;
      }
      if (!this.is24hTime && tmpString === 0) {
        tmpString = 12;
      }
      tmpString = tmpString.toString();
      if (tmpString.length < 2) {
        tmpString = '0' + tmpString;
      }
      dateString += tmpString;
    }
    if (this.precission === 'minute' || this.precission === 'second') {
      tmpString = dateObj.getMinutes();
      tmpString = tmpString.toString();
      if (tmpString.length < 2) {
        tmpString = '0' + tmpString;
      }
      dateString += ':' + tmpString;
    }
    if (this.precission === 'second') {
      tmpString = dateObj.getSeconds();
      tmpString = tmpString.toString();
      if (tmpString.length < 2) {
        tmpString = '0' + tmpString;
      }
      dateString += ':' + tmpString;
    }
    if (!this.is24hTime && isPM) {
      dateString += isc.Time.PMIndicator;
    } else if (!this.is24hTime && !isPM) {
      dateString += isc.Time.AMIndicator;
    }

    return dateString;
  },
  timeStringToDateObj: function(stringTime) {
    if (stringTime.length < 3) {
      stringTime = stringTime + ':00:00';
    } else if (stringTime.length < 6) {
      stringTime = stringTime + ':00';
    }

    if (typeof stringTime === 'string') {
      if (parseInt(stringTime.substring(0, stringTime.length - 6), 10) < 24) {
        stringTime = new Date(new Date(0).toDateString() + ' ' + stringTime);
      } else {
        stringTime = new Date(new Date(new Date(0).setDate(2)).setHours(0));
      }
    }
    return stringTime;
  },
  normalizeDateObj: function(dateObj) {
    var timeRefHrs, timeRefMins, timeRefSecs, newTimeRef;
    if (
      this.precission === 'hour' ||
      this.precission === 'minute' ||
      this.precission === 'second'
    ) {
      timeRefHrs = dateObj.getHours();
    } else {
      timeRefHrs = 0;
    }
    if (this.precission === 'minute' || this.precission === 'second') {
      timeRefMins = dateObj.getMinutes();
    } else {
      timeRefMins = 0;
    }
    if (this.precission === 'second') {
      timeRefSecs = dateObj.getSeconds();
    } else {
      timeRefSecs = 0;
    }
    newTimeRef = new Date(0);
    newTimeRef = new Date(newTimeRef.setHours(timeRefHrs));
    newTimeRef = new Date(newTimeRef.setMinutes(timeRefMins));
    newTimeRef = new Date(newTimeRef.setSeconds(timeRefSecs));
    newTimeRef = new Date(newTimeRef.setMilliseconds(0));
    return newTimeRef;
  },
  getDiffText: function(date, reference) {
    var diffMs = date - reference,
      diffHrs = (diffMs % 86400000) / 3600000,
      diffMins = ((diffMs % 86400000) % 3600000) / 60000,
      diffSecs = (((diffMs % 86400000) % 3600000) % 60000) / 1000,
      diffText = '';

    if (diffHrs >= 0) {
      diffHrs = Math.floor(diffHrs);
    } else {
      diffHrs = Math.ceil(diffHrs);
    }
    if (diffMins >= 0) {
      diffMins = Math.floor(diffMins);
    } else {
      diffMins = Math.ceil(diffMins);
    }
    if (diffSecs >= 0) {
      diffSecs = Math.floor(diffSecs);
    } else {
      diffSecs = Math.ceil(diffSecs);
    }

    if (diffHrs === 1 || diffHrs === -1) {
      diffText += diffHrs + ' ' + this.timeLabels[21];
    } else if (diffHrs || this.precission === 'hour') {
      diffText += diffHrs + ' ' + this.timeLabels[22];
    }

    if (diffText.length > 0 && diffMins) {
      diffText += ' ';
    }

    if (diffMins === 1 || diffMins === -1) {
      diffText += diffMins + ' ' + this.timeLabels[31];
    } else if (diffMins || (!diffHrs && this.precission === 'minute')) {
      diffText += diffMins + ' ' + this.timeLabels[32];
    }

    if (diffText.length > 0 && diffSecs) {
      diffText += ' ';
    }

    if (diffSecs === 1 || diffSecs === -1) {
      diffText += diffSecs + ' ' + this.timeLabels[41];
    } else if (
      diffSecs ||
      (!diffHrs && !diffMins && this.precission === 'second')
    ) {
      diffText += diffSecs + ' ' + this.timeLabels[42];
    }

    diffText = '(' + diffText + ')';

    if (this.maxTimeStringLength < diffText.length) {
      this.maxTimeStringLength = diffText.length;
    }

    return diffText;
  },
  convertTimes: function() {
    this.minTime = this.timeStringToDateObj(this.minTime);
    this.maxTime = this.timeStringToDateObj(this.maxTime);
    this.timeReference = this.timeStringToDateObj(this.timeReference);
  },
  selectTimeInList: function(time) {
    var rowNum, i;

    time = this.timeStringToDateObj(time);
    time = this.normalizeDateObj(time);

    for (i = 0; i < this.data.length; i++) {
      if (this.normalizeDateObj(this.data[i].jsTime) <= time) {
        rowNum = i;
      } else {
        break;
      }
    }
    this.scrollCellIntoView(rowNum, null, true, true);
    this.doSelectionUpdated = false;
    this.selectSingleRecord(rowNum);
    this.doSelectionUpdated = true;
  },
  doSelectionUpdated: true,
  selectionUpdated: function(record) {
    if (this.formItem && record && this.doSelectionUpdated) {
      var selectedDate = record.jsTime;
      if (this.formItem.isAbsoluteTime) {
        selectedDate = OB.Utilities.Date.substractTimezoneOffset(selectedDate);
      }
      this.formItem.setValue(selectedDate);
      this.formItem._hasChanged = true;
    }
  },

  show: function() {
    var timeRef, formItemWidth;
    if (this.isVisible()) {
      return;
    }
    if (this.formItem && this.formItem.relativeField) {
      this.formItem.eventParent.getValue(this.formItem.relativeField);
      timeRef = this.formItem.eventParent.getValue(this.formItem.relativeField);
      if (timeRef) {
        timeRef = this.normalizeDateObj(timeRef);
        this.timeReference = timeRef;
        if (this.formItem && !this.formItem.showNegativeTimes) {
          this.minTime = timeRef;
        }
        this.setData(this.generateData());
      }
    }

    if (this.precission === 'hour') {
      this.setWidth(
        3 * this.characterWidth +
          this.maxTimeStringLength * this.characterWidth +
          18
      );
    } else if (this.precission === 'minute') {
      this.setWidth(
        6 * this.characterWidth +
          this.maxTimeStringLength * this.characterWidth +
          18
      );
    } else if (this.precission === 'second') {
      this.setWidth(
        9 * this.characterWidth +
          this.maxTimeStringLength * this.characterWidth +
          18
      );
    }
    if (this.formItem) {
      formItemWidth = this.formItem.getVisibleWidth();
      if (formItemWidth && formItemWidth - 2 > this.getWidth()) {
        this.setWidth(formItemWidth - 2);
      }
    }

    this.updatePosition();
    return this.Super('show', arguments);
  },
  scrolled: function() {
    var me = this;
    if (isc.Browser.isIE) {
      //To avoid a problem in IE that once the scroll is pressed, the formItem loses the focus
      this._avoidHideOnBlur = true;
      this._waitingForReFocus.push('dummy');
      setTimeout(function() {
        me.formItem.form.focus();
      }, 10);
      setTimeout(function() {
        me._waitingForReFocus.pop();
        if (me._waitingForReFocus.length === 0) {
          me._avoidHideOnBlur = false;
        }
      }, 150);
    }
    this.Super('scrolled', arguments);
  },
  hide: function() {
    if (!this._avoidHideOnBlur) {
      return this.Super('hide', arguments);
    }
  },
  generateData: function() {
    var dateObj,
      timeGranularityInMilliSeconds,
      timeRef,
      dateArray = [];
    this.convertTimes();
    this.maxTimeStringLength = 0;
    timeRef = this.timeReference;

    if (this.precission === 'second') {
      timeGranularityInMilliSeconds = this.timeGranularity * 1000;
    } else if (this.precission === 'minute') {
      timeGranularityInMilliSeconds =
        Math.ceil(this.timeGranularity / 60) * 1000 * 60;
    } else if (this.precission === 'hour') {
      timeGranularityInMilliSeconds =
        Math.ceil(this.timeGranularity / (60 * 60)) * 1000 * 60 * 60;
    }

    while (this.minTime <= timeRef) {
      dateObj = {
        time:
          this.dateObjToTimeString(timeRef) +
          (this.showDiffText
            ? ' ' + this.getDiffText(timeRef, this.timeReference)
            : ''),
        jsTime: timeRef
      };
      dateArray.unshift(dateObj);
      timeRef = new Date(timeRef.getTime() - timeGranularityInMilliSeconds);
    }
    timeRef = this.timeReference;
    while (timeRef <= this.maxTime) {
      dateObj = {
        time:
          this.dateObjToTimeString(timeRef) +
          (this.showDiffText
            ? ' ' + this.getDiffText(timeRef, this.timeReference)
            : ''),
        jsTime: timeRef
      };
      if (timeRef !== this.timeReference) {
        dateArray.push(dateObj);
      }
      timeRef = new Date(timeRef.getTime() + timeGranularityInMilliSeconds);
    }
    return dateArray;
  },
  selectPreviousRecord: function() {
    var selectedRecord = this.getSelectedRecord(),
      i;
    if (selectedRecord) {
      for (i = 0; i < this.data.length; i++) {
        if (this.data[i] === selectedRecord && i !== 0) {
          this.scrollCellIntoView(i - 1, null, true, true);
          this.selectSingleRecord(i - 1);
          break;
        }
      }
    } else {
      this.scrollCellIntoView(0, null, true, true);
      this.selectSingleRecord(0);
    }
  },
  selectNextRecord: function() {
    var selectedRecord = this.getSelectedRecord(),
      i;
    if (selectedRecord) {
      for (i = 0; i < this.data.length; i++) {
        if (this.data[i] === selectedRecord && i !== this.data.length - 1) {
          this.scrollCellIntoView(i + 1, null, true, true);
          this.selectSingleRecord(i + 1);
          break;
        }
      }
    } else {
      this.scrollCellIntoView(0, null, true, true);
      this.selectSingleRecord(0);
    }
  },
  updatePosition: function() {
    if (this.formItem) {
      this.placeNear(
        this.formItem.getPageLeft() + 2,
        this.formItem.getPageTop() + 26
      );
    }
  },
  initWidget: function() {
    var labels;
    if (this.timeFormat.indexOf('SS') !== -1) {
      this.precission = 'second';
    } else if (this.timeFormat.indexOf('MM') !== -1) {
      this.precission = 'minute';
    } else if (this.timeFormat.indexOf('HH') !== -1) {
      this.precission = 'hour';
    }

    if (
      this.timeFormat.toUpperCase().indexOf(isc.Time.AMIndicator) !== -1 ||
      this.timeFormat.toUpperCase().indexOf(isc.Time.PMIndicator) !== -1
    ) {
      this.is24hTime = false;
    }

    if (this.formItem && this.formItem.timeGranularity) {
      this.timeGranularity = this.formItem.timeGranularity;
    }

    if (
      this.formItem &&
      this.formItem.relativeField &&
      this.showDiffText !== false
    ) {
      this.showDiffText = true;
    }

    labels = OB.I18N.getLabel('OBUIAPP_TimeUnits');
    if (labels) {
      this.timeLabels = labels.split(',');
    }

    this.setData(this.generateData());

    return this.Super('initWidget', arguments);
  },
  fields: [
    {
      name: 'time',
      title: 'Time'
    },
    {
      name: 'jsTime',
      title: 'JS Time',
      showIf: 'false'
    }
  ]
});

isc.ClassFactory.defineClass('OBAbsoluteTimeItem', isc.OBTimeItem);

isc.OBAbsoluteTimeItem.addProperties({
  isAbsoluteTime: true
});
