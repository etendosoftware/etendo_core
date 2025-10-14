package com.smf.jobs;

import com.smf.jobs.model.Job;
import com.smf.jobs.model.JobResult;
import com.smf.securewebservices.service.BaseWebService;
import com.smf.securewebservices.utils.WSResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.ui.ProcessRequest;
import org.openbravo.scheduling.OBScheduler;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessBundle.Channel;
import org.openbravo.scheduling.ProcessContext;
import org.openbravo.service.db.DalConnectionProvider;
import org.quartz.SchedulerException;

import jakarta.servlet.ServletException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.openbravo.scheduling.Process.SCHEDULED;

/**
 * Utility class to schedule a job execution in background.
 * see {@link Runner}
 */
public class Scheduler extends BaseWebService {

    private static final Logger log = LogManager.getLogger();

    /**
     * Schedules a job in background.
     * @param jobId the ID of the job to run
     * @param startDate the time when the job will run
     * @param timing see Process Timing Option Reference List
     * @param frequency the frequency in case the execution repeats. See Process Frequency Reference List for values.
     * @param cronExpression schedule using a cron expression
     * @param finishes whether the scheduled repetitions finish
     * @param finishDate when the scheduled repetitions finish
     * @param finishTime when scheduled repetitions finish
     * @param intervalInSeconds Every n seconds
     * @param intervalInMinutes Every n minutes
     * @param hourlyInterval Every n hours
     * @param repetitions how many repetitions when repeating each second/minutes/hours
     * @param dailyFrequency frequency when executing daily
     * @param dailyInterval Every n days (only when dailyFrequency is Every n days)
     * @param monday run on this day when frequency is weekly
     * @param tuesday run on this day when frequency is weekly
     * @param wednesday run on this day when frequency is weekly
     * @param thursday run on this day when frequency is weekly
     * @param friday run on this day when frequency is weekly
     * @param saturday run on this day when frequency is weekly
     * @param sunday run on this day when frequency is weekly
     * @param monthlyOption frequency when executing monthly
     * @param dayOfTheWeek which day of the week then frequency is monthly and monthlyOption is the first, second, third or fourth
     * @param dayInMonth which day of the month when monthlyOption is Specific Date
     */
    public static void schedule(String jobId, Date startDate, String timing, String frequency, String cronExpression,
                                boolean finishes, Date finishDate, Timestamp finishTime, Long intervalInSeconds, Long intervalInMinutes,
                                Long hourlyInterval, Long repetitions, String dailyFrequency, Long dailyInterval, boolean monday,
                                boolean tuesday, boolean wednesday, boolean thursday, boolean friday, boolean saturday, boolean sunday,
                                String monthlyOption, String dayOfTheWeek, Long dayInMonth) throws ServletException, SchedulerException {
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

        var request = createProcessRequest(vars,
                startDate,
                timing,
                frequency,
                cronExpression,
                finishes,
                finishDate,
                finishTime,
                intervalInSeconds,
                intervalInMinutes,
                hourlyInterval,
                repetitions,
                dailyFrequency,
                dailyInterval,
                monday,
                tuesday,
                wednesday,
                thursday,
                friday,
                saturday,
                sunday,
                monthlyOption,
                dayOfTheWeek,
                dayInMonth);

        var jobResult = OBProvider.getInstance().get(JobResult.class);
        jobResult.setNewOBObject(true);
        jobResult.setJobsJob(OBDal.getInstance().get(Job.class, jobId));
        jobResult.setStatus(Result.Type.PENDING.toString());
        jobResult.setMessage(OBMessageUtils.getI18NMessage("JobRunning"));
        jobResult.setProcessRequest(request);

        OBDal.getInstance().save(jobResult);

        var params = new HashMap<String, Object>();
        params.put("jobId", jobId);
        params.put("requestId", request.getId());
        params.put("resultId", jobResult.getId());

        final ProcessBundle bundle = new ProcessBundle(JobManager.BG_JOB_PROCESS_ID, request.getId(), vars, Channel.SCHEDULED,
                request.getClient().getId(), request.getOrganization().getId(), request.isSecurityBasedOnRole());
        bundle.init(new DalConnectionProvider());
        bundle.setParams(params);

        OBScheduler.getInstance().schedule(request.getId(), bundle);
    }


