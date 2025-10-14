package org.openbravo.client.application.event;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.application.Process;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import jakarta.enterprise.event.Observes;

/**
 * Checks that a Process Definition of Action type is always multi record.
 */
class ProcessDefinitionHandler extends EntityPersistenceEventObserver {
    private static final Entity[] entities = { ModelProvider.getInstance().getEntity(Process.ENTITY_NAME) };
    private static final String ACTION_UI_PATTERN = "A";

    @Override
    protected Entity[] getObservedEntities() {
        return entities;
    }

    public void onUpdate(@Observes EntityUpdateEvent event) {
        if (!isValidEvent(event)) {
            return;
        }
        Property uiPatternProperty = entities[0].getProperty(Process.PROPERTY_UIPATTERN);
        Property multiRecordProperty = entities[0].getProperty(Process.PROPERTY_ISMULTIRECORD);

        String uiPattern = (String) event.getCurrentState(uiPatternProperty);
        boolean isMultiRecord = (boolean) event.getCurrentState(multiRecordProperty);

        checkMultiRecord(uiPattern, isMultiRecord);

    }

    public void onNew(@Observes EntityNewEvent event) {
        if (!isValidEvent(event)) {
            return;
        }
        Property uiPatternProperty = entities[0].getProperty(Process.PROPERTY_UIPATTERN);
        Property multiRecordProperty = entities[0].getProperty(Process.PROPERTY_ISMULTIRECORD);

        String uiPattern = (String) event.getCurrentState(uiPatternProperty);
        boolean isMultiRecord = (boolean) event.getCurrentState(multiRecordProperty);

        checkMultiRecord(uiPattern, isMultiRecord);
    }

    private void checkMultiRecord(String uiPattern, boolean isMultiRecord) {
        if (uiPattern != null && uiPattern.equals(ACTION_UI_PATTERN) && !isMultiRecord) {
            String language = OBContext.getOBContext().getLanguage().getLanguage();
            ConnectionProvider conn = new DalConnectionProvider(false);
            throw new OBException(Utility.messageBD(conn, "ActionMustBeMultiRecord", language));
        }
    }
}
