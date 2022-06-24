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
 * All portions are Copyright (C) 2009-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.datasource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;

/**
 * Provides {@link DataSourceService} instances and caches them in a global cache.
 * 
 * @author mtaal
 */
@ApplicationScoped
public class DataSourceServiceProvider {

  private Map<String, DataSourceService> dataSources = new ConcurrentHashMap<String, DataSourceService>();

  @Inject
  private WeldUtils weldUtils;

  /**
   * Checks the internal cache for a datasource with the requested name and returns it if found. If
   * not found a new one is created, which is cached and then returned.
   * 
   * @param dataSourceIdentifier
   *          the name by which to search and identify the data source.
   * @return a {@link DataSourceService} object
   */
  public DataSourceService getDataSource(String dataSourceIdentifier) {
    DataSourceService dataSourceService = dataSources.get(dataSourceIdentifier);
    if (dataSourceService == null) {
      OBContext.setAdminMode();
      try {
        DataSource dataSource = getRealDataSource(dataSourceIdentifier);
        dataSourceService = getDataSourceServiceFromDataSource(dataSource, dataSourceIdentifier);
        dataSources.put(dataSourceIdentifier, dataSourceService);
      } catch (Exception e) {
        throw new OBException(e);
      } finally {
        OBContext.restorePreviousMode();
      }
    }
    return dataSourceService;
  }

  /**
   * Obtains a dataSource given a dataSource identifier.
   * 
   * This class should have been named getDataSource instead of getRealDataSource, but the name was
   * already taken by a public method that returns a DataSourceService
   * 
   * @param dataSourceIdentifier
   *          a string that identifies the dataSource. it can be either the ID of the DataSource,
   *          the name of the DataSource or the name of the Table whose datasource is to be
   *          retrieved
   * @return the datasource associated with the provided identifier or null if there aren't any
   */
  private DataSource getRealDataSource(String dataSourceIdentifier) {
    // Checks if the dataSourceIdentifier the ID of the DataSource
    DataSource dataSource = getDataSourceFromDataSourceId(dataSourceIdentifier);
    if (dataSource == null) {
      // If it is not the ID of the DataSource, checks if it is its name
      dataSource = getDataSourceFromDataSourceName(dataSourceIdentifier);
      if (dataSource == null) {
        // If the dataSourceIdentifier is not the DataSource ID nor its name, checks if it is the
        // name of a Table
        dataSource = getDataSourceFromTableName(dataSourceIdentifier);
      }
    }
    return dataSource;
  }

  private DataSource getDataSourceFromDataSourceId(String dataSourceId) {
    return OBDal.getInstance().get(DataSource.class, dataSourceId);
  }

  private DataSource getDataSourceFromDataSourceName(String dataSourceName) {
    final OBCriteria<DataSource> obCriteria = OBDal.getInstance().createCriteria(DataSource.class);
    obCriteria.add(Restrictions.eq(DataSource.PROPERTY_NAME, dataSourceName));
    // obserds_datasource.name has unique constraint
    return (DataSource) obCriteria.uniqueResult();
  }

  private DataSource getDataSourceFromTableName(String tableName) {
    DataSource dataSource = null;
    final OBCriteria<Table> qTable = OBDal.getInstance().createCriteria(Table.class);
    qTable.add(Restrictions.eq(Table.PROPERTY_NAME, tableName));
    // ad_table.name is unique
    Table table = (Table) qTable.uniqueResult();
    if (table != null) {
      if (ApplicationConstants.DATASOURCEBASEDTABLE.equals(table.getDataOriginType())) {
        // If the table is based on a manual datasource, return that particular datasource
        dataSource = table.getObserdsDatasource();
      } else if (ApplicationConstants.HQLBASEDTABLE.equals(table.getDataOriginType())) {
        // If the table is based on a HQL table, use the 'HQL Tables Datasource'
        dataSource = OBDal.getInstance()
            .get(DataSource.class, ApplicationConstants.HQL_TABLE_DATASOURCE_ID);
      }
    }
    return dataSource;
  }

  /**
   * Returns a DataSourceService given a DataSource
   * 
   * @param dataSource
   *          the dataSource whose DataSourceService is to be retrieved
   * @param dataSourceIdentifier
   *          the name that was used to retrieve the dataSource
   * @return the DataSourceService associated with the provided DataSource, or the
   *         DefaultDataSourceService otherwise
   * @throws ClassNotFoundException
   */
  private DataSourceService getDataSourceServiceFromDataSource(DataSource dataSource,
      String dataSourceIdentifier) throws ClassNotFoundException {
    DataSourceService ds = null;
    if (dataSource == null) {
      // if no dataSource is provided, return the DefaultDataSourceService
      ds = weldUtils.getInstance(DefaultDataSourceService.class);
      ds.setName(dataSourceIdentifier);
    } else {
      // try to retrieve the DataSourceService through the dataSource java class name, otherwise
      // return the DefaultDataSourceService
      if (dataSource.getJavaClassName() != null) {
        @SuppressWarnings("unchecked")
        final Class<DataSourceService> clz = (Class<DataSourceService>) OBClassLoader.getInstance()
            .loadClass(dataSource.getJavaClassName());
        ds = weldUtils.getInstance(clz);
      } else {
        ds = new DefaultDataSourceService();
      }
      ds.setDataSource(dataSource);
    }
    // don't fail if the entity does not exist, just don't assign it to the DataSourceService
    boolean checkIfNotExists = false;
    Entity entity = ModelProvider.getInstance().getEntity(dataSourceIdentifier, checkIfNotExists);
    if (entity != null) {
      ds.setEntity(entity);
    }
    return ds;
  }

}
