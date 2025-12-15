package com.smf.jobs.defaults.mail;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.KernelServlet;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.StringCollectionUtils;
import org.openbravo.erpCommon.utility.reporting.DocumentType;
import org.openbravo.erpCommon.utility.reporting.Report;
import org.openbravo.erpCommon.utility.reporting.ReportManager;
import org.openbravo.erpCommon.utility.reporting.ReportingException;
import org.openbravo.erpCommon.utility.reporting.TemplateInfo;
import org.openbravo.erpCommon.utility.reporting.printing.EmailUtilities;
import org.openbravo.erpCommon.utility.reporting.printing.PocData;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.DalConnectionProvider;

import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Data;
import com.smf.jobs.Result;

import jakarta.enterprise.context.Dependent;
import jakarta.servlet.ServletException;

@Dependent
public class SendMail extends Action {
    Logger log = LogManager.getLogger();
    private String tabId = null;
    private static final String INP_TAB_KEY = "inpTabId";

    @Override
    protected Data preRun(JSONObject jsonContent) {
        this.tabId = jsonContent.optString(INP_TAB_KEY);
        return super.preRun(jsonContent);
    }

    @Override
    protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
        var result = new ActionResult();
        VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

        try {
            result.setType(Result.Type.SUCCESS);

            var input = getInputContents();

            final ConnectionProvider connectionProvider = new DalConnectionProvider();
            final Map<String, Boolean> checks = new HashMap<>();
            final String emailConfigurationId = parameters.getString("from");
            final boolean shouldAttachReport = parameters.getBoolean("archiveReport");

            // Check that the document(s) are properly configured before continuing.
            doDocumentValidations(input, connectionProvider, checks);

            RequestContext.get().setRequestParameter("toEmail", parameters.getString("sentTo"));
            RequestContext.get().setRequestParameter("replyToEmail", getOptionalParameter(parameters, "replyTo"));
            RequestContext.get().setRequestParameter("emailSubject", getOptionalParameter(parameters, "subject"));
            RequestContext.get().setRequestParameter("emailBody", getOptionalParameter(parameters, "body"));
            RequestContext.get().setRequestParameter("fromEmailId", emailConfigurationId);
            RequestContext.get().setRequestParameter("ccEmail", getOptionalParameter(parameters, "toCC"));
            RequestContext.get().setRequestParameter("bccEmail", getOptionalParameter(parameters, "toBCC"));

            vars = new VariablesSecureApp(RequestContext.get().getRequest());
            vars.setSessionValue(INP_TAB_KEY, this.tabId);

            final ConfigParameters servletConfiguration = KernelServlet.getGlobalParameters();
            final ReportManager reportManager = new ReportManager(servletConfiguration.strFTPDirectory,
                    null, servletConfiguration.strBaseDesignPath,
                    servletConfiguration.strDefaultDesignPath, servletConfiguration.prefix, false);

            int emailsSent = 0;

            for (BaseOBObject record : input) {
                final EmailServerConfiguration emailConfiguration = OBDal.getInstance().get(EmailServerConfiguration.class, emailConfigurationId);
                final String senderAddress = emailConfiguration.getSmtpServerSenderAddress();

                DocumentType documentType = getDocumentType(record);

                Report report =  new Report(documentType, (String) record.getId(), vars.getLanguage(),
                        "default", false, Report.OutputTypeEnum.DEFAULT);
                reportManager.setTargetDirectory(report);

                // if there is only one document type id the user should be
                // able to choose between different templates
                boolean differentDocumentTypes = checks.getOrDefault("differentDocTypes", false);
                if (!differentDocumentTypes) {
                    String templateId = parameters.getString("template");
                    report.setTemplateInfo(getTemplateInfo(templateId, report, vars.getLanguage(), connectionProvider));
                }

                // Check if the document is not in status 'draft'
                if (report.isDraft()) {
                    throw new OBException(OBMessageUtils.messageBD("ErrorIncompleteDocuments"));
                } else if (!report.isAttached() && shouldAttachReport) {
                    // Check if the report is already attached

                    // get the Id of the entities table, this is used to
                    // store the file as an OB attachment
                    final String tableId = getTableId(report.getDocumentType().getTableName());

                        // Save the report as a attachment because it is
                        // being transferred to the user
                        reportManager.createAttachmentForReport(connectionProvider, report, tableId, vars,
                                ReportManager.GENERATED_BY_EMAILING);
                } else if (!shouldAttachReport) {
                    reportManager.saveTempReport(report, vars);
                } else if (report.isAttached()) {
                    log.debug("Document will not be attached because it already is. (For record ID: {})", record.getId());
                }

                checks.put("sendDocumentAttachments", true); // send all the attached files

                PocData[] pocData = EmailUtilities.getContactDetails(documentType, getSQLInClause(Collections.singletonList((String) record.getId())), connectionProvider);

                for (PocData documentData : pocData) {
                    EmailUtilities.prepareAndSendEmail(report, vars,
                            null, documentData,
                            senderAddress, checks,  connectionProvider);
                    emailsSent++;
                }

            }

            if (emailsSent == 0) {
                result.setType(Result.Type.ERROR);
                result.setMessage(OBMessageUtils.messageBD("DJOBS_NoMailsSent"));
            } else {
                result.setMessage(OBMessageUtils.messageBD("DJOBS_MailsSent"));
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setType(Result.Type.ERROR);
            result.setMessage(e.getMessage());
        } finally {
            vars.removeSessionValue(INP_TAB_KEY);
        }

        return result;
    }

