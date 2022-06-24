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
 * AccessTypeInjector for the FieldAccess class
 */
@AccessTypeInjector.Qualifier(FieldAccess.class)
public class FieldAccessInjector extends AccessTypeInjector {

  @Override
  public String getSecuredElementGetter() {
    return "getField";
  }

  @Override
  public int getPriority() {
    return 400;
  }

  @Override
  public String getSecuredElementName() {
    return FieldAccess.PROPERTY_FIELD;
  }

  @Override
  public Role getRole(InheritedAccessEnabled access) {
    // FieldAccess does not have role property as parent
    FieldAccess fieldAccess = (FieldAccess) access;
    if (fieldAccess.getTabAccess() == null
        || fieldAccess.getTabAccess().getWindowAccess() == null) {
      return null;
    }
    return fieldAccess.getTabAccess().getWindowAccess().getRole();
  }

  @Override
  public String getRoleProperty() {
    // FieldAccess does not have role property as parent
    return "tabAccess.windowAccess.role.id";
  }

  @Override
  public void setParent(InheritedAccessEnabled newAccess, InheritedAccessEnabled parentAccess,
      Role role) {
    // FieldAccess does not have role property as parent
    setParentTab((FieldAccess) newAccess, (FieldAccess) parentAccess, role);
  }

  /**
   * Sets the parent tab for a FieldAccess.
   * 
   * @param newFieldAccess
   *          FieldAccess whose parent tab will be set
   * @param parentFieldAccess
   *          FieldAccess used to retrieve the parent tab
   * @param role
   *          Parent role
   */
  private void setParentTab(FieldAccess newFieldAccess, FieldAccess parentFieldAccess, Role role) {
    String parentTabId = parentFieldAccess.getTabAccess().getTab().getId();
    for (WindowAccess wa : role.getADWindowAccessList()) {
      for (TabAccess ta : wa.getADTabAccessList()) {
        String currentTabId = ta.getTab().getId();
        if (currentTabId.equals(parentTabId)) {
          newFieldAccess.setTabAccess(ta);
          break;
        }
      }
    }
  }

  @Override
  public void removeReferenceInParentList(InheritedAccessEnabled access) {
    FieldAccess fa = (FieldAccess) access;
    boolean accessExists = OBDal.getInstance()
        .exists(TabAccess.ENTITY_NAME, fa.getTabAccess().getId());
    if (accessExists) {
      fa.getTabAccess().getADFieldAccessList().remove(fa);
    }
  }
}
