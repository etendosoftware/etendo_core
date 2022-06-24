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
 * All portions are Copyright (C) 2001-2010 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.wad.controls;

import java.util.Properties;

import org.openbravo.xmlEngine.XmlDocument;

public class WADNumber extends WADControl {
  private WADControl button;

  public WADNumber() {
  }

  public WADNumber(Properties prop) {
    setInfo(prop);
    initialize();
  }

  @Override
  public void initialize() {
    generateJSCode();
    this.button = new WADFieldButton("Calc", getData("ColumnName"), getData("ColumnNameInp"),
        getData("Name"), "calculator('frmMain.inp" + getData("ColumnNameInp")
            + "', document.frmMain.inp" + getData("ColumnNameInp") + ".value, false);");
  }

  private void generateJSCode() {
    addImport("calculator", "../../../../../web/js/calculator.js");
    generateValidation();
    setCalloutJS();
  }

  public void generateValidation() {
    String[] discard = { "", "", "", "" };
    String join = "";
    if (!getData("IsMandatory").equals("Y")) {
      discard[0] = "isMandatory";
    }
    if (getData("ValueMin").equals("") && getData("ValueMax").equals("")) {
      discard[1] = "isValueCheck";
    }
    boolean valmin = false;
    if (getData("ValueMin").equals("")) {
      discard[2] = "isValueMin";
    } else {
      valmin = true;
    }
    if (getData("ValueMax").equals("")) {
      discard[3] = "isValueMax";
    } else if (valmin) {
      join = " || ";
    }

    XmlDocument xmlDocument = getReportEngine()
        .readXmlTemplate("org/openbravo/wad/controls/WADNumberJSValidation", discard)
        .createXmlDocument();
    xmlDocument.setParameter("columnNameInp", getData("ColumnNameInp"));
    xmlDocument.setParameter("valueMin", getData("ValueMin"));
    xmlDocument.setParameter("valueMax", getData("ValueMax"));
    xmlDocument.setParameter("join", join);
    setValidation(replaceHTML(xmlDocument.print()));
  }

  @Override
  public String getType() {
    return "TextBox_btn";
  }

  @Override
  public String editMode() {
    String textButton = "";
    String buttonClass = "";
    if (getData("IsReadOnly").equals("N") && getData("IsReadOnlyTab").equals("N")
        && getData("IsUpdateable").equals("Y")) {
      this.button.setReportEngine(getReportEngine());
      textButton = this.button.toString();
      buttonClass = this.button.getType();
    }
    String[] discard = { "" };
    if (!getData("IsMandatory").equals("Y")) {
      // if field is not mandatory, discard it
      discard[0] = "xxmissingSpan";
    }
    XmlDocument xmlDocument = getReportEngine()
        .readXmlTemplate("org/openbravo/wad/controls/WADNumber", discard)
        .createXmlDocument();

    xmlDocument.setParameter("columnName", getData("ColumnName"));
    xmlDocument.setParameter("columnNameInp", getData("ColumnNameInp"));
    xmlDocument.setParameter("size", (textButton.equals("") ? "" : "btn_") + getData("CssSize"));
    xmlDocument.setParameter("maxlength", getData("FieldLength"));
    xmlDocument.setParameter("hasButton", (textButton.equals("") ? "TextButton_ContentCell" : ""));
    xmlDocument.setParameter("buttonClass", buttonClass + "_ContentCell");
    xmlDocument.setParameter("button", textButton);

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
    xmlDocument.setParameter("textBoxCSS", (isDisabled ? "_ReadOnly" : ""));

    xmlDocument.setParameter("callout", getOnChangeCode());

    xmlDocument.setParameter("columnName", getData("ColumnName"));
    xmlDocument.setParameter("outputFormat", getFormat());

    return replaceHTML(xmlDocument.print());
  }

