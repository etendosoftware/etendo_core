package org.openbravo.service.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

/**
 * Unit tests for PATCH HTTP method support in the WebService layer.
 *
 * Covers:
 * - Default doPatch() in WebService interface throws UnsupportedOperationException
 * - BaseWebServiceServlet intercepts PATCH requests in doService()
 * - WebServiceServlet routes PATCH to the correct WebService implementation
 * - Non-PATCH methods are not affected by the PATCH changes
 */
@RunWith(MockitoJUnitRunner.class)
public class WebServicePatchTest {

  private static final String TEST_SERVICE = "testService";
  private static final String PATH_INFO = "/testService/resource/123";

  @Spy
  @InjectMocks
  private WebServiceServlet servletUnderTest;

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private HttpServletResponse mockResponse;

  @Mock
  private WebService mockWebService;

  private MockedStatic<WebServiceUtil> mockedWebServiceUtil;

  @Mock
  private WebServiceUtil mockWebServiceUtilInstance;

  @Before
  public void setUp() {
    mockedWebServiceUtil = mockStatic(WebServiceUtil.class);
    mockedWebServiceUtil.when(WebServiceUtil::getInstance).thenReturn(mockWebServiceUtilInstance);
  }

  @After
  public void tearDown() {
    if (mockedWebServiceUtil != null) {
      mockedWebServiceUtil.close();
    }
  }

  // ---------------------------------------------------------------
  // 1. WebService interface: default doPatch() throws UnsupportedOperationException
  // ---------------------------------------------------------------

  @Test
  public void testDefaultDoPatchThrowsUnsupportedOperationException() {
    // GIVEN - a WebService that does not override doPatch (uses the default)
    WebService defaultImpl = new WebService() {
      @Override
      public void doGet(String path, HttpServletRequest request, HttpServletResponse response) {
      }

      @Override
      public void doPost(String path, HttpServletRequest request, HttpServletResponse response) {
      }

      @Override
      public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) {
      }

      @Override
      public void doPut(String path, HttpServletRequest request, HttpServletResponse response) {
      }

      @Override
      public String getModulePrefix() {
        return "test";
      }
    };

