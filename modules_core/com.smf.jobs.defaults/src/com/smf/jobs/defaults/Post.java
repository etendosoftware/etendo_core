package com.smf.jobs.defaults;

import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonUtility;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.assetmgmt.Amortization;
import org.openbravo.model.financialmgmt.cashmgmt.BankStatement;
import org.openbravo.model.financialmgmt.cashmgmt.CashJournal;
import org.openbravo.model.financialmgmt.gl.GLJournal;
import org.openbravo.model.financialmgmt.payment.DPManagement;
import org.openbravo.model.financialmgmt.payment.Settlement;
import org.openbravo.model.materialmgmt.transaction.InternalConsumption;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.ProductionTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.procurement.ReceiptInvoiceMatch;
import org.openbravo.service.db.DalConnectionProvider;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class Post extends Action {
  Logger log = LogManager.getLogger();

  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {

    ActionResult result = new ActionResult();
    try {
      List<BaseOBObject> registers = getInputContents(getInputClass());
      var vars = RequestContext.get().getVariablesSecureApp();
      result.setType(Result.Type.SUCCESS);
      int errors = 0;
      int success = 0;

      for (BaseOBObject register : registers) {
        OBError messageResult;
        String posted = (String) register.get("posted");
        Organization org = (Organization) register.get("organization");
        Client client = (Client) register.get("client");
        String tableId = register.getEntity().getTableId();
        Date date = (Date) register.get(getDateProperty(tableId, vars));
        DateFormat dateFormat = new SimpleDateFormat(vars.getJavaDateFormat());
        String strDate = dateFormat.format(date);
        if (!"Y".equals(posted)) {
          messageResult = ActionButtonUtility.processButton(vars, register.getId().toString(), tableId, org.getId(),
              new DalConnectionProvider());
        } else {
          messageResult = ActionButtonUtility.resetAccounting(vars, client.getId(), org.getId(), tableId,
              register.getId().toString(), strDate, new DalConnectionProvider());
        }
        if ("error".equalsIgnoreCase(messageResult.getType())) {
          result.setType(Result.Type.ERROR);
          errors++;
        }
        if ("success".equalsIgnoreCase(messageResult.getType())) {
          result.setType(Result.Type.SUCCESS);
          success++;
        }
        result.setMessage(messageResult.getTitle().concat(": ").concat(messageResult.getMessage()));
      }
      massiveMessageHandler(result, registers, errors, success);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      result.setType(Result.Type.ERROR);
      result.setMessage(e.getMessage());
    }
    return result;
  }

  private void massiveMessageHandler(ActionResult result, List<BaseOBObject> registers, int errors, int success) {
    if (registers.size() > 1) {
      if (success == registers.size()) {
        result.setType(Result.Type.SUCCESS);
      } else if (errors == registers.size()) {
        result.setType(Result.Type.ERROR);
      } else {
        result.setType(Result.Type.WARNING);
      }
      result.setMessage(String.format(OBMessageUtils.messageBD("DJOBS_PostUnpostMessage"), success, errors));
      result.setOutput(getInput());
    }
  }

  @Override
  protected Class<BaseOBObject> getInputClass() {
    return BaseOBObject.class;
  }

  private String getDateProperty(String adTableId, VariablesSecureApp vars) {
    Map<String, String> mapOfTablesSupported = Map.ofEntries(
        Map.entry("318", Invoice.PROPERTY_ACCOUNTINGDATE),
        Map.entry("800060", Amortization.PROPERTY_ACCOUNTINGDATE),
        Map.entry("800176", DPManagement.PROPERTY_ACCOUNTINGDATE),
        Map.entry("407", CashJournal.PROPERTY_ACCOUNTINGDATE),
        Map.entry("392", BankStatement.PROPERTY_TRANSACTIONDATE),
        Map.entry("259", Order.PROPERTY_ACCOUNTINGDATE),
        Map.entry("800019", Settlement.PROPERTY_ACCOUNTINGDATE),
        Map.entry("319", ShipmentInOut.PROPERTY_ACCOUNTINGDATE),
        Map.entry("321", InventoryCount.PROPERTY_MOVEMENTDATE),
        Map.entry("323", InternalMovement.PROPERTY_MOVEMENTDATE),
        Map.entry("325", ProductionTransaction.PROPERTY_MOVEMENTDATE),
        Map.entry("224", GLJournal.PROPERTY_ACCOUNTINGDATE),
        Map.entry("472", ReceiptInvoiceMatch.PROPERTY_TRANSACTIONDATE),
        Map.entry("800168", InternalConsumption.PROPERTY_MOVEMENTDATE)
    );

    String property = mapOfTablesSupported.get(adTableId);
    if (property == null) {
      throw new OBException(OBMessageUtils.messageBD(new DalConnectionProvider(), "TableNotFound", vars.getLanguage()));
    }
    return property;
  }

}
