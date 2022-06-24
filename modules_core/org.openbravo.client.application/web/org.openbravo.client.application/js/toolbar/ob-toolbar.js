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
 * All portions are Copyright (C) 2010-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):   Sreedhar Sirigiri (TDS), Mallikarjun M (TDS)
 ************************************************************************
 */
isc.ClassFactory.defineClass('OBToolbar', isc.ToolStrip);

isc.OBToolbar.addClassProperties({
  TYPE_SAVE: 'save',
  TYPE_SAVECLOSE: 'saveclose',
  TYPE_NEW_ROW: 'newRow',
  TYPE_NEW_DOC: 'newDoc',
  TYPE_DELETE: 'eliminate',
  TYPE_UNDO: 'undo',
  TYPE_REFRESH: 'refresh',
  TYPE_EXPORT: 'export',
  TYPE_ATTACHMENTS: 'attach',
  TYPE_CLONE: 'clone',

  SAVE_BUTTON_PROPERTIES: {
    action: function() {
      this.focus();
      this.view.savingWithShortcut = true;
      this.view.saveRow();
      delete this.view.savingWithShortcut;
    },
    disabled: true,
    buttonType: 'save',
    sortPosition: 30,
    prompt: OB.I18N.getLabel('OBUIAPP_SaveRow'),
    updateState: function() {
      var view = this.view,
        form = view.viewForm,
        hasErrors = false,
        editRow;
      if (view.isShowingForm) {
        this.setDisabled(
          !(form.isNew && form.allRequiredFieldsSet()) &&
            (form.isSaving ||
              form.readOnly ||
              !view.hasValidState() ||
              form.hasErrors() ||
              !form.hasChanged ||
              !form.allRequiredFieldsSet())
        );
      } else if (view.isEditingGrid) {
        form = view.viewGrid.getEditForm();
        editRow = view.viewGrid.getEditRow();
        hasErrors = view.viewGrid.rowHasErrors(editRow);
        this.setDisabled(
          !(form.isNew && form.allRequiredFieldsSet()) &&
            (form.isSaving ||
              form.readOnly ||
              !view.hasValidState() ||
              hasErrors ||
              form.hasErrors() ||
              !form.hasChanged ||
              !form.allRequiredFieldsSet())
        );
      } else {
        this.setDisabled(true);
      }
    },
    keyboardShortcutId: 'ToolBar_Save'
  },

  SAVECLOSE_BUTTON_PROPERTIES: {
    saveDisabled: true,
    action: function() {
      var actionObject = {
        target: this,
        method: this.saveAndClose,
        parameters: []
      };
      this.view.standardWindow.doActionAfterAutoSave(actionObject, false, true);
    },

    saveAndClose: function() {
      if (!this.saveDisabled && !this.view.viewForm.validateForm()) {
        return;
      }
      this.view.switchFormGridVisibility();
      this.view.messageBar.hide();
    },

    buttonType: 'savecloseX',
    sortPosition: 40,
    prompt: OB.I18N.getLabel('OBUIAPP_CLOSEBUTTON'),
    updateState: function() {
      var view = this.view,
        form = view.viewForm;
      if (view.isShowingForm) {
        this.setDisabled(false);
        var saveDisabled =
          !(form.isNew && form.allRequiredFieldsSet()) &&
          (form.isSaving ||
            form.readOnly ||
            !view.hasValidState() ||
            form.hasErrors() ||
            !form.hasChanged ||
            !form.allRequiredFieldsSet());
        if (saveDisabled) {
          this.buttonType = 'savecloseX';
          this.prompt = OB.I18N.getLabel('OBUIAPP_CLOSEBUTTON');
        } else {
          this.buttonType = 'saveclose';
          this.prompt = OB.I18N.getLabel('OBUIAPP_SaveClose');
        }
        this.saveDisabled = saveDisabled;
      } else {
        this.setDisabled(true);
      }
      this.resetBaseStyle();
    },
    keyboardShortcutId: 'ToolBar_SaveClose'
  },

  NEW_ROW_BUTTON_PROPERTIES: {
    action: function() {
      var view = this.view,
        grid = view.viewGrid;
      // In case of no record selected getRecordIndex(undefined) returns -1,
      // which is the top position, other case it adds bellow current selected row.
      if (grid.getSelectedRecord()) {
        view.newRow(grid.getRecordIndex(grid.getSelectedRecord()));
      } else {
        // pass in -1, as newrow will put the new row one further
        view.newRow();
      }
    },
    buttonType: 'newRow',
    sortPosition: 20,
    prompt: OB.I18N.getLabel('OBUIAPP_NewRow'),
    updateState: function() {
      var view = this.view,
        selectedRecords = view.viewGrid.getSelectedRecords(),
        form = view.viewGrid.getEditForm(),
        allFieldsSet = form && form.allRequiredFieldsSet(),
        isNew = form && form.isNew;
      this.setDisabled(
        view.viewGrid.isGrouped ||
          view.isShowingForm ||
          view.readOnly ||
          view.editOrDeleteOnly ||
          view.singleRecord ||
          !view.hasValidState() ||
          (selectedRecords && selectedRecords.length > 1) ||
          view.isShowingTree ||
          !view.roleCanCreateRecords() ||
          (isNew && !allFieldsSet)
      );
    },
    keyboardShortcutId: 'ToolBar_NewRow'
  },

  NEW_DOC_BUTTON_PROPERTIES: {
    action: function() {
      var view = this.view;
      view.newDocument();
    },
    buttonType: 'newDoc',
    sortPosition: 10,
    prompt: OB.I18N.getLabel('OBUIAPP_NewDoc'),
    updateState: function() {
      var view = this.view,
        form = view.viewForm;
      if (view.isShowingForm) {
        this.setDisabled(
          form.isSaving ||
            view.readOnly ||
            view.singleRecord ||
            !view.hasValidState() ||
            view.editOrDeleteOnly ||
            !view.roleCanCreateRecords()
        );
      } else {
        this.setDisabled(
          view.readOnly ||
            view.singleRecord ||
            !view.hasValidState() ||
            view.editOrDeleteOnly ||
            !view.roleCanCreateRecords()
        );
      }
    },
    keyboardShortcutId: 'ToolBar_NewDoc'
  },

  DELETE_BUTTON_PROPERTIES: {
    action: function() {
      this.view.deleteSelectedRows();
    },
    disabled: true,
    buttonType: 'eliminate',
    sortPosition: 60,
    prompt: OB.I18N.getLabel('OBUIAPP_DeleteRow'),
    updateState: function() {
      var view = this.view,
        form = view.viewForm,
        currentGrid,
        length,
        selectedRecords,
        i;
      if (view.isShowingTree) {
        currentGrid = view.treeGrid;
      } else {
        currentGrid = view.viewGrid;
      }
      selectedRecords = currentGrid.getSelectedRecords();
      length = selectedRecords.length;
      if (!this.view.isDeleteableTable) {
        this.setDisabled(true);
        return;
      }
      for (i = 0; i < length; i++) {
        if (!currentGrid.isWritable(selectedRecords[i])) {
          this.setDisabled(true);
          return;
        }
        if (selectedRecords[i]._new) {
          this.setDisabled(true);
          return;
        }
      }
      if (view.isShowingForm) {
        this.setDisabled(
          form.isSaving ||
            form.readOnly ||
            view.singleRecord ||
            !view.hasValidState() ||
            form.isNew ||
            view.standardWindow.allowDelete === 'N'
        );
      } else {
        this.setDisabled(
          view.readOnly ||
            view.singleRecord ||
            !view.hasValidState() ||
            !currentGrid.getSelectedRecords() ||
            currentGrid.getSelectedRecords().length === 0 ||
            view.standardWindow.allowDelete === 'N'
        );
      }
    },
    keyboardShortcutId: 'ToolBar_Eliminate'
  },

  REFRESH_BUTTON_PROPERTIES: {
    action: function() {
      var refreshChildren = true,
        callbackEnableButton,
        me = this;
      this.view.isRefreshing = true;
      // Disable the refresh button until the refresh is finished
      this.setDisabled(true);
      callbackEnableButton = function() {
        // Enable the refresh button in the callback
        delete me.view.isRefreshing;
        me.setDisabled(false);
      };
      try {
        if (this.view.isShowingForm) {
          // Refresh the form and its children records
          this.view.viewForm.refresh(callbackEnableButton, refreshChildren);
        } else {
          this.view.refresh(callbackEnableButton);
        }
      } catch (e) {
        callbackEnableButton();
      }
    },
    disabled: false,
    buttonType: 'refresh',
    sortPosition: 70,
    prompt: OB.I18N.getLabel('OBUIAPP_RefreshData'),
    updateState: function() {
      var gridVisibleAndWithValidationErrors = false;
      if (
        !this.view.isShowingForm &&
        this.view.viewGrid.gridHasValidationErrors()
      ) {
        // do not allow refreshing the grid if it has validation errors
        gridVisibleAndWithValidationErrors = true;
      }
      this.setDisabled(
        gridVisibleAndWithValidationErrors ||
          this.view.isRefreshing ||
          !this.view.hasNotChanged()
      );
    },
    keyboardShortcutId: 'ToolBar_Refresh'
  },

  UNDO_BUTTON_PROPERTIES: {
    action: function() {
      this.view.undo();
      if (!this.view.isShowingForm) {
        this.setDisabled(true);
      }
      this.view.updateSubtabVisibility();
    },
    disabled: true,
    buttonType: 'undo',
    sortPosition: 50,
    prompt: OB.I18N.getLabel('OBUIAPP_CancelEdit'),
    updateState: function() {
      if (this.view.isShowingForm) {
        this.setDisabled(false);
      } else {
        // Only enabled when the grid is being edited or when
        // the selected records have errors
        this.setDisabled(
          !this.view.isEditingGrid &&
            this.view.hasNotChanged() &&
            !this.view.viewGrid.selectedRecordHasValidationErrors()
        );
      }
    },
    keyboardShortcutId: 'ToolBar_Undo'
  },

  EXPORT_BUTTON_PROPERTIES: {
    action: function() {
      var requestProperties = {
        exportAs: 'csv',
        exportDisplay: 'download',
        _extraProperties:
          this.view &&
          this.view.dataSource &&
          this.view.dataSource.requestProperties.params._extraProperties,
        params: {
          exportToFile: true
        },
        viewState: this.view.viewGrid.getViewState(),
        tab: this.view.tabId
      };
      this.view.viewGrid.exportData(requestProperties);
    },
    disabled: false,
    buttonType: 'export',
    sortPosition: 80,
    prompt: OB.I18N.getLabel('OBUIAPP_ExportGrid'),
    updateState: function() {
      // Note that there is also logic in ob-grid that disables this button
      // when editing the filters having lazy filtering enabled
      // See https://issues.openbravo.com/view.php?id=43291 for more info
      this.setDisabled(
        this.view.isShowingForm ||
          this.view.viewGrid.getTotalRows() === 0 ||
          OB.PropertyStore.get(
            'ExportToCsv',
            this.view.standardWindow.windowId
          ) === 'N' ||
          this.view.isShowingTree ||
          this.view.viewGrid.filterHasChanged
      );
    },
    keyboardShortcutId: 'ToolBar_Export'
  },

  ATTACHMENTS_BUTTON_PROPERTIES: {
    action: function() {
      var selectedRows = this.view.viewGrid.getSelectedRecords(),
        attachmentExists = this.view.attachmentExists,
        attachmentSection = this.view.viewForm.getItem('_attachments_'),
        me = this,
        i;
      if (this.view.isShowingForm) {
        if (!attachmentSection.isExpanded()) {
          attachmentSection.expandSection();
        }
        attachmentSection.focusInItem();
        if (this.view.viewForm.parentElement) {
          // scroll after things have been expanded
          this.view.viewForm.parentElement.delayCall(
            'scrollTo',
            [null, attachmentSection.getTop()],
            100
          );
        }

        if (!attachmentExists) {
          attachmentSection.attachmentCanvasItem.canvas
            .getMember(0)
            .getMember(0)
            .click();
        }
        return;
      }
      if (selectedRows.size() === 1) {
        this.view.viewForm.setFocusItem(attachmentSection);
        this.view.viewForm.forceFocusedField = '_attachments_';
        this.view.viewForm.expandAttachments = true;
        this.view.editRecord(selectedRows[0]);

        // Move from grid view to form view could take a while.
        // Section needs to be expanded before the viewport adjustment.
        var expandedCount = 0,
          expandedInterval;
        expandedInterval = setInterval(function() {
          expandedCount += 1;
          if (attachmentSection === null) {
            // In case attachmenSection was not created try to load it again
            attachmentSection = me.view.viewForm.getItem('_attachments_');
          }
          if (attachmentSection !== null && attachmentSection.isExpanded()) {
            me.view.viewForm.parentElement.scrollTo(
              null,
              attachmentSection.getTop()
            );
            clearInterval(expandedInterval);
          }
          if (expandedCount === 50) {
            clearInterval(expandedInterval);
          }
        }, 100);

        if (!attachmentExists) {
          if (attachmentSection === null) {
            // In case attachmenSection was not created try to load it again
            attachmentSection = me.view.viewForm.getItem('_attachments_');
          }
          if (
            attachmentSection !== null &&
            attachmentSection.attachmentCanvasItem.canvas.getMember(0)
          ) {
            attachmentSection.attachmentCanvasItem.canvas
              .getMember(0)
              .getMember(0)
              .click();
          } else {
            // The first time the form view is loaded, the section is not already built and it could take a while to be.
            // Section needs to be built before the click event.
            var clickCount = 0,
              clickInterval;
            clickInterval = setInterval(function() {
              clickCount += 1;
              if (attachmentSection === null) {
                // In case attachmenSection was not created try to load it again
                attachmentSection = me.view.viewForm.getItem('_attachments_');
              }
              if (
                attachmentSection !== null &&
                attachmentSection.attachmentCanvasItem.canvas.getMember(0)
              ) {
                attachmentSection.attachmentCanvasItem.canvas
                  .getMember(0)
                  .getMember(0)
                  .click();
                clearInterval(clickInterval);
              }
              if (clickCount === 50) {
                clearInterval(clickInterval);
              }
            }, 100);
          }
        }
      } else {
        var recordIds = '';
        for (i = 0; i < selectedRows.size(); i++) {
          if (i > 0) {
            recordIds = recordIds + ',';
          }
          recordIds = recordIds + selectedRows[i].id;
        }
        var vTabId = this.view.tabId;
        var viewId = this.view.ID;
        var vbuttonId = this.ID;
        isc.confirm(
          OB.I18N.getLabel('OBUIAPP_ConfirmDownloadMultiple'),
          function(clickedOK) {
            if (clickedOK) {
              var d = {
                Command: 'DOWNLOAD_ALL',
                tabId: vTabId,
                buttonId: vbuttonId,
                recordIds: recordIds,
                viewId: viewId
              };
              OB.Utilities.postThroughHiddenForm(
                './businessUtility/TabAttachments_FS.html',
                d
              );
            }
          }
        );
      }
    },
    callback: function() {
      if (this.oldForm) {
        this.oldForm.destroy();
      }
      this.view.attachmentExists = true;
      this.customState = '';
      this.updateState();
    },
    disabled: false,
    buttonType: 'attach',
    sortPosition: 90,
    updateState: function() {
      var selectedRows = this.view.viewGrid.getSelectedRecords();
      var attachmentExists = this.view.attachmentExists;
      if (attachmentExists) {
        if (selectedRows.size() === 1) {
          this.prompt = OB.I18N.getLabel('OBUIAPP_GoToAttachments');
        } else {
          this.prompt = OB.I18N.getLabel('OBUIAPP_DownloadAttachments');
        }
        this.buttonType = 'attachExists';
      } else {
        this.prompt = OB.I18N.getLabel('OBUIAPP_CreateAttachments');
        this.buttonType = 'attach';
      }
      if (OB.PropertyStore.get('EnableScreenReader') === 'Y') {
        this.setAriaState('label', this.prompt);
      }
      if (!selectedRows || selectedRows.size() === 0) {
        // If there are now selected rows then attachments button will be disabled
        this.setDisabled(true);
      } else if (selectedRows.size() > 1 && !this.view.attachmentExists) {
        // If there are more than one rows selected, and no one has attachments,
        // then attachments button will be disabled
        this.setDisabled(true);
      } else {
        this.setDisabled(false);
      }
      this.resetBaseStyle();
      if (this.view.viewForm.readOnly && !this.view.attachmentExists) {
        this.setDisabled(true);
      }
      if (this.view.standardWindow.allowAttachment === 'N') {
        this.view.viewForm.enableAttachmentsSection(false);
        this.setDisabled(true);
      }
    },
    keyboardShortcutId: 'ToolBar_Attachments'
  },

  LINK_BUTTON_PROPERTIES: {
    action: function() {
      var url = this.view.getDirectLinkUrl();
      var form = isc.OBViewForm.create({
        width: 390,
        height: 1,
        numCols: 1,
        overflow: 'visible',
        fields: [
          {
            type: 'OBTextAreaItem',
            selectOnFocus: true,
            width: 390,
            height: 50,
            canFocus: true,
            showTitle: false,
            name: 'url',
            title: OB.I18N.getLabel('OBUIAPP_PasteLink'),
            value: url
          },
          {
            type: 'CanvasItem',
            showTitle: false,
            width: '100%',
            height: 1,
            overFlow: 'visible',
            cellStyle: 'OBFormField',
            titleStyle: 'OBFormFieldLabel',
            textBoxStyle: 'OBFormFieldInput',
            name: 'url',
            canvas: isc.Label.create({
              width: 360,
              contents: OB.I18N.getLabel('OBUIAPP_DeepLinkNote'),
              height: 1,
              overflow: 'visible'
            })
          }
        ],
        show: function() {
          var fld = this.getFields()[0];
          this.setFocusItem(fld);
          this.Super('show', arguments);
          // do the focus with a delay to give the popup time to draw
          this.fireOnPause('link_button_show', this.focus, 50, this);
        }
      });
      var dialog = OB.Utilities.createDialog(
        OB.I18N.getLabel('OBUIAPP_Document_Link')
      );
      dialog.setContent(form);
      dialog.show();
    },
    disabled: false,
    buttonType: 'link',
    sortPosition: 300,
    prompt: OB.I18N.getLabel('OBUIAPP_GetDirectLink'),
    updateState: function() {},
    keyboardShortcutId: 'ToolBar_Link'
  },
  // This offers a mechanism to add properties at runtime to buttons created through
  // templates and java
  BUTTON_PROPERTIES: {
    audit: {
      updateState: function() {
        var view = this.view,
          form = view.viewForm,
          grid = view.viewGrid;
        var selectedRecords = grid.getSelectedRecords();
        var disabled = false;
        if (selectedRecords && selectedRecords.length > 1) {
          disabled = true;
        } else if (view.isShowingForm && form.isNew) {
          disabled = true;
        } else if (view.isEditingGrid && grid.getEditForm().isNew) {
          disabled = true;
        } else if (
          (selectedRecords && selectedRecords.length > 1) ||
          (selectedRecords &&
            selectedRecords.length === 1 &&
            selectedRecords[0].updated &&
            selectedRecords[0].creationDate &&
            selectedRecords[0].updated.getTime() ===
              selectedRecords[0].creationDate.getTime())
        ) {
          disabled = true;
        }
        this.setDisabled(disabled);
      },
      keyboardShortcutId: 'ToolBar_Audit'
    },
    print: {
      updateState: function() {
        var view = this.view,
          form = view.viewForm,
          grid = view.viewGrid;
        var selectedRecords = grid.getSelectedRecords();
        var disabled = false;
        if (selectedRecords.length === 0) {
          disabled = true;
        }
        if (this.view.viewGrid.getTotalRows() === 0) {
          disabled = true;
        }
        if (view.isShowingForm && form.isNew) {
          disabled = true;
        }
        if (view.isEditingGrid && grid.getEditForm().isNew) {
          disabled = true;
        }
        /*
         * Fixes Issue 0023270. The Business partner print button provides statistics about the
         * business partner transactions in a pop up and so handling multiple records are not allowed.
         */
        if (view.tabId === '220' && selectedRecords.length > 1) {
          disabled = true;
        }
        this.setDisabled(disabled);
      },
      keyboardShortcutId: 'ToolBar_Print'
    },
    email: {
      updateState: function() {
        var view = this.view,
          form = view.viewForm,
          grid = view.viewGrid;
        var selectedRecords = grid.getSelectedRecords();
        var disabled = false;
        if (selectedRecords.length === 0) {
          disabled = true;
        }
        if (this.view.viewGrid.getTotalRows() === 0) {
          disabled = true;
        }
        if (view.isShowingForm && form.isNew) {
          disabled = true;
        }
        if (view.isEditingGrid && grid.getEditForm().isNew) {
          disabled = true;
        }
        this.setDisabled(disabled);
      },
      keyboardShortcutId: 'ToolBar_Email'
    },
    treeGrid: {
      updateState: function() {
        if (this.view) {
          this.setDisabled(this.view.isShowingForm || this.view.isEditingGrid);
        } else {
          this.setDisabled(true);
        }
      }
    },
    tree: {
      updateState: function() {
        var view = this.view,
          grid = view.viewGrid;
        var selectedRecords = grid.getSelectedRecords();
        var disabled = false;
        if (selectedRecords && selectedRecords.length === 0) {
          disabled = true;
        }
        this.setDisabled(disabled);
      }
    }
  },

  CLONE_BUTTON_PROPERTIES: {
    action: function() {
      alert('This method must be overridden when registering the button');
    },
    disabled: false,
    buttonType: 'clone',
    sortPosition: 100,
    prompt: OB.I18N.getLabel('OBUIAPP_CloneData'),
    updateState: function() {
      var view = this.view,
        form = view.viewForm,
        grid = view.viewGrid,
        selectedRecords = grid.getSelectedRecords();

      if (!view.showCloneButton) {
        this.setDisabled(true);
      } else if (view.isShowingForm && form.isNew) {
        this.setDisabled(true);
      } else if (view.isEditingGrid && grid.getEditForm().isNew) {
        this.setDisabled(true);
      } else if (
        form.hasChanged ||
        (view.isEditingGrid && grid.getEditForm().hasChanged)
      ) {
        this.setDisabled(true);
      } else {
        this.setDisabled(selectedRecords.length === 0);
      }
    },
    keyboardShortcutId: 'ToolBar_Clone'
  }
});

