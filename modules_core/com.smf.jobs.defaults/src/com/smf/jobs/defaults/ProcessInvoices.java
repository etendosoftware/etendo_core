package com.smf.jobs.defaults;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Data;
import com.smf.jobs.Result;
import com.smf.jobs.Action;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.ProcessInvoiceUtil;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonUtils;

import javax.inject.Inject;
import java.text.ParseException;

/**
 * Action for processing invoices.
 * Allows for the same actions available in the UI as part of a Job.
 */
public class ProcessInvoices extends Action {
    Logger log = LogManager.getLogger();
    private static final String VOIDDATE = "VoidDate";
    private static final String VOIDACCOUNTINGDATE = "VoidAccountingDate";
    @Inject
    private WeldUtils weldUtils;

    @Override
    protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
        var result = new ActionResult();

        try {
            var input = getInputContents(getInputClass());
            var documentAction = parameters.getString("DocAction");
            var voidDate = parameters.isNull(VOIDDATE) ? null : parameters.getString(VOIDDATE);
            var voidAcctDate = parameters.isNull(VOIDACCOUNTINGDATE) ? null : parameters.getString(VOIDACCOUNTINGDATE);
            var processMessages = new StringBuilder();
            int errors = 0;

            result.setType(Result.Type.SUCCESS);

            log.debug("Process Invoice Action Parameters:");
            log.debug(parameters.toString());

            for (Invoice invoice : input) {
                var message = processInvoice(invoice, documentAction, voidDate, voidAcctDate);
                if (message.getType().equals("Error")) {
                    errors++;
                }
                if (message.getMessage().isBlank()) {
                    processMessages.append(invoice.getDocumentNo()).append(": ").append(message.getTitle()).append("\n");
                } else {
                    processMessages.append(invoice.getDocumentNo()).append(": ").append(message.getMessage()).append("\n");
                }
            }

            if (errors == input.size()) {
                result.setType(Result.Type.ERROR);
            } else if (errors > 0) {
                result.setType(Result.Type.WARNING);
            }

            if (input.size() > 1) {
                // Show the message in a pop up when more than one invoice was selected, for better readability.
                var jsonMessage = new JSONObject();
                jsonMessage.put("message", processMessages.toString().replaceAll("\n","<br>"));
                result.setResponseActionsBuilder(getResponseBuilder().addCustomResponseAction("smartclientSay", jsonMessage));
            }

            result.setMessage(processMessages.toString());
            result.setOutput(getInput());


        } catch (JSONException | ParseException e) {
            log.error(e.getMessage(), e);
            result.setType(Result.Type.ERROR);
            result.setMessage(e.getMessage());
        }

        return result;
    }

    private OBError processInvoice(Invoice invoice, String docAction, String _strVoidDate, String _strVoidAcctDate) throws ParseException {

        var processor = weldUtils.getInstance(ProcessInvoiceUtil.class);
        var strVoidDate = "";
        var strVoidAcctDate = "";

        if (_strVoidDate != null && _strVoidAcctDate != null) {
            // Convert from the JSON date format to the OBProperties date format
            var voidDate = JsonUtils.createDateFormat().parse(_strVoidDate);
            var voidAcctDate = JsonUtils.createDateFormat().parse(_strVoidAcctDate);

            strVoidDate = OBDateUtils.formatDate(voidDate);
            strVoidAcctDate = OBDateUtils.formatDate(voidAcctDate);
        }

        return processor.process(
                invoice.getId(),
                docAction,
                strVoidDate,
                strVoidAcctDate,
                RequestContext.get().getVariablesSecureApp(),
                new DalConnectionProvider(false)
        );
    }

    @Override
    protected Data preRun(JSONObject jsonContent) {
        log.debug(jsonContent);
        try {
            var parameters = jsonContent.optJSONObject("_params");
            var input = getInputContents(getInputClass());
            var voidDate = parameters.isNull(VOIDDATE) ? null : parameters.getString(VOIDDATE);
            var voidAcctDate = parameters.isNull(VOIDACCOUNTINGDATE) ? null : parameters.getString(VOIDACCOUNTINGDATE);
            log.debug("Process Invoice preRun Parameters:");
            log.debug(parameters.toString());

            var doFlush = false;
            for (Invoice invoice : input) {
                // The following condition checks if the record is locked, this makes sense for Oracle databases
                if (!invoice.isProcessNow()) {
                    continue;
                }
                // In case of a locked record, the docAction will be forced to XL, this will unlock the record and proceed to complete
                var message = processInvoice(invoice, "XL", voidDate, voidAcctDate);
                if (!StringUtils.equals("Error", message.getType())){
                    invoice.setAPRMProcessinvoice("--");
                    OBDal.getInstance().save(invoice);
                    doFlush = true;
                }
            }
            if (doFlush) {
                OBDal.getInstance().flush();
            }
        } catch (Exception e){
            throw new OBException(e);
        }
        return getInput();
    }

    @Override
    protected Class<Invoice> getInputClass() {
        return Invoice.class;
    }
}
