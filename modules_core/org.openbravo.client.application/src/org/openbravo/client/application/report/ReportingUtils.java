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
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.client.kernel.reference.UIDefinitionController.FormatDefinition;
import org.openbravo.dal.core.DalContextListener;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.JRFormatFactory;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.StringCollectionUtils;
import org.openbravo.model.ad.utility.FileType;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRTextExporter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.fill.JRSwapFileVirtualizer;
import net.sf.jasperreports.engine.util.JRSwapFile;
import net.sf.jasperreports.export.SimpleCsvReportConfiguration;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleHtmlReportConfiguration;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import net.sf.jasperreports.export.SimpleTextExporterConfiguration;
import net.sf.jasperreports.export.SimpleTextReportConfiguration;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXlsReportConfiguration;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import net.sf.jasperreports.export.type.HtmlSizeUnitEnum;
import net.sf.jasperreports.j2ee.servlets.ImageServlet;
import net.sf.jasperreports.web.util.WebHtmlResourceHandler;

/** Utilities to generate jasper reports */
public class ReportingUtils {
  /**
   * Used to set a report parameter with a org.openbravo.dal.core.OBContext which contains the
   * OBContext that has launched the report.
   */
  public static final String JASPER_PARAM_OBCONTEXT = "jasper_obContext";
  /**
   * Used to set a report parameter with an org.hibernate.Session object with the current hibernate.
   * session.
   */
  public static final String JASPER_PARAM_HBSESSION = "jasper_hbSession";
  /**
   * Used to set the parameter with the URI to retrieve images in HTML reports.
   */
  public static final String IMAGES_URI = "Images URI";
  /**
   * Used to set Javascript content inside a PDF report.
   */
  public static final String PDF_JAVASCRIPT = "PDF Javascript";

  private static final float TEXT_CHAR_HEIGHT = 10;
  private static final float TEXT_CHAR_WIDTH = 10;
  private static final Logger log = LogManager.getLogger();
  private static CompiledReportManager compiledReportManager = CompiledReportManager.getInstance();

  /**
   * @see ReportingUtils#exportJR(String, ExportType, Map, File, boolean, ConnectionProvider,
   *      JRDataSource, Map)
   */
  public static void exportJR(String jasperFilePath, ExportType expType,
      Map<String, Object> parameters, String strFileName) throws OBException {
    File target = new File(getTempFolder(), strFileName);
    exportJR(jasperFilePath, expType, parameters, target, true, null, null, null);
  }

  /**
   * @see ReportingUtils#exportJR(String, ExportType, Map, File, boolean, ConnectionProvider,
   *      JRDataSource, Map)
   */
  public static void exportJR(String jasperFilePath, ExportType expType,
      Map<String, Object> parameters, String strFileName, boolean addProcessDefinitionParameters)
      throws OBException {
    File target = new File(getTempFolder(), strFileName);
    exportJR(jasperFilePath, expType, parameters, target, addProcessDefinitionParameters, null,
        null, null);
  }

  /**
   * @see ReportingUtils#exportJR(String, ExportType, Map, File, boolean, ConnectionProvider,
   *      JRDataSource, Map)
   */
  public static void exportJR(String jasperFilePath, ExportType expType,
      Map<String, Object> parameters, String strFileName, boolean addProcessDefinitionParameters,
      ConnectionProvider connectionProvider, JRDataSource data,
      Map<Object, Object> additionalExportParameters) throws OBException {
    File target = new File(getTempFolder(), strFileName);
    exportJR(jasperFilePath, expType, parameters, target, addProcessDefinitionParameters,
        connectionProvider, data, additionalExportParameters);
  }

