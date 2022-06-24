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

import java.math.BigDecimal;

import javax.enterprise.event.Observes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.model.financialmgmt.gl.GLJournalLine;

class GLJournalLineEventHandler extends EntityPersistenceEventObserver {
  private static Logger logger = LogManager.getLogger();
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(GLJournalLine.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final GLJournalLine journalLine = (GLJournalLine) event.getTargetInstance();
    checkAllowModification(event, journalLine);
  }

  // After reactivating the GL Journal, if you want to modify the Credit/Debit/Financial
  // Account/Payment Method/GL Item/Payment Date of any of these lines, you must first reactivate
  // and delete its related payments (if any)
  private void checkAllowModification(final EntityUpdateEvent event,
      final GLJournalLine journalLine) {
    final Entity gljournalLine = ModelProvider.getInstance().getEntity(GLJournalLine.ENTITY_NAME);
    if (!journalLine.getJournalEntry().isProcessed() && journalLine.getRelatedPayment() != null) {
      final Property credit = gljournalLine.getProperty(GLJournalLine.PROPERTY_CREDIT);
      final Property debit = gljournalLine.getProperty(GLJournalLine.PROPERTY_DEBIT);
      final Property openItems = gljournalLine.getProperty(GLJournalLine.PROPERTY_OPENITEMS);
      if (((BigDecimal) event.getCurrentState(credit))
          .compareTo((BigDecimal) event.getPreviousState(credit)) != 0
          || ((BigDecimal) event.getCurrentState(debit))
              .compareTo((BigDecimal) event.getPreviousState(debit)) != 0
          || !((Boolean) event.getCurrentState(openItems))
              .equals(event.getPreviousState(openItems))) {
        logger.info("Current credit: " + event.getCurrentState(credit) + ". Previous Credit: "
            + event.getPreviousState(credit));
        logger.info("Current debit: " + event.getCurrentState(debit) + ". Previous Debit: "
            + event.getPreviousState(debit));
        logger.info("Current Open items: " + event.getCurrentState(openItems)
            + ". Previous Open items: " + event.getPreviousState(openItems));
        throw new OBException("@ModifyGLJournalLine@");
      }
    }
  }

}
