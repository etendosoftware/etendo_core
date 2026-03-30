package org.openbravo.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for the multipart InputStream caching behavior in {@link VariablesBase}.
 *
 * <p>Covers the regression introduced by ETP-3613: when a multipart request carries an
 * {@code Authorization: Bearer} header, {@code VariablesBase} can be instantiated more than once
 * in the same request lifecycle (e.g., once in the auth filter and again in the servlet). Without
 * the cache, each instantiation attempts to re-read the already-exhausted {@code InputStream},
 * resulting in an empty parameter list and a "Process ID missing in request" error.
 */
public class VariablesBaseMultipartCachingTest {

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private HttpSession mockSession;

  @Mock
  private FileItem mockFileItem;

  private AutoCloseable mocks;
  private MockedStatic<ServletFileUpload> mockedServletFileUploadStatic;

  @Before
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    mockedServletFileUploadStatic = mockStatic(ServletFileUpload.class);

    when(mockRequest.getSession(false)).thenReturn(mockSession);
    when(mockRequest.getParameter("stateless")).thenReturn(null);
    when(mockRequest.getAttribute("stateless")).thenReturn(null);
  }

  @After
  public void tearDown() throws Exception {
    if (mockedServletFileUploadStatic != null) {
      mockedServletFileUploadStatic.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Verifies that on the first instantiation with a multipart request, {@code VariablesBase}
   * parses the stream and stores the resulting {@code FileItem} list as a request attribute.
   */
  @Test
  public void testMultipartItemsParsedAndCachedOnFirstInstantiation() throws Exception {
    // Given
    List<FileItem> expectedItems = Collections.singletonList(mockFileItem);

    mockedServletFileUploadStatic.when(
        () -> ServletFileUpload.isMultipartContent(any())).thenReturn(true);
    when(mockRequest.getAttribute(VariablesBase.MULTIPART_ITEMS_REQUEST_ATTR)).thenReturn(null);

    try (MockedConstruction<ServletFileUpload> mockedUpload = mockConstruction(
        ServletFileUpload.class,
        (mock, context) -> when(mock.parseRequest(mockRequest)).thenReturn(expectedItems))) {

      // When
      VariablesBase variables = new VariablesBase(mockRequest);

      // Then — stream was parsed and result cached
      assertEquals("Items should equal parsed result", expectedItems, variables.items);
      verify(mockRequest).setAttribute(VariablesBase.MULTIPART_ITEMS_REQUEST_ATTR, expectedItems);
      assertEquals("ServletFileUpload should have been instantiated once", 1,
          mockedUpload.constructed().size());
      verify(mockedUpload.constructed().get(0)).parseRequest(mockRequest);
    }
  }

  /**
   * Verifies that when multipart items are already cached in the request attribute (i.e.,
   * {@code VariablesBase} was already instantiated earlier in the same request), the second
   * instantiation reuses the cached list without attempting to re-read the stream.
   *
   * <p>This is the core regression test for ETP-3613.
   */
  @Test
  public void testCachedMultipartItemsReusedWithoutReParsing() throws Exception {
    // Given — a previous instantiation already parsed and cached the items
    List<FileItem> cachedItems = Collections.singletonList(mockFileItem);

    mockedServletFileUploadStatic.when(
        () -> ServletFileUpload.isMultipartContent(any())).thenReturn(true);
    when(mockRequest.getAttribute(
        VariablesBase.MULTIPART_ITEMS_REQUEST_ATTR)).thenReturn(cachedItems);

    try (MockedConstruction<ServletFileUpload> mockedUpload = mockConstruction(
        ServletFileUpload.class)) {

      // When — second instantiation on the same request
      VariablesBase variables = new VariablesBase(mockRequest);

      // Then — cached items reused, no new ServletFileUpload created, stream not re-read
      assertEquals("Cached items should be reused", cachedItems, variables.items);
      assertEquals("ServletFileUpload should NOT be instantiated again", 0,
          mockedUpload.constructed().size());
      verify(mockRequest, never()).setAttribute(
          eq(VariablesBase.MULTIPART_ITEMS_REQUEST_ATTR), any());
    }
  }

  /**
   * Verifies that for non-multipart requests, no parsing or attribute caching occurs.
   */
  @Test
  public void testNonMultipartRequestSkipsParsing() {
    // Given
    mockedServletFileUploadStatic.when(
        () -> ServletFileUpload.isMultipartContent(any())).thenReturn(false);

    // When
    VariablesBase variables = new VariablesBase(mockRequest);

    // Then
    assertNull("Items should be null for non-multipart requests", variables.items);
    verify(mockRequest, never()).getAttribute(VariablesBase.MULTIPART_ITEMS_REQUEST_ATTR);
    verify(mockRequest, never()).setAttribute(
        eq(VariablesBase.MULTIPART_ITEMS_REQUEST_ATTR), any());
  }
}
