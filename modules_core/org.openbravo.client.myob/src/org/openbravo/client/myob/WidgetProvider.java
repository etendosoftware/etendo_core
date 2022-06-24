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
package org.openbravo.client.myob;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.domaintype.BooleanDomainType;
import org.openbravo.base.model.domaintype.DomainType;
import org.openbravo.base.model.domaintype.EnumerateDomainType;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.ParameterTrl;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.client.application.ParameterValue;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.reference.EnumUIDefinition;
import org.openbravo.client.kernel.reference.FKComboUIDefinition;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.domain.ReferencedTable;
import org.openbravo.model.ad.module.ModuleDBPrefix;
import org.openbravo.service.json.JsonConstants;

/**
 * Responsible for creating a widget definition.
 * 
 * NOTE: must be instantiated through the {@link MyOBUtils#getWidgetProvider(WidgetClass)} method.
 * 
 * @author mtaal
 */
public abstract class WidgetProvider {
  private static final Logger log = LogManager.getLogger();

  public static final String WIDGETCLASSID = "widgetClassId";
  public static final String TITLE = "title";
  private static final String COLNUM = "colNum";
  private static final String ROWNUM = "rowNum";
  private static final String HEIGHT = "height";
  private static final String PRIORITY = "priority";
  private static final String DESCRIPTION = "description";
  private static final String SUPERCLASSTITLE = "superclassTitle";
  private static final String DATAACCESSLEVEL = "dataAccessLevel";
  private static final String ENABLEDALLUSERS = "enabledAllUsers";
  private static final String AUTHORMSG = "authorMsg";
  private static final String AUTHORURL = "authorUrl";
  private static final String MODULENAME = "moduleName";
  private static final String MODULEVERSION = "moduleVersion";
  private static final String MODULESTATUS = "moduleStatus";
  private static final String MODULEJPACKAGE = "moduleJavaPackage";
  private static final String MODULETYPE = "moduleType";
  private static final String MODULEDBPREFIX = "moduleDBPrefix";
  private static final String MODULELICENSETYPE = "moduleLicenseType";
  private static final String MODULEUPDATEINFO = "moduleUpdateInfo";
  private static final String MODULELICENSETEXT = "moduleLicenseText";
  private static final String MODULEAUTHOR = "moduleAuthor";
  private static final String ABOUTFIELDDEFINITIONS = "aboutFieldDefinitions";
  protected static final String PARAMETERS = "parameters";
  protected static final String FIELDDEFINITIONS = "fieldDefinitions";
  private static final String VALUEMAP = "valueMap";
  private static final String PARAMETERID = "parameterId";
  private static final String PARAMETERNAME = "name";
  private static final String PARAMETERTITLE = "title";
  private static final String PARAMETERTYPE = "type";
  private static final String PARAMETERWIDTH = "width";
  private static final String PARAMETERCOLSPAN = "colSpan";
  private static final String PARAMETERFIELDPROPERTIES = "fieldProperties";
  private static final String PARAMETERREQUIRED = "required";
  private static final String DBINSTANCEID = "dbInstanceId";
  private static final String CAN_MAXIMIZE = "showMaximizeButton";
  private static final String MENU_ITEMS = "menuItems";
  private static final Long WIDGET_HEADER_HEIGHT = 35L;

  private Map<String, Object> widgetParameters = new HashMap<>();

  // note this is only set if the widgetprovider is created through the MyOBUtils class
  private JSONObject widgetClassDefinition;

  // prevent anyone else from creating a widgetprovider directly
  protected WidgetProvider() {
  }

  public JSONObject getWidgetClassDefinition() {
    return widgetClassDefinition;
  }