// = OBToolbar =
//
// The OBToolbar is the toolbar to perform common actions within a form.
//
isc.OBToolbar.addProperties({
  randomId: null,
  initWidget: function() {
    var newMembers = [],
      i = 0,
      j = 0,
      length;

    this.Super('initWidget', arguments);

    this.randomId = OB.Utilities.generateRandomString(
      8,
      true,
      true,
      true,
      false
    );

    this.members = null;

    if (!this.leftMembers || this.leftMembers.length === 0) {
      this.leftMembers = OB.ToolbarRegistry.getButtons(this.view.tabId);
    }

    newMembers[j] = isc.HLayout.create({
      width: this.leftMargin,
      height: 1
    });
    j++;

    if (this.leftMembers) {
      length = this.leftMembers.length;

      for (i = 0; i < length; i++) {
        newMembers[j] = this.leftMembers[i];

        if (
          newMembers[j].buttonType &&
          isc.OBToolbar.BUTTON_PROPERTIES[newMembers[j].buttonType]
        ) {
          isc.addProperties(
            newMembers[j],
            isc.OBToolbar.BUTTON_PROPERTIES[newMembers[j].buttonType]
          );
          if (newMembers[j].isProcessDefinition) {
          // Toolbar buttons that are of type Process Definition will not have the width set properly by default.
          // We will set the width manually to conform to the width of the rest of the toolbar buttons
            isc.addProperties(
              newMembers[j],
              { width: 51 }
            );
          }
        }

        OB.TestRegistry.register(
          'org.openbravo.client.application.toolbar.button.' +
            this.leftMembers[i].buttonType +
            '.' +
            this.view.tabId,
          this.leftMembers[i]
        );

        newMembers[j].toolBar = this;
        newMembers[j].view = this.view;
        j++;
        newMembers[j] = isc.HLayout.create({
          width: this.leftMembersMargin,
          height: 1
        });
        j++;
      }
    }

    newMembers[j] = isc.HLayout.create({
      width: '100%',
      height: 1
    });
    j++;
    newMembers[j] = isc.HLayout.create({
      width: 40,
      height: 1
    });
    j++;

    if (this.rightMembers) {
      length = this.rightMembers.length;

      for (i = 0; i < length; i++) {
        newMembers[j] = this.rightMembers[i];
        OB.TestRegistry.register(
          'org.openbravo.client.application.toolbar.button.' +
            this.rightMembers[i].property +
            '.' +
            this.view.tabId,
          this.rightMembers[i]
        );
        newMembers[j].toolBar = this;
        newMembers[j].view = this.view;
        j++;
        newMembers[j] = isc.HLayout.create({
          width: this.rightMembersMargin,
          height: 1
        });
        j++;
      }
    }

    newMembers[j] = isc.HLayout.create({
      width: this.rightMargin,
      height: 1
    });
    j++;

    this.Super('addMembers', [newMembers]);
  },

  addMems: function(m) {
    this.Super('addMembers', m);
  },

  // ** {{{ updateButtonState }}} **
  //
  // Updates the visible and disabled state of buttons using the view's form and
  // grid information.
  //
  // NOTE: new buttons should implement the updateState method.
  //
  updateButtonState: function(noSetSession, changeEvent, forceUpdate) {
    var me = this;
    if (
      forceUpdate ||
      (this.view && this.view.isActiveView && this.view.isActiveView())
    ) {
      this.fireOnPause('updateButtonState', function() {
        me.pausedUpdateButtonState(noSetSession, changeEvent);
      });
    }
  },

  pausedUpdateButtonState: function(noSetSession, changeEvent) {
    var length = this.leftMembers.length,
      i,
      form = this.view.isEditingGrid
        ? this.view.viewGrid.getEditForm()
        : this.view.viewForm;

    for (i = 0; i < length; i++) {
      if (this.leftMembers[i].updateState) {
        this.leftMembers[i].updateState();
      }
    }

    // and refresh the process toolbar buttons
    if (!changeEvent) {
      this.refreshCustomButtons(noSetSession);
    } else if (this.rightMembers) {
      // determine if the buttons should be hidden or not
      if (this.view.isEditingGrid || this.view.isShowingForm) {
        if (form.hasErrors() || !form.allRequiredFieldsSet()) {
          this.hideShowRightMembers(false, noSetSession);
        } else {
          this.hideShowRightMembers(true, noSetSession);
        }
      } else {
        this.hideShowRightMembers(true, noSetSession);
      }
    }
  },

  // ** {{{ getLeftMember(member) }}} **
  //
  // It works just for left side members.
  // Given a numerical index or a left member type, return a pointer to the appropriate left member.
  // If passed a left member Canvas, just returns it.
  // If not found, returns undefined
  //
  // Parameters:
  // * {{{member}}} type: String | Number | Canvas - identifier for the required left member.
  //
  // Returns:  type: Canvas - left member widget.
  getLeftMember: function(member) {
    var i = 0,
      length = this.leftMembers.length;
    if (typeof member === 'number') {
      if (member >= 0 && member < this.leftMembers.length) {
        return this.leftMembers[member];
      }
    } else if (typeof member === 'string') {
      for (i = 0; i < length; i++) {
        if (this.leftMembers[i].buttonType === member) {
          return this.leftMembers[i];
        }
      }
    } else if (typeof member === 'object') {
      for (i = 0; i < length; i++) {
        if (this.leftMembers[i] === member) {
          return this.leftMembers[i];
        }
      }
    }
    return;
  },

  // ** {{{ getLeftMembers() }}} **
  //
  // It works just for left side members.
  // Get the Array of left members.
  // NOTE: the returned array should not be modified.
  //
  // Returns: type: Array - the Array of left members.
  getLeftMembers: function() {
    return this.leftMembers;
  },

  // ** {{{ getLeftMember(attribute, value) }}} **
  //
  // It works just for left side members.
  // Given a attribute an its value, return an array of matching left members.
  // If no matches, returns an empty array.
  //
  // Parameters:
  // * {{{attribute}}} type: String - attribute for search.
  // * {{{value}}} type: String | Number | Canvas - desired value of the attribute.
  //
  // Returns: type: Array - the Array of matching left members.
  getLeftMembersByAttribute: function(attribute, value) {
    var members = [],
      i = 0,
      length = this.leftMembers.length;
    for (i = 0; i < length; i++) {
      if (this.leftMembers[i][attribute] === value) {
        members.push(this.leftMembers[i]);
      }
    }
    return members;
  },

  // ** {{{ getLeftMemberNumber(member) }}} **
  //
  // It works just for left side members.
  // Given a left member Canvas, return its position.
  // If no matches, returns -1.
  //
  // Parameters:
  // * {{{member}}} type: Canvas - left member Canvas to obtain its position.
  //
  // Returns: type: Number - the left member Canvas position (starting from 0).
  getLeftMemberNumber: function(member) {
    var i = 0,
      length = this.leftMembers.length;
    for (i = 0; i < length; i++) {
      if (this.leftMembers[i] === member) {
        return i;
      }
    }
    return -1;
  },

  // ** {{{ removeLeftMembers(members) }}} **
  //
  // It works just for left side members.
  // Removes the specified left members from the layout.
  //
  // Parameters:
  // * {{{members}}} type: Array | Canvas - array of left members to be removed, or reference to single left member.
  removeLeftMembers: function(members) {
    var oldMembersSorted = [],
      oldArray = [],
      position = 0,
      length,
      i = 0,
      sortFunc;

    sortFunc = function(a, b) {
      return a - b;
    };

    if (
      !(
        typeof members.length === 'number' &&
        !Object.prototype.propertyIsEnumerable.call(members, 'length') &&
        typeof members.splice === 'function'
      )
    ) {
      members = [members];
    }
    length = members.length;
    for (i = 0; i < length; i++) {
      /* Clean-up of the given input and sort */
      if (typeof members[i] !== 'number') {
        members[i] = this.getLeftMemberNumber(members[i]);
      }
      if (members[i] <= this.leftMembers.length && members[i] !== -1) {
        oldMembersSorted[oldMembersSorted.length] = members[i];
      }
      oldMembersSorted = oldMembersSorted.sort(sortFunc);
    }
    length = oldMembersSorted.length;
    for (i = 0; i < length; i++) {
      /* Generate an array to determine which elements visually will be removed */
      position = oldMembersSorted[i];
      position = position * 2;
      position = position + 1;
      oldArray.push(position, position + 1);
    }
    oldMembersSorted = oldMembersSorted.reverse();

    length = oldMembersSorted.length;
    for (i = 0; i < length; i++) {
      /* Update the 'leftMembers' array */
      this.leftMembers.splice(oldMembersSorted[i], 1);
    }
    this.destroyAndRemoveMembers(
      oldArray
    ); /* Remove visually the desired elements */
  },

  // ** {{{ removeAllLeftMembers() }}} **
  //
  // It works just for left side members.
  // Removes all left members from the layout.
  //
  removeAllLeftMembers: function() {
    var membersNumArray = [],
      i = 0,
      length = this.leftMembers.length;
    for (i = 0; i < length; i++) {
      membersNumArray.push(i);
    }
    this.removeLeftMembers(membersNumArray);
  },

  // ** {{{ addLeftMembers(newMembers, position) }}} **
  //
  // It works just for left side members.
  // Add one or more canvases to the left side of the toolbar, optionally at specific position.
  //
  // Parameters:
  // * {{{newMembers}}} type: Array || Object - array of canvases to be added, or reference to single canvas.
  // * {{{position (optional)}}} type: Number - position to add newMembers; if omitted newMembers will be added at the last position.
  addLeftMembers: function(newMembers, position) {
    var i = 0,
      length;
    if (
      !(
        typeof newMembers.length === 'number' &&
        !Object.prototype.propertyIsEnumerable.call(newMembers, 'length') &&
        typeof newMembers.splice === 'function'
      )
    ) {
      newMembers = [newMembers];
    }
    if (position > this.leftMembers.length || typeof position === 'undefined') {
      position = this.leftMembers.length;
    }
    length = newMembers.length;
    for (i = 0; i < length; i++) {
      this.leftMembers.splice(position + i, 0, newMembers[i]);
    }
    position = position * 2;
    position = position + 1;
    length = newMembers.length;
    for (i = 0; i < length; i++) {
      this.Super('addMembers', [newMembers[i], position]);
      position = position + 1;
      this.Super('addMembers', [
        isc.HLayout.create({
          width: this.leftMembersMargin,
          height: 1
        }),
        position
      ]);
      position = position + 1;
    }
  },

  // ** {{{ setLeftMembers(newMembers, position) }}} **
  //
  // It works just for left side members.
  // Set/Display one or more canvases to the left side of the toolbar, optionally at specific position; if any exists, it will be deleted.
  //
  // Parameters:
  // * {{{newMembers}}} type: Array || Object - array of canvases to be displayed, or reference to single canvas.
  setLeftMembers: function(newMembers) {
    this.removeAllLeftMembers();
    this.addLeftMembers(newMembers);
  },

  // ** {{{ setLeftMemberDisabled(member, state) }}} **
  //
  // It works just for left side members.
  // Set the disabled state of this left member.
  //
  // Parameters:
  // * {{{member}}} type: String | Number | Canvas - identifier for the left member to perform the action.
  // * {{{state}}} type: Boolean - new disabled state of this object; pass true to disable the left member.
  setLeftMemberDisabled: function(member, state) {
    member = this.getLeftMember(member);
    if (member) {
      member.setDisabled(state);
    }
    return;
  },

  // ** {{{ setLeftMemberSelected(member, state) }}} **
  //
  // It works just for left side members.
  // Set the selected state of this left member.
  //
  // Parameters:
  // * {{{member}}} type: String | Number | Canvas - identifier for the left member to perform the action.
  // * {{{state}}} type: Boolean - new disabled state of this object; pass true to select the left member.
  setLeftMemberSelected: function(member, state) {
    member = this.getLeftMember(member);
    if (member) {
      member.setSelected(state);
    }
    return;
  },

  // ** {{{ getRightMember(member) }}} **
  //
  // It works just for right side members.
  // Given a numerical index or a right member ID, return a pointer to the appropriate right member.
  // If passed a right member Canvas, just returns it.
  // If not found, returns undefined.
  //
  // Parameters:
  // * {{{member}}} type: String | Number | Canvas - identifier for the required right member.
  //
  // Returns: type: Canvas - right member widget.
  getRightMember: function(member) {
    var i = 0,
      length = this.rightMembers.length;
    if (typeof member === 'number') {
      if (member >= 0 && member < this.rightMembers.length) {
        return this.rightMembers[member];
      }
    } else if (typeof member === 'string') {
      for (i = 0; i < length; i++) {
        if (this.rightMembers[i].ID === member) {
          return this.rightMembers[i];
        }
      }
    } else if (typeof member === 'object') {
      for (i = 0; i < length; i++) {
        if (this.rightMembers[i] === member) {
          return this.rightMembers[i];
        }
      }
    }
    return;
  },

  // ** {{{ getRightMembers() }}} **
  //
  // It works just for right side members.
  // Get the Array of right members.
  // NOTE: the returned array should not be modified.
  //
  // Returns: type: Array - the Array of right members.
  getRightMembers: function() {
    return this.rightMembers;
  },

  // ** {{{ getRightMembersByAttribute(attribute, value) }}} **
  //
  // It works just for right side members.
  // Given a attribute an its value, return an array of matching right members.
  // If no matches, returns an empty array.
  //
  // Parameters:
  // * {{{attribute}}} type: String - attribute for search.
  // * {{{value}}} type: String | Number | Canvas - desired value of the attribute.
  //
  // Returns: type: Array - the Array of matching right members.
  getRightMembersByAttribute: function(attribute, value) {
    var members = [],
      i = 0,
      length = this.rightMembers.length;
    for (i = 0; i < length; i++) {
      if (this.rightMembers[i][attribute] === value) {
        members.push(this.rightMembers[i]);
      }
    }
    return members;
  },

  // ** {{{ getRightMemberNumber(member) }}} **
  //
  // It works just for right side members.
  // Given a right member Canvas, return its position.
  // If no matches, returns -1.
  //
  // Parameters:
  // * {{{member}}} type: Canvas - right member Canvas to obtain its position.
  //
  // Returns: type: Number - the right member Canvas position (starting from 0).
  getRightMemberNumber: function(member) {
    var i = 0,
      length = this.rightMembers.length;
    for (i = 0; i < length; i++) {
      if (this.rightMembers[i] === member) {
        return i;
      }
    }
    return -1;
  },

  // ** {{{ removeRightMembers(members) }}} **
  //
  // It works just for right side members.
  // Removes the specified right members from the layout.
  //
  // Parameters:
  // * {{{members}}} type: Array | Canvas - array of right members to be removed, or reference to single right member.
  removeRightMembers: function(members) {
    var oldMembersSorted = [],
      length,
      oldArray = [],
      position = 0,
      i = 0,
      sortFunc;

    sortFunc = function(a, b) {
      return a - b;
    };
    if (
      !(
        typeof members.length === 'number' &&
        !Object.prototype.propertyIsEnumerable.call(members, 'length') &&
        typeof members.splice === 'function'
      )
    ) {
      members = [members];
    }
    length = members.length;
    for (i = 0; i < length; i++) {
      /* Clean-up of the given input and sort */
      if (typeof members[i] !== 'number') {
        members[i] = this.getRightMemberNumber(members[i]);
      }
      if (members[i] <= this.rightMembers.length && members[i] !== -1) {
        oldMembersSorted[oldMembersSorted.length] = members[i];
      }
      oldMembersSorted = oldMembersSorted.sort(sortFunc);
    }

    length = oldMembersSorted.length;
    for (i = 0; i < length; i++) {
      /* Generate an array to determine which elements visually will be removed */
      position = oldMembersSorted[i];
      position = position * 2;
      position = position + 3;
      position = position + this.leftMembers.length * 2;
      oldArray.push(position, position + 1);
    }
    oldMembersSorted = oldMembersSorted.reverse();
    length = oldMembersSorted.length;
    for (i = 0; i < length; i++) {
      /* Update the 'rightMembers' array */
      this.rightMembers.splice(oldMembersSorted[i], 1);
    }
    this.destroyAndRemoveMembers(
      oldArray
    ); /* Remove visually the desired elements */
  },

  // ** {{{ removeAllRightMembers() }}} **
  //
  // It works just for right side members.
  // Removes all right members from the layout.
  //
  removeAllRightMembers: function() {
    var membersNumArray = [],
      i = 0,
      length = this.rightMembers.length;
    for (i = 0; i < length; i++) {
      membersNumArray.push(i);
    }
    this.removeRightMembers(membersNumArray);
  },

  // ** {{{ addRightMembers(newMembers, position) }}} **
  //
  // It works just for right side members.
  // Add one or more canvases to the right side of the toolbar, optionally at specific position.
  //
  // Parameters:
  // * {{{newMembers}}} type: Array || Object - array of canvases to be added, or reference to single canvas.
  // * {{{position (optional)}}} type: Number - position to add newMembers; if omitted newMembers will be added at the last position.
  addRightMembers: function(newMembers, position) {
    var i = 0,
      length;
    if (
      !(
        typeof newMembers.length === 'number' &&
        !Object.prototype.propertyIsEnumerable.call(newMembers, 'length') &&
        typeof newMembers.splice === 'function'
      )
    ) {
      newMembers = [newMembers];
    }
    if (
      position > this.rightMembers.length ||
      typeof position === 'undefined'
    ) {
      position = this.rightMembers.length;
    }

    length = newMembers.length;
    for (i = 0; i < length; i++) {
      this.rightMembers.splice(position + i, 0, newMembers[i]);
    }
    position = position * 2;
    position = position + 3;
    position = position + this.leftMembers.length * 2;

    length = newMembers.length;
    for (i = 0; i < length; i++) {
      this.Super('addMembers', [newMembers[i], position]);
      position = position + 1;
      this.Super('addMembers', [
        isc.HLayout.create({
          width: this.rightMembersMargin,
          height: 1
        }),
        position
      ]);
      position = position + 1;
    }
  },

  // ** {{{ setRightMembers(newMembers, position) }}} **
  //
  // It works just for right side members.
  // Set/Display one or more canvases to the right side of the toolbar, optionally at specific position; if any exists, it will be deleted.
  //
  // Parameters:
  // * {{{newMembers}}} type: Array || Object - array of canvases to be displayed, or reference to single canvas.
  setRightMembers: function(newMembers) {
    this.removeAllRightMembers();
    this.addRightMembers(newMembers);
  },

  // ** {{{ setRightMemberDisabled(member, state) }}} **
  //
  // It works just for right side members.
  // Set the disabled state of this right member.
  //
  // Parameters:
  // * {{{member}}} type: String | Number | Canvas - identifier for the right member to perform the action.
  // * {{{state}}} type: Boolean - new disabled state of this object; pass true to disable the right member.
  setRightMemberDisabled: function(member, state) {
    member = this.getRightMember(member);
    if (member) {
      member.setDisabled(state);
    }
    return;
  },

  // ** {{{ setRightMemberSelected(member, state) }}} **
  //
  // It works just for right side members.
  // Set the selected state of this right member.
  //
  // Parameters:
  // * {{{member}}} type: String | Number | Canvas - identifier for the right member to perform the action.
  // * {{{state}}} type: Boolean - new disabled state of this object; pass true to select the right member.
  setRightMemberSelected: function(member, state) {
    member = this.getRightMember(member);
    if (member) {
      member.setSelected(state);
    }
    return;
  },

  // ** {{{ refreshCustomButtons }}} **
  //
  // Refreshes all the custom buttons in the toolbar based on current record selection
  //
  refreshCustomButtons: function(noSetSession) {
    var selectedRecords, multipleSelectedRowIds, allProperties, i;

    var currentTabCalled = false,
      me = this,
      requestParams;

    var buttons = this.getRightMembers(),
      buttonContexts = [],
      currentContext,
      buttonsByContext = [],
      length,
      iButtonContext,
      callbackHandler;

    function doRefresh(
      buttons,
      currentValues,
      hideAllButtons,
      noneOrMultipleRecordsSelected,
      me
    ) {
      var i,
        length = me.rightMembers.length;
      for (i = 0; i < length; i++) {
        // To disable any button previous defined keyboard shortcut
        me.rightMembers[i].disableShortcut();
      }
      length = buttons.length;
      for (i = 0; i < length; i++) {
        if (buttons[i].updateState) {
          buttons[i].updateState(
            currentValues,
            hideAllButtons,
            null,
            !noneOrMultipleRecordsSelected
          );
        }
      }
      length = me.leftMembers.length;
      for (i = 0; i < length; i++) {
        if (me.leftMembers[i].updateState) {
          me.leftMembers[i].updateState();
        }
      }
      if (me.view.isActiveView()) {
        me.defineRightMembersShortcuts(); // To re-calculate the target key for keyboard shortcuts
        length = me.rightMembers.length;
        for (i = 0; i < length; i++) {
          me.rightMembers[i].enableShortcut(); // To enable each button keyboard shortcut
        }
      }
    }

    if (buttons.length !== 0) {
      length = buttons.length;
      for (i = 0; i < length; i++) {
        if (!currentContext || currentContext !== buttons[i].contextView) {
          // Adding new context
          currentContext = buttons[i].contextView;
          buttonContexts.push(currentContext);
          buttonsByContext[currentContext] = [];
        }
        buttonsByContext[currentContext].push(buttons[i]);
      }

      // This is needed to prevent JSLint complaining about "Don't make functions within a loop.
      callbackHandler = function(currentContext, me) {
        return function(response, data, request) {
          var grid = currentContext.isShowingTree
              ? currentContext.treeGrid
              : currentContext.viewGrid,
            noneOrMultipleRecordsSelected =
              grid.getSelectedRecords().length !== 1;
          var sessionAttributes = data.sessionAttributes,
            auxInputs = data.auxiliaryInputValues,
            attachmentExists = data.attachmentExists,
            prop;
          if (sessionAttributes) {
            currentContext.viewForm.sessionAttributes = sessionAttributes;
          }

          if (auxInputs) {
            this.auxInputs = {};
            for (prop in auxInputs) {
              if (Object.prototype.hasOwnProperty.call(auxInputs, prop)) {
                currentContext.viewForm.setValue(prop, auxInputs[prop].value);
                currentContext.viewForm.auxInputs[prop] = auxInputs[prop].value;
              }
            }
          }
          currentContext.viewForm.view.attachmentExists = attachmentExists;
          doRefresh(
            buttonsByContext[currentContext],
            currentContext.getCurrentValues() || {},
            noneOrMultipleRecordsSelected,
            noneOrMultipleRecordsSelected,
            me
          );
          //compute and apply tab display logic again after fetching auxilary inputs.
          currentContext.handleDefaultTreeView();
          currentContext.updateSubtabVisibility();
        };
      };

      length = buttonContexts.length;
      for (iButtonContext = 0; iButtonContext < length; iButtonContext++) {
        currentContext = buttonContexts[iButtonContext];

        if (currentContext.isShowingTree) {
          selectedRecords = currentContext.treeGrid.getSelectedRecords() || [];
        } else {
          selectedRecords = currentContext.viewGrid.getSelectedRecords() || [];
        }
        var numOfSelRecords = 0,
          theForm = this.view.isEditingGrid
            ? this.view.viewGrid.getEditForm()
            : this.view.viewForm,
          isNew = currentContext.viewForm.isNew,
          hideAllButtons =
            selectedRecords.size() === 0 && !currentContext.isShowingForm,
          currentValues = currentContext.getCurrentValues(),
          gridVisibleProperties = [],
          len;

        if (
          !hideAllButtons &&
          (this.view.isEditingGrid || this.view.isShowingForm)
        ) {
          hideAllButtons =
            theForm.hasErrors() || !theForm.allRequiredFieldsSet();
        }
        if (hideAllButtons) {
          this.hideShowRightMembers(false, noSetSession);
        }

        if (currentContext.viewGrid.getSelectedRecords()) {
          numOfSelRecords = currentContext.viewGrid.getSelectedRecords().length;
        }

        var noneOrMultipleRecordsSelected = numOfSelRecords !== 1 && !isNew;

        if (
          currentValues &&
          !noSetSession &&
          !currentContext.isShowingForm &&
          !isNew &&
          !hideAllButtons &&
          currentContext.ID === this.view.ID
        ) {
          if (this.view.tabId === currentContext.tabId) {
            currentTabCalled = true;
          }
          // Call FIC to obtain possible session attributes and set them in form
          requestParams = {
            MODE: 'SETSESSION',
            PARENT_ID: currentContext.getParentId(),
            TAB_ID: currentContext.tabId,
            ROW_ID: currentValues.id
          };
          multipleSelectedRowIds = [];
          allProperties = currentContext.getContextInfo(
            false,
            true,
            false,
            true
          );
          if (selectedRecords.size() > 1) {
            for (i = 0; i < selectedRecords.size(); i++) {
              multipleSelectedRowIds[i] = selectedRecords[i].id;
            }
            allProperties.MULTIPLE_ROW_IDS = multipleSelectedRowIds;
          }
          if (currentContext.viewGrid && currentContext.viewGrid.fields) {
            len = currentContext.viewGrid.fields.length;
            for (i = 0; i < len; i++) {
              if (currentContext.viewGrid.fields[i].name[0] !== '_') {
                gridVisibleProperties.push(
                  currentContext.viewGrid.fields[i].name
                );
              }
            }
            allProperties._gridVisibleProperties = gridVisibleProperties;
          }
          OB.RemoteCallManager.call(
            'org.openbravo.client.application.window.FormInitializationComponent',
            allProperties,
            requestParams,
            callbackHandler(currentContext, me)
          );
        } else {
          doRefresh(
            buttonsByContext[currentContext],
            currentValues || {},
            hideAllButtons || noneOrMultipleRecordsSelected,
            numOfSelRecords !== 1,
            this
          );
        }
      }
    }
    if (
      !currentTabCalled &&
      !noSetSession &&
      !this.view.isShowingForm &&
      !this.view.viewForm.isNew &&
      this.view.viewGrid.getSelectedRecords().size() !== 0
    ) {
      selectedRecords = this.view.viewGrid.getSelectedRecords();
      //The call to the FIC for the current tab was not done (probably because it doesn't have buttons, or the buttons do not depend on session vars/aux ins.
      //However, a call still needs to be done, to set the attachments information
      requestParams = {
        MODE: 'SETSESSION',
        PARENT_ID: this.view.getParentId(),
        TAB_ID: this.view.tabId
      };
      multipleSelectedRowIds = [];
      allProperties = this.view.getContextInfo(false, true, false, true);
      if (selectedRecords.size() >= 1) {
        for (i = 0; i < selectedRecords.size(); i++) {
          if (i === 0) {
            requestParams.ROW_ID = selectedRecords[i].id;
          }
          multipleSelectedRowIds[i] = selectedRecords[i].id;
        }
        if (selectedRecords.size() > 1) {
          allProperties.MULTIPLE_ROW_IDS = multipleSelectedRowIds;
        }
      }
      OB.RemoteCallManager.call(
        'org.openbravo.client.application.window.FormInitializationComponent',
        allProperties,
        requestParams,
        function(response, data, request) {
          var attachmentExists = data.attachmentExists,
            auxInputs = data.auxiliaryInputValues,
            prop;
          me.view.attachmentExists = attachmentExists;

          // Added sessionAttributes to updateSubtabVisibility
          me.view.viewForm.sessionAttributes = data.sessionAttributes;
          if (auxInputs) {
            this.auxInputs = {};
            for (prop in auxInputs) {
              if (Object.prototype.hasOwnProperty.call(auxInputs, prop)) {
                me.view.viewForm.setValue(prop, auxInputs[prop].value);
                me.view.viewForm.auxInputs[prop] = auxInputs[prop].value;
              }
            }
          }
          me.view.handleDefaultTreeView();
          me.view.updateSubtabVisibility();

          //Call to refresh the buttons. As its called with noSetSession=true, it will not cause an infinite recursive loop
          me.updateButtonState(true);
        }
      );
    }
  },

  hideShowRightMembers: function(show, noSetSession) {
    var i, button, context;
    // if showing make sure that they are not always shown
    if (show) {
      this.refreshCustomButtons(noSetSession);
    } else {
      for (i = 0; i < this.rightMembers.length; i++) {
        button = this.rightMembers[i];
        if (button.autosave) {
          button.hide();
        } else {
          // do not hide non autosave buttons, keep them in case display logic allows it
          context = button.contextView;
          button.updateState(
            context.getCurrentValues(),
            false,
            context.getContextInfo(false, true, true)
          );
        }
      }
    }
  },

  // ** {{{ refreshCustomButtonsView }}} **
  //
  // Refreshes all the custom buttons in the toolbar having contextView === view
  // Used to update state of buttons dynamically on field change
  //
  refreshCustomButtonsView: function(view) {
    var i,
      context = view.getContextInfo(false, true, true),
      length;

    length = this.rightMembers.length;
    for (i = 0; i < length; i++) {
      if (this.rightMembers[i].contextView === view) {
        this.rightMembers[i].updateState(
          view.getCurrentValues(),
          false,
          context
        );
      }
    }
  },

  visibilityChanged: function(state) {
    if (state) {
      this.enableShortcuts();
    } else {
      this.disableShortcuts();
    }
  },

  draw: function() {
    this.Super('draw', arguments);
    this.defineRightMembersShortcuts();
    this.enableShortcuts();
  },

  rightMembersShortcuts: [],

  defineRightMembersShortcuts: function() {
    var i, j, k, length, titleLength;

    function isAssignedCharacter(character, me) {
      var n, length;
      if (character === ' ') {
        return true;
      }
      character = character.toString();
      character = character.toUpperCase();
      length = me.rightMembersShortcuts.length;
      for (n = 0; n < length; n++) {
        if (me.rightMembersShortcuts[n][0] === character) {
          return true;
        }
      }
      return false;
    }

    this.rightMembersShortcuts = [];
    length = this.rightMembers.length;
    for (i = 0; i < length; i++) {
      var title = this.rightMembers[i].realTitle,
        haveToContinue = true;
      this.rightMembersShortcuts[i] = [];
      if (haveToContinue) {
        // Check if free character and assign
        haveToContinue = true;
        titleLength = title.length;
        for (j = 0; j < titleLength; j++) {
          if (!isAssignedCharacter(title.substring(j, j + 1), this)) {
            this.rightMembersShortcuts[i][0] = title
              .substring(j, j + 1)
              .toUpperCase();
            this.rightMembersShortcuts[i][1] = j + 1;
            haveToContinue = false;
            break;
          }
        }
      }
      if (haveToContinue) {
        // Check if free number and assign
        haveToContinue = true;
        for (k = 1; k < 10; k++) {
          if (!isAssignedCharacter(k, this)) {
            this.rightMembersShortcuts[i][0] = k;
            this.rightMembersShortcuts[i][1] = 'end';
            haveToContinue = false;
            break;
          }
        }
      }
      if (haveToContinue) {
        this.rightMembersShortcuts[i][0] = '';
        this.rightMembersShortcuts[i][1] = 0;
      }
      this.rightMembers[i].keyboardShortcutId = this.randomId + '_' + i;
      this.rightMembers[
        i
      ].keyboardShortcutCharacter = this.rightMembersShortcuts[i][0];
      this.rightMembers[
        i
      ].keyboardShortcutPosition = this.rightMembersShortcuts[i][1];
    }
  },

  enableShortcuts: function() {
    var length, i;
    if (this.leftMembers) {
      length = this.leftMembers.length;
      for (i = 0; i < length; i++) {
        if (this.leftMembers[i].enableShortcut) {
          this.leftMembers[i].enableShortcut();
        }
      }
    }
    if (this.rightMembers) {
      this.defineRightMembersShortcuts();
      length = this.rightMembers.length;
      for (i = 0; i < length; i++) {
        if (this.rightMembers[i].enableShortcut) {
          this.rightMembers[i].enableShortcut();
        }
      }
    }
  },

  disableShortcuts: function() {
    var length, i;
    if (this.leftMembers) {
      length = this.leftMembers.length;
      for (i = 0; i < length; i++) {
        if (this.leftMembers[i].disableShortcut) {
          this.leftMembers[i].disableShortcut();
        }
      }
    }
    if (this.rightMembers) {
      length = this.rightMembers.length;
      for (i = 0; i < length; i++) {
        if (this.rightMembers[i].disableShortcut) {
          this.rightMembers[i].disableShortcut();
        }
      }
    }
  },

  addMembers: 'null',

  leftMembers: [],
  rightMembers: [],

  styleName: 'OBToolbar',
  overflow: 'auto',
  membersMargin: 0
});

