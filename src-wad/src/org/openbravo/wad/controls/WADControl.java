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
 * All portions are Copyright (C) 2001-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.wad.controls;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;
import org.openbravo.xmlEngine.XmlEngine;

public class WADControl {
  private Hashtable<String, String> data = new Hashtable<String, String>();
  private Vector<String[]> css = new Vector<String[]>();
  private Vector<String[]> imports = new Vector<String[]>();
  private Vector<String[]> jsCode = new Vector<String[]>();
  XmlEngine xmlEngine;
  private String validation = "";
  private String onload = "";
  protected static ConnectionProvider conn = null;
  protected static String sqlDateFormat;
  protected String reference;
  protected String subreference;

  private WADLabelControl label;

  public WADControl() {
  }

  public void setData(String name, String value) {
    if (name == null) {
      return;
    }
    if (this.data == null) {
      this.data = new Hashtable<String, String>();
    }
    if (value == null || value.equals("")) {
      this.data.remove(name.toUpperCase());
    } else {
      this.data.put(name.toUpperCase(), value);
    }
  }

  public String getData(String name) {
    String aux = data.get(name.toUpperCase());
    if (aux == null) {
      aux = "";
    }
    return aux;
  }

  public void setInfo(Properties prop) {
    if (prop == null) {
      return;
    }
    for (Enumeration<?> e = prop.propertyNames(); e.hasMoreElements();) {
      String _name = (String) e.nextElement();
      setData(_name, prop.getProperty(_name));
    }
  }

  public static void setConnection(ConnectionProvider _conn) {
    conn = _conn;
  }

  public ConnectionProvider getConnection() {
    return conn;
  }

  public void setReportEngine(XmlEngine _xmlEngine) {
    this.xmlEngine = _xmlEngine;
  }

  public XmlEngine getReportEngine() {
    return this.xmlEngine;
  }

  public void initialize() {
    generateJSCode();
  }

  public void addCSSImport(String name, String _data) {
    if (css == null) {
      css = new Vector<String[]>();
    }
    String[] aux = new String[2];
    aux[0] = name;
    aux[1] = _data;
    css.addElement(aux);
  }

  public void addImport(String name, String _data) {
    if (imports == null) {
      imports = new Vector<String[]>();
    }
    String[] aux = new String[2];
    aux[0] = name;
    aux[1] = _data;
    imports.addElement(aux);
  }

  public void addJSCode(String name, String _code) {
    if (jsCode == null) {
      jsCode = new Vector<String[]>();
    }
    String[] aux = new String[2];
    aux[0] = name;
    aux[1] = _code;
    jsCode.addElement(aux);
  }

  public void setValidation(String _code) {
    validation = _code;
  }

  public String getValidation() {
    return validation;
  }

  public void setOnLoad(String _code) {
    onload = _code;
  }

  public String getOnLoad() {
    return onload;
  }

  public Vector<String[]> getJSCode() {
    return jsCode;
  }

  public Vector<String[]> getImport() {
    return imports;
  }

  public Vector<String[]> getCSSImport() {
    return css;
  }

  protected String replaceHTML(String _text) {
    String text = _text;
    text = text.replace("<HTML>", "");
    text = text.replace("<HEAD>", "");
    text = text.replace("<BODY>", "");
    text = text.replace("</BODY>", "");
    text = text.replace("</HTML>", "");
    text = text.replace("</HEAD>", "");
    text = text.replace("<html>", "");
    text = text.replace("<head>", "");
    text = text.replace("<body>", "");
    text = text.replace("</body>", "");
    text = text.replace("</html>", "");
    text = text.replace("</head>", "");
    return text;
  }

  private void generateJSCode() {
    addImport("ValidationTextBox", "../../../../../web/js/default/ValidationTextBox.js");
    if (getData("IsMandatory").equals("Y")) {
      XmlDocument xmlDocument = getReportEngine()
          .readXmlTemplate("org/openbravo/wad/controls/WADControlJSValidation")
          .createXmlDocument();
      xmlDocument.setParameter("columnNameInp", getData("ColumnNameInp"));
      setValidation(replaceHTML(xmlDocument.print()));
    }
    setCalloutJS();
  }

