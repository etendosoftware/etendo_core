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
 * All portions are Copyright (C) 2010-2021 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.FetchMode;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.module.ModuleDependency;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Contains several utility methods used in the kernel.
 * 
 * @author mtaal
 */
public class KernelUtils {
  private static final Logger log = LogManager.getLogger();

  private static KernelUtils instance = new KernelUtils();

  // the static dependency list is used when a cycle is detected
  // in the modules
  private static String[] STATICDEPENDENCYLIST = new String[] { "org.openbravo",
      "org.openbravo.base.weld", "org.openbravo.service.json", "org.openbravo.client.kernel",
      "org.openbravo.userinterface.smartclient", "org.openbravo.service.datasource",
      "org.openbravo.client.application", "org.openbravo.userinterface.selector" };

  private ApplicationDictionaryCachedStructures adcs;

  public static synchronized KernelUtils getInstance() {
    if (instance == null) {
      instance = new KernelUtils();
    }
    return instance;
  }

  /** Note this constructor should not be generally used, use instead {@link #getInstance()} */
  public KernelUtils() {
    if (SessionFactoryController.isRunningInWebContainer()) {
      // allow running outside Web container for tests
      adcs = WeldUtils
          .getInstanceFromStaticBeanManager(ApplicationDictionaryCachedStructures.class);
    }
  }

  public static synchronized void setInstance(KernelUtils instance) {
    KernelUtils.instance = instance;
  }

  private List<Module> sortedModules = null;

  /**
   * @see OBMessageUtils#getI18NMessage(String, String[])
   */
  public String getI18N(String key, String[] params) {
    return OBMessageUtils.getI18NMessage(key, params);
  }

  public Property getPropertyFromColumn(Column column) {
    return getPropertyFromColumn(column, true);
  }

  public Property getPropertyFromColumn(Column column, boolean includeIdColumn) {
    final Entity entity = ModelProvider.getInstance().getEntity(column.getTable().getName());
    final String colId = column.getId();
    // first ignore id columns
    for (Property property : entity.getProperties()) {
      final String propColId = property.getColumnId();
      if (propColId == null) {
        continue;
      }
      if (property.isId() && !includeIdColumn) {
        continue;
      }
      if (propColId.equals(colId)) {
        return property;
      }
    }
    // now try without ignoring id columns
    for (Property property : entity.getProperties()) {
      final String propColId = property.getColumnId();
      if (propColId == null) {
        continue;
      }
      if (propColId.equals(colId)) {
        return property;
      }
    }
    throw new IllegalArgumentException(
        "Column " + column + " does not have a corresponding property in the model");
  }

  /**
   * Creates a javascript string which reports an exception to the client.
   */
  public String createErrorJavaScript(Exception e) {
    log.error(e.getMessage(), e);

    final StringBuilder sb = new StringBuilder();
    if (e instanceof OBUserException) {
      OBUserException ex = (OBUserException) e;
      sb.append("OB.KernelUtilities.handleUserException('"
          + StringEscapeUtils.escapeJavaScript(ex.getMessage()) + "', " + ex.getJavaScriptParams()
          + ");");
    } else {
      sb.append("OB.KernelUtilities.handleSystemException('"
          + StringEscapeUtils.escapeJavaScript(e.getMessage()) + "');");
    }
    return sb.toString();
  }

  public JSONObject createErrorJSON(Exception e) {
    JSONObject error = new JSONObject();
    try {
      error.put("message", e.getMessage());
      if (e instanceof OBUserException) {
        error.put("type", "user");
        error.put("params", ((OBUserException) e).getJavaScriptParams());
      } else {
        error.put("type", "system");
      }
    } catch (JSONException e1) {
      log.error("Error creating json error", e1);
    }
    return error;
  }

  /**
   * Computes parameters to add to a link of a resource. The parameters include the version and
   * language of the user.
   * 
   * @param module
   *          Module to get the version from (if not in developers mode)
   * @return the version parameter string, a concatenation of the version and language with
   *         parameter names
   * @see KernelConstants#RESOURCE_VERSION_PARAMETER
   * @see KernelConstants#RESOURCE_LANGUAGE_PARAMETER
   */
  public String getVersionParameters(Module module) {

    return KernelConstants.RESOURCE_VERSION_PARAMETER + "=" + module.getVersion() + "&"
        + KernelConstants.RESOURCE_LANGUAGE_PARAMETER + "="
        + OBContext.getOBContext().getLanguage().getId();
  }

