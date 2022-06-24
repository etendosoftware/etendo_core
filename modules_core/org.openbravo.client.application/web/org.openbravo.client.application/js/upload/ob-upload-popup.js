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
 * All portions are Copyright (C) 2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

isc.defineClass('OBUploadView', isc.OBBaseParameterWindowView);

// == OBUploadView ==
// OBUploadView is the view that represents the window to upload
// the file with data. Note that below are also class properties added to define the
// upload button props.
isc.OBUploadView.addProperties({
  // Set default properties for the OBPopup container
  showMinimizeButton: false,
  showMaximizeButton: false,
  popupWidth: '390',
  popupHeight: '250',
  popup: true,
  showsItself: true,

  // Set now pure P&E layout properties
  width: '100%',
  height: '100%',
  overflow: 'auto',

  members: [],

  tabId: null,
  ownerId: null,
  viewProperties: {},

  // to be set by subtype, note ID is written in capitals because it is the html element ID.
  button: null,
  buttonID: null,
  viewID: null,

  // needs to be set by sub type
  action: null,

  defaultsActionHandler:
    'org.openbravo.client.application.businesslogic.DefaultsUploadDataActionHandler',

  formProps: {
    encoding: 'multipart',
    action: '',
    // is set in initwidget
    target: 'background_target',
    numCols: 1,
    align: 'center'
    //redraw: function () {}
    //theCanvas: this.canvas
  },

  initWidget: function() {
    var formFields = [],
      i,
      form,
      hiddenFields,
      fileItemFormFields,
      fileItemForm;
    formFields = [
      {
        name: 'inpname',
        title: OB.I18N.getLabel('OBUIAPP_File'),
        // Upload type item cannot be used as it is not possible to redraw() the DynamicForm which
        // is needed to run the display and read only logics
        titleStyle: 'OBFormFieldLabel',
        cellStyle: 'OBFormField',
        type: 'file',
        multiple: false,
        canFocus: false
      },
      {
        selectOnFocus: true,
        width: '100%',
        canFocus: true,
        name: 'importMode',
        title: OB.I18N.getLabel('OBUIAPP_ImportMode'),
        defaultValue: 'add_import',
        prompt: OB.I18N.getLabel('OBUIAPP_ExplainImportMode'),
        // is the list reference
        type: '_id_17',
        valueMap: {
          add_import: OB.I18N.getLabel('OBUIAPP_ImportAndAdd'),
          replace_import: OB.I18N.getLabel('OBUIAPP_ImportAndReplace')
        }
      },
      {
        // is the list reference
        type: '_id_17',
        selectOnFocus: true,
        width: '100%',
        canFocus: true,
        name: 'importErrorHandling',
        title: OB.I18N.getLabel('OBUIAPP_ImportError'),
        defaultValue: 'continue_at_error',
        prompt: OB.I18N.getLabel('OBUIAPP_ExplainImportError'),
        valueMap: {
          continue_at_error: OB.I18N.getLabel('OBUIAPP_ContinueAtError'),
          stop_at_error: OB.I18N.getLabel('OBUIAPP_StopAtError')
        }
      }
    ];
    hiddenFields = [
      {
        name: 'command',
        type: 'hidden',
        value: 'upload'
      },
      {
        name: 'viewID',
        type: 'hidden',
        value: this.viewID
      },
      {
        name: 'buttonID',
        type: 'hidden',
        value: this.buttonID
      },
      {
        name: 'inpOwnerId',
        type: 'hidden',
        value: this.ownerId
      },
      {
        name: 'inpTabId',
        type: 'hidden',
        value: this.tabId
      }
    ];
    formFields.addAll(hiddenFields);

    this.formProps.action = this.action;

    this.baseParams.tabId = this.tabId;
    this.baseParams.ownerId = this.ownerId;

    this.formProps = isc.addProperties({}, this.formProps, this.otherFormProps);
    this.viewProperties.fields = isc.shallowClone(formFields);
    if (this.viewProperties.additionalFields) {
      for (i = 0; i < this.viewProperties.additionalFields.length; i++) {
        this.viewProperties.fields.push(
          this.viewProperties.additionalFields[i]
        );
      }
    }

    this.Super('initWidget', arguments);

    // To submit the file is needed a DynamicForm that contains a UploadFile item. In this case it
    // is used the FileItemForm that it is automatically generated for the FileItem. To submit all
    // the values it is needed to create in this form all the needed hidden inputs.
    form = this.theForm;
    fileItemForm = form.getFileItemForm();
    fileItemFormFields = isc.shallowClone(fileItemForm.getItems());
    // paramValues has a String representation of a JSONObject with the values of all the metadata values.
    // Command and hiddenFields are needed in the Request of TabAttachment servlet.
    fileItemFormFields.addAll([
      {
        name: 'paramValues',
        type: 'hidden',
        value: ''
      }
    ]);
    fileItemFormFields.addAll(hiddenFields);

    fileItemForm.setItems(fileItemFormFields);
    // redraw to ensure that the new items are added to the html form. If this not happens then the
    // values are not included in the submitForm.
    fileItemForm.redraw();
    fileItemForm.setAction(this.action);
    fileItemForm.setTarget('background_target');
  },

  destroy: function() {
    this.theForm.getFileItemForm().destroy();
    this.Super('destroy', arguments);
  },

  buildButtonLayout: function() {
    var view = this,
      buttons = [],
      me = this,
      submitbutton,
      cancelButton;

    function doClick() {
      var view = this.view,
        value = view.theForm.getItem('inpname').getValue(),
        lastChar,
        fileName;

      if (!value) {
        isc.say(OB.I18N.getLabel('OBUIAPP_SpecifyFile'));
        return;
      }
      if (
        !value.toLowerCase().endsWith('.txt') &&
        !value.toLowerCase().endsWith('.csv')
      ) {
        isc.say(OB.I18N.getLabel('OBUIAPP_OnlyTxtFileSupported'));
        return;
      }

      var msg = me.validate(view.theForm);
      if (msg) {
        isc.say(msg);
        return;
      }

      lastChar = value.lastIndexOf('\\') + 1;
      fileName = lastChar === -1 ? value : value.substring(lastChar);
      view.submitFile(fileName);
    }

    submitbutton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OBUIAPP_Submit'),
      click: doClick,
      realTitle: '',
      view: view
    });
    view.firstFocusedItem = submitbutton;
    cancelButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OBUISC_Dialog.CANCEL_BUTTON_TITLE'),
      realTitle: '',
      click: function() {
        view.closeClick();
      }
    });

    buttons.push(
      isc.LayoutSpacer.create({
        realTitle: ''
      })
    );
    buttons.push(submitbutton);
    buttons.push(
      isc.LayoutSpacer.create({
        realTitle: '',
        width: '30'
      })
    ); // 30px width
    buttons.push(cancelButton);
    buttons.push(
      isc.LayoutSpacer.create({
        realTitle: ''
      })
    );

    return buttons;
  },

  submitFile: function(fileName) {
    var form = this.theForm;

    // spin it
    this.button.customState = 'Progress';
    this.button.resetBaseStyle();

    form.updateFileItemForm();
    form
      .getFileItemForm()
      .getItem('paramValues')
      .setValue(isc.JSON.encode(this.getContextInfo()));
    form.getFileItemForm().submitForm();

    this.closeClick();
  },

  validate: function() {
    return null;
  }
});

