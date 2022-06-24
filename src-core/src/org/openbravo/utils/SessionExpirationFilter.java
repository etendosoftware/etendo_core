/*
 ************************************************************************************
 * Copyright (C) 2008-2019 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.utils;

import java.io.IOException;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Servlet filter that handles session expiration. There are requests such as alert ping that should
 * not be taken into account as active request to prevent session expiration, that's why we cannot
 * rely on Tomcat's default mechanism. This filter takes them into account to reset expiration time
 * accordingly.
 */
public class SessionExpirationFilter implements Filter {

  @Override
  public void init(FilterConfig config) {
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest hReq = (HttpServletRequest) req;

    HttpSession session = hReq.getSession(false);
    if (session != null) {
      try {
        Date expirationDate = (Date) session.getAttribute("expirationDate");

        if (expirationDate != null && expirationDate.before(new Date())) {
          session.invalidate();
        } else {
          // ignore requests marked as both ajaxCall and ignoreForSessionTimeout
          String isAjaxCall = hReq.getParameter("IsAjaxCall");
          String ignoreForSessionTimeout = hReq.getParameter("ignoreForSessionTimeout");
          boolean ignoreForTimeout = "1".equals(isAjaxCall) && "1".equals(ignoreForSessionTimeout);
          if (!ignoreForTimeout) {
            session.setAttribute("expirationDate",
                new Date(System.currentTimeMillis() + session.getMaxInactiveInterval() * 1000));
          }
        }
      } catch (IllegalStateException ignore) {
        // If session is already invalidated, IllegalStateException is thrown while trying to
        // read/write attributes. HttpSession does not provide any way to check validity.
        // Let's just ignore this case and continue.
      }
    }
    chain.doFilter(req, resp);
  }

  @Override
  public void destroy() {
  }

}
