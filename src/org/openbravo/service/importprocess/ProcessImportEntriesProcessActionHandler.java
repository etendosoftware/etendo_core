/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.service.importprocess;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Will trigger the import process.
 * 
 * @author mtaal
 */
public class ProcessImportEntriesProcessActionHandler extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    try {
      ImportEntryManager entryManager = WeldUtils
          .getInstanceFromStaticBeanManager(ImportEntryManager.class);

      entryManager.notifyNewImportEntryCreated();

      // do a 5 second wait to let the import entry manager time to start
      try {
        Thread.sleep(5000);
      } catch (Exception ignored) {
      }

      JSONObject result = new JSONObject();
      JSONObject msgTotal = new JSONObject();
      JSONArray actions = new JSONArray();

      // no flush needed as only needed to get some messages
      final DalConnectionProvider dalConnectionProvider = new DalConnectionProvider(false);

      final String importProcessLbl = Utility.messageBD(dalConnectionProvider, "ImportProcess",
          OBContext.getOBContext().getLanguage().getLanguage());

      String importProcessRunningLbl = Utility.messageBD(dalConnectionProvider,
          "ImportProcessRunning", OBContext.getOBContext().getLanguage().getLanguage());
      importProcessRunningLbl = importProcessRunningLbl.replaceAll("%1",
          "" + entryManager.getNumberOfActiveTasks());
      importProcessRunningLbl = importProcessRunningLbl.replaceAll("%2",
          "" + entryManager.getNumberOfQueuedTasks());

      final String importProcessNotRunningLbl = Utility.messageBD(dalConnectionProvider,
          "ImportProcessNotRunning", OBContext.getOBContext().getLanguage().getLanguage());

      msgTotal.put("msgType", "info");
      msgTotal.put("msgTitle", importProcessLbl);
      msgTotal.put("msgText",
          entryManager.isExecutorRunning() ? importProcessRunningLbl : importProcessNotRunningLbl);

      JSONObject msgTotalAction = new JSONObject();
      msgTotalAction.put("showMsgInProcessView", msgTotal);
      actions.put(msgTotalAction);
      result.put("responseActions", actions);

      return result;
    } catch (JSONException e) {
      log.error("Error in process", e);
      return new JSONObject();
    }
  }

}