  public void setCalloutJS() {
    String callout = getData("CallOutName");
    if (callout != null && !callout.equals("")) {
      XmlDocument xmlDocument = getReportEngine()
          .readXmlTemplate("org/openbravo/wad/controls/WADControlJS")
          .createXmlDocument();
      xmlDocument.setParameter("calloutName", callout);
      xmlDocument.setParameter("calloutMapping", getData("CallOutMapping"));
      addJSCode("callout" + callout, replaceHTML(xmlDocument.print()));
    }
  }

  public String getOnChangeCode() {
    StringBuffer text = new StringBuffer();
    if (getData("IsDisplayLogic").equals("Y")) {
      text.append("displayLogic();");
    }
    if (getData("IsReadOnlyLogic").equals("Y")) {
      text.append("readOnlyLogic();");
    }
    String callout = getData("CallOutName");
    String isComboReload = getData("IsComboReload");
    if (isComboReload == null || isComboReload.equals("")) {
      isComboReload = "N";
    }
    if (callout != null && !callout.equals("")) {
      text.append("callout").append(callout).append("(this.name);");
    }
    if (isComboReload.equals("Y")) {
      text.append("reloadComboReloads").append(getData("AD_Tab_ID")).append("(this.name);");
    }
    return text.toString();
  }

  public String editMode() {
    XmlDocument xmlDocument = getReportEngine()
        .readXmlTemplate("org/openbravo/wad/controls/WADControl")
        .createXmlDocument();

    xmlDocument.setParameter("columnName", getData("ColumnName"));
    xmlDocument.setParameter("columnNameInp", getData("ColumnNameInp"));
    xmlDocument.setParameter("size", getData("CssSize"));
    xmlDocument.setParameter("maxlength", getData("FieldLength"));
    boolean isDisabled = (getData("IsReadOnly").equals("Y") || getData("IsReadOnlyTab").equals("Y")
        || getData("IsUpdateable").equals("N"));
    xmlDocument.setParameter("disabled", (isDisabled ? "Y" : "N"));

    if (!isDisabled && getData("IsMandatory").equals("Y")) {
      xmlDocument.setParameter("required", "true");
      xmlDocument.setParameter("requiredClass", " required");
    } else {
      xmlDocument.setParameter("required", "false");
      xmlDocument.setParameter("requiredClass", (isDisabled ? " readonly" : ""));
    }

    xmlDocument.setParameter("callout", getOnChangeCode());

    return replaceHTML(xmlDocument.print());
  }

  public String newMode() {
    XmlDocument xmlDocument = getReportEngine()
        .readXmlTemplate("org/openbravo/wad/controls/WADControl")
        .createXmlDocument();

    xmlDocument.setParameter("columnName", getData("ColumnName"));
    xmlDocument.setParameter("columnNameInp", getData("ColumnNameInp"));
    xmlDocument.setParameter("size", getData("CssSize"));
    xmlDocument.setParameter("maxlength", getData("FieldLength"));

    boolean isDisabled = (getData("IsReadOnly").equals("Y")
        || getData("IsReadOnlyTab").equals("Y"));
    xmlDocument.setParameter("disabled", (isDisabled ? "Y" : "N"));
    if (!isDisabled && getData("IsMandatory").equals("Y")) {
      xmlDocument.setParameter("required", "true");
      xmlDocument.setParameter("requiredClass", " required");
    } else {
      xmlDocument.setParameter("required", "false");
      xmlDocument.setParameter("requiredClass", (isDisabled ? " readonly" : ""));
    }

    xmlDocument.setParameter("callout", getOnChangeCode());

    return replaceHTML(xmlDocument.print());
  }

  public String getType() {
    if (getData("IsDisplayed").equals("N")) {
      return "Hidden";
    } else {
      return "TextBox";
    }
  }

