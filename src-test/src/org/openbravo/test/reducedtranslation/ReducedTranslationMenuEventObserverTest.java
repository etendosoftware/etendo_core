/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.test.reducedtranslation;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.ui.Menu;
import org.openbravo.test.base.TestConstants;

/**
 * Tests the org.openbravo.event.ADMenuEventHandler event observer
 */
public class ReducedTranslationMenuEventObserverTest extends WeldBaseTest {

  @Before
  @After
  public void doNotIncludeApplicationDictionaryInReducedTranslation() {
    updateApplicationDictionaryMenuTranslationStrategy(
        ReducedTrlTestConstants.EXCLUDE_FROM_REDUCED_TRANSLATION);
  }

  @Before
  public void setModuleInDevelopmentYes() {
    setModuleInDevelopment(TestConstants.Modules.ID_CORE, true);
  }

  @After
  public void setModuleInDevelopmentNo() {
    setModuleInDevelopment(TestConstants.Modules.ID_CORE, false);
  }

  private void setModuleInDevelopment(final String moduleId, final boolean isInDevelopment) {
    try {
      OBContext.setAdminMode(false);
      final Module module = OBDal.getInstance().get(Module.class, moduleId);
      module.setInDevelopment(isInDevelopment);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void updateApplicationDictionaryMenuTranslationStrategy(String translationStrategy) {
    try {
      OBContext.setAdminMode(false);
      final Menu adMenu = OBDal.getInstance()
          .get(Menu.class, ReducedTrlTestConstants.APPLICATION_DICTIONARY_MENU_ID);
      adMenu.setTranslationStrategy(translationStrategy);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void childEntriesAreUpdated() {
    try {
      OBContext.setAdminMode(false);
      updateApplicationDictionaryMenuTranslationStrategy(null);
      assertThat("Child menu entry has been updated",
          OBDal.getInstance()
              .get(Menu.class, ReducedTrlTestConstants.ELEMENT_MENU_ID)
              .getTranslationStrategy(),
          equalTo(null));

      updateApplicationDictionaryMenuTranslationStrategy(
          ReducedTrlTestConstants.EXCLUDE_FROM_REDUCED_TRANSLATION);
      assertThat("Child menu entry has been updated",
          OBDal.getInstance()
              .get(Menu.class, ReducedTrlTestConstants.ELEMENT_MENU_ID)
              .getTranslationStrategy(),
          equalTo(ReducedTrlTestConstants.EXCLUDE_FROM_REDUCED_TRANSLATION));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void parentEntriesAreNotUpdated() {
    try {
      OBContext.setAdminMode(false);
      final Menu elementMenu = OBDal.getInstance()
          .get(Menu.class, ReducedTrlTestConstants.ELEMENT_MENU_ID);
      elementMenu.setTranslationStrategy(null);
      OBDal.getInstance().flush();
      assertThat("This menu entry has been updated",
          OBDal.getInstance()
              .get(Menu.class, ReducedTrlTestConstants.ELEMENT_MENU_ID)
              .getTranslationStrategy(),
          equalTo(null));
      assertThat("Parent menu entry has not been updated",
          OBDal.getInstance()
              .get(Menu.class, ReducedTrlTestConstants.APPLICATION_DICTIONARY_MENU_ID)
              .getTranslationStrategy(),
          equalTo(ReducedTrlTestConstants.EXCLUDE_FROM_REDUCED_TRANSLATION));

      elementMenu.setTranslationStrategy(ReducedTrlTestConstants.EXCLUDE_FROM_REDUCED_TRANSLATION);
      OBDal.getInstance().flush();
      assertThat("This menu entry has been updated",
          OBDal.getInstance()
              .get(Menu.class, ReducedTrlTestConstants.ELEMENT_MENU_ID)
              .getTranslationStrategy(),
          equalTo(ReducedTrlTestConstants.EXCLUDE_FROM_REDUCED_TRANSLATION));
      assertThat("Parent menu entry has not been updated",
          OBDal.getInstance()
              .get(Menu.class, ReducedTrlTestConstants.APPLICATION_DICTIONARY_MENU_ID)
              .getTranslationStrategy(),
          equalTo(ReducedTrlTestConstants.EXCLUDE_FROM_REDUCED_TRANSLATION));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
