package com.smf.securewebservices.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.util.CheckException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.SessionInfo;
import org.openbravo.service.json.DefaultJsonDataService;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;
import org.openbravo.service.web.InvalidContentException;
import org.openbravo.service.web.InvalidRequestException;
import org.openbravo.service.web.ResourceNotFoundException;
import org.openbravo.service.web.WebService;
import org.openbravo.service.web.WebServiceUtil;


public class JsonDalWebService implements WebService {
  private static final Logger log = LogManager.getLogger();

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {

    try {
      final Map<String, String> parameters = getParameterMap(request);
      // checks and set parameters, if not valid then go away
      if (!checkSetParameters(path,request, response, parameters)) {
        return;
      }
      SessionInfo.setQueryProfile("jsonWebService");
      // now do the action

      // a special case if the id is asked directly then only return the single record.
      if (parameters.containsKey(JsonConstants.ID)) {
        String result = DefaultJsonDataService.getInstance().fetch(parameters);
        final JSONObject jsonObject = new JSONObject(result);
        final JSONObject responseObject = jsonObject.getJSONObject(JsonConstants.RESPONSE_RESPONSE);
        if (responseObject.get(JsonConstants.DATA) instanceof JSONArray) {
          final JSONArray jsonArray = responseObject.getJSONArray(JsonConstants.DATA);
          if (jsonArray.length() == 0) {
            throw new ResourceNotFoundException("Object (" + parameters.get(JsonConstants.ID) + "/"
                + parameters.get(JsonConstants.ENTITYNAME) + ") not found");
          } else if (jsonArray.length() == 1) {
            result = jsonArray.getJSONObject(0).toString();
          }
        } else {
          final JSONObject jsonDataObject = responseObject.getJSONObject(JsonConstants.DATA);
          result = jsonDataObject.toString();
        }
        writeResult(response, result);
      } else {
        JSONStreamWriter writer = new JSONStreamWriter(response, parameters);
        parameters.put(JsonConstants.USE_ALIAS, "true");
        parameters.put(JsonConstants.IS_WS_CALL, "true");
        parameters.put(JsonConstants.WHERE_AND_FILTER_CLAUSE,
            parameters.get(JsonConstants.WHERE_PARAMETER));
        DefaultJsonDataService.getInstance().fetch(parameters, writer);
        writer.close();
      }

    } catch (final JSONException e) {
      throw new InvalidContentException(e);
    }
  }
  @Override
  public void  doPost(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
    final Map<String, String> parameters = getParameterMap(request);
    // note if clause updates parameter map
    if (checkSetIDEntityName(path,request, response, parameters)) {
      final String result = DefaultJsonDataService.getInstance()
          .add(parameters, getRequestContent(request));
      writeResult(response, result);
    }
  }
  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
    final Map<String, String> parameters = getParameterMap(request);
    // checks and set parameters, if not valid then go away
    if (!checkSetParameters(path,request, response, parameters)) {
      return;
    }
    final String id = parameters.get(JsonConstants.ID);

    final String entityName = parameters.get(JsonConstants.ENTITYNAME);

    if (entityName == null) {
      throw new InvalidRequestException("No entityName parameter");
    }

