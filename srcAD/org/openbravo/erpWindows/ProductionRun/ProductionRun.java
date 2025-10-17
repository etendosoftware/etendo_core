
package org.openbravo.erpWindows.ProductionRun;




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
public class ProductionRun extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  
  private static final String windowId = "FF808181323E504701323E57E08D0017";
  private static final String tabId = "C9B5394DBA8C465C9CE26A361696B06E";
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
     
      if (command.contains("FF80818132A4F6AD0132A573DD7A0021")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("FF80818132A4F6AD0132A573DD7A0021");
        SessionInfo.setModuleId("0");
      }
     
      if (command.contains("FF808181326CD80501326CE906D70042")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("FF808181326CD80501326CE906D70042");
        SessionInfo.setModuleId("0");
      }
     
      try {
        securedProcess = "Y".equals(org.openbravo.erpCommon.businessUtility.Preferences
            .getPreferenceValue("SecuredProcess", true, vars.getClient(), vars.getOrg(), vars
                .getUser(), vars.getRole(), windowId));
      } catch (PropertyException e) {
      }
     

     
      if (explicitAccess.contains("FF80818132A4F6AD0132A573DD7A0021") || (securedProcess && command.contains("FF80818132A4F6AD0132A573DD7A0021"))) {
        classInfo.type = "P";
        classInfo.id = "FF80818132A4F6AD0132A573DD7A0021";
      }
     
      if (explicitAccess.contains("FF808181326CD80501326CE906D70042") || (securedProcess && command.contains("FF808181326CD80501326CE906D70042"))) {
        classInfo.type = "P";
        classInfo.id = "FF808181326CD80501326CE906D70042";
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

    } else if (vars.commandIn("BUTTONUsedmaterialFF80818132A4F6AD0132A573DD7A0021")) {
        vars.setSessionValue("buttonFF80818132A4F6AD0132A573DD7A0021.strusedmaterial", vars.getStringParameter("inpusedmaterial"));
        vars.setSessionValue("buttonFF80818132A4F6AD0132A573DD7A0021.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("buttonFF80818132A4F6AD0132A573DD7A0021.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("buttonFF80818132A4F6AD0132A573DD7A0021.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("buttonFF80818132A4F6AD0132A573DD7A0021.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "FF80818132A4F6AD0132A573DD7A0021", request.getServletPath());
      } else if (vars.commandIn("BUTTONFF80818132A4F6AD0132A573DD7A0021")) {
        String strM_ProductionPlan_ID = vars.getGlobalVariable("inpmProductionplanId", windowId + "|M_ProductionPlan_ID", "");
        String strusedmaterial = vars.getSessionValue("buttonFF80818132A4F6AD0132A573DD7A0021.strusedmaterial");
        String strProcessing = vars.getSessionValue("buttonFF80818132A4F6AD0132A573DD7A0021.strProcessing");
        String strOrg = vars.getSessionValue("buttonFF80818132A4F6AD0132A573DD7A0021.strOrg");
        String strClient = vars.getSessionValue("buttonFF80818132A4F6AD0132A573DD7A0021.strClient");

        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonUsedmaterialFF80818132A4F6AD0132A573DD7A0021(response, vars, strM_ProductionPlan_ID, strusedmaterial, strProcessing);
        }
    } else if (vars.commandIn("BUTTONValidatingFF808181326CD80501326CE906D70042")) {
        vars.setSessionValue("buttonFF808181326CD80501326CE906D70042.strvalidating", vars.getStringParameter("inpvalidating"));
        vars.setSessionValue("buttonFF808181326CD80501326CE906D70042.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("buttonFF808181326CD80501326CE906D70042.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("buttonFF808181326CD80501326CE906D70042.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("buttonFF808181326CD80501326CE906D70042.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "FF808181326CD80501326CE906D70042", request.getServletPath());
      } else if (vars.commandIn("BUTTONFF808181326CD80501326CE906D70042")) {
        String strM_ProductionPlan_ID = vars.getGlobalVariable("inpmProductionplanId", windowId + "|M_ProductionPlan_ID", "");
        String strvalidating = vars.getSessionValue("buttonFF808181326CD80501326CE906D70042.strvalidating");
        String strProcessing = vars.getSessionValue("buttonFF808181326CD80501326CE906D70042.strProcessing");
        String strOrg = vars.getSessionValue("buttonFF808181326CD80501326CE906D70042.strOrg");
        String strClient = vars.getSessionValue("buttonFF808181326CD80501326CE906D70042.strClient");

        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonValidatingFF808181326CD80501326CE906D70042(response, vars, strM_ProductionPlan_ID, strvalidating, strProcessing);
        }


    } else if (vars.commandIn("SAVE_BUTTONUsedmaterialFF80818132A4F6AD0132A573DD7A0021")) {
        String strM_ProductionPlan_ID = vars.getGlobalVariable("inpKey", windowId + "|M_ProductionPlan_ID", "");
        
        ProcessBundle pb = new ProcessBundle("FF80818132A4F6AD0132A573DD7A0021", vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        params.put("M_ProductionPlan_ID", strM_ProductionPlan_ID);
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
    } else if (vars.commandIn("SAVE_BUTTONValidatingFF808181326CD80501326CE906D70042")) {
        String strM_ProductionPlan_ID = vars.getGlobalVariable("inpKey", windowId + "|M_ProductionPlan_ID", "");
        
        ProcessBundle pb = new ProcessBundle("FF808181326CD80501326CE906D70042", vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        params.put("M_ProductionPlan_ID", strM_ProductionPlan_ID);
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



    void printPageButtonUsedmaterialFF80818132A4F6AD0132A573DD7A0021(HttpServletResponse response, VariablesSecureApp vars, String strM_ProductionPlan_ID, String strusedmaterial, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process FF80818132A4F6AD0132A573DD7A0021");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/UsedmaterialFF80818132A4F6AD0132A573DD7A0021", discard).createXmlDocument();
      xmlDocument.setParameter("key", strM_ProductionPlan_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "ProductionRun_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "FF80818132A4F6AD0132A573DD7A0021");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("FF80818132A4F6AD0132A573DD7A0021");
        vars.removeMessage("FF80818132A4F6AD0132A573DD7A0021");
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
    void printPageButtonValidatingFF808181326CD80501326CE906D70042(HttpServletResponse response, VariablesSecureApp vars, String strM_ProductionPlan_ID, String strvalidating, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process FF808181326CD80501326CE906D70042");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ValidatingFF808181326CD80501326CE906D70042", discard).createXmlDocument();
      xmlDocument.setParameter("key", strM_ProductionPlan_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "ProductionRun_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "FF808181326CD80501326CE906D70042");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("FF808181326CD80501326CE906D70042");
        vars.removeMessage("FF808181326CD80501326CE906D70042");
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
