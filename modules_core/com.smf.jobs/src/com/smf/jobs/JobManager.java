package com.smf.jobs;


import com.smf.jobs.model.Job;
import com.smf.jobs.model.JobLine;
import com.smf.jobs.model.JobResult;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.common.actionhandler.KillProcess;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.ProcessRequest;
import org.openbravo.model.ad.ui.ProcessRun;
import org.openbravo.scheduling.OBScheduler;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;
import org.quartz.SchedulerException;

import javax.servlet.ServletException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Singleton enum to run Jobs.
 * It allows to run jobs both synchronously and asynchronously, and has utility methods regarding job filters, actions and results.
 */
public enum JobManager {
    INSTANCE;

    public static final String BG_JOB_PROCESS_ID = "4BFAA697355C46DFB0E62325C1D9AAA8";
    private static final Logger log = LogManager.getLogger();
    private final Map<String, String> jobs;

    JobManager() {
        this.jobs = new HashMap<>();
    }

    public JobManager get() {
        return INSTANCE;
    }

    /**
     * Runs a job. The job will be executed asynchronously via the {@link OBScheduler} class (Quartz).
     * The job's Filter and Actions will be obtained. For each result page of the Filter, all the actions will be executed.
     * @param jobId ID of the Job to run
     * @param requestId ID of the Process Request that will run the job. This will be stored in the Job Result
     * @return A Job Result indicating that the job was executed and is running. This object will be updated when the job finishes.
     */
    public JobResult runJob(String jobId, String requestId) throws ServletException, SchedulerException {

        if (jobId == null || requestId == null || jobId.isBlank() || requestId.isBlank()) {
            throw new IllegalArgumentException("Job ID or Request ID missing");
        }

        VariablesSecureApp vars;
        try {
            vars = RequestContext.get().getVariablesSecureApp();
        } catch (Exception e) {
            vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
                    OBContext.getOBContext().getCurrentClient().getId(),
                    OBContext.getOBContext().getCurrentOrganization().getId(),
                    OBContext.getOBContext().getRole().getId(),
                    OBContext.getOBContext().getLanguage().getLanguage());
        }

        var jobResult = OBProvider.getInstance().get(JobResult.class);
        jobResult.setNewOBObject(true);
        jobResult.setJobsJob(OBDal.getInstance().get(Job.class, jobId));
        jobResult.setStatus(Result.Type.RUNNING.toString());
        jobResult.setMessage(OBMessageUtils.getI18NMessage("JobRunning"));

        OBDal.getInstance().save(jobResult);

        var bundle = new ProcessBundle(BG_JOB_PROCESS_ID, vars)
                .init(new DalConnectionProvider(false));

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("jobId", jobId);
        parameters.put("requestId", requestId);
        parameters.put("resultId", jobResult.getId());
        bundle.setParams(parameters);

        OBScheduler.getInstance().scheduleImmediately(bundle, requestId);

        // OBScheduler created the process request with this ID
        jobResult.setProcessRequest(OBDal.getInstance().get(ProcessRequest.class, requestId));

        OBDal.getInstance().save(jobResult);
        OBDal.getInstance().flush();

        jobs.put(jobId, requestId);

