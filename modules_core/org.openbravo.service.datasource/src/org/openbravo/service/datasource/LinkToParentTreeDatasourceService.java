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

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.ApplicationUtils;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.ReferencedTree;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.TableTree;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;
import org.openbravo.service.json.JsonUtils;

public class LinkToParentTreeDatasourceService extends TreeDatasourceService {
  private static final Logger logger = LogManager.getLogger();
  private static final String ID_SEPARATOR = "-";

  @Override
  protected void addNewNode(JSONObject bobProperties) {
    // Nothing needs to be done
  }

  @Override
  protected void deleteNode(JSONObject bobProperties) {
    // When a node is deleted, reparents its children so that the referencial integrity is not
    // broken
    try {
      String entityName = bobProperties.getString("_entity");
      Entity entity = ModelProvider.getInstance().getEntity(entityName);
      Table table = OBDal.getInstance().get(Table.class, entity.getTableId());
      Property linkToParentProperty = getLinkToParentProperty(table);
      Property nodeIdProperty = getNodeIdProperty(table);
      String bobParentNode = null;
      String bobNodeId = null;
      if (bobProperties.has(linkToParentProperty.getName())) {
        bobParentNode = bobProperties.getString(linkToParentProperty.getName());
      }
      if (bobProperties.has(nodeIdProperty.getName())) {
        bobNodeId = bobProperties.getString(nodeIdProperty.getName());
      }
      int nChildrenMoved = reparentChildrenOfDeletedNode(entity, bobParentNode, bobNodeId);
      logger.info(nChildrenMoved + " children have been moved to another parent");
    } catch (Exception e) {
      logger.error("Error while deleting tree node: ", e);
      throw new OBException("The node could not be deleted");
    }
  }

  /**
   * Does the actual reparent
   * 
   * @param entity
   * @param newParentId
   *          new parent id to be used on the moved nodes
   * @param oldParentId
   *          parent id of the nodes to be moved
   * @return the number of reparented nodes
   */
  public int reparentChildrenOfDeletedNode(Entity entity, String newParentId, String oldParentId) {
    int nChildrenMoved = -1;
    Table table = OBDal.getInstance().get(Table.class, entity.getTableId());
    Property linkToParentProperty = getLinkToParentProperty(table);
    Column linkToParentColumn = OBDal.getInstance()
        .get(Column.class, linkToParentProperty.getColumnId());
    try {
      ConnectionProvider conn = new DalConnectionProvider(false);
      nChildrenMoved = TreeDatasourceServiceData.reparentChildrenLinkToParent(conn,
          table.getDBTableName(), linkToParentColumn.getDBColumnName(), newParentId, oldParentId);
    } catch (ServletException e) {
      logger.error("Error while deleting tree node: ", e);
    }
    return nChildrenMoved;
  }

  /**
   * Given a tableTree, returns the property that points to the parent node
   * 
   * @param tableTree
   * @return the property that points to the parent node
   */
  private Property getLinkToParentProperty(TableTree tableTree) {
    Column linkToParentColumn = tableTree.getLinkToParentColumn();
    Entity entity = ModelProvider.getInstance().getEntityByTableId(tableTree.getTable().getId());
    return entity.getPropertyByColumnName(linkToParentColumn.getDBColumnName());
  }

  /**
   * Given a table, returns the property that points to the parent node. It is used when the
   * TableTree is not available. Uses the first TableTree defined for that table
   * 
   * @param table
   * @return the property that points to the parent node
   */
  private Property getLinkToParentProperty(Table table) {
    List<TableTree> tableTreeList = table.getADTableTreeList();
    if (tableTreeList.size() != 1) {
      return null;
    }
    TableTree tableTree = tableTreeList.get(0);
    Column linkToParentColumn = tableTree.getLinkToParentColumn();
    Entity entity = ModelProvider.getInstance().getEntityByTableId(table.getId());
    return entity.getPropertyByColumnName(linkToParentColumn.getDBColumnName());
  }

  /**
   * Given a tableTree, returns the property that represents the node id
   * 
   * @param tableTree
   * @return the property that represents the node id
   */
  private Property getNodeIdProperty(TableTree tableTree) {
    Column nodeIdColumn = tableTree.getNodeIdColumn();
    Entity entity = ModelProvider.getInstance().getEntityByTableId(tableTree.getTable().getId());
    return entity.getPropertyByColumnName(nodeIdColumn.getDBColumnName());
  }

