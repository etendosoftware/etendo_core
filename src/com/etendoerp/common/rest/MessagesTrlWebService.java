package com.etendoerp.common.rest;

import java.io.BufferedReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.openbravo.service.web.WebService;

/**
 * REST web service that returns translated {@code AD_Message} entries ({@code AD_Message_Trl})
 * as a JSON object keyed by the message search key (value column).
 *
 * <p>This endpoint is a generic, Copilot-independent replacement for the {@code /labels} path
 * previously exposed by the Copilot module. It can be consumed by any Etendo development that
 * needs to retrieve UI message translations at runtime.</p>
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /etendo/ws/messages_trl?language=&moduleId=} — retrieves all messages for a
 *       given module. Both parameters are mandatory.</li>
 *   <li>{@code POST /etendo/ws/messages_trl} — retrieves specific messages by search key list.
 *       Accepts a JSON body; suitable for large key sets (50+ keys).</li>
 * </ul>
 *
 * <h3>GET query parameters</h3>
 * <table>
 *   <tr><th>Parameter</th><th>Required</th><th>Description</th></tr>
 *   <tr><td>{@code language}</td><td>Yes</td>
 *       <td>Language ID as stored in {@code AD_Language.ad_language} (e.g. {@code es_ES}).</td></tr>
 *   <tr><td>{@code moduleId}</td><td>Yes</td>
 *       <td>ID of the {@code AD_Module} whose messages will be returned.</td></tr>
 * </table>
 *
 * <h3>POST JSON body fields</h3>
 * <table>
 *   <tr><th>Field</th><th>Required</th><th>Description</th></tr>
 *   <tr><td>{@code language}</td><td>Yes</td>
 *       <td>Language ID as stored in {@code AD_Language.ad_language} (e.g. {@code es_ES}).</td></tr>
 *   <tr><td>{@code searchKeys}</td><td>Yes</td>
 *       <td>JSON array of {@code AD_Message.value} search keys to retrieve.</td></tr>
 * </table>
 *
 * <h3>Response</h3>
 * <pre>
 * {
 *   "ETCOP_ErrorSavingFile": "Error al guardar el archivo",
 *   "ETCOP_FileTooBig":      "El archivo es demasiado grande"
 * }
 * </pre>
 */
public class MessagesTrlWebService implements WebService {

  /** Query parameter (GET) / JSON field (POST): language ID (mandatory). */
  public static final String PARAM_LANGUAGE = "language";
  /** Query parameter (GET): module ID (mandatory for GET). */
  public static final String PARAM_MODULE_ID = "moduleId";
  /** JSON field (POST): array of AD_Message search keys (mandatory for POST). */
  public static final String PARAM_SEARCH_KEYS = "searchKeys";

  public static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json;charset=UTF-8";

  // -------------------------------------------------------------------------
  // GET — all messages for a module: ?language=&moduleId=
  // -------------------------------------------------------------------------

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    final String languageId = request.getParameter(PARAM_LANGUAGE);
    final String moduleId = request.getParameter(PARAM_MODULE_ID);

    if (StringUtils.isBlank(moduleId)) {
      sendError(response, HttpServletResponse.SC_BAD_REQUEST,
          "Parameter '" + PARAM_MODULE_ID + "' is required for GET requests.");
      return;
    }