  /**
   * Exports the report to a file. This method will try to compile the sub-reports (if any). Note
   * that the sub-reports to be compiled will be the .jrxml files placed in the same folder as the
   * main report and whose related parameter name starts with <b>SUBREP_</b>.
   * 
   * @param jasperFilePath
   *          The path to the JR template of the report.
   * @param expType
   *          The desired output type of the report.
   * @param parameters
   *          The parameters to be sent to Jasper Report.
   * @param target
   *          The file used to return the report.
   * @param addProcessDefinitionParameters
   *          A flag to indicate if the parameters needed to print a report from a Process
   *          Definition should be added.
   * @param connectionProvider
   *          A connection provider in case the report needs it.
   * @param data
   *          The data to be used in the report, if required.
   * @param additionalExportParameters
   *          Additional export parameters than can be added to configure the resulting report.
   * @throws OBException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  public static void exportJR(String jasperFilePath, ExportType expType,
      Map<String, Object> parameters, File target, boolean addProcessDefinitionParameters,
      ConnectionProvider connectionProvider, JRDataSource data,
      Map<Object, Object> additionalExportParameters) throws OBException {

    JRSwapFileVirtualizer virtualizer = null;
    Map<Object, Object> exportParameters = new HashMap<>();
    parameters.putAll(expType.getExportParameters());
    if (additionalExportParameters != null && additionalExportParameters.size() > 0) {
      exportParameters.putAll(additionalExportParameters);
    }
    // if no custom virtualizer is requested use a default one
    if (!parameters.containsKey(JRParameter.REPORT_VIRTUALIZER)) {
      // virtualizer is essentially using a tmp-file to avoid huge memory consumption by jasper
      // when processing big reports
      JRSwapFile swap = new JRSwapFile(getTempFolder(), 4096, 1);
      // start using the virtualizer when having more than 100 pages of data
      virtualizer = new JRSwapFileVirtualizer(100, swap);
      parameters.put(JRParameter.REPORT_VIRTUALIZER, virtualizer);
    }
    if (addProcessDefinitionParameters) {
      addProcessDefinitionParameters(parameters);
    }
    JasperPrint jasperPrint = generateJasperPrint(jasperFilePath, parameters, connectionProvider,
        data);
    if (expType == ExportType.HTML) {
      HttpSession session = (HttpSession) parameters.get("HTTP_SESSION");
      if (session != null) {
        session.setAttribute(ImageServlet.DEFAULT_JASPER_PRINT_SESSION_ATTRIBUTE, jasperPrint);
      }
    }
    try {
      saveReport(jasperPrint, expType, exportParameters, target);
    } catch (JRException e) {
      log.error("Error generating Jasper Report {}", jasperFilePath, e);
      throw new OBException(e.getMessage(), e);
    } finally {
      // remove virtualizer tmp files if we created them
      if (virtualizer != null) {
        virtualizer.cleanup();
      }
    }
  }

  /**
   * Exports the report to an output stream. This method will try to compile the sub-reports (if
   * any). Note that the sub-reports to be compiled will be the .jrxml files placed in the same
   * folder as the main report and whose related parameter name starts with <b>SUBREP_</b>.
   * 
   * @param jasperFilePath
   *          The path to the JR template of the report.
   * @param expType
   *          The desired output type of the report.
   * @param parameters
   *          The parameters to be sent to Jasper Report.
   * @param outputStream
   *          An output stream used to return the report.
   * @param addProcessDefinitionParameters
   *          A flag to indicate if the parameters needed to print a report from a Process
   *          Definition should be added.
   * @param connectionProvider
   *          A connection provider in case the report needs it.
   * @param data
   *          The data to be used in the report, if required.
   * @param additionalExportParameters
   *          Additional export parameters than can be added to configure the resulting report.
   * @throws OBException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  public static void exportJR(String jasperFilePath, ExportType expType,
      Map<String, Object> parameters, OutputStream outputStream,
      boolean addProcessDefinitionParameters, ConnectionProvider connectionProvider,
      JRDataSource data, Map<Object, Object> additionalExportParameters) throws OBException {

    JRSwapFileVirtualizer virtualizer = null;
    Map<Object, Object> exportParameters = new HashMap<>();
    parameters.putAll(expType.getExportParameters());
    if (additionalExportParameters != null && additionalExportParameters.size() > 0) {
      exportParameters.putAll(additionalExportParameters);
    }
    // if no custom virtualizer is requested use a default one
    if (!parameters.containsKey(JRParameter.REPORT_VIRTUALIZER)) {
      // virtualizer is essentially using a tmp-file to avoid huge memory consumption by jasper
      // when processing big reports
      JRSwapFile swap = new JRSwapFile(getTempFolder(), 4096, 1);
      // start using the virtualizer when having more than 100 pages of data
      virtualizer = new JRSwapFileVirtualizer(100, swap);
      parameters.put(JRParameter.REPORT_VIRTUALIZER, virtualizer);
    }
    if (addProcessDefinitionParameters) {
      addProcessDefinitionParameters(parameters);
    }
    JasperPrint jasperPrint = generateJasperPrint(jasperFilePath, parameters, connectionProvider,
        data);
    if (expType == ExportType.HTML) {
      HttpSession session = (HttpSession) parameters.get("HTTP_SESSION");
      if (session != null) {
        session.setAttribute(ImageServlet.DEFAULT_JASPER_PRINT_SESSION_ATTRIBUTE, jasperPrint);
      }
    }
    try {
      saveReport(jasperPrint, expType, exportParameters, outputStream);
    } catch (JRException e) {
      log.error("Error generating Jasper Report: " + jasperFilePath, e);
      throw new OBException(e.getMessage(), e);
    } finally {
      // remove virtualizer tmp files if we created them
      if (virtualizer != null) {
        virtualizer.cleanup();
      }
    }
  }

  /**
   * Exports the report to a file.
   * 
   * @deprecated The compileSubreports parameter has no effect. Therefore, use
   *             {@link ReportingUtils#exportJR(String, ExportType, Map, File, boolean, ConnectionProvider, JRDataSource, Map)}
   *             instead.
   */
  @Deprecated
  public static void exportJR(String jasperFilePath, ExportType expType,
      Map<String, Object> parameters, File target, boolean addProcessDefinitionParameters,
      ConnectionProvider connectionProvider, JRDataSource data,
      Map<Object, Object> additionalExportParameters, boolean compileSubreports)
      throws OBException {
    exportJR(jasperFilePath, expType, parameters, target, addProcessDefinitionParameters,
        connectionProvider, data, additionalExportParameters);
  }

  /**
   * Exports the report to an output stream.
   * 
   * @deprecated The compileSubreports parameter has no effect. Therefore, use
   *             {@link ReportingUtils#exportJR(String, ExportType, Map, OutputStream, boolean, ConnectionProvider, JRDataSource, Map)}
   *             instead.
   */
  @Deprecated
  public static void exportJR(String jasperFilePath, ExportType expType,
      Map<String, Object> parameters, OutputStream outputStream,
      boolean addProcessDefinitionParameters, ConnectionProvider connectionProvider,
      JRDataSource data, Map<Object, Object> additionalExportParameters, boolean compileSubreports)
      throws OBException {
    exportJR(jasperFilePath, expType, parameters, outputStream, addProcessDefinitionParameters,
        connectionProvider, data, additionalExportParameters);
  }

  /**
   * Saves a pre-compiled report into a file.
   * 
   * @param jasperPrint
   *          JasperPrint object which contains a compiled report.
   * @param expType
   *          The desired output type of the report.
   * @param exportParameters
   *          Export parameters than can be added to configure the resulting report.
   * @param target
   *          The file used to return the report.
   * @throws JRException
   *           In case there is any error saving the report an exception is thrown with the error
   *           message.
   */
  public static void saveReport(JasperPrint jasperPrint, ExportType expType,
      Map<Object, Object> exportParameters, File target) throws JRException {
    switch (expType) {
      case CSV:
        saveCsvReportToFile(jasperPrint, target);
        break;
      case HTML:
        if (log.isDebugEnabled()) {
          log.debug("JR: Print HTML");
        }
        saveHTMLReportToFile(jasperPrint, exportParameters, target);
        break;
      case PDF:
        JasperExportManager.exportReportToPdfFile(jasperPrint, target.getAbsolutePath());
        break;
      case TXT:
        saveTxtReportToFile(jasperPrint, target);
        break;
      case XLS:
        saveExcelReportToFile(new JRXlsExporter(), jasperPrint, exportParameters, target);
        break;
      case XLSX:
        saveExcelReportToFile(new JRXlsxExporter(), jasperPrint, exportParameters, target);
        break;
      case XML:
        JasperExportManager.exportReportToXmlFile(jasperPrint, target.getAbsolutePath(), true);
        break;
    }
  }

