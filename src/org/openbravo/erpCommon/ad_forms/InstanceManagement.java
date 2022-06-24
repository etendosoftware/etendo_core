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
 * All portions are Copyright (C) 2009-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_forms;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_process.HeartbeatProcess;
import org.openbravo.erpCommon.obps.ActivationKey;
import org.openbravo.erpCommon.obps.ActiveInstanceProcess;
import org.openbravo.erpCommon.obps.ModuleLicenseRestrictions;
import org.openbravo.erpCommon.obps.ModuleLicenseRestrictions.ActivationMsg;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.System;
import org.openbravo.model.ad.system.SystemInformation;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class InstanceManagement extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      printPageActive(response, vars, ActivationKey.getInstance());
    } else if (vars.commandIn("SHOW_ACTIVATE")) {
      printPageNotActive(response, vars);
    } else if (vars.commandIn("ACTIVATE")) {
      activateCancelRemote(vars, true);
      printPageClosePopUp(response, vars);
    } else if (ActivationKey.getInstance().isGolden()) {
      // Following options are not allowed for Golden Key licenses
      pageError(response);
    } else if (vars.commandIn("SHOW_ACTIVATE_LOCAL")) {
      printPageActivateLocal(response, vars);
    } else if (vars.commandIn("INSTALLFILE")) {
      printPageInstallFile(response, vars);
    } else if (vars.commandIn("SHOW_DEACTIVATE")) {
      printPageDeactivateCancel(response, vars, true);
    } else if (vars.commandIn("DEACTIVATE")) {
      printPageDeactivateProcess(response, vars);
    } else if (vars.commandIn("SHOW_CANCEL")) {
      printPageDeactivateCancel(response, vars, false);
    } else if (vars.commandIn("CANCEL")) {
      activateCancelRemote(vars, false);
      printPageClosePopUp(response, vars);
    } else {
      pageError(response);
    }
  }

  private void printPageDeactivateCancel(HttpServletResponse response, VariablesSecureApp vars,
      boolean deactivate) throws IOException {
    String discard[] = { "" };
    if (deactivate) {
      discard[0] = "discardCancel";
    } else {
      discard[0] = "discardDeactivate";
    }
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_forms/InstanceManagementDeactivate", discard)
        .createXmlDocument();
    response.setContentType("text/html; charset=UTF-8");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());
    final PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();

  }

  private void printPageDeactivateProcess(HttpServletResponse response, VariablesSecureApp vars)
      throws IOException, ServletException {
    OBError msg = new OBError();
    OBContext.setAdminMode();
    ConnectionProvider cp = new DalConnectionProvider(false);
    try {
      // Check for commercial modules installed in the instance
      OBCriteria<Module> qMods = OBDal.getInstance().createCriteria(Module.class);
      qMods.add(Restrictions.eq(Module.PROPERTY_COMMERCIAL, true));
      qMods.add(Restrictions.eq(Module.PROPERTY_ENABLED, true));
      qMods.addOrder(Order.asc(Module.PROPERTY_NAME));

      // core can be commercial, do not take it into account
      qMods.add(Restrictions.ne(Module.PROPERTY_ID, "0"));
      boolean deactivable = true;
      String commercialModules = "";
      for (Module mod : qMods.list()) {
        deactivable = false;
        commercialModules += "<br/>" + mod.getName();
      }

      if (!deactivable) {
        msg.setType("Error");
        msg.setMessage(
            Utility.messageBD(cp, "CannotDeactivateWithCommercialModules", vars.getLanguage())
                + commercialModules);
      } else {
        // Deactivate instance
        System sys = OBDal.getInstance().get(System.class, "0");
        sys.setActivationKey(null);
        sys.setInstanceKey(null);
        ActivationKey.reload();
        msg.setType("Success");
        msg.setMessage(Utility.messageBD(cp, "Success", vars.getLanguage()));

        ActiveInstanceProcess.updateShowProductionFields("N");

        // When deactivating a cloned instance insert a dummy heartbeat log so it is not detected as
        // a cloned instance anymore.
        if (HeartbeatProcess.isClonedInstance()) {
          insertDummyHBLog();
        }
      }
    } catch (Exception e) {
      log4j.error("Error deactivating instance", e);
      msg.setType("Error");
      msg.setMessage(Utility.parseTranslation(cp, vars, vars.getLanguage(), e.getMessage()));
    } finally {
      OBContext.restorePreviousMode();
    }
    vars.setMessage("InstanceManagement", msg);
    printPageClosePopUp(response, vars, "");

  }

  private void printPageInstallFile(HttpServletResponse response, VariablesSecureApp vars)
      throws IOException, ServletException {
    FileItem fi = vars.getMultiFile("inpFile");
    OBError msg = new OBError();
    try {
      InputStream is = fi.getInputStream();

      // read the file in a String
      StringBuffer buf = new StringBuffer();
      byte[] b = new byte[1024];
      for (int n; (n = is.read(b)) != -1;) {
        buf.append(new String(b, 0, n));
      }

      ProcessBundle pb = new ProcessBundle(null, vars);
      HashMap<String, Object> params = new HashMap<String, Object>();

      params.put("publicKey", vars.getStringParameter("publicKey"));
      params.put("activationKey", buf.toString());
      params.put("activate", true);

      pb.setParams(params);

      new ActiveInstanceProcess().execute(pb);
      msg = (OBError) pb.getResult();
    } catch (Exception e) {
      log4j.error(e);
      msg.setType("Error");
      msg.setMessage(Utility.parseTranslation(new DalConnectionProvider(false), vars,
          vars.getLanguage(), e.getMessage()));
    }
    vars.setMessage("InstanceManagement", msg);
    printPageClosePopUp(response, vars, "");

  }

  private void printPageActivateLocal(HttpServletResponse response, VariablesSecureApp vars)
      throws IOException {

    ActivationKey ak = ActivationKey.getInstance();
    String discard[] = { "", "" };

    if (ak.isOPSInstance()) {
      if (ak.hasExpired()) {
        // Renew
        discard[0] = "OPSActivate";
        discard[1] = "OPSRefresh";
      } else {
        // Refresh
        discard[0] = "OPSActivate";
        discard[1] = "OPSRefresh";
      }
    } else {
      // Activate
      discard[0] = "OPSRefresh";
      discard[1] = "OPSRenew";
    }

    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_forms/InstanceManagementActivateLocal",
            discard)
        .createXmlDocument();
    response.setContentType("text/html; charset=UTF-8");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());
    if (ak.hasActivationKey()) {
      xmlDocument.setParameter("publicKey", ak.getPublicKey());
    }
    final PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();

  }

  private void printPageActive(HttpServletResponse response, VariablesSecureApp vars,
      ActivationKey activationKey) throws IOException, ServletException {
    response.setContentType("text/html; charset=UTF-8");
    String discard[] = { "", "", "", "", "", "", "", "" };

    switch (activationKey.getSubscriptionStatus()) {
      case INVALID:
      case COMMUNITY:
        discard[0] = "OPSInstance";
        discard[1] = "OPSActiveTitle";
        discard[2] = "OPSExpired";
        discard[3] = "OPSConverted";
        discard[4] = "OPSNoActiveYet";
        discard[5] = "OPSActive";
        discard[6] = "OPSExpiredCancel";
        break;
      case ACTIVE:
        discard[0] = "CEInstance";
        discard[1] = "OPSExpired";
        if (!activationKey.hasExpirationDate()) {
          discard[2] = "OPSExpirationTime";
        }
        discard[3] = "OPSConverted";
        discard[4] = "OPSNoActiveYet";
        discard[5] = "OPSExpiredCancel";
        break;
      case CANCEL:
        discard[0] = "CEInstance";
        discard[1] = "OPSActiveTitle";
        discard[2] = "OPSExpired";
        discard[3] = "OPSNoActiveYet";
        discard[4] = "OPSExpiredCancel";
        break;
      case EXPIRED:
        discard[0] = "CEInstance";
        discard[1] = "OPSActiveTitle";
        discard[2] = "OPSNoActiveYet";
        discard[3] = "OPSConverted";
        discard[4] = "OPSActive";
        if (activationKey.isTrial()) {
          discard[5] = "OPSExpiredCancel";
        }
        break;
      case NO_ACTIVE_YET:
        discard[0] = "CEInstance";
        discard[1] = "OPSExpired";
        discard[2] = "OPSActiveTitle";
        discard[3] = "OPSConverted";
        discard[4] = "OPSActive";
        discard[5] = "OPSExpiredCancel";
        break;
    }

    if (activationKey.isGolden()) {
      discard[6] = "discardGolden";
    }

    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_forms/InstanceManagement", discard)
        .createXmlDocument();

    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());

    ConnectionProvider cp = new DalConnectionProvider(false);

    ToolBar toolbar = new ToolBar(cp, vars.getLanguage(), "InstanceManagement", false, "", "", "",
        false, "ad_forms", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    OBError myMessage = null;
    ActivationMsg msg = activationKey.getActivationMessage();
    if (msg == null) {
      myMessage = vars.getMessage("InstanceManagement");
    } else {
      myMessage = new OBError();
      myMessage.setType(activationKey.getMessageType());
      String msgTxt = Utility.parseTranslation(cp, vars, vars.getLanguage(), msg.getMsgText());

      OBError originalMessage = vars.getMessage("InstanceManagement");
      if (originalMessage != null) {
        msgTxt = originalMessage.getMessage() + "<br/>" + msgTxt;
      }
      myMessage.setMessage(msgTxt);
      myMessage.setType(msg.getSeverity().toString());
    }

    vars.removeMessage("InstanceManagement");
    if (myMessage != null) {
      xmlDocument.setParameter("messageType", myMessage.getType());
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }

    String instanceInfo = activationKey.isOPSInstance() ? //
        getLicenseDescription(activationKey, vars.getLanguage())
        : Utility.messageBD(cp, "OPSCommunityInstance", vars.getLanguage()).replace("\\n", "\n");
    xmlDocument.setParameter("instanceInfo", instanceInfo);

    if (activationKey.hasExpirationDate()) {
      String daysLeft = activationKey.getPendingDays() != null ? //
          activationKey.getPendingDays().toString()
          : Utility.messageBD(cp, "OPSUnlimitedUsers", vars.getLanguage()).replace("\\n", "\n");
      xmlDocument.setParameter("OPSdaysLeft", daysLeft);
    }

    xmlDocument.setParameter("moduleActions",
        activationKey.getInstanceActivationExtraActionsHtml(xmlEngine));

    String cacheMsg = Utility.messageBD(cp, "OUTDATED_FILES_CACHED", vars.getLanguage())
        .replace("\\n", "\n");
    cacheMsg = "var cacheMsg = \"" + cacheMsg + "\"";
    xmlDocument.setParameter("cacheMsg", cacheMsg);

    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private String getLicenseDescription(ActivationKey ak, String lang) {
    String dateFormat = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("dateFormat.java");

    ConnectionProvider conn = new DalConnectionProvider(false);
    StringBuilder sb = new StringBuilder();
    if (ak.getInstanceProperties() != null) {
      sb.append("<tr><td>")
          .append(Utility.messageBD(conn, "OPSCustomer", lang))
          .append("</td><td>")
          .append(ak.getProperty("customer"))
          .append("</td></tr>");

      sb.append("<tr><td>")
          .append(Utility.messageBD(conn, "OPSLicenseEdition", lang))
          .append("</td><td>")
          .append(
              Utility.getListValueName("OBPSLicenseEdition", ak.getLicenseClass().getCode(), lang))
          .append("</td></tr>");
      sb.append("<tr><td>")
          .append(Utility.messageBD(conn, "OPSLicenseType", lang))
          .append("</td><td>")
          .append(Utility.getListValueName("OPSLicenseType", ak.getProperty("lincensetype"), lang));
      if (ak.isTrial()) {
        sb.append(" (" + Utility.messageBD(conn, "OPSTrialLicense", lang) + ")");
      }
      sb.append("</td></tr>");
      Date startDate = ak.getStartDate();
      SimpleDateFormat outputFormat = new SimpleDateFormat(dateFormat);
      if (startDate != null) {
        sb.append("<tr><td>")
            .append(Utility.messageBD(conn, "OPSStartDate", lang))
            .append("</td><td>")
            .append(outputFormat.format(startDate))
            .append("</td></tr>");
      }

      Date endDate = ak.getEndDate();
      if (endDate != null) {
        sb.append("<tr><td>")
            .append(Utility.messageBD(conn, "OPSEndDate", lang))
            .append("</td><td>")
            .append(
                (ak.getProperty("enddate") == null ? Utility.messageBD(conn, "OPSNoEndDate", lang)
                    : outputFormat.format(endDate)))
            .append("</td></tr>");
      }

      Long maxUsers = ak.getMaxUsers();
      sb.append("<tr><td>")
          .append(Utility.messageBD(conn, "OPSConcurrentUsers", lang))
          .append("</td><td>")
          .append((maxUsers == null || maxUsers == 0L)
              ? Utility.messageBD(conn, "OPSUnlimitedUsers", lang)
              : maxUsers)
          .append("</td></tr>");
      if (ak.getProperty("limituserswarn") != null) {
        sb.append("<tr><td>")
            .append(Utility.messageBD(conn, "OPSConcurrentUsersWarn", lang))
            .append("</td><td>")
            .append(ak.getProperty("limituserswarn"))
            .append("</td></tr>");
      }

      sb.append("<tr><td>")
          .append(Utility.messageBD(conn, "OPSCurrentConcurrentUsers", lang))
          .append("</td><td>");
      sb.append(ak.getActiveSessions(null));
      sb.append("</td></tr>");

      sb.append("<tr><td>")
          .append(Utility.messageBD(conn, "OPSInstanceNo", lang))
          .append("</td><td>")
          .append(ak.getProperty("instanceno"))
          .append("\n");

      sb.append("<tr><td>")
          .append(Utility.messageBD(conn, "OPSInstancePurpose", lang))
          .append("</td><td>")
          .append(Utility.getListValueName("InstancePurpose", ak.getProperty("purpose"), lang))
          .append("</td></tr>");

      sb.append("<tr><td>")
          .append(Utility.messageBD(conn, "OPSWSLimitation", lang))
          .append("</td><td>");
      sb.append(ak.getWSExplanation(conn, lang));
      sb.append("</td></tr>");

      if (!ak.hasUnlimitedWsAccess()) {
        sb.append("<tr><td>")
            .append(Utility.messageBD(conn, "OPSWSCounterDay", lang))
            .append("</td><td>");
        sb.append(ak.getNumberWSDayCounter());
        sb.append("</td></tr>");
      }

      sb.append("<tr><td>")
          .append(Utility.messageBD(conn, "OPSPOSLimitation", lang))
          .append("</td><td>");
      sb.append(ak.getPOSTerminalsExplanation());
      sb.append("</td></tr>");

      for (ModuleLicenseRestrictions.AdditionalInfo addInfo : ak.getAdditionalMessageInfo()) {
        sb.append("<tr><td>")
            .append(Utility.messageBD(conn, addInfo.getKey(), lang))
            .append("</td><td>");
        sb.append(addInfo.getValue());
        sb.append("</td></tr>");
      }

    } else {
      sb.append(Utility.messageBD(conn, "OPSNonActiveInstance", lang));
    }
    return sb.toString();
  }

  private void printPageNotActive(HttpServletResponse response, VariablesSecureApp vars)
      throws IOException, ServletException {

    ActivationKey activationKey = ActivationKey.getInstance();
    response.setContentType("text/html; charset=UTF-8");
    String discard[] = { "", "", "" };
    if (activationKey.isOPSInstance()) {
      if (activationKey.hasExpired()) {
        // Renew
        discard[0] = "OPSActivate";
        discard[1] = "OPSRefresh";
      } else {
        // Refresh
        discard[0] = "OPSActivate";
        discard[1] = "OPSRefresh";
      }
    } else {
      // Activate
      discard[0] = "OPSRefresh";
      discard[1] = "OPSRenew";
    }

    if (activationKey.isGolden()) {
      discard[2] = "editable";
    } else {
      discard[2] = "readonly";
    }

    final PrintWriter out = response.getWriter();
    final XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_forms/InstanceManagementActivateOnline",
            discard)
        .createXmlDocument();
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());

    ConnectionProvider cp = new DalConnectionProvider(false);

    final OBError myMessage = vars.getMessage("InstanceManagement");
    vars.removeMessage("InstanceManagement");
    if (myMessage != null) {
      xmlDocument.setParameter("messageType", myMessage.getType());
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }

    final SystemInformation sysInfo = OBDal.getInstance().get(SystemInformation.class, "0");
    // Purpose combo
    try {
      ComboTableData comboTableData = new ComboTableData(cp, "LIST", "", "InstancePurpose", "",
          Utility.getContext(cp, vars, "#AccessibleOrgTree", "InstanceManagement"),
          Utility.getContext(cp, vars, "#User_Client", "InstanceManagement"), 0);
      Utility.fillSQLParameters(cp, vars, null, comboTableData, "InstanceManagement",
          sysInfo.getInstancePurpose());
      if (sysInfo.getInstancePurpose() != null) {
        xmlDocument.setParameter("paramSelPurpose", sysInfo.getInstancePurpose());
      }
      xmlDocument.setData("reportPurpose", "liststructure", comboTableData.select(false));
    } catch (Exception ex) {
      log4j.error(ex.getMessage(), ex);
      throw new ServletException(ex);
    }

    if (activationKey.hasActivationKey()) {
      xmlDocument.setParameter("publicKey", activationKey.getPublicKey());
    }

    if (activationKey.isOPSInstance()) {
      xmlDocument.setParameter("instanceNo", activationKey.getProperty("instanceno"));
    }

    out.println(xmlDocument.print());
    out.close();
  }

  /**
   * Activates or cancels the instance.
   * 
   * @param vars
   * @param activate
   *          true in case it is activating, false in case it is canceling
   * @return true if everything went correctly
   */
  private boolean activateCancelRemote(VariablesSecureApp vars, boolean activate)
      throws ServletException {
    boolean result = false;
    ProcessBundle pb = new ProcessBundle(null, vars);

    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("activate", activate);
    if (activate) {
      // activating instance, get parameters from form
      params.put("publicKey", vars.getStringParameter("publicKey"));
      params.put("purpose", vars.getStringParameter("purpose"));
      params.put("instanceNo", vars.getStringParameter("instanceNo"));

    } else {
      // canceling instance, get parameters from DB
      System sys = OBDal.getInstance().get(System.class, "0");
      params.put("publicKey", sys.getInstanceKey());
      params.put("instanceNo", ActivationKey.getInstance().getProperty("instanceno"));
      params.put("purpose", ActivationKey.getInstance().getProperty("purpose"));
    }

    pb.setParams(params);

    OBError msg = new OBError();
    ConnectionProvider cp = new DalConnectionProvider(false);
    try {
      new ActiveInstanceProcess().execute(pb);
      msg = (OBError) pb.getResult();
      result = msg.getType().equals("Success");

      ActivationKey ak = ActivationKey.getInstance();
      if (result && ak.isActive() && ak.isTrial() && !ak.isHeartbeatActive()) {
        msg.setType("Warning");
        msg.setTitle(Utility.messageBD(cp, "OPS_NOT_HB_ACTIVE_TITLE", vars.getLanguage()));
        msg.setMessage(Utility.messageBD(cp, "OPS_NOT_HB_ACTIVE", vars.getLanguage()));
      }
    } catch (Exception e) {
      log4j.error("Error Activating instance", e);
      msg.setType("Error");
      msg.setMessage(Utility.parseTranslation(cp, vars, vars.getLanguage(), e.getMessage()));
      result = false;
    }

    msg.setMessage(Utility.parseTranslation(cp, vars, vars.getLanguage(), msg.getMessage()));
    vars.setMessage("InstanceManagement", msg);
    return result;
  }

  static void insertDummyHBLog() throws ServletException {
    ActiveInstanceProcess.insertDummyHBLog();
  }
}
