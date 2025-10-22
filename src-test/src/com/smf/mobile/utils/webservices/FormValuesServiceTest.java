package com.smf.mobile.utils.webservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Tab;

import com.smf.securewebservices.rsql.OBRestUtils;
import com.smf.securewebservices.utils.SecureWebServicesUtils;
import com.smf.securewebservices.utils.WSResult;

/**
 * Test class for FormValuesService.
 */
@RunWith(MockitoJUnitRunner.class)
public class FormValuesServiceTest {

  private FormValuesService service;

  @Mock
  private Entity mockEntity;

  @Mock
  private Tab mockTab;

  @Mock
  private ModelProvider modelProviderInstance;

  @Mock
  private OBDal obDalInstance;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  /**
   * Sets up the test environment before each test.
   * Initializes the FormValuesService instance.
   */
  @Before
  public void setup() {
    service = new FormValuesService();
  }

  /**
   * Test that the get method returns an error if the entity is not found.
   *
   * @see FormValuesService#get(String, Map)
   */
  @Test
  public void testGetEntityNotFound() {
    Map<String, String> params = new HashMap<>();
    params.put(WBUtils.TAB_ID, WBUtils.TAB_123);
    params.put(WBUtils.PARENT_ID, WBUtils.PARENT_456);
    params.put(WBUtils.PARENT_ENTITY, WBUtils.INEXISTENT_ENTITY);

    try (MockedStatic<OBContext> obContext = mockStatic(
        OBContext.class); MockedStatic<ModelProvider> modelProvider = mockStatic(
        ModelProvider.class); MockedStatic<OBMessageUtils> messageUtils = mockStatic(OBMessageUtils.class)) {
      modelProvider.when(ModelProvider::getInstance).thenReturn(modelProviderInstance);
      when(modelProviderInstance.getEntity(WBUtils.INEXISTENT_ENTITY)).thenReturn(null);

      messageUtils.when(() -> OBMessageUtils.getI18NMessage(eq("SMFMU_EntityNotFound"), any())).thenReturn(
          "Entity InexistentEntity not found");

      WSResult result = service.get(WBUtils.ENDPOINT, params);

      assertEquals(WSResult.Status.INTERNAL_SERVER_ERROR, result.getStatus());
      assertEquals(WSResult.ResultType.SINGLE, result.getResultType());
      assertTrue(result.getMessage().contains(WBUtils.INEXISTENT_ENTITY));
    }
  }