  /**
   * Returns a pre-compiled report into an Output Stream.
   * 
   * @param jasperPrint
   *          JasperPrint object which contains a compiled report.
   * @param expType
   *          The desired output type of the report.
   * @param exportParameters
   *          Export parameters than can be added to configure the resulting report.
   * @param outputStream
   *          The output stream used to return the report.
   * @throws JRException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  public static void saveReport(JasperPrint jasperPrint, ExportType expType,
      Map<Object, Object> exportParameters, OutputStream outputStream) throws JRException {
    switch (expType) {
      case CSV:
        saveCsvReportToOutputStream(jasperPrint, outputStream);
        break;
      case HTML:
        if (log.isDebugEnabled()) {
          log.debug("JR: Print HTML");
        }
        saveHTMLReportToOutputStream(jasperPrint, exportParameters, outputStream);
        break;
      case PDF:
        savePDFReportToOutputStream(jasperPrint, exportParameters, outputStream);
        break;
      case TXT:
        saveTxtReportToOutputStream(jasperPrint, outputStream);
        break;
      case XLS:
        saveExcelReportToOutputStream(new JRXlsExporter(), jasperPrint, exportParameters,
            outputStream);
        break;
      case XLSX:
        saveExcelReportToOutputStream(new JRXlsxExporter(), jasperPrint, exportParameters,
            outputStream);
        break;
      case XML:
        JasperExportManager.exportReportToXmlStream(jasperPrint, outputStream);
        break;
    }
  }

  /**
   * Generates an HTML report from a pre-compiled report and returns it into a file.
   * 
   * @param jasperPrint
   *          JasperPrint object which contains a compiled report.
   * @param exportParameters
   *          Export parameters than can be added to configure the resulting report.
   * @param file
   *          The file used to return the report.
   * @throws JRException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  private static void saveHTMLReportToFile(JasperPrint jasperPrint,
      Map<Object, Object> exportParameters, File file) throws JRException {
    final HtmlExporter htmlExporter = new HtmlExporter();
    SimpleExporterInput exporterInput = new SimpleExporterInput(jasperPrint);
    SimpleHtmlExporterOutput exporterOutput = new SimpleHtmlExporterOutput(file);

    if (exportParameters != null && exportParameters.size() > 0) {
      SimpleHtmlReportConfiguration exportConfiguration = new SimpleHtmlReportConfiguration();
      setHtmlConfigurationFromExportParameters(exportParameters, exportConfiguration,
          exporterOutput);
      htmlExporter.setConfiguration(exportConfiguration);
    } else {
      SimpleHtmlReportConfiguration reportExportConfiguration = new SimpleHtmlReportConfiguration();
      reportExportConfiguration.setSizeUnit(HtmlSizeUnitEnum.POINT);
      htmlExporter.setConfiguration(reportExportConfiguration);
    }
    htmlExporter.setExporterInput(exporterInput);
    htmlExporter.setExporterOutput(exporterOutput);
    htmlExporter.exportReport();
  }

  /**
   * Generates an HTML report from a pre-compiled report and returns it into an output stream.
   * 
   * @param jasperPrint
   *          JasperPrint object which contains a compiled report.
   * @param exportParameters
   *          Export parameters than can be added to configure the resulting report.
   * @param outputStream
   *          The output stream used to return the report.
   * @throws JRException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  private static void saveHTMLReportToOutputStream(JasperPrint jasperPrint,
      Map<Object, Object> exportParameters, OutputStream outputStream) throws JRException {
    final HtmlExporter htmlExporter = new HtmlExporter();
    SimpleExporterInput exporterInput = new SimpleExporterInput(jasperPrint);
    SimpleHtmlExporterOutput exporterOutput = new SimpleHtmlExporterOutput(outputStream);

    if (exportParameters != null && exportParameters.size() > 0) {
      SimpleHtmlReportConfiguration exportConfiguration = new SimpleHtmlReportConfiguration();
      setHtmlConfigurationFromExportParameters(exportParameters, exportConfiguration,
          exporterOutput);
      htmlExporter.setConfiguration(exportConfiguration);
    } else {
      SimpleHtmlReportConfiguration reportExportConfiguration = new SimpleHtmlReportConfiguration();
      reportExportConfiguration.setSizeUnit(HtmlSizeUnitEnum.POINT);
      htmlExporter.setConfiguration(reportExportConfiguration);
    }
    htmlExporter.setExporterInput(exporterInput);
    htmlExporter.setExporterOutput(exporterOutput);
    try {
      htmlExporter.exportReport();
    } catch (Exception e) {
      // Handle the exception to ignore the error thrown when the user closes the browser before
      // getting the whole HTML report
      Throwable t = e.getCause();
      if (t != null && !(t.toString().contains("ClientAbortException"))) {
        throw new JRException(e.getMessage(), e);
      }
    }
  }

  /**
   * Generates an HTML report using the SimpleExporterInput, SimpleHtmlExporterOutput and
   * SimpleHtmlReportConfiguration received as parameters.
   * 
   * @param exporterInput
   *          SimpleExporterInput object with the input data.
   * @param exporterOutput
   *          SimpleHtmlExporterOutput object with the output data.
   * @param exportConfiguration
   *          SimpleHtmlReportConfiguration with the configuration data.
   * @throws JRException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  public static void saveHTMLReport(SimpleExporterInput exporterInput,
      SimpleHtmlExporterOutput exporterOutput, SimpleHtmlReportConfiguration exportConfiguration)
      throws JRException {
    final HtmlExporter htmlExporter = new HtmlExporter();
    htmlExporter.setExporterInput(exporterInput);
    htmlExporter.setExporterOutput(exporterOutput);
    htmlExporter.setConfiguration(exportConfiguration);
    htmlExporter.exportReport();
  }

  /**
   * Generates a PDF report from a pre-compiled report and returns it into an output stream.
   * 
   * @param jasperPrint
   *          JasperPrint object which contains a compiled report.
   * @param exportParameters
   *          Export parameters than can be added to configure the resulting report.
   * @param outputStream
   *          The output stream used to return the report.
   * @throws JRException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  public static void savePDFReportToOutputStream(JasperPrint jasperPrint,
      Map<Object, Object> exportParameters, OutputStream outputStream) throws JRException {
    if (exportParameters != null && exportParameters.size() > 0) {
      final JRPdfExporter exporter = new JRPdfExporter();
      SimpleExporterInput exporterInput = new SimpleExporterInput(jasperPrint);
      SimpleOutputStreamExporterOutput exporterOutput = new SimpleOutputStreamExporterOutput(
          outputStream);
      SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
      String jsContent = (String) exportParameters.get(PDF_JAVASCRIPT);
      if (jsContent != null) {
        configuration.setPdfJavaScript(jsContent);
      }
      exporter.setExporterInput(exporterInput);
      exporter.setExporterOutput(exporterOutput);
      exporter.setConfiguration(configuration);
      exporter.exportReport();
    } else {
      JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
    }
  }

  /**
   * Generates an Excel report from a pre-compiled report and returns it into a file.
   * 
   * @param jasperPrint
   *          JasperPrint object which contains a compiled report.
   * @param exportParameters
   *          Export parameters than can be added to configure the resulting report.
   * @param file
   *          The file used to return the report.
   * @throws JRException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static void saveExcelReportToFile(JRXlsAbstractExporter excelExporter,
      JasperPrint jasperPrint, Map<Object, Object> exportParameters, File file) throws JRException {
    SimpleExporterInput exporterInput = new SimpleExporterInput(jasperPrint);
    SimpleOutputStreamExporterOutput exporterOutput = new SimpleOutputStreamExporterOutput(file);

    if (exportParameters != null && exportParameters.size() > 0) {
      SimpleXlsReportConfiguration exportConfiguration = getXlsConfigurationFromExportParameters(
          exportParameters);
      excelExporter.setConfiguration(exportConfiguration);
    } else {
      SimpleXlsReportConfiguration reportExportConfiguration = new SimpleXlsReportConfiguration();
      reportExportConfiguration.setOnePagePerSheet(false);
      reportExportConfiguration.setRemoveEmptySpaceBetweenRows(true);
      excelExporter.setConfiguration(reportExportConfiguration);
    }
    excelExporter.setExporterInput(exporterInput);
    excelExporter.setExporterOutput(exporterOutput);
    excelExporter.exportReport();
  }

  /**
   * Generates an Excel report from a pre-compiled report and returns it into an output stream.
   * 
   * @param jasperPrint
   *          JasperPrint object which contains a compiled report.
   * @param exportParameters
   *          Export parameters than can be added to configure the resulting report.
   * @param outputStream
   *          The output stream used to return the report.
   * @throws JRException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static void saveExcelReportToOutputStream(JRXlsAbstractExporter excelExporter,
      JasperPrint jasperPrint, Map<Object, Object> exportParameters, OutputStream outputStream)
      throws JRException {
    SimpleExporterInput exporterInput = new SimpleExporterInput(jasperPrint);
    SimpleOutputStreamExporterOutput exporterOutput = new SimpleOutputStreamExporterOutput(
        outputStream);

    if (exportParameters != null && exportParameters.size() > 0) {
      SimpleXlsReportConfiguration exportConfiguration = getXlsConfigurationFromExportParameters(
          exportParameters);
      excelExporter.setConfiguration(exportConfiguration);
    } else {
      SimpleXlsReportConfiguration reportExportConfiguration = new SimpleXlsReportConfiguration();
      reportExportConfiguration.setOnePagePerSheet(false);
      reportExportConfiguration.setRemoveEmptySpaceBetweenRows(true);
      excelExporter.setConfiguration(reportExportConfiguration);
    }
    excelExporter.setExporterInput(exporterInput);
    excelExporter.setExporterOutput(exporterOutput);
    excelExporter.exportReport();
  }

  /**
   * Generates an XLS report using the SimpleExporterInput, SimpleOutputStreamExporterOutput and
   * SimpleXlsReportConfiguration received as parameters.
   * 
   * @param exporterInput
   *          SimpleExporterInput object with the input data.
   * @param exporterOutput
   *          SimpleOutputStreamExporterOutput object with the output data.
   * @param exportConfiguration
   *          SimpleXlsReportConfiguration with the configuration data.
   * @throws JRException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  public static void saveExcelReport(SimpleExporterInput exporterInput,
      SimpleOutputStreamExporterOutput exporterOutput,
      SimpleXlsReportConfiguration exportConfiguration) throws JRException {
    final JRXlsExporter excelExporter = new JRXlsExporter();
    excelExporter.setExporterInput(exporterInput);
    excelExporter.setExporterOutput(exporterOutput);
    excelExporter.setConfiguration(exportConfiguration);
    excelExporter.exportReport();
  }

  /**
   * Generates an XLSX report using the SimpleExporterInput, SimpleOutputStreamExporterOutput and
   * SimpleXlsxReportConfiguration received as parameters.
   * 
   * @param exporterInput
   *          SimpleExporterInput object with the input data.
   * @param exporterOutput
   *          SimpleOutputStreamExporterOutput object with the output data.
   * @param exportConfiguration
   *          SimpleXlsxReportConfiguration with the configuration data.
   * @throws JRException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  public static void saveExcelReport(SimpleExporterInput exporterInput,
      SimpleOutputStreamExporterOutput exporterOutput,
      SimpleXlsxReportConfiguration exportConfiguration) throws JRException {
    final JRXlsxExporter excelExporter = new JRXlsxExporter();
    excelExporter.setExporterInput(exporterInput);
    excelExporter.setExporterOutput(exporterOutput);
    excelExporter.setConfiguration(exportConfiguration);
    excelExporter.exportReport();
  }

  /**
   * Generates a CSV report from a pre-compiled report and returns it into a file.
   * 
   * @param jasperPrint
   *          JasperPrint object which contains a compiled report.
   * @param file
   *          The file used to return the report.
   * @throws JRException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  private static void saveCsvReportToFile(JasperPrint jasperPrint, File file) throws JRException {
    final JRCsvExporter csvExporter = new JRCsvExporter();
    SimpleExporterInput exporterInput = new SimpleExporterInput(jasperPrint);
    SimpleWriterExporterOutput exporterOutput = new SimpleWriterExporterOutput(file);

    csvExporter.setConfiguration(new SimpleCsvReportConfiguration());
    csvExporter.setExporterInput(exporterInput);
    csvExporter.setExporterOutput(exporterOutput);
    csvExporter.exportReport();
  }

  /**
   * Generates a CSV report from a pre-compiled report and returns it into an output stream.
   * 
   * @param jasperPrint
   *          JasperPrint object which contains a compiled report.
   * @param outputStream
   *          The output stream used to return the report.
   * @throws JRException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  private static void saveCsvReportToOutputStream(JasperPrint jasperPrint,
      OutputStream outputStream) throws JRException {
    final JRCsvExporter csvExporter = new JRCsvExporter();
    SimpleExporterInput exporterInput = new SimpleExporterInput(jasperPrint);
    SimpleWriterExporterOutput exporterOutput = new SimpleWriterExporterOutput(outputStream);

    csvExporter.setConfiguration(new SimpleCsvReportConfiguration());
    csvExporter.setExporterInput(exporterInput);
    csvExporter.setExporterOutput(exporterOutput);
    csvExporter.exportReport();
  }

  /**
   * Generates a CSV report using the SimpleExporterInput, SimpleWriterExporterOutput and
   * SimpleCsvReportConfiguration received as parameters.
   * 
   * @param exporterInput
   *          SimpleExporterInput object with the input data.
   * @param exporterOutput
   *          SimpleWriterExporterOutput object with the output data.
   * @param exportConfiguration
   *          SimpleCsvReportConfiguration with the configuration data.
   * @throws JRException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  public static void saveCsvReport(SimpleExporterInput exporterInput,
      SimpleWriterExporterOutput exporterOutput, SimpleCsvReportConfiguration exportConfiguration)
      throws JRException {
    final JRCsvExporter csvExporter = new JRCsvExporter();
    csvExporter.setExporterInput(exporterInput);
    csvExporter.setExporterOutput(exporterOutput);
    csvExporter.setConfiguration(exportConfiguration);
    csvExporter.exportReport();
  }

  /**
   * Generates a plain text report from a pre-compiled report and returns it into a file.
   * 
   * @param jasperPrint
   *          JasperPrint object which contains a compiled report.
   * @param file
   *          The file used to return the report.
   * @throws JRException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  private static void saveTxtReportToFile(JasperPrint jasperPrint, File file) throws JRException {
    final JRTextExporter textExporter = new JRTextExporter();
    SimpleExporterInput exporterInput = new SimpleExporterInput(jasperPrint);
    SimpleWriterExporterOutput exporterOutput = new SimpleWriterExporterOutput(file);

    // Default text configuration that can be overridden in the .jrxml template itself
    SimpleTextExporterConfiguration textExporterConfiguration = new SimpleTextExporterConfiguration();
    textExporterConfiguration.setOverrideHints(false);
    textExporter.setConfiguration(textExporterConfiguration);
    // Default item text configuration that can be overridden in the .jrxml template itself
    SimpleTextReportConfiguration textReportConfiguration = new SimpleTextReportConfiguration();
    textReportConfiguration.setCharHeight(TEXT_CHAR_HEIGHT);
    textReportConfiguration.setCharWidth(TEXT_CHAR_WIDTH);
    textReportConfiguration.setOverrideHints(false);
    textExporter.setConfiguration(textReportConfiguration);

    textExporter.setExporterInput(exporterInput);
    textExporter.setExporterOutput(exporterOutput);
    textExporter.exportReport();
  }

  /**
   * Generates a plain text report from a pre-compiled report and returns it into an output stream.
   * 
   * @param jasperPrint
   *          JasperPrint object which contains a compiled report.
   * @param outputStream
   *          The output stream used to return the report.
   * @throws JRException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  private static void saveTxtReportToOutputStream(JasperPrint jasperPrint,
      OutputStream outputStream) throws JRException {
    final JRTextExporter textExporter = new JRTextExporter();
    SimpleExporterInput exporterInput = new SimpleExporterInput(jasperPrint);
    SimpleWriterExporterOutput exporterOutput = new SimpleWriterExporterOutput(outputStream);

    // Default text configuration that can be overridden in the .jrxml template itself
    SimpleTextExporterConfiguration textExporterConfiguration = new SimpleTextExporterConfiguration();
    textExporterConfiguration.setOverrideHints(false);
    textExporter.setConfiguration(textExporterConfiguration);
    // Default item text configuration that can be overridden in the .jrxml template itself
    SimpleTextReportConfiguration textReportConfiguration = new SimpleTextReportConfiguration();
    textReportConfiguration.setCharHeight(TEXT_CHAR_HEIGHT);
    textReportConfiguration.setCharWidth(TEXT_CHAR_WIDTH);
    textReportConfiguration.setOverrideHints(false);
    textExporter.setConfiguration(textReportConfiguration);

    textExporter.setExporterInput(exporterInput);
    textExporter.setExporterOutput(exporterOutput);
    textExporter.exportReport();
  }

  /**
   * Generates a plain text report using the SimpleExporterInput, SimpleWriterExporterOutput,
   * SimpleTextExporterConfiguration and SimpleTextReportConfiguration received as parameters.
   * 
   * @param exporterInput
   *          SimpleExporterInput object with the input data.
   * @param exporterOutput
   *          SimpleWriterExporterOutput object with the output data.
   * @param textExporterConfiguration
   *          SimpleTextExporterConfiguration with the configuration data.
   * @param textReportConfiguration
   *          SimpleTextReportConfiguration with the item configuration data.
   * @throws JRException
   *           In case there is any error generating the report an exception is thrown with the
   *           error message.
   */
  public static void saveTxtReport(SimpleExporterInput exporterInput,
      SimpleWriterExporterOutput exporterOutput,
      SimpleTextExporterConfiguration textExporterConfiguration,
      SimpleTextReportConfiguration textReportConfiguration) throws JRException {
    final JRTextExporter textExporter = new JRTextExporter();
    textExporter.setExporterInput(exporterInput);
    textExporter.setExporterOutput(exporterOutput);
    textExporter.setConfiguration(textExporterConfiguration);
    textExporter.setConfiguration(textReportConfiguration);
    textExporter.exportReport();
  }

