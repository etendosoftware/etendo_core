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
package org.openbravo.test.webservice;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.hibernate.criterion.Restrictions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.geography.Country;
import org.openbravo.model.common.geography.Region;

@RunWith(Parameterized.class)
public class WSReadableClientsTest extends BaseWSTest {

  private static final String COUNTRY_ID = "114";
  private static final String REGION_VALUE = "T1";
  private static final String REGION_NAME = "TEST_REGION";
  private String clientId;
  private String orgId;
  private boolean isReadable;

  /**
   * @param clientId
   *          client ID of the object created in the test
   * @param orgId
   *          organization ID of the object created in the test
   * @param isReadable
   *          whether the web service result should contain the object created in the test
   */
  public WSReadableClientsTest(String clientId, String orgId, boolean isReadable) {
    this.clientId = clientId;
    this.orgId = orgId;
    this.isReadable = isReadable;
  }

  @Parameters(name = "clientId: {0}, orgId: {1}, isVisible: {2}")
  public static Collection<Object[]> data() {
    // Create a region for the "System" client and another one for the "QA Testing" client
    return Arrays.asList(new Object[][] { { "0", "0", true }, { QA_TEST_CLIENT_ID, "0", false } });
  }

  /**
   * Test to ensure that DAL web services return just the child objects which belong to the readable
   * clients of the current role.
   */
  @Test
  public void canReadChildPropertiesOfReadableClient() {
    // use a role of "F&B International Group" client
    setTestAdminContext();
    String response = doTestGetRequest("/ws/dal/Country/" + COUNTRY_ID, null, 200);
    assertThat("DAL Web Service response contains the expected regions",
        response.contains(REGION_NAME), equalTo(isReadable));
  }

  @Before
  public void createTestRegion() {
    OBContext.setAdminMode();
    try {
      Region region = OBProvider.getInstance().get(Region.class);
      region.setCountry(OBDal.getInstance().getProxy(Country.class, COUNTRY_ID));
      region.setClient(OBDal.getInstance().getProxy(Client.class, clientId));
      region.setOrganization(OBDal.getInstance().getProxy(Organization.class, orgId));
      region.setSearchKey(REGION_VALUE);
      region.setName(REGION_NAME);
      OBDal.getInstance().save(region);
      OBDal.getInstance().commitAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @After
  public void deleteTestRegion() {
    OBContext.setAdminMode();
    try {
      OBCriteria<Region> regionCriteria = OBDal.getInstance().createCriteria(Region.class);
      regionCriteria.add(Restrictions.eq(Region.PROPERTY_COUNTRY + ".id", COUNTRY_ID));
      regionCriteria.add(Restrictions.eq(Region.PROPERTY_NAME, REGION_NAME));
      regionCriteria.setFilterOnReadableClients(false);
      regionCriteria.setMaxResults(1);
      Region region = (Region) regionCriteria.uniqueResult();
      OBDal.getInstance().remove(region);
      OBDal.getInstance().commitAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
