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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.BaseCoreTest;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Tree;

/**
 * Unit tests for {@link InitialClientSetup}.
 */
@DisplayName("InitialClientSetup Tests")
public class InitialClientSetupTest extends BaseCoreTest {

  private static final String ERROR_TYPE = "Error";
  private static final String SUCCESS_TYPE = "Success";

  // -----------------------------------------------------------------------
  // Constructor & getLog
  // -----------------------------------------------------------------------
  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("New instance is created successfully")
    void newInstanceIsCreated() {
      InitialClientSetup setup = new InitialClientSetup();
      assertNotNull(setup);
    }

    @Test
    @DisplayName("getLog returns empty string initially")
    void getLogReturnsEmptyInitially() {
      InitialClientSetup setup = new InitialClientSetup();
      assertEquals("", setup.getLog());
    }
  }

  // -----------------------------------------------------------------------
  // getLog
  // -----------------------------------------------------------------------
  @Nested
  @DisplayName("getLog Tests")
  class GetLogTests {

    @Test
    @DisplayName("getLog returns accumulated header and log content")
    void getLogReturnsAccumulatedContent() throws Exception {
      InitialClientSetup setup = new InitialClientSetup();

      Field headerField = InitialClientSetup.class.getDeclaredField("strHeaderLog");
      headerField.setAccessible(true);
      StringBuffer header = (StringBuffer) headerField.get(setup);
      header.append("HEADER");

      Field logField = InitialClientSetup.class.getDeclaredField("strLog");
      logField.setAccessible(true);
      StringBuffer log = (StringBuffer) logField.get(setup);
      log.append("LOG");

      String result = setup.getLog();
      assertTrue(result.contains("HEADER"));
      assertTrue(result.contains("LOG"));
    }
  }

  // -----------------------------------------------------------------------
  // saveTree
  // -----------------------------------------------------------------------
  @Nested
  @DisplayName("saveTree Tests")
  class SaveTreeTests {

    private void verifySaveTree(String treeType, String expectedFieldName) throws Exception {
      InitialClientSetup setup = new InitialClientSetup();
      Tree mockTree = mock(Tree.class);

      // Ensure the field is null before calling saveTree
      Field field = InitialClientSetup.class.getDeclaredField(expectedFieldName);
      field.setAccessible(true);
      assertNull(field.get(setup));

      Method saveTreeMethod = InitialClientSetup.class.getDeclaredMethod("saveTree", Tree.class,
          String.class);
      saveTreeMethod.setAccessible(true);
      saveTreeMethod.invoke(setup, mockTree, treeType);

      assertEquals(mockTree, field.get(setup));
    }

    @Test
    @DisplayName("saveTree sets treeOrg for Organization type")
    void saveTreeOrganization() throws Exception {
      verifySaveTree("Organization", "treeOrg");
    }

    @Test
    @DisplayName("saveTree sets treeBPartner for Business Partner type")
    void saveTreeBusinessPartner() throws Exception {
      verifySaveTree("Business Partner", "treeBPartner");
    }

    @Test
    @DisplayName("saveTree sets treeProject for Project type")
    void saveTreeProject() throws Exception {
      verifySaveTree("Project", "treeProject");
    }

    @Test
    @DisplayName("saveTree sets treeSalesRegion for Sales Region type")
    void saveTreeSalesRegion() throws Exception {
      verifySaveTree("Sales Region", "treeSalesRegion");
    }

    @Test
    @DisplayName("saveTree sets treeProduct for Product type")
    void saveTreeProduct() throws Exception {
      verifySaveTree("Product", "treeProduct");
    }

    @Test
    @DisplayName("saveTree sets treeAccount for Element Value type")
    void saveTreeElementValue() throws Exception {
      verifySaveTree("Element Value", "treeAccount");
    }

    @Test
    @DisplayName("saveTree sets treeMenu for Menu type")
    void saveTreeMenu() throws Exception {
      verifySaveTree("Menu", "treeMenu");
    }

    @Test
    @DisplayName("saveTree sets treeCampaign for Campaign type")
    void saveTreeCampaign() throws Exception {
      verifySaveTree("Campaign", "treeCampaign");
    }

    @Test
    @DisplayName("saveTree sets treeAsset for Asset type")
    void saveTreeAsset() throws Exception {
      verifySaveTree("Asset", "treeAsset");
    }

    @Test
    @DisplayName("saveTree sets treeProductCategory for Product Category type")
    void saveTreeProductCategory() throws Exception {
      verifySaveTree("Product Category", "treeProductCategory");
    }

    @Test
    @DisplayName("saveTree sets treeCostcenter for Cost Center type")
    void saveTreeCostCenter() throws Exception {
      verifySaveTree("Cost Center", "treeCostcenter");
    }

    @Test
    @DisplayName("saveTree sets treeUserDimension1 for User Dimension 1 type")
    void saveTreeUserDimension1() throws Exception {
      verifySaveTree("User Dimension 1", "treeUserDimension1");
    }

    @Test
    @DisplayName("saveTree sets treeUserDimension2 for User Dimension 2 type")
    void saveTreeUserDimension2() throws Exception {
      verifySaveTree("User Dimension 2", "treeUserDimension2");
    }

    @Test
    @DisplayName("saveTree sets treeOBRE_ResourceCategory for OBRE_RC type")
    void saveTreeObreRc() throws Exception {
      verifySaveTree("OBRE_RC", "treeOBRE_ResourceCategory");
    }

    @Test
    @DisplayName("saveTree sets treeProductCharacteristic for Product Characteristic type")
    void saveTreeProductCharacteristic() throws Exception {
      verifySaveTree("Product Characteristic", "treeProductCharacteristic");
    }
  }

  // -----------------------------------------------------------------------
  // cleanUpStrModules
  // -----------------------------------------------------------------------
  @Nested
  @DisplayName("cleanUpStrModules Tests")
  class CleanUpStrModulesTests {

    private String invokeCleanUp(String input) throws Exception {
      InitialClientSetup setup = new InitialClientSetup();
      Method method = InitialClientSetup.class.getDeclaredMethod("cleanUpStrModules", String.class);
      method.setAccessible(true);
      return (String) method.invoke(setup, input);
    }

    @Test
    @DisplayName("null input returns empty string")
    void nullReturnsEmpty() throws Exception {
      assertEquals("", invokeCleanUp(null));
    }

    @Test
    @DisplayName("empty string returns empty string")
    void emptyReturnsEmpty() throws Exception {
      assertEquals("", invokeCleanUp(""));
    }

    @Test
    @DisplayName("parentheses are stripped from both ends")
    void parenthesesStripped() throws Exception {
      assertEquals("abc", invokeCleanUp("(abc)"));
    }

    @Test
    @DisplayName("string without parentheses is returned unchanged")
    void noParenthesesUnchanged() throws Exception {
      assertEquals("abc", invokeCleanUp("abc"));
    }

    @Test
    @DisplayName("leading parenthesis only is stripped")
    void leadingParenthesisStripped() throws Exception {
      assertEquals("abc", invokeCleanUp("(abc"));
    }

    @Test
    @DisplayName("trailing parenthesis only is stripped")
    void trailingParenthesisStripped() throws Exception {
      assertEquals("abc", invokeCleanUp("abc)"));
    }
  }

  // -----------------------------------------------------------------------
  // insertClient
  // -----------------------------------------------------------------------
  @Nested
  @DisplayName("insertClient Tests")
  class InsertClientTests {

    @Test
    @DisplayName("returns error when client name already exists")
    void errorWhenClientNameExists() throws Exception {
      try (MockedStatic<InitialSetupUtility> isuMock = mockStatic(InitialSetupUtility.class)) {
        isuMock.when(() -> InitialSetupUtility.existsClientName(anyString())).thenReturn(true);

        InitialClientSetup setup = new InitialClientSetup();
        OBError result = setup.insertClient(null, "TestClient", "TestUser", "102");

        assertEquals(ERROR_TYPE, result.getType());
      }
    }

    @Test
    @DisplayName("returns error when user name already exists")
    void errorWhenUserNameExists() throws Exception {
      try (MockedStatic<InitialSetupUtility> isuMock = mockStatic(InitialSetupUtility.class)) {
        isuMock.when(() -> InitialSetupUtility.existsClientName(anyString())).thenReturn(false);
        isuMock.when(() -> InitialSetupUtility.existsUserName(anyString())).thenReturn(true);

        InitialClientSetup setup = new InitialClientSetup();
        OBError result = setup.insertClient(null, "TestClient", "TestUser", "102");

        assertEquals(ERROR_TYPE, result.getType());
      }
    }

    @Test
    @DisplayName("returns error when insertClient utility returns null")
    void errorWhenInsertClientReturnsNull() throws Exception {
      try (MockedStatic<InitialSetupUtility> isuMock = mockStatic(InitialSetupUtility.class)) {
        isuMock.when(() -> InitialSetupUtility.existsClientName(anyString())).thenReturn(false);
        isuMock.when(() -> InitialSetupUtility.existsUserName(anyString())).thenReturn(false);
        isuMock.when(() -> InitialSetupUtility.insertClient(anyString(), anyString()))
            .thenReturn(null);

        InitialClientSetup setup = new InitialClientSetup();
        OBError result = setup.insertClient(null, "TestClient", "TestUser", "102");

        assertEquals(ERROR_TYPE, result.getType());
      }
    }
  }

  // -----------------------------------------------------------------------
  // insertTrees
  // -----------------------------------------------------------------------
  @Nested
  @DisplayName("insertTrees Tests")
  class InsertTreesTests {

    @Test
    @DisplayName("returns error when client is null")
    void errorWhenClientIsNull() {
      InitialClientSetup setup = new InitialClientSetup();
      OBError result = setup.insertTrees(null);
      assertEquals(ERROR_TYPE, result.getType());
    }
  }

  // -----------------------------------------------------------------------
  // insertClientInfo
  // -----------------------------------------------------------------------
  @Nested
  @DisplayName("insertClientInfo Tests")
  class InsertClientInfoTests {

    @Test
    @DisplayName("returns error when client is null")
    void errorWhenClientIsNull() {
      InitialClientSetup setup = new InitialClientSetup();
      OBError result = setup.insertClientInfo();
      assertEquals(ERROR_TYPE, result.getType());
    }

    @Test
    @DisplayName("returns error when trees are null even if client is set")
    void errorWhenTreesAreNull() throws Exception {
      InitialClientSetup setup = new InitialClientSetup();

      Field clientField = InitialClientSetup.class.getDeclaredField("client");
      clientField.setAccessible(true);
      clientField.set(setup, mock(Client.class));

      OBError result = setup.insertClientInfo();
      assertEquals(ERROR_TYPE, result.getType());
    }
  }

  // -----------------------------------------------------------------------
  // insertRoles
  // -----------------------------------------------------------------------
  @Nested
  @DisplayName("insertRoles Tests")
  class InsertRolesTests {

    @Test
    @DisplayName("returns error when client is null")
    void errorWhenClientIsNull() {
      InitialClientSetup setup = new InitialClientSetup();
      OBError result = setup.insertRoles();
      assertEquals(ERROR_TYPE, result.getType());
    }
  }

  // -----------------------------------------------------------------------
  // insertUser
  // -----------------------------------------------------------------------
  @Nested
  @DisplayName("insertUser Tests")
  class InsertUserTests {

    @Test
    @DisplayName("returns error when client is null")
    void errorWhenClientIsNull() {
      InitialClientSetup setup = new InitialClientSetup();
      OBError result = setup.insertUser("admin", "TestClient", "pass", "en_US");
      assertEquals(ERROR_TYPE, result.getType());
    }

    @Test
    @DisplayName("uses clientName + Client as default username when strUserName is null")
    void defaultUsernameWhenNull() throws Exception {
      InitialClientSetup setup = new InitialClientSetup();

      Client mockClient = mock(Client.class);
      org.mockito.Mockito.when(mockClient.getName()).thenReturn("MyCompany");

      Field clientField = InitialClientSetup.class.getDeclaredField("client");
      clientField.setAccessible(true);
      clientField.set(setup, mockClient);

      org.openbravo.model.ad.access.Role mockRole = mock(
          org.openbravo.model.ad.access.Role.class);
      Field roleField = InitialClientSetup.class.getDeclaredField("role");
      roleField.setAccessible(true);
      roleField.set(setup, mockRole);

      try (MockedStatic<InitialSetupUtility> isuMock = mockStatic(InitialSetupUtility.class)) {
        isuMock.when(() -> InitialSetupUtility.getLanguage(anyString())).thenReturn(null);
        isuMock.when(() -> InitialSetupUtility.insertUser(
            org.mockito.ArgumentMatchers.any(Client.class),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq("MyCompanyClient"),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(org.openbravo.model.ad.access.Role.class),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(mock(org.openbravo.model.ad.access.User.class));

        isuMock.when(() -> InitialSetupUtility.insertUserRoles(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(null);

        OBError result = setup.insertUser(null, "MyCompany", "pass", "en_US");
        assertEquals(SUCCESS_TYPE, result.getType());

        isuMock.verify(() -> InitialSetupUtility.insertUser(
            org.mockito.ArgumentMatchers.any(Client.class),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq("MyCompanyClient"),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(org.openbravo.model.ad.access.Role.class),
            org.mockito.ArgumentMatchers.any()
        ));
      }
    }

    @Test
    @DisplayName("uses clientName + Client as default username when strUserName is empty")
    void defaultUsernameWhenEmpty() throws Exception {
      InitialClientSetup setup = new InitialClientSetup();

      Client mockClient = mock(Client.class);
      org.mockito.Mockito.when(mockClient.getName()).thenReturn("Acme");

      Field clientField = InitialClientSetup.class.getDeclaredField("client");
      clientField.setAccessible(true);
      clientField.set(setup, mockClient);

      org.openbravo.model.ad.access.Role mockRole = mock(
          org.openbravo.model.ad.access.Role.class);
      Field roleField = InitialClientSetup.class.getDeclaredField("role");
      roleField.setAccessible(true);
      roleField.set(setup, mockRole);

      try (MockedStatic<InitialSetupUtility> isuMock = mockStatic(InitialSetupUtility.class)) {
        isuMock.when(() -> InitialSetupUtility.getLanguage(anyString())).thenReturn(null);
        isuMock.when(() -> InitialSetupUtility.insertUser(
            org.mockito.ArgumentMatchers.any(Client.class),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq("AcmeClient"),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(org.openbravo.model.ad.access.Role.class),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(mock(org.openbravo.model.ad.access.User.class));

        isuMock.when(() -> InitialSetupUtility.insertUserRoles(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(null);

        OBError result = setup.insertUser("", "Acme", "pass", "en_US");
        assertEquals(SUCCESS_TYPE, result.getType());

        isuMock.verify(() -> InitialSetupUtility.insertUser(
            org.mockito.ArgumentMatchers.any(Client.class),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq("AcmeClient"),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(org.openbravo.model.ad.access.Role.class),
            org.mockito.ArgumentMatchers.any()
        ));
      }
    }
  }
}
