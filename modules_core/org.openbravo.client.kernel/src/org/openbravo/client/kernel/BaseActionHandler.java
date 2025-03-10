/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2010-2022 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ObjectUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.service.json.JsonConstants;

/**
 * Implements the ActionHandler and provides utility methods to sub classes.
 * 
 * @author mtaal
 */
public abstract class BaseActionHandler implements ActionHandler {

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.client.kernel.ActionHandler#execute(javax.servlet.http.HttpServletRequest,
   * javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void execute() {
    final StringBuilder sb = new StringBuilder();
    String line;
    try {
      final HttpServletRequest request = RequestContext.get().getRequest();
      final Map<String, Object> parameterMap = this.extractParametersFromRequest(request);
      final String content = this.extractRequestContent(request, parameterMap);
      // also add the Http Stuff
      parameterMap.put(KernelConstants.HTTP_SESSION, request.getSession(false));
      parameterMap.put(KernelConstants.HTTP_REQUEST, request);

      final JSONObject result = execute(parameterMap, content);

      final HttpServletResponse response = RequestContext.get().getResponse();
      this.writeResponse(parameterMap, result, request, response);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Utility function used by execute() that lets us extend the part where we extract the list of
   * parameters of the request and returns it as a Map<String, Object>
   *
   * @param request
   *          The request
   * @return A map with key the name of the parameter and an Object as the value
   *
   */
  protected Map<String, Object> extractParametersFromRequest(HttpServletRequest request) {
    Map<String, Object> parameterMap = new HashMap<>();
    for (Enumeration<?> keys = request.getParameterNames(); keys.hasMoreElements();) {
      final String key = (String) keys.nextElement();
      if (request.getParameterValues(key) != null && request.getParameterValues(key).length > 1) {
        parameterMap.put(key, request.getParameterValues(key));
      } else {
        parameterMap.put(key, request.getParameter(key));
      }
    }

    return parameterMap;
  }

  /**
   * Extracts the content from the HttpServletRequest as a String.
   * This method reads the request's input stream and returns its content.
   * It uses UTF-8 encoding to read the data.
   *
   * @param request the HttpServletRequest from which to extract the content.
   * @param requestParameters a map of request parameters, which may be used for further processing (unused in this method).
   * @return a String containing the content of the request, or null if the request content is empty.
   * @throws IOException if an input or output exception occurs during reading the request content.
   */
  protected String extractRequestContent(HttpServletRequest request,
      Map<String, Object> requestParameters) throws IOException {
    final StringBuilder sb = new StringBuilder();

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }
    }

    return (sb.length() > 0 ? sb.toString() : null);
  }

  /**
   * Utility function used by execute() that, given the request, the response of the
   * execute(parameters, content) execution and the parameters, writes the response object to be
   * sent back to the client.
   *
   * @param parameters
   *          The list of parameters of the request generated by extractParametersFromRequest()
   * @param result
   *          The result returned by execute(parameters, content)
   * @param request
   *          The request
   * @param response
   *          The response object to be sent back to the client
   * @throws IOException
   *           if there is an error writing the response object
   */
  protected void writeResponse(Map<String, Object> parameters, JSONObject result,
      HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType(JsonConstants.JSON_CONTENT_TYPE);
    response.setHeader("Content-Type", JsonConstants.JSON_CONTENT_TYPE);
    response.getWriter().write(result.toString());
  }

  /**
   * Fixes the request map adding an "context" key to include context info in order to make it
   * available to be evaluated by FilterExpression
   */
  protected Map<String, String> fixRequestMap(Map<String, Object> parameters, JSONObject context) {
    final Map<String, String> retval = new HashMap<String, String>();
    for (Entry<String, Object> entries : parameters.entrySet()) {
      if (entries.getKey().equals(KernelConstants.HTTP_REQUEST)
          || entries.getKey().equals(KernelConstants.HTTP_SESSION)) {
        continue;
      }
      // TODO: ObjectUtils.toString is deprecated in latest versions. Substitute by
      // newer Objects.toString() method when Java 6 support is deprecated.
      retval.put(entries.getKey(), ObjectUtils.toString(entries.getValue(), (String) null));
    }
    if (context != null) {
      retval.put("context", context.toString());
    }
    return retval;
  }

  /**
   * Needs to be implemented by a subclass.
   * 
   * @param parameters
   *          the parameters obtained from the request. Note that the request object and the session
   *          object are also present in this map, resp. as the constants
   *          {@link KernelConstants#HTTP_REQUEST} and {@link KernelConstants#HTTP_SESSION}.
   * @param content
   *          the request content (if any)
   * @return the return should be a JSONObject, this is passed back to the caller on the client.
   */
  protected abstract JSONObject execute(Map<String, Object> parameters, String content);
}
