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
 * All portions are Copyright (C) 2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.myob;

/**
 * This class is used to keep information about the definition of a WidgetClass.
 */
class WidgetClassInfo {
  private WidgetProvider widgetProvider;
  private String widgetClassProperties;
  private String widgetClassDefinition;

  public WidgetClassInfo(WidgetProvider widgetProvider) {
    this.widgetProvider = widgetProvider;
  }

  /**
   * @return the WidgetProvider used to retrieve the information of a particular WidgetClass.
   */
  public WidgetProvider getWidgetProvider() {
    return widgetProvider;
  }

  /**
   * @return a String containing the WidgetClass properties.
   */
  public String getWidgetClassProperties() {
    if (widgetClassProperties == null) {
      widgetClassProperties = widgetProvider.getWidgetClassDefinition().toString();
    }
    return widgetClassProperties;
  }

  /**
   * This method returns the definition of those widget classes whose definition is generated at
   * runtime. For those widget classes whose definition is loaded through a js file this method
   * returns an empty String.
   * 
   * @return a String with the generated definition of the WidgetClass or an empty String if the
   *         widget class definition is loaded through a js file.
   */
  public String getWidgetClassDefinition() {
    if (widgetClassDefinition == null) {
      widgetClassDefinition = "";
      try {
        widgetClassDefinition = widgetProvider.generate();
        // remove ending semicolon to avoid errors when evaluating the widget class definition on
        // the client side
        widgetClassDefinition = widgetClassDefinition.substring(0,
            widgetClassDefinition.length() - 1);
      } catch (Exception e) {
        // The widget provider does not support the generate operation. This means that the widget
        // class definition will be completely created as part of the static Javascript content
      }
    }
    return widgetClassDefinition;
  }
}
