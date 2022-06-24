package org.openbravo.dal.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class OrgNode implements Serializable {
    private static final long serialVersionUID = 1L;
    String parentNodeId;
    boolean isReady;
    boolean isLegalEntity;
    boolean isBusinessUnit;
    boolean isTransactionsAllowed;
    boolean isPeriodControlAllowed;

    private final List<String> children = new ArrayList<>();

    void addChild(String childId) {
        children.add(childId);
    }

    OrgNode(Object[] nodeDef) {
        parentNodeId = (String) nodeDef[1];
        isReady = Objects.equals('Y', nodeDef[2]);
        isLegalEntity = Objects.equals('Y', nodeDef[3]);
        isBusinessUnit = Objects.equals('Y', nodeDef[4]);
        isTransactionsAllowed = Objects.equals('Y', nodeDef[5]);
        isPeriodControlAllowed = Objects.equals('Y', nodeDef[6]);
    }

    void resolve(String nodeId, Map<String, OrgNode> orgNodes) {
        OrgNode parentNode = parentNodeId != null ? orgNodes.get(parentNodeId) : null;
        if (parentNode != null) {
            parentNode.addChild(nodeId);
        }
    }

    String getParentNodeId() {
        return parentNodeId;
    }

    List<String> getChildren() {
        return children;
    }
}