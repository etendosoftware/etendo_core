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
 * All portions are Copyright (C) 2011-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
// This file contains direct overrides of Smartclient types.
// Normally we introduce new subtypes of Smartclient types. However for
// some cases it makes sense to directly set properties on top Smartclient
// types. This is done in this file.
// We have dates/times in the database without timezone, we assume GMT therefore 
// for all our date/times we use GMT on both the server and the client
// NOTE: causes issue https://issues.openbravo.com/view.php?id=16014
// NOTE: disabled as now timezone is send from the client to the server
// Time.setDefaultDisplayTimezone(0);
// Call duplicated - we include SmartClient embedded in StaticResources for classic windows
isc.setAutoDraw(false);
isc.screenReader = false;

isc.DataSource.serializeTimeAsDatetime = true;

isc.DataSource.addProperties({
  compareDates: function (date1, date2, fieldName, otherFieldName) {
    var field = this.getField(fieldName),
        otherField = otherFieldName ? this.getField(otherFieldName) : null;
    if ((field && (field.type === 'datetime' || field.type === '_id_24')) || (otherField && (otherField.type === 'datetime' || otherField.type === '_id_24'))) {
      return Date.compareDates(date1, date2);
    } else {
      return Date.compareLogicalDates(date1, date2);
    }
  }
});

isc.DataSource.addSearchOperator({
  ID: 'exists',
  // Compares two criteria that use the 'exists' operator:
  // - If they are the same, return 0 (no need to reapply the filter)
  // - If they are not the same, return -1 to force a datasource call. 
  // - Returning 1 would imply that the new criterion is more retrictive than the old criterion and that the filtered results can be
  // fetched locally, we don't want to do that with criteria that use the 'exists' operator
  compareCriteria: function (newCriterion, oldCriterion, operator, ds) {
    var newValues, oldValues, i;
    if (newCriterion.fieldName === oldCriterion.fieldName && newCriterion.existsQuery === oldCriterion.existsQuery) {
      // If the fieldName and the exists query is the same, compare the values
      newValues = newCriterion.value;
      oldValues = oldCriterion.value;
      if (newValues.length === oldValues.length) {
        // They have the same length, check if they are the same in the same order
        for (i = 0; i < newValues.length; i++) {
          if (newValues[i] !== oldValues[i]) {
            return -1;
          }
        }
        // Same fieldName, same existsQuery, same values: both criterias are the same
        return 0;
      } else {
        return -1;
      }
    } else {
      return -1;
    }
  },

  // condition function is used for local filtering, it returns true in case the field value
  // fulfills the criterion. As 'exists' operator is always evaluated in backend, it can be
  // safely assumed all records fulfill the criteria, not being necessary to recalculate it in
  // client. Note this function is invoked in case there is already an 'exists' applied in the
  // criteria and a more restrictive one is set so local filtering can be performed.
  condition: function (fieldName, fieldValue, criterionValues) {
    return true;
  }
});

isc.Tree.addProperties({
  _original_getLoadState: isc.Tree.getPrototype().getLoadState,
  getLoadState: function (node) {
    if (node._hasChildren === false) {
      return isc.Tree.LOADED;
    } else {
      return this._original_getLoadState(node);
    }
  }
});

isc.ResultTree.addProperties({
  _original_loadChildrenReply: isc.ResultTree.getPrototype().loadChildrenReply,
  loadChildrenReply: function (dsResponse, data, request) {
    var target = window[this.componentId];

    if (dsResponse && dsResponse.error && dsResponse.error.type === 'tooManyNodes') {
      if (target && target.view) {
        target.view.messageBar.setMessage('error', null, OB.I18N.getLabel('OBUIAPP_TooManyNodes'));
      }
      if (target && target.treeItem && target.treeItem.tree) {
        target.treeItem.tree.showErrorMessageInPicker(OB.I18N.getLabel('OBUIAPP_TooManyResults'));
      }
      return;
    }

    this._original_loadChildrenReply(dsResponse, data, request);
    if (request && request.params && request.params.selectedRecords) {
      if (target && target.treeDataArrived) {
        target.treeDataArrived();
      }
    }
  },
  _original_indexOf: isc.ResultTree.getPrototype().indexOf,
  indexOf: function (node, a, b, c, d) {
    if (!node) {
      return -1;
    } else {
      return this._original_indexOf(node, a, b, c, d);
    }
  },

  dataArrived: function (parentNode) {
    var children = this.getChildren(parentNode),
        target = window[this.componentId];
    if (target.transformData) {
      target.transformData(children);
    }
    this.Super('dataArrived', arguments);
  }
});

