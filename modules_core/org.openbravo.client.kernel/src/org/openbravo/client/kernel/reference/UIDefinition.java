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
package org.openbravo.client.kernel.reference;

import com.etendoerp.sequences.NextSequenceValue;
import com.etendoerp.sequences.SequenceUtils;
import com.etendoerp.sequences.UINextSequenceValueInterface;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.domaintype.DomainType;
import org.openbravo.base.model.domaintype.PrimitiveDomainType;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.GCSystem;
import org.openbravo.client.application.GCTab;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.application.window.OBViewUtil;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.data.Sqlc;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonConstants;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Base implementation of a user interface client reference.
 * 
 * @author mtaal
 */
public abstract class UIDefinition {
  private static final String TYPE_NAME_PREFIX = "_id_";
  private static final String LIST_REFERENCE_ID = "17";
  private static final String DATETIME_REFERENCE_ID = "16";

  private Reference reference;
  private DomainType domainType;
  private JSONObject gridConfigurationSettings;
  protected static final Logger log = LogManager.getLogger();

  /**
   * Unique name used to identify the type.
   * 
   */
  public String getName() {
    return TYPE_NAME_PREFIX + reference.getId();
  }

  /**
   * @return the Smartclient type from which this type inherits.
   */
  public String getParentType() {
    return "text";
  }

  /**
   * @return the form item type used for editing this reference in a form.
   */
  public String getFormEditorType() {
    return "OBTextItem";
  }

  /**
   * @return the form item type used for editing this reference in a grid. As a default will return
   *         {@link #getFormEditorType()}
   */
  public String getGridEditorType() {
    return getFormEditorType();
  }

  /**
   * @return the read only editor type. As default will return "".
   */
  public String getReadOnlyEditorType() {
    return "";
  }

  /**
   * @return the form item type used for filtering in grids. As a default will return
   *         {@link #getFormEditorType()}
   */
  public String getFilterEditorType() {
    return getFormEditorType();
  }

  /**
   * Computes the properties used to define the type, this includes all the Smartclient SimpleType
   * properties.
   * 
   * @return a javascript string which can be included in the javascript defining the SimpleType.
   *         The default implementation returns an empty string.
   */
  public String getTypeProperties() {
    return "";
  }

  /**
   * Computes properties to initialize and set the field in a Smartclient form. This can be the
   * default value or the sets of values in the valuemap.
   * 
   * NOTE: the field parameter may be null, implementors of subclasses should take this into
   * account.
   * 
   * @param field
   *          the field for which the information should be computed. NOTE: the caller is allowed to
   *          pass null for cases where the field properties are needed for a FormItem which is not
   *          backed by an Openbravo field.
   * @return a JSONObject string which is used to initialize the formitem.
   */
  public String getFieldProperties(Field field) {
    if (field != null && field.isDisplayed() != null && !field.isDisplayed()) {
      return ""; // Not displayed fields use HiddenItem
    }
    return "";
  }

  public String getValueFromSQLDefault(ResultSet rs) throws SQLException {
    return rs.getString(1);
  }

