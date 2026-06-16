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
package org.openbravo.erpCommon.utility.reporting.printing; //NOSONAR

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Tests for the thin servlet subclasses: {@link PrintInvoices}, {@link PrintOrders},
 * {@link PrintPayments}, {@link PrintQuotations}, and {@link PrintShipments}.
 */
@SuppressWarnings({"java:S100", "java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class PrintServletsTest {

  private static final String SERVLET_INFO = "Servlet that processes the print action";

  /** PrintInvoices servlet descriptor matches expected value. */
  @Test
  public void testPrintInvoices_getServletInfo() {
    PrintInvoices servlet = new ObjenesisStd().newInstance(PrintInvoices.class);
    assertEquals(SERVLET_INFO, servlet.getServletInfo());
  }

  /** PrintOrders servlet descriptor matches expected value. */
  @Test
  public void testPrintOrders_getServletInfo() {
    PrintOrders servlet = new ObjenesisStd().newInstance(PrintOrders.class);
    assertEquals(SERVLET_INFO, servlet.getServletInfo());
  }

  /** PrintPayments servlet descriptor matches expected value. */
  @Test
  public void testPrintPayments_getServletInfo() {
    PrintPayments servlet = new ObjenesisStd().newInstance(PrintPayments.class);
    assertEquals(SERVLET_INFO, servlet.getServletInfo());
  }

  /** PrintQuotations servlet descriptor matches expected value. */
  @Test
  public void testPrintQuotations_getServletInfo() {
    PrintQuotations servlet = new ObjenesisStd().newInstance(PrintQuotations.class);
    assertEquals(SERVLET_INFO, servlet.getServletInfo());
  }

  /** PrintShipments servlet descriptor matches expected value. */
  @Test
  public void testPrintShipments_getServletInfo() {
    PrintShipments servlet = new ObjenesisStd().newInstance(PrintShipments.class);
    assertEquals(SERVLET_INFO, servlet.getServletInfo());
  }
}
