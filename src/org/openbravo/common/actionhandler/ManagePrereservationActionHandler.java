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
 * All portions are Copyright (C) 2012-2016 Openbravo SLU 
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
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.onhandquantity.PrereservationManualPickEdit;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.service.db.DbUtility;

/**
 * 
 * @author gorkaion
 * 
 */
public class ManagePrereservationActionHandler extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode(true);
    try {
      jsonRequest = new JSONObject(content);
      log.debug(jsonRequest);

      final String strPOLId = jsonRequest.getString("inpcOrderlineId");
      final OrderLine pol = OBDal.getInstance().get(OrderLine.class, strPOLId);

      if (pol != null) {
        OBCriteria<ReservationStock> critRS = OBDao.getFilteredCriteria(ReservationStock.class,
            Restrictions.eq(ReservationStock.PROPERTY_SALESORDERLINE, pol),
            Restrictions.isNull(ReservationStock.PROPERTY_STORAGEBIN));
        List<String> idList = new ArrayList<String>();
        for (ReservationStock resStock : critRS.list()) {
          idList.add(resStock.getId());
        }
        managePrereservedStockLines(jsonRequest, pol, idList);
      }

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
        // do nothing, give up
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonRequest;
  }

  private void managePrereservedStockLines(JSONObject jsonRequest, OrderLine pol,
      List<String> idList) throws JSONException {
    JSONArray selectedLines = jsonRequest.getJSONObject("_params")
        .getJSONObject("grid")
        .getJSONArray("_selection");
    // if no lines selected don't do anything.
    if (selectedLines.length() == 0) {
      removeNonSelectedLines(idList, pol);
      return;
    }
    for (int i = 0; i < selectedLines.length(); i++) {
      JSONObject selectedLine = selectedLines.getJSONObject(i);
      log.debug(selectedLine);
      ReservationStock resStock = null;
      String strReservationStockId = selectedLine
          .get(PrereservationManualPickEdit.PROPERTY_RESERVATIONSTOCK)
          .equals(null) ? ""
              : selectedLine.getString(PrereservationManualPickEdit.PROPERTY_RESERVATIONSTOCK);
      boolean existsReservationStock = StringUtils.isNotBlank(strReservationStockId);

      if (existsReservationStock) {
        resStock = OBDal.getInstance().get(ReservationStock.class, strReservationStockId);
        idList.remove(strReservationStockId);
      } else {
        resStock = OBProvider.getInstance().get(ReservationStock.class);

        final String strSOLId = selectedLine
            .getString(PrereservationManualPickEdit.PROPERTY_SALESORDERLINE);
        OrderLine sol = OBDal.getInstance().get(OrderLine.class, strSOLId);
        Reservation reservation = ReservationUtils.getReservationFromOrder(sol);
        resStock.setReservation(reservation);
        resStock.setOrganization(reservation.getOrganization());
        resStock.setSalesOrderLine(pol);
        resStock.setReleased(BigDecimal.ZERO);

        List<ReservationStock> resStocks = pol.getMaterialMgmtReservationStockList();
        resStocks.add(resStock);
        pol.setMaterialMgmtReservationStockList(resStocks);
        OBDal.getInstance().save(pol);
      }

      final BigDecimal qty = new BigDecimal(
          selectedLine.getString(PrereservationManualPickEdit.PROPERTY_RESERVEDQTY));
      final boolean allocated = selectedLine
          .getBoolean(PrereservationManualPickEdit.PROPERTY_ALLOCATED);
      resStock.setAllocated(allocated);
      resStock.setQuantity(qty);

      OBDal.getInstance().save(resStock);
      OBDal.getInstance().flush();
    }

    removeNonSelectedLines(idList, pol);
  }

  private void removeNonSelectedLines(List<String> idList, OrderLine pol) {
    if (idList.size() > 0) {
      for (String id : idList) {
        ReservationStock resStock = OBDal.getInstance().get(ReservationStock.class, id);
        pol.getMaterialMgmtReservationStockList().remove(resStock);
        OBDal.getInstance().remove(resStock);
      }
      OBDal.getInstance().save(pol);
      OBDal.getInstance().flush();
    }
  }
}
