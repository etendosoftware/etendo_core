package com.smf.securewebservices.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.authentication.hashing.PasswordHash;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.AllowedCrossDomainsHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;

import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.smf.securewebservices.SWSConfig;
import com.smf.securewebservices.TestingConstants;
import com.smf.securewebservices.utils.SecureWebServicesUtils;

/**
 * Unit tests for the SecureLoginServlet class.
 */
@RunWith(MockitoJUnitRunner.class)
public class SecureLoginServletTest {
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<PasswordHash> mockedPasswordHash;
  private MockedStatic<Utility> mockedUtility;
  private MockedStatic<SecureWebServicesUtils> mockedSecureWebServicesUtils;
  private MockedStatic<SWSConfig> mockedSWSConfig;
  private MockedStatic<AllowedCrossDomainsHandler> mockedAllowedCrossDomainsHandler;
  private MockedStatic<OBProvider> mockedOBProvider;

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private HttpServletResponse mockResponse;

  @Mock
  private User mockUser;

  @Mock
  private Role mockRole;

  @Mock
  private Organization mockOrganization;

  @Mock
  private Warehouse mockWarehouse;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBContext mockOBContext;

  @Mock
  private SWSConfig mockSWSConfig;

  @Mock
  private AllowedCrossDomainsHandler mockAllowedCrossDomainsHandler;

  @Mock
  private OBProvider mockOBProvider;

  @Mock
  private DecodedJWT mockDecodedJWT;

  private SecureLoginServlet servletUnderTest;

  private StringWriter responseWriter;

  /**
   * Sets up the test environment before each test.
   *
   * @throws JSONException
   *     if a JSON error occurs
   * @throws IOException
   *     if an I/O error occurs
   */
  @Before
  public void setUp() throws JSONException, IOException {
    servletUnderTest = new SecureLoginServlet();

    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    mockedOBContext = mockStatic(OBContext.class);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);
    mockedOBContext.when(() -> OBContext.setAdminMode(anyBoolean())).thenAnswer(invocation -> null);
    mockedOBContext.when(OBContext::setAdminMode).thenAnswer(invocation -> null);
    mockedOBContext.when(OBContext::restorePreviousMode).thenAnswer(invocation -> null);

    mockedPasswordHash = mockStatic(PasswordHash.class);

    mockedUtility = mockStatic(Utility.class);
    mockedUtility.when(() -> Utility.messageBD(any(), anyString(), anyString())).thenReturn("Mocked error message");

    mockedSecureWebServicesUtils = mockStatic(SecureWebServicesUtils.class);
    mockedSecureWebServicesUtils.when(
        () -> SecureWebServicesUtils.generateToken(any(), any(), any(), any())).thenReturn(TestingConstants.DUMMY_TOKEN);

    JSONArray mockRolesArray = new JSONArray();
    mockedSecureWebServicesUtils.when(
        () -> SecureWebServicesUtils.getUserRolesAndOrg(any(), anyBoolean(), anyBoolean())).thenReturn(mockRolesArray);

    mockedSWSConfig = mockStatic(SWSConfig.class);
    mockedSWSConfig.when(SWSConfig::getInstance).thenReturn(mockSWSConfig);

    // Mock para AllowedCrossDomainsHandler
    mockedAllowedCrossDomainsHandler = mockStatic(AllowedCrossDomainsHandler.class);
    mockedAllowedCrossDomainsHandler.when(AllowedCrossDomainsHandler::getInstance).thenReturn(
        mockAllowedCrossDomainsHandler);
    doNothing().when(mockAllowedCrossDomainsHandler).setCORSHeaders(any(HttpServletRequest.class),
        any(HttpServletResponse.class));

    mockedOBProvider = mockStatic(OBProvider.class);
    mockedOBProvider.when(OBProvider::getInstance).thenReturn(mockOBProvider);

    responseWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(responseWriter);
    when(mockResponse.getWriter()).thenReturn(printWriter);

    when(mockSWSConfig.getPrivateKey()).thenReturn("dummy-key");

