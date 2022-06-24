/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use. this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2011-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

//== OBImageItemSmallImage ==
//This class is used for the small image shown within the OBImageItemSmallImageContainer
isc.ClassFactory.defineClass('OBImageItemSmallImage', isc.Img);

//== OBImageItemSmallImageContainer ==
//This class is used for the small image container box
isc.ClassFactory.defineClass('OBImageItemSmallImageContainer', isc.HLayout);

isc.OBImageItemSmallImageContainer.addProperties({
  imageItem: null,
  initWidget: function() {
    if (this.initWidgetStyle) {
      this.initWidgetStyle();
    }
    this.Super('initWidget', arguments);
  },
  click: function() {
    var imageId = this.imageItem.getValue();
    if (!imageId) {
      return;
    }
    var d = {
      inpimageId: imageId,
      command: 'GETSIZE'
    };
    OB.RemoteCallManager.call(
      'org.openbravo.client.application.window.ImagesActionHandler',
      {},
      d,
      function(response, data, request) {
        var pageHeight = isc.Page.getHeight() - 100;
        var pageWidth = isc.Page.getWidth() - 100;
        var height = data.height;
        var width = data.width;
        if (!width && !height) {
          width = pageWidth;
          height = pageHeight;
        }
        var ratio = width / height;
        if (ratio > pageWidth / pageHeight) {
          width = width > pageWidth ? pageWidth : width;
          height = width / ratio;
        } else {
          height = height > pageHeight ? pageHeight : height;
          width = height * ratio;
        }
        var imagePopup = isc.OBPopup.create({
          height: height,
          width: width,
          showMinimizeButton: false,
          showMaximizeButton: false
        });
        var image = isc.OBImageItemBigImage.create({
          popupContainer: imagePopup,
          height: height,
          width: width,
          click: function() {
            this.popupContainer.closeClick();
          },
          src:
            '../utility/ShowImage?id=' + imageId + '&nocache=' + Math.random()
        });
        image.setImageType('stretch');
        imagePopup.addItem(image);
        imagePopup.show();
      }
    );
  }
});

//== OBImageItemBigImage ==
//This class is used for the big image shown within the popup
isc.ClassFactory.defineClass('OBImageItemBigImage', isc.Img);

isc.OBImageItemBigImage.addProperties({
  initWidget: function() {
    this.setCursor('url("' + this.zoomOutCursorSrc + '"), pointer');
    return this.Super('initWidget', arguments);
  }
});

//== OBImageItemButton ==
//This class is used for the buttons shown in the OBImageItem
isc.ClassFactory.defineClass('OBImageItemButton', isc.ImgButton);

isc.OBImageItemButton.addProperties({
  initWidget: function() {
    this.initWidgetStyle();
    return this.Super('initWidget', arguments);
  }
});

//== OBImageCanvas ==
//This canvas contains the image shown in the OBImageItem, and the two buttons
//which are used to upload and delete images.
isc.ClassFactory.defineClass('OBImageCanvas', isc.HLayout);

