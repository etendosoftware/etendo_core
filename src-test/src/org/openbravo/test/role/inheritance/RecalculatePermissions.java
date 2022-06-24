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
package org.openbravo.test.role.inheritance;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;

import org.junit.Test;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.WindowAccess;
import org.openbravo.role.inheritance.RoleInheritanceManager;

/**
 * Test case for horizontal inheritance This test case is intended to simulate the "Recalculate
 * Permissions" process We remove an inherited access on purpose (this can not be done from the UI),
 * this way we can simulate the process of adding a permission without using DAL, like for example,
 * when using the "Grant Access" process which uses xsql to insert data.
 */
public class RecalculatePermissions extends WeldBaseTest {

  @Inject
  private RoleInheritanceManager manager;

  /**
   * Test case to simulate the "Recalculate Permissions" process, launched for a not template role
   */
  @Test
  public void recalculatePermissionsForRole() {
    Role template = null;
    Role role = null;
    try {
      OBContext.setAdminMode(true);
      template = RoleInheritanceTestUtils.createRole("template", RoleInheritanceTestUtils.CLIENT_ID,
          RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true, true);
      String templateId = template.getId();
      role = RoleInheritanceTestUtils.createRole("role", RoleInheritanceTestUtils.CLIENT_ID,
          RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true, true);
      String roleId = role.getId();

      OBDal.getInstance().commitAndClose();
      template = OBDal.getInstance().get(Role.class, templateId);
      role = OBDal.getInstance().get(Role.class, roleId);

      // Add inheritance
      RoleInheritanceTestUtils.addInheritance(role, template, 10L);
      OBDal.getInstance().commitAndClose();
      template = OBDal.getInstance().get(Role.class, templateId);

      // Add permission
      RoleInheritanceTestUtils.addAccess("WINDOW", template, "Sales Order");

      OBDal.getInstance().commitAndClose();
      role = OBDal.getInstance().get(Role.class, roleId);

      // Remove inherited permission for role (to simulate not propagated access)
      deleteInheritedWindowAccess(role);
      assertThat("Access not propagated for role", role.getADWindowAccessList(), hasSize(0));

      OBDal.getInstance().commitAndClose();
      role = OBDal.getInstance().get(Role.class, roleId);

      manager.recalculateAllAccessesForRole(role);
      OBDal.getInstance().commitAndClose();
      role = OBDal.getInstance().get(Role.class, roleId);
      template = OBDal.getInstance().get(Role.class, templateId);

      assertThat("There is a new access created with the recalculation",
          role.getADWindowAccessList(), hasSize(1));

      String[] expected = { "true", "true", templateId };
      String[] result = RoleInheritanceTestUtils.getAccessInfo("WINDOW", role, "Sales Order");
      assertThat("New access recalculated properly", expected, equalTo(result));

    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      // Delete roles
      RoleInheritanceTestUtils.deleteRole(role);
      RoleInheritanceTestUtils.deleteRole(template);

      OBDal.getInstance().commitAndClose();

      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test case to simulate the "Recalculate Permissions" process, launched for a template role
   */
  @Test
  public void recalculatePermissionsForTemplateRole() {
    Role template = null;
    Role role1 = null;
    Role role2 = null;
    try {
      OBContext.setAdminMode(true);
      template = RoleInheritanceTestUtils.createRole("template", RoleInheritanceTestUtils.CLIENT_ID,
          RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true, true);
      String templateId = template.getId();
      role1 = RoleInheritanceTestUtils.createRole("role1", RoleInheritanceTestUtils.CLIENT_ID,
          RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true, false);
      String role1Id = role1.getId();
      role2 = RoleInheritanceTestUtils.createRole("role2", RoleInheritanceTestUtils.CLIENT_ID,
          RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true, false);
      String role2Id = role2.getId();
      OBDal.getInstance().commitAndClose();

      template = OBDal.getInstance().get(Role.class, templateId);
      role1 = OBDal.getInstance().get(Role.class, role1Id);
      role2 = OBDal.getInstance().get(Role.class, role2Id);

      // Add inheritance
      RoleInheritanceTestUtils.addInheritance(role1, template, 10L);
      RoleInheritanceTestUtils.addInheritance(role2, template, 20L);
      OBDal.getInstance().commitAndClose();

      template = OBDal.getInstance().get(Role.class, templateId);
      // Add permission
      RoleInheritanceTestUtils.addAccess("WINDOW", template, "Sales Order");

      OBDal.getInstance().commitAndClose();
      role1 = OBDal.getInstance().get(Role.class, role1Id);
      role2 = OBDal.getInstance().get(Role.class, role2Id);
      // Remove inherited permission for roles (to simulate not propagated accesses)
      deleteInheritedWindowAccess(role1);
      deleteInheritedWindowAccess(role2);
      assertThat("Access not propagated for role 1", role1.getADWindowAccessList(), hasSize(0));
      assertThat("Access not propagated for role 2", role2.getADWindowAccessList(), hasSize(0));

      OBDal.getInstance().commitAndClose();
      template = OBDal.getInstance().get(Role.class, templateId);

      manager.recalculateAllAccessesFromTemplate(template);
      OBDal.getInstance().commitAndClose();
      role1 = OBDal.getInstance().get(Role.class, role1Id);
      role2 = OBDal.getInstance().get(Role.class, role2Id);
      template = OBDal.getInstance().get(Role.class, templateId);

      assertThat("There is a new access created with the recalculation for role1",
          role1.getADWindowAccessList(), hasSize(1));
      assertThat("There is a new access created with the recalculation for role2",
          role2.getADWindowAccessList(), hasSize(1));

      String[] expected = { "true", "true", templateId };
      String[] result = RoleInheritanceTestUtils.getAccessInfo("WINDOW", role1, "Sales Order");
      assertThat("New accesses recalculated properly for role 1", expected, equalTo(result));

      String[] result2 = RoleInheritanceTestUtils.getAccessInfo("WINDOW", role2, "Sales Order");
      assertThat("New accesses recalculated properly for role 2", expected, equalTo(result2));

    } finally {
      // Delete roles
      RoleInheritanceTestUtils.deleteRole(role1);
      RoleInheritanceTestUtils.deleteRole(role2);
      RoleInheritanceTestUtils.deleteRole(template);

      OBDal.getInstance().commitAndClose();

      OBContext.restorePreviousMode();
    }
  }

  private void deleteInheritedWindowAccess(Role role) {
    WindowAccess wa = role.getADWindowAccessList().get(0);
    wa.setInheritedFrom(null);
    role.getADWindowAccessList().remove(wa);
    OBDal.getInstance().remove(wa);
  }
}
