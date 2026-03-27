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

import java.util.List;
import java.util.Optional;

import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.EmailBpContact;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.BusinessPartner;

/**
 * Determines the best email contact to preselect when sending a document to a Business Partner.
 */
public class BPContactEmailSelector {

  private static final Logger log = LogManager.getLogger();
  private static final String ERROR_SAVING_EMAIL_CONTACT = "ErrorSavingLastUsedEmailContact";


  /**
   * Private constructor to prevent instantiation of this utility class.
   */
  private BPContactEmailSelector() {
  }

  /**
   * Returns the best {@link User} contact to preselect in the email popup for the given
   * Business Partner and sending user.
   * @param bpartnerId the ID of the Business Partner
   * @param sendingUserId the ID of the system user who is sending the email
   * @return the selected {@link User}, or {@code null} if the BP has no contacts with email
   */
  public static User selectBestContact(String bpartnerId, String sendingUserId) {
    List<User> contacts = getBPContactsWithEmail(bpartnerId);
    User lastUsed = getLastUsedContact(sendingUserId, bpartnerId);
    return selectFromCandidates(contacts, lastUsed);
  }

  /**
   * Applies the priority rule over an already-resolved list of candidates and last-used contact.
   * Extracted as package-protected to allow unit testing without DAL.
   * @param contacts the list of BP contacts with email, ordered by relevance
   * @param lastUsed the last contact used by the sending user for this BP, or {@code null}
   * @return the best candidate {@link User}, or {@code null} if the list is empty
   */
  public static User selectFromCandidates(List<User> contacts, User lastUsed) {
    if (contacts == null || contacts.isEmpty()) {
      return null;
    }
    return findDefaultForDocs(contacts).orElse(lastUsed != null ? lastUsed : findFirstActiveContact(contacts));
  }

  /**
   * Finds the first contact marked as default for document emailing.
   * @param contacts the list of candidate contacts
   * @return an {@link Optional} containing the default contact, or empty if none is marked
   */
  protected static Optional<User> findDefaultForDocs(List<User> contacts) {
    return contacts.stream()
        .filter(c -> Boolean.TRUE.equals(c.get(User.PROPERTY_ISDEFAULTFORDOCS)))
        .findFirst();
  }

  /**
   * Returns the first active contact from the given list, or the first contact regardless
   * of active status as a fallback.
   * @param contacts a non-empty list of contacts
   * @return the first active contact, or the first contact if none is active
   */
  protected static User findFirstActiveContact(List<User> contacts) {
    return contacts.stream()
        .filter(c -> Boolean.TRUE.equals(c.isActive()))
        .findFirst()
        .orElse(contacts.get(0));
  }

