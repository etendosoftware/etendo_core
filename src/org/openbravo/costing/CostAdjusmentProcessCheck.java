/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2014-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.costing;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;

/**
 * Define needed checks for Cost Adjustment process. Note that classes implementing this interface
 * should be annotated as {@code @RequestScoped}.
 */
public interface CostAdjusmentProcessCheck {

  /**
   * This method executes the needed checks to the cost adjustment when the process starts.
   * 
   * @param costAdjustment
   *          the cost adjustment being processed.
   * @param message
   *          a JSONObject with the message that is returned to the user.
   * @throws OBException
   *           when any check fails ant the process is required to stop.
   */
  void doCheck(CostAdjustment costAdjustment, JSONObject message) throws OBException;

  /**
   * This method executes the needed checks to the cost adjustment when the process endss.
   * 
   * @param costAdjustment
   *          the cost adjustment being processed.
   * @param message
   *          a JSONObject with the message that is returned to the user.
   * @throws OBException
   *           when any check fails ant the process is required to stop.
   */
  void doPostProcessCheck(CostAdjustment costAdjustment, JSONObject message) throws OBException;
}
