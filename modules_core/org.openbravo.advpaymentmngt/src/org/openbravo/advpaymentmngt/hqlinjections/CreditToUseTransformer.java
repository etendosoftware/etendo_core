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
 * All portions are Copyright (C) 2014-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.advpaymentmngt.hqlinjections;

import java.util.Map;

import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;

@ComponentProvider.Qualifier("59ED9B23854A4B048CBBAE38436B99C2")
public class CreditToUseTransformer extends HqlQueryTransformer {

  @Override
  public String transformHqlQuery(String hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {

    String transformedHQL = hqlQuery.replace("@selectClause@", " ");
    transformedHQL = transformedHQL.replace("@joinClause@", " ");
    transformedHQL = transformedHQL.replace("@whereClause@",
        getWhereClause(requestParameters, queryNamedParameters));

    // Sets parameters
    queryNamedParameters.put("currencyId", requestParameters.get("c_currency_id"));

    return transformedHQL;
  }

  private CharSequence getWhereClause(Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    StringBuffer whereClause = new StringBuffer();
    whereClause.append(" and f.currency.id = :currencyId ");
    return whereClause;
  }
}
