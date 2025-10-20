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
 * All portions are Copyright (C) 2001-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility.reporting;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;

import jakarta.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.erpCommon.utility.reporting.TemplateInfo.EmailDefinition;
import org.openbravo.service.db.DalConnectionProvider;

@SuppressWarnings("serial")
public class Report implements Serializable {
  public String getOrgId() {
    return orgId;
  }

  public void setOrgId(String orgId) {
    this.orgId = orgId;
  }

  public enum OutputTypeEnum {
    DEFAULT, PRINT, ARCHIVE, EMAIL
  }

  public OutputTypeEnum outputType = OutputTypeEnum.DEFAULT;

  private static Logger log4j = LogManager.getLogger();

  private DocumentType _DocumentType;
  private String _DocumentId; // Order Id, invoice id, etc.
  private String _DocumentStatus;
  private String _OurReference;
  private String _CusReference;
  private String _SalNam;
  private String _BPartnerId;
  private String _BPartnerName;
  private String _BPartnerLanguage;
  private String _DocDate; // Date Ordered, Date Invoices, etc.
  private String _MinDueDate; // Useful for Orders and Invoices
  private String _MaxDueDate; // Useful for Orders and Invoices
  private String _DocDescription;
  private String _ContactName;
  private String _Filename;
  private File _targetDirectory;
  private boolean _isAttached;
  private String docTypeId;
  private String orgId;
  private boolean deleteReport = false;
  private boolean multiReports = false;
  private String checkSalesOrder;

  public String getDocTypeId() {
    return docTypeId;
  }

  public void setDocTypeId(String docTypeId) {
    this.docTypeId = docTypeId;
  }

  private TemplateInfo templateInfo;

  public Report(DocumentType documentType, String documentId, String strLanguage, String templateId,
      boolean multiReport, OutputTypeEnum outputTypeString)
      throws ReportingException, ServletException {
    this(new DalConnectionProvider(false), documentType, documentId, strLanguage, templateId,
        multiReport, outputTypeString);
  }

  public Report(ConnectionProvider connectionProvider, DocumentType documentType, String documentId,
      String strLanguage, String templateId, boolean multiReport, OutputTypeEnum outputTypeString)
      throws ReportingException, ServletException {
    _DocumentType = documentType;
    _DocumentId = documentId;
    outputType = outputTypeString;
    ReportData[] reportData = null;

    switch (_DocumentType) {
      case QUOTATION: // Retrieve quotation information
        reportData = ReportData.getOrderInfo(connectionProvider, documentId);
        break;
      case SALESORDER: // Retrieve order information
        reportData = ReportData.getOrderInfo(connectionProvider, documentId);
        break;

      case SALESINVOICE: // Retrieve invoice information
        reportData = ReportData.getInvoiceInfo(connectionProvider, documentId);
        break;

      case SHIPMENT: // Retrieve shipment information
        reportData = ReportData.getShipmentInfo(connectionProvider, documentId);
        break;

      case PAYMENT: // Retrieve payment information
        reportData = ReportData.getPaymentInfo(connectionProvider, documentId);
        break;

      default:
        throw new ReportingException(
            Utility.messageBD(connectionProvider, "UnknownDocumentType", strLanguage)
                + _DocumentType);
    }

    multiReports = multiReport;
    if (reportData.length == 1) {
      checkSalesOrder = reportData[0].getField("isSalesOrderTransaction");
      orgId = reportData[0].getField("ad_Org_Id");
      docTypeId = reportData[0].getField("docTypeTargetId");

      _OurReference = reportData[0].getField("ourreference");
      _CusReference = reportData[0].getField("cusreference");
      _SalNam = reportData[0].getField("salnam");
      _BPartnerId = reportData[0].getField("bpartner_id");
      _BPartnerName = reportData[0].getField("bpartner_name");
      _BPartnerLanguage = reportData[0].getField("bpartner_language");
      _DocumentStatus = reportData[0].getField("docstatus");
      _DocDate = reportData[0].getField("docdate");
      _MinDueDate = reportData[0].getField("minduedate");
      _MaxDueDate = reportData[0].getField("maxduedate");
      _DocDescription = reportData[0].getField("docdesc");
      _ContactName = reportData[0].getField("contact_name");
      templateInfo = new TemplateInfo(connectionProvider, docTypeId, orgId, strLanguage,
          templateId);

      _Filename = generateReportFileName();
      _targetDirectory = null;
    } else {
      throw new ReportingException(
          Utility.messageBD(connectionProvider, "NoDataReport", strLanguage) + documentId);
    }

  }

  public void setTemplateInfo(TemplateInfo templateInfo) {
    this.templateInfo = templateInfo;
  }