isc.OBUploadView.addClassProperties({
  UPLOAD_BUTTON_PROPERTIES: {
    // the following properties should be set by the implementing type
    actionUrl: '',
    inChildTab: false,
    title: '',
    buttonType: 'ob-upload-import',
    prompt: '',
    getPopupType: function() {
      return isc.OBUploadView;
    },
    action: function() {
      var view = this.view,
        ownerId,
        tabId = view.tabId,
        grid = view.viewGrid,
        selectedRecords = grid.getSelectedRecords();

      if (this.inChildTab) {
        ownerId = this.view.parentRecordId;
      } else {
        ownerId = selectedRecords[0].id;
      }

      var popupContent = this.getPopupType().create({
        button: this,
        action: this.actionUrl,
        ownerId: ownerId,
        tabId: tabId,
        viewID: view.ID,
        title: this.popupTitle,
        buttonID: this.ID
      });
      view.standardWindow.openPopupInTab(
        popupContent,
        this.title,
        popupContent.popupWidth,
        popupContent.popupHeight,
        false,
        false,
        true,
        true
      );
    },

    updateState: function() {
      var ownerId;
      if (this.inChildTab) {
        ownerId = this.view.parentRecordId;
        if (ownerId && ownerId !== '-1') {
          this.setDisabled(false);
        } else {
          this.setDisabled(true);
        }
      } else {
        this.updateTabState();
      }
    },

    updateTabState: function() {
      var view = this.view,
        form = view.viewForm,
        grid = view.viewGrid,
        selectedRecords = grid.getSelectedRecords();
      if (view.isShowingForm && form.isNew) {
        this.setDisabled(true);
      } else if (view.isEditingGrid && grid.getEditForm().isNew) {
        this.setDisabled(true);
      } else {
        this.setDisabled(selectedRecords.length !== 1);
      }
    },

    callback: function(data) {
      this.view.refresh();
      if (data && data.msg) {
        isc.say(data.msg);
      }
      if (data.fileName) {
        this.downloadResult(data.fileName);
      }

      // back to non-spinned state
      this.customState = '';
      this.resetBaseStyle();
    },

    downloadResult: function(fileName) {
      var params = {
        paramValues: isc.JSON.encode({
          command: 'download',
          fileName: fileName
        })
      };
      OB.Utilities.postThroughHiddenForm(this.actionUrl, params);
    }
  }
});
