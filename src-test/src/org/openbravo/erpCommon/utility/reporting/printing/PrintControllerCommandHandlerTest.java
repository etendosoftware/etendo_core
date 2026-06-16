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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.erpCommon.utility.reporting.DocumentType;

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
}
