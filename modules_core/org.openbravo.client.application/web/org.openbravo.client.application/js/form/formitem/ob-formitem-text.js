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
 * All portions are Copyright (C) 2011-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

// == OBTextItem ==
// Input for normal strings
isc.ClassFactory.defineClass('OBTextItem', isc.TextItem);

isc.OBTextItem.addProperties({
  operator: 'iContains',
  validateOnExit: true,
  maskSaveLiterals: true,

  validateAgainstMask: true,

  init: function() {
    if (this.mask && this.validateAgainstMask) {
      this.resetMaskValidator(true);
    }

    this.Super('init', arguments);
  },

  resetMaskValidator: function(createNew) {
    var gridField;
    if (this.maskValidator && this.validators) {
      this.validators.remove(this.maskValidator);
      delete this.maskValidator;
    }
    if (createNew && this.mask && this.validateAgainstMask) {
      this.maskValidator = isc.clone(
        isc.Validator.getValidatorDefinition('mask')
      );
      this.maskValidator.mask = this.createRegExpFromMask(this.mask);
      this.validators = this.validators || [];
      this.validators.push(this.maskValidator);
    }
    if (this.grid) {
      gridField = this.grid.getField(this.name);
      if (gridField) {
        // update the validators of the grid field, so that it is taken into account in the ListGrid.validateRowValues function
        gridField.validators = this.validators;
      }
    }
  },

  createRegExpFromMask: function(mask) {
    var split,
      i,
      regexp = '',
      escaped = false;
    if (!mask) {
      return null;
    }
    //when ranges are already present, return the same
    if (mask.indexOf('[') !== -1) {
      return mask;
    }
    split = mask.split('');
    for (i = 0; i < split.length; i++) {
      if (escaped) {
        regexp = regexp + '\\' + split[i];
        escaped = false;
        continue;
      }
      if (split[i] === '\\') {
        escaped = true;
        continue;
      } else if (split[i] === '<' || split[i] === '>') {
        // ignore
        continue;
      } else if (split[i] === '0') {
        regexp = regexp + '[0-9-+]';
      } else if (split[i] === '9') {
        regexp = regexp + '[0-9\\s]';
      } else if (split[i] === '#') {
        regexp = regexp + '[\\d]';
      } else if (split[i] === 'L') {
        regexp = regexp + '[A-Za-z]';
      } else if (split[i] === '?') {
        regexp = regexp + '[A-Za-z\\s]';
      } else if (split[i] === 'A') {
        regexp = regexp + '[A-Za-z0-9]';
      } else if (split[i] === 'a') {
        regexp = regexp + '[A-Za-z0-9]';
      } else if (split[i] === 'C') {
        regexp = regexp + '[A-Za-z0-9\\s]';
      } else {
        regexp = regexp + split[i];
      }
    }
    return regexp;
  },

  itemHoverHTML: function(item, form) {
    if (this.isDisabled()) {
      return this.getValue();
    } else if (this.mask) {
      return this.mask;
    }
  },

  setMask: function(mask) {
    this.Super('setMask', arguments);
    this.resetMaskValidator(mask);
  }
});

isc.ClassFactory.defineClass('OBTextFilterItem', isc.OBTextItem);

isc.OBTextFilterItem.addProperties({
  allowExpressions: true,
  validateAgainstMask: false,
  // In P&E grids, on blur will be overridden to ensure correct record selection having filter on change disabled
  canOverrideOnBlur: true,

  init: function() {
    var field = this.grid.getField(this.name);
    if (
      field &&
      field.gridProps &&
      field.gridProps.filterEditorProperties &&
      field.gridProps.filterEditorProperties.filterOnChange === false
    ) {
      this.actOnKeypress = false;
      // Explicitly sets the filterOnKeypress property of the field.
      // This prevents the restoring of this.actOnKeypress to true when it should remain false.
      // See issue https://issues.openbravo.com/view.php?id=31904
      field.filterOnKeypress = false;
    }
    this.Super('init', arguments);
  },

  blur: function() {
    if (this.actOnKeypress === false) {
      this.form.grid.performAction();
    }
    return this.Super('blur', arguments);
  },

  // solve a small bug in the value expressions
  buildValueExpressions: function() {
    var ret = this.Super('buildValueExpressions', arguments);
    if (isc.isA.String(ret) && ret.contains('undefined')) {
      return ret.replace('undefined', '');
    }
    return ret;
  },

  // Solve a small bug on iBetweenInclusive criteria
  // See issue https://issues.openbravo.com/view.php?id=26504
  setCriterion: function(criterion) {
    if (
      criterion &&
      (criterion.operator === 'iBetweenInclusive' ||
        criterion.operator === 'betweenInclusive') &&
      criterion.end.indexOf('ZZZZZZZZZZ') !== -1
    ) {
      criterion.end = criterion.end.substring(0, criterion.end.length - 10);
    }
    this.Super('setCriterion', arguments);
  }
});
