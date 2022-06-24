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
 * All portions are Copyright (C) 2015-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.materialmgmt.ServicePriceUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderlineServiceRelation;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.db.DalConnectionProvider;

public class ServiceOrderLineRelate extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();
  private static final String UNIQUE_QUANTITY = "UQ";
  private static final String RFC_ORDERLINE_TAB_ID = "AF4090093D471431E040007F010048A5";

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode(true);
    JSONObject errorMessage = new JSONObject();
    ScrollableResults scroller = null;
    try {
      jsonRequest = new JSONObject(content);
      log.debug("{}", jsonRequest);

      JSONArray selectedLines = jsonRequest.getJSONObject("_params")
          .getJSONObject("grid")
          .getJSONArray("_selection");

      final String tabId = jsonRequest.getString("inpTabId");
      final BigDecimal signum = RFC_ORDERLINE_TAB_ID.equals(tabId) ? new BigDecimal("-1")
          : BigDecimal.ONE;

      BigDecimal totalPositiveLinesQuantity = BigDecimal.ZERO;
      BigDecimal totalNegativeLinesQuantity = BigDecimal.ZERO;
      BigDecimal totalPositiveLinesAmount = BigDecimal.ZERO;
      BigDecimal totalNegativeLinesAmount = BigDecimal.ZERO;
      BigDecimal totalPositiveLinesDiscount = BigDecimal.ZERO;
      BigDecimal totalNegativeLinesDiscount = BigDecimal.ZERO;
      BigDecimal totalPositiveLinesPrice = BigDecimal.ZERO;
      BigDecimal totalNegativeLinesPrice = BigDecimal.ZERO;
      BigDecimal totalPositiveLinesUnitDiscount = BigDecimal.ZERO;
      BigDecimal totalNegativeLinesUnitDiscount = BigDecimal.ZERO;

      OrderLine secondOrderline = null;

      final Client serviceProductClient = (Client) OBDal.getInstance()
          .getProxy(Client.ENTITY_NAME, jsonRequest.getString("inpadClientId"));
      final Organization serviceProductOrg = (Organization) OBDal.getInstance()
          .getProxy(Organization.ENTITY_NAME, jsonRequest.getString("inpadOrgId"));
      OrderLine mainOrderLine = (OrderLine) OBDal.getInstance()
          .getProxy(OrderLine.ENTITY_NAME, jsonRequest.getString("inpcOrderlineId"));
      final Product serviceProduct = mainOrderLine.getProduct();
      final String orderId = mainOrderLine.getSalesOrder().getId();
      final Long lineNo = ServicePriceUtils.getNewLineNo(orderId);

      /*
       * Check if the order line has positive or negative relations. If it has no relations then
       * false
       */

      boolean existingLinesNegative = existsNegativeLines(mainOrderLine);

      // Delete existing rows
      //@formatter:off
      String hql = " as rol "
                 + " where salesOrderLine.id = :orderLineId ";
      //@formatter:on
      scroller = OBDal.getInstance()
          .createQuery(OrderlineServiceRelation.class, hql)
          .setNamedParameter("orderLineId", mainOrderLine.getId())
          .setMaxResult(1000)
          .scroll(ScrollMode.FORWARD_ONLY);

      while (scroller.next()) {
        final OrderlineServiceRelation or = (OrderlineServiceRelation) scroller.get()[0];
        OBDal.getInstance().remove(or);
      }
      OBDal.getInstance().flush();

      mainOrderLine = (OrderLine) OBDal.getInstance()
          .getProxy(OrderLine.ENTITY_NAME, jsonRequest.getString("inpcOrderlineId"));

      boolean positiveLines = false;
      boolean negativeLines = false;

      // Check if there are negative quantity and positive quantity lines
      for (int i = 0; i < selectedLines.length(); i++) {
        JSONObject selectedLine = selectedLines.getJSONObject(i);
        BigDecimal lineAmount = BigDecimal.valueOf(selectedLine.getDouble("amount"));
        BigDecimal lineQuantity = BigDecimal.valueOf(selectedLine.getDouble("relatedQuantity"));
        BigDecimal lineDiscount = BigDecimal.valueOf(selectedLine.getDouble("discountsAmount"));
        BigDecimal linePrice = BigDecimal.valueOf(selectedLine.getDouble("price"));
        BigDecimal lineUnitDiscount = BigDecimal
            .valueOf(selectedLine.getDouble("unitDiscountsAmt"));
        if (lineQuantity.compareTo(BigDecimal.ZERO) < 0) {
          // There are negative quantity lines
          negativeLines = true;
          totalNegativeLinesQuantity = totalNegativeLinesQuantity.add(lineQuantity);
          totalNegativeLinesAmount = totalNegativeLinesAmount.add(lineAmount);
          totalNegativeLinesDiscount = totalNegativeLinesDiscount.add(lineDiscount);
          totalNegativeLinesPrice = totalNegativeLinesPrice.add(linePrice);
          totalNegativeLinesUnitDiscount = totalNegativeLinesUnitDiscount.add(lineUnitDiscount);
        }
        if (lineQuantity.compareTo(BigDecimal.ZERO) > 0) {
          // There are positive quantity lines
          positiveLines = true;
          totalPositiveLinesQuantity = totalPositiveLinesQuantity.add(lineQuantity);
          totalPositiveLinesAmount = totalPositiveLinesAmount.add(lineAmount);
          totalPositiveLinesDiscount = totalPositiveLinesDiscount.add(lineDiscount);
          totalPositiveLinesPrice = totalPositiveLinesPrice.add(linePrice);
          totalPositiveLinesUnitDiscount = totalPositiveLinesUnitDiscount.add(lineUnitDiscount);
        }
        if (negativeLines && positiveLines) {
          break;
        }
      }

      final boolean positiveLinesIsAfterDiscounts = ServicePriceUtils
          .servicePriceRuleIsAfterDiscounts(mainOrderLine, totalPositiveLinesAmount,
              totalPositiveLinesDiscount, totalPositiveLinesPrice, totalPositiveLinesUnitDiscount);
      final boolean negativeLinesIsAfterDiscounts = ServicePriceUtils
          .servicePriceRuleIsAfterDiscounts(mainOrderLine, totalNegativeLinesAmount,
              totalNegativeLinesDiscount, totalNegativeLinesPrice, totalNegativeLinesUnitDiscount);

      // Adding new rows
      for (int i = 0; i < selectedLines.length(); i++) {
        JSONObject selectedLine = selectedLines.getJSONObject(i);
        log.debug("{}", selectedLine);
        final OrderLine orderLine = (OrderLine) OBDal.getInstance()
            .getProxy(OrderLine.ENTITY_NAME, selectedLine.getString(OrderLine.PROPERTY_ID));

        // Check if deferred sale is allowed for the service, does not apply for returns
        if (!RFC_ORDERLINE_TAB_ID.equals(tabId)) {
          ServicePriceUtils.deferredSaleAllowed(mainOrderLine, orderLine);
        }

        BigDecimal lineAmount = BigDecimal.valueOf(selectedLine.getDouble("amount"));
        BigDecimal lineQuantity = BigDecimal.valueOf(selectedLine.getDouble("relatedQuantity"));
        BigDecimal lineDiscount = BigDecimal.valueOf(selectedLine.getDouble("discountsAmount"));

        if (lineQuantity.compareTo(BigDecimal.ZERO) < 0 && secondOrderline == null
            && positiveLines) {
          // New order line due to there are positive quantity and negative quantity relations
          secondOrderline = (OrderLine) DalUtil.copy(mainOrderLine, false);
          secondOrderline.setLineNo(lineNo);
          secondOrderline.setId(SequenceIdData.getUUID());
          secondOrderline.setNewOBObject(true);
          OBDal.getInstance().save(secondOrderline);
          OBDal.getInstance().flush();
          OBDal.getInstance().refresh(secondOrderline);
        }

        OrderlineServiceRelation olsr = OBProvider.getInstance()
            .get(OrderlineServiceRelation.class);
        olsr.setClient(serviceProductClient);
        olsr.setOrganization(serviceProductOrg);
        olsr.setOrderlineRelated(orderLine);
        if ((lineQuantity.compareTo(BigDecimal.ZERO) < 0 && positiveLines && !existingLinesNegative)
            || (lineQuantity.compareTo(BigDecimal.ZERO) > 0 && existingLinesNegative
                && negativeLines)) {
          olsr.setSalesOrderLine(secondOrderline);
        } else {
          olsr.setSalesOrderLine(mainOrderLine);
        }
        if (lineQuantity.compareTo(BigDecimal.ZERO) < 0) {
          if (negativeLinesIsAfterDiscounts) {
            olsr.setAmount(lineAmount.multiply(signum)
                .setScale(mainOrderLine.getCurrency().getPricePrecision().intValue(),
                    RoundingMode.HALF_UP));
          } else {
            olsr.setAmount(lineAmount.add(lineDiscount)
                .multiply(signum)
                .setScale(mainOrderLine.getCurrency().getPricePrecision().intValue(),
                    RoundingMode.HALF_UP));
          }
        } else {
          if (positiveLinesIsAfterDiscounts) {
            olsr.setAmount(lineAmount.multiply(signum)
                .setScale(mainOrderLine.getCurrency().getPricePrecision().intValue(),
                    RoundingMode.HALF_UP));
          } else {
            olsr.setAmount(lineAmount.add(lineDiscount)
                .multiply(signum)
                .setScale(mainOrderLine.getCurrency().getPricePrecision().intValue(),
                    RoundingMode.HALF_UP));
          }
        }
        olsr.setQuantity(lineQuantity.multiply(signum));
        OBDal.getInstance().save(olsr);
        if ((i % 100) == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
      }

      // Update orderlines

      BigDecimal baseProductPrice = ServicePriceUtils.getProductPrice(
          mainOrderLine.getSalesOrder().getOrderDate(),
          mainOrderLine.getSalesOrder().getPriceList(), serviceProduct);

      BigDecimal firstLineQuantity = BigDecimal.ZERO;
      BigDecimal secondLineQuantity = BigDecimal.ZERO;
      BigDecimal firstLineAmount = BigDecimal.ZERO;
      BigDecimal secondLineAmount = BigDecimal.ZERO;
      BigDecimal firstLineDiscount = BigDecimal.ZERO;
      BigDecimal secondLineDiscount = BigDecimal.ZERO;
      BigDecimal firstLinePrice = BigDecimal.ZERO;
      BigDecimal secondLinePrice = BigDecimal.ZERO;
      BigDecimal firstLineUnitDiscount = BigDecimal.ZERO;
      BigDecimal secondLineUnitDiscount = BigDecimal.ZERO;

      // Conditions to check which order line has negative relations and which one has positive
      // relations

      if ((!existingLinesNegative && negativeLines && !positiveLines)
          || (existingLinesNegative && negativeLines)) {
        firstLineQuantity = totalNegativeLinesQuantity;
        firstLineAmount = totalNegativeLinesAmount;
        firstLineDiscount = totalNegativeLinesDiscount;
        firstLinePrice = totalNegativeLinesPrice;
        firstLineUnitDiscount = totalNegativeLinesUnitDiscount;
        secondLineQuantity = totalPositiveLinesQuantity;
        secondLineAmount = totalPositiveLinesAmount;
        secondLineDiscount = totalPositiveLinesDiscount;
        secondLinePrice = totalPositiveLinesPrice;
        secondLineUnitDiscount = totalPositiveLinesUnitDiscount;
        if (UNIQUE_QUANTITY.equals(serviceProduct.getQuantityRule())
            || firstLineQuantity.compareTo(BigDecimal.ZERO) == 0) {
          firstLineQuantity = new BigDecimal("-1");
        }
        if (UNIQUE_QUANTITY.equals(serviceProduct.getQuantityRule())
            || secondLineQuantity.compareTo(BigDecimal.ZERO) == 0) {
          secondLineQuantity = BigDecimal.ONE;
        }
      } else {
        firstLineQuantity = totalPositiveLinesQuantity;
        firstLineAmount = totalPositiveLinesAmount;
        firstLineDiscount = totalPositiveLinesDiscount;
        firstLinePrice = totalPositiveLinesPrice;
        firstLineUnitDiscount = totalPositiveLinesUnitDiscount;
        secondLineQuantity = totalNegativeLinesQuantity;
        secondLineAmount = totalNegativeLinesAmount;
        secondLineDiscount = totalNegativeLinesDiscount;
        secondLinePrice = totalNegativeLinesPrice;
        secondLineUnitDiscount = totalNegativeLinesUnitDiscount;
        if (UNIQUE_QUANTITY.equals(serviceProduct.getQuantityRule())
            || firstLineQuantity.compareTo(BigDecimal.ZERO) == 0) {
          firstLineQuantity = BigDecimal.ONE;
        }
        if (UNIQUE_QUANTITY.equals(serviceProduct.getQuantityRule())
            || secondLineQuantity.compareTo(BigDecimal.ZERO) == 0) {
          secondLineQuantity = new BigDecimal("-1");
        }
      }

      // Update main order line total values
      updateOrderline(mainOrderLine, firstLineAmount, firstLineQuantity, firstLineDiscount,
          firstLinePrice, firstLineUnitDiscount, baseProductPrice, signum);

      // Update new created sales order line total values
      if (secondOrderline != null) {
        updateOrderline(secondOrderline, secondLineAmount, secondLineQuantity, secondLineDiscount,
            secondLinePrice, secondLineUnitDiscount, baseProductPrice, signum);
      }
      OBDal.getInstance().flush();

      errorMessage.put("severity", "success");
      errorMessage.put("title", OBMessageUtils.messageBD("Success"));
      jsonRequest.put("message", errorMessage);
    } catch (Exception e) {
      log.error("Error in ServiceOrderLineRelate Action Handler", e);
      OBDal.getInstance().rollbackAndClose();
      try {
        jsonRequest = new JSONObject();
        String message = OBMessageUtils.parseTranslation(new DalConnectionProvider(false),
            RequestContext.get().getVariablesSecureApp(),
            OBContext.getOBContext().getLanguage().getLanguage(), e.getMessage());
        errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonRequest.put("message", errorMessage);

      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
      }
    } finally {
      if (scroller != null) {
        scroller.close();
      }
      OBContext.restorePreviousMode();
    }
    return jsonRequest;
  }

  private void updateOrderline(OrderLine mainOrderLine, BigDecimal lineAmount,
      BigDecimal lineQuantity, BigDecimal lineDiscount, BigDecimal linePrice,
      BigDecimal lineUnitDiscount, BigDecimal baseProductPrice, BigDecimal signum) {

    BigDecimal listPrice;
    final Currency currency = mainOrderLine.getCurrency();

    BigDecimal serviceAmount = ServicePriceUtils.getServiceAmount(mainOrderLine, lineAmount,
        lineDiscount, linePrice, lineQuantity, lineUnitDiscount);

    BigDecimal servicePrice = baseProductPrice.add(serviceAmount.divide(lineQuantity,
        currency.getPricePrecision().intValue(), RoundingMode.HALF_UP));
    serviceAmount = serviceAmount.add(baseProductPrice.multiply(lineQuantity))
        .setScale(currency.getPricePrecision().intValue(), RoundingMode.HALF_UP);

    if (mainOrderLine.getSalesOrder().isPriceIncludesTax()) {
      mainOrderLine.setGrossUnitPrice(servicePrice);
      mainOrderLine.setLineGrossAmount(serviceAmount);
      mainOrderLine.setBaseGrossUnitPrice(servicePrice);
      listPrice = mainOrderLine.getGrossListPrice();
    } else {
      mainOrderLine.setUnitPrice(servicePrice);
      mainOrderLine.setLineNetAmount(serviceAmount);
      mainOrderLine.setStandardPrice(servicePrice);
      listPrice = mainOrderLine.getListPrice();
    }
    mainOrderLine.setTaxableAmount(serviceAmount);
    // Multiply with signum depending if the process is executed from Sales Order Line or from RFC
    // Line
    mainOrderLine.setOrderedQuantity(lineQuantity.multiply(signum));

    // Calculate discount
    BigDecimal discount = listPrice.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
        : listPrice.subtract(servicePrice)
            .multiply(new BigDecimal("100"))
            .divide(listPrice, currency.getPricePrecision().intValue(), RoundingMode.HALF_EVEN);
    mainOrderLine.setDiscount(discount);
    OBDal.getInstance().save(mainOrderLine);
  }

  private boolean existsNegativeLines(OrderLine mainOrderLine) {
    //@formatter:off
    String hql = " as olsr"
               + " where olsr.salesOrderLine.id = :salesorderline";
    //@formatter:on
    OrderlineServiceRelation osr = OBDal.getInstance()
        .createQuery(OrderlineServiceRelation.class, hql)
        .setNamedParameter("salesorderline", mainOrderLine.getId())
        .setMaxResult(1)
        .uniqueResult();
    if (osr != null) {
      return osr.getQuantity().compareTo(BigDecimal.ZERO) < 0;
    }
    return false;
  }
}