  /**
   * Given a table, returns the property that represents the node id. It is used when the TableTree
   * is not available. Uses the first TableTree defined for the provided table
   * 
   * @param table
   * @return the property that represents the node id
   */
  private Property getNodeIdProperty(Table table) {
    List<TableTree> tableTreeList = table.getADTableTreeList();
    if (tableTreeList.size() != 1) {
      return null;
    }
    TableTree tableTree = tableTreeList.get(0);
    Column nodeIdColumn = tableTree.getNodeIdColumn();
    Entity entity = ModelProvider.getInstance().getEntityByTableId(table.getId());
    return entity.getPropertyByColumnName(nodeIdColumn.getDBColumnName());
  }

  /**
   * 
   * @param parameters
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
  @Override
  protected JSONArray fetchNodeChildren(Map<String, String> parameters,
      Map<String, Object> datasourceParameters, String parentId, String hqlWhereClause,
      String hqlWhereClauseRootNodes) throws JSONException, TooManyTreeNodesException {

    boolean fetchRoot = ROOT_NODE_CLIENT.equals(parentId);
    String tabId = parameters.get("tabId");
    String treeReferenceId = parameters.get("treeReferenceId");
    Tab tab = null;
    Table table = null;
    TableTree tableTree = null;
    if (tabId != null) {
      tab = OBDal.getInstance().get(Tab.class, tabId);
      table = tab.getTable();
      tableTree = tab.getTableTree();
    } else if (treeReferenceId != null) {
      ReferencedTree treeReference = OBDal.getInstance().get(ReferencedTree.class, treeReferenceId);
      table = treeReference.getTable();
      tableTree = treeReference.getTableTreeCategory();
    } else {
      logger.error(
          "A request to the TreeDatasourceService must include the tabId or the treeReferenceId parameter");
      return new JSONArray();
    }
    Entity entity = ModelProvider.getInstance().getEntityByTableId(table.getId());
    Property linkToParentProperty = getLinkToParentProperty(tableTree);
    Property nodeIdProperty = getNodeIdProperty(tableTree);
    boolean isMultiParentTree = tableTree.isHasMultiparentNodes();
    //@formatter:off
    String whereClause = 
            " as e " +
            " where ";
    //@formatter:on
    final Map<String, Object> queryParameters = new HashMap<>();
    String actualParentId = new String(parentId);
    if (isMultiParentTree) {
      // The ids of multi parent trees are formed by the concatenation of the ids of its parents,
      // beginning with its root
      // node
      if (parentId.contains(ID_SEPARATOR)) {
        actualParentId = parentId.substring(parentId.lastIndexOf(ID_SEPARATOR) + 1);
      }
    }
    // check if we can avoid to apply the where clause when fetching the child nodes
    boolean allowNotApplyingWhereClauseToChildren = !tableTree.isApplyWhereClauseToChildNodes();
    if ((fetchRoot || !allowNotApplyingWhereClauseToChildren) && hqlWhereClause != null) {
      // Include the hql where clause for all root nodes and for child nodes only if it is required
      whereClause += "(" + hqlWhereClause + ") and ";
    }

    if (hqlWhereClauseRootNodes != null && fetchRoot) {
      // If we are fetching the root nodes and there is a defined hqlWhereClauseRootNodes, apply it
      whereClause += " " + hqlWhereClauseRootNodes + " ";
    } else {
      whereClause += " e." + linkToParentProperty.getName();
      if (fetchRoot) {
        whereClause += " is null ";
      } else {
        if (!linkToParentProperty.isPrimitive()) {
          whereClause += ".id";
        }
        whereClause += " = :parentId ";
        queryParameters.put("parentId", actualParentId);
      }
      if (tab != null && tab.getTabLevel() > 0) {
        // only try to add the parent tab criteria when the tab is not the header
        whereClause = addParentTabCriteria(whereClause, tab, parameters, queryParameters);
      }
    }
    final OBQuery<BaseOBObject> query = OBDal.getInstance()
        .createQuery(entity.getName(), whereClause);

    query.setFilterOnActive(false);
    query.setNamedParameters(queryParameters);

    final DataToJsonConverter toJsonConverter = OBProvider.getInstance()
        .get(DataToJsonConverter.class);

    JSONArray responseData = new JSONArray();

    // Check if the number of results to be returned is not higher than the defined limit
    int nResults = query.count();
    OBContext context = OBContext.getOBContext();
    int nMaxResults = -1;
    try {
      nMaxResults = Integer.parseInt(Preferences.getPreferenceValue("TreeDatasourceFetchLimit",
          false, context.getCurrentClient(), context.getCurrentOrganization(), context.getUser(),
          context.getRole(), null));
    } catch (Exception e) {
      nMaxResults = 1000;
    }
    if (nResults > nMaxResults) {
      throw new TooManyTreeNodesException();
    }
    int count = 0;
    final ScrollableResults scrollableResults = query.scroll(ScrollMode.FORWARD_ONLY);
    try {
      while (scrollableResults.next()) {
        BaseOBObject bob = (BaseOBObject) scrollableResults.get()[0];
        final JSONObject json = toJsonConverter.toJsonObject(bob, DataResolvingMode.FULL);
        if (fetchRoot) {
          json.put("parentId", ROOT_NODE_CLIENT);
        } else {
          json.put("parentId", parentId);
        }
        Object nodeId = bob.get(nodeIdProperty.getName());
        String nodeIdStr = null;
        if (nodeId instanceof String) {
          nodeIdStr = (String) nodeId;
        } else if (nodeId instanceof BaseOBObject) {
          nodeIdStr = ((BaseOBObject) nodeId).getId().toString();
        }

        Object parentNodeId = bob.get(linkToParentProperty.getName());
        String parentNodeIdStr = null;
        if (parentNodeId instanceof String) {
          parentNodeIdStr = (String) parentNodeId;
        } else if (parentNodeId instanceof BaseOBObject) {
          parentNodeIdStr = ((BaseOBObject) parentNodeId).getId().toString();
        }

        if (isMultiParentTree) {
          json.put("nodeId", parentNodeIdStr + ID_SEPARATOR + nodeIdStr);
        } else {
          json.put("nodeId", nodeIdStr);
        }
        addNodeCommonAttributes(entity, bob, json);
        json.put("_hasChildren", (this.nodeHasChildren(entity, linkToParentProperty, nodeIdProperty,
            bob, hqlWhereClause)) ? true : false);
        responseData.put(json);
        count++;
        if (count % 100 == 0) {
          OBDal.getInstance().getSession().clear();
        }

      }
    } finally {
      scrollableResults.close();
    }
    return responseData;
  }

  /**
   * Returns the where clause with the criteria to filter the rows that belong with the record
   * selected in the parent tab added
   * 
   * @param whereClause
   *          current hql where clase
   * @param tab
   *          the tab associated with the tree
   * @param parameters
   *          the parameters, including the criteria
   * @param queryParameters
   *          the parameters of the where clause, where the id of the record selected in the parent
   *          tab will be included
   * @return whereClause with criteria to filter added
   */
  private String addParentTabCriteria(String whereClause, Tab tab, Map<String, String> parameters,
      Map<String, Object> queryParameters) {
    String finalWhereClause = whereClause;
    Tab parentTab = KernelUtils.getInstance().getParentTab(tab);
    if (parentTab != null) {
      String parentPropertyName = ApplicationUtils.getParentProperty(tab, parentTab);
      if (parentPropertyName != null) {
        try {
          JSONArray criteria = (JSONArray) JsonUtils.buildCriteria(parameters).get("criteria");
          String parentRecordId = getParentRecordIdFromCriteria(criteria, parentPropertyName);
          if (parentRecordId != null) {
            finalWhereClause += " and e." + parentPropertyName + ".id = :parentRecordId ";
            queryParameters.put("parentRecordId", parentRecordId);
          }
        } catch (JSONException e) {
          logger.error("Error while obtaining a property from a JSONObject", e);
        }
      }
    }
    return finalWhereClause;
  }

