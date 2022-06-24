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
  // Import Product in Discounts
  var ProductDiscountButtonProps = isc.addProperties(
    {},
    isc.OBUploadView.UPLOAD_BUTTON_PROPERTIES,
    {
      actionUrl: './ApplicationDataUpload/ImportProductInDiscount',
      inChildTab: true,
      popupTitle: OB.I18N.getLabel('OBUIAPP_ImportProductInDiscount'),
      prompt: OB.I18N.getLabel('OBUIAPP_ImportProductInDiscountPrompt'),
      buttonType: 'ob-upload-import-product-in-discount'
    }
  );
  OB.ToolbarRegistry.registerButton(
    ProductDiscountButtonProps.buttonType,
    isc.OBToolbarIconButton,
    ProductDiscountButtonProps,
    500,
    '800082'
  );
})();