    private void doDocumentValidations(List<BaseOBObject> documents, ConnectionProvider connectionProvider, Map<String, Boolean> checks) throws ServletException {
        Set<Object> documentTypes = new HashSet<>();
        checks.put("moreThanOneDoc", documents.size() > 1);

        for (BaseOBObject document : documents) {
            DocumentType reportDocumentType = getDocumentType(document);
            PocData[] pocData = EmailUtilities.getContactDetails(reportDocumentType, getSQLInClause(Collections.singletonList((String) document.getId())), connectionProvider);
            Set<String> customers = new HashSet<>();
            Set<String> salesRepresentatives = new HashSet<>();

            for (final PocData documentData : pocData) {
                final String customer = documentData.contactEmail;
                final String salesRep = documentData.salesrepEmail;
                customers.add(customer);
                salesRepresentatives.add(salesRep);

                if (customers.size() > 1 && (StringUtils.isBlank(documentData.contactEmail))) {
                    throw new OBException(OBMessageUtils.messageBD("NoContact")
                            .replace("@docNum@", documentData.ourreference));
                }

                if (salesRepresentatives.size() > 1 && (StringUtils.isBlank(documentData.salesrepEmail))) {
                    throw new OBException(OBMessageUtils.messageBD("NoSenderDocument"));
                }
            }

            checks.put("moreThanOneCustomer", salesRepresentatives.size() > 1);
            checks.put("moreThanOnesalesRep", customers.size() > 1);
            documentTypes.add(document.get("documentType"));
        }

        checks.put("differentDocTypes", documentTypes.size() > 1);
    }

    private String getTableId(String tableName) {
        try {
            OBContext.setAdminMode();
            OBCriteria<Table> tableOBCriteria = OBDal.getInstance().createCriteria(Table.class);
            tableOBCriteria.add(Restrictions.eq(Table.PROPERTY_DBTABLENAME, tableName));
            tableOBCriteria.setMaxResults(1);

            Table table = (Table) tableOBCriteria.uniqueResult();

            if (table != null) {
                return table.getId();
            } else {
                return null;
            }

        } finally {
            OBContext.restorePreviousMode();
        }
    }

    private DocumentType getDocumentType(BaseOBObject record) {
        String tableName = record.getEntity().getTableName().toUpperCase();
        if ("C_ORDER".equalsIgnoreCase(tableName)) {
            // C_Order table also holds quotations, check if the document is a quotation first
            org.openbravo.model.common.enterprise.DocumentType transactionDoc = (org.openbravo.model.common.enterprise.DocumentType) record.get(Order.PROPERTY_TRANSACTIONDOCUMENT);
            if (transactionDoc != null && transactionDoc.getSOSubType() != null && transactionDoc.getSOSubType().contains("OB")) {
                return DocumentType.QUOTATION;
            }
            // If the document is an order, return the sales order document type in the same way as the legacy PrintController.
            return DocumentType.SALESORDER;
        }
        return DocumentType.getByTableName(tableName);
    }

    private TemplateInfo getTemplateInfo(String templateId, Report report, String language, ConnectionProvider connectionProvider) throws ServletException {
        try {
            return new TemplateInfo(connectionProvider, report.getDocTypeId(),
                    report.getOrgId(), language, templateId);
        } catch (final ReportingException e) {
            throw new ServletException("Error trying to get template information", e);
        }
    }

    private String getSQLInClause(List<String> elements) {
        return "(" +
                StringCollectionUtils.commaSeparated(elements) +
                ")";
    }

    private String getOptionalParameter(JSONObject parameters, String key) throws JSONException {
        if (parameters.has(key) && !parameters.isNull(key)) {
            return parameters.getString(key);
        } else {
            return null;
        }
    }

    @Override
    protected Class<?> getInputClass() {
        return BaseOBObject.class;
    }
}
