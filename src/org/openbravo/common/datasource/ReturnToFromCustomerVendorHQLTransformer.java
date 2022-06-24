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
 * All portions are Copyright (C) 2014-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.common.datasource;

import java.util.Map;

import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;

@ComponentProvider.Qualifier("CDB9DC9655F24DF8AB41AA0ADBD04390")
public class ReturnToFromCustomerVendorHQLTransformer extends HqlQueryTransformer {

  private static final String returnToVendorTabId = "5A5CCFC8359B4D79BA705DC487FE8173";

  private static final String rtv_unitPriceLeftClause = "(case when (select e.salesOrderLine.salesOrder.priceList.priceIncludesTax from ProcurementPOInvoiceMatch as e where e.goodsShipmentLine.id = iol.id) = true then  coalesce((select ol.unitPrice from OrderLine as ol where ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine.id = iol.id), (select e.salesOrderLine.grossUnitPrice from ProcurementPOInvoiceMatch as e where e.goodsShipmentLine.id = iol.id)) else   coalesce((select ol.unitPrice from OrderLine as ol where ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine.id = iol.id), (select e.salesOrderLine.unitPrice from ProcurementPOInvoiceMatch as e where e.goodsShipmentLine.id = iol.id)) end)";
  private static final String rtv_orderNoLeftClause = " coalesce ((select e.salesOrderLine.salesOrder.documentNo from ProcurementPOInvoiceMatch as e where e.goodsShipmentLine.id = iol.id), '')";
  private static final String rfc_unitPriceLeftClause = "(case when (iol.salesOrderLine.salesOrder.priceList.priceIncludesTax) = true then  coalesce((select ol.unitPrice from OrderLine as ol where ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine.id = iol.id), (iol.salesOrderLine.grossUnitPrice)) else   coalesce((select ol.unitPrice from OrderLine as ol where ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine.id = iol.id), (coalesce(iol.salesOrderLine.unitPrice,0))) end)";
  private static final String rfc_orderNoLeftClause = " coalesce((select e.salesOrderLine.salesOrder.documentNo from MaterialMgmtShipmentInOutLine as e where e.id = iol.id), '')";
  private static final String returnedLeftClause = " coalesce((select ol.orderedQuantity from OrderLine as ol where ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine.id = iol.id),0)";
  private static final String returnedOthersLeftClause = " coalesce((select sum(ol.orderedQuantity) from OrderLine as ol join ol.salesOrder as o where ol.goodsShipmentLine.id = iol.id and o.processed = true and o.documentStatus <> 'VO'), 0)";
  private static final String returnReasonLeftClause = " coalesce((select ol.returnReason.id from OrderLine as ol where ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine.id = iol.id), '')";
  private static final String returnReasonLeftClause2 = " coalesce((select ol.returnReason.name from OrderLine as ol where ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine.id = iol.id), '')";
  private static final String returnReasonCountQuery = " select count(distinct e.name) from ReturnReason as e where exists (select distinct ol.returnReason from OrderLine as ol where ol.returnReason = e and ol.salesOrder.id = :salesOrderId  and ol.goodsShipmentLine is not null) ";
  private static final String returnReasonDataQuery = " select distinct e, e.name from ReturnReason as e where exists (select distinct ol.returnReason from OrderLine as ol where ol.returnReason = e and ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine is not null) ";
  private static final String unitPriceProperty = "unitPrice";
  private static final String grossUnitPriceProperty = "grossUnitPrice";

  private static final String rtv_orderNo = "(select e.salesOrderLine.salesOrder.documentNo from ProcurementPOInvoiceMatch as e where e.goodsShipmentLine.id = iol.id)";
  private static final String rfc_orderNo = "(select e.salesOrderLine.salesOrder.documentNo from MaterialMgmtShipmentInOutLine as e where e.id = iol.id)";
  private static final String rtv_unitPrice = "(select e.salesOrderLine.unitPrice from ProcurementPOInvoiceMatch as e where e.goodsShipmentLine.id = iol.id)";
  private static final String rfc_unitPrice = "(select e.salesOrderLine.unitPrice from MaterialMgmtShipmentInOutLine as e where e.id = iol.id)";
  private static final String rtv_grossUnitPrice = "(select e.salesOrderLine.grossUnitPrice from ProcurementPOInvoiceMatch as e where e.goodsShipmentLine.id = iol.id)";
  private static final String rfc_grossUnitPrice = "(select e.salesOrderLine.grossUnitPrice from MaterialMgmtShipmentInOutLine as e where e.id = iol.id)";
  private static final String rtv_tax = "(select e.salesOrderLine.tax.id from ProcurementPOInvoiceMatch as e where e.goodsShipmentLine.id = iol.id)";
  private static final String rfc_tax = "(select e.salesOrderLine.tax.id from MaterialMgmtShipmentInOutLine as e where e.id = iol.id)";
  private static final String rtv_priceIncludeTax = "(select e.salesOrderLine.salesOrder.priceList.priceIncludesTax from ProcurementPOInvoiceMatch as e where e.goodsShipmentLine.id = iol.id)";
  private static final String rfc_priceIncludeTax = "(select e.priceList.priceIncludesTax from Order as e where e.id = :salesOrderId)";

