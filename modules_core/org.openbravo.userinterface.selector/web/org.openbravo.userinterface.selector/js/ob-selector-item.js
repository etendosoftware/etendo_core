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
 * All portions are Copyright (C) 2011-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
// = OBSelectorPopupWindow =
// The selector popup window shown when clicking the picker icon. Contains
// a selection grid and cancel/ok buttons.
//
isc.ClassFactory.defineClass('OBSelectorPopupWindow', isc.OBPopup);

isc.OBSelectorPopupWindow.addProperties({
  canDragReposition: true,
  canDragResize: true,
  dismissOnEscape: true,
  showMaximizeButton: true,
  multiselect: false,

  defaultSelectorGridField: {
    canFreeze: true,
    canGroupBy: false
  },

  initWidget: function() {
    var selectorWindow = this,
      okButton,
      createNewButton,
      cancelButton,
      operator,
      i;

    this.setFilterEditorProperties(this.selectorGridFields);

    okButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OBUISC_Dialog.OK_BUTTON_TITLE'),
      click: function() {
        selectorWindow.setValueInField();
      }
    });
    createNewButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('UINAVBA_CREATE_NEW'),
      showIf: function() {
        if (selectorWindow.selector.processId) {
          return true;
        } else {
          return false;
        }
      },
      click: function() {
        var enteredValues = [],
          criteria,
          value,
          i;
        if (
          selectorWindow &&
          selectorWindow.selectorGrid &&
          selectorWindow.selectorGrid.filterEditor &&
          selectorWindow.selectorGrid.filterEditor.getValues()
        ) {
          criteria = selectorWindow.selectorGrid.filterEditor.getValues()
            .criteria;
          if (Object.prototype.toString.call(criteria) === '[object Array]') {
            for (i = 0; i < criteria.length; i++) {
              value = {};
              value[criteria[i].fieldName] = criteria[i].value;
              enteredValues.push(value);
            }
          }
        }
        selectorWindow.closeClick();
        selectorWindow.selector.openProcess(enteredValues);
      }
    });
    cancelButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OBUISC_Dialog.CANCEL_BUTTON_TITLE'),
      click: function() {
        selectorWindow.closeClick();
      }
    });

    OB.Utilities.applyDefaultValues(
      this.selectorGridFields,
      this.defaultSelectorGridField
    );

    if (this.selector.popupTextMatchStyle === 'substring') {
      operator = 'iContains';
    } else {
      operator = 'iStartsWith';
    }

    for (i = 0; i < this.selectorGridFields.length; i++) {
      this.selectorGridFields[i].canSort =
        this.selectorGridFields[i].canSort === false ? false : true;
      if (this.selectorGridFields[i].name === OB.Constants.IDENTIFIER) {
        this.selectorGridFields[i].escapeHTML = true;
      } else {
        this.selectorGridFields[i].escapeHTML =
          this.selectorGridFields[i].escapeHTML === false ? false : true;
      }
      if (this.selectorGridFields[i].disableFilter) {
        this.selectorGridFields[i].canFilter = false;
      } else {
        this.selectorGridFields[i].canFilter = true;
      }
      // apply the proper operator for the filter of text fields
      if (
        isc.SimpleType.inheritsFrom(this.selectorGridFields[i].type, 'text')
      ) {
        if (this.selectorGridFields[i].filterEditorProperties.operator) {
          // if there is an operator defined in the grid configuration, use it
          this.selectorGridFields[i].operator = operator = this
            .selectorGridFields[i].filterEditorProperties.operator;
        } else {
          // if not, use the operator based on the popupTextMatchStyle
          this.selectorGridFields[i].operator = operator;
          this.selectorGridFields[i].filterEditorProperties.operator = operator;
        }
      }
    }
    if (
      !this.dataSource.fields ||
      !this.dataSource.fields.length ||
      this.dataSource.fields.length === 0
    ) {
      this.dataSource.fields = this.selectorGridFields;
      this.dataSource.init();
    }
    this.selectorGrid = isc.OBGrid.create({
      selector: this.selector,
      selectionAppearance: this.selectionAppearance,

      // drawAllMaxCells is set to 0 to prevent extra reads of data
      // Smartclient will try to read until drawAllMaxCells has been reached
      drawAllMaxCells: 0,

      dataProperties: {
        useClientFiltering: false,
        useClientSorting: false
      },

      width: this.selectorGridProperties.width,
      height: this.selectorGridProperties.height,
      alternateRecordStyles: this.selectorGridProperties.alternateRecordStyles,
      dataSource: this.dataSource,
      showFilterEditor: true,
      sortField: this.displayField,

      onFetchData: function(criteria, requestProperties) {
        this.setFechingData();
        requestProperties = requestProperties || {};
        requestProperties.params = this.getFetchRequestParams(
          requestProperties.params
        );
      },

      getFetchRequestParams: function(params) {
        var requestType = 'Window';
        params = params || {};
        if (this.selector) {
          isc.OBSelectorItem.prepareDSRequest(
            params,
            this.selector,
            requestType
          );
        }

        params._requestType = requestType;

        if (this.getSelectedRecord()) {
          params._targetRecordId = this.targetRecordId;
        }
        return params;
      },

      dataArrived: function() {
        var record,
          rowNum,
          i,
          selectedRecords = [],
          ds,
          ids;
        this.Super('dataArrived', arguments);
        // check if a record has been selected, if
        // not take the one
        // from the selectorField
        // by doing this when data arrives the selection
        // will show up
        // when the record shows in view
        if (this.selector.selectorWindow.multiselect) {
          ds = this.data;
          ids = this.selector.selectorWindow.selectedIds;
          for (i = 0; i < ids.length; i++) {
            selectedRecords.push(ds.find(OB.Constants.ID, ids[i]));
          }
          this.selectRecords(selectedRecords);
        } else {
          if (this.targetRecordId) {
            record = this.data.find(
              this.selector.valueField,
              this.targetRecordId
            );
            rowNum = this.getRecordIndex(record);
            this.selectSingleRecord(record);
            // give grid time to draw
            this.fireOnPause('scrollRecordIntoView_' + rowNum, function() {
              this.scrollRecordIntoView(rowNum, true);
            });
            delete this.targetRecordId;
          } else if (this.data.lengthIsKnown() && this.data.getLength() === 1) {
            // only one record, select that one straight away
            this.selectSingleRecord(0);
          } else {
            this.selectSingleRecord(null);
          }
        }
      },
      fields: this.selectorGridFields,
      recordDoubleClick: function() {
        selectorWindow.setValueInField();
      },

      handleFilterEditorSubmit: function(criteria, context) {
        var ids = [],
          crit = {},
          len,
          i,
          c,
          found,
          fixedCriteria;
        if (!selectorWindow.multiselect) {
          this.Super('handleFilterEditorSubmit', arguments);
          return;
        }
        if (criteria && criteria._selectorDefinitionId) {
          criteria = undefined;
        }
        if (criteria && criteria.criteria) {
          fixedCriteria = [];
          // remove from criteria dummy one created to preserve selected items
          for (i = 0; i < criteria.criteria.length; i++) {
            if (
              !criteria.criteria[i].dummyCriteria &&
              criteria.criteria[i].fieldName !== '_selectorDefinitionId'
            ) {
              fixedCriteria.push(criteria.criteria[i]);
            }
          }
          criteria.criteria = fixedCriteria;
        }

        len = this.selector.selectorWindow.selectedIds.length;
        for (i = 0; i < len; i++) {
          ids.push({
            fieldName: 'id',
            operator: 'equals',
            value: this.selector.selectorWindow.selectedIds[i]
          });
        }

        if (len > 0) {
          crit._constructor = 'AdvancedCriteria';
          crit._OrExpression = true; // trick to get a really _or_ in the backend
          crit.operator = 'or';
          crit.criteria = ids;

          c = (criteria && criteria.criteria) || [];
          found = false;

          for (i = 0; i < c.length; i++) {
            if (
              c[i].fieldName &&
              c[i].fieldName !== '_selectorDefinitionId' &&
              c[i].value !== ''
            ) {
              found = true;
              break;
            }
          }

          if (!found) {
            if (!criteria) {
              criteria = {
                _constructor: 'AdvancedCriteria',
                operator: 'and',
                criteria: []
              };
            }
            if (criteria.criteria) {
              // adding an *always true* sentence
              criteria.criteria.push({
                fieldName: 'id',
                operator: 'notNull',
                dummyCriteria: true
              });
            }
          }
          crit.criteria.push(criteria); // original filter
        } else {
          crit = criteria;
        }
        this.Super('handleFilterEditorSubmit', [crit, context]);
      },
      selectionChanged: function(record, state) {
        if (this.selector.selectorWindow.selectedIds) {
          if (state) {
            this.selector.selectorWindow.selectId(record[OB.Constants.ID]);
          } else {
            this.selector.selectorWindow.selectedIds.remove(
              record[OB.Constants.ID]
            );
          }
          this.markForRedraw('Selection changed');
        }

        this.Super('selectionChanged', arguments);
      }
    });

    this.items = [
      this.selectorGrid,
      isc.HLayout.create({
        styleName: this.buttonBarStyleName,
        height: this.buttonBarHeight,
        defaultLayoutAlign: 'center',
        members: [
          isc.LayoutSpacer.create({}),
          okButton,
          isc.LayoutSpacer.create({
            width: this.buttonBarSpace
          }),
          createNewButton,
          isc.LayoutSpacer.create({
            width: this.buttonBarSpace
          }),
          cancelButton,
          isc.LayoutSpacer.create({})
        ]
      })
    ];
    this.Super('initWidget', arguments);
  },

  setFilterEditorProperties: function(gridFields) {
    var type,
      selectorWindow = this,
      keyPressFunction,
      i,
      gridField;

    keyPressFunction = function(item, form, keyName, characterValue) {
      if (keyName === 'Escape') {
        selectorWindow.hide();
        return false;
      }
      return true;
    };

    for (i = 0; i < gridFields.length; i++) {
      gridField = gridFields[i];

      type = isc.SimpleType.getType(gridField.type);

      if (type.filterEditorType && !gridField.filterEditorType) {
        gridField.filterEditorType = type.filterEditorType;
      }

      gridField.canFilter = gridField.canFilter === false ? false : true;
      gridField.filterOnKeypress =
        gridField.filterOnKeypress === false ? false : true;

      if (!gridField.filterEditorProperties) {
        gridField.filterEditorProperties = {
          required: false
        };
      } else {
        gridField.filterEditorProperties.required = false;
      }

      gridField.filterEditorProperties.keyPress = keyPressFunction;

      if (!gridField.filterEditorProperties.icons) {
        gridField.filterEditorProperties.icons = [];
      }

      gridField.filterEditorProperties.showLabel = false;
      gridField.filterEditorProperties.showTitle = false;
      gridField.filterEditorProperties.selectorWindow = selectorWindow;
      gridField.filterEditorProperties.textMatchStyle =
        selectorWindow.selector.popupTextMatchStyle;
    }
  },

  closeClick: function() {
    this.hide(arguments);
    this.selector.focusInItem();
  },

  hide: function() {
    this.Super('hide', arguments);
    //focus is now moved to the next item in the form automatically
    if (!this.selector.form.getFocusItem()) {
      this.selector.focusInItem();
    }
  },

  show: function(applyDefaultFilter) {
    // draw now already otherwise the filter does not work the
    // first time
    var ret = this.Super('show', arguments);
    if (applyDefaultFilter) {
      this.setFilterByIdEditorCriteria(this.defaultFilter);
      this.selectorGrid.setFilterEditorCriteria(this.defaultFilter);
      this.selectorGrid.filterByEditor();
    }
    if (this.selectorGrid.isDrawn()) {
      this.selectorGrid.focusInFilterEditor();
    } else {
      isc.Page.setEvent(
        isc.EH.IDLE,
        this.selectorGrid,
        isc.Page.FIRE_ONCE,
        'focusInFilterEditor'
      );
    }

    if (this.selector.getValue()) {
      this.selectorGrid.selectSingleRecord(
        this.selectorGrid.data.find(this.valueField, this.selector.getValue())
      );
    } else {
      this.selectorGrid.selectSingleRecord(null);
    }

    return ret;
  },

  open: function() {
    var selectorWindow = this,
      callback,
      data;

    data = {
      _selectorDefinitionId:
        this.selectorDefinitionId || this.selector.selectorDefinitionId,
      _isFilterByIdSupported: true
    };

    // on purpose not passing the third boolean param
    if (
      this.selector &&
      this.selector.form &&
      this.selector.form.view &&
      this.selector.form.view.getContextInfo
    ) {
      isc.addProperties(
        data,
        this.selector.form.view.getContextInfo(false, true)
      );
    } else if (this.view && this.view.getUnderLyingRecordContext) {
      isc.addProperties(
        data,
        this.view.getUnderLyingRecordContext(false, true)
      );
    } else if (
      this.view &&
      this.view.sourceView &&
      this.view.sourceView.getContextInfo
    ) {
      isc.addProperties(data, this.view.sourceView.getContextInfo(false, true));
    } else if (
      this.selector &&
      this.selector.view &&
      this.selector.view.getUnderLyingRecordContext
    ) {
      // selector in a param window
      isc.addProperties(
        data,
        this.selector.view.getUnderLyingRecordContext(
          false,
          true,
          null,
          this.selector.isComboReference
        )
      );
    }

    callback = function(resp, data, req) {
      selectorWindow.fetchDefaultsCallback(resp, data, req);
    };
    OB.RemoteCallManager.call(
      'org.openbravo.userinterface.selector.SelectorDefaultFilterActionHandler',
      data,
      null,
      callback
    );
  },

  fetchDefaultsCallback: function(rpcResponse, data, rpcRequest) {
    var defaultFilter = {};
    if (data) {
      defaultFilter = {}; // Reset filter
      isc.addProperties(defaultFilter, data);
    }

    // adds the selector id to filter used to get filter information
    defaultFilter._selectorDefinitionId = this.selector.selectorDefinitionId;
    this.defaultFilter = defaultFilter;
    this.selectorGrid.targetRecordId = this.selector.getValue();
    this.show(true);
  },

  setFilterByIdEditorCriteria: function(defaultFilter) {
    var editForm = this.getFilterEditorForm(),
      filterField,
      idFilter,
      i;
    if (!defaultFilter.idFilters || !editForm) {
      return;
    }
    for (i = 0; i < defaultFilter.idFilters.length; i++) {
      idFilter = defaultFilter.idFilters[i];
      filterField = editForm.getField(idFilter.fieldName);
      if (filterField) {
        // Force filter by id
        filterField.filterType = 'id';
        filterField.filterAuxCache = [idFilter];
        defaultFilter[idFilter.fieldName] = idFilter._identifier;
      }
    }
    // idFilters is no longer needed. Its information is already included into the grid filters.
    delete this.defaultFilter.idFilters;
  },

  getFilterEditorForm: function() {
    if (
      !this.selectorGrid ||
      !this.selectorGrid.filterEditor ||
      !this.selectorGrid.filterEditor.getEditForm
    ) {
      return null;
    }
    return this.selectorGrid.filterEditor.getEditForm();
  },

  setValueInField: function() {
    if (this.multiselect) {
      this.selector.setSelectedRecords(this.selectorGrid.getSelectedRecords());
    } else {
      this.selector.setValueFromRecord(
        this.selectorGrid.getSelectedRecord(),
        true
      );
    }
    this.hide();
  }
});

