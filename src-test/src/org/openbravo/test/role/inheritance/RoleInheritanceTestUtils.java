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
 * All portions are Copyright (C) 2015-2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.role.inheritance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.OBUIAPPViewImplementation;
import org.openbravo.client.application.ViewRoleAccess;
import org.openbravo.client.myob.WidgetClass;
import org.openbravo.client.myob.WidgetClassAccess;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.FieldAccess;
import org.openbravo.model.ad.access.FormAccess;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleInheritance;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.access.TabAccess;
import org.openbravo.model.ad.access.TableAccess;
import org.openbravo.model.ad.access.WindowAccess;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Form;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.enterprise.Organization;

/**
 * This class provides some utility methods used to test the Role Inheritance functionality
 * 
 */
public class RoleInheritanceTestUtils {
  /**
   * F&amp;B International Group client id
   */
  public final static String CLIENT_ID = "23C59575B9CF467C9620760EB255B389";
  /**
   * zero organization id
   */
  public final static String ASTERISK_ORG_ID = "0";
  /**
   * list with the available access types
   */
  public final static List<String> ACCESS_NAMES = Arrays.asList("ORGANIZATION", "WINDOW", "TAB",
      "FIELD", "REPORT", "FORM", "WIDGET", "VIEW", "PROCESS", "TABLE", "ALERT", "PREFERENCE");

  public static final String DUMMY_VIEW_IMPL_NAME = "OBUIAPP_DummyView";

  /**
   * Creates a new role
   * 
   * @param name
   *          the name of the role
   * @param clientId
   *          role client id
   * @param organizationId
   *          role organization id
   * @param userLevel
   *          user level of the role's organization
   * @param isManual
   *          defines if the role is manual (true) or automatic (false)
   * @param isTemplate
   *          defines if the role is template (true) or not (false)
   * 
   * @return the new created role
   */
  public static Role createRole(String name, String clientId, String organizationId,
      String userLevel, boolean isManual, boolean isTemplate) {
    final Role role = OBProvider.getInstance().get(Role.class);
    Client client = OBDal.getInstance().get(Client.class, clientId);
    Organization org = OBDal.getInstance().get(Organization.class, organizationId);
    role.setClient(client);
    role.setOrganization(org);
    role.setTemplate(isTemplate);
    role.setManual(isManual);
    role.setName(name);
    role.setUserLevel(userLevel);
    OBDal.getInstance().save(role);
    return role;
  }

  /**
   * Deletes a role
   * 
   * @param role
   *          A Role object which will be deleted
   */
  public static void deleteRole(Role role) {
    OBDal.getInstance().remove(role);
  }

  /**
   * Adds a new inheritance between two roles
   * 
   * @param role
   *          The role owner of the inheritance
   * @param template
   *          The template role whose permissions will be inherited
   * @param sequenceNumber
   *          Sequence number to assign to the inheritance
   */
  public static void addInheritance(Role role, Role template, Long sequenceNumber) {
    final RoleInheritance inheritance = OBProvider.getInstance().get(RoleInheritance.class);
    inheritance.setClient(role.getClient());
    inheritance.setOrganization(role.getOrganization());
    inheritance.setRole(role);
    inheritance.setInheritFrom(template);
    inheritance.setSequenceNumber(sequenceNumber);
    OBDal.getInstance().save(inheritance);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(role);
  }

  /**
   * Removes an existing inheritance
   * 
   * @param role
   *          The role owner of the inheritance
   * @param template
   *          The template role whose permissions are being inherited
   */
  public static void removeInheritance(Role role, Role template) {
    final OBCriteria<RoleInheritance> obCriteria = OBDal.getInstance()
        .createCriteria(RoleInheritance.class);
    obCriteria.add(Restrictions.eq(RoleInheritance.PROPERTY_ROLE, role));
    obCriteria.add(Restrictions.eq(RoleInheritance.PROPERTY_INHERITFROM, template));
    obCriteria.setMaxResults(1);
    RoleInheritance roleInheritance = (RoleInheritance) obCriteria.uniqueResult();
    OBDal.getInstance().remove(roleInheritance);
  }

  /**
   * Creates a new permission and assigns it to a role
   * 
   * @param type
   *          A String which represents the type of the access
   * @param role
   *          The role to assign the permission
   * @param accessName
   *          The name of the particular permission to assign
   */
  public static void addAccess(String type, Role role, String accessName) {
    if ("ORGANIZATION".equals(type)) {
      addOrgAccess(role, accessName, true);
    } else if ("WINDOW".equals(type)) {
      addWindowAccess(role, accessName, true);
    } else if ("TAB".equals(type)) {
      // Create tab access for Business Partner window
      addTabAccess(role, "Business Partner", accessName, true, true);
    } else if ("FIELD".equals(type)) {
      // Create field access for header tab of Business Partner window
      addFieldAccess(role, "Business Partner", "Business Partner", accessName, true, true, true,
          true);
    } else if ("REPORT".equals(type)) {
      addReportAndProcessAccess(role, accessName);
    } else if ("FORM".equals(type)) {
      addFormAccess(role, accessName);
    } else if ("WIDGET".equals(type)) {
      addWidgetAccess(role, accessName);
    } else if ("VIEW".equals(type)) {
      addViewImplementationAccess(role, accessName);
    } else if ("PROCESS".equals(type)) {
      addProcessDefinitionAccess(role, accessName);
    } else if ("TABLE".equals(type)) {
      addTableAccess(role, accessName, true);
    } else if ("ALERT".equals(type)) {
      addAlertRecipient(role, accessName);
    } else if ("PREFERENCE".equals(type)) {
      addPreference(role, accessName, "");
    }
  }

  /**
   * Removes a permission assigned to a role
   * 
   * @param type
   *          A String which represents the type of the access
   * @param role
   *          The role owner of the permission
   */
  public static void removeAccesses(String type, Role role) {
    if ("ORGANIZATION".equals(type)) {
      removeOrgAccesses(role);
    } else if ("WINDOW".equals(type)) {
      removeWindowAccesses(role);
    } else if ("TAB".equals(type)) {
      // Remove tab access for Business Partner window
      removeTabAccesses(role, "Business Partner");
    } else if ("FIELD".equals(type)) {
      // Remove field access for header tab of Business Partner window
      removeFieldAccesses(role, "Business Partner", "Business Partner");
    } else if ("REPORT".equals(type)) {
      removeReportAndProcessAccesses(role);
    } else if ("FORM".equals(type)) {
      removeFormAccesses(role);
    } else if ("WIDGET".equals(type)) {
      removeWidgetClassAccesses(role);
    } else if ("VIEW".equals(type)) {
      removeViewImplementationAccesses(role);
    } else if ("PROCESS".equals(type)) {
      removeProcessDefinitionAccesses(role);
    } else if ("TABLE".equals(type)) {
      removeTableAccesses(role);
    } else if ("ALERT".equals(type)) {
      removeAlertRecipients(role);
    } else if ("PREFERENCE".equals(type)) {
      removePreferences(role);
    }
  }

  /**
   * Updates an existing permission assigned to a role
   * 
   * @param type
   *          A String which represents the type of the access
   * @param role
   *          The role owner of the permission
   * @param accessName
   *          The name of the particular permission to update
   * @param editedValue
   *          The edited value to assign to the permission
   * @param isActive
   *          a flag to activate (true) or deactivate (false) the permission
   */
  public static void updateAccess(String type, Role role, String accessName, boolean editedValue,
      boolean isActive) {
    if ("ORGANIZATION".equals(type)) {
      updateOrgAccess(role, accessName, editedValue, isActive);
    } else if ("WINDOW".equals(type)) {
      updateWindowAccess(role, accessName, editedValue, isActive);
    } else if ("TAB".equals(type)) {
      // Update information for Business Partner window tabs
      updateTabAccess(role, "Business Partner", accessName, editedValue, isActive);
    } else if ("FIELD".equals(type)) {
      // Update information for fields in header tab of Business Partner window
      updateFieldAccess(role, "Business Partner", "Business Partner", accessName, editedValue,
          isActive);
    } else if ("REPORT".equals(type)) {
      updateReportAndProcessAccess(role, accessName, isActive);
    } else if ("FORM".equals(type)) {
      updateFormAccess(role, accessName, isActive);
    } else if ("WIDGET".equals(type)) {
      updateWidgetAccess(role, accessName, isActive);
    } else if ("VIEW".equals(type)) {
      updateViewImplementationAccess(role, accessName, isActive);
    } else if ("PROCESS".equals(type)) {
      updateProcessDefinitionAccess(role, accessName, isActive);
    } else if ("TABLE".equals(type)) {
      updateTableAccess(role, accessName, editedValue, isActive);
    } else if ("ALERT".equals(type)) {
      updateAlertRecipientAccess(role, accessName, editedValue, isActive);
    } else if ("PREFERENCE".equals(type)) {
      updatePreference(role, accessName, editedValue, isActive);
    }
  }