  /**
   * Generates a SimpleXlsReportConfiguration from a parameter map.
   * 
   * This method allows backwards compatibility when generating XLS reports by using a parameter map
   * to define the export configuration. The usage of this parameter map is deprecated in the Jasper
   * Reports library to use the SimpleXlsReportConfiguration class instead.
   * 
   * @param params
   *          A parameter map with the export parameters.
   * @return A SimpleXlsReportConfiguration object with the resulting configuration.
   * 
   */
  @SuppressWarnings("deprecation")
  private static SimpleXlsReportConfiguration getXlsConfigurationFromExportParameters(
      Map<Object, Object> params) {
    SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
    for (Entry<Object, Object> pair : params.entrySet()) {
      Object key = pair.getKey();
      String parameter;
      if (key instanceof net.sf.jasperreports.engine.JRExporterParameter) {
        parameter = ((net.sf.jasperreports.engine.JRExporterParameter) key).toString();
      } else if (key instanceof String) {
        parameter = (String) key;
      } else {
        parameter = "";
      }
      if (parameter.equals("Is One Page per Sheet")) {
        configuration.setOnePagePerSheet((Boolean) pair.getValue());
      } else if (parameter.equals("Is Remove Empty Space Between Rows")) {
        configuration.setRemoveEmptySpaceBetweenRows((Boolean) pair.getValue());
      } else if (parameter.equals("Is Remove Empty Space Between Columns")) {
        configuration.setRemoveEmptySpaceBetweenColumns((Boolean) pair.getValue());
      } else if (parameter.equals("Is White Page Background")) {
        configuration.setWhitePageBackground((Boolean) pair.getValue());
      } else if (parameter.equals("Is Detect Cell Type")) {
        configuration.setDetectCellType((Boolean) pair.getValue());
      } else if (parameter.equals("Is Font Size Fix Enabled")) {
        configuration.setFontSizeFixEnabled((Boolean) pair.getValue());
      } else if (parameter.equals("Is Image Border Fix Enabled")) {
        configuration.setImageBorderFixEnabled((Boolean) pair.getValue());
      } else if (parameter.equals("Is Ignore Graphics")) {
        configuration.setIgnoreGraphics((Boolean) pair.getValue());
      } else if (parameter.equals("Is Collapse Row Span")) {
        configuration.setCollapseRowSpan((Boolean) pair.getValue());
      } else if (parameter.equals("Is Ignore Cell Border")) {
        configuration.setIgnoreCellBorder((Boolean) pair.getValue());
      } else if (parameter.equals("Is Ignore Cell Background")) {
        configuration.setIgnoreCellBackground((Boolean) pair.getValue());
      } else if (parameter.equals("Maximum Rows Per Sheet")) {
        configuration.setMaxRowsPerSheet((Integer) pair.getValue());
      } else if (parameter.equals("Ignore page margins")) {
        configuration.setIgnorePageMargins((Boolean) pair.getValue());
      } else {
        log.warn("Unknown XLS export configuration parameter {}", parameter);
      }
    }
    return configuration;
  }

