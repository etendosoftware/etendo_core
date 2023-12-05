/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distribfuted  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2010-2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

isc.ClassFactory.defineClass('OBFormContainerLayout', isc.VLayout);

isc.OBFormContainerLayout.addProperties({
  width: '100%',
  height: '*',
  overflow: 'auto'
});

// = OBStandardView =
//
// An OBStandardView represents a single Openbravo tab. An OBStandardView consists
// of three parts:
// 1) a grid an instance of an OBViewGrid (property: viewGrid)
// 2) a form an instance of an OBViewForm (property: viewForm)
// 3) a tab set with child OBStandardView instances (property: childTabSet)
//
// In addition an OBStandardView has components for a message bar and other visualization.
//
// A standard view can be opened as a result of a direct link from another window/tab. See
// the description in ob-standard-window for the flow in that case.
//
isc.ClassFactory.defineClass('OBStandardView', isc.VLayout);

isc.OBStandardView.addClassProperties({
  // the part in the top is maximized, meaning
  STATE_TOP_MAX: 'TopMax',

  // that the tabset in the bottom is minimized
  // the tabset part is maximized, the
  STATE_BOTTOM_MAX: 'BottomMax',

  // the top has height 0
  // the view is split in the middle, the top part has
  STATE_MID: 'Mid',

  // 50%, the tabset also
  // state of the tabset which is shown in the middle,
  STATE_IN_MID: 'InMid',

  // the parent of the tabset has state
  // isc.OBStandardView.STATE_MID
  // minimized state, the parent has
  STATE_MIN: 'Min',

  // isc.OBStandardView.STATE_TOP_MAX or
  // isc.OBStandardView.STATE_IN_MID
  // the inactive state does not show an orange hat on the tab button
  MODE_INACTIVE: 'Inactive',

  UI_PATTERN_READONLY: 'RO',
  UI_PATTERN_SINGLERECORD: 'SR',
  UI_PATTERN_EDITORDELETEONLY: 'ED',
  UI_PATTERN_STANDARD: 'ST'
});

