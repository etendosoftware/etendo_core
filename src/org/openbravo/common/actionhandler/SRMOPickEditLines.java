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
 * All portions are Copyright (C) 2011-2018 Openbravo SLU 
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

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.ReturnReason;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonUtils;

/**
 * 
 * @author gorkaion
 * 
 */
public class SRMOPickEditLines extends BaseProcessActionHandler {
  private static Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode();
    try {
      jsonRequest = new JSONObject(content);
      log.debug(jsonRequest);
      // When the focus is NOT in the tab of the button (i.e. any child tab) and the tab does not
      // contain any record, the inpcOrderId parameter contains "null" string. Use C_Order_ID
      // instead because it always contains the id of the selected order.
      // Issue 20585: https://issues.openbravo.com/view.php?id=20585
      final String strOrderId = jsonRequest.getString("C_Order_ID");
      Order order = OBDal.getInstance().get(Order.class, strOrderId);

      if (order != null) {
        List<String> idList = OBDao.getIDListFromOBObject(order.getOrderLineList());
        createOrderLines(jsonRequest, idList);
      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      log.error(e.getMessage(), e);

      try {
        jsonRequest = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils
            .translateError(new DalConnectionProvider(), vars, vars.getLanguage(), ex.getMessage())
            .getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonRequest.put("message", errorMessage);

      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
        // do nothing, give up
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonRequest;
  }

  private void createOrderLines(JSONObject jsonRequest, List<String> idList)
      throws JSONException, OBException {
    JSONObject grid = jsonRequest.getJSONObject("_params").getJSONObject("grid");
    JSONArray selectedLines = grid.getJSONArray("_selection");
    JSONObject orphanlinesgrid = jsonRequest.getJSONObject("_params")
        .getJSONObject("orphanlinesgrid");
    JSONArray selectedLinesOrphan = orphanlinesgrid.getJSONArray("_selection");

    final String strOrderId = jsonRequest.getString("C_Order_ID");
    Order order = OBDal.getInstance().get(Order.class, strOrderId);
    boolean isSOTrx = order.isSalesTransaction();

    for (long i = 0; i < selectedLinesOrphan.length(); i++) {
      JSONObject selectedLineOrphan = selectedLinesOrphan.getJSONObject((int) i);
      selectedLines.put(selectedLineOrphan);
    }

    // if no lines selected don't do anything.
    if (selectedLines.length() == 0) {
      removeNonSelectedLines(idList, order);
      return;
    }

    Long lineNo = 0L;
    boolean isUomManagementEnabled = UOMUtil.isUomManagementEnabled();
    for (long i = 0; i < selectedLines.length(); i++) {
      JSONObject selectedLine = selectedLines.getJSONObject((int) i);
      log.debug(selectedLine);

      Product product = OBDal.getInstance().get(Product.class, selectedLine.getString("product"));
      if (!product.isReturnable()) {
        throw new OBException(
            "@Product@ '" + product.getIdentifier() + "' @ServiceIsNotReturnable@");
      }

      if (selectedLine.get("returned").equals(null)) {
        continue;
      }

      OrderLine newOrderLine = null;
      boolean notExistsOrderLine = selectedLine.get("salesOrderLine").equals(null)
          || "".equals(selectedLine.get("salesOrderLine"));
      if (notExistsOrderLine) {
        newOrderLine = OBProvider.getInstance().get(OrderLine.class);
        newOrderLine.setSalesOrder(order);
        newOrderLine.setOrganization(order.getOrganization());
        lineNo = lineNo + 10L;
        newOrderLine.setLineNo(lineNo);
        newOrderLine.setOrderDate(order.getOrderDate());
        newOrderLine.setWarehouse(order.getWarehouse());
        newOrderLine.setCurrency(order.getCurrency());
      } else {
        newOrderLine = OBDal.getInstance().get(OrderLine.class, selectedLine.get("salesOrderLine"));
        idList.remove(selectedLine.get("salesOrderLine"));
      }
      ShipmentInOutLine shipmentLine = null;
      if (StringUtils.isNotEmpty(selectedLine.getString("goodsShipmentLine"))) {
        shipmentLine = OBDal.getInstance()
            .get(ShipmentInOutLine.class, selectedLine.getString("goodsShipmentLine"));
      }
      AttributeSetInstance asi = null;
      if (!selectedLine.get("attributeSetValue").equals(null)) {
        asi = OBDal.getInstance()
            .get(AttributeSetInstance.class, selectedLine.getString("attributeSetValue"));
      }
      UOM uom = OBDal.getInstance().get(UOM.class, selectedLine.get("uOM"));

      newOrderLine.setGoodsShipmentLine(shipmentLine);
      newOrderLine.setProduct(product);
      newOrderLine.setAttributeSetValue(asi);
      newOrderLine.setUOM(uom);
      newOrderLine.setOperativeUOM(shipmentLine != null ? shipmentLine.getOperativeUOM() : null);
      newOrderLine
          .setOperativeQuantity(shipmentLine != null && shipmentLine.getOperativeQuantity() != null
              ? shipmentLine.getOperativeQuantity().negate()
              : BigDecimal.ZERO);

      BigDecimal qtyReturned = new BigDecimal(selectedLine.getString("returned")).negate();

      boolean applyAUM = isUomManagementEnabled && shipmentLine != null
          && shipmentLine.getOrderUOM() == null;
      try {
        selectedLine.getString("aum");
      } catch (JSONException jse) {
        /**
         * The line is an orphan line, no AUM logic is applied
         */
        applyAUM = false;
      }
      if (applyAUM) {
        String aumId = selectedLine.getString("returnedUOM");
        UOM aum = OBDal.getInstance().get(UOM.class, aumId);
        newOrderLine.setOperativeUOM(aum);
        if (!aum.getId().equals(shipmentLine.getUOM().getId())) {
          qtyReturned = UOMUtil
              .getConvertedQty(shipmentLine.getProduct().getId(),
                  new BigDecimal(selectedLine.getString("returned")), aum.getId())
              .negate();
        }
        newOrderLine
            .setOperativeQuantity(new BigDecimal(selectedLine.getString("returned")).negate());
      }

      // Ordered Quantity = returned quantity.
      newOrderLine.setOrderedQuantity(qtyReturned);

      TaxRate tax = null;
      if (shipmentLine != null && shipmentLine.getSalesOrderLine() != null) {
        tax = shipmentLine.getSalesOrderLine().getTax();
      } else {
        String taxId = "";
        if (JsonUtils.isValueEmpty(selectedLine.get("tax").toString())) {
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
          parameters.add(order.isSalesTransaction() ? "Y" : "N");

          taxId = (String) CallStoredProcedure.getInstance().call("C_Gettax", parameters, null);
          if (taxId == null || "".equals(taxId)) {
            Map<String, String> errorParameters = new HashMap<String, String>();
            errorParameters.put("product", product.getName());
            String message = OBMessageUtils.messageBD("NoTaxFoundForProduct");
            throw new OBException(OBMessageUtils.parseTranslation(message, errorParameters));
          }
        } else {
          taxId = selectedLine.getString("tax");
        }
        tax = OBDal.getInstance().get(TaxRate.class, taxId);
      }
      newOrderLine.setTax(tax);

      // Price
      BigDecimal unitPrice, netPrice, grossPrice, stdPrice, limitPrice, grossAmt, netListPrice,
          grossListPrice, baseGrossUnitPrice;
      stdPrice = baseGrossUnitPrice = BigDecimal.ZERO;
      final int stdPrecision = order.getCurrency().getStandardPrecision().intValue();

      if (selectedLine.get("unitPrice").equals(null) || "".equals(selectedLine.get("unitPrice"))) {
        try {
          final ProductPrice pp = FinancialUtils.getProductPrice(product, order.getOrderDate(),
              isSOTrx, order.getPriceList());
          unitPrice = pp.getStandardPrice();
          limitPrice = pp.getPriceLimit();
          netListPrice = pp.getListPrice();
          grossListPrice = pp.getListPrice();
          stdPrice = pp.getStandardPrice();
          baseGrossUnitPrice = pp.getStandardPrice();
        } catch (OBException e) {
          // Product not found in price list. Prices default to ZERO
          unitPrice = limitPrice = netListPrice = grossListPrice = stdPrice = BigDecimal.ZERO;
        }
      } else {
        unitPrice = new BigDecimal(selectedLine.getString("unitPrice"));
        if (shipmentLine != null && shipmentLine.getSalesOrderLine() != null) {
          limitPrice = shipmentLine.getSalesOrderLine().getPriceLimit();
          netListPrice = shipmentLine.getSalesOrderLine().getListPrice();
          grossListPrice = shipmentLine.getSalesOrderLine().getGrossListPrice();
          stdPrice = shipmentLine.getSalesOrderLine().getStandardPrice();
          baseGrossUnitPrice = shipmentLine.getSalesOrderLine().getBaseGrossUnitPrice();
        } else {
          limitPrice = netListPrice = grossListPrice = stdPrice = unitPrice;
        }
      }

      if (order.getPriceList().isPriceIncludesTax()) {
        grossPrice = unitPrice;
        grossAmt = grossPrice.multiply(qtyReturned).setScale(stdPrecision, RoundingMode.HALF_UP);
        netPrice = limitPrice = stdPrice = netListPrice = BigDecimal.ZERO;
      } else {
        netPrice = unitPrice;
        grossListPrice = grossAmt = grossPrice = BigDecimal.ZERO;
      }

      newOrderLine.setUnitPrice(netPrice);
      newOrderLine.setGrossUnitPrice(grossPrice);
      newOrderLine.setListPrice(netListPrice);
      newOrderLine.setGrossListPrice(grossListPrice);
      newOrderLine.setPriceLimit(limitPrice);
      newOrderLine.setStandardPrice(stdPrice);
      newOrderLine.setLineNetAmount(
          netPrice.multiply(qtyReturned).setScale(stdPrecision, RoundingMode.HALF_UP));
      newOrderLine.setLineGrossAmount(grossAmt);
      newOrderLine.setBaseGrossUnitPrice(baseGrossUnitPrice);

      if (!selectedLine.get("returnReason").equals(null)) {
        newOrderLine.setReturnReason(
            OBDal.getInstance().get(ReturnReason.class, selectedLine.getString("returnReason")));
      } else {
        newOrderLine.setReturnReason(order.getReturnReason());
      }

      if (notExistsOrderLine) {
        List<OrderLine> orderLines = order.getOrderLineList();
        orderLines.add(newOrderLine);
        order.setOrderLineList(orderLines);
      }
      // Copy dimensions from Shipment/Receipt Line to RFC/RTV Order Line
      if (shipmentLine != null) {
        newOrderLine.setProject(shipmentLine.getProject());
        newOrderLine.setCostcenter(shipmentLine.getCostcenter());
        newOrderLine.setStDimension(shipmentLine.getStDimension());
        newOrderLine.setNdDimension(shipmentLine.getNdDimension());
      }
      OBDal.getInstance().save(newOrderLine);
      OBDal.getInstance().save(order);
      OBDal.getInstance().flush();
    }

    removeNonSelectedLines(idList, order);
  }

  private void removeNonSelectedLines(List<String> idList, Order order) {
    if (idList.size() > 0) {
      for (String id : idList) {
        OrderLine ol = OBDal.getInstance().get(OrderLine.class, id);
        order.getOrderLineList().remove(ol);
        OBDal.getInstance().remove(ol);
      }
      OBDal.getInstance().save(order);
      OBDal.getInstance().flush();
    }
  }
}
