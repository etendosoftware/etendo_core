package com.smf.securewebservices.utils;

import java.io.PrintWriter;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;
import org.openbravo.service.json.JsonConstants;

import com.smf.securewebservices.rsql.OBRestConstants;

/**
 * @author androettop
 */
public class WSResult extends Result {
	public enum Status {
		OK(200), //
		ACCEPTED(202), //
		NON_AUTHORITATIVE_INFORMATION(203), //
		NO_CONTENT(204), //
		RESET_CONTENT(205), //
		PARTIAL_CONTENT(206), //
		MULTI_STATUS(207), //
		BAD_REQUEST(400), //
		UNAUTHORIZED(401), //
		PAYMENT_REQUIRED(402), //
		FORBIDDEN(403), //
		NOT_FOUND(404), //
		METHOD_NOT_ALLOWED(405), //
		NOT_ACCEPTABLE(406), //
		PROXY_AUTHENTICATION_REQUIRED(407), //
		REQUEST_TIMEOUT(408), //
		CONFLICT(409), //
		GONE(410), //
		LENGTH_REQUIRED(411), //
		PRECONDITION_FAILED(412), //
		REQUEST_TOO_LONG(413), //
		REQUEST_URI_TOO_LONG(414), //
		UNSUPPORTED_MEDIA_TYPE(415), //
		REQUESTED_RANGE_NOT_SATISFIABLE(416), //
		EXPECTATION_FAILED(417), //
		INSUFFICIENT_SPACE_ON_RESOURCE(419), //
		METHOD_FAILURE(420), //
		UNPROCESSABLE_ENTITY(422), //
		LOCKED(423), //
		FAILED_DEPENDENCY(424), //
		INTERNAL_SERVER_ERROR(500), //
		NOT_IMPLEMENTED(501), //
		BAD_GATEWAY(502), //
		SERVICE_UNAVAILABLE(503), //
		GATEWAY_TIMEOUT(504), //
		HTTP_VERSION_NOT_SUPPORTED(505), //
		INSUFFICIENT_STORAGE(507);

		private Integer code;

		Status(Integer code) {
			this.code = code;
		}

		public Integer getCode() {
			return this.code;
		}
	};

	public enum ResultType {
		MULTIPLE, SINGLE;
	}

	private Status status;
	private List<? extends BaseOBObject> obData;
	private JSONObject jsonObjectData;
	private JSONArray jsonArrayData;
	private JSONObject action;
	private DataToJsonConverter dataToJsonConverter;
	private ResultType resultType;

	public WSResult() {
		this.dataToJsonConverter = new SecureDataToJson();
		this.resultType = ResultType.MULTIPLE;
		this.setType(Type.INFO);
	}

	public ResultType getResultType() {
		return resultType;
	}

	public void setResultType(ResultType resultType) {
		this.resultType = resultType;
	}

	public void setDataToJsonConverter(DataToJsonConverter dataToJsonConverter) {
		this.dataToJsonConverter = dataToJsonConverter;
	}

	public void setStatus(Status status) {
		if (status.getCode() >= 400) {
			this.setType(Type.ERROR);
		}
		this.status = status;
	}

	public Status getStatus() {
		return this.status;
	}

	public void setData(List<? extends BaseOBObject> data) {
		obData = data;
		jsonObjectData = null;
		jsonArrayData = null;
	}

	public void setData(JSONObject data) {
		obData = null;
		jsonObjectData = data;
		jsonArrayData = null;
	}

	public void setData(JSONArray data) {
		obData = null;
		jsonObjectData = null;
		jsonArrayData = data;
	}

	public void setAction(JSONObject value) {
		action = value;
	}

	public JSONObject getJSONResponse() throws JSONException {
		JSONObject response = new JSONObject();
		// message
		response.put(OBRestConstants.MESSAGE_PROPERTY, this.getMessage());

		// data part
		JSONArray arrayResult = null;
		if (jsonArrayData != null) {
			if (this.resultType == ResultType.MULTIPLE) {
				arrayResult = jsonArrayData;
			} else if (jsonArrayData.length() > 0) {
				arrayResult = new JSONArray();
				arrayResult.put(jsonArrayData.getJSONObject(0));
			}
		} else if (jsonObjectData != null) {
			arrayResult = new JSONArray();
			arrayResult.put(jsonObjectData);
		} else if (obData != null) {
			if (this.resultType == ResultType.MULTIPLE) {
				arrayResult = new JSONArray();
				for (JSONObject jsonBob : this.dataToJsonConverter.toJsonObjects(obData)) {
					arrayResult.put(jsonBob);
				}
			} else if (!obData.isEmpty()) {
				arrayResult = new JSONArray();
				arrayResult.put(this.dataToJsonConverter.toJsonObject(obData.get(0), DataResolvingMode.FULL));
			}
		}

		if(action != null){
			response.put(OBRestConstants.ACTION_PROPERTY,action);
		}

		response.put(OBRestConstants.RESULT_PROPERTY,
				arrayResult != null && arrayResult.length() > 0
						? (this.resultType == ResultType.MULTIPLE ? arrayResult : arrayResult.get(0))
						: JSONObject.NULL);
		return response;
	}

	public void writeResponse(HttpServletResponse response) throws Exception {
		PrintWriter writer = response.getWriter();
		JSONObject body = getJSONResponse();
		response.setContentType(JsonConstants.JSON_CONTENT_TYPE);
		response.setStatus(this.status.getCode());
		writer.write(body.toString());
		writer.close();
	}
}
