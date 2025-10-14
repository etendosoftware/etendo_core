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
 * All portions are Copyright (C) 2001-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpReports;

import java.io.IOException;
import java.util.HashMap;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

public class RptM_Requisition extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = createVars(request);

    if (vars.commandIn("DEFAULT")) {
      String strmRequisitionId = vars.getSessionValue("RptM_Requisition.inpmRequisitionId_R");
      if (strmRequisitionId.equals("")) {
        strmRequisitionId = vars.getSessionValue("RptM_Requisition.inpmRequisitionId");
      }
      if (log4j.isDebugEnabled()) {
        log4j.debug("+***********************: " + strmRequisitionId);
      }
      printPagePartePDF(response, vars, strmRequisitionId);
    } else {
      pageError(response);
    }
  }

  protected void printPagePartePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strmRequisitionId) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: pdf");
    }
    HashMap<String, Object> parameters = new HashMap<>();
    parameters.put("REQUISITION_ID", strmRequisitionId);
    renderJR(vars, response, null, "pdf", parameters, null, null);
  }

  @Override
  public String getServletInfo() {
    return "Servlet that presents the RptMRequisitions seeker";
  } // End of getServletInfo() method

  /**
   * Creates a {@link VariablesSecureApp} from the given {@link HttpServletRequest}.
   *
   * @param request the HTTP request
   * @return the initialized secure variables
   */
  protected VariablesSecureApp createVars(HttpServletRequest request) {
    return new VariablesSecureApp(request);
  }
}
