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
 * All portions are Copyright (C) 2017-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.process;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * This class is a helper that can be used to build the standard response actions of a
 * {@link BaseProcessActionHandler} in an easy way.
 */
public class ResponseActionsBuilder {
  private static final Logger log = LogManager.getLogger();

  private JSONArray responseActions;
  private JSONObject retryExecutionMsg;
  private boolean retryExecution;
  private boolean refreshParent;
  private boolean showResultsInProcessView;

  public enum MessageType {
    SUCCESS("success"), ERROR("error"), INFO("info"), WARNING("warning");

    private final String type;

    MessageType(String type) {
      this.type = type;
    }

    private String getType() {
      return this.type;
    }
  }

  public enum Command {
    DEFAULT("DEFAULT"), NEW("NEW");

    private final String commandName;

    Command(String commandName) {
      this.commandName = commandName;
    }

    private String getCommandName() {
      return this.commandName;
    }
  }

  ResponseActionsBuilder() {
    responseActions = new JSONArray();
  }

  /**
   * It shows a message in the current active view.
   * 
   * @param msgType
   *          The message type.
   * @param msgTitle
   *          The title of the message.
   * @param msgText
   *          The text of the message.
   * @return a ResponseActionsBuilder that contains a 'show message in view' response action.
   */
  public ResponseActionsBuilder showMsgInView(MessageType msgType, String msgTitle,
      String msgText) {
    addResponseAction("showMsgInView", buildResponseMessage(msgType, msgTitle, msgText));
    return this;
  }

  /**
   * @see ResponseActionsBuilder#showMsgInProcessView(MessageType, String, String)
   */
  public ResponseActionsBuilder showMsgInProcessView(MessageType msgType, String msgText) {
    showMsgInProcessView(msgType, "", msgText);
    return this;
  }

  /**
   * @see ResponseActionsBuilder#showMsgInProcessView(MessageType, String, String, boolean)
   */
  public ResponseActionsBuilder showMsgInProcessView(MessageType msgType, String msgTitle,
      String msgText) {
    showMsgInProcessView(msgType, msgTitle, msgText, false);
    return this;
  }

  /**
   * It shows a message in the view that invoked the process.
   * 
   * @param msgType
   *          The message type.
   * @param msgTitle
   *          The title of the message.
   * @param msgText
   *          The text of the message.
   * @param force
   *          If it should force the message to be show in the pop-up. Typically it is used in error
   *          cases together with the retryExecution action.
   * @return a ResponseActionsBuilder that contains a 'show message in process view' response
   *         action.
   */
  public ResponseActionsBuilder showMsgInProcessView(MessageType msgType, String msgTitle,
      String msgText, boolean force) {
    try {
      final JSONObject messageInfo = buildResponseMessage(msgType, msgTitle, msgText);
      if (force) {
        messageInfo.put("force", force);
      }
      addResponseAction("showMsgInProcessView", messageInfo);
    } catch (JSONException ignore) {
    }
    return this;
  }

  /**
   * @see ResponseActionsBuilder#openDirectTab(String, String, boolean)
   */
  public ResponseActionsBuilder openDirectTab(String tabId, boolean wait) {
    openDirectTab(tabId, null, wait);
    return this;
  }

  /**
   * @see ResponseActionsBuilder#openDirectTab(String, String, Command, boolean)
   */
  public ResponseActionsBuilder openDirectTab(String tabId, String recordId, boolean wait) {
    openDirectTab(tabId, recordId, Command.DEFAULT, wait);
    return this;
  }

  /**
   * @see ResponseActionsBuilder#openDirectTab(String, String, Command, String, boolean)
   */
  public ResponseActionsBuilder openDirectTab(String tabId, String recordId, Command command,
      boolean wait) {
    openDirectTab(tabId, recordId, command, null, wait);
    return this;
  }

  /**
   * Opens a view using a tab id and record id. The tab can be a child tab. If the record id is not
   * set then the tab is opened in grid mode.
   * 
   * @param tabId
   *          The id of the tab to be opened.
   * @param recordId
   *          The id of the record to be opened.
   * @param command
   *          The command to be used to open the tab ('DEFAULT', 'NEW').
   * @param criteria
   *          A removable filtering criteria which will be automatically added to the open tab
   * @param wait
   *          If true, the next response action will not be started until this one finishes.
   * @return a ResponseActionsBuilder that contains a 'open direct tab' response action.
   */
  public ResponseActionsBuilder openDirectTab(String tabId, String recordId, Command command,
      String criteria, boolean wait) {
    try {
      final JSONObject openDirectTab = new JSONObject();
      openDirectTab.put("tabId", tabId);
      openDirectTab.put("recordId", recordId);
      openDirectTab.put("command", command.getCommandName());
      if (criteria != null) {
        openDirectTab.put("criteria", criteria);
      }
      openDirectTab.put("wait", wait);
      addResponseAction("openDirectTab", openDirectTab);
    } catch (JSONException ignore) {
    }
    return this;
  }

  /**
   * It sets a given value in the selector caller field (if it exists).
   * 
   * @param recordId
   *          The id of the record to be set in the selector.
   * @param recordIdentifier
   *          The identifier of the record to be set in the selector.
   * @return a ResponseActionsBuilder that contains a 'set selector value' response action.
   */
  public ResponseActionsBuilder setSelectorValueFromRecord(String recordId,
      String recordIdentifier) {
    try {
      final JSONObject setSelectorValueFromRecord = new JSONObject();
      final JSONObject record = new JSONObject();
      record.put("value", recordId);
      record.put("map", recordIdentifier);
      setSelectorValueFromRecord.put("record", record);
      addResponseAction("setSelectorValueFromRecord", setSelectorValueFromRecord);
    } catch (JSONException ignore) {
    }
    return this;
  }

