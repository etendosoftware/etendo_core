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
 * All portions are Copyright (C) 2013-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.common.actionhandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DbUtility;

public class OrderCreatePOLines extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode(true);
    try {
      jsonRequest = new JSONObject(content);
      log.debug("{}", jsonRequest);
      final String strOrderId = jsonRequest.getString("C_Order_ID");
      Order order = OBDal.getInstance().get(Order.class, strOrderId);

      if (order != null) {
        createOrderLines(jsonRequest);
      }
      JSONObject errorMessage = new JSONObject();
      errorMessage.put("severity", "success");
      errorMessage.put("text", OBMessageUtils.messageBD("success"));
      jsonRequest.put("message", errorMessage);

    } catch (Exception e) {
      log.error("Error in ManagePrereservation Action Handler", e);

      try {
        jsonRequest = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonRequest.put("message", errorMessage);

      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonRequest;
  }

  private void createOrderLines(JSONObject jsonRequest) throws JSONException, OBException {
    JSONArray selectedLines = jsonRequest.getJSONObject("_params")
        .getJSONObject("grid")
        .getJSONArray("_selection");
    final String strOrderId = jsonRequest.getString("C_Order_ID");
    Order order = OBDal.getInstance().get(Order.class, strOrderId);
    // if no lines selected don't do anything.
    OBCriteria<OrderLine> obc = OBDal.getInstance().createCriteria(OrderLine.class);
    obc.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, order));
    obc.setProjection(Projections.max(OrderLine.PROPERTY_LINENO));
    Long lineNo = 0L;
    Object o = obc.uniqueResult();
    if (o != null) {
      lineNo = (Long) o;
    }

    for (int i = 0; i < selectedLines.length(); i++) {
      JSONObject selectedLine = selectedLines.getJSONObject(i);
      log.debug("{}", selectedLine);

      OrderLine newOrderLine = OBProvider.getInstance().get(OrderLine.class);
      newOrderLine.setSalesOrder(order);
      newOrderLine.setOrganization(order.getOrganization());
      lineNo = lineNo + 10L;
      newOrderLine.setLineNo(lineNo);
      newOrderLine.setOrderDate(order.getOrderDate());
      newOrderLine.setWarehouse(order.getWarehouse());
      newOrderLine.setCurrency(order.getCurrency());

      Product product = OBDal.getInstance().get(Product.class, selectedLine.getString("product"));
      if (product.getAttributeSetValue() != null
          && "D".equals(product.getUseAttributeSetValueAs())) {
        newOrderLine.setAttributeSetValue(product.getAttributeSetValue());
      }
      UOM uom = OBDal.getInstance().get(UOM.class, selectedLine.get("product$uOM"));
      UOM aum = OBDal.getInstance().get(UOM.class, selectedLine.get("aum"));

      newOrderLine.setProduct(product);

      newOrderLine.setUOM(uom);
      newOrderLine.setOperativeUOM(aum);
      // Ordered Quantity = returned quantity.
      BigDecimal qtyOrdered = new BigDecimal(selectedLine.getString("orderedQuantity"));
      BigDecimal aumQty = null;
      String strAumQty = selectedLine.getString("aumQuantity");
      if (strAumQty != null && !"".equals(strAumQty) && !"null".equals(strAumQty)) {
        aumQty = new BigDecimal(selectedLine.getString("aumQuantity"));
      }
      newOrderLine.setOrderedQuantity(qtyOrdered);
      newOrderLine.setOperativeQuantity(aumQty);

      List<Object> parameters = new ArrayList<Object>();
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
      parameters.add("N");

      String taxId = (String) CallStoredProcedure.getInstance().call("C_Gettax", parameters, null);
      if (taxId == null || "".equals(taxId)) {
        Map<String, String> errorParameters = new HashMap<String, String>();
        errorParameters.put("product", product.getName());
        String message = OBMessageUtils.messageBD("NoTaxFoundForProduct");
        throw new OBException(OBMessageUtils.parseTranslation(message, errorParameters));
      }
      TaxRate tax = OBDal.getInstance().get(TaxRate.class, taxId);
      newOrderLine.setTax(tax);

      // Price
      BigDecimal unitPrice, netPrice, grossPrice, stdPrice, limitPrice, grossAmt, netListPrice,
          grossListPrice, grossStdPrice;
      stdPrice = BigDecimal.ZERO;
      final int stdPrecision = order.getCurrency().getStandardPrecision().intValue();

      unitPrice = new BigDecimal(selectedLine.getString("standardPrice"));
      limitPrice = netListPrice = grossListPrice = stdPrice = unitPrice;

      if (order.getPriceList().isPriceIncludesTax()) {
        grossPrice = unitPrice;
        grossAmt = grossPrice.multiply(qtyOrdered).setScale(stdPrecision, RoundingMode.HALF_UP);
        netPrice = limitPrice = netListPrice = BigDecimal.ZERO;
        // selected line standard price is Gross Std Price in this case
        grossStdPrice = unitPrice;
      } else {
        netPrice = unitPrice;
        grossListPrice = grossAmt = grossPrice = grossStdPrice = BigDecimal.ZERO;
      }

      newOrderLine.setUnitPrice(netPrice);
      newOrderLine.setGrossUnitPrice(grossPrice);
      newOrderLine.setListPrice(netListPrice);
      newOrderLine.setGrossListPrice(grossListPrice);
      newOrderLine.setPriceLimit(limitPrice);
      newOrderLine.setStandardPrice(stdPrice);
      // Set Base Gross Unit Price thats is Gross Standard Price
      newOrderLine.setBaseGrossUnitPrice(grossStdPrice);
      newOrderLine.setLineNetAmount(
          netPrice.multiply(qtyOrdered).setScale(stdPrecision, RoundingMode.HALF_UP));
      newOrderLine.setLineGrossAmount(grossAmt);

      List<OrderLine> orderLines = order.getOrderLineList();
      orderLines.add(newOrderLine);
      order.setOrderLineList(orderLines);

      OBDal.getInstance().save(newOrderLine);
      OBDal.getInstance().save(order);
      OBDal.getInstance().flush();
    }

  }
}
