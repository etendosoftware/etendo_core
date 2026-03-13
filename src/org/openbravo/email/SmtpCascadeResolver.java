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
package org.openbravo.email;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Resolves the SMTP configuration to use when sending emails by applying a strict cascade
 * The first match wins. If the resolved configuration later fails at send time, no silent
 * fallback is performed — the error is propagated so the user can fix the configuration at the
 * appropriate level.
 */
public class SmtpCascadeResolver {

  private static final Logger log = LogManager.getLogger();
  static final String ROOT_ORG_ID = "0";

  private SmtpCascadeResolver() {
  }

  /**
   * Determines the SMTP configuration to use for the current user and organization context.
   * Applies the cascade: User → Organization → Client.
   * @return the resolved SMTP configuration, or {@code null} if no configuration exists at
   *   any level
   */
  public static ResolvedSmtpConfig resolve() {
    OBContext context = OBContext.getOBContext();
    User currentUser = context.getUser();
    Organization currentOrg = context.getCurrentOrganization();
    ResolvedSmtpConfig config = resolveUserLevel(currentUser);
    if (config != null) {
      return config;
    }
    config = resolveOrgOrClientLevel(currentOrg);
    if (config != null) {
      return config;
    }
    return null;
  }

  /**
   * Attempts to resolve the SMTP configuration from the user's personal email settings.
   * Queries for an active {@link EmailServerConfiguration} record linked to the given user
   * via {@code AD_USER_ID}.
   * @param user the current user
   * @return a {@link ResolvedSmtpConfig} at {@code USER} level, or {@code null} if none found
   */
  protected static ResolvedSmtpConfig resolveUserLevel(User user) {
    try {
      EmailServerConfiguration config = findActiveUserEmailConfig(user);
      if (config != null) {
        return new ResolvedSmtpConfig(config, ResolvedSmtpConfig.Level.USER);
      }
    } catch (Exception e) {
      log.error("Error resolving SMTP config at USER level for userId={}",
          user.getId(), e);
    }
    return null;
  }

  /**
   * Queries the database for an active {@link EmailServerConfiguration} record belonging to the
   * given user (where {@code AD_USER_ID} matches). Returns at most one result.
   * @param user the user to search for
   * @return the active user email configuration, or {@code null} if none exists
   */
  protected static EmailServerConfiguration findActiveUserEmailConfig(User user) {
    OBCriteria<EmailServerConfiguration> criteria = OBDal.getInstance()
        .createCriteria(EmailServerConfiguration.class);
    criteria.add(Restrictions.eq(EmailServerConfiguration.PROPERTY_USERCONTACT, user));
    criteria.add(Restrictions.eq(EmailServerConfiguration.PROPERTY_ACTIVE, true));
    // Default configuration first; if tied, most recently created wins
    criteria.addOrder(Order.desc(EmailServerConfiguration.PROPERTY_DEFAULTCONFIGURATION));
    criteria.addOrder(Order.desc(EmailServerConfiguration.PROPERTY_CREATIONDATE));
    criteria.setMaxResults(1);
    List<EmailServerConfiguration> results = criteria.list();
    return results.isEmpty() ? null : results.get(0);
  }

  /**
   * Attempts to resolve the SMTP configuration at the Organization or Client level.
   * Delegates to {@link EmailUtils#getEmailConfiguration(Organization)} which walks up the
   * organization tree and may return a Client-level (org {@code '0'}) configuration.
   * @param org the context organization
   * @return a {@link ResolvedSmtpConfig} at {@code ORGANIZATION} or {@code CLIENT} level,
   *   or {@code null} if no configuration is found
   */
  protected static ResolvedSmtpConfig resolveOrgOrClientLevel(Organization org) {
    EmailServerConfiguration config = EmailUtils.getEmailConfiguration(org);
    if (config == null) {
      return null;
    }
    ResolvedSmtpConfig.Level level = determineLevel(config);
    return new ResolvedSmtpConfig(config, level);
  }

  /**
   * Determines whether the given {@link EmailServerConfiguration} belongs to the Client
   * level (org ID {@code '0'}) or to a specific Organization.
   * @param config the email server configuration to inspect
   * @return {@link ResolvedSmtpConfig.Level#CLIENT} if the org ID is {@code '0'},
   *   otherwise {@link ResolvedSmtpConfig.Level#ORGANIZATION}
   */
  protected static ResolvedSmtpConfig.Level determineLevel(EmailServerConfiguration config) {
    String orgId = config.getOrganization().getId();
    if (StringUtils.equals(orgId, ROOT_ORG_ID)) {
      return ResolvedSmtpConfig.Level.CLIENT;
    }
    return ResolvedSmtpConfig.Level.ORGANIZATION;
  }
}
