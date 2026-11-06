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

package org.openbravo.test.base;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Language;
import org.openbravo.service.db.CallProcess;

/**
 * Makes a language a system language for a test class, so translations are looked up: core only
 * reads {@code *_Trl} rows when more than one system language is installed. Call
 * {@link #install()} from {@code @BeforeClass} and {@link #restore()} from {@code @AfterClass}.
 * Restoring clears the system flag only; the translation rows {@code AD_Language_Create} added
 * stay, as with {@code ReducedTranslationExportTests}.
 */
public class SystemLanguageFixture {
  private final String languageId;
  private boolean installedHere = false;

  public SystemLanguageFixture(String languageId) {
    this.languageId = languageId;
  }

  /** Makes the language a system language unless it already is, and commits. */
  public void install() {
    try {
      OBContext.setAdminMode(false);
      final Language language = OBDal.getInstance().get(Language.class, languageId);
      if (!language.isSystemLanguage()) {
        language.setSystemLanguage(true);
        installedHere = true;
        CallProcess.getInstance().call("AD_Language_Create", null, null);
      }
      OBDal.getInstance().commitAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /** Clears the system flag again when {@link #install()} set it, and commits. */
  public void restore() {
    if (!installedHere) {
      return;
    }
    try {
      OBContext.setAdminMode(false);
      final Language language = OBDal.getInstance().get(Language.class, languageId);
      language.setSystemLanguage(false);
      OBDal.getInstance().save(language);
      OBDal.getInstance().commitAndClose();
      installedHere = false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
