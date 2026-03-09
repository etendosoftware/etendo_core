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
package org.openbravo.test.reporting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.reporting.printing.BPContactEmailSelector;
import org.openbravo.model.ad.access.EmailBpContact;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for {@link BPContactEmailSelector}.
 * Uses {@link MockedStatic} to mock DAL layer classes ({@link OBDal}, {@link OBContext},
 * {@link OBProvider}) so that all methods can be tested without a database connection.
 */
@RunWith(MockitoJUnitRunner.class)
public class BPContactEmailSelectorTest {

  private static final String MSG_DEFAULT_PRESELECTED = "Default-for-docs contact must be preselected";
  private static final String MSG_LAST_USED_PRESELECTED = "Last-used contact must be preselected when no default exists";
  private static final String MSG_FIRST_ACTIVE_PRESELECTED = "First active contact (A-Z) must be preselected when no default or last-used";
  private static final String MSG_NULL_FOR_EMPTY = "Must return null when BP has no contacts with email";
  private static final String MSG_FALLBACK_INACTIVE = "Must fall back to first contact even if inactive";
  private static final String MSG_SINGLE_CONTACT = "Single available contact must always be returned";

  private static final String BP_ID = "TEST_BP_ID";
  private static final String SENDING_USER_ID = "TEST_SENDING_USER_ID";
  private static final String CONTACT_USER_ID = "TEST_CONTACT_USER_ID";
  private static final String CONTACT_EMAIL = "contact@example.com";

  @Mock private User defaultContact;
  @Mock private User lastUsedContact;
  @Mock private User activeContactA;
  @Mock private User activeContactB;
  @Mock private User inactiveContact;
  @Mock private User contactUser;

  @Mock private OBDal obDal;
  @Mock private OBContext obContext;
  @Mock private OBProvider obProvider;
  @Mock private Client mockClient;
  @Mock private Organization mockOrganization;
  @Mock private OBCriteria<User> userCriteria;
  @Mock private OBCriteria<EmailBpContact> emailBpContactCriteria;
  @Mock private EmailBpContact mockEmailBpContact;
  @Mock private BusinessPartner mockBusinessPartner;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBProvider> mockedOBProvider;

