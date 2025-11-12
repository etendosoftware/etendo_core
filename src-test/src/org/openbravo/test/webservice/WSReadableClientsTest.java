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
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.geography.Country;
import org.openbravo.model.common.geography.Region;

public class WSReadableClientsTest extends BaseWSTest {

  private static final String COUNTRY_ID = "114";
  private static final String REGION_VALUE = "T1";
  private static final String REGION_NAME = "TEST_REGION";
  private static final Logger log = LogManager.getLogger();

  private static Stream<Arguments> readableClientScenarios() {
    return Stream.of(Arguments.of("0", "0", true),
        Arguments.of(QA_TEST_CLIENT_ID, "0", false));
  }

  /**
   * Test to ensure that DAL web services return just the child objects which belong to the readable
   * clients of the current role.
   */
  @ParameterizedTest(name = "clientId: {0}, orgId: {1}, visible: {2}")
  @MethodSource("readableClientScenarios")
  public void canReadChildPropertiesOfReadableClient(String clientId, String orgId,
      boolean isReadable) {
    setTestAdminContext();
    createTestRegion(clientId, orgId);
    try {
      String response = doTestGetRequest("/ws/dal/Country/" + COUNTRY_ID, null, 200);
      log.info("Readable client scenario client={} org={} -> contains region? {}", clientId,
          orgId, response.contains(REGION_NAME));
      assertThat("DAL Web Service response contains the expected regions",
          response.contains(REGION_NAME), equalTo(isReadable));
    } finally {
      deleteTestRegion();
    }
  }

  private void createTestRegion(String clientId, String orgId) {
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

  private void deleteTestRegion() {
    OBContext.setAdminMode();
    try {
      OBCriteria<Region> regionCriteria = OBDal.getInstance().createCriteria(Region.class);
      regionCriteria.addEqual(Region.PROPERTY_COUNTRY + ".id", COUNTRY_ID);
      regionCriteria.addEqual(Region.PROPERTY_NAME, REGION_NAME);
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
