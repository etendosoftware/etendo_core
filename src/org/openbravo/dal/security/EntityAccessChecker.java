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
 * All portions are Copyright (C) 2008-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.dal.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.etendoerp.redis.interfaces.CachedList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.Table;
import org.openbravo.base.provider.OBNotSingleton;
import org.openbravo.client.application.Process;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.TableAccess;

/**
 * This class is responsible for determining the allowed read/write access for a combination of user
 * and Entity. It uses the window-role access information and the window-table relation to determine
 * which tables are readable and writable for a user. If the user has readWrite access to a Window
 * then also the related Table/Entity is writable.
 * <p>
 * In addition this class implements the concept of derived readable. Any entity refered to from a
 * readable/writable entity is a derived readable. A user may read (but not write) the following
 * properties from a deriver readable entity: id and identifier properties. Access to any other
 * property or changing a property on a derived readable entity results in a OBSecurityException.
 * Derived readable checks are done when a value is retrieved of an object (@see
 * BaseOBObject#get(String)).
 * <p>
 * This class is used from the {@link SecurityChecker} which combines all entity security checks.
 * 
 * @see Entity
 * @see Property
 * @see SecurityChecker
 * @author mtaal
 */

public class EntityAccessChecker implements OBNotSingleton {
  private static final Logger log = LogManager.getLogger();

  /**
   * Caches entities of the selectors with Search parent reference. Elements store
   * {@code [sel.table.ID | col.table.ID]}.
   */
  private static List<Object[]> targetTablesIds;

  /**
   * Caches processes linked with OBUISEL_Selector references. Elements store
   * {@code [sel.processDefinition.ID | col.table.ID]}.
   */
  private static List<Object[]> processAccessSelectors;

  /**
   * Caches processes. Every array contain the following data: {@code [process.ID | col.table.ID]}.
   */
  private static List<Object[]> processAccessButtons;

  /**
   * Caches entities of the processes defined in Window references. Elements store
   * {@code [tab.table.ID | parameter.process.ID | tab.ID]}.
   */
  private static List<Object[]> parameterOfWindowProcessReference;

  /**
   * Caches entities of the processes defined in OBUISEL_Selector and OBUISEL_Multi Selector
   * references. Elements store {@code [sel.table.ID | parameter.process.ID]}.
   */
  private static List<Object[]> parameterOfSelectorProcessReference;

  /**
   * Caches entities of all the selector references. These selector references should be filter
   * later by all the tabs of the window references. Elements store
   * {@code [field.tab.table.ID | field.tab.ID]}.
   */
  private static List<Object[]> selectorsFromWindowReferences;

  private static final String SELECTOR_REFERENCE = "95E2A8B50A254B2AAE6774B8C2F28120";
  private static final String MULTI_SELECTOR_REFERENCE = "87E6CFF8F71548AFA33F181C317970B5";
  private static final String SEARCH_REFERENCE = "30";
  private static final String WINDOW_REFERENCE = "FF80818132D8F0F30132D9BC395D0038";

  private static final int TABLE_ID = 1;
  private static final int SELECTED_TABLE_ID = 0;
  private static final int SELECTED_ID = 0;
  private static final int TAB_ID = 2;
  private static final int FILTER_ELEMENT_ID = 1;

  // Table Access Level:
  // "6";"System/Client"
  // "1";"Organization"
  // "3";"Client/Organization"
  // "4";"System only"
  // "7";"All"

  // User level:
  // "S";"System"
  // " C";"Client"
  // " O";"Organization"
  // " CO";"Client+Organization"

  private String roleId;
  private Set<String> tabsWithSelectors;
  private Set<Entity> writableEntities = new HashSet<Entity>();

  private Set<Entity> readableEntities = new HashSet<Entity>();
  // the derived readable entities only contains the entities which are
  // derived
  // readable
  // the completely readable entities are present in the readableEntities
  private Set<Entity> derivedReadableEntities = new HashSet<Entity>();
  // the derived entities from process only contains the entities which are
  // derived from process definition
  private Set<Entity> derivedEntitiesFromProcess = new HashSet<Entity>();
  private Set<String> processes = new HashSet<String>();
  private Set<Entity> nonReadableEntities = new HashSet<Entity>();
  private boolean isInitialized = false;

