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
 * All portions are Copyright (C) 2015-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

isc.defineClass('OBBaseParameterWindowView', isc.VLayout);

// == OBBaseParameterWindowView ==
//   OBBaseParameterWindowView is the base view that it can be extended by
// any process that use parameters defined in OBUIAPP_Parameter
isc.OBBaseParameterWindowView.addProperties({
  // Set default properties for the OBPopup container
  showMinimizeButton: true,
  showMaximizeButton: true,
  popupWidth: '90%',
  popupHeight: '90%',
  // Set later inside initWidget
  firstFocusedItem: null,
  // Set later by implementations of this class
  defaultsActionHandler: null,

  // Set now pure P&E layout properties
  width: '100%',
  height: '100%',
  overflow: 'auto',
  autoSize: false,

  toolBarLayout: null,
  members: [],
  baseParams: {},
  formProps: {},

  // allows to calculate extra context info (ie. when invoking from menu)
  additionalContextInfo: {},

  initWidget: function() {
    var i,
      field,
      items = [],
      buttonLayout = [],
      view = this,
      newShowIf,
      context,
      updatedExpandSection,
      params;

    // this flag can be used by Selenium to determine when defaults are set
    this.defaultsAreSet = false;

    buttonLayout = view.buildButtonLayout();

    if (!this.popup) {
      this.toolBarLayout = isc.OBToolbar.create({
        view: this,
        leftMembers: [{}],
        rightMembers: buttonLayout
      });
      // this.toolBarLayout.addMems(buttonLayout);
      this.members.push(this.toolBarLayout);
    }

    this.messageBar = isc.OBMessageBar.create({
      visibility: 'hidden',
      view: this,
      show: function() {
        var showMessageBar = true;
        this.Super('show', arguments);
        view.resized(showMessageBar);
      },
      hide: function() {
        var showMessageBar = false;
        this.Super('hide', arguments);
        view.resized(showMessageBar);
      }
    });
    this.members.push(this.messageBar);

    newShowIf = function(item, value, form, values) {
      var currentValues,
        originalShowIfValue = false,
        parentContext;

      currentValues = isc.shallowClone(values) || {};
      if (isc.isA.emptyObject(currentValues) && form && form.view) {
        currentValues = isc.shallowClone(form.view.getCurrentValues());
      } else if (isc.isA.emptyObject(currentValues) && form && form.getValues) {
        currentValues = isc.shallowClone(form.getValues());
      }
      OB.Utilities.fixNull250(currentValues);
      parentContext = this.view.getUnderLyingRecordContext(
        false,
        true,
        true,
        true
      );

      try {
        if (isc.isA.Function(this.originalShowIf)) {
          originalShowIfValue = this.originalShowIf(
            item,
            value,
            form,
            currentValues,
            parentContext
          );
        } else {
          originalShowIfValue = isc.JSON.decode(this.originalShowIf);
        }
      } catch (_exception) {
        isc.warn(
          _exception + ' ' + _exception.message + ' ' + _exception.stack
        );
      }
      if (originalShowIfValue && item.getType() === 'OBPickEditGridItem') {
        // load the grid if it is being shown for the first time
        if (
          item.canvas &&
          item.canvas.viewGrid &&
          !isc.isA.ResultSet(item.canvas.viewGrid.data)
        ) {
          if (
            item.defaultFilter !== null &&
            !isc.isA.emptyObject(item.defaultFilter)
          ) {
            // if it has a default filter, apply it and use it when filtering
            item.canvas.viewGrid.setFilterEditorCriteria(item.defaultFilter);
            item.canvas.viewGrid.filterByEditor();
          } else {
            // if it does not have a default filter, just refresh the grid
            item.canvas.viewGrid.refreshGrid();
          }
        }
      }
      if (this.view && this.view.theForm) {
        this.view.theForm.markForRedraw();
      }
      return originalShowIfValue;
    };
    // this function is only used in OBSectionItems that are collapsed originally
    // this is done to force the data fetch of its stored OBPickEditGridItems
    updatedExpandSection = function() {
      var i, itemName, item;
      this.originalExpandSection();
      for (i = 0; i < this.itemIds.length; i++) {
        itemName = this.itemIds[i];
        item = this.form.getItem(itemName);
        if (
          item.type === 'OBPickEditGridItem' &&
          !isc.isA.ResultSet(item.canvas.viewGrid.data)
        ) {
          item.canvas.viewGrid.fetchData(item.canvas.viewGrid.getCriteria());
        }
      }
    };

    // Parameters
    if (this.viewProperties.fields) {
      for (i = 0; i < this.viewProperties.fields.length; i++) {
        field = this.viewProperties.fields[i];
        field = isc.addProperties(
          {
            view: this
          },
          field
        );

        if (field.showIf) {
          field.originalShowIf = field.showIf;
          field.showIf = newShowIf;
        }
        if (field.onChangeFunction) {
          // the default
          field.onChangeFunction.sort = 50;

          OB.OnChangeRegistry.register(
            this.viewId || this.processId,
            field.name,
            field.onChangeFunction,
            'default'
          );
        }

        if (field.type === 'OBSectionItem' && !field.sectionExpanded) {
          // modifies the expandSection function of OBSectionItems collapsed originally to avoid having
          // unloaded grids when a section is expanded for the first time
          field.originalExpandSection = isc.OBSectionItem.getPrototype().expandSection;
          field.expandSection = updatedExpandSection;
        }
        items.push(field);
      }

      if (items.length !== 0) {
        // create form if there are items to include
        this.formProps.paramWindow = this;
        this.theForm = isc.OBParameterWindowForm.create(this.formProps);
        // If there is only one paremeter, it is a grid and the window is opened in a popup, then the window is a P&E window
        if (
          items &&
          items.length === 1 &&
          items[0].type === 'OBPickEditGridItem' &&
          this.popup
        ) {
          this.isPickAndExecuteWindow = true;
        }
        this.theForm.setItems(items);
        this.theForm.setFieldSections();
        this.formContainerLayout = isc.OBFormContainerLayout.create({});
        this.formContainerLayout.addMember(this.theForm);
        this.members.push(this.formContainerLayout);
      }
    }

    if (this.popup) {
      this.popupButtons = isc.OBFormContainerLayout.create({
        defaultLayoutAlign: 'center',
        align: 'center',
        width: '100%',
        height: OB.Styles.Process.PickAndExecute.buttonLayoutHeight,
        members: [
          isc.HLayout.create({
            width: 1,
            overflow: 'visible',
            styleName: this.buttonBarStyleName,
            height: this.buttonBarHeight,
            defaultLayoutAlign: 'center',
            members: buttonLayout
          })
        ]
      });
      this.members.push(this.popupButtons);
      this.closeClick = function() {
        this.closeClick = function() {
          return true;
        }; // To avoid loop when "Super call"
        this.enableParentViewShortcuts(); // restore active view shortcuts before closing
        if (this.isExpandedRecord) {
          this.callerField.grid.collapseRecord(this.callerField.record);
        } else {
          this.parentElement.parentElement.closeClick(); // Super call
        }
      };
    }
    this.loading = OB.Utilities.createLoadingLayout(
      OB.I18N.getLabel('OBUIAPP_PROCESSING')
    );
    this.loading.hide();
    this.members.push(this.loading);
    this.Super('initWidget', arguments);

    params = isc.shallowClone(this.baseParams);
    context = this.getUnderLyingRecordContext(false, true, true, true);

    // allow to add external parameters
    isc.addProperties(context, this.externalParams);

    if (
      this.callerField &&
      this.callerField.view &&
      this.callerField.view.getContextInfo
    ) {
      isc.addProperties(
        context || {},
        this.callerField.view.getContextInfo(true /*excludeGrids*/)
      );
    }

    this.disableParentViewShortcuts();

    params.windowId = this.windowId;
    OB.RemoteCallManager.call(
      this.defaultsActionHandler,
      context,
      params,
      function(rpcResponse, data, rpcRequest) {
        if (
          data &&
          data.message &&
          data.message.severity === isc.OBMessageBar.TYPE_ERROR
        ) {
          view.handleErrorState(data.message);
        } else {
          view.handleDefaults(data);
        }
      }
    );
  },

  /*
   * Function that creates the layout with the buttons. Classes implementing OBBaseParameterWindowView
   * have to override this function to add the needed buttons.
   */
  buildButtonLayout: function() {
    return [];
  },

  disableFormItems: function() {
    var i, params;
    if (this.theForm && this.theForm.getItems) {
      params = this.theForm.getItems();
      for (i = 0; i < params.length; i++) {
        if (params[i].disable) {
          params[i].disable();
        }
      }
    }
  },

  // dummy required by OBStandardView.prepareGridFields
  setFieldFormProperties: function() {},

  validate: function() {
    var isValid;
    if (this.theForm) {
      isValid = this.theForm.validate();
      if (!isValid) {
        return isValid;
      }
    }
    return true;
  },

  showProcessing: function(processing) {
    if (processing) {
      if (this.theForm) {
        this.theForm.hide();
      }
      if (this.popupButtons) {
        this.popupButtons.hide();
      }
      this.hideToolBarLayoutChildren();
      this.loading.show();
    } else {
      if (this.theForm) {
        this.theForm.show();
      }

      this.loading.hide();
    }
  },

  hideToolBarLayoutChildren: function() {
    var i;
    if (this.toolBarLayout) {
      for (i = 0; i < this.toolBarLayout.children.length; i++) {
        if (this.toolBarLayout.children[i].hide) {
          this.toolBarLayout.children[i].hide();
        }
      }
    }
  },

  // Checks params with readonly logic enabling or disabling them based on it
  handleReadOnlyLogic: function() {
    var form, fields, i, field, parentContext;

    form = this.theForm;
    if (!form) {
      return;
    }
    parentContext = this.getUnderLyingRecordContext(false, true, true, true);

    fields = form.getFields();
    for (i = 0; i < fields.length; i++) {
      field = form.getField(i);
      if (field.readOnlyIf && field.setDisabled) {
        field.setDisabled(field.readOnlyIf(form.getValues(), parentContext));
      }
    }
  },

  handleDisplayLogicForGridColumns: function() {
    var form, fields, i, field;

    form = this.theForm;
    if (!form) {
      return;
    }

    fields = form.getFields();
    for (i = 0; i < fields.length; i++) {
      field = form.getField(i);
      if (field.canvas) {
        if (field.canvas.viewGrid) {
          field.canvas.viewGrid.evaluateDisplayLogicForGridColumns();
        }
      }
    }
  },

  handleErrorState: function(message) {
    // Disable the parameter view elements
    this.disableFormItems();
    if (this.theForm) {
      this.theForm.disable();
    }
    // Hide the buttons (if any)
    this.hideToolBarLayoutChildren();
    if (this.popupButtons && this.popupButtons.hide) {
      this.popupButtons.hide();
    }
    if (!message) {
      return;
    }
    // Show the error message
    if (message.title) {
      this.messageBar.setMessage(message.severity, message.title, message.text);
    } else {
      this.messageBar.setMessage(
        message.severity,
        OB.I18N.getLabel('OBUIAPP_Error'),
        message.text
      );
    }
  },

  handleDefaults: function(result) {
    var i,
      field,
      def,
      defaults = result.defaults,
      filterExpressions = result.filterExpressions,
      defaultFilter = {},
      gridsToBeFiltered = [];
    if (!this.theForm) {
      if (this.onLoadFunction) {
        this.onLoadFunction(this);
      }
      return;
    }

    for (i in defaults) {
      if (Object.prototype.hasOwnProperty.call(defaults, i)) {
        def = defaults[i];
        field = this.theForm.getItem(i);
        if (field) {
          if (isc.isA.Object(def)) {
            if (def.identifier && def.value) {
              field.valueMap = field.valueMap || {};
              field.valueMap[def.value] = def.identifier;
              field.setValue(def.value);
            }
          } else {
            field.setValue(this.getTypeSafeValue(field.typeInstance, def));
          }
        }
      }
    }
    for (i in filterExpressions) {
      if (Object.prototype.hasOwnProperty.call(filterExpressions, i)) {
        field = this.theForm.getItem(i);
        defaultFilter = {};
        isc.addProperties(defaultFilter, filterExpressions[i]);
        field.setDefaultFilter(defaultFilter);
        if (field.isVisible() && !field.showIf) {
          field.canvas.viewGrid.setFilterEditorCriteria(defaultFilter);
          gridsToBeFiltered.push(field.canvas.viewGrid);
        }
      }
    }

    if (this.onLoadFunction) {
      this.onLoadFunction(this);
    }

    // filter after applying the onLoadFunction, just in case it has modified the filter editor criteria of a grid.
    // this way it a double requests for these grids is avoided
    for (i = 0; i < gridsToBeFiltered.length; i++) {
      gridsToBeFiltered[i].filterByEditor();
    }

    this.handleReadOnlyLogic();
    this.handleButtonsStatus();

    // redraw to execute display logic
    this.theForm.markForRedraw();
    this.handleDisplayLogicForGridColumns();

    // this flag can be used by Selenium to determine when defaults are set
    this.defaultsAreSet = true;
  },

  getContextInfo: function(excludeGrids) {
    var result = {},
      params,
      i;
    if (!this.theForm) {
      return result;
    }

    if (this.theForm && this.theForm.getItems) {
      params = this.theForm.getItems();
      for (i = 0; i < params.length; i++) {
        if (excludeGrids && params[i].type === 'OBPickEditGridItem') {
          continue;
        }
        result[params[i].name] = params[i].getValue();
      }
    }

    return result;
  },

  getUnderLyingRecordContext: function(
    onlySessionProperties,
    classicMode,
    forceSettingContextVars,
    convertToClassicFormat
  ) {
    var ctxInfo =
      (this.buttonOwnerView &&
        this.buttonOwnerView.getContextInfo(
          onlySessionProperties,
          classicMode,
          forceSettingContextVars,
          convertToClassicFormat
        )) ||
      {};
    return isc.addProperties(ctxInfo, this.additionalContextInfo);
  },

  /**
   * Given a value, it returns the proper value according to the provided type
   */
  getTypeSafeValue: function(type, value) {
    var isNumber;
    if (!type) {
      return value;
    }
    isNumber =
      isc.SimpleType.inheritsFrom(type, 'integer') ||
      isc.SimpleType.inheritsFrom(type, 'float');
    if (isNumber && isc.isA.Number(value)) {
      return value;
    } else if (
      isNumber &&
      OB.Utilities.Number.IsValidValueString(type, value)
    ) {
      return OB.Utilities.Number.OBMaskedToJS(
        value,
        type.decSeparator,
        type.groupSeparator
      );
    } else if (
      isNumber &&
      isc.isA.Number(OB.Utilities.Number.OBMaskedToJS(value, '.', ','))
    ) {
      // it might happen that default value uses the default '.' and ',' as decimal and group separator
      return OB.Utilities.Number.OBMaskedToJS(value, '.', ',');
    } else {
      return value;
    }
  },

  setAllButtonEnabled: function(enabled) {
    if (this.isReport) {
      if (this.pdfExport) {
        this.pdfButton.setEnabled(enabled);
      }
      if (this.xlsExport) {
        this.xlsButton.setEnabled(enabled);
      }
      if (this.htmlExport) {
        this.htmlButton.setEnabled(enabled);
      }
    } else {
      if (this.okButton) {
        this.okButton.setEnabled(enabled);
      }
    }
  },

  handleButtonsStatus: function() {
    var allRequiredSet = this.allRequiredParametersSet();
    this.setAllButtonEnabled(allRequiredSet);
  },

  // returns true if any non-grid required parameter does not have a value
  allRequiredParametersSet: function() {
    var i,
      item,
      length = this.theForm && this.theForm.getItems().length,
      value;
    for (i = 0; i < length; i++) {
      item = this.theForm.getItems()[i];
      value = item.getValue();
      // Multiple selectors value is an array, check that it is not empty
      if (item.editorType === 'OBMultiSelectorItem' && value.length === 0) {
        value = null;
      }
      // do not take into account the grid parameters when looking for required parameters without value
      if (
        item.type !== 'OBPickEditGridItem' &&
        item.required &&
        item.isVisible() &&
        value !== false &&
        value !== 0 &&
        !value
      ) {
        return false;
      }
    }
    return true;
  },

  getParentActiveView: function() {
    if (this.buttonOwnerView && this.buttonOwnerView.standardWindow) {
      return this.buttonOwnerView.standardWindow.activeView;
    }
    return null;
  },

  disableParentViewShortcuts: function() {
    var activeView = this.getParentActiveView();
    if (activeView && activeView.viewGrid && activeView.toolBar) {
      activeView.viewGrid.disableShortcuts();
      activeView.toolBar.disableShortcuts();
    }
  },

  enableParentViewShortcuts: function() {
    var activeView = this.getParentActiveView();
    if (activeView && activeView.viewGrid && activeView.toolBar) {
      activeView.viewGrid.enableShortcuts();
      activeView.toolBar.enableShortcuts();
    }
  },

  getBookMarkParams: function() {
    var result = {};
    result.viewId = this.getClassName();
    result.tabTitle = this.tabTitle;
    return result;
  },

  isSameTab: function(viewName, params) {
    // process definition based windows can be opened in more than one tab at the same time
    return false;
  }
});