  /**
   * It refreshes the grid where the process button is defined. This is commonly used when the
   * process adds or deletes records from that grid.
   * 
   * @return a ResponseActionsBuilder that contains a 'refresh grid' response action.
   */
  public ResponseActionsBuilder refreshGrid() {
    addResponseAction("refreshGrid", new JSONObject());
    return this;
  }

  /**
   * It refreshes a grid parameter defined within a parameter window. This can be useful for those
   * parameter windows which are not closed after the execution of the action handler, for example
   * those process definitions which are directly opened from the application menu.
   * 
   * @param gridName
   *          The name of the grid parameter.
   * @return a ResponseActionsBuilder that contains a 'refresh grid parameter' response action.
   */
  public ResponseActionsBuilder refreshGridParameter(String gridName) {
    try {
      final JSONObject refreshGrid = new JSONObject();
      refreshGrid.put("gridName", gridName);
      addResponseAction("refreshGridParameter", refreshGrid);
    } catch (JSONException ignore) {
    }
    return this;
  }

  /**
   * Allows to re-execute the process again, by enabling the process UI. This is useful to do
   * backend validations as this allows the user to fix data and resubmit again.
   * 
   * @return a ResponseActionsBuilder configured to retry the process execution.
   */
  public ResponseActionsBuilder retryExecution() {
    retryExecution = true;
    return this;
  }

  /**
   * Allows to re-execute the process again, by enabling the process UI. This is useful to do
   * backend validations as this allows the user to fix data and resubmit again.
   *
   * @return a ResponseActionsBuilder configured to retry the process execution.
   */
  public ResponseActionsBuilder refreshParent() {
    refreshParent = true;
    return this;
  }

  /**
   * Allows to re-execute the process again, by enabling the process UI. This is useful to do
   * backend validations as this allows the user to fix data and resubmit again. In addition, a
   * message will be displayed with the severity and the text specified with the parameters of this
   * method.
   * 
   * @param msgType
   *          The message type.
   * @param msgText
   *          The text of the message.
   * @return a ResponseActionsBuilder configured to retry the process execution.
   */
  public ResponseActionsBuilder retryExecution(MessageType msgType, String msgText) {
    try {
      retryExecution = true;
      retryExecutionMsg = new JSONObject();
      retryExecutionMsg.put("severity", msgType.getType());
      retryExecutionMsg.put("text", msgText);
    } catch (JSONException ignore) {
    }
    return this;
  }

  /**
   * Allows to re-execute the process again, by enabling the process UI. This is useful to do
   * backend validations as this allows the user to fix data and resubmit again. In addition, a
   * message will be displayed with the severity, title and text specified with the parameters of
   * this method.
   *
   * @param msgType
   *          The message type.
   * @param msgTitle
   *          The title of the message.
   * @param msgText
   *          The text of the message.
   * @return a ResponseActionsBuilder configured to retry the process execution.
   */
  public ResponseActionsBuilder retryExecution(MessageType msgType, String msgTitle,
      String msgText) {
    try {
      retryExecution = true;
      retryExecutionMsg = new JSONObject();
      retryExecutionMsg.put("severity", msgType.getType());
      retryExecutionMsg.put("title", msgTitle);
      retryExecutionMsg.put("text", msgText);
    } catch (JSONException ignore) {
    }
    return this;
  }

  /**
   * Allows to configure the response to show the result of a process directly in the process window
   * itself.
   * 
   * @return a ResponseActionsBuilder configured to show the result in the process window itself.
   */
  public ResponseActionsBuilder showResultsInProcessView() {
    showResultsInProcessView = true;
    return this;
  }

  /**
   * Generates the JSON with the response actions to be executed once the process has finished.
   * 
   * @return a JSONObject with the response actions.
   */
  public JSONObject build() {
    try {
      final JSONObject result = new JSONObject();
      if (responseActions.length() > 0) {
        result.put("responseActions", responseActions);
      }
      if (retryExecution) {
        result.put("retryExecution", true);
        if (retryExecutionMsg != null) {
          result.put("message", retryExecutionMsg);
        }
      }
      if (showResultsInProcessView) {
        result.put("showResultsInProcessView", true);
      }
      return result;
    } catch (JSONException jsonex) {
      log.error("Error building process response actions", jsonex);
      return new JSONObject();
    }
  }

  public ResponseActionsBuilder addCustomResponseAction(String actionName, JSONObject actionData) {
    addResponseAction(actionName, actionData);
    return this;
  }

  private void addResponseAction(String actionName, JSONObject actionData) {
    try {
      final JSONObject responseAction = new JSONObject();
      responseAction.put(actionName, actionData);
      responseActions.put(responseAction);
    } catch (JSONException jsonex) {
      log.error("Error adding response action {}", actionName, jsonex);
    }
  }

  private JSONObject buildResponseMessage(MessageType msgType, String msgTitle, String msgText) {
    JSONObject message = new JSONObject();
    try {
      message.put("msgType", msgType.getType());
      message.put("msgTitle", msgTitle);
      message.put("msgText", msgText);
    } catch (JSONException ignore) {
    }
    return message;
  }

}
