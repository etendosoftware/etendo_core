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
 * All portions are Copyright (C) 2011-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB = window.OB || {};
OB.Utilities = window.OB.Utilities || {};

// = Openbravo Number Utilities =
// Defines utility methods related to handling numbers on the client, for
// example formatting.
OB.Utilities.Number = {};

// ** {{{ OB.Utilities.Number.roundJSNumber }}} **
//
// Function that rounds a JS number to a given decimal number
//
// Parameters:
// * {{{num}}}: the JS number
// * {{{dec}}}: the JS number of decimals
// Return:
// * The rounded JS number
OB.Utilities.Number.roundJSNumber = function(num, dec) {
  var strNum;
  if (isNaN(num)) {
    return NaN;
  }
  strNum = typeof num === 'string' ? num : String(num);
  return Number(
    new BigDecimal(strNum).setScale(dec, BigDecimal.prototype.ROUND_HALF_UP)
  );
};

// ** {{{ OB.Utilities.Number.OBMaskedToOBPlain }}} **
//
// Function that returns a plain OB number just with the decimal Separator
//
// Parameters:
// * {{{number}}}: the formatted OB number
// * {{{decSeparator}}}: the decimal separator of the OB number
// * {{{groupSeparator}}}: the group separator of the OB number
// Return:
// * The plain OB number
OB.Utilities.Number.OBMaskedToOBPlain = function(
  number,
  decSeparator,
  groupSeparator
) {
  number = number.toString();
  var plainNumber = number,
    decimalNotation = number.indexOf('E') === -1 && number.indexOf('e') === -1;

  // Remove group separators
  if (groupSeparator) {
    var groupRegExp = new RegExp('\\' + groupSeparator, 'g');
    plainNumber = plainNumber.replace(groupRegExp, '');
  }

  //Check if the number is not on decimal notation
  if (!decimalNotation) {
    plainNumber = OB.Utilities.Number.ScientificToDecimal(number, decSeparator);
  }

  // Catch sign
  var numberSign = '';
  if (plainNumber.substring(0, 1) === '+') {
    numberSign = '';
    plainNumber = plainNumber.substring(1, plainNumber.length);
  } else if (plainNumber.substring(0, 1) === '-') {
    numberSign = '-';
    plainNumber = plainNumber.substring(1, plainNumber.length);
  }

  // Remove ending decimal '0'
  if (plainNumber.indexOf(decSeparator) !== -1) {
    while (
      plainNumber.substring(plainNumber.length - 1, plainNumber.length) === '0'
    ) {
      plainNumber = plainNumber.substring(0, plainNumber.length - 1);
    }
  }

  // Remove starting integer '0'
  while (
    plainNumber.substring(0, 1) === '0' &&
    plainNumber.substring(1, 2) !== decSeparator &&
    plainNumber.length > 1
  ) {
    plainNumber = plainNumber.substring(1, plainNumber.length);
  }

  // Remove decimal separator if is the last character
  if (
    plainNumber.substring(plainNumber.length - 1, plainNumber.length) ===
    decSeparator
  ) {
    plainNumber = plainNumber.substring(0, plainNumber.length - 1);
  }

  // Re-set sign
  if (plainNumber !== '0') {
    plainNumber = numberSign + plainNumber;
  }

  // Return plain number
  return plainNumber;
};

