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
isc.ClassFactory.defineClass('OBViewGrid', isc.OBGrid);
isc.OBViewGrid.addClassProperties({
  EDIT_LINK_FIELD_NAME: '_editLink',
  //prevent the count operation on the server
  NO_COUNT_PARAMETER: '_noCount',
  // note following 2 values should be the same
  // ListGrid._$ArrowUp and ListGrid._$ArrowDown
  ARROW_UP_KEY_NAME: 'Arrow_Up',
  ARROW_DOWN_KEY_NAME: 'Arrow_Down',
  ERROR_MESSAGE_PROP: '_hasErrors',
  EXISTS_FILTER_CLAUSE: 'hasFilterClause',
  IS_FILTER_CLAUSE_APPLIED: 'isImplicitFilterApplied',
  ICONS: {
    PROGRESS: 0,
    OPEN_IN_FORM: 1,
    SEPARATOR1: 2,
    EDIT_IN_GRID: 3,
    CANCEL: 4,
    SEPARATOR2: 5,
    SAVE: 6
  },

  SUPPORTED_SUMMARY_FUNCTIONS: ['count', 'avg', 'min', 'max', 'sum']
});

if (!isc.Browser.isIE) {
  isc.OBViewGrid.addProperties({
    enforceVClipping: true // To avoid apply in IE, since it moves the grid row content to the top of each cell (issue 17884)
  });
}

