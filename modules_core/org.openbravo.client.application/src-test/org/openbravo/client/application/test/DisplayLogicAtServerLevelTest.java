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
 * All portions are Copyright (C) 2016-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.CachedPreference;
import org.openbravo.client.application.DynamicExpressionParser;
import org.openbravo.client.application.window.OBViewFieldHandler;
import org.openbravo.client.application.window.OBViewFieldHandler.OBViewField;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Tab;

/**
 * 
 * This class is used to test the correct behavior of the Display Logic Evaluated at Server Level
 * functionality
 *
 */
public class DisplayLogicAtServerLevelTest extends WeldBaseTest {
  private CachedPreference cachedPreference;
  private Tab tab;
  private OBViewField field;

  /**
   * Initializes the global variables for the rest of the tests
   */
  @Before
  public void initializeTest() {
    setSystemAdministratorContext();

    cachedPreference = org.openbravo.base.weld.WeldUtils
        .getInstanceFromStaticBeanManager(CachedPreference.class);
    tab = OBDal.getInstance().get(Tab.class, "270");

    OBViewFieldHandler handler = new OBViewFieldHandler();
    handler.setTab(tab);
    field = handler.new OBViewField();
  }

  /**
   * Tests that the replacement of the DisplayLogic at Server level works correctly
   */
  @Test
  public void replaceSystemPreferencesInDisplayLogic() {
    cachedPreference.addCachedPreference("enableNegativeStockCorrections");
    cachedPreference.setPreferenceValue("enableNegativeStockCorrections", "Y");
    cachedPreference.addCachedPreference("uomManagement");
    cachedPreference.setPreferenceValue("uomManagement", "Y");

    String displayLogicEvaluatedInServerExpression = "@uomManagement@ = 'Y' & @enableNegativeStockCorrections@ = 'Y'";
    String expectedTranslatedDisplayLogic = "'Y' = 'Y' & 'Y' = 'Y'";

    testTranslationOfDisplayLogic(displayLogicEvaluatedInServerExpression,
        expectedTranslatedDisplayLogic);
  }

  /**
   * Tests that the replacement of the DisplayLogic at Server level works correctly containing null
   * values
   */
  @Test
  public void replaceSystemPreferencesInDisplayLogicWithNullValue() {
    cachedPreference.addCachedPreference("enableNegativeStockCorrections");
    cachedPreference.setPreferenceValue("enableNegativeStockCorrections", "Y");

    String displayLogicEvaluatedInServerExpression = "@uomManagement@ = 'Y' & @enableNegativeStockCorrections@ = 'N'";
    String expectedTranslatedDisplayLogic = "'null' = 'Y' & 'Y' = 'N'";

    testTranslationOfDisplayLogic(displayLogicEvaluatedInServerExpression,
        expectedTranslatedDisplayLogic);
  }

  /**
   * Tests that the evaluation of the DisplayLogic at Server level works correctly
   */
  @Test
  public void evaluatePreferencesInDisplayLogic() {
    cachedPreference.addCachedPreference("enableNegativeStockCorrections");
    cachedPreference.setPreferenceValue("enableNegativeStockCorrections", "Y");
    cachedPreference.addCachedPreference("uomManagement");
    cachedPreference.setPreferenceValue("uomManagement", "Y");

    String displayLogicEvaluatedInServerExpression = "@uomManagement@ = 'Y' & @enableNegativeStockCorrections@ = 'Y'";

    boolean expectedEvaluatedDisplayLogic = true;

    testEvaluationOfDisplayLogic(displayLogicEvaluatedInServerExpression,
        expectedEvaluatedDisplayLogic);
  }

  /**
   * Tests that the evaluation of the DisplayLogic at Server level works correctly containing null
   * values
   */
  @Test
  public void evaluatePreferencesInDisplayLogicWithNullValue() {
    cachedPreference.addCachedPreference("enableNegativeStockCorrections");
    cachedPreference.setPreferenceValue("enableNegativeStockCorrections", "N");

    String displayLogicEvaluatedInServerExpression = "@uomManagement@ = 'Y' & @enableNegativeStockCorrections@ = 'Y'";

    boolean expectedEvaluatedDisplayLogic = false;

    testEvaluationOfDisplayLogic(displayLogicEvaluatedInServerExpression,
        expectedEvaluatedDisplayLogic);
  }

  private void testTranslationOfDisplayLogic(String displayLogicEvaluatedInServerExpression,
      String expectedTranslatedDisplayLogic) {
    String translatedDisplayLogic = DynamicExpressionParser
        .replaceSystemPreferencesInDisplayLogic(displayLogicEvaluatedInServerExpression);

    assertThat(
        "The translation from the Display Logic Evaluated at Server Level expression was different than expected: ",
        translatedDisplayLogic, equalTo(expectedTranslatedDisplayLogic));
  }

  private void testEvaluationOfDisplayLogic(String displayLogicEvaluatedInServerExpression,
      boolean expectedEvaluatedDisplayLogic) {
    Class<?> clazz = field.getClass();
    Method evaluateDisplayLogicAtServerLevel;
    try {
      evaluateDisplayLogicAtServerLevel = clazz
          .getDeclaredMethod("evaluateDisplayLogicAtServerLevel", String.class, String.class);
      boolean originallyAccessible = evaluateDisplayLogicAtServerLevel.canAccess(field);
      evaluateDisplayLogicAtServerLevel.setAccessible(true);
      boolean evaluatedDisplayLogic = (boolean) evaluateDisplayLogicAtServerLevel.invoke(field,
          displayLogicEvaluatedInServerExpression, "0");
      evaluateDisplayLogicAtServerLevel.setAccessible(originallyAccessible);
      assertThat(
          "The result of the evaluation of the Display Logic Evaluated at Server Level expression was different than expected: ",
          evaluatedDisplayLogic, equalTo(expectedEvaluatedDisplayLogic));
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
