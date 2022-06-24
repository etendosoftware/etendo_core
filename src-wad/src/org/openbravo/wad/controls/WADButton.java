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
 * All portions are Copyright (C) 2001-2011 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.wad.controls;

import java.util.HashMap;
import java.util.Properties;

import org.openbravo.utils.FormatUtilities;
import org.openbravo.xmlEngine.XmlDocument;

public class WADButton extends WADControl {
  public WADButton() {
  }

  public WADButton(Properties prop) {
    setInfo(prop);
    initialize();
  }

  public void setShortcuts(HashMap<String, String> sc) {
    setData("nameButton", "");
  }

  @Override
  public void initialize() {
    generateJSCode();
  }

  private void generateJSCode() {
    setValidation("");
    setCalloutJS();
  }

  @Override
  public String getType() {
    return "Button_CenterAlign";
  }

  private StringBuffer getAction() {
    final String logClickCode = "logClick(document.getElementById('" + getData("ColumnName")
        + "'));";
    final boolean triggersAutosave = getData("IsAutosave").equalsIgnoreCase("Y");

    StringBuffer text = new StringBuffer();
    boolean isDisabled = (getData("IsReadOnly").equals("Y")
        || (getData("IsReadOnlyTab").equals("Y") && getData("isReadOnlyDefinedTab").equals("N"))
        || getData("IsUpdateable").equals("N"));

    if (isDisabled) {
      text.append("return true;");
    } else {
      if (getData("MappingName").equals("")) {
        if (triggersAutosave) {
          text.append(logClickCode);
        }
        text.append("openServletNewWindow('BUTTON")
            .append(FormatUtilities.replace(getData("ColumnName")))
            .append(getData("AD_Process_ID"));
        text.append("', true, '")
            .append(getData("TabName"))
            .append("_Edition.html', 'BUTTON', null, ")
            .append(triggersAutosave);

        text.append(", 600, 900, null, null, null, null, zz);");
      } else {
        if (triggersAutosave) {
          text.append(logClickCode);
        }
        text.append("openServletNewWindow('DEFAULT', true, '..");
        if (!getData("MappingName").startsWith("/")) {
          text.append('/');
        }
        text.append(getData("MappingName"))
            .append("', 'BUTTON', '")
            .append(getData("AD_Process_ID"))
            .append("', ")
            .append(triggersAutosave);
        text.append(",600, 900, null, null, null, null, zz);");
      }
    }
    return text;
  }

  @Override
  public String editMode() {
    XmlDocument xmlDocument = getReportEngine()
        .readXmlTemplate("org/openbravo/wad/controls/WADButton")
        .createXmlDocument();

    xmlDocument.setParameter("columnName", getData("ColumnName"));
    xmlDocument.setParameter("columnNameInp", getData("ColumnNameInp"));
    xmlDocument.setParameter("nameHTML", getData("nameButton"));
    xmlDocument.setParameter("name", getData("Name"));

    xmlDocument.setParameter("callout", getOnChangeCode());
    xmlDocument.setParameter("action", getAction().toString());

    boolean isDisabled = (getData("IsReadOnly").equals("Y")
        || (getData("IsReadOnlyTab").equals("Y") && getData("isReadOnlyDefinedTab").equals("N"))
        || getData("IsUpdateable").equals("N"));
    if (isDisabled) {
      xmlDocument.setParameter("disabled", "_disabled");
    }
    return replaceHTML(xmlDocument.print());
  }

  @Override
  public String newMode() {
    XmlDocument xmlDocument = getReportEngine()
        .readXmlTemplate("org/openbravo/wad/controls/WADButton")
        .createXmlDocument();

    xmlDocument.setParameter("columnName", getData("ColumnName"));
    xmlDocument.setParameter("columnNameInp", getData("ColumnNameInp"));
    xmlDocument.setParameter("nameHTML", getData("nameButton"));
    xmlDocument.setParameter("name", getData("Name"));

    xmlDocument.setParameter("callout", getOnChangeCode());

    xmlDocument.setParameter("inputId", getData("ColumnName"));
    xmlDocument.setParameter("action", getAction().toString());

    boolean isDisabled = (getData("IsReadOnly").equals("Y")
        || (getData("IsReadOnlyTab").equals("Y") && getData("isReadOnlyDefinedTab").equals("N"))
        || getData("IsUpdateable").equals("N"));

    if (isDisabled) {
      xmlDocument.setParameter("disabled", "_disabled");
    }
    return replaceHTML(xmlDocument.print());
  }

  @Override
  public String toXml() {
    StringBuffer text = new StringBuffer();

    boolean isDisabled = getData("IsReadOnly").equals("Y") || getData("IsUpdateable").equals("N");

    if (getData("IsParameter").equals("Y")) {
      text.append("<PARAMETER id=\"").append(getData("ColumnName"));
      text.append("\" name=\"").append(getData("ColumnName"));
      text.append("\" attribute=\"value\"/>");
      if (getData("IsDisplayed").equals("Y")
          && !getData("ColumnName").equalsIgnoreCase("ChangeProjectStatus")) {
        text.append("\n<PARAMETER id=\"").append(getData("ColumnName")).append("_BTN\" name=\"");
        text.append(getData("ColumnName"));
        text.append("_BTN\" replaceCharacters=\"htmlPreformated\"/>");
      }
    } else {
      if (getData("IsDisplayed").equals("Y")) {
        text.append("<PARAMETER id=\"")
            .append(getData("ColumnName"))
            .append("_BTNname\" name=\"")
            .append(getData("ColumnName"))
            .append("_BTNname\" default=\"\"/>\n");
      }
      text.append("<FIELD id=\"").append(getData("ColumnName"));
      text.append("\" attribute=\"value\">").append(getData("ColumnName")).append("</FIELD>");
      if (getData("IsDisplayed").equals("Y")
          && !getData("ColumnName").equalsIgnoreCase("ChangeProjectStatus")) {
        text.append("\n<FIELD id=\"")
            .append(getData("ColumnName"))
            .append("_BTN\" replaceCharacters=\"htmlPreformated\">");
        text.append(getData("ColumnName")).append("_BTN</FIELD>");
      }
    }

    if (!isDisabled) {
      text.append("<PARAMETER id=\"").append(getData("ColumnName"));
      text.append("_linkBTN\" name=\"").append(getData("ColumnName"));
      text.append("_Modal\" attribute=\"onclick\" replace=\"zz\" default=\"false\"/>");
    }
    return text.toString();
  }

}
