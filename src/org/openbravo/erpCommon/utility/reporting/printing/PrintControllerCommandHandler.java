package org.openbravo.erpCommon.utility.reporting.printing; //NOSONAR

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.email.ResolvedSmtpConfig;
import org.openbravo.email.SmtpCascadeResolver;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.erpCommon.utility.reporting.DocumentType;
import org.openbravo.erpCommon.utility.reporting.Report;
import org.openbravo.erpCommon.utility.reporting.Report.OutputTypeEnum;
import org.openbravo.erpCommon.utility.reporting.ReportManager;
import org.openbravo.erpCommon.utility.reporting.ReportingException;
import org.openbravo.erpCommon.utility.reporting.TemplateInfo;
import org.openbravo.model.common.enterprise.EmailTemplate;

import net.sf.jasperreports.engine.JasperPrint;

@SuppressWarnings("java:S00120")
final class PrintControllerCommandHandler {
  private static final String SUFFIX_DOCUMENTS = ".Documents";
  private static final String TEMPLATE_DEFAULT = "default";
  private static final String PARAM_TEMPLATES = "templates";
  private final PrintController controller;
  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final VariablesSecureApp vars;
  private final Context context;

  PrintControllerCommandHandler(PrintController controller, HttpServletRequest request,
      HttpServletResponse response, VariablesSecureApp vars, Context context) {
    this.controller = controller;
    this.request = request;
    this.response = response;
    this.vars = vars;
    this.context = context;
  }

  static Context createContext(PrintController controller, VariablesSecureApp vars,
      DocumentType documentType, String sessionValuePrefix, String strDocumentId)
      throws ServletException {
    String localStrDocumentId = PrintControllerDocumentHelper.normalizeDocumentId(strDocumentId);
    String fullDocumentIdentifier = PrintControllerDocumentHelper.sanitizeDocumentIdentifier(strDocumentId)
        + documentType.getTableName();
    HashMap<String, Boolean> checks = new HashMap<>();
    checks.put(PrintController.CHECK_MORE_THAN_ONE_CUSTOMER, Boolean.FALSE);
    checks.put(PrintController.CHECK_MORE_THAN_ONE_SALES_REP, Boolean.FALSE);
    if (controller.isDebugEnabled()) {
      controller.debug("strDocumentId: " + localStrDocumentId);
    }
    if (localStrDocumentId.length() == 0) {
      throw new ServletException(Utility.messageBD(controller, "NoDocument", vars.getLanguage()));
    }

    String[] rawIds = localStrDocumentId.split(",");
    String[] documentIds = new String[rawIds.length];
    for (int i = 0; i < rawIds.length; i++) {
      documentIds[i] = PrintControllerDocumentHelper.sanitizeDocumentIdentifier(rawIds[i].trim());
    }
    if (controller.isDebugEnabled()) {
      controller.debug("Number of documents selected: " + documentIds.length);
    }
    controller.multiReports = documentIds.length > 1;

    @SuppressWarnings("unchecked")
    HashMap<String, Report> reports = (HashMap<String, Report>) vars
        .getSessionObject(sessionValuePrefix + SUFFIX_DOCUMENTS);
    ReportManager reportManager = controller.createReportManager();
    return new Context(documentType, sessionValuePrefix, fullDocumentIdentifier, checks,
        documentIds, reports, reportManager);
  }


  void handle() throws Exception {
    if (vars.commandIn("PRINT")) {
      handlePrintCommand();
      return;
    }
    if (vars.commandIn("ARCHIVE")) {
      handleArchiveCommand();
      return;
    }
    if (vars.commandIn("DEFAULT")) {
      handleDefaultCommand();
      return;
    }
    if (vars.commandIn("ADD")) {
      openOptionsPage();
      return;
    }
    if (vars.commandIn("DEL")) {
      handleDeleteCommand();
      return;
    }
    if (vars.commandIn("EMAIL")) {
      handleEmailCommand();
      return;
    }
    if (vars.commandIn("UPDATE_TEMPLATE")) {
      handleUpdateTemplateCommand();
      return;
    }
    if (vars.commandIn("GET_BP_CONTACTS")) {
      handleGetBpContactsCommand();
      return;
    }
    if (vars.commandIn("UPDATE_EMAILCONFIG")) {
      handleUpdateEmailConfigCommand();
      return;
    }
    controller.showPageError(response);
  }

