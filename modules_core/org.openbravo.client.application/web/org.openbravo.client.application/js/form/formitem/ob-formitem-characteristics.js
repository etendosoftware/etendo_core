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

isc.ClassFactory.defineClass('OBCharacteristicsItem', isc.CanvasItem);

isc.OBCharacteristicsItem.addProperties({
  completeValue: null,
  showTitle: false,
  init: function() {
    this.canvas = isc.OBCharacteristicsLayout.create({});

    this.colSpan = 4;
    this.disabled = false;

    this.Super('init', arguments);
  },

  setValue: function(value) {
    var field,
      formFields = [],
      itemIds = [];

    this.completeValue = value;
    if (!value || !value.characteristics) {
      if (!value) {
        this.hide();
      }
      this.Super('setValue', arguments);
      return;
    }

    this.show();

    //Remove all members the widget might have
    //this.canvas.removeMembers(this.canvas.getMembers());
    //
    //clear existing values. Refer issue https://issues.openbravo.com/view.php?id=25113
    this.canvas.clearValues();

    if (value.characteristics) {
      for (field in value.characteristics) {
        if (
          Object.prototype.hasOwnProperty.call(value.characteristics, field)
        ) {
          formFields.push({
            width: '*',
            title: field,
            disabled: true,
            name: '__Characteristic__' + field,
            type: 'OBTextItem',
            value: value.characteristics[field]
          });
          itemIds.push('__Characteristic__' + field);
        }
      }
    }

    formFields.unshift({
      defaultValue: this.title,
      type: 'OBSectionItem',
      sectionExpanded: true,
      itemIds: itemIds
    });

    this.canvas.setFields(formFields);

    // actual value is the one in DB
    this.setValue(value.dbValue);
  },

  destroy: function() {
    if (this.canvas && typeof this.canvas.destroy === 'function') {
      this.canvas.destroy();
      this.canvas = null;
    }
    return this.Super('destroy', arguments);
  }
});

isc.ClassFactory.defineClass('OBCharacteristicsLayout', isc.DynamicForm);

isc.OBCharacteristicsLayout.addProperties({
  titleOrientation: 'top',
  width: '*',
  numCols: 4,
  colWidths: ['25%', '25%', '25%', '25%'],
  titlePrefix: '<b>',
  titleSuffix: '</b>'
});

isc.ClassFactory.defineClass('OBCharacteristicsFilterDialog', isc.OBPopup);

