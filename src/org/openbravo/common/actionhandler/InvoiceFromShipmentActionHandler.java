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
 * All portions are Copyright (C) 2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.common.actionhandler;

import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.InvoiceFromGoodsShipmentUtil;
import org.openbravo.materialmgmt.InvoiceGeneratorFromGoodsShipment;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.service.json.JsonUtils;

/**
 * Action handler to generate Invoice from Goods Shipment
 * 
 */
public class InvoiceFromShipmentActionHandler extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();

  private static final String TEXT = "text";
  private static final String TITLE = "title";
  private static final String SEVERITY = "severity";
  private static final String MESSAGE = "message";

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    final JSONObject message = new JSONObject();
    try {
      final JSONObject request = new JSONObject(content);
      final JSONObject params = request.getJSONObject("_params");
      final String shipmentId = request.getString("M_InOut_ID");
      final String invoiceDateStr = params.getString("DateInvoiced");
      final String priceListStr = params.getString("priceList");
      boolean processInvoice = params.getBoolean("processInvoice");

      final Date invoiceDate = JsonUtils.createDateFormat().parse(invoiceDateStr);
      final PriceList priceList = OBDal.getInstance().getProxy(PriceList.class, priceListStr);

      final Invoice invoice = new InvoiceGeneratorFromGoodsShipment(shipmentId, invoiceDate,
          priceList).createInvoiceConsideringInvoiceTerms(processInvoice);
      message.put(MESSAGE, getSuccessMessage(invoice));
    } catch (Exception e) {
      try {
        message.put(MESSAGE, getErrorMessage(e));
      } catch (JSONException e1) {
        log.error(e.getMessage());
      }
    }

    return message;
  }

  protected JSONObject getSuccessMessage(final Invoice invoice) {
    final JSONObject successMessage = new JSONObject();
    try {
      successMessage.put(SEVERITY, "success");
      successMessage.put(TITLE, OBMessageUtils.messageBD("Success"));
      if (invoice != null) {
        successMessage.put(TEXT, String.format(OBMessageUtils.messageBD("NewInvoiceGenerated"),
            invoice.getDocumentNo(), InvoiceFromGoodsShipmentUtil.getInvoiceStatus(invoice)));
      } else {
        successMessage.put(TEXT, OBMessageUtils.messageBD("NoInvoiceGenerated"));
      }
    } catch (JSONException e) {
      log.error(e.getMessage());
    }
    return successMessage;
  }

  private JSONObject getErrorMessage(final Exception e) {
    final JSONObject errorMessage = new JSONObject();
    try {
      errorMessage.put(SEVERITY, "error");
      errorMessage.put(TITLE, OBMessageUtils.messageBD("Error"));
      errorMessage.put(TEXT, e.getMessage());
    } catch (JSONException ex) {
      log.error(e.getMessage());
    }
    return errorMessage;
  }

}
