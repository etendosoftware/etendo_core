package com.smf.securewebservices.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.service.OBServiceException;
import org.openbravo.service.web.WebService;
import org.openbravo.service.web.WebServiceUtil;

import com.smf.securewebservices.TestingConstants;

/**
 * Unit tests for the SecureWebServiceServlet class.
 */
@RunWith(MockitoJUnitRunner.class)
public class SecureWebServiceServletTest {

  @Spy
  @InjectMocks
  private SecureWebServiceServlet servletUnderTest;

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private HttpServletResponse mockResponse;

  @Mock
  private WebService mockWebService;

  private MockedStatic<WebServiceUtil> mockedWebServiceUtil;

  @Mock
  private WebServiceUtil mockWebServiceUtilInstance;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    mockedWebServiceUtil = mockStatic(WebServiceUtil.class);
    mockedWebServiceUtil.when(WebServiceUtil::getInstance).thenReturn(mockWebServiceUtilInstance);
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    if (mockedWebServiceUtil != null) {
      mockedWebServiceUtil.close();
    }
  }

  /**
   * Tests the doGet method with a valid service.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testDoGetWithValidService() throws Exception {
    // GIVEN
    String pathInfo = TestingConstants.REMAINING_PATH;
    String segment = TestingConstants.TEST_SERVICE;

    when(mockRequest.getPathInfo()).thenReturn(pathInfo);
    when(mockWebServiceUtilInstance.getFirstSegment(pathInfo)).thenReturn(segment);

    doReturn(mockWebService).when(servletUnderTest).getWebService(mockRequest);

    // WHEN
    servletUnderTest.doGet(mockRequest, mockResponse);

    // THEN
    verify(mockWebService).doGet(anyString(), eq(mockRequest), eq(mockResponse));
  }

  /**
   * Tests the doGet method when an exception is thrown.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testDoGetWithException() throws Exception {
    // GIVEN
    String pathInfo = TestingConstants.REMAINING_PATH;
    String segment = TestingConstants.TEST_SERVICE;

    when(mockRequest.getPathInfo()).thenReturn(pathInfo);
    when(mockWebServiceUtilInstance.getFirstSegment(pathInfo)).thenReturn(segment);

    doReturn(mockWebService).when(servletUnderTest).getWebService(mockRequest);

    doThrow(new RuntimeException("Test Exception")).when(mockWebService).doGet(anyString(), eq(mockRequest),
        eq(mockResponse));

    // WHEN & THEN
    OBException exception = assertThrows(OBException.class, () -> servletUnderTest.doGet(mockRequest, mockResponse));

    assertTrue(exception.getCause() instanceof RuntimeException);
    assertEquals("Test Exception", exception.getCause().getMessage());
  }

  /**
   * Tests the doPost method with a valid service.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testDoPostWithValidService() throws Exception {
    // GIVEN
    String pathInfo = "/testService/postPath";
    String segment = TestingConstants.TEST_SERVICE;

    when(mockRequest.getPathInfo()).thenReturn(pathInfo);
    when(mockWebServiceUtilInstance.getFirstSegment(pathInfo)).thenReturn(segment);

    doReturn(mockWebService).when(servletUnderTest).getWebService(mockRequest);

    // WHEN
    servletUnderTest.doPost(mockRequest, mockResponse);

    // THEN
    verify(mockWebService).doPost(anyString(), eq(mockRequest), eq(mockResponse));
  }

  /**
   * Tests the getWebService method when the service is missing.
   */
  @Test
  public void testGetWebServiceWithMissingService() {
    // GIVEN
    String pathInfo = "/nonExistingService";
    when(mockRequest.getPathInfo()).thenReturn(pathInfo);

    SecureWebServiceServlet testServlet = new SecureWebServiceServlet() {
      @Override
      protected WebService getWebService(HttpServletRequest request) {
        WebService result = null;

        if (result == null) {
          throw new OBServiceException("No WebService found using the path info " + request.getPathInfo());
        }
        return result;
      }
    };

    // WHEN & THEN
    OBServiceException exception = assertThrows(OBServiceException.class, () -> testServlet.getWebService(mockRequest));

    assertEquals("No WebService found using the path info " + pathInfo, exception.getMessage());
  }

  /**
   * Tests the getRemainingPath method with a full segment.
   */
  @Test
  public void testGetRemainingPathFullSegment() {
    // GIVEN
    String pathInfo = "/testService";
    String segment = TestingConstants.TEST_SERVICE;

    // WHEN
    String result = servletUnderTest.getRemainingPath(pathInfo, segment);

    // THEN
    assertEquals("", result);
  }

  /**
   * Tests the getRemainingPath method with a partial path.
   */
  @Test
  public void testGetRemainingPathPartialPath() {
    // GIVEN
    String pathInfo = TestingConstants.REMAINING_PATH;
    String segment = TestingConstants.TEST_SERVICE;

    // WHEN
    String result = servletUnderTest.getRemainingPath(pathInfo, segment);

    // THEN
    assertEquals("/remainingPath", result);
  }

  /**
   * Tests the doDelete method with a valid service.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testDoDeleteWithValidService() throws Exception {
    // GIVEN
    String pathInfo = "/testService/deletePath";
    String segment = TestingConstants.TEST_SERVICE;

    when(mockRequest.getPathInfo()).thenReturn(pathInfo);
    when(mockWebServiceUtilInstance.getFirstSegment(pathInfo)).thenReturn(segment);

    doReturn(mockWebService).when(servletUnderTest).getWebService(mockRequest);

    // WHEN
    servletUnderTest.doDelete(mockRequest, mockResponse);

    // THEN
    verify(mockWebService).doDelete(anyString(), eq(mockRequest), eq(mockResponse));
  }

  /**
   * Tests the doPut method with a valid service.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testDoPutWithValidService() throws Exception {
    // GIVEN
    String pathInfo = "/testService/putPath";
    String segment = TestingConstants.TEST_SERVICE;

    when(mockRequest.getPathInfo()).thenReturn(pathInfo);
    when(mockWebServiceUtilInstance.getFirstSegment(pathInfo)).thenReturn(segment);

    doReturn(mockWebService).when(servletUnderTest).getWebService(mockRequest);

    // WHEN
    servletUnderTest.doPut(mockRequest, mockResponse);

    // THEN
    verify(mockWebService).doPut(anyString(), eq(mockRequest), eq(mockResponse));
  }
}
