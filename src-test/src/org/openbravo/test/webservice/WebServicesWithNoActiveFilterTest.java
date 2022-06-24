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

package org.openbravo.test.webservice;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.geography.Country;

/**
 * Test cases for JSON and dal Webservices, using noActiveFilter.
 * 
 * @author NaroaIriarte
 *
 */

public class WebServicesWithNoActiveFilterTest extends BaseWSTest {
  private static final String GERMANY_ID_DAL = "<id>101</id>";
  private static final String GERMANY_ID_JSON = "Country\\/101";
  private static final String GERMANY_ID = "101";

  /**
   * Test to ensure that the no_active_filter is working when it is set to true in the JSON
   * WebServices. The expected behavior is getting the elements which are not active.
   */
  @Test
  public void jsonWithNoActiveFilterTrue() throws JSONException {
    OBContext.setAdminMode();
    try {
      String jsonResp = jsonRequest("_noActiveFilter=true");
      assertTrue(jsonResp.contains(GERMANY_ID_JSON));
      Country country = OBDal.getInstance().get(Country.class, GERMANY_ID);
      country.setActive(true);
    } finally {
      OBDal.getInstance().commitAndClose();
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test to ensure that the no_active_filter is working when it is set to false in the JSON
   * WebServices. The expected behavior is not getting the elements which are not active.
   */
  @Test
  public void jsonWithNoActiveFilterFalse() throws JSONException {
    OBContext.setAdminMode();
    try {
      String jsonResp = jsonRequest("_noActiveFilter=false");
      assertFalse(jsonResp.contains(GERMANY_ID_JSON));
      Country country = OBDal.getInstance().get(Country.class, GERMANY_ID);
      country.setActive(true);
    } finally {
      OBDal.getInstance().commitAndClose();
      OBContext.restorePreviousMode();
    }

  }

  /**
   * Test to ensure that the no_active_filter is working when it is set to true in the dal
   * WebServices. The expected behavior is getting the elements which are not active.
   */
  @Test
  public void dalWithNoActiveFilterTrue() {
    OBContext.setAdminMode();
    try {
      String dalResp = dalRequest("/ws/dal/Country?_noActiveFilter=true");
      assertTrue(dalResp.contains(GERMANY_ID_DAL));
      Country country = OBDal.getInstance().get(Country.class, GERMANY_ID);
      country.setActive(true);
    } finally {
      OBDal.getInstance().commitAndClose();
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test to ensure that the no_active_filter is working when it is set to false in the dal
   * WebServices. The expected behavior is not getting the elements which are not active.
   */
  @Test
  public void dalWithNoActiveFilterFalse() {
    OBContext.setAdminMode();
    try {
      String dalResp = dalRequest("/ws/dal/Country?_noActiveFilter=false");
      assertFalse(dalResp.contains(GERMANY_ID_DAL));
      Country country = OBDal.getInstance().get(Country.class, GERMANY_ID);
      country.setActive(true);
    } finally {
      OBDal.getInstance().commitAndClose();
      OBContext.restorePreviousMode();
    }
  }

  private String jsonRequest(String noActiveFilter) throws JSONException {
    String countryDataString;
    OBContext.setAdminMode();
    try {
      Country country = OBDal.getInstance().get(Country.class, GERMANY_ID);
      country.setActive(false);
      OBDal.getInstance().commitAndClose();

      JSONWebServices jws = new JSONWebServices();
      JSONObject resp = new JSONObject(jws.request("Country", "", noActiveFilter, "GET"));
      JSONArray countryData = resp.getJSONObject("response").getJSONArray("data");
      // Now in this JSONArray, we are going to check if the country is shown.
      countryDataString = countryData.toString();
    } finally {
      OBContext.restorePreviousMode();
    }
    return countryDataString;
  }

  private String dalRequest(String noActiveFilter) {
    String countryDataString;
    OBContext.setAdminMode();
    try {
      Country country = OBDal.getInstance().get(Country.class, GERMANY_ID);
      country.setActive(false);
      OBDal.getInstance().commitAndClose();
      countryDataString = doTestGetRequest(noActiveFilter, null, 200);

    } finally {
      OBContext.restorePreviousMode();
    }
    return countryDataString;
  }

}
