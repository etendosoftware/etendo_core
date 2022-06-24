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
 * All portions are Copyright (C) 2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.datasource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

/**
 * Test cases for ExtendedNavigationModel
 * 
 * @author airaceburu
 * 
 */
public class ExtendedNavigationModelTest extends BaseDataSourceTestDal {

  // Role QA Testing Admin
  private static String ROLE_ID = "4028E6C72959682B01295A071429011E";

  // Language English (USA)
  private static String LANGUAGE_ID = "192";

  // Organization Spain
  private static String ORGANIZATION_ID = "357947E87C284935AD1D783CF6F099A1";

  // Warehouse with name: Spain warehouse
  private static String WAREHOUSE1_ID = "4028E6C72959682B01295ECFEF4502A0";

  // Sales order's lines row
  private static String row1 = "16A91C971E0F4E4E93F3D46587E6A02B";

  // Return from customer's lines row
  private static String row2 = "DE5E2B27DE494F9B8497E58FCA5EB427";

  // Sales invoice window
  private static String salesInvoiceWindow = "167";

  // Return material receipt window
  private static String returnMaterialReceiptWindow = "123271B9AD60469BAE8A924841456B63";

  // Sales order's lines tab
  private static String salesOrderLinesTab = "187";

  // Sales order window
  private static String salesOrderWindow = "143";

  // Return from customer's lines tab

  private static String returnFromCustomerLinesTab = "AF4090093D471431E040007F010048A5";

  // Return from customer window
  private static String returnFromCustomerWindow = "FF808081330213E60133021822E40007";

  /**
   * Tests the link to the lines tab of the sales order window
   */
  @Test
  public void testTableLevelRulesSalesOrder() throws Exception {
    // Change profile
    changeProfile(ROLE_ID, LANGUAGE_ID, ORGANIZATION_ID, WAREHOUSE1_ID);
    // Create a request to open a sales order line from the sales invoice window
    Map<String, String> params = new HashMap<String, String>();
    params.put("Command", "JSON");
    params.put("inpEntityName", "OrderLine");
    params.put("inpKeyReferenceId", row1);
    params.put("inpwindowId", salesInvoiceWindow);
    params.put("inpKeyReferenceColumnName", "C_OrderLine_ID");
    // Send the request
    String responseString = doRequest("/utility/ReferencedLink.html", params, 200, "POST");
    JSONObject responseJson = new JSONObject(responseString);
    // Check that the returned tab is the lines tab of the sales order window
    assertThat("The destination tab is not the lines tab of the sales order window",
        responseJson.getString("tabId"), equalTo(salesOrderLinesTab));
    // Check that the returned window is the sales order window
    assertThat("The destination window is not the sales order window",
        responseJson.getString("windowId"), equalTo(salesOrderWindow));
  }

  /**
   * Tests the link to the lines tab of the return from customer window
   */
  @Test
  public void testTableLevelRulesReturnFromCustomer() throws Exception {
    // Change profile
    changeProfile(ROLE_ID, LANGUAGE_ID, ORGANIZATION_ID, WAREHOUSE1_ID);
    // Create a request to open a sales order line from the return material receipt window
    Map<String, String> params2 = new HashMap<String, String>();
    params2.put("Command", "JSON");
    params2.put("inpEntityName", "OrderLine");
    params2.put("inpKeyReferenceId", row2);
    params2.put("inpwindowId", returnMaterialReceiptWindow);
    params2.put("inpKeyReferenceColumnName", "C_OrderLine_ID");
    // Send the request
    String responseString2 = doRequest("/utility/ReferencedLink.html", params2, 200, "POST");
    JSONObject responseJson2 = new JSONObject(responseString2);
    // Check that the returned tab is the lines tab of the return from customer window
    assertThat("The destination tab is not the lines tab of the return from customer window",
        responseJson2.getString("tabId"), equalTo(returnFromCustomerLinesTab));
    // Check that the returned window is the return from customer window
    assertThat("The destination window is not the return from customer window",
        responseJson2.getString("windowId"), equalTo(returnFromCustomerWindow));
  }
}