  /**
   * Computes properties to initialize and set the field in a Smartclient form. This can be the
   * default value or the sets of values in the valuemap.
   * 
   * @param field
   *          the field for which the information should be computed.
   * @param getValueFromSession
   * @return a JSONObject string which is used to initialize the formitem.
   */
  public String getFieldProperties(Field field, boolean getValueFromSession) {
    String columnValue = "";
    RequestContext rq = RequestContext.get();
    if (getValueFromSession) {
      String inpColumnName = null;
      if (field.getProperty() != null && !field.getProperty().isEmpty()) {
        inpColumnName = "inp" + "_propertyField_"
            + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
            + field.getColumn().getDBColumnName();
      } else {
        inpColumnName = "inp" + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
      }
      columnValue = rq.getRequestParameter(inpColumnName);
    } else {
      if (SequenceUtils.isSequence(field.getColumn())) {

        UINextSequenceValueInterface sequenceHandler = null;

        // First looks if there is a custom UI definition handler created.
        var referenceSearchKey = field.getColumn().getReferenceSearchKey();

        if (referenceSearchKey != null) {
          sequenceHandler = NextSequenceValue.getInstance().getSequenceHandler(referenceSearchKey.getId());
        }

        // If no custom UI definition is found, use the Default UI handler
        if (sequenceHandler == null) {
          sequenceHandler = NextSequenceValue.getInstance().getSequenceHandler(field.getColumn().getReference().getId());
        }

        if (sequenceHandler != null) {
          columnValue = "<" + sequenceHandler.generateNextSequenceValue(field, rq) + ">";
        }
      } else if (field.getColumn().getDBColumnName().equalsIgnoreCase("documentno")
          || (field.getColumn().isUseAutomaticSequence()
              && field.getColumn().getDBColumnName().equals("Value"))) {
        String docTypeTarget = rq
            .getRequestParameter("inp" + Sqlc.TransformaNombreColumna("C_DocTypeTarget_ID"));
        if (docTypeTarget == null) {
          docTypeTarget = "";
        }
        String docType = rq
            .getRequestParameter("inp" + Sqlc.TransformaNombreColumna("C_DocType_ID"));
        if (docType == null) {
          docType = "";
        }
        columnValue = "<" + Utility.getDocumentNo(new DalConnectionProvider(false),
            rq.getVariablesSecureApp(), field.getTab().getWindow().getId(),
            field.getColumn().getTable().getDBTableName(), docTypeTarget, docType, false, false)
            + ">";
      } else {
        final String windowId = field.getTab().getWindow().getId();
        final String colName = field.getColumn().getDBColumnName();

        final String prefValue = Utility.getPreference(rq.getVariablesSecureApp(), colName,
            windowId);

        String defaultS;
        if (StringUtils.isNotBlank(prefValue)) {
          // if there is a preference for this field, use it instead of the one that might be
          // defined at column level
          defaultS = prefValue;
        } else {
          defaultS = field.getColumn().getDefaultValue();
        }

        if (defaultS == null || defaultS.equals("\"\"")) {
          defaultS = "";
        }
        if (defaultS.equalsIgnoreCase("@#Date@")) {
          return setNOWDefault();
        } else if (!defaultS.startsWith("@SQL=")) {
          columnValue = getDefaultValue(rq.getVariablesSecureApp(), colName, defaultS, windowId);
        } else {
          columnValue = getDefaultValueFromSQLExpression(rq.getVariablesSecureApp(), field,
              defaultS);
        }
      }
    }
    if (columnValue == null || columnValue.equals("null")) {
      columnValue = "";
    }
    JSONObject jsnobject = new JSONObject();
    try {
      jsnobject.put("value", createFromClassicString(columnValue));
      jsnobject.put("classicValue", columnValue);
    } catch (JSONException e) {
      log.error(
          "Couldn't get field property value for column " + field.getColumn().getDBColumnName());
    }
    return jsnobject.toString();
  }

  /**
   * Returns the value for a default value expression which represents a session value or a fixed
   * value. This method is not used to calculate SQL based expressions (those that start with
   * '@SQL=') and NOW expression ('@#Date@').
   *
   * @param vars
   *          Handler for the session info.
   * @param columnName
   *          String with the name of the column that has the default value.
   * @param defaultValueExpression
   *          String with the default value expression.
   * @param windowId
   *          String with the window id.
   * @return String with the calculated default value.
   */
  public String getDefaultValue(VariablesSecureApp vars, String columnName,
      String defaultValueExpression, String windowId) {
    return Utility.getDefault(new DalConnectionProvider(false), vars, columnName,
        defaultValueExpression, windowId, "");
  }

