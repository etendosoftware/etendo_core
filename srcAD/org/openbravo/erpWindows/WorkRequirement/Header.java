
package org.openbravo.erpWindows.WorkRequirement;


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
  
  private static final String windowId = "800052";
  private static final String tabId = "800111";
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
     
      if (command.contains("FF808181324D007801324D2AE1130066")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("FF808181324D007801324D2AE1130066");
        SessionInfo.setModuleId("0");
      }
     
      try {
        securedProcess = "Y".equals(org.openbravo.erpCommon.businessUtility.Preferences
            .getPreferenceValue("SecuredProcess", true, vars.getClient(), vars.getOrg(), vars
                .getUser(), vars.getRole(), windowId));
      } catch (PropertyException e) {
      }
     
      if (command.contains("800103")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("800103");
        SessionInfo.setModuleId("0");
        if (securedProcess || explicitAccess.contains("800103")) {
          classInfo.type = "P";
          classInfo.id = "800103";
        }
      }
     
      if (command.contains("800117")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("800117");
        SessionInfo.setModuleId("0");
        if (securedProcess || explicitAccess.contains("800117")) {
          classInfo.type = "P";
          classInfo.id = "800117";
        }
      }
     

     
      if (explicitAccess.contains("FF808181324D007801324D2AE1130066") || (securedProcess && command.contains("FF808181324D007801324D2AE1130066"))) {
        classInfo.type = "P";
        classInfo.id = "FF808181324D007801324D2AE1130066";
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

     } else if (vars.commandIn("BUTTONExplote800103")) {
        vars.setSessionValue("button800103.strexplote", vars.getStringParameter("inpexplote"));
        vars.setSessionValue("button800103.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button800103.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button800103.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button800103.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "800103", request.getServletPath());    
     } else if (vars.commandIn("BUTTON800103")) {
        String strMA_Workrequirement_ID = vars.getGlobalVariable("inpmaWorkrequirementId", windowId + "|MA_Workrequirement_ID", "");
        String strexplote = vars.getSessionValue("button800103.strexplote");
        String strProcessing = vars.getSessionValue("button800103.strProcessing");
        String strOrg = vars.getSessionValue("button800103.strOrg");
        String strClient = vars.getSessionValue("button800103.strClient");
        
        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonExplote800103(response, vars, strMA_Workrequirement_ID, strexplote, strProcessing);
        }

     } else if (vars.commandIn("BUTTONClosed800117")) {
        vars.setSessionValue("button800117.strclosed", vars.getStringParameter("inpclosed"));
        vars.setSessionValue("button800117.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button800117.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button800117.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button800117.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "800117", request.getServletPath());    
     } else if (vars.commandIn("BUTTON800117")) {
        String strMA_Workrequirement_ID = vars.getGlobalVariable("inpmaWorkrequirementId", windowId + "|MA_Workrequirement_ID", "");
        String strclosed = vars.getSessionValue("button800117.strclosed");
        String strProcessing = vars.getSessionValue("button800117.strProcessing");
        String strOrg = vars.getSessionValue("button800117.strOrg");
        String strClient = vars.getSessionValue("button800117.strClient");
        
        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonClosed800117(response, vars, strMA_Workrequirement_ID, strclosed, strProcessing);
        }

    } else if (vars.commandIn("BUTTONCreateworkrequirementFF808181324D007801324D2AE1130066")) {
        vars.setSessionValue("buttonFF808181324D007801324D2AE1130066.strcreateworkrequirement", vars.getStringParameter("inpcreateworkrequirement"));
        vars.setSessionValue("buttonFF808181324D007801324D2AE1130066.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("buttonFF808181324D007801324D2AE1130066.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("buttonFF808181324D007801324D2AE1130066.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("buttonFF808181324D007801324D2AE1130066.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "FF808181324D007801324D2AE1130066", request.getServletPath());
      } else if (vars.commandIn("BUTTONFF808181324D007801324D2AE1130066")) {
        String strMA_Workrequirement_ID = vars.getGlobalVariable("inpmaWorkrequirementId", windowId + "|MA_Workrequirement_ID", "");
        String strcreateworkrequirement = vars.getSessionValue("buttonFF808181324D007801324D2AE1130066.strcreateworkrequirement");
        String strProcessing = vars.getSessionValue("buttonFF808181324D007801324D2AE1130066.strProcessing");
        String strOrg = vars.getSessionValue("buttonFF808181324D007801324D2AE1130066.strOrg");
        String strClient = vars.getSessionValue("buttonFF808181324D007801324D2AE1130066.strClient");

        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonCreateworkrequirementFF808181324D007801324D2AE1130066(response, vars, strMA_Workrequirement_ID, strcreateworkrequirement, strProcessing);
        }

    } else if (vars.commandIn("SAVE_BUTTONExplote800103")) {
        String strMA_Workrequirement_ID = vars.getGlobalVariable("inpKey", windowId + "|MA_Workrequirement_ID", "");
        String strexplote = vars.getStringParameter("inpexplote");
        String strProcessing = vars.getStringParameter("inpprocessing");
        OBError myMessage = null;
        try {
          String pinstance = SequenceIdData.getUUID();
          PInstanceProcessData.insertPInstance(this, pinstance, "800103", (("MA_Workrequirement_ID".equalsIgnoreCase("AD_Language"))?"0":strMA_Workrequirement_ID), strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
          
          
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
    } else if (vars.commandIn("SAVE_BUTTONClosed800117")) {
        String strMA_Workrequirement_ID = vars.getGlobalVariable("inpKey", windowId + "|MA_Workrequirement_ID", "");
        String strclosed = vars.getStringParameter("inpclosed");
        String strProcessing = vars.getStringParameter("inpprocessing");
        OBError myMessage = null;
        try {
          String pinstance = SequenceIdData.getUUID();
          PInstanceProcessData.insertPInstance(this, pinstance, "800117", (("MA_Workrequirement_ID".equalsIgnoreCase("AD_Language"))?"0":strMA_Workrequirement_ID), strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
          
          
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

    } else if (vars.commandIn("SAVE_BUTTONCreateworkrequirementFF808181324D007801324D2AE1130066")) {
        String strMA_Workrequirement_ID = vars.getGlobalVariable("inpKey", windowId + "|MA_Workrequirement_ID", "");
        
        ProcessBundle pb = new ProcessBundle("FF808181324D007801324D2AE1130066", vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        params.put("MA_Workrequirement_ID", strMA_Workrequirement_ID);
        params.put("adOrgId", vars.getStringParameter("inpadOrgId"));
        params.put("adClientId", vars.getStringParameter("inpadClientId"));
        params.put("tabId", tabId);
        
        String strdate = vars.getStringParameter("inpdate");
params.put("date", strdate);
String strstarttime = vars.getStringParameter("inpstarttime");
params.put("starttime", strstarttime);
String strendtime = vars.getStringParameter("inpendtime");
params.put("endtime", strendtime);

        
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

    private void printPageButtonExplote800103(HttpServletResponse response, VariablesSecureApp vars, String strMA_Workrequirement_ID, String strexplote, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 800103");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/Explote800103", discard).createXmlDocument();
      xmlDocument.setParameter("key", strMA_Workrequirement_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "800103");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("800103");
        vars.removeMessage("800103");
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
    private void printPageButtonClosed800117(HttpServletResponse response, VariablesSecureApp vars, String strMA_Workrequirement_ID, String strclosed, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 800117");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/Closed800117", discard).createXmlDocument();
      xmlDocument.setParameter("key", strMA_Workrequirement_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "800117");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("800117");
        vars.removeMessage("800117");
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


    void printPageButtonCreateworkrequirementFF808181324D007801324D2AE1130066(HttpServletResponse response, VariablesSecureApp vars, String strMA_Workrequirement_ID, String strcreateworkrequirement, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process FF808181324D007801324D2AE1130066");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/CreateworkrequirementFF808181324D007801324D2AE1130066", discard).createXmlDocument();
      xmlDocument.setParameter("key", strMA_Workrequirement_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "FF808181324D007801324D2AE1130066");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("FF808181324D007801324D2AE1130066");
        vars.removeMessage("FF808181324D007801324D2AE1130066");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    xmlDocument.setParameter("Date", "");
    xmlDocument.setParameter("Date_Format", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("Starttime", "");
    xmlDocument.setParameter("Endtime", "");
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      
      out.println(xmlDocument.print());
      out.close();
    }

}
