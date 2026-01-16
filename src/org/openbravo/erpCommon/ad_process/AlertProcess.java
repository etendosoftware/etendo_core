/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2008-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_process;

import java.net.InetAddress;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.obps.ActivationKey;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.erpCommon.utility.poc.EmailInfo;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.SystemInformation;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.quartz.JobExecutionException;

import jakarta.enterprise.context.Dependent;
import jakarta.servlet.ServletException;

@Dependent
public class AlertProcess implements Process {

  private static final Logger log4j = LogManager.getLogger();

  private static int counter = 0;

  private ConnectionProvider connection;
  private ProcessLogger logger;
  private static final String SYSTEM_CLIENT_ID = "0";
  private static final String CLIENT_ORG_SEPARATOR = "-";
  private static String LANGUAGE = null;

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();
    connection = bundle.getConnection();

    logger.log("Starting Alert Backgrouond Process. Loop " + counter + "\n");

    try {
      AlertProcessData[] alertRule = null;
      final String adClientId = bundle.getContext().getClient();
      LANGUAGE = bundle.getContext().getLanguage();

      if (adClientId.equals(SYSTEM_CLIENT_ID)) {
        // Process all clients
        alertRule = AlertProcessData.selectSQL(connection);
      } else {
        // Filter by Process Request's client
        alertRule = AlertProcessData.selectSQL(connection, adClientId);
      }

      if (alertRule != null && alertRule.length != 0) {

        for (int i = 0; i < alertRule.length; i++) {
          processAlert(alertRule[i], connection);
        }
      }
    } catch (Exception e) {
      throw new JobExecutionException(e.getMessage(), e);
    } finally {
      OBDal.getInstance().commitAndClose();
    }
  }

  private static AlertProcessData[] selectAlert(ConnectionProvider connectionProvider,
      String alertRule, String alertRuleId) throws ServletException {
    String alertRuleSQL = (alertRule == null || alertRule.equals("")) ? "" : alertRule;
    String strSql = "SELECT * FROM (" + alertRuleSQL + ") AAA where not exists ("
        + "select 1 from ad_alert a where a.ad_alertrule_id = ? "
        + "and a.referencekey_id = aaa.referencekey_id and coalesce(a.status, 'NEW') != 'SOLVED')";

    String dateTimeFormat = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("dateTimeFormat.java");

    ResultSet result;
    Vector<AlertProcessData> vector = new Vector<>(0);
    PreparedStatement st = null;

    try {
      connectionProvider.getConnection().setReadOnly(true);
      st = connectionProvider.getPreparedStatement(strSql);
      st.setString(1, alertRuleId);
      result = st.executeQuery();
      while (result.next()) {
        AlertProcessData objectAlertProcessData = new AlertProcessData();
        objectAlertProcessData.adClientId = UtilSql.getValue(result, "ad_client_id");
        objectAlertProcessData.adOrgId = UtilSql.getValue(result, "ad_org_id");
        objectAlertProcessData.created = UtilSql.getDateTimeValue(result, "created",
            dateTimeFormat);
        objectAlertProcessData.createdby = UtilSql.getValue(result, "createdby");
        objectAlertProcessData.updated = UtilSql.getValue(result, "updated");
        objectAlertProcessData.updatedby = UtilSql.getValue(result, "updatedby");
        objectAlertProcessData.recordId = UtilSql.getValue(result, "record_id");
        objectAlertProcessData.referencekeyId = UtilSql.getValue(result, "referencekey_id");
        objectAlertProcessData.description = UtilSql.getValue(result, "description");
        objectAlertProcessData.isactive = UtilSql.getValue(result, "isactive");
        objectAlertProcessData.adUserId = UtilSql.getValue(result, "ad_user_id");
        objectAlertProcessData.adRoleId = UtilSql.getValue(result, "ad_role_id");
        vector.addElement(objectAlertProcessData);
      }
      result.close();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.getConnection().setReadOnly(false);
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception e) {
        log4j.error("Error during release*Statement of query: " + strSql, e);
      }
    }
    AlertProcessData objectAlertProcessData[] = new AlertProcessData[vector.size()];
    vector.copyInto(objectAlertProcessData);
    return (objectAlertProcessData);
  }

  private static int insertAlert(ConnectionProvider connectionProvider, String alertId,
      String clientId, String orgId, String created, String createdBy, String ruleId,
      String recordId, String referenceKey, String description, String user, String role)
      throws ServletException {

    String dateTimeFormat = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("dateTimeFormat.sql");

    // These fields are foreign keys that might be null

    String userStr = user.isEmpty() ? null : user;
    String roleStr = role.isEmpty() ? null : role;
    String ruleIdStr = ruleId.isEmpty() ? null : ruleId;
    String recordIdStr = recordId.isEmpty() ? null : recordId;
    // The date needs to be formated
    String createdStr = "to_timestamp(\'" + created + "\', \'" + dateTimeFormat + "\')";
    // These field needs to be escaped
    String descriptionStr = null;
    if (description != null) {
      descriptionStr = StringUtils.replace(description, "'", "''");
    }

    StringBuilder sqlBuilder = new StringBuilder();
    sqlBuilder.append("INSERT INTO AD_ALERT ");
    sqlBuilder.append("(AD_ALERT_ID, AD_CLIENT_ID, AD_ORG_ID, ");
    sqlBuilder.append("ISACTIVE, CREATED, CREATEDBY, UPDATED, UPDATEDBY, ");
    sqlBuilder.append("AD_ALERTRULE_ID, RECORD_ID, REFERENCEKEY_ID, ");
    sqlBuilder.append("DESCRIPTION, AD_USER_ID, AD_ROLE_ID, STATUS) ");
    sqlBuilder.append("VALUES ");
    sqlBuilder.append("(?, ?, ?, " + "\'Y\', " + createdStr + ", ?, " + "now()" + ", " + "\'0\'"
        + ", ?, ?, ?, ?, ?, ?, " + "\'NEW\')");
    String strSql = sqlBuilder.toString();

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;

    try {
      st = connectionProvider.getPreparedStatement(strSql);

      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, alertId);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, clientId);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, orgId);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, createdBy);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, ruleIdStr);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, recordIdStr);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, referenceKey);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, descriptionStr);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, userStr);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, roleStr);

      updateCount = st.executeUpdate();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception e) {
        log4j.error("Error during release*Statement of query: " + strSql, e);
      }
    }
    return (updateCount);
  }

  private void processAlert(AlertProcessData alertRule, ConnectionProvider conn) throws Exception {
    logger.log("Processing rule " + alertRule.name + "\n");

    AlertProcessData[] alert = null;
    if (!alertRule.sql.equals("")) {
      try {
        if (!alertRule.sql.toUpperCase().trim().startsWith("SELECT ")) {
          logger.log(Utility.messageBD(conn, "AlertSelectConstraint", LANGUAGE) + " \n");
        } else {
          alert = selectAlert(conn, alertRule.sql, alertRule.adAlertruleId);
        }
      } catch (Exception ex) {
        logger.log("Error processing: " + ex.getMessage() + "\n");
        return;
      }
    }
    // Insert
    if (alert != null && alert.length != 0) {
      int insertions = 0;
      HashMap<String, StringBuilder> messageByClientOrg = new HashMap<String, StringBuilder>();
      StringBuilder msg = new StringBuilder();

      for (int i = 0; i < alert.length; i++) {
        String adAlertId = SequenceIdData.getUUID();

        StringBuilder newMsg = new StringBuilder();

        logger.log("Inserting alert " + adAlertId + " org:" + alert[i].adOrgId + " client:"
            + alert[i].adClientId + " reference key: " + alert[i].referencekeyId + " created"
            + alert[i].created + "\n");

        insertAlert(conn, adAlertId, alert[i].adClientId, alert[i].adOrgId, alert[i].created,
            alert[i].createdby, alertRule.adAlertruleId, alert[i].recordId, alert[i].referencekeyId,
            alert[i].description, alert[i].adUserId, alert[i].adRoleId);
        insertions++;

        String messageLine = "\n\nAlert: " + alert[i].description + "\nRecord: "
            + alert[i].recordId +"\nHost: " + InetAddress.getLocalHost().getHostName();
        msg.append(messageLine);
        newMsg.append(messageLine);

        String clientOrg = alert[i].adClientId + CLIENT_ORG_SEPARATOR + alert[i].adOrgId;
        if (messageByClientOrg.containsKey(clientOrg)) {
          messageByClientOrg.get(clientOrg).append(newMsg);
        } else {
          messageByClientOrg.put(clientOrg, newMsg);
        }
      }

      if (insertions > 0) {
        // Send mail

        // There are two ways of sending the email, depending if the SMTP server is configured in
        // the 'Client' tab or in the 'Email Configuration' tab.
        // The SMTP server configured in 'Client' tab way is @Deprecated in 3.0

        final String adClientId = alertRule.adClientId;
        final String adOrgId = alertRule.adOrgId;

        // Since it is a background process and each email sending takes some time (may vary
        // depending on the server), they are sent at the end, once all data is recollected, in
        // order to minimize problems/inconsistencies/NPE if the 'Alerts', 'AlertRecipient',
        // 'User' or 'UserRoles' columns change in the middle of the process.
        final List<EmailInfo> emailsToSendList = new ArrayList<>();
        EmailServerConfiguration mailConfig = null;
        OBContext.setAdminMode();
        try {
          // Getting the SMTP server parameters
          OBCriteria<EmailServerConfiguration> mailConfigCriteria = OBDal.getInstance()
              .createCriteria(EmailServerConfiguration.class);
          mailConfigCriteria.add(Restrictions.eq(EmailServerConfiguration.PROPERTY_CLIENT,
              OBDal.getInstance().get(Client.class, adClientId)));
          mailConfigCriteria.setFilterOnReadableClients(false);
          mailConfigCriteria.setFilterOnReadableOrganization(false);
          final List<EmailServerConfiguration> mailConfigList = mailConfigCriteria.list();

          if (mailConfigList.size() > 0) {
            // TODO: There should be a mechanism to select the desired Email server configuration
            // for alerts, until then, first search for the current organization (and use the
            // first returned one), then for organization '0' (and use the first returned one) and
            // then for any other of the organization tree where current organization belongs to
            // (and use the first returned one).

            for (EmailServerConfiguration currentOrgConfig : mailConfigList) {
              if (adOrgId.equals(currentOrgConfig.getOrganization().getId())) {
                mailConfig = currentOrgConfig;
                break;
              }
            }
            if (mailConfig == null) {
              for (EmailServerConfiguration zeroOrgConfig : mailConfigList) {
                if ("0".equals(zeroOrgConfig.getOrganization().getId())) {
                  mailConfig = zeroOrgConfig;
                  break;
                }
              }
            }
            if (mailConfig == null) {
              mailConfig = mailConfigList.get(0);
            }

            OBCriteria<AlertRecipient> alertRecipientsCriteria = OBDal.getInstance()
                .createCriteria(AlertRecipient.class);
            alertRecipientsCriteria.add(Restrictions.eq(AlertRecipient.PROPERTY_ALERTRULE,
                OBDal.getInstance().get(AlertRule.class, alertRule.adAlertruleId)));
            alertRecipientsCriteria.setFilterOnReadableClients(false);
            alertRecipientsCriteria.setFilterOnReadableOrganization(false);

            final List<AlertRecipient> alertRecipientsList = alertRecipientsCriteria.list();

            // Mechanism to avoid several mails are sent to the same email address for the same
            // alert
            List<String> alreadySentToList = new ArrayList<String>();
            for (AlertRecipient currentAlertRecipient : alertRecipientsList) {
              // If 'Send EMail' option is not checked, we are done for this alert recipient
              if (!currentAlertRecipient.isSendEMail()) {
                continue;
              }

              final List<User> usersList = new ArrayList<>();
              // If there is a 'Contact' established, take it, if not, take all users for the
              // selected 'Role'
              if (currentAlertRecipient.getUserContact() != null) {
                usersList.add(currentAlertRecipient.getUserContact());
              } else {
                OBCriteria<UserRoles> userRolesCriteria = OBDal.getInstance()
                    .createCriteria(UserRoles.class);
                userRolesCriteria.add(Restrictions.eq(AlertRecipient.PROPERTY_ROLE, currentAlertRecipient.getRole()));
                userRolesCriteria.add(Restrictions.eq(AlertRecipient.PROPERTY_CLIENT,
                    currentAlertRecipient.getClient()));
                userRolesCriteria.setFilterOnReadableClients(false);
                userRolesCriteria.setFilterOnReadableOrganization(false);

                final List<UserRoles> userRolesList = userRolesCriteria.list();
                for (UserRoles currenUserRole : userRolesList) {
                  usersList.add(currenUserRole.getUserContact());
                }
              }

              // If there are no 'Contact' for send the email, we are done for this alert
              // recipient
              if (usersList.size() == 0) {
                continue;
              }

              // Create alert's message
              final StringBuilder finalMessage = new StringBuilder();
              for (String currentClientAndOrg : messageByClientOrg.keySet()) {
                String[] clientAndOrg = currentClientAndOrg.split(CLIENT_ORG_SEPARATOR);
                Organization orgEntity = OBDal.getInstance()
                    .get(Organization.class, clientAndOrg[1]);
                if (currentAlertRecipient.getClient().getId().equals(clientAndOrg[0])) {
                  for (RoleOrganization roleOrganization : currentAlertRecipient.getRole()
                      .getADRoleOrganizationList()) {
                    if (OBContext.getOBContext()
                        .getOrganizationStructureProvider()
                        .isInNaturalTree(roleOrganization.getOrganization(), orgEntity)) {
                      finalMessage.append(messageByClientOrg.get(currentClientAndOrg));
                      break;
                    }
                  }
                }
              }

              // For each 'User', get the email parameters (to, subject, body, ...) and store them
              // to send the email at the end
              for (User targetUser : usersList) {
                if (targetUser == null) {
                  continue;
                }
                if (!targetUser.isActive()) {
                  continue;
                }
                final Client targetUserClient = targetUser.getClient();
                final String targetUserClientLanguage = (targetUserClient.getLanguage() != null
                    ? targetUserClient.getLanguage().getLanguage()
                    : null);
                final String targetUserEmail = targetUser.getEmail();
                if (targetUserEmail == null) {
                  continue;
                }

                boolean repeatedEmail = false;
                for (String alreadySentTo : alreadySentToList) {
                  if (targetUserEmail.equals(alreadySentTo)) {
                    repeatedEmail = true;
                    break;
                  }
                }
                if (repeatedEmail) {
                  continue;
                }

                // If there is no message for this user, skip it
                if (finalMessage.length() == 0) {
                  continue;
                }

                alreadySentToList.add(targetUserEmail);
                final String customerName = !ActivationKey.getInstance().isActive() ? "Inactive Instance" : ActivationKey.getInstance().getProperty("customer") ;
                final EmailInfo email = new EmailInfo.Builder().setRecipientTO(targetUserEmail)
                    .setSubject("[OB Alert] " + alertRule.name + "[" + getInstancePurposeName() + "] [" + customerName + "]")
                    .setContent(Utility.messageBD(conn, "AlertMailHead", targetUserClientLanguage) + "\n" + finalMessage)
                    .setContentType("text/plain; charset=utf-8")
                    .setSentDate(new Date())
                    .build();

                emailsToSendList.add(email);
              }
            }
          }
        } catch (Exception e) {
          throw new JobExecutionException(e.getMessage(), e);
        } finally {
          OBContext.restorePreviousMode();
        }
        // Send all the stored emails
        for (EmailInfo emailToSend : emailsToSendList) {
          try {
            EmailManager.sendEmail(mailConfig, emailToSend);
          } catch (Exception exception) {
            log4j.error(exception);
            final String exceptionClass = exception.getClass().toString().replace("class ", "");
            String exceptionString = "Problems while sending the email" + exception;
            exceptionString = exceptionString.replace(exceptionClass, "");
            throw new ServletException(exceptionString);
          }
        }
      }
    }

    // Update
    if (!alertRule.sql.equals("") && (alertRule.sql.toUpperCase().trim().startsWith("SELECT "))) {
      try {
        Integer count = AlertProcessData.updateAlert(conn, alertRule.adAlertruleId, alertRule.sql);
        logger.log("updated alerts: " + count + "\n");

      } catch (Exception ex) {
        logger.log("Error updating: " + ex.toString() + "\n");
      }
    }
  }

  private String getInstancePurposeName() {
    final String PURPOSE_REFERENCE_ID = "60E231391A7348DDA7171E780F62EF99";
    String purpose = OBDal.getInstance().get(SystemInformation.class, "0").getInstancePurpose();
    final Reference reference = OBDal.getInstance().get(Reference.class, PURPOSE_REFERENCE_ID);
    OBCriteria<org.openbravo.model.ad.domain.List> referenceCriteria = OBDal.getInstance().createCriteria(org.openbravo.model.ad.domain.List.class);
    referenceCriteria.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE, reference));
    referenceCriteria.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_SEARCHKEY, purpose));
    referenceCriteria.setMaxResults(1);

    org.openbravo.model.ad.domain.List purposeValue = (org.openbravo.model.ad.domain.List) referenceCriteria.uniqueResult();
    if (purposeValue != null) {
      return purposeValue.getName();
    }
    return purpose;
  }
}
