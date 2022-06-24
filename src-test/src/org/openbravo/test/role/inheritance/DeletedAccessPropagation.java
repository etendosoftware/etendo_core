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
 * Test case for deleted access propagation
 * 
 * We have a role which inherits from three different templates. All these templates have permission
 * to the same particular access.
 * 
 * We are removing the access for each template starting from the one with highest priority. Thus
 * the inherited access for the role will be updated with every deletion.
 * 
 * 
 */
public class DeletedAccessPropagation extends WeldBaseTest {
  private final List<String> ORGANIZATIONS = Arrays.asList("F&B España - Región Norte",
      "F&B España - Región Sur");
  private final List<String> WINDOWS = Arrays.asList("Sales Invoice", "Sales Order");
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
   * Test case for deleted access propagation
   */
  @Test
  public void checkPropagationOfDeletedAccess() {
    Role role = null;
    Role template1 = null;
    Role template2 = null;
    Role template3 = null;
    try {
      OBContext.setAdminMode(true);
      // Create roles
      role = RoleInheritanceTestUtils.createRole("role", RoleInheritanceTestUtils.CLIENT_ID,
          RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true, false);
      String roleId = role.getId();
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

      List<String> accesses = ACCESSES.get(testCounter);
      // Add accesses
      RoleInheritanceTestUtils.addAccess(parameter, template1, accesses.get(0));
      RoleInheritanceTestUtils.addAccess(parameter, template1, accesses.get(1));
      RoleInheritanceTestUtils.addAccess(parameter, template2, accesses.get(0));
      RoleInheritanceTestUtils.addAccess(parameter, template3, accesses.get(0));

      // Add inheritances
      RoleInheritanceTestUtils.addInheritance(role, template1, 10L);
      RoleInheritanceTestUtils.addInheritance(role, template2, 20L);
      RoleInheritanceTestUtils.addInheritance(role, template3, 30L);
      OBDal.getInstance().commitAndClose();

      String[] expected = { accesses.get(0), template3Id, accesses.get(1), template1Id };
      String[] result = RoleInheritanceTestUtils.getOrderedAccessNames(parameter, role);
      assertThat("Inherited access created properly", result, equalTo(expected));

      // Remove window access for template 3
      template3 = OBDal.getInstance().get(Role.class, template3Id);
      RoleInheritanceTestUtils.removeAccesses(parameter, template3);
      OBDal.getInstance().commitAndClose();

      String[] expected2 = { accesses.get(0), template2Id, accesses.get(1), template1Id };
      String[] result2 = RoleInheritanceTestUtils.getOrderedAccessNames(parameter, role);
      assertThat("Inherited access updated properly after first removal", result2,
          equalTo(expected2));

      // Remove window access for template 2
      template2 = OBDal.getInstance().get(Role.class, template2Id);
      RoleInheritanceTestUtils.removeAccesses(parameter, template2);
      OBDal.getInstance().commitAndClose();

      String[] expected3 = { accesses.get(0), template1Id, accesses.get(1), template1Id };
      String[] result3 = RoleInheritanceTestUtils.getOrderedAccessNames(parameter, role);
      assertThat("Inherited access updated properly after second removal", result3,
          equalTo(expected3));
      OBDal.getInstance().commitAndClose();

      // Remove window access for template 1
      template1 = OBDal.getInstance().get(Role.class, template1Id);
      RoleInheritanceTestUtils.removeAccesses(parameter, template1);
      OBDal.getInstance().commitAndClose();

      role = OBDal.getInstance().get(Role.class, roleId);
      template1 = OBDal.getInstance().get(Role.class, template1Id);
      template2 = OBDal.getInstance().get(Role.class, template2Id);
      template3 = OBDal.getInstance().get(Role.class, template3Id);

      String[] expected4 = {};
      String[] result4 = RoleInheritanceTestUtils.getOrderedAccessNames(parameter, role);
      assertThat("Inherited access updated properly after third removal", result4,
          equalTo(expected4));

      testCounter++;

    } finally {
      // Delete roles
      RoleInheritanceTestUtils.deleteRole(role);
      RoleInheritanceTestUtils.deleteRole(template1);
      RoleInheritanceTestUtils.deleteRole(template2);
      RoleInheritanceTestUtils.deleteRole(template3);

      OBDal.getInstance().commitAndClose();

      OBContext.restorePreviousMode();
    }
  }
}
