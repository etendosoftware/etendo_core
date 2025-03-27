package com.smf.securewebservices.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;

import com.smf.securewebservices.TestingConstants;

/**
 * Unit tests for the DataSourceServlet class.
 */
public class DataSourceServletTest {

  private DataSourceServlet servlet;

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private HttpServletResponse mockResponse;

  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<WeldUtils> mockedWeldUtils;
  private MockedStatic<LoginUtils> mockedLoginUtils;
  private AutoCloseable closeable;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
    servlet = new DataSourceServlet();
  }

  /**
   * Cleans up the test environment after each test.
   *
   * @throws Exception
   *     if an error occurs during teardown
   */
  @After
  public void tearDown() throws Exception {
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedWeldUtils != null) {
      mockedWeldUtils.close();
    }
    if (mockedLoginUtils != null) {
      mockedLoginUtils.close();
    }
    if (closeable != null) {
      closeable.close();
    }
  }

  /**
   * Tests that the doPost method calls doGet.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoPostCallsDoGet() throws Exception {
    // GIVEN
    com.smf.securewebservices.service.DataSourceServlet spyServlet = spy(servlet);
    doNothing().when(spyServlet).doGet(anyString(), any(HttpServletRequest.class), any(HttpServletResponse.class));

    // WHEN
    spyServlet.doPost(TestingConstants.TEST_PATH_NAME, mockRequest, mockResponse);

    // THEN
    verify(spyServlet, times(1)).doGet(TestingConstants.TEST_PATH_NAME, mockRequest, mockResponse);
  }

  /**
   * Tests that the doDelete method does nothing.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoDeleteNoOp() throws Exception {
    // WHEN
    servlet.doDelete(TestingConstants.TEST_PATH_NAME, mockRequest, mockResponse);
  }

  /**
   * Tests that the doPut method does nothing.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoPutNoOp() throws Exception {
    // WHEN
    servlet.doPut(TestingConstants.TEST_PATH_NAME, mockRequest, mockResponse);
  }

  /**
   * Tests the exception flow of the doGet method.
   */
  @Test
  public void testDoGetExceptionFlow() {
    // GIVEN
    mockedOBContext = mockStatic(OBContext.class);
    mockedWeldUtils = mockStatic(WeldUtils.class);

    mockedOBContext.when(OBContext::setAdminMode).thenThrow(new RuntimeException("Test exception"));

    // WHEN
    Exception caughtException = null;
    try {
      servlet.doGet(TestingConstants.TEST_PATH_NAME, mockRequest, mockResponse);
    } catch (Exception e) {
      caughtException = e;
    }

    // THEN
    assert caughtException != null;
    assert "Test exception".equals(caughtException.getMessage());

    mockedOBContext.verify(OBContext::restorePreviousMode);
  }
}
