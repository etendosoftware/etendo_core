package com.smf.jobs;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder;
import org.openbravo.dal.service.OBDal;

import com.smf.jobs.model.JobResult;

import jakarta.enterprise.context.Dependent;

@Dependent
public class KillJobProcess extends BaseProcessActionHandler {
    private static final Logger log = LogManager.getLogger();

    @Override
    protected JSONObject doExecute(Map<String, Object> parameters, String content) {
        JSONObject json;
        var responseBuilder = getResponseBuilder();
        try {
            json = new JSONObject(content);

            var keyName = json.getString("inpKeyName");
            var selectedRecordId = json.getString(keyName);

            var jobResult = OBDal.getInstance().get(JobResult.class, selectedRecordId);

            var manager = JobManager.INSTANCE;

            var resultMessage = manager.killJob(jobResult);

            responseBuilder.showMsgInView(ResponseActionsBuilder.MessageType.SUCCESS, "Success", resultMessage);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            responseBuilder.showMsgInView(ResponseActionsBuilder.MessageType.ERROR, "Error", e.getMessage());
        }

        return responseBuilder.build();
    }
}
