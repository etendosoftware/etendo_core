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
 * All portions are Copyright (C) 2009-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

window.OB = window.OB || {};
OB.Utilities = window.OB.Utilities || {};

// = Openbravo Date Utilities =
// Defines utility methods related to handling date, incl. formatting.
OB.Utilities.Date = {};
// ** {{{ OB.Utilities.Date.centuryReference }}} **
// For a two-digit year display format, it establishes where is the frontier
// between the 20th and the 21st century
// The range is taken between 1900+centuryReference and 2100-centuryReference-1
OB.Utilities.Date.centuryReference = 50;
// ** {{{ OB.Utilities.Date.normalizeDisplayFormat }}} **
// Repairs the displayFormat definition (passed in as a parameter) to a value
// expected by the rest of the system. For example mm is replaced by MM,
// dd is replacecd by DD, YYYY to %Y.
//
// Parameters:
// * {{{displayFormat}}}: the string displayFormat definition to repair.
OB.Utilities.Date.normalizeDisplayFormat = function(displayFormat) {
  var newFormat = '';
  displayFormat = displayFormat
    .replace('mm', 'MM')
    .replace('dd', 'DD')
    .replace('yyyy', 'YYYY')
    .replace('yy', 'YY');
  displayFormat = displayFormat.replace('%D', '%d').replace('%M', '%m');
  if (displayFormat !== null && displayFormat !== '') {
    newFormat = displayFormat;
    newFormat = newFormat.replace('YYYY', '%Y');
    newFormat = newFormat.replace('YY', '%y');
    newFormat = newFormat.replace('MM', '%m');
    newFormat = newFormat.replace('DD', '%d');
    newFormat = newFormat.substring(0, 8);
  }
  displayFormat = displayFormat
    .replace('hh', 'HH')
    .replace('HH24', 'HH')
    .replace('mi', 'MI')
    .replace('ss', 'SS');
  displayFormat = displayFormat
    .replace('%H', 'HH')
    .replace('HH:%m', 'HH:MI')
    .replace('HH.%m', 'HH.MI')
    .replace('%S', 'SS');
  displayFormat = displayFormat
    .replace('HH:mm', 'HH:MI')
    .replace('HH.mm', 'HH.MI');
  displayFormat = displayFormat
    .replace('HH:MM', 'HH:MI')
    .replace('HH.MM', 'HH.MI');
  if (displayFormat.indexOf(' HH:MI:SS') !== -1) {
    newFormat += ' %H:%M:%S';
  } else if (displayFormat.indexOf(' HH:MI') !== -1) {
    newFormat += ' %H:%M';
  } else if (displayFormat.indexOf(' HH.MI.SS') !== -1) {
    newFormat += ' %H.%M.%S';
  } else if (displayFormat.indexOf(' HH.MI') !== -1) {
    newFormat += ' %H.%M';
  }
  if (displayFormat.indexOf(' a') !== -1) {
    newFormat += ' A';
  }
  return newFormat;
};

//** {{{ OB.Utilities.getTimeFormat }}} **
//
// Returns an object with the timeformatter, a boolean if 24 hours
// time clock are being used and the timeformat itself
OB.Utilities.getTimeFormatDefinition = function() {
  var timeFormat,
    is24h = true;
  if (OB.Format.dateTime.indexOf(' ') === -1) {
    return 'to24HourTime';
  }

  timeFormat = OB.Format.dateTime.substring(
    OB.Format.dateTime.indexOf(' ') + 1
  );

  if (
    timeFormat &&
    timeFormat.toUpperCase().lastIndexOf(' A') !== -1 &&
    timeFormat.toUpperCase().lastIndexOf(' A') === timeFormat.length - 2
  ) {
    is24h = false;
  }

  const format = timeFormat.toLowerCase();
  const toTime = format.contains
    ? format.contains('ss')
    : format.includes('ss');
  if (toTime) {
    return {
      timeFormat: timeFormat,
      is24h: is24h,
      timeFormatter: is24h ? 'to24HourTime' : 'toTime'
    };
  }
  return {
    timeFormat: timeFormat,
    is24h: is24h,
    timeFormatter: is24h ? 'toShort24HourTime' : 'toShortTime'
  };
};

