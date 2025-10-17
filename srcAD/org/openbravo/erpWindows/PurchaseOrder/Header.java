
package org.openbravo.erpWindows.PurchaseOrder;


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
public class Header extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  
  private static final String windowId = "181";
  private static final String tabId = "294";
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
     
      if (command.contains("23D1B163EC0B41F790CE39BF01DA320E")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("23D1B163EC0B41F790CE39BF01DA320E");
        SessionInfo.setModuleId("0");
      }
     
      if (command.contains("A3FE1F9892394386A49FB707AA50A0FA")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("A3FE1F9892394386A49FB707AA50A0FA");
        SessionInfo.setModuleId("0");
      }
     
      if (command.contains("9EB2228A60684C0DBEC12D5CD8D85218")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("9EB2228A60684C0DBEC12D5CD8D85218");
        SessionInfo.setModuleId("0");
      }
     
      try {
        securedProcess = "Y".equals(org.openbravo.erpCommon.businessUtility.Preferences
            .getPreferenceValue("SecuredProcess", true, vars.getClient(), vars.getOrg(), vars
                .getUser(), vars.getRole(), windowId));
      } catch (PropertyException e) {
      }
     
      if (command.contains("104")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("104");
        SessionInfo.setModuleId("0");
        if (securedProcess || explicitAccess.contains("104")) {
          classInfo.type = "P";
          classInfo.id = "104";
        }
      }
     

     
      if (explicitAccess.contains("23D1B163EC0B41F790CE39BF01DA320E") || (securedProcess && command.contains("23D1B163EC0B41F790CE39BF01DA320E"))) {
        classInfo.type = "P";
        classInfo.id = "23D1B163EC0B41F790CE39BF01DA320E";
      }
     
      if (explicitAccess.contains("A3FE1F9892394386A49FB707AA50A0FA") || (securedProcess && command.contains("A3FE1F9892394386A49FB707AA50A0FA"))) {
        classInfo.type = "P";
        classInfo.id = "A3FE1F9892394386A49FB707AA50A0FA";
      }
     
      if (explicitAccess.contains("9EB2228A60684C0DBEC12D5CD8D85218") || (securedProcess && command.contains("9EB2228A60684C0DBEC12D5CD8D85218"))) {
        classInfo.type = "P";
        classInfo.id = "9EB2228A60684C0DBEC12D5CD8D85218";
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

     } else if (vars.commandIn("BUTTONDocAction104")) {
        vars.setSessionValue("button104.strdocaction", vars.getStringParameter("inpdocaction"));
        vars.setSessionValue("button104.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button104.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button104.strClient", vars.getStringParameter("inpadClientId"));
        vars.setSessionValue("button104.inpdocstatus", vars.getRequiredStringParameter("inpdocstatus"));

        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button104.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "104", request.getServletPath());    
     } else if (vars.commandIn("BUTTON104")) {
        String strC_Order_ID = vars.getGlobalVariable("inpcOrderId", windowId + "|C_Order_ID", "");
        String strdocaction = vars.getSessionValue("button104.strdocaction");
        String strProcessing = vars.getSessionValue("button104.strProcessing");
        String strOrg = vars.getSessionValue("button104.strOrg");
        String strClient = vars.getSessionValue("button104.strClient");
        
        String strdocstatus = vars.getSessionValue("button104.inpdocstatus");
String stradTableId = "259";

        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonDocAction104(response, vars, strC_Order_ID, strdocaction, strProcessing, strdocstatus, stradTableId);
        }

    } else if (vars.commandIn("BUTTONRM_AddOrphanLine23D1B163EC0B41F790CE39BF01DA320E")) {
        vars.setSessionValue("button23D1B163EC0B41F790CE39BF01DA320E.strrmAddorphanline", vars.getStringParameter("inprmAddorphanline"));
        vars.setSessionValue("button23D1B163EC0B41F790CE39BF01DA320E.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button23D1B163EC0B41F790CE39BF01DA320E.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button23D1B163EC0B41F790CE39BF01DA320E.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        p.put("IsSOTrx", vars.getStringParameter("inpissotrx"));

        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button23D1B163EC0B41F790CE39BF01DA320E.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "23D1B163EC0B41F790CE39BF01DA320E", request.getServletPath());
      } else if (vars.commandIn("BUTTON23D1B163EC0B41F790CE39BF01DA320E")) {
        String strC_Order_ID = vars.getGlobalVariable("inpcOrderId", windowId + "|C_Order_ID", "");
        String strrmAddorphanline = vars.getSessionValue("button23D1B163EC0B41F790CE39BF01DA320E.strrmAddorphanline");
        String strProcessing = vars.getSessionValue("button23D1B163EC0B41F790CE39BF01DA320E.strProcessing");
        String strOrg = vars.getSessionValue("button23D1B163EC0B41F790CE39BF01DA320E.strOrg");
        String strClient = vars.getSessionValue("button23D1B163EC0B41F790CE39BF01DA320E.strClient");

        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonRM_AddOrphanLine23D1B163EC0B41F790CE39BF01DA320E(response, vars, strC_Order_ID, strrmAddorphanline, strProcessing);
        }
    } else if (vars.commandIn("BUTTONConvertquotationA3FE1F9892394386A49FB707AA50A0FA")) {
        vars.setSessionValue("buttonA3FE1F9892394386A49FB707AA50A0FA.strconvertquotation", vars.getStringParameter("inpconvertquotation"));
        vars.setSessionValue("buttonA3FE1F9892394386A49FB707AA50A0FA.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("buttonA3FE1F9892394386A49FB707AA50A0FA.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("buttonA3FE1F9892394386A49FB707AA50A0FA.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("buttonA3FE1F9892394386A49FB707AA50A0FA.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "A3FE1F9892394386A49FB707AA50A0FA", request.getServletPath());
      } else if (vars.commandIn("BUTTONA3FE1F9892394386A49FB707AA50A0FA")) {
        String strC_Order_ID = vars.getGlobalVariable("inpcOrderId", windowId + "|C_Order_ID", "");
        String strconvertquotation = vars.getSessionValue("buttonA3FE1F9892394386A49FB707AA50A0FA.strconvertquotation");
        String strProcessing = vars.getSessionValue("buttonA3FE1F9892394386A49FB707AA50A0FA.strProcessing");
        String strOrg = vars.getSessionValue("buttonA3FE1F9892394386A49FB707AA50A0FA.strOrg");
        String strClient = vars.getSessionValue("buttonA3FE1F9892394386A49FB707AA50A0FA.strClient");

        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonConvertquotationA3FE1F9892394386A49FB707AA50A0FA(response, vars, strC_Order_ID, strconvertquotation, strProcessing);
        }
    } else if (vars.commandIn("BUTTONCalculate_Promotions9EB2228A60684C0DBEC12D5CD8D85218")) {
        vars.setSessionValue("button9EB2228A60684C0DBEC12D5CD8D85218.strcalculatePromotions", vars.getStringParameter("inpcalculatePromotions"));
        vars.setSessionValue("button9EB2228A60684C0DBEC12D5CD8D85218.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button9EB2228A60684C0DBEC12D5CD8D85218.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button9EB2228A60684C0DBEC12D5CD8D85218.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button9EB2228A60684C0DBEC12D5CD8D85218.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "9EB2228A60684C0DBEC12D5CD8D85218", request.getServletPath());
      } else if (vars.commandIn("BUTTON9EB2228A60684C0DBEC12D5CD8D85218")) {
        String strC_Order_ID = vars.getGlobalVariable("inpcOrderId", windowId + "|C_Order_ID", "");
        String strcalculatePromotions = vars.getSessionValue("button9EB2228A60684C0DBEC12D5CD8D85218.strcalculatePromotions");
        String strProcessing = vars.getSessionValue("button9EB2228A60684C0DBEC12D5CD8D85218.strProcessing");
        String strOrg = vars.getSessionValue("button9EB2228A60684C0DBEC12D5CD8D85218.strOrg");
        String strClient = vars.getSessionValue("button9EB2228A60684C0DBEC12D5CD8D85218.strClient");

        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonCalculate_Promotions9EB2228A60684C0DBEC12D5CD8D85218(response, vars, strC_Order_ID, strcalculatePromotions, strProcessing);
        }

    } else if (vars.commandIn("SAVE_BUTTONDocAction104")) {
        String strC_Order_ID = vars.getGlobalVariable("inpKey", windowId + "|C_Order_ID", "");
        String strdocaction = vars.getStringParameter("inpdocaction");
        String strProcessing = vars.getStringParameter("inpprocessing");
        OBError myMessage = null;
        try {
          String pinstance = SequenceIdData.getUUID();
          PInstanceProcessData.insertPInstance(this, pinstance, "104", (("C_Order_ID".equalsIgnoreCase("AD_Language"))?"0":strC_Order_ID), strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
          
          HeaderData.updateDocAction(this, strdocaction, strC_Order_ID);

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

    } else if (vars.commandIn("SAVE_BUTTONRM_AddOrphanLine23D1B163EC0B41F790CE39BF01DA320E")) {
        String strC_Order_ID = vars.getGlobalVariable("inpKey", windowId + "|C_Order_ID", "");
        
        ProcessBundle pb = new ProcessBundle("23D1B163EC0B41F790CE39BF01DA320E", vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        params.put("C_Order_ID", strC_Order_ID);
        params.put("adOrgId", vars.getStringParameter("inpadOrgId"));
        params.put("adClientId", vars.getStringParameter("inpadClientId"));
        params.put("tabId", tabId);
        
        String strmProductId = vars.getStringParameter("inpmProductId");
params.put("mProductId", strmProductId);
String strmAttributesetinstanceId = vars.getStringParameter("inpmAttributesetinstanceId");
params.put("mAttributesetinstanceId", strmAttributesetinstanceId);
String strreturned = vars.getNumericParameter("inpreturned");
params.put("returned", strreturned);
String strpricestd = vars.getNumericParameter("inppricestd");
params.put("pricestd", strpricestd);
String strcTaxId = vars.getStringParameter("inpcTaxId");
params.put("cTaxId", strcTaxId);
String strcReturnReasonId = vars.getStringParameter("inpcReturnReasonId");
params.put("cReturnReasonId", strcReturnReasonId);

        
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
    } else if (vars.commandIn("SAVE_BUTTONConvertquotationA3FE1F9892394386A49FB707AA50A0FA")) {
        String strC_Order_ID = vars.getGlobalVariable("inpKey", windowId + "|C_Order_ID", "");
        
        ProcessBundle pb = new ProcessBundle("A3FE1F9892394386A49FB707AA50A0FA", vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        params.put("C_Order_ID", strC_Order_ID);
        params.put("adOrgId", vars.getStringParameter("inpadOrgId"));
        params.put("adClientId", vars.getStringParameter("inpadClientId"));
        params.put("tabId", tabId);
        
        String strrecalculateprices = vars.getStringParameter("inprecalculateprices", "N");
params.put("recalculateprices", strrecalculateprices);

        
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
    } else if (vars.commandIn("SAVE_BUTTONCalculate_Promotions9EB2228A60684C0DBEC12D5CD8D85218")) {
        String strC_Order_ID = vars.getGlobalVariable("inpKey", windowId + "|C_Order_ID", "");
        
        ProcessBundle pb = new ProcessBundle("9EB2228A60684C0DBEC12D5CD8D85218", vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        params.put("C_Order_ID", strC_Order_ID);
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

    private void printPageButtonDocAction104(HttpServletResponse response, VariablesSecureApp vars, String strC_Order_ID, String strdocaction, String strProcessing, String strdocstatus, String stradTableId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 104");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/DocAction", discard).createXmlDocument();
      xmlDocument.setParameter("key", strC_Order_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "104");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("104");
        vars.removeMessage("104");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

      xmlDocument.setParameter("docstatus", strdocstatus);
xmlDocument.setParameter("adTableId", stradTableId);
    try {
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
xmlDocument.setParameter("processId", "104");
xmlDocument.setParameter("processDescription", "Process Order");
xmlDocument.setParameter("docaction", (strdocaction.equals("--")?"CL":strdocaction));
FieldProvider[] dataDocAction = ActionButtonUtility.docAction(this, vars, strdocaction, "FF80818130217A35013021A672400035", strdocstatus, strProcessing, stradTableId, tabId);
xmlDocument.setData("reportdocaction", "liststructure", dataDocAction);
StringBuffer dact = new StringBuffer();
if (dataDocAction!=null) {
  dact.append("var arrDocAction = new Array(\n");
  for (int i=0;i<dataDocAction.length;i++) {
    dact.append("new Array(\"" + dataDocAction[i].getField("id") + "\", \"" + dataDocAction[i].getField("name") + "\", \"" + dataDocAction[i].getField("description") + "\")\n");
    if (i<dataDocAction.length-1) dact.append(",\n");
  }
  dact.append(");");
} else dact.append("var arrDocAction = null");
xmlDocument.setParameter("array", dact.toString());

      
      out.println(xmlDocument.print());
      out.close();
    }


    void printPageButtonRM_AddOrphanLine23D1B163EC0B41F790CE39BF01DA320E(HttpServletResponse response, VariablesSecureApp vars, String strC_Order_ID, String strrmAddorphanline, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 23D1B163EC0B41F790CE39BF01DA320E");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/RM_AddOrphanLine23D1B163EC0B41F790CE39BF01DA320E", discard).createXmlDocument();
      xmlDocument.setParameter("key", strC_Order_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "23D1B163EC0B41F790CE39BF01DA320E");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("23D1B163EC0B41F790CE39BF01DA320E");
        vars.removeMessage("23D1B163EC0B41F790CE39BF01DA320E");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("M_Product_ID", "");
    xmlDocument.setParameter("M_AttributeSetInstance_ID", "");
    xmlDocument.setParameter("M_AttributeSetInstance_IDR", HeaderData.selectActDefM_AttributeSetInstance_ID(this, ""));
    xmlDocument.setParameter("Returned", "");
    xmlDocument.setParameter("PriceStd", "");
    xmlDocument.setParameter("C_Tax_ID", "");
    comboTableData = new ComboTableData(vars, this, "19", "C_Tax_ID", "", "299FA667CF374AC5ACC74739C3251134", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, (FieldProvider) vars.getSessionObject("button23D1B163EC0B41F790CE39BF01DA320E.originalParams"), comboTableData, windowId, "");
    xmlDocument.setData("reportC_Tax_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("C_Return_Reason_ID", "");
    comboTableData = new ComboTableData(vars, this, "19", "C_Return_Reason_ID", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, (FieldProvider) vars.getSessionObject("button23D1B163EC0B41F790CE39BF01DA320E.originalParams"), comboTableData, windowId, "");
    xmlDocument.setData("reportC_Return_Reason_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      
      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButtonConvertquotationA3FE1F9892394386A49FB707AA50A0FA(HttpServletResponse response, VariablesSecureApp vars, String strC_Order_ID, String strconvertquotation, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process A3FE1F9892394386A49FB707AA50A0FA");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ConvertquotationA3FE1F9892394386A49FB707AA50A0FA", discard).createXmlDocument();
      xmlDocument.setParameter("key", strC_Order_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "A3FE1F9892394386A49FB707AA50A0FA");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("A3FE1F9892394386A49FB707AA50A0FA");
        vars.removeMessage("A3FE1F9892394386A49FB707AA50A0FA");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    xmlDocument.setParameter("RecalculatePrices", "Y");
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      
      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButtonCalculate_Promotions9EB2228A60684C0DBEC12D5CD8D85218(HttpServletResponse response, VariablesSecureApp vars, String strC_Order_ID, String strcalculatePromotions, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 9EB2228A60684C0DBEC12D5CD8D85218");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/Calculate_Promotions9EB2228A60684C0DBEC12D5CD8D85218", discard).createXmlDocument();
      xmlDocument.setParameter("key", strC_Order_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "9EB2228A60684C0DBEC12D5CD8D85218");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("9EB2228A60684C0DBEC12D5CD8D85218");
        vars.removeMessage("9EB2228A60684C0DBEC12D5CD8D85218");
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
