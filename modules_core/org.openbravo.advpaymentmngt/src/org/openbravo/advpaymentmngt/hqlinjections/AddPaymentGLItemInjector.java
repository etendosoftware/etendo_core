package org.openbravo.advpaymentmngt.hqlinjections;

import java.util.Map;

import org.openbravo.service.datasource.hql.HQLInserterQualifier;
import org.openbravo.service.datasource.hql.HqlInserter;

@HQLInserterQualifier.Qualifier(tableId = "864A35C8FCD548B0AD1D69C89BBA6118", injectionId = "0")
public class AddPaymentGLItemInjector extends HqlInserter {

  @Override
  public String insertHql(Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    final String strPaymentId = requestParameters.get("fin_payment_id");
    queryNamedParameters.put("pid", strPaymentId);
    return "p.id = :pid";
  }
}