  private final static String rm_aum = "" + "(select u.name from UOM as u where u.id in "
      + "(case when ((select ('Y') from OrderLine as ol where ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine = iol) is null) "
      + "then " // obSelected = false
      + "(case when (coalesce (iol.operativeUOM.id, '0') <> '0') then iol.operativeUOM.id else M_GET_DEFAULT_AUM_FOR_DOCUMENT(iol.product.id, dt.id) end) "
      + "else " // obSelected = true
      + "(coalesce((select ol.operativeUOM.id from OrderLine as ol where ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine = iol), 'Y')) "
      + "end)) ";

  private final static String rm_aumqty = ""
      + "(case when iol.operativeQuantity is not null then iol.operativeQuantity else m_get_converted_aumqty(iol.product.id, iol.movementQuantity, (M_GET_DEFAULT_AUM_FOR_DOCUMENT(iol.product.id, dt.id))) end) ";

  private final static String rm_aum_id = ""
      + "(case when (select ('Y') from OrderLine as ol where ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine = iol) is null "
      + "then " // obSelected = false
      + "case when iol.operativeUOM.id is not null then iol.operativeUOM.id else M_GET_DEFAULT_AUM_FOR_DOCUMENT(iol.product.id, dt.id) end "
      + "else " // obSelected = true
      + "coalesce ((select ol.operativeUOM.id from OrderLine as ol where ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine = iol),'') "
      + "end)";

  private final static String rm_aum_conversion_rate = ""
      + "(case when (select ('Y') from OrderLine as ol where ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine = iol) is null "
      + "then " // obSelected = false
      + "(case when coalesce (iol.operativeUOM.id, '0') = '0' " + "then "
      + "TO_NUMBER(M_GET_CONVERTED_QTY(iol.product.id, 1.0, M_GET_DEFAULT_AUM_FOR_DOCUMENT(iol.product.id, dt.id))) "
      + "else " + "TO_NUMBER(M_GET_CONVERTED_QTY(iol.product.id, 1.0, iol.operativeUOM.id)) "
      + "end) " + "else " // obSelected = true
      + "TO_NUMBER(M_GET_CONVERTED_QTY(iol.product.id, 1.0, coalesce ((select ol.operativeUOM.id from OrderLine as ol where ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine = iol),''))) "
      + "end)";

  private static final String returnedLeftClauseAUM = " coalesce((select ol.operativeQuantity from OrderLine as ol where ol.salesOrder.id = :salesOrderId and ol.goodsShipmentLine = iol),0)";

  private static final String rm_returnedUOM = "" + "(select u from UOM as u where u.id in "
      + "(case when (coalesce (iol.operativeUOM.id, '0') <> '0') then iol.operativeUOM.id else M_GET_DEFAULT_AUM_FOR_DOCUMENT(iol.product.id, dt.id) end)) ";

