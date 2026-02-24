/*
 *************************************************************************
 * The contents of this file are subject to the Etendo Public License
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Message;
import org.openbravo.test.base.TestConstants;
import org.openbravo.test.webservice.BaseWSTest;

/**
 * Integration tests for {@link MessagesTrlWebService} ({@code /ws/messages_trl}).
 *
 * <p>Requires a running Tomcat and a database containing the Core module with at least one
 * {@code AD_Message} row. Tests that need Spanish skip gracefully when {@code es_ES} is absent.
 * All requests are authenticated as {@value BaseWSTest#LOGIN} / {@value BaseWSTest#PWD}.</p>
 */
public class MessagesTrlWebServiceTest extends BaseWSTest {

  /** URL path of the endpoint under test. */
  private static final String WS_PATH = "/ws/messages_trl";

  /** ID of the Core module — always present in every Etendo installation. */
  private static final String CORE_MODULE_ID = TestConstants.Modules.ID_CORE;

  /** Language code for English (base / system language). */
  private static final String LANG_EN_US = "en_US";

  /** Language code for Spanish. */
  private static final String LANG_ES_ES = TestConstants.Languages.ES_ES_ISOCODE;

  /** Query-string parameter prefix for {@code language}. */
  private static final String PARAM_LANGUAGE = "?language=";

  /** Query-string parameter suffix that appends {@code moduleId}. */
  private static final String PARAM_MODULE_ID = "&moduleId=";

  /** JSON field name for the language value in POST bodies. */
  private static final String FIELD_LANGUAGE = "language";

  /** JSON field name for the search-keys array in POST bodies. */
  private static final String FIELD_SEARCH_KEYS = "searchKeys";

  /** Content-Type header value used for POST requests. */
  private static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

  // ---------------------------------------------------------------------------
  // GET — happy path
  // ---------------------------------------------------------------------------

  /**
   * GET with a valid {@code moduleId} and base language must return HTTP 200 and a non-empty
   * JSON object with non-blank values.
   */
  @Test
  public void getBaseLanguageReturnsMessages() throws Exception {
    String response = doGetRequest(PARAM_LANGUAGE + LANG_EN_US + PARAM_MODULE_ID + CORE_MODULE_ID, 200);

    JSONObject json = new JSONObject(response);
    assertThat("Response must contain at least one message", json.length(), greaterThan(0));

    JSONArray keys = json.names();
    assertNotNull("Response must have keys", keys);
    for (int i = 0; i < keys.length(); i++) {
      String key = keys.getString(i);
      assertThat("Message text for key '" + key + "' must not be empty",
          json.getString(key), not(equalTo("")));
    }
  }

  /** GET with {@code es_ES} and Core module must return HTTP 200 and parseable JSON. */
  @Test
  public void getTranslatedLanguageReturnsValidJson() throws Exception {
    if (!isLanguageInstalled()) {
      return;
    }
    String response = doGetRequest(PARAM_LANGUAGE + LANG_ES_ES + PARAM_MODULE_ID + CORE_MODULE_ID, 200);
    new JSONObject(response); // must parse without error
  }

  // ---------------------------------------------------------------------------
  // GET — validation errors
  // ---------------------------------------------------------------------------

  /** A GET without {@code moduleId} must return HTTP 400. */
  @Test
  public void getMissingModuleIdReturns400() throws Exception {
    doGetRequest(PARAM_LANGUAGE + LANG_EN_US, 400);
  }

  /** A GET without {@code language} must return HTTP 400. */
  @Test
  public void getMissingLanguageReturns400() throws Exception {
    doGetRequest("?moduleId=" + CORE_MODULE_ID, 400);
  }

  /** A GET with a non-existent language must return HTTP 404. */
  @Test
  public void getUnknownLanguageReturns404() throws Exception {
    doGetRequest(PARAM_LANGUAGE + "xx_XX" + PARAM_MODULE_ID + CORE_MODULE_ID, 404);
  }

  /** A GET with a non-existent module ID must return HTTP 404. */
  @Test
  public void getUnknownModuleReturns404() throws Exception {
    doGetRequest(PARAM_LANGUAGE + LANG_EN_US + PARAM_MODULE_ID + "DOES_NOT_EXIST", 404);
  }

  // ---------------------------------------------------------------------------
  // GET — module language branch
  // ---------------------------------------------------------------------------