  /**
   * Retrieves the information of a permission assigned to a role
   * 
   * @param type
   *          A String which represents the type of the access
   * @param role
   *          The role owner of the permission
   * @param accessName
   *          The name of the particular permission to retrieve
   * @return An array of Strings with the permission information
   */
  public static String[] getAccessInfo(String type, Role role, String accessName) {
    if ("ORGANIZATION".equals(type)) {
      return getOrgAccessInfo(role, accessName);
    } else if ("WINDOW".equals(type)) {
      return getWindowAccessInfo(role, accessName);
    } else if ("TAB".equals(type)) {
      // Get information for Business Partner window tabs
      return getTabAccessInfo(role, "Business Partner", accessName);
    } else if ("FIELD".equals(type)) {
      // Get information for fields in header tab of Business Partner window
      return getFieldAccessInfo(role, "Business Partner", "Business Partner", accessName);
    } else if ("REPORT".equals(type)) {
      return getReportAndProcessAccessInfo(role, accessName);
    } else if ("FORM".equals(type)) {
      return getFormAccessInfo(role, accessName);
    } else if ("WIDGET".equals(type)) {
      return getWidgetAccessInfo(role, accessName);
    } else if ("VIEW".equals(type)) {
      return getViewImplementationAccessInfo(role, accessName);
    } else if ("PROCESS".equals(type)) {
      return getProcessDefinitonAccessInfo(role, accessName);
    } else if ("TABLE".equals(type)) {
      return getTableAccessInfo(role, accessName);
    } else if ("ALERT".equals(type)) {
      return getAlertRecipientAccessInfo(role, accessName);
    } else if ("PREFERENCE".equals(type)) {
      return getPreferenceInfo(role, accessName);
    } else {
      return null;
    }
  }

  private static void addOrgAccess(Role role, String orgName, boolean orgAdmin) {
    final RoleOrganization orgAccess = OBProvider.getInstance().get(RoleOrganization.class);
    final OBCriteria<Organization> obCriteria = OBDal.getInstance()
        .createCriteria(Organization.class);
    obCriteria.add(Restrictions.eq(Organization.PROPERTY_NAME, orgName));
    obCriteria.setMaxResults(1);
    orgAccess.setClient(role.getClient());
    orgAccess.setRole(role);
    orgAccess.setOrganization((Organization) obCriteria.uniqueResult());
    orgAccess.setOrgAdmin(orgAdmin);
    OBDal.getInstance().save(orgAccess);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(role);
  }

  private static void updateOrgAccess(Role role, String orgName, boolean isOrgAdmin,
      boolean isActive) {
    final OBCriteria<Organization> orgCriteria = OBDal.getInstance()
        .createCriteria(Organization.class);
    orgCriteria.add(Restrictions.eq(Window.PROPERTY_NAME, orgName));
    orgCriteria.setMaxResults(1);
    final OBCriteria<RoleOrganization> orgAccessCriteria = OBDal.getInstance()
        .createCriteria(RoleOrganization.class);
    orgAccessCriteria.add(Restrictions.eq(RoleOrganization.PROPERTY_ROLE, role));
    orgAccessCriteria
        .add(Restrictions.eq(RoleOrganization.PROPERTY_ORGANIZATION, orgCriteria.uniqueResult()));
    orgAccessCriteria.setMaxResults(1);
    RoleOrganization ro = (RoleOrganization) orgAccessCriteria.uniqueResult();
    ro.setOrgAdmin(isOrgAdmin);
    ro.setActive(isActive);
  }

  private static void removeOrgAccesses(Role role) {
    final OBCriteria<RoleOrganization> obCriteria = OBDal.getInstance()
        .createCriteria(RoleOrganization.class);
    obCriteria.add(Restrictions.eq(RoleOrganization.PROPERTY_ROLE, role));
    obCriteria.setFilterOnActive(false);
    for (RoleOrganization ro : obCriteria.list()) {
      role.getADRoleOrganizationList().remove(ro);
      OBDal.getInstance().remove(ro);
    }
  }

  private static String[] getOrgAccessInfo(Role role, String orgName) {
    String result[] = new String[3];
    for (RoleOrganization ro : role.getADRoleOrganizationList()) {
      if (orgName.equals(ro.getOrganization().getName())) {
        result[0] = ro.isOrgAdmin().toString();
        result[1] = ro.isActive().toString();
        result[2] = ro.getInheritedFrom() != null ? ro.getInheritedFrom().getId() : "";
        break;
      }
    }
    return result;
  }

  private static WindowAccess addWindowAccess(Role role, String windowName, boolean editableField) {
    final WindowAccess windowAccess = OBProvider.getInstance().get(WindowAccess.class);
    final OBCriteria<Window> obCriteria = OBDal.getInstance().createCriteria(Window.class);
    obCriteria.add(Restrictions.eq(Window.PROPERTY_NAME, windowName));
    obCriteria.setMaxResults(1);
    windowAccess.setClient(role.getClient());
    windowAccess.setOrganization(role.getOrganization());
    windowAccess.setRole(role);
    windowAccess.setWindow((Window) obCriteria.uniqueResult());
    windowAccess.setEditableField(editableField);
    OBDal.getInstance().save(windowAccess);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(role);
    return windowAccess;
  }