// ** {{{ OB.Utilities.Date.OBToJS }}} **
//
// Converts a String to a Date object.
//
// Parameters:
// * {{{OBDate}}}: the date string to convert
// * {{{dateFormat}}}: the dateFormat pattern to use
// Return:
// * a Date object or null if conversion was not possible.
OB.Utilities.Date.OBToJS = function(OBDate, dateFormat) {
  if (!OBDate) {
    return null;
  }

  // if already a date then return true
  var isADate = Object.prototype.toString.call(OBDate) === '[object Date]',
    PMIndicator = ' PM',
    AMIndicator = ' AM',
    is24h = true,
    isPM = false;
  if (isADate) {
    return OBDate;
  }

  if (window.isc && isc.Time && isc.Time.PMIndicator) {
    PMIndicator = isc.Time.PMIndicator;
  }
  if (window.isc && isc.Time && isc.Time.AMIndicator) {
    AMIndicator = isc.Time.AMIndicator;
  }

  dateFormat = OB.Utilities.Date.normalizeDisplayFormat(dateFormat);
  dateFormat = dateFormat.replace(' A', '');

  var isFullYear = dateFormat.indexOf('%Y') !== -1;
  if (
    OBDate.indexOf(PMIndicator) !== -1 ||
    OBDate.indexOf(AMIndicator) !== -1
  ) {
    is24h = false;
  }
  if (!is24h && OBDate.indexOf(PMIndicator) !== -1) {
    isPM = true;
  }
  OBDate = OBDate.replace(AMIndicator, '').replace(PMIndicator, '');

  if ((isFullYear ? OBDate.length - 2 : OBDate.length) !== dateFormat.length) {
    return null;
  }
  if (isFullYear) {
    dateFormat = dateFormat.replace('%Y', '%YYY');
  }

  if (dateFormat.indexOf('-') !== -1 && OBDate.indexOf('-') === -1) {
    return null;
  } else if (dateFormat.indexOf('/') !== -1 && OBDate.indexOf('/') === -1) {
    return null;
  } else if (dateFormat.indexOf(':') !== -1 && OBDate.indexOf(':') === -1) {
    return null;
  } else if (dateFormat.indexOf('.') !== -1 && OBDate.indexOf('.') === -1) {
    return null;
  }

  var year =
    dateFormat.indexOf('%y') !== -1
      ? OBDate.substring(dateFormat.indexOf('%y'), dateFormat.indexOf('%y') + 2)
      : 0;
  var fullYear =
    dateFormat.indexOf('%Y') !== -1
      ? OBDate.substring(dateFormat.indexOf('%Y'), dateFormat.indexOf('%Y') + 4)
      : 0;
  var month =
    dateFormat.indexOf('%m') !== -1
      ? OBDate.substring(dateFormat.indexOf('%m'), dateFormat.indexOf('%m') + 2)
      : 0;
  var day =
    dateFormat.indexOf('%d') !== -1
      ? OBDate.substring(dateFormat.indexOf('%d'), dateFormat.indexOf('%d') + 2)
      : 0;
  var hours =
    dateFormat.indexOf('%H') !== -1
      ? OBDate.substring(dateFormat.indexOf('%H'), dateFormat.indexOf('%H') + 2)
      : 0;
  var minutes =
    dateFormat.indexOf('%M') !== -1
      ? OBDate.substring(dateFormat.indexOf('%M'), dateFormat.indexOf('%M') + 2)
      : 0;
  var seconds =
    dateFormat.indexOf('%S') !== -1
      ? OBDate.substring(dateFormat.indexOf('%S'), dateFormat.indexOf('%S') + 2)
      : 0;

  // Check that really all date parts (if they are present) are numbers
  var digitRegExp = ['^\\d+$', 'gm'];
  if (
    (year && !new RegExp(digitRegExp[0], digitRegExp[1]).test(year)) ||
    (fullYear && !new RegExp(digitRegExp[0], digitRegExp[1]).test(fullYear)) ||
    (month && !new RegExp(digitRegExp[0], digitRegExp[1]).test(month)) ||
    (day && !new RegExp(digitRegExp[0], digitRegExp[1]).test(day)) ||
    (hours && !new RegExp(digitRegExp[0], digitRegExp[1]).test(hours)) ||
    (minutes && !new RegExp(digitRegExp[0], digitRegExp[1]).test(minutes)) ||
    (seconds && !new RegExp(digitRegExp[0], digitRegExp[1]).test(seconds))
  ) {
    return null;
  }

  month = parseInt(month, 10);
  day = parseInt(day, 10);
  hours = parseInt(hours, 10);
  minutes = parseInt(minutes, 10);
  seconds = parseInt(seconds, 10);

  if (!is24h) {
    if (!isPM && hours === 12) {
      hours = 0;
    }
    if (isPM && hours !== 12) {
      hours = hours + 12;
    }
  }

  if (
    day < 1 ||
    day > 31 ||
    month < 1 ||
    month > 12 ||
    year > 99 ||
    fullYear > 9999
  ) {
    return null;
  }

  if (hours > 23 || minutes > 59 || seconds > 59) {
    return null;
  }

  // alert('year: ' + year + '\n' + 'fullYear: ' + fullYear + '\n' + 'month: ' +
  // month + '\n' + 'day: ' + day + '\n' + 'hours: ' + hours + '\n' + 'minutes:
  // ' + minutes + '\n' + 'seconds: ' + seconds);
  // var JSDate = isc.Date.create(); /**It doesn't work in IE**/
  var JSDate = new Date();
  var centuryReference = OB.Utilities.Date.centuryReference;
  if (!isFullYear) {
    if (parseInt(year, 10) < centuryReference) {
      fullYear = '20' + year;
    } else {
      fullYear = '19' + year;
    }
  }

  fullYear = parseInt(fullYear, 10);
  JSDate.setFullYear(fullYear, month - 1, day);

  //  https://issues.openbravo.com/view.php?id=22505
  if (day !== JSDate.getDate()) {
    return null;
  }

  JSDate.setHours(hours);
  JSDate.setMinutes(minutes);
  JSDate.setSeconds(seconds);
  JSDate.setMilliseconds(0);
  if (JSDate.toString() === 'Invalid Date' || JSDate.toString() === 'NaN') {
    return null;
  } else {
    return JSDate;
  }
};