  @Override
  /**
   * Check if a node has children
   * 
   * @param entity
   *          the entity the node belongs to
   * @param nodeId
   *          the id (database record id) of the node to be checked
   * @param hqlWhereClause
   *          the where clause to be applied to the children
   * @return
   */
  protected boolean nodeHasChildren(Entity entity, String nodeId, String hqlWhereClause) {
    BaseOBObject bob = OBDal.getInstance().get(entity.getName(), nodeId);

    Table table = OBDal.getInstance().get(Table.class, entity.getTableId());
    List<TableTree> tableTreeList = table.getADTableTreeList();
    if (tableTreeList.size() == 0) {
      throw new OBException("Error while determining if the node has children");
    }
    TableTree tableTree = tableTreeList.get(0);
    Property linkToParentProperty = getLinkToParentProperty(tableTree);
    Property nodeIdProperty = getNodeIdProperty(tableTree);

    boolean nodeHasChilden = nodeHasChildren(entity, linkToParentProperty, nodeIdProperty, bob,
        hqlWhereClause);

    return nodeHasChilden;
  }

  /**
   * Check if a node has children that should be shown in the target treegrid
   * 
   * @param entity
   * @param linkToParentProperty
   *          property that points to the parent node
   * @param nodeIdProperty
   *          property that represents the node id
   * @param node
   *          bob with the node properties
   * @param hqlWhereClause
   *          where clause to be applied to the children
   */
  private boolean nodeHasChildren(Entity entity, Property linkToParentProperty,
      Property nodeIdProperty, BaseOBObject node, String hqlWhereClause) {

    Object nodeId = node.get(nodeIdProperty.getName());
    String nodeIdStr = null;
    if (nodeId instanceof String) {
      nodeIdStr = (String) nodeId;
    } else if (nodeId instanceof BaseOBObject) {
      nodeIdStr = ((BaseOBObject) nodeId).getId().toString();
    }
    //@formatter:off
    String whereClause = 
            " as e " +
            " where e." + linkToParentProperty.getName();
    //@formatter:on
    if (!linkToParentProperty.isPrimitive()) {
      whereClause += ".id";
    }
    whereClause += " = :nodeId ";
    if (hqlWhereClause != null) {
      whereClause += " and " + hqlWhereClause;
    }
    final OBQuery<BaseOBObject> query = OBDal.getInstance()
        .createQuery(entity.getName(), whereClause);
    query.setFilterOnActive(false);

    final Map<String, Object> parameters = new HashMap<>(1);
    parameters.put("nodeId", nodeIdStr);
    query.setNamedParameters(parameters);

    return query.count() > 0;
  }

