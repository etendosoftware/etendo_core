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
 * All portions are Copyright (C) 2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.dal.core;

import java.util.Map;

import org.hibernate.dialect.function.SQLFunction;

/**
 * An interface that must be implemented by those classes intended to register SQL functions which
 * are pretended to be used in HQL.
 */
public interface SQLFunctionRegister {

  /**
   * This method is executed by the {@link DalSessionFactoryController} to retrieve SQL functions
   * that should be registered in Hibernate.
   * 
   * @return A Map with SQL functions to be registered in Hibernate.
   */
  public Map<String, SQLFunction> getSQLFunctions();

}
