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
 * All portions are Copyright (C) 2010-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.query.Query;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.obps.ActivationKey;
import org.openbravo.erpCommon.obps.ActivationKey.FeatureRestriction;
import org.openbravo.model.ad.ui.Form;
import org.openbravo.model.ad.ui.Menu;
import org.openbravo.model.ad.ui.MenuTrl;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.TreeNode;

/**
 * Configures cached global menu (@see {@link GlobalMenu}) to adapt it to the current session's
 * permissions and caches it in memory for easy consumption by components.
 * 
 * @author mtaal
 */
@RequestScoped
public class MenuManager {
  private static final Logger log = LogManager.getLogger();

  public static enum MenuEntryType {
    Window, Process, ProcessManual, Report, Form, External, Summary, View, ProcessDefinition
  };

  /**
   * Points to globally cached generic menu
   */
  @Inject
  private GlobalMenu globalMenuOptions;

  private MenuOption cachedMenu;
  private List<MenuOption> selectableMenuOptions;
  private String roleId;
  private List<MenuOption> menuOptions;

  public MenuOption getMenu() {
    long t = System.currentTimeMillis();

    // set the current RoleId
    roleId = OBContext.getOBContext().getRole().getId();

    OBContext.setAdminMode();
    try {
      // take from global menus the one for current role and language
      menuOptions = globalMenuOptions.getMenuOptions(roleId,
          OBContext.getOBContext().getLanguage().getId());

      // configure global menu with role permissions
      linkWindows();
      linkProcesses();
      linkForms();
      linkProcessDefinition();
      linkViewDefinition();

      removeInvisibleNodes();
      removeInaccessibleNodes();

      // set the globals
      final MenuOption localCachedRoot = new MenuOption();
      localCachedRoot.setDbId("-1"); // just use any value
      selectableMenuOptions = new ArrayList<MenuOption>();
      for (MenuOption menuOption : menuOptions) {
        if (menuOption.getParentMenuOption() == null) {
          localCachedRoot.getChildren().add(menuOption);
        }
        if (menuOption.getType() != MenuEntryType.Summary) {
          selectableMenuOptions.add(menuOption);
        }
      }

      Collections.sort(selectableMenuOptions, new MenuComparator());

      cachedMenu = localCachedRoot;
    } finally {
      OBContext.restorePreviousMode();
    }

    log.debug("getMenu took {} ms", System.currentTimeMillis() - t);
    return cachedMenu;
  }

  private void linkForms() {
    final String formsHql = "select fa.specialForm.id " + //
        " from ADFormAccess fa " + //
        "where fa.role.id=:roleId" + //
        "  and fa.active = true";

    final Query<String> formsQry = OBDal.getInstance()
        .getSession()
        .createQuery(formsHql, String.class);
    formsQry.setParameter("roleId", OBContext.getOBContext().getRole().getId());

    for (String formId : formsQry.list()) {
      MenuOption option = getMenuOptionByType(MenuEntryType.Form, formId);
      if (option != null) {
        // allow access if not running in a webcontainer as then the config file can not be checked
        boolean hasAccess = !SessionFactoryController.isRunningInWebContainer()
            || ActivationKey.getInstance()
                .hasLicenseAccess("X", formId) == FeatureRestriction.NO_RESTRICTION;
        option.setAccessGranted(hasAccess);
      }
    }
  }

  private void linkProcesses() {
    final String allowedProcessHql = "select pa.process.id " + //
        " from ADProcessAccess pa " + //
        "where pa.role = :role" + //
        "  and pa.active = true";

    final Query<String> allowedProcessQry = OBDal.getInstance()
        .getSession()
        .createQuery(allowedProcessHql, String.class);
    allowedProcessQry.setParameter("role", OBContext.getOBContext().getRole());

    for (String processId : allowedProcessQry.list()) {
      MenuOption option = getMenuOptionByType(MenuEntryType.Process, processId);
      if (option != null) {
        // allow access if not running in a webcontainer as then the config file can not be checked
        boolean hasAccess = !SessionFactoryController.isRunningInWebContainer()
            || ActivationKey.getInstance()
                .hasLicenseAccess("P", processId) == FeatureRestriction.NO_RESTRICTION;
        option.setAccessGranted(hasAccess);
      }
    }
  }