  private static void updateWindowAccess(Role role, String windowName, boolean editableField,
      boolean isActive) {
    final OBCriteria<Window> windowCriteria = OBDal.getInstance().createCriteria(Window.class);
    windowCriteria.add(Restrictions.eq(Window.PROPERTY_NAME, windowName));
    windowCriteria.setMaxResults(1);
    final OBCriteria<WindowAccess> windowAccessCriteria = OBDal.getInstance()
        .createCriteria(WindowAccess.class);
    windowAccessCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_ROLE, role));
    windowAccessCriteria
        .add(Restrictions.eq(WindowAccess.PROPERTY_WINDOW, windowCriteria.uniqueResult()));
    windowAccessCriteria.setMaxResults(1);
    WindowAccess wa = (WindowAccess) windowAccessCriteria.uniqueResult();
    wa.setEditableField(editableField);
    wa.setActive(isActive);
  }

  private static void removeWindowAccesses(Role role) {
    final OBCriteria<WindowAccess> obCriteria = OBDal.getInstance()
        .createCriteria(WindowAccess.class);
    obCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_ROLE, role));
    obCriteria.setFilterOnActive(false);
    for (WindowAccess wa : obCriteria.list()) {
      role.getADWindowAccessList().remove(wa);
      OBDal.getInstance().remove(wa);
    }
  }

  private static String[] getWindowAccessInfo(Role role, String windowName) {
    String result[] = new String[3];
    for (WindowAccess wa : role.getADWindowAccessList()) {
      if (windowName.equals(wa.getWindow().getName())) {
        result[0] = wa.isEditableField().toString();
        result[1] = wa.isActive().toString();
        result[2] = wa.getInheritedFrom() != null ? wa.getInheritedFrom().getId() : "";
        break;
      }
    }
    return result;
  }

  private static TabAccess addTabAccess(Role role, String windowName, String tabName,
      boolean editableField, boolean editableTab) {

    final OBCriteria<Window> windowCriteria = OBDal.getInstance().createCriteria(Window.class);
    windowCriteria.add(Restrictions.eq(Window.PROPERTY_NAME, windowName));
    windowCriteria.setMaxResults(1);
    Window window = (Window) windowCriteria.uniqueResult();

    final OBCriteria<WindowAccess> waCriteria = OBDal.getInstance()
        .createCriteria(WindowAccess.class);
    waCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_ROLE, role));
    waCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_WINDOW, window));
    waCriteria.setMaxResults(1);
    WindowAccess wa = (WindowAccess) waCriteria.uniqueResult();
    if (wa == null) {
      // Window access does not exists, create it
      wa = addWindowAccess(role, windowName, editableField);
    }

    final TabAccess tabAccess = OBProvider.getInstance().get(TabAccess.class);
    final OBCriteria<Tab> obCriteria = OBDal.getInstance().createCriteria(Tab.class);
    obCriteria.add(Restrictions.eq(Tab.PROPERTY_NAME, tabName));
    obCriteria.add(Restrictions.eq(Tab.PROPERTY_WINDOW, window));
    obCriteria.setMaxResults(1);
    Tab tab = (Tab) obCriteria.uniqueResult();

    tabAccess.setClient(role.getClient());
    tabAccess.setOrganization(role.getOrganization());
    tabAccess.setWindowAccess(wa);
    tabAccess.setTab(tab);
    tabAccess.setEditableField(editableTab);
    OBDal.getInstance().save(tabAccess);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(role);
    return tabAccess;
  }

  private static void updateTabAccess(Role role, String windowName, String tabName,
      boolean editableTab, boolean isActive) {
    final OBCriteria<Window> windowCriteria = OBDal.getInstance().createCriteria(Window.class);
    windowCriteria.add(Restrictions.eq(Window.PROPERTY_NAME, windowName));
    windowCriteria.setMaxResults(1);
    Window window = (Window) windowCriteria.uniqueResult();

    final OBCriteria<WindowAccess> waCriteria = OBDal.getInstance()
        .createCriteria(WindowAccess.class);
    waCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_ROLE, role));
    waCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_WINDOW, window));
    waCriteria.setMaxResults(1);

    final OBCriteria<Tab> tabCriteria = OBDal.getInstance().createCriteria(Tab.class);
    tabCriteria.add(Restrictions.eq(Tab.PROPERTY_WINDOW, window));
    tabCriteria.add(Restrictions.eq(Tab.PROPERTY_NAME, tabName));
    tabCriteria.setMaxResults(1);

    final OBCriteria<TabAccess> tabAccessCriteria = OBDal.getInstance()
        .createCriteria(TabAccess.class);
    tabAccessCriteria
        .add(Restrictions.eq(TabAccess.PROPERTY_WINDOWACCESS, waCriteria.uniqueResult()));
    tabAccessCriteria.add(Restrictions.eq(TabAccess.PROPERTY_TAB, tabCriteria.uniqueResult()));
    tabAccessCriteria.setMaxResults(1);
    TabAccess ta = (TabAccess) tabAccessCriteria.uniqueResult();
    ta.setEditableField(editableTab);
    ta.setActive(isActive);
  }

  private static void removeTabAccesses(Role role, String windowName) {
    final OBCriteria<Window> windowCriteria = OBDal.getInstance().createCriteria(Window.class);
    windowCriteria.add(Restrictions.eq(Window.PROPERTY_NAME, windowName));
    windowCriteria.setMaxResults(1);
    Window window = (Window) windowCriteria.uniqueResult();

    final OBCriteria<WindowAccess> obCriteria = OBDal.getInstance()
        .createCriteria(WindowAccess.class);
    obCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_ROLE, role));
    obCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_WINDOW, window));
    obCriteria.setFilterOnActive(false);
    WindowAccess wa = (WindowAccess) obCriteria.uniqueResult();
    List<TabAccess> tabAccessToDelete = new ArrayList<TabAccess>();
    for (TabAccess ta : wa.getADTabAccessList()) {
      tabAccessToDelete.add(ta);
    }
    for (TabAccess ta : tabAccessToDelete) {
      wa.getADTabAccessList().remove(ta);
      OBDal.getInstance().remove(ta);
    }
  }

  private static String[] getTabAccessInfo(Role role, String windowName, String tabName) {
    String result[] = new String[3];
    for (WindowAccess wa : role.getADWindowAccessList()) {
      if (windowName.equals(wa.getWindow().getName())) {
        for (TabAccess ta : wa.getADTabAccessList()) {
          if (tabName.equals(ta.getTab().getName())) {
            result[0] = ta.isEditableField().toString();
            result[1] = ta.isActive().toString();
            result[2] = ta.getInheritedFrom() != null ? ta.getInheritedFrom().getId() : "";
            break;
          }
        }
      }
    }
    return result;
  }

  private static void addFieldAccess(Role role, String windowName, String tabName, String fieldName,
      boolean editableField, boolean editableTab, boolean editableInField, boolean checkOnSave) {

    final OBCriteria<Window> windowCriteria = OBDal.getInstance().createCriteria(Window.class);
    windowCriteria.add(Restrictions.eq(Window.PROPERTY_NAME, windowName));
    windowCriteria.setMaxResults(1);
    Window window = (Window) windowCriteria.uniqueResult();

    final OBCriteria<WindowAccess> waCriteria = OBDal.getInstance()
        .createCriteria(WindowAccess.class);
    waCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_ROLE, role));
    waCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_WINDOW, window));
    waCriteria.setMaxResults(1);

    final OBCriteria<Tab> tabCriteria = OBDal.getInstance().createCriteria(Tab.class);
    tabCriteria.add(Restrictions.eq(Tab.PROPERTY_NAME, tabName));
    tabCriteria.add(Restrictions.eq(Tab.PROPERTY_WINDOW, window));
    tabCriteria.setMaxResults(1);
    Tab tab = (Tab) tabCriteria.uniqueResult();

    final OBCriteria<TabAccess> taCriteria = OBDal.getInstance().createCriteria(TabAccess.class);
    taCriteria.add(Restrictions.eq(TabAccess.PROPERTY_WINDOWACCESS, waCriteria.uniqueResult()));
    taCriteria.add(Restrictions.eq(TabAccess.PROPERTY_TAB, tab));
    taCriteria.setMaxResults(1);

    TabAccess ta = (TabAccess) taCriteria.uniqueResult();
    if (ta == null) {
      // Window access does not exists, create it
      ta = addTabAccess(role, windowName, tabName, editableField, editableTab);
    }

    final FieldAccess fieldAccess = OBProvider.getInstance().get(FieldAccess.class);

    final OBCriteria<Field> obCriteria = OBDal.getInstance().createCriteria(Field.class);
    obCriteria.add(Restrictions.eq(Field.PROPERTY_NAME, fieldName));
    obCriteria.add(Restrictions.eq(Field.PROPERTY_TAB, tab));
    obCriteria.setMaxResults(1);
    Field field = (Field) obCriteria.uniqueResult();

    fieldAccess.setClient(role.getClient());
    fieldAccess.setOrganization(role.getOrganization());
    fieldAccess.setTabAccess(ta);
    fieldAccess.setField(field);
    fieldAccess.setEditableField(editableInField);
    fieldAccess.setCheckonsave(checkOnSave);
    OBDal.getInstance().save(fieldAccess);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(role);
  }

  private static void updateFieldAccess(Role role, String windowName, String tabName,
      String fieldName, boolean editableField, boolean isActive) {
    final OBCriteria<Window> windowCriteria = OBDal.getInstance().createCriteria(Window.class);
    windowCriteria.add(Restrictions.eq(Window.PROPERTY_NAME, windowName));
    windowCriteria.setMaxResults(1);
    Window window = (Window) windowCriteria.uniqueResult();

    final OBCriteria<WindowAccess> waCriteria = OBDal.getInstance()
        .createCriteria(WindowAccess.class);
    waCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_ROLE, role));
    waCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_WINDOW, window));
    waCriteria.setMaxResults(1);

    final OBCriteria<Tab> tabCriteria = OBDal.getInstance().createCriteria(Tab.class);
    tabCriteria.add(Restrictions.eq(Tab.PROPERTY_WINDOW, window));
    tabCriteria.add(Restrictions.eq(Tab.PROPERTY_NAME, tabName));
    tabCriteria.setMaxResults(1);
    Tab tab = (Tab) tabCriteria.uniqueResult();

    final OBCriteria<TabAccess> tabAccessCriteria = OBDal.getInstance()
        .createCriteria(TabAccess.class);
    tabAccessCriteria
        .add(Restrictions.eq(TabAccess.PROPERTY_WINDOWACCESS, waCriteria.uniqueResult()));
    tabAccessCriteria.add(Restrictions.eq(TabAccess.PROPERTY_TAB, tab));
    tabAccessCriteria.setMaxResults(1);
    TabAccess ta = (TabAccess) tabAccessCriteria.uniqueResult();

    final OBCriteria<Field> fieldCriteria = OBDal.getInstance().createCriteria(Field.class);
    fieldCriteria.add(Restrictions.eq(Field.PROPERTY_TAB, tab));
    fieldCriteria.add(Restrictions.eq(Field.PROPERTY_NAME, fieldName));
    fieldCriteria.setMaxResults(1);

    final OBCriteria<FieldAccess> fieldAccessCriteria = OBDal.getInstance()
        .createCriteria(FieldAccess.class);
    fieldAccessCriteria.add(Restrictions.eq(FieldAccess.PROPERTY_TABACCESS, ta));
    fieldAccessCriteria
        .add(Restrictions.eq(FieldAccess.PROPERTY_FIELD, fieldCriteria.uniqueResult()));
    fieldAccessCriteria.setMaxResults(1);

    FieldAccess fa = (FieldAccess) fieldAccessCriteria.uniqueResult();
    fa.setEditableField(editableField);
    fa.setActive(isActive);
  }

  private static void removeFieldAccesses(Role role, String windowName, String tabName) {
    final OBCriteria<Window> windowCriteria = OBDal.getInstance().createCriteria(Window.class);
    windowCriteria.add(Restrictions.eq(Window.PROPERTY_NAME, windowName));
    windowCriteria.setMaxResults(1);
    Window window = (Window) windowCriteria.uniqueResult();

    final OBCriteria<Tab> tabCriteria = OBDal.getInstance().createCriteria(Tab.class);
    tabCriteria.add(Restrictions.eq(Tab.PROPERTY_WINDOW, window));
    tabCriteria.add(Restrictions.eq(Tab.PROPERTY_NAME, tabName));
    tabCriteria.setMaxResults(1);
    Tab tab = (Tab) tabCriteria.uniqueResult();

    final OBCriteria<WindowAccess> waCriteria = OBDal.getInstance()
        .createCriteria(WindowAccess.class);
    waCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_ROLE, role));
    waCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_WINDOW, window));
    waCriteria.setFilterOnActive(false);
    WindowAccess wa = (WindowAccess) waCriteria.uniqueResult();

    final OBCriteria<TabAccess> taCriteria = OBDal.getInstance().createCriteria(TabAccess.class);
    taCriteria.add(Restrictions.eq(TabAccess.PROPERTY_WINDOWACCESS, wa));
    taCriteria.add(Restrictions.eq(TabAccess.PROPERTY_TAB, tab));
    taCriteria.setFilterOnActive(false);
    TabAccess ta = (TabAccess) taCriteria.uniqueResult();

    List<FieldAccess> fieldAccessToDelete = new ArrayList<FieldAccess>();
    for (FieldAccess fa : ta.getADFieldAccessList()) {
      fieldAccessToDelete.add(fa);
    }
    for (FieldAccess fa : fieldAccessToDelete) {
      ta.getADFieldAccessList().remove(fa);
      OBDal.getInstance().remove(fa);
    }
  }

  private static String[] getFieldAccessInfo(Role role, String windowName, String tabName,
      String fieldName) {
    String result[] = new String[3];
    for (WindowAccess wa : role.getADWindowAccessList()) {
      if (windowName.equals(wa.getWindow().getName())) {
        for (TabAccess ta : wa.getADTabAccessList()) {
          if (tabName.equals(ta.getTab().getName())) {
            for (FieldAccess fa : ta.getADFieldAccessList()) {
              if (fieldName.equals(fa.getField().getName())) {
                result[0] = fa.isEditableField().toString();
                result[1] = fa.isActive().toString();
                result[2] = fa.getInheritedFrom() != null ? fa.getInheritedFrom().getId() : "";
                break;
              }
            }
          }
        }
      }
    }
    return result;
  }

  private static void addReportAndProcessAccess(Role role, String reportName) {
    final org.openbravo.model.ad.access.ProcessAccess processAccess = OBProvider.getInstance()
        .get(org.openbravo.model.ad.access.ProcessAccess.class);
    final OBCriteria<Process> obCriteria = OBDal.getInstance().createCriteria(Process.class);
    obCriteria.add(Restrictions.eq(Process.PROPERTY_NAME, reportName));
    obCriteria.setMaxResults(1);
    processAccess.setClient(role.getClient());
    processAccess.setOrganization(role.getOrganization());
    processAccess.setRole(role);
    processAccess.setProcess((Process) obCriteria.uniqueResult());
    OBDal.getInstance().save(processAccess);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(role);
  }

  private static void updateReportAndProcessAccess(Role role, String reportName, boolean isActive) {
    final OBCriteria<Process> processCriteria = OBDal.getInstance().createCriteria(Process.class);
    processCriteria.add(Restrictions.eq(Window.PROPERTY_NAME, reportName));
    processCriteria.setMaxResults(1);
    final OBCriteria<org.openbravo.model.ad.access.ProcessAccess> processAccessCriteria = OBDal
        .getInstance()
        .createCriteria(org.openbravo.model.ad.access.ProcessAccess.class);
    processAccessCriteria
        .add(Restrictions.eq(org.openbravo.model.ad.access.ProcessAccess.PROPERTY_ROLE, role));
    processAccessCriteria
        .add(Restrictions.eq(org.openbravo.model.ad.access.ProcessAccess.PROPERTY_PROCESS,
            processCriteria.uniqueResult()));
    processAccessCriteria.setMaxResults(1);
    org.openbravo.model.ad.access.ProcessAccess pa = (org.openbravo.model.ad.access.ProcessAccess) processAccessCriteria
        .uniqueResult();
    pa.setActive(isActive);
  }

  private static void removeReportAndProcessAccesses(Role role) {
    final OBCriteria<org.openbravo.model.ad.access.ProcessAccess> obCriteria = OBDal.getInstance()
        .createCriteria(org.openbravo.model.ad.access.ProcessAccess.class);
    obCriteria
        .add(Restrictions.eq(org.openbravo.model.ad.access.ProcessAccess.PROPERTY_ROLE, role));
    obCriteria.setFilterOnActive(false);
    for (org.openbravo.model.ad.access.ProcessAccess pa : obCriteria.list()) {
      role.getADProcessAccessList().remove(pa);
      OBDal.getInstance().remove(pa);
    }
  }

  private static String[] getReportAndProcessAccessInfo(Role role, String reportName) {
    String result[] = new String[3];
    for (org.openbravo.model.ad.access.ProcessAccess pa : role.getADProcessAccessList()) {
      if (reportName.equals(pa.getProcess().getName())) {
        result[0] = "";
        result[1] = pa.isActive().toString();
        result[2] = pa.getInheritedFrom() != null ? pa.getInheritedFrom().getId() : "";
        break;
      }
    }
    return result;
  }

  private static void addFormAccess(Role role, String formName) {
    final FormAccess formAccess = OBProvider.getInstance().get(FormAccess.class);
    final OBCriteria<Form> obCriteria = OBDal.getInstance().createCriteria(Form.class);
    obCriteria.add(Restrictions.eq(Form.PROPERTY_NAME, formName));
    obCriteria.setMaxResults(1);
    formAccess.setClient(role.getClient());
    formAccess.setOrganization(role.getOrganization());
    formAccess.setRole(role);
    formAccess.setSpecialForm((Form) obCriteria.uniqueResult());
    OBDal.getInstance().save(formAccess);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(role);
  }

  private static void updateFormAccess(Role role, String formName, boolean isActive) {
    final OBCriteria<Form> formCriteria = OBDal.getInstance().createCriteria(Form.class);
    formCriteria.add(Restrictions.eq(Form.PROPERTY_NAME, formName));
    formCriteria.setMaxResults(1);
    final OBCriteria<FormAccess> formAccessCriteria = OBDal.getInstance()
        .createCriteria(FormAccess.class);
    formAccessCriteria.add(Restrictions.eq(FormAccess.PROPERTY_ROLE, role));
    formAccessCriteria
        .add(Restrictions.eq(FormAccess.PROPERTY_SPECIALFORM, formCriteria.uniqueResult()));
    formAccessCriteria.setMaxResults(1);
    FormAccess fa = (FormAccess) formAccessCriteria.uniqueResult();
    fa.setActive(isActive);
  }

  private static void removeFormAccesses(Role role) {
    final OBCriteria<FormAccess> obCriteria = OBDal.getInstance().createCriteria(FormAccess.class);
    obCriteria.add(Restrictions.eq(FormAccess.PROPERTY_ROLE, role));
    obCriteria.setFilterOnActive(false);
    for (FormAccess fa : obCriteria.list()) {
      role.getADFormAccessList().remove(fa);
      OBDal.getInstance().remove(fa);
    }
  }

  private static String[] getFormAccessInfo(Role role, String formName) {
    String result[] = new String[3];
    for (FormAccess fa : role.getADFormAccessList()) {
      if (formName.equals(fa.getSpecialForm().getName())) {
        result[0] = "";
        result[1] = fa.isActive().toString();
        result[2] = fa.getInheritedFrom() != null ? fa.getInheritedFrom().getId() : "";
        break;
      }
    }
    return result;
  }

  private static void addWidgetAccess(Role role, String widgetTitle) {
    final WidgetClassAccess widgetAccess = OBProvider.getInstance().get(WidgetClassAccess.class);
    final OBCriteria<WidgetClass> obCriteria = OBDal.getInstance()
        .createCriteria(WidgetClass.class);
    obCriteria.add(Restrictions.eq(WidgetClass.PROPERTY_WIDGETTITLE, widgetTitle));
    obCriteria.setMaxResults(1);
    widgetAccess.setClient(role.getClient());
    widgetAccess.setOrganization(role.getOrganization());
    widgetAccess.setRole(role);
    widgetAccess.setWidgetClass((WidgetClass) obCriteria.uniqueResult());
    OBDal.getInstance().save(widgetAccess);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(role);
  }

  private static void updateWidgetAccess(Role role, String widgetTitle, boolean isActive) {
    final OBCriteria<WidgetClass> widgetCriteria = OBDal.getInstance()
        .createCriteria(WidgetClass.class);
    widgetCriteria.add(Restrictions.eq(WidgetClass.PROPERTY_WIDGETTITLE, widgetTitle));
    widgetCriteria.setMaxResults(1);
    final OBCriteria<WidgetClassAccess> widgetAccessCriteria = OBDal.getInstance()
        .createCriteria(WidgetClassAccess.class);
    widgetAccessCriteria.add(Restrictions.eq(WidgetClassAccess.PROPERTY_ROLE, role));
    widgetAccessCriteria.add(
        Restrictions.eq(WidgetClassAccess.PROPERTY_WIDGETCLASS, widgetCriteria.uniqueResult()));
    widgetAccessCriteria.setMaxResults(1);
    WidgetClassAccess wa = (WidgetClassAccess) widgetAccessCriteria.uniqueResult();
    wa.setActive(isActive);
  }

  private static void removeWidgetClassAccesses(Role role) {
    final OBCriteria<WidgetClassAccess> obCriteria = OBDal.getInstance()
        .createCriteria(WidgetClassAccess.class);
    obCriteria.add(Restrictions.eq(WidgetClassAccess.PROPERTY_ROLE, role));
    obCriteria.setFilterOnActive(false);
    for (WidgetClassAccess wa : obCriteria.list()) {
      role.getOBKMOWidgetClassAccessList().remove(wa);
      OBDal.getInstance().remove(wa);
    }
  }

  private static String[] getWidgetAccessInfo(Role role, String widgetTitle) {
    String result[] = new String[3];
    for (WidgetClassAccess wa : role.getOBKMOWidgetClassAccessList()) {
      if (widgetTitle.equals(wa.getWidgetClass().getWidgetTitle())) {
        result[0] = "";
        result[1] = wa.isActive().toString();
        result[2] = wa.getInheritedFrom() != null ? wa.getInheritedFrom().getId() : "";
        break;
      }
    }
    return result;
  }

  private static void addViewImplementationAccess(Role role, String viewImplementationName) {
    final ViewRoleAccess viewAccess = OBProvider.getInstance().get(ViewRoleAccess.class);
    final OBCriteria<OBUIAPPViewImplementation> obCriteria = OBDal.getInstance()
        .createCriteria(OBUIAPPViewImplementation.class);
    obCriteria
        .add(Restrictions.eq(OBUIAPPViewImplementation.PROPERTY_NAME, viewImplementationName));
    obCriteria.setMaxResults(1);
    viewAccess.setClient(role.getClient());
    viewAccess.setOrganization(role.getOrganization());
    viewAccess.setRole(role);
    viewAccess.setViewImplementation((OBUIAPPViewImplementation) obCriteria.uniqueResult());
    OBDal.getInstance().save(viewAccess);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(role);
  }

  private static void updateViewImplementationAccess(Role role, String viewName, boolean isActive) {
    final OBCriteria<OBUIAPPViewImplementation> viewCriteria = OBDal.getInstance()
        .createCriteria(OBUIAPPViewImplementation.class);
    viewCriteria.add(Restrictions.eq(OBUIAPPViewImplementation.PROPERTY_NAME, viewName));
    viewCriteria.setMaxResults(1);
    final OBCriteria<ViewRoleAccess> viewAccessCriteria = OBDal.getInstance()
        .createCriteria(ViewRoleAccess.class);
    viewAccessCriteria.add(Restrictions.eq(ViewRoleAccess.PROPERTY_ROLE, role));
    viewAccessCriteria.add(
        Restrictions.eq(ViewRoleAccess.PROPERTY_VIEWIMPLEMENTATION, viewCriteria.uniqueResult()));
    viewAccessCriteria.setMaxResults(1);
    ViewRoleAccess va = (ViewRoleAccess) viewAccessCriteria.uniqueResult();
    va.setActive(isActive);
  }

  private static void removeViewImplementationAccesses(Role role) {
    final OBCriteria<ViewRoleAccess> obCriteria = OBDal.getInstance()
        .createCriteria(ViewRoleAccess.class);
    obCriteria.add(Restrictions.eq(ViewRoleAccess.PROPERTY_ROLE, role));
    obCriteria.setFilterOnActive(false);
    for (ViewRoleAccess va : obCriteria.list()) {
      role.getObuiappViewRoleAccessList().remove(va);
      OBDal.getInstance().remove(va);
    }
  }

  private static String[] getViewImplementationAccessInfo(Role role, String viewName) {
    String result[] = new String[3];
    for (ViewRoleAccess va : role.getObuiappViewRoleAccessList()) {
      if (viewName.equals(va.getViewImplementation().getName())) {
        result[0] = "";
        result[1] = va.isActive().toString();
        result[2] = va.getInheritedFrom() != null ? va.getInheritedFrom().getId() : "";
        break;
      }
    }
    return result;
  }

  private static void addProcessDefinitionAccess(Role role, String processName) {
    final org.openbravo.client.application.ProcessAccess processAccess = OBProvider.getInstance()
        .get(org.openbravo.client.application.ProcessAccess.class);
    final OBCriteria<org.openbravo.client.application.Process> obCriteria = OBDal.getInstance()
        .createCriteria(org.openbravo.client.application.Process.class);
    obCriteria
        .add(Restrictions.eq(org.openbravo.client.application.Process.PROPERTY_NAME, processName));
    obCriteria.setMaxResults(1);
    processAccess.setClient(role.getClient());
    processAccess.setOrganization(role.getOrganization());
    processAccess.setRole(role);
    processAccess
        .setObuiappProcess((org.openbravo.client.application.Process) obCriteria.uniqueResult());
    OBDal.getInstance().save(processAccess);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(role);
  }

  private static void updateProcessDefinitionAccess(Role role, String processName,
      boolean isActive) {
    final OBCriteria<org.openbravo.client.application.Process> processCriteria = OBDal.getInstance()
        .createCriteria(org.openbravo.client.application.Process.class);
    processCriteria
        .add(Restrictions.eq(org.openbravo.client.application.Process.PROPERTY_NAME, processName));
    processCriteria.setMaxResults(1);
    final OBCriteria<org.openbravo.client.application.ProcessAccess> processAccessCriteria = OBDal
        .getInstance()
        .createCriteria(org.openbravo.client.application.ProcessAccess.class);
    processAccessCriteria
        .add(Restrictions.eq(org.openbravo.client.application.ProcessAccess.PROPERTY_ROLE, role));
    processAccessCriteria
        .add(Restrictions.eq(org.openbravo.client.application.ProcessAccess.PROPERTY_OBUIAPPPROCESS,
            processCriteria.uniqueResult()));
    processAccessCriteria.setMaxResults(1);
    org.openbravo.client.application.ProcessAccess pa = (org.openbravo.client.application.ProcessAccess) processAccessCriteria
        .uniqueResult();
    pa.setActive(isActive);
  }

  private static void removeProcessDefinitionAccesses(Role role) {
    final OBCriteria<org.openbravo.client.application.ProcessAccess> obCriteria = OBDal
        .getInstance()
        .createCriteria(org.openbravo.client.application.ProcessAccess.class);
    obCriteria
        .add(Restrictions.eq(org.openbravo.client.application.ProcessAccess.PROPERTY_ROLE, role));
    obCriteria.setFilterOnActive(false);
    for (org.openbravo.client.application.ProcessAccess pa : obCriteria.list()) {
      role.getOBUIAPPProcessAccessList().remove(pa);
      OBDal.getInstance().remove(pa);
    }
  }

  private static String[] getProcessDefinitonAccessInfo(Role role, String processName) {
    String result[] = new String[3];
    for (org.openbravo.client.application.ProcessAccess pa : role.getOBUIAPPProcessAccessList()) {
      if (processName.equals(pa.getObuiappProcess().getName())) {
        result[0] = "";
        result[1] = pa.isActive().toString();
        result[2] = pa.getInheritedFrom() != null ? pa.getInheritedFrom().getId() : "";
        break;
      }
    }
    return result;
  }

  private static void addTableAccess(Role role, String tableName, boolean isReadOnly) {
    final TableAccess tableAccess = OBProvider.getInstance().get(TableAccess.class);
    final OBCriteria<Table> obCriteria = OBDal.getInstance().createCriteria(Table.class);
    obCriteria.add(Restrictions.eq(Table.PROPERTY_DBTABLENAME, tableName));
    obCriteria.setMaxResults(1);
    tableAccess.setClient(role.getClient());
    tableAccess.setOrganization(role.getOrganization());
    tableAccess.setRole(role);
    tableAccess.setTable((Table) obCriteria.uniqueResult());
    tableAccess.setReadOnly(isReadOnly);
    OBDal.getInstance().save(tableAccess);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(role);
  }

  private static void updateTableAccess(Role role, String tableName, boolean isReadOnly,
      boolean isActive) {
    final OBCriteria<Table> tableCriteria = OBDal.getInstance().createCriteria(Table.class);
    tableCriteria.add(Restrictions.eq(Table.PROPERTY_DBTABLENAME, tableName));
    tableCriteria.setMaxResults(1);
    final OBCriteria<TableAccess> tableAccessCriteria = OBDal.getInstance()
        .createCriteria(TableAccess.class);
    tableAccessCriteria.add(Restrictions.eq(TableAccess.PROPERTY_ROLE, role));
    tableAccessCriteria
        .add(Restrictions.eq(TableAccess.PROPERTY_TABLE, tableCriteria.uniqueResult()));
    tableAccessCriteria.setMaxResults(1);
    TableAccess ta = (TableAccess) tableAccessCriteria.uniqueResult();
    ta.setReadOnly(isReadOnly);
    ta.setActive(isActive);
  }

  private static String[] getTableAccessInfo(Role role, String tableName) {
    String result[] = new String[3];
    for (TableAccess ta : role.getADTableAccessList()) {
      if (tableName.equals(ta.getTable().getDBTableName())) {
        result[0] = ta.isReadOnly().toString();
        result[1] = ta.isActive().toString();
        result[2] = ta.getInheritedFrom() != null ? ta.getInheritedFrom().getId() : "";
        break;
      }
    }
    return result;
  }

  private static void removeTableAccesses(Role role) {
    final OBCriteria<TableAccess> obCriteria = OBDal.getInstance()
        .createCriteria(TableAccess.class);
    obCriteria.add(Restrictions.eq(TableAccess.PROPERTY_ROLE, role));
    obCriteria.setFilterOnActive(false);
    for (TableAccess ta : obCriteria.list()) {
      role.getADTableAccessList().remove(ta);
      OBDal.getInstance().remove(ta);
    }
  }

  private static void addAlertRecipient(Role role, String alertName) {
    final AlertRecipient alertRecipient = OBProvider.getInstance().get(AlertRecipient.class);
    final OBCriteria<AlertRule> obCriteria = OBDal.getInstance().createCriteria(AlertRule.class);
    obCriteria.add(Restrictions.eq(AlertRule.PROPERTY_NAME, alertName));
    obCriteria.setMaxResults(1);
    alertRecipient.setClient(role.getClient());
    alertRecipient.setOrganization(role.getOrganization());
    alertRecipient.setRole(role);
    alertRecipient.setAlertRule((AlertRule) obCriteria.uniqueResult());
    OBDal.getInstance().save(alertRecipient);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(role);
  }

  private static void updateAlertRecipientAccess(Role role, String alertName, boolean isSendEmail,
      boolean isActive) {
    final OBCriteria<AlertRule> alertCriteria = OBDal.getInstance().createCriteria(AlertRule.class);
    alertCriteria.add(Restrictions.eq(AlertRule.PROPERTY_NAME, alertName));
    alertCriteria.setMaxResults(1);
    final OBCriteria<AlertRecipient> alertRecipientCriteria = OBDal.getInstance()
        .createCriteria(AlertRecipient.class);
    alertRecipientCriteria.add(Restrictions.eq(AlertRecipient.PROPERTY_ROLE, role));
    alertRecipientCriteria
        .add(Restrictions.eq(AlertRecipient.PROPERTY_ALERTRULE, alertCriteria.uniqueResult()));
    alertRecipientCriteria.setMaxResults(1);
    AlertRecipient ar = (AlertRecipient) alertRecipientCriteria.uniqueResult();
    ar.setSendEMail(isSendEmail);
    ar.setActive(isActive);
  }

  private static String[] getAlertRecipientAccessInfo(Role role, String alertName) {
    String result[] = new String[3];
    for (AlertRecipient ar : role.getADAlertRecipientList()) {
      if (alertName.equals(ar.getAlertRule().getName())) {
        result[0] = ar.isSendEMail().toString();
        result[1] = ar.isActive().toString();
        result[2] = ar.getInheritedFrom() != null ? ar.getInheritedFrom().getId() : "";
        break;
      }
    }
    return result;
  }

  private static void removeAlertRecipients(Role role) {
    final OBCriteria<AlertRecipient> obCriteria = OBDal.getInstance()
        .createCriteria(AlertRecipient.class);
    obCriteria.add(Restrictions.eq(AlertRecipient.PROPERTY_ROLE, role));
    obCriteria.setFilterOnActive(false);
    for (AlertRecipient ar : obCriteria.list()) {
      OBDal.getInstance().remove(ar);
    }
    OBDal.getInstance().flush();
  }

  private static void addPreference(Role role, String propertyName, String value) {
    final Preference preference = OBProvider.getInstance().get(Preference.class);
    preference.setClient(role.getClient());
    preference.setOrganization(role.getOrganization());
    preference.setVisibleAtRole(role);
    preference.setPropertyList(true);
    preference.setProperty(propertyName);
    preference.setSearchKey(value);
    OBDal.getInstance().save(preference);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(role);
  }

  private static void updatePreference(Role role, String propertyName, boolean isSelected,
      boolean isActive) {
    final OBCriteria<Preference> preferenceCriteria = OBDal.getInstance()
        .createCriteria(Preference.class);
    preferenceCriteria.add(Restrictions.eq(Preference.PROPERTY_VISIBLEATROLE, role));
    preferenceCriteria.add(Restrictions.eq(Preference.PROPERTY_PROPERTY, propertyName));
    preferenceCriteria.setMaxResults(1);
    Preference p = (Preference) preferenceCriteria.uniqueResult();
    p.setSelected(isSelected);
    p.setActive(isActive);
  }

  private static String[] getPreferenceInfo(Role role, String propertyName) {
    String result[] = new String[3];
    for (Preference pf : role.getADPreferenceVisibleAtRoleList()) {
      if (propertyName.equals(pf.getProperty())) {
        result[0] = pf.isSelected().toString();
        result[1] = pf.isActive().toString();
        result[2] = pf.getInheritedFrom() != null ? pf.getInheritedFrom().getId() : "";
        break;
      }
    }
    return result;
  }

  private static void removePreferences(Role role) {
    final OBCriteria<Preference> obCriteria = OBDal.getInstance().createCriteria(Preference.class);
    obCriteria.add(Restrictions.eq(Preference.PROPERTY_VISIBLEATROLE, role));
    obCriteria.setFilterOnActive(false);
    for (Preference p : obCriteria.list()) {
      OBDal.getInstance().remove(p);
    }
    OBDal.getInstance().flush();
  }

  /**
   * Retrieves an array with permissions information assigned to a role, ordered by name
   * 
   * @param type
   *          A String which represents the type of the access
   * @param role
   *          The role to assign the permission
   * @return an array of Strings with the permissions information
   */
  public static String[] getOrderedAccessNames(String type, Role role) {
    if ("ORGANIZATION".equals(type)) {
      return getOrgsFromOrgAccesses(role);
    } else if ("WINDOW".equals(type)) {
      return getWindowsFromWindowAccesses(role);
    } else if ("TAB".equals(type)) {
      // Get tab accesses for Business Partner window
      return getTabFromTabAccesses(role, "Business Partner");
    } else if ("FIELD".equals(type)) {
      // Get field accesses for Business Partner header tab
      return getFieldFromFieldAccesses(role, "Business Partner", "Business Partner");
    } else if ("REPORT".equals(type)) {
      return getReportsFromReportAccesses(role);
    } else if ("FORM".equals(type)) {
      return getFormsFromFormAccesses(role);
    } else if ("WIDGET".equals(type)) {
      return getWidgetsFromWidgetAccesses(role);
    } else if ("VIEW".equals(type)) {
      return getViewsFromViewAccesses(role);
    } else if ("PROCESS".equals(type)) {
      return getProcessFromProcessAccesses(role);
    } else if ("TABLE".equals(type)) {
      return getTablesFromTableAccesses(role);
    } else if ("ALERT".equals(type)) {
      return getAlertRulesFromAlertRecipients(role);
    } else if ("PREFERENCE".equals(type)) {
      return getPreferences(role);
    }
    return null;
  }

  /**
   * Workaround to be able to test role inheritance of view implementations as core only have one
   * view available
   */
  public static void createDummyView() {
    try {
      OBContext.setAdminMode(false);

      Module module = OBDal.getInstance().get(Module.class, "9BA0836A3CD74EE4AB48753A47211BCC");
      boolean currentState = module.isInDevelopment();

      module.setInDevelopment(true);
      OBDal.getInstance().save(module);

      OBDal.getInstance().flush();

      Client client = OBDal.getInstance().getProxy(Client.class, "0");
      Organization org = OBDal.getInstance().getProxy(Organization.class, "0");
      Module mod = OBDal.getInstance().getProxy(Module.class, "9BA0836A3CD74EE4AB48753A47211BCC");
      OBUIAPPViewImplementation viewImplementation = OBProvider.getInstance()
          .get(OBUIAPPViewImplementation.class);
      viewImplementation.setClient(client);
      viewImplementation.setOrganization(org);
      viewImplementation.setModule(mod);
      viewImplementation.setName(DUMMY_VIEW_IMPL_NAME);
      OBDal.getInstance().save(viewImplementation);

      module.setInDevelopment(currentState);
      OBDal.getInstance().save(module);

      OBDal.getInstance().flush();

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Workaround to be able to test role inheritance of view implementations as core only have one
   * view available
   */
  public static void removeDummyView() {
    try {
      OBContext.setAdminMode(false);

      Module module = OBDal.getInstance().get(Module.class, "9BA0836A3CD74EE4AB48753A47211BCC");
      boolean currentState = module.isInDevelopment();

      module.setInDevelopment(true);
      OBDal.getInstance().save(module);

      OBDal.getInstance().flush();

      StringBuilder delete = new StringBuilder();
      delete.append(" delete from " + OBUIAPPViewImplementation.ENTITY_NAME);
      delete.append(" where " + OBUIAPPViewImplementation.PROPERTY_NAME + " = :name");

      @SuppressWarnings("rawtypes")
      Query query = OBDal.getInstance().getSession().createQuery(delete.toString());
      query.setParameter("name", DUMMY_VIEW_IMPL_NAME);
      query.executeUpdate();

      module.setInDevelopment(currentState);
      OBDal.getInstance().save(module);

      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private static String[] getOrgsFromOrgAccesses(Role role) {
    final OBCriteria<RoleOrganization> obCriteria = OBDal.getInstance()
        .createCriteria(RoleOrganization.class);
    obCriteria.add(Restrictions.eq(RoleOrganization.PROPERTY_ROLE, role));
    obCriteria.addOrderBy(RoleOrganization.PROPERTY_ORGANIZATION + "." + Organization.PROPERTY_NAME,
        true);
    List<RoleOrganization> list = obCriteria.list();
    String[] result = new String[list.size() * 2];
    int i = 0;
    for (RoleOrganization ro : list) {
      result[i] = ro.getOrganization().getName();
      result[i + 1] = ro.getInheritedFrom() != null ? ro.getInheritedFrom().getId() : "";
      i += 2;
    }
    return result;
  }

  private static String[] getWindowsFromWindowAccesses(Role role) {
    final OBCriteria<WindowAccess> obCriteria = OBDal.getInstance()
        .createCriteria(WindowAccess.class);
    obCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_ROLE, role));
    obCriteria.addOrderBy(WindowAccess.PROPERTY_WINDOW + "." + Window.PROPERTY_NAME, true);
    List<WindowAccess> list = obCriteria.list();
    String[] result = new String[list.size() * 2];
    int i = 0;
    for (WindowAccess wa : list) {
      result[i] = wa.getWindow().getName();
      result[i + 1] = wa.getInheritedFrom() != null ? wa.getInheritedFrom().getId() : "";
      i += 2;
    }
    return result;
  }

  private static String[] getTabFromTabAccesses(Role role, String windowName) {
    final OBCriteria<Window> windowCriteria = OBDal.getInstance().createCriteria(Window.class);
    windowCriteria.add(Restrictions.eq(Window.PROPERTY_NAME, windowName));
    windowCriteria.setMaxResults(1);
    Window window = (Window) windowCriteria.uniqueResult();

    final OBCriteria<WindowAccess> obCriteria = OBDal.getInstance()
        .createCriteria(WindowAccess.class);
    obCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_ROLE, role));
    obCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_WINDOW, window));
    obCriteria.setMaxResults(1);

    final OBCriteria<TabAccess> tabCriteria = OBDal.getInstance().createCriteria(TabAccess.class);
    tabCriteria.add(Restrictions.eq(TabAccess.PROPERTY_WINDOWACCESS, obCriteria.uniqueResult()));
    tabCriteria.addOrderBy(TabAccess.PROPERTY_TAB + "." + Tab.PROPERTY_NAME, true);
    List<TabAccess> list = tabCriteria.list();
    String[] result = new String[list.size() * 2];
    int i = 0;
    for (TabAccess ta : list) {
      result[i] = ta.getTab().getName();
      result[i + 1] = ta.getInheritedFrom() != null ? ta.getInheritedFrom().getId() : "";
      i += 2;
    }
    return result;
  }

  private static String[] getFieldFromFieldAccesses(Role role, String windowName, String tabName) {
    final OBCriteria<Window> windowCriteria = OBDal.getInstance().createCriteria(Window.class);
    windowCriteria.add(Restrictions.eq(Window.PROPERTY_NAME, windowName));
    windowCriteria.setMaxResults(1);
    Window window = (Window) windowCriteria.uniqueResult();

    final OBCriteria<Tab> tabCriteria = OBDal.getInstance().createCriteria(Tab.class);
    tabCriteria.add(Restrictions.eq(Tab.PROPERTY_NAME, tabName));
    tabCriteria.add(Restrictions.eq(Tab.PROPERTY_WINDOW, window));
    tabCriteria.setMaxResults(1);
    Tab tab = (Tab) tabCriteria.uniqueResult();

    final OBCriteria<WindowAccess> waCriteria = OBDal.getInstance()
        .createCriteria(WindowAccess.class);
    waCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_ROLE, role));
    waCriteria.add(Restrictions.eq(WindowAccess.PROPERTY_WINDOW, window));
    waCriteria.setMaxResults(1);
    WindowAccess windowAccess = (WindowAccess) waCriteria.uniqueResult();

    final OBCriteria<TabAccess> taCriteria = OBDal.getInstance().createCriteria(TabAccess.class);
    taCriteria.add(Restrictions.eq(TabAccess.PROPERTY_WINDOWACCESS, windowAccess));
    taCriteria.add(Restrictions.eq(TabAccess.PROPERTY_TAB, tab));
    taCriteria.setMaxResults(1);
    TabAccess tabAccess = (TabAccess) taCriteria.uniqueResult();

    final OBCriteria<FieldAccess> faCriteria = OBDal.getInstance()
        .createCriteria(FieldAccess.class);
    faCriteria.add(Restrictions.eq(FieldAccess.PROPERTY_TABACCESS, tabAccess));
    faCriteria.addOrderBy(FieldAccess.PROPERTY_FIELD + "." + Field.PROPERTY_NAME, true);

    List<FieldAccess> list = faCriteria.list();
    String[] result = new String[list.size() * 2];
    int i = 0;
    for (FieldAccess fa : list) {
      result[i] = fa.getField().getName();
      result[i + 1] = fa.getInheritedFrom() != null ? fa.getInheritedFrom().getId() : "";
      i += 2;
    }
    return result;
  }

  private static String[] getReportsFromReportAccesses(Role role) {
    final OBCriteria<org.openbravo.model.ad.access.ProcessAccess> obCriteria = OBDal.getInstance()
        .createCriteria(org.openbravo.model.ad.access.ProcessAccess.class);
    obCriteria
        .add(Restrictions.eq(org.openbravo.model.ad.access.ProcessAccess.PROPERTY_ROLE, role));
    obCriteria.addOrderBy(
        org.openbravo.model.ad.access.ProcessAccess.PROPERTY_PROCESS + "." + Process.PROPERTY_NAME,
        true);
    List<org.openbravo.model.ad.access.ProcessAccess> list = obCriteria.list();
    String[] result = new String[list.size() * 2];
    int i = 0;
    for (org.openbravo.model.ad.access.ProcessAccess pa : list) {
      result[i] = pa.getProcess().getName();
      result[i + 1] = pa.getInheritedFrom() != null ? pa.getInheritedFrom().getId() : "";
      i += 2;
    }
    return result;
  }

  private static String[] getFormsFromFormAccesses(Role role) {
    final OBCriteria<FormAccess> obCriteria = OBDal.getInstance().createCriteria(FormAccess.class);
    obCriteria.add(Restrictions.eq(FormAccess.PROPERTY_ROLE, role));
    obCriteria.addOrderBy(FormAccess.PROPERTY_SPECIALFORM + "." + Form.PROPERTY_NAME, true);
    List<FormAccess> list = obCriteria.list();
    String[] result = new String[list.size() * 2];
    int i = 0;
    for (FormAccess fa : list) {
      result[i] = fa.getSpecialForm().getName();
      result[i + 1] = fa.getInheritedFrom() != null ? fa.getInheritedFrom().getId() : "";
      i += 2;
    }
    return result;
  }

  private static String[] getWidgetsFromWidgetAccesses(Role role) {
    final OBCriteria<WidgetClassAccess> obCriteria = OBDal.getInstance()
        .createCriteria(WidgetClassAccess.class);
    obCriteria.add(Restrictions.eq(WidgetClassAccess.PROPERTY_ROLE, role));
    obCriteria.addOrderBy(
        WidgetClassAccess.PROPERTY_WIDGETCLASS + "." + WidgetClass.PROPERTY_WIDGETTITLE, true);
    List<WidgetClassAccess> list = obCriteria.list();
    String[] result = new String[list.size() * 2];
    int i = 0;
    for (WidgetClassAccess wa : list) {
      result[i] = wa.getWidgetClass().getWidgetTitle();
      result[i + 1] = wa.getInheritedFrom() != null ? wa.getInheritedFrom().getId() : "";
      i += 2;
    }
    return result;
  }

  private static String[] getViewsFromViewAccesses(Role role) {
    final OBCriteria<ViewRoleAccess> obCriteria = OBDal.getInstance()
        .createCriteria(ViewRoleAccess.class);
    obCriteria.add(Restrictions.eq(ViewRoleAccess.PROPERTY_ROLE, role));
    obCriteria.addOrderBy(
        ViewRoleAccess.PROPERTY_VIEWIMPLEMENTATION + "." + OBUIAPPViewImplementation.PROPERTY_NAME,
        true);
    List<ViewRoleAccess> list = obCriteria.list();
    String[] result = new String[list.size() * 2];
    int i = 0;
    for (ViewRoleAccess va : list) {
      result[i] = va.getViewImplementation().getName();
      result[i + 1] = va.getInheritedFrom() != null ? va.getInheritedFrom().getId() : "";
      i += 2;
    }
    return result;
  }

  private static String[] getProcessFromProcessAccesses(Role role) {
    final OBCriteria<org.openbravo.client.application.ProcessAccess> obCriteria = OBDal
        .getInstance()
        .createCriteria(org.openbravo.client.application.ProcessAccess.class);
    obCriteria
        .add(Restrictions.eq(org.openbravo.client.application.ProcessAccess.PROPERTY_ROLE, role));
    obCriteria.addOrderBy(org.openbravo.client.application.ProcessAccess.PROPERTY_OBUIAPPPROCESS
        + "." + org.openbravo.client.application.Process.PROPERTY_NAME, true);
    List<org.openbravo.client.application.ProcessAccess> list = obCriteria.list();
    String[] result = new String[list.size() * 2];
    int i = 0;
    for (org.openbravo.client.application.ProcessAccess pa : list) {
      result[i] = pa.getObuiappProcess().getName();
      result[i + 1] = pa.getInheritedFrom() != null ? pa.getInheritedFrom().getId() : "";
      i += 2;
    }
    return result;
  }

  private static String[] getTablesFromTableAccesses(Role role) {
    final OBCriteria<TableAccess> obCriteria = OBDal.getInstance()
        .createCriteria(TableAccess.class);
    obCriteria.add(Restrictions.eq(TableAccess.PROPERTY_ROLE, role));
    obCriteria.addOrderBy(TableAccess.PROPERTY_TABLE + "." + Table.PROPERTY_DBTABLENAME, true);
    List<TableAccess> list = obCriteria.list();
    String[] result = new String[list.size() * 2];
    int i = 0;
    for (TableAccess ta : list) {
      result[i] = ta.getTable().getDBTableName();
      result[i + 1] = ta.getInheritedFrom() != null ? ta.getInheritedFrom().getId() : "";
      i += 2;
    }
    return result;
  }

  private static String[] getAlertRulesFromAlertRecipients(Role role) {
    final OBCriteria<AlertRecipient> obCriteria = OBDal.getInstance()
        .createCriteria(AlertRecipient.class);
    obCriteria.add(Restrictions.eq(AlertRecipient.PROPERTY_ROLE, role));
    obCriteria.addOrderBy(AlertRecipient.PROPERTY_ALERTRULE + "." + AlertRule.PROPERTY_NAME, true);
    List<AlertRecipient> list = obCriteria.list();
    String[] result = new String[list.size() * 2];
    int i = 0;
    for (AlertRecipient ar : list) {
      result[i] = ar.getAlertRule().getName();
      result[i + 1] = ar.getInheritedFrom() != null ? ar.getInheritedFrom().getId() : "";
      i += 2;
    }
    return result;
  }

  private static String[] getPreferences(Role role) {
    final OBCriteria<Preference> obCriteria = OBDal.getInstance().createCriteria(Preference.class);
    obCriteria.add(Restrictions.eq(Preference.PROPERTY_VISIBLEATROLE, role));
    obCriteria.addOrderBy(Preference.PROPERTY_PROPERTY, true);
    List<Preference> list = obCriteria.list();
    String[] result = new String[list.size() * 2];
    int i = 0;
    for (Preference p : list) {
      result[i] = p.getProperty();
      result[i + 1] = p.getInheritedFrom() != null ? p.getInheritedFrom().getId() : "";
      i += 2;
    }
    return result;
  }
}
