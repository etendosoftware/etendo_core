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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.hqlinjections;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openbravo.advpaymentmngt.dao.MatchTransactionDao;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;

@ComponentProvider.Qualifier("BC21981DCF0846338D631887BEDFE7FA")
public class MatchStatementTransformer extends HqlQueryTransformer {

  @Override
  public String transformHqlQuery(String _hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    String transformedHql = _hqlQuery.replace("@whereClause@",
        getWhereClause(requestParameters, queryNamedParameters));
    transformedHql = transformedHql.replace("@selectClause@", " ");
    transformedHql = transformedHql.replace("@joinClause@", " ");

    final boolean isOrder = StringUtils.containsIgnoreCase(_hqlQuery, "order by");
    transformedHql = transformedHql.replace("@orderby@",
        isOrder ? " " : getDefaultOrderByClause(requestParameters, queryNamedParameters));
    return transformedHql;
  }

  protected String getWhereClause(Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    final String financialAccountId = requestParameters.get("@FIN_Financial_Account.id@");
    final StringBuffer whereClause = new StringBuffer();
    if (StringUtils.isNotBlank(financialAccountId)) {
      try {
        OBContext.setAdminMode(true);
        final FIN_FinancialAccount finAccount = OBDal.getInstance()
            .get(FIN_FinancialAccount.class, financialAccountId);
        final FIN_Reconciliation lastReconciliation = TransactionsDao
            .getLastReconciliation(finAccount, "N");

        whereClause.append(" (fat is null ");

        if (lastReconciliation != null) {
          whereClause.append("            or fat.reconciliation.id = :reconciliation ");
          queryNamedParameters.put("reconciliation", lastReconciliation.getId());
        }
        whereClause.append(" ) ");

        whereClause.append(" and bs.account.id = :account ");
        queryNamedParameters.put("account", finAccount.getId());

        if (lastReconciliation != null
            && !MatchTransactionDao.islastreconciliation(lastReconciliation)) {
          whereClause.append(" and bsl.transactionDate <= :endingdate ");
          queryNamedParameters.put("endingdate", lastReconciliation.getEndingDate());
        }
      } finally {
        OBContext.restorePreviousMode();
      }
    }

    return whereClause.toString();
  }

  protected String getDefaultOrderByClause(Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    return "order by banklineDate, lineNo";
  }
}