  /**
   * Test that the doGet method calls the get method and writes the response.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoGetCallsGetAndWritesResponse() throws Exception {
    Map<String, String> mockParams = Map.of(WBUtils.TAB_ID, WBUtils.TAB_123, WBUtils.PARENT_ID, WBUtils.PARENT_456, WBUtils.PARENT_ENTITY, WBUtils.SOME_ENTITY);

    WSResult mockResult = new WSResult();
    mockResult.setStatus(WSResult.Status.OK);
    mockResult.setResultType(WSResult.ResultType.SINGLE);

    try (MockedStatic<OBContext> obContext = mockStatic(
        OBContext.class); MockedStatic<SecureWebServicesUtils> secUtils = mockStatic(
        SecureWebServicesUtils.class); MockedStatic<OBRestUtils> obRestUtils = mockStatic(
        OBRestUtils.class); MockedStatic<ModelProvider> modelProvider = mockStatic(
        ModelProvider.class); MockedStatic<OBDal> obDal = mockStatic(
        OBDal.class); MockedStatic<WindowUtils> windowUtils = mockStatic(WindowUtils.class)) {

      secUtils.when(() -> SecureWebServicesUtils.fillSessionVariables(any())).thenAnswer(inv -> null);

      obRestUtils.when(() -> OBRestUtils.requestParamsToMap(any())).thenReturn(mockParams);

      JSONObject jsonData = new JSONObject(Map.of("field1", "value1"));

      modelProvider.when(ModelProvider::getInstance).thenReturn(modelProviderInstance);
      when(modelProviderInstance.getEntity(WBUtils.SOME_ENTITY)).thenReturn(mockEntity);

      obDal.when(OBDal::getInstance).thenReturn(obDalInstance);
      when(obDalInstance.get(Tab.class, WBUtils.TAB_123)).thenReturn(mockTab);

      windowUtils.when(() -> WindowUtils.computeColumnValues(mockTab, WBUtils.PARENT_456, mockEntity, null)).thenReturn(
          jsonData);

      obRestUtils.when(() -> OBRestUtils.writeWSResponse(any(WSResult.class), any())).thenAnswer(invocation -> {
        WSResult result = invocation.getArgument(0);
        assertEquals(WSResult.Status.OK, result.getStatus());
        return null;
      });

      service.doGet(WBUtils.ENDPOINT, request, response);

      obRestUtils.verify(() -> OBRestUtils.writeWSResponse(any(WSResult.class), eq(response)));
    }
  }

  /**
   * Tests that the post method returns the computed column values if the entity is found.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testPostSuccess() throws Exception {
    Map<String, String> params = Map.of(WBUtils.TAB_ID, WBUtils.TAB_123, WBUtils.PARENT_ID, WBUtils.PARENT_456, WBUtils.PARENT_ENTITY, WBUtils.SOME_ENTITY);

    JSONObject contextJson = new JSONObject(Map.of("someCtx", "val"));
    JSONObject body = new JSONObject();
    body.put("context", contextJson);

    JSONObject expectedData = new JSONObject(Map.of("field1", "value1"));

    try (MockedStatic<OBContext> obContext = mockStatic(
        OBContext.class); MockedStatic<ModelProvider> modelProvider = mockStatic(
        ModelProvider.class); MockedStatic<OBDal> obDal = mockStatic(
        OBDal.class); MockedStatic<WindowUtils> windowUtils = mockStatic(WindowUtils.class)) {
      modelProvider.when(ModelProvider::getInstance).thenReturn(modelProviderInstance);
      when(modelProviderInstance.getEntity(WBUtils.SOME_ENTITY)).thenReturn(mockEntity);

      obDal.when(OBDal::getInstance).thenReturn(obDalInstance);
      when(obDalInstance.get(Tab.class, WBUtils.TAB_123)).thenReturn(mockTab);

      windowUtils.when(() -> WindowUtils.computeColumnValues(mockTab, WBUtils.PARENT_456, mockEntity, contextJson)).thenReturn(
          expectedData);

      WSResult result = service.post(WBUtils.ENDPOINT, params, body);

      assertEquals(WSResult.Status.OK, result.getStatus());
      assertEquals(WSResult.ResultType.SINGLE, result.getResultType());
    }
  }

  /**
   * Tests that the post method returns an error if the entity is not found.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testPostEntityNotFound() throws Exception {
    Map<String, String> params = Map.of(WBUtils.TAB_ID, WBUtils.TAB_123, WBUtils.PARENT_ID, WBUtils.PARENT_456, WBUtils.PARENT_ENTITY, "MissingEntity");

    JSONObject body = new JSONObject();
    body.put("context", new JSONObject());

    try (MockedStatic<OBContext> obContext = mockStatic(
        OBContext.class); MockedStatic<ModelProvider> modelProvider = mockStatic(
        ModelProvider.class); MockedStatic<OBMessageUtils> obMessageUtils = mockStatic(OBMessageUtils.class)) {
      modelProvider.when(ModelProvider::getInstance).thenReturn(modelProviderInstance);
      when(modelProviderInstance.getEntity("MissingEntity")).thenReturn(null);

      obMessageUtils.when(() -> OBMessageUtils.getI18NMessage(eq("SMFMU_EntityNotFound"), any())).thenReturn(
          "Entity MissingEntity not found");

      WSResult result = service.post(WBUtils.ENDPOINT, params, body);

      assertEquals(WSResult.Status.INTERNAL_SERVER_ERROR, result.getStatus());
      assertEquals("Entity MissingEntity not found", result.getMessage());
      assertEquals(WSResult.ResultType.SINGLE, result.getResultType());
    }
  }
}
