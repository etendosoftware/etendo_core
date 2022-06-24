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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.filterexpression;

import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.openbravo.client.kernel.ComponentProvider;

@RequestScoped
abstract class MatchStatementFilterExpressionHandler {
  private static final Logger log = LogManager.getLogger();

  @Inject
  @Any
  private Instance<MatchStatementFilterExpressionHandler> matchStatementFilterExpressionHandlers;

  protected abstract long getSeq();

  /**
   * This method gets called to obtain the default filtering values for the grid. It can be
   * overwritten using Injections.
   * 
   * @param requestMap
   * @return String containing the value for the filter expression
   * @throws JSONException
   */
  String getFilterExpression(Map<String, String> requestMap) throws JSONException {
    return "No";
  }

  protected MatchStatementFilterExpressionHandler getDefaultsHandler(String strWindowId) {
    MatchStatementFilterExpressionHandler handler = null;

    for (MatchStatementFilterExpressionHandler nextHandler : matchStatementFilterExpressionHandlers
        .select(new ComponentProvider.Selector(strWindowId))) {
      if (handler == null) {
        handler = nextHandler;
      } else if (nextHandler.getSeq() < handler.getSeq()) {
        handler = nextHandler;
      } else if (nextHandler.getSeq() == handler.getSeq()) {
        log.warn(
            "Trying to get handler for window with id {}, there are more than one instance with the same sequence",
            strWindowId);
      }
    }
    return handler;
  }

}
