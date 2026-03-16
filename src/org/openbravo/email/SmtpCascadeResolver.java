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
import org.openbravo.dal.security.OrganizationStructureProvider;
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

  private SmtpCascadeResolver() {
  }

  /**
   * Determines the SMTP configuration to use for the current user and organization context.
   * Applies the cascade: User → Organization → Client.
   * A configuration is only used if it has both SMTP host and From Address set.
   * Incomplete configurations are skipped with a warning and the next level is tried.
   * @return the resolved SMTP configuration, or {@code null} if no usable configuration
   *   exists at any level
   */
  public static ResolvedSmtpConfig resolve() {
    OBContext context = OBContext.getOBContext();
    User currentUser = context.getUser();
    Organization currentOrg = context.getCurrentOrganization();
    ResolvedSmtpConfig config = resolveUserLevel(currentUser);
    if (isUsableConfig(config)) {
      return config;
    }
    config = resolveOrgOrClientLevel(currentOrg);
    if (isUsableConfig(config)) {
      return config;
    }
    return null;
  }

  /**
   * Returns {@code true} if the given configuration is non-null and has both SMTP host
   * and From Address set. Configurations missing either field are considered incomplete
   * and are skipped during cascade resolution.
   * @param config the resolved configuration to check
   * @return {@code true} if usable, {@code false} otherwise
   */
  static boolean isUsableConfig(ResolvedSmtpConfig config) {
    return config != null
        && StringUtils.isNotBlank(config.getHost())
        && StringUtils.isNotBlank(config.getFromAddress());
  }

  /**
   * Attempts to resolve the SMTP configuration from the user's personal email settings.
   * Iterates all active configs for the user (default first, then most recent) and returns
   * the first usable one (host + fromAddress both set).
   * @param user the current user
   * @return a {@link ResolvedSmtpConfig} at {@code USER} level, or {@code null} if none found
   */
  protected static ResolvedSmtpConfig resolveUserLevel(User user) {
    try {
      for (EmailServerConfiguration config : findActiveUserEmailConfigs(user)) {
        ResolvedSmtpConfig resolved = new ResolvedSmtpConfig(config, ResolvedSmtpConfig.Level.USER);
        if (isUsableConfig(resolved)) {
          return resolved;
        }
        log.warn("USER SMTP config (id={}) is incomplete (missing host or fromAddress) — trying next",
            config.getId());
      }
    } catch (Exception e) {
      log.error("Error resolving SMTP config at USER level for userId={}",
          user.getId(), e);
    }
    return null;
  }

  /**
   * Queries all active {@link EmailServerConfiguration} records for the given user,
   * ordered by default flag descending then creation date descending.
   * @param user the user to search for
   * @return list of matching configs, may be empty
   */
  protected static List<EmailServerConfiguration> findActiveUserEmailConfigs(User user) {
    OBCriteria<EmailServerConfiguration> criteria = OBDal.getInstance()
        .createCriteria(EmailServerConfiguration.class);
    criteria.add(Restrictions.eq(EmailServerConfiguration.PROPERTY_USERCONTACT, user));
    criteria.add(Restrictions.eq(EmailServerConfiguration.PROPERTY_ACTIVE, true));
    criteria.addOrder(Order.desc(EmailServerConfiguration.PROPERTY_DEFAULTCONFIGURATION));
    criteria.addOrder(Order.desc(EmailServerConfiguration.PROPERTY_CREATIONDATE));
    return criteria.list();
  }

  /**
   * @deprecated Use {@link #findActiveUserEmailConfigs(User)} instead.
   */
  @Deprecated
  protected static EmailServerConfiguration findActiveUserEmailConfig(User user) {
    List<EmailServerConfiguration> results = findActiveUserEmailConfigs(user);
    return results.isEmpty() ? null : results.get(0);
  }

  /**
   * Walks up the organization tree looking for the first usable SMTP configuration,
   * skipping incomplete configs (missing host or fromAddress) at each level.
   * If no org-level config is usable, falls through to the client-level config.
   * @param org the context organization
   * @return a {@link ResolvedSmtpConfig} at {@code ORGANIZATION} or {@code CLIENT} level,
   *   or {@code null} if no usable configuration is found at any level
   */
  protected static ResolvedSmtpConfig resolveOrgOrClientLevel(Organization org) {
    if (org == null) {
      return null;
    }
    boolean isRootOrg = "0".equals(org.getId());
    List<EmailServerConfiguration> configs = isRootOrg
        ? findClientLevelConfigs()
        : findOrgLevelConfigs(org);

    ResolvedSmtpConfig.Level level = isRootOrg
        ? ResolvedSmtpConfig.Level.CLIENT
        : ResolvedSmtpConfig.Level.ORGANIZATION;

    for (EmailServerConfiguration config : configs) {
      ResolvedSmtpConfig resolved = new ResolvedSmtpConfig(config, level);
      if (isUsableConfig(resolved)) {
        return resolved;
      }
      log.warn("{} SMTP config (id={}) is incomplete (missing host or fromAddress) — trying next",
          level, config.getId());
    }

    if (isRootOrg) {
      return null;
    }
    OrganizationStructureProvider orgStructure = new OrganizationStructureProvider();
    return resolveOrgOrClientLevel(orgStructure.getParentOrg(org));
  }

  /**
   * Queries all configs explicitly linked to the given organization (not client-level),
   * ordered by default flag descending then creation date descending.
   */
  private static List<EmailServerConfiguration> findOrgLevelConfigs(Organization org) {
    OBCriteria<EmailServerConfiguration> criteria = OBDal.getInstance()
        .createCriteria(EmailServerConfiguration.class);
    criteria.add(Restrictions.eq(EmailServerConfiguration.PROPERTY_EMAILCONFIGORGANIZATION, org));
    criteria.add(Restrictions.eq(EmailServerConfiguration.PROPERTY_CLIENT,
        OBContext.getOBContext().getCurrentClient()));
    criteria.add(Restrictions.isNull(EmailServerConfiguration.PROPERTY_USERCONTACT));
    criteria.addOrder(Order.desc(EmailServerConfiguration.PROPERTY_DEFAULTCONFIGURATION));
    criteria.addOrder(Order.desc(EmailServerConfiguration.PROPERTY_CREATIONDATE));
    return criteria.list();
  }

  /**
   * Queries all client-level configs (no org link, no user link),
   * ordered by default flag descending then creation date descending.
   */
  private static List<EmailServerConfiguration> findClientLevelConfigs() {
    OBCriteria<EmailServerConfiguration> criteria = OBDal.getInstance()
        .createCriteria(EmailServerConfiguration.class);
    criteria.add(Restrictions.eq(EmailServerConfiguration.PROPERTY_CLIENT,
        OBContext.getOBContext().getCurrentClient()));
    criteria.add(Restrictions.isNull(EmailServerConfiguration.PROPERTY_EMAILCONFIGORGANIZATION));
    criteria.add(Restrictions.isNull(EmailServerConfiguration.PROPERTY_USERCONTACT));
    criteria.addOrder(Order.desc(EmailServerConfiguration.PROPERTY_DEFAULTCONFIGURATION));
    criteria.addOrder(Order.desc(EmailServerConfiguration.PROPERTY_CREATIONDATE));
    return criteria.list();
  }

  /**
   * Determines whether the given {@link EmailServerConfiguration} belongs to the Client
   * level or to a specific Organization, based on {@code EMAIL_CONF_AD_ORG_ID}.
   * @param config the email server configuration to inspect
   * @return {@link ResolvedSmtpConfig.Level#CLIENT} if {@code emailConfOrg} is {@code null}
   *   (no explicit org link), otherwise {@link ResolvedSmtpConfig.Level#ORGANIZATION}
   */
  protected static ResolvedSmtpConfig.Level determineLevel(EmailServerConfiguration config) {
    if (config.getEmailConfigOrganization() == null) {
      return ResolvedSmtpConfig.Level.CLIENT;
    }
    return ResolvedSmtpConfig.Level.ORGANIZATION;
  }
}
