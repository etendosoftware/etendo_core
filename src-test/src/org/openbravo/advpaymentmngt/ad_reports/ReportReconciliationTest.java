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
package org.openbravo.advpaymentmngt.ad_reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.openbravo.advpaymentmngt.dao.MatchTransactionDao;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;

/**
 * Tests for the un-reconciled bank statement lines section of {@link ReportReconciliation}.
 */
public class ReportReconciliationTest {

  private MockedStatic<MatchTransactionDao> mockedDao;
  private FIN_Reconciliation reconciliation;
  private ReportReconciliation report;

  /**
   * Mocks the DAO that returns the un-reconciled bank statement lines.
   */
  @Before
  public void setUp() {
    mockedDao = mockStatic(MatchTransactionDao.class);
    reconciliation = mock(FIN_Reconciliation.class);
    report = new ReportReconciliation();
  }

  /**
   * Closes the static mock of the DAO.
   */
  @After
  public void tearDown() {
    mockedDao.close();
  }

  /**
   * The rows returned by the DAO are converted, in the same order, to the fields of the
   * UnreconciledBankStatement subreport.
   *
   * @throws ReflectiveOperationException
   *           if the private methods can not be invoked
   */
  @Test
  public void testLinesAreMapped() throws ReflectiveOperationException {
    Date firstDate = new Date(1000L);
    Date secondDate = new Date(2000L);
    List<Object[]> rows = Arrays.asList(
        new Object[] { firstDate, "Partner A", "REF1", new BigDecimal("0.09") },
        new Object[] { secondDate, "", null, new BigDecimal("-0.09") },
        new Object[] { secondDate, "Partner B", "REF3", new BigDecimal("500.00") });
    mockedDao.when(() -> MatchTransactionDao.getUnreconciledBankStatementLines(reconciliation))
        .thenReturn(rows);

    List<Map<String, Object>> lines = invokeGetLines();

    assertEquals(3, lines.size());
    Map<String, Object> first = lines.get(0);
    assertEquals(firstDate, first.get("BSLINE_DATE"));
    assertEquals("Partner A", first.get("DESCRIPTION"));
    assertEquals("REF1", first.get("REF"));
    assertEquals(new BigDecimal("0.09"), first.get("AMOUNT"));
    assertEquals("", lines.get(1).get("DESCRIPTION"));
    assertEquals("Partner B", lines.get(2).get("DESCRIPTION"));
  }

  /**
   * Without un-reconciled lines the detail is empty.
   *
   * @throws ReflectiveOperationException
   *           if the private methods can not be invoked
   */
  @Test
  public void testNoLines() throws ReflectiveOperationException {
    mockedDao.when(() -> MatchTransactionDao.getUnreconciledBankStatementLines(reconciliation))
        .thenReturn(Collections.emptyList());

    List<Map<String, Object>> lines = invokeGetLines();

    assertTrue(lines.isEmpty());
  }

  /**
   * Invokes the private method that returns the un-reconciled bank statement lines.
   *
   * @return the lines, as maps with the fields of the subreport
   * @throws ReflectiveOperationException
   *           if the method can not be invoked
   */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> invokeGetLines() throws ReflectiveOperationException {
    Method method = ReportReconciliation.class
        .getDeclaredMethod("getUnreconciledBankStatementLines", FIN_Reconciliation.class);
    method.setAccessible(true);
    return (List<Map<String, Object>>) method.invoke(report, reconciliation);
  }
}
