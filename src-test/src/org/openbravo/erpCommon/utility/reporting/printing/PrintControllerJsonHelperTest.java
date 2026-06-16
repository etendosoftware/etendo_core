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
package org.openbravo.erpCommon.utility.reporting.printing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.model.ad.access.User;

/**
 * Tests for {@link PrintControllerJsonHelper}.
 */
@SuppressWarnings({"java:S100", "java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class PrintControllerJsonHelperTest {

  private static final String ALICE_NAME = "Alice";
  private static final String ALICE_EMAIL = "a@test.com";
  private static final String BOB_EMAIL = "b@test.com";
  private static final String KEY_CONTACTS = "contacts";
  private static final String KEY_EMAIL = "email";

  private MockedStatic<BPContactEmailSelector> bpContactSelectorStatic;
  /** Sets up static mock for BPContactEmailSelector. */
  @Before
  public void setUp() {
    bpContactSelectorStatic = mockStatic(BPContactEmailSelector.class);
  }
  /** Closes static mock after each test. */
  @After
  public void tearDown() {
    if (bpContactSelectorStatic != null) {
      bpContactSelectorStatic.close();
    }
  }

  // -------------------------------------------------------------------------
  // buildBPContactsJson — multi-customer path (2+ distinct bpartnerIds)
  // -------------------------------------------------------------------------
  /** Multi-customer mode returns one contact entry per unique email. */
  @Test
  public void testBuildBPContactsJson_multiCustomer_returnsOneContactPerUniqueEmail() throws JSONException {
    PocData doc1 = pocData("bp1", "u1", ALICE_NAME, ALICE_EMAIL);
    PocData doc2 = pocData("bp2", "u2", "Bob", BOB_EMAIL);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, new PocData[]{ doc1, doc2 });

    JSONArray contacts = result.getJSONArray(KEY_CONTACTS);
    assertEquals(2, contacts.length());
    assertEquals(ALICE_EMAIL, contacts.getJSONObject(0).getString(KEY_EMAIL));
    assertEquals(BOB_EMAIL, contacts.getJSONObject(1).getString(KEY_EMAIL));
  }
  /** Duplicate email addresses across customers are deduplicated. */
  @Test
  public void testBuildBPContactsJson_multiCustomer_duplicateEmailDeduped() throws JSONException {
    PocData doc1 = pocData("bp1", "u1", ALICE_NAME, "shared@test.com");
    PocData doc2 = pocData("bp2", "u2", "Alice2", "shared@test.com");
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, new PocData[]{ doc1, doc2 });

    assertEquals(1, result.getJSONArray(KEY_CONTACTS).length());
  }
  /** Contacts with blank email are skipped in multi-customer mode. */
  @Test
  public void testBuildBPContactsJson_multiCustomer_blankEmailSkipped() throws JSONException {
    PocData doc1 = pocData("bp1", "u1", ALICE_NAME, ALICE_EMAIL);
    PocData doc2 = pocData("bp2", "u2", "NoEmail", "");
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, new PocData[]{ doc1, doc2 });

    assertEquals(1, result.getJSONArray(KEY_CONTACTS).length());
  }
  /** Contact JSON object contains contactId, name, email, isDefault, and isActive fields. */
  @Test
  public void testBuildBPContactsJson_multiCustomer_contactJsonFields() throws JSONException {
    PocData doc1 = pocData("bp1", "uid1", ALICE_NAME, ALICE_EMAIL);
    PocData doc2 = pocData("bp2", "uid2", "Bob", BOB_EMAIL);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, new PocData[]{ doc1, doc2 });

    JSONObject first = result.getJSONArray(KEY_CONTACTS).getJSONObject(0);
    assertEquals("uid1", first.getString("contactId"));
    assertEquals(ALICE_NAME, first.getString("name"));
    assertEquals(ALICE_EMAIL, first.getString(KEY_EMAIL));
    assertEquals(false, first.getBoolean("isDefault"));
    assertEquals(true, first.getBoolean("isActive"));
  }

  // -------------------------------------------------------------------------
  // buildBPContactsJson — single-customer path (all same bpartnerId)
  // -------------------------------------------------------------------------
  /** Single customer delegates contact loading to BPContactEmailSelector. */
  @Test
  public void testBuildBPContactsJson_singleCustomer_returnsBPContactsFromSelector() throws JSONException {
    PocData doc = pocData("bp1", "u1", ALICE_NAME, ALICE_EMAIL);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    User contact = mockUser("u1", ALICE_NAME, ALICE_EMAIL);
    bpContactSelectorStatic
        .when(() -> BPContactEmailSelector.getBPContactsWithEmail("bp1"))
        .thenReturn(Collections.singletonList(contact));

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, new PocData[]{ doc });

    JSONArray contacts = result.getJSONArray(KEY_CONTACTS);
    assertEquals(1, contacts.length());
    assertEquals("u1", contacts.getJSONObject(0).getString("contactId"));
    assertEquals(ALICE_EMAIL, contacts.getJSONObject(0).getString(KEY_EMAIL));
  }
  /** Null pocData falls back to bpartnerId from the vars session parameter. */
  @Test
  public void testBuildBPContactsJson_nullPocData_fallsBackToVarsParameter() throws JSONException {
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getStringParameter("bpartnerId")).thenReturn("bp-from-vars");
    User contact = mockUser("u2", "Bob", BOB_EMAIL);
    bpContactSelectorStatic
        .when(() -> BPContactEmailSelector.getBPContactsWithEmail("bp-from-vars"))
        .thenReturn(Collections.singletonList(contact));

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, null);

    assertEquals(1, result.getJSONArray(KEY_CONTACTS).length());
  }
  /** Null contact list from selector returns empty contacts array. */
  @Test
  public void testBuildBPContactsJson_singleCustomer_nullContactList_returnsEmptyContacts() throws JSONException {
    PocData doc = pocData("bp1", "u1", ALICE_NAME, ALICE_EMAIL);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    bpContactSelectorStatic
        .when(() -> BPContactEmailSelector.getBPContactsWithEmail("bp1"))
        .thenReturn(null);

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, new PocData[]{ doc });

    assertEquals(0, result.getJSONArray(KEY_CONTACTS).length());
  }
  /** Empty contact list from selector returns empty contacts array. */
  @Test
  public void testBuildBPContactsJson_singleCustomer_emptyContactList_returnsEmptyContacts() throws JSONException {
    PocData doc = pocData("bp1", "u1", ALICE_NAME, ALICE_EMAIL);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    bpContactSelectorStatic
        .when(() -> BPContactEmailSelector.getBPContactsWithEmail("bp1"))
        .thenReturn(Collections.emptyList());

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, new PocData[]{ doc });

    assertEquals(0, result.getJSONArray(KEY_CONTACTS).length());
  }
  /** Multiple contacts from selector are all included in the response. */
  @Test
  public void testBuildBPContactsJson_singleCustomer_multipleContacts_allReturned() throws JSONException {
    PocData doc = pocData("bp1", "u1", ALICE_NAME, ALICE_EMAIL);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    User c1 = mockUser("u1", ALICE_NAME, ALICE_EMAIL);
    User c2 = mockUser("u2", "Bob", BOB_EMAIL);
    bpContactSelectorStatic
        .when(() -> BPContactEmailSelector.getBPContactsWithEmail("bp1"))
        .thenReturn(Arrays.asList(c1, c2));

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, new PocData[]{ doc });

    assertEquals(2, result.getJSONArray(KEY_CONTACTS).length());
  }

  // -------------------------------------------------------------------------
  // buildErrorJson
  // -------------------------------------------------------------------------
  /** buildErrorJson returns a JSON object with error set to true. */
  @Test
  public void testBuildErrorJson_returnsObjectWithErrorTrue() throws JSONException {
    JSONObject error = PrintControllerJsonHelper.buildErrorJson();
    assertTrue(error.getBoolean("error"));
  }

  // -------------------------------------------------------------------------
  // helpers
  // -------------------------------------------------------------------------

  private static PocData pocData(String bpartnerId, String userId, String name, String email) {
    PocData d = new PocData();
    d.bpartnerId = bpartnerId;
    d.contactUserId = userId;
    d.contactName = name;
    d.contactEmail = email;
    return d;
  }

  private static User mockUser(String id, String name, String email) {
    User user = mock(User.class);
    when(user.getId()).thenReturn(id);
    when(user.getName()).thenReturn(name);
    when(user.getEmail()).thenReturn(email);
    when(user.isActive()).thenReturn(Boolean.TRUE);
    return user;
  }
}