    handleRequest(languageId, moduleId, null, response);
  }

  // -------------------------------------------------------------------------
  // POST — specific messages by search key list, JSON body
  // -------------------------------------------------------------------------

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

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
      sendError(response, HttpServletResponse.SC_BAD_REQUEST,
          "Invalid JSON body: " + e.getMessage());
      return;
    }

    final String languageId = body.optString(PARAM_LANGUAGE, null);

    if (!body.has(PARAM_SEARCH_KEYS)) {
      sendError(response, HttpServletResponse.SC_BAD_REQUEST,
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
      sendError(response, HttpServletResponse.SC_BAD_REQUEST,
          "'" + PARAM_SEARCH_KEYS + "' must be a JSON array of strings.");
      return;
    }

    if (searchKeyList.isEmpty()) {
      sendError(response, HttpServletResponse.SC_BAD_REQUEST,
          "'" + PARAM_SEARCH_KEYS + "' must contain at least one search key.");
      return;
    }

    handleRequest(languageId, null, searchKeyList, response);
  }

  // -------------------------------------------------------------------------
  // Shared logic
  // -------------------------------------------------------------------------

  /**
   * Validates parameters, resolves entities, builds and writes the JSON response.
   */
  private void handleRequest(String languageId, String moduleId, List<String> searchKeyList,
      HttpServletResponse response) throws Exception {

    if (StringUtils.isBlank(languageId)) {
      sendError(response, HttpServletResponse.SC_BAD_REQUEST,
          "Parameter '" + PARAM_LANGUAGE + "' is required.");
      return;
    }

    boolean hasModuleId = StringUtils.isNotBlank(moduleId);

    try {
      OBContext.setAdminMode(false);

      // Resolve Language entity
      OBCriteria<Language> langCrit = OBDal.getInstance().createCriteria(Language.class);
      langCrit.add(Restrictions.eq(Language.PROPERTY_LANGUAGE, languageId));
      langCrit.setMaxResults(1);
      Language lang = (Language) langCrit.uniqueResult();
      if (lang == null) {
        sendError(response, HttpServletResponse.SC_NOT_FOUND,
            "Language '" + languageId + "' not found.");
        return;
      }

      // Optionally resolve Module entity
      Module module = null;
      if (hasModuleId) {
        module = OBDal.getInstance().get(Module.class, moduleId);
        if (module == null) {
          sendError(response, HttpServletResponse.SC_NOT_FOUND,
              "Module with id '" + moduleId + "' not found.");
          return;
        }
      }

      JSONObject jsonLabels = buildJsonLabels(lang, module, searchKeyList);

      response.setContentType(APPLICATION_JSON_CHARSET_UTF_8);
      final Writer w = response.getWriter();
      w.write(jsonLabels.toString());
      w.close();

    } catch (JSONException e) {
      sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Error building JSON response: " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Builds a JSON object with translated message texts.
   *
   * <h4>Logic (mirrors the Copilot {@code getJSONLabels} behaviour):</h4>
   * <ul>
   *   <li>When a {@code module} is provided: if the module's own base language matches the
   *       requested language, read directly from {@code AD_Message}; otherwise read from
   *       {@code AD_Message_Trl}.</li>
   *   <li>When only {@code searchKeyList} is provided (no module): query
   *       {@code AD_Message_Trl} first, then fall back to {@code AD_Message} for every key
   *       whose translation was not found.</li>
   * </ul>
   *
   * @param lang          the resolved {@link Language} entity
   * @param module        optional {@link Module} to filter messages; may be {@code null}
   * @param searchKeyList optional list of message search keys to restrict results; may be
   *                      {@code null}
   * @return a {@link JSONObject} mapping search keys to translated message texts
   */
  private JSONObject buildJsonLabels(Language lang, Module module, List<String> searchKeyList)
      throws JSONException {

    if (module != null) {
      // Module-scoped: use the same branch logic as Copilot's getJSONLabels.
      // If the requested language IS the module's own base language, AD_Message already
      // contains the native text — no translation record exists for it.
      boolean useBaseLanguage = lang.getId().equals(module.getLanguage().getId());
      if (useBaseLanguage) {
        return buildFromBaseLanguage(module, searchKeyList);
      }
      return buildFromTranslations(lang, module, searchKeyList);
    }

    // No module: search-key list path.
    // Try translations first, then fill any missing keys from AD_Message.
    return buildFromTranslationsWithFallback(lang, searchKeyList);
  }

  /**
   * Reads messages directly from {@code AD_Message} (module base language branch).
   */
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

  /**
   * Reads translated messages from {@code AD_Message_Trl} joined with {@code AD_Message}.
   * Used when a module is provided and its language differs from the requested language.
   */
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

  /**
   * Reads translations from {@code AD_Message_Trl} for the given search keys, then falls
   * back to {@code AD_Message} for any key whose translation was not found.
   * Used for the search-key list path (no module provided).
   */
  private JSONObject buildFromTranslationsWithFallback(Language lang, List<String> searchKeyList)
      throws JSONException {

    JSONObject result = buildFromTranslations(lang, null, searchKeyList);

    // Determine which requested keys had no translation record.
    // JSONObject.names() returns null when the object is empty, so guard against it.
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
      // Fill missing keys from AD_Message (base language text)
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

  /**
   * Writes a plain-text error response with the given HTTP status code and message.
   */
  private void sendError(HttpServletResponse response, int status, String message)
      throws Exception {
    response.setStatus(status);
    response.setContentType("text/plain;charset=UTF-8");
    final Writer w = response.getWriter();
    w.write(message);
    w.close();
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    sendError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "PUT not supported.");
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    sendError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "DELETE not supported.");
  }
}
