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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Message;
import org.openbravo.model.ad.ui.MessageTrl;

/**
 * Unit tests for {@link MessagesTrlWebService}.
 * Uses Mockito to mock DAL and servlet layers — no running Tomcat required.
 */
@RunWith(MockitoJUnitRunner.class)
public class MessagesTrlWebServiceTest {

  private static final String LANG_EN_US = "en_US";
  private static final String LANG_ES_ES = "es_ES";
  private static final String MODULE_ID = "0";
  private static final String LANG_ID = "192";
  private static final String LANG_ES_ID = "140";
  private static final String PARAM_LANGUAGE = "language";
  private static final String PARAM_MODULE_ID = "moduleId";
  private static final String PARAM_SEARCH_KEYS = "searchKeys";
  private static final String MSG_KEY_SUCCESS = "SMFWSM_Success";
  private static final String MSG_KEY_ERROR = "Error";
  private static final String MSG_TEXT_SUCCESS = "Success";
  private static final String MSG_TEXT_ERROR = "Error occurred";
  private static final String MSG_TEXT_EXITO = "Exito";

  @Mock private HttpServletRequest mockRequest;
  @Mock private HttpServletResponse mockResponse;
  @Mock private OBDal mockOBDal;
  @Mock private OBContext mockOBContext;
  @Mock private Language mockLanguage;
  @Mock private Language mockContextLanguage;
  @Mock private Language mockEsLanguage;
  @Mock private Module mockModule;
  @Mock private Message mockMessage1;
  @Mock private Message mockMessage2;
  @Mock private MessageTrl mockMessageTrl1;

  private MockedStatic<OBDal> mockedOBDalStatic;
  private MockedStatic<OBContext> mockedOBContextStatic;

  private MessagesTrlWebService service;
  private StringWriter responseStringWriter;

  @SuppressWarnings("unchecked")
  private OBCriteria<Language> mockLangCriteria;
  @SuppressWarnings("unchecked")
  private OBCriteria<Message> mockMsgCriteria;
  @SuppressWarnings("unchecked")
  private OBCriteria<MessageTrl> mockTrlCriteria;

  /**
   * Initializes mocks for DAL, OBContext, criteria and entity objects.
   */
  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    service = new MessagesTrlWebService();
    responseStringWriter = new StringWriter();
    when(mockResponse.getWriter()).thenReturn(new PrintWriter(responseStringWriter));

    mockedOBDalStatic = mockStatic(OBDal.class);
    mockedOBDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    mockedOBContextStatic = mockStatic(OBContext.class);
    mockedOBContextStatic.when(OBContext::getOBContext).thenReturn(mockOBContext);
    when(mockOBContext.getLanguage()).thenReturn(mockContextLanguage);
    when(mockContextLanguage.getLanguage()).thenReturn(LANG_EN_US);

