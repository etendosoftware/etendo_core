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
 * All portions are Copyright © 2021–2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.advpaymentmngt.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.openbravo.advpaymentmngt.hook.BankStatementLineFilterContext.Usage;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;

/**
 * Tests for {@link BankStatementLineFilterContext}.
 */
public class BankStatementLineFilterContextTest {

  /**
   * The getters return the values received in the constructor.
   */
  @Test
  public void testGetters() {
    FIN_FinancialAccount account = mock(FIN_FinancialAccount.class);
    FIN_Reconciliation reconciliation = mock(FIN_Reconciliation.class);

    BankStatementLineFilterContext context = new BankStatementLineFilterContext(account,
        reconciliation, Usage.ENDING_BALANCE);

    assertSame(account, context.getAccount());
    assertSame(reconciliation, context.getReconciliation());
    assertEquals(Usage.ENDING_BALANCE, context.getUsage());
  }

  /**
   * The reconciliation is optional.
   */
  @Test
  public void testReconciliationIsOptional() {
    BankStatementLineFilterContext context = new BankStatementLineFilterContext(
        mock(FIN_FinancialAccount.class), null, Usage.MAX_DATE);

    assertNull(context.getReconciliation());
  }

  /**
   * The account is mandatory.
   */
  @Test(expected = NullPointerException.class)
  public void testAccountIsMandatory() {
    new BankStatementLineFilterContext(null, null, Usage.MAX_DATE);
  }

  /**
   * The usage is mandatory.
   */
  @Test(expected = NullPointerException.class)
  public void testUsageIsMandatory() {
    new BankStatementLineFilterContext(mock(FIN_FinancialAccount.class), null, null);
  }
}