    /**
     * Creates a process request.
     * @param vars will be used to save the context using {@link ProcessContext}
     * @param startDate the time when the job will run
     * @param timing see Process Timing Option Reference List
     * @param frequency the frequency in case the execution repeats. See Process Frequency Reference List for values.
     * @param cronExpression schedule using a cron expression
     * @param finishes whether the scheduled repetitions finish
     * @param finishDate when the scheduled repetitions finish
     * @param finishTime when scheduled repetitions finish
     * @param intervalInSeconds Every n seconds
     * @param intervalInMinutes Every n minutes
     * @param hourlyInterval Every n hours
     * @param repetitions how many repetitions when repeating each second/minutes/hours
     * @param dailyFrequency frequency when executing daily
     * @param dailyInterval Every n days (only when dailyFrequency is Every n days)
     * @param monday run on this day when frequency is weekly
     * @param tuesday run on this day when frequency is weekly
     * @param wednesday run on this day when frequency is weekly
     * @param thursday run on this day when frequency is weekly
     * @param friday run on this day when frequency is weekly
     * @param saturday run on this day when frequency is weekly
     * @param sunday run on this day when frequency is weekly
     * @param monthlyOption frequency when executing monthly
     * @param dayOfTheWeek which day of the week then frequency is monthly and monthlyOption is the first, second, third or fourth
     * @param dayInMonth which day of the month when monthlyOption is Specific Date
     * @return the created {@link ProcessRequest} object
     */
    public static ProcessRequest createProcessRequest(VariablesSecureApp vars, Date startDate, String timing, String frequency, String cronExpression,
                                                      boolean finishes, Date finishDate, Timestamp finishTime, Long intervalInSeconds, Long intervalInMinutes,
                                                      Long hourlyInterval, Long repetitions, String dailyFrequency, Long dailyInterval, boolean monday,
                                                      boolean tuesday, boolean wednesday, boolean thursday, boolean friday, boolean saturday, boolean sunday,
                                                      String monthlyOption, String dayOfTheWeek, Long dayInMonth) {
        var request = OBProvider.getInstance().get(ProcessRequest.class);
        request.setNewOBObject(true);
        request.setProcess(OBDal.getInstance().get(Process.class, JobManager.BG_JOB_PROCESS_ID));
        request.setUserContact(OBContext.getOBContext().getUser());
        request.setStatus(SCHEDULED);
        request.setChannel(Channel.SCHEDULED.toString());
        request.setSecurityBasedOnRole(true);
        request.setTiming(timing);

        // Timing: Run Later or Schedule
        request.setStartDate(startDate);
        request.setStartTime(new Timestamp(startDate.getTime()));

        // Timing: Schedule
        request.setFinishes(finishes);
        request.setFinishDate(finishDate);
        request.setFinishTime(finishTime);
        request.setFrequency(frequency);

        // Frequency: 01 - Every n Seconds
        request.setIntervalInSeconds(intervalInSeconds);

        // Frequency: 02 - Every n Minutes
        request.setIntervalInMinutes(intervalInMinutes);

        // Frequency: 03 - Hourly
        request.setHourlyInterval(hourlyInterval);

        // Frequency: 01,02,03
        request.setRepetitions(repetitions);

        // Frequency: 04 - Daily
        request.setDailyOption(dailyFrequency);
        // Daily Option: Every n days
        request.setDailyInterval(dailyInterval);

        // Frequency: 05 - Weekly
        request.setMonday(monday);
        request.setTuesday(tuesday);
        request.setWednesday(wednesday);
        request.setThursday(thursday);
        request.setFriday(friday);
        request.setSaturday(saturday);
        request.setSunday(sunday);

        // Frequency: 06 - Monthly
        request.setMonthlyOption(monthlyOption);
        // Monthly Option: 01 - First, 02 - Second, 03 - Third, 04 - Fourth
        request.setDayOfTheWeek(dayOfTheWeek);
        // Monthly Option: 06 - Specific Date
        request.setDayInMonth(dayInMonth);

        // Frequency: 07 - Cron expression
        request.setCronExpression(cronExpression);

        var processContext = new ProcessContext(vars);
        request.setOpenbravoContext(processContext.toString());

        OBDal.getInstance().save(request);
        OBDal.getInstance().flush();

        return request;

    }

    @Override
    public WSResult post(String path, Map<String, String> parameters, JSONObject body) throws Exception {
        var result = new WSResult();
        var messages = new JSONArray();
        var jobs = body.getJSONArray("jobs");

        for (int i = 0; i < jobs.length(); i++) {
            var job = jobs.getJSONObject(i);
            var jsonStartDate = job.getString("startDate");
            var jsonFinishDate = job.getString("finishDate");
            var jsonFinishTime = job.getString("finishTime");

            Date startDate = OBDateUtils.getDate(jsonStartDate);
            Date finishDate = OBDateUtils.getDate(jsonFinishDate);
            Date finishTime = OBDateUtils.getDateTime(jsonFinishTime);

            var message = new JSONObject();
            message.put("jobId", job.getString("id"));

            try {

                schedule(job.getString("id"),
                        startDate,
                        job.getString("timing"),
                        job.getString("frequency"),
                        job.getString("cronExpression"),
                        job.getBoolean("finishes"),
                        finishDate,
                        finishTime != null ? new Timestamp(finishTime.getTime()) : null,
                        job.getLong("intervalInSeconds"),
                        job.getLong("intervalInMinutes"),
                        job.getLong("hourlyInterval"),
                        job.getLong("repetitions"),
                        job.getString("dailyFrequency"),
                        job.getLong("dailyInterval"),
                        job.getBoolean("monday"),
                        job.getBoolean("tuesday"),
                        job.getBoolean("wednesday"),
                        job.getBoolean("thursday"),
                        job.getBoolean("friday"),
                        job.getBoolean("saturday"),
                        job.getBoolean("sunday"),
                        job.getString("monthlyOption"),
                        job.getString("dayOfTheWeek"),
                        job.getLong("dayInMonth"));

                message.put("message", OBMessageUtils.getI18NMessage("SCHED_SUCCESS"));
                result.setStatus(WSResult.Status.OK);

            } catch (ServletException | SchedulerException e) {
                result.setStatus(WSResult.Status.INTERNAL_SERVER_ERROR);
                result.setMessage(e.getMessage());
                log.error(e.getMessage(), e);
            }
        }

        result.setResultType(WSResult.ResultType.MULTIPLE);
        result.setData(messages);
        return result;
    }

    @Override
    public WSResult put(String path, Map<String, String> parameters, JSONObject body) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public WSResult delete(String path, Map<String, String> parameters, JSONObject body) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public WSResult get(String path, Map<String, String> parameters) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