  private String generateReportFileName() {
    // Generate the target report filename
    final String dateStamp = Utility.formatDate(new Date(), "yyyyMMdd-HHmmss");
    String reportFilename = templateInfo.getReportFilename();
    reportFilename = reportFilename.replaceAll("@our_ref@",
        Matcher.quoteReplacement(_OurReference));
    reportFilename = reportFilename.replaceAll("@cus_ref@",
        Matcher.quoteReplacement(_CusReference));
    reportFilename = reportFilename.replaceAll("@cus_nam@", Matcher.quoteReplacement(_ContactName));
    reportFilename = reportFilename.replaceAll("@bp_nam@", Matcher.quoteReplacement(_BPartnerName));
    reportFilename = reportFilename.replaceAll("@doc_date@", Matcher.quoteReplacement(_DocDate));
    reportFilename = reportFilename.replaceAll("@doc_nextduedate@",
        Matcher.quoteReplacement(_MinDueDate));
    reportFilename = reportFilename.replaceAll("@doc_lastduedate@",
        Matcher.quoteReplacement(_MaxDueDate));
    // reportFilename = reportFilename.replaceAll("@doc_desc@",
    // Matcher.quoteReplacement(_DocDescription)); // Too long
    if (checkSalesOrder.equalsIgnoreCase("y")
        && !_DocumentType.toString().equalsIgnoreCase("PAYMENT")) {
      reportFilename = reportFilename.replaceAll("@sal_nam@", Matcher.quoteReplacement(_SalNam));
    } else {
      reportFilename = reportFilename.replaceAll("@sal_nam@", "");
    }
    // only characters, numbers and "." are accepted. Others will be changed for "_"
    reportFilename = reportFilename.replaceAll("[^A-Za-z0-9\\.]", "_");
    reportFilename = reportFilename + "." + dateStamp + ".pdf";
    if (log4j.isDebugEnabled()) {
      log4j.debug("target report filename: " + reportFilename);
    }

    if (multiReports && outputType.equals(OutputTypeEnum.PRINT)) {
      reportFilename = UUID.randomUUID().toString() + "_" + reportFilename;
      setDeleteable(true);
    }

    return reportFilename;
  }

  public String getContextSubFolder() throws ServletException {
    return _DocumentType.getContextSubFolder();
  }

  public DocumentType getDocumentType() {
    return _DocumentType;
  }

  public String getDocumentId() {
    return _DocumentId;
  }

  public TemplateInfo getTemplateInfo() {
    return templateInfo;
  }

  public EmailDefinition getEmailDefinition() throws ReportingException {
    return templateInfo.getEmailDefinition(_BPartnerLanguage);
  }

  public EmailDefinition getDefaultEmailDefinition() throws ReportingException {
    return templateInfo.get_DefaultEmailDefinition();
  }

  public Map<String, EmailDefinition> getEmailDefinitions() throws ReportingException {
    return templateInfo.getEmailDefinitions();
  }

  public String getOurReference() {
    return _OurReference;
  }

  public String getCusReference() {
    return _CusReference;
  }

  public String getDocumentStatus() {
    return _DocumentStatus;
  }

  public String getBPartnerId() {
    return _BPartnerId;
  }

  public String getBPName() {
    return _BPartnerName;
  }

  public String getDocDate() {
    return _DocDate;
  }

  public String getMinDueDate() {
    return _MinDueDate;
  }

  public String getMaxDueDate() {
    return _MaxDueDate;
  }

  public String getDocDescription() {
    return _DocDescription;
  }

  public boolean isDraft() {
    return _DocumentStatus.equals("DR") || _DocumentStatus.equals("RPAP");
  }

  public String getFilename() {
    return _Filename;
  }

  public void setFilename(String newFileName) {
    _Filename = newFileName;
  }

  public File getTargetDirectory() {
    return _targetDirectory;
  }

  public void setTargetDirectory(File targetDirectory) {
    _targetDirectory = targetDirectory;
  }

  public String getTargetLocation() throws IOException {
    return _targetDirectory.getCanonicalPath() + "/" + _Filename;
  }

  public boolean isAttached() {
    return _isAttached;
  }

  public void setAttached(boolean attached) {
    _isAttached = attached;
  }

  public TemplateData[] getTemplate() {
    if (templateInfo.getTemplates() != null) {
      return templateInfo.getTemplates();
    }
    return null;
  }

  public boolean isDeleteable() {
    return deleteReport;
  }

  public void setDeleteable(boolean deleteable) {
    deleteReport = deleteable;
  }

  public String getCheckSalesOrder() {
    return checkSalesOrder;
  }

  public void setCheckSalesOrder(String checkSalesOrder) {
    this.checkSalesOrder = checkSalesOrder;
  }

}
