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
 * All portions are Copyright (C) 2010-2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_forms;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.filter.ValueListFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_process.HeartbeatProcess;
import org.openbravo.erpCommon.obps.ActivationKey;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.SystemInformation;
import org.openbravo.model.ad.ui.Form;
import org.openbravo.utils.Replace;
import org.openbravo.xmlEngine.XmlDocument;

public class InstancePurpose extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static final ValueListFilter availablePurposeFilter = new ValueListFilter("P", "D", "T",
      "E");
  private static final String PRODUCTION_INSTANCE = "P";

  @Inject
  private ApplicationDictionaryCachedStructures adCachedStructures;

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    final VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      printPageDataSheet(response, vars);
    } else if (vars.commandIn("PROCEED")) {
      savePurpose(vars.getRequiredStringParameter("instancePurpose", availablePurposeFilter));
      if (HeartbeatProcess.isShowHeartbeatRequired(vars, myPool)) {
        response.sendRedirect("Heartbeat.html");
        return;
      }
      printPageClosePopUp(response, vars);
    } else {
      pageError(response);
    }
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars)
      throws IOException, ServletException {
    final String instanceActivationId = "8D6282279F464B1696B0EE3E23023B65";
    String newTabTitle;
    log4j.debug("Output: dataSheet");
    response.setContentType("text/html; charset=UTF-8");
    final PrintWriter out = response.getWriter();

    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_forms/InstancePurpose")
        .createXmlDocument();

    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());

    OBContext.setAdminMode();
    Form instanceActivationForm = OBDal.getInstance().get(Form.class, instanceActivationId);
    if (instanceActivationForm != null) {
      newTabTitle = instanceActivationForm.getIdentifier();
      newTabTitle = StringEscapeUtils.escapeHtml4(newTabTitle);
    } else {
      newTabTitle = "Instance Activation";
    }
    OBContext.restorePreviousMode();

    xmlDocument.setParameter("newTabTitle", "var newTabTitle = \"" + newTabTitle + "\";");

    String strTitle = "IP_WELCOME_TITLE";
    String strWelcomeMsg = "IP_WELCOME_MSG";
    if (HeartbeatProcess.isClonedInstance()) {
      strTitle = "IP_CLONED_TITLE";
      ActivationKey ak = ActivationKey.getInstance();
      if (ak.isOPSInstance()) {
        strWelcomeMsg = "IP_CLONED_OPS_MSG";
        xmlDocument.setParameter("showPurposeCombo", "N");
        xmlDocument.setParameter("showButtonProceed", "N");
      } else {
        strWelcomeMsg = "IP_CLONED_COMM_MSG";
      }
    } else {
      String purpose = OBDal.getInstance().get(SystemInformation.class, "0").getInstancePurpose();
      if (purpose != null && !purpose.equals("")) {
        xmlDocument.setParameter("selectedPurpose", purpose);
      }
    }
    xmlDocument.setParameter("welcome", Replace
        .replace(Utility.messageBD(this, strWelcomeMsg, vars.getLanguage()), "\\n", "<br/>"));
    xmlDocument.setParameter("title", Utility.messageBD(myPool, strTitle, vars.getLanguage()));

    xmlDocument.setParameter("recordId",
        vars.getStringParameter("inpcRecordId", IsIDFilter.instance));

    try {
      ComboTableData comboTableData = new ComboTableData(this, "LIST", "", "InstancePurpose", "",
          Utility.getContext(this, vars, "#AccessibleOrgTree", "InstancePurpose"),
          Utility.getContext(this, vars, "#User_Client", "InstancePurpose"), 0);
      xmlDocument.setData("reportPurpose", "liststructure", comboTableData.select(false));
    } catch (Exception ex) {
      log4j.error(ex.getMessage(), ex);
      throw new ServletException(ex);
    }

    String jsCommand = "''";
    if (vars.commandIn("DEFAULT")) {
      jsCommand = "'PROCEED'";
    }
    xmlDocument.setParameter("cmd", "var cmd=" + jsCommand + ";");

    out.println(xmlDocument.print());
    out.close();
  }

  private void savePurpose(String strPurpose) throws ServletException {
    SystemInformation systemInfo = OBDal.getInstance().get(SystemInformation.class, "0");
    systemInfo.setInstancePurpose(strPurpose);
    OBDal.getInstance().save(systemInfo);
    if (HeartbeatProcess.isClonedInstance()) {
      InstanceManagement.insertDummyHBLog();
    }

    if (PRODUCTION_INSTANCE.equals(strPurpose)) {
      adCachedStructures.setNotInDevelopment();
    }
  }

  @Override
  public String getServletInfo() {
    return "InstancePurpose pop-up form servlet.";
  }
}
