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
 * All portions are Copyright (C) 2008-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.webservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.net.HttpURLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.geography.City;
import org.openbravo.model.common.geography.Country;
import org.openbravo.model.common.geography.Region;
import org.openbravo.test.base.Issue;

/**
 * Test webservice for reading, updating and posting. The test cases here require a running
 * Openbravo at http://localhost:8080/openbravo.
 * 
 * IMPORTANT: Test cases are called by one of them called testContent(). The name of the rest of the
 * test cases NOT begin by "test...".
 * 
 * @author mtaal
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WSUpdateTest extends BaseWSTest {

  private static final Logger log = LogManager.getLogger();

  private static String cityId = null;

  /**
   * Creates a city through a webservice calls. This test must be run before the others because it
   * sets the cityId member in this class.
   */
  @Test
  public void testACreateCity() {
    // do not replace this with a call to setUserContext,
    // the city must be stored using the client/org of the 100 user
    // this ensures that webservice calls will be able to find the city
    // again
    OBContext.setOBContext("100");

    // first delete the current cities, as we should start fresh
    final OBCriteria<City> obc = OBDal.getInstance().createCriteria(City.class);
    for (final City c : obc.list()) {
      OBDal.getInstance().remove(c);
    }

    final City city = OBProvider.getInstance().get(City.class);
    city.setAreaCode("3941");
    city.setCoordinates("00");
    city.setCoordinates("lo");
    city.setPostalCode("postal");
    city.setName("name");
    city.setCountry(getOneInstance(Country.class));
    city.setRegion(getOneInstance(Region.class));
    OBDal.getInstance().save(city);
    commitTransaction();
    cityId = city.getId();
  }

  /**
   * test case order execution cannot be warrantied, so check if city has already been created
   * 
   * @throws Exception
   */
  private void initializeCreateCity() throws Exception {
    if (cityId == null) {
      testACreateCity();
    }
  }

  /**
   * Read the created city using a webservice and make a small change and post it back.
   * 
   * @throws Exception
   */
  @Test
  public void testBReadUpdateCity() throws Exception {
    initializeCreateCity();

    final String city = doTestGetRequest("/ws/dal/City/" + cityId, null, 200);
    log.debug(System.currentTimeMillis());
    String newCity;
    if (city.indexOf("<coordinates>") != -1) { // test already run
      final int index1 = city.indexOf("<coordinates>");
      final int index2 = city.indexOf("</coordinates>");
      newCity = city.substring(0, index1) + "<coordinates>"
          + ("" + System.currentTimeMillis()).substring(5) + city.substring(index2);
    } else {
      newCity = city.replaceAll("<coordinates/>",
          "<coordinates>" + ("" + System.currentTimeMillis()).substring(5) + "</coordinates>");
    }
    final String content = doContentRequest("/ws/dal/City/" + cityId, newCity, 200, "<updated>",
        "POST");
    assertTrue(content.indexOf("City id=\"" + cityId + "") != -1);
    OBDal.getInstance().commitAndClose();
  }

  /**
   * Test is an error is returned if an incorrect message is posted.
   * 
   * @throws Exception
   */
  @Test
  public void testCIncorrectRootTag() throws Exception {
    initializeCreateCity();

    final String city = doTestGetRequest("/ws/dal/City/" + cityId, null, 200);
    log.debug(city);
    log.debug("---");
    String newCity = city.replaceAll("ob:Openbravo", "ob:WrongOpenbravo");
    doContentRequest("/ws/dal/City/" + cityId, newCity, 500, "<updated>", "POST");
  }

  /**
   * Test case executes the following steps: 1) get a city, 2) create a city, 3) count the cities,
   * 4) retrieve the cities through a query, 5) delete the new city, 6) check that it has been
   * deleted.
   * 
   * @throws Exception
   */
  @Test
  public void testDReadAddDeleteCity() throws Exception {
    initializeCreateCity();

    doTestReadAddDeleteCity(false);
  }

  @Test
  public void testEReadAddDeleteQueryCity() throws Exception {
    initializeCreateCity();

    doTestReadAddDeleteCity(true);
  }

  private void doTestReadAddDeleteCity(boolean doDeleteQuery) throws Exception {
    final String city = doTestGetRequest("/ws/dal/City/" + cityId, null, 200);
    String newCity = city.replaceAll("</name>",
        (System.currentTimeMillis() + "").substring(6) + "</name>");
    final String newName = getTagValue(newCity, "name");

    // newCity = newCity.replaceAll("City id=\"", "City id=\"test");
    // and replace the first <id>cityId</id> with <id>test...</id>
    int index = newCity.indexOf("<id>");
    newCity = newCity.substring(0, index) + "<id>test"
        + newCity.substring(index + "<id>test".length());
    index = newCity.indexOf("City id=\"");
    newCity = newCity.substring(0, index) + "City id=\"test"
        + newCity.substring(index + "City id=\"test".length());
    final String content = doContentRequest("/ws/dal/City", newCity, 200, "<inserted>", "POST");
    // log.debug(content);
    // get the id and check if it is there
    final int index1 = content.indexOf("City id=\"") + "City id=\"".length();
    final int index2 = content.indexOf("\"", index1);
    final String id = content.substring(index1, index2);

    // check if it is there
    doTestGetRequest("/ws/dal/City/" + id, "<City", 200);

    // count the cities
    doTestGetRequest("/ws/dal/City/count",
        "<ob:result xmlns:ob=\"http://www.openbravo.com\">2</ob:result>", 200);

    // test a simple whereclause
    // first count
    doTestGetRequest("/ws/dal/City/count?where=name='" + newName + "'",
        "<ob:result xmlns:ob=\"http://www.openbravo.com\">1</ob:result>", 200);

    // and then get a result, should only be one City
    final String queriedCities = doTestGetRequest("/ws/dal/City?where=name='" + newName + "'", null,
        200);
    final int queryIndex = queriedCities.indexOf("<City");
    assertTrue(queryIndex != -1);
    assertTrue(queriedCities.indexOf("<City", queryIndex + 5) == -1);

    // get all cities
    final String allCities = doTestGetRequest("/ws/dal/City", null, 200);
    // there should be two
    final int indexCity1 = allCities.indexOf("<City") + "<City".length();
    final int indexCity2 = allCities.indexOf("<City", indexCity1);
    assertTrue(indexCity1 != -1);
    assertTrue(indexCity2 != -1);

    // delete it
    if (doDeleteQuery) {
      doDirectDeleteRequest("/ws/dal/City?where=name='" + newName + "'", 200);
    } else {
      doDirectDeleteRequest("/ws/dal/City/" + id, 200);
    }

    // sleep 1 seconds, so that the city is deleted
    Thread.sleep(1000);

    // it should not be there!
    try {
      doTestGetRequest("/ws/dal/City/" + id, "<error>", 404, true, false);
      fail("City " + id + " was not deleted");
    } catch (final Exception e) {
      assertTrue(e.getCause() instanceof FileNotFoundException);
    }
  }

  /** DalWebServiceServlet does not report errors which occur at commit time */
  @Test
  @Issue("14973")
  public void testFDoTest14973() throws Exception {
    final HttpURLConnection hc = createConnection("/ws/dal/Product/1000004", "DELETE");
    hc.connect();
    assertEquals(500, hc.getResponseCode());
  }

  /**
   * Add a new city using the wrong HTTP method.
   * 
   * @throws Exception
   */
  @Test
  public void testGReadAddCityWrongMethodError() throws Exception {
    initializeCreateCity();
    final String city = doTestGetRequest("/ws/dal/City/" + cityId, null, 200);
    String newCity = city.replaceAll("</name>",
        (System.currentTimeMillis() + "").substring(6) + "</name>");
    newCity = newCity.replaceAll("id=\"", "id=\"test");
    final int index = newCity.indexOf("<id>");
    newCity = newCity.substring(0, index) + "<id>test" + newCity.substring(index + "<id>".length());
    try {
      doContentRequest("/ws/dal/City", newCity, 500, "", "PUT");
    } catch (final Exception e) {
      assertTrue(e.getMessage().indexOf("500") != -1);
    }
  }

  /**
   * Cleans up the database by removing the city. Is run as last therefore the use of the Z
   * character in the name.
   * 
   * @throws Exception
   */
  @Test
  public void testHRemoveCity() throws Exception {
    initializeCreateCity();
    doDirectDeleteRequest("/ws/dal/City/" + cityId, 200);
  }

}