// ** {{{ OB.Utilities.Date.JSToOB }}} **
//
// Converts a Date to a String
//
// Parameters:
// * {{{JSDate}}}: the javascript Date object
// * {{{dateFormat}}}: the dateFormat pattern to use
// Return:
// * a String or null if the JSDate is not a date.
OB.Utilities.Date.JSToOB = function(JSDate, dateFormat) {
  dateFormat = OB.Utilities.Date.normalizeDisplayFormat(dateFormat);

  var isADate = Object.prototype.toString.call(JSDate) === '[object Date]',
    PMIndicator = ' PM',
    AMIndicator = ' AM',
    is24h = true,
    isPM = false;
  if (!isADate) {
    return null;
  }

  if (window.isc && isc.Time && isc.Time.PMIndicator) {
    PMIndicator = isc.Time.PMIndicator;
  }
  if (window.isc && isc.Time && isc.Time.AMIndicator) {
    AMIndicator = isc.Time.AMIndicator;
  }
  if (
    dateFormat.toUpperCase().lastIndexOf(' A') !== -1 &&
    dateFormat.toUpperCase().lastIndexOf(' A') === dateFormat.length - 2
  ) {
    is24h = false;
  }

  var year = JSDate.getYear().toString();
  var fullYear = JSDate.getFullYear().toString();
  var month = (JSDate.getMonth() + 1).toString();
  var day = JSDate.getDate().toString();
  var hours = JSDate.getHours().toString();
  var minutes = JSDate.getMinutes().toString();
  var seconds = JSDate.getSeconds().toString();

  var centuryReference = OB.Utilities.Date.centuryReference;
  if (dateFormat.indexOf('%y') !== -1) {
    if (
      parseInt(fullYear, 10) >= 1900 + centuryReference &&
      parseInt(fullYear, 10) < 2100 - centuryReference
    ) {
      if (parseInt(year, 10) >= 100) {
        year = parseInt(year, 10) - 100;
        year = year.toString();
      }
    } else {
      return null;
    }
  }

  if (!is24h) {
    hours = parseInt(hours, 10);
    if (hours >= 12) {
      isPM = true;
    }
    if (hours > 12) {
      hours = hours - 12;
    }
    if (hours === 0) {
      hours = 12;
    }
    hours = hours.toString();
  }

  while (year.length < 2) {
    year = '0' + year;
  }
  while (fullYear.length < 4) {
    fullYear = '0' + fullYear;
  }
  while (month.length < 2) {
    month = '0' + month;
  }
  while (day.length < 2) {
    day = '0' + day;
  }
  while (hours.length < 2) {
    hours = '0' + hours;
  }
  while (minutes.length < 2) {
    minutes = '0' + minutes;
  }
  while (seconds.length < 2) {
    seconds = '0' + seconds;
  }
  var OBDate = dateFormat;
  OBDate = OBDate.replace('%y', year);
  OBDate = OBDate.replace('%Y', fullYear);
  OBDate = OBDate.replace('%m', month);
  OBDate = OBDate.replace('%d', day);
  OBDate = OBDate.replace('%H', hours);
  OBDate = OBDate.replace('%M', minutes);
  OBDate = OBDate.replace('%S', seconds);

  if (!is24h) {
    if (isPM) {
      OBDate = OBDate.replace(' A', PMIndicator);
    } else {
      OBDate = OBDate.replace(' A', AMIndicator);
    }
  }

  return OBDate;
};

