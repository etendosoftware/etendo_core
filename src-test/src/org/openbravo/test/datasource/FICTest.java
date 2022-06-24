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
 * All portions are Copyright (C) 2014-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.datasource;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.junit.Test;
import org.openbravo.client.kernel.reference.DateUIDefinition;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.test.base.Issue;

/**
 * Test cases for FormInitializationComponent
 * 
 * @author alostale
 * 
 */
public class FICTest extends BaseDataSourceTestDal {

  /**
   * Auxiliary Input for which SQL can't be evaluated on NEW and the value is correctly set by
   * callouts, should be populated when creating a new record.
   */
  @Test
  @Issue("27234")
  public void testAuxiliaryInputBasedOnCallout() throws Exception {
    Map<String, String> params = new HashMap<String, String>();
    params.put("MODE", "NEW");
    params.put("_action", "org.openbravo.client.application.window.FormInitializationComponent");
    params.put("TAB_ID", "186"); // Sales Order
    params.put("PARENT_ID", null);
    String response = doRequest("/org.openbravo.client.kernel", params, 200, "POST");

    JSONObject auxiliaryInputs = new JSONObject(response).getJSONObject("auxiliaryInputValues");
    assertTrue("ORDERTYPE should be set", auxiliaryInputs.has("ORDERTYPE"));

    JSONObject orderType = auxiliaryInputs.getJSONObject("ORDERTYPE");
    assertTrue("ORDERTYPE should have value",
        orderType.has("value") && StringUtils.isNotEmpty(orderType.getString("value")));
  }

  /**
   * Tests FIC doesn't change date-time value when row is retrieved to be edited
   */
  @Test
  @Issue("28541")
  public void dateTimeShouldntChange() throws Exception {
    OBCriteria<CostingRule> qRule = OBDal.getInstance().createCriteria(CostingRule.class);
    qRule.add(Restrictions.isNotNull(CostingRule.PROPERTY_STARTINGDATE));
    qRule.setMaxResults(1);
    assertThat(qRule.list(), not(empty()));

    CostingRule rule = qRule.list().get(0);

    Map<String, String> params = new HashMap<String, String>();
    params.put("MODE", "EDIT");
    params.put("_action", "org.openbravo.client.application.window.FormInitializationComponent");
    params.put("TAB_ID", "6868B706DA8340158DE353A6C252A564"); // Costing Rules
    params.put("ROW_ID", rule.getId());
    String response = doRequest("/org.openbravo.client.kernel", params, 200, "POST");

    String ficDateFromValue = new JSONObject(response).getJSONObject("columnValues")
        .getJSONObject("Datefrom")
        .getString("value");

    // FIC returns date-time in UTC, let's convert actual date-time to UTC...
    SimpleDateFormat utcFormatter = new DateUIDefinition().getFormat();
    utcFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    String utcFormattedDateTime = utcFormatter.format(rule.getStartingDate());

    // ...and compare it with the returned value
    assertThat(ficDateFromValue, startsWith(utcFormattedDateTime));
  }

  /**
   * Tests that the FIC returns the expected value for a combo which links to the parent tab after
   * clearing the organization field in the sub-tab.
   */
  @Test
  @Issue("38635")
  public void selectCorrectComboValueAfterClearingOrgField() throws Exception {
    final String selectedRoleId = "C7E9112E632348F396B4967517E62805";
    StringBuilder urlParams = new StringBuilder();
    urlParams.append("?MODE=CHANGE");
    urlParams.append("&PARENT_ID=" + selectedRoleId);
    urlParams.append("&TAB_ID=351"); // Org Access tab
    urlParams.append("&ROW_ID=null");
    urlParams.append("&CHANGED_COLUMN=inpadOrgId");
    urlParams
        .append("&_action=org.openbravo.client.application.window.FormInitializationComponent");

    JSONObject content = new JSONObject();
    content.put("inpadOrgId", "null"); // clearing organization field
    content.put("inpadRoleId", selectedRoleId);

    String response = doRequest("/org.openbravo.client.kernel" + urlParams.toString(),
        content.toString(), 200, "POST", "application/json");

    JSONObject columnValues = new JSONObject(response).getJSONObject("columnValues");
    assertTrue("AD_Role_ID column should be returned", columnValues.has("AD_Role_ID"));

    JSONObject role = columnValues.getJSONObject("AD_Role_ID");
    assertTrue("AD_Role_ID column value is correct",
        role.has("value") && selectedRoleId.equals(role.getString("value")));
  }
}
