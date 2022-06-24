package org.openbravo.email;

/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Collection of email utilities
 * 
 * @author shankar
 * 
 */

public class EmailUtils {
  static Logger log4j = LogManager.getLogger();

  /*
   * Retrieves the email configuration of the Organization
   * 
   * @param Organization Organization whose email server configuration is to be retrieved.
   * 
   * @return EmailServerConfiguration of the Organization.
   */
  public static EmailServerConfiguration getEmailConfiguration(Organization organization) {
    EmailServerConfiguration emailConfiguration = null;
    try {
      if (organization != null) {
        OBCriteria<EmailServerConfiguration> mailConfigCriteria = OBDal.getInstance()
            .createCriteria(EmailServerConfiguration.class);
        mailConfigCriteria
            .add(Restrictions.eq(EmailServerConfiguration.PROPERTY_ORGANIZATION, organization));
        mailConfigCriteria.add(Restrictions.eq(EmailServerConfiguration.PROPERTY_CLIENT,
            OBContext.getOBContext().getCurrentClient()));

        List<EmailServerConfiguration> mailConfigList = null;
        mailConfigList = mailConfigCriteria.list();
        // A client can define several organization, so uniqueRequlst can not be used
        if (mailConfigList.size() != 0) {
          emailConfiguration = mailConfigList.get(0);
        }

        if (organization.getId().equals("0")) {
          return emailConfiguration;
        } else {
          // if value not available look in parent organization
          if (emailConfiguration == null) {
            OrganizationStructureProvider orgStructure = new OrganizationStructureProvider();
            return getEmailConfiguration(orgStructure.getParentOrg(organization));
          } else {
            return emailConfiguration;
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while retrieving email configuration", e);
    }
    return emailConfiguration;
  }
}