//** {{{ OB.Utilities.Date.getTimeFields }}} **
//
// Returns an array with the names of the time fields.
//
// Parameters:
// * {{{allFields}}}: complete list of fields
// Return:
// * an array with the names of the time fields contained in allFields.
OB.Utilities.Date.getTimeFields = function(allFields) {
  var i,
    field,
    timeFields = [],
    length = allFields.length;
  for (i = 0; i < length; i++) {
    field = allFields[i];
    if (field.type === '_id_24') {
      timeFields.push(field);
    }
  }
  return timeFields;
};

//** {{{ OB.Utilities.Date.convertUTCTimeToLocalTime }}} **
//
// Converts the value of time fields from UTC to local time
//
// Parameters:
// * {{{newData}}}: records to be converted
// * {{{allFields}}}: array with the fields of the records
// Return:
// * Nothing. newData, after converting its time fields from UTC timezone the the client side timezone
OB.Utilities.Date.convertUTCTimeToLocalTime = function(newData, allFields) {
  var textField,
    fieldToDate,
    i,
    j,
    UTCOffsetInMiliseconds = OB.Utilities.Date.getUTCOffsetInMiliseconds(),
    timeFields = OB.Utilities.Date.getTimeFields(allFields),
    timeFieldsLength = timeFields.length,
    convertedData = isc.clone(newData),
    convertedDataLength = convertedData.length;
  for (i = 0; i < timeFieldsLength; i++) {
    for (j = 0; j < convertedDataLength; j++) {
      textField = convertedData[j][timeFields[i].name];
      if (!textField) {
        continue;
      }
      if (isc.isA.String(textField)) {
        fieldToDate = isc.Time.parseInput(textField);
      } else if (isc.isA.Date(textField)) {
        fieldToDate = textField;
      }
      fieldToDate.setTime(fieldToDate.getTime() + UTCOffsetInMiliseconds);
      convertedData[j][timeFields[i].name] =
        fieldToDate.getHours() +
        ':' +
        fieldToDate.getMinutes() +
        ':' +
        fieldToDate.getSeconds();
    }
  }
  return convertedData;
};

