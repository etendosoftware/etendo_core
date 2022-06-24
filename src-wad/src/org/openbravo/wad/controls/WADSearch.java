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

import org.openbravo.data.Sqlc;
import org.openbravo.utils.FormatUtilities;
import org.openbravo.xmlEngine.XmlDocument;

public class WADSearch extends WADControl {
  public WADControl button;
  public String command = "";
  public String hiddenFields = "";
  public String imageName = "";
  public String searchName = "";
  public boolean isFieldEditable = true;

  public WADSearch() {
  }

  public WADSearch(Properties prop) {
    setInfo(prop);
    initialize();
  }

  @Override
  public void initialize() {
    addImport("ValidationTextBox", "../../../../../web/js/default/ValidationTextBox.js");
    addImport("searchs", "../../../../../web/js/searchs.js");
    generateJSCode();
    this.button = new WADFieldButton(this.imageName, getData("ColumnName"),
        getData("ColumnNameInp"), this.searchName, this.command);
    if (getData("AD_Reference_Value_ID").equals("21")) {
      this.isFieldEditable = false;
    }
  }

  private void generateJSCode() {
    StringBuffer validation = new StringBuffer();
    if (getData("IsMandatory").equals("Y")) {
      validation.append("  if (inputValue(frm.inp")
          .append(getData("ColumnNameInp"))
          .append(")==null || inputValue(frm.inp")
          .append(getData("ColumnNameInp"))
          .append(")==\"\") {\n");
      if (getData("IsDisplayed").equals("Y")) {
        validation.append("    setWindowElementFocus(frm.inp")
            .append(getData("ColumnNameInp"))
            .append("_R);\n");
      }
      validation.append("    showJSMessage(1);\n");
      validation.append("    return false;\n");
      validation.append("  }\n");
    }
    setValidation(validation.toString());
    setCalloutJS();
    {
      String text = "function debugSearch(key, text, keyField) {\n" + "  return true;\n" + "}";
      addJSCode("debugSearch", text);
    }
    if (!getData("IsReadOnly").equals("Y") && !getData("IsReadOnlyTab").equals("Y")) {
      StringBuffer columnsScript = new StringBuffer();
      StringBuffer commandScript = new StringBuffer();
      StringBuffer hiddenScript = new StringBuffer();
      StringBuffer text = new StringBuffer();
      if (this.imageName == null || this.imageName.equals("")) {
        this.imageName = FormatUtilities.replace(getData("searchName"));
      }
      if (this.searchName == null || this.searchName.equals("")) {
        this.searchName = getData("Name");
      }
      String servletName = "/info/" + this.imageName + ".html";
      try {
        if (!getData("AD_Reference_Value_ID").equals("")) {
          WADSearchData[] data = WADSearchData.select(getConnection(), getData("AD_Language"),
              getData("AD_Reference_Value_ID"));
          if (data != null && data.length > 0) {
            servletName = data[0].mappingname;
            this.searchName = data[0].referenceNameTrl;
            // this.imageName =
            // FormatUtilities.replace(data[0].referenceName) +
            // ".gif";
            if (!servletName.startsWith("/")) {
              servletName = '/' + servletName;
            }
            for (int i = 0; i < data.length; i++) {
              if (data[i].columntype.equals("I")) {
                columnsScript.append(", 'inp").append(data[i].name).append("'");
                columnsScript.append(", inputValue(document.frmMain.inp")
                    .append(Sqlc.TransformaNombreColumna(data[i].columnname))
                    .append(')');
              } else {
                hiddenScript.append("<input type=\"hidden\" name=\"inp")
                    .append(Sqlc.TransformaNombreColumna(data[i].name));
                hiddenScript.append(data[i].columnSuffix).append("\" value=\"\" ");
                hiddenScript.append("id=\"")
                    .append(data[i].columnname)
                    .append(data[i].columnSuffix)
                    .append("\"/>\n");
              }
            }
          }
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      commandScript.append("openSearch(null, null, '..").append(servletName).append("', ");
      commandScript.append("null, false, 'frmMain', 'inp")
          .append(getData("ColumnNameInp"))
          .append("', ");
      commandScript.append("'inp").append(getData("ColumnNameInp")).append("_R', ");
      commandScript.append("inputValue(document.frmMain.inp")
          .append(getData("ColumnNameInp"))
          .append("_R), ");
      commandScript.append("'inpIDValue', inputValue(document.frmMain.inp")
          .append(getData("ColumnNameInp"))
          .append("), ");
      commandScript.append("'WindowID', inputValue(document.frmMain.inpwindowId)");
      commandScript.append(columnsScript);
      text.append(commandScript).append(", 'Command', 'KEY'");
      commandScript.append(");");
      text.append(");");
      setOnLoad("keyArray[keyArray.length] = new keyArrayItem(\"ENTER\", \"" + text.toString()
          + "\", \"inp" + getData("ColumnNameInp") + "_R\", \"null\");");
      this.command = commandScript.toString();
      this.hiddenFields = hiddenScript.toString();
    }
  }

  @Override
  public String getType() {
    return "TextBox_btn";
  }

  @Override
  public String editMode() {
    String textButton = "";
    String buttonClass = "";
    String tabIndex = "";

    if (getData("IsReadOnly").equals("N") && getData("IsReadOnlyTab").equals("N")
        && getData("IsUpdateable").equals("Y")) {
      this.button.setReportEngine(getReportEngine());
      textButton = this.button.toString();
      buttonClass = this.button.getType();
      if (!this.isFieldEditable) {
        tabIndex = "1";
      }
    }
    String[] discard = { "" };
    if (!getData("IsMandatory").equals("Y")) {
      // if field is not mandatory, discard it
      discard[0] = "xxmissingSpan";
    }

    XmlDocument xmlDocument = getReportEngine()
        .readXmlTemplate("org/openbravo/wad/controls/WADSearch", discard)
        .createXmlDocument();
    xmlDocument.setParameter("tabindex", tabIndex);
    xmlDocument.setParameter("columnName", getData("ColumnName"));
    xmlDocument.setParameter("columnNameInp", getData("ColumnNameInp"));
    xmlDocument.setParameter("size", (textButton.equals("") ? "" : "btn_") + getData("CssSize"));
    xmlDocument.setParameter("maxlength", getData("FieldLength"));
    xmlDocument.setParameter("hiddens", this.hiddenFields);
    xmlDocument.setParameter("hasButton", (textButton.equals("") ? "TextButton_ContentCell" : ""));
    xmlDocument.setParameter("buttonClass", buttonClass + "_ContentCell");
    xmlDocument.setParameter("button", textButton);
    String className = "";
    boolean isDisabled = (getData("IsReadOnly").equals("Y") || getData("IsReadOnlyTab").equals("Y")
        || getData("IsUpdateable").equals("N"));
    if (!isDisabled && !this.isFieldEditable && getData("IsMandatory").equals("Y")) {
      className += " readonly_required";
    } else if (isDisabled || !this.isFieldEditable) {
      className += " readonly";
    } else if (getData("IsMandatory").equals("Y")) {
      className += " required";
    }
    xmlDocument.setParameter("className", className);

    if (isDisabled || !this.isFieldEditable) {
      xmlDocument.setParameter("disabled", "Y");
    } else {
      xmlDocument.setParameter("disabled", "N");
    }
    if (getData("IsMandatory").equals("Y")) {
      xmlDocument.setParameter("required", "true");
    } else {
      xmlDocument.setParameter("required", "false");
    }
    xmlDocument.setParameter("textBoxCSS", (isDisabled ? "_ReadOnly" : ""));

    xmlDocument.setParameter("callout", getOnChangeCode());
    return replaceHTML(xmlDocument.print());
  }

  @Override
  public String newMode() {
    String textButton = "";
    String buttonClass = "";
    String tabIndex = "";
    if (getData("IsReadOnly").equals("N") && getData("IsReadOnlyTab").equals("N")) {
      this.button.setReportEngine(getReportEngine());
      textButton = this.button.toString();
      buttonClass = this.button.getType();
      if (!this.isFieldEditable) {
        tabIndex = "1";
      }
    }
    String[] discard = { "" };
    if (!getData("IsMandatory").equals("Y")) {
      // if field is not mandatory, discard it
      discard[0] = "xxmissingSpan";
    }
    XmlDocument xmlDocument = getReportEngine()
        .readXmlTemplate("org/openbravo/wad/controls/WADSearch", discard)
        .createXmlDocument();
    xmlDocument.setParameter("tabindex", tabIndex);
    xmlDocument.setParameter("columnName", getData("ColumnName"));
    xmlDocument.setParameter("columnNameInp", getData("ColumnNameInp"));
    xmlDocument.setParameter("size", (textButton.equals("") ? "" : "btn_") + getData("CssSize"));
    xmlDocument.setParameter("maxlength", getData("FieldLength"));
    xmlDocument.setParameter("hiddens", this.hiddenFields);
    xmlDocument.setParameter("hasButton", (textButton.equals("") ? "TextButton_ContentCell" : ""));
    xmlDocument.setParameter("buttonClass", buttonClass + "_ContentCell");
    xmlDocument.setParameter("button", textButton);
    String className = "";
    boolean isDisabled = (getData("IsReadOnly").equals("Y")
        || getData("IsReadOnlyTab").equals("Y"));
    if (isDisabled || !this.isFieldEditable) {
      className += " readonly";
    } else if (getData("IsMandatory").equals("Y")) {
      className += " required";
    }
    xmlDocument.setParameter("className", className);

    if (isDisabled || !this.isFieldEditable) {
      xmlDocument.setParameter("disabled", "Y");
    } else {
      xmlDocument.setParameter("disabled", "N");
    }
    if (getData("IsMandatory").equals("Y")) {
      xmlDocument.setParameter("required", "true");
    } else {
      xmlDocument.setParameter("required", "false");
    }
    xmlDocument.setParameter("textBoxCSS", (isDisabled ? "_ReadOnly" : ""));

    xmlDocument.setParameter("callout", getOnChangeCode());

    return replaceHTML(xmlDocument.print());
  }

  @Override
  public String toXml() {
    String[] discard = { "xx_PARAM", "xx_PARAM_R" };
    if (getData("IsParameter").equals("Y")) {
      discard[0] = "xx";
      discard[1] = "xx_R";
    }
    XmlDocument xmlDocument = getReportEngine()
        .readXmlTemplate("org/openbravo/wad/controls/WADSearchXML", discard)
        .createXmlDocument();

    xmlDocument.setParameter("columnName", getData("ColumnName"));
    return replaceHTML(xmlDocument.print());
  }

}
