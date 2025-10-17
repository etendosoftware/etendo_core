
package org.openbravo.erpCommon.ad_actionButton;


import org.openbravo.erpCommon.utility.*;
import org.openbravo.erpCommon.reference.*;
import org.openbravo.utils.Replace;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessRunner;
import org.openbravo.xmlEngine.XmlDocument;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.obps.ActivationKey;
import org.openbravo.erpCommon.obps.ActivationKey.FeatureRestriction;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Process;
import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.util.HashMap;

@SuppressWarnings("unused")
public class ActionButtonJava_Responser extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  protected static final String windowId = "ActionButtonResponser";
  
  public void init (ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void service(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String strProcessId = getProcessId(vars);

    // set process type and id for audit
    SessionInfo.setProcessType("P");
    SessionInfo.setProcessId(strProcessId);
    SessionInfo.setUserId(vars.getSessionValue("#AD_User_ID"));
    SessionInfo.setSessionId(vars.getSessionValue("#AD_Session_ID"));
    SessionInfo.setQueryProfile("manualProcess");

    try {
      OBContext.setAdminMode();
      Process process = OBDal.getInstance().get(Process.class, strProcessId);
      if (process != null) {
        SessionInfo.setModuleId(process.getModule().getId());
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    super.service(request, response);
  }

  private String getProcessId(VariablesSecureApp vars) throws ServletException {
    String command = vars.getCommand();
    if (command.equals("DEFAULT")) {
      return vars.getRequiredStringParameter("inpadProcessId");
    } else if (command.startsWith("BUTTON")) {
      return command.substring("BUTTON".length());
    } else if (command.startsWith("FRAMES")) {
      return command.substring("FRAMES".length());
    } else if (command.startsWith("SAVE_BUTTONActionButton")) {
      return command.substring("SAVE_BUTTONActionButton".length());
    }
    return null;
  }

  public void doPost (HttpServletRequest request, HttpServletResponse response) throws IOException,ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String strProcessId = getProcessId(vars);
    
    if (vars.getCommand().startsWith("FRAMES")) {
      printPageFrames(response, vars, strProcessId);
    }
   
    if (!vars.commandIn("DEFAULT")) {
      //Check access
      FeatureRestriction featureRestriction = ActivationKey.getInstance().hasLicenseAccess("P",
          strProcessId);
      if (featureRestriction != FeatureRestriction.NO_RESTRICTION) {
        licenseError("P", strProcessId, featureRestriction, response, request, vars, true);
      }
      if (!hasGeneralAccess(vars, "P", strProcessId)) {
        bdErrorGeneralPopUp(request, response,
            Utility.messageBD(this, "Error", vars.getLanguage()), Utility.messageBD(this,
                "AccessTableNoView", vars.getLanguage()));
      }
    }
    
      
    if (vars.commandIn("DEFAULT")) {
      printPageDefault(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON9DB4D30BFC5144B9B431CB49DDE9270D")) {
        
        printPageButton9DB4D30BFC5144B9B431CB49DDE9270D(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON7CB6B4D1ECCF4036B3F111D2CF11AADE")) {
        
        printPageButton7CB6B4D1ECCF4036B3F111D2CF11AADE(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON970EAD9B846648A7AB1F0CCA5058356C")) {
        
        printPageButton970EAD9B846648A7AB1F0CCA5058356C(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON7EDBFEC35BDA4FF4AF05ED516CDAFB90")) {
        
        printPageButton7EDBFEC35BDA4FF4AF05ED516CDAFB90(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTONABDFC8131D964936AD2EF7E0CED97FD9")) {
        
        printPageButtonABDFC8131D964936AD2EF7E0CED97FD9(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON3C386BC12832466790E50F2F8C5EBD85")) {
        
        printPageButton3C386BC12832466790E50F2F8C5EBD85(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTONEFDBF909811544DAAE4E876AA781E5DC")) {
        
        printPageButtonEFDBF909811544DAAE4E876AA781E5DC(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON107")) {
        
        printPageButton107(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTONCD7283DF804B449C97DA09446669EEEF")) {
        
        printPageButtonCD7283DF804B449C97DA09446669EEEF(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON85601427EAEE401FA0250FF0A6DD62EF")) {
        
        printPageButton85601427EAEE401FA0250FF0A6DD62EF(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTONA3FE1F9892394386A49FB707AA50A0FA")) {
        
        printPageButtonA3FE1F9892394386A49FB707AA50A0FA(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON136")) {
        
        printPageButton136(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTONFB740AB61B0E42B198D2C88D3A0D0CE6")) {
        
        printPageButtonFB740AB61B0E42B198D2C88D3A0D0CE6(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON58591E3E0F7648E4A09058E037CE49FC")) {
        
        printPageButton58591E3E0F7648E4A09058E037CE49FC(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON23D1B163EC0B41F790CE39BF01DA320E")) {
        
        printPageButton23D1B163EC0B41F790CE39BF01DA320E(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON6FBD65B0FDB74D1AB07F0EADF18D48AE")) {
        
        printPageButton6FBD65B0FDB74D1AB07F0EADF18D48AE(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON9EB2228A60684C0DBEC12D5CD8D85218")) {
        
        printPageButton9EB2228A60684C0DBEC12D5CD8D85218(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTOND85D5B5E368A49B1A6293BA4AE15F0F9")) {
        
        printPageButtonD85D5B5E368A49B1A6293BA4AE15F0F9(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTONFF80808133362F6A013336781FCE0066")) {
        
        printPageButtonFF80808133362F6A013336781FCE0066(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTONFF8081813219E68E013219ECFE930004")) {
        
        printPageButtonFF8081813219E68E013219ECFE930004(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTONFF808181324D007801324D2AE1130066")) {
        
        printPageButtonFF808181324D007801324D2AE1130066(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTONFF808181326CD80501326CE906D70042")) {
        
        printPageButtonFF808181326CD80501326CE906D70042(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTONFF80818132A4F6AD0132A573DD7A0021")) {
        
        printPageButtonFF80818132A4F6AD0132A573DD7A0021(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON2DDE7D3618034C38A4462B7F3456C28D")) {
        
        printPageButton2DDE7D3618034C38A4462B7F3456C28D(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON6BF16EFC772843AC9A17552AE0B26AB7")) {
        
        printPageButton6BF16EFC772843AC9A17552AE0B26AB7(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON0BDC2164ED3E48539FCEF4D306F29EFD")) {
        
        printPageButton0BDC2164ED3E48539FCEF4D306F29EFD(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON5BE14AA10165490A9ADEFB7532F7FA94")) {
        
        printPageButton5BE14AA10165490A9ADEFB7532F7FA94(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON58A9261BACEF45DDA526F29D8557272D")) {
        
        printPageButton58A9261BACEF45DDA526F29D8557272D(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON017312F51139438A9665775E3B5392A1")) {
        
        printPageButton017312F51139438A9665775E3B5392A1(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON6255BE488882480599C81284B70CD9B3")) {
        
        printPageButton6255BE488882480599C81284B70CD9B3(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTONF68F2890E96D4D85A1DEF0274D105BCE")) {
        
        printPageButtonF68F2890E96D4D85A1DEF0274D105BCE(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTON29D17F515727436DBCE32BC6CA28382B")) {
        
        printPageButton29D17F515727436DBCE32BC6CA28382B(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTONDE1B382FDD2540199D223586F6E216D0")) {
        
        printPageButtonDE1B382FDD2540199D223586F6E216D0(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTOND16966FBF9604A3D91A50DC83C6EA8E3")) {
        
        printPageButtonD16966FBF9604A3D91A50DC83C6EA8E3(response, vars, strProcessId);
    } else if (vars.commandIn("BUTTONFF8080812E2F8EAE012E2F94CF470014")) {
        
        printPageButtonFF8080812E2F8EAE012E2F94CF470014(response, vars, strProcessId);

    } else if (vars.commandIn("SAVE_BUTTONActionButton9DB4D30BFC5144B9B431CB49DDE9270D")) {
        process9DB4D30BFC5144B9B431CB49DDE9270D(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton7CB6B4D1ECCF4036B3F111D2CF11AADE")) {
        process7CB6B4D1ECCF4036B3F111D2CF11AADE(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton970EAD9B846648A7AB1F0CCA5058356C")) {
        process970EAD9B846648A7AB1F0CCA5058356C(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton7EDBFEC35BDA4FF4AF05ED516CDAFB90")) {
        process7EDBFEC35BDA4FF4AF05ED516CDAFB90(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButtonABDFC8131D964936AD2EF7E0CED97FD9")) {
        processABDFC8131D964936AD2EF7E0CED97FD9(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton3C386BC12832466790E50F2F8C5EBD85")) {
        process3C386BC12832466790E50F2F8C5EBD85(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButtonEFDBF909811544DAAE4E876AA781E5DC")) {
        processEFDBF909811544DAAE4E876AA781E5DC(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton107")) {
        process107(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButtonCD7283DF804B449C97DA09446669EEEF")) {
        processCD7283DF804B449C97DA09446669EEEF(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton85601427EAEE401FA0250FF0A6DD62EF")) {
        process85601427EAEE401FA0250FF0A6DD62EF(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButtonA3FE1F9892394386A49FB707AA50A0FA")) {
        processA3FE1F9892394386A49FB707AA50A0FA(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton136")) {
        process136(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButtonFB740AB61B0E42B198D2C88D3A0D0CE6")) {
        processFB740AB61B0E42B198D2C88D3A0D0CE6(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton58591E3E0F7648E4A09058E037CE49FC")) {
        process58591E3E0F7648E4A09058E037CE49FC(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton23D1B163EC0B41F790CE39BF01DA320E")) {
        process23D1B163EC0B41F790CE39BF01DA320E(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton6FBD65B0FDB74D1AB07F0EADF18D48AE")) {
        process6FBD65B0FDB74D1AB07F0EADF18D48AE(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton9EB2228A60684C0DBEC12D5CD8D85218")) {
        process9EB2228A60684C0DBEC12D5CD8D85218(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButtonD85D5B5E368A49B1A6293BA4AE15F0F9")) {
        processD85D5B5E368A49B1A6293BA4AE15F0F9(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButtonFF80808133362F6A013336781FCE0066")) {
        processFF80808133362F6A013336781FCE0066(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButtonFF8081813219E68E013219ECFE930004")) {
        processFF8081813219E68E013219ECFE930004(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButtonFF808181324D007801324D2AE1130066")) {
        processFF808181324D007801324D2AE1130066(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButtonFF808181326CD80501326CE906D70042")) {
        processFF808181326CD80501326CE906D70042(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButtonFF80818132A4F6AD0132A573DD7A0021")) {
        processFF80818132A4F6AD0132A573DD7A0021(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton2DDE7D3618034C38A4462B7F3456C28D")) {
        process2DDE7D3618034C38A4462B7F3456C28D(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton6BF16EFC772843AC9A17552AE0B26AB7")) {
        process6BF16EFC772843AC9A17552AE0B26AB7(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton0BDC2164ED3E48539FCEF4D306F29EFD")) {
        process0BDC2164ED3E48539FCEF4D306F29EFD(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton5BE14AA10165490A9ADEFB7532F7FA94")) {
        process5BE14AA10165490A9ADEFB7532F7FA94(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton58A9261BACEF45DDA526F29D8557272D")) {
        process58A9261BACEF45DDA526F29D8557272D(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton017312F51139438A9665775E3B5392A1")) {
        process017312F51139438A9665775E3B5392A1(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton6255BE488882480599C81284B70CD9B3")) {
        process6255BE488882480599C81284B70CD9B3(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButtonF68F2890E96D4D85A1DEF0274D105BCE")) {
        processF68F2890E96D4D85A1DEF0274D105BCE(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButton29D17F515727436DBCE32BC6CA28382B")) {
        process29D17F515727436DBCE32BC6CA28382B(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButtonDE1B382FDD2540199D223586F6E216D0")) {
        processDE1B382FDD2540199D223586F6E216D0(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButtonD16966FBF9604A3D91A50DC83C6EA8E3")) {
        processD16966FBF9604A3D91A50DC83C6EA8E3(strProcessId, vars, request, response);
    } else if (vars.commandIn("SAVE_BUTTONActionButtonFF8080812E2F8EAE012E2F94CF470014")) {
        processFF8080812E2F8EAE012E2F94CF470014(strProcessId, vars, request, response);

    } else pageErrorPopUp(response);
  }
  
  void printPageFrames(HttpServletResponse response, VariablesSecureApp vars, String strProcessId) throws IOException, ServletException {
    log4j.debug("Output: Default");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonDefaultFrames").createXmlDocument();
    xmlDocument.setParameter("processId", strProcessId);
    xmlDocument.setParameter("trlFormType", "PROCESS");
    xmlDocument.setParameter("type", "ActionButtonJava_Responser.html");
    xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
    out.println(xmlDocument.print());
    out.close();
  }

  void printPageDefault(HttpServletResponse response, VariablesSecureApp vars, String strProcessId) throws IOException, ServletException {
    log4j.debug("Output: Default");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonDefault").createXmlDocument();
    xmlDocument.setParameter("processId", strProcessId);
	  xmlDocument.setParameter("trlFormType", "PROCESS");
	  xmlDocument.setParameter("type", "ActionButtonJava_Responser.html");
	  xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
    out.println(xmlDocument.print());
    out.close();
  }

    void printPageButton9DB4D30BFC5144B9B431CB49DDE9270D(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 9DB4D30BFC5144B9B431CB49DDE9270D");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton9DB4D30BFC5144B9B431CB49DDE9270D", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("9DB4D30BFC5144B9B431CB49DDE9270D");
        vars.removeMessage("9DB4D30BFC5144B9B431CB49DDE9270D");
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
    void printPageButton7CB6B4D1ECCF4036B3F111D2CF11AADE(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 7CB6B4D1ECCF4036B3F111D2CF11AADE");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton7CB6B4D1ECCF4036B3F111D2CF11AADE", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
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
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, Utility.getContext(this, vars, "#M_Warehouse_ID", windowId));
    xmlDocument.setData("reportM_Warehouse_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton970EAD9B846648A7AB1F0CCA5058356C(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 970EAD9B846648A7AB1F0CCA5058356C");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton970EAD9B846648A7AB1F0CCA5058356C", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("970EAD9B846648A7AB1F0CCA5058356C");
        vars.removeMessage("970EAD9B846648A7AB1F0CCA5058356C");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    xmlDocument.setParameter("Name", "");
    xmlDocument.setParameter("ImportAuditInfo", "");
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton7EDBFEC35BDA4FF4AF05ED516CDAFB90(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 7EDBFEC35BDA4FF4AF05ED516CDAFB90");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton7EDBFEC35BDA4FF4AF05ED516CDAFB90", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("7EDBFEC35BDA4FF4AF05ED516CDAFB90");
        vars.removeMessage("7EDBFEC35BDA4FF4AF05ED516CDAFB90");
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
    void printPageButtonABDFC8131D964936AD2EF7E0CED97FD9(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process ABDFC8131D964936AD2EF7E0CED97FD9");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonABDFC8131D964936AD2EF7E0CED97FD9", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("ABDFC8131D964936AD2EF7E0CED97FD9");
        vars.removeMessage("ABDFC8131D964936AD2EF7E0CED97FD9");
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
    void printPageButton3C386BC12832466790E50F2F8C5EBD85(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 3C386BC12832466790E50F2F8C5EBD85");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton3C386BC12832466790E50F2F8C5EBD85", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("3C386BC12832466790E50F2F8C5EBD85");
        vars.removeMessage("3C386BC12832466790E50F2F8C5EBD85");
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
    void printPageButtonEFDBF909811544DAAE4E876AA781E5DC(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process EFDBF909811544DAAE4E876AA781E5DC");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonEFDBF909811544DAAE4E876AA781E5DC", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("EFDBF909811544DAAE4E876AA781E5DC");
        vars.removeMessage("EFDBF909811544DAAE4E876AA781E5DC");
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
    void printPageButton107(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 107");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton107", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
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
    void printPageButtonCD7283DF804B449C97DA09446669EEEF(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process CD7283DF804B449C97DA09446669EEEF");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonCD7283DF804B449C97DA09446669EEEF", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("CD7283DF804B449C97DA09446669EEEF");
        vars.removeMessage("CD7283DF804B449C97DA09446669EEEF");
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
    void printPageButton85601427EAEE401FA0250FF0A6DD62EF(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 85601427EAEE401FA0250FF0A6DD62EF");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton85601427EAEE401FA0250FF0A6DD62EF", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("85601427EAEE401FA0250FF0A6DD62EF");
        vars.removeMessage("85601427EAEE401FA0250FF0A6DD62EF");
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
    void printPageButtonA3FE1F9892394386A49FB707AA50A0FA(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process A3FE1F9892394386A49FB707AA50A0FA");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonA3FE1F9892394386A49FB707AA50A0FA", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
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
    void printPageButton136(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 136");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton136", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("136");
        vars.removeMessage("136");
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
    void printPageButtonFB740AB61B0E42B198D2C88D3A0D0CE6(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process FB740AB61B0E42B198D2C88D3A0D0CE6");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonFB740AB61B0E42B198D2C88D3A0D0CE6", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("FB740AB61B0E42B198D2C88D3A0D0CE6");
        vars.removeMessage("FB740AB61B0E42B198D2C88D3A0D0CE6");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("DueDate", Utility.getContext(this, vars, "Duedate", ""));
    xmlDocument.setParameter("DueDate_Format", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("FIN_Payment_Priority_ID", Utility.getContext(this, vars, "FIN_Payment_Priority_ID", ""));
    comboTableData = new ComboTableData(vars, this, "19", "FIN_Payment_Priority_ID", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, Utility.getContext(this, vars, "FIN_Payment_Priority_ID", ""));
    xmlDocument.setData("reportFIN_Payment_Priority_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton58591E3E0F7648E4A09058E037CE49FC(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 58591E3E0F7648E4A09058E037CE49FC");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton58591E3E0F7648E4A09058E037CE49FC", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("58591E3E0F7648E4A09058E037CE49FC");
        vars.removeMessage("58591E3E0F7648E4A09058E037CE49FC");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("M_Product_Id", "");
    xmlDocument.setParameter("M_Product_IdR", "");
    xmlDocument.setParameter("M_CH_Value_ID", "");
    comboTableData = new ComboTableData(vars, this, "19", "M_CH_Value_ID", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "");
    xmlDocument.setData("reportM_CH_Value_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton23D1B163EC0B41F790CE39BF01DA320E(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 23D1B163EC0B41F790CE39BF01DA320E");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton23D1B163EC0B41F790CE39BF01DA320E", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
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
    xmlDocument.setParameter("M_AttributeSetInstance_IDR", "");
    xmlDocument.setParameter("Returned", "");
    xmlDocument.setParameter("PriceStd", "");
    xmlDocument.setParameter("C_Tax_ID", "");
    comboTableData = new ComboTableData(vars, this, "19", "C_Tax_ID", "", "299FA667CF374AC5ACC74739C3251134", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "");
    xmlDocument.setData("reportC_Tax_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("C_Return_Reason_ID", "");
    comboTableData = new ComboTableData(vars, this, "19", "C_Return_Reason_ID", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "");
    xmlDocument.setData("reportC_Return_Reason_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton6FBD65B0FDB74D1AB07F0EADF18D48AE(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 6FBD65B0FDB74D1AB07F0EADF18D48AE");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton6FBD65B0FDB74D1AB07F0EADF18D48AE", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("6FBD65B0FDB74D1AB07F0EADF18D48AE");
        vars.removeMessage("6FBD65B0FDB74D1AB07F0EADF18D48AE");
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
    void printPageButton9EB2228A60684C0DBEC12D5CD8D85218(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 9EB2228A60684C0DBEC12D5CD8D85218");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton9EB2228A60684C0DBEC12D5CD8D85218", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
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
    void printPageButtonD85D5B5E368A49B1A6293BA4AE15F0F9(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process D85D5B5E368A49B1A6293BA4AE15F0F9");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonD85D5B5E368A49B1A6293BA4AE15F0F9", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("D85D5B5E368A49B1A6293BA4AE15F0F9");
        vars.removeMessage("D85D5B5E368A49B1A6293BA4AE15F0F9");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("AD_Client_ID", "");
    comboTableData = new ComboTableData(vars, this, "19", "AD_Client_ID", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "");
    xmlDocument.setData("reportAD_Client_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("ExportAuditInfo", "");
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButtonFF80808133362F6A013336781FCE0066(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process FF80808133362F6A013336781FCE0066");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonFF80808133362F6A013336781FCE0066", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("FF80808133362F6A013336781FCE0066");
        vars.removeMessage("FF80808133362F6A013336781FCE0066");
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
    void printPageButtonFF8081813219E68E013219ECFE930004(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process FF8081813219E68E013219ECFE930004");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonFF8081813219E68E013219ECFE930004", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("FF8081813219E68E013219ECFE930004");
        vars.removeMessage("FF8081813219E68E013219ECFE930004");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("Value", ActionButtonSQLDefaultData.selectActPFF8081813219E68E013219ECFE930004_Value(this, Utility.getContext(this, vars, "MA_SEQUENCEPRODUCT_ID", "")));
    xmlDocument.setParameter("Name", ActionButtonSQLDefaultData.selectActPFF8081813219E68E013219ECFE930004_Name(this, Utility.getContext(this, vars, "MA_SEQUENCEPRODUCT_ID", "")));
    xmlDocument.setParameter("M_Product_Category_ID", "");
    comboTableData = new ComboTableData(vars, this, "19", "M_Product_Category_ID", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "");
    xmlDocument.setData("reportM_Product_Category_ID", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("Productiontype", "+");
    comboTableData = new ComboTableData(vars, this, "17", "Productiontype", "800034", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "+");
    xmlDocument.setData("reportProductiontype", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("Qty", "0");
    xmlDocument.setParameter("Copyattribute", "Y");
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButtonFF808181324D007801324D2AE1130066(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process FF808181324D007801324D2AE1130066");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonFF808181324D007801324D2AE1130066", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
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
    void printPageButtonFF808181326CD80501326CE906D70042(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process FF808181326CD80501326CE906D70042");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonFF808181326CD80501326CE906D70042", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
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
    void printPageButtonFF80818132A4F6AD0132A573DD7A0021(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process FF80818132A4F6AD0132A573DD7A0021");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonFF80818132A4F6AD0132A573DD7A0021", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
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
    void printPageButton2DDE7D3618034C38A4462B7F3456C28D(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 2DDE7D3618034C38A4462B7F3456C28D");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton2DDE7D3618034C38A4462B7F3456C28D", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
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
    xmlDocument.setParameter("action", Utility.getContext(this, vars, "EM_APRM_Process_BS", ""));
    comboTableData = new ComboTableData(vars, this, "17", "action", "EC75B6F5A9504DB6B3F3356EA85F15EE", "CA425689672A42D7BE2158EE41E44F94", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, Utility.getContext(this, vars, "EM_APRM_Process_BS", ""));
    xmlDocument.setData("reportaction", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton6BF16EFC772843AC9A17552AE0B26AB7(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 6BF16EFC772843AC9A17552AE0B26AB7");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton6BF16EFC772843AC9A17552AE0B26AB7", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
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
    xmlDocument.setParameter("action", Utility.getContext(this, vars, "Process_Reconciliation", ""));
    comboTableData = new ComboTableData(vars, this, "17", "action", "FF8080812E443491012E443C053A001A", "FF808081332719060133271E5BB1001B", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, Utility.getContext(this, vars, "Process_Reconciliation", ""));
    xmlDocument.setData("reportaction", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton0BDC2164ED3E48539FCEF4D306F29EFD(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 0BDC2164ED3E48539FCEF4D306F29EFD");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton0BDC2164ED3E48539FCEF4D306F29EFD", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("0BDC2164ED3E48539FCEF4D306F29EFD");
        vars.removeMessage("0BDC2164ED3E48539FCEF4D306F29EFD");
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
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "");
    xmlDocument.setData("reportaction", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton5BE14AA10165490A9ADEFB7532F7FA94(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 5BE14AA10165490A9ADEFB7532F7FA94");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton5BE14AA10165490A9ADEFB7532F7FA94", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("5BE14AA10165490A9ADEFB7532F7FA94");
        vars.removeMessage("5BE14AA10165490A9ADEFB7532F7FA94");
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
    void printPageButton58A9261BACEF45DDA526F29D8557272D(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 58A9261BACEF45DDA526F29D8557272D");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton58A9261BACEF45DDA526F29D8557272D", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
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
    xmlDocument.setParameter("action", Utility.getContext(this, vars, "EM_APRM_Process_BS", ""));
    comboTableData = new ComboTableData(vars, this, "17", "action", "EC75B6F5A9504DB6B3F3356EA85F15EE", "CA425689672A42D7BE2158EE41E44F94", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, Utility.getContext(this, vars, "EM_APRM_Process_BS", ""));
    xmlDocument.setData("reportaction", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton017312F51139438A9665775E3B5392A1(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 017312F51139438A9665775E3B5392A1");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton017312F51139438A9665775E3B5392A1", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
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
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "");
    xmlDocument.setData("reportaction", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton6255BE488882480599C81284B70CD9B3(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 6255BE488882480599C81284B70CD9B3");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton6255BE488882480599C81284B70CD9B3", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("6255BE488882480599C81284B70CD9B3");
        vars.removeMessage("6255BE488882480599C81284B70CD9B3");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("action", Utility.getContext(this, vars, "EM_APRM_Process_Payment", ""));
    comboTableData = new ComboTableData(vars, this, "17", "action", "36972531DA994BB38ECB91993058282F", "575E470ABADB4C278132C957A78C47E3", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, Utility.getContext(this, vars, "EM_APRM_Process_Payment", ""));
    xmlDocument.setData("reportaction", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButtonF68F2890E96D4D85A1DEF0274D105BCE(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process F68F2890E96D4D85A1DEF0274D105BCE");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonF68F2890E96D4D85A1DEF0274D105BCE", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("F68F2890E96D4D85A1DEF0274D105BCE");
        vars.removeMessage("F68F2890E96D4D85A1DEF0274D105BCE");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("action", "");
    comboTableData = new ComboTableData(vars, this, "17", "action", "F671DDEA466D41A996F605590CB545BC", "FAE0D7C8A9D84FAFAE3C10CD5DCE6E30", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "");
    xmlDocument.setData("reportaction", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButton29D17F515727436DBCE32BC6CA28382B(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process 29D17F515727436DBCE32BC6CA28382B");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButton29D17F515727436DBCE32BC6CA28382B", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("29D17F515727436DBCE32BC6CA28382B");
        vars.removeMessage("29D17F515727436DBCE32BC6CA28382B");
        if (myMessage!=null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

          try {
    ComboTableData comboTableData = null;
    xmlDocument.setParameter("action", "RV");
    comboTableData = new ComboTableData(vars, this, "17", "action", "66F2DCC800A34F94923444C29478E70A", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, "RV");
    xmlDocument.setData("reportaction", "liststructure", comboTableData.select(false));
comboTableData = null;
    xmlDocument.setParameter("paymentDate", "");
    xmlDocument.setParameter("paymentDate_Format", vars.getSessionValue("#AD_SqlDateFormat"));
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }
    void printPageButtonDE1B382FDD2540199D223586F6E216D0(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process DE1B382FDD2540199D223586F6E216D0");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonDE1B382FDD2540199D223586F6E216D0", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("DE1B382FDD2540199D223586F6E216D0");
        vars.removeMessage("DE1B382FDD2540199D223586F6E216D0");
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
    void printPageButtonD16966FBF9604A3D91A50DC83C6EA8E3(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process D16966FBF9604A3D91A50DC83C6EA8E3");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonD16966FBF9604A3D91A50DC83C6EA8E3", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
      {
        OBError myMessage = vars.getMessage("D16966FBF9604A3D91A50DC83C6EA8E3");
        vars.removeMessage("D16966FBF9604A3D91A50DC83C6EA8E3");
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
    void printPageButtonFF8080812E2F8EAE012E2F94CF470014(HttpServletResponse response, VariablesSecureApp vars, String strProcessId)
    throws IOException, ServletException {
      log4j.debug("Output: Button process FF8080812E2F8EAE012E2F94CF470014");
      String[] discard = {"newDiscard"};
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/ActionButtonFF8080812E2F8EAE012E2F94CF470014", discard).createXmlDocument();
      xmlDocument.setParameter("processing", "Y");
      xmlDocument.setParameter("form", "ActionButtonJava_Responser.html");
      xmlDocument.setParameter("css", vars.getTheme());
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang = \"" + vars.getLanguage() + "\";\n");
      xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
      xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));
      xmlDocument.setParameter("processId", strProcessId);
			xmlDocument.setParameter("trlFormType", "PROCESS");
          
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
    xmlDocument.setParameter("action", Utility.getContext(this, vars, "Process_Reconciliation", ""));
    comboTableData = new ComboTableData(vars, this, "17", "action", "FF8080812E443491012E443C053A001A", "FF808081332719060133271E5BB1001B", Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, windowId, Utility.getContext(this, vars, "Process_Reconciliation", ""));
    xmlDocument.setData("reportaction", "liststructure", comboTableData.select(false));
comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

      out.println(xmlDocument.print());
      out.close();
    }


    private void process9DB4D30BFC5144B9B431CB49DDE9270D(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.erpCommon.ad_process.KillSession().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process7CB6B4D1ECCF4036B3F111D2CF11AADE(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String strmWarehouseId = vars.getStringParameter("inpmWarehouseId");
params.put("mWarehouseId", strmWarehouseId);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.erpCommon.ad_process.MRPPurchaseCreateReservations().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process970EAD9B846648A7AB1F0CCA5058356C(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String strname = vars.getStringParameter("inpname");
params.put("name", strname);
String strimportauditinfo = vars.getStringParameter("inpimportauditinfo", "N");
params.put("importauditinfo", strimportauditinfo);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.service.db.ImportClientProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process7EDBFEC35BDA4FF4AF05ED516CDAFB90(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.erpCommon.ad_process.CreateCustomModule().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void processABDFC8131D964936AD2EF7E0CED97FD9(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.erpCommon.ad_process.UpdateActuals().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process3C386BC12832466790E50F2F8C5EBD85(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.materialmgmt.VariantAutomaticGenerationProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void processEFDBF909811544DAAE4E876AA781E5DC(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.erpCommon.ad_process.EndYearClose().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process107(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.materialmgmt.InventoryCountProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void processCD7283DF804B449C97DA09446669EEEF(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.advpaymentmngt.process.ProcessBatch().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process85601427EAEE401FA0250FF0A6DD62EF(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.erpCommon.ad_process.assets.AssetLinearDepreciationMethodProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void processA3FE1F9892394386A49FB707AA50A0FA(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String strrecalculateprices = vars.getStringParameter("inprecalculateprices", "N");
params.put("recalculateprices", strrecalculateprices);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.erpCommon.ad_process.ConvertQuotationIntoOrder().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process136(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.erpCommon.ad_process.VerifyBOM().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void processFB740AB61B0E42B198D2C88D3A0D0CE6(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String strduedate = vars.getStringParameter("inpduedate");
params.put("duedate", strduedate);
String strfinPaymentPriorityId = vars.getStringParameter("inpfinPaymentPriorityId");
params.put("finPaymentPriorityId", strfinPaymentPriorityId);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.erpCommon.ad_process.UpdatePaymentPlan().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process58591E3E0F7648E4A09058E037CE49FC(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String strmProductId = vars.getStringParameter("inpmProductId");
params.put("mProductId", strmProductId);
String strmChValueId = vars.getStringParameter("inpmChValueId");
params.put("mChValueId", strmChValueId);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.materialmgmt.VariantChDescUpdateProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process23D1B163EC0B41F790CE39BF01DA320E(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
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
          new org.openbravo.erpCommon.ad_actionButton.RMInsertOrphanLine().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process6FBD65B0FDB74D1AB07F0EADF18D48AE(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.erpCommon.ad_actionButton.MRPManufacturingPlanProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process9EB2228A60684C0DBEC12D5CD8D85218(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.erpCommon.ad_process.CalculatePromotions().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void processD85D5B5E368A49B1A6293BA4AE15F0F9(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String stradClientId = vars.getStringParameter("inpadClientId");
params.put("adClientId", stradClientId);
String strexportauditinfo = vars.getStringParameter("inpexportauditinfo", "N");
params.put("exportauditinfo", strexportauditinfo);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.service.db.ExportClientProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void processFF80808133362F6A013336781FCE0066(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.erpCommon.ad_actionButton.RMCreateInvoice().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void processFF8081813219E68E013219ECFE930004(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String strvalue = vars.getStringParameter("inpvalue");
params.put("value", strvalue);
String strname = vars.getStringParameter("inpname");
params.put("name", strname);
String strmProductCategoryId = vars.getStringParameter("inpmProductCategoryId");
params.put("mProductCategoryId", strmProductCategoryId);
String strproductiontype = vars.getStringParameter("inpproductiontype");
params.put("productiontype", strproductiontype);
String strqty = vars.getNumericParameter("inpqty");
params.put("qty", strqty);
String strcopyattribute = vars.getStringParameter("inpcopyattribute", "N");
params.put("copyattribute", strcopyattribute);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.erpCommon.ad_actionButton.SequenceProductCreate().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void processFF808181324D007801324D2AE1130066(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String strdate = vars.getStringParameter("inpdate");
params.put("date", strdate);
String strstarttime = vars.getStringParameter("inpstarttime");
params.put("starttime", strstarttime);
String strendtime = vars.getStringParameter("inpendtime");
params.put("endtime", strendtime);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.erpCommon.ad_actionButton.CreateWorkEffort().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void processFF808181326CD80501326CE906D70042(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.erpCommon.ad_actionButton.ValidateWorkEffort_ProductionRun().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void processFF80818132A4F6AD0132A573DD7A0021(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.erpCommon.ad_actionButton.CreateStandards().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process2DDE7D3618034C38A4462B7F3456C28D(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String straction = vars.getStringParameter("inpaction");
params.put("action", straction);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.advpaymentmngt.process.FIN_BankStatementProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process6BF16EFC772843AC9A17552AE0B26AB7(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String straction = vars.getStringParameter("inpaction");
params.put("action", straction);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.advpaymentmngt.process.FIN_ReconciliationProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process0BDC2164ED3E48539FCEF4D306F29EFD(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String straction = vars.getStringParameter("inpaction");
params.put("action", straction);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.advpaymentmngt.process.FIN_DoubtfulDebtProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process5BE14AA10165490A9ADEFB7532F7FA94(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.advpaymentmngt.process.FIN_AddPaymentFromJournal().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process58A9261BACEF45DDA526F29D8557272D(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String straction = vars.getStringParameter("inpaction");
params.put("action", straction);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.advpaymentmngt.process.FIN_BankStatementProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process017312F51139438A9665775E3B5392A1(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String straction = vars.getStringParameter("inpaction");
params.put("action", straction);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.advpaymentmngt.process.FIN_DoubtfulDebtRunProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process6255BE488882480599C81284B70CD9B3(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String straction = vars.getStringParameter("inpaction");
params.put("action", straction);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.advpaymentmngt.process.FIN_PaymentProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void processF68F2890E96D4D85A1DEF0274D105BCE(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String straction = vars.getStringParameter("inpaction");
params.put("action", straction);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.advpaymentmngt.process.FIN_TransactionProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void process29D17F515727436DBCE32BC6CA28382B(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String straction = vars.getStringParameter("inpaction");
params.put("action", straction);
String strpaymentdate = vars.getStringParameter("inppaymentdate");
params.put("paymentdate", strpaymentdate);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.advpaymentmngt.process.FIN_PaymentProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void processDE1B382FDD2540199D223586F6E216D0(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.advpaymentmngt.process.FIN_AddPaymentFromJournalLine().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void processD16966FBF9604A3D91A50DC83C6EA8E3(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        
        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.advpaymentmngt.process.FIN_PaymentProposalProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }
    private void processFF8080812E2F8EAE012E2F94CF470014(String strProcessId, VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
        
        
        ProcessBundle pb = new ProcessBundle(strProcessId, vars).init(this);
        HashMap<String, Object> params= new HashMap<String, Object>();
       
        String straction = vars.getStringParameter("inpaction");
params.put("action", straction);

        
        pb.setParams(params);
        OBError myMessage = null;
        try {
          new org.openbravo.advpaymentmngt.process.FIN_ReconciliationProcess().execute(pb);
          if((OBError)pb.getResult()!=null){
            myMessage = (OBError) pb.getResult();
            myMessage.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getMessage()));
            myMessage.setTitle(Utility.parseTranslation(this, vars, vars.getLanguage(), myMessage.getTitle()));
          }
        } catch (Exception ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          log4j.error("Error calling process", ex);
          if (!myMessage.isConnectionAvailable()) {
            bdErrorConnection(response);
            return;
          }
        }

        processButtonHelper(request, response, vars, myMessage); 
   }


  public String getServletInfo() {
    return "Servlet ActionButton_Responser. This Servlet was made by Wad constructor";
  } // end of the getServletInfo() method

  private void processButtonHelper(HttpServletRequest request, HttpServletResponse response, VariablesSecureApp vars, OBError myMessage) 
     throws ServletException, IOException {
      advisePopUp(request, response, myMessage.getType(), myMessage.getTitle(), myMessage.getMessage());
  }
}
