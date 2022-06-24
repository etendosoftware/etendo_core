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
 * All portions are Copyright (C) 2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler.copyfromorderprocess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.service.db.CallProcess;

public class CopyFromOrdersProcess {
  @Inject
  @Any
  private Instance<CopyFromOrdersProcessImplementationInterface> copyFromOrdersProcessHooks;

  private static final Logger log = LogManager.getLogger();
  private static final String EXPLODE_BOM_PROCESS = "DFC78024B1F54CBB95DC73425BA6687F";

  private Order processingOrder;
  // Last Line number of the Processing Order
  private Long lastLineNo = 0L;
  // Order Lines to Explode non-stocked BOM Products
  private List<OrderLine> explodeBOMOrderLines = new ArrayList<OrderLine>();

  /**
   * This process copies the Order Lines of the selected Orders into the Order that is being
   * processed by this same Process
   * <ul>
   * <li>Retrieve the Order Lines of each Order that are not Discounts or are Non-Stocked BOM
   * Products and for each one:</li>
   * <li>1. Update Order and Order Line related information</li>
   * <li>2. Copy attributes</li>
   * <li>3. Calculate amounts and UOM's</li>
   * <li>4. Calculate Prices based on price list</li>
   * <li>5. Recalculate Taxes</li>
   * <li>Explodes the non-stocked BOM Products to create the corresponding Order Lines</li>
   * </ul>
   * 
   * @param selectedOrders
   *          . Orders from which the lines are going to be copied
   * @return The number of orders properly copied
   */
  public int copyOrderLines(final Order processingOrderParam, final JSONArray selectedOrders) {
    this.processingOrder = processingOrderParam;
    OBContext.setAdminMode(true);
    try {
      long startTime = System.currentTimeMillis();
      int createdOrderLinesCount = createOrderLinesFromSelectedOrders(selectedOrders);
      processExplodeBOMOrderLines();
      long endTime = System.currentTimeMillis();
      log.debug(String.format("CopyFromOrdersProcess: Time taken to complete the process: %d ms",
          (endTime - startTime)));
      return createdOrderLinesCount;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Creates order lines from selected orders. Iterates all the selected orders and copies it's
   * lines to the processing order
   * 
   * @param selectedOrders
   *          The selected orders from the lines will be copied.
   * @return The created order lines count
   */
  private int createOrderLinesFromSelectedOrders(final JSONArray selectedOrders) {
    // Initialize the line number with the last one in the processing Order.
    lastLineNo = getLastLineNoOfCurrentOrder();
    int createdOrderLinesCount = 0;
    for (int index = 0; index < selectedOrders.length(); index++) {
      Order selectedOrder = getSelectedOrderInPosition(selectedOrders, index);
      createdOrderLinesCount += createOrderLinesFromSelectedOrder(selectedOrder);
    }
    return createdOrderLinesCount;
  }

  private Order getSelectedOrderInPosition(final JSONArray selectedOrders, final int index) {
    try {
      String selectedOrderId = selectedOrders.getJSONObject(index).getString("id");
      return OBDal.getInstance().get(Order.class, selectedOrderId);
    } catch (JSONException e) {
      log.error(OBMessageUtils.messageBD("CopyFromOrdersError"),
          "Error in CopyFromOrdersProcess when reading a JSONObject", e);
      throw new OBException(e);
    }
  }

  /**
   * Creates order lines from selected order. Get all the selected order lines and copies them to
   * the processing order.
   * 
   * @param selectedOrder
   *          A selected order to be copied
   * @return The created order lines count
   */
  private int createOrderLinesFromSelectedOrder(final Order selectedOrder) {
    int createdOrderLinesCount = 0;
    // Iterate the Order Lines, excluding those ones that has been created from BOM explode Process
    ScrollableResults orderLines = getOrderLinesExcludingDiscountsAndExplodedBOMLines(
        selectedOrder);
    try {
      while (orderLines.next()) {
        OrderLine orderLine = (OrderLine) orderLines.get()[0];
        OrderLine newOrderLine = createLineFromSelectedOrderLineAndRunHooks(orderLine);
        processingOrder.getOrderLineList().add(newOrderLine);
        OBDal.getInstance().save(newOrderLine);
        OBDal.getInstance().save(processingOrder);

        // If original Order Line has already been processed to explode it's BOM List
        if (orderLine.isExplode()) {
          addOrderLineToExplodeBOMList(newOrderLine);
        }

        createdOrderLinesCount++;
      }
    } finally {
      orderLines.close();
    }
    OBDal.getInstance().flush();
    return createdOrderLinesCount;
  }

  private ScrollableResults getOrderLinesExcludingDiscountsAndExplodedBOMLines(final Order order) {
    OBCriteria<OrderLine> obc = OBDal.getInstance().createCriteria(OrderLine.class);
    obc.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, order));
    obc.add(Restrictions.isNull(OrderLine.PROPERTY_BOMPARENT));
    obc.add(Restrictions.isNull(OrderLine.PROPERTY_ORDERDISCOUNT));
    obc.addOrderBy(OrderLine.PROPERTY_LINENO, true);
    return obc.scroll(ScrollMode.FORWARD_ONLY);
  }

