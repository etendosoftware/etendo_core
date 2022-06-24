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
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.FieldAccess;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.TabAccess;
import org.openbravo.model.ad.access.WindowAccess;

/**
 * AccessTypeInjector for the TabAccess class
 */
@AccessTypeInjector.Qualifier(TabAccess.class)
public class TabAccessInjector extends AccessTypeInjector {

  @Override
  public String getSecuredElementGetter() {
    return "getTab";
  }

  @Override
  public int getPriority() {
    return 300;
  }

  @Override
  public String getSecuredElementName() {
    return TabAccess.PROPERTY_TAB;
  }

  @Override
  public Role getRole(InheritedAccessEnabled access) {
    // TabAccess does not have role property as parent
    TabAccess tabAccess = (TabAccess) access;
    if (tabAccess.getWindowAccess() == null) {
      return null;
    }
    return tabAccess.getWindowAccess().getRole();
  }

  @Override
  public String getRoleProperty() {
    // TabAccess does not have role property as parent
    return "windowAccess.role.id";
  }

  @Override
  public void setParent(InheritedAccessEnabled newAccess, InheritedAccessEnabled parentAccess,
      Role role) {
    // TabAccess does not have role property as parent
    TabAccess newTabAccess = (TabAccess) newAccess;
    TabAccess parentTabAccess = (TabAccess) parentAccess;
    setParentWindow(newTabAccess, parentTabAccess, role);
    // We need to have the new tab access in memory for the case where we are
    // adding field accesses also (when adding a new inheritance)
    newTabAccess.getWindowAccess().getADTabAccessList().add(newTabAccess);
  }

  /**
   * Sets the parent window for a TabAccess.
   * 
   * @param newTabAccess
   *          TabAccess whose parent window will be set
   * @param parentTabAccess
   *          TabAccess used to retrieve the parent window
   * @param role
   *          Parent role
   */
  private void setParentWindow(TabAccess newTabAccess, TabAccess parentTabAccess, Role role) {
    String parentWindowId = parentTabAccess.getWindowAccess().getWindow().getId();
    for (WindowAccess wa : role.getADWindowAccessList()) {
      String currentWindowId = wa.getWindow().getId();
      if (currentWindowId.equals(parentWindowId)) {
        newTabAccess.setWindowAccess(wa);
        break;
      }
    }
  }

  @Override
  public void clearInheritFromFieldInChilds(InheritedAccessEnabled access, boolean clearAll) {
    if (access.getInheritedFrom() != null) {
      String inheritedFromId = access.getInheritedFrom().getId();
      TabAccess ta = (TabAccess) access;
      for (FieldAccess fa : ta.getADFieldAccessList()) {
        if (clearAll) {
          clearInheritedFromField(fa);
        } else {
          clearInheritedFromField(fa, inheritedFromId);
        }
      }
    }
  }

  @Override
  public void removeReferenceInParentList(InheritedAccessEnabled access) {
    TabAccess ta = (TabAccess) access;
    boolean accessExists = OBDal.getInstance()
        .exists(WindowAccess.ENTITY_NAME, ta.getWindowAccess().getId());
    if (accessExists) {
      ta.getWindowAccess().getADTabAccessList().remove(ta);
    }
  }
}
