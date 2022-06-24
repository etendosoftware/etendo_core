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
 * All portions are Copyright (C) 2001-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.wad;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.data.FieldProvider;
import org.openbravo.data.Sqlc;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.utils.FormatUtilities;
import org.openbravo.wad.controls.WADControl;
import org.openbravo.xmlEngine.XmlEngine;

class WadUtility {
  private static final Logger log4j = LogManager.getLogger();

  // small cache to store mapping of <subRef + "-" + parentRef,classname>
  private static Map<String, String> referenceClassnameCache = new HashMap<String, String>();

  public static String getSQLWadContext(String code, Vector<Object> vecParameters) {
    if (code == null || code.trim().equals("")) {
      return "";
    }
    String token;
    String strValue = code;
    StringBuffer strOut = new StringBuffer();

    int i = strValue.indexOf("@");
    String strAux, strAux1;
    while (i != -1) {
      if (strValue.length() > (i + 5)
          && strValue.substring(i + 1, i + 5).equalsIgnoreCase("SQL=")) {
        strValue = strValue.substring(i + 5, strValue.length());
      } else {
        // Delete the chain symbol
        strAux = strValue.substring(0, i).trim();
        if (strAux.substring(strAux.length() - 1).equals("'")) {
          strAux = strAux.substring(0, strAux.length() - 1);
          strOut.append(strAux);
        } else {
          strOut.append(strValue.substring(0, i));
        }
        strAux1 = strAux;
        if (strAux.substring(strAux.length() - 1).equals("(")) {
          strAux = strAux.substring(0, strAux.length() - 1).toUpperCase().trim();
        }
        if (strAux.length() > 3
            && strAux.substring(strAux.length() - 3, strAux.length()).equals(" IN")) {
          strAux = " type=\"replace\" optional=\"true\" after=\"" + strAux1 + "\" text=\"'" + i
              + "'\"";
        } else {
          strAux = "";
        }
        strValue = strValue.substring(i + 1, strValue.length());

        int j = strValue.indexOf("@");
        if (j < 0) {
          return "";
        }

        token = strValue.substring(0, j);

        String modifier = ""; // holds the modifier (# or $) for the session value
        if (token.substring(0, 1).indexOf("#") > -1 || token.substring(0, 1).indexOf("$") > -1) {
          modifier = token.substring(0, 1);
          token = token.substring(1, token.length());
        }
        if (strAux.equals("")) {
          strOut.append("?");
        } else {
          strOut.append("'" + i + "'");
        }
        String parameter = "<Parameter name=\"" + token + "\"" + strAux + "/>";
        String paramElement[] = { parameter, modifier };
        vecParameters.addElement(paramElement);
        strValue = strValue.substring(j + 1, strValue.length());
        strAux = strValue.trim();
        if (strAux.length() > 0 && strAux.substring(0, 1).indexOf("'") > -1) {
          strValue = strAux.substring(1, strValue.length());
        }
      }
      i = strValue.indexOf("@");
    }
    strOut.append(strValue);
    return strOut.toString();
  }

  public static String getWadContext(String code, Vector<Object> vecFields,
      Vector<Object> vecAuxiliarFields, FieldsData[] parentsFieldsData, boolean isDefaultValue,
      String isSOTrx, String windowId) {
    if (code == null || code.trim().equals("")) {
      return "";
    }
    String token;
    String strValue = code;
    StringBuffer strOut = new StringBuffer();

    int i = strValue.indexOf("@");
    String strAux;
    while (i != -1) {
      if (strValue.length() > (i + 5)
          && strValue.substring(i + 1, i + 5).equalsIgnoreCase("SQL=")) {
        strValue = strValue.substring(i + 5, strValue.length());
      } else {
        strValue = strValue.substring(i + 1, strValue.length());

        int j = strValue.indexOf("@");
        if (j < 0) {
          return "";
        }

        token = strValue.substring(0, j);
        strAux = getWadContextTranslate(token, vecFields, vecAuxiliarFields, parentsFieldsData,
            isDefaultValue, isSOTrx, windowId, true);
        if (!strAux.trim().equals("") && strOut.toString().indexOf(strAux) == -1) {
          strOut.append(", " + strAux);
        }

        strValue = strValue.substring(j + 1, strValue.length());
      }
      i = strValue.indexOf("@");
    }
    return strOut.toString();
  }

