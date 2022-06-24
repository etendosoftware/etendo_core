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
 * All portions are Copyright (C) 2012 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB = window.OB || {};
OB.Utilities = window.OB.Utilities || {};

// = Openbravo Style Utilities =
// Defines utility methods related to Styles.
OB.Utilities.Style = {};

// ** {{{ OB.Utilities.Style.getSheet }}} **
//
// Gets a CSS spreadsheet
//
// Parameters:
// * {{{position}}}: {Integer} position
// Return:
// * The CSS spreadsheet
OB.Utilities.Style.getSheet = function(position, type) {
  var stylesheet, i;
  if (position) {
    stylesheet = document.styleSheets[position];
  } else {
    if (!type) {
      for (i = 0; i < document.styleSheets.length; i++) {
        if (document.styleSheets[i].href) {
          stylesheet = document.styleSheets[i];
        }
      }
    } else if (type) {
      for (i = 0; i < document.styleSheets.length; i++) {
        if (
          document.styleSheets[i].href &&
          document.styleSheets[i].href.indexOf(type) === -1
        ) {
          stylesheet = document.styleSheets[i];
        }
      }
    }
  }
  return stylesheet;
};
// ** {{{ OB.Utilities.Style.addRule }}} **
//
// Adds a style definition to the CSS in last position
//
// Parameters:
// * {{{selector}}}: {String} the selector name to be added
// * {{{declaration}}}: {String} the properties of this selector
OB.Utilities.Style.addRule = function(selector, declaration) {
  var stylesheet = OB.Utilities.Style.getSheet();
  if (typeof stylesheet === 'object') {
    if (navigator.userAgent.toUpperCase().indexOf('MSIE') !== -1) {
      stylesheet.addRule(selector, declaration);
    } else {
      stylesheet.insertRule(
        selector + ' { ' + declaration + ' }',
        stylesheet.cssRules.length
      );
    }
  }
};

// ** {{{ OB.Utilities.Style.removeRule }}} **
//
// Removes a style definition at given position
//
// Parameters:
// * {{{selectorIndex}}}: {Integer} the position of the selector to remove
OB.Utilities.Style.removeRule = function(selectorIndex) {
  var stylesheet = OB.Utilities.Style.getSheet();

  if (typeof stylesheet === 'object') {
    if (navigator.userAgent.toUpperCase().indexOf('MSIE') !== -1) {
      stylesheet.removeRule(selectorIndex);
    } else {
      stylesheet.deleteRule(selectorIndex);
    }
  }
};

// ** {{{ OB.Utilities.Style.getRulePosition }}} **
//
// Removes a style definition at given position
//
// Parameters:
// * {{{selector}}}: {String} the name of the selector to obtain position
// Return:
// * The selector position
OB.Utilities.Style.getRulePosition = function(selector) {
  var stylesheet = OB.Utilities.Style.getSheet(),
    position = [],
    i;

  if (typeof stylesheet === 'object') {
    if (navigator.userAgent.toUpperCase().indexOf('MSIE') !== -1) {
      for (i = 0; i < stylesheet.rules.length; i++) {
        if (
          stylesheet.rules[i].selectorText.toLowerCase() ===
          selector.toLowerCase()
        ) {
          position.push(i);
        }
      }
    } else {
      for (i = 0; i < stylesheet.cssRules.length; i++) {
        if (
          typeof stylesheet.cssRules[i].selectorText !== 'undefined' &&
          stylesheet.cssRules[i].selectorText.toLowerCase() ===
            selector.toLowerCase()
        ) {
          position.push(i);
        }
      }
    }
  }
  return position;
};