  private void handlePrintCommand() throws IOException, ServletException, JSONException {
    controller.archivedReports = false;
    String[] documentIds = getOrderedDocumentIds();
    Report report = null;
    JasperPrint jasperPrint = null;
    Collection<JasperPrint> jrPrintReports = new ArrayList<>();
    Collection<Report> savedReports = new ArrayList<>();
    for (String documentId : documentIds) {
      String safeId = PrintControllerDocumentHelper.sanitizeDocumentIdentifier(documentId);
      controller.preparePreProcessHooks(context.documentType, safeId);
      report = controller.buildReport(response, vars, safeId, context.reportManager,
          context.documentType, Report.OutputTypeEnum.PRINT, TEMPLATE_DEFAULT);
      if (report != null) {
        try {
          jasperPrint = context.reportManager.processReport(report, vars);
          jrPrintReports.add(jasperPrint);
        } catch (ReportingException e) {
          controller.showAdvicePopup(request, response, "Report processing failed",
              "Unable to process report selection");
          controller.error(e);
        }
        savedReports.add(report);
        if (controller.multiReports) {
          context.reportManager.saveTempReport(report, vars);
        }
      }
    }
    controller.printReports(response, jrPrintReports, savedReports, PrintControllerPreferenceHelper.isDirectPrint(vars));
  }

  private void handleArchiveCommand() throws IOException, ServletException, JSONException {
    controller.archivedReports = true;
    String[] documentIds = getOrderedDocumentIds();
    Report report = null;
    JasperPrint jasperPrint = null;
    Collection<JasperPrint> jrPrintReports = new ArrayList<>();
    Collection<Report> savedReports = new ArrayList<>();
    for (String documentId : documentIds) {
      String safeId = PrintControllerDocumentHelper.sanitizeDocumentIdentifier(documentId);
      controller.preparePreProcessHooks(context.documentType, safeId);
      report = controller.buildReport(response, vars, safeId, context.reportManager,
          context.documentType, OutputTypeEnum.ARCHIVE, TEMPLATE_DEFAULT);
      controller.buildReport(response, vars, safeId, context.reports, context.reportManager);
      if (report != null) {
        try {
          jasperPrint = context.reportManager.processReport(report, vars);
          jrPrintReports.add(jasperPrint);
        } catch (ReportingException e) {
          controller.error(e);
        }
        context.reportManager.saveTempReport(report, vars);
        savedReports.add(report);
      }
    }
    controller.printReports(response, jrPrintReports, savedReports, PrintControllerPreferenceHelper.isDirectPrint(vars));
  }

  private void handleDefaultCommand() throws Exception {
    controller.differentDocTypes.clear();
    context.reports = new HashMap<>();
    for (String rawId : context.documentIds) {
      String documentId = PrintControllerDocumentHelper.sanitizeDocumentIdentifier(rawId);
      if (controller.isDebugEnabled()) {
        controller.debug("Processing document with id: " + documentId);
      }
      Report report = new Report(context.documentType, documentId, vars.getLanguage(), TEMPLATE_DEFAULT,
          controller.multiReports, OutputTypeEnum.DEFAULT);
      context.reports.put(documentId, report);
      validateSenderConfiguration(report);
      if (!controller.differentDocTypes.containsKey(report.getDocTypeId())) {
        controller.differentDocTypes.put(report.getDocTypeId(), report.getTemplate());
      }
    }

    vars.setSessionObject(context.sessionValuePrefix + SUFFIX_DOCUMENTS, context.reports);
    openOptionsPage();
  }

  private void handleDeleteCommand() throws IOException, ServletException, ReportingException {
    String documentToDelete = vars.getStringParameter("idToDelete");
    @SuppressWarnings("unchecked")
    List<AttachContent> attachments = (List<AttachContent>) request.getSession()
        .getAttribute(PrintController.SESSION_FILES);
    request.getSession().setAttribute(PrintController.SESSION_FILES, attachments);
    if (attachments != null) {
      controller.seekAndDestroy(attachments, documentToDelete);
    }
    String docIdsForPage = PrintControllerDocumentHelper.getCommaSeparatedString(context.documentIds);
    controller.createEmailOptionsPage(request, response, vars, context.documentType,
        docIdsForPage, context.reports, context.checks);
  }

