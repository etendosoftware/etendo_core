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
 * All portions are Copyright (C) 2014-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.report;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.HttpBaseUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.domaintype.BigDecimalDomainType;
import org.openbravo.base.model.domaintype.BooleanDomainType;
import org.openbravo.base.model.domaintype.DateDomainType;
import org.openbravo.base.model.domaintype.DomainType;
import org.openbravo.base.model.domaintype.ForeignKeyDomainType;
import org.openbravo.base.model.domaintype.LongDomainType;
import org.openbravo.base.model.domaintype.StringDomainType;
import org.openbravo.base.model.domaintype.StringEnumerateDomainType;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.Process;
import org.openbravo.client.application.ReportDefinition;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.DalContextListener;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DbUtility;
import org.openbravo.userinterface.selector.reference.FKMultiSelectorUIDefinition;
import org.openbravo.utils.FileUtility;

import net.sf.jasperreports.engine.JRDataSource;

/**
 * Action Handler used as base for jasper reports generated from process definition. This handler
 * can be extended to customize its behavior.
 * 
 */
public class BaseReportActionHandler extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();
  private static final String JASPER_PARAM_PROCESS = "jasper_process";
  protected static final String JASPER_REPORT_PARAMETERS = "JASPER_REPORT_PARAMETERS";

  /**
   * execute() method overridden to add the logic to download or display the report file stored in
   * the temporary folder.
   */
  @Override
  public void execute() {
    final HttpServletRequest request = RequestContext.get().getRequest();
    String mode = request.getParameter("mode");
    if (mode == null) {
      mode = "Default";
    }
    if ("DOWNLOAD".equals(mode)) {
      try {
        doDownload(request);
      } catch (Exception e) {
        // Error downloading file
        log.error("Error downloading the file: " + e.getMessage(), e);
      }
      return;
    } else if ("BROWSE".equals(mode)) {
      try {
        doBrowse(request);
      } catch (IOException e) {
        log.error("Error browsing the file: " + e.getMessage(), e);
      }
      return;
    }
    SessionInfo.auditThisThread(false);
    super.execute();
  }

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {

    try {
      JSONObject result = getResponseBuilder().retryExecution().showResultsInProcessView().build();

      final JSONObject jsonContent = new JSONObject(content);
      final String action = jsonContent.getString(ApplicationConstants.BUTTON_VALUE);
      doGenerateReport(result, parameters, jsonContent, action);

      return result;
    } catch (Exception e) {
      log.error("Error generating report id: {}", parameters.get("reportId"), e);
      Throwable uiException = DbUtility.getUnderlyingSQLException(e);
      return getResponseBuilder().retryExecution()
          .showResultsInProcessView()
          .showMsgInProcessView(MessageType.ERROR,
              OBMessageUtils.translateError(uiException.getMessage()).getMessage())
          .build();
    }
  }

  /**
   * Returns the report output. The file containing the output is stored in a temporary folder with
   * a generated name. Its content is sent back to the browser to be shown in a new Openbravo tab.
   * Once the process is finished the file is removed from the server.
   *
   * @param request
   *          The HTTP request for this handler.
   */
  private void doBrowse(HttpServletRequest request) throws IOException {
    final Map<String, Object> parameters = getParameterMapFromRequest(request);
    final String strFileName = (String) parameters.get("fileName");
    final String tmpFileName = (String) parameters.get("tmpfileName");
    ExportType expType = ExportType.HTML;

    handleReportResponse(request, expType, strFileName, tmpFileName, false);
  }

  /**
   * Downloads the file with the report result. The file is stored in a temporary folder with a
   * generated name. It is renamed and download as an attachment of the HTTP response. Once it is
   * finished the file is removed from the server.
   *
   * @param request
   *          The HTTP request for this handler.
   */
  private void doDownload(HttpServletRequest request) throws IOException {
    final Map<String, Object> parameters = getParameterMapFromRequest(request);
    final String strFileName = (String) parameters.get("fileName");
    final String tmpFileName = (String) parameters.get("tmpfileName");
    ExportType expType = null;

    if (strFileName.endsWith("." + ExportType.PDF.getExtension())) {
      expType = ExportType.PDF;
    } else if (strFileName.endsWith("." + ExportType.XLS.getExtension())) {
      expType = ExportType.XLS;
    } else if (strFileName.endsWith("." + ExportType.XLSX.getExtension())) {
      expType = ExportType.XLSX;
    } else if (strFileName.endsWith("." + ExportType.CSV.getExtension())) {
      expType = ExportType.CSV;
    } else {
      throw new IllegalArgumentException(
          "Trying to download report file with unsupported type " + strFileName);
    }

    handleReportResponse(request, expType, strFileName, tmpFileName, true);
  }

  private void handleReportResponse(HttpServletRequest request, ExportType expType,
      String strFileName, String tmpFileName, boolean includeReportAsAttachment)
      throws IOException {
    if (!expType.isValidTemporaryFileName(tmpFileName)) {
      throw new IllegalArgumentException(
          "Trying to download report with invalid name " + strFileName);
    }
    final String tmpDirectory = ReportingUtils.getTempFolder();
    final File file = new File(tmpDirectory, tmpFileName);
    FileUtility fileUtil = new FileUtility(tmpDirectory, tmpFileName, false, true);
    try {
      final HttpServletResponse response = RequestContext.get().getResponse();
      response.setHeader("Content-Type", expType.getContentType());
      response.setContentType(expType.getContentType());
      response.setCharacterEncoding("UTF-8");
      if (includeReportAsAttachment) {
        // TODO: Compatibility code with IE8. To be reviewed when its support is stopped.
        // see issue #29109
        String userAgent = request.getHeader("user-agent");
        if (userAgent.contains("MSIE")) {
          response.setHeader("Content-Disposition",
              "attachment; filename=\"" + URLEncoder.encode(strFileName, "utf-8") + "\"");
        } else {
          response.setHeader("Content-Disposition",
              "attachment; filename=\"" + MimeUtility.encodeWord(strFileName, "utf-8", "Q") + "\"");
        }
      }
      fileUtil.dumpFile(response.getOutputStream());
      response.getOutputStream().flush();
      response.getOutputStream().close();
    } finally {
      if (file.exists()) {
        file.delete();
      }
    }
  }

  private Map<String, Object> getParameterMapFromRequest(HttpServletRequest request) {
    Map<String, Object> parameterMap = new HashMap<String, Object>();
    for (Enumeration<?> keys = request.getParameterNames(); keys.hasMoreElements();) {
      final String key = (String) keys.nextElement();
      if (request.getParameterValues(key) != null && request.getParameterValues(key).length > 1) {
        parameterMap.put(key, request.getParameterValues(key));
      } else {
        parameterMap.put(key, request.getParameter(key));
      }
    }
    parameterMap.put(KernelConstants.HTTP_SESSION, request.getSession(false));
    parameterMap.put(KernelConstants.HTTP_REQUEST, request);
    return parameterMap;
  }

  /**
   * Get the PDF, XLS or HTML template path from the Report Definition. Override this method to add
   * custom logic to get report template paths.
   * 
   * @param expType
   *          The export type.
   * @param report
   *          The Report Definition.
   * @param jsonContent
   *          JSONObject with the values set in the filter parameters, used by the classes extending
   *          this one when generating reports which use more than one template. In that case, the
   *          selection of the template can be done based on the values of the parameters present in
   *          this JSONObject.
   * @return The template path.
   */

  protected String getReportTemplatePath(ExportType expType, ReportDefinition report,
      JSONObject jsonContent) throws JSONException, OBException {
    String strJRPath = "";
    switch (expType) {
      case XLS:
      case XLSX:
        if (report.isUsePDFAsXLSTemplate()) {
          strJRPath = report.getPDFTemplate();
        } else {
          strJRPath = report.getXLSTemplate();
        }
        break;
      case HTML:
        if (report.isUsePDFAsHTMLTemplate()) {
          strJRPath = report.getPDFTemplate();
        } else {
          strJRPath = report.getHTMLTemplate();
        }
        break;
      case PDF:
        strJRPath = report.getPDFTemplate();
        break;
      default:
        throw new OBException(OBMessageUtils.getI18NMessage("OBUIAPP_UnsupportedAction",
            new String[] { expType.getExtension() }));
    }
    if (StringUtils.isEmpty(strJRPath)) {
      throw new OBException(OBMessageUtils.messageBD("OBUIAPP_NoJRTemplateFound"));
    }
    return strJRPath;
  }

  /**
   * Manages the report generation. It sets the proper response actions to download the generated
   * file or to open it in a new Openbravo tab.
   * 
   * @param result
   *          JSONObject with the response that is returned to the client.
   * @param parameters
   *          Map including the parameters of the call.
   * @param jsonContent
   *          JSONObject with the values set in the filter parameters.
   * @param action
   *          String with the output type of the report.
   *
   * @throws JSONException
   * @throws OBException
   *           Exception thrown when a validation fails.
   */

  protected void doGenerateReport(JSONObject result, Map<String, Object> parameters,
      JSONObject jsonContent, String action) throws JSONException, OBException {
    JSONObject params = jsonContent.getJSONObject("_params");
    final ReportDefinition report = OBDal.getInstance()
        .get(ReportDefinition.class, parameters.get("reportId"));

    doValidations(report, parameters, jsonContent);
    final ExportType expType = getExportType(action);

    String strFileName = getReportFileName(report, parameters, expType);
    String strTmpFileName = UUID.randomUUID().toString() + "." + expType.getExtension();
    String strJRPath = getReportTemplatePath(expType, report, jsonContent);

    if (!strJRPath.startsWith("/")) {
      // Tomcat 8 forces getRealPath to start with a slash
      strJRPath = "/" + strJRPath;
    }
    final String jrTemplatePath = DalContextListener.getServletContext().getRealPath(strJRPath);

    Map<String, Object> allParametersMap = new HashMap<>();
    Map<String, Object> jrParams = new HashMap<>();
    loadFilterParams(jrParams, report, params);
    loadReportParams(jrParams, report, jrTemplatePath, jsonContent);
    // Include the HTTP session into the parameters that are sent to the report
    jrParams.put("HTTP_SESSION", parameters.get(KernelConstants.HTTP_SESSION));
    allParametersMap.putAll(parameters);
    allParametersMap.put(JASPER_REPORT_PARAMETERS, jrParams);

    log.debug("Report: {}. Start export JR process.", report.getId());
    long t1 = System.currentTimeMillis();
    doJRExport(jrTemplatePath, expType, strTmpFileName, allParametersMap);
    log.debug("Report: {}. Finish export JR process. Elapsed time: {}", report.getId(),
        System.currentTimeMillis() - t1);

    final JSONObject recordInfo = new JSONObject();
    params.put("processId", parameters.get("processId"));
    params.put("reportId", parameters.get("reportId"));
    params.put("actionHandler", this.getClass().getName());
    recordInfo.put("processParameters", params);
    recordInfo.put("tmpfileName", strTmpFileName);
    recordInfo.put("fileName", strFileName);

    final JSONObject reportAction = new JSONObject();
    if (expType.equals(ExportType.HTML)) {
      recordInfo.put("tabTitle", getResultTabTitle(report));
      reportAction.put("OBUIAPP_browseReport", recordInfo);
    } else {
      reportAction.put("OBUIAPP_downloadReport", recordInfo);
    }

    final JSONArray actions = new JSONArray();
    actions.put(0, reportAction);
    result.put("responseActions", actions);
  }

  private ExportType getExportType(String action) {
    if (ExportType.XLS.hasExtension(action)) {
      return ReportingUtils.getExcelExportType();
    }
    return ExportType.getExportType(action);
  }

  private String getResultTabTitle(ReportDefinition report) {
    Process processDefinition = report.getProcessDefintion();
    return (String) processDefinition.get(Process.PROPERTY_NAME,
        OBContext.getOBContext().getLanguage());
  }

  /**
   * Override this method to add validations to the report before it is generated.
   * 
   * @param report
   *          the Report Definition
   * @param parameters
   *          Map including the parameters of the call.
   * @param jsonContent
   *          JSONObject with the values set in the filter parameters.
   */
  protected void doValidations(ReportDefinition report, Map<String, Object> parameters,
      JSONObject jsonContent) throws OBException {
  }

  /**
   * Method that loads the values used in the filter to include them in the parameters that are sent
   * to JasperReports
   * 
   * @param jrParams
   *          the Map instance with all the parameters to be sent to Jasper Reports.
   * @param report
   *          the Report Definition.
   * @param params
   *          JSONObject with the values set in the filter parameters.
   */
  private void loadFilterParams(Map<String, Object> jrParams, ReportDefinition report,
      JSONObject params) throws JSONException {
    for (Parameter param : report.getProcessDefintion().getOBUIAPPParameterList()) {
      String paramName = param.getDBColumnName();

      if (params.isNull(paramName)) {
        jrParams.put(paramName, null);
        continue;
      }

      DomainType baseDomainType = ModelProvider.getInstance()
          .getReference(param.getReference().getId())
          .getDomainType();
      DomainType domainType = null;
      if (param.getReferenceSearchKey() != null) {
        domainType = ModelProvider.getInstance()
            .getReference(param.getReferenceSearchKey().getId())
            .getDomainType();
      }

      if (baseDomainType instanceof ForeignKeyDomainType) {
        Entity referencedEntity = ((ForeignKeyDomainType) domainType)
            .getForeignKeyColumn(param.getDBColumnName())
            .getProperty()
            .getEntity();
        UIDefinition uiDefinition = UIDefinitionController.getInstance()
            .getUIDefinition(param.getReference());
        JSONObject def = new JSONObject();

        if (uiDefinition instanceof FKMultiSelectorUIDefinition) {
          JSONArray selectedValues = params.getJSONArray(paramName);
          JSONArray selectedIdentifiers = new JSONArray();
          String strValues = "";
          String strIdentifiers = "";

          for (int i = 0; i < selectedValues.length(); i++) {
            if (i > 0) {
              strValues += ", ";
              strIdentifiers += ", ";
            }
            String value = selectedValues.getString(i);
            strValues += "'" + value + "'";
            BaseOBObject record = OBDal.getInstance().get(referencedEntity.getName(), value);
            if (record != null) {
              String identifier = record.getIdentifier();
              strIdentifiers += identifier;
              selectedIdentifiers.put(identifier);
            }
          }
          def.put("values", selectedValues);
          def.put("identifiers", selectedIdentifiers);
          def.put("strValues", strValues);
          def.put("strIdentifiers", strIdentifiers);
          jrParams.put(paramName, def);
        } else {
          String value = params.getString(paramName);
          BaseOBObject record = OBDal.getInstance().get(referencedEntity.getName(), value);
          if (record != null) {
            String identifier = record.getIdentifier();
            def.put("value", value);
            def.put("identifier", identifier);
          }
        }
        jrParams.put(paramName, def);
      } else if (baseDomainType.getClass().equals(StringEnumerateDomainType.class)) {
        // List reference
        String value = params.getString(paramName);
        String identifier = "";
        for (org.openbravo.model.ad.domain.List list : param.getReferenceSearchKey()
            .getADListList()) {
          if (list.getSearchKey().equals(value)) {
            identifier = list.getName();
            break;
          }
        }
        JSONObject def = new JSONObject();
        def.put("value", value);
        def.put("identifier", identifier);
        jrParams.put(paramName, def);
      } else if (baseDomainType.getClass().equals(StringDomainType.class)) {
        jrParams.put(paramName, params.getString(paramName));
      } else if (baseDomainType.getClass().equals(DateDomainType.class)) {
        DateDomainType dateDomainType = (DateDomainType) baseDomainType;
        Date date = (Date) dateDomainType.createFromString(params.getString(paramName));
        jrParams.put(paramName, date);
      } else if (baseDomainType.getClass().getSuperclass().equals(BigDecimalDomainType.class)
          || baseDomainType.getClass().equals(LongDomainType.class)) {
        jrParams.put(paramName, new BigDecimal(params.getString(paramName)));
      } else if (baseDomainType.getClass().equals(BooleanDomainType.class)) {
        jrParams.put(paramName, params.getBoolean(paramName));
      } else { // default
        jrParams.put(paramName, params.getString(paramName));
      }

    }
  }

  /**
   * Method to load the generic parameters that are sent to Jasper Reports.
   * 
   * @param jrParams
   *          the Map instance with all the parameters to be sent to Jasper Reports.
   * @param report
   *          the Report Definition.
   * @param jrTemplatePath
   *          String with the path where the jr template is stored in the server.
   * @param jsonContent
   *          JSONObject with the values set in the filter parameters.
   */
  private void loadReportParams(Map<String, Object> jrParams, ReportDefinition report,
      String jrTemplatePath, JSONObject jsonContent) {

    final int lastSegmentIndex = jrTemplatePath.lastIndexOf("/");
    final String fileDir;
    if (lastSegmentIndex != -1) {
      fileDir = jrTemplatePath.substring(0, lastSegmentIndex + 1);
    } else {
      fileDir = "";
    }
    jrParams.put("SUBREPORT_DIR", fileDir);
    jrParams.put(JASPER_PARAM_PROCESS, report.getProcessDefintion());

    addAdditionalParameters(report, jsonContent, jrParams);
  }

  /**
   * @return the filename of the generated file, it is okay to return null, then a default filename
   *         is generated. NOTE, if you return a value it should include the extension. Also, it can
   *         make sense to use the {@link #getSafeFilename(String)} method to ensure the file name
   *         is valid
   */
  private String getReportFileName(ReportDefinition report, Map<String, Object> parameters,
      ExportType expType) {
    final SimpleDateFormat dateFormat = new SimpleDateFormat(OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("dateTimeFormat.java"));
    return getSafeFilename(
        report.getProcessDefintion().getName() + "-" + dateFormat.format(new Date())) + "."
        + expType.getExtension();
  }

  private static String getSafeFilename(String name) {
    return name.replaceAll("[:\\\\/*?|<>]", "_");
  }

  /**
   * Override this method to put additional parameters to send to the Jasper Report template.
   * Process Definition filter parameters are automatically added.
   * 
   * @param process
   *          the Process Definition of the Report
   * @param jsonContent
   *          values set in the filter parameters
   * @param parameters
   *          the current Parameter Map that it is send to the Jasper Report.
   */
  protected void addAdditionalParameters(ReportDefinition process, JSONObject jsonContent,
      Map<String, Object> parameters) {
  }

  /**
   * Get the data to pass to the report generation method. Override this method to put logic for
   * getting the data. The map received as argument contains parameters that can be used to create
   * some logic to build the report data
   * 
   * @param parameters
   *          map that contains the parameters of the HTTP request and the parameters that will be
   *          sent to the jasper report
   *
   * @return a JRDataSource object containing the report data
   */
  protected JRDataSource getReportData(Map<String, Object> parameters) {
    return null;
  }

  /**
   * Get the connection provider to use in report generation. Override this method to put logic for
   * getting the connection provider
   *
   * @return the ConnectionProvider to use during the report generation
   */
  protected ConnectionProvider getReportConnectionProvider() {
    return null;
  }

  /**
   * This method has no effect. Classes extending {@code BaseReportActionHandler} will always try to
   * compile the sub-reports (if any). Note that the sub-reports to be compiled will be the .jrxml
   * files placed in the same folder as the main report and whose related parameter name starts with
   * <b>SUBREP_</b>.
   * 
   * @return {@code true}
   * 
   * @deprecated This method has no effect
   */
  @Deprecated
  protected boolean isCompilingSubreports() {
    return true;
  }

  private void doJRExport(String jrTemplatePath, ExportType expType, String strFileName,
      Map<String, Object> parameters) {
    ReportSemaphoreHandling.getInstance().acquire();
    @SuppressWarnings("unchecked")
    Map<String, Object> jrParameters = (Map<String, Object>) parameters
        .get(JASPER_REPORT_PARAMETERS);
    Map<Object, Object> localExportParameters = null;
    try {
      if (ExportType.HTML.equals(expType)) {
        // Define the parameter for the URI to display images properly
        localExportParameters = new HashMap<Object, Object>();
        final String localAddress = HttpBaseUtils
            .getLocalAddress(RequestContext.get().getRequest());
        localExportParameters.put(ReportingUtils.IMAGES_URI,
            localAddress + "/servlets/image?image={0}");
      }
      ReportingUtils.exportJR(jrTemplatePath, expType, jrParameters,
          new File(ReportingUtils.getTempFolder(), strFileName), true,
          getReportConnectionProvider(), getReportData(parameters), localExportParameters);
    } finally {
      ReportSemaphoreHandling.getInstance().release();
    }
  }
}
