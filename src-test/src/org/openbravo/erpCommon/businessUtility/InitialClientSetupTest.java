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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.BaseCoreTest;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.utility.Tree;

/**
 * Unit tests for {@link InitialClientSetup}.
 */
@DisplayName("InitialClientSetup")
public class InitialClientSetupTest extends BaseCoreTest {

  // ── Constructor and getLog ────────────────────────────────────────────

  @Nested
  @DisplayName("Constructor and getLog")
  class ConstructorAndGetLog {
    @Test
    @DisplayName("default constructor creates instance with empty log")
    void constructorCreatesInstance() {
      InitialClientSetup setup = new InitialClientSetup();
      assertNotNull(setup);
      assertNotNull(setup.getLog());
    }

    @Test
    @DisplayName("getLog initially returns empty or near-empty string")
    void getLogInitiallyEmpty() {
      InitialClientSetup setup = new InitialClientSetup();
      String log = setup.getLog();
      assertTrue(log.isEmpty() || log.trim().isEmpty());
    }
  }

  // ── saveTree (package-private) ────────────────────────────────────────

  @Nested
  @DisplayName("saveTree - all 15 tree types")
  class SaveTree {

    private static final String[] TREE_TYPES = {
        "Organization", "Business Partner", "Project", "Sales Region",
        "Product", "Element Value", "Menu", "Campaign", "Asset",
        "Product Category", "Cost Center", "User Dimension 1",
        "User Dimension 2", "OBRE_RC", "Product Characteristic"
    };

    private static final String[] FIELD_NAMES = {
        "treeOrg", "treeBPartner", "treeProject", "treeSalesRegion",
        "treeProduct", "treeAccount", "treeMenu", "treeCampaign", "treeAsset",
        "treeProductCategory", "treeCostcenter", "treeUserDimension1",
        "treeUserDimension2", "treeOBRE_ResourceCategory", "treeProductCharacteristic"
    };

    @Test
    @DisplayName("saveTree sets the correct field for each tree type")
    void saveTreeSetsCorrectField() throws Exception {
      InitialClientSetup setup = new InitialClientSetup();
      Method saveTreeMethod = InitialClientSetup.class.getDeclaredMethod(
          "saveTree", Tree.class, String.class);
      saveTreeMethod.setAccessible(true);

      for (int i = 0; i < TREE_TYPES.length; i++) {
        Tree mockTree = mock(Tree.class);
        when(mockTree.getName()).thenReturn("TestTree_" + TREE_TYPES[i]);

        saveTreeMethod.invoke(setup, mockTree, TREE_TYPES[i]);

        Field treeField = InitialClientSetup.class.getDeclaredField(FIELD_NAMES[i]);
        treeField.setAccessible(true);
        assertEquals(mockTree, treeField.get(setup),
            "saveTree should set field " + FIELD_NAMES[i] + " for type " + TREE_TYPES[i]);
      }
    }
  }

  // ── cleanUpStrModules (private) ───────────────────────────────────────

  @Nested
  @DisplayName("cleanUpStrModules (private)")
  class CleanUpStrModules {

    private String invoke(InitialClientSetup setup, String input) throws Exception {
      Method method = InitialClientSetup.class.getDeclaredMethod(
          "cleanUpStrModules", String.class);
      method.setAccessible(true);
      return (String) method.invoke(setup, input);
    }

    @Test void nullReturnsEmpty() throws Exception {
      assertEquals("", invoke(new InitialClientSetup(), null));
    }

    @Test void emptyReturnsEmpty() throws Exception {
      assertEquals("", invoke(new InitialClientSetup(), ""));
    }

    @Test void wrappedParensStripped() throws Exception {
      assertEquals("abc", invoke(new InitialClientSetup(), "(abc)"));
    }

    @Test void noParensUnchanged() throws Exception {
      assertEquals("abc", invoke(new InitialClientSetup(), "abc"));
    }

    @Test void leadingParenOnly() throws Exception {
      assertEquals("abc", invoke(new InitialClientSetup(), "(abc"));
    }

    @Test void trailingParenOnly() throws Exception {
      assertEquals("abc", invoke(new InitialClientSetup(), "abc)"));
    }
  }

  // ── insertClient error path ───────────────────────────────────────────

  @Nested
  @DisplayName("insertClient error when client name exists")
  class InsertClientErrors {

    @Test
    @DisplayName("insertClient returns error when client name already exists")
    void insertClientDuplicateName() {
      try (MockedStatic<InitialSetupUtility> utilMock = mockStatic(InitialSetupUtility.class)) {
        utilMock.when(() -> InitialSetupUtility.existsClientName(anyString())).thenReturn(true);

        InitialClientSetup setup = new InitialClientSetup();
        VariablesSecureApp vars = mock(VariablesSecureApp.class);

        OBError result = setup.insertClient(vars, "TestClient", "TestUser", "102");
        assertEquals("Error", result.getType());
      }
    }
  }

  // ── insertTrees error when client is null ─────────────────────────────

  @Nested
  @DisplayName("insertTrees error when client is null")
  class InsertTreesErrors {
    @Test
    @DisplayName("insertTrees returns error when client is null")
    void insertTreesNullClient() {
      InitialClientSetup setup = new InitialClientSetup();
      VariablesSecureApp vars = mock(VariablesSecureApp.class);

      OBError result = setup.insertTrees(vars);
      assertEquals("Error", result.getType());
    }
  }

  // ── insertClientInfo error when client is null ────────────────────────

  @Nested
  @DisplayName("insertClientInfo error when client is null")
  class InsertClientInfoErrors {
    @Test
    @DisplayName("insertClientInfo returns error when client is null")
    void insertClientInfoNullClient() {
      InitialClientSetup setup = new InitialClientSetup();

      OBError result = setup.insertClientInfo();
      assertEquals("Error", result.getType());
    }
  }

  // ── insertRoles error when client is null ─────────────────────────────

  @Nested
  @DisplayName("insertRoles error when client is null")
  class InsertRolesErrors {
    @Test
    @DisplayName("insertRoles returns error when client is null")
    void insertRolesNullClient() {
      InitialClientSetup setup = new InitialClientSetup();

      OBError result = setup.insertRoles();
      assertEquals("Error", result.getType());
    }
  }

  // ── insertUser error when client is null ──────────────────────────────

  @Nested
  @DisplayName("insertUser error when client is null")
  class InsertUserErrors {
    @Test
    @DisplayName("insertUser returns error when client is null")
    void insertUserNullClient() {
      InitialClientSetup setup = new InitialClientSetup();

      OBError result = setup.insertUser("user", "client", "pass", "en_US");
      assertEquals("Error", result.getType());
    }
  }
}
