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
 * All portions are Copyright (C) 2013-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.email;

import java.util.Comparator;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.poc.EmailInfo;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.Organization;

/**
 * This singleton class, is in charge of generating events to send emails.
 * 
 * @author asier
 * @see EmailEventContentGenerator
 * 
 */
@ApplicationScoped
public class EmailEventManager {

  private static final Logger log = LogManager.getLogger();

  @Inject
  @Any
  private Instance<EmailEventContentGenerator> emailGenerators;

  /**
   * This method is invoked when an event for sending emails is generated. It looks for all
   * {@link EmailEventContentGenerator} classes listening to this event and generates an email using
   * them.
   * 
   * @param event
   *          Name of the event to send emails for
   * @param recipient
   *          Email address of the email's recipient
   * @param data
   *          Object that the EmailEventContentGenerator will receive to generate the email
   * @return <code>true</code> in case at least one email has been sent
   * @throws EmailEventException
   *           is thrown in case of problems sending the email or getting the email server
   *           configuration
   * 
   * @see EmailEventContentGenerator
   */
  public boolean sendEmail(String event, final String recipient, Object data)
      throws EmailEventException {
    // Retrieves the Email Server configuration
    Organization currenctOrg = OBContext.getOBContext().getCurrentOrganization();
    final EmailServerConfiguration mailConfig = EmailUtils.getEmailConfiguration(currenctOrg);

    if (mailConfig == null) {
      log.warn("Couldn't find email configuarion");
      throw new EmailEventException(
          OBMessageUtils.getI18NMessage("EmailConfigurationNotFound", null));
    }

    try {
      boolean sent = false;
      for (EmailEventContentGenerator gen : getValidEmailGenerators(event, data)) {
        sent = true;
        log.debug("sending email for event " + event + " with generator " + gen);

        final EmailInfo email = new EmailInfo.Builder() //
            .setSubject(gen.getSubject(data, event)) //
            .setRecipientTO(recipient) //
            .setContent(gen.getBody(data, event)) //
            .setContentType(gen.getContentType()) //
            .setAttachments(gen.getAttachments(data, event)) //
            .build();

        if (gen.isAsynchronous()) {
          new Thread(() -> {
            try {
              EmailManager.sendEmail(mailConfig, email);
            } catch (Exception e) {
              log.error("Failed to send email for event {} with generator {}.", event, gen, e);
            }
          }).start();
        } else {
          EmailManager.sendEmail(mailConfig, email);
        }

        if (gen.preventsOthersExecution()) {
          // prevent following execution, stop chain
          break;
        }
      }
      if (!sent) {
        log.warn("No email generator found for event " + event);
      }
      return sent;
    } catch (Exception e) {
      log.error("Failed to send email for event {}.", event, e);
      throw new EmailEventException(e);
    }
  }

  private List<EmailEventContentGenerator> getValidEmailGenerators(String event, Object data) {
    return StreamSupport
        .stream(Spliterators.spliteratorUnknownSize(emailGenerators.iterator(), 0), false)
        .filter(gen -> gen.isValidEvent(event, data))
        .sorted(Comparator.comparing(EmailEventContentGenerator::getPriority))
        .collect(Collectors.toList());
  }
}
