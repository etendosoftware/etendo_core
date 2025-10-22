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
 * All portions are Copyright (C) 2009-2010 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.security;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.filter.IsIDFilter;

/**
 * This servlet checks whether a session (passed as id parameter) is active in the current context.
 * 
 */
public class SessionActive extends HttpBaseServlet {

  private static final long serialVersionUID = 1L;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doOptions(req, resp);
    String sessionId = req.getParameter("id");
    boolean result = IsIDFilter.instance.accept(sessionId);
    if (result) {
      result = SessionListener.isSessionActiveInContext(sessionId);
    }

    PrintWriter out = resp.getWriter();
    out.print(result);
    out.close();
  }

  @Override
  public void doOptions(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    setCORSHeaders(request, response);
  }

  protected void setCORSHeaders(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String origin = request.getHeader("Origin");

    if (origin != null && !origin.equals("")) {
      response.setHeader("Access-Control-Allow-Origin", origin);
      response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
      response.setHeader("Access-Control-Allow-Headers",
          "Content-Type, origin, accept, X-Requested-With");
      response.setHeader("Access-Control-Max-Age", "1000");
    }
  }
}
