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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Unit tests for {@link ReportingUtils} and its inner {@link ExportType} enum.
 * <p>
 * The {@code ExportType} enum calls {@code OBDal} and {@code OBContext} during
 * static initialization (via {@code getContentType}), so we mock those statics
 * before the enum class is loaded.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportingUtils")
class ReportingUtilsTest {

  private static MockedStatic<OBDal> obDalStatic;
  private static MockedStatic<OBContext> obContextStatic;
  private static MockedStatic<OBMessageUtils> obMessageUtilsStatic;

  @BeforeAll
  static void setUpStaticMocks() {
    obDalStatic = mockStatic(OBDal.class);
    OBDal mockDal = mock(OBDal.class);
    obDalStatic.when(OBDal::getReadOnlyInstance).thenReturn(mockDal);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockDal);
    lenient().when(mockDal.get(any(), anyString())).thenReturn(null);

    obContextStatic = mockStatic(OBContext.class);

    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);
    lenient().when(OBMessageUtils.getI18NMessage(anyString(), any(String[].class)))
        .thenAnswer(invocation -> "Unsupported action: " + ((String[]) invocation.getArgument(1))[0]);
  }

  @AfterAll
  static void tearDownStaticMocks() {
    if (obMessageUtilsStatic != null) {
      obMessageUtilsStatic.close();
    }
    if (obContextStatic != null) {
      obContextStatic.close();
    }
    if (obDalStatic != null) {
      obDalStatic.close();
    }
  }

  // ---------------------------------------------------------------
  // ExportType.getExportType
  // ---------------------------------------------------------------
  @Nested
  @DisplayName("ExportType.getExportType()")
  class GetExportTypeTests {

    @ParameterizedTest(name = "getExportType(\"{0}\") should return {1}")
    @DisplayName("Returns correct ExportType for valid lowercase inputs")
    @CsvSource({
        "csv, CSV",
        "html, HTML",
        "pdf, PDF",
        "txt, TXT",
        "xls, XLS",
        "xlsx, XLSX",
        "xml, XML"
    })
    void shouldReturnCorrectTypeForValidInput(String input, String expectedName) {
      ExportType result = ExportType.getExportType(input);
      assertEquals(expectedName, result.name());
    }

    @ParameterizedTest(name = "getExportType(\"{0}\") should be case insensitive")
    @DisplayName("Is case insensitive")
    @CsvSource({
        "CSV, CSV",
        "Html, HTML",
        "PDF, PDF",
        "Txt, TXT",
        "XLS, XLS",
        "XLSX, XLSX",
        "Xml, XML"
    })
    void shouldBeCaseInsensitive(String input, String expectedName) {
      ExportType result = ExportType.getExportType(input);
      assertEquals(expectedName, result.name());
    }

    @ParameterizedTest(name = "getExportType(\"{0}\") should throw OBException")
    @DisplayName("Throws OBException for invalid input")
    @ValueSource(strings = { "doc", "odt", "json", "unknown", "pdfx" })
    void shouldThrowForInvalidInput(String input) {
      assertThrows(OBException.class, () -> ExportType.getExportType(input));
    }
  }

  // ---------------------------------------------------------------
  // ExportType.hasExtension
  // ---------------------------------------------------------------
  @Nested
  @DisplayName("ExportType.hasExtension()")
  class HasExtensionTests {

    @ParameterizedTest(name = "hasExtension(null or empty) should return false")
    @DisplayName("Returns false for null and empty strings")
    @NullAndEmptySource
    void shouldReturnFalseForNullOrEmpty(String input) {
      assertFalse(ExportType.PDF.hasExtension(input));
    }

    @Test
    @DisplayName("Returns true when extension matches")
    void shouldReturnTrueForMatchingExtension() {
      assertTrue(ExportType.PDF.hasExtension("pdf"));
      assertTrue(ExportType.CSV.hasExtension("csv"));
      assertTrue(ExportType.XLSX.hasExtension("xlsx"));
    }

    @Test
    @DisplayName("Is case insensitive")
    void shouldBeCaseInsensitive() {
      assertTrue(ExportType.PDF.hasExtension("PDF"));
      assertTrue(ExportType.CSV.hasExtension("Csv"));
      assertTrue(ExportType.HTML.hasExtension("HTML"));
    }

    @Test
    @DisplayName("Returns false when extension does not match")
    void shouldReturnFalseForNonMatchingExtension() {
      assertFalse(ExportType.PDF.hasExtension("csv"));
      assertFalse(ExportType.XLS.hasExtension("xlsx"));
      assertFalse(ExportType.HTML.hasExtension("xml"));
    }
  }

  // ---------------------------------------------------------------
  // ExportType.isValidTemporaryFileName
  // ---------------------------------------------------------------
  @Nested
  @DisplayName("ExportType.isValidTemporaryFileName()")
  class IsValidTemporaryFileNameTests {

    private static final String VALID_UUID = "550e8400-e29b-41d4-a716-446655440000";

    @Test
    @DisplayName("Returns true for a valid UUID filename with correct extension")
    void shouldReturnTrueForValidUuidFilename() {
      assertTrue(ExportType.PDF.isValidTemporaryFileName(VALID_UUID + ".pdf"));
      assertTrue(ExportType.CSV.isValidTemporaryFileName(VALID_UUID + ".csv"));
      assertTrue(ExportType.XLSX.isValidTemporaryFileName(VALID_UUID + ".xlsx"));
    }

    @Test
    @DisplayName("Returns false when extension does not match")
    void shouldReturnFalseForWrongExtension() {
      assertFalse(ExportType.PDF.isValidTemporaryFileName(VALID_UUID + ".csv"));
      assertFalse(ExportType.XLS.isValidTemporaryFileName(VALID_UUID + ".xlsx"));
    }

    @Test
    @DisplayName("Returns false for non-UUID filename")
    void shouldReturnFalseForNonUuidFilename() {
      assertFalse(ExportType.PDF.isValidTemporaryFileName("myreport.pdf"));
      assertFalse(ExportType.PDF.isValidTemporaryFileName("not-a-uuid-at-all.pdf"));
    }

    @Test
    @DisplayName("Returns false for UUID without hyphens")
    void shouldReturnFalseForUuidWithoutHyphens() {
      assertFalse(ExportType.PDF.isValidTemporaryFileName("550e8400e29b41d4a716446655440000.pdf"));
    }

    @Test
    @DisplayName("Returns false for filename missing extension")
    void shouldReturnFalseForMissingExtension() {
      assertFalse(ExportType.PDF.isValidTemporaryFileName(VALID_UUID));
    }
  }

  // ---------------------------------------------------------------
  // ExportType.getExtension
  // ---------------------------------------------------------------
  @Nested
  @DisplayName("ExportType.getExtension()")
  class GetExtensionTests {

    @ParameterizedTest(name = "{0} should have extension \"{1}\"")
    @DisplayName("Returns the correct extension for each type")
    @CsvSource({
        "CSV, csv",
        "HTML, html",
        "PDF, pdf",
        "TXT, txt",
        "XLS, xls",
        "XLSX, xlsx",
        "XML, xml"
    })
    void shouldReturnCorrectExtension(String typeName, String expectedExtension) {
      ExportType type = ExportType.valueOf(typeName);
      assertEquals(expectedExtension, type.getExtension());
    }
  }

  // ---------------------------------------------------------------
  // ExportType.getExportParameters (IS_IGNORE_PAGINATION)
  // ---------------------------------------------------------------
  @Nested
  @DisplayName("ExportType.getExportParameters()")
  class GetExportParametersTests {

    @ParameterizedTest(name = "{0} should have IS_IGNORE_PAGINATION = true")
    @DisplayName("IS_IGNORE_PAGINATION is true for non-page-oriented formats")
    @ValueSource(strings = { "CSV", "HTML", "XLS", "XLSX", "XML", "TXT" })
    void shouldHaveIgnorePaginationTrue(String typeName) {
      ExportType type = ExportType.valueOf(typeName);
      Map<String, Object> params = type.getExportParameters();
      assertEquals(Boolean.TRUE, params.get("IS_IGNORE_PAGINATION"));
    }

    @Test
    @DisplayName("IS_IGNORE_PAGINATION is false for PDF")
    void pdfShouldHaveIgnorePaginationFalse() {
      Map<String, Object> params = ExportType.PDF.getExportParameters();
      assertEquals(Boolean.FALSE, params.get("IS_IGNORE_PAGINATION"));
    }

    @Test
    @DisplayName("Returns a defensive copy of the parameters map")
    void shouldReturnDefensiveCopy() {
      Map<String, Object> params1 = ExportType.PDF.getExportParameters();
      Map<String, Object> params2 = ExportType.PDF.getExportParameters();
      params1.put("extra", "value");
      assertFalse(params2.containsKey("extra"),
          "Modifying one returned map should not affect subsequent calls");
    }
  }

  // ---------------------------------------------------------------
  // ReportingUtils.getTempFolder
  // ---------------------------------------------------------------
  @Nested
  @DisplayName("ReportingUtils.getTempFolder()")
  class GetTempFolderTests {

    @Test
    @DisplayName("Returns a non-null value")
    void shouldReturnNonNull() {
      assertNotNull(ReportingUtils.getTempFolder());
    }

    @Test
    @DisplayName("Returns System.getProperty(\"java.io.tmpdir\")")
    void shouldEqualSystemTmpDir() {
      assertEquals(System.getProperty("java.io.tmpdir"), ReportingUtils.getTempFolder());
    }
  }

  // ---------------------------------------------------------------
  // ReportingUtils.getLimit
  // ---------------------------------------------------------------
  @Nested
  @DisplayName("ReportingUtils.getLimit()")
  class GetLimitTests {

    @Test
    @DisplayName("Returns 1048576 for xlsx")
    void shouldReturn1048576ForXlsx() {
      assertEquals(1048576, ReportingUtils.getLimit("xlsx"));
    }

    @Test
    @DisplayName("Returns 1048576 for XLSX (case insensitive)")
    void shouldReturn1048576ForXlsxUpperCase() {
      assertEquals(1048576, ReportingUtils.getLimit("XLSX"));
    }

    @ParameterizedTest(name = "getLimit(\"{0}\") should return 65532")
    @DisplayName("Returns 65532 for non-xlsx types")
    @ValueSource(strings = { "xls", "csv", "pdf", "html", "txt", "xml", "unknown" })
    void shouldReturn65532ForOtherTypes(String action) {
      assertEquals(65532, ReportingUtils.getLimit(action));
    }
  }

  // ---------------------------------------------------------------
  // correctMaskForGrouping (private method, tested via reflection)
  // ---------------------------------------------------------------
  @Nested
  @DisplayName("correctMaskForGrouping() via reflection")
  class CorrectMaskForGroupingTests {

    private String invokeCorrectMaskForGrouping(String mask, String decimalSymbol,
        String groupingSymbol) throws Exception {
      Method method = ReportingUtils.class.getDeclaredMethod(
          "correctMaskForGrouping", String.class, String.class, String.class);
      method.setAccessible(true);
      return (String) method.invoke(null, mask, decimalSymbol, groupingSymbol);
    }

    @Test
    @DisplayName("Converts standard US format mask correctly")
    void shouldHandleStandardUsMask() throws Exception {
      // US format uses . as decimal and , as grouping
      // The method should normalize to . and , which is the same
      String result = invokeCorrectMaskForGrouping("#,##0.00", ".", ",");
      assertEquals("#,##0.00", result);
    }

    @Test
    @DisplayName("Converts European format mask (comma decimal, dot grouping)")
    void shouldHandleEuropeanMask() throws Exception {
      // European format: , as decimal, . as grouping
      // Input mask: #.##0,00 -> should become #,##0.00
      String result = invokeCorrectMaskForGrouping("#.##0,00", ",", ".");
      assertEquals("#,##0.00", result);
    }

    @Test
    @DisplayName("Handles mask with space as grouping symbol")
    void shouldHandleSpaceGrouping() throws Exception {
      // Space as grouping, comma as decimal
      String result = invokeCorrectMaskForGrouping("# ##0,00", ",", " ");
      assertEquals("#,##0.00", result);
    }

    @Test
    @DisplayName("Does not throw for mask without grouping or decimal")
    void shouldNotThrowForPlainMask() {
      assertDoesNotThrow(() -> invokeCorrectMaskForGrouping("###0", ".", ","));
    }
  }
}
