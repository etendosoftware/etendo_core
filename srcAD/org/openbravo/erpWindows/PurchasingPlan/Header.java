
package org.openbravo.erpWindows.PurchasingPlan;


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
  
  private static final String windowId = "800097";
  private static final String tabId = "800258";
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
     
      if (command.contains("7CB6B4D1ECCF4036B3F111D2CF11AADE")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("7CB6B4D1ECCF4036B3F111D2CF11AADE");
        SessionInfo.setModuleId("0");
      }
     
      try {
        securedProcess = "Y".equals(org.openbravo.erpCommon.businessUtility.Preferences
            .getPreferenceValue("SecuredProcess", true, vars.getClient(), vars.getOrg(), vars
                .getUser(), vars.getRole(), windowId));
      } catch (PropertyException e) {
      }
     
      if (command.contains("800164")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("800164");
        SessionInfo.setModuleId("0");
        if (securedProcess || explicitAccess.contains("800164")) {
          classInfo.type = "P";
          classInfo.id = "800164";
        }
      }
     
      if (command.contains("800163")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("800163");
        SessionInfo.setModuleId("0");
        if (securedProcess || explicitAccess.contains("800163")) {
          classInfo.type = "P";
          classInfo.id = "800163";
        }
      }
     

     
      if (explicitAccess.contains("7CB6B4D1ECCF4036B3F111D2CF11AADE") || (securedProcess && command.contains("7CB6B4D1ECCF4036B3F111D2CF11AADE"))) {
        classInfo.type = "P";
        classInfo.id = "7CB6B4D1ECCF4036B3F111D2CF11AADE";
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

     } else if (vars.commandIn("BUTTONSimulate800164")) {
        vars.setSessionValue("button800164.strsimulate", vars.getStringParameter("inpsimulate"));
        vars.setSessionValue("button800164.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button800164.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button800164.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button800164.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "800164", request.getServletPath());    
     } else if (vars.commandIn("BUTTON800164")) {
        String strMRP_Run_Purchase_ID = vars.getGlobalVariable("inpmrpRunPurchaseId", windowId + "|MRP_Run_Purchase_ID", "");
        String strsimulate = vars.getSessionValue("button800164.strsimulate");
        String strProcessing = vars.getSessionValue("button800164.strProcessing");
        String strOrg = vars.getSessionValue("button800164.strOrg");
        String strClient = vars.getSessionValue("button800164.strClient");
        
        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonSimulate800164(response, vars, strMRP_Run_Purchase_ID, strsimulate, strProcessing);
        }

     } else if (vars.commandIn("BUTTONLaunchpo800163")) {
        vars.setSessionValue("button800163.strlaunchpo", vars.getStringParameter("inplaunchpo"));
        vars.setSessionValue("button800163.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button800163.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button800163.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        p.put("AD_ORG_ID", vars.getStringParameter("inpadOrgId"));

        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button800163.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "800163", request.getServletPath());    
     } else if (vars.commandIn("BUTTON800163")) {
        String strMRP_Run_Purchase_ID = vars.getGlobalVariable("inpmrpRunPurchaseId", windowId + "|MRP_Run_Purchase_ID", "");
        String strlaunchpo = vars.getSessionValue("button800163.strlaunchpo");
        String strProcessing = vars.getSessionValue("button800163.strProcessing");
        String strOrg = vars.getSessionValue("button800163.strOrg");
        String strClient = vars.getSessionValue("button800163.strClient");
        
        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonLaunchpo800163(response, vars, strMRP_Run_Purchase_ID, strlaunchpo, strProcessing);
        }

    } else if (vars.commandIn("BUTTONCreate_Reservations7CB6B4D1ECCF4036B3F111D2CF11AADE")) {
        vars.setSessionValue("button7CB6B4D1ECCF4036B3F111D2CF11AADE.strcreateReservations", vars.getStringParameter("inpcreateReservations"));
        vars.setSessionValue("button7CB6B4D1ECCF4036B3F111D2CF11AADE.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button7CB6B4D1ECCF4036B3F111D2CF11AADE.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button7CB6B4D1ECCF4036B3F111D2CF11AADE.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button7CB6B4D1ECCF4036B3F111D2CF11AADE.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "7CB6B4D1ECCF4036B3F111D2CF11AADE", request.getServletPath());
      } else if (vars.commandIn("BUTTON7CB6B4D1ECCF4036B3F111D2CF11AADE")) {
        String strMRP_Run_Purchase_ID = vars.getGlobalVariable("inpmrpRunPurchaseId", windowId + "|MRP_Run_Purchase_ID", "");
        String strcreateReservations = vars.getSessionValue("button7CB6B4D1ECCF4036B3F111D2CF11AADE.strcreateReservations");
        String strProcessing = vars.getSessionValue("button7CB6B4D1ECCF4036B3F111D2CF11AADE.strProcessing");
        String strOrg = vars.getSessionValue("button7CB6B4D1ECCF4036B3F111D2CF11AADE.strOrg");
        String strClient = vars.getSessionValue("button7CB6B4D1ECCF4036B3F111D2CF11AADE.strClient");

        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonCreate_Reservations7CB6B4D1ECCF4036B3F111D2CF11AADE(response, vars, strMRP_Run_Purchase_ID, strcreateReservations, strProcessing);
        }

    } else if (vars.commandIn("SAVE_BUTTONSimulate800164")) {
        String strMRP_Run_Purchase_ID = vars.getGlobalVariable("inpKey", windowId + "|MRP_Run_Purchase_ID", "");
        String strsimulate = vars.getStringParameter("inpsimulate");
        String strProcessing = vars.getStringParameter("inpprocessing");
        OBError myMessage = null;
        try {
          String pinstance = SequenceIdData.getUUID();
          PInstanceProcessData.insertPInstance(this, pinstance, "800164", (("MRP_Run_Purchase_ID".equalsIgnoreCase("AD_Language"))?"0":strMRP_Run_Purchase_ID), strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
          
          
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
    } else if (vars.commandIn("SAVE_BUTTONLaunchpo800163")) {
        String strMRP_Run_Purchase_ID = vars.getGlobalVariable("inpKey", windowId + "|MRP_Run_Purchase_ID", "");
        String strlaunchpo = vars.getStringParameter("inplaunchpo");
        String strProcessing = vars.getStringParameter("inpprocessing");
        OBError myMessage = null;
        try {
          String pinstance = SequenceIdData.getUUID();
          PInstanceProcessData.insertPInstance(this, pinstance, "800163", (("MRP_Run_Purchase_ID".equalsIgnoreCase("AD_Language"))?"0":strMRP_Run_Purchase_ID), strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
          String strmWarehouseId = vars.getStringParameter("inpmWarehouseId");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "10", "M_Warehouse_ID", strmWarehouseId, vars.getClient(), vars.getOrg(), vars.getUser());

          
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

    } else if (vars.commandIn("SAVE_BUTTONCreate_Reservations7CB6B4D1ECCF4036B3F111D2CF11AADE")) {
        String strMRP_Run_Purchase_ID = vars.getGlobalVariable("inpKey", windowId + "|MRP_Run_Purchase_ID", "");
        
        ProcessBundle pb = new ProcessBundle("7CB6B4D1ECCF4036B3F111D2CF11AADE", vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        params.put("MRP_Run_Purchase_ID", strMRP_Run_Purchase_ID);
        params.put("adOrgId", vars.getStringParameter("inpadOrgId"));
        params.put("adClientId", vars.getStringParameter("inpadClientId"));
        params.put("tabId", tabId);
        
        String strmWarehouseId = vars.getStringParameter("inpmWarehouseId");
params.put("mWarehouseId", strmWarehouseId);

        
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

    private void printPageButtonSimulate800164(HttpServletResponse response, VariablesSecureApp vars, String strMRP_Run_Purchase_ID, String strsimulate, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 800164");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/Simulate800164", discard).createXmlDocument();
      xmlDocument.setParameter("key", strMRP_Run_Purchase_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "800164");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("800164");
        vars.removeMessage("800164");
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
    private void printPageButtonLaunchpo800163(HttpServletResponse response, VariablesSecureApp vars, String strMRP_Run_Purchase_ID, String strlaunchpo, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 800163");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/Launchpo800163", discard).createXmlDocument();
      xmlDocument.setParameter("key", strMRP_Run_Purchase_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "800163");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("800163");
        vars.removeMessage("800163");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("M_Warehouse_ID", Utility.getContext(this, vars, "#M_Warehouse_ID", windowId));
    comboTableData = new ComboTableData(vars, this, "19", "M_Warehouse_ID", "", "71188F0005494DA08311B4FFB2C5A993", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, (FieldProvider) vars.getSessionObject("button800163.originalParams"), comboTableData, windowId, Utility.getContext(this, vars, "#M_Warehouse_ID", windowId));
    xmlDocument.setData("reportM_Warehouse_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      
      out.println(xmlDocument.print());
      out.close();
    }


    void printPageButtonCreate_Reservations7CB6B4D1ECCF4036B3F111D2CF11AADE(HttpServletResponse response, VariablesSecureApp vars, String strMRP_Run_Purchase_ID, String strcreateReservations, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 7CB6B4D1ECCF4036B3F111D2CF11AADE");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/Create_Reservations7CB6B4D1ECCF4036B3F111D2CF11AADE", discard).createXmlDocument();
      xmlDocument.setParameter("key", strMRP_Run_Purchase_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "7CB6B4D1ECCF4036B3F111D2CF11AADE");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("7CB6B4D1ECCF4036B3F111D2CF11AADE");
        vars.removeMessage("7CB6B4D1ECCF4036B3F111D2CF11AADE");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("M_Warehouse_ID", Utility.getContext(this, vars, "#M_Warehouse_ID", windowId));
    comboTableData = new ComboTableData(vars, this, "19", "M_Warehouse_ID", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, (FieldProvider) vars.getSessionObject("button7CB6B4D1ECCF4036B3F111D2CF11AADE.originalParams"), comboTableData, windowId, Utility.getContext(this, vars, "#M_Warehouse_ID", windowId));
    xmlDocument.setData("reportM_Warehouse_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      
      out.println(xmlDocument.print());
      out.close();
    }

}
