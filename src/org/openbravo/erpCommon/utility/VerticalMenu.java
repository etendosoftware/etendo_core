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
 * All portions are Copyright (C) 2001-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.xmlEngine.XmlDocument;

/**
 * This class was designed to render the 2.50 vertical menu. This menu is not displayed anymore, but
 * it is still being referenced and used in old manual windows, i.e., manual reports and process
 * using old 2.50 styling.
 */
public class VerticalMenu extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    final VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("LOADING")) {
      printPageLoadingMenu(response, vars);
    } else if (vars.commandIn("HIDE")) {
      printPageHideMenu(response, vars);
    } else {
      throw new IllegalArgumentException("Unsupported command: " + vars.getCommand());
    }
  }

  private void printPageHideMenu(HttpServletResponse response, VariablesSecureApp vars)
      throws IOException, ServletException {
    log4j.debug("Output: Vertical Menu's screen");

    final XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/utility/VerticalMenu")
        .createXmlDocument();
    xmlDocument.ignoreTranslation = true;

    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";");
    xmlDocument.setParameter("autosave",
        "var autosave = "
            + (vars.getSessionValue("#Autosave").equals("")
                || vars.getSessionValue("#Autosave").equalsIgnoreCase("N") ? "false" : "true")
            + ";");

    xmlDocument.setParameter("menu", "");
    xmlDocument.setParameter("popup", "");

    response.setContentType("text/html; charset=UTF-8");
    final PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printPageLoadingMenu(HttpServletResponse response, VariablesSecureApp vars)
      throws IOException, ServletException {
    final XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/utility/VerticalMenuLoading")
        .createXmlDocument();
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());

    response.setContentType("text/html; charset=UTF-8");
    final PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }
}
