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
package org.openbravo.erpCommon.utility.reporting.printing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.erpCommon.utility.reporting.Report;

@SuppressWarnings({ "java:S120" })
@RunWith(MockitoJUnitRunner.Silent.class)
public class PrintControllerDocumentHelperTest {

  // -------------------------------------------------------------------------
  // sanitizeDocumentIdentifier
  // -------------------------------------------------------------------------

  @Test
  public void testSanitize_alphanumericHyphenComma_keptIntact() {
    assertEquals("ABC-123,DEF", PrintControllerDocumentHelper.sanitizeDocumentIdentifier("ABC-123,DEF"));
  }

  @Test
  public void testSanitize_uuid_keptIntact() {
    String uuid = "5057F533-9377-4AC3-A952-F9143426A922";
    assertEquals(uuid, PrintControllerDocumentHelper.sanitizeDocumentIdentifier(uuid));
  }

  @Test
  public void testSanitize_sqlSpecialChars_stripped() {
    assertEquals("ID1ID2", PrintControllerDocumentHelper.sanitizeDocumentIdentifier("'ID1'(ID2)"));
  }

  @Test
  public void testSanitize_spaces_stripped() {
    assertEquals("AB", PrintControllerDocumentHelper.sanitizeDocumentIdentifier("A B"));
  }

  @Test
  public void testSanitize_semicolonAndEquals_stripped() {
    assertEquals("1OR11", PrintControllerDocumentHelper.sanitizeDocumentIdentifier("1 OR 1=1"));
  }

  @Test
  public void testSanitize_emptyString_returnsEmpty() {
    assertEquals("", PrintControllerDocumentHelper.sanitizeDocumentIdentifier(""));
  }

  @Test
  public void testSanitize_onlyForbiddenChars_returnsEmpty() {
    assertEquals("", PrintControllerDocumentHelper.sanitizeDocumentIdentifier("'(); "));
  }

  /**
   * Documents the interaction between getCommaSeparatedString and sanitizeDocumentIdentifier:
   * applying sanitize on SQL-formatted output destroys the quoting. This is why
   * double-sanitization was removed from CommandHandler.
   */
  @Test
  public void testSanitize_onCommaSeparatedOutput_stripsQuotesAndParens() {
    String sqlFormatted = PrintControllerDocumentHelper.getCommaSeparatedString(new String[]{ "ID1", "ID2" });
    assertEquals("('ID1','ID2')", sqlFormatted);
    assertEquals("ID1,ID2", PrintControllerDocumentHelper.sanitizeDocumentIdentifier(sqlFormatted));
  }

  // -------------------------------------------------------------------------
  // getCommaSeparatedString
  // -------------------------------------------------------------------------

  @Test
  public void testGetCommaSeparated_singleId_wrappedInParensAndQuotes() {
    assertEquals("('id1')", PrintControllerDocumentHelper.getCommaSeparatedString(new String[]{ "id1" }));
  }

  @Test
  public void testGetCommaSeparated_multipleIds_commaSeparatedWithQuotes() {
    assertEquals("('a','b','c')",
        PrintControllerDocumentHelper.getCommaSeparatedString(new String[]{ "a", "b", "c" }));
  }

  @Test
  public void testGetCommaSeparated_emptyArray_returnsEmptyParens() {
    assertEquals("()", PrintControllerDocumentHelper.getCommaSeparatedString(new String[0]));
  }

  // -------------------------------------------------------------------------
  // normalizeDocumentId
  // -------------------------------------------------------------------------

  @Test
  public void testNormalize_quotedId_removesQuotes() {
    assertEquals("id1", PrintControllerDocumentHelper.normalizeDocumentId("'id1'"));
  }

  @Test
  public void testNormalize_parentheses_removed() {
    assertEquals("id1", PrintControllerDocumentHelper.normalizeDocumentId("(id1)"));
  }

  @Test
  public void testNormalize_noSpecialChars_unchanged() {
    assertEquals("ABC-123", PrintControllerDocumentHelper.normalizeDocumentId("ABC-123"));
  }

  @Test
  public void testNormalize_sqlFormatted_stripsAllDelimiters() {
    assertEquals("id1,id2", PrintControllerDocumentHelper.normalizeDocumentId("('id1','id2')"));
  }

  // -------------------------------------------------------------------------
  // getFilenameForReports
  // -------------------------------------------------------------------------

  @Test
  public void testGetFilename_emptyCollection_returnsEmpty() {
    assertEquals("", PrintControllerDocumentHelper.getFilenameForReports(Collections.emptyList()));
  }

  @Test
  public void testGetFilename_singleReport_returnsItsFilename() {
    Report report = mock(Report.class);
    when(report.getFilename()).thenReturn("invoice_001.pdf");
    assertEquals("invoice_001.pdf",
        PrintControllerDocumentHelper.getFilenameForReports(Collections.singletonList(report)));
  }

  @Test
  public void testGetFilename_multipleReports_returnsLastFilename() {
    Report r1 = mock(Report.class);
    Report r2 = mock(Report.class);
    Report r3 = mock(Report.class);
    when(r1.getFilename()).thenReturn("first.pdf");
    when(r2.getFilename()).thenReturn("second.pdf");
    when(r3.getFilename()).thenReturn("last.pdf");
    assertEquals("last.pdf",
        PrintControllerDocumentHelper.getFilenameForReports(Arrays.asList(r1, r2, r3)));
  }
}
