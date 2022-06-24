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

(function() {
  // Button in bp set window itself
  var BPSetButtonProps = isc.addProperties(
    {},
    isc.OBUploadView.UPLOAD_BUTTON_PROPERTIES,
    {
      actionUrl: './ApplicationDataUpload/ImportBPSet',
      inChildTab: false,
      title: OB.I18N.getLabel('OBUIAPP_ImportBPInBPSet'),
      prompt: OB.I18N.getLabel('OBUIAPP_ImportBPInBPSetPrompt'),
      buttonType: 'ob-upload-import-bp-in-bp-set',

      getPopupType: function() {
        return isc.OBUploadBPView;
      }
    }
  );
  OB.ToolbarRegistry.registerButton(
    BPSetButtonProps.buttonType,
    isc.OBToolbarIconButton,
    BPSetButtonProps,
    500,
    'BF972A02844E43AFAD23F3B25338E970'
  );

  // BP set line button
  var BPSetLineButtonProps = isc.addProperties(
    {},
    isc.OBUploadView.UPLOAD_BUTTON_PROPERTIES,
    {
      actionUrl: './ApplicationDataUpload/ImportBPSet',
      inChildTab: true,
      popupTitle: OB.I18N.getLabel('OBUIAPP_ImportBPInBPSet'),
      prompt: OB.I18N.getLabel('OBUIAPP_ImportBPInBPSetPrompt'),
      buttonType: 'ob-upload-import-bp-in-bp-set-line',

      getPopupType: function() {
        return isc.OBUploadBPView;
      }
    }
  );
  OB.ToolbarRegistry.registerButton(
    BPSetLineButtonProps.buttonType,
    isc.OBToolbarIconButton,
    BPSetLineButtonProps,
    510,
    '7D7E6951FF4945AE9CC556C36E680DBA'
  );
})();
