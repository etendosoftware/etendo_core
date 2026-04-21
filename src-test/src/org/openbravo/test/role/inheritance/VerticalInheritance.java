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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
  private static final List<String> ORGANIZATIONS = Arrays.asList("F&B España - Región Norte",
      "F&B España - Región Sur");
  private static final List<String> WINDOWS = Arrays.asList("Purchase Order", "Sales Order");
  private static final List<String> TABS = Arrays.asList("Bank Account", "Basic Discount");
  private static final List<String> FIELDS = Arrays.asList("Business Partner Category", "Commercial Name");
  private static final List<String> REPORTS = Arrays.asList("Alert Process", "Create Variants");
  private static final List<String> FORMS = Arrays.asList("About", "Heartbeat");
  private static final List<String> WIDGETS = Arrays.asList("Best Sellers", "Invoices to collect");
  private static final List<String> VIEWS = Arrays.asList("OBUIAPP_AlertManagement",
      RoleInheritanceTestUtils.DUMMY_VIEW_IMPL_NAME);
  private static final List<String> PROCESSES = Arrays.asList("Create Purchase Order Lines",
      "Grant Portal Access");
  private static final List<String> TABLES = Arrays.asList("AD_User", "C_Order");
  private static final List<String> ALERTS = Arrays.asList("Alert Taxes: Inversión del Sujeto Pasivo",
      "CUSTOMER WITHOUT ACCOUNTING");
  private static final List<String> PREFERENCES = Arrays.asList("AllowAttachment", "AllowDelete");

  private static final List<List<String>> ACCESSES = Arrays.asList(ORGANIZATIONS, WINDOWS, TABS, FIELDS,
      REPORTS, FORMS, WIDGETS, VIEWS, PROCESSES, TABLES, ALERTS, PREFERENCES);

  @Override
  protected void beforeTestExecution(ExtensionContext context) {
    super.beforeTestExecution(context);
    RoleInheritanceTestUtils.createDummyView();
  }

  @Override
  protected void afterTestExecution(ExtensionContext context) {
    RoleInheritanceTestUtils.removeDummyView();
    super.afterTestExecution(context);
  }

  /**
   * Test case for vertical inheritance
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("accessParameters")
  public void createBasicVerticalInheritance(String parameter, List<String> accesses) {
    Role roleA = null;
    Role roleB = null;
    Role roleC = null;
    try {
      OBContext.setAdminMode(true);
      // Create roles with unique names to avoid constraint violations between parameterized runs
      String suffix = "_" + parameter + "_" + System.nanoTime();
      roleA = RoleInheritanceTestUtils.createRole("roleA" + suffix, RoleInheritanceTestUtils.CLIENT_ID,
          RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true, true);
      String roleAId = roleA.getId();
      roleB = RoleInheritanceTestUtils.createRole("roleB" + suffix, RoleInheritanceTestUtils.CLIENT_ID,
          RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true, true);
      String roleBId = roleB.getId();
      roleC = RoleInheritanceTestUtils.createRole("roleC" + suffix, RoleInheritanceTestUtils.CLIENT_ID,
          RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true, false);
      String roleCId = roleC.getId();

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

    } finally {
      // Delete roles
      RoleInheritanceTestUtils.deleteRole(roleC);
      RoleInheritanceTestUtils.deleteRole(roleB);
      RoleInheritanceTestUtils.deleteRole(roleA);

      OBDal.getInstance().commitAndClose();

      OBContext.restorePreviousMode();
    }
  }

  private static Stream<Arguments> accessParameters() {
    return IntStream.range(0, RoleInheritanceTestUtils.ACCESS_NAMES.size())
        .mapToObj(index -> Arguments.of(RoleInheritanceTestUtils.ACCESS_NAMES.get(index),
            ACCESSES.get(index)));
  }
}
