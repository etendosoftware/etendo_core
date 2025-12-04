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

package org.openbravo.base.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import com.etendoerp.sequences.model.SequenceConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.openbravo.base.ConnectionProviderContextListener;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.domaintype.BaseDomainType;
import org.openbravo.base.model.domaintype.OneToManyDomainType;
import org.openbravo.base.model.domaintype.StringDomainType;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.provider.OBSingleton;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.base.session.UniqueConstraintColumn;
import org.openbravo.base.util.Check;
import org.openbravo.base.util.CheckException;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.ConnectionProviderImpl;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.exception.PoolNotFoundException;

/**
 * Builds the Runtime model base on the data model (application dictionary: table, column,
 * reference, etc). Makes the runtime model (Entity and Property) available to the rest of the
 * system.
 * 
 * @see Entity
 * @see Property
 * @see Table
 * @see Column
 * 
 * @author iperdomo
 * @author mtaal
 */

public class ModelProvider implements OBSingleton {
  private static final Logger log = LogManager.getLogger();
  private static final String DEPRECATED_STATUS = "DP";

  private static ModelProvider instance;
  private List<Entity> model = null;
  private List<Table> tables = null;
  private HashMap<String, Table> tablesByTableName = null;
  private HashMap<String, Table> dataSourceTablesByName = null;
  private Map<String, RefTable> refTableMap = new HashMap<String, RefTable>();
  private Map<String, RefSearch> refSearchMap = new HashMap<String, RefSearch>();
  private HashMap<String, Entity> entitiesByName = null;
  private HashMap<String, Entity> entitiesByClassName = null;
  private HashMap<String, Entity> entitiesByTableName = null;
  private HashMap<String, Entity> entitiesByTableId = null;
  private HashMap<String, Reference> referencesById = null;
  // a list because for small numbers a list is faster than a hashmap
  private List<Entity> entitiesWithTreeType = null;
  private HashMap<Entity, List<String>> entitiesWithImage = null;
  private HashMap<Entity, List<String>> entitiesWithFile = null;
  private List<Module> modules;
  private Session initsession;

  private static final String TABLEBASEDTABLE = "Table";

  private static final Set<String> ENTITIES_WITHOUT_ALL_CHILD_PROPERTIES = new HashSet<>(
      Arrays.asList("org.openbravo.model.ad.system.Client",
          "org.openbravo.model.common.enterprise.Organization",
          "org.openbravo.model.ad.module.Module", "org.openbravo.model.ad.system.Language"));

  /**
   * Returns the singleton instance providing the ModelProvider functionality.
   * 
   * @return the ModelProvider instance
   */
  public static synchronized ModelProvider getInstance() {
    // set in a localInstance to prevent threading issues when
    // reseting it in setInstance()
    ModelProvider localInstance = instance;
    if (localInstance == null) {
      localInstance = OBProvider.getInstance().get(ModelProvider.class);
      instance = localInstance;
    }
    return localInstance;
  }

  /**
   * Makes it possible to override the default ModelProvider with a custom implementation.
   * 
   * @param instance
   *          the custom ModelProvider
   */
  public static synchronized void setInstance(ModelProvider instance) {
    ModelProvider.instance = instance;
  }