  private void setWidgetClassDefinition(WidgetClass widgetClass) {
    try {
      widgetClassDefinition = new JSONObject();
      widgetClassDefinition.put(WIDGETCLASSID, widgetClass.getId());
      widgetClassDefinition.put(MyOpenbravoWidgetComponent.CLASSNAMEPARAMETER,
          getClientSideWidgetClassName());
      widgetClassDefinition.put(TITLE, MyOBUtils.getWidgetTitle(widgetClass));
      widgetClassDefinition.put(HEIGHT, widgetClass.getHeight() + WIDGET_HEADER_HEIGHT);
      widgetClassDefinition.put(MENU_ITEMS, MyOBUtils.getWidgetMenuItems(widgetClass));
      if (widgetClass.getWidgetSuperclass() != null) {
        widgetClassDefinition.put(CAN_MAXIMIZE, widgetClass.getWidgetSuperclass().isCanMaximize());
      } else {
        widgetClassDefinition.put(CAN_MAXIMIZE, widgetClass.isCanMaximize());
      }

      final JSONObject aboutFieldDefinitions = new JSONObject();
      aboutFieldDefinitions.put(MODULENAME, widgetClass.getModule().getName());
      aboutFieldDefinitions.put(MODULEVERSION, widgetClass.getModule().getVersion());
      aboutFieldDefinitions.put(MODULESTATUS, widgetClass.getModule().getStatus());
      aboutFieldDefinitions.put(MODULEJPACKAGE, widgetClass.getModule().getJavaPackage());
      aboutFieldDefinitions.put(MODULETYPE, widgetClass.getModule().getType());

      StringBuilder moduleDBPrefixList = new StringBuilder();
      for (ModuleDBPrefix moduleDBPrefix : widgetClass.getModule().getModuleDBPrefixList()) {
        moduleDBPrefixList.append(moduleDBPrefix.getName() + " ");
      }
      aboutFieldDefinitions.put(MODULEDBPREFIX, moduleDBPrefixList.toString());
      aboutFieldDefinitions.put(MODULELICENSETYPE, widgetClass.getModule().getLicenseType());
      aboutFieldDefinitions.put(MODULEUPDATEINFO,
          widgetClass.getModule().getUpdateInformation() == null ? ""
              : widgetClass.getModule().getUpdateInformation());
      aboutFieldDefinitions.put(MODULELICENSETEXT,
          widgetClass.getModule().getLicenseText() == null ? ""
              : widgetClass.getModule().getLicenseText());
      aboutFieldDefinitions.put(MODULEAUTHOR,
          widgetClass.getModule().getAuthor() == null ? "" : widgetClass.getModule().getAuthor());
      aboutFieldDefinitions.put(TITLE, MyOBUtils.getWidgetTitle(widgetClass));
      aboutFieldDefinitions.put(DESCRIPTION,
          widgetClass.getDescription() == null ? "" : widgetClass.getDescription());
      aboutFieldDefinitions.put(SUPERCLASSTITLE, widgetClass.getWidgetSuperclass() == null ? ""
          : MyOBUtils.getWidgetTitle(widgetClass.getWidgetSuperclass()));
      aboutFieldDefinitions.put(DATAACCESSLEVEL, widgetClass.getDataAccessLevel());
      aboutFieldDefinitions.put(ENABLEDALLUSERS, widgetClass.isAllowAnonymousAccess());
      aboutFieldDefinitions.put(AUTHORMSG,
          widgetClass.getAuthorMsg() == null ? "" : widgetClass.getAuthorMsg());
      aboutFieldDefinitions.put(AUTHORURL,
          widgetClass.getAuthorUrl() == null ? "" : widgetClass.getAuthorUrl());

      final JSONObject defaultParameters = new JSONObject();
      final List<JSONObject> fieldDefinitions = new ArrayList<>();
      for (Parameter parameter : widgetClass.getOBUIAPPParameterEMObkmoWidgetClassIDList()) {
        // fixed parameters are not part of the fielddefinitions
        if (parameter.isFixed()) {
          defaultParameters.put(parameter.getDBColumnName(), parameter.getFixedValue());
          continue;
        }
        if (parameter.getDefaultValue() != null) {
          DomainType domainType = ParameterUtils.getParameterDomainType(parameter);
          if (domainType.getClass().equals(BooleanDomainType.class)) {
            // boolean default value for widget parameters is not returned as string but as boolean
            // see issue https://issues.openbravo.com/view.php?id=29027
            defaultParameters.put(parameter.getDBColumnName(),
                getBooleanValueFromString(parameter.getDefaultValue()));
          } else {
            defaultParameters.put(parameter.getDBColumnName(), parameter.getDefaultValue());
          }
        }
        final JSONObject fieldDefinition = new JSONObject();
        fieldDefinition.put(PARAMETERID, parameter.getId());
        fieldDefinition.put(PARAMETERNAME, parameter.getDBColumnName());
        fieldDefinition.put(PARAMETERREQUIRED, parameter.isMandatory());
        fieldDefinition.put(PARAMETERWIDTH, "*");

        final Reference reference;
        if (parameter.getReferenceSearchKey() != null) {
          reference = parameter.getReferenceSearchKey();
        } else {
          reference = parameter.getReference();
        }
        if (reference.getName().equals("Text") || reference.getName().equals("Memo")) {
          fieldDefinition.put(PARAMETERCOLSPAN, 2);
        }

        final UIDefinition uiDefinition = UIDefinitionController.getInstance()
            .getUIDefinition(reference);
        fieldDefinition.put(PARAMETERTYPE, uiDefinition.getName());

        try {
          final String fieldProperties = uiDefinition.getFieldProperties(null);
          if (fieldProperties != null && fieldProperties.trim().length() > 0) {
            final JSONObject fieldPropertiesObject = new JSONObject(fieldProperties);
            fieldDefinition.put(PARAMETERFIELDPROPERTIES, fieldPropertiesObject);
          }
        } catch (NullPointerException e) {
          // handle non-carefull implementors of ui definitions
          log.error("Error when processing parameter: " + parameter, e);
          // ignore this field properties for now
        }

        final Object valueMap = getComboBoxData(reference);
        if (valueMap != null) {
          if (valueMap instanceof Collection<?>) {
            fieldDefinition.put(VALUEMAP, (Collection<?>) valueMap);
          } else {
            fieldDefinition.put(VALUEMAP, valueMap);
          }
        }
        fieldDefinition.put(PARAMETERTITLE, getParameterLabel(parameter));
        fieldDefinitions.add(fieldDefinition);
      }
      widgetClassDefinition.put(PARAMETERS, defaultParameters);
      widgetClassDefinition.put(FIELDDEFINITIONS, fieldDefinitions);
      widgetClassDefinition.put(ABOUTFIELDDEFINITIONS, aboutFieldDefinitions);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  protected void addDefaultWidgetProperties(JSONObject jsonObject, WidgetInstance widgetInstance)
      throws JSONException {
    jsonObject.put(WIDGETCLASSID, widgetInstance.getWidgetClass().getId());
    jsonObject.put(MyOpenbravoWidgetComponent.CLASSNAMEPARAMETER, getClientSideWidgetClassName());
    jsonObject.put(DBINSTANCEID, widgetInstance.getId());
    jsonObject.put(TITLE, MyOBUtils.getWidgetTitle(widgetInstance));
    jsonObject.put(COLNUM, widgetInstance.getColumnPosition());
    jsonObject.put(ROWNUM, widgetInstance.getSequenceInColumn());
    jsonObject.put(HEIGHT, widgetClassDefinition.get(HEIGHT));
    jsonObject.put(PRIORITY, widgetInstance.getRelativePriority());

    final JSONObject widgetParams = new JSONObject();
    for (ParameterValue parameterValue : widgetInstance
        .getOBUIAPPParameterValueEMObkmoWidgetInstanceIDList()) {
      widgetParams.put(parameterValue.getParameter().getDBColumnName(),
          ParameterUtils.getParameterValue(parameterValue));
    }

    // Include fixed parameters in the definition.
    for (Parameter parameter : widgetInstance.getWidgetClass()
        .getOBUIAPPParameterEMObkmoWidgetClassIDList()) {
      if (!widgetParams.has(parameter.getDBColumnName()) && parameter.isFixed()) {
        widgetParams.put(parameter.getDBColumnName(),
            ParameterUtils.getParameterFixedValue(getStringParameters(getParameters()), parameter));

      }
    }
    jsonObject.put(PARAMETERS, widgetParams);
  }

  public String getClientSideWidgetClassName() {
    return KernelConstants.ID_PREFIX + getWidgetClassId();
  }

  /**
   * As a default will generate javascript which extends the OBShowParameterWidget widget.
   * 
   */
  public String generate() {
    return "isc.defineClass('" + KernelConstants.ID_PREFIX + getWidgetClassId()
        + "', isc.OBShowParameterWidget);";
  }

  public JSONObject getWidgetInstanceDefinition(WidgetInstance widgetInstance) {
    try {
      final JSONObject jsonObject = new JSONObject();
      addDefaultWidgetProperties(jsonObject, widgetInstance);
      return jsonObject;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  public Map<String, Object> getParameters() {
    return widgetParameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.widgetParameters = parameters;
  }

  private Map<String, String> getStringParameters(Map<String, Object> params) {
    Map<String, String> stringParameters = new HashMap<>();
    final Iterator<String> keys = params.keySet().iterator();
    while (keys.hasNext()) {
      final String keyName = keys.next();
      if (params.get(keyName) instanceof String) {
        stringParameters.put(keyName, (String) params.get(keyName));
      }
    }
    return stringParameters;
  }

  private String getParameterLabel(Parameter parameter) {
    final String userLanguageId = OBContext.getOBContext().getLanguage().getId();

    for (ParameterTrl trl : parameter.getOBUIAPPParameterTrlList()) {
      if (trl.getLanguage().getId().equals(userLanguageId)) {
        return trl.getName();
      }
    }
    return parameter.getName();
  }

  // ++++++++++ Code below should be moved to the UIDefinition classes +++++++

  private static Object getComboBoxData(Reference reference) {
    OBContext.setAdminMode();
    try {
      final UIDefinition uiDefinition = UIDefinitionController.getInstance()
          .getUIDefinition(reference);

      if (uiDefinition instanceof EnumUIDefinition) {
        return getComboBoxData((EnumUIDefinition) uiDefinition);
      } else if (uiDefinition instanceof FKComboUIDefinition) {
        return getComboBoxData((FKComboUIDefinition) uiDefinition);
      } else {
        return null;
      }
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private static Object getComboBoxData(FKComboUIDefinition fkComboUIDefinition) throws Exception {
    // FIXME: Revisit this method later. Not all foreign keys have a ADReferenceTable.
    final ReferencedTable refTable = fkComboUIDefinition.getReference()
        .getADReferencedTableList()
        .get(0);
    final Entity entity = ModelProvider.getInstance().getEntity(refTable.getTable().getName());
    final String orderBy;

    if (refTable.getHqlorderbyclause() != null) {
      orderBy = refTable.getHqlorderbyclause();
    } else {
      final StringBuilder sb = new StringBuilder();
      for (Property prop : entity.getIdentifierProperties()) {
        if (sb.length() > 0) {
          sb.append(",");
        }
        sb.append(prop.getName());
      }
      orderBy = sb.toString();
    }
    final String whereOrderByClause = (refTable.getHqlwhereclause() != null
        ? refTable.getHqlwhereclause()
        : "") + " order by " + orderBy;
    final OBQuery<BaseOBObject> obQuery = OBDal.getInstance()
        .createQuery(entity.getName(), whereOrderByClause);
    final List<JSONObject> values = new ArrayList<>();
    for (BaseOBObject bob : obQuery.list()) {
      final JSONObject dataJSONObject = new JSONObject();
      dataJSONObject.put(JsonConstants.ID, bob.getId());
      // for now always display the identifier
      dataJSONObject.put(JsonConstants.IDENTIFIER, bob.getIdentifier());
      values.add(dataJSONObject);
    }
    return values;
  }

  private static JSONObject getComboBoxData(EnumUIDefinition enumUIDefinition) throws Exception {
    final EnumerateDomainType enumDomainType = (EnumerateDomainType) enumUIDefinition
        .getDomainType();
    @SuppressWarnings("unchecked")
    final Map<String, String> valueMap = createValueMap(
        (Set<String>) enumDomainType.getEnumerateValues(), enumUIDefinition.getReference().getId());
    final JSONObject valueMapJSONObject = new JSONObject();
    for (Entry<String, String> entry : valueMap.entrySet()) {
      valueMapJSONObject.put(entry.getKey(), entry.getValue());
    }
    return valueMapJSONObject;
  }

  public static Map<String, String> createValueMap(Set<String> allowedValues, String referenceId) {
    final String userLanguageId = OBContext.getOBContext().getLanguage().getId();

    final Map<String, String> translatedValues = new LinkedHashMap<>();

    for (String allowedValue : allowedValues) {
      translatedValues.put(allowedValue, allowedValue);
    }

    final String readReferenceHql = "select searchKey, name from ADList where reference.id=:referenceId";
    final Query<Object[]> readReferenceQry = OBDal.getInstance()
        .getSession()
        .createQuery(readReferenceHql, Object[].class);
    readReferenceQry.setParameter("referenceId", referenceId);
    for (Object[] row : readReferenceQry.list()) {
      final String value = (String) row[0];
      final String name = (String) row[1];
      if (allowedValues.contains(value)) {
        translatedValues.put(value, name);
      }
    }

    // set the default if no translation found
    final String hql = "select al.searchKey, trl.name from ADList al, ADListTrl trl where "
        + " al.reference.id=:referenceId and trl.listReference=al and trl.language.id=:languageId"
        + " and al.active=true and trl.active=true";
    final Query<Object[]> qry = OBDal.getInstance().getSession().createQuery(hql, Object[].class);
    qry.setParameter("referenceId", referenceId);
    qry.setParameter("languageId", userLanguageId);
    for (Object[] row : qry.list()) {
      translatedValues.put((String) row[0], (String) row[1]);
    }
    return translatedValues;
  }

  public WidgetClass getWidgetClass() {
    return OBDal.getInstance().get(WidgetClass.class, getWidgetClassId());
  }

  private String getWidgetClassId() {
    String widgetClassId = null;
    try {
      widgetClassId = widgetClassDefinition.getString(WIDGETCLASSID);
    } catch (JSONException ignore) {
      // should not happen if the WidgetProvider instance is initialized properly
    }
    return widgetClassId;
  }

  public void setWidgetClass(WidgetClass widgetClass) {
    setWidgetClassDefinition(widgetClass);
  }

  private boolean getBooleanValueFromString(String value) {
    return "true".equals(value) || "Y".equals(value) || "'Y'".equals(value);
  }

  /**
   * Override this method to make validations on widget classes. If this method returns false the
   * widget class won't be available for users to add new instances.
   * 
   * @return true if the widget class definition is valid.
   */
  public boolean validate() {
    return true;
  }
}