/** ----------------------------- **/

isc.ClassFactory.defineClass('OBToolbarIconButton', isc.MenuButton);

isc.OBToolbarIconButton.addProperties({
  showRollOver: true,
  showDisabled: true,
  showFocused: true,
  showDown: true,
  showFocusedAsOver: false,
  title: '',
  showHover: true,
  customState: '',
  showMenuButtonImage: false,

  initWidget: function() {
    this.Super('initWidget', arguments);
    this.resetBaseStyle();
  },

  resetBaseStyle: function() {
    var isMenu = false,
      extraClass;
    if (this.menu !== null) {
      isMenu = true;
    }
    if (isMenu) {
      extraClass = ' OBToolbarIconButtonMenu ';
      this.iconWidth = 3;
      this.iconHeight = 3;
    } else {
      extraClass = ' ';
      this.iconWidth = 1;
      this.iconHeight = 1;
    }

    this.setBaseStyle(
      'OBToolbarIconButton_icon_' +
        this.buttonType +
        this.customState +
        extraClass +
        'OBToolbarIconButton'
    );
  },

  keyboardShortcutId: null,
  enableShortcut: function() {
    if (this.keyboardShortcutId) {
      var me = this,
        ksAction;
      ksAction = function() {
        if (!me.disabled) {
          me.action();
        }
        return false; //To avoid keyboard shortcut propagation
      };
      OB.KeyboardManager.Shortcuts.set(
        this.keyboardShortcutId,
        'Canvas',
        ksAction
      );
    }
  },
  disableShortcut: function() {
    if (this.keyboardShortcutId) {
      OB.KeyboardManager.Shortcuts.set(
        this.keyboardShortcutId,
        null,
        function() {
          return true;
        }
      );
    }
  }
});

