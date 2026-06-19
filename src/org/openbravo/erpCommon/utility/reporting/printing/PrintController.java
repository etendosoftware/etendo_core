/*
 * The contents of this file are subject to the Openbravo Public License Version
 * 1.0 (the "License"), being the Mozilla Public License Version 1.1 with a
 * permitted attribution clause; you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.openbravo.com/legal/license.html Software distributed under the
 * License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing rights and limitations under the License. The Original Code is
 * Openbravo ERP. The Initial Developer of the Original Code is Openbravo SLU All
 * portions are Copyright (C) 2008-2021 Openbravo SLU All Rights Reserved.
 * Contributor(s): ______________________________________.
 */
package org.openbravo.erpCommon.utility.reporting.printing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.common.hooks.PrintControllerHookManager;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.email.EmailUtils;
import org.openbravo.email.ResolvedSmtpConfig;
import org.openbravo.email.SmtpCascadeResolver;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.BasicUtility;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.erpCommon.utility.reporting.DocumentType;
import org.openbravo.erpCommon.utility.reporting.Report;
import org.openbravo.erpCommon.utility.reporting.Report.OutputTypeEnum;
import org.openbravo.erpCommon.utility.reporting.ReportManager;
import org.openbravo.erpCommon.utility.reporting.ReportingException;
import org.openbravo.erpCommon.utility.reporting.TemplateData;
import org.openbravo.erpCommon.utility.reporting.TemplateInfo;
import org.openbravo.erpCommon.utility.reporting.TemplateInfo.EmailDefinition;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.EmailTemplate;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.xmlEngine.XmlDocument;

import com.google.common.collect.HashBasedTable;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;

