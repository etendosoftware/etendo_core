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
 * All portions are Copyright (C) 2013-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

isc.ClassFactory.defineClass('OBTreeItemPopupFilterWindow', isc.OBPopup);

isc.OBTreeItemPopupFilterWindow.addProperties({
  canDragReposition: true,
  canDragResize: true,
  dismissOnEscape: true,
  destroyOnClose: false,
  showMaximizeButton: true,
  multiselect: true,

  defaultTreeGridField: {
    canFreeze: true,
    canGroupBy: false
  },

  initWidget: function() {
    var treeWindow = this,
      okButton,
      cancelButton,
      i;

    okButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OBUISC_Dialog.OK_BUTTON_TITLE'),
      click: function() {
        treeWindow.accept();
      }
    });
    cancelButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OBUISC_Dialog.CANCEL_BUTTON_TITLE'),
      click: function() {
        treeWindow.closeClick();
      }
    });

    OB.Utilities.applyDefaultValues(
      this.treeGridFields,
      this.defaultTreeGridField
    );

    for (i = 0; i < this.treeGridFields.length; i++) {
      this.treeGridFields[i].canSort =
        this.treeGridFields[i].canSort === false ? false : true;
      if (this.treeGridFields[i].disableFilter) {
        this.treeGridFields[i].canFilter = false;
      } else {
        this.treeGridFields[i].canFilter = true;
      }
    }
    this.treeGrid = isc.OBTreeGrid.create({
      view: this.view,
      treePopup: this,
      autoFetchData: true,
      filterOnKeypress: true,
      selectionAppearance: 'checkbox',
      dataSourceId: this.dataSourceId,
      treeReferenceId: this.treeReferenceId,
      width: '100%',
      height: '100%',
      bodyStyleName: 'OBGridBody',
      showFilterEditor: true,
      alternateRecordStyles: true,
      sortField: this.displayField,

      init: function() {
        var theGrid;
        OB.Datasource.get(this.dataSourceId, this, null, true);
        if (this.view.paramWindow) {
          // the checkShowFilterFunnelIcon implementation of OBPickAndExecuteGrid requires the contentView to be defined
          theGrid = this.view.theForm.getField(
            this.treePopup.filterItem.containerWidget.grid.parentElement
              .parameterName
          ).canvas.viewGrid;
          this.contentView = theGrid.contentView;
          this.copyFunctionsFromGrid(theGrid);
        } else {
          this.copyFunctionsFromViewGrid();
        }
        this.Super('init', arguments);
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
      },

      copyFunctionsFromViewGrid: function() {
        this.copyFunctionsFromGrid(this.view.viewGrid);
      },

      copyFunctionsFromGrid: function(grid) {
        if (grid == null) {
          // need a grid just to copy some functions
          // if none is available, create a dummy one
          grid = isc.OBViewGrid.create({ fields: [], view: {} });
        }
        this.filterEditorProperties = grid.filterEditorProperties;
        this.checkShowFilterFunnelIcon = grid.checkShowFilterFunnelIcon;
        this.isGridFiltered = grid.isGridFiltered;
        this.isGridFilteredWithCriteria = grid.isGridFilteredWithCriteria;
        this.isValidFilterField = grid.isValidFilterField;
        this.convertCriteria = grid.convertCriteria;
        this.resetEmptyMessage = grid.resetEmptyMessage;
        this.filterData = grid.filterData;
        this.loadingDataMessage = grid.loadingDataMessage;
        this.emptyMessage = grid.emptyMessage;
        this.noDataEmptyMessage = grid.noDataEmptyMessage;
      },

      onFetchData: function(criteria, requestProperties) {
        requestProperties = requestProperties || {};
        requestProperties.params = this.getFetchRequestParams(
          requestProperties.params
        );
      },

      getFetchRequestParams: function(params) {
        params = params || {};
        if (this.getSelectedRecord()) {
          params._targetRecordId = this.targetRecordId;
        }
        return params;
      },

      dataArrived: function() {
        this.Super('dataArrived', arguments);
      },

      fields: this.treeGridFields,

      handleFilterEditorSubmit: function(criteria, context) {
        this.Super('handleFilterEditorSubmit', [criteria, context]);
      },

      setDataSource: function(ds, fields) {
        var me = this,
          i;
        ds.transformRequest = function(dsRequest) {
          var target = window[dsRequest.componentId];
          dsRequest.params = dsRequest.params || {};
          dsRequest.params._startRow = 0;
          dsRequest.params._endRow = OB.Properties.TreeDatasourceFetchLimit;
          dsRequest.params.treeReferenceId = target.treeReferenceId;
          return this.Super('transformRequest', arguments);
        };

        ds.transformResponse = function(dsResponse, dsRequest, jsonData) {
          var i, node;
          if (jsonData.response.error) {
            dsResponse.error = jsonData.response.error;
          }
          if (jsonData.response && jsonData.response.data && me.showNodeIcons) {
            for (i = 0; i < jsonData.response.data.length; i++) {
              node = jsonData.response.data[i];
              if (node.showDropIcon) {
                node.icon = OB.Styles.OBTreeGrid.iconFolder;
              } else {
                node.icon = OB.Styles.OBTreeGrid.iconNode;
              }
            }
          }
          return this.Super('transformResponse', arguments);
        };

        ds.handleError = function(response, request) {
          if (
            response &&
            response.error &&
            response.error.type === 'tooManyNodes'
          ) {
            isc.warn(OB.I18N.getLabel('OBUIAPP_TooManyNodes'));
          }
        };

        fields = this.treePopup.treeGridFields;
        for (i = 0; i < fields.length; i++) {
          fields[i].escapeHTML = true;
        }
        ds.primaryKeys = {
          id: 'id'
        };
        return this.Super('setDataSource', [ds, fields]);
      },

      // show or hide the filter button
      filterEditorSubmit: function(criteria) {
        this.checkShowFilterFunnelIcon(criteria);
      }
    });

    this.items = [
      isc.VLayout.create({
        height: this.height,
        width: this.width,
        members: [
          this.treeGrid,
          isc.LayoutSpacer.create({
            height: 10
          }),
          isc.HLayout.create({
            styleName: this.buttonBarStyleName,
            height: 30,
            defaultLayoutAlign: 'center',
            members: [
              isc.LayoutSpacer.create({}),
              okButton,
              isc.LayoutSpacer.create({
                width: 20
              }),
              cancelButton,
              isc.LayoutSpacer.create({})
            ]
          })
        ]
      })
    ];
    this.Super('initWidget', arguments);
  },

  destroy: function() {
    var i;
    for (i = 0; i < this.items.length; i++) {
      this.items[i].destroy();
    }
    return this.Super('destroy', arguments);
  },

  show: function(refreshGrid) {
    if (refreshGrid) {
      this.treeGrid.invalidateCache();
    }
    var ret = this.Super('show', arguments);
    if (this.treeGrid.isDrawn()) {
      this.treeGrid.focusInFilterEditor();
    } else {
      isc.Page.setEvent(
        isc.EH.IDLE,
        this.treeGrid,
        isc.Page.FIRE_ONCE,
        'focusInFilterEditor'
      );
    }

    this.treeGrid.checkShowFilterFunnelIcon(this.treeGrid.getCriteria());

    return ret;
  },

  resized: function() {
    this.items[0].setWidth(this.width - 4);
    this.items[0].setHeight(this.height - 40);
    this.items[0].redraw();
    return this.Super('resized', arguments);
  },

  accept: function() {
    if (this.changeCriteriacallback) {
      this.fireCallback(this.changeCriteriacallback, 'value', [
        this.getCriteria()
      ]);
    }
    this.hide();
  },

  clearValues: function() {
    this.treeGrid.deselectAllRecords();
  },

  getCriteria: function() {
    var selection = this.treeGrid.getSelection(),
      criteria = {},
      i,
      len = selection.length,
      fieldName =
        this.fieldName + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER;
    if (len === 0) {
      return {};
    } else if (len === 1) {
      criteria._constructor = 'AdvancedCriteria';
      criteria.operator = 'and';
      criteria.criteria = [
        {
          fieldName: fieldName,
          operator: 'equals',
          value: selection[0][OB.Constants.IDENTIFIER]
        }
      ];
    } else {
      criteria._constructor = 'AdvancedCriteria';
      criteria._OrExpression = true;
      criteria.operator = 'or';
      criteria.fieldName = fieldName;
      criteria.criteria = [];
      for (i = 0; i < len; i++) {
        criteria.criteria.push({
          fieldName: fieldName,
          operator: 'equals',
          value: selection[i][OB.Constants.IDENTIFIER]
        });
      }
    }
    return criteria;
  }
});