  /**
   * Creates a new order line from an already existing one
   * 
   * @param orderLine
   *          The order line to be copied
   * @return The created order line
   */
  private OrderLine createLineFromSelectedOrderLineAndRunHooks(final OrderLine orderLine) {
    long startTime = System.currentTimeMillis();

    OrderLine newOrderLine = OBProvider.getInstance().get(OrderLine.class);

    // Always increment the lineNo when adding a new order line
    newOrderLine.setLineNo(nextLineNo());

    // Execute Hooks to perform operations
    executeHooks(orderLine, newOrderLine);

    long endTime = System.currentTimeMillis();
    log.debug(String.format(
        "CopyFromOrdersProcess: Time taken to copy a line from the previous Order: %d ms",
        (endTime - startTime)));

    return newOrderLine;
  }

  private Long nextLineNo() {
    lastLineNo = lastLineNo + 10L;
    return lastLineNo;
  }

  private void executeHooks(final OrderLine orderLine, OrderLine newOrderLine) {
    if (copyFromOrdersProcessHooks != null) {
      final List<CopyFromOrdersProcessImplementationInterface> hooks = new ArrayList<>();
      for (CopyFromOrdersProcessImplementationInterface hook : copyFromOrdersProcessHooks
          .select(new ComponentProvider.Selector(
              CopyFromOrdersProcessImplementationInterface.COPY_FROM_ORDER_PROCESS_HOOK_QUALIFIER))) {
        if (hook != null) {
          hooks.add(hook);
        }
      }

      Collections.sort(hooks, new CopyFromOrdersHookComparator());
      for (CopyFromOrdersProcessImplementationInterface hook : hooks) {
        hook.exec(processingOrder, orderLine, newOrderLine);
      }
    }
  }

  private class CopyFromOrdersHookComparator
      implements Comparator<CopyFromOrdersProcessImplementationInterface> {
    @Override
    public int compare(CopyFromOrdersProcessImplementationInterface a,
        CopyFromOrdersProcessImplementationInterface b) {
      return a.getOrder() < b.getOrder() ? -1 : a.getOrder() == b.getOrder() ? 0 : 1;
    }
  }

  /**
   * Returns the max order line number defined in the order to which the lines are going to be added
   *
   * @return The max order line number
   */
  private Long getLastLineNoOfCurrentOrder() {
    OBCriteria<OrderLine> obc = OBDal.getInstance().createCriteria(OrderLine.class);
    obc.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, processingOrder));
    obc.setProjection(Projections.max(OrderLine.PROPERTY_LINENO));
    Long lineNumber = 0L;
    obc.setMaxResults(1);
    Object o = obc.uniqueResult();
    if (o != null) {
      lineNumber = (Long) o;
    }
    return lineNumber;
  }

  private void addOrderLineToExplodeBOMList(final OrderLine newOrderLine) {
    explodeBOMOrderLines.add(newOrderLine);
  }

  private void processExplodeBOMOrderLines() {
    long startTime = System.currentTimeMillis();
    for (OrderLine orderLine : explodeBOMOrderLines) {
      OBDal.getInstance().refresh(orderLine);
      org.openbravo.model.ad.ui.Process process = OBDal.getInstance()
          .getProxy(org.openbravo.model.ad.ui.Process.class, EXPLODE_BOM_PROCESS);

      final ProcessInstance pInstance = CallProcess.getInstance()
          .call(process, orderLine.getId(), null);

      if (pInstance.getResult() == 0) {
        throw new OBException("Error executing Explode process");
      }
    }
    long endTime = System.currentTimeMillis();
    log.debug(String.format("CopyFromOrdersProcess: Time taken to explode BOM Lines: %d ms",
        (endTime - startTime)));
  }

}
