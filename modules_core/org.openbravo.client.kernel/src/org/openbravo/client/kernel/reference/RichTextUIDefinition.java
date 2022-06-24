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
 * All portions are Copyright (C) 2010-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.kernel.reference;

import org.openbravo.model.ad.ui.Field;

/**
 * Implements Rich Text UI Definition
 * 
 * 
 */
public class RichTextUIDefinition extends TextUIDefinition {

  // don't support sorting on large text fields

  @Override
  public String getParentType() {
    return "RichTextEditor";
  }

  @Override
  public String getFormEditorType() {
    return "OBRichTextItem";
  }

  @Override
  public String getGridFieldProperties(Field field) {
    Long rowSpan = field.getObuiappRowspan();
    Long colSpan = field.getObuiappColspan();
    if ((colSpan == null) || (colSpan < 2)) {
      colSpan = 2L;
    }
    if ((rowSpan == null) || (rowSpan < 2)) {
      rowSpan = 2L;
    }

    return super.getGridFieldProperties(field) + ",  escapeHTML: true , canEdit: false , rowSpan : "
        + rowSpan + " , colSpan : " + colSpan;
  }

}