@SuppressWarnings("serial")
public class PrintController extends HttpSecureAppServlet {
  public static final String ERROR_PRINTING_DOCUMENT_KEY = "Error_Printing_Document";
  static final String PARAM_TO_EMAIL = "toEmail";
  static final String PARAM_TO_EMAIL_ORIG = "toEmailOrig";
  static final String PARAM_TO_CONTACT_ID = "toContactId";
  static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
  public static final String LIST_ITEM_TAG = "<li>";
  public static final String CLOSE_LIST_ITEM_TAG = "</li>";
  final Map<String, TemplateData[]> differentDocTypes = new HashMap<>();
  boolean multiReports = false;
  boolean archivedReports = false;
  final MutableBoolean printingErrorOccurred = new MutableBoolean(false);
  static final String MSG_TYPE_ERROR = "Error";
  static final String TAB = "tab";
  static final String INP_TAB_ID = "inpTabId";
  static final String ATTRIBUTESETINSTANCE_TABID = "AttributeSetInstance.tabId";
  static final String SESSION_POC_DATA = "pocData";
  static final String SESSION_FILES = "files";
  static final String CHECK_MORE_THAN_ONE_CUSTOMER = "moreThanOneCustomer";
  static final String CHECK_MORE_THAN_ONE_DOCUMENT = "moreThanOneDoc";
  static final String CHECK_MORE_THAN_ONE_SALES_REP = "moreThanOnesalesRep";
  static final String PARAM_INP_ARCHIVE = "inpArchive";
  static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";
  static final String XML_PARAMETER_DIRECTORY = "directory";
  private static final String BASE_DIRECTORY_JS = "var baseDirectory = \"%s/\";\r\n";
  private static JSONObject hookParams;
  private static PrintControllerHookManager hookManager;
  final MutableBoolean hooking = new MutableBoolean(false);

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    final VariablesSecureApp vars = new VariablesSecureApp(request);
    if (log4j.isDebugEnabled()) {
      log4j.debug("Servletpath: " + request.getServletPath());
    }
    PrintControllerRequestResolver.RequestContext requestContext =
        PrintControllerRequestResolver.resolve(request, vars);
    post(request, response, vars, requestContext.documentType, requestContext.sessionValuePrefix,
        requestContext.documentId);
  }

  private static void initializeHooksAndParams() throws JSONException {
    hookManager = WeldUtils.getInstanceFromStaticBeanManager(PrintControllerHookManager.class);
    hookParams = new JSONObject();
    JSONObject results = new JSONObject();
    results.put(PrintControllerHookManager.FAILURES, false);
    results.put(PrintControllerHookManager.MESSAGE, HashBasedTable.create());
    hookParams.put(PrintControllerHookManager.RESULTS, results);
    hookParams.put(PrintControllerHookManager.CANCELLATION, false);
  }

  @SuppressWarnings("unchecked")
  protected void post(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, DocumentType documentType, String sessionValuePrefix,
      String strDocumentId) throws IOException, ServletException {
    try {
      PrintControllerCommandHandler.Context context = PrintControllerCommandHandler.createContext(
          this, vars, documentType, sessionValuePrefix, strDocumentId);
      initializeHooksAndParams();
      hooking.setValue(true);
      new PrintControllerCommandHandler(this, request, response, vars, context).handle();
    } catch (Exception e) {
      // Catching the exception here instead of throwing it to HSAS because this is used in multi
      // part request making the mechanism to detect popup not to work.
      log4j.error("Error captured: ", e);
      bdErrorGeneralPopUp(request, response, MSG_TYPE_ERROR,
          Utility.translateError(this, vars, vars.getLanguage(), e.getMessage()).getMessage());
    } finally {
      hooking.setValue(false);
      printingErrorOccurred.setValue(false);
    }
  }

  void preparePreProcessHooks(DocumentType documentType, String documentId) throws JSONException {
    PrintControllerHookSupport.setPreHookParams(documentType, hookParams, documentId);
    PrintControllerHookSupport.executePreProcessHooks(hookManager, hookParams);
  }

  ReportManager createReportManager() {
    return new ReportManager(globalParameters.strFTPDirectory, strReplaceWithFull,
        globalParameters.strBaseDesignPath, globalParameters.strDefaultDesignPath,
        globalParameters.prefix, multiReports);
  }

  void showPageError(HttpServletResponse response) throws IOException, ServletException {
    pageError(response);
  }

  void showAdvicePopup(HttpServletRequest request, HttpServletResponse response, String title,
      String message) throws IOException, ServletException {
    advisePopUp(request, response, title, message);
  }

  void closePopupAndRefreshParent(HttpServletResponse response, VariablesSecureApp vars)
      throws IOException, ServletException {
    printPageClosePopUpAndRefreshParent(response, vars);
  }

  protected void persistLastUsedContact(VariablesSecureApp vars, String bpartnerId) throws ServletException {
    String toContactId = vars.getStringParameter(PARAM_TO_CONTACT_ID);
    if (StringUtils.isBlank(toContactId)) {
      String toEmail = vars.getStringParameter(PARAM_TO_EMAIL);
      toContactId = BPContactEmailSelector.findContactIdByEmail(bpartnerId, toEmail);
    }
    if (StringUtils.isNotBlank(toContactId)) {
      BPContactEmailSelector.saveLastUsedContact(vars.getUser(), bpartnerId, toContactId);
    }
  }

  public void printReports(HttpServletResponse response, Collection<JasperPrint> jrPrintReports,
      Collection<Report> reports, boolean directPrint) {

    ByteArrayOutputStream tempOutputStream = new ByteArrayOutputStream();
    String filename = "";
    Map<Object, Object> parameters = new HashMap<>();
    try {
      response.setContentType("application/pdf");
      ServletOutputStream os = response.getOutputStream();

      if (!multiReports && !archivedReports) {
        filename = PrintControllerDocumentHelper.getFilenameForReports(reports);
        if (!directPrint) {
          handleIndirectPrint(response, jrPrintReports, reports, filename, parameters, tempOutputStream);
        } else {
          handleDirectPrint(response, jrPrintReports, reports, filename, tempOutputStream, os);
        }
      } else {
        tempOutputStream = concatReport(reports.toArray(new Report[]{ }), jrPrintReports, response, directPrint);
      }
      updateOrderDatePrintedForSalesOrders(reports);
    } catch (IOException e) {
      log4j.error(e.getMessage());
      printingErrorOccurred.setValue(true);
    } catch (JRException | ServletException e) {
      e.printStackTrace();
      printingErrorOccurred.setValue(true);
    } catch (PrintControllerHookManager.PrintControllerHookException e) {
      printingErrorOccurred.setValue(true);
      throw new OBException(String.format(OBMessageUtils.messageBD(ERROR_PRINTING_DOCUMENT_KEY),
          LIST_ITEM_TAG + e.getMessage() + CLOSE_LIST_ITEM_TAG));
    } finally {
      finalizePrintResponse(response, reports, tempOutputStream);
    }
  }

  private void finalizePrintResponse(HttpServletResponse response, Collection<Report> reports,
      ByteArrayOutputStream tempOutputStream) {
    try {
      if (printingErrorOccurred.booleanValue()) {
        // Clear the output stream
        response.reset();
      } else {
        // Write the buffered PDF content to the actual output stream
        response.setContentType("application/pdf");
        ServletOutputStream os = response.getOutputStream();
        tempOutputStream.writeTo(os);
        os.flush();
      }
    } catch (IOException e) {
      log4j.error(e.getMessage(), e);
    } finally {
      try {
        for (Report report : reports) {
          // Delete temporal reports generated for the returned report in case they have been
          // attached also
          String targetLocation = report.getTargetLocation();
          if (targetLocation != null) {
            File canonicalFile = new File(targetLocation).getCanonicalFile();
            File canonicalBase = new File(globalParameters.strFTPDirectory).getCanonicalFile();
            if (canonicalFile.getPath().startsWith(canonicalBase.getPath() + File.separator)
                && canonicalFile.exists() && !canonicalFile.isDirectory()) {
              Files.delete(canonicalFile.toPath());
            }
          }
        }
      } catch (IOException e) {
        log4j.error("Error deleting temporal reports", e);
      }
    }
  }

  private void updateOrderDatePrintedForSalesOrders(Collection<Report> reports) throws ServletException {
    for (Report report : reports) {
      if (Objects.requireNonNull(report.getDocumentType()) == DocumentType.SALESORDER) {
        PrintControllerData.updateOrderDatePrinted(this, report.getDocumentId());
      }
    }
  }

  private void handleDirectPrint(HttpServletResponse response, Collection<JasperPrint> jrPrintReports,
      Collection<Report> reports, String filename, ByteArrayOutputStream tempOutputStream,
      ServletOutputStream os) throws IOException, PrintControllerHookManager.PrintControllerHookException, JRException {
    response.setContentType("text/html");
    String safeFilename = filename.replace("\r", "").replace("\n", "")
        .replace("/", "_").replace("\\", "_").replace("..", "_");
    File file = Files
        .createTempFile(Paths.get(globalParameters.strFTPDirectory), "rpt-", ".pdf")
        .toFile();
    manageDirectPrintOnlyHooks(jrPrintReports, reports, safeFilename, file, tempOutputStream, os);
  }

  private void handleIndirectPrint(HttpServletResponse response, Collection<JasperPrint> jrPrintReports,
      Collection<Report> reports, String filename, Map<Object, Object> parameters,
      ByteArrayOutputStream tempOutputStream) throws JRException, IOException, PrintControllerHookManager.PrintControllerHookException {
    String safeFilename = filename.replace("\r", "").replace("\n", "");
    response.setHeader("Content-disposition", "attachment; filename=" + safeFilename);

    // Generate the report in a temporary output stream
    for (JasperPrint jasperPrint : jrPrintReports) {
      ReportingUtils.saveReport(jasperPrint, ExportType.PDF, parameters, tempOutputStream);
    }

    managePrintOnlyHooking(reports, tempOutputStream);
  }

  private void manageDirectPrintOnlyHooks(Collection<JasperPrint> jrPrintReports, Collection<Report> reports, String filename, File file,
      ByteArrayOutputStream tempOutputStream,
      ServletOutputStream os) throws IOException, PrintControllerHookManager.PrintControllerHookException, JRException {
    if (hooking.booleanValue()) {
      hookedDirectPrint(filename, file, reports.iterator().next(), tempOutputStream, os);
    } else {
      for (JasperPrint jasperPrint : jrPrintReports) {
        ReportingUtils.saveReport(jasperPrint, ExportType.PDF, new HashMap<>(), tempOutputStream);
      }
      tempOutputStream.writeTo(os);
      doDirectPrint(os, file.getName());
    }
  }

  private void managePrintOnlyHooking(Collection<Report> reports,
      ByteArrayOutputStream tempOutputStream) throws IOException, PrintControllerHookManager.PrintControllerHookException {
    if (hooking.booleanValue()) {
      // Convert the output stream into an input stream to modify it in hooks
      try (ByteArrayInputStream pdfInputStream = new ByteArrayInputStream(tempOutputStream.toByteArray())) {
        Report report = reports.iterator().next();
        PrintControllerHookSupport.setPostHookParams(report.getDocumentType(), hookParams,
            report.getDocumentId(), pdfInputStream, tempOutputStream);
      }
      hookManager.executeHooks(hookParams, hookManager.getPostProcess());
    }
  }

  private void doDirectPrint(ServletOutputStream os, String fileName) throws IOException {
    String href = getServletContext().getContextPath() + "/utility/DownloadReport.html?report="
        + fileName + "&inline=true";
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/base/secureApp/DirectPrint")
        .createXmlDocument();
    xmlDocument.setParameter("href", href);
    os.println(xmlDocument.print());
  }

  public void printReports(HttpServletResponse response, Collection<JasperPrint> jrPrintReports,
      Collection<Report> reports) {
    JSONObject jsonParams = new JSONObject();
    try {
      JSONObject results = new JSONObject();
      results.put(PrintControllerHookManager.FAILURES, false);
      results.put(PrintControllerHookManager.MESSAGE, HashBasedTable.create());
      jsonParams.put(PrintControllerHookManager.RESULTS, results);
      jsonParams.put(PrintControllerHookManager.CANCELLATION, false);
      Report report = reports.iterator().next();
      PrintControllerHookSupport.setPreHookParams(report.getDocumentType(), jsonParams,
          report.getDocumentId());
    } catch (JSONException e) {
      throw new OBException(e);
    }

    printReports(response, jrPrintReports, reports, false);
  }

  private ByteArrayOutputStream concatReport(Report[] reports, Collection<JasperPrint> jrPrintReports,
      HttpServletResponse response, boolean directPrint) {
    ByteArrayOutputStream tempOutputStream = new ByteArrayOutputStream();
    String filename = "";
    boolean createBookmarks = true;
    SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();

    try {
      if (reports.length == 1) {
        filename = reports[0].getFilename();
        createBookmarks = false;
      } else if (reports.length > 1) {
        filename = reports[0].getTemplateInfo().getReportFilename();
        filename = filename.replace("@our_ref@", "");
        filename = filename.replace("@cus_ref@", "");
        filename = filename.replace(" ", "_");
        filename = filename.replace("-", "");
        filename = filename + ".pdf";
      }
      if (!directPrint) {
        String safeFilename = filename.replace("\r", "").replace("\n", "");
        response.setHeader("Content-disposition", "attachment; filename=" + safeFilename);

        // Concatenate reports in a temporary OutputStream
        ReportingUtils.concatPDFReport(new ArrayList<>(jrPrintReports), createBookmarks,
            tempOutputStream, configuration);
        // Modify the concatenated report with hooks
        ByteArrayInputStream pdfInputStream = new ByteArrayInputStream(tempOutputStream.toByteArray());

        // Call hooks
        if (hooking.booleanValue()) {
          ByteArrayOutputStream postProcessOutputStream = new ByteArrayOutputStream();
          Report report = reports[0];
          PrintControllerHookSupport.setPostHookParams(report.getDocumentType(), hookParams,
              report.getDocumentId(), pdfInputStream, postProcessOutputStream);

          hookManager.executeHooks(hookParams, hookManager.getPostProcess());

          if (postProcessOutputStream.size() > 0) {
            return postProcessOutputStream;
          } else {
            ServletOutputStream os = response.getOutputStream();
            tempOutputStream.writeTo(os);
            os.flush();
            return tempOutputStream;
          }
        }
      } else {
        response.setContentType("text/html");
        Path path = Files.createTempFile(Paths.get(globalParameters.strFTPDirectory),
            "rpt-", ".pdf");
        try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE)) {
          ReportingUtils.concatPDFReport(new ArrayList<>(jrPrintReports), createBookmarks,
              outputStream, configuration);
        }

        if (hooking.booleanValue()) {
          hookedDirectPrint(filename, path.toFile(), reports[0], tempOutputStream, response.getOutputStream());
        } else {
          doDirectPrint(response.getOutputStream(), path.toFile().getName());
        }
      }
    } catch (PrintControllerHookManager.PrintControllerHookException e) {
      printingErrorOccurred.setValue(true);
      throw new OBException(String.format(OBMessageUtils.messageBD(ERROR_PRINTING_DOCUMENT_KEY),
          LIST_ITEM_TAG + e.getMessage() + CLOSE_LIST_ITEM_TAG));
    } catch (Exception e) {
      log4j.error(e);
    }

    return tempOutputStream;
  }

  private void hookedDirectPrint(String filename, File path, Report reports, ByteArrayOutputStream tempResponse,
      ServletOutputStream response) throws IOException, PrintControllerHookManager.PrintControllerHookException {
    // Edit the temp file in hooks and write the result to another file
    File hookedFile = Files.createTempFile(Paths.get(globalParameters.strFTPDirectory), "rpt-hooked-",
        ".pdf").toFile();
    try (FileInputStream fileInputStream = new FileInputStream(path);
         FileOutputStream hookedFileOutputStream = new FileOutputStream(hookedFile)) {

      // Call hooks
      PrintControllerHookSupport.setPostHookParams(reports.getDocumentType(), hookParams,
          reports.getDocumentId(), fileInputStream, hookedFileOutputStream);
    }

    try {
      hookManager.executeHooks(hookParams, hookManager.getPostProcess());
    } catch (PrintControllerHookManager.PrintControllerHookException e) {
      throw new OBException(String.format(OBMessageUtils.messageBD(ERROR_PRINTING_DOCUMENT_KEY),
          LIST_ITEM_TAG + e.getMessage() + CLOSE_LIST_ITEM_TAG));
    }

    // Delete the original file
    Files.delete(path.toPath());
    tempResponse.writeTo(response);
    doDirectPrint(response, hookedFile.getName());
  }

  public Report buildReport(HttpServletResponse response, VariablesSecureApp vars,
      String strDocumentId, final ReportManager reportManager, DocumentType documentType,
      OutputTypeEnum outputType, String templateId) {
    String localStrDocumentId = strDocumentId;
    Report report = null;
    if (localStrDocumentId != null) {
      localStrDocumentId = localStrDocumentId.replace("(", "").replace(")", "").replace("'", "");
    }
    try {
      report = new Report(documentType, localStrDocumentId, vars.getLanguage(), templateId,
          multiReports, outputType);
    } catch (final ReportingException e) {
      log4j.error(e);
    } catch (final ServletException e) {
      log4j.error(e);
    }

    reportManager.setTargetDirectory(report);
    return report;
  }

  public void buildReport(HttpServletResponse response, VariablesSecureApp vars,
      String strDocumentId, Map<String, Report> reports, final ReportManager reportManager)
      throws ServletException, IOException {
    String localStrDocumentId = strDocumentId;
    final String documentId = vars.getStringParameter("inpDocumentId");
    if (localStrDocumentId != null) {
      localStrDocumentId = localStrDocumentId.replace("(", "").replace(")", "").replace("'", "");
    }
    final Report report = reports.get(localStrDocumentId);
    if (report == null) {
      throw new ServletException(
          Utility.messageBD(this, "NoDataReport", vars.getLanguage()) + documentId);
    }
    // Check if the document is not in status 'draft'
    if (!report.isDraft() && !report.isAttached() && vars.commandIn("ARCHIVE")) {
      // TODO: Move the table Id retrieval into the DocumentType
      // getTableId method!
      // get the Id of the entities table, this is used to store the
      // file as an OB attachment
      final String tableId = ToolsData.getTableId(this, report.getDocumentType().getTableName());

      if (log4j.isDebugEnabled()) {
        log4j.debug(
            "Table " + report.getDocumentType().getTableName() + " has table id: " + tableId);
      }
      // Save the report as a attachment because it is being
      // transferred to the user
      File attachedFile = null;
      try {
        attachedFile = reportManager.createAttachmentForReport(this, report, tableId, vars);
      } catch (final ReportingException exception) {
        throw new ServletException(exception);
      } finally {
        // Delete the original file generated for the attachment because the upload process has
        // already copied it on the proper location
        if (attachedFile != null && attachedFile.exists()) {
          Files.delete(attachedFile.toPath());
        }
      }
    } else {
      if (log4j.isDebugEnabled()) {
        log4j.debug("Document is not attached.");
      }
    }
  }

  void seekAndDestroy(List<AttachContent> attachments, String documentToDelete) {
    for (int i = 0; i < attachments.size(); i++) {
      final AttachContent content = attachments.get(i);
      if (content.id.equals(documentToDelete)) {
        attachments.remove(i);
        break;
      }
    }

  }

  void createPrintOptionsPage(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, DocumentType documentType, String strDocumentId,
      Map<String, Report> reports) throws IOException, ServletException {
    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/utility/reporting/printing/PrintOptions")
        .createXmlDocument();
    xmlDocument.setParameter("strDocumentId", strDocumentId);

    // Get additional document information
    String strIsDirectPDF = vars.getStringParameter("isDirectPDF");
    if (strIsDirectPDF == null || "".equals(strIsDirectPDF)) {
      strIsDirectPDF = "false";
    }
    String strIsDirectAttach = vars.getStringParameter("isDirectAttach");
    if (strIsDirectAttach == null || "".equals(strIsDirectAttach)) {
      strIsDirectAttach = "false";
    }
    xmlDocument.setParameter(XML_PARAMETER_DIRECTORY, getBaseDirectoryJs());
    xmlDocument.setParameter("language", vars.getLanguage());
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("description", "");
    xmlDocument.setParameter("help", "");
    xmlDocument.setParameter("isDirectPDF", "isDirectPDF = " + strIsDirectPDF + ";\r\n");
    xmlDocument.setParameter("isDirectAttach", "isDirectAttach = " + strIsDirectAttach + ";\r\n");
    response.setContentType(CONTENT_TYPE_HTML);
    final PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  void createEmailOptionsPage(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, DocumentType documentType, String strDocumentId,
      Map<String, Report> reports, HashMap<String, Boolean> checks)
      throws IOException, ServletException, ReportingException {
    String fullDocumentIdentifier = PrintControllerDocumentHelper.normalizeDocumentId(strDocumentId)
        + documentType.getTableName();
    PrintControllerEmailOptionsBuilder.Context context =
        new PrintControllerEmailOptionsBuilder.Context(documentType, strDocumentId, reports, checks,
            fullDocumentIdentifier);
    new PrintControllerEmailOptionsBuilder(this, request, response, vars, context).render();
  }

  XmlDocument createEmailOptionsXmlDocument(String[] hiddenTags) throws IOException, ServletException {
    if (hiddenTags != null) {
      return xmlEngine
          .readXmlTemplate("org/openbravo/erpCommon/utility/reporting/printing/EmailOptions",
              hiddenTags)
          .createXmlDocument();
    }
    return xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/utility/reporting/printing/EmailOptions")
        .createXmlDocument();
  }

  String getBaseDirectoryJs() {
    return String.format(BASE_DIRECTORY_JS, strReplaceWith);
  }

  boolean isDebugEnabled() {
    return log4j.isDebugEnabled();
  }

  void debug(String message) {
    log4j.debug(message);
  }

  void warn(String message, String detail) {
    log4j.warn(message.replace("{}", detail));
  }

  void error(Throwable throwable) {
    log4j.error(throwable);
  }

  /**
   * Reports a "no sender configured" error by setting an {@link OBError} message on the current
   * tab and closing the popup while refreshing the parent record. This ensures the error is
   * displayed inline on the record (in red) rather than as a generic popup dialog.
   * @param response the HTTP response used to render the close-popup page
   * @param vars the session variables, used to resolve the active tab and language
   * @throws IOException if writing the response fails
   * @throws ServletException always thrown after the error is reported, to stop processing
   */
  void reportNoSenderError(HttpServletResponse response, VariablesSecureApp vars)
      throws IOException, ServletException {
    final OBError on = new OBError();
    on.setMessage(Utility.messageBD(this, "NoSender", vars.getLanguage()));
    on.setTitle(Utility.messageBD(this, "EmailConfigError", vars.getLanguage()));
    on.setType(MSG_TYPE_ERROR);
    final String tabId = vars.getSessionValue(INP_TAB_ID);
    vars.getStringParameter(TAB);
    vars.setMessage(tabId, on);
    vars.getRequestGlobalVariable(INP_TAB_ID, ATTRIBUTESETINSTANCE_TABID);
    printPageClosePopUpAndRefreshParent(response, vars);
    throw new ServletException(Utility.messageBD(this, "EmailNoSenderDefined", vars.getLanguage()));
  }

  void createPrintStatusPage(HttpServletResponse response, VariablesSecureApp vars,
      int nrOfEmailsSend) throws IOException, ServletException {
    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/utility/reporting/printing/PrintStatus")
        .createXmlDocument();
    xmlDocument.setParameter(XML_PARAMETER_DIRECTORY, getBaseDirectoryJs());
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("language", vars.getLanguage());
    xmlDocument.setParameter("nrOfEmailsSend", "" + nrOfEmailsSend);

    response.setContentType(CONTENT_TYPE_HTML);
    final PrintWriter out = response.getWriter();

    out.println(xmlDocument.print());
    out.close();
  }

  @Override
  public String getServletInfo() {
    return "Servlet that processes the print action";
  } // End of getServletInfo() method
}
