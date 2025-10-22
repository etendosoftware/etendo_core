package com.smf.securewebservices.service;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.weld.WeldUtils;

import com.smf.securewebservices.TestingConstants;

/**
 * Unit tests for the {@link KernelServlet} class.
 */
public class KernelServletTest {

  private com.smf.securewebservices.service.KernelServlet kernelServlet;

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private HttpServletResponse mockResponse;

  @Mock
  private org.openbravo.client.kernel.KernelServlet mockOpenbravoKernelServlet;

  private MockedStatic<WeldUtils> mockedWeldUtils;
  private AutoCloseable mocks;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    mockedWeldUtils = mockStatic(WeldUtils.class);

    mockedWeldUtils.when(
        () -> WeldUtils.getInstanceFromStaticBeanManager(org.openbravo.client.kernel.KernelServlet.class)).thenReturn(
        mockOpenbravoKernelServlet);

    kernelServlet = new com.smf.securewebservices.service.KernelServlet();
  }

  /**
   * Cleans up the test environment after each test.
   *
   * @throws Exception
   *     if an error occurs during teardown
   */
  @After
  public void tearDown() throws Exception {
    if (mockedWeldUtils != null) {
      mockedWeldUtils.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Tests the doGet method to ensure it delegates to the Openbravo KernelServlet.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testDoGetDelegatesToKernelServlet() throws Exception {
    // Given
    String path = TestingConstants.TEST_PATH;

    // When
    kernelServlet.doGet(path, mockRequest, mockResponse);

    // Then
    verify(mockOpenbravoKernelServlet, times(1)).doGet(mockRequest, mockResponse);
  }

  /**
   * Tests the doPost method to ensure it delegates to the doGet method.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testDoPostDelegatesToDoGet() throws Exception {
    // Given
    String path = TestingConstants.TEST_PATH;

    // When
    kernelServlet.doPost(path, mockRequest, mockResponse);

    // Then
    verify(mockOpenbravoKernelServlet, times(1)).doGet(mockRequest, mockResponse);
  }

  /**
   * Tests the doDelete method to ensure it performs no operation.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testDoDeleteNoOperation() throws Exception {
    // Given
    String path = TestingConstants.TEST_PATH;

    // When
    kernelServlet.doDelete(path, mockRequest, mockResponse);

    // Then
    verifyNoInteractions(mockOpenbravoKernelServlet);
  }

  /**
   * Tests the doPut method to ensure it performs no operation.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testDoPutNoOperation() throws Exception {
    // Given
    String path = TestingConstants.TEST_PATH;

    // When
    kernelServlet.doPut(path, mockRequest, mockResponse);

    // Then
    verifyNoInteractions(mockOpenbravoKernelServlet);
  }
}
