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
 * All portions are Copyright (C) 2009-2011 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.db;

import java.util.List;

/**
 * Facade for executing Stored Procedures.
 * Delegates all logic to {@link CallProcess#executeRaw}.
 * Kept for backward compatibility.
 */
public class CallStoredProcedure {

  private static CallStoredProcedure instance = new CallStoredProcedure();

  public static synchronized CallStoredProcedure getInstance() {
    return instance;
  }

  public static synchronized void setInstance(CallStoredProcedure instance) {
    CallStoredProcedure.instance = instance;
  }

  /**
   * Delegates to CallProcess.
   */
  public Object call(String name, List<Object> parameters, List<Class<?>> types) {
    return call(name, parameters, types, true, true);
  }

  /**
   * Delegates to CallProcess.
   */
  public Object call(String name, List<Object> parameters, List<Class<?>> types, boolean doFlush) {
    return call(name, parameters, types, doFlush, true);
  }

  /**
   * Delegates to CallProcess.
   */
  public Object call(String name, List<Object> parameters, List<Class<?>> types, boolean doFlush,
      boolean returnResults) {

    return CallProcess.getInstance().executeRaw(name, parameters, types, doFlush, returnResults);
  }
}
