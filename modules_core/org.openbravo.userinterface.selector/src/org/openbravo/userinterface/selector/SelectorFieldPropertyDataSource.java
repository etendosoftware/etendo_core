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
package org.openbravo.userinterface.selector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.datasource.ModelDataSourceService;

/**
 * The datasource for the Selector Field. Gets the table from the parent.
 * 
 * @author mtaal
 */
public class SelectorFieldPropertyDataSource extends ModelDataSourceService {

  private static final String SELECTOR_FIELD = "inpobuiselSelectorId";
  private static final String TAB_ID = "inpTabId";

  @Override
  public void checkFetchDatasourceAccess(Map<String, String> parameter) {
    final OBContext obContext = OBContext.getOBContext();
    final String tabId = parameter.get(TAB_ID);
    try {
      final Entity entity = ModelProvider.getInstance().getEntityByTableId(tabId);
      if (entity != null) {
        obContext.getEntityAccessChecker().checkReadableAccess(entity);
      }
    } catch (OBSecurityException e) {
      handleExceptionUnsecuredDSAccess(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.service.datasource.ModelDataSourceService#getBaseEntity(java.util.Map)
   */
  // gets the base entity on the basis of the selector definition
  @Override
  protected Entity getBaseEntity(Map<String, String> parameters) {
    final String selectorId = parameters.get(SELECTOR_FIELD);
    if (selectorId == null) {
      return super.getBaseEntity(parameters);
    }
    final org.openbravo.userinterface.selector.Selector selector = OBDal.getInstance()
        .get(org.openbravo.userinterface.selector.Selector.class, selectorId);
    if (selector == null) {
      // TODO: log this?
      return super.getBaseEntity(parameters);
    }
    String entityName = null;
    if (selector.getTable() != null) {
      entityName = selector.getTable().getName();
    } else if (selector.getObserdsDatasource() != null
        && selector.getObserdsDatasource().getTable() != null) {
      entityName = selector.getObserdsDatasource().getTable().getName();
    }
    if (entityName != null) {
      return ModelProvider.getInstance().getEntity(entityName);
    }
    return super.getBaseEntity(parameters);
  }

  @Override
  protected List<Property> getEntityProperties(Entity entity) {
    final List<Property> entityProperties = super.getEntityProperties(entity);
    final List<Property> toRemove = new ArrayList<Property>();
    for (Property prop : entityProperties) {
      if (prop.isOneToMany()) {
        toRemove.add(prop);
      }
      // Blob columns have been disabled because they are not supported by the new selectors
      // See issue https://issues.openbravo.com/view.php?id=13147
      if (prop.getReferencedProperty() != null) {
        if (StringUtils.equals(prop.getReferencedProperty().getColumnName(), "AD_Image_ID")) {
          toRemove.add(prop);
        }
      }
    }
    entityProperties.removeAll(toRemove);
    return entityProperties;
  }

}
