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
 * All portions are Copyright (C) 2013-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.service.db.DalConnectionProvider;

class PeriodEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Period.ENTITY_NAME) };
  private static final Logger logger = LogManager.getLogger();

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    if (!adjustmentPeriod((Period) event.getTargetInstance())) {
      checkPeriod((Period) event.getTargetInstance());
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    if (!adjustmentPeriod((Period) event.getTargetInstance())) {
      checkPeriod((Period) event.getTargetInstance());
    }
  }

  private boolean adjustmentPeriod(Period period) {
    return "A".equals(period.getPeriodType());
  }

  private void checkPeriod(Period period) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    OBCriteria<Period> criteria = OBDal.getInstance().createCriteria(Period.class);
    criteria.add(Restrictions.eq(Period.PROPERTY_ORGANIZATION, period.getOrganization()));
    criteria.add(Restrictions.eq(Period.PROPERTY_CLIENT, period.getClient()));
    criteria.add(Restrictions.ne(Period.PROPERTY_ID, period.getId()));
    criteria.add(Restrictions.ge(Period.PROPERTY_ENDINGDATE, period.getStartingDate()));
    criteria.add(Restrictions.le(Period.PROPERTY_STARTINGDATE, period.getEndingDate()));
    criteria.add(Restrictions.eq(Period.PROPERTY_PERIODTYPE, "S"));
    criteria.createAlias(Period.PROPERTY_YEAR, "y");
    criteria.add(Restrictions.eq("y." + Year.PROPERTY_CALENDAR, period.getYear().getCalendar()));
    criteria.setMaxResults(1);

    if (criteria.uniqueResult() != null) {
      logger
          .error("Period " + period.getId() + " is being saved and is overlapping another period");
      throw new OBException(Utility.messageBD(conn, "DatesOverlappedParams", language)
          .replace("%1", ((Period) criteria.uniqueResult()).getName())
          .replace("%2", ((Period) criteria.uniqueResult()).getYear().getFiscalYear()));
    }
  }
}
