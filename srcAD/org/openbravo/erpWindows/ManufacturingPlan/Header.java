
package org.openbravo.erpWindows.ManufacturingPlan;


import org.openbravo.erpCommon.reference.*;



import org.openbravo.erpCommon.utility.*;
import org.openbravo.data.FieldProvider;
import org.openbravo.utils.FormatUtilities;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.exception.OBException;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessRunner;
import org.openbravo.xmlEngine.XmlDocument;
import org.openbravo.database.SessionInfo;
import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.util.*;

// Generated old code, not worth to make i.e. java imports perfect
@SuppressWarnings("unused")
public class Header extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  
  private static final String windowId = "800096";
  private static final String tabId = "800255";
  private static final int accesslevel = 1;
  private static final String moduleId = "0";
  
  @Override
  public void init(ServletConfig config) {
    setClassInfo("W", tabId, moduleId);
    super.init(config);
  }
  
  
  @Override
  public void service(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String command = vars.getCommand();
    
    boolean securedProcess = false;
    if (command.contains("BUTTON")) {
     List<String> explicitAccess = Arrays.asList( "");
    
     SessionInfo.setUserId(vars.getSessionValue("#AD_User_ID"));
     SessionInfo.setSessionId(vars.getSessionValue("#AD_Session_ID"));
     SessionInfo.setQueryProfile("manualProcess");
     
      if (command.contains("6FBD65B0FDB74D1AB07F0EADF18D48AE")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("6FBD65B0FDB74D1AB07F0EADF18D48AE");
        SessionInfo.setModuleId("0");
      }
     
      try {
        securedProcess = "Y".equals(org.openbravo.erpCommon.businessUtility.Preferences
            .getPreferenceValue("SecuredProcess", true, vars.getClient(), vars.getOrg(), vars
                .getUser(), vars.getRole(), windowId));
      } catch (PropertyException e) {
      }
     
      if (command.contains("800160")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("800160");
        SessionInfo.setModuleId("0");
        if (securedProcess || explicitAccess.contains("800160")) {
          classInfo.type = "P";
          classInfo.id = "800160";
        }
      }
     
      if (command.contains("800159")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("800159");
        SessionInfo.setModuleId("0");
        if (securedProcess || explicitAccess.contains("800159")) {
          classInfo.type = "P";
          classInfo.id = "800159";
        }
      }
     
      if (command.contains("800161")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("800161");
        SessionInfo.setModuleId("0");
        if (securedProcess || explicitAccess.contains("800161")) {
          classInfo.type = "P";
          classInfo.id = "800161";
        }
      }
     

     
      if (explicitAccess.contains("6FBD65B0FDB74D1AB07F0EADF18D48AE") || (securedProcess && command.contains("6FBD65B0FDB74D1AB07F0EADF18D48AE"))) {
        classInfo.type = "P";
        classInfo.id = "6FBD65B0FDB74D1AB07F0EADF18D48AE";
      }
     
    }
    if (!securedProcess) {
      setClassInfo("W", tabId, moduleId);
    }
    super.service(request, response);
  }
  

  public void doPost (HttpServletRequest request, HttpServletResponse response) throws IOException,ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Boolean saveRequest = (Boolean) request.getAttribute("autosave");
    
    if(saveRequest != null && saveRequest){
      throw new OBException("2.50 style request.autosave is no longer supported: " + vars.getCommand());
    }
    
    if (vars.commandIn("DEFAULT", "DIRECT", "TAB", "SEARCH", "RELATION", "NEW", "EDIT", "NEXT",
        "PREVIOUS", "FIRST_RELATION", "PREVIOUS_RELATION", "NEXT_RELATION", "LAST_RELATION",
        "LAST", "SAVE_NEW_RELATION", "SAVE_NEW_NEW", "SAVE_NEW_EDIT", "SAVE_EDIT_RELATION",
        "SAVE_EDIT_NEW", "SAVE_EDIT_EDIT", "SAVE_EDIT_NEXT", "DELETE", "SAVE_XHR")) {
      throw new OBException("2.50 style command is no longer supported: " + vars.getCommand());

     } else if (vars.commandIn("BUTTONLaunchwr800160")) {
        vars.setSessionValue("button800160.strlaunchwr", vars.getStringParameter("inplaunchwr"));
        vars.setSessionValue("button800160.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button800160.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button800160.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button800160.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "800160", request.getServletPath());    
     } else if (vars.commandIn("BUTTON800160")) {
        String strMRP_Run_Production_ID = vars.getGlobalVariable("inpmrpRunProductionId", windowId + "|MRP_Run_Production_ID", "");
        String strlaunchwr = vars.getSessionValue("button800160.strlaunchwr");
        String strProcessing = vars.getSessionValue("button800160.strProcessing");
        String strOrg = vars.getSessionValue("button800160.strOrg");
        String strClient = vars.getSessionValue("button800160.strClient");
        
        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonLaunchwr800160(response, vars, strMRP_Run_Production_ID, strlaunchwr, strProcessing);
        }

     } else if (vars.commandIn("BUTTONLaunchmr800159")) {
        vars.setSessionValue("button800159.strlaunchmr", vars.getStringParameter("inplaunchmr"));
        vars.setSessionValue("button800159.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button800159.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button800159.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button800159.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "800159", request.getServletPath());    
     } else if (vars.commandIn("BUTTON800159")) {
        String strMRP_Run_Production_ID = vars.getGlobalVariable("inpmrpRunProductionId", windowId + "|MRP_Run_Production_ID", "");
        String strlaunchmr = vars.getSessionValue("button800159.strlaunchmr");
        String strProcessing = vars.getSessionValue("button800159.strProcessing");
        String strOrg = vars.getSessionValue("button800159.strOrg");
        String strClient = vars.getSessionValue("button800159.strClient");
        
        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonLaunchmr800159(response, vars, strMRP_Run_Production_ID, strlaunchmr, strProcessing);
        }

     } else if (vars.commandIn("BUTTONRecalculatestock800161")) {
        vars.setSessionValue("button800161.strrecalculatestock", vars.getStringParameter("inprecalculatestock"));
        vars.setSessionValue("button800161.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button800161.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button800161.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button800161.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "800161", request.getServletPath());    
     } else if (vars.commandIn("BUTTON800161")) {
        String strMRP_Run_Production_ID = vars.getGlobalVariable("inpmrpRunProductionId", windowId + "|MRP_Run_Production_ID", "");
        String strrecalculatestock = vars.getSessionValue("button800161.strrecalculatestock");
        String strProcessing = vars.getSessionValue("button800161.strProcessing");
        String strOrg = vars.getSessionValue("button800161.strOrg");
        String strClient = vars.getSessionValue("button800161.strClient");
        
        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonRecalculatestock800161(response, vars, strMRP_Run_Production_ID, strrecalculatestock, strProcessing);
        }

    } else if (vars.commandIn("BUTTONSimulate6FBD65B0FDB74D1AB07F0EADF18D48AE")) {
        vars.setSessionValue("button6FBD65B0FDB74D1AB07F0EADF18D48AE.strsimulate", vars.getStringParameter("inpsimulate"));
        vars.setSessionValue("button6FBD65B0FDB74D1AB07F0EADF18D48AE.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button6FBD65B0FDB74D1AB07F0EADF18D48AE.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button6FBD65B0FDB74D1AB07F0EADF18D48AE.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button6FBD65B0FDB74D1AB07F0EADF18D48AE.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "6FBD65B0FDB74D1AB07F0EADF18D48AE", request.getServletPath());
      } else if (vars.commandIn("BUTTON6FBD65B0FDB74D1AB07F0EADF18D48AE")) {
        String strMRP_Run_Production_ID = vars.getGlobalVariable("inpmrpRunProductionId", windowId + "|MRP_Run_Production_ID", "");
        String strsimulate = vars.getSessionValue("button6FBD65B0FDB74D1AB07F0EADF18D48AE.strsimulate");
        String strProcessing = vars.getSessionValue("button6FBD65B0FDB74D1AB07F0EADF18D48AE.strProcessing");
        String strOrg = vars.getSessionValue("button6FBD65B0FDB74D1AB07F0EADF18D48AE.strOrg");
        String strClient = vars.getSessionValue("button6FBD65B0FDB74D1AB07F0EADF18D48AE.strClient");

        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonSimulate6FBD65B0FDB74D1AB07F0EADF18D48AE(response, vars, strMRP_Run_Production_ID, strsimulate, strProcessing);
        }

    } else if (vars.commandIn("SAVE_BUTTONLaunchwr800160")) {
        String strMRP_Run_Production_ID = vars.getGlobalVariable("inpKey", windowId + "|MRP_Run_Production_ID", "");
        String strlaunchwr = vars.getStringParameter("inplaunchwr");
        String strProcessing = vars.getStringParameter("inpprocessing");
        OBError myMessage = null;
        try {
          String pinstance = SequenceIdData.getUUID();
          PInstanceProcessData.insertPInstance(this, pinstance, "800160", (("MRP_Run_Production_ID".equalsIgnoreCase("AD_Language"))?"0":strMRP_Run_Production_ID), strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
          
          
          ProcessBundle bundle = ProcessBundle.pinstance(pinstance, vars, this);
          new ProcessRunner(bundle).execute(this);
          
          PInstanceProcessData[] pinstanceData = PInstanceProcessData.select(this, pinstance);
          myMessage = Utility.getProcessInstanceMessage(this, vars, pinstanceData);
        } catch (ServletException ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          } else vars.setMessage(tabId, myMessage);
        }
        //close popup
        if (myMessage!=null) {
          if (log4j.isDebugEnabled()) log4j.debug(myMessage.getMessage());
          vars.setMessage(tabId, myMessage);
        }
        printPageClosePopUp(response, vars);
    } else if (vars.commandIn("SAVE_BUTTONLaunchmr800159")) {
        String strMRP_Run_Production_ID = vars.getGlobalVariable("inpKey", windowId + "|MRP_Run_Production_ID", "");
        String strlaunchmr = vars.getStringParameter("inplaunchmr");
        String strProcessing = vars.getStringParameter("inpprocessing");
        OBError myMessage = null;
        try {
          String pinstance = SequenceIdData.getUUID();
          PInstanceProcessData.insertPInstance(this, pinstance, "800159", (("MRP_Run_Production_ID".equalsIgnoreCase("AD_Language"))?"0":strMRP_Run_Production_ID), strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
          
          
          ProcessBundle bundle = ProcessBundle.pinstance(pinstance, vars, this);
          new ProcessRunner(bundle).execute(this);
          
          PInstanceProcessData[] pinstanceData = PInstanceProcessData.select(this, pinstance);
          myMessage = Utility.getProcessInstanceMessage(this, vars, pinstanceData);
        } catch (ServletException ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          } else vars.setMessage(tabId, myMessage);
        }
        //close popup
        if (myMessage!=null) {
          if (log4j.isDebugEnabled()) log4j.debug(myMessage.getMessage());
          vars.setMessage(tabId, myMessage);
        }
        printPageClosePopUp(response, vars);
    } else if (vars.commandIn("SAVE_BUTTONRecalculatestock800161")) {
        String strMRP_Run_Production_ID = vars.getGlobalVariable("inpKey", windowId + "|MRP_Run_Production_ID", "");
        String strrecalculatestock = vars.getStringParameter("inprecalculatestock");
        String strProcessing = vars.getStringParameter("inpprocessing");
        OBError myMessage = null;
        try {
          String pinstance = SequenceIdData.getUUID();
          PInstanceProcessData.insertPInstance(this, pinstance, "800161", (("MRP_Run_Production_ID".equalsIgnoreCase("AD_Language"))?"0":strMRP_Run_Production_ID), strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
          
          
          ProcessBundle bundle = ProcessBundle.pinstance(pinstance, vars, this);
          new ProcessRunner(bundle).execute(this);
          
          PInstanceProcessData[] pinstanceData = PInstanceProcessData.select(this, pinstance);
          myMessage = Utility.getProcessInstanceMessage(this, vars, pinstanceData);
        } catch (ServletException ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          } else vars.setMessage(tabId, myMessage);
        }
        //close popup
        if (myMessage!=null) {
          if (log4j.isDebugEnabled()) log4j.debug(myMessage.getMessage());
          vars.setMessage(tabId, myMessage);
        }
        printPageClosePopUp(response, vars);

    } else if (vars.commandIn("SAVE_BUTTONSimulate6FBD65B0FDB74D1AB07F0EADF18D48AE")) {
        String strMRP_Run_Production_ID = vars.getGlobalVariable("inpKey", windowId + "|MRP_Run_Production_ID", "");
        
        ProcessBundle pb = new ProcessBundle("6FBD65B0FDB74D1AB07F0EADF18D48AE", vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        params.put("MRP_Run_Production_ID", strMRP_Run_Production_ID);
        params.put("adOrgId", vars.getStringParameter("inpadOrgId"));
        params.put("adClientId", vars.getStringParameter("inpadClientId"));
        params.put("tabId", tabId);
        
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new ProcessRunner(pb).execute(this);
          myMessage = (OBError) pb.getResult();
          myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
          myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error(ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          } else vars.setMessage(tabId, myMessage);
        }
        //close popup
        if (myMessage!=null) {
          if (log4j.isDebugEnabled()) log4j.debug(myMessage.getMessage());
          vars.setMessage(tabId, myMessage);
        }
        printPageClosePopUp(response, vars);



    } else if (vars.getCommand().toUpperCase().startsWith("BUTTON") || vars.getCommand().toUpperCase().startsWith("SAVE_BUTTON")) {
      pageErrorPopUp(response);
    } else pageError(response);
  }

  private void printPageButtonFS(HttpServletResponse response, VariablesSecureApp vars, String strProcessId, String path) throws IOException, ServletException {
    log4j.debug("Output: Frames action button");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_actionButton/ActionButtonDefaultFrames").createXmlDocument();
    xmlDocument.ignoreTranslation = true;
    xmlDocument.setParameter("processId", strProcessId);
    xmlDocument.setParameter("trlFormType", "PROCESS");
    xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
    xmlDocument.setParameter("type", strDireccion + path);
    out.println(xmlDocument.print());
    out.close();
  }

    private void printPageButtonLaunchwr800160(HttpServletResponse response, VariablesSecureApp vars, String strMRP_Run_Production_ID, String strlaunchwr, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 800160");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/Launchwr800160", discard).createXmlDocument();
      xmlDocument.setParameter("key", strMRP_Run_Production_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "800160");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("800160");
        vars.removeMessage("800160");
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
    private void printPageButtonLaunchmr800159(HttpServletResponse response, VariablesSecureApp vars, String strMRP_Run_Production_ID, String strlaunchmr, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 800159");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/Launchmr800159", discard).createXmlDocument();
      xmlDocument.setParameter("key", strMRP_Run_Production_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "800159");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("800159");
        vars.removeMessage("800159");
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
    private void printPageButtonRecalculatestock800161(HttpServletResponse response, VariablesSecureApp vars, String strMRP_Run_Production_ID, String strrecalculatestock, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 800161");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/Recalculatestock800161", discard).createXmlDocument();
      xmlDocument.setParameter("key", strMRP_Run_Production_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "800161");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("800161");
        vars.removeMessage("800161");
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


    void printPageButtonSimulate6FBD65B0FDB74D1AB07F0EADF18D48AE(HttpServletResponse response, VariablesSecureApp vars, String strMRP_Run_Production_ID, String strsimulate, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 6FBD65B0FDB74D1AB07F0EADF18D48AE");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/Simulate6FBD65B0FDB74D1AB07F0EADF18D48AE", discard).createXmlDocument();
      xmlDocument.setParameter("key", strMRP_Run_Production_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "6FBD65B0FDB74D1AB07F0EADF18D48AE");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("6FBD65B0FDB74D1AB07F0EADF18D48AE");
        vars.removeMessage("6FBD65B0FDB74D1AB07F0EADF18D48AE");
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

}
