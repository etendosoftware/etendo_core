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
package com.etendoerp.common.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.dal.security.OrganizationStructureProvider;

/**
 * Checks preference resolution in both query orders, independently of generated preference IDs.
 * Integration tests order records by ID, which can leave either comparison direction uncovered.
 */
class PreferencesPriorityTest {
  private static final String SYSTEM_ID = "0";
  private static final String CLIENT_ID = "test-client";
  private static final String PROPERTY = "priority-property";
  private static final String SELECTED_VALUE = "selected-value";
  private static final String OTHER_VALUE = "other-value";

  /**
   * A client-specific preference wins over an explicit system preference in either order.
   *
   * @param higherFirst whether the higher priority preference is returned first
   * @throws PropertyException if preference resolution fails
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void clientPreferenceOverridesSystem(boolean higherFirst) throws PropertyException {
    assertPriority(preference(CLIENT_ID, false), preference(SYSTEM_ID, false), higherFirst);
  }

  /**
   * An undefined client visibility is treated as system visibility in either order.
   *
   * @param higherFirst whether the higher priority preference is returned first
   * @throws PropertyException if preference resolution fails
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void clientPreferenceOverridesUndefinedClient(boolean higherFirst) throws PropertyException {
    assertPriority(preference(CLIENT_ID, false), preference(null, false), higherFirst);
  }

  /**
   * The selected preference resolves a conflict at the same visibility in either order.
   *
   * @param higherFirst whether the higher priority preference is returned first
   * @throws PropertyException if preference resolution fails
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void selectedPreferenceOverridesUnselected(boolean higherFirst) throws PropertyException {
    assertPriority(preference(CLIENT_ID, true), preference(CLIENT_ID, false), higherFirst);
  }

  /**
   * Creates a preference with the visibility and selection relevant to priority resolution.
   *
   * @param clientId visible client, or null for implicit system visibility
   * @param selected whether this preference is selected to resolve conflicts
   * @return the preference fixture
   */
  private static Preference preference(String clientId, boolean selected) {
    Preference preference = mock(Preference.class);
    when(preference.getAttribute()).thenReturn(PROPERTY);
    when(preference.isSelected()).thenReturn(selected);
    if (clientId != null) {
      Client client = mock(Client.class);
      when(client.getId()).thenReturn(clientId);
      when(preference.getVisibleAtClient()).thenReturn(client);
    }
    return preference;
  }

  /**
   * Checks both public resolution paths with a controlled query result order.
   *
   * @param higher preference expected to win
   * @param lower preference expected to lose
   * @param higherFirst whether the query returns the higher priority preference first
   * @throws PropertyException if preference resolution fails
   */
  private static void assertPriority(Preference higher, Preference lower, boolean higherFirst)
      throws PropertyException {
    when(higher.getSearchKey()).thenReturn(SELECTED_VALUE);
    when(lower.getSearchKey()).thenReturn(OTHER_VALUE);
    OBContext context = mock(OBContext.class);
    OBDal dal = mock(OBDal.class);
    OrganizationStructureProvider organizations = mock(OrganizationStructureProvider.class);
    @SuppressWarnings("unchecked")
    OBQuery<Preference> query = mock(OBQuery.class);
    when(context.getOrganizationStructureProvider()).thenReturn(organizations);
    when(context.getOrganizationStructureProvider(CLIENT_ID)).thenReturn(organizations);
    when(organizations.getParentList(null, true)).thenReturn(List.of(SYSTEM_ID));
    when(dal.createQuery(eq(Preference.class), anyString())).thenReturn(query);
    when(query.list()).thenReturn(higherFirst ? List.of(higher, lower) : List.of(lower, higher));

    try (MockedStatic<OBContext> contexts = mockStatic(OBContext.class);
        MockedStatic<OBDal> dals = mockStatic(OBDal.class)) {
      contexts.when(OBContext::getOBContext).thenReturn(context);
      dals.when(OBDal::getInstance).thenReturn(dal);

      List<Preference> preferences = Preferences.getAllPreferences(CLIENT_ID, null, null, null);
      assertEquals(1, preferences.size());
      assertSame(higher, preferences.get(0));
      assertEquals(SELECTED_VALUE, Preferences.getPreferenceValue(PROPERTY, false, CLIENT_ID,
          null, null, null, null));
    }
  }
}
