/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
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
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.hqlinjections;

import java.util.Date;
import java.util.Map;

import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;

@ComponentProvider.Qualifier("D56CF1065EF14D52ADAD2AAB0CB63EFC")
public class TransactionsToMatchTransformer extends HqlQueryTransformer {

  @Override
  public String transformHqlQuery(String _hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    String transformedHql = _hqlQuery.replace("@whereclause@",
        getWhereClause(requestParameters, queryNamedParameters));
    transformedHql = transformedHql.replace("@selectClause@", " ");
    transformedHql = transformedHql.replace("@joinClause@", " ");
    return transformedHql;
  }

  protected String getWhereClause(Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    final String accountId = requestParameters.get("@FIN_Financial_Account.id@");
    final String bankStatementLineId = requestParameters.get("bankStatementLineId");

    Date date;
    try {
      OBContext.setAdminMode(true);
      date = OBDal.getInstance()
          .get(FIN_BankStatementLine.class, bankStatementLineId)
          .getTransactionDate();
    } catch (Exception e) {
      date = new Date();
    } finally {
      OBContext.restorePreviousMode();
    }

    final StringBuffer whereClause = new StringBuffer();
    whereClause.append("e.reconciliation is null ");
    whereClause.append("and e.account.id = :account  ");

    queryNamedParameters.put("account", accountId);
    queryNamedParameters.put("dateTo", date); // In HQL filter clause

    return whereClause.toString();
  }
}