// = OBViewGrid =
// The OBViewGrid is the Openbravo specific subclass of the Smartclient
// ListGrid.
isc.OBViewGrid.addProperties({
  // ** {{{ view }}} **
  // The view member contains the pointer to the composite canvas which
  // handles this form
  // and the grid and other related components.
  view: null,
  // ** {{{ editGrid }}} **
  // Controls if an edit link column is created in the grid, set to false to
  // prevent this.
  editGrid: true,

  textMatchStyle: 'substring',

  // ** {{{ editLinkFieldProperties }}} **
  // The properties of the ListGridField created for the edit links.
  editLinkFieldProperties: {
    type: 'text',
    canSort: false,
    canReorder: false,
    frozen: true,
    canFreeze: false,
    canEdit: false,
    canGroupBy: false,
    canHide: false,
    showTitle: true,
    title: '&nbsp;',
    // autoFitWidth: true,
    canDragResize: false,
    canFilter: true,
    autoExpand: false,
    filterEditorType: 'StaticTextItem',
    name: isc.OBViewGrid.EDIT_LINK_FIELD_NAME
  },

  editLinkColNum: -1,

  // ** {{{ dataPageSize }}} **
  // The data page size used for loading paged data from the server.
  dataPageSize: 100,

  fetchDelay: 500,

  autoFitFieldWidths: true,
  autoFitWidthApproach: 'title',
  canAutoFitFields: false,
  minFieldWidth: 75,
  width: '100%',
  height: '100%',

  showSortArrow: 'field',
  autoFetchTextMatchStyle: 'substring',
  showFilterEditor: true,
  canEdit: true,
  alternateRecordStyles: true,
  canReorderFields: true,
  canFreezeFields: true,
  canAddFormulaFields: true,
  canAddSummaryFields: true,

  canGroupBy: true,
  showGroupSummaryInHeader: true,
  showGroupSummary: true,
  showGroupTitleColumn: false,
  groupByMaxRecords: 1000,
  selectionAppearance: 'checkbox',
  arrowKeyAction: 'select',
  useAllDataSourceFields: false,
  editEvent: 'none',
  showCellContextMenus: true,
  canOpenRecordEditor: true,
  showDetailFields: true,
  showErrorIcons: false,
  ungroupText: OB.I18N.getLabel('OBUIAPP_ungroup'),
  groupByText: OB.I18N.getLabel('OBUIAPP_GroupBy'),

  allowFilterExpressions: true,
  showFilterExpressionLegendMenuItem: true,

  // internal sc grid property, see the ListGrid source code
  preserveEditsOnSetData: false,

  // enabling this results in a slower user interaction
  // it is better to allow fast grid interaction and if an error occurs
  // dismiss any new records being edited and go back to the edit row
  // which causes the error
  // set to true to solve this issue:
  // https://issues.openbravo.com/view.php?id=21352
  waitForSave: true,
  stopOnErrors: false,
  confirmDiscardEdits: false,
  canMultiSort: false,

  emptyMessage: OB.I18N.getLabel('OBUISC_ListGrid.loadingDataMessage'),
  discardEditsSaveButtonTitle: OB.I18N.getLabel('UINAVBA_Save'),
  editPendingCSSText: null,

  // commented out because of: https://issues.openbravo.com/view.php?id=16515
  // default is much smaller which give smoother scrolling
  // quickDrawAheadRatio: 1.0,
  // drawAheadRatio: 2.0,
  // see this discussion:
  // http://forums.smartclient.com/showthread.php?t=16376
  // scrollRedrawDelay: 20,
  // note: don't set drawAllMaxCells too high as it results in extra reads
  // of data, Smartclient will try to read until drawAllMaxCells has been
  // reached
  drawAllMaxCells: 0,

  // the default is enabled which is a commonly used field
  recordEnabledProperty: '_enabled',

  // keeps track if we are in objectSelectionMode or in toggleSelectionMode
  // objectSelectionMode = singleRecordSelection === true
  singleRecordSelection: false,

  // editing props
  rowEndEditAction: 'next',
  listEndEditAction: 'next',

  fixedRecordHeights: true,

  validateByCell: true,

  currentEditColumnLayout: null,

  recordBaseStyleProperty: '_recordStyle',

  // set to false because of this: https://issues.openbravo.com/view.php?id=16509
  modalEditing: false,
  // set to true because if not all cols are drawn then when doing inline editing
  // errors were reported for undrawn columns
  // need to rework how the FormInitializationComponent sets the valuemap and defaultvalue
  // for non-existing columns this should be stored somewhere, see this reply in
  // the smartclient forum:
  // http://forums.smartclient.com/showthread.php?p=63146
  showAllColumns: true,
  // showGridSummary: true,
  timeFormatter: 'to24HourTime',

  dataProperties: {
    // this means that after an update/add the new/updated row does not fit
    // in the current filter criteria then they are still shown
    // note that if this is set to false that when using the _dummy criteria
    // that the _dummy criteria can mean that new/updated records are not
    // shown in the grid
    neverDropUpdatedRows: true,
    useClientFiltering: true,
    useClientSorting: true,
    fetchDelay: 300,

    // overridden to update the context/request properties for the fetch
    fetchRemoteData: function(serverCriteria, startRow, endRow) {
      // clone to prevent side effects
      var requestProperties = isc.clone(this.context);
      this.context.params = this.grid.getFetchRequestParams(
        requestProperties.params
      );
      if (this.grid.isFilteringExternally) {
        // requests triggered by filtering the grid should always load the first page
        // if after that the user scrolls down, the isFilteringExternally flag will be false and
        // the proper page will be loaded
        startRow = 0;
        endRow = this.grid.dataPageSize;
      } else if (this.grid.refreshingWithSelectedRecord) {
        // if the grid was refreshed with a record selected, use the range that contained that record
        //  instead of using targetRecordId to improve the performance
        startRow = this.grid.selectedRecordInitInterval;
        endRow = this.grid.selectedRecordEndInterval;
        // the startRow and endRow are being modified, so the localData attribute also
        // needs to be updated to wait for the proper records
        this.localData = [];
        this.setRangeLoading(startRow, endRow);
      }
      return this.Super('fetchRemoteData', arguments);
    },

    clearLoadingMarkers: function(start, end) {
      var j;
      if (this.localData) {
        for (j = start; j < end; j++) {
          if (Array.isLoading(this.localData[j])) {
            this.localData[j] = null;
          }
        }
      }
    },

    // always return false otherwise sc switches to local mode
    // which does not work correctly for when doing inserts in form mode
    // at that point the grid.data.allRows is being used which results
    // in mismatches with grid.data.localData, returning false here
    // prevents allRows from being used. In our case we never really
    // want to have all rows cached locally as we do all filtering
    // server side.
    allRowsCached: function() {
      return false;
    },

    transformData: function(newData, dsResponse) {
      var i, length, responseToFilter, newTotalRows;

      // when the data is received from the datasource, time fields are formatted in UTC time. They have to be converted to local time
      if (
        dsResponse &&
        dsResponse.context &&
        (dsResponse.context.operationType === 'fetch' ||
          dsResponse.context.operationType === 'update' ||
          dsResponse.context.operationType === 'add')
      ) {
        if (this.grid) {
          newData = OB.Utilities.Date.convertUTCTimeToLocalTime(
            newData,
            this.grid.completeFields
          );
        }
      }
      // only do this stuff for fetch operations, in other cases strange things
      // happen as update/delete operations do not return the totalRows parameter
      if (
        dsResponse &&
        dsResponse.context &&
        dsResponse.context.operationType !== 'fetch'
      ) {
        return newData;
      }
      // correct the length if there is already data in the localData array
      // only do this if filtering is not the origin action to the datasource request
      // see issue https://issues.openbravo.com/view.php?id=23006
      responseToFilter = false;
      if (
        dsResponse.context &&
        dsResponse.context._dsRequest &&
        dsResponse.context._dsRequest.filtering
      ) {
        responseToFilter = true;
      }

      if (this.localData && !responseToFilter) {
        length = this.localData.length;
        newTotalRows = dsResponse.totalRows;
        for (i = dsResponse.endRow + 1; i < length; i++) {
          if (!Array.isLoading(this.localData[i]) && this.localData[i]) {
            newTotalRows = i + 1;
          } else {
            break;
          }
        }

        // never decrease totalRows because when multiple requests are obtained it can
        // cause an incorrect computation of the whole data size
        if (newTotalRows > dsResponse.totalRows) {
          dsResponse.totalRows = newTotalRows + 1;
          // increase one to request additional page to backend
        }

        // detects if the request was issued due to having scrolled up.
        // in that case, set the totalRows of the response to the length of the localData, to avoid
        // setting the totalRows of the grid to an invalid value
        // to confirm if this is the case, we check if there are rows loaded after the page that was just received
        if (this.rowIsLoaded(dsResponse.endRow + 1)) {
          dsResponse.totalRows = this.localData.length;
        }

        // get rid of old loading markers, this has to be done explicitly
        // as we can return another rowset than requested
        // call with a delay otherwise the grid will keep requesting rows while processing the
        // current rowset
        this.delayCall(
          'clearLoadingMarkers',
          [dsResponse.context.startRow, dsResponse.context.endRow],
          100
        );
      } else {
        // Clear the filtering attribute from the context to prevent including it
        // automatically in the following datasource requests
        if (this.context) {
          delete this.context.filtering;
        }
      }
      if (this.localData && this.localData[dsResponse.totalRows]) {
        this.localData[dsResponse.totalRows] = null;
      }
      return newData;
    },

    shouldUseClientFiltering: function() {
      if (this.forceRefresh) {
        // forcing fetch from server
        return false;
      }
      return this.Super('shouldUseClientFiltering', arguments);
    }
  },

  initWidget: function() {
    var i, vwState;

    // make a copy of the dataProperties otherwise we get
    // change results that values of one grid are copied/coming back
    // in other grids
    this.dataProperties = isc.addProperties({}, this.dataProperties);

    // override setSort to sort by group title when the grouped by
    // column is clicked
    this.groupTreeProperties = {
      grid: this,

      setSort: function(sortSpecifier) {
        var i,
          fld,
          sortSpec = isc.clone(sortSpecifier),
          flds = this.grid.getAllFields(),
          groupByFields = this.grid.getGroupByFields();

        if (groupByFields && sortSpec && sortSpec[0]) {
          for (i = 0; i < flds.length; i++) {
            fld = flds[i];
            if (
              groupByFields.contains(fld.name) &&
              (fld.name === sortSpec[0].property ||
                fld.displayField === sortSpec[0].property)
            ) {
              sortSpec[0].property = 'groupValue';
              break;
            }
          }
        }

        return this.Super('setSort', [sortSpec]);
      }
    };

    // re-use getCellValue to handle count and related functions
    this.summaryRowProperties = {
      showRecordComponents: false,
      cellHoverHTML: this.cellHoverHTML,

      getCellAlign: function(record, rowNum, colNum) {
        var fld = this.getFields()[colNum],
          isRTL = this.isRTL(),
          func = this.getGridSummaryFunction(fld),
          isSummary =
            record &&
            (record[this.groupSummaryRecordProperty] ||
              record[this.gridSummaryRecordProperty]);

        // the count of a character column should also be right aligned
        if (isSummary && func === 'count') {
          return isRTL ? isc.Canvas.LEFT : isc.Canvas.RIGHT;
        }

        return this.Super('getCellAlign', arguments);
      },

      // only set active view but don't do any context menu
      cellContextClick: function() {
        this.view.setAsActiveView();
        return false;
      },

      view: this.view,

      getCellValue: function(record, recordNum, fieldNum, gridBody) {
        var field = this.getField(fieldNum),
          func = this.parentElement.getGridSummaryFunction(field),
          value =
            record && field
              ? field.displayField
                ? record[field.displayField]
                : record[field.name]
              : null;

        // get the summary function from the main grid
        if (!func) {
          delete field.summaryFunction;
        } else {
          field.summaryFunction = func;
        }

        // handle count much simpler than smartclient does
        // so no extra titles or formatting
        if (record && func === 'count' && value >= 0) {
          return value;
        }

        return this.Super('getCellValue', arguments);
      }
    };

    var localEditLinkField;
    if (this.editGrid) {
      // add the edit pencil in the beginning
      localEditLinkField = isc.addProperties({}, this.editLinkFieldProperties);
      localEditLinkField.width = this.editLinkColumnWidth;
      this.fields.unshift(localEditLinkField);
      // is the column after the checkbox field
      this.editLinkColNum = 1;
    }

    this.editFormDefaults = isc.addProperties(
      {},
      isc.clone(OB.ViewFormProperties),
      this.editFormDefaults
    );

    // added for showing counts in the filtereditor row
    this.checkboxFieldProperties = isc.addProperties(
      {},
      this.checkboxFieldProperties | {},
      {
        canFilter: true,
        // frozen is much nicer, but check out this forum discussion:
        // http://forums.smartclient.com/showthread.php?p=57581
        frozen: true,
        canFreeze: true,
        showHover: true,
        prompt: OB.I18N.getLabel('OBUIAPP_GridSelectAllColumnPrompt'),
        filterEditorType: 'StaticTextItem'
      }
    );

    var grid = this;
    var menuItems = [
      {
        title: OB.I18N.getLabel('OBUIAPP_CreateRecordInGrid'),
        click: function() {
          grid.deselectAllRecords();
          grid.startEditingNew();
        }
      },
      {
        title: OB.I18N.getLabel('OBUIAPP_CreateRecordInForm'),
        click: function() {
          grid.deselectAllRecords();
          grid.view.newDocument();
        }
      }
    ];

    if (this.showSortArrow === 'field') {
      // solves https://issues.openbravo.com/view.php?id=17362
      this.showSortArrow = isc.ListGrid.BOTH;
      if (!this.lazyFiltering) {
        this.sorterDefaults = {};
      }
    }

    // TODO: add dynamic part of readonly (via setWindowSettings: see issue 17441)
    // add context-menu only if 'new' is allowed in tab definition
    if (
      this.uiPattern !== 'SR' &&
      this.uiPattern !== 'RO' &&
      this.uiPattern !== 'ED'
    ) {
      this.contextMenu = this.getMenuConstructor().create({
        items: menuItems
      });
      this.contextMenu.show = function() {
        var me = this;
        // If not in the header tab, and no parent is selected, do not show the context menu
        // See issue https://issues.openbravo.com/view.php?id=21787
        if (!grid.view.hasValidState()) {
          return;
        }
        if (grid.isGrouped) {
          return;
        }
        if (!grid.view.roleCanCreateRecords()) {
          return;
        }
        if (!grid.view.isActiveView()) {
          // The view where the context menu is being opened must be active
          // See issue https://issues.openbravo.com/view.php?id=20872
          grid.view.setAsActiveView(true);
          setTimeout(function() {
            me.Super('show', arguments);
          }, 10);
        } else {
          me.Super('show', arguments);
        }
      };
    }

    if (this.view.isRootView && this.view.standardWindow.emptyFilterClause) {
      // this.view.standardWindow.emptyFilterClause will be true if the grid is being built based on
      // an URL that was obtained from a grid that either did not have originally a filterClause or
      // whose filters had been cleared
      delete this.filterClause;
    }

    var ret = this.Super('initWidget', arguments);

    if (!this.allowSummaryFunctions) {
      this.showGridSummary = false;
    } else {
      // only show summary rows if there are summary functions
      for (i = 0; i < this.getFields().length; i++) {
        if (this.getFields()[i].summaryFunction && !this.lazyFiltering) {
          this.showGridSummary = true;
        }
      }
    }

    // only personalize if there is a professional license
    if (
      !OB.Utilities.checkProfessionalLicense(null, true) &&
      this.view.standardWindow
    ) {
      vwState = this.view.standardWindow.getDefaultGridViewState(
        this.view.tabId
      );
      if (vwState) {
        this.setViewState(vwState);
      }
    }

    if (this.view && this.view.deferOpenNewEdit) {
      this.noDataEmptyMessage =
        '<span class="' +
        this.emptyMessageStyle +
        '">' +
        OB.I18N.getLabel('OBUIAPP_GridFilterNoResults') +
        '</span>' +
        '<span onclick="window[\'' +
        this.ID +
        '\'].clearFilter();" class="' +
        this.emptyMessageLinkStyle +
        '">' +
        OB.I18N.getLabel('OBUIAPP_GridClearFilter') +
        '</span>';
    } else if (this.lazyFiltering) {
      this.noDataEmptyMessage =
        '<span class="' +
        this.emptyMessageStyle +
        '">' +
        OB.I18N.getLabel('OBUIAPP_LazyFilteringNoFetch') +
        '</span>';
    } else {
      this.noDataEmptyMessage =
        '<span class="' +
        this.emptyMessageStyle +
        '">' +
        OB.I18N.getLabel('OBUISC_ListGrid.loadingDataMessage') +
        '</span>'; // OB.I18N.getLabel('OBUIAPP_GridNoRecords')
    }
    this.filterNoRecordsEmptyMessage =
      '<span class="' +
      this.emptyMessageStyle +
      '">' +
      OB.I18N.getLabel('OBUIAPP_GridFilterNoResults') +
      '</span>' +
      '<span onclick="window[\'' +
      this.ID +
      '\'].clearFilter();" class="' +
      this.emptyMessageLinkStyle +
      '">' +
      OB.I18N.getLabel('OBUIAPP_GridClearFilter') +
      '</span>';

    return ret;
  },

  clearFilter: function() {
    // hide the messagebar
    this.view.messageBar.hide();
    this._cleaningFilter = true;
    this.Super('clearFilter', arguments);
    delete this._cleaningFilter;
  },

  // select the first field after the frozen fields
  // as the one to use for grouping headers
  getGroupTitleField: function() {
    var frozenFields = this.frozenFields;
    if (frozenFields) {
      // first field after frozen section
      return this.getField(frozenFields.length).name;
    }
    // field number 2 is the first one after the standard frozen section
    return this.getField(2).name;
  },

  // prevent a jscript error if there are no group summary functions
  getGroupSummaryData: function() {
    var ret = this.Super('getGroupSummaryData', arguments);
    if (isc.isAn.Array(ret) && !ret[0]) {
      return [{}];
    }
    return ret;
  },

  // Overridden to sort before grouping, so that groups are sorted
  // and open initial group, move the group field to the left,
  // or put it back when ungrouping
  groupBy: function(fields) {
    var fld, currentGroupByFields, currentGroupByField;

    // move the current group column to where it came from
    currentGroupByFields = this.getGroupByFields();

    // no changes go away
    if (!currentGroupByFields && !fields) {
      return;
    } else if (fields === currentGroupByFields) {
      return;
    }

    if (currentGroupByFields && currentGroupByFields[0]) {
      currentGroupByField = this.getField(currentGroupByFields[0]);
      currentGroupByField.canReorder = true;
      currentGroupByField.canHide = true;
      this.reorderField(
        this.getFieldNum(currentGroupByField),
        currentGroupByField.previousFieldNum
      );
    }

    // first sort so that groups are correctly sorted
    if (fields) {
      if (isc.isAn.Array(fields)) {
        fld = fields[0];
      } else {
        fld = fields;
      }
      this.getField(fld).previousFieldNum = this.getFieldNum(fld);
      fld = this.getField(fld);
      fld.canReorder = false;
      fld.canHide = false;
      this.reorderField(this.getFieldNum(fld), 0);
      this.sort(fld);
    }

    this.Super('groupBy', arguments);

    this.view.toolBar.updateButtonState(true);

    // when there was already a group open, changing the group by
    // starts with all groups closed, explicitly open the first group
    if (fields && currentGroupByFields) {
      this.openInitialGroups();
    }

    this.view.standardWindow.storeViewState();
  },

  clearGroupBy: function() {
    var currentGroupByFields, currentGroupByField;

    // reason for clearing was large dataset, tell the user
    if (this.data && this.data.getLength() > this.groupByMaxRecords) {
      // move the current group column to where it came from
      currentGroupByFields = this.getGroupByFields();
      if (currentGroupByFields && currentGroupByFields[0]) {
        currentGroupByField = this.getField(currentGroupByFields[0]);
        currentGroupByField.canReorder = true;
        currentGroupByField.canHide = true;
        this.reorderField(
          this.getFieldNum(currentGroupByField),
          currentGroupByField.previousFieldNum
        );
      }

      this.Super('clearGroupBy');

      this.view.standardWindow.storeViewState();

      isc.say(
        OB.I18N.getLabel('OBUIAPP_MaxGroupingReached', [this.groupByMaxRecords])
      );
    } else {
      this.Super('clearGroupBy');
    }
  },

  // Overrides the standard SC function as that function
  // also returns the default summary function from the
  // type definition. We only want the explicitly set
  // summary functions.
  getGridSummaryFunction: function(field) {
    if (!field) {
      return;
    }
    return field.summaryFunction;
  },

  // when the summary information changes, refresh
  // the grid in the correct way
  setSummaryFunctionActions: function(clear) {
    var i, noSummaryFunction;
    if (this.isGrouped) {
      this.regroup();
    }
    if (this.lazyFiltering) {
      this.markForCalculateSummaries();
      return;
    }
    if (!clear) {
      if (!this.showGridSummary) {
        this.setShowGridSummary(true);
      }
      this.recalculateGridSummary();
    } else if (this.showGridSummary) {
      noSummaryFunction = true;
      for (i = 0; i < this.getFields().length; i++) {
        if (this.getFields()[i].summaryFunction) {
          noSummaryFunction = false;
          break;
        }
      }
      if (noSummaryFunction) {
        this.setShowGridSummary(false);
      } else {
        this.recalculateGridSummary();
      }
    }
  },

  setShowGridSummary: function(showGridSummary) {
    if (!this.allowSummaryFunctions) {
      return;
    }
    this.Super('setShowGridSummary', arguments);
  },

  markForCalculateSummaries: function() {
    if (!this.allowSummaryFunctions) {
      return;
    }
    this.Super('markForCalculateSummaries');
  },

  getHeaderContextMenuItems: function(colNum) {
    var field = this.getField(colNum),
      i,
      summarySubMenu = [],
      grid = this,
      groupByFields = this.getGroupByFields(),
      type,
      isNumber,
      menuItems = this.Super('getHeaderContextMenuItems', arguments);

    // remove the group by menu option if the field is grouped
    // and it does not have a submenu
    if (groupByFields && groupByFields.contains(field.name)) {
      for (i = 0; i < menuItems.length; i++) {
        if (menuItems[i].groupItem && !menuItems[i].submenu) {
          menuItems.removeAt(i);
          break;
        }
      }
    }

    if (field && this.allowSummaryFunctions && !field.isComputedColumn) {
      type = isc.SimpleType.getType(field.type);
      isNumber =
        isc.SimpleType.inheritsFrom(type, 'integer') ||
        isc.SimpleType.inheritsFrom(type, 'float');

      if (isNumber && !field.clientClass) {
        summarySubMenu.add({
          title: OB.I18N.getLabel('OBUIAPP_SummaryFunctionSum'),
          // enabled: field.summaryFunction != 'sum',
          checked: field.summaryFunction === 'sum',
          click: function(target, item) {
            field.summaryFunction = 'sum';
            grid.setSummaryFunctionActions();
          }
        });

        summarySubMenu.add({
          title: OB.I18N.getLabel('OBUIAPP_SummaryFunctionAvg'),
          // enabled: field.summaryFunction != 'avg',
          checked: field.summaryFunction === 'avg',
          click: function(target, item) {
            field.summaryFunction = 'avg';
            grid.setSummaryFunctionActions();
          }
        });
      }

      if (!field.clientClass) {
        summarySubMenu.add({
          title: OB.I18N.getLabel('OBUIAPP_SummaryFunctionMin'),
          checked: field.summaryFunction === 'min',
          click: function(target, item) {
            field.summaryFunction = 'min';
            grid.setSummaryFunctionActions();
          }
        });

        summarySubMenu.add({
          title: OB.I18N.getLabel('OBUIAPP_SummaryFunctionMax'),
          checked: field.summaryFunction === 'max',
          click: function(target, item) {
            field.summaryFunction = 'max';
            grid.setSummaryFunctionActions();
          }
        });
      }

      summarySubMenu.add({
        title: OB.I18N.getLabel('OBUIAPP_SummaryFunctionCount'),
        // enabled: field.summaryFunction != 'count',
        checked: field.summaryFunction === 'count',
        click: function(target, item) {
          field.summaryFunction = 'count';
          grid.setSummaryFunctionActions();
        }
      });

      menuItems.add({
        isSeparator: true
      });

      menuItems.add({
        groupItem: true,
        title: OB.I18N.getLabel('OBUIAPP_SetSummaryFunction'),
        fieldName: field.name,
        targetField: field,
        prompt: OB.I18N.getLabel('OBUIAPP_SetSummaryFunction_Description'),
        canSelectParent: true,
        submenu: summarySubMenu
      });

      if (field.summaryFunction) {
        menuItems.add({
          title: OB.I18N.getLabel('OBUIAPP_ClearSummaryFunction'),
          targetField: field,
          click: function(target, item) {
            delete field.summaryFunction;
            grid.setSummaryFunctionActions(true);
          }
        });
      }

      menuItems.add({
        title: OB.I18N.getLabel('OBUIAPP_ClearSummaries'),
        targetField: field,
        click: function(target, item) {
          var i, fld;
          for (i = 0; i < grid.getFields().length; i++) {
            fld = grid.getFields()[i];
            delete fld.summaryFunction;
          }
          grid.setSummaryFunctionActions(true);
        }
      });
    }

    // add the summary functions
    return menuItems;
  },

  // overridden to load all data in one request
  requestVisibleRows: function() {
    if (this.refreshingWithRecordSelected || this.refreshingWithScrolledGrid) {
      // don't make a request for the visible rows if the grid is already being refreshed
      return;
    }
    // fake smartclient to think that there groupByMaxRecords + 1 records
    if (this.data && this.isGrouped && !this.data.allRows) {
      this.data.totalRows = this.groupByMaxRecords + 1;
    }
    this.Super('requestVisibleRows', arguments);
  },

  // Overridden to make sure that the group header is not shown in
  // the frozen body
  getGroupNodeHTML: function(node, gridBody) {
    var isFrozenBody = this.frozenBody === gridBody;
    if (this.frozenBody && isFrozenBody) {
      return this.emptyCellValue;
    }
    var state = this.data.isOpen(node) ? 'opened' : 'closed',
      url = isc.Img.urlForState(this.groupIcon, null, null, state),
      iconIndent = isc.Canvas.spacerHTML(this.groupIconPadding, 1),
      groupIndent = isc.Canvas.spacerHTML(
        (this.data.getLevel(node) - 1) * this.groupIndentSize +
          this.groupLeadingIndent,
        1
      );
    var img = this.imgHTML(url, this.groupIconSize, this.groupIconSize);
    var retStr = this.canCollapseGroup
      ? groupIndent + img + iconIndent + this.getGroupTitle(node)
      : groupIndent + iconIndent + this.getGroupTitle(node);

    return retStr;
  },

  filterEditorSubmit: function() {
    // hide the messagebar
    this.view.messageBar.hide();
    this.Super('filterEditorSubmit', arguments);
  },

  // destroy the context menu also
  // see why this needs to be done in the
  // documentation of canvas.contextMenu in Canvas.js
  destroy: function() {
    var i,
      field,
      fields = this.getFields(),
      editorProperties,
      len = fields.length,
      ds,
      dataSources = [];

    if (this.getDataSource()) {
      // will get destroyed in the super class then
      this.getDataSource().potentiallyShared = false;
    }

    for (i = 0; i < len; i++) {
      field = fields[i];
      editorProperties = field && field.editorProperties;
      ds = editorProperties && editorProperties.optionDataSource;
      if (ds) {
        dataSources.push(ds);
      }
    }

    if (this.contextMenu) {
      this.contextMenu.destroy();
      this.contextMenu = null;
    }

    this.Super('destroy', arguments);

    len = dataSources.length;

    for (i = 0; i < len; i++) {
      ds = dataSources[i];
      if (ds) {
        ds.destroy();
        ds = null;
      }
    }
  },

  setData: function(data) {
    if (typeof data !== 'undefined' && data !== null) {
      data.grid = this;
    }
    this.Super('setData', arguments);
  },

  refreshFields: function() {
    this.setFields(this.completeFields.duplicate());
  },

  setReadOnlyMode: function() {
    if (this.uiPattern !== 'RO') {
      this.uiPattern = 'RO';
      this.canEdit = false;
      if (this.contextMenu) {
        this.contextMenu.destroy();
        this.contextMenu = null;
      }
    }
  },

  draw: function() {
    var drawnBefore = this.isDrawn(),
      i,
      form,
      item,
      items,
      length;
    this.enableShortcuts();
    this.Super('draw', arguments);

    // set the focus in the filter editor
    if (
      this.view &&
      this.view.isActiveView() &&
      !drawnBefore &&
      this.isVisible() &&
      this.getFilterEditor() &&
      this.getFilterEditor().getEditForm()
    ) {
      // there is a filter editor
      form = this.getFilterEditor().getEditForm();

      // compute a focus item, set focus with some delay
      // to give everyone time to be ready
      if (!form.getFocusItem()) {
        items = form.getItems();
        length = items.length;

        for (i = 0; i < length; i++) {
          item = items[i];
          if (item.getCanFocus() && !item.isDisabled()) {
            item.delayCall('focusInItem', null, 100);
            break;
          }
        }
      } else {
        form.getFocusItem().delayCall('focusInItem', null, 100);
      }
    }
  },

  // add the properties from the form
  addFormProperties: function(props) {
    isc.addProperties(this.editFormDefaults, props);
  },

  getCellVAlign: function() {
    return 'center';
  },

  getCellAlign: function(record, rowNum, colNum) {
    var fld = this.getFields()[colNum],
      isRTL = this.isRTL(),
      func = this.getGridSummaryFunction(fld),
      isSummary =
        record &&
        (record[this.groupSummaryRecordProperty] ||
          record[this.gridSummaryRecordProperty]);
    if (!fld.clientClass && rowNum === this.getEditRow()) {
      if (fld.editorType === 'OBCheckboxItem') {
        return isRTL ? isc.Canvas.RIGHT : isc.Canvas.LEFT;
      } else {
        return isc.Canvas.CENTER;
      }
    }

    if (isSummary && func === 'count') {
      return isRTL ? isc.Canvas.LEFT : isc.Canvas.RIGHT;
    }

    return this.Super('getCellAlign', arguments);
  },

  getFieldByName: function(fieldName) {
    return this.getFields().find('name', fieldName);
  },

  // overridden to support hover on the header for the checkbox field
  setFieldProperties: function(field, properties) {
    var localField = field;
    if (isc.isA.Number(localField)) {
      localField = this.fields[localField];
    }
    if (this.isCheckboxField(localField) && properties) {
      properties.showHover = true;
      properties.prompt = OB.I18N.getLabel('OBUIAPP_GridSelectAllColumnPrompt');
    }

    return this.Super('setFieldProperties', arguments);
  },

  reorderField: function(fieldNum, moveToPosition) {
    var res;
    if (this.showGridSummary) {
      res = this.handleSummaryFunctionGrid(fieldNum, moveToPosition);
    } else {
      res = this.Super('reorderField', arguments);
    }
    this.view.standardWindow.storeViewState();
    return res;
  },

  handleSummaryFunctionGrid: function(oldPosition, newPosition) {
    var res;
    this.summaryRowProperties = {};
    this.summaryRowProperties.isBeingReordered = true;
    this.summaryRowProperties.oldPosition = oldPosition;
    this.summaryRowProperties.newPosition = newPosition;
    this.showGridSummary = false;
    res = this.Super('reorderField', arguments);
    this.setShowGridSummary(true);
    delete this.summaryRowProperties.isBeingReordered;
    delete this.summaryRowProperties.oldPosition;
    delete this.summaryRowProperties.newPosition;
    return res;
  },

  hideField: function(field, suppressRelayout) {
    var res;
    this._hidingField = true;
    this._savedEditValues = this.getEditValues(this.getEditRow());
    res = this.Super('hideField', arguments);
    delete this._savedEditValues;
    delete this._hidingField;
    this.view.standardWindow.storeViewState();
    // Only refresh grid content when lazyFiltering is not on
    if (!this.lazyFiltering) {
      this.refreshContents();
    }
    return res;
  },

  showField: function(field, suppressRelayout) {
    var res;
    // Do not allow to add a new field while the grid is being edited. Adding a new field implies a grid refresh,
    // and the refresh toolbar button is disabled while the grid/form is being edited
    if (this.view.isEditingGrid) {
      this.view.messageBar.setMessage(
        isc.OBMessageBar.TYPE_ERROR,
        OB.I18N.getLabel('OBUIAPP_Error'),
        OB.I18N.getLabel('OBUIAPP_NotAddingFieldsWhileGridEditing')
      );
      return;
    }
    this._showingField = true;
    this._savedEditValues = this.getEditValues(this.getEditRow());
    res = this.Super('showField', arguments);
    delete this._savedEditValues;
    delete this._showingField;
    this.view.standardWindow.storeViewState();
    this.invalidateCache();
    // Only refresh grid content when lazyFiltering is not on
    if (!this.lazyFiltering) {
      this.refreshContents();
    }
    return res;
  },

  resizeField: function(fieldNum, newWidth, storeWidth) {
    var res = this.Super('resizeField', arguments);
    this.view.standardWindow.storeViewState();
    return res;
  },

  // also store the filter criteria
  getViewState: function(returnObject, includeFilter) {
    var i,
      fld,
      state = this.Super('getViewState', [returnObject || true]);

    if (includeFilter) {
      state.filter = this.getCriteria();
      if (state.filter) {
        // store the foreign key filter auxiliary cache. this is needed if the grid contains any filter column whose current filter type is 'id'
        state.filterAuxCache = this.getFKFilterAuxiliaryCache(state.filter);
      }
      if (!this.filterClause) {
        state.noFilterClause = true;
      }
    }

    state.filterClause = this.filterClause;

    // set summary information, can not be stored in the field state
    // because smartclient does not provide a nice override point
    // when setting the fieldstate back to also set the summary function
    state.summaryFunctions = {};
    for (i = 0; i < this.getAllFields().length; i++) {
      fld = this.getAllFields()[i];
      if (fld.summaryFunction && isc.isA.String(fld.summaryFunction)) {
        state.summaryFunctions[fld.name] = fld.summaryFunction;
      }
    }

    // get rid of the selected state
    delete state.selected;

    this.deleteSelectedParentRecordFilter(state);

    if (returnObject) {
      return state;
    }
    return '(' + isc.Comm.serialize(state, false) + ')';
  },

  setViewState: function(state, settingDefault) {
    var localState, i, fld, hasSummaryFunction, hasDefaultSavedView;

    localState = this.evalViewState(state, 'viewState');

    // strange case, sometimes need to call twice
    if (isc.isA.String(localState)) {
      localState = this.evalViewState(state, 'viewState');
    }

    if (!localState) {
      return;
    }

    // by default, there are no summary functions
    hasSummaryFunction = false;
    this.setShowGridSummary(false);

    if (this.getDataSource()) {
      // old versions stored selected records in grid view, this can cause
      // problems if record is not selected yet
      delete localState.selected;
      this.deselectAllRecords();

      if (localState.summaryFunctions) {
        for (i = 0; i < this.getAllFields().length; i++) {
          fld = this.getAllFields()[i];
          // summary functions are not allowed in computed columns
          if (localState.summaryFunctions[fld.name] && !fld.isComputedColumn) {
            hasSummaryFunction = true;
            fld.summaryFunction = localState.summaryFunctions[fld.name];
          } else {
            delete fld.summaryFunction;
          }
        }
      }
      // remove focus as this results in blur behavior before the
      // (filter)editor is redrawn with new fields when
      // doing setviewstate
      // https://issues.openbravo.com/view.php?id=21249
      if (this.getEditForm() && this.getEditForm().getFocusItem()) {
        this.getEditForm().getFocusItem().hasFocus = false;
      }
      if (
        this.filterEditor &&
        this.filterEditor.getEditForm() &&
        this.filterEditor.getEditForm().getFocusItem()
      ) {
        this.filterEditor.getEditForm().getFocusItem().hasFocus = false;
      }

      this.deleteSelectedParentRecordFilter(localState);

      if (
        settingDefault &&
        localState.group &&
        localState.group.groupByFields
      ) {
        // Setting default view, at this point fetch data is not already performed,
        // confings as field group are done in local with data, so not applying them
        // till fetch callback. Marking now grid to reaply state afterwards
        // see issue #25119
        if (this.view && this.view.standardWindow) {
          this.view.standardWindow.requiredReapplyViewState = true;
          this.view.standardWindow.gridsToReapply =
            this.view.standardWindow.gridsToReapply || [];
          // push only what is pending to be reapplied
          this.view.standardWindow.gridsToReapply.push({
            view: this,
            state: {
              group: isc.shallowClone(localState.group)
            }
          });
          localState.group.groupByFields = '';
        }
      }

      this.Super('setViewState', [
        '(' + isc.Comm.serialize(localState, false) + ')'
      ]);

      hasDefaultSavedView =
        this.view &&
        this.view.standardWindow &&
        this.view.standardWindow.checkIfDefaultSavedView();
      if (hasSummaryFunction && (!settingDefault || !hasDefaultSavedView)) {
        // setting summary functions only once, if not causes several requests (see issue #27157)
        // it is set when setting saved view, or setting defaults (grid configuration) if there is no saved view
        this.recalculateGridSummary();
        this.setShowGridSummary(true);
      }

      // Focus on the first filterable item
      if (this.view.isActiveView()) {
        this.focusInFirstFilterEditor();
      }
    }

    if (localState.noFilterClause) {
      if (this.filterClause) {
        if (this.data) {
          this.data.forceRefresh = true;
        }
      }
      this.filterClause = null;
      if (this.view.messageBar) {
        this.view.messageBar.hide();
      }
    } else if (localState.filterClause) {
      this.filterClause = localState.filterClause;
    }

    // and no additional filter clauses passed in
    if (
      localState.filter &&
      this.view.tabId !== this.view.standardWindow.additionalCriteriaTabId
    ) {
      // a filtereditor but no editor yet
      // set it in the initialcriteria of the filterEditro
      if (this.filterEditor && !this.filterEditor.getEditForm()) {
        this.filterEditor.setValuesAsCriteria(localState.filter);
      }
      // if any filter fields of the stored view was using the 'id' filter type, load its auxiliary cache
      this.loadFilterAuxiliaryCache(localState.filterAuxCache);
      // this initial criteria needs to be removed in order to properly
      // manage filtering clean up
      this.initialCriteriaSetBySavedView = true;
      this.setCriteria(localState.filter);
    }
  },

  viewHasFieldsNotInGrid: function(viewGridDefinition) {
    var state = this.evalViewState(viewGridDefinition, 'viewState'),
      i;
    if (state && state.field) {
      var viewGridDefinitionFields = isc.JSON.decode(state.field) || [];
      for (i = 0; i < viewGridDefinitionFields.length; i++) {
        var name = viewGridDefinitionFields[i].name;
        var isVisible = viewGridDefinitionFields[i].visible;
        if (isVisible !== false && !this.getFieldByName(name)) {
          return true;
        }
      }
    }
    return false;
  },

  // loads the foreign key filter auxiliary cache of all the filter fields that were using the 'id' filter type when the view was saved
  loadFilterAuxiliaryCache: function(filterAuxCache) {
    var i, cacheElement, filterField;
    if (!this.canLoadFilterAuxiliaryCache(filterAuxCache)) {
      return;
    }
    for (i = 0; i < filterAuxCache.length; i++) {
      cacheElement = filterAuxCache[i];
      filterField = this.filterEditor
        .getEditForm()
        .getField(cacheElement.fieldName);
      filterField.filterType = 'id';
      if (filterField) {
        filterField.filterAuxCache = cacheElement.cache;
      }
    }
  },

  canLoadFilterAuxiliaryCache: function(filterAuxCache) {
    return (
      filterAuxCache &&
      isc.isA.Array(filterAuxCache) &&
      filterAuxCache.length > 0 &&
      this.filterEditor &&
      this.filterEditor.getEditForm()
    );
  },

  // overridden to also store the group mode
  // http://forums.smartclient.com/showthread.php?p=93877#post93877
  getGroupState: function() {
    var i,
      fld,
      state = this.Super('getGroupState', arguments),
      result = {};
    result.groupByFields = state;
    result.groupingModes = {};
    for (i = 0; i < this.getFields().length; i++) {
      fld = this.getFields()[i];
      if (fld.groupingMode) {
        result.groupingModes[fld.name] = fld.groupingMode;
      }
    }
    return result;
  },

  setGroupState: function(state) {
    var fld, key;
    if (state && (state.groupByFields || state.groupByFields === '')) {
      if (state.groupingModes) {
        for (key in state.groupingModes) {
          if (Object.prototype.hasOwnProperty.call(state.groupingModes, key)) {
            fld = this.getField(key);
            if (fld) {
              fld.groupingMode = state.groupingModes[key];
            }
          }
        }
      }
      this.Super('setGroupState', [state.groupByFields]);
    } else {
      // older state definition
      this.Super('setGroupState', arguments);
    }
  },

  // Deletes the implicit filter on the selected record of the parent
  deleteSelectedParentRecordFilter: function(state) {
    var i, filterLength, filterItem;
    if (state.filter) {
      filterLength = state.filter.criteria.length;
      for (i = 0; i < filterLength; i++) {
        filterItem = state.filter.criteria[i];
        if (filterItem.fieldName === this.view.parentProperty) {
          // This way it is ensured that the sub tabs will not show the registers associated with
          // the register of its parent tab that was selected when the filter was created
          state.filter.criteria[i].value = '-1';
          break;
        }
      }
    }
  },

  getSummaryRowDataSource: function() {
    if (this.getSummarySettings()) {
      return this.getDataSource();
    }
  },

  getSummaryRowFetchRequestConfig: function() {
    var summary = this.getSummarySettings(),
      config = this.Super('getSummaryRowFetchRequestConfig', arguments);
    if (summary) {
      config.params = config.params || {};
      config.params._summary = summary;
      this.fetchingSummaryRow = true;
      config.params = this.getFetchRequestParams(config.params);
      delete this.fetchingSummaryRow;
    }
    return config;
  },

  getSummarySettings: function() {
    var fld, i, summary;

    for (i = 0; i < this.getFields().length; i++) {
      fld = this.getFields()[i];
      if (
        fld.summaryFunction &&
        isc.OBViewGrid.SUPPORTED_SUMMARY_FUNCTIONS.contains(fld.summaryFunction)
      ) {
        summary = summary || {};
        summary[fld.displayField || fld.name] = fld.summaryFunction;
      }
    }
    return summary;
  },

  setView: function(view) {
    var dataPageSizeaux, length, i, crit, groupByMaxRecords;

    this.view = view;

    this.editFormDefaults.view = view;

    if (this.getField(this.view.parentProperty)) {
      this.getField(this.view.parentProperty).canFilter = false;
      this.getField(this.view.parentProperty).canEdit = false;
    }

    if (
      this.view.tabId === this.view.standardWindow.additionalCriteriaTabId &&
      this.view.standardWindow.additionalCriteria
    ) {
      crit = isc.JSON.decode(
        unescape(this.view.standardWindow.additionalCriteria)
      );
      this.setCriteria(crit);
      if (this.view.standardWindow.fkCache) {
        this.fkCache = isc.JSON.decode(
          unescape(this.view.standardWindow.fkCache)
        );
        // cannot apply the fkCache yet because the grid might not have a filter editor yet
      }
      delete this.view.standardWindow.additionalCriteria;
    }
    // if there is no autoexpand field then just divide the space
    if (!this.getAutoFitExpandField()) {
      length = this.fields.length;

      // nobody, then give all the fields a new size, dividing
      // the space among them
      for (i = 0; i < length; i++) {
        // ignore the first 2 fields, the checkbox and edit/form
        // buttons
        if (i > 1) {
          this.fields[i].width = '*';
        }
      }
    }
    // Modify the quantity of lines to count per Window
    dataPageSizeaux = OB.PropertyStore.get(
      'dataPageSize',
      this.view.standardWindow.windowId
    );
    this.dataPageSize = dataPageSizeaux ? +dataPageSizeaux : 100;

    groupByMaxRecords = OB.PropertyStore.get(
      'OBUIAPP_GroupingMaxRecords',
      this.view.standardWindow.windowId
    );
    this.groupByMaxRecords = groupByMaxRecords ? +groupByMaxRecords : 1000;
    this.canGroupBy =
      'Y' ===
      OB.PropertyStore.get(
        'OBUIAPP_GroupingEnabled',
        this.view.standardWindow.windowId
      );
  },

  show: function() {
    var ret = this.Super('show', arguments);

    this.view.toolBar.updateButtonState(true);

    this.updateRowCountDisplay();

    this.resetEmptyMessage();

    return ret;
  },

  headerClick: function(fieldNum, header, autoSaveDone) {
    delete this.wasEditing;
    if (!autoSaveDone) {
      var actionObject = {
        target: this,
        method: this.headerClick,
        parameters: [fieldNum, header, true]
      };
      this.view.standardWindow.doActionAfterAutoSave(actionObject, true);
      return;
    }
    var field = this.fields[fieldNum],
      res;
    if (this.isCheckboxField(field) && this.singleRecordSelection) {
      this.deselectAllRecords();
      this.singleRecordSelection = false;
    }
    res = this.Super('headerClick', arguments);

    if (field.canSort !== false) {
      // saving grid configuration after sorting by new a field
      this.view.standardWindow.storeViewState();
    }
    return res;
  },

  keyPress: function() {
    var response = OB.KeyboardManager.Shortcuts.monitor('OBViewGrid');
    if (response !== false) {
      response = this.Super('keyPress', arguments);
    }
    return response;
  },

  bodyKeyPress: function(event, eventInfo) {
    var response = OB.KeyboardManager.Shortcuts.monitor('OBViewGrid.body');
    if (response !== false) {
      if (
        event &&
        event.keyName === 'Space' &&
        (isc.EventHandler.ctrlKeyDown() ||
          isc.EventHandler.altKeyDown() ||
          isc.EventHandler.shiftKeyDown())
      ) {
        return true;
      }
      response = this.Super('bodyKeyPress', arguments);
    }
    return response;
  },

  editFormKeyDown: function() {
    // Custom method. Only works if the form is an OBViewForm
    var response = OB.KeyboardManager.Shortcuts.monitor('OBViewGrid.editForm');
    if (response !== false) {
      response = this.Super('editFormKeyDown', arguments);
    }
    return response;
  },

  // called when the view gets activated
  setActive: function(active) {
    if (active) {
      this.enableShortcuts();
    } else {
      this.disableShortcuts();
    }
  },

  disableShortcuts: function() {
    OB.KeyboardManager.Shortcuts.set('ViewGrid_EditInGrid', null, function() {
      return true;
    });
    OB.KeyboardManager.Shortcuts.set('ViewGrid_EditInForm', null, function() {
      return true;
    });
  },

  enableShortcuts: function() {
    var me = this,
      ksAction_CancelEditing,
      ksAction_MoveUpWhileEditing,
      ksAction_MoveDownWhileEditing,
      ksAction_DeleteSelectedRecords,
      ksAction_EditInGrid,
      ksAction_EditInForm;

    // This is JUST for the case of an editing row with the whole row in "read only mode"
    ksAction_MoveUpWhileEditing = function() {
      if (me.getEditForm()) {
        var editRow = me.getEditRow();
        me.cancelEditing();
        if (editRow) {
          me.startEditing(editRow - 1);
        }
        return false; // To avoid keyboard shortcut propagation
      } else {
        return true;
      }
    };
    OB.KeyboardManager.Shortcuts.set(
      'ViewGrid_MoveUpWhileEditing',
      'OBViewGrid.body',
      ksAction_MoveUpWhileEditing,
      null,
      {
        key: 'Arrow_Up'
      }
    );

    // This is JUST for the case of an editing row with the whole row in "read only mode"
    ksAction_MoveDownWhileEditing = function() {
      if (me.getEditForm()) {
        var editRow = me.getEditRow();
        me.cancelEditing();
        if (editRow || editRow === 0) {
          me.startEditing(editRow + 1);
        }
        return false; // To avoid keyboard shortcut propagation
      } else {
        return true;
      }
    };
    OB.KeyboardManager.Shortcuts.set(
      'ViewGrid_MoveDownWhileEditing',
      'OBViewGrid.body',
      ksAction_MoveDownWhileEditing,
      null,
      {
        key: 'Arrow_Down'
      }
    );

    ksAction_CancelEditing = function() {
      if (me.getEditForm()) {
        me.cancelEditing();
        // force update of toolbar buttons state
        // https://issues.openbravo.com/view.php?id=31567
        me.view.toolBar.updateButtonState(true);
        return false; // To avoid keyboard shortcut propagation
      } else {
        return true;
      }
    };
    OB.KeyboardManager.Shortcuts.set(
      'ViewGrid_CancelEditing',
      ['OBViewGrid.body', 'OBViewGrid.editForm'],
      ksAction_CancelEditing
    );

    ksAction_DeleteSelectedRecords = function() {
      var isRecordDeleted = me.deleteSelectedRowsByToolbarIcon();
      // Return false to avoid keyboard shortcut propagation
      return !isRecordDeleted;
    };
    OB.KeyboardManager.Shortcuts.set(
      'ViewGrid_DeleteSelectedRecords',
      'OBViewGrid.body',
      ksAction_DeleteSelectedRecords
    );

    ksAction_EditInGrid = function() {
      if (me.getSelectedRecords().length === 1) {
        me.endEditing();
        me.startEditing(me.getRecordIndex(me.getSelectedRecords()[0]));
        return false; // To avoid keyboard shortcut propagation
      } else {
        return true;
      }
    };
    OB.KeyboardManager.Shortcuts.set(
      'ViewGrid_EditInGrid',
      'OBViewGrid.body',
      ksAction_EditInGrid
    );

    ksAction_EditInForm = function() {
      var wasEditingGrid = false,
        autoSaveEditsBackup = me.autoSaveEdits,
        recordToEdit,
        originalValuesOfEditedRow;
      if (me.getSelectedRecords().length === 1) {
        if (me.getEditForm() && me.view && me.view.viewForm) {
          // copy the list of dynamicCols to the viewForm, as otherwise they will not be set until for form
          // makes a call to the FIC (see issue https://issues.openbravo.com/view.php?id=28870)
          me.view.viewForm.dynamicCols = me.getEditForm().dynamicCols;
        }
        if (me.view.isEditingGrid) {
          // do not save the provisional changes
          me.autoSaveEdits = false;
          if (me.getSelectedRecords()[0]._new) {
            // if the record is new set the wasEditingGrid flag to true to prevent
            // doing a FIC request in mode NEW
            wasEditingGrid = true;
            // open the form view with the current values of the edited row
            recordToEdit = me.getEditedRecord(me.getEditRow());
            me.storeValueMaps();
          } else {
            recordToEdit = me.getSelectedRecords()[0];
            // store the original values of the row (previous ot the edition in grid)
            originalValuesOfEditedRow = recordToEdit;
          }
        } else {
          recordToEdit = me.getSelectedRecords()[0];
        }
        me.endEditing();
        me.autoSaveEdits = autoSaveEditsBackup;
        me.view.editRecord(recordToEdit, null, null, wasEditingGrid);
        if (originalValuesOfEditedRow) {
          me.view.viewForm.originalValuesOfEditedRow = originalValuesOfEditedRow;
        }
        delete me.storedValueMaps;
        return false; // To avoid keyboard shortcut propagation
      } else {
        return true;
      }
    };
    OB.KeyboardManager.Shortcuts.set(
      'ViewGrid_EditInForm',
      ['OBViewGrid.body', 'OBViewGrid.editForm'],
      ksAction_EditInForm
    );

    this.Super('enableShortcuts', arguments);
  },

  storeValueMaps: function() {
    var i,
      items,
      editForm = this.getEditForm();
    if (!editForm) {
      return;
    }
    this.storedValueMaps = {};
    items = editForm.getItems();
    for (i = 0; i < items.length; i++) {
      if (items[i].valueMap) {
        this.storedValueMaps[items[i].name] = items[i].valueMap;
      }
    }
  },

  deselectAllRecords: function(preventUpdateSelectInfo, autoSaveDone) {
    // if there is nothing to deselect then don't deselect
    if (!this.getSelectedRecord()) {
      return;
    }
    if (!autoSaveDone) {
      var actionObject = {
        target: this,
        method: this.deselectAllRecords,
        parameters: [preventUpdateSelectInfo, true]
      };
      this.view.standardWindow.doActionAfterAutoSave(actionObject, true);
      return;
    }

    this.allSelected = false;
    var ret = this.Super('deselectAllRecords', arguments);
    this.lastSelectedRecord = null;
    if (!preventUpdateSelectInfo) {
      this.selectionUpdated();
    }
    return ret;
  },

  selectAllRecords: function(autoSaveDone) {
    if (!autoSaveDone) {
      var actionObject = {
        target: this,
        method: this.selectAllRecords,
        parameters: [true]
      };
      this.view.standardWindow.doActionAfterAutoSave(actionObject, true);
      return;
    }
    this.allSelected = true;
    var ret = this.Super('selectAllRecords', arguments);
    this.selectionUpdated();
    return ret;
  },

  updateRowCountDisplay: function() {
    var newValue = '',
      length = isc.isA.Tree(this.data)
        ? this.countGroupContent()
        : this.data.getLength();
    if (length > this.dataPageSize) {
      newValue = '>' + this.dataPageSize;
    } else if (length === 0) {
      newValue = '&nbsp;';
    } else {
      newValue = length;
    }
    if (this.filterEditor && this.filterEditor.getEditForm()) {
      this.filterEditor
        .getEditForm()
        .setValue(isc.OBViewGrid.EDIT_LINK_FIELD_NAME, newValue);
      this.filterEditor
        .getEditForm()
        .getField(isc.OBViewGrid.EDIT_LINK_FIELD_NAME).defaultValue = newValue;
    }
  },

  countGroupContent: function() {
    var i,
      cnt = 0,
      data = this.data.getRange(0, this.groupByMaxRecords + 1);
    for (i = 0; i < data.length; i++) {
      if (!data[i].isFolder) {
        cnt++;
      }
    }
    return cnt;
  },

  refreshContents: function(callback) {
    var selectedValues, context, additionalCriteriaTabId;

    this.resetEmptyMessage();
    this.view.updateTabTitle();
    // apply the fk cache to ensure the identifiers of the filtered foreign keys are shown
    if (this.fkCache) {
      this.loadFilterAuxiliaryCache(this.fkCache);
      // delete it to avoid loading the cache more than once
      delete this.fkCache;
    }

    /*
     * In case the url contains advanced criteria, the initial criteria contains the criteria to be applied. So it should not be deleted.
     * Refer issue https://issues.openbravo.com/view.php?id=23333
     */
    additionalCriteriaTabId = this.view.standardWindow.additionalCriteriaTabId;
    if (
      additionalCriteriaTabId &&
      additionalCriteriaTabId !== this.view.tabId
    ) {
      delete this.initialCriteria;
    }

    // do not refresh if the parent is not selected and we have no data
    // anyway
    // we use this.view.parentView to identify if we are on a child tab
    // this.view.parentProperty was used before but this value could be
    // undefined under some circumstances
    // See issue https://issues.openbravo.com/view.php?id=29665
    if (
      this.view.parentView &&
      (!this.data || !this.data.getLength || this.data.getLength() === 0)
    ) {
      if (this.view.parentView.isShowingTree) {
        selectedValues = this.view.parentView.treeGrid.getSelectedRecords();
      } else {
        selectedValues = this.view.parentView.viewGrid.getSelectedRecords();
      }
      if (
        selectedValues &&
        !this.isOpenDirectMode &&
        selectedValues.length === 0
      ) {
        if (callback) {
          callback();
        }
        // but in this case we should show ourselves also
        if (!this.isVisible()) {
          this.makeVisible();
        }
        return;
      }
    }

    context = {
      showPrompt: false
    };
    this.filterData(this.getCriteria(), callback, context);
  },

  // the dataarrived method is where different actions are done after
  // data has arrived in the grid:
  // - open the edit view if default edit mode is enabled
  // - if the user goes directly to a tab (from a link in another window)
  // then
  // opening the relevant record is done here or if no record is passed grid
  // mode is opened
  // - if there is only one record then select it directly
  dataArrived: function(startRow, endRow) {
    var noSetSession, changeEvent, forceUpdate;
    // do this now, to replace the loading message
    // TODO: add dynamic part of readonly (via setWindowSettings: see issue 17441)
    if (
      this.uiPattern === 'SR' ||
      this.uiPattern === 'RO' ||
      this.uiPattern === 'ED' ||
      !this.view.roleCanCreateRecords()
    ) {
      this.noDataEmptyMessage =
        '<span class="' +
        this.emptyMessageStyle +
        '">' +
        OB.I18N.getLabel('OBUIAPP_NoDataInGrid') +
        '</span>';
    } else {
      this.noDataEmptyMessage =
        '<span class="' +
        this.emptyMessageStyle +
        '">' +
        OB.I18N.getLabel('OBUIAPP_GridNoRecords') +
        '</span>' +
        '<span onclick="this.onclick = new Function(); setTimeout(function() { window[\'' +
        this.ID +
        '\'].view.newRow(); }, 50); return false;" class="' +
        this.emptyMessageLinkStyle +
        '">' +
        OB.I18N.getLabel('OBUIAPP_GridCreateOne') +
        '</span>';
    }
    this.resetEmptyMessage();

    this.discardAllEdits();

    var record,
      ret = this.Super('dataArrived', arguments);
    this.updateRowCountDisplay();

    if (this.getSelectedRecords() && this.getSelectedRecords().length > 0) {
      this.selectionUpdated();
    }

    delete this.view.refreshingData;

    // no data and the grid is not visible, only do this is if the
    // form is not in new mode
    if (
      this.data &&
      this.data.getLength() === 0 &&
      !this.isVisible() &&
      !this.view.viewForm.isNew
    ) {
      this.makeVisible();
    }

    // get the record id from any record
    if (this.isOpenDirectMode && this.data && this.data.getLength() >= 1) {
      // now tell the parent grid to refresh on the basis of this parentRecordId also
      if (this.view.parentView) {
        this.view.parentRecordId = this.data.get(startRow)[
          this.view.parentProperty
        ];

        this.view.parentView.viewGrid.isOpenDirectMode = true;
        // makes sure that the parent refresh will not fire back to cause a child refresh
        this.view.parentView.isOpenDirectModeParent = true;
        // prevents opening edit mode for parent views
        this.view.parentView.viewGrid.isOpenDirectModeParent = true;
        this.view.parentView.viewGrid.targetRecordId = this.view.parentRecordId;
        this.view.parentView.viewGrid.delayCall('refreshContents', [], 10);
      }
    }
    delete this.isOpenDirectMode;

    if (this.initialCriteriaSetBySavedView) {
      delete this.initialCriteria;
      delete this.initialCriteriaSetBySavedView;
    }

    if (!this.targetRecordId) {
      delete this.isOpenDirectModeLeaf;
    }

    if (this.targetOpenNewEdit) {
      delete this.targetOpenNewEdit;
      // not passing record opens new
      this.view.editRecord();
    } else if (this.targetOpenGrid) {
      // direct link from other window but without a record id
      // so just show grid mode
      // don't need to do anything here
      delete this.targetOpenGrid;
    } else if (this.targetRecordId || this.selectedRecordId) {
      // direct link from other tab to a specific record
      this.delayedHandleTargetRecord(startRow, endRow);
    } else if (
      this.view.shouldOpenDefaultEditMode() &&
      !Array.isLoading(this.getRecord(startRow))
    ) {
      // ui-pattern: single record/edit mode
      this.view.openDefaultEditView(this.getRecord(startRow));
    } else if (this.data && this.data.getLength() === 1) {
      // Prevent the selection of an old record
      // See issue https://issues.openbravo.com/view.php?id=26679
      if (!this.view.viewForm.isNew) {
        // one record select it directly
        record = this.getRecord(0);
        // this select method prevents state changing if the record
        // was already selected
        this.doSelectSingleRecord(record);
      }

      // Call to updateButtonState to force a call to the FIC in setsession mode
      // See issue https://issues.openbravo.com/view.php?id=22655
      noSetSession = false;
      changeEvent = false;
      forceUpdate = true;
      this.view.toolBar.updateButtonState(
        noSetSession,
        changeEvent,
        forceUpdate
      );
    } else if (this.lastSelectedRecord) {
      // if nothing was select, select the record again
      if (!this.getSelectedRecord()) {
        // if it is still in the cache ofcourse
        var gridRecord = this.data.find(
          OB.Constants.ID,
          this.lastSelectedRecord.id
        );
        if (gridRecord) {
          this.doSelectSingleRecord(gridRecord);
        }
      } else if (
        this.getSelectedRecords() &&
        this.getSelectedRecords().length !== 1
      ) {
        this.lastSelectedRecord = null;
      }
    }

    if (this.actionAfterDataArrived) {
      this.actionAfterDataArrived();
      this.actionAfterDataArrived = null;
    }

    if (this.data.manualResultSet && !this.data.useClientFiltering) {
      this.data.useClientFiltering = true;
    }
    //  update the state of the toolbar buttons, as the availability of some of them depends on the number of records loaded
    this.view.toolBar.updateButtonState(true);
    return ret;
  },

  removeOrClause: function(criteria) {
    // The original criteria is stored in the position #0
    // The criteria to select the recently created records is stored in position #1..length-1
    return criteria.criteria.get(0);
  },

  refreshGrid: function(callback, newRecordsToBeIncluded, afterFilterCallback) {
    var originalCriteria,
      criteria = {},
      newRecordsCriteria,
      newRecordsLength,
      i,
      index,
      selectedRecordIndex,
      visibleRows,
      filterDataCallback,
      me = this;

    //check whether newRecordsToBeIncluded contains records not part of the current grid and remove them.
    if (
      newRecordsToBeIncluded &&
      newRecordsToBeIncluded.length > 0 &&
      this.data
    ) {
      for (i = 0; i < newRecordsToBeIncluded.length; i++) {
        if (
          this.data.findByKey &&
          !this.data.findByKey(newRecordsToBeIncluded[i])
        ) {
          index = newRecordsToBeIncluded.indexOf(newRecordsToBeIncluded[i]);
          if (index !== -1) {
            newRecordsToBeIncluded.splice(index, 1);
          }
        }
      }
    }

    if (this.getSelectedRecord()) {
      // this property is used to prevent an unneeded request in OBViewGridBody.redraw
      this.refreshingWithSelectedRecord = true;
      // obtain a range that contains the selected record
      selectedRecordIndex = this.getRecordIndex(this.getSelectedRecord());
      if (selectedRecordIndex !== -1) {
        this.selectedRecordId = this.getSelectedRecord()[OB.Constants.ID];
        this.selectedRecordInitInterval =
          selectedRecordIndex - Math.round(this.data.resultSize / 2);
        if (this.selectedRecordInitInterval < 0) {
          this.selectedRecordInitInterval = 0;
        }
        this.selectedRecordEndInterval =
          this.selectedRecordInitInterval + this.data.resultSize;
      }
      this.notRemoveFilter = true;
      if (this.getSelectedRecords().length > 1) {
        this.selectedRecordsBeforeRefresh = [];
        for (i = 0; i < this.getSelectedRecords().length; i++) {
          this.selectedRecordsBeforeRefresh.push(
            this.getSelectedRecords()[i][OB.Constants.ID]
          );
        }
      }
    } else {
      visibleRows = this.getVisibleRows();
      if (visibleRows && visibleRows[0] > 0) {
        // save the index of the record placed in the middle of the viewport to
        // move the scroll to it after receiving the response
        this.recordIndexToScroll = Math.round(
          (visibleRows[0] + visibleRows[1]) / 2
        );
      }
    }
    this.actionAfterDataArrived = callback;
    this.invalidateCache();

    var context = {
      showPrompt: false
    };

    // Removes the 'or' clause, if there is one
    // See note at the function foot
    originalCriteria = this.getCriteria();
    if (this._criteriaWithOrClause) {
      originalCriteria = this.removeOrClause(originalCriteria);
      this._criteriaWithOrClause = false;
    }

    // If a record has to be included in the refresh, it must be included
    // in the filter with an 'or' operator, along with the original filter,
    // but only if there is an original filter
    if (
      newRecordsToBeIncluded &&
      newRecordsToBeIncluded.length > 0 &&
      originalCriteria.criteria.length > 0
    ) {
      // Adds the current record to the criteria
      newRecordsCriteria = [];
      newRecordsLength = newRecordsToBeIncluded.length;
      for (i = 0; i < newRecordsLength; i++) {
        newRecordsCriteria.push({
          fieldName: 'id',
          operator: 'equals',
          value: newRecordsToBeIncluded[i]
        });
      }

      this._criteriaWithOrClause = true;
      criteria._constructor = 'AdvancedCriteria';
      criteria._OrExpression = true; // trick to get a really _or_ in the backend
      criteria.operator = 'or';
      criteria.criteria = [originalCriteria].concat(newRecordsCriteria);
    } else {
      criteria = originalCriteria;
    }
    filterDataCallback = function() {
      var i,
        gridRecord,
        recordIndexes = [];
      if (me.refreshingWithScrolledGrid) {
        // move the scroll to part of the grid that contains the data that was just received to
        // prevent unneded requests (see https://issues.openbravo.com/view.php?id=25811)
        // the adjustment is needed to show the records in the same exact position where they were
        // placed before refreshing the grid, if no records were added/removed
        me.scrollCellIntoView(me.recordIndexToScroll + 1, null, true, true);
      }
      delete me.recordIndexToScroll;
      delete me.refreshingWithScrolledGrid;
      delete me.refreshingWithRecordSelected;
      delete me.selectedRecordInitInterval;
      delete me.selectedRecordEndInterval;
      delete me.selectedRecordId;

      if (me.selectedRecordsBeforeRefresh) {
        for (i = 0; i < me.selectedRecordsBeforeRefresh.length; i++) {
          gridRecord = me.data.find(
            OB.Constants.ID,
            me.selectedRecordsBeforeRefresh[i]
          );
          if (gridRecord !== null) {
            recordIndexes.push(me.getRecordIndex(gridRecord));
          }
        }
        me.singleRecordSelection = false;
        me.selectRecords(recordIndexes);
        if (me.selectedRecordsBeforeRefresh.length !== recordIndexes.length) {
          if (me.view.messageBar.isVisible()) {
            isc.warn(
              OB.I18N.getLabel('OBUIAPP_NumOfSeledtedItemsChange', [
                me.selectedRecordsBeforeRefresh.length,
                recordIndexes.length
              ])
            );
          } else {
            me.view.messageBar.setMessage(
              isc.OBMessageBar.TYPE_WARNING,
              null,
              OB.I18N.getLabel('OBUIAPP_NumOfSeledtedItemsChange', [
                me.selectedRecordsBeforeRefresh.length,
                recordIndexes.length
              ])
            );
          }
        }
        delete me.selectedRecordsBeforeRefresh;
      }

      if (afterFilterCallback) {
        afterFilterCallback();
      }
    };
    this.filterData(criteria, filterDataCallback, context);
    // Set the refreshingWithRecordSelected and refreshingWithScrolledGrid flags to true when needed after
    // actually start filtering the data. These flags will prevent unneeded multiple datasource requests
    if (this.selectedRecordInitInterval !== undefined) {
      this.refreshingWithRecordSelected = true;
    } else if (this.recordIndexToScroll) {
      this.refreshingWithScrolledGrid = true;
    }
    // At this point the original criteria should be restored, to prevent
    // the 'or' clause that was just added to be used in subsequent refreshes.
    // It is not possible to do it here, though, because a this.setCriteria(originalCriteria)
    // would trigger an automatic refresh that would leave without effect that last filterData
    // The additional criteria will be removed in the next call to refreshGrid
  },

  refreshGridFromClientEventHandler: function(callback) {
    this.refreshGrid(null, null, callback);
  },

  // with a delay to handle the target record when the body has been drawn
  delayedHandleTargetRecord: function(startRow, endRow) {
    var recordIndex,
      data = this.data,
      tmpTargetRecordId = this.targetRecordId || this.selectedRecordId;
    if (!tmpTargetRecordId) {
      delete this.isOpenDirectModeLeaf;
      return;
    }
    if (this.body) {
      // don't need it anymore
      delete this.targetRecordId;
      delete this.notRemoveFilter;

      var gridRecord = data.find(OB.Constants.ID, tmpTargetRecordId);

      // no grid record found, stop here
      if (!gridRecord) {
        return;
      }
      recordIndex = this.getRecordIndex(gridRecord);

      if (data.criteria) {
        data.criteria._targetRecordId = null;
      }

      this.doSelectSingleRecord(gridRecord);

      this.scrollCellIntoView(recordIndex, null, true, true);

      // show the form with the selected record
      if (!this.view.isShowingForm && this.isOpenDirectModeLeaf) {
        this.view.editRecord(gridRecord);
      }

      delete this.isOpenDirectModeLeaf;
      delete this.isOpenDirectModeParent;
    } else {
      // wait a bit longer til the body is drawn
      this.delayCall(
        'delayedHandleTargetRecord',
        [startRow, endRow],
        200,
        this
      );
    }
  },

  selectRecordById: function(id, forceFetch) {
    if (forceFetch) {
      this.targetRecordId = id;
      this.filterData(this.getCriteria());
      return;
    }

    var recordIndex,
      gridRecord = this.data.find(OB.Constants.ID, id);
    // no grid record fetch it
    if (!gridRecord) {
      this.targetRecordId = id;
      this.filterData(this.getCriteria());
      return;
    }
    recordIndex = this.getRecordIndex(gridRecord);
    this.scrollRecordIntoView(recordIndex, true);
    this.doSelectSingleRecord(gridRecord);
  },

  filterData: function(criteria, callback, requestProperties) {
    var theView = this.view,
      newCallBack,
      me = this;

    if (!requestProperties) {
      requestProperties = {};
    }
    requestProperties.showPrompt = false;
    requestProperties.filtering = true;

    newCallBack = function() {
      theView.recordSelected();
      delete me.refreshingWithSelectedRecord;
      me.markForRedraw();
      if (typeof callback === 'function') {
        callback();
      }
    };

    return this.Super('filterData', [
      this.convertCriteria(criteria),
      newCallBack,
      requestProperties
    ]);
  },

  fetchData: function(criteria, callback, requestProperties) {
    var theView = this.view,
      newCallBack;

    if (!requestProperties) {
      requestProperties = {};
    }
    requestProperties.showPrompt = false;

    newCallBack = function() {
      if (
        theView.standardWindow &&
        theView.standardWindow.requiredReapplyViewState
      ) {
        theView.standardWindow.reapplyViewStates();
      }

      theView.recordSelected();
      if (callback) {
        callback();
      }
    };

    return this.Super('fetchData', [
      this.convertCriteria(criteria),
      newCallBack,
      requestProperties
    ]);
  },

  handleFilterEditorSubmit: function(criteria, context, autoSaveDone) {
    var callback,
      me = this;
    if (!autoSaveDone) {
      var actionObject = {
        target: this,
        method: this.handleFilterEditorSubmit,
        parameters: [criteria, context, true]
      };
      this.view.standardWindow.doActionAfterAutoSave(actionObject, true);
      return;
    }
    callback = function() {
      delete me.isFilteringExternally;
    };
    this.closeAnyOpenEditor();
    if (
      isc.isAn.Array(this.data) ||
      (this.data.willFetchData &&
        this.data.willFetchData(this.convertCriteria(criteria)))
    ) {
      // Use this flag when a filter editor submit results a datasource request
      // This flag will be used to prevent unneeded datasource requests, see https://issues.openbravo.com/view.php?id=29896
      // this.data is an empty array in case of lazy filtering is set and no data
      // has been fetched yet
      this.isFilteringExternally = true;
    }
    this.Super('handleFilterEditorSubmit', [criteria, context, callback]);
  },

  getInitialCriteria: function() {
    var criteria = this.Super('getInitialCriteria', arguments);
    return this.convertCriteria(criteria);
  },

  getCriteria: function() {
    var criteria = this.Super('getCriteria', arguments) || {};
    if (!criteria.criteria && this.initialCriteria) {
      criteria = isc.shallowClone(this.initialCriteria);
    }
    criteria = this.convertCriteria(criteria);
    return criteria;
  },

  convertCriteria: function(criteria) {
    var selectedValues,
      i,
      j,
      k,
      criterion,
      fldName,
      length,
      today = new Date(),
      currentTimeZoneOffsetInMinutes = -today.getTimezoneOffset();

    if (!criteria) {
      criteria = {};
    } else {
      criteria = isc.clone(criteria);
    }

    if (!criteria.operator) {
      criteria.operator = 'and';
    }
    if (!criteria._constructor) {
      criteria._constructor = 'AdvancedCriteria';
    }

    if (!criteria.criteria) {
      criteria.criteria = [];
    }

    if (!this.notRemoveFilter && this.targetRecordId) {
      // do not filter on anything with a targetrecord
      criteria = {
        operator: 'and',
        _constructor: 'AdvancedCriteria',
        criteria: []
      };

      // add a dummy criteria to force a fetch
      criteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
    }

    // note pass in criteria otherwise infinite looping!
    this.resetEmptyMessage(criteria);
    //convert relative dates to absolute dates. Refer issue https://issues.openbravo.com/view.php?id=27679
    if (this.view.parentProperty && !this.isOpenDirectMode) {
      if (this.view.parentView.isShowingTree) {
        selectedValues = this.view.parentView.treeGrid.getSelectedRecords();
      } else {
        selectedValues = this.view.parentView.viewGrid.getSelectedRecords();
      }
      var parentPropertyFilterValue = -1;
      if (selectedValues) {
        if (selectedValues.length === 0) {
          parentPropertyFilterValue = '-1';
        } else if (selectedValues.length > 1) {
          parentPropertyFilterValue = '-1';
        } else {
          parentPropertyFilterValue = selectedValues[0][OB.Constants.ID];
        }
      }

      this.view.parentRecordId = parentPropertyFilterValue;

      var fnd = false;
      var innerCriteria = criteria.criteria;
      length = innerCriteria.length;
      for (i = 0; i < length; i++) {
        criterion = innerCriteria[i];
        fldName = criterion.fieldName;
        if (fldName === this.view.parentProperty) {
          fnd = true;
          criterion.operator = 'equals';
          criterion.value = parentPropertyFilterValue;
          break;
        }
      }
      if (!fnd) {
        innerCriteria.add({
          fieldName: this.view.parentProperty,
          operator: 'equals',
          value: parentPropertyFilterValue
        });
      }
    }

    // Iterates all the criterias
    // -If they are not needed, they are removed
    // -Otherwise, if it is a datetime criteria, the UTC offset in minutes is added
    if (criteria && criteria.criteria) {
      var internalCriteria = criteria.criteria;
      for (i = internalCriteria.length - 1; i >= 0; i--) {
        var shouldRemove = false;
        criterion = internalCriteria[i];
        // but do not remove dummy criterion
        if (
          criterion.fieldName &&
          criterion.fieldName.startsWith('_') &&
          criterion.fieldName !== isc.OBRestDataSource.DUMMY_CRITERION_NAME
        ) {
          shouldRemove = true;
        } else if (isc.isA.emptyString(criterion.value)) {
          shouldRemove = true;
        } else if (this.view.parentView && !this.view.parentProperty) {
          // subtabs without an explicit reference to their parent property need to remove unused criterias
          if (this.view.parentView.isShowingTree) {
            selectedValues = this.view.parentView.treeGrid.getSelectedRecords();
          } else {
            selectedValues = this.view.parentView.viewGrid.getSelectedRecords();
          }

          if (selectedValues.length !== 1) {
            // if there is not a single record selected, remove dummies
            if (
              criterion.fieldName === isc.OBRestDataSource.DUMMY_CRITERION_NAME
            ) {
              shouldRemove = true;
            }
          } else {
            // with a single record selected, removed false criterion
            if (
              criterion.fieldName === 'id' &&
              criterion.operator === 'equals' &&
              criterion.value === '-1'
            ) {
              shouldRemove = true;
            }
          }
        } else if (
          criterion.fieldName ===
            this.view.parentProperty +
              OB.Constants.FIELDSEPARATOR +
              OB.Constants.IDENTIFIER &&
          criterion.operator === 'iEquals'
        ) {
          // Prevent the filtering of a parent column if it is shown on grid
          // See issue https://issues.openbravo.com/view.php?id=26767
          shouldRemove = true;
        }

        if (shouldRemove) {
          internalCriteria.removeAt(i);
        } else {
          var fieldName;
          // The first name a date time field is filtered, the fieldName is stored in criteria.criteria[i].criteria[0].fieldName
          if (
            criteria.criteria[i].criteria &&
            criteria.criteria[i].criteria[0]
          ) {
            fieldName = criteria.criteria[i].criteria[0].fieldName;
          } else {
            // After the first time, the fieldName is stored in criteria.criteria[i].fieldName
            fieldName = criteria.criteria[i].fieldName;
          }

          for (j = 0; j < this.fields.length; j++) {
            if (this.fields[j].name === fieldName) {
              if (
                isc.SimpleType.getType(this.fields[j].type).inheritsFrom ===
                'datetime'
              ) {
                if (criteria.criteria[i].criteria) {
                  for (k = 0; k < criteria.criteria[i].criteria.length; k++) {
                    criteria.criteria[i].criteria[
                      k
                    ].minutesTimezoneOffset = currentTimeZoneOffsetInMinutes;
                  }
                } else {
                  criteria.criteria[
                    i
                  ].minutesTimezoneOffset = currentTimeZoneOffsetInMinutes;
                }
              } else if (
                isc.SimpleType.getType(this.fields[j].type).inheritsFrom ===
                  'text' &&
                (criterion.operator === 'iBetweenInclusive' ||
                  criterion.operator === 'betweenInclusive') &&
                criterion.end.indexOf('ZZZZZZZZZZ') === -1
              ) {
                // Fix of iBetweenInclusive criteria
                // See issue https://issues.openbravo.com/view.php?id=26504
                criterion.end = criterion.end + 'ZZZZZZZZZZ';
              }
              break;
            }
          }
        }
      }
    }

    if (this.view.parentView && !this.view.parentProperty) {
      // subtabs without an explicit reference to their parent property
      // result in an empty criteria which is ignored not generating the
      // request. Forcing load
      // See issue #22645
      if (this.view.parentView.isShowingTree) {
        selectedValues = this.view.parentView.treeGrid.getSelectedRecords();
      } else {
        selectedValues = this.view.parentView.viewGrid.getSelectedRecords();
      }
      if (selectedValues.length !== 1) {
        // if there is not a single record selected, always false criterion
        criteria.criteria.push({
          fieldName: 'id',
          operator: 'equals',
          value: '-1'
        });
      } else {
        // with a single record selected, dummy criterion
        criteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
      }
    }

    if (
      this.view.parentView &&
      this.applyWhereClauseToChildren === false &&
      criteria.criteria.length > 1
    ) {
      for (i = 0; i < criteria.criteria.length; i++) {
        criterion = criteria.criteria[i];
        if (criterion.fieldName === this.view.parentProperty) {
          criteria.criteria.splice(i, 1);
        }
      }
    }

    this.checkShowFilterFunnelIcon(criteria);

    return criteria;
  },

  onFetchData: function(criteria, requestProperties) {
    if (this.data && this.data.forceRefresh) {
      // to force fetch from server, remove all cached data
      delete this.data.forceRefresh;
      delete this.data.localData;
      delete this.data.allRows;
      this.data.totalRows = 0;
      this.data.cachedRows = 0;
    }
    this.setFechingData();

    requestProperties = requestProperties || {};
    requestProperties.params = this.getFetchRequestParams(
      requestProperties.params
    );
  },

  getFetchRequestParams: function(params, isExporting) {
    params = params || {};

    if (this.targetRecordId) {
      params._targetRecordId = this.targetRecordId;
      if (!this.notRemoveFilter) {
        // remove the filter clause we don't want to use it anymore
        this.filterClause = null;
      }

      // this mode means that no parent is selected but the parent needs to be
      // determined from the target record and the parent property
      if (
        this.view.parentProperty &&
        this.isOpenDirectMode &&
        this.view.parentView
      ) {
        params._filterByParentProperty = this.view.parentProperty;
      }

      if (this.view && this.view.directNavigation) {
        params._directNavigation = true;
      }
    } else if (params._targetRecordId) {
      delete params._targetRecordId;
    }

    // prevent the count operation
    params[isc.OBViewGrid.NO_COUNT_PARAMETER] = 'true';

    if (this.orderByClause) {
      params[OB.Constants.ORDERBY_PARAMETER] = this.orderByClause;
    }

    // add all the new session properties context info to the requestProperties
    isc.addProperties(params, this.view.getContextInfo(true, false));

    params[
      isc.OBViewGrid.IS_FILTER_CLAUSE_APPLIED
    ] = this.isFilterClauseApplied();

    if (this.isSorting) {
      params.isSorting = true;
      delete this.isSorting;
    }

    if (!isExporting) {
      params._selectedProperties = this.getSelectedProperties();
    }
    return params;
  },

  getSelectedProperties: function() {
    var i,
      len,
      selectedProperties = '',
      first = true;

    len = this.requiredGridProperties.length;
    for (i = 0; i < len; i++) {
      if (first) {
        first = false;
        selectedProperties =
          selectedProperties + this.requiredGridProperties[i];
      } else {
        selectedProperties =
          selectedProperties + ',' + this.requiredGridProperties[i];
      }
    }

    len = this.fields.length;
    for (i = 0; i < len; i++) {
      if (this.fields[i].name[0] !== '_') {
        selectedProperties = selectedProperties + ',';
        selectedProperties = selectedProperties + this.fields[i].name;
      }
    }

    return selectedProperties;
  },

  createNew: function() {
    this.view.editRecord();
  },

  makeVisible: function() {
    if (this.view.isShowingForm) {
      this.view.switchFormGridVisibility();
    } else if (!this.view.isShowingTree) {
      this.show();
    }
  },

  // determine which field can be autoexpanded to use extra space
  getAutoFitExpandField: function() {
    var ret, i, length;
    length = this.view.autoExpandFieldNames.length;
    for (i = 0; i < length; i++) {
      var field = this.getField(this.view.autoExpandFieldNames[i]);
      if (field && field.name) {
        return field.name;
      }
    }
    ret = this.Super('getAutoFitExpandField', arguments);
    return ret;
  },

  recordClick: function(
    viewer,
    record,
    recordNum,
    field,
    fieldNum,
    value,
    rawValue
  ) {
    var textDeselectInterval = setInterval(function() {
      //To ensure that if finally a double click (recordDoubleClick) is executed, no work is highlighted/selected
      if (document.selection && document.selection.empty) {
        document.selection.empty();
      } else if (window.getSelection) {
        var sel = window.getSelection();
        sel.removeAllRanges();
      }
    }, 15);
    setTimeout(function() {
      clearInterval(textDeselectInterval);
    }, 350);
    var actionObject = {
      target: this,
      method: this.handleRecordSelection,
      parameters: [
        viewer,
        record,
        recordNum,
        field,
        fieldNum,
        value,
        rawValue,
        false,
        this.view.isEditingGrid
      ]
    };
    this.view.standardWindow.doActionAfterAutoSave(actionObject, true);
  },

  recordDoubleClick: function(
    viewer,
    record,
    recordNum,
    field,
    fieldNum,
    value,
    rawValue
  ) {
    var actionObject = {
      target: this.view,
      method: this.view.editRecord,
      parameters: [record, false, field ? field.name : null]
    };
    this.view.standardWindow.doActionAfterAutoSave(actionObject, true);
  },

  resetEmptyMessage: function(criteria) {
    var selectedValues,
      parentIsNew,
      oldMessage = this.emptyMessage;
    criteria = criteria || this.getCriteria();
    if (!this.view) {
      this.emptyMessage = this.noDataEmptyMessage;
    } else if (this.isGridFiltered(criteria)) {
      // there can be some initial filters, but still no parent selected
      if (this.view.parentView) {
        if (this.view.parentView.isShowingTree) {
          selectedValues = this.view.parentView.treeGrid.getSelectedRecords();
        } else {
          selectedValues = this.view.parentView.viewGrid.getSelectedRecords();
        }
        parentIsNew =
          this.view.parentView.isShowingForm &&
          this.view.parentView.viewForm.isNew;
        parentIsNew =
          parentIsNew ||
          (selectedValues.length === 1 && selectedValues[0]._new);
        if (parentIsNew) {
          this.emptyMessage =
            '<span class="' +
            this.emptyMessageStyle +
            '">' +
            OB.I18N.getLabel('OBUIAPP_ParentIsNew') +
            '</span>';
        } else if (!selectedValues || selectedValues.length === 0) {
          this.emptyMessage =
            '<span class="' +
            this.emptyMessageStyle +
            '">' +
            OB.I18N.getLabel('OBUIAPP_NoParentSelected') +
            '</span>';
        } else if (selectedValues.length > 1) {
          this.emptyMessage =
            '<span class="' +
            this.emptyMessageStyle +
            '">' +
            OB.I18N.getLabel('OBUIAPP_MultipleParentsSelected') +
            '</span>';
        } else {
          this.emptyMessage = this.filterNoRecordsEmptyMessage;
        }
      } else {
        if (this.lazyFiltering && !isc.isA.ResultSet(this.data)) {
          this.emptyMessage = this.noDataEmptyMessage;
        } else {
          this.emptyMessage = this.filterNoRecordsEmptyMessage;
        }
      }
    } else if (this.view.isRootView) {
      this.emptyMessage = this.noDataEmptyMessage;
    } else {
      if (this.view.parentView.isShowingTree) {
        selectedValues = this.view.parentView.treeGrid.getSelectedRecords();
      } else {
        selectedValues = this.view.parentView.viewGrid.getSelectedRecords();
      }
      parentIsNew =
        this.view.parentView.isShowingForm &&
        this.view.parentView.viewForm.isNew;
      parentIsNew =
        parentIsNew || (selectedValues.length === 1 && selectedValues[0]._new);
      if (parentIsNew) {
        this.emptyMessage =
          '<span class="' +
          this.emptyMessageStyle +
          '">' +
          OB.I18N.getLabel('OBUIAPP_ParentIsNew') +
          '</span>';
      } else if (!selectedValues || selectedValues.length === 0) {
        this.emptyMessage =
          '<span class="' +
          this.emptyMessageStyle +
          '">' +
          OB.I18N.getLabel('OBUIAPP_NoParentSelected') +
          '</span>';
      } else if (selectedValues.length > 1) {
        this.emptyMessage =
          '<span class="' +
          this.emptyMessageStyle +
          '">' +
          OB.I18N.getLabel('OBUIAPP_MultipleParentsSelected') +
          '</span>';
      } else if (this.lazyFiltering && !isc.isA.ResultSet(this.data)) {
        this.emptyMessage =
          '<span class="' +
          this.emptyMessageStyle +
          '">' +
          OB.I18N.getLabel('OBUIAPP_LazyFilteringNoFetch') +
          '</span>';
      } else {
        this.emptyMessage = this.noDataEmptyMessage;
      }
    }
    if (oldMessage !== this.emptyMessage && this.body) {
      this.body.markForRedraw();
    }
  },

  // +++++++++++++++++++++++++++++ Context menu on record click +++++++++++++++++++++++
  cellContextClick: function(record, rowNum, colNum) {
    var isGroupOrSummary =
      record &&
      (record[this.groupSummaryRecordProperty] ||
        record[this.gridSummaryRecordProperty]);

    // don't do anything if right-clicking on a selected record
    if (!this.isSelected(record)) {
      this.handleRecordSelection(
        null,
        record,
        rowNum,
        null,
        colNum,
        null,
        null,
        true
      );
    }

    this.view.setAsActiveView();

    if (isGroupOrSummary) {
      return false;
    }

    var ret = this.Super('cellContextClick', arguments);
    return ret;
  },

  makeCellContextItems: function(record, rowNum, colNum) {
    var sourceWindow = this.view.standardWindow.windowId;
    var menuItems = [];
    var recordsSelected = this.getSelectedRecords().length > 0;
    var singleSelected = this.getSelectedRecords().length === 1;
    var field = this.getField(colNum);
    var grid = this;
    if (!this.view.hasNotChanged() || this.view.viewGrid.hasErrors()) {
      menuItems.add({
        title: OB.I18N.getLabel('OBUIAPP_UndoChanges'),
        keyTitle: OB.KeyboardManager.Shortcuts.getProperty(
          'keyComb.text',
          'Grid_CancelChanges',
          'id'
        ),
        click: function() {
          grid.view.undo();
        }
      });
    }

    if (
      singleSelected &&
      this.canEdit &&
      this.isWritable(record) &&
      !this.view.readOnly
    ) {
      menuItems.add({
        title: OB.I18N.getLabel('OBUIAPP_EditInGrid'),
        keyTitle: OB.KeyboardManager.Shortcuts.getProperty(
          'keyComb.text',
          'ViewGrid_EditInGrid',
          'id'
        ),
        click: function() {
          grid.endEditing();
          if (colNum || colNum === 0) {
            grid.forceFocusColumn = grid.getField(colNum).name;
          }
          grid.startEditing(rowNum, colNum);
        }
      });
    }

    if (
      !this.view.singleRecord &&
      !this.view.readOnly &&
      !this.isGrouped &&
      !this.view.editOrDeleteOnly &&
      this.view.roleCanCreateRecords()
    ) {
      menuItems.add({
        title: OB.I18N.getLabel('OBUIAPP_CreateRecordInGrid'),
        keyTitle: OB.KeyboardManager.Shortcuts.getProperty(
          'keyComb.text',
          'ToolBar_NewRow',
          'id'
        ),
        click: function() {
          grid.startEditingNew(rowNum);
        }
      });
    }

    if (singleSelected && field.canFilter) {
      menuItems.add({
        title: OB.I18N.getLabel('OBUIAPP_UseAsFilter'),
        click: function() {
          var value,
            filterFormItem = grid.filterEditor
              .getEditForm()
              .getField(field.name),
            cacheElement = {};
          // a foreign key field, use the displayfield/identifier
          if (field.fkField && field.displayField) {
            value = record[field.displayField];
          } else {
            value = grid.getEditDisplayValue(rowNum, colNum, record);
          }
          // assume a date range filter item
          if (
            isc.isA.Date(value) &&
            field.filterEditorType === 'OBMiniDateRangeItem'
          ) {
            // set the logicalDate property to true so that the date values contained in the filter criteria will always be serialized as plain dates
            value.logicalDate = true;
            filterFormItem.setSingleDateValue(value);
          } else {
            grid.filterEditor
              .getEditForm()
              .setValue(field.name, OB.Utilities.encodeSearchOperator(value));
          }
          if (
            field.filterEditorType === 'OBFKFilterTextItem' &&
            filterFormItem
          ) {
            filterFormItem.filterType = 'id';
            if (!filterFormItem.getRecordIdentifierFromId(record[field.name])) {
              // if the filter editor does not know about this record, add the its id and its identifier to the auxiliary filter cache
              cacheElement[OB.Constants.ID] = record[field.name];
              cacheElement[OB.Constants.IDENTIFIER] =
                record[
                  field.name +
                    OB.Constants.FIELDSEPARATOR +
                    OB.Constants.IDENTIFIER
                ];
              filterFormItem.filterAuxCache.add(cacheElement);
            }
          }
          var criteria = grid.filterEditor.getEditForm().getValuesAsCriteria();
          grid.checkShowFilterFunnelIcon(criteria);
          grid.filterData(criteria);
        }
      });
    }
    if (singleSelected && field.fkField) {
      menuItems.add({
        title: OB.I18N.getLabel('OBUIAPP_OpenOnTab'),
        click: function() {
          var fldName = field.name;
          var dotIndex = fldName.lastIndexOf(OB.Constants.FIELDSEPARATOR);
          if (dotIndex !== -1 && fldName.endsWith(OB.Constants.IDENTIFIER)) {
            fldName = fldName.substring(0, dotIndex);
          }
          OB.Utilities.openDirectView(
            sourceWindow,
            field.refColumnName,
            field.targetEntity,
            record[fldName],
            field.id
          );
        }
      });
    }
    if (
      this.view.isDeleteableTable &&
      recordsSelected &&
      !this.view.readOnly &&
      !this.view.singleRecord &&
      this.allSelectedRecordsWritable() &&
      this.view.standardWindow.allowDelete !== 'N'
    ) {
      menuItems.add({
        title: OB.I18N.getLabel('OBUIAPP_Delete'),
        keyTitle: OB.KeyboardManager.Shortcuts.getProperty(
          'keyComb.text',
          'ToolBar_Eliminate',
          'id'
        ),
        click: function() {
          grid.deleteSelectedRowsByToolbarIcon();
        }
      });
    }
    return menuItems;
  },

  deleteSelectedRowsByToolbarIcon: function() {
    // The deleteSelectedRows action trigger should be the same than the toolbar button, so if this last one is overwritten,
    // this delete rows logic should perform the same action than the toolbar button.
    var grid = this,
      isToolbarButtonFound = false,
      toolbarButton,
      i;
    if (grid.getSelectedRecords().length < 1) {
      return false;
    }
    if (
      grid.view.toolBar &&
      grid.view.toolBar.leftMembers &&
      isc.OBToolbar.TYPE_DELETE
    ) {
      for (i = 0; i < grid.view.toolBar.leftMembers.length; i++) {
        if (
          grid.view.toolBar.leftMembers[i].buttonType ===
          isc.OBToolbar.TYPE_DELETE
        ) {
          isToolbarButtonFound = true;
          toolbarButton = grid.view.toolBar.leftMembers[i];
          if (!toolbarButton.disabled) {
            toolbarButton.action();
            return true;
          }
          break;
        }
      }
    }
    // But if the toolbar button is not found, do the default action
    if (!isToolbarButtonFound) {
      grid.view.deleteSelectedRows();
      return true;
    }
    return false;
  },

  // +++++++++++++++++++++++++++++ Record Selection Handling +++++++++++++++++++++++
  updateSelectedCountDisplay: function() {
    var selection = this.getSelection(),
      fld,
      grid = this;
    var selectionLength = selection.getLength();
    var newValue = '&nbsp;';
    if (selectionLength > 0) {
      newValue = selectionLength;

      if (this.filterEditor && this.filterEditor.getEditForm()) {
        fld = this.filterEditor
          .getEditForm()
          .getField(this.getCheckboxField().name);
        if (fld && !fld.clickForSelectedRow) {
          fld.clickForSelectedRow = true;
          fld.originalClick = fld.click;
          fld.click = function() {
            if (grid.getSelection().getLength() === 0) {
              return;
            }
            grid.scrollToRow(grid.getRecordIndex(grid.getSelectedRecord()));
            // do redraw as first columns with buttons are not drawn
            grid.markForRedraw();
          };
          fld.itemHoverHTML = function() {
            return OB.I18N.getLabel('OBUIAPP_ClickSelectedCount');
          };
        }
        fld.textBoxStyle = fld.clickableTextBoxStyle;
        fld.updateState();
      }
    } else {
      if (this.filterEditor && this.filterEditor.getEditForm()) {
        fld = this.filterEditor
          .getEditForm()
          .getField(this.getCheckboxField().name);
        if (fld) {
          fld.textBoxStyle = fld.nonClickableTextBoxStyle;
          fld.updateState();
        }
      }
    }
    if (this.filterEditor && this.filterEditor.getEditForm()) {
      this.filterEditor
        .getEditForm()
        .setValue(this.getCheckboxField().name, newValue);
      this.filterEditor
        .getEditForm()
        .getField(this.getCheckboxField().name).defaultValue = newValue;
    }
  },

  // note when solving selection issues in the future also
  // consider using the selectionChanged method, but that
  // one has as disadvantage that it is called multiple times
  // for one select/deselect action
  selectionUpdated: function(record, recordList) {
    if (
      (!recordList || recordList.length === 1) &&
      record === this.lastSelectedRecord &&
      (this.lastSelectedRecord || record)
    ) {
      return;
    }

    // close any editors, but only if it is different from the one we are editing
    if (this.isEditingGrid) {
      var editRecord = this.getRecord(this.getEditRow());
      if (editRecord !== record) {
        this.closeAnyOpenEditor();
      }
    }
    this.stopHover();
    this.updateSelectedCountDisplay();
    this.view.recordSelected();
    if (this.getSelectedRecords() && this.getSelectedRecords().length !== 1) {
      this.lastSelectedRecord = null;
    } else {
      this.lastSelectedRecord = this.getSelectedRecord();
    }
  },

  selectOnMouseDown: function(record, recordNum, fieldNum, autoSaveDone) {
    // don't change selection on right mouse down
    var EH = isc.EventHandler,
      eventType = EH.getEventType();
    this.wasEditing = this.view.isEditingGrid;

    // don't do anything if right-clicking on a selected record
    if (EH.rightButtonDown() && this.isSelected(record)) {
      return;
    }

    // do autosave when this is a click on a checkbox field or when this is not
    // a mouse event, in other cases the autosave is done as part of the recordclick
    // which is called for a mousedown also
    var passToAutoSave =
      this.getCheckboxFieldPosition() === fieldNum ||
      !EH.isMouseEvent(eventType);

    if (!autoSaveDone && passToAutoSave) {
      var actionObject = {
        target: this,
        method: this.selectOnMouseDown,
        parameters: [record, recordNum, fieldNum, true]
      };
      this.view.standardWindow.doActionAfterAutoSave(actionObject, true);
      return;
      // only call this method in case a checkbox click was done
      // in all other cases the recordClick will be called later
      // anyway
      //      if (this.getCheckboxFieldPosition() === fieldNum) {
      //        this.setActionAfterAutoSave(this, this.selectOnMouseDown, arguments);
      //      }
    }

    var previousSingleRecordSelection = this.singleRecordSelection;
    var currentSelectedRecordSelected = this.getSelectedRecord() === record;
    if (this.getCheckboxFieldPosition() === fieldNum) {
      if (this.singleRecordSelection) {
        this.deselectAllRecords(true);
      }
      this.singleRecordSelection = false;
      this.Super('selectOnMouseDown', arguments);

      // handle a special case:
      // - singlerecordmode: checkbox is not checked
      // - user clicks on checkbox
      // in this case move to multi select mode and keep the record selected
      if (previousSingleRecordSelection && currentSelectedRecordSelected) {
        this.selectSingleRecord(record);
      }

      this.selectionUpdated();

      this.markForRedraw('Selection checkboxes need to be redrawn');
    } else {
      // do some checking, the handleRecordSelection should only be called
      // in case of keyboard navigation and not for real mouse clicks,
      // these are handled by the recordClick and recordDoubleClick methods
      // if this method here would also handle mouseclicks then the
      // doubleClick
      // event is not captured anymore
      if (!EH.isMouseEvent(eventType)) {
        this.handleRecordSelection(
          null,
          record,
          recordNum,
          null,
          fieldNum,
          null,
          null,
          true
        );
      }
    }
  },

  handleRecordSelection: function(
    viewer,
    record,
    recordNum,
    field,
    fieldNum,
    value,
    rawValue,
    fromSelectOnMouseDown
  ) {
    var wasEditing = this.wasEditing;
    delete this.wasEditing;
    var EH = isc.EventHandler;
    var keyName = EH.getKey();

    // stop editing if the user clicks out of the row
    if (
      (this.getEditRow() || this.getEditRow() === 0) &&
      this.getEditRow() !== recordNum
    ) {
      this.endEditing();
      wasEditing = true;
    }
    // do nothing, click in the editrow itself
    if (
      (this.getEditRow() || this.getEditRow() === 0) &&
      this.getEditRow() === recordNum
    ) {
      return;
    }

    // if the arrow key was pressed and no ctrl/shift pressed then
    // go to single select mode
    var arrowKeyPressed =
      keyName &&
      (keyName === isc.OBViewGrid.ARROW_UP_KEY_NAME ||
        keyName === isc.OBViewGrid.ARROW_DOWN_KEY_NAME);

    var previousSingleRecordSelection = this.singleRecordSelection;
    if (arrowKeyPressed) {
      if (
        (EH.ctrlKeyDown() && !EH.altKeyDown() && !EH.shiftKeyDown()) ||
        (!EH.ctrlKeyDown() && !EH.altKeyDown() && EH.shiftKeyDown())
      ) {
        // move to multi-select mode, let the standard do it for us
        this.singleRecordSelection = false;
      } else if (!(!EH.ctrlKeyDown() && EH.altKeyDown() && EH.shiftKeyDown())) {
        // 'if' statement to avoid do an action when the KS to move to a child tab is fired
        this.doSelectSingleRecord(record);
      }
    } else if (this.getCheckboxFieldPosition() === fieldNum) {
      if (this.singleRecordSelection) {
        this.deselectAllRecords(true);
      }
      // click in checkbox field is done by standard logic
      // in the selectOnMouseDown
      this.singleRecordSelection = false;
      this.selectionUpdated();
    } else if (
      isc.EventHandler.ctrlKeyDown() &&
      !isc.EventHandler.altKeyDown() &&
      !isc.EventHandler.shiftKeyDown()
    ) {
      // only do something if record clicked and not from selectOnMouseDown
      // this method got called twice from one clicK: through recordClick
      // and
      // to selectOnMouseDown. Only handle one.
      if (!fromSelectOnMouseDown) {
        this.singleRecordSelection = false;
        // let ctrl-click also deselect records
        if (this.isSelected(record)) {
          this.deselectRecord(record);
        } else {
          this.selectRecord(record);
        }
      }
    } else if (
      !isc.EventHandler.ctrlKeyDown() &&
      !isc.EventHandler.altKeyDown() &&
      isc.EventHandler.shiftKeyDown()
    ) {
      this.singleRecordSelection = false;
      this.selection.selectOnMouseDown(this, recordNum, fieldNum);
      this.selectionUpdated(this.getSelectedRecord(), this.getSelection());
    } else {
      // click on the record which was already selected
      this.doSelectSingleRecord(record);

      // if we were editing then a single click continue edit mode
      if (wasEditing) {
        // set the focus in the clicked cell
        this.forceFocusColumn = this.getField(fieldNum).name;
        this.startEditing(recordNum, fieldNum);
      }
    }

    this.updateSelectedCountDisplay();
    this.view.toolBar.updateButtonState(true);

    // mark some redraws if there are lines which don't
    // have a checkbox flagged, so if we move from single record selection
    // to multi record selection
    if (!this.singleRecordSelection && previousSingleRecordSelection) {
      this.markForRedraw('Selection checkboxes need to be redrawn');
    }
  },

  selectRecordForEdit: function(record) {
    this.Super('selectRecordForEdit', arguments);
    this.doSelectSingleRecord(record);
  },

  doSelectSingleRecord: function(record) {
    // if this record is already selected and the only one then do nothing
    // note that when navigating with the arrow key that at a certain 2 are
    // selected
    // when going into this method therefore the extra check on length === 1
    if (
      this.singleRecordSelection &&
      this.getSelectedRecord() === record &&
      this.getSelection().length === 1
    ) {
      return;
    }
    this.singleRecordSelection = true;
    this.selectSingleRecord(record);

    // deselect the checkbox in the top
    var fieldNum = this.getCheckboxFieldPosition(),
      field = this.fields[fieldNum];
    var icon = this.checkboxFieldFalseImage || this.booleanFalseImage;
    var title = this.getValueIconHTML(icon, field);

    this.setFieldTitle(fieldNum, title);
  },

  // overridden to prevent the checkbox to be shown when only one
  // record is selected.
  getCellValue: function(record, recordNum, fieldNum, gridBody) {
    var field = this.fields[fieldNum],
      value,
      isEditRow = recordNum === this.getEditRow(),
      wasGrouped,
      func = this.getGridSummaryFunction(field),
      isGroupOrSummary =
        record &&
        (record[this.groupSummaryRecordProperty] ||
          record[this.gridSummaryRecordProperty]);

    // no checkbox in checkbox column for summary row
    if (isGroupOrSummary && this.isCheckboxField(field)) {
      return '';
    }

    if (!field || this.allSelected) {
      return this.Super('getCellValue', arguments);
    }

    if (isGroupOrSummary) {
      // handle count much simpler than smartclient does
      // so no extra titles or formatting
      if (
        !this.getGroupByFields().contains(field.name) &&
        func === 'count' &&
        (record[field.name] === 0 || record[field.name])
      ) {
        return record[field.name];
      }
      return this.Super('getCellValue', arguments);
    }

    // do all the cases which are handled in the super directly
    if (this.isCheckboxField(field)) {
      // NOTE: code copied from super class
      var icon;
      if (this.singleRecordSelection && !this.allSelected) {
        // always show the false image
        icon = this.checkboxFieldFalseImage || this.booleanFalseImage;
      } else {
        // checked if selected, otherwise unchecked
        var isSel = this.selection.isSelected(record) ? true : false;
        icon = isSel
          ? this.checkboxFieldTrueImage || this.booleanTrueImage
          : this.checkboxFieldFalseImage || this.booleanFalseImage;
      }
      // if the record is disabled, make the checkbox image disabled as well
      // or if the record is new then also show disabled
      if (!record || record[this.recordEnabledProperty] === false) {
        icon = icon.replace('.', '_Disabled.');
      }

      var html = this.getValueIconHTML(icon, field);

      return html;
    } else {
      // prevent group style behavior for edit rows
      if (isEditRow && this.isGrouped) {
        wasGrouped = true;
        this.isGrouped = false;
      }
      value = this.Super('getCellValue', arguments);
      if (wasGrouped) {
        this.isGrouped = true;
      }
      return value;
    }
  },

  getSelectedRecords: function() {
    return this.getSelection();
  },

  // +++++++++++++++++ functions for grid editing +++++++++++++++++
  setEditValue: function() {
    if (!this.showGridSummary) {
      this.Super('setEditValue', arguments);
      return;
    }
    // suppress summary function recalculation when editing values
    this.showGridSummary = false;
    this.Super('setEditValue', arguments);
    this.showGridSummary = true;
  },

  setEditValues: function() {
    if (!this.showGridSummary) {
      this.Super('setEditValues', arguments);
      return;
    }
    // suppress summary function recalculation when editing values
    this.showGridSummary = false;
    this.Super('setEditValues', arguments);
    this.showGridSummary = true;
  },

  startEditing: function(rowNum, colNum, suppressFocus, eCe, suppressWarning) {
    var i,
      ret,
      length = this.getFields().length;
    // if a row is set and not a col then check if we should focus in the
    // first error field
    if (
      (rowNum || rowNum === 0) &&
      (!colNum && colNum !== 0) &&
      this.rowHasErrors(rowNum)
    ) {
      for (i = 0; i < length; i++) {
        if (this.cellHasErrors(rowNum, i)) {
          colNum = i;
          break;
        }
      }
    }

    if (colNum || colNum === 0) {
      this.forceFocusColumn = this.getField(colNum).name;
    } else {
      // set the first focused column
      for (i = 0; i < length; i++) {
        if (
          this.getFields()[i].editorProperties &&
          this.getFields()[i].editorProperties.firstFocusedField
        ) {
          colNum = i;
        }
      }
      if (colNum < length && this.getFields()[colNum].disabled) {
        for (i = 0; i < length; i++) {
          if (
            this.getFields()[i].editorProperties &&
            !this.getFields()[i].disabled &&
            this.getFields()[i].visible
          ) {
            colNum = i;
            break;
          }
        }
      }
      if (colNum === undefined && !this.isThereAnyEditableField(rowNum)) {
        //The focus is set in the field 2 because the fiel 0 is the checkbox to select a record and
        //the 1 are the buttons to edit and open the record in form view.
        colNum = 2;
      }
    }

    // make sure that we are visible
    this.scrollRecordIntoView(rowNum);

    ret = this.Super('startEditing', [
      rowNum,
      colNum,
      suppressFocus,
      eCe,
      suppressWarning
    ]);

    return ret;
  },

  isThereAnyEditableField: function(rowNum) {
    return this.findNextEditCell(rowNum, 0, 1, true, true) !== null;
  },

  startEditingNew: function(rowNum) {
    // several cases:
    // - no current rows, add at position 0
    // - row selected, add row after selected row
    // - no row selected, add in the bottom
    var undef, insertRow;
    if (rowNum === undef) {
      // nothing selected
      if (!this.getSelectedRecord()) {
        if (this.getTotalRows() > this.data.cachedRows) {
          insertRow = 0;
        } else {
          insertRow = this.getTotalRows();
        }
      } else {
        insertRow = 1 + this.getRecordIndex(this.getSelectedRecord());
      }
    } else {
      insertRow = rowNum + 1;
    }

    if (this.lazyFiltering && !isc.isA.ResultSet(this.data)) {
      this.markForCalculateSummaries();
      OB.Utilities.createResultSetManually(this);
    }

    this.createNewRecordForEditing(insertRow);
    this.startEditing(insertRow);
    this.recomputeCanvasComponents(insertRow);
    this.view.initChildViewsForNewRecord();
  },

  initializeEditValues: function(rowNum, colNum) {
    var record = this.getRecord(rowNum);
    // no record create one
    if (!record) {
      this.createNewRecordForEditing(rowNum);
    }
    return this.Super('initializeEditValues', arguments);
  },

  createNewRecordForEditing: function(rowNum) {
    // note: the id is dummy, will be replaced when the save succeeds,
    // it MUST start with _ to identify it is a temporary id
    var record = {
      _new: true,
      id: OB.Utilities.getTemporaryId()
    };

    this.addToCacheData(record, rowNum);
    this.scrollToRow(rowNum);
    this.updateRowCountDisplay();
    this.view.toolBar.updateButtonState(true);

    // do it with a delay to give the system time to set the record information
    this.markForRedraw();
  },

  addToCacheData: function(record, rowNum) {
    // originalData is used when the grid is grouped
    var data = this.originalData || this.data;

    // When a new record is inserted and added to cache, existent cache of
    // rows received from server is replaced with current localData.
    // Not doing it causes problems when localData does not match allRows
    // beacause it has been restricted in client through adaptive filters.
    data.allRows = data.localData;
    data.allRowsCriteria = data.criteria || {};
    data.cachedRows = data.localData.length;
    data.totalRows = data.localData.length;

    data.insertCacheData(record, rowNum);
  },

  editFailed: function(
    rowNum,
    colNum,
    newValues,
    oldValues,
    editCompletionEvent,
    dsResponse,
    dsRequest
  ) {
    var record = this.getRecord(rowNum),
      view = this.view,
      form,
      isNewRecord;

    // set the default error message,
    // is possibly overridden in the next call
    if (record) {
      this.addRecordToValidationErrorList(record);
      if (!record[isc.OBViewGrid.ERROR_MESSAGE_PROP]) {
        this.setRecordErrorMessage(
          rowNum,
          OB.I18N.getLabel('OBUIAPP_ErrorInFields')
        );
        // do not automatically remove this message
        this.view.messageBar.keepOnAutomaticRefresh = true;
      } else {
        record[this.recordBaseStyleProperty] = this.recordStyleError;
      }
    }

    if (!this.isVisible()) {
      isc.warn(OB.I18N.getLabel('OBUIAPP_TabWithErrors', [this.view.tabTitle]));
    } else if (
      view.standardWindow.forceDialogOnFailure &&
      !this.view.isActiveView
    ) {
      isc.warn(OB.I18N.getLabel('OBUIAPP_AutoSaveError', [this.view.tabTitle]));
    }

    form = this.getEditForm();
    if (view.standardWindow.isAutoSaving) {
      // show an error message in the toolbar if the event that triggered the action was an autosave, to mimic the way client side validation errors are handled
      view.messageBar.setMessage(
        isc.OBMessageBar.TYPE_ERROR,
        null,
        OB.I18N.getLabel('OBUIAPP_ErrorInFieldsGrid', [view.ID])
      );
      if (form) {
        // return the focus to the edit form of the record that failed to be edited
        form.setFocusInForm();
      }
    }
    view.standardWindow.cleanUpAutoSaveProperties();
    view.updateTabTitle();
    view.toolBar.updateButtonState(true);

    // if nothing else got selected, select ourselves then
    if (record && !this.getSelectedRecord()) {
      this.selectRecord(record);
    }

    isNewRecord = form === null ? false : form.isNew;
    if (isNewRecord) {
      delete this.view._savingNewRecord;
    }
  },

  addRecordToValidationErrorList: function(record) {
    if (!record) {
      return;
    }
    record._hasValidationErrors = true;
    this.recordIdsWithValidationError = this.recordIdsWithValidationError || [];
    if (!this.recordIdsWithValidationError.contains(record[OB.Constants.ID])) {
      this.recordIdsWithValidationError.push(record[OB.Constants.ID]);
    }
  },

  removeRecordFromValidationErrorList: function(record) {
    if (!record) {
      return;
    }
    delete record._hasValidationErrors;
    this.recordIdsWithValidationError = this.recordIdsWithValidationError || [];
    this.recordIdsWithValidationError.remove(record[OB.Constants.ID]);
  },

  selectedRecordHasValidationErrors: function() {
    var record;
    // if the number of selected records is not 1, return false
    if (
      this.getSelectedRecords().length !== 1 ||
      !isc.isA.Array(this.recordIdsWithValidationError)
    ) {
      return false;
    }
    record = this.getSelectedRecord();
    return this.recordIdsWithValidationError.contains(record[OB.Constants.ID]);
  },

  gridHasValidationErrors: function() {
    if (!isc.isA.Array(this.recordIdsWithValidationError)) {
      return false;
    } else {
      // return true if the list of record ids with validation errors is not empty
      return !this.recordIdsWithValidationError.isEmpty();
    }
  },

  recordHasChanges: function(rowNum, colNum, checkEditor) {
    var record = this.getRecord(rowNum);
    // If a record has validation errors but had all the mandatory fields set,
    // smartclient's recordHasChanges will return false, and the record will be cleared (see ListGrid.hideInlineEditor function)
    // In this case recordhasChanges should return true, because the values in the grid differ with the values in the database
    // See issue https://issues.openbravo.com/view.php?id=22123
    if (record && record._hasValidationErrors) {
      return true;
    } else if (!this.recordHasActualChanges(rowNum, colNum, checkEditor)) {
      return false;
    } else {
      return this.Super('recordHasChanges', arguments);
    }
  },

  // Checks if there are changes other than a field changing from undefined to not undefined
  // Those kind of changes happen when a row is opened in edit mode, they should not be detected as an actual change
  recordHasActualChanges: function(rowNum, colNum, checkEditor) {
    var newValues,
      oldValues,
      changes = false,
      fieldName,
      oldFieldValue,
      newFieldValue,
      isNew;
    if (!checkEditor) {
      checkEditor = true;
    }
    newValues = checkEditor
      ? this.getEditValues(rowNum, colNum)
      : this.getEditSession(rowNum, colNum);
    oldValues = this.getCellRecord(rowNum);
    if (!oldValues) {
      return true;
    }
    isNew = this.getEditForm() ? this.getEditForm().isNew : false;
    for (fieldName in newValues) {
      if (Object.prototype.hasOwnProperty.call(newValues, fieldName)) {
        if (fieldName === this.removeRecordProperty) {
          continue;
        }
        oldFieldValue = oldValues[fieldName];
        newFieldValue = newValues[fieldName];
        // Use custom comparator to catch things like Dates where '==' check is not sufficient
        if (
          (isNew || oldFieldValue !== undefined) &&
          !this.fieldValuesAreEqual(
            this.getField(fieldName),
            oldFieldValue,
            newFieldValue
          ) &&
          !(newFieldValue === '' && oldFieldValue === null)
        ) {
          changes = true;
          break;
        }
      }
    }
    return changes;
  },

  editComplete: function(
    rowNum,
    colNum,
    newValues,
    oldValues,
    editCompletionEvent,
    dsResponse
  ) {
    var record = this.getRecord(rowNum),
      keepSelection,
      form,
      isNewRecord;

    // this happens when the record change causes a group name
    // change and therefore a group collapse
    if (!record) {
      return;
    }

    // the record has been sucessfully saved so it does not have validation errors
    this.removeRecordFromValidationErrorList(record);

    // a new id has been computed use that now
    if (record._newId) {
      record.id = record._newId;
      delete record._newId;
      if (this.view && this.view.updateLastSelectedState) {
        this.view.updateLastSelectedState();
      }
    }

    form = this.getEditForm();
    isNewRecord = form === null ? false : form.isNew;
    if (isNewRecord) {
      delete this.view._savingNewRecord;
    }

    // during save the record looses the link to the editColumnLayout,
    // restore it
    if (oldValues.editColumnLayout && !record.editColumnLayout) {
      var editColumnLayout = oldValues.editColumnLayout;
      editColumnLayout.record = record;
      record.editColumnLayout = editColumnLayout;
    }
    if (record.editColumnLayout) {
      record.editColumnLayout.editButton.setErrorState(false);
      record.editColumnLayout.showEditOpen();
    }

    // remove any new pointer
    delete record._new;

    // success invoke the action, if any there
    this.view.standardWindow.autoSaveDone(this.view, true);

    // if nothing else got selected, select ourselves then
    // if there is already a record selected, only force reselecting that record if the editCompletionEvent was 'programmatic',
    // otherwise ('enter', 'tab', etc) it is not needed, and doing it causes https://issues.openbravo.com/view.php?id=27957
    if (
      !this.getSelectedRecord() ||
      (editCompletionEvent === 'programmatic' &&
        this.getSelectedRecord().id === record._originalId)
    ) {
      this.selectRecord(record);
      keepSelection = true;
      this.view.refreshChildViews(keepSelection);
    } else if (this.getSelectedRecord() === record) {
      this.view.refreshChildViews();
    }

    // remove the error style/message
    this.setRecordErrorMessage(rowNum, null);
    // update after the error message has been removed
    this.view.updateTabTitle();
    this.view.toolBar.updateButtonState(true);
    if (this.view.messageBar.type === isc.OBMessageBar.TYPE_ERROR) {
      this.view.messageBar.hide();
    }
    this.view.refreshParentRecord();

    // Update the focus cell value if different from edit form values.
    // To avoid the case where sometimes data updated through trigger is not showing up without refreshing.
    // Refer issue https://issues.openbravo.com/view.php?id=25028
    this.setEditValue(
      rowNum,
      this.getField(colNum).name,
      this.getRecord(rowNum)[this.getField(colNum).name],
      true,
      true
    );

    if (this.getEditRow() === rowNum) {
      this.getEditForm().markForRedraw();
    } else {
      this.refreshRow(rowNum);
    }

    // If there is a summary row update its value just when creating a new record.
    // When updating a record this summary update is done by dataChanged method.
    if (
      this.showGridSummary &&
      this.getEditForm() &&
      this.getEditForm().isNew
    ) {
      this.getSummaryRow();
    }
  },

  undoEditSelectedRows: function() {
    var selectedRecords = this.getSelectedRecords(),
      toRemove = [],
      i,
      length = selectedRecords.length;
    for (i = 0; i < length; i++) {
      var rowNum = this.getRecordIndex(selectedRecords[i]);
      var record = selectedRecords[i];
      this.removeRecordFromValidationErrorList(record);
      this.Super('discardEdits', [
        rowNum,
        false,
        false,
        isc.ListGrid.PROGRAMMATIC
      ]);
      // remove the record if new
      if (record._new) {
        toRemove.push({
          id: record.id
        });
      } else {
        // remove the error style/msg
        this.setRecordErrorMessage(rowNum, null);
      }
    }
    this.deselectAllRecords();
    this.view.refreshChildViews();
    if (toRemove.length > 0) {
      this.data.handleUpdate('remove', toRemove);
      this.updateRowCountDisplay();
      this.view.toolBar.updateButtonState(true);
    }
    this.view.standardWindow.cleanUpAutoSaveProperties();
    this.view.updateTabTitle();
    this.view.toolBar.updateButtonState(true);
  },

  getCellStyle: function(record, rowNum, colNum) {
    // inactive, selected
    if (record && record[this.recordCustomStyleProperty]) {
      return record[this.recordCustomStyleProperty];
    }

    if (
      !this.view.isActiveView() &&
      record &&
      record[this.selection.selectionProperty]
    ) {
      return this.recordStyleSelectedViewInActive;
    }

    return this.Super('getCellStyle', arguments);
  },

  discardEdits: function(rowNum, colNum, dontHideEditor, editCompletionEvent) {
    var localArguments = arguments,
      totalRows,
      record = this.getRecord(rowNum),
      selectedRecord = this.getSelectedRecord();

    if (record) {
      this.removeRecordFromValidationErrorList(record);
    }

    this.Super('discardEdits', localArguments);

    // remove the record if new
    if (record && record._new) {
      // after cancelling a not saved record, the value for the selected record should be cleared
      // see issue https://issues.openbravo.com/view.php?id=31434
      if (this.selection && selectedRecord) {
        this.selection.deselect(selectedRecord);
      }
      totalRows = this.data.totalRows;
      if (this.showGridSummary) {
        this.showGridSummary = false;
        this.isBeingCancelled = true;
        this.data.handleUpdate('remove', [
          {
            id: record.id
          }
        ]);
        this.setShowGridSummary(true);
        delete this.isBeingCancelled;
      } else {
        this.data.handleUpdate('remove', [
          {
            id: record.id
          }
        ]);
      }
      // the total rows should be decreased
      if (this.data.totalRows === totalRows) {
        this.data.totalRows = this.data.totalRows - 1;
      }
      this.updateRowCountDisplay();
      this.view.toolBar.updateButtonState(true);
      this.view.refreshChildViews();
    } else {
      // remove the error style/msg
      this.setRecordErrorMessage(rowNum, null);
    }

    this.view.standardWindow.cleanUpAutoSaveProperties();
    this.refreshRow(rowNum);
    // update after removing the error msg
    this.view.updateTabTitle();
  },

  saveEdits: function(
    editCompletionEvent,
    callback,
    rowNum,
    colNum,
    validateOnly,
    skipValidation
  ) {
    var ret = this.Super('saveEdits', arguments);
    // save was not done, because there were no changes probably
    if (!ret) {
      this.view.standardWindow.cleanUpAutoSaveProperties();
      this.view.updateTabTitle();
      this.view.toolBar.updateButtonState(true);
    }
    return ret;
  },

  cellEditEnd: function(
    editCompletionEvent,
    newValue,
    ficCallDone,
    autoSaveDone
  ) {
    var rowNum,
      colNum,
      nextEditCell,
      newRow,
      me = this;

    rowNum = me.getEditRow();
    colNum = me.getEditCol();
    nextEditCell =
      (rowNum || rowNum === 0) && (colNum || colNum === 0)
        ? me.getNextEditCell(rowNum, colNum, editCompletionEvent)
        : null;
    newRow = nextEditCell && nextEditCell[0] !== rowNum;
    if (
      newRow !== false &&
      me.keyPressedForEditCompletion(editCompletionEvent) &&
      me.view.existsAction &&
      me.view.existsAction(OB.EventHandlerRegistry.PRESAVE)
    ) {
      me.view.executePreSaveActions(function() {
        me.doCellEditEnd(
          editCompletionEvent,
          newValue,
          ficCallDone,
          autoSaveDone
        );
      });
      return;
    }
    me.doCellEditEnd(editCompletionEvent, newValue, ficCallDone, autoSaveDone);
  },

  keyPressedForEditCompletion: function(editCompletionEvent) {
    return (
      editCompletionEvent === isc.ListGrid.DOWN_ARROW_KEYPRESS || //
      editCompletionEvent === isc.ListGrid.UP_ARROW_KEYPRESS || //
      editCompletionEvent === isc.ListGrid.ENTER_KEYPRESS || //
      editCompletionEvent === isc.ListGrid.TAB_KEYPRESS || //
      editCompletionEvent === isc.ListGrid.SHIFT_TAB_KEYPRESS
    );
  },

  // check if a fic call needs to be done when leaving a cell and moving to the next
  // row
  // see description in saveEditvalues
  doCellEditEnd: function(
    editCompletionEvent,
    newValue,
    ficCallDone,
    autoSaveDone
  ) {
    var rowNum = this.getEditRow(),
      colNum = this.getEditCol();
    var editForm = this.getEditForm(),
      editField = this.getEditField(colNum),
      focusItem = editForm ? editForm.getFocusItem() : null,
      isDynamicCol = false,
      i;
    // sometimes rowNum and colnum are not set, then don't compute the next cell
    var nextEditCell =
      (rowNum || rowNum === 0) && (colNum || colNum === 0)
        ? this.getNextEditCell(rowNum, colNum, editCompletionEvent)
        : null;
    var newRow = nextEditCell && nextEditCell[0] !== rowNum;
    var enterKey = editCompletionEvent === 'enter';

    // if event was triggered by pressing the enter key, do not continue if the current edit field is not the one focused when the enter key was pressed
    // this happens for instance when a value is selected from a pick list by pressing enter. if that happens the value is selected, the focus is moved to the
    // next form item and the cellEditEnd function can be invoked for the form item that just got the focus
    if (
      enterKey &&
      editField.name !== this.getEditForm().lastKeyDownItem.name
    ) {
      return;
    }

    // no newValue, compute it, this because in the super method there is a check
    // how many arguments are passed on, sometimes the newValue is not passed in
    // and then it must be recomputed, so if we then use the undefined newValue
    // in the actionObject below things will go wrong
    if (arguments.length < 2 && this.view.allowNewRow()) {
      newValue = this.getEditValue(rowNum, colNum);
    }

    if (
      !this.view.standardWindow.isAutoSaveEnabled() &&
      !enterKey &&
      !autoSaveDone &&
      newRow &&
      (editForm.hasChanged || editForm.isNew)
    ) {
      var actionObject = {
        target: this,
        method: this.cellEditEnd,
        parameters: [editCompletionEvent, newValue, ficCallDone, true]
      };
      this.view.standardWindow.doActionAfterAutoSave(actionObject, true);
      return;
    }

    if (this.getEditForm() && this.getEditForm().dynamicCols) {
      for (i = 0; i < this.getEditForm().dynamicCols.length; i++) {
        if (this.getEditForm().dynamicCols[i] === focusItem.inpColumnName) {
          isDynamicCol = true;
          break;
        }
      }
    }

    if (
      newRow &&
      this.getEditForm() &&
      this.getEditForm().isNew &&
      this.getEditForm().inFicCall &&
      isDynamicCol &&
      editCompletionEvent === 'tab' &&
      !ficCallDone
    ) {
      this.setEditValue(
        rowNum,
        'actionAfterFicReturn',
        {
          target: this,
          method: this.cellEditEnd,
          parameters: [editCompletionEvent, newValue, true, autoSaveDone]
        },
        true,
        true
      );
      return;
    }

    // If leaving the row or moving to a cell in the same row that is disabled...
    if (newRow || nextEditCell === null) {
      // do not leave the row if the row is new and not all mandatory fields have been set
      if (editForm && editForm.isNew && !editForm.allRequiredFieldsSet()) {
        return;
      }
      // See issue https://issues.openbravo.com/view.php?id=19830
      if (this.view.standardWindow.getDirtyEditForm()) {
        this.view.standardWindow.getDirtyEditForm().validateForm();
      }
    }

    this._leavingCell = true;

    if (newValue) {
      this.Super('cellEditEnd', [editCompletionEvent, newValue]);
    } else {
      this.Super('cellEditEnd', [editCompletionEvent]);
    }
    delete this._leavingCell;
    // only needed for non picklist fields
    // as picklist fields will always have picked a value
    // note that focusItem updatevalue for picklist can result in extra datasource requests
    if (
      focusItem &&
      editField &&
      focusItem.name === editField.name &&
      !focusItem.hasPickList
    ) {
      focusItem.blur(focusItem.form, focusItem);
    }
  },

  // overridden to set the enterkeyaction to nextrowstart in cases the current row
  // is the last being edited
  // also sets a flag which is used in canEditCell
  getNextEditCell: function(rowNum, colNum, editCompletionEvent) {
    var ret,
      i,
      length = this.getFields().length;
    this._inGetNextEditCell = true;
    // past the last row
    if (
      editCompletionEvent === isc.ListGrid.ENTER_KEYPRESS &&
      rowNum === this.getTotalRows() - 1 &&
      this.view.allowNewRow()
    ) {
      // move to the next row
      ret = this.findNextEditCell(rowNum + 1, 0, 1, true, true);

      // force the focus column in the first focus field
      for (i = 0; i < length; i++) {
        if (
          this.getFields()[i].editorProperties &&
          this.getFields()[i].editorProperties.firstFocusedField
        ) {
          this.forceFocusColumn = this.getFields()[i].name;
          break;
        }
      }
    } else {
      ret = this.Super('getNextEditCell', arguments);
    }

    // when moving between rows with the arrow keys, force the focus in the correct
    // column
    if (
      ret &&
      ret[0] !== rowNum &&
      this.getField(colNum) &&
      (editCompletionEvent === isc.ListGrid.UP_ARROW_KEYPRESS ||
        editCompletionEvent === isc.ListGrid.DOWN_ARROW_KEYPRESS) &&
      this.view.allowNewRow()
    ) {
      this.forceFocusColumn = this.getField(colNum).name;
    }

    delete this._inGetNextEditCell;
    return ret;
  },

  //used in Edit or Delete only UI pattern and in Single Record UI pattern
  setListEndEditAction: function() {
    this.listEndEditAction = 'done';
  },

  // overridden to take into account disabled at item level
  // only used when computing the next edit cell
  // if caneditcell returns false in other cases then smartclient
  // won't even show an input but shows the display value directly
  // this interferes sometimes with the very dynamic enabling
  // disabling of fields by the readonlylogic
  canEditCell: function(rowNum, colNum) {
    var ret;
    if (this._inGetNextEditCell) {
      var field = this.getField(colNum);
      if (field && this.getEditForm()) {
        var item = this.getEditForm().getItem(field.name);
        if (item && item.isDisabled()) {
          return false;
        }
      }
    }

    if (!colNum && colNum !== 0) {
      return false;
    }

    ret = this.Super('canEditCell', arguments);
    return ret;
  },

  // saveEditedValues: when saving, first check if a FIC call needs to be done to update to the
  // latest values. This can happen when the focus is in a field and the save action is
  // done, at that point first try to force a fic call (handleItemChange) and if that
  // indeed happens stop the saveEdit until the fic returns
  saveEditedValues: function(
    rowNum,
    colNum,
    newValues,
    oldValues,
    editValuesID,
    editCompletionEvent,
    originalCallback,
    ficCallDone
  ) {
    var previousExplicitOffline,
      saveCallback,
      editForm = this.getEditForm(),
      autoSaveAction,
      isNewRecord = false;
    if (!rowNum && rowNum !== 0) {
      rowNum = this.getEditRow();
    }
    if (!colNum && colNum !== 0) {
      colNum = this.getEditCol();
    }

    // nothing changed just fire the calback and bail
    if (!ficCallDone && editForm && !editForm.hasChanged && !editForm.isNew) {
      if (originalCallback) {
        this.fireCallback(
          originalCallback,
          'rowNum,colNum,editCompletionEvent,success',
          [rowNum, colNum, editCompletionEvent]
        );
      }
      return true;
    }

    if (editForm && editForm.isNew) {
      isNewRecord = true;
    }

    if (this.view.standardWindow) {
      autoSaveAction = this.view.standardWindow.autoSaveAction;
    }

    saveCallback = function() {
      var eventHandlerParams = {},
        eventHandlerCallback;

      if (originalCallback) {
        if (
          this.getSelectedRecord() &&
          this.getSelectedRecord()[OB.Constants.ID]
        ) {
          if (this.view.parentRecordId) {
            if (!this.view.newRecordsAfterRefresh) {
              this.view.newRecordsAfterRefresh = {};
            }
            if (!this.view.newRecordsAfterRefresh[this.view.parentRecordId]) {
              this.view.newRecordsAfterRefresh[this.view.parentRecordId] = [];
            }
            this.view.newRecordsAfterRefresh[this.view.parentRecordId].push(
              this.getSelectedRecord()[OB.Constants.ID]
            );
          } else {
            if (!this.view.newRecordsAfterRefresh) {
              this.view.newRecordsAfterRefresh = [];
            }
            this.view.newRecordsAfterRefresh.push(
              this.getSelectedRecord()[OB.Constants.ID]
            );
          }
        }
        this.fireCallback(
          originalCallback,
          'rowNum,colNum,editCompletionEvent,success',
          [rowNum, colNum, editCompletionEvent]
        );

        if (!this.hasErrors() && this.view.callSaveActions) {
          eventHandlerParams.data = isc.clone(this.getRecord(rowNum));
          eventHandlerParams.isNewRecord = isNewRecord;
          if (autoSaveAction) {
            eventHandlerCallback = function() {
              OB.Utilities.callAction(autoSaveAction);
            };
          }
          this.view.callSaveActions(
            OB.EventHandlerRegistry.POSTSAVE,
            eventHandlerParams,
            eventHandlerCallback
          );
        }
      }
    };

    if (!ficCallDone) {
      var focusItem = editForm.getFocusItem();
      if (focusItem && !focusItem.hasPickList) {
        focusItem.blur(focusItem.form, focusItem);
      }
      if (editForm.inFicCall) {
        // use editValues object as the edit form will be re-used for a next row
        this.setEditValue(
          rowNum,
          'actionAfterFicReturn',
          {
            target: this,
            method: this.saveEditedValues,
            parameters: [
              rowNum,
              colNum,
              newValues,
              oldValues,
              editValuesID,
              editCompletionEvent,
              saveCallback,
              true
            ]
          },
          true,
          true
        );
        return;
      }
    }
    // reset the new values as this can have changed because of a fic call or in the blur event of the focused item
    newValues = this.getEditValues(rowNum);

    previousExplicitOffline = isc.Offline.explicitOffline;
    isc.Offline.explicitOffline = false;
    this.Super('saveEditedValues', [
      rowNum,
      colNum,
      newValues,
      oldValues,
      editValuesID,
      editCompletionEvent,
      saveCallback
    ]);
    isc.Offline.explicitOffline = previousExplicitOffline;
    // commented out as it removes an autosave action which is done in the edit complete method
    //    this.view.standardWindow.setDirtyEditForm(null);
  },

  autoSave: function() {
    // flag to force the parsing of date fields when autosaving
    // see issue 20071 (https://issues.openbravo.com/view.php?id=20071)
    this._autoSaving = true;
    this.storeUpdatedEditorValue();
    delete this._autoSaving;
    this.endEditing();
  },

  hideInlineEditor: function(focusInBody, suppressCMHide) {
    var rowNum = this.getEditRow(),
      record = this.getRecord(rowNum),
      editForm = this.getEditForm();

    // Do not hide the inline editor if the action has been caused
    // by hiding or showing a field
    // See issue https://issues.openbravo.com/view.php?id=21352
    if (this._hidingField || this._showingField) {
      return;
    }
    this._hidingInlineEditor = true;
    this.view.isEditingGrid = false;
    if (record && (rowNum === 0 || rowNum)) {
      if (!this.rowHasErrors(rowNum)) {
        record[this.recordBaseStyleProperty] = null;
      }

      if (record.editColumnLayout) {
        isc.Log.logDebug(
          'hideInlineEditor has record and editColumnLayout',
          'OB'
        );
        record.editColumnLayout.showEditOpen();
      } else if (this.currentEditColumnLayout) {
        this.currentEditColumnLayout.showEditOpen();
      } else {
        isc.Log.logDebug(
          'hideInlineEditor has NO record and editColumnLayout',
          'OB'
        );
      }
      // Update the tab title after the record has been saved or canceled
      // to get rid of the '*' in the tab title
      // See https://issues.openbravo.com/view.php?id=21709
      this.view.updateTabTitle();
    }

    // always hide the clickmask, as it needs to be re-applied
    // this super call needs to be done before clearing the values
    // of the form, as the form value clear will result
    // in a field to be flagged with an error
    var ret = this.Super('hideInlineEditor', [focusInBody, false]);

    if (editForm) {
      // canFocus is set when disabling a form item
      // a new record needs to compute canFocus again
      editForm.resetCanFocus();
      // clear all values, as null values in the new row won't overwrite filled form
      // values
      editForm.clearValues();
      // clear the errors so that they don't show up at the next row
      editForm.clearErrors();
      // do not save the focus item to prevent wrong validations when creating a new row
      // see issue 20537 (https://issues.openbravo.com/view.php?id=20537)
      editForm.setFocusItem(null);
    }

    delete this._hidingInlineEditor;

    this.recomputeCanvasComponents(rowNum);

    this.body.markForRedraw();

    return ret;
  },

  getEditDisplayValue: function(rowNum, colNum, record) {
    // somehow this extra call is needed to not restore
    // the old value when the new value is null
    this.storeUpdatedEditorValue();
    return this.Super('getEditDisplayValue', arguments);
  },

  showInlineEditor: function(rowNum, colNum, newCell, newRow, suppressFocus) {
    var fld;

    this._showingEditor = true;

    if (newRow) {
      if (this.getEditForm()) {
        this.getEditForm().clearErrors();
      }
      // if the focus does not get suppressed then the clicked field will receive focus
      // and won't be disabled so the user can already start typing
      suppressFocus = true;
    }

    var ret = this.Super('showInlineEditor', [
      rowNum,
      colNum,
      newCell,
      newRow,
      suppressFocus
    ]);

    if (!newRow) {
      delete this._showingEditor;
      return ret;
    }

    if (this.forceFocusColumn) {
      // set the field to focus on after returning from the fic
      this.getEditForm().forceFocusedField = this.forceFocusColumn;
      delete this.forceFocusColumn;
    } else if (colNum || colNum === 0) {
      fld = this.getField(colNum);
      this.getEditForm().forceFocusedField = fld.name;
    }

    var record = this.getRecord(rowNum);

    this.view.isEditingGrid = true;

    // also called in case of new
    var form = this.getEditForm();

    // also make sure that the new indicator is send to the server
    if (record._new) {
      form.setValue('_new', true);
    }

    form.doEditRecordActions(false, record._new && !record._editedBefore);
    record._editedBefore = true;

    // must be done after doEditRecordActions
    if (this.rowHasErrors(rowNum)) {
      this.getEditForm().setErrors(this.getRowValidationErrors(rowNum));
      this.view.standardWindow.setDirtyEditForm(form);
    }

    if (record && record.editColumnLayout) {
      record.editColumnLayout.showSaveCancel();
    }

    this.view.messageBar.hide();
    if (this.view.parentView) {
      this.view.parentView.messageBar.hide();
    }

    this.markForCalculateSummaries();

    delete this._showingEditor;
    return ret;
  },

  closeAnyOpenEditor: function() {
    delete this.wasEditing;
    // close any editors we may have
    if (this.getEditRow() || this.getEditRow() === 0) {
      this.endEditing();
    }
  },

  validateField: function(field, validators, value, record, options) {
    // Smartclient passes in the grid field, use the editform field
    // as it contains the latest valuemap
    var editField = this.getEditForm().getField(field.name) || field;
    var ret = this.Super('validateField', [
      editField,
      validators,
      value,
      record,
      options
    ]);
    return ret;
  },

  refreshEditRow: function() {
    var editRow = this.view.viewGrid.getEditRow(),
      i,
      length;
    if (editRow || editRow === 0) {
      // don't refresh the frozen fields, this give strange
      // styling issues in chrome
      length = this.view.viewGrid.fields.length;
      for (i = 0; i < length; i++) {
        if (!this.fieldIsFrozen(i)) {
          this.view.viewGrid.refreshCell(editRow, i, true);
        }
      }
    }
  },

  // Set "allowEditCellRefresh" parameter to force a completely
  // redrawn in combo type fields when refreshing an editing cell
  // with the the focus on it.
  // See issue https://issues.openbravo.com/view.php?id=31198
  refreshCell: function(rowNum, colNum, refreshingRow) {
    return this.Super('refreshCell', [rowNum, colNum, refreshingRow, true]);
  },

  // having a valueMap property results in setValueMap to be called
  // on an item. On items with a picklist this causes calls to the
  // server side
  //  https://issues.openbravo.com/view.php?id=16611
  getEditItem: function() {
    var result = this.Super('getEditItem', arguments);
    if (
      Object.prototype.hasOwnProperty.call(result, 'valueMap') &&
      !result.valueMap
    ) {
      delete result.valueMap;
    }
    return result;
  },

  // set some flags to prevent the picklist fields from doing extra datasource
  // requests
  // https://issues.openbravo.com/view.php?id=16611
  storeUpdatedEditorValue: function(suppressChange, editCol) {
    if (this.fetchingSummaryRow) {
      return;
    }
    this._storingUpdatedEditorValue = true;
    this._preventDateParsing = true;
    this.Super('storeUpdatedEditorValue', arguments);
    delete this._storingUpdatedEditorValue;
    delete this._preventDateParsing;
  },

  // the form gets recreated many times, maintain the already read valuemap
  getEditorValueMap: function(field, values) {
    var editRow = this.getEditRow(),
      editValues = this.getEditValues(editRow);
    // valuemap is set in the processcolumnvalues of the ob-view-form.js
    if (editValues && editValues[field.name + '._valueMap']) {
      return editValues[field.name + '._valueMap'];
    }

    if (this.getEditForm() && this.getEditForm().getField(field.name)) {
      var liveField = this.getEditForm().getField(field.name);
      if (liveField.valueMap) {
        return liveField.valueMap;
      }
    }

    return this.Super('getEditorValueMap', arguments);
  },

  setFieldError: function(rowNum, fieldID, errorMessage, dontDisplay) {
    // if there are no errors then no need to clear
    // prevents an undefined exception because also keep errors in other
    // places then the editvalues._validationErrors
    if (!errorMessage && !this.Super('cellHasErrors', [rowNum, fieldID])) {
      return;
    }
    return this.Super('setFieldError', arguments);
  },

  cellHasErrors: function(rowNum, fieldID) {
    if (this.Super('cellHasErrors', arguments)) {
      return true;
    }
    if (this.getEditRow() === rowNum) {
      var itemName = this.getEditorName(rowNum, fieldID);

      if (this.getEditForm().hasFieldErrors(itemName)) {
        return true;
      }
      // sometimes the error is there but the error message is null
      if (
        Object.prototype.hasOwnProperty.call(
          this.getEditForm().getErrors(),
          itemName
        )
      ) {
        return true;
      }
    }
    return false;
  },

  getCellErrors: function(rowNum, fieldName) {
    var itemName;
    var ret = this.Super('getCellErrors', arguments);
    if (this.getEditRow() === rowNum) {
      return this.getEditForm().getFieldErrors(itemName);
    }
    return ret;
  },

  rowHasErrors: function(rowNum, colNum) {
    if (this.Super('rowHasErrors', arguments)) {
      return true;
    }
    if (this.getEditRow() === rowNum && this.getEditForm().hasErrors()) {
      return true;
    }
    var record = this.getRecord(rowNum);
    if (record) {
      return record[isc.OBViewGrid.ERROR_MESSAGE_PROP];
    }
    return false;
  },

  // we are being reshown, get new values for the combos
  visibilityChanged: function(visible) {
    if (visible && this.getEditRow()) {
      this.getEditForm().doChangeFICCall();
    }
    if (!this.view.isVisible() && this.hasErrors()) {
      isc.warn(OB.I18N.getLabel('OBUIAPP_TabWithErrors', [this.view.tabTitle]));
    }
  },

  isWritable: function(record) {
    return !record._readOnly;
  },

  allSelectedRecordsWritable: function() {
    var i,
      length = this.getSelectedRecords().length;
    for (i = 0; i < length; i++) {
      var record = this.getSelectedRecords()[i];
      if (!this.isWritable(record) || record._new) {
        return false;
      }
    }
    return true;
  },

  setRecordErrorMessage: function(rowNum, msg) {
    var record = this.getRecord(rowNum);
    if (!record) {
      return;
    }
    record[isc.OBViewGrid.ERROR_MESSAGE_PROP] = msg;
    if (msg) {
      record[this.recordBaseStyleProperty] = this.recordStyleError;
    } else {
      record[this.recordBaseStyleProperty] = null;
    }
    if (record.editColumnLayout) {
      record.editColumnLayout.editButton.setErrorState(msg);
      record.editColumnLayout.editButton.setErrorMessage(msg);
    }
    this.refreshRow(rowNum);
  },

  setRecordFieldErrorMessages: function(rowNum, errors) {
    var record = this.getRecord(rowNum);
    if (!record) {
      return;
    }
    if (record.editColumnLayout) {
      record.editColumnLayout.editButton.setErrorState(errors);
      record.editColumnLayout.editButton.setErrorMessage(
        OB.I18N.getLabel('OBUIAPP_ErrorInFields')
      );
    }
    this.setRowErrors(rowNum, errors);
    if (errors) {
      record[this.recordBaseStyleProperty] = this.recordStyleError;
    } else {
      record[this.recordBaseStyleProperty] = null;
    }

    if (this.frozenBody) {
      this.frozenBody.markForRedraw();
    }
    this.body.markForRedraw();
  },

  // overridden to handle the case that the rowNum is in fact
  // an edit state id
  getRecord: function(rowNum) {
    if (!isc.isA.Number(rowNum)) {
      // an edit id
      rowNum = this.getEditSessionRowNum(rowNum);
      return this.Super('getRecord', [rowNum]);
    }
    if (
      this.refreshingWithRecordSelected ||
      this.refreshingWithScrolledGrid ||
      this.isFilteringExternally
    ) {
      // if the grid if being refreshed do not try to return a record, just notify that is being loaded
      return Array.LOADING;
    }
    return this.Super('getRecord', arguments);
  },

  // always work with fixed rowheights
  // https://issues.openbravo.com/view.php?id=16307
  shouldFixRowHeight: function() {
    return true;
  },

  // needed for: https://issues.openbravo.com/view.php?id=16307
  getRowHeight: function() {
    return this.cellHeight;
  },

  // +++++++++++++++++ functions for the edit-link column +++++++++++++++++
  createRecordComponent: function(record, colNum) {
    var isSummary =
      record &&
      (record[this.groupSummaryRecordProperty] ||
        record[this.gridSummaryRecordProperty]);

    // don't support record components in summary fields
    if (isSummary) {
      return null;
    }

    if (this.isEditLinkColumn(colNum)) {
      var layout = isc.OBGridButtonsComponent.create({
        record: record,
        grid: this
      });
      layout.editButton.setErrorState(
        record[isc.OBViewGrid.ERROR_MESSAGE_PROP]
      );
      layout.editButton.setErrorMessage(
        record[isc.OBViewGrid.ERROR_MESSAGE_PROP]
      );
      record.editColumnLayout = layout;

      if (
        this.selection &&
        this.selection.lastSelectionItem &&
        this.selection.lastSelectionItem._new
      ) {
        this.selection.lastSelectionItem.editColumnLayout = layout;
      }

      if (record._new) {
        layout.showSaveCancel();
      } else {
        layout.showEditOpen();
      }
      return layout;
    } else {
      return this.Super('createRecordComponent', arguments);
    }
  },

  updateRecordComponent: function(record, colNum, component, recordChanged) {
    var isSummary =
      record &&
      (record[this.groupSummaryRecordProperty] ||
        record[this.gridSummaryRecordProperty]);

    // don't support record components in summary fields
    if (isSummary) {
      return null;
    }

    if (component.editButton) {
      if (recordChanged && component.record.editColumnLayout === component) {
        component.record.editColumnLayout = null;
      }
      component.record = record;
      record.editColumnLayout = component;
      component.editButton.setErrorState(
        record[isc.OBViewGrid.ERROR_MESSAGE_PROP]
      );
      component.editButton.setErrorMessage(
        record[isc.OBViewGrid.ERROR_MESSAGE_PROP]
      );
      if (record._new) {
        component.showSaveCancel();
      } else {
        component.showEditOpen();
      }
    } else {
      return this.Super('updateRecordComponent', arguments);
    }
    return component;
  },

  isEditLinkColumn: function(colNum) {
    return this.editLinkColNum === colNum;
  },

  getFieldFromColumnName: function(columnName) {
    var i,
      field,
      fields = this.view.propertyToColumns;

    for (i = 0; i < fields.length; i++) {
      if (fields[i].dbColumn === columnName) {
        field = fields[i];
        break;
      }
    }
    return field;
  },

  processColumnValue: function(rowNum, columnName, columnValue) {
    var field;
    if (!columnValue) {
      return;
    }
    field = this.getFieldFromColumnName(columnName);
    if (!field) {
      return;
    }
    this.setEditValue(rowNum, field.property, columnValue.value);
  },

  processFICReturn: function(response, data, request) {
    var context = response && response.clientContext,
      rowNum = context && context.rowNum,
      grid = context && context.grid,
      columnValues,
      prop,
      undef,
      field;

    if (rowNum === undef || !data || !data.columnValues) {
      return;
    }
    columnValues = data.columnValues;

    for (prop in columnValues) {
      if (Object.prototype.hasOwnProperty.call(columnValues, prop)) {
        field = this.getFieldFromColumnName(prop);
        // This call to the FIC was done to retrieve the missing values
        // Do not try to overwrite the existing values
        if (
          field &&
          !this.fieldIsVisibleInGrid(field.property) &&
          !this.getRecord(rowNum)[field.property]
        ) {
          grid.processColumnValue(rowNum, prop, columnValues[prop]);
        }
      }
    }
  },

  fieldIsVisibleInGrid: function(fieldName) {
    // this.getFields returns the list of fields that are currently visible in the grid,
    // as opposed to this.completeFields that contains the whole list of fields that can be shown in the grid
    var visibleFields = this.getFields();
    return visibleFields.containsProperty('name', fieldName);
  },

  updateRecord: function(recordIndex, data, req) {
    var sessionProperties = this.view.getContextInfo(true, true, false, true),
      me = this;
    data = OB.Utilities.Date.convertUTCTimeToLocalTime(
      data,
      this.completeFields
    );
    if (this.data.updateCacheData) {
      this.data.updateCacheData(data, req);
    }
    if (this.isGrouped) {
      // if the grid is group update its values to show the updated data
      this.setEditValues(recordIndex, data[0]);
    }
    this.selectRecord(this.getRecord(recordIndex));
    this.refreshRow(recordIndex);
    this.redraw();
    if (!this.view.isShowingForm) {
      OB.RemoteCallManager.call(
        'org.openbravo.client.application.window.FormInitializationComponent',
        sessionProperties,
        {
          MODE: 'SETSESSION',
          TAB_ID: this.view.tabId,
          PARENT_ID: this.view.getParentId(),
          ROW_ID: this.getSelectedRecord()
            ? this.getSelectedRecord().id
            : this.view.getCurrentValues().id
        },
        function(response, data, request) {
          var sessionAttributes = data.sessionAttributes,
            auxInputs = data.auxiliaryInputValues,
            attachmentExists = data.attachmentExists,
            prop;
          if (sessionAttributes) {
            me.view.viewForm.sessionAttributes = sessionAttributes;
          }

          if (auxInputs) {
            this.auxInputs = {};
            for (prop in auxInputs) {
              if (Object.prototype.hasOwnProperty.call(auxInputs, prop)) {
                me.view.viewForm.setValue(prop, auxInputs[prop].value);
                me.view.viewForm.auxInputs[prop] = auxInputs[prop].value;
              }
            }
          }
          me.view.viewForm.view.attachmentExists = attachmentExists;
          //compute and apply tab display logic again after fetching auxilary inputs.
          me.view.handleDefaultTreeView();
          me.view.updateSubtabVisibility();
        }
      );
    }
  },

  setSort: function(sortSpecifiers, forceSort) {
    this.isSorting = true;
    this.Super('setSort', arguments);
  }
});

