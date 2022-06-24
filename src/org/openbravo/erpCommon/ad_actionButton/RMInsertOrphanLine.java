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
 * All portions are Copyright (C) 2013-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_actionButton;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.ReturnReason;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;

public class RMInsertOrphanLine implements org.openbravo.scheduling.Process {

  static final String ITEM = "I";
  static final String SERVICE = "S";

  @Override
  public void execute(final ProcessBundle bundle) throws Exception {
    final OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(OBMessageUtils.messageBD("Success"));

    final String strOrderId = (String) bundle.getParams().get("C_Order_ID");
    final String strProductId = (String) bundle.getParams().get("mProductId");
    final String strAttributeSetInstanceId = (String) bundle.getParams()
        .get("mAttributesetinstanceId");
    final String strReturnedQty = (String) bundle.getParams().get("returned");
    final BigDecimal returnedQty = new BigDecimal(strReturnedQty);
    final String strUnitPrice = (String) bundle.getParams().get("pricestd");
    final String strTaxId = (String) bundle.getParams().get("cTaxId");
    final String strReturnReason = (String) bundle.getParams().get("cReturnReasonId");

    OBContext.setAdminMode(true);
    try {
      final Order order = OBDal.getInstance().get(Order.class, strOrderId);
      final Product product = OBDal.getInstance().get(Product.class, strProductId);

      if (!product.isReturnable()) {
        throw new OBException(
            "@Product@ '" + product.getIdentifier() + "' @ServiceIsNotReturnable@");
      }

      AttributeSetInstance attrSetInstance = null;
      if (strAttributeSetInstanceId != null) {
        attrSetInstance = OBDal.getInstance()
            .get(AttributeSetInstance.class, strAttributeSetInstanceId);
      }

      if (product.getAttributeSet() != null
          && (strAttributeSetInstanceId == null || strAttributeSetInstanceId.equals(""))
          && (product.getUseAttributeSetValueAs() == null
              || !product.getUseAttributeSetValueAs().equals("F"))) {
        throw new OBException("@productWithoutAttributeSet@");
      }

      final OrderLine newOrderLine = createNewOrderLine(returnedQty, strUnitPrice, order, product,
          attrSetInstance, strReturnReason);
      // tax
      final TaxRate tax = getTaxForNewOrderLine(strTaxId, order, product);

      if (tax != null) {
        newOrderLine.setTax(tax);
      } else {
        OBDal.getInstance().rollbackAndClose();
        final Map<String, String> errorParameters = new HashMap<>();
        errorParameters.put("product", product.getName());
        final String message = OBMessageUtils.messageBD("InsertOrphanNoTaxFoundForProduct");
        msg.setMessage(OBMessageUtils.parseTranslation(message, errorParameters));
        msg.setTitle(OBMessageUtils.messageBD("Error"));
        msg.setType("Error");
        bundle.setResult(msg);
        return;
      }

      final List<OrderLine> orderLines = order.getOrderLineList();
      orderLines.add(newOrderLine);
      order.setOrderLineList(orderLines);

      OBDal.getInstance().save(newOrderLine);
      OBDal.getInstance().save(order);
      OBDal.getInstance().flush();

    } finally {
      OBContext.restorePreviousMode();
    }

    bundle.setResult(msg);
  }

