package com.smf.jobs.defaults;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.ProcessInvoiceUtil;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.service.db.DalConnectionProvider;

import java.util.Map;

public class ProcessInvoicesDefaults extends BaseActionHandler {
    @Override
    protected JSONObject execute(Map<String, Object> parameters, String content) {
        final var result = new JSONObject();
        final var jsonActions = new JSONArray();

        try {
            final JSONObject jsonData = new JSONObject(content);
            final var documentStatuses = jsonData.getJSONArray("documentStatuses");
            final String isProcessing = jsonData.getString("isProcessing");
            final String tableId = jsonData.getString("tableId");
            final var vars = RequestContext.get().getVariablesSecureApp();
            final var conn = new DalConnectionProvider();

            for (int i = 0; i < documentStatuses.length(); i++) {
                var documentStatus = documentStatuses.getString(i);
                var actions = ProcessInvoiceUtil.getDocumentActionList(documentStatus, "", isProcessing, tableId, vars, conn);
                actions.forEach(jsonActions::put);
            }

            result.put("actions", jsonActions);
        } catch (Exception e) {
            throw new OBException(e);
        }

        return result;
    }
}
