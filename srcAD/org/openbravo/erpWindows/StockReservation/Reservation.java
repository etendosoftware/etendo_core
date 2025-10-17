
package org.openbravo.erpWindows.StockReservation;


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
public class Reservation extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  
  private static final String windowId = "C85110C9334B4C26BA29BB3B91000689";
  private static final String tabId = "D53F675ADB2745059623175D8870A721";
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
     
      if (command.contains("5A2A0AF88AF54BB085DCC52FCC9B17B7")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("5A2A0AF88AF54BB085DCC52FCC9B17B7");
        SessionInfo.setModuleId("0");
        if (securedProcess || explicitAccess.contains("5A2A0AF88AF54BB085DCC52FCC9B17B7")) {
          classInfo.type = "P";
          classInfo.id = "5A2A0AF88AF54BB085DCC52FCC9B17B7";
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

     } else if (vars.commandIn("BUTTONRES_Process5A2A0AF88AF54BB085DCC52FCC9B17B7")) {
        vars.setSessionValue("button5A2A0AF88AF54BB085DCC52FCC9B17B7.strresProcess", vars.getStringParameter("inpresProcess"));
        vars.setSessionValue("button5A2A0AF88AF54BB085DCC52FCC9B17B7.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button5A2A0AF88AF54BB085DCC52FCC9B17B7.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button5A2A0AF88AF54BB085DCC52FCC9B17B7.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        p.put("res_status", vars.getStringParameter("inpresStatus"));

        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button5A2A0AF88AF54BB085DCC52FCC9B17B7.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "5A2A0AF88AF54BB085DCC52FCC9B17B7", request.getServletPath());    
     } else if (vars.commandIn("BUTTON5A2A0AF88AF54BB085DCC52FCC9B17B7")) {
        String strM_Reservation_ID = vars.getGlobalVariable("inpmReservationId", windowId + "|M_Reservation_ID", "");
        String strresProcess = vars.getSessionValue("button5A2A0AF88AF54BB085DCC52FCC9B17B7.strresProcess");
        String strProcessing = vars.getSessionValue("button5A2A0AF88AF54BB085DCC52FCC9B17B7.strProcessing");
        String strOrg = vars.getSessionValue("button5A2A0AF88AF54BB085DCC52FCC9B17B7.strOrg");
        String strClient = vars.getSessionValue("button5A2A0AF88AF54BB085DCC52FCC9B17B7.strClient");
        
        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonRES_Process5A2A0AF88AF54BB085DCC52FCC9B17B7(response, vars, strM_Reservation_ID, strresProcess, strProcessing);
        }


    } else if (vars.commandIn("SAVE_BUTTONRES_Process5A2A0AF88AF54BB085DCC52FCC9B17B7")) {
        String strM_Reservation_ID = vars.getGlobalVariable("inpKey", windowId + "|M_Reservation_ID", "");
        String strresProcess = vars.getStringParameter("inpresProcess");
        String strProcessing = vars.getStringParameter("inpprocessing");
        OBError myMessage = null;
        try {
          String pinstance = SequenceIdData.getUUID();
          PInstanceProcessData.insertPInstance(this, pinstance, "5A2A0AF88AF54BB085DCC52FCC9B17B7", (("M_Reservation_ID".equalsIgnoreCase("AD_Language"))?"0":strM_Reservation_ID), strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
          String strresAction = vars.getStringParameter("inpresAction");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "10", "RES_Action", strresAction, vars.getClient(), vars.getOrg(), vars.getUser());

          
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

    private void printPageButtonRES_Process5A2A0AF88AF54BB085DCC52FCC9B17B7(HttpServletResponse response, VariablesSecureApp vars, String strM_Reservation_ID, String strresProcess, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 5A2A0AF88AF54BB085DCC52FCC9B17B7");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/RES_Process5A2A0AF88AF54BB085DCC52FCC9B17B7", discard).createXmlDocument();
      xmlDocument.setParameter("key", strM_Reservation_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Reservation_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "5A2A0AF88AF54BB085DCC52FCC9B17B7");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("5A2A0AF88AF54BB085DCC52FCC9B17B7");
        vars.removeMessage("5A2A0AF88AF54BB085DCC52FCC9B17B7");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("RES_Action", "");
    comboTableData = new ComboTableData(vars, this, "17", "RES_Action", "440DDA64A43F4799AAFF48BC86DC8F78", "1645143617E44289A08A1EA4D617A184", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, (FieldProvider) vars.getSessionObject("button5A2A0AF88AF54BB085DCC52FCC9B17B7.originalParams"), comboTableData, windowId, "");
    xmlDocument.setData("reportRES_Action", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      
      out.println(xmlDocument.print());
      out.close();
    }



}
