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

// = Search Attribute widget =
// A specific subclass of OBSearchItem for attribute fields.
isc.ClassFactory.defineClass('OBPAttributeSearchItem', isc.OBSearchItem);

isc.OBPAttributeSearchItem.addProperties({
  operator: 'iContains',

  showPicker: function() {
    if (this.isDisabled()) {
      return;
    }
    var parameters = [],
      index = 0,
      values;
    var form = this.form,
      view = form.view ? form.view : this.view;
    if (this.isFocusable()) {
      this.focusInItem();
    }
    parameters[index++] = 'inpKeyValue';
    if (this.getValue()) {
      parameters[index++] = this.getValue();
    } else {
      parameters[index++] = '';
    }
    values = view.getContextInfo(false, true, true, true);
    parameters[index++] = 'WindowID';
    parameters[index++] = view.standardWindow?.windowId ? view.standardWindow.windowId : view.windowId;
    parameters[index++] = 'inpwindowId';
    parameters[index++] = view.standardWindow?.windowId ? view.standardWindow.windowId : view.windowId;
    parameters[index++] = 'inpProduct';
    parameters[index++] = values.inpmProductId;
    if (values.inpmAttributesetId) {
      parameters[index++] = 'inpmAttributesetId';
      parameters[index++] = values.inpmAttributesetId;
    }
    this.openSearchWindow(
      '/info/AttributeSetInstance.html',
      parameters,
      this.getValue()
    );
  }
});