isc.OBImageCanvas.addProperties({
  initWidget: function() {
    this.Super('initWidget', arguments);
    this.imageLayout = isc.OBImageItemSmallImageContainer.create({
      imageItem: this.creator
    });
    if (this.creator.required) {
      this.imageLayout.setStyleName(this.imageLayout.styleName + 'Required');
    }
    if (this.creator.disabled) {
      this.imageLayout.setStyleName(this.imageLayout.styleName + 'Disabled');
    }
    if (this.creator.readOnly) {
      this.imageLayout.setStyleName(this.imageLayout.styleName + 'Disabled');
    }
    this.addMember(this.imageLayout);
    this.image = isc.OBImageItemSmallImage.create({
      width: '100%'
    });
    this.imageLayout.addMember(this.image);
    this.image.setSrc(this.imageNotAvailableSrc);
    this.image.setHeight(this.imageNotAvailableHeight);
    this.image.setWidth(this.imageNotAvailableWidth);
    var buttonLayout = isc.VLayout.create({
      width: '1%'
    });
    var selectorButton = isc.OBImageItemButton.create({
      buttonType: 'upload',
      imageItem: this.creator,
      action: function() {
        var selector = isc.OBImageSelector.create({
          columnName: this.imageItem.columnName,
          form: this.imageItem.form,
          imageItem: this.imageItem
        });
        var title = OB.I18N.getLabel('OBUIAPP_ImageSelectorTitle'),
          height = selector.height,
          width = selector.width,
          showMinimizeButton = false,
          showMaximizeButton = false;
        if (
          this.imageItem &&
          this.imageItem.form &&
          this.imageItem.form.view &&
          this.imageItem.form.view.standardWindow &&
          this.imageItem.form.view.standardWindow.openPopupInTab
        ) {
          this.imageItem.form.view.standardWindow.openPopupInTab(
            selector,
            title,
            width,
            height,
            showMaximizeButton,
            showMaximizeButton,
            true,
            true,
            this.imageItem.form
          );
        } else {
          var selectorContainer = isc.OBPopup.create({
            showMinimizeButton: showMinimizeButton,
            showMaximizeButton: showMaximizeButton,
            title: title,
            width: width,
            height: height,
            items: [selector]
          });
          selectorContainer.show();
        }
      },
      updateState: function(value) {
        if (value) {
          this.setDisabled(false);
        } else {
          this.setDisabled(true);
        }
      }
    });
    var deleteButton = isc.OBImageItemButton.create({
      buttonType: 'erase',
      imageItem: this.creator,
      deleteFunction: function() {
        var imageItem = this.imageItem,
          imageId = this.imageItem._value,
          isNewRecord = this.imageItem.form.isNewRecord();
        imageItem.refreshImage();

        // If the record is new and the image is deleted, remove it from the database
        if (isNewRecord) {
          OB.RemoteCallManager.call(
            'org.openbravo.client.application.DeleteImageActionHandler',
            {
              img: imageId,
              csrfToken: OB.User.csrfToken
            }
          );
        }
      },
      click: function(form, item) {
        this.deleteFunction();
      },
      updateState: function(value) {
        if (value) {
          this.setDisabled(false);
        } else {
          this.setDisabled(true);
        }
      }
    });

    if (this.parentItem.isPreviewFormItem) {
      selectorButton.showDisabled = false;
      selectorButton.showDisabledIcon = false;
    }

    this.deleteButton = deleteButton;
    this.selectorButton = selectorButton;
    buttonLayout.addMember(selectorButton);
    buttonLayout.addMember(deleteButton);
    this.addMember(buttonLayout);
  },
  setImage: function(url) {
    if (!url) {
      this.image.setSrc(this.imageNotAvailableSrc);
      this.image.setHeight(this.imageNotAvailableHeight);
      this.image.setWidth(this.imageNotAvailableWidth);
      this.image.setCursor('default');
      this.imageLayout.setCursor('default');
    } else {
      this.image.setSrc(url);
      this.image.setCursor('url("' + this.zoomInCursorSrc + '"), pointer');
      this.imageLayout.setCursor(
        'url("' + this.zoomInCursorSrc + '"), pointer'
      );
    }
  }
});

// == OBImageItem ==
// Item used for Openbravo ImageBLOB images.
isc.ClassFactory.defineClass('OBImageItem', isc.CanvasItem);

