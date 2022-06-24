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
 * All portions are Copyright (C) 2010-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.userinterface.selector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.expression.OBScriptEngine;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.OBBindings;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;

/**
 * 
 * @author iperdomo
 */
@ApplicationScoped
public class SelectorDefaultFilterActionHandler extends BaseActionHandler {
  private Logger log = LogManager.getLogger();

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject result = new JSONObject();

    Map<String, String> params = getParameterMap(parameters);
    addParametersFromRequestContent(params, content);

    OBContext.setAdminMode();

    try {
      if (!params.containsKey(SelectorConstants.DS_REQUEST_SELECTOR_ID_PARAMETER)) {
        return result;
      }

      String selectorId = params.get(SelectorConstants.DS_REQUEST_SELECTOR_ID_PARAMETER);

      Selector sel = OBDal.getInstance().get(Selector.class, selectorId);
      final Table table;
      // Some selectors have a definition that do not use a table but a datasource
      if (sel.getTable() != null) {
        table = sel.getTable();
      } else if (sel.getObserdsDatasource() != null
          && sel.getObserdsDatasource().getTable() != null) {
        table = sel.getObserdsDatasource().getTable();
      } else {
        // no table, don't do anything
        return result;
      }
      final String entityName = table.getName();
      final Entity entity = ModelProvider.getInstance().getEntity(entityName);

      OBCriteria<SelectorField> obc = OBDal.getInstance().createCriteria(SelectorField.class);
      obc.add(Restrictions.eq(SelectorField.PROPERTY_OBUISELSELECTOR, sel));
      obc.add(Restrictions.isNotNull(SelectorField.PROPERTY_DEFAULTEXPRESSION));

      List<SelectorField> selFields = obc.list();
      if (selFields.isEmpty()) {
        return result;
      }

      Map<String, Object> bindings = new HashMap<>();

      bindings.put("OB", new OBBindings(OBContext.getOBContext(), params,
          (HttpSession) parameters.get(KernelConstants.HTTP_SESSION)));

      boolean isFilterByIdSupported = params
          .containsKey(SelectorConstants.DS_REQUEST_IS_FILTER_BY_ID_SUPPORTED)
          && "true".equals(params.get(SelectorConstants.DS_REQUEST_IS_FILTER_BY_ID_SUPPORTED));
      Object exprResult = null;
      JSONArray idFilters = new JSONArray();
      for (SelectorField f : selFields) {
        try {
          exprResult = OBScriptEngine.getInstance().eval(f.getDefaultExpression(), bindings);
          Object bobId = null;

          if (exprResult != null && !exprResult.equals("") && !exprResult.equals("''")) {
            Property property = null;
            if (f.getColumn() != null) {
              property = KernelUtils.getInstance().getPropertyFromColumn(f.getColumn());
            } else if (f.getProperty() != null) {
              property = DalUtil.getPropertyFromPath(entity, f.getProperty());
            }
            if (property != null && property.getTargetEntity() != null && !property.isOneToMany()) {
              final BaseOBObject bob = OBDal.getInstance()
                  .get(property.getTargetEntity().getName(), exprResult);
              bobId = exprResult;
              exprResult = bob.getIdentifier();
            }
          }

          if (sel.isCustomQuery()) {
            result.put(f.getDisplayColumnAlias().replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR),
                exprResult);
          } else if (exprResult != null && !exprResult.equals("") && !exprResult.equals("''")) {
            String fieldName = f.getProperty().replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR);
            if (bobId != null && isFilterByIdSupported) {
              idFilters.put(createJSONObjectFilter(fieldName, (String) bobId, (String) exprResult));
            } else {
              result.put(fieldName, exprResult);
            }
          }
        } catch (Exception e) {
          log.error("Error evaluating expression for property " + f.getProperty()
              + f.getDisplayColumnAlias() + ": " + e.getMessage(), e);
        }
      }

      if (idFilters.length() > 0) {
        result.put(SelectorConstants.PARAM_ID_FILTERS, idFilters);
      }
      // Obtaining the filter Expression from Selector. Refer issue
      // https://issues.openbravo.com/view.php?id=21541
      Object dynamicFilterExpression = null;
      if (sel.getFilterExpression() != null) {
        dynamicFilterExpression = OBScriptEngine.getInstance()
            .eval(sel.getFilterExpression(), bindings);
        result.put(SelectorConstants.PARAM_FILTER_EXPRESSION, dynamicFilterExpression.toString());
      }

    } catch (Exception e) {
      log.error("Error generating Default Filter action result: " + e.getMessage(), e);
    } finally {
      OBContext.restorePreviousMode();
    }

    return result;
  }

  private void addParametersFromRequestContent(Map<String, String> params, String content) {
    JSONObject jsonContent;
    try {
      jsonContent = new JSONObject(content);
      @SuppressWarnings("unchecked")
      Iterator<String> keys = jsonContent.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        String value = jsonContent.getString(key);
        params.put(key, value);
      }
    } catch (JSONException e) {
      log.error("Could not retrieve JSON from content: " + content);
      return;
    }
  }

  private JSONObject createJSONObjectFilter(String fieldName, String id, String identifier)
      throws JSONException {
    JSONObject jsonResult = new JSONObject();
    jsonResult.put(SelectorConstants.PARAM_FIELD_NAME, fieldName);
    jsonResult.put(SelectorConstants.PARAM_ID, id);
    jsonResult.put(SelectorConstants.PARAM_IDENTIFIER, identifier);
    return jsonResult;
  }

  private Map<String, String> getParameterMap(Map<String, Object> parameters) {
    Map<String, String> params = new HashMap<>();
    for (Entry<String, Object> entry : parameters.entrySet()) {
      String key = entry.getKey();
      if (key.equals(KernelConstants.HTTP_SESSION) || key.equals(KernelConstants.HTTP_REQUEST)) {
        continue;
      }
      params.put(key, (String) entry.getValue());
    }
    return params;
  }
}
