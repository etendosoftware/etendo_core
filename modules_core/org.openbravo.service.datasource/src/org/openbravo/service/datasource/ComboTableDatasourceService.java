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
 * All portions are Copyright (C) 2014-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.datasource;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonConstants;

/**
 * The implementation of the combo table reference datasource.
 * 
 * @author Shankar Balachandran
 */

public class ComboTableDatasourceService extends BaseDataSourceService {
  private static final Logger log = LogManager.getLogger();

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.service.datasource.DataSourceService#fetch(java.util.Map)
   */
  @Override
  public String fetch(Map<String, String> parameters) {
    long init = System.currentTimeMillis();
    Field field = null;
    Column column = null;
    FieldProvider[] fps = null;
    String fieldId = parameters.get("fieldId");
    String windowId = parameters.get("windowId");
    String filterString = null;

    int startRow = -1, endRow = -1, num = 0;

    OBContext.setAdminMode(true);
    try {
      field = OBDal.getInstance().get(Field.class, fieldId);
      column = field.getColumn();

      if (!StringUtils.isEmpty(parameters.get("criteria"))) {
        String criteria = parameters.get("criteria");
        for (String criterion : criteria.split(JsonConstants.IN_PARAMETER_SEPARATOR)) {
          try {
            JSONObject jsonCriterion = new JSONObject(criterion);
            if (jsonCriterion.getString("fieldName").equals(JsonConstants.IDENTIFIER)) {
              filterString = jsonCriterion.getString("value");
            }
          } catch (JSONException e) {
            log.error("Error getting criteria for ComboTableDataSoruce - field: " + fieldId
                + " - criteria: " + criteria, e);
          }
        }
      }

      if (parameters.get(JsonConstants.STARTROW_PARAMETER) != null) {
        startRow = Integer.parseInt(parameters.get(JsonConstants.STARTROW_PARAMETER));
      }
      if (parameters.get(JsonConstants.ENDROW_PARAMETER) != null) {
        endRow = Integer.parseInt(parameters.get(JsonConstants.ENDROW_PARAMETER));
      }
      boolean applyLimits = startRow != -1 && endRow != -1;
      if (!applyLimits) {
        throw new OBException(JsonConstants.STARTROW_PARAMETER + " and "
            + JsonConstants.ENDROW_PARAMETER + " not present");
      } else {
        if (endRow - startRow > 500) {
          throw new OBException("trying to retrieve more than 500 records");
        }
      }

      RequestContext rq = RequestContext.get();
      VariablesSecureApp vars = rq.getVariablesSecureApp();

      ApplicationDictionaryCachedStructures cachedStructures = WeldUtils
          .getInstanceFromStaticBeanManager(ApplicationDictionaryCachedStructures.class);
      ComboTableData comboTableData = cachedStructures.getComboTableData(field);
      Map<String, String> newParameters = null;

      newParameters = comboTableData.fillSQLParametersIntoMap(new DalConnectionProvider(false),
          vars, new FieldProviderFactory(parameters), windowId, null);

      if (parameters.get("_currentValue") != null && StringUtils.isEmpty(filterString)) {
        newParameters.put("@ACTUAL_VALUE@", parameters.get("_currentValue"));
      }

      if (!StringUtils.isEmpty(filterString)) {
        newParameters.put("FILTER_VALUE", filterString);
      }

      boolean optionalFieldNonFirstPage = !column.isMandatory() && startRow > 0
          && StringUtils.isEmpty(filterString);
      // non-mandatory fields add a blank record at the beginning of 1st page, this needs to be
      // taken into account in subsequent pages

      fps = comboTableData.select(new DalConnectionProvider(false), newParameters, true,
          startRow - (optionalFieldNonFirstPage ? 1 : 0),
          endRow - (optionalFieldNonFirstPage ? 1 : 0));

      ArrayList<JSONObject> comboEntries = new ArrayList<JSONObject>();
      // If column is mandatory we add an initial blank value in the first page if not filtered
      if (!column.isMandatory() && startRow == 0 && StringUtils.isEmpty(filterString)) {
        JSONObject entry = new JSONObject();
        entry.put(JsonConstants.ID, (String) null);
        entry.put(JsonConstants.IDENTIFIER, (String) null);
        comboEntries.add(entry);
        // if we are fetching 76 records,with the additional null entry to it becomes 77.
        // so increasing it by 3 to force additional fetch
        num = (endRow + 3);
      } else {
        num = (endRow + 2);
      }
      for (FieldProvider fp : fps) {
        JSONObject entry = new JSONObject();
        entry.put(JsonConstants.ID, fp.getField("ID"));
        entry.put(JsonConstants.IDENTIFIER, fp.getField("NAME"));
        comboEntries.add(entry);
      }

      // now jsonfy the data
      try {
        final JSONObject jsonResult = new JSONObject();
        final JSONObject jsonResponse = new JSONObject();
        jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
        jsonResponse.put(JsonConstants.RESPONSE_STARTROW, startRow);
        jsonResponse.put(JsonConstants.RESPONSE_ENDROW, comboEntries.size() + startRow - 1);

        if ((endRow - startRow) > comboEntries.size()) {
          num = startRow + comboEntries.size();
        }
        jsonResponse.put(JsonConstants.RESPONSE_TOTALROWS, num);
        jsonResponse.put(JsonConstants.RESPONSE_DATA, new JSONArray(comboEntries));
        jsonResult.put(JsonConstants.RESPONSE_RESPONSE, jsonResponse);

        return jsonResult.toString();
      } catch (JSONException e) {
        throw new OBException(e);
      }
    } catch (Exception e) {
      log.error("Error in DS for combos - Field: " + fieldId + " - Filter: " + filterString, e);
      throw new OBException(e);
    } finally {
      log.debug("fetch ComboTableDatasourceService took: {} ms. Field: {}, filter: {}",
          new Object[] { (System.currentTimeMillis() - init), fieldId, filterString });
      OBContext.restorePreviousMode();
    }
  }

  @Override
  public String remove(Map<String, String> parameters) {
    throw new OBException("Method not implemented");
  }

  @Override
  public String add(Map<String, String> parameters, String content) {
    throw new OBException("Method not implemented");
  }

  @Override
  public String update(Map<String, String> parameters, String content) {
    throw new OBException("Method not implemented");
  }

  @Override
  public void checkFetchDatasourceAccess(Map<String, String> parameters) {
    Field field = null;
    Column column = null;
    String fieldId = parameters.get("fieldId");
    Entity targetEntity = null;

    OBContext.setAdminMode(true);
    try {
      // check access to current entity
      field = OBDal.getInstance().get(Field.class, fieldId);
      column = field.getColumn();
      targetEntity = ModelProvider.getInstance().getEntityByTableId(column.getTable().getId());
      OBContext.getOBContext().getEntityAccessChecker().checkReadableAccess(targetEntity);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  public void checkEditDatasourceAccess(Map<String, String> parameters) {
    throw new OBException("Method not implemented");
  }
}