isc.OBImageItem.addProperties({
  shouldSaveValue: true,
  canvasConstructor: 'OBImageCanvas',
  init: function() {
    this.canvasProperties = this.canvasProperties || {};
    this.canvasProperties.parentItem = this;
    this.canvasProperties.canFocus = true;
    this.Super('init', arguments);
  },
  //This formitem will never be disabled, so even if the form is readonly, click events will still be triggered
  isDisabled: function() {
    return false;
  },
  setValue: function(newValue) {
    if (!newValue || newValue === '') {
      this.canvas.setImage('');
    } else {
      if (this.isLoadedCurrentImage(newValue)) {
        this.canvas.setImage(this.canvas.image.src);
      } else {
        this.canvas.setImage(
          '../utility/ShowImage?id=' + newValue + '&nocache=' + Math.random()
        );
        var d = {
          inpimageId: newValue,
          command: 'GETSIZE'
        };
        var image = this.canvas.image;
        var imageLayout = this.canvas.imageLayout;
        OB.RemoteCallManager.call(
          'org.openbravo.client.application.window.ImagesActionHandler',
          {},
          d,
          function(response, data, request) {
            var maxHeight = imageLayout.getHeight() - 12;
            var maxWidth = imageLayout.getWidth() - 12;
            var maxRatio = maxWidth / maxHeight;

            var imgHeight = data.height;
            var imgWidth = data.width;
            if (!imgWidth && !imgHeight) {
              imgWidth = maxWidth;
              imgHeight = maxHeight;
            }
            var imgRatio = imgWidth / imgHeight;

            if (imgHeight < maxHeight && imgWidth < maxWidth) {
              image.setHeight(imgHeight);
              image.setWidth(imgWidth);
            } else if (imgRatio < maxRatio) {
              image.setHeight(maxHeight);
              image.setWidth(maxHeight * imgRatio);
            } else {
              image.setHeight(maxWidth / imgRatio);
              image.setWidth(maxWidth);
            }
          }
        );
      }
    }
    //Buttons will not be shown if the form is readonly
    this.canvas.deleteButton.updateState(
      newValue && (this.form && !this.form.readOnly) && !this.disabled
    );
    this.canvas.selectorButton.updateState(
      this.form && !this.form.readOnly && !this.disabled
    );
    return this.Super('setValue', arguments);
  },
  isLoadedCurrentImage: function(imageId) {
    return (
      this.canvas.image &&
      this.canvas.image.src &&
      this.canvas.image.src.contains('ShowImage?id=' + imageId)
    );
  },
  refreshImage: function(imageId) {
    //If creating/replacing an image, the form is marked as modified
    //and the image id is set as the value of the item
    if (typeof imageId === 'undefined') {
      imageId = '';
    }
    this.setValue(imageId);
    this.form.itemChangeActions();
  },
  //This function has been overwritten because this class needs to do specific things if the object is
  //disabled. It is necessary to hide the delete and selector buttons when the status is disabled
  //and to show them when enabled.
  setDisabled: function(disabled) {
    if (disabled) {
      this.canvas.deleteButton.hide();
      this.canvas.selectorButton.hide();
    } else {
      this.canvas.deleteButton.show();
      this.canvas.selectorButton.show();
    }
    this.Super('setDisabled', arguments);
  }
});

//== OBImageSelector ==
//This class displays a selector in a popup which can be used to upload images
isc.defineClass('OBImageSelector', isc.VLayout);

