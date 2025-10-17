
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
public class ReconciliationsFF8080813320657F0133209DE21B0042 extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  
  private static final String windowId = "94EAA455D2644E04AB25D93BE5157B6D";
  private static final String tabId = "FF8080813320657F0133209DE21B0042";
  private static final int accesslevel = 1;
  private static final String moduleId = "A918E3331C404B889D69AA9BFAFB23AC";
  
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
     List<String> explicitAccess = Arrays.asList("6BF16EFC772843AC9A17552AE0B26AB7",  "");
    
     SessionInfo.setUserId(vars.getSessionValue("#AD_User_ID"));
     SessionInfo.setSessionId(vars.getSessionValue("#AD_Session_ID"));
     SessionInfo.setQueryProfile("manualProcess");
     
      if (command.contains("FF8080812E2F8EAE012E2F94CF470014")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("FF8080812E2F8EAE012E2F94CF470014");
        SessionInfo.setModuleId("A918E3331C404B889D69AA9BFAFB23AC");
      }
     
      if (command.contains("6BF16EFC772843AC9A17552AE0B26AB7")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("6BF16EFC772843AC9A17552AE0B26AB7");
        SessionInfo.setModuleId("A918E3331C404B889D69AA9BFAFB23AC");
      }
     
      try {
        securedProcess = "Y".equals(org.openbravo.erpCommon.businessUtility.Preferences
            .getPreferenceValue("SecuredProcess", true, vars.getClient(), vars.getOrg(), vars
                .getUser(), vars.getRole(), windowId));
      } catch (PropertyException e) {
      }
     

     
      if (explicitAccess.contains("FF8080812E2F8EAE012E2F94CF470014") || (securedProcess && command.contains("FF8080812E2F8EAE012E2F94CF470014"))) {
        classInfo.type = "P";
        classInfo.id = "FF8080812E2F8EAE012E2F94CF470014";
      }
     
      if (explicitAccess.contains("6BF16EFC772843AC9A17552AE0B26AB7") || (securedProcess && command.contains("6BF16EFC772843AC9A17552AE0B26AB7"))) {
        classInfo.type = "P";
        classInfo.id = "6BF16EFC772843AC9A17552AE0B26AB7";
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

    } else if (vars.commandIn("BUTTONEM_Aprm_Process_RecFF8080812E2F8EAE012E2F94CF470014")) {
        vars.setSessionValue("buttonFF8080812E2F8EAE012E2F94CF470014.stremAprmProcessRec", vars.getStringParameter("inpemAprmProcessRec"));
        vars.setSessionValue("buttonFF8080812E2F8EAE012E2F94CF470014.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("buttonFF8080812E2F8EAE012E2F94CF470014.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("buttonFF8080812E2F8EAE012E2F94CF470014.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        p.put("Processed", vars.getStringParameter("inpprocessed"));
p.put("Processed", vars.getStringParameter("inpprocessed"));

        
        //Save in session needed params for combos if needed
        vars.setSessionObject("buttonFF8080812E2F8EAE012E2F94CF470014.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "FF8080812E2F8EAE012E2F94CF470014", request.getServletPath());
      } else if (vars.commandIn("BUTTONFF8080812E2F8EAE012E2F94CF470014")) {
        String strFIN_Reconciliation_ID = vars.getGlobalVariable("inpfinReconciliationId", windowId + "|FIN_Reconciliation_ID", "");
        String stremAprmProcessRec = vars.getSessionValue("buttonFF8080812E2F8EAE012E2F94CF470014.stremAprmProcessRec");
        String strProcessing = vars.getSessionValue("buttonFF8080812E2F8EAE012E2F94CF470014.strProcessing");
        String strOrg = vars.getSessionValue("buttonFF8080812E2F8EAE012E2F94CF470014.strOrg");
        String strClient = vars.getSessionValue("buttonFF8080812E2F8EAE012E2F94CF470014.strClient");

        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonEM_Aprm_Process_RecFF8080812E2F8EAE012E2F94CF470014(response, vars, strFIN_Reconciliation_ID, stremAprmProcessRec, strProcessing);
        }
    } else if (vars.commandIn("BUTTONEM_APRM_Process_Rec_Force6BF16EFC772843AC9A17552AE0B26AB7")) {
        vars.setSessionValue("button6BF16EFC772843AC9A17552AE0B26AB7.stremAprmProcessRecForce", vars.getStringParameter("inpemAprmProcessRecForce"));
        vars.setSessionValue("button6BF16EFC772843AC9A17552AE0B26AB7.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button6BF16EFC772843AC9A17552AE0B26AB7.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button6BF16EFC772843AC9A17552AE0B26AB7.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        p.put("Processed", vars.getStringParameter("inpprocessed"));

        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button6BF16EFC772843AC9A17552AE0B26AB7.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "6BF16EFC772843AC9A17552AE0B26AB7", request.getServletPath());
      } else if (vars.commandIn("BUTTON6BF16EFC772843AC9A17552AE0B26AB7")) {
        String strFIN_Reconciliation_ID = vars.getGlobalVariable("inpfinReconciliationId", windowId + "|FIN_Reconciliation_ID", "");
        String stremAprmProcessRecForce = vars.getSessionValue("button6BF16EFC772843AC9A17552AE0B26AB7.stremAprmProcessRecForce");
        String strProcessing = vars.getSessionValue("button6BF16EFC772843AC9A17552AE0B26AB7.strProcessing");
        String strOrg = vars.getSessionValue("button6BF16EFC772843AC9A17552AE0B26AB7.strOrg");
        String strClient = vars.getSessionValue("button6BF16EFC772843AC9A17552AE0B26AB7.strClient");

        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonEM_APRM_Process_Rec_Force6BF16EFC772843AC9A17552AE0B26AB7(response, vars, strFIN_Reconciliation_ID, stremAprmProcessRecForce, strProcessing);
        }


    } else if (vars.commandIn("SAVE_BUTTONEM_Aprm_Process_RecFF8080812E2F8EAE012E2F94CF470014")) {
        String strFIN_Reconciliation_ID = vars.getGlobalVariable("inpKey", windowId + "|FIN_Reconciliation_ID", "");
        
        ProcessBundle pb = new ProcessBundle("FF8080812E2F8EAE012E2F94CF470014", vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        params.put("FIN_Reconciliation_ID", strFIN_Reconciliation_ID);
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
    } else if (vars.commandIn("SAVE_BUTTONEM_APRM_Process_Rec_Force6BF16EFC772843AC9A17552AE0B26AB7")) {
        String strFIN_Reconciliation_ID = vars.getGlobalVariable("inpKey", windowId + "|FIN_Reconciliation_ID", "");
        
        ProcessBundle pb = new ProcessBundle("6BF16EFC772843AC9A17552AE0B26AB7", vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        params.put("FIN_Reconciliation_ID", strFIN_Reconciliation_ID);
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
        String strFIN_Reconciliation_ID = vars.getGlobalVariable("inpfinReconciliationId", windowId + "|FIN_Reconciliation_ID", "");
        String strTableId = "B1B7075C46934F0A9FD4C4D0F1457B42";
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
          vars.setSessionValue("Posted|key", strFIN_Reconciliation_ID);
          vars.setSessionValue("Posted|tableId", strTableId);
          vars.setSessionValue("Posted|tabId", tabId);
          vars.setSessionValue("Posted|posted", strPosted);
          vars.setSessionValue("Posted|processId", strProcessId);
          vars.setSessionValue("Posted|path", strDireccion + request.getServletPath());
          vars.setSessionValue("Posted|windowId", windowId);
          vars.setSessionValue("Posted|tabName", "ReconciliationsFF8080813320657F0133209DE21B0042");
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



    void printPageButtonEM_Aprm_Process_RecFF8080812E2F8EAE012E2F94CF470014(HttpServletResponse response, VariablesSecureApp vars, String strFIN_Reconciliation_ID, String stremAprmProcessRec, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process FF8080812E2F8EAE012E2F94CF470014");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/EM_Aprm_Process_RecFF8080812E2F8EAE012E2F94CF470014", discard).createXmlDocument();
      xmlDocument.setParameter("key", strFIN_Reconciliation_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "ReconciliationsFF8080813320657F0133209DE21B0042_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "FF8080812E2F8EAE012E2F94CF470014");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("FF8080812E2F8EAE012E2F94CF470014");
        vars.removeMessage("FF8080812E2F8EAE012E2F94CF470014");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("action", Utility.getContext(this, vars, "Process_Reconciliation", "94EAA455D2644E04AB25D93BE5157B6D"));
    comboTableData = new ComboTableData(vars, this, "17", "action", "FF8080812E443491012E443C053A001A", "FF808081332719060133271E5BB1001B", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, (FieldProvider) vars.getSessionObject("buttonFF8080812E2F8EAE012E2F94CF470014.originalParams"), comboTableData, windowId, Utility.getContext(this, vars, "Process_Reconciliation", "94EAA455D2644E04AB25D93BE5157B6D"));
    xmlDocument.setData("reportaction", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      
      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButtonEM_APRM_Process_Rec_Force6BF16EFC772843AC9A17552AE0B26AB7(HttpServletResponse response, VariablesSecureApp vars, String strFIN_Reconciliation_ID, String stremAprmProcessRecForce, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 6BF16EFC772843AC9A17552AE0B26AB7");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/EM_APRM_Process_Rec_Force6BF16EFC772843AC9A17552AE0B26AB7", discard).createXmlDocument();
      xmlDocument.setParameter("key", strFIN_Reconciliation_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "ReconciliationsFF8080813320657F0133209DE21B0042_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "6BF16EFC772843AC9A17552AE0B26AB7");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("6BF16EFC772843AC9A17552AE0B26AB7");
        vars.removeMessage("6BF16EFC772843AC9A17552AE0B26AB7");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("action", Utility.getContext(this, vars, "Process_Reconciliation", "94EAA455D2644E04AB25D93BE5157B6D"));
    comboTableData = new ComboTableData(vars, this, "17", "action", "FF8080812E443491012E443C053A001A", "FF808081332719060133271E5BB1001B", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, (FieldProvider) vars.getSessionObject("button6BF16EFC772843AC9A17552AE0B26AB7.originalParams"), comboTableData, windowId, Utility.getContext(this, vars, "Process_Reconciliation", "94EAA455D2644E04AB25D93BE5157B6D"));
    xmlDocument.setData("reportaction", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      
      out.println(xmlDocument.print());
      out.close();
    }

}