isc.ResultSet.addProperties({
  _original_removeCacheData: isc.ResultSet.getPrototype().removeCacheData,
  removeCacheData: function (updateData) {
    var filteringOnClient = this.allRows !== null,
        i, index, ds;
    this._original_removeCacheData(updateData);
    if (filteringOnClient) {
      ds = this.getDataSource();
      // remove any rows that were present in the cache
      for (i = 0; i < updateData.length; i++) {
        index = ds.findByKeys(updateData[i], this.localData);
        if (index !== -1) {
          this.localData.removeAt(index);
        }
      }
    }
  },

  _original_updateCacheData: isc.ResultSet.getPrototype().updateCacheData,
  updateCacheData: function (updateData, dsRequest) {
    var filteringOnClient = this.allRows !== null,
        i, indexAllRows, indexLocalData, ds;
    this._original_updateCacheData(updateData, dsRequest);
    if (filteringOnClient) {
      ds = this.getDataSource();
      // remove any rows that were present in the cache
      for (i = 0; i < updateData.length; i++) {
        indexLocalData = ds.findByKeys(updateData[i], this.localData);
        indexAllRows = ds.findByKeys(updateData[i], this.allRows);
        if (indexLocalData !== -1 && indexAllRows !== -1) {
          this.localData[indexLocalData] = this.allRows[indexAllRows];
        }
      }
    }
  },

  _original_shouldUseClientSorting: isc.ResultSet.getPrototype().shouldUseClientSorting,
  shouldUseClientSorting: function () {
    if (this.grid && this.grid._filteringAndSortingManually) {
      return false;
    } else {
      return this._original_shouldUseClientSorting();
    }
  }
});

isc.Canvas.addProperties({

  // make sure that the datasources are also destroyed
  _original_destroy: isc.Canvas.getPrototype().destroy,
  destroy: function () {
    if (this.optionDataSource && !this.optionDataSource.potentiallyShared) {
      this.optionDataSource.destroy();
      this.optionDataSource = null;
    }
    if (this.dataSource && !this.dataSource.potentiallyShared) {
      this.dataSource.destroy();
      this.dataSource = null;
    }
    this._original_destroy();
  }
});

//Let the click on an ImgButton and Button fall through to its action method 
isc.ImgButton.addProperties({
  click: function () {
    if (this.action) {
      this.action();
    }
  }
});

isc.Button.addProperties({
  click: function () {
    if (this.action) {
      this.action();
    }
  }
});

isc.StaticTextItem.addProperties({
  canFocus: false
});

// we generate datasources with minimal field sets to 
// minimize the javascript, therefore the standard
// processValue implementation does not work for this
// filter operator, it uses the datasource field definitions
// therefore override it to only return the value
// see DataSourceConstants.MINIMAL_PROPERTY_OUTPUT
// https://issues.openbravo.com/view.php?id=18557
if (!isc.DynamicForm.getOperatorIndex()) {
  isc.DynamicForm.buildOperatorIndex();
}
isc.DynamicForm.getOperatorIndex()['=.'][0].processValue = function (value, ds) {
  return value;
};

