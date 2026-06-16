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

@SuppressWarnings({ "java:S120" })
@RunWith(MockitoJUnitRunner.Silent.class)
public class PrintControllerJsonHelperTest {

  private MockedStatic<BPContactEmailSelector> bpContactSelectorStatic;

  @Before
  public void setUp() {
    bpContactSelectorStatic = mockStatic(BPContactEmailSelector.class);
  }

  @After
  public void tearDown() {
    if (bpContactSelectorStatic != null) {
      bpContactSelectorStatic.close();
    }
  }

  // -------------------------------------------------------------------------
  // buildBPContactsJson — multi-customer path (2+ distinct bpartnerIds)
  // -------------------------------------------------------------------------

  @Test
  public void testBuildBPContactsJson_multiCustomer_returnsOneContactPerUniqueEmail() throws JSONException {
    PocData doc1 = pocData("bp1", "u1", "Alice", "a@test.com");
    PocData doc2 = pocData("bp2", "u2", "Bob", "b@test.com");
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, new PocData[]{ doc1, doc2 });

    JSONArray contacts = result.getJSONArray("contacts");
    assertEquals(2, contacts.length());
    assertEquals("a@test.com", contacts.getJSONObject(0).getString("email"));
    assertEquals("b@test.com", contacts.getJSONObject(1).getString("email"));
  }

  @Test
  public void testBuildBPContactsJson_multiCustomer_duplicateEmailDeduped() throws JSONException {
    PocData doc1 = pocData("bp1", "u1", "Alice", "shared@test.com");
    PocData doc2 = pocData("bp2", "u2", "Alice2", "shared@test.com");
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, new PocData[]{ doc1, doc2 });

    assertEquals(1, result.getJSONArray("contacts").length());
  }

  @Test
  public void testBuildBPContactsJson_multiCustomer_blankEmailSkipped() throws JSONException {
    PocData doc1 = pocData("bp1", "u1", "Alice", "a@test.com");
    PocData doc2 = pocData("bp2", "u2", "NoEmail", "");
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, new PocData[]{ doc1, doc2 });

    assertEquals(1, result.getJSONArray("contacts").length());
  }

  @Test
  public void testBuildBPContactsJson_multiCustomer_contactJsonFields() throws JSONException {
    PocData doc1 = pocData("bp1", "uid1", "Alice", "a@test.com");
    PocData doc2 = pocData("bp2", "uid2", "Bob", "b@test.com");
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, new PocData[]{ doc1, doc2 });

    JSONObject first = result.getJSONArray("contacts").getJSONObject(0);
    assertEquals("uid1", first.getString("contactId"));
    assertEquals("Alice", first.getString("name"));
    assertEquals("a@test.com", first.getString("email"));
    assertEquals(false, first.getBoolean("isDefault"));
    assertEquals(true, first.getBoolean("isActive"));
  }

  // -------------------------------------------------------------------------
  // buildBPContactsJson — single-customer path (all same bpartnerId)
  // -------------------------------------------------------------------------

  @Test
  public void testBuildBPContactsJson_singleCustomer_returnsBPContactsFromSelector() throws JSONException {
    PocData doc = pocData("bp1", "u1", "Alice", "a@test.com");
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    User contact = mockUser("u1", "Alice", "a@test.com");
    bpContactSelectorStatic
        .when(() -> BPContactEmailSelector.getBPContactsWithEmail("bp1"))
        .thenReturn(Collections.singletonList(contact));

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, new PocData[]{ doc });

    JSONArray contacts = result.getJSONArray("contacts");
    assertEquals(1, contacts.length());
    assertEquals("u1", contacts.getJSONObject(0).getString("contactId"));
    assertEquals("a@test.com", contacts.getJSONObject(0).getString("email"));
  }

  @Test
  public void testBuildBPContactsJson_nullPocData_fallsBackToVarsParameter() throws JSONException {
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getStringParameter("bpartnerId")).thenReturn("bp-from-vars");
    User contact = mockUser("u2", "Bob", "b@test.com");
    bpContactSelectorStatic
        .when(() -> BPContactEmailSelector.getBPContactsWithEmail("bp-from-vars"))
        .thenReturn(Collections.singletonList(contact));

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, null);

    assertEquals(1, result.getJSONArray("contacts").length());
  }

  @Test
  public void testBuildBPContactsJson_singleCustomer_nullContactList_returnsEmptyContacts() throws JSONException {
    PocData doc = pocData("bp1", "u1", "Alice", "a@test.com");
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    bpContactSelectorStatic
        .when(() -> BPContactEmailSelector.getBPContactsWithEmail("bp1"))
        .thenReturn(null);

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, new PocData[]{ doc });

    assertEquals(0, result.getJSONArray("contacts").length());
  }

  @Test
  public void testBuildBPContactsJson_singleCustomer_emptyContactList_returnsEmptyContacts() throws JSONException {
    PocData doc = pocData("bp1", "u1", "Alice", "a@test.com");
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    bpContactSelectorStatic
        .when(() -> BPContactEmailSelector.getBPContactsWithEmail("bp1"))
        .thenReturn(Collections.emptyList());

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, new PocData[]{ doc });

    assertEquals(0, result.getJSONArray("contacts").length());
  }

  @Test
  public void testBuildBPContactsJson_singleCustomer_multipleContacts_allReturned() throws JSONException {
    PocData doc = pocData("bp1", "u1", "Alice", "a@test.com");
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    User c1 = mockUser("u1", "Alice", "a@test.com");
    User c2 = mockUser("u2", "Bob", "b@test.com");
    bpContactSelectorStatic
        .when(() -> BPContactEmailSelector.getBPContactsWithEmail("bp1"))
        .thenReturn(Arrays.asList(c1, c2));

    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, new PocData[]{ doc });

    assertEquals(2, result.getJSONArray("contacts").length());
  }

  // -------------------------------------------------------------------------
  // buildErrorJson
  // -------------------------------------------------------------------------

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
