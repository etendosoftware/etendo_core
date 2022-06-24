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

OB.ActionButton = {};
OB.ActionButton.executingProcess = null;

isc.ClassFactory.defineClass('OBToolbarActionButton', isc.OBToolbarTextButton);

isc.OBToolbarActionButton.addProperties({
  visible: false,
  modal: true,
  contextView: null,
  labelValue: {},

  initWidget: function() {
    this.originalTitle = this.title;
    this.Super('initWidget', arguments);
  },

  action: function() {
    this.runProcess();
  },

  runProcess: function() {
    var theView = this.view,
      record,
      rowNum,
      actionObject;

    if (
      !theView.isShowingForm &&
      theView.viewGrid.getSelectedRecords() &&
      theView.viewGrid.getSelectedRecords().length === 1
    ) {
      // Keep current selection that might be lost in autosave
      record = theView.viewGrid.getSelectedRecord();
      rowNum = theView.viewGrid.getRecordIndex(record);
    }

    actionObject = {
      target: this,
      method: this.doAction,
      parameters: [rowNum]
    };

    if (this.autosave) {
      theView.standardWindow.doActionAfterAutoSave(actionObject);
    } else {
      OB.Utilities.callAction(actionObject);
    }
  },

  doAction: function(rowNum) {
    var theView = this.contextView,
      me = this,
      standardWindow = this.view.standardWindow,
      autosaveButton = this.autosave,
      param,
      allProperties,
      sessionProperties,
      callbackFunction,
      popupParams,
      errorCallback,
      parameters;
    //Modified check from 'rowNum to 'rowNum ! = null' to handle case where rowNum is 0.
    if (rowNum !== null && !theView.viewGrid.getSelectedRecord()) {
      // Current selection was lost, restore it
      theView.viewGrid.selectRecord(rowNum);
    }

    allProperties = theView.getContextInfo(false, true, false, true);
    sessionProperties = theView.getContextInfo(true, true, false, true);

    OB.ActionButton.executingProcess = this;

    for (param in allProperties) {
      // TODO: these transformations shoulnd't be needed here as soon as getContextInfo returns
      // the transformed values.
      if (
        Object.prototype.hasOwnProperty.call(allProperties, param) &&
        typeof allProperties[param] === 'boolean'
      ) {
        allProperties[param] = allProperties[param] ? 'Y' : 'N';
      }
    }

    allProperties.inpProcessId = this.processId;
    allProperties._UTCOffsetMiliseconds = OB.Utilities.Date.getUTCOffsetInMiliseconds();

    // obuiapp_process definition
    if (this.newDefinition) {
      parameters = {
        paramWindow: true,
        processId: me.processId,
        windowId: me.windowId,
        windowTitle: me.windowTitle || me.realTitle,
        actionHandler: me.command,
        button: me,
        uiPattern: me.uiPattern,
        processOwnerView: theView
      };
      if (me.uiPattern === 'M') {
        parameters.buttons = me.labelValue;
      }
      callbackFunction = function() {
        standardWindow.openProcess(parameters);
        me.opening = false; // Activate again the button
      };

      if (!me.opening) {
        me.opening = true; // To avoid button could be clicked twice
        // prevent blocking the button by setting me.opening to false if there is a problem in the request done in theView.setContextInfo
        errorCallback = function() {
          me.opening = false;
        };
        theView.setContextInfo(
          sessionProperties,
          callbackFunction,
          true,
          errorCallback
        );
      }
      return;
    }

    // ad_process definition handling
    if (this.modal) {
      allProperties.Command = this.command;
      callbackFunction = function() {
        var popup = OB.Layout.ClassicOBCompatibility.Popup.open(
          'process',
          900,
          600,
          OB.Utilities.applicationUrl(me.obManualURL),
          '',
          null,
          true,
          true,
          true,
          allProperties
        );
        if (autosaveButton) {
          // Back to header if autosave button
          popup.activeViewWhenClosed = theView;
        }
      };
    } else {
      popupParams = {
        viewId: 'OBPopupClassicWindow',
        obManualURL: this.obManualURL,
        processId: this.id,
        id: this.id,
        popup: true,
        command: this.command,
        tabTitle: this.title,
        postParams: allProperties,
        height: 600,
        width: 900
      };
      callbackFunction = function() {
        OB.Layout.ViewManager.openView('OBPopupClassicWindow', popupParams);
      };
    }

    //Force setting context info, it needs to be forced in case the current record has just been saved.
    theView.setContextInfo(sessionProperties, callbackFunction, true);
  },

  closeProcessPopup: function(newWindow, params) {
    //Keep current view for the callback function. Refresh and look for tab message.
    var contextView = OB.ActionButton.executingProcess.contextView,
      currentView = this.view,
      afterRefresh,
      isAfterRefreshAlreadyExecuted,
      parsePathPart,
      parts;

    afterRefresh = function() {
      if (isAfterRefreshAlreadyExecuted) {
        // To avoid multiple calls to this function when
        // ob-standard-view.js -> refreshCurrentRecord -> this.refreshParentRecord
        // calls again this function, since it is passed as the 'callBackFunction' argument
        return;
      }
      isAfterRefreshAlreadyExecuted = true;

      // Refresh context view
      contextView.getTabMessage();
      contextView.toolBar.refreshCustomButtons();

      if (
        contextView &&
        contextView.viewGrid &&
        contextView.viewGrid.discardAllEdits
      ) {
        // discard edits coming from FIC as they pollute the state and they're already
        // reloaded
        contextView.viewGrid.discardAllEdits();
      }

      if (
        contextView !== currentView &&
        currentView.state === isc.OBStandardView.STATE_TOP_MAX
      ) {
        // Executing an action defined in parent tab, current tab is maximized,
        // let's set half for each in order to see the message
        contextView.setHalfSplit();
      }
      if (contextView.viewGrid.isGrouped) {
        // if the grid is grouped refresh the grid to show the records properly
        contextView.viewGrid.refreshGrid();
      }
      contextView.refreshParentRecord();
      contextView.refreshChildViews();
    };

    if (this.autosave) {
      if (contextView.parentView) {
        contextView.parentView.setChildsToRefresh();
      } else {
        contextView.setChildsToRefresh();
      }

      if (contextView.viewGrid.getSelectedRecord()) {
        // There is a record selected, refresh it and its parent
        contextView.refreshCurrentRecord(afterRefresh);
      } else {
        // No record selected, refresh parent
        contextView.refreshParentRecord(afterRefresh);
      }
    } else {
      // If the button is not autosave, do not refresh but get message.
      afterRefresh();
    }

    OB.ActionButton.executingProcess = null;

    if (newWindow) {
      // Split path into protocol, server, port part and the rest (pathname, query, etc)
      parsePathPart = /^((?:[A-Za-z]+:)?\/\/[^/]+)?(\/.*)$/;
      parts = parsePathPart.exec(newWindow);
      if (parts && parts[2]) {
        newWindow = parts[2];
      }

      if (
        OB.Application.contextUrl &&
        newWindow.indexOf(OB.Application.contextUrl) !== -1
      ) {
        newWindow = newWindow.substr(
          newWindow.indexOf(OB.Application.contextUrl) +
            OB.Application.contextUrl.length -
            1
        );
      }

      if (!newWindow.startsWith('/')) {
        newWindow = '/' + newWindow;
      }

      if (newWindow.startsWith(contextView.mapping250)) {
        // Refreshing current tab, do not open it again.
        return;
      }

      var windowParams = {
        viewId: this.title,
        tabTitle: this.title,
        obManualURL: newWindow
      };
      if (params) {
        if (params.tabTitle) {
          windowParams.tabTitle = params.tabTitle;
        }
        if (params.addToRecents !== null && params.addToRecents !== undefined) {
          windowParams.addToRecents = params.addToRecents;
        }
      }
      OB.Layout.ViewManager.openView('OBClassicWindow', windowParams);
    }
  },

  updateState: function(record, hide, context, keepNonAutosave) {
    var currentValues = isc.shallowClone(
        record || this.contextView.getCurrentValues() || {}
      ),
      grid,
      buttonValue,
      label,
      buttonValues = [];
    // do not hide non autosave buttons when hidding the rest if keepNonAutosave === true
    var hideButton = hide && (!keepNonAutosave || this.autosave);

    var multiSelect = false,
      readonly,
      i,
      selection;

    grid = this.contextView.isShowingTree
      ? this.contextView.treeGrid
      : this.contextView.viewGrid;

    if (hideButton || !record) {
      multiSelect = this.multiRecord && grid.getSelectedRecords().length > 1;
      if (!multiSelect) {
        this.hide();
        return;
      }
    }

    context = context || this.contextView.getContextInfo(false, true, true);

    if (!multiSelect) {
      OB.Utilities.fixNull250(currentValues);
      OB.Utilities.fixNull250(context);

      this.visible =
        !this.displayIf ||
        (context &&
          this.displayIf(this.contextView.viewForm, currentValues, context));
      readonly =
        this.readOnlyIf &&
        context &&
        this.readOnlyIf(this.contextView.viewForm, currentValues, context);

      buttonValue = record[this.property];
      if (buttonValue === '--') {
        buttonValue = 'CL';
      }

      label = this.labelValue[buttonValue];
      if (!label) {
        if (this.realTitle) {
          label = this.realTitle;
        } else {
          label = this.title;
        }
      }
      this.realTitle = label;
      this.setTitle(label);
    } else {
      // For multi selection processes:
      //   -Button is displayed in case it should be displayed in ALL selected records
      //   -Button is readonly in case it should be readonly in ALL sected records
      selection = grid.getSelectedRecords();
      readonly = false;
      this.visible = true;
      for (i = 0; i < selection.length; i++) {
        currentValues = isc.shallowClone(selection[i]);
        OB.Utilities.fixNull250(currentValues);
        this.visible =
          this.visible &&
          (!this.displayIf ||
            (context &&
              this.displayIf(
                this.contextView.viewForm,
                currentValues,
                context
              )));
        readonly =
          readonly ||
          (this.readOnlyIf &&
            context &&
            this.readOnlyIf(this.contextView.viewForm, currentValues, context));
        buttonValue = selection[i][this.property];
        if (buttonValue === '--') {
          buttonValue = 'CL';
        }
        if (buttonValues.indexOf(buttonValue) === -1) {
          buttonValues.add(buttonValue);
        }
      }

      if (buttonValues.length === 1) {
        label = this.labelValue[buttonValues[0]];
        if (!label) {
          if (this.realTitle) {
            label = this.realTitle;
          } else {
            label = this.title;
          }
        }
        this.realTitle = label;
        this.setTitle(label);
      } else {
        label = this.originalTitle;
        this.realTitle = label;
        this.setTitle(label);
      }
    }

    // Even visible is correctly set, it is necessary to execute show() or hide()
    if (this.visible) {
      this.show();
    } else {
      this.hide();
    }
    if (readonly) {
      this.disable();
    } else {
      this.enable();
    }
  }
});
