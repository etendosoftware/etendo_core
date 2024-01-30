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
 * All portions are Copyright (C) 2012-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

isc.defineClass('OBParameterWindowView', isc.OBBaseParameterWindowView);

// == OBParameterWindowView ==
//   OBParameterWindowView is the implementation of OBBaseParameterWindowView
//   for parameter windows, this is, Process Definition with Standard UIPattern.
//   It contains a series of parameters (fields) and, optionally, a grid.
isc.OBParameterWindowView.addProperties({
  // Set later inside initWidget
  firstFocusedItem: null,

  viewGrid: null,

  addNewButton: null,

  isReport: false,
  reportId: null,
  pdfExport: false,
  xlsExport: false,
  htmlExport: false,

  gridFields: [],
  defaultsActionHandler:
    'org.openbravo.client.application.process.DefaultsProcessActionHandler',

  initWidget: function() {
    this.baseParams.processId = this.processId;

    this.Super('initWidget', arguments);

    OB.TestRegistry.register(
      'org.openbravo.client.application.ParameterWindow_' + this.processId,
      this
    );
    OB.TestRegistry.register(
      'org.openbravo.client.application.ParameterWindow_MessageBar_' +
        this.processId,
      this.messageBar
    );
    OB.TestRegistry.register(
      'org.openbravo.client.application.ParameterWindow_Form_' + this.processId,
      this.theForm
    );
    OB.TestRegistry.register(
      'org.openbravo.client.application.ParameterWindow_FormContainerLayout_' +
        this.processId,
      this.formContainerLayout
    );
    if (this.isReport) {
      if (this.pdfExport) {
        OB.TestRegistry.register(
          'org.openbravo.client.application.ParameterWindow_PDF_Export_' +
            this.processId,
          this.pdfExport
        );
      }
      if (this.xlsExport) {
        OB.TestRegistry.register(
          'org.openbravo.client.application.ParameterWindow_XLS_Export_' +
            this.processId,
          this.xlsExport
        );
      }
      if (this.htmlExport) {
        OB.TestRegistry.register(
          'org.openbravo.client.application.ParameterWindow_HTML_Export_' +
            this.processId,
          this.htmlExport
        );
      }
    } else {
      OB.TestRegistry.register(
        'org.openbravo.client.application.ParameterWindow_OK_Button_' +
          this.processId,
        this.okButton
      );
    }
  },

  buildButtonLayout: function() {
    var view = this,
      buttonLayout = [],
      newButton,
      i;

    function actionClick() {
      view.setAllButtonEnabled(false);
      view.messageBar.hide();
      if (view.theForm) {
        view.theForm.errorMessage = '';
      }
      if (view.validate()) {
        view.doProcess(this._buttonValue);
      } else {
        // If the messageBar is visible, it means that it has been set due to a custom validation inside view.validate()
        // so we don't want to overwrite it with the generic OBUIAPP_ErrorInFields message
        if (!view.messageBar.isVisible()) {
          if (view.theForm.errorMessage) {
            view.messageBar.setMessage(
              isc.OBMessageBar.TYPE_ERROR,
              null,
              OB.I18N.getLabel('OBUIAPP_FillMandatoryFields') +
                ' ' +
                view.theForm.errorMessage
            );
          } else {
            view.messageBar.setMessage(
              isc.OBMessageBar.TYPE_ERROR,
              null,
              OB.I18N.getLabel('OBUIAPP_ErrorInFields')
            );
          }
        }
        view.setAllButtonEnabled(view.allRequiredParametersSet());
      }
    }

    if (this.popup) {
      if (this.isReport) {
        if (this.pdfExport) {
          this.firstFocusedItem = this.pdfButton;
        } else if (this.xlsExport) {
          this.firstFocusedItem = this.xlsButton;
        } else if (this.htmlExport) {
          this.firstFocusedItem = this.htmlButton;
        }
      } else {
        this.firstFocusedItem = this.okButton;
      }
      buttonLayout.push(isc.LayoutSpacer.create({}));
    }

    if (this.buttons && !isc.isA.emptyObject(this.buttons)) {
      for (i in this.buttons) {
        if (Object.prototype.hasOwnProperty.call(this.buttons, i)) {
          newButton = isc.OBFormButton.create({
            title: this.buttons[i],
            realTitle: '',
            _buttonValue: i,
            click: actionClick
          });
          buttonLayout.push(newButton);
          OB.TestRegistry.register(
            'org.openbravo.client.application.process.pickandexecute.button.' +
              i,
            newButton
          );

          // pushing a spacer
          if (this.popup) {
            buttonLayout.push(
              isc.LayoutSpacer.create({
                width: 32
              })
            );
          }
        }
      }
    } else {
      if (this.isReport) {
        if (this.htmlExport) {
          this.htmlButton = isc.OBFormButton.create({
            title: OB.I18N.getLabel('OBUIAPP_HTMLExport'),
            realTitle: '',
            _buttonValue: 'HTML',
            click: actionClick
          });
          buttonLayout.push(this.htmlButton);
          if (this.popup) {
            buttonLayout.push(
              isc.LayoutSpacer.create({
                width: 32
              })
            );
          }
        }
        if (this.pdfExport) {
          this.pdfButton = isc.OBFormButton.create({
            title: OB.I18N.getLabel('OBUIAPP_PDFExport'),
            realTitle: '',
            _buttonValue: 'PDF',
            click: actionClick
          });
          buttonLayout.push(this.pdfButton);
          if (this.popup) {
            buttonLayout.push(
              isc.LayoutSpacer.create({
                width: 32
              })
            );
          }
        }
        if (this.xlsExport) {
          this.xlsButton = isc.OBFormButton.create({
            title: OB.I18N.getLabel('OBUIAPP_XLSExport'),
            realTitle: '',
            _buttonValue: 'XLS',
            click: actionClick
          });
          buttonLayout.push(this.xlsButton);
          if (this.popup) {
            buttonLayout.push(
              isc.LayoutSpacer.create({
                width: 32
              })
            );
          }
        }
      } else {
        this.okButton = isc.OBFormButton.create({
          title: OB.I18N.getLabel('OBUIAPP_Done'),
          realTitle: '',
          _buttonValue: 'DONE',
          click: actionClick
        });

        buttonLayout.push(this.okButton);
        // TODO: check if this is used, and remove as it is already registered
        OB.TestRegistry.register(
          'org.openbravo.client.application.process.pickandexecute.button.ok',
          this.okButton
        );
        if (this.popup) {
          buttonLayout.push(
            isc.LayoutSpacer.create({
              width: 32
            })
          );
        }
      }
    }

    if (this.popup) {
      this.cancelButton = isc.OBFormButton.create({
        process: this,
        title: OB.I18N.getLabel('OBUISC_Dialog.CANCEL_BUTTON_TITLE'),
        realTitle: '',
        click: function() {
          if (this.process.isExpandedRecord) {
            this.process.callerField.grid.collapseRecord(
              this.process.callerField.record
            );
          } else {
            view.closeClick();
          }
        }
      });
      buttonLayout.push(this.cancelButton);
      buttonLayout.push(isc.LayoutSpacer.create({}));
      OB.TestRegistry.register(
        'org.openbravo.client.application.ParameterWindow_Cancel_Button_' +
          this.processId,
        this.cancelButton
      );
      // TODO: check if this is used, and remove as it is already registered
      OB.TestRegistry.register(
        'org.openbravo.client.application.process.pickandexecute.button.cancel',
        this.cancelButton
      );
    }
    return buttonLayout;
  },

  handleResponse: function(
    refreshParent,
    message,
    responseActions,
    retryExecution,
    data
  ) {
    var tab = OB.MainView.TabSet.getTab(this.viewTabId),
      i,
      afterRefreshCallback,
      me = this;

    // change title to done
    if (tab) {
      tab.setTitle(
        OB.I18N.getLabel('OBUIAPP_ProcessTitle_Done', [this.tabTitle])
      );
    }

    if (data && data.showResultsInProcessView) {
      if (!this.resultLayout) {
        this.resultLayout = isc.HLayout.create({
          width: '100%',
          height: '*'
        });
        this.addMember(this.resultLayout);
      } else {
        // clear the resultLayout
        this.resultLayout.setMembers([]);
      }
    }

    this.setAllButtonEnabled(this.allRequiredParametersSet());
    this.showProcessing(false);
    if (message) {
      if (this.popup) {
        if (!retryExecution) {
          if (message.title) {
            this.buttonOwnerView.messageBar.setMessage(
              message.severity,
              message.title,
              message.text
            );
          } else {
            this.buttonOwnerView.messageBar.setMessage(
              message.severity,
              message.text
            );
          }
        } else {
          // Popup has no message bar, showing the message in a warn popup
          isc.warn(message.text);
        }
      } else {
        if (message.title) {
          this.messageBar.setMessage(
            message.severity,
            message.title,
            message.text
          );
        } else {
          this.messageBar.setMessage(message.severity, message.text);
        }
      }
    }

    if (!retryExecution) {
      this.disableFormItems();
    } else {
      // Show again all toolbar buttons so the process
      // can be called again
      if (this.toolBarLayout) {
        for (i = 0; i < this.toolBarLayout.children.length; i++) {
          if (this.toolBarLayout.children[i].show) {
            this.toolBarLayout.children[i].show();
          }
        }
      }
      if (this.popupButtons) {
        this.popupButtons.show();
      }
    }

    if (responseActions) {
      responseActions._processView = this;
      OB.Utilities.Action.executeJSON(responseActions, null, null, this);
    }

    if (this.popup && !retryExecution) {
      this.buttonOwnerView.setAsActiveView();
      afterRefreshCallback = function() {
        var selectedRecords, i;

        if (
          me.buttonOwnerView &&
          isc.isA.Function(me.buttonOwnerView.refreshParentRecord) &&
          isc.isA.Function(me.buttonOwnerView.refreshChildViews)
        ) {
          me.buttonOwnerView.refreshParentRecord();
          me.buttonOwnerView.refreshChildViews();
          me.buttonOwnerView.toolBar.updateButtonState();
        }

        if (
          me.buttonOwnerView &&
          me.buttonOwnerView.viewGrid &&
          isc.isA.Function(me.buttonOwnerView.viewGrid.getSelectedRecords) &&
          isc.isA.Function(me.buttonOwnerView.viewGrid.discardEdits)
        ) {
          selectedRecords = me.buttonOwnerView.viewGrid.getSelectedRecords();
          for (i = 0; i < selectedRecords.length; i++) {
            me.buttonOwnerView.viewGrid.discardEdits(selectedRecords[i]);
          }
        }
      };
      if (refreshParent) {
        if (this.button && this.button.multiRecord && me.buttonOwnerView.viewGrid.getSelectedRecords().length > 1) {
          this.buttonOwnerView.refresh(afterRefreshCallback);
        } else {
          if (
            this.callerField &&
            this.callerField.view &&
            typeof this.callerField.view.onRefreshFunction === 'function'
          ) {
            // In this case we are inside a process called from another process, so we want to refresh the caller process instead of the main window.
            this.callerField.view.onRefreshFunction(this.callerField.view);
          } else {
            this.buttonOwnerView.refreshCurrentRecord(afterRefreshCallback);
          }
        }
      }

      this.closeClick = function() {
        return true;
      }; // To avoid loop when "Super call"
      this.enableParentViewShortcuts(); // restore active view shortcuts before closing
      if (this.isExpandedRecord) {
        this.callerField.grid.collapseRecord(this.callerField.record);
      } else {
        this.parentElement.parentElement.closeClick(); // Super call
      }
    }
  },

  doProcess: function(btnValue) {
    var i,
      view = this,
      allProperties,
      tab,
      actionHandlerCall,
      clientSideValidationFail,
      selectedRecords,
      recordIds,
      additionalInfo;

    if (this.button && this.button.multiRecord) {
      selectedRecords = this.buttonOwnerView.viewGrid.getSelectedRecords();
      recordIds = [];
      for (i = 0; i < selectedRecords.length; i++) {
        recordIds.push(selectedRecords.get(i).id);
      }
      allProperties = {
        recordIds: recordIds
      };
    } else {
      allProperties = this.getUnderLyingRecordContext(false, true, false, true);
    }

    if (this.resultLayout && this.resultLayout.destroy) {
      this.resultLayout.destroy();
      delete this.resultLayout;
    }
    // change tab title to show executing...
    tab = OB.MainView.TabSet.getTab(this.viewTabId);
    if (tab) {
      tab.setTitle(
        OB.I18N.getLabel('OBUIAPP_ProcessTitle_Executing', [this.tabTitle])
      );
    }

    allProperties._buttonValue = btnValue || 'DONE';

    allProperties._params = this.getContextInfo();

    if (view.processOwnerView && view.processOwnerView.entity) {
      allProperties._entityName = view.processOwnerView.entity;
    } else if (view.parentWindow && view.parentWindow.view && view.parentWindow.view.entity) {
      allProperties._entityName = view.parentWindow.view.entity;
    }

    actionHandlerCall = function() {
      view.showProcessing(true);

      // allow to add external parameters
      isc.addProperties(allProperties._params, view.externalParams);
      OB.RemoteCallManager.call(
        view.actionHandler,
        allProperties,
        {
          processId: view.processId,
          reportId: view.reportId,
          windowId: view.windowId
        },
        function(rpcResponse, data, rpcRequest) {
          view.handleResponse(
            data && data.refreshParent === true,
            data && data.message,
            data && data.responseActions,
            data && data.retryExecution,
            data
          );
        }
      );
    };

    if (this.clientSideValidation) {
      clientSideValidationFail = function() {
        view.setAllButtonEnabled(view.allRequiredParametersSet());
      };
      additionalInfo = {};
      additionalInfo.buttonValue = allProperties._buttonValue;
      this.clientSideValidation(
        this,
        actionHandlerCall,
        clientSideValidationFail,
        additionalInfo
      );
    } else {
      actionHandlerCall();
    }
  }
});
