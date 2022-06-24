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
 * All portions are Copyright (C) 2001-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.openbravo.database.ConnectionProvider;

public class ToolBar {
  private ConnectionProvider conn;
  private String language = "en_US";
  private String servlet_action = "";
  private boolean isNew = false;
  private String keyfield = "";
  private String grid_id = "";
  private String pdf = "";
  private boolean isDirectPrint = false;
  private String base_direction = "";
  private boolean debug = false;
  private boolean isSrcWindow = false;
  private boolean isFrame = false;
  private boolean email = false;
  private Map<String, HTMLElement> buttons = new HashMap<>();

  public void setEmail(boolean email) {
    this.email = email;
  }

  /**
   * Constructor used by the grid view of all generated windows.
   */
  public ToolBar(ConnectionProvider _conn, String _language, String _action, boolean _isNew,
      String _keyINP, String _gridID, String _PDFName, boolean _isDirectPrinting,
      String _windowName, String _baseDirection, boolean _debug) {
    this(_conn, true, _language, _action, _isNew, _keyINP, _gridID, _PDFName, _isDirectPrinting,
        _windowName, _baseDirection, _debug, false);
  }

  public ToolBar(ConnectionProvider _conn, String _language, String _action, boolean _isNew,
      String _keyINP, String _gridID, String _PDFName, boolean _isDirectPrinting,
      String _windowName, String _baseDirection, boolean _debug, boolean _isSrcWindow) {
    this(_conn, true, _language, _action, _isNew, _keyINP, _gridID, _PDFName, _isDirectPrinting,
        _windowName, _baseDirection, _debug, _isSrcWindow, false);
  }

  public ToolBar(ConnectionProvider _conn, String _language, String _action, boolean _isNew,
      String _keyINP, String _gridID, String _PDFName, boolean _isDirectPrinting,
      String _windowName, String _baseDirection, boolean _debug, boolean _isSrcWindow,
      boolean _isFrame) {
    this(_conn, true, _language, _action, _isNew, _keyINP, _gridID, _PDFName, _isDirectPrinting,
        _windowName, _baseDirection, _debug, _isSrcWindow, _isFrame);
  }

  public ToolBar(ConnectionProvider _conn, String _language, String _action, boolean _isNew,
      String _keyINP, String _gridID, String _PDFName, boolean _isDirectPrinting,
      String _windowName, String _baseDirection, boolean _debug, boolean _isSrcWindow,
      boolean _isFrame, boolean _hasAttachements) {
    this(_conn, true, _language, _action, _isNew, _keyINP, _gridID, _PDFName, _isDirectPrinting,
        _windowName, _baseDirection, _debug, _isSrcWindow, _isFrame, _hasAttachements, true);
  }

  public ToolBar(ConnectionProvider _conn, boolean _isEditable, String _language, String _action,
      boolean _isNew, String _keyINP, String _gridID, String _PDFName, boolean _isDirectPrinting,
      String _windowName, String _baseDirection, boolean _debug) {
    this(_conn, _isEditable, _language, _action, _isNew, _keyINP, _gridID, _PDFName,
        _isDirectPrinting, _windowName, _baseDirection, _debug, false);
  }

  public ToolBar(ConnectionProvider _conn, boolean _isEditable, String _language, String _action,
      boolean _isNew, String _keyINP, String _gridID, String _PDFName, boolean _isDirectPrinting,
      String _windowName, String _baseDirection, boolean _debug, boolean _isSrcWindow) {
    this(_conn, _isEditable, _language, _action, _isNew, _keyINP, _gridID, _PDFName,
        _isDirectPrinting, _windowName, _baseDirection, _debug, _isSrcWindow, false);
  }

  public ToolBar(ConnectionProvider _conn, boolean _isEditable, String _language, String _action,
      boolean _isNew, String _keyINP, String _gridID, String _PDFName, boolean _isDirectPrinting,
      String _windowName, String _baseDirection, boolean _debug, boolean _isSrcWindow,
      boolean _isFrame) {
    this(_conn, _isEditable, _language, _action, _isNew, _keyINP, _gridID, _PDFName,
        _isDirectPrinting, _windowName, _baseDirection, _debug, _isSrcWindow, false, false, true);
  }