isc.Layout.addProperties({

  destroyAndRemoveMembers: function (toDestroy) {
    var i, len, nextIndex = 0;
    if (!isc.isA.Array(toDestroy)) {
      toDestroy = [toDestroy];
    }
    len = toDestroy.length;
    for (i = 0; i < len; i++) {
      if (toDestroy[nextIndex] && toDestroy[nextIndex].destroy) {
        toDestroy[nextIndex].destroy();
      }
      if (toDestroy.length === len) {
        nextIndex = nextIndex + 1;
      }
    }
    this.removeMembers(toDestroy);
  }
});

isc.TextItem.addProperties({

  // to support and/or in text items
  // https://issues.openbravo.com/view.php?id=18747
  // NOTE: if Smartclient starts to support and/or, revisit this code
  parseValueExpressions: function (value, fieldName) {
    // enable hack to force Smartclient to support and/or logic
    if (isc.isA.String(value) && (value.toUpperCase().contains(' OR ') || value.toUpperCase().contains(' AND '))) {
      return this.parseOBValueExpressions(value, fieldName);
    }
    return this.Super('parseValueExpressions', arguments);
  },

  // this is a copy of the FormItem.parseValueExpressions to support
  // and/or logic for enum and text fields
  parseOBValueExpressions: function (value, fieldName) {
    var type = this.getType(),
        i, isValidLogicType = (isc.SimpleType.inheritsFrom(type, 'enum') || isc.SimpleType.inheritsFrom(type, 'text') || isc.SimpleType.inheritsFrom(type, 'integer') || isc.SimpleType.inheritsFrom(type, 'float') || isc.SimpleType.inheritsFrom(type, 'date')),
        opIndex = isc.DynamicForm.getOperatorIndex(),
        validOps = isc.getKeys(opIndex),
        result = {
        operator: 'and',
        criteria: []
        },
        valueParts = [],
        ds = isc.DS.get(this.form.expressionDataSource || this.form.dataSource);

    if (!value) {
      value = this.getValue();
    }
    if (!value) {
      return;
    }

    if (!isc.isA.String(value)) {
      value += '';
    }

    var tempOps, tempOp;

    var defOpName = this.getOperator();
    if (defOpName) {
      validOps.add(defOpName);
    }

    var defOp = ds ? ds.getSearchOperator(defOpName) : {
      id: defOpName
    };

    var field, insensitive = defOp.caseInsensitive;
    var partIndex, parts, partCrit, part;

    if (isValidLogicType && value.contains(' and ')) {
      valueParts = value.split(' and ');
    } else if (isValidLogicType && value.contains(' or ')) {
      valueParts = value.split(' or ');
      result.operator = 'or';
    } else if (value.contains('...')) {
      valueParts = value.split('...');
      if (valueParts.length === 2) {
        tempOps = opIndex['...'];

        if (tempOps) {
          tempOp = (insensitive ? tempOps.find('caseInsensitive', true) : tempOps[0]);
        }

        field = ds ? ds.getField(fieldName) : null;

        if (field && isc.SimpleType.inheritsFrom(field.type, 'date')) {
          valueParts[0] = new Date(Date.parse(valueParts[0]));
          valueParts[0].logicalDate = true;
          valueParts[1] = new Date(Date.parse(valueParts[1]));
          valueParts[1].logicalDate = true;
        } else if (field && field.type === 'text') {

          if (!valueParts[1].endsWith(this._betweenInclusiveEndCrit)) {
            valueParts[1] += this._betweenInclusiveEndCrit;
          }
        }

        return {
          fieldName: fieldName,
          operator: tempOp.ID,
          start: valueParts[0],
          end: valueParts[1]
        };
      }
    } else {
      valueParts = [value];
    }

    var skipTheseOps = [' and ', ' or '];

    for (i = 0; i < valueParts.length; i++) {
      var key, valuePart = valueParts[i],
          subCrit = {
          fieldName: fieldName
          };

      field = ds ? ds.getField(fieldName) : null;
      var isDateField = (field ? field && isc.SimpleType.inheritsFrom(field.type, 'date') : false),
          valueHasExpression = false;

      for (key in opIndex) {
        if (opIndex.hasOwnProperty(key)) {
          if (!key) {
            continue;
          }

          var ops = opIndex[key],
              wildCard = false,
              op;

          if (key === '==' && isc.isA.String(valuePart) && valuePart.startsWith('=') && !valuePart.startsWith('==') && !valuePart.startsWith('=(')) {
            wildCard = true;
          }

          if (ops && ops.length) {
            op = ops.find('caseInsensitive', insensitive) || ops[0];
          }

          if (!op || !op.symbol || skipTheseOps.contains(op.symbol)) {
            continue;
          }

          if (validOps.contains(op.symbol) && ((isc.isA.String(valuePart) && (valuePart.startsWith(op.symbol) ||

          (op.symbol === '...' && valuePart.contains(op.symbol)))) || wildCard)) {
            valueHasExpression = true;

            if (valuePart.startsWith(op.symbol)) {
              valuePart = valuePart.substring(op.symbol.length - (wildCard ? 1 : 0));
            }

            if (op.closingSymbol) {
              // this is a containing operator (inSet, notInSet), with opening and 
              // closing symbols...  check that the value endsWith the correct 
              // closing symbol and strip it off - op.processValue() will split 
              // the string for us later
              if (valuePart.endsWith(op.closingSymbol)) {
                valuePart = valuePart.substring(0, valuePart.length - op.closingSymbol.length);
              }
            }

            if (valuePart.contains('...')) {
              // allow range operators as well as conjunctives
              var rangeValueParts = valuePart.split('...');
              if (rangeValueParts.length === 2) {
                tempOps = opIndex['...'];

                if (tempOps) {
                  tempOp = (insensitive ? tempOps.find('caseInsensitive', true) : tempOps[0]);
                }

                field = ds ? ds.getField(fieldName) : null;

                if (field && isc.SimpleType.inheritsFrom(field.type, 'date')) {
                  rangeValueParts[0] = new Date(Date.parse(rangeValueParts[0]));
                  rangeValueParts[0].logicalDate = true;
                  rangeValueParts[1] = new Date(Date.parse(rangeValueParts[1]));
                  rangeValueParts[1].logicalDate = true;
                } else if (field && field.type === 'text') {

                  if (!rangeValueParts[1].endsWith(this._betweenInclusiveEndCrit)) {
                    rangeValueParts[1] += this._betweenInclusiveEndCrit;
                  }
                }

                result.criteria.add({
                  fieldName: fieldName,
                  operator: tempOp.ID,
                  start: rangeValueParts[0],
                  end: rangeValueParts[1]
                });

                continue;
              }
            }

            if (isDateField) {
              valuePart = new Date(Date.parse(valuePart));
              valuePart.logicalDate = true;
            }

            subCrit.operator = op.ID;

            if (op.processValue) {
              valuePart = op.processValue(valuePart, ds);
            }

            if (op.wildCard && isc.isA.String(valuePart) && valuePart.contains(op.wildCard)) {
              // this is an operator that supports wildCards (equals, notEquals)...
              parts = valuePart.split(op.wildCard);

              if (parts.length > 1) {
                for (partIndex = 0; partIndex < parts.length; partIndex++) {
                  part = parts[partIndex];

                  if (!part || part.length === 0) {
                    continue;
                  }

                  partCrit = {
                    fieldName: fieldName,
                    value: part
                  };

                  var hasPrefix = partIndex > 0,
                      hasSuffix = parts.length - 1 > partIndex;

                  if (hasPrefix && hasSuffix) {
                    // this is a contains criteria
                    partCrit.operator = insensitive ? 'iContains' : 'contains';
                  } else if (hasPrefix) {
                    // this is an endsWith criteria
                    partCrit.operator = insensitive ? 'iEndsWith' : 'endsWith';
                  } else if (hasSuffix) {
                    // this is a startsWith criteria
                    partCrit.operator = insensitive ? 'iStartsWith' : 'startsWith';
                  }

                  result.criteria.add(partCrit);
                }

                // we'll include a check for this attribute when rebuilding the 
                // value later
                this._lastValueHadWildCards = true;

                // clear out the sub-crit's operator - this will prevent it being
                // added to the result criteria below (we've already added 
                // everything we need above
                subCrit.operator = null;
              }
            } else {
              // set the value if one is required for the op
              if (op.valueType !== 'none') {
                subCrit.value = valuePart;
              }
            }

            break;
          }
        }
      }
      if (!valueHasExpression) {
        // this was a straight expression like "10"
        subCrit.operator = defOpName;
        subCrit.value = valuePart;
      }
      if (subCrit.operator) {
        result.criteria.add(subCrit);
      }
    }
    //  this.logWarn("Parsed expression:" + value + " to criterion:" + this.echo(result));
    if (result.criteria.length === 1) {
      result = result.criteria[0];
    }
    if (result.criteria && result.criteria.length === 0) {
      result = null;
    }
    if (!result.fieldName) {
      result.fieldName = fieldName;
    }

    return result;
  },

  // see comments in super type for useDisabledEventMask
  // http://forums.smartclient.com/showthread.php?p=70160#post70160
  // https://issues.openbravo.com/view.php?id=17936
  useDisabledEventMask: function () {
    if (isc.Browser.isIE) {
      return false;
    }
    return this.Super('useDisabledEventMask', arguments);
  },

  // store the item that was focused when the key is pressed
  // this information is then used in the OBViewGrid.cellEditEnd function. See issue https://issues.openbravo.com/view.php?id=27730
  _original_handleKeyDown: isc.TextItem.getPrototype().handleKeyDown,
  handleKeyDown: function (event, eventInfo) {
    this.form.lastKeyDownItem = this;
    return this._original_handleKeyDown(event, eventInfo);
  }
});

