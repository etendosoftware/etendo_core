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
 * All portions are Copyright (C) 2018-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.common.datasource;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;

@ComponentProvider.Qualifier("7EB9FFD7BD4E4113A13A096EB879D358")
public class OrderLinePEHQLTransformer extends HqlQueryTransformer {
  protected static final String EMPTY_STRING = "";
  protected static final String CREATE_INVOICE_LINES_FORM_ORDER_WINDOW = "D0E067F649AC457D9EA2CDAC2E8571D7";
  protected boolean isSalesTransaction;

  @Override
  public String transformHqlQuery(String _hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    isSalesTransaction = StringUtils.equals(requestParameters.get("@Invoice.salesTransaction@"),
        "true");
    final String strInvoicePriceListId = requestParameters.get("@Invoice.priceList@");
    final PriceList priceList = OBDal.getInstance().get(PriceList.class, strInvoicePriceListId);
    final String strBusinessPartnerId = requestParameters.get("@Invoice.businessPartner@");
    final String strCurrencyId = requestParameters.get("@Invoice.currency@");
    final String strInvoiceId = requestParameters.get("@Invoice.id@");

    queryNamedParameters.put("issotrx", isSalesTransaction);
    queryNamedParameters.put("bp", strBusinessPartnerId);
    queryNamedParameters.put("plIncTax", priceList.isPriceIncludesTax());
    queryNamedParameters.put("cur", strCurrencyId);
    if (!isSalesTransaction) {
      queryNamedParameters.put("invId", strInvoiceId);
    }

    String transformedHql = _hqlQuery.replace("@selectClause@", getSelectClauseHQL());
    transformedHql = transformedHql.replace("@fromClause@", getFromClauseHQL());
    transformedHql = transformedHql.replace("@whereClause@", getWhereClauseHQL());
    transformedHql = transformedHql.replace("@groupByClause@", getGroupByHQL());
    transformedHql = transformedHql.replace("@orderByClause@", getOrderByHQL());
    transformedHql = transformedHql.replace("@orderedQuantity@", getOrderedQuantityHQL());
    transformedHql = transformedHql.replace("@operativeQuantity@", getOperativeQuantityHQL());
    transformedHql = transformedHql.replace("@orderQuantity@", getOrderQuantityHQL());
    transformedHql = transformedHql.replace("@operativeUOM@", getOperativeUOM());
    transformedHql = transformedHql.replace("@documentNo@", getDocumentNo());
    transformedHql = transformedHql.replace("@grandTotalAmount@", getGrandTotalAmount());
    transformedHql = transformedHql.replace("@scheduledDeliveryDate@", getScheduledDeliveryDate());
    transformedHql = transformedHql.replace("@warehouse@", getWarehouse());
    transformedHql = transformedHql.replace("@filterByDocumentsProcessedSinceNDaysAgo@",
        getSinceHowManyDaysAgoOrdersShouldBeFiltered());
    transformedHql = changeAdditionalFilters(transformedHql);
    return transformedHql;
  }

  protected String getSelectClauseHQL() {
    return EMPTY_STRING;
  }

  protected String getFromClauseHQL() {
    StringBuilder fromClause = new StringBuilder();
    if (isSalesTransaction) {
      fromClause.append(" InvoiceCandidateV ic, ");
      fromClause.append(" OrderLine e");
    } else {
      fromClause.append(" OrderLine e");
      fromClause.append(" join e.salesOrder o");
    }
    fromClause.append(" left join e.goodsShipmentLine il");

    if (!isSalesTransaction) {
      fromClause
          .append(" left join e.procurementPOInvoiceMatchList m with m.invoiceLine.id is not null");
      fromClause
          .append(" left join e.invoiceLineList ci with (ci.id is null or ci.invoice.id = :invId)");
    }
    return fromClause.toString();
  }

