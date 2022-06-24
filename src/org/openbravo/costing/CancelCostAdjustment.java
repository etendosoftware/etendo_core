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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.cost.CostAdjustmentLine;
import org.openbravo.service.db.DbUtility;

public class CancelCostAdjustment extends BaseActionHandler {
  private static final Logger log = LogManager.getLogger();
  private static final String strCategoryCostAdj = "CAD";
  private static final String strTableCostAdj = "M_CostAdjustment";

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();
    JSONObject errorMessage = new JSONObject();
    OBContext.setAdminMode(true);
    try {
      final JSONObject jsonData = new JSONObject(data);
      String caId = jsonData.getString("inpmCostadjustmentId");
      CostAdjustment costAdjustmentOrig = OBDal.getInstance().get(CostAdjustment.class, caId);
      JSONObject message = doCancelCostAdjustment(costAdjustmentOrig);
      result.put("message", message);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(e.getMessage(), e);
      try {
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("title", OBMessageUtils.messageBD("Error"));
        errorMessage.put("text", message);
        result.put("message", errorMessage);
      } catch (JSONException ignore) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  public static JSONObject doCancelCostAdjustment(CostAdjustment costAdjustmentOrig)
      throws OBException, JSONException {
    CostAdjustment costAdjustmentCancel = (CostAdjustment) DalUtil.copy(costAdjustmentOrig, false);

    final DocumentType docType = FIN_Utility.getDocumentType(costAdjustmentOrig.getOrganization(),
        strCategoryCostAdj);
    final String docNo = FIN_Utility.getDocumentNo(docType, strTableCostAdj);
    costAdjustmentCancel.setDocumentNo(docNo);
    costAdjustmentCancel.setUpdated(new Date());
    costAdjustmentCancel.setUpdatedBy(OBContext.getOBContext().getUser());
    costAdjustmentCancel.setCreationDate(new Date());
    costAdjustmentCancel.setCreatedBy(OBContext.getOBContext().getUser());
    costAdjustmentCancel.setProcessed(false);
    costAdjustmentCancel.setPosted("N");
    OBDal.getInstance().save(costAdjustmentOrig);

    costAdjustmentOrig.setCostAdjustmentCancel(costAdjustmentCancel);
    costAdjustmentOrig.setDocumentStatus("VO");
    OBDal.getInstance().save(costAdjustmentCancel);
    OBDal.getInstance().flush();

    CostAdjustment cacProxy = (CostAdjustment) OBDal.getInstance()
        .getProxy(CostAdjustment.ENTITY_NAME, costAdjustmentCancel.getId());
    // Call cost
    OBCriteria<CostAdjustmentLine> qLines = OBDal.getInstance()
        .createCriteria(CostAdjustmentLine.class);
    qLines.add(Restrictions.eq(CostAdjustmentLine.PROPERTY_COSTADJUSTMENT, costAdjustmentOrig));
    qLines.add(Restrictions.eq(CostAdjustmentLine.PROPERTY_ISSOURCE, true));
    ScrollableResults scrollLines = qLines.scroll(ScrollMode.FORWARD_ONLY);
    try {
      int cnt = 0;
      while (scrollLines.next()) {
        final CostAdjustmentLine lineOrig = (CostAdjustmentLine) scrollLines.get()[0];
        CostAdjustmentLine lineCancel = (CostAdjustmentLine) DalUtil.copy(lineOrig, false);
        lineCancel.setUpdated(new Date());
        lineCancel.setUpdatedBy(OBContext.getOBContext().getUser());
        lineCancel.setCreationDate(new Date());
        lineCancel.setCreatedBy(OBContext.getOBContext().getUser());
        lineCancel.setCostAdjustment(cacProxy);
        lineCancel.setAdjustmentAmount(lineOrig.getAdjustmentAmount().negate());

        if (lineOrig.getInventoryTransaction().isCostPermanent()) {
          lineOrig.getInventoryTransaction().setCostPermanent(Boolean.FALSE);
          OBDal.getInstance().save(lineOrig.getInventoryTransaction());
        }
        OBDal.getInstance().save(lineCancel);
        if ((cnt++ % 10) == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
      }
    } finally {
      scrollLines.close();
    }
    OBDal.getInstance().flush();
    JSONObject message = new JSONObject();
    message.put("severity", "success");
    String strResult = OBMessageUtils.messageBD("CostAdjustmentCanceled");
    Map<String, String> map = new HashMap<String, String>();
    map.put("documentNo", docNo);
    message.put("title", OBMessageUtils.messageBD("Success"));
    message.put("text", OBMessageUtils.parseTranslation(strResult, map));

    CostAdjustmentProcess.doProcessCostAdjustment(costAdjustmentCancel);
    CostAdjustment costAdjCancel = OBDal.getInstance()
        .get(CostAdjustment.class, costAdjustmentCancel.getId());
    costAdjCancel.setDocumentStatus("VO");
    costAdjCancel.setPosted("N");
    OBDal.getInstance().save(costAdjCancel);
    OBDal.getInstance().flush();
    return message;
  }
}
