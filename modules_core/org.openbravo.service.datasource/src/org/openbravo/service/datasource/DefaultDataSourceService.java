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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.domaintype.DateDomainType;
import org.openbravo.base.model.domaintype.DatetimeDomainType;
import org.openbravo.base.model.domaintype.EnumerateDomainType;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.CachedPreference;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;
import org.openbravo.service.json.DefaultJsonDataService;
import org.openbravo.service.json.DefaultJsonDataService.QueryResultWriter;
import org.openbravo.service.json.JsonConstants;

/**
 * The default implementation of the {@link DataSourceService}. Supports data retrieval, update
 * operations as well as creation of the datasource in javascript.
 * 
 * Makes extensive use of the {@link DefaultJsonDataService}. Check the javadoc on that class for
 * more information.
 * 
 * @author mtaal
 */
public class DefaultDataSourceService extends BaseDataSourceService {
  private static final Logger log4j = LogManager.getLogger();

  @Inject
  private CachedPreference cachedPreference;

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.service.datasource.DataSource#fetch(java.util.Map)
   */
  @Override
  public String fetch(Map<String, String> parameters) {
    return fetch(parameters, true);
  }

  protected String fetch(Map<String, String> parameters,
      boolean shouldFilterOnRedeableOrganizations) {
    OBContext.setAdminMode(shouldFilterOnRedeableOrganizations);
    try {
      addFetchParameters(parameters);
      return DefaultJsonDataService.getInstance()
          .fetch(parameters, shouldFilterOnRedeableOrganizations);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void fetch(Map<String, String> parameters, QueryResultWriter writer) {
    OBContext.setAdminMode(true);
    try {
      addFetchParameters(parameters);
      DefaultJsonDataService.getInstance().fetch(parameters, writer);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Adds some extra parameters that will be used to fetch data.
   */
  protected void addFetchParameters(Map<String, String> parameters) {
    if (getEntity() != null) {
      parameters.put(JsonConstants.ENTITYNAME, getEntity().getName());
    }

    if (!"true".equals(parameters.get(JsonConstants.WHERE_CLAUSE_HAS_BEEN_CHECKED))) {
      if (whereParameterIsNotBlank(parameters)) {
        if (manualWhereClausePreferenceIsEnabled()) {
          parameters.put(JsonConstants.WHERE_AND_FILTER_CLAUSE,
              parameters.get(JsonConstants.WHERE_PARAMETER));
          String warnMsg = OBMessageUtils.getI18NMessage("WhereParameterAppliedWarning", null);
          log4j.warn(warnMsg + "Parameters: "
              + DefaultJsonDataService.convertParameterToString(parameters));
        } else {
          String errorMsg = OBMessageUtils.getI18NMessage("WhereParameterException", null);
          log4j.error(errorMsg + " Parameters: "
              + DefaultJsonDataService.convertParameterToString(parameters));
          throw new OBSecurityException(errorMsg, false);
        }
      } else {
        String whereAndFilterClause = getWhereAndFilterClause(parameters);
        if (StringUtils.isNotBlank(whereAndFilterClause)) {
          if (getWhereClause() != null) {
            parameters.put(JsonConstants.WHERE_AND_FILTER_CLAUSE,
                "(" + whereAndFilterClause + ") and (" + getWhereClause() + ")");
          } else {
            parameters.put(JsonConstants.WHERE_AND_FILTER_CLAUSE, whereAndFilterClause);
          }
        } else {
          if (getWhereClause() != null) {
            parameters.put(JsonConstants.WHERE_AND_FILTER_CLAUSE, getWhereClause());
          }
        }
      }
    }

    // add a filter on the parent of the entity
    if ((parameters.get(JsonConstants.FILTERBYPARENTPROPERTY_PARAMETER) != null
        && !"null".equals(parameters.get(JsonConstants.FILTERBYPARENTPROPERTY_PARAMETER)))
        && parameters.containsKey(JsonConstants.TARGETRECORDID_PARAMETER)) {
      final String parentProperty = parameters.get(JsonConstants.FILTERBYPARENTPROPERTY_PARAMETER);
      final BaseOBObject bob = OBDal.getInstance()
          .get(getEntity().getName(), parameters.get(JsonConstants.TARGETRECORDID_PARAMETER));

      // a special case, a child tab actually displays the parent record
      // but a different set of information of that record
      final String parentId;
      if (bob.getId().equals(bob.get(parentProperty))) {
        parentId = (String) bob.getId();
      } else {
        parentId = (String) ((BaseOBObject) bob.get(parentProperty)).getId();
      }

      final String whereClause;
      if (StringUtils.isNotBlank(parameters.get(JsonConstants.WHERE_AND_FILTER_CLAUSE))
          && !parameters.get(JsonConstants.WHERE_AND_FILTER_CLAUSE).equals("null")) {
        whereClause = parameters.get(JsonConstants.WHERE_AND_FILTER_CLAUSE) + " and (";
      } else {
        whereClause = " (";
      }
      parameters.put(JsonConstants.WHERE_AND_FILTER_CLAUSE, whereClause + JsonConstants.MAIN_ALIAS
          + "." + parentProperty + ".id='" + parentId + "')");
    }
    parameters.put(JsonConstants.USE_ALIAS, "true");
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

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.service.datasource.DataSource#remove(java.util.Map)
   */
  @Override
  public String remove(Map<String, String> parameters) {
    return remove(parameters, true);
  }

  protected String remove(Map<String, String> parameters,
      boolean shouldFilterOnRedeableOrganizations) {
    OBContext.setAdminMode(shouldFilterOnRedeableOrganizations);
    try {
      parameters.put(JsonConstants.ENTITYNAME, getEntity().getName());
      return DefaultJsonDataService.getInstance().remove(parameters);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.service.datasource.DataSource#add(java.util.Map, java.lang.String)
   */
  @Override
  public String add(Map<String, String> parameters, String content) {
    return add(parameters, content, true);
  }

  protected String add(Map<String, String> parameters, String content,
      boolean shouldFilterOnRedeableOrganizations) {
    OBContext.setAdminMode(shouldFilterOnRedeableOrganizations);
    try {
      parameters.put(JsonConstants.ENTITYNAME, getEntity().getName());
      testAccessPermissions(parameters, content);
      return DefaultJsonDataService.getInstance().add(parameters, content);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.service.datasource.DataSource#update(java.util.Map, java.lang.String)
   */
  @Override
  public String update(Map<String, String> parameters, String content) {
    OBContext.setAdminMode(true);
    try {
      parameters.put(JsonConstants.ENTITYNAME, getEntity().getName());
      testAccessPermissions(parameters, content);
      return DefaultJsonDataService.getInstance().update(parameters, content);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void testAccessPermissions(Map<String, String> parameters, String content) {
    try {
      if (parameters.get("tabId") == null) {
        return;
      }
      final Tab tab = OBDal.getInstance().get(Tab.class, parameters.get("tabId"));
      if (tab == null) {
        return;
      }
      final String roleId = OBContext.getOBContext().getRole().getId();

      final JSONObject jsonObject = new JSONObject(content);
      if (content == null) {
        return;
      }

      final JSONObject data = jsonObject.getJSONObject("data");
      String id = null;
      if (data.has(JsonConstants.ID)) {
        id = data.getString(JsonConstants.ID);
      }
      // if there is a new indicator then nullify the id again to treat the object has new
      final boolean isNew = data.has(JsonConstants.NEW_INDICATOR)
          && data.getBoolean(JsonConstants.NEW_INDICATOR);
      if (isNew) {
        id = null;
      }

      String entityName = null;
      if (!data.has(JsonConstants.ENTITYNAME) && parameters.containsKey(JsonConstants.ENTITYNAME)) {
        data.put(JsonConstants.ENTITYNAME, parameters.get(JsonConstants.ENTITYNAME));
      }
      if (data.has(JsonConstants.ENTITYNAME)) {
        entityName = data.getString(JsonConstants.ENTITYNAME);
      }
      if (entityName == null) {
        throw new IllegalArgumentException("Entity name not defined in jsonobject " + data);
      }
      final DataToJsonConverter toJsonConverter = OBProvider.getInstance()
          .get(DataToJsonConverter.class);
      final BaseOBObject oldDataObject = id == null ? null
          : OBDal.getInstance().get(entityName, id);
      final JSONObject oldData = oldDataObject == null ? null
          : toJsonConverter.toJsonObject(oldDataObject, DataResolvingMode.FULL);
      final OBQuery<Field> fieldQuery = OBDal.getInstance()
          .createQuery(Field.class, "as f where f.tab.id = :tabId"
              + " and (exists (from f.aDFieldAccessList fa where fa.tabAccess.windowAccess.role.id = :roleId and fa.editableField = false and fa.active = true and fa.ischeckonsave = true)"
              + "      or (not exists (from f.aDFieldAccessList fa where fa.tabAccess.windowAccess.role.id = :roleId and fa.active = true)"
              + "          and exists (from f.tab.aDTabAccessList ta where ta.windowAccess.role.id = :roleId and ta.editableField = false and ta.active = true)"
              + "          or not exists (from f.tab.aDTabAccessList  ta where  ta.windowAccess.role.id = :roleId and ta.active = true)"
              + "          and exists (from ADWindowAccess wa where f.tab.window = wa.window and wa.role.id = :roleId and wa.editableField = false and wa.active = true)))");
      fieldQuery.setNamedParameter("tabId", tab.getId());
      fieldQuery.setNamedParameter("roleId", roleId);
      for (Field f : fieldQuery.list()) {
        Property property = KernelUtils.getProperty(f);
        if (property.isAuditInfo()) {
          continue;
        }
        String key = property.getName();
        if (data.has(key)) {
          String newValue = getValue(data, key);
          String oldValue = getValue(oldData, key);
          if (property.isPrimitive() && property.isNumericType()
              && isSameNumericValue(newValue, oldValue)) {
            continue;
          }
          if (oldValue == null && newValue != null
              || oldValue != null && !oldValue.equals(newValue)) {
            throw new RuntimeException(KernelUtils.getInstance()
                .getI18N("OBSERDS_RoleHasNoFieldAccess",
                    new String[] { OBContext.getOBContext().getRole().getName(), f.getName() }));
          }
        }
      }

    } catch (JSONException e) {
      log4j.error("Unable to test access", e);
      throw new RuntimeException("Unable to test access", e);
    }
  }

  private static final String getValue(JSONObject jsonObject, String prop) throws JSONException {
    if (jsonObject == null) {
      return null;
    }
    if (!jsonObject.has(prop)) {
      return null;
    }
    Object val = jsonObject.get(prop);
    if (JSONObject.NULL.equals(val) || val == null
        || val instanceof String && ((String) val).trim().equals("")) {
      return null;
    } else {
      return val.toString();
    }
  }

  private static boolean isSameNumericValue(String str1, String str2) {
    try {
      if (str1 == null && str2 == null) {
        return true;
      }
      if (str1 == null && str2 != null || str1 != null && str2 == null) {
        return false;
      }
      BigDecimal bd1 = new BigDecimal(str1);
      BigDecimal bd2 = new BigDecimal(str2);
      return bd1.doubleValue() == bd2.doubleValue();
    } catch (NumberFormatException nfex) {
      log4j.error("Could not compare numeric values", nfex);
    }
    return false;
  }

  @Override
  public List<DataSourceProperty> getDataSourceProperties(Map<String, Object> parameters) {
    final Entity entity = getEntity();
    final List<DataSourceProperty> dsProperties;
    if (entity == null) {
      dsProperties = super.getDataSourceProperties(parameters);
    } else {
      dsProperties = getInitialProperties(entity, false);
    }

    // now see if there are additional properties, these are often property paths
    final String additionalPropParameter = (String) parameters
        .get(JsonConstants.ADDITIONAL_PROPERTIES_PARAMETER);
    final StringBuilder additionalProperties = new StringBuilder();
    if (additionalPropParameter != null) {
      additionalProperties.append(additionalPropParameter);
    }

    // get the additionalproperties from the properties
    for (DataSourceProperty dsProp : dsProperties) {
      final Map<String, Object> params = dsProp.getUIDefinition().getDataSourceParameters();
      String additionalProps = (String) params.get(JsonConstants.ADDITIONAL_PROPERTIES_PARAMETER);
      if (additionalProps != null) {
        final String[] additionalPropValues = additionalProps.toString().split(",");
        for (String addProp : additionalPropValues) {
          if (additionalProperties.length() > 0) {
            additionalProperties.append(",");
          }
          additionalProperties.append(dsProp.getName().replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR)
              + DalUtil.FIELDSEPARATOR + addProp.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR));
        }
      }
    }

    if (additionalProperties.length() > 0 && getEntity() != null) {
      final String[] additionalProps = additionalProperties.toString().split(",");

      // the additional properties are passed back using a different name
      // than the original property
      for (String additionalProp : additionalProps) {
        final Property property = DalUtil.getPropertyFromPath(entity, additionalProp);
        if (property == null) {
          log4j.info("Couldn't find property from additional property " + additionalProp
              + " in entity " + entity);
          continue;
        }
        final DataSourceProperty dsProperty = DataSourceProperty.createFromProperty(property);
        dsProperty.setAdditional(true);
        dsProperty.setName(additionalProp.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR));
        dsProperties.add(dsProperty);
      }
    }
    return dsProperties;
  }

  protected List<DataSourceProperty> getInitialProperties(Entity entity,
      boolean minimalProperties) {
    if (entity == null) {
      return Collections.emptyList();
    }

    final List<DataSourceProperty> result = new ArrayList<DataSourceProperty>();
    for (Property prop : entity.getProperties()) {
      if (prop.isOneToMany() || prop.isProxy()) {
        continue;
      }

      // if minimal then only generate date properties
      // and the id itself
      if (!prop.isId() && minimalProperties
          && !(prop.getDomainType() instanceof EnumerateDomainType)
          && !(prop.getDomainType() instanceof DateDomainType
              || prop.getDomainType() instanceof DatetimeDomainType)) {
        continue;
      }

      result.add(DataSourceProperty.createFromProperty(prop));
    }
    return result;
  }
}
