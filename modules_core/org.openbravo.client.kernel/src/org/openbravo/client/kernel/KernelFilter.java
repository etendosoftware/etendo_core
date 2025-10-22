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
 * All portions are Copyright (C) 2011 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.kernel;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.openbravo.dal.core.ThreadHandler;

/**
 * Sets the request/response object in the request context.
 * 
 * @author mtaal
 */

public class KernelFilter implements Filter {

  @Override
  public void init(FilterConfig fConfig) throws ServletException {
    RequestContext.setServletContext(fConfig.getServletContext());
  }

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response,
      final FilterChain chain) throws IOException, ServletException {
    final ThreadHandler dth = new ThreadHandler() {

      @Override
      public void doBefore() {
        RequestContext.get().setRequest((HttpServletRequest) request);
        RequestContext.get().setResponse((HttpServletResponse) response);
      }

      @Override
      protected void doAction() throws Exception {
        chain.doFilter(request, response);
      }

      @Override
      public void doFinal(boolean errorOccured) {
        RequestContext.clear();
      }
    };

    dth.run();
  }
}