  /**
   * Configures a SimpleHtmlReportConfiguration and a SimpleHtmlExporterOutput from a parameter map.
   * 
   * This method allows backwards compatibility when generating HTML reports by using a parameter
   * map to define the export configuration. The usage of this parameter map is deprecated in the
   * Jasper Reports library to use the SimpleHtmlReportConfiguration and SimpleHtmlExporterOutput
   * classes instead.
   * 
   * @param params
   *          A parameter map with the export parameters.
   * @param configuration
   *          The SimpleHtmlReportConfiguration object generated based on the parameter map.
   * @param exporterOutput
   *          The SimpleHtmlExporterOutput object generated based on the parameter map.
   * 
   */
  @SuppressWarnings("deprecation")
  private static void setHtmlConfigurationFromExportParameters(Map<Object, Object> params,
      SimpleHtmlReportConfiguration configuration, SimpleHtmlExporterOutput exporterOutput) {
    // Add configuration defaults
    // This is needed just in case the params map only contains the Images URI parameter
    configuration.setSizeUnit(HtmlSizeUnitEnum.POINT);
    for (Entry<Object, Object> pair : params.entrySet()) {
      Object key = pair.getKey();
      String parameter;
      if (key instanceof net.sf.jasperreports.engine.JRExporterParameter) {
        parameter = ((net.sf.jasperreports.engine.JRExporterParameter) key).toString();
      } else if (key instanceof String) {
        parameter = (String) key;
      } else {
        parameter = "";
      }
      if (parameter.equals(IMAGES_URI)) {
        exporterOutput.setImageHandler(new WebHtmlResourceHandler((String) pair.getValue()));
      } else if (parameter.equals("Is Remove Empty Space Between Rows")) {
        configuration.setRemoveEmptySpaceBetweenRows((Boolean) pair.getValue());
      } else if (parameter.equals("Size Unit")) {
        String sizeUnit = (String) pair.getValue();
        if (HtmlSizeUnitEnum.POINT.getName().equals(sizeUnit)) {
          configuration.setSizeUnit(HtmlSizeUnitEnum.POINT);
        } else if (HtmlSizeUnitEnum.PIXEL.getName().equals(sizeUnit)) {
          configuration.setSizeUnit(HtmlSizeUnitEnum.PIXEL);
        }
      } else if (parameter.equals("Ignore page margins")) {
        configuration.setIgnorePageMargins((Boolean) pair.getValue());
      } else if (parameter.equals("Is White Page Background")) {
        configuration.setWhitePageBackground((Boolean) pair.getValue());
      } else if (parameter.equals("Is Wrap Break Word")) {
        configuration.setWrapBreakWord((Boolean) pair.getValue());
      } else if (parameter.equals("Zoom Ratio")) {
        configuration.setZoomRatio((Float) pair.getValue());
      } else {
        log.warn("Unknown HTML export configuration parameter {}", parameter);
      }
    }
  }

