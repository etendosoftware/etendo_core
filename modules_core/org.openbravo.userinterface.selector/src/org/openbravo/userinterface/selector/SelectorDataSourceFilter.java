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
 * All portions are Copyright (C) 2010-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.userinterface.selector;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.application.CachedPreference;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.client.kernel.reference.StringUIDefinition;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.domain.Validation;
import org.openbravo.service.datasource.DataSourceFilter;
import org.openbravo.service.json.AdvancedQueryBuilder.TextMatching;
import org.openbravo.service.json.DefaultJsonDataService;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;

/**
 * Implements the a datasource filter request for the selectors. Used to generates Hibernate where
 * clauses based on dynamic expressions (JavaScript)
 * 
 * @author iperdomo
 */
public class SelectorDataSourceFilter implements DataSourceFilter {

  private static Logger log = LogManager.getLogger();
  private TextMatching textMatching = TextMatching.exact;

  @Inject
  private CachedPreference cachedPreference;

  public SelectorDataSourceFilter() {
  }

  @Override
  public void doFilter(Map<String, String> parameters, HttpServletRequest request) {
    final long t1 = System.currentTimeMillis();

    try {
      OBContext.setAdminMode();

      String selectorId = parameters.get(SelectorConstants.DS_REQUEST_SELECTOR_ID_PARAMETER);
      String requestType = parameters.get(SelectorConstants.DS_REQUEST_TYPE_PARAMETER);

      if (selectorId == null || selectorId.equals("")) {
        return;
      }

      Selector sel = OBDal.getInstance().get(Selector.class, selectorId);

      String filterExpression = sel.getFilterExpression() == null ? "" : sel.getFilterExpression();
      String filterHQL = "";

      filterHQL = applyFilterExpression(filterExpression, sel, parameters, request);

      String processId = parameters.get(SelectorConstants.DS_REQUEST_PROCESS_DEFINITION_ID);
      if (!StringUtils.isEmpty(processId)) {
        Parameter param = OBDal.getInstance()
            .get(Parameter.class, parameters.get(SelectorConstants.DS_REQUEST_SELECTOR_FIELD_ID));
        Validation validation = param.getValidation();
        if (validation != null) {
          if (validation.getType().equals("HQL_JS")) {
            String validationCode = validation.getValidationCode();
            String validationHQL = applyFilterExpression(validationCode, sel, parameters, request);

            if (!StringUtils.isEmpty(validationHQL)) {
              if (StringUtils.isEmpty(filterHQL)) {
                filterHQL = validationHQL;
              } else {
                filterHQL = "(" + filterHQL + ") and (" + validationHQL + ")";
              }
            }
          } else {
            log.error("Unsupported validation type '" + validation.getType() + "' in "
                + param.getObuiappProcess().getName() + "->" + param.getName()
                + ". Only 'HQL_JS' type is supported. No validation is applied!!!");
          }
        }
      }

      if (whereParameterIsNotBlank(parameters)) {
        if (manualWhereClausePreferenceIsEnabled()) {
          parameters.put(JsonConstants.WHERE_AND_FILTER_CLAUSE,
              parameters.get(JsonConstants.WHERE_PARAMETER));
          String warnMsg = OBMessageUtils.getI18NMessage("WhereParameterAppliedWarning", null);
          log.warn(warnMsg + " Parameters: "
              + DefaultJsonDataService.convertParameterToString(parameters));
        } else {
          String errorMsg = OBMessageUtils.getI18NMessage("WhereParameterException", null);
          log.error(errorMsg + " Parameters: "
              + DefaultJsonDataService.convertParameterToString(parameters));
          throw new OBSecurityException(errorMsg, false);
        }
      } else {
        String currentWhere = "";
        if (!StringUtils.isEmpty(filterHQL)) {
          log.debug("Adding to where clause (based on filter expression): " + filterHQL);
          currentWhere = sel.getHQLWhereClause();
          if (StringUtils.isBlank(currentWhere)) {
            parameters.put(JsonConstants.WHERE_AND_FILTER_CLAUSE, filterHQL);
          } else {
            parameters.put(JsonConstants.WHERE_AND_FILTER_CLAUSE,
                currentWhere + " and " + filterHQL);
          }
        } else {
          currentWhere = sel.getHQLWhereClause();
          if (StringUtils.isNotBlank(currentWhere)) {
            parameters.put(JsonConstants.WHERE_AND_FILTER_CLAUSE, currentWhere);
          }
        }
      }

      parameters.put(JsonConstants.WHERE_CLAUSE_HAS_BEEN_CHECKED, "true");

      // Applying default expression for selector fields when is not a selector window request
      if (!"Window".equals(requestType)) {
        OBCriteria<SelectorField> sfc = OBDal.getInstance().createCriteria(SelectorField.class);
        sfc.add(Restrictions.isNotNull(SelectorField.PROPERTY_DEFAULTEXPRESSION));
        sfc.add(Restrictions.eq(SelectorField.PROPERTY_OBUISELSELECTOR, sel));

        applyDefaultExpressions(sel, parameters, sfc, request, filterHQL);
        verifyPropertyTypes(sel, parameters);
      }

    } catch (OBSecurityException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error executing filter: " + e.getMessage(), e);
    } finally {
      OBContext.restorePreviousMode();
      log.debug("doFilter took: " + (System.currentTimeMillis() - t1) + "ms");
    }
  }

