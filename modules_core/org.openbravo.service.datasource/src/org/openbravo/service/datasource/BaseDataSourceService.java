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
 * All portions are Copyright (C) 2010-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.application.CachedPreference;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.kernel.Template;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.userinterface.selector.Selector;
import org.openbravo.userinterface.selector.SelectorConstants;

/**
 * A base data source service which can be extended. It combines the common parts for data sources
 * which are based on an entity and full-computed data sources.
 * 
 * @author mtaal
 */
public abstract class BaseDataSourceService implements DataSourceService {
  private static final Logger log = LogManager.getLogger();

  private String name;
  private Template template;

  // TODO: move this to a config parameter
  private String dataUrl = DataSourceServlet.getServletPathPart() + "/";

  private String whereClause = null;
  private Entity entity;
  private DataSource dataSource;
  private List<DataSourceProperty> dataSourceProperties = new ArrayList<DataSourceProperty>();

  @Inject
  private CachedPreference cachedPreference;

  @Inject
  private ApplicationDictionaryCachedStructures cachedStructures;

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.service.datasource.DataSourceService#getTemplate()
   */
  @Override
  public Template getTemplate() {
    if (template == null) {
      template = OBDal.getInstance().get(Template.class, DataSourceConstants.DS_TEMPLATE_ID);
      if (template == null) {
        log.error("The default data source template with id " + DataSourceConstants.DS_TEMPLATE_ID
            + " is not present in the database. This is an error!");
      }
    }
    return template;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDataUrl() {
    return dataUrl;
  }

  @Override
  public void setDataUrl(String dataUrl) {
    this.dataUrl = dataUrl;
  }

  @Override
  public String getWhereClause() {
    return whereClause;
  }

  @Override
  public void setWhereClause(String whereClause) {
    this.whereClause = whereClause;
  }

  @Override
  public List<DataSourceProperty> getDataSourceProperties(Map<String, Object> parameters) {
    return dataSourceProperties;
  }

  @Override
  public DataSource getDataSource() {
    return dataSource;
  }

  @Override
  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
    setName(dataSource.getId());
    dataSourceProperties = new ArrayList<DataSourceProperty>();
    for (DatasourceField dsField : dataSource.getOBSERDSDatasourceFieldList()) {
      if (dsField.isActive()) {
        dataSourceProperties.add(DataSourceProperty.createFromDataSourceField(dsField));
      }
    }
    if (dataSource.getTable() != null) {
      setEntity(ModelProvider.getInstance().getEntity(dataSource.getTable().getName()));
    }
    setWhereClause(dataSource.getHQLWhereClause());
  }

  @Override
  public void checkEditDatasourceAccess(Map<String, String> parameters) {
    Entity entityToCheck = getEntity();
    final OBContext obContext = OBContext.getOBContext();
    if (entity != null) {
      try {
        obContext.getEntityAccessChecker().checkWritableAccess(entityToCheck);
      } catch (OBSecurityException e) {
        handleExceptionUnsecuredDSAccess(e);
      }
    }
  }

  @Override
  public void checkFetchDatasourceAccess(Map<String, String> parameters) {
    Entity entityToCheck = getEntity();
    final OBContext obContext = OBContext.getOBContext();
    String selectorId = parameters.get(SelectorConstants.DS_REQUEST_SELECTOR_ID_PARAMETER);
    if (StringUtils.isNotBlank(selectorId)) {
      // selectors
      if (entityToCheck == null) {
        OBContext.setAdminMode(true);
        try {
          Selector sel = OBDal.getInstance().get(Selector.class, selectorId);
          entityToCheck = ModelProvider.getInstance().getEntityByTableId(sel.getTable().getId());
        } finally {
          OBContext.restorePreviousMode();
        }
      }
      if (entityToCheck != null) {
        try {
          obContext.getEntityAccessChecker().checkDerivedAccess(entityToCheck);
        } catch (OBSecurityException e) {
          handleExceptionUnsecuredDSAccess(e);
        }
      }
    } else if (entityToCheck != null) {
      try {
        obContext.getEntityAccessChecker().checkReadableAccess(entityToCheck);
      } catch (OBSecurityException e) {
        handleExceptionUnsecuredDSAccess(e);
      }
    }
  }

  /**
   * This method returns a String with the where and filter clauses that will be applied.
   *
   * @return A String with the value of the where and filter clause. It can be null when there is no
   *         filter clause nor where clause.
   */
  protected String getWhereAndFilterClause(Map<String, String> parameters) {
    if (!parameters.containsKey(JsonConstants.TAB_PARAMETER)) {
      return "";
    } else {
      String whereAndFilterClause = null;
      String tabId = parameters.get(JsonConstants.TAB_PARAMETER);
      try {
        OBContext.setAdminMode(true);
        Tab tab = cachedStructures.getTab(tabId);
        String where = tab.getHqlwhereclause();
        if (isFilterApplied(parameters)) {
          String filterClause = getFilterClause(tab);
          if (StringUtils.isNotBlank(where)) {
            whereAndFilterClause = " ((" + where + ") and (" + filterClause + "))";
          } else {
            whereAndFilterClause = filterClause;
          }
        } else if (StringUtils.isNotBlank(where)) {
          whereAndFilterClause = where;
        }
      } finally {
        OBContext.restorePreviousMode();
      }
      return whereAndFilterClause;
    }
  }

  protected void handleExceptionUnsecuredDSAccess(OBSecurityException securityException) {
    if (!Preferences.YES
        .equals(cachedPreference.getPreferenceValue(CachedPreference.ALLOW_UNSECURED_DS_REQUEST))) {
      throw new OBSecurityException(securityException);
    } else {
      log.warn(securityException.getMessage() + " but in fact it is being allowed access.");
    }
  }

  @Override
  public Entity getEntity() {
    return entity;
  }

  @Override
  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  private boolean isRootTab(Tab tab) {
    return tab.getTabLevel() == 0;
  }

  private String getFilterClause(Tab tab) {
    String tableId = tab.getTable().getId();
    Entity ent = ModelProvider.getInstance().getEntityByTableId(tableId);
    boolean isTransactionalWindow = tab.getWindow().getWindowType().equals("T");
    String filterClause = null;
    if (tab.getHqlfilterclause() == null) {
      filterClause = "";
    } else {
      filterClause = tab.getHqlfilterclause();
    }
    if (!isTransactionalFilterApplied(isTransactionalWindow, tab)) {
      return filterClause;
    }
    String transactionalFilter = " e.updated > " + JsonConstants.QUERY_PARAM_TRANSACTIONAL_RANGE
        + " ";
    if (ent.hasProperty(Order.PROPERTY_PROCESSED)) {
      transactionalFilter += " or e.processed = 'N' ";
    }
    transactionalFilter = " (" + transactionalFilter + ") ";

    if (filterClause.length() > 0) {
      return " (" + transactionalFilter + " and (" + filterClause + ")) ";
    }
    return transactionalFilter;
  }

  private boolean isTransactionalFilterApplied(boolean isTransactionalWindow, Tab tab) {
    return isTransactionalWindow && isRootTab(tab);
  }

  private boolean isFilterApplied(Map<String, String> parameters) {
    return "true".equals(parameters.get(JsonConstants.FILTER_APPLIED_PARAMETER));
  }
}
