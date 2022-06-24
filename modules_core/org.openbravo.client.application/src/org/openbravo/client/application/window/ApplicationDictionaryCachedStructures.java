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
 * All portions are Copyright (C) 2011-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.window;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.Parameter;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.ModelImplementation;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.domain.ReferencedTable;
import org.openbravo.model.ad.domain.ReferencedTree;
import org.openbravo.model.ad.domain.ReferencedTreeField;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.ui.AuxiliaryInput;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.userinterface.selector.Selector;
import org.openbravo.userinterface.selector.SelectorField;

/**
 * This class caches some AD structures used by the Form Initialization component. Basically, it
 * caches: AD components (fields, columns, auxiliary inputs) and ComboTableData instances. This
 * caching occurs to obtain better performance in FIC computations. For this cache to be used, the
 * system needs to be on 'production' mode, that is, all the modules need to be not in development
 */
@ApplicationScoped
public class ApplicationDictionaryCachedStructures {
  private static final Logger log = LogManager.getLogger();

  private Map<String, Window> windowMap;
  private Map<String, Tab> tabMap;
  private Map<String, Table> tableMap;
  private Map<String, List<Field>> fieldMap;
  private Map<String, List<Column>> columnMap;
  private Map<String, List<AuxiliaryInput>> auxInputMap;
  private Map<String, ComboTableData> comboTableDataMap;
  private Map<String, List<Parameter>> attMethodMetadataMap;
  private List<String> initializedWindows;
  private Set<String> inDevelopmentModules;

  private boolean useCache;

  private Map<String, Object> tabLocks;
  private Object getTabLock = new Object();
  private Object initializeWindowLock = new Object();
  private Map<String, Object> windowLocks;

  /**
   * Resets cache and sets whether cache should be used.
   *
   * This method is automatically invoked on creation.
   */
  @PostConstruct
  public void init() {
    log.debug("Resetting cache");
    windowMap = new ConcurrentHashMap<>();
    tabMap = new ConcurrentHashMap<>();
    tableMap = new ConcurrentHashMap<>();
    fieldMap = new ConcurrentHashMap<>();
    columnMap = new ConcurrentHashMap<>();
    auxInputMap = new ConcurrentHashMap<>();
    comboTableDataMap = new ConcurrentHashMap<>();
    attMethodMetadataMap = new ConcurrentHashMap<>();
    initializedWindows = new ArrayList<>();
    tabLocks = new ConcurrentHashMap<>();
    windowLocks = new ConcurrentHashMap<>();
    inDevelopmentModules = getModulesInDevelopment();

    // The cache will only be active when there are no modules in development in the system
    useCache = inDevelopmentModules.isEmpty();
    log.info("ADCS initialized, use cache: {}", useCache);
  }

  private Set<String> getModulesInDevelopment() {
    //@formatter:off
    final String query = 
            "select m.id " +
            "  from ADModule m " +
            " where m.inDevelopment=true";
    //@formatter:on
    final Query<String> indevelMods = OBDal.getInstance()
        .getSession()
        .createQuery(query, String.class);
    return new HashSet<>(indevelMods.list());
  }

  /**
   * In case caching is enabled, Tab for tabId is returned from cache if present. If it is not, this
   * tab and all the ones in the same window are initialized and cached.
   * <p>
   * Note as this method is in charge of doing the full initialization, {@link #getWindow(String)}
   * or {@link #getTab(String)} should be invoked before any other getter in this class. Other case,
   * partially initialized object could be cached, being potentially harmful if obtained from
   * another thread and tried to be initialized.
   * 
   * @param tabId
   *          ID of the tab to look for
   * @return Tab for the tabId, from cache if it is enabled
   */
  public Tab getTab(String tabId) {
    log.debug("get tab {}", tabId);
    if (!useCache()) {
      // not using cache, initialize just current tab and go
      return OBDal.getInstance().get(Tab.class, tabId);
    }

    if (tabMap.containsKey(tabId)) {
      log.debug("got tab {} from cache", tabId);
      return tabMap.get(tabId);
    }

    // tab is not cached: lock at method level to acquire a lock to initialize it
    synchronized (getTabLock) {
      // now we can safely check if lock for tab is not set and create it
      if (!tabLocks.containsKey(tabId)) {
        tabLocks.put(tabId, new Object());
      }
    }

    // lock for tab id, so it only gets initialized once
    synchronized (tabLocks.get(tabId)) {
      if (tabMap.containsKey(tabId)) {
        // another thread already cached this tab
        return tabMap.get(tabId);
      }
      Tab tab = OBDal.getInstance().get(Tab.class, tabId);
      initializeWindow(tab.getWindow().getId());
      return tab;
    }
  }

