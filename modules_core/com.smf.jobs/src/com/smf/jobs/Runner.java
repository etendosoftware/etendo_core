package com.smf.jobs;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.service.web.WebService;
import org.openbravo.service.web.WebServiceUtil;
import org.quartz.SchedulerException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Utility class that runs a Job
 * It can be used to run a Job from the smartclient UI as a Process Definition, or via a webservice call.
 */
public class Runner extends BaseProcessActionHandler implements WebService {

    /**
     * Runs a job from the smartclient UI
     * @param parameters request parameters
     * @param content JSON content as a String
     * @return JSONObject converted from the {@link ActionResult} returned by the Action(s)
     */
    @Override
    protected JSONObject doExecute(Map<String, Object> parameters, String content) {
        JSONObject json;
        try {
            json = new JSONObject(content);

            var keyName = json.getString("inpKeyName");
            var selectedRecordId = json.getString(keyName);

            var requestId = SequenceIdData.getUUID();
            var manager = JobManager.INSTANCE.get();

            var result = manager.runJob(selectedRecordId, requestId);

            return getResponseBuilder()
                    .showMsgInView(ResponseActionsBuilder.MessageType.SUCCESS, result.getStatus(), result.getMessage())
                    .build();

        } catch (JSONException | ServletException | SchedulerException e) {
            throw new OBException(e.getMessage(), e);
        }
    }

    /**
     * Runs a job via a webservice call
     * @param path
     *          the HttpRequest.getPathInfo(), the part of the url after the context path
     * @param request
     *          the HTTP request. It needs a json object in its body, with the key "jobs", which will contain an array of IDs.
     *          The IDs will be the Jobs to be executed.
     * @param response
     *          the HTTP response
     */
    @Override
    public void doPost(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
        var jsonResponse = new JSONObject();
        var jsonResults = new JSONArray();

        var rawRequest = WebServiceUtil.getInstance().getRequestContent(request);
        var jsonRequest = new JSONObject(rawRequest);
        var jobs = jsonRequest.getJSONArray("jobs");
        var isAsync = jsonRequest.getBoolean("async");
        var manager = JobManager.INSTANCE;

        for (int i = 0; i < jobs.length(); i++) {
            var job = jobs.getJSONObject(i);

            if (isAsync) {
                var requestId = SequenceIdData.getUUID();
                var message = new Result();

                manager.runJob(job.getString("id"), requestId);

                message.setType(Result.Type.SUCCESS);
                message.setMessage(OBMessageUtils.getI18NMessage("JobRunning"));

                jsonResults.put(message.toJSON());
            } else {
                var results = JobManager.runJobSynchronously(job.getString("id"), true);
                jsonResults.put(results.stream().map(Result::toJSON));
            }
        }

        jsonResponse.put("results", jsonResults);

        response.setCharacterEncoding("UTF-8");
        var writer = response.getWriter();

        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Content-Type", "application/json;charset=UTF-8");

        writer.write(jsonResponse.toString());
    }

    @Override
    public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void doPut(String path, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void doGet(String path, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
