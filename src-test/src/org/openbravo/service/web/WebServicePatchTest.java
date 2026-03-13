package org.openbravo.service.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
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
 * - Default doPatch() in WebService interface returns 405 Method Not Allowed
 * - BaseWebServiceServlet intercepts PATCH requests in doService()
 * - WebServiceServlet routes PATCH to the correct WebService implementation
 * - Non-PATCH methods are not affected by the PATCH changes
 */
@RunWith(MockitoJUnitRunner.class)
public class WebServicePatchTest {

  private static final String TEST_SERVICE = "testService";

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

  private String pathInfo;

  /** Sets up the WebServiceUtil static mock before each test. */
  @Before
  public void setUp() {
    pathInfo = "/" + TEST_SERVICE + "/resource/123";
    mockedWebServiceUtil = mockStatic(WebServiceUtil.class);
    mockedWebServiceUtil.when(WebServiceUtil::getInstance).thenReturn(mockWebServiceUtilInstance);
  }

  /** Closes the WebServiceUtil static mock after each test. */
  @After
  public void tearDown() {
    if (mockedWebServiceUtil != null) {
      mockedWebServiceUtil.close();
    }
  }

  /**
   * Creates a stub WebService implementation where all HTTP method handlers are no-ops.
   * Used to test the default doPatch() behavior from the interface.
   */
  private WebService createStubWebService() {
    return new StubWebService();
  }

  /** Configures mocks so that the servlet routes requests to the mock WebService. */
  private void setUpServletRouting() {
    when(mockRequest.getPathInfo()).thenReturn(pathInfo);
    when(mockWebServiceUtilInstance.getFirstSegment(pathInfo)).thenReturn(TEST_SERVICE);
    doReturn(mockWebService).when(servletUnderTest).getWebService(mockRequest);
  }

  // -- 1. WebService interface: default doPatch() returns 405 --

  /**
   * Verifies that the default doPatch() implementation sets 405 status.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testDefaultDoPatchReturns405() throws Exception {
    WebService defaultImpl = createStubWebService();

    defaultImpl.doPatch("/test", mockRequest, mockResponse);

    verify(mockResponse).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
  }

  // -- 2. BaseWebServiceServlet: doPatch sends 405 --

  /**
   * Verifies that BaseWebServiceServlet.doPatch sends a 405 error.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testBaseServletDoPatchSends405() throws Exception {
    BaseWebServiceServlet baseServlet = new BaseWebServiceServlet();

    baseServlet.doPatch(mockRequest, mockResponse);

    verify(mockResponse).sendError(
        eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED),
        eq("PATCH not supported")
    );
  }

  // -- 3. WebServiceServlet: doPatch routing --

  /**
   * Verifies that WebServiceServlet.doPatch delegates to the resolved WebService.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testDoPatchRoutesToWebService() throws Exception {
    setUpServletRouting();

    servletUnderTest.doPatch(mockRequest, mockResponse);

    verify(mockWebService).doPatch(anyString(), eq(mockRequest), eq(mockResponse));
  }

  /**
   * Verifies that exceptions thrown by WebService.doPatch are wrapped in OBException.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testDoPatchWrapsExceptionInOBException() throws Exception {
    setUpServletRouting();
    doThrow(new RuntimeException("Patch failed"))
        .when(mockWebService)
        .doPatch(anyString(), eq(mockRequest), eq(mockResponse));

    try {
      servletUnderTest.doPatch(mockRequest, mockResponse);
      fail("Expected OBException to be thrown");
    } catch (OBException exception) {
      assertTrue(exception.getCause() instanceof RuntimeException);
      assertEquals("Patch failed", exception.getCause().getMessage());
    }
  }

  /**
   * Verifies that a WebService with custom doPatch does not return 405.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testWebServiceWithPatchSupportDoesNotReturn405() throws Exception {
    WebService patchCapable = new StubWebService() {
      @Override
      public void doPatch(String path, HttpServletRequest request, HttpServletResponse response) {
        // Successful PATCH handling — no 405
      }
    };

    patchCapable.doPatch("/test", mockRequest, mockResponse);
  }

  // -- 4. Non-PATCH methods are NOT affected --

  /**
   * Verifies that doGet still routes correctly to the WebService.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testDoGetStillWorksCorrectly() throws Exception {
    setUpServletRouting();
    servletUnderTest.doGet(mockRequest, mockResponse);
    verify(mockWebService).doGet(anyString(), eq(mockRequest), eq(mockResponse));
  }

  /**
   * Verifies that doPost still routes correctly to the WebService.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testDoPostStillWorksCorrectly() throws Exception {
    setUpServletRouting();
    servletUnderTest.doPost(mockRequest, mockResponse);
    verify(mockWebService).doPost(anyString(), eq(mockRequest), eq(mockResponse));
  }

  /**
   * Verifies that doPut still routes correctly to the WebService.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testDoPutStillWorksCorrectly() throws Exception {
    setUpServletRouting();
    servletUnderTest.doPut(mockRequest, mockResponse);
    verify(mockWebService).doPut(anyString(), eq(mockRequest), eq(mockResponse));
  }

  /**
   * Verifies that doDelete still routes correctly to the WebService.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testDoDeleteStillWorksCorrectly() throws Exception {
    setUpServletRouting();
    servletUnderTest.doDelete(mockRequest, mockResponse);
    verify(mockWebService).doDelete(anyString(), eq(mockRequest), eq(mockResponse));
  }

  /**
   * Minimal WebService implementation used as a test stub.
   * All HTTP method handlers are intentionally empty since tests only need
   * the default doPatch() behavior from the interface.
   */
  private static class StubWebService implements WebService {
    @Override
    public void doGet(String path, HttpServletRequest request, HttpServletResponse response) {
      // No-op stub for testing
    }

    @Override
    public void doPost(String path, HttpServletRequest request, HttpServletResponse response) {
      // No-op stub for testing
    }

    @Override
    public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) {
      // No-op stub for testing
    }

    @Override
    public void doPut(String path, HttpServletRequest request, HttpServletResponse response) {
      // No-op stub for testing
    }

    public String getModulePrefix() {
      return "test";
    }
  }
}