  /**
   * In case caching is enabled, Window for windowId is returned from cache if present. If it is
   * not, this window and its tabs are initialized and cached.
   * <p>
   * Note as this method is in charge of doing the full initialization, {@link #getWindow(String)}
   * or {@link #getTab(String)} should be invoked before any other getter in this class. Other case,
   * partially initialized object could be cached, being potentially harmful if obtained from
   * another thread and tried to be initialized.
   * 
   * @param windowId
   *          ID of the window to look for
   * @return Window for the windowId, from cache if it is enabled
   */
  public Window getWindow(String windowId) {
    if (!useCache()) {
      return OBDal.getInstance().get(Window.class, windowId);
    }

    if (windowMap.containsKey(windowId)) {
      return windowMap.get(windowId);
    }

    initializeWindow(windowId);
    return windowMap.get(windowId);
  }

  /**
   * Initialized all the tabs for a given window
   */
  private void initializeWindow(String windowId) {
    if (!useCache() || initializedWindows.contains(windowId)) {
      return;
    }
    synchronized (initializeWindowLock) {
      if (!windowLocks.containsKey(windowId)) {
        windowLocks.put(windowId, new Object());
      }
    }

    synchronized (windowLocks.get(windowId)) {
      if (initializedWindows.contains(windowId)) {
        return;
      }

      Window window = OBDal.getInstance().get(Window.class, windowId);
      if (window == null) {
        return;
      }

      initializeDALObject(window.getModule());
      for (Tab tab : window.getADTabList()) {
        initializeTab(tab);
      }

      synchronized (initializedWindows) {
        initializedWindows.add(windowId);
        windowMap.put(windowId, window);
      }
    }
  }

  /**
   * Initializes a tab and its related elements (table, fields, columns, auxiliary inputs and table
   * combo data). If cache is enabled, tab is obtained from cache if it is already present and if
   * not, it is put in cache after initialization
   * 
   * @param tab
   */
  private void initializeTab(Tab tab) {
    String tabId = tab.getId();
    initializeDALObject(tab);

    // initialize other elements related with the tab
    getAuxiliarInputList(tab);
    getFieldsOfTab(tab);
    initializeDALObject(tab.getTable());
    initializeColumnsOfTable(tab.getTable().getId());
    initializeProcess(tab.getProcess());
    initializeDALObject(tab.getTableTree());

    if (useCache()) {
      tabMap.put(tabId, tab);
      log.debug("Initialized tab {}", tabId);
    }
  }

  private Table initializeTable(String tableId) {
    Table table = OBDal.getInstance().get(Table.class, tableId);
    initializeDALObject(table);
    initializeDALObject(table.getADColumnList());
    initializeDALObject(table.getObserdsDatasource());
    if (useCache() && !tableMap.containsKey(tableId)) {
      tableMap.put(tableId, table);
    }
    return table;
  }

  public Table getTable(String tableId) {
    if (useCache() && tableMap.containsKey(tableId)) {
      return tableMap.get(tableId);
    }

    return initializeTable(tableId);
  }

  public List<Field> getFieldsOfTab(String tabId) {
    if (useCache() && fieldMap.containsKey(tabId)) {
      return fieldMap.get(tabId);
    }
    Tab tab = getTab(tabId);
    return getFieldsOfTab(tab);
  }

  public List<Field> getFieldsOfTab(Tab tab) {
    String tableId = tab.getTable().getId();
    List<Field> fields = tab.getADFieldList();
    for (Field f : fields) {
      initializeDALObject(f.getFieldGroup());

      if (f.getColumn() == null) {
        continue;
      }
      initializeColumn(f.getColumn());

      // Property fields can link to columns in a different table than tab's one, in this case
      // initialize table
      if (!tableId.equals(f.getColumn().getTable().getId())) {
        initializeDALObject(f.getColumn().getTable());
      }
    }
    if (useCache()) {
      fieldMap.put(tab.getId(), fields);
    }
    return fields;
  }