  /**
   * Returns the value for a default value expression based on a SQL expression. This kind of
   * expressions start with '@SQL='.
   *
   * @param vars
   *          Handler for the session info.
   * @param field
   *          Field whose column has the default value.
   * @param defaultValueExpression
   *          String with the default value expression.
   * @return String with the calculated default value.
   */
  public String getDefaultValueFromSQLExpression(VariablesSecureApp vars, Field field,
      String defaultValueExpression) {
    ArrayList<String> params = new ArrayList<String>();
    String sql = parseSQL(defaultValueExpression, params);
    int indP = 1;
    PreparedStatement ps = null;
    String columnValue = null;
    try {
      ps = OBDal.getInstance().getConnection(false).prepareStatement(sql);
      for (String parameter : params) {
        String value = "";
        if (parameter.substring(0, 1).equals("#")) {
          value = Utility.getContext(new DalConnectionProvider(false),
              RequestContext.get().getVariablesSecureApp(), parameter,
              field.getTab().getWindow().getId());
        } else {
          String fieldId = "inp" + Sqlc.TransformaNombreColumna(parameter);
          if (RequestContext.get().getParameterMap().containsKey(fieldId)) {
            value = RequestContext.get().getRequestParameter(fieldId);
          }
          if (value == null || value.equals("")) {
            value = Utility.getContext(new DalConnectionProvider(false),
                RequestContext.get().getVariablesSecureApp(), parameter,
                field.getTab().getWindow().getId());
          }
        }
        ps.setObject(indP++, value);
      }
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        columnValue = getValueFromSQLDefault(rs);
      }
    } catch (Exception e) {
      log.error("Error computing default value for field " + field.getName() + " of tab "
          + field.getTab().getName(), e);
    } finally {
      try {
        ps.close();
      } catch (SQLException e) {
        // won't happen
      }
    }
    return columnValue;
  }

  /**
   * It returns the same as getFieldProperties except in the case of combo UIDefinitions. In combo
   * UI definitions, a call to the super will be done, but the combo computation itself will not be
   * done (so only the default value, or the current request value, will be considered).
   * 
   */
  public String getFieldPropertiesWithoutCombo(Field field, boolean getValueFromSession) {
    return getFieldProperties(field, getValueFromSession);
  }

  public String getFieldPropertiesFirstRecord(Field field, boolean getValueFromSession) {
    return getFieldProperties(field, getValueFromSession);
  }

  /**
   * Returns alignment in grid view. In case it returns null, default alignment for actual data type
   * is used.
   * 
   * @return <code>null</code> for default alignment, "left", "center" or "right"
   */
  public String getCellAlign() {
    return null;
  }

  private String setNOWDefault() {
    JSONObject jsonObject = new JSONObject();
    try {
      UIDefinition uiDef = this;
      if (!(this instanceof DateUIDefinition)) {
        Reference datetimeReference = OBDal.getInstance()
            .getProxy(Reference.class, DATETIME_REFERENCE_ID);
        uiDef = UIDefinitionController.getInstance().getUIDefinition(datetimeReference);
      }
      String columnValue = uiDef.convertToClassicString(new Date());
      jsonObject.put("value", uiDef.createFromClassicString(columnValue));
      jsonObject.put("classicValue", columnValue);
      jsonObject.put("hasDateDefault", true);
    } catch (JSONException e) {
      log.error("Couldn't get field property value");
    }
    return jsonObject.toString();
  }

  /**
   * Convert a string value as used in classic OB to a type safe value.
   * 
   * @see PrimitiveDomainType#createFromString(String)
   */
  public Object createFromClassicString(String value) {
    if (getDomainType() instanceof PrimitiveDomainType) {
      return ((PrimitiveDomainType) getDomainType()).createFromString(value);
    } else {
      return value;
    }
  }

  /**
   * Creates a classic string which is used by callouts from an object value.
   * 
   * @param value
   * @return the classic string
   */
  public String convertToClassicString(Object value) {
    if (value == null) {
      return "";
    }
    if (!(getDomainType() instanceof PrimitiveDomainType)) {
      return value.toString();
    }
    return ((PrimitiveDomainType) getDomainType()).convertToString(value);
  }

  /**
   * Parameters passed in to the datasource, for example the
   * {@link JsonConstants#ADDITIONAL_PROPERTIES_PARAMETER} can be passed in like this.
   * 
   * @return a list of parameters used to drive the datasource generation incorporating this
   *         UIDefinition.
   */
  public Map<String, Object> getDataSourceParameters() {
    return Collections.emptyMap();
  }

  /**
   * Computes properties to initialize and set the field in a Smartclient grid filter. This can be
   * the default value or the sets of values in the valuemap.
   * 
   * Note: the result should be either empty, if not empty then it start with a comma and end
   * without a comma, this to generate correct javascript.
   * 
   * @param field
   *          the field for which the information should be computed.
   * @return a JSONObject string which is used to initialize the formitem.
   */
  public String getFilterEditorProperties(Field field) {
    if (getFilterEditorType() == null) {
      return ",canFilter: false";
    }
    String filterEditorProperties = getFilterEditorPropertiesProperty(field);
    if (!"".equals(filterEditorProperties)) {
      filterEditorProperties = filterEditorProperties.replaceAll("(^)( *?)(,)", "");
      return ", filterEditorProperties: {" + filterEditorProperties + "}";
    } else {
      return "";
    }
  }

  /**
   * Returns the filterEditorProperties property set on the gridfield. Note for implementations in
   * the subclass: field maybe null.
   * 
   * @param field
   *          the field to generate the filter editor properties for, note it is allowed to pass
   *          null, implementors should gracefully handle this.
   */
  protected String getFilterEditorPropertiesProperty(Field field) {
    return "";
  }

  /**
   * Computes properties to initialize and set the field in a Smartclient grid cell. This can be the
   * default value or the sets of values in the valuemap.
   * 
   * Note: the result should be either empty, if not empty then it start with a comma and end
   * without a comma, this to generate correct javascript.
   * 
   * @param field
   *          the field for which the information should be computed.
   * @return a JSONObject string which is used to initialize the formitem.
   */
  public String getGridFieldProperties(Field field) {
    StringBuffer result = new StringBuffer();
    if (this.getGridEditorType() != null
        && !this.getGridEditorType().equals(this.getFormEditorType())) {
      result.append(", editorType: '" + this.getGridEditorType() + "'");
    }
    // Fixes issues 30507: SC Regression: When clicking on a field on edit mode on the grid the
    // value is properly selected.
    result.append(", selectOnClick: true");

    Boolean canSort;
    Boolean canFilter;

    if ((field.getTab().isObuiappCanAdd() || field.getTab().isObuiappCanDelete())
        && field.getTab().getWindow().getWindowType().equals("OBUIAPP_PickAndExecute")) {
      canSort = false;
      canFilter = false;
    } else {
      canSort = (Boolean) readGridConfigurationSetting("canSort");
      canFilter = (Boolean) readGridConfigurationSetting("canFilter");
    }

    if (canSort != null) {
      result.append(", canSort: " + canSort.toString());
    }
    if (canFilter != null) {
      result.append(", canFilter: " + canFilter.toString());
    }
    result.append(getShowHoverGridFieldSettings(field));
    return result.toString();
  }

  public String getParameterProperties(Parameter parameter) {
    if (parameter.isStartinnewline()) {
      JSONObject o = new JSONObject();
      try {
        o.put("startRow", true);
        return o.toString();
      } catch (Exception e) {
        return "";
      }
    }
    return "";
  }

  public String getParameterWidth(Parameter parameter) {
    return "*";
  }

  /**
   * Computes properties to initialize and set the field in a Smartclient grid cell when it is being
   * edited. This can be the default value or the sets of values in the valuemap.
   * 
   * Note: the result should be either empty, if not empty then it start with a comma and end
   * without a comma, this to generate correct javascript.
   * 
   * @param field
   *          the field for which the information should be computed.
   * @return a JSONObject string which is used to initialize the formitem.
   */
  public String getGridEditorFieldProperties(Field field) {
    return "";
  }

  public Reference getReference() {
    return reference;
  }

  public void setReference(Reference reference) {
    this.reference = reference;
  }

  public DomainType getDomainType() {
    if (domainType == null) {
      if (reference == null) {
        throw new OBException("Domain type can not be computed, reference is not set");
      }
      domainType = ModelProvider.getInstance().getReference(reference.getId()).getDomainType();
    }
    return domainType;
  }

  protected String removeAttributeFromString(String inpString, String attr) {
    String result = inpString;
    if (result.indexOf(attr) != -1) {
      // If there is a previous 'canSort' or 'canFilter' set, remove it to avoid collision when the
      // new one is set later
      result = result.replaceAll("(,)( *?)(" + attr + ")( *?)(:)( *?)(false|true)( *?)", "");
    }
    return result;

  }

  /**
   * Reads a particular value from the grid configuration settings
   * 
   * @param setting
   *          the setting whose value is to be returned.
   */
  protected Object readGridConfigurationSetting(String setting) {
    Object result = null;
    try {
      result = this.gridConfigurationSettings.get(setting);
    } catch (JSONException e) {
    } catch (Exception e) {
    }
    return result;
  }

  /**
   * Obtains the grid configuration values for the given field and sets them into the
   * 'gridConfigurationSettings' variable.
   * 
   * The aim of having all these values in a single variable at once is to make a single call to the
   * database and then be able to use the values stored into 'gridConfigurationSettings' wherever it
   * be needed (without more calls to the database).
   * 
   * @param field
   *          the field for which the information should be computed.
   * @param systemGC
   * @param tabGC
   */
  public void establishGridConfigurationSettings(Field field, Optional<GCSystem> systemGC,
      Optional<GCTab> tabGC) {
    this.gridConfigurationSettings = OBViewUtil.getGridConfigurationSettings(field, systemGC,
        tabGC);
  }

  // note can make sense to also enable hover of values for enums
  // but then the value should be converted to the translated
  // value of the enum
  protected String getShowHoverGridFieldSettings(Field field) {
    if (showHover()) {
      return ", showHover: true";
    }
    return "";
  }

  /**
   * This method determines if the UI definition should include the showHover property as part of
   * the grid field properties. Returns {@code true} by default.
   * 
   * @return {@code true} if fields using this UI definition should display their text on a hover
   *         box, otherwise return {@code false}
   */
  public boolean showHover() {
    return true;
  }

  protected String getGridFieldName(Field fld) {
    final Property prop = KernelUtils.getInstance().getPropertyFromColumn(fld.getColumn());
    return prop.getName();
  }

  protected String getValueInComboReference(Field field, boolean getValueFromSession,
      String columnValue) {
    return getValueInComboReference(field, getValueFromSession, columnValue, false);
  }

  protected String getValueInComboReference(Field field, boolean getValueFromSession,
      String columnValue, boolean onlyFirstRecord) {
    try {
      String ref = field.getColumn().getReference().getId();
      boolean isListReference = LIST_REFERENCE_ID.equals(ref);
      if (!isListReference && !field.getColumn().isMandatory()
          && StringUtils.isEmpty(columnValue)) {
        // non mandatory without value nor default, should only return empty value, prevent
        // everything else
        JSONObject entry = new JSONObject();
        entry.put(JsonConstants.ID, (String) null);
        entry.put(JsonConstants.IDENTIFIER, (String) null);
        return entry.toString();
      }

      FieldProvider[] fps = null;
      RequestContext rq = RequestContext.get();
      VariablesSecureApp vars = rq.getVariablesSecureApp();
      // handles a corner case when the whole text of the selector field is removed, FIC call is
      // done with comboReload, but reload is not necessary for the changed column as we have just
      // entered empty value. Refer issue https://issues.openbravo.com/view.php?id=27612
      String columnName = "inp" + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
      if (!isListReference && "AD_ORG_ID".equals(field.getColumn().getDBColumnName().toUpperCase())
          && "".equals(columnValue)
          && vars.getStringParameter("CHANGED_COLUMN").equals(columnName)) {
        JSONObject entry = new JSONObject();
        entry.put(JsonConstants.ID, columnValue);
        entry.put(JsonConstants.IDENTIFIER, columnValue);
        return entry.toString();
      }
      boolean comboreload = rq.getRequestParameter("donotaddcurrentelement") != null
          && rq.getRequestParameter("donotaddcurrentelement").equals("true");

      ApplicationDictionaryCachedStructures cachedStructures = WeldUtils
          .getInstanceFromStaticBeanManager(ApplicationDictionaryCachedStructures.class);
      ComboTableData comboTableData = cachedStructures.getComboTableData(field);
      FieldProvider tabData = generateTabData(field.getTab().getADFieldList(), field, columnValue);
      Map<String, String> parameters = comboTableData.fillSQLParametersIntoMap(
          new DalConnectionProvider(false), vars, tabData, field.getTab().getWindow().getId(),
          (getValueFromSession && !comboreload) ? columnValue : "");
      if ((onlyFirstRecord || columnValue != null) && !isListReference) {
        parameters.put("@ONLY_ONE_RECORD@", columnValue);
      }

      if (!isListReference) {
        fps = comboTableData.select(new DalConnectionProvider(false), parameters,
            getValueFromSession && !comboreload, 0, 0);
      } else {
        fps = comboTableData.select(new DalConnectionProvider(false), parameters,
            getValueFromSession && !comboreload);
      }
      ArrayList<FieldProvider> values = new ArrayList<FieldProvider>();
      values.addAll(Arrays.asList(fps));
      ArrayList<JSONObject> comboEntries = new ArrayList<JSONObject>();
      ArrayList<String> possibleIds = new ArrayList<String>();
      // If column is not mandatory we add an initial blank value
      if (!field.getColumn().isMandatory()) {
        // check if column value is present and is the first selected value.
        // If yes, no need for the blank value as it is the single value to be set.
        if (!StringUtils.isEmpty(columnValue) && !isListReference) {
          if (values.size() > 0 && values.get(0).getField(JsonConstants.ID) != null) {
            if (!columnValue.equals(values.get(0).getField(JsonConstants.ID))) {
              possibleIds.add("");
              JSONObject entry = new JSONObject();
              entry.put(JsonConstants.ID, (String) null);
              entry.put(JsonConstants.IDENTIFIER, (String) null);
              comboEntries.add(entry);
              // only one value has to be set if column value is present, but since it is not
              // present in the valueMap clearing it and setting empty value.
              // Refer issue https://issues.openbravo.com/view.php?id=27061
              values = new ArrayList<FieldProvider>();
            }
          }
        } else {
          possibleIds.add("");
          JSONObject entry = new JSONObject();
          entry.put(JsonConstants.ID, (String) null);
          entry.put(JsonConstants.IDENTIFIER, (String) null);
          comboEntries.add(entry);
        }
      }
      for (FieldProvider fp : values) {
        possibleIds.add(fp.getField("ID"));
        JSONObject entry = new JSONObject();
        entry.put(JsonConstants.ID, fp.getField("ID"));
        entry.put(JsonConstants.IDENTIFIER, fp.getField("NAME"));
        comboEntries.add(entry);
      }
      JSONObject fieldProps = new JSONObject();
      if (!isListReference) {
        if (comboEntries.size() > 0) {
          if (comboEntries.get(0).has(JsonConstants.ID)) {
            fieldProps.put("value", comboEntries.get(0).get(JsonConstants.ID));
            fieldProps.put("classicValue", comboEntries.get(0).get(JsonConstants.ID));
            fieldProps.put("identifier", comboEntries.get(0).get(JsonConstants.IDENTIFIER));
          } else {
            fieldProps.put("value", (String) null);
            fieldProps.put("classicValue", (String) null);
            fieldProps.put("identifier", (String) null);
          }
        } else {
          fieldProps.put("value", "");
          fieldProps.put("classicValue", "");
          fieldProps.put("identifier", "");
        }
      } else {
        if (getValueFromSession && !comboreload) {
          fieldProps.put("value", columnValue);
          fieldProps.put("classicValue", columnValue);
        } else {
          if (possibleIds.contains(columnValue)) {
            fieldProps.put("value", columnValue);
            fieldProps.put("classicValue", columnValue);
          } else {
            // In case the default value doesn't exist in the combo values, we choose the first one
            if (comboEntries.size() > 0) {
              if (comboEntries.get(0).has(JsonConstants.ID)) {
                fieldProps.put("value", comboEntries.get(0).get(JsonConstants.ID));
                fieldProps.put("classicValue", comboEntries.get(0).get(JsonConstants.ID));
              } else {
                fieldProps.put("value", (String) null);
                fieldProps.put("classicValue", (String) null);
              }
            } else {
              fieldProps.put("value", "");
              fieldProps.put("classicValue", "");
            }
          }
        }
        fieldProps.put("entries", new JSONArray(comboEntries));
      }
      return fieldProps.toString();
    } catch (Exception e) {
      throw new OBException("Error while computing combo data", e);
    }
  }

  private FieldProvider generateTabData(List<Field> fields, Field currentField,
      String currentValue) {
    HashMap<String, Object> noinpDataMap = new HashMap<String, Object>();
    for (Field field : fields) {
      if (field.getColumn() == null) {
        continue;
      }
      String oldKey = "inp" + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
      Object value;
      if (currentField.getId().equals(field.getId())) {
        value = currentValue;
      } else {
        value = RequestContext.get().getRequestParameter(oldKey);
      }
      noinpDataMap.put(field.getColumn().getDBColumnName(),
          value == null || value.equals("") ? null : value.toString());
    }
    return new FieldProviderFactory(noinpDataMap);
  }

  public static String parseSQL(String code, ArrayList<String> colNames) {
    if (code == null || code.trim().equals("")) {
      return "";
    }
    String token;
    String strValue = code;
    StringBuffer strOut = new StringBuffer();

    int i = strValue.indexOf("@");
    String strAux, strAux1;
    while (i != -1) {
      if (strValue.length() > (i + 5)
          && strValue.substring(i + 1, i + 5).equalsIgnoreCase("SQL=")) {
        strValue = strValue.substring(i + 5, strValue.length());
      } else {
        // Delete the chain symbol
        strAux = strValue.substring(0, i).trim();
        if (strAux.substring(strAux.length() - 1).equals("'")) {
          strAux = strAux.substring(0, strAux.length() - 1);
          strOut.append(strAux);
        } else {
          strOut.append(strValue.substring(0, i));
        }
        strAux1 = strAux;
        if (strAux.substring(strAux.length() - 1).equals("(")) {
          strAux = strAux.substring(0, strAux.length() - 1).toUpperCase().trim();
        }
        if (strAux.length() > 3
            && strAux.substring(strAux.length() - 3, strAux.length()).equals(" IN")) {
          strAux = " type=\"replace\" optional=\"true\" after=\"" + strAux1 + "\" text=\"'" + i
              + "'\"";
        } else {
          strAux = "";
        }
        strValue = strValue.substring(i + 1, strValue.length());

        int j = strValue.indexOf("@");
        if (j < 0) {
          return "";
        }

        token = strValue.substring(0, j);

        // String modifier = ""; // holds the modifier (# or $) for the session value
        // if (token.substring(0, 1).indexOf("#") > -1 || token.substring(0, 1).indexOf("$") > -1) {
        // modifier = token.substring(0, 1);
        // token = token.substring(1, token.length());
        // }
        if (strAux.equals("")) {
          strOut.append("?");
        } else {
          strOut.append("'" + i + "'");
        }
        // String parameter = "<Parameter name=\"" + token + "\"" + strAux + "/>";
        // String paramElement[] = { parameter, modifier };
        colNames.add(token);// paramElement);
        strValue = strValue.substring(j + 1, strValue.length());
        strAux = strValue.trim();
        if (strAux.length() > 0 && strAux.substring(0, 1).indexOf("'") > -1) {
          strValue = strAux.substring(1, strValue.length());
        }
      }
      i = strValue.indexOf("@");
    }
    strOut.append(strValue);
    return strOut.toString();
  }

  /**
   * @deprecated replaced by {@link #createFromClassicString(String)}
   */
  @Deprecated
  public Object createJsonValueFromClassicValueString(java.lang.String value) {
    if (getDomainType() instanceof PrimitiveDomainType) {
      return ((PrimitiveDomainType) getDomainType()).createFromString(value);
    } else {
      return value;
    }
  }

  /**
   * @deprecated replaced by {@link #createFromClassicString(String)}
   */
  @Deprecated
  public String formatValueFromSQL(java.lang.String value) {
    return value;
  }

  /**
   * @deprecated replaced by {@link #convertToClassicString(Object)}
   */
  @Deprecated
  public String formatValueToSQL(java.lang.String value) {
    return value;
  }

  protected boolean getSafeBoolean(Boolean value) {
    return value != null && value;
  }

}
