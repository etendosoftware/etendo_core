/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.client.application;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.CsrfUtil;
import org.openbravo.model.ad.utility.Image;
import org.openbravo.service.json.JsonUtils;

/**
 * Tests for {@link DeleteImageActionHandler}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class DeleteImageActionHandlerTest {

  private DeleteImageActionHandler handler;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private RequestContext mockRequestContext;

  @Mock
  private HttpServletRequest mockHttpRequest;

  @Mock
  private Image mockImage;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<RequestContext> requestContextStatic;
  private MockedStatic<CsrfUtil> csrfUtilStatic;
  private MockedStatic<JsonUtils> jsonUtilsStatic;

  @Before
  public void setUp() {
    handler = new DeleteImageActionHandler();

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obContextStatic = mockStatic(OBContext.class);

    requestContextStatic = mockStatic(RequestContext.class);
    requestContextStatic.when(RequestContext::get).thenReturn(mockRequestContext);
    lenient().when(mockRequestContext.getRequest()).thenReturn(mockHttpRequest);

    csrfUtilStatic = mockStatic(CsrfUtil.class);

    jsonUtilsStatic = mockStatic(JsonUtils.class);
  }

  @After
  public void tearDown() {
    if (jsonUtilsStatic != null) {
      jsonUtilsStatic.close();
    }
    if (csrfUtilStatic != null) {
      csrfUtilStatic.close();
    }
    if (requestContextStatic != null) {
      requestContextStatic.close();
    }
    if (obContextStatic != null) {
      obContextStatic.close();
    }
    if (obDalStatic != null) {
      obDalStatic.close();
    }
  }

  @Test
  public void testExecuteDeletesExistingImage() throws Exception {
    String imageId = "IMG123";
    when(mockOBDal.get(Image.class, imageId)).thenReturn(mockImage);

    Map<String, Object> parameters = new HashMap<>();
    String content = new JSONObject().put("img", imageId).put("csrfToken", "token123").toString();

    JSONObject result = handler.execute(parameters, content);

    assertNotNull(result);
    verify(mockOBDal).remove(mockImage);
    obContextStatic.verify(() -> OBContext.restorePreviousMode());
  }

  @Test
  public void testExecuteDoesNotRemoveWhenImageNotFound() throws Exception {
    String imageId = "NONEXISTENT";
    when(mockOBDal.get(Image.class, imageId)).thenReturn(null);

    Map<String, Object> parameters = new HashMap<>();
    String content = new JSONObject().put("img", imageId).put("csrfToken", "token123").toString();

    JSONObject result = handler.execute(parameters, content);

    assertNotNull(result);
    verify(mockOBDal, never()).remove(any());
  }

  @Test
  public void testExecuteHandlesMissingCsrfToken() throws Exception {
    String imageId = "IMG123";
    when(mockOBDal.get(Image.class, imageId)).thenReturn(mockImage);

    Map<String, Object> parameters = new HashMap<>();
    String content = new JSONObject().put("img", imageId).toString();

    JSONObject result = handler.execute(parameters, content);

    assertNotNull(result);
    csrfUtilStatic.verify(() -> CsrfUtil.checkCsrfToken(eq(""), any()));
  }

  @Test
  public void testExecuteReturnsErrorJsonOnException() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String content = "invalid json{{{";

    lenient().when(JsonUtils.convertExceptionToJson(any(Exception.class))).thenReturn("{}");
    jsonUtilsStatic.when(() -> JsonUtils.convertExceptionToJson(any(Exception.class)))
        .thenReturn("{}");

    JSONObject result = handler.execute(parameters, content);

    assertNotNull(result);
  }
}
