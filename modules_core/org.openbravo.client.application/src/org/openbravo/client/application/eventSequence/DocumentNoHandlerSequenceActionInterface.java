package org.openbravo.client.application.eventSequence;

import org.openbravo.base.model.Entity;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;

public interface DocumentNoHandlerSequenceActionInterface {

    void handleEvent(EntityPersistenceEvent event);

    Entity[] getObservedEntities();

}
