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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.advpaymentmngt.actionHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.utility.APRM_MatchingUtility;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonUtils;
import org.openbravo.service.json.OBStaleObjectException;

public class UnMatchTransactionActionHandler extends BaseActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();
    JSONObject errorMessage = new JSONObject();
    try {
      OBContext.setAdminMode(true);
      final JSONObject jsonData = new JSONObject(data);
      final String strBankStatementLineId = jsonData.getString("bankStatementLineId");
      String dateStr = jsonData.getString("updated");
      SimpleDateFormat xmlDateTimeFormat = JsonUtils.createJSTimeFormat();
      Date date = null;
      try {
        date = xmlDateTimeFormat.parse(dateStr);
      } catch (ParseException e) {
      }
      final FIN_BankStatementLine bsline = OBDal.getInstance()
          .get(FIN_BankStatementLine.class, strBankStatementLineId);
      Date bbddBSLUpdated = bsline.getUpdated();
      // Remove milis
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(OBDateUtils.convertDateToUTC(bbddBSLUpdated));
      calendar.setLenient(true);
      calendar.set(Calendar.MILLISECOND, 0);
      if (date.getTime() != calendar.getTimeInMillis()) {
        throw new OBStaleObjectException("@APRM_StaleDate@");
      }
      final FIN_FinaccTransaction transaction = bsline.getFinancialAccountTransaction();
      if (transaction != null) {
        APRM_MatchingUtility.unmatch(bsline);
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Error Unmatching Transaction", e);
      try {
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("title", "Error");
        errorMessage.put("text", message);
        result.put("message", errorMessage);
      } catch (Exception e2) {
        log.error("Message could not be built", e2);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

}
