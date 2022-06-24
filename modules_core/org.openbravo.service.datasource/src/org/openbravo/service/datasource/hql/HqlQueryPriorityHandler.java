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
 * All portions are Copyright (C) 2014 Openbravo SLU 
 * All Rights Reserved. 
 ************************************************************************
 */
package org.openbravo.service.datasource.hql;

import java.util.Map;

import org.openbravo.service.datasource.HQLDataSourceService;

/**
 * Defines the priority of the injectors, so in case there are more than one with the same
 * qualifier, the one with lowest priority is selected.
 * {@link HQLDataSourceService#getData(Map, int, int)}
 * 
 */

public abstract class HqlQueryPriorityHandler {
  /**
   * Returns the priority of this handler based on the parameters of the request
   * 
   * @param parameters
   *          the parameters of the request
   * @return the priority of this injector
   */
  public int getPriority(Map<String, String> parameters) {
    return 100;
  }
}
