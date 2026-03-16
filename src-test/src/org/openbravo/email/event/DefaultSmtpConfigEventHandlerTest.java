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
package org.openbravo.email.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for {@link DefaultSmtpConfigEventHandler}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultSmtpConfigEventHandlerTest {

  private static final String SAVED_ID = "SAVED-CONFIG-ID";
  private static final String OTHER_ID = "OTHER-CONFIG-ID";

  @Mock private OBDal obDal;
  @Mock private OBCriteria<EmailServerConfiguration> criteria;
  @Mock private User user;
  @Mock private Organization org;
  @Mock private Client client;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;

  /**
   * Initializes static mocks for {@link OBDal} and {@link OBMessageUtils} before each test.
   */
  @BeforeEach
  void setUp() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    when(obDal.createCriteria(EmailServerConfiguration.class)).thenReturn(criteria);
    when(criteria.add(any())).thenReturn(criteria);
    when(criteria.list()).thenReturn(Collections.emptyList());
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(any(String.class)))
        .thenReturn("conflict error message");
  }

  /**
   * Closes all static mocks after each test to prevent leakage between tests.
   */
  @AfterEach
  void tearDown() {
    mockedOBDal.close();
    mockedOBMessageUtils.close();
  }

  /**
   * A config with {@code defaultConfiguration = true} must be recognized as marked as default.
   */
  @Test
  void testIsMarkedAsDefaultReturnsTrueWhenFlagIsTrue() {
    EmailServerConfiguration config = mockConfig(SAVED_ID, true, null, null, null);
    assertTrue(DefaultSmtpConfigEventHandler.isMarkedAsDefault(config));
  }

  /**
   * A config with {@code defaultConfiguration = false} must not be recognized as default.
   */
  @Test
  void testIsMarkedAsDefaultReturnsFalseWhenFlagIsFalse() {
    EmailServerConfiguration config = mockConfig(SAVED_ID, false, null, null, null);
    assertFalse(DefaultSmtpConfigEventHandler.isMarkedAsDefault(config));
  }

  /**
   * A config with a non-null {@code userContact} must be identified as user-level.
   */
  @Test
  void testIsUserLevelConfigReturnsTrueWhenUserContactPresent() {
    EmailServerConfiguration config = mockConfig(SAVED_ID, true, user, null, null);
    assertTrue(DefaultSmtpConfigEventHandler.isUserLevelConfig(config));
  }

  /**
   * A config with a null {@code userContact} must not be identified as user-level.
   */
  @Test
  void testIsUserLevelConfigReturnsFalseWhenUserContactAbsent() {
    EmailServerConfiguration config = mockConfig(SAVED_ID, true, null, null, null);
    assertFalse(DefaultSmtpConfigEventHandler.isUserLevelConfig(config));
  }

  /**
   * A config with a non-null {@code emailConfigOrganization} must be identified as org-level.
   */
  @Test
  void testIsOrgLevelConfigReturnsTrueWhenEmailConfigOrgPresent() {
    EmailServerConfiguration config = mockConfig(SAVED_ID, true, null, org, null);
    assertTrue(DefaultSmtpConfigEventHandler.isOrgLevelConfig(config));
  }

  /**
   * A config with a null {@code emailConfigOrganization} must not be identified as org-level.
   */
  @Test
  void testIsOrgLevelConfigReturnsFalseWhenEmailConfigOrgAbsent() {
    EmailServerConfiguration config = mockConfig(SAVED_ID, true, null, null, null);
    assertFalse(DefaultSmtpConfigEventHandler.isOrgLevelConfig(config));
  }

  /**
   * When the saved config is marked as default and no conflict exists, no exception is thrown.
   */
  @Test
  void testHandleDefaultConfigChangeDoesNotThrowWhenNoConflict() {
    EmailServerConfiguration saved = mockConfig(SAVED_ID, true, null, null, client);
    assertDoesNotThrow(() -> DefaultSmtpConfigEventHandler.handleDefaultConfigChange(saved));
  }

  /**
   * When the saved config is marked as default and a conflict exists, an {@link OBException} must
   * be thrown.
   */
  @Test
  void testHandleDefaultConfigChangeThrowsWhenConflictExists() {
    EmailServerConfiguration saved = mockConfig(SAVED_ID, true, null, null, client);
    EmailServerConfiguration conflict = mockConfig(OTHER_ID, true, null, null, client);
    when(criteria.list()).thenReturn(Collections.singletonList(conflict));

    assertThrows(OBException.class,
        () -> DefaultSmtpConfigEventHandler.handleDefaultConfigChange(saved));
  }

  /**
   * When the saved config is NOT marked as default, the handler must exit early without querying.
   */
  @Test
  void testHandleDefaultConfigChangeDoesNothingWhenNotMarkedAsDefault() {
    EmailServerConfiguration saved = mockConfig(SAVED_ID, false, null, null, client);
    DefaultSmtpConfigEventHandler.handleDefaultConfigChange(saved);
    verify(obDal, never()).createCriteria(EmailServerConfiguration.class);
  }

  /**
   * For a user-level config, {@code findOtherDefaultsInScope} must delegate to user scope.
   */
  @Test
  void testFindOtherDefaultsInScopeUsesUserLevelWhenUserPresent() {
    EmailServerConfiguration saved = mockConfig(SAVED_ID, true, user, null, null);
    List<EmailServerConfiguration> result =
        DefaultSmtpConfigEventHandler.findOtherDefaultsInScope(saved);
    assertTrue(result.isEmpty());
  }

  /**
   * For an org-level config (no user, emailConfigOrganization set), {@code findOtherDefaultsInScope}
   * must delegate to org scope.
   */
  @Test
  void testFindOtherDefaultsInScopeUsesOrgLevelWhenEmailConfigOrgPresent() {
    EmailServerConfiguration saved = mockConfig(SAVED_ID, true, null, org, null);
    List<EmailServerConfiguration> result =
        DefaultSmtpConfigEventHandler.findOtherDefaultsInScope(saved);
    assertTrue(result.isEmpty());
  }

  /**
   * For a client-level config (no user, no emailConfigOrganization), {@code findOtherDefaultsInScope}
   * must delegate to client scope.
   */
  @Test
  void testFindOtherDefaultsInScopeUsesClientLevelWhenUserAndOrgAbsent() {
    EmailServerConfiguration saved = mockConfig(SAVED_ID, true, null, null, client);
    List<EmailServerConfiguration> result =
        DefaultSmtpConfigEventHandler.findOtherDefaultsInScope(saved);
    assertTrue(result.isEmpty());
  }

  /**
   * {@code findOtherUserDefaults} must return results from the criteria query.
   */
  @Test
  void testFindOtherUserDefaultsReturnsQueryResults() {
    EmailServerConfiguration conflict = mockConfig(OTHER_ID, true, user, null, null);
    when(criteria.list()).thenReturn(Collections.singletonList(conflict));
    List<EmailServerConfiguration> result =
        DefaultSmtpConfigEventHandler.findOtherUserDefaults(user, SAVED_ID);
    assertEquals(1, result.size());
    assertEquals(conflict, result.get(0));
  }

  /**
   * {@code findOtherUserDefaults} must return an empty list when no conflicts exist.
   */
  @Test
  void testFindOtherUserDefaultsReturnsEmptyListWhenNoConflicts() {
    List<EmailServerConfiguration> result =
        DefaultSmtpConfigEventHandler.findOtherUserDefaults(user, SAVED_ID);
    assertTrue(result.isEmpty());
  }

  /**
   * {@code findOtherOrgDefaults} must return results from the criteria query.
   */
  @Test
  void testFindOtherOrgDefaultsReturnsQueryResults() {
    EmailServerConfiguration conflict = mockConfig(OTHER_ID, true, null, org, null);
    when(criteria.list()).thenReturn(Collections.singletonList(conflict));
    List<EmailServerConfiguration> result =
        DefaultSmtpConfigEventHandler.findOtherOrgDefaults(org, SAVED_ID);
    assertEquals(1, result.size());
    assertEquals(conflict, result.get(0));
  }

  /**
   * {@code findOtherOrgDefaults} must return an empty list when no conflicts exist.
   */
  @Test
  void testFindOtherOrgDefaultsReturnsEmptyListWhenNoConflicts() {
    List<EmailServerConfiguration> result =
        DefaultSmtpConfigEventHandler.findOtherOrgDefaults(org, SAVED_ID);
    assertTrue(result.isEmpty());
  }

  /**
   * {@code findOtherClientDefaults} must return results from the criteria query.
   */
  @Test
  void testFindOtherClientDefaultsReturnsQueryResults() {
    EmailServerConfiguration conflict = mockConfig(OTHER_ID, true, null, null, client);
    when(criteria.list()).thenReturn(Collections.singletonList(conflict));
    List<EmailServerConfiguration> result =
        DefaultSmtpConfigEventHandler.findOtherClientDefaults(client, SAVED_ID);
    assertEquals(1, result.size());
    assertEquals(conflict, result.get(0));
  }

  /**
   * {@code findOtherClientDefaults} must return an empty list when no conflicts exist.
   */
  @Test
  void testFindOtherClientDefaultsReturnsEmptyListWhenNoConflicts() {
    List<EmailServerConfiguration> result =
        DefaultSmtpConfigEventHandler.findOtherClientDefaults(client, SAVED_ID);
    assertTrue(result.isEmpty());
  }

  /**
   * {@code buildDefaultConfigCriteria} must return a non-null criteria instance.
   */
  @Test
  void testBuildDefaultConfigCriteriaReturnsNonNullCriteria() {
    OBCriteria<EmailServerConfiguration> result =
        DefaultSmtpConfigEventHandler.buildDefaultConfigCriteria(SAVED_ID);
    assertNotNull(result);
  }

  /**
   * Creates a mocked {@link EmailServerConfiguration} with the given field values.
   * @param id the record ID
   * @param isDefault the value to return for {@code isDefaultConfiguration()}
   * @param user the user contact ({@code null} for org/client-level configs)
   * @param emailConfigOrg the email config organization ({@code null} for user/client-level configs)
   * @param emailConfigClient the email config client ({@code null} for user/org-level configs)
   * @return a configured mock
   */
  private EmailServerConfiguration mockConfig(String id, boolean isDefault, User user,
      Organization emailConfigOrg, Client emailConfigClient) {
    EmailServerConfiguration config = mock(EmailServerConfiguration.class);
    when(config.getId()).thenReturn(id);
    when(config.isDefaultConfiguration()).thenReturn(isDefault);
    when(config.getUserContact()).thenReturn(user);
    when(config.getEmailConfigOrganization()).thenReturn(emailConfigOrg);
    when(config.getEmailConfigClient()).thenReturn(emailConfigClient);
    return config;
  }
}