  private OBContext obContext;

  static {
    calculateCachedElements();
  }

  /**
   * This method calculates all the information that could be cached in order to improve performance
   * in entityAccessChecker process.
   * 
   * The static block only gets called once, when the class itself is initialized.
   */
  public static void calculateCachedElements() {
    targetTablesIds = new CachedList<>("EntityAccessChecker:targetTablesIds");
    processAccessSelectors = new CachedList<>("EntityAccessChecker:processAccessSelectors");
    processAccessButtons = new CachedList<>("EntityAccessChecker:processAccessButtons");
    parameterOfWindowProcessReference = new CachedList<>("EntityAccessChecker:parameterOfWindowProcessReference");
    parameterOfSelectorProcessReference = new CachedList<>("EntityAccessChecker:parameterOfSelectorProcessReference");
    selectorsFromWindowReferences = new CachedList<>("EntityAccessChecker:selectorsFromWindowReferences");

    // @formatter:off
    String hqlQry = "select distinct(s.table.id), c.table.id"
        + " from OBUISEL_Selector s"
        + "  left join s.reference r"
        + "  left join r.aDColumnReferenceSearchKeyList c "
        + " where r.parentReference.id='" + SEARCH_REFERENCE + "'";
    targetTablesIds.addAll(SessionHandler.getInstance().createQuery(hqlQry, Object[].class).list());
    
    hqlQry = "select p.id, c.table.id"
        + " from ADColumn c"
        + "  inner join c.table t"
        + "  inner join c.referenceSearchKey r"
        + "  inner join r.oBUISELSelectorList s"
        + "  inner join s.processDefintion p"
        + " where r.parentReference.id='" + SELECTOR_REFERENCE + "'";
    processAccessSelectors.addAll(SessionHandler.getInstance()
        .createQuery(hqlQry, Object[].class)
        .list());

    hqlQry = "select p.id, c.table.id"
        + " from ADColumn c"
        + " inner join c.table t"
        + " inner join c.oBUIAPPProcess p";
    processAccessButtons.addAll(SessionHandler.getInstance().createQuery(hqlQry, Object[].class).list());

    hqlQry = "select t.table.id, p.obuiappProcess.id, t.id"
        + " from OBUIAPP_Parameter p"
        + "  inner join p.referenceSearchKey r"
        + "  inner join r.oBUIAPPRefWindowList rw"
        + "  inner join rw.window w"
        + "  inner join w.aDTabList t"
        + " where p.reference.id in ('" + WINDOW_REFERENCE + "')";
    parameterOfWindowProcessReference.addAll(SessionHandler.getInstance()
        .createQuery(hqlQry, Object[].class)
        .list());

    hqlQry = "select s.table.id, p.obuiappProcess.id"
        + " from OBUIAPP_Parameter p"
        + "  inner join p.referenceSearchKey r"
        + "  inner join r.oBUISELSelectorList s"
        + " where p.reference.id in('" + SELECTOR_REFERENCE + "','" + MULTI_SELECTOR_REFERENCE + "')";
    parameterOfSelectorProcessReference.addAll(SessionHandler.getInstance()
        .createQuery(hqlQry, Object[].class)
        .list());

    hqlQry = "select f.tab.table.id, f.tab.id"
        + " from ADField f"
        + "  inner join f.column c"
        + "  inner join c.referenceSearchKey r"
        + " where r.parentReference.id='" + SELECTOR_REFERENCE + "'";
    selectorsFromWindowReferences.addAll(SessionHandler.getInstance()
        .createQuery(hqlQry, Object[].class)
        .list());
    // @formatter:on
  }

