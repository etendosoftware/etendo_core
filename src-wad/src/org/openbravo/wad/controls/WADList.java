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

public class WADList extends WADControl {

  public WADList() {
  }

  public WADList(Properties prop) {
    setInfo(prop);
    initialize();
  }

  @Override
  public void initialize() {
    generateJSCode();
  }

  private void generateJSCode() {
    if (getData("IsMandatory").equals("Y")) {
      StringBuffer text = new StringBuffer();
      text.append("  if (inputValue(frm.inp").append(getData("ColumnNameInp"));
      text.append(")==null || inputValue(frm.inp");
      text.append(getData("ColumnNameInp"));
      text.append(")==\"\") {\n");
      text.append("    setWindowElementFocus(frm.inp")
          .append(getData("ColumnNameInp"))
          .append(");\n");
      text.append("    showJSMessage(1);\n");
      text.append("    return false;\n");
      text.append("  }");
      setValidation(replaceHTML(text.toString()));
    }
    if ("Y".equals(getData("ValidateOnNew"))) {
      setOnLoad("if (inputValue(key)==null || inputValue(key)==\"\") updateOnChange(frm.inp"
          + getData("ColumnNameInp") + ");");
    }
    setCalloutJS();
  }

  @Override
  public String getType() {
    return "Combo";
  }

  @Override
  public String editMode() {
    String[] discard = { "" };
    if (getData("IsMandatory").equals("Y")) {
      discard[0] = "fieldBlankSection";
    }
    XmlDocument xmlDocument = getReportEngine()
        .readXmlTemplate("org/openbravo/wad/controls/WADList", discard)
        .createXmlDocument();

    xmlDocument.setParameter("columnName", getData("ColumnName"));
    xmlDocument.setParameter("columnNameInp", getData("ColumnNameInp"));
    String length = getData("DisplayLength");
    if (!length.endsWith("%")) {
      length += "px";
    }
    xmlDocument.setParameter("size", getData("CssSize"));

    String auxClassName = "";
    if (getData("IsMandatory").equals("Y")) {
      auxClassName += "Key";
    }
    if (getData("IsReadOnly").equals("Y") || getData("IsReadOnlyTab").equals("Y")) {
      auxClassName += "ReadOnly";
    } else if (getData("IsUpdateable").equals("N")) {
      auxClassName += "NoUpdatable";
    }
    xmlDocument.setParameter("myClass", auxClassName);

    if (getData("IsReadOnly").equals("Y") || getData("IsReadOnlyTab").equals("Y")
        || getData("IsUpdateable").equals("N")) {
      xmlDocument.setParameter("disabled", "Y");
    } else {
      xmlDocument.setParameter("disabled", "N");
      // if (getData("IsMandatory").equals("Y"))
      // xmlDocument.setParameter("required", "Y");
      // else xmlDocument.setParameter("required", "N");
    }

    StringBuffer text = new StringBuffer();
    if (getData("IsReadOnly").equals("Y") || getData("IsReadOnlyTab").equals("Y")
        || getData("IsUpdateable").equals("N")) {
      text.append("selectCombo(this, 'xx');return true; tmp_water_mark; ");
    }
    text.append(getOnChangeCode());
    xmlDocument.setParameter("callout", text.toString());

    return replaceHTML(xmlDocument.print());
  }

  @Override
  public String newMode() {
    String[] discard = { "" };
    if (getData("IsMandatory").equals("Y")) {
      discard[0] = "fieldBlankSection";
    }
    XmlDocument xmlDocument = getReportEngine()
        .readXmlTemplate("org/openbravo/wad/controls/WADList", discard)
        .createXmlDocument();

    xmlDocument.setParameter("columnName", getData("ColumnName"));
    xmlDocument.setParameter("columnNameInp", getData("ColumnNameInp"));
    String length = getData("DisplayLength");
    if (!length.endsWith("%")) {
      length += "px";
    }
    xmlDocument.setParameter("size", getData("CssSize"));

    String auxClassName = "";
    if (getData("IsMandatory").equals("Y")) {
      auxClassName += "Key";
    }
    if (getData("IsReadOnly").equals("Y") || getData("IsReadOnlyTab").equals("Y")) {
      auxClassName += "ReadOnly";
    }
    xmlDocument.setParameter("myClass", auxClassName);

    if (getData("IsReadOnly").equals("Y") || getData("IsReadOnlyTab").equals("Y")) {
      xmlDocument.setParameter("disabled", "Y");
    } else {
      xmlDocument.setParameter("disabled", "N");
      // if (getData("IsMandatory").equals("Y"))
      // xmlDocument.setParameter("required", "Y");
      // else xmlDocument.setParameter("required", "N");
    }

    StringBuffer text = new StringBuffer();
    if (getData("IsReadOnly").equals("Y") || getData("IsReadOnlyTab").equals("Y")) {
      text.append("selectCombo(this, 'xx');return true; tmp_water_mark; ");
    }
    text.append(getOnChangeCode());
    xmlDocument.setParameter("callout", text.toString());

    return replaceHTML(xmlDocument.print());
  }

  @Override
  public String toXml() {
    StringBuffer text = new StringBuffer();
    if (getData("IsParameter").equals("Y")) {
      text.append("<PARAMETER id=\"").append(getData("ColumnName"));
      text.append("\" name=\"").append(getData("ColumnName")).append("\" attribute=\"value\"/>");
      if (getData("IsReadOnly").equals("Y") || getData("IsReadOnlyTab").equals("Y")
          || getData("IsUpdateable").equals("N")) {
        text.append("\n<PARAMETER id=\"report").append(getData("ColumnName"));
        text.append("_S\" name=\"report")
            .append(getData("ColumnName"))
            .append("_S\" attribute=\"onchange\" replace=\"xx\"/>");
      }
    } else {
      text.append("<FIELD id=\"").append(getData("ColumnName"));
      text.append("\" attribute=\"value\">");
      text.append(getData("ColumnName")).append("</FIELD>");
      if (getData("IsReadOnly").equals("Y") || getData("IsReadOnlyTab").equals("Y")
          || getData("IsUpdateable").equals("N")) {
        text.append("\n<FIELD id=\"report").append(getData("ColumnName"));
        text.append("_S\" attribute=\"onchange\" replace=\"xx\">");
        text.append(getData("ColumnName")).append("</FIELD>");
      }
    }
    if (getData("IsDisplayed").equals("Y")) {
      text.append("\n<SUBREPORT id=\"report").append(getData("ColumnName"));
      text.append("\" name=\"report").append(getData("ColumnName"));
      text.append("\" report=\"org/openbravo/erpCommon/reference/List\">\n");
      text.append("  <ARGUMENT name=\"parameterListSelected\" withId=\"")
          .append(getData("ColumnName"))
          .append("\"/>\n");
      text.append("</SUBREPORT>");
    }
    return text.toString();
  }

}
