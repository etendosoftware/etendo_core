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
 * All portions are Copyright (C) 2013-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.client.application.MenuManager.MenuEntryType;
import org.openbravo.client.application.MenuManager.MenuOption;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.domain.ModelImplementation;
import org.openbravo.model.ad.domain.ModelImplementationMapping;
import org.openbravo.model.ad.ui.Menu;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.Tree;
import org.openbravo.model.ad.utility.TreeNode;

/**
 * 
 * Caches in memory generic global menus per language and tree to be consumed by {@link MenuManager}
 * 
 * @author alostale
 * 
 */
@ApplicationScoped
public class GlobalMenu {
  private static final Logger log = LogManager.getLogger();

  private Map<String, List<MenuOption>> menuOptionsByLangAndTree = null;

  private long cacheTimeStamp = 0;

  /**
   * Returns the menu for current role's tree and language looking in the application scoped cached
   * ones. If it is not present there, it is generated and cached.
   * 
   */
  List<MenuOption> getMenuOptions(String roleId, String language) {
    long t = System.currentTimeMillis();

    if (menuOptionsByLangAndTree == null) {
      invalidateCache();
    }

    Role role = OBDal.getInstance().get(Role.class, roleId);
    final Tree tree;
    if (role.getPrimaryTreeMenu() != null) {
      tree = role.getPrimaryTreeMenu();
    } else {
      tree = OBDal.getInstance().get(Tree.class, "10");
    }

    String menuKey = language + tree.getId();
    if (menuOptionsByLangAndTree.get(menuKey) == null) {
      menuOptionsByLangAndTree.put(menuKey, createInitialMenuList(tree, language));
      log.debug("Generating menu took " + (System.currentTimeMillis() - t));
      t = System.currentTimeMillis();
    } else {
      log.debug("Using cached menu for tree and language");
    }

    ArrayList<MenuOption> newOptions = new ArrayList<MenuOption>();
    final Map<String, MenuOption> menuOptionsByMenuId = new HashMap<String, MenuOption>();
    for (MenuOption option : menuOptionsByLangAndTree.get(menuKey)) {
      final MenuOption clonedMenuOption = new MenuOption(option);

      if (clonedMenuOption.getMenu() != null) {
        menuOptionsByMenuId.put(clonedMenuOption.getMenu().getId(), clonedMenuOption);
      }
      newOptions.add(clonedMenuOption);
    }

    // Tree is generated in the cloned instances to refer to itself and not to the pristine one
    for (MenuOption menuOption : newOptions) {
      menuOption.setParentMenuOption(menuOptionsByMenuId);
    }

    log.debug("Getting a copy of globally cached menu took {} ms", System.currentTimeMillis() - t);
    return newOptions;
  }

  private List<MenuOption> createInitialMenuList(Tree tree, String language) {
    List<MenuOption> menuOptions = new ArrayList<MenuOption>();
    OBCriteria<TreeNode> treeNodes = OBDal.getInstance().createCriteria(TreeNode.class);
    treeNodes.add(Restrictions.eq(TreeNode.PROPERTY_TREE, tree));
    treeNodes.setFilterOnActive(false);

    // Cache in DAL session all menu entries in a single query, so no need to query one by one
    // afterwards
    final String menuHql = "select m from ADMenu m left join fetch m.aDMenuTrlList where m.module.enabled=true";
    final Query<Menu> menuQry = OBDal.getInstance().getSession().createQuery(menuHql, Menu.class);
    List<Menu> menus = menuQry.list();

    List<TreeNode> nodes = treeNodes.list();

    for (TreeNode treeNode : nodes) {
      boolean addOption = treeNode.isActive();

      if (!addOption) {
        Menu menuEntry = OBDal.getInstance().get(Menu.class, treeNode.getNode());
        if (menuEntry != null) {
          addOption = menuEntry.isSummaryLevel();
        }
      }

      if (addOption) {
        final MenuOption menuOption = new MenuOption();
        menuOption.setTreeNode(treeNode);
        menuOption.setDbId(treeNode.getId());
        Menu menuEntry = OBDal.getInstance().get(Menu.class, treeNode.getNode());
        if (menuEntry != null && !menuEntry.isActive()) {
          menuOption.setVisible(false);
        }
        menuOptions.add(menuOption);
      }
    }

    linkMenus(menus, menuOptions, language);

    // sort them by sequencenumber of the treenode
    Collections.sort(menuOptions, new MenuSequenceComparator());

    return menuOptions;
  }

