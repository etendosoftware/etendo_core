package org.openbravo.base.secureApp;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.secureApp.AllowedCrossDomainsHandler.AllowedCrossDomainsChecker;
import org.openbravo.base.weld.WeldUtils;

/**
 * Unit tests for the {@link AllowedCrossDomainsHandler} class.
 * Verifies the behavior of origin validation and CORS header handling under various scenarios.
 */
public class AllowedCrossDomainsHandlerTest {
  public static final String ORIGIN = "Origin";
  public static final String EXAMPLE_URL = "https://example.org";


  private AutoCloseable closeable;
  private MockedStatic<WeldUtils> mockedWeldUtils;

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private HttpServletResponse mockResponse;

  private AllowedCrossDomainsHandler handler;

  /**
   * Unit tests for the {@link AllowedCrossDomainsHandler} class.
   * Verifies the behavior of origin validation and CORS header handling under various scenarios.
   */
  @BeforeEach
  void setUp() {
    closeable = MockitoAnnotations.openMocks(this);

    handler = new AllowedCrossDomainsHandler();
    AllowedCrossDomainsHandler.setInstance(handler);

    mockedWeldUtils = mockStatic(WeldUtils.class);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes all mocked static instances and resources.
   *
   * @throws Exception
   *     if an error occurs during cleanup
   */
  @AfterEach
  void tearDown() throws Exception {
    if (mockedWeldUtils != null) {
      mockedWeldUtils.close();
    }
    if (closeable != null) {
      closeable.close();
    }
  }

  /**
   * Nested test class for origin validation scenarios.
   */
  @Nested
  @DisplayName("Tests for origin validation")
  class OriginValidationTests {

    /**
     * Tests that the method returns false for null or empty origin.
     */
    @Test
    @DisplayName("Should return false for null or empty origin when checking invalid origin")
    void isCheckedInvalidOriginNullOrEmptyOriginReturnsFalse() {
      when(mockRequest.getHeader(ORIGIN)).thenReturn(null);

      boolean result = handler.isCheckedInvalidOrigin(mockRequest);

      assertFalse(result, "Should return false for null origin");

      when(mockRequest.getHeader(ORIGIN)).thenReturn("");

      result = handler.isCheckedInvalidOrigin(mockRequest);

      assertFalse(result, "Should return false for empty origin");
    }

    /**
     * Tests that the method returns false when no checkers are installed.
     */
    @Test
    @DisplayName("Should return false when no checkers are installed")
    void isCheckedInvalidOriginNoCheckersReturnsFalse() {
      when(mockRequest.getHeader(ORIGIN)).thenReturn(EXAMPLE_URL);
      mockedWeldUtils.when(() -> WeldUtils.getInstances(AllowedCrossDomainsChecker.class)).thenReturn(List.of());

      boolean result = handler.isCheckedInvalidOrigin(mockRequest);

      assertFalse(result, "Should return false when no checkers are installed");
    }

    /**
     * Tests that the method returns false when the origin is allowed by a checker.
     */
    @Test
    @DisplayName("Should return false when origin is allowed by a checker")
    void isCheckedInvalidOriginAllowedOriginReturnsFalse() {
      String validOrigin = "https://valid-domain.com";
      when(mockRequest.getHeader(ORIGIN)).thenReturn(validOrigin);

      AllowedCrossDomainsChecker mockChecker = mock(AllowedCrossDomainsChecker.class);
      when(mockChecker.isAllowedOrigin(mockRequest, validOrigin)).thenReturn(true);

      mockedWeldUtils.when(() -> WeldUtils.getInstances(AllowedCrossDomainsChecker.class)).thenReturn(
          List.of(mockChecker));

      boolean result = handler.isCheckedInvalidOrigin(mockRequest);

      assertFalse(result, "Should return false when origin is allowed by a checker");
    }

    /**
     * Tests that the method returns true when the origin is not allowed by any checker.
     */
    @Test
    @DisplayName("Should return true when origin is not allowed by any checker")
    void isCheckedInvalidOriginNotAllowedOriginReturnsTrue() {
      String invalidOrigin = "https://invalid-domain.com";
      when(mockRequest.getHeader(ORIGIN)).thenReturn(invalidOrigin);

      AllowedCrossDomainsChecker mockChecker = mock(AllowedCrossDomainsChecker.class);
      when(mockChecker.isAllowedOrigin(mockRequest, invalidOrigin)).thenReturn(false);

      mockedWeldUtils.when(() -> WeldUtils.getInstances(AllowedCrossDomainsChecker.class)).thenReturn(
          List.of(mockChecker));

      boolean result = handler.isCheckedInvalidOrigin(mockRequest);

      assertTrue(result, "Should return true when origin is not allowed by any checker");
    }
  }

  /**
   * Nested test class for CORS header handling scenarios.
   */
  @Nested
  @DisplayName("Tests for CORS headers")
  class CorsHeadersTests {

    /**
     * Tests that CORS headers are not set when no checkers are available.
     */
    @Test
    @DisplayName("Should not set CORS headers when no checkers are available")
    void setCORSHeadersNoCheckersShouldNotSetHeaders() {
      // Arrange
      when(mockRequest.getHeader(ORIGIN)).thenReturn(EXAMPLE_URL);
      mockedWeldUtils.when(() -> WeldUtils.getInstances(AllowedCrossDomainsChecker.class)).thenReturn(List.of());

      // Act
      handler.setCORSHeaders(mockRequest, mockResponse);

      // Assert
      verify(mockResponse, never()).setHeader(org.mockito.ArgumentMatchers.anyString(),
          org.mockito.ArgumentMatchers.anyString());
    }

    /**
     * Tests that CORS headers are not set when the request URL starts with the origin.
     */
    @Test
    @DisplayName("Should not set CORS headers when request URL starts with origin")
    void setCORSHeadersRequestUrlStartsWithOriginShouldNotSetHeaders() {
      // Arrange
      String origin = EXAMPLE_URL;
      when(mockRequest.getHeader(ORIGIN)).thenReturn(origin);
      when(mockRequest.getRequestURL()).thenReturn(new StringBuffer(origin + "/path"));

      AllowedCrossDomainsChecker mockChecker = mock(AllowedCrossDomainsChecker.class);
      mockedWeldUtils.when(() -> WeldUtils.getInstances(AllowedCrossDomainsChecker.class)).thenReturn(
          List.of(mockChecker));

      // Act
      handler.setCORSHeaders(mockRequest, mockResponse);

      // Assert
      verify(mockResponse, never()).setHeader(org.mockito.ArgumentMatchers.anyString(),
          org.mockito.ArgumentMatchers.anyString());
    }

    /**
     * Tests that CORS headers are set when the origin is allowed.
     */
    @Test
    @DisplayName("Should set CORS headers when origin is allowed")
    void setCORSHeadersAllowedOriginShouldSetHeaders() {
      // Arrange
      String validOrigin = "https://valid-domain.com";
      when(mockRequest.getHeader(ORIGIN)).thenReturn(validOrigin);
      when(mockRequest.getRequestURL()).thenReturn(new StringBuffer("https://myapp.com/path"));

      AllowedCrossDomainsChecker mockChecker = mock(AllowedCrossDomainsChecker.class);
      when(mockChecker.isAllowedOrigin(mockRequest, validOrigin)).thenReturn(true);

      mockedWeldUtils.when(() -> WeldUtils.getInstances(AllowedCrossDomainsChecker.class)).thenReturn(
          List.of(mockChecker));

      handler.setCORSHeaders(mockRequest, mockResponse);

      assertAll("Should set all CORS headers",
          () -> verify(mockResponse).setHeader("Access-Control-Allow-Origin", validOrigin),
          () -> verify(mockResponse).setHeader("Access-Control-Allow-Credentials", "true"),
          () -> verify(mockResponse).setHeader("Access-Control-Allow-Methods", "POST, GET, DELETE, OPTIONS"),
          () -> verify(mockResponse).setHeader(org.mockito.ArgumentMatchers.eq("Access-Control-Allow-Headers"),
              org.mockito.ArgumentMatchers.anyString()),
          () -> verify(mockResponse).setHeader("Access-Control-Max-Age", "10000"));
    }

    /**
     * Tests that CORS headers are not set when the origin is not allowed.
     */
    @Test
    @DisplayName("Should not set CORS headers when origin is not allowed")
    void setCORSHeadersNotAllowedOriginShouldNotSetHeaders() {
      String invalidOrigin = "https://invalid-domain.com";
      when(mockRequest.getHeader(ORIGIN)).thenReturn(invalidOrigin);
      when(mockRequest.getRequestURL()).thenReturn(new StringBuffer("https://myapp.com/path"));

      AllowedCrossDomainsChecker mockChecker = mock(AllowedCrossDomainsChecker.class);
      when(mockChecker.isAllowedOrigin(mockRequest, invalidOrigin)).thenReturn(false);

      mockedWeldUtils.when(() -> WeldUtils.getInstances(AllowedCrossDomainsChecker.class)).thenReturn(
          List.of(mockChecker));

      handler.setCORSHeaders(mockRequest, mockResponse);

      verify(mockResponse, never()).setHeader(org.mockito.ArgumentMatchers.anyString(),
          org.mockito.ArgumentMatchers.anyString());
    }
  }

  /**
   * Nested test class for scenarios with multiple checkers.
   */
  @Nested
  @DisplayName("Tests with multiple checkers")
  class MultipleCheckersTests {

    /**
     * Tests that the method returns false if any checker allows the origin.
     */
    @Test
    @DisplayName("Should return true if any checker allows the origin")
    void isCheckedInvalidOriginMultipleCheckersOneAllows() {
      String origin = EXAMPLE_URL;
      when(mockRequest.getHeader(ORIGIN)).thenReturn(origin);

      AllowedCrossDomainsChecker acceptingChecker = mock(AllowedCrossDomainsChecker.class);
      when(acceptingChecker.isAllowedOrigin(mockRequest, origin)).thenReturn(true);

      AllowedCrossDomainsChecker rejectingChecker = mock(AllowedCrossDomainsChecker.class);
      when(rejectingChecker.isAllowedOrigin(mockRequest, origin)).thenReturn(false);

      mockedWeldUtils.when(() -> WeldUtils.getInstances(AllowedCrossDomainsChecker.class)).thenReturn(
          Arrays.asList(rejectingChecker, acceptingChecker));

      boolean result = handler.isCheckedInvalidOrigin(mockRequest);

      assertFalse(result, "Should return false when any checker allows the origin");
    }

    /**
     * This test is to check the behavior when multiple checkers are present and none of them
     * allows the origin.
     */
    @Test
    @DisplayName("Should return false if no checker allows the origin")
    void isCheckedInvalidOriginMultipleCheckersNoneAllows() {
      String origin = EXAMPLE_URL;
      when(mockRequest.getHeader(ORIGIN)).thenReturn(origin);

      AllowedCrossDomainsChecker rejectingChecker1 = mock(AllowedCrossDomainsChecker.class);
      when(rejectingChecker1.isAllowedOrigin(mockRequest, origin)).thenReturn(false);

      AllowedCrossDomainsChecker rejectingChecker2 = mock(AllowedCrossDomainsChecker.class);
      when(rejectingChecker2.isAllowedOrigin(mockRequest, origin)).thenReturn(false);

      mockedWeldUtils.when(() -> WeldUtils.getInstances(AllowedCrossDomainsChecker.class)).thenReturn(
          Arrays.asList(rejectingChecker1, rejectingChecker2));

      boolean result = handler.isCheckedInvalidOrigin(mockRequest);

      assertTrue(result, "Should return true when no checker allows the origin");
    }
  }

}
