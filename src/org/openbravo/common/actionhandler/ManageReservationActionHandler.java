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
 * All portions are Copyright (C) 2013-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.common.actionhandler;

import java.math.BigDecimal;
import java.util.ArrayList;
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
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.service.db.DbUtility;

/**
 * 
 * @author gorkaion
 * 
 */
public class ManageReservationActionHandler extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();
  private static final String strOrderLineTableId = "260";
  private static final String strReservationsTableId = "77264B07BB0E4FA483A07FB40C2E0FE0";
  private static final String strResStockTableId = "7BDAC914CA60418795E453BC0E8C89DC";
  private static final String strResStockOrderTableId = "8A36D18D1D164189B7C3AE892F310E11";
  private static final String strTabResStockOrderTableId = "D6079A4A6C2542678D9A50114367B967";

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode(true);
    try {
      jsonRequest = new JSONObject(content);
      log.debug(jsonRequest);
      final String strTableId = jsonRequest.getString("inpTableId");
      boolean processReservation = false;

      Reservation reservation = null;
      if (strTableId.equals(strReservationsTableId) || strTableId.equals(strResStockTableId)
          || strTableId.equals(strTabResStockOrderTableId)) {
        final String strReservationId = jsonRequest.getString("inpmReservationId");
        reservation = OBDal.getInstance().get(Reservation.class, strReservationId);
      } else if (strTableId.equals(strOrderLineTableId)
          || strTableId.equals(strResStockOrderTableId)) {
        final String strOrderLineId = jsonRequest.getString("C_OrderLine_ID");
        final OrderLine sol = OBDal.getInstance().get(OrderLine.class, strOrderLineId);
        reservation = ReservationUtils.getReservationFromOrder(sol);
        processReservation = reservation.getRESStatus().equals("DR");
      }

      if (StringUtils.equals(reservation.getRESStatus(), "CL")) {
        throw new OBException(OBMessageUtils.messageBD("ClosedReservation"));
      }

      // Process reservation before managing stock reservations to avoid reserve all available stock
      // Issue 28051: https://issues.openbravo.com/view.php?id=28051
      if (processReservation) {
        OBError result = ReservationUtils.processReserve(reservation, "PR");
        if (result.getType().equals("Error")) {
          JSONObject errorMessage = new JSONObject();
          errorMessage.put("severity", result.getType().toLowerCase());
          errorMessage.put("text", result.getMessage());
          jsonRequest.put("message", errorMessage);
        } else {
          // Force status to completed in case it was kept as draft because no stock reservation was
          // created when processing reservation
          reservation.setRESStatus("CO");
          reservation.setRESProcess("HO");
        }
      }
      if (reservation != null) {
        // FIXME: Replace with OBDao method when handler is merged with latest pi.
        // List<String> idList = OBDao.getIDListFromOBObject(reservation
        // .getMaterialMgmtReservationStockList());
        List<String> idList = new ArrayList<String>();
        for (ReservationStock resStock : reservation.getMaterialMgmtReservationStockList()) {
          idList.add(resStock.getId());
        }
        manageReservedStockLines(jsonRequest, reservation, idList);
        // Remove reservation in case reservation has no lines
        if (reservation.getMaterialMgmtReservationStockList().isEmpty()) {
          reservation.setRESStatus("DR");
          reservation.setRESProcess("PR");
          OBDal.getInstance().flush();
          OBDal.getInstance().remove(reservation);
        }
      }

    } catch (Exception e) {
      log.error("Error in Manage Reservation Action Handler", e);

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
        // do nothing, give up
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonRequest;
  }

  private void manageReservedStockLines(JSONObject jsonRequest, Reservation reservation,
      List<String> idList) throws JSONException {
    JSONArray selectedLines = jsonRequest.getJSONObject("_params")
        .getJSONObject("grid")
        .getJSONArray("_selection");
    // if no lines selected don't do anything.
    if (selectedLines.length() == 0) {
      removeNonSelectedLines(idList, reservation);
      return;
    }
    for (int i = 0; i < selectedLines.length(); i++) {
      JSONObject selectedLine = selectedLines.getJSONObject(i);
      log.debug(selectedLine);
      ReservationStock resStock = null;
      String strReservationStockId = selectedLine.get("reservationStock").equals(null) ? ""
          : selectedLine.getString("reservationStock");
      boolean existsReservationStock = StringUtils.isNotBlank(strReservationStockId);
      if (!existsReservationStock) {
        String released = selectedLine.get("released").equals(null) ? ""
            : selectedLine.getString("released");
        if (StringUtils.isNotBlank(released)) {
          BigDecimal qtyReleased = new BigDecimal(released);
          if (qtyReleased.compareTo(BigDecimal.ZERO) != 0) {
            strReservationStockId = selectedLine.getString("id");
            existsReservationStock = true;
          }
        }
      }
      if (existsReservationStock) {
        resStock = OBDal.getInstance().get(ReservationStock.class, strReservationStockId);
        idList.remove(strReservationStockId);
      } else {
        resStock = OBProvider.getInstance().get(ReservationStock.class);
        resStock.setReservation(reservation);
        resStock.setOrganization(reservation.getOrganization());

        final String strLocator = selectedLine.get("storageBin").equals(null) ? ""
            : selectedLine.getString("storageBin");
        if (StringUtils.isNotBlank(strLocator)) {
          resStock.setStorageBin(
              (Locator) OBDal.getInstance().getProxy(Locator.ENTITY_NAME, strLocator));
        }
        final String strASIId = selectedLine.get("attributeSetValue").equals(null) ? ""
            : selectedLine.getString("attributeSetValue");
        if (StringUtils.isNotBlank(strASIId)) {
          resStock.setAttributeSetValue((AttributeSetInstance) OBDal.getInstance()
              .getProxy(AttributeSetInstance.ENTITY_NAME, strASIId));
        }
        final String strOrderLineId = selectedLine.get("purchaseOrderLine").equals(null) ? ""
            : selectedLine.getString("purchaseOrderLine");
        if (StringUtils.isNotBlank(strOrderLineId)) {
          resStock.setSalesOrderLine(
              (OrderLine) OBDal.getInstance().getProxy(OrderLine.ENTITY_NAME, strOrderLineId));
        }
        resStock.setReleased(BigDecimal.ZERO);

        List<ReservationStock> resStocks = reservation.getMaterialMgmtReservationStockList();
        resStocks.add(resStock);
        reservation.setMaterialMgmtReservationStockList(resStocks);
      }

      final Boolean isAllocated = selectedLine.getString("allocated").equals(null) ? false
          : selectedLine.getBoolean("allocated");
      resStock.setAllocated(isAllocated == true);

      final BigDecimal qty = new BigDecimal(selectedLine.getString("quantity"));
      resStock.setQuantity(qty);

      OBDal.getInstance().save(resStock);
      OBDal.getInstance().save(reservation);
      OBDal.getInstance().flush();
    }

    removeNonSelectedLines(idList, reservation);
  }

  private void removeNonSelectedLines(List<String> idList, Reservation reservation) {
    if (idList.size() > 0) {
      for (String id : idList) {
        ReservationStock resStock = OBDal.getInstance().get(ReservationStock.class, id);
        if (resStock.getReleased() == null
            || resStock.getReleased().compareTo(BigDecimal.ZERO) == 0) {
          reservation.getMaterialMgmtReservationStockList().remove(resStock);
          OBDal.getInstance().remove(resStock);
        }
      }
      OBDal.getInstance().save(reservation);
      OBDal.getInstance().flush();
    }
  }
}