isc.ClassFactory.defineClass('OBToolbarTextButton', isc.Button);

isc.OBToolbarTextButton.addProperties({
  baseStyle: 'OBToolbarTextButton',
  showRollOver: true,
  showDisabled: true,
  showFocused: true,
  showDown: true,
  showFocusedAsOver: false,
  title: '',
  realTitle: '',
  // difference between title and realTitle is just the <u>xx</u> for keyboard shortcut notation.
  action: function() {
    alert(this.title);
  },
  initWidget: function() {
    this.Super('initWidget', arguments);
    this.realTitle = this.title;
  },

  keyboardShortcutId: null,
  keyboardShortcutCharacter: null,
  keyboardShortcutPosition: null,
  enableShortcut: function() {
    if (this.keyboardShortcutId) {
      var me = this,
        newTitle = this.realTitle,
        ksAction;

      ksAction = function() {
        if (!me.disabled && me.visible) {
          me.action();
        }
        return false; //To avoid keyboard shortcut propagation
      };

      if (this.keyboardShortcutPosition === 'end') {
        newTitle =
          newTitle + ' (<u>' + this.keyboardShortcutCharacter + '</u>)';
      } else {
        newTitle =
          newTitle.substring(0, this.keyboardShortcutPosition - 1) +
          '<u>' +
          newTitle.substring(
            this.keyboardShortcutPosition - 1,
            this.keyboardShortcutPosition
          ) +
          '</u>' +
          newTitle.substring(this.keyboardShortcutPosition, newTitle.length);
      }
      this.setTitle(newTitle);
      if (this.keyboardShortcutPosition) {
        // If 'this.keyboardShortcutPosition' equals 0 means that there is no shortcut assigned
        OB.KeyboardManager.Shortcuts.set(
          this.keyboardShortcutId,
          'Canvas',
          ksAction,
          null,
          {
            ctrl: true,
            alt: true,
            shift: true,
            key: this.keyboardShortcutCharacter
          }
        );
      }
    }
  },
  disableShortcut: function() {
    if (this.keyboardShortcutId) {
      var newTitle = this.realTitle;
      this.setTitle(newTitle);
      OB.KeyboardManager.Shortcuts.set(
        this.keyboardShortcutId,
        null,
        function() {
          return true;
        },
        '',
        {
          ctrl: true,
          alt: true,
          shift: true,
          key: 'xyz'
        }
      );
    }
  }
});

