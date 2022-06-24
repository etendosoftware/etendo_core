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
package org.openbravo.client.application.window;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.etendoerp.sequences.SequenceUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.domaintype.PrimitiveDomainType;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.application.DynamicExpressionParser;
import org.openbravo.client.application.Note;
import org.openbravo.client.application.window.servlet.CalloutServletConfig;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.reference.EnumUIDefinition;
import org.openbravo.client.kernel.reference.ForeignKeyUIDefinition;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.SecurityChecker;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.Sqlc;
import org.openbravo.erpCommon.ad_callouts.CalloutConstants;
import org.openbravo.erpCommon.ad_callouts.CalloutInformationProvider;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.ad_callouts.SimpleCalloutInformationProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.ReferencedTable;
import org.openbravo.model.ad.ui.AuxiliaryInput;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonToDataConverter;
import org.openbravo.service.json.JsonUtils;

/**
 * This class computes all the required information in Openbravo 3 forms. Basically, this can be
 * summarized in the following actions:
 * 
 * Computation of all required column information (including combo values)
 * 
 * Computation of auxiliary input values
 * 
 * Execution of callouts
 * 
 * Insertion of all relevant data in the session
 * 
 * Format: in the request and session the values are always formatted in classic mode. The ui
 * definition computes jsonobjects which contain a value as well as a classicValue, the latter is
 * placed in the request/session for subsequent callout computations.
 */
public class FormInitializationComponent extends BaseActionHandler {
  private static final Logger log = LogManager.getLogger();

  private static final int MAX_CALLOUT_CALLS = 50;

  @Inject
  private ApplicationDictionaryCachedStructures cachedStructures;