  protected String getWhereClauseHQL() {
    StringBuilder whereClause = new StringBuilder();
    if (isSalesTransaction) {
      whereClause.append(" and ic.salesTransaction = :issotrx");
      whereClause.append(" and ic.priceIncludesTax = :plIncTax");
      whereClause.append(" and ic.currency.id = :cur");
      whereClause.append(" and ic.businessPartner.id = :bp");
      whereClause.append(" and e.salesOrder.id = ic.salesOrder.id");
      whereClause.append(" and (");
      whereClause.append("     ic.term in ('D', 'S') and ic.deliveredQuantity <> 0");
      whereClause.append("  or (ic.term = 'I' AND EXISTS");
      whereClause.append("   (SELECT 1");
      whereClause.append("    FROM OrderLine ol2");
      whereClause.append("    WHERE ol2.salesOrder.id = ic.salesOrder.id");
      whereClause.append("    GROUP BY ol2.id ");
      whereClause.append("    HAVING SUM(ol2.orderedQuantity) - SUM(ol2.invoicedQuantity) <> 0))");
      whereClause.append("  or (ic.term = 'O' and ic.orderedQuantity = ic.deliveredQuantity)");
      whereClause.append(" )");
    } else {
      whereClause.append(" and o.salesTransaction = :issotrx");
      whereClause.append(" and o.priceIncludesTax = :plIncTax");
      whereClause.append(" and o.currency.id = :cur");

      whereClause.append(" and o.businessPartner.id = :bp");
      whereClause.append(" and o.documentStatus in ('CO', 'CL')");
      whereClause.append(" and o.invoiceTerms <> 'N'");
    }
    return whereClause.toString();
  }

  protected String getGroupByHQL() {
    StringBuilder groupByClause = new StringBuilder();
    groupByClause.append("  e.orderedQuantity,");
    groupByClause.append("  e.orderDate,");
    groupByClause.append("  e.lineNo,");
    groupByClause.append("  e.id,");
    groupByClause.append("  e.creationDate,");
    groupByClause.append("  e.createdBy.name,");
    groupByClause.append("  e.updated,");
    groupByClause.append("  e.updatedBy.name,");
    groupByClause.append("  e.explode,");
    groupByClause.append("  e.operativeQuantity,");
    groupByClause.append("  e.uOM.id, e.product.id, e.operativeUOM.id,");
    groupByClause.append("  il.id,");
    groupByClause.append(isSalesTransaction ? " ic.id," : " o.id,");
    groupByClause.append(isSalesTransaction ? " ic.documentNo," : " o.documentNo,");
    groupByClause.append(isSalesTransaction ? " ic.orderDate," : " o.orderDate,");
    groupByClause.append(isSalesTransaction ? " ic.grandTotalAmount," : " o.grandTotalAmount,");
    groupByClause
        .append(isSalesTransaction ? " ic.scheduledDeliveryDate," : " o.scheduledDeliveryDate,");
    groupByClause.append(isSalesTransaction ? " COALESCE(e.asset.id, ic.asset.id),"
        : " COALESCE(e.asset.id, o.asset.id),");
    groupByClause.append(isSalesTransaction ? " COALESCE(e.project.id, ic.project.id),"
        : " COALESCE(e.project.id, o.project.id),");
    groupByClause.append(isSalesTransaction ? " COALESCE(e.costcenter.id, ic.costcenter.id),"
        : " COALESCE(e.costcenter.id, o.costcenter.id),");
    groupByClause.append(isSalesTransaction ? " COALESCE(e.stDimension.id, ic.stDimension.id),"
        : " COALESCE(e.stDimension.id, o.stDimension.id),");
    groupByClause.append(isSalesTransaction ? " COALESCE(e.ndDimension.id, ic.ndDimension.id),"
        : " COALESCE(e.ndDimension.id, o.ndDimension.id),");
    groupByClause.append(isSalesTransaction ? " ic.documentType.id," : " o.documentType.id,");
    groupByClause.append(isSalesTransaction ? " ic.businessPartner.id," : " o.businessPartner.id,");
    groupByClause.append(isSalesTransaction ? " ic.warehouse.id," : " o.warehouse.id,");
    groupByClause.append("  @orderQuantity@,");
    if (isSalesTransaction) {
      groupByClause.append("  e.invoicedQuantity");
    } else {
      groupByClause.append("  ci.id");
      groupByClause.append(
          " HAVING ((e.explode='Y') OR ((e.orderedQuantity-SUM(COALESCE(m.quantity,0))) <> 0");
      groupByClause.append(
          " AND (ci.id is null OR SUM(COALESCE(ci.invoicedQuantity,0))-COALESCE(e.orderedQuantity,0)-SUM(COALESCE(m.quantity,0)) < 0)");
      groupByClause.append("))");
    }
    return groupByClause.toString();
  }

  protected String getOrderByHQL() {
    return isSalesTransaction ? " ic.documentNo desc, e.lineNo asc"
        : " o.documentNo desc, e.lineNo asc";
  }

  protected String getOrderedQuantityHQL() {
    StringBuilder orderedQuantityHql = new StringBuilder();
    if (isSalesTransaction) {
      orderedQuantityHql.append(" e.orderedQuantity-COALESCE(e.invoicedQuantity,0)");
    } else {
      orderedQuantityHql.append(
          " e.orderedQuantity-SUM(COALESCE(m.quantity, 0))-SUM(COALESCE(ci.invoicedQuantity, 0))");
    }
    return orderedQuantityHql.toString();
  }