  private void handleEmailCommand() throws IOException, ServletException {
    String toEmailParam = vars.getStringParameter(PrintController.PARAM_TO_EMAIL);
    if (StringUtils.isBlank(toEmailParam)) {
      throw new ServletException(
          Utility.messageBD(controller, "NoCustomerEmail", vars.getLanguage()));
    }
    PocData[] pocData = (PocData[]) vars
        .getSessionObject(PrintController.SESSION_POC_DATA + context.fullDocumentIdentifier);
    int nrOfEmailsSend = 0;
    if (pocData != null) {
      for (PocData documentData : pocData) {
        nrOfEmailsSend += sendEmailForDocument(pocData, documentData);
      }
    }
    request.getSession().removeAttribute(PrintController.SESSION_FILES);
    if (nrOfEmailsSend > 0 && pocData != null && pocData.length > 0) {
      controller.persistLastUsedContact(vars, pocData[0].bpartnerId);
    }
    vars.removeSessionValue(PrintController.SESSION_POC_DATA + context.fullDocumentIdentifier);
    controller.createPrintStatusPage(response, vars, nrOfEmailsSend);
  }

  private int sendEmailForDocument(PocData[] pocData, PocData documentData)
      throws IOException, ServletException {
    PrintControllerEmailSupport.updateEnvironmentInfo(pocData, context.checks);
    String documentId = PrintControllerDocumentHelper.sanitizeDocumentIdentifier(documentData.documentId);
    if (controller.isDebugEnabled()) {
      controller.debug("Processing document with id: " + documentId);
    }

    String templateInUse = controller.differentDocTypes.size() == 1
        ? vars.getRequestGlobalVariable(PARAM_TEMPLATES, PARAM_TEMPLATES)
        : TEMPLATE_DEFAULT;
    Report report = controller.buildReport(response, vars, documentId, context.reportManager,
        context.documentType, OutputTypeEnum.EMAIL, templateInUse);
    if (report == null) {
      throw new ServletException(Utility.messageBD(controller, "NoDataReport", vars.getLanguage())
          + documentId);
    }
    applySelectedTemplate(report);
    if (report.isDraft()) {
      return 0;
    }
    archiveOrSaveEmailReport(report);
    if (documentData.contactEmail != null) {
      documentData.contactEmail = documentData.contactEmail.replace("\r", "").replace("\n", "");
    }
    String senderAddress = StringUtils.defaultString(vars.getStringParameter("fromEmail")).replace("\r", "").replace("\n", "");
    context.checks.put("differentDocTypes", controller.differentDocTypes.size() > 1);
    @SuppressWarnings("unchecked")
    List<AttachContent> attachments = (List<AttachContent>) request.getSession()
        .getAttribute(PrintController.SESSION_FILES);
    EmailUtilities.prepareAndSendEmail(report, vars, attachments, documentData, senderAddress,
        context.checks, controller);
    return 1;
  }

  private void applySelectedTemplate(Report report) throws ServletException {
    if (controller.differentDocTypes.size() != 1) {
      return;
    }
    String templateId = vars.getRequestGlobalVariable(PARAM_TEMPLATES, PARAM_TEMPLATES);
    try {
      TemplateInfo usedTemplateInfo = new TemplateInfo(controller, report.getDocTypeId(),
          report.getOrgId(), vars.getLanguage(), templateId);
      report.setTemplateInfo(usedTemplateInfo);
    } catch (ReportingException e) {
      throw new ServletException("Error trying to get template information", e);
    }
  }

  private void archiveOrSaveEmailReport(Report report) throws IOException, ServletException {
    if (report.isAttached()) {
      if (controller.isDebugEnabled()) {
        controller.debug("Document is not attached.");
      }
      return;
    }
    String tableId = ToolsData.getTableId(controller, report.getDocumentType().getTableName());
    if ("Y".equals(vars.getStringParameter(PrintController.PARAM_INP_ARCHIVE))) {
      try {
        context.reportManager.createAttachmentForReport(controller, report, tableId, vars,
            ReportManager.GENERATED_BY_EMAILING);
      } catch (ReportingException exception) {
        throw new ServletException(exception);
      }
    } else {
      context.reportManager.saveTempReport(report, vars);
    }
  }

