package com.smf.jobs.defaults;

import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Data;
import com.smf.jobs.Result;
import com.smf.jobs.hooks.CloneRecordHook;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An action that clones any record.
 * It will use {@link DalUtil#copy(BaseOBObject)} by default.
 * To customize the copy behaviour, implement a hook of type {@link CloneRecordHook}
 */
public class CloneRecords extends Action {
    Logger log = LogManager.getLogger();
    
    private static final Pattern POSTGRESQL_ERROR_PATTERN = Pattern.compile("^ERROR:\\s*+(.+)$", Pattern.MULTILINE);
    private static final Pattern MESSAGE_CODE_PATTERN = Pattern.compile("@([^@]+)@");
    
    @Inject
    @Any
    private Instance<CloneRecordHook> cloneHooks;

    private JSONArray jsonOutput;

    @Override
    protected JSONObject doExecute(Map<String, Object> parameters, String content) {
        var result = super.doExecute(parameters, content);
        // Add an extra "records" property to use when being called from a toolbar button in smartclient UI
        try {
            result.put("records", jsonOutput);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    @Override
    protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
        var result = new ActionResult();
        var output = new Data();
        var outputContents = new ArrayList<BaseOBObject>();
        final var jsonConverter = new DataToJsonConverter();
        jsonOutput = new JSONArray();


        result.setType(Result.Type.SUCCESS);

        try {
            var input = getInputContents(getInputClass());
            var inputIterator = input.listIterator();
            var copyChildren = parameters.getBoolean("copyChildren");

            CloneRecordHook hook = null;

            while (inputIterator.hasNext() && isStopped.isFalse()) {
                BaseOBObject bob = inputIterator.next();
                BaseOBObject clonedBOB;

                if (hook == null) {
                    // Hook is only selected once, as list should have all members of the same entity.
                    hook = selectHook(bob.getEntityName());
                }

                if (hook != null) {
                    clonedBOB = hook.copy(bob, copyChildren);
                } else {
                    clonedBOB = DalUtil.copy(bob, copyChildren);
                }

                OBDal.getInstance().save(clonedBOB);
                outputContents.add(clonedBOB);
                jsonOutput.put(jsonConverter.toJsonObject(clonedBOB, DataResolvingMode.FULL));
            }
            OBDal.getInstance().flush();
        } catch (Exception e) {
            jsonOutput = new JSONArray();
            log.error(e.getMessage(), e);
            result.setType(Result.Type.ERROR);
            result.setMessage(getRootCauseMessage(e));
        }

        output.setContents(outputContents);
        result.setOutput(output);

        return result;
    }

    /**
     * @see CloneRecordHook
     * @param entityName the record's entity name
     * @return a hook when found for this entity, null otherwise
     */
    public CloneRecordHook selectHook(String entityName) {
        CloneRecordHook hook = null;

        for (CloneRecordHook nextHook : cloneHooks.select(new ComponentProvider.Selector(entityName))) {
            if (hook == null) {
                hook = nextHook;
            } else if (nextHook.getPriority() < hook.getPriority()) {
                hook = nextHook;
            } else if (nextHook.getPriority() == hook.getPriority()) {
                log.warn(
                        "Trying to get a clone records hook for the entity {}, and there are more than one instance with same priority. Selecting the first.",
                        entityName);
            }
        }

        return hook;
    }

    @Override
    protected Class<BaseOBObject> getInputClass() {
        return BaseOBObject.class;
    }

    /**
     * Returns the message of the deepest cause of the given exception.
     * @param e the exception to inspect
     * @return the cleaned and translated root-cause message, or an empty string if none is available
     */
    protected String getRootCauseMessage(Exception e) {
        Throwable rootCause = e;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        String message = rootCause.getMessage();
        if (message == null) {
            return "";
        }
        message = cleanPostgreSQLMessage(message);
        message = translateMessageCodes(message);
        return message.trim();
    }

    /**
     * Extracts and normalizes the main PostgreSQL error text from a raw exception message.
     * @param message the raw exception message
     * @return the extracted PostgreSQL error text, or an empty string if the input is {@code null}
     */
    private String cleanPostgreSQLMessage(String message) {
        if (message == null) {
            return "";
        }
        Matcher matcher = POSTGRESQL_ERROR_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return message;
    }

    /**
     * Replaces all message codes in the {@code @CODE@} format with their translated values.
     * @param message the message that may contain {@code @CODE@} tokens
     * @return the message with all codes translated (when possible)
     */
    private String translateMessageCodes(String message) {
        if (message == null || !message.contains("@")) {
            return message;
        }
        try {
            OBDal.getInstance().rollbackAndClose();
        } catch (Exception ex) {
            log.debug("Could not rollback session before translating message codes.", ex);
        }
        Matcher matcher = MESSAGE_CODE_PATTERN.matcher(message);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String messageCode = matcher.group(1);
            String translation = resolveMessageCode(messageCode);
            matcher.appendReplacement(result, Matcher.quoteReplacement(translation));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Resolves a single message code using {@link OBMessageUtils#messageBD(String)}.
     * If the database is not accessible, returns the code itself as fallback.
     * @param messageCode the message code to resolve
     * @return the translated message, or the original code if translation is not available
     */
    private String resolveMessageCode(String messageCode) {
        try {
            String translated = OBMessageUtils.messageBD(messageCode);
            if (!messageCode.equals(translated)) {
                return translated;
            }
        } catch (Exception ex) {
            log.warn("Could not resolve message code '{}' via DB.", messageCode);
        }
        return messageCode;
    }
}
