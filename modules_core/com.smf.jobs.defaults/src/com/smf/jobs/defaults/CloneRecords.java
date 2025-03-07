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
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Map;

/**
 * An action that clones any record.
 * It will use {@link DalUtil#copy(BaseOBObject)} by default.
 * To customize the copy behaviour, implement a hook of type {@link CloneRecordHook}
 */
public class CloneRecords extends Action {
    Logger log = LogManager.getLogger();

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
            result.setMessage(e.getMessage());
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
}
