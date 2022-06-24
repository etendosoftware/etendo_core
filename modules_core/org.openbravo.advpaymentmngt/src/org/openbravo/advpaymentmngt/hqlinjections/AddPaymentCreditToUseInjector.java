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
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.hqlinjections;

import java.util.Map;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.service.datasource.hql.HQLInserterQualifier;
import org.openbravo.service.datasource.hql.HqlInserter;

@HQLInserterQualifier.Qualifier(tableId = "59ED9B23854A4B048CBBAE38436B99C2", injectionId = "0")
public class AddPaymentCreditToUseInjector extends HqlInserter {

  @Override
  public String insertHql(Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    final String strBusinessPartnerId = requestParameters.get("received_from");
    boolean isSalesTransaction = "true".equals(requestParameters.get("issotrx")) ? true : false;
    if (strBusinessPartnerId != null) {
      final BusinessPartner businessPartner = OBDal.getInstance()
          .get(BusinessPartner.class, strBusinessPartnerId);
      if (businessPartner != null) {
        queryNamedParameters.put("bp", businessPartner.getId());
      } else {
        // If there is no bp no credit is available
        queryNamedParameters.put("bp", "-1");
      }
    } else {
      queryNamedParameters.put("bp", "-1");
    }
    queryNamedParameters.put("issotrx", isSalesTransaction);
    return "f.businessPartner.id = :bp and f.receipt = :issotrx";
  }
}
