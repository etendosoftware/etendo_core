/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.utility;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.base.TestConstants;

/**
 * Integration tests for {@link DimensionDisplayUtility}.
 * Tests the accounting dimension configuration retrieval from real DB data.
 */
public class DimensionDisplayUtilityIntegrationTest extends OBBaseTest {

  @Test
  public void testGetAccountingDimensionConfigurationReturnsNonNull() {
    setSystemAdministratorContext();
    OBContext.setAdminMode(true);
    try {
      Client fbClient = OBDal.getInstance().get(Client.class, TestConstants.Clients.FB_GRP);
      assertNotNull("F&B client should exist", fbClient);

      Map<String, String> config = DimensionDisplayUtility
          .getAccountingDimensionConfiguration(fbClient);
      assertNotNull("Dimension configuration should not be null", config);
      assertFalse("Configuration map should not be empty", config.isEmpty());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testDimensionConfigContainsExpectedKeys() {
    setSystemAdministratorContext();
    OBContext.setAdminMode(true);
    try {
      Client fbClient = OBDal.getInstance().get(Client.class, TestConstants.Clients.FB_GRP);
      Map<String, String> config = DimensionDisplayUtility
          .getAccountingDimensionConfiguration(fbClient);

      // Should contain at least one dimension type
      boolean hasDimension = config.keySet().stream()
          .anyMatch(k -> k.contains("$Element"));
      assertTrue("Configuration should contain dimension element keys", hasDimension);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testDimensionConfigValuesAreYorN() {
    setSystemAdministratorContext();
    OBContext.setAdminMode(true);
    try {
      Client fbClient = OBDal.getInstance().get(Client.class, TestConstants.Clients.FB_GRP);
      Map<String, String> config = DimensionDisplayUtility
          .getAccountingDimensionConfiguration(fbClient);

      for (Map.Entry<String, String> entry : config.entrySet()) {
        String val = entry.getValue();
        assertTrue("Dimension value should be Y or N but was '" + val + "' for key '"
            + entry.getKey() + "'", "Y".equals(val) || "N".equals(val));
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testDimensionConfigForSystemClientReturnsResult() {
    setSystemAdministratorContext();
    OBContext.setAdminMode(true);
    try {
      Client systemClient = OBDal.getInstance().get(Client.class, "0");
      Map<String, String> config = DimensionDisplayUtility
          .getAccountingDimensionConfiguration(systemClient);
      assertNotNull("System client dimension config should not be null", config);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