// ** {{{ OB.Utilities.Number.OBPlainToOBMasked }}} **
//
// Function that transform a OB plain number into a OB formatted one (by
// applying a mask).
//
// Parameters:
// * {{{number}}}: The OB plain number
// * {{{maskNumeric}}}: The numeric mask of the OB number
// * {{{decSeparator}}}: The decimal separator of the OB number
// * {{{groupSeparator}}}: The group separator of the OB number
// * {{{groupInterval}}}: The group interval of the OB number
// Return:
// * The OB formatted number.
OB.Utilities.Number.OBPlainToOBMasked = function(
  number,
  maskNumeric,
  decSeparator,
  groupSeparator,
  groupInterval
) {
  if (number === '' || number === null || number === undefined) {
    return number;
  }

  if (groupInterval === null || groupInterval === undefined) {
    groupInterval = OB.Format.defaultGroupingSize;
  }
  // Management of the mask
  if (maskNumeric.indexOf('+') === 0 || maskNumeric.indexOf('-') === 0) {
    maskNumeric = maskNumeric.substring(1, maskNumeric.length);
  }
  if (
    groupSeparator &&
    maskNumeric.indexOf(groupSeparator) !== -1 &&
    maskNumeric.indexOf(decSeparator) !== -1 &&
    maskNumeric.indexOf(groupSeparator) > maskNumeric.indexOf(decSeparator)
  ) {
    var fixRegExp = new RegExp('\\' + groupSeparator, 'g');
    maskNumeric = maskNumeric.replace(fixRegExp, '');
  }
  var maskLength = maskNumeric.length;
  var decMaskPosition = maskNumeric.indexOf(decSeparator);
  if (decMaskPosition === -1) {
    decMaskPosition = maskLength;
  }
  var intMask = maskNumeric.substring(0, decMaskPosition);
  var decMask = maskNumeric.substring(decMaskPosition + 1, maskLength);

  if (
    (groupSeparator && decMask.indexOf(groupSeparator) !== -1) ||
    decMask.indexOf(decSeparator) !== -1
  ) {
    if (groupSeparator) {
      var fixRegExp_1 = new RegExp('\\' + groupSeparator, 'g');
      decMask = decMask.replace(fixRegExp_1, '');
    }
    var fixRegExp_2 = new RegExp('\\' + decSeparator, 'g');
    decMask = decMask.replace(fixRegExp_2, '');
  }

  // Management of the number
  number = number.toString();
  number = OB.Utilities.Number.OBMaskedToOBPlain(
    number,
    decSeparator,
    groupSeparator
  );
  var numberSign = '';
  if (number.substring(0, 1) === '+') {
    numberSign = '';
    number = number.substring(1, number.length);
  } else if (number.substring(0, 1) === '-') {
    numberSign = '-';
    number = number.substring(1, number.length);
  }

  // //Splitting the number
  var formattedNumber = '';
  var numberLength = number.length;
  var decPosition = number.indexOf(decSeparator);
  if (decPosition === -1) {
    decPosition = numberLength;
  }
  var intNumber = number.substring(0, decPosition);
  var decNumber = number.substring(decPosition + 1, numberLength);

  // //Management of the decimal part
  if (decNumber.length > decMask.length) {
    decNumber = '0.' + decNumber;
    decNumber = OB.Utilities.Number.roundJSNumber(decNumber, decMask.length);
    if (isNaN(decNumber)) {
      return number;
    }
    decNumber = decNumber.toString();

    // Check if the number is on Scientific notation
    if (decNumber.indexOf('e') !== -1 || decNumber.indexOf('E') !== -1) {
      decNumber = OB.Utilities.Number.ScientificToDecimal(
        decNumber,
        decSeparator
      );
    }
    if (decNumber.substring(0, 1) === '1') {
      intNumber = parseFloat(intNumber);
      intNumber = intNumber + 1;
      intNumber = intNumber.toString();
    }
    decNumber = decNumber.substring(2, decNumber.length);
  }

  if (decNumber.length < decMask.length) {
    var decNumber_temp = '',
      decMaskLength = decMask.length,
      i;
    for (i = 0; i < decMaskLength; i++) {
      if (decMask.substring(i, i + 1) === '#') {
        if (decNumber.substring(i, i + 1) !== '') {
          decNumber_temp = decNumber_temp + decNumber.substring(i, i + 1);
        }
      } else if (decMask.substring(i, i + 1) === '0') {
        if (decNumber.substring(i, i + 1) !== '') {
          decNumber_temp = decNumber_temp + decNumber.substring(i, i + 1);
        } else {
          decNumber_temp = decNumber_temp + '0';
        }
      }
    }
    decNumber = decNumber_temp;
  }

  // Management of the integer part
  var isGroup = false;

  if (groupSeparator) {
    if (intMask.indexOf(groupSeparator) !== -1) {
      isGroup = true;
    }

    var groupRegExp = new RegExp('\\' + groupSeparator, 'g');
    intMask = intMask.replace(groupRegExp, '');
  }

  var intNumber_temp;
  if (intNumber.length < intMask.length) {
    intNumber_temp = '';
    var diff = intMask.length - intNumber.length,
      j;
    for (j = intMask.length; j > 0; j--) {
      if (intMask.substring(j - 1, j) === '#') {
        if (intNumber.substring(j - 1 - diff, j - diff) !== '') {
          intNumber_temp =
            intNumber.substring(j - 1 - diff, j - diff) + intNumber_temp;
        }
      } else if (intMask.substring(j - 1, j) === '0') {
        if (intNumber.substring(j - 1 - diff, j - diff) !== '') {
          intNumber_temp =
            intNumber.substring(j - 1 - diff, j - diff) + intNumber_temp;
        } else {
          intNumber_temp = '0' + intNumber_temp;
        }
      }
    }
    intNumber = intNumber_temp;
  }

  if (isGroup === true) {
    intNumber_temp = '';
    var groupCounter = 0,
      k;
    for (k = intNumber.length; k > 0; k--) {
      intNumber_temp = intNumber.substring(k - 1, k) + intNumber_temp;
      groupCounter++;
      if (groupCounter.toString() === groupInterval.toString() && k !== 1) {
        groupCounter = 0;
        intNumber_temp = groupSeparator + intNumber_temp;
      }
    }
    intNumber = intNumber_temp;
  }

  // Building the final number
  if (intNumber === '' && decNumber !== '') {
    intNumber = '0';
  }

  formattedNumber = numberSign + intNumber;
  if (decNumber !== '') {
    formattedNumber += decSeparator + decNumber;
  }
  return formattedNumber;
};

