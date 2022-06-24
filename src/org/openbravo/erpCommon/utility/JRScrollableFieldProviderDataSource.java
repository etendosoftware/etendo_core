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
package org.openbravo.erpCommon.utility;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.data.ScrollableFieldProvider;

import net.sf.jasperreports.engine.JRException;

/**
 * This class allows to use a class implementing the {@link ScrollableFieldProvider} interface to be
 * used as a JRDatasource consumable by JapserReports.
 * 
 * @author huehner
 * 
 */
public class JRScrollableFieldProviderDataSource extends JRFieldProviderDataSource {
  private static final Logger log = LogManager.getLogger();

  private final ScrollableFieldProvider input;

  public JRScrollableFieldProviderDataSource(ScrollableFieldProvider fp, String strDateFormat) {
    super(strDateFormat);
    this.input = fp;
  }

  @Override
  public boolean next() throws JRException {
    try {
      if (input.next()) {
        oneDataRow = input.get();
        return true;
      }
    } catch (ServletException e) {
      log.error("Error getting more data: ", e);
      // re-throw with original message text to get that text out to the user
      throw new JRException(e.getMessage(), e);
    }
    return false;
  }

}
