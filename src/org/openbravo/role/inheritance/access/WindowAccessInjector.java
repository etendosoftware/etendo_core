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
 * All portions are Copyright (C) 2015 Openbravo SLU 
 * All Rights Reserved. 
 ************************************************************************
 */
package org.openbravo.role.inheritance.access;

import org.openbravo.base.structure.InheritedAccessEnabled;
import org.openbravo.model.ad.access.FieldAccess;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.TabAccess;
import org.openbravo.model.ad.access.WindowAccess;

/**
 * AccessTypeInjector for the WindowAccess class
 */
@AccessTypeInjector.Qualifier(WindowAccess.class)
public class WindowAccessInjector extends AccessTypeInjector {

  @Override
  public String getSecuredElementGetter() {
    return "getWindow";
  }

  @Override
  public int getPriority() {
    return 200;
  }

  @Override
  public String getSecuredElementName() {
    return WindowAccess.PROPERTY_WINDOW;
  }

  @Override
  public void setParent(InheritedAccessEnabled newAccess, InheritedAccessEnabled parentAccess,
      Role role) {
    super.setParent(newAccess, parentAccess, role);
    // We need to have the new window access in memory for the case where we are
    // adding tab accesses also (when adding a new inheritance)
    role.getADWindowAccessList().add((WindowAccess) newAccess);
  }

  @Override
  public void clearInheritFromFieldInChilds(InheritedAccessEnabled access, boolean clearAll) {
    if (access.getInheritedFrom() != null) {
      String inheritedFromId = access.getInheritedFrom().getId();
      WindowAccess wa = (WindowAccess) access;
      for (TabAccess ta : wa.getADTabAccessList()) {
        if (clearAll) {
          clearInheritedFromField(ta);
        } else {
          clearInheritedFromField(ta, inheritedFromId);
        }
        for (FieldAccess fa : ta.getADFieldAccessList()) {
          if (clearAll) {
            clearInheritedFromField(fa);
          } else {
            clearInheritedFromField(fa, inheritedFromId);
          }
        }
      }
    }
  }
}
