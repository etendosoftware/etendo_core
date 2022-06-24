/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distribfuted  on  an "AS IS"
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
isc.ClassFactory.defineClass('OBStandardWindow', isc.VLayout);

isc.OBStandardWindow.addClassProperties({
  // tells the window to open the first tab in new mode
  COMMAND_NEW: 'NEW'
});

// = OBStandardWindow =
//
// Represents the root container for an Openbravo window consisting of a
// hierarchy of tabs. Each tab is represented with an instance of the
// OBStandardView.
//
// The standard window can be opened as a result of a click on a link
// in another tab. In this case the window should open all tabs from the
// target tab up to the root tab. The flow starts by opening the deepest tab.
// This tab then forces the ancestor tabs to read data (asynchronously in sequence). This is
// controlled through the isOpenDirectMode flag which tells a tab that it
// should open its grid using a target record id and use the parent property
// to define the parent id by which to filter (if the tab has a parent).
//
isc.OBStandardWindow.addProperties({
  toolBarLayout: null,
  view: null,

  viewProperties: null,

  activeView: null,

  views: [],

  stackZIndex: 'firstOnTop',
  align: 'center',
  defaultLayoutAlign: 'center',

  // is set when a form or grid editing results in dirty data
  // in the window
  dirtyEditForm: null,

  allowDelete: 'Y',

  allowAttachment: 'Y',

  initWidget: function() {
    var me = this,
      callback;

    this.views = [];

    this.windowLayout = isc.VLayout.create({
      width: '100%',
      // is set by its content
      height: '100%',
      overflow: 'visible'
    });

    this.toolBarLayout = isc.HLayout.create({
      mouseDownCancelParentPropagation: true,
      width: '100%',
      // is set by its content
      height: 1,
      overflow: 'visible'
    });

    if (this.targetTabId) {
      // is used as a flag so that we are in direct link mode
      // prevents extra fetch data actions
      this.directTabInfo = {};
    }

    this.addChild(this.windowLayout);
    this.windowLayout.addMember(this.toolBarLayout);

    this.viewProperties.standardWindow = this;
    this.viewProperties.isRootView = true;
    if (this.command === isc.OBStandardWindow.COMMAND_NEW) {
      this.viewProperties.allowDefaultEditMode = false;
      this.viewProperties.deferOpenNewEdit = true;
    } else {
      this.viewProperties.deferOpenNewEdit = false;
    }

    if (OB.Utilities.checkProfessionalLicense(null, true)) {
      this.viewState = OB.PropertyStore.get(
        'OBUIAPP_GridConfiguration',
        this.windowId
      );
    } else {
      this.viewState = null;
    }

    this.allowDelete = OB.PropertyStore.get('AllowDelete', this.windowId);
    this.allowAttachment = OB.PropertyStore.get(
      'AllowAttachment',
      this.windowId
    );
    this.view = isc.OBStandardView.create(this.viewProperties);
    this.addView(this.view);
    this.windowLayout.addMember(this.view);

    this.Super('initWidget', arguments);

    // is set later after creation
    this.view.tabTitle = this.tabTitle;

    // retrieve user specific window settings from the server
    // they are stored at class level to only do the call once
    // note this if is not done inside the method as this
    // method is also called explicitly from the personalization window
    if (!this.getClass().windowSettingsRead) {
      this.readWindowSettings();
    } else if (this.getClass().windowSettingsCached) {
      callback = function() {
        me.setWindowSettings(me.getClass().windowSettingsCached);
      };
      this.fireOnPause('setWindowSettings_' + this.ID, callback);
    } else if (this.getClass().personalization) {
      this.setPersonalization(this.getClass().personalization);
    } else {
      // not applying personalization, not need to defer the form opening
      this.viewProperties.deferOpenNewEdit = false;
    }
  },

  openPopupInTab: function(
    element,
    title,
    width,
    height,
    showMinimizeControl,
    showMaximizeControl,
    showCloseControl,
    isModal
  ) {
    var prevFocusedItem = isc.EH.getFocusCanvas();
    title = title ? title : '';
    width = width ? width : '85%';
    height = height ? height : '85%';
    showMinimizeControl = showMinimizeControl ? showMinimizeControl : false;
    showMaximizeControl = showMaximizeControl ? showMaximizeControl : false;
    showCloseControl = showCloseControl ? showCloseControl : true;
    isModal = isModal !== false ? true : false;

    var dummyFirstField = isc.OBFocusButton.create({
      getFocusTarget: function() {
        return this.parentElement.children[2];
      }
    });

    var dummyMiddleField = isc.Button.create({
      title: '',
      width: 1,
      height: 1,
      border: '0px solid'
    });

    var dummyLastField = isc.OBFocusButton.create({
      getFocusTarget: function() {
        return this.parentElement.children[2];
      }
    });

    var thePopup = isc.OBPopup.create({
      width: width,
      height: height,
      title: title,
      showMinimizeButton: showMinimizeControl,
      showMaximizeButton: showMaximizeControl,
      showCloseButton: showCloseControl,
      autoSize: false,
      canDragReposition: true,
      canDragResize: true,
      keepInParentRect: true,
      itemCloseClick: function() {
        return true;
      },
      setSize: function(width, height) {
        var me = this;
        this.hide();
        // The timeouts are to avoid, as far as possible, ugly resizing effects.
        setTimeout(function() {
          me.setWidth(width);
          me.setHeight(height);
          if (isc.Browser.isWebKit) {
            // To avoid strange effect in Chrome when restoring the maximized window (it only happens odd times)
            me.parentElement.parentElement.parentElement.setWidth('99%');
            me.parentElement.parentElement.parentElement.setWidth('100%');
          }

          setTimeout(function() {
            me.show();
          }, 1);
        }, 1);
      },
      restore: function() {
        this.Super('restore', arguments);
        if (isc.Browser.isWebKit) {
          // To avoid strange effect in Chrome when restoring the maximized window (it only happens odd times)
          this.parentElement.parentElement.parentElement.setWidth('99%');
          this.parentElement.parentElement.parentElement.setWidth('100%');
        }
      },
      initWidget: function() {
        if (width.toString().indexOf('%') === -1) {
          // Smartclient to calculate the width takes into account the margin width
          this.setWidth(parseInt(width, 10) + this.edgeSize + this.edgeSize);
        }
        if (height.toString().indexOf('%') === -1) {
          // Smartclient to calculate the width takes into account the margin width
          this.setHeight(parseInt(height, 10) + this.edgeBottom + this.edgeTop);
        }
        if (this.items[0].closeClick) {
          this.itemCloseClick = function() {
            this.items[0].closeClick();
          };
        }
        this.closeClick = function() {
          this.itemCloseClick();
          this.Super('closeClick', arguments);
        };

        this.Super('initWidget', arguments);
      },
      items: [element]
    });

    if (isModal) {
      thePopup.closeClick = function() {
        thePopup.itemCloseClick();
        if (prevFocusedItem) {
          prevFocusedItem.focus();
        }
        if (this.parentElement) {
          this.parentElement.destroy();
        }
        return false;
      };
      var theModalMask = isc.Canvas.create({
        width: '100%',
        height: '100%',
        memberOverlap: '100%',
        draw: function() {
          var me = this;
          if (prevFocusedItem) {
            var myInterval;
            myInterval = setInterval(function() {
              if (me.children && prevFocusedItem === isc.EH.getFocusCanvas()) {
                if (
                  me.children[3] &&
                  me.children[3].items[0] &&
                  me.children[3].items[0].firstFocusedItem
                ) {
                  me.children[3].items[0].firstFocusedItem.focus();
                } else {
                  me.children[2].focus();
                }
              } else {
                clearInterval(myInterval);
              }
            }, 10);
          }
          this.Super('draw', arguments);
        },
        children: [
          isc.Canvas.create({
            width: '100%',
            height: '100%',
            styleName: 'OBPopupInTabModalMask'
          }),
          dummyFirstField,
          dummyMiddleField,
          thePopup,
          dummyLastField
        ]
      });
      this.addChild(theModalMask);
      // Always force to show popup over mask so when switching tabs before popup is shown,
      // it still shows in front of mask. Related to issue #42178
      thePopup.bringToFront();
    } else {
      this.addChild(thePopup);
    }
  },

  buildProcess: function(params) {
    var parts = this.getPrototype().Class.split('_'),
      len = parts.length,
      className = '_',
      originalClassName,
      processClass,
      processOwnerView,
      runningProcess;

    if (params.paramWindow) {
      className = className + params.processId;
      if (len === 3) {
        // keep original classname in case one with timestamp is not present
        originalClassName = className;

        // debug mode, we have added _timestamp
        className = className + '_' + parts[2];
      }

      processClass = isc[className] || isc[originalClassName];

      if (processClass) {
        if (params.processOwnerView) {
          processOwnerView = params.processOwnerView;
        } else {
          processOwnerView = this.getProcessOwnerView(params.processId);
        }
        runningProcess = processClass.create(
          isc.addProperties({}, params, {
            parentWindow: this,
            sourceView: this.activeView,
            buttonOwnerView: processOwnerView
          })
        );
        return runningProcess;
      } else {
        isc.warn(
          OB.I18N.getLabel('OBUIAPP_ProcessClassNotFound', [params.processId]),
          function() {
            return true;
          },
          {
            icon: '[SKINIMG]Dialog/error.png',
            title: OB.I18N.getLabel('OBUIAPP_Error')
          }
        );
      }
    }
  },

  openProcess: function(params) {
    var processOwnerView, processToBeOpened;
    if (params.uiPattern === 'M') {
      // Manual UI Pattern
      try {
        if (isc.isA.Function(params.actionHandler)) {
          params.actionHandler(params, this);
        }
      } catch (e) {
        // handling possible exceptions in manual code not to lock the application
        isc.warn(e.message);
      }
    } else {
      processToBeOpened = this.buildProcess(params);
      if (processToBeOpened) {
        processOwnerView =
          params.processOwnerView || this.getProcessOwnerView(params.processId);
        this.runningProcess = processToBeOpened;
        this.selectedState =
          processOwnerView.viewGrid &&
          processOwnerView.viewGrid.getSelectedState();
        this.openPopupInTab(
          this.runningProcess,
          params.windowTitle,
          this.runningProcess.popupWidth
            ? this.runningProcess.popupWidth
            : '90%',
          this.runningProcess.popupHeight
            ? this.runningProcess.popupHeight
            : '90%',
          this.runningProcess.showMinimizeButton
            ? this.runningProcess.showMinimizeButton
            : false,
          this.runningProcess.showMaximizeButton
            ? this.runningProcess.showMaximizeButton
            : false,
          true,
          true
        );
      }
    }
  },

  refresh: function() {
    var currentView = this.activeView,
      afterRefresh;

    afterRefresh = function() {
      // Refresh context view
      //contextView.getTabMessage();
      currentView.toolBar.refreshCustomButtons();
      //
      //      if (contextView !== currentView && currentView.state === isc.OBStandardView.STATE_TOP_MAX) {
      //        // Executing an action defined in parent tab, current tab is maximized,
      //        // let's set half for each in order to see the message
      //        contextView.setHalfSplit();
      //      }
      // Refresh in order to show possible new records
      currentView.refresh(null, false);
    };

    if (!currentView) {
      return;
    }

    if (this.selectedState) {
      currentView.viewGrid.setSelectedState(this.selectedState);
      this.selectedState = null;
    }

    if (currentView.parentView) {
      currentView.parentView.setChildsToRefresh();
    } else {
      currentView.setChildsToRefresh();
    }

    if (currentView.viewGrid.getSelectedRecord()) {
      // There is a record selected, refresh it and its parent
      currentView.refreshCurrentRecord(afterRefresh);
    } else {
      // No record selected, refresh parent
      currentView.refreshParentRecord(afterRefresh);
    }
  },

  //  Refreshes the selected records of all the window views, provided:
  //  - They belong to the entity specified in the 'entity' parameter
  //  - They are not included in the 'excludedTabIds' list
  refreshViewsWithEntity: function(entity, excludedTabIds) {
    if (
      this.view &&
      typeof this.view.refreshMeAndMyChildViewsWithEntity === 'function'
    ) {
      this.view.refreshMeAndMyChildViewsWithEntity(entity, excludedTabIds);
    }
  },

  readWindowSettings: function() {
    var standardWindow = this;

    OB.RemoteCallManager.call(
      'org.openbravo.client.application.WindowSettingsActionHandler',
      null,
      {
        windowId: this.windowId
      },
      function(response, data, request) {
        standardWindow.setWindowSettings(data);
      }
    );
  },

  // set window specific user settings, purposely set on class level
  setWindowSettings: function(data) {
    var i,
      j,
      length,
      t,
      tab,
      view,
      field,
      button,
      buttonParent, //
      st,
      stView,
      stBtns,
      stBtn,
      disabledFields,
      personalization,
      notAccessibleProcesses,
      extraCallback, //
      callbackFunc,
      alwaysReadOnly = function(view, record, context) {
        return true;
      };

    if (data) {
      this.getClass().autoSave = data.autoSave;
      this.getClass().windowSettingsRead = true;
      this.getClass().windowSettingsCached = data;
      this.getClass().uiPattern = data.uiPattern;
      this.getClass().showAutoSaveConfirmation = data.showAutoSaveConfirmation;
    }

    if (this.getClass().personalization) {
      // Don't overwrite personalization if it is already set in class
      personalization = this.getClass().personalization;
    } else if (data && data.personalization) {
      personalization = data.personalization;
    }

    if (personalization) {
      this.setPersonalization(personalization);
    }

    // set the views to readonly
    length = this.views.length;
    for (i = 0; i < length; i++) {
      this.views[i].setReadOnly(
        data.uiPattern[this.views[i].tabId] ===
          isc.OBStandardView.UI_PATTERN_READONLY
      );
      this.views[i].setSingleRecord(
        data.uiPattern[this.views[i].tabId] ===
          isc.OBStandardView.UI_PATTERN_SINGLERECORD
      );
      this.views[i].setEditOrDeleteOnly(
        data.uiPattern[this.views[i].tabId] ===
          isc.OBStandardView.UI_PATTERN_EDITORDELETEONLY
      );
      this.views[i].toolBar.updateButtonState(true);
    }

    // set as readonly not accessible processes
    if (data && data.notAccessibleProcesses) {
      for (t = 0; t < data.notAccessibleProcesses.length; t++) {
        notAccessibleProcesses = data.notAccessibleProcesses[t];
        view = this.getView(notAccessibleProcesses.tabId);
        if (!view) {
          continue;
        }
        for (i = 0; i < view.toolBar.rightMembers.length; i++) {
          button = view.toolBar.rightMembers[i];
          if (
            notAccessibleProcesses.tabId === button.contextView.tabId &&
            button.property &&
            notAccessibleProcesses.processes.contains(button.property)
          ) {
            button.readOnlyIf = alwaysReadOnly;
            // set readOnlyIf in actionToolbarButtons because it is required for
            // a good creation of buttonParents of no-active child tabs.
            if (
              button.view.actionToolbarButtons.containsProperty(
                'property',
                button.property
              )
            ) {
              for (j = 0; j < view.actionToolbarButtons.length; j++) {
                buttonParent = view.actionToolbarButtons[j];
                if (buttonParent.property === button.property) {
                  buttonParent.readOnlyIf = alwaysReadOnly;
                }
              }
            }
            // looking for this button in subtabs
            for (st = 0; st < this.views.length; st++) {
              stView = this.views[st];
              if (stView === view) {
                continue;
              }
              for (
                stBtns = 0;
                stBtns < stView.toolBar.rightMembers.length;
                stBtns++
              ) {
                stBtn = stView.toolBar.rightMembers[stBtns];
                if (
                  stBtn.contextView === button.contextView &&
                  stBtn.property &&
                  stBtn.property === button.property &&
                  notAccessibleProcesses.processes.contains(stBtn.property)
                ) {
                  stBtn.readOnlyIf = alwaysReadOnly;
                  break;
                }
              }
            }
          }
        }
      }
    }

    if (this.targetTabGrid) {
      // in direct navigation for refresh contents after applying personalizations if any
      this.targetTabGrid.refreshContents();
    }
    // Field level permissions
    if (data && data.tabs) {
      for (t = 0; t < data.tabs.length; t++) {
        tab = data.tabs[t];
        view = this.getView(tab.tabId);
        disabledFields = [];
        if (view !== null) {
          for (i = 0; i < view.formFields.length; i++) {
            field = view.formFields[i];
            if (tab.fields[field.name] !== undefined) {
              field.updatable = tab.fields[field.name];
              field.disabled = !tab.fields[field.name];
              if (!tab.fields[field.name]) {
                disabledFields.push(field.name);
              }
            }
          }
          view.disabledFields = disabledFields;
          for (i = 0; i < view.viewGrid.getFields().length; i++) {
            field = view.viewGrid.getFields()[i];
            if (tab.fields[field.name] !== undefined) {
              field.editorProperties.updatable = tab.fields[field.name];
              field.editorProperties.disabled = !tab.fields[field.name];
            }
          }
          for (i = 0; i < view.toolBar.rightMembers.length; i++) {
            button = view.toolBar.rightMembers[i];
            if (
              tab.tabId === button.contextView.tabId &&
              button.property &&
              !tab.fields[button.property]
            ) {
              button.readOnlyIf = alwaysReadOnly;
              // looking for this button in subtabs
              for (st = 0; st < this.views.length; st++) {
                stView = this.views[st];
                if (stView === view) {
                  continue;
                }
                for (
                  stBtns = 0;
                  stBtns < stView.toolBar.rightMembers.length;
                  stBtns++
                ) {
                  stBtn = stView.toolBar.rightMembers[stBtns];
                  if (
                    stBtn.contextView === button.contextView &&
                    button.property === stBtn.property &&
                    !tab.fields[stBtn.property]
                  ) {
                    stBtn.readOnlyIf = alwaysReadOnly;
                    break;
                  }
                }
              }
            }
          }
        }
      }
    }

    //Execute extraCallbacks
    if (data && data.extraCallbacks) {
      for (i = 0; i < data.extraCallbacks.length; i++) {
        extraCallback = data.extraCallbacks[i].trim();
        // extraCallback functions only allow 'data' as unique argument. If implementor just sets
        // the name of the function append the argument to complete the call.
        if (
          !extraCallback.endsWith('(data)') &&
          !extraCallback.endsWith('(data);')
        ) {
          extraCallback += '(data);';
        }
        callbackFunc = isc.Func.expressionToFunction('data', extraCallback);
        callbackFunc(data);
      }
    }
  },

  checkIfDefaultSavedView: function() {
    var persDefaultValue = OB.PropertyStore.get(
      'OBUIAPP_DefaultSavedView',
      this.windowId
    );
    if (
      persDefaultValue &&
      persDefaultValue !== 'dummyId' &&
      OB.Utilities.checkProfessionalLicense(null, true)
    ) {
      return true;
    } else {
      return false;
    }
  },

  setPersonalization: function(personalization) {
    var i,
      defaultView,
      persDefaultValue,
      views,
      currentView = this.activeView || this.view,
      length,
      formPersonalizationApplied = false;

    // only personalize if there is a professional license
    if (!OB.Utilities.checkProfessionalLicense(null, true)) {
      // open new record in form if the form opening has been deferred
      if (currentView.deferOpenNewEdit) {
        currentView.editRecord();
        this.command = null;
      }
      return;
    }

    // cache the original view so that it can be restored
    if (!this.getClass().originalView) {
      this.getClass().originalView = {
        originalView: true
      };
      this.getClass().originalView.personalizationId = 'dummyId';
      this.getClass().originalView.viewDefinition = OB.Personalization.getViewDefinition(
        this,
        '',
        false
      );
      this.getClass().originalView.viewDefinition.name = OB.I18N.getLabel(
        'OBUIAPP_StandardView'
      );
      this.getClass().originalView.canDelete = false;

      // and clone the original view so that it can't get updated accidentally
      this.getClass().originalView = isc.clone(this.getClass().originalView);
    }

    this.getClass().personalization = personalization;

    persDefaultValue = OB.PropertyStore.get(
      'OBUIAPP_DefaultSavedView',
      this.windowId
    );

    // find the default view, the personalizations are
    // returned in order of prio, then do sort by name
    views = this.getClass().personalization.views;
    if (views && isc.isA.Array(views) && views.length > 0) {
      length = views.length;

      this.getClass().personalization.views.sort(function(v1, v2) {
        var t1 = v1.viewDefinition.name,
          t2 = v2.viewDefinition.name;
        if (t1 < t2) {
          return -1;
        } else if (t1 === t2) {
          return 0;
        }
        return 1;
      });

      if (persDefaultValue !== 'dummyId') {
        if (persDefaultValue) {
          for (i = 0; i < length; i++) {
            if (persDefaultValue === views[i].personalizationId) {
              defaultView = views[i];
              break;
            }
          }
        }
        if (!defaultView) {
          for (i = 0; i < length; i++) {
            if (views[i].viewDefinition && views[i].viewDefinition.isDefault) {
              defaultView = views[i];
              break;
            }
          }
        }

        // apply the default view
        // maybe do this in a separate thread
        if (defaultView) {
          OB.Personalization.applyViewDefinition(
            defaultView.personalizationId,
            defaultView.viewDefinition,
            this
          );
        } else {
          // only apply the default form/grid if there are no views
          // otherwise you get strange interference
          // check the default form and grid viewstates
          length = this.views.length;
          for (i = 0; i < length; i++) {
            if (
              personalization.forms &&
              personalization.forms[this.views[i].tabId] &&
              this.views[i].viewForm.getDataSource()
            ) {
              this.lastViewApplied = true;
              formPersonalizationApplied = true;
              OB.Personalization.personalizeForm(
                personalization.forms[this.views[i].tabId],
                this.views[i].viewForm
              );
            }
          }
        }
      }
    }
    if (
      this.view.dataLoadDelayedForDefaultSavedView &&
      !defaultView &&
      !formPersonalizationApplied
    ) {
      // it might happen that the load of the initial grid data was delayed because it had a
      // default saved view, but then the default saved view is not returned by the WindowSettingsActionHandler.
      // in that case, detect it and load the grid now
      this.view.viewGrid.fetchData(this.view.viewGrid.getCriteria());
    }

    // restore focus as the focusitem may have been hidden now
    // https://issues.openbravo.com/view.php?id=21249
    this.setFocusInView();

    // personalization has been applied, open new record in form if the form opening has been deferred
    if (currentView.deferOpenNewEdit) {
      currentView.editRecord();
      this.command = null;
    }
  },

  // reapplies partial states that couldn't be initially applied because
  // data in client was required
  reapplyViewStates: function() {
    var i, reapp;
    if (!this.requiredReapplyViewState || !this.gridsToReapply) {
      return;
    }
    for (i = 0; i < this.gridsToReapply.length; i++) {
      reapp = this.gridsToReapply[i];
      reapp.view.setViewState(reapp.state);
    }
    delete this.requiredReapplyViewState;
    delete this.gridsToReapply;
  },

  clearLastViewPersonalization: function() {
    var p,
      personalization = this.getClass().personalization;
    delete this.lastViewApplied;
    if (personalization.forms) {
      for (p in personalization.forms) {
        if (
          Object.prototype.hasOwnProperty.call(personalization.forms, p) &&
          personalization.forms[p].personalizationId
        ) {
          OB.RemoteCallManager.call(
            'org.openbravo.client.application.personalization.PersonalizationActionHandler',
            {},
            {
              personalizationId: personalization.forms[p].personalizationId,
              action: 'delete'
            },
            null
          );
        }
      }
      delete personalization.forms;
    }
    delete this.viewState;

    // remove the grid properties
    OB.PropertyStore.set('OBUIAPP_GridConfiguration', null, this.windowId);
  },

  getDefaultGridViewState: function(tabId) {
    var views,
      length,
      i,
      personalization = this.getClass().personalization,
      defaultView,
      persDefaultValue = OB.PropertyStore.get(
        'OBUIAPP_DefaultSavedView',
        this.windowId
      );

    if (personalization && personalization.views) {
      views = personalization.views;
      length = views.length;
      if (persDefaultValue) {
        for (i = 0; i < length; i++) {
          if (persDefaultValue === views[i].personalizationId) {
            defaultView = views[i];
            break;
          }
        }
      }
      if (!defaultView) {
        for (i = 0; i < length; i++) {
          if (views[i].viewDefinition && views[i].viewDefinition.isDefault) {
            defaultView = views[i];
            break;
          }
        }
      }
    }

    if (
      defaultView &&
      defaultView.viewDefinition &&
      defaultView.viewDefinition[tabId]
    ) {
      return defaultView.viewDefinition[tabId].grid;
    }

    if (this.viewState && this.viewState[tabId]) {
      return this.viewState[tabId];
    }

    return null;
  },

  // Update the personalization record which is stored
  updateFormPersonalization: function(view, formPersonalization) {
    if (!this.getClass().personalization) {
      this.getClass().personalization = {};
    }
    if (!this.getClass().personalization.forms) {
      this.getClass().personalization.forms = [];
    }
    this.getClass().personalization.forms[view.tabId] = formPersonalization;
  },

  getFormPersonalization: function(view, checkSavedView) {
    var formPersonalization, i, persView;
    if (
      !this.getClass().personalization ||
      !this.getClass().personalization.forms
    ) {
      // no form personalization on form level
      // check window level
      if (
        checkSavedView &&
        this.getClass().personalization &&
        this.getClass().personalization.views &&
        this.selectedPersonalizationId
      ) {
        for (i = 0; i < this.getClass().personalization.views.length; i++) {
          persView = this.getClass().personalization.views[i];
          if (
            persView.viewDefinition &&
            persView.viewDefinition[view.tabId] &&
            persView.personalizationId === this.selectedPersonalizationId &&
            persView.viewDefinition[view.tabId].form
          ) {
            return persView.viewDefinition[view.tabId];
          }
        }
      }
      // nothing found go away
      return null;
    }
    formPersonalization = this.getClass().personalization.forms;
    return formPersonalization[view.tabId];
  },

  removeAllFormPersonalizations: function() {
    var i,
      updateButtons = false,
      length = this.views.length;
    if (!this.getClass().personalization) {
      return;
    }
    updateButtons = this.getClass().personalization.forms;
    if (updateButtons) {
      delete this.getClass().personalization.forms;
      for (i = 0; i < length; i++) {
        this.views[i].toolBar.updateButtonState(false);
      }
    }
  },

  isAutoSaveEnabled: function() {
    return this.getClass().autoSave;
  },

  getDirtyEditForm: function() {
    return this.dirtyEditForm;
  },

  setDirtyEditForm: function(editObject) {
    this.dirtyEditForm = editObject;
    if (!editObject) {
      this.cleanUpAutoSaveProperties();
    }
  },

  autoSave: function() {
    this.doActionAfterAutoSave(null, false);
  },

  doActionAfterAutoSave: function(
    action,
    forceDialogOnFailure,
    ignoreAutoSaveEnabled
  ) {
    var me = this,
      preSaveCallback,
      saveCallback;

    preSaveCallback = function(ok) {
      if (ok) {
        me.activeView.executePreSaveActions(function() {
          saveCallback(true);
        });
        return;
      }
      saveCallback(false);
    };

    saveCallback = function(ok) {
      var dirtyEditForm = me.getDirtyEditForm();
      if (!ok) {
        if (dirtyEditForm) {
          dirtyEditForm.resetForm();
        }
        if (action) {
          OB.Utilities.callAction(action);
        }
        return;
      }

      // If me.getDirtyEditForm() is undefined -> only for new created records that have not been modified
      // See issue https://issues.openbravo.com/view.php?id=26628
      if (!dirtyEditForm) {
        if (me.activeView && !me.activeView.isShowingForm) {
          // Look if the record is new
          if (
            me.activeView.viewGrid.getEditForm() &&
            me.activeView.viewGrid.getEditForm().isNew
          ) {
            // Set a new dirtyEditForm
            dirtyEditForm = me.activeView.viewGrid.getEditForm();
          }
        }
      }

      // if not dirty or we know that the object has errors
      if (!dirtyEditForm || (dirtyEditForm && !dirtyEditForm.validateForm())) {
        // clean up before calling the action, as the action
        // can set dirty form again
        me.cleanUpAutoSaveProperties();

        // nothing to do, execute immediately
        OB.Utilities.callAction(action);
        return;
      }

      if (action) {
        me.autoSaveAction = action;
      }

      // saving stuff already, go away
      if (me.isAutoSaving) {
        return;
      }

      if (!me.isAutoSaveEnabled() && !ignoreAutoSaveEnabled) {
        me.autoSaveConfirmAction();
        return;
      }

      me.isAutoSaving = true;
      me.forceDialogOnFailure = forceDialogOnFailure;
      if (action && action.parameters) {
        dirtyEditForm.autoSave(action.parameters);
      } else {
        dirtyEditForm.autoSave(null);
      }
    };

    if (this.getClass().autoSave && this.getClass().showAutoSaveConfirmation) {
      // Auto save confirmation required
      if (!this.getDirtyEditForm()) {
        // No changes in record, clean it up and continue
        this.cleanUpAutoSaveProperties();
        OB.Utilities.callAction(action);
        return;
      }
      if (
        this.getDirtyEditForm() &&
        this.activeView.existsAction &&
        this.activeView.existsAction(OB.EventHandlerRegistry.PRESAVE)
      ) {
        isc.ask(OB.I18N.getLabel('OBUIAPP_AutosaveConfirm'), preSaveCallback);
        return;
      }
      isc.ask(OB.I18N.getLabel('OBUIAPP_AutosaveConfirm'), saveCallback);
    } else {
      // Auto save confirmation not required: continue as confirmation was accepted
      if (
        this.getDirtyEditForm() &&
        this.activeView.existsAction &&
        this.activeView.existsAction(OB.EventHandlerRegistry.PRESAVE)
      ) {
        preSaveCallback(true);
        return;
      }
      saveCallback(true);
    }
  },

  callAutoSaveAction: function() {
    var action = this.autoSaveAction;
    this.cleanUpAutoSaveProperties();
    if (!action) {
      return;
    }
    if (
      this.activeView &&
      this.activeView.existsAction &&
      this.activeView.existsAction(OB.EventHandlerRegistry.POSTSAVE)
    ) {
      // If there exists post-save actions, the auto save action will be fired right after them
      return;
    }
    OB.Utilities.callAction(action);
  },

  cleanUpAutoSaveProperties: function() {
    delete this.dirtyEditForm;
    delete this.isAutoSaving;
    delete this.autoSaveAction;
    delete this.forceDialogOnFailure;
  },

  autoSaveDone: function(view, success) {
    if (!this.isAutoSaving) {
      this.cleanUpAutoSaveProperties();
      return;
    }

    if (success) {
      this.callAutoSaveAction();
    } else if (!view.isVisible() || this.forceDialogOnFailure) {
      isc.warn(OB.I18N.getLabel('OBUIAPP_AutoSaveError', [view.tabTitle]));
    } else if (!this.isAutoSaveEnabled()) {
      this.autoSaveConfirmAction();
    }
    this.cleanUpAutoSaveProperties();
  },

  autoSaveConfirmAction: function() {
    var action = this.autoSaveAction,
      me = this,
      callback;
    this.autoSaveAction = null;

    if (this.isAutoSaveEnabled()) {
      // clean up everything
      me.cleanUpAutoSaveProperties();
    }

    callback = function(ok) {
      delete me.inAutoSaveConfirmation;
      if (ok) {
        if (me.getDirtyEditForm()) {
          me.getDirtyEditForm().resetForm();
        }
        if (action) {
          OB.Utilities.callAction(action);
        }
      } else {
        // and focus to the first error field
        if (!me.getDirtyEditForm()) {
          me.view.setAsActiveView();
        } else {
          me.getDirtyEditForm().setFocusInErrorField(true);
          me.getDirtyEditForm().focus();
        }
      }
    };

    this.inAutoSaveConfirmation = true;
    isc.ask(
      OB.I18N.getLabel('OBUIAPP_AutoSaveNotPossibleExecuteAction'),
      callback
    );
  },

  addView: function(view) {
    view.standardWindow = this;
    this.views.push(view);
    this.toolBarLayout.addMember(view.toolBar);
    if (this.getClass().readOnlyTabDefinition) {
      view.setReadOnly(this.getClass().readOnlyTabDefinition[view.tabId]);
    }
  },

  // is called from the main app tabset. Redirects to custom viewSelected
  tabSelected: function(tabNum, tabPane, ID, tab) {
    if (this.activeView && this.activeView.setViewFocus) {
      this.activeView.setViewFocus();
    }
  },

  // is called from the main app tabset. Redirects to custom viewDeselected
  tabDeselected: function(tabNum, tabPane, ID, tab, newTab) {
    this.wasDeselected = true;
  },

  // ** {{{ selectParentTab }}} **
  //
  // Called from the main app tabset
  // Selects the parent tab of the current selected and active tab (independently of its level)
  selectParentTab: function(mainTabSet) {
    if (!this.activeView.parentView) {
      return false;
    }

    var parentTabSet = this.activeView.parentView.parentTabSet;

    if (!parentTabSet) {
      // If parentTabSet is null means that we are going to move to the top level
      parentTabSet = mainTabSet;
    }

    var parentTab = parentTabSet.getSelectedTab(),
      parentTabNum = parentTabSet.getTabNumber(parentTab),
      parentTabPane = parentTabSet.getTabPane(parentTab);

    parentTabSet.selectTab(parentTabNum);
    if (parentTabPane.setAsActiveView) {
      parentTabPane.setAsActiveView();
      isc.Timer.setTimeout(function() {
        // Inside a timeout like in itemClick case. Also to avoid a strange effect that child tab not deployed properly
        parentTabSet.doHandleClick();
      }, 0);
    } else if (parentTabPane.view.setAsActiveView) {
      parentTabPane.view.setAsActiveView();
      isc.Timer.setTimeout(function() {
        // Inside a timeout like in itemClick case. Also to avoid a strange effect that parent tab not deployed properly
        parentTabPane.view.doHandleClick();
      }, 0);
    }
  },

  // ** {{{ selectChildTab }}} **
  //
  // Called from the main app tabset
  // Selects the child tab of the current selected and active tab (independently of its level)
  selectChildTab: function(mainTabSet) {
    var childTabSet = this.activeView.childTabSet;

    // If all the subtabs are hidden due to its display logic, the child tabset will be hidden
    if (!childTabSet || childTabSet.visibility === 'hidden') {
      return false;
    }

    var childTab = childTabSet.getSelectedTab(),
      childTabNum = childTabSet.getTabNumber(childTab),
      childTabPane = childTabSet.getTabPane(childTab);

    childTabSet.selectTab(childTabNum);
    if (childTabPane.setAsActiveView) {
      childTabPane.setAsActiveView();
      isc.Timer.setTimeout(function() {
        // Inside a timeout like in itemClick case. Also to avoid a strange effect that child tab not deployed properly
        childTabSet.doHandleClick();
      }, 0);
    }
  },

  // ** {{{ selectPreviousTab }}} **
  //
  // Called from the main app tabset
  // Selects the previous tab of the current selected and active tab (independently of its level)
  selectPreviousTab: function(mainTabSet) {
    var activeTabSet = this.activeView.parentTabSet,
      previousTabVisible,
      previousTabIndex;
    if (!activeTabSet) {
      // If activeTabSet is null means that we are in the top level
      activeTabSet = mainTabSet;
    }
    var activeTab = activeTabSet.getSelectedTab(),
      activeTabNum = activeTabSet.getTabNumber(activeTab),
      activeTabPane = activeTabSet.getTabPane(activeTab);

    // Look for the next visible tab
    previousTabVisible = false;
    previousTabIndex = activeTabNum - 1;
    while (previousTabVisible === false && previousTabIndex >= 0) {
      if (!activeTabSet.tabs[previousTabIndex].pane.hidden) {
        previousTabVisible = true;
      } else {
        previousTabIndex--;
      }
    }

    if (!previousTabVisible) {
      return false;
    }

    activeTabPane = activeTabSet.getTabPane(previousTabIndex);
    if (
      activeTabPane.isRenderedChildView === false &&
      activeTabPane.setAsActiveView
    ) {
      // If it is a basic child view, set as active first in order to load the full child view
      activeTabPane.setAsActiveView();
    }

    activeTabSet.selectTab(previousTabIndex);

    // after select the new tab, activeTab related variables are updated
    activeTab = activeTabSet.getSelectedTab();
    activeTabNum = activeTabSet.getTabNumber(activeTab);
    activeTabPane = activeTabSet.getTabPane(activeTab);

    // and the new selected view is set as active
    if (activeTabPane.setAsActiveView) {
      activeTabPane.setAsActiveView();
    }
  },

  // ** {{{ selectNextTab }}} **
  //
  // Called from the main app tabset
  // Selects the next tab of the current selected and active tab (independently of its level)
  selectNextTab: function(mainTabSet) {
    var activeTabSet = this.activeView.parentTabSet,
      nextTabVisible,
      nextTabIndex;
    if (!activeTabSet) {
      // If activeTabSet is null means that we are in the top level
      activeTabSet = mainTabSet;
    }
    var activeTab = activeTabSet.getSelectedTab(),
      activeTabNum = activeTabSet.getTabNumber(activeTab),
      activeTabPane = activeTabSet.getTabPane(activeTab);

    // Look for the next visible tab
    nextTabVisible = false;
    nextTabIndex = activeTabNum + 1;
    while (
      nextTabVisible === false &&
      nextTabIndex < activeTabSet.tabs.getLength()
    ) {
      if (!activeTabSet.tabs[nextTabIndex].pane.hidden) {
        nextTabVisible = true;
      } else {
        nextTabIndex++;
      }
    }

    if (!nextTabVisible) {
      return false;
    }

    activeTabPane = activeTabSet.getTabPane(nextTabIndex);
    if (
      activeTabPane.isRenderedChildView === false &&
      activeTabPane.setAsActiveView
    ) {
      // If it is a basic child view, set as active first in order to load the full child view
      activeTabPane.setAsActiveView();
    }

    activeTabSet.selectTab(nextTabIndex);

    // after select the new tab, activeTab related variables are updated
    activeTab = activeTabSet.getSelectedTab();
    activeTabNum = activeTabSet.getTabNumber(activeTab);
    activeTabPane = activeTabSet.getTabPane(activeTab);

    // and the new selected view is set as active
    if (activeTabPane.setAsActiveView) {
      activeTabPane.setAsActiveView();
    }
  },

  closeClick: function(tab, tabSet) {
    if (
      (!this.activeView || !this.activeView.viewForm.hasChanged) &&
      this.activeView.viewForm.isNew
    ) {
      this.view.standardWindow.setDirtyEditForm(null);
    }

    var actionObject = {
      target: tabSet,
      method: tabSet.doCloseClick,
      parameters: [tab]
    };
    this.doActionAfterAutoSave(actionObject, false);
  },

  setActiveView: function(view) {
    if (!this.isDrawn()) {
      return;
    }
    if (this.activeView === view) {
      return;
    }

    var currentView = this.activeView;
    // note the new activeView must be set before disabling
    // the other one
    this.activeView = view;
    if (currentView) {
      currentView.setActiveViewProps(false);
    }
    view.setActiveViewProps(true);
  },

  setFocusInView: function(view) {
    var currentView = view || this.activeView || this.view;
    this.setActiveView(currentView);
  },

  show: function() {
    var ret = this.Super('show', arguments);
    this.setFocusInView();
    return ret;
  },

  draw: function() {
    var ret = this.Super('draw', arguments),
      i,
      length = this.views.length;
    if (this.targetTabId) {
      for (i = 0; i < length; i++) {
        if (this.views[i].tabId === this.targetTabId) {
          this.views[i].viewGrid.targetRecordId = this.targetRecordId;
          this.targetTabGrid = this.views[i].viewGrid;
          this.views[i].openDirectTabView(true);
          this.setFocusInView(this.views[i]);
          // do not refresh yet, data will be fetched after personalization
          // is applied
          break;
        }
      }
    } else if (this.command === isc.OBStandardWindow.COMMAND_NEW) {
      var currentView = this.activeView || this.view;
      if (!currentView.deferOpenNewEdit) {
        currentView.editRecord();
        this.command = null;
      }
    } else {
      this.setFocusInView(this.view);
    }

    return ret;
  },

  setViewTabId: function(viewTabId) {
    this.view.viewTabId = viewTabId;
    this.viewTabId = viewTabId;
  },

  doHandleClick: function() {
    // happens when we are getting selected
    // then don't change state
    if (this.wasDeselected) {
      this.wasDeselected = false;
      return;
    }
    this.setActiveView(this.view);
    this.view.doHandleClick();
  },

  doHandleDoubleClick: function() {
    // happens when we are getting selected
    // then don't change state
    if (this.wasDeselected) {
      this.wasDeselected = false;
      return;
    }
    this.setActiveView(this.view);
    this.view.doHandleDoubleClick();
  },

  // +++++++++++++ Methods for the main tab handling +++++++++++++++++++++
  getHelpView: function() {
    // tabTitle is set in the viewManager
    return {
      viewId: 'ClassicOBHelp',
      tabTitle: this.tabTitle + ' - ' + OB.I18N.getLabel('UINAVBA_Help'),
      windowId: this.windowId,
      windowType: 'W',
      windowName: this.tabTitle
    };
  },

  getBookMarkParams: function() {
    var result = {};
    result.windowId = this.windowId;
    result.viewId = this.getClassName();
    result.tabTitle = this.tabTitle;
    if (this.targetTabId) {
      result.targetTabId = this.targetTabId;
      result.targetRecordId = this.targetRecordId;
    }
    return result;
  },

  isEqualParams: function(params) {
    var equalTab = params.windowId && params.windowId === this.windowId;
    return equalTab;
  },

  isSameTab: function(viewName, params) {
    // always return false to force new tabs
    if (this.multiDocumentEnabled) {
      return false;
    } else if (OB.PropertyStore.get('AllowMultiTab', this.windowId) === 'Y') {
      return false;
    }
    return this.isEqualParams(params) && viewName === this.getClassName();
  },

  setTargetInformation: function(tabId, recordId) {
    this.targetTabId = tabId;
    this.targetRecordId = recordId;
    OB.Layout.HistoryManager.updateHistory();
  },

  clearTargetInformation: function() {
    this.targetTabId = null;
    this.targetRecordId = null;
    OB.Layout.HistoryManager.updateHistory();
  },

  getView: function(tabId) {
    // find is a SC extension on arrays
    return this.views.find('tabId', tabId);
  },

  storeViewState: function() {
    var result = {},
      i,
      length = this.views.length;

    if (!OB.Utilities.checkProfessionalLicense(null, true)) {
      return;
    }

    for (i = 0; i < length; i++) {
      if (this.views[i].viewGrid) {
        result[this.views[i].tabId] = this.views[i].viewGrid.getViewState();
      }
    }
    this.viewState = result;
    OB.PropertyStore.set('OBUIAPP_GridConfiguration', result, this.windowId);
  },

  getProcessOwnerView: function(processId) {
    var i,
      j,
      nActionButtons,
      nViews = this.views.length;
    for (i = 0; i < nViews; i++) {
      nActionButtons = this.views[i].actionToolbarButtons.length;
      for (j = 0; j < nActionButtons; j++) {
        if (processId === this.views[i].actionToolbarButtons[j].processId) {
          return this.views[i];
        }
      }
    }
    // If it is not found, return the header view
    return this.view;
  }
});
