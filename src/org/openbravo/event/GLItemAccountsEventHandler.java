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
 * All portions are Copyright (C) 2018-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.gl.GLItemAccounts;

class GLItemAccountsEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(GLItemAccounts.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes final EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkForValidAccountCombination(event);
  }

  private void checkForValidAccountCombination(final EntityPersistenceEvent event) {
    GLItemAccounts gLItemAccounts = (GLItemAccounts) event.getTargetInstance();

    final String gLItemAccountingSchemaId = gLItemAccounts.getAccountingSchema().getId();
    final String gLItemDebitAccountingSchemaId = gLItemAccounts.getGlitemDebitAcct()
        .getAccountingSchema()
        .getId();
    final String gLItemCreditAccountingSchemaId = gLItemAccounts.getGlitemCreditAcct()
        .getAccountingSchema()
        .getId();

    if (!StringUtils.equals(gLItemAccountingSchemaId, gLItemDebitAccountingSchemaId)
        || !StringUtils.equals(gLItemAccountingSchemaId, gLItemCreditAccountingSchemaId)) {
      throw new OBException(OBMessageUtils.messageBD("InvalidAccountForGeneralLedger"));
    }

  }
}