  /**
   * Updates the parent of a given node a returns its definition in a JSONObject
   */
  @Override
  protected JSONObject moveNode(Map<String, String> parameters, String nodeId, String newParentId,
      String prevNodeId, String nextNodeId) throws Exception {

    String referencedTableId = parameters.get("referencedTableId");
    String tabId = parameters.get("tabId");

    Table table = OBDal.getInstance().get(Table.class, referencedTableId);
    Entity referencedEntity = ModelProvider.getInstance().getEntityByTableId(table.getId());
    Tab tab = OBDal.getInstance().get(Tab.class, tabId);

    String hqlWhereClause = tab.getHqlwhereclause();
    if (hqlWhereClause != null) {
      hqlWhereClause = this.substituteParameters(hqlWhereClause, parameters);
    }

    TableTree tableTree = tab.getTableTree();
    Property linkToParentProperty = getLinkToParentProperty(tableTree);
    Entity entity = ModelProvider.getInstance().getEntityByTableId(table.getId());
    Property nodeIdProperty = getNodeIdProperty(tableTree);

    BaseOBObject bob = OBDal.getInstance().get(referencedEntity.getName(), nodeId);
    BaseOBObject parentBob = null;
    if (!ROOT_NODE_CLIENT.equals(newParentId)) {
      parentBob = OBDal.getInstance().get(referencedEntity.getName(), newParentId);
    }
    bob.set(linkToParentProperty.getName(), parentBob);
    OBDal.getInstance().flush();
    final DataToJsonConverter toJsonConverter = OBProvider.getInstance()
        .get(DataToJsonConverter.class);
    JSONObject updatedData = toJsonConverter.toJsonObject(bob, DataResolvingMode.FULL);
    addNodeCommonAttributes(entity, bob, updatedData);
    updatedData.put("parentId", newParentId);
    updatedData.put("_hasChildren",
        (this.nodeHasChildren(entity, linkToParentProperty, nodeIdProperty, bob, hqlWhereClause))
            ? true
            : false);

    return updatedData;
  }

