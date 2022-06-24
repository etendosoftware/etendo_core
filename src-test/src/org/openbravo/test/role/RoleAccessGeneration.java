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
 * All portions are Copyright (C) 2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.role;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.OBUIAPPViewImplementation;
import org.openbravo.client.application.ViewRoleAccess;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Test case for role access generation when a new role is created
 * 
 * @author cberner
 *
 */
public class RoleAccessGeneration extends WeldBaseTest {
  /**
   * F&B Client
   */
  private final static String CLIENT_ID = "23C59575B9CF467C9620760EB255B389";
  /**
   * zero organization id
   */
  private final static String ASTERISK_ORG_ID = "0";

  private final String CLIENT_LEVEL = " C";

  private Role role;

  @Before
  public void createDummyRole() {
    role = OBProvider.getInstance().get(Role.class);
    Client client = OBDal.getInstance().get(Client.class, CLIENT_ID);
    Organization organization = OBDal.getInstance().get(Organization.class, ASTERISK_ORG_ID);
    role.setName("Test role access generation");
    role.setClient(client);
    role.setOrganization(organization);
  }

  @Test
  public void createViewImplAccessIfAutomatic() {
    boolean isManual = false;
    createRoleAndRefresh(isManual);

    OBCriteria<OBUIAPPViewImplementation> criteria = OBDal.getInstance()
        .createCriteria(OBUIAPPViewImplementation.class);
    criteria.setFilterOnReadableClients(false);
    criteria.setFilterOnActive(true);
    List<ViewRoleAccess> roleViewsAccess = role.getObuiappViewRoleAccessList();
    List<OBUIAPPViewImplementation> allRoleImplementation = criteria.list();
    // Check if role contains all roleViewsAccess for all active ViewImpl
    assertEquals(roleViewsAccess.size(), allRoleImplementation.size());

    boolean allImplContained = true;
    for (ViewRoleAccess roleView : roleViewsAccess) {
      OBUIAPPViewImplementation viewImpl = roleView.getViewImplementation();
      if (!allRoleImplementation.contains(viewImpl)) {
        allImplContained = false;
        break;
      }
    }

    assertTrue(allImplContained);
  }

  @Test
  public void dontCreateViewImplAccessIfManual() {
    boolean isManual = true;
    createRoleAndRefresh(isManual);
    // Check ViewRoleAccess not created for role
    List<ViewRoleAccess> roleViewsAccess = role.getObuiappViewRoleAccessList();

    assertTrue(roleViewsAccess.isEmpty());
  }

  /**
   * Sets a role to manual or automatic, applies flush and refresh on DAL
   * 
   * @param isManual
   *          Sets manual or automatic role mode
   */
  private void createRoleAndRefresh(boolean isManual) {
    // Create role with automatic access and any user level
    role.setManual(isManual);
    role.setUserLevel(CLIENT_LEVEL);

    OBDal.getInstance().save(role);
    OBDal.getInstance().flush();
    // Get the updated role after triggers
    OBDal.getInstance().refresh(role);
  }

  @After
  public void deleteDummyRole() {
    OBDal.getInstance().rollbackAndClose();
  }
}
