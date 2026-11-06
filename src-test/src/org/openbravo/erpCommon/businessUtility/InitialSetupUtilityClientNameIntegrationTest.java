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

package org.openbravo.erpCommon.businessUtility;

import static org.junit.Assert.assertEquals;

import javax.persistence.PersistenceException;

import org.junit.After;
import org.junit.Test;
import org.openbravo.dal.service.OBDal;
import org.openbravo.test.base.OBBaseTest;

/**
 * Checks {@link InitialSetupUtility#isClientNameUnique()} against what the database really does:
 * a client with the name of an existing one is rejected exactly when the method says names are
 * unique. Core declares the unique name index; the Etendo GO template drops it, so this holds in
 * both kinds of environment without knowing which one it runs in.
 *
 * @covers org.openbravo.erpCommon.businessUtility.InitialSetupUtility
 */
public class InitialSetupUtilityClientNameIntegrationTest extends OBBaseTest {

  /** Discards the duplicate client, inserted or not. */
  @After
  public void rollback() {
    OBDal.getInstance().rollbackAndClose();
  }

  /** The method agrees with the database on whether a repeated client name is rejected. */
  @Test
  public void clientNameUniquenessMatchesTheDatabase() {
    boolean unique = InitialSetupUtility.isClientNameUnique();

    boolean duplicateRejected;
    try {
      OBDal.getInstance()
          .getSession()
          .createNativeQuery("INSERT INTO AD_CLIENT (AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, CREATED,"
              + " CREATEDBY, UPDATED, UPDATEDBY, VALUE, NAME)"
              + " SELECT get_uuid(), '0', 'Y', now(), '0', now(), '0', VALUE, NAME"
              + " FROM AD_CLIENT WHERE AD_CLIENT_ID = '0'")
          .executeUpdate();
      duplicateRejected = false;
    } catch (PersistenceException e) {
      duplicateRejected = true;
    }

    assertEquals("isClientNameUnique() must match whether the database rejects a repeated name",
        duplicateRejected, unique);
  }
}
