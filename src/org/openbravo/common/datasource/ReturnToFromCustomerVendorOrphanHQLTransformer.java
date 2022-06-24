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

package org.openbravo.common.datasource;

import java.util.Map;

import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;

@ComponentProvider.Qualifier("CEA5D55DB2AC497D98B9C02A5E1EA0B6")
public class ReturnToFromCustomerVendorOrphanHQLTransformer extends HqlQueryTransformer {

  private static final String unitPriceProperty = "unitPrice";
  private static final String grossUnitPriceProperty = "grossUnitPrice";

  private static final String returnReasonLeftClause = " coalesce((select oli.returnReason.id from OrderLine as oli where oli.salesOrder.id = :salesOrderId  and oli.id=ol.id), '')";
  private static final String returnReasonCountQuery = " select count(distinct e.name) from ReturnReason as e where exists (select distinct ol.returnReason from OrderLine as ol where ol.returnReason = e and ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine is null) ";
  private static final String returnReasonDataQuery = " select distinct e, e.name from ReturnReason as e where exists (select distinct ol.returnReason from OrderLine as ol where ol.returnReason = e and ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine is null) ";

  @Override
  public String transformHqlQuery(String hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    // Sets the named parameters
    String salesOrderId = requestParameters.get("@Order.id@");
    queryNamedParameters.put("salesOrderId", salesOrderId);

    String transformedHqlQuery = hqlQuery;
    Order order = OBDal.getInstance().get(Order.class, salesOrderId);
    if (order.getPriceList().isPriceIncludesTax()) {
      transformedHqlQuery = transformedHqlQuery.replaceAll("@unitPriceProperty@",
          grossUnitPriceProperty);
    } else {
      transformedHqlQuery = transformedHqlQuery.replaceAll("@unitPriceProperty@",
          unitPriceProperty);
    }
    String distinctProperty = requestParameters.get("_distinct");
    if ("returnReason".equals(distinctProperty)) {
      // Uses custom queries for the return reason column
      String justCount = requestParameters.get("_justCount");
      if ("true".equals(justCount)) {
        transformedHqlQuery = returnReasonCountQuery;
      } else {
        transformedHqlQuery = returnReasonDataQuery;
      }
    } else {
      transformedHqlQuery = transformedHqlQuery.replace("@returnReasonLeftClause@.id",
          returnReasonLeftClause);
    }
    if (distinctProperty != null) {
      transformedHqlQuery = transformedHqlQuery.replace("ORDER BY obSelected des", "ORDER BY");
    }
    return transformedHqlQuery;
  }

}
