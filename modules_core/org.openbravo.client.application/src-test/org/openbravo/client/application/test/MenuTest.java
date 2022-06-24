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
 * All portions are Copyright (C) 2010-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.test;

import static org.junit.Assert.assertFalse;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.MenuManager;
import org.openbravo.client.application.MenuManager.MenuOption;

/**
 * Tests the reading of the menu in memory
 * 
 * @author iperdomo
 */
public class MenuTest extends WeldBaseTest {
  private static final Logger log = LogManager.getLogger();

  @Inject
  private MenuManager menuManager;

  /**
   * Test reading the menu
   */
  @Test
  public void testSystemAdministratorMenu() throws Exception {
    setSystemAdministratorContext();
    final MenuManager.MenuOption rootMenuOption = menuManager.getMenu();
    dumpMenuOption(rootMenuOption, 0);
    assertFalse(menuManager.getSelectableMenuOptions().isEmpty());
  }

  /**
   * Test reading the menu
   */
  @Test
  public void testOpenbravoAdminMenu() throws Exception {
    setTestAdminContext();
    final MenuManager.MenuOption rootMenuOption = menuManager.getMenu();
    dumpMenuOption(rootMenuOption, 0);
    assertFalse(menuManager.getSelectableMenuOptions().isEmpty());
  }

  private void dumpMenuOption(MenuOption menuOption, int level) {
    if (!log.isDebugEnabled()) {
      return;
    }
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < level; i++) {
      sb.append(">");
    }
    sb.append(menuOption.getLabel());
    sb.append(" (" + menuOption.getType() + "): " + menuOption.getId());
    log.debug(sb.toString());
    for (MenuOption childOption : menuOption.getChildren()) {
      dumpMenuOption(childOption, level + 1);
    }
  }
}