// ** {{{ OB.Utilities.Number.OBMaskedToJS }}} **
//
// Function that returns a JS number just with the decimal separator which
// always is '.'. It is used for math operations
//
// Parameters:
// * {{{number}}}: The OB formatted (or plain) number
// * {{{decSeparator}}}: The decimal separator of the OB number
// * {{{groupSeparator}}}: The group separator of the OB number
// Return:
// * The JS number.
OB.Utilities.Number.OBMaskedToJS = function(
  numberStr,
  decSeparator,
  groupSeparator
) {
  if (!numberStr || numberStr.trim() === '') {
    return null;
  }
  var calcNumber = OB.Utilities.Number.OBMaskedToOBPlain(
    numberStr,
    decSeparator,
    groupSeparator
  );
  /* Remove hidden character when copying from windows calculator
   * See issue #43483
   */
  if (calcNumber.indexOf('\u202C') !== -1) {
    calcNumber = calcNumber.replace('\u202C', '');
  }
  calcNumber = calcNumber.replace(decSeparator, '.');
  var numberResult = Number(calcNumber);
  if (isNaN(numberResult)) {
    return numberStr;
  }
  return numberResult;
};

// ** {{{ OB.Utilities.Number.JSToOBMasked }}} **
//
// Function that returns a OB formatted number given as input a JS number just
// with the decimal separator which always is '.'
//
// Parameters:
// * {{{number}}}: The JS number
// * {{{maskNumeric}}}: The numeric mask of the OB number
// * {{{decSeparator}}}: The decimal separator of the OB number
// * {{{groupSeparator}}}: The group separator of the OB number
// * {{{groupInterval}}}: The group interval of the OB number
// Return:
// * The OB formatted number.
OB.Utilities.Number.JSToOBMasked = function(
  number,
  maskNumeric,
  decSeparator,
  groupSeparator,
  groupInterval
) {
  var isANumber = Object.prototype.toString.call(number) === '[object Number]';
  if (!isANumber) {
    return number;
  }
  var formattedNumber = number;
  formattedNumber = formattedNumber.toString();
  formattedNumber = formattedNumber.replace('.', decSeparator);
  formattedNumber = OB.Utilities.Number.OBPlainToOBMasked(
    formattedNumber,
    maskNumeric,
    decSeparator,
    groupSeparator,
    groupInterval
  );
  return formattedNumber;
};

OB.Utilities.Number.IsValidValueString = function(type, numberStr) {
  var maskNumeric = type.maskNumeric;
  // note 0 is also okay to return true
  if (!numberStr) {
    return true;
  }

  var bolNegative = true;
  if (maskNumeric.indexOf('+') === 0) {
    bolNegative = false;
    maskNumeric = maskNumeric.substring(1, maskNumeric.length);
  }

  var bolDecimal = true;
  if (maskNumeric.indexOf(type.decSeparator) === -1) {
    bolDecimal = false;
  }
  var checkPattern = '';
  checkPattern += '^';
  if (bolNegative) {
    checkPattern += '([+]|[-])?';
  }
  checkPattern +=
    '(\\d+)?((\\' +
    type.groupSeparator +
    '\\d{' +
    OB.Format.defaultGroupingSize +
    '})?)+';
  if (bolDecimal) {
    checkPattern += '(\\' + type.decSeparator + '\\d+)?';
  }
  checkPattern += '$';
  var checkRegExp = new RegExp(checkPattern);
  if (
    numberStr.match(checkRegExp) &&
    numberStr.substring(0, 1) !== type.groupSeparator
  ) {
    return true;
  }
  return false;
};

