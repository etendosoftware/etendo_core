package org.openbravo.erpCommon.utility.reporting.printing;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import jakarta.persistence.criteria.Subquery;
import jakarta.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.ParameterValue;
import org.openbravo.client.application.attachment.AttachImplementation;
import org.openbravo.client.application.attachment.AttachImplementationManager;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.DetachedCriteria;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Projections;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.dal.service.Subqueries;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.email.EmailUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.erpCommon.utility.poc.EmailInfo;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.erpCommon.utility.poc.EmailType;
import org.openbravo.erpCommon.utility.reporting.DocumentType;
import org.openbravo.erpCommon.utility.reporting.Report;
import org.openbravo.erpCommon.utility.reporting.ReportManager;
import org.openbravo.erpCommon.utility.reporting.ReportingException;
import org.openbravo.erpCommon.utility.reporting.TemplateInfo;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.Organization;

public class EmailUtilities {
  private static final Logger log4j = LogManager.getLogger();

  private EmailUtilities() {
  }

  public static void prepareAndSendEmail(Report report, VariablesSecureApp vars,
      List<AttachContent> attachedContent, PocData documentData, String senderAddress,
      Map<String, Boolean> checks, ConnectionProvider connectionProvider)
      throws IOException, ServletException {
    final String attachmentFileLocation = report.getTargetLocation();
    String emailSubject = "";
    String emailBody = "";
    final String ourReference = report.getOurReference();
    final String cusReference = report.getCusReference();
    log4j.debug("our document ref: {}", ourReference);
    log4j.debug("cus document ref: {}", cusReference);

    final String toName = documentData.contactName;
    String toEmail;
    final String replyToName = documentData.salesrepName;
    String replyToEmail;

    boolean moreThanOneCustomer = checks.get("moreThanOneCustomer");
    boolean moreThanOnesalesRep = checks.get("moreThanOnesalesRep");
    boolean differentDocTypes = checks.get("differentDocTypes");

    toEmail = getEmailValue(moreThanOneCustomer, documentData.contactEmail, vars.getStringParameter("toEmail"), true,
        "NoSalesRepEmail");
    replyToEmail = getEmailValue(moreThanOnesalesRep, documentData.salesrepEmail,
        vars.getStringParameter("replyToEmail"), true, "NoCustomerEmail");

    if (differentDocTypes) {
      try {
        TemplateInfo.EmailDefinition emailDefinition = report.getDefaultEmailDefinition();
        emailSubject = emailDefinition.getSubject();
        emailBody = emailDefinition.getBody();
      } catch (ReportingException e) {
        log4j.error(e.getMessage(), e);
      }
    } else {
      emailSubject = vars.getStringParameter("emailSubject");
      emailBody = vars.getStringParameter("emailBody");
    }

    // Replace special tags

    emailSubject = emailSubject.replace("@cus_ref@", Matcher.quoteReplacement(cusReference));
    emailSubject = emailSubject.replace("@our_ref@", Matcher.quoteReplacement(ourReference));
    emailSubject = emailSubject.replace("@cus_nam@", Matcher.quoteReplacement(toName));
    emailSubject = emailSubject.replace("@sal_nam@", Matcher.quoteReplacement(replyToName));
    emailSubject = emailSubject.replace("@bp_nam@",
        Matcher.quoteReplacement(report.getBPName()));
    emailSubject = emailSubject.replace("@doc_date@",
        Matcher.quoteReplacement(report.getDocDate()));
    emailSubject = emailSubject.replace("@doc_nextduedate@",
        Matcher.quoteReplacement(report.getMinDueDate()));
    emailSubject = emailSubject.replace("@doc_lastduedate@",
        Matcher.quoteReplacement(report.getMaxDueDate()));
    emailSubject = emailSubject.replace("@doc_desc@",
        Matcher.quoteReplacement(report.getDocDescription()));

    emailBody = emailBody.replace("@cus_ref@", Matcher.quoteReplacement(cusReference));
    emailBody = emailBody.replace("@our_ref@", Matcher.quoteReplacement(ourReference));
    emailBody = emailBody.replace("@cus_nam@", Matcher.quoteReplacement(toName));
    emailBody = emailBody.replace("@sal_nam@", Matcher.quoteReplacement(replyToName));
    emailBody = emailBody.replace("@bp_nam@", Matcher.quoteReplacement(report.getBPName()));
    emailBody = emailBody.replace("@doc_date@", Matcher.quoteReplacement(report.getDocDate()));
    emailBody = emailBody.replace("@doc_nextduedate@",
        Matcher.quoteReplacement(report.getMinDueDate()));
    emailBody = emailBody.replace("@doc_lastduedate@",
        Matcher.quoteReplacement(report.getMaxDueDate()));
    emailBody = emailBody.replace("@doc_desc@",
        Matcher.quoteReplacement(report.getDocDescription()));

    OBContext.setAdminMode(true);
    EmailServerConfiguration mailConfig;
    try {
      mailConfig = OBDal.getInstance()
          .get(EmailServerConfiguration.class, vars.getStringParameter("fromEmailId"));
    } finally {
      OBContext.restorePreviousMode();
    }

    List<File> attachments = new ArrayList<>();
    attachments.add(new File(attachmentFileLocation));

    if (attachedContent != null) {
      for (AttachContent objContent : attachedContent) {
        final File file = prepareFile(objContent);
        attachments.add(file);
      }
    }

    if (Boolean.TRUE.equals(checks.getOrDefault("sendDocumentAttachments", false))) {
      attachments.addAll(getDocumentAttachments(documentData.documentId));
    }

    final EmailInfo email = new EmailInfo.Builder().setRecipientTO(toEmail)
        .setRecipientCC(vars.getStringParameter("ccEmail"))
        .setRecipientBCC(vars.getStringParameter("bccEmail"))
        .setReplyTo(replyToEmail)
        .setSubject(emailSubject)
        .setContent(emailBody)
        .setContentType("text/plain; charset=utf-8")
        .setAttachments(attachments)
        .setSentDate(new Date())
        .build();

    log4j.debug("From: {}", senderAddress);
    log4j.debug("Recipient TO (contact email): {}", email.getRecipientTO());
    log4j.debug("Recipient CC: {}", email.getRecipientCC());
    log4j.debug("Recipient BCC (user email): {}", email.getRecipientBCC());
    log4j.debug("Reply-to (sales rep email): {}", email.getReplyTo());

    sendEmail(mailConfig, email, attachments);

    // Store the email in the database
    saveEmail(connectionProvider, vars.getClient(), vars.getUser(), email, report, documentData.documentId);

  }

