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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.report;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.DalContextListener;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.uiTranslation.TranslationHandler;
import org.openbravo.utils.Replace;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

/**
 * This class is used to compile reports. It also provides the ability to compile the sub-reports
 * passed as parameters within the report design.
 */
class ReportCompiler {
  private static final Logger log = LogManager.getLogger();

  // connection provider used to retrieve the translated texts
  private ConnectionProvider connectionProvider;
  // main report file path
  private String templatePath;
  // main report file location
  private String templateLocation;
  // language report
  private String language;
  // main report design
  private JasperDesign mainDesign;
  // server base design folder
  private String baseDesignPath;

  ReportCompiler(String templatePath, String language, ConnectionProvider connectionProvider) {
    this.templatePath = templatePath;
    this.templateLocation = new File(templatePath).getParent() + "/";
    this.baseDesignPath = getBaseDesignPath();
    this.language = language;
    this.connectionProvider = connectionProvider;
  }

  private String getBaseDesignPath() {
    ServletContext servletContext = DalContextListener.getServletContext();
    return servletContext.getRealPath(ReportingUtils.getBaseDesign());
  }

  /**
   * @return a JasperReport with the result of the compilation for the main report.
   * @throws JRException
   *           In case any error occurs during the report compilation.
   */
  JasperReport compileReport() throws JRException {
    return compileJasperReport(getMainDesign());
  }

  /**
   * Generates and returns the sub-reports by retrieving them from the report parameter list. At
   * present this process assumes the subreport is a .jrxml file. It does not support the
   * possibility that this subreport file could be a .jasper file.
   * 
   * @param provider
   *          A connection provider in case sub-reports need it.
   * @return a Map containing the compiled sub-reports. The keys of the map are the name of the
   *         parameter that references to each sub-report.
   * @throws OBException
   *           In case there is any error generating the sub-reports an exception is thrown with the
   *           error message.
   */
  Map<String, JasperReport> compileSubReports(ConnectionProvider provider) throws OBException {
    ConnectionProvider cp = provider != null ? provider
        : DalConnectionProvider.getReadOnlyConnectionProvider();
    try {
      Map<String, JasperReport> compiledSubReports = new HashMap<>();
      for (Object parameterObj : getMainDesign().getParametersList()) {
        final JRDesignParameter parameter = (JRDesignParameter) parameterObj;
        if (parameter.getName().startsWith("SUBREP_")) {
          String parameterName = parameter.getName();
          String subReportName = Replace.replace(parameterName, "SUBREP_", "") + ".jrxml";
          JasperDesign jasperDesign = getJasperDesign(cp, templateLocation + subReportName);
          JasperReport jasperReportLines = compileJasperReport(jasperDesign);
          compiledSubReports.put(parameterName, jasperReportLines);
        }
      }
      return compiledSubReports;
    } catch (final JRException e) {
      log.error("Error processing subreports for template {}", templatePath, e);
      throw new OBException(e.getMessage(), e);
    }
  }

  private JasperDesign getMainDesign() throws JRException {
    if (mainDesign == null) {
      mainDesign = getJasperDesign(connectionProvider, templatePath);
    }
    return mainDesign;
  }

  private JasperDesign getJasperDesign(ConnectionProvider provider, String reportPath)
      throws JRException {
    File reportFile = new File(reportPath);
    if (!reportFile.exists()) {
      throw new JRException(reportPath + " (No such jasper template file)");
    }
    if (language != null && provider != null) {
      log.debug("Translating report {} for language {}", reportPath, language);
      TranslationHandler handler = new TranslationHandler(provider);
      handler.prepareFile(reportPath, language, reportFile, baseDesignPath);
      InputStream reportInputStream = handler.getInputStream();
      if (reportInputStream != null) {
        return parseJRXml(reportInputStream);
      }
    }
    return parseJRXml(reportPath);
  }

  private JasperDesign parseJRXml(InputStream reportInputStream) throws JRException {
    long t = System.currentTimeMillis();
    JasperDesign design = JRXmlLoader.load(reportInputStream);
    log.debug("Report {} parsed using an InputStream in {} ms", design.getName(),
        (System.currentTimeMillis() - t));
    return design;
  }

  private JasperDesign parseJRXml(String reportPath) throws JRException {
    long t = System.currentTimeMillis();
    JasperDesign design = JRXmlLoader.load(reportPath);
    log.debug("Report {} parsed in {} ms", design.getName(), (System.currentTimeMillis() - t));
    return design;
  }

  private JasperReport compileJasperReport(JasperDesign jasperDesign) throws JRException {
    long t = System.currentTimeMillis();
    JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
    log.debug("Report {} compiled in {} ms", jasperDesign.getName(),
        (System.currentTimeMillis() - t));
    return jasperReport;
  }
}