OB.Utilities.Number.Grouping = {
  getGroupingModes: function() {
    return this.groupingModes;
  },
  groupingModes: {
    byDecimal10: OB.I18N.getLabel('OBUIAPP_GroupByDecimal10'),
    by1: OB.I18N.getLabel('OBUIAPP_GroupBy1'),
    by10: OB.I18N.getLabel('OBUIAPP_GroupBy10'),
    by100: OB.I18N.getLabel('OBUIAPP_GroupBy100'),
    by1000: OB.I18N.getLabel('OBUIAPP_GroupBy1000'),
    by10000: OB.I18N.getLabel('OBUIAPP_GroupBy10000'),
    by100000: OB.I18N.getLabel('OBUIAPP_GroupBy100000')
  },
  defaultGroupingMode: 'by10',
  //default grouping mode
  groupingMode: 'by10',
  getGroupingMultiplier: function(groupingMode) {
    switch (groupingMode) {
      case 'byDecimal10':
        return 0.1;
      case 'by1':
        return 1;
      case 'by10':
        return 10;
      case 'by100':
        return 100;
      case 'by1000':
        return 1000;
      case 'by10000':
        return 10000;
      case 'by100000':
        return 100000;
    }
    // default
    return 10;
  },
  getGroupValue: function(value, record, field, fieldName, grid) {
    var returnValue,
      groupingMode =
        field.groupingMode || OB.Utilities.Number.Grouping.defaultGroupingMode,
      multiplier = this.getGroupingMultiplier(groupingMode);

    if (!isc.isA.Number(value) || !groupingMode) {
      return value;
    }
    returnValue = value / multiplier;
    // round down
    returnValue = Math.round(returnValue - 0.49);
    returnValue = returnValue * multiplier;
    return returnValue;
  },
  getGroupTitle: function(value, record, field, fieldName, grid) {
    var groupValue = this.getGroupValue(value, record, field, fieldName, grid),
      groupingMode =
        field.groupingMode || OB.Utilities.Number.Grouping.defaultGroupingMode,
      multiplier = this.getGroupingMultiplier(groupingMode);
    return groupValue + ' - ' + (groupValue + multiplier);
  }
};

//** {{{ OB.Utilities.Number.ScientificToDecimal }}} **
//
// Convert a number from Scientific notation to decimal notation
//
// Parameters:
// * {{{number}}}: the number on scientific notation
// * {{{decSeparator}}}: the decimal separator of the OB number
// Return:
// * The OB number on decimal notation
OB.Utilities.Number.ScientificToDecimal = function(number, decSeparator) {
  number = number.toString();
  // remove leading zeros
  // see issue https://issues.openbravo.com/view.php?id=28561
  number = number.replace(/^0+/, '');
  var coeficient,
    exponent,
    numberOfZeros,
    zeros = '',
    i,
    split,
    index,
    sign;

  // Look for 'e' or 'E'
  if (number.indexOf('e') !== -1) {
    index = number.indexOf('e');
  } else if (number.indexOf('E') !== -1) {
    index = number.indexOf('E');
  } else {
    // Number is not expressed in scientific notation
    return number;
  }

  // Set the number before and after e
  coeficient = number.substring(0, index);
  exponent = number.substring(index + 1, number.length);

  //Remove the decimal separator
  if (coeficient.indexOf(decSeparator) !== -1) {
    split = coeficient.split(decSeparator);
    coeficient = split[0] + split[1];
  }

  if (exponent.indexOf('-') !== -1) {
    // Case the number is smaller than 1
    numberOfZeros = exponent.substring(1, exponent.length);

    //Create the string of zeros
    for (i = 1; i < numberOfZeros; i++) {
      zeros = zeros + '0';
    }
    //Create the final number
    if (coeficient.substring(0, 1) === '-') {
      sign = '-';
      coeficient = coeficient.substring(1, coeficient.length);
      number = sign + '0.' + zeros + coeficient;
    } else {
      number = '0.' + zeros + coeficient;
    }
  } else {
    // Case the number is bigger than 1
    numberOfZeros =
      exponent.indexOf('+') !== -1
        ? exponent.substring(1, exponent.length)
        : exponent;
    if (split) {
      numberOfZeros = numberOfZeros - split[1].length;
    }

    if (numberOfZeros >= 0) {
      //Need to concatenate zeros to the coefficient
      for (i = 0; i < numberOfZeros; i++) {
        zeros = zeros + '0';
      }
      //Create the final number
      number = coeficient + zeros;
    } else {
      // final decimal number is not integer: add dot decimal separator in the correct position
      number =
        coeficient.substr(0, coeficient.length + numberOfZeros) +
        '.' +
        coeficient.substr(coeficient.length + numberOfZeros);
    }
  }

  return number;
};