  public List<Column> initializeColumnsOfTable(String tableId) {
    Table table = initializeTable(tableId);
    List<Column> columns = table.getADColumnList();
    for (Column c : columns) {
      initializeColumn(c);
    }
    if (useCache() && !columnMap.containsKey(tableId)) {
      columnMap.put(tableId, columns);
    }
    return columns;
  }

  public List<Column> getColumnsOfTable(String tableId) {
    if (useCache() && columnMap.get(tableId) != null) {
      return columnMap.get(tableId);
    }

    return initializeColumnsOfTable(tableId);
  }

  private void initializeColumn(Column c) {
    initializeDALObject(c);
    initializeDALObject(c.getValidation());
    if (c.getValidation() != null) {
      initializeDALObject(c.getValidation().getValidationCode());
    }
    if (c.getCallout() != null) {
      initializeDALObject(c.getCallout());
      initializeDALObject(c.getCallout().getADModelImplementationList());
      for (ModelImplementation imp : c.getCallout().getADModelImplementationList()) {
        initializeDALObject(imp);
      }
    }

    if (c.getReference() != null) {
      initializeDALObject(c.getReference());
      initializeReference(c.getReference());
    }
    if (c.getReferenceSearchKey() != null) {
      initializeReference(c.getReferenceSearchKey());
    }

    initializeDALObject(c.getOBUIAPPProcess());
    initializeProcess(c.getProcess());

  }

  private void initializeProcess(Process p) {
    if (p == null) {
      return;
    }
    initializeDALObject(p);
    initializeDALObject(p.getModule());
    initializeDALObject(p.getADModelImplementationList());
    p.getADModelImplementationList()
        .stream()
        //
        .filter(ModelImplementation::isDefault)
        .forEach(m -> initializeDALObject(m.getADModelImplementationMappingList()));

  }

  private void initializeReference(Reference reference) {
    initializeDALObject(reference.getADReferencedTableList());
    for (ReferencedTable t : reference.getADReferencedTableList()) {
      initializeDALObject(t);
      initializeDALObject(t.getDisplayedColumn().getTable());
    }

    initializeDALObject(reference.getOBUISELSelectorList());
    for (Selector s : reference.getOBUISELSelectorList()) {
      initializeDALObject(s);
      SelectorField displayField = s.getDisplayfield();
      initializeDALObject(displayField);
    }

    initializeDALObject(reference.getADReferencedTreeList());
    for (ReferencedTree t : reference.getADReferencedTreeList()) {
      initializeDALObject(t);
      ReferencedTreeField displayField = t.getDisplayfield();
      initializeDALObject(displayField);
      initializeDALObject(t.getTableTreeCategory());
      initializeDALObject(t.getTable());
    }

    initializeDALObject(reference.getADListList());
    for (org.openbravo.model.ad.domain.List list : reference.getADListList()) {
      initializeDALObject(list);
    }
    initializeDALObject(reference.getOBUIAPPRefWindowList());

    for (ReferencedTree refTree : reference.getADReferencedTreeList()) {
      initializeDALObject(refTree);
      for (ReferencedTreeField refTreeField : refTree.getADReferencedTreeFieldList()) {
        initializeDALObject(refTreeField);
      }
    }

    initializeDALObject(reference.getOBCLKERREFMASKList());
  }

  public List<AuxiliaryInput> getAuxiliarInputList(String tabId) {
    if (useCache() && auxInputMap.get(tabId) != null) {
      return auxInputMap.get(tabId);
    }
    Tab tab = getTab(tabId);
    return getAuxiliarInputList(tab);
  }

  private List<AuxiliaryInput> getAuxiliarInputList(Tab tab) {
    initializeDALObject(tab.getADAuxiliaryInputList());
    List<AuxiliaryInput> auxInputs = new ArrayList<AuxiliaryInput>(tab.getADAuxiliaryInputList());
    for (AuxiliaryInput auxIn : auxInputs) {
      initializeDALObject(auxIn);
    }
    if (useCache()) {
      auxInputMap.put(tab.getId(), auxInputs);
    }
    return auxInputs;
  }

  private void initializeDALObject(Object obj) {
    if (obj == null) {
      return;
    }
    synchronized (obj) {
      Hibernate.initialize(obj);
    }
  }

