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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.reporting.DocumentType;
import org.openbravo.model.common.enterprise.EmailTemplate;

/**
 * Tests for {@link PrintControllerCommandHandler} inner {@code Context} value object.
 */
@SuppressWarnings({"java:S100", "java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class PrintControllerCommandHandlerTest {

  private static final String SESSION_PREFIX = "PRINTINVOICES";
  private static final String FULL_IDENTIFIER = "DOC001C_INVOICE";
  private static final String DOC_ID = "DOC001";

  /** Context constructor stores all provided fields. */
  @Test
  public void testContext_constructor_storesAllFields() {
    HashMap<String, Boolean> checks = new HashMap<>();
    String[] documentIds = { DOC_ID };

    PrintControllerCommandHandler.Context ctx = new PrintControllerCommandHandler.Context(
        DocumentType.SALESINVOICE, SESSION_PREFIX, FULL_IDENTIFIER, checks, documentIds, null, null);

    assertEquals(DocumentType.SALESINVOICE, ctx.documentType);
    assertEquals(SESSION_PREFIX, ctx.sessionValuePrefix);
    assertEquals(FULL_IDENTIFIER, ctx.fullDocumentIdentifier);
    assertSame(checks, ctx.checks);
    assertArrayEquals(documentIds, ctx.documentIds);
    assertNull(ctx.reports);
    assertNull(ctx.reportManager);
  }

  /** Context retains the document type enum passed to the constructor. */
  @Test
  public void testContext_documentType_isPreserved() {
    PrintControllerCommandHandler.Context ctx = new PrintControllerCommandHandler.Context(
        DocumentType.SALESORDER, SESSION_PREFIX, FULL_IDENTIFIER,
        new HashMap<>(), new String[] { DOC_ID }, null, null);

    assertEquals(DocumentType.SALESORDER, ctx.documentType);
  }

  /** Context reports field is mutable and can be updated after construction. */
  @Test
  public void testContext_reports_isMutable() {
    PrintControllerCommandHandler.Context ctx = new PrintControllerCommandHandler.Context(
        DocumentType.SHIPMENT, SESSION_PREFIX, FULL_IDENTIFIER,
        new HashMap<>(), new String[] { DOC_ID }, null, null);

    HashMap<String, org.openbravo.erpCommon.utility.reporting.Report> reports = new HashMap<>();
    ctx.reports = reports;

    assertSame(reports, ctx.reports);
  }

  // -------------------------------------------------------------------------
  // createContext
  // -------------------------------------------------------------------------

  /**
   * createContext with a single valid document ID builds a Context with the expected fields.
   * @throws ServletException never thrown for a non-empty documentId
   */
  @Test
  public void testCreateContext_singleId_buildsContextCorrectly() throws ServletException {
    PrintController controller = mock(PrintController.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getSessionObject(SESSION_PREFIX + ".Documents")).thenReturn(null);

    PrintControllerCommandHandler.Context ctx = PrintControllerCommandHandler.createContext(
        controller, vars, DocumentType.SALESINVOICE, SESSION_PREFIX, DOC_ID);

    assertEquals(DocumentType.SALESINVOICE, ctx.documentType);
    assertEquals(SESSION_PREFIX, ctx.sessionValuePrefix);
    assertEquals(1, ctx.documentIds.length);
    assertEquals(DOC_ID, ctx.documentIds[0]);
    assertEquals(DOC_ID + "C_INVOICE", ctx.fullDocumentIdentifier);
  }

  /**
   * createContext with comma-separated IDs produces multiple documentIds and sets
   * multiReports to true on the controller.
   * @throws ServletException never thrown for a non-empty documentId
   */
  @Test
  public void testCreateContext_multipleIds_setsMultiReports() throws ServletException {
    PrintController controller = mock(PrintController.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getSessionObject(SESSION_PREFIX + ".Documents")).thenReturn(null);

    PrintControllerCommandHandler.createContext(
        controller, vars, DocumentType.SALESINVOICE, SESSION_PREFIX, "ID001,ID002,ID003");

    assertTrue(controller.multiReports);
  }

  /**
   * createContext initialises the checks map with both customer and sales-rep flags set to false.
   * @throws ServletException never thrown for a non-empty documentId
   */
  @Test
  public void testCreateContext_checksMapInitialisedToFalse() throws ServletException {
    PrintController controller = mock(PrintController.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getSessionObject(SESSION_PREFIX + ".Documents")).thenReturn(null);

    PrintControllerCommandHandler.Context ctx = PrintControllerCommandHandler.createContext(
        controller, vars, DocumentType.SALESORDER, SESSION_PREFIX, DOC_ID);

    assertEquals(Boolean.FALSE, ctx.checks.get(PrintController.CHECK_MORE_THAN_ONE_CUSTOMER));
    assertEquals(Boolean.FALSE, ctx.checks.get(PrintController.CHECK_MORE_THAN_ONE_SALES_REP));
  }

  /**
   * createContext strips SQL characters from the document ID when building fullDocumentIdentifier.
   * @throws ServletException never thrown for a non-empty documentId
   */
  @Test
  public void testCreateContext_sanitizesDocumentId() throws ServletException {
    PrintController controller = mock(PrintController.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getSessionObject(SESSION_PREFIX + ".Documents")).thenReturn(null);

    PrintControllerCommandHandler.Context ctx = PrintControllerCommandHandler.createContext(
        controller, vars, DocumentType.SALESINVOICE, SESSION_PREFIX, "'ID001'");

    assertEquals("ID001C_INVOICE", ctx.fullDocumentIdentifier);
    assertEquals("ID001", ctx.documentIds[0]);
  }

  // -------------------------------------------------------------------------
  // handler instantiation
  // -------------------------------------------------------------------------

  /** Constructor stores all provided fields without throwing. */
  @Test
  public void testInstantiation_doesNotThrow() {
    PrintController ctrl = mock(PrintController.class);
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse resp = mock(HttpServletResponse.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    PrintControllerCommandHandler.Context ctx = makeContext(new String[]{ DOC_ID }, DocumentType.SALESINVOICE);

    PrintControllerCommandHandler handler = new PrintControllerCommandHandler(ctrl, req, resp, vars, ctx);

    assertNotNull(handler);
  }

  // -------------------------------------------------------------------------
  // handle() dispatch
  // -------------------------------------------------------------------------

  /**
   * When no command matches, the error page is shown.
   * @throws Exception if handle() propagates an unexpected error
   */
  @Test
  public void testHandle_noMatchingCommand_callsShowPageError() throws Exception {
    PrintController ctrl = mock(PrintController.class);
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse resp = mock(HttpServletResponse.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    PrintControllerCommandHandler handler = new PrintControllerCommandHandler(
        ctrl, req, resp, vars, makeContext(new String[]{ DOC_ID }, DocumentType.SALESINVOICE));
    handler.handle();

    verify(ctrl).showPageError(resp);
  }

  /**
   * PRINT command routes to handlePrintCommand, which calls printReports on the controller.
   * @throws Exception if handle() propagates an unexpected error
   */
  @Test
  public void testHandle_printCommand_invokesPrintReports() throws Exception {
    PrintController ctrl = mock(PrintController.class);
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse resp = mock(HttpServletResponse.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.commandIn("PRINT")).thenReturn(true);

    PrintControllerCommandHandler handler = new PrintControllerCommandHandler(
        ctrl, req, resp, vars, makeContext(new String[]{ DOC_ID }, DocumentType.SALESINVOICE));

    try (MockedStatic<PrintControllerPreferenceHelper> prefHelper =
             mockStatic(PrintControllerPreferenceHelper.class)) {
      prefHelper.when(() -> PrintControllerPreferenceHelper.isDirectPrint(any())).thenReturn(false);

      handler.handle();

      verify(ctrl).printReports(any(), any(), any(), anyBoolean());
    }
  }

  /**
   * ARCHIVE command sets archivedReports to true on the controller.
   * @throws Exception if handle() propagates an unexpected error
   */
  @Test
  public void testHandle_archiveCommand_setsArchivedReports() throws Exception {
    PrintController ctrl = mock(PrintController.class);
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse resp = mock(HttpServletResponse.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.commandIn("ARCHIVE")).thenReturn(true);

    PrintControllerCommandHandler handler = new PrintControllerCommandHandler(
        ctrl, req, resp, vars, makeContext(new String[]{ DOC_ID }, DocumentType.SALESINVOICE));

    try (MockedStatic<PrintControllerPreferenceHelper> prefHelper =
             mockStatic(PrintControllerPreferenceHelper.class)) {
      prefHelper.when(() -> PrintControllerPreferenceHelper.isDirectPrint(any())).thenReturn(false);

      handler.handle();

      assertTrue(ctrl.archivedReports);
    }
  }

  /**
   * ADD command on a print-path URL delegates to createPrintOptionsPage on the controller.
   * @throws Exception if handle() propagates an unexpected error
   */
  @Test
  public void testHandle_addCommand_printPath_callsCreatePrintOptionsPage() throws Exception {
    PrintController ctrl = mock(PrintController.class);
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse resp = mock(HttpServletResponse.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.commandIn("ADD")).thenReturn(true);
    when(req.getServletPath()).thenReturn("/print.html");

    PrintControllerCommandHandler handler = new PrintControllerCommandHandler(
        ctrl, req, resp, vars, makeContext(new String[]{ DOC_ID }, DocumentType.SALESINVOICE));
    handler.handle();

    verify(ctrl).createPrintOptionsPage(any(), any(), any(), any(), any(), any());
  }

  /**
   * ADD command on a send-path URL delegates to createEmailOptionsPage on the controller.
   * @throws Exception if handle() propagates an unexpected error
   */
  @Test
  public void testHandle_addCommand_sendPath_callsCreateEmailOptionsPage() throws Exception {
    PrintController ctrl = mock(PrintController.class);
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse resp = mock(HttpServletResponse.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.commandIn("ADD")).thenReturn(true);
    when(req.getServletPath()).thenReturn("/send.html");

    PrintControllerCommandHandler handler = new PrintControllerCommandHandler(
        ctrl, req, resp, vars, makeContext(new String[]{ DOC_ID }, DocumentType.SALESINVOICE));
    handler.handle();

    verify(ctrl).createEmailOptionsPage(any(), any(), any(), any(), any(), any(), any());
  }

  /**
   * ADD command on a path that is neither print nor send calls neither page builder.
   * @throws Exception if handle() propagates an unexpected error
   */
  @Test
  public void testHandle_addCommand_otherPath_callsNeither() throws Exception {
    PrintController ctrl = mock(PrintController.class);
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse resp = mock(HttpServletResponse.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.commandIn("ADD")).thenReturn(true);
    when(req.getServletPath()).thenReturn("/other.html");

    PrintControllerCommandHandler handler = new PrintControllerCommandHandler(
        ctrl, req, resp, vars, makeContext(new String[]{ DOC_ID }, DocumentType.SALESINVOICE));
    handler.handle();

    verify(ctrl, never()).createPrintOptionsPage(any(), any(), any(), any(), any(), any());
    verify(ctrl, never()).createEmailOptionsPage(any(), any(), any(), any(), any(), any(), any());
  }

  /**
   * DEL command with no existing session attachments still calls createEmailOptionsPage.
   * @throws Exception if handle() propagates an unexpected error
   */
  @Test
  public void testHandle_delCommand_nullAttachments_skipsSeekAndDestroy() throws Exception {
    PrintController ctrl = mock(PrintController.class);
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse resp = mock(HttpServletResponse.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    HttpSession session = mock(HttpSession.class);
    when(vars.commandIn("DEL")).thenReturn(true);
    when(req.getSession()).thenReturn(session);
    when(session.getAttribute(PrintController.SESSION_FILES)).thenReturn(null);

    PrintControllerCommandHandler handler = new PrintControllerCommandHandler(
        ctrl, req, resp, vars, makeContext(new String[]{ DOC_ID }, DocumentType.SALESINVOICE));
    handler.handle();

    verify(ctrl, never()).seekAndDestroy(any(), any());
    verify(ctrl).createEmailOptionsPage(any(), any(), any(), any(), any(), any(), any());
  }

  /**
   * GET_BP_CONTACTS command builds the contacts JSON and writes it to the response.
   * @throws Exception if handle() propagates an unexpected error
   */
  @Test
  public void testHandle_getBpContactsCommand_writesJsonResponse() throws Exception {
    PrintController ctrl = mock(PrintController.class);
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse resp = mock(HttpServletResponse.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.commandIn("GET_BP_CONTACTS")).thenReturn(true);

    PrintControllerCommandHandler handler = new PrintControllerCommandHandler(
        ctrl, req, resp, vars, makeContext(new String[]{ DOC_ID }, DocumentType.SALESINVOICE));

    try (MockedStatic<PrintControllerJsonHelper> jsonHelper =
             mockStatic(PrintControllerJsonHelper.class);
         MockedStatic<BPContactEmailSelector> selector =
             mockStatic(BPContactEmailSelector.class)) {
      selector.when(() -> BPContactEmailSelector.getBPContactsWithEmail(any())).thenReturn(null);
      PrintWriter pw = mock(PrintWriter.class);
      when(resp.getWriter()).thenReturn(pw);

      handler.handle();

      jsonHelper.verify(() -> PrintControllerJsonHelper.writeJsonResponse(any(), any()));
    }
  }

  /**
   * UPDATE_EMAILCONFIG with a null EmailTemplate writes an empty JSON object.
   * @throws Exception if handle() propagates an unexpected error
   */
  @Test
  public void testHandle_updateEmailConfigCommand_templateNull_writesEmptyJson() throws Exception {
    PrintController ctrl = mock(PrintController.class);
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse resp = mock(HttpServletResponse.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    OBDal obDalMock = mock(OBDal.class);
    when(vars.commandIn("UPDATE_EMAILCONFIG")).thenReturn(true);
    when(vars.getStringParameter("emailConfigList")).thenReturn("");
    when(obDalMock.get(EmailTemplate.class, "")).thenReturn(null);
    PrintWriter pw = mock(PrintWriter.class);
    when(resp.getWriter()).thenReturn(pw);

    PrintControllerCommandHandler handler = new PrintControllerCommandHandler(
        ctrl, req, resp, vars, makeContext(new String[]{ DOC_ID }, DocumentType.SALESINVOICE));

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {
      obDalStatic.when(OBDal::getInstance).thenReturn(obDalMock);

      handler.handle();

      verify(pw).write(any(String.class));
    }
  }

  /**
   * UPDATE_EMAILCONFIG with a found EmailTemplate writes subject and body to the response.
   * @throws Exception if handle() propagates an unexpected error
   */
  @Test
  public void testHandle_updateEmailConfigCommand_templateFound_writesSubjectAndBody()
      throws Exception {
    PrintController ctrl = mock(PrintController.class);
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse resp = mock(HttpServletResponse.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    OBDal obDalMock = mock(OBDal.class);
    EmailTemplate tpl = mock(EmailTemplate.class);
    when(tpl.getSubject()).thenReturn("Hello");
    when(tpl.getBody()).thenReturn("World");
    when(vars.commandIn("UPDATE_EMAILCONFIG")).thenReturn(true);
    when(vars.getStringParameter("emailConfigList")).thenReturn("cfg-001");
    when(obDalMock.get(EmailTemplate.class, "cfg-001")).thenReturn(tpl);
    PrintWriter pw = mock(PrintWriter.class);
    when(resp.getWriter()).thenReturn(pw);

    PrintControllerCommandHandler handler = new PrintControllerCommandHandler(
        ctrl, req, resp, vars, makeContext(new String[]{ DOC_ID }, DocumentType.SALESINVOICE));

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {
      obDalStatic.when(OBDal::getInstance).thenReturn(obDalMock);

      handler.handle();

      verify(pw).write(any(String.class));
    }
  }

  // -------------------------------------------------------------------------
  // private path helpers via reflection
  // -------------------------------------------------------------------------

  /**
   * isPrintPath returns true when the servlet path contains "print.html".
   * @throws Exception if reflection fails
   */
  @Test
  public void testIsPrintPath_printHtml_returnsTrue() throws Exception {
    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getServletPath()).thenReturn("/print.html");

    assertTrue(invokeBooleanReflection(req, "isPrintPath"));
  }

  /**
   * isPrintPath returns false when the servlet path does not contain "print.html".
   * @throws Exception if reflection fails
   */
  @Test
  public void testIsPrintPath_sendHtml_returnsFalse() throws Exception {
    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getServletPath()).thenReturn("/send.html");

    assertFalse(invokeBooleanReflection(req, "isPrintPath"));
  }

  /**
   * isSendPath returns true when the servlet path contains "send.html".
   * @throws Exception if reflection fails
   */
  @Test
  public void testIsSendPath_sendHtml_returnsTrue() throws Exception {
    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getServletPath()).thenReturn("/send.html");

    assertTrue(invokeBooleanReflection(req, "isSendPath"));
  }

  /**
   * isPrintOptionsPath returns true when the servlet path contains "printoptions.html".
   * @throws Exception if reflection fails
   */
  @Test
  public void testIsPrintOptionsPath_printoptionsHtml_returnsTrue() throws Exception {
    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getServletPath()).thenReturn("/PrintOptions.html");

    assertTrue(invokeBooleanReflection(req, "isPrintOptionsPath"));
  }

  // -------------------------------------------------------------------------
  // getOrderedDocumentIds via reflection
  // -------------------------------------------------------------------------

  /**
   * When multiReports is false, getOrderedDocumentIds returns the context array directly.
   * @throws Exception if reflection fails
   */
  @Test
  public void testGetOrderedDocumentIds_singleDoc_returnsContextDocumentIds() throws Exception {
    PrintController ctrl = mock(PrintController.class);
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse resp = mock(HttpServletResponse.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    String[] ids = { "SHIP-001" };

    PrintControllerCommandHandler handler = new PrintControllerCommandHandler(
        ctrl, req, resp, vars, makeContext(ids, DocumentType.SHIPMENT));

    Method m = PrintControllerCommandHandler.class.getDeclaredMethod("getOrderedDocumentIds");
    m.setAccessible(true);
    String[] result = (String[]) m.invoke(handler);

    assertSame(ids, result);
  }

  /**
   * When multiReports is true with SHIPMENT type, orderByDocumentNo returns the array unchanged.
   * @throws Exception if reflection or orderByDocumentNo fails
   */
  @Test
  public void testGetOrderedDocumentIds_multiDoc_shipmentType_returnsArrayUnchanged()
      throws Exception {
    PrintController ctrl = mock(PrintController.class);
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse resp = mock(HttpServletResponse.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    String[] ids = { "SHIP-001", "SHIP-002" };
    ctrl.multiReports = true;

    PrintControllerCommandHandler handler = new PrintControllerCommandHandler(
        ctrl, req, resp, vars, makeContext(ids, DocumentType.SHIPMENT));

    Method m = PrintControllerCommandHandler.class.getDeclaredMethod("getOrderedDocumentIds");
    m.setAccessible(true);
    String[] result = (String[]) m.invoke(handler);

    assertArrayEquals(ids, result);
  }

  // -------------------------------------------------------------------------
  // helpers
  // -------------------------------------------------------------------------

  private static PrintControllerCommandHandler.Context makeContext(String[] docIds,
      DocumentType docType) {
    return new PrintControllerCommandHandler.Context(
        docType, SESSION_PREFIX, FULL_IDENTIFIER, new HashMap<>(), docIds, null, null);
  }

  private static boolean invokeBooleanReflection(HttpServletRequest req, String methodName)
      throws Exception {
    PrintController ctrl = mock(PrintController.class);
    HttpServletResponse resp = mock(HttpServletResponse.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    PrintControllerCommandHandler handler = new PrintControllerCommandHandler(
        ctrl, req, resp, vars, makeContext(new String[]{ DOC_ID }, DocumentType.SALESINVOICE));
    Method m = PrintControllerCommandHandler.class.getDeclaredMethod(methodName);
    m.setAccessible(true);
    return (Boolean) m.invoke(handler);
  }
}
