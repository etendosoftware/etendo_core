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
package org.openbravo.erpCommon.utility.reporting.printing; //NOSONAR

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.model.ad.access.User;

/**
 * Tests for {@link BPContactEmailSelector} pure-logic methods that do not require the DAL.
 */
@SuppressWarnings({"java:S100", "java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class BPContactEmailSelectorTest {

  // -------------------------------------------------------------------------
  // selectFromCandidates
  // -------------------------------------------------------------------------
  /** Null contacts list returns null. */
  @Test
  public void testSelectFromCandidates_nullContacts_returnsNull() {
    assertNull(BPContactEmailSelector.selectFromCandidates(null, null));
  }
  /** Empty contacts list returns null. */
  @Test
  public void testSelectFromCandidates_emptyContacts_returnsNull() {
    assertNull(BPContactEmailSelector.selectFromCandidates(Collections.emptyList(), null));
  }
  /** Default-for-docs contact is preferred over last-used. */
  @Test
  public void testSelectFromCandidates_defaultForDocsContact_isPreferred() {
    User defaultContact = mockUser(true, true, "default@a.com");
    User lastUsed = mockUser(false, true, "lastused@a.com");

    User result = BPContactEmailSelector.selectFromCandidates(
        Arrays.asList(lastUsed, defaultContact), lastUsed);

    assertEquals(defaultContact, result);
  }
  /** Last-used contact is returned when no default-for-docs exists. */
  @Test
  public void testSelectFromCandidates_noDefault_lastUsedReturned() {
    User contact1 = mockUser(false, true, "c1@a.com");
    User lastUsed = mockUser(false, true, "lastused@a.com");

    User result = BPContactEmailSelector.selectFromCandidates(
        Arrays.asList(contact1, lastUsed), lastUsed);

    assertEquals(lastUsed, result);
  }
  /** First active contact is returned when no default and no last-used. */
  @Test
  public void testSelectFromCandidates_noDefaultNoLastUsed_firstActiveReturned() {
    User inactive = mockUser(false, false, "inactive@a.com");
    User active = mockUser(false, true, "active@a.com");

    User result = BPContactEmailSelector.selectFromCandidates(
        Arrays.asList(inactive, active), null);

    assertEquals(active, result);
  }
  /** When all contacts are inactive, the first one is returned as fallback. */
  @Test
  public void testSelectFromCandidates_allInactive_firstContactReturned() {
    User first = mockUser(false, false, "first@a.com");
    User second = mockUser(false, false, "second@a.com");

    User result = BPContactEmailSelector.selectFromCandidates(
        Arrays.asList(first, second), null);

    assertEquals(first, result);
  }

  // -------------------------------------------------------------------------
  // findDefaultForDocs
  // -------------------------------------------------------------------------
  /** Contact with isDefaultForDocs=true is found. */
  @Test
  public void testFindDefaultForDocs_contactMarkedAsDefault_isFound() {
    User defaultContact = mockUser(true, true, "d@a.com");
    User regular = mockUser(false, true, "r@a.com");
    List<User> contacts = Arrays.asList(regular, defaultContact);

    Optional<User> result = BPContactEmailSelector.findDefaultForDocs(contacts);

    assertEquals(defaultContact, result.orElse(null));
  }
  /** Empty when no contact is marked as default. */
  @Test
  public void testFindDefaultForDocs_noDefaultContact_returnsEmpty() {
    User c1 = mockUser(false, true, "c1@a.com");
    User c2 = mockUser(false, true, "c2@a.com");

    Optional<User> result = BPContactEmailSelector.findDefaultForDocs(Arrays.asList(c1, c2));

    assertEquals(Optional.empty(), result);
  }

  // -------------------------------------------------------------------------
  // findFirstActiveContact
  // -------------------------------------------------------------------------
  /** First active contact in the list is returned. */
  @Test
  public void testFindFirstActiveContact_firstActiveIsReturned() {
    User inactive = mockUser(false, false, "i@a.com");
    User active = mockUser(false, true, "a@a.com");

    User result = BPContactEmailSelector.findFirstActiveContact(Arrays.asList(inactive, active));

    assertEquals(active, result);
  }
  /** When all contacts are inactive the first one is returned as fallback. */
  @Test
  public void testFindFirstActiveContact_allInactive_firstReturned() {
    User first = mockUser(false, false, "first@a.com");
    User second = mockUser(false, false, "second@a.com");

    User result = BPContactEmailSelector.findFirstActiveContact(Arrays.asList(first, second));

    assertEquals(first, result);
  }

  // -------------------------------------------------------------------------
  // helpers
  // -------------------------------------------------------------------------

  private static User mockUser(boolean isDefaultForDocs, boolean isActive, String email) {
    User user = mock(User.class);
    when(user.get(User.PROPERTY_ISDEFAULTFORDOCS)).thenReturn(isDefaultForDocs);
    when(user.isActive()).thenReturn(isActive);
    when(user.getEmail()).thenReturn(email);
    return user;
  }
}
