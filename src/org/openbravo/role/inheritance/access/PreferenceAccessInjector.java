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
 * All portions are Copyright (C) 2015-2019 Openbravo SLU 
 * All Rights Reserved. 
 ************************************************************************
 */
package org.openbravo.role.inheritance.access;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.InheritedAccessEnabled;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.domain.Preference;

/**
 * AccessTypeInjector for the Preference class
 */
@AccessTypeInjector.Qualifier(Preference.class)
public class PreferenceAccessInjector extends AccessTypeInjector {

  private static final Set<String> propertyBlackList = new HashSet<String>(Arrays.asList(
      "OBUIAPP_RecentDocumentsList", "OBUIAPP_RecentViewList", "OBUIAPP_GridConfiguration",
      "OBUIAPP_DefaultSavedView", "UINAVBA_MenuRecentList", "UINAVBA_RecentLaunchList"));

  @Override
  public String getSecuredElementGetter() {
    return "getIdentifier";
  }

  @Override
  public String getSecuredElementName() {
    // This method is not explicitly use, as preferences are handled in a special way
    // See PreferenceAccessInjector.findAccess() method
    return Preference.PROPERTY_PROPERTY + "-" + Preference.PROPERTY_ATTRIBUTE;
  }

  @Override
  public boolean isInheritable(InheritedAccessEnabled access) {
    // An inheritable preference should define a role on its visibility settings and it must
    // not be present in the black list.
    Preference preference = (Preference) access;
    if (preference.getVisibleAtRole() != null) {
      return true;
    }
    if (preference.isPropertyList()) {
      return !propertyBlackList.contains(preference.getProperty());
    } else {
      return true;
    }
  }

  @Override
  public Role getRole(InheritedAccessEnabled access) {
    // Preference does not have role property as parent
    Preference preference = (Preference) access;
    if (preference.getVisibleAtRole() == null) {
      return null;
    }
    String roleId = preference.getVisibleAtRole().getId();
    return OBDal.getInstance().get(Role.class, roleId);
  }

  @Override
  public String getRoleProperty() {
    // Preference does not have role property as parent
    return "visibleAtRole.id";
  }

  @Override
  public void setParent(InheritedAccessEnabled newAccess, InheritedAccessEnabled parentAccess,
      Role role) {
    // Preference does not have role property as parent
    ((Preference) (newAccess)).setVisibleAtRole(role);
  }

  @Override
  public String addEntityWhereClause(String whereClause) {
    // Inheritable preferences are those that are not in the black list and also has a value in
    // the Visible At Role field
    //@formatter:off
    return whereClause + 
            "   and p.visibleAtRole is not null" +
            "   and p.property not in (:blackList)";
    //@formatter:on
  }

  @Override
  public <T extends BaseOBObject> void doEntityParameterReplacement(OBQuery<T> query) {
    query.setNamedParameter("blackList", propertyBlackList);
  }

  @Override
  public String getSecuredElementIdentifier(InheritedAccessEnabled access) {
    Preference preference = (Preference) access;
    String identifier = preference.isPropertyList() ? preference.getProperty()
        : preference.getAttribute();
    String isPropertyList = preference.isPropertyList() ? "Y" : "N";
    String visibleAtClient = preference.getVisibleAtClient() != null
        ? preference.getVisibleAtClient().getId()
        : " ";
    String visibleAtOrg = preference.getVisibleAtOrganization() != null
        ? preference.getVisibleAtOrganization().getId()
        : " ";
    String visibleAtUser = preference.getUserContact() != null ? preference.getUserContact().getId()
        : " ";
    String visibleAtWindow = preference.getWindow() != null ? preference.getWindow().getId() : " ";
    return identifier + "_" + isPropertyList + "_" + visibleAtClient + "_" + visibleAtOrg + "_"
        + visibleAtUser + "_" + visibleAtWindow;
  }

  @Override
  public InheritedAccessEnabled findAccess(InheritedAccessEnabled access, String roleId) {
    Preference preference = (Preference) access;
    String property = preference.isPropertyList() ? preference.getProperty()
        : preference.getAttribute();
    if (propertyBlackList.contains(property)) {
      return null;
    }
    String clientId = preference.getVisibleAtClient() != null
        ? preference.getVisibleAtClient().getId()
        : null;
    String orgId = preference.getVisibleAtOrganization() != null
        ? preference.getVisibleAtOrganization().getId()
        : null;
    String userId = preference.getUserContact() != null ? preference.getUserContact().getId()
        : null;
    String windowId = preference.getWindow() != null ? preference.getWindow().getId() : null;
    List<Preference> prefList = Preferences.getPreferences(property, preference.isPropertyList(),
        clientId, orgId, userId, roleId, windowId);
    if (prefList.size() == 0) {
      return null;
    }
    for (Preference pref : prefList) {
      if (pref.getInheritedFrom() != null) {
        return pref;
      }
    }
    return prefList.get(0);
  }

  @Override
  public List<String> getSkippedProperties() {
    List<String> skippedProperties = super.getSkippedProperties();
    skippedProperties.add("visibleAtRole");
    return skippedProperties;
  }

  @Override
  public void checkAccessExistence(InheritedAccessEnabled access) {
    Preference preference = (Preference) access;
    if (Preferences.existsPreference(preference)) {
      Utility.throwErrorMessage("DuplicatedPreferenceForTemplate");
    }
  }
}
