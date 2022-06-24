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
 * All portions are Copyright (C) 2014-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

isc.ClassFactory.defineClass('OBParameterWindowForm', isc.DynamicForm);

// = OBParameterWindowForm =
// The OBParameterWindowForm is the DynamicForm used in the OBParameterWindowView
isc.OBParameterWindowForm.addProperties({
  paramWindow: null,
  width: '99%',
  titleSuffix: '',
  requiredTitleSuffix: '',
  autoFocus: true,
  titleOrientation: 'top',
  numCols: 4,
  showErrorIcons: false,
  colWidths: ['*', '*', '*', '*'],
  itemChanged: function(item, newValue) {
    this.paramWindow.handleReadOnlyLogic();
    this.paramWindow.handleButtonsStatus();
  },

  setItems: function(itemList) {
    itemList.forEach(function(item) {
      item.setValueProgrammatically = function(value) {
        if (this.setDateParameterValue) {
          this.setDateParameterValue(value);
        } else if (this.setValue) {
          this.setValue(value);
        }
        if (this.onChangeFunction && this.view && this.view.theForm) {
          this.view.theForm.handleItemChange(item);
        }
      };
    });
    this.Super('setItems', arguments);
  },

  // this function is invoked on the blur action of the formitems
  // this is the proper place to execute the client-side callouts
  handleItemChange: function(item) {
    var dynamicColumns,
      affectedParams,
      i,
      field,
      me = this,
      registryId;

    registryId = this.paramWindow.viewId || this.paramWindow.processId;

    // Execute onChangeFunctions if they exist
    if (OB.OnChangeRegistry.hasOnChange(registryId, item)) {
      OB.OnChangeRegistry.call(
        registryId,
        item,
        this.paramWindow,
        this,
        this.paramWindow.viewGrid
      );
    }
    // Check validation rules (subordinated fields), when value of a
    // parent field is changed, all its subordinated are reset
    dynamicColumns = this.paramWindow.dynamicColumns;
    if (dynamicColumns && dynamicColumns[item.name]) {
      affectedParams = dynamicColumns[item.name];
      for (i = 0; i < affectedParams.length; i++) {
        field = this.getField(affectedParams[i]);
        if (field && field.setValue) {
          field.setValue(null);
          this.itemChanged(field, null);
        }
      }
    }
    // evaluate explicitly the display logic for the grid fields
    this.paramWindow.handleDisplayLogicForGridColumns();
    this.markForRedraw();
    // this timeout is needed to ensure that the availability of the process definition buttons is updated after the redrawal of the form because:
    // - the availability of the process definition buttons must be updated after the form redrawal
    // - at this point the form cannot be directly redrawn because otherwise the focus does not behave properly, that's why markForRedraw is used
    // - there is no way to assign a callback to the markForRedraw function
    setTimeout(function() {
      me.paramWindow.handleButtonsStatus();
    }, 200);
    item._hasChanged = false;
  },

  setFieldSections: function() {
    var i, item, length;

    length = this.getItems().length;
    for (i = 0; i < length; i++) {
      item = this.getItem(i);
      if (item && item.setSectionItemInContent) {
        item.setSectionItemInContent(this);
      }
    }
  }
});
