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
 * All portions are Copyright (C) 2016-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.security;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Ignore;
import org.junit.Test;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;

/**
 * Test cases covering references to cross natural tree organizations. They should not be allowed.
 * 
 * @author alostale
 *
 */
public class StandardCrossOrganizationReference extends CrossOrganizationReference {
  /** References from org Spain to USA should not be allowed on insertion */
  @Test
  @Ignore("Expected exception is not thrown on insert, see issue #32063")
  public void crossOrgRefShouldBeIllegalOnInsert() {
    createOrder(SPAIN_ORG, USA_WAREHOUSE);

    exception.expect(OBSecurityException.class);

    OBDal.getInstance().commitAndClose();
  }

  /** References from org Spain to USA should not be allowed on update */
  @Test
  public void crossOrgRefShouldBeIllegalOnUpdate() {
    Order order = createOrder(SPAIN_ORG, SPAIN_WAREHOUSE);
    order.setWarehouse(OBDal.getInstance().getProxy(Warehouse.class, USA_WAREHOUSE));

    exception.expect(OBSecurityException.class);

    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void combinedAdminAndCrossOrgModes() {
    try {
      OBContext.setAdminMode(false);
      assertThat("Admin mode (no cross org)", OBContext.getOBContext().isInAdministratorMode(),
          is(true));
      assertThat("No cross org mode (in admin)",
          OBContext.getOBContext().isInCrossOrgAdministratorMode(), is(false));

      OBContext.setCrossOrgReferenceAdminMode();
      assertThat("Admin mode (with cross org)", OBContext.getOBContext().isInAdministratorMode(),
          is(true));
      assertThat("Cross org mode (in admin)",
          OBContext.getOBContext().isInCrossOrgAdministratorMode(), is(true));
    } finally {
      OBContext.restorePreviousMode();
      OBContext.restorePreviousCrossOrgReferenceMode();
    }
  }
}
