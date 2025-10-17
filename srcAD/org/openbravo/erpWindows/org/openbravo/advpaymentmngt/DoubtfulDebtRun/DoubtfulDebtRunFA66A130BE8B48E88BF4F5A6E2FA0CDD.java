
package org.openbravo.erpWindows.org.openbravo.advpaymentmngt.DoubtfulDebtRun;


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
public class DoubtfulDebtRunFA66A130BE8B48E88BF4F5A6E2FA0CDD extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  
  private static final String windowId = "681E982F747A48A6AF328A236A067559";
  private static final String tabId = "FA66A130BE8B48E88BF4F5A6E2FA0CDD";
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
     List<String> explicitAccess = Arrays.asList( "");
    
     SessionInfo.setUserId(vars.getSessionValue("#AD_User_ID"));
     SessionInfo.setSessionId(vars.getSessionValue("#AD_Session_ID"));
     SessionInfo.setQueryProfile("manualProcess");
     
      if (command.contains("017312F51139438A9665775E3B5392A1")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("017312F51139438A9665775E3B5392A1");
        SessionInfo.setModuleId("A918E3331C404B889D69AA9BFAFB23AC");
      }
     
      try {
        securedProcess = "Y".equals(org.openbravo.erpCommon.businessUtility.Preferences
            .getPreferenceValue("SecuredProcess", true, vars.getClient(), vars.getOrg(), vars
                .getUser(), vars.getRole(), windowId));
      } catch (PropertyException e) {
      }
     

     
      if (explicitAccess.contains("017312F51139438A9665775E3B5392A1") || (securedProcess && command.contains("017312F51139438A9665775E3B5392A1"))) {
        classInfo.type = "P";
        classInfo.id = "017312F51139438A9665775E3B5392A1";
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

    } else if (vars.commandIn("BUTTONEM_APRM_Process017312F51139438A9665775E3B5392A1")) {
        vars.setSessionValue("button017312F51139438A9665775E3B5392A1.stremAprmProcess", vars.getStringParameter("inpemAprmProcess"));
        vars.setSessionValue("button017312F51139438A9665775E3B5392A1.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button017312F51139438A9665775E3B5392A1.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button017312F51139438A9665775E3B5392A1.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        p.put("EM_APRM_Process", vars.getStringParameter("inpemAprmProcess"));

        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button017312F51139438A9665775E3B5392A1.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "017312F51139438A9665775E3B5392A1", request.getServletPath());
      } else if (vars.commandIn("BUTTON017312F51139438A9665775E3B5392A1")) {
        String strFIN_Doubtful_Debt_Run_ID = vars.getGlobalVariable("inpfinDoubtfulDebtRunId", windowId + "|FIN_Doubtful_Debt_Run_ID", "");
        String stremAprmProcess = vars.getSessionValue("button017312F51139438A9665775E3B5392A1.stremAprmProcess");
        String strProcessing = vars.getSessionValue("button017312F51139438A9665775E3B5392A1.strProcessing");
        String strOrg = vars.getSessionValue("button017312F51139438A9665775E3B5392A1.strOrg");
        String strClient = vars.getSessionValue("button017312F51139438A9665775E3B5392A1.strClient");

        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonEM_APRM_Process017312F51139438A9665775E3B5392A1(response, vars, strFIN_Doubtful_Debt_Run_ID, stremAprmProcess, strProcessing);
        }


    } else if (vars.commandIn("SAVE_BUTTONEM_APRM_Process017312F51139438A9665775E3B5392A1")) {
        String strFIN_Doubtful_Debt_Run_ID = vars.getGlobalVariable("inpKey", windowId + "|FIN_Doubtful_Debt_Run_ID", "");
        
        ProcessBundle pb = new ProcessBundle("017312F51139438A9665775E3B5392A1", vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        params.put("FIN_Doubtful_Debt_Run_ID", strFIN_Doubtful_Debt_Run_ID);
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



    void printPageButtonEM_APRM_Process017312F51139438A9665775E3B5392A1(HttpServletResponse response, VariablesSecureApp vars, String strFIN_Doubtful_Debt_Run_ID, String stremAprmProcess, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 017312F51139438A9665775E3B5392A1");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/EM_APRM_Process017312F51139438A9665775E3B5392A1", discard).createXmlDocument();
      xmlDocument.setParameter("key", strFIN_Doubtful_Debt_Run_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "DoubtfulDebtRunFA66A130BE8B48E88BF4F5A6E2FA0CDD_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "017312F51139438A9665775E3B5392A1");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("017312F51139438A9665775E3B5392A1");
        vars.removeMessage("017312F51139438A9665775E3B5392A1");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("action", "");
    comboTableData = new ComboTableData(vars, this, "17", "action", "798239EB069F41A9BA8EE040C63DDBBC", "3842B167CA6F44239C3357A721E3BA6A", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, (FieldProvider) vars.getSessionObject("button017312F51139438A9665775E3B5392A1.originalParams"), comboTableData, windowId, "");
    xmlDocument.setData("reportaction", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      
      out.println(xmlDocument.print());
      out.close();
    }

}