OB.ToolbarUtils = {};

OB.ToolbarUtils.print = function(view, url, directPrint, type) {
  var selectedRecords = view.viewGrid.getSelectedRecords(),
    length = selectedRecords.length;

  if (length === 0) {
    view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_WARNING,
      '',
      OB.I18N.getLabel('OBUIAPP_PrintNoRecordSelected')
    );
    return;
  }

  var popupParams = {},
    allProperties = view.getContextInfo(false, true, false, true),
    sessionProperties = view.getContextInfo(true, true, false, true),
    param,
    i,
    value,
    selectedIds = '';

  popupParams = {
    Command: 'DEFAULT',
    inppdfpath: url,
    inphiddenkey: view.standardProperties.inpKeyName,
    inpdirectprint: directPrint ? 'Y' : 'N',
    inpButtonType: type
  };

  for (param in allProperties) {
    if (Object.prototype.hasOwnProperty.call(allProperties, param)) {
      value = allProperties[param];

      if (typeof value === 'boolean') {
        value = value ? 'Y' : 'N';
      }

      popupParams[param] = value;
    }
  }

  selectedIds = '';
  for (i = 0; i < length; i++) {
    selectedIds += (i > 0 ? ',' : '') + selectedRecords[i].id;
  }

  popupParams.inphiddenvalue = selectedIds;

  view.setContextInfo(sessionProperties, function() {
    OB.Layout.ClassicOBCompatibility.Popup.open(
      'print',
      0,
      0,
      OB.Application.contextUrl + 'businessUtility/PrinterReports.html',
      '',
      window,
      false,
      false,
      true,
      popupParams
    );
  });
};

