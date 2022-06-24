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
 * All portions are Copyright (C) 2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.copyLinesFromOrders.data;

/**
 * Create orderCount Orders with linesCountPerOrder lines each.
 * 
 * @author Mark
 *
 */
public class CLFOTestDataPerformance extends CopyLinesFromOrdersTestData {

  private int orderCount;
  private int linesCountPerOrder;

  public CLFOTestDataPerformance(int orderCount, int linesCountPerOrder) {
    this.orderCount = orderCount;
    this.linesCountPerOrder = linesCountPerOrder;
    initialize();
  }

  @Override
  public void initialize() {
    generateDataForPerformanceTest(this.orderCount, this.linesCountPerOrder);
  }

  @Override
  public String getTestNumber() {
    return "09";
  }

  @Override
  public String getTestDescription() {
    return "Create " + orderCount + " Orders with " + linesCountPerOrder
        + " lines each. Copy From all 10 Orders (50 lines).";
  }

  @Override
  public boolean isExecuteAsQAAdmin() {
    return true;
  }

}