    // WHEN & THEN
    UnsupportedOperationException exception = assertThrows(
        UnsupportedOperationException.class,
        () -> defaultImpl.doPatch("/test", mockRequest, mockResponse)
    );
    assertEquals("PATCH method not supported by this web service.", exception.getMessage());
  }

  // ---------------------------------------------------------------
  // 2. BaseWebServiceServlet: PATCH interception in doService()
  // ---------------------------------------------------------------

  @Test
  public void testBaseServletDoPatchSends405() throws Exception {
    // GIVEN
    BaseWebServiceServlet baseServlet = new BaseWebServiceServlet();

    // WHEN
    baseServlet.doPatch(mockRequest, mockResponse);

    // THEN
    verify(mockResponse).sendError(
        eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED),
        eq("PATCH not supported")
    );
  }

  // ---------------------------------------------------------------
  // 3. WebServiceServlet: doPatch routes to correct WebService
  // ---------------------------------------------------------------

  @Test
  public void testDoPatchRoutesToWebService() throws Exception {
    // GIVEN
    when(mockRequest.getPathInfo()).thenReturn(PATH_INFO);
    when(mockWebServiceUtilInstance.getFirstSegment(PATH_INFO)).thenReturn(TEST_SERVICE);
    doReturn(mockWebService).when(servletUnderTest).getWebService(mockRequest);

    // WHEN
    servletUnderTest.doPatch(mockRequest, mockResponse);

    // THEN
    verify(mockWebService).doPatch(anyString(), eq(mockRequest), eq(mockResponse));
  }

  @Test
  public void testDoPatchWrapsExceptionInOBException() throws Exception {
    // GIVEN
    when(mockRequest.getPathInfo()).thenReturn(PATH_INFO);
    when(mockWebServiceUtilInstance.getFirstSegment(PATH_INFO)).thenReturn(TEST_SERVICE);
    doReturn(mockWebService).when(servletUnderTest).getWebService(mockRequest);

    doThrow(new RuntimeException("Patch failed"))
        .when(mockWebService)
        .doPatch(anyString(), eq(mockRequest), eq(mockResponse));

    // WHEN & THEN
    OBException exception = assertThrows(
        OBException.class,
        () -> servletUnderTest.doPatch(mockRequest, mockResponse)
    );
    assertTrue(exception.getCause() instanceof RuntimeException);
    assertEquals("Patch failed", exception.getCause().getMessage());
  }

  @Test
  public void testWebServiceWithPatchSupportDoesNotThrow() throws Exception {
    // GIVEN - a WebService that supports PATCH
    WebService patchCapable = new WebService() {
      @Override
      public void doGet(String path, HttpServletRequest request, HttpServletResponse response) {
      }

      @Override
      public void doPost(String path, HttpServletRequest request, HttpServletResponse response) {
      }

      @Override
      public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) {
      }

      @Override
      public void doPut(String path, HttpServletRequest request, HttpServletResponse response) {
      }

      @Override
      public void doPatch(String path, HttpServletRequest request, HttpServletResponse response) {
        // Custom PATCH implementation - no exception
      }

      @Override
      public String getModulePrefix() {
        return "test";
      }
    };

    // WHEN & THEN - no exception thrown
    patchCapable.doPatch("/test", mockRequest, mockResponse);
  }

  // ---------------------------------------------------------------
  // 4. Non-PATCH methods are NOT affected
  // ---------------------------------------------------------------

  @Test
  public void testDoGetStillWorksCorrectly() throws Exception {
    // GIVEN
    when(mockRequest.getPathInfo()).thenReturn(PATH_INFO);
    when(mockWebServiceUtilInstance.getFirstSegment(PATH_INFO)).thenReturn(TEST_SERVICE);
    doReturn(mockWebService).when(servletUnderTest).getWebService(mockRequest);

    // WHEN
    servletUnderTest.doGet(mockRequest, mockResponse);

    // THEN
    verify(mockWebService).doGet(anyString(), eq(mockRequest), eq(mockResponse));
  }

  @Test
  public void testDoPostStillWorksCorrectly() throws Exception {
    // GIVEN
    when(mockRequest.getPathInfo()).thenReturn(PATH_INFO);
    when(mockWebServiceUtilInstance.getFirstSegment(PATH_INFO)).thenReturn(TEST_SERVICE);
    doReturn(mockWebService).when(servletUnderTest).getWebService(mockRequest);

    // WHEN
    servletUnderTest.doPost(mockRequest, mockResponse);

    // THEN
    verify(mockWebService).doPost(anyString(), eq(mockRequest), eq(mockResponse));
  }

  @Test
  public void testDoPutStillWorksCorrectly() throws Exception {
    // GIVEN
    when(mockRequest.getPathInfo()).thenReturn(PATH_INFO);
    when(mockWebServiceUtilInstance.getFirstSegment(PATH_INFO)).thenReturn(TEST_SERVICE);
    doReturn(mockWebService).when(servletUnderTest).getWebService(mockRequest);

    // WHEN
    servletUnderTest.doPut(mockRequest, mockResponse);

    // THEN
    verify(mockWebService).doPut(anyString(), eq(mockRequest), eq(mockResponse));
  }

  @Test
  public void testDoDeleteStillWorksCorrectly() throws Exception {
    // GIVEN
    when(mockRequest.getPathInfo()).thenReturn(PATH_INFO);
    when(mockWebServiceUtilInstance.getFirstSegment(PATH_INFO)).thenReturn(TEST_SERVICE);
    doReturn(mockWebService).when(servletUnderTest).getWebService(mockRequest);

    // WHEN
    servletUnderTest.doDelete(mockRequest, mockResponse);

    // THEN
    verify(mockWebService).doDelete(anyString(), eq(mockRequest), eq(mockResponse));
  }
}