  private void linkProcessDefinition() {
    final String processHql = "select pa.obuiappProcess.id " + //
        " from OBUIAPP_Process_Access pa " + //
        "where pa.role = :role" + //
        "  and pa.active = true ";
    final Query<String> processQry = OBDal.getInstance()
        .getSession()
        .createQuery(processHql, String.class);
    processQry.setParameter("role", OBContext.getOBContext().getRole());

    for (String processId : processQry.list()) {
      MenuOption option = getMenuOptionByType(MenuEntryType.ProcessDefinition, processId);
      if (option != null) {
        option.setAccessGranted(true);
      }
    }
  }

  private void linkViewDefinition() {
    final String processHql = "select va.viewImplementation.id " + //
        " from obuiapp_ViewRoleAccess va " + //
        "where va.role = :role" + //
        "  and va.active = true ";
    final Query<String> processQry = OBDal.getInstance()
        .getSession()
        .createQuery(processHql, String.class);
    processQry.setParameter("role", OBContext.getOBContext().getRole());

    for (String processId : processQry.list()) {
      MenuOption option = getMenuOptionByType(MenuEntryType.View, processId);
      if (option != null) {
        option.setAccessGranted(true);
      }
    }
  }

  private void linkWindows() {
    final String windowsHql = "select wa.window.id, t.id, ta.editableField, wa.editableField" + //
        " from ADWindowAccess wa " + //
        " left join wa.aDTabAccessList as ta " + //
        " left join ta.tab as t with t.tabLevel = 0 " + //
        " where wa.role = :role" + //
        " and wa.active = true " + //
        " and (ta.active = true or ta.active is null) " + //
        " order by wa.id, t.sequenceNumber DESC ";
    final Query<Object[]> windowsQry = OBDal.getInstance()
        .getSession()
        .createQuery(windowsHql, Object[].class);
    windowsQry.setParameter("role", OBContext.getOBContext().getRole());

    for (Object[] row : windowsQry.list()) {
      final String windowId = (String) row[0];
      final String tabId = (String) row[1];
      final Boolean isEditableTab = (Boolean) row[2];
      final Boolean isEditableWindow = (Boolean) row[3];
      MenuOption option = getMenuOptionByType(MenuEntryType.Window, windowId);
      if (option != null) {
        boolean hasAccess = !SessionFactoryController.isRunningInWebContainer()
            || ActivationKey.getInstance()
                .hasLicenseAccess("MW", windowId) == FeatureRestriction.NO_RESTRICTION;
        option.setAccessGranted(hasAccess);

        if (tabId == null) {
          option.setIsReadOnlyForRole(!isEditableWindow);
        } else {
          option.setIsReadOnlyForRole(!isEditableTab);
        }
      }
    }
  }

  private MenuOption getMenuOptionByType(MenuEntryType type, String objectId) {
    for (MenuOption option : menuOptions) {
      if (option.getType() == type && objectId.equals(option.objectId)) {
        return option;
      }

      // Process is special case, there are several types of processes
      if (type == MenuEntryType.Process && objectId.equals(option.objectId)) {
        if (option.getType() == MenuEntryType.Process || option.getType() == MenuEntryType.Report
            || option.getType() == MenuEntryType.ProcessManual) {
          return option;
        }
      }
    }
    return null;
  }

  private void removeInvisibleNodes() {
    final List<MenuOption> toRemove = new ArrayList<MenuOption>();
    for (MenuOption menuOption : menuOptions) {
      if (!menuOption.isVisible()) {
        toRemove.add(menuOption);
      }
    }
    for (MenuOption menuOption : toRemove) {
      if (menuOption.getParentMenuOption() != null) {
        menuOption.getParentMenuOption().getChildren().remove(menuOption);
      }
    }
    menuOptions.removeAll(toRemove);
  }

  private void removeInaccessibleNodes() {
    final List<MenuOption> toRemove = new ArrayList<MenuOption>();
    for (MenuOption menuOption : menuOptions) {
      if (!menuOption.isAccessible()) {
        toRemove.add(menuOption);
      }
    }
    for (MenuOption menuOption : toRemove) {
      if (menuOption.getParentMenuOption() != null) {
        menuOption.getParentMenuOption().getChildren().remove(menuOption);
      }
    }
    menuOptions.removeAll(toRemove);
  }

