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
 * All portions are Copyright (C) 2008-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_process;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.JRFieldProviderDataSource;
import org.openbravo.erpCommon.utility.PrintJRData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.utils.Replace;

public class JasperProcess implements Process {

  static Logger log4j = LogManager.getLogger();

  private ConnectionProvider connection;

  public void initialize(ProcessBundle bundle) {
    connection = bundle.getConnection();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void execute(ProcessBundle bundle) throws Exception {

    HashMap<String, Object> designParameters = null;
    HashMap<Object, Object> exportParameters = null;

    ConfigParameters config = bundle.getConfig();
    VariablesSecureApp vars = bundle.getContext().toVars();
    FieldProvider[] data = (FieldProvider[]) bundle.getParams().get("data");

    String classInfoId = (String) bundle.getParams().get("classInfoId");

    String strReportName = bundle.getImpl();
    String strOutputType = (String) bundle.getParams().get("strOutputType");

    designParameters = (HashMap<String, Object>) bundle.getParams().get("designParameters");
    exportParameters = (HashMap<Object, Object>) bundle.getParams().get("exportParameters");

    if (strReportName == null || strReportName.equals("")) {
      strReportName = PrintJRData.getReportName(connection, classInfoId);
    }

    String strAttach = config.strFTPDirectory + "/284-" + classInfoId;

    String strLanguage = bundle.getContext().getLanguage();
    Locale locLocale = new Locale(strLanguage.substring(0, 2), strLanguage.substring(3, 5));

    String strBaseDesign = getBaseDesignPath(config);

    strReportName = Replace.replace(Replace.replace(strReportName, "@basedesign@", strBaseDesign),
        "@attach@", strAttach);

    // FIXME: os is never assigned, but used leading to an NPE
    ServletOutputStream os = null;
    try {
      if (designParameters == null) {
        designParameters = new HashMap<String, Object>();
      }

      // designParameters.put("BASE_WEB", strReplaceWithFull);
      designParameters.put("BASE_DESIGN", strBaseDesign);
      designParameters.put("ATTACH", strAttach);
      designParameters.put("USER_CLIENT", Utility.getContext(connection, vars, "#User_Client", ""));
      designParameters.put("USER_ORG", Utility.getContext(connection, vars, "#User_Org", ""));
      designParameters.put("LANGUAGE", strLanguage);
      designParameters.put("LOCALE", locLocale);

      DecimalFormatSymbols dfs = new DecimalFormatSymbols();
      dfs.setDecimalSeparator(vars.getSessionValue("#AD_ReportDecimalSeparator").charAt(0));
      dfs.setGroupingSeparator(vars.getSessionValue("#AD_ReportGroupingSeparator").charAt(0));
      DecimalFormat numberFormat = new DecimalFormat(vars.getSessionValue("#AD_ReportNumberFormat"),
          dfs);
      designParameters.put("NUMBERFORMAT", numberFormat);

      if (exportParameters == null) {
        exportParameters = new HashMap<Object, Object>();
      }
      if (strOutputType == null || strOutputType.equals("")) {
        strOutputType = "html";
      }
      final ExportType expType = ExportType.getExportType(strOutputType);
      ReportingUtils.exportJR(strReportName, expType, designParameters, os, false, connection,
          new JRFieldProviderDataSource(data, vars.getJavaDateFormat()), exportParameters);
    } catch (Exception e) {
      throw new ServletException(e.getMessage());
    } finally {
      try {
        os.close();
      } catch (Exception e) {
      }
    }
  }

  /**
   * Returns the absolute path to the correct language subfolder within the context's src-loc
   * folder.
   * 
   * @return String with the absolute path on the local drive.
   */
  protected String getBaseDesignPath(ConfigParameters config) {
    if (log4j.isDebugEnabled()) {
      log4j.debug("*********************Base path: " + config.strBaseDesignPath);
    }
    String strNewAddBase = config.strDefaultDesignPath;
    String strFinal = config.strBaseDesignPath;
    if (!strFinal.endsWith("/" + strNewAddBase)) {
      strFinal += "/" + strNewAddBase;
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("*********************Base path: " + strFinal);
    }
    return config.prefix + strFinal;
  }

}
