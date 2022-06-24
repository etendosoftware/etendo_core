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
 * All portions are Copyright (C) 2012-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.client.application.event;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationAcctSchema;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;

class GeneralLedgerOrganizationHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Organization.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final Organization organization = (Organization) event.getTargetInstance();
    if (organization != null) {
      if ((event.getPreviousState(getProperty(Organization.PROPERTY_GENERALLEDGER)) == null
          && event.getCurrentState(getProperty(Organization.PROPERTY_GENERALLEDGER)) != null)
          || (event.getPreviousState(getProperty(Organization.PROPERTY_GENERALLEDGER)) != null
              && !event.getPreviousState(getProperty(Organization.PROPERTY_GENERALLEDGER))
                  .equals(
                      event.getCurrentState(getProperty(Organization.PROPERTY_GENERALLEDGER))))) {
        final AcctSchema generalLedger = organization.getGeneralLedger();

        OBCriteria<OrganizationAcctSchema> orgSchema = OBDal.getInstance()
            .createCriteria(OrganizationAcctSchema.class);
        orgSchema.setFilterOnReadableOrganization(false);
        orgSchema.setFilterOnActive(false);
        orgSchema.add(Restrictions.eq(OrganizationAcctSchema.PROPERTY_ORGANIZATION, organization));
        List<OrganizationAcctSchema> orgSchemalist = orgSchema.list();
        ArrayList<String> idlist = new ArrayList<>();

        boolean exist = false;

        for (OrganizationAcctSchema oas : orgSchemalist) {
          idlist.add(oas.getId());
        }
        for (String ids : idlist) {
          OrganizationAcctSchema orgAcctSchema = OBDal.getInstance()
              .get(OrganizationAcctSchema.class, ids);
          if (generalLedger != null
              && generalLedger.getId() == orgAcctSchema.getAccountingSchema().getId()) {
            orgAcctSchema.setActive(true);
            exist = true;
            continue;
          }
          if (orgAcctSchema.getOrganizationClosingList().isEmpty()) {
            OBDal.getInstance().remove(orgAcctSchema);
          } else {
            orgAcctSchema.setActive(false);
            OBDal.getInstance().save(orgAcctSchema);
          }
        }

        if ((generalLedger != null) && !exist) {
          final OrganizationAcctSchema orgAcctSchema = OBProvider.getInstance()
              .get(OrganizationAcctSchema.class);
          orgAcctSchema.setOrganization(organization);
          orgAcctSchema.setAccountingSchema(generalLedger);
          OBDal.getInstance().save(orgAcctSchema);
        }
      }
    }
  }

  private Property getProperty(String property) {
    return entities[0].getProperty(property);
  }
}
