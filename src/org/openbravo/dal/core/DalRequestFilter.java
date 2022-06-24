/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2008-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.dal.core;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.SessionInfo;

/**
 * The DalRequestFilter ensures that the request thread is handled inside of a
 * {@link DalThreadHandler DalThreadHandler} this ensures that all requests are handled within a
 * transaction which is committed or rolled back at the end of the request.
 * <p>
 * This request filter also initializes the Dal layer (see the {@link DalLayerInitializer
 * DalLayerInitializer}). Although this is not required (session factory initialization is done
 * automatically at first database access), it is better for test/debug purposes to do the
 * initialization here.
 * 
 * The DalRequestFilter is enabled by setting it in the web.xml file:
 * 
 * {@literal
 * <filter> <filter-name>dalFilter</filter-name>
 * <filter-class>org.openbravo.dal.core.DalRequestFilter</filter-class> </filter>
 * 
 * <filter-mapping>
 * 
 * <filter-name>dalFilter</filter-name> <url-pattern>/*</url-pattern>
 * 
 * </filter-mapping>
 * }
 * 
 * Note the url-pattern can be defined more strictly if it is possible to identify the pages which
 * require a session/transaction.
 * 
 * @author mtaal
 */

public class DalRequestFilter implements Filter {
  private static final Logger log = LogManager.getLogger();

  @Override
  public void init(FilterConfig fConfig) throws ServletException {
    DalLayerInitializer.getInstance().initialize(true);
  }

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response,
      final FilterChain chain) throws IOException, ServletException {
    final DalThreadHandler dth = new DalThreadHandler() {

      @Override
      public void doBefore() {
        HttpServletRequest req = (HttpServletRequest) request;
        try {
          OBContext.setOBContext(req);
        } catch (IllegalStateException e) {
          // If session has already been invalidated, IllegalStateException is thrown. Just log it
          // but don't fail.
          log.error("Could not set OBContext in request for URI {} - ", req.getRequestURI(), e);
        }
      }

      @Override
      protected void doAction() throws Exception {
        chain.doFilter(request, response);
      }

      // note OBContext is set to null in DalThreadHandler
      @Override
      public void doFinal(boolean errorOccured) {
        try {
          if (OBContext.getOBContext() != null) {
            // set the obcontext in the session
            OBContext.setOBContextInSession((HttpServletRequest) request, OBContext.getOBContext());
          }

          OBContext.clearAdminModeStack();
          OBInterceptor.setPreventUpdateInfoChange(false);
          super.doFinal(errorOccured);
        } finally {
          // set to null all the session info
          SessionInfo.init();
        }
      }
    };

    dth.run();
  }
}