// NOTE BEWARE: methods/props added here will overwrite and NOT extend FormItem
// properties! 
isc.FormItem.addProperties({
  // default, is overridden in generated field template
  personalizable: true,
  updatable: true,
  width: '*',

  // always take up space when an item is hidden in a form
  alwaysTakeSpace: true,

  // If an item has an optiomDataSource, a fetch is made in the init() or setValue() ...
  // "The fetch occurs if the item value is non null on initial draw of the form or whenever setValue() is called"
  // http://www.smartclient.com/docs/8.1/a/b/c/go.html#attr..FormItem.fetchMissingValues
  fetchMissingValues: false,

  // disable tab to icons
  canTabToIcons: false,

  _original_validate: isc.FormItem.getPrototype().validate,
  validate: function () {
    if (this.preventValidation) {
      return;
    }
    return this._original_validate();
  },

  _original_init: isc.FormItem.getPrototype().init,
  init: function () {
    this.obShowIf = this.showIf; // Copy the reference of showIf definition
    OB.Utilities.addRequiredSuffixToBaseStyle(this);
    // and continue with the original init
    this._original_init();
  },

  _handleEditorExit: isc.FormItem.getPrototype().handleEditorExit,
  handleEditorExit: function () {
    if (this.form && this.form._isRedrawing) {
      return;
    }
    return this._handleEditorExit();
  },

  // make sure that the datasources are also destroyed
  _original_destroy: isc.FormItem.getPrototype().destroy,
  destroy: function () {
    if (this.optionDataSource && !this.optionDataSource.potentiallyShared) {
      this.optionDataSource.destroy();
      this.optionDataSource = null;
    }
    if (this.dataSource && !this.dataSource.potentiallyShared) {
      this.dataSource.destroy();
      this.dataSource = null;
    }
    this.isBeingDestroyed = true;
    this._original_destroy();
  },

  // overridden to not show if hiddenInForm is set
  _show: isc.FormItem.getPrototype().show,
  show: function (arg1) {
    if (this.hiddenInForm) {
      return;
    }
    this._show(arg1);
  },

  // overridden to not make a difference between undefined and null
  _original_compareValues: isc.FormItem.getPrototype().compareValues,
  compareValues: function (value1, value2) {
    var undef, val1NullOrUndefined = (value1 === null || value1 === undef || value1 === ''),
        val2NullOrUndefined = (value2 === null || value2 === undef || value2 === '');
    if (val1NullOrUndefined && val2NullOrUndefined) {
      return true;
    }
    // a special case, smartclient makes a mistake when comparing
    // zero against an empty string
    if (value1 === 0 && value2 !== 0) {
      return false;
    }
    if (value1 !== 0 && value2 === 0) {
      return false;
    }
    return this._original_compareValues(value1, value2);
  },

  _handleTitleClick: isc.FormItem.getPrototype().handleTitleClick,
  handleTitleClick: function () {
    // always titleclick directly as sc won't call titleclick
    // in that case
    if (this.isDisabled()) {
      this.titleClick(this.form, this);
      return false;
    }
    // forward to the original method
    return this._handleTitleClick();
  },

  titleClick: function (form, item) {
    item.focusInItem();
    if (item.linkButtonClick) {
      item.linkButtonClick();
    }
  },

  changed: function (form, item, value) {
    this._hasChanged = true;
    this.clearErrors();

    if (this.redrawOnChange) {
      if (this.form.onFieldChanged) {
        this.form.onFieldChanged(this.form, item || this, value);
      }
      if (this.form && this.form.view && this.form.view.toolBar && this.form.view.toolBar.refreshCustomButtonsView) {
        this.form.view.toolBar.refreshCustomButtonsView(this.form.view);
      }
    }
  },

  focus: function (form, item) {
    var view = OB.Utilities.determineViewOfFormItem(item);
    if (view) {
      view.lastFocusedItem = this;
    }
    this.hasFocus = true;
  },

  blur: function (form, item) {
    if (this.form && this.form._isRedrawing) {
      return;
    }
    if (item._hasChanged && form && form.handleItemChange) {
      form.handleItemChange(item);
    }
  },

  // prevent a jscript error in ie when closing a tab
  // https://issues.openbravo.com/view.php?id=18890
  _doBlurItem: isc.FormItem.getPrototype().blurItem,
  blurItem: function () {
    if (!this.form || this.form.destroyed || this.form._isRedrawing) {
      return;
    }
    this._doBlurItem();
  },

  isDisabled: function (ignoreTemporaryDisabled) {
    if (!this.form) {
      return false;
    }
    // disabled if the property can not be updated and the form or record is new
    // explicitly comparing with false as it is only set for edit form fields
    if (this.updatable === false && !(this.form.isNew || this.form.getValue('_new'))) {
      // note: see the ob-view-form.js resetCanFocus method 
      this.canFocus = false;
      return true;
    }
    var disabled = this.form.readOnly || this.readonly || this.disabled;
    // allow focus if all items are disabled
    // note: see the ob-view-form.js resetCanFocus method 
    this.canFocus = this.form.allItemsDisabled || !disabled;
    return (!ignoreTemporaryDisabled && this.form.allItemsDisabled) || disabled;
  },

  // return all relevant focus condition
  isFocusable: function (ignoreTemporaryDisabled) {
    return this.getCanFocus() && this.isVisible() && !this.isDisabled(ignoreTemporaryDisabled);
  },

  // overridden to never use the forms datasource for fields
  getOptionDataSource: function () {
    var ods = this.optionDataSource;

    if (isc.isA.String(ods)) {
      ods = isc.DataSource.getDataSource(ods);
    }

    return ods;
  }
});