  private static Collection<? extends File> getDocumentAttachments(String documentId) throws IOException {
    List<File> files = new ArrayList<>();
    AttachImplementationManager attachmentManager = WeldUtils.getInstanceFromStaticBeanManager(AttachImplementationManager.class);
    final String tempAttachPath = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("attach.path") + File.separator + "tmp";
    try {
      OBContext.setAdminMode();
      DetachedCriteria attachmentMetadataCriteria = DetachedCriteria.forEntityName(ParameterValue.ENTITY_NAME, "metadata");
      attachmentMetadataCriteria.add(Restrictions.eqProperty("metadata." + ParameterValue.PROPERTY_FILE + ".id", "attachment.id"));
      attachmentMetadataCriteria.add(Restrictions.eq(ParameterValue.PROPERTY_VALUESTRING, ReportManager.GENERATED_BY_EMAILING));
      attachmentMetadataCriteria.setProjection(Projections.id());

      OBCriteria<Attachment> attachmentOBCriteria = OBDal.getInstance().createCriteria(Attachment.class, "attachment");
      attachmentOBCriteria.add(Restrictions.eq(Attachment.PROPERTY_RECORD, documentId));
      // Do not include reports generated by emailing, as they will be already included in the email
      attachmentOBCriteria.add(Subqueries.notExists(attachmentMetadataCriteria));
      // do not filter by the attachment's organization
      // if the user has access to the record where the file its attached, it has access to all its
      // attachments
      attachmentOBCriteria.setFilterOnReadableOrganization(false);

      for (Attachment attachment : attachmentOBCriteria.list()) {
        AttachImplementation handler = attachmentManager.getHandler(attachment.getAttachmentConf() == null ? "Default"
            : attachment.getAttachmentConf().getAttachmentMethod().getValue());
        if (handler == null) {
          throw new OBException(OBMessageUtils.messageBD("OBUIAPP_NoMethod"));
        }
        File file = handler.downloadFile(attachment);
        if (file.exists()) {
          // Files will be deleted after being sent, copy them to a temporary file directory
          Path tempFilePath = Files.copy(file.toPath(), Path.of(tempAttachPath, file.getName()));
          files.add(tempFilePath.toFile());
        } else {
          throw new OBException(OBMessageUtils.messageBD("OBUIAPP_NoAttachmentFound"));
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    return files;
  }

  public static void sendEmail(EmailServerConfiguration mailConfig, EmailInfo email,
      List<File> attachments) throws ServletException, IOException {
    try {
      EmailManager.sendEmail(mailConfig, email);
    } catch (Exception exception) {
      log4j.error("error sending mail", exception);
      final String exceptionClass = exception.getClass().toString().replace("class ", "");
      String exceptionString = "Problems while sending the email" + exception;
      exceptionString = exceptionString.replace(exceptionClass, "");
      throw new ServletException(exceptionString);
    } finally {
      // Delete the temporary files generated for the email attachments
      for (File attachment : attachments) {
        if (attachment.exists() && !attachment.isDirectory()) {
          Files.delete(attachment.toPath());
        }
      }
    }
  }


  public static void saveEmail(ConnectionProvider connectionProvider, String clientId, String userId, EmailInfo email,
      Report report, String documentId) throws ServletException {
    Connection conn = null;
    try {
      conn = connectionProvider.getTransactionConnection();

      // First store the email message
      final String newEmailId = SequenceIdData.getUUID();
      if (log4j.isDebugEnabled()) {
        log4j.debug("New email id: {}", newEmailId);
      }

      EmailData.insertEmail(conn, connectionProvider, newEmailId, clientId, report.getOrgId(),
          userId, EmailType.OUTGOING.getStringValue(), email.getReplyTo(),
          email.getRecipientTO(), email.getRecipientCC(), email.getRecipientBCC(),
          Utility.formatDate(new Date(), "yyyyMMddHHmmss"), email.getSubject(), email.getContent(),
          report.getBPartnerId(),
          ToolsData.getTableId(connectionProvider, report.getDocumentType().getTableName()),
          documentId);

      connectionProvider.releaseCommitConnection(conn);
    } catch (final NoConnectionAvailableException exception) {
      log4j.error(exception);
      throw new ServletException(exception);
    } catch (final SQLException exception) {
      log4j.error(exception);
      try {
        connectionProvider.releaseRollbackConnection(conn);
      } catch (final Exception ignored) {
        // Rollback of this connection should not fail
        // If it fails another previous error would have been logged already
      }

      throw new ServletException(exception);
    }
  }

  /**
   * @throws ServletException
   *     when loading the attachment from disk fails
   * @author gmauleon
   */
  private static File prepareFile(AttachContent content) throws ServletException {
    final String attachPath = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("attach.path") + "/tmp";
    final File f = new File(attachPath, content.getFileName());

    try (OutputStream out = new FileOutputStream(f)) {
      final InputStream inputStream = content.getFileItem().getInputStream();
      final byte[] buf = new byte[1024];
      int len;
      while ((len = inputStream.read(buf)) > 0) {
        out.write(buf, 0, len);
      }
      inputStream.close();
      return f;
    } catch (final Exception e) {
      throw new ServletException("Error trying to get the attached file", e);
    }

  }

  public static String getEmailValue(boolean condition, String defaultValue, String alternativeValue,
      boolean failIfEmpty, String failureMessage) throws ServletException {
    String result;
    if (condition) {
      result = defaultValue;
    } else {
      result = alternativeValue;
    }

    if (failIfEmpty && (result == null || result.length() == 0)) {
      throw new ServletException(OBMessageUtils.messageBD(failureMessage));
    }

    return result;
  }

  public static PocData[] getContactDetails(DocumentType documentType, String strDocumentId,
      ConnectionProvider connectionProvider)
      throws ServletException {
    switch (documentType) {
      case QUOTATION:
      case SALESORDER:
      case PURCHASEORDER:
        return PocData.getContactDetailsForOrders(connectionProvider, strDocumentId);
      case SALESINVOICE:
        return PocData.getContactDetailsForInvoices(connectionProvider, strDocumentId);
      case SHIPMENT:
        return PocData.getContactDetailsForShipments(connectionProvider, strDocumentId);
      case PAYMENT:
        return PocData.getContactDetailsForPayments(connectionProvider, strDocumentId);
      case UNKNOWN:
        return new PocData[0];
    }
    return new PocData[0];
  }

  public static EmailServerConfiguration getEmailConfiguration(String organizationId) throws ServletException {
    OBContext.setAdminMode(true);
    try {
      OBCriteria<EmailServerConfiguration> mailConfigCriteria = OBDal.getInstance()
          .createCriteria(EmailServerConfiguration.class);
      mailConfigCriteria.addOrderBy("client.id", false);
      final List<EmailServerConfiguration> mailConfigList = mailConfigCriteria.list();

      if (mailConfigList.isEmpty()) {
        throw new ServletException("No Poc configuration found for this client.");
      }

      EmailServerConfiguration mailConfig = EmailUtils
          .getEmailConfiguration(OBDal.getInstance().get(Organization.class, organizationId));

      if (mailConfig == null) {
        throw new ServletException(
            "No sender defined: Please go to client configuration to complete the email configuration.");
      }

      return mailConfig;
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