  public static String getTextWadContext(String code, Vector<Object> vecFields,
      Vector<Object> vecAuxiliarFields, FieldsData[] parentsFieldsData, boolean isDefaultValue,
      String isSOTrx, String windowId) {
    if (code == null || code.trim().equals("")) {
      return "";
    }
    String token;
    String strValue = code;
    StringBuffer strOut = new StringBuffer();

    int h = strValue.indexOf(";");
    if (h != -1) {
      StringBuffer total = new StringBuffer();
      String strFirstElement = getTextWadContext(strValue.substring(0, h), vecFields,
          vecAuxiliarFields, parentsFieldsData, isDefaultValue, isSOTrx, windowId);
      total.append("(");
      if (strValue.substring(0, h).indexOf("@") == -1) {
        total.append("(\"");
      }
      total.append(strFirstElement);
      if (strValue.substring(0, h).indexOf("@") == -1) {
        total.append("\")");
      }
      total.append(".equals(\"\")?(");
      if (strValue.substring(h + 1).indexOf("@") == -1) {
        total.append("\"");
      }
      total.append(getTextWadContext(strValue.substring(h + 1), vecFields, vecAuxiliarFields,
          parentsFieldsData, isDefaultValue, isSOTrx, windowId));
      if (strValue.substring(h + 1).indexOf("@") == -1) {
        total.append("\"");
      }
      total.append("):(");
      if (strValue.substring(0, h).indexOf("@") == -1) {
        total.append("\"");
      }
      total.append(strFirstElement);
      if (strValue.substring(0, h).indexOf("@") == -1) {
        total.append("\"");
      }
      total.append("))");
      return total.toString();
    }

    int i = strValue.indexOf("@");
    while (i != -1) {
      strOut.append(strValue.substring(0, i));
      strValue = strValue.substring(i + 1, strValue.length());

      int j = strValue.indexOf("@");
      if (j < 0) {
        return "";
      }

      token = strValue.substring(0, j);
      strOut.append(getWadContextTranslate(token, vecFields, vecAuxiliarFields, parentsFieldsData,
          isDefaultValue, isSOTrx, windowId, true));

      strValue = strValue.substring(j + 1, strValue.length());

      i = strValue.indexOf("@");
    }
    strOut.append(strValue);
    return strOut.toString();
  }

  private static String transformFieldName(String field) {
    if (field == null || field.trim().equals("")) {
      return "";
    }
    int aux = field.toUpperCase().indexOf(" AS ");
    if (aux != -1) {
      return field.substring(aux + 3).trim();
    }
    aux = field.lastIndexOf(".");
    if (aux != -1) {
      return field.substring(aux + 1).trim();
    }

    return field.trim();
  }

  public static boolean findField(Vector<Object> vecFields, String field) {
    String strAux;
    for (int i = 0; i < vecFields.size(); i++) {
      strAux = transformFieldName((String) vecFields.elementAt(i));
      if (strAux.equalsIgnoreCase(field)) {
        return true;
      }
    }
    return false;
  }

  private static String getWadContextTranslate(String token, Vector<Object> vecFields,
      Vector<Object> vecAuxiliarFields, FieldsData[] parentsFieldsData, boolean isDefaultValue,
      String isSOTrx, String windowId, boolean dataMultiple) {
    if (token.substring(0, 1).indexOf("#") > -1 || token.substring(0, 1).indexOf("$") > -1) {
      if (token.equalsIgnoreCase("#DATE")) {
        return "DateTimeData.today(this)";
        // else return "vars.getSessionValue(\"" + token + "\")";
      } else {
        return "Utility.getContext(this, vars, \"" + token + "\", windowId)";
      }
    } else {
      String aux = Sqlc.TransformaNombreColumna(token);
      if (token.equalsIgnoreCase("ISSOTRX")) {
        return ("\"" + isSOTrx + "\"");
      }
      if (parentsFieldsData != null) {
        for (int i = 0; i < parentsFieldsData.length; i++) {
          if (parentsFieldsData[i].name.equalsIgnoreCase(token)) {
            return "strP" + parentsFieldsData[i].name;
          }
        }
      }
      if (!isDefaultValue) {
        if (vecFields != null && findField(vecFields, token)) {
          return (dataMultiple
              ? "((dataField!=null)?dataField.getField(\"" + aux
                  + "\"):((data==null || data.length==0)?\"\":data[0]."
              : "((data==null)?\"\":data.") + "getField(\"" + aux + "\")))";
        } else if (vecAuxiliarFields != null && findField(vecAuxiliarFields, token)) {
          return "str" + token;
        }
      }
      return "Utility.getContext(this, vars, \"" + token + "\", \"" + windowId + "\")";
    }
  }