  public ToolBar(ConnectionProvider _conn, boolean _isEditable, String _language, String _action,
      boolean _isNew, String _keyINP, String _gridID, String _PDFName, boolean _isDirectPrinting,
      String _windowName, String _baseDirection, boolean _debug, boolean _isSrcWindow,
      boolean _isFrame, boolean _hasAttachments) {
    this(_conn, _isEditable, _language, _action, _isNew, _keyINP, _gridID, _PDFName,
        _isDirectPrinting, _windowName, _baseDirection, _debug, _isSrcWindow, _isFrame,
        _hasAttachments, true);
  }

  /**
   * Constructor used by the edit view of all generated windows.
   */
  public ToolBar(ConnectionProvider _conn, boolean _isEditable, String _language, String _action,
      boolean _isNew, String _keyINP, String _gridID, String _PDFName, boolean _isDirectPrinting,
      String _windowName, String _baseDirection, boolean _debug, boolean _isSrcWindow,
      boolean _isFrame, boolean _hasAttachments, boolean _hasNewButton) {
    this.conn = _conn;
    this.language = _language;
    this.servlet_action = _action;
    this.isNew = _isNew;
    this.keyfield = _keyINP;
    if (_gridID != null) {
      this.grid_id = _gridID;
    }
    if (_PDFName != null) {
      this.pdf = _PDFName;
    }
    this.isDirectPrint = _isDirectPrinting;
    this.base_direction = _baseDirection;
    this.debug = _debug;
    this.isFrame = _isFrame;
    this.isSrcWindow = _isSrcWindow;
    createAllButtons();
  }

  public void removeElement(String name) {
    if (name != null) {
      buttons.remove(name);
    }
  }

  private String getButtonScript(String name) {
    if (name.equals("EXCEL")) {
      return "openExcel('" + servlet_action + "_Excel.xls?Command=RELATION_XLS', '_blank');";
    } else if (name.equals("PRINT")) {
      return "openPDFSession('" + pdf + "', '" + (isDirectPrint ? "Printing" : "") + "', "
          + keyfield + ".name, "
          + ((grid_id == null || grid_id.equals("")) ? "null"
              : "dijit.byId('" + grid_id + "').getSelectedRows()")
          + ", " + ((grid_id == null || grid_id.equals("")) ? "true" : "null") + ");";
    } else if (name.equals("EMAIL")) {
      return "openPDFSession('" + pdf.replaceAll("print.html", "send.html") + "', '"
          + (isDirectPrint ? "Printing" : "") + "', " + keyfield + ".name, "
          + ((grid_id == null || grid_id.equals("")) ? "null"
              : "dijit.byId('" + grid_id + "').getSelectedRows()")
          + ", " + ((grid_id == null || grid_id.equals("")) ? "true" : "null") + ");";
    } else if (name.equals("GRID_VIEW")) {
      return "submitCommandForm('RELATION', isUserChanges, null, '" + servlet_action
              + (isSrcWindow ? "" : "_Relation") + ".html', '_self', null, true);";
    } else if (name.equals("FORM_VIEW")) {
      return "";
    } else {
      return "submitCommandForm('" + (name.equals("REFRESH") ? "DEFAULT" : name) + "', "
          + (name.equals("NEW") && (this.grid_id.equals("")) ? "true" : "false") + ", null, '"
          + servlet_action + (isSrcWindow ? "" : "_Relation") + ".html', '"
          + (isFrame ? "_parent" : "_self") + "', null, " + (debug ? "true" : "false") + ");";
    }
  }