  /**
   * When the requested language matches the Core module's own language, the endpoint reads from
   * {@code AD_Message} directly. Both branches must return the same number of messages.
   */
  @Test
  public void getWithModuleLanguageUsesBaseLanguageBranch() throws Exception {
    String moduleLanguage = fetchModuleLanguage();
    if (moduleLanguage == null) {
      return;
    }

    JSONObject jsonModuleLang = new JSONObject(doGetRequest(
        PARAM_LANGUAGE + moduleLanguage + PARAM_MODULE_ID + CORE_MODULE_ID, 200));
    JSONObject jsonEnUs = new JSONObject(doGetRequest(
        PARAM_LANGUAGE + LANG_EN_US + PARAM_MODULE_ID + CORE_MODULE_ID, 200));

    assertEquals("Both branches should return the same number of messages",
        jsonEnUs.length(), jsonModuleLang.length());
  }

  // ---------------------------------------------------------------------------
  // POST — happy path
  // ---------------------------------------------------------------------------

  /** POST with {@code en_US} and known Core keys must return HTTP 200 with non-blank values. */
  @Test
  public void postBaseLanguageWithKnownKeysReturnsMessages() throws Exception {
    List<String> keys = fetchSomeMessageKeys(5);
    if (keys.isEmpty()) {
      return;
    }

    JSONObject json = new JSONObject(
        doPostRequest(buildPostBody(LANG_EN_US, keys).toString(), 200));

    assertThat("Response must contain at least one entry", json.length(), greaterThan(0));
    for (String key : keys) {
      if (json.has(key)) {
        assertThat("Text for '" + key + "' must not be empty",
            json.getString(key), not(equalTo("")));
      }
    }
  }

  /**
   * POST with {@code es_ES} must return every base-language key, using translated text when
   * available and the base text as fallback. Skipped when Spanish is not installed.
   */
  @Test
  public void postTranslatedLanguageFallsBackToBaseTextForMissingTranslations() throws Exception {
    if (!isLanguageInstalled()) {
      return;
    }

    List<String> keys = fetchSomeMessageKeys(10);
    if (keys.isEmpty()) {
      return;
    }

    JSONObject baseResponse = new JSONObject(
        doPostRequest(buildPostBody(LANG_EN_US, keys).toString(), 200));
    JSONObject trlResponse = new JSONObject(
        doPostRequest(buildPostBody(LANG_ES_ES, keys).toString(), 200));

    JSONArray baseKeys = baseResponse.names();
    if (baseKeys == null) {
      return;
    }
    for (int i = 0; i < baseKeys.length(); i++) {
      String key = baseKeys.getString(i);
      assertTrue("Key '" + key + "' must be present in translated response (fallback expected)",
          trlResponse.has(key));
      assertThat("Text for '" + key + "' must not be empty in translated response",
          trlResponse.getString(key), not(equalTo("")));
    }
  }

  // ---------------------------------------------------------------------------
  // POST — validation errors
  // ---------------------------------------------------------------------------

  /** A POST with an empty body must return HTTP 400. */
  @Test
  public void postEmptyBodyReturns400() throws Exception {
    doPostRequest("", 400);
  }

  /** A POST with a missing {@code language} field must return HTTP 400. */
  @Test
  public void postMissingLanguageReturns400() throws Exception {
    JSONObject body = new JSONObject();
    body.put(FIELD_SEARCH_KEYS, new JSONArray(List.of("Error")));
    doPostRequest(body.toString(), 400);
  }

  /** A POST with a missing {@code searchKeys} field must return HTTP 400. */
  @Test
  public void postMissingSearchKeysReturns400() throws Exception {
    JSONObject body = new JSONObject();
    body.put(FIELD_LANGUAGE, LANG_EN_US);
    doPostRequest(body.toString(), 400);
  }

  /** A POST with an empty {@code searchKeys} array must return HTTP 400. */
  @Test
  public void postEmptySearchKeysArrayReturns400() throws Exception {
    JSONObject body = new JSONObject();
    body.put(FIELD_LANGUAGE, LANG_EN_US);
    body.put(FIELD_SEARCH_KEYS, new JSONArray());
    doPostRequest(body.toString(), 400);
  }

