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
 * All portions are Copyright (C) 2014-2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.manufacturing.processplan.Operation;
import org.openbravo.model.manufacturing.processplan.OperationProduct;
import org.openbravo.model.manufacturing.processplan.OperationProductAttribute;
import org.openbravo.model.manufacturing.processplan.ProcessPlan;
import org.openbravo.model.manufacturing.processplan.Version;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;

public class CopyProcessPlanVersion extends BaseProcessActionHandler {
  final static private Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    Boolean dataInconsistency = false;
    String message = "";
    JSONObject errorMessage = new JSONObject();
    final OBContext obContext = OBContext.getOBContext();
    OBContext.setAdminMode(true);
    try {
      jsonRequest = new JSONObject(content);
      JSONObject params = jsonRequest.getJSONObject("_params");
      String processPlanVersionId = params.getString("MA_Processplan_Version_ID");
      String processPlanId = jsonRequest.getString("inpmaProcessplanId");
      ProcessPlan processPlan = OBDal.getInstance().get(ProcessPlan.class, processPlanId);
      Version objVersion = OBDal.getInstance().get(Version.class, processPlanVersionId);

      for (Operation manufacturingOpern : objVersion.getManufacturingOperationList()) {
        // Check
        if (!obContext
            .getOrganizationStructureProvider(processPlan.getOrganization().getClient().getId())
            .isInNaturalTree(processPlan.getOrganization(),
                manufacturingOpern.getActivity().getOrganization())) {
          dataInconsistency = true;
          message = "There is an inconsistency with Activity "
              + manufacturingOpern.getActivity().getIdentifier() + " for organization "
              + processPlan.getOrganization().getIdentifier();
          break;
        }
        for (OperationProduct opernProd : manufacturingOpern
            .getManufacturingOperationProductList()) {
          if (!obContext
              .getOrganizationStructureProvider(processPlan.getOrganization().getClient().getId())
              .isInNaturalTree(processPlan.getOrganization(),
                  opernProd.getProduct().getOrganization())) {
            dataInconsistency = true;
            message = "There is an inconsistency with Product "
                + opernProd.getProduct().getIdentifier() + " for Organization "
                + processPlan.getOrganization().getIdentifier();
            break;
          }
        }
      }

      if (!dataInconsistency) {
        // Copy Version Tab record
        User currentUser = OBContext.getOBContext().getUser();
        Version objCloneVersion = (Version) DalUtil.copy(objVersion, false);
        // Set Parent record as Process Plan on which the copy process is being called.
        objCloneVersion.setProcessPlan(processPlan);
        objCloneVersion.setDocumentNo(callAdSequenceDoc(objCloneVersion));
        // Set Organization as the Organization of parent tab record i.e Process Plan on which copy
        // process is being called
        objCloneVersion.setOrganization(processPlan.getOrganization());
        // As ma_sequence_trg takes care to update Estimated Time in Version Tab
        // as per Estimated Time in Operation Tab
        // Set Estimated Time by Default to Zero
        objCloneVersion.setEstimatedtime(BigDecimal.ZERO);
        // Set Created By, Created, UpdatedBy, Updated
        objCloneVersion.setCreationDate(new Date());
        objCloneVersion.setCreatedBy(currentUser);
        objCloneVersion.setUpdated(new Date());
        objCloneVersion.setUpdatedBy(currentUser);
        OBDal.getInstance().save(objCloneVersion);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(objCloneVersion);
        for (Operation manufacturingOpern : objVersion.getManufacturingOperationList()) {
          // Copy Operations
          Operation objOperation = (Operation) DalUtil.copy(manufacturingOpern, false);
          // Set Parent as the cloned Version
          objOperation.setProcessPlanVersion(objCloneVersion);
          // Set Organization as the Organization of the parent tab record
          objOperation.setOrganization(objCloneVersion.getOrganization());
          // Set Created By, Created, UpdatedBy, Updated
          objOperation.setCreationDate(new Date());
          objOperation.setCreatedBy(currentUser);
          objOperation.setUpdated(new Date());
          objOperation.setUpdatedBy(currentUser);
          objCloneVersion.getManufacturingOperationList().add(objOperation);
          for (OperationProduct opernProd : manufacturingOpern
              .getManufacturingOperationProductList()) {
            // Copy Operation Products
            OperationProduct objOpernProd = (OperationProduct) DalUtil.copy(opernProd, false);
            // Set Parent as ObjOperation
            objOpernProd.setMASequence(objOperation);
            // Set Organization of parent
            objOpernProd.setOrganization(objCloneVersion.getOrganization());
            // Set Created By, Created, UpdatedBy, Updated
            objOpernProd.setCreationDate(new Date());
            objOpernProd.setCreatedBy(currentUser);
            objOpernProd.setUpdated(new Date());
            objOpernProd.setUpdatedBy(currentUser);
            objOperation.getManufacturingOperationProductList().add(objOpernProd);
            // Copy Product Attributes
            for (OperationProductAttribute opernProdAtt : opernProd
                .getManufacturingOperationProductAttributeList()) {
              OperationProductAttribute objOpernProdAtt = (OperationProductAttribute) DalUtil
                  .copy(opernProdAtt);
              // Set Parent as objOpernProd
              objOpernProdAtt.setSequenceproduct(objOpernProd);
              // Set Organization as Organization of the Parent Tab record i.e objOpernProd
              objOpernProdAtt.setOrganization(objOpernProd.getOrganization());
              // Set Created By, Created, UpdatedBy, Updated
              objOpernProdAtt.setCreationDate(new Date());
              objOpernProdAtt.setCreatedBy(currentUser);
              objOpernProdAtt.setUpdated(new Date());
              objOpernProdAtt.setUpdatedBy(currentUser);
              objOpernProd.getManufacturingOperationProductAttributeList().add(objOpernProdAtt);
            }
          }
        }
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(objCloneVersion);
        errorMessage.put("severity", "success");
        errorMessage.put("text", OBMessageUtils.messageBD("success"));
        jsonRequest.put("message", errorMessage);

      } else {
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonRequest.put("message", errorMessage);
      }

    } catch (Exception e) {
      log.error("Error in Copy Process Plan Version Action Handler", e);
      try {
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        return getErrorMessage(OBMessageUtils.translateError(ex.getMessage()).getMessage());
      } catch (Exception ignore) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    return jsonRequest;

  }

  /**
   * Call AD_Sequence_Doc
   */
  private String callAdSequenceDoc(Version objCloneVersion) {
    return Utility.getDocumentNo(new DalConnectionProvider(false),
        objCloneVersion.getClient().getId(), "MA_ProcessPlan_Version", true);
  }

  /**
   * Returns a JSONObject with the error message to be printed and retry execution
   */
  private static JSONObject getErrorMessage(final String msgText) {
    final JSONObject result = new JSONObject();
    try {
      final JSONObject msg = new JSONObject();
      msg.put("severity", "error");
      msg.put("text", msgText);
      result.put("message", msg);
      result.put("retryExecution", false);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return result;
  }
}