OB.ToolbarUtils.showAuditTrail = function(view) {
  var selectedRecords = view.viewGrid.getSelectedRecords();

  if (selectedRecords.length > 1) {
    var setWarning = {
      set: function(label) {
        view.messageBar.setMessage(isc.OBMessageBar.TYPE_WARNING, '', label);
      }
    };
    OB.I18N.getLabel('JS28', null, setWarning, 'set');
    return;
  }

  var popupParams = 'Command=POPUP_HISTORY';
  popupParams += '&inpTabId=' + view.tabId;
  popupParams += '&inpTableId=' + view.standardProperties.inpTableId;

  if (view.viewGrid.getSelectedRecord()) {
    popupParams += '&inpRecordId=' + view.viewGrid.getSelectedRecord().id;
  }

  OB.Layout.ClassicOBCompatibility.Popup.open(
    'audit',
    900,
    600,
    OB.Application.contextUrl +
      'businessUtility/AuditTrail.html?' +
      popupParams,
    '',
    window,
    false,
    false,
    true
  );
};

OB.ToolbarUtils.showTreeGrid = function(view) {
  var treeGrid = view.treeGrid,
    viewGrid = view.viewGrid;

  if (viewGrid && treeGrid) {
    // Copy the viewGrid fields to the treeGrid, so that both views always display the same fields
    treeGrid.setFields(treeGrid.getTreeGridFields(viewGrid.getFields()));
    treeGrid.copyCriteriaFromViewGrid();
    treeGrid.setData([]); // forcing refresh
    treeGrid.fetchData(treeGrid.getCriteria());
    treeGrid.checkShowFilterFunnelIcon(treeGrid.getCriteria());
    treeGrid.show();
    viewGrid.hide();
  }
  view.isShowingTree = true;
};

