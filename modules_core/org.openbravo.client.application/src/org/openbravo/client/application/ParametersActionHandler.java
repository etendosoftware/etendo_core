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
 * All portions are Copyright (C) 2010-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.portal.PortalAccessible;

/**
 * Responsible of retrieving/storing Parameter values
 * 
 * @author iperdomo
 */
@ApplicationScoped
public class ParametersActionHandler extends BaseActionHandler implements PortalAccessible {
  private static Logger log = LogManager.getLogger();

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    try {
      final JSONObject data = new JSONObject(content);
      final String action = data.getString("action");
      if (action == null || action.equals("")) {
        log.warn("No action in the request, nothing to process");
        return data;
      }
      if (action.equals("SAVE")) {
        return doSave(parameters, content);
      }
    } catch (Exception e) {
      log.error("Error trying to process action: " + e.getMessage(), e);
    }
    return new JSONObject();
  }

  private JSONObject doSave(Map<String, Object> parameters, String content) {
    JSONObject result, data;
    try {

      OBContext.setAdminMode();

      data = new JSONObject(content);
      result = new JSONObject(content);

      final String entityName = data.getString("entityName");
      final String dbInstanceId = data.getString("dbInstanceId");
      final String dbFilterProperty = data.getString("dbFilterProperty");
      final JSONArray params = data.getJSONArray("parameters");

      if (entityName == null || entityName.equals("")) {
        result.put("message",
            getMessge("Error", "Can't process parameters action without entity name"));
        return result;
      }

      if (dbInstanceId == null || dbInstanceId.equals("")) {
        result.put("message",
            getMessge("Error", "Can't process parameters without object instance id"));
        return result;
      }

      if (dbFilterProperty == null || dbFilterProperty.equals("")) {
        result.put("Error", getMessge("Error", "Can't process parameters without filter property"));
        return result;
      }

      if (params == null || params.length() == 0) {
        result.put("message",
            getMessge("Error", "Parameters not found in request, nothing to process"));
        return result;
      }

      try {
        ParameterValue.class.getDeclaredField("PROPERTY_" + dbFilterProperty.toUpperCase());
      } catch (NoSuchFieldException fieldException) {
        result.put("message", getMessge("Error",
            "Property " + dbFilterProperty + " is not defined in Parameters class"));
        return result;
      }

      BaseOBObject filterObject = OBDal.getInstance().getProxy(entityName, dbInstanceId);
      for (int i = 0; i < params.length(); i++) {
        final JSONObject p = params.getJSONObject(i);
        final Parameter param = OBDal.getInstance()
            .getProxy(Parameter.class, p.getString("parameterId"));

        ParameterValue value = (ParameterValue) OBDal.getInstance()
            .createCriteria(ParameterValue.class)
            .add(Restrictions.eq(dbFilterProperty, filterObject))
            .add(Restrictions.eq(ParameterValue.PROPERTY_PARAMETER, param))
            .setMaxResults(1)
            .uniqueResult();

        if (value == null) {
          value = OBProvider.getInstance().get(ParameterValue.class);
          value.setParameter(param);
          value.set(dbFilterProperty, filterObject);
        }

        ParameterUtils.setParameterValue(value, p);
        OBDal.getInstance().save(value);
      }
      OBDal.getInstance().flush();
      result.put("message", getMessge("Success", "Process completed successfully"));
      return result;
    } catch (Exception e) {
      log.error("Error processing Parameters action: {}", e.getMessage(), e);
      return new JSONObject();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private Map<String, String> getMessge(String type, String message) {
    OBError m = new OBError();
    m.setTitle("");
    m.setType(type);
    m.setMessage(message);
    if (type.equalsIgnoreCase("Error")) {
      log.error(message);
    }
    return m.toMap();
  }
}
