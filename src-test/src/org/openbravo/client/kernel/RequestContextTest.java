/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.client.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RequestContext}, covering ThreadLocal lifecycle,
 * request/response handling, parameter operations, session attributes,
 * and request content via the HTTP servlet request wrapper.
 */
@DisplayName("RequestContext")
public class RequestContextTest {

  @BeforeEach
  void setUp() {
    RequestContext.clear();
  }

  @AfterEach
  void tearDown() {
    RequestContext.clear();
  }

  private HttpServletRequest mockRequestWithSession() {
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);
    when(req.getSession()).thenReturn(session);
    when(req.getSession(true)).thenReturn(session);
    when(req.getParameterMap()).thenReturn(Collections.emptyMap());
    return req;
  }

  @Nested
  @DisplayName("ThreadLocal lifecycle")
  class ThreadLocalLifecycle {
    @Test
    void testGetReturnsNonNull() {
      assertNotNull(RequestContext.get());
    }

    @Test
    void testGetReturnsSameInstance() {
      assertSame(RequestContext.get(), RequestContext.get());
    }

    @Test
    void testClearAndGet() {
      RequestContext before = RequestContext.get();
      RequestContext.clear();
      assertNotSame(before, RequestContext.get());
    }
  }

  @Nested
  @DisplayName("Request and response")
  class RequestResponse {
    @Test
    void testSetGetResponse() {
      HttpServletResponse mockResp = mock(HttpServletResponse.class);
      RequestContext ctx = RequestContext.get();
      ctx.setResponse(mockResp);
      assertSame(mockResp, ctx.getResponse());
    }

    @Test
    void testResponseNullByDefault() {
      assertNull(RequestContext.get().getResponse());
    }

    @Test
    void testGetParameterWithNoRequest() {
      assertNull(RequestContext.get().getRequestParameter("anything"));
    }

    @Test
    void testSetRequestWraps() {
      HttpServletRequest mockReq = mockRequestWithSession();
      RequestContext ctx = RequestContext.get();
      ctx.setRequest(mockReq);
      assertNotNull(ctx.getRequest());
    }
  }

  @Nested
  @DisplayName("Parameter operations via wrapper")
  class ParameterOperations {
    @Test
    void testSetGetParameter() {
      RequestContext ctx = RequestContext.get();
      ctx.setRequest(mockRequestWithSession());
      ctx.setRequestParameter("key1", "value1");
      assertEquals("value1", ctx.getRequestParameter("key1"));
    }

    @Test
    void testMissingParameterReturnsNull() {
      HttpServletRequest req = mockRequestWithSession();
      when(req.getParameter("nonexistent")).thenReturn(null);
      RequestContext ctx = RequestContext.get();
      ctx.setRequest(req);
      assertNull(ctx.getRequestParameter("nonexistent"));
    }
  }

  @Nested
  @DisplayName("Session attributes via wrapper")
  class SessionAttributes {
    @Test
    void testSessionNotNull() {
      RequestContext ctx = RequestContext.get();
      ctx.setRequest(mockRequestWithSession());
      assertNotNull(ctx.getSession());
    }
  }

  @Nested
  @DisplayName("Request content via wrapper")
  class RequestContentTests {
    @Test
    void testSetAndGetRequestContent() {
      RequestContext ctx = RequestContext.get();
      ctx.setRequest(mockRequestWithSession());
      RequestContext.HttpServletRequestWrapper wrapper =
          (RequestContext.HttpServletRequestWrapper) ctx.getRequest();
      wrapper.setRequestContent("{\"test\":true}");
      assertEquals("{\"test\":true}", wrapper.getRequestContent());
    }
  }
}
