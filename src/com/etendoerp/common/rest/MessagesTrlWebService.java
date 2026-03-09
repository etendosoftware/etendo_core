/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * Version 1.0 ("License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * https://etendo.software/licenses/etendo-public-license
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * The Original Code is Etendo ERP.
 * All portions are Copyright (C) 2026 Etendo Software SL
 * All Rights Reserved.
 ************************************************************************
 */
package com.etendoerp.common.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Message;
import org.openbravo.model.ad.ui.MessageTrl;
import org.openbravo.service.json.JsonConstants;

import com.smf.securewebservices.service.BaseSecureWebServiceServlet;

/**
 * Servlet that returns {@code AD_Message} translations as a JSON object keyed by search key.
 * Extends {@link BaseSecureWebServiceServlet} to support Bearer token (JWT) authentication
 * via Secure Web Services (SWS).
 *
 * <ul>
 *   <li>{@code GET /etendo/rest/MessagesTrl?language=&moduleId=} — all messages for a module.</li>
 *   <li>{@code POST /etendo/rest/MessagesTrl} body: {@code {"language":"es_ES","searchKeys":[…]}}
 *       — specific messages by key; falls back to base-language text for missing translations.</li>
 * </ul>
 */
public class MessagesTrlWebService extends BaseSecureWebServiceServlet {
  private static final long serialVersionUID = 1L;

  /** Query parameter (GET) / JSON field (POST): language ID (mandatory). */
  public static final String PARAM_LANGUAGE = "language";
  /** Query parameter (GET): module ID (mandatory for GET). */
  public static final String PARAM_MODULE_ID = "moduleId";
  /** JSON field (POST): array of AD_Message search keys (mandatory for POST). */
  public static final String PARAM_SEARCH_KEYS = "searchKeys";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    final String languageId = StringUtils.defaultIfBlank(
        request.getParameter(PARAM_LANGUAGE),
        OBContext.getOBContext().getLanguage().getLanguage());
    final String moduleId = request.getParameter(PARAM_MODULE_ID);

    if (StringUtils.isBlank(moduleId)) {
      sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
          "Parameter '" + PARAM_MODULE_ID + "' is required for GET requests.");
      return;
    }

    handleRequest(languageId, moduleId, null, response);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    JSONObject body;
    try {
      StringBuilder sb = new StringBuilder();
      try (BufferedReader reader = request.getReader()) {
        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line);
        }
      }
      body = new JSONObject(sb.toString());
    } catch (JSONException e) {
      sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
          "Invalid JSON body: " + e.getMessage());
      return;
    }

    final String languageId = StringUtils.defaultIfBlank(
        body.optString(PARAM_LANGUAGE, null),
        OBContext.getOBContext().getLanguage().getLanguage());

    if (!body.has(PARAM_SEARCH_KEYS)) {
      sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
          "Field '" + PARAM_SEARCH_KEYS + "' is required for POST requests.");
      return;
    }

    List<String> searchKeyList;
    try {
      JSONArray keysArray = body.getJSONArray(PARAM_SEARCH_KEYS);
      searchKeyList = new ArrayList<>();
      for (int i = 0; i < keysArray.length(); i++) {
        String key = keysArray.optString(i, "").trim();
        if (!key.isEmpty()) {
          searchKeyList.add(key);
        }
      }
    } catch (JSONException e) {
      sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
          "'" + PARAM_SEARCH_KEYS + "' must be a JSON array of strings.");
      return;
    }

    if (searchKeyList.isEmpty()) {
      sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
          "'" + PARAM_SEARCH_KEYS + "' must contain at least one search key.");
      return;
    }

    handleRequest(languageId, null, searchKeyList, response);
  }

  /**
   * Validates parameters, resolves entities, builds and writes the JSON response.
   */
  private void handleRequest(String languageId, String moduleId, List<String> searchKeyList,
      HttpServletResponse response) throws IOException {

    if (StringUtils.isBlank(languageId)) {
      sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
          "Parameter '" + PARAM_LANGUAGE + "' is required.");
      return;
    }

    boolean hasModuleId = StringUtils.isNotBlank(moduleId);

    try {
      OBContext.setAdminMode(false);

      OBCriteria<Language> langCrit = OBDal.getInstance().createCriteria(Language.class);
      langCrit.add(Restrictions.eq(Language.PROPERTY_LANGUAGE, languageId));
      langCrit.setMaxResults(1);
      Language lang = (Language) langCrit.uniqueResult();
      if (lang == null) {
        sendJsonError(response, HttpServletResponse.SC_NOT_FOUND,
            "Language '" + languageId + "' not found.");
        return;
      }

      Module module = null;
      if (hasModuleId) {
        module = OBDal.getInstance().get(Module.class, moduleId);
        if (module == null) {
          sendJsonError(response, HttpServletResponse.SC_NOT_FOUND,
              "Module with id '" + moduleId + "' not found.");
          return;
        }
      }

      JSONObject jsonLabels = buildJsonLabels(lang, module, searchKeyList);

      response.setContentType(JsonConstants.JSON_CONTENT_TYPE);
      final Writer w = response.getWriter();
      w.write(jsonLabels.toString(2));
      w.close();

    } catch (JSONException e) {
      sendJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Error building JSON response: " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private JSONObject buildJsonLabels(Language lang, Module module, List<String> searchKeyList)
      throws JSONException {

    if (module != null) {
      boolean useBaseLanguage = lang.getId().equals(module.getLanguage().getId());
      if (useBaseLanguage) {
        return buildFromBaseLanguage(module, searchKeyList);
      }
      return buildFromTranslations(lang, module, searchKeyList);
    }

    return buildFromTranslationsWithFallback(lang, searchKeyList);
  }

  private JSONObject buildFromBaseLanguage(Module module, List<String> searchKeyList)
      throws JSONException {

    OBCriteria<Message> crit = OBDal.getInstance().createCriteria(Message.class);
    if (module != null) {
      crit.add(Restrictions.eq(Message.PROPERTY_MODULE, module));
    }
    if (searchKeyList != null && !searchKeyList.isEmpty()) {
      crit.add(Restrictions.in(Message.PROPERTY_SEARCHKEY, searchKeyList));
    }

    JSONObject result = new JSONObject();
    for (Message msg : crit.list()) {
      result.put(msg.getSearchKey(), msg.getMessageText());
    }
    return result;
  }

  private JSONObject buildFromTranslations(Language lang, Module module, List<String> searchKeyList)
      throws JSONException {

    OBCriteria<MessageTrl> crit = OBDal.getInstance().createCriteria(MessageTrl.class);
    crit.add(Restrictions.eq(MessageTrl.PROPERTY_LANGUAGE, lang));
    crit.createAlias(MessageTrl.PROPERTY_MESSAGE, "msg");
    if (module != null) {
      crit.add(Restrictions.eq("msg." + Message.PROPERTY_MODULE, module));
    }
    if (searchKeyList != null && !searchKeyList.isEmpty()) {
      crit.add(Restrictions.in("msg." + Message.PROPERTY_SEARCHKEY, searchKeyList));
    }

    JSONObject result = new JSONObject();
    for (MessageTrl msgTrl : crit.list()) {
      result.put(msgTrl.getMessage().getSearchKey(), msgTrl.getMessageText());
    }
    return result;
  }

  private JSONObject buildFromTranslationsWithFallback(Language lang, List<String> searchKeyList)
      throws JSONException {

    JSONObject result = buildFromTranslations(lang, null, searchKeyList);

    Set<String> found = new HashSet<>();
    JSONArray foundNames = result.names();
    if (foundNames != null) {
      for (int i = 0; i < foundNames.length(); i++) {
        found.add(foundNames.getString(i));
      }
    }

    List<String> missing = new ArrayList<>();
    for (String key : searchKeyList) {
      if (!found.contains(key)) {
        missing.add(key);
      }
    }

    if (!missing.isEmpty()) {
      JSONObject fallback = buildFromBaseLanguage(null, missing);
      JSONArray fallbackNames = fallback.names();
      if (fallbackNames != null) {
        for (int i = 0; i < fallbackNames.length(); i++) {
          String key = fallbackNames.getString(i);
          result.put(key, fallback.get(key));
        }
      }
    }

    return result;
  }

  private void sendJsonError(HttpServletResponse response, int status, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("text/plain;charset=UTF-8");
    final Writer w = response.getWriter();
    w.write(message);
    w.close();
  }
}
