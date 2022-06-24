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
package org.openbravo.common.inserters;

import java.util.Map;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.util.Check;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.materialmgmt.cost.LCMatched;
import org.openbravo.model.materialmgmt.cost.LandedCostType;
import org.openbravo.service.datasource.hql.HQLInserterQualifier;
import org.openbravo.service.datasource.hql.HqlInserter;

@HQLInserterQualifier.Qualifier(tableId = "B2960E2BDCCD4F7599A2433F2681847F", injectionId = "0")
public class LCMatchFromInvoiceInserter extends HqlInserter {

  @Override
  public String insertHql(Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    final String strInvoiceLineID = requestParameters.get("@InvoiceLine.id@");
    Check.isTrue(IsIDFilter.instance.accept(strInvoiceLineID),
        "Value " + strInvoiceLineID + " is not a valid id.");
    String strWhereClause = " (il is null or il.id = :invlineid) ";
    queryNamedParameters.put("invlineid", strInvoiceLineID);

    InvoiceLine invLine = OBDal.getInstance().get(InvoiceLine.class, strInvoiceLineID);
    if (invLine.getProduct() != null) {
      strWhereClause += " and lct." + LandedCostType.PROPERTY_PRODUCT + ".id = :product ";
      queryNamedParameters.put("product", invLine.getProduct().getId());
    }

    if (invLine.getAccount() != null) {
      strWhereClause += " and lct." + LandedCostType.PROPERTY_ACCOUNT + ".id = :glitem ";
      queryNamedParameters.put("glitem", invLine.getAccount().getId());
    }
    strWhereClause += " and ( lcm is null or lcm." + LCMatched.PROPERTY_ISCONVERSIONMATCHING
        + "=false) ";
    return strWhereClause;
  }
}
