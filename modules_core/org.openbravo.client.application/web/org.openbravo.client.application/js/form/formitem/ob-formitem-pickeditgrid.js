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
 * All portions are Copyright (C) 2014-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

// == OBPickEditGridItem ==
isc.ClassFactory.defineClass('OBPickEditGridItem', isc.CanvasItem);

isc.OBPickEditGridItem.addProperties({
  rowSpan: 1,
  colSpan: 4,
  defaultFilter: null,

  // validator at item level, check grid has no errors
  validators: {
    condition: function(item) {
      var grid = item.canvas.viewGrid,
        hasErrors = false,
        i,
        j,
        fields,
        recordsToValidate,
        record,
        lineNumbers;
      grid.endEditing();
      fields = grid.getFields();
      if (grid.viewProperties && grid.viewProperties.showSelect) {
        // validate the selected records
        recordsToValidate = grid.getSelectedRecords() || [];
      } else {
        // grid doesn't allow record selection, validate all the records
        recordsToValidate = (grid.data && grid.data.allRows) || [];
      }
      for (i = 0; i < recordsToValidate.length; i++) {
        record = grid.getEditedRecord(
          grid.getRecordIndex(recordsToValidate[i])
        );
        for (j = 0; j < fields.length; j++) {
          if (
            fields[j].required &&
            (record[fields[j].name] === null ||
              record[fields[j].name] === undefined ||
              record[fields[j].name] === '')
          ) {
            hasErrors = true;
            if (!lineNumbers) {
              lineNumbers = grid
                .getRecordIndex(recordsToValidate[i])
                .toString();
            } else {
              lineNumbers =
                lineNumbers +
                ',' +
                grid.getRecordIndex(recordsToValidate[i]).toString();
            }
          }
        }
      }
      if (hasErrors) {
        if (item.form.errorMessage) {
          item.form.errorMessage =
            item.form.errorMessage + '. ' + item.title + ': ' + lineNumbers;
        } else {
          item.form.errorMessage = item.title + ': ' + lineNumbers;
        }
      }
      return !(hasErrors || grid.hasErrors());
    }
  },

  init: function() {
    var me = this,
      pickAndExecuteViewProperties = {};
    pickAndExecuteViewProperties.viewProperties = this.viewProperties;
    pickAndExecuteViewProperties.view = this.view;
    pickAndExecuteViewProperties.parameterName = this.name;
    pickAndExecuteViewProperties.onGridLoadFunction = this.onGridLoadFunction;
    if (this.view.isPickAndExecuteWindow) {
      this.view.resized = function(messagebarVisible) {
        var heightCorrection = 95;
        if (me.view.isExpandedRecord) {
          heightCorrection = heightCorrection - 61;
        }
        if (messagebarVisible) {
          me.canvas.setHeight(
            me.view.height - (heightCorrection + me.view.messageBar.height)
          );
        } else {
          me.canvas.setHeight(me.view.height - heightCorrection);
        }
        me.canvas.redraw();
      };
    } else {
      pickAndExecuteViewProperties.height =
        45 +
        OB.Styles.Process.PickAndExecute.gridCellHeight *
          this.displayedRowsNumber;
    }
    this.canvas = isc.OBPickAndExecuteView.create(pickAndExecuteViewProperties);
    this.Super('init', arguments);
    this.selectionLayout = this.canvas;
  },

  getValue: function() {
    var allProperties = {},
      grid = this.canvas.viewGrid,
      allRows,
      len,
      i,
      selection,
      tmp;

    // if available, use grid.pneSelectedRecords because getSelectedRecords can
    // return inaccurate values in case of records selected in different pages
    selection = grid.pneSelectedRecords || grid.getSelectedRecords() || [];
    len = selection.length;
    allRows = grid.data.allRows || grid.data.localData || grid.data;
    allProperties._selection = [];
    allProperties._allRows = [];
    for (i = 0; i < len; i++) {
      tmp = isc.addProperties(
        {},
        selection[i],
        grid.getEditedRecord(grid.getRecordIndex(selection[i]))
      );
      allProperties._selection.push(tmp);
    }
    len = (allRows && allRows.length) || 0;
    if (!grid.data.resultSize || len < grid.data.resultSize) {
      for (i = 0; i < len; i++) {
        tmp = isc.addProperties(
          {},
          allRows[i],
          grid.getEditedRecord(grid.getRecordIndex(allRows[i]))
        );
        allProperties._allRows.push(tmp);
      }
    }
    return allProperties;
  },

  setDisabled: function(newState) {
    this.Super('setDisabled', arguments);
    if (newState === true) {
      this.setDisabled(false);
      this.canvas.viewGrid.setCanEdit(false);
    } else {
      this.canvas.viewGrid.setCanEdit(true);
    }
  },

  setDefaultFilter: function(defaultFilter) {
    this.defaultFilter = defaultFilter;
  },

  destroy: function() {
    this.canvas.destroy();
    this.Super('destroy', arguments);
  }
});
