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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.logmanagement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Action to set the log level to one or more Loggers
 */
public class LogManagementActionHandler extends BaseProcessActionHandler {

  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    try {
      JSONObject request = new JSONObject(content);
      JSONObject params = request.getJSONObject("_params");
      List<String> loggersToUpdate = getLoggerToUpdateList(
          params.getJSONObject("loggers").getJSONArray("_selection"));
      Level newLogLevel = Level.getLevel(params.getString("loglevel"));

      updateLoggerConfiguration(loggersToUpdate, newLogLevel);

      return getResponseBuilder().refreshGridParameter("loggers")
          .showMsgInProcessView(ResponseActionsBuilder.MessageType.SUCCESS,
              OBMessageUtils.messageBD("OBUIAPP_LogLevelChanged")) //
          .retryExecution() //
          .build();

    } catch (JSONException e) {
      log.error("Error in set log level process", e);
      return getResponseBuilder()
          .showMsgInView(ResponseActionsBuilder.MessageType.ERROR,
              OBMessageUtils.messageBD("Error"), OBMessageUtils.messageBD("Error"))
          .build();
    }
  }

  private List<String> getLoggerToUpdateList(JSONArray array) {
    List<String> result = new ArrayList<>();

    for (int i = 0; i < array.length(); i++) {
      try {
        result.add(array.getJSONObject(i).getString("logger"));
      } catch (JSONException e) {
        log.error("Error getting logger name for index {}", i);
      }
    }

    return result;
  }

  private void updateLoggerConfiguration(List<String> loggersToUpdate, Level newLogLevel) {
    final LoggerContext context = LoggerContext.getContext(false);
    final Configuration config = context.getConfiguration();

    for (String loggerName : loggersToUpdate) {
      LoggerConfig loggerConfig = config.getLoggerConfig(loggerName);

      LoggerConfig specificConfig = loggerConfig;
      if (!loggerConfig.getName().equals(loggerName)) {
        specificConfig = new LoggerConfig(loggerName, newLogLevel, true);
        specificConfig.setParent(loggerConfig);
        config.addLogger(loggerName, specificConfig);
      }
      specificConfig.setLevel(newLogLevel);
      log.info("Setting logger {} to level {}", loggerName, newLogLevel.toString());
    }

    context.updateLoggers();
  }
}