// = OBGridToolStripIcon =
// The icons which are inside of OBGridToolStrip
isc.ClassFactory.defineClass('OBGridToolStripIcon', isc.ImgButton);

isc.OBGridToolStripIcon.addProperties({
  buttonType: null,
  /* This could be: edit - form - cancel - save */
  initWidget: function() {
    if (this.initWidgetStyle) {
      this.initWidgetStyle();
    }
    this.Super('initWidget', arguments);
  }
});

// = OBGridToolStripSeparator =
// The separator between icons of OBGridToolStrip
isc.ClassFactory.defineClass('OBGridToolStripSeparator', isc.Img);

isc.OBGridToolStripSeparator.addProperties({});

// = OBGridButtonsComponent =
// The component which is used to create the contents of the
// edit open column in the grid
isc.ClassFactory.defineClass('OBGridButtonsComponent', isc.HLayout);

isc.OBGridButtonsComponent.addProperties({
  OBGridToolStrip: null,
  saveCancelLayout: null,

  // the grid to which this component belongs
  grid: null,

  rowNum: null,

  // the record to which this component belongs
  record: null,

  initWidget: function() {
    var me = this,
      formButton;

    this.editButton = isc.OBGridToolStripIcon.create({
      buttonType: 'edit',
      originalPrompt: OB.I18N.getLabel('OBUIAPP_GridEditButtonPrompt'),
      prompt: OB.I18N.getLabel('OBUIAPP_GridEditButtonPrompt'),
      action: function() {
        var actionObject = {
          target: me,
          method: me.doEdit,
          parameters: null
        };
        me.grid.view.standardWindow.doActionAfterAutoSave(actionObject, true);
      },

      setErrorMessage: function(msg) {
        if (msg) {
          this.prompt = msg + '<br><br>' + this.originalPrompt;
        } else {
          this.prompt = this.originalPrompt;
        }
      },

      showable: function() {
        return !me.grid.view.readOnly && !me.record._readOnly;
      },

      show: function() {
        if (!this.showable()) {
          return;
        }
        return this.Super('show', arguments);
      }
    });

    formButton = isc.OBGridToolStripIcon.create({
      buttonType: 'form',
      prompt: OB.I18N.getLabel('OBUIAPP_GridFormButtonPrompt'),
      action: function() {
        var actionObject = {
          target: me,
          method: me.doOpen,
          parameters: null
        };
        me.grid.view.standardWindow.doActionAfterAutoSave(actionObject, true);
      }
    });

    this.buttonSeparator1 = isc.OBGridToolStripSeparator.create({});

    if (me.grid.view.readOnly) {
      this.buttonSeparator1.visibility = 'hidden';
    }

    this.addMembers([formButton, this.buttonSeparator1, this.editButton]);
    this.Super('initWidget', arguments);
  },

  addSaveCancelProgressButtons: function() {
    var me = this;
    // already been here
    if (this.cancelButton) {
      return;
    }

    this.progressIcon = isc.Img.create(this.grid.progressIconDefaults);
    this.progressIcon.setVisibility(false);
    this.addMember(this.progressIcon, 0);

    // is referred to in OBViewForm.showClickMask
    this.cancelButton = isc.OBGridToolStripIcon.create({
      buttonType: 'cancel',
      prompt: OB.I18N.getLabel('OBUIAPP_GridCancelButtonPrompt'),
      action: function() {
        me.doCancel();
      }
    });

    var saveButton = isc.OBGridToolStripIcon.create({
      buttonType: 'save',
      prompt: OB.I18N.getLabel('OBUIAPP_GridSaveButtonPrompt'),
      action: function() {
        if (
          me.grid.view &&
          me.grid.view.existsAction &&
          me.grid.view.existsAction(OB.EventHandlerRegistry.PRESAVE)
        ) {
          me.grid.view.executePreSaveActions(function() {
            me.doSave();
          });
          return;
        }
        me.doSave();
      }
    });

    this.addMembers([
      this.cancelButton,
      isc.OBGridToolStripSeparator.create({}),
      saveButton
    ]);
  },

  toggleProgressIcon: function(toggle) {
    if (toggle) {
      this.hideAllMembers();
      this.showMember(isc.OBViewGrid.PROGRESS);
    } else {
      this.hideMember(isc.OBViewGrid.PROGRESS);
      if (this.grid.view.isEditingGrid) {
        this.showSaveCancel();
      } else {
        this.showEditOpen();
      }
    }
  },

  hideAllMembers: function() {
    this.hideMember(isc.OBViewGrid.ICONS.EDIT_IN_GRID);
    this.hideMember(isc.OBViewGrid.ICONS.SEPARATOR1);
    this.hideMember(isc.OBViewGrid.ICONS.OPEN_IN_FORM);
    this.hideMember(isc.OBViewGrid.ICONS.PROGRESS);
    this.hideMember(isc.OBViewGrid.ICONS.CANCEL);
    this.hideMember(isc.OBViewGrid.ICONS.SEPARATOR2);
    this.hideMember(isc.OBViewGrid.ICONS.SAVE);
  },

  showEditOpen: function() {
    var offset = 0;
    if (this.cancelButton) {
      this.hideMember(isc.OBViewGrid.ICONS.SAVE);
      this.hideMember(isc.OBViewGrid.ICONS.SEPARATOR2);
      this.hideMember(isc.OBViewGrid.ICONS.CANCEL);
      this.hideMember(isc.OBViewGrid.ICONS.PROGRESS);
      offset = 1;
    }
    this.showMember(offset);
    if (this.editButton.showable()) {
      this.showMember(1 + offset);
      this.showMember(2 + offset);
    } else {
      this.hideMember(1 + offset);
      this.hideMember(2 + offset);
    }
    this.grid.currentEditColumnLayout = null;
  },

  showSaveCancel: function() {
    this.addSaveCancelProgressButtons();

    this.hideMember(isc.OBViewGrid.ICONS.EDIT_IN_GRID);
    this.hideMember(isc.OBViewGrid.ICONS.SEPARATOR1);
    this.hideMember(isc.OBViewGrid.ICONS.OPEN_IN_FORM);
    this.hideMember(isc.OBViewGrid.ICONS.PROGRESS);

    this.showMember(isc.OBViewGrid.ICONS.CANCEL);
    this.showMember(isc.OBViewGrid.ICONS.SEPARATOR2);
    this.showMember(isc.OBViewGrid.ICONS.SAVE);

    this.grid.currentEditColumnLayout = this;
  },

  doEdit: function() {
    this.showSaveCancel();
    this.grid.selectSingleRecord(this.record);
    var rowNum = this.grid.getRecordIndex(this.record);
    this.grid.startEditing(rowNum);
  },

  doOpen: function() {
    this.grid.endEditing();
    this.grid.view.editRecord(this.record);
  },

  doSave: function() {
    // note change back to editOpen is done in the editComplete event of the
    // grid itself
    this.grid.endEditing();
  },

  doCancel: function() {
    this.grid.cancelEditing();
    // force update of toolbar buttons state
    // https://issues.openbravo.com/view.php?id=31567
    this.grid.view.toolBar.updateButtonState(true);
  },

  hideMember: function(memberNo) {
    if (!this.members[memberNo]) {
      return;
    }
    // already hidden
    if (
      this.members[memberNo] &&
      this.members[memberNo].visibility === isc.Canvas.HIDDEN
    ) {
      return;
    }
    this.Super('hideMember', arguments);
  },

  showMember: function(memberNo) {
    if (!this.members[memberNo]) {
      return;
    }
    // already visible
    if (
      this.members[memberNo] &&
      (this.members[memberNo].visibility === isc.Canvas.INHERIT ||
        this.members[memberNo].visibility === isc.Canvas.VISIBLE)
    ) {
      return;
    }
    this.Super('showMember', arguments);
  }
});
