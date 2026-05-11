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
package org.openbravo.client.application.window;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.dal.service.OBDal;

/**
 * Unit tests for {@link ApplicationDictionaryCachedStructures}.
 */
@DisplayName("ApplicationDictionaryCachedStructures Tests")
public class ApplicationDictionaryCachedStructuresTest {

  private static final String IN_DEVELOPMENT_MODULES_FIELD = "inDevelopmentModules";
  private static final String USE_CACHE_FIELD = "useCache";
  private static final String MODULE_ID_UNKNOWN = "UNKNOWN_MODULE_999";
  private static final String MODULE_ID_DEV = "DEV_MODULE_001";

  private MockedStatic<OBDal> obDalMock;
  private OBDal dalInstance;
  private Session sessionMock;

  @BeforeEach
  void setUp() {
    dalInstance = mock(OBDal.class);
    sessionMock = mock(Session.class);

    obDalMock = mockStatic(OBDal.class);
    obDalMock.when(OBDal::getInstance).thenReturn(dalInstance);
    when(dalInstance.getSession()).thenReturn(sessionMock);
  }

  @AfterEach
  void tearDown() {
    obDalMock.close();
  }

  /**
   * Creates an instance bypassing the constructor, then sets internal fields via reflection.
   */
  private ApplicationDictionaryCachedStructures createInstanceWithModules(
      Set<String> devModules) throws Exception {
    ApplicationDictionaryCachedStructures instance = new ApplicationDictionaryCachedStructures();

    // Mock the query that getModulesInDevelopment() executes
    @SuppressWarnings("unchecked")
    Query<String> queryMock = mock(Query.class);
    when(sessionMock.createQuery(anyString(), eq(String.class))).thenReturn(queryMock);
    when(queryMock.list()).thenReturn(new java.util.ArrayList<>(devModules));

    // Call init() which triggers getModulesInDevelopment()
    instance.init();

    return instance;
  }

  @Nested
  @DisplayName("init and useCache tests")
  class InitAndUseCacheTests {

    @Test
    @DisplayName("useCache should return true when no modules in development")
    void useCacheShouldReturnTrueWhenNoModulesInDev() throws Exception {
      ApplicationDictionaryCachedStructures instance = createInstanceWithModules(
          Collections.emptySet());
      assertTrue(instance.useCache());
    }

    @Test
    @DisplayName("useCache should return false when modules are in development")
    void useCacheShouldReturnFalseWhenModulesInDev() throws Exception {
      Set<String> devModules = new HashSet<>();
      devModules.add(MODULE_ID_DEV);
      ApplicationDictionaryCachedStructures instance = createInstanceWithModules(devModules);
      assertFalse(instance.useCache());
    }
  }

  @Nested
  @DisplayName("isInDevelopment() tests")
  class IsInDevelopmentNoArgTests {

    @Test
    @DisplayName("Should return false when no modules in development")
    void shouldReturnFalseWhenNoModulesInDev() throws Exception {
      ApplicationDictionaryCachedStructures instance = createInstanceWithModules(
          Collections.emptySet());
      assertFalse(instance.isInDevelopment());
    }

    @Test
    @DisplayName("Should return true when modules are in development")
    void shouldReturnTrueWhenModulesInDev() throws Exception {
      Set<String> devModules = new HashSet<>();
      devModules.add(MODULE_ID_DEV);
      ApplicationDictionaryCachedStructures instance = createInstanceWithModules(devModules);
      assertTrue(instance.isInDevelopment());
    }
  }

  @Nested
  @DisplayName("isInDevelopment(String) tests")
  class IsInDevelopmentWithModuleIdTests {

    @Test
    @DisplayName("Should return false for unknown module ID when no modules in dev")
    void shouldReturnFalseForUnknownModuleId() throws Exception {
      ApplicationDictionaryCachedStructures instance = createInstanceWithModules(
          Collections.emptySet());
      assertFalse(instance.isInDevelopment(MODULE_ID_UNKNOWN));
    }

    @Test
    @DisplayName("Should return true for known in-development module ID")
    void shouldReturnTrueForKnownDevModuleId() throws Exception {
      Set<String> devModules = new HashSet<>();
      devModules.add(MODULE_ID_DEV);
      ApplicationDictionaryCachedStructures instance = createInstanceWithModules(devModules);
      assertTrue(instance.isInDevelopment(MODULE_ID_DEV));
    }

    @Test
    @DisplayName("Should return false for unknown module ID when other modules in dev")
    void shouldReturnFalseForUnknownModuleWhenOthersInDev() throws Exception {
      Set<String> devModules = new HashSet<>();
      devModules.add(MODULE_ID_DEV);
      ApplicationDictionaryCachedStructures instance = createInstanceWithModules(devModules);
      assertFalse(instance.isInDevelopment(MODULE_ID_UNKNOWN));
    }
  }
}