package org.openbravo.erpCommon.utility.reporting.printing; //NOSONAR

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.email.ResolvedSmtpConfig;
import org.openbravo.email.SmtpCascadeResolver;
import org.openbravo.erpCommon.utility.BasicUtility;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.erpCommon.utility.reporting.DocumentType;
import org.openbravo.erpCommon.utility.reporting.Report;
import org.openbravo.erpCommon.utility.reporting.ReportingException;
import org.openbravo.erpCommon.utility.reporting.TemplateInfo;
import org.openbravo.erpCommon.utility.reporting.TemplateInfo.EmailDefinition;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.xmlEngine.XmlDocument;

@SuppressWarnings("java:S00120")
final class PrintControllerEmailOptionsBuilder {
  private static final String FORM_FROM_EMAIL_ID = "fromEmailId";
  private static final String FORM_FROM_EMAIL = "fromEmail";
  private static final String FORM_CC_EMAIL = "ccEmail";
  private static final String FORM_CC_EMAIL_ORIG = "ccEmailOrig";
  private static final String FORM_BCC_EMAIL = "bccEmail";
  private static final String FORM_BCC_EMAIL_ORIG = "bccEmailOrig";
  private static final String FORM_REPLY_TO_EMAIL = "replyToEmail";
  private static final String FORM_REPLY_TO_EMAIL_ORIG = "replyToEmailOrig";
  private static final String FORM_EMAIL_SUBJECT = "emailSubject";
  private static final String FORM_EMAIL_BODY = "emailBody";
  private final PrintController controller;
  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final VariablesSecureApp vars;
  private final Context context;

  PrintControllerEmailOptionsBuilder(PrintController controller, HttpServletRequest request,
      HttpServletResponse response, VariablesSecureApp vars, Context context) {
    this.controller = controller;
    this.request = request;
    this.response = response;
    this.vars = vars;
    this.context = context;
  }