isc.OBImageSelector.addProperties({
  submitButton: null,
  addForm: null,
  initWidget: function(args) {
    var imageId = this.imageItem.getValue();
    var view = args.form.view;
    var imageSizeAction = this.imageItem.imageSizeValuesAction;
    var imageWidthValue = this.imageItem.imageWidth;
    imageWidthValue = parseInt(imageWidthValue, 10);
    if (!imageWidthValue) {
      imageWidthValue = 0;
    }
    var imageHeightValue = this.imageItem.imageHeight;
    imageHeightValue = parseInt(imageHeightValue, 10);
    if (!imageHeightValue) {
      imageHeightValue = 0;
    }
    var form = isc.DynamicForm.create({
      autoFocus: true,
      fields: [
        {
          name: 'inpFile',
          title: OB.I18N.getLabel('OBUIAPP_ImageFile'),
          type: 'upload',
          canFocus: false,
          align: 'right'
        },
        {
          name: 'Command',
          type: 'hidden',
          value: 'SAVE_OB3'
        },
        {
          name: 'inpColumnName',
          type: 'hidden',
          value: args.columnName
        },
        {
          name: 'inpTabId',
          type: 'hidden',
          value: view.tabId
        },
        {
          name: 'inpadOrgId',
          type: 'hidden',
          value: args.form.values.organization
        },
        {
          name: 'parentObjectId',
          type: 'hidden',
          value: args.form.values.id
        },
        {
          name: 'imageId',
          type: 'hidden',
          value: imageId
        },
        {
          name: 'imageSizeAction',
          type: 'hidden',
          value: imageSizeAction
        },
        {
          name: 'imageWidthValue',
          type: 'hidden',
          value: imageWidthValue
        },
        {
          name: 'imageHeightValue',
          type: 'hidden',
          value: imageHeightValue
        },
        {
          name: 'inpSelectorId',
          type: 'hidden',
          value: this.ID
        }
      ],
      height: '20px',
      encoding: 'multipart',
      action: 'utility/ImageInfoBLOB',
      target: 'background_target',
      redraw: function() {}
    });
    this.formDeleteImage = isc.DynamicForm.create({
      fields: [
        {
          name: 'Command',
          type: 'hidden',
          value: 'DELETE_OB3'
        },
        {
          name: 'inpTabId',
          type: 'hidden',
          value: view.tabId
        },
        {
          name: 'imageId',
          type: 'hidden',
          value: imageId
        }
      ],
      height: '1px',
      width: '1px',
      encoding: 'normal',
      action: 'utility/ImageInfoBLOB',
      target: 'background_target',
      redraw: function() {}
    });

    var uploadbutton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OBUIAPP_Upload'),
      action: function() {
        var value = form.getItem('inpFile').getElement().value;
        if (!value) {
          return;
        }
        form.getField('Command').setValue('SAVE_OB3');
        form.submitForm();
      }
    });
    var messageBarText = this.getMessageText(
      'Warn',
      imageSizeAction,
      imageWidthValue,
      imageHeightValue
    );

    var messageBar = isc.OBMessageBar.create({
      visibility: 'hidden'
    });
    messageBar.setType(isc.OBMessageBar.TYPE_WARNING);
    messageBar.setText(null, messageBarText);
    messageBar.hideCloseIcon();
    if (messageBarText && (imageWidthValue || imageHeightValue)) {
      messageBar.show();
    }

    this.addMembers([
      isc.HLayout.create({
        width: '100%',
        height: 1,
        align: 'center',
        members: [messageBar]
      }),
      isc.HLayout.create({
        width: '100%',
        height: 20,
        layoutTopMargin: this.hlayoutTopMargin,
        layoutBottomMargin: this.hlayoutBottomMargin,
        align: 'center',
        members: [form, uploadbutton, this.formDeleteImage]
      })
    ]);
    this.Super('initWidget', arguments);
  },
  getMessageText: function(type, imageSizeAction, XXX, YYY, AAA, BBB, msgInfo) {
    var message = '';
    if (imageSizeAction === 'N') {
      return message;
    }
    if (imageSizeAction.indexOf('RESIZE') !== -1 && type === 'Confirm') {
      imageSizeAction = 'RESIZE';
    }
    if (!XXX) {
      XXX = 'ANY';
    }
    if (!YYY) {
      YYY = 'ANY';
    }
    if (!AAA) {
      AAA = 'ANY';
    }
    if (!BBB) {
      BBB = 'ANY';
    }
    message = OB.I18N.getLabel(
      'OBUIAPP_Image_' + type + '_' + imageSizeAction,
      [msgInfo]
    );
    message = message
      .replace('XXX', XXX)
      .replace('YYY', YYY)
      .replace('AAA', AAA)
      .replace('BBB', BBB);
    message = message.replace(/\n/g, '<br />');
    return message;
  },
  deleteTempImage: function(imageId) {
    if (imageId) {
      this.formDeleteImage.getField('imageId').setValue(imageId);
      this.formDeleteImage.submitForm();
    }
  },
  callback: function(
    imageId,
    imageSizeAction,
    oldWidth,
    oldHeight,
    newWidth,
    newHeight,
    msgInfo
  ) {
    oldWidth = parseInt(oldWidth, 10);
    oldHeight = parseInt(oldHeight, 10);
    newWidth = parseInt(newWidth, 10);
    newHeight = parseInt(newHeight, 10);
    var selector = this;
    if (
      imageSizeAction === 'WRONGFORMAT' ||
      imageSizeAction === 'ERROR_UPLOADING'
    ) {
      isc.warn(
        this.getMessageText(
          'Error',
          imageSizeAction,
          null,
          null,
          null,
          null,
          msgInfo
        ),
        function() {
          return true;
        },
        {
          icon: '[SKINIMG]Dialog/error.png',
          title: OB.I18N.getLabel('OBUIAPP_Error')
        }
      );
    } else if (
      imageSizeAction === 'ALLOWED' &&
      ((oldWidth !== 0 && oldWidth !== newWidth) ||
        (oldHeight !== 0 && oldHeight !== newHeight))
    ) {
      isc.warn(
        this.getMessageText(
          'Error',
          imageSizeAction,
          oldWidth,
          oldHeight,
          newWidth,
          newHeight
        ),
        function() {
          selector.deleteTempImage(imageId);
        },
        {
          icon: '[SKINIMG]Dialog/error.png',
          title: OB.I18N.getLabel('OBUIAPP_Error')
        }
      );
    } else if (
      imageSizeAction === 'ALLOWED_MINIMUM' &&
      ((oldWidth !== 0 && oldWidth > newWidth) ||
        (oldHeight !== 0 && oldHeight > newHeight))
    ) {
      isc.warn(
        this.getMessageText(
          'Error',
          imageSizeAction,
          oldWidth,
          oldHeight,
          newWidth,
          newHeight
        ),
        function() {
          selector.deleteTempImage(imageId);
        },
        {
          icon: '[SKINIMG]Dialog/error.png',
          title: OB.I18N.getLabel('OBUIAPP_Error')
        }
      );
    } else if (
      imageSizeAction === 'ALLOWED_MAXIMUM' &&
      ((oldWidth !== 0 && oldWidth < newWidth) ||
        (oldHeight !== 0 && oldHeight < newHeight))
    ) {
      isc.warn(
        this.getMessageText(
          'Error',
          imageSizeAction,
          oldWidth,
          oldHeight,
          newWidth,
          newHeight
        ),
        function() {
          selector.deleteTempImage(imageId);
        },
        {
          icon: '[SKINIMG]Dialog/error.png',
          title: OB.I18N.getLabel('OBUIAPP_Error')
        }
      );
    } else if (
      imageSizeAction === 'RECOMMENDED' &&
      ((oldWidth !== 0 && oldWidth !== newWidth) ||
        (oldHeight !== 0 && oldHeight !== newHeight))
    ) {
      isc.confirm(
        this.getMessageText(
          'Confirm',
          imageSizeAction,
          oldWidth,
          oldHeight,
          newWidth,
          newHeight
        ),
        function(clickedOK) {
          if (clickedOK) {
            selector.refreshImage(imageId);
          } else {
            selector.deleteTempImage(imageId);
          }
        }
      );
    } else if (
      imageSizeAction === 'RECOMMENDED_MINIMUM' &&
      ((oldWidth !== 0 && oldWidth > newWidth) ||
        (oldHeight !== 0 && oldHeight > newHeight))
    ) {
      isc.confirm(
        this.getMessageText(
          'Confirm',
          imageSizeAction,
          oldWidth,
          oldHeight,
          newWidth,
          newHeight
        ),
        function(clickedOK) {
          if (clickedOK) {
            selector.refreshImage(imageId);
          } else {
            selector.deleteTempImage(imageId);
          }
        }
      );
    } else if (
      imageSizeAction === 'RECOMMENDED_MAXIMUM' &&
      ((oldWidth !== 0 && oldWidth < newWidth) ||
        (oldHeight !== 0 && oldHeight < newHeight))
    ) {
      isc.confirm(
        this.getMessageText(
          'Confirm',
          imageSizeAction,
          oldWidth,
          oldHeight,
          newWidth,
          newHeight
        ),
        function(clickedOK) {
          if (clickedOK) {
            selector.refreshImage(imageId);
          } else {
            selector.deleteTempImage(imageId);
          }
        }
      );
    } else if (
      imageSizeAction.indexOf('RESIZE') !== -1 &&
      (oldWidth !== newWidth || oldHeight !== newHeight)
    ) {
      isc.confirm(
        this.getMessageText(
          'Confirm',
          imageSizeAction,
          newWidth,
          newHeight,
          oldWidth,
          oldHeight
        ),
        function(clickedOK) {
          if (clickedOK) {
            selector.refreshImage(imageId);
          } else {
            selector.deleteTempImage(imageId);
          }
        }
      );
    } else {
      this.refreshImage(imageId);
    }
  },
  refreshImage: function(imageId) {
    this.imageItem.refreshImage(imageId);
    this.parentElement.parentElement.closeClick();
  }
});
