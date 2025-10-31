package com.smf.jobs.background;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.openbravo.scheduling.KillableProcess;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

import com.smf.jobs.JobManager;
import com.smf.jobs.Runner;

import jakarta.enterprise.context.Dependent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Class used to execute a job in background.
 * To execute a job from the UI or code, use the {@link Runner#doPost(String, HttpServletRequest, HttpServletResponse)} web service
 * or the {@link Runner} and {@link JobManager} classes.
 */
@Dependent
public class BackgroundRunner extends DalBaseProcess implements KillableProcess {
    private final MutableBoolean stopped = new MutableBoolean(false);

    @Override
    protected void doExecute(ProcessBundle bundle) throws Exception {
        var parameters = bundle.getParams();
        var logger = bundle.getLogger();
        var jobId = (String) parameters.get("jobId");
        var requestId = (String) parameters.get("requestId");

        var results = JobManager.runJobSynchronously(jobId, requestId, stopped, false);

        var jobResult = JobManager.saveResults(results, jobId, requestId);
        logger.logln(jobResult.getMessage());
    }

    @Override
    public void kill(ProcessBundle processBundle) {
        stopped.setValue(true);
    }
}
