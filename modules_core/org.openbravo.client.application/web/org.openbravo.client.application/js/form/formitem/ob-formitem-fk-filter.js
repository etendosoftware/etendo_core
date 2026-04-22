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
 * All portions are Copyright (C) 2011-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

//== OBFKFilterTextItem ==
//Input used for filtering on FK fields.
isc.ClassFactory.defineClass('OBFKFilterTextItem', isc.OBListFilterItem);

isc.OBFKFilterTextItem.addProperties({
  operator: 'iContains',
  overrideTextMatchStyle: 'substring',
  allowExpressions: false,
  showOptionsFromDataSource: true,
  selectOnFocus: false,
  validateOnExit: true,

  multiple: true,
  multipleAppearance: 'picklist',
  multipleValueSeparator: ' or ',

  // only show the drop down on demand
  // this because we want to support partial values
  // for filtering, also getting trouble because values get
  // completely selected
  showPickListOnKeypress: true,
  filterOnKeypress: false,
  changeOnKeypress: true,
  addUnknownValues: true,
  defaultToFirstOption: false,
  // filterType = {'id', 'identifier'}
  // filterType = 'id' means that the foreign key will be filtered using the record ids. This is only possible when filtering the grid by selecting a record from the filter drop down
  // filterType = 'identifier' means that the foreign key will be filtered using the record ids
  filterType: 'identifier',
  filterAuxCache: [],

  emptyPickListMessage: OB.I18N.getLabel('OBUISC_ListGrid.emptyMessage'),

  init: function() {
    var me = this,
      grid = this.form.grid.sourceWidget,
      gridField = grid.getField(this.name),
      dataSource;

    // the textMatchStyle is sometimes overridden from the underlying
    // grid, this happens when used within the selector editor.
    // for foreign key fields we only support like/contains/substring
    // so force that
    this.textMatchStyle = this.overrideTextMatchStyle;

    // the data from the datasource will contain the id and the identifier
    // the value for the filter and the display are the same: the identifier
    this.displayField = this.criteriaDisplayField || OB.Constants.IDENTIFIER;
    this.valueField = this.criteriaDisplayField || OB.Constants.IDENTIFIER;
    this.keyProperty = this.keyProperty || OB.Constants.ID;
    // if this field was being filtered by its id before being recreated, reset its filter type an its filterAuxCache
    if (
      this.grid &&
      this.grid.sourceWidget &&
      this.grid.sourceWidget.filterByIdFields &&
      this.grid.sourceWidget.filterByIdFields.contains(this.name)
    ) {
      this.filterType = 'id';
      if (
        this.grid.sourceWidget.fkCacheCopy &&
        this.grid.sourceWidget.fkCacheCopy.find('fieldName', this.name)
      ) {
        this.filterAuxCache = this.grid.sourceWidget.fkCacheCopy.find(
          'fieldName',
          this.name
        ).cache;
      }
    }

    if (this.disableFkDropdown) {
      this.showPickerIcon = false;
      this.showPickListOnKeypress = false;
      if (this.filterOnChange) {
        this.actOnKeypress = true;
      }
    } else {
      this.pickListProperties = {
        // 'showOverAsSelected' and 'bodyKeyPress' defined here until issue 28475 be fixed.
        // After the fix the following two lines must be removed, since it will be inherited from
        // OBListFilterItem  as usual
        showOverAsSelected: this.pickListProperties.showOverAsSelected,
        bodyKeyPress: this.pickListProperties.bodyKeyPress,

        // make sure that we send the same parameters as the grid
        onFetchData: function(criteria, requestProperties) {
          var gridView = grid.view;
          requestProperties = requestProperties || {};
          requestProperties.params =
            grid.getFetchRequestParams(requestProperties.params) || {};
          if (gridView) {
            requestProperties.params.tabId =
              gridView.tabId ||
              (grid.viewProperties && grid.viewProperties.tabId) ||
              (gridView.sourceView && gridView.sourceView.tabId);
            if (gridView.buttonOwnerView && gridView.buttonOwnerView.tabId) {
              requestProperties.params.buttonOwnerViewTabId =
                gridView.buttonOwnerView.tabId;
            }
          }
          // send the display field in request params to add it to the list of fields to be fetched in DefaultJsonDataService.
          // used for displaying table references properly. Refer issue https://issues.openbravo.com/view.php?id=26696
          if (this.formItem && this.formItem.displayProperty) {
            requestProperties.params.displayProperty = this.formItem.displayProperty;
          }
          delete me.forceReload;
        },

        // drawAllMaxCells is set to 0 to prevent extra reads of data
        // Smartclient will try to read until drawAllMaxCells has been reached
        drawAllMaxCells: 0,

        fetchDelay: 400,
        // prevent aggressive local filtering by smartclient
        filterLocally: false,
        multipleValueSeparator: ' or ',
        dataProperties: {
          useClientFiltering: false
        },

        isSelected: function(record) {
          var i,
            values = this.formItem.getValue();
          if (values.length) {
            for (i = 0; i < values.length; i++) {
              if (record[me.displayField] === values[i]) {
                return true;
              }
            }
          }
          return record[me.displayField] === values;
        },

        // override data arrived to prevent the first entry from being
        // selected
        // this to handle the picklist in foreign key filter item. When a user
        // types a partial value maybe he/she wants to filter by this partial
        // value
        // auto-selecting the first value makes this impossible.
        // Therefore this option to prevent this.
        // There are maybe nicer points to do this overriding but this was the
        // place after the first item was selected.
        // This first selection happens in ScrollingMenu.dataChanged
        dataArrived: function(startRow, endRow) {
          var record,
            rowNum,
            i,
            values = this.formItem.getValue(),
            fixedValues = [],
            value;
          this.Super('dataArrived', arguments);
          if (values) {
            if (!isc.isA.Array(values)) {
              values = [values];
            }

            // fix selected values before checking them in the data to re-select them
            for (i = 0; i < values.length; i++) {
              value = values[i];
              if (isc.isAn.Array(value)) {
                value = value[0];
              }
              fixedValues.push(
                value.startsWith('==') ? value.substring(2) : value
              );
            }

            for (rowNum = startRow; rowNum < endRow + 1; rowNum++) {
              record = this.getRecord(rowNum);
              if (record && fixedValues.contains(record[me.displayField])) {
                // selectRecord asynchronously invokes handleChanged, this should be
                // managed as when the value is picked from the list by pickValue
                this.formItem._pickingArrivedValue = true;
                this.selectRecord(record, true);
              }
            }
          }
        },
        _original_recordClick: isc.PickListMenu.getPrototype().recordClick,
        recordClick: function(
          viewer,
          record,
          recordNum,
          field,
          fieldNum,
          value,
          rawValue
        ) {
          var filterEditor = this.formItem && this.formItem.grid,
            grid = filterEditor && filterEditor.parentElement;
          if (field && field.name === '_checkboxField') {
            // when clicking on the checkbox, execute default behaviour
            return this._original_recordClick(
              viewer,
              record,
              recordNum,
              field,
              fieldNum,
              value,
              rawValue
            );
          } else {
            // when clicking a row outside the checkbox, select that specific row and close the popup
            this.formItem.setValue('==' + record._identifier);
            if (grid) {
              if (grid.lazyFiltering) {
                grid.filterHasChanged = true;
                grid.sorter.enable();
              } else {
                filterEditor.performFilter(true, true);
              }
            }
            this.hide();
          }
        }
      };
    }
    dataSource = OB.Datasource.create({
      dataURL: grid.getDataSource().dataURL,
      requestProperties: {
        params: {
          // distinct forces the distinct query on the server side
          _distinct: gridField.valueField || gridField.name
        }
      },
      fields: this.pickListFields
    });
    if (grid.Class === 'OBTreeGrid') {
      dataSource.requestProperties.params.tabId = grid.view.tabId;
    }
    if (this.showFkDropdownUnfiltered) {
      dataSource.requestProperties.params._showFkDropdownUnfiltered = true;
    }
    this.setOptionDataSource(dataSource);

    this.Super('init', arguments);

    // don't validate for FK filtering, any value is allowed
    this.validators = [];

    // listen to data arrival in the grid
    // if data arrived we have to reload also
    this.observe(grid, 'dataArrived', 'observer.setForceReload()');

    this.multipleValueSeparator = ' or ';

    // if the filter by identifier has been disabled using grid configuration, set the filter type to 'id'
    if (
      this.allowFkFilterByIdentifier === false &&
      !grid.alwaysFilterFksByIdentifier
    ) {
      this.filterType = 'id';
    }
  },

  destroy: function() {
    var grid = this.form && this.form.grid && this.form.grid.sourceWidget;
    if (grid) {
      this.ignore(grid, 'dataArrived');
    }
    return this.Super('destroy', arguments);
  },

  // When the selected value is part of the pickList the grid is already filtered,
  // so no additional request is required. But when there is a keyword entered,
  // the grid has to be filtered. Refer issue, https://issues.openbravo.com/view.php?id=26700.
  handleEditorExit: function() {
    var value = this.getValue(),
      performFetch = false,
      rows,
      i;
    if (
      this.pickList &&
      this.pickList.data &&
      (this.pickList.data.allRows || this.pickList.data.localData)
    ) {
      rows = this.pickList.data.allRows || this.pickList.data.localData;
    }
    if (value && isc.isA.Array(value) && value.length > 0 && rows) {
      for (i = 0; i < value.length; i++) {
        if (
          value[i].indexOf('==') === 0 &&
          rows.find('name', value[i].substring(2, value[i].length)) ===
            undefined
        ) {
          performFetch = true;
          break;
        }
      }
    } else if (rows && rows.find('name', value)) {
      performFetch = true;
    }

    if (performFetch) {
      this.Super('handleEditorExit', arguments);
    } else {
      return value;
    }
  },

  // note: can't override changed as it is used by the filter editor
  // itself, see the RecordEditor source code and the changed event
  change: function(form, item, value, oldValue) {
    this._hasChanged = true;
    this.Super('change', arguments);
  },

  blur: function() {
    // Check if blur is caused by clicking on the grid
    // In that case, skip performAction to prevent duplicate fetch
    var isGridClick = false;
    try {
      var target = isc.EH.getTarget();
      var grid = this.grid && this.grid.sourceWidget;
      
      // Check if the event target is the grid or one of its components
      if (target && grid) {
        // Walk up the component hierarchy to see if we're clicking on the grid
        var current = target;
        while (current) {
          if (current === grid || current === grid.body || current === grid.header) {
            isGridClick = true;
            break;
          }
          current = current.parentElement;
        }
      }
    } catch (e) {
      // If we can't determine, allow the action (safer default)
    }
    
    if (this._hasChanged && this.allowFkFilterByIdentifier === false) {
      // close the picklist if the item is due to a user tab action
      if (isc.EH.getKeyName() === 'Tab') {
        this.pickList.hide();
      }
      if (!this.grid.sourceWidget.lazyFiltering) {
        // restore the filter editor with the previous criteria
        // if lazy filtering is enabled don't do this because the filter criteria may have not been applied into the grid yet
        this.setCriterion(this.getAppliedCriteria());
      }
      // do not perform a filter action on blur if the filtering by identifier is not allowed
    } else if (this._hasChanged && this.allowFkFilterByIdentifier !== false && !isGridClick) {
      this.form.grid.performAction();
    }
    delete this._hasChanged;
    this.Super('blur', arguments);
  },

  // returns the criteria for this field that is applied to the data that the grid is currently showing
  // this.getCriteria() would return the modified criteria that needs to be reverted
  getAppliedCriteria: function() {
    var currentGridCriteria = this.grid.sourceWidget.getCriteria(),
      i,
      emptyCriteria = {
        operator: 'and',
        _constructor: 'AdvancedCriteria',
        criteria: []
      };
    if (!currentGridCriteria || !currentGridCriteria.criteria) {
      return emptyCriteria;
    }
    for (i = 0; i < currentGridCriteria.criteria.length; i++) {
      if (currentGridCriteria.criteria[i].fieldName === this.name) {
        return currentGridCriteria.criteria[i];
      }
    }
    // if no criteria was found for this field, return an empty criteria
    return emptyCriteria;
  },

  // overridden otherwise the picklist fields from the grid field
  // are being used
  getPickListFields: function() {
    return [
      {
        name: this.displayField,
        escapeHTML: true
      }
    ];
  },

  mapValueToDisplay: function(value) {
    var i,
      result = '';
    if (isc.isAn.Array(value) && value.length === 1) {
      // '_nativeElementBlur' calls 'refreshDisplayValue' and this one calls to this 'mapValueToDisplay' passing as argument "this.getValue()".
      // EXCEPT in the 'or' case, in Smartclient 8.3d this value was a string containing the typed value but in Smartclient 9.1d this value
      // is an array, being the typed value in the first element, so a conversion is needed to preserve the old logic.
      value = value[0];
    }
    if (!isc.isAn.Array(value)) {
      return this.Super('mapValueToDisplay', arguments);
    }
    for (i = 0; i < value.length; i++) {
      if (i > 0) {
        result += this.multipleValueSeparator;
      }
      // encode 'or' and 'and'
      result += OB.Utilities.encodeSearchOperator(
        this.Super('mapValueToDisplay', value[i])
      );
    }
    return result;
  },

  // combine the value of the field with the overall grid
  // filter values
  getPickListFilterCriteria: function() {
    var forceFilterByIdentifier = true,
      pickListCriteria = this.getCriterion(null, forceFilterByIdentifier),
      gridCriteria,
      criteriaFieldName = this.getCriteriaFieldName(),
      me = this;

    function isInPickAndExecuteGrid() {
      return (
        me.grid &&
        me.grid.parentElement &&
        me.grid.parentElement.getClassName() === 'OBPickAndExecuteGrid'
      );
    }
    /**
     * Checks if the current object is an instance of OBSelectorFilterSelectItem.
     *
     * @returns {boolean} True if the current object is an instance of OBSelectorFilterSelectItem, otherwise false.
     */
    function isOBSelectorFilterSelectItem() {
      return (
        me &&
        me.getClassName() === 'OBSelectorFilterSelectItem'
      );
    }

    function cleanCriteria(crit, fkItem) {
      var i, criterion, fkFilterOnThisField;
      for (i = crit.length - 1; i >= 0; i--) {
        criterion = crit[i];
        if (criterion.criteria && isc.isAn.Array(criterion.criteria)) {
          // nested criterion, clean inside
          cleanCriteria(criterion.criteria);
          continue;
        }

        fkFilterOnThisField =
          criterion.operator === 'equals' && criterion.fieldName === me.name;

        if (fkFilterOnThisField || criteriaFieldName === criterion.fieldName) {
          crit.removeAt(i);
        }

        if ((isInPickAndExecuteGrid() || isOBSelectorFilterSelectItem()) && criterion.fieldName === 'id') {
          // we're in a P&E grid, selected ids should also be removed from criteria
          crit.removeAt(i);
        }
      }
    }

    function cleanOrCriterion() {
      if ((isInPickAndExecuteGrid() || isOBSelectorFilterSelectItem()) && gridCriteria._OrExpression) {
        // we're in a P&E grid, _OrExpression parameter should also be removed as it is used as part of the selection criteria
        if (gridCriteria.criteria.length > 0) {
          gridCriteria = {
            operator: 'and',
            _constructor: 'AdvancedCriteria',
            criteria: gridCriteria.criteria
          };
        } else {
          gridCriteria = {};
          gridCriteria.criteria = [];
        }
      }
    }

    if (this.form.grid.sourceWidget.lazyFiltering) {
      // Fetch the criteria from the current values of the filter editor
      // Invoke the convertCriteria function to filter by the record selected in the parent tab if needed
      gridCriteria = this.grid.sourceWidget.convertCriteria(
        this.form.grid.getValues()
      );
    } else {
      gridCriteria = this.form.grid.sourceWidget.getCriteria();
    }

    gridCriteria = gridCriteria || {
      _constructor: 'AdvandedCriteria',
      operator: 'and'
    };
    gridCriteria.criteria = gridCriteria.criteria || [];

    // remove from criteria the field used for current filter so drop down doesn't
    // restrict its values
    cleanCriteria(gridCriteria.criteria);
    cleanOrCriterion();

    if (this.form.grid.sourceWidget && this.form.grid.sourceWidget.dataSource) {
      gridCriteria = this.form.grid.sourceWidget.dataSource.convertRelativeDates(
        gridCriteria
      );
    }

    // when in refresh picklist the user is typing
    // a value, filter using that
    if (this.keyPressed && pickListCriteria) {
      gridCriteria.criteria.add(pickListCriteria);
      delete this.keyPressed;
    }

    // add a dummy criteria to force a fetch
    // smartclient will try to do smart and prevent fetches if
    // criteria have not changed
    // note the system can be made smarter by checking if something
    // got reloaded in the underlying grid
    if (this.forceReload) {
      gridCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
    }
    return gridCriteria;
  },

  setForceReload: function() {
    this.forceReload = true;
    if (this.form) {
      this.invalidateDisplayValueCache();
    }
  },

  canEditCriterion: function(criterion) {
    var firstCriteria;
    if (criterion.operator === 'and') {
      // and operator does not include the fieldName as a root property
      if (!criterion.criteria || criterion.criteria.length === 0) {
        return true;
      } else {
        // all criteria of the criterion are associated with the same name, pick the first
        firstCriteria = criterion.criteria[0];
        return (
          firstCriteria.fieldName === this.name ||
          (this.criteriaField && firstCriteria.fieldName === this.criteriaField)
        );
      }
    } else {
      return (
        criterion &&
        (criterion.fieldName === this.name ||
          (this.criteriaField && criterion.fieldName === this.criteriaField))
      );
    }
  },

  getCriterion: function(textMatchStyle, forceFilterByIdentifier) {
    var value, operator, fieldName, crit, manualDSForcedFilterByIdentifier;

    // sometimes (i.e. when the filter drop down is populated) it is needed to force a filter using the identifier
    // if the filter using the identifier is not allowed, the filter type will be reverted to 'id' at the end of this function
    if (forceFilterByIdentifier) {
      this.filterType = 'identifier';
    }
    value = this.getCriteriaValue();
    if (value === null || isc.is.emptyString(value)) {
      return;
    }

    // the criteria parser expects an or expression
    if (isc.isAn.Array(value)) {
      value = this.mapValueToDisplay(value);
    }

    operator = this.getOperator(textMatchStyle, isc.isAn.Array(value));
    fieldName = this.getCriteriaFieldName();

    crit = this.parseValueExpressions(value, fieldName, operator);

    if (crit === null) {
      crit = {
        fieldName: fieldName,
        operator: operator,
        value: value
      };
    }

    if (this.operator && this.operator !== 'iContains') {
      // In this case we need to overwrite the operator assigned by the parseValueExpressions/parseOBValueExpressions logic
      crit = this.replaceCriterionOperator(crit, value, this.operator);
    }

    if (this.allowFkFilterByIdentifier === false) {
      // in order to maintain backwards compatibility, manual datasources can be
      // defined as not supporting filtering by id see issue # 28432
      manualDSForcedFilterByIdentifier =
        this.form &&
        this.form.grid &&
        this.form.grid.sourceWidget &&
        this.form.grid.sourceWidget.alwaysFilterFksByIdentifier;

      if (!manualDSForcedFilterByIdentifier) {
        this.filterType = 'id';
      }
    }

    return crit;
  },

  getOperator: function(textMatchStyle) {
    if (this.filterType === 'id') {
      return 'equals';
    } else {
      if (this.operator && this.operator !== 'iContains') {
        return this.operator;
      } else {
        return this.Super('getOperator', arguments);
      }
    }
  },

  replaceCriterionOperator: function(criterion, value, newOperator) {
    var newCriterion = criterion,
      i;
    if (newCriterion.criteria && newCriterion.criteria.length > 0) {
      // If there is a sub-criteria, go inside to process the childs
      for (i = 0; i < newCriterion.criteria.length; i++) {
        newCriterion.criteria[i] = this.replaceCriterionOperator(
          newCriterion.criteria[i],
          value,
          newOperator
        );
      }
    } else if (
      (criterion.operator === 'iContains' ||
        criterion.operator === 'contains') &&
      value.indexOf('~') !== 0 &&
      value.indexOf('!~') !== 0
    ) {
      // In case the criteria is 'iContains'/'contains', replace it by the desired one,
      // but only in the case there are no explicit 'iContains'/'contains' prefixes
      newCriterion.operator = newOperator;
    }
    // TODO: If there is a complex criteria with a 'iContains'/'contains' prefix, like "Cust or ~mplo", it won't work ok, since it will be
    //       translated to "^Cust or ^mplo" or "==Cust or ==mplo" (depending of the newOperator) instead of "^Cust or ~mplo" or "==Cust or ~mplo"
    return newCriterion;
  },

  setCriterion: function(criterion) {
    var i,
      value,
      values = [],
      operators = isc.DataSource.getSearchOperators(),
      valueSet = false,
      criteria = criterion ? criterion.criteria : null,
      identifier;
    if (criteria && criteria.length && criterion.operator === 'or') {
      for (i = 0; i < criteria.length; i++) {
        //handles case where column filter symbols are removed. Refer Issue https://issues.openbravo.com/view.php?id=23925
        if (
          criteria[i].operator !== 'iContains' &&
          criteria[i].operator !== 'contains' &&
          criteria[i].operator !== 'regexp'
        ) {
          value = criteria[i].value;
          if (
            operators[criteria[i].operator] &&
            operators[criteria[i].operator].ID === criteria[i].operator &&
            operators[criteria[i].operator].symbol &&
            value &&
            value.indexOf(operators[criteria[i].operator].symbol) === -1
          ) {
            identifier = this.getRecordIdentifierFromId(value);
            if (this.filterType === 'id' && identifier) {
              value = identifier;
            }
            values.push(operators[criteria[i].operator].symbol + value);
            valueSet = true;
          }
        }
        if (valueSet === false) {
          values.push(criteria[i].value);
        }
        valueSet = false;
      }
      this.setValue(values);
    } else {
      value = this.buildValueExpressions(criterion);
      if (this.filterType === 'id') {
        identifier = this.getRecordIdentifierFromId(value);
        if (identifier) {
          value = identifier;
        }
      }
      if (this.disableFkDropdown) {
        // if the fk dropdown is disabled then the filter must behave like a text filter
        // that means that the symbol of the default operator is not shown
        if (criterion.operator !== this.getOperator()) {
          value = operators[criterion.operator].symbol + value;
        }
      } else {
        if (
          criterion.operator !== 'iContains' &&
          criterion.operator !== 'contains' &&
          criterion.operator !== 'regexp'
        ) {
          if (
            operators[criterion.operator] &&
            operators[criterion.operator].ID === criterion.operator &&
            operators[criterion.operator].symbol &&
            value &&
            value.indexOf(operators[criterion.operator].symbol) === -1
          ) {
            value = operators[criterion.operator].symbol + value;
          }
        }
      }
      this.setValue(value);
    }
    // if iBetweenInclusive operator is used, delete the ZZZZZZZZZZ
    if (
      this.getValue()
        .toString()
        .indexOf('...') !== -1 &&
      this.getValue()
        .toString()
        .indexOf('ZZZZZZZZZZ') !== -1
    ) {
      this.setValue(
        this.getValue()
          .toString()
          .substring(0, this.getValue().toString().length - 10)
      );
    }
  },

  // for Table references the property name used in the criteria depends on the type of filtering
  // when the filtering is done in the server, the displayProperty is used
  // on adaptive filtering (client side), OB.Constants.IDENTIFIER is used
  getDisplayProperty: function() {
    var theGrid = this.grid,
      name = this.name;
    // Use willFetchData() to identify whether we are performing adaptive filtering
    if (
      theGrid &&
      name &&
      theGrid.getField(name) &&
      theGrid.getField(name).displayProperty &&
      this.form.grid.sourceWidget.willFetchData()
    ) {
      return (
        name +
        OB.Constants.FIELDSEPARATOR +
        theGrid.getField(name).displayProperty
      );
    }
    return null;
  },

  // make sure that the correct field name is used to filter the main grid
  // if this is not here then the value will be removed by smartclient as it
  // sets the criterion back into the item
  // see also the setValuesAsCriteria in ob-grid-js which again translates
  // back
  getCriteriaFieldName: function() {
    if (this.filterType === 'id') {
      return this.name;
    } else {
      return (
        this.criteriaField ||
        this.getDisplayProperty() ||
        this.name + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER
      );
    }
  },

  // solve a small bug in the value expressions
  buildValueExpressions: function() {
    var ret = this.Super('buildValueExpressions', arguments);
    if (isc.isA.String(ret) && ret.contains('undefined')) {
      return ret.replace('undefined', '');
    }
    return ret;
  },

  refreshPickList: function() {
    if (this.disableFkDropdown) {
      return;
    }
    if (this.valueIsExpression()) {
      return;
    }

    // is called when the user enters values
    // filter using those values
    if (!this._pickedValue) {
      this.keyPressed = true;
    }

    return this.Super('refreshPickList', arguments);
  },

  valueIsExpression: function() {
    var prop,
      opDefs,
      val = this.getElementValue();
    // if someone starts typing and and or then do not filter
    // onkeypress either
    if (val.contains(' and')) {
      return true;
    }

    if (val.startsWith('=')) {
      return true;
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

        return opDefs[prop].symbol && val.startsWith(opDefs[prop].symbol);
      }
    }
    return false;
  },

  pickValue: function(value) {
    // this is needed to discern if the text has been changed automatically when a record is selected, or manually by the user
    this._pickingValue = true;
    this.Super('pickValue', arguments);
    delete this._pickingValue;
  },

  handleChanged: function(value) {
    if (
      !this.grid.sourceWidget.alwaysFilterFksByIdentifier &&
      (this._pickingValue ||
        this._pickingArrivedValue ||
        this.allowFkFilterByIdentifier === false)
    ) {
      // if the filter text has changed because a value has been ficked from the filter drop down, use the id filter
      // do this also if the only filter type allowed is 'id'
      this.filterType = 'id';
    } else {
      // otherwise use the standard filter using the record identifier
      this.filterType = 'identifier';
    }
    if (this._pickingArrivedValue) {
      // changed caused by showing the pick list having a value previously selected
      // as it is invoked asynchronously, remove flag here
      delete this._pickingArrivedValue;
    }
    this.Super('handleChanged', arguments);
  },

  // if the filterType is ID, try to return the record ids instead of the record identifiers
  getCriteriaValue: function() {
    var value,
      values = this.getValue(),
      i,
      j,
      criteriaValues = [],
      recordIds;
    if (values && this.filterType === 'id') {
      for (i = 0; i < values.length; i++) {
        value = values[i];
        if (isc.isAn.Array(value)) {
          // when "or" criteria value is an array of 1 element
          value = value[0];
        }
        if (value.startsWith('==')) {
          // if the value has the equals operator prefix, get rid of it
          value = value.substring(2);
        }
        recordIds = this.getRecordIdsFromIdentifier(value);
        if (!recordIds) {
          // if the record is not found  or it does not have an id, use the standard criteria value
          return this.Super('getCriteriaValue', arguments);
        } else {
          for (j = 0; j < recordIds.length; j++) {
            if (!criteriaValues.contains(recordIds[j])) {
              criteriaValues.add(recordIds[j]);
            }
          }
        }
      }
      return criteriaValues;
    } else {
      return this.Super('getCriteriaValue', arguments);
    }
  },

  // given an identifier, returns an array of the filter picklist records that have that identifier
  getRecordIdsFromIdentifier: function(identifier) {
    var records,
      recordIds = [],
      i;
    if (
      this.pickList &&
      this.pickList.data.find(OB.Constants.IDENTIFIER, identifier)
    ) {
      records = this.pickList.data.findAll(OB.Constants.IDENTIFIER, identifier);
    } else if (
      this.filterAuxCache &&
      this.filterAuxCache.find(OB.Constants.IDENTIFIER, identifier)
    ) {
      records = this.filterAuxCache.findAll(
        OB.Constants.IDENTIFIER,
        identifier
      );
    }

    if (!records) {
      // it is possible not to have any records in case of multitple items selected in current criteria,
      // after that another criteria is added making some of current one not to apply anymore
      return recordIds;
    }

    for (i = 0; i < records.length; i++) {
      recordIds.add(records[i][this.keyProperty]);
    }
    return recordIds;
  },

  getRecordIdentifierFromId: function(keyValue) {
    var recordIdentifier,
      keyProperty = this.keyProperty || OB.Constants.ID;
    if (this.pickList && this.pickList.data.find(keyProperty, keyValue)) {
      recordIdentifier = this.pickList.data.find(keyProperty, keyValue)[
        OB.Constants.IDENTIFIER
      ];
    } else if (
      this.filterAuxCache &&
      this.filterAuxCache.find(keyProperty, keyValue)
    ) {
      recordIdentifier = this.filterAuxCache.find(keyProperty, keyValue)[
        OB.Constants.IDENTIFIER
      ];
    }
    return recordIdentifier;
  },

  showPickList: function() {
    if (this.disableFkDropdown) {
      return;
    }
    this.Super('showPickList', arguments);
  }
});
