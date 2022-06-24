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
 * All portions are Copyright (C) 2013-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.datasource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.inject.Inject;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.WindowAccess;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.ReferencedTree;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.TableTree;
import org.openbravo.service.datasource.CheckTreeOperationManager.ActionResponse;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;

/**
 * The TreeDatasourceService is an abstract class that provides a simple way of returning data in
 * the correct format for a OBTreeGrid client side component using a datasource.
 * 
 */
public abstract class TreeDatasourceService extends DefaultDataSourceService {
  private static final Logger log = LogManager.getLogger();
  private static final String JSON_PREFIX = "<SCRIPT>//'\"]]>>isc_JSONResponseStart>>";
  private static final String JSON_SUFFIX = "//isc_JSONResponseEnd";
  protected static final String ROOT_NODE_DB = "0";
  protected static final String ROOT_NODE_CLIENT = "-1";
  private static final String AD_ORG_TABLE_ID = "155";
  private static final String ROOT_ORGANIZATION_ID = "0";
  private static final String SUMMARY_LEVEL_PROPERTY = "summaryLevel";
  private static final String ACCESSIBLE_ORG_TREE = "#AccessibleOrgTree";

  @Inject
  private DataSourceServiceProvider dataSourceServiceProvider;

  // A CheckTreeOperationManager allows to check if an action on a node is valid before actually
  // doing it
  @Inject
  @Any
  private Instance<CheckTreeOperationManager> checkTreeOperationManagers;

  /**
   * This method is called when a new record is created in a tree table. It calls the addNewNode
   * abstract method, which is implemented in the classes that extend TreeDatasourceService
   */
  @Override
  public final String add(Map<String, String> parameters, String content) {
    try {
      // We can't get the bob from DAL, it has not been saved yet
      JSONObject bobProperties = new JSONObject(parameters.get("jsonBob"));
      addNewNode(bobProperties);
    } catch (Exception e) {
      log.error("Error while adding the tree node", e);
      throw new OBException(e);
    }
    // This is called from TreeTablesEventHandler, no need to return anything
    return "";
  }

  /**
   * Classes that extend TreeDatasourceService this method must implement this method to handle the
   * creation of a node in a tree table
   */
  protected abstract void addNewNode(JSONObject bobProperties);

  /**
   * This method is called when a new record is deleted in a tree table. It calls the deleteNode
   * abstract method, which is implemented in the classes that extend TreeDatasourceService
   */
  @Override
  public final String remove(Map<String, String> parameters) {
    try {
      // We can't get the bob from DAL, it has not been saved yet
      JSONObject bobProperties = new JSONObject(parameters.get("jsonBob"));
      String bobId = bobProperties.getString("id");
      String entityName = bobProperties.getString("_entity");
      Entity entity = ModelProvider.getInstance().getEntity(entityName);
      String nodeDeletionPolicy = this.getNodeDeletionPolicy(entity);
      if ("DNAIHC".equals(nodeDeletionPolicy) && this.nodeHasChildren(entity, bobId, null)) {
        // Handle the deletion policy "Do Not Allow If Has Children" if it applies to this entity
        throw new OBException(OBMessageUtils.messageBD("CannotDeleteNodeBecauseChildren"));
      }

      this.deleteNode(bobProperties);
    } catch (Exception e) {
      log.error("Error while deleting tree node: ", e);
      throw new OBException(e);
    }

    // This is called from TreeTablesEventHandler, no need to return anything
    return "";
  }

  @Override
  public void checkEditDatasourceAccess(Map<String, String> parameter) {
    final OBContext obContext = OBContext.getOBContext();
    String tableId = parameter.get("referencedTableId");
    try {
      Entity treeEntity = ModelProvider.getInstance().getEntityByTableId(tableId);
      if (treeEntity != null) {
        obContext.getEntityAccessChecker().checkWritableAccess(treeEntity);
      }
    } catch (OBSecurityException e) {
      handleExceptionUnsecuredDSAccess(e);
    }
  }

  /**
   * Classes that extend TreeDatasourceService this method must implement this method to handle the
   * deletion of a node in a tree table
   */
  protected abstract void deleteNode(JSONObject bobProperties);

  /**
   * Classes that extend TreeDatasourceService this method must implement this method to handle the
   * check if a node has children in a tree table
   */
  protected boolean nodeHasChildren(Entity entity, String nodeId, String hqlWhereClause) {
    // By default it returns true, so if the 'Don't allow to delete node if they have children'
    // delete policy is selected, a node will be deletable only if this method is overwritten
    return true;
  }

