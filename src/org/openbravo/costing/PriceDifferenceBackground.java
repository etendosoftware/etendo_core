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
 * All portions are Copyright (C) 2014-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.costing;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

/**
 * @author mdejuana
 * 
 */
public class PriceDifferenceBackground extends DalBaseProcess {
  private static final Logger log4j = LogManager.getLogger();
  public static final String AD_PROCESS_ID = "BA574D8A4CF54AFF8B7BC2E6FACA161E";
  private ProcessLogger logger;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();
    OBError result = new OBError();
    try {
      OBContext.setAdminMode(false);
      result.setType("Success");
      result.setTitle(OBMessageUtils.messageBD("Success"));

      List<Organization> legalOrganizations = PriceDifferenceUtil
          .getLegalOrganizationList(PriceDifferenceUtil.ALL_ORGANIZATIONS);
      for (Organization legalOrganization : legalOrganizations) {
        PriceDifferenceProcess.processPriceDifference(legalOrganization);
      }

      logger.logln(OBMessageUtils.messageBD("Success"));
      bundle.setResult(result);
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      result = OBMessageUtils.translateError(bundle.getConnection(), bundle.getContext().toVars(),
          OBContext.getOBContext().getLanguage().getLanguage(), e.getMessage());
      log4j.error(result.getMessage(), e);
      logger.logln(result.getMessage());
      bundle.setResult(result);
      return;
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