  public static boolean isInVector(Vector<Object> vec, String field) {
    if (field == null || field.trim().equals("")) {
      return false;
    }
    for (int i = 0; i < vec.size(); i++) {
      String aux = (String) vec.elementAt(i);
      if (aux.equalsIgnoreCase(field)) {
        return true;
      }
    }
    return false;
  }

  private static void saveVectorField(Vector<Object> vec, String field) {
    if (field == null || field.trim().equals("")) {
      return;
    }
    if (!isInVector(vec, field)) {
      vec.addElement(field);
    }
  }

  public static String getComboReloadText(String token, Vector<Object> vecFields,
      FieldsData[] parentsFieldsData, Vector<Object> vecComboReload, String prefix) {
    return getComboReloadText(token, vecFields, parentsFieldsData, vecComboReload, prefix, "");
  }

  public static String getComboReloadText(String _token, Vector<Object> vecFields,
      FieldsData[] parentsFieldsData, Vector<Object> vecComboReload, String prefix,
      String columnname) {
    String token = _token;
    StringBuffer strOut = new StringBuffer();
    int i = token.indexOf("@");
    while (i != -1) {
      // strOut.append(token.substring(0,i));
      token = token.substring(i + 1);
      if (!token.startsWith("SQL")) {
        i = token.indexOf("@");
        if (i != -1) {
          String strAux = token.substring(0, i);
          token = token.substring(i + 1);
          if (!strOut.toString().trim().equals("")) {
            strOut.append(", ");
          }
          strOut.append(getComboReloadTextTranslate(strAux, vecFields, parentsFieldsData,
              vecComboReload, prefix, columnname));
        }
      }
      i = token.indexOf("@");
    }
    // strOut.append(token);
    return strOut.toString();
  }

  private static String getComboReloadTextTranslate(String token, Vector<Object> vecFields,
      FieldsData[] parentsFieldsData, Vector<Object> vecComboReload, String prefix,
      String columnname) {
    if (token == null || token.trim().equals("")) {
      return "";
    }
    if (!token.equalsIgnoreCase(columnname)) {
      saveVectorField(vecComboReload, token);
    }
    if (parentsFieldsData != null) {
      for (int i = 0; i < parentsFieldsData.length; i++) {
        if (parentsFieldsData[i].name.equalsIgnoreCase(token)) {
          return ((prefix.equals("")) ? ("\"" + parentsFieldsData[i].name + "\"")
              : ("\"" + prefix + Sqlc.TransformaNombreColumna(parentsFieldsData[i].name) + "\""));
        }
      }
    }
    if (vecFields != null && findField(vecFields, token)) {
      return ((prefix.equals("")) ? ("\"" + token + "\"")
          : ("\"" + prefix + Sqlc.TransformaNombreColumna(token) + "\""));
    }
    return ((prefix.equals("")) ? ("\"" + FormatUtilities.replace(token) + "\"")
        : ("\"" + prefix + Sqlc.TransformaNombreColumna(token) + "\""));
  }

  private static void setPropertyValue(Properties _prop, FieldProvider _field, String _name,
      String _fieldName, String _defaultValue) throws Exception {
    String aux = "";
    try {
      aux = _field.getField(_fieldName);
      if (aux == null || aux.equals("")) {
        aux = _defaultValue;
      }
    } catch (Exception ex) {
      if (_defaultValue == null) {
        throw new Exception("Inexistent field: " + _fieldName);
      } else {
        aux = _defaultValue;
      }
    }
    if (aux != null) {
      _prop.setProperty(_name, aux);
    }
  }

