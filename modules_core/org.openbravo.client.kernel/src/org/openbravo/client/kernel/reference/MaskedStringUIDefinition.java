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
 * All portions are Copyright (C) 2013 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel.reference;

import java.util.List;

import org.openbravo.client.kernel.ReferencedMask;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Field;

public class MaskedStringUIDefinition extends StringUIDefinition {
  @Override
  public String getFieldProperties(Field field) {
    if (field.getColumn().getReferenceSearchKey() != null) {
      Reference adReferenceSearch = field.getColumn().getReferenceSearchKey();
      List<ReferencedMask> adReferenceSearchList = adReferenceSearch.getOBCLKERREFMASKList();
      ReferencedMask adReferenceMask;
      if (adReferenceSearchList != null && adReferenceSearchList.size() > 0) {
        adReferenceMask = adReferenceSearchList.get(0);
        return "{mask: '" + adReferenceMask.getCommercialName() + "'}";
      } else {
        return "{mask: '" + "'}";
      }
    } else {
      return "{mask: '" + "'}";
    }
  }
}
