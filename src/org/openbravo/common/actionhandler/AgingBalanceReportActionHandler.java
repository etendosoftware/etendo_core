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
 * All portions are Copyright (C) 2016-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.common.actionhandler;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.application.ReportDefinition;
import org.openbravo.client.application.report.BaseReportActionHandler;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_reports.AgingDao;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.JRFieldProviderDataSource;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonUtils;

import net.sf.jasperreports.engine.JRDataSource;

/**
 * 
 * @author Nono Carballo
 * 
 */
public class AgingBalanceReportActionHandler extends BaseReportActionHandler {

  private static final String KEY_REPORT_DATA = "REPORT_DATA";
  private static final String MESSAGE_NO_DATA_FOUND = "NoDataFound";
  private static final String MESSAGE_PARAMETER_MISSING = "ParameterMissing";
  private static final String MESSAGE_SEVERITY_ERROR = "error";
  private static final String MESSAGE_SEVERITY_WARNING = "warning";
  private static final String MESSAGE_MESSAGE_PROPERTY = "message";
  private static final String MESSAGE_SHOW_RESULTS_IN_PROCESS_VIEW_PROPERTY = "showResultsInProcessView";
  private static final String MESSAGE_RETRY_EXECUTION_PROPERTY = "retryExecution";
  private static final String MESSAGE_TEXT_PROPERTY = "text";
  private static final String MESSAGE_SEVERITY_PROPERTY = "severity";
  private static final String CONTENT_PARAM = "contentParam";
  private static final String PARAM_PARAM = "_params";
  private static final String PARAM_ACTION = "action";
  private static final String PARAM_DETAILS = "Details";
  private static final String PARAM_BP = "BusinessPartner";
  private static final String PARAM_SHOWVOID = "ShowVoid";
  private static final String PREFERENCE_SHOWVOID = "AGING_ShowVoidCheckbox";
  private static final String PARAM_SHOWREVERSED = "ShowReversed";
  private static final String PREFERENCE_SHOWREVERSED = "AGING_ShowReversedCheckbox";
  private static final String PARAM_DOUBTFUL = "Doubtful";
  private static final String PARAM_RECORPAY = "RecOrPay";
  private static final String RECEIVABLES = "RECEIVABLES";
  private static final String PARAM_CURRENTDATE = "CurrentDate";
  private static final String PROPERTY_DATEFORMAT = "dateFormat.java";
  private static final String PARAM_GL = "AccSchema";
  private static final String PARAM_ORGANIZATION = "Organization";
  private static final String PARAM_COLUMN1 = "Column1";
  private static final String PARAM_COLUMN2 = "Column2";
  private static final String PARAM_COLUMN3 = "Column3";
  private static final String PARAM_COLUMN4 = "Column4";
  private static final String PARAM_WARNING = "warning";
  private static final String TRUE = "true";
  private static final String FALSE = "false";
  private static final String BLANK = "";
  private static final SimpleDateFormat jsDateFormat = JsonUtils.createDateFormat();
  private static String BASE_DESIGN = ReportingUtils.getBaseDesign();