  /**
   * Creates a new ModelProvider, initializes it and sets it in the instance here.
   */
  public static void refresh() {
    try {
      OBProvider.getInstance().removeInstance(ModelProvider.class);
      final ModelProvider localProvider = OBProvider.getInstance().get(ModelProvider.class);
      setInstance(localProvider);
      // initialize it
      localProvider.getModel();
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * The list of Entities created on the basis of the Application Dictionary. The main entry point
   * for retrieving the in-memory model. This method will initialize the in-memory model when it is
   * called for the first time.
   * 
   * @return the list Entities
   */
  public List<Entity> getModel() {
    if (model == null) {
      initialize();
    }

    return model;
  }

  private void initialize() {
    long modelBuildTime;
    long startTime = System.currentTimeMillis();
    log.info("Building runtime model");
    // Caching model (tables, table-references, search-references,
    // list-references)
    // Changed to use the SessionHandler directly because the dal
    // layer uses the ModelProvider, so otherwise there will be a
    // cyclic relation.
    long t0 = System.currentTimeMillis();
    final ModelSessionFactoryController sessionFactoryController = new ModelSessionFactoryController();
    initializeReferenceClasses(sessionFactoryController);
    initsession = sessionFactoryController.getSessionFactory().openSession();
    final Transaction tx = initsession.beginTransaction();
    log.info("Session factory initialization took {}ms", System.currentTimeMillis() - t0);
    try {
      log.debug("Read model from db");

      // Tables are now sorted in the database query for better performance
      long t1 = System.currentTimeMillis();
      tables = list(initsession, Table.class, "name");
      log.info("Loaded {} tables in {}ms", tables.size(), System.currentTimeMillis() - t1);

      t1 = System.currentTimeMillis();
      final List<Reference> references = list(initsession, Reference.class);
      referencesById = new HashMap<>(references.size());
      for (Reference reference : references) {
        reference.getDomainType().setModelProvider(this);
        reference.getDomainType().initialize();
        referencesById.put(reference.getId(), reference);
      }
      log.info("Loaded {} references in {}ms", references.size(), System.currentTimeMillis() - t1);

      // read the columns in one query and assign them to the table
      t1 = System.currentTimeMillis();
      final List<Column> cols = readColumns(initsession);
      assignColumnsToTable(cols);
      log.info("Loaded {} columns in {}ms", cols.size(), System.currentTimeMillis() - t1);

      // reading will automatically link the reftable, refsearch and reflist
      // to the reference
      t1 = System.currentTimeMillis();
      final List<RefTable> refTables = list(initsession, RefTable.class);
      final List<RefSearch> refSearches = list(initsession, RefSearch.class);
      list(initsession, RefList.class);
      list(initsession, SequenceConfiguration.class);
      modules = retrieveModules(initsession);
      log.info("Retrieved modules");
      for (Module module : modules) {
        log.info("- Module " + module.getJavaPackage());
      }
      log.info("Retrieved {} modules in {}ms", modules.size(), System.currentTimeMillis() - t1);
      tables = removeInvalidTables(tables);

      // maintained for api support of the
      // getColumnByReference method
      for (final RefTable rt : refTables) {
        refTableMap.put(rt.getId(), rt);
      }
      for (final RefSearch rs : refSearches) {
        // note mapped by reference id
        refSearchMap.put(rs.getReference(), rs);
      }
      // see remark above

      // this map stores the mapped tables
      tablesByTableName = new HashMap<>(tables.size());
      dataSourceTablesByName = new HashMap<>(tables.size());
      for (final Table t : tables) {
        // tables are stored case insensitive!
        if (TABLEBASEDTABLE.equals(t.getDataOrigin())) {
          tablesByTableName.put(t.getTableName().toLowerCase(), t);
        } else {
          dataSourceTablesByName.put(t.getName().toLowerCase(), t);
        }
      }

      log.debug("Setting referencetypes for columns ");
      for (final Table t : tablesByTableName.values()) {
        t.setReferenceTypes(ModelProvider.instance);
      }
      for (final Table t : dataSourceTablesByName.values()) {
        t.setReferenceTypes(ModelProvider.instance);
      }

      model = new ArrayList<>(tables.size());
      entitiesByName = new HashMap<>(tables.size());
      entitiesByClassName = new HashMap<>(tables.size());
      entitiesByTableName = new HashMap<>(tables.size());
      entitiesByTableId = new HashMap<>(tables.size());
      entitiesWithTreeType = new ArrayList<>();
      entitiesWithImage = new HashMap<>(tables.size() / 10);
      entitiesWithFile = new HashMap<>(tables.size() / 10);
      for (final Table t : tables) {
        log.trace("Building model for table {}", t.getName());

        final Entity e = new Entity();
        e.initialize(t);
        model.add(e);
        entitiesByClassName.put(e.getClassName(), e);
        entitiesByName.put(e.getName(), e);
        if (TABLEBASEDTABLE.equals(t.getDataOrigin())) {
          entitiesByTableName.put(t.getTableName().toUpperCase(), e);
        }
        entitiesByTableId.put(t.getId(), e);
        if (e.getTreeType() != null) {
          entitiesWithTreeType.add(e);
        }

        if (e.hasComputedColumns()) {
          // When the entity has computed columns, an extra virtual entity is generated in order to
          // access these computed columns through a proxy that allows to compute them lazily.
          log.trace("Generating computed columns proxy entity for entity {}", e.getName());
          final Entity computedColsEntity = new Entity();
          computedColsEntity.initializeComputedColumns(t, e);

          model.add(computedColsEntity);
          entitiesByClassName.put(computedColsEntity.getClassName(), computedColsEntity);
          entitiesByName.put(computedColsEntity.getName(), computedColsEntity);
          entitiesByTableId.put(computedColsEntity.getTableId(), computedColsEntity);
        }
      }

      // in the second pass set all the referenceProperties
      // and targetEntities
      // uses global member tablesByTableName.
      // Obtains list of columns candidate to be translated, to be handled after setting properties
      // in parent entities.
      List<Column> translatableColumns = setReferenceProperties();

      // add virtual property for the case that the
      // id property is also a reference (a foreign key)
      // In this case hibernate requires two mappings
      // one for the id (a string) and for the reference
      // in addition the id generation strategy should be set
      // to foreign.
      log.debug("Setting virtual property for many-to-one id's");
      setVirtualPropertiesForReferenceId();

      buildUniqueConstraints(initsession, sessionFactoryController);

      final Map<String, Boolean> colMandatories = getColumnMandatories(initsession,
          sessionFactoryController);

      // initialize the name and also set the mandatory value on the basis
      // of the real not-null in the database!
      for (final Entity e : model) {
        for (final Property p : e.getProperties()) {
          if (!p.isOneToMany()) {
            p.initializeName();
            // don't do mandatory value setting for views, computed columns or datasource based
            // tables
            if (!e.isView() && p.getColumnName() != null && !e.isDataSourceBased()
                && !e.isHQLBased() && !e.isVirtualEntity()) {
              final Boolean mandatory = colMandatories
                  .get(createColumnMandatoryKey(e.getTableName(), p.getColumnName()));
              if (mandatory != null) {
                p.setMandatory(mandatory);
              } else if (!p.isComputedColumn() && !p.isProxy() && !e.isVirtualEntity()) {
                // only log in case the sql logic is not set and it is not a proxy
                log.warn("Column " + p + " mandatory setting not found in the database metadata. "
                    + "A cause can be that the column does not exist in the database schema");
              }
            }
          }
        }
        // dumpPropertyNames(e);
      }

      boolean generateAllChildProperties = OBPropertiesProvider.getInstance()
          .getBooleanProperty("hb.generate.all.parent.child.properties");
      if (generateAllChildProperties) {
        log.warn("Generating all children properties in parent entities.");
        log.warn(
            "Properties created from columns flagged as 'do not generate child property in parent entity' are deprecated and will be removed in a future release.");
      }
      for (final Entity e : model) {
        if (!e.isDataSourceBased() && !e.isHQLBased()) {
          createPropertyInParentEntity(e, generateAllChildProperties);
        }
      }

      for (final Entity e : model) {
        for (final Property p : e.getProperties()) {
          if (p.isOneToMany()) {
            p.initializeName();
          }
          if (p.getReferencedProperty() != null) {
            Entity referencedEntity = p.getReferencedProperty().getEntity();
            if ("ADImage".equals(referencedEntity.getName())) {
              entitiesWithImage.computeIfAbsent(p.getEntity(), k -> new ArrayList<>())
                  .add(p.getName());
            } else if ("OBPRF_FILE".equals(referencedEntity.getName())) {
              entitiesWithFile.computeIfAbsent(p.getEntity(), k -> new ArrayList<>())
                  .add(p.getName());
            }
          }
        }
      }

      setTranslatableColumns(translatableColumns);

    } finally {
      log.debug("Closing session and sessionfactory used during model read");
      tx.commit();
      initsession.close();
      sessionFactoryController.getSessionFactory().close();
    }
    clearLists();
    modelBuildTime = System.currentTimeMillis();
    log.info("Runtime model built successfully in {}ms", modelBuildTime - startTime);
  }

  private void setTranslatableColumns(List<Column> translatableColumns) {
    for (Column c : translatableColumns) {
      final Entity translationEntity = getEntityByTableName(c.getTable().getTableName() + "_Trl");

      Property translationProperty = null;
      if (translationEntity != null) {
        translationProperty = translationEntity.getPropertyByColumnName(c.getColumnName());
      }
      final Property thisProp = c.getProperty();
      thisProp.setTranslatable(translationProperty);
    }
  }

  /**
   * This method uses a normal JDBC connection to retrieve the classes of the references. These
   * classes will be instantiated and if they implement the correct interface, they will be added to
   * the SessionFactoryController
   */
  private void initializeReferenceClasses(ModelSessionFactoryController sessionFactoryController) {
    ConnectionProvider con = null;
    Connection connection = null;
    boolean createdNewPool = false;
    try {
      con = ConnectionProviderContextListener.getPool();
      if (con == null) {
        con = new ConnectionProviderImpl(
            OBPropertiesProvider.getInstance().getOpenbravoProperties());
        createdNewPool = true;
      }
      connection = con.getConnection();
      PreparedStatement ps = null;
      try {
        //@formatter:off
        String hql = 
                "select distinct model_impl " +
                "  from ad_reference " +
                " where model_impl is not null";
        //@formatter:on
        ps = connection.prepareStatement(hql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
          String classname = rs.getString(1);
          Class<?> myClass = Class.forName(classname);
          if (org.openbravo.base.model.domaintype.BaseDomainType.class.isAssignableFrom(myClass)) {
            BaseDomainType classInstance = (BaseDomainType) myClass.getDeclaredConstructor()
                .newInstance();
            for (Class<?> aClass : classInstance.getClasses()) {
              sessionFactoryController.addAdditionalClasses(aClass);
            }
          }
        }
      } finally {
        if (ps != null && !ps.isClosed()) {
          ps.close();
        }
      }
    } catch (Exception e) {
      throw new OBException("Failed to load reference classes", e);
    } finally {
      try {
        if (connection != null) {
          connection.close();
        }
      } catch (Exception e) {
        // do nothing
      }
      try {
        if (con != null && createdNewPool) {
          con.destroy();
        }
      } catch (Exception e) {
        // do nothing
      }
    }

  }

  /**
   * Returns list of tables known in the dal in memory model.
   * 
   * This excludes i.e. tables which do not have any column defined with iskey='Y'
   * 
   * @return list of tables known by dal in no particular stable order
   */
  public List<Table> getTables() {
    return new ArrayList<Table>(tablesByTableName.values());
  }

  /**
   * Returns list of dataSource based tables known in the dal in memory model.
   * 
   * @return list of dataSource based tables known by dal in no particular stable order
   */
  public List<Table> getDataSourceBasedTables() {
    return new ArrayList<Table>(dataSourceTablesByName.values());
  }

  /**
   * @return the last time that one of the relevant Application Dictionary objects was modified.
   *         Relevant AD objects are: Table, Column, Reference, RefList, RefSearch, RefTable,
   *         Module, Package.
   */
  public long computeLastUpdateModelTime() {
    final SessionFactoryController sessionFactoryController = new ModelSessionFactoryController();
    final Session session = sessionFactoryController.getSessionFactory().openSession();
    final Transaction tx = session.beginTransaction();
    try {
      // compute the last updated time
      long currentLastTimeUpdated = 0;
      currentLastTimeUpdated = getLastUpdated(Table.class, currentLastTimeUpdated, session);
      currentLastTimeUpdated = getLastUpdated(Column.class, currentLastTimeUpdated, session);
      currentLastTimeUpdated = getLastUpdated(RefTable.class, currentLastTimeUpdated, session);
      currentLastTimeUpdated = getLastUpdated(RefSearch.class, currentLastTimeUpdated, session);
      currentLastTimeUpdated = getLastUpdated(RefList.class, currentLastTimeUpdated, session);
      currentLastTimeUpdated = getLastUpdated(Module.class, currentLastTimeUpdated, session);
      currentLastTimeUpdated = getLastUpdated(Package.class, currentLastTimeUpdated, session);
      currentLastTimeUpdated = getLastUpdated(Reference.class, currentLastTimeUpdated, session);
      currentLastTimeUpdated = getLastUpdated(SequenceConfiguration.class, currentLastTimeUpdated, session);
      return currentLastTimeUpdated;
    } finally {
      tx.commit();
      session.close();
      sessionFactoryController.getSessionFactory().close();
    }
  }

  private <T extends ModelObject> long getLastUpdated(Class<T> clz, long currentLastTime,
      Session session) {
    final ModelObject mo = queryLastUpdateObject(session, clz);
    if (mo.getUpdated().getTime() > currentLastTime) {
      return mo.getUpdated().getTime();
    }
    return currentLastTime;
  }

  private <T extends ModelObject> T queryLastUpdateObject(Session session, Class<T> clazz) {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<T> criteria = builder.createQuery(clazz);
    Root<T> root = criteria.from(clazz);
    criteria.select(root);
    criteria.orderBy(builder.desc(root.get("updated")));

    Query<T> query = session.createQuery(criteria);
    query.setMaxResults(1);

    final List<T> list = query.list();
    if (list.isEmpty()) {
      throw new OBException("No instances of " + clazz.getName()
          + " in the database, has the database been created and filled with data?");
    }
    return list.get(0);

  }

  // clears some in-memory lists to save memory
  private void clearLists() {
    tables = null;
  }

  private List<Column> readColumns(Session session) {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<Column> criteria = builder.createQuery(Column.class);
    Root<Column> root = criteria.from(Column.class);
    criteria.select(root);
    criteria.orderBy(builder.asc(root.get("position")), builder.asc(root.get("name")));

    Query<Column> query = session.createQuery(criteria);
    return query.list();
  }

  private void assignColumnsToTable(List<Column> cols) {
    for (final Column column : cols) {
      final Table table = column.getTable();
      table.getColumns().add(column);
    }
  }

  private void setVirtualPropertiesForReferenceId() {

    for (final Entity e : entitiesByName.values()) {
      if (e.getIdProperties().size() == 1 && !e.getIdProperties().get(0).isPrimitive()) {
        createIdReferenceProperty(e);
      } else if (e.getIdProperties().size() > 1) {
        createCompositeId(e);
      }
    }
  }

  private List<Column> setReferenceProperties() {
    log.debug("Setting reference property");
    // uses global member tablesByTableName

    List<Column> translatableColumns = new ArrayList<Column>();
    for (final Table t : tablesByTableName.values()) {
      setReferencedPropertiesForTable(translatableColumns, t);
    }
    for (final Table t : dataSourceTablesByName.values()) {
      setReferencedPropertiesForTable(translatableColumns, t);
    }

    // setting referenced properties from computed columns
    for (final Entity entity : model) {
      if (!entity.isVirtualEntity()) {
        continue;
      }

      Entity baseEntity = entitiesByTableId
          .get(entity.getTableId().substring(0, entity.getTableId().indexOf("_CC")));
      if (baseEntity == null) {
        log.warn("Not found base entity for computed column entity " + entity);
        continue;
      }

      Table baseTable = tablesByTableName.get(baseEntity.getTableName().toLowerCase());
      if (baseTable == null) {
        log.warn("Not found base table for computed column entity " + entity);
        continue;
      }

      for (Property p : entity.getProperties()) {
        for (Column c : baseTable.getColumns()) {
          if (c.getColumnName().equals(p.getColumnName())) {
            if (!c.isPrimitiveType() && c.getReferenceType() != null) {
              p.setReferencedProperty(c.getReferenceType().getProperty());
            }
            break;
          }
        }
      }
    }

    return translatableColumns;
  }

  private void setReferencedPropertiesForTable(List<Column> translatableColumns, final Table t) {
    for (final Column c : t.getColumns()) {
      if (!c.isPrimitiveType()) {
        final Property thisProp = c.getProperty();
        log.debug("Setting targetEntity and reference Property for " + thisProp);
        final Column thatColumn = c.getReferenceType();
        if (thatColumn == null) {
          if (!OBPropertiesProvider.isFriendlyWarnings()) {
            log.error("Property " + thisProp
                + " is mapped incorrectly, there is no referenced column for it, removing from the mapping");
          }
          thisProp.getEntity().getProperties().remove(thisProp);
          if (thisProp.getEntity().getIdProperties().remove(thisProp)) {
            Check.fail("Incorrect mapping for property " + thisProp
                + " which is an id, mapping fails, stopping here");
          }
          thisProp.getEntity().getIdentifierProperties().remove(thisProp);
          continue;
        }

        // can occur if the column is read and returned through a
        // module provided Domain Type
        if (thatColumn.getProperty() == null) {
          final Entity entity = getEntityByTableName(thatColumn.getTable().getTableName());
          Check.isNotNull(entity, "No entity found using tablename "
              + thatColumn.getTable().getTableName() + " for column " + thatColumn);
          final Property property = entity.getPropertyByColumnName(thatColumn.getColumnName());
          thatColumn.setProperty(property);
        }

        // targetentity is set within setReferencedProperty
        final Property thatProperty = thatColumn.getProperty();
        thisProp.setReferencedProperty(thatProperty);
      }

      if (c.isTranslatable()) {
        translatableColumns.add(c);
      }
    }
  }

  private List<Table> removeInvalidTables(List<Table> allTables) {
    final List<Table> toRemove = new ArrayList<Table>();
    final List<Table> localTables = allTables;
    for (final Table t : localTables) {
      // taking into account inactive tables for now...

      // if (false && !t.isActive()) {
      // log.debug("Table " + t.getName() + " is not active ignoring it");
      // toRemove.add(t);
      // continue;
      // }

      // Support datasource based tables
      if (TABLEBASEDTABLE.equals(t.getDataOrigin())) {
        if (t.getPrimaryKeyColumns().size() == 0) {
          log.warn("Ignoring table/view " + t.getName() + " because it has no primary key columns");
          toRemove.add(t);
          continue;
        }
      }
    }
    allTables.removeAll(toRemove);
    return tables;
  }

  private Map<String, Boolean> getColumnMandatories(Session session,
      SessionFactoryController sfController) {
    final String columnQry = sfController.getColumnMetadataQuery();

    final Map<String, Boolean> result = new HashMap<>();
    for (final Object row : session.createNativeQuery(columnQry).list()) {
      final Object[] vals = (Object[]) row;
      final String key = createColumnMandatoryKey(vals[0], vals[1]);
      if (vals[2] instanceof String) {
        // note the string contains Y or N
        result.put(key, ((String) vals[2]).equalsIgnoreCase("N"));
      } else {
        result.put(key, (Boolean) vals[2]);
      }
    }
    return result;
  }

  private String createColumnMandatoryKey(Object tableName, Object columnName) {
    return tableName.toString().toUpperCase() + ";" + columnName.toString().toUpperCase();
  }

  // Build unique constraints
  private void buildUniqueConstraints(Session session,
      SessionFactoryController sessionFactoryController) {
    final List<UniqueConstraintColumn> uniqueConstraintColumns = getUniqueConstraintColumns(session,
        sessionFactoryController);
    Entity entity = null;
    UniqueConstraint uniqueConstraint = null;
    for (final UniqueConstraintColumn uniqueConstraintColumn : uniqueConstraintColumns) {
      // get the entity
      if (entity == null
          || !entity.getTableName().equalsIgnoreCase(uniqueConstraintColumn.getTableName())) {
        entity = getEntityByTableName(uniqueConstraintColumn.getTableName());
        uniqueConstraint = null;
      }
      if (entity == null) {
        continue;
      }

      // the uniqueconstraint
      if (uniqueConstraint == null || !uniqueConstraint.getName()
          .equalsIgnoreCase(uniqueConstraintColumn.getUniqueConstraintName())) {
        // note uniqueconstraint should be set to null, because the
        // for loop my not find another one
        uniqueConstraint = null;
        // get a new one, walk through all of them of the entity
        for (final UniqueConstraint uc : entity.getUniqueConstraints()) {
          if (uc.getName().equalsIgnoreCase(uniqueConstraintColumn.getUniqueConstraintName())) {
            uniqueConstraint = uc;
            break;
          }
        }
      }
      if (uniqueConstraint == null) {
        uniqueConstraint = new UniqueConstraint();
        uniqueConstraint.setEntity(entity);
        uniqueConstraint.setName(uniqueConstraintColumn.getUniqueConstraintName());
        entity.getUniqueConstraints().add(uniqueConstraint);
      }
      uniqueConstraint.addPropertyForColumn(uniqueConstraintColumn.getColumnName());
    }

    // dumpUniqueConstraints();
  }

  // returns a list of uniqueconstraint columns containing all
  // uniqueconstraints from the database
  private List<UniqueConstraintColumn> getUniqueConstraintColumns(Session session,
      SessionFactoryController sessionFactoryController) {
    final List<UniqueConstraintColumn> result = new ArrayList<>();
    @SuppressWarnings("rawtypes")
    final NativeQuery sqlQuery = session
        .createNativeQuery(sessionFactoryController.getUniqueConstraintQuery());
    for (final Object row : sqlQuery.list()) {
      // cast to an array of strings!
      // 0: tablename
      // 1: columnname
      // 2: uniqueconstraintname
      final Object[] values = (Object[]) row;
      Check.isTrue(values.length == 3,
          "Unexpected value length for constraint query, should be 3, but is " + values.length);
      final UniqueConstraintColumn uniqueConstraintColumn = new UniqueConstraintColumn();
      uniqueConstraintColumn.setTableName((String) values[0]);
      uniqueConstraintColumn.setColumnName((String) values[1]);
      uniqueConstraintColumn.setUniqueConstraintName((String) values[2]);
      result.add(uniqueConstraintColumn);
    }
    return result;
  }

  // expects that there is only one property
  private void createIdReferenceProperty(Entity e) {
    Check.isTrue(e.getIdProperties().size() == 1 && !e.getIdProperties().get(0).isPrimitive(),
        "Expect one id property for the entity and it should be a reference type");
    final Property idProperty = e.getIdProperties().get(0);
    log.debug("Handling many-to-one reference for " + idProperty);
    Check.isTrue(e.getIdProperties().size() == 1,
        "Foreign-key id-properties are only handled if there is one in an entity " + e.getName());
    // create a reference property
    final Property newProp = new Property();
    newProp.setEntity(e);
    newProp.setId(false);
    newProp.setIdentifier(idProperty.isIdentifier());
    newProp.setMandatory(true);
    newProp.setDomainType(idProperty.getDomainType());
    newProp.setColumnName(idProperty.getColumnName());
    newProp.setColumnId(idProperty.getColumnId());
    newProp.setParent(idProperty.isParent());
    newProp.setTargetEntity(idProperty.getTargetEntity());
    newProp.setReferencedProperty(idProperty.getTargetEntity().getIdProperties().get(0));
    newProp.setOneToOne(true);
    newProp.setChildPropertyInParent(idProperty.isChildPropertyInParent());

    // the name is the name of the class of the target without
    // the package part and with the first character lowercased
    final String propName = idProperty.getSimpleTypeName().substring(0, 1).toLowerCase()
        + idProperty.getSimpleTypeName().substring(1);
    newProp.setName(propName);
    e.addProperty(newProp);

    // and change the old id property to a primitive one
    // this assumes that the column in the target entity is itself
    // not a foreign key!
    final Property targetIdProp = idProperty.getTargetEntity().getIdProperties().get(0);
    Check.isTrue(targetIdProp.isPrimitive(), "Entity " + e
        + ", The ID property of the referenced class should be primitive, an other case is not supported");
    idProperty.setDomainType(targetIdProp.getDomainType());
    idProperty.setIdBasedOnProperty(newProp);
    idProperty.setIdentifier(false);
    idProperty.setParent(false);
    idProperty.setTargetEntity(null);
  }

  private void createCompositeId(Entity e) {
    Check.isTrue(e.getIdProperties().size() > 1,
        "Expect that entity " + e + " has more than one id property ");
    final Property compId = new Property();
    compId.setEntity(e);
    compId.setId(true);
    compId.setIdentifier(false);
    compId.setMandatory(true);
    final StringDomainType domainType = new StringDomainType();
    domainType.setModelProvider(this);
    compId.setDomainType(domainType);
    compId.setCompositeId(true);
    compId.setName("id");
    // compId is added to the entity below

    final List<Property> toRemove = new ArrayList<Property>();
    for (final Property p : e.getIdProperties()) {
      compId.getIdParts().add(p);
      p.setPartOfCompositeId(true);
      p.setId(false);
      toRemove.add(p);
    }
    e.getIdProperties().removeAll(toRemove);
    Check.isTrue(e.getIdProperties().size() == 0,
        "There should not be any id properties (entity " + e + ") at this point");

    // and now add the id property again
    e.addProperty(compId);
  }

  private void createPropertyInParentEntity(Entity e, boolean generateAllChildProperties) {
    try {
      List<Property> props = new ArrayList<>(e.getProperties());
      for (final Property p : props) {
        if (!shouldGenerateChildPropertyInParent(p, generateAllChildProperties)) {
          if (!generateAllChildProperties && shouldGenerateChildPropertyInParent(p, true)) {
            // When creating child property parent entity, base property is flagged as referenced,
            // let's keep this flag even it does not get generated as it affects
            // BaseOBObject.checkDerivedReadable.
            p.setBeingReferenced(true);
          }
          continue;
        }

        if (p.getReferencedProperty() == null) {
          // Log message in case referenced property is null, this will cause a NPE, which is not
          // solved but at least relevant info is shown to fix it in AD
          log.error("Referenced property is null for {}.{}", e.getName(), p.getName());
        }

        final Entity parent = p.getReferencedProperty().getEntity();
        createChildProperty(parent, p);
      }
    } catch (Exception ex) {
      log.error("Could not create parent entity properties for entity {}", e, ex);
    }
  }

  /**
   * Determines whether for a given property, it is a child that should have a property on its
   * parent entity.
   */
  public boolean shouldGenerateChildPropertyInParent(Property p,
      boolean generateAllChildProperties) {
    return (p.isChildPropertyInParent() || generateAllChildProperties) && !p.isOneToMany()
        && !p.isId() && !p.isAuditInfo() && p.getReferencedProperty() != null
        && (!ENTITIES_WITHOUT_ALL_CHILD_PROPERTIES
            .contains(p.getReferencedProperty().getEntity().getClassName()) || p.isParent())
        && p.getSqlLogic() == null;
  }

  private void createChildProperty(Entity parentEntity, Property childProperty) {
    final Property newProp = new Property();
    newProp.setEntity(parentEntity);
    newProp.setId(false);
    newProp.setIdentifier(false);
    newProp.setMandatory(false);
    final OneToManyDomainType domainType = new OneToManyDomainType();
    domainType.setModelProvider(this);
    newProp.setDomainType(domainType);
    newProp.setTargetEntity(childProperty.getEntity());
    newProp.setReferencedProperty(childProperty);
    newProp.setOneToOne(false);
    newProp.setOneToMany(true);
    newProp.setChild(childProperty.isParent());
    parentEntity.addProperty(newProp);
  }

  /**
   * Retrieves a list of model objects of the class passed as parameter.
   *
   * @param session
   *          the session used to query for the objects
   * @param clazz
   *          the class of the model objects to be retrieved
   * @return a list of model objects
   */
  public <T extends Object> List<T> list(Session session, Class<T> clazz) {
    return list(session, clazz, null);
  }

  /**
   * Retrieves a list of model objects of the class passed as parameter, optionally sorted.
   *
   * @param session
   *          the session used to query for the objects
   * @param clazz
   *          the class of the model objects to be retrieved
   * @param orderByField
   *          the field name to order by (optional, can be null)
   * @return a list of model objects
   */
  public <T extends Object> List<T> list(Session session, Class<T> clazz, String orderByField) {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<T> criteria = builder.createQuery(clazz);
    Root<T> root = criteria.from(clazz);
    criteria.select(root);

    if (orderByField != null) {
      criteria.orderBy(builder.asc(root.get(orderByField)));
    }

    Query<T> query = session.createQuery(criteria);
    return query.list();
  }

  public List<Module> getModules() {
    return modules;
  }

  private List<Module> retrieveModules(Session session) {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<Module> criteria = builder.createQuery(Module.class);
    Root<Module> root = criteria.from(Module.class);
    criteria.select(root);
    criteria.where(builder.equal(root.get("active"), true));
    criteria.orderBy(builder.asc(root.get("seqno")));

    Query<Module> query = session.createQuery(criteria);
    return query.list();
  }

  /**
   * Return the table using the tableName. If not found then a CheckException is thrown.
   * 
   * @param tableName
   * @return the Table object
   * @throws CheckException
   */
  public Table getTable(String tableName) throws CheckException {
    final Table table = getTableWithoutCheck(tableName);
    if (table == null) {
      if (OBPropertiesProvider.isFriendlyWarnings()) {
        // this error won't be logged...
        throw new IllegalArgumentException(
            "Table: " + tableName + " not found in runtime model, is it maybe inactive?");
      } else {
        Check.fail("Table: " + tableName + " not found in runtime model, is it maybe inactive?");
      }
    }
    return table;
  }

  /**
   * Retrieves a table using the tableName. If not found then continue without exception.
   *
   * @return Table if exists, otherwise null.
   */
  public Table getTableWithoutCheck(String tableName) {
    if (tablesByTableName == null) {
      getModel();
    }
    return tablesByTableName.get(tableName.toLowerCase());
  }

  /**
   * Retrieves an Entity using the entityName. If not found then a CheckException is thrown.
   * 
   * @param entityName
   *          the name used for searching the Entity.
   * @return the Entity object
   * @throws CheckException
   */
  public Entity getEntity(String entityName) throws CheckException {
    boolean checkIfNotExists = true;
    return getEntity(entityName, checkIfNotExists);
  }

  /**
   * Retrieves an Entity using the entityName. If not found then a CheckException is thrown if the
   * checkIfNotExists parameter is true.
   * 
   * @param entityName
   *          the name used for searching the Entity.
   * @param checkIfNotExists
   *          a boolean that is true calls to Check.fail if the entity does not exist
   * @return the Entity object
   * @throws CheckException
   */
  public Entity getEntity(String entityName, boolean checkIfNotExists) throws CheckException {
    if (model == null) {
      getModel();
    }
    final Entity entity = entitiesByName.get(entityName);
    if (entity == null && checkIfNotExists) {
      Check.fail("Mapping name: " + entityName + " not found in runtime model");
    }
    return entity;
  }

  /**
   * Returns an Entity using the table name of the table belonging to the Entity. If no Entity is
   * found then null is returned, no Exception is thrown.
   * 
   * Note: the AD_Table.tablename should be used here, not the AD_Table.name!
   * 
   * @param tableName
   *          the name used to search for the Entity
   * @return the Entity or null if not found
   */
  public Entity getEntityByTableName(String tableName) {
    if (model == null) {
      getModel();
    }
    final Entity entity = entitiesByTableName.get(tableName.toUpperCase());
    // is null for views
    // if (entity == null) {
    // log.warn("Table name: " + tableName + " not found in runtime model");
    // }
    return entity;
  }

  /**
   * Returns an Entity based on the ID of the table belonging to the Entity. If no Entity is found
   * then null is returned, no Exception is thrown.
   * 
   * @param tableId
   *          the ID of the table belonging to the table
   * @return the Entity or null if not found
   */
  public Entity getEntityByTableId(String tableId) {
    if (model == null) {
      getModel();
    }

    final Entity entity = entitiesByTableId.get(tableId);

    if (entity == null) {
      log.warn("Entity not found in runtime model for table id: " + tableId);
    }

    return entity;
  }

  /**
   * Searches for an Entity using the business object class implementing the Entity in the business
   * code. Throws a CheckException if the Entity can not be found.
   * 
   * @param clz
   *          the java class used for the Entity
   * @return the Entity
   * @throws CheckException
   */
  public Entity getEntity(Class<?> clz) throws CheckException {
    if (model == null) {
      getModel();
    }
    // TODO: handle subclasses, so if not found then try to find superclass!
    final Entity entity = entitiesByClassName.get(clz.getName());
    if (entity == null) {
      Check.fail("Class name: " + clz.getName() + " not found in runtime model");
    }
    return entity;
  }

  /**
   * Returns a reference instance from the org.openbravo.base.model package.
   * 
   * @param referenceId
   * @return the reference identified by the referenceId, if not found then null is returned
   */
  public Reference getReference(String referenceId) {
    return referencesById.get(referenceId);
  }

  /**
   * Returns all reference (instance from the org.openbravo.base.model package).
   * 
   * @return the references
   */
  public Collection<Reference> getAllReferences() {
    return referencesById.values();
  }

  /**
   * Returns the entity for a specific tree type. The tree type is used to link an entity to a tree
   * (see the AD_Tree table).
   * 
   * @param treeType
   *          the tree type
   * @return Entity or null if none found
   */
  public Entity getEntityFromTreeType(String treeType) {
    for (Entity entity : entitiesWithTreeType) {
      if (entity.getTreeType().equals(treeType)) {
        return entity;
      }
    }
    // note that treeType does not determine entity for new trees, in this case entity is obtained
    // from tree.getTable()
    return null;
  }

  /**
   * Returns the entities that have images
   * 
   * @return Entity list
   */
  public HashMap<Entity, List<String>> getEntityWithImage() {
    return entitiesWithImage;
  }

  /**
   * Returns the entities that have images
   * 
   * @return Entity list
   */
  public HashMap<Entity, List<String>> getEntityWithFile() {
    return entitiesWithFile;
  }

  /**
   * This method can be used to get the session of the ModelProvider. This method is intended to be
   * used by DomainType classes during initialization phase, to do queries in the database
   */
  public Session getSession() {
    return initsession;
  }

  /**
   * Adds help comments and deprecation status to corresponding entities and properties in the model
   */
  public void addHelpAndDeprecationToModel(boolean addDeprecation) {
    addHelpAndDeprecationToEntities(addDeprecation);
    addHelpAndDeprecationToProperties(addDeprecation);
  }

  /**
   * Gets and maps and deprecation status to entities without using DAL as it has not been
   * initialized here yet
   */
  private void addHelpAndDeprecationToEntities(boolean addDeprecation) {
    //@formatter:off
    String qry = "SELECT ad_table_id, help, developmentStatus "
               + "FROM AD_TABLE ";
    //@formatter:on
    try (Connection connection = getConnection();
        PreparedStatement ps = connection.prepareStatement(qry);
        ResultSet resultSet = ps.executeQuery()) {
      while (resultSet.next()) {
        Entity entity = entitiesByTableId.get(UtilSql.getValue(resultSet, "ad_table_id"));
        String helpComment = UtilSql.getValue(resultSet, "help");
        if ("".equals(helpComment)) {
          helpComment = null;
        }
        entity.setHelp(helpComment);
        if (addDeprecation) {
          String developmentStatus = UtilSql.getValue(resultSet, "developmentStatus");
          entity.setDeprecated(DEPRECATED_STATUS.equals(developmentStatus));
        }
      }
    } catch (Exception e) {
      log.error("Couldn't add help to entity. Failed database query.");
      throw new OBException("Couldn't add help to entity, failed database query.", e);
    }
  }

  /**
   * Gets and maps help and deprecation status to properties without using DAL as it has not been
   * initialized here yet
   */
  private void addHelpAndDeprecationToProperties(boolean addDeprecation) {
    //@formatter:off
    String qry = "SELECT ad_table_id, columnname, help, developmentStatus " +
                 "FROM AD_COLUMN";
    //@formatter:on
    try (Connection connection = getConnection();
        PreparedStatement ps = connection.prepareStatement(qry);
        ResultSet resultSet = ps.executeQuery()) {
      while (resultSet.next()) {
        Entity entity = entitiesByTableId.get(UtilSql.getValue(resultSet, "ad_table_id"));
        if (entity == null) {
          continue;
        }
        String columnName = UtilSql.getValue(resultSet, "columnname");
        String helpComment = UtilSql.getValue(resultSet, "help");
        Property property = entity.getPropertyByColumnName(columnName);
        if ("".equals(helpComment)) {
          helpComment = null;
        }
        if (addDeprecation) {
          String developmentStatus = UtilSql.getValue(resultSet, "developmentStatus");
          property.setDeprecated(DEPRECATED_STATUS.equals(developmentStatus));
        }
        property.setHelp(helpComment);
      }
    } catch (Exception e) {
      log.error("Not able to get column help from database.");
      throw new OBException("Couldn't add help to column, failed database query.", e);
    }
  }

  /**
   * Removes help comments and deprecation status from all entities and properties in the model
   */
  public void removeHelpAndDeprecationFromModel() {
    for (final Entity entity : getModel()) {
      entity.removeHelp();
      entity.removeDeprecated();
    }
  }

  private Connection getConnection() {
    ConnectionProvider con = ConnectionProviderContextListener.getPool();
    try {
      if (con == null) {
        con = new ConnectionProviderImpl(
            OBPropertiesProvider.getInstance().getOpenbravoProperties());
      }
      return con.getConnection();
    } catch (PoolNotFoundException | NoConnectionAvailableException e) {
      log.error("Couldn't establish database connection in ModelProvider", e);
      throw new OBException("Couldn't extablish database connection in ModelProvider.", e);
    }
  }
}
