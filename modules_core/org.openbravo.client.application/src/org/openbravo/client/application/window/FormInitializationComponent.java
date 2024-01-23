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

import com.etendoerp.sequences.SequenceUtils;

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
  private static final String CHANGE = "CHANGE";
  private static final String VALUE = "value";
  private static final String SET_SESSION = "SETSESSION";
  private static final String CLASSIC_VALUE = "classicValue";
  private static final String EDIT = "EDIT";
  private static final String NEW = "NEW";
  private static final String NULL = "null";
  private static final String INP = "inp";
  private static final String IDENTIFIER = "identifier";
  private static final String GRID_VISIBLE_PROPERTIES = "_gridVisibleProperties";
  private static final String PROPERTY_FIELD = "_propertyField_";
  private static final String VISIBLE_PROPERTIES = "_visibleProperties";
  private static final String DO_NOT_ADD_CURRENT_ELEMENT = "donotaddcurrentelement";
  private static final String TRUE = "true";
  private static final String MULTIPLE_ROW_IDS = "MULTIPLE_ROW_IDS";
  private static final String ENTRIES = "entries";
  private static final String DOCUMENTNO = "documentno";
  private static final String MESSAGE = "MESSAGE";
  private static final String TEXT = "text";
  private static final String OVER_WRITTEN_AUXILIARY_INPUTS = "overwrittenAuxiliaryInputs";
  private static final String ID = "id";
  private static final String AD_ORG_ID = "Ad_Org_Id";
  private static final String SEVERITY = "severity";

  @Inject
  private ApplicationDictionaryCachedStructures cachedStructures;

  @Inject
  @Any
  private Instance<FICExtension> ficExtensions;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    OBContext.setAdminMode(true);
    long iniTime = System.currentTimeMillis();
    String mode = null;
    String tabId = null;
    String rowId = null;
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
      Map<String, JSONObject> columnValues = new HashMap<>();
      List<String> allColumns = new ArrayList<>();
      List<String> calloutsToCall = new ArrayList<>();
      List<String> lastFieldChanged = new ArrayList<>();
      List<String> changeEventCols = new ArrayList<>();
      Map<String, List<String>> columnsInValidation = new HashMap<>();
      List<JSONObject> calloutMessages = new ArrayList<>();
      List<String> jsExcuteCode = new ArrayList<>();
      Map<String, Object> hiddenInputs = new HashMap<>();

      boolean databaseBasedTable = StringUtils
          .equals(tab.getTable().getDataOriginType(), ApplicationConstants.TABLEBASEDTABLE);

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
      if (databaseBasedTable && rowId != null && !StringUtils.equals(NULL, rowId)) {
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
      if (jsContent.has(VISIBLE_PROPERTIES)) {
        visibleProperties = convertJSONArray(jsContent.getJSONArray(VISIBLE_PROPERTIES));
      }
      List<String> gridVisibleProperties = new ArrayList<>();
      if (jsContent.has(GRID_VISIBLE_PROPERTIES)) {
        gridVisibleProperties = convertJSONArray(jsContent.getJSONArray(GRID_VISIBLE_PROPERTIES));
      }

      List<String> overwrittenAuxiliaryInputs = new ArrayList<>();
      // The provided overwrittenAuxiliaryInputs only have to be persisted when calling the FIC in
      // CHANGE mode. In the rest of the modes all auxiliary inputs are computed regardless of
      // whether a callout have modified them in a previous request with the exception of NEW. In
      // NEW mode auxiliary inputs are not recomputed if they were previously calculated by callouts
      // (within in the same request)
      if (jsContent.has(OVER_WRITTEN_AUXILIARY_INPUTS) && StringUtils.equals(CHANGE, mode)) {
        overwrittenAuxiliaryInputs = convertJSONArray(
            jsContent.getJSONArray(OVER_WRITTEN_AUXILIARY_INPUTS));
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
              final String inpName = INP + Sqlc.TransformaNombreColumna(property.getColumnName());
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
          convertedJson.remove(ID);
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
          changedColumn, jsContent, changeEventCols, calloutsToCall, lastFieldChanged,
          visibleProperties, gridVisibleProperties);

      // Execution of callouts
      long t6 = System.currentTimeMillis();
      List<String> changedCols = executeCallouts(mode, tab, columnValues, changedColumn,
          calloutsToCall, lastFieldChanged, calloutMessages, changeEventCols, jsExcuteCode,
          hiddenInputs, overwrittenAuxiliaryInputs);

      // Compute the auxiliary inputs after executing the callout to ensure they use the updated
      // parameters
      if (StringUtils.equals(NEW, mode) || StringUtils.equals(CHANGE, mode)) {
        // In the case of NEW mode, we compute auxiliary inputs again to take into account that
        // auxiliary inputs could depend on a default value
        computeAuxiliaryInputs(mode, tab, allColumnsInTab, columnValues,
            overwrittenAuxiliaryInputs);
      }

      if (!changedCols.isEmpty()) {
        RequestContext.get().setRequestParameter(DO_NOT_ADD_CURRENT_ELEMENT, TRUE);
        subsequentComboReload(tab, columnValues, changedCols, columnsInValidation);
      }

      // Attachment information
      long t7 = System.currentTimeMillis();
      List<JSONObject> attachments = new ArrayList<>();
      int attachmentCount = 0;
      if (jsContent.has(MULTIPLE_ROW_IDS)) {
        attachmentCount = computeAttachmentCount(tab,
            convertJSONArray(jsContent.getJSONArray(MULTIPLE_ROW_IDS)), true);
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

  /**
   * Returns an unsorted list of all columns present in the tab
   */
  private List<String> getAllColumnsInTab(Tab tab) {
    List<String> allColumns = new ArrayList<>();
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
    StringBuilder heavyCols = new StringBuilder();
    for (String col : columnValues.keySet()) {
      if (columnValues.get(col).has(ENTRIES)) {
        try {
          JSONArray array = columnValues.get(col).getJSONArray(ENTRIES);
          if (array.length() > maxEntries) {
            if (i > 0) {
              heavyCols.append(",");
            }
            heavyCols.append(col);
            i++;
          }
        } catch (JSONException e) {
          log.error("There was an error while analyzing the response for field: " + col);
        }
      }
    }
    if (!StringUtils.isBlank(heavyCols.toString())) {
      log.warn("Warning: In the window " + tab.getWindow().getName() + ", in tab " + tab.getName()
          + " the combo fields " + heavyCols + " contain more than " + maxEntries
          + " entries, and this could cause bad performance in the application. Possible fixes include changing these columns from a combo into a Selector, or adding a validation to reduce the number of entries in the combo.");
    }
  }

  /**
   * Get JSONObject list with data of the attachments in given tab and records
   *
   * @param tab
   *     tab to take attachments
   * @param recordIds
   *     list of record IDs where taken attachments
   * @param doExists
   *     flag to not return the actual count just 1 or 0
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
    List<String> elements = new ArrayList<>(jsonArray.length());
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
      BaseOBObject row, List<String> changeEventCols, List<JSONObject> callOutMessages,
      List<JSONObject> attachments, int attachmentCount, List<String> jsExcuteCode,
      Map<String, Object> hiddenInputs, int noteCount, List<String> overwrittenAuxiliaryInputs) {
    JSONObject finalObject = new JSONObject();
    try {
      if ((StringUtils.equals(NEW, mode) || StringUtils.equals(CHANGE, mode)) && !hiddenInputs.isEmpty()) {
        JSONObject jsonHiddenInputs = new JSONObject();
        for (String key : hiddenInputs.keySet()) {
          jsonHiddenInputs.put(key, hiddenInputs.get(key));
        }
        finalObject.put("hiddenInputs", jsonHiddenInputs);
      }
      if (StringUtils.equals(NEW, mode) || StringUtils.equals(EDIT, mode) || StringUtils.equals(CHANGE, mode)) {
        if (!callOutMessages.isEmpty()) {
          for (int i = 0; i < callOutMessages.size(); i++) {
            if (callOutMessages.get(i).getString(TEXT).isBlank()) {
              callOutMessages.remove(i);
            }
          }
        }
        JSONArray arrayMessages = new JSONArray(callOutMessages);
        finalObject.put("calloutMessages", arrayMessages);

        JSONObject jsonColumnValues = new JSONObject();
        for (Field field : getADFieldList(tab.getId())) {
          if (field.getColumn() == null) {
            continue;
          }

          String inpColName = null;
          if (field.getProperty() != null && !field.getProperty().isEmpty()) {
            inpColName = INP + PROPERTY_FIELD
                + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
                + field.getColumn().getDBColumnName();
          } else {
            inpColName = INP + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
          }
          jsonColumnValues.put(OBViewFieldHandler.getFieldColumnName(field, null),
              columnValues.get(inpColName));
        }
        finalObject.put("columnValues", jsonColumnValues);
      }
      JSONObject jsonAuxiliaryInputValues = new JSONObject();
      for (AuxiliaryInput auxIn : getAuxiliaryInputList(tab.getId())) {
        jsonAuxiliaryInputValues.put(auxIn.getName(),
            columnValues.get(INP + Sqlc.TransformaNombreColumna(auxIn.getName())));
      }
      finalObject.put("auxiliaryInputValues", jsonAuxiliaryInputValues);
      finalObject.put(OVER_WRITTEN_AUXILIARY_INPUTS, new JSONArray(overwrittenAuxiliaryInputs));

      if (StringUtils.equals(NEW, mode) || StringUtils.equals(EDIT, mode) || StringUtils.equals(SET_SESSION, mode)) {
        // We also include information related to validation dependencies
        // and we add the columns which have a callout

        final Map<String, String> sessionAttributesMap = new HashMap<>();

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
            final String columnName = INP
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

      if ((StringUtils.equals(EDIT, mode) || StringUtils.equals(CHANGE, mode)) && row != null) {
        if (!SecurityChecker.getInstance().isWritable(row)) {
          finalObject.put("_readOnly", true);
        }

        finalObject.put("noteCount", noteCount);
      }
      if (!attachments.isEmpty()) {
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
    String attribute = null;
    String attrValue = null;
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
        .contains(INP + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName())))
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
      List<String> calloutsToCall, List<String> lastFieldChanged, List<String> visibleProperties,
      List<String> gridVisibleProperties) {
    boolean forceComboReload = (StringUtils.equals(CHANGE, mode) && changedColumn == null);
    if (StringUtils.equals(CHANGE, mode) && changedColumn != null) {
      RequestContext.get().setRequestParameter(DO_NOT_ADD_CURRENT_ELEMENT, TRUE);
    }
    log.debug("computeColumnValues - forceComboReload: " + forceComboReload);
    HashMap<String, Field> columnsOfFields = new HashMap<>();
    for (Field field : getADFieldList(tab.getId())) {
      if (field.getColumn() == null) {
        continue;
      }

      String colName = null;
      if (field.getProperty() != null && !field.getProperty().isEmpty()) {
        colName = PROPERTY_FIELD + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "")
            + "_" + field.getColumn().getDBColumnName();
      } else {
        colName = field.getColumn().getDBColumnName();
      }
      columnsOfFields.put(colName, field);
    }
    List<String> changedCols = new ArrayList<>();
    for (String col : allColumns) {
      if (StringUtils.equals(NEW, mode) && containsIgnoreCase(getAuxiliaryInputNamesList(tab.getId()), col)) {
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
        if (StringUtils.equals(NEW, mode)) {
          // On NEW mode, the values are computed through the UIDefinition (the defaults will be
          // used)
          if (field.getProperty() != null && !field.getProperty().isEmpty()) {
            // if the column is a property we try to compute the property value, if value is not
            // found null is passed. Refer issue https://issues.openbravo.com/view.php?id=25754
            Object propertyValue = DalUtil.getValueFromPath(parentRecord, field.getProperty());
            if (propertyValue != null) {
              JSONObject jsonObject = new JSONObject();
              if (propertyValue instanceof BaseOBObject) {
                jsonObject.put(VALUE, ((BaseOBObject) propertyValue).getId());
                jsonObject.put(CLASSIC_VALUE, ((BaseOBObject) propertyValue).getId());
                ArrayList<JSONObject> comboEntries = new ArrayList<>();
                JSONObject entries = new JSONObject();
                entries.put(ID, ((BaseOBObject) propertyValue).getId());
                entries.put("_identifier", ((BaseOBObject) propertyValue).getIdentifier());
                comboEntries.add(entries);
                jsonObject.put(ENTRIES, new JSONArray(comboEntries));
              } else {
                jsonObject.put(VALUE, propertyValue.toString());
                jsonObject.put(CLASSIC_VALUE, propertyValue.toString());
              }
              value = jsonObject.toString();
            }
          } else {
            if (field.getColumn().isLinkToParentColumn() && parentRecord != null
                && referencedEntityIsParent(parentRecord, field)) {
              // If the column is link to the parent tab, we set its value as the parent id
              RequestContext.get()
                  .setRequestParameter(INP + Sqlc.TransformaNombreColumna(col), parentId);
              value = uiDef.getFieldProperties(field, true);
            } else if (field.getColumn().getDBColumnName().equalsIgnoreCase("IsActive")) {
              // The Active column is always set to 'true' on new records
              RequestContext.get()
                  .setRequestParameter(INP + Sqlc.TransformaNombreColumna(col), "Y");
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
        } else if (StringUtils.equals(EDIT, mode) || (StringUtils.equals(CHANGE, mode)
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
                    && visibleProperties.contains(INP + Sqlc.TransformaNombreColumna(col)))
                    + ", isDisplayed=" + field.isDisplayed() + ", isShowInGridView="
                    + field.isShowInGridView() + ", isShownInStatusBar=" + field.isShowInGridView()
                    + ", hasDefaultValue=" + (field.getColumn().getDefaultValue() != null)
                    + ", isMandatory=" + field.getColumn().isMandatory());
            uiDef.getFieldPropertiesWithoutCombo(field, true);
          } else {
            log.debug("field: " + field + " - getFieldProperties: hasVisibleProperties: "
                + (visibleProperties != null) + ", &contains: "
                + (visibleProperties != null
                && visibleProperties.contains(INP + Sqlc.TransformaNombreColumna(col)))
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
        } else if (StringUtils.equals(CHANGE, mode) || StringUtils.equals(SET_SESSION, mode)) {
          // On CHANGE and SETSESSION mode, the values are read from the request
          JSONObject jsCol = new JSONObject();
          String colName = INP + Sqlc.TransformaNombreColumna(col);
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
              jsCol.put(VALUE, uiDef.createFromClassicString((String) jsonValue));
              jsCol.put(CLASSIC_VALUE, jsonValue);
            } else {
              jsCol.put(VALUE, jsonValue);
              jsCol.put(CLASSIC_VALUE, uiDef.convertToClassicString(jsonValue));
            }
          } else {
            jsCol.put(VALUE, jsonValue);
            jsCol.put(CLASSIC_VALUE, jsonValue);
          }
          value = jsCol.toString();
        }
        JSONObject jsonobject = null;
        if (value != null) {
          jsonobject = new JSONObject(value);
          if (StringUtils.equals(CHANGE, mode)) {
            String oldValue = RequestContext.get()
                .getRequestParameter(INP + Sqlc.TransformaNombreColumna(col));
            String newValue;
            if (jsonobject.has(CLASSIC_VALUE)) {
              newValue = jsonobject.getString(CLASSIC_VALUE);
            } else {
              if (jsonobject.has(VALUE)) {
                newValue = jsonobject.getString(VALUE);
              } else {
                newValue = null;
              }
            }
            if (newValue == null || StringUtils.equals(NULL, newValue)) {
              newValue = "";
            }
            if (oldValue == null || StringUtils.equals(NULL, oldValue)) {
              oldValue = "";
            }
            if (!StringUtils.equals(newValue, oldValue)) {
              changedCols.add(field.getColumn().getDBColumnName());
            }
          }

          String colName = null;
          if (field.getProperty() != null && !field.getProperty().isEmpty()) {
            colName = PROPERTY_FIELD
                + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
                + field.getColumn().getDBColumnName();
          } else {
            colName = Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
          }
          columnValues.put(INP + colName, jsonobject);
          setRequestContextParameter(field, jsonobject);

          String fullPropertyName = null;
          if (field.getProperty() != null) {
            fullPropertyName = field.getProperty().replace('.', '$');
          } else {
            fullPropertyName = prop.getName();
          }

          // We also set the session value for the column in Edit or SetSession mode
          if (gridVisibleProperties.contains(fullPropertyName)
              && (StringUtils.equals(NEW, mode) || StringUtils.equals(EDIT, mode) || StringUtils.equals(SET_SESSION,
              mode))) {

            if (field.getColumn().isStoredInSession() || field.getColumn().isKeyColumn()) {
              setSessionValue(
                  tab.getWindow().getId() + "|" + field.getColumn().getDBColumnName().toUpperCase(),
                  jsonobject.has(CLASSIC_VALUE) ? jsonobject.get(CLASSIC_VALUE) : null);
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
        inpColName = INP + PROPERTY_FIELD
            + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
            + field.getColumn().getDBColumnName();
      } else {
        inpColName = INP + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
      }
      JSONObject value = columnValues.get(inpColName);
      String classicValue;
      try {
        classicValue = (value == null || !value.has(CLASSIC_VALUE)) ? ""
            : value.getString(CLASSIC_VALUE);
      } catch (JSONException e) {
        throw new OBException("Couldn't get data for column " + field.getColumn().getDBColumnName(),
            e);
      }
      if (((StringUtils.equals(NEW, mode) && !StringUtils.isBlank(classicValue)
          && (uiDef instanceof EnumUIDefinition || uiDef instanceof ForeignKeyUIDefinition))
          || (StringUtils.equals(CHANGE, mode) && changedCols.contains(field.getColumn().getDBColumnName())
          && changedColumn != null))
          && field.getColumn().isValidateOnNew() && (field.getColumn().getCallout() != null)) {
        addCalloutToList(field.getColumn(), calloutsToCall, lastFieldChanged);
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

    List<String> columnsToComputeAgain = new ArrayList<>();
    for (String changedCol : changedCols) {
      for (String colWithVal : columnsInValidation.keySet()) {
        for (String colInVal : columnsInValidation.get(colWithVal)) {
          if (colInVal.equalsIgnoreCase(changedCol) && (!columnsToComputeAgain.contains(colInVal))) {
            columnsToComputeAgain.add(colWithVal);
          }
        }
      }
    }
    HashMap<String, Field> columnsOfFields = new HashMap<>();
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
              INP + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName()),
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
      if (StringUtils.equals(CHANGE, mode) || StringUtils.equals(NEW, mode) && (overwrittenAuxiliaryInputs.contains(
          auxIn.getName()))) {
        continue;
      }

      if ((StringUtils.equals(EDIT, mode) || StringUtils.equals(CHANGE, mode))
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
        jsonObj.put(VALUE, value);
        jsonObj.put(CLASSIC_VALUE, value);
      } catch (JSONException e) {
        log.error("Error while computing auxiliary input " + auxIn.getName(), e);
      }

      columnValues.put(INP + Sqlc.TransformaNombreColumna(auxIn.getName()), jsonObj);
      RequestContext.get()
          .setRequestParameter(INP + Sqlc.TransformaNombreColumna(auxIn.getName()),
              value == null || value.equals(NULL) ? null : value.toString());

      if (StringUtils.equals(NEW, mode) && containsIgnoreCase(allColumns, auxIn.getName())) {
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
            if (jsonDefinition.has(IDENTIFIER)) {
              jsonObj.put(IDENTIFIER, jsonDefinition.get(IDENTIFIER));
            }
          }
        } catch (Exception e) {
          log.error("Error trying to calculate identifier for auxiliary input tab " + tab
              + " aux input " + auxIn, e);
        }
      }

      // Now we insert session values for auxiliary inputs
      if (StringUtils.equals(NEW, mode) || StringUtils.equals(EDIT, mode) || StringUtils.equals(SET_SESSION, mode)) {
        setSessionValue(tab.getWindow().getId() + "|" + auxIn.getName(), value);
      }
    }
  }

  private BaseOBObject setSessionVariablesInParent(String mode, Tab tab, BaseOBObject row,
      String parentId) {
    // If the FIC is called in CHANGE mode, we don't need to set session variables for the parent
    // records, because those were already set in the previous FIC call (either in NEW or EDIT mode)
    if (StringUtils.equals(CHANGE, mode)) {
      return null;
    }
    BaseOBObject parentRecord = null;
    if (StringUtils.equals(EDIT, mode)) {
      parentRecord = KernelUtils.getInstance().getParentRecord(row, tab);
    }
    Tab parentTab = KernelUtils.getInstance().getParentTab(tab);
    // If the parent table is not based in a db table, don't try to retrieve the record
    // Because tables not based on db tables do not have BaseOBObjects
    // See issue https://issues.openbravo.com/view.php?id=29667
    if (parentId != null && parentTab != null
        && StringUtils.equals(parentTab.getTable().getDataOriginType(), ApplicationConstants.TABLEBASEDTABLE)) {
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

    boolean tableBasedTable = StringUtils
        .equals(tab.getTable().getDataOriginType(), ApplicationConstants.TABLEBASEDTABLE);

    List<Field> fields = getADFieldList(tab.getId());
    // If the table is based on a datasource it is not possible to initialize the values from the
    // database
    if (StringUtils.equals(EDIT, mode) && tableBasedTable) {
      // In EDIT mode we initialize them from the database
      List<Column> columns = getADColumnList(tab.getTable().getId());

      for (Column column : columns) {
        setValueOfColumnInRequest(row, column.getDBColumnName(), tab);
      }
    }

    List<String> gridVisibleProperties = new ArrayList<>();
    if (jsContent.has(GRID_VISIBLE_PROPERTIES)) {
      try {
        gridVisibleProperties = convertJSONArray(jsContent.getJSONArray(GRID_VISIBLE_PROPERTIES));
      } catch (JSONException e) {
        log.error("Error while retrieving _gridVisibleProperties from jsContent" + jsContent, e);
      }
    }

    // and then overwrite with what gets passed in
    if (StringUtils.equals(EDIT, mode) || StringUtils.equals(CHANGE, mode) || StringUtils.equals(SET_SESSION, mode)) {
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
          inpColName = INP + PROPERTY_FIELD
              + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
              + field.getColumn().getDBColumnName();
        } else {
          fullPropertyName = prop.getName();
          inpColName = INP + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
        }
        if ((StringUtils.equals(EDIT, mode) || StringUtils.equals(SET_SESSION, mode))
            && !gridVisibleProperties.contains(fullPropertyName)) {
          continue;
        }
        try {
          if (jsContent.has(inpColName)) {
            final Object jsonValue = jsContent.get(inpColName);
            String value;
            if (jsonValue == null || StringUtils.equals(NULL, jsonValue.toString())) {
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

            if (value != null && StringUtils.equals(NULL, value)) {
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
    addSpecialParameters(jsContent);
  }

  private void addSpecialParameters(JSONObject jsContent) {
    Iterator<?> it = jsContent.keys();
    while (it.hasNext()) {
      String key = it.next().toString();
      try {
        if (RequestContext.get().getRequestParameter(key) == null) {
          String value = jsContent.getString(key);
          if (value != null && StringUtils.equals(NULL, value)) {
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
    ArrayList<String> columns = new ArrayList<>();
    List<String> columnsWithValidation = new ArrayList<>();
    HashMap<String, String> validations = new HashMap<>();
    for (Field field : fields) {
      if (field.getColumn() == null) {
        continue;
      }

      String colName = null;
      if (field.getProperty() != null && !field.getProperty().isEmpty()) {
        colName = PROPERTY_FIELD + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "")
            + "_" + field.getColumn().getDBColumnName();
        columns.add(colName);
      } else {
        colName = field.getColumn().getDBColumnName();
        columns.add(colName.toUpperCase());
      }

      String validation = getValidation(field);
      if (!StringUtils.isBlank(validation)) {
        columnsWithValidation.add(colName);
        validations.put(colName, validation);
      }
    }
    for (String column : columnsWithValidation) {
      columnsInValidation.put(column, parseValidation(column, validations.get(column), columns));
      if (log.isDebugEnabled()) {
        StringBuilder cols = new StringBuilder();
        for (String col : columnsInValidation.get(column)) {
          cols.append(col).append(",");
        }
        log.debug("Column: " + column);
        log.debug("Validation: '" + validations.get(column) + "'");
        log.debug("Columns in validation: '" + cols + "'");
      }
    }

    if (StringUtils.equals(CHANGE, mode) && changedColumn != null && !StringUtils.equals("inpadOrgId", changedColumn)) {
      // In case of a CHANGE event, we only add the changed column, to avoid firing reloads for
      // every column in the tab, instead firing reloads just for the dependant columns
      String changedCol = "";
      for (Field field : fields) {
        if (field.getColumn() == null) {
          continue;
        }
        String colName = field.getColumn().getDBColumnName();
        if (changedColumn.equalsIgnoreCase(INP + Sqlc.TransformaNombreColumna(colName))) {
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
        if (colName.equalsIgnoreCase(AD_ORG_ID)) {
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
          colName = PROPERTY_FIELD
              + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
              + field.getColumn().getDBColumnName();
        } else {
          colName = field.getColumn().getDBColumnName();
        }
        if (!SequenceUtils.isSequence(field.getColumn()) && !columnsWithValidation.contains(
            field.getColumn().getDBColumnName())
            && !sortedColumns.contains(colName) && !colName.equalsIgnoreCase(DOCUMENTNO)) {
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
    if (!mode.equalsIgnoreCase(CHANGE)) {
      for (Field field : fields) {
        if (field.getColumn() == null) {
          continue;
        }
        String colName = field.getColumn().getDBColumnName();
        if (SequenceUtils.isSequence(field.getColumn()) || colName.equalsIgnoreCase(DOCUMENTNO)) {
          if (field.getProperty() != null && !field.getProperty().isEmpty()) {
            colName = PROPERTY_FIELD
                + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
                + field.getColumn().getDBColumnName();
          }
          sortedColumns.add(colName);
        }
      }

      StringBuilder validationErrors = new StringBuilder();
      for (String col : columnsWithValidation) {
        if (!sortedColumns.contains(col)) {
          if (validationErrors.length() > 0) {
            validationErrors.append(" -- ");
          }
          String errorMessage = String.format(
              "%s column has a validation that depends on columns %s which creates a cycle",
              col, columnsInValidation.get(col));
          validationErrors.append(errorMessage);
        }
      }
      if (!StringUtils.isBlank(validationErrors.toString())) {
        String errorMessage = String.format(
            "%s -- List of sorted columns: %s", validationErrors, sortedColumns);
        throw new OBException(errorMessage, false);
      }
    }
    StringBuilder finalCols = new StringBuilder();
    for (String col : sortedColumns) {
      finalCols.append(col).append(",");
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
      String columnInp = INP + Sqlc.TransformaNombreColumna(column);
      if (column.equalsIgnoreCase(AD_ORG_ID) && !changeEventCols.contains(columnInp)) {
        changeEventCols.add(columnInp);
      }
      if (columnsInValidation.get(column) != null && !columnsInValidation.get(column).isEmpty()) {
        for (String colInVal : columnsInValidation.get(column)) {
          final String columnName = INP + Sqlc.TransformaNombreColumna(colInVal);
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
      if (currentValue != null && !currentValue.toString().equals(NULL)) {
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
        if (currentValue != null && currentValue.equals(NULL)) {
          currentValue = null;
        }
        if (currentValue == null) {
          RequestContext.get()
              .setRequestParameter(INP + Sqlc.TransformaNombreColumna(columnName), null);
        } else {
          RequestContext.get()
              .setRequestParameter(INP + Sqlc.TransformaNombreColumna(columnName),
                  currentValue.toString());
        }
      } else {
        RequestContext.get()
            .setRequestParameter(INP + Sqlc.TransformaNombreColumna(columnName), null);
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
      String fieldId = INP + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
      if (field.getProperty() != null && !field.getProperty().isEmpty()) {
        fieldId = INP + PROPERTY_FIELD
            + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
            + field.getColumn().getDBColumnName();
      }
      RequestContext.get()
          .setRequestParameter(fieldId,
              jsonObj.has(CLASSIC_VALUE) && jsonObj.get(CLASSIC_VALUE) != null
                  && !StringUtils.equals(NULL, jsonObj.getString(CLASSIC_VALUE))
                  ? jsonObj.getString(CLASSIC_VALUE)
                  : null);
    } catch (JSONException e) {
      log.error("Couldn't read JSON parameter for column " + field.getColumn().getDBColumnName());
    }
  }

  private HashMap<String, Field> buildInpField(List<Field> fields) {
    HashMap<String, Field> inpFields = new HashMap<>();
    for (Field field : fields) {
      if (field.getColumn() == null) {
        continue;
      }
      String fieldId = null;
      if (field.getProperty() != null && !field.getProperty().isEmpty()) {
        fieldId = INP + PROPERTY_FIELD
            + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
            + field.getColumn().getDBColumnName();
      } else {
        fieldId = INP + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
      }
      inpFields.put(fieldId, field);
    }
    return inpFields;
  }

  private List<String> executeCallouts(String mode, Tab tab, Map<String, JSONObject> columnValues,
      String changedColumn, List<String> calloutsToCall, List<String> lastFieldChanged,
      List<JSONObject> messages, List<String> dynamicCols, List<String> jsExecuteCode,
      Map<String, Object> hiddenInputs, List<String> overwrittenAuxiliaryInputs) {

    // In CHANGE mode, we will add the initial callout call for the changed column, if there is
    // one
    if (StringUtils.equals(CHANGE, mode) && (changedColumn != null)) {
      for (Column col : getADColumnList(tab.getTable().getId())) {
        if ((INP + Sqlc.TransformaNombreColumna(col.getDBColumnName())).equals(
            changedColumn) && (col.getCallout() != null)) {
          // The column has a callout. We will add the callout to the callout list
          addCalloutToList(col, calloutsToCall, lastFieldChanged);
        }
      }
    }

    ArrayList<String> calledCallouts = new ArrayList<>();
    if (calloutsToCall.isEmpty()) {
      return new ArrayList<>();
    }
    return runCallouts(columnValues, tab, calledCallouts, calloutsToCall, lastFieldChanged,
        messages, dynamicCols, jsExecuteCode, hiddenInputs, overwrittenAuxiliaryInputs);
  }

  private List<String> runCallouts(Map<String, JSONObject> columnValues, Tab tab,
      List<String> calledCallouts, List<String> calloutsToCall, List<String> lastfieldChangedList,
      List<JSONObject> messages, List<String> dynamicCols, List<String> jsExecuteCode,
      Map<String, Object> hiddenInputs, List<String> overwrittenAuxiliaryInputs) {
    HashMap<String, Object> callOutInstances = new HashMap<>();
    List<String> changedCols = new ArrayList<>();
    List<Field> fields = getADFieldList(tab.getId());
    HashMap<String, Field> inpFields = buildInpField(fields);
    String lastCalledCallout = "";
    String lastFieldOfLastCalloutCalled = "";

    while (!calloutsToCall.isEmpty() && calledCallouts.size() < MAX_CALLOUT_CALLS) {
      String callOutClassName = calloutsToCall.get(0);
      String lastFieldChanged = lastfieldChangedList.get(0);
      if (StringUtils.equals(lastCalledCallout, callOutClassName)
          && StringUtils.equals(lastFieldOfLastCalloutCalled, lastFieldChanged)) {
        log.debug("Callout filtered: " + callOutClassName);
        calloutsToCall.remove(callOutClassName);
        lastfieldChangedList.remove(lastFieldChanged);
        continue;
      }
      log.debug("Calling callout " + callOutClassName + " with field changed " + lastFieldChanged);
      Class<?> calloutClass;
      try {
        calloutClass = Class.forName(callOutClassName);
      } catch (ClassNotFoundException e) {
        throw new OBException("Couldn't find class " + callOutClassName, e);
      }
      try {
        calloutsToCall.remove(callOutClassName);
        lastfieldChangedList.remove(lastFieldChanged);

        Object calloutObject;
        if (callOutInstances.get(callOutClassName) != null) {
          calloutObject = callOutInstances.get(callOutClassName);
        } else {
          calloutObject = calloutClass.getDeclaredConstructor().newInstance();
          callOutInstances.put(callOutClassName, calloutObject);
        }

        if (!(calloutObject instanceof SimpleCallout)) {
          log.error(
              "Callout {} in (Window, Tab, Field) ({}, {}, {}), only reference instances of type SimpleCallout are allowed."
                  + " The callout is an instance of {} class.",
              callOutClassName, tab.getWindow().getName(), tab.getName(), lastFieldChanged,
              calloutObject.getClass().getName());
          continue;
        }

        RequestContext request = RequestContext.get();
        RequestContext.get().setRequestParameter("inpLastFieldChanged", lastFieldChanged);
        RequestContext.get().setRequestParameter("inpOB3UIMode", "Y");
        CalloutServletConfig config = new CalloutServletConfig(callOutClassName,
            RequestContext.getServletContext());
        CalloutInformationProvider calloutResponseManager = null;

        // execute SimpleCallout callouts
        if (SimpleCallout.class.isAssignableFrom(calloutClass)) {

          SimpleCallout callOutInstance = (SimpleCallout) calloutObject;
          callOutInstance.init(config);

          // execute SimpleCallout callout
          JSONObject result = callOutInstance.executeSimpleCallout(request);

          // updated info values of callouts infrastructure
          String callOutNameJS = callOutClassName.substring(callOutClassName.lastIndexOf(".") + 1);
          calledCallouts.add(callOutNameJS);

          calloutResponseManager = new SimpleCalloutInformationProvider(result);
        }

        manageUpdatedValuesForCallout(columnValues, tab, calloutsToCall, lastfieldChangedList,
            messages, dynamicCols, jsExecuteCode, hiddenInputs, overwrittenAuxiliaryInputs,
            changedCols, inpFields, callOutClassName, request, calloutResponseManager);

        lastCalledCallout = callOutClassName;
        lastFieldOfLastCalloutCalled = lastFieldChanged;
      } catch (Exception e) {
        throw new OBException("Couldn't execute callout (class " + callOutClassName + ")", e);
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
      HashMap<String, Field> inpFields, String callOutClassName, RequestContext request,
      CalloutInformationProvider calloutInformationProvider) throws JSONException {
    Object element = calloutInformationProvider.getNextElement();
    while (element != null) {
      String name = calloutInformationProvider.getCurrentElementName();
      if (StringUtils.equals(MESSAGE, name) || StringUtils.equals("INFO", name) || StringUtils.equals("WARNING", name)
          || StringUtils.equals("ERROR", name) || StringUtils.equals("SUCCESS", name)) {
        log.debug("Callout message: " + calloutInformationProvider.getCurrentElementValue(element));
        JSONObject message = new JSONObject();
        message.put(TEXT, calloutInformationProvider.getCurrentElementValue(element).toString());
        message.put(SEVERITY, name.equals(MESSAGE) ? "TYPE_INFO" : "TYPE_" + name);
        messages.add(message);
      } else if (StringUtils.equals("JSEXECUTE", name)) {
        // The code on a JSEXECUTE command is sent directly to the client for eval()
        String code = (String) calloutInformationProvider.getCurrentElementValue(element);
        if (code != null) {
          jsExecuteCode.add(code);
        }
      } else if (StringUtils.equals("EXECUTE", name)) {
        String js = calloutInformationProvider.getCurrentElementValue(element) == null ? null
            : calloutInformationProvider.getCurrentElementValue(element).toString();
        if (js != null && !StringUtils.isBlank(js)) {
          if (StringUtils.equals("displayLogic();", js)) {
            // We don't do anything, this is a harmless js response
          } else {
            JSONObject message = new JSONObject();
            message.put(TEXT,
                Utility.messageBD(new DalConnectionProvider(false), "OBUIAPP_ExecuteInCallout",
                    RequestContext.get().getVariablesSecureApp().getLanguage()));
            message.put(SEVERITY, "TYPE_ERROR");
            messages.add(message);
            log.warn("Callout " + callOutClassName
                + " returned EXECUTE command which is no longer supported, it should be fixed. Window-tab: "
                + tab.getWindow().getName() + " - " + tab.getName());
          }
        }
      } else {
        if (name.startsWith(INP)) {
          boolean changed = false;
          if (inpFields.containsKey(name)) {
            Column col = inpFields.get(name).getColumn();
            if (col != null) {
              String colId = INP + Sqlc.TransformaNombreColumna(col.getDBColumnName());
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
                      || (oldValue != null && newValue != null && !StringUtils.equals(newValue, oldValue))) {
                    columnValues.put(INP + Sqlc.TransformaNombreColumna(col.getDBColumnName()),
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
              if (changed && col.getCallout() != null && (isShouldBeFired(callOutClassName, col))) {
                addCalloutToList(col, calloutsToCall, lastfieldChangedList);
              }
            }
          } else {
            for (AuxiliaryInput aux : tab.getADAuxiliaryInputList()) {
              if (name.equalsIgnoreCase(INP + Sqlc.TransformaNombreColumna(aux.getName()))) {
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
   * @param callOutClassName
   *     callout that is firing
   * @return true if it is should be fired.
   */
  private boolean isShouldBeFired(String callOutClassName, Column col) {
    return !StringUtils
        .equals(col.getCallout().getADModelImplementationList().get(0).getJavaClassName(), callOutClassName);
  }

  private void addCalloutToList(Column col, List<String> listOfCallouts,
      List<String> lastFieldChangedList) {
    if (col.getCallout().getADModelImplementationList() == null
        || col.getCallout().getADModelImplementationList().size() == 0) {
      log.info("The callout of the column " + col.getDBColumnName()
          + " doesn't have a corresponding model object, and therefore cannot be executed.");
    } else {
      String callOutClassNameToCall = col.getCallout()
          .getADModelImplementationList()
          .get(0)
          .getJavaClassName();
      listOfCallouts.add(callOutClassNameToCall);
      lastFieldChangedList.add(INP + Sqlc.TransformaNombreColumna(col.getDBColumnName()));
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
    StringBuilder val = new StringBuilder();
    if (c.getValidation() != null && c.getValidation().getValidationCode() != null) {
      val.append(c.getValidation().getValidationCode());
    }
    if (StringUtils.equals("18", c.getReference().getId()) && (c.getReferenceSearchKey() != null)) {
        for (ReferencedTable t : c.getReferenceSearchKey().getADReferencedTableList()) {
          val.append(" AND ").append(t.getSQLWhereClause());
        }
    }
    return val.toString();

  }

  private ArrayList<String> parseValidation(String column, String validation,
      List<String> possibleColumns) {
    String token = validation;
    ArrayList<String> columns = new ArrayList<>();
    int i = token.indexOf("@");
    while (i != -1) {
      token = token.substring(i + 1);
      if (!token.startsWith("SQL")) {
        i = token.indexOf("@");
        if (i != -1) {
          String strAux = token.substring(0, i);
          token = token.substring(i + 1);
          if (!columns.contains(strAux) && (!strAux.equalsIgnoreCase(column)
              && possibleColumns.contains(strAux.toUpperCase()))) {
            columns.add(strAux);
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
        ArrayList<String> params = new ArrayList<>();
        String sql = UIDefinition.parseSQL(code, params);
        log.debug("Transformed SQL code: " + sql);
        int indP = 1;
        PreparedStatement ps = OBDal.getInstance().getConnection(false).prepareStatement(sql);
        try {
          for (String parameter : params) {
            String value = "";
            if (StringUtils.equals("#", parameter.substring(0, 1))) {
              value = Utility.getContext(new DalConnectionProvider(false),
                  RequestContext.get().getVariablesSecureApp(), parameter, windowId);
            } else {
              String fieldId = INP + Sqlc.TransformaNombreColumna(parameter);
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
    List<String> result = new ArrayList<>();
    for (AuxiliaryInput ai : cachedStructures.getAuxiliarInputList(tabId)) {
      result.add(ai.getName());
    }
    return result;
  }

  private String readParameter(Map<String, Object> parameters, String parameterName) {
    String paramValue = (String) parameters.get(parameterName);
    if (paramValue != null && paramValue.equalsIgnoreCase(NULL)) {
      paramValue = null;
    }
    return paramValue;
  }
}
