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
 * All portions are Copyright (C) 2013-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

// Base OBTreeGrid class
// This class is extended by the tree used in the Tree Windows and in the Tree References
isc.ClassFactory.defineClass('OBTreeGrid', isc.TreeGrid);

isc.OBTreeGrid.addProperties({
  showOpenIcons: true,
  showDropIcons: false,
  showNodeIcons: true,
  openerImage:
    OB.Styles.skinsPath +
    'Default/org.openbravo.client.application/images/tree-grid/iconTree.png',
  openerIconSize: 16,
  showCustomIconOpen: true,
  extraIconGap: 5,
  openerIconWidth: 24,
  nodeIcon: null,
  folderIcon: null,
  showSortArrow: 'both',
  showRecordComponentsByCell: true,
  showRecordComponents: true,
  autoFetchTextMatchStyle: 'substring',
  dataProperties: {
    modelType: 'parent',
    rootValue: '-1',
    idField: 'nodeId',
    parentIdField: 'parentId',
    openProperty: 'isOpen'
  },

  initWidget: function() {
    this.sorterDefaults = {};
    this.Super('initWidget', arguments);
  },

  /**
   * When the grid is filtered, show the records that did not comply with the filter (but were ancestors of nodes that did) in grey
   */
  getCellCSSText: function(record, rowNum, colNum) {
    if (record.notFilterHit) {
      return OB.Styles.OBTreeGrid.cellCSSText_notFilterHit;
    } else {
      return OB.Styles.OBTreeGrid.cellCSSText_filterHit;
    }
  },

  clearFilter: function(keepFilterClause, noPerformAction) {
    var i = 0,
      fld,
      length;
    this.view.messageBar.hide();
    if (!keepFilterClause) {
      delete this.filterClause;
      delete this.sqlFilterClause;
    }
    this.forceRefresh = true;
    if (this.filterEditor) {
      if (this.filterEditor.getEditForm()) {
        this.filterEditor.getEditForm().clearValues();
        // clear the date values in a different way
        length = this.filterEditor.getEditForm().getFields().length;

        for (i = 0; i < length; i++) {
          fld = this.filterEditor.getEditForm().getFields()[i];
          if (fld.clearFilterValues) {
            fld.clearFilterValues();
          }
        }
      } else {
        this.filterEditor.setValuesAsCriteria(null);
      }
    }
    if (!noPerformAction) {
      this.filterEditor.performAction();
    }
  },

  createRecordComponent: function(record, colNum) {
    var field = this.getField(colNum),
      rowNum = this.getRecordIndex(record),
      isSummary =
        record &&
        (record[this.groupSummaryRecordProperty] ||
          record[this.gridSummaryRecordProperty]),
      isEditRecord = rowNum === this.getEditRow(),
      canvas,
      clientClassArray,
      clientClass,
      clientClassProps,
      clientClassIsShownInGridEdit;

    if (isSummary) {
      return null;
    }

    if (field.clientClass) {
      clientClassArray = OB.Utilities.clientClassSplitProps(field.clientClass);
      clientClass = clientClassArray[0];
      clientClassProps = clientClassArray[1];

      clientClassIsShownInGridEdit = OB.Utilities.getCanvasProp(
        clientClass,
        'isShownInGridEdit'
      );

      if (!isEditRecord || clientClassIsShownInGridEdit) {
        canvas = isc.ClassFactory.newInstance(
          clientClass,
          {
            grid: this,
            align: this.getCellAlign(record, rowNum, colNum),
            field: field,
            record: record,
            rowNum: rowNum,
            colNum: colNum
          },
          clientClassProps
        );
        if (canvas) {
          if (canvas.setRecord) {
            canvas.setRecord(record);
          }
          return canvas;
        }
      }
    }
    return null;
  },

  handleFilterEditorSubmit: function(criteria, context, callback) {
    var gridCriteria = isc.isA.Function(this.convertCriteria)
      ? this.convertCriteria(criteria)
      : criteria;
    if (isc.isA.Tree(this.data) && this.willFetchData(gridCriteria)) {
      // Only reset the open state if the tree already has data and if the filter criteria has changed
      this.setOpenState('[]');
    }
    this.Super('handleFilterEditorSubmit', arguments);
  },

  // No actions should be triggered when clicking a record with the right button
  cellContextClick: function(record, rowNum, colNum) {
    return false;
  },

  applyCellTypeFormatters: function(
    value,
    record,
    field,
    rowNum,
    colNum,
    isMultipleElement
  ) {
    if (
      (field.type === '_id_15' || field.type === '_id_16') &&
      value &&
      !isc.isA.Date(value) &&
      isc.isA.Date(Date.parseSchemaDate(value))
    ) {
      // applyCellTypeFormatters expects a date as value if the field is a date
      // if the original value is not a date, convert it to date before calling applyCellTypeFormatters
      value = Date.parseSchemaDate(value);
    }
    return this.Super('applyCellTypeFormatters', [
      value,
      record,
      field,
      rowNum,
      colNum,
      isMultipleElement
    ]);
  },

  // converts the date and datetime fields from string to a js date
  transformData: function(data) {
    var dateFields = [],
      fieldName,
      i,
      j,
      type,
      record;
    for (i = 0; i < this.getFields().length; i++) {
      type = isc.SimpleType.getType(this.getFields()[i].type);
      if (type.inheritsFrom === 'date' || type.inheritsFrom === 'datetime') {
        dateFields.add(this.getFields()[i].name);
      }
    }
    if (dateFields.length > 0) {
      for (i = 0; i < data.length; i++) {
        record = data[i];
        for (j = 0; j < dateFields.length; j++) {
          fieldName = dateFields[j];
          if (
            record[fieldName] &&
            !isc.isA.Date(record[fieldName]) &&
            isc.isA.Date(isc.Date.parseSchemaDate(record[fieldName]))
          ) {
            record[fieldName] = isc.Date.parseSchemaDate(record[fieldName]);
          }
        }
      }
    }
  }
});