OB.ToolbarUtils.hideTreeGrid = function(view) {
  var treeGrid = view.treeGrid,
    viewGrid = view.viewGrid;

  if (viewGrid && treeGrid) {
    viewGrid.show();
    treeGrid.hide();
  }
  view.isShowingTree = false;
};

OB.ToolbarUtils.toggleTreeGridVisibility = function(view) {
  if (view.isShowingTree) {
    OB.ToolbarUtils.hideTreeGrid(view);
  } else {
    OB.ToolbarUtils.showTreeGrid(view);
  }
};

// ** {{{ OB.ToolbarUtils.createCloneButton(/*String*/ actionHandler, /*Object*/ requestParams, /*Array[String]*/ tabIds, /*String*/ askMsg, /*Integer*/ sortOrder, /*Boolean*/ editRecordAfterClone, /*String*/ buttonId, /*Boolean*/ overwriteIfExists, /*Array[String]*/ tabIdsToAvoid}}} **
// Automatically set up a clone button for the provided tabs
// Parameters:
// * {{{actionHandler}}}:  action handler which processes and returns the cloned record
// * {{{requestParams}}}: (Optional) aditional parameters to send to the action handler
// * {{{tabIds}}}: (Optional, all tabs will be included by default) array of tabIds where this button will be shown
// * {{{askMsg}}}: (Optional, 'OBUIAPP_WantToCloneRecord' by default) Text that will be displayed when the button be pressed.
// * {{{sortOrder}}}: (Optional, 'CLONE_BUTTON_PROPERTIES.sortPosition' by default) Position in the toolbar of the clone button.
// * {{{editRecordAfterClone}}}: (Optional, true by default) If the form edit view (of the cloned record) should be opened after clone it.
// * {{{buttonId}}}: (Optional, random by default) Don't set it unless you plan to do advanced coding with this button (as, for example, overwrite it later in another place).
// * {{{overwriteIfExists}}}: (Optional, false by default) To be able to overwrite a particular existing clone button. The buttonId should match with the overwritten one.
// * {{{tabIdsToAvoid}}}: (Optional, no tabs to avoid by default) array of tabIds where this button should not be shown
OB.ToolbarUtils.createCloneButton = function(
  actionHandler,
  requestParams,
  tabIds,
  askMsg,
  sortOrder,
  editRecordAfterClone,
  buttonId,
  overwriteIfExists,
  tabIdsToAvoid
) {
  var cloneButtonProps = isc.addProperties(
    {},
    isc.OBToolbar.CLONE_BUTTON_PROPERTIES
  );

  if (!askMsg) {
    askMsg = OB.I18N.getLabel('OBUIAPP_WantToCloneRecord');
  }
  if (!sortOrder) {
    sortOrder = isc.OBToolbar.CLONE_BUTTON_PROPERTIES.sortPosition;
  }
  if (editRecordAfterClone !== false) {
    editRecordAfterClone = true;
  }
  if (!buttonId) {
    buttonId =
      cloneButtonProps.buttonType + '_' + OB.Utilities.generateRandomString(8);
  }
  if (overwriteIfExists !== true) {
    overwriteIfExists = false;
  }

  cloneButtonProps.action = function()
{
    var view = this.view,
      callback,
      copyChildren = true,
      buttons = null;

    if (view.askToCloneChildren) {
      var copyButton = isc.addProperties({}, isc.Dialog.OK);
      copyButton.getTitle = () => OB.I18N.getLabel('OBUIAPP_CloneButton')
      copyButton.click = function () { copyChildren = false; this.topElement.okClick() };

      var copyChildrenButton = isc.addProperties({}, isc.Dialog.OK);
          copyChildrenButton.getTitle = () => OB.I18N.getLabel('OBUIAPP_CloneChildrenButton')
          copyChildrenButton.click = function () { copyChildren = true; this.topElement.okClick() };

      buttons = { buttons: [copyButton, copyChildrenButton, isc.Dialog.CANCEL] };
    }

    callback = function(ok) {
      if (!requestParams) {
        requestParams = {};
      }
      requestParams.recordId = view.viewGrid.getSelectedRecord().id;
      requestParams.tabId = view.tabId;
      requestParams.windowId = view.windowId;

      var params = {};
      params.recordIds = view.viewGrid.getSelectedRecords().map(r => r.id);
      params._entityName = view.entity;
      params._params =  { copyChildren: copyChildren };

      if (ok) {
        OB.RemoteCallManager.call(actionHandler, params, requestParams, function(
          rpcResponse,
          data,
          rpcRequest
        ) {
          if (data.records.length == 0) {
            // likely an error, attempt to show it
            if (data.responseActions) {
              data.responseActions._processView = view;
              OB.Utilities.Action.executeJSON(data.responseActions, null, null, view);
              return;
            }
          }
          var records = data.records;
          var recordIndex =
                          view.viewGrid.getRecordIndex(view.viewGrid.getSelectedRecord()) +
                          1,
                        recordsData = view.viewGrid
                          .getDataSource()
                          .recordsFromObjects(records)[0];
                      view.viewGrid.addToCacheData(recordsData, recordIndex);
                      view.viewGrid.scrollToRow(recordIndex);
                      view.viewGrid.markForRedraw();
          if (records.length == 1) {
            if (view.viewGrid.getEditRow()) {
              view.viewGrid.endEditing();
            }
            view.viewGrid.doSelectSingleRecord(recordIndex);
            if (editRecordAfterClone) {
              view.editRecord(view.viewGrid.getRecord(recordIndex), false);
            }
          } else {
            view.viewGrid.refreshGrid();
          }

        });
      }
    };
    isc.ask(askMsg, callback, buttons);
  };
  OB.ToolbarRegistry.registerButton(
    buttonId,
    isc.OBToolbarIconButton,
    cloneButtonProps,
    sortOrder,
    tabIds,
    tabIdsToAvoid,
    overwriteIfExists
  );
};

