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

import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.window.StandardWindowComponent;
import org.openbravo.client.kernel.ComponentGenerator;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.test.base.mock.HttpServletRequestMock;

/**
 * Tests generation of the javascript for standard windows
 * 
 * @author iperdomo
 */
public class StandardWindowTest extends WeldBaseTest {
  private static final Logger log = LogManager.getLogger();
  private static final String USER_INTERFACE_APP_MOD = "9BA0836A3CD74EE4AB48753A47211BCC";

  /**
   * Tests generating the javascript for all windows, printing one of them.
   */
  @Test
  public void testStandardViewGeneration() throws Exception {
    setSystemAdministratorContext();

    // set user interface application module in dev to enable jslint
    Module uiapp = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = uiapp.isInDevelopment();
    if (!wasInDev) {
      uiapp.setInDevelopment(true);
    }

    String errorMsg = "";
    try {
      HttpServletRequestMock.setRequestMockInRequestContext();

      List<Window> allWindows = OBDal.getInstance().createQuery(Window.class, "").list();
      int i = 0;
      setTestLogAppenderLevel(Level.WARN);
      List<String> errors = new ArrayList<>();
      for (Window window : allWindows) {
        if (hasAtLeastOneActiveTab(window)) {
          log.info("window {} of {}: {}", ++i, allWindows.size(), window.getName());
          try {
            generateForWindow(window);
          } catch (Throwable t) {
            log.error(t.getMessage(), t);
          }
        }
        for (String error : getTestLogAppender().getMessages(Level.ERROR)) {
          errors.add("ERROR - Window " + window.getName() + " - " + window.getId() + ": " + error);
        }

        for (String warn : getTestLogAppender().getMessages(Level.WARN)) {
          errors.add("WARN - Window " + window.getName() + " - " + window.getId() + ": " + warn);
        }
        getTestLogAppender().reset();
      }
      log.info("Generated {} windows", allWindows.size());
      if (!errors.isEmpty()) {
        log.error("{} errors detected:", errors.size());
        for (String error : errors) {
          log.error(error);
          errorMsg += error + "\n";
        }
      }
    } finally {
      if (!wasInDev) {
        uiapp.setInDevelopment(false);
      }
    }

    assertThat("Errors generating windows", errorMsg, isEmptyString());
  }

  /**
   * Tests generating the javascript for one window to analyze problems.
   */
  @Ignore
  @Test
  public void _testOneStandardViewGeneration() throws Exception {
    setSystemAdministratorContext();
    generateForWindow(OBDal.getInstance().get(Window.class, "1005400002"));
  }

  private void generateForWindow(Window window) {
    final StandardWindowComponent component = super.getWeldComponent(StandardWindowComponent.class);
    component.setWindow(window);
    ComponentGenerator.getInstance().generate(component);
  }

  private boolean hasAtLeastOneActiveTab(Window window) {
    for (Tab tab : window.getADTabList()) {
      if (tab.isActive()) {
        return true;
      }
    }
    return false;
  }
}