  public static class MenuOption implements Serializable {
    private static final long serialVersionUID = 1L;
    private TreeNode treeNode;
    private String label;
    private MenuEntryType type = MenuEntryType.Summary;
    private String id;
    private String dbId;
    private Menu menu;
    private Tab tab;
    private Form form;
    private boolean isReport;
    private MenuOption parentMenuOption;
    private List<MenuOption> children = new ArrayList<MenuOption>();
    private Boolean visible = null;
    private String objectId;
    private boolean isModal;

    private boolean accessGranted = false;
    private Boolean isReadOnlyForRole = false;

    public MenuOption() {
      // Default constructor, just sets all the defaults
    }

    public MenuOption(MenuOption option) {
      this();
      this.treeNode = option.treeNode;
      this.label = option.label;
      this.type = option.type;
      this.id = option.id;
      this.dbId = option.dbId;
      this.menu = option.menu;
      this.tab = option.tab;
      this.form = option.form;
      this.isReport = option.isReport;
      this.objectId = option.objectId;
      this.isModal = option.isModal;
    }

    public boolean isSingleRecord() {
      return getTab() != null && getTab().getUIPattern().equals("SR");
    }

    public String getSingleRecordStringValue() {
      return Boolean.toString(isSingleRecord());
    }

    /**
     * This method returns true if the menu entry is read only. The menu entry will be read only
     * either if the uipattern of its tab is "RO" (Read Only) or if the current role only has read
     * only access to the tab or to its window
     */
    public boolean isReadOnly() {
      boolean tabIsReadOnlyForAll = getTab() != null && getTab().getUIPattern().equals("RO");
      boolean tabIsReadOnlyForRole = tabIsReadOnlyForAll || isTabReadOnlyforRole();
      return tabIsReadOnlyForAll || tabIsReadOnlyForRole;
    }

    /**
     * Returns true if the tab is not editable by the current role by checking the WindowAccess and
     * TabAccess entities
     */
    public boolean isTabReadOnlyforRole() {

      // If there is no tab there is nothing to check
      if (getTab() == null) {
        return false;
      } else {
        return this.getIsReadOnlyForRole();
      }
    }

    public String getReadOnlyStringValue() {
      return Boolean.toString(isReadOnly());
    }

    private Boolean getIsReadOnlyForRole() {
      return isReadOnlyForRole;
    }

    public boolean isEditOrDeleteOnly() {
      return getTab() != null && getTab().getUIPattern().equals("ED");
    }

    public String getEditOrDeleteOnlyStringValue() {
      return Boolean.toString(isEditOrDeleteOnly());
    }

    public boolean isReport() {
      return isReport;
    }

    public void setReport(boolean isReport) {
      this.isReport = isReport;
    }

    public boolean isVisible() {
      if (visible != null) {
        if (!visible && type == MenuEntryType.Summary) {
          for (MenuOption menuOption : children) {
            menuOption.visible = false;
          }
        }
        return visible;
      }

      if (menu == null) {
        visible = false;
      } else if (!children.isEmpty()) {
        boolean localVisible = false;
        for (MenuOption menuOption : children) {
          localVisible |= menuOption.isVisible();
        }
        visible = localVisible;
      } else if (type == MenuEntryType.Summary) {
        visible = false;
      } else if (type == MenuEntryType.External) {
        visible = true;
      } else {
        visible = accessGranted;
      }
      return visible;
    }

    public boolean isAccessible() {
      // In order to be accessible, all its menu entry parents must be active;
      if (parentMenuOption == null) {
        return treeNode.isActive();
      } else {
        return treeNode.isActive() && parentMenuOption.isAccessible();
      }
    }

    public void setVisible(Boolean visible) {
      this.visible = visible;
    }

    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }

    public MenuEntryType getType() {
      return type;
    }

