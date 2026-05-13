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
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.client.application.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.test.base.OBBaseTest;

/**
 * Integration tests for {@link ReportingUtils}.
 * Requires a running database with the Etendo model loaded.
 */
public class ReportingUtilsIntegrationTest extends OBBaseTest {

  // --- ExportType.getExportType ---

  @Test
  public void testGetExportTypePdf() {
    assertEquals(ExportType.PDF, ExportType.getExportType("pdf"));
  }

  @Test
  public void testGetExportTypeCsv() {
    assertEquals(ExportType.CSV, ExportType.getExportType("csv"));
  }

  @Test
  public void testGetExportTypeHtml() {
    assertEquals(ExportType.HTML, ExportType.getExportType("html"));
  }

  @Test
  public void testGetExportTypeXls() {
    assertEquals(ExportType.XLS, ExportType.getExportType("xls"));
  }

  @Test
  public void testGetExportTypeXlsx() {
    assertEquals(ExportType.XLSX, ExportType.getExportType("xlsx"));
  }

  @Test
  public void testGetExportTypeXml() {
    assertEquals(ExportType.XML, ExportType.getExportType("xml"));
  }

  @Test
  public void testGetExportTypeTxt() {
    assertEquals(ExportType.TXT, ExportType.getExportType("txt"));
  }

  @Test
  public void testGetExportTypeCaseInsensitive() {
    assertEquals(ExportType.PDF, ExportType.getExportType("PDF"));
    assertEquals(ExportType.CSV, ExportType.getExportType("Csv"));
  }

  @Test(expected = OBException.class)
  public void testGetExportTypeInvalidThrows() {
    ExportType.getExportType("invalid");
  }

  // --- ExportType.hasExtension ---

  @Test
  public void testHasExtensionMatch() {
    assertTrue(ExportType.PDF.hasExtension("pdf"));
  }

  @Test
  public void testHasExtensionCaseInsensitive() {
    assertTrue(ExportType.PDF.hasExtension("PDF"));
  }

  @Test
  public void testHasExtensionMismatch() {
    assertFalse(ExportType.PDF.hasExtension("csv"));
  }

  @Test
  public void testHasExtensionNull() {
    assertFalse(ExportType.PDF.hasExtension(null));
  }

  @Test
  public void testHasExtensionEmpty() {
    assertFalse(ExportType.PDF.hasExtension(""));
  }

  // --- ExportType.getExtension ---

  @Test
  public void testGetExtension() {
    assertEquals("pdf", ExportType.PDF.getExtension());
    assertEquals("csv", ExportType.CSV.getExtension());
    assertEquals("html", ExportType.HTML.getExtension());
    assertEquals("xls", ExportType.XLS.getExtension());
    assertEquals("xlsx", ExportType.XLSX.getExtension());
    assertEquals("xml", ExportType.XML.getExtension());
    assertEquals("txt", ExportType.TXT.getExtension());
  }

  // --- ExportType.getExportParameters ---

  @Test
  public void testPdfIgnorePaginationIsFalse() {
    Map<String, Object> params = ExportType.PDF.getExportParameters();
    assertEquals(false, params.get("IS_IGNORE_PAGINATION"));
  }

  @Test
  public void testCsvIgnorePaginationIsTrue() {
    Map<String, Object> params = ExportType.CSV.getExportParameters();
    assertEquals(true, params.get("IS_IGNORE_PAGINATION"));
  }

  @Test
  public void testExportParametersDefensiveCopy() {
    Map<String, Object> p1 = ExportType.PDF.getExportParameters();
    Map<String, Object> p2 = ExportType.PDF.getExportParameters();
    p1.put("extra", "value");
    assertFalse("Should be a defensive copy", p2.containsKey("extra"));
  }

  // --- ExportType.isValidTemporaryFileName ---

  @Test
  public void testValidTempFileName() {
    assertTrue(ExportType.PDF.isValidTemporaryFileName(
        "550e8400-e29b-41d4-a716-446655440000.pdf"));
  }

  @Test
  public void testInvalidTempFileNameWrongExtension() {
    assertFalse(ExportType.PDF.isValidTemporaryFileName(
        "550e8400-e29b-41d4-a716-446655440000.csv"));
  }

  @Test
  public void testInvalidTempFileNameNotUuid() {
    assertFalse(ExportType.PDF.isValidTemporaryFileName("report.pdf"));
  }

  // Note: ReportingUtils static methods (getTempFolder, getLimit, getExcelExportType)
  // cannot be tested without the Weld CDI container (requires ServletContext).
  // Only ExportType enum methods are testable in this context.
}