    if (id == null) {
      JSONObject jsonRequest = new JSONObject(getRequestContent(request));
      JSONArray data = jsonRequest.getJSONArray("data");
      JSONArray dataResult = new JSONArray();

      if (data == null || data.length() == 0) {
        throw new InvalidRequestException("No id parameter");
      }

      for (int i = 0; i < data.length(); i++) {
        String currentId = data.getString(i);
        Map<String, String> removeParams = new HashMap<String, String>();
        removeParams.put(JsonConstants.ID, currentId);
        removeParams.put(JsonConstants.ENTITYNAME, parameters.get(JsonConstants.ENTITYNAME));
        
        JSONObject res = new JSONObject(DefaultJsonDataService.getInstance().remove(removeParams));
        JSONObject _resObj = res.getJSONObject("response");
        JSONObject _data = null;

        if (_resObj.has("data")) {
          _data = _resObj.getJSONArray("data").getJSONObject(0);
        } else {
          _data = _resObj;
        }
        if (_resObj.has("error")) {
          JSONObject err = _resObj.getJSONObject("error");
          writeResult(response, JsonUtils.convertExceptionToJson(new OBException(err.getString("message"))));
        }


        dataResult.put(_data);
      }
      
      final String result = dataResult.toString();
      writeResult(response, result);
    } else {
      final BaseOBObject bob = OBDal.getInstance().get(entityName, id);
      if (bob == null) {
        throw new ResourceNotFoundException("Object (" + id + "/" + entityName + ") not found");
      }

      // now do the action
      final String result = DefaultJsonDataService.getInstance().remove(parameters);
      writeResult(response, result);
    }
  }
  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
    final Map<String, String> parameters = getParameterMap(request);
    // note if clause updates parameter map
    if (checkSetIDEntityName(path,request, response, parameters)) {
      final String result = DefaultJsonDataService.getInstance()
          .update(parameters, getRequestContent(request));
      writeResult(response, result);
    }
  }

  private boolean checkSetParameters(String path,HttpServletRequest request, HttpServletResponse response,
      Map<String, String> parameters) throws IOException {
    final String[] pathParts = WebServiceUtil.getInstance().getSegments(path);
    if (pathParts.length == 0) {
      writeResult(response, JsonUtils.convertExceptionToJson(
          new InvalidRequestException("Invalid url, no entityName: " + request.getRequestURI())));
      return false;
    }
    final String entityName = pathParts[0];
    // check it the entity
    try {
      ModelProvider.getInstance().getEntity(entityName);
    } catch (CheckException e) {
      writeResult(response, JsonUtils.convertExceptionToJson(new InvalidRequestException(
          "Invalid url, no entity found with entityName: " + request.getRequestURI())));
      return false;
    }
    parameters.put(JsonConstants.ENTITYNAME, entityName);
    if (pathParts.length > 1) {
      // search on the exact id
      parameters.put(JsonConstants.ID, pathParts[1]);
      if (!parameters.containsKey(JsonConstants.TEXTMATCH_PARAMETER)) {
        parameters.put(JsonConstants.TEXTMATCH_PARAMETER, JsonConstants.TEXTMATCH_EXACT);
        parameters.put(JsonConstants.TEXTMATCH_PARAMETER_OVERRIDE, JsonConstants.TEXTMATCH_EXACT);
      }
    }
    return true;
  }

  private Map<String, String> getParameterMap(HttpServletRequest request) {
    final Map<String, String> parameterMap = new HashMap<String, String>();
    for (@SuppressWarnings("rawtypes")
    Enumeration keys = request.getParameterNames(); keys.hasMoreElements();) {
      final String key = (String) keys.nextElement();
      parameterMap.put(key, request.getParameter(key));
    }
    return parameterMap;
  }

  // NOTE: parameters parameter is updated inside this method
  private boolean checkSetIDEntityName(String path, HttpServletRequest request, HttpServletResponse response,
      Map<String, String> parameters) throws IOException {
	final String[] pathParts = WebServiceUtil.getInstance().getSegments(path);
    if (pathParts.length == 0) {
      return true;
    }
    final String entityName = pathParts[0];
    // check it the entity
    try {
      ModelProvider.getInstance().getEntity(entityName);
    } catch (CheckException e) {
      writeResult(response, JsonUtils.convertExceptionToJson(new InvalidRequestException(
          "Invalid url, no entity found with entityName: " + request.getRequestURI())));
      return false;
    }
    parameters.put(JsonConstants.ENTITYNAME, entityName);
    if (pathParts.length > 1) {
      // search on the exact id
      parameters.put(JsonConstants.ID, pathParts[1]);
      if (!parameters.containsKey(JsonConstants.TEXTMATCH_PARAMETER)) {
        parameters.put(JsonConstants.TEXTMATCH_PARAMETER, JsonConstants.TEXTMATCH_EXACT);
        parameters.put(JsonConstants.TEXTMATCH_PARAMETER_OVERRIDE, JsonConstants.TEXTMATCH_EXACT);
      }
    }
    return true;
  }

  private void writeResult(HttpServletResponse response, String result) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Content-Type", "application/json;charset=UTF-8");

    final Writer w = response.getWriter();
    w.write(result);
    w.close();
  }

  private String getRequestContent(HttpServletRequest request) throws IOException {
    final BufferedReader reader = request.getReader();
    if (reader == null) {
      return "";
    }
    String line;
    final StringBuilder sb = new StringBuilder();
    while ((line = reader.readLine()) != null) {
      if (sb.length() > 0) {
        sb.append("\n");
      }
      sb.append(line);
    }
    log.debug("REQUEST CONTENT>>>>");
    log.debug(sb.toString());
    return sb.toString();
  }

  /**
   * Helper class to write JSON results streaming them directly into the response PrintWriter
   * 
   * @author alostale
   * 
   */
  private class JSONStreamWriter extends DefaultJsonDataService.QueryResultWriter {
    PrintWriter writer;
    int lines = 0;
    int startRow = 0;
    int endRow = -1;
    int computedMaxResults = Integer.MAX_VALUE;
    boolean limitReached = false;

    public JSONStreamWriter(HttpServletResponse response, Map<String, String> parameters)
        throws IOException {

      if (parameters.containsKey(JsonConstants.ENDROW_PARAMETER)) {
        endRow = Integer.valueOf(parameters.get(JsonConstants.ENDROW_PARAMETER));
      }

      if (parameters.containsKey(JsonConstants.STARTROW_PARAMETER)) {
        startRow = Integer.valueOf(parameters.get(JsonConstants.STARTROW_PARAMETER));
        computedMaxResults = endRow - startRow + 1;
      }
      response.setCharacterEncoding("UTF-8");
      writer = response.getWriter();

      response.setContentType("application/json;charset=UTF-8");
      response.setHeader("Content-Type", "application/json;charset=UTF-8");

      writer.write("{\"" + JsonConstants.RESPONSE_RESPONSE + "\":{\"" + JsonConstants.RESPONSE_DATA
          + "\":[");
    }

    @Override
    public void write(JSONObject json) {
      try {
        lines += 1;
        if (lines >= computedMaxResults) {
          limitReached = true;
          return;
        }
        if (lines > 1) {
          writer.write(",");
        }
        writer.write(json.toString());
      } catch (Exception e) {
        log.error("Error writing json ws response", e);
      }
    }

    public void close() throws IOException {
      writer.write("]");
      writer.write(
          ",\"" + JsonConstants.RESPONSE_STATUS + "\":" + JsonConstants.RPCREQUEST_STATUS_SUCCESS);
      writer.write(
          ",\"" + JsonConstants.RESPONSE_TOTALROWS + "\":" + (lines + (limitReached ? 1 : 0)));
      if (startRow != -1) {
        writer.write(",\"" + JsonConstants.RESPONSE_STARTROW + "\":" + startRow);
      }

      writer.write(",\"" + JsonConstants.RESPONSE_ENDROW + "\":" + (startRow + lines - 1));

      writer.write("}}");
      writer.close();
    }
  }

}