// = Selector Item =
// Contains the OBSelector Item. This widget consists of two main parts:
// 1) a combo box with a picker icon
// 2) a popup window showing a search grid with data
//
isc.ClassFactory.defineClass('OBSelectorItem', isc.OBComboBoxItem);

isc.ClassFactory.mixInInterface('OBSelectorItem', 'OBLinkTitleItem');

isc.OBSelectorItem.addProperties({
  hasPickList: true,
  popupTextMatchStyle: 'startswith',
  suggestionTextMatchStyle: 'startswith',
  showOptionsFromDataSource: true,

  // forces fetch whenever drop down is opened
  addDummyCriterion: true,

  // https://issues.openbravo.com/view.php?id=18739
  selectOnFocus: false,
  // still do select on focus initially
  doInitialSelectOnFocus: true,

  // if addUnknownValues is set to true, fetch is performed on item blur
  addUnknownValues: false,
  // ** {{{ selectorGridFields }}} **
  // the definition of the columns in the popup window
  selectorGridFields: [
    {
      title: OB.I18N.getLabel('OBUISC_Identifier'),
      name: OB.Constants.IDENTIFIER
    }
  ],

  // Do not fetch data upon creation
  // http://www.smartclient.com/docs/8.1/a/b/c/go.html#attr..ComboBoxItem.optionDataSource
  fetchMissingValues: false,

  autoFetchData: false,
  showPickerIcon: true,
  //  selectors should not be validated on change, only after its content has been deleted
  //  and after an option of the combo has been selected
  //  see issue 19956 (https://issues.openbravo.com/view.php?id=19956)
  validateOnChange: false,
  completeOnTab: true,
  // note validateonexit does not work when completeOnTab is true
  // setting it anyway, the this.validate() is called in the blur
  validateOnExit: true,

  pickListProperties: {
    fetchDelay: 400,
    showHeaderContextMenu: false,
    dataProperties: {
      useClientFiltering: false
    }
  },

  hidePickListOnBlur: function() {
    // when the form gets redrawn the focus may not be in
    // the item but it is still the item which gets the focus
    // after redrawing
    if (
      this.form &&
      this.form._isRedrawing &&
      this.form.getFocusItem() === this
    ) {
      return;
    }

    this.Super('hidePickListOnBlur', arguments);
  },

  // adds a single entry to valueMap if it is not already present
  addValueMapEntry: function(id, identifier) {
    var i,
      entries = this.entries || [],
      entry = {},
      valueField = this.getValueFieldName(),
      length = entries.length;
    for (i = 0; i < length; i++) {
      if (entries[i][valueField] === id) {
        return;
      }
    }

    // not found add/create a new one
    entry[valueField] = id;

    if (id && identifier) {
      entry[OB.Constants.ID] = id;
      entry[OB.Constants.IDENTIFIER] = identifier;
    }

    entries.push(entry);
    this.setEntries(entries);
  },

  // all entries are set at once in client, this happens when a callout
  // computes all possible values, from this point subsequent filtering
  // requires to be done in local
  setEntries: function(entries) {
    var length = entries.length,
      i,
      id,
      identifier,
      valueMap = {},
      valueMapElement,
      valueField = this.getValueFieldName(),
      valueFieldContent,
      valueMapData = [];

    if (!this.setValueMap) {
      return;
    }

    for (i = 0; i < length; i++) {
      id = entries[i][OB.Constants.ID] || '';
      identifier = entries[i][OB.Constants.IDENTIFIER] || '';
      valueMap[id] = identifier;

      valueMapElement = {};
      valueMapElement[OB.Constants.IDENTIFIER] = identifier;
      valueMapElement[OB.Constants.ID] = id;

      // We include the value field into the entry.
      // With this we avoid to retrieve an incorrect value from mapValueToDisplay() when looking for undefined values.
      // This could happen when creating a new record in form view, after clearing the values.
      // See issue https://issues.openbravo.com/view.php?id=31331
      if (valueField) {
        valueFieldContent = entries[i][valueField] || '';
        valueMapElement[valueField] = valueFieldContent;
      }

      valueMapData.push(valueMapElement);
    }

    this.wholeMapSet = true; // flag to use local filtering from now on
    this.preventPickListRequest = true; // preventing 1st request triggered by setValueMap
    this.setValueMap(valueMap);

    if (this.pickList) {
      // there is no a proper way of initializing local data, let's do it editing
      // picklist.data properties
      this.pickList.data.localData = valueMapData;
      this.pickList.data.allRows = valueMapData;
      this.pickList.data.allRowsCriteria = this.pickList.data.criteria;
      this.pickList.data.cachedRows = valueMapData.length;
    }
  },

  setUpPickList: function(show, queueFetches, request) {
    this.pickListProperties.canResizeFields = true;
    // drawAllMaxCells is set to 0 to prevent extra reads of data
    // Smartclient will try to read until drawAllMaxCells has been reached
    this.pickListProperties.drawAllMaxCells = 0;
    // Set the pickListWidth just before being shown.
    this.setPickListWidth();
    this.Super('setUpPickList', arguments);
  },

  // don't do update value in all cases, updatevalue results in a data source request
  // to the server, so only do updatevalue when the user changes information
  // https://issues.openbravo.com/view.php?id=16611
  updateValue: function() {
    if (
      this.form &&
      this.form.grid &&
      (this.form.grid._storingUpdatedEditorValue ||
        this.form.grid._showingEditor ||
        this.form.grid._hidingInlineEditor)
    ) {
      // prevent updatevalue while the form is being shown or hidden
      return;
    }
    this.Super('updateValue', arguments);
  },

  setValue: function(val) {
    var i, displayedVal;

    if (val && this.valueMap) {
      displayedVal = this.valueMap[val];
      for (i in this.valueMap) {
        if (Object.prototype.hasOwnProperty.call(this.valueMap, i)) {
          if (this.valueMap[i] === displayedVal && i !== val) {
            // cleaning up valueMap: there are 2 values that display the same info, keep just the one for
            // the current value
            delete this.valueMap[i];
            break;
          }
        }
      }
    } else {
      //Select by default the first option in the picklist, if possible
      this.selectFirstPickListOption();
    }

    if (this._clearingValue) {
      this._editorEnterValue = null;
    }

    this.Super('setValue', arguments);
  },

  selectFirstPickListOption: function() {
    var firstRecord;
    if (this.pickList) {
      if (this.pickList.data && this.pickList.data.totalRows > 0) {
        firstRecord = this.pickList.data.get(0);
        this.pickList.selection.selectSingle(firstRecord);
        this.pickList.clearLastHilite();
        this.pickList.scrollRecordIntoView(0);
      }
    }
  },

  // changed handles the case that the user removes the value using the keyboard
  // this should do the same things as setting the value through the pickvalue
  changed: function(form, item, newValue) {
    var identifier;
    // only do the identifier actions when clearing
    // in all other cases pickValue is called
    if (!newValue) {
      if (
        this.required &&
        this.getElementValue() === '' &&
        this.pickList &&
        this.pickList.getSelectedRecord() &&
        this.pickList.getSelectedRecord().id
      ) {
        // handle special case: after selecting a value, a redraw is fired in the form.
        // due to asynchrony problems, the redraw flow was able to access to _value before setting it with the current value.
        // if we are in this case, then we do not need to continue setting the value because 'null' is not the value to be assigned.
        return;
      }
      this.setValueFromRecord(null);
    }
    if (OB.Utilities.isUUID(newValue)) {
      identifier = this.mapValueToDisplay(newValue);
    } else {
      identifier = newValue;
    }

    // check if the whole item identifier has been entered
    // see issue https://issues.openbravo.com/view.php?id=22821
    if (
      OB.Utilities.isUUID(this.mapDisplayToValue(identifier)) &&
      this._notUpdatingManually !== true &&
      !this.valuePicked
    ) {
      this.fullIdentifierEntered = true;
    } else {
      delete this.fullIdentifierEntered;
    }

    //Setting the element value again to align the cursor position correctly.
    //Before setting the value check if the identifier is part of the value map or the full identifier is entered.
    //If it fails set newValue as value.
    if (
      (this.valueMap &&
        this.valueMap[newValue] === identifier &&
        identifier.trim() !== '') ||
      this.fullIdentifierEntered
    ) {
      this.setElementValue(identifier);
    } else {
      this.setElementValue(newValue);
    }
  },

  setPickListWidth: function() {
    var extraWidth = 0,
      leftFieldsWidth = 0,
      nameField = this.name,
      fieldWidth = this.getVisibleWidth();
    // minimum width for smaller fields.
    fieldWidth = fieldWidth < 150 ? 150 : fieldWidth;
    // Dropdown selector that shows more than one column.
    if (this.pickListFields.length > 1) {
      if (this.form.view && !this.form.view.isShowingForm && this.grid) {
        // Calculate the extra space available right of the field to add it as extra space to the pick list width
        leftFieldsWidth = this.getVisibleLeftFieldWidth(nameField);
        extraWidth = Math.min(
          150 * (this.pickListFields.length - 1),
          Math.max(
            this.getAvailableRightFieldWidth(fieldWidth, leftFieldsWidth),
            0
          )
        );
      }
    }
    this.pickListWidth = fieldWidth + extraWidth;
  },

  getAvailableRightFieldWidth: function(fieldWidth, leftFieldsWidth) {
    return (
      this.grid.width - fieldWidth - this.grid.scrollbarSize - leftFieldsWidth
    );
  },

  getVisibleLeftFieldWidth: function(fieldName) {
    var i = 0,
      leftFieldsWidth = 0;

    // Calculate the width of all fields located left of the field which will display the pick list
    while (
      i < this.grid.fields.size() &&
      fieldName.localeCompare(this.grid.fields.get(i).valueField) !== 0
    ) {
      leftFieldsWidth = leftFieldsWidth + this.grid.fields.get(i).width;
      i++;
    }

    return this.removeSpaceHiddenByScroll(leftFieldsWidth);
  },

  removeSpaceHiddenByScroll: function(space) {
    var visibleSpace = 0;
    if (this.grid.body && isc.isA.Number(this.grid.body.scrollLeft)) {
      visibleSpace = space - this.grid.body.scrollLeft;
      return visibleSpace >= 0 ? visibleSpace : space;
    }
    return space;
  },

  enableShortcuts: function() {
    var ksAction_ShowPopup;

    ksAction_ShowPopup = function(caller) {
      caller.openSelectorWindow();
      return false; //To avoid keyboard shortcut propagation
    };
    OB.KeyboardManager.Shortcuts.set(
      'Selector_ShowPopup',
      ['OBSelectorItem', 'OBSelectorItem.icon', 'OBSelectorItem.add'],
      ksAction_ShowPopup
    );
  },

  init: function() {
    this.enableShortcuts();
    this.icons = [
      {
        selector: this,
        src: this.popupIconSrc,
        width: this.popupIconWidth,
        height: this.popupIconHeight,
        hspace: this.popupIconHspace,
        keyPress: function(keyName, character, form, item, icon) {
          var response = OB.KeyboardManager.Shortcuts.monitor(
            'OBSelectorItem.icon',
            this.selector
          );
          if (response !== false) {
            response = this.Super('keyPress', arguments);
          }
          return response;
        },
        click: function(form, item, icon) {
          item.openSelectorWindow();
        }
      },
      {
        selector: this,
        src: this.addIconSrc,
        width: this.addIconWidth,
        height: this.addIconHeight,
        hspace: this.addIconHspace,
        showIf: function() {
          if (this.selector.processId) {
            return true;
          } else {
            return false;
          }
        },
        keyPress: function(keyName, character, form, item, icon) {
          var response = OB.KeyboardManager.Shortcuts.monitor(
            'OBSelectorItem.add',
            this.selector
          );
          if (response !== false) {
            response = this.Super('keyPress', arguments);
          }
          return response;
        },
        click: function(form, item, icon) {
          var enteredValue = {};
          enteredValue[item.defaultPopupFilterField] = item.getEnteredValue();
          item.openProcess([enteredValue], {
            processOwnerView: form.view
          });
        }
      }
    ];

    if (this.disabled) {
      // TODO: disable, remove icons
      this.icons = null;
    }
    if (!this.showSelectorGrid) {
      this.icons = null;
    }

    if (this.showSelectorGrid && !this.form.isPreviewForm) {
      this.selectorWindow = isc.OBSelectorPopupWindow.create({
        // solves issue: https://issues.openbravo.com/view.php?id=17268
        title:
          this.form && this.form.grid
            ? this.form.grid.getField(this.name).title
            : this.title,
        dataSource: this.optionDataSource,
        selector: this,
        valueField: this.valueField,
        displayField: this.displayField,
        selectorGridFields: isc.shallowClone(this.selectorGridFields)
      });
    }

    this.optionCriteria = {
      _selectorDefinitionId: this.selectorDefinitionId
    };

    return this.Super('init', arguments);
  },

  setValueFromRecord: function(record, fromPopup, addToValueMap) {
    var currentValue = this.getValue(),
      identifierFieldName =
        this.name + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER,
      valueMapObj = {},
      valueToDisplay;
    this._notUpdatingManually = true;
    if (!record) {
      this.storeValue(null);
      this.form.setValue(
        this.name + OB.Constants.FIELDSEPARATOR + this.displayField,
        null
      );
      this.form.setValue(identifierFieldName, null);

      // make sure that the grid does not display the old identifier
      if (this.form.grid && this.form.grid.getEditForm()) {
        this.form.grid.setEditValue(
          this.form.grid.getEditRow(),
          this.name,
          null
        );
        this.form.grid.setEditValue(
          this.form.grid.getEditRow(),
          identifierFieldName,
          ''
        );
        this.form.grid.setEditValue(
          this.form.grid.getEditRow(),
          this.name + OB.Constants.FIELDSEPARATOR + this.displayField,
          ''
        );
      }
    } else {
      if (
        OB.Utilities.getObjectSize(record) === 2 && //
        Object.prototype.toString.call(record.value) === '[object String]' && //
        Object.prototype.toString.call(record.map) === '[object String]'
      ) {
        // This function admits the record provided as a full record (as it has come from the database with all the columns with their proper names)
        // or as a simplified record where only the value of the input and its mapping (value to display) are known (but not the DB names of the columns that need to be stored and shown)
        // In case of have a simplified record, it should come as {value: 'theValueToBeStored', map: 'theDisplayValueToBeShown'}
        // Here it happens the adaptation of the 'record' object from the 'value' and 'map' nomenclature to the proper one based on DB names
        if (this.valueField !== 'value') {
          record[this.valueField] = record.value;
          delete record.value;
        }
        if (this.displayField !== 'map') {
          record[this.displayField] = record.map;
          delete record.map;
        }
      }
      this.handleOutFields(record);
      if (addToValueMap) {
        valueMapObj[record[this.valueField]] = record[this.displayField];
        this.setValueMap(valueMapObj);
      }
      this.storeValue(record[this.valueField]);
      this.form.setValue(
        this.name + OB.Constants.FIELDSEPARATOR + this.displayField,
        record[this.displayField]
      );
      this.form.setValue(identifierFieldName, record[OB.Constants.IDENTIFIER]);
      // make sure the identifier is not null in the grid. See issue https://issues.openbravo.com/view.php?id=25727
      if (
        this.form.grid &&
        this.form.grid.getEditValues(0) &&
        !this.form.grid.getEditValues(0)[
          this.name + OB.Constants.FIELDSEPARATOR + this.displayField
        ]
      ) {
        this.form.grid.setEditValue(
          this.form.grid.getEditRow(),
          this.name + OB.Constants.FIELDSEPARATOR + this.displayField,
          record[OB.Constants.IDENTIFIER]
        );
      }
      if (!this.valueMap) {
        this.valueMap = {};
      }

      if (record[this.valueField]) {
        // it can be undefined in case of empty (null) entry
        valueToDisplay = record[this.displayField];
        if (valueToDisplay) {
          valueToDisplay = valueToDisplay.replace(/[\n\r]/g, '');
        }
        this.valueMap[record[this.valueField]] = valueToDisplay;
      }

      this.updateValueMap();
    }

    if (this.form && this.form.handleItemChange) {
      this._hasChanged = true;
      this.form.handleItemChange(this);
    }

    // only jump to the next field if the value has really been set
    // do not jump to the next field if the event has been triggered by the Tab key,
    // to prevent a field from being skipped (see https://issues.openbravo.com/view.php?id=21419)
    if (
      (currentValue || fromPopup) &&
      this.form.focusInNextItem &&
      isc.EH.getKeyName() !== 'Tab'
    ) {
      this.form.focusInNextItem(this.name);
    }
    delete this._notUpdatingManually;
  },

  blur: function(form, item) {
    var selectedRecord;
    // Handles the case where the user has entered the whole item identifier and has moved out of the
    // selector field by clicking on another field, instead of pressing the tab key. in that case the change
    // was not being detected and if the selector had some callouts associated they were not being executed
    // See issue https://issues.openbravo.com/view.php?id=22821
    if (this.fullIdentifierEntered) {
      selectedRecord = this.pickList.getSelectedRecord();
      this.setValueFromRecord(selectedRecord);
      delete this.fullIdentifierEntered;
    }
  },

  handleOutFields: function(record) {
    var i,
      j,
      outFields = this.outFields,
      form = this.form,
      grid = this.grid,
      item,
      value,
      fields;

    if (
      (!form || (form && !form.fields)) &&
      (!grid || (grid && !grid.fields))
    ) {
      // not handling out fields
      return;
    }

    fields = form.fields || grid.fields;
    form.hiddenInputs = form.hiddenInputs || {};
    for (i in outFields) {
      if (Object.prototype.hasOwnProperty.call(outFields, i)) {
        if (outFields[i].suffix) {
          // when it has a suffix
          if (record) {
            value = record[i];
            if (typeof value === 'undefined') {
              form.hiddenInputs[
                this.outHiddenInputPrefix + outFields[i].suffix
              ] = '';
              continue;
            }
            if (isc.isA.Number(value)) {
              if (outFields[i].formatType && outFields[i].formatType !== '') {
                value = OB.Utilities.Number.JSToOBMasked(
                  value,
                  OB.Format.formats[outFields[i].formatType],
                  OB.Format.defaultDecimalSymbol,
                  OB.Format.defaultGroupingSymbol,
                  OB.Format.defaultGroupingSize
                );
              } else {
                value = value
                  .toString()
                  .replace('.', OB.Format.defaultDecimalSymbol);
              }
            }
            form.hiddenInputs[
              this.outHiddenInputPrefix + outFields[i].suffix
            ] = value;
            item = form.getItem(outFields[i].fieldName);
            if (item && item.valueMap) {
              item.valueMap[value] =
                record[
                  outFields[i].fieldName +
                    OB.Constants.FIELDSEPARATOR +
                    OB.Constants.IDENTIFIER
                ];
            }
          } else {
            form.hiddenInputs[
              this.outHiddenInputPrefix + outFields[i].suffix
            ] = null;
          }
        } else {
          // it does not have a suffix
          for (j = 0; j < fields.length; j++) {
            if (
              fields[j].name !== '' &&
              fields[j].name === outFields[i].fieldName
            ) {
              if (record) {
                value = record[i];
                if (typeof value === 'undefined') {
                  continue;
                }
              } else {
                value = null;
              }
              // fields[j].setValue will be used when the selector is used in form view, and grid.setEditValue when it is used in grid view
              if (fields[j].setValue) {
                fields[j].setValue(value);
              } else {
                grid.setEditValue(grid.getEditRow(), j, value);
              }
            }
          }
        }
      }
    }
  },

  openSelectorWindow: function() {
    var selectorGrid = this.selectorWindow.selectorGrid;
    // always refresh the content of the grid to force a reload
    // if the organization has changed
    if (selectorGrid && selectorGrid.data) {
      delete selectorGrid.data;
      // Ensure that the scroll is on the top after reopening the selector pop-up
      selectorGrid.scrollToRow(0);
      if (selectorGrid.body) {
        selectorGrid.body.markForRedraw();
      }
      if (selectorGrid.getSelectedRecord()) {
        // Clean selection information, it will be recalculated on dataArrived
        selectorGrid.deselectAllRecords();
      }
    }
    this.selectorWindow.open();
  },

  openProcess: function(enteredValues, additionalProcessProperties) {
    var params, view, standardWindow;
    if (this.form && this.form.view) {
      // If the selector is in a standard window
      view = this.form.view;
    } else if (
      this.form &&
      this.form.paramWindow &&
      this.form.paramWindow.parentWindow &&
      this.form.paramWindow.parentWindow.view
    ) {
      // If the selector is in a parameter window
      view = this.form.paramWindow.parentWindow.view;
    }
    params = {
      callerField: this,
      enteredValues: enteredValues,
      paramWindow: true,
      processId: this.processId,
      windowId: view.windowId,
      windowTitle: OB.I18N.getLabel('OBUISEL_AddNewRecord', [this.title])
    };
    // Avoid that windowId is null. WindowId in params is necessary
    // to check access process of OBUISEL_Selector Reference.
    if (!params.windowId) {
      params.windowId =
        view && view.standardWindow && view.standardWindow.windowId;
    }
    if (additionalProcessProperties) {
      isc.addProperties(params, additionalProcessProperties);
    }
    standardWindow = view.standardWindow;
    standardWindow.openProcess(params);
  },

  keyPress: function(item, form, keyName, characterValue) {
    var response = OB.KeyboardManager.Shortcuts.monitor('OBSelectorItem', this);
    if (response !== false) {
      response = this.Super('keyPress', arguments);
    }
    return response;
  },

  pickValue: function(value) {
    var selectedRecord, ret;
    // get the selected record before calling the super, as this super call
    // will deselect the record
    selectedRecord = this.pickList.getSelectedRecord();
    this.valuePicked = true;
    ret = this.Super('pickValue', arguments);
    delete this.valuePicked;
    this.setValueFromRecord(selectedRecord);
    delete this.fullIdentifierEntered;
    return ret;
  },

  filterDataBoundPickList: function(requestProperties, dropCache) {
    requestProperties = requestProperties || {};
    requestProperties.params = requestProperties.params || {};

    isc.OBSelectorItem.prepareDSRequest(
      requestProperties.params,
      this,
      'PickList'
    );

    // sometimes the value is passed as a filter criteria remove it
    if (
      this.getValueFieldName() &&
      requestProperties.params[this.getValueFieldName()]
    ) {
      requestProperties.params[this.getValueFieldName()] = null;
    }

    // do not prevent the count operation
    requestProperties.params[isc.OBViewGrid.NO_COUNT_PARAMETER] = 'true';

    if (
      this.form.getFocusItem() !== this &&
      !(this.form.view && this.form.view.isShowingForm) &&
      this.getEnteredValue() === '' &&
      this.savedEnteredValue
    ) {
      this.setElementValue(this.savedEnteredValue);
      delete this.savedEnteredValue;
    } else if (
      this.form &&
      this.form.view &&
      this.form.view.isShowingForm &&
      this.savedEnteredValue
    ) {
      if (this.getEnteredValue() !== '') {
        this.setElementValue(this.savedEnteredValue + this.getEnteredValue());
      } else {
        this.setElementValue(this.savedEnteredValue);
      }
      delete this.savedEnteredValue;
    }

    var criteria = this.getPickListFilterCriteria(),
      i;
    for (i = 0; i < criteria.criteria.length; i++) {
      if (criteria.criteria[i].fieldName === this.displayField) {
        // for the suggestion box it is one big or
        requestProperties.params[OB.Constants.OR_EXPRESSION] = 'true';
      }
    }

    return this.Super('filterDataBoundPickList', [
      requestProperties,
      dropCache
    ]);
  },

  getPickListFilterCriteria: function() {
    var crit = this.Super('getPickListFilterCriteria', arguments),
      operator;
    var criteria = {
      operator: 'or',
      _constructor: 'AdvancedCriteria',
      criteria: []
    };

    if (this.addDummyCriterion) {
      // add a dummy criteria to force a fetch
      criteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
    }

    // only filter if the display field is also passed
    // the displayField filter is not passed when the user clicks the drop-down button
    // display field is passed on the criteria.
    var displayFieldValue = null,
      i;
    if (crit.criteria) {
      for (i = 0; i < crit.criteria.length; i++) {
        if (crit.criteria[i].fieldName === this.displayField) {
          displayFieldValue = crit.criteria[i].value;
        }
      }
    } else if (crit[this.displayField]) {
      displayFieldValue = crit[this.displayField];
    }
    if (displayFieldValue !== null) {
      if (this.textMatchStyle === 'substring') {
        operator = 'iContains';
      } else {
        operator = 'iStartsWith';
      }
      for (i = 0; i < this.extraSearchFields.length; i++) {
        criteria.criteria.push({
          fieldName: this.extraSearchFields[i],
          operator: operator,
          value: displayFieldValue
        });
      }
      criteria.criteria.push({
        fieldName: this.displayField,
        operator: operator,
        value: displayFieldValue
      });
    }
    return criteria;
  },

  getSelectedPropertiesString: function() {
    var i,
      fieldName,
      selectedProperties = {};
    selectedProperties.entityProperties = OB.Constants.ID;
    selectedProperties.derivedProperties = '';
    for (i = 0; i < this.pickListFields.length; i++) {
      fieldName = this.pickListFields[i].name;
      if (
        fieldName === OB.Constants.ID ||
        fieldName === OB.Constants.IDENTIFIER
      ) {
        continue;
      }
      if (fieldName.contains(OB.Constants.FIELDSEPARATOR)) {
        if (selectedProperties.derivedProperties === '') {
          selectedProperties.derivedProperties = fieldName;
        } else {
          selectedProperties.derivedProperties += ',' + fieldName;
        }
      } else {
        selectedProperties.entityProperties += ',' + fieldName;
      }
    }
    return selectedProperties;
  },

  getExtraPropertiesString: function() {
    var i,
      outFieldName,
      outFieldsNames,
      extraProperties = this.valueField || '';
    if (this.displayField && this.displayField !== OB.Constants.IDENTIFIER) {
      extraProperties += ',' + this.displayField;
    }
    if (this.outFields) {
      outFieldsNames = isc.getKeys(this.outFields);
      for (i = 0; i < outFieldsNames.length; i++) {
        outFieldName = outFieldsNames[i];
        if (
          outFieldName === this.valueField ||
          outFieldName === this.displayField
        ) {
          continue;
        }
        extraProperties += ',' + outFieldName;
      }
    }
    return extraProperties;
  },

  mapValueToDisplay: function(value) {
    var ret = this.Super('mapValueToDisplay', arguments);
    if (ret === value && this.isDisabled()) {
      if (!this.valueMap || (this.valueMap && !this.valueMap[value])) {
        return '';
      }
    }
    // if value is null then don't set it in the valueMap, this results
    // in null being displayed in the combobox
    if (ret === value && value) {
      if (!this.valueMap) {
        this.valueMap = {};
        this.valueMap[value] = '';
        return '';
      } else if (!this.valueMap[value] && OB.Utilities.isUUID(value)) {
        return '';
      }
    }
    if (value && value !== '' && ret === '' && !OB.Utilities.isUUID(value)) {
      this.savedEnteredValue = value;
    }
    return ret;
  },

  mapDisplayToValue: function(value) {
    if (value === '' || value === null) {
      return null;
    }
    return this.Super('mapDisplayToValue', arguments);
  },

  destroy: function() {
    // Explicitly destroy the selector window to avoid memory leaks
    if (this.selectorWindow) {
      this.selectorWindow.destroy();
      this.selectorWindow = null;
    }

    // Sometimes, internal _columnSizer member of pickList is leaked
    if (
      this.pickList &&
      this.pickList.members &&
      this.pickList.members.length > 0 &&
      this.pickList.members[0]._columnSizer
    ) {
      this.pickList.members[0]._columnSizer.destroy();
    }

    this.Super('destroy', arguments);
  }
});

