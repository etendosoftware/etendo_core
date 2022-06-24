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
 * All portions are Copyright (C) 2015-2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.role.inheritance;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;

/**
 * Test case for horizontal inheritance
 * 
 * Role A inherits from T1 (sequence 10), T2 (sequence 20) and T3 (sequence 30)
 * 
 * T1 Accesses {A3, A4} , T2 Accesses {A1, A2}, T3 Accesses {A0, A2, A3}
 * 
 * With this settings, Role A Accesses must be {A0(T3), A1(T2), A2(T3), A3(T3), A4(T1)}
 * 
 * After removing the inheritance of T3, access for Role A must be {A1(T2), A2(T2), A3(T1), A4(T1)}
 */
public class HorizontalInheritance extends WeldBaseTest {
  private final List<String> ORGANIZATIONS = Arrays.asList("F&B España - Región Norte",
      "F&B España - Región Sur", "F&B España, S.A", "F&B International Group", "F&B US East Coast");
  private final List<String> WINDOWS = Arrays.asList("Business Partner", "Purchase Invoice",
      "Purchase Order", "Sales Invoice", "Sales Order");
  private final List<String> TABS = Arrays.asList("Bank Account", "Basic Discount", "Contact",
      "Customer", "Employee");
  private final List<String> FIELDS = Arrays.asList("Business Partner Category", "Commercial Name",
      "Credit Line Limit", "Description", "URL");
  private final List<String> REPORTS = Arrays.asList("Alert Process", "Create Variants",
      "Journal Entries Report", "Print orders process", "Set as Ready");
  private final List<String> FORMS = Arrays.asList("About", "Heartbeat", "Logout", "Menu",
      "Payment Execution");
  private final List<String> WIDGETS = Arrays.asList("Best Sellers", "Invoices to collect",
      "Motion Chart", "Planet", "Twitter");
  private final List<String> VIEWS = Arrays.asList("OBUIAPP_AlertManagement",
      RoleInheritanceTestUtils.DUMMY_VIEW_IMPL_NAME);
  private final List<String> PROCESSES = Arrays.asList("Create Purchase Order Lines",
      "Grant Portal Access", "Manage Variants", "Modify Payment In Plan",
      "Process Cost Adjustment");
  private final List<String> TABLES = Arrays.asList("AD_User", "C_Order", "FIN_Payment",
      "M_Warehouse", "OBUIAPP_Note");
  private final List<String> ALERTS = Arrays.asList("Alert Taxes: Inversión del Sujeto Pasivo",
      "CUSTOMER WITHOUT ACCOUNTING", "Process Execution Failed", "Updates Available",
      "Wrong Purchase Order Payment Plan");
  private final List<String> PREFERENCES = Arrays.asList("AllowAttachment", "AllowDelete",
      "AllowMultiTab", "OBSERDS_CSVTextEncoding", "StartPage");

  private final List<List<String>> ACCESSES = Arrays.asList(ORGANIZATIONS, WINDOWS, TABS, FIELDS,
      REPORTS, FORMS, WIDGETS, VIEWS, PROCESSES, TABLES, ALERTS, PREFERENCES);
  private static int testCounter = 0;

  /** defines the values the parameter will take. */
  @Rule
  public ParameterCdiTestRule<String> parameterValuesRule = new ParameterCdiTestRule<String>(
      RoleInheritanceTestUtils.ACCESS_NAMES);

  /** this field will take the values defined by parameterValuesRule field. */
  private @ParameterCdiTest String parameter;

  @Before
  public void createDummyView() {
    RoleInheritanceTestUtils.createDummyView();
  }

  @After
  public void removeDummyView() {
    RoleInheritanceTestUtils.removeDummyView();
  }

