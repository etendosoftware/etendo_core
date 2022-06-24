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
 * All portions are Copyright (C) 2011-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
isc.ClassFactory.defineClass(
  'OBSelectorFilterSelectItem',
  isc.OBFKFilterTextItem
);

isc.OBSelectorFilterSelectItem.addProperties({
  filterDataBoundPickList: function(requestProperties, dropCache) {
    requestProperties = requestProperties || {};
    requestProperties.params = requestProperties.params || {};
    // selector can be placed on a standard window view or on a process definition view.
    var contextInfo;
    if (
      this.selectorWindow.selector &&
      this.selectorWindow.selector.form &&
      this.selectorWindow.selector.form.view
    ) {
      // on purpose not passing the third boolean param
      contextInfo = this.selectorWindow.selector.form.view.getContextInfo(
        false,
        true
      );
    } else if (
      this.selectorWindow.selector &&
      this.selectorWindow.selector.form &&
      this.selectorWindow.selector.form.paramWindow
    ) {
      contextInfo = this.selectorWindow.selector.form.paramWindow.getContextInfo(
        false,
        true
      );
    }

    // also add the special ORG parameter
    if (this.selectorWindow.selector.form.getField('organization')) {
      requestProperties.params[
        OB.Constants.ORG_PARAMETER
      ] = this.selectorWindow.selector.form.getValue('organization');
    } else if (contextInfo.inpadOrgId) {
      requestProperties.params[OB.Constants.ORG_PARAMETER] =
        contextInfo.inpadOrgId;
    }

    return this.Super('filterDataBoundPickList', [requestProperties, true]);
  }
});