  /**
   * Fetches some tree nodes Two operation modes:
   * 
   * - If a criteria is included in the parameters, this method will return the nodes that conform
   * to the criteria plus all its parent until reaching the root nodes
   * 
   * - Otherwise, the child nodes of a given node (its node id included in the parameters) are
   * returned
   * 
   * Either the tabId (when it is called from a tree window) or the treeReferenceId (when called
   * from a tree reference) must be included in the parameters
   * 
   * If the datasource were to return a number of nodes higher than the limit (defined in the
   * TreeDatasourceFetchLimit preference), an empty data is returned and an error is shown to the
   * user
   */
  @Override
  public final String fetch(Map<String, String> parameters) {
    OBContext.setAdminMode(true);
    final JSONObject jsonResult = new JSONObject();

    try {
      // If the distinct parameter is included in the parameters, delegate to the default standard
      // datasource
      if (parameters.containsKey(JsonConstants.DISTINCT_PARAMETER)) {
        String tabId = parameters.get("_tabId");
        String tableId = parameters.get("_tableId");
        Entity entity = null;
        if (tabId != null) {
          Tab tab = OBDal.getInstance().get(Tab.class, tabId);
          entity = ModelProvider.getInstance().getEntityByTableId(tab.getTable().getId());
        } else if (tableId != null) {
          entity = ModelProvider.getInstance().getEntityByTableId(tableId);
        }
        DataSourceService dataSource = dataSourceServiceProvider.getDataSource(entity.getName());
        return dataSource.fetch(parameters);
      }

      String tabId = parameters.get("tabId");
      String treeReferenceId = parameters.get("treeReferenceId");
      Tab tab = null;
      Table table = null;
      String hqlTreeWhereClause = null;
      String hqlTreeWhereClauseRootNodes = null;
      boolean fromTreeView = true;
      if (tabId != null) {
        tab = OBDal.getInstance().get(Tab.class, tabId);
        table = tab.getTable();
        hqlTreeWhereClause = tab.getHqlwhereclause();
        hqlTreeWhereClauseRootNodes = tab.getHQLWhereClauseForRootNodes();

      } else if (treeReferenceId != null) {
        ReferencedTree treeReference = OBDal.getInstance()
            .get(ReferencedTree.class, treeReferenceId);
        table = treeReference.getTable();
        hqlTreeWhereClause = treeReference.getHQLSQLWhereClause();
        hqlTreeWhereClauseRootNodes = treeReference.getHQLWhereClauseForRootNodes();
        fromTreeView = false;
      } else {
        log.error(
            "A request to the TreeDatasourceService must include the tabId or the treeReferenceId parameter");
        final JSONObject jsonResponse = new JSONObject();
        JSONArray responseData = new JSONArray();
        jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_FAILURE);
        jsonResponse.put(JsonConstants.RESPONSE_DATA, responseData);
        jsonResponse.put(JsonConstants.RESPONSE_TOTALROWS, 0);
        jsonResponse.put(JsonConstants.RESPONSE_STARTROW, 0);
        jsonResponse.put(JsonConstants.RESPONSE_ENDROW, 0);
        jsonResult.put(JsonConstants.RESPONSE_RESPONSE, jsonResponse);
        return jsonResult.toString();
      }

      Entity entity = ModelProvider.getInstance().getEntityByTableId(table.getId());
      if (!hasAccess(entity, tab, fromTreeView)) {
        return noAccessError().toString();
      }

      if (hqlTreeWhereClause != null) {
        hqlTreeWhereClause = this.substituteParameters(hqlTreeWhereClause, parameters);
      }
      if (hqlTreeWhereClauseRootNodes != null) {
        hqlTreeWhereClauseRootNodes = this.substituteParameters(hqlTreeWhereClauseRootNodes,
            parameters);
      }

      JSONArray responseData = null;
      boolean tooManyNodes = false;
      // Do not consider dummy criteria as valid criteria
      boolean validCriteria = false;
      JSONArray criterias = (JSONArray) JsonUtils.buildCriteria(parameters).get("criteria");

      String parentId = parameters.get("parentId");

      for (int i = 0; i < criterias.length(); i++) {
        JSONObject criteria = criterias.getJSONObject(i);
        if (!isDummyCriteria(criteria) && !isSubtabCriteria(entity, criteria)
            && !isParentIdCriteria(criteria) && !isEmptyCriteria(criteria)) {
          validCriteria = true;
        } else if (parentId == null && isParentIdCriteria(criteria)) {
          parentId = criteria.getString("value");
        }
      }

      Map<String, Object> datasourceSpecificParams = this.getDatasourceSpecificParams(parameters);

      if (validCriteria && parentId.equals(ROOT_NODE_CLIENT)) {

        try {
          // Obtain the list of nodes that conforms to the criteria
          List<String> filteredNodes = getFilteredNodes(table, parameters);
          if (!filteredNodes.isEmpty()) {
            // Return the filtered nodes and its parents
            responseData = fetchFilteredNodes(parameters, datasourceSpecificParams, filteredNodes);
          } else {
            responseData = new JSONArray();
          }
        } catch (TooManyTreeNodesException e) {
          tooManyNodes = true;
        }
      } else {
        // Fetch the children of a given node
        try {
          responseData = fetchNodeChildren(parameters, datasourceSpecificParams, parentId,
              hqlTreeWhereClause, hqlTreeWhereClauseRootNodes);
        } catch (TooManyTreeNodesException e) {
          tooManyNodes = true;
        }
      }

      final JSONObject jsonResponse = new JSONObject();
      if (tooManyNodes) {
        responseData = new JSONArray();
        jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_FAILURE);
        final JSONObject error = new JSONObject();
        error.put("type", "tooManyNodes");
        jsonResponse.put(JsonConstants.RESPONSE_ERROR, error);
        jsonResponse.put(JsonConstants.RESPONSE_ERRORS, JsonConstants.RPCREQUEST_STATUS_FAILURE);
      } else {
        jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
      }
      jsonResponse.put(JsonConstants.RESPONSE_DATA, responseData);
      jsonResponse.put(JsonConstants.RESPONSE_TOTALROWS, responseData.length());
      jsonResponse.put(JsonConstants.RESPONSE_STARTROW, 0);
      jsonResponse.put(JsonConstants.RESPONSE_ENDROW, responseData.length() - 1);
      jsonResult.put(JsonConstants.RESPONSE_RESPONSE, jsonResponse);
    } catch (Throwable t) {
      log.error("Error on tree datasource", t);
      return JsonUtils.convertExceptionToJson(t);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonResult.toString();
  }

  @Override
  public void checkFetchDatasourceAccess(Map<String, String> parameter) {
    final OBContext obContext = OBContext.getOBContext();
    String tableId = parameter.get("referencedTableId");
    try {
      Entity treeEntity = ModelProvider.getInstance().getEntityByTableId(tableId);
      if (treeEntity != null) {
        obContext.getEntityAccessChecker().checkReadableAccess(treeEntity);
      }
    } catch (OBSecurityException e) {
      handleExceptionUnsecuredDSAccess(e);
    }
  }

  protected abstract Map<String, Object> getDatasourceSpecificParams(
      Map<String, String> parameters);

  private boolean isSubtabCriteria(Entity entity, JSONObject jsonCriteria) {
    try {
      if (jsonCriteria.has("criteria")) {
        JSONArray criteriaArray = (JSONArray) jsonCriteria.get("criteria");
        if (criteriaArray.length() == 1) {
          return isSubtabCriteria(entity, criteriaArray.getJSONObject(0));
        } else {
          return false;
        }
      }
      if (jsonCriteria.has("fieldName")) {
        String fieldName = jsonCriteria.getString("fieldName");
        if (entity.hasProperty(fieldName)) {
          Property property = entity.getProperty(fieldName);
          if (property.isParent()) {
            return true;
          } else {
            return false;
          }
        } else {
          return false;
        }
      } else {
        return false;
      }
    } catch (JSONException e) {
      return false;
    }
  }

  private boolean isEmptyCriteria(JSONObject jsonCriteria) {
    try {
      return jsonCriteria.has("criteria") && jsonCriteria.getJSONArray("criteria").length() == 0;
    } catch (JSONException e) {
      return false;
    }
  }

  private boolean isDummyCriteria(JSONObject jsonCriteria) {
    try {
      if (jsonCriteria.has("criteria")) {
        JSONArray criteriaArray = (JSONArray) jsonCriteria.get("criteria");
        if (criteriaArray.length() == 1) {
          return isDummyCriteria(criteriaArray.getJSONObject(0));
        } else {
          return false;
        }
      }
      if (jsonCriteria.has("fieldName") && "_dummy".equals(jsonCriteria.get("fieldName"))) {
        return true;
      } else {
        if (jsonCriteria.has("criteria")) {
          JSONArray criteriaArray = jsonCriteria.getJSONArray("criteria");
          if (criteriaArray.length() == 0) {
            return true;
          }
        }
        return false;
      }
    } catch (JSONException e) {
      return false;
    }
  }

  private boolean isParentIdCriteria(JSONObject jsonCriteria) {
    boolean isParentIdCriteria = false;
    try {
      if (jsonCriteria.has("criteria")) {
        JSONArray criteriaArray = (JSONArray) jsonCriteria.get("criteria");
        if (criteriaArray.length() == 1) {
          return isParentIdCriteria(criteriaArray.getJSONObject(0));
        } else {
          return false;
        }
      }
      if (jsonCriteria.has("fieldName") && "parentId".equals(jsonCriteria.get("fieldName"))) {
        isParentIdCriteria = true;
      }
    } catch (JSONException e) {
      return false;
    }
    return isParentIdCriteria;
  }

  /**
   * Checks if an entity is accessible
   * 
   * @param entity
   *          Entity whose accessibility is to be determined
   * @param tab
   *          Tab from where the entity wants to be accessed. Can be null.
   * @param fromReference
   *          flag that determines if the entity is being accessed from a reference o from a tree
   *          grid view
   * @return true if the entity is accessible, false otherwise
   */
  private boolean hasAccess(Entity entity, Tab tab, boolean fromReference) {
    boolean hasAccessToTable = true;
    boolean isDerivedReadable = OBContext.getOBContext()
        .getEntityAccessChecker()
        .isDerivedReadable(entity);
    try {
      OBContext.getOBContext().getEntityAccessChecker().checkReadable(entity);
    } catch (OBSecurityException e) {
      hasAccessToTable = false;
    }
    if (fromReference) {
      // If accessing the entity from a reference, it is enough for the entity to be derived
      // readable
      return hasAccessToTable || isDerivedReadable;
    }
    if (hasAccessToTable && tab != null) {
      // If the tab has been provided, check if the user has access to its window
      OBCriteria<WindowAccess> qWindowAccess = OBDal.getInstance()
          .createCriteria(WindowAccess.class);
      qWindowAccess.add(Restrictions.eq(WindowAccess.PROPERTY_WINDOW, tab.getWindow()));
      qWindowAccess
          .add(Restrictions.eq(WindowAccess.PROPERTY_ROLE, OBContext.getOBContext().getRole()));
      hasAccessToTable = qWindowAccess.count() > 0;
    }
    return hasAccessToTable;
  }

  /**
   * Obtains the value of the node deletion policy for a particular tree based on the entity
   * 
   * @param entity
   *          Entity whose accessibility is to be determined
   * @return the value of the node deletion policy
   */
  private String getNodeDeletionPolicy(Entity entity) {
    try {
      String tableTreeNodeDeletionPolicy = "";

      Table table = OBDal.getInstance().get(Table.class, entity.getTableId());
      List<TableTree> tableTreeList = table.getADTableTreeList();
      if (tableTreeList.size() == 0) {
        throw new OBException("Error while determining the node deletion policy");
      }

      TableTree tableTree = tableTreeList.get(0);
      tableTreeNodeDeletionPolicy = tableTree.getNodeDeletionPolicy();

      return tableTreeNodeDeletionPolicy;
    } catch (Exception e) {
      throw new OBException("Error while determining the node deletion policy");
    }
  }

  /**
   * Given a criteria, return the list of nodes that conforms to the criteria If the number of
   * returned nodes is too high, throws the TooManyTreeNodesException exception
   * 
   * @param table
   *          tree table being fetched
   * @param parameters
   * @return the list of filtered nodes
   * @throws TooManyTreeNodesException
   */
  private List<String> getFilteredNodes(Table table, Map<String, String> parameters)
      throws TooManyTreeNodesException {
    List<String> filteredNodes = new ArrayList<String>();
    Entity entity = ModelProvider.getInstance().getEntityByTableId(table.getId());
    // Delegate on the default standard datasource to fetch the filtered nodes
    DataSourceService dataSource = dataSourceServiceProvider.getDataSource(entity.getName());
    String dsResult = dataSource.fetch(parameters);
    try {
      JSONObject jsonDsResult = new JSONObject(dsResult);
      JSONObject jsonResponse = jsonDsResult.getJSONObject(JsonConstants.RESPONSE_RESPONSE);
      JSONArray dataArray = jsonResponse.getJSONArray(JsonConstants.RESPONSE_DATA);

      // Check if the number of filtered results has reached the limit
      // An _endRow parameter is included in the parameters, being equal to the limit amount nodes.
      // If the number of nodes returned by the default datasource is equals to this limit, throw
      // the TooManyTreeNodesException exception
      int nRecords = dataArray.length();
      OBContext context = OBContext.getOBContext();
      int nMaxResults = -1;
      try {
        nMaxResults = Integer.parseInt(Preferences.getPreferenceValue("TreeDatasourceFetchLimit",
            false, context.getCurrentClient(), context.getCurrentOrganization(), context.getUser(),
            context.getRole(), null));
      } catch (Exception e) {
        nMaxResults = 1000;
      }
      if (nRecords >= nMaxResults) {
        throw new TooManyTreeNodesException();
      }

      for (int i = 0; i < nRecords; i++) {
        JSONObject data = dataArray.getJSONObject(i);
        String ref = data.getString("$ref");
        String id = ref.substring(ref.lastIndexOf("/") + 1);
        filteredNodes.add(id);
      }
    } catch (JSONException e) {
      log.error("Error while getting the filtered nodes from the datasource", e);
    }
    return filteredNodes;
  }

  /**
   * Explodes the list of filteredNodes to include in the response those nodes plus its parents
   * 
   * @param parameters
   * @param filteredNodes
   *          list of filtered nodes
   * @return a JSON array containing the filtered nodes plus all its parents until the root node is
   *         reached
   * @throws MultipleParentsException
   * @throws TooManyTreeNodesException
   */
  private JSONArray fetchFilteredNodes(Map<String, String> parameters,
      Map<String, Object> datasourceParameters, List<String> filteredNodes)
      throws MultipleParentsException, TooManyTreeNodesException {
    String tabId = parameters.get("tabId");
    String treeReferenceId = parameters.get("treeReferenceId");
    Tab tab = null;
    TableTree tableTree = null;
    String hqlTreeWhereClause = null;
    String hqlTreeWhereClauseRootNodes = null;
    if (tabId != null) {
      tab = OBDal.getInstance().get(Tab.class, tabId);
      tableTree = tab.getTableTree();
      hqlTreeWhereClause = tab.getHqlwhereclause();
      hqlTreeWhereClauseRootNodes = tab.getHQLWhereClauseForRootNodes();
    } else if (treeReferenceId != null) {
      ReferencedTree treeReference = OBDal.getInstance().get(ReferencedTree.class, treeReferenceId);
      tableTree = treeReference.getTableTreeCategory();
      hqlTreeWhereClause = treeReference.getHQLSQLWhereClause();
      hqlTreeWhereClauseRootNodes = treeReference.getHQLWhereClauseForRootNodes();
    } else {
      log.error(
          "A request to the TreeDatasourceService must include the tabId or the treeReferenceId parameter");
      return new JSONArray();
    }
    if (hqlTreeWhereClause != null) {
      hqlTreeWhereClause = this.substituteParameters(hqlTreeWhereClause, parameters);
    }
    if (hqlTreeWhereClauseRootNodes != null) {
      hqlTreeWhereClauseRootNodes = this.substituteParameters(hqlTreeWhereClauseRootNodes,
          parameters);
    }
    // If this property is true, the whereclause of the tabs does not need to be apllied to the
    // non root nodes
    boolean allowNotApplyingWhereClauseToChildren = !tableTree.isApplyWhereClauseToChildNodes();

    if (tableTree.isHasMultiparentNodes()) {
      return fetchFilteredNodesForTreesWithMultiParentNodes(parameters, datasourceParameters,
          tableTree, filteredNodes, hqlTreeWhereClause, hqlTreeWhereClauseRootNodes,
          allowNotApplyingWhereClauseToChildren);
    } else {
      return fetchFilteredNodesForTrueTrees(parameters, datasourceParameters, tableTree,
          filteredNodes, hqlTreeWhereClause, hqlTreeWhereClauseRootNodes,
          allowNotApplyingWhereClauseToChildren);
    }

  }

  private JSONArray fetchFilteredNodesForTrueTrees(Map<String, String> parameters,
      Map<String, Object> datasourceParameters, TableTree tableTree, List<String> filteredNodes,
      String hqlTreeWhereClause, String hqlTreeWhereClauseRootNodes,
      boolean allowNotApplyingWhereClauseToChildren) throws MultipleParentsException {

    JSONArray responseData = new JSONArray();
    Map<String, JSONObject> addedNodesMap = new HashMap<String, JSONObject>();

    try {
      for (String nodeId : filteredNodes) {
        JSONObject node = getJSONObjectByRecordId(parameters, datasourceParameters, nodeId);
        if (!allowNotApplyingWhereClauseToChildren && !this.nodeConformsToWhereClause(tableTree,
            node.getString("id"), hqlTreeWhereClause)) {
          // If the node does not conform the where clase, do not include it in the response
          continue;
        }
        JSONObject savedNode = addedNodesMap.get(node.getString("id"));
        if (hqlTreeWhereClauseRootNodes == null) {
          // If there is no hqlTreeWhereClauseRootNodes, include all the parents until reaching the
          // node with parentId ROOT_NODE
          if (savedNode == null) {
            // The nodes that conform to the filter will be flagged as filterHit to display them
            // using a diferent style
            node.put("filterHit", true);
            addedNodesMap.put(node.getString("id"), node);
          } else {
            savedNode.put("filterHit", true);
          }
          while (node.has("parentId") && !ROOT_NODE_CLIENT.equals(node.get("parentId"))
              && (allowNotApplyingWhereClauseToChildren || this.nodeConformsToWhereClause(tableTree,
                  node.getString("parentId"), hqlTreeWhereClause))) {
            nodeId = node.getString("parentId");
            node = getJSONObjectByNodeId(parameters, datasourceParameters, nodeId);
            savedNode = addedNodesMap.get(node.getString("id"));
            if (savedNode == null) {
              // All the parents will be shown open in the tree grid
              node.put("isOpen", true);
              addedNodesMap.put(node.getString("id"), node);
            } else {
              savedNode.put("isOpen", true);
            }
          }
        } else {
          // If a node has hqlTreeWhereClauseRootNodes, we have to make sure that the filtered node
          // is either a root node or a descendant of it
          Map<String, JSONObject> preAddedNodesMap = new HashMap<String, JSONObject>();
          if (savedNode == null) {
            node.put("filterHit", true);
            preAddedNodesMap.put(node.getString("id"), node);
          } else {
            savedNode.put("filterHit", true);
          }
          while (node.has("parentId") && !isRoot(node, hqlTreeWhereClauseRootNodes, tableTree)
              && (allowNotApplyingWhereClauseToChildren || this.nodeConformsToWhereClause(tableTree,
                  node.getString("parentId"), hqlTreeWhereClause))) {
            nodeId = node.getString("parentId");
            node = getJSONObjectByNodeId(parameters, datasourceParameters, nodeId);
            savedNode = addedNodesMap.get(node.getString("id"));
            if (savedNode == null) {
              node.put("isOpen", true);
              preAddedNodesMap.put(node.getString("id"), node);
            } else {
              savedNode.put("isOpen", true);
            }
          }
          // We have to make sure that the filtered node was not about the
          // root nodes as defined by the hqlTreeWhereClauseRootNodes
          if (this.nodeConformsToWhereClause(tableTree, node.getString("id"),
              hqlTreeWhereClauseRootNodes)) {
            addedNodesMap.putAll(preAddedNodesMap);
          }
        }
        if (allowNotApplyingWhereClauseToChildren || this.nodeConformsToWhereClause(tableTree,
            node.getString("parentId"), hqlTreeWhereClause)) {
          node.put("parentId", ROOT_NODE_CLIENT);
        }
      }

      // Add the values in the map to responsedata
      for (String key : addedNodesMap.keySet()) {
        if (addedNodesMap.get(key).has("filterHit")) {
          addedNodesMap.get(key).remove("filterHit");
        } else {
          addedNodesMap.get(key).put("notFilterHit", true);
        }
        responseData.put(addedNodesMap.get(key));
      }
    } catch (JSONException e) {
      log.error("Error while processing the filtered nodes from the datasource", e);
    }

    return responseData;
  }

  protected abstract JSONArray fetchFilteredNodesForTreesWithMultiParentNodes(
      Map<String, String> parameters, Map<String, Object> datasourceParameters, TableTree tableTree,
      List<String> filteredNodes, String hqlTreeWhereClause, String hqlTreeWhereClauseRootNodes,
      boolean allowNotApplyingWhereClauseToChildren)
      throws MultipleParentsException, TooManyTreeNodesException;

  /**
   * Checks if a node is a root node
   * 
   * @param node
   *          JSON objects that contains the properties of the node
   * @param hqlTreeWhereClauseRootNodes
   *          hqlWhereClause that defines the root nodes
   * @param tableTree
   *          tableTree that defines the tree category that defines the tree
   */
  private boolean isRoot(JSONObject node, String hqlTreeWhereClauseRootNodes, TableTree tableTree) {
    try {
      String nodeId = null;
      String parentId = null;
      nodeId = node.getString("id");
      parentId = node.getString("parentId");
      if (ROOT_NODE_CLIENT.equals(parentId)) {
        return true;
      }
      if (hqlTreeWhereClauseRootNodes != null) {
        return nodeConformsToWhereClause(tableTree, nodeId, hqlTreeWhereClauseRootNodes);
      } else {
        return false;
      }
    } catch (JSONException e) {
      return false;
    }
  }

  /**
   * Method that checks if a node conforms to a hqlWhereClause
   * 
   * @param tableTree
   *          tableTree that defines the tree category that defines the tree
   * @param nodeId
   *          id of the node to be checked
   * @param hqlWhereClause
   *          hql where clause to be applied
   */
  protected abstract boolean nodeConformsToWhereClause(TableTree tableTree, String nodeId,
      String hqlWhereClause);

  /**
   * If a where clause contains parameters, substitute the parameter with the actual value
   * 
   * @param hqlTreeWhereClause
   *          the original where clause as defined in the tab/selector
   * @param parameters
   * @return the updated where clause, having replaced the parameters with their actual values
   */
  protected String substituteParameters(String hqlTreeWhereClause, Map<String, String> parameters) {
    Pattern pattern = Pattern.compile("@\\S*@");
    Matcher matcher = pattern.matcher(hqlTreeWhereClause);
    HashMap<String, String> replacements = new HashMap<>();
    while (matcher.find()) {
      String contextPropertyName = hqlTreeWhereClause.substring(matcher.start(), matcher.end());
      String value = null;
      if (parameters.containsKey(contextPropertyName)) {
        value = parameters.get(contextPropertyName);
      } else if (parameters
          .containsKey(contextPropertyName.substring(1, contextPropertyName.length() - 1))) {
        // try again without the '@'
        value = parameters.get(contextPropertyName.substring(1, contextPropertyName.length() - 1));
      }
      replacements.put(contextPropertyName, "'" + StringEscapeUtils.escapeSql(value) + "'");
    }
    String hqlCopy = hqlTreeWhereClause;
    for (Entry<String, String> entry : replacements.entrySet()) {
      // if the key is not found in the request parameters, its value in the replacement list will
      // be 'null'
      String key = entry.getKey();
      if (entry.getValue().equals("'null'")) {
        // Strip the "@" from the key
        String keyWithoutAt = key.substring(1, key.length() - 1);
        hqlCopy = hqlCopy.replaceAll(key, getEscapedSessionAttribute(keyWithoutAt));
      } else {
        hqlCopy = hqlCopy.replaceAll(key, entry.getValue());
      }
    }
    return hqlCopy;
  }

  private String getEscapedSessionAttribute(String keyWithoutAt) {
    String value = (String) RequestContext.get().getSessionAttribute(keyWithoutAt);
    if (ACCESSIBLE_ORG_TREE.equals(keyWithoutAt)) {
      return value;
    }

    return "'" + StringEscapeUtils.escapeSql(value) + "'";
  }

  /**
   * @param parameters
   *          a map with the parameters of the request
   * @param datasourceParameters
   *          specific datasource parameters obtained using method
   *          {@link #getDatasourceSpecificParams(Map)}
   * @param nodeId
   *          id of the tree node
   * @return returns a json object with the definition of a node give its record id
   */
  protected abstract JSONObject getJSONObjectByRecordId(Map<String, String> parameters,
      Map<String, Object> datasourceParameters, String nodeId);

  /**
   * @param parameters
   *          a map with the parameters of the request
   * @param datasourceParameters
   *          specific datasource parameters obtained using method
   *          {@link #getDatasourceSpecificParams(Map)}
   * @param nodeId
   *          id of the tree node
   * @return returns a json object with the definition of a node give its node id
   */
  protected abstract JSONObject getJSONObjectByNodeId(Map<String, String> parameters,
      Map<String, Object> datasourceParameters, String nodeId) throws MultipleParentsException;

  /**
   * 
   * @param parameters
   *          a map with the parameters of the request
   * @param datasourceParameters
   *          specific datasource parameters
   * @param parentId
   *          id of the node whose children are to be retrieved
   * @param hqlWhereClause
   *          hql where clase of the tab/selector
   * @param hqlWhereClauseRootNodes
   *          hql where clause that define what nodes are roots
   * @throws JSONException
   * @throws TooManyTreeNodesException
   *           if the number of returned nodes were to be too high
   */
  protected abstract JSONArray fetchNodeChildren(Map<String, String> parameters,
      Map<String, Object> datasourceParameters, String parentId, String hqlWhereClause,
      String hqlWhereClauseRootNodes) throws JSONException, TooManyTreeNodesException;

  @Override
  /**
   * This method is called when a node is reparented
   * 
   * @param parameters
   * @param content
   *          json objects containing the definition of the updated node(s)
   * @return a valid response that contains the definition of the updated node(s)
   */
  public final String update(Map<String, String> parameters, String content) {
    OBContext.setAdminMode(true);

    String response = null;
    try {
      final JSONObject jsonObject = new JSONObject(content);
      if (content == null) {
        return "";
      }
      // These two parameters define the position where the node should be placed among its peer
      // nodes
      String prevNodeId = parameters.get("prevNodeId");
      String nextNodeId = parameters.get("nextNodeId");
      if (jsonObject.has("data")) {
        JSONObject data = jsonObject.getJSONObject("data");
        JSONObject oldValues = jsonObject.getJSONObject("oldValues");
        response = processNodeMovement(parameters, data, oldValues, prevNodeId, nextNodeId);
      } else if (jsonObject.has("transaction")) {
        // If more than one nodes are moved at the same time, we need to handle a transaction
        JSONArray jsonResultArray = new JSONArray();
        JSONObject transaction = jsonObject.getJSONObject("transaction");
        JSONArray operations = transaction.getJSONArray("operations");
        for (int i = 0; i < operations.length(); i++) {
          JSONObject operation = operations.getJSONObject(i);
          JSONObject data = operation.getJSONObject("data");
          JSONObject oldValues = operation.getJSONObject("oldValues");
          jsonResultArray
              .put(processNodeMovement(parameters, data, oldValues, prevNodeId, nextNodeId));
        }
        response = JSON_PREFIX + jsonResultArray.toString() + JSON_SUFFIX;
      }
    } catch (Exception e) {
      log.error("Error while moving tree node", e);
      return JsonUtils.convertExceptionToJson(e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return response;
  }

  /**
   * 
   * @param parameters
   * @param data
   *          updated values of the node, including its new parent
   * @param oldValues
   *          previous values of the node, including its previous parent
   * @param prevNodeId
   * @param nextNodeId
   *          new position of the node amount its peers
   * @throws Exception
   */
  private String processNodeMovement(Map<String, String> parameters, JSONObject data,
      JSONObject oldValues, String prevNodeId, String nextNodeId) throws Exception {
    String nodeId = data.getString("id");
    String newParentId = data.getString("parentId");

    JSONObject jsonResult = new JSONObject();
    JSONObject jsonResponse = new JSONObject();
    JSONArray dataResponse = new JSONArray();

    String tabId = parameters.get("tabId");
    String treeReferenceId = parameters.get("treeReferenceId");
    Tab tab = null;
    TableTree tableTree = null;
    if (tabId != null) {
      tab = OBDal.getInstance().get(Tab.class, tabId);
      tableTree = tab.getTableTree();
      if (!hasAccess(tab.getTable().getEntity(), tab, false)) {
        return noAccessError().toString();
      }
    } else if (treeReferenceId != null) {
      ReferencedTree treeReference = OBDal.getInstance().get(ReferencedTree.class, treeReferenceId);
      tableTree = treeReference.getTableTreeCategory();
    } else {
      log.error(
          "A request to the TreeDatasourceService must include the tabId or the treeReferenceId parameter");
      return null;
    }

    String tableName = tableTree.getTable().getName();
    CheckTreeOperationManager ctom = null;
    try {
      ctom = checkTreeOperationManagers.select(new ComponentProvider.Selector(tableName)).get();
    } catch (UnsatisfiedResolutionException e) {
      // Controlled exception, there aren't any CheckTreeOperationManager
    }
    boolean success = true;
    String messageType = null;
    String message = null;
    if (ctom != null) {
      // Check if the node movement is allowed
      ActionResponse actionResponse = ctom.checkNodeMovement(parameters, nodeId, newParentId,
          prevNodeId, nextNodeId);
      success = actionResponse.isSuccess();
      messageType = actionResponse.getMessageType();
      message = actionResponse.getMessage();
    }

    // Move it
    if (success) {
      JSONObject updatedData = moveNode(parameters, nodeId, newParentId, prevNodeId, nextNodeId);
      if (updatedData != null) {
        dataResponse.put(updatedData);
      } else {
        dataResponse.put(data);
      }
      jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
    } else {
      // These node movement is not valid, it has to be reverted in the client
      oldValues.put("revertMovement", true);
      dataResponse.put(oldValues);
      jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_FAILURE);
    }

    if (messageType != null && message != null) {
      JSONObject jsonMessage = new JSONObject();
      jsonMessage.put("messageType", messageType);
      jsonMessage.put("message", message);
      jsonResponse.put("message", jsonMessage);
    }
    jsonResponse.put(JsonConstants.RESPONSE_DATA, dataResponse);
    jsonResponse.put(JsonConstants.RESPONSE_STARTROW, 0);
    jsonResponse.put(JsonConstants.RESPONSE_ENDROW, 0);
    jsonResponse.put(JsonConstants.RESPONSE_TOTALROWS, 1);
    jsonResult.put(JsonConstants.RESPONSE_RESPONSE, jsonResponse);
    return jsonResult.toString();
  }

  protected abstract JSONObject moveNode(Map<String, String> parameters, String nodeId,
      String newParentId, String prevNodeId, String nextNodeId) throws Exception;

  /**
   * Exception thrown when an operation can not be done upon a node that has several parents
   */
  protected class MultipleParentsException extends Exception {
    private static final long serialVersionUID = 1L;

  }

  /**
   * Exception thrown when the number of records returned by the datasource is higher than the
   * defined limit
   */
  protected class TooManyTreeNodesException extends Exception {
    private static final long serialVersionUID = 1L;
  }

  protected Entity getEntity(JSONObject bobProperties) throws JSONException {
    String entityName = bobProperties.getString("_entity");
    return ModelProvider.getInstance().getEntity(entityName);
  }

  private JSONObject noAccessError() {
    JSONObject jsonResult = new JSONObject();
    JSONObject jsonResponse = new JSONObject();
    JSONObject jsonMessage = new JSONObject();
    try {
      jsonMessage.put("messageType", "error");
      jsonMessage.put("message", Utility.messageBD(new DalConnectionProvider(false),
          "AccessTableNoView", OBContext.getOBContext().getLanguage().getLanguage()));
      jsonResponse.put("message", jsonMessage);
      jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_FAILURE);
      jsonResponse.put(JsonConstants.RESPONSE_DATA, new JSONArray());
      jsonResponse.put(JsonConstants.RESPONSE_STARTROW, 0);
      jsonResponse.put(JsonConstants.RESPONSE_ENDROW, 0);
      jsonResponse.put(JsonConstants.RESPONSE_TOTALROWS, 0);
      jsonResult.put(JsonConstants.RESPONSE_RESPONSE, jsonResponse);
    } catch (JSONException e) {
      log.error("Error generating no access message", e);
    }

    return jsonResult;
  }

  /**
   * Adds to a json representation of a node some attributes that are common to all trees regardless
   * of its particular implementation
   * 
   * @param entity
   *          entity of the table associated with the tree
   * @param bob
   *          BaseOBObject representation of the node
   * @param node
   *          JSONObject representation of the node. The common attributes will be added to this
   *          object
   */
  protected final void addNodeCommonAttributes(Entity entity, BaseOBObject bob, JSONObject node) {
    try {
      if (!canAcceptDrop(entity, bob)) {
        node.put("canBeParentNode", false);
        node.put("showDropIcon", false);
        node.put("showOpenIcon", false);
      } else {
        node.put("showDropIcon", true);

      }
    } catch (JSONException e) {
      log.error("Error while adding the node common attributes", e);
    }
  }

  /**
   * 
   * @param entity
   *          entity of the tree being fetched
   * @param bob
   *          node whose disponibility to accept drops is to be determined
   * @return true if the node can accept dropped records, false otherwise
   */
  private boolean canAcceptDrop(Entity entity, BaseOBObject bob) {
    if (AD_ORG_TABLE_ID.equals(entity.getTableId()) && ROOT_ORGANIZATION_ID.equals(bob.getId())) {
      // Special case, * organization has summaryField = false but can accept drop
      return true;
    }
    boolean canAcceptDrop = true;
    if (entity.hasProperty(SUMMARY_LEVEL_PROPERTY)
        && (Boolean) bob.get(SUMMARY_LEVEL_PROPERTY) == false) {
      canAcceptDrop = false;
    }
    // else {} If the entity does not have a summaryLevel property then all its nodes can accept
    // drop
    return canAcceptDrop;
  }

  /**
   * Given a criteria and the name of a property, returns the value of that property in the criteria
   * 
   * @param criteria
   *          the criteria that might contain a value for the provided property
   * @param parentPropertyName
   *          the property whose value might be contained in the criteria
   * @return the value of the property in the criteria, or null if it is not found
   */
  protected final String getParentRecordIdFromCriteria(JSONArray criteria,
      String parentPropertyName) {
    String parentRecordId = null;
    for (int i = 0; i < criteria.length(); i++) {
      try {
        JSONObject criterion = (JSONObject) criteria.get(i);
        if (criterion.has("criteria")) {
          return getParentRecordIdFromCriteria(criterion.getJSONArray("criteria"),
              parentPropertyName);
        }
        if (parentPropertyName.equals(criterion.getString("fieldName"))) {
          parentRecordId = criterion.getString("value");
          break;
        }
      } catch (JSONException e) {
        log.error("Error while obtaining a property from a JSONObject", e);
      }
    }
    return parentRecordId;
  }

}