  void render() throws IOException, ServletException, ReportingException {
    PocData[] pocData = EmailUtilities.getContactDetails(context.documentType, context.strDocumentId, controller);
    List<AttachContent> attachments = getSessionAttachments();
    boolean isTheFirstEntry = attachments == null;
    if (attachments == null) {
      attachments = new ArrayList<>(0);
    }

    XmlDocument xmlDocument = controller.createEmailOptionsXmlDocument(
        PrintControllerEmailSupport.getHiddenTags(pocData, attachments, vars, context.checks,
            controller.differentDocTypes.size()));
    xmlDocument.setParameter("strDocumentId", context.strDocumentId);
    handleAttachmentUpload(pocData, attachments);
    handleClosedPopup(xmlDocument);
    xmlDocument.setParameter(PrintController.XML_PARAMETER_DIRECTORY, controller.getBaseDirectoryJs());
    xmlDocument.setParameter("language", vars.getLanguage());
    xmlDocument.setParameter("theme", vars.getTheme());

    EmailDefinitionResult emailConfig = resolveEmailDefinition(xmlDocument);
    FromAddress fromAddress = resolveFromEmail();
    if (fromAddress == null) {
      return;
    }
    DocumentMapsResult docMaps = buildDocumentMaps(pocData);
    if (!docMaps.allDocsCompleted) {
      reportIncompleteDocuments();
    }

    setupAttachmentParams(xmlDocument, attachments, isTheFirstEntry, pocData, docMaps);
    if (controller.isDebugEnabled()) {
      controller.debug("Documents still in draft: " + docMaps.draftDocumentIds);
    }
    xmlDocument.setParameter("draftDocumentIds", docMaps.draftDocumentIds);

    EmailFormData formData = buildEmailFormData(pocData, docMaps, emailConfig, fromAddress);
    applyEmailFormData(xmlDocument, pocData, emailConfig, formData);

    vars.setSessionObject(PrintController.SESSION_POC_DATA + context.fullDocumentIdentifier, pocData);
    response.setContentType(PrintController.CONTENT_TYPE_HTML);
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  @SuppressWarnings("unchecked")
  private List<AttachContent> getSessionAttachments() {
    return (List<AttachContent>) request.getSession().getAttribute(PrintController.SESSION_FILES);
  }

  private void handleAttachmentUpload(PocData[] pocData, List<AttachContent> attachments) {
    FileItem file = vars.getMultiFile("inpFile");
    if (file == null || file.getName().equals("")) {
      return;
    }
    if (pocData == null || pocData.length == 0) {
      return;
    }
    AttachContent content = new AttachContent();
    content.setFileName(pocData[0].ourreference.replace('/', '_') + '-'
        + Utility.formatDate(new Date(), "yyyyMMdd-HHmmss") + '.' + file.getName());
    content.setFileItem(file);
    content.setId(Utility.formatDate(new Date(), "yyyyMMdd-HHmmss") + '.' + file.getName());
    content.visible = "hidden";
    if ("Y".equals(vars.getStringParameter(PrintController.PARAM_INP_ARCHIVE))) {
      content.setSelected("true");
    }
    attachments.add(content);
    request.getSession().setAttribute(PrintController.SESSION_FILES, attachments);
  }

  private void handleClosedPopup(XmlDocument xmlDocument) {
    if (!"yes".equals(vars.getStringParameter("closed"))) {
      return;
    }
    xmlDocument.setParameter("closed", "yes");
    request.getSession().removeAttribute(PrintController.SESSION_FILES);
  }

  private EmailDefinitionResult resolveEmailDefinition(XmlDocument xmlDocument)
      throws IOException, ServletException {
    EmailDefinitionResult result = new EmailDefinitionResult();
    try {
      if (moreThanOneLanguageDefined() && hasDifferentBpLanguages()) {
        List<EmailDefinition> emailDefinitions = new ArrayList<>();
        for (Map.Entry<String, EmailDefinition> entry : context.reports.values().iterator().next()
            .getEmailDefinitions().entrySet()) {
          emailDefinitions.add(entry.getValue());
        }
        result.emailDefinition = context.reports.values().iterator().next().getTemplateInfo()
            .get_DefaultEmailDefinition();
        xmlDocument.setParameter("reportEmailConfig",
            getOptionsList(emailDefinitions, result.emailDefinition.getId(), false));
        result.hasMultiple = true;
      } else {
        result.emailDefinition = context.reports.values().iterator().next().getEmailDefinition();
      }
    } catch (OBException exception) {
      OBError on = new OBError();
      on.setMessage(Utility.messageBD(controller, "EmailConfiguration", vars.getLanguage()));
      on.setTitle(Utility.messageBD(controller, "Info", vars.getLanguage()));
      on.setType("info");
      StringBuilder tabIdBuilder = new StringBuilder();
      for (char c : vars.getSessionValue(PrintController.INP_TAB_ID).toCharArray()) {
        if (Character.isDigit(c)) {
          tabIdBuilder.append(c);
        }
      }
      vars.setMessage(tabIdBuilder.toString(), on);
      vars.getRequestGlobalVariable(PrintController.INP_TAB_ID, PrintController.ATTRIBUTESETINSTANCE_TABID);
      controller.closePopupAndRefreshParent(response, vars);
    } catch (ReportingException exception) {
      controller.error(exception);
    }
    return result;
  }

  private FromAddress resolveFromEmail() throws IOException, ServletException {
    OBContext.setAdminMode(true);
    try {
      ResolvedSmtpConfig resolvedConfig = SmtpCascadeResolver.resolve();
      if (resolvedConfig == null) {
        controller.reportNoSenderError(response, vars);
        return null;
      }
      return new FromAddress(resolvedConfig.getFromAddress(), resolvedConfig.getConfigId());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private DocumentMapsResult buildDocumentMaps(PocData[] pocData)
      throws IOException, ServletException {
    DocumentMapsResult result = new DocumentMapsResult();
    AttachContent attachedContent = result.attachedContent;
    boolean onlyOneAttachedDoc = PrintControllerEmailSupport.hasSingleAttachmentDoc(context.reports);
    for (PocData documentData : pocData) {
      String customer = documentData.contactEmail;
      PrintControllerEmailSupport.updateEnvironmentInfo(pocData, context.checks);
      if (context.checks.get(PrintController.CHECK_MORE_THAN_ONE_DOCUMENT)) {
        validateCustomer(documentData, customer);
      }
      result.customerMap.putIfAbsent(customer, documentData);

      String salesRep = documentData.salesrepEmail;
      if (context.checks.get(PrintController.CHECK_MORE_THAN_ONE_SALES_REP).booleanValue()) {
        validateSalesRep(documentData, salesRep);
      }
      result.salesRepMap.putIfAbsent(salesRep, documentData);
      updateDocumentState(result, documentData, attachedContent, onlyOneAttachedDoc);
    }
    return result;
  }

  private void validateCustomer(PocData documentData, String customer)
      throws IOException, ServletException {
    if (customer == null || customer.length() == 0) {
      showInfoAndClose("NoContact", "@docNum@", documentData.ourreference);
    } else if (documentData.contactEmail == null || documentData.contactEmail.equals("")) {
      showInfoAndClose("NoEmail", "@customer@", documentData.contactName);
    }
  }

  private void validateSalesRep(PocData documentData, String salesRep)
      throws IOException, ServletException {
    if (salesRep == null || salesRep.length() == 0) {
      showInfoAndClose("NoSenderDocument", null, null);
    } else if (documentData.salesrepEmail == null || documentData.salesrepEmail.equals("")) {
      showInfoAndClose("NoEmailSender", "@salesRep@", documentData.salesrepName);
    }
  }

  private void showInfoAndClose(String messageKey, String token, String value)
      throws IOException, ServletException {
    OBError on = new OBError();
    String message = Utility.messageBD(controller, messageKey, vars.getLanguage());
    if (token != null) {
      message = message.replace(token, value);
    }
    on.setMessage(message);
    on.setTitle(Utility.messageBD(controller, "Info", vars.getLanguage()));
    on.setType("info");
    StringBuilder tabIdBuilder = new StringBuilder();
    for (char c : vars.getSessionValue(PrintController.INP_TAB_ID).toCharArray()) {
      if (Character.isDigit(c)) {
        tabIdBuilder.append(c);
      }
    }
    vars.setMessage(tabIdBuilder.toString(), on);
    vars.getRequestGlobalVariable(PrintController.INP_TAB_ID, PrintController.ATTRIBUTESETINSTANCE_TABID);
    controller.closePopupAndRefreshParent(response, vars);
  }

  private void updateDocumentState(DocumentMapsResult result, PocData documentData,
      AttachContent attachedContent, boolean onlyOneAttachedDoc) throws ServletException {
    Report report = context.reports.get(documentData.documentId);
    if (report.isDraft()) {
      if (result.draftDocumentIds.length() > 0) {
        result.draftDocumentIds += ",";
      }
      result.draftDocumentIds += report.getDocumentId();
      result.allDocsCompleted = false;
    }

    String reportFilename = report.getContextSubFolder() + report.getFilename();
    documentData.reportLocation = request.getContextPath() + "/" + reportFilename
        + "?documentId=" + documentData.documentId;
    if (controller.isDebugEnabled()) {
      controller.debug(" Filling report location with: " + documentData.reportLocation);
    }
    if (onlyOneAttachedDoc
        && !StringUtils.equals(attachedContent.getDocName(), report.getFilename())) {
      attachedContent.setDocName(report.getFilename());
      attachedContent.setVisible("checkbox");
      result.clonedAttachments.add(attachedContent);
    }
  }

  private void reportIncompleteDocuments() throws IOException, ServletException {
    OBError on = new OBError();
    on.setMessage(Utility.messageBD(controller, "ErrorIncompleteDocuments", vars.getLanguage()));
    on.setTitle(Utility.messageBD(controller, "ErrorSendingEmail", vars.getLanguage()));
    on.setType(PrintController.MSG_TYPE_ERROR);
    StringBuilder tabIdBuilder = new StringBuilder();
    for (char c : vars.getSessionValue(PrintController.INP_TAB_ID).toCharArray()) {
      if (Character.isDigit(c)) {
        tabIdBuilder.append(c);
      }
    }
    vars.setMessage(tabIdBuilder.toString(), on);
    vars.getRequestGlobalVariable(PrintController.INP_TAB_ID, PrintController.ATTRIBUTESETINSTANCE_TABID);
    controller.closePopupAndRefreshParent(response, vars);
  }

  private void setupAttachmentParams(XmlDocument xmlDocument, List<AttachContent> attachments,
      boolean isTheFirstEntry, PocData[] pocData, DocumentMapsResult docMaps) {
    boolean onlyOneAttachedDoc = PrintControllerEmailSupport.hasSingleAttachmentDoc(context.reports);
    int numberOfCustomers = docMaps.customerMap.size();
    if (!onlyOneAttachedDoc && isTheFirstEntry) {
      if (numberOfCustomers > 1) {
        docMaps.attachedContent.setDocName(
            context.reports.size() + " Documents to " + numberOfCustomers + " Customers");
      } else {
        docMaps.attachedContent.setDocName(context.reports.size() + " Documents");
      }
      docMaps.attachedContent.setVisible("checkbox");
      docMaps.clonedAttachments.add(docMaps.attachedContent);
    }
    if (!docMaps.clonedAttachments.isEmpty()) {
      xmlDocument.setData("structure2",
          docMaps.clonedAttachments.toArray(new AttachContent[docMaps.clonedAttachments.size()]));
      xmlDocument.setData("structure1",
          attachments.toArray(new AttachContent[attachments.size()]));
    }
    if (pocData.length >= 1) {
      xmlDocument.setData("reportEmail", "liststructure",
          context.reports.get(pocData[0].documentId).getTemplate());
    }
  }

  private EmailFormData buildEmailFormData(PocData[] pocData, DocumentMapsResult docMaps,
      EmailDefinitionResult emailConfig, FromAddress fromAddress) throws ServletException {
    EmailFormData formData = new EmailFormData();
    PocData[] currentUserInfo = PocData.getContactDetailsForUser(controller, vars.getUser());
    if (StringUtils.isNotBlank(currentUserInfo[0].userEmail)) {
      formData.bccEmail = currentUserInfo[0].userEmail;
      formData.bccName = currentUserInfo[0].userName;
    }
    formData.selectedContact = resolvePreselectedContact(pocData);
    formData.fromEmail = fromAddress.fromEmail;
    formData.fromEmailId = fromAddress.fromEmailId;
    formData.numberOfCustomers = docMaps.customerMap.size();
    formData.numberOfSalesReps = docMaps.salesRepMap.size();
    if (!vars.commandIn("ADD", "DEL")) {
      fillInitialRecipientData(formData, docMaps.customerMap, pocData, emailConfig.emailDefinition);
    }
    return formData;
  }

  private User resolvePreselectedContact(PocData[] pocData) {
    if (vars.commandIn("ADD", "DEL") || pocData.length == 0) {
      return null;
    }
    try {
      return BPContactEmailSelector.selectBestContact(pocData[0].bpartnerId, vars.getUser());
    } catch (Exception exception) {
      controller.warn(
          "Could not determine best email contact, falling back to default. Reason: {}",
          exception.getMessage());
      return null;
    }
  }

  private void fillInitialRecipientData(EmailFormData formData, Map<String, PocData> customerMap,
      PocData[] pocData, EmailDefinition emailDefinition) {
    if (formData.numberOfCustomers > 1) {
      List<String> emails = new ArrayList<>();
      for (String email : customerMap.keySet()) {
        if (StringUtils.isNotBlank(email)) {
          emails.add(email);
        }
      }
      formData.toEmail = String.join("; ", emails);
      formData.toContactId = "";
    } else {
      formData.toEmail = getContactField(formData.selectedContact, User::getEmail);
      formData.toContactId = getContactField(formData.selectedContact, User::getId);
    }
    formData.replyToEmail = pocData.length > 0 ? pocData[0].salesrepEmail : "";
    if (emailDefinition != null) {
      formData.emailSubject = emailDefinition.getSubject();
      formData.emailBody = emailDefinition.getBody();
    }
  }

  private void applyEmailFormData(XmlDocument xmlDocument, PocData[] pocData,
      EmailDefinitionResult emailConfig, EmailFormData formData) {
    if (vars.commandIn("ADD", "DEL")) {
      applyEditedEmailParams(xmlDocument);
    } else {
      applyInitialEmailParams(xmlDocument, formData);
    }

    xmlDocument.setParameter("bpartnerId",
        pocData.length > 0 ? pocData[0].bpartnerId : StringUtils.EMPTY);
    xmlDocument.setParameter(PrintController.PARAM_INP_ARCHIVE,
        vars.getStringParameter(PrintController.PARAM_INP_ARCHIVE));
    xmlDocument.setParameter("fromName", "");
    xmlDocument.setParameter("toName", resolveToName(formData.selectedContact, pocData,
        formData.numberOfCustomers));
    xmlDocument.setParameter("ccName", "");
    xmlDocument.setParameter("bccName", formData.bccName);
    xmlDocument.setParameter("replyToName", pocData[0].salesrepName);
    xmlDocument.setParameter("multCusCount", String.valueOf(formData.numberOfCustomers));
    xmlDocument.setParameter("multSalesRepCount", String.valueOf(formData.numberOfSalesReps));
    if (!emailConfig.hasMultiple) {
      xmlDocument.setParameter("useDefault", "Y");
    }
    if (controller.differentDocTypes.size() > 1) {
      xmlDocument.setParameter("multiDocType", "Y");
    }
  }

  private void applyEditedEmailParams(XmlDocument xmlDocument) {
    xmlDocument.setParameter(FORM_FROM_EMAIL_ID, vars.getStringParameter(FORM_FROM_EMAIL_ID));
    xmlDocument.setParameter(FORM_FROM_EMAIL, vars.getStringParameter(FORM_FROM_EMAIL));
    xmlDocument.setParameter(PrintController.PARAM_TO_EMAIL,
        vars.getStringParameter(PrintController.PARAM_TO_EMAIL));
    xmlDocument.setParameter(PrintController.PARAM_TO_EMAIL_ORIG,
        vars.getStringParameter(PrintController.PARAM_TO_EMAIL_ORIG));
    xmlDocument.setParameter(PrintController.PARAM_TO_CONTACT_ID,
        vars.getStringParameter(PrintController.PARAM_TO_CONTACT_ID));
    xmlDocument.setParameter(FORM_CC_EMAIL, vars.getStringParameter(FORM_CC_EMAIL));
    xmlDocument.setParameter(FORM_CC_EMAIL_ORIG, vars.getStringParameter(FORM_CC_EMAIL_ORIG));
    xmlDocument.setParameter(FORM_BCC_EMAIL, vars.getStringParameter(FORM_BCC_EMAIL));
    xmlDocument.setParameter(FORM_BCC_EMAIL_ORIG, vars.getStringParameter(FORM_BCC_EMAIL_ORIG));
    xmlDocument.setParameter(FORM_REPLY_TO_EMAIL, vars.getStringParameter(FORM_REPLY_TO_EMAIL));
    xmlDocument.setParameter(FORM_REPLY_TO_EMAIL_ORIG, vars.getStringParameter(FORM_REPLY_TO_EMAIL_ORIG));
    xmlDocument.setParameter(FORM_EMAIL_SUBJECT, vars.getStringParameter(FORM_EMAIL_SUBJECT));
    xmlDocument.setParameter(FORM_EMAIL_BODY, vars.getStringParameter(FORM_EMAIL_BODY));
  }

  private void applyInitialEmailParams(XmlDocument xmlDocument, EmailFormData formData) {
    xmlDocument.setParameter(FORM_FROM_EMAIL_ID, formData.fromEmailId);
    xmlDocument.setParameter(FORM_FROM_EMAIL, formData.fromEmail);
    xmlDocument.setParameter(PrintController.PARAM_TO_EMAIL, formData.toEmail);
    xmlDocument.setParameter(PrintController.PARAM_TO_EMAIL_ORIG, formData.toEmail);
    xmlDocument.setParameter(PrintController.PARAM_TO_CONTACT_ID, formData.toContactId);
    xmlDocument.setParameter(FORM_CC_EMAIL, "");
    xmlDocument.setParameter(FORM_CC_EMAIL_ORIG, "");
    xmlDocument.setParameter(FORM_BCC_EMAIL, formData.bccEmail);
    xmlDocument.setParameter(FORM_BCC_EMAIL_ORIG, formData.bccEmail);
    xmlDocument.setParameter(FORM_REPLY_TO_EMAIL, formData.replyToEmail);
    xmlDocument.setParameter(FORM_REPLY_TO_EMAIL_ORIG, formData.replyToEmail);
    xmlDocument.setParameter(FORM_EMAIL_SUBJECT, formData.emailSubject);
    xmlDocument.setParameter(FORM_EMAIL_BODY, formData.emailBody);
  }

  private String resolveToName(User selectedContact, PocData[] pocData, int numberOfCustomers) {
    if (numberOfCustomers > 1) {
      return StringUtils.EMPTY;
    }
    if (selectedContact != null) {
      return getContactField(selectedContact, User::getName);
    }
    if (pocData.length > 0) {
      return pocData[0].contactName;
    }
    return StringUtils.EMPTY;
  }

  private static String getContactField(User contact, FieldExtractor getter) {
    if (contact == null) {
      return StringUtils.EMPTY;
    }
    return StringUtils.defaultString(getter.extract(contact));
  }

  private String getOptionsList(List<EmailDefinition> emailDefinitions, String selectedValue,
      boolean isMandatory) {
    StringBuilder strOptions = new StringBuilder();
    if (!isMandatory) {
      strOptions.append("<option value=\"\"></option>");
    }
    for (EmailDefinition definition : emailDefinitions) {
      strOptions.append("<option value=\"").append(definition.getId()).append("\"");
      if (definition.getId().equals(selectedValue)) {
        strOptions.append(" selected=\"selected\"");
      }
      strOptions.append(">");
      strOptions.append(BasicUtility.formatMessageBDToHtml(
          definition.getSubject() + " - " + definition.getLanguage()));
      strOptions.append("</option>");
    }
    return strOptions.toString();
  }

  private boolean moreThanOneLanguageDefined() throws ReportingException {
    for (Report report : context.reports.values()) {
      if (report.getEmailDefinitions().size() > 1) {
        return true;
      }
    }
    return false;
  }

  private boolean hasDifferentBpLanguages() throws ReportingException {
    Language currentLanguage = null;
    for (Report report : context.reports.values()) {
      BusinessPartner businessPartner = OBDal.getInstance()
          .get(BusinessPartner.class, report.getBPartnerId());
      Language language = businessPartner.getLanguage();
      if (currentLanguage == null) {
        currentLanguage = language;
      } else if (!currentLanguage.getId().equals(language.getId())) {
        return true;
      }
    }
    return false;
  }

  /** Extracts a string field from a {@link User}. */
  interface FieldExtractor {
    /**
     * Returns the relevant string field value from the given user.
     *
     * @param user the user from which to extract the field value
     * @return the extracted string field value
     */
    String extract(User user);
  }

  static final class Context {
    final DocumentType documentType;
    final String strDocumentId;
    final Map<String, Report> reports;
    final HashMap<String, Boolean> checks;
    final String fullDocumentIdentifier;

    Context(DocumentType documentType, String strDocumentId, Map<String, Report> reports,
        HashMap<String, Boolean> checks, String fullDocumentIdentifier) {
      this.documentType = documentType;
      this.strDocumentId = strDocumentId;
      this.reports = reports;
      this.checks = checks;
      this.fullDocumentIdentifier = fullDocumentIdentifier;
    }
  }

  private static final class FromAddress {
    private final String fromEmail;
    private final String fromEmailId;

    private FromAddress(String fromEmail, String fromEmailId) {
      this.fromEmail = fromEmail;
      this.fromEmailId = fromEmailId;
    }
  }

  private static final class EmailDefinitionResult {
    private EmailDefinition emailDefinition;
    private boolean hasMultiple;
  }

  private static final class DocumentMapsResult {
    private final Map<String, PocData> customerMap = new HashMap<>();
    private final Map<String, PocData> salesRepMap = new HashMap<>();
    private final List<AttachContent> clonedAttachments = new ArrayList<>();
    private final AttachContent attachedContent = new AttachContent();
    private String draftDocumentIds = "";
    private boolean allDocsCompleted = true;
  }

  private static final class EmailFormData {
    private User selectedContact;
    private String fromEmail = "";
    private String fromEmailId = "";
    private String toEmail = "";
    private String toContactId = "";
    private String bccEmail = "";
    private String bccName = "";
    private String replyToEmail = "";
    private String emailSubject = "";
    private String emailBody = "";
    private int numberOfCustomers;
    private int numberOfSalesReps;
  }
}
