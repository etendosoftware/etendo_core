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
 * All portions are Copyright (C) 2010-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
isc.ClassFactory.defineClass('OBGrid', isc.ListGrid);

// = OBGrid =
// The OBGrid combines common grid functionality usefull for different
// grid implementations.
isc.OBGrid.addProperties({
  bodyConstructor: 'OBViewGridBody',
  reverseRTLAlign: true,
  dragTrackerMode: 'none',
  // recycle gives better performance but also results
  // in strange results that not all record components are
  // drawn when scrolling very fast
  recordComponentPoolingMode: 'viewport',

  showRecordComponentsByCell: true,
  recordComponentPosition: 'within',
  poolComponentsPerColumn: true,
  showRecordComponents: true,
  escapeHTML: true,
  canMultiSort: false,
  bodyProperties: {
    canSelectText: true,

    // the redraw on change should not only redraw the current item
    // but the whole edit row, make sure that happens asynchronously
    redrawFormItem: function(item, reason) {
      var lg = this.grid,
        row = lg.getEditRow(),
        col = lg.getColNum(item.getFieldName());

      // If the user has edited the cell, or setValue() has been called on the item
      // we don't want a call to redraw() on the item to drop that value
      if (lg.getEditCol() === col) {
        lg.storeUpdatedEditorValue();
      }

      if (row === 0 || row > 0) {
        lg.fireOnPause('refreshEditRow', function() {
          lg.refreshRow(row);
        });
      }
    }
  },

  //prevent multi-line content to show strangely
  //https://issues.openbravo.com/view.php?id=17531, https://issues.openbravo.com/view.php?id=24878
  formatDisplayValue: function(value, record, rowNum, colNum) {
    var index;

    if (this.inCellHoverHTML || !isc.isA.String(value)) {
      return value;
    }

    index = value.indexOf('\n');
    if (index !== -1) {
      return value.substring(0, index) + '...';
    }

    return value;
  },

  onFetchData: function(criteria, requestProperties) {
    this.setFechingData();
  },

  setFechingData: function() {
    this.fetchingData = true;
  },

  isFetchingData: function() {
    return this.fetchingData;
  },

  isFilterClauseApplied: function() {
    return !!this.filterClause;
  },

  cellHoverHTML: function(record, rowNum, colNum) {
    var ret,
      field = this.getField(colNum),
      cellErrors,
      prefix = '',
      func = this.getGridSummaryFunction(field),
      isGroupOrSummary =
        record &&
        (record[this.groupSummaryRecordProperty] ||
          record[this.gridSummaryRecordProperty]);

    if (!record) {
      return;
    }

    if (func && isGroupOrSummary) {
      if (func === 'sum') {
        prefix = OB.I18N.getLabel('OBUIAPP_SummaryFunctionSum');
      }
      if (func === 'min') {
        prefix = OB.I18N.getLabel('OBUIAPP_SummaryFunctionMin');
      }
      if (func === 'max') {
        prefix = OB.I18N.getLabel('OBUIAPP_SummaryFunctionMax');
      }
      if (func === 'count') {
        prefix = OB.I18N.getLabel('OBUIAPP_SummaryFunctionCount');
      }
      if (func === 'avg') {
        prefix = OB.I18N.getLabel('OBUIAPP_SummaryFunctionAvg');
      }
      if (prefix) {
        prefix = prefix + ' ';
      }
    }

    if (this.isCheckboxField(field)) {
      return OB.I18N.getLabel('OBUIAPP_GridSelectColumnPrompt');
    }

    if (this.cellHasErrors(rowNum, colNum)) {
      cellErrors = this.getCellErrors(rowNum, colNum);
      // note cellErrors can be a string or array
      // accidentally both have the length property
      if (cellErrors && cellErrors.length > 0) {
        return OB.Utilities.getPromptString(cellErrors);
      }
    }
    if (record[isc.OBViewGrid.ERROR_MESSAGE_PROP]) {
      return record[isc.OBViewGrid.ERROR_MESSAGE_PROP];
    }

    this.inCellHoverHTML = true;
    ret = this.Super('cellHoverHTML', arguments);
    delete this.inCellHoverHTML;
    return prefix + (ret ? ret : '');
  },

  enableShortcuts: function() {
    var ksAction_FocusFilter,
      ksAction_FocusGrid,
      ksAction_ClearFilter,
      ksAction_SelectAll,
      ksAction_UnselectAll;

    ksAction_FocusFilter = function(caller) {
      caller.focusInFirstFilterEditor();
      return false; //To avoid keyboard shortcut propagation
    };
    OB.KeyboardManager.Shortcuts.set(
      'Grid_FocusFilter',
      ['OBGrid.body', 'OBGrid.editForm'],
      ksAction_FocusFilter
    );

    ksAction_FocusGrid = function(caller) {
      if (
        caller.getPrototype().Class !== 'OBViewGrid' ||
        caller.data.localData[0]
      ) {
        // In OBViewGrid case, only execute action if there are at least one row in the grid
        caller.focus();
        if (!caller.getSelectedRecord()) {
          // If there are no rows already selected in the grid, select the first one
          caller.selectSingleRecord(0);
        }
      }
      return false; //To avoid keyboard shortcut propagation
    };
    OB.KeyboardManager.Shortcuts.set(
      'Grid_FocusGrid',
      'OBGrid.filter',
      ksAction_FocusGrid
    );

    ksAction_ClearFilter = function(caller) {
      caller.clearFilter(true);
      return false; //To avoid keyboard shortcut propagation
    };
    OB.KeyboardManager.Shortcuts.set(
      'Grid_ClearFilter',
      ['OBGrid.body', 'OBGrid.filter', 'OBGrid.editForm'],
      ksAction_ClearFilter
    );

    ksAction_SelectAll = function(caller) {
      caller.selectAllRecords();
      return false; //To avoid keyboard shortcut propagation
    };
    OB.KeyboardManager.Shortcuts.set(
      'Grid_SelectAll',
      'OBGrid.body',
      ksAction_SelectAll
    );

    ksAction_UnselectAll = function(caller) {
      if (caller.getSelectedRecords().length > 1) {
        caller.deselectAllRecords();
      }
      return false; //To avoid keyboard shortcut propagation
    };
    OB.KeyboardManager.Shortcuts.set(
      'Grid_UnselectAll',
      'OBGrid.body',
      ksAction_UnselectAll
    );
  },

  draw: function() {
    this.enableShortcuts();
    this.Super('draw', arguments);
  },

  bodyKeyPress: function(event, eventInfo) {
    if (
      eventInfo &&
      this.lastSelectedRecord &&
      ((eventInfo.keyName === isc.OBViewGrid.ARROW_UP_KEY_NAME &&
        this.data.localData &&
        this.data.localData[0].id === this.lastSelectedRecord.id) ||
        (eventInfo.keyName === isc.OBViewGrid.ARROW_DOWN_KEY_NAME &&
          this.data.localData &&
          this.data.localData[this.data.localData.length - 1] &&
          this.data.localData[this.data.localData.length - 1].id ===
            this.lastSelectedRecord.id))
    ) {
      return true;
    }
    var response = OB.KeyboardManager.Shortcuts.monitor('OBGrid.body', this);
    if (response !== false) {
      response = this.Super('bodyKeyPress', arguments);
    }
    return response;
  },

  editFormKeyDown: function() {
    // Custom method. Only works if the form is an OBViewForm
    var response = OB.KeyboardManager.Shortcuts.monitor(
      'OBGrid.editForm',
      this
    );
    if (response !== false) {
      response = this.Super('editFormKeyDown', arguments);
    }
    return response;
  },

  filterFieldsKeyDown: function(item, form, keyName) {
    // To fix issue https://issues.openbravo.com/view.php?id=21786
    var isEscape =
        isc.EH.getKey() === 'Escape' &&
        !isc.EH.ctrlKeyDown() &&
        !isc.EH.altKeyDown() &&
        !isc.EH.shiftKeyDown(),
      response;
    if (
      isEscape &&
      item &&
      Object.prototype.toString.call(item.isPickListShown) ===
        '[object Function]' &&
      item.isPickListShown()
    ) {
      return true; // Then the event will bubble to ComboBoxItem.keyDown
    }

    response = OB.KeyboardManager.Shortcuts.monitor(
      'OBGrid.filter',
      this.grid.fieldSourceGrid
    );
    if (response !== false) {
      if (item.keyDownAction) {
        return item.keyDownAction(item, form, keyName);
      }
    }
    return response;
  },

  isEditing: function() {
    return this.getEditForm();
  },

  focusInFirstFilterEditor: function() {
    if (this.getFilterEditor() && this.getFilterEditor().getEditForm()) {
      // there is a filter editor
      var object = this.getFilterEditor().getEditForm(),
        items,
        item,
        i,
        length;

      // compute a focusable item
      items = object.getItems();
      length = items.length;
      for (i = 0; i < length; i++) {
        item = items[i];
        // The first filterable item (editorType!=='StaticTextItem') should be focused
        if (
          item.getCanFocus() &&
          !item.isDisabled() &&
          item.editorType !== 'StaticTextItem'
        ) {
          this.focusInFilterEditor(item);
          return true;
        }
      }
    }
    return false;
  },

  getCellAlign: function(record, rowNum, colNum) {
    var fld = this.getFields()[colNum];
    if (
      fld &&
      fld.clientClass &&
      OB.Utilities.getCanvasProp(fld.clientClass, 'cellAlign')
    ) {
      return OB.Utilities.getCanvasProp(fld.clientClass, 'cellAlign');
    } else {
      return this.Super('getCellAlign', arguments);
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

    if (
      !OB.User.isPortal &&
      field.isLink &&
      !field.clientClass &&
      record[field.name]
    ) {
      // To keep compatibility with < 3.0MP20 versions that didn't implement 'clientClass' and only have 'isLink' property
      field.clientClass = 'OBGridLinkCellClick';
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

  updateRecordComponent: function(record, colNum, component, recordChanged) {
    var field = this.getField(colNum),
      isSummary =
        record &&
        (record[this.groupSummaryRecordProperty] ||
          record[this.gridSummaryRecordProperty]),
      rowNum = this.getRecordIndex(record),
      isEditRecord = rowNum === this.getEditRow();

    if (isSummary) {
      return null;
    }

    if (isEditRecord && !component.isShownInGridEdit) {
      //TODO: In OBPickAndExecuteGrid this logic doesn't work very well
      return null;
    }

    if (field.clientClass) {
      component.align = this.getCellAlign(record, rowNum, colNum);
      component.field = field;
      component.record = record;
      component.rowNum = rowNum;
      component.colNum = colNum;
      if (component.setRecord) {
        component.setRecord(record);
      }
      return component;
    }
    return null;
  },

  // recompute RecordComponents
  recomputeCanvasComponents: function(rowNum) {
    var i,
      fld,
      length = this.getFields().length;

    // remove client record components in edit mode
    for (i = 0; i < length; i++) {
      fld = this.getFields()[i];
      if (fld.clientClass) {
        this.refreshRecordComponent(rowNum, i);
      }
    }
  },

  startEditing: function(rowNum, colNum, suppressFocus, eCe, suppressWarning) {
    var ret = this.Super('startEditing', arguments);
    this.recomputeCanvasComponents(rowNum);
    return ret;
  },

  startEditingNew: function(rowNum) {
    var ret;
    if (this.getEditRecord() && this.isRequiredFieldWithNoValue()) {
      return;
    }
    ret = this.Super('startEditingNew', arguments);
    this.recomputeCanvasComponents(rowNum + 1);
    return ret;
  },

  isRequiredFieldWithNoValue: function() {
    var fields, i, field, requiredWithNoValue;
    fields = this.fields;
    requiredWithNoValue = false;
    for (i = 0; i < fields.length; i++) {
      field = fields[i];
      if (
        field.required &&
        this.getEditRecord() &&
        this.isEmptyFieldValue(field)
      ) {
        requiredWithNoValue = true;
        break;
      }
    }
    return requiredWithNoValue;
  },

  isEmptyFieldValue: function(field) {
    return (
      this.getEditRecord()[field.name] === undefined ||
      this.getEditRecord()[field.name] === null
    );
  },

  formatLinkValue: function(record, field, colNum, rowNum, value) {
    if (typeof value === 'undefined' || value === null) {
      return '';
    }
    var simpleType = isc.SimpleType.getType(field.type, this.dataSource);
    // note: originalFormatCellValue is set in the initWidget below
    if (field && field.originalFormatCellValue) {
      return field.originalFormatCellValue(value, record, rowNum, colNum, this);
    } else if (simpleType.shortDisplayFormatter) {
      return simpleType.shortDisplayFormatter(
        value,
        field,
        this,
        record,
        rowNum,
        colNum
      );
    }
    return value;
  },

  filterEditorProperties: {
    // http://forums.smartclient.com/showthread.php?p=73107
    // https://issues.openbravo.com/view.php?id=18557
    showAllColumns: true,

    setEditValue: function(
      rowNum,
      colNum,
      newValue,
      suppressDisplay,
      suppressChange
    ) {
      // prevent any setting of non fields in the filter editor
      // this prevents a specific issue that smartclient will set a value
      // in the {field.name} + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER (for example warehouse + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER)
      // because it thinks that the field does not have its own datasource
      if (isc.isA.String(colNum) && !this.getField(colNum)) {
        return;
      }
      return this.Super('setEditValue', arguments);
    },

    getValuesAsCriteria: function(advanced, textMatchStyle, returnNulls) {
      return this.Super('getValuesAsCriteria', [
        true,
        textMatchStyle,
        returnNulls
      ]);
    },

    // is needed to display information in the checkbox field
    // header in the filter editor row
    isCheckboxField: function() {
      return false;
    },

    // overridden for:
    // https://issues.openbravo.com/view.php?id=18509
    editorChanged: function(item) {
      var prop,
        same,
        opDefs,
        val = item.getElementValue(),
        actOnKeypress =
          item.actOnKeypress === true ? item.actOnKeypress : this.actOnKeypress,
        grid = this.parentElement;

      if (this.sourceWidget.allowFilterExpressions && val && actOnKeypress) {
        // if someone starts typing and and or then do not filter
        // onkeypress either
        if (val.contains(' and') || val.contains(' or ')) {
          this.preventPerformFilterFiring();
          return;
        }

        if (val.startsWith('=')) {
          this.preventPerformFilterFiring();
          return;
        }

        // now check if the item element value is only
        // an operator, if so, go away
        opDefs = isc.DataSource.getSearchOperators();
        for (prop in opDefs) {
          if (Object.prototype.hasOwnProperty.call(opDefs, prop)) {
            // let null and not null fall through
            // as they should be filtered
            if (prop === 'isNull' || prop === 'notNull') {
              continue;
            }

            same = opDefs[prop].symbol && val.startsWith(opDefs[prop].symbol);
            if (same) {
              this.preventPerformFilterFiring();
              return;
            }
          }
        }
      }

      if (item.thresholdToFilter && item.thresholdToFilter > grid.fetchDelay) {
        this.currentThresholdToFilter = item.thresholdToFilter;
      } else {
        delete this.currentThresholdToFilter;
      }

      if (grid && grid.lazyFiltering) {
        grid.filterHasChanged = true;
        grid.sorter.enable();
      }
      return this.Super('editorChanged', arguments);
    },

    // function called to clear any pending performFilter calls
    // earlier type actions can already have pending filter actions
    // this deletes them
    preventPerformFilterFiring: function() {
      this.fireOnPause('performFilter', {}, this.fetchDelay);
    },

    // If the criteria contains an 'or' operator due to the changes made for solving
    // issue 20722 (https://issues.openbravo.com/view.php?id=20722), remove the criteria
    // that makes reference to a specific id and return the original one
    removeSpecificIdFilter: function(criteria) {
      var i, length;
      if (!criteria) {
        return criteria;
      }
      if (criteria.operator !== 'or') {
        return criteria;
      }
      if (criteria.criteria && criteria.criteria.length < 2) {
        return criteria;
      }
      // The original criteria is in the position 0, the rest are specific ids
      length = criteria.criteria.length;
      for (i = 1; i < length; i++) {
        if (criteria.criteria.get(i).fieldName !== 'id') {
          return criteria;
        }
      }
      return criteria.criteria.get(0);
    },

    // repair that filter criteria on fk fields can be
    // on the identifier instead of the field itself.
    // after applying the filter the grid will set the criteria
    // back in the filtereditor effectively clearing
    // the filter field. The code here repairs/prevents this.
    setValuesAsCriteria: function(criteria, refresh) {
      // create an edit form right away
      if (!this.getEditForm()) {
        this.makeEditForm();
      }
      var prop, fullPropName;
      // make a copy so that we don't change the object
      // which is maybe used somewhere else
      criteria = criteria ? isc.clone(criteria) : {};
      // If a criterion has been added to include the selected record, remove it
      // See issue https://issues.openbravo.com/view.php?id=20722
      criteria = this.removeSpecificIdFilter(criteria);
      var internCriteria = criteria.criteria;
      if (internCriteria && this.getEditForm()) {
        // now remove anything which is not a field
        // otherwise smartclient will keep track of them and send them again
        var fields = this.getEditForm().getFields(),
          length = fields.length,
          i;
        for (i = internCriteria.length - 1; i >= 0; i--) {
          prop = internCriteria[i].fieldName;
          // happens when the internCriteria[i], is again an advanced criteria
          if (!prop) {
            continue;
          }
          fullPropName = prop;
          if (prop.lastIndexOf(OB.Constants.FIELDSEPARATOR) > 0) {
            var index = prop.lastIndexOf(OB.Constants.FIELDSEPARATOR);
            var propName = prop.substring(0, index);
            if (
              prop.endsWith(
                OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER
              )
            ) {
              prop = propName;
            } else {
              // for Table reference the displayProperty is used in the filtering criteria instead of OB.Constants.IDENTIFIER
              // see issue https://issues.openbravo.com/view.php?id=30800
              var propField = this.getEditForm().getField(propName);
              if (
                propField &&
                propField.displayProperty &&
                prop.endsWith(
                  OB.Constants.FIELDSEPARATOR + propField.displayProperty
                )
              ) {
                prop = propName;
              }
            }
          }
          var fnd = false,
            j;
          for (j = 0; j < length; j++) {
            if (
              fields[j].displayField === fullPropName ||
              fields[j].criteriaField === fullPropName
            ) {
              fnd = true;
              break;
            }
            if (fields[j].name === prop) {
              internCriteria[i].fieldName = prop;
              fnd = true;
              break;
            }
            if (fields[j].name === fullPropName) {
              fnd = true;
              break;
            }
          }
          if (!fnd) {
            internCriteria.removeAt(i);
          }
        }
      }
      return this.Super('setValuesAsCriteria', [criteria, refresh]);
    },

    // the filtereditor will assign the grids datasource to a field
    // if it has a display field and no datasource
    // prevent this as we get the datasource later it is not
    // yet set
    getEditorProperties: function(field) {
      var noDataSource = !field.optionDataSource,
        ret = this.Super('getEditorProperties', arguments);
      if (ret.optionDataSource && noDataSource) {
        delete ret.optionDataSource;
      }
      return ret;
    },

    actionButtonProperties: {
      baseStyle: 'OBGridFilterFunnelIcon',
      visibility: 'hidden',
      showFocused: false,
      showDisabled: false,
      prompt: OB.I18N.getLabel('OBUIAPP_GridFilterIconToolTip'),
      initWidget: function() {
        this.recordEditor.sourceWidget.filterImage = this;
        this.recordEditor.filterImage = this;
        if (
          this.recordEditor.sourceWidget.filterClause ||
          this.recordEditor.sourceWidget.sqlFilterClause
        ) {
          this.prompt = OB.I18N.getLabel('OBUIAPP_GridFilterImplicitToolTip');
          this.visibility = 'inherit';
        }
        this.Super('initWidget', arguments);
      },
      click: function() {
        this.recordEditor.sourceWidget.clearFilter();
      }
    }
  },

  initWidget: function() {
    // prevent the value to be displayed in case of a clientClass
    var i,
      length,
      field,
      formatCellValueFunction,
      OBAbsoluteTimeItem_FormatCellValueFunction,
      OBAbsoluteDateTimeItem_FormatCellValueFunction;

    formatCellValueFunction = function(value, record, rowNum, colNum, grid) {
      return '';
    };

    OBAbsoluteTimeItem_FormatCellValueFunction = function(
      value,
      record,
      rowNum,
      colNum,
      grid
    ) {
      var newValue = value,
        format = isc.OBAbsoluteTimeItem.getInstanceProperty('timeFormatter');
      if (Object.prototype.toString.call(newValue) === '[object String]') {
        newValue = isc.Time.parseInput(newValue);
      }
      newValue = OB.Utilities.Date.addTimezoneOffset(newValue);
      newValue = isc.Time.format(newValue, format);
      return newValue;
    };

    OBAbsoluteDateTimeItem_FormatCellValueFunction = function(
      value,
      record,
      rowNum,
      colNum,
      grid
    ) {
      var newValue = value;
      newValue = OB.Utilities.Date.addTimezoneOffset(newValue);
      var showTime = false;
      if (
        this.editorType &&
        OB.Utilities.getCanvasProp(this.editorType, 'showTime')
      ) {
        showTime = true;
      }

      return OB.Utilities.Date.JSToOB(
        newValue,
        showTime ? OB.Format.dateTime : OB.Format.date
      );
    };

    if (this.fields) {
      length = this.fields.length;
      for (i = 0; i < length; i++) {
        field = this.fields[i];

        if (!field.filterEditorProperties) {
          field.filterEditorProperties = {};
        }

        field.filterEditorProperties.keyDown = this.filterFieldsKeyDown;

        if (OB.PropertyStore.get('EnableScreenReader') === 'Y') {
          field.filterEditorProperties.title = OB.I18N.getLabel(
            'OBUIAPP_Filter_By_Column',
            [field.name]
          );
        }

        if (field.criteriaField) {
          field.filterEditorProperties.criteriaField = field.criteriaField;
        }

        // send the display property to formitem to be used in request params used to fetch data.
        // used for displaying table references properly. Refer issue https://issues.openbravo.com/view.php?id=26696
        if (field.displayProperty) {
          field.filterEditorProperties.displayProperty = field.displayProperty;
        }

        if (
          field.editorType &&
          OB.Utilities.getCanvasProp(field.editorType, 'isAbsoluteTime')
        ) {
          // In the case of an absolute time, the time needs to be converted in order to avoid the UTC conversion
          // http://forums.smartclient.com/showthread.php?p=116135
          field.formatCellValue = OBAbsoluteTimeItem_FormatCellValueFunction;
        }

        if (
          field.editorType &&
          OB.Utilities.getCanvasProp(field.editorType, 'isAbsoluteDateTime')
        ) {
          // In the case of an absolute datetime, the JS date needs to be converted in order to avoid the UTC conversion
          // http://forums.smartclient.com/showthread.php?p=116135
          field.formatCellValue = OBAbsoluteDateTimeItem_FormatCellValueFunction;
        }

        if (field.clientClass) {
          // store the originalFormatCellValue if not already set
          if (field.formatCellValue && !field.formatCellValueFunctionReplaced) {
            field.originalFormatCellValue = field.formatCellValue;
          }
          field.formatCellValueFunctionReplaced = true;
          field.formatCellValue = formatCellValueFunction;
          // if there is a clientClass that expands a grid record, fixedRecordHeight should be false in order to allow the record expansion
          if (
            OB.Utilities.getCanvasProp(field.clientClass, 'canExpandRecord')
          ) {
            this.fixedRecordHeights = false;
          }
          // Manage the case the clientClass overwrites the 'canEdit'
          // If it is not set in the javascript definition of the component, the value returned from the datasource will be used
          // In case canEdit be true, in grid edition mode the 'editorType' of the component will be rendered.
          // In case canEdit be false, nothing will be rendered
          // NOTE: This applies to the editor specified in the component. By default the editor is 'OBClientClassCanvasItem', that means that the editor will be exactly the same
          //       component than the defined Client Class. You can overwrite the 'editorType' to have a different one. In any case, when 'canEdit' be true, it is desiderable to set
          //       'isShownInGridEdit' to false, to avoid view two components one above the other.
          //       As just has been said, if you want to have a particular editor for the edition mode, set 'canEdit' to trie and 'isShownInGridEdit' to false
          //       If you don't want to see anything in edition mode, just set both 'canEdit' and 'isShownInGridEdit' to false
          //       If you want to see exactly the same in edition mode than in read mode (no edition capabilities), set 'canEdit' to false and 'isShownInGridEdit' to true
          if (
            typeof OB.Utilities.getCanvasProp(field.clientClass, 'canEdit') !==
              'undefined' ||
            OB.Utilities.getCanvasProp(field.clientClass, 'canEdit') === null
          ) {
            field.canEdit = OB.Utilities.getCanvasProp(
              field.clientClass,
              'canEdit'
            );
          }
          // Manage the case the clientClass overwrites the 'canSort'
          if (
            typeof OB.Utilities.getCanvasProp(field.clientClass, 'canSort') !==
              'undefined' ||
            OB.Utilities.getCanvasProp(field.clientClass, 'canSort') === null
          ) {
            field.canSort = OB.Utilities.getCanvasProp(
              field.clientClass,
              'canSort'
            );
          }
          // Manage the case the clientClass overwrites the 'canFilter'
          if (
            typeof OB.Utilities.getCanvasProp(
              field.clientClass,
              'canFilter'
            ) !== 'undefined' ||
            OB.Utilities.getCanvasProp(field.clientClass, 'canFilter') === null
          ) {
            field.canFilter = OB.Utilities.getCanvasProp(
              field.clientClass,
              'canFilter'
            );
          }
          // Manage the case the clientClass overwrites the 'filterEditorType'
          if (
            typeof OB.Utilities.getCanvasProp(
              field.clientClass,
              'filterEditorType'
            ) !== 'undefined' ||
            OB.Utilities.getCanvasProp(
              field.clientClass,
              'filterEditorType'
            ) === null
          ) {
            field.filterEditorType = OB.Utilities.getCanvasProp(
              field.clientClass,
              'filterEditorType'
            );
          }
          // Manage the case the clientClass overwrites the 'editorType'. 'OBClientClassCanvasItem' by default.
          if (
            typeof OB.Utilities.getCanvasProp(
              field.clientClass,
              'editorType'
            ) !== 'undefined' ||
            OB.Utilities.getCanvasProp(field.clientClass, 'editorType') === null
          ) {
            field.editorType = OB.Utilities.getCanvasProp(
              field.clientClass,
              'editorType'
            );
            if (field.editorProperties) {
              field.editorProperties.editorType = OB.Utilities.getCanvasProp(
                field.clientClass,
                'editorType'
              );
            }
          }
        }
      }
    }

    if (this.lazyFiltering) {
      this.showSortArrow = isc.ListGrid.BOTH;
      this.sorterDefaults = {
        click: function() {
          var grid = this.parentElement,
            alreadySorted;
          if (!this._iconEnabled) {
            return;
          }
          if (grid.summaryFunctionsHaveChanged && !grid.showGridSummary) {
            grid.setShowGridSummary(true);
          }
          if (grid.filterHasChanged || grid.filterClauseJustRemoved) {
            // the filter clause can only be removed once
            delete grid.filterClauseJustRemoved;
            // Do not change the sorting after receiving the data from the datasource
            grid._filteringAndSortingManually = true;
            grid.filterEditor.performFilter(true, true);
            delete grid.filterHasChanged;
            delete grid.sortingHasChanged;
            delete grid._filteringAndSortingManually;
            delete grid.summaryFunctionsHaveChanged;
          } else if (
            !isc.isA.ResultSet(grid.data) ||
            grid.serverDataNotLoaded
          ) {
            // The initial data has not been loaded yet, refreshGrid
            // refreshGrid applies also the current sorting
            grid.refreshGrid();
            delete grid.sortingHasChanged;
            delete grid.serverDataNotLoaded;
            delete grid.summaryFunctionsHaveChanged;
          } else if (grid.sortingHasChanged) {
            if (grid.summaryFunctionsHaveChanged) {
              alreadySorted = grid.isSortApplied();
              grid.setSort(grid.savedSortSpecifiers, true);
              if (!grid.savedSortSpecifiers) {
                grid.recalculateGridSummary();
              } else if (alreadySorted) {
                grid.data.resort();
              }
            } else {
              grid.setSort(grid.savedSortSpecifiers, true);
            }
            delete grid.sortingHasChanged;
            delete grid.summaryFunctionsHaveChanged;
          } else if (grid.summaryFunctionsHaveChanged) {
            grid.recalculateGridSummary();
            delete grid.summaryFunctionsHaveChanged;
          }
          if (grid && grid.sorter) {
            grid.sorter.disable();
          }
        },
        disable: function() {
          this.setIcon(
            OB.Styles.skinsPath +
              'Default/org.openbravo.client.application/images/grid/applyPendingChanges_Disabled.png'
          );
          this._iconEnabled = false;
        },
        enable: function() {
          this.setIcon(
            OB.Styles.skinsPath +
              'Default/org.openbravo.client.application/images/grid/applyPendingChanges.png'
          );
          this._iconEnabled = true;

          // Disable export to CSV button as actual filter doesn't match with current values in UI.
          // There is no need to re-enable it as it will be handled by button's logic once data is received.
          const grid = this.parentElement;
          grid.view &&
            grid.view.toolBar &&
            grid.view.toolBar.setLeftMemberDisabled('export', true);
        },
        align: 'center',
        prompt: OB.I18N.getLabel('OBUIAPP_ApplyFilters'),
        iconWidth: 10,
        iconHeight: 10,
        icon:
          OB.Styles.skinsPath +
          'Default/org.openbravo.client.application/images/grid/applyPendingChanges.png',
        _iconEnabled: true
      };
    }

    this.Super('initWidget', arguments);
  },

  isSortApplied: function() {
    var i, gridSortSpecifiers, gridDataSortSpecifiers;

    if (!this.data || !this.data.getSort) {
      return false;
    }

    gridSortSpecifiers = this.getSort() || [];
    gridDataSortSpecifiers = this.data.getSort() || [];

    if (gridSortSpecifiers.length !== gridDataSortSpecifiers.length) {
      return false;
    }

    for (i = 0; i < gridSortSpecifiers.length; i++) {
      if (
        gridSortSpecifiers[i].property !== gridDataSortSpecifiers[i].property ||
        gridSortSpecifiers[i].direction !== gridDataSortSpecifiers[i].direction
      ) {
        return false;
      }
    }

    return true;
  },

  clearFilter: function(keepFilterClause, noPerformAction) {
    var i, fld, length, groupState, forceRefresh;
    if (this.lazyFiltering) {
      noPerformAction = true;
      if (this.sorter) {
        this.filterHasChanged = true;
        this.sorter.enable();
        if (this.filterImage) {
          this.filterImage.hide();
        }
      }
    }
    if (!keepFilterClause) {
      if (this.view && this.view.viewGrid) {
        this.view.viewGrid.fetchingData = true;
      }
      // forcing fetch from server in case default filters are removed, in other
      // cases adaptive filtering can be used if possible
      if (this.data) {
        forceRefresh =
          this.filterClause ||
          this.sqlFilterClause ||
          (this.view && this.view.deferOpenNewEdit);

        groupState = this.getGroupState();
        if (forceRefresh && groupState && groupState.groupByFields) {
          // in case of field grouping and filter clause, remove filter grouping
          // because when filter clause is removed data could be bigger than the
          // amount allowed by grouping
          this.setGroupState(null);
        }

        this.data.forceRefresh = forceRefresh;
        if (this.data.context && this.data.context.params) {
          delete this.data.context.params._where;
        }
      }

      delete this.filterClause;
      delete this.sqlFilterClause;
    }

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
    if (this.view && this.view.directNavigation) {
      delete this.view.directNavigation;
    }
    if (this.view && this.view.deferOpenNewEdit) {
      delete this.view.deferOpenNewEdit;
    }
  },

  showSummaryRow: function() {
    var i,
      fld,
      fldsLength,
      newFields = [];
    var ret = this.Super('showSummaryRow', arguments);
    if (this.summaryRow && !this.summaryRowFieldRepaired) {
      // the summaryrow shares the same field instances as the
      // original grid, this must be repaired as the grid and
      // and the summary row need different behavior.
      // copy the fields and repair specific parts
      // don't support links in the summaryrow
      fldsLength = this.summaryRow.fields.length;
      for (i = 0; i < fldsLength; i++) {
        fld = isc.addProperties({}, this.summaryRow.fields[i]);
        newFields[i] = fld;
        fld.isLink = false;
        if (fld.originalFormatCellValue) {
          fld.formatCellValue = fld.originalFormatCellValue;
          fld.originalFormatCellValue = null;
        } else {
          fld.formatCellValue = null;
        }
      }
      this.summaryRow.isSummaryRow = true;
      this.summaryRowFieldRepaired = true;
      this.summaryRow.setFields(newFields);
    }
    return ret;
  },

  // overwritten to prevent undesired requests having summary functions and lazy filtering enabled
  // creates (or updates) and returns the summaryRow autoChild
  // not called directly -- call 'setShowGridSummary' instead
  getSummaryRow: function() {
    if (
      this.summaryRow &&
      (this.isBeingCancelled || this.summaryRowProperties.isBeingReordered)
    ) {
      if (this.summaryRowProperties.isBeingReordered) {
        this.summaryRow.reorderField(
          this.summaryRowProperties.oldPosition,
          this.summaryRowProperties.newPosition
        );
      }
      return this.summaryRow;
    }
    if (this.lazyFiltering && this.summaryRow) {
      if (this.getSummaryRowDataSource && this.completeFields) {
        this.summaryRow.setDataSource(
          this.getSummaryRowDataSource(),
          this.completeFields.duplicate()
        );
      }
      if (this.summaryFunctionsHaveChanged) {
        return this.summaryRow;
      }
      this.summaryFunctionsHaveChanged = true;
      if (this.sorter) {
        this.sorter.enable();
      }
      return this.summaryRow;
    }
    if (this.summaryRow && this.isParentGridFetchingData()) {
      // return the summaryRow if the grid is child of another grid whose data is being fetched
      // this prevents unneeded datasource requests by not calling the Super 'getSummaryRow' function
      return this.summaryRow;
    }
    return this.Super('getSummaryRow');
  },

  isParentGridFetchingData: function() {
    if (this.view && this.view.parentView && this.view.parentView.viewGrid) {
      return this.view.parentView.viewGrid.isFetchingData();
    }
    return false;
  },

  // puts the grid in a state pending of recalculate summaries
  markForCalculateSummaries: function() {
    if (!this.lazyFiltering) {
      return;
    }
    if (this.showGridSummary) {
      this.setShowGridSummary(false);
      if (!this.hasSummaryFunctions()) {
        // the grid does not have any summary function
        // not mark it as pending for recalculation
        return;
      }
    }
    if (!this.summaryFunctionsHaveChanged) {
      this.summaryFunctionsHaveChanged = true;
      if (this.sorter) {
        this.sorter.enable();
      }
    }
  },

  hasSummaryFunctions: function() {
    var i,
      fields = this.getFields() || [];
    for (i = 0; i < fields.length; i++) {
      if (fields[i].summaryFunction) {
        return true;
      }
    }
    return false;
  },

  // show or hide the filter button
  filterEditorSubmit: function(criteria) {
    this.checkShowFilterFunnelIcon(criteria);
  },

  // overwrites setFields to store the list of fk columns filtered using its id
  // this info is then used when the filter field is recreated
  setFields: function(newFields) {
    this.filterByIdFields = this.getFilterByIdFields();
    if (this.filterByIdFields.length > 0) {
      this.fkCacheCopy = this.getFKFilterAuxiliaryCache(this.getCriteria());
    }
    this.Super('setFields', arguments);
    delete this.fkCacheCopy;
    delete this.filterByIdFields;
  },

  // returns the list of fk fields that are currently being filtered using their id
  getFilterByIdFields: function() {
    var fields,
      i,
      filterByIdFields = [];
    if (this.filterEditor && this.filterEditor.getEditForm()) {
      fields = this.filterEditor.getEditForm().getFields();
      for (i = 0; i < fields.length; i++) {
        if (fields[i].filterType === 'id') {
          filterByIdFields.push(fields[i].name);
        }
      }
    }
    return filterByIdFields;
  },

  // returns an object containing the foreign key filter cache of all the filter fields whose current filter type is 'id'
  getFKFilterAuxiliaryCache: function(criteria) {
    var filterField,
      criterion,
      filterLength = criteria.criteria.length,
      fkFilterAuxCache = [],
      innerCache = [],
      filterEditForm,
      cacheElement,
      i,
      j,
      keyProperty;
    if (!this.filterEditor || !this.filterEditor.getEditForm()) {
      return fkFilterAuxCache;
    }
    filterEditForm = this.filterEditor.getEditForm();
    for (i = 0; i < filterLength; i++) {
      criterion = criteria.criteria[i];
      filterField = filterEditForm.getField(criterion.fieldName);
      innerCache = [];
      if (filterField && filterField.filterType === 'id') {
        keyProperty = this.getKeyProperty(criterion.fieldName);
        if (criterion.criteria) {
          for (j = 0; j < criterion.criteria.length; j++) {
            cacheElement = {};
            cacheElement.fieldName = criterion.criteria[j].fieldName;
            cacheElement[keyProperty] = criterion.criteria[j].value;
            cacheElement[
              OB.Constants.IDENTIFIER
            ] = filterField.getRecordIdentifierFromId(
              criterion.criteria[j].value
            );
            innerCache.add(cacheElement);
          }
        } else {
          cacheElement = {};
          cacheElement.fieldName = criterion.fieldName;
          cacheElement[keyProperty] = criterion.value;
          cacheElement[
            OB.Constants.IDENTIFIER
          ] = filterField.getRecordIdentifierFromId(criterion.value);
          innerCache.add(cacheElement);
        }
        fkFilterAuxCache.add({
          fieldName: criterion.fieldName,
          cache: innerCache
        });
      }
    }
    return fkFilterAuxCache;
  },

  getKeyProperty: function(fieldName) {
    var keyProperty = OB.Constants.ID,
      field = this.getFieldByName(fieldName);
    if (
      field &&
      field.filterEditorProperties &&
      field.filterEditorProperties.keyProperty
    ) {
      keyProperty = field.filterEditorProperties.keyProperty;
    }
    return keyProperty;
  },

  setNewRecordFilterMessage: function() {
    var showMessageProperty, showMessage;

    showMessageProperty = OB.PropertyStore.get(
      'OBUIAPP_ShowNewRecordFilterMsg'
    );
    showMessage = showMessageProperty !== 'N' && showMessageProperty !== '"N"';
    if (showMessage) {
      this.view.messageBar.setMessage(
        isc.OBMessageBar.TYPE_INFO,
        '<div><div style="float: left;">' +
          OB.I18N.getLabel('OBUIAPP_NewRecordFilterMsg') +
          '<br/>' +
          OB.I18N.getLabel('OBUIAPP_ClearFilters') +
          '</div><div style="float: right; padding-top: 15px;"><a href="#" style="font-weight:normal; color:inherit;" onclick="' +
          "window['" +
          this.view.messageBar.ID +
          "'].hide(); OB.PropertyStore.set('OBUIAPP_ShowNewRecordFilterMsg', 'N');\">" +
          OB.I18N.getLabel('OBUIAPP_NeverShowMessageAgain') +
          '</a></div></div>',
        ' '
      );
      this.view.messageBar.hasFilterMessage = true;
    }
  },

  setSingleRecordFilterMessage: function() {
    var showMessageProperty, showMessage;

    if (
      !this.isOpenDirectModeLeaf &&
      !this.view.isShowingForm &&
      (this.view.messageBar && !this.view.messageBar.isVisible())
    ) {
      showMessageProperty = OB.PropertyStore.get(
        'OBUIAPP_ShowSingleRecordFilterMsg'
      );
      showMessage =
        showMessageProperty !== 'N' && showMessageProperty !== '"N"';
      if (showMessage) {
        this.view.messageBar.setMessage(
          isc.OBMessageBar.TYPE_INFO,
          '<div><div style="float: left;">' +
            OB.I18N.getLabel('OBUIAPP_SingleRecordFilterMsg') +
            '<br/>' +
            OB.I18N.getLabel('OBUIAPP_ClearFilters') +
            '</div><div style="float: right; padding-top: 15px;"><a href="#" style="font-weight:normal; color:inherit;" onclick="' +
            "window['" +
            this.view.messageBar.ID +
            "'].hide(); OB.PropertyStore.set('OBUIAPP_ShowSingleRecordFilterMsg', 'N');\">" +
            OB.I18N.getLabel('OBUIAPP_NeverShowMessageAgain') +
            '</a></div></div>',
          ' '
        );
        this.view.messageBar.hasFilterMessage = true;
      }
    } else if (
      this.isOpenDirectModeLeaf &&
      this.view.messageBar.hasFilterMessage
    ) {
      // remove grid message if it was set previously when in direct open
      this.view.messageBar.hasFilterMessage = false;
      this.view.messageBar.hide();
    }
  },

  checkShowFilterFunnelIcon: function(criteria, messageBar) {
    if (!this.filterImage) {
      return;
    }
    // if no message bar is provided, use the message bar of the view
    if (!messageBar && this.view && this.view.messageBar) {
      messageBar = this.view.messageBar;
    }
    var gridIsFiltered = this.isGridFiltered(criteria);
    var noParentOrParentSelected =
      !this.view ||
      !this.view.parentView ||
      (this.view.parentView.viewGrid.getSelectedRecords() &&
        this.view.parentView.viewGrid.getSelectedRecords().length > 0);

    if (this.view && this.view.directNavigation) {
      this.filterImage.prompt = OB.I18N.getLabel(
        'OBUIAPP_GridFilterSingleRecord'
      );
      this.filterImage.show(true);
      this.setSingleRecordFilterMessage();
      return;
    } else if ((this.filterClause || this.sqlFilterClause) && gridIsFiltered) {
      this.filterImage.prompt = OB.I18N.getLabel(
        'OBUIAPP_GridFilterBothToolTip'
      );
      this.filterImage.show(true);
    } else if (this.filterClause || this.sqlFilterClause) {
      this.filterImage.prompt = OB.I18N.getLabel(
        'OBUIAPP_GridFilterImplicitToolTip'
      );
      this.filterImage.show(true);
    } else if (gridIsFiltered) {
      this.filterImage.prompt = OB.I18N.getLabel(
        'OBUIAPP_GridFilterExplicitToolTip'
      );
      this.filterImage.show(true);
    } else {
      this.filterImage.prompt = OB.I18N.getLabel(
        'OBUIAPP_GridFilterIconToolTip'
      );
      if (
        this.view &&
        this.view.messageBar &&
        this.view.messageBar.hasFilterMessage
      ) {
        this.view.messageBar.hide();
      }
      this.filterImage.hide();
    }

    if (
      (this.filterClause || this.sqlFilterClause) &&
      !this.view.isShowingForm &&
      (messageBar && !messageBar.isVisible())
    ) {
      var showMessageProperty = OB.PropertyStore.get(
          'OBUIAPP_ShowImplicitFilterMsg'
        ),
        showMessage =
          showMessageProperty !== 'N' &&
          showMessageProperty !== '"N"' &&
          noParentOrParentSelected;
      if (showMessage) {
        messageBar.setMessage(
          isc.OBMessageBar.TYPE_INFO,
          '<div><div class="' +
            OB.Styles.MessageBar.leftMsgContainerStyle +
            '">' +
            this.filterName +
            '<br/>' +
            OB.I18N.getLabel('OBUIAPP_ClearFilters') +
            '</div><div class="' +
            OB.Styles.MessageBar.rightMsgContainerStyle +
            '"><a href="#" class="' +
            OB.Styles.MessageBar.rightMsgTextStyle +
            '" onclick="' +
            "window['" +
            this.view.messageBar.ID +
            "'].hide(); OB.PropertyStore.set('OBUIAPP_ShowImplicitFilterMsg', 'N');\">" +
            OB.I18N.getLabel('OBUIAPP_NeverShowMessageAgain') +
            '</a></div></div>',
          ' '
        );
        messageBar.hasFilterMessage = true;
      }
    }
  },

  isGridFiltered: function(criteria) {
    if (!this.filterEditor) {
      return false;
    }
    if (this.filterClause) {
      return true;
    }
    if (!criteria) {
      return false;
    }
    return this.isGridFilteredWithCriteria(criteria.criteria);
  },

  isGridFilteredWithCriteria: function(criteria) {
    var i, length;
    if (!criteria) {
      return false;
    }
    length = criteria.length;
    for (i = 0; i < length; i++) {
      var criterion = criteria[i];
      var prop = criterion && criterion.fieldName;
      var fullPropName = prop;
      if (!prop) {
        if (this.isGridFilteredWithCriteria(criterion.criteria)) {
          return true;
        }
        continue;
      }
      var value = criterion.value;
      // see the description in setValuesAsCriteria above
      var separatorIndex = prop.lastIndexOf(OB.Constants.FIELDSEPARATOR);
      if (separatorIndex !== -1) {
        prop = prop.substring(0, separatorIndex);
      }
      var field = this.filterEditor.getField(prop);
      // criterion.operator is set in case of an and/or expression
      if (
        this.isValidFilterField(field) &&
        (criterion.operator || value === false || value || value === 0)
      ) {
        return true;
      }

      field = this.filterEditor.getField(fullPropName);
      // criterion.operator is set in case of an and/or expression
      if (
        this.isValidFilterField(field) &&
        (criterion.operator || value === false || value || value === 0)
      ) {
        return true;
      }
    }
    return false;
  },

  isValidFilterField: function(field) {
    if (!field) {
      return false;
    }
    return !field.name.startsWith('_') && field.canFilter;
  },

  // the valuemap is updated in the form item, make sure that the
  // grid field also has it
  getEditorValueMap: function(field, values) {
    var form,
      ret = this.Super('getEditorValueMap', arguments);
    if (!ret) {
      if (this.getEditForm()) {
        form = this.getEditForm();
        if (form.getItem(field.name) && form.getItem(field.name).valueMap) {
          return form.getItem(field.name).valueMap;
        }
      }
    }
    return ret;
  },

  // = exportData =
  // The exportData function exports the data of the grid to a file. The user will
  // be presented with a save-as dialog.
  // Parameters:
  // * {{{exportProperties}}} defines different properties used for controlling the export, currently only the
  // exportProperties.exportAs and exportProperties._extraProperties are supported (which is defaulted to csv).
  // * {{{data}}} the parameters to post to the server, in addition the filter criteria of the grid are posted.
  exportData: function(exportProperties, data) {
    var d = data || {},
      expProp = exportProperties || {},
      dsURL = this.dataSource.dataURL;
    var sortCriteria;
    var lcriteria = this.getCriteria();
    var gdata = this.getData(),
      isExporting = true;
    if (gdata && gdata.dataSource) {
      lcriteria = gdata.dataSource.convertRelativeDates(lcriteria);
    }

    isc.addProperties(
      d,
      {
        _dataSource: this.dataSource.ID,
        _operationType: 'fetch',
        _noCount: true,
        // never do count for export
        exportAs: expProp.exportAs || 'csv',
        viewState: expProp.viewState,
        _extraProperties: expProp._extraProperties,
        tabId: expProp.tab,
        exportToFile: true,
        _textMatchStyle: 'substring',
        _UTCOffsetMiliseconds: OB.Utilities.Date.getUTCOffsetInMiliseconds()
      },
      lcriteria,
      this.getFetchRequestParams(null, isExporting)
    );

    sortCriteria = this.getSort();
    if (sortCriteria && sortCriteria.length > 0) {
      d._sortBy = sortCriteria[0].property;
      if (sortCriteria[0].direction === 'descending') {
        d._sortBy = '-' + d._sortBy;
      }
    }

    if (d.criteria) {
      // Encode the grid criteria as it is done for the standard grid requests
      // Note that OB.Utilities.postThroughHiddenForm has its own logic for encoding dates
      d.criteria = isc.JSON.encode(d.criteria);
    }

    OB.Utilities.postThroughHiddenForm(dsURL, d);
  },

  getFetchRequestParams: function(params) {
    return params;
  },

  editorKeyDown: function(item, keyName) {
    if (item) {
      if (typeof item.keyDownAction === 'function') {
        item.keyDownAction();
      }
    }
    return this.Super('editorKeyDown', arguments);
  },

  // Prevents empty message to be shown in frozen part
  // http://forums.smartclient.com/showthread.php?p=57581
  createBodies: function() {
    var ret = this.Super('createBodies', arguments);
    if (this.frozenBody) {
      this.frozenBody.showEmptyMessage = false;
    }
    return ret;
  },

  //= getErrorRows =
  // Returns all the rows that have errors.
  getErrorRows: function() {
    var editRows,
      errorRows = [],
      i,
      length;

    if (this.hasErrors()) {
      editRows = this.getAllEditRows(true);
      length = editRows.length;
      for (i = 0; i < length; i++) {
        if (this.rowHasErrors(editRows[i])) {
          errorRows.push(editRows[i]);
        }
      }
    }
    return errorRows;
  },

  // Does not apply if the grid is filtering lazily
  setSort: function(sortSpecifiers, forceSort) {
    if (!forceSort && this.lazyFiltering) {
      this.sortingHasChanged = true;
      if (this.sorter) {
        this.sorter.enable();
      }
      this.savedSortSpecifiers = isc.shallowClone(sortSpecifiers);
      // Refresh the header button titles
      this.refreshHeaderButtons();
    } else {
      this.Super('setSort', arguments);
    }
  },

  refreshHeaderButtons: function() {
    var i, headerButton;
    for (i = 0; i < this.fields.length; i++) {
      headerButton = this.getFieldHeaderButton(i);
      if (headerButton) {
        headerButton.setTitle(headerButton.getTitle());
      }
    }
  },

  getSortFieldCount: function() {
    if (this.lazyFiltering) {
      if (this.savedSortSpecifiers) {
        return this.savedSortSpecifiers.length;
      } else {
        return 0;
      }
    } else {
      return this.Super('getSortFieldCount', arguments);
    }
  },

  toggleSort: function(fieldName, direction) {
    var fullIdentifierName =
      fieldName + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER;
    if (this.lazyFiltering) {
      this.sortingHasChanged = true;
      // If the user clicks on a column that is already ordered, reverse the sort direction
      if (this.savedSortSpecifiers && this.savedSortSpecifiers.length > 0) {
        if (
          this.savedSortSpecifiers[0].property === fieldName ||
          this.savedSortSpecifiers[0].property === fullIdentifierName
        ) {
          if (this.savedSortSpecifiers[0].direction === 'ascending') {
            this.savedSortSpecifiers[0].direction = 'descending';
          } else {
            this.savedSortSpecifiers[0].direction = 'ascending';
          }
        }
        if (this.sorter) {
          this.sorter.enable();
        }
        this.refreshHeaderButtons();
      }
    } else {
      this.Super('toggleSort', arguments);
    }
  },

  getSort: function() {
    if (this.lazyFiltering) {
      return this.savedSortSpecifiers;
    } else {
      return this.Super('getSort', arguments);
    }
  },

  // If the grid is lazy filtering, a field will be considered ordered if it is saved in savedSortSpecifiers
  isSortField: function(fieldName) {
    var i,
      len,
      fullIdentifierName =
        fieldName + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER;
    if (this.lazyFiltering) {
      if (!this.savedSortSpecifiers) {
        return false;
      } else {
        //Search for the fieldName in the savedSortSpecifiers
        len = this.savedSortSpecifiers.length;
        for (i = 0; i < len; i++) {
          if (
            this.savedSortSpecifiers[i].property === fieldName ||
            this.savedSortSpecifiers[0].property === fullIdentifierName
          ) {
            return true;
          }
        }
        return false;
      }
    } else {
      return this.Super('isSortField', arguments);
    }
  },

  getSortArrowImage: function(fieldNum) {
    var sortDirection,
      field = this.getField(fieldNum),
      fullIdentifierName;

    if (this.lazyFiltering) {
      if (!field) {
        return isc.Canvas.spacerHTML(1, 1);
      }
      fullIdentifierName =
        field.name + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER;
      if (this.savedSortSpecifiers && this.savedSortSpecifiers.length > 0) {
        if (
          this.savedSortSpecifiers[0].property === field.name ||
          this.savedSortSpecifiers[0].property === fullIdentifierName
        ) {
          sortDirection = this.savedSortSpecifiers[0].direction;
        }
      }
      if (sortDirection) {
        return this.imgHTML(
          Array.shouldSortAscending(sortDirection)
            ? this.sortAscendingImage
            : this.sortDescendingImage,
          null,
          null,
          null,
          null,
          this.widgetImgDir
        );
      } else {
        return isc.Canvas.spacerHTML(1, 1);
      }
    } else {
      return this.Super('getSortArrowImage', arguments);
    }
  },

  collapseRecord: function(record) {
    var expandedItem = this.getCurrentExpansionComponent(record),
      ret = this.Super('collapseRecord', arguments);
    if (expandedItem && typeof expandedItem.destroy === 'function') {
      expandedItem.destroy();
    }
    return ret;
  },

  selectionChanged: function(record, state) {
    if (record._collapseOnRecordDeselection && !state) {
      var me = this;
      setTimeout(function() {
        // Due to multiple 'record selection' calls due to multiple 'record selection' handlers
        // There are sometimes where selectionChanged is called three times in a 'true' - 'false' - 'true' sequence
        // or even more complex sequences, so a timeout is needed to ensure after a short time span that really
        // the record has been deselected
        if (!me.isSelected(record)) {
          me.collapseRecord(record);
        }
      }, 100);
    }
    return this.Super('selectionChanged', arguments);
  },

  //** {{{ openExpansionProcess }}} **
  //
  // Opens a process inside a grid row.
  // Parameters:
  //  * {{{process}}} The process to be opened
  //  * {{{record}}} The record where the process will be opened
  //  * {{{selectOnOpen}}} It indicates if the record will be selected when the process be opened (true by default)
  //  * {{{deselectAllOnOpen}}} It indicates if all other records will be unselected when the process be opened. It is applied before 'selectOnOpen' (true by default)
  //  * {{{collapseOthersOnOpen}}} It indicates if any other opened process should be closed (true by default)
  //  * {{{collapseOnRecordDeselection}}} It indicates if the process should be closed once the record is deselected (true by default)
  //  * {{{width}}} The width of the opened process (100% by default)
  //  * {{{height}}} The height of the opened process (7 grid rows + 'bottom buttons layout' by default)
  //  * {{{topMargin}}} The top margin of the process. (10 by default)
  //  * {{{rightMargin}}} The right margin of the process. (30 by default)
  //  * {{{bottomMargin}}} The bottom margin of the process. (10 by default)
  //  * {{{leftMargin}}} The left margin of the process. (30 by default)
  openExpansionProcess: function(
    process,
    record,
    selectOnOpen,
    deselectAllOnOpen,
    collapseOthersOnOpen,
    collapseOnRecordDeselection,
    width,
    height,
    topMargin,
    rightMargin,
    bottomMargin,
    leftMargin
  ) {
    var defaultHeight;

    if (!process || !record) {
      return;
    }
    if (this.fixedRecordHeights) {
      isc.warn(
        'This grid has "fixedRecordHeights" set to "true". It should be set to "false" in order to view the process',
        function() {
          return true;
        },
        {
          icon: '[SKINIMG]Dialog/error.png',
          title: OB.I18N.getLabel('OBUIAPP_Error')
        }
      );
      return;
    }

    if (typeof selectOnOpen === 'undefined' || selectOnOpen === null) {
      selectOnOpen = true;
    }
    if (
      typeof deselectAllOnOpen === 'undefined' ||
      deselectAllOnOpen === null
    ) {
      deselectAllOnOpen = true;
    }
    if (
      typeof collapseOthersOnOpen === 'undefined' ||
      collapseOthersOnOpen === null
    ) {
      collapseOthersOnOpen = true;
    }
    if (
      typeof collapseOnRecordDeselection === 'undefined' ||
      collapseOnRecordDeselection === null
    ) {
      collapseOnRecordDeselection = true;
    }
    if (typeof topMargin === 'undefined' || topMargin === null) {
      topMargin = process.expandedTopMargin ? process.expandedTopMargin : 10;
    }
    if (typeof rightMargin === 'undefined' || rightMargin === null) {
      rightMargin = process.expandedRightMargin
        ? process.expandedRightMargin
        : 30;
    }
    if (typeof bottomMargin === 'undefined' || bottomMargin === null) {
      bottomMargin = process.expandedBottomMargin
        ? process.expandedBottomMargin
        : 10;
    }
    if (typeof leftMargin === 'undefined' || leftMargin === null) {
      leftMargin = process.expandedLeftMargin ? process.expandedLeftMargin : 30;
    }

    if (typeof width === 'undefined' || width === null) {
      width = process.expandedWidth ? process.expandedWidth : '100%';
      if (typeof width === 'string' && width.indexOf('%') === -1) {
        width = parseInt(width, 10);
      }
      if (typeof width === 'number') {
        width = width + rightMargin + leftMargin;
      }
    }

    defaultHeight =
      isc.OBViewGrid.getPrototype().cellHeight * 6 + //
      isc.OBViewGrid.getPrototype().filterEditorDefaults.height + //
      OB.Styles.Process.PickAndExecute.buttonLayoutHeight;

    if (typeof height === 'undefined' || height === null) {
      height = process.expandedHeight ? process.expandedHeight : defaultHeight;
      if (typeof height === 'string' && height.indexOf('%') === -1) {
        height = parseInt(width, 10);
      }
      if (typeof height === 'number') {
        height = height + topMargin + bottomMargin;
      }
    }

    if (deselectAllOnOpen) {
      this.deselectAllRecords();
    }

    if (selectOnOpen) {
      this.selectRecord(record);
    }

    process.isExpandedRecord = true;

    this.getExpansionComponent = function(theRecord) {
      var layout = isc.VLayout.create({
        height: height,
        width: width,
        layoutTopMargin: topMargin,
        layoutRightMargin: rightMargin,
        layoutBottomMargin: bottomMargin,
        layoutLeftMargin: leftMargin,
        members: [process]
      });
      return layout;
    };

    this.canExpandMultipleRecords = !collapseOthersOnOpen;
    record._collapseOnRecordDeselection = collapseOnRecordDeselection;

    this.expandRecord(record);

    this.getExpansionComponent = function() {
      return;
    };
  }
});

// = OBViewGridBody =
// OBViewGridBody is used as bodyConstructor for OBGrid, its purpose is to flag
// in the grid when fetch data is complete and data is drawn, to be used by automated
// Selenium tests
isc.ClassFactory.defineClass('OBViewGridBody', 'GridBody');
isc.OBViewGridBody.addProperties({
  redraw: function() {
    var newDrawArea, grid, drawArea, firstRecord, loading;
    this.Super('redraw', arguments);

    grid = this.grid;
    if (grid && grid.fetchingData && grid.body === this) {
      // check if we are still loading data
      newDrawArea = this.getDrawArea();
      drawArea = this._oldDrawArea;
      if (!drawArea) {
        this._oldDrawArea = [0, 0, 0, 0];
      }

      firstRecord = grid.getRecord(newDrawArea[0]);

      loading = firstRecord === Array.LOADING;

      if (!loading) {
        // data is already loaded
        this.grid.fetchingData = false;
      }
    }
  },

  scrollTo: function() {
    if (this.grid.isFilteringExternally) {
      // prevents scrolling the grid while in the middle of a filter datasource request
      // this prevents a duplicated request, see issue https://issues.openbravo.com/view.php?id=29896
      return;
    }
    this.Super('scrollTo', arguments);
  }
});

isc.ClassFactory.defineClass('OBGridSummary', isc.OBGrid);

isc.OBGridSummary.addProperties({
  getCellStyle: function(record, rowNum, colNum) {
    var field = this.parentElement.getField(colNum);
    if (
      field.summaryFunction &&
      this['summaryRowStyle_' + field.summaryFunction]
    ) {
      return this['summaryRowStyle_' + field.summaryFunction];
    } else {
      return this.summaryRowStyle;
    }
  }
});

isc.ClassFactory.defineClass('OBGridHeaderImgButton', isc.ImgButton);

isc.ClassFactory.defineClass('OBGridLinkItem', isc.HLayout);
isc.OBGridLinkItem.addProperties({
  overflow: 'clip-h',
  btn: null,
  height: 1,
  width: '100%',

  isShownInGridEdit: true,
  initWidget: function() {
    if (!this.btn) {
      this.btn = isc.OBGridLinkButton.create({});
    }
    this.setTitle(this.title);
    this.btn.owner = this;
    this.addMember(this.btn);
    this.Super('initWidget', arguments);
  },

  setTitle: function(title) {
    this.btn.setTitle(title);
  }
});

isc.ClassFactory.defineClass('OBGridLinkButton', isc.Button);

isc.OBGridLinkButton.addProperties({
  action: function() {
    this.owner.doAction();
  }
});

isc.ClassFactory.defineClass('OBGridFormButton', isc.OBFormButton);
isc.OBGridFormButton.addProperties({
  showValue: function(displayValue, dataValue, form, item) {
    if (this.autoFit_orig) {
      // Restore the autofit attribute if it was set in a first instance to avoid that if the button is
      // shown as an editor in edition mode (canEdit: true) spans the row width.
      this.setAutoFit(true);
    }
    return this.Super('showValue', arguments);
  },

  initWidget: function() {
    this.autoFit_orig = this.autoFit;
    return this.Super('initWidget', arguments);
  }
});

isc.defineClass('OBGridLinkCellClick', isc.OBGridLinkItem);

isc.OBGridLinkCellClick.addProperties({
  setRecord: function() {
    this.setTitle(
      this.grid.formatLinkValue(
        this.record,
        this.field,
        this.colNum,
        this.rowNum,
        this.record[this.field.name]
      )
    );
  },

  doAction: function() {
    if (this.grid && this.grid.doCellClick) {
      this.grid.doCellClick(this.record, this.rowNum, this.colNum);
    } else if (this.grid && this.grid.cellClick) {
      this.grid.cellClick(this.record, this.rowNum, this.colNum);
    }
  }
});
