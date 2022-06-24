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
 * All portions are Copyright (C) 2011-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
// = OBAttachments =
//
// Represents the attachments section in the form.
//
isc.ClassFactory.defineClass('OBAttachmentsSectionItem', isc.OBSectionItem);

isc.OBAttachmentsSectionItem.addProperties({
  // as the name is always the same there should be at most
  // one linked item section per form
  name: '_attachments_',

  // note: setting these apparently completely hides the section
  // width: '100%',
  // height: '100%',
  // this field group does not participate in personalization
  personalizable: false,

  canFocus: true,

  // don't expand as a default
  sectionExpanded: false,

  prompt: OB.I18N.getLabel('OBUIAPP_AttachmentPrompt'),

  attachmentCanvasItem: null,

  visible: false,

  attachmentCount: 0,

  itemIds: ['_attachments_Canvas'],

  // note formitems don't have an initWidget but an init method
  init: function() {
    // override the one passed in
    this.defaultValue = OB.I18N.getLabel('OBUIAPP_AttachmentTitle');
    this.sectionExpanded = false;

    // tell the form who we are
    this.form.attachmentsSection = this;

    return this.Super('init', arguments);
  },

  setAttachmentCount: function(lAttachmentCount) {
    var formView = this.form.view;

    lAttachmentCount = parseInt(lAttachmentCount, 10);
    this.attachmentCount = lAttachmentCount;

    if (lAttachmentCount === 0) {
      this.setValue(OB.I18N.getLabel('OBUIAPP_AttachmentTitle'));
      formView.attachmentExists = false;
    } else {
      formView.attachmentExists = true;
      this.setValue(
        OB.I18N.getLabel('OBUIAPP_AttachmentTitle') +
          ' (' +
          lAttachmentCount +
          ')'
      );
    }
    this.getAttachmentPart().hasAttachments = formView.attachmentExists;
    formView.toolBar.updateButtonState();
  },

  getAttachmentPart: function() {
    if (!this.attachmentCanvasItem) {
      this.attachmentCanvasItem = this.form.getField(this.itemIds[0]);
    }
    return this.attachmentCanvasItem.canvas;
  },

  setRecordInfo: function(entity, id, tabId, attachmentForm) {
    this.getAttachmentPart().setRecordInfo(entity, id, tabId, attachmentForm);
  },

  collapseSection: function() {
    var ret = this.Super('collapseSection', arguments);
    this.getAttachmentPart().setExpanded(false);
    return ret;
  },

  expandSection: function() {
    // if this is not there then when clicking inside the
    // section item will visualize it
    if (!this.isVisible()) {
      return;
    }
    var ret = this.Super('expandSection', arguments);
    this.getAttachmentPart().setExpanded(true);
    return ret;
  },

  fillAttachments: function(attachments) {
    this.getAttachmentPart().fillAttachments(attachments);
  }
});

isc.ClassFactory.defineClass('OBAttachmentCanvasItem', isc.CanvasItem);

isc.OBAttachmentCanvasItem.addProperties({
  // some defaults, note if this changes then also the
  // field generation logic needs to be checked
  colSpan: 4,
  startRow: true,
  endRow: true,

  canFocus: true,

  // setting width/height makes the canvasitem to be hidden after a few
  // clicks on the section item, so don't do that for now
  // width: '100%',
  // height: '100%',
  showTitle: false,

  // note that explicitly setting the canvas gives an error as not
  // all props are set correctly on the canvas (for example the
  // pointer back to this item: canvasItem
  // for setting more properties use canvasProperties, etc. see
  // the docs
  canvasConstructor: 'OBAttachmentsLayout',

  // never disable this one
  isDisabled: function() {
    return false;
  }
});

isc.ClassFactory.defineClass('OBAttachmentsLayout', isc.VLayout);

