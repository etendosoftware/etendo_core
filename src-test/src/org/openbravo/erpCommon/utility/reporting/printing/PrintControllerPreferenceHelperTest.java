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

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Tab;

/**
 * Tests for {@link PrintControllerPreferenceHelper} with mocked DAL dependencies.
 */
@SuppressWarnings({"java:S100", "java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class PrintControllerPreferenceHelperTest {

  /**
   * isDirectPrint returns false when the Tab lookup returns null for the given tab ID.
   */
  @Test
  public void testIsDirectPrint_tabNotFound_returnsFalse() {
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    OBDal obDalMock = mock(OBDal.class);
    OBContext obContextMock = mock(OBContext.class);

    when(vars.getSessionValue(PrintController.INP_TAB_ID)).thenReturn("nonexistent-tab-id");

    try (MockedStatic<OBContext> obContextStatic = mockStatic(OBContext.class);
        MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {

      obContextStatic.when(OBContext::getOBContext).thenReturn(obContextMock);
      obDalStatic.when(OBDal::getInstance).thenReturn(obDalMock);
      when(obDalMock.get(Tab.class, "nonexistent-tab-id")).thenReturn(null);

      boolean result = PrintControllerPreferenceHelper.isDirectPrint(vars);

      assertFalse(result);
    }
  }

  /**
   * isDirectPrint returns false when the session value for the tab ID is an empty string,
   * causing the Tab lookup to return null.
   */
  @Test
  public void testIsDirectPrint_emptyTabId_returnsFalse() {
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    OBDal obDalMock = mock(OBDal.class);
    OBContext obContextMock = mock(OBContext.class);

    when(vars.getSessionValue(PrintController.INP_TAB_ID)).thenReturn("");
    when(obDalMock.get(Tab.class, "")).thenReturn(null);

    try (MockedStatic<OBContext> obContextStatic = mockStatic(OBContext.class);
        MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {

      obContextStatic.when(OBContext::getOBContext).thenReturn(obContextMock);
      obDalStatic.when(OBDal::getInstance).thenReturn(obDalMock);

      boolean result = PrintControllerPreferenceHelper.isDirectPrint(vars);

      assertFalse(result);
    }
  }
}
