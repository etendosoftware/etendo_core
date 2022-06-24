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
 * All portions are Copyright (C) 2013-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.event;

import java.util.HashMap;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.client.kernel.event.TransactionBeginEvent;
import org.openbravo.client.kernel.event.TransactionCompletedEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.VariantChDescUpdateProcess;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.scheduling.OBScheduler;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;

class CharacteristicValueEventHandler extends EntityPersistenceEventObserver {
  private static Logger logger = LogManager.getLogger();
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(CharacteristicValue.ENTITY_NAME) };
  private static ThreadLocal<String> chvalueUpdated = new ThreadLocal<>();

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onTransactionBegin(@Observes TransactionBeginEvent event) {
    chvalueUpdated.remove();
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final CharacteristicValue chv = (CharacteristicValue) event.getTargetInstance();
    chvalueUpdated.set(chv.getId());
    // Update all product characteristics configurations with updated code of the characteristic.
    // Only when product characteristics is not linked with subset.
    final Entity prodchValue = ModelProvider.getInstance()
        .getEntity(CharacteristicValue.ENTITY_NAME);
    final Property codeProperty = prodchValue.getProperty(CharacteristicValue.PROPERTY_CODE);
    if (event.getCurrentState(codeProperty) != event.getPreviousState(codeProperty)) {
      //@formatter:off
      String hql = "update ProductCharacteristicConf as pcc "
                 + "set code = :code, "
                 + "updated = now(), "
                 + "updatedBy = :user "
                 + "where exists ( "
                 + "    select 1 "
                 + "    from  ProductCharacteristic as pc "
                 + "    where pcc.characteristicOfProduct = pc "
                 + "    and pcc.characteristicValue.id = :characteristicValueId "
                 + "    and pc.characteristicSubset is null "
                 + "    and pcc.code <> :code) ";
      //@formatter:on
      try {
        OBDal.getInstance()
            .getSession()
            .createQuery(hql)
            .setParameter("user", OBContext.getOBContext().getUser())
            .setParameter("characteristicValueId", chv.getId())
            .setParameter("code", chv.getCode())
            .executeUpdate();
      } catch (Exception e) {
        logger.error(
            "Error on CharacteristicValueEventHandler. ProductCharacteristicConf could not be updated",
            e);
      }
    }
  }

  public void onTransactionCompleted(@Observes TransactionCompletedEvent event) {
    String strChValueId = chvalueUpdated.get();
    chvalueUpdated.remove();
    if (StringUtils.isBlank(strChValueId)
        || event.getTransaction().getStatus() == TransactionStatus.ROLLED_BACK) {
      return;
    }
    try {
      VariablesSecureApp vars = initializeVars();
      ProcessBundle pb = new ProcessBundle(VariantChDescUpdateProcess.AD_PROCESS_ID, vars)
          .init(new DalConnectionProvider(false));
      HashMap<String, Object> parameters = new HashMap<>();
      parameters.put("mProductId", "");
      parameters.put("mChValueId", strChValueId);
      pb.setParams(parameters);
      OBScheduler.getInstance().schedule(pb);
    } catch (Exception e) {
      logger.error("Error executing process", e);
    }
  }

  private VariablesSecureApp initializeVars() {
    VariablesSecureApp vars = null;
    try {
      vars = RequestContext.get().getVariablesSecureApp();
    } catch (Exception e) {
      logger.info("Vars could not be initialized from RequestContext, initializing from OBContext");
      vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getCurrentClient().getId(),
          OBContext.getOBContext().getCurrentOrganization().getId(),
          OBContext.getOBContext().getRole().getId(),
          OBContext.getOBContext().getLanguage().getLanguage());
    }
    return vars;
  }
}