  /**
   * Generates a compiled, translated and filled report into a JasperPrint object.
   * 
   * @deprecated The compileSubreports parameter has no effect. Therefore, use
   *             {@link ReportingUtils#generateJasperPrint(String, Map, ConnectionProvider, JRDataSource)}
   *             instead.
   */
  @Deprecated
  public static JasperPrint generateJasperPrint(String jasperFilePath,
      Map<String, Object> parameters, boolean compileSubreports,
      ConnectionProvider connectionProvider, JRDataSource data) throws OBException {
    return generateJasperPrint(jasperFilePath, parameters, connectionProvider, data);
  }

  /**
   * Generates a compiled, translated and filled report into a JasperPrint object.
   * 
   * @param jasperFilePath
   *          The path to the JR template of the report.
   * @param parameters
   *          The parameters to be sent to Jasper Report.
   * @param connectionProvider
   *          A connection provider in case the report needs it.
   * @param data
   *          The data to be used in the report, if required.
   * @return A JasperPrint object with the compiled, translated and filled report.
   * @throws OBException
   *           In case there is any error processing the report an exception is thrown with the
   *           error message.
   */
  public static JasperPrint generateJasperPrint(String jasperFilePath,
      Map<String, Object> parameters, ConnectionProvider connectionProvider, JRDataSource data)
      throws OBException {
    long t1 = System.currentTimeMillis();
    try {
      setReportFormatFactory(parameters);

      log.debug("Parameters for report {}: {}", jasperFilePath, parameters);

      if (!jasperFilePath.endsWith("jrxml")) {
        ReportFiller reportFiller = new ReportFiller(jasperFilePath, parameters);
        return reportFiller.fillReport();
      }

      String language = OBContext.getOBContext().getLanguage().getLanguage();
      JasperReport jReport;

      jReport = compiledReportManager.compileReportWithSubreports(jasperFilePath, language,
          parameters, connectionProvider);

      ReportFiller reportFiller = new ReportFiller(jReport, parameters);
      if (connectionProvider != null) {
        reportFiller.setConnectionProvider(connectionProvider);
      }
      if (data != null) {
        reportFiller.setJRDataSource(data);
      }
      return reportFiller.fillReport();
    } catch (JRException e) {
      log.error("Error generating Jasper Report {}", jasperFilePath, e);
      throw new OBException(e.getMessage(), e);
    } finally {
      log.debug("JasperPrint for {} generated in {} ms", jasperFilePath,
          (System.currentTimeMillis() - t1));
    }
  }

  /**
   * @deprecated Use {@link #getTranslatedJasperReport(ConnectionProvider, String, String)} instead.
   */
  @Deprecated
  public static JasperReport getTranslatedJasperReport(ConnectionProvider conn, String reportName,
      String language, String baseDesignPath) throws JRException {
    return getTranslatedJasperReport(conn, reportName, language);
  }

  /**
   * Generates a compiled and translated report to the language passed as parameter.
   * 
   * @param conn
   *          The connection provider used to get the translations.
   * @param reportName
   *          The path to the JR template of the report.
   * @param language
   *          Language to be used when generating the report.
   * @return A JasperReport object with the compiled and translated report.
   * @throws JRException
   *           In case there is any error generating the translated report an exception is thrown
   *           with the error message.
   */
  public static JasperReport getTranslatedJasperReport(ConnectionProvider conn, String reportName,
      String language) throws JRException {
    return compiledReportManager.compileReport(reportName, language, conn);
  }

  /**
   * Compiles a jrxml file generating a JasperReport object.
   * 
   * @param jasperFilePath
   *          The path to the JR template of the report.
   * @return A JasperReport object with the compiled report.
   * @throws JRException
   *           In case there is any error compiling the report an exception is thrown with the error
   *           message.
   */
  public static JasperReport compileReport(String jasperFilePath) throws JRException {
    return compiledReportManager.compileReport(jasperFilePath, null, null);
  }

  /**
   * @see ReportingUtils#concatPDFReport(List, boolean, OutputStream,
   *      SimplePdfExporterConfiguration)
   */
  public static void concatPDFReport(List<JasperPrint> jasperPrintList, boolean createBookmarks,
      OutputStream outputStream) throws JRException {
    concatPDFReport(jasperPrintList, createBookmarks, outputStream, null);
  }

