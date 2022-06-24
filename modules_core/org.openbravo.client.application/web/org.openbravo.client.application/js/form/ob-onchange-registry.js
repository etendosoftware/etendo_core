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

// == OB.OnChangeRegistry ==
// A registry which can be used to register callouts for a certain
// tab and field combination. Multiple callouts can be registered
// for one field.
isc.ClassFactory.defineClass('OBOnChangeRegistry', isc.OBFunctionRegistry);

isc.OBOnChangeRegistry.addProperties({
  hasOnChange: function(tabId, item) {
    return this.getEntries(tabId, item);
  },

  getEntries: function(tabId, item) {
    return this.getFieldEntry(tabId, item);
  },

  getFieldEntry: function(tabId, item) {
    var field;
    if (item.grid && item.grid.parameterName) {
      field = item.grid.parameterName + OB.Constants.FIELDSEPARATOR + item.name;
    } else {
      field = item.name;
    }
    return this.Super('getEntries', [tabId, field]);
  }
});

OB.OnChangeRegistry = isc.OBOnChangeRegistry.create();