isc.OBStandardView.addProperties({
  // properties used by the ViewManager, only relevant in case this is the
  // top
  // view shown directly in the main tab
  showsItself: false,
  tabTitle: null,

  // ** {{{ windowId }}} **
  // The id of the window shown here, only set for the top view in the
  // hierarchy
  // and if this is a window/tab view.
  windowId: null,

  // ** {{{ tabId }}} **
  // The id of the tab shown here, set in case of a window/tab view.
  tabId: null,

  // ** {{{ processId }}} **
  // The id of the process shown here, set in case of a process view.
  processId: null,

  // ** {{{ formId }}} **
  // The id of the form shown here, set in case of a form view.
  formId: null,

  // ** {{{ parentView }}} **
  // The parentView if this view is a child in a parent child structure.
  parentView: null,

  // ** {{{ parentTabSet }}} **
  // The tabSet which shows this view. If the parentView is null then this
  // is the
  // top tabSet.
  parentTabSet: null,
  tab: null,

  // ** {{{ toolbar }}} **
  // The toolbar canvas.
  toolBar: null,

  messageBar: null,

  // ** {{{ formGridLayout }}} **
  // The layout which holds the form and grid.
  formGridLayout: null,

  // ** {{{ childTabSet }}} **
  // The tabSet holding the child tabs with the OBView instances.
  childTabSet: null,

  // ** {{{ hasChildTabs }}} **
  // Is set to true if there are child tabs.
  hasChildTabs: false,

  // ** {{{ dataSource }}} **
  // The dataSource used to fill the data in the grid/form.
  dataSource: null,

  // ** {{{ viewForm }}} **
  // The viewForm used to display single records
  viewForm: null,

  // ** {{{ viewGrid }}} **
  // The viewGrid used to display multiple records
  viewGrid: null,

  // ** {{{ parentProperty }}} **
  // The name of the property refering to the parent record, if any
  parentProperty: null,

  // ** {{{ targetRecordId }}} **
  // The id of the record to initially show.
  targetRecordId: null,

  // ** {{{ entity }}} **
  // The entity to show.
  entity: null,

  width: '100%',
  height: '100%',
  margin: 0,
  padding: 0,
  overflow: 'hidden',

  // set if one record has been selected
  lastRecordSelected: null,
  lastRecordSelectedCount: 0,
  fireOnPauseDelay: 200,

  // ** {{{ refreshContents }}} **
  // Should the contents listgrid/forms be refreshed when the tab
  // gets selected and shown to the user.
  refreshContents: true,

  state: isc.OBStandardView.STATE_MID,
  previousState: isc.OBStandardView.STATE_TOP_MAX,

  // last item in the filtergrid or the form which had focus
  // when the view is activated it will set focus here
  lastFocusedItem: null,

  // initially set to true, is set to false after the
  // first time default edit mode is opened or a new parent
  // is selected.
  // note that opening the edit view is done in the viewGrid.dataArrived
  // method
  allowDefaultEditMode: true,

  readOnly: false,
  singleRecord: false,
  editOrDeleteOnly: false,

  isShowingForm: false,
  isEditingGrid: false,

  propertyToColumns: [],

  isShowingTree: false,

  initWidget: function(properties) {
    var length,
      rightMemberButtons = [],
      leftMemberButtons = [],
      i,
      actionButton,
      preferenceValue;

    if (this.isRootView) {
      this.buildStructure();
    }

    OB.TestRegistry.register(
      'org.openbravo.client.application.View_' + this.tabId,
      this
    );
    OB.TestRegistry.register(
      'org.openbravo.client.application.ViewGrid_' + this.tabId,
      this.viewGrid
    );
    OB.TestRegistry.register(
      'org.openbravo.client.application.ViewForm_' + this.tabId,
      this.viewForm
    );

    if (this.showTabIf && !this.originalShowTabIf) {
      this.originalShowTabIf = this.showTabIf;
      this.showTabIf = function(context) {
        var originalShowTabIfValue = false;

        try {
          if (isc.isA.Function(this.originalShowTabIf)) {
            originalShowTabIfValue = this.originalShowTabIf(context);
          } else {
            originalShowTabIfValue = isc.JSON.decode(this.originalShowTabIf);
          }
        } catch (_exception) {
          isc.warn(
            _exception + ' ' + _exception.message + ' ' + _exception.stack
          );
        }
        return originalShowTabIfValue;
      };
    }

    // If the tab comes with session attributes (preference attributes used in the display
    // logic of the tab, see issue https://issues.openbravo.com/view.php?id=5202), assign them
    // to the form, so they will be retrieved when getContextInfo() is called for the form
    if (this.sessionAttributesNames) {
      this.preferenceValues = {};
      for (i = 0; i < this.sessionAttributesNames.length; i++) {
        preferenceValue = OB.PropertyStore.get(
          this.sessionAttributesNames[i],
          this.standardWindow.windowId
        );
        if (preferenceValue !== null) {
          this.preferenceValues[
            this.sessionAttributesNames[i]
          ] = preferenceValue;
        }
      }
    }

    if (this.actionToolbarButtons) {
      length = this.actionToolbarButtons.length;
      for (i = 0; i < length; i++) {
        actionButton = isc.OBToolbarActionButton.create(
          this.actionToolbarButtons[i]
        );
        actionButton.contextView = this;
        rightMemberButtons.push(actionButton);
      }
    }

    // Look for specific toolbar buttons for this tab
    if (this.iconToolbarButtons) {
      length = this.iconToolbarButtons.length;
      for (i = 0; i < length; i++) {
        const button = this.iconToolbarButtons[i];
        if (button.isProcessDefinition) {
          button.newDefinition = true;
          button.windowId = this.windowId;
          button.baseStyle = 'OBToolbarIconButton_icon_' + button.buttonType + ' OBToolbarIconButton';
          button.width = 51;
        }

        // note create a somewhat unique id by concatenating the tabid and the index
        OB.ToolbarRegistry.registerButton(
          this.tabId + '_' + i,
          button.isProcessDefinition ? isc.OBToolbarActionButton : isc.OBToolbarIconButton,
          button,
          200 + i * 10,
          this.tabId,
          null,
          false
        );
      }
    }

    this.toolBar = isc.OBToolbar.create({
      view: this,
      visibility: 'hidden',
      leftMembers: leftMemberButtons,
      rightMembers: rightMemberButtons
    });

    this.Super('initWidget', arguments);

    this.toolBar.updateButtonState(true, false, true);

    // Update the subtab visibility before the tabs are shown to the client
    this.handleDefaultTreeView();
    this.updateSubtabVisibility();
  },

  // updates some view properties based on its uiPattern
  updateViewBasedOnUiPattern: function() {
    // this.standardWindow.getClass().uiPattern will only exists after setWindowSettings is executed
    if (this.standardWindow.getClass().uiPattern) {
      this.setReadOnly(
        this.standardWindow.getClass().uiPattern[this.tabId] ===
          isc.OBStandardView.UI_PATTERN_READONLY
      );
      this.setSingleRecord(
        this.standardWindow.getClass().uiPattern[this.tabId] ===
          isc.OBStandardView.UI_PATTERN_SINGLERECORD
      );
      this.setEditOrDeleteOnly(
        this.standardWindow.getClass().uiPattern[this.tabId] ===
          isc.OBStandardView.UI_PATTERN_EDITORDELETEONLY
      );
    }
  },

  show: function() {
    this.Super('show', arguments);
  },

  destroy: function() {
    // destroy the datasource
    if (this.dataSource) {
      this.dataSource.destroy();
      this.dataSource = null;
    }

    // destroy notes datasource
    if (this.notesDataSource) {
      this.notesDataSource.destroy();
      this.notesDataSource = null;
    }

    // destroy view form
    if (this.viewForm) {
      this.viewForm.destroy();
      this.viewForm = null;
    }

    // destroy view grid
    if (this.viewGrid) {
      this.viewGrid.destroy();
      this.viewGrid = null;
    }
    return this.Super('destroy', arguments);
  },

  prepareViewForm: function() {
    var personalizationData = {};

    if (!this.viewForm) {
      return;
    }

    // setDataSource executes setFields which replaces the current fields
    // We don't want to destroy the associated DataSource objects
    this.viewForm.destroyItemObjects = false;

    // is used to keep track of the original simple objects
    // used to create fields
    this.viewForm._originalFields = isc.clone(this.formFields);
    this.viewForm.fields = this.formFields;
    this.viewForm.firstFocusedField = this.firstFocusedField;

    this.viewForm.setDataSource(this.dataSource, this.formFields);
    this.viewForm.isViewForm = true;
    this.viewForm.destroyItemObjects = true;

    personalizationData = this.getFormPersonalization(true);
    if (personalizationData && personalizationData.form) {
      OB.Personalization.personalizeForm(personalizationData, this.viewForm);
    }
    this.setMaximizeRestoreButtonState();
    // The tabIndex of the child tabs will be recalculated to have a higher index number than the tabIndex of their parent elements.
    if (this.viewForm.view.childTabSet) {
      this.viewForm.parentElement.updateMemberTabIndex(
        this.viewForm.view.childTabSet
      );
    }
  },

  buildStructure: function() {
    var lazyFiltering;
    this.createMainParts();
    this.createViewStructure();
    if (this.childTabSet && this.childTabSet.tabs.length === 0) {
      this.hasChildTabs = false;
      this.activeGridFormMessageLayout.setHeight('100%');
    }
    this.dataSource.view = this;

    // In case of grid configuration, apply it now. In this way:
    //   -No extra fetch is done for root tab
    //   -Grid is not rendered twice (one for the standard confing and another
    //    one for the saved config)
    if (
      this.standardWindow &&
      this.standardWindow.viewState &&
      this.standardWindow.viewState[this.tabId]
    ) {
      this.viewGrid.setViewState(
        this.standardWindow.viewState[this.tabId],
        true
      );
      // lastViewApplied is set because there are modifications in grid, so not
      // marking "Standard View" in view's menu
      this.standardWindow.lastViewApplied = true;
    }

    if (this.viewGrid) {
      lazyFiltering = this.viewGrid.lazyFiltering;
    }

    // directTabInfo is set when we are in direct link mode, i.e. directly opening
    // a specific tab with a record, the direct link logic will already take care
    // of fetching data
    if (this.isRootView && !this.standardWindow.directTabInfo) {
      if (!lazyFiltering && !this.deferOpenNewEdit) {
        if (!this.standardWindow.checkIfDefaultSavedView()) {
          this.viewGrid.fetchData(this.viewGrid.getCriteria());
        } else {
          this.dataLoadDelayedForDefaultSavedView = true;
        }
      }
      this.refreshContents = false;
    }

    if (this.isRootView) {
      if (this.childTabSet) {
        this.childTabSet.setState(isc.OBStandardView.STATE_IN_MID);
        this.childTabSet.selectTab(this.childTabSet.tabs[0]);
        OB.TestRegistry.register(
          'org.openbravo.client.application.ChildTabSet_' + this.tabId,
          this.viewForm
        );
      }
    }

    if (this.defaultEditMode) {
      // prevent the grid from showing very shortly, so hide it right away
      this.viewGrid.hide();
    }
  },

  // ** {{{ handleDefaultTreeView }}} **
  //
  // Evaluates the 'Default Tree View Logic' to show the grid view or the tree view
  //
  // Parameters:
  // * {{{handleCurrent}}}: 'false' by default. It specifies if the logic should be applied in current record.
  // * {{{handleChilds}}}: 'true' by default. It specifies if the logic should be applied in child records.
  // * {{{parentContextInfo}}}: the context info of the parent. To ensure that the childs (if 'handleChilds' is 'true') have also the context info of its parent.
  handleDefaultTreeView: function(
    handleCurrent,
    handleChilds,
    parentContextInfo
  ) {
    var contextInfo, tabViewPane, length, i, p;
    contextInfo = this.getContextInfo(false, true, true);

    if (!handleCurrent) {
      handleCurrent = false;
    }
    if (!handleChilds) {
      handleChilds = true;
    }

    for (p in parentContextInfo) {
      // While evaluating the 'defaultTreeViewLogicIf' the parent contextInfo is needed
      // because based on the parent selected record, the current view will be shown
      // in tree view or in grid view mode.
      if (Object.prototype.hasOwnProperty.call(parentContextInfo, p)) {
        contextInfo[p] = parentContextInfo[p];
      }
    }

    if (handleCurrent) {
      if (this.treeGrid && isc.isA.Function(this.defaultTreeViewLogicIf)) {
        if (this.defaultTreeViewLogicIf(contextInfo)) {
          if (!this.isShowingTree) {
            if (this.treeGrid.getDataSource()) {
              OB.ToolbarUtils.showTreeGrid(this);
            } else {
              this.defaultTreeView = true;
            }
          }
        } else if (this.isShowingTree) {
          OB.ToolbarUtils.hideTreeGrid(this);
        }
      }
    }

    if (handleChilds && this.childTabSet) {
      length = this.childTabSet.tabs.length;
      for (i = 0; i < length; i++) {
        tabViewPane = this.childTabSet.tabs[i].pane;
        if (tabViewPane.handleDefaultTreeView) {
          this.addPreferenceValues(contextInfo, tabViewPane);
          tabViewPane.handleDefaultTreeView(true, true, contextInfo);
        }
      }
    }
  },

  executeWhenTreeGridDSReady: function() {
    if (this.defaultTreeView) {
      // If the default visualization of the tab is the tree grid, it needs to be shown after
      // the tree grid datasource has been set. That's why it is handled here.
      this.handleDefaultTreeView();
      delete this.defaultTreeView;
    }
  },

  // handles different ways by which an error can be passed from the
  // system, translates this to an object with a type, title and message
  setErrorMessageFromResponse: function(resp, data, req) {
    var errorCode, index1, index2;

    // only handle it once
    if (resp._errorMessageHandled) {
      return true;
    }
    var msg = '',
      title = null,
      type = isc.OBMessageBar.TYPE_ERROR,
      isLabel = false,
      params = null;
    var gridEditing =
      req.clientContext &&
      (req.clientContext.editRow || req.clientContext.editRow === 0);
    if (isc.isA.String(data)) {
      msg = data;
    } else if (data && data.response) {
      if (data.response.errors) {
        // give it to the form
        if (this.isShowingForm) {
          this.viewForm.handleFieldErrors(data.response.errors);
        } else {
          this.viewGrid.setRecordFieldErrorMessages(
            req.clientContext.editRow,
            data.response.errors
          );
        }
        return true;
      } else if (data.response.error) {
        var error = data.response.error;
        if (error.type && error.type === 'user') {
          isLabel = true;
          msg = error.message;
          params = error.params;
        } else if (isc.isA.String(error.message)) {
          type = error.messageType || type;
          params = error.params;
          // error.messageType can be Error
          type = type.toLowerCase();
          title = error.title || title;
          msg = error.message;
        } else {
          // hope that someone else will handle it
          return false;
        }
      } else {
        // hope that someone else will handle it
        return false;
      }
    } else if (data && data.data) {
      // try it with data.data
      return this.setErrorMessageFromResponse(resp, data.data, req);
    } else {
      // hope that someone else will handle it
      return false;
    }

    req.willHandleError = true;
    resp._errorMessageHandled = true;
    if (msg.indexOf('@') !== -1) {
      index1 = msg.indexOf('@');
      index2 = msg.indexOf('@', index1 + 1);
      if (index2 !== -1) {
        errorCode = msg.substring(index1 + 1, index2);
        if (gridEditing) {
          this.setLabelInRow(req.clientContext.editRow, errorCode, params);
        } else {
          this.messageBar.setLabel(type, title, errorCode, params);
        }
      }
    } else if (isLabel) {
      if (gridEditing) {
        this.setLabelInRow(req.clientContext.editRow, msg, params);
      } else {
        this.messageBar.setLabel(type, title, msg, params);
      }
    } else if (gridEditing) {
      this.viewGrid.setRecordErrorMessage(req.clientContext.editRow, msg);
    } else {
      this.messageBar.setMessage(type, title, msg);
    }
    return true;
  },

  setLabelInRow: function(rowNum, label, params) {
    var me = this;
    OB.I18N.getLabel(
      label,
      params,
      {
        setLabel: function(text) {
          me.viewGrid.setRecordErrorMessage(rowNum, text);
        }
      },
      'setLabel'
    );
  },

  // ** {{{ createViewStructure }}} **
  // Is to be overridden, is called in initWidget.
  createViewStructure: function() {},

  // ** {{{ createMainParts }}} **
  // Creates the main layout components of this view.
  createMainParts: function() {
    var completeFieldsWithoutBLOBs, fieldsWithoutBLOBs;
    if (this.tabId && this.tabId.length > 0) {
      this.messageBar = isc.OBMessageBar.create({
        visibility: 'hidden',
        view: this
      });

      this.formGridLayout = isc.HLayout.create({
        width: '100%',
        height: '*',
        overflow: 'visible',
        view: this
      });

      this.activeBar = isc.HLayout.create({
        height: '100%',
        // to set active view when it gets clicked
        contents: '&nbsp;',
        width: OB.Styles.ActiveBar.width,
        styleName: OB.Styles.ActiveBar.inActiveStyleName,
        activeStyleName: OB.Styles.ActiveBar.activeStyleName,
        inActiveStyleName: OB.Styles.ActiveBar.inActiveStyleName,

        setActive: function(active) {
          if (active) {
            this.setStyleName(this.activeStyleName);
          } else {
            this.setStyleName(this.inActiveStyleName);
          }
        }
      });

      if (this.viewGrid) {
        // the grid should not show the image fields
        // see issue 20049 (https://issues.openbravo.com/view.php?id=20049)
        completeFieldsWithoutBLOBs = this.removeBLOBFields(
          this.viewGrid.completeFields
        );
        fieldsWithoutBLOBs = this.removeBLOBFields(this.viewGrid.fields);

        this.viewGrid.setDataSource(
          this.dataSource,
          completeFieldsWithoutBLOBs || fieldsWithoutBLOBs
        );
        this.viewGrid.setWidth('100%');
        this.viewGrid.setView(this);
        this.formGridLayout.addMember(this.viewGrid);
      }

      if (this.treeGrid) {
        this.treeGrid.setWidth('100%');
        this.treeGrid.setView(this);
        // The following method sets the tree grid datasource in an asynchronous way.
        // In order to execute actions once it has been set, use the 'executeWhenTreeGridDSReady' function.
        OB.Datasource.get(
          this.treeGrid.dataSourceId,
          this.treeGrid,
          null,
          true
        );
        this.treeGrid.hide();
        this.formGridLayout.addMember(this.treeGrid);
      }

      if (this.viewForm) {
        this.viewForm.setWidth('100%');
        this.formGridLayout.addMember(this.viewForm);
        this.viewForm.view = this;

        this.viewGrid.addFormProperties(this.viewForm.obFormProperties);
      }

      this.statusBar = isc.OBStatusBar.create({
        view: this.viewForm.view
      });

      // NOTE: when changing the layout structure and the scrollbar
      // location for these layouts check if the scrollTo method
      // in ob-view-form-linked-items is still called on the correct
      // object
      this.statusBarFormLayout = isc.VLayout.create({
        width: '100%',
        height: '*',
        visibility: 'hidden',
        overflow: 'hidden'
      });

      // to make sure that the form gets the correct scrollbars
      this.formContainerLayout = isc.OBFormContainerLayout.create({});
      this.formContainerLayout.addMember(this.viewForm);

      this.statusBarFormLayout.addMember(this.statusBar);
      this.statusBarFormLayout.addMember(this.formContainerLayout);

      this.formGridLayout.addMember(this.statusBarFormLayout);

      // wrap the messagebar and the formgridlayout in a VLayout
      this.gridFormMessageLayout = isc.VLayout.create({
        height: '100%',
        width: '100%',
        overflow: 'auto'
      });
      this.gridFormMessageLayout.addMember(this.messageBar);
      this.gridFormMessageLayout.addMember(this.formGridLayout);

      // and place the active bar to the left of the form/grid/messagebar
      this.activeGridFormMessageLayout = isc.HLayout.create({
        height: this.hasChildTabs ? '50%' : '100%',
        width: '100%',
        overflow: 'hidden'
      });

      this.activeGridFormMessageLayout.addMember(this.activeBar);
      this.activeGridFormMessageLayout.addMember(this.gridFormMessageLayout);

      this.addMember(this.activeGridFormMessageLayout);
    }
    if (this.hasChildTabs) {
      this.childTabSet = isc.OBTabSetChild.create({
        height: '*',
        parentContainer: this,
        parentTabSet: this.parentTabSet
      });
      this.addMember(this.childTabSet);
    } else if (this.isRootView) {
      // disable the maximize button if this is the root without
      // children
      this.statusBar.maximizeButton.disable();
    }
  },

  // returns a copy of fields after deleting the image fields and file fields
  // see issue 20049 (https://issues.openbravo.com/view.php?id=20049)
  // Added also file fields that cannot be displayed in grid view.
  removeBLOBFields: function(fields) {
    var indexesToDelete, i, length, fieldsWithoutBLOBs;
    indexesToDelete = [];
    if (fields) {
      fieldsWithoutBLOBs = fields.duplicate();
      length = fieldsWithoutBLOBs.length;
      // gets the index of the image fields
      for (i = 0; i < length; i++) {
        if (
          fieldsWithoutBLOBs[i].targetEntity === 'ADImage' ||
          fieldsWithoutBLOBs[i].targetEntity === 'OBFBL_FILE'
        ) {
          indexesToDelete.push(i);
        }
      }
      // removes the image fields
      length = indexesToDelete.length;
      for (i = 0; i < length; i++) {
        fieldsWithoutBLOBs.splice(indexesToDelete[i] - i, 1);
      }
    } else {
      fieldsWithoutBLOBs = fields;
    }
    return fieldsWithoutBLOBs;
  },

  getDirectLinkUrl: function() {
    var url = window.location.href,
      crit,
      fkCache;
    var qIndex = url.indexOf('?');
    var dIndex = url.indexOf('#');
    var index = -1;
    if (dIndex !== -1 && qIndex !== -1) {
      if (dIndex < qIndex) {
        index = dIndex;
      } else {
        index = qIndex;
      }
    } else if (qIndex !== -1) {
      index = qIndex;
    } else if (dIndex !== -1) {
      index = dIndex;
    }
    if (index !== -1) {
      url = url.substring(0, index);
    }

    url = url + '?tabId=' + this.tabId;
    if (this.isShowingForm && this.viewForm.isNew && this.isRootView) {
      url = url + '&command=NEW';
    } else if (
      (this.isShowingForm || !this.isRootView) &&
      !this.isShowingTree &&
      this.viewGrid.getSelectedRecords() &&
      this.viewGrid.getSelectedRecords().length === 1
    ) {
      url = url + '&recordId=' + this.viewGrid.getSelectedRecord().id;
    } else if (
      (this.isShowingForm || !this.isRootView) &&
      this.isShowingTree &&
      this.treeGrid.getSelectedRecords() &&
      this.treeGrid.getSelectedRecords().length === 1
    ) {
      url = url + '&recordId=' + this.treeGrid.getSelectedRecord().id;
    } else if (!this.isShowingForm && this.isRootView) {
      crit = this.viewGrid.getCriteria();
      if (crit && crit.criteria && crit.criteria.length > 0) {
        url =
          url +
          '&criteria=' +
          escape(
            isc.JSON.encode(crit, {
              prettyPrint: false,
              dateFormat: 'dateConstructor'
            })
          );
        fkCache = this.viewGrid.getFKFilterAuxiliaryCache(crit);
        if (isc.isA.Array(fkCache) && fkCache.length > 0) {
          url =
            url +
            '&fkCache=' +
            escape(
              isc.JSON.encode(fkCache, {
                prettyPrint: false,
                dateFormat: 'dateConstructor'
              })
            ) +
            '&';
        }
      }
      if (!this.viewGrid.filterClause) {
        // if the grid does not currently have a filterClause (i.e. because the filters have been cleared), make it explicit in the URL
        // this way the filter clause can be removed when building a grid based on this URL (see issue https://issues.openbravo.com/view.php?id=24577)
        url = url + '&emptyFilterClause=true';
      }
    }

    return url;
  },

  // ** {{{ addChildView }}} **
  // The addChildView creates the child tab and sets the pointer back to
  // this
  // parent.
  addChildView: function(childView) {
    if (
      (childView.isTrlTab &&
        OB.PropertyStore.get('ShowTrl', this.standardWindow.windowId) !==
          'Y') ||
      (childView.isAcctTab &&
        OB.PropertyStore.get('ShowAcct', this.standardWindow.windowId) !== 'Y')
    ) {
      return;
    }

    childView.parentView = this;
    childView.parentTabSet = this.childTabSet;

    this.standardWindow.addView(childView);

    if (this.childTabSet.tabs.length > 0 && !this.standardWindow.targetTabId) {
      // If it is a child tab that is not in the first position and if it is not a
      // direct navigation to the record (issue 27008), load a basic child view
      // to ensure a lazy initialization of the contents.
      // Once the tab be selected, the proper content will be loaded.
      this.prepareBasicChildView(childView);
    } else {
      // If the child tab is in first position, the content needs to be displayed immediately
      // once the parent view is loaded.
      this.prepareFullChildView(childView);
    }

    childView.tab = this.childTabSet.getTab(this.childTabSet.tabs.length - 1);
    childView.tab.setCustomState(isc.OBStandardView.MODE_INACTIVE);

    OB.TestRegistry.register(
      'org.openbravo.client.application.ChildTab_' +
        this.tabId +
        '_' +
        childView.tabId,
      childView.tab
    );
  },

  // ** {{{ prepareBasicChildView }}} **
  // It adds a tab with a basic layout. Once the tab is selected/set as active
  // a call to 'prepareFullChildView' is performed. The purpose of this view
  // is have a lazy initialization of the tab, so the proper content is
  // loaded only when it is required.
  prepareBasicChildView: function(childView) {
    var me = this;

    childView.isRenderedChildView = false;

    var childTabDef = {
      title: childView.tabTitle
    };

    childTabDef.pane = isc.VLayout.create({
      isRenderedChildView: false,
      lastCalledSizeFunction: null,
      updateSubtabVisibility: function() {
        return null;
      },
      doRefreshContents: function() {
        return null;
      },
      setTopMaximum: function() {
        this.lastCalledSizeFunction = 'setTopMaximum';
        return null;
      },
      setBottomMaximum: function() {
        this.lastCalledSizeFunction = 'setBottomMaximum';
        return null;
      },
      setHalfSplit: function() {
        this.lastCalledSizeFunction = 'setHalfSplit';
        return null;
      },
      toolBar: {
        updateButtonState: function() {
          return null;
        }
      },

      members: [isc.VLayout.create({})]
    });

    if (childView.showTabIf) {
      childTabDef.pane.showTabIf = childView.showTabIf;
      if (childView.originalShowTabIf) {
        childTabDef.pane.originalShowTabIf = childView.originalShowTabIf;
      }
    }

    childTabDef.pane.setAsActiveView = function() {
      me.prepareFullChildView(childView, childTabDef);
    };

    childTabDef.pane.paneActionOnSelect = function() {
      // If the initial load of the window makes that a child tab different from the first one be selected,
      // the logic doesn't pass through the 'setAsActiveView' so the 'prepareFullChildView' should be done
      // on the tab selection instead.
      me.prepareFullChildView(childView, childTabDef);
    };

    childTabDef.pane.destroy = function() {
      if (childView.members.length === 0) {
        // That means that there is nothing still loaded in the basic view
        // so if the pane is destroyed, its childView can be destroyed too.
        // In the other case, the full child view will handle the childView destruction.
        childView.destroy();
      }
      return this.Super('destroy', arguments);
    };

    this.childTabSet.addTab(childTabDef);
  },

  // ** {{{ prepareBasicChildView }}} **
  // It adds a tab with the whole view content based on the provided 'childView'.
  // If 'tab' parameter is provided, the 'childView' content is loaded inside the provided tab.
  prepareFullChildView: function(childView, tab) {
    var length, i, actionButton, toolBarButton, lastCalledSizeFunction;

    // Add buttons in parent to child. Note that currently it is only added one level.
    if (
      this.actionToolbarButtons &&
      this.actionToolbarButtons.length > 0 &&
      childView.showParentButtons
    ) {
      length = this.actionToolbarButtons.length;
      for (i = 0; i < length; i++) {
        actionButton = isc.OBToolbarActionButton.create(
          isc.addProperties({}, this.actionToolbarButtons[i], {
            baseStyle: 'OBToolbarTextButtonParent'
          })
        );
        actionButton.contextView = this; // Context is still parent view
        actionButton.toolBar = childView.toolBar;
        actionButton.view = childView;
        if (this.toolBar && this.toolBar.rightMembers) {
          toolBarButton = this.toolBar.rightMembers.find(
            'property',
            actionButton.property
          );
          if (toolBarButton && toolBarButton.readOnlyIf) {
            actionButton.readOnlyIf = toolBarButton.readOnlyIf;
          }
        }

        childView.toolBar.rightMembers.push(actionButton);

        childView.toolBar.addMems([[actionButton]]);
        childView.toolBar.addMems([
          [
            isc.HLayout.create({
              width: (this.toolBar && this.toolBar.rightMembersMargin) || 12,
              height: 1
            })
          ]
        ]);
      }

      if (this.actionToolbarButtons.length > 0) {
        // Add margin in the right
        childView.toolBar.addMems([
          [
            isc.HLayout.create({
              width: (this.toolBar && this.toolBar.rightMargin) || 4,
              height: 1
            })
          ]
        ]);
      }
    }

    // build the structure of the children
    childView.buildStructure();

    childView.updateViewBasedOnUiPattern();

    var childTabDef = {
      title: childView.tabTitle,
      pane: childView
    };
    if (!tab) {
      this.childTabSet.addTab(childTabDef);
    } else {
      lastCalledSizeFunction = tab.pane.lastCalledSizeFunction;
      delete tab.pane.lastCalledSizeFunction;

      // Destroy the old basic child view pane since it is not needed anymore
      tab.pane.destroy();

      this.childTabSet.setTabPane(tab, childTabDef.pane);

      if (
        this.state === isc.OBStandardView.STATE_IN_MID ||
        this.state === isc.OBStandardView.STATE_MID ||
        this.state === isc.OBStandardView.STATE_TOP_MAX
      ) {
        // If the view is in the middle or maximized, set the child view (if exists) minimized
        childView.setHeight('100%');
        if (childView.members[1]) {
          childView.members[1].setState(isc.OBStandardView.STATE_MIN);
        } else {
          childView.members[0].setHeight('100%');
        }
      } else if (lastCalledSizeFunction) {
        // If 'setTopMaximum' or 'setBottomMaximum' or 'setHalfSplit' has been called in an unrendered tab,
        // call it again now that the tab has been rendered to set the proper child view status
        if (lastCalledSizeFunction === 'setTopMaximum') {
          childView.setTopMaximum();
        } else if (lastCalledSizeFunction === 'setBottomMaximum') {
          childView.setBottomMaximum();
        } else if (lastCalledSizeFunction === 'setHalfSplit') {
          childView.setHalfSplit();
        }
      }
    }

    childView.isRenderedChildView = true;

    if (childView.initialTabDefinition) {
      // If there is an initial tab definition it means that there is a process that have set it there
      // but since the window was not loaded yet, it has not been applied. Apply this tab definition now
      // and delete the initialTabDefinition variable since it has been already applied.
      OB.Personalization.applyViewDefinitionToView(
        childView,
        childView.initialTabDefinition
      );
      delete childView.initialTabDefinition;
    }
  },

  setReadOnly: function(readOnly) {
    this.readOnly = readOnly;
    this.viewForm.readOnly = readOnly;
    if (this.viewGrid && readOnly) {
      this.viewGrid.setReadOnlyMode();
    }
    if (this.treeGrid && readOnly) {
      this.treeGrid.canReorderRecords = false;
    }
  },

  setEditOrDeleteOnly: function(editOrDeleteOnly) {
    this.editOrDeleteOnly = editOrDeleteOnly;
    if (editOrDeleteOnly) {
      this.dontCreateNewRowAutomatically();
    }
  },

  setSingleRecord: function(singleRecord) {
    this.singleRecord = singleRecord;
    if (singleRecord) {
      this.dontCreateNewRowAutomatically();
    }
  },

  dontCreateNewRowAutomatically: function() {
    this.viewGrid.setListEndEditAction();
  },

  allowNewRow: function() {
    if (this.readOnly || this.singleRecord || this.editOrDeleteOnly) {
      return false;
    }
    return true;
  },

  setViewFocus: function() {
    var object, functionName, i;

    // clear for a non-focusable item
    if (this.lastFocusedItem && !this.lastFocusedItem.getCanFocus()) {
      this.lastFocusedItem = null;
    }

    // Enable the shortcuts of the form and grid view
    // See issue 20651 (https://issues.openbravo.com/view.php?id=20651)
    if (this.viewForm && this.viewForm.enableShortcuts) {
      this.viewForm.enableShortcuts();
    }
    if (this.viewGrid && this.viewGrid.enableShortcuts) {
      this.viewGrid.enableShortcuts();
    }

    if (this.isShowingForm && this.viewForm) {
      if (!this.lastFocusedItem) {
        this.lastFocusedItem = this.viewForm.getItem(this.firstFocusedField);
      }
      if (this.lastFocusedItem && this.lastFocusedItem.getCanFocus()) {
        object = this.lastFocusedItem;
      } else if (
        this.viewForm.getFocusItem() &&
        this.viewForm.getFocusItem().getCanFocus()
      ) {
        object = this.viewForm.getFocusItem();
      } else {
        var fields = this.viewForm.fields;
        for (i = 0; i < fields.length; i++) {
          if (fields[i].getCanFocus()) {
            object = fields[i];
            break;
          }
        }
      }
      functionName = 'focusInItem';
    } else if (
      this.isEditingGrid &&
      this.viewGrid.getEditForm() &&
      this.viewGrid.getEditForm().getFocusItem()
    ) {
      object = this.viewGrid.getEditForm();
      functionName = 'focus';
    } else if (this.lastRecordSelected) {
      object = this.viewGrid;
      functionName = 'focus';
    } else if (this.lastFocusedItem) {
      object = this.lastFocusedItem;
      functionName = 'focusInItem';
    } else if (
      this.viewGrid &&
      !this.isShowingForm &&
      this.viewGrid.getFilterEditor() &&
      this.viewGrid.getFilterEditor().getEditForm()
    ) {
      this.viewGrid.focusInFirstFilterEditor();
      functionName = 'focus';
    } else if (this.viewGrid) {
      object = this.viewGrid;
      functionName = 'focus';
    }

    if (object && functionName) {
      isc.Page.setEvent(isc.EH.IDLE, object, isc.Page.FIRE_ONCE, functionName);
    }
  },

  setTabButtonState: function(active) {
    var tabButton;
    if (this.tab) {
      tabButton = this.tab;
    } else {
      // don't like to use the global window object, but okay..
      tabButton = window[this.standardWindow.viewTabId];
    }
    // enable this code to set the styleclass changes
    if (!tabButton) {
      return;
    }
    if (active) {
      tabButton.setCustomState('');
    } else {
      tabButton.setCustomState(isc.OBStandardView.MODE_INACTIVE);
    }
  },

  hasValidState: function() {
    return this.isRootView || this.getParentId();
  },

  isActiveView: function() {
    if (this.standardWindow && this.standardWindow.activeView) {
      return this.standardWindow.activeView === this;
    } else {
      return false;
    }
  },

  setAsActiveView: function(autoSaveDone) {
    var activeView = this.standardWindow.activeView;
    if (
      activeView &&
      activeView !== this &&
      ((activeView.isShowingForm && activeView.viewForm.inFicCall) ||
        (!activeView.isShowingForm &&
          activeView.viewGrid.getEditForm() &&
          activeView.viewGrid.getEditForm().inFicCall))
    ) {
      return;
    }
    if (
      !autoSaveDone &&
      this.standardWindow.activeView &&
      this.standardWindow.activeView !== this
    ) {
      var actionObject = {
        target: this,
        method: this.setAsActiveView,
        parameters: [true]
      };
      this.standardWindow.doActionAfterAutoSave(actionObject, false);
      return;
    }
    this.standardWindow.setActiveView(this);
  },

  setTargetRecordInWindow: function(recordId) {
    if (this.isActiveView()) {
      this.standardWindow.setTargetInformation(this.tabId, recordId);
    }
  },

  clearTargetRecordInWindow: function() {
    if (this.isActiveView()) {
      this.standardWindow.clearTargetInformation();
    }
  },

  setRecentDocument: function(record) {
    var params = this.standardWindow.getBookMarkParams();
    params.targetTabId = this.tabId;
    params.targetRecordId = record.id;
    params.recentId = this.tabId + '_' + record.id;
    params.recentTitle = record[OB.Constants.IDENTIFIER];
    OB.Layout.ViewManager.addRecentDocument(params);
  },

  setActiveViewProps: function(state) {
    if (state) {
      this.toolBar.show();
      if (this.statusBar) {
        this.statusBar.setActive(true);
      }
      if (this.activeBar) {
        this.activeBar.setActive(true);
      }
      this.setViewFocus();
      this.viewGrid.setActive(true);
      this.viewGrid.markForRedraw();
      // if we are in form view
      if (this.isShowingForm && !this.viewForm.isNew) {
        this.setTargetRecordInWindow(this.viewGrid.getSelectedRecord().id);
      }
      this.toolBar.updateButtonState(true, false, true);
    } else {
      // close any editors we may have
      this.viewGrid.closeAnyOpenEditor();

      this.toolBar.hide();
      if (this.statusBar) {
        this.statusBar.setActive(false);
      }
      if (this.activeBar) {
        this.activeBar.setActive(false);
      }
      this.viewGrid.setActive(false);
      this.viewGrid.markForRedraw();
      // note we can not check on viewForm visibility as
      // the grid and form can both be hidden when changing
      // to another tab, this handles the case that the grid
      // is shown but the underlying form has errors
      if (this.isShowingForm) {
        this.lastFocusedItem = this.viewForm.getFocusItem();
        this.viewForm.setFocusItem(null);
      }
      this.standardWindow.autoSave();
    }
    this.setTabButtonState(state);
  },

  visibilityChanged: function(visible) {
    if (visible && this.refreshContents) {
      this.doRefreshContents(true);
    }
  },

  doRefreshContents: function(
    doRefreshWhenVisible,
    forceRefresh,
    keepSelection
  ) {
    var callback,
      me = this;
    // if not visible anymore, reset the view back
    if (!this.isViewVisible()) {
      if (this.isShowingForm) {
        this.switchFormGridVisibility();
      }
      // deselect any records
      this.viewGrid.deselectAllRecords(false, true);
    }

    // update this one at least before bailing out
    this.updateTabTitle();

    if (!this.isViewVisible() && !forceRefresh) {
      this.refreshContents = doRefreshWhenVisible;
      return;
    }

    if (!this.refreshContents && !doRefreshWhenVisible && !forceRefresh) {
      return;
    }

    if (this.viewGrid.lazyFiltering && !forceRefresh) {
      this.viewGrid.filterHasChanged = true;
      this.viewGrid.sorter.enable();
      this.viewGrid.setData([]);
      this.viewGrid.resetEmptyMessage();
      return;
    }

    // can be used by others to see that we are refreshing content
    this.refreshContents = true;

    if (keepSelection) {
      this.viewGrid.recordsSelectedBeforeRefresh = this.viewGrid.getSelectedRecords();
      this.formVisibleBeforeRefresh = this.isShowingForm;
    }

    // clear all our selections..
    // note the true parameter prevents autosave actions from happening
    // this should have been done before anyway
    this.viewGrid.deselectAllRecords(false, true);

    if (this.viewGrid.filterEditor) {
      // do not clear the implicit filter
      // see issue https://issues.openbravo.com/view.php?id=19943
      this.viewGrid.clearFilter(true, true);
    }
    if (this.viewGrid.data && this.viewGrid.data.setCriteria) {
      this.viewGrid.data.setCriteria(null);
    }

    // hide the messagebar
    this.messageBar.hide();

    // allow default edit mode again
    this.allowDefaultEditMode = true;

    if (this.viewForm && this.isShowingForm) {
      this.viewForm.resetForm();
    }

    if (this.shouldOpenDefaultEditMode()) {
      this.openDefaultEditView();
    } else if (
      this.isShowingForm &&
      !(this.allowDefaultEditMode && this.defaultEditMode)
    ) {
      this.switchFormGridVisibility();
    }

    if (keepSelection) {
      callback = function() {
        var length, i, recordIndex;

        if (me.viewGrid.gridHasValidationErrors()) {
          // there are unsaved records with errors: discard the changes in order to show the refreshed data
          me.viewGrid.discardAllEdits();
        }

        length = me.viewGrid.recordsSelectedBeforeRefresh.length;
        for (i = 0; i < length; i++) {
          recordIndex = me.viewGrid.getRecordIndex(
            me.viewGrid.recordsSelectedBeforeRefresh[i]
          );
          me.viewGrid.selectRecord(recordIndex);
        }
        if (me.formVisibleBeforeRefresh) {
          if (me.viewGrid.getRecordIndex(me.viewForm.getValues()) !== -1) {
            // it might be that the record selected in the form is no longer present in the grid after being refreshed, due to having been deleted
            // in that case do not switch to form view, and keep the grid view to show the currently existing records
            me.switchFormGridVisibility();
          }
        }
        delete me.formVisibleBeforeRefresh;
        delete me.viewGrid.recordsSelectedBeforeRefresh;
      };
    } else {
      callback = null;
    }

    if (this.treeGrid && this.isShowingTree) {
      if (
        this.treeGrid.data &&
        !this.treeGrid.willFetchData(this.treeGrid.getCriteria())
      ) {
        // Force to do a datasource call keeping the current criteria
        this.treeGrid.data.invalidateCache();
      }
      this.treeGrid.fetchData(this.treeGrid.getCriteria());
    }
    this.viewGrid.refreshContents(callback);

    this.toolBar.updateButtonState(true);

    // if not visible or the parent also needs to be refreshed
    // enable the following code if we don't automatically select the first
    // record
    this.refreshChildViews();

    // set this at false at the end
    this.refreshContents = false;
  },

  refreshChildViews: function(keepSelection) {
    var i, length, tabViewPane;

    if (this.childTabSet) {
      length = this.childTabSet.tabs.length;
      for (i = 0; i < length; i++) {
        tabViewPane = this.childTabSet.tabs[i].pane;
        // force a refresh, only the visible ones will really
        // be refreshed
        tabViewPane.doRefreshContents(true, null, keepSelection);
      }
    }
  },

  /**
   * Empties the data of the child tabs and shows emptyMessage
   */
  initChildViewsForNewRecord: function() {
    var i, length, tabViewPane;

    if (this.childTabSet) {
      length = this.childTabSet.tabs.length;
      for (i = 0; i < length; i++) {
        tabViewPane = this.childTabSet.tabs[i].pane;
        if (tabViewPane.viewGrid) {
          tabViewPane.viewGrid.setData([]);
          tabViewPane.viewGrid.resetEmptyMessage();
        }
      }
    }
  },

  refreshMeAndMyChildViewsWithEntity: function(entity, excludedTabIds) {
    var i,
      length,
      tabViewPane,
      excludeTab = false;
    if (entity && excludedTabIds) {
      //Check is the tab has to be refreshed
      for (i = 0; i < excludedTabIds.length; i++) {
        if (excludedTabIds[i].match(this.tabId)) {
          excludeTab = true;
          // removes the tabId from the list of excluded, so it does
          // not have to be checked by the child tabs
          excludedTabIds.splice(i, 1);
          break;
        }
      }
      // If it the tab is not in the exclude list, refresh
      // it if it belongs to the entered entity
      if (!excludeTab) {
        if (this.entity === entity) {
          this.doRefreshContents(true);
        }
      }
      // Refresh the child views of this tab
      if (this.childTabSet) {
        length = this.childTabSet.tabs.length;
        for (i = 0; i < length; i++) {
          tabViewPane = this.childTabSet.tabs[i].pane;
          if (
            typeof tabViewPane.refreshMeAndMyChildViewsWithEntity === 'function'
          ) {
            tabViewPane.refreshMeAndMyChildViewsWithEntity(
              entity,
              excludedTabIds
            );
          }
        }
      }
    }
  },

  shouldOpenDefaultEditMode: function() {
    // can open default edit mode if defaultEditMode is set
    // and this is the root view or a child view with a selected parent.
    var oneOrMoreSelected =
      this.viewGrid &&
      this.viewGrid.data &&
      this.viewGrid.data.lengthIsKnown &&
      this.viewGrid.data.lengthIsKnown() &&
      this.viewGrid.data.getLength() >= 1;
    return (
      this.allowDefaultEditMode &&
      oneOrMoreSelected &&
      this.defaultEditMode &&
      (this.isRootView ||
        this.parentView.viewGrid.getSelectedRecords().length === 1)
    );
  },

  // opendefaultedit view for a child view is only called
  // when a new parent is selected, in that case the
  // edit view should be opened without setting the focus in the form
  openDefaultEditView: function(record) {
    if (!this.shouldOpenDefaultEditMode()) {
      return;
    }
    // preventFocus is treated as a boolean later
    var preventFocus = !this.isRootView;

    // don't open it again
    this.allowDefaultEditMode = false;

    // open form in edit mode
    if (record) {
      this.editRecord(record, preventFocus);
    } else if (
      this.viewGrid.data &&
      this.viewGrid.data.getLength() > 0 &&
      this.viewGrid.data.lengthIsKnown &&
      this.viewGrid.data.lengthIsKnown()
    ) {
      // edit the first record
      this.editRecord(this.viewGrid.getRecord(0), preventFocus);
    }
    // in other cases just show grid
  },

  // ** {{{ switchFormGridVisibility }}} **
  // Switch from form to grid view or the other way around
  switchFormGridVisibility: function() {
    if (!this.isShowingForm) {
      if (!this.viewForm.getDataSource()) {
        this.prepareViewForm();
      } else {
        this.viewForm.updateAlwaysTakeSpaceInSections();
      }
      if (this.treeGrid) {
        if (this.isShowingTree) {
          this.treeGrid.hide();
          this.changePreviousNextRecordsButtonVisibility(false);
        } else {
          this.changePreviousNextRecordsButtonVisibility(true);
        }
      }
      this.viewGrid.hide();
      this.statusBarFormLayout.show();
      this.statusBarFormLayout.setHeight('100%');
      if (this.isActiveView()) {
        this.viewForm.focus();
      }
      this.isShowingForm = true;
    } else {
      this.statusBarFormLayout.hide();
      // clear the form
      this.viewForm.resetForm();
      this.isShowingForm = false;
      this.viewGrid.markForRedraw('showing');
      if (this.isShowingTree) {
        this.treeGrid.show();
      } else {
        this.viewGrid.show();
      }
      if (this.isActiveView()) {
        if (
          this.viewGrid.getSelectedRecords() &&
          this.viewGrid.getSelectedRecords().length === 1
        ) {
          this.viewGrid.focus();
        } else {
          this.viewGrid.focusInFirstFilterEditor();
        }
      }

      this.viewGrid.setHeight('100%');
    }
    this.updateTabTitle();
  },

  changePreviousNextRecordsButtonVisibility: function(show) {
    if (show) {
      this.statusBar.previousButton.show();
      this.statusBar.nextButton.show();
    } else {
      this.statusBar.previousButton.hide();
      this.statusBar.nextButton.hide();
    }
  },

  doHandleClick: function() {
    if (!this.childTabSet) {
      return;
    }
    if (this.state !== isc.OBStandardView.STATE_MID) {
      this.setHalfSplit();
      this.previousState = this.state;
      this.state = isc.OBStandardView.STATE_MID;
    }
  },

  doHandleDoubleClick: function() {
    var tempState;
    if (!this.childTabSet) {
      return;
    }
    tempState = this.state;
    this.state = this.previousState;
    if (this.previousState === isc.OBStandardView.STATE_BOTTOM_MAX) {
      this.setBottomMaximum();
    } else if (
      tempState === isc.OBStandardView.STATE_MID &&
      this.previousState === isc.OBStandardView.STATE_MID
    ) {
      this.setTopMaximum();
    } else if (this.previousState === isc.OBStandardView.STATE_MID) {
      this.setHalfSplit();
    } else if (this.previousState === isc.OBStandardView.STATE_TOP_MAX) {
      this.setTopMaximum();
    } else {
      isc.warn(this.previousState + ' not supported ');
    }
    this.previousState = tempState;
  },

  // ** {{{ editNewRecordGrid }}} **
  // Opens the inline grid editing for a new record.
  editNewRecordGrid: function(rowNum) {
    if (this.isShowingForm) {
      this.switchFormGridVisibility();
    }
    this.viewGrid.startEditingNew(rowNum);
  },

  // ** {{{ editRecord }}} **
  // Opens the edit form and selects the record in the grid, will refresh
  // child views also
  editRecord: function(record, preventFocus, focusFieldName, wasEditingGrid) {
    var rowNum,
      recordToEdit,
      // at this point the time fields of the record are formatted in local time
      localTime = true;
    this.messageBar.hide();
    if (this.parentView && (this.parentView.entity !== this.entity)) {
      this.parentView.messageBar.hide();
    }

    // Set a temporary identifier for the record being edited in form view
    // See issue https://issues.openbravo.com/view.php?id=31331
    this.viewForm.recordIdInForm = OB.Utilities.getTemporaryId();

    if (!this.isShowingForm) {
      this.viewGrid.markForCalculateSummaries();
      this.switchFormGridVisibility();
    }

    if (!record) {
      //  new case
      this.viewGrid.deselectAllRecords();
      this.initChildViewsForNewRecord();
      this.viewForm.editNewRecord(preventFocus);
    } else {
      this.viewGrid.doSelectSingleRecord(record);

      // also handle the case that there are unsaved values in the grid
      // show them in the form
      rowNum = this.viewGrid.getRecordIndex(record);
      // If the record to be edited is new and was being edited in the grid, use it,
      // because this.viewGrid.getEditedRecord would return an empty record in this case
      if (record._new && wasEditingGrid) {
        recordToEdit = record;
      } else {
        recordToEdit = this.viewGrid.getEditedRecord(rowNum);
      }
      this.viewForm.editRecord(
        recordToEdit,
        preventFocus,
        this.viewGrid.recordHasChanges(rowNum),
        focusFieldName,
        localTime,
        wasEditingGrid
      );
    }
  },

  // ** {{{ editRecord }}} **
  // Opens the edit form and selects the record in the grid, will refresh
  // child views also
  editRecordFromTreeGrid: function(record, preventFocus, focusFieldName) {
    // at this point the time fields of the record are formatted in local time
    var localTime = true;
    this.messageBar.hide();

    if (!this.isShowingForm) {
      this.switchFormGridVisibility();
    }
    this.viewForm.editRecord(
      record,
      preventFocus,
      false,
      focusFieldName,
      localTime
    );
  },

  setMaximizeRestoreButtonState: function() {
    // single view, no maximize or restore
    if ((!this.hasChildTabs && this.isRootView) || !this.statusBar) {
      return;
    }
    // different cases:
    var theState = this.state;
    if (this.parentTabSet) {
      theState = this.parentTabSet.state;
    }

    if (theState === isc.OBStandardView.STATE_TOP_MAX) {
      this.statusBar.maximizeButton.hide();
      this.statusBar.restoreButton.show(true);
    } else if (theState === isc.OBStandardView.STATE_IN_MID) {
      this.statusBar.maximizeButton.show(true);
      this.statusBar.restoreButton.hide();
    } else if (!this.hasChildTabs) {
      this.statusBar.maximizeButton.hide();
      this.statusBar.restoreButton.show(true);
    } else {
      this.statusBar.maximizeButton.show(true);
      this.statusBar.restoreButton.hide();
    }
  },

  maximize: function() {
    if (this.parentTabSet) {
      this.parentTabSet.doHandleDoubleClick();
    } else {
      this.doHandleDoubleClick();
    }
    this.setMaximizeRestoreButtonState();
  },

  restore: function() {
    if (this.parentTabSet) {
      this.parentTabSet.doHandleDoubleClick();
    } else {
      this.doHandleDoubleClick();
    }
    this.setMaximizeRestoreButtonState();
  },

  // go to a next or previous record, if !next then the previous one is used
  editNextPreviousRecord: function(next) {
    var rowNum,
      increment,
      newRowNum,
      newRecord,
      currentSelectedRecord = this.viewGrid.getSelectedRecord();
    if (!currentSelectedRecord) {
      return;
    }
    rowNum = this.viewGrid.data.indexOf(currentSelectedRecord);
    if (next) {
      increment = 1;
    } else {
      increment = -1;
    }

    newRowNum = rowNum + increment;
    newRecord = this.viewGrid.getRecord(newRowNum);
    if (!newRecord) {
      return;
    }
    // a group and moving back, go back one more
    if (newRecord.isFolder && increment < 0) {
      newRowNum = newRowNum + increment;
      newRecord = this.viewGrid.getRecord(newRowNum);
    }
    if (!newRecord) {
      return;
    }
    if (newRecord.isFolder) {
      if (!this.viewGrid.groupTree.isOpen(newRecord)) {
        this.viewGrid.groupTree.openFolder(newRecord);
      }
      if (increment < 0) {
        // previous, pick the last from the group
        newRecord = newRecord.groupMembers[newRecord.groupMembers.length - 1];
        newRowNum = this.viewGrid.getRecordIndex(newRecord);
      } else {
        // next, pick the first from the group
        newRowNum = newRowNum + increment;
        newRecord = this.viewGrid.getRecord(newRowNum);
      }
      if (!newRecord) {
        return;
      }
    }
    this.viewGrid.scrollRecordToTop(newRowNum);
    this.editRecord(newRecord);
  },

  openDirectTabView: function(showContent) {
    // our content is done through the direct mode stuff
    this.refreshContents = false;

    if (this.parentTabSet && this.parentTabSet.getSelectedTab() !== this.tab) {
      this.parentTabSet.selectTab(this.tab);
    }

    if (showContent) {
      // this view is the last in the list then show it
      if (this.parentTabSet) {
        this.parentTabSet.setState(isc.OBStandardView.STATE_MID);
      } else {
        this.doHandleClick();
      }
      this.setMaximizeRestoreButtonState();

      // show the form with the selected record
      // if there is one, otherwise we are in grid mode
      if (this.viewGrid.targetRecordId && !this.isShowingForm) {
        // hide the grid as it should not show up in a short flash
        this.viewGrid.hide();
      }
      // bypass the autosave logic
      this.standardWindow.setActiveView(this);
      this.viewGrid.isOpenDirectMode = true;
      this.viewGrid.isOpenDirectModeLeaf = true;
    }

    if (this.parentView) {
      if (this.parentView.defaultEditMode && this.parentView.viewGrid) {
        // mark the parent grid to open the parent record in edit mode once its data has arrived
        this.parentView.viewGrid.isOpenDirectModeLeaf = true;
      }
      this.parentView.openDirectTabView(false);
    }
  },

  // ** {{{ recordSelected }}} **
  // Is called when a record get's selected. Will refresh direct child views
  // which will again refresh their children.
  recordSelected: function() {
    // no change go away
    if (!this.hasSelectionStateChanged()) {
      return;
    }

    // Update the tab visibility after a record has been selected and its session
    // attributes have been updated
    this.handleDefaultTreeView();
    this.updateSubtabVisibility();

    // If the record has been automatically selected because was the only record in the header tab,
    // only select the record if the window has not been opened by clicking on the recent views icon to
    // create a new record
    // see issue https://issues.openbravo.com/view.php?id=20564
    if (this.isShowingForm && this.viewForm.isNew) {
      return;
    }
    var me = this,
      callback = function() {
        me.delayedRecordSelected();
      };
    // wait 2 times longer than the fire on pause delay default
    this.fireOnPause(
      'delayedRecordSelected_' + this.ID,
      callback,
      this.fireOnPauseDelay * 2
    );
  },

  // function is called with a small delay to handle the case that a user
  // navigates quickly over a grid
  delayedRecordSelected: function() {
    var length;

    // is actually a different parent selected, only then refresh children
    var differentRecordId =
      !this.lastRecordSelected ||
      !this.viewGrid.getSelectedRecord() ||
      this.viewGrid.getSelectedRecord().id !== this.lastRecordSelected.id;
    var selectedRecordId = this.viewGrid.getSelectedRecord()
      ? this.viewGrid.getSelectedRecord().id
      : null;

    this.updateLastSelectedState();
    this.updateTabTitle();

    // commented line because of https://issues.openbravo.com/view.php?id=18963
    // toolbar seems to be refreshed in any case
    // note only set session info if there is a record selected
    this.toolBar.updateButtonState(
      !selectedRecordId || this.isEditingGrid || this.isShowingForm
    );

    var tabViewPane = null,
      i;

    // Do not try to refresh the child tabs of a new record
    if (this.viewGrid.getEditForm() && this.viewGrid.getEditForm().isNew) {
      return;
    }
    // refresh the tabs
    if (
      this.childTabSet &&
      (differentRecordId || !this.isOpenDirectModeParent)
    ) {
      length = this.childTabSet.tabs.length;
      for (i = 0; i < length; i++) {
        tabViewPane = this.childTabSet.tabs[i].pane;

        if (
          !selectedRecordId ||
          !this.isOpenDirectModeParent ||
          selectedRecordId !== tabViewPane.parentRecordId
        ) {
          tabViewPane.doRefreshContents(true);
        }
        if (this.isOpenDirectModeParent) {
          tabViewPane.toolBar.updateButtonState(true);
        }
      }
    }
    delete this.isOpenDirectModeParent;
  },

  updateSubtabVisibility: function() {
    var i,
      length,
      tabViewPane,
      activeTab,
      activeTabPane,
      indexFirstNotHiddenTab,
      contextInfo;
    if (this.childTabSet) {
      length = this.childTabSet.tabs.length;
      for (i = 0; i < length; i++) {
        tabViewPane = this.childTabSet.tabs[i].pane;
        // Calling getContextInfo with (false, true, true) in order to obtain also the value of the
        // session attributes of the form
        contextInfo = this.getContextInfo(false, true, true);
        this.addPreferenceValues(contextInfo, tabViewPane);
        if (
          !this.isSubtabOpenedByDirectLink() &&
          tabViewPane.showTabIf &&
          !tabViewPane.showTabIf(contextInfo)
        ) {
          this.childTabSet.tabBar.members[i].hide();
          // disabling the tab disables also it in the picker menu
          this.childTabSet.tabs[i].disabled = true;
          tabViewPane.hidden = true;
        } else {
          if (this.childTabSet.visibility === 'hidden') {
            this.childTabSet.show();
            if (
              tabViewPane.showTabIf &&
              !tabViewPane.data &&
              !tabViewPane.refreshingData &&
              tabViewPane.isVisible() &&
              !this.isEditingNewRecord()
            ) {
              // If the child tab does not have data yet, refresh it
              tabViewPane.refreshingData = true;
              tabViewPane.refresh();
            }
          }
          this.childTabSet.tabBar.members[i].show();
          this.childTabSet.tabs[i].disabled = false;
          tabViewPane.hidden = false;
          tabViewPane.updateSubtabVisibility();
        }
      }

      // menu might have changed, resetting it will force it to be regenereated next time it's opened
      this.childTabSet.resetTabPickerMenu();

      // If the active tab of the tabset is now hidden, another tab has to to be selected
      // If there are no visible tabs left, maximize the current view
      activeTab = this.childTabSet.getSelectedTab();
      activeTabPane = this.childTabSet.getTabPane(activeTab);
      if (activeTabPane.hidden) {
        //Look for the first not-hidden tab
        indexFirstNotHiddenTab = -1;
        for (i = 0; i < length; i++) {
          tabViewPane = this.childTabSet.tabs[i].pane;
          if (!tabViewPane.hidden) {
            indexFirstNotHiddenTab = i;
            break;
          }
        }
        if (indexFirstNotHiddenTab !== -1) {
          this.childTabSet.selectTab(indexFirstNotHiddenTab);
        } else {
          this.childTabSet.hide();
        }
      }
    }
  },

  isSubtabOpenedByDirectLink: function() {
    return (
      this.isOpenedByDirectLink() &&
      this.isSubTab(this.standardWindow.targetTabId)
    );
  },

  isOpenedByDirectLink: function() {
    return this.standardWindow.directTabInfo;
  },

  isSubTab: function(tabId) {
    var view;
    if (!tabId) {
      return false;
    }
    view = this.standardWindow.getView(tabId);
    if (!view) {
      return false;
    }
    return view.parentView !== null && view.parentView !== undefined;
  },

  //This function returns true if it is a new record and it is being edited
  isEditingNewRecord: function() {
    var form = this.isShowingForm ? this.viewForm : this.viewGrid.getEditForm();
    return form === null ? false : form.isNew;
  },

  // Adds to contextInfo the session attributes of the childView,
  // unless the session attribute is an auxiliary input of its parent tab
  addPreferenceValues: function(contextInfo, childView) {
    var auxInputs = {},
      p;
    if (this.viewForm && this.viewForm.auxInputs) {
      auxInputs = this.viewForm.auxInputs;
    }
    for (p in childView.preferenceValues) {
      if (
        Object.prototype.hasOwnProperty.call(childView.preferenceValues, p) &&
        !Object.prototype.hasOwnProperty.call(auxInputs, p)
      ) {
        contextInfo[p] = childView.preferenceValues[p];
      }
    }
  },

  // set childs to refresh when they are made visible
  setChildsToRefresh: function() {
    var length, i;

    if (this.childTabSet) {
      length = this.childTabSet.tabs.length;
      for (i = 0; i < length; i++) {
        if (!this.childTabSet.tabs[i].pane.isVisible()) {
          this.childTabSet.tabs[i].pane.refreshContents = true;
        }
      }
    }
  },

  hasSelectionStateChanged: function() {
    return (
      this.viewGrid &&
      ((this.viewGrid.getSelectedRecords() &&
        this.viewGrid.getSelectedRecords().length !==
          this.lastRecordSelectedCount) ||
        (this.viewGrid.getSelectedRecord() &&
          this.viewGrid.getSelectedRecord().id !==
            this.lastRecordSelected.id) ||
        (this.lastRecordSelected && !this.viewGrid.getSelectedRecord()))
    );
  },

  updateLastSelectedState: function() {
    this.lastRecordSelectedCount = this.viewGrid.getSelectedRecords().length;
    this.lastRecordSelected = this.viewGrid.getSelectedRecord();
  },

  getParentId: function() {
    var parentRecord = this.getParentRecord();
    if (parentRecord) {
      return parentRecord.id;
    }
  },

  getParentRecord: function() {
    var grid = null;
    // if there is no parent view, there is no parent record
    if (!this.parentView) {
      return null;
    }
    // use the standard tree of the tree grid depending on the view being shown
    if (this.parentView.isShowingTree) {
      grid = this.parentView.treeGrid;
    } else {
      grid = this.parentView.viewGrid;
    }
    // if the parent grid does not have exactly one selected record, return null
    if (!grid.getSelectedRecords() || grid.getSelectedRecords().length !== 1) {
      return null;
    }
    // a new parent is not a real parent
    if (
      !this.parentView.isShowingTree &&
      this.parentView.viewGrid.getSelectedRecord()._new
    ) {
      return null;
    }
    return grid.getSelectedRecord();
  },

  updateTabTitle: function() {
    var prefix = '',
      postFix;
    var suffix = '';
    var hasChanged =
      this.isShowingForm && (this.viewForm.isNew || this.viewForm.hasChanged);
    hasChanged =
      hasChanged ||
      (this.isEditingGrid &&
        this.viewGrid.getEditForm() &&
        (this.viewGrid.hasErrors() ||
          this.viewGrid.getEditForm().isNew ||
          this.viewGrid.getEditForm().hasChanged));
    if (hasChanged) {
      prefix = '* ';
    }

    // store the original tab title
    if (!this.originalTabTitle) {
      this.originalTabTitle = this.tabTitle;
    }

    var identifier, tab, tabSet, title;

    if (this.viewGrid.getSelectedRecord()) {
      identifier = this.viewGrid.getSelectedRecord()[OB.Constants.IDENTIFIER];
      if (this.viewGrid.getSelectedRecord()._new) {
        identifier = OB.I18N.getLabel('OBUIAPP_New');
      }
      if (!identifier) {
        identifier = '';
      } else {
        identifier = ' - ' + identifier;
      }
    }

    // showing the form
    if (
      this.isShowingForm &&
      this.viewGrid.getSelectedRecord() &&
      this.viewGrid.getSelectedRecord()[OB.Constants.IDENTIFIER]
    ) {
      if (!this.parentTabSet && this.viewTabId) {
        tab = OB.MainView.TabSet.getTab(this.viewTabId);
        tabSet = OB.MainView.TabSet;
        title = this.originalTabTitle + identifier;
      } else if (this.parentTabSet && this.tab) {
        tab = this.tab;
        tabSet = this.parentTabSet;
        title = this.originalTabTitle + identifier;
      }
    } else if (
      this.viewGrid.getSelectedRecords() &&
      this.viewGrid.getSelectedRecords().length > 0
    ) {
      if (this.viewGrid.getSelectedRecords().length === 1) {
        postFix = identifier;
      } else {
        postFix =
          ' - ' +
          OB.I18N.getLabel('OBUIAPP_SelectedRecords', [
            this.viewGrid.getSelectedRecords().length
          ]);
      }
      if (!this.parentTabSet && this.viewTabId) {
        tab = OB.MainView.TabSet.getTab(this.viewTabId);
        tabSet = OB.MainView.TabSet;
        title = this.originalTabTitle + postFix;
      } else if (this.parentTabSet && this.tab) {
        tab = this.tab;
        tabSet = this.parentTabSet;
        title = this.originalTabTitle + postFix;
      }
    } else if (!this.parentTabSet && this.viewTabId) {
      // the root view
      tabSet = OB.MainView.TabSet;
      tab = OB.MainView.TabSet.getTab(this.viewTabId);
      title = this.originalTabTitle;
    } else if (this.parentTabSet && this.tab) {
      // the check on this.tab is required for the initialization phase
      // only show a count if there is one parent
      tab = this.tab;
      tabSet = this.parentTabSet;

      if (
        !this.parentView.viewGrid.getSelectedRecords() ||
        this.parentView.viewGrid.getSelectedRecords().length !== 1
      ) {
        title = this.originalTabTitle;
      } else if (this.recordCount) {
        title = this.originalTabTitle + ' (' + this.recordCount + ')';
      } else {
        title = this.originalTabTitle;
      }
    }

    // happens when a tab gets closed
    if (!tab) {
      return;
    }

    if (title) {
      // show a prompt with the title info
      tab.prompt = title.asHTML();
      tab.showPrompt = true;
      tab.hoverWidth = 150;

      // trunc the title if it too large
      title = OB.Utilities.truncTitle(title);

      // add the prefix/suffix here to prevent cutoff on that
      title = prefix + title + suffix;
      tabSet.setTabTitle(tab, title);
    }

    // added check on tab as initially it is not set
    if (this.isRootView && tab) {
      // update the document title
      document.title = OB.Constants.WINTITLE + ' - ' + tab.title;
    }
  },

  isViewVisible: function() {
    // this prevents data requests for minimized tabs
    // note this.tab.isVisible is done as the tab is visible earlier than
    // the pane
    var visible =
      this.tab &&
      this.tab.isDrawn() &&
      this.tab.pane.isDrawn() &&
      this.tab.pane.isVisible();
    return (
      visible &&
      (!this.parentTabSet ||
        this.parentTabSet.getSelectedTabNumber() ===
          this.parentTabSet.getTabNumber(this.tab))
    );
  },

  // ++++++++++++++++++++ Button Actions ++++++++++++++++++++++++++
  // make a special refresh:
  // - refresh the current selected record without changing the selection
  // - refresh the parent/grand-parent in the same way without changing the selection
  // - recursive to children: refresh the children, put the children in grid mode and refresh
  refresh: function(refreshCallback, autoSaveDone, newRecordsToBeIncluded) {
    // If a record should be visible after the refresh, even if it does not comply with the
    // current filter, its ID should be entered in the newRecordsToBeIncluded parameter
    // See issue https://issues.openbravo.com/view.php?id=20722
    var me = this,
      view = this,
      actionObject,
      formRefresh,
      callback;

    // first save what we have edited
    if (!autoSaveDone) {
      actionObject = {
        target: this,
        method: this.refresh,
        parameters: [refreshCallback, true, newRecordsToBeIncluded]
      };
      this.standardWindow.doActionAfterAutoSave(actionObject, false);
      return;
    }

    if (this.viewForm && this.viewForm.contextInfo) {
      this.viewForm.contextInfo = null;
    }

    formRefresh = function() {
      if (refreshCallback) {
        refreshCallback();
      }
      // only perform refresh if the viewForm has a valid record.
      // else a request is done with incomplete criteria which results in non paginated request.
      // Refer issue https://issues.openbravo.com/view.php?id=26838
      if (me.viewForm.getValues()[OB.Constants.ID]) {
        me.viewForm.refresh();
      }
    };

    if (!newRecordsToBeIncluded) {
      if (this.parentRecordId && this.newRecordsAfterRefresh) {
        this.newRecordsAfterRefresh[this.parentRecordId] = [];
      } else {
        this.newRecordsAfterRefresh = [];
      }
    }
    if (!this.isShowingForm) {
      if (this.isShowingTree) {
        this.treeGrid.setData([]);
        this.treeGrid.refreshGrid(refreshCallback);
      } else {
        if (this.deferOpenNewEdit && this.messageBar.isVisible()) {
          this.messageBar.hide();
        }
        this.viewGrid.refreshGrid(refreshCallback, newRecordsToBeIncluded);
      }
    } else {
      if (this.viewForm.hasChanged) {
        callback = function(ok) {
          if (ok) {
            view.viewGrid.refreshGrid(formRefresh, newRecordsToBeIncluded);
          }
        };
        isc.ask(OB.I18N.getLabel('OBUIAPP_ConfirmRefresh'), callback);
      } else {
        this.viewGrid.refreshGrid(formRefresh, newRecordsToBeIncluded);
      }
    }
  },

  refreshParentRecord: function(callBackFunction) {
    if (this.parentView) {
      this.parentView.refreshCurrentRecord(callBackFunction);
    }
  },

  refreshCurrentRecord: function(callBackFunction) {
    var me = this,
      criteria,
      callback,
      requestProperties = {},
      params = {};

    if (!this.viewGrid.getSelectedRecord()) {
      return;
    }

    // Summary Functions are refreshed when data gets refreshed
    if (this.viewGrid.showGridSummary) {
      this.viewGrid.getSummaryRow();
    }

    callback = function(resp, data, req) {
      // this line does not work, but it should:
      //      me.getDataSource().updateCaches(resp, req);
      // therefore do an explicit update of the visual components
      if (me.viewGrid.data) {
        if (data.length !== 0) {
          var recordIndex = me.viewGrid.getRecordIndex(
            me.viewGrid.getSelectedRecord()
          );
          me.viewGrid.updateRecord(recordIndex, data, req);
        } else {
          me.viewGrid.data.localData.remove(me.viewGrid.getSelectedRecord());
          me.viewGrid.deselectAllRecords();
          me.markForRedraw();
          this.view.updateSubtabVisibility();
        }
      }

      if (callBackFunction) {
        callBackFunction();
      }
    };

    if (this.viewForm && this.viewForm.contextInfo) {
      this.viewForm.contextInfo = null;
    }

    if (this.isShowingForm) {
      // Refresh the form. This function will also update the info of the selected record with
      // the data returned by the datasource request done to update the form
      this.viewForm.refresh(callBackFunction);
    } else {
      // Make a request to refresh the grid
      criteria = this.buildCriteriaToRefreshSelectedRecord();
      // Include grid selected properties in the request
      // This way we retrieve the same record information which is obtained when refreshing the whole grid
      params._selectedProperties = this.viewGrid.getSelectedProperties();
      // We're going to refresh the grid filtering by the ID of the selected record
      // Set the flags required to avoid applying any other filter clauses
      params._directNavigation = true;
      params._targetRecordId = this.viewGrid.getSelectedRecord().id;
      requestProperties.params = params;
      this.getDataSource().fetchData(criteria, callback, requestProperties);
    }
    this.refreshParentRecord(callBackFunction);
  },

  buildCriteriaToRefreshSelectedRecord: function() {
    var record,
      criteria = {
        operator: 'and',
        _constructor: 'AdvancedCriteria',
        criteria: []
      };
    // add a dummy criteria to force a fetch
    criteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());

    record = this.viewGrid.getSelectedRecord();
    // and add a criteria for the record itself
    criteria.criteria.push({
      fieldName: OB.Constants.ID,
      operator: 'equals',
      value: record.id
    });
    return criteria;
  },

  hasNotChanged: function() {
    var view = this,
      form = view.viewForm,
      length,
      selectedRecords,
      grid = view.viewGrid,
      allRowsHaveErrors,
      hasErrors = false,
      editRow,
      i;
    if (view.isShowingForm) {
      if (form.isNew) {
        return false;
      }
      return (
        form.isSaving ||
        form.readOnly ||
        !view.hasValidState() ||
        !form.hasChanged
      );
    } else if (view.isEditingGrid) {
      editRow = view.viewGrid.getEditRow();
      hasErrors = view.viewGrid.rowHasErrors(editRow);
      form = grid.getEditForm();
      return (
        !form.isNew &&
        !hasErrors &&
        (form.isSaving ||
          form.readOnly ||
          !view.hasValidState() ||
          !form.hasChanged)
      );
    } else {
      selectedRecords = grid.getSelectedRecords();
      allRowsHaveErrors = true;
      length = selectedRecords.length;
      for (i = 0; i < length; i++) {
        var rowNum = grid.getRecordIndex(selectedRecords[i]);
        allRowsHaveErrors = allRowsHaveErrors && grid.rowHasErrors(rowNum);
      }
      return selectedRecords.length === 0 || !allRowsHaveErrors;
    }
  },

  saveRow: function() {
    var me = this;
    if (me.existsAction(OB.EventHandlerRegistry.PRESAVE)) {
      me.executePreSaveActions(function() {
        me.doSaveRow();
      });
      return;
    }
    me.doSaveRow();
  },

  doSaveRow: function() {
    if (this.isEditingGrid) {
      this.viewGrid.endEditing();
    } else {
      this.viewForm.saveRow();
    }
  },

  executePreSaveActions: function(saveRowCallback) {
    var editForm,
      eventHandlerParams = {};

    if (this.isEditingGrid) {
      editForm = this.viewGrid.getEditForm();
      if (editForm) {
        eventHandlerParams.data = isc.clone(editForm.getValues());
        eventHandlerParams.isNewRecord = editForm.isNew;
      }
    } else {
      eventHandlerParams.data = isc.clone(this.viewForm.getValues());
      eventHandlerParams.isNewRecord = this.viewForm.isNew;
    }
    this.callSaveActions(
      OB.EventHandlerRegistry.PRESAVE,
      eventHandlerParams,
      saveRowCallback
    );
  },

  executePreDeleteActions: function(deleteRowCallback) {
    var eventHandlerParams = {},
      currentGrid;

    if (this.isShowingTree) {
      currentGrid = this.treeGrid;
    } else {
      currentGrid = this.viewGrid;
    }
    eventHandlerParams.recordsToDelete = isc.shallowClone(
      currentGrid.getSelection()
    );
    this.callClientEventHandlerActions(
      OB.EventHandlerRegistry.PREDELETE,
      eventHandlerParams,
      deleteRowCallback,
      true
    );
  },

  existsAction: function(actionType) {
    return (
      this.tabId && OB.EventHandlerRegistry.hasAction(this.tabId, actionType)
    );
  },

  callSaveActions: function(actionType, extraParameters, callback) {
    if (
      actionType !== OB.EventHandlerRegistry.PRESAVE &&
      actionType !== OB.EventHandlerRegistry.POSTSAVE
    ) {
      return;
    }
    this.callClientEventHandlerActions(actionType, extraParameters, callback);
  },

  callClientEventHandlerActions: function(
    actionType,
    extraParameters,
    callback,
    executeCallback
  ) {
    var params;
    if (this.existsAction(actionType)) {
      params = {
        tabId: this.tabId,
        actionType: actionType,
        view: this,
        form: this.viewForm,
        grid: this.viewGrid,
        extraParameters: extraParameters,
        callback: callback
      };
      OB.EventHandlerRegistry.call(params);
    } else if (executeCallback && isc.isA.Function(callback)) {
      callback();
    }
  },

  deleteSelectedRows: function(autoSaveDone) {
    var msg,
      dialogTitle,
      view = this,
      deleteCount,
      callback,
      currentGrid;

    if (!this.readOnly && this.isDeleteableTable) {
      // first save what we have edited
      if (!autoSaveDone) {
        var actionObject = {
          target: this,
          method: this.deleteSelectedRows,
          parameters: [true]
        };
        this.standardWindow.doActionAfterAutoSave(actionObject, false);
        return;
      }
      if (this.isShowingTree) {
        currentGrid = this.treeGrid;
      } else {
        currentGrid = this.viewGrid;
      }
      deleteCount = currentGrid.getSelection().length;

      if (deleteCount === 1) {
        msg = OB.I18N.getLabel('OBUIAPP_DeleteConfirmationSingle');
        dialogTitle = OB.I18N.getLabel('OBUIAPP_DialogTitle_DeleteRecord');
      } else {
        msg = OB.I18N.getLabel('OBUIAPP_DeleteConfirmationMultiple', [
          deleteCount
        ]);
        dialogTitle = OB.I18N.getLabel('OBUIAPP_DialogTitle_DeleteRecords');
      }

      callback = function(ok) {
        var i,
          doUpdateTotalRows,
          deleteData,
          error,
          recordInfos = [],
          length,
          removeCallBack,
          selection;

        //modal dialog shown to restrict the user from accessing records when deleting records. Will be closed after successful deletion in removeCallback.
        //refer issue https://issues.openbravo.com/view.php?id=24611
        if (ok) {
          isc.showPrompt(
            OB.I18N.getLabel('OBUIAPP_DeletingRecords') +
              isc.Canvas.imgHTML({
                src: OB.Styles.LoadingPrompt.loadingImage.src
              })
          );
        }

        removeCallBack = function(resp, data, req) {
          var length,
            localData = resp.dataObject || resp.data || data,
            i,
            updateTotalRows,
            currentGrid;

          if (!localData) {
            // bail out, an error occured which should be displayed to the user now
            //clear deleting prompt
            view.restoreGridSelection(selection);
            isc.clearPrompt();
            return;
          }
          var status = resp.status;
          if (
            localData &&
            Object.prototype.hasOwnProperty.call(localData, 'status')
          ) {
            status = localData.status;
          }
          if (
            localData &&
            localData.response &&
            Object.prototype.hasOwnProperty.call(localData.response, 'status')
          ) {
            status = localData.response.status;
          }
          if (status === isc.RPCResponse.STATUS_SUCCESS) {
            if (view.isShowingTree) {
              currentGrid = view.treeGrid;
            } else {
              currentGrid = view.viewGrid;
            }
            if (view.isShowingForm) {
              view.switchFormGridVisibility();
            }
            view.messageBar.setMessage(
              isc.OBMessageBar.TYPE_SUCCESS,
              null,
              OB.I18N.getLabel('OBUIAPP_DeleteResult', [deleteCount])
            );
            if (deleteData) {
              // note totalrows is used when inserting a new row, to determine after which
              // record to add a new row
              updateTotalRows =
                currentGrid.data.getLength() === currentGrid.data.totalRows;
              // deleteData is computed below
              length = deleteData.ids.length;
              for (i = 0; i < length; i++) {
                recordInfos.push({
                  id: deleteData.ids[i]
                });
              }
              view.viewGrid.data.handleUpdate(
                'remove',
                recordInfos,
                false,
                req
              );
              if (
                view.treeGrid &&
                view.treeGrid.data &&
                view.treeGrid.data.handleUpdate
              ) {
                view.treeGrid.data.handleUpdate(
                  'remove',
                  recordInfos,
                  false,
                  req
                );
              }
              if (updateTotalRows) {
                currentGrid.data.totalRows = currentGrid.data.getLength();
              }
            } else if (doUpdateTotalRows) {
              currentGrid.data.totalRows = currentGrid.data.getLength();
            }
            view.viewGrid.updateRowCountDisplay();
            // Refresh the grid based on Refresh After Deletion preference
            if (
              OB.PropertyStore.get(
                'OBUIAPP_RefreshAfterDeletion',
                view.standardWindow.windowId
              ) === 'Y'
            ) {
              view.viewGrid.refreshGrid();
            } else {
              view.refreshChildViews();
            }
            view.refreshParentRecord();
          } else {
            view.restoreGridSelection(selection);
            // get the error message from the dataObject
            if (
              localData.response &&
              localData.response.error &&
              localData.response.error.message
            ) {
              error = localData.response.error;
              if (error.type && error.type === 'user') {
                view.messageBar.setLabel(
                  isc.OBMessageBar.TYPE_ERROR,
                  null,
                  error.message,
                  error.params
                );
              } else if (error.message && error.params) {
                view.messageBar.setLabel(
                  isc.OBMessageBar.TYPE_ERROR,
                  null,
                  error.message,
                  error.params
                );
              } else if (error.message) {
                view.messageBar.setMessage(
                  isc.OBMessageBar.TYPE_ERROR,
                  null,
                  error.message
                );
              } else {
                view.messageBar.setMessage(
                  isc.OBMessageBar.TYPE_ERROR,
                  null,
                  OB.I18N.getLabel('OBUIAPP_DeleteResult', [0])
                );
              }
            }
          }
          isc.clearPrompt();
        };
        if (ok) {
          if (view.isShowingTree) {
            currentGrid = view.treeGrid;
          } else {
            currentGrid = view.viewGrid;
          }

          selection = currentGrid.getSelection().duplicate();
          // deselect the current records
          currentGrid.deselectAllRecords();
          view.viewGrid.markForCalculateSummaries();

          if (selection.length > 1) {
            deleteData = {};
            deleteData.entity = view.entity;
            deleteData.ids = [];
            length = selection.length;
            for (i = 0; i < length; i++) {
              deleteData.ids.push(selection[i][OB.Constants.ID]);
            }
            OB.RemoteCallManager.call(
              'org.openbravo.client.application.MultipleDeleteActionHandler',
              deleteData,
              {},
              removeCallBack,
              {
                refreshGrid: true
              }
            );
          } else {
            if (view.isShowingTree) {
              deleteData = {};
              deleteData.entity = view.entity;
              deleteData.ids = [];
              length = selection.length;
              deleteData.ids.push(selection[0][OB.Constants.ID]);
            }
            // note totalrows is used when inserting a new row, to determine after which
            // record to add a new row
            doUpdateTotalRows =
              currentGrid.data.getLength() === currentGrid.data.totalRows;
            // note remove data expects only the id, the record key as the first param
            view.viewGrid.removeData(
              {
                id: selection[0].id
              },
              removeCallBack,
              {}
            );
          }
        }
      };
      this.executePreDeleteActions(function() {
        isc.ask(msg, callback, {
          title: dialogTitle
        });
      });
    }
  },

  restoreGridSelection: function(selection) {
    var currentGrid;
    if (this.isShowingTree) {
      currentGrid = this.treeGrid;
    } else {
      currentGrid = this.viewGrid;
    }
    currentGrid.selection.selectList(selection);
    currentGrid.fireSelectionUpdated();
  },

  newRow: function(rowNum) {
    var actionObject = {
      target: this,
      method: this.editNewRecordGrid,
      parameters: [rowNum]
    };
    this.standardWindow.doActionAfterAutoSave(actionObject, false);
  },

  newDocument: function() {
    var actionObject = {
      target: this,
      method: this.editRecord,
      parameters: {
        isNewDocument: true
      }
    };
    this.standardWindow.doActionAfterAutoSave(actionObject, false);
  },

  undo: function() {
    var view = this,
      form,
      grid,
      errorRows,
      i,
      length;
    view.messageBar.hide(true);
    if (this.isEditingGrid) {
      grid = view.viewGrid;
      // the editing grid will take care of the confirmation
      grid.cancelEditing();

      // undo edit in all records with errors
      if (grid.hasErrors()) {
        errorRows = grid.getErrorRows();
        length = errorRows.length;
        for (i = 0; i < length; i++) {
          grid.selectRecord(grid.getRecord(errorRows[i]));
        }
        grid.undoEditSelectedRows();
      }
      return;
    } else if (this.isShowingForm) {
      form = this.viewForm;
    } else {
      // selected records
      grid = view.viewGrid;
    }
    if (form) {
      form.undo();
    } else {
      grid.undoEditSelectedRows();
    }
  },

  // ++++++++++++++++++++ Parent-Child Tab Handling ++++++++++++++++++++++++++
  convertToPercentageHeights: function() {
    if (!this.members[1]) {
      return;
    }
    var height = this.members[1].getHeight();
    var percentage = (height / this.getHeight()) * 100;
    // this.members[0].setHeight((100 - percentage) + '%');
    this.members[0].setHeight('*');
    this.members[1].setHeight(percentage + '%');
  },

  setTopMaximum: function() {
    this.setHeight('100%');
    if (this.members[1]) {
      this.members[1].setState(isc.OBStandardView.STATE_MIN);
      this.convertToPercentageHeights();
    } else {
      this.members[0].setHeight('100%');
    }
    this.members[0].show();
    this.state = isc.OBStandardView.STATE_TOP_MAX;
    this.setMaximizeRestoreButtonState();
  },

  setBottomMaximum: function() {
    if (this.members[1]) {
      this.members[0].hide();
      this.members[1].setHeight('100%');
    }
    this.state = isc.OBStandardView.STATE_BOTTOM_MAX;
    this.setMaximizeRestoreButtonState();
  },

  setHalfSplit: function() {
    if (this.members[1]) {
      // divide the space between the first and second level
      if (this.members[1].draggedHeight) {
        this.members[1].setHeight(this.members[1].draggedHeight);
        this.convertToPercentageHeights();
      } else {
        // NOTE: noticed that when resizing multiple members in a layout, that it
        // makes a difference what the order of resizing is, first resize the
        // one which will be larger, then the one which will be smaller.
        this.members[1].setHeight('50%');
      }
      this.members[1].setState(isc.OBStandardView.STATE_IN_MID);
    } else {
      this.members[0].setHeight('100%');
    }
    this.members[0].show();
    this.state = isc.OBStandardView.STATE_MID;
    this.setMaximizeRestoreButtonState();
  },

  getCurrentValues: function() {
    var ret;
    if (this.isShowingForm) {
      ret = this.viewForm.getValues();
    } else if (this.isShowingTree) {
      ret = this.treeGrid.getSelectedRecord();
    } else if (this.isEditingGrid) {
      ret = isc.addProperties(
        {},
        this.viewGrid.getSelectedRecord(),
        this.viewGrid.getEditForm().getValues()
      );
    } else {
      ret = this.viewGrid.getSelectedRecord();
    }
    // return an empty object if ret is not set
    // this happens when a new record could not be saved
    // and the form view is switched for grid view
    return ret || {};
  },

  getPropertyFromColumnName: function(columnName) {
    var length = this.view.propertyToColumns.length,
      i;
    for (i = 0; i < length; i++) {
      var propDef = this.view.propertyToColumns[i];
      if (propDef.dbColumn === columnName) {
        return propDef.property;
      }
    }
    return null;
  },

  getPropertyDefinitionFromDbColumnName: function(columnName) {
    var length = this.propertyToColumns.length,
      i;
    for (i = 0; i < length; i++) {
      var propDef = this.propertyToColumns[i];
      if (propDef.dbColumn === columnName) {
        return propDef;
      }
    }
    return null;
  },

  getPropertyFromDBColumnName: function(columnName) {
    var length = this.propertyToColumns.length,
      i;
    for (i = 0; i < length; i++) {
      var propDef = this.propertyToColumns[i];
      if (propDef.dbColumn === columnName) {
        return propDef.property;
      }
    }
    return null;
  },

  getPropertyDefinitionFromInpColumnName: function(columnName) {
    var length = this.propertyToColumns.length,
      i;
    for (i = 0; i < length; i++) {
      var propDef = this.propertyToColumns[i];
      if (propDef.inpColumn === columnName) {
        return propDef;
      }
    }
    return null;
  },

  //++++++++++++++++++ Reading context ++++++++++++++++++++++++++++++
  getContextInfo: function(
    onlySessionProperties,
    classicMode,
    forceSettingContextVars,
    convertToClassicFormat
  ) {
    var contextInfo = {},
      addProperty,
      rowNum,
      properties,
      i,
      p,
      grid;
    // if classicmode is undefined then both classic and new props are used
    var classicModeUndefined = typeof classicMode === 'undefined';
    var value, field, record, form, component, propertyObj, type, length;

    if (classicModeUndefined) {
      classicMode = true;
    }

    if (this.isShowingTree) {
      grid = this.treeGrid;
    } else {
      grid = this.viewGrid;
    }

    // a special case, the editform has been build but it is not present yet in the
    // form, so isEditingGrid is true but the edit form is not there yet, in that
    // case use the viewGrid as component and the selected record
    if (this.isEditingGrid && this.viewGrid.getEditForm()) {
      rowNum = this.viewGrid.getEditRow();
      if (rowNum || rowNum === 0) {
        if (this.viewGrid._hidingField || this.viewGrid._showingField) {
          // If this has been caused by hiding or showing a field while the grid was being edited,
          // add the properties of the saved edit values
          // See issue https://issues.openbravo.com/view.php?id=21352
          record = isc.addProperties(
            {},
            this.viewGrid.getRecord(rowNum),
            this.viewGrid.getEditValues(rowNum),
            this.viewGrid._savedEditValues
          );
        } else {
          record = isc.addProperties(
            {},
            this.viewGrid.getRecord(rowNum),
            this.viewGrid.getEditValues(rowNum)
          );
        }
        // Prevents the proper id from being overwritten with the dummy id
        // See issue https://issues.openbravo.com/view.php?id=22625
        if (
          this.viewGrid.getEditValues(rowNum)[OB.Constants.ID] &&
          this.viewGrid.getEditValues(rowNum)[OB.Constants.ID].indexOf('_') ===
            0 &&
          this.viewGrid.getRecord(rowNum)[OB.Constants.ID].indexOf('_') !== 0
        ) {
          record[OB.Constants.ID] = this.viewGrid.getRecord(rowNum)[
            OB.Constants.ID
          ];
        }
      } else {
        record = isc.addProperties({}, this.viewGrid.getSelectedRecord());
      }
      component = this.viewGrid.getEditForm();
      form = component;
    } else if (this.isShowingForm) {
      // note on purpose not calling form.getValues() as this will cause extra requests
      // in case of a picklist
      record = isc.addProperties(
        {},
        this.viewGrid.getSelectedRecord(),
        this.viewForm.values
      );
      component = this.viewForm;
      form = component;
    } else {
      record = grid.getSelectedRecord();
      rowNum = grid.getRecordIndex(record);
      if (rowNum || rowNum === 0) {
        record = isc.addProperties({}, record, grid.getEditValues(rowNum));
      }
      component = grid;
    }

    properties = this.propertyToColumns;

    if (record) {
      // add the id of the record itself also if not set
      if (!record[OB.Constants.ID] && grid.getSelectedRecord()) {
        // if in edit mode then the grid always has the current record selected
        record[OB.Constants.ID] = grid.getSelectedRecord()[OB.Constants.ID];
      }

      // New records in grid have a dummy id (see OBViewGrid.createNewRecordForEditing)
      // whereas new form records don't have it. This temporary id starts with _. Removing this
      // id so it behaves in the same way in form and grid
      if (
        record[OB.Constants.ID] &&
        record[OB.Constants.ID].indexOf('_') === 0
      ) {
        // startsWith a SC function, is slower than indexOf
        record[OB.Constants.ID] = undefined;
      }

      length = properties.length;
      for (i = 0; i < length; i++) {
        propertyObj = properties[i];
        value = record[propertyObj.property];
        field = component.getField(propertyObj.property);
        if (
          field &&
          field.editorType && //
          Object.prototype.toString.call(value) === '[object Date]' && //
          OB.Utilities.getCanvasProp(field.editorType, 'isAbsoluteDateTime')
        ) {
          // In the case of an absolute datetime, it needs to be converted in order to avoid the UTC conversion
          // http://forums.smartclient.com/showthread.php?p=116135
          value = OB.Utilities.Date.addTimezoneOffset(value);
        }
        addProperty = propertyObj.sessionProperty || !onlySessionProperties;
        if (addProperty) {
          if (classicMode) {
            if (propertyObj.type && convertToClassicFormat) {
              type = isc.SimpleType.getType(propertyObj.type);
              if (type.createClassicString) {
                if (type.editorType === 'OBDateTimeItem') {
                  // converting time to UTC before it is sent to FIC
                  value = OB.Utilities.Date.addTimezoneOffset(value);
                }
                contextInfo[properties[i].inpColumn] = type.createClassicString(
                  value
                );
              } else {
                contextInfo[properties[i].inpColumn] = this.convertContextValue(
                  value,
                  propertyObj.type
                );
              }
            } else {
              contextInfo[properties[i].inpColumn] = this.convertContextValue(
                value,
                propertyObj.type
              );
            }
          } else {
            // surround the property name with @ symbols to make them different
            // from filter criteria and such
            contextInfo[
              '@' + this.entity + '.' + properties[i].property + '@'
            ] = this.convertContextValue(value, propertyObj.type);
          }
        }
      }

      if (!onlySessionProperties) {
        for (p in this.standardProperties) {
          if (
            Object.prototype.hasOwnProperty.call(this.standardProperties, p)
          ) {
            if (classicMode) {
              contextInfo[p] = this.convertContextValue(
                this.standardProperties[p]
              );
            } else {
              // surround the property name with @ symbols to make them different
              // from filter criteria and such
              contextInfo[
                '@' + this.entity + '.' + p + '@'
              ] = this.convertContextValue(this.standardProperties[p]);
            }
          }
        }
      }
    }
    if (form || forceSettingContextVars) {
      if (!form) {
        form = this.viewForm;
      }
      isc.addProperties(contextInfo, form.auxInputs);
      isc.addProperties(contextInfo, form.hiddenInputs);
      isc.addProperties(contextInfo, form.sessionAttributes);
    }

    if (this.parentView) {
      // parent properties do not override contextInfo
      var parentContextInfo = this.parentView.getContextInfo(
        onlySessionProperties,
        classicMode,
        forceSettingContextVars,
        convertToClassicFormat
      );
      contextInfo = isc.addProperties(parentContextInfo, contextInfo);
    }

    return contextInfo;
  },

  convertContextValue: function(value, type) {
    var isTime;
    // if a string is received, it is converted to a date so that the function
    //   is able to return its UTC time in the HH:mm:ss format
    if (
      isc.isA.String(value) &&
      value.length > 0 &&
      type &&
      isc.SimpleType.getType(type).inheritsFrom === 'time'
    ) {
      value = this.convertToDate(value);
    }
    isTime =
      isc.isA.Date(value) &&
      type &&
      isc.SimpleType.getType(type).inheritsFrom === 'time';
    if (isTime) {
      return (
        value.getUTCHours() +
        ':' +
        value.getUTCMinutes() +
        ':' +
        value.getUTCSeconds()
      );
    }
    return value;
  },

  convertToDate: function(stringValue) {
    var today = new Date(),
      dateValue = isc.Time.parseInput(stringValue);
    // Set the month initially to January to prevent error like this
    // provided date: 15/02/2014
    // today: 31/03/2014
    // date.setDate(today.getDate()) would result in Mon Mar 02 2014 18:00:00 GMT+0100 (CET), because february does not have 31 days
    dateValue.setMonth(0);
    // Only the time is relevant. In order to be able to convert it from UTC to local time
    //   properly the date value should be today's date
    dateValue.setDate(today.getDate());
    dateValue.setMonth(today.getMonth());
    dateValue.setYear(today.getFullYear());
    return dateValue;
  },

  getPropertyDefinition: function(property) {
    var properties = this.propertyToColumns,
      i,
      length = properties.length;
    for (i = 0; i < length; i++) {
      if (property === properties[i].property) {
        return properties[i];
      }
    }
    return null;
  },

  // if defined, the errorCallbackFunction will be executed if the FIC call returns with an error status (i.e. connectivity error)
  setContextInfo: function(
    sessionProperties,
    callbackFunction,
    forced,
    errorCallbackFunction
  ) {
    var newCallback,
      me = this,
      gridVisibleProperties = [],
      len,
      i,
      originalID;
    // no need to set the context in this case
    if (!forced && (this.isEditingGrid || this.isShowingForm)) {
      if (callbackFunction) {
        callbackFunction();
      }
      return;
    }

    if (!sessionProperties) {
      // Call to the FIC in EDIT mode, all properties must be sent, not only the session properties
      sessionProperties = this.getContextInfo(false, true, false, true);
    }

    if (this.viewGrid && this.viewGrid.getSelectedRecord()) {
      originalID = this.viewGrid.getSelectedRecord()[OB.Constants.ID];
    }

    newCallback = function(response, data, request) {
      var context = {},
        grid = me.viewGrid,
        currentRecord,
        currentID;
      currentRecord = grid.getSelectedRecord();
      if (currentRecord) {
        context.rowNum = grid.getRecordIndex(currentRecord);
        currentID = currentRecord[OB.Constants.ID];
      }
      context.grid = grid;
      response.clientContext = context;
      if (originalID === currentID) {
        // Only update the grid if the user has not changed rows
        grid.processFICReturn(response, data, request);
      }
      if (callbackFunction) {
        callbackFunction();
      }
    };

    if (this.viewGrid && this.viewGrid.fields) {
      gridVisibleProperties.push('id');
      len = this.viewGrid.fields.length;
      for (i = 0; i < len; i++) {
        if (this.viewGrid.fields[i].name[0] !== '_') {
          gridVisibleProperties.push(this.viewGrid.fields[i].name);
        }
      }
      sessionProperties._gridVisibleProperties = gridVisibleProperties;
    }

    OB.RemoteCallManager.call(
      'org.openbravo.client.application.window.FormInitializationComponent',
      sessionProperties,
      {
        MODE: 'EDIT',
        TAB_ID: this.tabId,
        PARENT_ID: this.getParentId(),
        ROW_ID: this.viewGrid.getSelectedRecord()
          ? this.viewGrid.getSelectedRecord().id
          : this.getCurrentValues().id
      },
      newCallback,
      null,
      errorCallbackFunction
    );
  },

  getTabMessage: function(forcedTabId) {
    var tabId = forcedTabId || this.tabId,
      callback,
      me = this;

    callback = function(resp, data, req) {
      if (
        req.clientContext &&
        window[req.clientContext.ID] === me &&
        data.type &&
        (data.text || data.title)
      ) {
        req.clientContext.messageBar.setMessage(
          isc.OBMessageBar[data.type],
          data.title,
          data.text
        );
      }
    };

    OB.RemoteCallManager.call(
      'org.openbravo.client.application.window.GetTabMessageActionHandler',
      {
        tabId: tabId
      },
      null,
      callback,
      this
    );
  },

  getFormPersonalization: function(checkSavedView) {
    if (!this.standardWindow) {
      // happens during the initialization
      return null;
    }
    return this.standardWindow.getFormPersonalization(this, checkSavedView);
  },

  // TODO: consider caching the prepared fields on
  // class level, the question is if it is faster
  // as then a clone action needs to be done
  prepareFields: function() {
    // first compute the gridfields and then the formfields
    this.prepareViewFields(this.fields);
    this.gridFields = this.prepareGridFields(this.fields);
    this.formFields = this.prepareFormFields(this.fields);
  },

  prepareFormFields: function(fields) {
    var i,
      length = fields.length,
      result = [],
      fld;

    for (i = 0; i < length; i++) {
      fld = isc.shallowClone(fields[i]);
      result.push(this.setFieldFormProperties(fld));

      if (fld.firstFocusedField) {
        this.firstFocusedField = fld.name;
      }
    }

    return result;
  },

  setFieldFormProperties: function(fld, isGridField) {
    var newShowIf;

    if (fld.displayed === false && !isGridField) {
      fld.hiddenInForm = true;
      fld.visible = false;
      fld.alwaysTakeSpace = false;
    }

    if (this.statusBarFields.contains(fld.name)) {
      fld.statusBarField = true;
    }

    if (!fld.width) {
      fld.width = '*';
    }
    if (fld.showIf && !fld.originalShowIf) {
      fld.originalShowIf = fld.showIf;
      newShowIf = function(item, value, form, values) {
        var currentValues = isc.shallowClone(
            values || form.view.getCurrentValues()
          ),
          context = form.getCachedContextInfo(),
          originalShowIfValue = false;

        OB.Utilities.fixNull250(currentValues);

        try {
          if (isc.isA.Function(this.originalShowIf)) {
            originalShowIfValue = this.originalShowIf(
              item,
              value,
              form,
              currentValues,
              context
            );
          } else {
            originalShowIfValue = isc.JSON.decode(this.originalShowIf);
          }
        } catch (_exception) {
          isc.warn(
            _exception + ' ' + _exception.message + ' ' + _exception.stack
          );
        }
        return (
          !(this.hiddenInForm && !this.statusBarField) &&
          context &&
          originalShowIfValue
        );
      };
      if (fld.statusBarField) {
        fld.showIf = null;
        fld.statusBarShowIf = newShowIf;
      } else {
        fld.showIf = newShowIf;
      }
    }
    if (fld.type === 'OBAuditSectionItem') {
      var expandAudit = OB.PropertyStore.get(
        'ShowAuditDefault',
        this.standardProperties.inpwindowId
      );
      if (expandAudit && expandAudit === 'Y') {
        fld.sectionExpanded = true;
      }
    }

    if (fld.onChangeFunction) {
      // the default
      fld.onChangeFunction.sort = 50;

      OB.OnChangeRegistry.register(
        this.tabId,
        fld.name,
        fld.onChangeFunction,
        'default'
      );
    }

    return fld;
  },

  // prepare stuff on view level
  prepareViewFields: function(fields) {
    var i,
      length = fields.length,
      fld;

    // start with the initial ones
    this.propertyToColumns = this.initialPropertyToColumns.duplicate();

    this.propertyToColumns.push({
      property: this.standardProperties.keyProperty,
      dbColumn: this.standardProperties.keyColumnName,
      inpColumn: this.standardProperties.inpKeyName,
      sessionProperty: true,
      type: this.standardProperties.keyPropertyType
    });

    for (i = 0; i < length; i++) {
      fld = fields[i];
      if (fld.columnName) {
        this.propertyToColumns.push({
          property: fld.name,
          dbColumn: fld.columnName,
          inpColumn: fld.inpColumnName,
          sessionProperty: fld.sessionProperty,
          type: fld.type
        });
      }
    }
  },

  prepareGridFields: function(fields) {
    var result = [],
      i,
      length = fields.length,
      fld,
      type,
      hoverFunction,
      yesNoFormatFunction;

    hoverFunction = function(record, value, rowNum, colNum, grid) {
      return grid.getDisplayValue(
        colNum,
        record[this.displayField ? this.displayField : this.name]
      );
    };

    yesNoFormatFunction = function(value, record, rowNum, colNum, grid) {
      return OB.Utilities.getYesNoDisplayValue(value);
    };

    for (i = 0; i < length; i++) {
      fld = fields[i];
      if (!fld.gridProps) {
        continue;
      }
      fld = isc.shallowClone(fields[i]);

      if (fld.showHover) {
        fld.hoverHTML = hoverFunction;
      }

      if (fld.gridProps.displaylength) {
        fld.gridProps.width = isc.OBGrid.getDefaultColumnWidth(
          fld.gridProps.displaylength
        );
      } else {
        fld.gridProps.width = isc.OBGrid.getDefaultColumnWidth(30);
      }

      // move the showif defined on form level
      // otherwise it interferes with the grid level
      if (fld.showIf) {
        fld.formShowIf = fld.showIf;
        delete fld.showIf;
      }

      isc.addProperties(fld, fld.gridProps);

      // if a client class/canvas field
      if (fld.clientClass) {
        if (fld.showGridSummary !== true && fld.showGridSummary !== false) {
          fld.showGridSummary = false;
        }
        if (fld.showGroupSummary !== true && fld.showGridSummary !== false) {
          fld.showGridSummary = false;
        }
      }

      // correct some stuff coming from the form fields
      if (fld.displayed === false) {
        fld.visible = true;
        fld.alwaysTakeSpace = true;
      }

      fld.canExport = fld.canExport === false ? false : true;
      fld.canHide = fld.canHide === false ? false : true;
      fld.canFilter = fld.canFilter === false ? false : true;
      fld.filterOnKeypress = fld.filterOnKeypress === false ? false : true;
      fld.escapeHTML = fld.escapeHTML === false ? false : true;
      fld.prompt = fld.title;
      fld.editorProperties = isc.addProperties(
        {},
        fld,
        isc.shallowClone(fld.editorProps)
      );
      //issue 20192: 2nd parameter is true because fld.editorProperties is a grid property.
      if (fld.editorProperties.width) {
        //Issue 26092: Avoid input icons be cropped
        delete fld.editorProperties.width;
      }
      this.setFieldFormProperties(fld.editorProperties, true);
      if (fld.disabled) {
        fld.editorProperties.disabled = true;
      }
      fld.disabled = false;

      if (fld.yesNo) {
        fld.formatCellValue = yesNoFormatFunction;
      }

      type = isc.SimpleType.getType(fld.type);
      fld.readOnlyEditorType = type.readOnlyEditorType;
      if (type.editorType && !fld.editorType) {
        fld.editorType = type.editorType;
      }

      if (type.filterEditorType && !fld.filterEditorType) {
        fld.filterEditorType = type.filterEditorType;
      }

      if (type.sortNormalizer) {
        fld.sortNormalizer = type.sortNormalizer;
      }

      if (!fld.filterEditorProperties) {
        fld.filterEditorProperties = {};
      }

      if (fld.fkField) {
        fld.displayField =
          fld.name + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER;
        fld.valueField = fld.name;
        fld.filterOnKeypress = false;
        fld.filterEditorProperties.displayField = OB.Constants.IDENTIFIER;
        fld.filterEditorProperties.valueField = OB.Constants.IDENTIFIER;
      }

      if (fld.validationFn) {
        if (!fld.validators) {
          fld.validators = [];
        }

        fld.validators.push({
          type: 'custom',
          condition: fld.validationFn
        });
      }

      fld.filterEditorProperties.required = false;

      // get rid of illegal summary functions
      if (
        fld.summaryFunction &&
        !isc.OBViewGrid.SUPPORTED_SUMMARY_FUNCTIONS.contains(
          fld.summaryFunction
        )
      ) {
        delete fld.summaryFunction;
      }

      // add grouping stuff
      if (type.inheritsFrom === 'float' || type.inheritsFrom === 'integer') {
        // this is needed because of a bug in smartclient in Listgrid
        // only groupingmodes on type level are considered
        // http://forums.smartclient.com/showthread.php?p=91605#post91605
        isc.addProperties(type, OB.Utilities.Number.Grouping);
        // so can't define on field level
        //      isc.addProperties(fld, OB.Utilities.Number.Grouping);
      }

      result.push(fld);
    }

    // sort according to displaylength, for the autoexpandfieldnames
    result.sort(function(v1, v2) {
      var t1 = v1.displaylength,
        t2 = v2.displaylength,
        l1 = v1.length,
        l2 = v2.length;
      if (!t1 && !t2) {
        return 0;
      }
      if (!t1) {
        return 1;
      }
      if (!t2) {
        return -1;
      }
      if (t1 > t2) {
        return -1;
      } else if (t1 === t2) {
        if (!l1 && !l2) {
          return 0;
        }
        if (!l1) {
          return 1;
        }
        if (!l2) {
          return -1;
        }
        if (l1 > l2) {
          return -1;
        } else if (l1 === l2) {
          if (v1.name > v2.name) {
            return 1;
          } else {
            return -1;
          }
        }
        return 1;
      }
      return 1;
    });

    this.autoExpandFieldNames = [];
    length = result.length;
    for (i = 0; i < length; i++) {
      if (result[i].autoExpand) {
        this.autoExpandFieldNames.push(result[i].name);
      }
    }
    // sort according to the sortnum
    // that's how they are displayed
    result.sort(function(v1, v2) {
      var t1 = v1.sort,
        t2 = v2.sort;
      if (!t1 && !t2) {
        return 0;
      }
      if (!t1) {
        return -1;
      }
      if (!t2) {
        return 1;
      }
      if (t1 < t2) {
        return -1;
      } else if (t1 === t2) {
        return 0;
      }
      return 1;
    });

    return result;
  },

  roleCanCreateRecords: function() {
    return (
      this.organizationFieldIsEditable() ||
      this.roleHasWriteAccessToParentRecordOrg()
    );
  },

  organizationFieldIsEditable: function() {
    var organizationField = this.fields.find('name', 'organization');
    return organizationField !== null && !organizationField.disabled;
  },

  roleHasWriteAccessToParentRecordOrg: function() {
    var parentRecordOrganization;
    if (
      !this.parentView ||
      !this.parentView.viewGrid.getSelectedRecord() ||
      this.parentView.entity === 'Organization'
    ) {
      return true;
    }
    parentRecordOrganization = this.parentView.viewGrid.getSelectedRecord()
      .organization;
    return OB.User.writableOrganizations.contains(parentRecordOrganization);
  }
});
