/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.test.reducedtranslation;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_forms.TranslationManager;
import org.openbravo.model.ad.system.Language;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.base.TestConstants;

/**
 * Tests that checks some examples of expected scenarios where the reduced translation version
 * should exclude or include some terms
 */
public class ReducedTranslationExportTests extends OBBaseTest {
  private static boolean restoreSystemLanguageFlag = false;

  @BeforeClass
  public static void exportReducedTranslation() throws IOException {
    doVerifyLanguageIfNotPreviouslyDoneBefore();
    exportReducedAndFullTranslations();
  }

  private static void doVerifyLanguageIfNotPreviouslyDoneBefore() {
    try {
      OBContext.setAdminMode(false);
      final Language lang = OBDal.getInstance()
          .get(Language.class, TestConstants.Languages.SQ_AL_LANG_ID);
      if (!lang.isSystemLanguage()) {
        lang.setSystemLanguage(true);
        restoreSystemLanguageFlag = true;
        CallProcess.getInstance().call("AD_Language_Create", null, null);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private static void exportReducedAndFullTranslations() throws IOException {
    Files.createDirectories(ReducedTrlTestConstants.REDUCED_TRL_DIR);

    TranslationManager.exportTrl(new DalConnectionProvider(),
        ReducedTrlTestConstants.REDUCED_TRL_DIR.toAbsolutePath().toString(),
        TestConstants.Languages.SQ_AL_ISOCODE, TestConstants.Clients.SYSTEM,
        TestConstants.Languages.SQ_AL_ISOCODE, true);

    Files.createDirectories(ReducedTrlTestConstants.FULL_TRL_DIR);
    TranslationManager.exportTrl(new DalConnectionProvider(),
        ReducedTrlTestConstants.FULL_TRL_DIR.toAbsolutePath().toString(),
        TestConstants.Languages.SQ_AL_ISOCODE, TestConstants.Clients.SYSTEM,
        TestConstants.Languages.SQ_AL_ISOCODE, false);
  }

  @AfterClass
  public static void removeTranslationFolders() throws IOException {
    FileUtils.deleteDirectory(ReducedTrlTestConstants.REDUCED_TRL_DIR.getFileName().toFile());
    FileUtils.deleteDirectory(ReducedTrlTestConstants.FULL_TRL_DIR.getFileName().toFile());
  }

  @AfterClass
  public static void resetSystemLanguageFlag() {
    try {
      OBContext.setAdminMode(false);
      if (restoreSystemLanguageFlag) {
        final Language lang = OBDal.getInstance()
            .get(Language.class, TestConstants.Languages.SQ_AL_LANG_ID);
        lang.setSystemLanguage(false);
        OBDal.getInstance().save(lang);
        OBDal.getInstance().flush();
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testReducedTranslationSize() {
    assertThat("Reduced translation size",
        FileUtils.sizeOfDirectory(ReducedTrlTestConstants.REDUCED_TRL_DIR.toFile()),
        lessThan(FileUtils.sizeOfDirectory(ReducedTrlTestConstants.FULL_TRL_DIR.toFile())));
  }

  @Test
  public void testAdTextInterfacesJrxml() throws IOException {
    testExistInFullAndNotExistsInReduced(
        "lang/sq_AL/org.openbravo.financial.paymentreport/AD_TEXTINTERFACES_TRL_sq_AL.xml",
        "ACCS_ACCOUNT_ID_D");
  }

  @Test
  public void testAdElementDirectAccessInWindowExcludingTranslation() throws IOException {
    testExistInFullAndNotExistsInReduced("lang/sq_AL/AD_ELEMENT_TRL_sq_AL.xml", "\"SO Sub Type\"");
  }

  @Test
  public void testAdElementIndirectAccessInWindowReferenceExcludingTranslation()
      throws IOException {
    testExistInFullAndNotExistsInReduced(
        "lang/sq_AL/org.openbravo.client.application/AD_ELEMENT_TRL_sq_AL.xml", "\"Logger\"");
  }

  @Test
  public void testAdElementIndirectProcessDefinitionIncludedTranslation() throws IOException {
    testExistInBothTranslations("lang/sq_AL/AD_ELEMENT_TRL_sq_AL.xml", "\"LC Costs\"");
  }

  @Test
  public void testAdElementDirectProcessIncludedTranslation() throws IOException {
    testExistInBothTranslations("lang/sq_AL/AD_ELEMENT_TRL_sq_AL.xml", "\"Export Audit Info\"");
  }

  @Test
  public void testAdFieldExcludedTranslation() throws IOException {
    testExistInFullAndNotExistsInReduced("lang/sq_AL/AD_FIELD_TRL_sq_AL.xml",
        "\"Copy from Attribute\"");
  }

  @Test
  public void testAdProcessAdMenuExcludedTranslation() throws IOException {
    final String string = "\"Project Status Summary\"";
    testExistInFullAndNotExistsInReduced("lang/sq_AL/AD_PROCESS_TRL_sq_AL.xml", string);
    testExistInFullAndNotExistsInReduced("lang/sq_AL/AD_MENU_TRL_sq_AL.xml", string);
  }

  @Test
  public void testProcessDefinitionAdMenuAdWindowExcludedTranslation() throws IOException {
    final String string = "\"Log Management\"";
    testExistInFullAndNotExistsInReduced(
        "lang/sq_AL/org.openbravo.client.application/OBUIAPP_PROCESS_TRL_sq_AL.xml", string);
    testExistInFullAndNotExistsInReduced(
        "lang/sq_AL/org.openbravo.client.application/AD_MENU_TRL_sq_AL.xml", string);
    testExistInFullAndNotExistsInReduced(
        "lang/sq_AL/org.openbravo.client.application/AD_WINDOW_TRL_sq_AL.xml", string);
  }

  @Test
  public void testAdProcessParaExcludedTranslation() throws IOException {
    testExistInFullAndNotExistsInReduced("lang/sq_AL/AD_PROCESS_PARA_TRL_sq_AL.xml",
        "Target Payment Rule");
  }

  @Test
  public void testAdWindowAdMenuExcludedTranslation() throws IOException {
    final String string = "\"Windows, Tabs, and Fields\"";
    testExistInFullAndNotExistsInReduced("lang/sq_AL/AD_WINDOW_TRL_sq_AL.xml", string);
    testExistInFullAndNotExistsInReduced("lang/sq_AL/AD_MENU_TRL_sq_AL.xml", string);
  }

  @Test
  public void testAdTabAdElementExcludedTranslation() throws IOException {
    final String string = "\"Window Translation\"";
    testExistInFullAndNotExistsInReduced("lang/sq_AL/AD_TAB_TRL_sq_AL.xml", string);
    testExistInFullAndNotExistsInReduced("lang/sq_AL/AD_ELEMENT_TRL_sq_AL.xml", string);
  }

  @Test
  public void testAdFieldGroupExcludedTranslation() throws IOException {
    testExistInFullAndNotExistsInReduced("lang/sq_AL/AD_FIELDGROUP_TRL_sq_AL.xml", "\"Assets\"");
  }

  @Test
  public void testAdRefListExcludedTranslation() throws IOException {
    testExistInFullAndNotExistsInReduced("lang/sq_AL/AD_REF_LIST_TRL_sq_AL.xml", "\"Servlet\"");
  }

  @Test
  public void expectedEmptyTranslationFiles() {
    Stream.of("lang/sq_AL/AD_REFERENCE_TRL_sq_AL.xml", "lang/sq_AL/OBUISEL_SELECTOR_TRL_sq_AL.xml")
        .forEach(this::testExpectedEmptyTranslationFile);
  }

  private void testExistInFullAndNotExistsInReduced(final String relativePath, final String string)
      throws IOException {
    assertThat("Full version " + relativePath,
        Files.readString(ReducedTrlTestConstants.FULL_TRL_DIR.resolve(relativePath)),
        containsString(string));
    assertThat("Reduced version " + relativePath,
        Files.readString(ReducedTrlTestConstants.REDUCED_TRL_DIR.resolve(relativePath)),
        not(containsString(string)));
  }

  private void testExistInBothTranslations(final String relativePath, final String string)
      throws IOException {
    assertThat("Full version " + relativePath,
        Files.readString(ReducedTrlTestConstants.FULL_TRL_DIR.resolve(relativePath)),
        containsString(string));
    assertThat("Reduced version " + relativePath,
        Files.readString(ReducedTrlTestConstants.REDUCED_TRL_DIR.resolve(relativePath)),
        containsString(string));
  }

  private void testExpectedEmptyTranslationFile(final String relativePath) {
    final long lineCountReduced = countLines(ReducedTrlTestConstants.REDUCED_TRL_DIR, relativePath);
    final long lineCountFull = countLines(ReducedTrlTestConstants.FULL_TRL_DIR, relativePath);

    final long xmlHeaderLinesOnly = 2l;
    assertThat("Reduced translation " + relativePath, lineCountReduced,
        equalTo(xmlHeaderLinesOnly));
    assertThat("Reduced translation " + relativePath, lineCountReduced, lessThan(lineCountFull));
  }

  private int countLines(final Path translationDir, final String fileRelativePath) {
    try {
      return Files.readAllLines(translationDir.resolve(fileRelativePath)).size();
    } catch (IOException e) {
      throw new OBException(e);
    }
  }

}