  private void createAllButtons() {
    buttons.put("FIND", new ToolBar_Button(base_direction, "Find",
        Utility.messageBD(conn, "Find", language), getButtonScript("FIND")));
    buttons.put("SEPARATOR2", new ToolBar_Space(base_direction));
    buttons.put("SEPARATOR3", new ToolBar_Space(base_direction));
    buttons.put("SEPARATOR4", new ToolBar_Space(base_direction));

    if (Utility.isNewUI()) {
      buttons.put("REFRESH", new ToolBar_Button(base_direction, "Refresh",
          Utility.messageBD(conn, "Refresh", language), getButtonScript("REFRESH")));
    }

    buttons.put("EXCEL", new ToolBar_Button(base_direction, "Excel",
        Utility.messageBD(conn, "ExportExcel", language), getButtonScript("EXCEL")));

    if (pdf != null && !pdf.equals("") && !pdf.equals("..")) {
      buttons.put("PRINT", new ToolBar_Button(base_direction, "Print",
          Utility.messageBD(conn, "Print", language), getButtonScript("PRINT")));
      buttons.put("EMAIL", new ToolBar_Button(base_direction, "Email",
          Utility.messageBD(conn, "Email", language), getButtonScript("EMAIL")));
    }
    buttons.put("SEPARATOR5", new ToolBar_Space(base_direction));

    buttons.put("SEPARATOR6", new ToolBar_Space(base_direction));
    buttons.put("PREVIOUS_RELATION",
        new ToolBar_Button(base_direction, "Previous",
            Utility.messageBD(conn, "GotoPreviousRange", language),
            getButtonScript("PREVIOUS_RELATION")));
    buttons.put("PREVIOUS_RELATION_DISABLED", new ToolBar_Button(base_direction,
        "PreviousRangeDisabled", Utility.messageBD(conn, "GotoPreviousRange", language), ""));
    buttons.put("NEXT_RELATION", new ToolBar_Button(base_direction, "Next",
        Utility.messageBD(conn, "GotoNextRange", language), getButtonScript("NEXT_RELATION")));
    buttons.put("NEXT_RELATION_DISABLED", new ToolBar_Button(base_direction, "NextRangeDisabled",
        Utility.messageBD(conn, "GotoNextRange", language), ""));

    buttons.put("SEPARATOR7", new ToolBar_Space(base_direction));
    buttons.put("HR1", new ToolBar_HR());
  }

  public void prepareSimpleToolBarTemplate() {
    removeElement("FIND");
    removeElement("SEPARATOR2");
    removeElement("EXCEL");
    removeElement("PREVIOUS_RELATION");
    removeElement("PREVIOUS_RELATION_DISABLED");
    removeElement("NEXT_RELATION");
    removeElement("NEXT_RELATION_DISABLED");
    removeElement("EMAIL");
    removeElement("PRINT");
    if (pdf != null && !pdf.equals("") && !pdf.equals("..")) {
      buttons.put("PRINT", new ToolBar_Button(base_direction, "Print",
          Utility.messageBD(conn, "Print", language), pdf));

    }
    if (email) {
      buttons.put("EMAIL", new ToolBar_Button(base_direction, "Email",
          Utility.messageBD(conn, "Email", language), pdf));
    }
  }

  public void prepareRelationBarTemplate(boolean hasPrevious, boolean hasNext) {
    prepareRelationBarTemplate(hasPrevious, hasNext, "");
  }

  public void prepareRelationBarTemplate(boolean hasPrevious, boolean hasNext, String excelScript) {
    removeElement("FIND");
    removeElement("SEPARATOR2");
    removeElement("EXCEL");
    removeElement("EMAIL");
    removeElement("PRINT");

    removeElement(hasPrevious ? "PREVIOUS_RELATION_DISABLED" : "PREVIOUS_RELATION");
    removeElement(hasNext ? "NEXT_RELATION_DISABLED" : "NEXT_RELATION");

    if (pdf != null && !pdf.equals("") && !pdf.equals("..")) {
      buttons.put("PRINT", new ToolBar_Button(base_direction, "Print",
          Utility.messageBD(conn, "Print", language), pdf));

    }
    if (email) {
      buttons.put("EMAIL", new ToolBar_Button(base_direction, "Email",
          Utility.messageBD(conn, "Email", language), pdf));
    }
    if (!excelScript.equals("")) {
      buttons.put("EXCEL", new ToolBar_Button(base_direction, "Excel",
          Utility.messageBD(conn, "ExportExcel", language), excelScript));
    }
  }

  public void prepareSimpleExcelToolBarTemplate(String excelScript) {
    removeElement("FIND");
    removeElement("SEPARATOR2");
    removeElement("PREVIOUS_RELATION");
    removeElement("PREVIOUS_RELATION_DISABLED");
    removeElement("NEXT_RELATION");
    removeElement("NEXT_RELATION_DISABLED");

    if (!excelScript.equals("")) {
      buttons.put("EXCEL", new ToolBar_Button(base_direction, "Excel",
          Utility.messageBD(conn, "ExportExcel", language), excelScript));
    }
  }

