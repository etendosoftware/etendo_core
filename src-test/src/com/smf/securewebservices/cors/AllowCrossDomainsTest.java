package com.smf.securewebservices.cors;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.smf.securewebservices.TestingConstants;

/**
 * Unit tests for the AllowCrossDomains class.
 */
@RunWith(MockitoJUnitRunner.class)
public class AllowCrossDomainsTest {

  @InjectMocks
  private AllowCrossDomains classUnderTest;

  private AutoCloseable mocks;
  private HttpServletRequest mockRequest;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    mockRequest = mock(HttpServletRequest.class);
  }

  /**
   * Cleans up the test environment after each test.
   *
   * @throws Exception
   *     if an error occurs during teardown
   */
  @After
  public void tearDown() throws Exception {
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Tests the isAllowedOrigin method with a valid origin.
   */
  @Test
  public void testIsAllowedOriginWithValidOriginReturnsTrue() {
    // Given
    String origin = "https://example.com";
    when(mockRequest.getRequestURL()).thenReturn(new StringBuffer(TestingConstants.API_URL));
    when(mockRequest.getQueryString()).thenReturn(TestingConstants.PARAM_VALUE);

    // When
    boolean result = classUnderTest.isAllowedOrigin(mockRequest, origin);

    // Then
    assertTrue("The method should allow any origin", result);
  }

  /**
   * Tests the isAllowedOrigin method with a null origin.
   */
  @Test
  public void testIsAllowedOriginWithNullOriginReturnsTrue() {
    // Given
    String origin = null;
    when(mockRequest.getRequestURL()).thenReturn(new StringBuffer(TestingConstants.API_URL));
    when(mockRequest.getQueryString()).thenReturn(null);

    // When
    boolean result = classUnderTest.isAllowedOrigin(mockRequest, origin);

    // Then
    assertTrue("The method should allow null origin", result);
  }

  /**
   * Tests the isAllowedOrigin method with an empty origin.
   */
  @Test
  public void testIsAllowedOriginWithEmptyOriginReturnsTrue() {
    // Given
    String origin = "";
    when(mockRequest.getRequestURL()).thenReturn(new StringBuffer(TestingConstants.API_URL));
    when(mockRequest.getQueryString()).thenReturn(TestingConstants.PARAM_VALUE);

    // When
    boolean result = classUnderTest.isAllowedOrigin(mockRequest, origin);

    // Then
    assertTrue("The method should allow empty origin", result);
  }

  /**
   * Tests the isAllowedOrigin method with a localhost origin.
   */
  @Test
  public void testIsAllowedOriginWithLocalhostReturnsTrue() {
    // Given
    String origin = "http://localhost:8080";
    when(mockRequest.getRequestURL()).thenReturn(new StringBuffer(TestingConstants.API_URL));
    when(mockRequest.getQueryString()).thenReturn(TestingConstants.PARAM_VALUE);

    // When
    boolean result = classUnderTest.isAllowedOrigin(mockRequest, origin);

    // Then
    assertTrue("The method should allow localhost origin", result);
  }
}
