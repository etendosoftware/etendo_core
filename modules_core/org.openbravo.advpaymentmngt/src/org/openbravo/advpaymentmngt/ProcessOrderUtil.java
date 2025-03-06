package org.openbravo.advpaymentmngt;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;


import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonUtility;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.CallProcess;

/**
 * Utility with methods related to the processing of orders of any type.
 */
public class ProcessOrderUtil {
  private static final Logger log4j = LogManager.getLogger();
  private final AdvPaymentMngtDao dao = new AdvPaymentMngtDao();

  @Inject
  @Any
  private Instance<ProcessOrderHook> hooks;

  /**
   * Processes an order.
   * This method uses the {@link ProcessOrderHook} type hooks to execute pre and post hooks.
   *
   * @param strC_Order_ID
   *     Order ID
   * @param strdocaction
   *     Document Action
   * @param vars
   *     {@link VariablesSecureApp} Used to obtain current language and by Payment Processes. Use {@link org.openbravo.client.kernel.RequestContext#getVariablesSecureApp()} outside of servlets.
   * @param conn
   *     {@link ConnectionProvider} Used to connect to the database. Use 'this' when in servlets.
   * @return an {@link OBError} with the message of the resulting operation. It can be a success.
   */
  public OBError process(String strC_Order_ID, String strdocaction, VariablesSecureApp vars, ConnectionProvider conn) {
    OBError myMessage = null;
    try {

      Order order = dao.getObject(Order.class, strC_Order_ID);
      order.setDocumentAction(strdocaction);
      OBDal.getInstance().save(order);
      OBDal.getInstance().flush();

      OBError msg;
      for (ProcessOrderHook hook : hooks) {
        msg = hook.preProcess(order, strdocaction);
        if (msg != null && StringUtils.equals("Error", msg.getType())) {
          return msg;
        }
      }
      // check BP currency
      if (StringUtils.equals("CO", strdocaction) && order.getBusinessPartner().getCurrency() == null) {
        String errorMSG = Utility.messageBD(conn, "InitBPCurrencyLnk", vars.getLanguage(),
            false);
        msg = new OBError();
        msg.setType("Error");
        msg.setTitle(Utility.messageBD(conn, "Error", vars.getLanguage()));
        msg.setMessage(String.format(errorMSG, order.getBusinessPartner().getId(),
            order.getBusinessPartner().getName()));
        return msg;
      }

      Process process = null;
      try {
        OBContext.setAdminMode(true);
        process = dao.getObject(Process.class, "104");
      } finally {
        OBContext.restorePreviousMode();
      }


      final ProcessInstance pinstance = CallProcess.getInstance()
          .call(process, strC_Order_ID, null);

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

      for (ProcessOrderHook hook : hooks) {
        msg = hook.postProcess(order, strdocaction);
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

  public static List<String> getDocumentActionList(String documentStatus, String documentAction, String isProcessing,
      String tableId, VariablesSecureApp vars, ConnectionProvider conn) {
    FieldProvider[] fields = ActionButtonUtility.docAction(conn, vars, documentAction, "135",
        documentStatus, isProcessing, tableId);

    List<String> actionList = new ArrayList<>();

    for (FieldProvider field : fields) {
      actionList.add(field.getField("ID"));
    }

    return actionList;
  }
}
