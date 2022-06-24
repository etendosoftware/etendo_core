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
 * All portions are Copyright (C) 2013-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.utility.TableTree;
import org.openbravo.service.datasource.DataSourceService;
import org.openbravo.service.datasource.DataSourceServiceProvider;

/**
 * This event handler listens to events that are fired in all the tables with the flag isTree
 * checked. When a record is created or deleted in one of these tables, the add or remove method of
 * the corresponding datasource is executed.
 * 
 */
class TreeTablesEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = getTreeTables();

  private static final String TREENODE_DATASOURCE = "90034CAE96E847D78FBEF6D38CB1930D";
  private static final String LINKTOPARENT_DATASOURCE = "610BEAE5E223447DBE6FF672B703F72F";

  private static final String TREENODE_STRUCTURE = "ADTree";
  private static final String LINKTOPARENT_STRUCTURE = "LinkToParent";
  private static final String CUSTOM_STRUCTURE = "Custom";
  private static Logger logger = LogManager.getLogger();

  @Inject
  private DataSourceServiceProvider dataSourceServiceProvider;

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onNew(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    OBContext.setAdminMode(true);
    try {
      BaseOBObject bob = event.getTargetInstance();
      DataSourceService dataSource = getDataSource(bob.getEntity().getTableId());
      if (dataSource == null) {
        return;
      }
      JSONObject jsonBob = this.fromBobToJSONObject(bob);
      Map<String, String> parameters = new HashMap<>();
      parameters.put("jsonBob", jsonBob.toString());
      dataSource.add(parameters, null);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    OBContext.setAdminMode(true);
    try {
      BaseOBObject bob = event.getTargetInstance();
      DataSourceService dataSource = getDataSource(bob.getEntity().getTableId());
      if (dataSource == null) {
        return;
      }
      JSONObject jsonBob = this.fromBobToJSONObject(bob);
      Map<String, String> parameters = new HashMap<>();
      parameters.put("jsonBob", jsonBob.toString());
      dataSource.remove(parameters);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private DataSourceService getDataSource(String tableId) {
    Table table = OBDal.getInstance().getProxy(Table.class, tableId);
    OBCriteria<TableTree> obq = OBDal.getInstance().createCriteria(TableTree.class);
    obq.add(Restrictions.eq(TableTree.PROPERTY_TABLE, table));
    obq.add(Restrictions.eq(TableTree.PROPERTY_ISMAINTREE, true));
    List<TableTree> tableTreeList = obq.list();
    if (tableTreeList.isEmpty()) {
      return null;
    }
    TableTree mainTableTree = tableTreeList.get(0);
    DataSourceService dataSource = null;
    if (TREENODE_STRUCTURE.equals(mainTableTree.getTreeStructure())) {
      dataSource = dataSourceServiceProvider.getDataSource(TREENODE_DATASOURCE);
    } else if (LINKTOPARENT_STRUCTURE.equals(mainTableTree.getTreeStructure())) {
      dataSource = dataSourceServiceProvider.getDataSource(LINKTOPARENT_DATASOURCE);
    } else if (CUSTOM_STRUCTURE.equals(mainTableTree.getTreeStructure())
        && mainTableTree.getDatasource() != null) {
      String customDataSourceId = mainTableTree.getDatasource().getId();
      dataSource = dataSourceServiceProvider.getDataSource(customDataSourceId);
    }
    return dataSource;
  }

  private JSONObject fromBobToJSONObject(BaseOBObject bob) {
    Entity entity = bob.getEntity();
    List<Property> propertyList = entity.getProperties();
    JSONObject jsonBob = new JSONObject();
    try {
      for (Property property : propertyList) {
        if (property.isOneToMany()) {
          continue;
        }
        if (property.getReferencedProperty() != null) {
          BaseOBObject referencedbob = (BaseOBObject) bob.get(property.getName());
          if (referencedbob != null) {
            jsonBob.put(property.getName(), referencedbob.getId());
          } else {
            jsonBob.put(property.getName(), (Object) null);
          }
        } else {
          jsonBob.put(property.getName(), bob.get(property.getName()));
        }
      }
      jsonBob.put("_entity", entity.getName());
    } catch (JSONException e) {
      logger.error("Error while converting the BOB to JsonObject", e);
    }
    return jsonBob;
  }

  private static Entity[] getTreeTables() {
    OBContext.setAdminMode(true);
    try {
      OBCriteria<Table> treeTablesCriteria = OBDal.getInstance().createCriteria(Table.class);
      treeTablesCriteria.add(Restrictions.eq(Table.PROPERTY_ISTREE, true));
      List<Table> treeTableList = treeTablesCriteria.list();
      ArrayList<Entity> entityArray = new ArrayList<>();
      for (Table treeTable : treeTableList) {
        entityArray.add(ModelProvider.getInstance().getEntityByTableId(treeTable.getId()));
      }
      return entityArray.toArray(new Entity[entityArray.size()]);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
