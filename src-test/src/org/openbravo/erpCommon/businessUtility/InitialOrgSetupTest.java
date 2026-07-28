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
package org.openbravo.erpCommon.businessUtility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.BaseCoreTest;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Language;

/**
 * Unit tests for {@link InitialOrgSetup}.
 */
@DisplayName("InitialOrgSetup")
public class InitialOrgSetupTest extends BaseCoreTest {

  private InitialOrgSetup createSetup() {
    Language mockLang = mock(Language.class);
    when(obContext.getLanguage()).thenReturn(mockLang);
    return new InitialOrgSetup(client);
  }

  // ── Constructor, getLog, getOrgId ─────────────────────────────────────

  @Nested
  @DisplayName("Constructor, getLog, getOrgId")
  class ConstructorTests {
    @Test
    @DisplayName("constructor creates instance")
    void constructorCreatesInstance() {
      InitialOrgSetup setup = createSetup();
      assertNotNull(setup);
    }

    @Test
    @DisplayName("getLog returns non-null")
    void getLogNonNull() {
      InitialOrgSetup setup = createSetup();
      assertNotNull(setup.getLog());
    }

    @Test
    @DisplayName("getOrgId returns empty string initially")
    void getOrgIdReturnsEmpty() {
      InitialOrgSetup setup = createSetup();
      assertEquals("", setup.getOrgId());
    }
  }

  // ── cleanUpStrModules (private) ───────────────────────────────────────

  @Nested
  @DisplayName("cleanUpStrModules (private)")
  class CleanUpStrModules {

    private String invoke(InitialOrgSetup setup, String input) throws Exception {
      Method method = InitialOrgSetup.class.getDeclaredMethod(
          "cleanUpStrModules", String.class);
      method.setAccessible(true);
      return (String) method.invoke(setup, input);
    }

    @Test void nullReturnsEmpty() throws Exception {
      assertEquals("", invoke(createSetup(), null));
    }

    @Test void emptyReturnsEmpty() throws Exception {
      assertEquals("", invoke(createSetup(), ""));
    }

    @Test void wrappedParensStripped() throws Exception {
      assertEquals("abc", invoke(createSetup(), "(abc)"));
    }

    @Test void noParensUnchanged() throws Exception {
      assertEquals("abc", invoke(createSetup(), "abc"));
    }

    @Test void leadingParenOnly() throws Exception {
      assertEquals("abc", invoke(createSetup(), "(abc"));
    }

    @Test void trailingParenOnly() throws Exception {
      assertEquals("abc", invoke(createSetup(), "abc)"));
    }
  }

  // ── coaModule ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("coaModule")
  class CoaModule {

    @Test
    @DisplayName("coaModule returns error when no CoA modules found")
    void noCoaModulesReturnsError() {
      try (MockedStatic<InitialSetupUtility> utilMock = mockStatic(InitialSetupUtility.class)) {
        List<Module> emptyList = new ArrayList<>();
        utilMock.when(() -> InitialSetupUtility.getCOAModules(anyString())).thenReturn(emptyList);

        InitialOrgSetup setup = createSetup();
        OBError result = setup.coaModule("someModule");
        assertEquals("Error", result.getType());
      }
    }

    @Test
    @DisplayName("coaModule returns success when CoA module exists")
    void coaModuleExistsReturnsSuccess() {
      try (MockedStatic<InitialSetupUtility> utilMock = mockStatic(InitialSetupUtility.class)) {
        List<Module> moduleList = new ArrayList<>();
        moduleList.add(mock(Module.class));
        utilMock.when(() -> InitialSetupUtility.getCOAModules(anyString())).thenReturn(moduleList);

        InitialOrgSetup setup = createSetup();
        OBError result = setup.coaModule("(someModule)");
        assertEquals("Success", result.getType());
      }
    }
  }
}
