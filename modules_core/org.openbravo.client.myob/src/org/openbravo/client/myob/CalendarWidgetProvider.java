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
 * All portions are Copyright (C) 2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.myob;

import org.openbravo.client.kernel.KernelConstants;

/**
 * Responsible for creating the Calendar Widget.
 * 
 * @author dbaz
 */
public class CalendarWidgetProvider extends WidgetProvider {

  public static final String WIDGETCLASS_PARAMETER = "WIDGET_CLASS";

  @Override
  public String generate() {
    final String result = "isc.defineClass('" + KernelConstants.ID_PREFIX + getWidgetClass().getId()
        + "', isc.OBCalendarWidget).addProperties({widgetId: '" + getWidgetClass().getId() + "'});";
    return result;
  }

}
