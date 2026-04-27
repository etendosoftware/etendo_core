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
 * All portions are Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.generalsetup.enterprise.organization;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetupAccountingContext;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetupAccountingHandler;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetupAccountingResult;

public class InitialOrgSetupAccountingHookSelectionTest {

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
