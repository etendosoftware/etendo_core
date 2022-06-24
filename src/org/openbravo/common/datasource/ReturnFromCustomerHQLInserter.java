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
 * All portions are Copyright (C) 2014-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.common.datasource;

import java.util.Map;

import org.openbravo.service.datasource.hql.HQLInserterQualifier;
import org.openbravo.service.datasource.hql.HqlInserter;

@HQLInserterQualifier.Qualifier(tableId = "CDB9DC9655F24DF8AB41AA0ADBD04390", injectionId = "0")
/**
 * HQL Inserter for the HQL table shared by the Return From Customer and Return To Vendor P&E
 * windows It is used to exclude the discounts for the Return From Customer P&E window
 *
 */
public class ReturnFromCustomerHQLInserter extends HqlInserter {

  private final static String RETURN_FROM_CUSTOMER_TAB_ID = "AF4090093CFF1431E040007F010048A5";

  @Override
  public String insertHql(Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {

    // if the table is being used in the Return From Customer tab, add a filter to exclude the
    // discounts
    String buttonOwnerViewTabId = requestParameters.get("buttonOwnerViewTabId");
    if (RETURN_FROM_CUSTOMER_TAB_ID.equals(buttonOwnerViewTabId)) {
      return " not exists (select 1 from OrderLine e where e.id = iol.salesOrderLine.id and e.orderDiscount.id is not null) ";
    } else {
      return null;
    }
  }
}
