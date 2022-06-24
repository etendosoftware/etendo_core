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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

/**
 * This class is used to fill the data of the reports.
 */
class ReportFiller {
  private static final Logger log = LogManager.getLogger();

  // main report file path
  private String templatePath;
  // the compiled Jasper report
  private JasperReport jasperReport;
  // report parameters
  private Map<String, Object> parameters;
  // a connection provider to get the report data from database if required
  private ConnectionProvider connectionProvider;
  // an optional data source in case the report uses it
  private JRDataSource dataSource;

  ReportFiller(JasperReport jasperReport, Map<String, Object> parameters) {
    this.jasperReport = jasperReport;
    this.parameters = parameters;
  }

  ReportFiller(String templatePath, Map<String, Object> parameters) {
    this.templatePath = templatePath;
    this.parameters = parameters;
  }

  void setConnectionProvider(ConnectionProvider connectionProvider) {
    this.connectionProvider = connectionProvider;
  }

  /**
   * A report can be filled by taking information from a database in two ways: <br>
   * a) By defining a query within the template <br>
   * b) By providing a data source.
   * 
   * This method would be used for the latter case in order to provide the data source.
   * 
   * @param dataSource
   *          the data source to to get the data which will be used to fill the report
   */
  void setJRDataSource(JRDataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * @return a JasperPrint with the resulting filled report.
   * @throws JRException
   *           In case any error occurs when filling the report.
   */
  JasperPrint fillReport() throws JRException {
    JasperPrint jasperPrint;
    long t1 = System.currentTimeMillis();
    if (connectionProvider == null) {
      jasperPrint = fill(OBDal.getReadOnlyInstance().getConnection());
    } else {
      jasperPrint = fill();
    }
    log.debug("Report {} filled in {} ms", jasperReport.getName(),
        (System.currentTimeMillis() - t1));
    return jasperPrint;
  }

  private JasperPrint fill() {
    Connection connection = null;
    try {
      connection = connectionProvider.getTransactionConnection();
      return fill(connection);
    } catch (final Exception e) {
      Throwable t = (e.getCause() != null) ? e.getCause().getCause() : null;
      if (t != null) {
        throw new OBException(
            (t instanceof SQLException && t.getMessage().contains("@NoConversionRate@"))
                ? t.getMessage()
                : e.getMessage(),
            e);
      } else {
        throw new OBException(
            e.getCause() instanceof SQLException ? e.getCause().getMessage() : e.getMessage(), e);
      }
    } finally {
      try {
        connectionProvider.releaseRollbackConnection(connection);
      } catch (SQLException e) {
      }
    }
  }

  private JasperPrint fill(Connection connection) throws JRException {
    JasperPrint jasperPrint;
    if (jasperReport == null) {
      jasperPrint = JasperFillManager.fillReport(templatePath, parameters);
    } else if (dataSource != null) {
      parameters.put("REPORT_CONNECTION", connection);
      jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
    } else {
      jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, connection);
    }
    return jasperPrint;
  }
}
