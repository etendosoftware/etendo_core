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

import org.openbravo.data.FieldProvider;
import org.openbravo.data.ScrollableFieldProvider;

/**
 * This is an abstract base class to help creation of code which allows to filter/process data in a
 * streaming fashion.
 * 
 * Example usage is adding more data while streaming data from an xsql-based data class to
 * JasperReports.
 * 
 * @author huehner
 * 
 */
public class AbstractScrollableFieldProviderFilter implements ScrollableFieldProvider {

  protected final ScrollableFieldProvider input;

  public AbstractScrollableFieldProviderFilter(ScrollableFieldProvider input) {
    this.input = input;
  }

  @Override
  public boolean hasData() {
    return input.hasData();
  }

  @Override
  public boolean next() throws ServletException {
    return input.next();
  }

  @Override
  public FieldProvider get() throws ServletException {
    return input.get();
  }

  @Override
  public void close() {
    input.close();
  }

}