isc.OBCharacteristicsFilterDialog.addProperties({
  isModal: true,
  showModalMask: true,
  dismissOnEscape: true,
  autoCenter: true,
  autoSize: true,
  vertical: true,
  showMinimizeButton: false,
  destroyOnClose: false,
  width: 100,
  height: 200,

  mainLayoutDefaults: {
    _constructor: 'VLayout',
    width: 300,
    layoutMargin: 5
  },

  buttonLayoutDefaults: {
    _constructor: 'HLayout',
    width: '100%',
    height: 40,
    layoutAlign: 'right',
    align: 'center',
    membersMargin: 5,
    autoParent: 'mainLayout'
  },

  okButtonDefaults: {
    _constructor: 'OBFormButton',
    height: 22,
    width: 80,
    canFocus: true,
    autoParent: 'buttonLayout',
    click: function() {
      this.creator.accept();
    }
  },

  clearButtonDefaults: {
    _constructor: 'OBFormButton',
    height: 22,
    width: 80,
    canFocus: true,
    autoParent: 'buttonLayout',
    click: function() {
      this.creator.clearValues();
    }
  },

  cancelButtonDefaults: {
    _constructor: 'OBFormButton',
    height: 22,
    width: 80,
    canFocus: true,
    autoParent: 'buttonLayout',
    click: function() {
      this.creator.cancel();
    }
  },

  /**
   * Based on values selected in the tree, returns the ones that are
   * going to be used for visualization and/or filtering:
   *
   *   -Filtering: includes all selected leaf nodes
   *   -Visualization: includes the top in branch fully selected nodes
   */
  getValue: function() {
    var selection = this.tree.getSelection(),
      result = {},
      i,
      c,
      completeParentNodes = [],
      node,
      currentChar,
      grandParent;

    for (i = 0; i < selection.length; i++) {
      node = selection[i];
      if (node.isCharacteristic) {
        continue;
      }

      if (!result[node.characteristic]) {
        result[node.characteristic] = {
          name: node.characteristic$_identifier,
          values: []
        };
      }

      currentChar = result[node.characteristic];

      if (node.children) {
        // parent node, include it only if fully selected
        if (!this.tree.isPartiallySelected(node)) {
          // this is a fully selected group value
          grandParent = false;
          for (c = 0; c < node.children.length; c++) {
            if (node.children[c].children) {
              grandParent = true;
              break;
            }
          }

          if (!grandParent) {
            completeParentNodes.push(node.id);
            currentChar.values.push({
              value: node._identifier,
              filter: false,
              visualize: true
            });
          }
        }
      } else {
        // leaf node: always filters, visualized if parent is not fully selected
        currentChar.values.push({
          value: node.id,
          shownValue: node._identifier,
          filter: true,
          visualize: completeParentNodes.indexOf(node.parentId) === -1
        });
      }
    }

    return result;
  },

  accept: function() {
    if (this.callback) {
      this.fireCallback(this.callback, 'value', [this.getValue()]);
    }
    this.hide();
  },

  clearValues: function() {
    this.tree.deselectAllRecords();
  },

  cancel: function() {
    this.hide();
  },

  initWidget: function() {
    var me = this,
      dataArrived,
      checkInitialNodes,
      getNodeByID;

    this.Super('initWidget', arguments);

    this.addAutoChild('mainLayout');

    this.selectionVisualization = isc.Label.create({
      contents: null
    });
    this.mainLayout.addMember(this.selectionVisualization);

    /**
     * Overrides dataArrived to initialize the tree initial selection
     * based on the filter initial criteria
     */
    dataArrived = function() {
      this.Super('dataArrived', arguments);
      if (
        this.topElement &&
        this.topElement.creator &&
        this.topElement.creator.internalValue
      ) {
        this.checkInitialNodes(this.topElement.creator.internalValue);
      }
      // Sort tree, so all values that arrived are sorted alphanumerically
      if (this.canSort) {
        this.sort();
      }
    };

    /**
     * Marks the checkboxes of the nodes that
     * are present in the initial criteria
     */
    checkInitialNodes = function(internalValue) {
      var c, v, value, node, characteristic;
      for (c in internalValue) {
        if (Object.prototype.hasOwnProperty.call(internalValue, c)) {
          characteristic = internalValue[c];
          for (v = 0; v < characteristic.values.length; v++) {
            value = characteristic.values[v];
            if (value.filter) {
              node = this.getNodeByID(value.value);
              if (node) {
                this.selectRecord(node);
              }
            }
          }
        }
      }
    };

    /**
     * Returns a tree node given its id
     */
    getNodeByID = function(nodeId) {
      var i,
        node,
        nodeList = this.data.getNodeList();
      for (i = 0; i < nodeList.length; i++) {
        node = nodeList[i];
        if (node.id === nodeId) {
          return node;
        }
      }
      return null;
    };

    this.tree = isc.TreeGrid.create({
      styleName: '',
      showHeader: false,
      autoFetchData: true,
      dataArrived: dataArrived,
      checkInitialNodes: checkInitialNodes,
      getNodeByID: getNodeByID,
      loadDataOnDemand: false,
      // loading the whole tree in a single request
      height: 200,
      showOpenIcons: false,
      showDropIcons: false,
      nodeIcon: null,
      folderIcon: null,
      openIconSuffix: 'open',
      selectionAppearance: 'checkbox',
      showSelectedStyle: false,
      showPartialSelection: true,
      cascadeSelection: true,
      selectionChanged: function() {
        me.fireOnPause(
          'updateCharacteristicsText',
          function() {
            //fire on pause because selecting a node raises several time selectionChanged to select its parants
            me.selectionVisualization.setContents(
              isc.OBCharacteristicsFilterItem.getDisplayValue(
                me.getValue()
              ).asHTML()
            );
          },
          100
        );
      },

      setDataSource: function(ds, fields) {
        ds.transformRequest = function(dsRequest) {
          dsRequest.params = dsRequest.params || {};
          if (
            me.creator &&
            me.creator.parentGrid &&
            me.creator.parentGrid.view &&
            me.creator.parentGrid.view.buttonOwnerView
          ) {
            dsRequest.params._buttonOwnerContextInfo = me.creator.parentGrid.view.buttonOwnerView.getContextInfo(
              false,
              true,
              true,
              false
            );
          }
          return this.Super('transformRequest', dsRequest);
        };

        var treeField;
        if (!fields || fields.length === 0) {
          treeField = isc.shallowClone(isc.TreeGrid.TREE_FIELD);
          treeField.escapeHTML = true;
          fields = [treeField];
        }

        ds.requestProperties.params._parentDSIdentifier = me.parentDSIdentifier;
        ds.requestProperties.params._propertyPath = me.propertyName;
        ds.requestProperties.params._selectorDefinition = me.selectorDefinition;

        if (me.processId) {
          ds.requestProperties.params._processId = me.processId;
        }

        return this.Super('setDataSource', [ds, fields]);
      }
    });

    OB.Datasource.get(
      'BE2735798ECC4EF88D131F16F1C4EC72',
      this.tree,
      null,
      true
    );

    this.mainLayout.addMember(this.tree);
    this.addAutoChild('buttonLayout');
    this.addAutoChild('okButton', {
      canFocus: true,
      title: OB.I18N.getLabel('OBUISC_Dialog.OK_BUTTON_TITLE')
    });
    this.addAutoChild('clearButton', {
      canFocus: true,
      title: OB.I18N.getLabel('OBUIAPP_Clear')
    });
    this.addAutoChild('cancelButton', {
      canFocus: true,
      title: OB.I18N.getLabel('OBUISC_Dialog.CANCEL_BUTTON_TITLE')
    });
    this.addItem(this.mainLayout);
  }
});

