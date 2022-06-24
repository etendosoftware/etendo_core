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
 * All portions are Copyright (C) 2013-2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.datasource;

import java.util.Map;

/**
 * Abstract class used to restrict the movements of nodes for a particular tree
 * 
 * To check if a node movement is valid for a particular tree, this class has to be subclassed and
 * the checkNodeMovement method must be implemented to determine whether a node movement is valid.
 * Also this annotations must be added for the class to be used using dependency injection:
 * 
 * <code>&#64;ApplicationScoped</code>
 * <p>
 * <code>&#64;Qualifier("entityName"), entityName being the name of the DAL entity associated with the tree
 * </code>
 * 
 */
public abstract class CheckTreeOperationManager {

  /**
   * Checks if a node movement is valid
   * 
   * @param parameters
   *          Map of the parameters sent to the client to the datasource
   * @param nodeId
   *          id of the node being moved
   * @param newParentId
   *          id of the new parent of the node (ROOT_NODE_CLIENT if the node is being moved to the
   *          root)
   * @param prevNodeId
   *          id of the sibling node that would be placed just before the node being moved. Can be
   *          null if the node is being placed in the first position of its siblings. Irrelevant if
   *          the tree is not ordered
   * @param nextNodeId
   *          id of the sibling node that would be placed just after the node being moved. Can be
   *          null if the node is being placed in the last position of its siblings. Irrelevant if
   *          the tree is not ordered
   * @return an ActionResponse object. If the movement is valid, the success attribute must be true,
   *         false otherwise. The message and messageType attributes can be used to show a message
   *         in the client message bar
   */
  public abstract ActionResponse checkNodeMovement(Map<String, String> parameters, String nodeId,
      String newParentId, String prevNodeId, String nextNodeId);

  protected class ActionResponse {
    private boolean success;
    private String messageType;
    private String message;

    public ActionResponse(boolean success) {
      this.success = success;
    }

    public ActionResponse(boolean success, String messageType, String message) {
      this.success = success;
      this.messageType = messageType;
      this.message = message;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getMessageType() {
      return messageType;
    }

    public void setMessageType(String messageType) {
      this.messageType = messageType;
    }

    public boolean isSuccess() {
      return success;
    }

    public void setSuccess(boolean success) {
      this.success = success;
    }
  }
}
