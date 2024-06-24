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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringJoiner;

import javax.persistence.PersistenceException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.common.hooks.PrintControllerHookManager;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.email.EmailUtils;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.BasicUtility;
import org.openbravo.erpCommon.utility.OBError;
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
  private static final String DOCUMENT_ID = "documentId";
  private static final String DOCUMENT_TYPE = "documentType";
  public static final String REPORT_FILE_PATH = "reportFilePath";
  private final Map<String, TemplateData[]> differentDocTypes = new HashMap<String, TemplateData[]>();
  private boolean multiReports = false;
  private boolean archivedReports = false;
  private static final String PRINT_PATH = "print.html";
  private static final String PRINT_OPTIONS_PATH = "printoptions.html";
  private static final String SEND_PATH = "send.html";

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    final VariablesSecureApp vars = new VariablesSecureApp(request);

    DocumentType documentType = DocumentType.UNKNOWN;
    String sessionValuePrefix = null;
    String strDocumentId = null;

    // Determine which process called the print controller
    if (log4j.isDebugEnabled()) {
      log4j.debug("Servletpath: " + request.getServletPath());
    }
    if (request.getServletPath().toLowerCase().indexOf("quotations") != -1) {
      documentType = DocumentType.QUOTATION;
      // The prefix PRINTORDERS is a fixed name based on the KEY of the
      // AD_PROCESS
      sessionValuePrefix = "PRINTQUOTATIONS";

      strDocumentId = vars.getSessionValue(sessionValuePrefix + ".inpcOrderId_R");
      if (strDocumentId.equals("")) {
        strDocumentId = vars.getSessionValue(sessionValuePrefix + ".inpcOrderId");
      }
    }
    if (request.getServletPath().toLowerCase().indexOf("orders") != -1) {
      documentType = DocumentType.SALESORDER;
      // The prefix PRINTORDERS is a fixed name based on the KEY of the
      // AD_PROCESS
      sessionValuePrefix = "PRINTORDERS";

      strDocumentId = vars.getSessionValue(sessionValuePrefix + ".inpcOrderId_R");
      if (strDocumentId.equals("")) {
        strDocumentId = vars.getSessionValue(sessionValuePrefix + ".inpcOrderId");
      }
    }
    if (request.getServletPath().toLowerCase().indexOf("invoices") != -1) {
      documentType = DocumentType.SALESINVOICE;
      // The prefix PRINTINVOICES is a fixed name based on the KEY of the
      // AD_PROCESS
      sessionValuePrefix = "PRINTINVOICES";

      strDocumentId = vars.getSessionValue(sessionValuePrefix + ".inpcInvoiceId_R");
      if (strDocumentId.equals("")) {
        strDocumentId = vars.getSessionValue(sessionValuePrefix + ".inpcInvoiceId");
      }
    }
    if (request.getServletPath().toLowerCase().indexOf("shipments") != -1) {
      documentType = DocumentType.SHIPMENT;
      // The prefix PRINTINVOICES is a fixed name based on the KEY of the
      // AD_PROCESS
      sessionValuePrefix = "PRINTSHIPMENTS";

      strDocumentId = vars.getSessionValue(sessionValuePrefix + ".inpmInoutId_R");
      if (strDocumentId.equals("")) {
        strDocumentId = vars.getSessionValue(sessionValuePrefix + ".inpmInoutId");
      }
    }
    if (request.getServletPath().toLowerCase().indexOf("payments") != -1) {
      documentType = DocumentType.PAYMENT;
      // The prefix PRINTPAYMENTS is a fixed name based on the KEY of the
      // AD_PROCESS
      sessionValuePrefix = "PRINTPAYMENT";

      strDocumentId = vars.getSessionValue(sessionValuePrefix + ".inpfinPaymentId_R");
      if (strDocumentId.equals("")) {
        strDocumentId = vars.getSessionValue(sessionValuePrefix + ".inpfinPaymentId");
      }
    }

    post(request, response, vars, documentType, sessionValuePrefix, strDocumentId);

  }

  @SuppressWarnings("unchecked")
  protected void post(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, DocumentType documentType, String sessionValuePrefix,
      String strDocumentId) throws IOException, ServletException {
    String localStrDocumentId = strDocumentId;
    try {
      String fullDocumentIdentifier = localStrDocumentId + documentType.getTableName();

      // reports is set in session: defining it as a Serializable HashMap
      HashMap<String, Report> reports;

      // Checks are maintained in this way for mulithread safety
      HashMap<String, Boolean> checks = new HashMap<String, Boolean>();
      checks.put("moreThanOneCustomer", Boolean.FALSE);
      checks.put("moreThanOnesalesRep", Boolean.FALSE);

      String documentIds[] = null;
      if (log4j.isDebugEnabled()) {
        log4j.debug("strDocumentId: " + localStrDocumentId);
      }
      // normalize the string of ids to a comma separated list
      localStrDocumentId = localStrDocumentId.replaceAll("\\(|\\)|'", "");
      if (localStrDocumentId.length() == 0) {
        throw new ServletException(Utility.messageBD(this, "NoDocument", vars.getLanguage()));
      }

      documentIds = localStrDocumentId.split(",");

      if (log4j.isDebugEnabled()) {
        log4j.debug("Number of documents selected: " + documentIds.length);
      }

      multiReports = (documentIds.length > 1);

      reports = (HashMap<String, Report>) vars.getSessionObject(sessionValuePrefix + ".Documents");
      final ReportManager reportManager = new ReportManager(globalParameters.strFTPDirectory,
          strReplaceWithFull, globalParameters.strBaseDesignPath,
          globalParameters.strDefaultDesignPath, globalParameters.prefix, multiReports);
      PrintControllerHookManager hookManager = WeldUtils.getInstanceFromStaticBeanManager(
          PrintControllerHookManager.class);
      JSONObject jsonParams = new JSONObject();
      JSONObject results = new JSONObject();
      StringBuilder errorMessages = new StringBuilder();
      StringJoiner errorPopUpMessage = new StringJoiner("\n",
          "The following errors have arisen when printing the selected documents: \n<ul>\n", "\n</ul>");
      results.put(PrintControllerHookManager.FAILURES, false);
      results.put(PrintControllerHookManager.MESSAGE, HashBasedTable.create());
      jsonParams.put(PrintControllerHookManager.RESULTS, results);
      jsonParams.put(PrintControllerHookManager.CANCELLATION, false);
      if (vars.commandIn("PRINT")) {
        archivedReports = false;
        // Order documents by Document No.
        if (multiReports) {
          documentIds = orderByDocumentNo(documentType, documentIds);
        }

        /*
         * PRINT option will print directly to the UI for a single report. For multiple reports the
         * documents will each be saved individually and the concatenated in the same manner as the
         * saved reports. After concatenating the reports they will be deleted.
         */
        Report report = null;
        JasperPrint jasperPrint = null;
        Collection<JasperPrint> jrPrintReports = new ArrayList<JasperPrint>();
        final Collection<Report> savedReports = new ArrayList<Report>();
        for (int i = 0; i < documentIds.length; i++) {
          String documentId = documentIds[i];
          jsonParams.put(DOCUMENT_ID, documentId);
          jsonParams.put(DOCUMENT_TYPE, documentType);
          try {
              hookManager.executeHooks(jsonParams, hookManager.getPreProcess());
          } catch (final OBException e) {
            String documentNo = getDocumentNo(documentType, jsonParams);
            errorMessages.append("<li>").append(documentNo).append(": ").append(e.getMessage()).append("</li>");
          }
          report = buildReport(response, vars, documentId, reportManager, documentType,
              Report.OutputTypeEnum.PRINT);
          try {
            jasperPrint = reportManager.processReport(report, vars);
            jrPrintReports.add(jasperPrint);
          }catch (final ReportingException e) {
            advisePopUp(request, response, "Report processing failed","Unable to process report selection");
            log4j.error(e.getMessage());
            e.getStackTrace();
          }
          savedReports.add(report);
          if (multiReports) {
            reportManager.saveTempReport(report, vars);
          }
          if (!jsonParams.getBoolean(PrintControllerHookManager.CANCELLATION)) {
            jsonParams.put(DOCUMENT_ID, documentId);
            jsonParams.put(DOCUMENT_TYPE, documentType);
            jsonParams.put(REPORT_FILE_PATH, report.getTargetLocation());
            hookManager.executeHooks(jsonParams, hookManager.getPostProcess());
          }
        }
        if (StringUtils.isNotEmpty(errorMessages.toString())) {
          errorMessages.append("</ul>");
          errorPopUpMessage.add(errorMessages.toString());
          throw new OBException(errorPopUpMessage.toString());
        }
        printReports(response, jrPrintReports, savedReports, isDirectPrint(vars));
      } else if (vars.commandIn("ARCHIVE")) {
        // Order documents by Document No.
        if (multiReports) {
          documentIds = orderByDocumentNo(documentType, documentIds);
        }

        /*
         * ARCHIVE will save each report individually and then print the reports in a single
         * printable (concatenated) format.
         */
        archivedReports = true;
        Report report = null;
        JasperPrint jasperPrint = null;
        Collection<JasperPrint> jrPrintReports = new ArrayList<JasperPrint>();
        final Collection<Report> savedReports = new ArrayList<Report>();
        for (int index = 0; index < documentIds.length; index++) {
          String documentId = documentIds[index];
          jsonParams.put(DOCUMENT_ID, documentId);
          jsonParams.put(DOCUMENT_TYPE, documentType);
          try {
            hookManager.executeHooks(jsonParams, hookManager.getPreProcess());
          } catch (final OBException e) {
            String documentNo = getDocumentNo(documentType, jsonParams);
            errorMessages.append("<li>").append(documentNo).append(": ").append(e.getMessage()).append("</li>");
          }
          report = buildReport(response, vars, documentId, reportManager, documentType,
              OutputTypeEnum.ARCHIVE);
          buildReport(response, vars, documentId, reports, reportManager);
          try {
            jasperPrint = reportManager.processReport(report, vars);
            jrPrintReports.add(jasperPrint);
          } catch (final ReportingException e) {
            log4j.error(e);
          }
          reportManager.saveTempReport(report, vars);
          if (!jsonParams.getBoolean(PrintControllerHookManager.CANCELLATION)) {
            jsonParams.put(DOCUMENT_ID, documentId);
            jsonParams.put(DOCUMENT_TYPE, documentType);
            jsonParams.put(REPORT_FILE_PATH, report.getTargetLocation());
            hookManager.executeHooks(jsonParams, hookManager.getPostProcess());
          }
          savedReports.add(report);
        }
        if (StringUtils.isNotEmpty(errorMessages.toString())) {
          errorMessages.append("</ul>");
          errorPopUpMessage.add(errorMessages.toString());
          throw new OBException(errorPopUpMessage.toString());
        }
        printReports(response, jrPrintReports, savedReports, isDirectPrint(vars));
      } else {
        if (vars.commandIn("DEFAULT")) {

          differentDocTypes.clear();
          reports = new HashMap<String, Report>();
          for (int index = 0; index < documentIds.length; index++) {
            final String documentId = documentIds[index];
            if (log4j.isDebugEnabled()) {
              log4j.debug("Processing document with id: " + documentId);
            }

            try {
              final Report report = new Report(documentType, documentId, vars.getLanguage(),
                  "default", multiReports, OutputTypeEnum.DEFAULT);
              reports.put(documentId, report);

              final String senderAddress = EmailData.getSenderAddress(this, vars.getClient(),
                  report.getOrgId());

              if (request.getServletPath().toLowerCase().indexOf(PRINT_PATH) == -1
                  && request.getServletPath().toLowerCase().indexOf(PRINT_OPTIONS_PATH) == -1) {
                if ("".equals(senderAddress) || senderAddress == null) {
                  final OBError on = new OBError();
                  on.setMessage(Utility.messageBD(this, "NoSender", vars.getLanguage()));
                  on.setTitle(Utility.messageBD(this, "EmailConfigError", vars.getLanguage()));
                  on.setType("Error");
                  final String tabId = vars.getSessionValue("inpTabId");
                  vars.getStringParameter("tab");
                  vars.setMessage(tabId, on);
                  vars.getRequestGlobalVariable("inpTabId", "AttributeSetInstance.tabId");
                  printPageClosePopUpAndRefreshParent(response, vars);
                  throw new ServletException("Configuration Error no sender defined");
                }
              }

              // check the different doc typeId's if all the selected
              // doc's
              // has the same doc typeId the template selector should
              // appear
              if (!differentDocTypes.containsKey(report.getDocTypeId())) {
                differentDocTypes.put(report.getDocTypeId(), report.getTemplate());
              }
            } catch (final ReportingException exception) {
              throw new ServletException(exception);
            }

          }

          vars.setSessionObject(sessionValuePrefix + ".Documents", reports);

          if (request.getServletPath().toLowerCase().indexOf(PRINT_PATH) != -1) {
            createPrintOptionsPage(request, response, vars, documentType,
                getComaSeparatedString(documentIds), reports);
          } else if (request.getServletPath().toLowerCase().indexOf(SEND_PATH) != -1) {
            createEmailOptionsPage(request, response, vars, documentType,
                getComaSeparatedString(documentIds), reports, checks, fullDocumentIdentifier);
          }

        } else if (vars.commandIn("ADD")) {
          if (request.getServletPath().toLowerCase().indexOf(PRINT_PATH) != -1) {
            createPrintOptionsPage(request, response, vars, documentType,
                getComaSeparatedString(documentIds), reports);
          } else {
            createEmailOptionsPage(request, response, vars, documentType,
                getComaSeparatedString(documentIds), reports, checks, fullDocumentIdentifier);
          }

        } else if (vars.commandIn("DEL")) {
          final String documentToDelete = vars.getStringParameter("idToDelete");
          final List<AttachContent> attachments = (List<AttachContent>) request.getSession()
              .getAttribute("files");
          request.getSession().setAttribute("files", attachments);

          seekAndDestroy(attachments, documentToDelete);
          createEmailOptionsPage(request, response, vars, documentType,
              getComaSeparatedString(documentIds), reports, checks, fullDocumentIdentifier);

        } else if (vars.commandIn("EMAIL")) {
          PocData[] pocData = (PocData[]) vars.getSessionObject("pocData" + fullDocumentIdentifier);
          int nrOfEmailsSend = 0;
          for (final PocData documentData : pocData) {
            getEnvironentInformation(pocData, checks);
            final String documentId = documentData.documentId;
            if (log4j.isDebugEnabled()) {
              log4j.debug("Processing document with id: " + documentId);
            }

            String templateInUse = "default";
            if (differentDocTypes.size() == 1) {
              templateInUse = vars.getRequestGlobalVariable("templates", "templates");
            }

            final Report report = buildReport(response, vars, documentId, reportManager,
                documentType, OutputTypeEnum.EMAIL, templateInUse);

            // if there is only one document type id the user should be
            // able to choose between different templates
            if (differentDocTypes.size() == 1) {
              final String templateId = vars.getRequestGlobalVariable("templates", "templates");
              try {
                final TemplateInfo usedTemplateInfo = new TemplateInfo(this, report.getDocTypeId(),
                    report.getOrgId(), vars.getLanguage(), templateId);
                report.setTemplateInfo(usedTemplateInfo);
              } catch (final ReportingException e) {
                throw new ServletException("Error trying to get template information", e);
              }
            }

            if (report == null) {
              throw new ServletException(
                  Utility.messageBD(this, "NoDataReport", vars.getLanguage()) + documentId);
            }
            // Check if the document is not in status 'draft'
            if (!report.isDraft()) {
              // Check if the report is already attached
              if (!report.isAttached()) {
                // get the Id of the entities table, this is used to
                // store the file as an OB attachment
                final String tableId = ToolsData.getTableId(this,
                    report.getDocumentType().getTableName());

                // If the user wants to archive the document
                if (vars.getStringParameter("inpArchive").equals("Y")) {
                  // Save the report as a attachment because it is
                  // being transferred to the user
                  try {
                    reportManager.createAttachmentForReport(this, report, tableId, vars,
                        ReportManager.GENERATED_BY_EMAILING);
                  } catch (final ReportingException exception) {
                    throw new ServletException(exception);
                  }
                } else {
                  reportManager.saveTempReport(report, vars);
                }
              } else {
                if (log4j.isDebugEnabled()) {
                  log4j.debug("Document is not attached.");
                }
              }
              final String senderAddress = vars.getStringParameter("fromEmail");
              checks.put("differentDocTypes", differentDocTypes.size() > 1);

              EmailUtilities.prepareAndSendEmail(report, vars,
                  (List<AttachContent>) request.getSession().getAttribute("files"), documentData,
                  senderAddress, checks,  this);
              nrOfEmailsSend++;
            }
          }
          request.getSession().removeAttribute("files");
          vars.removeSessionValue("pocData" + fullDocumentIdentifier);
          createPrintStatusPage(response, vars, nrOfEmailsSend);
        } else if (vars.commandIn("UPDATE_TEMPLATE")) {
          JSONObject o = new JSONObject();
          try {
            PocData[] pocData = (PocData[]) vars
                .getSessionObject("pocData" + fullDocumentIdentifier);
            final String templateId = vars.getRequestGlobalVariable("templates", "templates");
            final String documentId = pocData[0].documentId;
            final Report report = new Report(documentType, documentId, vars.getLanguage(),
                templateId, multiReports, OutputTypeEnum.DEFAULT);
            o.put("templateId", templateId);
            o.put("subject", report.getEmailDefinition().getSubject());
            o.put("body", report.getEmailDefinition().getBody());
            if (!multiReports) {
              o.put("filename", report.getFilename());
            }
            reports = new HashMap<String, Report>();
            reports.put(documentId, report);
            vars.setSessionObject(sessionValuePrefix + ".Documents", reports);

          } catch (Exception e) {
            log4j.error("Error in change template ajax", e);
            o = new JSONObject();
            try {
              o.put("error", true);
            } catch (JSONException e1) {
              log4j.error("Error in change template ajax", e1);
            }
          }

          response.setContentType("application/json; charset=UTF-8");
          final PrintWriter out = response.getWriter();
          out.println(o.toString());
          out.close();
        } else if (vars.commandIn("UPDATE_EMAILCONFIG")) {
          JSONObject o = new JSONObject();
          try {
            String currentEmailConfigId = vars.getStringParameter("emailConfigList");
            EmailTemplate emailTemplate = OBDal.getInstance()
                .get(EmailTemplate.class, currentEmailConfigId);
            o.put("subject", emailTemplate.getSubject());
            o.put("body", emailTemplate.getBody());

          } catch (Exception e) {
            log4j.error("Error in change template ajax", e);
            o = new JSONObject();
            try {
              o.put("error", true);
            } catch (JSONException e1) {
              log4j.error("Error in change template ajax", e1);
            }
          }

          response.setContentType("application/json; charset=UTF-8");
          final PrintWriter out = response.getWriter();
          out.println(o.toString());
          out.close();
        }

        pageError(response);
      }
    } catch (Exception e) {
      // Catching the exception here instead of throwing it to HSAS because this is used in multi
      // part request making the mechanism to detect popup not to work.
      log4j.error("Error captured: ", e);
      bdErrorGeneralPopUp(request, response, "Error",
          Utility.translateError(this, vars, vars.getLanguage(), e.getMessage()).getMessage());
    }
  }

  /**
   * Retrieves the document number for a specified document type and document ID.
   *
   * @param documentType
   *     the type of the document for which to retrieve the document number
   * @param jsonParams
   *     a JSONObject containing the parameters, including the document ID
   * @return the document number as a String
   * @throws JSONException
   *     if there is an error parsing the document ID from jsonParams
   */
  private static String getDocumentNo(DocumentType documentType, JSONObject jsonParams) throws JSONException {
    String query = String.format("SELECT documentNo FROM %s WHERE %s_id = :id", documentType.getTableName(),
        documentType.getTableName());
    return (String) OBDal.getInstance().getSession().createNativeQuery(query).setParameter("id",
        jsonParams.getString(DOCUMENT_ID)).uniqueResult();
  }

  public void printReports(HttpServletResponse response, Collection<JasperPrint> jrPrintReports,
      Collection<Report> reports, boolean directPrint) {
    ServletOutputStream os = null;
    String filename = "";
    Map<Object, Object> parameters = new HashMap<Object, Object>();
    try {
      os = response.getOutputStream();
      response.setContentType("application/pdf");

      if (!multiReports && !archivedReports) {
        for (Report report : reports) {
          filename = report.getFilename();
        }
        if (!directPrint) {
          response.setHeader("Content-disposition", "attachment" + "; filename=" + filename);
          for (JasperPrint jasperPrint : jrPrintReports) {
            ReportingUtils.saveReport(jasperPrint, ExportType.PDF, parameters, os);
          }
        } else {
          response.setContentType("text/html");
          File file = Files
              .createTempFile(Paths.get(globalParameters.strFTPDirectory), filename + "-", ".pdf")
              .toFile();
          for (JasperPrint jasperPrint : jrPrintReports) {
            ReportingUtils.saveReport(jasperPrint, ExportType.PDF, parameters, file);
          }
          doDirectPrint(os, file.getName());
        }
      } else {
        concatReport(reports.toArray(new Report[] {}), jrPrintReports, response, directPrint);
      }
      for (Report report : reports) {
        switch (report.getDocumentType()) {
          case SALESORDER:
            PrintControllerData.updateOrderDatePrinted(this, report.getDocumentId());
          default:
            break;
        }
      }
    } catch (IOException e) {
      log4j.error(e.getMessage());
    } catch (JRException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        os.close();
        response.flushBuffer();
      } catch (IOException e) {
        log4j.error(e.getMessage(), e);
      } finally {
        try {
          for (Report report : reports) {
            // Delete temporal reports generated for the returned report in case they have been
            // attached also
            File file = new File(report.getTargetLocation());
            if (file.exists() && !file.isDirectory()) {
              file.delete();
            }
          }
        } catch (IOException e) {
          log4j.error("Error deleting temporal reports", e);
        }
      }
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
    printReports(response, jrPrintReports, reports, false);
  }

  private void concatReport(Report[] reports, Collection<JasperPrint> jrPrintReports,
      HttpServletResponse response, boolean directPrint) {
    try {
      String filename = "";
      boolean createBookmarks = true;
      SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
      if (reports.length == 1) {
        filename = reports[0].getFilename();
        createBookmarks = false;
      } else if (reports.length > 1) {
        filename = reports[0].getTemplateInfo().getReportFilename();
        filename = filename.replaceAll("@our_ref@", "");
        filename = filename.replaceAll("@cus_ref@", "");
        filename = filename.replaceAll(" ", "_");
        filename = filename.replaceAll("-", "");
        filename = filename + ".pdf";
      }
      if (!directPrint) {
        response.setHeader("Content-disposition", "attachment" + "; filename=" + filename);
        ReportingUtils.concatPDFReport(new ArrayList<>(jrPrintReports), createBookmarks,
            response.getOutputStream(), configuration);
      } else {
        response.setContentType("text/html");
        Path path = Files.createTempFile(Paths.get(globalParameters.strFTPDirectory),
            filename + "-", ".pdf");
        try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE)) {
          ReportingUtils.concatPDFReport(new ArrayList<>(jrPrintReports), createBookmarks,
              outputStream, configuration);
        }
        doDirectPrint(response.getOutputStream(), path.toFile().getName());
      }
    } catch (Exception e) {
      log4j.error(e);
    }
  }

  public Report buildReport(HttpServletResponse response, VariablesSecureApp vars,
      String strDocumentId, final ReportManager reportManager, DocumentType documentType,
      OutputTypeEnum outputType) {
    return buildReport(response, vars, strDocumentId, reportManager, documentType, outputType,
        "default");
  }

  public Report buildReport(HttpServletResponse response, VariablesSecureApp vars,
      String strDocumentId, final ReportManager reportManager, DocumentType documentType,
      OutputTypeEnum outputType, String templateId) {
    String localStrDocumentId = strDocumentId;
    Report report = null;
    if (localStrDocumentId != null) {
      localStrDocumentId = localStrDocumentId.replaceAll("\\(|\\)|'", "");
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
      localStrDocumentId = localStrDocumentId.replaceAll("\\(|\\)|'", "");
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
          attachedFile.delete();
        }
      }
    } else {
      if (log4j.isDebugEnabled()) {
        log4j.debug("Document is not attached.");
      }
    }
  }

  private void seekAndDestroy(List<AttachContent> attachments, String documentToDelete) {
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
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\r\n");
    xmlDocument.setParameter("language", vars.getLanguage());
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("description", "");
    xmlDocument.setParameter("help", "");
    xmlDocument.setParameter("isDirectPDF", "isDirectPDF = " + strIsDirectPDF + ";\r\n");
    xmlDocument.setParameter("isDirectAttach", "isDirectAttach = " + strIsDirectAttach + ";\r\n");
    response.setContentType("text/html; charset=UTF-8");
    final PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  void createEmailOptionsPage(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, DocumentType documentType, String strDocumentId,
      Map<String, Report> reports, HashMap<String, Boolean> checks) {
    createEmailOptionsPage(request, response, vars, documentType, strDocumentId, reports, checks);
  }

  void createEmailOptionsPage(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, DocumentType documentType, String strDocumentId,
      Map<String, Report> reports, HashMap<String, Boolean> checks, String fullDocumentIdentifier)
      throws IOException, ServletException, ReportingException {
    boolean hasMultipleEmailConfigurations = false;
    XmlDocument xmlDocument = null;
    PocData[] pocData = EmailUtilities.getContactDetails(documentType, strDocumentId, this);
    @SuppressWarnings("unchecked")
    List<AttachContent> attachments = (List<AttachContent>) request.getSession()
        .getAttribute("files");

    final String[] hiddenTags = getHiddenTags(pocData, attachments, vars, checks);
    if (hiddenTags != null) {
      xmlDocument = xmlEngine
          .readXmlTemplate("org/openbravo/erpCommon/utility/reporting/printing/EmailOptions",
              hiddenTags)
          .createXmlDocument();
    } else {
      xmlDocument = xmlEngine
          .readXmlTemplate("org/openbravo/erpCommon/utility/reporting/printing/EmailOptions")
          .createXmlDocument();
    }

    xmlDocument.setParameter("strDocumentId", strDocumentId);

    boolean isTheFirstEntry = false;
    if (attachments == null) {
      attachments = new ArrayList<>(0);
      isTheFirstEntry = true;
    }

    if (vars.getMultiFile("inpFile") != null
        && !vars.getMultiFile("inpFile").getName().equals("")) {
      final AttachContent content = new AttachContent();
      final FileItem file1 = vars.getMultiFile("inpFile");
      content.setFileName(pocData[0].ourreference.replace('/', '_') + '-'
          + Utility.formatDate(new Date(), "yyyyMMdd-HHmmss") + '.' + file1.getName());
      content.setFileItem(file1);
      content.setId(Utility.formatDate(new Date(), "yyyyMMdd-HHmmss") + '.' + file1.getName());
      content.visible = "hidden";
      if ("Y".equals(vars.getStringParameter("inpArchive"))) {
        content.setSelected("true");
      }
      attachments.add(content);
      request.getSession().setAttribute("files", attachments);

    }

    if ("yes".equals(vars.getStringParameter("closed"))) {
      xmlDocument.setParameter("closed", "yes");
      request.getSession().removeAttribute("files");
    }

    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\r\n");
    xmlDocument.setParameter("language", vars.getLanguage());
    xmlDocument.setParameter("theme", vars.getTheme());

    EmailDefinition emailDefinition = null;
    try {
      if (moreThanOneLanguageDefined(reports) && hasDifferentBpLanguages(reports)) {
        // set multiple email configurations
        List<EmailDefinition> emailDef = new ArrayList<EmailDefinition>();
        Map<String, EmailDefinition> emailDefinitions = reports.values()
            .iterator()
            .next()
            .getEmailDefinitions();
        Iterator<Entry<String, EmailDefinition>> entries = emailDefinitions.entrySet().iterator();
        while (entries.hasNext()) {
          Map.Entry<String, EmailDefinition> entry = entries.next();
          emailDef.add(entry.getValue());
        }
        emailDefinition = reports.values()
            .iterator()
            .next()
            .getTemplateInfo()
            .get_DefaultEmailDefinition();
        String emailDefinitionsComboHtml = getOptionsList(emailDef, emailDefinition.getId(), false);
        xmlDocument.setParameter("reportEmailConfig", emailDefinitionsComboHtml);
        hasMultipleEmailConfigurations = true;
      } else {
        emailDefinition = reports.values().iterator().next().getEmailDefinition();
        hasMultipleEmailConfigurations = false;
      }
    } catch (final OBException exception) {
      final OBError on = new OBError();
      on.setMessage(Utility.messageBD(this, "EmailConfiguration", vars.getLanguage()));
      on.setTitle(Utility.messageBD(this, "Info", vars.getLanguage()));
      on.setType("info");
      final String tabId = vars.getSessionValue("inpTabId");
      vars.getStringParameter("tab");
      vars.setMessage(tabId, on);
      vars.getRequestGlobalVariable("inpTabId", "AttributeSetInstance.tabId");
      printPageClosePopUpAndRefreshParent(response, vars);
    } catch (ReportingException e) {
      log4j.error(e);
    }

    String fromEmail = null;
    String fromEmailId = null;

    OBContext.setAdminMode(true);
    try {
      OBCriteria<EmailServerConfiguration> mailConfigCriteria = OBDal.getInstance()
          .createCriteria(EmailServerConfiguration.class);
      mailConfigCriteria.addOrderBy("client.id", false);
      final List<EmailServerConfiguration> mailConfigList = mailConfigCriteria.list();

      if (mailConfigList.size() == 0) {
        throw new ServletException("No Poc configuration found for this client.");
      }

      EmailServerConfiguration mailConfig = EmailUtils
          .getEmailConfiguration(OBDal.getInstance().get(Organization.class, vars.getOrg()));

      if (mailConfig == null) {
        throw new ServletException(
            "No sender defined: Please go to client configuration to complete the email configuration.");
      }

      fromEmail = mailConfig.getSmtpServerSenderAddress();
      fromEmailId = mailConfig.getId();
    } finally {
      OBContext.restorePreviousMode();
    }

    // Get additional document information
    String draftDocumentIds = "";
    final AttachContent attachedContent = new AttachContent();
    final boolean onlyOneAttachedDoc = onlyOneAttachedDocs(reports);
    final Map<String, PocData> customerMap = new HashMap<String, PocData>();
    final Map<String, PocData> salesRepMap = new HashMap<String, PocData>();
    final List<AttachContent> clonedAttachemnts = new ArrayList<>();
    boolean allTheDocsCompleted = true;
    for (final PocData documentData : pocData) {
      // Map used to count the different users

      final String customer = documentData.contactEmail;
      getEnvironentInformation(pocData, checks);
      if (checks.get("moreThanOneDoc")) {
        if (customer == null || customer.length() == 0) {
          final OBError on = new OBError();
          on.setMessage(Utility.messageBD(this, "NoContact", vars.getLanguage())
              .replace("@docNum@", documentData.ourreference));

          on.setTitle(Utility.messageBD(this, "Info", vars.getLanguage()));
          on.setType("info");
          final String tabId = vars.getSessionValue("inpTabId");
          vars.getStringParameter("tab");
          vars.setMessage(tabId, on);
          vars.getRequestGlobalVariable("inpTabId", "AttributeSetInstance.tabId");
          printPageClosePopUpAndRefreshParent(response, vars);
        } else if (documentData.contactEmail == null || documentData.contactEmail.equals("")) {
          final OBError on = new OBError();
          on.setMessage(Utility.messageBD(this, "NoEmail", vars.getLanguage())
              .replace("@customer@", documentData.contactName));
          on.setTitle(Utility.messageBD(this, "Info", vars.getLanguage()));
          on.setType("info");
          final String tabId = vars.getSessionValue("inpTabId");
          vars.getStringParameter("tab");
          vars.setMessage(tabId, on);
          vars.getRequestGlobalVariable("inpTabId", "AttributeSetInstance.tabId");
          printPageClosePopUpAndRefreshParent(response, vars);
        }
      }

      if (!customerMap.containsKey(customer)) {
        customerMap.put(customer, documentData);
      }

      final String salesRep = documentData.salesrepEmail;

      boolean moreThanOnesalesRep = checks.get("moreThanOnesalesRep").booleanValue();
      if (moreThanOnesalesRep) {
        if (salesRep == null || salesRep.length() == 0) {
          final OBError on = new OBError();
          on.setMessage(Utility.messageBD(this, "NoSenderDocument", vars.getLanguage()));
          on.setTitle(Utility.messageBD(this, "Info", vars.getLanguage()));
          on.setType("info");
          final String tabId = vars.getSessionValue("inpTabId");
          vars.getStringParameter("tab");
          vars.setMessage(tabId, on);
          vars.getRequestGlobalVariable("inpTabId", "AttributeSetInstance.tabId");
          printPageClosePopUpAndRefreshParent(response, vars);
        } else if (documentData.salesrepEmail == null || documentData.salesrepEmail.equals("")) {
          final OBError on = new OBError();
          on.setMessage(Utility.messageBD(this, "NoEmailSender", vars.getLanguage())
              .replace("@salesRep@", documentData.salesrepName));
          on.setTitle(Utility.messageBD(this, "Info", vars.getLanguage()));
          on.setType("info");
          final String tabId = vars.getSessionValue("inpTabId");
          vars.getStringParameter("tab");
          vars.setMessage(tabId, on);
          vars.getRequestGlobalVariable("inpTabId", "AttributeSetInstance.tabId");
          printPageClosePopUpAndRefreshParent(response, vars);
        }
      }

      if (!salesRepMap.containsKey(salesRep)) {
        salesRepMap.put(salesRep, documentData);
      }

      final Report report = reports.get(documentData.documentId);
      // All ids of documents in draft are passed to the web client
      if (report.isDraft()) {
        if (draftDocumentIds.length() > 0) {
          draftDocumentIds += ",";
        }
        draftDocumentIds += report.getDocumentId();
        allTheDocsCompleted = false;
      }

      // Fill the report location
      final String reportFilename = report.getContextSubFolder() + report.getFilename();
      documentData.reportLocation = request.getContextPath() + "/" + reportFilename + "?documentId="
          + documentData.documentId;
      if (log4j.isDebugEnabled()) {
        log4j.debug(" Filling report location with: " + documentData.reportLocation);
      }

      if (onlyOneAttachedDoc) {
        attachedContent.setDocName(report.getFilename());
        attachedContent.setVisible("checkbox");
        clonedAttachemnts.add(attachedContent);
      }

    }
    if (!allTheDocsCompleted) {
      final OBError on = new OBError();
      on.setMessage(Utility.messageBD(this, "ErrorIncompleteDocuments", vars.getLanguage()));
      on.setTitle(Utility.messageBD(this, "ErrorSendingEmail", vars.getLanguage()));
      on.setType("Error");
      final String tabId = vars.getSessionValue("inpTabId");
      vars.getStringParameter("tab");
      vars.setMessage(tabId, on);
      vars.getRequestGlobalVariable("inpTabId", "AttributeSetInstance.tabId");
      printPageClosePopUpAndRefreshParent(response, vars);
    }

    final int numberOfCustomers = customerMap.size();
    final int numberOfSalesReps = salesRepMap.size();

    if (!onlyOneAttachedDoc && isTheFirstEntry) {
      if (numberOfCustomers > 1) {
        attachedContent.setDocName(String.valueOf(
            reports.size() + " Documents to " + String.valueOf(numberOfCustomers) + " Customers"));
        attachedContent.setVisible("checkbox");

      } else {
        attachedContent.setDocName(String.valueOf(reports.size() + " Documents"));
        attachedContent.setVisible("checkbox");

      }
      clonedAttachemnts.add(attachedContent);
    }

    if (!clonedAttachemnts.isEmpty()) {
      final AttachContent[] data = attachments.toArray(new AttachContent[attachments.size()]);
      final AttachContent[] data2 = clonedAttachemnts
          .toArray(new AttachContent[clonedAttachemnts.size()]);
      xmlDocument.setData("structure2", data2);
      xmlDocument.setData("structure1", data);
    }
    if (pocData.length >= 1) {
      xmlDocument.setData("reportEmail", "liststructure",
          reports.get((pocData[0].documentId)).getTemplate());
    }

    if (log4j.isDebugEnabled()) {
      log4j.debug("Documents still in draft: " + draftDocumentIds);
    }
    xmlDocument.setParameter("draftDocumentIds", draftDocumentIds);

    final PocData[] currentUserInfo = PocData.getContactDetailsForUser(this, vars.getUser());
    final String userName = currentUserInfo[0].userName;
    final String userEmail = currentUserInfo[0].userEmail;
    String bccEmail = "";
    String bccName = "";
    if (userEmail != null && userEmail.length() > 0) {
      bccEmail = userEmail;
      bccName = userName;
    }

    if (vars.commandIn("ADD") || vars.commandIn("DEL")) {
      xmlDocument.setParameter("fromEmailId", vars.getStringParameter("fromEmailId"));
      xmlDocument.setParameter("fromEmail", vars.getStringParameter("fromEmail"));
      xmlDocument.setParameter("toEmail", vars.getStringParameter("toEmail"));
      xmlDocument.setParameter("toEmailOrig", vars.getStringParameter("toEmailOrig"));
      xmlDocument.setParameter("ccEmail", vars.getStringParameter("ccEmail"));
      xmlDocument.setParameter("ccEmailOrig", vars.getStringParameter("ccEmailOrig"));
      xmlDocument.setParameter("bccEmail", vars.getStringParameter("bccEmail"));
      xmlDocument.setParameter("bccEmailOrig", vars.getStringParameter("bccEmailOrig"));
      xmlDocument.setParameter("replyToEmail", vars.getStringParameter("replyToEmail"));
      xmlDocument.setParameter("replyToEmailOrig", vars.getStringParameter("replyToEmailOrig"));
      xmlDocument.setParameter("emailSubject", vars.getStringParameter("emailSubject"));
      xmlDocument.setParameter("emailBody", vars.getStringParameter("emailBody"));
    } else {
      xmlDocument.setParameter("fromEmailId", fromEmailId);
      xmlDocument.setParameter("fromEmail", fromEmail);
      xmlDocument.setParameter("toEmail", pocData[0].contactEmail);
      xmlDocument.setParameter("toEmailOrig", pocData[0].contactEmail);
      xmlDocument.setParameter("ccEmail", "");
      xmlDocument.setParameter("ccEmailOrig", "");
      xmlDocument.setParameter("bccEmail", bccEmail);
      xmlDocument.setParameter("bccEmailOrig", bccEmail);
      xmlDocument.setParameter("replyToEmail", pocData[0].salesrepEmail);
      xmlDocument.setParameter("replyToEmailOrig", pocData[0].salesrepEmail);
      xmlDocument.setParameter("emailSubject", emailDefinition.getSubject());
      xmlDocument.setParameter("emailBody", emailDefinition.getBody());
    }
    xmlDocument.setParameter("inpArchive", vars.getStringParameter("inpArchive"));
    xmlDocument.setParameter("fromName", "");
    xmlDocument.setParameter("toName", pocData[0].contactName);
    xmlDocument.setParameter("ccName", "");
    xmlDocument.setParameter("bccName", bccName);
    xmlDocument.setParameter("replyToName", pocData[0].salesrepName);
    xmlDocument.setParameter("inpArchive", vars.getStringParameter("inpArchive"));
    xmlDocument.setParameter("multCusCount", String.valueOf(numberOfCustomers));
    xmlDocument.setParameter("multSalesRepCount", String.valueOf(numberOfSalesReps));
    if (!hasMultipleEmailConfigurations) {
      xmlDocument.setParameter("useDefault", "Y");
    }
    if (differentDocTypes.size() > 1) {
      xmlDocument.setParameter("multiDocType", "Y");
    }

    vars.setSessionObject("pocData" + fullDocumentIdentifier, pocData);
    response.setContentType("text/html; charset=UTF-8");
    final PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private String getOptionsList(List<EmailDefinition> emailDef, String selectedValue,
      boolean isMandatory) {
    StringBuilder strOptions = new StringBuilder();
    if (!isMandatory) {
      strOptions.append("<option value=\"\"></option>");
    }
    for (EmailDefinition obObject : emailDef) {
      strOptions.append("<option value=\"").append(obObject.getId()).append("\"");
      if (obObject.getId().equals(selectedValue)) {
        strOptions.append(" selected=\"selected\"");
      }
      strOptions.append(">");
      strOptions.append(BasicUtility
          .formatMessageBDToHtml(obObject.getSubject() + " - " + obObject.getLanguage()));
      strOptions.append("</option>");
    }
    return strOptions.toString();
  }

  private boolean moreThanOneLanguageDefined(Map<String, Report> reports)
      throws ReportingException {
    boolean hasMoreThanOneLanguage = false;
    @SuppressWarnings("rawtypes")
    Iterator itRep = reports.values().iterator();
    while (itRep.hasNext()) {
      Report report = (Report) itRep.next();
      if (report.getEmailDefinitions().size() > 1) {
        hasMoreThanOneLanguage = true;
        break;
      }
    }
    return hasMoreThanOneLanguage;
  }

  private boolean hasDifferentBpLanguages(Map<String, Report> reports) throws ReportingException {
    boolean hasMoreThanOneLanguage = false;
    @SuppressWarnings("rawtypes")
    Iterator itRep = reports.values().iterator();
    Language currentLanguage = null;
    while (itRep.hasNext()) {
      Report report = (Report) itRep.next();
      String bPartnerId = report.getBPartnerId();
      BusinessPartner businessPartner = OBDal.getInstance().get(BusinessPartner.class, bPartnerId);
      Language language = businessPartner.getLanguage();
      if (currentLanguage == null) {
        currentLanguage = language;
      } else if (currentLanguage != language) {
        hasMoreThanOneLanguage = true;
      }
    }
    return hasMoreThanOneLanguage;
  }

  private void getEnvironentInformation(PocData[] pocData, HashMap<String, Boolean> checks) {
    final Map<String, PocData> customerMap = new HashMap<String, PocData>();
    final Map<String, PocData> salesRepMap = new HashMap<String, PocData>();
    int docCounter = 0;
    checks.put("moreThanOneDoc", false);
    for (final PocData documentData : pocData) {
      // Map used to count the different users
      docCounter++;
      final String customer = documentData.contactEmail;
      final String salesRep = documentData.salesrepEmail;
      if (!customerMap.containsKey(customer)) {
        customerMap.put(customer, documentData);
      }
      if (!salesRepMap.containsKey(salesRep)) {
        salesRepMap.put(salesRep, documentData);
      }
    }
    if (docCounter > 1) {
      checks.put("moreThanOneDoc", true);
    }
    boolean moreThanOneCustomer = (customerMap.size() > 1);
    boolean moreThanOnesalesRep = (salesRepMap.size() > 1);
    checks.put("moreThanOneCustomer", Boolean.valueOf(moreThanOneCustomer));
    checks.put("moreThanOnesalesRep", Boolean.valueOf(moreThanOnesalesRep));
  }

  /**
   * @author gmauleon
   */
  private String[] getHiddenTags(PocData[] pocData, List<AttachContent> attachedContent,
      VariablesSecureApp vars, HashMap<String, Boolean> checks) {
    String[] discard;
    final Map<String, PocData> customerMap = new HashMap<String, PocData>();
    final Map<String, PocData> salesRepMap = new HashMap<String, PocData>();
    for (final PocData documentData : pocData) {
      // Map used to count the different users

      final String customer = documentData.contactEmail;
      final String salesRep = documentData.salesrepEmail;
      if (!customerMap.containsKey(customer)) {
        customerMap.put(customer, documentData);
      }
      if (!salesRepMap.containsKey(salesRep)) {
        salesRepMap.put(salesRep, documentData);
      }
    }
    boolean moreThanOneCustomer = (customerMap.size() > 1);
    boolean moreThanOnesalesRep = (salesRepMap.size() > 1);
    checks.put("moreThanOneCustomer", Boolean.valueOf(moreThanOneCustomer));
    checks.put("moreThanOnesalesRep", Boolean.valueOf(moreThanOnesalesRep));

    // check the number of customer and the number of
    // sales Rep. to choose one of the 3 possibilities
    // 1.- n customer n sales rep (hide "To" and "Reply-to" inputs)
    // 2.- n customers 1 sales rep (hide "To" input)
    // 3.- 1 customer n sales rep (hide Reply-to inputs)
    // 4.- Otherwise show both
    if (moreThanOneCustomer && moreThanOnesalesRep) {
      discard = new String[] { "to", "to_bottomMargin", "replyTo", "replyTo_bottomMargin" };
    } else if (moreThanOneCustomer) {
      discard = new String[] { "to", "to_bottomMargin", "multSalesRep", "multSalesRepCount" };
    } else if (moreThanOnesalesRep) {
      discard = new String[] { "replyTo", "replyTo_bottomMargin" };
    } else {
      discard = new String[] { "multipleCustomer", "multipleCustomer_bottomMargin" };
    }

    // check the templates
    if (differentDocTypes.size() > 1) { // the templates selector shouldn't
      // appear
      final String[] discardAux = new String[discard.length + 1];
      for (int i = 0; i < discard.length; i++) {
        discardAux[i] = discard[i];
      }
      discardAux[discard.length] = "discardSelect";
      return discardAux;
    }
    if (attachedContent == null && vars.getMultiFile("inpFile") == null) {
      final String[] discardAux = new String[discard.length + 1];
      for (int i = 0; i < discard.length; i++) {
        discardAux[i] = discard[i];
      }
      discardAux[discard.length] = "view";
      return discardAux;
    }
    return discard;
  }

  private boolean onlyOneAttachedDocs(Map<String, Report> reports) {
    if (reports.size() == 1) {
      return true;
    } else {
      return false;
    }

  }

  void createPrintStatusPage(HttpServletResponse response, VariablesSecureApp vars,
      int nrOfEmailsSend) throws IOException, ServletException {
    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/utility/reporting/printing/PrintStatus")
        .createXmlDocument();
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\r\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("language", vars.getLanguage());
    xmlDocument.setParameter("nrOfEmailsSend", "" + nrOfEmailsSend);

    response.setContentType("text/html; charset=UTF-8");
    final PrintWriter out = response.getWriter();

    out.println(xmlDocument.print());
    out.close();
  }

  /**
   * 
   * @param documentIds
   * @return returns a comma separated and quoted string of documents id's. useful to sql querys
   */
  private String getComaSeparatedString(String[] documentIds) {
    String result = new String("(");
    for (int index = 0; index < documentIds.length; index++) {
      final String documentId = documentIds[index];
      if (index + 1 == documentIds.length) {
        result = result + "'" + documentId + "')";
      } else {
        result = result + "'" + documentId + "',";
      }

    }
    return result;
  }

  /**
   * Returns an array of document's ID ordered by Document No ASC
   * 
   * @param documentType
   * @param documentIds
   *          array of document's ID without order
   * @return List of ordered IDs
   * @throws ServletException
   */
  private String[] orderByDocumentNo(DocumentType documentType, String[] documentIds)
      throws ServletException {
    String strTable = documentType.getTableName();

    StringBuffer strIds = new StringBuffer();
    strIds.append("'");
    for (int i = 0; i < documentIds.length; i++) {
      if (i > 0) {
        strIds.append("', '");
      }
      strIds.append(documentIds[i]);
    }
    strIds.append("'");

    PrintControllerData[] printControllerData;
    String documentIdsOrdered[] = new String[documentIds.length];
    int i = 0;
    if (strTable.equals("C_INVOICE")) {
      printControllerData = PrintControllerData.selectInvoices(this, strIds.toString());
      for (PrintControllerData docID : printControllerData) {
        documentIdsOrdered[i++] = docID.getField("Id");
      }
    } else if (strTable.equals("C_ORDER")) {
      printControllerData = PrintControllerData.selectOrders(this, strIds.toString());
      for (PrintControllerData docID : printControllerData) {
        documentIdsOrdered[i++] = docID.getField("Id");
      }
    } else if (strTable.equals("FIN_PAYMENT")) {
      printControllerData = PrintControllerData.selectPayments(this, strIds.toString());
      for (PrintControllerData docID : printControllerData) {
        documentIdsOrdered[i++] = docID.getField("Id");
      }
    } else {
      return documentIds;
    }

    return documentIdsOrdered;
  }

  private boolean isDirectPrint(VariablesSecureApp vars) {
    OBContext context = OBContext.getOBContext();
    String preferenceValue = "";
    try {
      OBContext.setAdminMode(true);
      String tabId = vars.getSessionValue("inpTabId");
      Tab tab = OBDal.getInstance().get(Tab.class, tabId);
      try {
        preferenceValue = Preferences.getPreferenceValue("DirectPrint", true,
            context.getCurrentClient(), context.getCurrentOrganization(), context.getUser(),
            context.getRole(), tab.getWindow());
      } catch (PropertyException e) {
        return false;
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return Preferences.YES.equals(preferenceValue);
  }

  @Override
  public String getServletInfo() {
    return "Servlet that processes the print action";
  } // End of getServletInfo() method
}