OB.ToolbarRegistry = {
  buttonDefinitions: [],

  // note tabIds is an array of strings, but maybe null/undefined
  registerButton: function(
    buttonId,
    clazz,
    properties,
    sortOrder,
    tabIds,
    tabIdsToAvoid,
    overwriteIfExists
  ) {
    var length;

    if (tabIds && !isc.isA.Array(tabIds)) {
      tabIds = [tabIds];
    }

    if (tabIdsToAvoid && !isc.isA.Array(tabIdsToAvoid)) {
      tabIdsToAvoid = [tabIdsToAvoid];
    }

    // declare the vars and the object which will be stored
    var i,
      index = 0,
      buttonDef;

    buttonDef = {
      buttonId: buttonId,
      clazz: clazz,
      properties: properties,
      sortOrder: sortOrder,
      tabIds: tabIds,
      tabIdsToAvoid: tabIdsToAvoid
    };

    // already registered button handling
    length = this.buttonDefinitions.length;
    for (i = 0; i < length; i++) {
      if (this.buttonDefinitions[i].buttonId === buttonId) {
        if (overwriteIfExists) {
          this.buttonDefinitions.splice(i, 1);
          break;
        } else {
          return;
        }
      }
    }

    index = this.buttonDefinitions.length;
    for (i = 0; i < length; i++) {
      if (this.buttonDefinitions[i].sortOrder > sortOrder) {
        index = i;
        break;
      }
    }

    if (index === this.buttonDefinitions.length) {
      this.buttonDefinitions[index] = buttonDef;
    } else {
      this.buttonDefinitions.splice(index, 0, buttonDef);
    }
  },

  getButtons: function(tabId) {
    // get the buttons for the tabId, this includes all buttons with that tabId or with no tabId set
    // as the button defs are already stored by their sortorder we can just iterate over the array
    // and pick them up in the correct order
    // the return should be an array of button instances created by doing
    //  btnDefinitionClass.create(btnDefinitionProperties);
    var result = [],
      j,
      resultIndex = 0,
      i,
      validTabId,
      tabIds,
      tabIdsToAvoid,
      length = this.buttonDefinitions.length,
      tabIdsLength,
      tabIdsToAvoidLength;
    for (i = 0; i < length; i++) {
      tabIds = this.buttonDefinitions[i].tabIds;
      tabIdsToAvoid = this.buttonDefinitions[i].tabIdsToAvoid;
      validTabId = !tabIds;
      if (tabIds) {
        tabIdsLength = tabIds.length;
        for (j = 0; j < tabIdsLength; j++) {
          if (tabIds[j] === tabId) {
            validTabId = true;
            break;
          }
        }
      }
      if (validTabId) {
        tabIdsToAvoidLength =
          tabIdsToAvoid && isc.isA.Array(tabIdsToAvoid) && tabIdsToAvoid.length
            ? tabIdsToAvoid.length
            : 0;
        for (j = 0; j < tabIdsToAvoidLength; j++) {
          if (tabIdsToAvoid[j] === tabId) {
            validTabId = false;
            break;
          }
        }
      }
      if (validTabId) {
        result[resultIndex++] = this.buttonDefinitions[i].clazz.create(
          isc.clone(this.buttonDefinitions[i].properties)
        );
      }
    }
    return result;
  }
};

//These are the icon toolbar buttons shown in all the tabs
OB.ToolbarRegistry.registerButton(
  isc.OBToolbar.NEW_DOC_BUTTON_PROPERTIES.buttonType,
  isc.OBToolbarIconButton,
  isc.OBToolbar.NEW_DOC_BUTTON_PROPERTIES,
  isc.OBToolbar.NEW_DOC_BUTTON_PROPERTIES.sortPosition,
  null,
  null,
  false
);
OB.ToolbarRegistry.registerButton(
  isc.OBToolbar.NEW_ROW_BUTTON_PROPERTIES.buttonType,
  isc.OBToolbarIconButton,
  isc.OBToolbar.NEW_ROW_BUTTON_PROPERTIES,
  isc.OBToolbar.NEW_ROW_BUTTON_PROPERTIES.sortPosition,
  null,
  null,
  false
);
OB.ToolbarRegistry.registerButton(
  isc.OBToolbar.SAVE_BUTTON_PROPERTIES.buttonType,
  isc.OBToolbarIconButton,
  isc.OBToolbar.SAVE_BUTTON_PROPERTIES,
  isc.OBToolbar.SAVE_BUTTON_PROPERTIES.sortPosition,
  null,
  null,
  false
);
OB.ToolbarRegistry.registerButton(
  isc.OBToolbar.SAVECLOSE_BUTTON_PROPERTIES.buttonType,
  isc.OBToolbarIconButton,
  isc.OBToolbar.SAVECLOSE_BUTTON_PROPERTIES,
  isc.OBToolbar.SAVECLOSE_BUTTON_PROPERTIES.sortPosition,
  null,
  null,
  false
);
OB.ToolbarRegistry.registerButton(
  isc.OBToolbar.UNDO_BUTTON_PROPERTIES.buttonType,
  isc.OBToolbarIconButton,
  isc.OBToolbar.UNDO_BUTTON_PROPERTIES,
  isc.OBToolbar.UNDO_BUTTON_PROPERTIES.sortPosition,
  null,
  null,
  false
);
OB.ToolbarRegistry.registerButton(
  isc.OBToolbar.DELETE_BUTTON_PROPERTIES.buttonType,
  isc.OBToolbarIconButton,
  isc.OBToolbar.DELETE_BUTTON_PROPERTIES,
  isc.OBToolbar.DELETE_BUTTON_PROPERTIES.sortPosition,
  null,
  null,
  false
);
OB.ToolbarRegistry.registerButton(
  isc.OBToolbar.REFRESH_BUTTON_PROPERTIES.buttonType,
  isc.OBToolbarIconButton,
  isc.OBToolbar.REFRESH_BUTTON_PROPERTIES,
  isc.OBToolbar.REFRESH_BUTTON_PROPERTIES.sortPosition,
  null,
  null,
  false
);
OB.ToolbarRegistry.registerButton(
  isc.OBToolbar.EXPORT_BUTTON_PROPERTIES.buttonType,
  isc.OBToolbarIconButton,
  isc.OBToolbar.EXPORT_BUTTON_PROPERTIES,
  isc.OBToolbar.EXPORT_BUTTON_PROPERTIES.sortPosition,
  null,
  null,
  false
);
OB.ToolbarRegistry.registerButton(
  isc.OBToolbar.ATTACHMENTS_BUTTON_PROPERTIES.buttonType,
  isc.OBToolbarIconButton,
  isc.OBToolbar.ATTACHMENTS_BUTTON_PROPERTIES,
  isc.OBToolbar.ATTACHMENTS_BUTTON_PROPERTIES.sortPosition,
  null,
  null,
  false
);

//and add the direct link at the end
OB.ToolbarRegistry.registerButton(
  isc.OBToolbar.LINK_BUTTON_PROPERTIES.buttonType,
  isc.OBToolbarIconButton,
  isc.OBToolbar.LINK_BUTTON_PROPERTIES,
  isc.OBToolbar.LINK_BUTTON_PROPERTIES.sortPosition,
  null,
  null,
  false
);
