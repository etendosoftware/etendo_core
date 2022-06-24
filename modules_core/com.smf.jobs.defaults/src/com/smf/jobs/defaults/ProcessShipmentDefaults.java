package com.smf.jobs.defaults;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonUtility;
import org.openbravo.service.db.DalConnectionProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProcessShipmentDefaults extends BaseActionHandler {
    public static final String DOCUMENT_ACTION_REFERENCE_ID = "135";
    public static final String AD_TABLE_ID = "319";


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
                var actions = getDocumentActionList(documentStatus, isProcessing, vars, conn);
                actions.forEach(jsonActions::put);
            }
            if(jsonActions.length() == 1) {
                jsonActions.put("");
            }

            result.put("actions", jsonActions);
        } catch (Exception e) {
            throw new OBException(e);
        }

        return result;
    }

    public static List<String> getDocumentActionList(String documentStatus, String isProcessing, VariablesSecureApp vars, ConnectionProvider conn) {
        // (Document Action is never used in ActionButtonUtility)
        FieldProvider[] fields = ActionButtonUtility.docAction(conn, vars, "", DOCUMENT_ACTION_REFERENCE_ID,
                documentStatus, isProcessing, AD_TABLE_ID);

        List<String> actionList = new ArrayList<>();

        for (FieldProvider field : fields) {
            actionList.add(field.getField("ID"));
        }

        return actionList;
    }
}