isc.OBSelectorItem.addClassMethods({
  // Prepares requestProperties adding contextInfo, this is later used in backed
  // to prepare filters
  prepareDSRequest: function(params, selector, requestType) {
    var selectedProperties, extraProperties;

    function setOrgIdParam(params) {
      if (!params.inpadOrgId) {
        // look for an ad_org_id parameter. If there is no such parameter or its value is empty, selector will be filter by natural tree of writable organizations
        params.inpadOrgId = params.ad_org_id || params.AD_Org_ID;
      }
    }

    function multipleRecordsSelected() {
      // Check if multiple records are selected in the parent grid
      if (
        selector &&
        selector.view &&
        selector.view.buttonOwnerView &&
        selector.view.buttonOwnerView.lastRecordSelectedCount > 1
      ) {
        return true;
      }
      return false;
    }

    // on purpose not passing the third boolean param
    if (
      selector.form &&
      selector.form.view &&
      selector.form.view.getContextInfo
    ) {
      // for table and table dir reference values needs to be transformed to classic (ex.: true -> Y)
      isc.addProperties(
        params,
        selector.form.view.getContextInfo(
          false,
          true,
          null,
          selector.isComboReference
        )
      );
    } else if (selector.view && selector.view.getUnderLyingRecordContext) {
      isc.addProperties(
        params,
        selector.view.getUnderLyingRecordContext(
          false,
          true,
          null,
          selector.isComboReference
        )
      );
      if (
        selector.form &&
        selector.form.paramWindow &&
        selector.form.paramWindow.getContextInfo
      ) {
        isc.addProperties(params, selector.form.paramWindow.getContextInfo());
      }
      setOrgIdParam(params);
    } else if (
      selector.view &&
      selector.view.sourceView &&
      selector.view.sourceView.getContextInfo
    ) {
      isc.addProperties(
        params,
        selector.view.sourceView.getContextInfo(
          false,
          true,
          null,
          selector.isComboReference
        )
      );
    } else if (
      selector.grid &&
      selector.grid.contentView &&
      selector.grid.contentView.getContextInfo
    ) {
      isc.addProperties(
        params,
        selector.grid.contentView.getContextInfo(
          false,
          true,
          null,
          selector.isComboReference
        )
      );
    } else if (
      selector.form &&
      selector.form.paramWindow &&
      selector.form.paramWindow.getContextInfo
    ) {
      isc.addProperties(params, selector.form.paramWindow.getContextInfo());
      setOrgIdParam(params);
    }

    // if the selector belongs to a P&E grid, include the info of the record being edited
    if (
      selector.grid &&
      selector.grid.getClassName() === 'OBPickAndExecuteGrid'
    ) {
      isc.addProperties(
        params,
        selector.grid.getContextInfo(selector.grid.getEditRow())
      );
    }

    if (
      selector.form &&
      selector.form.view &&
      selector.form.view.standardWindow
    ) {
      isc.addProperties(params, {
        windowId: selector.form.view.standardWindow.windowId,
        tabId: selector.form.view.tabId,
        moduleId: selector.form.view.moduleId
      });
    }

    // Include the windowId in the params if possible
    if (
      selector.form &&
      selector.form.view &&
      selector.form.view.standardProperties &&
      selector.form.view.standardProperties.inpwindowId
    ) {
      params.windowId = selector.form.view.standardProperties.inpwindowId;
    }

    // also add the special ORG parameter
    if (params.inpadOrgId && !multipleRecordsSelected()) {
      params[OB.Constants.ORG_PARAMETER] = params.inpadOrgId;
    } else {
      params[OB.Constants.CALCULATE_ORGS] = true;
    }

    // adds the selector id to filter used to get filter information
    params._selectorDefinitionId = selector.selectorDefinitionId;

    if (selector.isComboReference) {
      if (selector.getValue && selector.getValue()) {
        // sending current value only if set (not null) to be able
        // to include it even validation is not matched
        params._currentValue = selector.getValue();
      }
    } else {
      // add field's default filter expressions
      params.filterClass =
        'org.openbravo.userinterface.selector.SelectorDataSourceFilter';
    }

    // and sort according to the display field
    // initially
    params[OB.Constants.SORTBY_PARAMETER] = selector.displayField;

    if (requestType === 'PickList') {
      selectedProperties = selector.getSelectedPropertiesString();
      extraProperties = selector.getExtraPropertiesString();
      // Add the fields to be displayed in the picklist as selected properties
      params[OB.Constants.SELECTED_PROPERTIES] =
        selectedProperties.entityProperties;
      // Include value field, display field and out fields into additional properties
      // Also include the fields whose value will be obtained through property navigation
      params[OB.Constants.EXTRA_PROPERTIES] =
        extraProperties + ',' + selectedProperties.derivedProperties;
    }

    // Parameter windows
    if (selector.form.paramWindow) {
      params._processDefinitionId = selector.form.paramWindow.processId;
      params._selectorFieldId = selector.paramId;
      isc.addProperties(params, selector.form.paramWindow.getContextInfo());
    }
  }
});

