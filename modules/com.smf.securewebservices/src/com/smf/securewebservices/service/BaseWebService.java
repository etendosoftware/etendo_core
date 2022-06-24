package com.smf.securewebservices.service;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.service.web.WebService;

import com.smf.securewebservices.rsql.OBRestUtils;
import com.smf.securewebservices.utils.WSResult;

/**
 * @author androettop
 */
public abstract class BaseWebService implements WebService {

	public abstract WSResult get(String path, Map<String, String> parameters) throws Exception;

	public void doGet(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		WSResult result = get(path, OBRestUtils.requestParamsToMap(request));
		OBRestUtils.writeWSResponse(result, response);
	}

	public abstract WSResult post(String path, Map<String, String> parameters, JSONObject body) throws Exception;

	public void doPost(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		WSResult result = post(path, OBRestUtils.requestParamsToMap(request), OBRestUtils.getBodyData(request));
		OBRestUtils.writeWSResponse(result, response);
	}

	public abstract WSResult put(String path, Map<String, String> parameters, JSONObject body) throws Exception;

	public void doPut(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		WSResult result = put(path, OBRestUtils.requestParamsToMap(request), OBRestUtils.getBodyData(request));
		OBRestUtils.writeWSResponse(result, response);
	}

	public abstract WSResult delete(String path, Map<String, String> parameters, JSONObject body) throws Exception;

	public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		WSResult result = delete(path, OBRestUtils.requestParamsToMap(request), OBRestUtils.getBodyData(request));
		OBRestUtils.writeWSResponse(result, response);
	}
}