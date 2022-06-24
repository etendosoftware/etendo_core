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
 * All portions are Copyright (C) 2012-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.erpCommon.ad_actionButton;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.mrp.ProductionRun;
import org.openbravo.model.mrp.ProductionRunLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;

public class MRPManufacturingPlanProcess extends DalBaseProcess {
  private static final Logger log4j = LogManager.getLogger();
  private static final String NULL = null;

  @Override
  protected void doExecute(final ProcessBundle bundle) throws Exception {
    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(OBMessageUtils.messageBD("Success"));
    final String strManufacturingMRPID = (String) bundle.getParams().get("MRP_Run_Production_ID");
    final ProductionRun productionRun = OBDal.getInstance()
        .get(ProductionRun.class, strManufacturingMRPID);
    final String userId = OBContext.getOBContext().getUser().getId();
    final String orgId = productionRun.getOrganization().getId();
    final String clientId = productionRun.getClient().getId();
    final String plannerId = productionRun.getPlanner() != null ? productionRun.getPlanner().getId()
        : NULL;
    final String productId = productionRun.getProduct() != null ? productionRun.getProduct().getId()
        : NULL;
    final String productCategoryId = productionRun.getProductCategory() != null
        ? productionRun.getProductCategory().getId()
        : NULL;
    final String bpId = productionRun.getBusinessPartner() != null
        ? productionRun.getBusinessPartner().getId()
        : NULL;
    final String bpCatId = productionRun.getBusinessPartnerCategory() != null
        ? productionRun.getBusinessPartnerCategory().getId()
        : NULL;
    final long timeHorizon = productionRun.getTimeHorizon();
    final long safetyLeadTime = productionRun.getSafetyLeadTime();
    final Date docDate = productionRun.getDocumentDate();

    try {
      log4j.debug("Prepare process delete not fixed lines and set exploded to false.");

      deleteNotFixedLines(strManufacturingMRPID);

      ScrollableResults linesToUpdate = getLinesToUpdate(strManufacturingMRPID);
      int i = 0;
      try {
        while (linesToUpdate.next()) {
          ProductionRunLine prLine = (ProductionRunLine) linesToUpdate.get(0);
          prLine.setInserted(false);
          OBDal.getInstance().save(prLine);

          if (i % 100 == 0) {
            OBDal.getInstance().flush();
            OBDal.getInstance().getSession().clear();
          }
        }
      } finally {
        linesToUpdate.close();
      }
      log4j.debug("Call MRP_Run_Initialize process");
      // v_ResultStr:='Initialize';
      // MRP_RUN_INITIALIZE(v_User_ID, v_Org_ID, v_Client_ID, v_Record_ID, v_Planner_ID,
      // v_Product_ID,
      // v_Product_Category_ID, v_BPartner_ID, v_BP_Group_ID, NULL, v_TimeHorizon,
      // v_PlanningDate, 'Y');
      List<Object> parameters = new ArrayList<>();
      parameters.add(userId);
      parameters.add(orgId);
      parameters.add(clientId);
      parameters.add(strManufacturingMRPID);
      parameters.add(plannerId);
      parameters.add(productId);
      parameters.add(productCategoryId);
      parameters.add(bpId);
      parameters.add(bpCatId);
      parameters.add(NULL);
      parameters.add(timeHorizon);
      parameters.add(docDate);
      parameters.add("Y");
      CallStoredProcedure.getInstance().call("MRP_RUN_INITIALIZE", parameters, null, true, false);

      OBDal.getInstance().commitAndClose();
      OBDal.getInstance().getSession().clear();

      log4j.debug("Call MRP_Run_Explode process");
      // v_ResultStr:='Explode';
      // MRP_RUN_EXPLODE(v_User_ID, v_Org_ID, v_Client_ID, v_Record_ID, v_Planner_ID, v_TimeHorizon,
      // v_PlanningDate, v_SecurityMargin);
      parameters = new ArrayList<>();
      parameters.add(userId);
      parameters.add(orgId);
      parameters.add(clientId);
      parameters.add(strManufacturingMRPID);
      parameters.add(plannerId);
      parameters.add(timeHorizon);
      parameters.add(docDate);
      parameters.add(safetyLeadTime);
      CallStoredProcedure.getInstance().call("MRP_RUN_EXPLODE", parameters, null, true, false);
      OBDal.getInstance().commitAndClose();
      OBDal.getInstance().getSession().clear();

      log4j.debug("Call MRP_ProcessPlan process");
      // v_ResultStr:='ProcessPlan';
      // MRP_PROCESSPLAN(v_User_ID, v_Org_ID, v_Client_ID, v_Record_ID, v_Planner_ID, v_TimeHorizon,
      // v_PlanningDate, v_SecurityMargin);
      parameters = new ArrayList<>();
      parameters.add(userId);
      parameters.add(orgId);
      parameters.add(clientId);
      parameters.add(strManufacturingMRPID);
      parameters.add(plannerId);
      parameters.add(timeHorizon);
      parameters.add(docDate);
      parameters.add(safetyLeadTime);
      CallStoredProcedure.getInstance().call("MRP_PROCESSPLAN", parameters, null, true, false);

      OBDal.getInstance().commitAndClose();
      OBDal.getInstance().getSession().clear();
    } catch (final Exception e) {
      deleteNotFixedLines(strManufacturingMRPID);

      log4j.error("Exception found in MRPManufacturingProcess: ", e);
      msg = OBMessageUtils.translateError(bundle.getConnection(), bundle.getContext().toVars(),
          OBContext.getOBContext().getLanguage().getLanguage(), e.getMessage());
      msg.setTitle(OBMessageUtils.messageBD("Error"));

    } finally {
      bundle.setResult(msg);
    }

  }

  private void deleteNotFixedLines(final String strManufacturingMRPID) {
    //@formatter:off
    final String hqlDelete =
            "delete from MRPProductionRunLine" +
            " where manufacturingPlan.id = :prodRunId" +
            "   and fixed = false";
    //@formatter:on

    @SuppressWarnings("rawtypes")
    final Query delete = OBDal.getInstance()
        .getSession()
        .createQuery(hqlDelete)
        .setParameter("prodRunId", strManufacturingMRPID);

    delete.executeUpdate();
    OBDal.getInstance().flush();
  }

  private ScrollableResults getLinesToUpdate(final String productionRunId) {
    //@formatter:off
    final String hqlWhere =
                  " where manufacturingPlan.id = :prodRunId" +
                  "   and quantity < 0" +
                  "   and transactionType <> 'WR'";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(ProductionRunLine.class, hqlWhere)
        .setNamedParameter("prodRunId", productionRunId)
        .setFetchSize(1000)
        .scroll(ScrollMode.FORWARD_ONLY);
  }

}