  /**
   * Sets up static mocks for the DAL layer before each test.
   */
  @Before
  public void setUp() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBProvider = mockStatic(OBProvider.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);
    mockedOBProvider.when(OBProvider::getInstance).thenReturn(obProvider);
    when(obContext.getCurrentClient()).thenReturn(mockClient);
    when(obContext.getCurrentOrganization()).thenReturn(mockOrganization);
  }

  /**
   * Closes all static mocks after each test to avoid leaks.
   */
  @After
  public void tearDown() {
    mockedOBDal.close();
    mockedOBContext.close();
    mockedOBProvider.close();
  }

  /**
   * Verifies that a contact marked with {@code isdefaultfordocs = true} is always returned,
   * regardless of other flags or a last-used contact being present.
   */
  @Test
  public void testSelectFromCandidates_defaultContactTakesPriority() {
    when(defaultContact.get(User.PROPERTY_ISDEFAULTFORDOCS)).thenReturn(Boolean.TRUE);
    when(activeContactA.get(User.PROPERTY_ISDEFAULTFORDOCS)).thenReturn(Boolean.FALSE);
    List<User> contacts = Arrays.asList(activeContactA, defaultContact);
    User result = BPContactEmailSelector.selectFromCandidates(contacts, lastUsedContact);
    assertEquals(MSG_DEFAULT_PRESELECTED, defaultContact, result);
  }

  /**
   * Verifies that when no contact is marked as default, the last-used contact takes priority
   * over alphabetical ordering.
   */
  @Test
  public void testSelectFromCandidates_lastUsedTakesPriorityOverAlphabetical() {
    when(activeContactA.get(User.PROPERTY_ISDEFAULTFORDOCS)).thenReturn(Boolean.FALSE);
    when(activeContactB.get(User.PROPERTY_ISDEFAULTFORDOCS)).thenReturn(Boolean.FALSE);
    List<User> contacts = Arrays.asList(activeContactA, activeContactB);
    User result = BPContactEmailSelector.selectFromCandidates(contacts, lastUsedContact);
    assertEquals(MSG_LAST_USED_PRESELECTED, lastUsedContact, result);
  }

  /**
   * Verifies that when there is no default contact and no last-used contact, the first active
   * contact in the already-sorted list is returned.
   */
  @Test
  public void testSelectFromCandidates_firstActiveWhenNoDefaultOrLastUsed() {
    when(activeContactA.get(User.PROPERTY_ISDEFAULTFORDOCS)).thenReturn(Boolean.FALSE);
    when(activeContactB.get(User.PROPERTY_ISDEFAULTFORDOCS)).thenReturn(Boolean.FALSE);
    when(activeContactA.isActive()).thenReturn(Boolean.TRUE);
    List<User> contacts = Arrays.asList(activeContactA, activeContactB);
    User result = BPContactEmailSelector.selectFromCandidates(contacts, null);
    assertEquals(MSG_FIRST_ACTIVE_PRESELECTED, activeContactA, result);
  }

  /**
   * Verifies that {@code null} is returned when the contact list is empty.
   */
  @Test
  public void testSelectFromCandidates_returnsNullForEmptyList() {
    User result = BPContactEmailSelector.selectFromCandidates(Collections.emptyList(), null);
    assertNull(MSG_NULL_FOR_EMPTY, result);
  }

  /**
   * Verifies that {@code null} is returned when the contact list is {@code null}.
   */
  @Test
  public void testSelectFromCandidates_returnsNullForNullList() {
    User result = BPContactEmailSelector.selectFromCandidates(null, lastUsedContact);
    assertNull(MSG_NULL_FOR_EMPTY, result);
  }

  /**
   * Verifies that when all contacts are inactive and there is no default or last-used contact,
   * the first contact in the list is returned as a fallback.
   */
  @Test
  public void testSelectFromCandidates_fallbackToFirstWhenAllInactive() {
    when(inactiveContact.get(User.PROPERTY_ISDEFAULTFORDOCS)).thenReturn(Boolean.FALSE);
    when(inactiveContact.isActive()).thenReturn(Boolean.FALSE);
    List<User> contacts = Collections.singletonList(inactiveContact);
    User result = BPContactEmailSelector.selectFromCandidates(contacts, null);
    assertEquals(MSG_FALLBACK_INACTIVE, inactiveContact, result);
  }

  /**
   * Verifies that a single active contact with no default flag is correctly returned.
   */
  @Test
  public void testSelectFromCandidates_singleContactAlwaysReturned() {
    when(activeContactA.get(User.PROPERTY_ISDEFAULTFORDOCS)).thenReturn(Boolean.FALSE);
    when(activeContactA.isActive()).thenReturn(Boolean.TRUE);
    List<User> contacts = Collections.singletonList(activeContactA);
    User result = BPContactEmailSelector.selectFromCandidates(contacts, null);
    assertEquals(MSG_SINGLE_CONTACT, activeContactA, result);
  }

  /**
   * Verifies that the criteria is built and returns the list from DAL.
   */
  @Test
  public void testGetBPContactsWithEmail_returnsCriteriaResults() {
    when(obDal.createCriteria(User.class)).thenReturn(userCriteria);
    List<User> expectedContacts = Arrays.asList(activeContactA, activeContactB);
    when(userCriteria.list()).thenReturn(expectedContacts);
    List<User> result = BPContactEmailSelector.getBPContactsWithEmail(BP_ID);
    assertEquals("Must return contacts from criteria", expectedContacts, result);
  }

  /**
   * Verifies that an empty list is returned when no contacts match.
   */
  @Test
  public void testGetBPContactsWithEmail_returnsEmptyWhenNoContacts() {
    when(obDal.createCriteria(User.class)).thenReturn(userCriteria);
    when(userCriteria.list()).thenReturn(Collections.emptyList());
    List<User> result = BPContactEmailSelector.getBPContactsWithEmail(BP_ID);
    assertNotNull("Must never return null", result);
    assertEquals("Must return empty list", 0, result.size());
  }

  /**
   * Verifies that {@code null} is returned when no last-used record exists.
   */
  @Test
  public void testGetLastUsedContact_returnsNullWhenNoRecord() {
    when(obDal.createCriteria(EmailBpContact.class)).thenReturn(emailBpContactCriteria);
    when(emailBpContactCriteria.list()).thenReturn(Collections.emptyList());
    User result = BPContactEmailSelector.getLastUsedContact(SENDING_USER_ID, BP_ID);
    assertNull("Must return null when no last-used record exists", result);
  }

  /**
   * Verifies that the contact is returned when the last-used record exists
   * and the contact is active with a valid email.
   */
  @Test
  public void testGetLastUsedContact_returnsValidContact() {
    when(obDal.createCriteria(EmailBpContact.class)).thenReturn(emailBpContactCriteria);
    when(emailBpContactCriteria.list()).thenReturn(Collections.singletonList(mockEmailBpContact));
    when(mockEmailBpContact.getContactAdUser()).thenReturn(activeContactA);
    when(activeContactA.isActive()).thenReturn(Boolean.TRUE);
    when(activeContactA.getEmail()).thenReturn(CONTACT_EMAIL);
    User result = BPContactEmailSelector.getLastUsedContact(SENDING_USER_ID, BP_ID);
    assertEquals("Must return the valid last-used contact", activeContactA, result);
  }

  /**
   * Verifies that {@code null} is returned when the last-used contact is inactive.
   */
  @Test
  public void testGetLastUsedContact_returnsNullWhenContactInactive() {
    when(obDal.createCriteria(EmailBpContact.class)).thenReturn(emailBpContactCriteria);
    when(emailBpContactCriteria.list()).thenReturn(Collections.singletonList(mockEmailBpContact));
    when(mockEmailBpContact.getContactAdUser()).thenReturn(inactiveContact);
    when(inactiveContact.isActive()).thenReturn(Boolean.FALSE);
    User result = BPContactEmailSelector.getLastUsedContact(SENDING_USER_ID, BP_ID);
    assertNull("Must return null when last-used contact is inactive", result);
  }

  /**
   * Verifies that {@code null} is returned when the last-used contact has a blank email.
   */
  @Test
  public void testGetLastUsedContact_returnsNullWhenContactHasNoEmail() {
    User contactWithBlankEmail = mock(User.class);
    when(obDal.createCriteria(EmailBpContact.class)).thenReturn(emailBpContactCriteria);
    when(emailBpContactCriteria.list()).thenReturn(Collections.singletonList(mockEmailBpContact));
    when(mockEmailBpContact.getContactAdUser()).thenReturn(contactWithBlankEmail);
    when(contactWithBlankEmail.isActive()).thenReturn(Boolean.TRUE);
    when(contactWithBlankEmail.getEmail()).thenReturn("  ");
    User result = BPContactEmailSelector.getLastUsedContact(SENDING_USER_ID, BP_ID);
    assertNull("Must return null when last-used contact has blank email", result);
  }

  /**
   * Verifies that {@code null} is returned when the record exists but the contact
   * reference is {@code null} (orphaned record).
   */
  @Test
  public void testGetLastUsedContact_returnsNullWhenContactAdUserIsNull() {
    when(obDal.createCriteria(EmailBpContact.class)).thenReturn(emailBpContactCriteria);
    when(emailBpContactCriteria.list()).thenReturn(Collections.singletonList(mockEmailBpContact));
    when(mockEmailBpContact.getContactAdUser()).thenReturn(null);
    User result = BPContactEmailSelector.getLastUsedContact(SENDING_USER_ID, BP_ID);
    assertNull("Must return null when contact reference is null", result);
  }

  /**
   * Verifies the full flow: contacts are fetched, last-used is resolved,
   * and the best candidate is returned.
   */
  @Test
  public void testSelectBestContact_fullFlow() {
    when(obDal.createCriteria(User.class)).thenReturn(userCriteria);
    when(activeContactA.get(User.PROPERTY_ISDEFAULTFORDOCS)).thenReturn(Boolean.FALSE);
    when(activeContactA.isActive()).thenReturn(Boolean.TRUE);
    when(userCriteria.list()).thenReturn(Collections.singletonList(activeContactA));
    when(obDal.createCriteria(EmailBpContact.class)).thenReturn(emailBpContactCriteria);
    when(emailBpContactCriteria.list()).thenReturn(Collections.emptyList());
    User result = BPContactEmailSelector.selectBestContact(BP_ID, SENDING_USER_ID);
    assertEquals("Must return the only active contact", activeContactA, result);
  }

  /**
   * Verifies that saving is skipped when the contact ID is blank.
   */
  @Test
  public void testSaveLastUsedContact_skipsWhenContactIdBlank() throws ServletException {
    BPContactEmailSelector.saveLastUsedContact(SENDING_USER_ID, BP_ID, "  ");
    verify(obDal, never()).flush();
  }

  /**
   * Verifies that saving is skipped when the contact ID is {@code null}.
   */
  @Test
  public void testSaveLastUsedContact_skipsWhenContactIdNull() throws ServletException {
    BPContactEmailSelector.saveLastUsedContact(SENDING_USER_ID, BP_ID, null);
    verify(obDal, never()).flush();
  }

  /**
   * Verifies that an existing record is updated with the new contact.
   */
  @Test
  public void testSaveLastUsedContact_updatesExistingRecord() throws ServletException {
    when(obDal.createCriteria(EmailBpContact.class)).thenReturn(emailBpContactCriteria);
    when(emailBpContactCriteria.list()).thenReturn(Collections.singletonList(mockEmailBpContact));
    when(obDal.get(User.class, CONTACT_USER_ID)).thenReturn(contactUser);
    BPContactEmailSelector.saveLastUsedContact(SENDING_USER_ID, BP_ID, CONTACT_USER_ID);
    verify(mockEmailBpContact).setContactAdUser(contactUser);
    verify(obDal).flush();
  }

  /**
   * Verifies that a new record is inserted when no existing record is found.
   */
  @Test
  public void testSaveLastUsedContact_insertsNewRecord() throws ServletException {
    User sendingUser = mock(User.class);
    when(obDal.createCriteria(EmailBpContact.class)).thenReturn(emailBpContactCriteria);
    when(emailBpContactCriteria.list()).thenReturn(Collections.emptyList());
    when(obDal.get(User.class, CONTACT_USER_ID)).thenReturn(contactUser);
    when(obDal.get(User.class, SENDING_USER_ID)).thenReturn(sendingUser);
    when(obDal.get(BusinessPartner.class, BP_ID)).thenReturn(mockBusinessPartner);
    when(obProvider.get(EmailBpContact.class)).thenReturn(mockEmailBpContact);
    BPContactEmailSelector.saveLastUsedContact(SENDING_USER_ID, BP_ID, CONTACT_USER_ID);
    verify(mockEmailBpContact).setClient(mockClient);
    verify(mockEmailBpContact).setOrganization(mockOrganization);
    verify(mockEmailBpContact).setUserContact(sendingUser);
    verify(mockEmailBpContact).setBusinessPartner(mockBusinessPartner);
    verify(mockEmailBpContact).setContactAdUser(contactUser);
    verify(obDal).save(mockEmailBpContact);
    verify(obDal).flush();
  }

  /**
   * Verifies that a {@link ServletException} is thrown when an unexpected error occurs
   * during persistence.
   */
  @Test
  public void testSaveLastUsedContact_throwsServletExceptionOnError() {
    when(obDal.createCriteria(EmailBpContact.class)).thenReturn(emailBpContactCriteria);
    when(emailBpContactCriteria.list()).thenThrow(new RuntimeException("DB error"));
    try {
      BPContactEmailSelector.saveLastUsedContact(SENDING_USER_ID, BP_ID, CONTACT_USER_ID);
      fail("Expected ServletException to be thrown");
    } catch (ServletException e) {
      assertEquals("Error saving last-used email contact", e.getMessage());
    }
  }

  /**
   * Verifies that an empty string is returned when the email parameter is blank.
   */
  @Test
  public void testFindContactIdByEmail_returnsEmptyForBlankEmail() {
    String result = BPContactEmailSelector.findContactIdByEmail(BP_ID, "  ");
    assertEquals("Must return empty string for blank email", "", result);
  }

  /**
   * Verifies that an empty string is returned when the email parameter is {@code null}.
   */
  @Test
  public void testFindContactIdByEmail_returnsEmptyForNullEmail() {
    String result = BPContactEmailSelector.findContactIdByEmail(BP_ID, null);
    assertEquals("Must return empty string for null email", "", result);
  }

  /**
   * Verifies that the correct contact ID is returned when a matching email is found
   * (case-insensitive, with whitespace trimming).
   */
  @Test
  public void testFindContactIdByEmail_findsMatchingContact() {
    User matchingContact = mock(User.class);
    when(matchingContact.getEmail()).thenReturn(CONTACT_EMAIL);
    when(matchingContact.getId()).thenReturn(CONTACT_USER_ID);
    when(obDal.createCriteria(User.class)).thenReturn(userCriteria);
    when(userCriteria.list()).thenReturn(Collections.singletonList(matchingContact));
    String result = BPContactEmailSelector.findContactIdByEmail(BP_ID, " CONTACT@EXAMPLE.COM ");
    assertEquals("Must find contact by case-insensitive email match", CONTACT_USER_ID, result);
  }

  /**
   * Verifies that an empty string is returned when no contact matches the given email.
   */
  @Test
  public void testFindContactIdByEmail_returnsEmptyWhenNoMatch() {
    User nonMatchingContact = mock(User.class);
    when(nonMatchingContact.getEmail()).thenReturn("other@example.com");
    when(obDal.createCriteria(User.class)).thenReturn(userCriteria);
    when(userCriteria.list()).thenReturn(Collections.singletonList(nonMatchingContact));
    String result = BPContactEmailSelector.findContactIdByEmail(BP_ID, CONTACT_EMAIL);
    assertEquals("Must return empty string when no email matches", "", result);
  }

  /**
   * Verifies that the record is returned when it exists.
   */
  @Test
  public void testFindLastUsedRecord_returnsRecordWhenExists() {
    when(obDal.createCriteria(EmailBpContact.class)).thenReturn(emailBpContactCriteria);
    when(emailBpContactCriteria.list()).thenReturn(Collections.singletonList(mockEmailBpContact));
    EmailBpContact result = BPContactEmailSelector.findLastUsedRecord(SENDING_USER_ID, BP_ID);
    assertEquals("Must return the existing record", mockEmailBpContact, result);
  }

  /**
   * Verifies that {@code null} is returned when no record exists.
   */
  @Test
  public void testFindLastUsedRecord_returnsNullWhenEmpty() {
    when(obDal.createCriteria(EmailBpContact.class)).thenReturn(emailBpContactCriteria);
    when(emailBpContactCriteria.list()).thenReturn(Collections.emptyList());
    EmailBpContact result = BPContactEmailSelector.findLastUsedRecord(SENDING_USER_ID, BP_ID);
    assertNull("Must return null when no record exists", result);
  }

  /**
   * Verifies that the contact is set on the existing record.
   */
  @Test
  public void testUpdateLastUsedRecord_setsContactOnRecord() {
    BPContactEmailSelector.updateLastUsedRecord(mockEmailBpContact, contactUser);
    verify(mockEmailBpContact).setContactAdUser(contactUser);
  }
}
