
package org.openbravo.erpCommon.ad_actionButton;


import org.openbravo.erpCommon.utility.*;
import org.openbravo.erpCommon.reference.*;
import org.openbravo.utils.Replace;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessRunner;
import org.openbravo.xmlEngine.XmlDocument;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.obps.ActivationKey;
import org.openbravo.erpCommon.obps.ActivationKey.FeatureRestriction;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Process;
import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

@SuppressWarnings("unused")
public class ActionButton_Responser extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  protected static final String windowId = "ActionButtonResponser";
  
  public void init (ServletConfig config) {
    super.init(config);
    boolHist = false;
  }
  
  @Override
  public void service(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String strProcessId = getProcessId(vars);

    // set process type and id for audit
    SessionInfo.setProcessType("P");
    SessionInfo.setProcessId(strProcessId);
    SessionInfo.setUserId(vars.getSessionValue("#AD_User_ID"));
    SessionInfo.setSessionId(vars.getSessionValue("#AD_Session_ID"));
    SessionInfo.setQueryProfile("manualProcess");

    try {
      OBContext.setAdminMode();
      Process process = OBDal.getInstance().get(Process.class, strProcessId);
      if (process != null) {
        SessionInfo.setModuleId(process.getModule().getId());
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    super.service(request, response);
  }

  private String getProcessId(VariablesSecureApp vars) throws ServletException {
    String command = vars.getCommand();
    if (command.equals("DEFAULT")) {
      return vars.getRequiredStringParameter("inpadProcessId");
    } else if (command.startsWith("BUTTON")) {
      return command.substring("BUTTON".length());
    } else if (command.startsWith("FRAMES")) {
      return command.substring("FRAMES".length());
    } else if (command.startsWith("SAVE_BUTTONActionButton")) {
      return command.substring("SAVE_BUTTONActionButton".length());
    }
    return null;
  }

  public void doPost (HttpServletRequest request, HttpServletResponse response) throws IOException,ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String strProcessId = getProcessId(vars);

    if (vars.getCommand().startsWith("FRAMES")) {
      printPageFrames(response, vars, strProcessId);
    }
    
    if (!vars.commandIn("DEFAULT")) {
      //Check access
      FeatureRestriction featureRestriction = ActivationKey.getInstance().hasLicenseAccess("P",
          strProcessId);
      if (featureRestriction != FeatureRestriction.NO_RESTRICTION) {
        licenseError("P", strProcessId, featureRestriction, response, request, vars, true);
      }
      if (!hasGeneralAccess(vars, "P", strProcessId)) {
        bdErrorGeneralPopUp(request, response,
            Utility.messageBD(this, "Error", vars.getLanguage()), Utility.messageBD(this,
                "AccessTableNoView", vars.getLanguage()));
      }
    }
    
      
    if (vars.commandIn("DEFAULT")) {
      printPageDefault(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON172")) {
        
        printPageButton172(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON800087")) {
        
        printPageButton800087(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON800075")) {
        
        printPageButton800075(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON119")) {
        
        printPageButton119(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON193")) {
        
        printPageButton193(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON185")) {
        
        printPageButton185(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON800130")) {
        
        printPageButton800130(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON800109")) {
        
        printPageButton800109(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTOND234AE084F7040DCB66E281A4237FF99")) {
        
        printPageButtonD234AE084F7040DCB66E281A4237FF99(response, vars, strProcessId);

    } else if (vars.commandIn("SAVE_BUTTONActionButton172")) {
       process172(vars, request, response);    
    } else if (vars.commandIn("SAVE_BUTTONActionButton800087")) {
       process800087(vars, request, response);    
    } else if (vars.commandIn("SAVE_BUTTONActionButton800075")) {
       process800075(vars, request, response);    
    } else if (vars.commandIn("SAVE_BUTTONActionButton119")) {
       process119(vars, request, response);    
    } else if (vars.commandIn("SAVE_BUTTONActionButton193")) {
       process193(vars, request, response);    
    } else if (vars.commandIn("SAVE_BUTTONActionButton185")) {
       process185(vars, request, response);    
    } else if (vars.commandIn("SAVE_BUTTONActionButton800130")) {
       process800130(vars, request, response);    
    } else if (vars.commandIn("SAVE_BUTTONActionButton800109")) {
       process800109(vars, request, response);    
    } else if (vars.commandIn("SAVE_BUTTONActionButtonD234AE084F7040DCB66E281A4237FF99")) {
       processD234AE084F7040DCB66E281A4237FF99(vars, request, response);    

    } else pageErrorPopUp(response);
  }

  void printPageDefault(HttpServletResponse response, VariablesSecureApp vars, String strProcessId) throws IOException, ServletException {
    log4j.debug("Output: Default");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonDefault").createXmlDocument();
    xmlDocument.setParameter("processId", strProcessId);
	xmlDocument.setParameter("trlFormType", "PROCESS");
	xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
    out.println(xmlDocument.print());
    out.close();
  }
  
  void printPageFrames(HttpServletResponse response, VariablesSecureApp vars, String strProcessId) throws IOException, ServletException {
    log4j.debug("Output: Default");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonDefaultFrames").createXmlDocument();
    xmlDocument.setParameter("processId", strProcessId);
    xmlDocument.setParameter("trlFormType", "PROCESS");
    xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
    out.println(xmlDocument.print());
    out.close();
  }

    void printPageButton172(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 172");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton172", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButton_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("172");
        vars.removeMessage("172");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton800087(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 800087");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton800087", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButton_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("800087");
        vars.removeMessage("800087");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton800075(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 800075");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton800075", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButton_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("800075");
        vars.removeMessage("800075");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("DateFrom", "");
    xmlDocument.setParameter("DateFrom_Format", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("DateTo", "");
    xmlDocument.setParameter("DateTo_Format", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("M_Warehouse_ID", "");
    comboTableData = new ComboTableData(vars, this, "18", "M_Warehouse_ID", "197", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "");
    xmlDocument.setData("reportM_Warehouse_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("C_BPartner_ID", "");
    comboTableData = new ComboTableData(vars, this, "18", "C_BPartner_ID", "192", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "");
    xmlDocument.setData("reportC_BPartner_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("ReferenceNo", "");
    xmlDocument.setParameter("DateInvoiced", "");
    xmlDocument.setParameter("DateInvoiced_Format", vars.getSessionValue("#AD_SqlDateFormat"));
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton119(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 119");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton119", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButton_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("119");
        vars.removeMessage("119");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("DateInvoiced", "");
    xmlDocument.setParameter("DateInvoiced_Format", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("AD_Org_ID", Utility.getContext(this, vars, "#AD_Org_ID", windowId));
    comboTableData = new ComboTableData(vars, this, "19", "AD_Org_ID", "", "FDC45D1AE2404B8384D14DB6DED90DCF", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, Utility.getContext(this, vars, "#AD_Org_ID", windowId));
    xmlDocument.setData("reportAD_Org_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("IncludeChildOrgs", "");
    xmlDocument.setParameter("C_Order_ID", "");
    xmlDocument.setParameter("C_BPartner_ID", "");
    comboTableData = new ComboTableData(vars, this, "19", "C_BPartner_ID", "", "135", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "");
    xmlDocument.setData("reportC_BPartner_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("InvoiceToDate", "");
    xmlDocument.setParameter("InvoiceToDate_Format", vars.getSessionValue("#AD_SqlDateFormat"));
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton193(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 193");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton193", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButton_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("193");
        vars.removeMessage("193");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("DateOrdered", "");
    xmlDocument.setParameter("DateOrdered_Format", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("C_BPartner_ID", "");
    xmlDocument.setParameter("C_BPartner_IDR", "");
    xmlDocument.setParameter("Vendor_ID", "");
    comboTableData = new ComboTableData(vars, this, "18", "Vendor_ID", "192", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "");
    xmlDocument.setData("reportVendor_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("C_Order_ID", "");
    xmlDocument.setParameter("C_Order_IDR", "");
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton185(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 185");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton185", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButton_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("185");
        vars.removeMessage("185");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton800130(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 800130");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton800130", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButton_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("800130");
        vars.removeMessage("800130");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    xmlDocument.setParameter("Initdate", DateTimeData.today(this));
    xmlDocument.setParameter("Initdate_Format", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("EndDate", "");
    xmlDocument.setParameter("EndDate_Format", vars.getSessionValue("#AD_SqlDateFormat"));
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton800109(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 800109");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton800109", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButton_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("800109");
        vars.removeMessage("800109");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("C_Budget_ID", "");
    comboTableData = new ComboTableData(vars, this, "19", "C_Budget_ID", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "");
    xmlDocument.setData("reportC_Budget_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("MA_Processplan_ID", "");
    comboTableData = new ComboTableData(vars, this, "19", "MA_Processplan_ID", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "");
    xmlDocument.setData("reportMA_Processplan_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("CalcDate", "");
    xmlDocument.setParameter("CalcDate_Format", vars.getSessionValue("#AD_SqlDateFormat"));
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButtonD234AE084F7040DCB66E281A4237FF99(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process D234AE084F7040DCB66E281A4237FF99");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonD234AE084F7040DCB66E281A4237FF99", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButton_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("D234AE084F7040DCB66E281A4237FF99");
        vars.removeMessage("D234AE084F7040DCB66E281A4237FF99");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("reportType", "cus");
    comboTableData = new ComboTableData(vars, this, "17", "reportType", "B82C3C28E51F4AA6B87D98E7ABBF92F0", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "cus");
    xmlDocument.setData("reportreportType", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("AD_Org_ID", Utility.getContext(this, vars, "AD_Org_ID", ""));
    comboTableData = new ComboTableData(vars, this, "19", "AD_Org_ID", "", "D9463AFD77E44F619D396C19BF9E6A15", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, Utility.getContext(this, vars, "AD_Org_ID", ""));
    xmlDocument.setData("reportAD_Org_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("C_AcctSchema_ID", Utility.getContext(this, vars, "C_AcctSchema_ID", ""));
    comboTableData = new ComboTableData(vars, this, "19", "C_AcctSchema_ID", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, Utility.getContext(this, vars, "C_AcctSchema_ID", ""));
    xmlDocument.setData("reportC_AcctSchema_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("C_BPartner_ID", Utility.getContext(this, vars, "C_BPartner_ID", ""));
    xmlDocument.setParameter("C_BPartner_IDR", "");
    xmlDocument.setParameter("DateFrom", "");
    xmlDocument.setParameter("DateFrom_Format", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("DateTo", "");
    xmlDocument.setParameter("DateTo_Format", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("Multicurrency", "N");
    xmlDocument.setParameter("sumInitialBalance", "N");
    xmlDocument.setParameter("outputType", "pdf");
    comboTableData = new ComboTableData(vars, this, "17", "outputType", "800104", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "pdf");
    xmlDocument.setData("reportoutputType", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }



    private void process172(VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException{
         
        String strProcessing = vars.getStringParameter("inpprocessing");
        String pinstance = SequenceIdData.getUUID();
        PInstanceProcessData.insertPInstance(this, pinstance, "172", "0", strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
        
        
        ProcessBundle bundle = ProcessBundle.pinstance(pinstance, vars, this);
        new ProcessRunner(bundle).execute(this);

        processButtonHelper(request, response, vars, pinstance);    
  }
    private void process800087(VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException{
         
        String strProcessing = vars.getStringParameter("inpprocessing");
        String pinstance = SequenceIdData.getUUID();
        PInstanceProcessData.insertPInstance(this, pinstance, "800087", "0", strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
        
        
        ProcessBundle bundle = ProcessBundle.pinstance(pinstance, vars, this);
        new ProcessRunner(bundle).execute(this);

        processButtonHelper(request, response, vars, pinstance);    
  }
    private void process800075(VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException{
         
        String strProcessing = vars.getStringParameter("inpprocessing");
        String pinstance = SequenceIdData.getUUID();
        PInstanceProcessData.insertPInstance(this, pinstance, "800075", "0", strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
        String strdatefrom = vars.getStringParameter("inpdatefrom");
PInstanceProcessData.insertPInstanceParamDate(this, pinstance, "10", "DateFrom", strdatefrom, vars.getClient(), vars.getOrg(), vars.getUser());
String strdateto = vars.getStringParameter("inpdateto");
PInstanceProcessData.insertPInstanceParamDate(this, pinstance, "20", "DateTo", strdateto, vars.getClient(), vars.getOrg(), vars.getUser());
String strmWarehouseId = vars.getStringParameter("inpmWarehouseId");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "30", "M_Warehouse_ID", strmWarehouseId, vars.getClient(), vars.getOrg(), vars.getUser());
String strcBpartnerId = vars.getStringParameter("inpcBpartnerId");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "40", "C_BPartner_ID", strcBpartnerId, vars.getClient(), vars.getOrg(), vars.getUser());
String strreferenceno = vars.getStringParameter("inpreferenceno");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "50", "ReferenceNo", strreferenceno, vars.getClient(), vars.getOrg(), vars.getUser());
String strdateinvoiced = vars.getStringParameter("inpdateinvoiced");
PInstanceProcessData.insertPInstanceParamDate(this, pinstance, "60", "DateInvoiced", strdateinvoiced, vars.getClient(), vars.getOrg(), vars.getUser());

        
        ProcessBundle bundle = ProcessBundle.pinstance(pinstance, vars, this);
        new ProcessRunner(bundle).execute(this);

        processButtonHelper(request, response, vars, pinstance);    
  }
    private void process119(VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException{
         
        String strProcessing = vars.getStringParameter("inpprocessing");
        String pinstance = SequenceIdData.getUUID();
        PInstanceProcessData.insertPInstance(this, pinstance, "119", "0", strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
        String strdateinvoiced = vars.getStringParameter("inpdateinvoiced");
PInstanceProcessData.insertPInstanceParamDate(this, pinstance, "10", "DateInvoiced", strdateinvoiced, vars.getClient(), vars.getOrg(), vars.getUser());
String stradOrgId = vars.getStringParameter("inpadOrgId");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "15", "AD_Org_ID", stradOrgId, vars.getClient(), vars.getOrg(), vars.getUser());
String strincludechildorgs = vars.getStringParameter("inpincludechildorgs", "N");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "16", "IncludeChildOrgs", strincludechildorgs, vars.getClient(), vars.getOrg(), vars.getUser());
String strcOrderId = vars.getStringParameter("inpcOrderId");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "20", "C_Order_ID", strcOrderId, vars.getClient(), vars.getOrg(), vars.getUser());
String strcBpartnerId = vars.getStringParameter("inpcBpartnerId");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "30", "C_BPartner_ID", strcBpartnerId, vars.getClient(), vars.getOrg(), vars.getUser());
String strinvoicetodate = vars.getStringParameter("inpinvoicetodate");
PInstanceProcessData.insertPInstanceParamDate(this, pinstance, "40", "InvoiceToDate", strinvoicetodate, vars.getClient(), vars.getOrg(), vars.getUser());

        
        ProcessBundle bundle = ProcessBundle.pinstance(pinstance, vars, this);
        new ProcessRunner(bundle).execute(this);

        processButtonHelper(request, response, vars, pinstance);    
  }
    private void process193(VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException{
         
        String strProcessing = vars.getStringParameter("inpprocessing");
        String pinstance = SequenceIdData.getUUID();
        PInstanceProcessData.insertPInstance(this, pinstance, "193", "0", strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
        String strdateordered = vars.getStringParameter("inpdateordered");
PInstanceProcessData.insertPInstanceParamDate(this, pinstance, "10", "DateOrdered", strdateordered, vars.getClient(), vars.getOrg(), vars.getUser());
String strcBpartnerId = vars.getStringParameter("inpcBpartnerId");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "20", "C_BPartner_ID", strcBpartnerId, vars.getClient(), vars.getOrg(), vars.getUser());
String strvendorId = vars.getStringParameter("inpvendorId");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "30", "Vendor_ID", strvendorId, vars.getClient(), vars.getOrg(), vars.getUser());
String strcOrderId = vars.getStringParameter("inpcOrderId");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "40", "C_Order_ID", strcOrderId, vars.getClient(), vars.getOrg(), vars.getUser());

        
        ProcessBundle bundle = ProcessBundle.pinstance(pinstance, vars, this);
        new ProcessRunner(bundle).execute(this);

        processButtonHelper(request, response, vars, pinstance);    
  }
    private void process185(VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException{
         
        String strProcessing = vars.getStringParameter("inpprocessing");
        String pinstance = SequenceIdData.getUUID();
        PInstanceProcessData.insertPInstance(this, pinstance, "185", "0", strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
        
        
        ProcessBundle bundle = ProcessBundle.pinstance(pinstance, vars, this);
        new ProcessRunner(bundle).execute(this);

        processButtonHelper(request, response, vars, pinstance);    
  }
    private void process800130(VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException{
         
        String strProcessing = vars.getStringParameter("inpprocessing");
        String pinstance = SequenceIdData.getUUID();
        PInstanceProcessData.insertPInstance(this, pinstance, "800130", "0", strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
        String strinitdate = vars.getStringParameter("inpinitdate");
PInstanceProcessData.insertPInstanceParamDate(this, pinstance, "10", "Initdate", strinitdate, vars.getClient(), vars.getOrg(), vars.getUser());
String strenddate = vars.getStringParameter("inpenddate");
PInstanceProcessData.insertPInstanceParamDate(this, pinstance, "20", "EndDate", strenddate, vars.getClient(), vars.getOrg(), vars.getUser());

        
        ProcessBundle bundle = ProcessBundle.pinstance(pinstance, vars, this);
        new ProcessRunner(bundle).execute(this);

        processButtonHelper(request, response, vars, pinstance);    
  }
    private void process800109(VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException{
         
        String strProcessing = vars.getStringParameter("inpprocessing");
        String pinstance = SequenceIdData.getUUID();
        PInstanceProcessData.insertPInstance(this, pinstance, "800109", "0", strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
        String strcBudgetId = vars.getStringParameter("inpcBudgetId");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "10", "C_Budget_ID", strcBudgetId, vars.getClient(), vars.getOrg(), vars.getUser());
String strmaProcessplanId = vars.getStringParameter("inpmaProcessplanId");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "20", "MA_Processplan_ID", strmaProcessplanId, vars.getClient(), vars.getOrg(), vars.getUser());
String strcalcdate = vars.getStringParameter("inpcalcdate");
PInstanceProcessData.insertPInstanceParamDate(this, pinstance, "30", "CalcDate", strcalcdate, vars.getClient(), vars.getOrg(), vars.getUser());

        
        ProcessBundle bundle = ProcessBundle.pinstance(pinstance, vars, this);
        new ProcessRunner(bundle).execute(this);

        processButtonHelper(request, response, vars, pinstance);    
  }
    private void processD234AE084F7040DCB66E281A4237FF99(VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException{
         
        String strProcessing = vars.getStringParameter("inpprocessing");
        String pinstance = SequenceIdData.getUUID();
        PInstanceProcessData.insertPInstance(this, pinstance, "D234AE084F7040DCB66E281A4237FF99", "0", strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
        String strreporttype = vars.getStringParameter("inpreporttype");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "10", "reportType", strreporttype, vars.getClient(), vars.getOrg(), vars.getUser());
String stradOrgId = vars.getStringParameter("inpadOrgId");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "20", "AD_Org_ID", stradOrgId, vars.getClient(), vars.getOrg(), vars.getUser());
String strcAcctschemaId = vars.getStringParameter("inpcAcctschemaId");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "30", "C_AcctSchema_ID", strcAcctschemaId, vars.getClient(), vars.getOrg(), vars.getUser());
String strcBpartnerId = vars.getStringParameter("inpcBpartnerId");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "40", "C_BPartner_ID", strcBpartnerId, vars.getClient(), vars.getOrg(), vars.getUser());
String strdatefrom = vars.getStringParameter("inpdatefrom");
PInstanceProcessData.insertPInstanceParamDate(this, pinstance, "50", "DateFrom", strdatefrom, vars.getClient(), vars.getOrg(), vars.getUser());
String strdateto = vars.getStringParameter("inpdateto");
PInstanceProcessData.insertPInstanceParamDate(this, pinstance, "60", "DateTo", strdateto, vars.getClient(), vars.getOrg(), vars.getUser());
String strmulticurrency = vars.getStringParameter("inpmulticurrency", "N");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "70", "Multicurrency", strmulticurrency, vars.getClient(), vars.getOrg(), vars.getUser());
String strsuminitialbalance = vars.getStringParameter("inpsuminitialbalance", "N");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "80", "sumInitialBalance", strsuminitialbalance, vars.getClient(), vars.getOrg(), vars.getUser());
String stroutputtype = vars.getStringParameter("inpoutputtype");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "90", "outputType", stroutputtype, vars.getClient(), vars.getOrg(), vars.getUser());

        
        ProcessBundle bundle = ProcessBundle.pinstance(pinstance, vars, this);
        new ProcessRunner(bundle).execute(this);

        processButtonHelper(request, response, vars, pinstance);    
  }


  public String getServletInfo() {
    return "Servlet ActionButton_Responser. This Servlet was made by Wad constructor";
  } // end of the getServletInfo() method

  private void processButtonHelper(HttpServletRequest request, HttpServletResponse response, VariablesSecureApp vars, String pinstance) 
     throws ServletException, IOException {
      OBError myMessage;
      try {
        PInstanceProcessData[] pinstanceData = PInstanceProcessData.select(this, pinstance);
      myMessage = Utility.getProcessInstanceMessage(this, vars, pinstanceData);
      } catch (Exception e) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), e.getMessage());
          e.printStackTrace();
          log4j.warn("Error");
      }
      advisePopUp(request, response, myMessage.getType(), myMessage.getTitle(), myMessage.getMessage());
  }
}
