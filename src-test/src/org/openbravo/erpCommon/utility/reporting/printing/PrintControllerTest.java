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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.secureApp.VariablesSecureApp;

/**
 * Tests for {@link PrintController}: descriptor, constants, pure-logic methods, and
 * contact-persistence paths that can be exercised without a running servlet container.
 */
@SuppressWarnings({"java:S100", "java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class PrintControllerTest {

  private static final String BP_ID = "BP-001";
  private static final String USER_ID = "USER-001";
  private static final String CONTACT_ID = "CONTACT-001";
  private static final String CONTACT_EMAIL = "contact@example.com";

  // -------------------------------------------------------------------------
  // getServletInfo / constants
  // -------------------------------------------------------------------------

  /** Servlet info descriptor is non-null and matches expected value. */
  @Test
  public void testGetServletInfo_returnsNonEmpty() {
    PrintController controller = new ObjenesisStd().newInstance(PrintController.class);
    assertEquals("Servlet that processes the print action", controller.getServletInfo());
    assertNotNull(controller.getServletInfo());
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

  // -------------------------------------------------------------------------
  // seekAndDestroy
  // -------------------------------------------------------------------------

  /** Item with matching ID is removed from the list. */
  @Test
  public void testSeekAndDestroy_matchingId_removesItem() {
    PrintController controller = new ObjenesisStd().newInstance(PrintController.class);
    List<AttachContent> list = new ArrayList<>();
    list.add(attachContent("A"));
    list.add(attachContent("B"));

    controller.seekAndDestroy(list, "A");

    assertEquals(1, list.size());
    assertEquals("B", list.get(0).id);
  }

  /** Nothing changes when the ID is not present in the list. */
  @Test
  public void testSeekAndDestroy_noMatch_listUnchanged() {
    PrintController controller = new ObjenesisStd().newInstance(PrintController.class);
    List<AttachContent> list = new ArrayList<>();
    list.add(attachContent("A"));
    list.add(attachContent("B"));

    controller.seekAndDestroy(list, "C");

    assertEquals(2, list.size());
  }

  /** Only the first matching item is removed; duplicates are preserved. */
  @Test
  public void testSeekAndDestroy_duplicateIds_onlyFirstRemoved() {
    PrintController controller = new ObjenesisStd().newInstance(PrintController.class);
    List<AttachContent> list = new ArrayList<>();
    list.add(attachContent("A"));
    list.add(attachContent("A"));
    list.add(attachContent("B"));

    controller.seekAndDestroy(list, "A");

    assertEquals(2, list.size());
    assertEquals("A", list.get(0).id);
    assertEquals("B", list.get(1).id);
  }

  /** Empty list is handled without error. */
  @Test
  public void testSeekAndDestroy_emptyList_noException() {
    PrintController controller = new ObjenesisStd().newInstance(PrintController.class);
    List<AttachContent> list = new ArrayList<>();
    controller.seekAndDestroy(list, "A");
    assertEquals(0, list.size());
  }

  // -------------------------------------------------------------------------
  // persistLastUsedContact
  // -------------------------------------------------------------------------

  /**
   * When PARAM_TO_CONTACT_ID is blank, the contact is resolved via email lookup and then saved.
   * @throws ServletException if BPContactEmailSelector.saveLastUsedContact throws
   */
  @Test
  public void testPersistLastUsedContact_blankContactId_resolvesViaEmail()
      throws ServletException {
    PrintController controller = new ObjenesisStd().newInstance(PrintController.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getStringParameter(PrintController.PARAM_TO_CONTACT_ID)).thenReturn("");
    when(vars.getStringParameter(PrintController.PARAM_TO_EMAIL)).thenReturn(CONTACT_EMAIL);
    when(vars.getUser()).thenReturn(USER_ID);

    try (MockedStatic<BPContactEmailSelector> selector = mockStatic(BPContactEmailSelector.class)) {
      selector.when(() -> BPContactEmailSelector.findContactIdByEmail(BP_ID, CONTACT_EMAIL))
          .thenReturn(CONTACT_ID);

      controller.persistLastUsedContact(vars, BP_ID);

      selector.verify(() -> BPContactEmailSelector.saveLastUsedContact(USER_ID, BP_ID, CONTACT_ID));
    }
  }

  /**
   * When PARAM_TO_CONTACT_ID is populated, it is used directly without the email lookup.
   * @throws ServletException if BPContactEmailSelector.saveLastUsedContact throws
   */
  @Test
  public void testPersistLastUsedContact_populatedContactId_savesDirectly()
      throws ServletException {
    PrintController controller = new ObjenesisStd().newInstance(PrintController.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getStringParameter(PrintController.PARAM_TO_CONTACT_ID)).thenReturn(CONTACT_ID);
    when(vars.getUser()).thenReturn(USER_ID);

    try (MockedStatic<BPContactEmailSelector> selector = mockStatic(BPContactEmailSelector.class)) {
      controller.persistLastUsedContact(vars, BP_ID);

      selector.verify(() -> BPContactEmailSelector.saveLastUsedContact(USER_ID, BP_ID, CONTACT_ID));
      selector.verify(() -> BPContactEmailSelector.findContactIdByEmail(anyString(), anyString()),
          never());
    }
  }

  /**
   * When both PARAM_TO_CONTACT_ID and the email lookup return blank, saveLastUsedContact is
   * never called.
   * @throws ServletException if BPContactEmailSelector.saveLastUsedContact throws
   */
  @Test
  public void testPersistLastUsedContact_bothBlank_doesNotSave() throws ServletException {
    PrintController controller = new ObjenesisStd().newInstance(PrintController.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getStringParameter(PrintController.PARAM_TO_CONTACT_ID)).thenReturn("");
    when(vars.getStringParameter(PrintController.PARAM_TO_EMAIL)).thenReturn("");

    try (MockedStatic<BPContactEmailSelector> selector = mockStatic(BPContactEmailSelector.class)) {
      selector.when(() -> BPContactEmailSelector.findContactIdByEmail(BP_ID, ""))
          .thenReturn("");

      controller.persistLastUsedContact(vars, BP_ID);

      selector.verify(() -> BPContactEmailSelector.saveLastUsedContact(
          anyString(), anyString(), anyString()), never());
    }
  }

  // -------------------------------------------------------------------------
  // field defaults
  // -------------------------------------------------------------------------

  /** multiReports defaults to false after construction. */
  @Test
  public void testMultiReports_defaultFalse() {
    PrintController controller = new ObjenesisStd().newInstance(PrintController.class);
    assertFalse(controller.multiReports);
  }

  /** archivedReports defaults to false after construction. */
  @Test
  public void testArchivedReports_defaultFalse() {
    PrintController controller = new ObjenesisStd().newInstance(PrintController.class);
    assertFalse(controller.archivedReports);
  }

  // -------------------------------------------------------------------------
  // logger wrappers
  // -------------------------------------------------------------------------

  /** getBaseDirectoryJs returns a JS var declaration string. */
  @Test
  public void testGetBaseDirectoryJs_returnsFormatString() {
    PrintController controller = new ObjenesisStd().newInstance(PrintController.class);
    String result = controller.getBaseDirectoryJs();
    assertTrue(result.contains("var baseDirectory = "));
  }

  /** isDebugEnabled returns a boolean without throwing.
   * @throws Exception if logger injection fails */
  @Test
  public void testIsDebugEnabled_returnsBooleanValue() throws Exception {
    PrintController controller = controllerWithLogger();
    controller.isDebugEnabled();
  }

  /** debug delegates to log4j without throwing.
   * @throws Exception if logger injection fails */
  @Test
  public void testDebug_doesNotThrow() throws Exception {
    PrintController controller = controllerWithLogger();
    controller.debug("test message");
  }

  /** warn replaces placeholder without throwing.
   * @throws Exception if logger injection fails */
  @Test
  public void testWarn_replacesPlaceholder_doesNotThrow() throws Exception {
    PrintController controller = controllerWithLogger();
    controller.warn("msg {}", "detail");
  }

  /** error delegates to log4j without throwing.
   * @throws Exception if logger injection fails */
  @Test
  public void testError_doesNotThrow() throws Exception {
    PrintController controller = controllerWithLogger();
    controller.error(new RuntimeException("test error"));
  }

  // -------------------------------------------------------------------------
  // helpers
  // -------------------------------------------------------------------------

  private static AttachContent attachContent(String id) {
    AttachContent ac = new AttachContent();
    ac.id = id;
    return ac;
  }

  private static PrintController controllerWithLogger() throws Exception {
    PrintController controller = new ObjenesisStd().newInstance(PrintController.class);
    Class<?> cls = controller.getClass();
    Field logField = null;
    while (cls != null && logField == null) {
      try {
        logField = cls.getDeclaredField("log4j");
      } catch (NoSuchFieldException e) {
        cls = cls.getSuperclass();
      }
    }
    if (logField != null) {
      logField.setAccessible(true);
      logField.set(controller, LogManager.getLogger(PrintController.class));
    }
    return controller;
  }
}