  public static WADControl getControl(ConnectionProvider conn, FieldProvider field,
      boolean isreadonly, String tabName, String adLanguage, XmlEngine xmlEngine,
      boolean isDisplayLogic, boolean isReloadObject, boolean isReadOnlyLogic,
      boolean hasParentsFields) throws Exception {
    return getControl(conn, field, isreadonly, tabName, adLanguage, xmlEngine, isDisplayLogic,
        isReloadObject, isReadOnlyLogic, hasParentsFields, false);
  }

  private static WADControl getControl(ConnectionProvider conn, FieldProvider field,
      boolean isreadonly, String tabName, String adLanguage, XmlEngine xmlEngine,
      boolean isDisplayLogic, boolean isReloadObject, boolean isReadOnlyLogic,
      boolean hasParentsFields, boolean isReadOnlyDefinedTab) throws Exception {
    if (field == null) {
      return null;
    }
    Properties prop = new Properties();
    setPropertyValue(prop, field, "ColumnName", "columnname", null);
    prop.setProperty("ColumnNameInp", Sqlc.TransformaNombreColumna(field.getField("columnname")));
    setPropertyValue(prop, field, "Name", "name", null);
    setPropertyValue(prop, field, "AD_Field_ID", "adFieldId", null);
    setPropertyValue(prop, field, "IsMandatory", "required", "N");
    setPropertyValue(prop, field, "AD_Reference_ID", "reference", null);
    setPropertyValue(prop, field, "ReferenceName", "referenceName", null);
    setPropertyValue(prop, field, "ReferenceNameTrl", "referenceNameTrl", "");
    setPropertyValue(prop, field, "AD_Reference_Value_ID", "referencevalue", "");
    setPropertyValue(prop, field, "AD_Val_Rule_ID", "adValRuleId", "");
    setPropertyValue(prop, field, "DisplayLength", "displaysize", "0");
    setPropertyValue(prop, field, "IsSameLine", "issameline", "N");
    setPropertyValue(prop, field, "IsDisplayed", "isdisplayed", "N");
    setPropertyValue(prop, field, "IsUpdateable", "isupdateable", "N");
    setPropertyValue(prop, field, "IsParent", "isparent", "N");
    setPropertyValue(prop, field, "FieldLength", "fieldlength", "0");
    setPropertyValue(prop, field, "AD_Column_ID", "adColumnId", "null");
    setPropertyValue(prop, field, "ColumnNameSearch", "realname", "");
    setPropertyValue(prop, field, "SearchName", "searchname", "");
    setPropertyValue(prop, field, "AD_CallOut_ID", "adCalloutId", "");
    setPropertyValue(prop, field, "ValidateOnNew", "validateonnew", "Y");
    setPropertyValue(prop, field, "CallOutName", "calloutname", "");
    setPropertyValue(prop, field, "CallOutMapping", "mappingnameCallout", "");
    setPropertyValue(prop, field, "CallOutClassName", "classnameCallout", "");
    setPropertyValue(prop, field, "AD_Process_ID", "adProcessId", "");
    setPropertyValue(prop, field, "IsReadOnly", "isreadonly", "N");
    setPropertyValue(prop, field, "DisplayLogic", "displaylogic", "");
    setPropertyValue(prop, field, "IsEncrypted", "isencrypted", "N");
    setPropertyValue(prop, field, "AD_FieldGroup_ID", "fieldgroup", "");
    setPropertyValue(prop, field, "AD_Tab_ID", "tabid", null);
    setPropertyValue(prop, field, "ValueMin", "valuemin", "");
    setPropertyValue(prop, field, "ValueMax", "valuemax", "");
    setPropertyValue(prop, field, "MappingName", "javaClassName", "");
    setPropertyValue(prop, field, "IsColumnEncrypted", "iscolumnencrypted", "");
    setPropertyValue(prop, field, "IsDesencryptable", "isdesencryptable", "");
    setPropertyValue(prop, field, "ReadOnlyLogic", "readonlylogic", "");
    setPropertyValue(prop, field, "IsAutosave", "isautosave", "Y");
    prop.setProperty("TabName", tabName);
    prop.setProperty("IsReadOnlyTab", (isreadonly ? "Y" : "N"));
    prop.setProperty("AD_Language", adLanguage);
    prop.setProperty("IsDisplayLogic", (isDisplayLogic ? "Y" : "N"));
    prop.setProperty("IsReadOnlyLogic", (isReadOnlyLogic ? "Y" : "N"));
    prop.setProperty("IsComboReload", (isReloadObject ? "Y" : "N"));
    prop.setProperty("isReadOnlyDefinedTab", (isReadOnlyDefinedTab ? "Y" : "N"));
    prop.setProperty("hasParentsFields", (hasParentsFields ? "Y" : "N"));

    WADControl _myClass = getWadControlClass(conn, field.getField("AD_Reference_ID"),
        field.getField("AD_Reference_Value_ID"));

    _myClass.setReportEngine(xmlEngine);
    _myClass.setInfo(prop);
    _myClass.initialize();

    return _myClass;
  }

