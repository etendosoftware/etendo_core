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
 * All portions are Copyright (C) 2018-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.common.actionhandler.createlinesfromprocess;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.service.db.DbUtility;

/**
 * Abstract class to be implemented by any process that creates invoice lines from any Openbravo
 * BaseOBObject
 * 
 * @param <T>
 *          Invoice lines will be created from an object whose class extends from the BaseOBObject
 */
abstract class CreateInvoiceLinesFromHandler<T extends BaseOBObject>
    extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();

  protected abstract Class<T> getFromClass();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    try {
      JSONObject jsonRequest = new JSONObject(content);
      JSONArray selectedLines = CreateLinesFromUtil.getSelectedLines(jsonRequest);
      Invoice currentInvoice = CreateLinesFromUtil.getCurrentInvoice(jsonRequest);

      throwExceptionIfNoLineSelected(selectedLines);

      // CreateLinesFromProcess is instantiated using Weld so it can use Dependency Injection
      CreateInvoiceLinesFromProcess createLinesFromProcess = WeldUtils
          .getInstanceFromStaticBeanManager(CreateInvoiceLinesFromProcess.class);
      createLinesFromProcess.createInvoiceLinesFromDocumentLines(selectedLines, currentInvoice,
          getFromClass());

      jsonRequest.put(CreateLinesFromUtil.MESSAGE, CreateLinesFromUtil.getSuccessMessage());
      return jsonRequest;
    } catch (Exception e) {
      log.error("Error in CreateInvoiceLinesFrom Action Handler", e);
      return showExceptionInViewAndRetry(e);
    }
  }

  private void throwExceptionIfNoLineSelected(JSONArray selectedLines) {
    if (selectedLines.length() <= 0) {
      throw new OBException(OBMessageUtils.messageBD("NotSelected"));
    }
  }

  private JSONObject showExceptionInViewAndRetry(Exception e) {
    final Throwable ex = DbUtility
        .getUnderlyingSQLException(e.getCause() != null ? e.getCause() : e);
    final OBError msg = OBMessageUtils.translateError(ex.getMessage());
    return getResponseBuilder()
        .showMsgInProcessView(MessageType.ERROR, msg.getTitle(), msg.getMessage(), true)
        .retryExecution()
        .build();
  }
}
