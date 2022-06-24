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
 * All portions are Copyright (C) 2011-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.StaticResourceComponent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.service.datasource.DataSourceService;
import org.openbravo.service.datasource.DataSourceServiceProvider;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;

/**
 * Action handler which can delete multiple records in one transaction.
 * 
 * @author mtaal
 * @see StaticResourceComponent
 */
@ApplicationScoped
public class MultipleDeleteActionHandler extends BaseActionHandler {

  @Inject
  private DataSourceServiceProvider dataSourceServiceProvider;

  @Inject
  private ApplicationDictionaryCachedStructures cachedStructures;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {

    try {
      final JSONObject dataObject = new JSONObject(data);
      final String entityName = dataObject.getString("entity");
      final JSONArray ids = dataObject.getJSONArray("ids");
      DataSourceService dataSourceService = getCustomDataSourceService(entityName);
      for (int i = 0; i < ids.length(); i++) {
        if (dataSourceService == null) {
          final BaseOBObject object = OBDal.getInstance().get(entityName, ids.get(i));
          if (object != null) {
            OBDal.getInstance().remove(object);
            // https://issues.openbravo.com/view.php?id=21229#c51631
            OBDal.getInstance().flush();
            OBDal.getInstance().getSession().clear();
          }
        } else {
          // if a custom data source, remove them one at a time using the custom data source
          Map<String, String> dsParameters = new HashMap<String, String>();
          dsParameters.put(JsonConstants.ID, ids.getString(i));
          dataSourceService.remove(dsParameters);
        }
      }
      if (dataSourceService == null) {
        OBDal.getInstance().commitAndClose();
      }

      // just return an empty message, as the system knows how many have been deleted.
      return new JSONObject();
    } catch (Exception e) {
      try {
        return new JSONObject(JsonUtils.convertExceptionToJson(e));
      } catch (JSONException t) {
        throw new OBException(t);
      }
    }
  }

  /**
   * Gets the data source service in case it is a custom datasource, other cases it returns null.
   * 
   * Note this should be extended to always delegate in the datasource instead of deleting within
   * this class: see issue #28118
   *
   * @param entityName
   *          the entity name
   * @return the custom data source service
   */
  private DataSourceService getCustomDataSourceService(String entityName) {
    OBContext.setAdminMode(true);
    try {
      final Entity entity = ModelProvider.getInstance().getEntity(entityName);
      Table table = cachedStructures.getTable(entity.getTableId());
      if (ApplicationConstants.DATASOURCEBASEDTABLE.equals(table.getDataOriginType())) {
        // if a data source based table, return the data source service
        return dataSourceServiceProvider.getDataSource(table.getObserdsDatasource().getName());
      }
      // otherwise return null
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