  /**
   * Returns all contacts of the given Business Partner that have a non-empty email address.
   * Results are ordered by: default-for-docs descending, active descending, then name ascending.
   * @param bpartnerId the ID of the Business Partner
   * @return a list of {@link User} contacts with email; never {@code null}
   */
  public static List<User> getBPContactsWithEmail(String bpartnerId) {
    OBContext.setAdminMode(true);
    try {
      OBCriteria<User> criteria = OBDal.getInstance().createCriteria(User.class);
      criteria.add(Restrictions.eq(User.PROPERTY_BUSINESSPARTNER + ".id", bpartnerId));
      criteria.add(Restrictions.isNotNull(User.PROPERTY_EMAIL));
      criteria.add(Restrictions.ne(User.PROPERTY_EMAIL, StringUtils.EMPTY));
      criteria.addOrderBy(User.PROPERTY_ISDEFAULTFORDOCS, false);
      criteria.addOrderBy(User.PROPERTY_ACTIVE, false);
      criteria.addOrderBy(User.PROPERTY_NAME, true);
      return criteria.list();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns the last {@link User} contact used by the given sending user for the given BP,
   * or {@code null} if no record exists or the contact is no longer valid.
   * @param sendingUserId the ID of the system user who is sending the email
   * @param bpartnerId the ID of the Business Partner
   * @return the last used {@link User} contact, or {@code null} if not found or invalid
   */
  public static User getLastUsedContact(String sendingUserId, String bpartnerId) {
    OBContext.setAdminMode(true);
    try {
      EmailBpContact emailBpContact = findLastUsedRecord(sendingUserId, bpartnerId);
      if (emailBpContact == null) {
        return null;
      }
      User contact = emailBpContact.getContactAdUser();
      if (contact == null) {
        return null;
      }
      boolean isActive = Boolean.TRUE.equals(contact.isActive());
      boolean hasEmail = StringUtils.isNotBlank(contact.getEmail());
      return isActive && hasEmail ? contact : null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns the last-used record for the given user and Business Partner, or {@code null}.
   * @param sendingUserId the ID of the sending user
   * @param bpartnerId the ID of the Business Partner
   * @return the {@link EmailBpContact} record, or {@code null} if none exists
   */
  public static EmailBpContact findLastUsedRecord(String sendingUserId, String bpartnerId) {
    OBCriteria<EmailBpContact> criteria = OBDal.getInstance()
        .createCriteria(EmailBpContact.class);
    criteria.add(Restrictions.eq(EmailBpContact.PROPERTY_USERCONTACT + ".id", sendingUserId));
    criteria.add(Restrictions.eq(EmailBpContact.PROPERTY_BUSINESSPARTNER + ".id", bpartnerId));
    criteria.setMaxResults(1);
    return (EmailBpContact) criteria.uniqueResult();
  }

  /**
   * Persists the contact that was used as email recipient so it can be preselected next time.
   * Uses an upsert pattern: updates the existing record if one exists for the user and BP pair,
   * otherwise inserts a new one.
   * @param sendingUserId the ID of the system user who sent the email
   * @param bpartnerId the ID of the Business Partner
   * @param contactAdUserId the ID of the {@link User} contact that was used as recipient
   * @throws ServletException if an error occurs during persistence
   */
  public static void saveLastUsedContact(String sendingUserId, String bpartnerId,
      String contactAdUserId) throws ServletException {
    if (StringUtils.isBlank(contactAdUserId)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      EmailBpContact emailBpContact = findLastUsedRecord(sendingUserId, bpartnerId);
      User contactUser = OBDal.getInstance().get(User.class, contactAdUserId);
      if (emailBpContact != null) {
        updateLastUsedRecord(emailBpContact, contactUser);
      } else {
        insertLastUsedRecord(sendingUserId, bpartnerId, contactUser);
      }
      OBDal.getInstance().flush();
      log.debug("Saved last-used contact {} for user {} / BP {}",
          contactAdUserId, sendingUserId, bpartnerId);
    } catch (Exception e) {
      log.error(OBMessageUtils.messageBD(ERROR_SAVING_EMAIL_CONTACT), e);
      throw new ServletException(OBMessageUtils.messageBD(ERROR_SAVING_EMAIL_CONTACT), e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Updates an existing last-used record with the new contact.
   * @param emailBpContact the existing record to update
   * @param contactUser the new contact to store
   */
  public static void updateLastUsedRecord(EmailBpContact emailBpContact, User contactUser) {
    emailBpContact.setContactAdUser(contactUser);
  }

  /**
   * Inserts a new last-used record for the given user and Business Partner.
   * @param sendingUserId the ID of the sending user
   * @param bpartnerId the ID of the Business Partner
   * @param contactUser the contact to store
   */
  protected static void insertLastUsedRecord(String sendingUserId, String bpartnerId,
      User contactUser) {
    User sendingUser = OBDal.getInstance().get(User.class, sendingUserId);
    BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, bpartnerId);

    EmailBpContact emailBpContact = OBProvider.getInstance().get(EmailBpContact.class);
    emailBpContact.setClient(OBContext.getOBContext().getCurrentClient());
    emailBpContact.setOrganization(OBContext.getOBContext().getCurrentOrganization());
    emailBpContact.setUserContact(sendingUser);
    emailBpContact.setBusinessPartner(bp);
    emailBpContact.setContactAdUser(contactUser);
    OBDal.getInstance().save(emailBpContact);
  }

  /**
   * Finds the ID of the contact whose email matches the given address among the BP's contacts.
   * Comparison is case-insensitive and trims whitespace.
   * @param bpartnerId the ID of the Business Partner
   * @param toEmail the email address to match
   * @return the matching contact's ID, or an empty string if not found
   */
  public static String findContactIdByEmail(String bpartnerId, String toEmail) {
    if (StringUtils.isBlank(toEmail)) {
      return StringUtils.EMPTY;
    }
    String trimmedEmail = StringUtils.trim(toEmail);
    return getBPContactsWithEmail(bpartnerId).stream()
        .filter(contact -> StringUtils.equalsIgnoreCase(trimmedEmail, contact.getEmail()))
        .findFirst()
        .map(User::getId)
        .orElse(StringUtils.EMPTY);
  }
}