  /**
   * Returns the combo for the given field from cache if present, if not it also gets cached if
   * applicable.
   */
  public ComboTableData getComboTableData(Field field) {
    String comboId = field.getId();
    if (useCache() && comboTableDataMap.get(comboId) != null) {
      return comboTableDataMap.get(comboId);
    }

    ComboTableData comboTableData;
    try {
      comboTableData = ComboTableData.getTableComboDataFor(field);
    } catch (Exception e) {
      throw new OBException("Error while computing combo table data for field " + field, e);
    }

    log.debug("Combo - cacheable: {} id: {}", comboTableData.canBeCached(), comboId);
    if (useCache() && comboTableData.canBeCached()) {
      comboTableDataMap.put(comboId, comboTableData);
    }
    return comboTableData;
  }

  /**
   * Gets the list of parameters associated to an Attachment Method and a Tab. The list is sorted so
   * the fixed parameters are returned first.
   * 
   * @param strAttMethodId
   *          active attachment method id
   * @param strTabId
   *          tab id to take metadata
   * @return List of parameters by attachment method and tab sorted by Fixed and Sequence Number
   *         where fixed parameters are first.
   */
  public List<Parameter> getMethodMetadataParameters(String strAttMethodId, String strTabId) {
    String strMethodTab = strAttMethodId + "-" + strTabId;
    if (useCache() && attMethodMetadataMap.get(strMethodTab) != null) {
      return attMethodMetadataMap.get(strMethodTab);
    }
    //@formatter:off
    String where = "attachmentMethod.id = :attMethod" +
            "   and (tab is null or tab.id = :tab) " +
            " order by case when fixed is true then 1 else 2 end , sequenceNumber";
    //@formatter:on
    final OBQuery<Parameter> qryParams = OBDal.getInstance()
        .createQuery(Parameter.class, where)
        .setNamedParameter("attMethod", strAttMethodId)
        .setNamedParameter("tab", strTabId);
    List<Parameter> metadatas = qryParams.list();
    for (Parameter metadata : metadatas) {
      initializeMetadata(metadata);
    }

    if (useCache()) {
      attMethodMetadataMap.put(strMethodTab, metadatas);
    }
    return metadatas;
  }

  private void initializeMetadata(Parameter metadata) {
    initializeDALObject(metadata);
    if (metadata.getApplicationElement() != null) {
      initializeDALObject(metadata.getApplicationElement().getADElementTrlList());
    }
    initializeDALObject(metadata.getOBUIAPPParameterTrlList());

    if (metadata.getReference() != null) {
      initializeDALObject(metadata.getReference());
      initializeReference(metadata.getReference());
    }
    if (metadata.getReferenceSearchKey() != null) {
      initializeReference(metadata.getReferenceSearchKey());
    }
  }

  /** Can cache be used, AD components are cacheable if there are no modules in development */
  public boolean useCache() {
    return useCache;
  }

  /**
   * @return {@code true} if there are modules in "in development" status. Otherwise, return
   *         {@code false}
   */
  public boolean isInDevelopment() {
    return !this.inDevelopmentModules.isEmpty();
  }

  /**
   * Checks whether a module is "in development" status.
   * 
   * @param moduleId
   *          the ID of the AD_Module to be checked if it is in "in development".
   * 
   * @return {@code true} if the module passed as parameter is in "in development" status.
   *         Otherwise, return {@code false}
   */
  public boolean isInDevelopment(String moduleId) {
    return this.inDevelopmentModules.contains(moduleId);
  }

  /**
   * Marks all modules as not in development and updates the cache status
   */
  public void setNotInDevelopment() {
    setAllModulesAsNotInDevelopment();
    inDevelopmentModules.clear();
    useCache = true;
    log.info("Setting all modules as not In Development");
  }

  private void setAllModulesAsNotInDevelopment() {
    OBDal.getInstance()
        .getSession()
        .createQuery(
            "update " + Module.ENTITY_NAME + " set " + Module.PROPERTY_INDEVELOPMENT + " = false")
        .executeUpdate();
  }

  Collection<String> getCachedWindows() {
    return windowMap.values()
        .stream() //
        .map(Window::toString) //
        .collect(Collectors.toList());
  }
}