  @Override
  public String newMode() {
    String textButton = "";
    String buttonClass = "";
    if (getData("IsReadOnly").equals("N") && getData("IsReadOnlyTab").equals("N")) {
      this.button.setReportEngine(getReportEngine());
      textButton = this.button.toString();
      buttonClass = this.button.getType();
    }
    String[] discard = { "" };
    if (!getData("IsMandatory").equals("Y")) {
      // if field is not mandatory, discard it
      discard[0] = "xxmissingSpan";
    }
    XmlDocument xmlDocument = getReportEngine()
        .readXmlTemplate("org/openbravo/wad/controls/WADNumber", discard)
        .createXmlDocument();

    xmlDocument.setParameter("columnName", getData("ColumnName"));
    xmlDocument.setParameter("columnNameInp", getData("ColumnNameInp"));
    xmlDocument.setParameter("size", (textButton.equals("") ? "" : "btn_") + getData("CssSize"));
    xmlDocument.setParameter("maxlength", getData("FieldLength"));
    xmlDocument.setParameter("hasButton", (textButton.equals("") ? "TextButton_ContentCell" : ""));
    xmlDocument.setParameter("buttonClass", buttonClass + "_ContentCell");
    xmlDocument.setParameter("button", textButton);

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
    xmlDocument.setParameter("textBoxCSS", (isDisabled ? "_ReadOnly" : ""));

    xmlDocument.setParameter("callout", getOnChangeCode());

    xmlDocument.setParameter("columnName", getData("ColumnName"));
    xmlDocument.setParameter("outputFormat", getFormat());

    return replaceHTML(xmlDocument.print());
  }

  @Override
  public String toXml() {
    String[] discard = { "xx_PARAM" };
    if (getData("IsParameter").equals("Y")) {
      discard[0] = "xx";
    }
    XmlDocument xmlDocument = getReportEngine()
        .readXmlTemplate("org/openbravo/wad/controls/WADNumberXML", discard)
        .createXmlDocument();

    xmlDocument.setParameter("columnName", getData("ColumnName"));
    xmlDocument.setParameter("columnFormat", getFormat());

    return replaceHTML(xmlDocument.print());
  }

  private String getFormat() {
    if (isDecimalNumber(getData("AD_Reference_ID"))) {
      return "euroEdition";
    } else if (isQtyNumber(getData("AD_Reference_ID"))) {
      return "qtyEdition";
    } else if (isPriceNumber(getData("AD_Reference_ID"))) {
      return "priceEdition";
    } else if (isIntegerNumber(getData("AD_Reference_ID"))) {
      return "integerEdition";
    } else if (isGeneralNumber(getData("AD_Reference_ID"))) {
      return "generalQtyEdition";
    } else {
      return "qtyEdition";
    }
  }

  private static boolean isDecimalNumber(String reference) {
    if (reference == null || reference.equals("")) {
      return false;
    }
    return (reference.equals("12") || reference.equals("22"));
  }

  private static boolean isGeneralNumber(String reference) {
    if (reference == null || reference.equals("")) {
      return false;
    }
    return reference.equals("800019");
  }

  private static boolean isQtyNumber(String reference) {
    if (reference == null || reference.equals("")) {
      return false;
    }
    return reference.equals("29");
  }

  private static boolean isPriceNumber(String reference) {
    if (reference == null || reference.equals("")) {
      return false;
    }
    return reference.equals("800008");

  }

  private static boolean isIntegerNumber(String reference) {
    if (reference == null || reference.equals("")) {
      return false;
    }
    return reference.equals("11");
  }

  @Override
  public boolean isNumericType() {
    return true;
  }

  @Override
  public String getHiddenHTML() {
    XmlDocument xmlDocument = getReportEngine()
        .readXmlTemplate("org/openbravo/wad/controls/WADHiddenNumber")
        .createXmlDocument();

    xmlDocument.setParameter("columnName", getData("ColumnName"));
    xmlDocument.setParameter("columnNameInp", getData("ColumnNameInp"));
    xmlDocument.setParameter("outputformat", getFormat());

    return replaceHTML(xmlDocument.print());
  }

}
