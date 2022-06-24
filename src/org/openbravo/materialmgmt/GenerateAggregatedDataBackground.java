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
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.materialmgmt;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class GenerateAggregatedDataBackground extends DalBaseProcess {

  private static final Logger log4j = LogManager.getLogger();
  private ProcessLogger logger;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    OBError result = new OBError();

    try {
      OBContext.setAdminMode(true);

      result.setType("Success");
      result.setTitle(OBMessageUtils.messageBD("Success"));

      List<Organization> legalEntities = new ArrayList<Organization>();
      Organization org = OBContext.getOBContext().getCurrentOrganization();
      Client client = OBContext.getOBContext().getCurrentClient();
      OrganizationStructureProvider osp = OBContext.getOBContext()
          .getOrganizationStructureProvider(client.getId());

      // Retrieve the Legal Entity related to the selected Organization
      Organization legalEntityOrg = osp.getLegalEntity(org);
      if (legalEntityOrg == null && !org.getId().equals("0")) {
        // In case there are no Legal Entities above this Organization, look in their children
        legalEntities = osp.getChildLegalEntitesList(org);
      } else if (legalEntityOrg == null && org.getId().equals("0")) {
        // In case is * Organization, retrieve all the Legal Entities for the Client
        legalEntities = osp.getLegalEntitiesListForSelectedClient(client.getId());
      } else {
        legalEntities.add(legalEntityOrg);
      }

      // If there are no Legal Entities present raise an error
      if (legalEntities.isEmpty()) {
        result.setMessage(OBMessageUtils.messageBD("NoLegalEntityFound"));
        result.setType("Error");
        result.setTitle(OBMessageUtils.messageBD("Error"));
        logger.logln(OBMessageUtils.messageBD("NoLegalEntityFound"));
        bundle.setResult(result);
        return;
      }

      for (Organization legalEntity : legalEntities) {

        // Get Closed Periods that need to be aggregated
        List<Period> periodList = ResetValuedStockAggregated.getClosedPeriodsToAggregate(new Date(),
            legalEntity.getClient().getId(), legalEntity.getId());

        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        Date startingDate = formatter.parse("01-01-0000");
        int totalNumberOfPeriods = periodList.size();
        int contPeriodNumber = 0;
        long start = System.currentTimeMillis();

        log4j.debug("[GenerateAggregatedDataBackground] Total number of Periods to aggregate: "
            + totalNumberOfPeriods);

        for (Period period : periodList) {
          long startPeriod = System.currentTimeMillis();
          // Aggregate Data for Valued Stock
          if (ResetValuedStockAggregated.noAggregatedDataForPeriod(period)
              && ResetValuedStockAggregated.costingRuleDefindedForPeriod(legalEntity, period)) {
            ResetValuedStockAggregated.insertValuesIntoValuedStockAggregated(legalEntity, period,
                startingDate);
            startingDate = period.getEndingDate();

            // Aggregate Data for other entities below this line
          }
          long elapsedTimePeriod = (System.currentTimeMillis() - startPeriod);
          contPeriodNumber++;
          log4j.debug("[GenerateAggregatedDataBackground] Periods processed: " + contPeriodNumber
              + " of " + totalNumberOfPeriods);
          log4j.debug(
              "[GenerateAggregatedDataBackground] Time to process period: " + elapsedTimePeriod);
        }
        long elapsedTime = (System.currentTimeMillis() - start);
        log4j.debug(
            "[GenerateAggregatedDataBackground] Time to process all periods: " + elapsedTime);
      }

      logger.logln(OBMessageUtils.messageBD("Success"));
      bundle.setResult(result);
      return;

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      String message = OBMessageUtils.parseTranslation(bundle.getConnection(),
          bundle.getContext().toVars(), OBContext.getOBContext().getLanguage().getLanguage(),
          e.getMessage());
      result.setMessage(message);
      result.setType("Error");
      result.setTitle(OBMessageUtils.messageBD("Error"));
      log4j.error(message, e);
      logger.logln(message);
      bundle.setResult(result);
      return;
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}
