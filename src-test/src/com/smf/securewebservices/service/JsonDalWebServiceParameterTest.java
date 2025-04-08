package com.smf.securewebservices.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.util.CheckException;
import org.openbravo.service.json.JsonConstants;

import com.smf.securewebservices.TestingConstants;

/**
 * Unit tests for JsonDalWebService parameter validation methods.
 * These tests verify the correct handling of entity names, IDs and parameters
 * in the JsonDalWebService class.
 */
public class JsonDalWebServiceParameterTest {

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private HttpServletResponse mockResponse;

  @Mock
  private ModelProvider mockModelProvider;

  private JsonDalWebService jsonDalWebService;
  private StringWriter stringWriter;

  /**
   * Sets up the test environment before each test.
   * Initialize mocks and prepares the response writer.
   *
   * @throws IOException
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.openMocks(this);
    jsonDalWebService = new JsonDalWebService();

    // Set up response writer
    stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    when(mockResponse.getWriter()).thenReturn(printWriter);
  }

  /**
   * Tests the checkSetParameters method with a valid entity.
   * Verifies that the method correctly processes and sets parameters for a valid entity.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testCheckSetParametersValidEntity() throws Exception {
    // Given
    String path = TestingConstants.VALID_ENTITY;
    Map<String, String> parameters = new HashMap<>();
    Entity mockEntity = mock(Entity.class);

    try (var mocked = mockStatic(ModelProvider.class)) {
      mocked.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
      when(mockModelProvider.getEntity(TestingConstants.VALID_ENTITY)).thenReturn(mockEntity);

      // When
      boolean result = invokeCheckSetParameters(path, mockRequest, mockResponse, parameters);

      // Then
      assertTrue(result);
      assertEquals(TestingConstants.VALID_ENTITY, parameters.get(JsonConstants.ENTITYNAME));
    }
  }

  /**
   * Tests the checkSetParameters method with a valid entity and ID.
   * Verifies that the method correctly processes and sets parameters for both entity and ID.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testCheckSetParametersValidEntityAndId() throws Exception {
    // Given
    String path = "validEntity/123";
    Map<String, String> parameters = new HashMap<>();
    Entity mockEntity = mock(Entity.class);

    try (var mocked = mockStatic(ModelProvider.class)) {
      mocked.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
      when(mockModelProvider.getEntity(TestingConstants.VALID_ENTITY)).thenReturn(mockEntity);

      // When
      boolean result = invokeCheckSetParameters(path, mockRequest, mockResponse, parameters);

      // Then
      assertTrue(result);
      assertEquals(TestingConstants.VALID_ENTITY, parameters.get(JsonConstants.ENTITYNAME));
      assertEquals("123", parameters.get(JsonConstants.ID));
      assertEquals(JsonConstants.TEXTMATCH_EXACT, parameters.get(JsonConstants.TEXTMATCH_PARAMETER));
      assertEquals(JsonConstants.TEXTMATCH_EXACT, parameters.get(JsonConstants.TEXTMATCH_PARAMETER_OVERRIDE));
    }
  }

  /**
   * Tests the checkSetParameters method with an invalid entity.
   * Verifies that the method handles invalid entities appropriately.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testCheckSetParametersInvalidEntity() throws Exception {
    // Given
    String path = TestingConstants.INVALID_ENTITY;
    Map<String, String> parameters = new HashMap<>();

    try (var mocked = mockStatic(ModelProvider.class)) {
      mocked.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
      when(mockModelProvider.getEntity(TestingConstants.INVALID_ENTITY)).thenThrow(new CheckException("Entity not found"));

      // When
      boolean result = invokeCheckSetParameters(path, mockRequest, mockResponse, parameters);

      // Then
      assertFalse(result);
      assertTrue(stringWriter.toString().contains("Invalid url, no entity found"));
    }
  }

  /**
   * Tests the checkSetIDEntityName method with a valid entity.
   * Verifies that the method correctly processes and sets the entity name.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testCheckSetIDEntityNameValidEntity() throws Exception {
    // Given
    String path = TestingConstants.VALID_ENTITY;
    Map<String, String> parameters = new HashMap<>();
    Entity mockEntity = mock(Entity.class);

    try (var mocked = mockStatic(ModelProvider.class)) {
      mocked.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
      when(mockModelProvider.getEntity(TestingConstants.VALID_ENTITY)).thenReturn(mockEntity);

      // When
      boolean result = invokeCheckSetIDEntityName(path, mockRequest, mockResponse, parameters);

      // Then
      assertTrue(result);
      assertEquals(TestingConstants.VALID_ENTITY, parameters.get(JsonConstants.ENTITYNAME));
    }
  }

  /**
   * Tests the checkSetIDEntityName method with an invalid entity.
   * Verifies that the method handles invalid entities appropriately.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testCheckSetIDEntityNameInvalidEntity() throws Exception {
    // Given
    String path = TestingConstants.INVALID_ENTITY;
    Map<String, String> parameters = new HashMap<>();

    try (var mocked = mockStatic(ModelProvider.class)) {
      mocked.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
      when(mockModelProvider.getEntity(TestingConstants.INVALID_ENTITY)).thenThrow(new CheckException("Entity not found"));

      // When
      boolean result = invokeCheckSetIDEntityName(path, mockRequest, mockResponse, parameters);

      // Then
      assertFalse(result);
      assertTrue(stringWriter.toString().contains("Invalid url, no entity found"));
    }
  }

  /**
   * Helper method to invoke the private checkSetParameters method through reflection.
   *
   * @param path
   *     the URL path to process
   * @param request
   *     the HTTP servlet request
   * @param response
   *     the HTTP servlet response
   * @param parameters
   *     the map to store the processed parameters
   * @return boolean indicating success or failure of the operation
   * @throws Exception
   *     if an error occurs during method invocation
   */
  private boolean invokeCheckSetParameters(String path, HttpServletRequest request, HttpServletResponse response,
      Map<String, String> parameters) throws Exception {
    Method method = JsonDalWebService.class.getDeclaredMethod("checkSetParameters", String.class,
        HttpServletRequest.class, HttpServletResponse.class, Map.class);
    method.setAccessible(true);
    return (boolean) method.invoke(jsonDalWebService, path, request, response, parameters);
  }

  /**
   * Helper method to invoke the private checkSetIDEntityName method through reflection.
   *
   * @param path
   *     the URL path to process
   * @param request
   *     the HTTP servlet request
   * @param response
   *     the HTTP servlet response
   * @param parameters
   *     the map to store the processed parameters
   * @return boolean indicating success or failure of the operation
   * @throws Exception
   *     if an error occurs during method invocation
   */
  private boolean invokeCheckSetIDEntityName(String path, HttpServletRequest request, HttpServletResponse response,
      Map<String, String> parameters) throws Exception {
    Method method = JsonDalWebService.class.getDeclaredMethod("checkSetIDEntityName", String.class,
        HttpServletRequest.class, HttpServletResponse.class, Map.class);
    method.setAccessible(true);
    return (boolean) method.invoke(jsonDalWebService, path, request, response, parameters);
  }
}
