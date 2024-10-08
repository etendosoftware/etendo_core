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
 * All portions are Copyright (C) 2022 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

isc.ClassFactory.defineClass('UnredrawableDynamicForm', isc.DynamicForm);
isc.UnredrawableDynamicForm.addProperties({
  // Override redraw implementation to prevent FileItem content to be lost on redraws
  redraw: function() {}
});

//== OBProcessFileUpload ==
//This class is used to upload files to a process definition
isc.ClassFactory.defineClass('OBProcessFileUpload', isc.FileItem);

isc.OBProcessFileUpload.addProperties({
  multiple: false, // Allows only one file per parameter
  editFormConstructor: 'UnredrawableDynamicForm',
  setDisabled: function(disabled) {
    // Disable the UploadItem contained within this component
    this.form
      .getItem(this.name)
      .editForm.getItem(0)
      .setDisabled(disabled);
  },
  fileSizeIsAboveMax: function(fileItem) {
    const maxFileSizeStr = OB.PropertyStore.get('OBUIAPP_ProcessFileUploadMaxSize');
    const BYTES_IN_A_MEGABYTE = 1024 * 1024;
    const maxFileSize = Number(maxFileSizeStr);
    if (isNaN(maxFileSize) || maxFileSize < 0) {
      return false;
    }
    if (fileItem && typeof fileItem.size === 'number') {
      const fileSizeMB = fileItem.size / BYTES_IN_A_MEGABYTE;
      return fileSizeMB > maxFileSize;
    }

    return false;
  },
  validators: [
    {
      type: 'custom',
      condition: function(item) {
        const fileItem = item.form
          .getItem(item.name)
          .editForm.getItem(0)
          .getElement().files[0];

        if (item.fileSizeIsAboveMax(fileItem)) {
          item.view.messageBar.setMessage(
            isc.OBMessageBar.TYPE_ERROR,
            null,
            OB.I18N.getLabel('OBUIAPP_ProcessFileMaxSizeExceeded', [
              fileItem.name,
              OB.PropertyStore.get('OBUIAPP_ProcessFileUploadMaxSize')
            ])
          );
          return false;
        }

        return true;
      }
    }
  ]
});