  /**
   * Returns "true" if the module given its java package exists and "false" if it doesn't.
   * 
   * @param javaPackage
   *          the java package used to read the module
   * @return boolean
   */
  public boolean isModulePresent(String javaPackage) {
    for (Module module : getModulesOrderedByDependency()) {
      // do trim to handle small typing errors, consider to do lowercase also
      if (javaPackage.trim().equals(module.getJavaPackage().trim())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get a module using its java package, the module is read from the internal cache.
   * 
   * @param javaPackage
   *          the java package used to read the module
   * @return a Module
   * @throws OBException
   *           if the module can not be found.
   */
  public Module getModule(String javaPackage) {
    Module chosenModule = null;
    String chosenPackage = null;
    for (Module module : getModulesOrderedByDependency()) {
      // do trim to handle small typing errors, consider to do lowercase also
      if (javaPackage.trim().startsWith(module.getJavaPackage().trim())) {
        // We pick a module if:
        // - Its javapackage is a prefix of the javapackage of the class
        // - We don't have a module yet, or the javapackage is longer than the previously picked
        // module
        // We do this length check, in order to prioritize javapackages which better fit the class.
        // This is to avoid situations in which, for example, org.openbravo is chosen over
        // org.openbravo.client.kernel
        if (chosenModule == null
            || module.getJavaPackage().trim().length() > chosenPackage.length()) {
          chosenModule = module;
          chosenPackage = module.getJavaPackage().trim();
        }
      }
    }
    if (chosenModule == null) {
      throw new OBException("No module found for java package " + javaPackage);
    } else {
      return chosenModule;
    }
  }

  /**
   * Note the result of this method is cached. If module dependencies change then a system restart
   * is required to refresh this cache.
   * 
   * @return the modules in order of their dependencies, so core will be the first module etc.
   */
  public List<Module> getModulesOrderedByDependency() {
    if (sortedModules != null) {
      return sortedModules;
    }

    final List<ModuleWithLowLevelCode> moduleLowLevelCodes = new ArrayList<ModuleWithLowLevelCode>();
    OBContext.setAdminMode();

    try {
      // note, because of the left join fetch on module dependencies
      // a module is returned for each module dependency, take care of this in
      // the for-loop below
      final OBCriteria<Module> modules = OBDal.getInstance().createCriteria(Module.class);
      modules.addOrderBy(Module.PROPERTY_ID, true);
      modules.setFetchMode(Module.PROPERTY_MODULEDEPENDENCYLIST, FetchMode.JOIN);
      final List<Module> handledModules = new ArrayList<Module>();
      try {
        for (Module module : modules.list()) {
          if (handledModules.contains(module)) {
            continue;
          }
          handledModules.add(module);
          final ModuleWithLowLevelCode moduleLowLevelCode = new ModuleWithLowLevelCode();
          moduleLowLevelCode.setModule(module);
          moduleLowLevelCode.setLowLevelCode(computeLowLevelCode(module, new ArrayList<Module>()));
          moduleLowLevelCodes.add(moduleLowLevelCode);
        }
      } catch (ModuleDependencyCycleException e) {
        // use static list...
        moduleLowLevelCodes.clear();
        handledModules.clear();
        for (Module module : modules.list()) {
          if (handledModules.contains(module)) {
            continue;
          }
          handledModules.add(module);
          final ModuleWithLowLevelCode moduleLowLevelCode = new ModuleWithLowLevelCode();
          moduleLowLevelCode.setModule(module);
          int index = 0;
          for (String pkg : STATICDEPENDENCYLIST) {
            if (pkg.equals(module.getJavaPackage())) {
              break;
            }

            index++;
          }
          moduleLowLevelCode.setLowLevelCode(index);
          moduleLowLevelCodes.add(moduleLowLevelCode);
        }
      }
      Collections.sort(moduleLowLevelCodes);
      final List<Module> result = new ArrayList<Module>();
      for (ModuleWithLowLevelCode moduleLowLevelCode : moduleLowLevelCodes) {
        result.add(moduleLowLevelCode.getModule());
      }
      sortedModules = result;
      log.debug("Modules ordered by dependency: " + sortedModules);
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Retrieves all the modules that are part of the dependency tree of the given module. This
   * includes all the modules that depend on the provided module (ancestors) and their ancestors, as
   * well as all the modules the provided module depends on (descendants) and their descendants.
   *
   * @param module
   *          the module to get the dependency tree for
   *
   * @return the full tree of dependencies for the given module
   */
  public List<Module> getDependencyTree(Module module) {
    Set<String> moduleDependencyTree = getDescendantsDependencyTree(module);
    moduleDependencyTree.addAll(getAncestorsDependencyTree(module));

    return KernelUtils.getInstance()
        .getModulesOrderedByDependency()
        .stream()
        .filter(m -> moduleDependencyTree.contains(m.getId()))
        .collect(Collectors.toList());
  }

  private Set<String> getDescendantsDependencyTree(Module module) {
    Set<String> tree = new HashSet<>();
    tree.add(module.getId());
    for (ModuleDependency dep : module.getModuleDependencyList()) {
      Module depMod = dep.getDependentModule();
      if (!tree.contains(depMod.getId())) {
        tree.add(depMod.getId());
        tree.addAll(getDescendantsDependencyTree(depMod));
      }
    }
    return tree;
  }

  private Set<String> getAncestorsDependencyTree(Module module) {
    Set<String> tree = new HashSet<>();

    for (Module m : KernelUtils.getInstance().getModulesOrderedByDependency()) {
      boolean isParent = m.getModuleDependencyList()
          .stream()
          .map(ModuleDependency::getDependentModule)
          .anyMatch(dep -> dep.getId().equals(module.getId()) && !tree.contains(dep.getId()));
      if (isParent) {
        tree.addAll(getAncestorsDependencyTree(m));
      }
    }

    tree.add(module.getId());
    return tree;
  }

  protected int computeLowLevelCode(Module module, List<Module> modules) {
    if (module.getId().equals("0")) {
      return 0;
    }
    // have been here, go away, with a signal number that there is a loop
    // infinite loop
    if (modules.contains(module)) {
      log.error("Cyclic relation in module dependencies of module " + module);
      for (Module moduleCycle : modules) {
        log.error(moduleCycle.getName());
      }
      throw new ModuleDependencyCycleException("Cycle detected in module dependencies with module "
          + module + " check the error log for the cycle");
    }
    modules.add(module);
    int currentLevel = 0;
    for (ModuleDependency dependency : module.getModuleDependencyList()) {
      final int computedLevel = 1 + computeLowLevelCode(dependency.getDependentModule(), modules);
      if (computedLevel > currentLevel) {
        currentLevel = computedLevel;
      }
    }
    modules.remove(module);
    return currentLevel;
  }

  private static class ModuleWithLowLevelCode implements Comparable<ModuleWithLowLevelCode> {
    private Module module;
    private int lowLevelCode;

    @Override
    public int compareTo(ModuleWithLowLevelCode other) {
      return lowLevelCode - other.getLowLevelCode();
    }

    public Module getModule() {
      return module;
    }

    public void setModule(Module module) {
      this.module = module;
    }

    public int getLowLevelCode() {
      return lowLevelCode;
    }

    public void setLowLevelCode(int lowLevelCode) {
      this.lowLevelCode = lowLevelCode;
    }

  }

  /**
   * Returns the parent object for a specified combination of DAL object and tab. For example,
   * returns the corresponding Sales Order header for a Sales Order Line Returns null if there is no
   * parent object.
   * 
   * @param object
   *          The object whose parent should be returned
   * @param tab
   *          The tab the object belongs to
   * @return The BaseOBObject of the parent record
   */
  public BaseOBObject getParentRecord(BaseOBObject object, Tab tab) {
    Entity parentEntity = null;
    Tab parentTab = getParentTab(tab);
    if (parentTab != null) {
      parentEntity = ModelProvider.getInstance().getEntityByTableId(parentTab.getTable().getId());
    }
    Property fkProp = null;
    for (Property property : object.getEntity().getProperties()) {
      if (property.isParent()) {
        if (property.getTargetEntity() != null && parentEntity != null
            && parentEntity == property.getTargetEntity()) {
          fkProp = property;
        }
      }
    }
    if (fkProp != null) {
      return (BaseOBObject) object.get(fkProp.getName());
    } else {
      return null;
    }
  }

  /**
   * Returns the parent tab of a BaseOBObject which belongs to a given tab
   * 
   * @param tab
   *          The tab the object belongs to
   * @return The parent tab of the given tab, <code>null</code> in case the tab is root
   */
  public Tab getParentTab(Tab tab) {
    if (tab.getTabLevel() == 0L) {
      // root level, there is no parent
      return null;
    }

    ConnectionProvider connection = new DalConnectionProvider(false);
    Tab targetTab = null;
    String tabId = null;
    try {
      tabId = KernelUtilsData.getParentTab(connection, tab.getWindow().getId(),
          tab.getTabLevel().toString(), tab.getSequenceNumber().toString());
      if (tabId != null) {
        targetTab = adcs != null ? adcs.getTab(tabId) : OBDal.getInstance().get(Tab.class, tabId);
      }
    } catch (ServletException e) {
      log.error(e.getMessage(), e);
    }
    return targetTab;
  }

  /**
   * Returns the list of subtabs of a given tab
   * 
   * @param tab
   *          The tab whose subtabs are to be retrieved
   * @param onlyFirstLevel
   *          Boolean used to determine whether all the descendent tabs should be returned or only
   *          the next level ones
   * @return The list of subtabs of the given tab, an empty List if the tab has none
   */
  public List<Tab> getTabSubtabs(Tab tab, boolean onlyFirstLevel) {
    ConnectionProvider connection = new DalConnectionProvider(false);
    Long seqno = tab.getSequenceNumber();
    Long tabLevel = tab.getTabLevel();
    String windowId = tab.getWindow().getId();
    KernelUtilsData[] tabIds = null;
    try {
      if (onlyFirstLevel) {
        tabIds = KernelUtilsData.getFirstLevelSubtabs(connection, windowId, seqno.toString(),
            tabLevel.toString());
      } else {
        tabIds = KernelUtilsData.getAllSubtabs(connection, windowId, seqno.toString(),
            tabLevel.toString());
      }
    } catch (ServletException e) {
      log.error(e.getMessage(), e);
    }
    List<Tab> subTabList = new ArrayList<Tab>();
    for (int i = 0; i < tabIds.length; i++) {
      String tabId = tabIds[i].adTabId;
      Tab subTab = OBDal.getInstance().get(Tab.class, tabId);
      subTabList.add(subTab);
    }
    return subTabList;
  }

  /**
   * Calls {@link #getProperty(Entity, Field)} using the entity of the tab of the field.
   */
  public static Property getProperty(org.openbravo.model.ad.ui.Field field) {
    final String tableId = field.getTab().getTable().getId();
    final Entity entity = ModelProvider.getInstance().getEntityByTableId(tableId);
    return KernelUtils.getProperty(entity, field);
  }

  /**
   * Compute the property from the field, assuming that the field is defined on the basis of entity.
   *
   * @return The property associated to this field, <code>null</code> if the field is not associated
   *         to any property or column
   */
  public static Property getProperty(Entity entity, org.openbravo.model.ad.ui.Field field) {
    if (field.getProperty() == null) {
      if (field.getColumn() == null) {
        return null;
      }
      return entity.getPropertyByColumnName(field.getColumn().getDBColumnName());
    }
    return DalUtil.getPropertyFromPath(entity, field.getProperty());
  }

  private class ModuleDependencyCycleException extends OBException {

    private static final long serialVersionUID = 1L;

    public ModuleDependencyCycleException(String message) {
      super(message);
    }

  }

  /**
   * Returns the name of the parent column of the tab
   * 
   * @param tab
   *          The tab the object belongs to
   * @return The parent column of the table in the given tab, <code>null</code> in case there is no
   *         parent tab
   * 
   */
  public String getParentColumnName(Tab tab) {
    String targetColumnName = null;
    Entity parentEntity = null;
    Entity currentEntity = ModelProvider.getInstance().getEntityByTableId(tab.getTable().getId());
    Tab parentTab = getParentTab(tab);
    if (parentTab != null) {
      parentEntity = ModelProvider.getInstance().getEntityByTableId(parentTab.getTable().getId());
    }
    Property fkProp = null;
    for (Property property : currentEntity.getProperties()) {
      if (property.isParent()) {
        if (property.getTargetEntity() != null && parentEntity != null
            && parentEntity == property.getTargetEntity()) {
          fkProp = property;
        }
      }
    }
    if (fkProp != null) {
      targetColumnName = fkProp.getColumnName();
    }
    return targetColumnName;
  }

  /**
   * Returns true if any of the identifier properties of the provided entity is nullable
   * 
   * @param entity
   *          entity whose identifier might be comprised by nullable properties
   * @return true if at least one of the identifier properties of the provided entity is nullable,
   *         false if all of them are mandatory
   */
  public static boolean hasNullableIdentifierProperties(Entity entity) {
    List<Property> identifierProperties = entity.getIdentifierProperties();
    for (Property property : identifierProperties) {
      if (!property.isMandatory()) {
        return true;
      }
    }
    return false;
  }
}
