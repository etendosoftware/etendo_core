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
import org.mockito.ArgumentMatchers;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
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
  private static final String MSG_DEFAULT_PRESELECTED =
      "Default-for-docs contact must be preselected";
  private static final String MSG_LAST_USED_PRESELECTED =
      "Last-used contact must be preselected when no default exists";
  private static final String MSG_FIRST_ACTIVE_PRESELECTED =
      "First active contact (A-Z) must be preselected when no default or last-used";
  private static final String MSG_NULL_FOR_EMPTY =
      "Must return null when BP has no contacts with email";
  private static final String MSG_FALLBACK_INACTIVE =
      "Must fall back to first contact even if inactive";
  private static final String MSG_SINGLE_CONTACT =
      "Single available contact must always be returned";
  private static final String MSG_RETURN_CONTACTS_FROM_CRITERIA =
      "Must return contacts from criteria";
  private static final String MSG_NEVER_RETURN_NULL = "Must never return null";
  private static final String MSG_RETURN_EMPTY_LIST = "Must return empty list";
  private static final String MSG_NULL_WHEN_NO_LAST_USED =
      "Must return null when no last-used record exists";
  private static final String MSG_RETURN_VALID_LAST_USED =
      "Must return the valid last-used contact";
  private static final String MSG_NULL_WHEN_CONTACT_INACTIVE =
      "Must return null when last-used contact is inactive";
  private static final String MSG_NULL_WHEN_BLANK_EMAIL =
      "Must return null when last-used contact has blank email";
  private static final String MSG_NULL_WHEN_CONTACT_REF_NULL =
      "Must return null when contact reference is null";
  private static final String MSG_RETURN_ONLY_ACTIVE_CONTACT =
      "Must return the only active contact";
  private static final String MSG_EMPTY_FOR_BLANK_EMAIL =
      "Must return empty string for blank email";
  private static final String MSG_EMPTY_FOR_NULL_EMAIL =
      "Must return empty string for null email";
  private static final String MSG_FIND_CASE_INSENSITIVE =
      "Must find contact by case-insensitive email match";
  private static final String MSG_EMPTY_WHEN_NO_MATCH =
      "Must return empty string when no email matches";
  private static final String MSG_RETURN_EXISTING_RECORD = "Must return the existing record";
  private static final String MSG_NULL_WHEN_NO_RECORD = "Must return null when no record exists";
  private static final String MSG_SAVE_ERROR = "Error saving last-used email contact";
  private static final String BP_ID = "TEST_BP_ID";
  private static final String SENDING_USER_ID = "TEST_SENDING_USER_ID";
  private static final String CONTACT_USER_ID = "TEST_CONTACT_USER_ID";
  private static final String CONTACT_EMAIL = "contact@example.com";
  private static final String CONTACT_EMAIL_UPPERCASE_PADDED = " CONTACT@EXAMPLE.COM ";
  private static final String OTHER_EMAIL = "other@example.com";
  private static final String BLANK_STRING = "  ";
  private static final String EMPTY_STRING = "";
  
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
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;

  /**
   * Sets up static mocks for the DAL layer before each test.
   */
  @Before
  public void setUp() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBProvider = mockStatic(OBProvider.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);
    mockedOBProvider.when(OBProvider::getInstance).thenReturn(obProvider);
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(ArgumentMatchers.anyString()))
        .thenReturn(MSG_SAVE_ERROR);
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
    mockedOBMessageUtils.close();
  }

  /**
   * Verifies that a contact marked with {@code isdefaultfordocs = true} is always
   * returned, regardless of other flags or a last-used contact being present.
   */
  @Test
  public void testSelectFromCandidatesDefaultContactTakesPriority() {
    stubIsDefaultForDocs(defaultContact, true);
    stubIsDefaultForDocs(activeContactA, false);
    List<User> contacts = Arrays.asList(activeContactA, defaultContact);
    User result = BPContactEmailSelector.selectFromCandidates(contacts, lastUsedContact);
    assertEquals(MSG_DEFAULT_PRESELECTED, defaultContact, result);
  }

  /**
   * Verifies that when no contact is marked as default, the last-used contact takes
   * priority over alphabetical ordering.
   */
  @Test
  public void testSelectFromCandidatesLastUsedTakesPriorityOverAlphabetical() {
    stubIsDefaultForDocs(activeContactA, false);
    stubIsDefaultForDocs(activeContactB, false);
    List<User> contacts = Arrays.asList(activeContactA, activeContactB);
    User result = BPContactEmailSelector.selectFromCandidates(contacts, lastUsedContact);
    assertEquals(MSG_LAST_USED_PRESELECTED, lastUsedContact, result);
  }

  /**
   * Verifies that when there is no default contact and no last-used contact, the first
   * active contact in the already-sorted list is returned.
   */
  @Test
  public void testSelectFromCandidatesFirstActiveWhenNoDefaultOrLastUsed() {
    stubIsDefaultForDocs(activeContactA, false);
    stubIsDefaultForDocs(activeContactB, false);
    when(activeContactA.isActive()).thenReturn(Boolean.TRUE);
    List<User> contacts = Arrays.asList(activeContactA, activeContactB);
    User result = BPContactEmailSelector.selectFromCandidates(contacts, null);
    assertEquals(MSG_FIRST_ACTIVE_PRESELECTED, activeContactA, result);
  }

  /**
   * Verifies that {@code null} is returned when the contact list is empty.
   */
  @Test
  public void testSelectFromCandidatesReturnsNullForEmptyList() {
    User result = BPContactEmailSelector.selectFromCandidates(Collections.emptyList(), null);
    assertNull(MSG_NULL_FOR_EMPTY, result);
  }

  /**
   * Verifies that {@code null} is returned when the contact list is {@code null}.
   */
  @Test
  public void testSelectFromCandidatesReturnsNullForNullList() {
    User result = BPContactEmailSelector.selectFromCandidates(null, lastUsedContact);
    assertNull(MSG_NULL_FOR_EMPTY, result);
  }

  /**
   * Verifies that when all contacts are inactive and there is no default or last-used
   * contact, the first contact in the list is returned as a fallback.
   */
  @Test
  public void testSelectFromCandidatesFallbackToFirstWhenAllInactive() {
    stubIsDefaultForDocs(inactiveContact, false);
    when(inactiveContact.isActive()).thenReturn(Boolean.FALSE);
    List<User> contacts = Collections.singletonList(inactiveContact);
    User result = BPContactEmailSelector.selectFromCandidates(contacts, null);
    assertEquals(MSG_FALLBACK_INACTIVE, inactiveContact, result);
  }

  /**
   * Verifies that a single active contact with no default flag is correctly returned.
   */
  @Test
  public void testSelectFromCandidatesSingleContactAlwaysReturned() {
    stubIsDefaultForDocs(activeContactA, false);
    when(activeContactA.isActive()).thenReturn(Boolean.TRUE);
    List<User> contacts = Collections.singletonList(activeContactA);
    User result = BPContactEmailSelector.selectFromCandidates(contacts, null);
    assertEquals(MSG_SINGLE_CONTACT, activeContactA, result);
  }

  /**
   * Verifies that the criteria is built and returns the list from DAL.
   */
  @Test
  public void testGetBPContactsWithEmailReturnsCriteriaResults() {
    stubUserCriteria();
    List<User> expectedContacts = Arrays.asList(activeContactA, activeContactB);
    when(userCriteria.list()).thenReturn(expectedContacts);
    List<User> result = BPContactEmailSelector.getBPContactsWithEmail(BP_ID);
    assertEquals(MSG_RETURN_CONTACTS_FROM_CRITERIA, expectedContacts, result);
  }

  /**
   * Verifies that an empty list is returned when no contacts match.
   */
  @Test
  public void testGetBPContactsWithEmailReturnsEmptyWhenNoContacts() {
    stubUserCriteria();
    when(userCriteria.list()).thenReturn(Collections.emptyList());
    List<User> result = BPContactEmailSelector.getBPContactsWithEmail(BP_ID);
    assertNotNull(MSG_NEVER_RETURN_NULL, result);
    assertEquals(MSG_RETURN_EMPTY_LIST, 0, result.size());
  }

  /**
   * Verifies that {@code null} is returned when no last-used record exists.
   */
  @Test
  public void testGetLastUsedContactReturnsNullWhenNoRecord() {
    stubEmailBpContactCriteriaEmpty();
    User result = BPContactEmailSelector.getLastUsedContact(SENDING_USER_ID, BP_ID);
    assertNull(MSG_NULL_WHEN_NO_LAST_USED, result);
  }

  /**
   * Verifies that the contact is returned when the last-used record exists and
   * the contact is active with a valid email.
   */
  @Test
  public void testGetLastUsedContactReturnsValidContact() {
    stubEmailBpContactCriteriaWithRecord();
    when(mockEmailBpContact.getContactAdUser()).thenReturn(activeContactA);
    when(activeContactA.isActive()).thenReturn(Boolean.TRUE);
    when(activeContactA.getEmail()).thenReturn(CONTACT_EMAIL);
    User result = BPContactEmailSelector.getLastUsedContact(SENDING_USER_ID, BP_ID);
    assertEquals(MSG_RETURN_VALID_LAST_USED, activeContactA, result);
  }

  /**
   * Verifies that {@code null} is returned when the last-used contact is inactive.
   */
  @Test
  public void testGetLastUsedContactReturnsNullWhenContactInactive() {
    stubEmailBpContactCriteriaWithRecord();
    when(mockEmailBpContact.getContactAdUser()).thenReturn(inactiveContact);
    when(inactiveContact.isActive()).thenReturn(Boolean.FALSE);
    User result = BPContactEmailSelector.getLastUsedContact(SENDING_USER_ID, BP_ID);
    assertNull(MSG_NULL_WHEN_CONTACT_INACTIVE, result);
  }

  /**
   * Verifies that {@code null} is returned when the last-used contact has a blank
   * email.
   */
  @Test
  public void testGetLastUsedContactReturnsNullWhenContactHasNoEmail() {
    User contactWithBlankEmail = mock(User.class);
    stubEmailBpContactCriteriaWithRecord();
    when(mockEmailBpContact.getContactAdUser()).thenReturn(contactWithBlankEmail);
    when(contactWithBlankEmail.isActive()).thenReturn(Boolean.TRUE);
    when(contactWithBlankEmail.getEmail()).thenReturn(BLANK_STRING);
    User result = BPContactEmailSelector.getLastUsedContact(SENDING_USER_ID, BP_ID);
    assertNull(MSG_NULL_WHEN_BLANK_EMAIL, result);
  }

  /**
   * Verifies that {@code null} is returned when the record exists but the contact
   * reference is {@code null} (orphaned record).
   */
  @Test
  public void testGetLastUsedContactReturnsNullWhenContactAdUserIsNull() {
    stubEmailBpContactCriteriaWithRecord();
    when(mockEmailBpContact.getContactAdUser()).thenReturn(null);
    User result = BPContactEmailSelector.getLastUsedContact(SENDING_USER_ID, BP_ID);
    assertNull(MSG_NULL_WHEN_CONTACT_REF_NULL, result);
  }
  
  /**
   * Verifies the full flow: contacts are fetched, last-used is resolved, and
   * the best candidate is returned.
   */
  @Test
  public void testSelectBestContactFullFlow() {
    stubUserCriteria();
    stubIsDefaultForDocs(activeContactA, false);
    when(activeContactA.isActive()).thenReturn(Boolean.TRUE);
    when(userCriteria.list()).thenReturn(Collections.singletonList(activeContactA));
    stubEmailBpContactCriteriaEmpty();
    User result = BPContactEmailSelector.selectBestContact(BP_ID, SENDING_USER_ID);
    assertEquals(MSG_RETURN_ONLY_ACTIVE_CONTACT, activeContactA, result);
  }

  /**
   * Verifies that saving is skipped when the contact ID is blank.
   * @throws ServletException if an unexpected persistence error occurs
   */
  @Test
  public void testSaveLastUsedContactSkipsWhenContactIdBlank() throws ServletException {
    BPContactEmailSelector.saveLastUsedContact(SENDING_USER_ID, BP_ID, BLANK_STRING);
    verify(obDal, never()).flush();
  }

  /**
   * Verifies that saving is skipped when the contact ID is {@code null}.
   * @throws ServletException if an unexpected persistence error occurs
   */
  @Test
  public void testSaveLastUsedContactSkipsWhenContactIdNull() throws ServletException {
    BPContactEmailSelector.saveLastUsedContact(SENDING_USER_ID, BP_ID, null);
    verify(obDal, never()).flush();
  }

  /**
   * Verifies that an existing record is updated with the new contact.
   * @throws ServletException if an unexpected persistence error occurs
   */
  @Test
  public void testSaveLastUsedContactUpdatesExistingRecord() throws ServletException {
    stubEmailBpContactCriteriaWithRecord();
    when(obDal.get(User.class, CONTACT_USER_ID)).thenReturn(contactUser);
    BPContactEmailSelector.saveLastUsedContact(SENDING_USER_ID, BP_ID, CONTACT_USER_ID);
    verify(mockEmailBpContact).setContactAdUser(contactUser);
    verify(obDal).flush();
  }

  /**
   * Verifies that a new record is inserted when no existing record is found.
   * @throws ServletException if an unexpected persistence error occurs
   */
  @Test
  public void testSaveLastUsedContactInsertsNewRecord() throws ServletException {
    User sendingUser = mock(User.class);
    stubEmailBpContactCriteriaEmpty();
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
   * Verifies that a {@link ServletException} is thrown when an unexpected error
   * occurs during persistence.
   */
  @Test
  public void testSaveLastUsedContactThrowsServletExceptionOnError() {
    stubEmailBpContactCriteria();
    when(emailBpContactCriteria.uniqueResult()).thenThrow(new RuntimeException("DB error"));
    try {
      BPContactEmailSelector.saveLastUsedContact(SENDING_USER_ID, BP_ID, CONTACT_USER_ID);
      fail("Expected ServletException to be thrown");
    } catch (ServletException e) {
      assertEquals(MSG_SAVE_ERROR, e.getMessage());
    }
  }

  /**
   * Verifies that an empty string is returned when the email parameter is blank.
   */
  @Test
  public void testFindContactIdByEmailReturnsEmptyForBlankEmail() {
    String result = BPContactEmailSelector.findContactIdByEmail(BP_ID, BLANK_STRING);
    assertEquals(MSG_EMPTY_FOR_BLANK_EMAIL, EMPTY_STRING, result);
  }

  /**
   * Verifies that an empty string is returned when the email parameter is
   * {@code null}.
   */
  @Test
  public void testFindContactIdByEmailReturnsEmptyForNullEmail() {
    String result = BPContactEmailSelector.findContactIdByEmail(BP_ID, null);
    assertEquals(MSG_EMPTY_FOR_NULL_EMAIL, EMPTY_STRING, result);
  }

  /**
   * Verifies that the correct contact ID is returned when a matching email is
   * found (case-insensitive, with whitespace trimming).
   */
  @Test
  public void testFindContactIdByEmailFindsMatchingContact() {
    User matchingContact = mock(User.class);
    when(matchingContact.getEmail()).thenReturn(CONTACT_EMAIL);
    when(matchingContact.getId()).thenReturn(CONTACT_USER_ID);
    stubUserCriteria();
    when(userCriteria.list()).thenReturn(Collections.singletonList(matchingContact));
    String result = BPContactEmailSelector.findContactIdByEmail(
        BP_ID, CONTACT_EMAIL_UPPERCASE_PADDED);
    assertEquals(MSG_FIND_CASE_INSENSITIVE, CONTACT_USER_ID, result);
  }

  /**
   * Verifies that an empty string is returned when no contact matches the given
   * email.
   */
  @Test
  public void testFindContactIdByEmailReturnsEmptyWhenNoMatch() {
    User nonMatchingContact = mock(User.class);
    when(nonMatchingContact.getEmail()).thenReturn(OTHER_EMAIL);
    stubUserCriteria();
    when(userCriteria.list()).thenReturn(Collections.singletonList(nonMatchingContact));
    String result = BPContactEmailSelector.findContactIdByEmail(BP_ID, CONTACT_EMAIL);
    assertEquals(MSG_EMPTY_WHEN_NO_MATCH, EMPTY_STRING, result);
  }
  
  /**
   * Verifies that the record is returned when it exists.
   */
  @Test
  public void testFindLastUsedRecordReturnsRecordWhenExists() {
    stubEmailBpContactCriteriaWithRecord();
    EmailBpContact result = BPContactEmailSelector.findLastUsedRecord(SENDING_USER_ID, BP_ID);
    assertEquals(MSG_RETURN_EXISTING_RECORD, mockEmailBpContact, result);
  }

  /**
   * Verifies that {@code null} is returned when no record exists.
   */
  @Test
  public void testFindLastUsedRecordReturnsNullWhenEmpty() {
    stubEmailBpContactCriteriaEmpty();
    EmailBpContact result = BPContactEmailSelector.findLastUsedRecord(SENDING_USER_ID, BP_ID);
    assertNull(MSG_NULL_WHEN_NO_RECORD, result);
  }

  /**
   * Verifies that the contact is set on the existing record.
   */
  @Test
  public void testUpdateLastUsedRecordSetsContactOnRecord() {
    BPContactEmailSelector.updateLastUsedRecord(mockEmailBpContact, contactUser);
    verify(mockEmailBpContact).setContactAdUser(contactUser);
  }

  /**
   * Stubs the {@code isDefaultForDocs} property on the given user mock.
   * @param user the user mock to configure
   * @param isDefaultForDocs whether the contact is the default for documents
   */
  private void stubIsDefaultForDocs(User user, boolean isDefaultForDocs) {
    when(user.get(User.PROPERTY_ISDEFAULTFORDOCS)).thenReturn(isDefaultForDocs);
  }

  /**
   * Stubs {@link OBDal#createCriteria(Class)} for {@link User} to return the
   * shared {@code userCriteria} mock.
   */
  private void stubUserCriteria() {
    when(obDal.createCriteria(User.class)).thenReturn(userCriteria);
  }

  /**
   * Stubs {@link OBDal#createCriteria(Class)} for {@link EmailBpContact} to
   * return the shared {@code emailBpContactCriteria} mock.
   */
  private void stubEmailBpContactCriteria() {
    when(obDal.createCriteria(EmailBpContact.class)).thenReturn(emailBpContactCriteria);
  }

  /**
   * Stubs the {@link EmailBpContact} criteria to return an empty list,
   * simulating no existing last-used record.
   */
  private void stubEmailBpContactCriteriaEmpty() {
    stubEmailBpContactCriteria();
    when(emailBpContactCriteria.uniqueResult()).thenReturn(null);
  }

  /**
   * Stubs the {@link EmailBpContact} criteria to return {@code mockEmailBpContact}
   * via {@code uniqueResult()}, simulating an existing last-used record.
   */
  private void stubEmailBpContactCriteriaWithRecord() {
    stubEmailBpContactCriteria();
    when(emailBpContactCriteria.uniqueResult()).thenReturn(mockEmailBpContact);
  }
}