  private OrderLine createNewOrderLine(final BigDecimal returnedQty, final String strUnitPrice,
      final Order order, final Product product, final AttributeSetInstance attrSetInstance,
      final String strReturnReason) {
    final OrderLine newOrderLine = OBProvider.getInstance().get(OrderLine.class);
    newOrderLine.setSalesOrder(order);
    newOrderLine.setOrganization(order.getOrganization());
    newOrderLine.setLineNo(getNewLineNo(order));
    newOrderLine.setOrderDate(order.getOrderDate());
    newOrderLine.setWarehouse(order.getWarehouse());
    newOrderLine.setCurrency(order.getCurrency());
    newOrderLine.setProduct(product);
    newOrderLine.setAttributeSetValue(attrSetInstance);
    newOrderLine.setUOM(product.getUOM());
    newOrderLine.setOrderedQuantity(returnedQty.negate());

    if (UOMUtil.isUomManagementEnabled()) {
      newOrderLine.setOperativeQuantity(returnedQty.negate());
      newOrderLine.setOperativeUOM(product.getUOM());
    }

    if (strUnitPrice.isEmpty()) {
      final ProductPrice productPrice = getProductPrice(product, order.getOrderDate(),
          order.isSalesTransaction(), order.getPriceList());
      newOrderLine.setUnitPrice(productPrice.getStandardPrice());
      newOrderLine.setListPrice(productPrice.getListPrice());
      newOrderLine.setPriceLimit(productPrice.getPriceLimit());
      newOrderLine.setStandardPrice(productPrice.getStandardPrice());
      if (order.getPriceList().isPriceIncludesTax()) {
        newOrderLine.setGrossUnitPrice(productPrice.getStandardPrice());
        newOrderLine
            .setLineGrossAmount(productPrice.getStandardPrice().multiply(returnedQty).negate());
        newOrderLine.setUnitPrice(BigDecimal.ZERO);
      }
    } else {
      final BigDecimal unitPrice = new BigDecimal(strUnitPrice);
      newOrderLine.setUnitPrice(unitPrice);
      newOrderLine.setListPrice(unitPrice);
      newOrderLine.setPriceLimit(unitPrice);
      newOrderLine.setStandardPrice(unitPrice);
      if (order.getPriceList().isPriceIncludesTax()) {
        newOrderLine.setGrossUnitPrice(unitPrice);
        newOrderLine.setLineGrossAmount(unitPrice.multiply(returnedQty).negate());
        newOrderLine.setUnitPrice(BigDecimal.ZERO);
      }
    }

    if (strReturnReason.isEmpty()) {
      newOrderLine.setReturnReason(order.getReturnReason());
    } else {
      newOrderLine.setReturnReason(OBDal.getInstance().get(ReturnReason.class, strReturnReason));
    }

    return newOrderLine;
  }

  private Long getNewLineNo(final Order order) {
    //@formatter:off
    final String hqlWhere =
            "as ol" +
            " where ol.salesOrder.id = :orderId" +
            " order by ol.lineNo desc";
    //@formatter:on

    final OBQuery<OrderLine> olQry = OBDal.getInstance()
        .createQuery(OrderLine.class, hqlWhere)
        .setNamedParameter("orderId", order.getId());

    if (olQry.count() > 0) {
      final OrderLine ol = olQry.list().get(0);
      return ol.getLineNo() + 10L;
    }
    return 10L;
  }

  private ProductPrice getProductPrice(final Product product, final Date date,
      final boolean useSalesPriceList, final PriceList priceList) {
    //@formatter:off
    String hqlWhere =
            "as pp" +
            "  join pp.priceListVersion as plv" +
            "  join plv.priceList as pl" +
            " where pp.product.id = :productId" +
            "   and plv.validFromDate <= :date";
    //@formatter:on
    if (priceList != null) {
      //@formatter:off
      hqlWhere +=
            "   and pl.id = :pricelistId";
      //@formatter:on
    } else {
      //@formatter:off
      hqlWhere +=
            "   and pl.salesPriceList = :salespricelist";
      //@formatter:on
    }
    //@formatter:off
    hqlWhere +=
            " order by pl.default desc" +
            "   , plv.validFromDate desc";
    //@formatter:on

    final OBQuery<ProductPrice> ppQry = OBDal.getInstance()
        .createQuery(ProductPrice.class, hqlWhere)
        .setNamedParameter("productId", product.getId())
        .setNamedParameter("date", date);
    if (priceList != null) {
      ppQry.setNamedParameter("pricelistId", priceList.getId());
    } else {
      ppQry.setNamedParameter("salespricelist", useSalesPriceList);
    }

    final List<ProductPrice> ppList = ppQry.list();
    if (ppList.isEmpty()) {
      // No product price found.
      throw new OBException("@PriceListVersionNotFound@. @Product@: " + product.getIdentifier()
          + " @Date@: " + OBDateUtils.formatDate(date));
    }
    return ppList.get(0);
  }

  private TaxRate getTaxForNewOrderLine(final String strTaxId, final Order order,
      final Product product) {
    TaxRate tax;
    if (strTaxId.isEmpty()) {
      final List<Object> parameters = new ArrayList<>();
      parameters.add(product.getId());
      parameters.add(order.getOrderDate());
      parameters.add(order.getOrganization().getId());
      parameters.add(order.getWarehouse().getId());
      parameters.add(order.getInvoiceAddress().getId());
      parameters.add(order.getPartnerAddress().getId());

      if (order.getProject() != null) {
        parameters.add(order.getProject().getId());
      } else {
        parameters.add(null);
      }
      parameters.add("Y");

      final String strDefaultTaxId = (String) CallStoredProcedure.getInstance()
          .call("C_Gettax", parameters, null);
      if (strDefaultTaxId == null || strDefaultTaxId.equals("")) {
        return null;
      }
      tax = OBDal.getInstance().get(TaxRate.class, strDefaultTaxId);
    } else {
      tax = OBDal.getInstance().get(TaxRate.class, strTaxId);
    }
    return tax;
  }
}