  /**
   * Reads the windows from the database using the current role of the user. Then it iterates
   * through the windows and tabs to determine which entities are readable/writable for that user.
   * In addition non-readable and derived-readable entities are computed. Besides derived entities
   * from process definition are being computed too.
   * 
   * @see ModelProvider
   */
  public synchronized void initialize() {
    OBContext.setAdminMode();
    try {
      final ModelProvider mp = ModelProvider.getInstance();
      final String userLevel = obContext.getUserLevel();

      // Don't use dal because otherwise we can end up in infinite loops
      // there is always only one windowaccess per role due to unique constraints
      // @formatter:off
      final String qryStr = "select t.table.id, wa.editableField"
          + " from ADTab t"
          + "  left join t.window w"
          + "  left join w.aDWindowAccessList wa"
          + " where wa.role.id= :roleId";
      // @formatter:on
      final Query<Object[]> qry = SessionHandler.getInstance()
          .createQuery(qryStr, Object[].class)
          .setParameter("roleId", getRoleId());
      final List<Object[]> tabData = qry.list();

      for (final Object[] os : tabData) {
        final String tableId = (String) os[0];
        final Entity e = mp.getEntityByTableId(tableId);
        if (e == null) { // happens for AD_Client_Info and views
          continue;
        }

        final int accessLevel = e.getAccessLevel().getDbValue();
        if (!hasCorrectAccessLevel(userLevel, accessLevel)) {
          continue;
        }

        final boolean writeAccess = (Boolean) os[1];
        if (writeAccess) {
          writableEntities.add(e);
          readableEntities.add(e);
        } else {
          readableEntities.add(e);
        }
      }

      // and take into account table access
      // @formatter:off
      final String tafQryStr = "select ta"
          + " from ADTableAccess ta"
          + " where role.id= :roleId";
      // @formatter:on
      Query<TableAccess> tafQry = SessionHandler.getInstance()
          .createQuery(tafQryStr, TableAccess.class)
          .setParameter("roleId", getRoleId());

      final List<TableAccess> tas = tafQry.list();
      for (final TableAccess ta : tas) {
        final String tableName = ta.getTable().getName();
        final Entity e = mp.getEntity(tableName);

        if (ta.isExclude()) {
          readableEntities.remove(e);
          writableEntities.remove(e);
          nonReadableEntities.add(e);
        } else if (ta.isReadOnly()) {
          writableEntities.remove(e);
          readableEntities.add(e);
          nonReadableEntities.remove(e);
        } else {
          if (!writableEntities.contains(e)) {
            writableEntities.add(e);
          }
          if (!readableEntities.contains(e)) {
            readableEntities.add(e);
          }
          nonReadableEntities.remove(e);
        }
      }

      // and compute the derived readable
      for (final Entity e : new ArrayList<Entity>(readableEntities)) {
        for (final Property p : e.getProperties()) {
          if (p.getTargetEntity() != null && !readableEntities.contains(p.getTargetEntity())) {
            derivedReadableEntities.add(p.getTargetEntity());
            addDerivedReadableIdentifierProperties(p.getTargetEntity());
          }
        }
      }

      // and take into account derived entities from process definition
      // union of writableEntities and readableEntities
      List<Entity> processEntities = new ArrayList<>(writableEntities);
      for (final Entity readableEntity : readableEntities) {
        if (!processEntities.contains(readableEntity)) {
          processEntities.add(readableEntity);
        }
      }
      if (!processEntities.isEmpty()) {
        Set<String> processTables = new HashSet<>();
        for (final Entity entity : processEntities) {
          Table table = mp.getTableWithoutCheck(entity.getTableName());
          if (table == null) {
            continue;
          }
          processTables.add(table.getId());
        }

        // take into account processes
        processes.addAll(getProcessAccessButtons(processTables));

        // take into account processes linked with selectors
        processes.addAll(getProcessAccessSelectors(processTables));

        // and take into account entities of the selectors with Search parent reference
        for (String tableId : getTargetTablesIds(processTables)) {
          Entity targetSelectorEntity = ModelProvider.getInstance().getEntityByTableId(tableId);
          if (!writableEntities.contains(targetSelectorEntity)
              && !readableEntities.contains(targetSelectorEntity)
              && !nonReadableEntities.contains(targetSelectorEntity)) {
            derivedReadableEntities.add(targetSelectorEntity);
          }
        }
      }

      // and take into account explicit process access
      // @formatter:off
      final String processAccessQryStr = "select p.obuiappProcess.id"
          + " from OBUIAPP_Process_Access p"
          + " where p.role.id= :roleId";
      // @formatter:on
      Query<String> processAccessQry = SessionHandler.getInstance()
          .createQuery(processAccessQryStr, String.class)
          .setParameter("roleId", getRoleId());

      final List<String> processAccessQuery = processAccessQry.list();
      for (final String processAccess : processAccessQuery) {
        processes.add(processAccess);
      }

      addEntitiesFromProcesses();
      isInitialized = true;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private Set<String> getProcessAccessSelectors(Set<String> processTables) {
    return getProcessAccess(processTables, processAccessSelectors);
  }

  private Set<String> getProcessAccessButtons(Set<String> processTables) {
    return getProcessAccess(processTables, processAccessButtons);
  }

  private Set<String> getTargetTablesIds(Set<String> processTables) {
    return getProcessAccess(processTables, targetTablesIds);
  }

  private Set<String> getProcessAccess(Set<String> processTables,
      List<Object[]> targetProcessAccess) {
    Set<String> targetProcesses = new HashSet<>();
    for (Object[] pa : targetProcessAccess) {
      if (processTables.contains(pa[TABLE_ID])) {
        targetProcesses.add((String) pa[SELECTED_ID]);
      }
    }
    return targetProcesses;
  }

  /**
   * Checks if a certain user access level and a certain data access level match. Meaning that with
   * a certain user access level it is allowed to view something with a certain data access level.
   * 
   * @param userLevel
   *          the user level as defined in the role of the user
   * @param accessLevel
   *          the data access level defined in the table
   * @return true if access is allowed, false otherwise
   */
  public static boolean hasCorrectAccessLevel(String userLevel, int accessLevel) {
    // copied from HttpSecureAppServlet.
    if (!OBContext.getOBContext().doAccessLevelCheck()) {
      return true;
    }

    if (accessLevel == 4 && userLevel.indexOf("S") == -1) {
      return false;
    } else if (accessLevel == 1 && userLevel.indexOf("O") == -1) {
      return false;
    } else if (accessLevel == 3
        && (!(userLevel.indexOf("C") != -1 || userLevel.indexOf("O") != -1))) {
      return false;
    } else if (accessLevel == 6
        && (!(userLevel.indexOf("S") != -1 || userLevel.indexOf("C") != -1))) {
      return false;
    }
    return true;
  }

  /**
   * Dumps the readable, writable, derived readable entities. For debugging purposes.
   */
  public void dump() {
    log.info("");
    log.info(">>> Readable entities: ");
    log.info("");
    dumpSorted(readableEntities);

    log.info("");
    log.info(">>> Derived Readable entities: ");
    log.info("");
    dumpSorted(derivedReadableEntities);

    log.info("");
    log.info(">>> Derived entities from process: ");
    log.info("");
    dumpSorted(derivedEntitiesFromProcess);

    log.info("");
    log.info(">>> Writable entities: ");
    log.info("");
    dumpSorted(writableEntities);
    log.info("");
    log.info("");

    final Set<Entity> readableNotWritable = new HashSet<Entity>(readableEntities);
    readableNotWritable.removeAll(writableEntities);

    log.info("");
    log.info(">>> Readable Not-Writable entities: ");
    log.info("");
    dumpSorted(readableNotWritable);
    log.info("");
    log.info("");

    log.info("");
    log.info(">>> Processes accessible: ");
    log.info("");
    dumpSortedProcess(processes);
    log.info("");
    log.info("");
    log.info(">>> Entities granted: " + (processes.size() + readableNotWritable.size()
        + writableEntities.size() + derivedEntitiesFromProcess.size()
        + derivedReadableEntities.size() + readableEntities.size()));

  }

  private void dumpSorted(Set<Entity> set) {
    final List<String> names = new ArrayList<String>();
    for (final Entity e : set) {
      names.add(e.getName());
    }
    Collections.sort(names);
    for (final String n : names) {
      log.info(n);
    }
  }

  private void dumpSortedProcess(Set<String> set) {
    final List<String> names = new ArrayList<String>();
    OBContext.setAdminMode(true);
    try {
      for (final String p : set) {
        names.add(OBDal.getInstance().get(Process.class, p).getName());
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    Collections.sort(names);
    for (final String n : names) {
      log.info(n);
    }
  }

  // a special case whereby an identifier property is again a reference to
  // another entity, then this other entity is also derived readable, etc.
  private void addDerivedReadableIdentifierProperties(Entity entity) {
    for (final Property p : entity.getProperties()) {
      if (p.isIdentifier() && p.getTargetEntity() != null
          && !readableEntities.contains(p.getTargetEntity())
          && !derivedReadableEntities.contains(p.getTargetEntity())) {
        derivedReadableEntities.add(p.getTargetEntity());
        addDerivedReadableIdentifierProperties(p.getTargetEntity());
      }
    }
  }

  /**
   * @param entity
   *          the entity to check
   * @return true if the entity is derived readable for this user, otherwise false is returned.
   */
  public boolean isDerivedReadable(Entity entity) {
    // prevent infinite looping
    if (!isInitialized) {
      return false;
    }

    // false is the allow read reply
    if (obContext.isInAdministratorMode()) {
      return false;
    }
    return derivedReadableEntities.contains(entity);
  }

  /**
   * @param entity
   *          the entity to check
   * @return true if the entity is writable for this user, otherwise false is returned.
   */
  public boolean isWritable(Entity entity) {
    // prevent infinite looping
    if (!isInitialized) {
      return true;
    }

    if (obContext.isInAdministratorMode()) {
      return true;
    }
    return isWritableWithoutAdminMode(entity);
  }

  /**
   * Checks if an entity is writable for this user. If not then a OBSecurityException is thrown.
   * 
   * @param entity
   *          the entity to check
   * @throws OBSecurityException
   */
  public void checkWritable(Entity entity) {
    if (!isWritable(entity)) {
      throw new OBSecurityException("Entity " + entity + " is not writable by this user");
    }
  }

  /**
   * Checks if an entity is readable for this user. If not then a OBSecurityException is thrown.
   * 
   * @param entity
   *          the entity to check
   * @throws OBSecurityException
   */
  public void checkReadable(Entity entity) {
    // prevent infinite looping
    if (!isInitialized) {
      return;
    }

    if (obContext.isInAdministratorMode()) {
      return;
    }

    if (nonReadableEntities.contains(entity)) {
      throw new OBSecurityException("Entity " + entity + " is not readable by this user");
    }

    if (derivedReadableEntities.contains(entity)) {
      return;
    }

    if (!readableEntities.contains(entity)) {
      throw new OBSecurityException(
          "Entity " + entity + " is not readable by the user " + obContext.getUser().getId());
    }
  }

  /**
   * Checks if an entity is readable for current user. It is not take into account admin mode.
   * 
   * @param entity
   *          the entity to check
   */
  public void checkReadableAccess(Entity entity) {
    if (!isReadableWithoutAdminMode(entity)) {
      throw new OBSecurityException("Entity " + entity + " is not accessible by this role/user: "
          + obContext.getRole().getName() + "/" + obContext.getUser().getName());
    }
  }

  /**
   * Checks if an entity is derived for current user. It is not take into account admin mode.
   * 
   * @param entity
   *          the entity to check
   */
  public void checkDerivedAccess(Entity entity) {
    if (!isDerivedWithoutAdminMode(entity)) {
      throw new OBSecurityException("Entity " + entity + " is not accessible by this role/user: "
          + obContext.getRole().getName() + "/" + obContext.getUser().getName());
    }
  }

  /**
   * Checks if an entity is writable for current user. It is not take into account admin mode.
   * 
   * @param entity
   *          the entity to check
   */
  public void checkWritableAccess(Entity entity) {
    if (!isWritableWithoutAdminMode(entity)) {
      throw new OBSecurityException("Entity " + entity + " is not writable by this role/user: "
          + obContext.getRole().getName() + "/" + obContext.getUser().getName());
    }
  }

  /**
   * Checks if a process is accessible for current user. It is not take into account admin mode.
   */
  public boolean checkProcessAccess(String processId) {
    // prevent infinite looping
    if (!isInitialized) {
      return false;
    }

    return processes.contains(processId);
  }

  public String getRoleId() {
    return roleId;
  }

  public void setRoleId(String roleId) {
    this.roleId = roleId;
  }

  public OBContext getObContext() {
    return obContext;
  }

  public void setObContext(OBContext obContext) {
    this.obContext = obContext;
  }

  public Set<Entity> getReadableEntities() {
    return readableEntities;
  }

  public Set<Entity> getWritableEntities() {
    return writableEntities;
  }

  public Set<Entity> getDerivedReadableEntities() {
    return derivedReadableEntities;
  }

  public Set<Entity> getDerivedEntitiesFromProcess() {
    return derivedEntitiesFromProcess;
  }

  private boolean isReadableWithoutAdminMode(Entity entity) {
    // prevent infinite looping
    if (!isInitialized) {
      return false;
    }

    if (readableEntities.contains(entity)) {
      return true;
    }

    return false;
  }

  private boolean isDerivedWithoutAdminMode(Entity entity) {
    // prevent infinite looping
    if (!isInitialized) {
      return false;
    }

    if (readableEntities.contains(entity)) {
      return true;
    }

    if (derivedReadableEntities.contains(entity)) {
      return true;
    }

    if (derivedEntitiesFromProcess.contains(entity)) {
      return true;
    }

    return false;
  }

  private boolean isWritableWithoutAdminMode(Entity entity) {
    // prevent infinite looping
    if (!isInitialized) {
      return false;
    }
    return writableEntities.contains(entity);
  }

  /**
   * For those processes that are granted, entities used by them through selectors and grids (window
   * references) need also to be granted
   */
  private void addEntitiesFromProcesses() {
    if (processes.isEmpty()) {
      return;
    }

    final ModelProvider mp = ModelProvider.getInstance();
    tabsWithSelectors = new HashSet<String>();

    for (Object[] ref : getProcessWindowReference(processes)) {
      addEntitiesFromProcessWindowReference(mp, ref);
    }

    for (Object[] selTabs : getSelectorReferencesFromTabs(tabsWithSelectors)) {
      addEntitiesOfSelectorReference(mp, selTabs);
    }

    for (Object[] ref : getProcessSelectorReference(processes)) {
      addEntitiesFromProcessSelectorReference(mp, ref);
    }
  }

  private List<Object[]> getProcessWindowReference(Set<String> processTables) {
    return getReferences(processTables, parameterOfWindowProcessReference);
  }

  private List<Object[]> getProcessSelectorReference(Set<String> processTables) {
    return getReferences(processTables, parameterOfSelectorProcessReference);
  }

  private List<Object[]> getSelectorReferencesFromTabs(Set<String> tabs) {
    return getReferences(tabs, selectorsFromWindowReferences);
  }

  private List<Object[]> getReferences(Set<String> filterElements, List<Object[]> references) {
    List<Object[]> selectedReferences = new ArrayList<Object[]>();
    for (Object[] ref : references) {
      if (filterElements.contains(ref[FILTER_ELEMENT_ID])) {
        selectedReferences.add(ref);
      }
    }
    return selectedReferences;
  }

  private void addEntitiesFromProcessWindowReference(ModelProvider mp, Object[] ref) {
    addEntitiesOfWindowReference(mp, ref);
  }

  private void addEntitiesFromProcessSelectorReference(ModelProvider mp, Object[] ref) {
    addEntitiesOfSelectorReference(mp, ref);
  }

  /**
   * Obtain entity from selector and added to derivedReadableEntities to take into account as a
   * derived entity.
   */
  private void addEntitiesOfSelectorReference(ModelProvider mp, Object[] ref) {
    final Entity derivedEntity = mp.getEntityByTableId((String) ref[SELECTED_TABLE_ID]);
    if (!writableEntities.contains(derivedEntity) && !readableEntities.contains(derivedEntity)
        && !derivedReadableEntities.contains(derivedEntity)
        && !nonReadableEntities.contains(derivedEntity)) {
      derivedEntitiesFromProcess.add(derivedEntity);
    }
  }

  /**
   * Obtain entities from window and added to readable and writable entities.
   */
  private void addEntitiesOfWindowReference(ModelProvider mp, Object[] ref) {
    tabsWithSelectors.add((String) ref[TAB_ID]);

    final Entity derivedEntity = mp.getEntityByTableId((String) ref[SELECTED_TABLE_ID]);
    if (!writableEntities.contains(derivedEntity) && !readableEntities.contains(derivedEntity)
        && !nonReadableEntities.contains(derivedEntity)) {
      readableEntities.add(derivedEntity);
      writableEntities.add(derivedEntity);
      // Removed from derived entities
      if (derivedReadableEntities.contains(derivedEntity)) {
        derivedReadableEntities.remove(derivedEntity);
      }
    }
  }
}
