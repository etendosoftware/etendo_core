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

import javax.enterprise.context.ApplicationScoped;

/**
 * A HqlQueryTransformer is able to transform an HQL query. HqlTransformer are instantiated using
 * dependency injection.
 */

@ApplicationScoped
public abstract class HqlQueryTransformer extends HqlQueryPriorityHandler {
  /**
   * Returns some code to be injected in a HQL query, and adds query named parameters when needed
   * 
   * @param requestParameters
   *          the parameters of the request. The injected code may vary depending on these
   *          parameters
   * @param queryNamedParameters
   *          the named parameters of the hql query that will be used to fetch the table data. If
   *          the injected code uses named parameters, the named parameters must be added to this
   *          map
   * @return the hql code to be injected
   */

  /**
   * Returns the transformed hql query
   * 
   * @param hqlQuery
   *          original hql query
   * @param requestParameters
   *          the parameters of the request
   * @param queryNamedParameters
   *          the named parameters of the hql query that will be used to fetch the table data. If
   *          the transformed hql query uses named parameters that did not exist in the original hql
   *          query, the named parameters must be added to this map
   * @return the transformed hql query
   */
  public abstract String transformHqlQuery(String hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters);
}
