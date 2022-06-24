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
 * All portions are Copyright (C) 2013-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
isc.ClassFactory.defineClass('OBTreeViewGrid', isc.OBTreeGrid);

isc.OBTreeViewGrid.addProperties({
  referencedTableId: null,
  parentTabRecordId: null,
  view: null,
  orderedTree: false,

  arrowKeyAction: 'select',
  canPickFields: false,
  canDropOnLeaves: true,
  canHover: false,
  // It will be set to false if the tree is ordered
  canReorderRecords: true,
  canAcceptDroppedRecords: true,
  dropIconSuffix: 'into',
  autoFetchData: false,
  closedIconSuffix: '',
  showFilterEditor: true,
  selectionAppearance: 'checkbox',
  showSelectedStyle: true,

  autoFitFieldWidths: true,
  autoFitWidthApproach: 'title',
  canAutoFitFields: false,
  minFieldWidth: 75,
  width: '100%',
  height: '100%',

  // the grid will be refreshed when:
  // - The tree category is LinkToParent and
  // - There has been at least a reparent
  needsViewGridRefresh: false,
  init: function() {
    this.copyFunctionsFromViewGrid();
    this.Super('init', arguments);
    if (this.orderedTree) {
      this.canSort = false;
    } else {
      this.canSort = true;
    }
    this.confirmNodeReparent = OB.PropertyStore.get(
      'OBUIAPP_ConfirmNodeReparent',
      this.view.windowId
    );
    this.filterNoRecordsEmptyMessage =
      '<span class="' +
      this.emptyMessageStyle +
      '">' +
      OB.I18N.getLabel('OBUIAPP_GridFilterNoResults') +
      '</span>' +
      '<span onclick="window[\'' +
      this.ID +
      '\'].clearFilter();" class="' +
      this.emptyMessageLinkStyle +
      '">' +
      OB.I18N.getLabel('OBUIAPP_GridClearFilter') +
      '</span>';
  },

  // Some OBTreeViewGrid functionality is alreadyd implemented in OBViewGrid
  // Instead of rewriting it, copy it
  // Do not do this for functions that makes call to super() if it needs to use code from OBGrid. It would not use OBGrid as prototype, but ListGrid
  copyFunctionsFromViewGrid: function() {
    this.filterEditorProperties = this.view.viewGrid.filterEditorProperties;
    this.checkShowFilterFunnelIcon = this.view.viewGrid.checkShowFilterFunnelIcon;
    this.isGridFiltered = this.view.viewGrid.isGridFiltered;
    this.isGridFilteredWithCriteria = this.view.viewGrid.isGridFilteredWithCriteria;
    this.isValidFilterField = this.view.viewGrid.isValidFilterField;
    this.convertCriteria = this.view.viewGrid.convertCriteria;
    this.resetEmptyMessage = this.view.viewGrid.resetEmptyMessage;
    this.filterData = this.view.viewGrid.filterData;
    this.loadingDataMessage = this.view.viewGrid.loadingDataMessage;
    this.emptyMessage = this.view.viewGrid.emptyMessage;
    this.noDataEmptyMessage = this.view.viewGrid.noDataEmptyMessage;
    this.clearFilter = this.view.viewGrid.clearFilter;
    this.setSingleRecordFilterMessage = this.view.viewGrid.setSingleRecordFilterMessage;
  },

  // Sets the fields of the datasource and extends the transformRequest and transformResponse functions
  setDataSource: function(ds, fields) {
    var ret,
      me = this;
    ds.transformRequest = function(dsRequest) {
      dsRequest.params = dsRequest.params || {};
      dsRequest.params._startRow = 0;
      dsRequest.params._endRow = OB.Properties.TreeDatasourceFetchLimit;
      dsRequest.params.referencedTableId = me.referencedTableId;
      me.parentTabRecordId = me.getParentTabRecordId();
      dsRequest.params.parentRecordId = me.parentTabRecordId;
      dsRequest.params.tabId = me.view.tabId;
      dsRequest.params._noActiveFilter = true;
      dsRequest.params._extraProperties =
        me.view.dataSource &&
        me.view.dataSource.requestProperties.params._extraProperties;
      if (dsRequest.dropIndex || dsRequest.dropIndex === 0) {
        //Only send the index if the tree is ordered
        dsRequest = me.addOrderedTreeParameters(dsRequest);
      }
      // Includes the context, it could be used in the hqlwhereclause
      isc.addProperties(dsRequest.params, me.view.getContextInfo(true, false));
      dsRequest.willHandleError = true;
      return this.Super('transformRequest', arguments);
    };

    ds.transformResponse = function(dsResponse, dsRequest, jsonData) {
      var i, node;
      if (jsonData.response.message) {
        me.view.messageBar.setMessage(
          jsonData.response.message.messageType,
          null,
          jsonData.response.message.message
        );
      } else if (dsRequest.operationType === 'update') {
        me.view.messageBar.hide();
      }
      if (jsonData.response.error) {
        dsResponse.error = jsonData.response.error;
      }
      if (jsonData.response && jsonData.response.data && me.showNodeIcons) {
        for (i = 0; i < jsonData.response.data.length; i++) {
          node = jsonData.response.data[i];
          if (node.showDropIcon) {
            node.icon = OB.Styles.OBTreeGrid.iconFolder;
          } else {
            node.icon = OB.Styles.OBTreeGrid.iconNode;
          }
        }
      }
      return this.Super('transformResponse', arguments);
    };

    ds.handleError = function(response, request) {
      var errorMessage;
      if (!response || !response.error) {
        return;
      }
      if (response.error.type === 'tooManyNodes') {
        errorMessage = 'OBUIAPP_TooManyNodes';
      } else if (response.error.type === 'user' && response.error.message) {
        errorMessage = response.error.message;
      }
      me.view.messageBar.setMessage(
        'error',
        null,
        OB.I18N.getLabel(errorMessage)
      );
    };

    ds.updateData = function(updatedRecord, callback, requestProperties) {
      // the new callback checks if the node movement has to be reverted
      var newCallback = function(dsResponse, data, dsRequest) {
        var i, node, parentNode;
        if (dsResponse.error) {
          ds.handleError(dsResponse, dsRequest);
        } else if (
          dsRequest.newParentNode &&
          dsRequest.dragTree &&
          dsRequest.newParentNode.nodeId === dsRequest.dragTree.rootValue
        ) {
          // if the node is being moved to the root, reload the grid to force
          // displaying properly the node in its new position. see issue https://issues.openbravo.com/view.php?id=26898
          dsRequest.dragTree.invalidateCache();
        } else {
          for (i = 0; i < data.length; i++) {
            node = data[i];
            if (node.revertMovement) {
              parentNode = dsRequest.dragTree.find('id', node.parentId);
              if (parentNode) {
                // move the node back to its previous index
                dsRequest.dragTree.move(node, parentNode, node.prevIndex);
              }
            }
          }
        }
      };
      this.Super('updateData', [updatedRecord, newCallback, requestProperties]);
    };
    fields = this.getTreeGridFields(me.fields);
    ds.primaryKeys = {
      id: 'id'
    };
    ret = this.Super('setDataSource', [ds, fields]);
    if (isc.isA.Function(this.view.executeWhenTreeGridDSReady)) {
      this.view.executeWhenTreeGridDSReady();
    }
    return ret;
  },

  // Used to copy the fields from the OBViewGrid to the OBTreeViewGrid.
  // It does not copy the fields that start with underscore
  getTreeGridFields: function(fields) {
    var treeGridFields = isc.shallowClone(fields),
      i,
      nDeleted = 0;
    for (i = 0; i < treeGridFields.length; i++) {
      if (treeGridFields[i - nDeleted].name[0] === '_') {
        treeGridFields.splice(i - nDeleted, 1);
        nDeleted = nDeleted + 1;
      }
    }
    return treeGridFields;
  },

  // Adds to the request the parameters related with the node ordering
  // * prevNodeId: Id of the node placed right before the moved node. Null if there are none
  // * prevNodeId: Id of the node placed right after the moved node. Null if there are none
  addOrderedTreeParameters: function(dsRequest) {
    var childrenOfNewParent, prevNode, nextNode;
    if (this.orderedTree) {
      dsRequest.params.dropIndex = dsRequest.dropIndex;
      childrenOfNewParent = this.getData().getChildren(dsRequest.newParentNode);
      if (childrenOfNewParent.length !== 0) {
        if (dsRequest.dropIndex === 0) {
          nextNode = childrenOfNewParent[dsRequest.dropIndex];
          dsRequest.params.nextNodeId = nextNode.id;
        } else if (dsRequest.dropIndex === childrenOfNewParent.length) {
          prevNode = childrenOfNewParent[dsRequest.dropIndex - 1];
          dsRequest.params.prevNodeId = prevNode.id;
        } else {
          prevNode = childrenOfNewParent[dsRequest.dropIndex - 1];
          dsRequest.params.prevNodeId = prevNode.id;
          nextNode = childrenOfNewParent[dsRequest.dropIndex];
          dsRequest.params.nextNodeId = nextNode.id;
        }
      }
    }
    return dsRequest;
  },

  // Returns the id of the parent tab, if any
  getParentTabRecordId: function() {
    if (
      !this.view.parentView ||
      !this.view.parentView.viewGrid.getSelectedRecord()
    ) {
      return null;
    }
    return this.view.parentView.viewGrid.getSelectedRecord().id;
  },

  // Returns a string that represents a jsonarray containing the names of all the TreeGrid fields
  getSelectedPropertiesString: function() {
    var selectedProperties = '[',
      first = true,
      len = this.fields.length,
      i;
    for (i = 0; i < len; i++) {
      if (first) {
        first = false;
        selectedProperties =
          selectedProperties + "'" + this.fields[i].name + "'";
      } else {
        selectedProperties =
          selectedProperties + ',' + "'" + this.fields[i].name + "'";
      }
    }
    selectedProperties = selectedProperties + ']';
    return selectedProperties;
  },

  transferNodes: function(nodes, folder, index, sourceWidget, callback) {
    var me = this,
      i,
      len = nodes.length,
      nodesIdentifier = '',
      parentIdentifier,
      message;
    if (folder.canBeParentNode === false) {
      return;
    }
    if (this.canReorderRecords) {
      if (this.confirmNodeReparent && this.canReorderRecords) {
        for (i = 0; i < len; i++) {
          nodesIdentifier = nodesIdentifier + nodes[i][OB.Constants.IDENTIFIER];
          if (i + 1 < len) {
            nodesIdentifier = nodesIdentifier + ', ';
          }
        }
        if (folder.nodeId === this.dataProperties.rootValue) {
          parentIdentifier = OB.I18N.getLabel('OBUIAPP_RootNode');
        } else {
          parentIdentifier = folder[OB.Constants.IDENTIFIER];
        }

        message = OB.I18N.getLabel('OBUIAPP_MoveTreeNode', [
          nodesIdentifier,
          parentIdentifier
        ]);
        isc.confirm(message, function(value) {
          if (value) {
            me.doTransferNodes(nodes, folder, index, sourceWidget, callback);
          }
        });
      } else {
        this.doTransferNodes(nodes, folder, index, sourceWidget, callback);
      }
    }
  },

  // smartclients transferNodes does not update the tree it a node is moved within its same parent
  // do it here
  doTransferNodes: function(nodes, folder, index, sourceWidget, callback) {
    var node, dataSource, oldValues, dragTree, dataSourceProperties, i;
    if (this.movedToSameParent(nodes, folder)) {
      dragTree = sourceWidget.getData();
      dataSource = this.getDataSource();
      for (i = 0; i < nodes.length; i++) {
        node = nodes[i];
        // stores the node original index just in case the movement is not valid and the node has to be moved back to its original position
        node.prevIndex = this.getData()
          .getChildren(this.getData().getParent(node))
          .indexOf(node);
        oldValues = isc.addProperties({}, node);
        dataSourceProperties = {
          oldValues: oldValues,
          parentNode: this.data.getParent(node),
          newParentNode: folder,
          dragTree: dragTree,
          draggedNode: node,
          draggedNodeList: nodes,
          dropIndex: index
        };
        if (index > 0) {
          dataSourceProperties.dropNeighbor = this.data.getChildren(folder)[
            index - 1
          ];
        }
        this.updateDataViaDataSource(
          node,
          dataSource,
          dataSourceProperties,
          sourceWidget
        );
      }
    } else {
      if (this.treeStructure === 'LinkToParent') {
        this.needsViewGridRefresh = true;
      }
    }

    this.Super('transferNodes', arguments);
  },

  // Checks if any node has been moved to another position of its current parent node
  movedToSameParent: function(nodes, newParent) {
    var i,
      len = nodes.length;
    for (i = 0; i < len; i++) {
      if (nodes[i].parentId === this.dataProperties.rootValue) {
        if (nodes[i].parentId !== newParent.nodeId) {
          return false;
        }
      } else {
        if (nodes[i].parentId !== newParent.id) {
          return false;
        }
      }
    }
    return true;
  },

  // Returns a node from its id (the id property of the record, not the nodeId property)
  // If no node exists with that id, it return null
  getNodeByID: function(nodeId) {
    var i,
      node,
      nodeList = this.data.getNodeList();
    for (i = 0; i < nodeList.length; i++) {
      node = nodeList[i];
      if (node.id === nodeId) {
        return node;
      }
    }
    return null;
  },

  setView: function(view) {
    this.view = view;
  },

  // Opens the record in the edit form
  // TODO: Check if the record is readonly?
  recordDoubleClick: function(
    viewer,
    record,
    recordNum,
    field,
    fieldNum,
    value,
    rawValue
  ) {
    this.view.editRecordFromTreeGrid(record, false, field ? field.name : null);
  },

  show: function() {
    this.view.toolBar.updateButtonState();
    this.Super('show', arguments);
    this.checkShowFilterFunnelIcon(this.getCriteria());
  },

  // When hiding the tree grid to show the view grid, only refresh it if needed
  hide: function() {
    this.copyCriteriaToViewGrid();
    if (this.needsViewGridRefresh) {
      this.needsViewGridRefresh = false;
      this.view.viewGrid.refreshGrid();
    }
    this.Super('hide', arguments);
  },

  // Takes the criteria from the view grid and applies it to the tree grid
  copyCriteriaFromViewGrid: function() {
    var viewGridCriteria = this.view.viewGrid.getCriteria();
    this.setCriteria(viewGridCriteria);
  },

  // Takes the criteria from the tree grid and applies it to the view grid
  copyCriteriaToViewGrid: function() {
    var treeGridCriteria = this.getCriteria();
    this.view.viewGrid.setCriteria(treeGridCriteria);
  },

  rowMouseDown: function(record, rowNum, colNum) {
    this.Super('rowMouseDown', arguments);
    if (!isc.EventHandler.ctrlKeyDown()) {
      this.deselectAllRecords();
    }
    this.selectRecord(rowNum);
  },

  recordClick: function(
    viewer,
    record,
    recordNum,
    field,
    fieldNum,
    value,
    rawValue
  ) {
    if (isc.EH.getEventType() === 'mouseUp') {
      // Don't do anything on the mouseUp event, the record is actually selected in the mouseDown event
      return;
    }
    this.deselectAllRecords();
    this.selectRecord(recordNum);
  },

  selectionUpdated: function(record, recordList) {
    var me = this,
      callback = function() {
        me.delayedSelectionUpdated();
      };
    // wait 2 times longer than the fire on pause delay default
    this.fireOnPause(
      'delayedSelectionUpdated_' + this.ID,
      callback,
      this.fireOnPauseDelay * 2
    );
  },

  delayedSelectionUpdated: function(record, recordList) {
    var length, tabViewPane, i;
    this.view.updateSubtabVisibility();
    this.view.toolBar.updateButtonState();
    // refresh the tabs
    if (this.view.childTabSet) {
      length = this.view.childTabSet.tabs.length;
      for (i = 0; i < length; i++) {
        tabViewPane = this.view.childTabSet.tabs[i].pane;
        tabViewPane.doRefreshContents(true);
      }
    }
  },

  getFetchRequestParams: function(params) {
    params = this.view.viewGrid.getFetchRequestParams(params);
    params._tabId = this.view.tabId;
    return params;
  },

  // show or hide the filter button
  filterEditorSubmit: function(criteria) {
    this.checkShowFilterFunnelIcon(criteria);
  },

  // If any filter change, the view grid will have to te refreshed when the tree grid is hidden
  editorChanged: function(item) {
    this.needsViewGridRefresh = true;
    this.Super('editorChanged', arguments);
  },

  getCriteria: function() {
    var criteria = this.Super('getCriteria', arguments) || {};
    if ((criteria === null || !criteria.criteria) && this.initialCriteria) {
      criteria = isc.shallowClone(this.initialCriteria);
    }
    criteria = this.convertCriteria(criteria);
    return criteria;
  },

  refreshGrid: function(callback) {
    this.actionAfterDataArrived = callback;
    this.fetchData(this.getCriteria());
  },

  dataArrived: function(startRow, endRow) {
    // reset noDataEmptyMessage to prevent showing "loading..." indefinitely if the datasource does not return any data
    this.noDataEmptyMessage =
      '<span class="' +
      this.emptyMessageStyle +
      '">' +
      OB.I18N.getLabel('OBUIAPP_NoDataInGrid') +
      '</span>';
    this.resetEmptyMessage();
    if (this.actionAfterDataArrived) {
      this.actionAfterDataArrived();
      this.actionAfterDataArrived = null;
    }
  },

  // refreshes record after edition
  refreshRecord: function(values) {
    var record, p, childrenProperty;
    record = this.getNodeByID(values.id);
    if (record) {
      childrenProperty = this.data ? this.data.childrenProperty : 'children';
      for (p in values) {
        if (
          Object.prototype.hasOwnProperty.call(values, p) &&
          p !== childrenProperty
        ) {
          record[p] = values[p];
        }
      }
      this.markForRedraw();
    } else {
      // record not found, can be new, force refresh
      this.setData([]);
      this.fetchData(this.getCriteria());
    }
  },

  isWritable: function(record) {
    return !record._readOnly;
  }
});