    mockLangCriteria = mock(OBCriteria.class);
    mockMsgCriteria = mock(OBCriteria.class);
    mockTrlCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(eq(Language.class))).thenReturn(mockLangCriteria);
    when(mockOBDal.createCriteria(eq(Message.class))).thenReturn(mockMsgCriteria);
    when(mockOBDal.createCriteria(eq(MessageTrl.class))).thenReturn(mockTrlCriteria);
    when(mockTrlCriteria.createAlias(any(String.class), any(String.class)))
        .thenReturn(mockTrlCriteria);

    when(mockLanguage.getId()).thenReturn(LANG_ID);
    when(mockModule.getLanguage()).thenReturn(mockLanguage);
    when(mockEsLanguage.getId()).thenReturn(LANG_ES_ID);

    when(mockMessage1.getSearchKey()).thenReturn(MSG_KEY_SUCCESS);
    when(mockMessage1.getMessageText()).thenReturn(MSG_TEXT_SUCCESS);
    when(mockMessage2.getSearchKey()).thenReturn(MSG_KEY_ERROR);
    when(mockMessage2.getMessageText()).thenReturn(MSG_TEXT_ERROR);

    when(mockMessageTrl1.getMessage()).thenReturn(mockMessage1);
    when(mockMessageTrl1.getMessageText()).thenReturn(MSG_TEXT_EXITO);
  }

  /**
   * Closes static mocks to avoid leaks between tests.
   */
  @After
  public void tearDown() {
    if (mockedOBDalStatic != null) {
      mockedOBDalStatic.close();
    }
    if (mockedOBContextStatic != null) {
      mockedOBContextStatic.close();
    }
  }

  // ---------------------------------------------------------------------------
  // GET
  // ---------------------------------------------------------------------------

  /**
   * GET with base language and valid module returns a JSON object with message entries.
   */
  @Test
  public void getBaseLanguageReturnsMessages() throws Exception {
    when(mockRequest.getParameter(PARAM_LANGUAGE)).thenReturn(LANG_EN_US);
    when(mockRequest.getParameter(PARAM_MODULE_ID)).thenReturn(MODULE_ID);
    when(mockLangCriteria.uniqueResult()).thenReturn(mockLanguage);
    when(mockOBDal.get(Module.class, MODULE_ID)).thenReturn(mockModule);
    when(mockMsgCriteria.list()).thenReturn(List.of(mockMessage1, mockMessage2));

    service.doGet("", mockRequest, mockResponse);

    JSONObject json = new JSONObject(responseStringWriter.toString());
    assertEquals(MSG_TEXT_SUCCESS, json.optString(MSG_KEY_SUCCESS));
    assertEquals(MSG_TEXT_ERROR, json.optString(MSG_KEY_ERROR));
  }

  /**
   * GET with a non-base language uses the AD_Message_Trl translation branch.
   */
  @Test
  public void getWithTranslationBranch() throws Exception {
    when(mockRequest.getParameter(PARAM_LANGUAGE)).thenReturn(LANG_ES_ES);
    when(mockRequest.getParameter(PARAM_MODULE_ID)).thenReturn(MODULE_ID);
    when(mockLangCriteria.uniqueResult()).thenReturn(mockEsLanguage);
    when(mockOBDal.get(Module.class, MODULE_ID)).thenReturn(mockModule);
    when(mockTrlCriteria.list()).thenReturn(List.of(mockMessageTrl1));

    service.doGet("", mockRequest, mockResponse);

    JSONObject json = new JSONObject(responseStringWriter.toString());
    assertEquals(MSG_TEXT_EXITO, json.optString(MSG_KEY_SUCCESS));
  }

  /**
   * GET without moduleId parameter returns HTTP 400.
   */
  @Test
  public void getMissingModuleIdReturns400() throws Exception {
    when(mockRequest.getParameter(PARAM_LANGUAGE)).thenReturn(LANG_EN_US);
    when(mockRequest.getParameter(PARAM_MODULE_ID)).thenReturn(null);

    service.doGet("", mockRequest, mockResponse);

    verify(mockResponse).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  /**
   * GET with a language code that does not exist in the database returns HTTP 404.
   */
  @Test
  public void getUnknownLanguageReturns404() throws Exception {
    when(mockRequest.getParameter(PARAM_LANGUAGE)).thenReturn("xx_XX");
    when(mockRequest.getParameter(PARAM_MODULE_ID)).thenReturn(MODULE_ID);
    when(mockLangCriteria.uniqueResult()).thenReturn(null);

    service.doGet("", mockRequest, mockResponse);

    verify(mockResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
  }

  /**
   * GET with a module ID that does not exist in the database returns HTTP 404.
   */
  @Test
  public void getUnknownModuleReturns404() throws Exception {
    when(mockRequest.getParameter(PARAM_LANGUAGE)).thenReturn(LANG_EN_US);
    when(mockRequest.getParameter(PARAM_MODULE_ID)).thenReturn("DOES_NOT_EXIST");
    when(mockLangCriteria.uniqueResult()).thenReturn(mockLanguage);
    when(mockOBDal.get(Module.class, "DOES_NOT_EXIST")).thenReturn(null);

    service.doGet("", mockRequest, mockResponse);

    verify(mockResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
  }

  // ---------------------------------------------------------------------------
  // POST
  // ---------------------------------------------------------------------------

  /**
   * POST falls back to AD_Message base text when a translation record is missing.
   */
  @Test
  public void postFallsBackToBaseTextForMissingTranslations() throws Exception {
    JSONObject body = new JSONObject();
    body.put(PARAM_LANGUAGE, LANG_ES_ES);
    body.put(PARAM_SEARCH_KEYS, buildJsonArray(MSG_KEY_SUCCESS, MSG_KEY_ERROR));
    mockPostBody(body.toString());

    when(mockLangCriteria.uniqueResult()).thenReturn(mockEsLanguage);
    when(mockTrlCriteria.list()).thenReturn(List.of(mockMessageTrl1));
    when(mockMsgCriteria.list()).thenReturn(List.of(mockMessage2));

    service.doPost("", mockRequest, mockResponse);

    JSONObject json = new JSONObject(responseStringWriter.toString());
    assertEquals(MSG_TEXT_EXITO, json.optString(MSG_KEY_SUCCESS));
    assertEquals(MSG_TEXT_ERROR, json.optString(MSG_KEY_ERROR));
  }

  /**
   * POST with an empty request body returns HTTP 400.
   */
  @Test
  public void postEmptyBodyReturns400() throws Exception {
    mockPostBody("");

    service.doPost("", mockRequest, mockResponse);

    verify(mockResponse).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  /**
   * POST without the searchKeys field returns HTTP 400.
   */
  @Test
  public void postMissingSearchKeysReturns400() throws Exception {
    JSONObject body = new JSONObject();
    body.put(PARAM_LANGUAGE, LANG_EN_US);
    mockPostBody(body.toString());

    service.doPost("", mockRequest, mockResponse);

    verify(mockResponse).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  // ---------------------------------------------------------------------------
  // PUT / DELETE
  // ---------------------------------------------------------------------------

  /**
   * PUT requests are not supported and must return HTTP 405.
   */
  @Test
  public void putReturns405() throws Exception {
    service.doPut("", mockRequest, mockResponse);
    verify(mockResponse).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
  }

  /**
   * DELETE requests are not supported and must return HTTP 405.
   */
  @Test
  public void deleteReturns405() throws Exception {
    service.doDelete("", mockRequest, mockResponse);
    verify(mockResponse).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private void mockPostBody(String jsonBody) throws Exception {
    when(mockRequest.getReader()).thenReturn(
        new BufferedReader(new StringReader(jsonBody)));
  }

  private JSONArray buildJsonArray(String... values) throws Exception {
    JSONArray array = new JSONArray();
    for (String value : values) {
      array.put(value);
    }
    return array;
  }
}