  /**
   * This method verifies that in the parameters, there are not numeric or date values. In case that
   * it finds numeric or date parameter, these are deleted
   * 
   * @author jecharri
   */
  private void verifyPropertyTypes(Selector sel, Map<String, String> parameters) {
    String value = parameters.get("criteria");
    if (value == null) {
      return;
    }
    boolean isCustomQuerySelector = sel.getHQL() != null;
    String filteredCriteria = "";
    String fieldName;
    if (sel.getTable() == null) {
      // don't do verifications on selectors not based on tables
      return;
    }
    Entity entity = ModelProvider.getInstance()
        .getEntityByTableName(sel.getTable().getDBTableName());
    Entity cEntity = null;
    try {
      OBContext.setAdminMode(true);
      if (value.contains(JsonConstants.IN_PARAMETER_SEPARATOR)) {
        final String[] separatedValues = value.split(JsonConstants.IN_PARAMETER_SEPARATOR);
        for (String separatedValue : separatedValues) {
          cEntity = entity;
          JSONObject jSONObject = new JSONObject(separatedValue);
          fieldName = (String) jSONObject.get("fieldName");
          if (fieldName.contains("_dummy") || fieldName.contains(JsonConstants.IDENTIFIER)
              || fieldName.contains("searchKey")) {
            filteredCriteria += jSONObject.toString() + JsonConstants.IN_PARAMETER_SEPARATOR;
            continue;
          }
          boolean filterParameter = false;
          if (isCustomQuerySelector) {
            // This is a custom query selector. We cannot filter parameters by linking them to
            // entity properties
            // Instead, we will do it by checking the references of the fields
            for (SelectorField field : sel.getOBUISELSelectorFieldList()) {
              if (field.isSearchinsuggestionbox()) {
                if (field.getDisplayColumnAlias().equals(fieldName)) {
                  UIDefinition uiDef = UIDefinitionController.getInstance()
                      .getUIDefinition(field.getReference());
                  if (!(uiDef instanceof StringUIDefinition)) {
                    filterParameter = true;
                  }
                }
              }
            }
          } else {
            // A property in the entity is searched for this fieldName
            // If the property is numeric or date, then it is filtered
            String[] fieldNameSplit;
            if (fieldName.contains(DalUtil.FIELDSEPARATOR)) {
              fieldNameSplit = fieldName.split("\\" + DalUtil.FIELDSEPARATOR);
            } else {
              fieldNameSplit = fieldName.split("\\" + DalUtil.DOT);
            }
            Property fProp = null;
            if (fieldNameSplit.length == 1) {
              if (entity.hasProperty(fieldName)) {
                fProp = entity.getProperty(fieldName);
              } else {
                continue;
              }
            } else {
              for (int i = 0; i < fieldNameSplit.length; i++) {
                fProp = cEntity.getProperty(fieldNameSplit[i]);
                if (i != fieldNameSplit.length - 1) {
                  cEntity = fProp.getReferencedProperty().getEntity();
                }
              }
            }
            if (fProp.isNumericType() || fProp.isDate()) {
              filterParameter = true;
            }
          }
          if (filterParameter) {
            try {
              jSONObject.put("operator", "equals");
              BigDecimal valueJSONObject = new BigDecimal(jSONObject.get("value").toString());
              jSONObject.put("value", valueJSONObject);
              filteredCriteria += jSONObject.toString() + JsonConstants.IN_PARAMETER_SEPARATOR;
            } catch (Exception ex) {
              // do nothing
            }
          } else {
            filteredCriteria += jSONObject.toString() + JsonConstants.IN_PARAMETER_SEPARATOR;
          }
        }
        parameters.put("criteria", filteredCriteria.substring(0, (filteredCriteria.length() - 5)));
      }
    } catch (Exception ex) {
      log.error("Error converting to JSON object: " + ex.getMessage(), ex);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Evaluates the Selector filter expression and modifies the parameters map for data filtering
   */
  private String applyFilterExpression(String filterExpression, Selector sel,
      Map<String, String> parameters, HttpServletRequest request) {

    if (StringUtils.isEmpty(filterExpression)) {
      return "";
    }

    Object result = null;
    String dynamicWhere = "";

    try {
      result = ParameterUtils.getJSExpressionResult(parameters, request.getSession(),
          filterExpression);
      if (result != null && !result.toString().equals("")) {
        dynamicWhere = result.toString();
      }
    } catch (Exception e) {
      log.error("Error evaluating filter expression: " + filterExpression + " Selector id: "
          + sel.getId() + " " + e.getMessage(), e);
    }

    return dynamicWhere;
  }

  /**
   * Evaluates the default expressions and modifies the parameters map for data filtering
   */
  private void applyDefaultExpressions(Selector sel, Map<String, String> parameters,
      OBCriteria<SelectorField> sfc, HttpServletRequest request, String hqlFilterClause) {

    String currentWhere = "";
    List<SelectorField> selectorFields = sfc.list();
    if (selectorFields.size() == 0) {
      return;
    }

    Object result = null;
    StringBuffer sb = new StringBuffer();
    String textMatchingName = null;

    if (parameters.containsKey(JsonConstants.TEXTMATCH_PARAMETER_OVERRIDE)) {
      textMatchingName = parameters.get(JsonConstants.TEXTMATCH_PARAMETER_OVERRIDE);
    } else {
      textMatchingName = parameters.get(JsonConstants.TEXTMATCH_PARAMETER);
    }

    if (textMatchingName != null) {
      for (TextMatching txtMatching : TextMatching.values()) {
        if (txtMatching.name().equals(textMatchingName)) {
          textMatching = txtMatching;
          break;
        }
      }
    }

    Entity entity = ModelProvider.getInstance().getEntityByTableId(sel.getTable().getId());

    for (SelectorField sf : selectorFields) {
      // skip selector fields which do not have a property defined (needed for selector definitions
      // using a custom query
      if (sf.getProperty() == null) {
        continue;
      }

      // Skip values from the request
      if (parameters.get(sf.getProperty()) != null) {
        log.debug("Skipping the default value evaluation for property: " + sf.getProperty()
            + " - value from request: " + parameters.get(sf.getProperty()));
        continue;
      }

      final List<Property> properties = JsonUtils.getPropertiesOnPath(entity, sf.getProperty());

      if (properties.isEmpty()) {
        continue;
      }

      final Property property = properties.get(properties.size() - 1);

      try {
        result = ParameterUtils.getJSExpressionResult(parameters, request.getSession(),
            sf.getDefaultExpression());

        if (result == null || result.toString().equals("") || result.toString().equals("''")) {
          continue;
        }

        if (sb.length() > 0) {
          sb.append(" and ");
        }

        if (!property.isPrimitive()) {
          sb.append("e." + sf.getProperty() + ".id = '" + result.toString() + "'");
        } else if (String.class == property.getPrimitiveObjectType()) {
          if (textMatching == TextMatching.exact) {
            sb.append("e." + sf.getProperty() + " = '" + result.toString() + "'");
          } else if (textMatching == TextMatching.startsWith) {
            sb.append("upper(" + "e." + sf.getProperty() + ") like '"
                + result.toString().toUpperCase() + "%'");
          } else {
            sb.append("upper(" + "e." + sf.getProperty() + ") like '%"
                + result.toString().toUpperCase().replaceAll(" ", "%") + "%'");
          }
        } else if (Boolean.class == property.getPrimitiveObjectType() || property.isNumericType()) {
          sb.append("e." + sf.getProperty() + " = " + result.toString());
        } else if (Date.class.isAssignableFrom(property.getPrimitiveObjectType())) {
          try {
            final Calendar cal = Calendar.getInstance();
            cal.setTime(UIDefinitionController.DATE_UI_DEFINITION.parse(result.toString()));
            sb.append("(day(" + "e." + sf.getProperty() + ") = " + cal.get(Calendar.DATE)
                + " and month(" + "e." + sf.getProperty() + ") = " + (cal.get(Calendar.MONTH) + 1)
                + " and year(" + "e." + sf.getProperty() + ") = " + cal.get(Calendar.YEAR) + ")");
          } catch (Exception e) {
            log.error("Error trying to parse date for property " + sf.getProperty(), e);
          }

        }
      } catch (Exception e) {
        log.error("Error evaluating filter expression: " + sf.getDefaultExpression(), e);
      }
    }

    if (sb.length() == 0) {
      return;
    }

    log.debug("Adding to where clause (based on fields default expression): " + sb.toString());

    if (whereParameterIsNotBlank(parameters)) {
      if (manualWhereClausePreferenceIsEnabled()) {
        parameters.put(JsonConstants.WHERE_AND_FILTER_CLAUSE,
            parameters.get(JsonConstants.WHERE_PARAMETER));
      } else {
        String errorMsg = OBMessageUtils.getI18NMessage("WhereParameterException", null);
        log.error(errorMsg + " Parameters: "
            + DefaultJsonDataService.convertParameterToString(parameters));
        throw new OBSecurityException(errorMsg);
      }
    } else {
      if (StringUtils.isNotBlank(hqlFilterClause)) {
        currentWhere = parameters.get(JsonConstants.WHERE_AND_FILTER_CLAUSE);
      } else {
        currentWhere = sel.getHQLWhereClause();
      }
      if (currentWhere == null || currentWhere.equals("null") || currentWhere.equals("")) {
        parameters.put(JsonConstants.WHERE_AND_FILTER_CLAUSE, sb.toString());
      } else {
        parameters.put(JsonConstants.WHERE_AND_FILTER_CLAUSE,
            currentWhere + " and " + sb.toString());
      }
    }
  }

  private boolean manualWhereClausePreferenceIsEnabled() {
    return Preferences.YES
        .equals(cachedPreference.getPreferenceValue(CachedPreference.ALLOW_WHERE_PARAMETER));
  }

  private boolean whereParameterIsNotBlank(Map<String, String> parameters) {
    return parameters.containsKey(JsonConstants.WHERE_PARAMETER)
        && StringUtils.isNotBlank(parameters.get(JsonConstants.WHERE_PARAMETER))
        && !"null".equals(parameters.get(JsonConstants.WHERE_PARAMETER));
  }
}
