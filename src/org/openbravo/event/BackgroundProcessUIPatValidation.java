package org.openbravo.event;


import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;

import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.domain.ModelImplementation;
import org.openbravo.model.ad.ui.Process;


import javax.enterprise.event.Observes;

public class BackgroundProcessUIPatValidation extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Process.class) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    doCheck(event);
  }


  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    doCheck(event);
  }

  private static void doCheck(EntityPersistenceEvent event) {
    Property propBackground = event.getTargetInstance().getEntity().getProperty(Process.PROPERTY_BACKGROUND);
    Property propUIPattern = event.getTargetInstance().getEntity().getProperty(Process.PROPERTY_UIPATTERN);
    if (((boolean) event.getCurrentState(propBackground))
        && StringUtils.equalsIgnoreCase((String) event.getCurrentState(propUIPattern), "S")
        && notHasProcessClasses(event)) {
      throw new OBException(OBMessageUtils.messageBD("BackgndProcessValidationUIPat"));
    }
  }

  private static boolean notHasProcessClasses(EntityPersistenceEvent event) {
    String processId = (String) event.getTargetInstance().getId();
    OBCriteria<ModelImplementation> modelImplCrit = OBDal.getInstance().createCriteria(
        ModelImplementation.class);
    modelImplCrit.add(Restrictions.eq(ModelImplementation.PROPERTY_PROCESS + ".id", processId));
    return modelImplCrit.setMaxResults(1).uniqueResult() == null;
  }
}

