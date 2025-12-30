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
 * <p>
 * This class provides a simplified interface for calling database stored procedures.
 * It delegates all logic to {@link CallProcess#executeRaw(String, List, List, boolean, boolean)}.
 * </p>
 * <p>
 * This class is kept primarily for backward compatibility with older versions of the system.
 * New code should prefer using {@link CallProcess} directly.
 * </p>
 *
 * @author Openbravo
 * @author etendo
 * @see CallProcess
 */
public class CallStoredProcedure {

  private static CallStoredProcedure instance = new CallStoredProcedure();

  /**
   * Returns the singleton instance of {@code CallStoredProcedure}.
   *
   * @return the singleton instance.
   */
  public static synchronized CallStoredProcedure getInstance() {
    return instance;
  }

  /**
   * Sets the singleton instance of {@code CallStoredProcedure}.
   *
   * @param instance
   *     the instance to set.
   */
  public static synchronized void setInstance(CallStoredProcedure instance) {
    CallStoredProcedure.instance = instance;
  }

  /**
   * Executes a stored procedure with the given name and parameters. Delegates to
   * {@link #call(String, List, List, boolean, boolean)} with default values.
   * 
   * @param name
   *          the name of the stored procedure to execute
   * @param parameters
   *          the list of parameters to pass to the stored procedure
   * @param types
   *          the list of Java types corresponding to the parameters
   * @return the result of the stored procedure execution
   */
  public Object call(String name, List<Object> parameters, List<Class<?>> types) {
    return call(name, parameters, types, true, true);
  }

  /**
   * Executes a stored procedure with the given name and parameters, allowing control over session
   * flushing. Delegates to {@link #call(String, List, List, boolean, boolean)} with default values.
   * 
   * @param name
   *          the name of the stored procedure to execute
   * @param parameters
   *          the list of parameters to pass to the stored procedure
   * @param types
   *          the list of Java types corresponding to the parameters
   * @param doFlush
   *          whether to flush the current session before executing the stored procedure
   * @return the result of the stored procedure execution
   */
  public Object call(String name, List<Object> parameters, List<Class<?>> types, boolean doFlush) {
    return call(name, parameters, types, doFlush, true);
  }

  /**
   * Executes a stored procedure with the given name and parameters, allowing full control over
   * execution options. This method delegates the actual execution to
   * {@link CallProcess#executeRaw(String, List, List, boolean, boolean)}.
   * 
   * @param name
   *          the name of the stored procedure to execute
   * @param parameters
   *          the list of parameters to pass to the stored procedure
   * @param types
   *          the list of Java types corresponding to the parameters
   * @param doFlush
   *          whether to flush the current session before executing the stored procedure
   * @param returnResults
   *          whether to return the results of the stored procedure execution
   * @return the result of the stored procedure execution, or null if returnResults is false
   */
  public Object call(String name, List<Object> parameters, List<Class<?>> types, boolean doFlush,
      boolean returnResults) {

    return CallProcess.getInstance().executeRaw(name, parameters, types, doFlush, returnResults);
  }
}