isc.ClassFactory.defineClass('OBTreeFilterItem', isc.OBTextItem);

isc.OBTreeFilterItem.addProperties({
  showPickerIcon: true,
  filterDialogConstructor: isc.OBTreeItemPopupFilterWindow,
  lastValueFromPopup: null,
  pickerConstructor: 'ImgButton',
  allowExpressions: true,
  pickerIconDefaults: {
    name: 'showDateRange',
    width: 21,
    height: 21,
    showOver: false,
    showFocused: false,
    showFocusedWithItem: false,
    hspace: 0,
    click: function(form, item, icon) {
      if (!item.disabled) {
        item.showDialog();
      }
    }
  },

  filterDialogCallback: function(criterion) {
    this.updateCriterion(criterion);
    this.lastValueFromPopup = this.getValue();
    this.form.grid.performAction();
  },

  //This function updates the criterion of the tree filter, deletes the old
  //criterion and adds the new criterion.
  updateCriterion: function(criterion) {
    if (!criterion.criteria) {
      this.setValue(null);
    } else {
      this.setCriterion(criterion);
    }
  },

  init: function() {
    var field, treeGridFields, treeReferenceId, dataSourceId, view;
    this.pickerIconSrc = OB.Styles.OBFormField.DefaultSearch.pickerIconSrc;
    this.Super('init', arguments);
    field = this.grid.getField(this.name);
    this.criteriaField = field.displayField;
    if (this.selectorWindow) {
      treeGridFields = this.selectorWindow.selectorGridFields.find(
        'name',
        this.name
      ).treeGridFields;
      treeReferenceId = this.selectorWindow.selectorGridFields.find(
        'name',
        this.name
      ).treeReferenceId;
      dataSourceId = this.selectorWindow.selectorGridFields.find(
        'name',
        this.name
      ).dataSourceId;
      view = this.selectorWindow.selectorGrid.selector.form.view;
    } else {
      treeGridFields = field.editorProperties.treeGridFields;
      treeReferenceId = field.editorProperties.treeReferenceId;
      dataSourceId = field.editorProperties.dataSourceId;
      view = this.grid.parentElement.view;
    }
    this.addAutoChild(
      'filterDialog',
      {
        title: this.title,
        filterItem: this,
        treeGridFields: treeGridFields,
        treeReferenceId: treeReferenceId,
        dataSourceId: dataSourceId,
        fieldName: field.name,
        view: view,
        changeCriteriacallback: this.getID() + '.filterDialogCallback(value)'
      },
      'isc.OBTreeItemPopupFilterWindow'
    );
  },

  destroy: function() {
    if (this.filterDialog) {
      this.filterDialog.destroy();
    }
    return this.Super('destroy', arguments);
  },

  showDialog: function() {
    var hasChanged = false;
    if (this.lastValueFromPopup !== this.getValue()) {
      hasChanged = true;
    }
    this.filterDialog.show(hasChanged);
  }
});
