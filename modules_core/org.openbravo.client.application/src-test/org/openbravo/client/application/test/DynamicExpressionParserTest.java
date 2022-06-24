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
 * All portions are Copyright (C) 2010-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.DynamicExpressionParser;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Tab;

/** Tests parsing of dynamic expression */
public class DynamicExpressionParserTest extends WeldBaseTest {
  private static final String SALES_INVOICE_LINE_TAB = "270";

  @Test
  public void eqFalseExpression() {
    assertExpression("((@Financial_Invoice_Line@='N'))",
        "((OB.Utilities.getValue(currentValues,'financialInvoiceLine') === false))");
  }

  @Test
  public void eqTrueExpression() {
    assertExpression("@Financial_Invoice_Line@='Y'",
        "OB.Utilities.getValue(currentValues,'financialInvoiceLine') === true");
  }

  @Test
  public void ltExpression() {
    assertExpression("@lineNetAmt@ < -5",
        "OB.Utilities.getValue(currentValues,'lineNetAmount')  <  -5");
  }

  @Test
  public void leExpression() {
    assertExpression("@lineNetAmt@ <= 5",
        "OB.Utilities.getValue(currentValues,'lineNetAmount')  <=  5");
  }

  @Test
  public void gtExpression() {
    assertExpression("@lineNetAmt@ > 100",
        "OB.Utilities.getValue(currentValues,'lineNetAmount')  >  100");
  }

  @Test
  public void geExpression() {
    assertExpression("@lineNetAmt@ >= 100",
        "OB.Utilities.getValue(currentValues,'lineNetAmount')  >=  100");
  }

  @Test
  public void orExpression() {
    assertExpression("@Financial_Invoice_Line@='N' | @lineNetAmt@ > 100",
        "OB.Utilities.getValue(currentValues,'financialInvoiceLine') === false || OB.Utilities.getValue(currentValues,'lineNetAmount')  >  100");
  }

  @Test
  public void andExpression() {
    assertExpression("@Financial_Invoice_Line@='N' & @lineNetAmt@ > 100",
        "OB.Utilities.getValue(currentValues,'financialInvoiceLine') === false && OB.Utilities.getValue(currentValues,'lineNetAmount')  >  100");
  }

  private void assertExpression(String originalExpression, String expectedExpression) {
    setSystemAdministratorContext();
    Tab tab = OBDal.getInstance().get(Tab.class, SALES_INVOICE_LINE_TAB);
    DynamicExpressionParser parser = new DynamicExpressionParser(originalExpression, tab);

    String parsedExpression = parser.getJSExpression();
    assertThat("Parsed dynamic expresion [" + originalExpression + "]", parsedExpression,
        equalTo(expectedExpression));
  }
}
