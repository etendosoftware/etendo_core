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
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Tests for {@link PrintController}.
 * The servlet depends on the full ERP runtime; this suite covers the descriptor and
 * constant values that are safe to verify without a running container.
 */
@SuppressWarnings({"java:S100", "java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class PrintControllerTest {

  /** Servlet info descriptor is non-null and non-empty. */
  @Test
  public void testGetServletInfo_returnsNonEmpty() {
    PrintController controller = new ObjenesisStd().newInstance(PrintController.class);
    String info = controller.getServletInfo();
    assertNotNull(info);
    assertEquals("Servlet that processes the print action", info);
  }

  /** INP_TAB_ID constant matches the session key used by PrintControllerPreferenceHelper. */
  @Test
  public void testInpTabId_constantValue() {
    assertEquals("inpTabId", PrintController.INP_TAB_ID);
  }

  /** CHECK_MORE_THAN_ONE_CUSTOMER constant retains its expected value. */
  @Test
  public void testCheckMoreThanOneCustomer_constantValue() {
    assertEquals("moreThanOneCustomer", PrintController.CHECK_MORE_THAN_ONE_CUSTOMER);
  }

  /** CHECK_MORE_THAN_ONE_SALES_REP constant retains its expected value. */
  @Test
  public void testCheckMoreThanOneSalesRep_constantValue() {
    assertEquals("moreThanOnesalesRep", PrintController.CHECK_MORE_THAN_ONE_SALES_REP);
  }
}