//** {{{ OB.Utilities.Date.addTimezoneOffset }}} **
//
// Adds to a date its timezone offset
//
// Parameters:
// * {{{date}}}: date in which it be added its timezone offset
OB.Utilities.Date.addTimezoneOffset = function(date) {
  var newDate, originalTimezoneOffset, newTimezoneOffset;

  if (Object.prototype.toString.call(date) !== '[object Date]') {
    return date;
  }
  originalTimezoneOffset = date.getTimezoneOffset();
  newDate = new Date(date.getTime() + originalTimezoneOffset * 60000);
  newTimezoneOffset = newDate.getTimezoneOffset();
  // Apply a correction if the timezone offset has changed
  if (originalTimezoneOffset !== newTimezoneOffset) {
    newDate = new Date(
      newDate.getTime() + (newTimezoneOffset - originalTimezoneOffset) * 60000
    );
  }
  return newDate;
};

//** {{{ OB.Utilities.Date.substractTimezoneOffset }}} **
//
// Substracts to a date its timezone offset
//
// Parameters:
// * {{{date}}}: date in which it be substracted its timezone offset
OB.Utilities.Date.substractTimezoneOffset = function(date) {
  var newDate;

  if (Object.prototype.toString.call(date) !== '[object Date]') {
    return date;
  }
  newDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return newDate;
};

//** {{{ OB.Utilities.Date.getUTCOffsetInMiliseconds }}} **
//
// Return the offset with UTC measured in miliseconds
OB.Utilities.Date.getUTCOffsetInMiliseconds = function() {
  var UTCHourOffset = isc.Time.getUTCHoursDisplayOffset(new Date()),
    UTCMinuteOffset = isc.Time.getUTCMinutesDisplayOffset(new Date());
  return UTCHourOffset * 60 * 60 * 1000 + UTCMinuteOffset * 60 * 1000;
};

//** {{{ OB.Utilities.Date.roundToNextQuarter }}} **
//
// Round any date to the next quarter
OB.Utilities.Date.roundToNextQuarter = function(date) {
  var newDate = new Date(date),
    minutes = newDate.getMinutes(),
    timeBreak = 15;
  if (
    newDate.getMilliseconds() === 0 &&
    newDate.getSeconds() === 0 &&
    minutes % timeBreak === 0
  ) {
    return newDate;
  }
  var roundedMinutes =
    (parseInt((minutes + timeBreak) / timeBreak, 10) * timeBreak) % 60;
  newDate.setMilliseconds(0);
  newDate.setSeconds(0);
  newDate.setMinutes(roundedMinutes);
  if (roundedMinutes === 0) {
    newDate.setHours(newDate.getHours() + 1);
  }
  return newDate;
};

//** {{{ OB.Utilities.Date.roundToNextHalfHour }}} **
//
// Round any date to the next half hour
OB.Utilities.Date.roundToNextHalfHour = function(date) {
  var newDate = new Date(date),
    minutes = newDate.getMinutes(),
    timeBreak = 30;
  if (
    newDate.getMilliseconds() === 0 &&
    newDate.getSeconds() === 0 &&
    minutes % timeBreak === 0
  ) {
    return newDate;
  }
  var roundedMinutes =
    (parseInt((minutes + timeBreak) / timeBreak, 10) * timeBreak) % 60;
  newDate.setMilliseconds(0);
  newDate.setSeconds(0);
  newDate.setMinutes(roundedMinutes);
  if (roundedMinutes === 0) {
    newDate.setHours(newDate.getHours() + 1);
  }
  return newDate;
};

//** {{{ OB.Utilities.Date.getDateSeparator }}} **
//
// Returns the date separator
OB.Utilities.Date.getDateSeparator = function(dateFormat) {
  // obtains the date separator by selecting the first characters that is not 'D', 'M' or 'Y'
  return dateFormat
    .toUpperCase()
    .replace(/D/g, '')
    .replace(/M/g, '')
    .replace(/Y/g, '')
    .substr(0, 1);
};