  private static final String AGING_SCHEDULE_HTML = BASE_DESIGN
      + "/org/openbravo/erpCommon/ad_reports/AgingScheduleHTML.jrxml";
  private static final String AGING_SCHEDULE_PDF = BASE_DESIGN
      + "/org/openbravo/erpCommon/ad_reports/AgingSchedulePDF.jrxml";
  private static final String AGING_SCHEDULE_PDF_DOUBTFUL_DEBT = BASE_DESIGN
      + "/org/openbravo/erpCommon/ad_reports/AgingSchedulePDFDoubtfulDebt.jrxml";
  private static final String AGING_SCHEDULE_XLS = BASE_DESIGN
      + "/org/openbravo/erpCommon/ad_reports/AgingScheduleXLS.jrxml";
  private static final String AGING_SCHEDULE_DETAIL_HTML = BASE_DESIGN
      + "/org/openbravo/erpCommon/ad_reports/AgingScheduleDetailHTML.jrxml";
  private static final String AGING_SCHEDULE_DETAIL_XLS = BASE_DESIGN
      + "/org/openbravo/erpCommon/ad_reports/AgingScheduleDetailXLS.jrxml";
  private static final String AGING_SCHEDULE_DETAIL_PDF = BASE_DESIGN
      + "/org/openbravo/erpCommon/ad_reports/AgingScheduleDetailPDF.jrxml";
  private static final String AGING_SCHEDULE_DETAIL_XLS_DOUBTFUL_DEBT = BASE_DESIGN
      + "/org/openbravo/erpCommon/ad_reports/AgingScheduleDetailXLSDoubtfulDebt.jrxml";
  private static final String AGING_SCHEDULE_DETAIL_PDF_DOUBTFUL_DEBT = BASE_DESIGN
      + "/org/openbravo/erpCommon/ad_reports/AgingScheduleDetailPDFDoubtfulDebt.jrxml";

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject result = new JSONObject();
    parameters.put(CONTENT_PARAM, content);
    try {
      if (TRUE.equals(getParameter(PARAM_DETAILS, content))) {
        result = printPageDetails(parameters);
      } else {
        result = printPageSchedule(parameters);
      }
    } catch (JSONException e) {
      throw new OBException(e);
    }
    return result;
  }

  @Override
  protected JRDataSource getReportData(Map<String, Object> parameters) {
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    FieldProvider[] data = (FieldProvider[]) parameters.get(KEY_REPORT_DATA);
    return new JRFieldProviderDataSource(data, vars.getJavaDateFormat());
  }

  @Override
  protected ConnectionProvider getReportConnectionProvider() {
    return DalConnectionProvider.getReadOnlyConnectionProvider();
  }

  private String getParameter(String parameter, JSONObject jsonContent) {
    try {
      final JSONObject params = jsonContent.getJSONObject(PARAM_PARAM);
      if (PARAM_ACTION.equals(parameter)) {
        return jsonContent.getString(ApplicationConstants.BUTTON_VALUE);
      } else if (PARAM_BP.equalsIgnoreCase(parameter)) {
        String strcBpartnerId = params.getString(PARAM_BP);
        if (strcBpartnerId.equals("[]") || strcBpartnerId.equals("[\"\"]")) {
          strcBpartnerId = BLANK;
        }
        if (!strcBpartnerId.isEmpty()) {
          if (strcBpartnerId.contains("(\'")) {
            strcBpartnerId = strcBpartnerId.replace("(\'", BLANK);
          }
          if (strcBpartnerId.contains("\')")) {
            strcBpartnerId = strcBpartnerId.replace("\')", BLANK);
          }
          strcBpartnerId = strcBpartnerId.replace("[", "(");
          strcBpartnerId = strcBpartnerId.replace("]", ")");
          strcBpartnerId = strcBpartnerId.replace("\"", "'");
        }
        return strcBpartnerId;
      } else if (PARAM_SHOWVOID.equals(parameter)) {
        String strVoid;
        try {
          strVoid = Preferences
              .getPreferenceValue(PREFERENCE_SHOWVOID, true,
                  OBContext.getOBContext().getCurrentClient(),
                  OBContext.getOBContext().getCurrentOrganization(),
                  OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null)
              .equals("Y") ? params.getString(PARAM_SHOWVOID) : TRUE;
        } catch (Exception ex) {
          strVoid = TRUE;
        }
        return strVoid;
      } else if (PARAM_SHOWREVERSED.equals(parameter)) {
        String strReversed;
        try {
          strReversed = Preferences
              .getPreferenceValue(PREFERENCE_SHOWREVERSED, true,
                  OBContext.getOBContext().getCurrentClient(),
                  OBContext.getOBContext().getCurrentOrganization(),
                  OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null)
              .equals("Y") ? params.getString(PARAM_SHOWREVERSED) : TRUE;
        } catch (Exception ex) {
          strReversed = TRUE;
        }
        return strReversed;
      } else if (PARAM_DOUBTFUL.equals(parameter)) {
        String recOrPay = params.getString(PARAM_RECORPAY);
        String strDoubtful = FALSE;
        if (RECEIVABLES.equals(recOrPay)) {
          strDoubtful = params.getString(PARAM_DOUBTFUL);
        }
        return strDoubtful;
      }
      return params.getString(parameter);
    } catch (JSONException e) {
      throw new OBException(OBMessageUtils.messageBD(MESSAGE_PARAMETER_MISSING));
    }
  }

  private String getParameter(String parameter, String content) throws JSONException {
    final JSONObject jsonContent = new JSONObject(content);
    return getParameter(parameter, jsonContent);
  }

  private FieldProvider[] getPageDetailsData(String content) throws Exception {
    FieldProvider[] data;
    boolean showDoubtful = TRUE.equals(getParameter(PARAM_DOUBTFUL, content));
    boolean excludeVoid = FALSE.equals(getParameter(PARAM_SHOWVOID, content));
    boolean excludeReversed = FALSE.equals(getParameter(PARAM_SHOWREVERSED, content));

    Currency convCurrency = null;
    String dateFormatString = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty(PROPERTY_DATEFORMAT);
    SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
    final Date currentDate = jsDateFormat.parse(getParameter(PARAM_CURRENTDATE, content));
    final AgingDao dao = new AgingDao();
    AcctSchema acctSchema = null;
    OBContext.setAdminMode(true);
    try {
      acctSchema = OBDal.getInstance().get(AcctSchema.class, getParameter(PARAM_GL, content));
      convCurrency = acctSchema.getCurrency();
    } finally {
      OBContext.restorePreviousMode();
    }
    // Save in session report limit
    RequestContext.get()
        .getVariablesSecureApp()
        .setSessionObject("reportsLimit", getReportsLimit(content));
    data = dao.getOpenReceivablesAgingScheduleDetails(getReportConnectionProvider(), currentDate,
        dateFormat, convCurrency,
        new OrganizationStructureProvider().getChildTree(getParameter(PARAM_ORGANIZATION, content),
            true),
        getParameter(PARAM_RECORPAY, content), getParameter(PARAM_COLUMN1, content),
        getParameter(PARAM_COLUMN2, content), getParameter(PARAM_COLUMN3, content),
        getParameter(PARAM_COLUMN4, content), getParameter(PARAM_BP, content), showDoubtful,
        excludeVoid, excludeReversed);
    return data;
  }

  private JSONObject printPageDetails(Map<String, Object> parameters) {
    JSONObject result = new JSONObject();
    String content = (String) parameters.get(CONTENT_PARAM);

    FieldProvider[] data;
    try {
      data = getPageDetailsData(content);
      if (data.length == 0) {
        JSONObject msg = new JSONObject();
        msg.put(MESSAGE_SEVERITY_PROPERTY, MESSAGE_SEVERITY_WARNING);
        msg.put(MESSAGE_TEXT_PROPERTY, OBMessageUtils.messageBD(MESSAGE_NO_DATA_FOUND));
        result.put(MESSAGE_RETRY_EXECUTION_PROPERTY, true);
        result.put(MESSAGE_SHOW_RESULTS_IN_PROCESS_VIEW_PROPERTY, true);
        result.put(MESSAGE_MESSAGE_PROPERTY, msg);
      } else {
        result = runReport(data, parameters, content);
      }
    } catch (Exception ex) {
      JSONObject msg = new JSONObject();
      try {
        msg.put(MESSAGE_SEVERITY_PROPERTY, MESSAGE_SEVERITY_ERROR);
        msg.put(MESSAGE_TEXT_PROPERTY,
            OBMessageUtils.messageBD(OBMessageUtils.translateError(ex.getMessage()).getMessage()));
        result.put(MESSAGE_RETRY_EXECUTION_PROPERTY, true);
        result.put(MESSAGE_SHOW_RESULTS_IN_PROCESS_VIEW_PROPERTY, true);
        result.put(MESSAGE_MESSAGE_PROPERTY, msg);
      } catch (JSONException ignore) {
      }
    }
    return result;
  }

  private FieldProvider[] getPageScheduleData(String content) throws Exception {
    FieldProvider[] data;
    final AgingDao dao = new AgingDao();
    final Date currentDate = jsDateFormat.parse(getParameter(PARAM_CURRENTDATE, content));
    Calendar cal = new GregorianCalendar();
    cal.setTime(currentDate);
    boolean showDoubtful = TRUE.equals(getParameter(PARAM_DOUBTFUL, content));
    boolean excludeVoid = FALSE.equals(getParameter(PARAM_SHOWVOID, content));
    boolean excludeReversed = FALSE.equals(getParameter(PARAM_SHOWREVERSED, content));

    // Save in session report limit
    RequestContext.get()
        .getVariablesSecureApp()
        .setSessionObject("reportsLimit", getReportsLimit(content));
    data = dao.getOpenReceivablesAgingSchedule(getReportConnectionProvider(),
        getParameter(PARAM_BP, content), getParameter(PARAM_GL, content), currentDate,
        getParameter(PARAM_COLUMN1, content), getParameter(PARAM_COLUMN2, content),
        getParameter(PARAM_COLUMN3, content), getParameter(PARAM_COLUMN4, content),
        getParameter(PARAM_ORGANIZATION, content),
        new OrganizationStructureProvider().getChildTree(getParameter(PARAM_ORGANIZATION, content),
            true),
        getParameter(PARAM_RECORPAY, content), showDoubtful, excludeVoid, excludeReversed);
    return data;
  }

  private JSONObject printPageSchedule(Map<String, Object> parameters) {
    JSONObject result = new JSONObject();
    FieldProvider[] data;
    String content = (String) parameters.get(CONTENT_PARAM);

    try {
      data = getPageScheduleData(content);
      if (data.length == 0) {
        JSONObject msg = new JSONObject();
        msg.put(MESSAGE_SEVERITY_PROPERTY, MESSAGE_SEVERITY_WARNING);
        msg.put(MESSAGE_TEXT_PROPERTY, OBMessageUtils.messageBD(MESSAGE_NO_DATA_FOUND));
        result.put(MESSAGE_RETRY_EXECUTION_PROPERTY, true);
        result.put(MESSAGE_SHOW_RESULTS_IN_PROCESS_VIEW_PROPERTY, true);
        result.put(MESSAGE_MESSAGE_PROPERTY, msg);
      } else {
        result = runReport(data, parameters, content);
      }
    } catch (Exception ex) {
      JSONObject msg = new JSONObject();
      try {
        msg.put(MESSAGE_SEVERITY_PROPERTY, MESSAGE_SEVERITY_ERROR);
        msg.put(MESSAGE_TEXT_PROPERTY,
            OBMessageUtils.messageBD(OBMessageUtils.translateError(ex.getMessage()).getMessage()));
        result.put(MESSAGE_RETRY_EXECUTION_PROPERTY, true);
        result.put(MESSAGE_SHOW_RESULTS_IN_PROCESS_VIEW_PROPERTY, true);
        result.put(MESSAGE_MESSAGE_PROPERTY, msg);
      } catch (JSONException ignore) {
      }
    }
    return result;
  }

  @Override
  protected String getReportTemplatePath(ExportType expType, ReportDefinition report,
      JSONObject jsonContent) throws JSONException {
    String jRPath = BLANK;
    boolean includeDoubtful = TRUE.equals(getParameter(PARAM_DOUBTFUL, jsonContent));
    boolean includeDetails = TRUE.equals(getParameter(PARAM_DETAILS, jsonContent));
    if (includeDetails) {
      switch (expType) {
        case XLS:
        case XLSX:
          if (includeDoubtful) {
            if (report.isUsePDFAsXLSTemplate()) {
              jRPath = AGING_SCHEDULE_DETAIL_PDF_DOUBTFUL_DEBT;
            } else {
              jRPath = AGING_SCHEDULE_DETAIL_XLS_DOUBTFUL_DEBT;
            }
          } else {
            if (report.isUsePDFAsXLSTemplate()) {
              jRPath = AGING_SCHEDULE_DETAIL_PDF;
            } else {
              jRPath = AGING_SCHEDULE_DETAIL_XLS;
            }
          }
          break;
        case PDF:
          if (includeDoubtful) {
            jRPath = AGING_SCHEDULE_DETAIL_PDF_DOUBTFUL_DEBT;
          } else {
            jRPath = AGING_SCHEDULE_DETAIL_PDF;
          }
          break;
        case HTML:
          jRPath = AGING_SCHEDULE_DETAIL_HTML;
          break;
        default:
          jRPath = BLANK;
      }
    } else {
      switch (expType) {
        case XLS:
        case XLSX:
          jRPath = AGING_SCHEDULE_XLS;
          break;
        case PDF:
          if (includeDoubtful) {
            jRPath = AGING_SCHEDULE_PDF_DOUBTFUL_DEBT;
          } else {
            jRPath = AGING_SCHEDULE_PDF;
          }
          break;
        case HTML:
          jRPath = AGING_SCHEDULE_HTML;
          break;
        default:
          jRPath = BLANK;
      }
    }
    if (StringUtils.isEmpty(jRPath)) {
      throw new OBException(OBMessageUtils.messageBD("OBUIAPP_NoJRTemplateFound"));
    }
    return jRPath;
  }

  @Override
  protected void addAdditionalParameters(ReportDefinition report, JSONObject jsonContent,
      Map<String, Object> parameters) {
    ConnectionProvider conn = getReportConnectionProvider();
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    try {
      String strColumn1 = getParameter(PARAM_COLUMN1, jsonContent);
      String strColumn2 = getParameter(PARAM_COLUMN2, jsonContent);
      String strColumn3 = getParameter(PARAM_COLUMN3, jsonContent);
      String strColumn4 = getParameter(PARAM_COLUMN4, jsonContent);
      String recOrPay = getParameter(PARAM_RECORPAY, jsonContent);
      String strOrg = getParameter(PARAM_ORGANIZATION, jsonContent);
      String strAccSchema = getParameter(PARAM_GL, jsonContent);
      String strCurrentDate = getParameter(PARAM_CURRENTDATE, jsonContent);
      String strcBpartnerId = getParameter(PARAM_BP, jsonContent);
      String strVoid = getParameter(PARAM_SHOWVOID, jsonContent);
      String strReversed = getParameter(PARAM_SHOWREVERSED, jsonContent);
      String strDoubtful = getParameter(PARAM_DOUBTFUL, jsonContent);
      String dateFormatString = OBPropertiesProvider.getInstance()
          .getOpenbravoProperties()
          .getProperty(PROPERTY_DATEFORMAT);
      SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
      final Date currentDate = jsDateFormat.parse(strCurrentDate);
      parameters.put("USER_CLIENT", Utility.getContext(conn, vars, "#User_Client", BLANK));
      parameters.put("processId", report.getProcessDefintion().getId());
      parameters.put("reportId", report.getId());
      parameters.put("dateFromUI", strCurrentDate);
      parameters.put("recOrPay", recOrPay);
      if (TRUE.equals(getParameter(PARAM_DETAILS, jsonContent))) {
        parameters.put("currentDate", dateFormat.format(currentDate));
        parameters.put("BPartnerID", strcBpartnerId.replace("'", "\\\'"));
        parameters.put("organizationID", strOrg);
        parameters.put("accSchemaID", strAccSchema);
        parameters.put("Organization",
            OBDal.getInstance().get(Organization.class, strOrg).getIdentifier());
        AcctSchema acctSchema = null;
        OBContext.setAdminMode(true);
        try {
          acctSchema = OBDal.getInstance()
              .get(AcctSchema.class, getParameter(PARAM_GL, jsonContent));
        } finally {
          OBContext.restorePreviousMode();
        }
        parameters.put("AccSchema", acctSchema.getIdentifier());
        parameters.put("showDoubtfulDebt", strDoubtful);
        parameters.put("inpColumn1", strColumn1);
        parameters.put("inpColumn2", strColumn2);
        parameters.put("inpColumn3", strColumn3);
        parameters.put("inpColumn4", strColumn4);
        parameters.put("inpLabel1", "1-" + strColumn1);
        parameters.put("inpLabel2", (Integer.valueOf(strColumn1) + 1) + "-" + strColumn2);
        parameters.put("inpLabel3", (Integer.valueOf(strColumn2) + 1) + "-" + strColumn3);
        parameters.put("inpLabel4", (Integer.valueOf(strColumn3) + 1) + "-" + strColumn4);
        parameters.put("inpLabel5", "Over " + strColumn4);
        parameters.put("void", strVoid);
        parameters.put("reversed", strReversed);
        if (RECEIVABLES.equals(recOrPay)) {
          parameters.put("title", Utility.messageBD(conn, "AGING_ORASD", vars.getLanguage()));
          parameters.put("tabID", "263");
          parameters.put("tabTitle", Utility.messageBD(conn, "AGING_ORASD", vars.getLanguage()));
        } else {
          parameters.put("title", Utility.messageBD(conn, "AGING_PASD", vars.getLanguage()));
          parameters.put("tabID", "290");
          parameters.put("tabTitle", Utility.messageBD(conn, "AGING_PASD", vars.getLanguage()));
        }
      } else {
        Calendar cal = new GregorianCalendar();
        cal.setTime(currentDate);
        Currency toCurrency = null;
        AcctSchema acctSchema = null;
        Organization organization = null;
        OBContext.setAdminMode(true);
        try {
          if (strAccSchema == null || BLANK.equals(strAccSchema)) {
            organization = OBDal.getInstance().get(Organization.class, strOrg);
            toCurrency = organization.getCurrency();
          } else {
            acctSchema = OBDal.getInstance().get(AcctSchema.class, strAccSchema);
            toCurrency = acctSchema.getCurrency();
          }
        } finally {
          OBContext.restorePreviousMode();
        }
        parameters.put("currentDate", dateFormat.format(currentDate));
        parameters.put("col1", "1-" + strColumn1);
        parameters.put("inpColumn1", strColumn1);
        cal.add(Calendar.DATE, -Integer.parseInt(strColumn1));
        parameters.put("Date1", dateFormat.format(cal.getTime()));
        parameters.put("col2",
            String.valueOf((Integer.valueOf(strColumn1) + 1)) + "-" + strColumn2);
        parameters.put("inpColumn2", strColumn2);
        cal.add(Calendar.DATE, -((Integer.parseInt(strColumn2) - (Integer.parseInt(strColumn1)))));
        parameters.put("Date2", dateFormat.format(cal.getTime()));
        parameters.put("col3",
            String.valueOf((Integer.valueOf(strColumn2) + 1)) + "-" + strColumn3);
        parameters.put("inpColumn3", strColumn3);
        cal.add(Calendar.DATE, -((Integer.parseInt(strColumn3) - (Integer.parseInt(strColumn2)))));
        parameters.put("Date3", dateFormat.format(cal.getTime()));
        parameters.put("col4",
            String.valueOf((Integer.valueOf(strColumn3) + 1)) + "-" + strColumn4);
        parameters.put("inpColumn4", strColumn4);
        cal.add(Calendar.DATE, -((Integer.parseInt(strColumn4) - (Integer.parseInt(strColumn3)))));
        parameters.put("Date4", dateFormat.format(cal.getTime()));
        parameters.put("col5", ">" + strColumn4);
        parameters.put("BPartners", strcBpartnerId.replace("'", "\\\'"));
        parameters.put("Organization", strOrg);
        parameters.put("AccSchema", strAccSchema);
        parameters.put("OrganizationName",
            OBDal.getInstance().get(Organization.class, strOrg).getIdentifier());
        parameters.put("AccSchemaName",
            (strAccSchema != null) ? acctSchema.getIdentifier() : BLANK);
        parameters.put("Currency", Utility.stringBaseCurrencyId(conn, vars.getClient()));
        parameters.put("toCurrency", toCurrency.getId());
        parameters.put("showDoubtfulDebt", strDoubtful);
        parameters.put("void", strVoid);
        parameters.put("reversed", strReversed);
        if (RECEIVABLES.equals(recOrPay)) {
          parameters.put("tabTitle", Utility.messageBD(conn, "AGING_ORASD", vars.getLanguage()));
          parameters.put("title", Utility.messageBD(conn, "AGING_ORAS", vars.getLanguage()));
        } else {
          parameters.put("tabTitle", Utility.messageBD(conn, "AGING_PASD", vars.getLanguage()));
          parameters.put("title", Utility.messageBD(conn, "AGING_PAS", vars.getLanguage()));
        }
      }
      parameters.put("USER_ORG", Utility.getContext(conn, vars, "#User_Org", BLANK));
      parameters.put("REPORT_TITLE", parameters.get("title"));
      if (((JSONObject) jsonContent.get(PARAM_PARAM)).has(PARAM_WARNING)) {
        parameters.put("warning", getParameter(PARAM_WARNING, jsonContent));
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  private int getReportsLimit(String content) throws JSONException {
    if (StringUtils.equals(getParameter(PARAM_ACTION, content), "HTML")) {
      final VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      String limitPreference = Utility.getPreference(vars, "ReportsLimit", "");
      return Integer.parseInt(StringUtils.isEmpty(limitPreference) ? "0" : limitPreference);
    }
    return 0;
  }

  private JSONObject runReport(FieldProvider[] data, Map<String, Object> parameters, String content)
      throws JSONException {
    JSONObject msg = null;
    JSONObject paramContent = new JSONObject(content);
    int limit = (int) RequestContext.get().getVariablesSecureApp().getSessionObject("reportsLimit");

    if (limit > 0 && data.length > limit) {
      msg = new JSONObject();
      String msgbody = OBMessageUtils.messageBD("ReportsLimit");
      msgbody = msgbody.replace("@limit@", String.valueOf(limit));
      // Add warning to the report
      ((JSONObject) paramContent.get(PARAM_PARAM)).put(PARAM_WARNING, msgbody);
      msg.put(MESSAGE_SEVERITY_PROPERTY, MESSAGE_SEVERITY_WARNING);
      msg.put(MESSAGE_TEXT_PROPERTY, msgbody);
    }

    parameters.put(KEY_REPORT_DATA, data);
    JSONObject result = super.doExecute(parameters, paramContent.toString());
    if (msg != null) {
      result.put(MESSAGE_MESSAGE_PROPERTY, msg);
    }

    return result;
  }
}
