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
 * All portions are Copyright (C) 2014-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.common.actionhandler;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.onhandquantity.ReservedGoodMovementPickEdit;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DbUtility;

public class ReservedGoodMovementActionHandler extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode(true);
    try {
      jsonRequest = new JSONObject(content);
      log.debug("{}", jsonRequest);
      JSONArray selectedLines = jsonRequest.getJSONObject("_params")
          .getJSONObject("grid")
          .getJSONArray("_selection");
      if (selectedLines.length() == 0) {
        return jsonRequest;
      }

      InternalMovement gmHeader = null;
      Long lineno = 0L;

      int numLinesToProcess = 0;

      for (int i = 0; i < selectedLines.length(); i++) {
        JSONObject selectedLine = selectedLines.getJSONObject(i);
        log.debug("{}", selectedLine);

        String strReservationStockId = selectedLine
            .getString(ReservedGoodMovementPickEdit.PROPERTY_ID);

        ReservationStock resStock = OBDal.getInstance()
            .get(ReservationStock.class, strReservationStockId);

        final BigDecimal qty = new BigDecimal(
            selectedLine.getString(ReservedGoodMovementPickEdit.PROPERTY_MOVEMENTQUANTITY));

        final String strStorageBin = selectedLine
            .getString(ReservedGoodMovementPickEdit.PROPERTY_NEWSTORAGEBIN);

        if ("null".equals(strStorageBin)) {
          throw new OBException(OBMessageUtils.messageBD("OBUIAPP_DefineStorageBin"));
        }

        if (qty.compareTo(BigDecimal.ZERO) <= 0
            || strStorageBin.equals(resStock.getStorageBin().getId())) {
          continue;
        }

        OBDal.getInstance().save(resStock);

        // create Good Movement header
        if (gmHeader == null) {
          gmHeader = OBProvider.getInstance().get(InternalMovement.class);
          gmHeader.setOrganization(resStock.getOrganization());
          gmHeader.setClient(resStock.getClient());
          gmHeader.setMovementDate(DateUtils.truncate(new Date(), Calendar.DATE));
          gmHeader.setName(resStock.getReservation().getProduct().getName());
          OBDal.getInstance().save(gmHeader);
          OBDal.getInstance().flush();
        }

        // create lines
        InternalMovementLine gmLines = OBProvider.getInstance().get(InternalMovementLine.class);
        gmLines.setMovement(gmHeader);
        gmLines.setOrganization(gmHeader.getOrganization());
        gmLines.setClient(gmHeader.getClient());
        lineno += 10L;
        gmLines.setLineNo(lineno);
        gmLines.setProduct(resStock.getReservation().getProduct());
        gmLines.setAttributeSetValue(resStock.getAttributeSetValue());
        gmLines.setMovementQuantity(qty);
        gmLines.setUOM(resStock.getReservation().getUOM());
        gmLines.setStorageBin(resStock.getStorageBin());
        Locator newStorageBin = (Locator) OBDal.getInstance()
            .getProxy(Locator.ENTITY_NAME, strStorageBin);
        gmLines.setNewStorageBin(newStorageBin);
        gmLines.setStockReservation(resStock.getReservation());
        OBDal.getInstance().save(gmLines);

        numLinesToProcess++;
      }
      OBDal.getInstance().flush();

      if (numLinesToProcess > 0) {
        // Process good Movement
        Process process = OBDal.getInstance().get(Process.class, "122");
        ProcessInstance pinstance = CallProcess.getInstance().call(process, gmHeader.getId(), null);
        if (pinstance.getResult() == 0L) {
          throw new OBException(OBMessageUtils.messageBD("ErrorProcessingGoodMovement"));
        }
      } else {
        throw new OBException(OBMessageUtils.messageBD("NoGoodsMovementCreated"));
      }

    } catch (Exception e) {
      log.error("Error in Reserved Good Movement Action Handler", e);
      OBDal.getInstance().rollbackAndClose();
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

}
