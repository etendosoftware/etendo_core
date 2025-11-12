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
 * All portions are Copyright (C) 2018-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openbravo.dal.service.OBDal;
import org.openbravo.test.base.OBBaseTest;

/** Test cases covering detection of DB model changes */
public class DBModifiedTest extends OBBaseTest {

  @Test
  public void noChangesTest() {
    assertFalse(isDBModified(false), "DB changes detected without any change performed in DB");
  }

  @Test
  public void dbChangesShouldBeDetected() {
    try {
      createDBObject();
      assertTrue(isDBModified(false), "DB changes were not detected");
    } finally {
      dropDBObject();
    }
  }

  @Test
  public void dbChecksumCanBePersisted() {
    try {
      createDBObject();
      assertTrue(isDBModified(true), "DB changes were not detected");

      assertFalse(isDBModified(false), "DB changes were detected just after updating check sum in DB");
    } finally {
      dropDBObject();
    }
  }

  @Test
  public void canCheckChangesWithoutSavingChecksum() {
    try {
      createDBObject();
      assertTrue(isDBModified(false), "DB changes were not detected");

      assertTrue(isDBModified(false), "DB changes were not detected after calling AD_DB_MODIFIED persisting checksum ");
    } finally {
      dropDBObject();
    }
  }

  private boolean isDBModified(boolean saveChecksum) {
    Object modified = OBDal.getInstance()
        .getSession()
        .createNativeQuery("SELECT AD_DB_MODIFIED(:saveChecksum) FROM DUAL")
        .setParameter("saveChecksum", saveChecksum ? "Y" : "N") //
        .uniqueResult();

    // PG returns Character and ORA String
    return modified.equals('Y') || modified.equals("Y");
  }

  private void createDBObject() {
    OBDal.getInstance()
        .getSession()
        .createNativeQuery("CREATE TABLE TEST_OBJ AS SELECT * FROM DUAL") //
        .executeUpdate();
  }

  private void dropDBObject() {
    OBDal.getInstance()
        .getSession() //
        .createNativeQuery("DROP TABLE TEST_OBJ") //
        .executeUpdate();

    // reset checksum after dropping object
    isDBModified(true);
  }
}
