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

package org.openbravo.test.datasource;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.Issue;

/**
 * Test cases to verify CSV Encoding behavior with Arabic characters
 * 
 * @author guillermogil
 */
@Issue("26162")
public class TestCSVEncoding extends BaseDataSourceTestDal {

  /**
   * Creates a preference to ensure that the Arabic characters are correctly displayed and compares
   * the values with and without the preference
   * 
   * @throws Exception
   */

  @Test
  public void testEncoding() throws Exception {

    OBContext.setAdminMode(false);

    Organization org0 = OBDal.getInstance().get(Organization.class, "0");
    Client client0 = OBDal.getInstance().get(Client.class, "0");

    // Creation of CSV Text Encoding Preference
    final Preference pref = OBProvider.getInstance().get(Preference.class);
    pref.setClient(client0);
    pref.setOrganization(org0);
    pref.setPropertyList(true);
    pref.setActive(true);
    pref.setPropertyList(true);
    pref.setProperty("OBSERDS_CSVTextEncoding");
    // Set value UTF-8
    pref.setSearchKey("UTF-8");
    OBDal.getInstance().save(pref);
    OBDal.getInstance().commitAndClose();

    // Creations of the params to do the request
    Map<String, String> params = new HashMap<String, String>();
    params.put("_operationType", "fetch");
    params.put("_noCount", "true");
    params.put("exportAs", "csv");
    params.put("_dataSource", "isc_OBViewDataSource_0");
    params.put("_noCount", "true");
    params.put("_extraProperties", "undefined");
    params.put("tab", "151");
    params.put("exportToFile", "true");
    params.put("_textMatchStyle", "substring");
    params.put("_UTCOffsetMiliseconds", "7200000");
    params.put("operator", "and");
    params.put("_constructor", "AdvancedCriteria");
    JSONArray criteria = new JSONArray();
    JSONObject criteriaObj = new JSONObject();
    criteriaObj.put("fieldName", "iSOCode");
    criteriaObj.put("operator", "iContains");
    criteriaObj.put("value", "aed");
    criteriaObj.put("_constructor", "AdvancedCriteria");
    criteria.put(criteriaObj);
    params.put("criteria", criteria.toString());

    JSONObject viewState = new JSONObject();
    viewState.put("field",
        "[{name:\"_editLink\",frozen:true,width:56},{name:\"organization\",visible:false,width:200},{name:\"iSOCode\",visible:false,width:100},{name:\"symbol\",width:1572},{name:\"description\",visible:false,width:200},{name:\"standardPrecision\",visible:false,width:100},{name:\"costingPrecision\",visible:false,width:100},{name:\"pricePrecision\",visible:false,width:100},{name:\"currencySymbolAtTheRight\",visible:false,autoFitWidth:false,width:100},{name:\"active\",visible:false,autoFitWidth:false,width:100},{name:\"creationDate\",visible:false,width:100},{name:\"createdBy\",visible:false,width:100},{name:\"updated\",visible:false,width:100},{name:\"updatedBy\",visible:false,width:100}]");
    viewState.put("sort",
        "({fieldName:null,sortDir:\"ascending\",sortSpecifiers:[{property:\"iSOCode\",direction:\"ascending\"}]})");
    viewState.put("hilite", "null");

    JSONObject group = new JSONObject();
    group.put("groupByFields", "");
    group.put("groupingModes", "{}");
    viewState.put("group", group);
    viewState.put("filterClause", "null");
    viewState.put("summaryFunctions", "{}");
    params.put("viewState", "(" + viewState.toString() + ")");

    String response = doRequest("/org.openbravo.service.datasource/Currency", params, 200, "POST");
    String[] res = response.split("\n");

    // Remove previously created preference
    OBDal.getInstance().remove(pref);
    OBDal.getInstance().commitAndClose();

    // First assert
    assertTrue("Cannot show Arabic characters on UTF-8 encoding",
        res[1].substring(1, 4).equals("د.إ"));

    // Request again to see the differences
    String response2 = doRequest("/org.openbravo.service.datasource/Currency", params, 200, "POST");
    String[] res2 = response2.split("\n");

    OBContext.restorePreviousMode();
    OBDal.getInstance().commitAndClose();

    // Second assert
    assertTrue("On delete CSV Text Preference Arabic characters are still showing",
        res2[1].substring(1, 4).equals("?.?"));
  }
}