isc.OBAttachmentsLayout.addProperties({
  // set to true when the content has been created at first expand
  isInitialized: false,

  layoutMargin: 5,

  width: '100%',
  align: 'left',
  // Data initialized when the record info is set
  attachmentForm: null,
  tabId: null,
  entity: null,
  recordId: null,
  docOrganization: null,
  docClient: null,
  hasAttachments: false,

  // never disable this item
  isDisabled: function() {
    return false;
  },

  getForm: function() {
    return this.canvasItem.form;
  },

  setRecordInfo: function(entity, id, tabId, attachmentForm) {
    this.entity = entity;
    // use recordId instead of id, as id is often used to keep
    // html ids
    this.recordId = id;
    this.tabId = tabId;
    this.attachmentForm = attachmentForm;
    //Here we are checking if the entity is 'Organization' because the way of obtaining the
    //id of the organization of the form is different depending on the entity
    if (this.entity === 'Organization') {
      this.docOrganization = this.recordId;
    } else {
      this.docOrganization = this.attachmentForm.values.organization;
    }
    if (this.entity === 'Client') {
      this.docClient = this.recordId;
    } else {
      this.docClient = this.attachmentForm.values.client;
    }

    this.isInitialized = false;
  },

  setExpanded: function(expanded) {
    var attachLayout = this;
    if (expanded) {
      var d = {
        Command: 'LOAD',
        tabId: this.tabId,
        buttonId: this.ID,
        recordIds: this.recordId,
        viewId: this.attachmentForm.view.ID
      };

      OB.RemoteCallManager.call(
        'org.openbravo.client.application.attachment.AttachmentAH',
        {},
        d,
        function(response, data, request) {
          attachLayout.fillAttachments(data.attachments);
          if (data.status === -1) {
            OB.Utilities.writeErrorMessage(data.viewId, data.errorMessage);
          }
          if (!attachLayout.isInitialized) {
            this.isInitialized = true;
          }
        }
      );
    } else {
      // When collapsing the section remove loaded attachments.
      this.savedAttachments = [];
      this.destroyAndRemoveMembers(this.getMembers());
    }
  },

  addAttachmentInfo: function(attachmentLayout, attachment) {},

  callback: function(attachmentsobj) {
    var button = this.getForm().view.toolBar.getLeftMember(
      isc.OBToolbar.TYPE_ATTACHMENTS
    );
    if (!button) {
      button = this.getForm().view.toolBar.getLeftMember('attachExists');
    }
    button.customState = '';
    button.resetBaseStyle();
    this.fillAttachments(attachmentsobj.attachments);
  },
  resetToolbar: function() {
    var canvas = null;
    var currentElement = null;
    var positionOfLastMember = 0;
    var button = this.getForm().view.toolBar.getLeftMember(
      isc.OBToolbar.TYPE_ATTACHMENTS
    );
    if (!button) {
      button = this.getForm().view.toolBar.getLeftMember('attachExists');
    }
    button.customState = '';
    button.resetBaseStyle();
    //Deleting the upload message of the cancelled upload
    if (OB.Utilities.currentUploader) {
      canvas = window[OB.Utilities.currentUploader];
      if (canvas) {
        //The last member is the cancelled upload.
        positionOfLastMember = canvas.getMembers().size() - 1;
        //The first member is the Hlayout where the buttons are.
        if (positionOfLastMember > 0) {
          currentElement = canvas.getMembers()[positionOfLastMember];
          if (currentElement) {
            canvas.removeMember(currentElement);
          }
        }
      }
    }
  },
  fileExists: function(fileName, attachments) {
    var i, length;

    if (!attachments || attachments.length === 0) {
      return false;
    }

    length = attachments.length;
    for (i = 0; i < length; i++) {
      if (attachments[i].name === fileName) {
        return true;
      }
    }
    return false;
  },

  fillAttachments: function(attachments) {
    var attachLayout = this,
      i,
      editDescActions;

    attachments = attachments || [];
    this.getForm()
      .getItem('_attachments_')
      .setAttachmentCount(attachments.length);
    this.savedAttachments = attachments;
    this.destroyAndRemoveMembers(this.getMembers());
    var hLayout = isc.HLayout.create();

    if (this.getForm().isNew) {
      return;
    }

    this.addMember(hLayout);
    var addButton = isc.OBLinkButtonItem.create({
      title: '[ ' + OB.I18N.getLabel('OBUIAPP_AttachmentAdd') + ' ]',
      width: '30px',
      action: function(forceUpload) {
        if (OB.Utilities.currentUploader === null || forceUpload) {
          attachLayout.openAttachPopup(true);
        } else {
          isc.ask(
            OB.I18N.getLabel('OBUIAPP_OtherUploadInProgress'),
            function(clickOK) {
              if (clickOK) {
                var forceUpload = true;
                this.button.action(forceUpload);
              }
            },
            {
              button: this
            }
          );
        }
      }
    });
    if (!this.getForm().view.viewForm.readOnly) {
      hLayout.addMember(addButton);
    }
    // If there are no attachments, we only display the "[Add]" button
    if (this.hasAttachments === false) {
      return;
    }
    var downloadAllButton = isc.OBLinkButtonItem.create({
      title: '[ ' + OB.I18N.getLabel('OBUIAPP_AttachmentDownloadAll') + ' ]',
      width: '30px',
      action: function() {
        isc.confirm(
          OB.I18N.getLabel('OBUIAPP_FormConfirmDownloadMultiple'),
          function(clickedOK) {
            if (clickedOK) {
              var d = {
                Command: 'DOWNLOAD_ALL',
                tabId: attachLayout.tabId,
                recordIds: attachLayout.recordId,
                viewId: attachLayout.attachmentForm.view.ID
              };
              OB.Utilities.postThroughHiddenForm(
                './businessUtility/TabAttachments_FS.html',
                d
              );
            }
          }
        );
      }
    });
    var removeAllButton = isc.OBLinkButtonItem.create({
      title: '[ ' + OB.I18N.getLabel('OBUIAPP_AttachmentRemoveAll') + ' ]',
      width: '30px',
      action: function() {
        var d = {
          Command: 'DELETE',
          tabId: attachLayout.tabId,
          buttonId: attachLayout.ID,
          recordIds: attachLayout.recordId,
          viewId: attachLayout.attachmentForm.view.ID
        };
        isc.confirm(
          OB.I18N.getLabel('OBUIAPP_ConfirmRemoveAll'),
          function(clickedOK) {
            if (clickedOK) {
              OB.RemoteCallManager.call(
                'org.openbravo.client.application.attachment.AttachmentAH',
                {},
                d,
                function(response, data, request) {
                  attachLayout.fillAttachments(data.attachments);
                  if (data.status === -1) {
                    OB.Utilities.writeErrorMessage(
                      data.viewId,
                      data.errorMessage
                    );
                  }
                }
              );
            }
          },
          {
            title: OB.I18N.getLabel('OBUIAPP_DialogTitle_RemoveAttachments')
          }
        );
      }
    });
    hLayout.addMember(downloadAllButton);
    if (!this.getForm().view.viewForm.readOnly) {
      hLayout.addMember(removeAllButton);
    }

    var downloadActions;
    downloadActions = function() {
      var d = {
        Command: 'DOWNLOAD_FILE',
        attachmentId: this.attachmentId,
        viewId: attachLayout.attachmentForm.view.ID
      };
      OB.Utilities.postThroughHiddenForm(
        './businessUtility/TabAttachments_FS.html',
        d
      );
    };

    var removeActions;
    removeActions = function() {
      var d = {
        Command: 'DELETE',
        tabId: attachLayout.tabId,
        buttonId: attachLayout.ID,
        recordIds: attachLayout.recordId,
        attachId: this.attachmentId,
        viewId: attachLayout.attachmentForm.view.ID
      };

      isc.confirm(
        OB.I18N.getLabel('OBUIAPP_ConfirmRemove'),
        function(clickedOK) {
          if (clickedOK) {
            OB.RemoteCallManager.call(
              'org.openbravo.client.application.attachment.AttachmentAH',
              {},
              d,
              function(response, data, request) {
                attachLayout.fillAttachments(data.attachments);
                if (data.status === -1) {
                  OB.Utilities.writeErrorMessage(
                    data.viewId,
                    data.errorMessage
                  );
                }
              }
            );
          }
        },
        {
          title: OB.I18N.getLabel('OBUIAPP_DialogTitle_RemoveAttachment')
        }
      );
    };

    editDescActions = function() {
      attachLayout.openAttachPopup(false, this.attachment);
    };

    for (i = 0; i < attachments.length; i++) {
      var attachment = attachments[i];
      var buttonLayout = isc.HLayout.create();
      var attachmentLabel = isc.Label.create({
        contents: attachment.name.asHTML(),
        className: 'OBNoteListGrid',
        width: '200px',
        height: 20,
        wrap: false
      });
      var creationDate = OB.Utilities.getTimePassedInterval(attachment.age);
      var attachmentBy = isc.Label.create({
        height: 1,
        className: 'OBNoteListGridAuthor',
        width: '200px',
        contents:
          creationDate +
          ' ' +
          OB.I18N.getLabel('OBUIAPP_AttachmentBy') +
          ' ' +
          attachment.updatedby
      });
      var downloadAttachment = isc.OBLinkButtonItem.create({
        title: '[ ' + OB.I18N.getLabel('OBUIAPP_AttachmentDownload') + ' ]',
        width: '30px',
        attachmentName: attachment.name,
        attachmentId: attachment.id,
        attachmentMethod: attachment.attmethod,
        action: downloadActions
      });
      downloadAttachment.height = 0;
      var removeAttachment = isc.OBLinkButtonItem.create({
        title: '[ ' + OB.I18N.getLabel('OBUIAPP_AttachmentRemove') + ' ]',
        width: '30px',
        attachmentName: attachment.name,
        attachmentId: attachment.id,
        action: removeActions
      });

      var editDescription = isc.OBLinkButtonItem.create({
        title: '[ ' + OB.I18N.getLabel('OBUIAPP_AttachmentEditDesc') + ' ]',
        width: '30px',
        attachment: attachment,
        action: editDescActions
      });
      var descText = attachment.description || '';
      var description = isc.Label.create({
        contents: descText.asHTML(),
        className: 'OBNoteListGrid',
        width: '100%',
        height: 20,
        wrap: true
      });

      buttonLayout.addMember(attachmentLabel);
      buttonLayout.addMember(attachmentBy);
      buttonLayout.addMember(downloadAttachment);
      if (!this.getForm().view.viewForm.readOnly) {
        buttonLayout.addMember(removeAttachment);
      }
      buttonLayout.addMember(editDescription);
      buttonLayout.addMember(description);
      this.addMember(buttonLayout);
    }
  },

  openAttachPopup: function(uploadMode, attachment) {
    var viewId = 'attachment_' + this.tabId,
      ownerView = this.getForm().view,
      standardWindow = ownerView.standardWindow,
      clientContext = null,
      windowTitle = OB.I18N.getLabel(
        uploadMode ? 'OBUIAPP_AttachFile' : 'OBUIAPP_AttachmentEditDesc'
      ),
      params = {},
      editParams = {},
      attachSection = this,
      callback;

    if (uploadMode === false) {
      viewId = viewId + '_' + attachment.attmethod;
      editParams.attachmentId = attachment.id;
      editParams.attachmentName = attachment.name;
      editParams.attachmentMethod = attachment.attmethod;
    }
    callback = function(response, data, request) {
      if (data.Class !== undefined) {
        standardWindow.selectedState =
          ownerView.viewGrid && ownerView.viewGrid.getSelectedState();

        standardWindow.runningProcess = data.create(
          isc.addProperties(
            {},
            {
              parentWindow: standardWindow,
              buttonOwnerView: ownerView,
              attachSection: attachSection,
              uploadMode: uploadMode
            },
            editParams
          )
        );

        standardWindow.openPopupInTab(
          standardWindow.runningProcess,
          windowTitle,
          standardWindow.runningProcess.popupWidth,
          standardWindow.runningProcess.popupHeight,
          standardWindow.runningProcess.showMinimizeButton,
          standardWindow.runningProcess.showMaximizeButton,
          true,
          true
        );
      } else {
        isc.warn(
          OB.I18N.getLabel('OBUIAPP_ProcessClassNotFound', [viewId]),
          function() {
            return true;
          },
          {
            icon: '[SKINIMG]Dialog/error.png',
            title: OB.I18N.getLabel('OBUIAPP_Error')
          }
        );
      }
    };

    OB.Layout.ViewManager.fetchView(
      viewId,
      callback,
      clientContext,
      null,
      false,
      params
    );
  },

  // ensure that the view gets activated
  focusChanged: function() {
    var view = this.getForm().view;
    if (view && view.setAsActiveView) {
      view.setAsActiveView();
    }
    return this.Super('focusChanged', arguments);
  }
});