  /**
   * Obtains an instance of the WAD implementator for the reference passed as parameter
   */
  public static WADControl getWadControlClass(ConnectionProvider conn, String parentRef,
      String subRef) {
    String classname;
    WADControl control;

    try {
      // lookup value from cache, if not found, search in db and put into cache
      String cacheKey = subRef + "-" + parentRef;
      classname = referenceClassnameCache.get(cacheKey);
      if (classname == null) {
        classname = WadUtilityData.getReferenceClassName(conn, subRef, parentRef);
        referenceClassnameCache.put(cacheKey, classname);
      }
    } catch (ServletException e1) {
      log4j.warn("Couldn't find reference classname ref " + parentRef + ", subRef " + subRef, e1);
      return new WADControl();
    }

    if (classname == null || classname.isEmpty()) {
      control = new WADControl();
      log4j.debug("Class no defined for reference " + parentRef + " - subreference:" + subRef);
    } else {
      try {
        Class<?> c = Class.forName(classname);
        control = (WADControl) c.getDeclaredConstructor().newInstance();
        control.setReference(parentRef);
        control.setSubreference(subRef);
      } catch (ClassNotFoundException ex) {
        log4j.warn("Couldn't find class: " + classname + " - reference: " + parentRef
            + " - subreference:" + subRef);
        control = new WADControl();
      } catch (InstantiationException e) {
        log4j.warn("Couldn't instanciate class: " + classname);
        control = new WADControl();
      } catch (IllegalAccessException e) {
        log4j.warn("Illegal access class: " + classname);
        control = new WADControl();
      } catch (InvocationTargetException e) {
        log4j.warn("Exception thrown by default constructor of class: " + classname);
        control = new WADControl();
      } catch (NoSuchMethodException e) {
        log4j.warn("Could not find a default constructor for class: " + classname);
        control = new WADControl();
      }
    }
    return control;
  }

  public static void writeFile(File path, String filename, String text) throws IOException {
    File fileData = new File(path, filename);
    FileOutputStream fileWriterData = new FileOutputStream(fileData);
    OutputStreamWriter printWriterData = new OutputStreamWriter(fileWriterData, "UTF-8");
    printWriterData.write(text);
    printWriterData.flush();
    fileWriterData.close();
  }

  /**
   * Returns a where parameter, this parameter can contain a modifier to decide which level of
   * session value is (# or $).
   * <p>
   * This method returns the parameter applying the modifier if exists. It can return the complete
   * parameter to be used in xsql files or just the name for the parameter with the modifier.
   * 
   * @param parameter
   *          parameter for the where clause to parse
   * @param complete
   *          return the complete parameter or just the name
   * @return the paresed parameter
   */
  public static String getWhereParameter(Object parameter, boolean complete) {
    String strParam = "";
    if (parameter instanceof String) {
      // regular parameter without modifier
      strParam = (String) parameter;
      if (!complete) {
        strParam = strParam.substring(17, strParam.lastIndexOf("\""));
      }
    } else {
      // parameter with modifier, used for session values (#, $)
      String paramElement[] = (String[]) parameter;
      if (complete) {
        strParam = paramElement[0];
      } else {
        strParam = paramElement[1]
            + paramElement[0].substring(17, paramElement[0].lastIndexOf("\""));
      }
    }
    return strParam;
  }
}