  private void handleUpdateTemplateCommand() throws IOException {
    JSONObject json = new JSONObject();
    try {
      PocData[] pocData = (PocData[]) vars
          .getSessionObject(PrintController.SESSION_POC_DATA + context.fullDocumentIdentifier);
      if (pocData == null || pocData.length == 0) {
        throw new IllegalStateException("No document data found in session");
      }
      String templateId = vars.getRequestGlobalVariable(PARAM_TEMPLATES, PARAM_TEMPLATES);
      String documentId = PrintControllerDocumentHelper.sanitizeDocumentIdentifier(pocData[0].documentId);
      Report report = new Report(context.documentType, documentId, vars.getLanguage(), templateId,
          controller.multiReports, OutputTypeEnum.DEFAULT);
      json.put("templateId", templateId);
      json.put("subject", report.getEmailDefinition().getSubject());
      json.put("body", report.getEmailDefinition().getBody());
      if (!controller.multiReports) {
        json.put("filename", report.getFilename());
      }
      context.reports = new HashMap<>();
      context.reports.put(documentId, report);
      vars.setSessionObject(context.sessionValuePrefix + SUFFIX_DOCUMENTS, context.reports);
    } catch (Exception e) {
      controller.error(e);
      json = PrintControllerJsonHelper.buildErrorJson();
    }
    PrintControllerJsonHelper.writeJsonResponse(response, json);
  }

  private void handleGetBpContactsCommand() throws IOException {
    PocData[] pocDataForContacts = (PocData[]) vars
        .getSessionObject(PrintController.SESSION_POC_DATA + context.fullDocumentIdentifier);
    JSONObject result = PrintControllerJsonHelper.buildBPContactsJson(vars, pocDataForContacts);
    PrintControllerJsonHelper.writeJsonResponse(response, result);
  }

  private void handleUpdateEmailConfigCommand() throws IOException {
    JSONObject json = new JSONObject();
    try {
      String currentEmailConfigId = vars.getStringParameter("emailConfigList");
      EmailTemplate emailTemplate = OBDal.getInstance().get(EmailTemplate.class,
          currentEmailConfigId);
      if (emailTemplate != null) {
        json.put("subject", emailTemplate.getSubject());
        json.put("body", emailTemplate.getBody());
      }
    } catch (Exception e) {
      controller.error(e);
      json = PrintControllerJsonHelper.buildErrorJson();
    }
    PrintControllerJsonHelper.writeJsonResponse(response, json);
  }

  private void openOptionsPage() throws IOException, ServletException, ReportingException {
    String docIdsForPage = PrintControllerDocumentHelper.getCommaSeparatedString(context.documentIds);
    if (isPrintPath()) {
      controller.createPrintOptionsPage(request, response, vars, context.documentType,
          docIdsForPage, context.reports);
      return;
    }
    if (isSendPath()) {
      controller.createEmailOptionsPage(request, response, vars, context.documentType,
          docIdsForPage, context.reports, context.checks);
    }
  }

  private void validateSenderConfiguration(Report report) throws IOException, ServletException {
    String senderAddress = EmailData.getSenderAddress(controller, vars.getClient(), report.getOrgId());
    if (isPrintPath() || isPrintOptionsPath()) {
      return;
    }
    ResolvedSmtpConfig resolvedForCheck = SmtpCascadeResolver.resolve();
    boolean hasSender = resolvedForCheck != null || StringUtils.isNotEmpty(senderAddress);
    if (!hasSender) {
      controller.reportNoSenderError(response, vars);
    }
  }

  private String[] getOrderedDocumentIds() throws ServletException {
    return controller.multiReports
        ? PrintControllerDocumentHelper.orderByDocumentNo(controller, context.documentType, context.documentIds)
        : context.documentIds;
  }

  private boolean isPrintPath() {
    return request.getServletPath().toLowerCase().indexOf("print.html") != -1;
  }

  private boolean isPrintOptionsPath() {
    return request.getServletPath().toLowerCase().indexOf("printoptions.html") != -1;
  }

  private boolean isSendPath() {
    return request.getServletPath().toLowerCase().indexOf("send.html") != -1;
  }



  static final class Context {
    final DocumentType documentType;
    final String sessionValuePrefix;
    final String fullDocumentIdentifier;
    final HashMap<String, Boolean> checks;
    final String[] documentIds;
    HashMap<String, Report> reports;
    final ReportManager reportManager;

    Context(DocumentType documentType, String sessionValuePrefix,
        String fullDocumentIdentifier, HashMap<String, Boolean> checks, String[] documentIds,
        HashMap<String, Report> reports, ReportManager reportManager) {
      this.documentType = documentType;
      this.sessionValuePrefix = sessionValuePrefix;
      this.fullDocumentIdentifier = fullDocumentIdentifier;
      this.checks = checks;
      this.documentIds = documentIds;
      this.reports = reports;
      this.reportManager = reportManager;
    }
  }
}
