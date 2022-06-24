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
 * All portions are Copyright (C) 2010-2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.system;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.sql.Connection;

import javax.servlet.ServletException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.Test;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test the ErrorTextParser class logic by reproducing some scenarios that lead to an error that
 * should be properly translated by the parser.
 * 
 * @author huehner
 */

public class ErrorTextParserIntegrationTest extends OBBaseTest {

  @Test
  public void testDuplicatePrimaryKey() throws Exception {
    doErrorTextParserTest(1);
  }

  @Test
  public void testNotNull() throws Exception {
    // test disabled on pgsql, as its error message text in not-null case cannot be parsed so far
    if (getConnectionProvider().getRDBMS().equals("ORACLE")) {
      doErrorTextParserTest(2);
    }
  }

  @Test
  public void testBoolean() throws Exception {
    doErrorTextParserTest(3);
  }

  @Test
  public void testUniqueSingleField() throws Exception {
    doErrorTextParserTest(4);
  }

  @Test
  public void testUniqueMultipleFields() throws Exception {
    doErrorTextParserTest(5);
  }

  @Test
  public void testFKInsert() throws Exception {
    doErrorTextParserTest(6);
  }

  @Test
  public void testFKDelete() throws Exception {
    doErrorTextParserTest(7);
  }

  @Test
  public void testNonBooleanCheck() throws Exception {
    doErrorTextParserTest(8);
  }

  @Test
  public void testTriggerError() throws Exception {
    doErrorTextParserTest(9);
  }

  @Test
  public void whenStatementFailsNoStackTraceIsLoggedByDefault() throws Exception {
    setTestLogAppenderLevel(Level.INFO);
    setLogStackTraces(true);

    doErrorTextParserTest(1);

    assertThat("Stack trace shouldn't be included in log",
        getTestLogAppender().getMessages(Level.ERROR), not(hasItem(
            containsString("at org.openbravo.test.system.ErrorTextParserTestData.insertUserPK"))));
  }

  @Test
  public void whenStatementFailsStackTraceIsLoggedHavingDebugLogLevel() throws Exception {
    Logger sqlcLogger = LogManager.getLogger(ErrorTextParserTestData.class);
    Level originalLogLevel = sqlcLogger.getLevel();

    try {
      setTestLogAppenderLevel(Level.DEBUG);
      setLoggerLevel(sqlcLogger, Level.DEBUG);
      setLogStackTraces(true);

      doErrorTextParserTest(1);

      assertThat("Stack trace should be included in log",
          getTestLogAppender().getMessages(Level.ERROR), hasItem(
              containsString("at org.openbravo.test.system.ErrorTextParserTestData.insertUserPK")));
    } finally {
      setLoggerLevel(sqlcLogger, originalLogLevel);
    }
  }

  private void setLoggerLevel(Logger logger, Level level) {
    LoggerContext context = LoggerContext.getContext(false);
    Configuration config = context.getConfiguration();
    LoggerConfig loggerConfig = config.getLoggerConfig(logger.getName());
    loggerConfig.setLevel(level);
    context.updateLoggers();
  }

  private void doErrorTextParserTest(int testCase) throws Exception {
    ConnectionProvider conn = getConnectionProvider();
    VariablesSecureApp vars = new VariablesSecureApp("", "", "");
    Connection con = conn.getTransactionConnection();

    if (testCase != 9) {
      // set core to development to avoid the _mod_ trigger restrictions
      ErrorTextParserTestData.setCoreInDevelopment(con, conn);
    } else {
      // ensure that the are not any modules in development
      ErrorTextParserTestData.setNotInDevelopment(con, conn);
    }

    String errorMessage = "";
    String expectedErrorMessage = "";
    try {
      switch (testCase) {
        case 1:
          expectedErrorMessage = "Internal Error: Duplicate primary key/uuid. Your record has not been saved into the table User/Contact";
          ErrorTextParserTestData.insertUserPK(con, conn, "0", "N", "name");
          break;
        case 2:
          expectedErrorMessage = "The column Active is mandatory and cannot be left empty.";
          ErrorTextParserTestData.insertUser(con, conn, null, "name");
          break;
        case 3:
          expectedErrorMessage = "Only values 'Y'or 'N' may be entered into the field Active.";
          ErrorTextParserTestData.insertUser(con, conn, "B", "name");
          break;
        case 4:
          expectedErrorMessage = "There is already a Client with the same Name. Name must be unique. You must change the values entered.";
          ErrorTextParserTestData.insertClientWithName(con, conn, "System", "System");
          break;
        case 5:
          expectedErrorMessage = "There is already a Month Translation with the same (Language, Month). (Language, Month) must be unique. You must change the values entered.";
          ErrorTextParserTestData.insertMonthTrl(con, conn);
          break;
        case 6:
          expectedErrorMessage = "This record cannot be deleted because it is associated with other existing elements. Please see Linked Items";
          ErrorTextParserTestData.insertUserWithClient(con, conn, "42", "Y", "Openbravo");
          break;
        case 7:
          expectedErrorMessage = "This record cannot be deleted because it is associated with other existing elements. Please see Linked Items";
          ErrorTextParserTestData.deleteClient(con, conn, "0");
          break;
        case 8:
          expectedErrorMessage = "There is a constraint defined that was not satisfied. Please check the data entered";
          ErrorTextParserTestData.insertProcess(con, conn, "value", "name", "test");
          break;
        case 9:
          expectedErrorMessage = "Cannot update an object in a module not in development and without an active template";
          ErrorTextParserTestData.updateTableDescription(con, conn, "description", "146");
          break;
      }

    } catch (ServletException se) {
      errorMessage = se.getMessage();
    } finally {
      conn.releaseRollbackConnection(con);
    }
    // Ignore the testCase 5 until another two-part unique constraint is found
    if (testCase != 5) {
      OBError trlError = Utility.translateError(conn, vars, "en_US", errorMessage);

      assertEquals(expectedErrorMessage, trlError.getMessage());
    }
  }
}
