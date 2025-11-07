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
 * Contributor(s):  Mallikarjun M
 ************************************************************************
 */
package org.openbravo.client.application.businesslogic;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.CloneOrderHookCaller;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderlineServiceRelation;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;

import jakarta.enterprise.context.Dependent;

/**
 * When user on the Sales Order window and have a Sales Order displayed / selected, you then click a
 * button on the toolbar (where the 'new' order button is, among other buttons) called 'Clone
 * Order'. The process would then create a new order, and copy the information from the old order to
 * the new one.
 * 
 * @author Mallikarjun M
 * 
 */
@Deprecated
@Dependent
public class CloneOrderActionHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    final DataToJsonConverter jsonConverter = new DataToJsonConverter();
    JSONObject json = null;
    try {
      String orderId = (String) parameters.get("recordId");
      User currentUser = OBContext.getOBContext().getUser();
      Order objOrder = OBDal.getInstance().get(Order.class, orderId);

      Order objCloneOrder = cloneOrder(currentUser, objOrder);

      json = jsonConverter.toJsonObject(objCloneOrder, DataResolvingMode.FULL);
      OBDal.getInstance().commitAndClose();
      return json;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  private Order cloneOrder(final User currentUser, final Order objOrder) throws Exception {
    Order objCloneOrder = (Order) DalUtil.copy(objOrder, false);
    objCloneOrder.setDocumentAction("CO");
    objCloneOrder.setDocumentStatus("DR");
    objCloneOrder.setPosted("N");
    objCloneOrder.setProcessed(false);
    objCloneOrder.setDelivered(false);
    objCloneOrder.setSalesTransaction(true);
    objCloneOrder.setDocumentNo(null);
    objCloneOrder.setSalesTransaction(objOrder.isSalesTransaction());
    objCloneOrder.setCreationDate(new Date());
    objCloneOrder.setUpdated(new Date());
    objCloneOrder.setCreatedBy(currentUser);
    objCloneOrder.setUpdatedBy(currentUser);
    objCloneOrder.setReservationStatus(null);
    // Setting order date and scheduled delivery date of header and the order lines to current
    // date to avoid issues with tax rates. Refer issue
    // https://issues.openbravo.com/view.php?id=23671
    Date date = new Date();
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    objCloneOrder.setOrderDate(cal.getTime());
    objCloneOrder.setScheduledDeliveryDate(cal.getTime());
    objCloneOrder.setGrandTotalAmount(BigDecimal.ZERO);
    objCloneOrder.setSummedLineAmount(BigDecimal.ZERO);

    // Calling Clone Order Hook
    WeldUtils.getInstanceFromStaticBeanManager(CloneOrderHookCaller.class)
        .executeHook(objCloneOrder);

    // save the cloned order object
    OBDal.getInstance().save(objCloneOrder);

    // Clone the Order Lines related to the original Order
    cloneOrderLines(currentUser, objOrder, objCloneOrder);

    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(objCloneOrder);
    return objCloneOrder;
  }

  private void cloneOrderLines(final User currentUser, final Order objOrder, Order objCloneOrder) {
    Map<String, OrderLine> mapOriginalOrderLineWithCloneOrderLine = new HashMap<>();
    List<OrderlineServiceRelation> orderLinesServiceRelation = new ArrayList<>();
    List<OrderLine> orderLinesCreatedFromExplodedBOM = new ArrayList<>();

    for (OrderLine ordLine : objOrder.getOrderLineList()) {
      String strPriceVersionId = getPriceListVersion(objOrder.getPriceList().getId(),
          objOrder.getClient().getId());
      BigDecimal bdPriceList = getPriceList(ordLine.getProduct().getId(), strPriceVersionId);
      OrderLine objCloneOrdLine = (OrderLine) DalUtil.copy(ordLine, false);
      objCloneOrdLine.setReservedQuantity(new BigDecimal("0"));
      objCloneOrdLine.setDeliveredQuantity(new BigDecimal("0"));
      objCloneOrdLine.setInvoicedQuantity(new BigDecimal("0"));
      if (bdPriceList != null && bdPriceList.compareTo(BigDecimal.ZERO) != 0) {
        objCloneOrdLine.setListPrice(bdPriceList);
      }
      objCloneOrdLine.setCreationDate(new Date());
      objCloneOrdLine.setUpdated(new Date());
      objCloneOrdLine.setCreatedBy(currentUser);
      objCloneOrdLine.setUpdatedBy(currentUser);
      objCloneOrdLine.setOrderDate(new Date());
      objCloneOrdLine.setScheduledDeliveryDate(new Date());
      objCloneOrder.getOrderLineList().add(objCloneOrdLine);
      objCloneOrdLine.setSalesOrder(objCloneOrder);
      objCloneOrdLine.setReservationStatus(null);

      mapOriginalOrderLineWithCloneOrderLine.put(ordLine.getId(), objCloneOrdLine);
      List<OrderlineServiceRelation> lineServiceRelation = cloneProductServiceRelation(ordLine,
          objCloneOrdLine);
      orderLinesServiceRelation.addAll(lineServiceRelation);
      if (ordLine.getBOMParent() != null) {
        orderLinesCreatedFromExplodedBOM.add(ordLine);
      }
    }

    fixRelatedServicesReferences(mapOriginalOrderLineWithCloneOrderLine, orderLinesServiceRelation);

    fixRelatedBOMProductsReferences(mapOriginalOrderLineWithCloneOrderLine,
        orderLinesCreatedFromExplodedBOM);

    mapOriginalOrderLineWithCloneOrderLine.clear();
    orderLinesServiceRelation.clear();
    orderLinesCreatedFromExplodedBOM.clear();
  }

  private String getPriceListVersion(final String priceList, final String clientId) {
    try {
      String whereClause = " as plv left outer join plv.priceList pl where plv.active='Y' and plv.active='Y' and "
          + " pl.id = :priceList and plv.client.id = :clientId order by plv.validFromDate desc";

      OBQuery<PriceListVersion> ppriceListVersion = OBDal.getInstance()
          .createQuery(PriceListVersion.class, whereClause);
      ppriceListVersion.setNamedParameter("priceList", priceList);
      ppriceListVersion.setNamedParameter("clientId", clientId);

      if (!ppriceListVersion.list().isEmpty()) {
        return ppriceListVersion.list().get(0).getId();
      } else {
        return "0";
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  private BigDecimal getPriceList(final String strProductID, final String strPriceVersionId) {
    BigDecimal bdPriceList = null;
    try {
      final List<Object> parameters = new ArrayList<Object>();
      parameters.add(strProductID);
      parameters.add(strPriceVersionId);
      final String procedureName = "M_BOM_PriceList";
      bdPriceList = (BigDecimal) CallStoredProcedure.getInstance()
          .call(procedureName, parameters, null);
    } catch (Exception e) {
      throw new OBException(e);
    }

    return (bdPriceList);
  }

  private List<OrderlineServiceRelation> cloneProductServiceRelation(final OrderLine ordLine,
      OrderLine objCloneOrdLine) {
    List<OrderlineServiceRelation> cloneServiceRelation = new ArrayList<>(
        ordLine.getOrderlineServiceRelationList().size());
    for (OrderlineServiceRelation orderLineServiceRelation : ordLine
        .getOrderlineServiceRelationList()) {
      OrderlineServiceRelation lineServiceRelation = (OrderlineServiceRelation) DalUtil
          .copy(orderLineServiceRelation, false);
      lineServiceRelation.setOrderlineRelated(orderLineServiceRelation.getOrderlineRelated());
      lineServiceRelation.setSalesOrderLine(objCloneOrdLine);
      cloneServiceRelation.add(lineServiceRelation);
    }
    objCloneOrdLine.setOrderlineServiceRelationList(cloneServiceRelation);

    return cloneServiceRelation;
  }

  private void fixRelatedServicesReferences(
      final Map<String, OrderLine> mapOriginalOrderLineWithCloneOrderLine,
      final List<OrderlineServiceRelation> orderLinesServiceRelation) {
    for (OrderlineServiceRelation lineServiceRelation : orderLinesServiceRelation) {
      OrderLine clonedOrderLine = mapOriginalOrderLineWithCloneOrderLine
          .get(lineServiceRelation.getOrderlineRelated().getId());
      lineServiceRelation.setOrderlineRelated(clonedOrderLine);
      OBDal.getInstance().save(lineServiceRelation);
    }
  }

  private void fixRelatedBOMProductsReferences(
      final Map<String, OrderLine> mapOriginalOrderLineWithCloneOrderLine,
      final List<OrderLine> orderLinesCreatedFromExplodedBOM) {
    for (OrderLine orderLine : orderLinesCreatedFromExplodedBOM) {
      OrderLine clonedOrderLine = mapOriginalOrderLineWithCloneOrderLine.get(orderLine.getId());
      String bomParentId = orderLine.getBOMParent().getId();
      OrderLine clonedBomParent = mapOriginalOrderLineWithCloneOrderLine.get(bomParentId);
      clonedOrderLine.setBOMParent(clonedBomParent);
    }
  }

  public static BigDecimal getLineNetAmt(final String strOrderId) {
    BigDecimal bdLineNetAmt = new BigDecimal("0");
    final String readLineNetAmtHql = " select (coalesce(ol.lineNetAmount,0) + coalesce(ol.freightAmount,0) + coalesce(ol.chargeAmount,0)) as LineNetAmt from OrderLine ol where ol.salesOrder.id=:orderId";
    final Query<BigDecimal> readLineNetAmtQry = OBDal.getInstance()
        .getSession()
        .createQuery(readLineNetAmtHql, BigDecimal.class);
    readLineNetAmtQry.setParameter("orderId", strOrderId);

    for (BigDecimal amount : readLineNetAmtQry.list()) {
      bdLineNetAmt = bdLineNetAmt.add(amount);
    }

    return bdLineNetAmt;
  }

}
