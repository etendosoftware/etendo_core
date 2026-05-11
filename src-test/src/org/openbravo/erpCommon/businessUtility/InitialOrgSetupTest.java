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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.Language;

/**
 * Unit tests for {@link InitialOrgSetup}.
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InitialOrgSetup")
public class InitialOrgSetupTest {

  @Mock
  private Client clientMock;
  @Mock
  private OBContext obContextMock;
  @Mock
  private Language languageMock;
  @Mock
  private OBDal obDalMock;

  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<OBDal> obDalStatic;

  private InitialOrgSetup setup;

  @BeforeEach
  void setUp() {
    obContextStatic = mockStatic(OBContext.class);
    obDalStatic = mockStatic(OBDal.class);

    obContextStatic.when(OBContext::getOBContext).thenReturn(obContextMock);
    when(obContextMock.getLanguage()).thenReturn(languageMock);

    obDalStatic.when(OBDal::getInstance).thenReturn(obDalMock);

    setup = new InitialOrgSetup(clientMock);
  }

  @AfterEach
  void tearDown() {
    obContextStatic.close();
    obDalStatic.close();
  }

  // ── Constructor ──────────────────────────────────────────────────────

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("should initialize log as non-null")
    void logIsNotNull() {
      assertNotNull(setup.getLog());
    }

    @Test
    @DisplayName("should return empty orgId when org is null")
    void orgIdIsEmptyWhenOrgIsNull() {
      assertEquals("", setup.getOrgId());
    }
  }

  // ── getLog ───────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getLog")
  class GetLogTests {

    @Test
    @DisplayName("should contain header, separator, and log content")
    void logContainsSeparator() {
      String log = setup.getLog();
      assertTrue(log.contains("*****************************************************"),
          "Log should contain the separator line");
    }
  }

  // ── getOrgId ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getOrgId")
  class GetOrgIdTests {

    @Test
    @DisplayName("should return empty string when org is null")
    void returnsEmptyWhenOrgIsNull() {
      assertEquals("", setup.getOrgId());
    }
  }

  // ── cleanUpStrModules (private, tested via reflection) ───────────────

  @Nested
  @DisplayName("cleanUpStrModules")
  class CleanUpStrModulesTests {

    private String invokeCleanUp(String input) throws Exception {
      Method method = InitialOrgSetup.class.getDeclaredMethod("cleanUpStrModules", String.class);
      method.setAccessible(true);
      return (String) method.invoke(setup, input);
    }

    @Test
    @DisplayName("null input returns empty string")
    void nullReturnsEmpty() throws Exception {
      assertEquals("", invokeCleanUp(null));
    }

    @Test
    @DisplayName("empty input returns empty string")
    void emptyReturnsEmpty() throws Exception {
      assertEquals("", invokeCleanUp(""));
    }

    @Test
    @DisplayName("strips surrounding parentheses")
    void stripsParentheses() throws Exception {
      assertEquals("abc", invokeCleanUp("(abc)"));
    }

    @Test
    @DisplayName("no parentheses returns same string")
    void noParenthesesReturnsSame() throws Exception {
      assertEquals("abc", invokeCleanUp("abc"));
    }

    @Test
    @DisplayName("strips only leading parenthesis")
    void stripsLeadingParenthesis() throws Exception {
      assertEquals("abc", invokeCleanUp("(abc"));
    }

    @Test
    @DisplayName("strips only trailing parenthesis")
    void stripsTrailingParenthesis() throws Exception {
      assertEquals("abc", invokeCleanUp("abc)"));
    }
  }

  // ── coaModule (package-private) ──────────────────────────────────────

  @Nested
  @DisplayName("coaModule")
  class CoaModuleTests {

    @Test
    @DisplayName("should return error when no CoA modules found")
    void errorWhenNoCoaModules() {
      try (MockedStatic<InitialSetupUtility> isuMock = mockStatic(InitialSetupUtility.class)) {
        isuMock.when(() -> InitialSetupUtility.getCOAModules(anyString()))
            .thenReturn(Collections.emptyList());

        OBError result = setup.coaModule("");
        assertEquals("Error", result.getType());
        assertTrue(result.getMessage().contains("@CreateReferenceDataFailed@"));
      }
    }

    @Test
    @DisplayName("should return success when exactly one CoA module found")
    void successWhenOneCoaModule() {
      try (MockedStatic<InitialSetupUtility> isuMock = mockStatic(InitialSetupUtility.class)) {
        Module module = mock(Module.class);
        List<Module> modules = new ArrayList<>();
        modules.add(module);
        isuMock.when(() -> InitialSetupUtility.getCOAModules(anyString()))
            .thenReturn(modules);

        OBError result = setup.coaModule("");
        assertEquals("Success", result.getType());
      }
    }
  }
}
