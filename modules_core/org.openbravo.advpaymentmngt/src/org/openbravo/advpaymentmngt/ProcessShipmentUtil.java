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
  private static final String ERROR = "Error";

  @Inject
  @Any
  private Instance<ProcessShipmentHook> hooks;

  /**
   * Processes a shipment.
   * This method uses the {@link ProcessShipmentHook} type hooks to execute pre and pos hooks.
   *
   * @param strMInoutID
   *     Shipment ID
   * @param strDocAction
   *     Document Action
   * @param vars
   *     {@link VariablesSecureApp} Used to obtain current language and by Payment Processes. Use {@link org.openbravo.client.kernel.RequestContext#getVariablesSecureApp()} outside of servlets.
   * @param conn
   *     {@link ConnectionProvider} Used to connect to the database. Use 'this' when in servlets.
   * @return an {@link OBError} with the message of the resulting operation. It can be a success.
   */
  public OBError process(String strMInoutID, String strDocAction, VariablesSecureApp vars, ConnectionProvider conn) {
    OBError myMessage;
    try {

      ShipmentInOut shipment = dao.getObject(ShipmentInOut.class, strMInoutID);
      shipment.setDocumentAction(strDocAction);
      OBDal.getInstance().save(shipment);
      OBDal.getInstance().flush();

      OBError msg;
      for (ProcessShipmentHook hook : hooks) {
        msg = hook.preProcess(shipment, strDocAction);
        if (msg != null && StringUtils.equals(ERROR, msg.getType())) {
          return msg;
        }
      }
      // check BP currency
      if (StringUtils.equals("CO", strDocAction) && shipment.getBusinessPartner().getCurrency() == null) {
        String errorMSG = Utility.messageBD(conn, "InitBPCurrencyLnk", vars.getLanguage(),
            false);
        msg = new OBError();
        msg.setType(ERROR);
        msg.setTitle(Utility.messageBD(conn, ERROR, vars.getLanguage()));
        msg.setMessage(String.format(errorMSG, shipment.getBusinessPartner().getId(),
            shipment.getBusinessPartner().getName()));
        return msg;
      }

      Process process;
      try {
        OBContext.setAdminMode(true);
        process = dao.getObject(Process.class, "109");
      } finally {
        OBContext.restorePreviousMode();
      }


      final ProcessInstance pInstance = CallProcess.getInstance()
          .call(process, strMInoutID, null);

      try {
        OBContext.setAdminMode();
        // on error close popup and rollback
        if (pInstance.getResult() == 0L) {
          OBDal.getInstance().rollbackAndClose();
          myMessage = Utility.translateError(conn, vars, vars.getLanguage(),
              pInstance.getErrorMsg().replaceFirst("@ERROR=", ""));
          log4j.debug(myMessage.getMessage());

          return myMessage;
        }
      } finally {
        OBContext.restorePreviousMode();
      }

      for (ProcessShipmentHook hook : hooks) {
        msg = hook.postProcess(shipment, strDocAction);
        if (msg != null && StringUtils.equals(ERROR, msg.getType())) {
          OBDal.getInstance().rollbackAndClose();
          return msg;
        }
      }

      OBDal.getInstance().commitAndClose();
      final PInstanceProcessData[] pInstanceData = PInstanceProcessData.select(conn,
          pInstance.getId());
      myMessage = Utility.getProcessInstanceMessage(conn, vars, pInstanceData);
      log4j.debug(myMessage.getMessage());

    } catch (ServletException ex) {
      myMessage = Utility.translateError(conn, vars, vars.getLanguage(), ex.getMessage());
      return myMessage;
    }

    return myMessage;
  }
}
