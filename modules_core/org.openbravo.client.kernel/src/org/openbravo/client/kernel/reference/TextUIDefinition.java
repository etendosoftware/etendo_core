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
 * All portions are Copyright (C) 2010-2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.kernel.reference;

import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.model.ad.ui.Field;

/**
 * Implements Text (textarea) UI Definition
 * 
 * @author iperdomo
 * 
 */
public class TextUIDefinition extends StringUIDefinition {

  // don't support sorting on large text fields
  @Override
  public String getGridFieldProperties(Field field) {
    final Property property = KernelUtils.getInstance().getPropertyFromColumn(field.getColumn());
    Boolean canFilter = true;
    String superFieldProps = super.getGridFieldProperties(field);
    // If there is a previous 'canSort' or 'canFilter' set, remove it to avoid collision when the
    // new one is set later
    superFieldProps = removeAttributeFromString(superFieldProps, "canSort");
    if (property.getFieldLength() > 4000) {
      // anything above 4000 is probably a clob
      canFilter = false;
      superFieldProps = removeAttributeFromString(superFieldProps, "canFilter");
    }

    return superFieldProps + ", canSort: false" + (canFilter ? "" : ", canFilter: false");
  }

  @Override
  public String getParentType() {
    return "textArea";
  }

  @Override
  public String getFormEditorType() {
    return "OBTextAreaItem";
  }

  @Override
  public String getFilterEditorType() {
    return "OBTextItem";
  }

  @Override
  public String getGridEditorType() {
    return "OBPopUpTextAreaItem";
  }
}