isc.ClassFactory.defineClass('OBSelectorLinkItem', isc.StaticTextItem);

isc.ClassFactory.mixInInterface('OBSelectorLinkItem', 'OBLinkTitleItem');

isc.OBSelectorLinkItem.addProperties({
  canFocus: true,
  showFocused: true,
  wrap: false,
  clipValue: true,

  // show the complete displayed value, handy when the display value got clipped
  itemHoverHTML: function(item, form) {
    return this.getDisplayValue(this.getValue());
  },

  setValue: function(value) {
    var ret = this.Super('setValue', arguments);
    // in this case the clearIcon needs to be shown or hidden
    if (!this.disabled && !this.required) {
      if (value) {
        this.showIcon(this.instanceClearIcon);
      } else {
        this.hideIcon(this.instanceClearIcon);
      }
    }
    return ret;
  },

  click: function() {
    this.showPicker();
    return false;
  },

  keyPress: function(item, form, keyName, characterValue) {
    var response = OB.KeyboardManager.Shortcuts.monitor(
      'OBSelectorLinkItem',
      this
    );
    if (response !== false) {
      response = this.Super('keyPress', arguments);
    }
    return response;
  },

  showPicker: function() {
    if (this.isFocusable()) {
      this.focusInItem();
    }
    this.selectorWindow.open();
  },

  setValueFromRecord: function(record) {
    // note this.displayfield already contains the prefix of the property name
    if (!record) {
      this.setValue(null);
      this.form.setValue(this.displayField, null);
    } else {
      this.setValue(record[this.gridValueField]);
      this.form.setValue(this.displayField, record[this.gridDisplayField]);
      if (!this.valueMap) {
        this.valueMap = {};
      }
      this.valueMap[record[this.gridValueField]] = record[
        this.gridDisplayField
      ].replace(/[\n\r]/g, '');
      this.updateValueMap();
    }
    this.handleOutFields(record);
    if (this.form && this.form.handleItemChange) {
      this._hasChanged = true;
      this.form.handleItemChange(this);
    }
  },

  handleOutFields: function(record) {
    var i,
      j,
      outFields = this.outFields,
      form = this.form,
      grid = this.grid,
      value,
      fields = form.fields || grid.fields;
    form.hiddenInputs = form.hiddenInputs || {};
    for (i in outFields) {
      if (Object.prototype.hasOwnProperty.call(outFields, i)) {
        if (outFields[i].suffix) {
          if (record) {
            value = record[i];
            if (isc.isA.Number(value)) {
              value = OB.Utilities.Number.JSToOBMasked(
                value,
                OB.Format.defaultNumericMask,
                OB.Format.defaultDecimalSymbol,
                OB.Format.defaultGroupingSymbol,
                OB.Format.defaultGroupingSize
              );
            }
            form.hiddenInputs[
              this.outHiddenInputPrefix + outFields[i].suffix
            ] = value;
          } else {
            form.hiddenInputs[
              this.outHiddenInputPrefix + outFields[i].suffix
            ] = null;
          }
        } else {
          // it does not have a suffix
          for (j = 0; j < fields.length; j++) {
            if (
              fields[j].name !== '' &&
              fields[j].name === outFields[i].fieldName
            ) {
              value = record ? record[i] : null;
              fields[j].setValue(value);
            }
          }
        }
      }
    }
  },

  enableShortcuts: function() {
    var ksAction_ShowPopup;

    ksAction_ShowPopup = function(caller) {
      caller.showPicker();
      return false; //To avoid keyboard shortcut propagation
    };
    OB.KeyboardManager.Shortcuts.set(
      'SelectorLink_ShowPopup',
      'OBSelectorLinkItem',
      ksAction_ShowPopup
    );
  },

  init: function() {
    this.enableShortcuts();
    if (this.disabled) {
      this.showPickerIcon = false;
    }

    this.instanceClearIcon = isc.shallowClone(this.clearIcon);
    this.instanceClearIcon.showIf = function(form, item) {
      if (item.disabled) {
        return false;
      }
      if (item.required) {
        return false;
      }
      if (form && form.view && form.view.readOnly) {
        return false;
      }
      if (item.getValue()) {
        return true;
      }
      return false;
    };

    this.instanceClearIcon.click = function() {
      this.formItem.setValue(null);
      this.formItem.form.itemChangeActions();
    };

    this.icons = [this.instanceClearIcon];
    this.icons[0].formItem = this;

    if (this.disabled) {
      // TODO: disable, remove icons
      this.icons = null;
    }

    if (!this.form.isPreviewForm) {
      this.selectorWindow = isc.OBSelectorPopupWindow.create({
        // solves issue: https://issues.openbravo.com/view.php?id=17268
        title:
          this.form && this.form.grid
            ? this.form.grid.getField(this.name).title
            : this.title,
        dataSource: this.dataSource,
        selector: this,
        valueField: this.gridValueField,
        displayField: this.gridDisplayField,
        selectorGridFields: isc.shallowClone(this.selectorGridFields)
      });
    }

    return this.Super('init', arguments);
  },

  changed: function() {
    var ret = this.Super('changed', arguments);
    this._hasChanged = true;
    this._doFICCall = true;
    if (this.form && this.form.handleItemChange) {
      this.form.handleItemChange(this);
    }
    return ret;
  },

  destroy: function() {
    // Explicitly destroy the selector window to avoid memory leaks
    if (this.selectorWindow) {
      this.selectorWindow.destroy();
      this.selectorWindow = null;
    }
    this.Super('destroy', arguments);
  }
});
