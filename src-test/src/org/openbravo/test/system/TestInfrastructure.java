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

package org.openbravo.test.system;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test cases to verify test case infrastructure
 * 
 * @author alostale
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestInfrastructure extends OBBaseTest {
  private static final String NEW_UOM_ID = "1000";

  /** Creates a BaseOBObject */
  @Test
  public void inf001JustCreateAnObject() {
    createNewUom();
  }

  /**
   * Checks the object created in previous test is still present. This ensures that after correct
   * test completion DAL commit is performed.
   */
  @Test
  public void inf002CreatedObjectShouldPresent() {
    assertThat(getNewUom(), is(notNullValue()));
  }

  /** Removes the object created before */
  @Test
  public void inf003DeletePreviousObject() {
    UOM newUom = getNewUom();
    assertThat(newUom, is(notNullValue()));
    OBDal.getInstance().remove(newUom);
  }

  /**
   * Checks the object removed in previous test is not present anymore. This ensures that after
   * correct test completion DAL commit is performed.
   */
  @Test
  public void inf004RemovedObjectShouldNotPresent() {
    assertThat(getNewUom(), is(nullValue()));
  }

  private void createNewUom() {
    setSystemAdministratorContext();
    UOM newUom = OBProvider.getInstance().get(UOM.class);
    newUom.setId(NEW_UOM_ID);
    newUom.setNewOBObject(true);
    newUom.setName("TESTUOM");
    newUom.setEDICode("TT");
    newUom.setStandardPrecision(1L);
    newUom.setCostingPrecision(1L);
    OBDal.getInstance().save(newUom);
  }

  private UOM getNewUom() {
    setSystemAdministratorContext();
    return OBDal.getInstance().get(UOM.class, NEW_UOM_ID);
  }
}
