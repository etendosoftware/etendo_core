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
 * All portions are Copyright (C) 2011-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.common.actionhandler;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.sales.ConditionGoods;
import org.openbravo.service.db.DbUtility;

/**
 * 
 * @author gorkaion
 * 
 */
public class RMInOutPickEditLines extends BaseProcessActionHandler {
  private static Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode();
    try {
      jsonRequest = new JSONObject(content);
      log.debug(jsonRequest);
      // When the focus is NOT in the tab of the button (i.e. any child tab) and the tab does not
      // contain any record, the inpmInoutId parameter contains "null" string. Use M_InOut_ID
      // instead because it always contains the id of the selected goods.
      // Issue 20585: https://issues.openbravo.com/view.php?id=20585
      final String strInOutId = jsonRequest.getString("M_InOut_ID");
      ShipmentInOut inOut = OBDal.getInstance().get(ShipmentInOut.class, strInOutId);
      if (inOut != null) {
        List<String> idList = OBDao
            .getIDListFromOBObject(inOut.getMaterialMgmtShipmentInOutLineList());
        createInOutLines(jsonRequest, idList);
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);

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

  private void createInOutLines(JSONObject jsonRequest, List<String> idList) throws JSONException {
    JSONArray selectedLines = jsonRequest.getJSONObject("_params")
        .getJSONObject("grid")
        .getJSONArray("_selection");
    final String strInOutId = jsonRequest.getString("M_InOut_ID");
    ShipmentInOut inOut = OBDal.getInstance().get(ShipmentInOut.class, strInOutId);
    // if no lines selected don't do anything.
    if (selectedLines.length() == 0) {
      removeNonSelectedLines(idList, inOut);
      return;
    }

    boolean isUomManagementEnabled = UOMUtil.isUomManagementEnabled();
    for (long i = 0; i < selectedLines.length(); i++) {
      JSONObject selectedLine = selectedLines.getJSONObject((int) i);
      log.debug(selectedLine);
      ShipmentInOutLine newInOutLine = null;
      boolean notExistsShipmentLine = selectedLine.get("goodsShipmentLine").equals(null);
      if (notExistsShipmentLine) {
        newInOutLine = OBProvider.getInstance().get(ShipmentInOutLine.class);
      } else {
        newInOutLine = OBDal.getInstance()
            .get(ShipmentInOutLine.class, selectedLine.get("goodsShipmentLine"));
        idList.remove(selectedLine.get("goodsShipmentLine"));
      }
      newInOutLine.setShipmentReceipt(inOut);
      newInOutLine.setOrganization(inOut.getOrganization());
      newInOutLine.setLineNo((i + 1L) * 10L);

      OrderLine orderLine = OBDal.getInstance()
          .get(OrderLine.class, selectedLine.getString("orderLine"));
      newInOutLine.setSalesOrderLine(orderLine);
      newInOutLine.setStorageBin(
          OBDal.getInstance().get(Locator.class, selectedLine.getString("storageBin")));
      newInOutLine.setProduct(orderLine.getProduct());
      newInOutLine.setAttributeSetValue(orderLine.getAttributeSetValue());
      newInOutLine.setUOM(orderLine.getUOM());
      // Ordered Quantity = returned quantity.
      BigDecimal qtyReceived = new BigDecimal(selectedLine.getString("receiving"));
      if (isUomManagementEnabled) {
        try {
          OBContext.setAdminMode(true);
          UOM operativeUOM = OBDal.getInstance()
              .get(UOM.class, selectedLine.getString("alternativeUOM"));
          newInOutLine.setOperativeUOM(operativeUOM);
          newInOutLine.setOperativeQuantity(qtyReceived.negate());
          if (alternativeUOMEqualsToReturnedUOM(selectedLine)) {
            qtyReceived = convertedQtyReceivedBasedOnTheAUM(qtyReceived, selectedLine);
          } else {
            BigDecimal operativeQuantity = convertQtyReceivedBasedOnTheUOM(qtyReceived,
                selectedLine);
            newInOutLine.setOperativeQuantity(operativeQuantity.negate());
          }
        } finally {
          OBContext.restorePreviousMode();
        }
      }
      newInOutLine.setMovementQuantity(qtyReceived.negate());

      if (selectedLine.getString("conditionGoods") != null
          && !selectedLine.getString("conditionGoods").equals("null")) {
        newInOutLine.setConditionGoods(OBDal.getInstance()
            .get(ConditionGoods.class, selectedLine.getString("conditionGoods")));
      } else {
        newInOutLine.setConditionGoods(inOut.getConditionGoods());
      }

      if (notExistsShipmentLine) {
        List<ShipmentInOutLine> inOutLines = inOut.getMaterialMgmtShipmentInOutLineList();
        inOutLines.add(newInOutLine);
        inOut.setMaterialMgmtShipmentInOutLineList(inOutLines);
      }
      if (orderLine.isExplode()) {
        newInOutLine.setExplode(true);
      }
      // Copy Dimensions From RFC Order Line
      newInOutLine.setProject(orderLine.getProject());
      newInOutLine.setCostcenter(orderLine.getCostcenter());
      newInOutLine.setStDimension(orderLine.getStDimension());
      newInOutLine.setNdDimension(orderLine.getNdDimension());

      OBDal.getInstance().save(newInOutLine);
      OBDal.getInstance().save(inOut);
      OBDal.getInstance().flush();
    }
    for (ShipmentInOutLine inOutLine : inOut.getMaterialMgmtShipmentInOutLineList()) {
      if (inOutLine.getSalesOrderLine().getBOMParent() != null) {
        OBCriteria<ShipmentInOutLine> obc = OBDal.getInstance()
            .createCriteria(ShipmentInOutLine.class);
        obc.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SALESORDERLINE,
            inOutLine.getSalesOrderLine().getBOMParent()));
        ShipmentInOutLine parentInOutLine = (ShipmentInOutLine) obc.uniqueResult();
        inOutLine.setBOMParent(parentInOutLine);
        OBDal.getInstance().save(inOutLine);
      }
    }

    removeNonSelectedLines(idList, inOut);
  }

  private BigDecimal convertQtyReceivedBasedOnTheUOM(BigDecimal qtyReceived,
      JSONObject selectedLine) throws JSONException {
    return UOMUtil.getConvertedAumQty(selectedLine.getString("product"), qtyReceived,
        selectedLine.getString("alternativeUOM"));
  }

  private BigDecimal convertedQtyReceivedBasedOnTheAUM(BigDecimal qtyReceived,
      JSONObject selectedLine) throws JSONException {
    return UOMUtil.getConvertedQty(selectedLine.getString("product"), qtyReceived,
        selectedLine.getString("alternativeUOM"));
  }

  private boolean alternativeUOMEqualsToReturnedUOM(JSONObject selectedLine) throws JSONException {
    return selectedLine.getString("alternativeUOM").equals(selectedLine.getString("returnedUOM"));
  }

  private void removeNonSelectedLines(List<String> idList, ShipmentInOut inOut) {
    if (idList.size() > 0) {
      for (String id : idList) {
        ShipmentInOutLine iol = OBDal.getInstance().get(ShipmentInOutLine.class, id);
        OBDal.getInstance().remove(iol);
        inOut.getMaterialMgmtShipmentInOutLineList().remove(iol);
      }
      OBDal.getInstance().save(inOut);
      OBDal.getInstance().flush();
    }
  }
}
