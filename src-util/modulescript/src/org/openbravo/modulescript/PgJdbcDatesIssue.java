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
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.modulescript;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;

/**
 * Creates an alert for instances that might be affected by issue 31959
 * 
 * @author alostale
 *
 */
public class PgJdbcDatesIssue extends ModuleScript {

  private static final Logger log = LogManager.getLogger();
  private static final String CORE = "0";
  private static final OpenbravoVersion PR15Q4 = new OpenbravoVersion("3.0.27657");
  private static final OpenbravoVersion FIXED_VERSION = new OpenbravoVersion("3.0.28174");
  private static final String ALERT_NAME = "Updating from 15PRQ4 potential problems with dates";
  private static final String ALERT_TXT = "Some dates can be corrupted. Check http://wiki.openbravo.com/wiki/Release_Notes/Issue31959";

  @Override
  public void execute() {
    if (!shouldExecute()) {
      return;
    }

    try {
      createAlert();
    } catch (Exception e) {
      log.error("Error processing alert", e);
    }
  }

  private boolean shouldExecute() {
    ConnectionProvider cp = getConnectionProvider();
    if (!"POSTGRE".equals(cp.getRDBMS())) {
      return false;
    }

    Properties properties = new Properties();
    try {
      properties.load(new FileInputStream(getPropertiesFile()));
    } catch (Exception e) {
      log.error("Could not load Openbravo.properties");
      return false;
    }

    boolean usingCommonsDBCPPool = StringUtils.isBlank(properties
        .getProperty("db.externalPoolClassName"));

    if (!usingCommonsDBCPPool) {
      return false;
    }

    try {
      boolean alertAlreadyCraeted = PgJdbcDatesIssueData.existsAlert(cp, ALERT_NAME);
      return !alertAlreadyCraeted;
    } catch (Exception e) {
      log.error("Error processing alert", e);
      return false;
    }
  }

  private void createAlert() throws Exception {
    log.warn(ALERT_NAME + " - " + ALERT_TXT);
    ConnectionProvider cp = getConnectionProvider();
    PgJdbcDatesIssueData.insertAlertRule(cp, ALERT_NAME);
    String ruleId = PgJdbcDatesIssueData.getAlertRuleId(cp, ALERT_NAME);
    PgJdbcDatesIssueData.insertAlertRecipient(cp, ruleId);
    PgJdbcDatesIssueData.insertAlert(cp, ALERT_TXT, ruleId);
  }

  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits(CORE, PR15Q4, FIXED_VERSION);
  }

  @Override
  protected boolean executeOnInstall() {
    return false;
  }
}
