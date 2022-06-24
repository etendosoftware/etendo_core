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
 * All portions are Copyright (C) 2014-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.info;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.openbravo.base.filter.RequestFilter;
import org.openbravo.base.filter.ValueListFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.SQLReturnObject;
import org.openbravo.erpCommon.utility.TableSQLData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

public class LocatorMultiple extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  private static final String[] colNames = { "Organization", "Warehouse", "value", "aisle", "bin",
      "nivel", "M_Locator_ID", "RowKey" };
  private static final RequestFilter columnFilter = new ValueListFilter(colNames);
  private static final RequestFilter directionFilter = new ValueListFilter("asc", "desc");

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      removePageSessionVariables(vars);
      String strName = vars.getRequestGlobalVariable("inpKey", "LocatorMultiple.name");
      strName = strName + "%";
      vars.setSessionValue("LocatorMultiple.name", strName);
      String strWarehouseId = "";
      vars.setSessionValue("LocatorMultiple.warehouseid", strWarehouseId);
      String strOrg = vars.getGlobalVariable("inpadOrgId", "LocatorMultiple.adorgid", "");
      if ("".equals(strOrg) || strOrg == null) {
        strOrg = vars.getStringParameter("paramOrgTree");
      }
      printPage(response, vars, strName, strWarehouseId, strOrg);
    } else if (vars.commandIn("STRUCTURE")) {
      printGridStructure(response, vars);
    } else if (vars.commandIn("DATA")) {
      if (vars.getStringParameter("newFilter").equals("1")) {
        removePageSessionVariables(vars);
      }
      String strName = vars.getGlobalVariable("inpKey", "LocatorMultiple.name", "");
      String strWarehouseId = vars.getGlobalVariable("inpmWarehouseId",
          "LocatorMultiple.warehouseid", "");
      String strAisle = vars.getGlobalVariable("inpAisle", "LocatorMultiple.aisle", "");
      String strBin = vars.getGlobalVariable("inpBin", "LocatorMultiple.bin", "");
      String strLevel = vars.getGlobalVariable("inpLevel", "LocatorMultiple.level", "");
      String strNewFilter = vars.getStringParameter("newFilter");
      String strOffset = vars.getStringParameter("offset");
      String strPageSize = vars.getStringParameter("page_size");
      String strSortCols = vars.getInStringParameter("sort_cols", columnFilter);
      String strSortDirs = vars.getInStringParameter("sort_dirs", directionFilter);
      String strOrg = vars.getGlobalVariable("inpadOrgId", "LocatorMultiple.adorgid", "");
      if ("".equals(strOrg) || strOrg == null) {
        strOrg = vars.getStringParameter("paramOrgTree");
      }
      printGridData(response, vars, strName, strWarehouseId, strAisle, strBin, strLevel,
          strSortCols, strSortDirs, strOffset, strPageSize, strNewFilter, strOrg);
    } else {
      pageError(response);
    }
  }

  private void removePageSessionVariables(VariablesSecureApp vars) {
    vars.removeSessionValue("LocatorMultiple.name");
    vars.removeSessionValue("LocatorMultiple.warehouseid");
    vars.removeSessionValue("LocatorMultiple.aisle");
    vars.removeSessionValue("LocatorMultiple.bin");
    vars.removeSessionValue("LocatorMultiple.level");
    vars.removeSessionValue("LocatorMultiple.adorgid");
    vars.removeSessionValue("LocatorMultiple.currentPage");
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strNameValue,
      String strWarehouseId, String strOrg) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: Multiple locators seeker Frame Set");
    }
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/info/LocatorMultiple")
        .createXmlDocument();
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("alertMsg",
        "ALERT_MSG=\"" + Utility.messageBD(this, "NoLocatorSelected", vars.getLanguage()) + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());
    if (strNameValue.equals("")) {
      xmlDocument.setParameter("key", "%");
    } else {
      xmlDocument.setParameter("key", strNameValue);
    }
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLE", "M_Warehouse_ID",
          "M_Warehouse of Client", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
          Utility.getContext(this, vars, "#User_Client", ""), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "LocatorMultiple",
          vars.getSessionValue("LocatorMultiple.warehouseid", ""));
      xmlDocument.setData("reportM_WAREHOUSEID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    xmlDocument.setParameter("OrgTree", strOrg);
    xmlDocument.setParameter("grid", "20");
    xmlDocument.setParameter("grid_Offset", "");
    xmlDocument.setParameter("grid_SortCols", "1");
    xmlDocument.setParameter("grid_SortDirs", "ASC");
    xmlDocument.setParameter("grid_Default", "0");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printGridStructure(HttpServletResponse response, VariablesSecureApp vars)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: print page structure");
    }
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/utility/DataGridStructure")
        .createXmlDocument();

    SQLReturnObject[] data = getHeaders(vars);
    String type = "Hidden";
    String title = "";
    String description = "";

    xmlDocument.setParameter("type", type);
    xmlDocument.setParameter("title", title);
    xmlDocument.setParameter("description", description);
    xmlDocument.setData("structure1", data);
    xmlDocument.setParameter("backendPageSize", String.valueOf(TableSQLData.maxRowsPerGridPage));
    response.setContentType("text/xml; charset=UTF-8");
    response.setHeader("Cache-Control", "no-cache");
    PrintWriter out = response.getWriter();
    if (log4j.isDebugEnabled()) {
      log4j.debug(xmlDocument.print());
    }
    out.println(xmlDocument.print());
    out.close();
  }

  private SQLReturnObject[] getHeaders(VariablesSecureApp vars) {
    SQLReturnObject[] data = null;
    Vector<SQLReturnObject> vAux = new Vector<SQLReturnObject>();
    String[] colWidths = { "185", "185", "185", "68", "68", "68", "0", "0" };
    for (int i = 0; i < colNames.length; i++) {
      SQLReturnObject dataAux = new SQLReturnObject();
      dataAux.setData("columnname", colNames[i]);
      dataAux.setData("gridcolumnname", colNames[i]);
      dataAux.setData("adReferenceId", "AD_Reference_ID");
      dataAux.setData("adReferenceValueId", "AD_ReferenceValue_ID");
      dataAux.setData("isidentifier", (colNames[i].equals("rowkey") ? "true" : "false"));
      dataAux.setData("iskey", (colNames[i].equals("RowKey") ? "true" : "false"));
      dataAux.setData("isvisible",
          (colNames[i].equals("M_Locator_ID") || colNames[i].equalsIgnoreCase("RowKey") ? "false"
              : "true"));
      String name = Utility.messageBD(this, "LS_" + colNames[i].toUpperCase(), vars.getLanguage());
      dataAux.setData("name", (name.startsWith("LS_") ? colNames[i] : name));
      dataAux.setData("type", "string");
      dataAux.setData("width", colWidths[i]);
      vAux.addElement(dataAux);
    }
    data = new SQLReturnObject[vAux.size()];
    vAux.copyInto(data);
    return data;
  }

  private void printGridData(HttpServletResponse response, VariablesSecureApp vars, String strName,
      String strWarehouseId, String strAisle, String strBin, String strLevel, String strOrderCols,
      String strOrderDirs, String strOffset, String strPageSize, String strNewFilter, String strOrg)
      throws IOException, ServletException {
    String localStrNewFilter = strNewFilter;
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: print page rows");
    }
    int page = 0;
    SQLReturnObject[] headers = getHeaders(vars);
    FieldProvider[] data = null;
    String type = "Hidden";
    String title = "";
    String description = "";
    String strNumRows = "0";
    int offset = Integer.valueOf(strOffset).intValue();
    int pageSize = Integer.valueOf(strPageSize).intValue();

    if (headers != null) {
      try {
        // build sql orderBy clause
        String strOrderBy = SelectorUtility.buildOrderByClause(strOrderCols, strOrderDirs);
        page = TableSQLData.calcAndGetBackendPage(vars, "LocatorMultiple.currentPage");
        if (vars.getStringParameter("movePage", "").length() > 0) {
          // on movePage action force executing countRows again
          localStrNewFilter = "";
        }
        int oldOffset = offset;
        offset = (page * TableSQLData.maxRowsPerGridPage) + offset;
        log4j.debug("relativeOffset: " + oldOffset + " absoluteOffset: " + offset);
        if (localStrNewFilter.equals("1") || localStrNewFilter.equals("")) {
          // New filter or first load
          String rownum = "0", oraLimit1 = null, oraLimit2 = null, pgLimit = null;
          if (this.myPool.getRDBMS().equalsIgnoreCase("ORACLE")) {
            oraLimit1 = String.valueOf(offset + TableSQLData.maxRowsPerGridPage);
            oraLimit2 = (offset + 1) + " AND " + oraLimit1;
            rownum = "ROWNUM";
          } else {
            pgLimit = TableSQLData.maxRowsPerGridPage + " OFFSET " + offset;
          }
          strNumRows = LocatorMultipleData.countRows(this, rownum,
              Utility.getContext(this, vars, "#User_Client", "Locator"),
              Utility.getSelectorOrgs(this, vars, strOrg), strName, strWarehouseId, strAisle,
              strBin, strLevel, pgLimit, oraLimit1, oraLimit2);
          vars.setSessionValue("LocatorMultiple.numrows", strNumRows);
        } else {
          strNumRows = vars.getSessionValue("LocatorMultiple.numrows");
        }

        // Filtering result
        if (this.myPool.getRDBMS().equalsIgnoreCase("ORACLE")) {
          String oraLimit = (offset + 1) + " AND " + String.valueOf(offset + pageSize);
          data = LocatorMultipleData.select(this, "ROWNUM",
              Utility.getContext(this, vars, "#User_Client", "Locator"),
              Utility.getSelectorOrgs(this, vars, strOrg), strName, strWarehouseId, strAisle,
              strBin, strLevel, strOrderBy, oraLimit, "");
        } else {
          String pgLimit = pageSize + " OFFSET " + offset;
          data = LocatorMultipleData.select(this, "1",
              Utility.getContext(this, vars, "#User_Client", "Locator"),
              Utility.getSelectorOrgs(this, vars, strOrg), strName, strWarehouseId, strAisle,
              strBin, strLevel, strOrderBy, "", pgLimit);
        }
      } catch (ServletException e) {
        log4j.error("Error in print page data: " + e);
        e.printStackTrace();
        OBError myError = Utility.translateError(this, vars, vars.getLanguage(), e.getMessage());
        if (!myError.isConnectionAvailable()) {
          bdErrorAjax(response, "Error", "Connection Error", "No database connection");
          return;
        } else {
          type = myError.getType();
          title = myError.getTitle();
          if (!myError.getMessage().startsWith("<![CDATA[")) {
            description = "<![CDATA[" + myError.getMessage() + "]]>";
          } else {
            description = myError.getMessage();
          }
        }
      } catch (Exception e) {
        if (log4j.isDebugEnabled()) {
          log4j.debug("Error obtaining rows data");
        }
        type = "Error";
        title = "Error";
        if (e.getMessage().startsWith("<![CDATA[")) {
          description = "<![CDATA[" + e.getMessage() + "]]>";
        } else {
          description = e.getMessage();
        }
        e.printStackTrace();
      }
    }

    if (!type.startsWith("<![CDATA[")) {
      type = "<![CDATA[" + type + "]]>";
    }
    if (!title.startsWith("<![CDATA[")) {
      title = "<![CDATA[" + title + "]]>";
    }
    if (!description.startsWith("<![CDATA[")) {
      description = "<![CDATA[" + description + "]]>";
    }
    StringBuffer strRowsData = new StringBuffer();
    strRowsData.append("<xml-data>\n");
    strRowsData.append("  <status>\n");
    strRowsData.append("    <type>").append(type).append("</type>\n");
    strRowsData.append("    <title>").append(title).append("</title>\n");
    strRowsData.append("    <description>").append(description).append("</description>\n");
    strRowsData.append("  </status>\n");
    strRowsData.append("  <rows numRows=\"")
        .append(strNumRows)
        .append("\" backendPage=\"" + page + "\">\n");
    if (data != null && data.length > 0) {
      for (int j = 0; j < data.length; j++) {
        strRowsData.append("    <tr>\n");
        for (int k = 0; k < headers.length; k++) {
          strRowsData.append("      <td><![CDATA[");
          String columnname = headers[k].getField("columnname");

          if ((data[j].getField(columnname)) != null) {
            if (headers[k].getField("adReferenceId").equals("32")) {
              strRowsData.append(strReplaceWith).append("/images/");
            }
            strRowsData.append(StringEscapeUtils.escapeHtml(data[j].getField(columnname)));
          } else {
            if (headers[k].getField("adReferenceId").equals("32")) {
              strRowsData.append(strReplaceWith).append("/images/blank.gif");
            } else {
              strRowsData.append(StringEscapeUtils.escapeHtml("&nbsp;"));
            }
          }
          strRowsData.append("]]></td>\n");
        }
        strRowsData.append("    </tr>\n");
      }
    }
    strRowsData.append("  </rows>\n");
    strRowsData.append("</xml-data>\n");

    response.setContentType("text/xml; charset=UTF-8");
    response.setHeader("Cache-Control", "no-cache");
    PrintWriter out = response.getWriter();
    if (log4j.isDebugEnabled()) {
      log4j.debug(strRowsData.toString());
    }
    out.print(strRowsData.toString());
    out.close();
  }

  @Override
  public String getServletInfo() {
    return "Servlet that presents the multiple locators seeker";
  } // end of getServletInfo() method

}
