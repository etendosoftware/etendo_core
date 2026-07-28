/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.test.generalsetup.enterprise.organization;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetupAccountingContext;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetupAccountingHandler;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetupAccountingResult;

/**
 * Verifies priority defaults for accounting setup handlers used by Initial Organization Setup.
 */
public class InitialOrgSetupAccountingHookSelectionTest {

  /**
   * Verifies that handlers without an explicit priority use the default ordering value.
   */
  @Test
  public void accountingHandlerHasDefaultPriorityForStableHookSelection() {
    InitialOrgSetupAccountingHandler handler = new InitialOrgSetupAccountingHandler() {
      @Override
      public boolean applies(InitialOrgSetupAccountingContext context) {
        return false;
      }

      @Override
      public InitialOrgSetupAccountingResult wire(InitialOrgSetupAccountingContext context) {
        return InitialOrgSetupAccountingResult.notHandled();
      }
    };

    assertEquals(100, handler.getPriority());
  }

  /**
   * Verifies that handlers can override priority when a module needs to run first.
   */
  @Test
  public void accountingHandlerCanOverridePriorityForDeterministicSelection() {
    InitialOrgSetupAccountingHandler handler = new InitialOrgSetupAccountingHandler() {
      @Override
      public int getPriority() {
        return 10;
      }

      @Override
      public boolean applies(InitialOrgSetupAccountingContext context) {
        return false;
      }

      @Override
      public InitialOrgSetupAccountingResult wire(InitialOrgSetupAccountingContext context) {
        return InitialOrgSetupAccountingResult.notHandled();
      }
    };

    assertEquals(10, handler.getPriority());
  }
}