  /** A POST with a non-existent language must return HTTP 404. */
  @Test
  public void postUnknownLanguageReturns404() throws Exception {
    doPostRequest(buildPostBody("xx_XX", List.of("Error")).toString(), 404);
  }

  /**
   * POST with unknown keys must return HTTP 200 with an empty JSON object.
   */
  @Test
  public void postUnknownSearchKeysReturnsEmptyObject() throws Exception {
    JSONObject body = buildPostBody(LANG_EN_US,
        List.of("ZZZZ_NONEXISTENT_KEY_ABC", "ZZZZ_NONEXISTENT_KEY_XYZ"));
    JSONObject json = new JSONObject(doPostRequest(body.toString(), 200));
    assertEquals("Response must be an empty object for unknown keys", 0, json.length());
  }

  // ---------------------------------------------------------------------------
  // HTTP helpers
  // ---------------------------------------------------------------------------

  private String doGetRequest(String queryString, int expectedStatus) throws Exception {
    String path = WS_PATH + queryString;
    HttpURLConnection hc = createConnection(path, "GET");
    hc.connect();
    assertEquals("Unexpected HTTP status for GET " + path, expectedStatus, hc.getResponseCode());
    return readBody(hc);
  }

  private String doPostRequest(String jsonBody, int expectedStatus) throws Exception {
    HttpURLConnection hc = createConnection(WS_PATH, "POST");
    hc.setRequestProperty("Content-Type", CONTENT_TYPE_JSON);
    try (OutputStream os = hc.getOutputStream()) {
      os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
      os.flush();
    }
    hc.connect();
    assertEquals("Unexpected HTTP status for POST " + WS_PATH, expectedStatus, hc.getResponseCode());
    return readBody(hc);
  }

  /** Reads the response body; falls back to the error stream for 4xx/5xx responses. */
  private String readBody(HttpURLConnection hc) throws IOException {
    InputStream is;
    try {
      is = hc.getInputStream();
    } catch (IOException e) {
      is = hc.getErrorStream();
    }
    if (is == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(is, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }
    }
    return sb.toString();
  }

  // ---------------------------------------------------------------------------
  // DAL helpers
  // ---------------------------------------------------------------------------

  /** Returns up to {@code limit} {@code AD_Message} search keys from the Core module. */
  private List<String> fetchSomeMessageKeys(int limit) {
    try {
      OBContext.setAdminMode(false);
      Module module = OBDal.getInstance().get(Module.class, CORE_MODULE_ID);
      if (module == null) {
        return List.of();
      }
      OBCriteria<Message> crit = OBDal.getInstance().createCriteria(Message.class);
      crit.add(Restrictions.eq(Message.PROPERTY_MODULE, module));
      crit.setMaxResults(limit);
      List<String> keys = new ArrayList<>();
      for (Message msg : crit.list()) {
        keys.add(msg.getSearchKey());
      }
      return keys;
    } catch (Exception e) {
      throw new OBException("Could not fetch message keys for module " + CORE_MODULE_ID, e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns the {@code AD_Language.ad_language} code of the Core module's own language,
   * or {@code null} when it cannot be resolved.
   */
  private String fetchModuleLanguage() {
    try {
      OBContext.setAdminMode(false);
      Module module = OBDal.getInstance().get(Module.class, CORE_MODULE_ID);
      if (module == null || module.getLanguage() == null) {
        return null;
      }
      return module.getLanguage().getLanguage();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /** Returns {@code true} when a {@code Language} row exists for {@link #LANG_ES_ES}. */
  private boolean isLanguageInstalled() {
    try {
      OBContext.setAdminMode(false);
      OBCriteria<Language> crit = OBDal.getInstance().createCriteria(Language.class);
      crit.add(Restrictions.eq(Language.PROPERTY_LANGUAGE, LANG_ES_ES));
      crit.setMaxResults(1);
      return crit.uniqueResult() != null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /** Builds a POST body with {@code language} and {@code searchKeys} fields. */
  private JSONObject buildPostBody(String language, List<String> searchKeys) throws Exception {
    JSONObject body = new JSONObject();
    body.put(FIELD_LANGUAGE, language);
    JSONArray keysArray = new JSONArray();
    for (String key : searchKeys) {
      keysArray.put(key);
    }
    body.put(FIELD_SEARCH_KEYS, keysArray);
    return body;
  }
}
