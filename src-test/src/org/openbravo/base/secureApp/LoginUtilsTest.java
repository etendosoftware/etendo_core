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
package org.openbravo.base.secureApp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.authentication.hashing.PasswordHash;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Tests for {@link LoginUtils} covering client/org list building and password checking.
 */
@DisplayName("LoginUtils")
public class LoginUtilsTest {

  private static final String ADMIN_USERNAME = "admin";

  private RoleOrganization mockRoleOrg(String clientId, String orgId) {
    RoleOrganization ro = mock(RoleOrganization.class);
    Client mockClient = mock(Client.class);
    when(mockClient.getId()).thenReturn(clientId);
    Organization mockOrg = mock(Organization.class);
    when(mockOrg.getId()).thenReturn(orgId);
    when(ro.getClient()).thenReturn(mockClient);
    when(ro.getOrganization()).thenReturn(mockOrg);
    return ro;
  }

  @Nested
  @DisplayName("buildClientList")
  class BuildClientList {
    @Test
    void testEmptyList() {
      assertEquals("", LoginUtils.buildClientList(Collections.emptyList()));
    }

    @Test
    void testSingleItem() {
      List<RoleOrganization> list = new ArrayList<>();
      list.add(mockRoleOrg("C1", "O1"));
      assertEquals("'C1'", LoginUtils.buildClientList(list));
    }

    @Test
    void testDeduplicateSameClient() {
      List<RoleOrganization> list = new ArrayList<>();
      list.add(mockRoleOrg("C1", "O1"));
      list.add(mockRoleOrg("C1", "O2"));
      assertEquals("'C1'", LoginUtils.buildClientList(list));
    }

    @Test
    void testMultipleClients() {
      List<RoleOrganization> list = new ArrayList<>();
      list.add(mockRoleOrg("C1", "O1"));
      list.add(mockRoleOrg("C2", "O2"));
      assertEquals("'C1','C2'", LoginUtils.buildClientList(list));
    }
  }

  @Nested
  @DisplayName("buildOrgList")
  class BuildOrgList {
    @Test
    void testEmptyList() {
      assertEquals("", LoginUtils.buildOrgList(Collections.emptyList()));
    }

    @Test
    void testSingleItem() {
      List<RoleOrganization> list = new ArrayList<>();
      list.add(mockRoleOrg("C1", "O1"));
      assertEquals("'O1'", LoginUtils.buildOrgList(list));
    }

    @Test
    void testMultipleOrgs() {
      List<RoleOrganization> list = new ArrayList<>();
      list.add(mockRoleOrg("C1", "O1"));
      list.add(mockRoleOrg("C1", "O2"));
      list.add(mockRoleOrg("C2", "O3"));
      assertEquals("'O1','O2','O3'", LoginUtils.buildOrgList(list));
    }
  }

  @Nested
  @DisplayName("checkUserPassword")
  class CheckUserPassword {
    @Test
    void testValidCredentials() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn("USER123");
      try (MockedStatic<PasswordHash> phMock = mockStatic(PasswordHash.class)) {
        phMock.when(() -> PasswordHash.getUserWithPassword(ADMIN_USERNAME, "pass123"))
            .thenReturn(Optional.of(mockUser));
        assertEquals("USER123", LoginUtils.checkUserPassword(null, ADMIN_USERNAME, "pass123"));
      }
    }

    @Test
    void testInvalidCredentials() {
      try (MockedStatic<PasswordHash> phMock = mockStatic(PasswordHash.class)) {
        phMock.when(() -> PasswordHash.getUserWithPassword(ADMIN_USERNAME, "wrong"))
            .thenReturn(Optional.empty());
        assertNull(LoginUtils.checkUserPassword(null, ADMIN_USERNAME, "wrong"));
      }
    }
  }
}