  public void prepareQueryTemplate(boolean hasPrevious, boolean hasNext, boolean isTest) {
    if (!hasPrevious) {
      removeElement("PREVIOUS_RELATION");
    } else {
      removeElement("PREVIOUS_RELATION_DISABLED");
    }
    if (!hasNext) {
      removeElement("NEXT_RELATION");
    } else {
      removeElement("NEXT_RELATION_DISABLED");
    }
  }

  private String transformElementsToString(HTMLElement element, Vector<String> vecLastType) {
    Vector<String> localVecLastType = vecLastType;
    if (element == null) {
      return "";
    }
    if (localVecLastType == null) {
      localVecLastType = new Vector<String>(0);
    }
    final StringBuffer sbElement = new StringBuffer();
    String lastType = "";
    if (localVecLastType.size() > 0) {
      lastType = localVecLastType.elementAt(0);
    }
    if (lastType.equals("SPACE") && element.elementType().equals("SPACE")) {
      return "";
    }
    sbElement.append("<td ");
    if (element.elementType().equals("SPACE")) {
      sbElement.append("class=\"Main_ToolBar_Separator_cell\" ");
    } else if (!element.elementType().equals("HR")) {
      sbElement.append("width=\"").append(element.getWidth()).append("\" ");
    } else {
      sbElement.append("class=\"Main_ToolBar_Space\"");
    }
    sbElement.append(">");
    if (!element.elementType().equals("HR")) {
      sbElement.append(element);
    }
    sbElement.append("</td>\n");
    localVecLastType.clear();
    localVecLastType.addElement(element.elementType());
    return sbElement.toString();
  }

  @Override
  public String toString() {
    final StringBuffer toolbar = new StringBuffer();
    toolbar.append("<table class=\"Main_ContentPane_ToolBar Main_ToolBar_bg\" id=\"tdToolBar\">\n");
    toolbar.append("<tr>\n");
    final Vector<String> lastType = new Vector<String>(0);

    // In case of using new UI, add in toolbar grid and edition buttons
    if (Utility.isNewUI() && !isSrcWindow) {
      buttons.put("FORM_VIEW",
          new ToolBar_Button(base_direction, "Edition",
              Utility.messageBD(conn, "Form View", language), getButtonScript("FORM_VIEW"),
              true, "Edition" + (isNew ? "_new" : "")));
      buttons.put("GRID_VIEW",
          new ToolBar_Button(base_direction, "Relation",
              Utility.messageBD(conn, "Grid View", language), getButtonScript("GRID_VIEW"),
              false));
      buttons.put("SEPARATOR_NEWUI", new ToolBar_Space(base_direction));
      toolbar.append(transformElementsToString(buttons.get("FORM_VIEW"), lastType));
      toolbar.append(transformElementsToString(buttons.get("GRID_VIEW"), lastType));
      toolbar.append(transformElementsToString(buttons.get("SEPARATOR_NEWUI"), lastType));
    }

    toolbar.append(transformElementsToString(buttons.get("FIND"), lastType));
    toolbar.append(transformElementsToString(buttons.get("SEPARATOR2"), lastType));
    toolbar.append(transformElementsToString(buttons.get("SEPARATOR3"), lastType));
    toolbar.append(transformElementsToString(buttons.get("SEPARATOR4"), lastType));
    toolbar.append(transformElementsToString(buttons.get("REFRESH"), lastType));
    toolbar.append(transformElementsToString(buttons.get("EXCEL"), lastType));
    toolbar.append(transformElementsToString(buttons.get("PRINT"), lastType));
    toolbar.append(transformElementsToString(buttons.get("EMAIL"), lastType));
    toolbar.append(transformElementsToString(buttons.get("SEPARATOR5"), lastType));
    toolbar.append(transformElementsToString(buttons.get("SEPARATOR6"), lastType));
    toolbar.append(transformElementsToString(buttons.get("PREVIOUS_RELATION"), lastType));
    toolbar.append(transformElementsToString(buttons.get("PREVIOUS_RELATION_DISABLED"), lastType));
    toolbar.append(transformElementsToString(buttons.get("NEXT_RELATION"), lastType));
    toolbar.append(transformElementsToString(buttons.get("NEXT_RELATION_DISABLED"), lastType));
    toolbar.append(transformElementsToString(buttons.get("SEPARATOR7"), lastType));
    toolbar.append(transformElementsToString(buttons.get("HR1"), lastType));
    toolbar.append("</tr>\n");
    toolbar.append("</table>\n");
    return toolbar.toString();
  }
}
