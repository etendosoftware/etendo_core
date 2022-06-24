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
 * All portions are Copyright (C) 2015-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.gl.GLJournal;
import org.openbravo.model.financialmgmt.gl.GLJournalLine;

class GLJournalEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(GLJournal.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final GLJournal glj = (GLJournal) event.getTargetInstance();
    // Update GLJournalLine with updated Currency and Currency Rate.
    final Entity gljournal = ModelProvider.getInstance().getEntity(GLJournal.ENTITY_NAME);
    final Property currencyProperty = gljournal.getProperty(GLJournal.PROPERTY_CURRENCY);
    final Property currencyRate = gljournal.getProperty(GLJournal.PROPERTY_RATE);
    if (!event.getCurrentState(currencyProperty).equals(event.getPreviousState(currencyProperty))
        || !event.getCurrentState(currencyRate).equals(event.getPreviousState(currencyRate))) {
      OBCriteria<GLJournalLine> gljournallineCriteria = OBDal.getInstance()
          .createCriteria(GLJournalLine.class);
      gljournallineCriteria.add(Restrictions.eq(GLJournalLine.PROPERTY_JOURNALENTRY, glj));
      ScrollableResults scrollLines = gljournallineCriteria.scroll(ScrollMode.FORWARD_ONLY);

      try {
        if (gljournallineCriteria.count() > 0) {
          int i = 0;
          while (scrollLines.next()) {
            final GLJournalLine journalLine = (GLJournalLine) scrollLines.get()[0];
            if (!glj.getCurrency().getId().equals(journalLine.getCurrency().getId())) {
              journalLine.setCurrency(glj.getCurrency());
              OBDal.getInstance().save(journalLine);
            }
            if (!glj.getRate().equals(journalLine.getRate())) {
              journalLine.setRate(glj.getRate());
              OBDal.getInstance().save(journalLine);
            }
            i++;
            if (i % 100 == 0) {
              OBDal.getInstance().flush();
              OBDal.getInstance().getSession().clear();
            }
          }
        }
      } finally {
        scrollLines.close();
      }
    }
  }
}
