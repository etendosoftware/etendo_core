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
 * Test case for vertical inheritance
 * 
 * Role B inherits from Role A and Role C from Role B : A -&gt; B -&gt; C
 * 
 * A Access {A1} , B Access {A2}
 * 
 * With this settings, Role B Accesses must be {A1(A), A2} and Role C Accesses must be {A1(B),
 * A2(B)}
 * 
 */
public class VerticalInheritance extends WeldBaseTest {
  private final List<String> ORGANIZATIONS = Arrays.asList("F&B España - Región Norte",
      "F&B España - Región Sur");
  private final List<String> WINDOWS = Arrays.asList("Purchase Order", "Sales Order");
  private final List<String> TABS = Arrays.asList("Bank Account", "Basic Discount");
  private final List<String> FIELDS = Arrays.asList("Business Partner Category", "Commercial Name");
  private final List<String> REPORTS = Arrays.asList("Alert Process", "Create Variants");
  private final List<String> FORMS = Arrays.asList("About", "Heartbeat");
  private final List<String> WIDGETS = Arrays.asList("Best Sellers", "Invoices to collect");
  private final List<String> VIEWS = Arrays.asList("OBUIAPP_AlertManagement",
      RoleInheritanceTestUtils.DUMMY_VIEW_IMPL_NAME);
  private final List<String> PROCESSES = Arrays.asList("Create Purchase Order Lines",
      "Grant Portal Access");
  private final List<String> TABLES = Arrays.asList("AD_User", "C_Order");
  private final List<String> ALERTS = Arrays.asList("Alert Taxes: Inversión del Sujeto Pasivo",
      "CUSTOMER WITHOUT ACCOUNTING");
  private final List<String> PREFERENCES = Arrays.asList("AllowAttachment", "AllowDelete");

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
   * Test case for vertical inheritance
   */
  @Test
  public void createBasicVerticalInheritance() {
    Role roleA = null;
    Role roleB = null;
    Role roleC = null;
    try {
      OBContext.setAdminMode(true);
      // Create roles
      roleA = RoleInheritanceTestUtils.createRole("roleA", RoleInheritanceTestUtils.CLIENT_ID,
          RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true, true);
      String roleAId = roleA.getId();
      roleB = RoleInheritanceTestUtils.createRole("roleB", RoleInheritanceTestUtils.CLIENT_ID,
          RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true, true);
      String roleBId = roleB.getId();
      roleC = RoleInheritanceTestUtils.createRole("roleC", RoleInheritanceTestUtils.CLIENT_ID,
          RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true, false);
      String roleCId = roleC.getId();

      List<String> accesses = ACCESSES.get(testCounter);
      // Add window accesses for template roles
      RoleInheritanceTestUtils.addAccess(parameter, roleA, accesses.get(0));
      RoleInheritanceTestUtils.addAccess(parameter, roleB, accesses.get(1));

      OBDal.getInstance().commitAndClose();

      roleA = OBDal.getInstance().get(Role.class, roleAId);
      roleB = OBDal.getInstance().get(Role.class, roleBId);
      roleC = OBDal.getInstance().get(Role.class, roleCId);

      // Add inheritances
      RoleInheritanceTestUtils.addInheritance(roleB, roleA, 10L);
      RoleInheritanceTestUtils.addInheritance(roleC, roleB, 20L);

      OBDal.getInstance().commitAndClose();

      roleA = OBDal.getInstance().get(Role.class, roleAId);
      roleB = OBDal.getInstance().get(Role.class, roleBId);
      roleC = OBDal.getInstance().get(Role.class, roleCId);

      String[] expected = { accesses.get(0), roleAId, accesses.get(1), "" };
      String[] result = RoleInheritanceTestUtils.getOrderedAccessNames(parameter, roleB);
      assertThat("Accesses inherited for role B ", result, equalTo(expected));

      String[] expected2 = { accesses.get(0), roleBId, accesses.get(1), roleBId };
      result = RoleInheritanceTestUtils.getOrderedAccessNames(parameter, roleC);
      assertThat("Accesses inherited for role C ", result, equalTo(expected2));

      RoleInheritanceTestUtils.removeAccesses(parameter, roleA);
      RoleInheritanceTestUtils.removeAccesses(parameter, roleB);
      RoleInheritanceTestUtils.removeAccesses(parameter, roleC);
      OBDal.getInstance().flush();
      testCounter++;

    } finally {
      // Delete roles
      RoleInheritanceTestUtils.deleteRole(roleC);
      RoleInheritanceTestUtils.deleteRole(roleB);
      RoleInheritanceTestUtils.deleteRole(roleA);

      OBDal.getInstance().commitAndClose();

      OBContext.restorePreviousMode();
    }
  }
}