isc.ClassFactory.defineClass('OBCharacteristicsFilterItem', isc.OBTextItem);

isc.OBCharacteristicsFilterItem.addClassProperties({
  getDisplayValue: function(displayValue) {
    var c,
      characteristic,
      v,
      value,
      hasAny = false,
      result = '';

    for (c in displayValue) {
      if (Object.prototype.hasOwnProperty.call(displayValue, c)) {
        characteristic = displayValue[c];
        result += (hasAny ? '], ' : '') + characteristic.name + ':[';
        hasAny = true;

        for (v = 0; v < characteristic.values.length; v++) {
          value = characteristic.values[v];
          if (value.visualize) {
            result += (v > 0 ? ' - ' : '') + (value.shownValue || value.value);
          }
        }
      }
    }
    result += hasAny ? ']' : '';
    return result;
  }
});

isc.OBCharacteristicsFilterItem.addProperties({
  operator: 'exists',

  // Allow expressions so when multiple expressions (different characteristics
  // are selected) they are properly grouped and not stored in _extraAdvancedCriteria
  // in DynamicForm.setValuesAsCriteria method, being possible in this way to properly
  // clear filters. See issue #24739
  allowExpressions: true,
  canEdit: false,
  disableIconsOnReadOnly: false,
  hqlExists:
    'exists (from ProductCharacteristicValue v where {productInEntity} = v.product and v.characteristicValue.id in ($value))',
  showPickerIcon: false,
  filterDialogConstructor: isc.OBCharacteristicsFilterDialog,
  propertyName: null,
  pickerIconDefaults: {
    name: 'showDateRange',
    src:
      OB.Styles.skinsPath +
      'Default/org.openbravo.client.application/images/form/productCharacteristicsFilter_ico.png',
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

  setCriterion: function(criterion) {
    if (criterion && criterion.internalValue) {
      this.internalValue = criterion.internalValue;
    }
  },

  /**
   * Criterion obtained queries the text field with the concatenation of all characteristics.
   *
   * It might be changed to query actual table of characteristic values, but this would make it
   * not usable in other views than Product
   */
  getCriterion: function() {
    var c, characteristic, v, value, charCriteria, inValues;
    if (!this.internalValue) {
      return;
    }

    var result;
    result = {
      _constructor: 'AdvancedCriteria',
      operator: 'and',
      internalValue: this.internalValue,
      isProductCharacteristicsCriteria: true,
      criteria: []
    };

    for (c in this.internalValue) {
      if (Object.prototype.hasOwnProperty.call(this.internalValue, c)) {
        characteristic = this.internalValue[c];

        inValues = [];
        for (v = 0; v < characteristic.values.length; v++) {
          value = characteristic.values[v];
          if (value.filter) {
            inValues.push(value.value);
          }
        }

        charCriteria = {
          operator: 'exists',
          fieldName: this.getCriteriaFieldName(),
          value: inValues
        };

        charCriteria.existsQuery = this.hqlExists;
        result.criteria.push(charCriteria);
      }
    }

    return result;
  },

  setValue: function(value) {
    this.Super(
      'setValue',
      isc.OBCharacteristicsFilterItem.getDisplayValue(this.internalValue)
    );
  },

  /**
   * Reusing same method as in OBMiniDateRangeItem. It is invoked when filter is removed
   * from grid.
   */
  clearFilterValues: function() {
    this.filterDialog.tree.deselectAllRecords();
    delete this.internalValue;
  },

  filterDialogCallback: function(value) {
    // Whenever filter is changed, new criteria must force a backend call, adaptive
    // filter cannot be used for characteristics as the information to do the matching
    // is not present in client. Cache of localData needs to be cleaned up to force it;
    // if not, this criteria can be considered to be more restrictive without even
    // executing compare criteria method in case the whole page was originally retrieved
    // without any criteria.
    // See issue #24750
    if (this.grid.parentElement.data) {
      this.grid.parentElement.data.localData = null;
      this.grid.parentElement.data.allRows = null;
    }

    this.internalValue = value;
    this.setElementValue(
      isc.OBCharacteristicsFilterItem.getDisplayValue(value)
    );
    this.form.grid.performAction();
  },

  init: function() {
    var propertyPath,
      i,
      parentDSIdentifier = null,
      selectorDefinition = null,
      filterDialogProperties;
    this.canEdit = false;

    // Getting the product property in the entity we are filtering it.
    // It is obtained based on fieldName, in case its path is compound (i.e.
    // product$characteristicDescription), path is included up to the element
    // previous to the last one
    if (
      this.grid &&
      this.grid.parentElement &&
      this.grid.parentElement.viewProperties &&
      this.grid.parentElement.viewProperties.gridProperties &&
      this.grid.parentElement.viewProperties.gridProperties.alias
    ) {
      this.propertyName = this.grid.parentElement.viewProperties.gridProperties.alias;
      if (
        !this.isProductEntity() &&
        !this.isPropertyPathFromProduct(this.getFieldName())
      ) {
        this.propertyName += '.product';
      }
    } else {
      this.propertyName = 'e'; // "e" is the base entity
    }
    propertyPath = this.getFieldName().split(OB.Constants.FIELDSEPARATOR);
    for (i = 0; i < propertyPath.length - 1; i++) {
      this.propertyName += '.' + propertyPath[i];
    }
    this.hqlExists = this.hqlExists.replace(
      '{productInEntity}',
      this.propertyName
    );

    if (this.grid.parentElement.view && this.grid.parentElement.view.viewGrid) {
      this.parentGrid = this.grid.parentElement.view.viewGrid;
    } else {
      this.parentGrid = this.grid.parentElement;
    }

    if (
      this.parentGrid
        .getDataSource()
        .dataURL.indexOf('org.openbravo.service.datasource') !== -1
    ) {
      parentDSIdentifier = this.parentGrid
        .getDataSource()
        .dataURL.substr(
          this.parentGrid.getDataSource().dataURL.lastIndexOf('/') + 1
        );
    }

    if (this.parentGrid.selector) {
      // parent grid is a selector
      selectorDefinition = this.parentGrid.selector.selectorDefinitionId;
    }

    filterDialogProperties = {
      title: this.title,
      callback: this.getID() + '.filterDialogCallback(value)',
      parentDSIdentifier: parentDSIdentifier,
      selectorDefinition: selectorDefinition,
      propertyName: this.propertyName
    };

    if (this.isPickAndExecute()) {
      filterDialogProperties.processId = this.parentGrid.view.processId;
    }

    this.addAutoChild('filterDialog', filterDialogProperties);

    this.icons = [
      isc.addProperties(
        {
          prompt: this.pickerIconPrompt
        },
        this.pickerIconDefaults,
        this.pickerIconProperties
      )
    ];

    this.Super('init', arguments);
  },

  isPickAndExecute: function() {
    if (
      this.parentGrid &&
      this.parentGrid.view &&
      this.parentGrid.view.uiPattern === 'OBUIAPP_PickAndExecute'
    ) {
      return true;
    }
    return false;
  },

  isProductEntity: function() {
    var entity, theGrid;

    if (this.grid && !this.grid.sourceWidget) {
      return false; // can not retrieve entity
    }
    theGrid = this.grid.sourceWidget;
    if (theGrid.view && theGrid.view.entity) {
      // Standard view
      entity = theGrid.view.entity;
    } else if (theGrid.viewProperties && theGrid.viewProperties.entity) {
      // Pick and Edit view
      entity = theGrid.viewProperties.entity;
    }
    return entity === 'Product';
  },

  isPropertyPathFromProduct: function(propertyName) {
    return propertyName.startsWith('product' + OB.Constants.FIELDSEPARATOR);
  },

  removeProductCharacteristicsCriteria: function(fullCriteria) {
    var newCriteria = isc.shallowClone(fullCriteria);
    if (
      fullCriteria.criteria &&
      fullCriteria.criteria.find('isProductCharacteristicsCriteria', true)
    ) {
      newCriteria.criteria.remove(
        newCriteria.criteria.find('isProductCharacteristicsCriteria', true)
      );
    }
    return newCriteria;
  },

  showDialog: function() {
    if (this.showFkDropdownUnfiltered) {
      this.filterDialog.show();
    } else {
      var criteria = this.removeProductCharacteristicsCriteria(
        this.parentGrid.getCriteria()
      );
      this.filterDialog.tree.fetchData(criteria);
      this.filterDialog.show();
    }
  },

  destroy: function() {
    this.filterDialog.destroy();
    this.filterDialog = null;
    this.Super('destroy', arguments);
  }
});

// == OBCharacteristicsGridItem ==
// If the Form Item used when editing in grid characteristics, it is an OBTextItem
// but its value is not complete (js object with information about all characteristics)
// but just database value.
isc.ClassFactory.defineClass('OBCharacteristicsGridItem', isc.OBTextItem);

isc.OBCharacteristicsGridItem.addProperties({
  setValue: function(value) {
    // forget about complex object value and use just what is in DB
    if (!value || !value.characteristics || !value.dbValue) {
      this.Super('setValue', arguments);
      return;
    }

    this.setValue(value.dbValue);
  }
});