  /**
   * Returns a PDF file into an output stream as result of the concatenation of the JasperPrint
   * objects list passed as parameter.
   * 
   * @param jasperPrintList
   *          A list of JasperPrint objects.
   * @param createBookmarks
   *          A flag to indicate if the document should contain bookmarks, to mark the beginning of
   *          each individual document that was part of the initial document list.
   * @param outputStream
   *          The output stream used for returning the report.
   * @param reportConfiguration
   *          An optional configuration for the report.
   * @throws JRException
   *           In case there is any error compiling the report an exception is thrown with the error
   *           message.
   */
  public static void concatPDFReport(List<JasperPrint> jasperPrintList, boolean createBookmarks,
      OutputStream outputStream, SimplePdfExporterConfiguration reportConfiguration)
      throws JRException {

    JRPdfExporter exporter = new JRPdfExporter();
    SimpleOutputStreamExporterOutput exporterOutput = new SimpleOutputStreamExporterOutput(
        outputStream);
    SimplePdfExporterConfiguration configuration = reportConfiguration != null ? reportConfiguration
        : new SimplePdfExporterConfiguration();

    configuration.setCreatingBatchModeBookmarks(createBookmarks);
    exporter.setConfiguration(configuration);
    exporter.setExporterInput(SimpleExporterInput.getInstance(jasperPrintList));
    exporter.setExporterOutput(exporterOutput);

    exporter.exportReport();
  }

  /**
   * Returns an encrypted PDF file into an output stream as result of the concatenation of the
   * JasperPrint objects list passed as parameter.
   * 
   * @param jasperPrintList
   *          A list of JasperPrint objects.
   * @param createBookmarks
   *          A flag to indicate if the document should contain bookmarks, to mark the beginning of
   *          each individual document that was part of the initial document list.
   * @param userPassword
   *          A String that contains the user password of the resulting document.
   * @param ownerPassword
   *          A String that contains the owner password of the resulting document.
   * @param outputStream
   *          The output stream used for returning the report.
   * @throws JRException
   *           In case there is any error compiling the report an exception is thrown with the error
   *           message.
   */
  public static void concatPDFReportEncrypted(List<JasperPrint> jasperPrintList,
      boolean createBookmarks, String userPassword, String ownerPassword, OutputStream outputStream)
      throws JRException {

    JRPdfExporter exporter = new JRPdfExporter();
    SimpleOutputStreamExporterOutput exporterOutput = new SimpleOutputStreamExporterOutput(
        outputStream);
    SimplePdfExporterConfiguration reportConfiguration = new SimplePdfExporterConfiguration();

    reportConfiguration.setEncrypted(true);
    reportConfiguration.set128BitKey(true);
    reportConfiguration.setUserPassword(userPassword);
    reportConfiguration.setOwnerPassword(ownerPassword);
    reportConfiguration.setCreatingBatchModeBookmarks(createBookmarks);
    exporter.setConfiguration(reportConfiguration);
    exporter.setExporterInput(SimpleExporterInput.getInstance(jasperPrintList));
    exporter.setExporterOutput(exporterOutput);

    exporter.exportReport();
  }

  /**
   * Adds the parameters needed to print a report from a standard process definition into the
   * parameter map.
   * 
   * @param parameters
   *          Map of parameters where the standard process definition parameters are added.
   */
  private static void addProcessDefinitionParameters(Map<String, Object> parameters) {
    parameters.put(JASPER_PARAM_HBSESSION, OBDal.getReadOnlyInstance().getSession());
    parameters.put(JASPER_PARAM_OBCONTEXT, OBContext.getOBContext());

    FormatDefinition amountFormat = UIDefinitionController.getInstance()
        .getFormatDefinition("amount", UIDefinitionController.NORMALFORMAT_QUALIFIER);
    addFormatParameter(parameters, amountFormat, "AMOUNTFORMAT");

    FormatDefinition generalQtyFormat = UIDefinitionController.getInstance()
        .getFormatDefinition("generalQty", UIDefinitionController.SHORTFORMAT_QUALIFIER);
    addFormatParameter(parameters, generalQtyFormat, "QUANTITYFORMAT");

    String strClientId = OBContext.getOBContext().getCurrentClient().getId();
    parameters.put("Current_Client_ID", strClientId);
    String strOrgs = StringCollectionUtils
        .commaSeparated(OBContext.getOBContext().getReadableOrganizations());
    parameters.put("Readable_Organizations", strOrgs);
  }

  private static void addFormatParameter(Map<String, Object> parameters,
      FormatDefinition reportFormat, String parameterName) {
    final DecimalFormatSymbols dfs = new DecimalFormatSymbols();
    dfs.setDecimalSeparator(reportFormat.getDecimalSymbol().charAt(0));
    dfs.setGroupingSeparator(reportFormat.getGroupingSymbol().charAt(0));

    final DecimalFormat numberFormat = new DecimalFormat(
        correctMaskForGrouping(reportFormat.getFormat(), reportFormat.getDecimalSymbol(),
            reportFormat.getGroupingSymbol()),
        dfs);
    parameters.put(parameterName, numberFormat);
  }

  /**
   * Adds the date format configuration into the parameter map.
   * 
   * @param parameters
   *          Map of parameters where the date format configuration is put.
   */
  private static void setReportFormatFactory(Map<String, Object> parameters) {
    String javaDateFormat = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("dateFormat.java");
    if (log.isDebugEnabled()) {
      log.debug("creating the format factory: " + javaDateFormat);
    }
    final JRFormatFactory jrFormatFactory = new JRFormatFactory();
    jrFormatFactory.setDatePattern(javaDateFormat);
    parameters.put(JRParameter.REPORT_FORMAT_FACTORY, jrFormatFactory);
  }

  /**
   * Changes the mask to use "." as Decimal Symbol and "," as grouping symbol.
   * 
   * @param mask
   *          the current mask.
   * @param decimalSymbol
   *          the current decimal symbol.
   * @param groupingSymbol
   *          the current grouping symbol.
   * @return the mask with the updated decimal and grouping symbols.
   */
  private static String correctMaskForGrouping(String mask, String decimalSymbol,
      String groupingSymbol) {
    final String strTmpDecSymbol = "xxTmpDecSymbolxx";
    String localMask = mask.replace(decimalSymbol, strTmpDecSymbol);
    localMask = localMask.replace(groupingSymbol, ",");
    return localMask.replace(strTmpDecSymbol, ".");
  }

