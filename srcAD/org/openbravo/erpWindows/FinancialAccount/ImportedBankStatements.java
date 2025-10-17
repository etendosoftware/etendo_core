
package org.openbravo.erpWindows.FinancialAccount;


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
public class ImportedBankStatements extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  
  private static final String windowId = "94EAA455D2644E04AB25D93BE5157B6D";
  private static final String tabId = "C56E698100314ABBBBD3A89626CA551C";
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
     List<String> explicitAccess = Arrays.asList("2DDE7D3618034C38A4462B7F3456C28D",  "");
    
     SessionInfo.setUserId(vars.getSessionValue("#AD_User_ID"));
     SessionInfo.setSessionId(vars.getSessionValue("#AD_Session_ID"));
     SessionInfo.setQueryProfile("manualProcess");
     
      if (command.contains("2DDE7D3618034C38A4462B7F3456C28D")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("2DDE7D3618034C38A4462B7F3456C28D");
        SessionInfo.setModuleId("A918E3331C404B889D69AA9BFAFB23AC");
      }
     
      if (command.contains("58A9261BACEF45DDA526F29D8557272D")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("58A9261BACEF45DDA526F29D8557272D");
        SessionInfo.setModuleId("A918E3331C404B889D69AA9BFAFB23AC");
      }
     
      try {
        securedProcess = "Y".equals(org.openbravo.erpCommon.businessUtility.Preferences
            .getPreferenceValue("SecuredProcess", true, vars.getClient(), vars.getOrg(), vars
                .getUser(), vars.getRole(), windowId));
      } catch (PropertyException e) {
      }
     

     
      if (explicitAccess.contains("2DDE7D3618034C38A4462B7F3456C28D") || (securedProcess && command.contains("2DDE7D3618034C38A4462B7F3456C28D"))) {
        classInfo.type = "P";
        classInfo.id = "2DDE7D3618034C38A4462B7F3456C28D";
      }
     
      if (explicitAccess.contains("58A9261BACEF45DDA526F29D8557272D") || (securedProcess && command.contains("58A9261BACEF45DDA526F29D8557272D"))) {
        classInfo.type = "P";
        classInfo.id = "58A9261BACEF45DDA526F29D8557272D";
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

    } else if (vars.commandIn("BUTTONEM_APRM_Process_BS_Force2DDE7D3618034C38A4462B7F3456C28D")) {
        vars.setSessionValue("button2DDE7D3618034C38A4462B7F3456C28D.stremAprmProcessBsForce", vars.getStringParameter("inpemAprmProcessBsForce"));
        vars.setSessionValue("button2DDE7D3618034C38A4462B7F3456C28D.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button2DDE7D3618034C38A4462B7F3456C28D.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button2DDE7D3618034C38A4462B7F3456C28D.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        p.put("Processed", vars.getStringParameter("inpprocessed"));

        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button2DDE7D3618034C38A4462B7F3456C28D.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "2DDE7D3618034C38A4462B7F3456C28D", request.getServletPath());
      } else if (vars.commandIn("BUTTON2DDE7D3618034C38A4462B7F3456C28D")) {
        String strFIN_Bankstatement_ID = vars.getGlobalVariable("inpfinBankstatementId", windowId + "|FIN_Bankstatement_ID", "");
        String stremAprmProcessBsForce = vars.getSessionValue("button2DDE7D3618034C38A4462B7F3456C28D.stremAprmProcessBsForce");
        String strProcessing = vars.getSessionValue("button2DDE7D3618034C38A4462B7F3456C28D.strProcessing");
        String strOrg = vars.getSessionValue("button2DDE7D3618034C38A4462B7F3456C28D.strOrg");
        String strClient = vars.getSessionValue("button2DDE7D3618034C38A4462B7F3456C28D.strClient");

        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonEM_APRM_Process_BS_Force2DDE7D3618034C38A4462B7F3456C28D(response, vars, strFIN_Bankstatement_ID, stremAprmProcessBsForce, strProcessing);
        }
    } else if (vars.commandIn("BUTTONEM_APRM_Process_BS58A9261BACEF45DDA526F29D8557272D")) {
        vars.setSessionValue("button58A9261BACEF45DDA526F29D8557272D.stremAprmProcessBs", vars.getStringParameter("inpemAprmProcessBs"));
        vars.setSessionValue("button58A9261BACEF45DDA526F29D8557272D.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button58A9261BACEF45DDA526F29D8557272D.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button58A9261BACEF45DDA526F29D8557272D.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        p.put("Processed", vars.getStringParameter("inpprocessed"));

        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button58A9261BACEF45DDA526F29D8557272D.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "58A9261BACEF45DDA526F29D8557272D", request.getServletPath());
      } else if (vars.commandIn("BUTTON58A9261BACEF45DDA526F29D8557272D")) {
        String strFIN_Bankstatement_ID = vars.getGlobalVariable("inpfinBankstatementId", windowId + "|FIN_Bankstatement_ID", "");
        String stremAprmProcessBs = vars.getSessionValue("button58A9261BACEF45DDA526F29D8557272D.stremAprmProcessBs");
        String strProcessing = vars.getSessionValue("button58A9261BACEF45DDA526F29D8557272D.strProcessing");
        String strOrg = vars.getSessionValue("button58A9261BACEF45DDA526F29D8557272D.strOrg");
        String strClient = vars.getSessionValue("button58A9261BACEF45DDA526F29D8557272D.strClient");

        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonEM_APRM_Process_BS58A9261BACEF45DDA526F29D8557272D(response, vars, strFIN_Bankstatement_ID, stremAprmProcessBs, strProcessing);
        }


    } else if (vars.commandIn("SAVE_BUTTONEM_APRM_Process_BS_Force2DDE7D3618034C38A4462B7F3456C28D")) {
        String strFIN_Bankstatement_ID = vars.getGlobalVariable("inpKey", windowId + "|FIN_Bankstatement_ID", "");
        
        ProcessBundle pb = new ProcessBundle("2DDE7D3618034C38A4462B7F3456C28D", vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        params.put("FIN_Bankstatement_ID", strFIN_Bankstatement_ID);
        params.put("adOrgId", vars.getStringParameter("inpadOrgId"));
        params.put("adClientId", vars.getStringParameter("inpadClientId"));
        params.put("tabId", tabId);
        
        String straction = vars.getStringParameter("inpaction");
params.put("action", straction);

        
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
    } else if (vars.commandIn("SAVE_BUTTONEM_APRM_Process_BS58A9261BACEF45DDA526F29D8557272D")) {
        String strFIN_Bankstatement_ID = vars.getGlobalVariable("inpKey", windowId + "|FIN_Bankstatement_ID", "");
        
        ProcessBundle pb = new ProcessBundle("58A9261BACEF45DDA526F29D8557272D", vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        params.put("FIN_Bankstatement_ID", strFIN_Bankstatement_ID);
        params.put("adOrgId", vars.getStringParameter("inpadOrgId"));
        params.put("adClientId", vars.getStringParameter("inpadClientId"));
        params.put("tabId", tabId);
        
        String straction = vars.getStringParameter("inpaction");
params.put("action", straction);

        
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


    } else if (vars.commandIn("BUTTONPosted")) {
        String strFIN_Bankstatement_ID = vars.getGlobalVariable("inpfinBankstatementId", windowId + "|FIN_Bankstatement_ID", "");
        String strTableId = "D4C23A17190649E7B78F55A05AF3438C";
        String strPosted = vars.getStringParameter("inpposted");
        String strProcessId = "";
        log4j.debug("Loading Posted button in table: " + strTableId);
        String strOrg = vars.getStringParameter("inpadOrgId");
        String strClient = vars.getStringParameter("inpadClientId");
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{
          vars.setSessionValue("Posted|key", strFIN_Bankstatement_ID);
          vars.setSessionValue("Posted|tableId", strTableId);
          vars.setSessionValue("Posted|tabId", tabId);
          vars.setSessionValue("Posted|posted", strPosted);
          vars.setSessionValue("Posted|processId", strProcessId);
          vars.setSessionValue("Posted|path", strDireccion + request.getServletPath());
          vars.setSessionValue("Posted|windowId", windowId);
          vars.setSessionValue("Posted|tabName", "ImportedBankStatements");
          response.sendRedirect(strDireccion + "/ad_actionButton/Posted.html");
        }

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



    void printPageButtonEM_APRM_Process_BS_Force2DDE7D3618034C38A4462B7F3456C28D(HttpServletResponse response, VariablesSecureApp vars, String strFIN_Bankstatement_ID, String stremAprmProcessBsForce, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 2DDE7D3618034C38A4462B7F3456C28D");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/EM_APRM_Process_BS_Force2DDE7D3618034C38A4462B7F3456C28D", discard).createXmlDocument();
      xmlDocument.setParameter("key", strFIN_Bankstatement_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "ImportedBankStatements_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "2DDE7D3618034C38A4462B7F3456C28D");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("2DDE7D3618034C38A4462B7F3456C28D");
        vars.removeMessage("2DDE7D3618034C38A4462B7F3456C28D");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("action", Utility.getContext(this, vars, "EM_APRM_Process_BS", "94EAA455D2644E04AB25D93BE5157B6D"));
    comboTableData = new ComboTableData(vars, this, "17", "action", "EC75B6F5A9504DB6B3F3356EA85F15EE", "CA425689672A42D7BE2158EE41E44F94", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, (FieldProvider) vars.getSessionObject("button2DDE7D3618034C38A4462B7F3456C28D.originalParams"), comboTableData, windowId, Utility.getContext(this, vars, "EM_APRM_Process_BS", "94EAA455D2644E04AB25D93BE5157B6D"));
    xmlDocument.setData("reportaction", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      
      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButtonEM_APRM_Process_BS58A9261BACEF45DDA526F29D8557272D(HttpServletResponse response, VariablesSecureApp vars, String strFIN_Bankstatement_ID, String stremAprmProcessBs, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 58A9261BACEF45DDA526F29D8557272D");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/EM_APRM_Process_BS58A9261BACEF45DDA526F29D8557272D", discard).createXmlDocument();
      xmlDocument.setParameter("key", strFIN_Bankstatement_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "ImportedBankStatements_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "58A9261BACEF45DDA526F29D8557272D");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("58A9261BACEF45DDA526F29D8557272D");
        vars.removeMessage("58A9261BACEF45DDA526F29D8557272D");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("action", Utility.getContext(this, vars, "EM_APRM_Process_BS", "94EAA455D2644E04AB25D93BE5157B6D"));
    comboTableData = new ComboTableData(vars, this, "17", "action", "EC75B6F5A9504DB6B3F3356EA85F15EE", "CA425689672A42D7BE2158EE41E44F94", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, (FieldProvider) vars.getSessionObject("button58A9261BACEF45DDA526F29D8557272D.originalParams"), comboTableData, windowId, Utility.getContext(this, vars, "EM_APRM_Process_BS", "94EAA455D2644E04AB25D93BE5157B6D"));
    xmlDocument.setData("reportaction", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      
      out.println(xmlDocument.print());
      out.close();
    }

}
