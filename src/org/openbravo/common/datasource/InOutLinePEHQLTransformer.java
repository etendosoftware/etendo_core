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

@ComponentProvider.Qualifier("631D227DC83A4898BBD041D46D829D27")
public class InOutLinePEHQLTransformer extends HqlQueryTransformer {
  protected static final String EMPTY_STRING = "";
  protected static final String CREATE_INVOICE_LINES_FORM_INOUT_WINDOW = "E4524BA1D1354AAD8B31C290672D8417";
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

    queryNamedParameters.put("issotrx", isSalesTransaction);
    queryNamedParameters.put("bp", strBusinessPartnerId);
    queryNamedParameters.put("plIncTax", priceList.isPriceIncludesTax());
    queryNamedParameters.put("cur", strCurrencyId);

    String transformedHql = _hqlQuery.replace("@selectClause@", getSelectClauseHQL());
    transformedHql = transformedHql.replace("@fromClause@", getFromClauseHQL());
    transformedHql = transformedHql.replace("@whereClause@", getWhereClauseHQL());
    transformedHql = transformedHql.replace("@groupByClause@", getGroupByHQL());
    transformedHql = transformedHql.replace("@movementQuantity@", getMovementQuantityHQL());
    transformedHql = transformedHql.replace("@operativeQuantity@", getOperativeQuantityHQL());
    transformedHql = transformedHql.replace("@orderQuantity@", getOrderQuantityHQL());
    transformedHql = transformedHql.replace("@operativeUOM@", getOperativeUOM());
    transformedHql = transformedHql.replace("@filterByDocumentsProcessedSinceNDaysAgo@",
        getSinceHowManyDaysAgoInOutsShouldBeFiltered());
    transformedHql = changeAdditionalFilters(transformedHql);
    return transformedHql;
  }

  protected String getSelectClauseHQL() {
    return EMPTY_STRING;
  }

  protected String getFromClauseHQL() {
    StringBuilder fromClause = new StringBuilder();
    fromClause.append(" MaterialMgmtShipmentInOutLine e");
    fromClause.append(" join e.shipmentReceipt sh");
    fromClause.append(" left join e.salesOrderLine ol");
    fromClause.append(" left join ol.salesOrder o");
    if (isSalesTransaction) {
      fromClause.append(" left join e.invoiceLineList il");
      fromClause.append(" left join il.invoice i");
    } else {
      fromClause.append(" left join e.procurementReceiptInvoiceMatchList mi");
      fromClause.append(" left join mi.invoiceLine mil");
    }

    return fromClause.toString();
  }

  protected String getWhereClauseHQL() {
    StringBuilder whereClause = new StringBuilder();
    whereClause.append(" and sh.salesTransaction = :issotrx");
    whereClause.append(" and sh.documentStatus in ('CO', 'CL')");
    whereClause.append(" and sh.processed = 'Y'");
    whereClause.append(" and sh.logistic <> 'Y'");
    whereClause.append(" and sh.businessPartner.id = :bp");
    whereClause.append(" and (ol.id is null or o.priceIncludesTax = :plIncTax)");
    whereClause.append(" and (o.id is null or o.currency.id = :cur)");
    if (isSalesTransaction) {
      whereClause.append(" and sh.completelyInvoiced = 'N'");
      whereClause.append(" and NOT EXISTS");
      whereClause.append(" (SELECT 1");
      whereClause.append(" FROM Order o2");
      whereClause.append(" WHERE o2.id = o.id");
      whereClause.append(
          " AND ((o2.invoiceTerms = 'O' and o2.delivered = 'N') or o2.invoiceTerms = 'N'))");
    } else {
    }
    return whereClause.toString();
  }

  protected String getGroupByHQL() {
    StringBuilder groupByClause = new StringBuilder();
    groupByClause.append("  sh.id,");
    groupByClause.append("  sh.documentNo,");
    groupByClause.append("  sh.movementDate,");
    groupByClause.append("  e.movementQuantity,");
    groupByClause.append("  e.uOM.id,");
    groupByClause.append("  e.product.id,");
    groupByClause.append("  e.id,");
    groupByClause.append("  e.lineNo,");
    groupByClause.append("  ol.id,");
    groupByClause.append("  COALESCE(e.asset.id, sh.asset.id),");
    groupByClause.append("  COALESCE(e.project.id, sh.project.id),");
    groupByClause.append("  COALESCE(e.costcenter.id, sh.costcenter.id),");
    groupByClause.append("  COALESCE(e.stDimension.id, sh.stDimension.id),");
    groupByClause.append("  COALESCE(e.ndDimension.id, sh.ndDimension.id),");
    groupByClause.append("  e.explode,");
    groupByClause.append("  e.bOMParent.id,");
    groupByClause.append("  e.creationDate,");
    groupByClause.append("  e.createdBy.name,");
    groupByClause.append("  e.updated,");
    groupByClause.append("  e.updatedBy.name,");
    groupByClause.append("  e.operativeUOM.id,");
    groupByClause.append("  e.operativeQuantity,");
    groupByClause.append("  sh.documentType.id,");
    groupByClause.append("  ol.id,");
    groupByClause.append("  o.id,");
    groupByClause.append("  o.priceList.id,");
    groupByClause.append("  sh.warehouse.id,");
    groupByClause.append("  e.storageBin.id,");
    groupByClause.append("  @orderQuantity@");
    if (isSalesTransaction) {
      groupByClause.append(
          " HAVING (e.movementQuantity >= 0 AND e.movementQuantity > SUM(COALESCE(CASE WHEN i.documentStatus = 'CO' THEN il.invoicedQuantity ELSE 0 END, 0)))");
      groupByClause.append(
          "  OR (e.movementQuantity < 0 AND e.movementQuantity < SUM(COALESCE(CASE WHEN i.documentStatus = 'CO' THEN il.invoicedQuantity ELSE 0 END, 0)))");
      groupByClause.append("  OR (e.explode='Y')");
    } else {
      groupByClause.append(
          " HAVING ((e.movementQuantity-SUM(COALESCE(mil.invoicedQuantity,0))) <> 0 OR (e.explode='Y'))");
    }
    return groupByClause.toString();
  }

  protected String getMovementQuantityHQL() {
    StringBuilder movementQuantityHql = new StringBuilder();
    if (isSalesTransaction) {
      movementQuantityHql.append(
          " (e.movementQuantity - sum(COALESCE(CASE WHEN i.documentStatus = 'CO' THEN il.invoicedQuantity ELSE 0 END, 0)))");
    } else {
      movementQuantityHql.append(" (e.movementQuantity - COALESCE(SUM(mil.invoicedQuantity), 0))");
    }
    return movementQuantityHql.toString();
  }

  protected String getOperativeQuantityHQL() {
    if (!UOMUtil.isUomManagementEnabled()) {
      return " '' ";
    }
    StringBuilder operativeQuantityHql = new StringBuilder();
    operativeQuantityHql.append(" to_number(M_GET_CONVERTED_AUMQTY(e.product.id, ");
    operativeQuantityHql.append(getMovementQuantityHQL());
    operativeQuantityHql.append(
        " , coalesce(e.operativeUOM.id, TO_CHAR(M_GET_DEFAULT_AUM_FOR_DOCUMENT(e.product.id, sh.documentType.id)))))");

    return operativeQuantityHql.toString();
  }

  protected String getOrderQuantityHQL() {
    StringBuilder orderQuantityHql = new StringBuilder();
    if (isSalesTransaction) {
      orderQuantityHql.append(
          " e.orderQuantity - coalesce((case when i.documentStatus = 'CO' then il.orderQuantity else 0 end),0)");

    } else {
      orderQuantityHql.append(
          " e.orderQuantity * ((e.movementQuantity - coalesce(mil.invoicedQuantity,0)) / (case when e.movementQuantity <> 0 then e.movementQuantity else null end))");
    }
    return orderQuantityHql.toString();
  }

  protected String getOperativeUOM() {
    StringBuilder operativeUOMHql = new StringBuilder();
    if (UOMUtil.isUomManagementEnabled()) {
      operativeUOMHql.append(" (select aum2.name from UOM aum2 where aum2.id = ");
      operativeUOMHql.append(
          " (coalesce(e.operativeUOM.id, TO_CHAR(M_GET_DEFAULT_AUM_FOR_DOCUMENT(e.product.id, sh.documentType.id))))) ");
    } else {
      operativeUOMHql.append("'' ");
    }
    return operativeUOMHql.toString();
  }

  /**
   * Return the value of FilterByDocumentsProcessedSinceNDaysAgo preference to be used to define a
   * starting range date filter to limit the shipment/receipts records to be returned by the query
   * 
   * @return The value of the preference if exists for the Create Invoice Lines From
   *         Shipment/Receipt window, or since one year (365 days) if not or exists any conflict in
   *         the preference definition
   */
  protected String getSinceHowManyDaysAgoInOutsShouldBeFiltered() {
    int daysCount = 365;
    try {
      Window window = OBDal.getInstance().get(Window.class, CREATE_INVOICE_LINES_FORM_INOUT_WINDOW);
      String value = Preferences.getPreferenceValue("FilterByDocumentsProcessedSinceNDaysAgo", true,
          OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(), window);
      daysCount = Integer.valueOf(value);
    } catch (Exception ignore) {
    }
    return String.valueOf(daysCount);
  }

  private String changeAdditionalFilters(String transformedHql) {
    // Change the CLIENT and ORG filters to use the org and client from the InOut header instead of
    // the lines.
    String additionalFilters = transformedHql;
    additionalFilters = additionalFilters.replace("e.client.id in (", "sh.client.id in (");
    additionalFilters = additionalFilters.replace("e.organization in (", "sh.organization.id in (");
    return additionalFilters;
  }
}
