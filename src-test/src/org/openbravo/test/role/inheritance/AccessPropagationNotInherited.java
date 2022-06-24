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
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;

/**
 * Test case to check that non inherited accesses are not affected by access propagation
 * 
 * We start having Role "role" which inherits from Role "template"
 * 
 * We add access A1 for "role", before adding the same access A1 for "template"
 * 
 * If we update access A1 for "template" this change must not affect A1 access for "role" because it
 * has not been inherited
 * 
 */
public class AccessPropagationNotInherited extends WeldBaseTest {
  private final List<String> ORGANIZATIONS = Arrays.asList("F&B España - Región Norte");
  private final List<String> WINDOWS = Arrays.asList("Sales Invoice");
  private final List<String> TABS = Arrays.asList("Bank Account");
  private final List<String> FIELDS = Arrays.asList("Business Partner Category");
  private final List<String> REPORTS = Arrays.asList("Alert Process");
  private final List<String> FORMS = Arrays.asList("About");
  private final List<String> WIDGETS = Arrays.asList("Best Sellers");
  private final List<String> VIEWS = Arrays.asList("OBUIAPP_AlertManagement");
  private final List<String> PROCESSES = Arrays.asList("Create Purchase Order Lines");
  private final List<String> TABLES = Arrays.asList("C_Order");
  private final List<String> ALERTS = Arrays.asList("Alert Taxes: Inversión del Sujeto Pasivo");
  private final List<String> PREFERENCES = Arrays.asList("AllowAttachment");

  private final List<List<String>> ACCESSES = Arrays.asList(ORGANIZATIONS, WINDOWS, TABS, FIELDS,
      REPORTS, FORMS, WIDGETS, VIEWS, PROCESSES, TABLES, ALERTS, PREFERENCES);
  private static int testCounter = 0;

  /** defines the values the parameter will take. */
  @Rule
  public ParameterCdiTestRule<String> parameterValuesRule = new ParameterCdiTestRule<String>(
      RoleInheritanceTestUtils.ACCESS_NAMES);

  /** this field will take the values defined by parameterValuesRule field. */
  private @ParameterCdiTest String parameter;

  /**
   * Test case to check that non inherited accesses are not affected by access propagation.
   */
  @Test
  public void notInheritedAccessNotAffectedByPropagation() {
    Role role = null;
    Role template = null;
    try {
      OBContext.setAdminMode(true);
      // Create roles
      role = RoleInheritanceTestUtils.createRole("role", RoleInheritanceTestUtils.CLIENT_ID,
          RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true, false);
      String roleId = role.getId();
      template = RoleInheritanceTestUtils.createRole("template", RoleInheritanceTestUtils.CLIENT_ID,
          RoleInheritanceTestUtils.ASTERISK_ORG_ID, " C", true, true);
      String templateId = template.getId();

      // Add inheritance
      RoleInheritanceTestUtils.addInheritance(role, template, 10L);

      OBDal.getInstance().commitAndClose();
      role = OBDal.getInstance().get(Role.class, roleId);
      template = OBDal.getInstance().get(Role.class, templateId);

      List<String> accesses = ACCESSES.get(testCounter);
      // Add accesses
      RoleInheritanceTestUtils.addAccess(parameter, role, accesses.get(0));
      RoleInheritanceTestUtils.addAccess(parameter, template, accesses.get(0));

      OBDal.getInstance().commitAndClose();
      role = OBDal.getInstance().get(Role.class, roleId);
      template = OBDal.getInstance().get(Role.class, templateId);

      String[] expected = { accesses.get(0), "" };
      String[] result = RoleInheritanceTestUtils.getOrderedAccessNames(parameter, role);
      assertThat("New access has not affected non inherited access", result, equalTo(expected));

      boolean value = false;
      boolean active = false;
      if (parameter.equals("ALERT") || parameter.equals("PREFERENCE")) {
        value = true;
      }
      // Perform an update in the access of the parent
      RoleInheritanceTestUtils.updateAccess(parameter, template, accesses.get(0), value, active);
      OBDal.getInstance().commitAndClose();

      role = OBDal.getInstance().get(Role.class, roleId);
      template = OBDal.getInstance().get(Role.class, templateId);

      String editedValue = !value + "";
      String isActive = !active + "";
      if (parameter.equals("REPORT") || parameter.equals("FORM") || parameter.equals("WIDGET")
          || parameter.equals("VIEW") || parameter.equals("PROCESS")) {
        // Accesses for Report, Form, Widget, View and Process just have the active flag
        editedValue = "";
      }

      String[] expected2 = { editedValue, isActive, "" };
      String[] result2 = RoleInheritanceTestUtils.getAccessInfo(parameter, role, accesses.get(0));
      assertThat("Updated access has not affected non inherited access", result2,
          equalTo(expected2));

      // NOTE: At this point, if we remove the access of "role", it will not inherit automatically
      // the access from the template because is not possible to handle the removal and the insert
      // of the record at the same time inside the event handler. So, in this case the user should
      // call the "Recalculate Process" after removing the non inherited access.

      RoleInheritanceTestUtils.removeAccesses(parameter, template);
      RoleInheritanceTestUtils.removeAccesses(parameter, role);
      OBDal.getInstance().flush();
      testCounter++;

    } finally {
      // Delete roles
      RoleInheritanceTestUtils.deleteRole(role);
      RoleInheritanceTestUtils.deleteRole(template);

      OBDal.getInstance().commitAndClose();

      OBContext.restorePreviousMode();
    }
  }
}
