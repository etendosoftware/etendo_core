
package org.openbravo.erpWindows.PhysicalInventory;


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
  
  private static final String windowId = "168";
  private static final String tabId = "255";
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
     
      if (command.contains("107")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("107");
        SessionInfo.setModuleId("0");
      }
     
      try {
        securedProcess = "Y".equals(org.openbravo.erpCommon.businessUtility.Preferences
            .getPreferenceValue("SecuredProcess", true, vars.getClient(), vars.getOrg(), vars
                .getUser(), vars.getRole(), windowId));
      } catch (PropertyException e) {
      }
     
      if (command.contains("105")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("105");
        SessionInfo.setModuleId("0");
        if (securedProcess || explicitAccess.contains("105")) {
          classInfo.type = "P";
          classInfo.id = "105";
        }
      }
     
      if (command.contains("106")) {
        SessionInfo.setProcessType("P");
        SessionInfo.setProcessId("106");
        SessionInfo.setModuleId("0");
        if (securedProcess || explicitAccess.contains("106")) {
          classInfo.type = "P";
          classInfo.id = "106";
        }
      }
     

     
      if (explicitAccess.contains("107") || (securedProcess && command.contains("107"))) {
        classInfo.type = "P";
        classInfo.id = "107";
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

     } else if (vars.commandIn("BUTTONGenerateList105")) {
        vars.setSessionValue("button105.strgeneratelist", vars.getStringParameter("inpgeneratelist"));
        vars.setSessionValue("button105.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button105.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button105.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button105.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "105", request.getServletPath());    
     } else if (vars.commandIn("BUTTON105")) {
        String strM_Inventory_ID = vars.getGlobalVariable("inpmInventoryId", windowId + "|M_Inventory_ID", "");
        String strgeneratelist = vars.getSessionValue("button105.strgeneratelist");
        String strProcessing = vars.getSessionValue("button105.strProcessing");
        String strOrg = vars.getSessionValue("button105.strOrg");
        String strClient = vars.getSessionValue("button105.strClient");
        
        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonGenerateList105(response, vars, strM_Inventory_ID, strgeneratelist, strProcessing);
        }

     } else if (vars.commandIn("BUTTONUpdateQty106")) {
        vars.setSessionValue("button106.strupdateqty", vars.getStringParameter("inpupdateqty"));
        vars.setSessionValue("button106.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button106.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button106.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button106.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "106", request.getServletPath());    
     } else if (vars.commandIn("BUTTON106")) {
        String strM_Inventory_ID = vars.getGlobalVariable("inpmInventoryId", windowId + "|M_Inventory_ID", "");
        String strupdateqty = vars.getSessionValue("button106.strupdateqty");
        String strProcessing = vars.getSessionValue("button106.strProcessing");
        String strOrg = vars.getSessionValue("button106.strOrg");
        String strClient = vars.getSessionValue("button106.strClient");
        
        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonUpdateQty106(response, vars, strM_Inventory_ID, strupdateqty, strProcessing);
        }

    } else if (vars.commandIn("BUTTONProcessing107")) {
        vars.setSessionValue("button107.strprocessing", vars.getStringParameter("inpprocessing"));
        vars.setSessionValue("button107.strProcessing", vars.getStringParameter("inpprocessing", "Y"));
        vars.setSessionValue("button107.strOrg", vars.getStringParameter("inpadOrgId"));
        vars.setSessionValue("button107.strClient", vars.getStringParameter("inpadClientId"));
        
        
        HashMap<String, String> p = new HashMap<String, String>();
        
        
        //Save in session needed params for combos if needed
        vars.setSessionObject("button107.originalParams", FieldProviderFactory.getFieldProvider(p));
        printPageButtonFS(response, vars, "107", request.getServletPath());
      } else if (vars.commandIn("BUTTON107")) {
        String strM_Inventory_ID = vars.getGlobalVariable("inpmInventoryId", windowId + "|M_Inventory_ID", "");
        String strprocessing = vars.getSessionValue("button107.strprocessing");
        String strProcessing = vars.getSessionValue("button107.strProcessing");
        String strOrg = vars.getSessionValue("button107.strOrg");
        String strClient = vars.getSessionValue("button107.strClient");

        
        if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(), tabId)) || !(Utility.isElementInList(Utility.getContext(this, vars, "#User_Client", windowId, accesslevel),strClient)  && Utility.isElementInList(Utility.getContext(this, vars, "#User_Org", windowId, accesslevel),strOrg))){
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
          vars.setMessage(tabId, myError);
          printPageClosePopUp(response, vars);
        }else{       
          printPageButtonProcessing107(response, vars, strM_Inventory_ID, strprocessing, strProcessing);
        }

    } else if (vars.commandIn("SAVE_BUTTONGenerateList105")) {
        String strM_Inventory_ID = vars.getGlobalVariable("inpKey", windowId + "|M_Inventory_ID", "");
        String strgeneratelist = vars.getStringParameter("inpgeneratelist");
        String strProcessing = vars.getStringParameter("inpprocessing");
        OBError myMessage = null;
        try {
          String pinstance = SequenceIdData.getUUID();
          PInstanceProcessData.insertPInstance(this, pinstance, "105", (("M_Inventory_ID".equalsIgnoreCase("AD_Language"))?"0":strM_Inventory_ID), strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
          String strmLocatorId = vars.getStringParameter("inpmLocatorId");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "5", "M_Locator_ID", strmLocatorId, vars.getClient(), vars.getOrg(), vars.getUser());
String strproductvalue = vars.getStringParameter("inpproductvalue");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "10", "ProductValue", strproductvalue, vars.getClient(), vars.getOrg(), vars.getUser());
String strmProductCategoryId = vars.getStringParameter("inpmProductCategoryId");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "15", "M_Product_Category_ID", strmProductCategoryId, vars.getClient(), vars.getOrg(), vars.getUser());
String strqtyrange = vars.getStringParameter("inpqtyrange");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "20", "QtyRange", strqtyrange, vars.getClient(), vars.getOrg(), vars.getUser());
String strregularization = vars.getStringParameter("inpregularization", "N");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "30", "regularization", strregularization, vars.getClient(), vars.getOrg(), vars.getUser());
String strabc = vars.getStringParameter("inpabc");
PInstanceProcessData.insertPInstanceParam(this, pinstance, "40", "ABC", strabc, vars.getClient(), vars.getOrg(), vars.getUser());

          
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
    } else if (vars.commandIn("SAVE_BUTTONUpdateQty106")) {
        String strM_Inventory_ID = vars.getGlobalVariable("inpKey", windowId + "|M_Inventory_ID", "");
        String strupdateqty = vars.getStringParameter("inpupdateqty");
        String strProcessing = vars.getStringParameter("inpprocessing");
        OBError myMessage = null;
        try {
          String pinstance = SequenceIdData.getUUID();
          PInstanceProcessData.insertPInstance(this, pinstance, "106", (("M_Inventory_ID".equalsIgnoreCase("AD_Language"))?"0":strM_Inventory_ID), strProcessing, vars.getUser(), vars.getClient(), vars.getOrg());
          
          
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

    } else if (vars.commandIn("SAVE_BUTTONProcessing107")) {
        String strM_Inventory_ID = vars.getGlobalVariable("inpKey", windowId + "|M_Inventory_ID", "");
        
        ProcessBundle pb = new ProcessBundle("107", vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        params.put("M_Inventory_ID", strM_Inventory_ID);
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


    } else if (vars.commandIn("BUTTONPosted")) {
        String strM_Inventory_ID = vars.getGlobalVariable("inpmInventoryId", windowId + "|M_Inventory_ID", "");
        String strTableId = "321";
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
          vars.setSessionValue("Posted|key", strM_Inventory_ID);
          vars.setSessionValue("Posted|tableId", strTableId);
          vars.setSessionValue("Posted|tabId", tabId);
          vars.setSessionValue("Posted|posted", strPosted);
          vars.setSessionValue("Posted|processId", strProcessId);
          vars.setSessionValue("Posted|path", strDireccion + request.getServletPath());
          vars.setSessionValue("Posted|windowId", windowId);
          vars.setSessionValue("Posted|tabName", "Header");
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

    private void printPageButtonGenerateList105(HttpServletResponse response, VariablesSecureApp vars, String strM_Inventory_ID, String strgeneratelist, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 105");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/GenerateList105", discard).createXmlDocument();
      xmlDocument.setParameter("key", strM_Inventory_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "105");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("105");
        vars.removeMessage("105");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("M_Locator_ID", "");
    xmlDocument.setParameter("M_Locator_IDR", HeaderData.selectActDefM_Locator_ID(this, ""));
    xmlDocument.setParameter("ProductValue", "%");
    xmlDocument.setParameter("M_Product_Category_ID", "0");
    comboTableData = new ComboTableData(vars, this, "19", "M_Product_Category_ID", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, (FieldProvider) vars.getSessionObject("button105.originalParams"), comboTableData, windowId, "0");
    xmlDocument.setData("reportM_Product_Category_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("QtyRange", "N");
    comboTableData = new ComboTableData(vars, this, "17", "QtyRange", "212", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, (FieldProvider) vars.getSessionObject("button105.originalParams"), comboTableData, windowId, "N");
    xmlDocument.setData("reportQtyRange", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("regularization", "N");
    xmlDocument.setParameter("ABC", "");
    comboTableData = new ComboTableData(vars, this, "17", "ABC", "1000500000", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, (FieldProvider) vars.getSessionObject("button105.originalParams"), comboTableData, windowId, "");
    xmlDocument.setData("reportABC", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      
      out.println(xmlDocument.print());
      out.close();
    }
    private void printPageButtonUpdateQty106(HttpServletResponse response, VariablesSecureApp vars, String strM_Inventory_ID, String strupdateqty, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 106");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/UpdateQty106", discard).createXmlDocument();
      xmlDocument.setParameter("key", strM_Inventory_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "106");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("106");
        vars.removeMessage("106");
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


    void printPageButtonProcessing107(HttpServletResponse response, VariablesSecureApp vars, String strM_Inventory_ID, String strprocessing, String strProcessing)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 107");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/Processing107", discard).createXmlDocument();
      xmlDocument.setParameter("key", strM_Inventory_ID);
      xmlDocument.setParameter("processing", strProcessing);
      xmlDocument.setParameter("form", "Header_Edition.html");
      xmlDocument.setParameter("window", windowId);
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("processId", "107");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      
      {
        OBError myMessage = vars.getMessage("107");
        vars.removeMessage("107");
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