    public void setType(MenuEntryType type) {
      if (id != null && id.toLowerCase().startsWith("http")) {
        this.type = MenuEntryType.External;
      } else {
        this.type = type;
      }
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      if (id.toLowerCase().startsWith("http")) {
        setType(MenuEntryType.External);
      }
      this.id = id;
    }

    public List<MenuOption> getChildren() {
      return children;
    }

    public void setChildren(List<MenuOption> children) {
      this.children = children;
    }

    public TreeNode getTreeNode() {
      return treeNode;
    }

    public void setTreeNode(TreeNode treeNode) {
      this.treeNode = treeNode;
    }

    public MenuOption getParentMenuOption() {
      return parentMenuOption;
    }

    public String getFormId() {
      return form.getId();
    }

    public void setForm(Form form) {
      this.form = form;
    }

    public void setParentMenuOption(MenuOption parentMenuOption) {
      this.parentMenuOption = parentMenuOption;
    }

    public void setParentMenuOption(Map<String, MenuOption> menuOptionsByMenuId) {
      if (treeNode.getReportSet() != null) {
        parentMenuOption = menuOptionsByMenuId.get(treeNode.getReportSet());
        if (parentMenuOption != null) {
          parentMenuOption.getChildren().add(this);
        }
      }
    }

    public Menu getMenu() {
      return menu;
    }

    public void setMenu(Menu menu, String userLanguageId) {
      this.menu = menu;
      for (MenuTrl menuTrl : menu.getADMenuTrlList()) {
        final String trlLanguageId = menuTrl.getLanguage().getId();
        if (trlLanguageId.equals(userLanguageId)) {
          setLabel(menuTrl.getName());
        }
      }
      if (getLabel() == null) {
        setLabel(menu.getName());
      }

      // initialize some collections
      Hibernate.initialize(menu.getADMenuTrlList());
      Hibernate.initialize(menu.getOBUIAPPMenuParametersList());
    }

    public boolean isWindow() {
      return getType().equals(MenuEntryType.Window);
    }

    public boolean isProcess() {
      return getType().equals(MenuEntryType.Process);
    }

    public boolean isModal() {
      return isModal;
    }

    public void setModal(boolean isModal) {
      this.isModal = isModal;
    }

    public boolean isProcessManual() {
      return getType().equals(MenuEntryType.ProcessManual);
    }

    public boolean isView() {
      return getType().equals(MenuEntryType.View);
    }

    public boolean isForm() {
      return getType().equals(MenuEntryType.Form);
    }

    public boolean isProcessDefinition() {
      return getType().equals(MenuEntryType.ProcessDefinition);
    }

    public boolean isExternal() {
      return getType().equals(MenuEntryType.External);
    }

    public Tab getTab() {
      return tab;
    }

    public void setTab(Tab tab) {
      this.tab = tab;
    }

    public List<MenuParameter> getParameters() {
      final List<MenuParameter> parameters = new ArrayList<MenuParameter>();
      for (MenuParameter menuParameter : getMenu().getOBUIAPPMenuParametersList()) {
        if (menuParameter.isActive() && menuParameter.getParameterValue() != null) {
          parameters.add(menuParameter);
        }
      }
      return parameters;
    }

    public String getDbId() {
      return dbId;
    }

    public void setDbId(String dbId) {
      this.dbId = dbId;
    }

    public void setAccessGranted(boolean accessGranted) {
      this.accessGranted = accessGranted;
    }

    public void setObjectId(String objectId) {
      this.objectId = objectId;
    }

    public void setIsReadOnlyForRole(Boolean isReadOnlyForRole) {
      this.isReadOnlyForRole = isReadOnlyForRole;
    }
  }

  private static class MenuComparator implements Comparator<MenuOption> {

    @Override
    public int compare(MenuOption o1, MenuOption o2) {
      return o1.getLabel().compareTo(o2.getLabel());
    }
  }

  public List<MenuOption> getSelectableMenuOptions() {
    // initialize
    getMenu();

    return selectableMenuOptions;
  }

  /**
   * Sets globalMenuOptions. This method is intended to be used only when running out of a context;
   * 
   * @param globalMenuOptions
   */
  public void setGlobalMenuOptions(GlobalMenu globalMenuOptions) {
    this.globalMenuOptions = globalMenuOptions;
  }
}
