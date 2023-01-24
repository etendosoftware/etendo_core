package org.openbravo.advpaymentmngt;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.service.db.CallProcess;

/**
 * Utility with methods related to the processing of shipments of any type.
 */
public class ProcessShipmentUtil {
  private static final Logger log4j = LogManager.getLogger();
  private final AdvPaymentMngtDao dao = new AdvPaymentMngtDao();

  @Inject
  @Any
  private Instance<ProcessShipmentHook> hooks;

  /**
   * Processes an shipment.
   * This method uses the {@link ProcessShipmentHook} type hooks to execute pre and post hooks.
   *
   * @param strM_Inout_ID
   *     Shipment ID
   * @param strdocaction
   *     Document Action
   * @param vars
   *     {@link VariablesSecureApp} Used to obtain current language and by Payment Processes. Use {@link org.openbravo.client.kernel.RequestContext#getVariablesSecureApp()} outside of servlets.
   * @param conn
   *     {@link ConnectionProvider} Used to connect to the database. Use 'this' when in servlets.
   * @return an {@link OBError} with the message of the resulting operation. It can be a success.
   */
  public OBError process(String strM_Inout_ID, String strdocaction, VariablesSecureApp vars, ConnectionProvider conn) {
    OBError myMessage = null;
    try {

      ShipmentInOut shipment = dao.getObject(ShipmentInOut.class, strM_Inout_ID);
      shipment.setDocumentAction(strdocaction);
      OBDal.getInstance().save(shipment);
      OBDal.getInstance().flush();

      OBError msg;
      for (ProcessShipmentHook hook : hooks) {
        msg = hook.preProcess(shipment, strdocaction);
        if (msg != null && StringUtils.equals("Error", msg.getType())) {
          return msg;
        }
      }
      // check BP currency
      if (StringUtils.equals("CO", strdocaction) && shipment.getBusinessPartner().getCurrency() == null) {
        String errorMSG = Utility.messageBD(conn, "InitBPCurrencyLnk", vars.getLanguage(),
            false);
        msg = new OBError();
        msg.setType("Error");
        msg.setTitle(Utility.messageBD(conn, "Error", vars.getLanguage()));
        msg.setMessage(String.format(errorMSG, shipment.getBusinessPartner().getId(),
            shipment.getBusinessPartner().getName()));
        return msg;
      }

      Process process = null;
      try {
        OBContext.setAdminMode(true);
        process = dao.getObject(Process.class, "109");
      } finally {
        OBContext.restorePreviousMode();
      }


      final ProcessInstance pinstance = CallProcess.getInstance()
          .call(process, strM_Inout_ID, null);

      try {
        OBContext.setAdminMode();
        // on error close popup and rollback
        if (pinstance.getResult() == 0L) {
          OBDal.getInstance().rollbackAndClose();
          myMessage = Utility.translateError(conn, vars, vars.getLanguage(),
              pinstance.getErrorMsg().replaceFirst("@ERROR=", ""));
          log4j.debug(myMessage.getMessage());

          return myMessage;
        }
      } finally {
        OBContext.restorePreviousMode();
      }

      for (ProcessShipmentHook hook : hooks) {
        msg = hook.postProcess(shipment, strdocaction);
        if (msg != null && StringUtils.equals("Error", msg.getType())) {
          OBDal.getInstance().rollbackAndClose();
          return msg;
        }
      }

      OBDal.getInstance().commitAndClose();
      final PInstanceProcessData[] pinstanceData = PInstanceProcessData.select(conn,
          pinstance.getId());
      myMessage = Utility.getProcessInstanceMessage(conn, vars, pinstanceData);
      log4j.debug(myMessage.getMessage());

    } catch (ServletException ex) {
      myMessage = Utility.translateError(conn, vars, vars.getLanguage(), ex.getMessage());
      return myMessage;
    }

    return myMessage;
  }
}