    // Mock language
    when(mockOBContext.getLanguage()).thenReturn(mock(Language.class));
    when(mockOBContext.getLanguage().getLanguage()).thenReturn("en_US");

  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedPasswordHash != null) {
      mockedPasswordHash.close();
    }
    if (mockedUtility != null) {
      mockedUtility.close();
    }
    if (mockedSecureWebServicesUtils != null) {
      mockedSecureWebServicesUtils.close();
    }
    if (mockedSWSConfig != null) {
      mockedSWSConfig.close();
    }
    if (mockedAllowedCrossDomainsHandler != null) {
      mockedAllowedCrossDomainsHandler.close();
    }
    if (mockedOBProvider != null) {
      mockedOBProvider.close();
    }
  }

  /**
   * Tests the doPost method when the user is inactive.
   *
   * @throws ServletException
   *     if a servlet error occurs
   * @throws IOException
   *     if an I/O error occurs
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testDoPostUserInactive() throws ServletException, IOException, JSONException {
    // GIVEN
    String username = "inactiveUser";
    String password = "testPassword";

    JSONObject loginJson = new JSONObject();
    loginJson.put(TestingConstants.USERNAME, username);
    loginJson.put(TestingConstants.PASSWORD, password);
    String jsonString = loginJson.toString();

    BufferedReader reader = new BufferedReader(new StringReader(jsonString));
    when(mockRequest.getReader()).thenReturn(reader);

    mockedPasswordHash.when(() -> PasswordHash.getUserWithPassword(eq(username), eq(password))).thenReturn(
        Optional.of(mockUser));

    mockedSecureWebServicesUtils.when(
        () -> SecureWebServicesUtils.generateToken(eq(mockUser), any(), any(), any())).thenThrow(
        new RuntimeException("User is inactive"));

    // WHEN
    servletUnderTest.doPost(mockRequest, mockResponse);

    // THEN
    String responseContent = responseWriter.toString();
    JSONObject jsonResponse = new JSONObject(responseContent);

    assertEquals(TestingConstants.ERROR, jsonResponse.getString(TestingConstants.STATUS));
    assertTrue(jsonResponse.has(TestingConstants.MESSAGE));
  }

  /**
   * Tests the doPost method for a successful login.
   *
   * @throws ServletException
   *     if a servlet error occurs
   * @throws IOException
   *     if an I/O error occurs
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testDoPostSuccessful() throws ServletException, IOException, JSONException {
    // GIVEN
    String username = TestingConstants.VALID_USER;
    String password = TestingConstants.VALID_PASSWORD;

    JSONObject loginJson = new JSONObject();
    loginJson.put(TestingConstants.USERNAME, username);
    loginJson.put(TestingConstants.PASSWORD, password);
    String jsonString = loginJson.toString();

    BufferedReader reader = new BufferedReader(new StringReader(jsonString));
    when(mockRequest.getReader()).thenReturn(reader);

    mockedPasswordHash.when(() -> PasswordHash.getUserWithPassword(eq(username), eq(password))).thenReturn(
        Optional.of(mockUser));

    when(mockRequest.getParameter(TestingConstants.SHOW_ROLES)).thenReturn(null);
    when(mockRequest.getParameter("showOrgs")).thenReturn(null);
    when(mockRequest.getParameter("showWarehouses")).thenReturn(null);

    // WHEN
    servletUnderTest.doPost(mockRequest, mockResponse);

    // THEN
    String responseContent = responseWriter.toString();
    JSONObject jsonResponse = new JSONObject(responseContent);

    assertEquals(TestingConstants.SUCCESS, jsonResponse.getString(TestingConstants.STATUS));
    assertEquals(TestingConstants.DUMMY_TOKEN, jsonResponse.getString(TestingConstants.TOKEN));
    assertTrue(jsonResponse.has(TestingConstants.ROLE_LIST));
  }

  /**
   * Tests the doPost method with role, organization, and warehouse parameters.
   *
   * @throws ServletException
   *     if a servlet error occurs
   * @throws IOException
   *     if an I/O error occurs
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testDoPostWithRoleOrgWarehouse() throws ServletException, IOException, JSONException {
    // GIVEN
    String username = TestingConstants.VALID_USER;
    String password = TestingConstants.VALID_PASSWORD;
    String roleId = "role123";
    String orgId = "org123";
    String warehouseId = "warehouse123";

    JSONObject loginJson = new JSONObject();
    loginJson.put(TestingConstants.USERNAME, username);
    loginJson.put(TestingConstants.PASSWORD, password);
    loginJson.put("role", roleId);
    loginJson.put(TestingConstants.ORGANIZATION, orgId);
    loginJson.put(TestingConstants.WAREHOUSE, warehouseId);
    String jsonString = loginJson.toString();

    BufferedReader reader = new BufferedReader(new StringReader(jsonString));
    when(mockRequest.getReader()).thenReturn(reader);

    mockedPasswordHash.when(() -> PasswordHash.getUserWithPassword(eq(username), eq(password))).thenReturn(
        Optional.of(mockUser));

    when(mockOBDal.get(Role.class, roleId)).thenReturn(mockRole);
    when(mockOBDal.get(Organization.class, orgId)).thenReturn(mockOrganization);
    when(mockOBDal.get(Warehouse.class, warehouseId)).thenReturn(mockWarehouse);

    when(mockRequest.getParameter(TestingConstants.SHOW_ROLES)).thenReturn("false");

    // WHEN
    servletUnderTest.doPost(mockRequest, mockResponse);

    // THEN
    String responseContent = responseWriter.toString();
    JSONObject jsonResponse = new JSONObject(responseContent);

    assertEquals(TestingConstants.SUCCESS, jsonResponse.getString(TestingConstants.STATUS));
    assertEquals(TestingConstants.DUMMY_TOKEN, jsonResponse.getString(TestingConstants.TOKEN));
    assertFalse(jsonResponse.has(TestingConstants.ROLE_LIST));

    // Verify that generateToken was called with the right parameters
    mockedSecureWebServicesUtils.verify(
        () -> SecureWebServicesUtils.generateToken(eq(mockUser), eq(mockRole), eq(mockOrganization), eq(mockWarehouse)),
        times(1));
  }

  /**
   * Tests the doPost method with a valid token.
   *
   * @throws ServletException
   *     if a servlet error occurs
   * @throws IOException
   *     if an I/O error occurs
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testDoPostWithToken() throws ServletException, IOException, JSONException {
    // GIVEN
    String token = "valid-token";

    // Setup request with token
    when(mockRequest.getHeader(TestingConstants.AUTHORIZATION)).thenReturn("Bearer " + token);

    // Empty request body
    BufferedReader reader = new BufferedReader(new StringReader("{}"));
    when(mockRequest.getReader()).thenReturn(reader);

    // Mock token validation
    mockedSecureWebServicesUtils.when(() -> SecureWebServicesUtils.decodeToken(eq(token))).thenReturn(mockDecodedJWT);

    // Mock claims
    when(mockDecodedJWT.getClaim("user")).thenReturn(mock(com.auth0.jwt.interfaces.Claim.class));
    when(mockDecodedJWT.getClaim("user").asString()).thenReturn("userId");
    when(mockDecodedJWT.getClaim("role")).thenReturn(mock(com.auth0.jwt.interfaces.Claim.class));
    when(mockDecodedJWT.getClaim("role").asString()).thenReturn("roleId");
    when(mockDecodedJWT.getClaim(TestingConstants.ORGANIZATION)).thenReturn(mock(com.auth0.jwt.interfaces.Claim.class));
    when(mockDecodedJWT.getClaim(TestingConstants.ORGANIZATION).asString()).thenReturn("orgId");
    when(mockDecodedJWT.getClaim(TestingConstants.WAREHOUSE)).thenReturn(mock(com.auth0.jwt.interfaces.Claim.class));
    when(mockDecodedJWT.getClaim(TestingConstants.WAREHOUSE).asString()).thenReturn("warehouseId");

    // Mock DAL retrievals
    when(mockOBDal.get(User.class, "userId")).thenReturn(mockUser);
    when(mockOBDal.get(Role.class, "roleId")).thenReturn(mockRole);
    when(mockOBDal.get(Organization.class, "orgId")).thenReturn(mockOrganization);
    when(mockOBDal.get(Warehouse.class, "warehouseId")).thenReturn(mockWarehouse);

    when(mockRequest.getParameter(TestingConstants.SHOW_ROLES)).thenReturn(null);

    // WHEN
    servletUnderTest.doPost(mockRequest, mockResponse);

    // THEN
    String responseContent = responseWriter.toString();
    JSONObject jsonResponse = new JSONObject(responseContent);

    assertEquals(TestingConstants.SUCCESS, jsonResponse.getString(TestingConstants.STATUS));
    assertEquals(TestingConstants.DUMMY_TOKEN, jsonResponse.getString(TestingConstants.TOKEN));
    assertTrue(jsonResponse.has(TestingConstants.ROLE_LIST));
  }

  /**
   * Tests the doPost method with an invalid token.
   *
   * @throws ServletException
   *     if a servlet error occurs
   * @throws IOException
   *     if an I/O error occurs
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testDoPostInvalidToken() throws ServletException, IOException, JSONException {
    // GIVEN
    String token = "invalid-token";

    // Setup request with token
    when(mockRequest.getHeader(TestingConstants.AUTHORIZATION)).thenReturn("Bearer " + token);

    // Empty request body
    BufferedReader reader = new BufferedReader(new StringReader("{}"));
    when(mockRequest.getReader()).thenReturn(reader);

    // Mock token validation to fail
    mockedSecureWebServicesUtils.when(() -> SecureWebServicesUtils.decodeToken(eq(token))).thenReturn(null);

    // WHEN
    servletUnderTest.doPost(mockRequest, mockResponse);

    // THEN
    String responseContent = responseWriter.toString();
    JSONObject jsonResponse = new JSONObject(responseContent);

    assertEquals(TestingConstants.ERROR, jsonResponse.getString(TestingConstants.STATUS));
    assertTrue(jsonResponse.has(TestingConstants.MESSAGE));
  }

  /**
   * Tests the doPost method with missing credentials.
   *
   * @throws ServletException
   *     if a servlet error occurs
   * @throws IOException
   *     if an I/O error occurs
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testDoPostMissingCredentials() throws ServletException, IOException, JSONException {
    // GIVEN
    // Empty JSON with no credentials
    JSONObject emptyJson = new JSONObject();
    String jsonString = emptyJson.toString();

    BufferedReader reader = new BufferedReader(new StringReader(jsonString));
    when(mockRequest.getReader()).thenReturn(reader);

    // No authorization header
    when(mockRequest.getHeader(TestingConstants.AUTHORIZATION)).thenReturn(null);

    // WHEN
    servletUnderTest.doPost(mockRequest, mockResponse);

    // THEN
    String responseContent = responseWriter.toString();
    JSONObject jsonResponse = new JSONObject(responseContent);

    assertEquals(TestingConstants.ERROR, jsonResponse.getString(TestingConstants.STATUS));
    assertTrue(jsonResponse.has(TestingConstants.MESSAGE));
  }

  /**
   * Tests the doPost method when SWS is misconfigured.
   *
   * @throws ServletException
   *     if a servlet error occurs
   * @throws IOException
   *     if an I/O error occurs
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testDoPostMisconfiguredSWS() throws ServletException, IOException, JSONException {
    // GIVEN
    String username = TestingConstants.VALID_USER;
    String password = TestingConstants.VALID_PASSWORD;

    JSONObject loginJson = new JSONObject();
    loginJson.put(TestingConstants.USERNAME, username);
    loginJson.put(TestingConstants.PASSWORD, password);
    String jsonString = loginJson.toString();

    BufferedReader reader = new BufferedReader(new StringReader(jsonString));
    when(mockRequest.getReader()).thenReturn(reader);

    // Simulate misconfigured SWS
    when(mockSWSConfig.getPrivateKey()).thenReturn(null);

    // WHEN
    servletUnderTest.doPost(mockRequest, mockResponse);

    // THEN
    String responseContent = responseWriter.toString();
    JSONObject jsonResponse = new JSONObject(responseContent);

    assertEquals(TestingConstants.ERROR, jsonResponse.getString(TestingConstants.STATUS));
    assertTrue(jsonResponse.has(TestingConstants.MESSAGE));
  }

  /**
   * Tests the doPost method with an invalid user.
   *
   * @throws ServletException
   *     if a servlet error occurs
   * @throws IOException
   *     if an I/O error occurs
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testDoPostInvalidUser() throws ServletException, IOException, JSONException {
    // GIVEN
    String username = "invalidUser";
    String password = "invalidPassword";

    JSONObject loginJson = new JSONObject();
    loginJson.put(TestingConstants.USERNAME, username);
    loginJson.put(TestingConstants.PASSWORD, password);
    String jsonString = loginJson.toString();

    BufferedReader reader = new BufferedReader(new StringReader(jsonString));
    when(mockRequest.getReader()).thenReturn(reader);

    // User not found
    mockedPasswordHash.when(() -> PasswordHash.getUserWithPassword(eq(username), eq(password))).thenReturn(
        Optional.empty());

    // WHEN
    servletUnderTest.doPost(mockRequest, mockResponse);

    // THEN
    String responseContent = responseWriter.toString();
    JSONObject jsonResponse = new JSONObject(responseContent);

    assertEquals(TestingConstants.ERROR, jsonResponse.getString(TestingConstants.STATUS));
    assertTrue(jsonResponse.has(TestingConstants.MESSAGE));
  }

  /**
   * Tests the doPost method when a token creation exception occurs.
   *
   * @throws ServletException
   *     if a servlet error occurs
   * @throws IOException
   *     if an I/O error occurs
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testDoPostTokenCreationException() throws ServletException, IOException, JSONException {
    // GIVEN
    String username = TestingConstants.VALID_USER;
    String password = TestingConstants.VALID_PASSWORD;

    JSONObject loginJson = new JSONObject();
    loginJson.put(TestingConstants.USERNAME, username);
    loginJson.put(TestingConstants.PASSWORD, password);
    String jsonString = loginJson.toString();

    BufferedReader reader = new BufferedReader(new StringReader(jsonString));
    when(mockRequest.getReader()).thenReturn(reader);

    mockedPasswordHash.when(() -> PasswordHash.getUserWithPassword(eq(username), eq(password))).thenReturn(
        Optional.of(mockUser));

    // Throw JWT creation exception
    mockedSecureWebServicesUtils.when(() -> SecureWebServicesUtils.generateToken(any(), any(), any(), any())).thenThrow(
        new JWTCreationException("Error creating token", new Exception()));

    // WHEN
    servletUnderTest.doPost(mockRequest, mockResponse);

    // THEN
    String responseContent = responseWriter.toString();
    JSONObject jsonResponse = new JSONObject(responseContent);

    assertEquals(TestingConstants.ERROR, jsonResponse.getString(TestingConstants.STATUS));
    assertTrue(jsonResponse.has(TestingConstants.MESSAGE));
  }

  /**
   * Tests the doPost method with invalid JSON data.
   *
   * @throws ServletException
   *     if a servlet error occurs
   * @throws IOException
   *     if an I/O error occurs
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testDoPostInvalidJson() throws ServletException, IOException, JSONException {
    // GIVEN
    // Invalid JSON data
    String invalidJson = "{invalid json";

    BufferedReader reader = new BufferedReader(new StringReader(invalidJson));
    when(mockRequest.getReader()).thenReturn(reader);

    // WHEN
    servletUnderTest.doPost(mockRequest, mockResponse);

    // THEN
    String responseContent = responseWriter.toString();
    JSONObject jsonResponse = new JSONObject(responseContent);

    assertEquals(TestingConstants.ERROR, jsonResponse.getString(TestingConstants.STATUS));
    assertTrue(jsonResponse.has(TestingConstants.MESSAGE));
  }

  /**
   * Tests the doOptions method.
   *
   * @throws ServletException
   *     if a servlet error occurs
   * @throws IOException
   *     if an I/O error occurs
   */
  @Test
  public void testDoOptions() throws ServletException, IOException {
    // WHEN
    servletUnderTest.doOptions(mockRequest, mockResponse);

    // THEN
    verify(mockAllowedCrossDomainsHandler, times(1)).setCORSHeaders(mockRequest, mockResponse);
  }
}