  @Override
  public String transformHqlQuery(String hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    // Sets the named parameters
    String salesOrderId = requestParameters.get("@Order.id@");
    String businessPartnerId = requestParameters.get("@Order.businessPartner@");
    queryNamedParameters.put("salesOrderId", salesOrderId);
    queryNamedParameters.put("businessPartnerId", businessPartnerId);

    String transformedHqlQuery = null;
    if (UOMUtil.isUomManagementEnabled()) {
      transformedHqlQuery = hqlQuery.replace("@returnedLeftClause@", returnedLeftClauseAUM);
    } else {
      transformedHqlQuery = hqlQuery.replace("@returnedLeftClause@", returnedLeftClause);
    }

    transformedHqlQuery = transformedHqlQuery.replace("@returnedOthersLeftClause@",
        returnedOthersLeftClause);
    Order order = OBDal.getInstance().get(Order.class, salesOrderId);
    if (order.getPriceList().isPriceIncludesTax()) {
      transformedHqlQuery = transformedHqlQuery.replaceAll("@unitPriceProperty@",
          grossUnitPriceProperty);
    } else {
      transformedHqlQuery = transformedHqlQuery.replaceAll("@unitPriceProperty@",
          unitPriceProperty);
    }

    String buttonOwnerViewTabId = requestParameters.get("buttonOwnerViewTabId");
    if (returnToVendorTabId.equals(buttonOwnerViewTabId)) {
      transformedHqlQuery = transformHqlQueryReturnToVendor(transformedHqlQuery, requestParameters,
          salesOrderId);
    } else {
      transformedHqlQuery = transformHqlQueryReturnFromCustomer(transformedHqlQuery,
          requestParameters, salesOrderId);
    }
    String distinctProperty = requestParameters.get("_distinct");
    if (distinctProperty != null) {
      transformedHqlQuery = transformedHqlQuery.replace("ORDER BY obSelected des", "ORDER BY");
    } else {
      transformedHqlQuery = transformedHqlQuery.replace("ORDER BY", "ORDER BY obSelected desc,");
    }
    transformedHqlQuery = transformedHqlQuery.replace("@rmAUM@", rm_aum);
    transformedHqlQuery = transformedHqlQuery.replace("@rmAUMQty@", rm_aumqty);
    transformedHqlQuery = transformedHqlQuery.replace("@rmAUMId@", rm_aum_id);
    transformedHqlQuery = transformedHqlQuery.replace("@rmAUMConversionRate@",
        rm_aum_conversion_rate);
    transformedHqlQuery = transformedHqlQuery.replace("@rmReturnedUOM@", rm_returnedUOM);
    return transformedHqlQuery;
  }

  public String transformHqlQueryReturnToVendor(String hqlQuery,
      Map<String, String> requestParameters, String salesOrderId) {
    // uses the subqueries of the columns in the left clauses
    String transformedHqlQuery = hqlQuery.replace("@unitPriceLeftClause@", rtv_unitPriceLeftClause);
    transformedHqlQuery = transformedHqlQuery.replace("@orderNoLeftClause@", rtv_orderNoLeftClause);
    transformedHqlQuery = transformedHqlQuery.replace("@isSalesTransaction@", "false");
    transformedHqlQuery = transformedHqlQuery.replace("@orderNoSubQuery@", rtv_orderNo);
    transformedHqlQuery = transformedHqlQuery.replace("@grossUnitPriceSubQuery@",
        rtv_grossUnitPrice);
    transformedHqlQuery = transformedHqlQuery.replace("@unitPriceSubQuery@", rtv_unitPrice);
    transformedHqlQuery = transformedHqlQuery.replace("@taxSubQuery@", rtv_tax);
    transformedHqlQuery = transformedHqlQuery.replace("@priceIncludeTaxSubQuery@",
        rtv_priceIncludeTax);

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
      transformedHqlQuery = transformedHqlQuery.replace("@returnReasonLeftClause@.name",
          returnReasonLeftClause2);
    }
    return transformedHqlQuery;
  }

  public String transformHqlQueryReturnFromCustomer(String hqlQuery,
      Map<String, String> requestParameters, String salesOrderId) {

    String transformedHqlQuery = hqlQuery.replace("@unitPriceLeftClause@", rfc_unitPriceLeftClause);
    transformedHqlQuery = transformedHqlQuery.replace("@orderNoLeftClause@", rfc_orderNoLeftClause);
    transformedHqlQuery = transformedHqlQuery.replace("@isSalesTransaction@", "true");
    transformedHqlQuery = transformedHqlQuery.replace("@orderNoSubQuery@", rfc_orderNo);
    transformedHqlQuery = transformedHqlQuery.replace("@grossUnitPriceSubQuery@",
        rfc_grossUnitPrice);
    transformedHqlQuery = transformedHqlQuery.replace("@unitPriceSubQuery@", rfc_unitPrice);
    transformedHqlQuery = transformedHqlQuery.replace("@taxSubQuery@", rfc_tax);
    transformedHqlQuery = transformedHqlQuery.replace("@priceIncludeTaxSubQuery@",
        rfc_priceIncludeTax);

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
      transformedHqlQuery = transformedHqlQuery.replace("@returnReasonLeftClause@.name",
          returnReasonLeftClause2);
    }
    return transformedHqlQuery;
  }

}