        return jobResult;
    }

    /**
     * Runs a job.
     * The job's Filter and Actions will be obtained. For each result page of the Filter, all the actions will be executed.
     * @param jobId ID of the Job to run
     * @param saveResultsInDB whether to create a {@link JobResult} record in database
     * @return a list of {@link ActionResult}. One for each action ran.
     */
    public static List<ActionResult> runJobSynchronously(String jobId, boolean saveResultsInDB) {
        return runJobSynchronously(jobId, null, new MutableBoolean(false), saveResultsInDB);
    }

    /**
     * Runs a job.
     * The job's Filter and Actions will be obtained. For each result page of the Filter, all the actions will be executed.
     * If the job is not being run with a Process Request, or there is no need for another class to control if the job has to stop,
     * then the correct method to use should be {@link #runJobSynchronously(String, boolean)}
     * @param jobId ID of the Job to run
     * @param requestId (optional) ID of the Process Request that is running this Job
     * @param stopped a {@link MutableBoolean} that controls if the job must stop or not.
     * @param saveResultsInDB whether to create a {@link JobResult} record in database
     * @return a list of {@link ActionResult}. One for each action ran.
     */
    public static List<ActionResult> runJobSynchronously(String jobId, String requestId, MutableBoolean stopped, boolean saveResultsInDB) {
        var results = new ArrayList<ActionResult>();
        Job job = null;

        try {
            OBContext.setAdminMode();

            job = getJob(jobId);

            var actions =  getActions(job);

            var filter = getFilter(job);

            while (filter.hasNext() && stopped.isFalse()) {
                var data = filter.getResults();
                Optional<Data> previousResult = Optional.empty();

                for (var action : actions) {
                    ActionResult result;

                    try {
                        if (previousResult.isEmpty()) {
                            result = action.run(data, stopped);
                        } else {
                            result = action.run(previousResult.orElseThrow(), stopped);
                        }
                        previousResult = result.getOutput();
                    } catch (Exception e) {
                        if (job.isStopsOnError()) {
                            throw e;
                        } else {
                            result = new ActionResult();
                            result.setType(Result.Type.ERROR);
                            result.setMessage(e.getMessage());
                            log.error(e.getMessage(), e);
                        }
                    }
                    results.add(result);
                }

                // Clear session for each execution
                OBDal.getInstance().getSession().clear();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            var error = new ActionResult();
            error.setType(Result.Type.ERROR);
            error.setMessage(e.getMessage());
            results.add(error);
        } finally {
            if (saveResultsInDB) {
                saveResults(results, job, requestId);
            }
            OBContext.restorePreviousMode();
        }

        return results;
    }

    /**
     * Sends a kill signal to a running job.
     * The job will attempt to stop when advancing to the next input page,
     * but stopping in the middle of an action will depend if that action has code to deal with a signal change.
     * @param jobId the id of the Job to execute
     * @return a String message indicating that the process was attempted to be killed.
     * @throws Exception when the process to be killed cannot be found.
     */
    public String killJob(String jobId) throws Exception {
        var requestId = jobs.get(jobId);
        var executionId = getExecutionId(requestId);

        var result = getResult(jobId, requestId);

        if (result == null) {
            return OBMessageUtils.getI18NMessage("JobNotRunning");
        }

        result.setStatus(Result.Type.ERROR.toString());
        result.setMessage(OBMessageUtils.getI18NMessage("JobKilled"));

        OBDal.getInstance().save(result);

        jobs.remove(jobId);

        return KillProcess.killProcess(executionId);
    }

    /**
     * Sends a kill signal to a running job.
     * The job will attempt to stop when advancing to the next input page,
     * but stopping in the middle of an action will depend if that action has code to deal with a signal change.
     * @param result the (pre) result of the Job that is running (status = RUNNING).
     *               This result must have a corresponding Process Request associated to it.
     * @return a String message indicating that the process was attempted to be killed.
     * @throws Exception when the process to be killed cannot be found.
     * @throws NullPointerException when the ProcessRequest of the Result is null
     */
    public String killJob(JobResult result) throws Exception {
        var executionId = getExecutionId(result.getProcessRequest().getId());

        result.setStatus(Result.Type.ERROR.toString());
        result.setMessage(OBMessageUtils.getI18NMessage("JobKilled"));

        OBDal.getInstance().save(result);

        jobs.remove(result.getJobsJob().getId());

        return KillProcess.killProcess(executionId);
    }

    /**
     * Saves in database the results of actions for a Job
     * @param results the list of {@link ActionResult}
     * @param jobId the id of the Job that these results belong to.
     * @return the job result created
     */
    public static JobResult saveResults(List<ActionResult> results, String jobId) {
        return saveResults(results, jobId, null);
    }

    /**
     * Saves in database the results of actions for a Job
     * @param results the list of {@link ActionResult}
     * @param jobId the id of the Job that these results belong to.
     * @param requestId (when resultId is not null) the id of the ProcessRequest which ran this job
     * @return the job result created
     */
    public static JobResult saveResults(List<ActionResult> results, String jobId, String requestId) {
        return saveResults(results, getJob(jobId), JobManager.INSTANCE.getResult(jobId, requestId));
    }

    /**
     * Saves in database the results of actions for a Job
     * @param results the list of {@link ActionResult}
     * @param job the {@link Job} that these results belong to.
     * @param requestId (when resultId is not null) the id of the ProcessRequest which ran this job
     * @return the job result created
     */
    public static JobResult saveResults(List<ActionResult> results, Job job, String requestId) {
        return saveResults(results, job, JobManager.INSTANCE.getResult( job.getId(), requestId));
    }

    /**
     * Saves in database the results of actions for a Job
     * @param results the list of {@link ActionResult}
     * @param job the {@link Job} that these results belong to.
     * @param jobResult the {@link JobResult} in case one already exists for this job instance
     * @return the job result created
     */
    public static JobResult saveResults(List<ActionResult> results, Job job, JobResult jobResult) {
        var aggregatedMessages = new StringBuilder();
        var errorOccurred = false;

        for (var result : results) {
            if (result.getType().equals(Result.Type.ERROR)) {
                errorOccurred = true;
            }
            aggregatedMessages.append(result.getMessage()).append("\n");
        }

        var status = errorOccurred ? Result.Type.ERROR : Result.Type.SUCCESS;

        if (jobResult == null) {
            jobResult = OBProvider.getInstance().get(JobResult.class);
            jobResult.setNewOBObject(true);
            jobResult.setJobsJob(job);
        }
        jobResult.setStatus(status.toString());
        jobResult.setMessage(aggregatedMessages.toString());

        OBDal.getInstance().save(jobResult);

        return jobResult;
    }

    /**
     * Gets the execution ID of a certain Process Request.
     * Checks the ProcessRun (AD_PROCESS_RUN) entity. Used to kill identify processes to kill
     * @param processRequestId the ID of the Process Request
     * @return the execution ID as a {@link String}
     */
    public String getExecutionId(String processRequestId) {
        var executionCriteria = OBDal.getInstance().createCriteria(ProcessRun.class);
        executionCriteria.add(Restrictions.eq(ProcessRun.PROPERTY_PROCESSREQUEST+".id", processRequestId));
        executionCriteria.setMaxResults(1);

        var execution = (ProcessRun) executionCriteria.uniqueResult();

        if (execution != null) {
            return execution.getId();
        } else {
            return null;
        }
    }

    /**
     * Obtains the filter from a Job
     * @param job Job
     * @return a {@link Filter} object that is used to get the results
     */
    public static Filter getFilter(Job job) {
        var linesCriteria = OBDal.getInstance().createCriteria(JobLine.class);
        linesCriteria.add(Restrictions.eq(JobLine.PROPERTY_JOBSJOB, job));
        linesCriteria.add(Restrictions.eq(JobLine.PROPERTY_ISAFILTER, true));
        linesCriteria.setMaxResults(1);

        var filterLine = (JobLine) Optional.ofNullable(linesCriteria.uniqueResult()).orElseThrow();

        String rsql = filterLine.getFilterTemplate().getDefinition();

        if (filterLine.getFilterDefinition() != null && !filterLine.getFilterDefinition().isBlank()) {
            if (rsql != null && !rsql.isBlank()) {
                rsql += " and ";
            }
            rsql += filterLine.getFilterDefinition();
        }

        if (rsql == null || rsql.isBlank()) {
            rsql = "1==1"; // Query can't be empty
        }

        var entityName = filterLine.getFilterTemplate().getTable().getName();

        return new Filter(entityName, rsql);
    }

    /**
     * Obtains a Job based on its id. It throws a runtime exception when the job is not found.
     * @param jobId the ID of the Job
     * @return a {@link Job} instance
     * */
    public static Job getJob(String jobId) {
        return Optional.ofNullable(OBDal.getInstance().get(Job.class, jobId)).orElseThrow();
    }

    /**
     * Obtains the list of actions of a Job, in order.
     * The actions are processed and instantiated as a {@link Action} object, so runtime exception can be thrown is something is incorrect.
     * @param job the Job which the Actions belong to
     * @return a list of {@link Action}
     */
    public static List<Action> getActions(Job job) {
        var linesCriteria = OBDal.getInstance().createCriteria(JobLine.class);
        linesCriteria.add(Restrictions.eq(JobLine.PROPERTY_JOBSJOB, job));
        linesCriteria.add(Restrictions.eq(JobLine.PROPERTY_ISAFILTER, false));
        linesCriteria.add(Restrictions.isNotNull(JobLine.PROPERTY_ACTION));
        linesCriteria.addOrderBy(JobLine.PROPERTY_LINENO, true);

        return linesCriteria
                .list()
                .stream()
                .map(line -> processActions(line.getAction().getJavaClassName(), line.getParameters()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Returns an action corresponding to the class name.
     * If parameters are passed, they are stored in the action as a {@link JSONObject}
     * @param className Action class name
     * @param parameters (Optional) parameters as a JSON String
     * @return an {@link Action} instance
     */
    private static Action processActions(String className, String parameters) {
        try {
            @SuppressWarnings("unchecked") final Class<Action> actionClass = (Class<Action>) OBClassLoader
                    .getInstance()
                    .loadClass(className);
            var action = WeldUtils.getInstanceFromStaticBeanManager(actionClass);

            if (parameters != null) {
                action.setParameters(new JSONObject(parameters));
            }

            return action;
        } catch (Exception e) {
            log.error("Could not process action: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Obtains a Job Result
     * @param jobId the ID of the Job that was executed
     * @param requestId the ID of the Process Request that ran the job.
     *                  This can be null, for example, when the process was executed synchronously.
     *                  Although Jobs that ran synchronously can have requests ID to identify them if necessary.
     * @return a {@link JobResult} object with the Result of the desired job.
     */
    public JobResult getResult(String jobId, String requestId) {
        var resultCriteria = OBDal.getInstance().createCriteria(JobResult.class);
        resultCriteria.add(Restrictions.eq(JobResult.PROPERTY_JOBSJOB + ".id", jobId));
        if (requestId != null && !requestId.isBlank()) {
            resultCriteria.add(Restrictions.eq(JobResult.PROPERTY_PROCESSREQUEST + ".id", requestId));
        } else {
            resultCriteria.add(Restrictions.isNull(JobResult.PROPERTY_PROCESSREQUEST));
        }
        resultCriteria.setMaxResults(1);

        return (JobResult) resultCriteria.uniqueResult();
    }
}