  /**
   * Each menu option is configured with additional parameters
   * 
   */
  private void linkMenus(List<Menu> menus, List<MenuOption> menuOptions, String language) {
    final Map<String, MenuOption> menuOptionsByNodeId = new HashMap<String, MenuOption>();
    for (MenuOption menuOption : menuOptions) {
      menuOptionsByNodeId.put(menuOption.getTreeNode().getNode(), menuOption);
    }

    for (Menu menu : menus) {
      final MenuOption foundOption = menuOptionsByNodeId.get(menu.getId());
      if (menu.isActive() || menu.isSummaryLevel()) {
        if (foundOption != null) {
          foundOption.setMenu(menu, language);
          if (menu.getURL() != null) {
            foundOption.setType(MenuEntryType.External);
            foundOption.setId(menu.getURL());
          } else if (menu.getObuiappView() != null && menu.getObuiappView().isActive()) {
            foundOption.setType(MenuEntryType.View);
            foundOption.setId(menu.getObuiappView().getName());
            foundOption.setObjectId(menu.getObuiappView().getId());
          } else if (menu.getSpecialForm() != null && menu.getSpecialForm().isActive()) {
            boolean found = false;
            for (ModelImplementation mi : menu.getSpecialForm().getADModelImplementationList()) {
              if (found) {
                break;
              }
              for (ModelImplementationMapping mim : mi.getADModelImplementationMappingList()) {
                if (mim.isDefault()) {
                  found = true;
                  foundOption.setType(MenuEntryType.Form);
                  foundOption.setForm(menu.getSpecialForm());
                  foundOption.setId(mim.getMappingName());
                  foundOption.setObjectId(menu.getSpecialForm().getId());
                  break;
                }
              }
            }
          } else if (menu.getProcess() != null && menu.getProcess().isActive()) {
            Process process = menu.getProcess();
            boolean found = false;

            for (ModelImplementation mi : process.getADModelImplementationList()) {
              if (found) {
                break;
              }
              for (ModelImplementationMapping mim : mi.getADModelImplementationMappingList()) {
                if (mim.isDefault()) {
                  found = true;
                  foundOption.setId(mim.getMappingName());
                  foundOption.setObjectId(process.getId());
                  if (process.getUIPattern().equals("Standard")) {
                    foundOption.setType(MenuEntryType.Process);
                    foundOption.setModal(Utility.isModalProcess(process.getId()));
                  } else if (process.isReport() || process.isJasperReport()) {
                    foundOption.setType(MenuEntryType.Report);
                    foundOption.setReport(true);
                  } else {
                    foundOption.setType(MenuEntryType.ProcessManual);
                  }
                  break;
                }
              }
            }
            if (!found && "P".equals(menu.getAction())) {
              foundOption.setType(MenuEntryType.Process);
              foundOption.setObjectId(process.getId());
              foundOption.setModal(Utility.isModalProcess(process.getId()));
              if (process.isExternalService() != null && process.isExternalService()
                  && "PS".equals(process.getServiceType())) {
                foundOption.setId("/utility/OpenPentaho.html?inpadProcessId=" + process.getId());
              } else if ("S".equals(process.getUIPattern()) && !process.isJasperReport()
                  && process.getProcedure() == null) {
                // see the MenuData.isGenericJavaProcess method
                foundOption.setId("/ad_actionButton/ActionButtonJava_Responser.html");
              } else {
                foundOption.setId("/ad_actionButton/ActionButton_Responser.html");
              }
            }
          } else if (menu.getOBUIAPPProcessDefinition() != null
              && menu.getOBUIAPPProcessDefinition().isActive()) {
            foundOption.setType(MenuEntryType.ProcessDefinition);
            foundOption.setObjectId(menu.getOBUIAPPProcessDefinition().getId());
            if (ApplicationConstants.REPORT_UI_PATTERN
                .equals(menu.getOBUIAPPProcessDefinition().getUIPattern())) {
              foundOption.setReport(true);
            }
          } else if (menu.getWindow() != null && menu.getWindow().isActive()) {
            boolean found = false;
            for (Tab tab : menu.getWindow().getADTabList()) {
              if (tab.getTabLevel() == 0) {
                found = true;
                foundOption.setType(MenuEntryType.Window);
                foundOption.setId(tab.getId());
                foundOption.setObjectId(menu.getWindow().getId());
                foundOption.setTab(tab);
                break;
              }
            }
            if (!found) {
              log.warn("Not found tab with level 0 for window {}", menu.getWindow());
            }
          }
        }
      }
    }
  }

  /**
   * Sorts menu based on its nodes sequence
   * 
   */
  private static class MenuSequenceComparator implements Comparator<MenuOption> {
    @Override
    public int compare(MenuOption o1, MenuOption o2) {
      TreeNode tn1 = o1.getTreeNode();
      TreeNode tn2 = o2.getTreeNode();
      return (int) (tn1.getSequenceNumber() - tn2.getSequenceNumber());
    }
  }

  /**
   * Invalidates menu cache. To be invoked when the menu changes.
   */
  public void invalidateCache() {
    log.debug("Invalidating menu cache");
    menuOptionsByLangAndTree = new HashMap<String, List<MenuOption>>();
    cacheTimeStamp = System.currentTimeMillis();
  }

  /**
   * Whenever the cache is regenerated time stamp is changed to check differences
   * 
   */
  public long getCacheTimeStamp() {
    return cacheTimeStamp;
  }
}
