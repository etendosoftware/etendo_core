
package org.openbravo.erpCommon.ad_callouts;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;


public class ComboReloadsProcessHelper extends CalloutHelper {
  private static final long serialVersionUID = 1L;

  void printPage(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
   String strProcessId = vars.getStringParameter("inpadProcessId");
   
     if (strProcessId.equals("224")) {
       process224(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("108")) {
       process108(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("155")) {
       process155(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("6255BE488882480599C81284B70CD9B3")) {
       process6255BE488882480599C81284B70CD9B3(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("2DDE7D3618034C38A4462B7F3456C28D")) {
       process2DDE7D3618034C38A4462B7F3456C28D(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("221")) {
       process221(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("58A9261BACEF45DDA526F29D8557272D")) {
       process58A9261BACEF45DDA526F29D8557272D(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("0BDC2164ED3E48539FCEF4D306F29EFD")) {
       process0BDC2164ED3E48539FCEF4D306F29EFD(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("5A2A0AF88AF54BB085DCC52FCC9B17B7")) {
       process5A2A0AF88AF54BB085DCC52FCC9B17B7(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("6BF16EFC772843AC9A17552AE0B26AB7")) {
       process6BF16EFC772843AC9A17552AE0B26AB7(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("140")) {
       process140(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("F68F2890E96D4D85A1DEF0274D105BCE")) {
       processF68F2890E96D4D85A1DEF0274D105BCE(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("800163")) {
       process800163(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("1004400000")) {
       process1004400000(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("800075")) {
       process800075(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("800136")) {
       process800136(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("154")) {
       process154(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("225")) {
       process225(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("D234AE084F7040DCB66E281A4237FF99")) {
       processD234AE084F7040DCB66E281A4237FF99(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("800131")) {
       process800131(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("800172")) {
       process800172(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("017312F51139438A9665775E3B5392A1")) {
       process017312F51139438A9665775E3B5392A1(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("112")) {
       process112(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("23D1B163EC0B41F790CE39BF01DA320E")) {
       process23D1B163EC0B41F790CE39BF01DA320E(response, vars, strTabId, windowId);
       return;
     }
    
     if (strProcessId.equals("FF8080812E2F8EAE012E2F94CF470014")) {
       processFF8080812E2F8EAE012E2F94CF470014(response, vars, strTabId, windowId);
       return;
     }
    
    
    pageError(response);
  }
  
  
    private void process224(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads224';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpcProjectId")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "19", "C_ProjectLine_ID", "", "175", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpcProjectlineId";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process108(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads108';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpcAcctschemaId")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "19", "C_AcctSchema_ID", "", "FDA7BA9355A6468DAF67E1C5288990A6", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpcAcctschemaId";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process155(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads155';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpinppaymentrule")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "17", "PaymentRule", "195", "162", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inppaymentrule";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process6255BE488882480599C81284B70CD9B3(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads6255BE488882480599C81284B70CD9B3';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpemAprmProcessPayment", "inpemAprmProcessPayment", "inpfinPaymentId", "inpemAprmProcessPayment", "inpfinPaymentId", "inpemAprmProcessPayment", "inpstatus", "inpemAprmProcessPayment")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "17", "action", "36972531DA994BB38ECB91993058282F", "575E470ABADB4C278132C957A78C47E3", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpaction";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process2DDE7D3618034C38A4462B7F3456C28D(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads2DDE7D3618034C38A4462B7F3456C28D';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpprocessed", "inpprocessed")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "17", "action", "EC75B6F5A9504DB6B3F3356EA85F15EE", "CA425689672A42D7BE2158EE41E44F94", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpaction";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process221(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads221';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpadOrgId")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "19", "C_BankAccount_ID", "", "", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpcBankaccountId";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process58A9261BACEF45DDA526F29D8557272D(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads58A9261BACEF45DDA526F29D8557272D';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpprocessed", "inpprocessed")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "17", "action", "EC75B6F5A9504DB6B3F3356EA85F15EE", "CA425689672A42D7BE2158EE41E44F94", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpaction";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process0BDC2164ED3E48539FCEF4D306F29EFD(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads0BDC2164ED3E48539FCEF4D306F29EFD';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpemAprmProcess", "inpemAprmProcess")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "17", "action", "798239EB069F41A9BA8EE040C63DDBBC", "3842B167CA6F44239C3357A721E3BA6A", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpaction";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process5A2A0AF88AF54BB085DCC52FCC9B17B7(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads5A2A0AF88AF54BB085DCC52FCC9B17B7';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpresStatus", "inpresStatus", "inpresStatus", "inpresStatus")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "17", "RES_Action", "440DDA64A43F4799AAFF48BC86DC8F78", "1645143617E44289A08A1EA4D617A184", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpresAction";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process6BF16EFC772843AC9A17552AE0B26AB7(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads6BF16EFC772843AC9A17552AE0B26AB7';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpprocessed", "inpprocessed")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "17", "action", "FF8080812E443491012E443C053A001A", "FF808081332719060133271E5BB1001B", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpaction";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process140(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads140';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpadOrgId", "inpadOrgId")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "19", "C_AcctSchema_ID", "", "FF8081812F06A183012F07323A2A001C", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpcAcctschemaId";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void processF68F2890E96D4D85A1DEF0274D105BCE(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloadsF68F2890E96D4D85A1DEF0274D105BCE';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpprocessed", "inpprocessed")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "17", "action", "F671DDEA466D41A996F605590CB545BC", "FAE0D7C8A9D84FAFAE3C10CD5DCE6E30", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpaction";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process800163(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads800163';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpadOrgId")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "19", "M_Warehouse_ID", "", "71188F0005494DA08311B4FFB2C5A993", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpmWarehouseId";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process1004400000(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads1004400000';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpadOrgId")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "19", "M_Warehouse_ID", "", "A3DCDE5EDD4A4403AC205B131F10F84D", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpmWarehouseId";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process800075(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads800075';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inp#adClientId")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "18", "M_Warehouse_ID", "197", "", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpmWarehouseId";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process800136(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads800136';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpadOrgId", "inpadOrgId")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "19", "C_AcctSchema_ID", "", "FF8081812F06A183012F07323A2A001C", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpcAcctschemaId";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process154(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads154';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpissotrx", "inpadOrgId", "inpadClientId")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "19", "M_PriceList_Version_ID", "", "26D8602C48004E1182B46310DF7015AE", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpmPricelistVersionId";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process225(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads225';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpcProjectId")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "19", "C_ProjectLine_ID", "", "174", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpcProjectlineId";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void processD234AE084F7040DCB66E281A4237FF99(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloadsD234AE084F7040DCB66E281A4237FF99';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inp#adRoleId", "inp#adRoleId")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "19", "AD_Org_ID", "", "D9463AFD77E44F619D396C19BF9E6A15", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpadOrgId";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
      if (CalloutHelper.commandInCommandList(command, "inpadOrgId")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "17", "outputType", "800104", "", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpoutputtype";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
      if (CalloutHelper.commandInCommandList(command, "inpadOrgId")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "17", "reportType", "B82C3C28E51F4AA6B87D98E7ABBF92F0", "", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpreporttype";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
      if (CalloutHelper.commandInCommandList(command, "inpadOrgId")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "19", "C_AcctSchema_ID", "", "", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpcAcctschemaId";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process800131(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads800131';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpstatus", "inpstatus")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "17", "action", "657B89EF105149F2B011CF8F5034FF92", "C5A7AABB91A440EBAA53A0222B99FF2F", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpaction";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process800172(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads800172';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpinpoutputtype")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "17", "outputType", "800104", "1000200002", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpoutputtype";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process017312F51139438A9665775E3B5392A1(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads017312F51139438A9665775E3B5392A1';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpemAprmProcess", "inpemAprmProcess")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "17", "action", "798239EB069F41A9BA8EE040C63DDBBC", "3842B167CA6F44239C3357A721E3BA6A", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpaction";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process112(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads112';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpadOrgId", "inpadOrgId")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "19", "C_AcctSchema_ID", "", "FF8081812F06A183012F07323A2A001C", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpcAcctschemaId";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void process23D1B163EC0B41F790CE39BF01DA320E(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloads23D1B163EC0B41F790CE39BF01DA320E';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpissotrx", "inpadClientId", "inpadOrgId", "inp#adClientId", "inpmProductId", "inpissotrx", "inpadOrgId", "inpadOrgId")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "19", "C_Tax_ID", "", "299FA667CF374AC5ACC74739C3251134", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpcTaxId";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
    private void processFF8080812E2F8EAE012E2F94CF470014(HttpServletResponse response, VariablesSecureApp vars, String strTabId, String windowId) throws IOException, ServletException {
        String resultField;
        String command = vars.getStringParameter("Command", "DEFAULT");
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
        
        StringBuffer resultado = new StringBuffer();
        boolean isFirst=true;
        ComboTableData comboTableData = null;
        resultado.append("var calloutName='ComboReloadsFF8080812E2F8EAE012E2F94CF470014';\n\n");
        resultado.append("var respuesta = new Array(\n");
    
        try {
          
      if (CalloutHelper.commandInCommandList(command, "inpprocessed", "inpprocessed")) {
        if (!isFirst) resultado.append(", \n");
        comboTableData = new ComboTableData(vars, this, "17", "action", "FF8080812E443491012E443C053A001A", "FF808081332719060133271E5BB1001B", Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId")), Utility.getContext(this, vars, "#User_Client", windowId), 0);
        comboTableData.fillParameters(null, windowId, "");
        resultField = "inpaction";

        resultado.append("new Array(\"" + resultField + "\", ");
        resultado.append(generateArray(comboTableData.select(false), vars.getStringParameter(resultField)));
        comboTableData = null;
        resultado.append(")");
        isFirst=false;
      }
    
        } catch (ServletException ex) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        } catch (Exception ex1) {
          OBError myError = Utility.translateError(this, vars, vars.getLanguage(), ex1.toString());
          bdErrorHidden(response, myError.getType(), myError.getTitle(), myError.getMessage());
          return;
        }
    
        resultado.append("\n);");
    
        xmlDocument.setParameter("array", resultado.toString());
        xmlDocument.setParameter("frameName", "mainframe");
        xmlDocument.setParameter("frameName1", "mainframe");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
 
       return;
     }
    
}