  /**
   * Returns the format to be used when exporting Excel reports.
   * 
   * @return a ExportType with the default Excel format (XLS or XLSX).
   */
  public static ExportType getExcelExportType() {
    try {
      OBContext.setAdminMode(true);
      try {
        OBContext context = OBContext.getOBContext();
        String preferenceValue = Preferences.getPreferenceValue("ExcelExportFormat", true,
            context.getCurrentClient(), context.getCurrentOrganization(), context.getUser(),
            context.getRole(), null);
        ExportType exportType = ExportType.getExportType(preferenceValue);
        if (ExportType.XLS == exportType || ExportType.XLSX == exportType) {
          return exportType;
        }
      } catch (Exception ignore) {
        // preference not found: return default excel format
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return ExportType.XLSX;
  }

  /**
   * enum with the supported Export Outputs. Includes custom properties to be used when the report
   * is generated.
   */
  public enum ExportType {
    // When IS_IGNORE_PAGINATION is set to true, the report-filling engine will generate the
    // document on a single, very long page. Running the generated report to any output format, a
    // single page document will be visualized. This is the desired behavior for some document
    // types, such as HTML, XML, XLS or XLSX.
    // This flag should be false for documents with page-oriented layout, like PDF, to allow the
    // possibility to navigate between pages.
    /**
     * CSV export type
     */
    @SuppressWarnings("serial")
    CSV("csv", "", new HashMap<String, Object>() {
      {
        put("IS_IGNORE_PAGINATION", true);
      }
    }), //
    /**
     * HTML export type
     */
    @SuppressWarnings("serial")
    HTML("html", "", new HashMap<String, Object>() {
      {
        put("IS_IGNORE_PAGINATION", true);
      }
    }), //
    /**
     * PDF export type
     */
    @SuppressWarnings("serial")
    PDF("pdf", "103", new HashMap<String, Object>() {
      {
        put("IS_IGNORE_PAGINATION", false);
      }
    }), //
    /**
     * TXT export type
     */
    @SuppressWarnings("serial")
    TXT("txt", "100", new HashMap<String, Object>() {
      {
        put("IS_IGNORE_PAGINATION", true);
      }
    }), //
    /**
     * XLS export type
     */
    @SuppressWarnings("serial")
    XLS("xls", "101", new HashMap<String, Object>() {
      {
        put("IS_IGNORE_PAGINATION", true);
      }
    }), //
    /**
     * XLSX export type
     */
    @SuppressWarnings("serial")
    XLSX("xlsx", "", new HashMap<String, Object>() {
      {
        put("IS_IGNORE_PAGINATION", true);
      }
    }), //
    /**
     * XML export type
     */
    @SuppressWarnings("serial")
    XML("xml", "800004", new HashMap<String, Object>() {
      {
        put("IS_IGNORE_PAGINATION", true);
      }
    });
    private final String extension;
    private final String contentType;
    private final Map<String, Object> params;

    ExportType(String extension, String strFileTypeId, Map<String, Object> params) {
      this.extension = extension;
      this.contentType = getContentType(strFileTypeId);
      this.params = params;
    }

    private String getContentType(String fileTypeId) {
      OBContext.setAdminMode(true);
      try {
        FileType type = OBDal.getReadOnlyInstance().get(FileType.class, fileTypeId);
        if (type != null) {
          return type.getFormat();
        } else if ("html".equals(extension) || "csv".equals(extension)) {
          return "text/" + extension;
        } else if ("xlsx".equals(extension)) {
          return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
          return "application/" + extension;
        }
      } finally {
        OBContext.restorePreviousMode();
      }
    }

    /**
     * @return a String with the extension type.
     */
    public String getExtension() {
      return this.extension;
    }

    /**
     * @return a String with the content type.
     */
    public String getContentType() {
      return contentType;
    }

    /**
     * @return a map with the configuration parameters.
     */
    public Map<String, Object> getExportParameters() {
      // An instance of the Map is done for making sure
      // that if this method is called, it is only accessing
      // to the parameters of the current Map instance.
      return new HashMap<>(params);
    }

    /**
     * Returns the corresponding ExportType item based on the action. This method is
     * case-insensitive.
     * 
     * @param action
     *          a String that defines the export type.
     * @return the ExportType associated to the input String parameter.
     * @throws OBException
     *           In case the input String parameter does not correspond with any valid ExportType,
     *           an exception is thrown with the error message.
     */
    public static ExportType getExportType(String action) throws OBException {
      if (ExportType.CSV.hasExtension(action)) {
        return ExportType.CSV;
      } else if (ExportType.HTML.hasExtension(action)) {
        return ExportType.HTML;
      } else if (ExportType.PDF.hasExtension(action)) {
        return ExportType.PDF;
      } else if (ExportType.TXT.hasExtension(action)) {
        return ExportType.TXT;
      } else if (ExportType.XLS.hasExtension(action)) {
        return ExportType.XLS;
      } else if (ExportType.XLSX.hasExtension(action)) {
        return ExportType.XLSX;
      } else if (ExportType.XML.hasExtension(action)) {
        return ExportType.XML;
      } else {
        throw new OBException(
            OBMessageUtils.getI18NMessage("OBUIAPP_UnsupportedAction", new String[] { action }));
      }
    }

    /**
     * Checks if the extension of the the ExportType is the same as the one passed as parameter.
     * This method is case-insensitive.
     * 
     * @param fileExtension
     *          a String containing a file extension name.
     * @return true if the extension of the ExportType is the same as the one passed as parameter,
     *         false otherwise.
     */
    public boolean hasExtension(String fileExtension) {
      if (StringUtils.isEmpty(fileExtension)) {
        return false;
      }
      return extension.equalsIgnoreCase(fileExtension);
    }

    /**
     * Checks if temporary file name is a valid one: has extension and the name is a uuid.
     * 
     * @param tmpFileName
     *          a String with the file name.
     * 
     * @return true if the the temporary file name is valid, false otherwise.
     */
    public boolean isValidTemporaryFileName(String tmpFileName) {
      if (!tmpFileName.endsWith("." + getExtension())) {
        // file name should end with the extension
        return false;
      }
      final String tmpFileNameWithoutExtension = tmpFileName.substring(0,
          tmpFileName.length() - getExtension().length() - 1);

      // temp file must be a valid uuid
      return tmpFileNameWithoutExtension.matches("[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}");
    }
  }

  /**
   * Returns temporary directory to save generated reports.
   * 
   * @return a String with the temporary directory location.
   */
  public static String getTempFolder() {
    return System.getProperty("java.io.tmpdir");
  }

  /**
   * Returns the relative path to the default design sub-folder within the context's base design
   * folder.
   * 
   * @return a String with the relative path to the default design sub-folder.
   */
  public static String getBaseDesign() {
    ServletContext servletContext = DalContextListener.getServletContext();
    ConfigParameters configParameters = ConfigParameters.retrieveFrom(servletContext);

    String base = configParameters.strBaseDesignPath;
    String design = configParameters.strDefaultDesignPath;

    if (!base.startsWith("/")) {
      base = "/" + base;
    }
    return base + "/" + design;
  }
}
