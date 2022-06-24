package com.smf.jobs;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder;

import java.util.Optional;

/**
 * Represents a result of an Action
 */
public class ActionResult extends Result {
    private Data output;
    private ResponseActionsBuilder responseActionsBuilder;

    public Optional<ResponseActionsBuilder> getResponseActionsBuilder() {
        return Optional.ofNullable(responseActionsBuilder);
    }

    /**
     * Having used {@link #setResponseActionsBuilder(ResponseActionsBuilder)}
     * this will return the response actions to be used as the return value in a Process Defition
     * @return an Optional instance with the response action JSONObject present, a builder was previously set. If not, the Optional will be empty
     */
    public Optional<JSONObject> getResponseActions() {
        return getResponseActionsBuilder().map(ResponseActionsBuilder::build);
    }

    /**
     * Set a custom {@link ResponseActionsBuilder} when you need more than the message shown in the UI
     * NOTE: This is only used by Process Definitions
     * @param responseActionsBuilder the builder obtained from {@link BaseProcessActionHandler#getResponseBuilder}
     */
    public void setResponseActionsBuilder(ResponseActionsBuilder responseActionsBuilder) {
        this.responseActionsBuilder = responseActionsBuilder;
    }

    /**
     * Use when an action returns data that can be chained to the next action to be executed
     * @param output a {@link Data} object with the output in its contents.
     */
    public void setOutput(Data output) {
        this.output = output;
    }

    /**
     * @return an Optional Data instance. It will have contents if the action that generated this result, outputs data to be used in another action.
     */
    public Optional<Data> getOutput() {
        return Optional.ofNullable(output);
    }
}
