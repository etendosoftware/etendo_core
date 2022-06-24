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
 * All portions are Copyright (C) 2015-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.buildvalidation;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;

import org.openbravo.base.ExecutionLimits;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * This validation is related to this issue: https://issues.openbravo.com/view.php?id=29222
 * 
 * G/L Journal accounting records could have wrong amounts, if document rate is not the same than
 * current system rate
 */
public class GLJournalAccountingCheck extends BuildValidation {

  final static private int Q1_VERSION = 25704;
  final static private int Q1_1_VERSION = 25735;
  final static private String ALERT_RULE = "Wrong G/L Journal Accounting amounts";
  final static private String ALERT_NAME = ALERT_RULE
      + ". Please reset accounting of G/L Journal %s document";
  final static private String AD_WINDOW = "132";
  final static private String AD_TAB = "160";
  final static private String ERROR_MSG = "Wrong G/L Journal accounting data. Please review alerts (Alert Rule: "
      + ALERT_RULE + ") and reset accounting of wrong entries to fix the data";

  public List<String> execute() {
    ArrayList<String> errors = new ArrayList<String>();
    try {
      ConnectionProvider cp = getConnectionProvider();
      String version = GLJournalAccountingCheckData.getModuleVersion(cp);
      int intVersion = Integer.valueOf(version.substring(version.lastIndexOf('.') + 1));
      if (intVersion >= Q1_VERSION && intVersion <= Q1_1_VERSION) {
        if (!GLJournalAccountingCheckData.hasPreference(cp)) {
          GLJournalAccountingCheckData[] documentList = GLJournalAccountingCheckData
              .getWrongGLJournalAccountingClients(cp);
          for (GLJournalAccountingCheckData document : documentList) {
            createAlert(cp, document.adClientId);
          }
          if (documentList != null && documentList.length > 0) {
            errors.add(ERROR_MSG);
          } else {
            GLJournalAccountingCheckData.createPreference(cp);
          }
        }
      }
    } catch (Exception e) {
      return handleError(e);
    }
    return errors;
  }

  private void createAlert(ConnectionProvider cp, String clientId) throws ServletException {
    if (!GLJournalAccountingCheckData.existsAlertRule(cp, ALERT_RULE, clientId)) {
      GLJournalAccountingCheckData.insertAlertRule(cp, clientId, ALERT_RULE, AD_TAB);
    }
    final String alertRuleId = GLJournalAccountingCheckData
        .getAlertRuleId(cp, ALERT_RULE, clientId);
    for (GLJournalAccountingCheckData document : GLJournalAccountingCheckData
        .getWrongGLJournalAccountingDocuments(cp, clientId)) {
      if (!GLJournalAccountingCheckData.existsAlert(cp, alertRuleId, document.glJournalId)) {
        GLJournalAccountingCheckData.insertAlert(cp, clientId,
            String.format(ALERT_NAME, document.recordinfo), alertRuleId, document.recordinfo,
            document.glJournalId);
      }
    }
  }

  @Override
  protected ExecutionLimits getBuildValidationLimits() {
    return new ExecutionLimits("0", null, new OpenbravoVersion(3, 0, 26688));
  }
}