  @Override
  public String toString() {
    StringBuffer text = new StringBuffer();
    if (getData("IsDisplayed").equals("N")) {
      text.append(getHiddenHTML());
    } else {
      text.append("<div id=\"editDiscard\">");
      text.append(editMode()).append("");
      text.append("</div>");
      text.append("<div id=\"newDiscard\">");
      text.append(newMode()).append("");
      text.append("</div>");
    }
    return text.toString();
  }

  public String toLabel() {
    if (getData("AD_Reference_ID").equals("28")) {
      return "";
    }
    String[] discard = { "isNotLinkable" };
    String isLinkable = getData("IsLinkable");
    if (isLinkable == null || !isLinkable.equals("Y")) {
      discard[0] = "isLinkable";
    }

    createWADLabelControl();
    WadControlLabelBuilder builder = new WadControlLabelBuilder(label);
    builder.buildLabelControl();
    return builder.getLabelString();
  }

  public String toXml() {
    StringBuffer text = new StringBuffer();
    if (getData("IsParameter").equals("Y")) {
      text.append("<PARAMETER id=\"").append(getData("ColumnName"));
      text.append("\" name=\"").append(getData("ColumnName"));
      text.append("\" attribute=\"value\" default=\"N\" replaceCharacters=\"htmlPreformated\"/>");
    } else {
      text.append("<FIELD id=\"").append(getData("ColumnName"));
      text.append("\" attribute=\"value\" replaceCharacters=\"htmlPreformated\" default=\"N\">");
      text.append(getData("ColumnName")).append("</FIELD>");
    }
    return text.toString();
  }

  public String toLabelXML() {
    StringBuffer labelText = new StringBuffer();
    createWADLabelControl();
    if (label.getLabelId() != null && !label.getLabelId().equals("")
        && label.getLabelPlaceHolderText() != null && !label.getLabelPlaceHolderText().equals("")) {
      labelText.append("<LABEL id=\"").append(label.getLabelId());
      labelText.append("\" name=\"").append(label.getLabelId());
      labelText.append("\" replace=\"" + label.getLabelPlaceHolderText() + "\">");
      labelText.append(label.getColumnName()).append("lbl");
      labelText.append("</LABEL>");
    } else {
      labelText.append("");
    }

    return labelText.toString();
  }

  private void createWADLabelControl() {
    String column = getData("ColumnName");
    String labelText = getData("ColumnLabelText");
    if (labelText.trim().equals("")) {
      labelText = column;
    }
    String columnId = getData("AdColumnId");
    String columnLink = getData("ColumnNameLabel");
    if (columnLink == null || columnLink.equals("")) {
      columnLink = getData("ColumnName");
    }
    label = new WADLabelControl(WADLabelControl.FIELD_LABEL, null, null, columnId, column, null,
        null, getData("IsLinkable"), getData("KeyColumnName"), getData("ColumnNameInp"),
        getData("AD_Table_ID"), columnLink);
  }

  /**
   * Checks whether the reference is a numeric value
   * 
   * @return true in case the reference is numeric
   */
  public boolean isNumericType() {
    return false;
  }

  public static void setDateFormat(String dateFormat) {
    sqlDateFormat = dateFormat;
  }

  public boolean isDate() {
    return false;
  }

  public boolean isTime() {
    return false;
  }

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public String getSubreference() {
    return subreference;
  }

  public void setSubreference(String subreference) {
    this.subreference = subreference;
  }

  /**
   * Returns HTML needed for hidden fields
   */
  public String getHiddenHTML() {
    XmlDocument xmlDocument = getReportEngine()
        .readXmlTemplate("org/openbravo/wad/controls/WADHidden")
        .createXmlDocument();

    xmlDocument.setParameter("columnName", getData("ColumnName"));
    xmlDocument.setParameter("columnNameInp", getData("ColumnNameInp"));

    return replaceHTML(xmlDocument.print());
  }

  /**
   * Returns XML needed for hidden fields
   */
  public String getHiddenXML() {
    XmlDocument xmlDocument = getReportEngine()
        .readXmlTemplate("org/openbravo/wad/controls/WADHiddenXML")
        .createXmlDocument();

    xmlDocument.setParameter("columnName", getData("ColumnName"));
    return replaceHTML(xmlDocument.print());
  }

}