  /**
   * Test case for horizontal inheritance
   */
  @Test
  public void createBasicHorizontalInheritance() {
    Role template1 = null;
    Role template2 = null;
    Role template3 = null;
    Role inheritedRole = null;
    try {
      OBContext.setAdminMode(true);
      // Create roles
      template1 = RoleInheritanceTestUtils.createRole("template1",
          RoleInheritanceTestUtils.CLIENT_ID, RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true,
          true);
      String template1Id = template1.getId();
      template2 = RoleInheritanceTestUtils.createRole("template2",
          RoleInheritanceTestUtils.CLIENT_ID, RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true,
          true);
      String template2Id = template2.getId();
      template3 = RoleInheritanceTestUtils.createRole("template3",
          RoleInheritanceTestUtils.CLIENT_ID, RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true,
          true);
      String template3Id = template3.getId();
      inheritedRole = RoleInheritanceTestUtils.createRole("inheritedRole",
          RoleInheritanceTestUtils.CLIENT_ID, RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true,
          false);
      String inheritedRoleId = inheritedRole.getId();

      List<String> accesses = ACCESSES.get(testCounter);
      if (!parameter.equals("VIEW")) {
        RoleInheritanceTestUtils.addAccess(parameter, template1, accesses.get(3));
        RoleInheritanceTestUtils.addAccess(parameter, template1, accesses.get(4));
        RoleInheritanceTestUtils.addAccess(parameter, template2, accesses.get(1));
        RoleInheritanceTestUtils.addAccess(parameter, template2, accesses.get(2));
        RoleInheritanceTestUtils.addAccess(parameter, template3, accesses.get(3));
        RoleInheritanceTestUtils.addAccess(parameter, template3, accesses.get(2));
        RoleInheritanceTestUtils.addAccess(parameter, template3, accesses.get(0));
      } else {
        RoleInheritanceTestUtils.addAccess(parameter, template1, accesses.get(0));
        RoleInheritanceTestUtils.addAccess(parameter, template2, accesses.get(1));
        RoleInheritanceTestUtils.addAccess(parameter, template3, accesses.get(0));
        RoleInheritanceTestUtils.addAccess(parameter, template3, accesses.get(1));
      }
      OBDal.getInstance().commitAndClose();

      template1 = OBDal.getInstance().get(Role.class, template1Id);
      template2 = OBDal.getInstance().get(Role.class, template2Id);
      template3 = OBDal.getInstance().get(Role.class, template3Id);
      inheritedRole = OBDal.getInstance().get(Role.class, inheritedRoleId);

      // Save Inheritances
      RoleInheritanceTestUtils.addInheritance(inheritedRole, template1, 10L);
      RoleInheritanceTestUtils.addInheritance(inheritedRole, template2, 20L);
      RoleInheritanceTestUtils.addInheritance(inheritedRole, template3, 30L);
      OBDal.getInstance().commitAndClose();

      template1 = OBDal.getInstance().get(Role.class, template1Id);
      template2 = OBDal.getInstance().get(Role.class, template2Id);
      template3 = OBDal.getInstance().get(Role.class, template3Id);
      inheritedRole = OBDal.getInstance().get(Role.class, inheritedRoleId);

      String[] expected;
      if (!parameter.equals("VIEW")) {
        String[] expectedAccesses = { accesses.get(0), template3Id, accesses.get(1), template2Id,
            accesses.get(2), template3Id, accesses.get(3), template3Id, accesses.get(4),
            template1Id };
        expected = expectedAccesses;
      } else {
        String[] expectedAccesses = { accesses.get(0), template3Id, accesses.get(1), template3Id };
        expected = expectedAccesses;
      }
      String[] result = RoleInheritanceTestUtils.getOrderedAccessNames(parameter, inheritedRole);
      assertThat("Inheritances added. Accesses have been inherited", result, equalTo(expected));

      // Delete Inheritance
      RoleInheritanceTestUtils.removeInheritance(inheritedRole, template3);
      OBDal.getInstance().commitAndClose();

      template1 = OBDal.getInstance().get(Role.class, template1Id);
      template2 = OBDal.getInstance().get(Role.class, template2Id);
      template3 = OBDal.getInstance().get(Role.class, template3Id);
      inheritedRole = OBDal.getInstance().get(Role.class, inheritedRoleId);

      String[] expected2;
      if (!parameter.equals("VIEW")) {
        String[] expectedAccesses = { accesses.get(1), template2Id, accesses.get(2), template2Id,
            accesses.get(3), template1Id, accesses.get(4), template1Id };
        expected2 = expectedAccesses;
      } else {
        String[] expectedAccesses = { accesses.get(0), template1Id, accesses.get(1), template2Id };
        expected2 = expectedAccesses;
      }
      result = RoleInheritanceTestUtils.getOrderedAccessNames(parameter, inheritedRole);
      assertThat("Inheritance removed. Related accesses have been removed", result,
          equalTo(expected2));

      RoleInheritanceTestUtils.removeAccesses(parameter, template1);
      RoleInheritanceTestUtils.removeAccesses(parameter, template2);
      RoleInheritanceTestUtils.removeAccesses(parameter, template3);
      RoleInheritanceTestUtils.removeAccesses(parameter, inheritedRole);
      OBDal.getInstance().flush();

      testCounter++;

    } finally {
      // Delete roles
      RoleInheritanceTestUtils.deleteRole(inheritedRole);
      RoleInheritanceTestUtils.deleteRole(template1);
      RoleInheritanceTestUtils.deleteRole(template2);
      RoleInheritanceTestUtils.deleteRole(template3);

      OBDal.getInstance().commitAndClose();

      OBContext.restorePreviousMode();
    }
  }
}