  @Inject
  @Any
  private Instance<FICExtension> ficExtensions;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    OBContext.setAdminMode(true);
    long iniTime = System.currentTimeMillis();
    String mode = null, tabId = null, rowId = null;
    try {
      // Execution mode. It can be:
      // - NEW: used when the user clicks on the "New record" button
      // - EDIT: used when the user opens a record in form view
      // - CHANGE: used when the user changes a field which should fire callouts or comboreloads
      // - SETSESSION: used when the user calls a process
      mode = readParameter(parameters, "MODE");
      // ID of the parent record
      String parentId = readParameter(parameters, "PARENT_ID");
      // The ID of the tab
      tabId = readParameter(parameters, "TAB_ID");
      // The ID of the record. Only relevant on EDIT, CHANGE and SETSESSION modes
      rowId = readParameter(parameters, "ROW_ID");
      // The column changed by the user. Only relevant on CHANGE mode
      String changedColumn = readParameter(parameters, "CHANGED_COLUMN");
      Tab tab = getTab(tabId);
      BaseOBObject row = null;
      BaseOBObject parentRecord = null;
      Map<String, JSONObject> columnValues = new HashMap<String, JSONObject>();
      List<String> allColumns = new ArrayList<String>();
      List<String> calloutsToCall = new ArrayList<String>();
      List<String> lastfieldChanged = new ArrayList<String>();
      List<String> changeEventCols = new ArrayList<String>();
      Map<String, List<String>> columnsInValidation = new HashMap<String, List<String>>();
      List<JSONObject> calloutMessages = new ArrayList<JSONObject>();
      List<String> jsExcuteCode = new ArrayList<String>();
      Map<String, Object> hiddenInputs = new HashMap<String, Object>();

      boolean databaseBasedTable = ApplicationConstants.TABLEBASEDTABLE
          .equals(tab.getTable().getDataOriginType());

      log.debug("Form Initialization Component Execution. Tab Name: " + tab.getWindow().getName()
          + "." + tab.getName() + " Tab Id:" + tab.getId());
      log.debug("Execution mode: " + mode);
      if (rowId != null) {
        log.debug("Row id: " + rowId);
      }
      if (changedColumn != null) {
        log.debug("Changed field: " + changedColumn);
      }

      // If the table is not based in db table there is no BaseOBObject associated to it, don't try
      // to retrieve the row
      if (databaseBasedTable && rowId != null && !rowId.equals("null")) {
        row = OBDal.getInstance().get(tab.getTable().getName(), rowId);
      }
      JSONObject jsContent = new JSONObject();
      try {
        if (content != null) {
          jsContent = new JSONObject(content);
        }
      } catch (JSONException e) {
        throw new OBException("Error while parsing content", e);
      }
      List<String> visibleProperties = null;
      if (jsContent.has("_visibleProperties")) {
        visibleProperties = convertJSONArray(jsContent.getJSONArray("_visibleProperties"));
      }
      List<String> gridVisibleProperties = new ArrayList<String>();
      if (jsContent.has("_gridVisibleProperties")) {
        gridVisibleProperties = convertJSONArray(jsContent.getJSONArray("_gridVisibleProperties"));
      }

      List<String> overwrittenAuxiliaryInputs = new ArrayList<String>();
      // The provided overwrittenAuxiliaryInputs only have to be persisted when calling the FIC in
      // CHANGE mode. In the rest of the modes all auxiliary inputs are computed regardless of
      // whether a callout have modified them in a previous request with the exception of NEW. In
      // NEW mode auxiliary inputs are not recomputed if they were previously calculated by callouts
      // (within in the same request)
      if (jsContent.has("overwrittenAuxiliaryInputs") && "CHANGE".equals(mode)) {
        overwrittenAuxiliaryInputs = convertJSONArray(
            jsContent.getJSONArray("overwrittenAuxiliaryInputs"));
      }

      // If the table is not based in a db table, don't try to create a BaseOBObject
      if (databaseBasedTable) {
        // create the row from the json content then
        if (row == null) {
          final JsonToDataConverter fromJsonConverter = OBProvider.getInstance()
              .get(JsonToDataConverter.class);

          // create a new json object using property names:
          final JSONObject convertedJson = new JSONObject();
          final Entity entity = ModelProvider.getInstance()
              .getEntityByTableName(tab.getTable().getDBTableName());
          for (Property property : entity.getProperties()) {
            if (property.getColumnName() != null) {
              final String inpName = "inp" + Sqlc.TransformaNombreColumna(property.getColumnName());
              if (jsContent.has(inpName)) {
                final UIDefinition uiDef = UIDefinitionController.getInstance()
                    .getUIDefinition(property.getColumnId());
                Object jsonValue = jsContent.get(inpName);
                if (jsonValue instanceof String) {
                  jsonValue = uiDef.createFromClassicString((String) jsonValue);
                }
                convertedJson.put(property.getName(), jsonValue);
                if (property.isId()) {
                  setSessionValue(tab.getWindow().getId() + "|" + property.getColumnName(),
                      jsonValue);
                }
              }
            }
          }
          // remove the id as it must be a new record
          convertedJson.remove("id");
          convertedJson.put(JsonConstants.ENTITYNAME, entity.getName());
          row = fromJsonConverter.toBaseOBObject(convertedJson);
          row.setNewOBObject(true);
        } else {
          final Entity entity = ModelProvider.getInstance()
              .getEntityByTableName(tab.getTable().getDBTableName());
          for (Property property : entity.getProperties()) {
            if (property.isId()) {
              setSessionValue(tab.getWindow().getId() + "|" + property.getColumnName(),
                  row.getId());
            }
          }
        }
      }

      // First the parent record is retrieved and the session variables for the parent records are
      // set
      long t1 = System.currentTimeMillis();
      // If the table is not based in a db table, don't try to retrieve the parent record (the row
      // is null because tables not based on db tables do not have BaseOBObjects)
      if (databaseBasedTable) {
        parentRecord = setSessionVariablesInParent(mode, tab, row, parentId);
      }

      // We also need to set the current record values in the request
      long t2 = System.currentTimeMillis();
      setValuesInRequest(mode, tab, row, jsContent);

      // Calculation of validation dependencies
      long t3 = System.currentTimeMillis();
      computeListOfColumnsSortedByValidationDependencies(mode, tab, allColumns, columnsInValidation,
          changeEventCols, changedColumn);

      // Computation of the Auxiliary Input values
      long t4 = System.currentTimeMillis();
      // allColumns cannot be used here because in change mode it only contains the modified columns
      List<String> allColumnsInTab = getAllColumnsInTab(tab);
      computeAuxiliaryInputs(mode, tab, allColumnsInTab, columnValues, overwrittenAuxiliaryInputs);

      // Computation of Column Values (using UIDefinition, so including combo values and all
      // relevant additional information)
      long t5 = System.currentTimeMillis();
      computeColumnValues(mode, tab, allColumns, columnValues, parentRecord, parentId,
          changedColumn, jsContent, changeEventCols, calloutsToCall, lastfieldChanged,
          visibleProperties, gridVisibleProperties);

      // Execution of callouts
      long t6 = System.currentTimeMillis();
      List<String> changedCols = executeCallouts(mode, tab, columnValues, changedColumn,
          calloutsToCall, lastfieldChanged, calloutMessages, changeEventCols, jsExcuteCode,
          hiddenInputs, overwrittenAuxiliaryInputs);

      // Compute the auxiliary inputs after executing the callout to ensure they use the updated
      // parameters
      if (mode.equals("NEW") || mode.equals("CHANGE")) {
        // In the case of NEW mode, we compute auxiliary inputs again to take into account that
        // auxiliary inputs could depend on a default value
        computeAuxiliaryInputs(mode, tab, allColumnsInTab, columnValues,
            overwrittenAuxiliaryInputs);
      }

      if (changedCols.size() > 0) {
        RequestContext.get().setRequestParameter("donotaddcurrentelement", "true");
        subsequentComboReload(tab, columnValues, changedCols, columnsInValidation);
      }

      // Attachment information
      long t7 = System.currentTimeMillis();
      List<JSONObject> attachments = new ArrayList<JSONObject>();
      int attachmentCount = 0;
      if (jsContent.has("MULTIPLE_ROW_IDS")) {
        attachmentCount = computeAttachmentCount(tab,
            convertJSONArray(jsContent.getJSONArray("MULTIPLE_ROW_IDS")), true);
      } else {
        attachmentCount = computeAttachmentCount(tab, Arrays.asList(rowId), false);
      }

      // Notes information
      long t8 = System.currentTimeMillis();
      int noteCount = computeNoteCount(tab, rowId);

      // Execute hooks implementing FICExtension.
      long t9 = System.currentTimeMillis();
      for (FICExtension ficExtension : ficExtensions) {
        ficExtension.execute(mode, tab, columnValues, row, changeEventCols, calloutMessages,
            attachments, jsExcuteCode, hiddenInputs, noteCount, overwrittenAuxiliaryInputs);
      }

      // Construction of the final JSONObject
      long t10 = System.currentTimeMillis();
      JSONObject finalObject = buildJSONObject(mode, tab, columnValues, row, changeEventCols,
          calloutMessages, attachments, attachmentCount, jsExcuteCode, hiddenInputs, noteCount,
          overwrittenAuxiliaryInputs);
      analyzeResponse(tab, columnValues);
      long t11 = System.currentTimeMillis();
      log.debug("Elapsed time: " + (System.currentTimeMillis() - iniTime) + "(" + (t2 - t1) + ","
          + (t3 - t2) + "," + (t4 - t3) + "," + (t5 - t4) + "," + (t6 - t5) + "," + (t7 - t6) + ","
          + (t8 - t7) + "," + (t9 - t8) + "," + (t10 - t9) + "," + (t11 - t10) + ")");
      log.debug("Attachment exists: " + finalObject.getBoolean("attachmentExists"));
      return finalObject;
    } catch (Throwable t) {
      log.error("TabId:" + tabId + " - Mode:" + mode + " - rowId:" + rowId, t);
      final String jsonString = JsonUtils.convertExceptionToJson(t);
      try {
        return new JSONObject(jsonString);
      } catch (JSONException e) {
        log.error("Error while generating the error JSON object: " + jsonString, e);
      }
    } finally {
      // Clear session to prevent slow flush
      OBDal.getInstance().getSession().clear();

      OBContext.restorePreviousMode();
    }
    return null;
  }

  /** Returns an unsorted list of all columns present in the tab */
  private List<String> getAllColumnsInTab(Tab tab) {
    List<String> allColumns = new ArrayList<String>();
    for (Field field : getADFieldList(tab.getId())) {
      if (field.getColumn() == null) {
        continue;
      }
      allColumns.add(field.getColumn().getDBColumnName());
    }
    return allColumns;
  }

  private void analyzeResponse(Tab tab, Map<String, JSONObject> columnValues) {
    int maxEntries = 1000;
    int i = 0;
    String heavyCols = "";
    for (String col : columnValues.keySet()) {
      if (columnValues.get(col).has("entries")) {
        try {
          JSONArray array = columnValues.get(col).getJSONArray("entries");
          if (array.length() > maxEntries) {
            if (i > 0) {
              heavyCols += ",";
            }
            heavyCols += col;
            i++;
          }
        } catch (JSONException e) {
          log.error("There was an error while analyzing the response for field: " + col);
        }
      }
    }
    if (!"".equals(heavyCols)) {
      log.warn("Warning: In the window " + tab.getWindow().getName() + ", in tab " + tab.getName()
          + " the combo fields " + heavyCols + " contain more than " + maxEntries
          + " entries, and this could cause bad performance in the application. Possible fixes include changing these columns from a combo into a Selector, or adding a validation to reduce the number of entries in the combo.");
    }
  }

  /**
   * Get JSONObject list with data of the attachments in given tab and records
   * 
   * @param tab
   *          tab to take attachments
   * @param recordIds
   *          list of record IDs where taken attachments
   * @param doExists
   *          flag to not return the actual count just 1 or 0
   * @return count of attachment found for the given records.
   */
  private int computeAttachmentCount(Tab tab, List<String> recordIds, boolean doExists) {
    String tableId = tab.getTable().getId();
    OBCriteria<Attachment> attachmentFiles = OBDao.getFilteredCriteria(Attachment.class,
        Restrictions.eq("table.id", tableId), Restrictions.in("record", recordIds));
    // do not filter by the attachment's organization
    // if the user has access to the record where the file its attached, it has access to all its
    // attachments
    attachmentFiles.setFilterOnReadableOrganization(false);
    if (doExists) {
      // We only want to know if there is at least 1 attachment. Limit the query to 1 record and
      // return the size of the result.
      attachmentFiles.setMaxResults(1);
      return attachmentFiles.list().size();
    }
    return attachmentFiles.count();
  }

  private int computeNoteCount(Tab tab, String rowId) {
    OBQuery<Note> obq = OBDal.getInstance()
        .createQuery(Note.class, " table.id=:tableId and record=:recordId");
    obq.setFilterOnReadableOrganization(false);
    obq.setNamedParameter("tableId", tab.getTable().getId());
    obq.setNamedParameter("recordId", rowId);
    return obq.count();
  }

  private List<String> convertJSONArray(JSONArray jsonArray) {
    List<String> elements = new ArrayList<String>(jsonArray.length());
    for (int i = 0; i < jsonArray.length(); i++) {
      try {
        elements.add(jsonArray.getString(i));
      } catch (JSONException e) {
        throw new OBException("Error while reading the visible properties JSON array");
      }
    }
    return elements;
  }

  private JSONObject buildJSONObject(String mode, Tab tab, Map<String, JSONObject> columnValues,
      BaseOBObject row, List<String> changeEventCols, List<JSONObject> calloutMessages,
      List<JSONObject> attachments, int attachmentCount, List<String> jsExcuteCode,
      Map<String, Object> hiddenInputs, int noteCount, List<String> overwrittenAuxiliaryInputs) {
    JSONObject finalObject = new JSONObject();
    try {
      if ((mode.equals("NEW") || mode.equals("CHANGE")) && !hiddenInputs.isEmpty()) {
        JSONObject jsonHiddenInputs = new JSONObject();
        for (String key : hiddenInputs.keySet()) {
          jsonHiddenInputs.put(key, hiddenInputs.get(key));
        }
        finalObject.put("hiddenInputs", jsonHiddenInputs);
      }
      if (mode.equals("NEW") || mode.equals("EDIT") || mode.equals("CHANGE")) {
        JSONArray arrayMessages = new JSONArray(calloutMessages);
        finalObject.put("calloutMessages", arrayMessages);

        JSONObject jsonColumnValues = new JSONObject();
        for (Field field : getADFieldList(tab.getId())) {
          if (field.getColumn() == null) {
            continue;
          }

          String inpColName = null;
          if (field.getProperty() != null && !field.getProperty().isEmpty()) {
            inpColName = "inp" + "_propertyField_"
                + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
                + field.getColumn().getDBColumnName();
          } else {
            inpColName = "inp" + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
          }
          jsonColumnValues.put(OBViewFieldHandler.getFieldColumnName(field, null),
              columnValues.get(inpColName));
        }
        finalObject.put("columnValues", jsonColumnValues);
      }
      JSONObject jsonAuxiliaryInputValues = new JSONObject();
      for (AuxiliaryInput auxIn : getAuxiliaryInputList(tab.getId())) {
        jsonAuxiliaryInputValues.put(auxIn.getName(),
            columnValues.get("inp" + Sqlc.TransformaNombreColumna(auxIn.getName())));
      }
      finalObject.put("auxiliaryInputValues", jsonAuxiliaryInputValues);
      finalObject.put("overwrittenAuxiliaryInputs", new JSONArray(overwrittenAuxiliaryInputs));

      if (mode.equals("NEW") || mode.equals("EDIT") || mode.equals("SETSESSION")) {
        // We also include information related to validation dependencies
        // and we add the columns which have a callout

        final Map<String, String> sessionAttributesMap = new HashMap<String, String>();

        // Adds to the session attributes the attributes used in
        // the display logic of the tabs
        Window w = tab.getWindow();
        for (Tab aTab : w.getADTabList()) {
          if (aTab.getDisplayLogic() != null && aTab.isActive()) {
            final DynamicExpressionParser parser = new DynamicExpressionParser(
                aTab.getDisplayLogic(), aTab);
            setSessionAttributesFromParserResult(parser, sessionAttributesMap,
                tab.getWindow().getId());
          }
        }

        for (Field field : getADFieldList(tab.getId())) {
          if (field.getColumn() == null) {
            continue;
          }
          if (field.getColumn().getCallout() != null) {
            final String columnName = "inp"
                + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
            if (!changeEventCols.contains(columnName)) {
              changeEventCols.add(columnName);
            }
          }

          // Adding session attributes in a dynamic expression
          // This session attributes could be a preference
          if (field.getDisplayLogic() != null && field.isDisplayed() && field.isActive()) {
            final DynamicExpressionParser parser = new DynamicExpressionParser(
                field.getDisplayLogic(), tab, cachedStructures, field);
            setSessionAttributesFromParserResult(parser, sessionAttributesMap,
                tab.getWindow().getId());
          }
          // We also add session attributes from readonly logic fields
          if (field.getColumn().getReadOnlyLogic() != null && field.isDisplayed()
              && field.isActive()) {
            final DynamicExpressionParser parser = new DynamicExpressionParser(
                field.getColumn().getReadOnlyLogic(), tab, cachedStructures);
            setSessionAttributesFromParserResult(parser, sessionAttributesMap,
                tab.getWindow().getId());
          }

        }

        final JSONObject sessionAttributes = new JSONObject();
        for (String attr : sessionAttributesMap.keySet()) {
          sessionAttributes.put(attr, sessionAttributesMap.get(attr));
        }

        finalObject.put("sessionAttributes", sessionAttributes);
        finalObject.put("dynamicCols", new JSONArray(changeEventCols));
      }

      if ((mode.equals("EDIT") || mode.equals("CHANGE")) && row != null) {
        if (!SecurityChecker.getInstance().isWritable(row)) {
          finalObject.put("_readOnly", true);
        }

        finalObject.put("noteCount", noteCount);
      }
      if (attachments.size() > 0) {
        finalObject.put("attachments", new JSONArray(attachments));
      }
      finalObject.put("attachmentCount", attachmentCount);
      finalObject.put("attachmentExists", attachmentCount > 0);

      if (!jsExcuteCode.isEmpty()) {
        finalObject.put("jscode", new JSONArray(jsExcuteCode));
      }

      log.debug(finalObject.toString(1));
      return finalObject;
    } catch (JSONException e) {
      log.error("Error while generating the final JSON object: ", e);
      return null;
    }
  }

  private void setSessionAttributesFromParserResult(DynamicExpressionParser parser,
      Map<String, String> sessionAttributesMap, String windowId) {
    String attribute = null, attrValue = null;
    for (String attrName : parser.getSessionAttributes()) {
      if (!sessionAttributesMap.containsKey(attrName)) {
        if (attrName.startsWith("inp_propertyField")) {
          // do not add the property fields to the session attributes to avoid overwriting its value
          // with an empty string
          continue;
        }
        if (attrName.startsWith("#")) {
          attribute = attrName.substring(1, attrName.length());
          attrValue = Utility.getContext(new DalConnectionProvider(false),
              RequestContext.get().getVariablesSecureApp(), attribute, windowId);
        } else {
          attrValue = Utility.getContext(new DalConnectionProvider(false),
              RequestContext.get().getVariablesSecureApp(), attrName, windowId);
        }
        sessionAttributesMap.put(attrName.startsWith("#") ? attrName.replace("#", "_") : attrName,
            attrValue);
      }
    }

  }

  private boolean isNotActiveOrVisible(Field field, List<String> visibleProperties) {
    return ((visibleProperties == null || !visibleProperties
        .contains("inp" + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName())))
        && !field.isDisplayed() && !field.isShowInGridView() && !field.isShownInStatusBar())
        || !field.isActive();
  }

  private boolean isNotActiveOrVisibleAndNotNeeded(Field field, List<String> visibleProperties) {
    return isNotActiveOrVisible(field, visibleProperties)
        && field.getColumn().getDefaultValue() == null && !field.getColumn().isMandatory();
  }

  private void computeColumnValues(String mode, Tab tab, List<String> allColumns,
      Map<String, JSONObject> columnValues, BaseOBObject parentRecord, String parentId,
      String changedColumn, JSONObject jsContent, List<String> changeEventCols,
      List<String> calloutsToCall, List<String> lastfieldChanged, List<String> visibleProperties,
      List<String> gridVisibleProperties) {
    boolean forceComboReload = (mode.equals("CHANGE") && changedColumn == null);
    if (mode.equals("CHANGE") && changedColumn != null) {
      RequestContext.get().setRequestParameter("donotaddcurrentelement", "true");
    }
    log.debug("computeColumnValues - forceComboReload: " + forceComboReload);
    HashMap<String, Field> columnsOfFields = new HashMap<String, Field>();
    for (Field field : getADFieldList(tab.getId())) {
      if (field.getColumn() == null) {
        continue;
      }

      String colName = null;
      if (field.getProperty() != null && !field.getProperty().isEmpty()) {
        colName = "_propertyField_" + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "")
            + "_" + field.getColumn().getDBColumnName();
      } else {
        colName = field.getColumn().getDBColumnName();
      }
      columnsOfFields.put(colName, field);
    }
    List<String> changedCols = new ArrayList<String>();
    for (String col : allColumns) {
      if (mode.equals("NEW") && containsIgnoreCase(getAuxiliaryInputNamesList(tab.getId()), col)) {
        // creating a new record, there is an auxiliary input that has the same name than the
        // field's column, in this case auxiliary input is used to calculate the default, so there
        // is no need of calculating it here as it will be done in computeAuxiliaryInputs
        continue;
      }

      checkNamingCollisionWithAuxiliaryInput(tab, col);
      Field field = columnsOfFields.get(col);
      try {
        String columnId = field.getColumn().getId();
        final Property prop = KernelUtils.getInstance().getPropertyFromColumn(field.getColumn());
        UIDefinition uiDef = UIDefinitionController.getInstance().getUIDefinition(columnId);
        String value = null;
        if (mode.equals("NEW")) {
          // On NEW mode, the values are computed through the UIDefinition (the defaults will be
          // used)
          if (field.getProperty() != null && !field.getProperty().isEmpty()) {
            // if the column is a property we try to compute the property value, if value is not
            // found null is passed. Refer issue https://issues.openbravo.com/view.php?id=25754
            Object propertyValue = DalUtil.getValueFromPath(parentRecord, field.getProperty());
            if (propertyValue != null) {
              JSONObject jsonObject = new JSONObject();
              if (propertyValue instanceof BaseOBObject) {
                jsonObject.put("value", ((BaseOBObject) propertyValue).getId());
                jsonObject.put("classicValue", ((BaseOBObject) propertyValue).getId());
                ArrayList<JSONObject> comboEntries = new ArrayList<JSONObject>();
                JSONObject entries = new JSONObject();
                entries.put("id", ((BaseOBObject) propertyValue).getId());
                entries.put("_identifier", ((BaseOBObject) propertyValue).getIdentifier());
                comboEntries.add(entries);
                jsonObject.put("entries", new JSONArray(comboEntries));
              } else {
                jsonObject.put("value", propertyValue.toString());
                jsonObject.put("classicValue", propertyValue.toString());
              }
              value = jsonObject.toString();
            }
          } else {
            if (field.getColumn().isLinkToParentColumn() && parentRecord != null
                && referencedEntityIsParent(parentRecord, field)) {
              // If the column is link to the parent tab, we set its value as the parent id
              RequestContext.get()
                  .setRequestParameter("inp" + Sqlc.TransformaNombreColumna(col), parentId);
              value = uiDef.getFieldProperties(field, true);
            } else if (field.getColumn().getDBColumnName().equalsIgnoreCase("IsActive")) {
              // The Active column is always set to 'true' on new records
              RequestContext.get()
                  .setRequestParameter("inp" + Sqlc.TransformaNombreColumna(col), "Y");
              value = uiDef.getFieldProperties(field, true);
            } else {
              // Else, the default is used
              if (isNotActiveOrVisibleAndNotNeeded(field, visibleProperties)) {
                // If the column is not currently visible, and its not mandatory, we don't need to
                // compute the combo.
                // If a column is mandatory then the combo needs to be computed, because the
                // selected
                // value can depend on the computation if there is no default value
                log.debug("Not calculating combo in " + mode + " mode for column " + col);
                value = uiDef.getFieldPropertiesWithoutCombo(field, false);
              } else {
                if (isNotActiveOrVisible(field, visibleProperties)) {
                  log.debug("Only first combo record in " + mode + " mode for column " + col);
                  value = uiDef.getFieldPropertiesFirstRecord(field, false);
                } else {
                  value = uiDef.getFieldProperties(field, false);
                }
              }
            }
          }
        } else if (mode.equals("EDIT") || (mode.equals("CHANGE")
            && (forceComboReload || changeEventCols.contains(changedColumn)))) {
          // On EDIT mode, the values are computed through the UIDefinition (the values have been
          // previously set in the RequestContext)
          // This is also done this way on CHANGE mode where a combo reload is needed
          if (isNotActiveOrVisibleAndNotNeeded(field, visibleProperties)) {
            // If the column is not currently visible, and its not mandatory, we don't need to
            // compute the combo.
            // If a column is mandatory then the combo needs to be computed, because the selected
            // value can depend on the computation if there is no default value
            log.debug(
                "field: " + field + " - getFieldPropertiesWithoutCombo: hasVisibleProperties: "
                    + (visibleProperties != null) + ", &contains: "
                    + (visibleProperties != null
                        && visibleProperties.contains("inp" + Sqlc.TransformaNombreColumna(col)))
                    + ", isDisplayed=" + field.isDisplayed() + ", isShowInGridView="
                    + field.isShowInGridView() + ", isShownInStatusBar=" + field.isShowInGridView()
                    + ", hasDefaultValue=" + (field.getColumn().getDefaultValue() != null)
                    + ", isMandatory=" + field.getColumn().isMandatory());
            uiDef.getFieldPropertiesWithoutCombo(field, true);
          } else {
            log.debug("field: " + field + " - getFieldProperties: hasVisibleProperties: "
                + (visibleProperties != null) + ", &contains: "
                + (visibleProperties != null
                    && visibleProperties.contains("inp" + Sqlc.TransformaNombreColumna(col)))
                + ", isDisplayed=" + field.isDisplayed() + ", isShowInGridView="
                + field.isShowInGridView() + ", isShownInStatusBar=" + field.isShowInGridView()
                + ", hasDefaultValue=" + (field.getColumn().getDefaultValue() != null)
                + ", isMandatory=" + field.getColumn().isMandatory());
            if (isNotActiveOrVisible(field, visibleProperties)) {
              log.debug("Only first combo record in " + mode + " mode for column " + col);
              value = uiDef.getFieldPropertiesFirstRecord(field, true);
            } else {
              value = uiDef.getFieldProperties(field, true);
            }
          }
        } else if (mode.equals("CHANGE") || mode.equals("SETSESSION")) {
          // On CHANGE and SETSESSION mode, the values are read from the request
          JSONObject jsCol = new JSONObject();
          String colName = "inp" + Sqlc.TransformaNombreColumna(col);
          Object jsonValue = null;
          if (jsContent.has(colName)) {
            jsonValue = jsContent.get(colName);
          } else if (jsContent.has(field.getColumn().getDBColumnName())) {
            // Special case related to the primary key column, which is sent with its dbcolumnname
            // instead of the "inp" name
            jsonValue = jsContent.get(field.getColumn().getDBColumnName());
          }

          if (prop.isPrimitive()) {
            if (JSONObject.NULL.equals(jsonValue)) {
              jsonValue = null;
            }
            if (jsonValue instanceof String) {
              jsCol.put("value", uiDef.createFromClassicString((String) jsonValue));
              jsCol.put("classicValue", jsonValue);
            } else {
              jsCol.put("value", jsonValue);
              jsCol.put("classicValue", uiDef.convertToClassicString(jsonValue));
            }
            value = jsCol.toString();
          } else {
            jsCol.put("value", jsonValue);
            jsCol.put("classicValue", jsonValue);
            value = jsCol.toString();
          }
        }
        JSONObject jsonobject = null;
        if (value != null) {
          jsonobject = new JSONObject(value);
          if (mode.equals("CHANGE")) {
            String oldValue = RequestContext.get()
                .getRequestParameter("inp" + Sqlc.TransformaNombreColumna(col));
            String newValue = jsonobject.has("classicValue") ? jsonobject.getString("classicValue")
                : (jsonobject.has("value") ? jsonobject.getString("value") : null);
            if (newValue == null || newValue.equals("null")) {
              newValue = "";
            }
            if (oldValue == null || oldValue.equals("null")) {
              oldValue = "";
            }
            if (!oldValue.equals(newValue)) {
              changedCols.add(field.getColumn().getDBColumnName());
            }
          }

          String colName = null;
          if (field.getProperty() != null && !field.getProperty().isEmpty()) {
            colName = "_propertyField_"
                + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
                + field.getColumn().getDBColumnName();
          } else {
            colName = Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
          }
          columnValues.put("inp" + colName, jsonobject);
          setRequestContextParameter(field, jsonobject);

          String fullPropertyName = null;
          if (field.getProperty() != null) {
            fullPropertyName = field.getProperty().replace('.', '$');
          } else {
            fullPropertyName = prop.getName();
          }

          // We also set the session value for the column in Edit or SetSession mode
          if (gridVisibleProperties.contains(fullPropertyName)
              && (mode.equals("NEW") || mode.equals("EDIT") || mode.equals("SETSESSION"))) {

            if (field.getColumn().isStoredInSession() || field.getColumn().isKeyColumn()) {
              setSessionValue(
                  tab.getWindow().getId() + "|" + field.getColumn().getDBColumnName().toUpperCase(),
                  jsonobject.has("classicValue") ? jsonobject.get("classicValue") : null);
            }
          }
        }
      } catch (Exception e) {
        log.error("Couldn't get data for column " + field.getColumn().getDBColumnName(), e);
      }
    }

    for (Field field : getADFieldList(tab.getId())) {
      if (field.getColumn() == null) {
        continue;
      }
      String columnId = field.getColumn().getId();
      UIDefinition uiDef = UIDefinitionController.getInstance().getUIDefinition(columnId);
      // We need to fire callouts if the field is a combo
      // (due to how ComboReloads worked, callouts were always called)
      String inpColName = null;
      if (field.getProperty() != null) {
        inpColName = "inp" + "_propertyField_"
            + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
            + field.getColumn().getDBColumnName();
      } else {
        inpColName = "inp" + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
      }
      JSONObject value = columnValues.get(inpColName);
      String classicValue;
      try {
        classicValue = (value == null || !value.has("classicValue")) ? ""
            : value.getString("classicValue");
      } catch (JSONException e) {
        throw new OBException("Couldn't get data for column " + field.getColumn().getDBColumnName(),
            e);
      }
      if (((mode.equals("NEW") && !classicValue.equals("")
          && (uiDef instanceof EnumUIDefinition || uiDef instanceof ForeignKeyUIDefinition))
          || (mode.equals("CHANGE") && changedCols.contains(field.getColumn().getDBColumnName())
              && changedColumn != null))
          && field.getColumn().isValidateOnNew()) {
        if (field.getColumn().getCallout() != null) {
          addCalloutToList(field.getColumn(), calloutsToCall, lastfieldChanged);
        }
      }
    }
  }

  private void checkNamingCollisionWithAuxiliaryInput(Tab tab, String col) {
    List<AuxiliaryInput> auxIns = getAuxiliaryInputList(tab.getId());
    for (AuxiliaryInput auxIn : auxIns) {
      if (Sqlc.TransformaNombreColumna(col).equalsIgnoreCase(auxIn.getName())) {
        log.error("Error: a column and an auxiliary input have the same name in " + tab
            + ". This will lead to wrong computation of values for that column.");
      }
    }

  }

  private void subsequentComboReload(Tab tab, Map<String, JSONObject> columnValues,
      List<String> changedCols, Map<String, List<String>> columnsInValidation) {

    List<String> columnsToComputeAgain = new ArrayList<String>();
    for (String changedCol : changedCols) {
      for (String colWithVal : columnsInValidation.keySet()) {
        for (String colInVal : columnsInValidation.get(colWithVal)) {
          if (colInVal.equalsIgnoreCase(changedCol)) {
            if (!columnsToComputeAgain.contains(colInVal)) {
              columnsToComputeAgain.add(colWithVal);
            }
          }
        }
      }
    }
    HashMap<String, Field> columnsOfFields = new HashMap<String, Field>();
    for (Field field : getADFieldList(tab.getId())) {
      if (field.getColumn() == null) {
        continue;
      }
      for (String col : columnsToComputeAgain) {
        if (col.equalsIgnoreCase(field.getColumn().getDBColumnName())) {
          columnsOfFields.put(col, field);
        }
      }
    }
    for (String col : columnsToComputeAgain) {
      Field field = columnsOfFields.get(col);
      try {
        String columnId = field.getColumn().getId();
        UIDefinition uiDef = UIDefinitionController.getInstance().getUIDefinition(columnId);
        String value = uiDef.getFieldProperties(field, true);
        JSONObject jsonobject = null;
        if (value != null) {
          jsonobject = new JSONObject(value);
          columnValues.put(
              "inp" + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName()),
              jsonobject);
          setRequestContextParameter(field, jsonobject);
        }
      } catch (Exception e) {
        throw new OBException("Couldn't get data for column " + field.getColumn().getDBColumnName(),
            e);
      }
    }

  }

  private void computeAuxiliaryInputs(String mode, Tab tab, List<String> allColumns,
      Map<String, JSONObject> columnValues, List<String> overwrittenAuxiliaryInputs) {
    for (AuxiliaryInput auxIn : getAuxiliaryInputList(tab.getId())) {
      if (mode.equals("CHANGE") || mode.equals("NEW")) {
        // Don't compute the auxiliary inputs that have been overwritten by callouts
        if (overwrittenAuxiliaryInputs.contains(auxIn.getName())) {
          continue;
        }
      }

      if ((mode.equals("EDIT") || mode.equals("CHANGE"))
          && containsIgnoreCase(allColumns, auxIn.getName())) {
        // Don't recalculate auxiliary inputs with same name than a column in the tab because it
        // would overwrite its actual value
        log.debug("Skip aux input in mode " + mode + " " + auxIn.getName());
        continue;
      }
      Object value = computeAuxiliaryInput(auxIn, tab.getWindow().getId());
      log.debug("Final Computed Value. Name: " + auxIn.getName() + " Value: " + value);
      JSONObject jsonObj = new JSONObject();
      try {
        jsonObj.put("value", value);
        jsonObj.put("classicValue", value);
      } catch (JSONException e) {
        log.error("Error while computing auxiliary input " + auxIn.getName(), e);
      }

      columnValues.put("inp" + Sqlc.TransformaNombreColumna(auxIn.getName()), jsonObj);
      RequestContext.get()
          .setRequestParameter("inp" + Sqlc.TransformaNombreColumna(auxIn.getName()),
              value == null || value.equals("null") ? null : value.toString());

      if (mode.equals("NEW") && containsIgnoreCase(allColumns, auxIn.getName())) {
        // auxiliary input used to calculate default value for a field, let's obtain the complete
        // value from ui definition in order to obtain also its identifier
        try {
          Field field = null;
          for (Field f : getADFieldList(tab.getId())) {
            if (f.getColumn() != null
                && auxIn.getName().equalsIgnoreCase(f.getColumn().getDBColumnName())) {
              field = f;
              break;
            }
          }
          if (field != null) {
            String columnId = field.getColumn().getId();
            UIDefinition uiDef = UIDefinitionController.getInstance().getUIDefinition(columnId);
            JSONObject jsonDefinition = new JSONObject(uiDef.getFieldProperties(field, true));
            if (jsonDefinition.has("identifier")) {
              jsonObj.put("identifier", jsonDefinition.get("identifier"));
            }
          }
        } catch (Exception e) {
          log.error("Error trying to calculate identifier for auxiliary input tab " + tab
              + " aux input " + auxIn, e);
        }
      }

      // Now we insert session values for auxiliary inputs
      if (mode.equals("NEW") || mode.equals("EDIT") || mode.equals("SETSESSION")) {
        setSessionValue(tab.getWindow().getId() + "|" + auxIn.getName(), value);
      }
    }
  }

  private BaseOBObject setSessionVariablesInParent(String mode, Tab tab, BaseOBObject row,
      String parentId) {
    // If the FIC is called in CHANGE mode, we don't need to set session variables for the parent
    // records, because those were already set in the previous FIC call (either in NEW or EDIT mode)
    if (mode.equals("CHANGE")) {
      return null;
    }
    BaseOBObject parentRecord = null;
    if (mode.equals("EDIT")) {
      parentRecord = KernelUtils.getInstance().getParentRecord(row, tab);
    }
    Tab parentTab = KernelUtils.getInstance().getParentTab(tab);
    // If the parent table is not based in a db table, don't try to retrieve the record
    // Because tables not based on db tables do not have BaseOBObjects
    // See issue https://issues.openbravo.com/view.php?id=29667
    if (parentId != null && parentTab != null
        && ApplicationConstants.TABLEBASEDTABLE.equals(parentTab.getTable().getDataOriginType())) {
      parentRecord = OBDal.getInstance()
          .get(ModelProvider.getInstance()
              .getEntityByTableName(parentTab.getTable().getDBTableName())
              .getName(), parentId);
    }
    if (parentTab != null && parentRecord != null) {
      setSessionValues(parentRecord, parentTab);
    }
    return parentRecord;
  }

  private void setValuesInRequest(String mode, Tab tab, BaseOBObject row, JSONObject jsContent) {

    boolean tableBasedTable = ApplicationConstants.TABLEBASEDTABLE
        .equals(tab.getTable().getDataOriginType());

    List<Field> fields = getADFieldList(tab.getId());
    // If the table is based on a datasource it is not possible to initialize the values from the
    // database
    if (mode.equals("EDIT") && tableBasedTable) {
      // In EDIT mode we initialize them from the database
      List<Column> columns = getADColumnList(tab.getTable().getId());

      for (Column column : columns) {
        setValueOfColumnInRequest(row, column.getDBColumnName(), tab);
      }
    }

    List<String> gridVisibleProperties = new ArrayList<String>();
    if (jsContent.has("_gridVisibleProperties")) {
      try {
        gridVisibleProperties = convertJSONArray(jsContent.getJSONArray("_gridVisibleProperties"));
      } catch (JSONException e) {
        log.error("Error while retrieving _gridVisibleProperties from jsContent" + jsContent, e);
      }
    }

    // and then overwrite with what gets passed in
    if (mode.equals("EDIT") || mode.equals("CHANGE") || mode.equals("SETSESSION")) {
      // In CHANGE and SETSESSION we get them from the request
      for (Field field : fields) {
        if (field.getColumn() == null) {
          continue;
        }
        // Do not overwrite the value of fields that are not visible in the grid, because they are
        // empty in the request

        final Property prop = KernelUtils.getInstance().getPropertyFromColumn(field.getColumn());
        String fullPropertyName = null;
        String inpColName = null;
        if (field.getProperty() != null) {
          fullPropertyName = field.getProperty().replace('.', '$');
          inpColName = "inp" + "_propertyField_"
              + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
              + field.getColumn().getDBColumnName();
        } else {
          fullPropertyName = prop.getName();
          inpColName = "inp" + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
        }
        if ((mode.equals("EDIT") || mode.equals("SETSESSION"))
            && !gridVisibleProperties.contains(fullPropertyName)) {
          continue;
        }
        try {
          if (jsContent.has(inpColName)) {
            final Object jsonValue = jsContent.get(inpColName);
            String value;
            if (jsonValue == null || jsonValue.toString().equals("null")) {
              value = null;
            } else if (!(jsonValue instanceof String)) {
              final Object propValue = JsonToDataConverter.convertJsonToPropertyValue(prop,
                  jsContent.get(inpColName));
              // convert to a valid classic string
              value = UIDefinitionController.getInstance()
                  .getUIDefinition(field.getColumn().getId())
                  .convertToClassicString(propValue);
            } else {
              value = (String) jsonValue;
            }

            if (value != null && value.equals("null")) {
              value = null;
            }
            RequestContext.get().setRequestParameter(inpColName, value);
          }
        } catch (Exception e) {
          log.error("Couldn't read column value from the request for column " + inpColName, e);
        }
      }
    }

    // We also add special parameters such as the one set by selectors to the request, so the
    // callouts can use them
    addSpecialParameters(tab, jsContent);
  }

  private void addSpecialParameters(Tab tab, JSONObject jsContent) {
    Iterator<?> it = jsContent.keys();
    while (it.hasNext()) {
      String key = it.next().toString();
      try {
        if (RequestContext.get().getRequestParameter(key) == null) {
          String value = jsContent.getString(key);
          if (value != null && value.equals("null")) {
            value = null;
          }
          RequestContext.get().setRequestParameter(key, value);
        }
      } catch (JSONException e) {
        log.error("Couldn't read parameter from the request: " + key, e);
      }
    }
  }

  private boolean referencedEntityIsParent(BaseOBObject parentRecord, Field field) {
    if (field.getColumn() == null) {
      return false;
    }

    Entity parentEntity = parentRecord.getEntity();
    Property property = KernelUtils.getProperty(field);
    Entity referencedEntity = property.getReferencedProperty().getEntity();
    return referencedEntity.equals(parentEntity);
  }

  private void computeListOfColumnsSortedByValidationDependencies(String mode, Tab tab,
      List<String> sortedColumns, Map<String, List<String>> columnsInValidation,
      List<String> changeEventCols, String changedColumn) {
    List<Field> fields = getADFieldList(tab.getId());
    ArrayList<String> columns = new ArrayList<String>();
    List<String> columnsWithValidation = new ArrayList<String>();
    HashMap<String, String> validations = new HashMap<String, String>();
    for (Field field : fields) {
      if (field.getColumn() == null) {
        continue;
      }

      String colName = null;
      if (field.getProperty() != null && !field.getProperty().isEmpty()) {
        colName = "_propertyField_" + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "")
            + "_" + field.getColumn().getDBColumnName();
        columns.add(colName);
      } else {
        colName = field.getColumn().getDBColumnName();
        columns.add(colName.toUpperCase());
      }

      String validation = getValidation(field);
      if (!validation.equals("")) {
        columnsWithValidation.add(colName);
        validations.put(colName, validation);
      }
    }
    for (String column : columnsWithValidation) {
      columnsInValidation.put(column, parseValidation(column, validations.get(column), columns));
      if (log.isDebugEnabled()) {
        String cols = "";
        for (String col : columnsInValidation.get(column)) {
          cols += col + ",";
        }
        log.debug("Column: " + column);
        log.debug("Validation: '" + validations.get(column) + "'");
        log.debug("Columns in validation: '" + cols + "'");
      }
    }

    if (mode.equals("CHANGE") && changedColumn != null && !changedColumn.equals("inpadOrgId")) {
      // In case of a CHANGE event, we only add the changed column, to avoid firing reloads for
      // every column in the tab, instead firing reloads just for the dependant columns
      String changedCol = "";
      for (Field field : fields) {
        if (field.getColumn() == null) {
          continue;
        }
        String colName = field.getColumn().getDBColumnName();
        if (changedColumn.equalsIgnoreCase("inp" + Sqlc.TransformaNombreColumna(colName))) {
          sortedColumns.add(colName);
          changedCol = colName;
        }
      }
      String depColumn = pickDependantColumn(sortedColumns, columnsWithValidation,
          columnsInValidation);
      while (depColumn != null) {
        sortedColumns.add(depColumn);
        depColumn = pickDependantColumn(sortedColumns, columnsWithValidation, columnsInValidation);
      }
      sortedColumns.remove(changedCol);
    } else {
      // Add client and org first to compute dependencies correctly
      for (Field field : fields) {
        if (field.getColumn() == null) {
          continue;
        }
        String colName = field.getColumn().getDBColumnName();
        if (colName.equalsIgnoreCase("Ad_Client_Id")) {
          sortedColumns.add(colName);
        }
      }
      for (Field field : fields) {
        if (field.getColumn() == null) {
          continue;
        }
        String colName = field.getColumn().getDBColumnName();
        if (colName.equalsIgnoreCase("Ad_Org_Id")) {
          sortedColumns.add(colName);
        }
      }
      // we add the columns not included in the sortedColumns
      // (the ones which don't have validations)
      for (Field field : fields) {
        if (field.getColumn() == null) {
          continue;
        }
        String colName = null;
        if (field.getProperty() != null && !field.getProperty().isEmpty()) {
          colName = "_propertyField_"
              + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
              + field.getColumn().getDBColumnName();
        } else {
          colName = field.getColumn().getDBColumnName();
        }
        if (!SequenceUtils.isSequence(field.getColumn()) && !columnsWithValidation.contains(field.getColumn().getDBColumnName())
            && !sortedColumns.contains(colName) && !colName.equalsIgnoreCase("documentno")) {
          sortedColumns.add(colName);
        }
      }
      String nonDepColumn = pickNonDependantColumn(sortedColumns, columnsWithValidation,
          columnsInValidation);
      while (nonDepColumn != null) {
        sortedColumns.add(nonDepColumn);
        nonDepColumn = pickNonDependantColumn(sortedColumns, columnsWithValidation,
            columnsInValidation);
      }
    }
    if (!mode.equalsIgnoreCase("CHANGE")) {
      for (Field field : fields) {
        if (field.getColumn() == null) {
          continue;
        }
        String colName = field.getColumn().getDBColumnName();
        if (SequenceUtils.isSequence(field.getColumn()) || colName.equalsIgnoreCase("documentno")) {
          if (field.getProperty() != null && !field.getProperty().isEmpty()) {
            colName = "_propertyField_"
                + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
                + field.getColumn().getDBColumnName();
          }
          sortedColumns.add(colName);
        }
      }

      String validationErrors = "";
      for (String col : columnsWithValidation) {
        if (!sortedColumns.contains(col)) {
          if (!validationErrors.isEmpty()) {
            validationErrors += " -- ";
          }
          validationErrors += col + " column has a validation that depends on columns "
              + columnsInValidation.get(col) + " which creates a cycle";
        }
      }
      if (!validationErrors.equals("")) {
        throw new OBException(validationErrors + " -- List of sorted columns: " + sortedColumns,
            false);
      }
    }
    String finalCols = "";
    for (String col : sortedColumns) {
      finalCols += col + ",";
    }
    log.debug("Final order of column computation: " + finalCols);

    // We also fill the changeEventCols
    // These are the columns which should trigger a CHANGE request to the FIC (because either they
    // require a combo reload because they are used in a validation, or there is a callout
    // associated with them)
    for (Field field : fields) {
      if (field.getColumn() == null) {
        continue;
      }
      String column = field.getColumn().getDBColumnName();
      String columninp = "inp" + Sqlc.TransformaNombreColumna(column);
      if (column.equalsIgnoreCase("Ad_Org_Id") && !changeEventCols.contains(columninp)) {
        changeEventCols.add(columninp);
      }
      if (columnsInValidation.get(column) != null && columnsInValidation.get(column).size() > 0) {
        for (String colInVal : columnsInValidation.get(column)) {
          final String columnName = "inp" + Sqlc.TransformaNombreColumna(colInVal);
          if (!changeEventCols.contains(columnName)) {
            changeEventCols.add(columnName);
          }
        }
      }
    }
  }

  private void setValueOfColumnInRequest(BaseOBObject obj, String columnName, Tab tab) {
    Entity entity = obj.getEntity();
    Property prop = entity.getPropertyByColumnName(columnName);
    Object currentValue = obj.get(prop.getName());

    try {
      if (currentValue != null && !currentValue.toString().equals("null")) {
        if (currentValue instanceof BaseOBObject) {
          if (prop.getReferencedProperty() != null) {
            currentValue = ((BaseOBObject) currentValue)
                .get(prop.getReferencedProperty().getName());
          } else {
            currentValue = ((BaseOBObject) currentValue).getId();
          }
        } else {
          currentValue = UIDefinitionController.getInstance()
              .getUIDefinition(prop.getColumnId())
              .convertToClassicString(currentValue);
        }
        if (currentValue != null && currentValue.equals("null")) {
          currentValue = null;
        }
        if (currentValue == null) {
          RequestContext.get()
              .setRequestParameter("inp" + Sqlc.TransformaNombreColumna(columnName), null);
        } else {
          RequestContext.get()
              .setRequestParameter("inp" + Sqlc.TransformaNombreColumna(columnName),
                  currentValue.toString());
        }
      } else {
        RequestContext.get()
            .setRequestParameter("inp" + Sqlc.TransformaNombreColumna(columnName), null);
      }
    } catch (Exception ignore) {
      String msg = "Could not get value for column: " + columnName + " - tab: " + tab;
      if (obj != null) {
        msg += " - row: " + obj.getId();
      }

      log.error(msg, ignore);
    }
  }

  private void setSessionValues(BaseOBObject object, Tab tab) {
    for (Column col : getADColumnList(tab.getTable().getId())) {
      if (col.isStoredInSession() || col.isKeyColumn()) {
        Property prop = object.getEntity().getPropertyByColumnName(col.getDBColumnName());
        Object value = object.get(prop.getName());
        if (value != null) {
          if (value instanceof BaseOBObject) {
            if (prop.getReferencedProperty() != null) {
              value = ((BaseOBObject) value).get(prop.getReferencedProperty().getName());
            } else {
              value = ((BaseOBObject) value).getId();
            }
          } else {
            value = UIDefinitionController.getInstance()
                .getUIDefinition(col.getId())
                .convertToClassicString(value);
          }
          setSessionValue(tab.getWindow().getId() + "|" + col.getDBColumnName(), value);
        }
        // We also set the value of every column in the RequestContext so that it is available for
        // the Auxiliary Input computation
        setValueOfColumnInRequest(object, col.getDBColumnName(), tab);
      }
    }
    List<AuxiliaryInput> auxInputs = getAuxiliaryInputList(tab.getId());
    for (AuxiliaryInput auxIn : auxInputs) {
      Object value = computeAuxiliaryInput(auxIn, tab.getWindow().getId());
      setSessionValue(tab.getWindow().getId() + "|" + auxIn.getName(), value);
    }
    Tab parentTab = KernelUtils.getInstance().getParentTab(tab);
    BaseOBObject parentRecord = KernelUtils.getInstance().getParentRecord(object, tab);
    if (parentTab != null && parentRecord != null) {
      setSessionValues(parentRecord, parentTab);
    }
  }

  private void setSessionValue(String key, Object value) {
    log.debug("Setting session value. Key: " + key + "  Value:" + value + " - type "
        + (value != null ? value.getClass() : null));
    RequestContext.get().setSessionAttribute(key, value != null ? value.toString() : null);
  }

  private void setRequestContextParameter(Field field, JSONObject jsonObj) {
    if (field.getColumn() == null) {
      return;
    }

    try {
      String fieldId = "inp" + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
      if (field.getProperty() != null && !field.getProperty().isEmpty()) {
        fieldId = "inp" + "_propertyField_"
            + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
            + field.getColumn().getDBColumnName();
      }
      RequestContext.get()
          .setRequestParameter(fieldId,
              jsonObj.has("classicValue") && jsonObj.get("classicValue") != null
                  && !jsonObj.getString("classicValue").equals("null")
                      ? jsonObj.getString("classicValue")
                      : null);
    } catch (JSONException e) {
      log.error("Couldn't read JSON parameter for column " + field.getColumn().getDBColumnName());
    }
  }

  private HashMap<String, Field> buildInpField(List<Field> fields) {
    HashMap<String, Field> inpFields = new HashMap<String, Field>();
    for (Field field : fields) {
      if (field.getColumn() == null) {
        continue;
      }
      String fieldId = null;
      if (field.getProperty() != null && !field.getProperty().isEmpty()) {
        fieldId = "inp" + "_propertyField_"
            + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
            + field.getColumn().getDBColumnName();
      } else {
        fieldId = "inp" + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
      }
      inpFields.put(fieldId, field);
    }
    return inpFields;
  }

  private List<String> executeCallouts(String mode, Tab tab, Map<String, JSONObject> columnValues,
      String changedColumn, List<String> calloutsToCall, List<String> lastfieldChanged,
      List<JSONObject> messages, List<String> dynamicCols, List<String> jsExecuteCode,
      Map<String, Object> hiddenInputs, List<String> overwrittenAuxiliaryInputs) {

    // In CHANGE mode, we will add the initial callout call for the changed column, if there is
    // one
    if (mode.equals("CHANGE")) {
      if (changedColumn != null) {
        for (Column col : getADColumnList(tab.getTable().getId())) {
          if (("inp" + Sqlc.TransformaNombreColumna(col.getDBColumnName())).equals(changedColumn)) {
            if (col.getCallout() != null) {
              // The column has a callout. We will add the callout to the callout list
              addCalloutToList(col, calloutsToCall, lastfieldChanged);
            }
          }
        }
      }
    }

    ArrayList<String> calledCallouts = new ArrayList<String>();
    if (calloutsToCall.isEmpty()) {
      return new ArrayList<String>();
    }
    return runCallouts(columnValues, tab, calledCallouts, calloutsToCall, lastfieldChanged,
        messages, dynamicCols, jsExecuteCode, hiddenInputs, overwrittenAuxiliaryInputs);

  }

  private List<String> runCallouts(Map<String, JSONObject> columnValues, Tab tab,
      List<String> calledCallouts, List<String> calloutsToCall, List<String> lastfieldChangedList,
      List<JSONObject> messages, List<String> dynamicCols, List<String> jsExecuteCode,
      Map<String, Object> hiddenInputs, List<String> overwrittenAuxiliaryInputs) {
    HashMap<String, Object> calloutInstances = new HashMap<String, Object>();
    List<String> changedCols = new ArrayList<String>();
    List<Field> fields = getADFieldList(tab.getId());
    HashMap<String, Field> inpFields = buildInpField(fields);
    String lastCalledCallout = "";
    String lastFieldOfLastCalloutCalled = "";

    while (!calloutsToCall.isEmpty() && calledCallouts.size() < MAX_CALLOUT_CALLS) {
      String calloutClassName = calloutsToCall.get(0);
      String lastFieldChanged = lastfieldChangedList.get(0);
      if (calloutClassName.equals(lastCalledCallout)
          && lastFieldChanged.equals(lastFieldOfLastCalloutCalled)) {
        log.debug("Callout filtered: " + calloutClassName);
        calloutsToCall.remove(calloutClassName);
        lastfieldChangedList.remove(lastFieldChanged);
        continue;
      }
      log.debug("Calling callout " + calloutClassName + " with field changed " + lastFieldChanged);
      Class<?> calloutClass;
      try {
        calloutClass = Class.forName(calloutClassName);
      } catch (ClassNotFoundException e) {
        throw new OBException("Couldn't find class " + calloutClassName, e);
      }
      try {
        calloutsToCall.remove(calloutClassName);
        lastfieldChangedList.remove(lastFieldChanged);

        Object calloutObject;
        if (calloutInstances.get(calloutClassName) != null) {
          calloutObject = calloutInstances.get(calloutClassName);
        } else {
          calloutObject = calloutClass.getDeclaredConstructor().newInstance();
          calloutInstances.put(calloutClassName, calloutObject);
        }

        if (!(calloutObject instanceof SimpleCallout)) {
          log.error(
              "Callout {} in (Window, Tab, Field) ({}, {}, {}), only reference instances of type SimpleCallout are allowed."
                  + " The callout is an instance of {} class.",
              calloutClassName, tab.getWindow().getName(), tab.getName(), lastFieldChanged,
              calloutObject.getClass().getName());
          continue;
        }

        RequestContext request = RequestContext.get();
        RequestContext.get().setRequestParameter("inpLastFieldChanged", lastFieldChanged);
        RequestContext.get().setRequestParameter("inpOB3UIMode", "Y");
        CalloutServletConfig config = new CalloutServletConfig(calloutClassName,
            RequestContext.getServletContext());
        CalloutInformationProvider calloutResponseManager = null;

        // execute SimpleCallout callouts
        if (SimpleCallout.class.isAssignableFrom(calloutClass)) {

          SimpleCallout calloutInstance = (SimpleCallout) calloutObject;
          calloutInstance.init(config);

          // execute SimpleCallout callout
          JSONObject result = calloutInstance.executeSimpleCallout(request);

          // updated info values of callouts infrastructure
          String calloutNameJS = calloutClassName.substring(calloutClassName.lastIndexOf(".") + 1);
          calledCallouts.add(calloutNameJS);

          calloutResponseManager = new SimpleCalloutInformationProvider(result);
        }

        manageUpdatedValuesForCallout(columnValues, tab, calloutsToCall, lastfieldChangedList,
            messages, dynamicCols, jsExecuteCode, hiddenInputs, overwrittenAuxiliaryInputs,
            changedCols, inpFields, calloutClassName, request, calloutResponseManager);

        lastCalledCallout = calloutClassName;
        lastFieldOfLastCalloutCalled = lastFieldChanged;
      } catch (Exception e) {
        throw new OBException("Couldn't execute callout (class " + calloutClassName + ")", e);
      }
    }
    if (calledCallouts.size() == MAX_CALLOUT_CALLS) {
      log.warn("Warning: maximum number of callout calls reached");
    }
    return changedCols;

  }

  private void manageUpdatedValuesForCallout(Map<String, JSONObject> columnValues, Tab tab,
      List<String> calloutsToCall, List<String> lastfieldChangedList, List<JSONObject> messages,
      List<String> dynamicCols, List<String> jsExecuteCode, Map<String, Object> hiddenInputs,
      List<String> overwrittenAuxiliaryInputs, List<String> changedCols,
      HashMap<String, Field> inpFields, String calloutClassName, RequestContext request,
      CalloutInformationProvider calloutInformationProvider) throws JSONException {
    Object element = calloutInformationProvider.getNextElement();
    while (element != null) {
      String name = calloutInformationProvider.getCurrentElementName();
      if (name.equals("MESSAGE") || name.equals("INFO") || name.equals("WARNING")
          || name.equals("ERROR") || name.equals("SUCCESS")) {
        log.debug("Callout message: " + calloutInformationProvider.getCurrentElementValue(element));
        JSONObject message = new JSONObject();
        message.put("text", calloutInformationProvider.getCurrentElementValue(element).toString());
        message.put("severity", name.equals("MESSAGE") ? "TYPE_INFO" : "TYPE_" + name);
        messages.add(message);
      } else if (name.equals("JSEXECUTE")) {
        // The code on a JSEXECUTE command is sent directly to the client for eval()
        String code = (String) calloutInformationProvider.getCurrentElementValue(element);
        if (code != null) {
          jsExecuteCode.add(code);
        }
      } else if (name.equals("EXECUTE")) {
        String js = calloutInformationProvider.getCurrentElementValue(element) == null ? null
            : calloutInformationProvider.getCurrentElementValue(element).toString();
        if (js != null && !js.equals("")) {
          if (js.equals("displayLogic();")) {
            // We don't do anything, this is a harmless js response
          } else {
            JSONObject message = new JSONObject();
            message.put("text",
                Utility.messageBD(new DalConnectionProvider(false), "OBUIAPP_ExecuteInCallout",
                    RequestContext.get().getVariablesSecureApp().getLanguage()));
            message.put("severity", "TYPE_ERROR");
            messages.add(message);
            log.warn("Callout " + calloutClassName
                + " returned EXECUTE command which is no longer supported, it should be fixed. Window-tab: "
                + tab.getWindow().getName() + " - " + tab.getName());
          }
        }
      } else {
        if (name.startsWith("inp")) {
          boolean changed = false;
          if (inpFields.containsKey(name)) {
            Column col = inpFields.get(name).getColumn();
            if (col != null) {
              String colId = "inp" + Sqlc.TransformaNombreColumna(col.getDBColumnName());
              if (calloutInformationProvider.isComboData(element)) {
                // Combo data
                calloutInformationProvider.manageComboData(columnValues, dynamicCols, changedCols,
                    request, element, col, colId);
                // When managing combos, it is not taken into account if the column value has
                // changed, so 'changed' is always true.
                changed = true;
              } else {
                // Normal data
                Object el = calloutInformationProvider.getCurrentElementValue(element);
                String oldValue = request.getRequestParameter(colId);
                // We set the new value in the request, so that the JSONObject is computed
                // with the new value
                UIDefinition uiDef = UIDefinitionController.getInstance()
                    .getUIDefinition(col.getId());
                if (el instanceof String
                    || !(uiDef.getDomainType() instanceof PrimitiveDomainType)) {
                  request.setRequestParameter(colId, el == null ? null : el.toString());
                } else {
                  request.setRequestParameter(colId, uiDef.convertToClassicString(el));
                }
                String jsonStr = uiDef.getFieldProperties(inpFields.get(name), true);
                JSONObject jsonobj = new JSONObject(jsonStr);
                if (el == null && (uiDef instanceof ForeignKeyUIDefinition
                    || uiDef instanceof EnumUIDefinition)) {
                  // Special case for null values for combos: we must clean the combo values
                  jsonobj.put(CalloutConstants.VALUE, "");
                  jsonobj.put(CalloutConstants.CLASSIC_VALUE, "");
                  jsonobj.put(CalloutConstants.ENTRIES, new JSONArray());
                }
                if (jsonobj.has(CalloutConstants.CLASSIC_VALUE)) {
                  String newValue = jsonobj.getString(CalloutConstants.CLASSIC_VALUE);
                  log.debug("Modified column: " + col.getDBColumnName() + "  Value: " + el);
                  if ((oldValue == null && newValue != null)
                      || (oldValue != null && newValue == null)
                      || (oldValue != null && newValue != null && !oldValue.equals(newValue))) {
                    columnValues.put("inp" + Sqlc.TransformaNombreColumna(col.getDBColumnName()),
                        jsonobj);
                    changed = true;
                    if (dynamicCols.contains(colId)) {
                      changedCols.add(col.getDBColumnName());
                    }
                    request.setRequestParameter(colId,
                        jsonobj.getString(CalloutConstants.CLASSIC_VALUE));
                  }
                } else {
                  log.debug(
                      "Column value didn't change. We do not attempt to execute any additional callout");
                }
              }
              if (changed && col.getCallout() != null) {
                // We need to fire this callout, as the column value was changed
                // but only if the callout we are firing is different
                if (isShouldBeFired(calloutClassName, col)) {
                  addCalloutToList(col, calloutsToCall, lastfieldChangedList);
                }
              }
            }
          } else {
            for (AuxiliaryInput aux : tab.getADAuxiliaryInputList()) {
              if (name.equalsIgnoreCase("inp" + Sqlc.TransformaNombreColumna(aux.getName()))) {
                Object el = calloutInformationProvider.getCurrentElementValue(element);
                JSONObject obj = new JSONObject();
                obj.put(CalloutConstants.VALUE, el);
                obj.put(CalloutConstants.CLASSIC_VALUE, el);
                columnValues.put(name, obj);
                // Add the auxiliary input to the list of auxiliary inputs modified by
                // callouts
                if (!overwrittenAuxiliaryInputs.contains(aux.getName())) {
                  overwrittenAuxiliaryInputs.add(aux.getName());
                }
              }
            }
            if (!columnValues.containsKey(name)) {
              // This returned value wasn't found to be either a column or an auxiliary
              // input. We assume it is a hidden input, which are used in places like
              // selectors
              Object el = calloutInformationProvider.getCurrentElementValue(element);
              if (el != null) {
                if (calloutInformationProvider.isComboData(element)) {
                  // In this case, we ignore the value, as a hidden input cannot be an array
                  // of elements
                } else {
                  hiddenInputs.put(name, el);
                  // We set the hidden fields in the request, so that subsequent callouts
                  // can use them
                  request.setRequestParameter(name, el.toString());
                }
              }
            }
          }
        }
      }
      element = calloutInformationProvider.getNextElement();
    }
  }

  /**
   * This callout should be fire only if the callout we are firing is different.
   * 
   * @param calloutClassName
   *          callout that is firing
   * @return true if it is should be fired.
   */
  private boolean isShouldBeFired(String calloutClassName, Column col) {
    return !calloutClassName
        .equals(col.getCallout().getADModelImplementationList().get(0).getJavaClassName());
  }

  private void addCalloutToList(Column col, List<String> listOfCallouts,
      List<String> lastFieldChangedList) {
    if (col.getCallout().getADModelImplementationList() == null
        || col.getCallout().getADModelImplementationList().size() == 0) {
      log.info("The callout of the column " + col.getDBColumnName()
          + " doesn't have a corresponding model object, and therefore cannot be executed.");
    } else {
      String calloutClassNameToCall = col.getCallout()
          .getADModelImplementationList()
          .get(0)
          .getJavaClassName();
      listOfCallouts.add(calloutClassNameToCall);
      lastFieldChangedList.add("inp" + Sqlc.TransformaNombreColumna(col.getDBColumnName()));
    }
  }

  private String pickDependantColumn(List<String> sortedColumns, List<String> columns,
      Map<String, List<String>> columnsInValidation) {
    for (String col : columns) {
      if (sortedColumns.contains(col)) {
        continue;
      }
      for (String depCol : columnsInValidation.get(col)) {
        if (containsIgnoreCase(sortedColumns, depCol)) {
          return col;
        }
      }
    }

    return null;
  }

  private String pickNonDependantColumn(List<String> sortedColumns, List<String> columns,
      Map<String, List<String>> columnsInValidation) {
    for (String col : columns) {
      if (sortedColumns.contains(col)) {
        continue;
      }
      if (columnsInValidation.get(col) == null || columnsInValidation.get(col).isEmpty()) {
        return col;
      }
      boolean allColsSorted = true;
      for (String depCol : columnsInValidation.get(col)) {
        if (!containsIgnoreCase(sortedColumns, depCol)) {
          allColsSorted = false;
        }
      }
      if (allColsSorted) {
        return col;
      }
    }

    return null;
  }

  private boolean containsIgnoreCase(List<String> list, String element) {
    for (String e : list) {
      if (e.equalsIgnoreCase(element)) {
        return true;
      }
    }
    return false;
  }

  private String getValidation(Field field) {
    if (field.getColumn() == null) {
      return "";
    }

    Column c = field.getColumn();
    String val = "";
    if (c.getValidation() != null && c.getValidation().getValidationCode() != null) {
      val += c.getValidation().getValidationCode();
    }
    if (c.getReference().getId().equals("18")) {
      if (c.getReferenceSearchKey() != null) {
        for (ReferencedTable t : c.getReferenceSearchKey().getADReferencedTableList()) {
          val += " AND " + t.getSQLWhereClause();
        }
      }
    }
    return val;

  }

  private ArrayList<String> parseValidation(String column, String validation,
      List<String> possibleColumns) {
    String token = validation;
    ArrayList<String> columns = new ArrayList<String>();
    int i = token.indexOf("@");
    while (i != -1) {
      token = token.substring(i + 1);
      if (!token.startsWith("SQL")) {
        i = token.indexOf("@");
        if (i != -1) {
          String strAux = token.substring(0, i);
          token = token.substring(i + 1);
          if (!columns.contains(strAux)) {
            if (!strAux.equalsIgnoreCase(column)
                && possibleColumns.contains(strAux.toUpperCase())) {
              columns.add(strAux);
            }
          }
        }
      }
      i = token.indexOf("@");
    }
    return columns;
  }

  private Object computeAuxiliaryInput(AuxiliaryInput auxIn, String windowId) {
    try {
      String code = auxIn.getValidationCode();
      log.debug("Auxiliary Input: " + auxIn.getName() + " Code:" + code);
      Object fvalue = null;
      if (code.startsWith("@SQL=")) {
        ArrayList<String> params = new ArrayList<String>();
        String sql = UIDefinition.parseSQL(code, params);
        // final StringBuffer parametros = new StringBuffer();
        // for (final Enumeration<String> e = params.elements(); e.hasMoreElements();) {
        // String paramsElement = WadUtility.getWhereParameter(e.nextElement(), true);
        // parametros.append("\n" + paramsElement);
        // }
        log.debug("Transformed SQL code: " + sql);
        int indP = 1;
        PreparedStatement ps = OBDal.getInstance().getConnection(false).prepareStatement(sql);
        try {
          for (String parameter : params) {
            String value = "";
            if (parameter.substring(0, 1).equals("#")) {
              value = Utility.getContext(new DalConnectionProvider(false),
                  RequestContext.get().getVariablesSecureApp(), parameter, windowId);
            } else {
              String fieldId = "inp" + Sqlc.TransformaNombreColumna(parameter);
              value = RequestContext.get().getRequestParameter(fieldId);
            }
            log.debug("Parameter: " + parameter + ": Value " + value);
            ps.setObject(indP++, value);
          }
          ResultSet rs = ps.executeQuery();
          if (rs.next()) {
            fvalue = rs.getObject(1);
          }
        } finally {
          ps.close();
        }
      } else if (code.startsWith("@")) {
        String codeWithoutAt = code.substring(1, code.length() - 1);
        fvalue = Utility.getContext(new DalConnectionProvider(false),
            RequestContext.get().getVariablesSecureApp(), codeWithoutAt, windowId);
      } else {
        fvalue = code;
      }
      return fvalue;
    } catch (Exception e) {
      log.error(
          "Error while computing auxiliary input parameter: " + auxIn.getName() + " from tab: "
              + auxIn.getTab().getName() + " of window: " + auxIn.getTab().getWindow().getName(),
          e);
    }
    return null;
  }

  private Tab getTab(String tabId) {
    return cachedStructures.getTab(tabId);
  }

  private List<Field> getADFieldList(String tabId) {
    return cachedStructures.getFieldsOfTab(tabId);
  }

  private List<Column> getADColumnList(String tableId) {
    return cachedStructures.getColumnsOfTable(tableId);
  }

  private List<AuxiliaryInput> getAuxiliaryInputList(String tabId) {
    return cachedStructures.getAuxiliarInputList(tabId);
  }

  private List<String> getAuxiliaryInputNamesList(String tabId) {
    List<String> result = new ArrayList<String>();
    for (AuxiliaryInput ai : cachedStructures.getAuxiliarInputList(tabId)) {
      result.add(ai.getName());
    }
    return result;
  }

  private String readParameter(Map<String, Object> parameters, String parameterName) {
    String paramValue = (String) parameters.get(parameterName);
    if (paramValue != null && paramValue.equalsIgnoreCase("null")) {
      paramValue = null;
    }
    return paramValue;
  }
}
