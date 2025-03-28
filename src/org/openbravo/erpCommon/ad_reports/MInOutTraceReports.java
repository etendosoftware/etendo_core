/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2001-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Hashtable;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class MInOutTraceReports extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  private static final BigDecimal ZERO = new BigDecimal(0.0);

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    // Counter maintained as Vector to ensure thread safety
    Vector<Integer> count = new Vector<Integer>();

    count.add(0);
    String strmProductIdGlobal = "";
    String strmAttributesetinstanceIdGlobal = "";
    Hashtable<String, Integer> calculated = new Hashtable<String, Integer>();

    if (log4j.isDebugEnabled()) {
      log4j.debug("MInOutTraceReports doPost, commandIn: " + vars.getCommand());
    }

    if (vars.commandIn("DEFAULT")) {
      strmProductIdGlobal = vars.getGlobalVariable("inpmProductId",
          "MInOutTraceReports|M_Product_Id", "");
      strmAttributesetinstanceIdGlobal = vars.getGlobalVariable("inpmAttributeSetInstanceId",
          "MInOutTraceReports|M_AttributeSetInstance_Id", "");
      String strIn = vars.getGlobalVariable("inpInOut", "MInOutTraceReports|in", "Y");
      printPageDataSheet(response, vars, strIn, strmProductIdGlobal,
          strmAttributesetinstanceIdGlobal, calculated, count);
    } else if (vars.commandIn("FIND")) {
      strmProductIdGlobal = vars.getRequestGlobalVariable("inpmProductId",
          "MInOutTraceReports|M_Product_Id");
      strmAttributesetinstanceIdGlobal = vars.getRequestGlobalVariable("inpmAttributeSetInstanceId",
          "MInOutTraceReports|M_AttributeSetInstance_Id");
      String strIn = StringUtils.isEmpty(vars.getStringParameter("inpInOut")) ? "N"
          : vars.getStringParameter("inpInOut");
      printPageDataSheet(response, vars, strIn, strmProductIdGlobal,
          strmAttributesetinstanceIdGlobal, calculated, count);
    } else if (vars.commandIn("INVERSE")) {
      strmProductIdGlobal = vars.getRequiredStringParameter("inpmProductId2");
      strmAttributesetinstanceIdGlobal = vars
          .getRequiredStringParameter("inpmAttributeSetInstanceId2");
      String strIn = vars.getRequiredStringParameter("inpIn2");
      if (StringUtils.isEmpty(strIn)) {
        strIn = "N";
      }
      vars.setSessionValue("MInOutTraceReports|in", strIn);
      printPageDataSheet(response, vars, strIn, strmProductIdGlobal,
          strmAttributesetinstanceIdGlobal, calculated, count);
    } else {
      pageError(response);
    }
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strIn, String strmProductIdGlobal, String strmAttributesetinstanceIdGlobal,
      Hashtable<String, Integer> calculated, Vector<Integer> count)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("/org/openbravo/erpCommon/ad_reports/MInOutTraceReports")
        .createXmlDocument();
    MInOutTraceReportsData[] data = null;
    calculated.clear();
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    if (StringUtils.isEmpty(strmProductIdGlobal)) {
      data = new MInOutTraceReportsData[0];
    } else {
      data = MInOutTraceReportsData.select(readOnlyCP, vars.getLanguage(), strmProductIdGlobal,
          strmAttributesetinstanceIdGlobal,
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "MInOutTraceReports"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "MInOutTraceReports"));
    }

    xmlDocument.setParameter("calendar", vars.getLanguage());
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("mProduct", strmProductIdGlobal);
    xmlDocument.setParameter("parameterM_ATTRIBUTESETINSTANCE_ID",
        strmAttributesetinstanceIdGlobal);
    xmlDocument.setData("reportM_ATTRIBUTESETINSTANCE_ID", "liststructure",
        AttributeSetInstanceComboData.select(readOnlyCP, vars.getLanguage(), strmProductIdGlobal,
            Utility.getContext(readOnlyCP, vars, "#User_Client", "MInOutTraceReports"),
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "MInOutTraceReports")));
    xmlDocument.setParameter("productDescription",
        MInOutTraceReportsData.selectMproduct(readOnlyCP, strmProductIdGlobal));
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("in", strIn);
    xmlDocument.setParameter("out", strIn);

    xmlDocument.setData("structure1",
        processData(vars, data, strIn, strmProductIdGlobal, calculated, count));
    if (log4j.isDebugEnabled()) {
      log4j.debug("****FIN: "/*
                              * + ((data!=null && data.length>0)?data[0].html:"")
                              */);
    }

    ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(), "MInOutTraceReports", false, "",
        "", "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    try {
      WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
          "org.openbravo.erpCommon.ad_reports.MInOutTraceReports");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(),
          "MInOutTraceReports.html", classInfo.id, classInfo.type, strReplaceWith,
          tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(), "MInOutTraceReports.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("MInOutTraceReports");
      vars.removeMessage("MInOutTraceReports");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private MInOutTraceReportsData[] processData(VariablesSecureApp vars,
      MInOutTraceReportsData[] data, String strIn, String strmProductIdGlobal,
      Hashtable<String, Integer> calculated, Vector<Integer> count) throws ServletException {
    MInOutTraceReportsData[] localData = data;
    if (localData == null || localData.length == 0) {
      return localData;
    }
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    for (int i = 0; i < localData.length; i++) {
      MInOutTraceReportsData[] dataChild = MInOutTraceReportsData.selectChilds(readOnlyCP,
          vars.getLanguage(), localData[i].mAttributesetinstanceId, localData[i].mProductId,
          localData[i].mLocatorId, StringUtils.equals(strIn, "Y") ? "plusQty" : "minusQty",
          StringUtils.equals(strIn, "N") ? "minusQty" : "plusQty");
      if (dataChild == null || dataChild.length == 0) {
        localData[i].htmlHeader = "";
        localData[i].html = "";
      } else {
        localData[i].htmlHeader = createHeader(vars, dataChild);
        localData[i].html = processChilds(vars, dataChild, localData[i].mAttributesetinstanceId,
            localData[i].mProductId, localData[i].mLocatorId, strIn, true, strmProductIdGlobal,
            calculated, count);
      }

      if (StringUtils.isEmpty(localData[i].html)) {
        // Delete localData[i] from array
        localData = (MInOutTraceReportsData[]) ArrayUtils.removeElement(localData, localData[i]);
        i--;
      }
    }
    return localData;
  }

  private String createHeader(VariablesSecureApp vars, MInOutTraceReportsData[] dataChild)
      throws ServletException {
    boolean hasSecUom = false;
    for (int i = 0; i < dataChild.length; i++) {
      if (StringUtils.isNotEmpty(dataChild[i].quantityorder)) {
        hasSecUom = true;
        break;
      }
    }
    StringBuffer strHtmlHeader = new StringBuffer();
    strHtmlHeader.append(insertHeaderHtml(false, "0"));
    strHtmlHeader.append("<tr style=\"background: #CFDDE8\">");
    strHtmlHeader.append("<td >\n");
    strHtmlHeader.append(
        "<table class=\"DataGrid_Header_Table\" border=\"0\" cellspacing=0 cellpadding=0 width=\"100%\">");
    strHtmlHeader.append("  <tr>");
    strHtmlHeader.append("    <th class=\"DataGrid_Header_Cell\" width=\"70\">Date</th>");
    strHtmlHeader.append("    <th class=\"DataGrid_Header_Cell\" width=\"100\">Movement Type</th>");
    strHtmlHeader.append("    <th class=\"DataGrid_Header_Cell\" width=\"100\">Locator</th>");
    strHtmlHeader.append("    <th class=\"DataGrid_Header_Cell\" width=\"90\">Movement Qty</th>");
    if (hasSecUom) {
      strHtmlHeader.append("    <th class=\"DataGrid_Header_Cell\" width=\"90\">Order Qty</th>\n");
    }
    strHtmlHeader.append("    <th class=\"DataGrid_Header_Cell\">Transaction Reference</th>");
    strHtmlHeader.append("</tr></table>");
    strHtmlHeader.append("</td>");
    strHtmlHeader.append("<th class=\"DataGrid_Header_Cell\">Total Qty\n");
    strHtmlHeader.append("</th>\n");
    strHtmlHeader.append("  </tr>");
    strHtmlHeader.append(insertHeaderHtml(true, ""));
    return strHtmlHeader.toString();
  }

  private String insertTabHtml(boolean border) {
    return "    <td width=\"20px\" class=\"DataGrid_Body_Cell\""
        + (border ? " style=\"border-bottom: 0px !important;\""
            : " style=\"border-top: 0px !important;\"")
        + ">&nbsp;</td>\n";
  }

  private String insertHeaderHtml(boolean isClose, String border) {
    if (!isClose) {
      return "<table border=\"" + border + "\" cellspacing=0 cellpadding=0 width=\"100%\" >\n";
    } else {
      return "</table>\n";
    }
  }

  private String insertTotal(String strTotal, String strUnit, String strTotalPedido,
      String strUnitPedido) {
    BigDecimal total, totalPedido;
    total = new BigDecimal(strTotal);
    totalPedido = (StringUtils.isNotEmpty(strTotalPedido) ? new BigDecimal(strTotalPedido) : ZERO);
    total = total.setScale(2, RoundingMode.HALF_UP);
    totalPedido = totalPedido.setScale(2, RoundingMode.HALF_UP);
    StringBuffer resultado = new StringBuffer();
    resultado.append("<td class=\"DataGrid_Body_Cell_Amount\">\n");
    resultado.append(total.toString()).append(" ").append(strUnit);
    resultado.append("</td>\n");
    if (totalPedido.intValue() != 0) {
      resultado.append("<td class=\"DataGrid_Body_Cell_Amount\">\n");
      resultado.append(totalPedido.toString())
          .append(" ")
          .append(StringEscapeUtils.escapeHtml4(strUnitPedido));
      resultado.append("</td>\n");
    }
    return resultado.toString();
  }

  private String processChilds(VariablesSecureApp vars, MInOutTraceReportsData[] dataChild,
      String mAttributesetinstanceId, String mProductId, String mLocatorId, String strIn,
      boolean colorbg2, String strmProductIdGlobal, Hashtable<String, Integer> calculated,
      Vector<Integer> count) throws ServletException {
    BigDecimal total = BigDecimal.ZERO;
    BigDecimal totalPedido = BigDecimal.ZERO;
    StringBuffer strHtml = new StringBuffer();

    String strCalculated = mProductId + "&" + mAttributesetinstanceId + "&" + mLocatorId;

    int c = count.get(0).intValue();
    c += 1;
    count.removeAllElements();
    count.add(c);
    calculated.put(strCalculated, c);

    if (log4j.isDebugEnabled()) {
      log4j.debug("****** Hashtable.add: " + strCalculated);
    }

    boolean colorbg = true;

    strHtml.append(insertHeaderHtml(false, "0"));
    for (int i = 0; i < dataChild.length; i++) {

      strHtml.append("<tr style=\"background: ")
          .append((colorbg ? "#CFDDE8" : "#FFFFFF"))
          .append("\">");
      colorbg = !colorbg;

      strHtml.append("<td >\n");
      strHtml.append(getData(dataChild[i], ""));
      strHtml.append("</td>");
      total = total.add(new BigDecimal(dataChild[i].movementqty));
      if (StringUtils.isNotEmpty(dataChild[i].quantityorder)) {
        totalPedido = totalPedido.add(new BigDecimal(dataChild[i].quantityorder));
      }

      strHtml.append(insertTotal(total.toPlainString(), dataChild[i].uomName,
          totalPedido.toPlainString(), dataChild[i].productUomName));
      strHtml.append("  </tr>\n");
      if (log4j.isDebugEnabled()) {
        log4j.debug("****** New line, qty: " + dataChild[i].movementqty + " "
            + getData(dataChild[i], "TraceSubTable"));
      }
      strHtml.append(processExternalChilds(vars, dataChild[i], strIn, colorbg2, strmProductIdGlobal,
          calculated, count));
    }
    strHtml.append(insertHeaderHtml(true, ""));
    return strHtml.toString();
  }

  private String processExternalChilds(VariablesSecureApp vars, MInOutTraceReportsData dataChild,
      String strIn, boolean colorbg, String strmProductIdGlobal,
      Hashtable<String, Integer> calculated, Vector<Integer> count) throws ServletException {
    StringBuffer strHtml = new StringBuffer();
    BigDecimal movementQty = new BigDecimal(dataChild.movementqty);
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    // if (log4j.isDebugEnabled()) log4j.debug("****PROCESSING EXTERNAL 1: "
    // + movementQty.toString() + " and strIn: " + strIn);
    if (StringUtils.equals(strIn, "Y")) {
      movementQty = movementQty.negate();
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("****PROCESSING EXTERNAL 2: " + movementQty.toString() + " and movementType:"
          + dataChild.movementtype);
    }

    if (dataChild.movementtype.startsWith("P") && (movementQty.compareTo(BigDecimal.ZERO) > 0)) {
      String strNewId = dataChild.mProductionlineId;
      MInOutTraceReportsData[] dataProduction;
      if (log4j.isDebugEnabled()) {
        log4j.debug("****PROCESSING PRODUCTIONLINE: " + strNewId + " " + strIn);
      }
      if (StringUtils.equals(strIn, "Y")) {
        dataProduction = MInOutTraceReportsData.selectProductionOut(readOnlyCP, vars.getLanguage(),
            strNewId);
      } else {
        dataProduction = MInOutTraceReportsData.selectProductionIn(readOnlyCP, vars.getLanguage(),
            strNewId);
      }
      if (dataProduction != null && dataProduction.length > 0) {
        strHtml.append("  <tr>\n");
        strHtml.append("    <td colspan=\"3\">\n");
        strHtml.append(insertHeaderHtml(false, "0"));
        for (int j = 0; j < dataProduction.length; j++) {
          strHtml.append("  <tr style=\"background: ")
              .append((colorbg ? "#CCCCCC" : "#AAAAAA"))
              .append("\">\n");
          strHtml.append(insertTabHtml(true));
          strHtml.append("    <td >\n");

          String resultado2 = "";
          strHtml.append("<table border=\"0\" cellspacing=0 cellpadding=0 width=\"100%\">\n");
          strHtml.append("  <tr>\n");
          strHtml.append("    <td class=\"DataGrid_Body_Cell\" width=\"70\">")
              .append(dataProduction[j].movementdate)
              .append("</td>\n");
          strHtml.append("    <td class=\"DataGrid_Body_Cell\" width=\"100\">")
              .append(StringEscapeUtils.escapeHtml4(dataProduction[j].movementtypeName))
              .append("</td>\n");
          strHtml.append("    <td class=\"DataGrid_Body_Cell\" width=\"100\">")
              .append(dataProduction[j].locatorName)
              .append("</td>\n");
          strHtml.append("    <td class=\"DataGrid_Body_Cell_Amount\" width=\"90\">")
              .append(dataProduction[j].movementqty)
              .append("&nbsp;")
              .append(StringEscapeUtils.escapeHtml4(dataProduction[j].uomName))
              .append("</td>\n");
          strHtml.append("    <td class=\"DataGrid_Body_Cell\" width=\"90\">")
              .append(dataProduction[j].quantityorder)
              .append("&nbsp;")
              .append(StringEscapeUtils.escapeHtml4(dataProduction[j].productUomName))
              .append("</td>\n");
          resultado2 = dataProduction[j].productName;
          strHtml.append(
              "    <td class=\"DataGrid_Body_Cell\"><a href=\"#\" onclick=\"submitCommandForm('INVERSE', true, null, 'MInOutTraceReports.html?inpmProductId2="
                  + dataProduction[j].mProductId + "&inpmAttributeSetInstanceId2="
                  + dataProduction[j].mAttributesetinstanceId + "&inpIn2="
                  + (StringUtils.equals(strIn, "Y") ? "N" : "Y")
                  + "', '_self');return true;\" class=\"LabelLink\">");
          if (StringUtils.isNotEmpty(resultado2)) {
            strHtml.append(StringEscapeUtils.escapeHtml4(resultado2));
          }
          strHtml.append("&nbsp;</a></td>\n");
          resultado2 = dataProduction[j].attributeName;
          strHtml.append("    <td class=\"DataGrid_Body_Cell\" width=\"120\">");
          if (StringUtils.isNotEmpty(resultado2)) {
            strHtml.append(StringEscapeUtils.escapeHtml4(resultado2));
          }
          strHtml.append("&nbsp;</td>\n");
          strHtml.append("</tr></table>");

          strHtml.append("  </td></tr>\n");
          if (!StringUtils.equals(dataProduction[j].mAttributesetinstanceId, "0")) {
            String strCalculate = dataProduction[j].mProductId + "&"
                + dataProduction[j].mAttributesetinstanceId + "&" + dataProduction[j].mLocatorId;
            if (log4j.isDebugEnabled()) {
              log4j.debug("******** Hashtable.production: " + strCalculate);
              log4j.debug(
                  "******** Production, hashtable calculated: " + calculated.get(strCalculate));
            }
            Integer isnull = calculated.get(strCalculate);
            if (isnull == null
                && !StringUtils.equals(dataProduction[j].mAttributesetinstanceId, "0")) {
              // If there is not attribute Set Instance ID, do not explode the children for this
              // report. It is meant to explode children of Products with attribute in this scenario
              MInOutTraceReportsData[] data = MInOutTraceReportsData.selectChilds(readOnlyCP,
                  vars.getLanguage(), dataProduction[j].mAttributesetinstanceId,
                  dataProduction[j].mProductId, dataProduction[j].mLocatorId,
                  StringUtils.equals(strIn, "Y") ? "plusQty" : "minusQty",
                  StringUtils.equals(strIn, "N") ? "minusQty" : "plusQty");
              String strPartial = data == null || data.length == 0 ? ""
                  : processChilds(vars, data, dataProduction[j].mAttributesetinstanceId,
                      dataProduction[j].mProductId, dataProduction[j].mLocatorId, strIn, !colorbg,
                      strmProductIdGlobal, calculated, count);
              if (StringUtils.isNotEmpty(strPartial)) {
                strHtml.append("  <tr style=\"background: ")
                    .append((colorbg ? "#CCCCCC" : "#AAAAAA"))
                    .append("\">\n");
                strHtml.append(insertTabHtml(false));
                strHtml.append("    <td>\n");
                strHtml.append(strPartial);
                strHtml.append("    </td>\n");
                strHtml.append("  </tr>\n");
              }
            }
          }
        }
        strHtml.append(insertHeaderHtml(true, ""));
        strHtml.append("</td></tr>\n");
      }
    }

    if (dataChild.movementtype.startsWith("M") && (movementQty.compareTo(BigDecimal.ZERO) > 0)) {
      String strNewId = dataChild.mMovementlineId;
      MInOutTraceReportsData[] dataMovement;
      if (log4j.isDebugEnabled()) {
        log4j.debug("****PROCESSING MOVEMENTLINE: " + strNewId + " " + strIn);
      }
      dataMovement = MInOutTraceReportsData.selectMovement(readOnlyCP, vars.getLanguage(),
          StringUtils.equals(strIn, "Y") ? "M+" : "M-", strNewId);
      if (dataMovement != null && dataMovement.length > 0) {
        strHtml.append("  <tr>\n");
        strHtml.append("    <td colspan=\"3\">\n");
        strHtml.append(insertHeaderHtml(false, "1"));
        for (int j = 0; j < dataMovement.length; j++) {
          strHtml.append("  <tr style=\"background: ")
              .append((colorbg ? "#CCCCCC" : "#AAAAAA"))
              .append("\">\n");
          strHtml.append(insertTabHtml(true));
          strHtml.append("    <td >\n");

          String resultado2 = "";
          strHtml.append("<table border=\"0\" cellspacing=0 cellpadding=0 width=\"100%\">\n");
          strHtml.append("  <tr>\n");
          strHtml.append("    <td class=\"DataGrid_Body_Cell\" width=\"70\">")
              .append(dataMovement[j].movementdate)
              .append("</td>\n");
          strHtml.append("    <td class=\"DataGrid_Body_Cell\" width=\"100\">")
              .append(dataMovement[j].movementtypeName)
              .append("</td>\n");
          strHtml.append("    <td class=\"DataGrid_Body_Cell\" width=\"100\">")
              .append(dataMovement[j].locatorName)
              .append("</td>\n");
          strHtml.append("    <td class=\"DataGrid_Body_Cell_Amount\" width=\"90\">")
              .append(dataMovement[j].movementqty)
              .append("&nbsp;")
              .append(dataMovement[j].uomName)
              .append("</td>\n");
          strHtml.append("    <td class=\"DataGrid_Body_Cell\" width=\"90\">")
              .append(dataMovement[j].quantityorder)
              .append("&nbsp;")
              .append(dataMovement[j].productUomName)
              .append("</td>\n");
          resultado2 = dataMovement[j].productName;
          strHtml.append("    <td class=\"DataGrid_Body_Cell\">");
          if (StringUtils.isNotEmpty(resultado2)) {
            strHtml.append(resultado2);
          }
          strHtml.append("&nbsp;</td>\n");
          resultado2 = dataMovement[j].attributeName;
          strHtml.append("    <td class=\"DataGrid_Body_Cell\" width=\"120\">");
          if (StringUtils.isNotEmpty(resultado2)) {
            strHtml.append(resultado2);
          }
          strHtml.append("&nbsp;</td>\n");
          strHtml.append("</tr></table>");

          // strHtml.append(getData(dataProduction[j], "Bordes"));
          strHtml.append("  </td></tr>\n");
          if (!StringUtils.equals(dataMovement[j].mAttributesetinstanceId, "0")) {
            String strPartial = "";
            if (!StringUtils.equals(dataMovement[j].mProductId, strmProductIdGlobal)) {
              if (log4j.isDebugEnabled()) {
                log4j.debug("******** hashtable.production: Prod: " + dataMovement[j].mProductId
                    + " Attr " + dataMovement[j].mAttributesetinstanceId + " Loc: "
                    + dataMovement[j].mLocatorId);
              }
              String strCalculate = dataMovement[j].mProductId + "&"
                  + dataMovement[j].mAttributesetinstanceId + "&" + dataMovement[j].mLocatorId;
              if (log4j.isDebugEnabled()) {
                log4j.debug(
                    "******** Movement, hashtable calculated: " + calculated.get(strCalculate));
              }
              if (calculated.get(strCalculate) == null
                  && !StringUtils.equals(dataMovement[j].mAttributesetinstanceId, "0")) {
                // If there is not attribute Set Instance ID, do not explode the children for this
                // report. It is meant to explode children of Products with attribute in this
                // scenario
                MInOutTraceReportsData[] data = MInOutTraceReportsData.selectChilds(readOnlyCP,
                    vars.getLanguage(), dataMovement[j].mAttributesetinstanceId,
                    dataMovement[j].mProductId, dataMovement[j].mLocatorId,
                    StringUtils.equals(strIn, "Y") ? "plusQty" : "minusQty",
                    StringUtils.equals(strIn, "N") ? "minusQty" : "plusQty");
                strPartial = data == null || data.length == 0 ? ""
                    : processChilds(vars, data, dataMovement[j].mAttributesetinstanceId,
                        dataMovement[j].mProductId, dataMovement[j].mLocatorId, strIn, !colorbg,
                        strmProductIdGlobal, calculated, count);
              }
            }
            if (StringUtils.isNotEmpty(strPartial)) {
              strHtml.append("  <tr style=\"background: ")
                  .append((colorbg ? "#CCCCCC" : "#AAAAAA"))
                  .append("\">\n");
              strHtml.append(insertTabHtml(false));
              strHtml.append("    <td>\n");
              strHtml.append(strPartial);
              strHtml.append("    </td>\n");
              strHtml.append("  </tr>\n");
            }
          }
        }
        strHtml.append(insertHeaderHtml(true, ""));
        strHtml.append("</td></tr>\n");
      }
    }
    return strHtml.toString();
  }

  private String getData(MInOutTraceReportsData data, String strClassName) throws ServletException {
    StringBuffer resultado = new StringBuffer();
    String resultado2 = "";
    resultado.append("<table border=\"0\" cellspacing=0 cellpadding=0 width=\"100%\" class=\"")
        .append(strClassName)
        .append("\">\n");
    resultado.append("  <tr>\n");
    resultado.append("    <td class=\"DataGrid_Body_Cell\" width=\"70\">")
        .append(data.movementdate)
        .append("</td>\n");
    resultado.append("    <td class=\"DataGrid_Body_Cell\" width=\"100\">")
        .append(data.movementtypeName)
        .append("</td>\n");
    resultado.append("    <td class=\"DataGrid_Body_Cell\" width=\"100\">")
        .append(data.locatorName)
        .append("</td>\n");
    resultado.append("    <td class=\"DataGrid_Body_Cell_Amount\" width=\"90\">")
        .append(data.movementqty)
        .append("&nbsp;")
        .append(StringEscapeUtils.escapeHtml4(data.uomName))
        .append("</td>\n");
    if (StringUtils.isNotEmpty(data.quantityorder)) {
      resultado.append("    <td class=\"DataGrid_Body_Cell\" width=\"90\">")
          .append(data.quantityorder)
          .append("&nbsp;")
          .append(StringEscapeUtils.escapeHtml4(data.productUomName))
          .append("</td>\n");
    }
    if (StringUtils.equalsIgnoreCase(data.movementtype, "W+")) {
      // resultado2 = data.productionName;
    } else if (StringUtils.equalsIgnoreCase(data.movementtype, "W-")) {
      // resultado2 = data.productionName;
    } else if (StringUtils.equalsIgnoreCase(data.movementtype, "C+")) {
      resultado2 = data.vendorName;
    } else if (StringUtils.equalsIgnoreCase(data.movementtype, "C-")) {
      resultado2 = data.vendorName;
    } else if (StringUtils.equalsIgnoreCase(data.movementtype, "V+")) {
      resultado2 = data.vendorName;
    } else if (StringUtils.equalsIgnoreCase(data.movementtype, "V-")) {
      resultado2 = data.vendorName;
    } else if (StringUtils.equalsIgnoreCase(data.movementtype, "I+")) {
      resultado2 = data.inventoryName;
    } else if (StringUtils.equalsIgnoreCase(data.movementtype, "I-")) {
      resultado2 = data.inventoryName;
    } else if (StringUtils.equalsIgnoreCase(data.movementtype, "M+")) {
      resultado2 = data.movementName;
    } else if (StringUtils.equalsIgnoreCase(data.movementtype, "M-")) {
      resultado2 = data.movementName;
    } else if (StringUtils.equalsIgnoreCase(data.movementtype, "P+")) {
      resultado2 = data.productionName;
    } else if (StringUtils.equalsIgnoreCase(data.movementtype, "P-")) {
      resultado2 = data.productionName;
    } else {
      resultado2 = data.name;
    }

    resultado.append("    <td class=\"DataGrid_Body_Cell\">");
    if (StringUtils.isNotEmpty(resultado2)) {
      resultado.append(StringEscapeUtils.escapeHtml4(resultado2));
    }
    resultado.append("&nbsp;</td>\n");
    resultado.append("</tr></table>");
    return resultado.toString();
  }

  @Override
  public String getServletInfo() {
    return "Servlet MInOutTraceReports. This Servlet was made by Fernando Iriazabal";
  } // end of getServletInfo() method
}
