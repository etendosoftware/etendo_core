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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.email.ResolvedSmtpConfig;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.poc.EmailInfo;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.erpCommon.utility.reporting.DocumentType;

/**
 * Tests for {@link EmailUtilities}.
 * Focuses on the testable static utility method getEmailValue.
 */
@SuppressWarnings({"java:S100", "java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class EmailUtilitiesTest {

  private static final String NO_SALES_REP_EMAIL = "NoSalesRepEmail";
  private static final String DEFAULT_TEST_COM = "default@test.com";
  private static final String ALT_TEST_COM = "alt@test.com";

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private MockedStatic<OBMessageUtils> obMessageUtilsStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);
    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD(NO_SALES_REP_EMAIL))
        .thenReturn("No sales rep email");
    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD("NoCustomerEmail"))
        .thenReturn("No customer email");
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obMessageUtilsStatic != null) {
      obMessageUtilsStatic.close();
    }
  }
  /**
   * Get email value condition true returns default.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testGetEmailValueConditionTrueReturnsDefault() throws ServletException {
    String result = EmailUtilities.getEmailValue(true, DEFAULT_TEST_COM, ALT_TEST_COM, false, "");
    assertEquals(DEFAULT_TEST_COM, result);
  }
  /**
   * Get email value condition false returns alternative.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testGetEmailValueConditionFalseReturnsAlternative() throws ServletException {
    String result = EmailUtilities.getEmailValue(false, DEFAULT_TEST_COM, ALT_TEST_COM, false, "");
    assertEquals(ALT_TEST_COM, result);
  }
  /**
   * Get email value null allowed when fail if empty false.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testGetEmailValueNullAllowedWhenFailIfEmptyFalse() throws ServletException {
    String result = EmailUtilities.getEmailValue(true, null, ALT_TEST_COM, false, "");
    assertNull(result);
  }
  /**
   * Get email value empty allowed when fail if empty false.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testGetEmailValueEmptyAllowedWhenFailIfEmptyFalse() throws ServletException {
    String result = EmailUtilities.getEmailValue(true, "", ALT_TEST_COM, false, "");
    assertEquals("", result);
  }
  /**
   * Get email value null throws when fail if empty true.
   * @throws ServletException if an error occurs
   */

  @Test(expected = ServletException.class)
  public void testGetEmailValueNullThrowsWhenFailIfEmptyTrue() throws ServletException {
    EmailUtilities.getEmailValue(true, null, ALT_TEST_COM, true, NO_SALES_REP_EMAIL);
  }
  /**
   * Get email value empty throws when fail if empty true.
   * @throws ServletException if an error occurs
   */

  @Test(expected = ServletException.class)
  public void testGetEmailValueEmptyThrowsWhenFailIfEmptyTrue() throws ServletException {
    EmailUtilities.getEmailValue(true, "", ALT_TEST_COM, true, NO_SALES_REP_EMAIL);
  }
  /**
   * Get email value non empty passes when fail if empty true.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testGetEmailValueNonEmptyPassesWhenFailIfEmptyTrue() throws ServletException {
    String result = EmailUtilities.getEmailValue(true, "valid@test.com", ALT_TEST_COM, true, NO_SALES_REP_EMAIL);
    assertEquals("valid@test.com", result);
  }
  /**
   * Get email value alternative null throws when fail if empty.
   * @throws ServletException if an error occurs
   */

  @Test(expected = ServletException.class)
  public void testGetEmailValueAlternativeNullThrowsWhenFailIfEmpty() throws ServletException {
    EmailUtilities.getEmailValue(false, DEFAULT_TEST_COM, null, true, "NoCustomerEmail");
  }

  // -------------------------------------------------------------------------
  // deleteTemporaryAttachments
  // -------------------------------------------------------------------------

  /** An empty attachments list completes without throwing.
   * @throws IOException never in this code path */
  @Test
  public void testDeleteTemporaryAttachments_emptyList_noException() throws IOException {
    EmailUtilities.deleteTemporaryAttachments(Collections.emptyList());
  }

  /** An existing regular file is deleted.
   * @throws IOException if temp file creation or deletion fails */
  @Test
  public void testDeleteTemporaryAttachments_existingFile_isDeleted() throws IOException {
    File tempFile = tempFolder.newFile("eut-test.tmp");
    assertTrue(tempFile.exists());

    EmailUtilities.deleteTemporaryAttachments(Collections.singletonList(tempFile));

    assertFalse(tempFile.exists());
  }

  /** A non-existent path is silently skipped.
   * @throws IOException if temp file creation or deletion fails */
  @Test
  public void testDeleteTemporaryAttachments_nonExistentFile_isSkipped() throws IOException {
    File ghost = tempFolder.newFile("ghost-eut.tmp");
    Files.delete(ghost.toPath());
    assertFalse(ghost.exists());
    EmailUtilities.deleteTemporaryAttachments(Collections.singletonList(ghost));
  }

  /** A directory is not deleted; only files are removed.
   * @throws IOException if temp dir creation fails */
  @Test
  public void testDeleteTemporaryAttachments_directory_isSkipped() throws IOException {
    File tempDir = tempFolder.newFolder("eut-dir-test");
    EmailUtilities.deleteTemporaryAttachments(Collections.singletonList(tempDir));
    assertTrue("Directory should still exist", tempDir.exists());
  }

  // -------------------------------------------------------------------------
  // getContactDetails
  // -------------------------------------------------------------------------

  /** DocumentType.UNKNOWN returns an empty array without any DB call.
   * @throws ServletException never in this code path */
  @Test
  public void testGetContactDetails_unknown_returnsEmptyArray() throws ServletException {
    PocData[] result = EmailUtilities.getContactDetails(
        DocumentType.UNKNOWN, "any-id", mock(ConnectionProvider.class));
    assertEquals(0, result.length);
  }

  /** DocumentType.SALESINVOICE delegates to PocData.getContactDetailsForInvoices.
   * @throws ServletException never in this code path */
  @Test
  public void testGetContactDetails_salesInvoice_callsGetContactDetailsForInvoices()
      throws ServletException {
    try (MockedStatic<PocData> pocDataStatic = mockStatic(PocData.class)) {
      pocDataStatic.when(() -> PocData.getContactDetailsForInvoices(any(), any()))
          .thenReturn(new PocData[0]);

      PocData[] result = EmailUtilities.getContactDetails(
          DocumentType.SALESINVOICE, "INV-001", mock(ConnectionProvider.class));

      assertEquals(0, result.length);
      pocDataStatic.verify(() -> PocData.getContactDetailsForInvoices(any(), any()));
    }
  }

  /** DocumentType.SHIPMENT delegates to PocData.getContactDetailsForShipments.
   * @throws ServletException never in this code path */
  @Test
  public void testGetContactDetails_shipment_callsGetContactDetailsForShipments()
      throws ServletException {
    try (MockedStatic<PocData> pocDataStatic = mockStatic(PocData.class)) {
      pocDataStatic.when(() -> PocData.getContactDetailsForShipments(any(), any()))
          .thenReturn(new PocData[0]);

      PocData[] result = EmailUtilities.getContactDetails(
          DocumentType.SHIPMENT, "SHIP-001", mock(ConnectionProvider.class));

      assertEquals(0, result.length);
      pocDataStatic.verify(() -> PocData.getContactDetailsForShipments(any(), any()));
    }
  }

  // -------------------------------------------------------------------------
  // sendEmail(ResolvedSmtpConfig, EmailInfo, List<File>)
  // -------------------------------------------------------------------------

  /** On success, temporary attachment files are deleted via the finally block.
   * @throws ServletException if EmailManager throws (not expected here)
   * @throws IOException if temp file creation fails */
  @Test
  public void testSendEmail_resolvedConfig_success_deletesAttachments()
      throws ServletException, IOException {
    File tempFile = tempFolder.newFile("eut-send-test.tmp");
    List<File> files = new ArrayList<>();
    files.add(tempFile);

    try (MockedStatic<EmailManager> emailMgr = mockStatic(EmailManager.class)) {
      EmailUtilities.sendEmail(mock(ResolvedSmtpConfig.class), mock(EmailInfo.class), files);
    }

    assertFalse("Temp file should be deleted after sending", tempFile.exists());
  }

  /** When EmailManager throws, ServletException is rethrown and attachments are still deleted.
   * @throws IOException if temp file creation fails */
  @Test
  public void testSendEmail_resolvedConfig_emailManagerThrows_rethrowsServletException()
      throws IOException {
    File tempFile = tempFolder.newFile("eut-fail-test.tmp");
    List<File> files = new ArrayList<>();
    files.add(tempFile);

    try (MockedStatic<EmailManager> emailMgr = mockStatic(EmailManager.class)) {
      emailMgr.when(() -> EmailManager.sendEmail(
          any(ResolvedSmtpConfig.class), any(EmailInfo.class)))
          .thenThrow(new Exception("SMTP failure"));

      boolean exceptionCaught = false;
      try {
        EmailUtilities.sendEmail(mock(ResolvedSmtpConfig.class), mock(EmailInfo.class), files);
      } catch (ServletException e) {
        exceptionCaught = true;
        assertTrue(e.getMessage().contains("Problems while sending the email"));
      }
      assertTrue("Expected ServletException to be thrown", exceptionCaught);
    }

    assertFalse("Temp file should be deleted even after failure", tempFile.exists());
  }
}
