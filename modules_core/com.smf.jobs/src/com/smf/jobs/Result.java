package com.smf.jobs;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.ResponseActionsBuilder;
import org.openbravo.erpCommon.utility.OBError;

import java.util.Optional;

/**
 * Represents a generic result that can be used on webservices, processes, or anything else.
 * @author androettop
 * @author angelo
 */
public class Result {
    /**
     * Type enum. Contains all the kinds of Results available.
     */
    public enum Type {
        SUCCESS, ERROR, WARNING, INFO, RUNNING, PENDING;

        /**
         * @return a String title depending on the Type. Useful when constructing messages.
         */
        private String getTitle() {
            switch (this) {
                case SUCCESS: return "Success";
                case ERROR: return "Error";
                case WARNING: return "Warning";
                case INFO: return "Info";
                case RUNNING: return "Running";
                case PENDING: return "Pending";
                default: return "";
            }
        }

        /**
         * @return a {@link ResponseActionsBuilder.MessageType} depending on the Type. Useful when constructing response actions for Process Definitions.
         */
        private ResponseActionsBuilder.MessageType toMessageType() {
            switch (this) {
                case SUCCESS: return ResponseActionsBuilder.MessageType.SUCCESS;
                case ERROR: return ResponseActionsBuilder.MessageType.ERROR;
                case WARNING: return ResponseActionsBuilder.MessageType.WARNING;
                case INFO: return ResponseActionsBuilder.MessageType.INFO;
                default: return ResponseActionsBuilder.MessageType.valueOf(this.toString());
            }
        }
    }

    private Type type;
    private String message;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Transform this results to an {@link OBError} type for compatibility
     * @return {@link OBError} representation of this Result
     */
    public OBError toOBError() {
        OBError error = new OBError();
        String type = this.type.name();
        error.setType(type);
        // capitalize title error
        error.setTitle(type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase());
        error.setMessage(this.message);
        return error;
    }

    /**
     * Creates a Result from an existing {@link OBError}
     * @param error the input {@link OBError}
     * @return a {@link Result} based on the input
     */
    public static Result fromOBError(OBError error) {
        Result result = new Result();
        result.setMessage(error.getMessage());
        try {
            result.setType(Result.Type.valueOf(error.getType().toUpperCase()));
        } catch (IllegalArgumentException e) {
            result.setType(Type.INFO);
        }
        return result;
    }

    /**
     * Creates a JSONObject based on this Results contents.
     * This is equivalent to converting first to an {@link OBError} and then to a {@link JSONObject}
     * @return a {@link JSONObject} with message, title, and type keys filled with the Result's data.
     */
    public JSONObject toJSON() {
        return new JSONObject(toOBError().toMap());
    }

    /**
     * Construct a JSON object based on the ResponseActionBuilder factory.
     * In order to add more actions other than showMsgInView, pass a constructed builder
     * instead of using getResponseBuilder()
     * @param builder a ResponseActionsBuilder
     * @param includeMessage where to add a showMsgInView response to show the message in the UI
     * @return a JSON Object like the ones returned by a Standard Process definition
     */
    public JSONObject toJSON(ResponseActionsBuilder builder, boolean includeMessage) {
        final JSONObject jsonMessage;

        jsonMessage = Optional.ofNullable(builder)
            .map(b -> includeMessage ? b.showMsgInProcessView(type.toMessageType(), type.getTitle(), this.message).build() : b.build())
            .orElseGet(this::toJSON);


        return jsonMessage;
    }

    /**
     * Creates a Result from the json.
     * keys "type" and "message" are used by default to fill the result object.
     * To use custom keys use the method {@link #fromJSON(JSONObject, String, String)}
     * @param json the input json object (can not be null)
     * @return result based on the JSON
     */
    public static Result fromJSON(JSONObject json) {
        return fromJSON(json, null, null);
    }

    /**
     * Creates a Result from the json.
     * keys "type" and "message" are used by default to fill the result object.
     * @param json the input json object (can not be null)
     * @return result based on the JSON
     */
    public static Result fromJSON(JSONObject json, String typeKey, String messageKey) {
        var result = new Result();
        var _typeKey = Optional.ofNullable(typeKey).orElse("type");
        var _messageKey = Optional.ofNullable(messageKey).orElse("message");

        var typeString = Optional.ofNullable(json.optString(_typeKey));
        var message = Optional.ofNullable(json.optString(_messageKey));

        result.setType(Type.valueOf(typeString.orElseThrow()));
        result.setMessage(message.orElseThrow());

        return result;
    }
}