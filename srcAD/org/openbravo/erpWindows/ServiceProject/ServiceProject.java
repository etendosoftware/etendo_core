
package org.openbravo.erpWindows.ServiceProject;


import org.openbravo.erpCommon.reference.*;


import org.openbravo.erpCommon.ad_actionButton.*;


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
public class ServiceProject extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  
  private static final String windowId = "800001";
  private static final String tabId = "800002";
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
     
      try {
        securedProcess = "Y".equals(org.openbravo.erpCommon.businessUtility.Preferences
            .getPreferenceValue("SecuredProcess", true, vars.getClient(), vars.getOrg(), vars
                .getUser(), vars.getRole(), windowId));
      } catch (PropertyException e) {
      }
     
      if (command.contains("800002")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("800002");
        SessionInfo.setModuleId("0");
        if (securedProcess || explicitAccess.contains("800002")) {
          classInfo.type = "P";
          classInfo.id = "800002";
        }
      }
     
      if (command.contains("800005")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("800005");
        SessionInfo.setModuleId("0");
        if (securedProcess || explicitAccess.contains("800005")) {
          classInfo.type = "P";
          classInfo.id = "800005";
        }
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

     } else if (vars.commandIn("BUTTONChangeProjectStatus800002")) {
        vars.setSessionValue("button800002.strchangeprojectstatus", vars.getStringParameter("inpchangeprojectstatus"));
        vars.setSessionValue("button800002.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button800002.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button800002.strClient", vars.getStringParameter("inpadClientId"));
        vars.setSessionValue("button800002.inpprojectstatus", vars.getRequiredStringParameter("inpprojectstatus"));

        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button800002.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "800002", request.getServletPath());    
     } else if (vars.commandIn("BUTTON800002")) {
        String strC_Project_ID = vars.getGlobalVariable("inpcProjectId", windowId + "|C_Project_ID", "");
        String strchangeprojectstatus = vars.getSessionValue("button800002.strchangeprojectstatus");
        String strProcessing = vars.getSessionValue("button800002.strProcessing");
        String strOrg = vars.getSessionValue("button800002.strOrg");
        String strClient = vars.getSessionValue("button800002.strClient");
        
        String strprojectstatus = vars.getSessionValue("button800002.inpprojectstatus");

        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonChangeProjectStatus800002(response, vars, strC_Project_ID, strchangeprojectstatus, strProcessing, strprojectstatus);
        }

     } else if (vars.commandIn("BUTTONGenerateOrder800005")) {
        vars.setSessionValue("button800005.strgenerateorder", vars.getStringParameter("inpgenerateorder"));
        vars.setSessionValue("button800005.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button800005.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button800005.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button800005.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "800005", request.getServletPath());    
     } else if (vars.commandIn("BUTTON800005")) {
        String strC_Project_ID = vars.getGlobalVariable("inpcProjectId", windowId + "|C_Project_ID", "");
        String strgenerateorder = vars.getSessionValue("button800005.strgenerateorder");
        String strProcessing = vars.getSessionValue("button800005.strProcessing");
        String strOrg = vars.getSessionValue("button800005.strOrg");
        String strClient = vars.getSessionValue("button800005.strClient");
        
        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonGenerateOrder800005(response, vars, strC_Project_ID, strgenerateorder, strProcessing);
        }


    } else if (vars.commandIn("SAVE_BUTTONChangeProjectStatus800002")) {
        String strC_Project_ID = vars.getGlobalVariable("inpKey", windowId + "|C_Project_ID", "");
        String strchangeprojectstatus = vars.getStringParameter("inpchangeprojectstatus");
        String strProcessing = vars.getStringParameter("inpprocessing");
        OBError myMessage = null;
        try {
          String pinstance = SequenceIdData.getUUID();
          PInstanceProcessData.insertPInstance(this, pinstance, "800002", (("C_Project_ID".equalsIgnoreCase("AD_Language"))?"0":strC_Project_ID), strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
          PInstanceProcessData.insertPInstanceParam(this, pinstance, "0", "ChangeProjectStatus", strchangeprojectstatus, vars.getClient(), vars.getOrg(), vars.getUser());

          
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
    } else if (vars.commandIn("SAVE_BUTTONGenerateOrder800005")) {
        String strC_Project_ID = vars.getGlobalVariable("inpKey", windowId + "|C_Project_ID", "");
        String strgenerateorder = vars.getStringParameter("inpgenerateorder");
        String strProcessing = vars.getStringParameter("inpprocessing");
        OBError myMessage = null;
        try {
          String pinstance = SequenceIdData.getUUID();
          PInstanceProcessData.insertPInstance(this, pinstance, "800005", (("C_Project_ID".equalsIgnoreCase("AD_Language"))?"0":strC_Project_ID), strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
          
          
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

    private void printPageButtonChangeProjectStatus800002(HttpServletResponse response, VariablesSecureApp vars, String strC_Project_ID, String strchangeprojectstatus, String strProcessing, String strprojectstatus)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 800002");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ChangeProjectStatus", discard).createXmlDocument();
      xmlDocument.setParameter("key", strC_Project_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "ServiceProject_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "800002");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("800002");
        vars.removeMessage("800002");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

      xmlDocument.setParameter("projectstatus", strprojectstatus);
    try {
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
xmlDocument.setParameter("processId", "800002");
xmlDocument.setParameter("processDescription", "Change project status");
xmlDocument.setParameter("projectaction", strchangeprojectstatus);
FieldProvider[] dataProjectAction = ActionButtonUtility.projectAction(this, vars, strchangeprojectstatus, "800002", strprojectstatus);
xmlDocument.setData("reportprojectaction", "liststructure", dataProjectAction);
StringBuffer dact = new StringBuffer();
if (dataProjectAction!=null) {
  dact.append("var arrProjectAction = new Array(\n");
  for (int i=0;i<dataProjectAction.length;i++) {
    dact.append("new Array(\"" + dataProjectAction[i].getField("id") + "\", \"" + dataProjectAction[i].getField("name") + "\", \"" + dataProjectAction[i].getField("description") + "\")\n");
    if (i<dataProjectAction.length-1) dact.append(",\n");
  }
  dact.append(");");
} else dact.append("var arrProjectAction = null");
xmlDocument.setParameter("array", dact.toString());

      
      out.println(xmlDocument.print());
      out.close();
    }
    private void printPageButtonGenerateOrder800005(HttpServletResponse response, VariablesSecureApp vars, String strC_Project_ID, String strgenerateorder, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 800005");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/GenerateOrder800005", discard).createXmlDocument();
      xmlDocument.setParameter("key", strC_Project_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "ServiceProject_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "800005");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("800005");
        vars.removeMessage("800005");
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
