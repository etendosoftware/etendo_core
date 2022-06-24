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
 * All portions are Copyright (C) 2009-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.json;

import static java.util.stream.Collectors.toList;
import static org.openbravo.userinterface.selector.SelectorConstants.includeOrgFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollableResults;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Column;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.util.Check;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.CachedPreference;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.datasource.DataSourceUtils;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonToDataConverter.JsonConversionError;
import org.openbravo.userinterface.selector.Selector;
import org.openbravo.userinterface.selector.SelectorConstants;

/**
 * Implements generic data operations which have parameters and json as an input and return results
 * as json strings.
 * 
 * Note the parameters, json input and generated json follow the Smartclient specs. See the
 * Smartclient <a href="http://www.smartclient.com/docs/7.0rc2/a/b/c/go.html#class..RestDataSource">
 * RestDataSource</a> for more information.
 * 
 * The main usage of this class is through the {@link #getInstance()} method (as a singleton). This
 * class can however also be extended and instantiated directly.
 * 
 * There are several methods to override/implement update and insert hooks, see the pre* and post*
 * methods.
 * 
 * @author mtaal
 */
public class DefaultJsonDataService implements JsonDataService {
  private static final Logger log = LogManager.getLogger();

  private static final String ADD_FLAG = "_doingAdd";
  private static final int DEFAULT_ID_LENGTH = 32;

  @Inject
  private CachedPreference cachedPreference;

  private static DefaultJsonDataService instance = WeldUtils
      .getInstanceFromStaticBeanManager(DefaultJsonDataService.class);

  public static DefaultJsonDataService getInstance() {
    return instance;
  }

