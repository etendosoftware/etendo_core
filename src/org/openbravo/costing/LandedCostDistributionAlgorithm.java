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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.costing;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.cost.LCReceiptLineAmt;
import org.openbravo.model.materialmgmt.cost.LandedCostCost;
import org.openbravo.service.db.DbUtility;

public abstract class LandedCostDistributionAlgorithm {
  private static final Logger log = LogManager.getLogger();

  public abstract void distributeAmount(LandedCostCost lcCost, boolean isMatchingAdjustment);

  public JSONObject cancelDistributeAmount(LandedCostCost lcCost) {

    JSONObject messageResponse = new JSONObject();
    OBContext.setAdminMode(false);
    try {
      messageResponse.put("severity", "success");
      int i = 0;
      List<String> idList = OBDao.getIDListFromOBObject(lcCost.getLandedCostReceiptLineAmtList());
      OBDal.getInstance().save(lcCost);

      for (String id : idList) {
        i++;
        LCReceiptLineAmt lcrla = OBDal.getInstance().get(LCReceiptLineAmt.class, id);
        lcCost.getLandedCostReceiptLineAmtList().remove(lcrla);
        OBDal.getInstance().remove(lcrla);
        if (i % 100 == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
      }
      OBDal.getInstance().flush();
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Error in LCMatchingProcessHandler: " + e.getMessage(), e);
      try {
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String strMessage = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        messageResponse.put("severity", "error");
        messageResponse.put("title", OBMessageUtils.messageBD("Error"));
        messageResponse.put("text", strMessage);
      } catch (Exception ignore) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return messageResponse;
  }
}