  /**
   * @param parameters
   * @param nodeId
   * @return returns a json object with the definition of a node give its node id
   */
  @Override
  protected JSONObject getJSONObjectByNodeId(Map<String, String> parameters,
      Map<String, Object> datasourceParameters, String nodeId) throws MultipleParentsException {
    String tabId = parameters.get("tabId");
    String treeReferenceId = parameters.get("treeReferenceId");
    Tab tab = null;
    Table table = null;
    TableTree tableTree = null;
    if (tabId != null) {
      tab = OBDal.getInstance().get(Tab.class, tabId);
      table = tab.getTable();
      tableTree = tab.getTableTree();
    } else if (treeReferenceId != null) {
      ReferencedTree treeReference = OBDal.getInstance().get(ReferencedTree.class, treeReferenceId);
      table = treeReference.getTable();
      tableTree = treeReference.getTableTreeCategory();
    } else {
      logger.error(
          "A request to the TreeDatasourceService must include the tabId or the treeReferenceId parameter");
      return new JSONObject();
    }
    // Obtain the recordId based on the nodeId
    Entity entity = ModelProvider.getInstance().getEntityByTableId(table.getId());
    Property nodeIdProperty = getNodeIdProperty(tableTree);

    //@formatter:off
    String whereClause = " where " + nodeIdProperty.getName();
    //@formatter:on
    if (!nodeIdProperty.isPrimitive()) {
      whereClause += ".id";
    }
    whereClause += " = :nodeId ";
    final OBQuery<BaseOBObject> query = OBDal.getInstance()
        .createQuery(entity.getName(), whereClause);

    final Map<String, Object> queryParameters = new HashMap<>(1);
    queryParameters.put("nodeId", nodeId);
    query.setNamedParameters(queryParameters);

    if (query.count() != 1) {
      // If the node has several parents, it is not possible to know which node should be returned
      throw new MultipleParentsException();
    }
    BaseOBObject bob = query.uniqueResult();
    return this.getJSONObjectByRecordId(parameters, datasourceParameters, bob.getId().toString());
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
  @Override
  protected boolean nodeConformsToWhereClause(TableTree tableTree, String nodeId,
      String hqlWhereClause) {
    if (hqlWhereClause == null || hqlWhereClause.isEmpty()) {
      return true;
    }
    Table table = tableTree.getTable();
    Entity entity = ModelProvider.getInstance().getEntityByTableId(table.getId());
    Property nodeIdProperty = getNodeIdProperty(tableTree);

    //@formatter:off
    String whereClause = 
            " as e " +
            " where e." + nodeIdProperty.getName();
    //@formatter:on
    if (!nodeIdProperty.isPrimitive()) {
      whereClause += ".id";
    }
    whereClause += " = :nodeId ";
    whereClause += " and (" + hqlWhereClause + ")";
    final OBQuery<BaseOBObject> query = OBDal.getInstance()
        .createQuery(entity.getName(), whereClause);

    final Map<String, Object> queryParameters = new HashMap<>(1);
    queryParameters.put("nodeId", nodeId);
    query.setNamedParameters(queryParameters);

    return (query.count() == 1);
  }

  protected JSONObject getJSONObjectByRecordId(Map<String, String> parameters, String bobId,
      boolean fillNodeIdAndParentId) {
    String tabId = parameters.get("tabId");
    String treeReferenceId = parameters.get("treeReferenceId");
    Tab tab = null;
    Table table = null;
    TableTree tableTree = null;
    String hqlWhereClause = null;
    if (tabId != null) {
      tab = OBDal.getInstance().get(Tab.class, tabId);
      table = tab.getTable();
      tableTree = tab.getTableTree();
      hqlWhereClause = tab.getHqlwhereclause();
    } else if (treeReferenceId != null) {
      ReferencedTree treeReference = OBDal.getInstance().get(ReferencedTree.class, treeReferenceId);
      table = treeReference.getTable();
      tableTree = treeReference.getTableTreeCategory();
      hqlWhereClause = treeReference.getHQLSQLWhereClause();
    } else {
      logger.error(
          "A request to the TreeDatasourceService must include the tabId or the treeReferenceId parameter");
      return new JSONObject();
    }

    if (hqlWhereClause != null) {
      hqlWhereClause = this.substituteParameters(hqlWhereClause, parameters);
    }
    Property linkToParentProperty = getLinkToParentProperty(tableTree);
    Property nodeIdProperty = getNodeIdProperty(tableTree);

    Entity entity = ModelProvider.getInstance().getEntityByTableId(table.getId());
    JSONObject json = null;

    final DataToJsonConverter toJsonConverter = OBProvider.getInstance()
        .get(DataToJsonConverter.class);

    try {
      BaseOBObject bob = OBDal.getInstance().get(entity.getName(), bobId);
      json = toJsonConverter.toJsonObject(bob, DataResolvingMode.FULL);
      if (fillNodeIdAndParentId) {
        String parentId = null;
        if (linkToParentProperty.isPrimitive()) {
          parentId = (String) bob.get(linkToParentProperty.getName());
        } else {
          BaseOBObject parent = (BaseOBObject) bob.get(linkToParentProperty.getName());
          if (parent != null) {
            parentId = parent.getId().toString();
          }
        }
        if (parentId != null) {
          json.put("parentId", parentId);
        } else {
          json.put("parentId", ROOT_NODE_CLIENT);
        }
        Object nodeId = bob.get(nodeIdProperty.getName());
        String nodeIdStr = null;
        if (nodeId instanceof String) {
          nodeIdStr = (String) nodeId;
        } else if (nodeId instanceof BaseOBObject) {
          nodeIdStr = ((BaseOBObject) nodeId).getId().toString();
        }
        json.put("nodeId", nodeIdStr);
      }
      addNodeCommonAttributes(entity, bob, json);
      json.put("_hasChildren",
          (this.nodeHasChildren(entity, linkToParentProperty, nodeIdProperty, bob, hqlWhereClause))
              ? true
              : false);
    } catch (JSONException e) {
      logger.error("Error on tree datasource", e);
    }
    return json;
  }

  /**
   * @return returns a json object with the definition of a node give its record id
   */
  @Override
  protected JSONObject getJSONObjectByRecordId(Map<String, String> parameters,
      Map<String, Object> datasourceParameters, String bobId) {
    boolean fillNodeIdAndParentId = true;
    return getJSONObjectByRecordId(parameters, bobId, fillNodeIdAndParentId);
  }

  @Override
  protected JSONArray fetchFilteredNodesForTreesWithMultiParentNodes(Map<String, String> parameters,
      Map<String, Object> datasourceParameters, TableTree tableTree, List<String> filteredNodes,
      String hqlTreeWhereClause, String hqlTreeWhereClauseRootNodes,
      boolean allowNotApplyingWhereClauseToChildren)
      throws MultipleParentsException, TooManyTreeNodesException {

    Property linkToParentProperty = getLinkToParentProperty(tableTree);
    Property nodeIdProperty = getNodeIdProperty(tableTree);
    JSONArray responseData = new JSONArray();
    Map<String, JSONObject> addedNodesMap = new HashMap<String, JSONObject>();

    try {

      for (String nodeRecordId : filteredNodes) {
        JSONObject node = getJSONObjectByRecordId(parameters, datasourceParameters, nodeRecordId);
        BasicTreeInfo treeInfo = new BasicTreeInfo(nodeRecordId,
            node.getString(nodeIdProperty.getName()),
            node.getString(linkToParentProperty.getName()));
        List<JSONArray> pathsToRootNodes = buildPathToRootNodes(parameters, treeInfo, tableTree,
            hqlTreeWhereClause, hqlTreeWhereClauseRootNodes);
        for (JSONArray pathToRootNode : pathsToRootNodes) {
          for (int i = 0; i < pathToRootNode.length(); i++) {
            JSONObject pathNode = pathToRootNode.getJSONObject(i);
            addedNodesMap.put(pathNode.getString("nodeId"), pathNode);
          }
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
      logger.error("Error while processing the filtered nodes from the datasource", e);
    }

    return responseData;
  }

  private List<JSONArray> buildPathToRootNodes(Map<String, String> parameters,
      BasicTreeInfo recordInfo, TableTree tableTree, String hqlTreeWhereClause,
      String hqlTreeWhereClauseRootNodes) throws TooManyTreeNodesException {
    List<JSONArray> pathsToRootNodes = new ArrayList<JSONArray>();
    try {
      String recordId = recordInfo.getRecordId();
      Property nodeIdProperty = getNodeIdProperty(tableTree);
      if (isInvalidRoot(recordInfo, tableTree, hqlTreeWhereClause, hqlTreeWhereClauseRootNodes)) {
        return pathsToRootNodes;
      }
      if (isValidRoot(recordInfo, tableTree, hqlTreeWhereClause, hqlTreeWhereClauseRootNodes)) {
        boolean fillNodeIdAndParentId = false;
        JSONObject node = getJSONObjectByRecordId(parameters, recordId, fillNodeIdAndParentId);
        String nodeId = node.getString(nodeIdProperty.getName());
        node.put("parentId", ROOT_NODE_CLIENT);
        node.put("nodeId", nodeId);
        node.put("filterHit", true);
        JSONArray pathToRootNode = new JSONArray();
        pathToRootNode.put(node);
        pathsToRootNodes.add(pathToRootNode);
        return pathsToRootNodes;
      }
      boolean fillNodeIdAndParentId = false;
      JSONObject node = getJSONObjectByRecordId(parameters, recordId, fillNodeIdAndParentId);
      String nodeId = node.getString(nodeIdProperty.getName());
      List<BasicTreeInfo> parentInfoList = getRecordInfoOfParents(parameters, recordInfo,
          hqlTreeWhereClause, hqlTreeWhereClauseRootNodes);
      for (BasicTreeInfo parentInfo : parentInfoList) {
        List<JSONArray> pathsToRootNodesAux = buildPathToRootNodes(parameters, parentInfo,
            tableTree, hqlTreeWhereClause, hqlTreeWhereClauseRootNodes);
        for (JSONArray pathToRootNode : pathsToRootNodesAux) {
          if (pathToRootNode != null && pathToRootNode.length() > 0) {
            JSONObject lastNode = pathToRootNode.getJSONObject(pathToRootNode.length() - 1);
            lastNode.put("isOpen", true);
            String lastNodeId = lastNode.getString("nodeId");
            JSONObject auxNode = new JSONObject(node.toString());
            auxNode.put("parentId", lastNodeId);
            auxNode.put("nodeId", lastNodeId + ID_SEPARATOR + nodeId);
            auxNode.put("filterHit", true);
            pathToRootNode.put(auxNode);
            pathsToRootNodes.add(pathToRootNode);
          }
        }
      }
    } catch (JSONException e) {
      logger.error("Error while processing the filtered nodes from the datasource", e);
    }

    return pathsToRootNodes;
  }

  private boolean isValidRoot(BasicTreeInfo recordInfo, TableTree tableTree,
      String hqlTreeWhereClause, String hqlTreeWhereClauseRootNodes) {
    String nodeId = recordInfo.getNodeId();
    String parentId = recordInfo.getParentId();
    if (hqlTreeWhereClauseRootNodes == null) {
      return ROOT_NODE_CLIENT.equals(parentId);
    } else {
      return nodeConformsToWhereClause(tableTree, nodeId, hqlTreeWhereClauseRootNodes);
    }
  }

  private boolean isInvalidRoot(BasicTreeInfo recordInfo, TableTree tableTree,
      String hqlTreeWhereClause, String hqlTreeWhereClauseRootNodes) {
    String nodeId = recordInfo.getNodeId();
    String parentId = recordInfo.getParentId();
    if (hqlTreeWhereClauseRootNodes == null) {
      return false;
    } else if (ROOT_NODE_CLIENT.equals(parentId)
        && !nodeConformsToWhereClause(tableTree, nodeId, hqlTreeWhereClauseRootNodes)) {
      return true;
    } else {
      return false;
    }
  }

  private List<BasicTreeInfo> getRecordInfoOfParents(Map<String, String> parameters,
      BasicTreeInfo treeInfo, String hqlWhereClause, String hqlWhereClauseRootNodes)
      throws TooManyTreeNodesException {
    List<BasicTreeInfo> treeInfoList = new ArrayList<BasicTreeInfo>();
    try {
      List<JSONObject> parentNodes = fetchParentsOfNode(parameters, treeInfo.getParentId(),
          hqlWhereClause, hqlWhereClauseRootNodes);
      for (JSONObject parentNode : parentNodes) {
        treeInfoList.add(new BasicTreeInfo(parentNode.getString("id"),
            parentNode.getString("nodeId"), parentNode.getString("parentId")));
      }
    } catch (JSONException e) {
      logger.error("Error on tree datasource", e);
    }
    return treeInfoList;
  }

  protected List<JSONObject> fetchParentsOfNode(Map<String, String> parameters, String parentId,
      String hqlWhereClause, String hqlWhereClauseRootNodes) throws TooManyTreeNodesException {
    List<JSONObject> parentList = new ArrayList<JSONObject>();
    String tabId = parameters.get("tabId");
    String treeReferenceId = parameters.get("treeReferenceId");
    Tab tab = null;
    Table table = null;
    TableTree tableTree = null;
    if (tabId != null) {
      tab = OBDal.getInstance().get(Tab.class, tabId);
      table = tab.getTable();
      tableTree = tab.getTableTree();
    } else if (treeReferenceId != null) {
      ReferencedTree treeReference = OBDal.getInstance().get(ReferencedTree.class, treeReferenceId);
      table = treeReference.getTable();
      tableTree = treeReference.getTableTreeCategory();
    } else {
      logger.error(
          "A request to the TreeDatasourceService must include the tabId or the treeReferenceId parameter");
      return new ArrayList<JSONObject>();
    }

    Entity entity = ModelProvider.getInstance().getEntityByTableId(table.getId());
    Property linkToParentProperty = getLinkToParentProperty(tableTree);
    Property nodeIdProperty = getNodeIdProperty(tableTree);

    //@formatter:off
    String whereClause = 
            " as e " +
            " where e." + nodeIdProperty.getName();
    //@formatter:on
    if (!nodeIdProperty.isPrimitive()) {
      whereClause += ".id";
    }
    whereClause += " = :parentId ";

    final OBQuery<BaseOBObject> query = OBDal.getInstance()
        .createQuery(entity.getName(), whereClause);

    final Map<String, Object> queryParameters = new HashMap<>();
    queryParameters.put("parentId", parentId);
    query.setNamedParameters(queryParameters);

    final DataToJsonConverter toJsonConverter = OBProvider.getInstance()
        .get(DataToJsonConverter.class);

    // Check if the number of results to be returned is not higher than the defined limit
    int nResults = query.count();
    OBContext context = OBContext.getOBContext();
    int nMaxResults = -1;
    try {
      nMaxResults = Integer.parseInt(Preferences.getPreferenceValue("TreeDatasourceFetchLimit",
          false, context.getCurrentClient(), context.getCurrentOrganization(), context.getUser(),
          context.getRole(), null));
    } catch (Exception e) {
      nMaxResults = 100;
    }
    if (nResults > nMaxResults) {
      throw new TooManyTreeNodesException();
    }
    int count = 0;
    final ScrollableResults scrollableResults = query.scroll(ScrollMode.FORWARD_ONLY);
    try {
      while (scrollableResults.next()) {
        BaseOBObject bob = (BaseOBObject) scrollableResults.get()[0];
        final JSONObject json = toJsonConverter.toJsonObject(bob, DataResolvingMode.FULL);

        Object nodeId = bob.get(nodeIdProperty.getName());
        String nodeIdStr = null;
        if (nodeId instanceof String) {
          nodeIdStr = (String) nodeId;
        } else if (nodeId instanceof BaseOBObject) {
          nodeIdStr = ((BaseOBObject) nodeId).getId().toString();
        }

        Object parentNodeId = bob.get(linkToParentProperty.getName());
        String parentNodeIdStr = null;
        if (parentNodeId instanceof String) {
          parentNodeIdStr = (String) parentNodeId;
        } else if (parentNodeId instanceof BaseOBObject) {
          parentNodeIdStr = ((BaseOBObject) parentNodeId).getId().toString();
        }
        try {
          json.put("nodeId", nodeIdStr);
          if (parentNodeIdStr == null) {
            json.put("parentId", ROOT_NODE_CLIENT);
          } else {
            json.put("parentId", parentNodeIdStr);
          }
        } catch (JSONException e) {
          logger.error("Error on tree datasource", e);
        }

        parentList.add(json);
        count++;
        if (count % 100 == 0) {
          OBDal.getInstance().getSession().clear();
        }

      }
    } finally {
      scrollableResults.close();
    }
    return parentList;
  }

  private class BasicTreeInfo {
    private String recordId;
    private String nodeId;
    private String parentId;

    public BasicTreeInfo(String recordId, String nodeId, String parentId) {
      this.recordId = recordId;
      this.nodeId = nodeId;
      this.parentId = parentId;
    }

    public String getRecordId() {
      return this.recordId;
    }

    public String getNodeId() {
      return this.nodeId;
    }

    public String getParentId() {
      return this.parentId;
    }
  }

  @Override
  protected Map<String, Object> getDatasourceSpecificParams(Map<String, String> parameters) {
    return new HashMap<String, Object>();
  }

}