  public static void setInstance(DefaultJsonDataService instance) {
    DefaultJsonDataService.instance = instance;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.service.json.JsonDataService#fetch(java.util.Map)
   */
  @Override
  public String fetch(Map<String, String> parameters) {
    return fetch(parameters, true);
  }

  public String fetch(Map<String, String> parameters, boolean filterOnReadableOrganizations) {
    try {
      final String entityName = parameters.get(JsonConstants.ENTITYNAME);
      Check.isNotNull(entityName, "The name of the service/entityname should not be null");
      Check.isNotNull(parameters, "The parameters should not be null");

      doPreAction(parameters, "", DataSourceAction.FETCH);
      String selectedProperties = parameters.get(JsonConstants.SELECTEDPROPERTIES_PARAMETER);
      // The display property is present only for displaying table references in filter.
      // This parameter is used to set the identifier with the display column value.
      // Refer https://issues.openbravo.com/view.php?id=26696
      String displayField = parameters.get(JsonConstants.DISPLAYFIELD_PARAMETER);
      if (StringUtils.isNotEmpty(displayField) && StringUtils.isNotEmpty(selectedProperties)) {
        boolean propertyPresent = false;
        for (String selectedProp : selectedProperties.split(",")) {
          if (selectedProp.equals(displayField)) {
            propertyPresent = true;
            break;
          }
        }
        if (!propertyPresent) {
          if (StringUtils.isNotEmpty(selectedProperties)) {
            selectedProperties = selectedProperties.concat("," + displayField);
          } else {
            selectedProperties = displayField;
          }
        }
      }

      final JSONObject jsonResult = new JSONObject();
      final JSONObject jsonResponse = new JSONObject();
      List<BaseOBObject> bobs;
      final String id = parameters.get(JsonConstants.ID);
      // if the id is set that's a special case of one object being requested
      if (id != null) {
        bobs = new ArrayList<BaseOBObject>();
        final OBQuery<BaseOBObject> obq = OBDal.getInstance()
            .createQuery(entityName, "id = :bobId");
        obq.setNamedParameter("bobId", id);
        obq.setFilterOnActive(false);
        obq.setMaxResult(1);
        final BaseOBObject bob = obq.uniqueResult();
        if (bob != null) {
          bobs.add(bob);
        }
      } else {
        // Retrieve parameter to identify if the fetch request comes from a Pick And Edit window
        String isPickAndEditParam = parameters.get(JsonConstants.IS_PICK_AND_EDIT);
        final boolean isPickAndEdit = StringUtils.isNotEmpty(isPickAndEditParam)
            ? Boolean.valueOf(isPickAndEditParam)
            : Boolean.FALSE;
        // Retrieve parameter to identify if the fetch request comes from a multi selector
        String isMultiSelectorParam = parameters.get(JsonConstants.IS_MULTI_SELECTOR);
        final boolean isMultiSelector = StringUtils.isNotEmpty(isMultiSelectorParam)
            ? Boolean.valueOf(isMultiSelectorParam)
            : Boolean.FALSE;
        final String startRowStr = parameters.get(JsonConstants.STARTROW_PARAMETER);
        final String endRowStr;
        if (isPickAndEdit || isMultiSelector) {
          endRowStr = getEndRowForSelectedRecords(parameters, startRowStr);
          if (endRowStr != null
              && !endRowStr.equals(parameters.get(JsonConstants.ENDROW_PARAMETER))) {
            parameters.put(JsonConstants.ENDROW_PARAMETER, endRowStr);
            log.debug("The amount of selected records is higher than the page size, "
                + "setting page size equal to the amount of selected records: " + endRowStr);
          }
        } else {
          endRowStr = parameters.get(JsonConstants.ENDROW_PARAMETER);
        }

        boolean preventCountOperation = !parameters.containsKey(JsonConstants.NOCOUNT_PARAMETER)
            || "true".equals(parameters.get(JsonConstants.NOCOUNT_PARAMETER));

        @SuppressWarnings("unchecked")
        Map<String, String> paramsCount = (Map<String, String>) ((HashMap<String, String>) parameters)
            .clone();
        // _isWsCall can not be used as an URL parameter, we prevent its usage by removing it
        paramsCount.remove(JsonConstants.IS_WS_CALL);
        DataEntityQueryService queryService = createSetQueryService(paramsCount, true);
        queryService.setEntityName(entityName);

        // only do the count if a paging request is done and it has not been prevented
        // explicitly
        boolean doCount = false;
        int count = -1;
        int computedMaxResults = (queryService.getMaxResults() == null ? Integer.MAX_VALUE
            : queryService.getMaxResults());
        if (startRowStr != null) {
          doCount = true;
        }
        if (endRowStr != null) {
          // note computedmaxresults must be set before
          // endRow is increased by 1
          // increase by 1 to see if there are more results.
          if (preventCountOperation) {
            // set count here, is corrected in specific cases later
            count = queryService.getMaxResults();
          }
        } else {
          // can't do count if there is no endrow...
          preventCountOperation = false;
        }

        if (doCount && !preventCountOperation) {
          count = queryService.count();
        }

        if (parameters.containsKey(JsonConstants.ONLYCOUNT_PARAMETER)) {
          // stop here
          jsonResponse.put(JsonConstants.RESPONSE_TOTALROWS, count);
          return jsonResponse.toString();
        }
        queryService = createSetQueryService(parameters, false, false,
            filterOnReadableOrganizations);

        if (parameters.containsKey(JsonConstants.SUMMARY_PARAMETER)) {
          final JSONObject singleResult = new JSONObject();
          if (queryService.getSummaryFields().size() == 1) {
            singleResult.put(queryService.getSummaryFields().get(0),
                queryService.buildOBQuery().uniqueResultObject());
          } else {
            final Object[] os = queryService.buildOBQuery()
                .createQuery(Object[].class)
                .uniqueResult();
            int i = 0;
            if (os != null && os.length > 0) {
              for (String key : queryService.getSummaryFields()) {
                singleResult.put(key, os[i++]);
              }
            }
          }
          singleResult.put("isGridSummary", true);

          jsonResponse.put(JsonConstants.RESPONSE_DATA,
              new JSONArray(Collections.singleton(singleResult)));
          jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
          jsonResult.put(JsonConstants.RESPONSE_RESPONSE, jsonResponse);
          jsonResponse.put(JsonConstants.RESPONSE_STARTROW, 0);
          jsonResponse.put(JsonConstants.RESPONSE_ENDROW, 1);
          jsonResponse.put(JsonConstants.RESPONSE_TOTALROWS, 1);
          return jsonResult.toString();
        } else {
          long t = System.currentTimeMillis();
          bobs = queryService.list();
          log.debug("query time: {}", System.currentTimeMillis() - t);

          // If the request is done from a P&E window, then we should adapt the page size to include
          // all selected records into the response
          if (isPickAndEdit) {
            if (shouldIncreasePageSize(bobs, startRowStr, endRowStr)) {
              String newEndRow = Integer
                  .toString(Integer.parseInt(endRowStr) + JsonConstants.PAE_DATA_PAGE_SIZE);
              parameters.put(JsonConstants.ENDROW_PARAMETER, newEndRow);
              log.debug(
                  "The amount of selected records is higher than the page size, increasing page size to {}",
                  newEndRow);
              return fetch(parameters, filterOnReadableOrganizations);
            }
            bobs = getFixedMultiSelection(queryService, bobs);
          }

          // Multi selectors adapt the page size to include all selected records at the top of the
          // response
          if (isMultiSelector) {
            // Check if endrow should be updated, update it if necessary and rerun this fetch
            if (endRowStr != null && startRowStr != null) {
              int nSelected = DataSourceUtils.getNumberOfSelectedRecords(parameters);
              int nElements = Integer.parseInt(endRowStr) - Integer.parseInt(startRowStr);
              if (nSelected > nElements) {
                String newEndRow = Integer
                    .toString(Integer.parseInt(endRowStr) + (nSelected - nElements));
                parameters.put(JsonConstants.ENDROW_PARAMETER, newEndRow);
                log.debug(
                    "The amount of selected records is higher than the page size, increasing page size to {}",
                    newEndRow);
                return fetch(parameters, filterOnReadableOrganizations);
              }
            }

            bobs = getFixedMultiSelection(queryService, bobs);
          }
        }

        bobs = bobFetchTransformation(bobs, parameters);
        // take start row from actual query service because it can be modified from the originally
        // requested one
        int startRow = queryService.getFirstResult() != null ? queryService.getFirstResult() : 0;

        if (preventCountOperation) {
          count = bobs.size() + startRow;
          // computedMaxResults is one too much, if we got one to much then correct
          // the result and up the count so that the grid knows that there are more
          if (bobs.size() >= computedMaxResults) {
            count++;
          }

          // Redo fetch if missing elements on last fetch, from start, to take into consideration
          // non selected elements in between selected elements
          if (bobs.isEmpty() && startRow > 0 && (isPickAndEdit || isMultiSelector)) {
            parameters.put(JsonConstants.STARTROW_PARAMETER, "0");
            parameters.put(JsonConstants.ENDROW_PARAMETER, count + computedMaxResults + "");
            return fetch(parameters);
          }
        }

        jsonResponse.put(JsonConstants.RESPONSE_STARTROW, startRow);
        jsonResponse.put(JsonConstants.RESPONSE_ENDROW,
            (bobs.size() > 0 ? bobs.size() + startRow - 1 : 0));
        // bobs can be empty and count > 0 if the order by forces a join without results
        if (bobs.isEmpty()) {
          if (startRow > 0) {
            // reload the startrow again from 0
            parameters.put(JsonConstants.STARTROW_PARAMETER, "0");
            parameters.put(JsonConstants.ENDROW_PARAMETER, computedMaxResults + "");
            return fetch(parameters);
          }
          jsonResponse.put(JsonConstants.RESPONSE_TOTALROWS, 0);
        } else if (doCount) {
          jsonResponse.put(JsonConstants.RESPONSE_TOTALROWS, count);
        }
      }

      final DataToJsonConverter toJsonConverter = OBProvider.getInstance()
          .get(DataToJsonConverter.class);
      toJsonConverter.setAdditionalProperties(JsonUtils.getAdditionalProperties(parameters));
      toJsonConverter.setSelectedProperties(selectedProperties);
      if (StringUtils.isNotEmpty(displayField)
          && (!displayField.equals(JsonConstants.IDENTIFIER))) {
        toJsonConverter.setDisplayProperty(displayField);
      }
      final List<JSONObject> jsonObjects = toJsonConverter.toJsonObjects(bobs);

      addWritableAttribute(jsonObjects);

      jsonResponse.put(JsonConstants.RESPONSE_DATA, new JSONArray(jsonObjects));
      jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
      jsonResult.put(JsonConstants.RESPONSE_RESPONSE, jsonResponse);

      return doPostAction(parameters, jsonResult.toString(), DataSourceAction.FETCH, null);
    } catch (Throwable t) {
      log.error(t.getMessage(), t);
      return JsonUtils.convertExceptionToJson(t);
    }
  }

  /**
   * Fixes selected records for multi-selection datasource (P&E and multiple selectors) requests
   * ensuring all selected records are placed on top of not selected ones. If the complete selection
   * was not initially fetched in current page, an additional query to retrieve it will be executed.
   */
  private List<BaseOBObject> getFixedMultiSelection(DataEntityQueryService queryService,
      List<BaseOBObject> bobs) {
    JSONObject criteria = queryService.getQueryBuilder().getCriteria();
    Set<String> selectedIds = DataSourceUtils.getSelectedRecordsFromCriteria(criteria);
    if (selectedIds.isEmpty()) {
      return bobs;
    }

    List<BaseOBObject> selected = new ArrayList<>(bobs.size());
    List<BaseOBObject> unselected = new ArrayList<>(bobs.size());

    for (BaseOBObject row : bobs) {
      if (selectedIds.contains(row.getId())) {
        selected.add(row);
      } else {
        unselected.add(row);
      }
    }

    int selectionInCurrentPage = selected.size();
    boolean completeSelectionInCurrentPage = selectionInCurrentPage == selectedIds.size();
    if (completeSelectionInCurrentPage) {
      selected.addAll(unselected);
      if (queryService.getFirstResult() > selectionInCurrentPage) {
        selected = selected.stream().skip(selectionInCurrentPage).collect(toList());
      }
      return selected;
    }

    log.debug("Initial query doesn't contain all selection, querying again to get it {}", criteria);

    List<BaseOBObject> gridRows = bobs;
    try {
      selected = getAllSelectedRecords(queryService);
      int idsOutOfCurrentPage = selected.size() - selectionInCurrentPage;
      selected.addAll(unselected);

      if (queryService.getFirstResult() > 0 && idsOutOfCurrentPage > 0) {
        // fix pagination to prevent selection appearing in all pages
        log.trace("Skipping {} rows for {}", idsOutOfCurrentPage, criteria);
        gridRows = selected.stream().skip(idsOutOfCurrentPage).collect(toList());
        if (queryService.getFirstResult() > 0 && selectionInCurrentPage > 0) {
          // Remove selected items if those have been already shown in previous pages, removing
          // duplicates
          gridRows = gridRows.stream().skip(selectionInCurrentPage).collect(toList());
        }
      } else {
        gridRows = selected;
      }
    } catch (JSONException e) {
      log.error("failed selecting ids {}", criteria, e);
    }

    return gridRows;
  }

  /** Note side effect: queryService's criteria gets modified */
  private List<BaseOBObject> getAllSelectedRecords(DataEntityQueryService queryService)
      throws JSONException {
    JSONObject criteria = queryService.getQueryBuilder().getCriteria();
    JSONArray criteriaFields = criteria.getJSONArray("criteria");
    JSONArray crieriaIdFields = new JSONArray();
    for (int i = 0; i < criteriaFields.length(); i++) {
      JSONObject field = criteriaFields.getJSONObject(i);
      if (field.has("fieldName") && "id".equals(field.getString("fieldName"))
          && field.has("operator") && "equals".equals(field.getString("operator"))) {
        crieriaIdFields.put(field);
      }
    }
    log.trace("Old criteria {}", criteria);
    criteria.put("criteria", crieriaIdFields);

    log.trace("New criteria {}", criteria);

    Integer originalFirstResult = queryService.getFirstResult();
    queryService.setCriteria(criteria);
    queryService.setFirstResult(0);
    queryService.setMaxResults(JsonConstants.PAE_MAX_PAGE_SIZE);
    List<BaseOBObject> rows = queryService.list();
    queryService.setFirstResult(originalFirstResult);
    return rows;
  }

  /**
   * Used on requests received from pick and execute windows in order to avoid losing the selection.
   * It checks if the request has a criteria that contains the selected records. In that case, if
   * the amount of selected records is higher than the page size, then the end row is increased so
   * that all the selected records can be returned within the same page.
   * 
   * @param parameters
   *          map of request parameters
   * @param startRowStr
   *          start row of the page
   * @return the new value for the end row
   */
  private String getEndRowForSelectedRecords(Map<String, String> parameters, String startRowStr) {
    String endRowStr = parameters.get(JsonConstants.ENDROW_PARAMETER);
    if (startRowStr == null || endRowStr == null) {
      return endRowStr;
    }
    int startRow = Integer.parseInt(startRowStr);
    int endRow = Integer.parseInt(endRowStr);
    int selectedRecords = DataSourceUtils.getNumberOfSelectedRecords(parameters);
    if (startRow == 0 && endRow != -1 && selectedRecords > JsonConstants.PAE_DATA_PAGE_SIZE
        && selectedRecords > endRow) {
      return Integer.toString(selectedRecords);
    }
    return endRowStr;
  }

  /**
   * Used on requests received from pick and execute windows in order to avoid losing the selection.
   * It is used when the request does not contain information about the selected records. In that
   * case, this method decides if the query should be done again with a higher page size, according
   * to the the selection status of the last record returned by the database query and the current
   * page size.
   * 
   * @param bobs
   *          list of objects returned by the database query
   * @param startRowStr
   *          start row of the page
   * @param endRowStr
   *          end row of the page
   * @return true if the page size should be increased, false otherwise
   */
  private boolean shouldIncreasePageSize(List<BaseOBObject> bobs, String startRowStr,
      String endRowStr) {
    if (startRowStr == null || endRowStr == null) {
      return false;
    }
    int startRow = Integer.parseInt(startRowStr);
    int endRow = Integer.parseInt(endRowStr);
    if (startRow == 0 && endRow != -1 && bobs.size() > endRow
        && endRow <= JsonConstants.PAE_MAX_PAGE_SIZE) {
      return isLastRecordSelected(bobs);
    }
    return false;
  }

  private boolean isLastRecordSelected(List<BaseOBObject> bobs) {
    if (bobs.isEmpty()) {
      return false;
    }

    try {
      BaseOBObject bob = bobs.get(bobs.size() - 1);

      if (!bob.getEntity().hasProperty("obSelected")) {
        return false;
      }
      return (Boolean) bob.get("obSelected");
    } catch (Exception ex) {
      // Error retrieving obSelected property, do nothing: record not selected
      return false;
    }
  }

  public void fetch(Map<String, String> parameters, QueryResultWriter writer) {
    long t = System.currentTimeMillis();

    doPreAction(parameters, "", DataSourceAction.FETCH);
    final DataEntityQueryService queryService = createSetQueryService(parameters, false);

    String selectedProperties = parameters.get(JsonConstants.SELECTEDPROPERTIES_PARAMETER);

    final DataToJsonConverter toJsonConverter = OBProvider.getInstance()
        .get(DataToJsonConverter.class);
    toJsonConverter.setAdditionalProperties(JsonUtils.getAdditionalProperties(parameters));
    // Convert to Json only the properties specified in the request. If no properties are specified,
    // all of them will be converted to Json
    toJsonConverter.setSelectedProperties(selectedProperties);

    final ScrollableResults scrollableResults = queryService.scroll();
    try {
      int i = 0;
      while (scrollableResults.next()) {
        final Object result = scrollableResults.get()[0];
        final JSONObject json = toJsonConverter.toJsonObject((BaseOBObject) result,
            DataResolvingMode.FULL);

        try {
          doPostFetch(parameters, json);
        } catch (JSONException e) {
          throw new OBException(e);
        }

        writer.write(json);

        i++;
        // Clear session every 1000 records to prevent huge memory consumption in case of big loops
        if (i % 1000 == 0) {
          OBDal.getInstance().getSession().clear();
          log.debug(
              "clearing in record " + i + " elapsed time " + (System.currentTimeMillis() - t));
        }
      }
    } finally {
      scrollableResults.close();
    }
    log.debug("Fetch took " + (System.currentTimeMillis() - t) + " ms");
  }

  protected DataEntityQueryService createSetQueryService(Map<String, String> parameters,
      boolean forCountOperation) {
    return createSetQueryService(parameters, forCountOperation, false, true);
  }

  private DataEntityQueryService createSetQueryService(Map<String, String> parameters,
      boolean forCountOperation, boolean forSubEntity, boolean filterOnReadableOrganizations) {
    String entityName = parameters.get(JsonConstants.ENTITYNAME);
    final DataEntityQueryService queryService = OBProvider.getInstance()
        .get(DataEntityQueryService.class);

    String currentProfile = SessionInfo.getQueryProfile();
    if (currentProfile == null || currentProfile.isEmpty()) {
      currentProfile = "grid";
      SessionInfo.setQueryProfile(currentProfile);
    }
    log.trace("Generating QueryService for entity {} with profile {}", entityName, currentProfile);

    boolean includeOrgFilter = includeOrgFilter(parameters);

    boolean hasSubentity = !forSubEntity
        && parameters.get(JsonConstants.DISTINCT_PARAMETER) != null;
    if (hasSubentity) {
      // this is the main entity of a 'contains' (used in FK drop down lists), it will create also
      // info for subentity

      if ("true".equals(parameters.get(JsonConstants.SHOW_FK_DROPDOWN_UNFILTERED_PARAMETER))) {
        // Do not filter out the rows of the referenced tables if
        // they are not referenced from the referencing tables
        // Showing the records unfiltered improves the performance if the referenced table has just
        // a few records and the referencing table has lots
        final String distinctPropertyPath = parameters.get(JsonConstants.DISTINCT_PARAMETER);
        final Property distinctProperty = DalUtil.getPropertyFromPath(
            ModelProvider.getInstance().getEntity(entityName), distinctPropertyPath);
        final Entity distinctEntity = distinctProperty.getTargetEntity();
        queryService.setEntityName(distinctEntity.getName());
        queryService.addFilterParameter(JsonConstants.SHOW_FK_DROPDOWN_UNFILTERED_PARAMETER,
            "true");
        queryService.setFilterOnReadableOrganizations(filterOnReadableOrganizations);
        if (parameters.containsKey(JsonConstants.USE_ALIAS)) {
          queryService.setUseAlias();
        }

        String baseCriteria = "";
        // The main entity is now the referenced table, so each criterion that references it must be
        // updated
        // The criteria that does not apply to the referenced table can be ignored
        if (!StringUtils.isEmpty(parameters.get("criteria"))) {
          String criteria = parameters.get("criteria");
          for (String criterion : criteria.split(JsonConstants.IN_PARAMETER_SEPARATOR)) {
            try {
              JSONObject jsonCriterion = new JSONObject(criterion);
              if (jsonCriterion.getString("fieldName")
                  .equals(distinctPropertyPath + "$" + JsonConstants.IDENTIFIER)) {
                jsonCriterion.put("fieldName", JsonConstants.IDENTIFIER);
                baseCriteria = jsonCriterion.toString();
              }
            } catch (JSONException e) {
              log.error("Error obtaining 'distint' criterion for " + criterion, e);
            }
          }
        }
        if (StringUtils.isEmpty(baseCriteria)) {
          parameters.remove("criteria");
        } else {
          parameters.put("criteria", baseCriteria);
        }
        // The where clause of the referencing table no longer needs to be applied, as the query
        // will be done on the referenced table
        removeWhereParameter(parameters);
      } else {

        final String distinctPropertyPath = parameters.get(JsonConstants.DISTINCT_PARAMETER);
        final Property distinctProperty = DalUtil.getPropertyFromPath(
            ModelProvider.getInstance().getEntity(entityName), distinctPropertyPath);
        final Entity distinctEntity = distinctProperty.getTargetEntity();

        // criteria needs to be split in two parts:
        // -One for main entity (the one directly queried for)
        // -Another one for subentity
        String baseCriteria = "";
        String subCriteria = "";
        if (!StringUtils.isEmpty(parameters.get("criteria"))) {
          String criteria = parameters.get("criteria");
          for (String criterion : criteria.split(JsonConstants.IN_PARAMETER_SEPARATOR)) {
            try {
              JSONObject jsonCriterion = new JSONObject(criterion);
              if (jsonCriterion.getString("fieldName")
                  .equals(distinctPropertyPath + "$" + JsonConstants.IDENTIFIER)) {
                jsonCriterion.put("fieldName", JsonConstants.IDENTIFIER);
                baseCriteria = jsonCriterion.toString();
              } else {
                subCriteria += subCriteria.length() > 0 ? JsonConstants.IN_PARAMETER_SEPARATOR : "";
                subCriteria += criterion;
              }
            } catch (JSONException e) {
              log.error("Error obtaining 'distint' criterion for " + criterion, e);
            }
          }
        }

        // params for subentity are based on main entity ones
        @SuppressWarnings("unchecked")
        Map<String, String> paramSubCriteria = (Map<String, String>) ((HashMap<String, String>) parameters)
            .clone();

        // set proper criteria for each case
        if (StringUtils.isEmpty(subCriteria)) {
          paramSubCriteria.remove("criteria");
        } else {
          paramSubCriteria.put("criteria", subCriteria);
        }
        if (StringUtils.isEmpty(baseCriteria)) {
          parameters.remove("criteria");
        } else {
          parameters.put("criteria", baseCriteria);
        }

        // where parameter is only applied in subentity, remove it from main entity
        removeWhereParameter(parameters);

        // main entity ("me") settings
        queryService.getQueryBuilder().setMainAlias("me");
        queryService.setEntityName(distinctEntity.getName());

        queryService.setFilterOnReadableClients(false);
        queryService.setFilterOnReadableOrganizations(false);
        queryService.setFilterOnActive(false);

        // create now subentity
        queryService.setSubEntity(entityName, createSetQueryService(paramSubCriteria,
            forCountOperation, true, filterOnReadableOrganizations), distinctProperty,
            distinctPropertyPath);
      }
    } else {
      queryService.setEntityName(entityName);
      queryService
          .setFilterOnReadableOrganizations(filterOnReadableOrganizations && includeOrgFilter);
      if (parameters.containsKey(JsonConstants.USE_ALIAS)) {
        queryService.setUseAlias();
      }
    }

    final String startRowStr = parameters.get(JsonConstants.STARTROW_PARAMETER);
    final String endRowStr = parameters.get(JsonConstants.ENDROW_PARAMETER);
    final JSONObject criteria = JsonUtils.buildCriteria(parameters);

    if ((StringUtils.isEmpty(startRowStr) || StringUtils.isEmpty(endRowStr))
        && !isIDCriteria(criteria) && !parameters.containsKey("exportAs")) {
      // pagination is not set, this is most likely a bug
      log.warn("Fetching data without pagination, this can cause perfomance issues. Parameters: "
          + convertParameterToString(parameters));

      boolean isWsCall = parameters.containsKey(JsonConstants.IS_WS_CALL)
          && "true".equals(parameters.get(JsonConstants.IS_WS_CALL));

      if (parameters.containsKey(JsonConstants.TAB_PARAMETER)
          || parameters.containsKey(SelectorConstants.DS_REQUEST_SELECTOR_ID_PARAMETER)) {

        // for standard tab and selector datasources pagination is mandatory
        throw new OBException(OBMessageUtils.messageBD("OBJSON_NoPagedFetch"));
      } else if (!Preferences.YES.equals(
          cachedPreference.getPreferenceValue(CachedPreference.ALLOW_UNPAGED_DS_MANUAL_REQUEST))
          && !isWsCall) {
        throw new OBException(OBMessageUtils.messageBD("OBJSON_NoPagedFetchManual"));
      }
    }

    boolean directNavigation = parameters.containsKey("_directNavigation")
        && "true".equals(parameters.get("_directNavigation"))
        && parameters.containsKey(JsonConstants.TARGETRECORDID_PARAMETER);

    if (parameters.containsKey(JsonConstants.TARGETRECORDID_PARAMETER)
        && parameters.get(JsonConstants.TARGETRECORDID_PARAMETER) != null
        && !"null".equals(parameters.get(JsonConstants.TARGETRECORDID_PARAMETER))
        && !"true".equals(parameters.get("_directNavigation"))) {
      log.warn(
          "Datasource request with targetRecordId but without directNavigation detected. This type of requests should be avoided because they result in a query that performs poorly. Parameters: "
              + convertParameterToString(parameters));
    }

    if (!directNavigation) {
      // set the where/org filter parameters and the @ parameters
      for (String key : parameters.keySet()) {
        if (key.equals(JsonConstants.IDENTIFIER) || key.equals(JsonConstants.WHERE_PARAMETER)
            || key.equals(JsonConstants.WHERE_AND_FILTER_CLAUSE)
            || (key.equals(JsonConstants.ORG_PARAMETER) && includeOrgFilter)
            || key.equals(JsonConstants.CALCULATE_ORGS)
            || key.equals(JsonConstants.TARGETRECORDID_PARAMETER)
            || (key.startsWith(DataEntityQueryService.PARAM_DELIMITER)
                && key.endsWith(DataEntityQueryService.PARAM_DELIMITER))
            || (key.equals(SelectorConstants.DS_REQUEST_SELECTOR_ID_PARAMETER))) {
          queryService.addFilterParameter(key, parameters.get(key));
        }
      }
    }
    queryService.setCriteria(criteria);

    if (parameters.get(JsonConstants.NO_ACTIVE_FILTER) != null
        && parameters.get(JsonConstants.NO_ACTIVE_FILTER).equals("true")) {
      queryService.setFilterOnActive(false);
    }

    if (parameters.containsKey(JsonConstants.TEXTMATCH_PARAMETER_OVERRIDE)) {
      queryService.setTextMatching(parameters.get(JsonConstants.TEXTMATCH_PARAMETER_OVERRIDE));
    } else {
      queryService.setTextMatching(parameters.get(JsonConstants.TEXTMATCH_PARAMETER));
    }

    // only do the count if a paging request is done
    // note preventCountOperation variable is considered further below
    int startRow = 0;
    int computedMaxResults = Integer.MAX_VALUE;
    if (startRowStr != null) {
      startRow = Integer.parseInt(startRowStr);
      queryService.setFirstResult(startRow);
    }

    if (endRowStr != null) {
      int endRow = Integer.parseInt(endRowStr);
      computedMaxResults = endRow - startRow + 1;
      queryService.setMaxResults(computedMaxResults);
    }

    String orderBy = "";
    if (!hasSubentity) {
      final String sortBy = parameters.get(JsonConstants.SORTBY_PARAMETER);
      if (sortBy != null) {
        orderBy = sortBy;
      } else if (parameters.get(JsonConstants.ORDERBY_PARAMETER) != null) {
        orderBy = parameters.get(JsonConstants.ORDERBY_PARAMETER);
      }

      if (parameters.get(JsonConstants.SUMMARY_PARAMETER) != null
          && parameters.get(JsonConstants.SUMMARY_PARAMETER).trim().length() > 0) {
        queryService.setSummarySettings(parameters.get(JsonConstants.SUMMARY_PARAMETER));
      } else if (parameters.containsKey(SelectorConstants.DS_REQUEST_SELECTOR_ID_PARAMETER)
          && entityIsViewWithConcatenatedPK(entityName)) {
        // To avoid performance problems, views whose primary key is built with the concatenation of
        // several columns must not use that key for predictable sorting
        String idProperty = getSelectorValueFieldProperty(entityName,
            parameters.get(SelectorConstants.DS_REQUEST_SELECTOR_ID_PARAMETER));
        orderBy += (orderBy.isEmpty() ? "" : ",") + idProperty;
      } else {
        // Always append id to the orderby to make a predictable sorting
        orderBy += (orderBy.isEmpty() ? "" : ",") + JsonConstants.ID;
      }
    } else {
      orderBy = JsonConstants.IDENTIFIER;
    }

    queryService.setOrderBy(orderBy);

    // compute a new startrow if the targetrecordid was passed in
    int targetRowNumber = -1;
    if (!forCountOperation && !directNavigation
        && parameters.containsKey(JsonConstants.TARGETRECORDID_PARAMETER)) {
      final String targetRecordId = parameters.get(JsonConstants.TARGETRECORDID_PARAMETER);
      if (StringUtils.isNotBlank(targetRecordId) && !"null".equals(targetRecordId)) {
        targetRowNumber = queryService.getRowNumber(targetRecordId);
      }
      if (targetRowNumber != -1) {
        startRow = targetRowNumber;
        // if the startrow is really low, then just read from 0
        // to make sure that we have a full page of data to display
        if (startRow < (computedMaxResults / 2)) {
          startRow = 0;
        } else {
          startRow -= 20;
        }
        queryService.setFirstResult(startRow);
      }
      queryService.clearCachedValues();
    }
    if (!forCountOperation) {
      queryService.setAdditionalProperties(JsonUtils.getAdditionalProperties(parameters));
      // joining associated entities actually proved to be slower than doing
      // individual queries for them... so disabling this functionality for now
      // queryService.setJoinAssociatedEntities(true);
    }
    return queryService;
  }

  private void removeWhereParameter(Map<String, String> parameters) {
    if (parameters.containsKey(JsonConstants.WHERE_AND_FILTER_CLAUSE)) {
      parameters.remove(JsonConstants.WHERE_AND_FILTER_CLAUSE);
    }
  }

  // Given a map of parameters, returns a string with the pairs key:value
  public static String convertParameterToString(Map<String, String> parameters) {
    String paramMsg = "";
    for (String paramKey : parameters.keySet()) {
      paramMsg += paramKey + ":" + parameters.get(paramKey) + "\n";
    }
    return paramMsg;
  }

  private boolean entityIsViewWithConcatenatedPK(String entityName) {
    Entity entity = ModelProvider.getInstance().getEntity(entityName);
    if (entity.isView()) {
      // If a view has a concatenation of several column values as its primary key value, the length
      // of the primary key will be higher than 32, the default size for a UUID.
      List<Column> primaryKeys = ModelProvider.getInstance()
          .getTable(entity.getTableName())
          .getPrimaryKeyColumns();
      return primaryKeys.size() > 0 && primaryKeys.get(0).getFieldLength() > DEFAULT_ID_LENGTH;
    }
    return false;
  }

  private String getSelectorValueFieldProperty(String entityName, String selectorId) {
    try {
      OBContext.setAdminMode(false); // Need access to Application Dictionary information
      Selector selector = OBDal.getInstance().get(Selector.class, selectorId);
      if (selector != null && selector.getValuefield() != null
          && selector.getValuefield().getProperty() != null) {
        return selector.getValuefield().getProperty();
      }
      return JsonConstants.ID;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void addWritableAttribute(List<JSONObject> jsonObjects) throws JSONException {
    for (JSONObject jsonObject : jsonObjects) {
      final Object rowClient = getFKValue(jsonObject, "client", Client.ENTITY_NAME);
      final Object rowOrganization = getFKValue(jsonObject, "organization",
          Organization.ENTITY_NAME);

      if (rowClient == null || !(rowClient instanceof String) || rowOrganization == null
          || !(rowOrganization instanceof String)) {
        continue;
      }
      final String currentClientId = OBContext.getOBContext().getCurrentClient().getId();
      if (!rowClient.equals(currentClientId)) {
        jsonObject.put("_readOnly", true);
      } else {
        boolean writable = OBContext.getOBContext()
            .getWritableOrganizations()
            .contains(rowOrganization);
        if (!writable && isOrganizationEntity(jsonObject)) {
          writable = OBContext.getOBContext()
              .getDeactivatedOrganizations()
              .contains(rowOrganization);
        }
        if (!writable) {
          jsonObject.put("_readOnly", true);
        }
      }
    }
  }

  private boolean isOrganizationEntity(JSONObject json) throws JSONException {
    return json.has(JsonConstants.ENTITYNAME)
        && Organization.ENTITY_NAME.equals(json.get(JsonConstants.ENTITYNAME));
  }

  /**
   * Returns the value for a FK property, in case the entity of the row is the referencedEntity for
   * that FK, it returns the row id.
   */
  private Object getFKValue(JSONObject row, String propertyName, String referencedEntityName)
      throws JSONException {
    Object value = null;
    if (row.has(propertyName)) {
      value = row.get(propertyName);
    } else if (row.has(JsonConstants.ENTITYNAME)
        && referencedEntityName.equals(row.get(JsonConstants.ENTITYNAME))
        && row.has(BaseOBObject.ID)) {
      value = row.get(BaseOBObject.ID);
    }
    return value;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.service.json.JsonDataService#remove(java.util.Map)
   */
  @Override
  public String remove(Map<String, String> parameters) {
    final String id = parameters.get(JsonConstants.ID);
    if (id == null) {
      return JsonUtils.convertExceptionToJson(new IllegalStateException("No id parameter"));
    }
    final String entityName = parameters.get(JsonConstants.ENTITYNAME);
    if (entityName == null) {
      return JsonUtils.convertExceptionToJson(new IllegalStateException("No entityName parameter"));
    }
    BaseOBObject bob = OBDal.getInstance().get(entityName, id);
    if (bob != null) {

      try {
        // create the result info before deleting to prevent Hibernate errors
        final DataToJsonConverter toJsonConverter = OBProvider.getInstance()
            .get(DataToJsonConverter.class);
        final List<JSONObject> jsonObjects = toJsonConverter
            .toJsonObjects(Collections.singletonList(bob));

        final JSONObject jsonResult = new JSONObject();
        final JSONObject jsonResponse = new JSONObject();
        jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
        jsonResponse.put(JsonConstants.RESPONSE_DATA, new JSONArray(jsonObjects));
        jsonResult.put(JsonConstants.RESPONSE_RESPONSE, jsonResponse);
        OBDal.getInstance().commitAndClose();

        doPreAction(parameters, jsonObjects.toString(), DataSourceAction.REMOVE);

        // now do the real delete in a separate transaction
        // to prevent side effects that a child can not be deleted
        // from its parent
        // https://issues.openbravo.com/view.php?id=21229
        bob = OBDal.getInstance().get(entityName, id);
        OBDal.getInstance().remove(bob);

        final String result = doPostAction(parameters, jsonResult.toString(),
            DataSourceAction.REMOVE, null);

        OBDal.getInstance().commitAndClose();

        return result;
      } catch (Throwable t) {
        Throwable localThrowable = DbUtility.getUnderlyingSQLException(t);
        if (!(localThrowable instanceof OBException
            && !((OBException) localThrowable).isLogExceptionNeeded())) {
          log.error("Error removing object of " + entityName + " entity", localThrowable);
        }
        return JsonUtils.convertExceptionToJson(t);
      }
    } else {
      return JsonUtils.convertExceptionToJson(new IllegalStateException("Object not found"));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.service.json.JsonDataService#add(java.util.Map, java.lang.String)
   */
  @Override
  public String add(Map<String, String> parameters, String content) {
    parameters.put(ADD_FLAG, "true");
    return update(parameters, content);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.service.json.JsonDataService#update(java.util.Map, java.lang.String)
   */
  @Override
  public String update(Map<String, String> parameters, String content) {
    OBContext.setCrossOrgReferenceAdminMode();
    try {
      final boolean sendOriginalIdBack = "true"
          .equals(parameters.get(JsonConstants.SEND_ORIGINAL_ID_BACK));

      final JsonToDataConverter fromJsonConverter = OBProvider.getInstance()
          .get(JsonToDataConverter.class);

      String localContent = content;
      if (parameters.containsKey(ADD_FLAG)) {
        localContent = doPreAction(parameters, content, DataSourceAction.ADD);
      } else {
        localContent = doPreAction(parameters, content, DataSourceAction.UPDATE);
      }

      final Object jsonContent = getContentAsJSON(localContent);
      final List<BaseOBObject> bobs;
      final List<JSONObject> originalData = new ArrayList<JSONObject>();
      if (jsonContent instanceof JSONArray) {
        bobs = fromJsonConverter.toBaseOBObjects((JSONArray) jsonContent);
        final JSONArray jsonArray = (JSONArray) jsonContent;
        for (int i = 0; i < jsonArray.length(); i++) {
          originalData.add(jsonArray.getJSONObject(i));
        }
      } else {
        final JSONObject jsonObject = (JSONObject) jsonContent;
        originalData.add(jsonObject);
        // now set the id and entityname from the parameters if it was set
        if (!jsonObject.has(JsonConstants.ID) && parameters.containsKey(JsonConstants.ID)) {
          jsonObject.put(JsonConstants.ID, parameters.containsKey(JsonConstants.ID));
        }
        if (!jsonObject.has(JsonConstants.ENTITYNAME)
            && parameters.containsKey(JsonConstants.ENTITYNAME)) {
          jsonObject.put(JsonConstants.ENTITYNAME, parameters.get(JsonConstants.ENTITYNAME));
        }

        bobs = Collections
            .singletonList(fromJsonConverter.toBaseOBObject((JSONObject) jsonContent));
      }

      if (fromJsonConverter.hasErrors()) {
        OBDal.getInstance().rollbackAndClose();
        // report the errors
        final JSONObject jsonResult = new JSONObject();
        final JSONObject jsonResponse = new JSONObject();
        jsonResponse.put(JsonConstants.RESPONSE_STATUS,
            JsonConstants.RPCREQUEST_STATUS_VALIDATION_ERROR);
        final JSONObject errorsObject = new JSONObject();
        for (JsonConversionError error : fromJsonConverter.getErrors()) {
          errorsObject.put(error.getProperty().getName(), error.getThrowable().getMessage());
        }
        jsonResponse.put(JsonConstants.RESPONSE_ERRORS, errorsObject);
        jsonResult.put(JsonConstants.RESPONSE_RESPONSE, jsonResponse);
        return jsonResult.toString();
      } else {
        for (BaseOBObject bob : bobs) {
          OBDal.getInstance().save(bob);
        }
        OBDal.getInstance().flush();

        // business event handlers can change the data
        // flush again before refreshing, refreshing can
        // potentially remove any in-memory changes
        int countFlushes = 0;
        while (OBDal.getInstance().isSessionDirty()) {
          OBDal.getInstance().flush();
          countFlushes++;
          // arbitrary point to give up...
          if (countFlushes > 100) {
            throw new OBException("Infinite loop in flushing when persisting json: " + content);
          }
        }

        // Objects might have been modified in DB through triggers, let's force them to be fetched
        // DB again, to do so session is cleared (any possible modification is already persisted by
        // previous flush).
        // Using OBDal.refresh does not perform well, see issue
        // https://issues.openbravo.com/view.php?id=30308
        OBDal.getInstance().getSession().clear();

        final List<BaseOBObject> refreshedBobs = new ArrayList<BaseOBObject>();
        for (BaseOBObject bob : bobs) {
          // forcing fetch from DB
          BaseOBObject refreshedBob = OBDal.getInstance().get(bob.getEntityName(), bob.getId());

          // if object has computed columns refresh from the database too
          if (refreshedBob.getEntity().hasComputedColumns()) {
            OBDal.getInstance()
                .getSession()
                .refresh(refreshedBob.get(Entity.COMPUTED_COLUMNS_PROXY_PROPERTY));
          }
          refreshedBobs.add(refreshedBob);
        }

        // almost successful, now create the response
        // needs to be done before the close of the session
        final DataToJsonConverter toJsonConverter = OBProvider.getInstance()
            .get(DataToJsonConverter.class);
        toJsonConverter.setAdditionalProperties(JsonUtils.getAdditionalProperties(parameters));
        final List<JSONObject> jsonObjects = toJsonConverter.toJsonObjects(refreshedBobs);

        if (sendOriginalIdBack) {
          // now it is assumed that the jsonObjects are the same size and the same location
          // in the array
          if (jsonObjects.size() != originalData.size()) {
            throw new OBException("Unequal sizes in json data processed " + jsonObjects.size() + " "
                + originalData.size());
          }

          // now add the old id back
          for (int i = 0; i < originalData.size(); i++) {
            final JSONObject original = originalData.get(i);
            final JSONObject ret = jsonObjects.get(i);
            if (original.has(JsonConstants.ID) && original.has(JsonConstants.NEW_INDICATOR)) {
              ret.put(JsonConstants.ORIGINAL_ID, original.get(JsonConstants.ID));
            }
          }
        }

        final JSONObject jsonResult = new JSONObject();
        final JSONObject jsonResponse = new JSONObject();
        jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
        jsonResponse.put(JsonConstants.RESPONSE_DATA, new JSONArray(jsonObjects));
        jsonResult.put(JsonConstants.RESPONSE_RESPONSE, jsonResponse);

        final String result;
        if (parameters.containsKey(ADD_FLAG)) {
          result = doPostAction(parameters, jsonResult.toString(), DataSourceAction.ADD, content);
        } else {
          result = doPostAction(parameters, jsonResult.toString(), DataSourceAction.UPDATE,
              content);
        }

        OBDal.getInstance().commitAndClose();

        return result;
      }
    } catch (Throwable t) {
      Throwable localThrowable = DbUtility.getUnderlyingSQLException(t);
      if (!(localThrowable instanceof OBException
          && !((OBException) localThrowable).isLogExceptionNeeded())) {
        if (parameters.containsKey(ADD_FLAG)) {
          log.error("Error adding new object", localThrowable);
        } else {
          log.error("Error updating object", localThrowable);
        }
      }
      return JsonUtils.convertExceptionToJson(localThrowable);
    } finally {
      OBContext.restorePreviousCrossOrgReferenceMode();
    }
  }

  private Object getContentAsJSON(String content) throws JSONException {
    Check.isNotNull(content, "Content must be set");
    final Object jsonRepresentation;
    if (content.trim().startsWith("[")) {
      jsonRepresentation = new JSONArray(content);
    } else {
      final JSONObject jsonObject = new JSONObject(content);
      jsonRepresentation = jsonObject.get(JsonConstants.DATA);
    }
    return jsonRepresentation;
  }

  public static abstract class QueryResultWriter {
    public abstract void write(JSONObject json);
  }

  protected List<BaseOBObject> bobFetchTransformation(List<BaseOBObject> bobs,
      Map<String, String> parameters) {
    // If is override, take into account:
    // * If the number of the returned bobs change, there could be problems because endRow and
    // totalRows parameters will be out-of-sync with that the requester expects, and some values can
    // be missing in the following fetches. If there is no pagination (all values are returned at
    // once), there is no problem.
    // * If any bob is modified, the original entity is being modified, so a good practice could be
    // clone the bob (using DalUtil.copy, for example) before modify it, and then return the clone.

    return bobs;
  }

  /**
   * Hooks executed at the end of doPreAction and doPostAction to modify or to validate DataService
   * calls.
   */
  @Inject
  @Any
  private Instance<JsonDataServiceExtraActions> extraActions;

  protected String doPreAction(Map<String, String> parameters, String content,
      DataSourceAction action) {
    try {
      if (action == DataSourceAction.FETCH) {
        // In fetch operations there is no data. Just call doPreFetch and extraActions.
        doPreFetch(parameters);
        for (JsonDataServiceExtraActions extraAction : extraActions) {
          extraAction.doPreAction(parameters, new JSONArray(), action);
        }
        return "";
      }
      final Object contentObject = getContentAsJSON(content);
      final boolean isArray = contentObject instanceof JSONArray;
      final JSONArray data;
      if (isArray) {
        data = (JSONArray) contentObject;
      } else {
        final JSONObject request = new JSONObject(content);
        data = new JSONArray(Collections.singleton(request.getJSONObject(JsonConstants.DATA)));
      }

      final JSONArray newData = new JSONArray();
      for (int i = 0; i < data.length(); i++) {
        final JSONObject dataElement = data.getJSONObject(i);

        // do the pre thing
        switch (action) {
          case UPDATE:
            doPreUpdate(parameters, dataElement);
            break;
          case ADD:
            doPreInsert(parameters, dataElement);
            break;
          case REMOVE:
            doPreRemove(parameters, dataElement);
            break;
          default:
            throw new OBException("Unsupported action " + action);
        }

        // and set it in the new array
        newData.put(dataElement);
      }
      for (JsonDataServiceExtraActions extraAction : extraActions) {
        extraAction.doPreAction(parameters, newData, action);
      }

      // return the array directly
      if (isArray) {
        return newData.toString();
      }

      final JSONObject request = new JSONObject(content);
      request.put(JsonConstants.DATA, newData.getJSONObject(0));
      return request.toString();

    } catch (JSONException e) {
      throw new OBException(e);
    }
  }

  protected String doPostAction(Map<String, String> parameters, String content,
      DataSourceAction action, String originalObject) {
    try {
      // this gets the data before the insert, so that it can be used
      // for preprocessing, for example inserting an order
      final JSONObject json = new JSONObject(content);
      final JSONObject response = json.getJSONObject(JsonConstants.RESPONSE_RESPONSE);
      final JSONArray data = response.getJSONArray(JsonConstants.RESPONSE_DATA);
      final JSONArray newData = new JSONArray();
      for (int i = 0; i < data.length(); i++) {
        final JSONObject dataElement = data.getJSONObject(i);

        // do the pre thing
        switch (action) {
          case FETCH:
            doPostFetch(parameters, dataElement);
            break;
          case UPDATE:
            doPostUpdate(parameters, dataElement, originalObject);
            break;
          case ADD:
            doPostInsert(parameters, dataElement, originalObject);
            break;
          case REMOVE:
            doPostRemove(parameters, dataElement);
            break;
          default:
            throw new OBException("Unsupported action " + action);
        }

        // and set it in the new array
        newData.put(dataElement);
      }
      // update the response with the changes, make it a string
      response.put(JsonConstants.RESPONSE_DATA, newData);
      json.put(JsonConstants.RESPONSE_RESPONSE, response);

      for (JsonDataServiceExtraActions extraAction : extraActions) {
        extraAction.doPostAction(parameters, json, action, originalObject);
      }

      return json.toString();
    } catch (JSONException e) {
      throw new OBException(e);
    }
  }

  /**
   * Is called before the actual remove of the object. The toRemove object contains the id and
   * entity name of the to-be-deleted object.
   */
  protected void doPreRemove(Map<String, String> parameters, JSONObject toRemove)
      throws JSONException {

  }

  /**
   * Is called after the remove in the database but before the commit. The removed parameter object
   * can be changed, the changes are sent to the client. This method is called in the same
   * transaction as the remove action.
   */
  protected void doPostRemove(Map<String, String> parameters, JSONObject removed)
      throws JSONException {

  }

  /**
   * Is called before fetching an object. This method is called in the same transaction as the main
   * fetch operation.
   */
  protected void doPreFetch(Map<String, String> parameters) throws JSONException {

  }

  /**
   * Is called after fetching an object before the result is sent to the client, the fetched
   * {@link JSONObject} can be changed. The changes are sent to the client. This method is called in
   * the same transaction as the main fetch operation.
   */
  protected void doPostFetch(Map<String, String> parameters, JSONObject fetched)
      throws JSONException {

  }

  /**
   * Is called before an object is inserted. The toInsert {@link JSONObject} can be changed, the
   * changes are persisted to the database. This method is called in the same transaction as the
   * insert.
   */
  protected void doPreInsert(Map<String, String> parameters, JSONObject toInsert)
      throws JSONException {

  }

  /**
   * Is called after the insert action in the same transaction as the insert. The inserted
   * {@link JSONObject} can be changed, the changes are sent to the client.
   * 
   * The originalToInsert contains the json object/array string as it was passed into the
   * doPreInsert method. The inserted JSONObject is the object read from the database after it was
   * inserted. So it contains the changes done by stored procedures.
   */
  protected void doPostInsert(Map<String, String> parameters, JSONObject inserted,
      String originalToInsert) throws JSONException {
    // final String id = inserted.getString(JsonConstants.ID);
    // final String entityName = inserted.getString(JsonConstants.ENTITYNAME);

  }

  /**
   * Called before the update of an object. Is called in the same transaction as the main update
   * operation. Changes to the toUpdate {@link JSONObject} are persisted in the database.
   */
  protected void doPreUpdate(Map<String, String> parameters, JSONObject toUpdate)
      throws JSONException {
  }

  /**
   * Called after the updates have been done, within the same transaction as the main update.
   * Changes to the updated {@link JSONObject} are sent to the client (but not persisted to the
   * database).
   * 
   * The originalToUpdate contains the json object/array string as it was passed into the
   * doPreUpdate method. The updated JSONObject is the object read from the database after it was
   * updated. So it contains all the changes done by stored procedures.
   */
  protected void doPostUpdate(Map<String, String> parameters, JSONObject updated,
      String originalToUpdate) throws JSONException {
  }

  public enum DataSourceAction {
    FETCH, ADD, UPDATE, REMOVE
  }

  /**
   * Checks whether a criteria is filtering by ID property
   * 
   * @param jsonCriteria
   *          criteria to check
   * @return <code>true</code> if the criteria is filtering by ID
   */
  private boolean isIDCriteria(JSONObject jsonCriteria) {
    if (!jsonCriteria.has("criteria")) {
      return false;
    }

    try {
      JSONArray criteria = jsonCriteria.getJSONArray("criteria");
      for (int i = 0; i < criteria.length(); i++) {
        JSONObject criterion = criteria.getJSONObject(i);
        if (criterion.has("fieldName")
            && JsonConstants.ID.equals(criterion.getString("fieldName"))) {
          return true;
        }
      }
    } catch (JSONException e) {
      log.error("Error parsing criteria " + jsonCriteria, e);
    }
    return false;
  }
}