// To fix issue https://issues.openbravo.com/view.php?id=21786
isc.ComboBoxItem.addProperties({
  isPickListShown: function () {
    return (this.pickList ? (this.pickList.isDrawn() && this.pickList.isVisible()) : false);
  },

  hidePicker: function () {
    if (this.pickList) {
      this.pickList.hideClickMask();
      this.pickList.hide();
    }
  }
});

// overridden to never show a prompt. A prompt can be created manually 
// when overriding for example the DataSource (see the OBStandardView).
isc.RPCManager.showPrompt = false;
isc.RPCManager.neverShowPrompt = true;

// Overrides hasFireBug function to always return false,
// the SmartClient code has too many trace() calls that result in worse
// performance when using Firefox/Firebug
isc.Log.hasFireBug = function () {
  return false;
};

// prevent caching of picklists globally to prevent js error 
// when a picklist has been detached from a formitem
isc.PickList.getPrototype().cachePickListResults = false;

isc.DateItem.changeDefaults('textFieldDefaults', {
  isDisabled: function () {
    var disabled = this.Super('isDisabled', arguments);
    if (disabled) {
      return true;
    }
    if (this.parentItem.isDisabled()) {
      return true;
    }
    return false;
  }
});

// if not overridden then also errors handled by OB are shown in a popup
// see https://issues.openbravo.com/view.php?id=17136
isc.RPCManager.addClassProperties({
  _originalhandleError: isc.RPCManager.handleError,
  handleError: function (response, request) {
    var target = window[request.componentId];
    // refresh the toolbar buttons if possible to ensure that the refresh button is enabled
    if (target && target.view && target.view.toolBar && isc.isA.Function(target.view.toolBar.updateButtonState)) {
      delete target.view.isRefreshing;
      target.view.toolBar.updateButtonState();
    }

    if (response.status === -4 && target && target.showMessage) {
      var errorMessage = this.parseErrorMessage(response);
      if (errorMessage) {
        target.showMessage(isc.OBMessageBar.TYPE_ERROR, errorMessage);
      }
    }

    // in case of response timeout, show the error in the view if possible
    if (this.isServerTimeoutResponse(response) && this.canShowErrorMessage(target)) {
      target.view.setErrorMessageFromResponse(response, response.data, request);
    }

    if (!request.willHandleError) {
      this._originalhandleError(response, request);
    }
  },

  parseErrorMessage: function (response) {
    if (response.httpResponseText) {
      var jsonResponse = JSON.parse(response.httpResponseText);
      if (jsonResponse.response && jsonResponse.response.error && jsonResponse.response.error.message) {
        return jsonResponse.response.error.message;
      }
    }

    return null;
  },

  isServerTimeoutResponse: function (response) {
    return response.status === isc.RPCResponse.STATUS_SERVER_TIMEOUT;
  },

  canShowErrorMessage: function (target) {
    return target && target.view && isc.isA.Function(target.view.setErrorMessageFromResponse);
  },

  _originalEvalResult: isc.RPCManager.evalResult,
  evalResult: function (request, response, results) {
    if (response.status !== isc.RPCResponse.STATUS_SUCCESS && isc.isA.Function(request.errorCallback)) {
      // if the response contains an error status, call the errorCallback
      request.errorCallback(request, response);
    }

    return this._originalEvalResult(request, response, results);
  },

  // Escape characters that are not properly handled in JavaScript's eval. See issue #36788.
  // Solution based on Crockford's JSON.parse implementation
  // https://github.com/douglascrockford/JSON-js
  dangerousChars: /[\u0000\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,
  _originalperformTransactionReply: isc.RPCManager.performTransactionReply,
  performTransactionReply: function (transactionNum, results, wd) {
    var resp = results.responseText;

    this.dangerousChars.lastIndex = 0;
    if (resp && isc.isA.String(resp) && this.dangerousChars.test(resp)) {
      resp = resp.replace(this.dangerousChars, function (a) {
        return '\\u' + ('0000' + a.charCodeAt(0).toString(16)).slice(-4);
      });

      // results is a XMLHttpRequest, response properties are immutable by default,
      // this hacks allows to modify them
      Object.defineProperties(results, {
        'responseText': {
          writable: true
        },
        'response': {
          writable: true
        }
      });

      results.responseText = resp;
      results.response = resp;
    }

    return this._originalperformTransactionReply(transactionNum, results, wd);
  }
});

isc.Class.addClassProperties({
  _originalFireOnPause: isc.Class.fireOnPause,
  fireOnPause: function (id, callback, delay, target, instanceID) {
    if (id === 'performFilter') {
      if (target.currentThresholdToFilter) {
        delay = target.currentThresholdToFilter;
      }
    }
    this._originalFireOnPause(id, callback, delay, target, instanceID);
  }
});


// Allow searchs (with full dataset in memory/the datasource) not distinguish
// between accent or non-accent words
isc.DataSource.addProperties({

  _fieldMatchesFilter: isc.DataSource.getPrototype().fieldMatchesFilter,
  fieldMatchesFilter: function (fieldValue, filterValue, requestProperties) {
    fieldValue = OB.Utilities.removeAccents(fieldValue);
    filterValue = OB.Utilities.removeAccents(filterValue);
    return this._fieldMatchesFilter(fieldValue, filterValue, requestProperties);
  }
});

isc.RecordEditor.addProperties({
  _originalPerformFilter: isc.RecordEditor.getPrototype().performFilter,
  performFilter: function (suppressPrompt, forceFilter) {
    var grid = this.parentElement,
        key = isc.EventHandler.getKey();
    if (grid.lazyFiltering && !forceFilter && key === 'Enter') {
      // Pressing the enter key in the filter editor triggers the 'Apply Filter' actions
      grid.sorter.click();
      return;
    }
    if (!grid.lazyFiltering || forceFilter || grid._cleaningFilter) {
      this._originalPerformFilter(suppressPrompt);
    }
  }
});

// When filtering strings in backend, spaces are replaced by % in the resultant
// ilike expression. For example if filter is "jo sm" the query will be 
// "ilike '%jo%sm%'", so "John Smith" would be found. When filtering in client
// Smartclient doesn't do this conversion. This code, overwrittes Smartclient
// isc.contains comparator to work like in backend when it is called from stringComparasion
// function.
(function () {
  var containsNoBlanks, stringComparison = isc.DataSource.getSearchOperators().iContains.condition,
      originalContains = isc.clone(isc.contains);

  // Replaces isc.contains in the custom string comparator. Blank spaces are not
  // part of the comparision, but they separate different tokens to be found in 
  // the text. It returns true in case the tested text contains, in order, all the
  // tokens separated by blank spaces. 
  containsNoBlanks = function (tested, test) {
    var tokens, token, i, pendingToTest, idx;
    if (!tested) {
      return true;
    }

    tokens = test.split(' ');
    pendingToTest = tested;
    for (i = 0; i < tokens.length; i++) {
      token = tokens[i];
      idx = pendingToTest.indexOf(token);
      if (token && idx === -1) {
        return false;
      }
      pendingToTest = pendingToTest.substring(idx + token.length);
    }
    return true;
  };

  isc.addMethods(isc, {
    contains: function (string1, substring) {
      var args = arguments;
      if (args.callee.caller === stringComparison) {
        // invoking from stringComparsin function to do local (adaptive) filtering
        return containsNoBlanks(string1, substring);
      } else {
        // in other cases, use default logic
        return OB.Utilities.callAction({
          method: originalContains,
          target: this,
          parameters: arguments
        });
      }
    }
  });
}());

isc.builtinTypes.textArea = {
  inheritsFrom: 'text'
};

//delete the wildCard to avoid strange behaviour when filtering '*'
//see issue 25808
delete isc.DataSource.getSearchOperators().equals.wildCard;
delete isc.DataSource.getSearchOperators().iEquals.wildCard;
delete isc.DataSource.getSearchOperators().notEqual.wildCard;
delete isc.DataSource.getSearchOperators().iNotEqual.wildCard;