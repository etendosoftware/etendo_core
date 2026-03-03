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
 * All portions are Copyright (C) 2010-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.userinterface.selectors.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openbravo.base.expression.OBScriptEngine;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.OBBindings;
import org.openbravo.dal.core.OBContext;
import org.openbravo.test.base.mock.HttpServletRequestMock;

/**
 * Tests the current API exposed to JavaScript expression through OBBindings class
 * 
 * @author iperdomo
 */
public class ExpressionsTest extends WeldBaseTest {

  private OBScriptEngine engine;
  private Logger log = LogManager.getLogger();

  private HashMap<String, String> expr = new HashMap<>();
  private Map<String, Object> bindings = new HashMap<>();

  /**
   * This before method is named setUpEt() to avoid overwriting the super setUp method that is
   * invoke automatically before this one.
   */
  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();

    initializeTestData();
  }

  @Override
  protected void beforeTestExecution(ExtensionContext context) {
    super.beforeTestExecution(context);
    try {
      initializeTestData();
    } catch (Exception e) {
      throw new RuntimeException("Error initializing test data", e);
    }
  }

  private void initializeTestData() {
    expr.clear();
    bindings.clear();

    // Everything runs as System Admin user
    setSystemAdministratorContext();

    HttpServletRequestMock request = new HttpServletRequestMock();
    engine = OBScriptEngine.getInstance();
    bindings.put("OB",
        new OBBindings(OBContext.getOBContext(), Collections.emptyMap(), request.getSession()));

    // Initialize expressions
    expr.put("Get current user's name", "OB.getContext().getUser().name");

    expr.put("Get current language", "OB.getContext().getLanguage().language");

    expr.put("Format today's date", "OB.formatDate(new Date(), 'yyyy-MM-dd')");

    expr.put("Get current client id", "OB.getContext().getCurrentClient().id");

    expr.put("Parse date with fixed format", "OB.parseDate('1979-04-24','yyyy-MM-dd')");

    expr.put("Format a parsed date",
        "OB.formatDate(OB.parseDate('1979-04-24', 'yyyy-MM-dd'), 'MM-dd-yyyy')");

    expr.put("Filter by vendor/customer",
        "if(OB.isSalesTransaction()===null){'';}"
            + "else if(OB.isSalesTransaction()==true){'e.customer = true';}"
            + "else{'e.vendor = true';}");

    expr.put("Complex expression from Java",
        "OB.getFilterExpression('org.openbravo.userinterface.selectors.test.SampleFilterExpression');");

  }

  private Object evaluateExpression(String expressionKey) {
    final String expression = expr.get(expressionKey);
    assertNotNull(expression, "Expression not found for key: " + expressionKey);
    try {
      return engine.eval(expression, bindings);
    } catch (Exception e) {
      log.error("Error evaluating expression: " + expression, e);
      fail("Error evaluating expression: " + expression + " -> " + e.getMessage());
      return null;
    }
  }

  @Test
  public void testUserName() {
    final Object result = evaluateExpression("Get current user's name");
    assertEquals(OBContext.getOBContext().getUser().getName(), result.toString());
  }

  @Test
  public void testLanguage() {
    final Object result = evaluateExpression("Get current language");
    assertEquals(OBContext.getOBContext().getLanguage().getLanguage(), result.toString());
  }

  @Test
  public void testFormatDate() {
    final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    final Object result = evaluateExpression("Format today's date");
    assertEquals(df.format(Calendar.getInstance().getTime()), result);
  }

  @Test
  public void testCurrentClientId() {
    final Object result = evaluateExpression("Get current client id");
    assertEquals(OBContext.getOBContext().getCurrentClient().getId(), result);
  }

  @Test
  public void testParseDate() {
    final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    final Object result = evaluateExpression("Parse date with fixed format");
    try {
      assertEquals(df.parse("1979-04-24"), result);
    } catch (Exception e) {
      fail("Error parsing expected date: " + e.getMessage());
    }
  }

  @Test
  public void testFormatParsedDate() {
    final Object result = evaluateExpression("Format a parsed date");
    assertEquals("04-24-1979", result);
  }

  @Test
  public void testCustomerVendorFilter() {
    final Object result = evaluateExpression("Filter by vendor/customer");
    assertEquals("", result);
  }

  @Test
  public void testGetFilterExpression() {
    final Object result = evaluateExpression("Complex expression from Java");
    assertEquals("This is a complex expression", result);
  }
}