  protected String getOperativeQuantityHQL() {
    if (!UOMUtil.isUomManagementEnabled()) {
      return " '' ";
    }
    StringBuilder operativeQuantityHql = new StringBuilder();
    operativeQuantityHql.append(" to_number(M_GET_CONVERTED_AUMQTY(e.product.id, ");
    operativeQuantityHql.append(getOrderedQuantityHQL());
    operativeQuantityHql.append(
        " , coalesce(e.operativeUOM.id, TO_CHAR(M_GET_DEFAULT_AUM_FOR_DOCUMENT(e.product.id, ");
    operativeQuantityHql.append(isSalesTransaction ? "ic.documentType.id" : "o.documentType.id");
    operativeQuantityHql.append(")))))");
    return operativeQuantityHql.toString();
  }

  protected String getOrderQuantityHQL() {
    StringBuilder orderQuantityHql = new StringBuilder();
    if (isSalesTransaction) {
      orderQuantityHql.append(" COALESCE(il.orderQuantity ");
      orderQuantityHql.append(
          " , e.orderQuantity * ((e.orderedQuantity - coalesce(e.invoicedQuantity,0)) / (case when e.orderedQuantity <> 0 then e.orderedQuantity else null end)))");
    } else {
      orderQuantityHql.append(" COALESCE(il.orderQuantity ");
      orderQuantityHql.append(
          " , e.orderQuantity * ((e.orderedQuantity - coalesce(e.invoicedQuantity,0)) / (case when e.orderedQuantity <> 0 then e.orderedQuantity else null end)))");
    }
    return orderQuantityHql.toString();
  }

  protected String getOperativeUOM() {
    StringBuilder operativeUOMHql = new StringBuilder();
    if (UOMUtil.isUomManagementEnabled()) {
      operativeUOMHql.append(" (select aum2.name from UOM aum2 where aum2.id = ");
      operativeUOMHql.append(
          " (coalesce(e.operativeUOM.id, TO_CHAR(M_GET_DEFAULT_AUM_FOR_DOCUMENT(e.product.id, ");
      operativeUOMHql.append(isSalesTransaction ? "ic.documentType.id" : "o.documentType.id");
      operativeUOMHql.append("))))) ");
    } else {
      operativeUOMHql.append("'' ");
    }
    return operativeUOMHql.toString();
  }

  protected String getDocumentNo() {
    return isSalesTransaction ? " ic.documentNo" : " o.documentNo";
  }

  protected String getGrandTotalAmount() {
    return isSalesTransaction ? " ic.grandTotalAmount" : " o.grandTotalAmount";
  }

  protected String getScheduledDeliveryDate() {
    return isSalesTransaction ? " ic.scheduledDeliveryDate" : " o.scheduledDeliveryDate";
  }

  protected String getWarehouse() {
    if (isSalesTransaction) {
      return " (select wh.name from Warehouse wh where wh.id = ic.warehouse.id)";
    } else {
      return " (select wh.name from Warehouse wh where wh.id = o.warehouse.id)";
    }
  }

  /**
   * Returns the value of FilterByDocumentsProcessedSinceNDaysAgo preference to be used to define a
   * starting range date filter to limit the order records to be returned by the query
   * 
   * @return The value of the preference if exists for the Create Invoice Lines From Order window,
   *         or since one year (365 days) if not or exists any conflict in the preference definition
   */
  protected String getSinceHowManyDaysAgoOrdersShouldBeFiltered() {
    int daysCount = 365;
    try {
      Window window = OBDal.getInstance().get(Window.class, CREATE_INVOICE_LINES_FORM_ORDER_WINDOW);
      String value = Preferences.getPreferenceValue("FilterByDocumentsProcessedSinceNDaysAgo", true,
          OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(), window);
      daysCount = Integer.valueOf(value);
    } catch (Exception ignore) {
    }
    return (isSalesTransaction ? "ic" : "o") + ".orderDate >= (now()-" + daysCount + ")";
  }

  private String changeAdditionalFilters(String transformedHql) {
    // If Create Lines From SO then change the CLIENT and ORG filters to use InvoiceCandidateV
    // instead of the order line. If it is executed from PO then takes the org and lient from the
    // Order header.
    String additionalFilters = transformedHql;
    additionalFilters = additionalFilters.replace("e.client.id in (",
        isSalesTransaction ? "ic.client.id in (" : "o.client.id in (");
    additionalFilters = additionalFilters.replace("e.organization in (",
        isSalesTransaction ? "ic.organization.id in (" : "o.organization.id in (");
    return additionalFilters;
  }
}
